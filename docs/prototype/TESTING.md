# Testing strategy

## Burrowing recovery and ten-second window — 2026-09-09

This revision supersedes the permanent-lock behavior in the original experiment
below. Three native regressions first failed against the old implementation:
rock hardened before ten seconds, release had no traversal cooldown, and the old
saved lock survived Spectator mode. They now test a ten-second lease, native
suffocation followed by successful fueled reactivation **inside** hardened rock,
a native two-second release cooldown with zero cost on refused retries, refusal
with an empty reserve, and Spectator escape → Creative re-entry using the same
equipment. Legacy saved lock flags are removed without requiring a new world.

The full gate contains **105 unit tests and 145 required GameTests**. Lease unit
tests assert the new absolute deadlines while retaining capacity, fair review and
serialization coverage. Six-axis/no-flight, lighting, terrain protection,
restoration, shared admission and the existing armor/mining tests remain adjacent
regressions. The client showcase adds an eighth phase: actual held-use input
reopens hardened rock after a native suffocation hit, before testing open-air
refusal. Longer safe stopping windows are a documented future progression axis,
not implemented upgrade tiers or production balance certification.

Validation: `prototypeCheck` passes both normally and with `-PwithoutCurios`
(105 unit tests, 145 required GameTests). The updated eight-phase graphical run
could not open the save because the owner's running lab held `session.lock`.
Only the attempted showcase process was stopped; the owner's game was left alone.
The added restart capture therefore still needs a rerun after closing that session;
the older seven screenshots are not evidence for this revision.

## Dangerous burrowing — 2026-09-09

The initial lease test failed before its domain owner existed. Native red checks
then caught fluid displacement of temporary rock; the proxy now explicitly refuses
liquids. A suffocation check initially hit vanilla new-player invulnerability;
the fixture now ages that native protection before testing actual damage, without
replacing suffocation with a fake damage call.

The full gate now contains **105 unit tests and 143 required GameTests**. New tests
cover bounded lease capacity/review/persistence, invalid saved lengths, whole-cost
fuel debit and six-quad model geometry; native graft input and serialization,
all six travel directions, no open-air flight, ore/protection/fluid/fuel/equipment
refusals, illumination, exact deepslate-axis restoration, native suffocation,
expired-toggle refusal, liquid/piston immunity, orphan proxy restoration, later
world edits, and four-wearer admission/release. Older mining, HUD and armor tests
remain adjacent regressions.

`runBurrowingShowcase` completes seven native-client captures: entrance, dark
passage, light-grafted passage, ascent, countdown, hardened-rock damage, no flight.
The first visual pass showed an empty-looking temporary tunnel; three two-sided
veined planes now make the phase volume legible from inside. The second pass
confirmed client block-light values 0/12, vertical motion only in rock, and actual
health loss after hardening. See [BURROWING.md](BURROWING.md) for controls, fixture
coordinates and explicit untested multiplayer/crash/soak boundaries. Local test
success is not production balance approval or certification of all claim mods.

The adjacent `runSuitHudShowcase` also passes all eight phases after adding
burrowing expenditure, including native configuration, refueling, incomplete-set
hiding and display preferences. Both normal and `-PwithoutCurios` full gates pass.

## Suit HUD display options — 2026-09-08

Two new pure layout tests first failed before implementation, then passed: four-corner placement, GUI-size/oversized-offset clamping, exact 128/255 background alpha and pixel-by-pixel non-overlap of the three panel rectangles. Full `prototypeCheck` passes with and without Curios: **100 unit tests and 136 required GameTests**. Suit fuel/combat/mining mechanics are unchanged.

`runSuitHudShowcase` now passes eight captures: empty upper-left, native-refueled upper-right, incomplete set, restored lower-left at scale 3, full lower-right with custom insets, spectator, disabled while wearing a fueled full set in Survival, and the native config screen. Captures were visually inspected for transparency, placement, legibility and absence. The final phase clicks the actual NeoForge visibility/corner widgets, closes the screen to save, verifies the TOML values, then restores the original preferences. Offset validator endpoints and rejection outside 0–4096 are checked in the native client fixture (unit tests intentionally have no Minecraft/NeoForge classpath). The fixture uses fixed native 1.21.1 GUI row coordinates; revisit these if upgrading NeoForge's screen layout. This is not arbitrary HUD-mod overlap or multiplayer load certification.

