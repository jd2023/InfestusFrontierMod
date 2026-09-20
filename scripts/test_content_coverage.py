#!/usr/bin/env python3
"""Regressions for the incremental content coverage gate."""

from __future__ import annotations

import copy
import json
import tempfile
import unittest
from pathlib import Path

import check_content_coverage as coverage


TASKS = """# fixture queue
IF-108 Coverage harness
Owner: integration
Blocks: none

IF-095 Initial content
Owner: construction
Blocks: T0-16

IF-005 Substrate
Owner: ecology
Blocks: T0-01

IF-004 Guide
Owner: discovery
Blocks: none

IF-035 First rank
Owner: equipment
Blocks: none
"""

PLAN = {
    "items": {"I000": "IF-095", "I001": "IF-095"},
    "mutations": {"M1.I": "IF-035"},
    "services": {"content_assertions": "IF-108"},
    "requires": {
        "IF-095": ["content_assertions", "I000"],
        "IF-005": ["I000"],
        "IF-035": ["I000"],
    },
    "excluded_items": [],
}


def entry(*catalog: str, stem: str | None = None) -> dict:
    stem = stem or catalog[0].lower().replace(".", "_")
    return {
        "catalog": list(catalog),
        "registry": [f"infestusfrontier:fixture/{stem}"],
        "producer": f"infestusfrontier:fixture/{stem}",
        "assertions": {
            "obtain": f"infestusfrontier_tests:fixture.{stem}.obtain",
            "use": f"infestusfrontier_tests:fixture.{stem}.use",
            "guide": f"infestusfrontier_client:fixture.{stem}.guide",
        },
    }


CONSTRUCTION = {
    "schema": 1,
    "owner": "construction",
    "contributions": [
        {
            "task": "IF-095",
            "entries": [entry("I000"), entry("I001", "T0-16", stem="organ_bud")],
        }
    ],
}


