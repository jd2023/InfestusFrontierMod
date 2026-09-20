import json
from pathlib import Path
import signal
import subprocess
import sys
import tempfile
import time
import unittest
from gradle.integration.test_harness import harness, EvidenceCase, alive
from gradle.integration.supervisor import Supervisor


class LifecycleTest(unittest.TestCase):
    def test_interruptions_reap_children_grandchildren_and_port(self):
        for sig in (signal.SIGINT, signal.SIGTERM):
            with self.subTest(signal=sig), tempfile.TemporaryDirectory() as raw:
                output = Path(raw)
                with subprocess.Popen(
                    [
                        sys.executable,
                        str(Path(harness.__file__)),
                        "fixture",
                        "readiness-timeout",
                        raw,
                    ],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                ) as process:
                    deadline = time.monotonic() + 3
                    while (
                        not (output / "fixture-owned.json").exists()
                        and time.monotonic() < deadline
                    ):
                        time.sleep(0.1)
                    self.assertTrue((output / "fixture-owned.json").is_file())
                    process.send_signal(sig)
                    stdout, _ = process.communicate(timeout=5)
                    self.assertNotEqual(0, process.returncode, stdout)
                result = json.loads((output / "result.json").read_text())
                self.assertFalse(result["success"])
                self.assertIn(signal.Signals(sig).name, result["failure"])
                owned = json.loads((output / "fixture-owned.json").read_text())
                self.assertTrue(all(not alive(pid) for pid in owned["pids"]))
                self.assertTrue(harness._port_released(owned["port"]))
                self.assertFalse((output / "success.receipt").exists())
                # The next invocation removes only the now-inactive owned run.
                old_run = output / "build/integration/runs" / result["runId"]
                self.assertEqual(0, harness.run_fixture_scenario("positive", output))
                self.assertFalse(old_run.exists())

    def test_exclusive_ownership_preserves_active_output_and_world(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            with Supervisor(root, "owner", harness.DEFAULT_DEADLINES, 420) as owner:
                sentinel = root / "result.json"
                sentinel.write_text("active result")
                (owner.run_dir / "world").mkdir()
                with self.assertRaisesRegex(
                    harness.HarnessFailure, "another integration scenario"
                ):
                    harness.run_fixture_scenario("positive", root)
                self.assertTrue(
                    sentinel.is_file(), "contending launch removed active evidence"
                )
                self.assertEqual("active result", sentinel.read_text())
                self.assertTrue((owner.run_dir / "world").is_dir())

    def test_absolute_work_deadline_reserves_shared_cleanup(self):
        now = [0.0]
        limits = harness.DEFAULT_DEADLINES | {"shutdown": 3, "cleanup": 2}
        s = Supervisor(
            Path("/unused"),
            "fake",
            limits,
            10,
            clock=lambda: now[0],
            sleep=lambda n: now.__setitem__(0, now[0] + n),
        )
        self.assertEqual(5, s.phase_deadline("readiness", 120))
        with self.assertRaisesRegex(harness.HarnessFailure, "readiness: timeout"):
            s.phase("readiness", 120, lambda: False)
        self.assertAlmostEqual(5, now[0])
        self.assertEqual(5, s.deadline - now[0])
        with self.assertRaisesRegex(harness.HarnessFailure, "scenario deadline"):
            s.phase("join", 30, lambda: True)

    def test_phase_limit_applies_before_overall_deadline(self):
        now = [0.0]
        s = Supervisor(
            Path("/unused"),
            "fake",
            harness.DEFAULT_DEADLINES,
            420,
            clock=lambda: now[0],
            sleep=lambda n: now.__setitem__(0, now[0] + n),
        )
        with self.assertRaisesRegex(harness.HarnessFailure, "readiness: timeout"):
            s.phase("readiness", 120, lambda: False)
        self.assertEqual(120, now[0])

    def test_poll_sleep_never_crosses_deadline_with_negative_duration(self):
        readings = iter((0.0, 0.0, 0.0, 0.0, 0.5, 1.1))
        now = [0.0]

        def advancing_clock():
            now[0] = next(readings, now[0])
            return now[0]

        def nonnegative_sleep(duration):
            if duration < 0:
                raise ValueError("sleep length must be non-negative")

        s = Supervisor(
            Path("/unused"),
            "fake",
            harness.DEFAULT_DEADLINES,
            420,
            clock=advancing_clock,
            sleep=nonnegative_sleep,
        )
        with self.assertRaisesRegex(harness.HarnessFailure, "readiness: timeout"):
            s.phase("readiness", 1, lambda: False)


class EvidenceRegressionTest(EvidenceCase):
    def test_missing_required_metadata_and_evidence(self):
        for field, diagnostic in [
            ("children", "child roles"),
            ("assertions", "assertions"),
            ("phaseDurationsSeconds", "durations"),
            ("artifacts", "artifacts"),
            ("candidateIdentity", "metadata"),
            ("runStartedNs", "start time"),
            ("releaseJarSha256", "JAR digest"),
        ]:
            with self.subTest(field=field):
                result = self.result()
                del result[field]
                with self.assertRaisesRegex(harness.HarnessFailure, diagnostic):
                    harness.ResultEvaluator().evaluate(result)

    def test_evaluator_failure_is_published_only_as_failure(self):
        with tempfile.TemporaryDirectory() as raw:
            result = self.result()
            result["runtimeMods"]["client"]["geckolib"] = "wrong"
            self.assertEqual(1, harness.publish_result(Path(raw), result))
            saved = json.loads((Path(raw) / "result.json").read_text())
            self.assertFalse(saved["success"])
            self.assertIn("geckolib", saved["failure"])

    def test_digests_and_process_roles_are_required(self):
        for change in ("digest", "runtime", "child", "nonzero"):
            with self.subTest(change=change):
                result = self.result()
                if change == "digest":
                    result["artifactDigests"]["server.log"] = "wrong"
                if change == "runtime":
                    del result["runtimeMods"]["client"]
                if change == "child":
                    result["children"][0]["groupReaped"] = False
                if change == "nonzero":
                    result["children"][0]["exitCode"] = 4
                with self.assertRaises(harness.HarnessFailure):
                    harness.ResultEvaluator().evaluate(result)

    def test_zero_exit_loader_refusal_requires_named_diagnostic(self):
        with tempfile.TemporaryDirectory() as raw:
            result = self.result()
            result["command"] = "profileSmoke"
            result["omittedRequired"] = "modonomicon"
            result["runtimeMods"] = {}
            result["children"] = [
                c for c in result["children"] if c["role"] == "server"
            ]
            result["assertions"] = {name: True for name in harness.NEGATIVE_ASSERTIONS}
            result["phaseDurationsSeconds"] = {
                "loaderRefusal": 1,
                "shutdown": 0,
                "cleanup": 0,
                "total": 1,
            }
            log = Path(raw) / "server.log"
            log.write_text(
                "Missing or unsupported mandatory dependencies\nMod ID: 'modonomicon', Requested by: 'infestusfrontier'\nMod loading has failed\n"
            )
            result["artifacts"] = {"server.log": str(log)}
            result["artifactDigests"] = {"server.log": harness._sha256(log)}
            try:
                harness.ResultEvaluator().evaluate(result)
            except harness.HarnessFailure as exc:
                self.fail(
                    f"valid named loader refusal with completed zero exit was rejected: {exc}"
                )
            log.write_text("generic failure: modonomicon infestusfrontier\n")
            result["artifactDigests"]["server.log"] = harness._sha256(log)
            with self.assertRaisesRegex(harness.HarnessFailure, "loader refusal"):
                harness.ResultEvaluator().evaluate(result)
