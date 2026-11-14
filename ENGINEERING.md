# Engineering

For execution and repair. Read [AGENTS.md](AGENTS.md) and the relevant gameplay specification before changing behavior. [VISION.md](VISION.md) takes precedence over older designs.

## Architecture

- `core/` holds pure gameplay rules; `ui/` holds layout primitives. Both depend only on the JDK.
- The mod entry point wires feature modules. Each feature owns its registrations, behavior and state. Public interfaces belong in its `api` package; implementation stays package-private except the module entry point.
- Dependencies point inward, without cycles. Features communicate through small interfaces rather than reaching into each other's implementation.
- The server decides gameplay. Screens display snapshots and send intents; the server checks reach, ownership, inputs and targets again.
- Screens compose `foundation/screen` parts. Shared layout and drawing belong in that kit, not individual screens. Menus use the same layout coordinates.
- Shared behavior has one owner. Reuse existing persistence, storage, work-budget and organ helpers. Add abstractions when a real consumer needs them.
- Use supported vanilla and NeoForge behavior. Do not patch or recreate engine placement, drops, chunk saves or neighbor updates.
- Optional integrations stay behind adapters. Test-mod classes never ship in the runtime JAR.

## Code style

Use the configured formatter. Keep classes and methods focused, names concrete and comments limited to contracts the code cannot express. No copied logic, unused code or speculative frameworks.

Use records for values and explicit result types for success/refusal. Public APIs use Optional or empty collections instead of null. Copy mutable ItemStacks across value boundaries. Use checked integer arithmetic for balances. Keep client classes in the existing client-facing packages.

Do not suppress warnings or weaken architecture tests to accommodate a change. Avoid static mutable state, reflection, Mixins, access transformers and coremods.

## State and resource ownership

- Validate before consuming. A refusal leaves inputs unchanged.
- Reserve batch inputs, outputs, containers and waste space before starting. Recover from saved progress, not elapsed wall time.
- Preserve unreadable saved data unchanged and refuse operations on it. Do not replace it with empty state. Breaking and replacing an organ must preserve contents and history.
- A store owns its stock and reserves. Every automatic extraction uses that store's reserve check. Routes and indexes do not invent another resource balance.
- Full outputs and waste buffers pause work. Each admitted input must have a supported output or recovery path.
- Automation applies to the stages designed for it. Deliberately manual stages, such as the first Shaping Palm, remain manual.
- Passive veins connect loaded endpoints. Known unloaded connections may be used from saved topology; unloaded inventories cannot be accessed.
- Tissue and organ removal follow their existing gameplay contracts. Multiblock parts do not copy the core's experience or reset its evolution branch.
- Use the shared organ interaction behavior: ordinary hands open screens; applicable tools or ingredients perform their action; sneaking permits building against the organ.

## Performance limits

Keep work, memory and network traffic bounded. Share work budgets across machines; a per-machine cap alone does not bound a busy colony. Idle systems sleep. Refused work retries after a relevant change or a bounded delay, not continuously.

Check chunk availability before world reads. Do not create implicit chunk tickets. Bound searches, queues, inventories, entity counts and saved-data allocations. Sync changes rather than full state. Load-test the mechanic being changed; prefer the simplest implementation that meets its limits.

Existing numerical contracts remain below until moved to their owning system specifications. Do not silently raise a limit.

