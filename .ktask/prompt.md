# Scoped implementation

{{TASK}}

## Process

1. Read required instructions and owning sections. Use the adapter's baseline;
   preserve unrelated files. Check owner, scope and dependencies. Contract gaps
   are FAILED for coordinator correction, not automatic human questions.
2. Write the narrowest meaningful test FIRST; preserve the failing assertion and
   command output. Missing imports alone are insufficient: once scaffolding
   exists, prove the behavioral assertion detects the missing implementation.
3. Implement the supplied contract in its owning module. Keep public APIs small,
   dependencies inward and acyclic; do not duplicate UI, transport or persistence
   infrastructure. Test shared limits, refusal, concurrent use and reload.
4. Run focused tests, adjacent regressions, bash .ktask/verify.sh and every packet
   Evidence kind. Game means actual Minecraft assertions; visual means client
   captures inspected from relevant angles/states; soak means measured data.
   A build, registration test or uninspected screenshot cannot substitute.
5. Review the entire diff: correctness, placement, simplicity, scope, isolation,
   test quality and performance. Refactor and repeat tests. Remove narration,
   obsolete notes and comments compensating for confusing structure. Useful API
   documentation remains. Never remove assertions or bless goldens to get green.
6. Write evidence and report. DO NOT commit or push. DONE means a candidate ready
   for independent acceptance, not final delivery.

Routine API debugging, test failures, refactoring and tuning are engineering work.
Only a new product choice or external authority can require human input. Do not
silently change progression, scope, model/provider, host software or acceptance
machinery. Do not expand a task because adjacent code could be improved.

## Evidence

Write .ktask/session/evidence/<task-id>/evidence.json:
{
  "task": "<task-id>",
  "baseline": "<adapter baseline commit>",
  "red": {"command": ["<executable>", "<arg>"], "exit": 1, "log": "red.log"},
  "green": {"command": ["<executable>", "<arg>"], "exit": 0, "log": "green.log"},
  "artifacts": {"rules": ["green.log"], "game": ["gametest.log"]}
}

Paths are relative to this evidence directory and must be nonempty files.
Include every Evidence kind in the packet: visual (captures and inspection notes),
integration (named profiles/results), soak (hardware/duration/measurements).
Preserve focused and adjacent output, not only the full-gate log. Explain why the
red assertion detects the behavior. Schema validity is not substantive proof.

## Report

First nonempty line: KTASK_RESULT: DONE, FAILED or NEEDS_INPUT (choose one).
Then at most five short lines: outcome, evidence path, checks, defect or exact
external blocker. Write at the path supplied by the orchestrator. Detailed evidence
belongs in artifacts, not a verbose completion report.
