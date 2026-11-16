# Engineering

How to develop and repair this project. Read [AGENTS.md](AGENTS.md) first. [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) holds the technical decisions every change must conform to; this file holds the working rules.

## Scope of a change

- Deliver the authorized outcome and nothing else. Make routine implementation choices yourself.
- Stop and report when the task needs a product decision nobody has made, or needs engine behavior bypassed. Do not invent the authorization.
- Choose the simplest solution that meets the architecture's limits. Build no framework, option or abstraction for a future need.
- Put each rule in the module that owns that behavior. When the task exposes a defect elsewhere, repair it in its owning module, update the callers, add a regression test and continue. Ownership decides where code lives, not which files you may edit.
- Repair the class of failure, not only the reported instance. A defect in one placement path means checking the other placement paths.
- Reuse the existing helper before writing a new one. Do not copy logic.
- Leave unrelated code alone.

## Code

- Run the configured formatter. The build fails on unformatted code and on any compiler warning.
- Keep classes and methods focused and names concrete. Comment only the contract the code cannot express.
- Use records for values. Return an explicit result type for success or refusal; do not signal refusal with null, a boolean or an exception.
- Public APIs return Optional or an empty collection, never null.
- Use checked integer arithmetic for every balance and count.
- Copy mutable item stacks when they cross a value boundary.
- Every limit is a named constant beside the code it bounds.
- Do not suppress warnings. Do not weaken an architecture test, an assertion or a limit to make a change pass.
- Leave no unused code, dead flags or commented-out code.
- Player-facing text goes through translation keys.

## Tests

Use the simplest evidence that actually proves the claim.

| Claim | Evidence |
|---|---|
| A rule gives the right result | A focused unit test in `core` or `ui` |
| Behavior holds at an engine boundary: placement, breaking, sided capabilities, chunk unload, recipes, several players | A GameTest in the test mod |
| Saved state survives | A real save and load in a GameTest. A codec round trip is not enough |
| A limit holds | A test that fails when the limit is exceeded |
| A mechanic scales | Measurement under a dense, representative workload |
| A model or screen looks right | Captures from the relevant states and angles, opened and inspected |

- Put combinations of accounting, shapes, slots and reservations in pure `core` tests. Keep GameTests for what only the engine can show.
- A regression test must fail on the defect it guards against.
- Test one failure condition with a short fixture. Keep only a few complete production routes.
- Wait for the completion condition, not for a padded number of ticks.
- Raising a limit, loosening an assertion or replacing a golden image needs a stated reason.
- A passing build does not prove the behavior. Say what you checked and what you did not.

```bash
./gradlew :core:test :ui:test                      # pure rules and layout
./gradlew test                                     # architecture rules
./gradlew spotlessApply                            # format
./gradlew verifyCaptures -PcaptureOnly=SCENARIO    # one capture scenario
bash scripts/verify.sh                             # the full check
```

`scripts/verify.sh` is the full check; read it for its current steps. Each capture run writes to its own directory under `build/captures/` and prints the path.

Under ktask, run focused checks while developing; the supervisor runs the full check. Document and art tasks have no project checks. Outside ktask, run the checks that fit the change and report their real scope.

Build settings and dependency versions live in the Gradle files. A dependency change needs evidence from the build, a server run and a client run.

## Finishing a feature

New or changed gameplay is complete when it has:

- its recipes and translations;
- its discovery: advancements and the conditions that unlock them;
- an in-game guide entry that describes the shipped behavior and renders recipes from recipe data;
- a labeled, usable exhibit in a demo scenario under `src/testMod/resources/captures/`;
- tests for the changed behavior.

The guide never presents a planned ability as available. Do not describe shipped behavior in a design document; the code, the tests and the guide own it.

```bash
bash scripts/demo.sh                          # demo world with every exhibit
./gradlew runClient                           # own world
./gradlew runClient -PwithJei -PwithCurios    # with the optional mods
./gradlew runServer                           # dedicated server
```

## Art

- For a generated asset, edit its generator under `art/`, run it from the repository root and commit the output. Never patch generated output alone.
- Keep editable model sources beside their generators under `art/`. Image masters and renders stay outside the repository.
- Record prompts, source digests, export settings and provenance in CREDITS.txt.
- A functional temporary asset may ship. Mark it as temporary in CREDITS.txt. Missing final art does not block gameplay work and creates no automatic follow-up; a finishing pass runs only when requested.
- An asset must read clearly in the world and at inventory size.
- Collision and selection shapes match the visible object.
- Inspect the front, back, corners and every working state. No missing textures, depth fighting, open seams or clipping. Animation loops close cleanly.
- A finishing pass also checks that materials, lighting and repetition are consistent across joined surfaces.
- Judge appearance from real captures. A pixel check alone does not establish quality.
