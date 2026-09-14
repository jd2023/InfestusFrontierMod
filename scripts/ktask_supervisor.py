"""Plan readiness and bounded coordinator recovery; ktask still executes workers."""

import argparse
import fcntl
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import tomllib

import ktask_workflow as flow
import ktask_delivery as delivery
from ktask_timeouts import budgets
from ktask_contracts import check_review
from ktask_process import codex_args
from ktask_guardian import invoke as run_runner

PLAN_FILES = ('.ktask/tasks.md', '.ktask/content-plan.json', 'docs/ARCHITECTURE.md', 'docs/TASK_TEMPLATE.md',
              'docs/BLOCK_CATALOG.md', 'docs/ITEM_CATALOG.md', 'docs/ARMOR_EVOLUTION.md',
              'docs/LIVING_SUBSTRATE_MUTATIONS.md', 'docs/GUIDE_PROGRESSION_TREE.md',
              'docs/PROGRESSION_MAP.md')


class PlanRejected(ValueError):
    """A planning candidate can be corrected without discarding its parked implementation."""


class ExternalPause(ValueError):
    """Missing execution/access is not a rejected implementation or planning candidate."""


def accepted_prefix(root, tasks):
    receipts = {path.stem: json.loads(path.read_text()) for path in
                (root / '.ktask/session/accepted').glob('*.json')}
    if set(receipts) - {task['id'] for task in tasks}:
        raise ValueError('An accepted task was removed from the plan')
    count = 0
    for task in tasks:
        receipt = receipts.get(task['id'])
        if not receipt:
            continue
        if count != tasks.index(task) or receipt['packet'] != task['digest']:
            raise ValueError('Accepted prefix changed or a task was skipped')
        flow.git(root, 'merge-base', '--is-ancestor', receipt['commit'], 'HEAD')
        count += 1
    return count


def reconcile(root, tasks, policy):
    """Regenerate only runtime markers; accepted task contracts and ancestry are immutable."""
    if flow.git(root, 'branch', '--show-current') != policy['branch']:
        raise ValueError('Wrong delivery branch')
    count = accepted_prefix(root, tasks)
    if count < len(tasks) and flow.git(root, 'log', '-1', '--format=%H', '--fixed-strings',
                                     '--grep=Task-packet: ' + tasks[count]['digest']):
        raise ValueError('Task delivery exists without its receipt; run restore-receipts before dispatch')
    selected = tasks[:count + 1]
    runtime = root / '.ktask/session/.ktask'
    runtime.mkdir(parents=True, exist_ok=True)
    text = '\n\n---\n\n'.join(('[DONE] ' if i < count else '') + task['body']
                               for i, task in enumerate(selected)) + '\n'
    (runtime / 'tasks.md').write_text(text)
    for name in ('context.md', 'prompt.md', 'autoresolve.md'):
        shutil.copyfile(root / '.ktask' / name, runtime / name)
    config = (root / '.ktask/config.toml').read_text()
    config = re.sub(r'^project_dir = .*$', 'project_dir = ' + json.dumps(str(root)),
                    config, count=1, flags=re.M)
    (runtime / 'config.toml').write_text(config)
    flow.save(root / '.ktask/session/plan.json',
              dict(digest=flow.plan_digest(root), task_ids=[task['id'] for task in selected]))
    return count


def park(root, task, state, policy):
    """Shelve only the failed candidate's paths. Keep its stash as a recovery copy."""
    folder = root / '.ktask/session/parked'
    folder.mkdir(parents=True, exist_ok=True)
    record = folder / (task['id'] + '.json')
    if record.exists():
        raise ValueError('An unfinished recovery already exists for this task')
    paths = set(flow.git(root, 'diff', '--no-renames', '--name-only', '-z', state['baseline']).split('\0')) - {''}
    paths |= flow.untracked(root) - state['protected'].keys()
    if (flow.git(root, 'rev-parse', 'HEAD') != state['baseline']
            or flow.git(root, 'branch', '--show-current') != policy['branch']
            or flow.file_digest(root, state['protected']) != state['protected']):
        raise ValueError('Cannot park work after an unrelated checkpoint/file change')
    if any(path.startswith(('.ktask/', 'scripts/ktask_', 'scripts/test_ktask_')) for path in paths):
        raise ValueError('Process-control edits require explicit coordinator recovery; no files discarded')
    saved = dict(state=state, stash=None, phase='parking', paths=flow.file_digest(root, paths))
    flow.save(record, saved)
    if paths:
        flow.git(root, 'stash', 'push', '--include-untracked', '-m', 'ktask-' + state['token'], '--', *sorted(paths))
        saved['stash'] = flow.git(root, 'rev-parse', 'refs/stash')
    saved['phase'] = 'parked'
    flow.save(record, saved)
    active = root / '.ktask/session/active.json'
    if active.exists():
        active.replace(folder / (task['id'] + '-prior-active.json'))


