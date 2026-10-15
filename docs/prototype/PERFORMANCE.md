# Performance and server-safety policy

## Burrowing prototype — 2026-09-09

Material cost: temporary terrain replacement/restoration, native lighting updates,
item component updates and server-directed movement. Per dimension, at most **four
wearers** hold short-lived traversal admissions; at most **four steps per tick**
share `WorkBudget.BURROW`. A wearer normally attempts every other player tick, so
at most ten native movement synchronizations/sec/wearer. Each step builds at most
36 cell candidates and checks at most six loaded neighbors per occupied cell.
That is a conservative 144 conversion writes and 864 neighbor probes per tick,
plus candidate reads, revalidation, permission hooks and native lighting/network
work. Ordinary continuous travel usually renews existing cells, not all 36.
Do not mistake these bounds for measured TPS under real latency or other mods.

`PassageLease` persists at most **256 position/deadline pairs per dimension**, and
review rotates through at most 16 entries per tick. Expired unloaded entries retain
their capacity; neither restoration nor movement creates chunk tickets. Two new
boolean equipment components identify grafts; existing armor fuel remains its
single source of truth. Per-player session state is a fixed deadline and flags.
The transient admitted-wearer table is capped at four and expires/removes stale
players; additional wearers are refused before taking active wearers' step quota.

At most **16 restoration writes per dimension tick** share a separate quota,
including scheduled fallback callbacks. Proxies carry the original host/axis in
fixed block states, cannot be displaced by fluid/pistons, and have no block entity,
loot or ticking renderer. A scheduled callback survives with the chunk for orphan
restoration and rechecks after ten ticks while a proxy remains; it shares the
restoration write quota. Native tick queues may temporarily retain stale callbacks
after other edits, not just currently live ledger entries. Their creation is bounded
by admitted conversion writes and initial 200-tick scheduling, not by an unbounded
custom retry queue. Shader/lighting propagation and native neighbor internals still
need profiling. Six translucent quads per proxy reuse original prototype art.

Explicitly hazardous behavior: insufficient fuel, blocked geology, capacity limits
or releasing use can stop motion. Unrenewed rock restores after 200 ticks (plus
bounded restoration scheduling), potentially suffocating the wearer. No open-air
gravity exemption, general noclip, resource extraction or gameplay rescue teleport
is added. Permissions are checked at each affected cell; proxies are shared world
geometry, not player-private collision. Multi-player interaction, disconnect/rejoin,
and arbitrary crash-point consistency need further certification before production.

The ten-second lifetime retains more cells/light/translucent geometry simultaneously
than the initial three-second experiment. The existing 256-cell cap, four-wearer
admission and per-tick quotas are unchanged; capacity may therefore stop a long or
crowded passage earlier. No extra chunks are loaded to compensate. Release uses
the native 40-tick item cooldown and its existing synchronization, not another timer
collection or packet family. Expiry no longer permanently disables the wearer;
reopening hardened rock still pays the same fuel and admission costs. Old saved
lock flags are discarded with one fixed-key lookup; no world/player-history scan
is added. Longer progression tiers must revisit occupancy/lighting costs explicitly.

The opt-in Burrowing Lab preflights/replaces exactly **31×19×25 = 14,725 cells**,
checks six entity chunks and at most 64 entities, and refuses unexpected blocks,
inventories, non-player entities or that entity cap. It resets only its lab player's
equipment and uses native graft/refuel
interactions. The eight-phase capture uses actual held-use input and checks client
block light 0 versus 12, movement, suffocation, restart and no-flight behavior. These bulk
fixture costs never run during ordinary gameplay. General suit metabolism remains
deferred; the older readout-only description below is historical.

## Suit biomass readout — 2026-09-08

Suit fuel adds **one bounded 0–1,000 item data component**, owned by the chest piece; old/new pieces without it read as zero. There is no player attachment, ticker, passive drain, world query, Curios search, custom packet, entity, chunk ticket or growing collection. Explicit main-hand sneak-use of rotten flesh checks the four fixed armor slots and reads the chest. Successful feeding costs one item for 250 biomass, requires room for the whole dose and shares a native ten-tick item cooldown with refused full-reserve attempts. Cooldown suppresses repeat feedback. Vanilla inventory/equipment synchronization carries changed components; idle suits do not emit fuel updates. Vanilla death/drop/keep-inventory rules carry the chest and its stored fuel normally.

The HUD reads at most four equipment slots plus one chest lookup per rendered frame for the **local** player. It composes the shared `HudMeter`/`UiTheme` adapter: three **non-overlapping**, 50%-opaque panel rectangles, two text lines and one bar (five rectangle submissions total). Relative panel geometry is static; placement creates one bounded coordinate pair, with no search or retained position cache. Four cached client preferences govern visibility, corner and two offsets bounded to 0–4096 GUI pixels and clamped on-screen. Disabled HUDs return before querying armor. No settings are written per frame or sent to the server; native NeoForge config infrastructure saves explicit edits. The meter also hides without a complete correctly slotted bio suit, during spectator/death, when another camera is used, with hidden HUD/debug display, or while a screen is open. All four corners are checked across GUI scales 2/3; arbitrary HUD-mod overlap is not certified. Fixed work counts are not measured dense-multiplayer FPS/TPS.

The explicit `runSuitHudShowcase` fixture resets only nine glass-platform cells and one note-block marker after checking two loaded entity chunks, with a 400-tick readiness deadline; unexpected block types abort. It resets disposable player inventory/equipment and changes the lab player's game mode. Eight acknowledged phases exercise native client feeding packets, server admission, equipment synchronization, removal/re-equipping, GUI scale, corner placement, spectator/disabled visibility and the native configuration screen. Its final step clicks native visibility/corner controls, saves and checks the small config file, then restores prior preferences in a finally block. No fixture behavior runs in ordinary play. Fuel expenditure, reservoir docking, damage absorption and progression-dependent capacity are deliberately deferred.

## Ground excavation showcase — 2026-09-08

