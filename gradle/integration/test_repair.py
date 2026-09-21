"""Regressions for the independent review's observed acceptance failures."""

import struct
import sys
import tempfile
import tomllib
import unittest
from pathlib import Path
from gradle.integration.test_harness import harness, EvidenceCase


class DisposableClientConfigurationTest(unittest.TestCase):
    def test_both_disposable_clients_disable_only_the_racy_loader_splash(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            for role in ("client", "observer"):
                directory = root / role
                directory.mkdir()
                harness._prepare_client(directory)
                config = tomllib.loads((directory / "config/fml.toml").read_text())
                self.assertEqual({"earlyWindowControl": False}, config)
                self.assertIn("renderDistance:4", (directory / "options.txt").read_text())
            self.assertFalse((root / "config").exists())


class AcceptanceRegressionTest(EvidenceCase):
    def test_missing_assertions_rejected(self):
        result = self.result()
        result["assertions"] = {}
        with self.assertRaisesRegex(harness.HarnessFailure, "assertions"):
            harness.ResultEvaluator().evaluate(result)

    def test_missing_children_rejected(self):
        result = self.result()
        result["children"] = []
        with self.assertRaisesRegex(harness.HarnessFailure, "child roles"):
            harness.ResultEvaluator().evaluate(result)

    def test_unknown_child_exit_rejected(self):
        result = self.result()
        result["children"][0]["exitCode"] = None
        with self.assertRaisesRegex(harness.HarnessFailure, "exit/reaping"):
            harness.ResultEvaluator().evaluate(result)

    def test_truncated_png_rejected(self):
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "capture.png"
            path.write_bytes(
                b"\x89PNG\r\n\x1a\n" + b"\0" * 8 + struct.pack(">II", 1280, 720)
            )
            with self.assertRaises(harness.HarnessFailure):
                harness._png_dimensions(path)

    def test_newline_free_output_overflow_rejected(self):
        self.assert_overflow("import sys; sys.stdout.write('x' * (9 * 1024 * 1024))")

    def test_output_overflow_after_success_marker_rejected(self):
        self.assert_overflow(
            "import sys; print('SUCCESS', flush=True); sys.stdout.write('x' * (9 * 1024 * 1024))"
        )

    def assert_overflow(self, program):
        with tempfile.TemporaryDirectory() as raw:
            supervisor = harness.Supervisor(
                Path(raw), "overflow", harness.DEFAULT_DEADLINES, 420
            )
            with self.assertRaisesRegex(harness.HarnessFailure, "overflow"):
                with supervisor:
                    child = supervisor.start(
                        "server",
                        [sys.executable, "-c", program],
                        Path(raw),
                        Path(raw) / "output.log",
                    )
                    supervisor.phase("shutdown", 2, child.finished)
            self.assertTrue(supervisor.cleanup_ok)

    def test_positive_fixture_requires_actual_png_files(self):
        with tempfile.TemporaryDirectory() as raw:
            output = Path(raw)
            self.assertEqual(0, harness.run_fixture_scenario("positive", output))
            for name in ("bootstrap-title.png", "bootstrap-world.png"):
                self.assertTrue((output / name).is_file(), name)
