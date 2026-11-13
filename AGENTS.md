# Repository rules

## Read by role

All roles follow this file and [VISION.md](VISION.md).

- Execution and repair: [ENGINEERING.md](ENGINEERING.md), then only the design sections relevant to the task.
- Review: [REVIEW.md](REVIEW.md), with engineering or design sections needed to check a claim.
- Validation: REVIEW's acceptance and evidence rules, applied to the task's category.
- Planning: [ROADMAP.md](ROADMAP.md). Put concrete implementation work in ktask.

VISION owns product direction. ROADMAP owns milestones and status. Older specifications do not override them. ROADMAP_OLD and docs/archive are historical references, not instructions. Do not read them unless the task needs history.

## Git

- Work on `v4`; push only to `origin/v4`. Never force-push, push to main, create a remote, tag or release.
- Every commit uses Illya Yalovyy <1623643+IllyaYalovyy@users.noreply.github.com> as its sole author. Verify the effective author. Do not bypass hooks or add Co-authored-by or generated-by trailers.
- Use a short imperative commit subject. Stage named files, never `git add -A`.
- Preserve unrelated work. Do not stash or commit the owner's changes without authorization. Never commit build/, runs/, .ktask/ or IDE files.

## Execution

Ktask owns orchestration, checks, independent review, commits and delivery for its tasks. Follow its role and output protocol. Do not run a competing manual loop. Keep existing model defaults; never choose Fable for execution.

Outside ktask, implement the authorized change, run appropriate checks, and obtain independent review before pushing implementation changes. Do not leave background work running after the task.

Use simple technical solutions. Make routine implementation choices within scope. Report a missing product decision or a need to bypass engine behavior; do not invent authorization to change either.

## Documentation

Use short, concrete language. Keep one authoritative location for each fact. Update existing documents instead of creating permanent reports or sprint files. Human decisions define direction; implementation notes do not silently add features. Mark undecided designs as proposals and untested tuning as starting values.
