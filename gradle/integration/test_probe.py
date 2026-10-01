"""The multiplayer evidence must come from matching, separately observed snapshots."""
import tempfile
import unittest
from pathlib import Path
from gradle.integration.harness import _probe_consistent, _observer_command, HarnessFailure


def snapshot(role, revision=4, work=21, state="WORKING", slots="[honey, culture]"):
    return (f"INFESTUS_PROBE_SNAPSHOT role={role} revision={revision} work={work} "
            f"required=1200 water=0 state={state} slots={slots} refusal=NONE")


class ProbeEvidenceTest(unittest.TestCase):
    def test_matching_received_progress_within_one_cadence(self):
        self.assertTrue(_probe_consistent(snapshot("owner"), snapshot("observer", work=30), "WORKING"))

    def test_missing_stale_or_conflicting_observer_cannot_pass(self):
        for other in ("", snapshot("observer", revision=5), snapshot("observer", work=32),
                      snapshot("observer", slots="[]"), snapshot("observer", state="IDLE")):
            with self.subTest(other=other):
                self.assertFalse(_probe_consistent(snapshot("owner"), other, "WORKING"))

    def test_observer_identity_changes_only_owned_argument_copy(self):
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            source = root / "launch.txt"
            text = "--username\nFixturePlayer\n--uuid\n11111111-1111-1111-1111-111111111111"
            source.write_text(text)
            command = _observer_command(["java", "@" + str(source)], root)
            self.assertEqual(text, source.read_text())
            self.assertIn("FixtureObserver", Path(command[-1][1:]).read_text())
            source.write_text("unrecognized")
            with self.assertRaises(HarnessFailure):
                _observer_command(["java", "@" + str(source)], root)

class SharedDisplayTest(unittest.TestCase):
    def test_wrapper_publishes_only_display_then_executes_same_command(self):
        from unittest.mock import patch
        from gradle.integration.display_client import launch
        import json
        with tempfile.TemporaryDirectory() as raw:
            destination = Path(raw) / "display.json"
            with patch.dict("os.environ", {"DISPLAY": ":123", "XAUTHORITY": raw + "/auth"}), patch("os.execvp") as execute:
                launch(destination, ["java", "@args"])
            self.assertEqual({"DISPLAY": ":123", "XAUTHORITY": raw + "/auth"}, json.loads(destination.read_text()))
            execute.assert_called_once_with("java", ["java", "@args"])
            self.assertFalse(destination.with_suffix(".tmp").exists())
