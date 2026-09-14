# Task execution

Canonical packets: `.ktask/tasks.md`. Module interfaces: `ARCHITECTURE.md`.
Worker/context/reviewer/repair prompts and exact model roles are in `.ktask/`.
Do not duplicate these contracts in issue descriptions or another roadmap.

```bash
python3 scripts/ktask_workflow.py validate
python3 scripts/ktask_workflow.py status
python3 scripts/ktask_workflow.py check-evidence
python3 scripts/ktask_workflow.py run
python3 scripts/ktask_workflow.py resume
python3 scripts/ktask_workflow.py retry
python3 scripts/ktask_workflow.py reconcile
python3 scripts/ktask_workflow.py run --through IF-094
```

Only run/resume/retry invoke models. --through stops after the named accepted task.
check-evidence validates an active candidate's receipts without tests, review or
delivery. Workers run it before submitting DONE; it is not acceptance.
Use this launcher, not bare ktask: it creates ignored `.ktask/session/.ktask`
runtime files, pins the plan/prompts, and leaves tracked packets free of status
markers. Reconciliation preserves accepted receipts, verifies their unchanged
contracts and commit ancestry, and regenerates the accepted prefix plus one task.
Accepted work cannot be removed, reordered or silently marked pending.
Never clear statuses or delete a session to repeat accepted gameplay work.

## Roles and acceptance

| Role | Initial configuration | Responsibility |
|---|---|---|
| Implementation | gpt-5.6-sol, high | Supplied contract, red-first tests, narrow implementation |
| Repair | gpt-6-astra, high; at most two attempts | Resolve engineering failure and revise candidate |
| Independent review | Fresh gpt-6-astra, high; read-only | Inspect whole diff, boundaries, tests, performance and actual evidence |
| Planning coordinator | Fresh gpt-6-astra, high | Readiness before dispatch; scoped plan correction after exhausted repairs |
| Delivery adapter | No model | Run full gate, validate evidence/verdict, commit and push feature branch |

Model selections are not cost guarantees. Offline tests never launch models.

```text
readiness -> worker -> captured evidence -> authoritative gate -> independent review
                                                  ├─ reject -> bounded smart repair -> repeat
                                                  └─ accept -> commit -> push/confirm -> next task
```

ktask itself executes the configured verification command after a DONE report.
The project adapter supplies semantic review for every successful candidate,
including stronger-model repairs. Acceptance binds task packet, baseline, branch
and content digest. It rejects out-of-scope files, changed checkpoints, missing
evidence, stale verdicts, incomplete review checks and changes during testing/review.
Workers cannot edit process-control files inside ordinary task scopes.

Commit/push happen only after acceptance. No force push, merge to main, remote
creation or public release. Remote confirmation is required before advancement.
A push failure retains the accepted local commit and retries its delivery without
reimplementing the task. A crash between commit and receipt write stops for
coordinator recovery; it never guesses and duplicates work.

Delivery uses the existing authenticated GitHub CLI as a per-command credential
helper when enabled in policy.toml. It does not change global/local Git settings,
switch accounts or initiate login. Missing repository access remains an external
authentication failure; the accepted local commit is retained.

The external runner owns worker retries and provider waits. The supervisor admits
one pending packet at a time under a project lock. No fork or patch of ktask.
Workers use workspace-write, explicit network access and the named Gradle cache;
reviews use read-only. Both ignore user config, retain existing authentication and
disable interactive approvals. User/project execpolicy rules still apply. Settings follow
[Codex non-interactive documentation](https://learn.chatgpt.com/docs/non-interactive-mode).

## Failure ownership and evidence limits

The supervisor calls the coordinator when readiness fails or worker/repair attempts
are exhausted. It parks only the failed candidate in a retained Git stash, leaving
unrelated files untouched. Planning edits are limited to existing contracts and
gameplay specifications, pass the full gate and fresh independent review, then
commit/push. A diagnosis can also restart repair without changing the plan.
The candidate is restored when its task is next; intervening prerequisites can run.
Two coordinator cycles/task bound costs. Provider/access failures preserve progress.
Interrupted stash application stops with the recovery receipt and stash intact;
never delete a session or stash to bypass recovery checks.
Interrupted planning resumes before parsing its unfinished task edits. Provider and
access pauses do not consume engineering retries. On Linux, a process guardian
forwards cancellation and reaps detached workers before releasing the project lock.

The record command captures actual exit codes, logs and generated artifact hashes.
Green and qualification receipts bind to exact candidate content; red binds to
task/baseline. A failed rerun invalidates the prior phase receipt. Logs are limited
to 16 MiB and 64 captures/task. Review checks require supporting evidence.
These bindings establish provenance, not honesty or artistic quality.
The independent reviewer must inspect red assertions, actual captures, negative
cases and measurements. Same-user local processes are not a security boundary
against a malicious agent rewriting ignored files. Offline tests validate delivery
control with disposable Git remotes and fake review outcomes; they do not certify
real-model review quality, visual judgment, account access or campaign gameplay.

The full gate stays offline with respect to AI. Fast regression checks join Gradle
verifyAll; the adapter allows 30 minutes. Client/multiplayer/soak qualification uses
separate recorded commands, up to two hours for soak. Any candidate edit invalidates
qualification. Acceptance requires both classes; long soaks do not run inside
every fast-gate invocation. No model is invoked recursively from a test.
