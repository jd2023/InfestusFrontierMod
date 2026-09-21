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


class VisualSetupTest(unittest.TestCase):
    def test_only_active_owner_fixture_commands_are_sent(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            fixture = root / "src/testMod/resources/construction/visual-setup.json"
            fixture.parent.mkdir(parents=True)
            fixture.write_text(json.dumps({"requires": "bud.obtain", "commands": ["give FixturePlayer minecraft:stone"]}))
            self.assertEqual([], harness.visual_setup_commands(root, {"gameTest": []}))
            self.assertEqual(["give FixturePlayer minecraft:stone"],
                             harness.visual_setup_commands(root, {"gameTest": ["bud.obtain"]}))

    def test_survival_setup_is_owner_scoped_and_shares_the_combined_bound(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            fixture = root / "src/testMod/resources/discovery/survival-setup.json"
            fixture.parent.mkdir(parents=True)
            fixture.write_text(json.dumps({"requires": "guide.use", "commands": ["give FixturePlayer minecraft:book"]}))
            self.assertEqual(([], []), harness._visual_setup(root, {"gameTest": []}, "survival-setup.json"))
            self.assertEqual((["give FixturePlayer minecraft:book"], []),
                             harness._visual_setup(root, {"gameTest": ["guide.use"]}, "survival-setup.json"))
            with self.assertRaisesRegex(harness.HarnessFailure, "combined setup"):
                harness._client_lifecycle(None, None, None, None, None, root, None,
                                         setup_commands=["say visual"] * (2 * harness.MAX_SETUP_OWNERS * harness.OWNER_SETUP_COMMANDS),
                                         discovery_setup=["say survival"])

    def test_owner_detail_captures_are_bounded_and_activation_scoped(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            path = root / "src/testMod/resources/ecology/visual-setup.json"
            path.parent.mkdir(parents=True)
            fixture = {"requires": "substrate.use", "commands": ["say fixture"],
                       "captures": ["ecology-underside.png"]}
            path.write_text(json.dumps(fixture))
            self.assertEqual([], harness.visual_setup_captures(root, {"gameTest": []}))
            self.assertEqual(["ecology-underside.png"],
                             harness.visual_setup_captures(root, {"gameTest": ["substrate.use"]}))
            fixture["captures"] = [f"view-{i}.png" for i in range(harness.OWNER_SETUP_CAPTURES)]
            path.write_text(json.dumps(fixture))
            self.assertEqual(fixture["captures"],
                             harness.visual_setup_captures(root, {"gameTest": ["substrate.use"]}))
            for names in (["../escape.png"], ["bootstrap-world.png"], ["same.png"] * 2,
                          [f"view-{i}.png" for i in range(harness.OWNER_SETUP_CAPTURES + 1)]):
                fixture["captures"] = names
                path.write_text(json.dumps(fixture))
                with self.assertRaisesRegex(harness.HarnessFailure, "visual setup"):
                    harness.visual_setup_captures(root, {"gameTest": ["substrate.use"]})

    def test_capture_budget_is_per_owner_not_shared(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            limit = harness.OWNER_SETUP_CAPTURES
            for owner in ("ecology", "processing"):
                path = root / f"src/testMod/resources/{owner}/visual-setup.json"
                path.parent.mkdir(parents=True)
                fixture = {"requires": f"{owner}.use", "commands": ["say fixture"],
                           "captures": [f"{owner}-{i}.png" for i in range(limit)]}
                path.write_text(json.dumps(fixture))
            requirements = {"gameTest": ["ecology.use", "processing.use"]}
            self.assertEqual(2 * limit, len(harness.visual_setup_captures(root, requirements)))
            fixture["captures"].append("processing-extra.png")
            path.write_text(json.dumps(fixture))
            with self.assertRaisesRegex(harness.HarnessFailure, "invalid visual setup captures"):
                harness.visual_setup_captures(root, requirements)

    def test_rejects_duplicate_capture_names_across_owners(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            for owner in ("ecology", "processing"):
                path = root / f"src/testMod/resources/{owner}/visual-setup.json"
                path.parent.mkdir(parents=True)
                path.write_text(json.dumps({"requires": "shared.use", "commands": ["say x"],
                                            "captures": ["same.png"]}))
            with self.assertRaisesRegex(harness.HarnessFailure, "duplicate"):
                harness.visual_setup_captures(root, {"gameTest": ["shared.use"]})

    def test_owner_fixture_admits_its_full_command_budget(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            path = root / "src/testMod/resources/construction/visual-setup.json"
            path.parent.mkdir(parents=True)
            commands = ["say bounded"] * harness.OWNER_SETUP_COMMANDS
            path.write_text(json.dumps({"requires": "shell.use", "commands": commands}))
            self.assertEqual(commands, harness.visual_setup_commands(root, {"gameTest": ["shell.use"]}))

    def test_rejects_excessive_or_multiline_commands(self):
        for commands in (["say one\nsay two"], ["say test"] * (harness.OWNER_SETUP_COMMANDS + 1)):
            with self.subTest(commands=commands), tempfile.TemporaryDirectory() as raw:
                root = Path(raw)
                fixture = root / "src/testMod/resources/construction/visual-setup.json"
                fixture.parent.mkdir(parents=True)
                fixture.write_text(json.dumps({"requires": "bud.obtain", "commands": commands}))
                with self.assertRaisesRegex(harness.HarnessFailure, "visual setup"):
                    harness.visual_setup_commands(root, {"gameTest": ["bud.obtain"]})


class EvaluatorTest(EvidenceCase):
    def result_for_profile(self, profile):
        result = self.result()
        result["profile"] = profile
        for mods in result["runtimeMods"].values():
            if profile in ("jei", "combined"):
                mods["jei"] = "19.56.0.438"
                mods["mezz_config"] = "0.5.6"
            if profile in ("curios", "combined"):
                mods["curios"] = "9.5.1+1.21.1"
        return result

    def test_profile_file_must_match_bootstrap_contract_exactly(self):
        with tempfile.TemporaryDirectory() as raw:
            profile = Path(raw) / "profiles.json"
            profile.write_text('{"schema": 2}', encoding="utf-8")
            with self.assertRaisesRegex(harness.HarnessFailure, "schema 1 bootstrap"):
                harness.validate_profile_file(profile, "required", "bootstrap")

    def test_invalid_profile_is_rejected_before_launch(self):
        with self.assertRaisesRegex(harness.HarnessFailure, "invalid profile"):
            harness.validate_profile_file(None, "everything", "bootstrap")

    def test_multiplayer_requires_observer_runtime_and_action_evidence(self):
        result = self.result()
        result["multiplayer"] = True
        with self.assertRaisesRegex(harness.HarnessFailure, "process roles"):
            harness.ResultEvaluator().evaluate(result)
        result["runtimeMods"]["observer"] = copy.deepcopy(result["runtimeMods"]["client"])
        result["runtimeMods"]["observer"]["geckolib"] = "4.9.1"
        with self.assertRaisesRegex(harness.HarnessFailure, "observer mod geckolib"):
            harness.ResultEvaluator().evaluate(result)
        result["runtimeMods"]["observer"]["geckolib"] = "4.9.2"
        with self.assertRaisesRegex(harness.HarnessFailure, "probeCancelledBoth"):
            harness.ResultEvaluator().evaluate(result)

    def test_wrong_runtime_version_is_rejected_at_named_mod(self):
        result = self.result()
        result["runtimeMods"]["server"]["geckolib"] = "4.9.1"
        with self.assertRaisesRegex(harness.HarnessFailure, "geckolib.*4.9.2"):
            harness.ResultEvaluator().evaluate(result)

    def test_pinned_jei_bundled_identity_is_required_for_both_processes(self):
        for profile in ("jei", "combined"):
            with self.subTest(profile=profile):
                harness.ResultEvaluator().evaluate(self.result_for_profile(profile))

    def test_pinned_jei_bundled_identity_rejects_missing_wrong_and_unknown(self):
        cases = (
            ("missing", "mezz_config"),
            ("wrong", "mezz_config.*0.5.6"),
            ("unknown", "identities do not match"),
        )
        for profile in ("jei", "combined"):
            for role in ("server", "client"):
                for mutation, diagnostic in cases:
                    with self.subTest(
                        profile=profile, role=role, mutation=mutation
                    ):
                        result = self.result_for_profile(profile)
                        if mutation == "missing":
                            del result["runtimeMods"][role]["mezz_config"]
                        elif mutation == "wrong":
                            result["runtimeMods"][role]["mezz_config"] = "0.5.5"
                        else:
                            result["runtimeMods"][role]["unknown_library"] = "1.0"
                        with self.assertRaisesRegex(
                            harness.HarnessFailure, diagnostic
                        ):
                            harness.ResultEvaluator().evaluate(result)

    def test_jei_bundled_identity_is_rejected_when_jei_is_unselected(self):
        for profile in ("required", "curios"):
            for role in ("server", "client"):
                with self.subTest(profile=profile, role=role):
                    result = self.result_for_profile(profile)
                    result["runtimeMods"][role]["mezz_config"] = "0.5.6"
                    with self.assertRaisesRegex(
                        harness.HarnessFailure, "identities do not match"
                    ):
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

    def test_required_detail_capture_cannot_be_missing_or_invalid(self):
        for mutation in ("missing", "empty", "truncated", "stale"):
            with self.subTest(mutation=mutation), tempfile.TemporaryDirectory() as raw:
                result = self.result()
                result["requiredCaptures"] = ["ecology-underside.png"]
                path = Path(raw) / "ecology-underside.png"
                if mutation != "missing":
                    contents = Path(result["artifacts"]["bootstrap-world.png"]).read_bytes()
                    path.write_bytes(b"" if mutation == "empty" else contents[:30]
                                     if mutation == "truncated" else contents)
                    if mutation == "stale":
                        os.utime(path, ns=(result["runStartedNs"] - 1, result["runStartedNs"] - 1))
                    result["artifacts"][path.name] = str(path)
                    result["artifactDigests"][path.name] = harness._sha256(path)
                with self.assertRaises(harness.HarnessFailure):
                    harness.ResultEvaluator().evaluate(result)
        result = self.result()
        result["requiredCaptures"] = ["ecology-underside.png"]
        result["artifacts"]["ecology-underside.png"] = result["artifacts"]["bootstrap-world.png"]
        result["artifactDigests"]["ecology-underside.png"] = result["artifactDigests"]["bootstrap-world.png"]
        harness.ResultEvaluator().evaluate(result)

    def test_valid_evidence_is_accepted(self):
        harness.ResultEvaluator().evaluate(self.result())

    def test_content_requirements_reject_missing_named_runtime_assertion(self):
        requirements = {
            "gameTest": ["infestusfrontier_tests:fixture.obtain"],
            "client": [],
        }
        self.assertEqual(
            harness._named_assertions(
                "", requirements["gameTest"], harness.CONTENT_MARKER
            ),
            {"content:infestusfrontier_tests:fixture.obtain": False},
        )
        log = "INFESTUS_CONTENT_ASSERTION name=infestusfrontier_tests:fixture.obtain\n"
        self.assertTrue(
            harness._named_assertions(
                log, requirements["gameTest"], harness.CONTENT_MARKER
            )["content:infestusfrontier_tests:fixture.obtain"]
        )

    def test_guide_requirement_is_not_satisfied_by_server_or_partial_marker(self):
        name = "infestusfrontier_client:fixture.guide"
        for log in ("", f"INFESTUS_CONTENT_ASSERTION name={name}\n",
                    f"INFESTUS_GUIDE_ASSERTION name={name}_other\n"):
            result = self.result()
            result["assertions"].update(harness._named_assertions(log, [name], harness.GUIDE_MARKER))
            with self.assertRaisesRegex(harness.HarnessFailure, name):
                harness.ResultEvaluator().evaluate(result)
        result = self.result()
        result["assertions"].update(harness._named_assertions(
            f"INFESTUS_GUIDE_ASSERTION name={name}\n", [name], harness.GUIDE_MARKER))
        harness.ResultEvaluator().evaluate(result)

    def test_content_requirements_file_is_bounded_and_typed(self):
        with tempfile.TemporaryDirectory() as raw:
            path = Path(raw) / "requirements.json"
            path.write_text(
                json.dumps(
                    {
                        "assertions": {
                            "gameTest": ["infestusfrontier_tests:fixture.obtain"],
                            "client": ["infestusfrontier_client:fixture.guide"],
                        }
                    }
                )
            )
            self.assertEqual(
                harness.load_content_requirements(path)["client"],
                ["infestusfrontier_client:fixture.guide"],
            )
            path.write_text(
                json.dumps({"assertions": {"gameTest": [], "client": [7]}})
            )
            with self.assertRaisesRegex(harness.HarnessFailure, "client"):
                harness.load_content_requirements(path)
            path.write_text(json.dumps({"assertions": [], "harnessAssertions": []}))
            with self.assertRaisesRegex(harness.HarnessFailure, "content assertion"):
                harness.load_content_requirements(path)
            path.write_text(" " * (1024 * 1024 + 1))
            with self.assertRaisesRegex(harness.HarnessFailure, "1 MiB"):
                harness.load_content_requirements(path)


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