Expected reset preflight refusals now enter a terminal canceled state, with one native chat message and one log entry per launch attempt. They make no reset writes, inventory clearing or entity removals, clear pending capture/deadline scalars, and do not repeat the region scan on subsequent player ticks. A fresh login can explicitly attempt preparation again. The fixed region/entity limits below are unchanged; no exception-catching framework, retry queue, chunk ticket or world-content deletion is added. Unexpected programming errors still fail visibly rather than being swallowed.

Strata Maw shares the existing **eight MINING admissions per dimension tick**, including horizontal/downward miners and manual treatments. Its 5×24 layer has 120 targets; depth is one of 16/64/160, capped at **19,200 targets**. A pulse cuts at most one target or places one passive climbing-tendon cell at a completed layer. There is no whole-volume discovery or stored target list. Save data adds only clamped depth, layer cursor and limit; mined telemetry is clamped to 19,200. Existing nine-slot cargo, nine-slot pending storage, 4,000 biomass and bounded native loot/export rules remain. No automated loose items, entities, custom packets, topology caches or chunk tickets are added.

Frame admission now checks at most **62 loaded ground positions**, plus one loaded read to recognize a legacy raised frame. Root ground collars use 26 cells because their four-block headroom passes under a longer footprint. Older raised collars retain 24 cells. These bounds supersede the historical universal 24-cell count below. Geometry is derived into a bounded temporary list per attempt; no global collection grows. The shared conservative 408 direct write-attempt ceiling remains sufficient; trusted protection/loot/capability hooks and native lighting work are additional.

The cost of the deep option is materially larger **total work and output**, not increased per-tick admission: at most 115,200 biomass for 19,200 solid blocks, with native scheduled work every two ticks. Full storage, unsafe geology, denied interaction and unloaded boundaries stop progress. The 160 preset may meet bedrock/build limits before completing. The test suite excavates all 1,920 solid cells of the 16-depth preset and separately resumes the final 160-depth layer; it does not claim a complete maximum-depth soak or measured TPS under many loaded miners.

Survey Culture shares the native ten-tick treatment cooldown and mining admission. It converts one explicit exposed Living Substrate block through `MiningSafety` and `MiningEdits`, never sprays loot or searches for new area. Admitting the original substrate in `boreable_terrain` also lets miners harvest that terrain through the same safety/loot rules. Surveyed Substrate has 16 coordinate-derived macro-tile states, full native collision and no BE, random ticking or growth. Its baked model has five side/bottom quads and five non-overlapping top quads, exactly at the ground surface; no coplanar overlay or new raster material is used. The placement callback may make one bounded tile-state correction; ordinary culture conversion already supplies that tile.

The **opt-in** excavation fixture preflights/resets exactly **83×34×36 = 101,592** terrain positions, after checking 18 fixed entity-chunk readiness flags. It enumerates at most 65 entities in its fixed reset box, refuses that cap or unrelated non-player entities, and replaces only its three tagged labels. It resets only its two known cores/chests; unknown inventories abort. These fixed but significant bulk setup costs are outside gameplay budgets and never run in ordinary play. Preparation accelerates only the two known cores through their ordinary budgeted entry point (four calls each/player tick), periodically tops up bounded reservoirs, and stops at completion or a 2,400-tick deadline. There is no production free-fuel mode. Capture uses eight phases in the existing acknowledged protocol. These development conveniences are not a dedicated-server load certification.

## Living excavation iteration

The revised rootstock replaces each previous nine-cut/one-marker step with **twelve headroom cuts and three paid tread replacements**, one target per admitted operation. It still shares MINING's eight operations per dimension tick with every excavator. There are three fixed tips: an eight-step trunk and two side tips, each explicitly capped at sixteen steps (one additional eight-step authorization). Maximum depth is 24; the complete plan has at most **600 work cells**. Each tip persists only clamped step, cursor and an 8/16 limit; targets remain derived, not saved arbitrary coordinates. Legacy partial cursors restart their current step safely; already-finished old geometry is not retroactively rewritten. Treads reuse native stairs (80 states including waterlogging, three baked shapes), emit light 10, have no ticker/BE/particles and do not invoke substrate vegetation growth.

Manual leaching also needs MINING admission and a native ten-tick item cooldown. One accepted application reads nine plane targets and, for each of at most nine allowlisted hosts, six neighbors plus the existing permission boundary. It edits at most nine blocks; a refused write can roll back at most eight prior writes. `MiningEdits` centralizes the same maximum-25-write commit infrastructure used by downward structural changes. The existing conservative 408 direct write-attempt ceiling across eight admitted operations therefore remains sufficient. Native relighting, client rebuilds and trusted claim hooks add cost; this is not a TPS guarantee. No conversion emits item/XP entities. Later manual block harvesting uses normal player drops (one host stack), not an automatic item producer.

Leached rock has six host-identity states sharing one original porous membrane material, six native cube faces before neighbor culling, and native transparent-block lighting. Internal faces between treated cells are culled. Neither passive surface adds an idle scan, random growth, packet, persistent collection, job queue or chunk ticket. Transparency remains a client overdraw cost at high placement density. Stress cases still needed before production: many simultaneously treating players, eight active rootstocks with lighting rebuilds, and long tunnels densely lined with transparent blocks. The tests establish admission, conservation, native movement and bounded section/face behavior, not measured maximum-density server/client latency.

The historical downward baseline below describes the original nine-cut roots; the revised root limits above supersede those counts. The disposable root lab geology now extends nineteen blocks either side and 27 below its surface, so the opt-in fixture can test a second section; ordinary worlds never run that preparation.

## Downward mining experiments

Leaching feedback uses the native item cooldown even on a refused attempt: at most one attempt per player per ten ticks, one cooldown-start packet, its later expiry, and one action-bar result. Calls during cooldown return without extra feedback. Native interaction acknowledgements are Minecraft infrastructure. Successful conversion additionally triggers the bounded changed-block/lighting synchronization; no custom packet type is added. Root headroom admission retains three support reads before any cut, so missing treads stop the next section before exposing a fall.

