# Module contracts

## Dependency direction

```text
composition (registry wiring, event subscriptions, service adapters)
    ├── client / NeoForge / optional-library adapters
    └── feature commands and immutable queries
             └── feature rules + foundation (:core, Java only)
```

Implemented now: entry point, isolated testMod and foundation.TickQuota.
The contracts below define implementation ownership; they are not claims that
these modules already exist. Create each module with its first task/consumer.

Each feature owns its rules, state schema and transitions. Pure rules live in
`core/.../<owner>`; platform code in `src/main/.../<owner>`. Public domain
interfaces live in `<owner>.api`; implementation packages stay private.
A feature needing another service declares a narrow port in its own API.
Composition adapters translate between those APIs. Domain modules never import
other feature implementations or Minecraft types.

No shared global block/item/menu buckets. Each feature registers its own content.
The entry point wires registrations, not gameplay. A platform bridge belongs in
`platform/<consumer>`; it contains translation only. Clients observe snapshots and
send validated intents; server transitions own all costs and decisions.

## Owners and interfaces

Names below are project contracts, not claims about NeoForge API signatures.
Commands return immutable success/refusal outcomes. A refusal names the unmet
condition and leaves uncommitted resources unchanged.

| Owner | Owns | Public command/query surface |
|---|---|---|
| foundation | Shared work admission, checked quantities, bounded identifiers | TickQuota admission; no world scans or feature state |
| organ | Common organ identity, completed-batch counts and earned choices | completeBatch(batchId), choose(property), snapshot; no recipe execution |
| processing | Bowl, digestion, rack/loom, furnace, activation and generic reserved recipe execution | BatchWork.start(request, revision), advance(work), cancel, snapshot |
| storage | Item/fluid/BE holdings, reservoir cell accounting and partitioning | QuantityStore.preview, reserve, commit, release, snapshot |
| construction | Loaded-only structure validation, shell parts, paid finite placement and transplantation placement | Structure.inspect(origin, ruleId, budget); Placement.step(plan, budget) |
| ecology | Substrate maturity/function/lining/reinforcement/dye and deliberate conversion | Cell.mutate(treatment), reinforce, appearance; no farm harvest policy |
| cultivation | Assigned crop/tree growth, allocated harvest, planting reserves | Plot.assign, advance, harvest, snapshot |
| logistics | Typed local route topology, ports, filters, reservations and movement | RouteService.configure, transfer, page; no recipe conversion |
| control | Stable signals, interlocks, selection and finite schedules | Signals.publish/read; Schedule.step; commands reference actual batch IDs |
| genetics | Species/quality counts, sample collection, coverage, native eligibility, cultured stock | Samples.offer/withdraw; Research.extract/merge; Genetics.eligible |
| equipment | Piece identity, frame tree, counters, mutations, capabilities, fuel and death preservation | Equipment.applyActivity/fuse/install/spend; capabilities; resolveDeath |
| portable | Carry Sac slots, pouch socket, nesting refusal | CarryStore.insert/extract/page; cannot add armor abilities |
| mineral | Workpiece ownership/stage, treatment, fracture and dust/tailings recovery | Bed.reserve/present/treat/fracture; MineralRecipe.resolve |
| excavation | Finite survey/cut plan, safe section stepping, access/light requirements | Excavation.plan/step/cancel/status; requests cutters, never smelts |
| thermal | Heat/pressure/steam, cooling and mechanical-work service | ThermalProcess.start/advance/relieve; HeatPort.exchange |
| energy | Biological electricity generation and explicit external-energy exchange | Generator.step; Exchange.preview/commit; storage owns balances |
| containment | Finite spill volume, damage delay, isolation/recovery/neutralization | Spill.admit/step/recover; refuses at shared admission limits |
| spatial | Native End bed/service eligibility, anchoring, conditioning and phase isolation | SpatialService.validate/reserve/complete; no passenger/cargo ownership |
| transit | Explicit endpoint links, safe player travel, durable freight escrow | Transit.link/travel; Freight.reserve/commit/ack/recover |
| indexing | Registered-store summaries and bounded material requests | Index.page; Request.reserve/dispatch/status; no authoritative item copies |
| husbandry | Assigned animal/water work, feed/container/berth reservations | Husbandry.assign/start/complete; no living-creature DNA extraction |
| nutrition | Food and potion recipes/effect allowlists | Nutrition.recipe; Potion.prepare; executes through processing ports |
| helpers | Nursery population, berths, jobs, cargo, routes and paid Sail tickets | Nursery.hatch; Worker.assign/cancel; Roost.enable/status |
| defense | Hostile targeting, firing/repulsion/restraint and projectile limits | Defense.select/fire; membrane collision policy queries equipment port |
| synthesis | Installed socket/lobe services and idle organ profile switching | Anatomy.attach/isolate; Synthesis.reserve; Profile.switch |
| ui | Layout, widgets, focus, theme, tooltips and menu snapshot presentation | ScreenModel(snapshot, intents); no inventory/cost/mutation decisions |
| interaction | Probe targeting, reach/ownership validation and dispatch to feature intents | Probe.select/configure/open; feature ports validate their own state transitions |
| discovery | Recipe reveal, advancements, guide teaching prerequisites | Discovery.observe/query; adapters render the same content definitions |
| integration | JEI, Modonomicon, Curios, GeckoLib and NeoForge-specific boundary adapters | Register adapter against owner API; no duplicated domain state |
| campaign | Development-only action fixtures, cross-feature conservation and performance tests | Run scenario with assertions; never shipped production gameplay |

