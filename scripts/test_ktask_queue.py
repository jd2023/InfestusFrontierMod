"""Native queue metadata must never hide contract edits or skip undelivered work."""

import json
from pathlib import Path
import subprocess
import tempfile
import unittest

import ktask_workflow as flow
from ktask_queue import accepted_prefix, check_dispatch


class QueueTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git('init', '-b', 'feature/test')
        self.git('config', 'user.name', 'Test')
        self.git('config', 'user.email', 'test@example.invalid')
        (self.root / '.ktask').mkdir()
        (self.root / '.gitignore').write_text('.ktask/session/\n')
        self.queue = self.root / '.ktask/tasks.md'
        self.queue.write_text('IF-001 First\n\n---\n\nIF-002 Second\n')
        (self.root / 'code.txt').write_text('before\n')
        self.git('add', '.')
        self.git('commit', '-m', 'baseline')
        self.base = self.git('rev-parse', 'HEAD')

    def git(self, *args):
        return flow.git(self.root, *args)

    def test_only_native_status_changes_allow_clean_checkpoint(self):
        self.queue.write_text('[DONE] IF-001 First\n\n---\n\nIF-002 Second\n')
        self.assertTrue(flow.tracked_clean(self.root))
        self.queue.write_text('[DONE] IF-001 Changed contract\n\n---\n\nIF-002 Second\n')
        self.assertFalse(flow.tracked_clean(self.root))

    def test_staged_changes_hidden_by_worktree_restore_are_not_clean(self):
        for path in (self.queue, self.root / 'code.txt'):
            before = path.read_text()
            path.write_text('unexpected indexed content')
            self.git('add', str(path))
            path.write_text(before)
            self.assertFalse(flow.tracked_clean(self.root))
            self.git('add', str(path))

    def test_done_marker_without_receipt_cannot_skip_task(self):
        tasks = [dict(id='IF-001', digest='packet', status='DONE')]
        with self.assertRaisesRegex(ValueError, 'no accepted delivery'):
            accepted_prefix(self.root, tasks, flow.git)

    def test_accepted_contract_cannot_change_or_be_reexecuted(self):
        receipt = dict(task='IF-001', packet='packet', commit=self.base)
        flow.save(self.root / '.ktask/session/accepted/IF-001.json', receipt)
        tasks = [dict(id='IF-001', digest='packet', status='TODO')]
        with self.assertRaisesRegex(ValueError, 'cannot be reimplemented'):
            check_dispatch(self.root, tasks, 0, flow.git)
        tasks[0]['digest'] = 'changed'
        with self.assertRaisesRegex(ValueError, 'Accepted prefix changed'):
            accepted_prefix(self.root, tasks, flow.git)

    def test_queue_mutation_during_worker_is_rejected(self):
        task = dict(id='IF-001', digest='packet', scope=['code.txt'])
        policy = dict(branch='feature/test')
        state = flow.begin(self.root, task, policy)
        (self.root / 'code.txt').write_text('after\n')
        self.queue.write_text('[DONE] IF-001 First\n\n---\n\nIF-002 Second\n')
        with self.assertRaisesRegex(ValueError, 'queue'):
            flow.candidate_state(self.root, task, state, policy)

    def test_native_progress_does_not_enter_gameplay_candidate(self):
        task = dict(id='IF-002', digest='packet', scope=['code.txt'])
        policy = dict(branch='feature/test')
        self.queue.write_text('[DONE] IF-001 First\n\n---\n\nIF-002 Second\n')
        state = flow.begin(self.root, task, policy)
        (self.root / 'code.txt').write_text('after\n')
        paths, candidate = flow.candidate_state(self.root, task, state, policy)
        self.assertEqual(['code.txt'], paths)
        self.git('add', '.')
        self.git('commit', '-m', 'next implementation plus prior native progress')
        self.assertEqual(candidate, flow.delivery.committed_candidate(
            self.root, task, self.base, self.git('rev-parse', 'HEAD'), flow.git))
