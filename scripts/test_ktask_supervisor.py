"""Plan migration preserves accepted receipts and unfinished implementation."""

import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import ktask_supervisor as supervisor
import ktask_workflow as flow


class SupervisorTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git('init', '-b', 'feature/test')
        self.git('config', 'user.name', 'Test')
        self.git('config', 'user.email', 'test@example.invalid')
        (self.root / '.ktask').mkdir()
        (self.root / 'docs').mkdir()
        for name in ('config.toml', 'context.md', 'prompt.md', 'autoresolve.md'):
            (self.root / '.ktask' / name).write_text('project_dir = "."\n' if name == 'config.toml' else 'prompt\n')
        (self.root / '.gitignore').write_text('.ktask/session/\n')
        (self.root / 'code.txt').write_text('baseline\n')
        self.git('add', '.')
        self.git('commit', '-m', 'baseline')
        self.policy = dict(branch='feature/test', remote='origin')
        self.tasks = [dict(id=f'IF-00{i}', digest=f'digest{i}', body=f'IF-00{i} task',
                           dependencies=[] if i == 1 else ['IF-001'], scope=['code.txt','new.txt'])
                      for i in (1, 2)]

    def git(self, *args):
        return subprocess.run(['git', *args], cwd=self.root, text=True, check=True,
                              capture_output=True).stdout.strip()

    def accept_first(self):
        flow.save(self.root / '.ktask/session/accepted/IF-001.json',
                  dict(task='IF-001', packet='digest1', commit=self.git('rev-parse', 'HEAD')))

    def test_migration_keeps_done_prefix_and_allows_pending_revision(self):
        self.accept_first()
        self.tasks[1].update(digest='revised', body='IF-002 revised task')
        supervisor.reconcile(self.root, self.tasks, self.policy)
        runtime = (self.root / '.ktask/session/.ktask/tasks.md').read_text()
        self.assertIn('[DONE] IF-001 task', runtime)
        self.assertIn('IF-002 revised task', runtime)
        self.assertEqual(1, runtime.count('[DONE]'))

    def test_completed_contract_cannot_be_rewritten_or_removed(self):
        self.accept_first()
        self.tasks[0]['digest'] = 'changed'
        with self.assertRaises(ValueError):
            supervisor.reconcile(self.root, self.tasks, self.policy)
        with self.assertRaises(ValueError):
            supervisor.reconcile(self.root, self.tasks[1:], self.policy)

    def test_nonprefix_or_forged_receipt_cannot_skip_work(self):
        flow.save(self.root / '.ktask/session/accepted/IF-002.json',
                  dict(task='IF-002', packet='digest2', commit=self.git('rev-parse', 'HEAD')))
        with self.assertRaises(ValueError):
            supervisor.reconcile(self.root, self.tasks, self.policy)

    def test_park_restore_preserves_new_files_and_user_files(self):
        (self.root / 'user.pdf').write_bytes(b'untouched')
        state = flow.begin(self.root, self.tasks[0], self.policy)
        flow.save(self.root / '.ktask/session/active.json', state)
        (self.root / 'code.txt').write_text('unfinished\n')
        (self.root / 'new.txt').write_text('new test\n')
        supervisor.park(self.root, self.tasks[0], state, self.policy)
        self.assertTrue(flow.tracked_clean(self.root))
        self.assertFalse((self.root / 'new.txt').exists())
        self.assertEqual(b'untouched', (self.root / 'user.pdf').read_bytes())
        (self.root / 'docs/plan.md').write_text('corrected plan\n')
        self.git('add', 'docs/plan.md')
        self.git('commit', '-m', 'plan correction')
        supervisor.restore(self.root, self.tasks[0], self.policy)
        self.assertEqual('unfinished\n', (self.root / 'code.txt').read_text())
        self.assertEqual('new test\n', (self.root / 'new.txt').read_text())
        restored = json.loads((self.root / '.ktask/session/active.json').read_text())
        self.assertEqual(self.git('rev-parse', 'HEAD'), restored['baseline'])
        self.assertNotEqual(state['token'], restored['token'])

    def test_failed_runner_calls_coordinator_and_then_continues(self):
        policy = dict(self.policy, runner='fake-runner', coordinator_attempts=2)
        events = []
        def execute(*args, **kwargs):
            events.append('worker')
            if events.count('worker') == 1:
                return 1
            self.accept_first()
            return 0
        def correct(*args):
            events.append('coordinator')
        with patch.object(flow, 'load_project', return_value=(self.tasks[:1], policy)), \
                patch.object(flow, 'validate'), \
                patch.object(supervisor, 'decision', return_value=dict(status='ready', reason='precise contract')), \
                patch.object(supervisor, 'run_runner', side_effect=execute), \
                patch.object(supervisor, 'coordinate', side_effect=correct):
            self.assertEqual(0, supervisor.supervise(self.root, policy))
        self.assertEqual(['worker', 'coordinator', 'worker'], events)

    def test_readiness_failure_does_not_start_worker_and_is_bounded(self):
        policy = dict(self.policy, runner='fake-runner', coordinator_attempts=1)
        with patch.object(flow, 'load_project', return_value=(self.tasks, policy)), \
                patch.object(flow, 'validate'), \
                patch.object(supervisor, 'decision', return_value=dict(status='retry', reason='missing producer')), \
                patch.object(supervisor, 'run_runner') as execute, \
                patch.object(supervisor, 'coordinate') as correct:
            with self.assertRaisesRegex(ValueError, 'exhausted'):
                supervisor.supervise(self.root, policy)
            execute.assert_not_called()
            correct.assert_called_once()

    def test_rejected_plan_can_be_corrected_without_parking_twice(self):
        from ktask_contracts import CHECKS
        task = dict(self.tasks[0], title='One operation')
        reviews = []
        def revise(*args):
            (self.root / 'docs/ARCHITECTURE.md').write_text(f'Contract revision {len(reviews)}\n')
            return dict(status='revised', reason='Define the missing atomic operation')
        def review(root, packet, state, candidate, policy):
            reviews.append(candidate)
            return dict(task=task['id'], candidate=candidate,
                        verdict='reject' if len(reviews) == 1 else 'accept', findings=[],
                        checks={key: dict(status='pass', evidence='docs/ARCHITECTURE.md contract checked') for key in CHECKS})
        with patch.object(supervisor, 'decision', side_effect=revise), \
                patch.object(flow, 'load_project', return_value=([task], self.policy)), \
                patch.object(flow, 'validate'), patch.object(flow, 'run_gate'), \
                patch.object(flow, 'review_candidate', side_effect=review), \
                patch.object(flow, 'publish') as publish:
            with self.assertRaises(supervisor.PlanRejected):
                supervisor.coordinate(self.root, task, self.policy, 'missing interface')
            supervisor.coordinate(self.root, task, self.policy, 'review correction')
            publish.assert_called_once()
        self.assertEqual(2, len(reviews))
        self.assertTrue(flow.tracked_clean(self.root))

    def test_out_of_scope_gameplay_file_is_preserved_for_scope_repair(self):
        state = flow.begin(self.root, self.tasks[0], self.policy)
        (self.root / 'unexpected.java').write_text('unfinished implementation\n')
        supervisor.park(self.root, self.tasks[0], state, self.policy)
        self.assertFalse((self.root / 'unexpected.java').exists())
        supervisor.restore(self.root, self.tasks[0], self.policy)
        self.assertEqual('unfinished implementation\n', (self.root / 'unexpected.java').read_text())

    def test_restore_merges_prerequisite_edits_without_losing_candidate(self):
        baseline = ''.join(f'line {i}\n' for i in range(20))
        (self.root / 'code.txt').write_text(baseline)
        self.git('add', 'code.txt')
        self.git('commit', '-m', 'shared source')
        state = flow.begin(self.root, self.tasks[0], self.policy)
        (self.root / 'code.txt').write_text(baseline.replace('line 1\n', 'candidate\n'))
        supervisor.park(self.root, self.tasks[0], state, self.policy)
        (self.root / 'code.txt').write_text(baseline.replace('line 18\n', 'prerequisite\n'))
        self.git('add', 'code.txt')
        self.git('commit', '-m', 'prerequisite')
        supervisor.restore(self.root, self.tasks[0], self.policy)
        self.assertEqual(baseline.replace('line 1\n', 'candidate\n').replace('line 18\n', 'prerequisite\n'),
                         (self.root / 'code.txt').read_text())

    def test_external_pauses_do_not_spend_engineering_retries(self):
        policy = dict(self.policy, runner='fake-runner', coordinator_attempts=2)
        for status in ('ready', 'external'):
            with self.subTest(status=status), \
                    patch.object(flow, 'load_project', return_value=(self.tasks, policy)), \
                    patch.object(flow, 'validate'), \
                    patch.object(supervisor, 'decision', return_value=dict(status=status, reason='access required')), \
                    patch.object(supervisor, 'run_runner', return_value=5) as execute, \
                    patch.object(supervisor, 'coordinate') as coordinate:
                self.assertEqual(5, supervisor.supervise(self.root, policy))
                coordinate.assert_not_called()
                self.assertEqual(status == 'ready', execute.called)
        self.assertFalse((self.root / '.ktask/session/planning/IF-001/attempts.json').exists())

    def test_interrupted_invalid_plan_recovers_before_parsing(self):
        policy = dict(self.policy, coordinator_attempts=2)
        phase = self.root / '.ktask/session/planning/active.json'
        flow.save(phase, dict(task=self.tasks[0], reason='interrupted planning'))
        (self.root / '.ktask/tasks.md').write_text('invalid interrupted packet')
        events = []
        def correct(*args):
            events.append('recover')
            (self.root / '.ktask/tasks.md').write_text('valid repaired packet')
        def parse(*args):
            self.assertEqual('valid repaired packet', (self.root / '.ktask/tasks.md').read_text())
            events.append('parse')
            return [], policy
        with patch.object(supervisor, 'coordinate', side_effect=correct), \
                patch.object(flow, 'load_project', side_effect=parse), patch.object(flow, 'validate'):
            self.assertEqual(0, supervisor.supervise(self.root, policy))
        self.assertEqual(['recover', 'parse'], events)
        self.assertFalse(phase.exists())

    def test_review_rejection_reenters_coordinator_without_worker(self):
        policy = dict(self.policy, coordinator_attempts=2)
        with patch.object(supervisor, 'coordinate', side_effect=[supervisor.PlanRejected('bad contract'), None]) as correct:
            supervisor.recover(self.root, self.tasks[0], policy, 'missing interface')
        self.assertEqual(2, correct.call_count)
        self.assertEqual('bad contract', correct.call_args.args[-1])
        self.assertFalse((self.root / '.ktask/session/planning/active.json').exists())

    def test_coordinator_execution_failure_preserves_retry_budget(self):
        policy = dict(self.policy, coordinator_attempts=2, coordinator_model='fake', coordinator_effort='high')
        (self.root / '.ktask/coordinator.md').write_text('correct the packet')
        with patch.object(supervisor, 'run_runner', side_effect=ValueError('provider unavailable')):
            for _ in range(3):
                with self.assertRaises(supervisor.ExternalPause):
                    supervisor.recover(self.root, self.tasks[0], policy, 'missing contract')
        attempts = json.loads((self.root / '.ktask/session/planning/IF-001/attempts.json').read_text())
        self.assertEqual(0, attempts['used'])
        with patch.object(supervisor, 'decision', return_value=dict(status='retry', reason='concrete fix')), \
                patch.object(flow, 'load_project', return_value=(self.tasks, policy)), patch.object(flow, 'validate'):
            supervisor.recover(self.root, self.tasks[0], policy, 'missing contract')
        self.assertFalse((self.root / '.ktask/session/planning/active.json').exists())

    def test_review_execution_failure_is_not_a_plan_rejection(self):
        policy = dict(self.policy, coordinator_attempts=2)
        def revise(*args):
            (self.root / 'docs/ARCHITECTURE.md').write_text('corrected contract\n')
            return dict(status='revised', reason='define operation')
        with patch.object(supervisor, 'decision', side_effect=revise), \
                patch.object(flow, 'load_project', return_value=(self.tasks, policy)), patch.object(flow, 'validate'), \
                patch.object(flow, 'run_gate'), \
                patch.object(flow, 'review_candidate', side_effect=flow.ReviewUnavailable('review provider offline')):
            for _ in range(3):
                with self.assertRaises(supervisor.ExternalPause):
                    supervisor.recover(self.root, self.tasks[0], policy, 'missing contract')
        attempts = json.loads((self.root / '.ktask/session/planning/IF-001/attempts.json').read_text())
        self.assertEqual(0, attempts['used'])
        self.assertEqual('corrected contract\n', (self.root / 'docs/ARCHITECTURE.md').read_text())