## Shared invariants

- Processing reserves inputs, outputs, returned containers, waste and target before
  starting. Each organ owns one active batch. Persistent stage, not elapsed wall
  time, determines recovery. Completed conversion retains an actual intermediate.
- OrganHistory is the only owner of counts/choices. A multiblock core carries its
  identity once; wall blocks and controllers never copy component histories.
- QuantityStore is the only owner of balances. Routes and indexes hold reservations
  or observations, not independent resource totals. Integer arithmetic is checked.
- Equipment owns every suit predicate and expenditure. Travel tissue, UI, Curios
  and defenses query it instead of reconstructing the same full-set test.
- Genetics owns native-class predicates. Being in another dimension, owning a
  genome and having a native processing bed are distinct conditions.
- Freight has durable, bounded escrow with one owner at each transition.
  Local reservations are not falsely described as a cross-world transaction.
- Rendering is client-only. Logical connections determine geometry/pulse direction;
  geometry never decides whether a resource transferred.
- Optional adapters load only when their library is present. Core retains an empty
  external dependency classpath. testMod/campaign code never enters release JARs.

## Discovery adapters

Discovery owns one persisted tri-state claim Boolean per player. Delivery examines
at most the 36 main inventory slots and inserts only into a real free slot, including
Creative; pending delivery retries on player ticks without drops or a delivery queue.
Vanilla inventory-change advancement criteria recognize Culture acquisition and
retain one instruction-reveal bit per implemented item. Reveal conditions are
separate from successful-operation milestones; Creative possession cannot complete
an operation. Vanilla advancement data owns earned player progress and its sync.

The Bowl retains two pending milestone bits beside its existing owner UUID. They
survive serialization and the single recovery item. A completed loaded Bowl resolves
the current online player once per 20 ticks, or on that owner's interaction; it
never retains a player object, scans chunks or loads an offline Bowl. Bud completion
is recorded only after its batch commits. Repeated batches coalesce the same bits.
The Modonomicon loader renders each Bowl recipe from `CultureBowlRecipes` into a
standard synchronized text page with localized components. JEI projects that same
finite catalog; optional JEI classes are absent from common initialization.

## Culture Bowl adapter

