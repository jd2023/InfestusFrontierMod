# Review

For independent review and validation. Inspect the change and evidence; do not edit, commit or rerun writable checks from a read-only review role.

## Scope and result

Read the task goal, criteria, complete diff and relevant surrounding code. Review what the task delivers, not every future feature in a catalog. Follow VISION where older designs disagree.

Under ktask, return its required structured result. Outside ktask, state APPROVED or REJECTED and list concrete findings. A blocker identifies a location, reachable trigger, consequence and focused correction. Report minor issues separately; speculative risks are not blockers.

On repair rounds, verify earlier findings against current code and evidence. Do not repeat a fixed finding from an old report. Inspect newly changed behavior for regressions.

## Evidence

- Code tasks: inspect the supervisor's check results for the reviewed tree and the focused proof for each criterion. Do not substitute an old green log or executor summary.
- Document tasks: check accuracy, scope, clarity, consistency and references. Do not demand game-test logs or build runs.
- Art tasks: inspect the actual deliverables and relevant images. Do not demand project checks when the category disables them.
- Visual criteria require opening the images. A successful pixel check alone does not establish readability.
- Validation checks the complete goal and criteria, rather than repeating a general code review.

## Applicable checks

- Resources have one owner. Refused work consumes nothing; full outputs pause safely; retries and save/load cannot duplicate results.
- Unknown or corrupt saves remain intact and unavailable, including through manual screens, hoppers, breaking and replacing.
- Chunk availability is checked before access. Searches, queues, allocations, entities and retries respect shared bounds.
- Clients cannot choose unchecked targets or manufacture results. Menus validate current server state, including competing viewers.
- Module boundaries and shared screen primitives remain intact. New behavior does not duplicate an existing implementation or bypass engine rules.
- Automation matches the intended stage. A manual starter stage is not a defect; a promised automatic operation requiring repeated hand input is.
- Recipes, translations, discovery, guide text and usable demo exhibits match the shipped change. Planned abilities are not presented as available.
- Model, selection and collision shapes agree. Assets render clearly; temporary assets are identified in CREDITS. Missing final art alone is not a blocker or an automatic follow-up.
- Tests exercise the changed behavior. Weakened assertions, raised limits or replaced golden images need a concrete justification; a passing build alone is not approval.

Consult ENGINEERING's relevant contracts when needed, without importing its developer workflow into review. Under ktask the supervisor owns all execution and delivery.