## Excavation reset refusal — 2026-09-08

The reported crash was reproduced headlessly at **X506 Y230 Z0** with a player-added barrel: the old preflight threw `IllegalStateException` from `ExcavationLab.prepare`. The regression now checks a normal return into canceled state, preservation of the barrel and its three nether stars, preservation of the known fixture chest's seven diamonds and the player's eleven emeralds, and rejection of late capture acknowledgements. This test explicitly loads only the fixture's eighteen chunks in the separate GameTest world and cleans up only its two created inventories. The user's Biology Lab save is not reset by this test.

The full gate contains **98 unit tests and 136 required GameTests**, retaining all adjacent mining, armor/HUD, inventory and progression checks. Expected unknown-block/entity preflight refusals use a single cancellation path and leave ordinary play available; they are not caught as generic exceptions. Preparation timeouts and unexpected programming failures remain distinct errors. No graphical terrain/model changes are included in this fix.

## Suit biomass HUD — 2026-09-08

The suit reserve adds one pure test and two native GameTests to the full gate: **98 unit tests and 135 required GameTests**. The reserve test first failed to compile before its owning domain type existed. It covers capacity, complete-dose acceptance and invalid values. Native use-item tests cover initially empty/legacy gear, exactly-once feeding under cooldown, full-reserve refusal without wasting flesh, Survival/Creative material cost, real dye/mutation and item serialization, preserving damage/name, swapping chest pieces, wrong-slot/vanilla/missing equipment, removal/re-equipping and spectator refusal. Existing armor/Curios/fast-tissue/damage/cauldron regressions remain unchanged.

`./gradlew runSuitHudShowcase` passes six acknowledged captures through the shared capture adapter: empty, native-client refueling to 250, boots removed, re-equipped at GUI scale 3, full 1,000 and spectator. Refueling uses actual client use-item packets, server interaction admission and vanilla equipment-component synchronization; it does not seed an arbitrary HUD number. Images are `run/screenshots/suit-hud-*.png`. All six states were visually inspected, including the absent meter and the restored reserve. The first run also checked contrast against a dark rainy sky; the repeat fixture sets clear daylight. HUD tests cover GUI scales 2/3, not every third-party HUD configuration.

The capture launcher uses the disposable Biology Lab save, resets its player's inventory/equipment and game mode, and creates nine glass platform cells around **X600 Y229 Z0** plus one marker at **X600 Y234 Z−2**. Unexpected block types abort. It exits after captures; ordinary `runClient` or existing lab profiles show the HUD whenever a full suit is worn, without running this fixture. See the README for feeding controls. Ability drain, consumption balance, reservoir refueling and multiplayer latency/soak certification are intentionally outside this readout experiment.

## Ground-level excavation showcase — 2026-09-08

`./gradlew prototypeCheck` and `./gradlew prototypeCheck -PwithoutCurios` pass **97 unit tests and 133 required GameTests**. Six new in-world cases cover native Survey Culture/Strata Maw recipes and discovery, actual one-item ground mutation with flush passive collision, authoritative native-menu depth selection and shared-budget refusal, a complete 1,920-solid-block strip with exact stored output and zero loose mining items, saved final-layer work at the 160-depth cap with no material created from air, and root headroom beneath an intact flush border. The pure plan test enumerates all 19,200 unique targets and the 62-cell boundary. The asset test checks every macro-tile top for exact flush coverage, no gaps and no overlapping coplanar faces.

The first graphical preparation stalled even though the initial headless tests passed: the root's four-block headroom struck the far edge of its new ground collar. A dedicated failing GameTest reproduced the stall at depth four/cursor 60. Extending the root-only footprint from 7×7 to **7×8** fixed it while keeping all markers at ground level. A client label codec error was also corrected. An adjacent `runDownMiningShowcase` regression then reproduced a fixture timing race: progress reached 120 one pulse before the root finalized its step, so automatic Branch could be refused. Both fixtures now wait for the normal completed-section stop before branching; gameplay timing is unchanged. This is why fixture completion and pixel review remain separate gates, not inferred from compilation or geometry tests.

