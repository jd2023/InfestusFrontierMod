# Implementation and self-repair

{{TASK}}

Complete the intended behavior. Scope paths identify starting points, not file
permissions. Necessary repairs may cross modules and include existing production
code, tests, harnesses, build files, documentation and project configuration.
This authority overrides task wording that limits work to a manifest, fixture or
test-only path. Do not stop because a prerequisite implementation needs repair.

1. Read the owning requirements and inspect the existing implementation.
2. Add meaningful regression tests before fixing behavior. Preserve evidence
   that the tests detect the defect; do not use incidental failures as proof.
3. Implement or repair behavior in its owning module. Keep interfaces simple,
   dependencies inward, and server work bounded. Refactor where needed.
4. Run focused and adjacent checks, plus the required game, visual, integration
   and performance scenarios. Fix failures and repeat affected validation.
   The acceptance hook runs the full gate; run it yourself when useful.
5. Review the complete diff for correctness, placement, simplicity, relevance,
   module isolation, test quality, performance and low-value comments.
6. Save evidence and the native report. Independent acceptance owns commit/push.

Use engineering judgment to resolve technical gaps and balance initial numbers.
Human input is for product decisions or external authority, not file permissions,
API debugging, faulty tests, configuration errors or integration defects.
Preserve intended gameplay and acceptance outcomes. Do not remove coverage,
lower thresholds or fabricate results to make a failure disappear.
Do not edit the live task queue, its content-plan ownership/dependency contracts,
progress markers, or acceptance receipts to redefine the work being accepted.
Do not start a second orchestrator or approve your own work.

## Evidence

Use standard test reports, logs, screenshots and profiler results. The optional
record command can capture a command; phase receipts are NOT required.
Keep artifacts in .ktask/session/evidence/<task-id>/ and write evidence.json:

    {"task":"<task-id>","baseline":"<baseline commit>",
     "artifacts":{"rules":["unit.log"],"integration":["profile-results.json"]}}

Include each Evidence kind requested by the task. List the actual artifacts,
including captures for visual work, and explain commands, assertions, inspected
states and measurements in a short evidence note. Empty output from a successful
command is valid; it is not proof of correctness by itself.
After a repair, rerun affected checks and explain any reused evidence.
Never present stale results as results for changed behavior.
The independent reviewer checks substance and freshness, not receipt ceremony.

## Report

Write KTASK_RESULT: DONE, FAILED or NEEDS_INPUT as the first line at the native
report path. Follow with the outcome, evidence path and any unresolved defect.
DONE means ready for independent acceptance, not permission to commit or mark DONE.
