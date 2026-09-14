"""Linux process-tree ownership for a runner that creates independent child sessions."""

import ctypes
import os
from pathlib import Path
import signal
import subprocess
import sys
import time


def invoke(argv, root, timeout, prompt=None, *, log=None, check=False):
    from ktask_process import run
    return run([sys.executable, str(Path(__file__).resolve()), str(os.getpid()), str(timeout), *argv],
               root, timeout + 40, prompt, log=log, check=check, graceful=True)


def children(pid):
    try:
        with Path(f'/proc/{pid}/task/{pid}/children').open() as stream:
            data = stream.read(65536)
        values = data.split()
        if data and not data[-1].isspace():
            values = values[:-1]
        return [int(value) for value in values[:512]]
    except FileNotFoundError:
        return []


def reap_tree(deadline):
    """Only this guardian's descendants are eligible; pidfds prevent PID reuse races."""
    while children(os.getpid()):
        for pid in children(os.getpid()):
            try:
                descriptor = os.pidfd_open(pid)
            except ProcessLookupError:
                continue
            try:
                fields = Path(f'/proc/{pid}/stat').read_text().rpartition(')')[2].split()
                if int(fields[1]) == os.getpid():
                    signal.pidfd_send_signal(descriptor, signal.SIGKILL)
            except (FileNotFoundError, ProcessLookupError):
                pass
            finally:
                os.close(descriptor)
        for _ in range(512):
            try:
                if os.waitpid(-1, os.WNOHANG)[0] == 0:
                    break
            except ChildProcessError:
                break
        if time.monotonic() > deadline:
            raise RuntimeError('Owned process tree did not stop; inspect the guardian process')
        time.sleep(0.01)


def main():
    libc = ctypes.CDLL(None, use_errno=True)
    if libc.prctl(36, 1, 0, 0, 0) != 0:
        raise OSError(ctypes.get_errno(), 'Cannot establish runner subreaper')
    cancelled = False
    def stop(signum, frame):
        nonlocal cancelled
        cancelled = True
    for sig in (signal.SIGTERM, signal.SIGINT):
        signal.signal(sig, stop)
    if libc.prctl(1, signal.SIGTERM, 0, 0, 0) != 0:
        raise OSError(ctypes.get_errno(), 'Cannot establish parent-death signal')
    parent, timeout, *argv = sys.argv[1:]
    if os.getppid() != int(parent):
        return 130
    child = subprocess.Popen(argv, start_new_session=True)
    deadline = time.monotonic() + float(timeout)
    timed_out = False
    try:
        while child.poll() is None:
            if cancelled or time.monotonic() >= deadline:
                timed_out = not cancelled
                child.send_signal(signal.SIGTERM)
                try:
                    child.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    pass
                break
            time.sleep(0.05)
        return 130 if cancelled else 124 if timed_out else child.returncode
    finally:
        reap_tree(time.monotonic() + 30)


if __name__ == '__main__':
    sys.exit(main())
