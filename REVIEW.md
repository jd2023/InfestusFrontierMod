# Review

How to review and validate a change. Read [AGENTS.md](AGENTS.md) first. A review role is read-only: inspect the change and its evidence; do not edit, commit or run checks that write.

Judge the change against three things: the task's goal and criteria, [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), and [VISION.md](VISION.md). VISION wins where an older design disagrees.

## Scope

- Read the task goal, its criteria, the complete diff and the surrounding code the diff depends on.
- Review what this task delivers. Work that belongs to a later feature is not a defect of this one.
- A necessary repair in a file the task did not predict is acceptable. Unrelated work is not.
- Validation checks the whole goal and every criterion. It does not repeat a general code review.

## Findings

Sort every finding into one of three kinds.

- **Defect:** required behavior is missing, or the change is demonstrably wrong. This blocks.
- **Risk:** a credible concern that needs a targeted reproduction or measurement before it can block.
- **Improvement:** useful, but not needed for this task. This never blocks.

Rules for a blocking finding:

- Tie it to an explicit requirement or a demonstrated defect. An outcome that can merely be imagined does not block.
- Give its location, the reachable trigger, the consequence and a focused correction.
- Keep severity proportional to the consequence.
- Ordinary vanilla or NeoForge behavior is not a violation. Show the execution path and the extra risk the mod introduces.

Report all findings in one consolidated set. When one path is wrong, inspect the related paths and report them together.

On a repair round:

- Check each earlier finding against the current code and evidence. Do not repeat a finding that has been fixed.
- Inspect the newly changed behavior for regressions.
- Say whether a new finding was introduced by the repair or was missed earlier.

Under ktask, return its structured result. Outside ktask, state APPROVED or REJECTED and list the findings.

## Evidence

- **Code tasks:** inspect the supervisor's check results for the tree under review and the focused proof for each criterion. An old passing log or the executor's summary is not evidence.
- **Document tasks:** check accuracy, scope, clarity, consistency and references. Do not ask for builds or game tests.
- **Art tasks:** inspect the deliverables and their images. Do not ask for project checks when the category disables them.
- A visual criterion requires opening the images. A passing pixel check does not show that something is readable.
- A passing build is not approval. The tests must exercise the changed behavior.
- A scaling claim needs a measurement under a representative workload.
- A persistence claim needs a real save and load, not only a codec round trip.

## What to check

Apply only the checks relevant to the task's category and diff.

- **Architecture.** The change follows docs/ARCHITECTURE.md. Look hardest at: one owner per resource, a refusal that changes nothing, pausing on a full output, chunk checks before world access, shared work limits, unreadable saves kept intact, and the server validating every client intent.
- **Failure cases.** The change holds under the cases listed in ARCHITECTURE's "Every feature survives" that it can reach.
- **Structure.** Module boundaries hold. The change reuses existing helpers and shared screen parts, duplicates no existing implementation and bypasses no engine rule.
- **Scope.** The change adds no feature, option or framework the task did not ask for.
- **Tests.** A regression test fails on the defect it guards. A weakened assertion, raised limit or replaced golden image has a stated reason.
- **Automation.** It matches the intended stage. A deliberately manual stage is not a defect; a promised automatic operation that needs repeated hand input is.
- **Completion.** Recipes, translations, discovery, guide text and the demo exhibit match what ships. Nothing planned is presented as available.
- **Assets.** Model, selection and collision shapes agree, and the asset reads clearly. A temporary asset is marked in CREDITS.txt. Missing final art does not block and creates no automatic follow-up.
- **Documents.** Each fact has one owner. Proposals are marked as proposals and untested numbers as starting values. Shipped behavior is not copied into a design document.

A clean review shows that the change was inspected. It does not guarantee that no defects remain.
