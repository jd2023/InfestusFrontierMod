"""Offline acceptance-contract tests; never starts an AI worker."""

import unittest

from ktask_contracts import parse_tasks, check_review


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

    def test_native_status_markers_do_not_change_packet_identity(self):
        original = parse_tasks(self.packet())[0]
        for marker in ('DONE', 'FAIL', 'INPUT'):
            parsed = parse_tasks(f'[{marker}] ' + self.packet())[0]
            self.assertEqual(original['digest'], parsed['digest'])
            self.assertEqual(original['body'], parsed['body'])
            self.assertEqual(marker, parsed['status'])

    def test_named_module_starting_points_expand(self):
        packet = self.packet().replace('["src/processing/**"]', '["@module:processing"]')
        task = parse_tasks(packet)[0]
        self.assertIn("core/src/main/java/org/jd/infestusfrontier/processing/**", task['scope'])
        with self.assertRaises(ValueError):
            parse_tasks(packet.replace("@module:processing", "@module:../../"))

    def test_review_requires_all_checks_and_current_candidate(self):
        good = {"task": "IF-001", "candidate": "abc", "verdict": "accept",
                "checks": {name: {"status": "pass", "evidence": "file.py:1 — verified boundary"} for name in ["correctness", "placement", "simplicity",
                    "scope", "boundaries", "tests", "comments", "performance", "evidence"]},
                "findings": []}
        check_review(good, "IF-001", "abc")
        for broken in [dict(good, candidate="old"), dict(good, findings=["bug"]),
                       dict(good, checks={}), dict(good, verdict="reject")]:
            with self.assertRaises(ValueError):
                check_review(broken, "IF-001", "abc")

    def test_guide_starting_points_match_resource_layout(self):
        root = 'src/main/resources/data/infestusfrontier/modonomicon/books/the_waking_genome/'
        for owner in ('processing', 'discovery'):
            task = parse_tasks(self.packet().replace('["src/processing/**"]',
                               f'["@module:{owner}"]'))[0]
            self.assertIn(root + f'categories/{owner}.json', task['scope'])
            self.assertIn(root + f'entries/{owner}/**', task['scope'])
            if owner == 'discovery':
                self.assertIn(root + 'book.json', task['scope'])

    def test_review_without_supporting_evidence_is_rejected(self):
        from ktask_contracts import CHECKS
        verdict = dict(task='IF-001', candidate='abc', verdict='accept', findings=[],
                       checks=dict.fromkeys(CHECKS, 'pass'))
        with self.assertRaises(ValueError):
            check_review(verdict, 'IF-001', 'abc')


if __name__ == "__main__":
    unittest.main()
