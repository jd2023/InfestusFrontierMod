# Engineering

For the builder and the worker. Rules about git and roles are in [AGENTS.md](AGENTS.md).

## Toolchain and commands

Minecraft 1.21.1, NeoForge 21.1.249, Java 21, Gradle 9.2.1, ModDevGradle 2.0.146, Parchment 2024.11.17. Exact pins: [Dependencies](#dependencies).

```bash
./gradlew spotlessApply                     # format everything; run before every commit
./gradlew :core:test :ui:test               # fast, no Minecraft
bash scripts/verify.sh                      # the gate; log in build/verification/full-gate.log
./gradlew verifyCaptures                    # client screenshots into build/captures, then pixel checks
./gradlew runClient                         # client in runs/client
./gradlew runClient -PwithJei -PwithCurios  # with the optional mods
./gradlew runServer                         # dedicated server in runs/server
bash scripts/demo.sh                        # fresh demo world with every feature on show, in runs/demo
bash scripts/demo.sh -PdemoScene=equipment_showcase # living equipment and sentry, ready to play
```

Output: `build/libs/infestusfrontier-0.1.0-dev.1.jar`. It holds the mod, `core` and `ui`. It never holds the test mod.

| Path | Holds |
|---|---|
| `core/` | Pure rules in `org.jd.infestusfrontier.core.<owner>`. Plain Java, JUnit. No Minecraft. |
| `ui/` | Screen layout grammar, sheet geometry, palette. Plain Java, JUnit. |
| `src/main/java/org/jd/infestusfrontier/` | The NeoForge mod: entry point, one folder per owner module. |
| `src/main/templates/` | `neoforge.mods.toml` with the version expanded at build time. |
| `src/testMod/` | Development-only GameTests and the capture client, mod id `infestusfrontier_tests`. |
| `src/testMod/resources/captures/` | One JSON per capture scenario; `golden/` holds approved screenshots. |
| `docs/design/` | Gameplay design. One owner document per subject. |
| `docs/sprints/` | One file per sprint (`S01.md`, ...). Art tasks live in ktask; source records live in `CREDITS.txt`. |
| `scripts/verify.sh` | The gate. |

## Architecture

```text
InfestusFrontier (composition root)      registers modules, owns no gameplay
  └── <owner>/                           NeoForge adapters: blocks, items, block entities, menus, packets, screens
        └── <owner>/api                  feature commands and immutable queries
              └── core/.../<owner>/      pure rules, JUnit tests
foundation/screen/                       the screen kit: OrganScreen and its parts, the only code that draws
foundation/menu/                         OrganMenu, the server half every organ screen shares
ui/                                      layout grammar, sheet geometry and palette, used by the kit and by menus
```

- Arrows point inward only. No cycles. `core` and `ui` import only the JDK; the build fails otherwise. `src/test` holds ArchUnit rules that `./gradlew test` runs: no cycles between feature packages, an `api` package reaches its module's implementation only through the `*Module` class, client classes (`net.minecraft.client`, NeoForge client, Blaze3D) are used only by `foundation/screen`, `foundation/render` (shared block-entity renderers), `*Screen`, `*Module` and `integration`, and block entities stay package-private.
- One behavior has one owner module. One folder per owner. No global `block/`, `item/`, `menu/` buckets.
- Each owner registers its own content. The entry point calls one `register` per module.
- Public types live in `<owner>/api`, plus one `<Owner>Module` class with `register(IEventBus)` and the module's registry handles. Everything else is package-private.
- Registry ids are flat `infestusfrontier:<snake_case>`, the catalog name in snake case: `living_substrate`, `spore_culture`. Block state property names are nouns: `stage`, `facing`.
- Recipes, tags, loot tables, models, blockstates, advancements and guide pages are JSON written by hand under `src/main/resources`. No data generators until a story shows more than 50 files of one kind.
- A feature that needs another feature declares a small port interface in its own `api`. The composition root connects it.
- Commands return success or a refusal. A refusal names the unmet condition and changes nothing.
- Queries return immutable snapshots, never a live collection.
- Screens read snapshots and send intents. Every decision runs on the server.
- Rendering is client-only and never decides whether a resource moved.
- Use vanilla and NeoForge behavior as it is. Never patch, bypass or re-implement engine rules. If a story seems to need that, stop and report.
- Shared low-level code (UI primitives, serialization, packet validation, bounded simulation, discovery) lives in one module from its first use. Features compose it, never copy it. Do not build it before the first consumer.
- A feature screen extends `foundation.screen.OrganScreen`, adds parts in `addParts(PartRow)`: `Emblem` (the organ's own mark, or none), `Well`, `Vein`, `Tank`, `button`. It never draws. A new kind of part goes into `foundation/screen` with its sprite in `ui.Sheet` and `art/gen_gui_sheet.py`, then every screen can use it. `verifyScreenKit` fails the build when a screen outside the kit touches `GuiGraphics`. Menus put their slots at `Sheet.SLOT_X, Sheet.SLOT_Y` and following row positions; a `Well` throws when the slot is off the row.
- Comments state a contract. Narrating comments are defects.

## Code style

The formatter decides layout. `./gradlew spotlessApply` formats. `check`, and so the gate, fails on unformatted code. Do not fight the formatter and do not add style exceptions.

- Palantir Java Format: 4 spaces, 120 columns, no unused imports. `.editorconfig` tells editors the same.
- `core` and `ui` compile with `-Xlint:all -Werror`. A warning is a build failure. Fix the cause, do not suppress it.
- A class is `final` unless a story says it is extended. A field is `private final` unless it must change.
- A value is a `record`. A result with several shapes is a `sealed` interface with record cases, read with a pattern `switch`, no `default` branch.
- A command returns a result with one success case and one refusal case. The refusal names the unmet condition. The first story that needs it creates the type in `core`.
- No `null` across a public boundary. Return `Optional` or an empty snapshot. Accept nothing nullable.
- No static mutable state. No reflection, Mixins, access transformers or coremods.
- `var` only when the type is written on the same line.
- Names: registry ids `snake_case`; Java `UpperCamelCase` types, `lowerCamelCase` members, `UPPER_SNAKE_CASE` constants. Suffixes: `XxxBlock`, `XxxBlockEntity`, `XxxItem`, `XxxMenu`, `XxxScreen`, `XxxPayload`, `XxxRecipe`. GameTest classes: `<Owner>Tests`.
- A test name states the behavior: `refusesWhenFull`, `restoresBatchAfterReload`. One behavior per test. No sleeps, no unseeded randomness, no shared mutable fixtures.
- One logger per owner module. No logging inside tick methods.

### Owners

Names are project contracts. Create a module with its first story, not earlier.

| Owner | Owns |
|---|---|
| foundation (`core`) | Shared work admission (`TickQuota`), checked quantities, bounded identifiers |
| organ | Organ identity, completed-batch counts, earned choices |
| processing | Bowl, digestion, rack, loom, furnace, activation, the Mineral Gizzard, the Washing Kidney, and the Shaping Palm: reserved batch execution; `OrganWork` is the one batch lifecycle; `BatchOrganKind` makes the Bowl, the Rack, the Loom, the Cradle, the Bio-Furnace, the Gizzard and the Activation Cyst one block, block entity and menu (a kind has one to three output slots, a recipe's result and byproducts fill them in order); the treatments the Bowl and the Cyst make are its items; a kind's `BatchBook` is its data recipes, the vanilla smelting book, or for the Bio-Furnace both; `core.organ.Growth` turns completed batches into stages, mirrored in the `stage` block state; the Shaping Palm (all three stages ship) is not a batch organ: its own block, block entity and menu (`PalmCrafting` reaches the engine's result lookup and asks its remainder rule whether a craft has room; the menu is the vanilla result slot over nine kept cells), with no item handler until it has Rote; stage 2 adds Reach and three screen actions that reach storage only through the Mouth under the Palm (`PalmSupply` asks that Mouth's `logistics.api.ItemAccess`; logistics owns the network, the Mouth rules, the sided inventories and the shared work, and the Palm keeps no position, link or inventory of its own; the bounded insert and the double-chest guard are `storage.api.ItemStores`, shared with the Mouth); stage 3 adds Rote (`PalmRote` in `processing.api` is its saved and carried state, `PalmPlan` the recipe lookup and the tray's room, `PalmPorts` the item handler of 9 grid and 4 tray slots; the block entity ticks, holds the 1,000 BU tank as a `BiomassHandler`, and takes its cycle starts from the `palm_starts` quota of 32 a server tick) |
| storage | Item, fluid and biomass balances; reservoir cell accounting; `Bodies` is the life of every storage body (form, join, fall apart), now the Reservoir's; a Capsule Stem's plan is `core` `StemPlan`, its root grows it and its one store is `CapsuleTree`, behind the item handler of every cell and pod and `api.ItemStore`; pod kinds are `core` `PodKind`: the Mixed Craw and the Larder (each its own block, item and block entity, a mutation that replaces the pod's block), the Larder with its labels and reserves (`core` `Reserves` holds the arithmetic), and a reserve enforced in `CapsuleTree.extract` and `take` only |
| construction | Loaded-only structure validation, shell parts, paid placement, transplanting |
| ecology | Substrate maturity, function, lining, reinforcement, dye, deliberate conversion; Dead Tissue, what any broken grown cell leaves; living walls (`LivingWallBlock`, three tiers), the grafts that harden them, and the openings mutated from them (`MembraneWindowBlock`, `SphincterDoorBlock`, a vanilla door drawn by GeckoLib); vegetation infection (`Vegetation` judges what stands on a host and converts the host through it; `WitheredFlowerBlock` with its `FlowerSource` is what a small flower leaves; `TallWitheredFlowerBlock` and `TallBlightedUndergrowthBlock`, both `DeadTallPlantBlock`, what a tall flower and tall grass or fern leave; `DeadShrubBlock` with `DeadBerryBushBlock`, `BlightedDeadBushBlock`, `BlightedUndergrowthBlock` and `WitheredSaplingBlock`, what a berry bush, dead bush, short grass or fern and a sapling leave; `InfectedLogBlock` and `BlightedLeavesBlock` are what every infected tree is made of, one vanilla pillar and one vanilla leaves block with no origin, and `InfectedPlanksBlock` the planks old worlds keep; `TreeInfection` is which blocks a tree is made of (the `infestusfrontier:infectable_logs`/`uninfectable_logs`/`infectable_leaves`/`uninfectable_leaves` tags and property checks), how a wave or a culture touches a standing tree and what one of its blocks becomes, `TreeInfections` each dimension's saved jobs, the shared budgets and the tick, over `core.ecology.TreeDiscovery`, the bounded discovery of one rooted tree by block kind) |
| cultivation | Crop and tree growth, allocated harvest, planting reserves; `CultivationTissueBlock` answers NeoForge's `canSustainPlant` and `isFertile`; a `PlotOrganBlockEntity` acts once a pulse on its `Plot`; the Seed Pouch's `surplus()` is what pullers see |
| logistics | Passive topology (`NetworkTopology`, persisted connection knowledge) and incremental shared components (`core.logistics.NetworkIndex`); loaded endpoint transfers through Mouths and `NetworkFluid`; `api.ItemAccess`, the capability on a Mouth through which the organ standing on it (the Shaping Palm) pulls from and pushes to the network in one bounded transaction (`MouthAccess`); server/dimension admission in `NetworkWork`; the Junction owns its local tank and pumping ports. Veins, crossings and stitches own no contents or ticks. Mouths retain only an undelivered remainder. Lobes expose sided organ inventories; Filter Valves enforce source reserves. |
| control | Signals, interlocks, selection, finite schedules; the Sensor Polyp reads one faced block into its `power` state |
| genetics | Samples, species and quality counts, genome coverage, native eligibility, cultured stock |
| equipment | Armor piece identity, fusion tree, counters, mutations, fuel, death preservation; the Fuel Papilla fills worn pieces from the ground |
| portable | Carry Sac and pouch slots; refuses nesting; adds no armor abilities |
| mineral | Workpiece stages, treatment, fracture, dust and tailings. The automated mineral workshop owns the dust, Tailings (`parcel` component) and Rock Residue items and the dust rules (`core.mineral`: metals, parcel bounds, the 4-4-4 accounting); the Gizzard and the Kidney run as `processing` batch kinds with `organ_batch` recipes (`core.mineral.Washing` holds the wash and settle rules) |
| excavation | Finite survey and cut plans, safe stepping, access and light |
| thermal | Heat, pressure, steam, cooling |
| energy | Biological electricity and explicit external energy exchange |
| containment | Spill volume, damage delay, isolation and recovery |
| spatial | End-native beds, anchoring, conditioning |
| transit | Endpoint links, player travel, durable freight escrow |
| indexing | Store summaries and bounded material requests; holds no item copies |
| husbandry | Animal work, feed, containers, berths |
| nutrition | Food and potion recipes; runs through processing ports |
| helpers | Nursery population, jobs, routes, paid Sail chunk tickets |
| defense | Hostile targeting, firing, projectile limits |
| synthesis | Sockets, lobes, idle profile switching |
| ui (`ui/`, `foundation/screen`, `foundation/menu`) | Layout grammar, parts, focus, theme, tooltips; the shared organ menu |
| interaction | Probe targeting, reach and ownership checks, dispatch to feature intents; the Surgical Blade, the fast tool for organs |
| discovery | Recipe reveal, advancements, guide prerequisites |
| integration | Modonomicon, GeckoLib, JEI, Curios adapters; no domain state |

### Invariants

- Processing reserves inputs, outputs, containers, waste and target before it starts. One active batch per organ. Saved stage, not wall time, drives recovery.
- Every organ runs without a player: it starts on its own when its inputs are complete, hoppers and pipes load it through its item and fluid capabilities and take its output, and its batches are credited to the last player who loaded it by hand, hopper batches after that included. A batch credit names the recipe the batch ran (`OrganBatchCompletedEvent.recipe`, kept in `PendingCredits`), so an advancement can tell a grinding from a washing. The Proboscis, the Corolla, the Sac and the Palm also keep a steward (`organ.api.Steward`) who is credited for their automatic work (`AutomaticWorkEvent`, offline too, in `PendingCredits`); a Sac batch is automatic only when a Mouth put its item in, and a Palm cycle only when a Mouth fetched one of its ingredients (`PalmFetched`, `storage.api.MouthFed`). What a player does at a screen (a stack stored in a Capsule Tree, a Palm craft that spent a fetched item) is a `WorkshopActionEvent` to that player, with no credit store. The rule: [GUIDE_PROGRESSION_TREE](docs/design/GUIDE_PROGRESSION_TREE.md#shipped-advancement-tab). A slot admits only a kind of item no other slot holds and that one batch can use with the rest, so a hopper cannot clog it. The Bladder's Fill is the last hand-only step; M3 gives it an automated path. Passive veins establish network access. Junctions transfer liquids through configured ports; output Mouths transfer items only after an intake accepts a simulated offer. Known unloaded connections may remain usable; unloaded endpoints are unavailable.
- Saved state is judged as a whole on load and on placement (`Persistence.read` with a check). What fails is kept as unreadable bytes: saved back unchanged, carried on an organ's item as `unreadable_save`, refusing every command and every insertion, and counting as contents when a Creative player breaks the organ. Nothing is silently dropped or replaced.
- Grown tissue never moves. Living Substrate and every cell grown in it (veins, crossings, stitches, Mouths, beds, papillae) give only Dead Tissue when broken, by any tool or Silk Touch, and no piston pushes them; what such a cell held dies with it, except a Mouth's undelivered remainder, which spills. Organs (`Organs.ORGANS`) are hard to break (`Organs.HARDNESS`) and come out whole, carrying their contents and history; the Surgical Blade is the one tool made for them. The Capsule Stem is tissue (Dead Tissue when broken, regrown from its root's plan) and the Item Capsule pod is an organ.
- A reserve belongs to the store that keeps it. Every external extraction passes the store's own reserve check; no caller re-implements it. Only a player at the screen, by an explicit action, overrides it. A Mouth's amount is a different, per-Mouth setting.
- Mineral dust has one source: a raw item of iron, gold or copper through the Gizzard, and the Kidney's wash of its Tailings. No recipe turns an ingot, block, tool or ore block into dust, Tailings or residue. A batch holds its products until they fit; waste has a finite buffer, and a full one pauses the organ.
- Organ history has one owner. Wall blocks never copy a core's history.
- Balances have one owner (`storage`). Routes and indexes hold reservations, not totals. Integer math is checked.
- `equipment` owns every suit predicate and every fuel spend. Nobody rebuilds the full-set check.
- `genetics` owns native-class predicates. Being in a dimension, owning a genome and having a native bed are three separate conditions.
- Freight has one owner at every step and a bounded escrow.
- A record carried as a data component is a value: item stacks are copied in and out and compared with `ItemStack.matches`.
- Optional adapters load only when their mod is present. Test mod code never ships.
- An organ opens its screen for any hand, like a chest, through `Organs.openScreen`. Only a bucket, a feed item or an ingredient acts on the organ instead; an awakened piece opens the screen and is filled from the Bladder's slot. Sneak to build against it.

## Performance limits

The gate runs with fixed heaps: the Gradle daemon 2 GB (`gradle.properties`), the GameTest server 2 GB, the capture and demo clients 3 GB (`build.gradle` runs). A leak fails the gate instead of starving the machine. The GameTest server run times out after 15 minutes and the capture run after 30, so a client stuck on a crash screen fails the gate instead of holding it.

Performance is correctness. A feature that can stall a server is a broken feature.

Before implementation, the story states: the work unit, who shares the budget (dimension, team, network), the hard per-tick bound, which blocks it reads, maximum entities and geometry, persistent state size, packet rate, behavior on overload, behavior on chunk unload and reload. Each has a test.

- No unbounded loose items, XP orbs or projectiles. Insert into bounded storage, keep a capped buffer, then pause. A player harvesting by hand may get bounded stacks.
- No implicit chunk loading. No spread, route, helper or cross-dimension lookup creates a ticket. A paid loader (Sail Roost) has owner, quota, upkeep, expiry and restart tests, and counts the engine-loaded neighbors too.
- No recursive flood fill, whole-world scan, position-keyed unbounded cache, ever-growing queue, unlimited inventory or attacker-sized NBT allocation.
- Budgets are shared through `TickQuota` or a sibling in `core`. Rate limiting one machine does not bound a world of machines.
- Refused admission does not retry every tick. `core.foundation.RetryGate` keys the retry on the state the refusal saw, with a slow periodic retry beside it.
- Idle systems sleep. Decorative blocks have no block entity. Refused work pauses and retries without losing items or multiplying pending work.
- Check `isLoaded` before any neighbor read. Vanilla neighbor updates, leaf decay, drops and third-party handlers can reach unloaded chunks.
- Sync changes only, as compact deltas. Particles are client-side, distance-limited, short-lived, and share one budget.
- The server owns all gameplay state. A client cannot mint resources, pick arbitrary targets, skip ownership checks or supply a mutation result.

| Rule from the design | Limit |
|---|---|
| Defence | At most 32 defence scans per server tick, shared across dimensions. A scan inspects at most 128 living entities and tests sight to at most 16 candidates; every ray refuses unloaded chunk crossings. Denied scans retry after 5–24 ticks without spending. Stores keep eight slots; unreadable saves remain opaque. Aerocyte pellets live at most 40 loaded ticks and share the scan admission for spawning (at most 1280 new pellets over that interval); projectile position updates are at most once per ten ticks per tracked entity |
| Sample collection | One death offers one sample to one destination. At most 64 offers per server tick. No kill log |
| Capsule pod UI | 2 organ slots plus 36 player slots, pinned to one pod (a Craw: 5 organ slots, a Larder: 4 and 3 label wells); a graft in the treatment well grows the pod at once, at most two grades, the rest stays in the well and returns on close; Withdraw reserve moves at most one stack a click |
| Network topology | 1,024 logical cells per component; 65,536 observed positions and 65,536 endpoint capability watches per dimension; 256 waiting discovery origins. Oversized components refuse instead of exposing a truncated result. Known unloaded tissue uses saved topology, never saved inventories |
| Network work | 2,048 discovery work units per dimension/tick, 8,192 per server/tick, shared by graph expansion and endpoint capability refresh. At most 64 endpoint work admissions per dimension/tick, 256 per server/tick. A simulated fluid offer reserves admission for its matching execution in the same tick |
| Endpoint transfer | At most 16 destination candidates per operation. A Mouth sends at most 8 items and retains at most 8 undelivered items; source scans 16 slots, generic destination insertion/count scans 64 slots. A fluid access request moves at most 250 mB to/from one endpoint. Junctions retain their 1,000 mB tank |
| Capsule UI ledger | 24 view cells over the tree's kinds (a kind's pods counted together, sorted by name) plus 36 player slots, pinned to one tree, with the kept count of each cell synced beside the row; a cell or pod leaving, a regrowth or an unload invalidates it. The kinds list is built once per change of the store, over at most 42 compartments (168 with Mixed Craws), and read from cache by every viewer; a scroll is one button click answered by the next broadcast; the screen sends no click on a cell until it shows the row it asked for, and the server refuses a click on a cell whose kind changed since its last broadcast (the round trip between that broadcast and the click stays open, as in any container). Vanilla syncs changed slots and thirty integers |
| Capsule Stem growth | One bone meal fed a use, a growth when the fed count meets its level's cost (1 to 32), on the user's tick; a root walks the plan once a tick per viewer of its tree screen for the growth line: the root walks its plan (at most 7 + 1 + 6 cells and 16 pods) once for repairs, then draws one step; no random ticks, no retries. A grown cell back in reach checks its root on one tick, asks again every 100 ticks only while the root's chunk is out of reach, and never judges an unreadable or plan-less root; a pod whose root is out of reach is served alone and read again on every use. A root breaking schedules at most one tick per cell of its plan. The tree's store is read again, once, after a change, and on every use while a chunk of its plan is out of reach: at most 56 block entities looked at. Pods pressed by hand may raise a plan to 42 pods, three a trunk cell; the saved plan refuses more than 14 cells or 42 pods |
| Mixed Craw and Larder (shipped) | 4 or 3 compartments a pod; a Larder adds a label and a reserve each (at most 13 settings a reserve, wrapping); a pod saves at most 4 compartments and 4 marks, and a saved kind that is not its block's is unreadable. A tree reads its slots from per-pod offsets built once per store. A reserve check is arithmetic on counts the tree already reads: no scan, tick or packet. Reserves apply to `extract` and `take`, not to `insert`; a ledger's per-kind kept count is built with the kinds, once per change |
| Mineral workshop (the Gizzard and the Kidney ship) | One batch a Gizzard or Kidney. A batch starts only if every product fits its tray; dirty water cannot be extracted by any path. Buffer sizes: [Items](docs/design/ITEM_CATALOG.md#first-release-the-automated-mineral-workshop). Recipe matching is the existing `organ_batch` plan, run on a change of the organ and at a batch end, never in a quiet tick |
| Shaping Palm (all three stages ship) | Stage 3: a Palm without Rote does one block-state comparison a tick and nothing else; a Palm with Rote does at most one attempt to start a cycle on a change of its grid, tray, biomass or pattern and otherwise one every 20 ticks, each attempt comparing nine stacks and, only when they match and 10 BU is held, doing one recipe lookup, one tray-room check and one admission from the 32 cycle starts a server tick shared across dimensions (a denied start retries after 5 to 24 ticks and spends nothing); a running cycle does an integer step a tick and one save mark; one restock per completed cycle through the Mouth under the Palm, one network admission, at most 9 requests, 16 far Mouths and 64 slots of a far store a request, and 9 grid cells; the item handler does constant work a call. Saved state is 9 grid + 4 tray stacks, 9 pattern kinds, one recipe id and one cycle (a result and at most 3 remainders); an older `link` position is read by nothing and dropped on the next save. Stage 2: no tick, no scan, no packet beyond the menu's four synced ints and a button id; the Palm looks at one position, the block under it, and loads no chunk; one button press is one `ItemAccess` call (one admission from the network work, at most 9 requests, 16 far Mouths, 64 slots of a far store a request) and touches at most 9 grid cells, and a refused press changes nothing. Stage 1: nine saved stacks, no tick, no scan, no packet of its own; one recipe lookup per change of the grid per open viewer and one before each click on the result, one inventory copy per click that takes the result; a refused click changes nothing and spawns nothing; a save of more than nine stacks, a stack over its limit or bytes that do not decode is kept as bytes. Recipe lookup at Select and at each cycle start only; no range beyond the 1,024-cell network, same dimension, loaded endpoints; Restock/Return/Store at most 9 grid cells and 64 slots of a far store. At most 30 crafts a minute (a cycle takes 40 ticks and a start follows the delivery by a tick, so 29) |
| Vegetation infection | One block read over each host a wave or a culture judges, two for a supported tall plant (the block over its lower half, in a loaded chunk), inside the existing twelve-read neighbour scan; one replacement per converted host, two for a tall plant, before the ground; no tick, scan, block entity, saved appearance or loose item; a dead flower's harvest is one nested roll of its source's loot table, a dead tall plant's one roll of its own table, from whichever half breaks first; a dead shrub, grass or sapling's one roll of its source's table, a dead berry bush's one roll of its own table by age; infected timber has no tick, scan, block entity, saved appearance or saved origin, a log's harvest is one Infected Log, a blighted leaf's nothing or, with shears or Silk Touch, one Blighted Leaves, and leaf support and decay are vanilla's through the `logs` tag |
| Tree infection | Contact is 12 reads beside a cell the wave converts or steps or a culture makes, plus one below each log found, in loaded chunks only. At most 16 jobs a dimension, discovering and converting together; 128 discovery reads and 16 block replacements a server tick across every dimension (`core.foundation.TickQuota` on the overworld's data), dealt round the jobs in turn. One job reads each position at most once, at most 2,048, inside a 17x32x17 envelope, and admits at most 128 logs of the root's block and 512 leaves of at most 4 kinds; eligibility is tag and block-state checks on the block read, no extra read; one read and at most one write per replacement, with no neighbour update; every refusal (capacity, oversized, ambiguous, unloaded) is one action-bar message to the players within 32 blocks, once per tree and reason in 200 ticks, and no queue. Saved state is at most 16 jobs a dimension, each of 640 positions with one state index each, a table of at most 640 block states (one a target), 2 block ids and the root's state; discovery keeps the state of each log and leaf it reads, at most 2,048, until it settles; a job in no loaded chunk waits 100 ticks between looks and loads nothing. No tick while a dimension has no job beyond one list check |
| Living tools | A carried living tool or fang mends on one tick in twenty, at most four points; a Contractile break reads at most eight neighbours in loaded chunks, each broken through the player's own block breaking, never recursing; a Grasping hit adds one vanilla effect after the blade's own hit; no scans, no scheduled ticks, no state outside the item |
| Spine Sentry | One entity scan of a 13x13x7 box every 20 ticks while it strikes, every 40 while it rests, one ray clip per candidate, no scan while dry or full; one hit, one particle and one sound per strike; drops routed by one event listener keyed on the damage type, never a world scan for items |
| Capsule membrane | One pocket on a pod's face, four triangles, drawn only while the pod holds something. Nine-second client breath with 0.12-pixel amplitude; no extra packets, server ticks, neighbor scans or persistent state |
| Sample UI page | 16 species rows, 48 quality counts, 2 changed pages per second per viewer |
| Portable supply | 4 transactions per wearer per second, 64 per server tick in total |
| Death preservation | Looks at the 4 worn pieces once. One loaded-endpoint lookup per piece. No retry queue |
| Death Bond | At most one pending piece per equipment slot per player |
| Hunger damage | One cadence per wearer, not one per piece. Floor at one heart |

Every feature survives: full outputs; broken, split or merged structures; cycles; unloaded endpoints; restart with resources in flight; corrupt or old saves; two players at once; ownership change; dead helper reservations; dense mobs; packet spam; unsafe teleport target; integer, coordinate and time limits; shutdown during a transfer. Nothing duplicates, nothing is lost, and the refusal is explained.

## Testing

`bash scripts/verify.sh` runs `./gradlew verifyAll`. Gradle decides pass or fail. The script does not parse log text.

| Layer | Command | Proves |
|---|---|---|
| Pure tests | `./gradlew :core:test :ui:test` | Rules, limits, edge cases. Milliseconds, no Minecraft |
| Boundaries | `:core:verifyBoundary`, `:ui:verifyBoundary` | Those modules have an empty classpath |
| Distribution | `verifyDistribution` | The JAR holds the entry point, `core`, `ui`, expanded metadata, and no test mod |
| Visual assets | `verifyVisualAssets` (Python 3 with Pillow) | Loop closure, bone references and UV bounds; overlapping storage/model faces; fluid continuity and repaired motion regressions |
| GameTests | `runGameTestServer` | Server-side behavior across real ticks, in `runs/gametest` |
| Client captures | `./gradlew verifyCaptures` | Real client screenshots in `build/captures/<scenario>/<view>.png`. Checked for missing textures and blank frames, compared with goldens when present. The reviewer opens the images |
| Demo world | `bash scripts/demo.sh` | Every shipped block, item and recipe in one fresh world, labeled. The owner plays it after each sprint |
| Manual play | `runClient`, `runServer` | Feel and usability. Owner only, at milestone gates |

- Every behavior gets a test that fails without it. Write the test first.
- Every reviewer finding gets a regression test.
- Test save and reload through a real save, not only the codec.
- Shared-budget tests isolate one fixture, then test contention separately.
- The GameTest server sets `randomTickSpeed` to 0 and ticks no chunk without a player. A test that needs crops to grow runs in its own batch, like `M3RouteTests`: `@BeforeBatch` raises the vanilla gamerule, a NeoForge ticket forces the test's chunks to tick, and `@AfterBatch` puts both back.
- A flaky test is a defect to investigate. Never rerun until green. Never delete the assertion. Never raise a limit or replace a golden image to get green.
- Fixtures and the test mod stay in `src/testMod`. Use disposable worlds under `runs/`. Never touch a personal save.
- Run graphical and headless runs as separate Gradle invocations. `scripts/verify.sh` runs `verifyAll` and then `verifyCaptures`.

### Captures

A view may request `frames` (1–96) and `frameTicks` (1–20, default 4) for a timed
sequence at the same camera position. The first image keeps the view name; later
images add `_001`, `_002`, and so on. The report records actual capture timestamps.
An optional `menuButton` sends one vanilla menu button after the screen settles, then waits for a fresh container ID before capturing a paginated screen.
An optional `trigger` command runs once when the camera is ready, immediately before
the first frame. Door sequences use it to capture redstone opening and closing.
The verifier checks every frame. `art/gen_motion_captures.py` owns the organ motion
fixtures: vanilla `tick freeze` holds idle/working, fill and port states while the
render clock runs. These prove appearance, not gameplay. Each scenario resets the
server with `tick unfreeze` before its commands.

`./gradlew verifyCaptures -PcaptureOnly=<scenario>,<scenario>` reruns and verifies
only the named scenes during visual iteration. The full gate always runs every
scene. `python3 art/motion_contact_sheets.py` makes timestamped contact sheets in
`build/art/motion/` from a completed capture report.

The capture client is part of the test mod. `./gradlew verifyCaptures` starts the real client, creates a fresh flat Creative world in `runs/capture`, runs each scenario, writes the screenshots and exits. Then a Gradle task checks every expected image.

A scenario is one JSON file in `src/testMod/resources/captures/<scenario>.json`, with 1 to 184 views. Scenarios share one flat world and run in name order, so a scenario that builds on the common plot near the origin first clears it (`fill ... air` above the ground, `fill ... grass_block` on it, then `kill @e[type=item]` for what the cleared organs dropped); the demo keeps its own rows to the north.

```json
{
  "commands": ["setblock 0 -60 3 infestusfrontier:living_substrate[stage=basic]"],
  "views": [
    {"name": "front", "x": 0.5, "y": -60, "z": 0.5, "yaw": 0, "pitch": 20},
    {"name": "hotbar", "x": 0.5, "y": -60, "z": 0.5, "yaw": 0, "pitch": 20, "gui": true}
  ]
}
```

- The harness sets frozen time and weather, no clouds, no view bobbing, and hides the GUI unless `gui` is true; before every scenario it clears the inventory and sets noon and clear weather again, so a scenario that sets midnight leaves nothing behind. `commands` run as the player, one per tick, before the views.
- A view teleports the player to `x y z` with `yaw pitch`. On the ground the player stands; in the air it flies and is pinned there. It waits until every chunk section around the player is rendered, and saves `build/captures/<scenario>/<view>.png`.
- The player's inventory is cleared before every scenario's commands, so a scenario gives itself what its views show.
- A view with `"use": [x, y, z]` and `"gui": true` right-clicks that block from where it stands and photographs the screen that opens, with an empty hand, or with the item named by `"holding": "<item id>"` selected in the hotbar (the scenario's commands give it). Every screen gets such a view in `demo.json`. A view with `"inventory": true` opens the player's inventory instead. A view with `"book": "<entry id>"` (empty for the guide's first screen) opens the guide there, `"page"` picking the left page of the pair shown (0, 2, 4, ...); the demo scenario grants the contact advancements first so every entry is unlocked. A view with `"advancements": true` opens the Advancements screen with the Infestus Frontier tab selected when the client knows it. `"scroll": [dx, dy]` drags the tab by that many pixels so a branch off the edge shows. A view with `"press": [button ids]` presses those menu buttons of the screen it opened, one every few ticks, and `"quickMove": [slot ids]` then shift-clicks those slots: the server judges each as it judges a player's. A view with `"feed": [x, y, z]` and `"holding"` right-clicks that block with the held item, as a player loads an organ by hand, and opens no screen. `"expectTab": true|false` holds the shot until the client does or does not know the tab; `"expectToast": "<advancement id>"` holds it until that advancement's toast shows. A wait that does not end fails the capture at the stage timeout. A scenario may also list `"play"` commands: only `scripts/demo.sh` runs them, after `commands`. The window is 1280x720; world views are shot at GUI scale 2, screen views at scale 3. A world view with `"icons": [item ids]` names the hotbar left to right (at most eight: the client parks the selection ring on the last slot); the verifier fails when an icon's centroid is more than 1.5 px off its slot centre or it draws outside its 16 px square.
- A capture fails when the image is missing, when more than 0.2% of its pixels are the missing-texture magenta, when the whole frame is one flat color, or when a golden exists in `src/testMod/resources/captures/golden/<scenario>/<view>.png` and more than 0.5% of pixels differ by more than 32 in a channel.
- Goldens are added only after the owner accepts the art. A golden is replaced only in a story that says so.
- The reviewer reads the PNG files. A passing check is not an accepted picture.

### Demo world

`bash scripts/demo.sh` builds the mod, deletes `runs/demo/saves/demo`, creates a fresh flat Creative world, runs the commands of the capture scenario `src/testMod/resources/captures/demo.json`, teleports the player to its first view and leaves the game open to play. The same scenario is photographed by `verifyCaptures`, so the gate sees the demo world too. Each exhibit is the blocks or items plus a waxed sign that names it.

Every change that adds or alters a block, item, recipe, screen or guide entry adds its exhibit to `demo.json`. Exhibits stand north of spawn in three rows, far enough that another scenario sees them only as a line on the horizon: row A at z=-36 with signs at z=-37 (blocks and organs, one per even x), row B at z=-33 to z=-31 with signs at z=-34 (ground plots), row C at z=-29 to z=-26 with signs at z=-30 (patches). The taken x positions are the ones `demo.json` already holds. The first command of `demo.json` is `forceload add` over the box of every exhibit (now x -6 to 174, z -47 to -2): the clients wait until the server has those chunks, so a `setblock` or `fill` never meets an unloaded one. An exhibit outside the box needs the box widened. A command that reports an unloaded position fails the capture; `scripts/demo.sh` logs `INFESTUS_DEMO_FAILURE`. `./gradlew verifyCaptureFailure` (run by `scripts/verify.sh`) proves it: the fixture `src/testMod/resources/captures_negative/unloaded_position.json` sends a `setblock` to x 4000 from spawn, and the task requires the capture client's report to fail with `argument.pos.unloaded`. Any other outcome fails the task. The owner must be able to see and try every feature there without commands.

The starter supplies stand at spawn, in view of the first screen: a chest at (2, -60, -47) with a sign beside it, holding logs, the Spore Culture ingredients (rotten flesh, a red mushroom, wheat seeds), string, bones, sticks, bone meal, a furnace, raw iron, a water bucket, a Biomass Bucket and a bowl. The `play` commands of `demo.json` revoke the Infestus Frontier tab (`advancement revoke @s from infestusfrontier:contact/root`) after the guide unlocks, so the owner who runs `scripts/demo.sh` starts with no tab: taking a log from the chest opens it, then the owner crafts the culture, plants it, and runs a Membrane Rack, a Bone Loom and a Bio-Furnace in the exhibits to the north. The `advancement_tab` scenario does the same on the real client without a grant: it shows no tab, gives a log and then a culture, loads a Rack, a Loom and a Furnace by hand, and waits for the toast of each milestone. It then opens a farm line (a Sac on an intake Mouth, a gap in an Item Vein, a Harvest Corolla on an output Mouth, beds and a Proboscis): seeds loaded by hand, the Corolla and the Sac opened once, the crop ripened by command and the vein gap closed by command, and waits for the toasts of the planting, the harvest and the goal. It then opens the Workshop steps row (an ore row, a Palm on a chest and a Palm on a Capsule Tree): raw iron by hand in the Gizzard, a Tailings in the Kidney and the fourth dust in the furnace, each waited for in turn, a stack put into the tree's list by a shift-click, and a Restock and a shift-click on the result at the Palm (the scenario's `press` and `quickMove`). It waits for the toast of Raw Ore to Dust, One More Dust, A Workshop That Grinds (which needs Dust to Ingot), Stock in the Tree and Crafted From Stock.

The Workshop steps exhibit shows in a block south of row C at x=84 to 112, z=-22 to -15, with a signpost at spawn (16, -47): an ore row (a Gizzard, a Kidney and a Bio-Furnace each with biomass in its tank, the Kidney with water, and beside each a chest of what it takes: raw iron, gold and copper; Tailings of 4 units; dust), a chest of logs and a Reach Palm joined by three Item Veins, and a Capsule Tree of bulk, Craw and Larder pods joined the same way to a Reach Palm that holds six iron ingots, with a stock chest. Signs number the steps. A visitor earns Raw Ore to Dust, One More Dust, Dust to Ingot and the goal by loading the three organs by hand, Stock in the Tree by putting a stack in the tree's list, and Crafted From Stock by pressing Restock on either Palm and taking the craft. Nothing is granted or commanded. `WorkshopStepsExhibitTests` builds the block from the demo's own commands and earns them that way.

The farm in row B at x=14 to 25 is an automatic supply line a player can finish with the starter chest's seeds and bone meal or the chest beside it: a Planting Proboscis with no seed, young wheat beds, a Harvest Corolla on an output Mouth, Item Veins, and a Digestive Sac on an intake Mouth, with signs that say to open the Corolla and the Sac once. Nothing in it is ripe, loaded or tended, so the owner earns the three farm advancements of the tab by loading the Proboscis, opening the Corolla and the Sac, and bone-mealing the wheat. The Seed Pouch stands at x=22.

The automated mineral workshop adds its exhibits (a Gizzard-Kidney-furnace line with a raw-ore chest, a Craw, a Larder with a reserve and a Palm at each stage) in the free positions of rows A to C, and its book entries and captures, in the same tasks that add the content. Shipped: the Gizzard line in row C at x=44 to 58 (raw-ore chest and hopper, Gizzard, Yield Lobe and filtered Mouths, a dust furnace, an ingot chest, the five items in frames), the Kidney line in row C at x=62 to 76 (Tailings chest and hopper, Kidney, water pool and Junction, biomass Reservoir and Junction, Yield Lobe and Mouths, a sink chest), the `mineral` scenario and the two guide entries. The Craw shows in row C at x=80 to 86: a bulk pod of raw iron, a Craw of hoppers, pistons, observers and repeaters, an empty pod with a chest of Elastic Gel and Skeletal Grafts, and the guide entry. The Larder shows in row C at x=90 to 102: an empty Craw with a chest of Sealing Resin and Skeletal Grafts, a stocked Larder of three kinds with reserves, a tower (a chest of iron ingots over a feed hopper, a Larder keeping 16 iron, an out hopper and a chest) that stops at the reserve and resumes when the top chest is refilled, and the guide entry. The Shaping Palm shows in row C at x=108 to 124: a Palm with a cake in its grid, an empty Palm, a chest of craft ingredients (milk, honey, planks, ingots), a chest of the Palm's own parts, a Palm under a hopper that cannot load it, and the guide entry. Its Reach shows in row C at x=128 to 148: a Palm with Reach on a Mouth, joined by three Item Veins to a chest, a Palm with Reach on a Mouth joined to a Larder that keeps 16 iron, and a kit (a Palm without Reach, a chest of Contractile Fibers, Mouths, Item Veins and a Synaptic Probe), and the guide pages. Its Rote shows in row C at x=150 to 168: a working cake line (a Palm with Rote and the cake pattern selected, a biomass Reservoir and Junction, a chest of ingredients over a hopper, a Yield Lobe on a Mouth and a chest for the cakes and buckets), a plank line at x=150 to 151 that keeps working for about a quarter of an hour (a Palm with Rote and the oak planks pattern on a Mouth, a chest of logs over a hopper, a Reservoir and Junction behind it and a chest of planks on an intake Mouth; `PalmRoteTests` runs it from the demo's own commands), a Palm with Reach beside a chest of Synaptic Gel, biomass buckets and planks, and the guide pages.

The Woodland plot of tree infection stands east of the Timber exhibit at x 79 to 115, z -13 to -2: a living oak at the end of a one-wide corridor from a basic cell under a bone-meal dispenser with a button (bone meal by hand works too), its infected twin, a birch and a cherry with touching crowns and a cell by each, a treehouse of planks, a stripped log, a chest and a fence against an oak, a 2x2 spruce of 132 logs that is refused, and a chest of bone meal, culture, an axe and shears. `TreeInfectionTests` replays the oak from the demo's own commands. The Jungle plot east of it at x 117 to 129, z -13 to -2 shows a living jungle tree with four cocoa pods beside its infected twin without pods; `TreeInfectionTests` replays it too. The Grove east of that at x 131 to 165, z -13 to -2 sets a living grove (a 2x2 dark oak with lying branches, an oak with a low limb, a birch, a spruce) beside the same grove in Infected Logs and Blighted Leaves, and a frame built of Infected Logs with an old plank; `InfectedTimberTests` replays it.

The iron-bars factory shows in its own block south of row C at x=0 to 31, z=-22 to -15, with a signpost at spawn (8, -47): a Larder of 18 raw iron keeping 3 (the input), a Gizzard, a Kidney with a water pool and Junction, a waste chest, a Bio-Furnace, a Palm with Rote and the vanilla iron-bars pattern, and a chest of bars (the output), joined by 11 Mouths with filters, Feeding and Yield Lobes and an Item Vein bus, with a Reservoir of 4,000 BU, a biomass trunk under the organs and signs for the input, the output, biomass, water, waste and the reserve. It starts working when the world is built; the player carries raw iron, two Biomass Buckets and cobblestone to restock it, fill an output and cut a supply with the probe. Its guide entry is `circulation/iron_bars_factory`; the plate on its first page is drawn by `art/gen_book_iron_bars_factory.py`. `IronBarsFactoryTests` copies the demo's commands in that box into the test world and runs the line, so the exhibit and the test cannot drift apart.

Two Palm supply exhibits show in a block south of row C at x=40 to 80, z=-22 to -15, with a signpost at spawn (12, -47): a Capsule Tree (a root on a Mouth, a trunk of three cells, a bulk pod, a Mixed Craw, a Larder keeping 16 of its 20 iron and a pod of coal) whose Mouth is joined by 12 Item Veins to the Mouth under a Palm with Rote and the vanilla iron-bars pattern, and a chest of 30 iron, planks and apples joined the same way to another such Palm; each Palm makes bars into a chest through an intake Mouth. Nothing in them is a command, a probe pairing or a preset link. `PalmExhibitTests` builds both from the demo's own commands and runs them: four crafts (64 bars) from the tree's 24 free iron with the Larder's 16 untouched, five crafts (80 bars) from the chest, a broken and a mended vein, a Palm saved in the middle of a cycle, and the Restock, Return and Store buttons on the tree's Palm. The storage Mouths are set to Both so Return can send items back.

Not covered yet: optional-mod profiles, multiplayer soak, crash recovery, measured TPS and FPS on a dense base. Each arrives with the feature that needs it.

## Stories and sprints

The owner directs, a strong model builds, a cheap model does mechanical tasks, and the owner plays when a milestone's organs are in. A sprint file is an optional plan; the template below is for when one is written, and for art stories.

A sprint is one milestone from ROADMAP.md, or one slice of it, and ends in a playable build. Its stories live in one file, `docs/sprints/S<nn>.md`: milestone, goal in one sentence, the acceptance line from ROADMAP.md, then the stories in order. Only the current sprint is written at this depth. Later milestones stay at one line each in ROADMAP.md.

A story is ready only when a worker can finish it without making a design decision or reading a whole catalog.

One story, one kind, one outcome. About 12 files and 400 changed lines at most. If the title needs "and", split it.

| Kind | Contains | Evidence |
|---|---|---|
| rules | Pure `core` types and JUnit tests. No Minecraft types | rules |
| platform | Block, item, block entity, codec, menu wiring, GameTests. Temporary or procedural assets allowed, identified in `CREDITS.txt` | game |
| data | Recipes, tags, loot, translations and their GameTests | game |
| art | Final textures, models, blockstates, icons, CREDITS line. Assets only, no Java. Only when the owner asks for a finishing pass | visual |
| complete | Guide entry, advancement or discovery step | game, visual |
| qualify | A scripted route or soak over accepted content. No new behavior | game |
| repair | One reviewer finding with its regression test | as needed |
| harness | Build, test or capture tooling in Gradle, `scripts/` or `src/testMod`. No gameplay | gate |

A content feature is one task: platform with functional temporary art, data, guide and demo. Final art is a separate finishing pass, only when the owner asks for one.

```text
IF-nnn  <one observable outcome>
Status: ready                # ready | doing | review <commit> | accepted <commit> | blocked <reason>
Milestone: M1
Owner: processing            # a module from the Owners table
Kind: platform
Depends: IF-012              # accepted stories only
Read: docs/design/BLOCK_CATALOG.md#t0-03--digestive-sac, src/main/java/.../processing/   # at most five
Files: src/main/java/.../processing/DigestiveSacBlock.java, ...   # exact allowlist, tests included
Do:
  1. Create class X with method Y(signature). Exact names, IDs and numbers.
Tests:
  - testName: what it asserts, and why it fails before the change
Done:
  - ./gradlew :core:test
  - bash scripts/verify.sh
Not: adjacent behavior that belongs to another story
Bounds: hard work and state limits, with numbers
Evidence: rules | game | visual
```

`Status` moves `ready`, `doing`, `review`, `accepted`. The worker writes `doing` and `review` with the commit hash. The builder writes `accepted` or `blocked`. The sprint file is the one file a story may edit beyond its `Files`, and only its own `Status` line.

How to do a story:

1. Read the story. If it cannot be done inside `Files`, stop and report which file or decision is missing. Otherwise set `Status: doing`.
2. Write `Tests` first. Run them. See them fail for the stated reason.
3. Implement in the owning module. Keep the public interface small.
4. Run `./gradlew spotlessApply`, then `Done`, then `bash scripts/verify.sh`.
5. Walk through the checklist in [REVIEW.md](REVIEW.md) against your diff.
6. Set `Status: review <commit>`. Commit, push to `origin/v4`, and report: what you did, what you did not do, what you measured.

The builder decides every name, number and interface before the story enters the sprint, after reading the real code. Common invariants (save and reload, break and replace, refusal leaves state unchanged) are written into the story that owns them.

`Evidence` names what the reviewer looks at: `rules` is JUnit output, `game` is the GameTest line in the gate log, `visual` is the capture PNGs, `gate` is the gate log itself.

The sprint loop:

1. The builder reads the real code and ROADMAP.md and writes the sprint file: 5 to 12 stories, each with `Depends` on accepted or earlier stories only.
2. The builder, or a cheap model for a mechanical story, takes the first `ready` story whose `Depends` are all `accepted`.
3. The reviewer gives the verdict. On `REJECTED` the builder fixes the findings in one commit per round. Three rejected rounds block the story and send it back to the builder.
4. When every story is `accepted`, the owner plays the build. What is broken becomes `repair` stories in this sprint. What is missing becomes stories in the next sprint.
5. The builder writes the next sprint.


### Art track

Functional temporary or procedural artwork is allowed throughout development and playable milestone qualification. `ktask` owns the art queue. Each art task names the assets, intended appearance, relevant generators, demo/book changes and capture acceptance in its own goal and criteria; it must not require a retired sprint file. `CREDITS.txt` owns provenance and identifies assets that are still temporary.

The planner appends requested art tasks after all existing work. They are ready implementation tasks, with checks and review inside each task. Never insert an art approval or owner-input task between development tasks. Workers report new placeholders in their result and credits; they do not file automatic art follow-ups. No development task depends on final art, and missing final art, its quality or its provenance alone never blocks development or a playable milestone.

Keep project model defaults unless the task needs a different capability. A new articulated model or animation design needs a strong model; rendering a guide plate from existing models and adding ordinary UI symbols normally use the default executor. Use an image-capable executor only when new image generation is required, and verify that the runner exposes that capability before queuing it as ready.
Still mandatory on every task: functional rendering, readable UI, correct behavior, tests, guide entries and labeled usable exhibits in the default `scripts/demo.sh`.

The art quality bar below applies to a finishing pass the owner requests separately. Such a task is appended after all existing queued work, never between development tasks, and no development task depends on it. An art task depends only on the task that registers its content.

Two kinds of art story. A texture story gives an image model the prompt, the size and the output path; whoever runs the task saves the result, exporting it with `scripts/texture.py` (and `art/fit_icon_source.py` for an icon). A model story gives a strong model the GeckoLib `geo` and `animation` JSON to write, with bone names, dimensions and animation names fixed in the story. Never mix the two in one story.

- A `platform` story ships functional placeholder assets: the geometry the catalog describes, with a texture at the shipped size (a flat two-tone texture at the least) and an item model; a vanilla-textured cube is not enough ([REVIEW.md](REVIEW.md) "Visuals"). The game must run and the reviewer must see the block. Workers never attempt final art. A task passes with honest functional temporary or procedural assets identified as temporary in `CREDITS.txt` and reported in the task result; functional rendering, readable UI, guide and demo coverage and the behavioral checks stay mandatory on it.
- Who makes finals: flat block tiles and item icons come from the image model (Astra) through a texture story. Organ models with their textures, animations and glowmasks, the GUI sheet and fluid textures come from the builder through a generator under `art/` (`art/organ_model.py` is the shared pipeline; `art/gen_<name>.py` is one model). The generator is the source; run it from the repository root and commit its output.
- `art/geo_audit.py` checks every GeckoLib model for faces fighting for depth (parallel, overlapping, closer than 0.1 px, in the rest pose and through the animations); `art/test_motion.py` runs it, `--all` checks every keyframe.
- Armor comes from the builder too (owner, 2026-10-24): `art/gen_armor.py` writes each set's two vanilla-layout layers, its growth sheet, the growth file `armor/<set>.json` and the icons, rendered from the pieces themselves so they always match. The base stays vanilla armor so dye, trims and the glint work; growths are drawn by `foundation/render/LivingArmorLayer`, a render layer on every humanoid renderer; a module registers a set with `LivingArmor.register`. Growth cubes keep whole sizes, never share a plane with the base shell and never reach another bone (`art/test_armor.py`).
- An animated organ: the block entity implements `GeoBlockEntity`, its item is an `integration.geckolib.OrganItem`, the module calls `OrganRenderers.register` on the client, the block returns `RenderShape.ENTITYBLOCK_ANIMATED` and its blockstate names only the break particle. Block state properties drive the animations; nothing extra is sent.
- Shipped assets live in `src/main/resources/assets/infestusfrontier/`. File name is the registry id. Block textures are 64x64 and item icons 32x32, exported from image-model sources with `scripts/texture.py`, unless the story says otherwise. Animated organs use GeckoLib models in `geo/` and `animations/`.
- Image masters and renders stay outside the repository; prompts, source digests and export settings live in `CREDITS.txt`; the ktask definition holds the implementation outcome. Model sources (`.bbmodel`) are committed under `art/`, next to any generator that bakes them.
- Every asset adds one line to `CREDITS.txt`: file, author, license.
- An art story is accepted on a client capture judged against the catalog description and the art quality bar below, by the checklist in [REVIEW.md](REVIEW.md) "Visuals". Final art quality is enforced only on a finishing pass the owner requests. It is not enforced on an implementation task or at milestone acceptance, and an art task is accepted on the capture and the bar above.

### Art quality bar

- The image model draws the source at its own size. `python3 scripts/texture.py <source.png> <shipped.png> --size 64` downsamples it, caps the palette and writes `build/art/<name>-tiled.png`, a 3x3 repeat to check for seams and repeated bright spots. Only the shipped file is committed.
- Blocks are 64x64, opaque and seamless on all four edges: the prompt says "veins continue across opposite edges". Items are 32x32 on a transparent background, one centered object.
- Original biological look: membrane, muscle, veins, pores, nodules. Broad readable forms, low noise. No grid, no border, no bevel, no central emblem, no text, no copy of an existing game texture.
- Prompts record: use case, asset type, subject, style, composition, palette, constraints, avoid. 16x16 pixel-art prompts with a five color palette gave the weak placeholders; sources drawn large and downsampled are what read as living material.
- One `CREDITS.txt` record per asset: file, who made it, tool, date.

The quality bar applies to a finishing pass the owner requests and is checked in its normal task review. Temporary assets are not judged against it and never hold a task or a milestone.

- A texture reads as its material from four blocks away in a capture, without a sign: flesh, membrane, bone, soil. The bar is the V3 substrate and the Astra living tissue from the model evaluation; weaker than those is rejected.
- Ground textures are a 4x4-block macro pattern, 256x256 source cut into sixteen 64x64 tiles, so a 4x4 patch shows no repeat and no seam. The stages of one ground share one vein network and one ground tone, derived from one master by a generator under `art/`. Other blocks get random rotation in the blockstate.
- One light direction, top-left, across every texture. Soft shading, no flat fills, no dithering noise, no outlines inside a block face.
- An item icon is one object with a silhouette that reads at hotbar size, a thin dark outline, and a highlight on the lit side.
- An organ item's icon sits centred in its slot: its visual centroid within 1.5 px of the slot centre, nothing drawn outside the 16 px square. The gate measures it on the organs hotbar capture; the numbers live in the item model's `display.gui`.
- A model has one bone per moving part. At rest no cube of one bone sits inside a cube of another bone, except plates named as layered. Every loop closes: the last key equals the first pose, and a rotating channel ends at a multiple of 360. No channel moves faster than 720 degrees per second. Emissive parts have a glowmask. No two parallel faces of different cubes overlap closer than a tenth of a pixel, facing either way, in any pose (`art/geo_audit.py`; organ models draw both sides of every face). Model textures are 64x64 per model; living armor growth sheets are 128x64 (both sets' growths share one texel per unit, as the vanilla armor layers do).
- Model sources are `.bbmodel` files committed under `art/`, openable in Blockbench; the shipped `geo` and `animation` JSON are exported from them. The first model story also adds the lint that measures the four rules above.
- Evidence for a texture: `build/art/<name>-tiled.png` and the captures. For a model: front, side, back, dark and an animation frame sheet.

### Model and animation verification

The [capture generator](art/gen_motion_captures.py) covers every shipped
GeckoLib clip, including each furnace stage, bladder fill and junction port mode.
Each organ scene shows idle and working side by side, followed by back and side
views. The Seed Pouch and storage scenes show their available resting states.
Timed frames and their capture timestamps are kept in `build/captures/`.
[Contact sheets](art/motion_contact_sheets.py) summarize them under
`build/art/motion/`; inspect the source frames for fine detail.

| Models | Visual checks |
|---|---|
| Culture Bowl | Pool stays inside its walls; bubbles form independently at the surface |
| Digestive Sac | Reads as a stomach: the acid window is the cue, the body stays quiet; the gullet stays seated while the sac squeezes; mouth and glands stay attached |
| Biomass Bladder | Reads as a bladder: a translucent sac swelling out of a lip of flesh and closing in under a lipped collar, bone ribs bowed down its corners, the level seen through it; no straight frame, no box; empty is empty; all fill levels stay inside the membrane (`motion_bladder_2` close, corner; `motion_bladder_0`..`4`) |
| Membrane Rack | Skin stays between posts; the droplet resets while invisible |
| Bone Loom | Reads as a ribcage loom: curved ribs gripping a plate woven in unequal strips is the cue; the ribs breathe and clench without parting from the mound; the shuttle stays on its strip and clear of the ribs (`motion_bone_loom` close, corner, back) |
| Awakening Cradle | Reads as something being brought to life: the glowing cocoon is the cue; closed petals clear the cocoon and its pulse, open ones stay inside the block |
| Bio-Furnace | All three stages; reads as a furnace from across a room, day and night: dark body, hot mouth and seams, fire and light only while it works; the fangs, coals, chimney, buttresses, bellows and second chimney remain distinct |
| Activation Cyst, Seed Pouch | Chambers, neck, rim and throat follow their supporting tissue |
| Planting Proboscis | Reads as a seed weevil: the long rostrum over the bed in front and the sac of seeds on its back are the cues; the stab reaches the bed and the seed is left in it; the rostrum never enters the head; legs and claws stay on the slab (`motion_planting_proboscis`: close, corner and low views) |
| Harvest Corolla | Moving parts remain attached; petals clear the heart; tips clear the floor |
| Spine Sentry | Reads as a rooted lash: the plated, barbed spine coiled over a muscle mound that slopes up to its bone collar, tendons pulling at it, is the cue; working, it swings from side to side and whips out without the segments parting; the thorn clears the mound (`motion_spine_sentry` close, corner; `sentry`: a zoglin beside it, the store screen) |
| Mineral Gizzard | Reads as a grinding muscle: two bone millstones in a lipped hopper on a wide, low belly wrapped in ridged muscle, and a bone chute with a drift of dust, are the cues; the stones swing against each other inside the hopper without reaching a lip; the ridges clench in a wave; the drift builds and slumps below the chute (`motion_mineral_gizzard` close, corner, top; `mineral`: screens, icons) |
| Washing Kidney | Reads as a rinsing organ: two soft blue-grey lobes under a pale clean-water sac and a murky dirty-water sac, a bone sieve between them and a low drip spout, are the cues; the lobes squeeze in a wave without parting, the sieve shivers between the sacs, the dirty sac swells while working, the drip stays under the spout and inside the block (`motion_washing_kidney` close, corner; `mineral`: screens `kidney_screen`, `full_kidney_screen`, `kidneys`) |
| Compost Gland | Reads as a fermentation gut of the mod's flesh: the gaping maw of scraps above and the meal-crusted spout below are the cues; the belly churns in a wave without its plates parting; the flanks swell at rest; the crumb of meal shows only while the spout presses, at its largest in `close_008` (`motion_compost_gland` close, corner, back) |
| Shaping Palm | Reads as a hand: an open pink hand palm up on a root foot, four fingers and a splayed thumb with ivory tips cupped round an ivory plate of three by three studs, is the cue; idle, the fingers breathe and nothing is lit; working, the fingers and thumb curl over the plate without touching a stud and open again, and the studs glow faintly (`motion_shaping_palm` front, close, corner; demo `palm_rote_line`, `palm_rote_screen`) |
| Seed Pouch | Reads as a seed sac: the crowned heap of seeds in flared lips and the split lobes are the cues; the body swells at its waist and tapers to the neck with no ledge; the lobes part without leaving the core; the cord stays round the neck (`motion_seed_pouch` close, corner, side) |
| Vascular Junction | Reads as a heart: the lobed body that beats and the lipped sphincters are the cues; all six rings in all three modes gape, pucker and shrink without their lips parting; the bottom ring stays on the foundation (`motion_junction_intake` close, corner; `motion_junction_closed`, `motion_junction_outlet`) |
| Reservoir | Joined bodies, exterior corners, fill surfaces and inventory models |
| Capsule Stem, Item Capsule | `capsule_stem`: a grown tree with a fork, pods with stock on their faces, three grades, the root's screen; `motion_item_capsule`: the pod breathes |
| Mixed Craw | Reads apart from a bulk pod at four blocks: bog-green hide with pale bone creases, an ivory cross dividing the socket into four cups behind a one-pixel rim, two knuckled ivory ribs on the flanks; stock sits inside the rim and in front of the cross at all three grades, empty cups still read as four (`mixed_craw` front, grades, `empty_grades`, `bulk_beside`, hotbar; `motion_mixed_craw` close, corner) |
| Larder | Reads apart from the Craw and a bulk pod at four blocks: plump waxed-amber hide with pale bone creases, an ivory strap along each flank ending in a latch hooked over the socket's lip, a one-pixel rim framing three pockets, two above one, the fourth quarter sealed flush with resin; stock sits inside the rim and clear of the seal at all three grades, empty cups still read as three (`larder` front, grades, `empty_grades`, `craw_beside`, hotbar; `motion_larder` close, corner) |
| Bud, veins, Mouth, lobes, cultivation, Polyp, Valve, Papilla | Existing static scenes plus `visual_static`: front/back views, recessed surfaces and connections |
| Dormant and awakened armor | `visual_armor`: front/back/side views with bent arms and legs; dyed, trimmed and enchanted sets; an adult and a baby zombie wearing them |
| Living walls, windows, crossings and stitches | `walls`, `openings`, `crossings`: all tiers, joined frames and routed faces |
| Sphincter Door | `motion_sphincter_door`: closed pulse and redstone opening/closing from both sides; sealed flaps and distinct edge planes |

[Asset regression checks](art/test_motion.py) run in the engineering gate.
They cover resource references, UV bounds, bone references, loop closure, rotation
speed, overlapping surface planes and the motion constraints above. In-game visual
review remains required; those checks cannot judge appearance by themselves.


## Dependencies

Change a pin only in its own story, with a cold build, a server run and a client run. No dynamic versions.

| Component | Pin |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.249 (metadata accepts only this version) |
| Java | 21 |
| Gradle wrapper | 9.2.1 |
| ModDevGradle | 2.0.146 |
| Parchment | 1.21.1 / 2024.11.17 |
| JUnit BOM | 5.11.4 |

| Mod | Artifact | Status |
|---|---|---|
| Modonomicon | `com.klikli_dev:modonomicon-1.21.1-neoforge:1.120.4` | Required. Renders the guide. `discovery` owns prerequisites |
| GeckoLib | `software.bernie.geckolib:geckolib-neoforge-1.21.1:4.9.2` | Required. Organ models and items; `integration/geckolib` is the only adapter |
| JEI | `mezz.jei:jei-1.21.1-neoforge:19.56.0.438` | Optional. API on compile classpath. Runtime with `-PwithJei` |
| Curios | `top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1` | Optional. API on compile classpath. Runtime with `-PwithCurios` |
| Jade, FTB Quests | none | Not used. Probe and console give diagnostics. Pack authors own quests |

Optional mods live behind an adapter in `integration/`. The mod works without them, and a test proves it. Before a release, read the license notice of every resolved artifact (expected: JEI and GeckoLib MIT, Curios LGPL-3.0, Modonomicon MIT with separately licensed assets). `LICENSE.txt` says CC BY 4.0 and old metadata said All Rights Reserved; the owner decides before any release.
