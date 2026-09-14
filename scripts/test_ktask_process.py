"""Exercise real process groups and bounded output without launching models."""

import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

from ktask_process import run


class ProcessTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

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
        with patch.dict(os.environ, {}, clear=False):
            os.environ.pop('INFESTUS_TASK_GROUP', None)
            with self.assertRaises(subprocess.TimeoutExpired):
                run([sys.executable, '-c', program], self.root, 2, log=self.root / 'nested.log')
        pid, group = (self.root / 'child').read_text().split()
        self.assertEqual(group, (self.root / 'parent').read_text())
        status = Path(f'/proc/{pid}/stat')
        if status.exists():
            self.assertEqual('Z', status.read_text().split()[2], 'Descendant must not remain running')