The three [downward experiments](DOWNWARD_MINING.md) share the existing **MINING quota of eight admitted operations per dimension tick with horizontal miners**. Active cores request work every two ticks; inactive cores schedule only a cheap 40-tick check, and retained output can request budgeted export. A finished digestive batch waits for the Recover command rather than repeatedly validating its enclosure. No new chunk loader, mob, dropped-item output, persisted queue or position cache is introduced.

Each operation checks the 24-cell frame and can export at most one 64-item stack through the existing 54-slot storage adapter. A dig edits at most one real terrain cell, uses the existing bounded native loot/protection boundary, and reserves fixed inventory capacity first. Six neighbors are checked for unsafe fluids/falling terrain. Loose automated output is always zero. Third-party loot/capability/event hook costs remain trusted and are not bounded by our collection limits.

Cradle validation reads nine deck cells and at most thirteen tendon cells. Descent enumerates **at most eight intersecting entities**, counting spectators too, refuses the cap, admits only securely positioned non-riding players, and prevalidates 19 positions before replacing nine deck blocks, removing nine old ones and extending one tendon. It does not enumerate a global player list or carry arbitrary block entities. Initialization writes at most eleven inert states. Rootstock owns exactly three fixed tip records, each eight steps and nine cut cells per step; its support check reads three treads and its floor mark is one passive write. Crucible enclosure checks read 72 side/corner cells and 25 bottom cells, plus at most 25 seal cells. Digestion/draining writes one cell; opening writes at most 25 lid cells plus four access cells. Thus the largest successful admitted structure transition is **29 world block writes**, plus a possible controller active-state transition. Structural transactions can roll back their already-written prefix on refusal; conservatively allow twice the 25-edit transaction bound plus one controller transition: **408 direct write attempts per dimension tick** across eight admitted operations. Native lighting and trusted event-hook costs are additional; this is not a tick-latency guarantee.

Downward cores persist nine output stacks, nine pending stacks and a fixed set of clamped scalar fields. Individual loot emission and item-stack sizes retain the shared nine-stack/64-item limits. No NBT-supplied inventory size or tip count expands these structures. Cradle/crucible excavation caps at 300 cells; rootstock at 216 planned cuts with a maximum depth of sixteen. Decorative parts have no ticker, block entity or loot. Models use original existing four 64×64 material sprites and no additional atlas animation, particle emitter or block-entity renderer. Each new model is capped at 24 cuboids by the asset gate.

Platform-console resolution is admitted through the existing CONSOLE quota before searching at most **52 loaded positions** (13 heights × four directions); the result is not retained in a global map. The native menu owns one resolved controller reference and validates the current terminal/landing on refresh or command. Moving or removing the terminal invalidates that menu. The 16 integer telemetry fields use the existing short-word synchronization and command/refresh cooldowns; there is no new packet protocol.

GameTests cover quota refusal, actual resource conservation, full-output back-pressure, bounded recovery and platform obstruction. These complement, but do not replace, the original nine-excavator shared-quota test. Many loaded idle cores, eight simultaneous enclosure validations, multiplayer rider latency and prolonged third-party chunk loading still require measured stress/soak profiles before production acceptance. Disposable lab terrain preparation intentionally runs outside gameplay simulation budgets and is only enabled by its explicit development profile.

## Rule

Every design or implementation that can materially affect server tick time, memory, save size, network traffic, chunk loading, entity count, or client rendering must include a performance discussion before it is accepted.

Performance is a correctness property. A mechanic that works for one block but stalls or crashes a long-running multiplayer server at realistic scale is defective.

Every scalable system must define:

- the unit of work;
- its maximum work per tick;
- whether cost scales by player, loaded chunk, block, block entity, network, or world;
- behavior while its chunk is unloaded;
- persistence and network costs;
- a degradation or back-pressure strategy;
- a stress case and measurable acceptance limit.

## World item entities

Unbounded item drops are prohibited. Automated or large-area operations must not create one item entity per converted block, plant, sample, or failed inventory insertion.

Preferred order of handling:

1. Insert directly into a bounded inventory or biological transport network.
2. Aggregate identical outputs into stacks.
3. Queue a bounded amount of back-pressure inside the owning machine.
4. Pause processing when output capacity is exhausted.
5. Drop a small, explicitly capped fallback only for player-visible exceptional actions.

Breaking a small player-owned inventory may return its contents, but the number of emitted stacks must be bounded by that inventory's slot count. Cascading block conversion, infection spread, plant assimilation, DNA sampling, and background simulation may not eject loose items.

DNA collection should normally credit a nearby genome network or remain stored in the converted biological block until harvested. It must never spray a sample entity for every automatic conversion.

## Chunk loading

- Infection does not force-load chunks by default.
- Simulation stops cleanly when a chunk unloads and resumes from persisted state when it reloads.
- No task may recursively load neighboring chunks to continue spread, pathfinding, network traversal, or structure discovery.
- Any future explicit chunk-loading organ requires ownership, dimensional limits, a hard per-player/team cap, configuration, diagnostics, and adversarial testing.
- Costs must be evaluated as **work per loaded chunk multiplied by the maximum loaded chunks**, including interaction with third-party chunk loaders.

Chunk loaders turn local mistakes into permanent server-wide work. Every loaded-chunk mechanic must therefore remain bounded even when no player is nearby.

## Bounded simulation

- Never perform an unbounded flood fill, recursive network walk, or whole-chunk scan in one tick.
- Use iterative queues with visited limits, time/work budgets, and resumable state.
- Infection spread must use a central or chunk-level budget rather than independently ticking every infected surface block.
- Ordinary decorative infection blocks must not be block entities.
- Functional organs may be block entities, but inactive organs should avoid per-tick work where practical.
- Cache expensive topology only when invalidation is cheaper and reliable; otherwise use bounded incremental discovery.
- Neighbor updates and block replacements must be batched and capped to prevent cascading update storms.
- Algorithms must tolerate cycles, invalid references, removed blocks, chunk unloads, and integer overflow.

## Networking and persistence

