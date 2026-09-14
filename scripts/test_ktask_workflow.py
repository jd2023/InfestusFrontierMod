"""Exercise delivery against disposable local repositories, without AI calls."""

import json
import io
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch, Mock

import ktask_workflow as flow
from ktask_contracts import CHECKS


class DeliveryTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / "work"
        self.root.mkdir()
        self.remote = Path(self.temp.name) / "remote.git"
        subprocess.run(["git", "init", "--bare", str(self.remote)], check=True, capture_output=True)
        self.git("init", "-b", "feature/test")
        self.git("config", "user.name", "Workflow Test")
        self.git("config", "user.email", "test@example.invalid")
        (self.root / ".gitignore").write_text(".ktask/session/\n")
        (self.root / "owned.txt").write_text("before\n")
        self.git("add", ".")
        self.git("commit", "-m", "baseline")
        self.git("remote", "add", "origin", str(self.remote))
        self.git("push", "-u", "origin", "HEAD")
        self.base = self.git("rev-parse", "HEAD")
        self.task = dict(id="IF-001", title="Change owned behavior", digest="packet",
                         scope=["owned.txt"], dependencies=[], Evidence="rules")
        self.policy = dict(branch="feature/test", remote="origin", reviewer_model="fake",
                           reviewer_effort="high", review_timeout=10)
        self.session = self.root / ".ktask/session"
        self.session.mkdir(parents=True)
        self.state = flow.begin(self.root, self.task, self.policy)
        (self.root / "owned.txt").write_text("after\n")

    def git(self, *args):
        return subprocess.run(["git", *args], cwd=self.root, check=True,
                              capture_output=True, text=True).stdout.strip()

    def evidence(self):
        folder = self.session / "evidence/IF-001"
        folder.mkdir(parents=True)
        candidate = flow.candidate_state(self.root, self.task, self.state, self.policy)[1]
        logs = []
        for phase, status in [('red', 1), ('green', 0)]:
            receipt = flow.record(folder, flow.evidence_binding(self.state), candidate, phase,
                                 [sys.executable, '-c', f'print("assertion"); raise SystemExit({status})'], self.root, 10)
            logs.append(receipt['log'])
        (folder / "evidence.json").write_text(json.dumps(dict(
            task="IF-001", baseline=self.base, artifacts={"rules": logs})))

    def review(self, root, task, state, candidate, policy):
        return dict(task=task["id"], candidate=candidate, verdict="accept",
                    checks={key: dict(status="pass", evidence="owned.txt:1 and red/green assertions")
                            for key in CHECKS}, findings=[])

    def accept(self):
        with patch.object(flow, "review_candidate", side_effect=self.review), \
                patch.object(flow, "run_gate") as gate:
            result = flow.accept(self.root, self.task, self.state, self.policy)
            gate.assert_called_once()
            return result

    def preflight(self):
        flow.save(self.session / 'active.json', self.state)
        with patch.object(flow, 'ROOT', self.root), \
                patch.object(flow, 'load_project', return_value=([self.task], self.policy)), \
                patch.object(flow, 'require_session'), \
                patch.object(flow.sys, 'argv', ['ktask_workflow.py', 'check-evidence']), \
                patch.object(flow, 'accept') as deliver, patch.object(flow, 'run_gate') as gate, \
                patch.object(flow, 'review_candidate') as review:
            try:
                flow.main()
            finally:
                deliver.assert_not_called()
                gate.assert_not_called()
                review.assert_not_called()

    def test_evidence_preflight_does_not_deliver_or_change_candidate(self):
        self.evidence()
        status = self.git('status', '--porcelain')
        candidate = flow.candidate_state(self.root, self.task, self.state, self.policy)[1]
        self.preflight()
        self.assertEqual(status, self.git('status', '--porcelain'))
        self.assertEqual(self.base, self.git('rev-parse', 'HEAD'))
        self.assertEqual(candidate, flow.candidate_state(self.root, self.task, self.state, self.policy)[1])
        self.assertFalse((self.session / 'accepted/IF-001.json').exists())

    def test_evidence_preflight_identifies_superseded_capture_before_handoff(self):
        self.evidence()
        folder = self.session / 'evidence/IF-001'
        candidate = flow.candidate_state(self.root, self.task, self.state, self.policy)[1]
        current = flow.record(folder, flow.evidence_binding(self.state), candidate, 'green',
                              [sys.executable, '-c', 'print("current assertion")'], self.root, 10)
        with self.assertRaisesRegex(ValueError, 'Artifact lacks a producing run'):
            self.preflight()
        manifest = json.loads((folder / 'evidence.json').read_text())
        manifest['artifacts']['rules'] = [current['log']]
        flow.save(folder / 'evidence.json', manifest)
        self.preflight()
        self.assertEqual(self.base, self.git('rev-parse', 'HEAD'))
        self.assertFalse((self.session / 'accepted/IF-001.json').exists())

    def test_accepted_change_committed_and_pushed(self):
        self.evidence()
        self.accept()
        self.assertNotEqual(self.base, self.git("rev-parse", "HEAD"))
        self.assertEqual(self.git("rev-parse", "HEAD"),
                         self.git("ls-remote", "origin", "refs/heads/feature/test").split()[0])
        self.assertEqual("", self.git("status", "--porcelain"))

    def test_new_file_accepts_without_a_retry(self):
        self.task['scope'].append('new.txt')
        (self.root / 'new.txt').write_text('new behavior\n')
        self.evidence()
        self.accept()
        self.assertEqual('new behavior', self.git('show', 'HEAD:new.txt'))

    def test_rename_delete_and_staging_preserve_candidate_identity(self):
        self.task['scope'].append('renamed.txt')
        (self.root / 'owned.txt').rename(self.root / 'renamed.txt')
        before = flow.candidate_state(self.root, self.task, self.state, self.policy)
        self.git('add', '-A')
        self.assertEqual(before, flow.candidate_state(self.root, self.task, self.state, self.policy))
        self.evidence()
        self.accept()
        self.assertEqual('after', self.git('show', 'HEAD:renamed.txt'))

    def test_identical_content_rename_accepts(self):
        self.task['scope'].append('renamed.txt')
        (self.root / 'owned.txt').write_text('before\n')
        (self.root / 'owned.txt').rename(self.root / 'renamed.txt')
        before = flow.candidate_state(self.root, self.task, self.state, self.policy)
        self.git('add', '-A')
        self.assertEqual(before, flow.candidate_state(self.root, self.task, self.state, self.policy))
        self.evidence()
        self.accept()

    def test_git_owner_execute_bit_is_not_group_execute_bit(self):
        path = self.root / 'owned.txt'
        path.chmod(0o755)
        before = flow.candidate_state(self.root, self.task, self.state, self.policy)
        path.chmod(0o655)
        self.assertNotEqual(before, flow.candidate_state(self.root, self.task, self.state, self.policy))

    def test_executable_bit_changes_candidate_identity(self):
        before = flow.candidate_state(self.root, self.task, self.state, self.policy)
        (self.root / 'owned.txt').chmod(0o755)
        self.assertNotEqual(before, flow.candidate_state(self.root, self.task, self.state, self.policy))

    def test_symlink_retarget_changes_candidate_identity(self):
        self.task['scope'].append('link')
        link = self.root / 'link'
        link.symlink_to('owned.txt')
        before = flow.candidate_state(self.root, self.task, self.state, self.policy)
        link.unlink()
        link.symlink_to('missing.txt')
        self.assertNotEqual(before, flow.candidate_state(self.root, self.task, self.state, self.policy))

    def test_failed_gate_cannot_commit_or_push(self):
        self.evidence()
        with patch.object(flow, "run_gate", side_effect=ValueError("gate failed")):
            with self.assertRaisesRegex(ValueError, "gate failed"):
                flow.accept(self.root, self.task, self.state, self.policy)
        self.assertEqual(self.base, self.git("rev-parse", "HEAD"))

    def test_missing_red_evidence_cannot_deliver(self):
        with self.assertRaises(ValueError):
            self.accept()
        self.assertEqual(self.base, self.git("rev-parse", "HEAD"))

    def test_reviewer_rejection_cannot_deliver(self):
        self.evidence()
        with patch.object(flow, "run_gate"), patch.object(flow, "review_candidate", return_value={}):
            with self.assertRaises(ValueError):
                flow.accept(self.root, self.task, self.state, self.policy)
        self.assertEqual(self.base, self.git("rev-parse", "HEAD"))

    def test_review_time_mutation_invalidates_approval(self):
        self.evidence()
        def mutating_review(*args):
            result = self.review(*args)
            (self.root / "owned.txt").write_text("changed during review\n")
            return result
        with patch.object(flow, "run_gate"), patch.object(flow, "review_candidate", side_effect=mutating_review):
            with self.assertRaisesRegex(ValueError, "changed"):
                flow.accept(self.root, self.task, self.state, self.policy)
        self.assertEqual(self.base, self.git("rev-parse", "HEAD"))

    def test_review_time_evidence_change_invalidates_approval(self):
        self.evidence()
        def changed_evidence(*args):
            result = self.review(*args)
            folder = self.session / 'evidence/IF-001'
            receipt = json.loads((folder / 'red.json').read_text())
            (folder / receipt['log']).write_text("different evidence\n")
            return result
        with patch.object(flow, "run_gate"), patch.object(flow, "review_candidate", side_effect=changed_evidence):
            with self.assertRaisesRegex(ValueError, "Changed red log"):
                flow.accept(self.root, self.task, self.state, self.policy)
        self.assertEqual(self.base, self.git("rev-parse", "HEAD"))

    def test_push_retry_reuses_accepted_commit_without_reimplementation(self):
        self.evidence()
        real_git = flow.git
        def failed_push(root, *args):
            if args[0] == "push":
                raise ValueError("remote temporarily unavailable")
            return real_git(root, *args)
        with patch.object(flow, "git", side_effect=failed_push):
            with self.assertRaisesRegex(ValueError, "remote temporarily unavailable"):
                self.accept()
        accepted = self.git("rev-parse", "HEAD")
        self.assertNotEqual(self.base, accepted)
        with patch.object(flow, "run_gate") as gate, patch.object(flow, "review_candidate") as review:
            flow.accept(self.root, self.task, self.state, self.policy)
            gate.assert_not_called()
            review.assert_not_called()
        self.assertEqual(accepted, self.git("rev-parse", "HEAD"))
        self.assertTrue((self.session / "accepted/IF-001.json").exists())

    def test_configured_github_credentials_are_scoped_to_push_command(self):
        self.evidence()
        self.policy["github_cli_credentials"] = True
        with patch.object(flow, "git", wraps=flow.git) as commands:
            self.accept()
        push = next(call.args[1:] for call in commands.call_args_list if "push" in call.args)
        self.assertEqual(("-c", "credential.helper=", "-c",
                          "credential.helper=!gh auth git-credential"), push[:4])
        self.assertNotIn("credential.helper", self.git("config", "--local", "--list"))

    def test_preexisting_untracked_user_file_is_preserved(self):
        self.git("restore", "owned.txt")
        user_file = self.root / "notes.pdf"
        user_file.write_bytes(b"user document")
        self.state = flow.begin(self.root, self.task, self.policy)
        (self.root / "owned.txt").write_text("after\n")
        self.evidence()
        self.accept()
        self.assertEqual(b"user document", user_file.read_bytes())
        self.assertEqual("?? notes.pdf", self.git("status", "--porcelain"))

    def test_foreign_change_and_worker_commit_are_refused(self):
        self.evidence()
        (self.root / "foreign.txt").write_text("user file\n")
        with self.assertRaises(ValueError):
            self.accept()
        self.git("add", "owned.txt")
        self.git("commit", "-m", "unreviewed")
        with self.assertRaises(ValueError):
            self.accept()

    def test_dirty_start_and_wrong_branch_refused(self):
        with self.assertRaises(ValueError):
            flow.begin(self.root, self.task, self.policy)
        with self.assertRaises(ValueError):
            flow.begin(self.root, self.task, dict(self.policy, branch="main"))

    def test_executor_uses_supplied_packet_and_removes_sandbox_bypass(self):
        self.task["body"] = "IF-001 Change owned behavior"
        flow.save(self.session / "active.json", self.state)
        prompt = "[Orchestrator context] Task 1 of 1 (attempt 1).\n" + self.task["body"]
        with patch.object(flow, "require_session"), patch.object(flow.sys, "stdin", io.StringIO(prompt)), \
                patch.object(flow, "run_model") as execute:
            flow.executor(self.root, [self.task], self.policy,
                          ["exec", "--dangerously-bypass-approvals-and-sandbox", "-"])
        argv = execute.call_args.args[0]
        self.assertIn('--ignore-user-config', argv)
        self.assertIn('workspace-write', argv)
        self.assertIn('sandbox_workspace_write.network_access=true', argv)
        self.assertNotIn('--dangerously-bypass-approvals-and-sandbox', argv)
        self.assertIn(self.base, execute.call_args.args[3])

    def test_dependency_receipt_required_before_worker_starts(self):
        self.task.update(body="IF-002 Consumer", id="IF-002", dependencies=["IF-001"])
        prompt = "[Orchestrator context] Task 1 of 1 (attempt 1).\n" + self.task["body"]
        with patch.object(flow, "require_session"), patch.object(flow.sys, "stdin", io.StringIO(prompt)), \
                patch.object(flow, "run_model") as execute:
            with self.assertRaisesRegex(ValueError, "Unaccepted dependency"):
                flow.executor(self.root, [self.task], self.policy, ["exec", "-"])
        execute.assert_not_called()

    def test_mismatched_runtime_packet_refused_before_worker(self):
        self.task["body"] = "IF-001 Change owned behavior"
        flow.save(self.session / "active.json", self.state)
        prompt = "[Orchestrator context] Task 1 of 1 (attempt 1).\nIF-001 Wrong contract"
        with patch.object(flow, "require_session"), patch.object(flow.sys, "stdin", io.StringIO(prompt)), \
                patch.object(flow, "run_model") as execute:
            with self.assertRaisesRegex(ValueError, "packet"):
                flow.executor(self.root, [self.task], self.policy, ["exec", "-"])
        execute.assert_not_called()

    def test_subprocess_stays_in_ktasks_kill_group(self):
        child = Mock(returncode=0, stdin=None, stdout=None)
        child.poll.return_value = 0
        with patch.object(flow.os, "getpgrp", return_value=100), \
                patch.object(flow.os, "getpid", return_value=100), \
                patch.object(flow.subprocess, "Popen", return_value=child) as spawn:
            flow.run(["fake-cli"], self.root, 1)
        self.assertFalse(spawn.call_args.kwargs["start_new_session"])


if __name__ == "__main__":
    unittest.main()
