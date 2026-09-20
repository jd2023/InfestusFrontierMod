"""Read ktask-owned progress without treating status markers as implementation."""

import hashlib
import json
import re

QUEUE = '.ktask/tasks.md'
MARKER = re.compile(r'^\[(DONE|FAIL|INPUT)\] (?=IF-\d{3} )', re.M)


def packet_text(text):
    return MARKER.sub('', text)


def queue_digest(root):
    path = root / QUEUE
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.exists() else None


def implementation_paths(root, paths, baseline, git, commit=None):
    """Exclude status-only edits from the implementation diff."""
    paths = set(paths)
    if QUEUE in paths:
        before = git(root, 'show', f'{baseline}:{QUEUE}')
        after = (git(root, 'show', f'{commit}:{QUEUE}') if commit is not None else
                 (root / QUEUE).read_text().strip())
        if packet_text(before) == packet_text(after):
            paths.remove(QUEUE)
    return paths


def accepted_prefix(root, tasks, git):
    receipts = {path.stem: json.loads(path.read_text()) for path in
                (root / '.ktask/session/accepted').glob('*.json')}
    if set(receipts) - {task['id'] for task in tasks}:
        raise ValueError('An accepted task was removed from the plan')
    count = 0
    for index, task in enumerate(tasks):
        receipt = receipts.get(task['id'])
        if receipt:
            if count != index or receipt['packet'] != task['digest']:
                raise ValueError('Accepted prefix changed or a task was skipped')
            git(root, 'merge-base', '--is-ancestor', receipt['commit'], 'HEAD')
            count += 1
        if task.get('status') == 'DONE' and not receipt:
            raise ValueError(f'{task["id"]}: DONE has no accepted delivery receipt')
    return count


def check_dispatch(root, tasks, index, git):
    count = accepted_prefix(root, tasks, git)
    if index > count:
        raise ValueError('Unaccepted dependency or predecessor; ktask cannot skip delivery')
    if any(task.get('status') != 'DONE' for task in tasks[:index]):
        raise ValueError('Earlier native queue tasks are not complete')
    if index < count:
        active = root / '.ktask/session/active.json'
        state = json.loads(active.read_text()) if active.exists() else {}
        if state.get('task') != tasks[index]['id'] or state.get('commit') != git(root, 'rev-parse', 'HEAD'):
            raise ValueError('Accepted task cannot be reimplemented; restore its native DONE marker')