class Fixture:
    def __init__(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        (self.root / ".ktask").mkdir()
        (self.root / "src/testMod/resources").mkdir(parents=True)
        (self.root / ".ktask/tasks.md").write_text(TASKS, encoding="utf-8")
        (self.root / ".ktask/content-plan.json").write_text(
            json.dumps(PLAN), encoding="utf-8"
        )

    def checkpoint(self, task: str) -> None:
        (self.root / "src/testMod/resources/content-checkpoint.json").write_text(
            json.dumps({"schema": 1, "through": task}), encoding="utf-8"
        )

    def contributor(self, value: dict, owner: str | None = None) -> Path:
        owner = owner or value["owner"]
        path = self.root / "src/testMod/resources" / owner / "coverage.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value), encoding="utf-8")
        return path

    def close(self) -> None:
        self.temporary.cleanup()


class CoverageTests(unittest.TestCase):
    def setUp(self):
        self.fixture = Fixture()

    def tearDown(self):
        self.fixture.close()

    def assert_rejected(self, identifier: str, action) -> None:
        with self.assertRaises(coverage.CoverageError) as caught:
            action()
        self.assertIn(identifier, str(caught.exception))

    def test_empty_bootstrap_passes_only_before_content_is_accepted(self):
        self.fixture.checkpoint("IF-108")
        result = coverage.validate(self.fixture.root)
        self.assertEqual(result["representations"], [])
        self.assertEqual(result["assertions"]["gameTest"], [])
        self.fixture.checkpoint("IF-095")
        self.assert_rejected("I000", lambda: coverage.validate(self.fixture.root))

    def test_default_reads_candidate_checkpoint_and_only_contributor(self):
        self.fixture.checkpoint("IF-095")
        path = self.fixture.contributor(CONSTRUCTION)
        self.assertEqual(coverage.validate(self.fixture.root)["through"], "IF-095")
        path.unlink()
        self.assert_rejected("I000", lambda: coverage.validate(self.fixture.root))

    def test_missing_item_rank_assertion_and_producer_name_exact_identifier(self):
        self.fixture.checkpoint("IF-095")
        broken = copy.deepcopy(CONSTRUCTION)
        broken["contributions"][0]["entries"] = broken["contributions"][0][
            "entries"
        ][1:]
        self.fixture.contributor(broken)
        self.assert_rejected("I000", lambda: coverage.validate(self.fixture.root))

        broken = copy.deepcopy(CONSTRUCTION)
        del broken["contributions"][0]["entries"][0]["assertions"]["use"]
        self.fixture.contributor(broken)
        self.assert_rejected("I000", lambda: coverage.validate(self.fixture.root))

        broken = copy.deepcopy(CONSTRUCTION)
        del broken["contributions"][0]["entries"][0]["producer"]
        self.fixture.contributor(broken)
        self.assert_rejected("I000", lambda: coverage.validate(self.fixture.root))

        self.fixture.contributor(CONSTRUCTION)
        ecology = {
            "schema": 1,
            "owner": "ecology",
            "contributions": [
                {"task": "IF-005", "entries": [entry("T0-01", stem="substrate")]}
            ],
        }
        self.fixture.contributor(ecology)
        self.fixture.checkpoint("IF-035")
        self.assert_rejected("M1.I", lambda: coverage.validate(self.fixture.root))

    def test_explicit_checkpoint_can_only_raise_requirement(self):
        self.fixture.checkpoint("IF-095")
        self.fixture.contributor(CONSTRUCTION)
        self.assert_rejected(
            "IF-108", lambda: coverage.validate(self.fixture.root, through="IF-108")
        )
        self.assert_rejected(
            "T0-01", lambda: coverage.validate(self.fixture.root, through="IF-005")
        )

    def test_future_unknown_and_misowned_contributions_are_rejected(self):
        self.fixture.checkpoint("IF-095")
        self.fixture.contributor(CONSTRUCTION)
        future = {
            "schema": 1,
            "owner": "equipment",
            "contributions": [
                {"task": "IF-035", "entries": [entry("M1.I", stem="rank")]}
            ],
        }
        self.fixture.contributor(future)
        self.assert_rejected("IF-035", lambda: coverage.validate(self.fixture.root))
        (self.fixture.root / "src/testMod/resources/equipment/coverage.json").unlink()

        broken = copy.deepcopy(CONSTRUCTION)
        broken["contributions"][0]["entries"].append(entry("I999"))
        self.fixture.contributor(broken)
        self.assert_rejected("I999", lambda: coverage.validate(self.fixture.root))

        broken = copy.deepcopy(CONSTRUCTION)
        broken["owner"] = "ecology"
        self.fixture.contributor(broken, owner="ecology")
        (self.fixture.root / "src/testMod/resources/construction/coverage.json").unlink()
        self.assert_rejected("IF-095", lambda: coverage.validate(self.fixture.root))

    def test_aliases_share_one_representation(self):
        self.fixture.checkpoint("IF-095")
        self.fixture.contributor(CONSTRUCTION)
        result = coverage.validate(self.fixture.root)
        bud = next(r for r in result["representations"] if "I001" in r["catalog"])
        self.assertEqual(bud["catalog"], ["I001", "T0-16"])
        self.assertEqual(len(bud["registry"]), 1)

        broken = copy.deepcopy(CONSTRUCTION)
        broken["contributions"][0]["entries"][1:] = [entry("I001"), entry("T0-16")]
        self.fixture.contributor(broken)
        self.assert_rejected("I001/T0-16", lambda: coverage.validate(self.fixture.root))

    def test_guide_assertions_stage_then_become_client_requirements(self):
        self.fixture.checkpoint("IF-095")
        self.fixture.contributor(CONSTRUCTION)
        staged = coverage.validate(self.fixture.root)
        self.assertEqual(staged["guideMode"], "staged")
        self.assertEqual(len(staged["assertions"]["stagedGuide"]), 2)
        self.assertEqual(staged["assertions"]["client"], [])

        ecology = {
            "schema": 1,
            "owner": "ecology",
            "contributions": [
                {"task": "IF-005", "entries": [entry("T0-01", stem="substrate")]}
            ],
        }
        self.fixture.contributor(ecology)
        active = coverage.validate(self.fixture.root, through="IF-004")
        self.assertEqual(active["guideMode"], "execute")
        self.assertEqual(len(active["assertions"]["client"]), 3)
        self.assertEqual(active["assertions"]["stagedGuide"], [])

    def test_identifier_and_edge_bounds_fail_before_expansion(self):
        self.fixture.checkpoint("IF-108")
        plan = copy.deepcopy(PLAN)
        plan["services"] = {f"service_{n}": "IF-035" for n in range(4097)}
        (self.fixture.root / ".ktask/content-plan.json").write_text(json.dumps(plan))
        self.assert_rejected("4096", lambda: coverage.validate(self.fixture.root))

        plan = copy.deepcopy(PLAN)
        plan["requires"] = {"IF-035": ["I000"] * 16385}
        (self.fixture.root / ".ktask/content-plan.json").write_text(json.dumps(plan))
        self.assert_rejected("16384", lambda: coverage.validate(self.fixture.root))


if __name__ == "__main__":
    unittest.main()
