"""Real subprocess ownership, including grandchildren that detach into new sessions."""

import os
import fcntl
from pathlib import Path
import subprocess
import signal
import sys
import tempfile
import unittest
import time

from ktask_guardian import invoke


class GuardianTests(unittest.TestCase):
    def test_repeated_cancellation_keeps_lock_until_detached_worker_stops(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            runner = """
import signal, subprocess, sys, time
from pathlib import Path
signal.signal(signal.SIGTERM, signal.SIG_IGN)
worker = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(30)'], start_new_session=True)
Path('worker.pid').write_text(str(worker.pid))
time.sleep(30)
"""
            owner = f"""
import fcntl, sys
from pathlib import Path
sys.path.insert(0, {str(Path(__file__).resolve().parent)!r})
from ktask_guardian import invoke
with open('owner.lock', 'a+') as lock:
    fcntl.flock(lock, fcntl.LOCK_EX)
    invoke([sys.executable, '-c', {runner!r}], Path.cwd(), 30)
"""
            with (root / 'owner.log').open('w') as log:
                process = subprocess.Popen([sys.executable, '-c', owner], cwd=root, stdout=log, stderr=log,
                                           start_new_session=True)
                try:
                    deadline = time.monotonic() + 5
                    while not (root / 'worker.pid').exists() and time.monotonic() < deadline:
                        time.sleep(0.02)
                    self.assertTrue((root / 'worker.pid').exists())
                    process.send_signal(signal.SIGTERM)
                    time.sleep(0.2)
                    process.send_signal(signal.SIGTERM)
                    time.sleep(0.2)
                    with (root / 'owner.lock').open('a+') as lock:
                        with self.assertRaises(BlockingIOError):
                            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
                    self.assertNotEqual(0, process.wait(timeout=10))
                    self.assertFalse(Path('/proc/' + (root / 'worker.pid').read_text()).exists())
                finally:
                    if process.poll() is None:
                        process.terminate()
                        process.wait(timeout=10)

    def test_orphaned_detached_worker_is_reaped_before_return(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            code = """
import subprocess, sys
from pathlib import Path
worker = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(30)'], start_new_session=True)
Path('worker.pid').write_text(str(worker.pid))
"""
            unrelated = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(30)'])
            try:
                self.assertEqual(0, invoke([sys.executable, '-c', code], root, 5))
                self.assertFalse(Path('/proc/' + (root / 'worker.pid').read_text()).exists())
                self.assertIsNone(unrelated.poll())
            finally:
                unrelated.terminate()
                unrelated.wait(timeout=5)

    def test_timeout_forwards_signal_then_reaps_detached_worker(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            code = """
import signal, subprocess, sys, time
from pathlib import Path
def stop(signum, frame):
    Path('forwarded').write_text('TERM')
    sys.exit(0)
signal.signal(signal.SIGTERM, stop)
worker = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(30)'], start_new_session=True)
Path('worker.pid').write_text(str(worker.pid))
while True:
    time.sleep(0.1)
"""
            self.assertEqual(124, invoke([sys.executable, '-c', code], root, 0.5))
            self.assertEqual('TERM', (root / 'forwarded').read_text())
            self.assertFalse(Path('/proc/' + (root / 'worker.pid').read_text()).exists())
