"""Bounded subprocess execution shared by delivery and evidence capture."""

import os
from pathlib import Path
import signal
import selectors
import subprocess
import time


def run(argv, root, timeout, prompt=None, *, log=None, check=True):
    """Keep descendants in the runner's kill group; cap each captured log at 16 MiB."""
    runner_group = os.getpgrp() == os.getpid() or bool(os.environ.get('INFESTUS_TASK_GROUP'))
    output = Path(log).open('wb', buffering=0) if log else None
    child = subprocess.Popen(argv, cwd=root, stdin=subprocess.PIPE if prompt is not None else None,
                             stdout=subprocess.PIPE if output else None,
                             stderr=subprocess.STDOUT if output else None,
                             env=dict(os.environ, INFESTUS_TASK_GROUP='1'),
                             start_new_session=not runner_group)
    group = os.getpgrp() if runner_group else child.pid
    def stop(signum, frame):
        os.killpg(group, signal.SIGKILL)
        raise KeyboardInterrupt
    previous = {sig: signal.signal(sig, stop) for sig in (signal.SIGTERM, signal.SIGINT)}
    deadline, heartbeat = time.monotonic() + timeout, time.monotonic() + 30
    selector = selectors.DefaultSelector()
    pending = memoryview(prompt.encode()) if prompt is not None else None
    if child.stdin:
        os.set_blocking(child.stdin.fileno(), False)
        selector.register(child.stdin, selectors.EVENT_WRITE)
    if output:
        selector.register(child.stdout, selectors.EVENT_READ)
    written = 0
    try:
        while True:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                raise subprocess.TimeoutExpired(argv, timeout)
            for key, _ in selector.select(min(0.1, remaining)):
                if key.fileobj == child.stdin:
                    try:
                        pending = pending[os.write(child.stdin.fileno(), pending[:65536]):]
                    except BrokenPipeError:
                        pending = b''
                    if not pending:
                        selector.unregister(child.stdin)
                        child.stdin.close()
                else:
                    chunk = os.read(child.stdout.fileno(), 65536)
                    if not chunk:
                        selector.unregister(child.stdout)
                        child.stdout.close()
                    else:
                        available = 16 * 1024 * 1024 - written
                        output.write(chunk[:available])
                        written += min(len(chunk), available)
                        if len(chunk) > available:
                            raise ValueError('Captured log exceeded 16 MiB')
            if child.poll() is not None and not selector.get_map():
                break
            if time.monotonic() >= heartbeat:
                print('Task process still running within its deadline.', flush=True)
                heartbeat = time.monotonic() + 30
    except (subprocess.TimeoutExpired, KeyboardInterrupt, ValueError):
        try:
            os.killpg(group, signal.SIGKILL)
        except ProcessLookupError:
            pass
        child.wait()
        raise
    finally:
        for sig, handler in previous.items():
            signal.signal(sig, handler)
        if output:
            output.close()
        for pipe in (child.stdin, child.stdout):
            if pipe and not pipe.closed:
                pipe.close()
        selector.close()
    if check and child.returncode:
        raise ValueError(f'Command failed ({child.returncode}): {argv}')
    return child.returncode


def codex_args(model, effort, *, writable=False, cache=None):
    """Explicit automation policy; credentials still come from the existing Codex login."""
    args = ['codex', 'exec', '--ephemeral', '--ignore-user-config', '--sandbox',
            'workspace-write' if writable else 'read-only', '-c', 'approval_policy="never"',
            '--model', model, '-c', f'model_reasoning_effort="{effort}"']
    if writable:
        args += ['-c', 'sandbox_workspace_write.network_access=true']
        if cache:
            args += ['--add-dir', str(cache)]
    return args