def restore(root, task, policy):
    record = root / '.ktask/session/parked' / (task['id'] + '.json')
    if not record.exists():
        return
    saved = json.loads(record.read_text())
    if saved['phase'] != 'parked':
        raise ValueError(f'Interrupted recovery: inspect {record}; stash and candidate are retained')
    state = flow.begin(root, task, policy)
    saved['phase'] = 'restoring'
    flow.save(record, saved)
    flow.save(root / '.ktask/session/active.json', state)
    if saved['stash']:
        flow.git(root, 'stash', 'apply', saved['stash'])
    record.replace(record.with_name(task['id'] + '-restored-' + state['token'] + '.json'))


def decision(root, task, policy, role, extra=''):
    folder = root / '.ktask/session/planning' / task['id']
    folder.mkdir(parents=True, exist_ok=True)
    stem = f'{role}-{time.time_ns()}'
    schema, output = folder / (stem + '-schema.json'), folder / (stem + '.json')
    flow.save(schema, dict(type='object', additionalProperties=False, required=['status', 'reason'],
              properties=dict(status=dict(type='string', enum=['ready', 'revised', 'retry', 'external']),
                              reason=dict(type='string'))))
    prompt = (root / f'.ktask/{role}.md').read_text() + '\n' + task['body'] + '\n' + extra
    try:
        run_runner([*codex_args(policy['coordinator_model'], policy['coordinator_effort'],
                        writable=role == 'coordinator', cache=policy.get('gradle_cache')),
             '--output-schema', str(schema), '-o', str(output), '-'], root, 2400, prompt,
            log=folder / (stem + '.log'), check=True)
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        raise ExternalPause(str(error)) from error
    result = json.loads(output.read_text())
    if result['status'] not in ('ready', 'revised', 'retry', 'external') or not result['reason'].strip():
        raise ValueError('Invalid coordinator decision')
    return result


