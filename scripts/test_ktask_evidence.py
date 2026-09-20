"""Optional recorder preserves real results without limiting repair iterations."""

from pathlib import Path
import sys
import tempfile
import unittest

import ktask_evidence as evidence


class EvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.binding = dict(task='IF-001', baseline='base', packet='packet', token='run')

    def record(self, phase, status):
        return evidence.record(self.root, self.binding, 'v1', phase,
                               [sys.executable, '-c', f'raise SystemExit({status})'], self.root, 10)

    def test_records_actual_exit_and_accepts_silent_success(self):
        with self.assertRaisesRegex(ValueError, 'red'):
            self.record('red', 0)
        self.assertEqual(1, self.record('red', 1)['exit'])
        result = self.record('green', 0)
        self.assertEqual(0, result['exit'])
        self.assertEqual(b'', (self.root / result['log']).read_bytes())

    def test_old_logs_do_not_prevent_another_repair_run(self):
        for number in range(64):
            (self.root / f'green-{number}.log').write_text('old result')
        self.assertEqual(0, self.record('green', 0)['exit'])

    def test_failed_rerun_preserves_history_but_not_current_success(self):
        self.record('green', 0)
        with self.assertRaises(ValueError):
            self.record('green', 1)
        self.assertFalse((self.root / 'green.json').exists())
        self.assertEqual(1, len(list(self.root.glob('green-prior-*.json'))))

    def test_attached_capture_must_be_produced_by_command(self):
        image = self.root / 'capture.png'
        image.write_bytes(b'old capture')
        with self.assertRaisesRegex(ValueError, 'not produced'):
            evidence.record(self.root, self.binding, 'v1', 'visual',
                            [sys.executable, '-c', 'pass'], self.root, 10, ['capture.png'])
        result = evidence.record(self.root, self.binding, 'v1', 'visual',
            [sys.executable, '-c', 'from pathlib import Path; Path("capture.png").write_bytes(b"new capture")'],
            self.root, 10, ['capture.png'])
        self.assertEqual(evidence.digest(image), result['artifacts']['capture.png'])
