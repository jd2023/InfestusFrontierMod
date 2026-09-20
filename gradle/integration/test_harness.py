#!/usr/bin/env python3
from __future__ import annotations

import copy
import json
import os
import tempfile
import unittest
from pathlib import Path
from gradle.integration import harness


class EvidenceCase(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temporary = tempfile.TemporaryDirectory()
        cls.output = Path(cls.temporary.name)
        if harness.run_fixture_scenario("positive", cls.output) != 0:
            raise AssertionError((cls.output / "result.json").read_text())
        cls.valid_result = json.loads((cls.output / "result.json").read_text())

    @classmethod
    def tearDownClass(cls):
        cls.temporary.cleanup()

    def result(self):
        return copy.deepcopy(self.valid_result)


def alive(pid: int) -> bool:
    try:
        os.kill(pid, 0)
        return True
    except ProcessLookupError:
        return False


class EvaluatorTest(EvidenceCase):
    def test_profile_file_must_match_bootstrap_contract_exactly(self):
        with tempfile.TemporaryDirectory() as raw:
            profile = Path(raw) / "profiles.json"
            profile.write_text('{"schema": 2}', encoding="utf-8")
            with self.assertRaisesRegex(harness.HarnessFailure, "schema 1 bootstrap"):
                harness.validate_profile_file(profile, "required", "bootstrap")

    def test_invalid_profile_is_rejected_before_launch(self):
        with self.assertRaisesRegex(harness.HarnessFailure, "invalid profile"):
            harness.validate_profile_file(None, "everything", "bootstrap")

    def test_wrong_runtime_version_is_rejected_at_named_mod(self):
        result = self.result()
        result["runtimeMods"]["server"]["geckolib"] = "4.9.1"
        with self.assertRaisesRegex(harness.HarnessFailure, "geckolib.*4.9.2"):
            harness.ResultEvaluator().evaluate(result)

    def test_stale_or_absent_capture_is_rejected(self):
        for mutation in ("missing", "stale"):
            with self.subTest(mutation=mutation):
                result = self.result()
                if mutation == "missing":
                    del result["artifacts"]["bootstrap-title.png"]
                else:
                    result["runStartedNs"] = (
                        self.output.joinpath("bootstrap-title.png").stat().st_mtime_ns
                        + 1
                    )
                with self.assertRaisesRegex(harness.HarnessFailure, "capture"):
                    harness.ResultEvaluator().evaluate(result, require_captures=True)

    def test_valid_evidence_is_accepted(self):
        harness.ResultEvaluator().evaluate(self.result())


class SupervisorFixtureTest(unittest.TestCase):
    def assert_failure_cleans_children(self, scenario: str, phase: str):
        with tempfile.TemporaryDirectory() as raw:
            output = Path(raw)
            self.assertNotEqual(0, harness.run_fixture_scenario(scenario, output))
            result = json.loads((output / "result.json").read_text(encoding="utf-8"))
            self.assertFalse(result["success"])
            self.assertIn(phase, result["failure"])
            self.assertFalse((output / "success.receipt").exists())
            self.assertTrue(
                all(not alive(child["pid"]) for child in result["children"])
            )
            owned = json.loads((output / "fixture-owned.json").read_text())
            self.assertTrue(all(not alive(pid) for pid in owned["pids"]))
            self.assertTrue(harness._port_released(owned["port"]))
            self.assertTrue(all(child["groupReaped"] for child in result["children"]))

    def test_readiness_timeout(self):
        self.assert_failure_cleans_children("readiness-timeout", "readiness")

    def test_ping_without_player_login_fails_join(self):
        self.assert_failure_cleans_children("failed-join", "join")

    def test_missing_capture(self):
        self.assert_failure_cleans_children("missing-capture", "capture")

    def test_shutdown_timeout_reaps_grandchild(self):
        self.assert_failure_cleans_children("shutdown-timeout", "shutdown")

    def test_stale_capture(self):
        self.assert_failure_cleans_children("stale-capture", "stale")

    def test_wrong_version_leaves_failure_result(self):
        self.assert_failure_cleans_children("wrong-mod-version", "geckolib")

    def test_newline_flood_cleans_group_and_port(self):
        self.assert_failure_cleans_children("newline-flood", "overflow")

    def test_marker_then_flood_cleans_group_and_port(self):
        self.assert_failure_cleans_children("marker-flood", "overflow")

    def test_setup_failure_cleans_group_and_port(self):
        self.assert_failure_cleans_children("setup-failure", "setup")

    def test_positive_fixture_completes(self):
        with tempfile.TemporaryDirectory() as raw:
            output = Path(raw)
            self.assertEqual(0, harness.run_fixture_scenario("positive", output))
            self.assertTrue(json.loads((output / "result.json").read_text())["success"])


if __name__ == "__main__":
    unittest.main()