def coordinate(root, task, policy, reason):
    state_path = root / '.ktask/session/active.json'
    parked = root / '.ktask/session/parked' / (task['id'] + '.json')
    if parked.exists():
        saved = json.loads(parked.read_text())
        if saved['phase'] != 'parked':
            raise ValueError('Interrupted parking/restoration requires recovery')
        state = saved['state']
    else:
        state = json.loads(state_path.read_text()) if state_path.exists() else flow.begin(root, task, policy)
        if state.get('commit') or (root / '.ktask/session/delivery-intent.json').exists():
            flow.accept(root, task, state, policy)
            return
        park(root, task, state, policy)
    baseline = flow.git(root, 'rev-parse', 'HEAD')
    protected = state['protected']
    if baseline != state['baseline']:
        raise ValueError('Parked baseline changed outside planning delivery')
    result = decision(root, task, policy, 'coordinator',
                      f'Failure: {reason}\nFailed work: .ktask/session/parked/{task["id"]}.json\n'
                      f'Allowed planning paths: {json.dumps(PLAN_FILES)}')
    if flow.git(root, 'rev-parse', 'HEAD') != baseline or flow.file_digest(root, protected) != protected:
        raise ValueError('Coordinator changed the checkpoint or unrelated files')
    changed = set(flow.git(root, 'diff', '--name-only', '-z', baseline).split('\0')) - {''}
    changed |= flow.untracked(root) - protected.keys()
    if not changed <= set(PLAN_FILES):
        raise ValueError('Coordinator exceeded planning scope: ' + str(changed - set(PLAN_FILES)))
    if result['status'] == 'external':
        raise ExternalPause(result['reason'])
    try:
        tasks, _ = flow.load_project(root)
        flow.validate(root, tasks)
        accepted_prefix(root, tasks)
    except ValueError as error:
        raise PlanRejected(str(error)) from error
    if task['id'] not in {entry['id'] for entry in tasks}:
        raise ValueError('Coordinator must retain the blocked task identity')
    if changed:
        plan_task = dict(task, digest='plan-' + flow.plan_digest(root), scope=list(PLAN_FILES),
                         body='PLAN-ONLY CORRECTION. Preserve all gameplay obligations.\nOriginal task:\n' + task['body'] +
                              '\nCoordinator diagnosis:\n' + result['reason'])
        plan_state = dict(state, baseline=baseline, packet=plan_task['digest'], protected=protected)
        _, candidate = flow.candidate_state(root, plan_task, plan_state, policy, allowed_controls=PLAN_FILES)
        try:
            flow.run_gate(root)
            verdict = flow.review_candidate(root, plan_task, plan_state, candidate, policy)
            flow.save(root / '.ktask/session/planning' / task['id'] / 'last-review.json', verdict)
            check_review(verdict, task['id'], candidate)
        except flow.ReviewUnavailable as error:
            raise ExternalPause(str(error)) from error
        except (ValueError, subprocess.SubprocessError) as error:
            raise PlanRejected(str(error) + '; inspect planning last-review.json and gate output') from error
        if flow.candidate_state(root, plan_task, plan_state, policy, allowed_controls=PLAN_FILES)[1] != candidate:
            raise ValueError('Plan changed during review')
        flow.git(root, 'add', '--', *sorted(changed))
        intent_path = root / '.ktask/session/planning/commit-intent.json'
        intent = delivery.prepare(root, intent_path, dict(plan_state, candidate=candidate),
                                  f'Clarify {task["id"]} implementation contract\n\n{result["reason"]}', flow.git, flow.save)
        commit = delivery.finish(root, intent, flow.git)['commit']
        pending_delivery = root / '.ktask/session/planning/pending-delivery.json'
        flow.save(pending_delivery, dict(commit=commit))
        phase = root / '.ktask/session/planning/active.json'
        if phase.exists():
            saved = json.loads(phase.read_text())
            flow.save(phase, dict(saved, accepted_commit=commit))
        flow.publish(root, policy, commit)
        pending_delivery.replace(pending_delivery.with_name('delivered-' + str(time.time_ns()) + '.json'))
        intent_path.unlink()
    folder = root / '.ktask/session/planning' / task['id']
    folder.mkdir(parents=True, exist_ok=True)
    (folder / 'reason.txt').write_text(result['reason'] + '\n')


def recover(root, task, policy, reason):
    """Persist the planning phase before model execution so interrupted edits can resume."""
    phase = root / '.ktask/session/planning/active.json'
    if phase.exists():
        saved = json.loads(phase.read_text())
        if saved.get('accepted_commit'):
            if not flow.tracked_clean(root):
                raise ValueError('Accepted plan has unexpected edits')
            flow.publish(root, policy, saved['accepted_commit'])
            phase.replace(phase.with_name('completed-' + str(time.time_ns()) + '.json'))
            return
        task, reason = saved['task'], saved['reason']
    else:
        flow.save(phase, dict(task=task, reason=reason))
    attempts = root / '.ktask/session/planning' / task['id'] / 'attempts.json'
    used = json.loads(attempts.read_text())['used'] if attempts.exists() else 0
    while used < policy['coordinator_attempts']:
        used += 1
        flow.save(attempts, dict(used=used))
        flow.save(phase, dict(task=task, reason=reason))
        try:
            coordinate(root, task, policy, reason)
            phase.replace(phase.with_name('completed-' + str(time.time_ns()) + '.json'))
            return
        except PlanRejected as error:
            reason = str(error)
            flow.save(phase, dict(task=task, reason=reason))
        except ExternalPause:
            flow.save(attempts, dict(used=used - 1))
            raise
    raise ValueError(f'{task["id"]}: bounded coordinator attempts exhausted; all work retained')


