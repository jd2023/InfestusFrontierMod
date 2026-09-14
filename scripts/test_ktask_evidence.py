"""Execution receipts must describe real commands and the current candidate."""

import json
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

    def record(self, phase, status, candidate='v1'):
        return evidence.record(self.root, self.binding, candidate, phase,
                               [sys.executable, '-c', f'print("assertion"); raise SystemExit({status})'],
                               self.root, 10)

    def test_records_actual_exit_and_rejects_wrong_phase(self):
        with self.assertRaisesRegex(ValueError, 'red'):
            self.record('red', 0)
        self.record('red', 1)
        self.record('green', 0)
        evidence.validate_runs(self.root, self.binding, 'v1', ['rules'])

    def test_old_candidate_cannot_supply_green_or_soak(self):
        self.record('red', 1)
        self.record('soak', 0)
        self.record('green', 0, 'v2')
        with self.assertRaisesRegex(ValueError, 'soak'):
            evidence.validate_runs(self.root, self.binding, 'v2', ['rules', 'soak'])
        with self.assertRaisesRegex(ValueError, 'green'):
            evidence.validate_runs(self.root, self.binding, 'v3', ['rules'])

    def test_failed_rerun_cannot_reuse_previous_success(self):
        self.record('red', 1)
        self.record('green', 0)
        with self.assertRaises(ValueError):
            self.record('green', 1)
        with self.assertRaises(FileNotFoundError):
            evidence.validate_runs(self.root, self.binding, 'v1', ['rules'])

    def test_stale_packet_log_tampering_and_green_before_red_fail(self):
        self.record('green', 0)
        self.record('red', 1)
        with self.assertRaisesRegex(ValueError, 'order'):
            evidence.validate_runs(self.root, self.binding, 'v1', ['rules'])
        self.record('green', 0)
        with self.assertRaisesRegex(ValueError, 'binding'):
            evidence.validate_runs(self.root, dict(self.binding, packet='new'), 'v1', ['rules'])
        receipt = json.loads((self.root / 'green.json').read_text())
        (self.root / receipt['log']).write_text('tampered')
        with self.assertRaisesRegex(ValueError, 'log'):
            evidence.validate_runs(self.root, self.binding, 'v1', ['rules'])

    def test_visual_files_must_be_new_and_unchanged(self):
        self.record('red', 1)
        self.record('green', 0)
        image = self.root / 'capture.png'
        image.write_bytes(b'old capture')
        with self.assertRaisesRegex(ValueError, 'not produced'):
            evidence.record(self.root, self.binding, 'v1', 'visual',
                            [sys.executable, '-c', 'print("no capture")'], self.root, 10, ['capture.png'])
        evidence.record(self.root, self.binding, 'v1', 'visual',
                        [sys.executable, '-c', 'from pathlib import Path; Path("capture.png").write_bytes(b"new capture")'],
                        self.root, 10, ['capture.png'])
        evidence.validate_runs(self.root, self.binding, 'v1', ['rules', 'visual'])
        image.write_bytes(b'changed capture')
        with self.assertRaisesRegex(ValueError, 'artifact'):
            evidence.validate_runs(self.root, self.binding, 'v1', ['rules', 'visual'])
