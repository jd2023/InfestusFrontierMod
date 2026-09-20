"""Optional command recorder for task evidence."""

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