def supervise(root, policy, through=None):
    intent_path = root / '.ktask/session/planning/commit-intent.json'
    if intent_path.exists():
        intent = json.loads(intent_path.read_text())
        if flow.file_digest(root, intent['state']['protected']) != intent['state']['protected']:
            raise ValueError('Unrelated files changed during plan delivery')
        commit = delivery.finish(root, intent, flow.git)['commit']
        flow.publish(root, policy, commit)
        phase = root / '.ktask/session/planning/active.json'
        if phase.exists():
            flow.save(phase, dict(json.loads(phase.read_text()), accepted_commit=commit))
        intent_path.unlink()
    pending_delivery = root / '.ktask/session/planning/pending-delivery.json'
    if pending_delivery.exists():
        if not flow.tracked_clean(root):
            raise ValueError('Pending plan delivery has new edits')
        flow.publish(root, policy, json.loads(pending_delivery.read_text())['commit'])
        pending_delivery.replace(pending_delivery.with_name('delivered-' + str(time.time_ns()) + '.json'))
    phase = root / '.ktask/session/planning/active.json'
    if phase.exists():
        saved = json.loads(phase.read_text())
        recover(root, saved['task'], policy, saved['reason'])
    while True:
        tasks, policy = flow.load_project(root)
        flow.validate(root, tasks)
        pending = root / '.ktask/session/delivery-intent.json'
        if pending.exists():
            saved = json.loads(pending.read_text())['state']
            pending_task = next(task for task in tasks if task['id'] == saved['task'])
            flow.accept(root, pending_task, saved, policy)
        active_path = root / '.ktask/session/active.json'
        if active_path.exists():
            saved = json.loads(active_path.read_text())
            if saved.get('commit') and not (root / '.ktask/session/accepted' / (saved['task'] + '.json')).exists():
                pending_task = next(task for task in tasks if task['id'] == saved['task'])
                flow.accept(root, pending_task, saved, policy)
        if through and through not in {task['id'] for task in tasks}:
            raise ValueError('Unknown --through task')
        count = reconcile(root, tasks, policy)
        if count == len(tasks) or (through and any(task['id'] == through for task in tasks[:count])):
            return 0
        task = tasks[count]
        restore(root, task, policy)
        active = root / '.ktask/session/active.json'
        state = json.loads(active.read_text()) if active.exists() else None
        if not state or state['task'] != task['id']:
            flow.save(active, flow.begin(root, task, policy))
        assessment = decision(root, task, policy, 'readiness')
        if assessment['status'] == 'ready':
            config = tomllib.loads((root / '.ktask/config.toml').read_text())
            code = run_runner([policy['runner'], 'run', '--retry-failed'], root / '.ktask/session',
                              budgets(task['id'], config, policy)[1])
            if code == 0:
                if accepted_prefix(root, tasks) <= count:
                    raise ValueError('Runner exited without an accepted receipt')
                continue
            if code != 1:
                return code
            reason = 'Implementation/repair failed; inspect session runner logs and last-review.json.'
        elif assessment['status'] == 'external':
            print('External pause: ' + assessment['reason'])
            return 5
        else:
            reason = assessment['reason']
        recover(root, task, policy, reason)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['run', 'resume', 'retry', 'status', 'reconcile', 'restore-receipts'])
    parser.add_argument('--through')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    policy = tomllib.loads((root / '.ktask/policy.toml').read_text())
    if args.command == 'status':
        tasks, _ = flow.load_project(root)
        print(f'{accepted_prefix(root, tasks)}/{len(tasks)} accepted tasks')
        return 0
    with (root / '.ktask/supervisor.lock').open('a+') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        if args.command == 'restore-receipts':
            if not flow.tracked_clean(root) or flow.git(root, 'branch', '--show-current') != policy['branch']:
                raise ValueError('Receipt restoration requires a clean tracked checkpoint')
            tasks, _ = flow.load_project(root)
            receipts = delivery.published_receipts(root, tasks, policy, flow.git)
            folder = root / '.ktask/session/accepted'
            for identity, receipt in receipts.items():
                path = folder / (identity + '.json')
                if path.exists():
                    existing = json.loads(path.read_text())
                    if any(existing.get(key) != receipt[key] for key in ('task', 'packet', 'commit')):
                        raise ValueError('Existing receipt disagrees with published history')
                else:
                    flow.save(path, receipt)
            print(f'{len(receipts)} published task receipts verified')
            return 0
        if args.command == 'reconcile':
            if not flow.tracked_clean(root):
                raise ValueError('Commit coordinator plan changes before explicit reconciliation')
            tasks, _ = flow.load_project(root)
            reconcile(root, tasks, policy)
            return 0
        return supervise(root, policy, args.through)


if __name__ == '__main__':
    try:
        sys.exit(main())
    except ExternalPause as error:
        print(f'External pause: {error}', file=sys.stderr)
        sys.exit(5)
    except KeyboardInterrupt:
        sys.exit(130)
    except (ValueError, OSError, subprocess.SubprocessError) as error:
        print(f'Coordinator stopped safely: {error}', file=sys.stderr)
        sys.exit(1)