- Synchronize state transitions and compact deltas, not unchanged state every tick.
- Client-only visuals should be generated on the client from synchronized state whenever exact server timing is not gameplay-critical.
- Packets require size limits, rate limits where player-triggered, validation, and safe rejection.
- Persistent collections require maximum sizes and versioned codecs.
- Do not save derived caches when they can be rebuilt incrementally.
- Malformed or legacy data must fail safely without allocation proportional to attacker-controlled sizes.

## Visual effects

Particles and decorative animation must not add server tick or packet cost unless other players need an authoritative event.

The Mutation Chamber prototype follows this model:

- the server synchronizes only the `active` block-state transition;
- the double-helix particles are generated locally by each client;
- emission is limited to chambers within 32 blocks of the local player;
- one visible chamber requests four particles per client tick;
- a shared effect-module budget permits at most 64 requested particles per client tick;
- excess emitters degrade by omitting particles instead of allocating an unbounded queue;
- geometry and budget behavior have fast deterministic tests;
- the client showcase captures multiple animation phases for inspection.

The current chamber still performs a small server-side block-entity check every tick. This is acceptable for the prototype, but the production chamber needs a placement-density stress test. If large chamber counts materially affect tick time, inactive chambers must move to scheduled/event-driven work and active processing must use a coarser bounded interval.

## Biology lab implementation budgets

- Substrate has no block entity. Random ticks can mature it and attempt budgeted vegetation assimilation directly above it; they never spread the ground. Mature substrate retains random ticks to retry quota-rejected assimilation and notice new plants. Bone meal checks at most 26 loaded neighbors and converts at most one ground cell. Texture selection is a stored 0–15 coordinate tile; 96 model variants cover three stages and specialization without a custom renderer.
- Reservoirs are limited to 4×4×4, 64 cells, and 64,000 biomass. Flood discovery exits on an over-size axis or member count, with at most 385 world probes (one seed plus six per accepted member). It never calls world access on a chunk reported unloaded.
- Reservoir cells have no ticker. Neighbor-triggered scheduled rebuilds and transfers share **eight operations per dimension per server tick**. A rejected scheduled rebuild retries after 10–29 ticks. Rendering changes notify clients only when the quantized fill changes; visual updates do not notify neighbors or recursively rebuild topology.
- Connected reservoir surfaces derive six boundary flags from the already-discovered cuboid, with no extra world probes. Only fill or boundary changes notify clients. The 17 fill levels × 64 boundary combinations still produce 1088 bounded block states. A client-only model loader bakes the rounded outside and inset liquid at resource reload; pure mesh code has no world or platform dependencies. There is no block-entity renderer, runtime mesh construction, geometry cache keyed by position, or extra timer. The current complete catalog contains 64,288 quads (largest cell: 355; hard limit: 384). The maximum 4×4×4 test, across all 65 visible water heights, requests at most 1120 quads before neighbor culling; tests enforce a 2000-quad ceiling. These are geometry counts, not measured FPS/TPS. Native composite models dispatch two fixed layers: opaque biomass first, translucent skin afterward. Mixing them in the translucent sorter is prohibited by a graphical contract check because it produced triangular holes. Transparent outer membranes still require dense-base profiling alongside other glass effects.
- Each reservoir cell persists one clamped integer. No controller ownership list or asynchronous cache needs migration after splitting. Integer allocation distributes rounding remainders exactly. An invalid structure retains local data and refuses transfers.
- The fixed extractor checks at most 14 template cells per attempt, with a ten-tick cooldown on extraction attempts. It checks at most four adjacent ports; reservoir transfers use the shared budget. It does no background scanning or production and records four counters capped at 16.
- Outputs go directly to reservoir storage or available player inventory. Ordinary extraction creates zero item entities. Breaking a cell/controller produces at most one item stack carrying bounded data. Full storage and full inventories apply back-pressure.
- Texture animation is a four-frame client atlas animation. Neither multiblock adds a block-entity renderer, particle emitter, periodic synchronization packet, or chunk loader.

The numerical bounds and conservation rules are tested. A many-player or chunk-loader soak and measured maximum-density server/client profiling are still required before calling this production-ready. Local bounds alone do not establish a server-wide latency guarantee; repeated player inspection and thousands of scheduled placement events must be included in that profile.

## Colony prototype additions

Vascular Stitches are static, non-ticking blocks: 24 states and six seven-element native models (at most 42 quads). Placement or reversal checks two loaded endpoints and changes only their outlet states; no excavation or propagation is performed. A route still caps at 64 total nodes, including grafts. Each graft visits at most two endpoint blocks in addition to the node itself, so use a conservative four loaded-block probes per route node when mixing tissue and stitches. No new queue, block entity, network packet, item buffer or chunk loader is added. The visible-riser GameTests use real BlockItem placement and player interaction to carry four items over two rises in either direction. This does not establish dense-base frame time or placement-storm performance.

Connected tissue now uses 30 directed inlet/outlet pairs, 480 macro-tiled skin models and 90 channel models. Each cell has at most 54 baked surface quads before neighbor culling. No runtime mesh generation is added. Inlet appearance checks at most six immediate loaded neighbors on placement, neighbor notification or an outlet click; its own derived-state write uses client-only notification flags and does not propagate neighbor updates. No tickers or persistent topology collections are introduced. The 2016 possible tissue states replace the previous 288; atlas animation cost is unchanged. Dense-base client frame-time and placement-storm profiling remain required. The older straight model assets remain available but block states select the connected models.

Vein pulse rendering uses six fixed 64×512 eight-frame atlases (three channels and reversed vertical playback), with four client ticks per frame and interpolation between frames. The cost of atlas animation does not grow with placed vein count; drawn geometry still does. Recessed tissue uses three static bed elements plus one horizontal membrane face or two vertical membrane faces. There are 48 bed models (16 macro tiles × three recess axes); UVs remain inside their sprites. Drone trails are nearly flush rather than protruding. Dense-base client profiling remains necessary. No particles, block entities, packets, server timers or routing changes are added by these visuals. Gatherling geometry adds a fixed six lower-leg segments and two mandibles; its existing entity and work caps are unchanged. No chunk-loading behavior is added to the Sail.

Brood death finalizes removal immediately after its bounded death loot. It does not release admission capacity while retaining a separate, potentially accumulating corpse population. The lethal-damage test asserts both removal and same-tick capacity reuse.

