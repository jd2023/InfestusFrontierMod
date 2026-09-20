"""Exercise real process groups and bounded output without launching models."""

import os
from pathlib import Path
import select
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch, Mock

from ktask_process import run


class ProcessTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def test_silent_child_does_not_emit_wrapper_heartbeat(self):
        child = Mock(returncode=0, stdin=None, stdout=None)
        child.poll.side_effect = [None, 0]
        with patch('ktask_process.subprocess.Popen', return_value=child), \
                patch('ktask_process.time.monotonic', side_effect=range(0, 1000, 31)), \
                patch('builtins.print') as output:
            run(['fake-child'], self.root, 1000)
        output.assert_not_called()

    def test_noisy_child_cannot_write_past_capture_limit(self):
        log = self.root / 'noise.log'
        scripts = str(Path(__file__).resolve().parent)
        payload = 'import os; os.write(1, b"x" * (20 * 1024 * 1024))'
        program = (f'import sys; sys.path.insert(0,{scripts!r}); from ktask_process import run; '
                   f'from pathlib import Path; run([sys.executable,"-c",{payload!r}],'
                   f'Path.cwd(),10,log=Path("noise.log"))')
        result = subprocess.run([sys.executable, '-c', program], cwd=self.root,
                                env=dict(os.environ, INFESTUS_TASK_GROUP='1'),
                                start_new_session=True, capture_output=True, timeout=15)
        self.assertEqual(-9, result.returncode)
        self.assertEqual(16 * 1024 * 1024, log.stat().st_size)

    def test_nested_recorder_remains_in_ancestor_kill_group(self):
        scripts = str(Path(__file__).resolve().parent)
        payload = 'import os,time; from pathlib import Path; Path("child").write_text(str(os.getpid())+" "+str(os.getpgrp())); time.sleep(30)'
        program = (f'import os,sys; sys.path.insert(0,{scripts!r}); from ktask_process import run; '
                   f'from pathlib import Path; Path("parent").write_text(str(os.getpgrp())); '
                   f'run([sys.executable,"-c",{payload!r}],Path.cwd(),30)')
        launcher = (f'import sys; sys.path.insert(0,{scripts!r}); from ktask_process import run; '
                    f'from pathlib import Path; run([sys.executable,"-c",{program!r}],'
                    f'Path.cwd(),2,log=Path("nested.log"))')
        result = subprocess.run([sys.executable, '-c', launcher], cwd=self.root,
                                env=dict(os.environ, INFESTUS_TASK_GROUP='1'),
                                start_new_session=True, capture_output=True, timeout=15)
        self.assertEqual(-9, result.returncode)
        pid, group = (self.root / 'child').read_text().split()
        self.assertEqual(group, (self.root / 'parent').read_text())
        self.assert_descendant_stopped(int(pid))

    def assert_descendant_stopped(self, pid):
        try:
            descriptor = os.pidfd_open(pid)
        except ProcessLookupError:
            return
        try:
            poller = select.poll()
            poller.register(descriptor, select.POLLIN)
            self.assertTrue(any(events & select.POLLIN for _, events in poller.poll(2000)),
                            'Descendant must not remain running')
        finally:
            os.close(descriptor)

    def test_already_reaped_descendant_is_stopped(self):
        with patch('os.pidfd_open', side_effect=ProcessLookupError), patch('select.poll') as poller:
            self.assert_descendant_stopped(123)
        poller.assert_not_called()

    def test_live_process_is_not_accepted(self):
        with self.assertRaisesRegex(AssertionError, 'Descendant must not remain running'):
            self.assert_descendant_stopped(os.getpid())

    def test_exit_notification_required_and_descriptor_always_closed(self):
        for events in ([(99, select.POLLIN)], [(99, select.POLLIN | select.POLLHUP)],
                       [], [(99, select.POLLERR)], [(99, select.POLLNVAL)]):
            with self.subTest(events=events), patch('os.pidfd_open', return_value=99), \
                    patch('select.poll') as poll, patch('os.close') as close:
                poll.return_value.poll.return_value = events
                if events and events[0][1] & select.POLLIN:
                    self.assert_descendant_stopped(123)
                else:
                    with self.assertRaisesRegex(AssertionError, 'Descendant must not remain running'):
                        self.assert_descendant_stopped(123)
                poll.return_value.register.assert_called_once_with(99, select.POLLIN)
                poll.return_value.poll.assert_called_once_with(2000)
                close.assert_called_once_with(99)

    def test_unrelated_process_observation_errors_propagate(self):
        for error in (PermissionError, OSError):
            with self.subTest(error=error.__name__), patch('os.pidfd_open', side_effect=error):
                with self.assertRaises(error):
                    self.assert_descendant_stopped(123)
        with patch('os.pidfd_open', return_value=99), patch('select.poll') as poll, \
                patch('os.close') as close:
            poll.return_value.poll.side_effect = OSError('poll failed')
            with self.assertRaises(OSError):
                self.assert_descendant_stopped(123)
            close.assert_called_once_with(99)
