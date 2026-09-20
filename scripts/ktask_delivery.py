"""Recoverable Git commit intent and reconstruction of published task receipts."""

import json
import re
import hashlib
import subprocess

from ktask_queue import implementation_paths


def fingerprint(contents, modes):
    return hashlib.sha256(json.dumps([contents, modes], sort_keys=True).encode()).hexdigest()


def committed_candidate(root, task, parent, commit, git):
    paths = git(root, 'diff', '--no-renames', '--name-only', '-z', parent, commit).split('\0')
    paths = sorted(implementation_paths(root, set(paths) - {''}, parent, git, commit))
    contents, modes = {}, {}
    for path in paths:
        entry = git(root, 'ls-tree', commit, '--', path)
        if not entry:
            contents[path] = 'missing'
            continue
        mode, kind, object_id = entry.split('\t', 1)[0].split()
        if kind != 'blob':
            raise ValueError('Unsupported published candidate entry')
        data = subprocess.run(['git', 'cat-file', 'blob', object_id], cwd=root,
                              check=True, capture_output=True, timeout=30).stdout
        contents[path] = 'link:' + data.decode() if mode == '120000' else hashlib.sha256(data).hexdigest()
        if mode != '120000':
            modes[path] = mode == '100755'
    return fingerprint(contents, modes)


def prepare(root, path, state, message, git, save):
    """Persist the exact reviewed tree and message before Git can move HEAD."""
    intent = dict(state=state, tree=git(root, 'write-tree'), message=message)
    save(path, intent)
    return intent


def finish(root, intent, git):
    """Commit once, or recognize only the exact child described by the intent."""
    state = intent['state']
    head = git(root, 'rev-parse', 'HEAD')
    if git(root, 'branch', '--show-current') != state['branch']:
        raise ValueError('Pending commit branch changed')
    if git(root, 'write-tree') != intent['tree'] or git(root, 'diff', '--name-only'):
        raise ValueError('Pending commit index or working tree changed')
    if head == state['baseline']:
        git(root, 'commit', '--allow-empty', '--cleanup=verbatim', '-m', intent['message'])
        head = git(root, 'rev-parse', 'HEAD')
    if (git(root, 'show', '-s', '--format=%P', head) != state['baseline']
            or git(root, 'show', '-s', '--format=%T', head) != intent['tree']
            or git(root, 'show', '-s', '--format=%B', head) != intent['message'].strip()):
        raise ValueError('HEAD is not the prepared reviewed commit')
    return dict(state, commit=head)


def published_receipts(root, tasks, policy, git):
    """Reconstruct from trusted published history, never from a task title alone."""
    remote = git(root, 'ls-remote', policy['remote'], f"refs/heads/{policy['branch']}")
    if not remote:
        raise ValueError('Published branch is unavailable')
    tip = remote.split()[0]
    git(root, 'merge-base', '--is-ancestor', tip, 'HEAD')
    commits = git(root, 'rev-list', '--first-parent', '--max-count=10001', tip).splitlines()
    if len(commits) > 10000:
        raise ValueError('Receipt reconstruction exceeds 10000 commits')
    known = {task['id']: task for task in tasks}
    receipts = {}
    for commit in reversed(commits):
        message = git(root, 'show', '-s', '--format=%B', commit)
        match = re.match(r'(IF-\d{3}): ', message)
        packet = re.search(r'^Task-packet: (\S+)$', message, re.M)
        candidate = re.search(r'^Reviewed-candidate: ([a-f0-9]{64})$', message, re.M)
        portable = re.search(r'^Git-candidate: ([a-f0-9]{64})$', message, re.M)
        if not match or not packet or not candidate:
            continue
        identity = match[1]
        if identity not in known or packet[1] != known[identity]['digest'] or identity in receipts:
            raise ValueError(f'Published task contract changed or duplicated: {identity}')
        parent = git(root, 'show', '-s', '--format=%P', commit)
        if len(parent.split()) != 1:
            raise ValueError('Task receipt must describe one non-merge commit')
        if committed_candidate(root, known[identity], parent, commit, git) != (portable or candidate)[1]:
            raise ValueError(f'Published candidate hash differs: {identity}')
        receipts[identity] = dict(task=identity, packet=packet[1], candidate=candidate[1],
                                  baseline=parent, commit=commit, branch=policy['branch'])
    if list(receipts) != [task['id'] for task in tasks[:len(receipts)]]:
        raise ValueError('Published receipts do not form the task prefix')
    return receipts