Nursery admission uses the existing shared brood quota. It checks nine floor cells and sixteen headroom cells, enumerates at most 32 intersecting living entities, then considers at most eight berths. A crowded query fails closed at the cap. The roster adds one bounded 0–7 berth integer per member (saved-data version 2); legacy/malformed assignments are assigned a free berth without increasing population limits. Unloaded members keep their berths. Pure perimeter approaches contain at most five cells; together with the existing 64-cell route, the derived worker path has at most 67 cells. Movement checks at most two floor locations and two headroom locations per tick; a nursery floor may require two block-state reads to classify. No free-world navigation or extra periodic job queue is introduced. Idle workers use the existing 20–24-tick work schedule and bounded pickup/output simulation before leaving their berth; they do not wander while no job is available. The 64-entity stress fixture now uses ten separated 3×3 nurseries. These bounds do not certify collision-free traffic or multiplayer tick latency.

Brood uses a persisted ledger of at most 64 UUID reservations per dimension, seven per cradle (six larvae/workers and one Sail), and three larvae. Unloaded entities retain reservations. Vanilla AI is disabled: no goals or free-world navigator runs. At most 64 creatures perform bounded movement per tick. Courier route/interaction work shares 16 accepted operations per dimension tick; rejected requests retry, accepted work repeats after 20–24 ticks. Routes cap at 64 cells, pickup enumeration aborts after 16 intersecting item entities, and storage uses the shared 54-slot adapter. Cargo caps at eight items. Broken/unloaded routes pause without loading chunks or teleporting. Expiry waits for cargo delivery. Death may return one exceptional cargo stack. The loaded maximum-reservation GameTest checks admission, work and cleanup, not multiplayer TPS.

Armor has no tick handler or custom packets. A full-set access query reads exactly four equipment slots. After successful qualification, the optional Curios adapter reads only symbiont slot 0, without the library's same-tick item-search cache. Other symbiont slots are not scanned.

Transport organs retry every 20–24 ticks and share eight accepted operations per dimension per tick. Each operation visits at most 64 directed tissue nodes with at most two loaded-block probes per node. Inventory extraction/insertion inspect at most 54 slots each; fluid transfer calls at most one drain and one fill, capped at 250 mB. Every organ persists at most one item and 250 mB, and never exports loose items. Output refusal retains the bounded buffer and stops new extraction. Fluid Bladders each have one 4000 mB tank; only four-step fill transitions notify the client. Tissue itself has no ticker or block entity. No topology cache or traversal queue persists. External capability implementations are trusted to honor their contracts; this gate does not establish performance or transactional correctness of every third-party handler. A large population of idle organs still schedules callbacks, so maximum-density profiling is required.

Lift work shares a dimension-local quota of 16 accepted validations per tick. Each validation reads at most 26 center cells, 16 floor/roof ring cells, 192 wall cells and 96 exit-adjacent cells; occupant handling may first read 24 cells downward. Oversized and unloaded shafts fail closed. Medium never flows, has no block entity, and performs no idle work. A constant-size last-handled tick prevents duplicate occupant effects in adjacent cells. Propulsion uses one vanilla velocity update per accepted occupant tick, not a new packet protocol. This bound is not proof of smooth multiplayer motion: client prediction, latency and anti-flight handling need a dedicated-server soak.

## Aerocyte Bloom budgets

An empty, disconnected bloom stops after its scheduled check. A charged bloom, or an empty bloom touching a reservoir, checks every 20 ticks, firing no faster than once per 40 ticks. Placement, neighbor changes, capability deposits and chunk load re-arm it; no chunk ticket is acquired. Scan/refill admission shares **four operations per dimension per server tick**. Direct feeding checks at most six loaded neighbors and attempts one capped reservoir withdrawal. Rejected requests retry after 5–19 ticks with a diagnostic; there is no background work queue. This hard ceiling is not a fairness or maximum-latency guarantee under overload.

Each scan enumerates at most **24 intersecting living entities**, keeps at most 24 eligible candidates, and attempts at most four line-of-sight rays. Dense passive-mob crowds can reduce target acquisition; the prototype does not bypass the cap to search every entity. Target centers are at most 24 blocks from the muzzle. Before each ray, all chunks in its X/Z bounding rectangle are checked (at most nine); unloaded terrain fails closed. Vanilla collision ray tracing is then limited to that short segment. No whole-chunk or world search is performed.

Each organ stores one magazine (0–64 shots), 0–250 pending biomass, cooldown (0–40 remaining ticks), a saturated diagnostic shot count and version 1. Missing legacy biomass defaults to zero. There is no persistent target list or projectile entity. Shots, successful deposits and conversion of a pending batch synchronize state; a conversion and shot in the same callback can produce two updates. No unchanged per-tick synchronization is performed. Break drops comprise one data-bearing block item. Shared combat hooks suppress automated mob rewards rather than spawning a growing collection; normal player death drops are excluded from suppression. The dragon's delayed death phase retains one boolean reward-suppression marker, not an external collection. No player/friendly target is admitted as an aerial target even through a modified target tag.

Client geometry is bounded to 37 cubes (222 quads) in seventeen bones, rendered by the existing GeckoLib dependency. There is one cached inventory preview, not a preview entity per frame. A transient shot timestamp admits **one green pellet per visible shot**, using the existing shared 64-particle budget and 32-block visibility rule. It lives for ten ticks with constant velocity and emits no children or trail. Missed/offscreen shots are not queued. Chamber wisps are unchanged. No particle packets or persistent missiles are added. Dense animated-organ and transparency/FPS profiling remain necessary.

The fleshier reservoir replaces lower membrane faces with an opaque muscular sole, rather than adding overlapping layers. A single extra vertical mesh knot produces a 64,288-quad catalog, at most 355 faces per cell and 1120 faces for the maximum 4×4×4 fixture; existing 384/2000 hard ceilings remain. Exact-knot liquid clipping deduplicates vertices before triangulation. No server work or animation ticker is added.