`./gradlew runExcavationShowcase` prepares actual excavations and captures eight acknowledged native views: manual station, untreated ground, leached ground/ore preview, flush root entrance, deep strip, climbing access and both consoles. The first completed run was visually inspected; that review prompted clearer manual-station labeling without viewpoint-dependent left/right directions, and a Start label/depth tooltip on the strip console. `runExcavationLab` prepares the same world and stays open for manual play. See [EXCAVATION_SHOWCASE.md](EXCAVATION_SHOWCASE.md) for locations, reset scope and hotbar controls.

The corrected adjacent `runDownMiningShowcase` also passes all twelve phases; its old raised-collar root entry and recovered crucible were visually inspected. A launcher dry-run verified that both new profiles use the shared first-time Biology Lab save preparation, rather than relying on an already-created save.

All existing manual-leaching harvesting/durability, old mining, protection, inventory, UI and colony regressions remain in the full gate. The deep-cap case resumes the last layer; it is **not** a complete 160-depth mining soak. The 16-depth case verifies all 1,920 actual terrain outputs, but does not certify performance of many simultaneous miners, long-running chunk loaders, multiplayer latency or third-party claim/loot hooks. Climbing access is checked as continuous native climbable blocks and visually inspected, not certified by a new keyboard-driven ascent test. No final progression or balance approval is claimed.

## Living excavation iteration — 2026-09-07

`./gradlew prototypeCheck` and `./gradlew prototypeCheck -PwithoutCurios` pass **95 unit tests and 127 required GameTests**. New coverage exercises all six leaching-face orientations and host identities, ordinary recipe discovery/crafting, zero automatic drops, native manual pickaxe salvage and wear, untouched ore wear, wrong-tool salvage refusal, fluids, protection and shared mining admission. Rootstock coverage now opens Extend through the actual probe/native menu, reloads its authorization, excavates the second section to 24m, refuses further extension and moves a native player up the resulting stairs without jump input. The last movement check drives native collision directly; it is not a multiplayer keyboard-input/latency certification.

The four-block headroom test failed before the geometry change. Client review then caught opaque ambient shading behind the porous membrane; a failing world assertion reproduced it before adopting native transparent-block lighting. Review of the shared cutting path added two further red-first regressions: enclosed air must not stall crucible digestion or mint output, and missing treads must stop the root before it opens headroom over a fall. The corrected crucible case recovers exactly 74 terrain blocks and retains 3556 of its original 4000 biomass.

`runDownMiningShowcase` completed all twelve acknowledged phases, including untreated/treated ore-preview walls. Root entry/branch captures inspect native substrate stair geometry and lighting; the existing three consoles, cradle landing and sealed/open crucible remain adjacent visual checks. The membrane, stairs and nodule reuse original mod art, and the asset gate validates all forty native stair transforms, their custom textures and actual membrane alpha. The fixture prepares 320 actual rootstock terrain removals/replacements, not prefilled resources. Final corrected membrane and root-console captures were inspected alongside the root passage and passive shaft/chamber access. Historical counts in older sections refer to those earlier revisions.

As before, these are bounded functional/visual checks, not a balance certification, dense-base FPS/TPS measurement or third-party-tool/claim compatibility guarantee. The current manual instant-break/no-abrasion behavior is verified with a native iron pickaxe; modded tools can override their own mining behavior.

## Downward mining experiments — 2026-09-07

`./gradlew prototypeCheck` and `./gradlew prototypeCheck -PwithoutCurios` pass **94 unit tests and 119 required GameTests**. This includes ten new in-world downward-mining cases and the original 109 required regressions. `DownGeometryTest` first failed because the bounded geometry did not exist; the platform-control test reproduced the missing rider console before that feature was added. Expanded asset checks caught overlapping rootstock faces. The final cases cover real loot/fuel conservation, rider descent and probe access from the deck, passive climbability, branching/focused supply, sealed digestion, recovery, same-state reload, partial recovery/resume, full output, a data-bearing core drop, enclosure/protection refusal, altered platform, spectator-inclusive entity caps, recipe discovery/native crafting, native menu cooldown/range and shared mining-quota refusal. No automated mining item spills are accepted.

