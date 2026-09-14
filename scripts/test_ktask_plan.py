"""Reject missing obligations and unavailable inputs before any model is dispatched."""

import copy
import unittest

from ktask_plan import validate_plan, bind_contracts


class PlanTests(unittest.TestCase):
    def setUp(self):
        self.tasks = [dict(id=f'IF-00{i}', Blocks='none', dependencies=[] if i == 1 else [f'IF-00{i-1}'])
                      for i in range(1, 4)]
        self.plan = dict(items={'I000':'IF-001','I001':'IF-002'},
                         mutations={f'M1.{rank}':'IF-002' for rank in ('I','II','III')},
                         services={'prepared':'IF-002'}, requires={'IF-003':['I001','prepared','M1.I']},
                         excluded_items=[])

    def check(self):
        validate_plan(self.tasks, self.plan, {'I000','I001'}, {'M1'})

    def test_complete_ordered_plan_passes_without_mutation(self):
        original = copy.deepcopy(self.plan)
        self.check()
        self.assertEqual(original, self.plan)

    def test_missing_item_and_rank_each_refused(self):
        for section, key in [('items','I001'), ('mutations','M1.II')]:
            with self.subTest(section=section):
                value = self.plan[section].pop(key)
                with self.assertRaisesRegex(ValueError, key):
                    self.check()
                self.plan[section][key] = value

    def test_consumer_before_producer_refused_even_with_valid_task_order(self):
        self.plan['requires'] = {'IF-001':['I001']}
        with self.assertRaisesRegex(ValueError, 'IF-001.*I001'):
            self.check()

    def test_unknown_ingredient_refused(self):
        self.plan['requires']['IF-003'].append('Spatial Fiber')
        with self.assertRaisesRegex(ValueError, 'Spatial Fiber'):
            self.check()

    def test_missing_dependency_refused(self):
        self.tasks[2]['dependencies'] = []
        with self.assertRaisesRegex(ValueError, 'dependency'):
            self.check()

    def test_unknown_owner_refused(self):
        self.plan['items']['I000'] = 'IF-999'
        with self.assertRaisesRegex(ValueError, 'IF-999'):
            self.check()

    def test_overlapping_exclusion_refused(self):
        self.plan['excluded_items'] = ['I000']
        with self.assertRaisesRegex(ValueError, 'I000'):
            self.check()

    def test_metadata_changes_invalidate_only_the_owning_contract(self):
        tasks = [dict(task, digest=task['id']) for task in self.tasks]
        before = bind_contracts(tasks, self.plan)
        self.plan['mutations']['M1.II'] = 'IF-003'
        after = bind_contracts(tasks, self.plan)
        self.assertEqual(before[0]['digest'], after[0]['digest'])
        self.assertNotEqual(before[1]['digest'], after[1]['digest'])
        self.assertNotEqual(before[2]['digest'], after[2]['digest'])
        self.assertEqual(tasks[0]['id'], tasks[0]['digest'])

    def test_metadata_free_pilot_retains_existing_receipt_hash(self):
        task = dict(id='IF-094', digest='accepted-pilot')
        self.assertEqual([task], bind_contracts([task], self.plan))