The stress fixture loads and feeds 64 organs, asserts aggregate shots advance by no more than four per tick and verifies work really occurred. It cleans up the active organs to avoid starving later tests through the shared quota. This is a bound check, not a TPS, fairness, full boss-combat or multiplayer certification.

## Digestive tissue and biomass transport

Digestive cells have no ticker, network updates or idle scans. Contact work uses a shared **eight accepted attempts per dimension tick**, with one timestamp per living entity limiting contact to once per twenty ticks across all digestive cells. Full-suit checks read four slots. At most two base damage and twenty biomass are credited per accepted contact; only actual health loss counts. Local storage clamps to 250. Full cells retain defense but omit excess yield; they never eject items. There is no autonomous reproduction or resource-generation timer. This allows healing-based farms in principle, but neither their yield buffer nor transfer throughput is unlimited.

Patch extraction shares **eight operations per dimension tick**, separate from contact admission. Six-neighbor discovery caps at 64 members, 385 visited/read positions and at most 64 subsequent cell-buffer reads. Missing chunks and oversized patches fail closed. Queues and sets are temporary and bounded; no position collection is persisted. One block item carries each broken cell's local remainder. The 64-contact GameTest asserts no more than 160 biomass can be credited in one tick; the 65-cell patch test checks refusal and conservation. These are work bounds, not a measured TPS guarantee.

Amber transport reuses the existing eight-operation quota, 64-node route and scheduled pump cadence. It adds a single clamped 0–250 transit-biomass integer. Each accepted biomass pulse uses at most one extraction and one insertion through the scalar capability contract; source detection adds one capability lookup. Reservoir operations retain their eight-operation discovery/distribution quota and 64-cell limit. Each partial transfer is atomic at its endpoint; the pump owns the remainder when a destination refuses. No conversion to/from ordinary fluids, item entities, target search, persistent topology cache or forced chunk loading is introduced. Invalid/full storage and unloaded routes apply back-pressure.

The new ground surface has sixteen static macro-tile models, each with six cube faces and three pore faces. It reuses original art, with no renderer, animation ticker or particles. Existing biological storage/defense update bounds still apply. Many-player profiles, maximum-density frame-time measurements and chunk-loader soaks remain outstanding.

## Surface intake transport option

The transport module owns one persisted boolean per existing port (absent in legacy saves means above-organ intake). Surface mode derives one source position from the already-read first vein's outlet: one cell horizontally behind it at ground level. It adds no source search, idle scan, vein ticker, entity, packet, chunk ticket or extra scheduled pulse. Normal inspection reads at most the first vein again to report source coordinates; sneak configuration only changes the bounded setting, without triggering a transfer. All endpoint access retains loaded-chunk checks and existing sided capability semantics. Routing still caps at 64 cells, patch discovery at 64 members, extraction at 250 biomass, and transport admission at eight operations per dimension tick. Existing back-pressure retains cargo through mode changes. Four-direction, real-contact, blocked-output, persistence and invalid vertical-start GameTests cover the new option; these are not a dense-base TPS measurement.

## Vegetation assimilation

`vegetation` owns the plant allowlist, harvest identity, tree propagation and dormant state. The existing substrate adapter calls its single `seed` command; vegetation has no dependency on substrate, its registry or resource transport. The pure six-face selector never recurses or stores a frontier. Registration and creative inventory exposure are the only other public operations.

Placement, substrate random ticks, infected tree random ticks and bone meal share **eight accepted attempts per dimension per tick** through `simulation.WorkBudget`. Each accepted seed reads at most the plant and optional tall-plant upper half; a tree step reads six loaded face-neighbors and rechecks one chosen target. Admission includes empty attempts. Origin/type checks are constant work before admission; rejected callbacks do not scan neighbors or enqueue retries. At most eight plants/tree cells convert per dimension tick; tall grass/fern additionally removes one upper half, for at most sixteen conversion writes (plus up to eight dormant-state writes instead of conversions). Every assimilation/dormancy write uses `UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE`: it updates rendering without vanilla recursive neighbor propagation, which can read unloaded adjacent chunks and cause incidental plant drops. These replacements retain support: logs keep the vanilla logs tag and canopy cysts retain the replaced leaf's distance. This also means automatic assimilation does not pulse neighboring observers. Player placement/breaking retains normal vanilla updates. A real loaded-edge conversion test verifies the adjacent unloaded chunk remains unloaded, including the placement-notification path.

Substrate keeps random ticks (including mature cells) so rejected plants survive and retry. Its plant-support hook keeps plants supported while waiting; unsupported flowers/saplings are preserved. An infected tree cell becomes dormant after finding no convertible neighbor, and a new eligible neighboring block re-arms it. An unloaded boundary keeps it eligible for a later random tick. No scheduled growth queue, block entity, timer, world-position cache, packet type or chunk ticket is added. Natural random tick opportunities still scale with loaded sections; the eight-attempt ceiling is not a fairness guarantee or a claim of measured TPS with thousands of chunk loaders. Default random growth can be slow; bone meal gives a visible, bounded acceleration path.

The explicit prototype allowlist covers eight vanilla log species, dry mangrove roots and ten leaf types; stripped wood, waterlogged vegetation and unknown mod blocks are skipped. There is no ownership or natural-tree detection: connected player-built logs are vulnerable. Connected forests can gradually assimilate across arbitrary distances **over time**, but never through unloaded chunks or an unbounded scan. There is no autonomous spread of substrate ground in this experiment.

All output stays implicit in block state until harvest. Logs encode one of nine original wood/root identities and axis (54 states including active); canopy encodes leaf distance and active (14 states); brushes encode three harvest categories. Automatic assimilation spawns **zero item/XP/mob entities**. A normal harvested block has one fixed output stack: original wood/root ×1, seed ×1, sticks ×2, berries ×2, or rotten flesh ×1. No bonus affected by Fortune or alternate Silk Touch output is introduced. Canopy feedstock can be manually fed into a reservoir; no new automatic biomass or DNA producer is claimed.

