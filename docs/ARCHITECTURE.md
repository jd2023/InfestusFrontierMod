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

## Enforcement and growth

The first production task adds import/package checks, including a deliberately
invalid fixture proving rejection. Additional Gradle projects are justified by
compile-time isolation, not one project per noun. Check API references and cycles
in the full gate; package naming alone is not enforcement.

Registry IDs use `infestusfrontier:<owner>/<name>`. Owner recipes, models, loot and
advancements use that path consistently. Shared translation/vanilla-tag edits add
only the task's entries. Save schema versions belong to their feature owner;
reject unsupported versions without silently replacing contents.

Use data tables for recipe costs and family/rank values. UI, JEI and guide read
those definitions; do not hard-code a second balance table. If one gameplay change
requires edits to several rule implementations, correct ownership before adding
the next feature. Public APIs grow only for a named production caller and contract
test. No empty frameworks or new global manager to bypass a dependency.