`./gradlew runDownMiningShowcase` captures ten acknowledged scenes in the disposable Biology Lab save, including the cradle surface/landing, a walkable root branch, sealed/open crucible and all three native consoles. The prepared world contains 150 actual cradle cuts, 192 actual rootstock cuts and 75 captured crucible cuts. Images are `run/screenshots/down-mining-*.png`. Repeated runs on the same save passed; visual iteration corrected root selection bounds/orientation, overlapping model faces and a branch camera placed at the wrong stair tread. The platform console was exercised six blocks below its surface controller. Captures show settled states; they are not a multiplayer motion/latency test.

The adjacent `runMiningShowcase` (seven captures) and `runLogisticsShowcase` (six) also pass after reuse of mining inventory and console infrastructure. Their native mining/organ screens were visually inspected. Other armor/reservoir art was not edited. A final malformed-coordinate regression failed in the rootstock case before removing redundant saved target coordinates; targets now derive from the bounded step/branch plan.

See [DOWNWARD_MINING.md](DOWNWARD_MINING.md) for player instructions and explicit experiment limits. No dedicated-server latency soak, dense-colony TPS/FPS certification, hostile capability/loot-hook certification or arbitrary-instruction crash-durability claim is made. The pure geometry, fixed collections and per-dimension quotas establish bounds, not overall server performance.

## Colony experiments and fixture isolation

GameTests now use a generated **16×12×16** empty structure (`java tools/ExportTestFixtures.java`). The old borrowed `bastion/mobs/empty` was only 1×1×1; larger fixtures could overlap neighboring test cleanup. Physical courier testing exposed this. Keep fixture blocks inside their advertised bounds.

`./gradlew runColonyShowcase` captures nine server-signaled phases: armor front/back, lift exterior, actual lift ride, transport, brood, courier, and two vertical-vein pulse phases. The ride capture waits for the client to reach the upper shaft. Shared `client.capture` infrastructure owns timing. `./gradlew runColonyLab` stays open. See [COLONY_LAB.md](COLONY_LAB.md).

`./gradlew prototypeCheck -PwithoutCurios` exercises the absent-mod path. Presence tests equip/remove an organ through the real Curios inventory. Additional GameTests cover full-set gates, persistence, caustic access, physical delivery, back-pressure, cycles, item/fluid capabilities and the loaded maximum brood population.

The prototype deliberately separates deterministic mutation rules from Minecraft integration.

`./gradlew prototypeCheck` is the one-command headless gate: it regenerates data, runs unit tests, packages the JAR, and boots the GameTest server.

The gate refreshes generated resources after data generation and verifies every generated file byte-for-byte inside the final JAR. This prevents a successful gate from silently packaging yesterday's guide/language data. Plain `build` packages the currently checked-in/generated resources; use `prototypeCheck` for the authoritative regenerated artifact.

## Fast loop

`./gradlew test` runs pure JUnit tests without launching Minecraft. Mutation costs, rank limits, and durability growth belong here.

The effect module also tests deterministic double-helix geometry and its aggregate per-tick particle budget without launching Minecraft.

## In-world loop

`./gradlew runGameTestServer` launches NeoForge's headless GameTest server. It verifies block conversion, substrate growth, chamber requirements, inventories, persistence-facing components, and mutation completion.

The original survival-loop group has 19 acceptance tests; the full suite now has 41 GameTests and 49 unit tests. Recent regressions cover immediate hatch-capacity release after lethal damage, reservoir surface join/split state, scheduled uphill/downhill item transport, embedded vein geometry/UVs and pulse direction, and rounded-reservoir surface geometry. The original eight cover Survival and Creative recipe-book unlocks, culture use and consumption, controlled three-dimensional bone-meal spread, the showcase blocks' render/shape contracts, the chamber's substrate interlock, and a full timed carapace mutation.

The next hardening pass added executable coverage for ten additional risk areas:

1. Seeding permissions and failure safety: Creative use is free, while an invalid target changes and consumes nothing.
2. Spread safety: bone meal cannot replace unsupported neighboring blocks.
3. Automation filters: the chamber accepts damageable gear and rotten flesh only in their respective slots.
4. Resource thresholds: fewer than four biomass items cannot begin or consume a mutation.
5. Interrupted work: removing the living substrate resets progress without consuming inputs.
6. Item identity: mutation preserves wear, custom components, and stack count.
7. Mutation limits: processing stops at rank three and charges exactly once per completed rank.
8. Persistence: inventory components, biomass, and in-flight progress survive block-entity serialization.
9. Inventory recovery: breaking the chamber drops each stored stack exactly once.
10. UI/menu behavior: shift-click routing, synchronized progress scaling, and distance-based menu closure work headlessly.

### Recipe unlock test

For a clean manual check, use a new player or revoke the generated recipe advancements first. Pick up rotten flesh in Survival or put it into your inventory from the Creative menu; the Spore Culture recipe should appear within half a second. Craft or obtain a Spore Culture; the Mutation Chamber and Waking Genome recipes should then appear. The mod compensates for Creative slot packets bypassing vanilla's inventory-change trigger, and a dedicated GameTest covers that exact path.

## Packaging loop

`./gradlew build` must produce a JAR containing generated resources. `./gradlew runData` validates recipes, loot tables, models, tags, advancements, and guide data before release.

## Graphical loop

`./gradlew runClient` is used for normal development. Once the development world named `New World` exists, `./gradlew runClientSmoke` bypasses the menus and joins it directly. `./gradlew runVisualShowcase` builds a lit four-direction gallery around the player so saved client camera orientation cannot hide the specimens, activates the Mutation Chambers, disables pause-on-focus-loss, and automatically writes several frames of their model animation and client-local double-helix effect to `run/screenshots/infestus-showcase-*.png`. It then opens the real native Mutation Chamber screen with deterministic half-progress data and writes `run/screenshots/infestus-ui-mutation-chamber.png`. Gameplay correctness does not depend on screenshots alone.

The UI animation is derived from synchronized process progress rather than wall-clock time. This makes repeated screenshots comparable and keeps visual regressions diagnosable.

## Biology and multiblock loop

`java tools/ExportBiologicalAssets.java` reproducibly exports the committed generated art sources into macro texture tiles and model JSON. Fast tests check macro wrap boundaries, common stage boundaries, shape limits, unloaded-boundary refusal, scan budgets, and exact reservoir allocation for every supported cuboid size.

`BiologyGameTests` covers reservoir formation, invalid-shape refusal, shared access, expansion, splitting, persistence, portable drops, bottle interaction, full-inventory back-pressure, zero loose-item output, fixed-template failure, output-full refusal, DNA bounds, tissue maturity, and the full-armor speed requirement.

`./gradlew runBiologyShowcase` captures eight server-signaled phases in an isolated copied lab save. Inspect all eight images for terrain seams, model integrity, shared fluid height, capacity changes, and full/empty views from different sides. The graphical gate also checks every rounded-reservoir state model and rejects opaque biomass in the translucent sorting pass. `./gradlew runBiologyLab` leaves the same fixture open for human exploration. See [BIOLOGY_LAB.md](BIOLOGY_LAB.md).

## Open armor and dye regression loop

`ArmorAnatomyTest` checks all branches/layers for bounded detailed geometry, unique parts, material-quadrant UV containment and absence of complete enclosing head/torso/limb cubes. `ArmorDyeAssetTest` checks tint indices on all 44 inventory models, the additive native dyeable tag, and disjoint opaque masks across all worn layers. These are structural checks, not a claim that arbitrary poses cannot clip.

`ArmorGameTests.vanillaDyeMixingMutationPersistenceAndCauldronWashing` exercises actual recipe-manager assembly for all four pieces, multi-dye mixing and redyeing, mutation after dyeing, save/load preservation of name/damage/enchantments/trim/color, and real water-cauldron dispatch. Washing consumes exactly one water level; an undyed piece consumes none. The native crafting test was observed failing before the implementation.