Five code-native models reuse original biological materials, with at most sixteen elements / 96 quads per block before culling. No new textures, particles, renderer or animation timer. Contract tests check materials, geometry bounds and block-shaped inventory previews; the real client checks every block-state model and captures a tree before and after real culture/bone-meal use. A 64-plant placement/retry stress test asserts the eight-conversion ceiling, eventual progress and no loose drops. Species/axis serialization, tall halves, actual culture and bone meal, unsupported vegetation, six-axis propagation and dormancy/rearming have GameTests. Large forest soak, dense-canopy FPS and multiplayer/chunk-loader profiling remain outstanding.

## Known crash and denial-of-service patterns

Design reviews and tests must explicitly consider:

- item, experience-orb, projectile, particle, or mob entity storms;
- chunk loaders multiplying background work indefinitely;
- recursive spread or network traversal causing stack overflow;
- cyclic graphs causing infinite traversal;
- block updates recursively triggering more block updates;
- unbounded queues that grow faster than their processing budget;
- oversized NBT, attachments, packets, or genome catalogs exhausting memory;
- corrupted saved data causing repeated load crashes;
- recipes or automation loops producing outputs faster than storage can accept;
- cross-dimensional lookups loading or retaining worlds unexpectedly;
- per-player packet spam or expensive commands without rate limits;
- accidental client classes on a dedicated-server code path;
- async access to world state causing races or corruption;
- arithmetic overflow turning a limit into a negative or enormous allocation.

Limits must be enforced in code. Documentation or configuration defaults alone are not protection.

## Open carapace / vanilla dye rendering

`bioarmor.client` owns a pure bounded anatomy and its native-model adapter. Eight models (four branches × inner/outer) are baked once, never rebuilt per frame; geometry tests cap each at 80 cubes. Empty posing bones replace the opaque humanoid under-shell. Two native material passes separate tintable chitin from untinted organs/horn; their atlas alpha masks are disjoint, preventing doubled opaque material faces. Trim and enchantment passes remain vanilla. Sixteen 512×256 layer files total 2,097,152 texels (8 MiB raw RGBA before GPU overhead/mipmaps), plus two 256×128 inventory material sprites. This is a bounded client cost, not a measured FPS guarantee; densely equipped players/stands still need profiling.

Dye crafting and washing use native recipes, `DYED_COLOR` and cauldron behavior. Setup adds exactly four entries to the existing water-cauldron interaction map. No server ticker, scan, queue, entity, chunk loading or custom packet is added. Dye is one bounded native color component, preserved alongside the existing bounded evolution component. Seven additional opt-in lab poses reuse the same player; they add no permanent gameplay entities. The common capture driver initializes a deterministic first-person view and restores camera/shift input on successful completion.

## Verification

### Synaptic console and active junction intake

Active intakes reuse the existing eight-operation TRANSPORT lane and 20–24 tick cadence. One arm is examined per accepted junction operation. Reverse routing visits at most 64 nodes and checks six loaded neighbor positions per node; stitch predecessor validation may add two endpoint reads per candidate, so the conservative bound is 19 loaded-state probes per node, plus constant source checks. Missing neighbor chunks fail closed. No search frontier, topology cache, vein block entity or new scheduled callback is added. Branch ambiguity refuses rather than traversing every branch. Source extraction, optional output and mounted-storage deposit retain the scalar 250-unit limit and endpoint-specific quotas. This is more work per accepted intake than the previous outward-only route; it still needs maximum-density profiling, not just a local bound claim.

Configuration uses one additional dimension-local CONSOLE lane, capped at eight accepted opens, updates or commands per tick. One open menu refreshes no faster than every ten ticks and accepts commands no faster than every four ticks. Diagnostics traverse only the requested arm, at most 64 nodes, without resource transfers. No whole-network scan, chunk loading, remote command coordinates, background history or extra packet type exists. Native menu data sends changed values through 24 short words (12 full integers), preserving signed world coordinates and saturated transfer totals. Range (eight blocks), player/session identity, dimension, held probe, target identity, spectator status and spawn-interaction permission are rechecked server-side. Actual mouse-click and native packet/menu tests exercise the boundary. Third-party claim integrations still require their own compatibility tests; vanilla permission checks alone are not a claim-system certification.

Each junction adds three bounded persisted integers: port/pause bits (0–511) and two saturated lifetime arm counters. Diagnostics are transient constant-size data. Four boolean block-state apertures multiply the three stem forms to 48 states, using shared multipart geometry: four port pieces, four cuboids each, at most 96 extra quads per base. Shape and color distinguish a green open lumen from a fleshy sealed cap; no renderer or server animation ticker is added. Existing green biomass atlas animations retain eight frames and 64×64 resolution. The probe contributes one 64×64 item sprite.

The disposable logistics fixture replaces a fixed 23×17×10 area, creates one extra-health husk and at most one pending acknowledged phase. It is off in normal play. The husk retains ordinary physics and digestive contact; no reservoir biomass is seeded. Capture waits for a measured positive reservoir amount. This is an end-to-end functional test, not a multiplayer TPS/latency or dense-transparency benchmark.

The existing brood death-capacity test has its own batch because other default-batch hatch tests could consume the shared 16-operation lane in the same tick. No quota was raised and no brood runtime behavior changed to make the gate pass.

### Growable junction / living reservoir revision

The `junction` module shares the existing **eight accepted transport operations per dimension tick**, including manual tests. A base schedules one callback every 20–24 ticks; its optional upper stalk is passive and has no block entity. One accepted callback selects one of four horizontal directions, reads its first loaded vein and follows at most one existing 64-node directed route (including bounded stitches). It performs at most three scalar transfer calls: mounted-storage extraction, consumer insertion, and spare-buffer deposit. Each call is capped at 250, with endpoint-specific discovery quotas unchanged. Capability presence checks and loaded-position checks are constant extras; capability callbacks only mutate the local buffer and never recursively route.

Each base persists version 1, one 0–250 integer and one 0–3 cursor. No topology cache, packet type, persistent queue, forced chunk, ordinary item drop or collection is added. Missing chunks/invalid routes/full endpoints pause delivery. Nine simultaneous junctions exercise the shared admission ceiling. Round-robin rotates output opportunities but is not a global throughput/fairness guarantee. Thousands of idle scheduled organs and third-party chunk loaders still require density/latency profiling.

