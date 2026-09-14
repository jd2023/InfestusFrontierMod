"""Runner-captured command receipts, bound to a task and exact green candidate."""

import hashlib
import json
import time

from ktask_process import run


def digest(path):
    if not path.is_file() or path.stat().st_size > 16 * 1024 * 1024:
        raise ValueError(f'Invalid evidence log: {path}')
    return hashlib.sha256(path.read_bytes()).hexdigest()


def record(folder, binding, candidate, phase, command, root, timeout, artifacts=()):
    if phase not in ('red', 'green', 'game', 'visual', 'integration', 'soak') or not command:
        raise ValueError('Expected red, green, game, integration or soak and a command')
    folder.mkdir(parents=True, exist_ok=True)
    started = time.time_ns()
    log = f'{phase}-{started}.log'
    if len(list(folder.glob('*.log'))) >= 64:
        raise ValueError('Task evidence reached its 64-command limit')
    previous = folder / f'{phase}.json'
    if previous.exists():
        previous.rename(folder / f'{phase}-prior-{started}.json')
    status = run(command, root, timeout, log=folder / log, check=False)
    if (status == 0) != (phase != 'red'):
        raise ValueError(f'{phase} command returned unexpected exit {status}; see {folder / log}')
    produced = {}
    for name in artifacts:
        path = folder / name
        if (not path.resolve().is_relative_to(folder.resolve()) or not path.is_file()
                or path.stat().st_mtime_ns < started):
            raise ValueError(f'Artifact was not produced by this run: {name}')
        produced[name] = digest(path)
    value = dict(binding=binding, candidate=candidate, command=command, exit=status, artifacts=produced,
                 started=started, finished=time.time_ns(), log=log, digest=digest(folder / log))
    temporary = folder / f'{phase}.tmp'
    temporary.write_text(json.dumps(value, indent=2) + '\n')
    temporary.replace(folder / f'{phase}.json')
    print(f'Recorded {phase}: {folder / log}', flush=True)
    return value


def validate_runs(folder, binding, candidate, kinds):
    records, files = {}, []
    for phase in ('red', 'green', *(kind for kind in kinds if kind in ('game', 'visual', 'integration', 'soak'))):
        path = folder / f'{phase}.json'
        record = json.loads(path.read_text())
        if record['binding'] != binding:
            raise ValueError(f'Stale {phase} evidence binding')
        if phase != 'red' and record['candidate'] != candidate:
            raise ValueError(f'Stale {phase} candidate')
        if (record['exit'] == 0) != (phase != 'red') or not record['command']:
            raise ValueError(f'Invalid {phase} command result')
        log = folder / record['log']
        if not log.resolve().is_relative_to(folder.resolve()) or digest(log) != record['digest']:
            raise ValueError(f'Changed {phase} log')
        records[phase] = record
        files.extend([path.name, record['log']])
        if phase == 'visual' and not record.get('artifacts'):
            raise ValueError('Visual execution has no captured artifacts')
        for name, expected in record.get('artifacts', {}).items():
            path = folder / name
            if not path.resolve().is_relative_to(folder.resolve()) or digest(path) != expected:
                raise ValueError(f'Changed {phase} artifact: {name}')
            files.append(name)
    if records['red']['finished'] > records['green']['started']:
        raise ValueError('Invalid red/green execution order')
    return files