Run `./gradlew prototypeCheck`, then separately `./gradlew runColonyShowcase` (24 phases) and `./gradlew runBiologyShowcase` (10 phases). Never combine graphical tasks sharing Biology Lab in one invocation. The seven `colony-player-*.png` portraits add front/back skin visibility, three dye colors and crouching to the existing gallery. Inspect these and adjacent block captures manually. Repeat the colony run on the same disposable save for reload coverage. `prototypeCheck -PwithoutCurios` remains the optional-integration absence gate.

Resource checks and selected captures do not certify every player skin, slim-arm alignment, all action poses, shader packs or crowded-scene FPS. Those remain explicit follow-up coverage.

Validation, 2026-09-07: the full gate passed **77 unit tests and 73 required GameTests**, with zero skips/failures, in the default and Curios-absent profiles. The colony showcase completed twice on the disposable save; the second run contains the final tapered chest/thigh segments. The 10-phase biology showcase also completed. Player front/back, dyed variants, crouching, reservoir fill/expansion/empty, substrate, elbow/stitch, brood and defense views were inspected. No error-level log entries occurred in these final runs. No golden images were automatically approved. Adjacent block models were left unchanged because these sampled views did not reproduce a specific new block defect.

## Surface intake regression loop

The 2026-09-07 surface-intake pass adds three real `SurfaceIntakeGameTests`: an unseeded ground-level digestive patch feeds storage through scheduled pulses; all four horizontal intake orientations ignore an unselected above-organ source and reject vertical starts; full-output cargo and intake selection survive serialization/portable drops, then deliver without duplication after switching sources. Legacy saves default to above-organ intake. The original two regressions were observed failing before implementation. The authoritative gate passes 77 unit tests and 76 required GameTests, including existing item/fluid transport, digestion, reservoir and defense checks.

The first graphical reload exposed a stale final note-block marker from the previous run. The shared capture driver now waits for phase one before starting a fresh sequence; subsequent phase-skip checks remain strict. The colony fixture uses a flat digestive patch and surface-mode first organ, while its second organ retains default intake. Its existing assertion requires actual harvested biomass to reach the defense bloom—no prefilled source masks a broken route.

`./gradlew runSurfaceIntakeShowcase` is the focused visual gate for this arrangement. It uses the same disposable Biology Lab save and holds one scene after 600 server ticks until rendered/captured, writing `run/screenshots/colony-surface-intake.png`. The client also checks the synchronized fed-bloom state and expected camera position. Run it separately from other graphical profiles. The full timed 24-phase showcase still has an intermittent low-render-rate failure: one observed transition had only two rendered frames before the next phase. Improved diagnostics retain the phase/frame/screen evidence; this is not claimed fixed by the focused gate.

The focused showcase passed twice, including a reload with the final client readiness checks; the flat intake/vein/reservoir image was inspected. The final authoritative headless gate passed again after the capture changes. The reservoir-side junction/storage concept remains discussion only.

## Synaptic console / screenshot replica (2026-09-07)

The current gate contains **88 pure tests and 89 required GameTests**, passing in both normal and `-PwithoutCurios` profiles. The gate regenerates data, verifies packaged bytes, and runs the real server. New coverage includes the pump-free digestive-patch layout; closed/reopened and persisted intake modes; full-storage conservation; ambiguous sources; reverse cycles, branches and the 64-node ceiling; independent diagnostic-arm selection; native block placement and empty-hand non-configuration; real probe/menu opening; invalid/rate-limited/out-of-range/stale-target commands; crafting and Creative recipe discovery; 32-bit menu synchronization; green pulse pixels and original transparent probe art.

`./gradlew runLogisticsShowcase` captures six acknowledged phases: working replica, organ console, closed-port console, vein console, sealed-port close-up, and adjacent Mutation Chamber UI. The reservoir starts empty. The final live capture observed **152 biomass** harvested from real husk damage before taking its first image. Real screen clicks travel through the native menu-button packet to Trace and Shut; the server checks the closed setting before advancing. Screenshots are in `run/screenshots/logistics-*.png`. The latest organ/vein/port/chamber images were visually inspected; no screenshot baseline was automatically blessed.

