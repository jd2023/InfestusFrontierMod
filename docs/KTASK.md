# Task execution

Run `ktask resume` from InfestusFrontierModV3_dev.
ktask alone owns task selection, progress, retries, provider waits, locking and
the terminal. Project hooks never launch another queue.

## Responsibilities

- Planning defines gameplay, architecture and task outcomes.
- Workers implement and repair, including necessary changes across modules,
  tests, harnesses, build files, documentation and project configuration.
- Independent review checks the complete diff and actual validation evidence.
- Acceptance runs the full gate, reviews, commits and pushes the feature branch.

Scope paths are navigation hints, not permissions. A qualification task may repair
the implementation or evaluator it qualifies. Repair in the owning module and
rerun affected checks. No separate coordinator task is required for that repair.
This rule overrides manifest-only and test-only editing restrictions in packets.

Do not change gameplay requirements, fake success, remove necessary coverage or
lower performance thresholds to pass. Review enforces relevance and quality;
there is no cross-module file allowlist or whole-project editing freeze.

## Evidence and settings

Use standard test reports, logs, captures and measurements. Index the required
evidence kinds in .ktask/session/evidence/<task-id>/evidence.json.
The recorder is optional; acceptance does not require phase receipts or combine
all qualification runs into one command. The reviewer verifies meaningful red
tests, current green results, required scenarios and artifact freshness.

The current review uses the task baseline's reviewer prompt/model, so proposed
review changes cannot approve themselves. Project configuration may be repaired;
committed hook settings are used while pending settings are edited. Native ktask
reads runner settings at run startup; those changes take effect on its next run.

## Recovery and delivery

Keep the live queue and its status markers under ktask's control. content-plan.json
defines the queue's required content and dependencies, not implementation settings;
it cannot be rewritten during execution to redefine task acceptance. Do not rewrite
accepted task records or lower content-coverage obligations to claim completion.
An unfinished candidate remains in the working tree for native repair/resume.
Do not discard unrelated user files or ordinary Minecraft saves.

Delivery is recoverable across commit/push interruption and requires remote
confirmation. No force push, main merge, release or new remote is authorized.
After a fresh clone, `python3 scripts/ktask_workflow.py restore-receipts`
reconstructs delivery receipts from verified published commits.

`bash .ktask/verify.sh` is the authoritative gate. Workflow CLI regression
fixtures use temporary repositories, fake model executables and local remotes;
they never execute this project's tasks. They test delivery mechanics, not AI
quality or Minecraft gameplay.
