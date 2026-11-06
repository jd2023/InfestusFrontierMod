# Roadmap

Each milestone ends in a playable build. Only the current sprint is planned in detail, in `docs/sprints/`. Later milestones stay at one line each here.

Now: M0 and M1 are done; S01 and S02 are accepted. M2 is in progress: the Bowl, the Sac, the Bladder, the Bud, the probe and the guide are in; the Rack, the Loom, the Cradle and the four awakened pieces are in; the batch organs run on their own and take hoppers; the Bio-Furnace smelts on biomass and grows through three stages. M3 is in progress (`docs/sprints/S04.md`): the Activation Cyst and the eight treatments are in; the three veins and the Vascular Junction move biomass, water and items; veins climb walls and fork; Mouths under chests and organs route items through Item Veins by filter, amount and priority; the farm plants, waters, harvests and composts on its own; the Sensor Polyp reads a block into redstone; the Fuel Papilla refuels worn armor at base. Art finishing work is queued in ktask; `CREDITS.txt` identifies temporary assets.

Next: the [automated mineral workshop](#next-milestone--the-automated-mineral-workshop), M3b, is defined and proposed; its step 1 (the Mixed Craw and the Larder), its steps 2 and 3 (the Gizzard, the Kidney and finite waste) its steps 4 and 5 (the Palm, its reach through the Mouth under it and its Rote cycle) and its step 6 (the iron-bars factory in the default demo) are built and await the owner's play. The living-workshop acceptance below stays open and is not replaced by it.

## First meaningfully playable version — the living workshop

### Scope and finish line

Ship a complete Overworld Survival loop through M3: grow a workshop, automate its
food and water supply, keep a biomass surplus, and spend that surplus on useful
production. M4 research starts after this version. Completing the organ list alone
does not meet this scope; the connected loop must work from a fresh world.

The player finishes this slice when a colony replenishes its own operating supplies
and runs a Bio-Furnace from surplus while protecting its next farm cycle. They can
then expand production, change the layout or keep using it beside their vanilla base.
This is the first private playable build, not the full campaign or a public release.

### Player experience

1. Start beside a vanilla home. Discover Spore Culture, choose the colony's footprint
   and grow Living Substrate. All required inputs are obtainable in the Overworld.
2. Feed the Digestive Sac by hand. Store biomass, make starter components and run
   the first useful batch in the Bio-Furnace. Manual operation remains a recovery path.
3. Build one renewable crop line. Route planting stock back to the farm, retain
   player food and send surplus crops to digestion. Supply water automatically from
   a renewable vanilla source using the Vascular Junction.
4. Connect storage and consumers. Set ports, filters, priorities and reserves. Read
   why an organ is waiting and correct a blocked route without consulting source code.
5. Add furnace demand. See the surplus shrink, then increase production or limit
   consumption. Save, leave the area, return and resume the same colony.
6. Craft and awaken an armor piece, then fill it at the base. The four existing
   pieces remain available; completing a full suit is not required.

Target pacing for playtesting: first useful biomass-funded production within
30–45 minutes of starting the mod beside an established vanilla home; a working
automated colony in 2–4 hours. These are starting targets, not measured results.
Fresh-world testing also records the vanilla preparation time separately.

### Required content

| Area | Minimum for this version |
|---|---|
| Foundation | Deliberate substrate growth, maturity, placement and readable organ foundations |
| Workshop | Bowl, Sac, Bladder, Bud, Rack, Loom, Cradle, Bio-Furnace and Activation Cyst; their required ingredients and treatments |
| Renewable supply | One complete crop route using Cultivation Tissue, Seed Pouch, Planting Proboscis and Harvest Corolla; Compost Gland available for surplus seeds |
| Water | Automatic local collection and distribution; no recurring bucket delivery once the line is running |
| Transport | Biomass, fluid and item veins; Junction, Mouths, Feeding and Yield Lobes; branches, vertical routes, Septum Crossing and Vascular Stitch |
| Storage and control | Item Capsule, Biomass Reservoir, Filter Valve and fullness Sensor Polyp; protected planting, water and restart supplies |
| Equipment | Existing dormant/awakened pieces, Cradle awakening, Bladder filling and Fuel Papilla refueling |
| Discovery | Guide, recipe reveal, advancements and probe diagnostics covering the entire required route and the finish line |
| Presentation | Functional art for required content (temporary assets allowed, on the placeholder list); readable inventory icons, connections, flow direction, operating states and screens |

Recipes and balance values remain in the [item](docs/design/ITEM_CATALOG.md) and
[block](docs/design/BLOCK_CATALOG.md) catalogs. The current implementation plan is
[S04](docs/sprints/S04.md). This section defines the playable boundary, not new recipes.

Armor is an introductory project in this slice. Its current reserve and refueling
support do not establish a fuel-consuming ability. The furnace is the required
useful biomass sink; neither the guide nor acceptance claims armor learning,
self-healing or mutations before those behaviors exist.

### Decisions the player must make

- Divide harvest between planting stock, food, compost and biomass. One crop cannot
  pay every route at once.
- Reserve enough biomass and water to restart before feeding discretionary consumers.
- Trade space and construction cost for throughput. A larger store buffers demand;
  it does not fix a colony that consumes more than it produces.
- Choose routing and consumer priority. A full or unavailable destination must be
  understandable and recoverable through normal play.

### Outside this version

DNA collection and banks, genome completion, graft research and Mutation Chamber;
armor learning, permanent fusion branches and advanced abilities; ore treatment
and excavation; tree, aquatic and animal production branches; electricity, heat
and pressure; Nether and End industry; cross-dimensional travel and freight;
helpers, chunk loaders and combat systems. These remain later milestone work.

Only one renewable crop route must qualify. Extra species, alternate production
chains and advanced controls do not block this build. Vanilla redstone, hoppers
and containers remain valid tools. JEI and Curios are optional, not prerequisites.

### Acceptance: playable means all of these pass

1. **Survival bootstrap.** Complete the route in a fresh Survival world using the
   packaged mod and required dependencies. No commands, Creative items, prepared
   demo stock or developer instructions. Every prerequisite is obtainable before
   the recipe that needs it. The guide identifies the next project and its inputs.
2. **Sustained production.** Run three complete harvest-to-replant supply cycles
   after startup. Record starting and ending crops, seed reserves, water, total
   biomass across stores and routes, and useful output. Include all operating costs;
   moving a startup buffer into another store is not production. With discretionary
   consumers off, each cycle leaves a positive biomass surplus and retains enough
   supplies for the next cycle. Then run a declared furnace workload for three more
   cycles without manual feed, reserve depletion or a downward biomass trend.
3. **Useful reward.** The automated line produces smelted goods the player can use
   in ordinary Minecraft. The player also awakens and refuels one armor piece.
4. **Recovery.** Fill an output, interrupt water, break a route and unload the base.
   Each condition pauses the affected work without duplication or unintended loss.
   Restore the condition and resume. Save/reload with a batch and a transfer active;
   contents, settings and organ history survive. Retained reserves allow a restart.
5. **Concrete layout.** The iron-bars factory ([M3b step 6](#order-of-work)) is the
   layout built on top of the loop. A second materially different route is not required.
6. **Readable play.** The owner completes the loop using the guide and in-game
   feedback. Record confusing steps and long waits; fix progression blockers before
   acceptance. Required art is functional and readable; temporary assets are allowed and identified in `CREDITS.txt`.
7. **Build and server health.** Pass the [engineering gate](ENGINEERING.md#testing)
   and M2/M3 evidence below. Smoke-test the packaged build on a dedicated server
   with two clients using the same stores and routes. The full campaign and broad
   multiplayer qualification remain M11.

The remaining work is to finish S04, qualify the connected Survival route, close
bootstrap and usability gaps, and keep the placeholder list current. M2 and M3 stay in
progress until their evidence passes. New systems do not substitute for that evidence.

## Next milestone — the automated mineral workshop

M3b. Defined here and proposed; steps 1 to 6 are built. It starts from the colony the living workshop
delivers and turns the player's hand-mined raw ore into dust and ingots, keeps supplies
in a store that guards them, and repeats one crafting recipe on its own.

### Status of the living workshop

The scripted route is recorded in [S04](docs/sprints/S04.md) (F9: six supply cycles,
three with a furnace). The acceptance list above is **still outstanding**: no fresh Survival run, readable-play
review or dedicated-server smoke test with two clients is recorded. Scripted tests do not stand in for them. M3b work may
proceed on top of the shipped content; it does not close M2 or M3.

### Scope

In:
- Iron, gold and copper raw items the player mined by hand (Fortune included).
- The Mineral Gizzard, four dust per raw item. The Washing Kidney, one more dust per item, optional.
- Finite waste on the same stage: dirty water, Rock Residue, Tailings. A full buffer pauses the line.
- The Bio-Furnace smelting four dust into an ingot.
- The Mixed Craw and the Larder, two mutations of the Capsule Stem pod.
- The Shaping Palm: a retained crafting grid, reach to stores through the Mouth under it and the Item Veins, one recipe repeated on biomass through Lobes.

Out (later milestones): Leaching and the Leaching Gland; intact extraction and treatment
(Work Bed, Grasping Root, Reaction Polyp, Fracture Jaw); planned mines and shafts;
genetics infrastructure; thermal, steam and electrical upgrades, the Separator and
smelters that need fewer than four dust; recursive Request Cortex orders.
Washing needs no electricity and no completed genome.

### Contracts

| Subject | Owner document |
|---|---|
| Dust ratios, costs, buffers, waste sinks, comparison with direct smelting and Fortune, closed duplication paths | [Items](docs/design/ITEM_CATALOG.md#first-release-the-automated-mineral-workshop) |
| Gizzard, Kidney | [Blocks T2-23, T2-24](docs/design/BLOCK_CATALOG.md#t2-23--mineral-gizzard) |
| Mixed Craw, Larder, reserves, manual withdrawal | [Blocks T1-09](docs/design/BLOCK_CATALOG.md#t1-09--item-capsule-and-capsule-stem) |
| Shaping Palm, three stages, cycle, bounds | [Blocks T1-41](docs/design/BLOCK_CATALOG.md#t1-41--shaping-palm) |
| Player projects and the guide pages | [Progression](docs/design/PROGRESSION_MAP.md), [Guide tree](docs/design/GUIDE_PROGRESSION_TREE.md) |
| Limits and invariants | [Engineering](ENGINEERING.md#performance-limits) |

### Order of work

Each step is one feature task with its own review. A step ships its recipes, its
discovery and advancements, its in-game book entry, a labeled usable exhibit in the
default demo, any temporary art identified in `CREDITS.txt` and tests.
Missing final art never blocks a step. The book entry and the demo exhibit are verified through the capture
workflow in the same step.

1. **Mixed Craw and Larder.** Both are built (review and the owner's play pending): pod kinds, the two mutations, the screens, saves, the Larder's labels and reserves, and the reserve check in `CapsuleTree.extract` and `take`. Tests: every extraction path (hopper, Mouth, Item Vein, handler call) stops at the reserve; a Mouth's amount and a reserve combine; manual withdrawal; break, shed and reload keep stock, label and reserve; an old bulk pod loads unchanged; a Craw's capacity is half a bulk pod's at each grade; a non-empty pod refuses the gel.
2. **Dust and the Gizzard** (built; review and the owner's play pending). The raw-item inputs, the dust items, Tailings and Rock Residue, the multi-result `organ_batch` with its reserved tray, the Gizzard, dust smelting in the Bio-Furnace. Tests: 4 dust and 1 ingot per raw item to the item; refused inputs (ingot, nugget, block, ore block, Nether gold ore); full tray pauses; save mid-batch gives the products once; conservation through a whole line.
3. **The Kidney and waste** (built; review and the owner's play pending). Washing, the dirty tank, settling, the discard and cobblestone crafts. Tests: 5 dust per washed raw item; a 3-unit or foreign parcel is refused; dirty water full pauses and a settle resumes; no path from dirty water to clean water; Rock Residue and Tailings feed nothing else; bounded buffers.
4. **Palm stages 1 and 2.** The retained grid, vanilla remainders, reach through the Mouth under the Palm and its three actions. Stage 1 is built (review and the owner's play pending): the grid, the engine's own result slot and remainders, tests through a crafting menu (cake, honey bottle, a banner and a shield, Tailings), a full inventory, two viewers, a client that claims a result, save and reload through a chunk, breaking and placing, and no item handler. Stage 2 is built (review and the owner's play pending): Reach from a Contractile Fiber, access to stores only through the Mouth under the Palm and the Item Veins, Restock, Return and Store result, and nothing automatic. Tests: the fiber, a chest 12 blocks away and an unconnected chest beside the Palm, the Mouth's direction, filters, amounts and priorities, Capsule Trees with Craw and Larder pods and Larder reserves, component-sensitive matching, competing Palms, a full destination, a broken and a mended vein, the shared network work, an unloaded endpoint, an older save with a direct link, and no work done by a button.
5. **Palm stage 3.** Built (review and the owner's play pending): the Synaptic Gel, Select, the cycle, the tray, the Lobes, the biomass tank. Tests: special recipes refused, a recipe change pauses, mid-cycle save, 10 BU and 40 ticks per craft, outputs and remainders held until they fit, no duplication through Lobes and hoppers, 32 starts a tick across many Palms.
6. **The iron-bars factory** (built; review and the owner's play pending). One labeled line in the default demo, south of row C at x=0 to 31, z=-22 to -15 (`src/testMod/resources/captures/demo.json`): a Larder of 18 raw iron that keeps 3, a Gizzard, a Kidney with a water pool and Junction, a waste chest, a Bio-Furnace, a Palm with Rote and the vanilla iron-bars recipe selected, and a chest of bars, joined by 11 Mouths, Feeding and Yield Lobes and Item Veins, with one Reservoir of 4,000 BU and a biomass vein under the organs. The player brings the raw iron and the biomass; the line makes neither. Guide entry `circulation/iron_bars_factory` names every part, its recipe and its costs. `IronBarsFactoryTests` builds the line from the demo's own commands and runs it: three crafts, 48 bars from 15 raw iron, with an exact ledger in raw iron, dust, ingots, bars, Tailings, Rock Residue, water and biomass; the water and the biomass cut by the probe and restored; the chunks unloaded and loaded in the middle of the washing; a full waste chest and a full chest of bars each pausing the line with its reason and resuming; the recipes and the Palm's pattern checked against what ships. No human Survival or multiplayer play of it is recorded.

### Acceptance

1. Every step's tests pass in the gate (`bash scripts/verify.sh`), with the evidence kept as the task requires.
2. Captures of each new book entry and each demo exhibit are read, not only checked by pixels.
3. The iron-bars factory of step 6 runs with a measured ledger in a test, with the line's chunks saved and loaded in the middle of its work.
4. Every number in the catalogs for this milestone is a starting value. Play testing may change it in the Items, Blocks and Engineering documents only.
5. The owner plays the line in Survival before the milestone is called accepted. Scripted tests do not claim Survival or multiplayer acceptance.

## Milestones

| M | Playable result |
|---|---|
| M0 | Clean build, one tested core rule, one tested ui primitive, one GameTest in a real server |
| M1 | The feedback loop. Spore Culture and Living Substrate are in the game with recipes and guide pages. Every story is proven by tests and by screenshots from the real game, and an agent reads both without a human |
| M2 | Feed a living workshop: culture bowl, sac, bladder, rack, loom, cradle, one awakened armor piece |
| M3 | Self-supplying Overworld: farms, veins, reservoirs, controls, refuel at base |
| M3b | Automated mineral workshop (proposed): hand-mined iron, gold and copper to dust and ingots; Mixed Craw and Larder; one recipe repeated by a Shaping Palm |
| M4 | First designed trait: samples, extractor, DNA bank, graft, mutation chamber |
| M5 | Specialist equipment and manual industry: armor branches, leaching, ore treatment |
| M6 | Nether industrial base with local heat, power and DNA |
| M7 | Precision processing and major excavation |
| M8 | End base with local spatial production |
| M9 | One distributed colony: passenger and freight links, storage index, armor recovery |
| M10 | Remaining production branches: aquatic, husbandry, food, helpers, defense, synthesis |
| M11 | Full campaign from a fresh world, two different layouts, dedicated server with two clients |

## Acceptance evidence

Each milestone needs this evidence beyond the full gate (`bash scripts/verify.sh`). The owner plays the build at the end of each sprint.

| M | Evidence |
|---|---|
| M0 | Clean build; core and ui tests; one GameTest in a real server; `core` and `ui` have an empty classpath |
| M1 | Spore Culture converts eligible exposed ground and bone meal matures it, in GameTests; recipes and guide pages exist; client captures of all three stages, front, back and inventory scale, pass the capture checks and are read by the reviewer; textures functional and readable, temporary ones identified in `CREDITS.txt`; the owner repeats it by hand in a fresh world |
| M2 | Fresh Survival acquisition and recipe reveal, empty/full inventory, insufficient biomass, break/reload preserving one organ history; client models render at all faces and inventory scale |
| M3 | Three complete reference supply cycles with a positive measured surplus; seed/water/restart reserves retained; 3D branches/crossings and port direction inspected visually; full outputs and unloaded routes cannot spill or duplicate |
| M3b | Four dust a raw item, five when washed, four an ingot, to the item; refused recycling and foreign parcels; every waste buffer fills, pauses and resumes; every extraction path keeps a Larder reserve; Palm remainders, refusals of the Palm's Mouth and mid-cycle saves; bounded work at 32 starts a tick; save and reload of the running iron-bars factory |
| M4 | All sample qualities and capacities, last-slot contention, no-drop vanilla species, duplicate death callbacks, relocation across dimensions, copied-record merge, consumed sample never recreated; no ordinary-slot pollution or nested pack duplication |
| M5 | Every activity has one owner, sum-cap clipping, partial clearing costs, permanent branch rejection, one-heart hunger floor, sleep/rescue persistence; ore conservation against direct mining/Fortune and retained dust |
| M6 | Start with the declared finite kit, replace every recurring fuel/water input locally, manufacture/export lining, then restart without Overworld fuel; wrong dimension/bed/level/mutation each independently refuses unchanged |
| M7 | Walk every excavation type including descent, turn, section continuation and light/access; cavities/fluids/protected blocks/output-full/chunk boundary stop safely; bounded spill containment tested under shared load |
| M8 | First End materials need no shulker genome; first shulker genome needs no R3 service; local powered cultivation restarts from reserve; burrowing cannot fly or erase terrain, and wing fuel exhaustion leaves the specified fallback |
| M9 | Concurrent freight, crash/restart mid-transfer, both endpoint unloads, changed ownership, unsafe destination and full stores; each preserved armor piece has exactly one owner after death/respawn; no unrequested chunk tickets |
| M10 | Catalog-to-registry/recipe/guide coverage for every in-scope entry; helpers obey population/path budgets and release dead reservations; connected storage renders without internal seams; optional-mod and no-content-mod profiles |
| M11 | Fresh-world playthrough plus a second materially different layout; packaged dedicated server with two clients; worst-case shared-budget soak at declared limits; restart/save recovery, dense-base TPS/FPS profiles and human appearance/usability review |

## Outside M0 to M11

The Fold dimension, its boss, and every Fold-dependent recipe and graft. If it comes, it follows M11 as its own sequence: a playable foothold, a native sustainable habitat, adaptive processing, the Manyfold encounter, reconstructed boss stock. Its first habitat cannot require boss loot.

## Proposed — verification that scales

Status: proposed. This is an implementation plan, not a change to the mandatory
[engineering gate](ENGINEERING.md#testing). Keep the current full gate until the
selection and evidence rules below are implemented and verified.

### Start with the actual cost

The saved `build/verification/full-gate.log` inspected on 2026-11-06 records:

| Work | Recorded duration |
|---|---:|
| Headless `verifyAll`, including builds and GameTests | 5m 55s |
| GameTest execution inside that run: 677 tests | 2.651 minutes |
| Separate graphical `verifyCaptures` invocation | 25m 29s |

The checkout has 65 capture scenarios with 599 views. The available JUnit reports
contain 119 core tests, 21 UI tests and five architecture tests; their summed test
body times are about 0.54 seconds, excluding Gradle/JVM startup. These are one saved
run and available report files, not fresh benchmarks or guaranteed future timings.

Target captures first. Keep all cheap pure tests and static checks. Keep one server
startup for the union of selected GameTests, and one client startup for the union of
selected captures. Headless and graphical Gradle invocations remain separate as the
current harness requires. Splitting every test into a fresh Minecraft process would
spend more time starting the game and complicate shared test quotas.

### Three verification passes

| Pass | Runs | When |
|---|---|---|
| Development | All cheap pure/static checks; affected GameTest groups; affected capture scenarios | While implementing and repairing |
| Task completion, first rollout | Full headless gate; affected captures plus cross-feature smoke scenarios | Every completed implementation |
| Full integration | Existing full headless gate and every capture | Shared/global changes, milestone builds, releases, and periodic integration during targeted operation |

Keep full headless completion while it costs roughly six minutes. Do not spend
engineering effort selectively skipping sub-second JUnit tests. Introduce selected
GameTests for development now; make them the task-completion default only if measured
server cost becomes a real bottleneck and the dependency coverage rules are proven.

Once targeted completion is enabled, a full integration run is due after at most five
completed tasks or one day of development, whichever comes first. Run it only at a
queue boundary on a fixed tree, never concurrently against the worker's changing
checkout. Overdue or failed integration stops promotion of later work until fixed.
A targeted pass is labeled targeted; it never claims complete-suite coverage.
A playable milestone or release always has a full passing run on its exact build.

### Selection follows dependencies, not just filenames

One checked-in ownership map associates production modules and resource families
with GameTest groups, capture scenarios and dependent consumers. Every new test and
scenario belongs to a group. Every changed production/resource path is classified.
Unknown files, missing mappings or an invalid selection expand to full verification.
The selector explains each inclusion in its output. Do not ask the executor to guess
which tests are safe to omit, or pretend Java imports alone cover data-driven links.

| Changed area | Required expansion |
|---|---|
| Gizzard or mineral recipes | Processing/mineral tests, storage and logistics port contracts, iron-bars factory route, mineral screens/motion and relevant demo views |
| A vein, Mouth or item capability | Logistics, every direct consuming organ/storage group, cross-feature automated routes |
| Persistence, shared quotas or common batch lifecycle | All GameTests; captures for affected states, full capture fallback when reach is uncertain |
| Screen kit, GeckoLib adapter, shared atlas or texture generator | All affected consumers; initially full captures for shared rendering changes |
| One organ texture/model | Asset checks, that organ's model states/screens, shared demo exhibit and guide plate using it |
| Guide/advancement/recipe data | Discovery checks and dependent routes; book pages, recipe screens and matching demo scenarios |
| Build configuration, dependencies, loader or test selector | Full verification |
| Prose-only design text with no shipped assets or rules | Link/reference checks; no Minecraft launch |

Selection is the union since the last verified baseline, including uncommitted,
untracked, renamed and deleted inputs. Reusing a result requires matching relevant
inputs, dependency/toolchain pins, test selection and harness version. For the first
rollout, reuse only exact-tree reports. Never assemble a green report from arbitrary
old results after a repair. A failing test stays selected until it passes on the fixed
inputs; shrinking the selection does not clear the failure.

### Use the existing harness, with explicit inventories

JUnit already supports Gradle test filters. The existing `-PcaptureOnly=a,b` selects
whole capture scenarios. Preserve that mechanism and add dependency-aware selection
around it. Scenarios currently share one world, so first make selected scenarios
self-contained or declare their prerequisite setup. A test which only passes after
another scenario is not independently selectable. Each selected run uses a fresh
output directory and disposable world; stale images cannot fill a missing result.

GameTests currently share one namespace, so `neoforge.enabledGameTestNamespaces`
is not a feature-group selector. NeoForge 21.1.249 exposes `RegisterGameTestsEvent`
for explicit registration. Register selected classes through that supported event;
remove their automatic `@GameTestHolder` discovery so it cannot silently register
all classes again. Preserve explicit template namespaces, test names and before/after
batch hooks, including the crop-growth batch. No engine patch or reflective deletion
from the registry. Verify full registration against the existing inventory before
turning filtering on.

Proposed command shape: `scripts/verify.sh --scope changed --base COMMIT` and
`scripts/verify.sh --scope full`. These flags do not exist yet. The selector resolves
one complete selection before launching either Minecraft process. A repair can use
an explicit group, but task completion recomputes the required union from the diff.

Every run records selected IDs, discovered IDs, executed IDs, pass/fail/skip status,
start/finish time, source tree, artifact digests and log paths. Empty selections,
missing expected tests, crashes, timeouts, disabled required tests and incomplete
capture reports fail. A generic “all required tests passed” line is not enough to
prove the intended selection ran. Keep failed suites separate from passing ones.

Emit bounded per-suite `KTASK_CHECK_RESULT` records for ktask, with report paths.
Preserve full raw logs and actual screenshots separately. The reviewer receives the
selection reason, concise failures and coverage inventory, then opens the necessary
proof. Smaller reports must not mean hidden omissions or relaxed assertions.

### Keep the suite itself efficient

Move combinations of accounting, shape matching, slot rules and reservations into
pure core tests. Retain real GameTests for each engine boundary: placement, sided
capabilities, save/reload, unload, breaking, multiplayer access and recipe integration.
Do not replace a save/reload test with a codec round-trip or simulate the batch in
place of its server execution.

Keep a few complete production routes for shared behavior. Test individual failure
conditions with short fixtures instead of repeating the entire colony bootstrap.
Observe completion conditions rather than padding waits. Preserve real long-running
cycles where sustained throughput, reserve depletion or growth is the property.

Measure per-group runtime and startup time before changing concurrency. GameTests
share a server and some global budgets/gamerules; isolate those batches rather than
starting competing servers in the same world. If one process eventually becomes too
slow, shard only independent groups into separate worlds, ports and output paths.
Keep full coverage and combine explicit inventories. This is a later option, not the
first optimization on this workstation.

### Concrete implementation order

1. **Record verification inventories and timings.** Add per-suite structured reports,
   expected/discovered/executed ID checks and capture timing. Preserve current full
   runs. A crash after a passing suite must remain a failed overall run.
2. **Select related capture scenarios.** Add the ownership/dependency map, standalone
   scenario setup and fresh output handling. A Gizzard-only change selects its states,
   guide/demo evidence and factory route; a shared renderer change selects all captures.
3. **Select GameTest groups in one server run.** Use explicit supported registration,
   preserve batch hooks and verify full-mode inventory equality. Keep the full
   headless task-completion gate during this rollout.
4. **Enforce scoped proof in ktask completion.** Pass the baseline and selected scope,
   pin evidence to inputs, and keep unresolved failures mandatory. Before accepting
   targeted completion, run it alongside full verification on representative changes
   to recipes, ports, shared rendering and persistence, including deliberate faults
   in dependents. A missed failure repairs the map and keeps full mode mandatory.
5. **Schedule full integration at queue boundaries.** Implement the five-task/one-day
   limit and milestone/release full gate. A failed full run prevents later promotion;
   fixing it includes focused proof and a fresh full integration run.

Each item is implementation work with its own verification. No research, standalone
approval or generic qualification tasks are needed. This plan adds no queue entries.

Technical references: [Gradle test filtering](https://docs.gradle.org/current/userguide/java_testing.html#test_filtering)
and [NeoForge 1.21.1 GameTests](https://docs.neoforged.net/docs/1.21.1/misc/gametest/).
The registration details above were also checked against the installed NeoForge
21.1.249 source JAR, `RegisterGameTestsEvent` and `GameTestHooks`.