Processing owns the Bowl block entity, finite registry translation and strict schema-1
NBT codec. Hand interactions submit commands to `BatchWork`; the block owns no
parallel balances or counters. Storage alone validates reservations and commits a
fully validated candidate before replacing balances. Restored active work must match
one recipe alternative, earned water/time choices, outputs and returned containers.
Allocation lists are bounded before copying (9 item entries per category, 2 fluid
entries), and quantities use checked arithmetic. Organ owns the single history.
Start and restore reserve revision capacity for each remaining work unit and a
separate completion after quota refusal (plus start's own transition). Repeated
admission refusals without progress leave the revision unchanged.

The server adapter uses nine item slots, one 2000 mB water tank and one 2000 BU
biomass tank, with only catalog resources and component-free input stacks. NBT list bounds, types, IDs, shape and
stack limits are checked before domain restoration. Rejected saves disable edits
and retain their original data on save/recovery. Breaking produces one block item
carrying that state; placement consumes it. There is no second contents drop.

Only active loaded block entities advance one work unit/tick; idle tick callbacks
return without snapshots or block probes. Pending discovery credit uses the bounded
online-player lookup described above. Active work probes only the loaded supporting
cell, never requests a ticket, and never reads wall time. Every dimension shares
one server-identity `SharedRecipeCompletionAdmission` (16 completions/tick); excess
completed work retains its reservation until admitted. There is no pending queue.
One transition scans at most nine slots, two tanks and one reservation. Marking the
owning chunk dirty avoids comparator neighbor callbacks. Only start/finish/cancel
change the client block state. The Probe menu is the sole custom-packet path: it
sends bounded intents and immutable changed snapshots, never automatic exports,
particles, renderers or ambient scans. Bowl geometry has at most 13 cuboids; the
four item silhouettes reuse the production substrate texture with finite tints.

Core tests cover recipe conservation, refusal equality, hostile restore, choices,
transfer locking and shared admission. GameTests cover actual recipes/registration,
hand loading/collection, tick completion, save/reload, recovery, corrupt saves and
cross-dimension contention. The connected client fixture observes the Bowl models
and all four collected material icons in a disposable server scene.

## Membrane and skeletal preparation adapters

Rack and Loom compose `BatchWork` with the processing-owned `PreparationRecipes`
catalog. Their strict schema-1 codec bounds state to four item slots, one 1,000-unit
tank, one reservation and three history choices. Active recovery must match a
catalog route. Rejected payloads remain frozen through save and recovered placement.
One stateful drop carries both the contents and history; placement consumes it.
Idle collection returns one stack, prioritizing output; active reservations lock
all hand transfers. Only loaded, rooted active organs advance through the existing
16-completion server quota. There is no additional scheduler or offline work.
Loom deposits invoke `BiomassBucketTransfer`, which owns the portable admission,
hand exchange and discovery credit also used by Sac and Bladder. The destination
callback performs one atomic BatchWork insertion without exposing its mutable store.

Gameplay tests exercise all three routes across active reload, recovered placement,
exact completion and completed recovery, plus malformed payload preservation,
clog recovery and shared player/server transfer contention. Client evidence includes
nearby block models, inventory icons, each guide route and the actual optional JEI
categories. JEI checks inspect rendered duration/biomass labels as well as ingredients.

## Starter biomass adapter

Storage extends `QuantityStore` reservations to finite fluid outputs; digestion and
portable transfers use the same preview/reserve/commit invariant as item recipes.
The Digestive Sac holds one item slot, one 1,000 mB biomass tank and one active
batch. Its finite starter recipes are wheat (100 mB, 40 loaded ticks) and rotten
flesh (50 mB, 160 loaded ticks). The complete output is reserved before the hand-fed
item is accepted. Loaded rooted Sac ticks alone advance work; uprooting pauses it,
and normal recovery retains the input, reservation, progress, output and history.

The starter Biomass Bladder owns one 4,000 mB biomass tank and exposes an
`EquipmentFueling` port. Equipment remains the compatibility and destination owner;
an incompatible target refuses before source debit. I019 is an unstackable full
1,000 mB transaction item, not a placeable fluid container. Filling and emptying
reserve the complete source or destination change, so cancellation and refusal
leave exact mB unchanged. No ampoule or armor registration is introduced here.

One server-identity admission table bounds bucket/equipment transactions to 64 per
tick across dimensions and four per player in any 20 ticks. It retains at most
1,024 player windows, prunes at most 64 expired windows per request, creates no
queue and weakly retains no stopped server. Idle Bladders have no ticker; idle Sacs
return before block probes. Neither organ scans neighbors, requests chunk tickets,
spawns rejected output nor performs offline catch-up.

Sac admission reserves revision capacity for its start, every remaining progress
transition, completion and a restorable final state. Quota refusals without progress
do not advance the revision. Sac restoration rejects a next-batch identifier at or behind completed history;
completion checks history eligibility before committing reserved quantities.
Unsupported main/offhand interactions pass through on both logical sides; only
catalog feeds and recognized buckets are consumed by the direct hand controls.

Discovery uses vanilla inventory criteria to reveal all three biomass guide entries
independently of Bud completion. Successful bucket commits and Bladder withdrawals
report milestones through `DiscoveryObserver`; refusal and possession earn none.
The Sac persists one operator UUID and one pending completion bit beside its batch.
Completion credits only the feeder, resolving the current online player once on
completion and once per 20 loaded ticks while pending, or on direct interaction.
Recovery preserves that evidence; a pending credit prevents new batch admission
until delivered, without blocking tank transfers or retaining player references.

Biomass uses static baked models: at most 11 cuboids per organ, nine for the
bucket. The Bladder has one shared translucent membrane/frame model and four
solid contents models selected by its finite fill state. Sac start/finish swaps
its throat/band geometry; no renderer, particle loop or animation ticker is added.
Original production organ and substrate art supplies the fleshy frame and tinted
biomass; vanilla stained-glass and iron textures supply membrane and bucket metal.
Connected-client fixtures require captures of all five fill states, active/idle
Sacs and the distinct bucket, and exercise actual main/offhand building dispatch.

## Probe and organ-menu adapter

Interaction owns the reusable Synaptic Probe item, server-observed target selection,
reach and owner checks. A first Probe use claims an unowned legacy/test Bowl; its owner
persists with the single recovered core. Other players may inspect an open snapshot but
their intents refuse unchanged. The Probe never requests a chunk ticket and one use
examines only the selected loaded block entity. Unsupported held-item interactions pass
through to normal item placement, so building beside an organ is not intercepted.

Processing owns the Culture Bowl menu target and every recipe transition. The client
screen composes the internal UI owner: dependency-free `:ui` supplies layout, palette
and snapshot cadence; `ui/client/OrganScreen` supplies Minecraft widgets, panel/bar
drawing, keyboard focus/narration and themed tooltips. Processing binds values and
intents only. Pure tests check geometry/cadence and the real client fixture checks
widget bounds, focus, narrated labels and rendered tooltips at all three GUI scales.
The screen observes an immutable snapshot and sends only start, cancel or slot-
collection intents; it does not edit balances, recipes or progress. A snapshot contains
a command revision, exactly nine bounded slots, water amount/capacity, work state and
progress, completed count, selected recipe and latest refusal code. Its codec is bounded
below 4 KiB before allocation.

Progress changes do not invalidate a command revision. Commands and batch completion
advance it to the storage-backed work revision; restoring a core seeds it from that
saved revision. Existing menus invalidate when their loaded entity disappears, so
no separate persisted menu state or save-schema change is required.

Vanilla permits one open menu per player. Each player admits at most four intents in any
20-tick window across menus, targets, dimensions and reconnects. One server-identity
quota admits at most 64 intents/tick across dimensions; refused excess is not queued. A server-local table holds at most 1024
player windows (four timestamps each). Each request prunes at most 64 expired
windows in last-admission order; a full table refuses new entries. Entries expire
after 20 ticks without admission and the server table is weakly owned. Every intent
revalidates menu ID, exact target,
loaded state, dimension, reach, owner, revision and slot/recipe shape on the server.
Open menus compare snapshots each player tick but send only changes, at most once per
10 ticks per viewer. Idle menus send nothing. Reopening always receives one current
snapshot in the bounded menu-open payload.

## Freight persistence protocol

The durability boundary is Cargo Lock to Cargo Lock, not arbitrary external
inventories. External deposit/withdrawal follows the source mod's save semantics;
do not promise crash-atomic transfers into a foreign inventory.

Storage owns one world-level freight ledger: admitted Cargo Lock balances,
reserved receiver capacity, payloads and monotonically increasing transfer IDs.
Chunk data stores lock identity, not another authoritative cargo copy. Transit
submits commands through storage's freight port and never writes ledger files.

| Transition | Durable result | Retry/restart behavior |
|---|---|---|
| request | Reserve existing source cargo and receiver capacity; cargo remains source-owned and unavailable to other requests | Repeated request ID returns the same reservation; cancel releases both holds |
| pack | Atomically debit source and move the same payload into escrow | Before durable commit: request remains; after commit: only escrow owns the cargo |
| receive | Atomically move escrow into the reserved destination balance and mark the transfer received | Repeated transfer ID returns its existing result without adding cargo again |
| acknowledge | Advance the link's contiguous acknowledged sequence and retire received records | IDs at or below the watermark cannot be replayed; out-of-order acknowledgments stay within the admitted transfer slots |

After packing, cancellation cannot refund the source. It must finish delivery or
perform a separately reserved return transfer. Unloaded endpoints pause; they do
not load chunks or run offline production. Removing a lock with reservations or
cargo refuses normal dismantling; unexpected removal retains the same ledger
identity for explicit recovery, never spills a second copy. Stale/cloned block
identities cannot claim an occupied lock. Rebinding requires owner authorization
and the original location to be loaded and absent.

The ledger admits at most 512 locks, 256 links and 256 in-flight transfers, with
at most 8 transfers/link. One lock has 9 item slots; separate installed fluid/BE
compartments hold at most 16000 mB/100000 BE. Item data is limited to 16 KiB/stack.
Refuse excess before reserving or spending anything. Retired lock/link identities
use a monotonic allocation counter; do not retain an ever-growing tombstone map.

Persistence uses a versioned, checksummed snapshot plus bounded write-ahead log.
One writer serializes commits; at most 64 pending records, 1 MiB/record and a
64 MiB journal. Compact into a new snapshot before admitting beyond that bound;
atomically replace only after flushing it, keeping the previous valid snapshot.
Disk I/O runs off the server thread. Commands remain pending until the durable
completion is applied on the server thread; at most 16 completions/tick. I/O
failure stops freight admission with cargo retained. No timeout invents success.
Test process termination at every commit boundary, truncated final records,
duplicate acknowledgments, slow-disk back-pressure and snapshot replacement.

## Substrate adapter

Ecology owns immutable `LivingCell` transitions and the Minecraft block, conversion,
appearance and ownership adapters. Construction injects culture use through its
`CultureUse` port; it does not implement substrate rules. Native growth receives
an environment resolved by the server adapter. The current playable registration
is ordinary substrate; later native content must supply its actual environment.

One culture use validates the selected face against a ray from the server-observed
player eye position. Reach is capped at eight blocks and the player's interaction
range plus vanilla's one-block tolerance. The ray and shape callbacks share a
loaded-only view capped at 64 block-state probes (including the face neighbor);
missing chunks, invalid hit coordinates, occlusion or unavailable shape data
refuse before payment. No chunk tickets or block-entity lookups are permitted.
One server-wide quota admits 16 conversions/tick.
Replacement sends client state without recursive neighbor notifications. Static
cells have no ticker or block entity; finite stage, pigment, function and framework
block states plus seeded weighted models carry appearance. Lumen Secretion changes
only a mature cell's function and emits block light; Skeletal Graft changes only its
framework. Neither treatment creates a machine upgrade or block entity. Vanilla soil tag membership supports vegetation;
already-living cells explicitly refuse culture conversion.

Ownership uses a schema-1 per-dimension `SavedData` map capped at 65,536 cells.
Validate the schema, list bound and every position/UUID before accepting the map.
An absent storage entry creates fresh ownership only when the dimension's file
is confirmed absent. Disk/decompression/NBT failures swallowed by Minecraft's
loader, inaccessible files, unsupported schemas and malformed records all cache
a non-dirty rejected state that refuses claims and mutations, logs the reason once
and never rewrites the original file.
Do not throw from the deserializer: Minecraft catches exceptions and may create
empty replacement data. Tests exercise the real storage loader and serialized
round trips, truncated-file byte preservation, occluded/forged selection and
unloaded ray paths, alongside vegetation updates and shared-quota GameTests.

## Bud construction adapter

Construction owns the startup-only `BudRecipeRegistrar` and the deterministic
`BudConstructionPort`. Production attaches only implemented recipes. One use
validates one visible bud and all remaining inventory costs before replacement
and payment; every implemented organ mutation also requires loaded Living Substrate
support, and duplicate uses encounter the replaced target. The shared server-tick
quota admits at most 16 replacements across dimensions. Idle buds have no ticker
or block entity. Registry data is finite startup data, not player-supplied input.

The platform adapter probes the target and a one-block horizontal halo (at most
four loaded-chunk checks). An unavailable chunk refuses before mutation/payment.
Replacement sends the client update with known shape and recursion depth zero;
it does not initiate vanilla neighbor notifications or shape cascades. Registered
output owners must keep placement/state-change callbacks bounded and loaded-only;
any necessary organ connection update belongs to that owner's bounded service.
Adapter GameTests cover the loaded/unloaded boundary, unchanged refusal, exact
payment and duplicate use without installing production placeholder recipes.

Construction shell blocks are ordinary static blocks. Living Skin full blocks,
slabs, cornering stairs and multi-face coverings, Rib Frames and Membrane Windows
have no block entity or ticker; window connection properties own
only visible joins and the thin pane model supplies both faces. The Seed Pouch is
the single stateful shell part: it admits at most four registry item types and one
stack per type, retains one item for planting during ordinary withdrawal and has
no ticker. Explicit player recovery may take that reserve. Removal emits at most
four bounded stacks, once, from the pouch's sole authoritative store.

`Structure.inspect(origin, ruleId, budget)` evaluates a registered ordered list of
relative cells, never neighboring connectivity. Rules contain 1..4,096 unique
positions and at most one expected core. A request continuation stores only its
origin, rule, cursor and counters; it retains no world reference or cell cache.
One call reads at most 64 loaded cells and all requests share a 256-cell server-tick
`TickQuota`. The loaded predicate runs before every state read. Missing chunks,
call exhaustion and shared exhaustion return `Deferred`; mismatches return
`Invalid`, and a complete matching plan returns `Valid`. Passive parts cannot carry
a core identity, so sharing or removing a wall cannot create a second core.

## Integration bootstrap harness

Integration owns library-profile selection, runtime identity checks, isolated
process lifecycle and bootstrap evidence. IF-001 implements and tests this harness;
IF-127 supplies the four-profile qualification manifest and runs it after acceptance.
There are no recipe, item, armor-rank or gameplay registrations in either packet.

`gradle/integration/bootstrap.gradle` wires the Gradle tasks;
`gradle/integration/harness.py` owns launch/readiness/connection/capture/shutdown
and result validation; `gradle/integration/test_harness.py` owns fast fixtures.
Fixture child programs/data live under `gradle/integration/fixtures/**`.
`:core:verifyBoundary` checks the domain module's empty production classpath.
Development Java hooks live only in
`src/testMod/java/org/jd/infestusfrontier/testmod/integration/**`; shared fixture
resources live in `src/testMod/resources/integration/**`. Client automation is
packaged separately for disposable clients; neither it nor the existing testMod
is installed on the packaged server or included in the release JAR. Any runtime
identity diagnostic adapter lives in the production integration package, exposes
only loaded IDs/versions and owns no gameplay state. Build-file changes only wire
these tasks/source sets, pinned dependencies, verification metadata and fixtures;
existing acceptance machinery and checks remain intact.

The task inputs are `modProfile=required|jei|curios|combined` (default required),
`scenario=bootstrap`, and an output directory (`captureDir` for captureClient;
otherwise `build/integration/evidence/<profile>/<task>`). Invalid inputs fail before
launch. `integrationProfileFile`, when supplied, is a JSON object with `schema: 1`,
`scenario: "bootstrap"`, `profiles: ["required","jei","curios","combined"]`,
`seed: 11`, and `deadlinesSeconds` containing `readiness: 120`, `join: 30`,
`capture: 30`, `disconnect: 30`, `shutdown: 30`, `cleanup: 5`, `profileSmoke: 180`,
`clientScenario: 420`. Validate these fields and CLI agreement before launch.
Absence of this file uses the same built-in bootstrap defaults, allowing IF-001
to test the harness before the IF-127 qualification file exists.

Every command writes a fresh `result.json` with schema version, run ID, profile,
scenario, Java version, candidate identity supplied by the evidence runner (or
Git tree plus dirty-diff digest for local diagnosis), release JAR SHA-256 when
used, runtime mod lists by process, assertion outcomes, child PIDs/exit codes,
phase durations, artifact paths/digests and final success/failure. Logs are kept
as `server.log` and/or `client.log`. Success requires the current run's evidence
and reaped processes; stale files, empty/truncated evidence, timeout, missing
assertion or nonzero ordinary child exit produce failure and a nonzero task exit.
Expected negative loader exits are handled only by the explicit omission fixture.

Runtime mod lists come from the running loader, never inferred from Maven
resolution or copied from the selected profile. Assert `minecraft` 1.21.1,
`neoforge` 21.1.249, `infestusfrontier` at the built `mod_version`, `modonomicon`
1.120.4 and `geckolib` 4.9.2. Assert `jei` 19.56.0.438 exactly when selected and
`curios` 9.5.1+1.21.1 exactly when selected; require their absence otherwise.
Check client and server lists separately. Required transitive mod identities are
recorded and matched to the pinned resolved artifacts/verification metadata;
they are not forbidden by the name "required". Development fixture IDs are
explicitly identified on development runs/automated clients and absent from the
packaged server. Do not silently change pins or treat a missing optional selected
mod as success.

| Command | Observable result |
|---|---|
| `./gradlew :core:verifyBoundary :core:compileJava` | Reject external dependencies in the domain module and compile it without Minecraft or optional-library classes. |
| `./gradlew integrationHarnessTest` | Run the fast supervisor/evaluator fixtures without launching Minecraft. |
| `./gradlew profileSmoke -PmodProfile=<p>` | Start an isolated development GameTest server in that profile, collect actual loader identities, execute at least one real world/server-state assertion (including the existing required GameTest), see the required-test success summary, then stop cleanly. Dependency resolution or a socket opening alone is insufficient. |
| `./gradlew captureClient -PmodProfile=<p> -Pscenario=bootstrap -PcaptureDir=<dir>` | Launch a packaged server and disposable client; after resources load capture `bootstrap-title.png`, join the server and capture `bootstrap-world.png` with the HUD/world visible. Both are fresh readable 1280x720 PNGs; report their digests and loaded client/server identities in result.json. Missing-texture/resource-load errors fail; review inspects actual images. |
| `./gradlew packagedServerSmoke -PmodProfile=<p>` | Install the release JAR and pinned profile libraries in a disposable dedicated server; await readiness, join using a real Minecraft client, disconnect, then stop both cleanly. Assert server-side player UUID/name join and subsequent removal plus client play-state entry/exit. Server-list ping, TCP connect or simulated player objects cannot replace this evidence. |
| `./gradlew profileSmoke -PmodProfile=required -PomitRequired=<id>` | Only `modonomicon` or `geckolib` is allowed. Remove that actual runtime artifact, retain the release mod's required-dependency declaration, require loader refusal before world readiness and a diagnostic naming both the missing ID and dependent mod. Wrapper exits zero only for that expected refusal and cleanup; crash/timeout/generic failure is insufficient. |

Bootstrap uses seed 11, a default superflat disposable Overworld and a camera at
yaw 0, pitch 15 for the world image. Owner `visual-setup.json` fixtures may prepare
the disposable player's inventory and scene through server console commands after
join. Each names an active GameTest assertion and at most 16 single-line commands
of 256 characters; all active fixtures together admit at most 64 commands. The
client's development-only `ContentVisualScenario` providers wait for the resulting
world/inventory packets before rendering/capture, under the existing capture
deadline. Owners may additionally declare up to 24 unique detail capture names
across all active fixtures. Client contributors supply matching bounded camera
angles; the shared capture adapter renders and saves these after the unchanged
overview, within the existing disconnect deadline. Each required image must be a
fresh 1280x720 PNG under the same 4 MiB/digest checks; missing detail captures fail
acceptance. The setup never modifies an ordinary world or fabricates client state.
Bind the server to loopback on an allocated port, allow only the fixture identities, and disable online-account
authentication solely in this owned disposable server. The client runs with an
isolated game directory and deterministic fixture identity; no personal account,
ordinary save or production-server access is required. Wait for rendered frames
and observed player state, not arbitrary sleeps. A display helper is allowed when
needed and shares the scenario's ownership and cleanup.

When Probe coverage is active, captureClient also starts the allowlisted
FixtureObserver. A bounded file carries phase names only; each client independently
opens the real menu and records received snapshots. Actual screen clicks start and
cancel work, the owner closes/reopens, and the observer checks ownership refusal.
The evaluator compares revision, recipe work requirement, slots, water, state and
progress within the 10-tick snapshot cadence. Missing/mismatched observations,
identity, mod lists, clean disconnect or process cleanup fail acceptance.
When discovery is active, a fresh Survival phase before scene setup supplies ordinary starter
materials from its owner-local `survival-setup.json` (the same schema and combined
64-command ceiling as visual setup) and uses actual inventory clicks and block-use packets to craft Culture,
convert ground, craft/place a Bowl, renew Culture and recraft a lost book. It observes
synchronized prerequisites and advancements, and exercises a rebound guide key.
This phase admits 100 seconds (including the 60-second base batch) within the same
420-second scenario ceiling. Every contributed guide spread is then inspected;
additional captures show all Bowl recipes and the optional JEI recipe browser. Each
additional action phase is bounded by 30 seconds within the existing 420-second
scenario ceiling. Shared admission and snapshot cadence/idle silence have focused
rule tests, while the game fixtures cover reopening and offhand placement.

Run scenarios serially: at most one dedicated server and one world. The Probe
acceptance fixture uses exactly two disposable clients sharing one owned display helper to
observe the same menu via actual packets; other scenarios use one client/display.
At most four owned helper descendants and four root processes (including the
already-reaped installer) are admitted, with no nested Gradle invocations. ProfileSmoke needs only its server. Readiness is bounded
by 120 seconds per process, join/capture/disconnect by 30 seconds per phase,
graceful shutdown by 30 seconds for all children together, then forced termination
and reaping by 5 seconds. Every failure or interrupted launch cleans the entire
owned process group, including grandchildren, and releases its port. Failure to
clean up remains a failure. Entire profileSmoke is bounded by 180 seconds and each
client scenario by 420 seconds, including cleanup; setup/downloads are separately
bounded by 600 seconds per qualification matrix. No automatic retry. Each stream
log is capped at 8 MiB, result JSON at 1 MiB, captures at 4 MiB each; overflow fails
with cleanup rather than silently discarding evidence. Poll at most 10 times/second.
Only owned run directories under `build/integration/runs` may be created/removed;
keep result evidence outside them. At most one such world is retained at a time.

Fast fixtures exercise the same supervisor and evaluator through injected clock,
process and evidence interfaces. Require positive completion and individual
readiness-timeout, failed-join (including ping without player login), absent/stale
capture, wrong-runtime-version and shutdown-timeout assertions. Each failure names
its phase, exits unsuccessfully, emits no success receipt, and proves no live
owned child/grandchild or listening port remains. Include real fixture subprocesses
for graceful stop and forced cleanup; fake-clock tests alone do not prove reaping.
Use 1-second fixture readiness/shutdown limits and a 2-second cleanup limit;
the entire fixture suite has a 60-second deadline. No Minecraft/downloads in it.
`-PintegrationFixture=wrong-mod-version` runs a deliberately invalid synthetic
result through the production evaluator and must fail nonzero at the mod ID;
the ordinary fast suite asserts this rejection as a passing regression.

Core isolation is enforced by its empty production classpath and Java compilation.
Server/client separation is checked by reviewing actual registration and dependency
paths and exercising the implemented features on a dedicated server. These runtime
tests cover exercised paths; they do not prove every hypothetical class-load path.
No custom Java source analyzer or static transitive-reachability checker is required.
Client-only registrations remain valid; common initialization must not load them.

## Incremental content coverage harness

Integration owns the build-time catalog coverage gate. The tracked
`src/testMod/resources/content-checkpoint.json` is schema 1 with one `through`
task ID. The default gate reads that file and owner contributors directly from the
current working tree. `--through`/`-PcontentThrough` may require a later queue task,
but cannot lower the tracked checkpoint. Git state, ignored receipts, runtime
registry discovery and production placeholders are never coverage inputs.

An owner contributes only at
`src/testMod/resources/<owner>/coverage.json`. Its schema-1 `owner` must match the
directory and each contribution contains one queue `task` plus `entries`. An entry
has exactly these fields:

```json
{
  "catalog": ["I001", "T0-16"],
  "registry": ["infestusfrontier:construction/organ_bud"],
  "producer": "infestusfrontier:construction/organ_bud",
  "guide": "guide/organ_bud.json",
  "assertions": {
    "obtain": "infestusfrontier_tests:construction.organ_bud.obtain",
    "use": "infestusfrontier_tests:construction.organ_bud.use",
    "guide": "infestusfrontier_client:construction.organ_bud.guide"
  }
}
```

`catalog` groups aliases that share the listed real representation; it does not
authorize another registration. Multiple catalog IDs must exactly match a declared
`KNOWN_ALIASES` group in the checker, independent of order; unrelated items, blocks
and ranks require separate representations and named assertions. Families may list
their finite actual registry IDs. `producer` names the real recipe, mutation or construction operation. Obtain
and use assertions are GameTests and call `ContentAssertion.passGameTest` only
after their behavior succeeds. Owner client contributors live under
`src/testMod/java/org/jd/infestusfrontier/testmod/<owner>/client/`, implement
`ContentGuideScenario`, and are explicitly listed in the testMod resource
`META-INF/services/org.jd.infestusfrontier.testmod.integration.client.ContentGuideScenario`.
The client runner calls one scenario per tick after the world capture; returning
true certifies the observed page/recipe behavior and emits `ContentAssertion.passGuide`.
A scenario may retain bounded state across ticks to open and inspect UI; all
scenarios together must finish within the existing 30-second disconnect phase.
Missing or duplicate providers fail by assertion name. Staged providers are not
executed. Client contributors and guide records are included in the disposable
client artifact. Client classes are excluded from the dedicated GameTest source
set and release JAR. Their exact markers are required by the integration harness,
so a registration without the behavior test is insufficient. The owner-local `guide/<name>.json` is required even before
IF-004. It contains exactly `schema: 1`, a namespaced `entry`, nonblank `title`,
`obtain` and `use` teaching text (at most 4096 characters each), and `assertion`
matching the coverage guide assertion. Entry IDs are unique across representations;
aliases share the same record. Missing, malformed or mismatched staged data fails
by catalog ID. The `guide` field is a relative path within the owner's directory,
not an ignored receipt. Guide assertions and records are schema-checked before
IF-004; at and after IF-004 the emitted client requirement includes every eligible
staged and new assertion.

Item and mutation assignment comes from `.ktask/content-plan.json`; block assignment
comes from the queue's `Blocks` fields. Contributors for unknown or later tasks,
misowned or duplicate catalog IDs, missing producers/assertions and incomplete
accepted tasks fail by exact identifier. The checker admits at most 4096 distinct
identifiers, 16384 mapping/dependency edges and 1 MiB per JSON input. It performs
no game/world scan. The isolated IF-108 GameTest separately proves the bootstrap
has no production item or block registrations. The generated requirements carry
`emptyRegistry`, derived from accepted representations, to both test runtimes;
contributed content replaces that empty-state expectation with named assertions.
A separate always-required harness GameTest and multi-tick client UI scenario
prove the marker and contributor execution paths without registering gameplay.
The checker output is a generated input to both test resource sets, so advancing
an uncommitted checkpoint also changes the packaged runtime requirements.

`verifyAll` adds integrationHarnessTest, required-profile smoke and one serial
`captureClient` run without removing its existing core, distribution or GameTest
checks. Its smoke always uses required, independent of a caller's profile selection;
reuse the existing required GameTest run where possible. The default client run executes all
eligible guide assertions, including staged predecessors after IF-004. Full profile
captures and packaged server runs are recorded task evidence, not four extra copies of the full gate.
IF-127 runs four times (180+420+420) seconds at most: 4080 seconds runtime, plus
600 seconds preparation inside a 4800-second matrix deadline. Reserve 1200 seconds
for the existing gate within the unchanged 7200-second worker budget. Qualification
may repair implementation and harness defects in their owning modules. Rerun
affected checks after repairs; final matrix evidence must describe the delivered
implementation. These are execution ceilings, not measured
performance claims.

## Enforcement and growth

Additional Gradle projects are justified by compile-time isolation, not one project
per noun. Review API references and dependency cycles; package naming alone is not
enforcement. Add executable boundary checks only for a concrete rule that the
chosen compiler, classpath or runtime fixture can establish reliably.

Registry IDs use `infestusfrontier:<owner>/<name>`. Owner recipes, models, loot and
advancements use that path consistently. Shared translation/vanilla-tag edits add
only the task's entries. Save schema versions belong to their feature owner;
reject unsupported versions without silently replacing contents.

Use data tables for recipe costs and family/rank values. UI, JEI and guide read
those definitions; do not hard-code a second balance table. If one gameplay change
requires edits to several rule implementations, correct ownership before adding
the next feature. Public APIs grow only for a named production caller and contract
test. No empty frameworks or new global manager to bypass a dependency.
