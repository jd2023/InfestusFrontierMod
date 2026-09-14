# Scoped implementation

{{TASK}}

## Process

1. Read required instructions and owning sections. Use the adapter's baseline;
   preserve unrelated files. Check owner, scope and dependencies. Contract gaps
   are FAILED for coordinator correction, not automatic human questions.
2. Write the narrowest meaningful test FIRST; preserve the failing assertion and
   command output. Missing imports alone are insufficient: once scaffolding
   exists, prove the behavioral assertion detects the missing implementation.
   Exercise required red cases independently: one failure must not skip another.
   Boundary fixtures must otherwise be valid; keep malformed-input/admission-order
   tests separate. Inspect failures: incidental exceptions do not prove a missing
   limit or rule.
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
6. Write evidence, run python3 scripts/ktask_workflow.py check-evidence, and correct
   any rejection before writing the report. DO NOT commit or push. DONE means a candidate ready
   for independent acceptance, not final delivery.

Routine API debugging, test failures, refactoring and tuning are engineering work.
Only a new product choice or external authority can require human input. Do not
silently change progression, scope, model/provider, host software or acceptance
machinery. Do not expand a task because adjacent code could be improved.

## Evidence

Capture actual commands with the launcher, never write execution receipts yourself:

    python3 scripts/ktask_workflow.py record red -- <focused test command>
    python3 scripts/ktask_workflow.py record green -- <focused test command>

Red requires a behavioral failure. Record green after the final source/test edits.
Record game/visual/integration/soak the same way for each required kind; each must run
against the final candidate. Long qualification runs are separate from the fast
full gate. Any source/resource edit invalidates green and qualification receipts.
The original red receipt may survive a repair on the same task/baseline. New tests
need their own demonstrated failure, preserved as additional evidence.
For capture files, pass --artifact <relative-path> before --. The command must
create these files inside the task evidence directory during that run. Visual
receipts require captures. Describe inspection in the report; do not replace the
capture receipt with a notes-only run. The reviewer also inspects the images.
Manifest paths must be captured logs or declared artifacts, not unbound files.
Each phase has ONE current receipt (red.json, green.json, etc.). Recording it again
archives the previous receipt. Only logs/artifacts named by current required-phase
receipts can appear in evidence.json; prior captures remain diagnostic history,
not valid manifest entries. Use one command for all checks in a phase, or reference
only its final capture. The pre-check validates bindings and manifest references;
it does not run tests, invoke models, approve code, commit or push.

Then write .ktask/session/evidence/<task-id>/evidence.json:
{
  "task": "<task-id>",
  "baseline": "<adapter baseline commit>",
  "artifacts": {"rules": ["<recorded green log>"], "game": ["<recorded game log>"]}
}

Paths are relative to this evidence directory and must be nonempty files.
Include every Evidence kind in the packet: visual (recorded captures),
integration (named profiles/results), soak (hardware/duration/measurements).
Preserve focused and adjacent output, not only the full-gate log. Explain why the
red assertion detects the behavior. Schema validity is not substantive proof.

## Report

First nonempty line: KTASK_RESULT: DONE, FAILED or NEEDS_INPUT (choose one).
Then at most five short lines: outcome, evidence path, checks, defect or exact
external blocker. Write at the path supplied by the orchestrator. Detailed evidence
belongs in artifacts, not a verbose completion report.
