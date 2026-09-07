# Scoped implementation contract

Read the repository instructions and the task's owning specification first.
The task is the scope, not permission to resolve open product decisions. Stop
with NEEDS_INPUT if required choices, authority or external prerequisites are missing.

## Task

{{TASK}}

## Required process

Start at a clean committed checkpoint on the authorized task branch. State the
owning module, outcome, allowed files, non-goals and adjacent risks. Add the
narrowest meaningful failing test before implementing. Keep rules inward, hide
internals and explain any public API growth. Discuss material performance costs
and enforce hard limits; test overload and safe refusal rather than dropping work.

Run focused checks, the interaction/adjacent regression sweep, then
`./.ktask/verify.sh` and any required client, compatibility or performance gates.
Do not treat logs without assertions, a screenshot alone or a successful build as
proof of gameplay. Never weaken verification, rerun until lucky, acknowledge a
HUMAN gate or replace goldens. Review the complete staged diff; commit one coherent
scope only after required gates pass. No push/merge/release without authorization.

## Final structured report

Start with exactly one result line:

KTASK_RESULT: DONE
KTASK_RESULT: FAILED
KTASK_RESULT: NEEDS_INPUT

Choose one, not all three. Follow it with outcome, scope, API/boundary changes,
red/green evidence, full-gate result, adjacent checks, performance bounds, visual
evidence/human approvals where relevant, commit and branch, skipped checks and
remaining risk. NEEDS_INPUT includes one focused question and why it blocks work.
DONE is not allowed if required verification or approval is missing.
