"""Exercise the installed ktask CLI with real hooks, fake models and local Git only."""

import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import tempfile
import time
import unittest

import ktask_workflow as flow

SCRIPTS = Path(__file__).resolve().parent


class NativeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='infestus-ktask-fixture-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / 'work'
        self.root.mkdir()
        self.remote = Path(self.temp.name) / 'remote.git'
        subprocess.run(['git', 'init', '--bare', str(self.remote)], check=True, capture_output=True)
        self.git('init', '-b', 'feature/test')
        self.git('config', 'user.name', 'Native Fixture')
        self.git('config', 'user.email', 'fixture@example.invalid')
        self.git('remote', 'add', 'origin', str(self.remote))
        (self.root / 'scripts').mkdir()
        for source in SCRIPTS.glob('ktask_*.py'):
            shutil.copyfile(source, self.root / 'scripts' / source.name)
        binary = Path(self.temp.name) / 'bin'
        binary.mkdir()
        fake = binary / 'codex'
        shutil.copyfile(SCRIPTS / 'ktask_fixtures/fake_codex.py', fake)
        fake.chmod(0o755)
        self.env = dict(os.environ, PATH=str(binary) + os.pathsep + os.environ['PATH'],
                        KTASK_TEST_ROOT=str(self.root), TERM='xterm-256color', NO_COLOR='1')
        self.env.pop('INFESTUS_TASK_GROUP', None)
        self.runner = shutil.which('ktask')
        self.assertIsNotNone(self.runner, 'The native ktask CLI is required for integration tests')
        (self.root / '.ktask').mkdir()
        (self.root / '.gitignore').write_text('.ktask/session/\n.ktask/logs/\n.ktask/queue/\n.ktask/*.lock\n__pycache__/\n')
        (self.root / '.ktask/config.toml').write_text('''project_dir = "."
executor = "codex"
codex_cmd = "python3 scripts/ktask_workflow.py executor"
model = "fixture-worker"
model_reasoning_effort = "high"
autoresolve_attempts = 2
autoresolve_model = "fixture-repair"
verify_task_report = true
verification_command = ["python3", "scripts/ktask_workflow.py", "accept"]
verification_timeout = 60
timeout = 120
idle_timeout = 30
limit_auto_wait = false
''')
        (self.root / '.ktask/policy.toml').write_text('''branch = "feature/test"
remote = "origin"
reviewer_model = "fixture-review"
reviewer_effort = "high"
review_timeout = 10
worker_timeout = 60
''')
        (self.root / '.ktask/content-plan.json').write_text(json.dumps(dict(
            items={}, mutations={}, services={}, requires={}, excluded_items=[])))
        for name in ('context.md', 'review.md', 'autoresolve.md'):
            shutil.copyfile(SCRIPTS.parent / '.ktask' / name, self.root / '.ktask' / name)
        (self.root / '.ktask/prompt.md').write_text('{{TASK}}\n')
        (self.root / '.ktask/verify.sh').write_text('''#!/bin/sh
test -s owned.txt
test ! -e .ktask/session/fail-gate
''')
        (self.root / 'owned.txt').write_text('before')
        self.prepare(1)

    def git(self, *args):
        return flow.git(self.root, *args)

    def prepare(self, count):
        packets = []
        for index in range(1, count + 1):
            identity = f'IF-{index:03}'
            packets.append(f'''{identity} Produce result {index}
Milestone: M0
Owner: foundation
Depends: {f'IF-{index-1:03}' if index > 1 else 'none'}
Spec: docs/ARCHITECTURE.md
Blocks: none
Scope: ["owned.txt", "assert_result.py"]
Contract: write {identity} once
Red: result missing
Accept: exact result after independent review
Bounds: one file
Evidence: rules''')
        (self.root / '.ktask/tasks.md').write_text('\n\n---\n\n'.join(packets) + '\n')
        self.git('add', '.')
        self.git('commit', '-m', f'Prepare {count}-task fixture')
        self.git('push', 'origin', 'HEAD')

    def run_native(self, command='resume', scenario='success'):
        args = [command] if isinstance(command, str) else command
        result = subprocess.run([self.runner, *args], cwd=self.root,
                                env=dict(self.env, KTASK_TEST_SCENARIO=scenario),
                                start_new_session=True, capture_output=True, text=True, timeout=45)
        return result

    def events(self):
        path = self.root / '.ktask/session/fixture-events.jsonl'
        return [json.loads(line) for line in path.read_text().splitlines()]

    def assert_delivered(self, count):
        queue = (self.root / '.ktask/tasks.md').read_text()
        self.assertEqual(count, queue.count('[DONE]'))
        self.assertEqual(self.git('rev-parse', 'HEAD'), self.git('ls-remote', 'origin', 'refs/heads/feature/test').split()[0])
        for index in range(1, count + 1):
            self.assertTrue((self.root / f'.ktask/session/accepted/IF-{index:03}.json').is_file())
            self.assertTrue((self.root / f'.ktask/queue/report-{index}.md').read_text().startswith('KTASK_RESULT: DONE'))
        self.assertFalse((self.root / '.ktask/session/.ktask').exists())

    def test_resume_from_clean_root_delivers_whole_queue_and_streams_output(self):
        self.prepare(2)
        result = self.run_native()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assert_delivered(2)
        self.assertEqual(['worker', 'review', 'worker', 'review'], [row['role'] for row in self.events()])
        self.assertNotIn('Task process still running', result.stdout)
        self.assertIn('fixture worker progress', (self.root / '.ktask/logs/task-1.log').read_text())
        self.assertIn('Task 2 of 2', (self.root / '.ktask/queue/current-task.md').read_text())

    def test_failed_assertion_uses_native_repair(self):
        result = self.run_native(scenario='failed-test')
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assert_delivered(1)
        self.assertEqual(['worker', 'worker', 'review'], [row['role'] for row in self.events()])
        self.assertIn('automatic resolver fixture-repair', result.stdout)

    def test_rejected_review_requires_repair_and_fresh_review(self):
        result = self.run_native(scenario='rejected-review')
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assert_delivered(1)
        self.assertEqual(['worker', 'review', 'worker', 'review'], [row['role'] for row in self.events()])

    def test_failed_gate_cannot_commit_or_advance(self):
        session = self.root / '.ktask/session'
        session.mkdir()
        (session / 'fail-gate').touch()
        before = self.git('rev-parse', 'HEAD')
        result = self.run_native()
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(before, self.git('rev-parse', 'HEAD'))
        self.assertNotIn('[DONE]', (self.root / '.ktask/tasks.md').read_text())
        self.assertNotIn('review', [row['role'] for row in self.events()])

    def test_skipped_dependency_reports_failure_without_invoking_worker(self):
        self.prepare(2)
        result = self.run_native(['run', '--start', '2'])
        self.assertNotEqual(0, result.returncode)
        report = (self.root / '.ktask/queue/report-2.md').read_text()
        self.assertTrue(report.startswith('KTASK_RESULT: FAILED'))
        self.assertIn('Unaccepted dependency', report)
        self.assertNotIn('No report was written', result.stdout)
        self.assertFalse((self.root / '.ktask/session/fixture-events.jsonl').exists())

    def test_native_input_pause_resolve_and_resume(self):
        result = self.run_native(scenario='needs-input')
        self.assertNotEqual(0, result.returncode)
        self.assertEqual('INPUT', flow.load_project(self.root)[0][0]['status'])
        before = self.events()
        self.assertNotEqual(0, self.run_native().returncode)
        self.assertEqual(before, self.events())
        result = self.run_native(['resolve', '1', '--note', 'Use the fixture outcome.'])
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        result = self.run_native()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assert_delivered(1)

    def test_push_retry_does_not_reimplement_reviewed_commit(self):
        reject = self.remote / 'reject'
        reject.touch()
        hook = self.remote / 'hooks/pre-receive'
        hook.write_text('#!/bin/sh\ntest ! -e "' + str(reject) + '"\n')
        hook.chmod(0o755)
        result = self.run_native()
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(['worker', 'review'], [row['role'] for row in self.events()])
        commit = self.git('rev-parse', 'HEAD')
        self.assertFalse((self.root / '.ktask/session/accepted/IF-001.json').exists())
        reject.unlink()
        result = self.run_native('retry')
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assert_delivered(1)
        self.assertEqual(commit, self.git('rev-parse', 'HEAD'))
        self.assertEqual(['worker', 'review'], [row['role'] for row in self.events()])

    def test_interrupt_preserves_work_reaps_detached_child_and_resumes(self):
        output = self.root / '.ktask/session'
        output.mkdir()
        with (output / 'terminal.log').open('w+') as log:
            process = subprocess.Popen([self.runner, 'resume'], cwd=self.root,
                env=dict(self.env, KTASK_TEST_SCENARIO='interrupt'), start_new_session=True,
                stdout=log, stderr=subprocess.STDOUT)
            try:
                deadline = time.monotonic() + 15
                while not (output / 'fixture-waiting').exists() and time.monotonic() < deadline:
                    self.assertIsNone(process.poll())
                    time.sleep(0.05)
                self.assertTrue((output / 'fixture-waiting').exists())
                process.send_signal(signal.SIGINT)
                self.assertEqual(130, process.wait(timeout=15))
            finally:
                if process.poll() is None:
                    process.send_signal(signal.SIGTERM)
                    process.wait(timeout=15)
        self.assertEqual('unfinished', (self.root / 'owned.txt').read_text())
        self.assertNotIn('[FAIL]', (self.root / '.ktask/tasks.md').read_text())
        pid = (output / 'fixture-child.pid').read_text()
        self.assertFalse(Path(f'/proc/{pid}').exists(), 'Detached child must be reaped')
        result = self.run_native()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assert_delivered(1)
