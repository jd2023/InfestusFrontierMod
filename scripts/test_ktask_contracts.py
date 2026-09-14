"""Offline acceptance-contract tests; never starts an AI worker."""

import unittest

from ktask_contracts import parse_tasks, check_scope, check_review


class ContractTests(unittest.TestCase):
    def packet(self, identity="IF-001", depends="none"):
        return f'''{identity} One operation
Milestone: M0
Owner: processing
Depends: {depends}
Spec: docs/BLOCK_CATALOG.md
Blocks: T0-02
Scope: ["src/processing/**"]
Contract: process one reserved batch
Red: refused output retains inputs
Accept: success consumes once
Bounds: one retained batch
Evidence: rules
'''

    def test_dependency_must_precede_consumer(self):
        with self.assertRaisesRegex(ValueError, "dependency"):
            parse_tasks(self.packet(depends="IF-002"))

    def test_duplicate_ids_and_missing_contract_refused(self):
        with self.assertRaises(ValueError):
            parse_tasks(self.packet() + "\n---\n" + self.packet())
        with self.assertRaises(ValueError):
            parse_tasks(self.packet().replace("Contract: process one reserved batch\n", ""))

    def test_queue_markers_are_runtime_only(self):
        with self.assertRaises(ValueError):
            parse_tasks("[DONE] " + self.packet())

    def test_scope_rejects_gate_changes_and_neighbor_feature(self):
        task = parse_tasks(self.packet())[0]
        check_scope(task, ["src/processing/Recipe.java"])
        for path in [".ktask/config.toml", "src/equipment/Armor.java", "src/processing/../../secret"]:
            with self.assertRaises(ValueError):
                check_scope(task, [path])

    def test_named_module_scope_expands_without_granting_neighbor_internals(self):
        packet = self.packet().replace('["src/processing/**"]', '["@module:processing"]')
        task = parse_tasks(packet)[0]
        check_scope(task, ["core/src/main/java/org/jd/infestusfrontier/processing/Batch.java"])
        with self.assertRaises(ValueError):
            check_scope(task, ["core/src/main/java/org/jd/infestusfrontier/equipment/Piece.java"])
        with self.assertRaises(ValueError):
            parse_tasks(packet.replace("@module:processing", "@module:../../"))

    def test_review_requires_all_checks_and_current_candidate(self):
        good = {"task": "IF-001", "candidate": "abc", "verdict": "accept",
                "checks": {name: "pass" for name in ["correctness", "placement", "simplicity",
                    "scope", "boundaries", "tests", "comments", "performance", "evidence"]},
                "findings": []}
        check_review(good, "IF-001", "abc")
        for broken in [dict(good, candidate="old"), dict(good, findings=["bug"]),
                       dict(good, checks={}), dict(good, verdict="reject")]:
            with self.assertRaises(ValueError):
                check_review(broken, "IF-001", "abc")


if __name__ == "__main__":
    unittest.main()
