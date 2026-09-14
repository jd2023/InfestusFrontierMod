# Task execution

Canonical packets: `.ktask/tasks.md`. Module interfaces: `ARCHITECTURE.md`.
Worker/context/reviewer/repair prompts and exact model roles are in `.ktask/`.
Do not duplicate these contracts in issue descriptions or another roadmap.

```bash
python3 scripts/ktask_workflow.py validate
python3 scripts/ktask_workflow.py status
python3 scripts/ktask_workflow.py run
python3 scripts/ktask_workflow.py resume
python3 scripts/ktask_workflow.py retry
```

Only run/resume/retry invoke workers. This planning change does not launch them.
Use this launcher, not bare ktask: it creates ignored `.ktask/session/.ktask`
runtime files, pins the plan/prompts, and leaves tracked packets free of status
markers. Existing sessions refuse a changed plan; coordinator migration preserves
accepted receipts and explicitly revalidates dependencies before restarting.
Never clear statuses or delete a session to repeat accepted gameplay work.

## Roles and acceptance

| Role | Initial configuration | Responsibility |
|---|---|---|
| Implementation | gpt-5.6-sol, high | Supplied contract, red-first tests, narrow implementation |
| Repair | gpt-6-astra, high; at most two attempts | Resolve engineering failure and revise candidate |
| Independent review | Fresh gpt-6-astra, high; read-only | Inspect whole diff, boundaries, tests, performance and actual evidence |
| Planning coordinator | Smart model | Research/design, contracts, dependency ordering, re-scoping failures |
| Delivery adapter | No model | Run full gate, validate evidence/verdict, commit and push feature branch |

These are initial role selections, not measured cost or account-access claims.
Actual account/model access must succeed at execution; no paid worker/reviewer run
is part of the offline configuration tests.

```text
worker -> red/green evidence -> authoritative gate -> independent review
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

The external runner owns retries, locks, provider waits and queue state. The project
adapter does not fork or patch ktask. Its Codex wrapper removes the runner's
sandbox-bypass flag and uses workspace-write; review uses read-only, ephemeral
execution and a structured output schema. These CLI options were checked against
[Codex non-interactive documentation](https://learn.chatgpt.com/docs/non-interactive-mode).

## Failure ownership and evidence limits

Normal failures and task ambiguities are engineering work. The resolver repairs
within scope; an exhausted or invalid packet returns to the planning coordinator.
The coordinator refines the contract/baseline and resumes. Only genuine product
choices or new external authority reach the user. No automatic HUMAN placeholders.

Evidence schema validates presence and binding, not honesty or artistic quality.
The independent reviewer must inspect red assertions, actual captures, negative
cases and measurements. Same-user local processes are not a security boundary
against a malicious agent rewriting ignored files. Offline tests validate delivery
control with disposable Git remotes and fake review outcomes; they do not certify
real-model review quality, visual judgment, account access or campaign gameplay.

The full gate must stay offline with respect to AI: no reviewer recursively calls
acceptance from tests. Feature checks join Gradle verifyAll and the existing script.
