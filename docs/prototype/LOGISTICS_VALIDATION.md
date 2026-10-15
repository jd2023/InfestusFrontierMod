# Logistics validation — 2026-09-07

Scope: the prototype's harvesting → directed veins/stitches → junction → mounted reservoir → consumer loop, its configuration boundaries, and adjacent visual regressions. Work started from clean checkpoint `641b2b9` on `test/logistics-end-to-end-validation`. The separate production checkout was not modified.

## Defects reproduced before correction

- A paused junction still accepted and exported biomass through its scalar capability. Pause now gates the owning endpoint, including retained handles; removed endpoints also reject transfers. An actual legacy pump retains its refused batch and automatically delivers after resume.
- Changing a junction arm from output to input left an old READY route/endpoint on screen. A changed mode now invalidates that arm's diagnostic; pause/resume invalidates all four. No scan is performed just to invalidate a trace.
- A derived vein appearance update could load an unloaded neighboring chunk through vanilla shape propagation. The update now explicitly suppresses neighbor-shape propagation. The loaded-edge test failed on placement before routing even began, then passed after the fix.
- Graphical reloads accumulated old husks because block loading preceded asynchronous saved-entity loading. The new exactly-one-specimen assertion reproduced this contamination. Fixture cleanup/spawning now waits for all four fixture chunks' entity data, with hard readiness/cleanup bounds. This affects only the opt-in disposable lab.

## Added and strengthened tests

Seven new `LogisticsNetworkGameTests` cover:

1. Pause/resume and removal through an already acquired capability, with unchanged buffer contents on refusal.
2. Actual scheduled pump back-pressure during pause and complete delivery after resume.
3. Nine simultaneous native server menus, shared work admission, same-tick rejection, and later retries.
4. Mode-change diagnostic invalidation followed by a fresh direction-mismatch trace.
5. Actual health damage funding biomass, bends and two grafted rises, a bone-meal-grown organ, initially empty raised storage, a closed output, then a descending graft into Aerocyte Bloom ammunition. The assertion conserves yield across patch, junction, reservoir, pending biomass and ammunition. Contact is explicitly dispatched through the block adapter; natural ground contact is separately exercised by the graphical lab.
6. A full destination, broken intermediate vein, block-entity serialization round trip, repair, and automatic complete delivery without manual pulses or loss.
7. An already-loaded/unloaded chunk boundary: derived appearance, forward traversal, reverse traversal and endpoint queries must leave the neighboring chunk unloaded.

The existing downhill item-transport test now opens the actual held-probe console and sends its native menu intent. It no longer calls the obsolete raw empty-hand block handler, which bypassed the current interaction event. Four items still traverse both visible rises in either direction.

Existing adjacent coverage remains in the authoritative gate: reservoir formation/splitting/drops, digestion damage and reward suppression, recipe discovery, armor/Curios contracts, item/fluid transport, bounded route cycles/branches/length, defense work limits, packet validation, GUI synchronization and asset structure.

## Reproduction commands

Run headless profiles separately, then each graphical task separately because they share the disposable Biology Lab save:

```sh
./gradlew prototypeCheck --console=plain
./gradlew prototypeCheck -PwithoutCurios --console=plain
./gradlew runLogisticsShowcase --console=plain
./gradlew runLogisticsShowcase --console=plain
./gradlew runReservoirShowcase --console=plain
./gradlew runSurfaceIntakeShowcase --console=plain
```

The second logistics invocation matters: it tests the same saved-world reload path. Graphical output is in `run/screenshots/`; automated completion is not a substitute for inspecting the images. No golden image is automatically approved.

## Final gate results

Both final `prototypeCheck` runs passed after the fixture correction: **88 unit tests, zero failures/errors/skips, and all 96 required GameTests** in the normal and `-PwithoutCurios` profiles. Data generation, packaged-resource verification and build tasks also passed. The final normal and Curios-absent invocations each completed in approximately 30 seconds on this machine; that is gate wall time, not a gameplay performance benchmark.

## Graphical results

- The corrected six-phase logistics showcase passed twice on the same save. Each run verified exactly one specimen and **114 harvested biomass** in the initially empty raised reservoir before the first capture. World layout, green veins, open/closed apertures, organ/vein consoles and adjacent Mutation Chamber UI were inspected.
- The ten-phase reservoir showcase passed. Front, back, both sides, top, underneath, night glow, empty state, interior and short-organ views were inspected. No new missing-texture, geometry-gap or overlapping-layer defect was identified in these sampled images; no reservoir art/model changes were made in this scope.
- The focused legacy surface-intake showcase passed its live storage-to-defense checks and single capture; the resulting image was inspected.
- Final successful graphical runs had no error-level log entries. The earlier deliberately failing specimen-count run produced a crash report in the disposable lab, as expected; it is not counted as successful validation. Old lab mobs were cleared only within the existing fixed reset area before spawning the replacement specimen.

## Limits of the evidence

- Native server-player/menu fixtures are not nine independent network clients. Real dedicated-server latency, disconnect/reconnect races, claim-mod permissions and adversarial network soak remain open.
- The loaded-edge test and serialization test are separate. They do not establish a complete unload/reload lifecycle for an active cross-chunk network during a server restart.
- Work ceilings and exact conservation are tested; dense-base TPS/FPS, fairness under saturation, sustained chunk-loader farms and third-party capability transaction behavior are not certified.
- Screenshots sample known camera poses at one GUI scale. They cannot prove every angle, shader pack, animation frame or display scale free of artifacts.
- Horizontal arm settings govern the junction's own scheduled routes. The legacy central/bottom capability remains separate; master Pause is the isolation control for all transfers. Flow counters are cumulative scheduled arm totals, not per-side capability accounting or rates.
- Same-reservoir loops through a different multiblock cell, stale diagnostics after changes to a remote route, and richer network-wide diagnostics need dedicated follow-up contracts. This pass does not claim those are solved.

Performance rationale and fixed budgets are recorded in [PERFORMANCE.md](PERFORMANCE.md). Player-facing controls are in [LOGISTICS_CONSOLE.md](LOGISTICS_CONSOLE.md).