| Rule from the design | Limit |
|---|---|
| Defence | At most 32 defence scans per server tick, shared across dimensions. A scan inspects at most 128 living entities and tests sight to at most 16 candidates; every ray refuses unloaded chunk crossings. Denied scans retry after 5–24 ticks without spending. Stores keep eight slots; unreadable saves remain opaque. Aerocyte pellets live at most 40 loaded ticks and share the scan admission for spawning (at most 1280 new pellets over that interval); projectile position updates are at most once per ten ticks per tracked entity |
| Sample collection | One death offers one sample to one destination. At most 64 offers per server tick. No kill log |
| Capsule pod UI | 2 organ slots plus 36 player slots, pinned to one pod (a Craw: 5 organ slots, a Larder: 4 and 3 label wells); a graft in the treatment well grows the pod at once, at most two grades, the rest stays in the well and returns on close; Withdraw reserve moves at most one stack a click |
| Network topology | 1,024 logical cells per component; 65,536 observed positions and 65,536 endpoint capability watches per dimension; 256 waiting discovery origins. Oversized components refuse instead of exposing a truncated result. Known unloaded tissue uses saved topology, never saved inventories |
| Network work | 2,048 discovery work units per dimension/tick, 8,192 per server/tick, shared by graph expansion and endpoint capability refresh. At most 64 endpoint work admissions per dimension/tick, 256 per server/tick. A simulated fluid offer reserves admission for its matching execution in the same tick |
| Endpoint transfer | At most 16 destination candidates per operation. A Mouth sends at most 8 items and retains at most 8 undelivered items; source scans 16 slots, generic destination insertion/count scans 64 slots. A fluid access request moves at most 250 mB to/from one endpoint. Junctions retain their 1,000 mB tank |
| Capsule UI ledger | 24 view cells over the tree's kinds (a kind's pods counted together, sorted by name) plus 36 player slots, pinned to one tree, with the kept count of each cell synced beside the row; a cell or pod leaving, a regrowth or an unload invalidates it. The kinds list is built once per change of the store, over at most 42 compartments (168 with Mixed Craws), and read from cache by every viewer; a broadcast reads the store, counts, reserves and the root's next-growth cost once for all its ints and cells, and a click reads the tree as it is then; a scroll is one button click answered by the next broadcast; the screen sends no click on a cell until it shows the row it asked for, and the server refuses a click on a cell whose kind changed since its last broadcast (the round trip between that broadcast and the click stays open, as in any container). Vanilla syncs changed slots and thirty integers |
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

Use focused checks for the changed behavior. Pure accounting and branching rules belong in core tests; engine boundaries need GameTests. Persistence changes need a real save/load case. A regression test must exercise the failure being repaired.

```bash
./gradlew :core:test :ui:test
./gradlew test
./gradlew spotlessApply
./gradlew verifyCaptures -PcaptureOnly=SCENARIO
bash scripts/verify.sh
```

Each capture run writes screenshots and `report.json` to its own `build/captures/<run id>/` (the negative fixture uses `build/captures-negative/<run id>/`), so a focused run never erases the evidence of an earlier one. The run id is a timestamp, or `-PcaptureRun=NAME`; an existing run directory is never reused. The capture commands print their run directory, and verification reads only it. Old run directories are not pruned; delete them by hand.

Under ktask, run focused checks during development. The supervisor owns the configured full check, independent review, commits and delivery. Do not start a second manual workflow. Documents and art categories have no project checks; assess their deliverables directly. Outside ktask, run checks appropriate to the change and report their actual scope.

The full check is `scripts/verify.sh`; read its current commands rather than copying them into task descriptions. Build settings and dependency versions live in Gradle files. Use the configured system JDK and Java release target. Dependency changes need matching build, server and client evidence.

## Feature completion

New or changed gameplay includes its recipes, translations, discovery, in-game guide and a labeled usable demo exhibit. The guide describes shipped behavior and renders recipes from recipe data. Keep demo scenarios under `src/testMod/resources/captures/`; use the relevant scenario rather than maintaining a coordinate inventory here.

```bash
bash scripts/demo.sh
./gradlew runClient
./gradlew runClient -PwithJei -PwithCurios
./gradlew runServer
```

## Art track

For generated assets, edit the generator under `art/`, run it from the repository root and commit its output; do not patch generated output alone. Keep editable model sources (`.bbmodel`) under `art/` beside their generators. Image masters and renders stay outside the repository; record prompts, source digests and export settings in CREDITS.txt.

Functional temporary assets may ship. Identify them and record asset provenance in CREDITS.txt. Missing final art alone does not block gameplay work or create an automatic follow-up. A finishing pass runs only when requested.

Textures and models must remain readable in-game and at inventory scale. Match collision and selection shapes to the visible object. Inspect front, back, corners and working states. Avoid missing textures, depth fighting, open seams and clipping; animation loops close cleanly. A finishing pass also checks consistent materials, lighting and repetition across joined surfaces. Use existing asset tests and actual captures; pixel checks alone do not establish visual quality.

## Ktask configuration

`config/ktask-roles.json` records role-specific reading instructions for code, documents and art. `python3 scripts/configure_ktask.py` applies them to this project's local ktask database without changing models or checks. Add `--refresh-pending` to update pending tasks to the current category templates. Neither command starts execution.

Ktask configuration is machine-local. Run the script after cloning on another computer if that computer will execute tasks. Explicit review lenses replace the ordinary category review prompt; configure their reading instructions separately before using them.