Bone-meal growth checks one loaded air cell and build-height limits, then writes at most two blocks. It never translates storage. Breaking a base removes at most one passive upper block and emits at most one bounded data-bearing item; removing the upper block emits none. Reservoir data is not owned or copied by the organ.

The revised reservoir catalog has **96,544 quads**, at most **525 per cell** (hard cap 576), and **1536 per maximum 4×4×4 reservoir** across all water heights (existing 2000 ceiling). Compared with the previous catalog, inward walls increase baked geometry; this is deliberately paid at resource reload, not per frame or server tick. Outer flesh replaces membrane facets rather than adding intersecting layers. Two fixed native render layers remain. Four selected 64×64 texture sprites add no animation timers; biomass's baked emissive floor adds no server lighting propagation. These counts are not an FPS measurement, and dense transparency still needs profiling.

The opt-in disposable visual fixture adds ten poses and at most one pending integrated-server acknowledgement at a time. It uses the shared capture module, no gameplay packet. Its bounded setup and cleanup affect only the explicitly selected lab; ordinary gameplay does not run them.

Performance-sensitive modules require, as appropriate:

- pure tests for budgets, queue limits, and termination;
- GameTests for bounded world effects and unload/reload behavior;
- dedicated-server startup and soak tests;
- stress fixtures at the supported maximum density;
- packet and save-size measurements;
- entity-count assertions after automated processing;
- profiler captures with stated hardware and scenario;
- severe-log and crash-report scanning.

No benchmark may silently discard work to appear faster. Degraded behavior must be intentional, visible in diagnostics, and consistent with gameplay rules.

## Logistics validation corrections (2026-09-07)

Derived vein appearance now writes with `UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE`. Client-only notification flags **alone do not suppress vanilla neighbor-shape propagation**: a real loaded-edge regression reproduced an incidental neighboring-chunk load before this correction. Forward/reverse routing and endpoint queries are checked against the same unloaded boundary. Ordinary player block placement keeps its native update behavior; this guarantee concerns our derived appearance writes and route/capability work.

Organ pause/removal guards add constant-time checks at the owning endpoint, including cached handles. Mode changes clear one fixed diagnostic record, or four for pause/resume. No topology scan, schedule, capability lookup, packet type, persistent collection or resource limit is added. Nine simultaneous native server menus exercise the existing shared eight-work console admission ceiling and bounded retries; this is not a multiplayer latency or fairness measurement.

The opt-in disposable logistics lab waits for entity data in its four fixed fixture chunks before cleanup/spawning. It polls at most four readiness flags per player tick for at most 400 ticks, without acquiring chunk tickets. One cleanup enumerates at most 65 mobs, fails before removal if the 64-mob cleanup ceiling is exceeded, and otherwise removes only mobs in the lab's existing fixed reset area. A capped two-mob query rejects contaminated captures. These are test-fixture costs, not a new gameplay job or entity scan.

## Finite mining organs (2026-09-07)

Mining is a material world-editing cost. The prototype deliberately permits at most **eight accepted mining operations per dimension tick**, shared across all three tiers through `WorkBudget.MINING`. Active cores schedule every 12/6/2 ticks; paused cores every 40. One admitted operation validates at most **27 frame cells**, visits one cursor target and checks its six neighbors. It can export one stack of at most 64 items through `StoragePorts` (at most 54 destination slots), then replace at most **one terrain block**. No flood fill, ore search, moving mining entity, forced chunk or persistent work queue exists. All gameplay target/frame/neighbor/capability reads require already-loaded chunks. The owner must be online in that dimension.

Each core owns a 4,000-unit fuel scalar, nine output slots (at most 576 ordinary stackable items), owner UUID and clamped finite counters. Cursor bounds are 48/336/2,976. NBT reads nine fixed cargo keys and never allocates from a serialized inventory size. Incoming fuel accepts at most 250 per call. Output handles reject insertion and cap extraction at 64. One native core drop contains the bounded inventory; mining itself emits no item/XP entities. It prepares a nine-slot copy and fits the entire loot batch before world replacement, so full output cannot consume ore or mining fuel. Per-operation inventory work is bounded by nine loot stacks × nine merge slots, plus fixed copies.

Raw native loot streaming aborts at a tenth nonempty stack or a stack larger than min(64, native maximum). **Global loot modifiers are excluded** because NeoForge's convenience path first builds a potentially unbounded list. This bounds our output collection, not arbitrary CPU inside user-supplied loot functions/rolls or third-party break-event handlers: trusted datapacks and explicit compatibility profiling remain necessary. Never advertise universal loot/protection-mod support from these tests alone.

Terrain replacement and derived active poses use `UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE`, suppressing neighbor-shape cascades. Adjacent fluids/falling blocks fail closed. One small ceiling secretion per fourth slice can emit native light 10; core active/inactive transitions also affect native lighting. These pay bounded local light-engine work, not a custom scanner/ticker, but native relighting is a real cost. A rejected or paused core can change its active state once independently of admission; unchanged states do not resend. Scheduled callbacks still scale with the number of placed cores. The eight-work limit is not a maximum-density TPS or starvation/fairness guarantee.

Menus reuse the existing shared eight-operation console lane, four-tick command cooldown and ten-tick refresh interval. Mining synchronizes twelve integers as 24 native short words plus a fixed 45-slot native inventory. No new custom packet, per-block particle stream, topology cache or renderer is introduced. Native models have at most 24 cuboids and use four static 64×64 sprites; active/rest poses are baked states, not per-frame geometry construction.

The opt-in mining lab resets a fixed 53×27×12 region (17,172 writes) and creates three bounded 14-deep geology masses. It clears only its three known output chests before replacement, waits on twelve fixed entity-chunk readiness flags for at most 400 player ticks, and enumerates at most 65 old item entities within its reset box before accepting cleanup of at most 64. A one-item query rejects contaminated captures. These destructive reset costs and cleanup apply only to the explicitly launched disposable lab, not gameplay. No dense-base/long-soak performance certification is claimed.
