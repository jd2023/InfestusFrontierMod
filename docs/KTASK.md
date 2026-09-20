# Task execution

Run from the production repository:

```bash
ktask status
ktask resume
```

`ktask run` starts the queue; `ktask retry` retries a failed task. Ctrl+C stops
the active attempt while preserving its work. Only ktask owns selection, progress
markers, retries, provider waits, locking and the terminal. Project scripts never
launch ktask or select another task. `.ktask/tasks.md` is the single queue.

## Hooks under ktask

| Hook | Responsibility |
|---|---|
| `codex_cmd` → `ktask_workflow.py executor` | Validate the selected packet and accepted prerequisites; pin its checkpoint; execute one worker attempt with scoped permissions |
| Native autoresolution | At most two gpt-6-astra/high repair attempts after the gpt-5.6-sol/high implementation attempt |
| `verification_command` → `ktask_workflow.py accept` | Validate scope/evidence, run the full gate, obtain fresh read-only gpt-6-astra/high review, then commit and push the configured feature branch |

Models and limits live in config.toml and policy.toml. Planning, research and
contract correction belong to the planning session, not a hidden execution loop.
Workers and repairs cannot edit their contracts, process controls or queue markers.
Exhausted retries stop in ktask with evidence; no second orchestrator restarts them.

Review checks correctness, placement, simplicity, scope, boundaries, tests,
comments, performance and evidence. Every check needs concrete supporting evidence;
any unresolved finding rejects the candidate. A successful worker report does not
mean accepted delivery. Only native ktask marks DONE after verification succeeds.

Queue status-only changes are excluded from gameplay fingerprints and included
as metadata in the next delivery commit. Contract edits are never excluded.
A DONE predecessor requires its matching accepted receipt and Git ancestry.
The active attempt pins the queue as well as the packet, baseline and protected
user files; a worker cannot change status markers to pass acceptance.

## Evidence and recovery

```bash
python3 scripts/ktask_workflow.py validate
python3 scripts/ktask_workflow.py scope IF-001
python3 scripts/ktask_workflow.py record red -- <focused test command>
python3 scripts/ktask_workflow.py record green -- <focused test command>
python3 scripts/ktask_workflow.py check-evidence
```

The recorder captures exit codes, bounded logs and generated artifact hashes.
Red binds to the task/checkpoint; green and qualification bind to the exact
candidate. `check-evidence` is read-only and never invokes a model or delivers code.
The prompt specifies evidence.json and required game/visual/integration/soak kinds.
Acceptance rechecks bindings after tests and review. Logs are capped at 16 MiB
and captures at 64 per task. These establish provenance, not artistic quality;
the independent reviewer must inspect actual assertions and client captures.

Reports and runner logs live in `.ktask/queue` and `.ktask/logs`. Attempt bindings,
evidence, review verdicts and delivery receipts live in ignored `.ktask/session`;
there is no nested runtime queue. Do not delete evidence or receipts to retry work.
After a fresh clone, `python3 scripts/ktask_workflow.py restore-receipts` reconstructs
missing receipts from verified published commits, never from DONE text. It does
not launch models, change queue markers or execute tasks.

A delivery intent records the reviewed tree, parent and commit message before
committing. Interrupted commit/push resumes that delivery without running another
implementation or accepting different content. Remote confirmation is mandatory.
No force push, main merge, new remote or public release is allowed. The configured
GitHub CLI credential helper is per-command; no global Git settings are changed.

The guardian only bounds and cleans up the hook's child processes, including
detached descendants. It never retries, advances tasks or prints heartbeat messages.
Worker output passes through to ktask; detailed verification output goes to its
native verification log.

`bash .ktask/verify.sh` runs offline workflow regressions and the authoritative
Minecraft gate. CLI fixtures use disposable repositories, fake models and local
bare remotes; they never execute this project's task queue. They test orchestration
and delivery mechanics, not real-model quality or future gameplay.
