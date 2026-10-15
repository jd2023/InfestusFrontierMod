# Testing

`bash scripts/verify.sh` is the gate. It runs `./gradlew verifyAll` and keeps the log in `build/verification/full-gate.log`.
Gradle decides pass or fail. The script does not parse log text.

## Layers

| Layer | Command | Proves |
|---|---|---|
| Pure tests | `./gradlew :core:test :ui:test` | Rules, limits, edge cases. Milliseconds, no Minecraft |
| Boundaries | `:core:verifyBoundary`, `:ui:verifyBoundary` | Those modules have an empty classpath |
| Distribution | `verifyDistribution` | The JAR holds the entry point, `core`, `ui`, expanded metadata, and no test mod |
| GameTests | `runGameTestServer` | Server-side behavior across real ticks, in `runs/gametest` |
| Client captures | per feature task | What the player sees. A reviewer looks at the image |
| Manual play | `runClient`, `runServer` | Feel and usability. Owner only, at milestone gates |

## Rules

- Every behavior gets a test that fails without it. Write the test first.
- Every reviewer finding gets a regression test.
- Test save and reload through a real save, not only the codec.
- Shared-budget tests isolate one fixture, then test contention separately.
- A flaky test is a defect to investigate. Never rerun until green. Never delete the assertion.
- Test fixtures and the test mod stay in `src/testMod`. They never ship.
- Use disposable worlds under `runs/`. Never copy, delete or rebuild a personal save.
- Run graphical and headless runs as separate Gradle invocations.

## Not covered yet

Client screenshots, recipe and menu tests, optional-mod profiles, multiplayer soak, crash recovery, measured TPS and FPS on a dense base. Each arrives with the feature that needs it.