The feedback loop found and corrected a native short-word overflow, a mock NeoForge handshake omission, selected-hotbar synchronization in the fixture, an overflowing footer, shared diagnostic selection between viewers, and block/menu packet ordering in the new chamber capture. The initial NoAI specimen did not generate ground contact; the working fixture uses ordinary physics with goals removed. The existing brood death test was isolated from same-tick hatch quota contention, not given a larger quota. Earlier failing attempts remain failures, not accepted evidence. The final six-phase run has no error-level log entries.

This is local functional/visual coverage, not certification for third-party claim mods, multiplayer latency, dense-base FPS/TPS, all screen scales, or whole-network history/overlays. Probe grades beyond this local console are design work, not implemented features. The former discussion-only junction note above is superseded by this implementation and [LOGISTICS_CONSOLE.md](LOGISTICS_CONSOLE.md).

## Full logistics validation pass (2026-09-07)

See [LOGISTICS_VALIDATION.md](LOGISTICS_VALIDATION.md) for the reproduction commands, failing-before-fix evidence, seven new end-to-end/boundary tests, strengthened real probe-to-stitch interaction, graphical reload checks and explicit remaining gaps. The current server suite has **96 required GameTests** alongside **88 pure tests**; the earlier 89-test figure above describes the preceding checkpoint.

This pass corrected pause capability bypass, stale arm diagnostics, incidental chunk loading from derived vein appearance, and a saved-entity loading race in the disposable lab. The lab now refuses a capture unless exactly one specimen is present. Historical captures containing accumulated husks are not accepted as a single-specimen repeatability result.

## Mining organ experiments (2026-09-07)

The authoritative gate now passes **91 pure tests and 109 required GameTests**, with zero skips/failures, in both default and `-PwithoutCurios` profiles. Thirteen new real-server tests exercise three actual tunnel profiles, retained arch corners, fuel/output conservation, buffer-full recovery, tool/liquid/container/falling-terrain refusal, incomplete frames and native protection cancellation, shared eight-operation admission, no target-chunk loading, finite completion and non-farmable lights, real probe/menu commands, all four native crafting recipes and discovery, offline ownership and portable native placement, and reservoir/green-vein/chest integration. See [MINING_ORGANS.md](MINING_ORGANS.md) for playable setup and limits.

The finite-range fixture initially encountered its template's glass wall and correctly refused it; the corrected test prepares its entire intended air path. A strengthened asset assertion was observed failing on overlapping coplanar outward faces, then passed after the model exporter recessed those surfaces. This is geometry validation, not proof against every possible visual defect.

`runMiningShowcase` uses ordinary scheduled mining, waits for 28/98/434 real blocks to be collected, then takes seven acknowledged world/console captures. Visual iteration corrected rib repetition, camera framing/eye height, selection bounds and confusing output telemetry. Close inspection caught a fixture-only reload spill from replacing filled chests; the lab now clears its three known inventories first, waits for bounded entity readiness, cleans at most 64 stale fixture items and refuses captures with any loose item. Do not accept earlier contaminated captures as zero-entity evidence.

The adjacent six-phase `runLogisticsShowcase` passed after extraction of shared `BlockMenuAccess`; organ and Mutation Chamber screens were inspected. No existing armor, reservoir, vein texture or gameplay module was redesigned. This gate is not a substitute for dense-server profiling, a long soak, third-party loot/protection compatibility or multiplayer latency testing. The raw-loot/global-modifier restriction and native relighting costs are explicit in [PERFORMANCE.md](PERFORMANCE.md).

Final mining captures completed twice after the no-spill reset fix, including a reload; the second run includes the coplanar-face correction. Both passed all seven phases and the zero-loose-item assertion. The final mining and adjacent logistics runs contained no error-level log entries. The final headless default/Curios-absent gates passed after the asset correction. No golden screenshot baseline was automatically approved.

## Compatibility matrix (planned integrations)

The default profile contains NeoForge, GeckoLib, Modonomicon, Curios and Infestus Frontier; `-PwithoutCurios` tests optional-adapter absence. Later CI profiles will add JEI/Jade and Create separately, so an optional integration cannot hide a base-game failure.
