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

Bootstrap uses seed 11, a default superflat disposable Overworld and the fixture
player's spawn camera at yaw 0, pitch 15 for the world image. Bind the server to loopback
on an allocated port, allow only the fixture player, and disable online-account
authentication solely in this owned disposable server. The client runs with an
isolated game directory and deterministic fixture identity; no personal account,
ordinary save or production-server access is required. Wait for rendered frames
and observed player state, not arbitrary sleeps. A display helper is allowed when
needed and shares the scenario's ownership and cleanup.

Run scenarios serially: at most one dedicated server, one client, one world and
one display helper at a time, with at most four owned helper descendants and no
nested Gradle invocations. ProfileSmoke needs only its server. Readiness is bounded
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
  "assertions": {
    "obtain": "infestusfrontier_tests:construction.organ_bud.obtain",
    "use": "infestusfrontier_tests:construction.organ_bud.use",
    "guide": "infestusfrontier_client:construction.organ_bud.guide"
  }
}
```

`catalog` groups aliases that share the listed real representation; it does not
authorize another registration. Families may list their finite actual registry
IDs. `producer` names the real recipe, mutation or construction operation. Obtain
and use assertions are GameTests and call `ContentAssertion.passGameTest` only
after their behavior succeeds. Guide assertions call `ContentAssertion.passGuide`
after the client has observed the named page/recipe behavior. Their exact markers
are required by the integration harness, so a registration without the behavior
test is insufficient. Guide assertions are schema-checked and staged before
IF-004; at and after IF-004 the emitted client requirement includes every eligible
staged and new assertion.

Item and mutation assignment comes from `.ktask/content-plan.json`; block assignment
comes from the queue's `Blocks` fields. Contributors for unknown or later tasks,
misowned or duplicate catalog IDs, missing producers/assertions and incomplete
accepted tasks fail by exact identifier. The checker admits at most 4096 distinct
identifiers, 16384 mapping/dependency edges and 1 MiB per JSON input. It performs
no game/world scan. The isolated IF-108 GameTest separately proves the bootstrap
has no production item or block registrations; later checkpoints replace that
empty-state expectation with their named contributors.

`verifyAll` adds integrationHarnessTest and required-profile
smoke without removing its existing core, distribution or GameTest checks. Its
smoke always uses required, independent of a caller's profile selection; reuse the
existing required GameTest run where possible. Full profile captures and packaged
server runs are recorded task evidence, not four extra copies of the full gate.
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
