# Implementation queue — M0–M10
# Contract format and shared requirements: docs/TASK_TEMPLATE.md.
# Each packet is one accepted checkpoint; runtime statuses are generated separately.

IF-001 Pin library profiles and enforce platform boundaries
Milestone: M0
Owner: integration
Depends: none
Spec: docs/DEPENDENCIES.md, docs/ARCHITECTURE.md
Blocks: none
Scope: ["@module:integration","build.gradle","gradle.properties","settings.gradle","gradle/**","src/main/templates/**","scripts/check_boundaries.py"]
Contract: Install the exact artifacts in DEPENDENCIES; add base/JEI/Curios presence and absence launch profiles, a client capture run and packaged dedicated-server smoke run. Keep testMod out of every release artifact.
Red: An intentionally imported client class in server code or third-party type in core fails the boundary check; missing required library gives the expected loader diagnostic.
Accept: All four profiles launch on Java 21; game assertions execute, not merely compile; capture names the actual loaded profile. Add checks to Gradle verifyAll, not a second full-gate script.
Bounds: One disposable world/process per profile; 120 s readiness and 30 s shutdown deadlines; no ordinary save access.
Evidence: integration, visual

---

IF-002 Culture Bowl: one server-owned batch
Milestone: M0
Owner: processing
Depends: IF-001
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T0-02
Scope: ["@module:processing","core/src/main/java/org/jd/infestusfrontier/organ/**","core/src/test/java/org/jd/infestusfrontier/organ/**"]
Contract: Implement T0-02 and I000/I004/I005/I006/I007 Bowl recipes only where Items assigns Bowl. BatchWork.start(request, expectedRevision) returns Started or typed Refused; advance(workUnits) returns immutable state. Reserve inputs, outputs and returned containers before consumption. Introduce shared OrganHistory here; persist counts only on completed batches.
Red: Full bottle-return slot, insufficient BU and duplicate start each leave inventory/counts unchanged; a midway reload completes exactly once.
Accept: Culture renewal and Binder batches match Items; one completion earns one count; block dismantling retains one history; L1/L2/L3 choices follow Block Catalog. No auto-export.
Bounds: One active batch/core, at most 9 item slots and 2 tanks; 16 shared recipe completions/server tick; no offline catch-up.
Evidence: rules, game

---

IF-003 Probe and reusable organ screen
Milestone: M0
Owner: interaction
Depends: IF-002
Spec: docs/ARCHITECTURE.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:interaction","@module:ui","src/main/java/org/jd/infestusfrontier/processing/client/**","src/main/java/org/jd/infestusfrontier/processing/menu/**"]
Contract: Implement I010 Synaptic Probe in interaction, not in UI primitives. Compose the Culture Bowl screen from the separate internal ui module. Menu snapshots contain revision, slots, tank amounts, state and refusal code. Probe use opens/configures; empty-hand interaction does not consume a placement click. UI sends intents; only processing changes recipes.
Red: Stale revision, wrong owner, remote target, malformed slot or spammed start cannot mutate the Bowl; changing UI scale must retain clickable bounds.
Accept: Keyboard focus, tooltip contrast and GUI scales 2/3/4 are captured; two clients observe consistent progress; placement beside Bowl works; close/reopen retains state.
Bounds: One menu/player; 4 intents/player/s, 64/server tick; <=4 KiB snapshot, <=2 changed snapshots/s; no idle sync.
Evidence: rules, game, visual

---

IF-004 First-join guide and recipe discovery
Milestone: M1
Owner: discovery
Depends: IF-003
Spec: docs/GUIDE_PROGRESSION_TREE.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:discovery","src/main/java/org/jd/infestusfrontier/integration/jei/**","src/main/java/org/jd/infestusfrontier/integration/modonomicon/**"]
Contract: Implement I009 craft and once-only first-join claim; Modonomicon adapter teaches the implemented Bowl recipes and JEI displays their inputs, output, duration and BU. Advancement completion is server-owned; guide visibility follows GUIDE_PROGRESSION_TREE. Add only nodes whose content exists.
Red: Full inventory cannot lose or duplicate a claim; repeated join cannot grant a second book; creative give does not accidentally satisfy a completion-only node.
Accept: Fresh Survival player can craft Culture, find Bowl and renew Culture from the guide alone. Lost book recrafts without resetting progress; JEI absent leaves the guide usable.
Bounds: One pending claim boolean/player, no delivery queue or world item spawn.
Evidence: rules, game, visual

---

IF-005 Player-selected substrate and living visual stages
Milestone: M1
Owner: ecology
Depends: IF-004
Spec: docs/LIVING_SUBSTRATE_MUTATIONS.md, docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T0-01
Scope: ["@module:ecology"]
Contract: Implement base/mature functional host anatomy from LIVING_SUBSTRATE_MUTATIONS; Culture converts only visible selected eligible ground. Preserve reinforcement, dye and ownership through compatible mutations. Render deterministic connected variants with original 32/64px seamless textures; no autonomous spread.
Red: Hidden blocks, protected cells, unpaid conversion and incompatible host mutation refuse without changing target or inventory.
Accept: Horizontal, wall and underside neighboring cells connect without UV seams; mature stages remain distinguishable. Soil conversion cannot consume trees or grass implicitly.
Bounds: One requested conversion/use under 16 conversions/server tick; static cells have no block entity/ticker; variant selection uses position, not stored random history.
Evidence: rules, game, visual

---

IF-006 Hand-fed biomass and portable transfers
Milestone: M1
Owner: storage
Depends: IF-005
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T0-03, T0-04
Scope: ["@module:storage","src/main/java/org/jd/infestusfrontier/processing/digestion/**","core/src/main/java/org/jd/infestusfrontier/processing/digestion/**"]
Contract: Implement Digestive Sac and Bladder with Items feed yields; make BU exactly mB. QuantityStore.preview/reserve/commit use bounded integer amounts and simulation before commit. Bladder Fill slot feeds one armor-compatible container; I019 bucket holds 1000 mB, I036 ampoule 250 mB. Bucket world placement remains unavailable until containment exists.
Red: Full receiving store, partial ampoule and interrupted transfer conserve exact mB; rotten flesh is consumed only when its complete output fits.
Accept: Digest a batch, fill and partially empty ampoule, move a bucket between tanks; rejected output remains held; automate neither gathering nor delivery.
Bounds: One batch/Sac; 4000 mB starter Bladder; global 64 transfers/tick and 4 portable transactions/player/s.
Evidence: rules, game

---

IF-007 Membrane and skeletal preparation
Milestone: M1
Owner: processing
Depends: IF-006
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T0-05, T0-06
Scope: ["@module:processing"]
Contract: Implement Membrane Rack/Bone Loom recipes I002/I003/I050 through BatchWork. Leather substitution takes twice the rack time; bone blocks never become nine bones. Output is one prepared item family; no machine-local transfer framework.
Red: Full outputs and missing water preserve inputs; leather route doubles duration without doubling output; one bone block cannot mint bones.
Accept: Original readable item art and static block models; ingredients, byproducts and times agree between recipes, guide and JEI.
Bounds: One batch/organ and existing 16 shared completion budget; no new tick scheduler.
Evidence: rules, game, visual

---

IF-008 Bud placement and biological shell parts
Milestone: M1
Owner: construction
Depends: IF-007
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T0-16, T0-09, T0-10, T0-11, T0-12, T0-13
Scope: ["@module:construction"]
Contract: Implement Bud as the same item as I001; attached skin, rib, window, lumen and seed storage follow catalog construction. Introduce Structure.inspect(origin, ruleId, budget) returning Valid/Invalid/Deferred; validate only loaded positions and never recursively inspect a connected colony.
Red: Removing a shared wall cannot duplicate a core; missing chunks defer rather than load; incompatible substrate refuses a Bud mutation.
Accept: Each part crafts/mutates by its catalog route; window back faces and joins render correctly, lumen lights actual blocks; substrate reinforcement is not an extra machine upgrade.
Bounds: 64 inspected cells/call, 256/server tick; finite 4096-cell structure hard ceiling; passive parts no block entities.
Evidence: rules, game, visual

---

IF-009 Bio-Furnace with earned choices
Milestone: M1
Owner: processing
Depends: IF-008
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T0-07
Scope: ["@module:processing"]
Contract: Implement baseline Bio-Furnace and the specified L1/L2/L3 growth choices using OrganHistory. Initially one hand-fed recipe; attachments unlock automation and higher heat separately. Hold output and returned containers; recipe efficiency is explicit, not fractional item duplication.
Red: 32nd batch offers one choice once; reopening, breaking or upgrading cannot award it twice; failed batches earn no history.
Accept: Manual smelting matches vanilla ingredient semantics and Items ratios; chose speed versus efficiency gives distinct measured consumption/throughput; history survives item placement.
Bounds: One reserved batch; shared recipe budget; XP from smelting is bounded stored credit, not ambient orb generation.
Evidence: rules, game, visual

---

IF-010 Awaken four independently fueled armor pieces
Milestone: M1
Owner: equipment
Depends: IF-009
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: T0-08
Scope: ["@module:equipment"]
Contract: Implement I013/I014 dormant/living equipment and Awakening Cradle. EquipmentState owns identity, frame, wear, fuel and counters; awakening preserves wear percentage and creates no fuel. Bladder manual Fill uses equipment service port, not inventory internals.
Red: Awakening worn armor cannot restore it for free; a duplicate menu confirmation cannot clone it; empty fuel remains empty after reload.
Accept: Each piece wears independently with G1 stats and stated starting tank; explicit fueling conserves BU; no full-set requirement for individual armor protection.
Bounds: Exactly four worn pieces checked; persistent schema caps collections at catalog limits; no per-item inventory ticker.
Evidence: rules, game, visual

---

IF-011 Biological models and vanilla appearance compatibility
Milestone: M1
Owner: equipment
Depends: IF-010
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:equipment","src/main/java/org/jd/infestusfrontier/integration/curios/**"]
Contract: Create original articulated armor models with visible face/skin openings, dye layer, vanilla trims and the documented enchantment policy. Rendering reads immutable appearance data; GeckoLib stays in the client adapter. Curios supplies a separate optional sample-pouch slot, never required armor stats.
Red: Dye/trim application must not reset frame, identity, fuel or mutations; absent Curios cannot crash a client or dedicated server.
Accept: Capture front/back/sides, crouch/sprint/swim and held-item poses at two FOVs; inspect gaps, clipping, transparent back faces and inventory scale; normal armor remains unchanged.
Bounds: <=96 model bones across a visible suit; no server animation packets for ordinary pose; client-distance culling.
Evidence: rules, game, visual, integration

---

IF-012 First workshop Survival acceptance
Milestone: M1
Owner: campaign
Depends: IF-011
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign"]
Contract: Add a deterministic fresh-world fixture and executable player action script: acquire Culture, renew it, build Bowl/Sac/Bladder/Rack/Loom/Furnace, awaken and manually feed one piece. Drive normal recipes and interactions, not direct inventory injection after fixture setup.
Red: Remove a recipe unlock or make a returned bottle unavailable and the route fails at the named action rather than timing out silently.
Accept: Packaged client/server complete the route with no administrative commands after initial fixture creation; record actual inputs, BU and action sequence; every shown recipe exists.
Bounds: Finite <=300 actions, per-action timeout and owned disposable world; no simulation clock sleeps without state assertions.
Evidence: rules, game, visual

---

IF-013 Activation Cyst and prepared tissue reagents
Milestone: M2
Owner: processing
Depends: IF-012
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-30
Scope: ["@module:processing"]
Contract: Implement Cyst construction without requiring any of its own outputs; activate I020-I030/I033/I028 inputs only through catalog recipes. Reuse BatchWork; introduce no separate machine engine.
Red: A first Cyst constructs from starter products only; inactive minerals cannot substitute for prepared grafts; refused conversion retains containers.
Accept: Each Cyst recipe has original item art, guide and JEI entry; hand-feeding works before Item Veins; resin's Elastic Gel alternative avoids a tree bootstrap cycle.
Bounds: One batch/core, existing shared completion limit.
Evidence: rules, game

---

IF-014 Embedded biomass veins and configured ports
Milestone: M2
Owner: logistics
Depends: IF-013
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-01, T1-04
Scope: ["@module:logistics"]
Contract: Implement biomass tissue edges and Vascular Junction. RouteService.transfer(source, destination, amount) plans then commits exact BU using QuantityStore ports. Probe configures six faces as closed/input/output/both; junction can support a reservoir and grow from one to two blocks with bone meal.
Red: Cycle A-B-A cannot transfer twice; blocked output or unloaded endpoint retains source; both-direction port cannot withdraw the same reserved amount twice.
Accept: Green pulses follow committed flow and remain inside all exposed faces. Source -> junction -> consumer works with a reservoir above at either height; screenshots include underside and vertical segment.
Bounds: <=4096 loaded nodes/network, <=64 probes/network/tick and 512/server tick; <=64 transfer commits/server tick; one invalidation flag/node, no unlimited route queue.
Evidence: rules, game, visual

---

IF-015 Terrain stitches, turns and independent crossings
Milestone: M2
Owner: logistics
Depends: IF-014
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-05, T1-06
Scope: ["@module:logistics"]
Contract: Implement six-axis turns/splits/joins, Septum Crossing's separated channels and Vascular Stitch across one exposed terrain step. Probe selects visible endpoints; never require mutation of a hidden block. Topology edges have explicit channel and direction.
Red: Crossed channels cannot mix; adding/removing a step invalidates only affected routes; a hidden endpoint or gap beyond one block refuses.
Accept: Build uphill/downhill route over a rounded hill with turns, a three-way junction and crossing; visual veins join at face centers without top-face texture artifacts.
Bounds: Use RouteService budgets; invalidate a bounded neighborhood, never rebuild whole network synchronously.
Evidence: rules, game, visual

---

IF-016 Expandable reservoir with seam-free connected body
Milestone: M2
Owner: storage
Depends: IF-015
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-07
Scope: ["@module:storage"]
Contract: Implement rectangular Reservoir Cell formation with a single authoritative capacity index and per-cell retained quantities. Adding an invalid extra cell cannot lose existing contents or history; split partitions contents deterministically without duplication. Integrate the existing junction port rather than a second opening organ.
Red: Remove a center/outer cell at full capacity, reload or add a nonrectangular cell: total stored plus recovered BU remains equal; invalid addition never zeroes the core.
Accept: Connected inner glass/flesh faces are culled; rounded fleshy base and restrained glow render from inside/outside, below, and through opposite wall. Junction underneath fills and drains it.
Bounds: <=512 cells/reservoir, 16000 mB/cell baseline; shared Structure inspection limits; one visible surface mesh per formed region, no ticker per wall.
Evidence: rules, game, visual

---

IF-017 Item and ordinary-fluid channels
Milestone: M2
Owner: logistics
Depends: IF-016
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-02, T1-03, T1-08, T1-09
Scope: ["@module:logistics","src/main/java/org/jd/infestusfrontier/storage/**","core/src/main/java/org/jd/infestusfrontier/storage/**","core/src/test/java/org/jd/infestusfrontier/storage/**"]
Contract: Add typed Item/Fluid channels over RouteService; Item Capsule and Fluid Cyst expose bounded store ports. Crossing separates payload kinds; biomass is not converted into arbitrary fluids. NeoForge capability adapters wrap reservation checks.
Red: Simultaneous hopper and vein withdrawals cannot clone items; incompatible fluid insertion or simulated transfer has no side effects; capacity shrink preserves contents.
Accept: Two channels share a physical junction without mixing, sort order is stable, full receiver pauses; compatible vanilla chest/furnace and fluid test handler work.
Bounds: 27 slots/Capsule, 16000 mB/Cyst; same global transfer/probe budgets, no item-entity transport.
Evidence: rules, game, integration

---

IF-018 Reusable mouths, filtering and safe overflow
Milestone: M2
Owner: logistics
Depends: IF-017
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-10, T1-11, T1-12, T1-13
Scope: ["@module:logistics"]
Contract: Intake/Output mouths perform one directional port transfer; Filter Valve applies explicit item/fluid whitelist and quantity limit; Overflow Valve uses configured reserve before exporting excess. No automatic world dumping.
Red: Two consumers race for the last reserved unit; full overflow destination leaves source full, not spilled; a route loop cannot bypass filters.
Accept: Player builds shared feed/reserve/overflow branches, observes held blockage reason and adjusts with Probe; ordinary hoppers remain viable on compatible faces.
Bounds: <=9 filter entries/port, no recursive tag scans during transfer; same shared routing quotas.
Evidence: rules, game, visual

---

IF-019 Stable readiness signals and reusable interlocks
Milestone: M2
Owner: control
Depends: IF-018
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-14, T1-15, T1-34, T1-35
Scope: ["@module:control"]
Contract: Implement Sensor Polyp/Nerve Tissue carrying ready, working, result-held and blocked; Reflex Knot AND/OR/NOT of <=4 named local signals; Selector sends one idempotent start to one ready lane, round-robin or fixed priority. Vanilla comparator mirrors stable state.
Red: Holding start high never repeats one job; a result remains held across unloaded receiver; readiness loop cannot recurse in one tick.
Accept: Two branches complete jobs in order without timed redstone clocks; switch fixed priority versus round-robin produces distinct measurable scheduling.
Bounds: <=32 signal transitions/network/tick, 256/server tick; <=4 inputs/Knot and 8 lanes/Selector; next-tick propagation only.
Evidence: rules, game

---

IF-020 Network monitoring and configuration UI
Milestone: M2
Owner: control
Depends: IF-019
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-16
Scope: ["@module:control"]
Contract: Synaptic Console queries loaded nodes through paged RouteService views; Probe shows direction, flow, reserves and exact blocked reason. I037 Route Imprint copies configuration only after ownership/compatibility validation.
Red: Client-supplied arbitrary coordinates, excessive page size or ownership change cannot read/configure inaccessible nodes; copied imprint cannot clone contents.
Accept: Configure source -> raised junction -> reservoir -> consumer entirely through UI; visible open/closed faces agree with actual transfers; empty-hand placement is unaffected.
Bounds: <=16 rows/page, 2 changed pages/s/viewer, <=4 KiB response; no whole-network snapshot packet.
Evidence: rules, game, visual

---

IF-021 Crop beds, planting and independent harvesting
Milestone: M2
Owner: cultivation
Depends: IF-020
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-17, T1-18, T1-19, T1-31
Scope: ["@module:cultivation"]
Contract: Implement Cultivation Tissue, Planting Proboscis, Harvest Corolla and Compost Gland as separate operations. Grower reserves water/feed and real seed; harvester holds its result; planter requires an empty eligible cell. Match Overworld reference wheat loop in Items.
Red: Missing seed, unloaded crop, full seed/output reserve and same-tick harvest requests cannot delete crops or mint harvests; vanilla random tick never awards a second allocated harvest.
Accept: Manual, serial and two-bed shared-harvester layouts work; reserve seed before selling output; crops visually change through actual growth.
Bounds: <=64 assigned cells/organ, 4 loaded probes/organ/s under 256 cultivation probes/server tick; <=16 harvest commits/server tick.
Evidence: rules, game, visual

---

IF-022 Useful infected bushes and trees
Milestone: M2
Owner: cultivation
Depends: IF-021
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-20, T1-25, T1-26, T1-27, T1-28
Scope: ["@module:cultivation"]
Contract: Implement deliberate bush/tree conversion and Arbor growth allocation. Choose one harvest allocation per cycle: resin, fiber, fruit or wood; tapping does not also award a full wood harvest. Retain one cutting from a qualifying conversion; canopy pods open once.
Red: Re-converting a living tree, breaking an emptied pod or loading a canopy twice cannot create extra cuttings/wood; protected trees and foreign block entities remain untouched.
Accept: A cultivated grove visibly retains foliage, varied wood and luminous pods; choosing wood versus resin gives different useful supply chains. No barren automatic clearcut.
Bounds: <=256 explicitly assigned loaded tree cells/Arbor, <=4 cell probes/s under cultivation quota; cap held harvest to 9 stacks.
Evidence: rules, game, visual

---

IF-023 Native water and starter biological power
Milestone: M2
Owner: processing
Depends: IF-022
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-24, T1-39
Scope: ["@module:processing"]
Contract: Implement Dew Gland and Hearth Lung recipes from renewable native production tables. Biological energy (BE) is a separate bounded store, never synonyms for BU or redstone. Add energy port only with Hearth's first consumer: filtration in the reference loop.
Red: Generator lacking returned-water room cannot consume feed; simulated BE extraction has no side effects; a stopped pump cannot multiply water on restart.
Accept: Three exact 60 s Overworld reference cycles match water/biomass/BE deltas; startup reserves are retained before overflow exports; record measured rates.
Bounds: One batch/organ; BE stored as checked long with declared capacity 100000/Hearth; shared 16 completion budget, no per-tick neighbor search.
Evidence: rules, game

---

IF-024 Ground-based fueling and equipment service
Milestone: M2
Owner: equipment
Depends: IF-023
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: T1-36
Scope: ["@module:equipment"]
Contract: Fuel Papilla is a Capillary Gel mutation of mature Living Substrate, not a freestanding crafted machine. Feed wearer only from attached store and maintain reserve; physical service additions improve rate/efficiency according to catalog. HUD shows per-piece and aggregate fuel with movable half-transparent background and disable option.
Red: No connected supply means no fuel; four pieces cannot each withdraw the full same amount; off-HUD still spends correct biomass; movement off tissue stops fueling.
Accept: Refuel through a level ground cell and through manual ampoule; identify hungry piece in HUD; dedicated server owns all balances.
Bounds: Use 4 wearable supply transactions/player/s and 64/server tick; no scan for nearby tanks.
Evidence: rules, game, visual

---

IF-025 Travel tissue, climbing and controlled damage harvest
Milestone: M2
Owner: ecology
Depends: IF-024
Spec: docs/LIVING_SUBSTRATE_MUTATIONS.md, docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-21, T1-22, T1-23, T1-29
Scope: ["@module:ecology"]
Contract: Implement Digestive Tissue hostile-only damage/biomass, Travel Tissue gated by full qualified armor, Climbing Tendon and Sphincter Door. Read suit capability query; do not duplicate mutation predicates. Door obeys ownership and vanilla collision.
Red: Pets/players are never tissue prey; repeated damage callback cannot duplicate biomass; incomplete suit gets no travel bonus; full output stops harvesting BU.
Accept: Connect a hostile damage floor through 3D vein to reservoir then consumer; movement and door open/closed silhouettes remain readable.
Bounds: <=16 victim candidates/cell query, shared 64 defensive queries/tick; <=1 damage/target/s; no per-victim persistent history.
Evidence: rules, game, visual

---

IF-026 Self-supplying Overworld checkpoint
Milestone: M2
Owner: campaign
Depends: IF-025
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign"]
Contract: Automate the declared reference farm using separate planter/grower/harvester/collector/storage/control operations; validate positive reserves under full-output and chunk pause. Add route visuals and performance counters to reusable lab harness.
Red: Remove one planting reserve or reverse a port and named assertion fails before resource exhaustion; capture verifies pulse direction.
Accept: Run three cycles then restart without external feed; compare serial and parallel layouts for throughput, footprint and BU/item; no economy tuning in this task.
Bounds: <=256 organs fixture, 12000 loaded ticks/run, explicit seed/water reserves and exact conservation.
Evidence: rules, game, visual, soak

---

IF-027 Bounded samples and a separate collection pouch
Milestone: M3
Owner: genetics
Depends: IF-026
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:genetics"]
Contract: Implement I040 sample species/quality records and I012 pouch capacities 16x256, 32x1024, 64x4096 from Items. One death offers one sample to one active destination; vanilla loot remains unchanged. Quality categories store counts, not individual specimen IDs.
Red: Duplicate death dispatch, full pouch, two eligible pouches and a no-loot species cannot duplicate output or fill normal inventory with refused samples.
Accept: Normal kills collect Fragmented samples; configured weapon quality can later replace quality, not quantity. Plant packaging consumes real harvest; vial withdrawal returns actual specimen.
Bounds: 64 collection attempts/server tick; excess discarded with bounded diagnostic; 128 registered species maximum initial catalog; no ambient entity scan.
Evidence: rules, game

---

IF-028 Sample docking, archive and collection links
Milestone: M3
Owner: genetics
Depends: IF-027
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-38, T2-35, T2-36
Scope: ["@module:genetics"]
Contract: Implement Sample Dock, Specimen Archive and Sample Collector as finite counted transfers. Bind one explicit killer and one destination; pouch-to-dock is an explicit operation. Archive max128 species, 16384/species. Paginate species/quality display.
Red: Last-slot competition, oversized NBT, changed ownership, unloaded destination and duplicate death transactions refuse safely without secondary loose drops.
Accept: Move mixed-quality samples pouch -> dock -> archive -> extractor port and back through one vial; counts survive break/reload with no inventory noise.
Bounds: 16 species/page, 48 quality counters/page, 2 changed pages/s; existing collection/supply budgets.
Evidence: rules, game, visual

---

IF-029 Extraction and genome coverage
Milestone: M3
Owner: genetics
Depends: IF-028
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-01, T2-02, T2-03
Scope: ["@module:genetics"]
Contract: Implement R1 Extractor and DNA Bank/Archive Lobe. Coverage required16 plants/32 ordinary/128 complex/1000 bosses; sample quality1/2/4. Consumed sample yields one stock plus coverage; cloned records merge by maximum, not sum. Native species checks live in Genetics eligibility.
Red: Copied records do not double research; a consumed sample cannot be recreated after restart; a wrong native dimension gives a named refusal without spending BU.
Accept: Reconstruct an ordinary genome, inspect partial coverage, merge a copy safely and withdraw stock. Test boss arithmetic at1000 without spawning1000 mobs.
Bounds: Bounded scalar record/species; active extraction one batch, 128 species/bank baseline; existing UI page and recipe budgets.
Evidence: rules, game, visual

---

IF-030 Stock cultivation and multi-species mutation chamber
Milestone: M3
Owner: genetics
Depends: IF-029
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-05, T2-06
Scope: ["@module:genetics"]
Contract: Culture Vat consumes1 stock+2 feed+100BU and30s to produce3 only after complete genome. Mutation Chamber prepares catalog grafts from multiple named species; native bed/organ-level/mutation are independent predicates. Return reusable vials only into reserved space.
Red: Incomplete genome, wrong dimension, missing species or insufficient power each independently refuses unchanged; two users cannot withdraw same last stock.
Accept: Prepare one two-species rank-I graft through actual activation/extraction/culture/chamber recipes; guide explains missing predicates rather than a generic tier lock.
Bounds: One reserved batch/chamber, <=8 distinct species/recipe; shared completion quota.
Evidence: rules, game, visual

---

IF-031 Living tools and permanent Dissector choice
Milestone: M3
Owner: equipment
Depends: IF-030
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:equipment"]
Contract: Implement I015-I018 awakened tools/Fang with separate permanent combat versus Dissector branches. Initial tools use vanilla wooden speed/harvest tier/damage and durability59; living awakening preserves wear, uses G1 fuel10, heals one durability for1BU. Dissector I spider+silverfish gives Intact; II adds enderman fibers for Pristine. No area mining or Fortune multiplication.
Red: Dissector upgrades cannot coexist with combat path or re-roll quality after death; an unqualified tool cannot mine a higher harvest-tier block; no free awakening heal.
Accept: Player chooses a better sample tool at measurable cost of missing combat upgrades; only one sample offer/death; tool identity and wear survive reload.
Bounds: No inventory-wide ticker; equipment checks active hand only; collection uses existing global budget.
Evidence: rules, game, visual

---

IF-032 Independent Carry Sac and pouch socket
Milestone: M3
Owner: portable
Depends: IF-031
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:portable"]
Contract: Implement Carry Sac9/18/27 slots separate from armor; one designated Sample Pouch socket. Reject nested portable storage and storage-bearing third-party items via conservative capability checks. No feeding, flight, reach or armor features included.
Red: Nested Sac, shulker box or portable handler cannot create recursion; rapid swapping and disconnect during move conserve items; Curios absent retains manual pouch use.
Accept: Use all grades while changing armor; pouch collection consumes no general slots; UI shows actual fixed capacity and disables refused insertion.
Bounds: At most27 general stacks plus one pouch; <=4KiB page, no recursive save walking.
Evidence: rules, game, visual, integration

---

IF-033 Per-piece activity ownership and sum-capped learning
Milestone: M4
Owner: equipment
Depends: IF-032
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:equipment"]
Contract: Implement every activity/buff table in Armor Evolution, one owner/piece per event. Each piece has one summed cap; activities cannot pause; at most one non-death point/piece/600 loaded ticks. Map thresholds25/100/300/800/1800 to table buffs; persist earned symbiosis separately.
Red: At capA80+B20=100 neither increments; equipping duplicate pieces cannot double an event; swap/reload cannot reset cadence; a counter reduction cannot undo symbiosis.
Accept: Parameterized tests cover every activity row and threshold; HUD/menu shows piece capacity, competing counters and exact next buff; no player-wide level substitutes for piece state.
Bounds: Four worn pieces and fixed counter enum only; no per-entity activity histories.
Evidence: rules, game, visual

---

IF-034 Paid fusion and irreversible frame trees
Milestone: M4
Owner: equipment
Depends: IF-033
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: T2-31
Scope: ["@module:equipment"]
Contract: Implement Fusion Chrysalis and G1->G2->G3->G4 directed edges exactly as Armor Evolution. Consume prepared bioactive fusion medium, Binder and BU with the listed escalating costs/times; raw ingots/diamonds are rejected. Maintain identity, wear ratio, counters, installed branch and caps.
Red: Backward/sibling transitions refuse; cap increase does not allocate learning points; full output/disconnect cannot clone a frame or charge twice.
Accept: Iron/Auric paths and soft/rigid descendants have exact defense, durability, learning cap and mutation slots; UI previews permanent choice and required prepared inputs.
Bounds: One piece and one reserved fusion transaction/Chrysalis; checked integer BU arithmetic.
Evidence: rules, game

---

IF-035 Graft installation and first survival mutations
Milestone: M4
Owner: equipment
Depends: IF-034
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: T2-09
Scope: ["@module:equipment"]
Contract: Grafting Bench installs only legal target/family/rank successors; no graft extraction or branch swapping. Implement host-accessible M1/M2/M3 and H1-H4 ranks available before native materials, using armor tables; gate later ranks by recipes, not duplicate item identities.
Red: Wrong slot, missing predecessor, mutually exclusive branch and insufficient slots refuse without consuming graft; repeated confirmation cannot install twice.
Accept: Early self-healing, basic protection, lighting and breathing draw actual fuel; UI shows exact cost/benefit and permanent exclusions; advanced ranks remain discoverable but unavailable.
Bounds: At most catalog family/rank slots per piece; action query returns immutable capability snapshot.
Evidence: rules, game, visual

---

IF-036 Fuel priority, hunger, symbiosis and feeding branches
Milestone: M4
Owner: equipment
Depends: IF-035
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: T2-08
Scope: ["@module:equipment"]
Contract: Implement self-healing, Healing Dock and fuel priority/starvation from Armor Evolution; hunger has one wearer cadence and one-heart floor until that piece earns100 learning. C12 intake consumes configured containers; C13 Digestive Crop consumes only listed bio-materials using prepared grafts. Include C6 Gill Bellows air pulses before burrowing is enabled; H4 is underwater vision, not breathing.
Red: Four hungry pieces cannot bypass health floor; armor hunger neither kills nor heals; external damage remains lethal. Empty automatic source stops features rather than creating fuel.
Accept: Manual feed, Papilla and Dock share Equipment service; internal tanks differ by frame/mutations; C13 field production matches capped table rather than a portable factory.
Bounds: 4 supply transactions/player/s and64/server tick; inspect configured slots only; one hunger cadence/player.
Evidence: rules, game, visual

---

IF-037 Partial learning reduction and death rescue
Milestone: M4
Owner: equipment
Depends: IF-036
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: T4-09, T4-20
Scope: ["@module:equipment"]
Contract: Implement Trait Regulator ritual removing25 points from one selected eligible counter for catalog BU/XP/ingredients, never negative; Mnemonic Vessel stores explicit raw XP transfers. C-D actual-death learning unlocks one rescue; sleeping recharges rescue without clearing learned counters.
Red: Insufficient points/XP/output room, simultaneous deposit+ritual, logout and repeat confirmation cannot duplicate XP/charges; prevented death earns no actual-death count.
Accept: Reduce unwanted points and let another activity fill freed capacity; rescue fires once and remains spent after reload until qualifying sleep.
Bounds: One reservation per piece/Vessel, stored XP capped at1000000 raw points; no orb scans or spawned XP.
Evidence: rules, game

---

IF-038 Work Bed and one-block extraction presentation
Milestone: M4
Owner: mineral
Depends: IF-037
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-32, T2-32
Scope: ["@module:mineral"]
Contract: Implement Work Bed as exclusive held workpiece plus stage, not a duplicate world ore. Grasping Root moves exactly one existing eligible block to one bed then stops. Block entities, protected blocks and unsupported modded ores refuse. Vanilla placing a real ore on a bed remains possible.
Red: Two roots cannot own one bed; interruption between removal/presentation leaves exactly one authoritative workpiece; full bed never deletes source.
Accept: Manually place and root-present iron/gold/copper ore; comparator states distinguish ready/held/blocked; no conversion/smelting in Root.
Bounds: One workpiece/bed, one loaded source lookup/start,16 world extraction commits/server tick.
Evidence: rules, game, visual

---

IF-039 Reaction, fracture and separate collection
Milestone: M4
Owner: mineral
Depends: IF-038
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-33, T2-34, T1-33
Scope: ["@module:mineral"]
Contract: Reaction Polyp treats a held block once using prepared penetrant; Fracture Jaw consumes the final workpiece stage into retained fragments; Collection Cilia alone moves retained output to storage. Honor stable interlocks; no loose automatic item spawning.
Red: Jaw started before treatment-complete refuses; repeated treatment cannot multiply yield; a collected workpiece cannot be restored as a fresh ore block.
Accept: Three independently controlled organs execute one bed cycle and pause safely at any blocked downstream stage; vanilla hopper/manual collection remains possible.
Bounds: One reserved target/operation;16 extraction/fracture commits/server tick;9 retained stacks/organ.
Evidence: rules, game, visual

---

IF-040 Dust production, retained fractions and basic smelting
Milestone: M4
Owner: mineral
Depends: IF-039
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-23, T2-24
Scope: ["@module:mineral"]
Contract: Implement Mineral Gizzard, Washing Kidney and retained tailings. Start iron/gold/copper at4 dust/raw ore and4 dust/ingot; define permitted higher paths in recipe tables from Items, never automatic multiply then apply vanilla Fortune. Dust fragments remain items/storage until a complete recipe fits.
Red: All routes reject reprocessing treated intermediates as pristine ore; incomplete dust batches are retained; swapping recipes cannot erase fractional progress.
Accept: Compare raw ore/direct furnace, simple dust and washed chain using exact resource ledgers; guide/JEI expose total recovery and waste, not misleading per-stage multipliers.
Bounds: One batch/processor; finite9-slot waste/output buffers; no floating-point production amounts.
Evidence: rules, game

---

IF-041 Manual leaching with ground-level boundaries
Milestone: M4
Owner: excavation
Depends: IF-040
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T0-14, T0-15
Scope: ["@module:excavation"]
Contract: Implement Leaching Gland/I008 on one exposed3x3 eligible host-rock face. Replace only allowed non-ore natural rock with weak translucent Leached Rock carrying original state; manual pick remains required. Exclude ores, inventories, protected blocks and fluids. Boundary selection uses visible mutated substrate, no above-ground posts.
Red: Ore and block-entity fixtures remain byte-for-byte unchanged; duplicate dose cannot farm rock drops; unload/reload cannot lose original recovery state.
Accept: Walk showcase, see ore through treated rock, mine more quickly with reduced durability cost and actual retained host drop. Original host light/visibility handled client-side without x-ray through untouched terrain.
Bounds: 9 target cells/dose,16 replacement commits/server tick; <=4096 active leached cells/chunk, encoded state allowlist.
Evidence: rules, game, visual

---

IF-042 Specialist armor and hand-worked industry checkpoint
Milestone: M4
Owner: campaign
Depends: IF-041
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign"]
Contract: Add Survival scenarios for two different armor lineages, paid partial learning removal, safe hunger/refueling and manual leaching -> bed -> reaction -> fracture -> dust -> ingot. Use guide-discovered recipes without unlock commands.
Red: Disable a learned buff or allow a sibling fusion and tests fail at exact step; full waste store halts refinery without deleting ore.
Accept: Measure consumables, armor BU and retained dust for lean versus recovery-focused layouts; all required M4 guide nodes correspond to real interactions.
Bounds: Finite <=600 actions; fixed fixture inventory and world ore ledger; no worldgen ore scan.
Evidence: rules, game, visual

---

IF-043 Thermal ground and local Nether cultivation
Milestone: M5
Owner: cultivation
Depends: IF-042
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T3-01, T3-02, T3-17, T3-18
Scope: ["@module:cultivation"]
Contract: Implement Thermal Substrate/Nursery/Root/Cultivation using native bed contracts. Root supplies the reference powered grower; thermal fruit needs real seed, feed and local water production. Specialized soil is created only in actual Nether; imported soil does not spoof dimension.
Red: Copied bed layout in Overworld refuses native recipe without consuming materials; first thermal feed does not require a Wither genome or advanced fusion it enables.
Accept: Build the initial3x3 thermal bed and complete local fruit harvest; useful flora remains part of the base. Structure UI names missing bed cell versus missing energy.
Bounds: Existing64-cell cultivation/4096 structure ceilings and shared probe budgets.
Evidence: rules, game, visual

---

IF-044 Lava intake and reusable heat generation
Milestone: M5
Owner: thermal
Depends: IF-043
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T3-03, T3-04, T3-09
Scope: ["@module:thermal"]
Contract: Implement Lava Siphon, Steam Heart and Thermal Mantle as separate intake/generation/application operations. Lava must be removed from a real loaded source or supplied by a real tank. Bind temperature/pressure to ThermalProcess state; recipes reserve water, steam and condensate capacity.
Red: No water, full steam store or removed heat face stops start without disappearing lava; unloading pauses work rather than accumulating unlimited heat catch-up.
Accept: Produce heat/steam and manufacture Thermal Lining through its actual recipe; unsafe temperatures are visible before damage; no mineral output from empty space.
Bounds: One source cell probe/start,16 thermal steps/server tick, one batch/core; max100000 checked pressure units/process baseline.
Evidence: rules, game, visual

---

IF-045 Steam transport, pressure storage and relief
Milestone: M5
Owner: thermal
Depends: IF-044
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T3-05, T3-06, T3-07, T3-08
Scope: ["@module:thermal"]
Contract: Steam Vein uses typed RouteService payload; Pressure Vesicle stores finite steam/pressure; Relief Chimney dissipates metered steam; Condenser returns actual cooling output. Pressure state is thermal-owned, not a generic item-pipe field.
Red: Closed outlet/full condenser raises specified pressure but never routes through closed face; unloaded relief cannot be treated as open; simulation never spends steam.
Accept: Build bypass/relief/condensation loops with visible direction and warning states; normal full uncharged biomass storage still merely refuses.
Bounds: Existing routing and thermal quotas; finite16000 mB steam/Vesicle; relief processes one reserved dose/step.
Evidence: rules, game, visual

---

IF-046 Mechanical work and thermal material processing
Milestone: M5
Owner: thermal
Depends: IF-045
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T3-10, T3-13, T3-14, T3-15
Scope: ["@module:thermal"]
Contract: Steam Muscle consumes steam to expose one bounded work impulse; Crucible performs one digestion reaction; Heat-Exchange Gill exchanges real process heat with cooling fluid; Distillation Crown separates reserved products. No automatic extraction, fuel production or free cooling side jobs.
Red: Work impulse without steam fails; cooling output-full refuses transfer; distillation never emits only valuable output while discarding unreserved waste.
Accept: Create thermal bioactive fusion medium and cooling products from Items; drive one existing processor with Steam Muscle via a port, not direct internals.
Bounds: One impulse or batch/core, shared16 thermal steps/tick;9 retained slots and2 finite tanks/organ.
Evidence: rules, game, visual

---

IF-047 Nether-native extraction and stock culture
Milestone: M5
Owner: genetics
Depends: IF-046
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:genetics"]
Contract: Enable Nether-class genetics only on real Nether3x3 thermal beds with Extractor/Vat L1 and2 Thermal Linings plus200BU native surcharge. Species class derives from the registered sample, not current loot name. Ordinary enderman remains Overworld-processable; Wither is Nether-class.
Red: Individually remove dimension/bed/level/lining/power and each refuses unchanged; migrating a bank does not make an Overworld extractor Nether-native.
Accept: Process blaze and Wither samples and culture reconstructed stock in Nether; completed research remains portable while actual production remains location-bound.
Bounds: Use existing one-batch genetics and bounded structure checks; no dimension lookup creates tickets.
Evidence: rules, game

---

IF-048 Thermal armor branches and advanced fusion materials
Milestone: M5
Owner: equipment
Depends: IF-047
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:equipment"]
Contract: Implement all Nether-accessible mutation ranks and rigid frame fusion recipes from Armor Evolution, including independent heat/fire/lava capabilities and listed incompatibilities. Thermal set support is a capability query, not an invulnerability flag.
Red: Lava drains exact BU and kills an unfunded unprotected wearer normally; fire resistance does not imply underwater breathing or arbitrary damage immunity.
Accept: A thermal-specialist suit completes a measured work window and refuels at Nether Papilla; mixed generalist pieces cannot equal every dedicated set.
Bounds: Fixed4-piece evaluation and listed upkeep cadence; no chunk tickets or extra armor inventory ticker.
Evidence: rules, game, visual

---

IF-049 Nether base sustains itself and exports
Milestone: M5
Owner: campaign
Depends: IF-048
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign"]
Contract: Automate three native60s reference cycles starting from the finite Items kit. Use exact recipe-table durations and actual deltas, including6000BE filtration charge, planting/resin reserves and1500 netBU/declared cycle. Export only after local restart reserves.
Red: Disconnect Overworld supply before start; remove local seed/water reserve and restart assertion fails. No creative refueling after initial kit.
Accept: Nether manufactures Thermal Lining, DNA stock and surplus fuel locally, restarts after save/reload and outproduces declared Overworld reference at stated cost/footprint.
Bounds: <=512 organ fixture, finite24000 tick deadline; measure shared-budget throttling separately from recipe arithmetic.
Evidence: rules, game, visual, soak

---

IF-050 Electrical generation, conduction and exchange
Milestone: M6
Owner: energy
Depends: IF-049
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T4-01, T4-02, T4-03, T4-04
Scope: ["@module:energy"]
Contract: Electrocyte Stack generates recipe-defined BE; Conductive Tissue and Charge Sac move/store it using typed ports. Exchange Organ converts BE<->external FE only at explicit1:1 baseline with no loop gain; core has no NeoForge classes.
Red: Round trip through two exchange organs cannot increase either balance; full Charge Sac and unsupported FE handler refuse; absent external mods leave native loop functional.
Accept: Supply a real processor through tissue and through a standard FE test handler; charging requires actual fuel/cooling; UI distinguishes BE, BU and steam.
Bounds: <=64 energy commits/server tick under shared transfer admission; checked capacity1000000BE/Sac.
Evidence: rules, game, integration

---

IF-051 Independent recovery and smelting efficiency paths
Milestone: M6
Owner: mineral
Depends: IF-050
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T4-05
Scope: ["@module:mineral"]
Contract: Ion Separator recovers retained tailings through recipe-defined chemistry/BE. Complete4->5->6->7->8 dust recovery and4->3->2->1 dust/ingot branches from Items as separate installed services; every stronger recipe consumes its listed additional reagents/energy.
Red: Unsupported metal recipes cannot inherit iron multipliers; combining max paths pays both costs; raw ore/Fortune/workpiece routes cannot double-roll loot.
Accept: Measured base and max-path ingot yields exactly match ledger; intermediate layouts can save dust without buying every improvement; JEI lists waste and all energy.
Bounds: One batch/Separator, finite waste slots; same16 completion quota; integer ratios only.
Evidence: rules, game

---

IF-052 R2 precision sequencing and cooling service
Milestone: M6
Owner: genetics
Depends: IF-051
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-04, T4-07, T4-19
Scope: ["@module:genetics"]
Contract: Implement Sequencing Lens/Precision Sequencer/Cold Lobe and R2: Extractor L2 plus lens/precision service yields quality*4 coverage with additional100BU+2000BE/sample. Stock yield remains1; cooling material is consumed by actual recipe, not presence of a decorative block.
Red: Removing one precision component mid-batch cannot grant boosted coverage for R1 cost; high quality is not multiplied into extra free stock.
Accept: An existing L2 Extractor upgrades in place with history retained; same sample completes more coverage at measured higher expense; dismantling precision service preserves held batch safely.
Bounds: Bounded structure query and existing genetics budgets; one active precision job/service.
Evidence: rules, game

---

IF-053 Bounded sequencing and readable production displays
Milestone: M6
Owner: control
Depends: IF-052
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T4-10, T4-11
Scope: ["@module:control"]
Contract: Scheduler Ganglion executes <=16 steps containing wait-state/start/ack/branch and optional bounded delay; no arbitrary script language. Display Membrane shows paged supplied readings. Schedule uses stable completion IDs, not redstone tick timing.
Red: Cyclic schedule yields after each step; held start never creates duplicate work; unload/reload resumes waiting on same batch, not replaying committed conversion.
Accept: Configure two alternative ore layouts, one shared processor and one parallel line; both remain reliable under changed processing speed.
Bounds: 1 schedule step/Ganglion/tick,64/server tick; <=16 steps and8 local bindings; display2 changed pages/s.
Evidence: rules, game, visual

---

IF-054 Surveyed ground and finite vertical shaft
Milestone: M6
Owner: excavation
Depends: IF-053
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-21, T2-22
Scope: ["@module:excavation"]
Contract: Surveyed Tissue is ground-level selection, not border posts. Descending Rootstock plans one finite walkable vertical section with continuous Climbing Tendon access and Lumen inserts; it requests existing cutters/collectors and never smelts. Initial section3x3 footprint,16 blocks deep,1 retained entrance.
Red: Unloaded boundary, fluid, protected block, missing light/access supply or full output pauses before unsafe cut; a second start cannot duplicate the section.
Accept: Player descends, climbs out, lights shaft and starts next one-shot section from a reachable landing. Show actual removed blocks and access costs.
Bounds: One <=144-cell section/core; <=16 excavation world changes/server tick shared across all mines; one retained plan/core.
Evidence: rules, game, visual

---

IF-055 Descending walkable galleries and modular boring
Milestone: M6
Owner: excavation
Depends: IF-054
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T3-11, T3-12
Scope: ["@module:excavation"]
Contract: Descending Cradle plans 3-wide stairs dropping one block per forward step, with4-block headroom and16-step finite length; Boring Jaw removes only its assigned real face. Insert paid stair/light material rather than spawned surface posts; control shares existing Bed/Root operations.
Red: A cavity below next stair, missing support or lava ahead stops safely; two cutters cannot claim same block; light/refill costs remain conserved.
Accept: Walk down and back without jumping or suffocating; rotate a fresh section90 degrees from a landing; demonstrate manual pause/reload/continue.
Bounds: <=256 planned cells/core;16 global excavation edits/tick includes stairs and lights; loaded targets only.
Evidence: rules, game, visual

---

IF-056 Deep surface strips and spoil routing
Milestone: M6
Owner: excavation
Depends: IF-055
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T4-13, T4-14
Scope: ["@module:excavation"]
Contract: Strata Maw plans a1-block-wide,16-long strip from surface down to selected depth up to64; begins at Climbing Tendon entry. Spoil Sorter routes extracted stone/ore independently, with no free ore conversion. User extends by building another one-shot section.
Red: Stop at bedrock, fluid, world floor, protection or full spoil; changing selected depth mid-work cannot skip paid excavation or lose reservations.
Accept: Inspect lit open strip from surface and climb to lowest landing; compare shaft/stairs/strip footprint, speed and BU cost using same ore ledger.
Bounds: <=1024 planned cells/core;16 shared world edits/tick;9-stack sorter buffer; bounded indexed plan, no flood fill.
Evidence: rules, game, visual

---

IF-057 Finite biomass spills and reusable recovery
Milestone: M6
Owner: containment
Depends: IF-056
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T4-15, T4-16, T4-17
Scope: ["@module:containment"]
Contract: Implement Recovery Sump, Neutralization Gland and Pressure Regulator with catalog containment rules. Enable intentional I019 placement only through finite spill admission. Ordinary full tanks refuse; only specified charged/damaged process ruptures. Excess volume remains in breached vessel.
Red: At64 jobs/16 probes budget bucket refuses unchanged; secondary casualty cannot spawn another spill job or item storm; unload pauses10s damage without catch-up.
Accept: Recover a dose before tissue death, isolate a blocked pressure process and neutralize retained waste. Warn before rupture; finite quantity never becomes a renewable source.
Bounds: 64 cells/spill at250mB, one neighbor probe/job/tick under16/server tick,64 jobs; secondary fluids join same bounded spill.
Evidence: rules, game, visual, soak

---

IF-058 Excavation, refinery and containment checkpoint
Milestone: M6
Owner: campaign
Depends: IF-057
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign"]
Contract: Run shaft/stairs/strip fixtures through real tool configuration and supplied machines; demonstrate all three using same quarry ledger and independent dust/smelting upgrades. Include shared-load spill case and ore resource conservation.
Red: Reverse a collection port or unload exit chunk: scenario must pause with recoverable held state, not finish on time alone.
Accept: Player can explore every cut, extend a section and route ore through an efficient controlled workshop; spill recovery works at admission limit; profile loaded probes/edit counts.
Bounds: At most8 simultaneous mines in fixed test volume;24000-tick bound, test16-edit shared cap under contention.
Evidence: rules, game, visual, soak

---

IF-059 End anchoring and first local nursery
Milestone: M7
Owner: spatial
Depends: IF-058
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T5-01, T5-02, T5-03, T5-04
Scope: ["@module:spatial"]
Contract: Implement Anchored Substrate, Spatial Nursery, Anchor Root and Void Tether using real End bed eligibility. First spatial materials require End resources and power but no reconstructed Shulker genome or R3 service. Void Tether prevents only the specified local fall, not universal flight.
Red: Overworld replica refuses native operation; missing anchor/energy cannot satisfy bed; first nursery recipe graph has no shulker-R3 cycle.
Accept: Build initial End3x3 bed, grow a native substrate cell and recover from a bounded fall using charged Tether; recipe and guide prerequisites agree.
Bounds: <=64 assigned cells/anchor under shared structure probes; one wearer tether action/player/tick; no chunk creation.
Evidence: rules, game, visual

---

IF-060 Chorus orchards and renewable End power
Milestone: M7
Owner: cultivation
Depends: IF-059
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T5-05, T5-08
Scope: ["@module:cultivation"]
Contract: Chorus Resonator and Orchard Tissue implement native reference cultivation: real planting stock, local Dew water, powered growth, separate harvest and generation. Outputs remain physical fruit/stock, not spontaneous biomass.
Red: No End feed without seed/water/BE; repeating harvest callback cannot award a second fruit allocation; Overworld dimension refuses native recipe.
Accept: Complete three120s reference cycles, retaining planting/restart kit and matching2300 netBU and26000 netBE reference surplus per declared cycle before transport.
Bounds: Existing shared cultivation/recipe budgets; explicit fixed plot and finite reserve buffer.
Evidence: rules, game, visual

---

IF-061 Spatial conditioning and levitation work service
Milestone: M7
Owner: spatial
Depends: IF-060
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T5-06, T5-07, T5-12
Scope: ["@module:spatial"]
Contract: Spatial Conditioner creates bioactive spatial intermediates; Levitation Chamber supplies paid recipe service; Phase Isolator bounds phase process to formed structure. No free cargo teleport or player flight in these organs.
Red: Missing phase isolation, native bed, coolant or energy independently refuses unchanged; unloaded service cannot count as formed.
Accept: Manufacture first Spatial Fiber and eligible G4 fusion materials without R3; physical components expose state through existing UI/control interfaces.
Bounds: One service reservation/core,4096-cell structure max under shared probes; no tickets.
Evidence: rules, game, visual

---

IF-062 End-native research and R3 sequencing
Milestone: M7
Owner: genetics
Depends: IF-061
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:genetics"]
Contract: End native Extractor/Vat require End3x3 bed+4 anchors, L2,2 Spatial Fibers and400BU native surcharge. R3 requires Extractor L3, ion/levitation/cold services, extra400BU+16000BE+1 shulker stock+100mB cooling/sample, quality*8 coverage and1 stock yield.
Red: R2 can reconstruct first Shulker without already requiring shulker stock; removing a service cannot keep R3 yield with R1 cost; portable genome never bypasses native culture location.
Accept: Demonstrate first-shulker -> cultured stock -> R3 upgrade sequence and restart with local supplies; Wither/Enderman classifications remain unchanged.
Bounds: Existing bounded genotype arrays, one job/core and fixed local structure inspection.
Evidence: rules, game

---

IF-063 Burrowing without flight or terrain loss
Milestone: M7
Owner: equipment
Depends: IF-062
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:equipment"]
Contract: Implement H8/C8/L7/B8 full-set burrowing and all ranks per armor contract: stationary window10/20/35s, speed0.8/1.2/1.6m/s, cost24/20/18BU/s. Traverse only ground-supported eligible solid volume; original blocks are not mined. H2 lighting and C6 breathing are separate requirements. No permanent disable after entrapment.
Red: Switch spectator->creative->survival, unload/rejoin or stop underground: ability follows cooldown/fuel, not stale disabled flag; air outside terrain cannot grant flight; protected blocks remain impassable.
Accept: Travel up/down/diagonally inside rock; stationary expiry restores collision and danger; client shadows show boundary without seeing untouched distant ores. Terrain compares identical after use.
Bounds: At most27 local collision probes/player/tick and256/server tick; no world block replacement queue; snapshot sync only on capability transitions.
Evidence: rules, game, visual

---

IF-064 Elytra fusion and bounded advanced movement
Milestone: M7
Owner: equipment
Depends: IF-063
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: T3-16
Scope: ["@module:equipment"]
Contract: Implement C11 wing tree using consumed real Elytra, mutually exclusive C8 burrowing branch; ranks use2/6/10BU/s and6/8/10m/s table caps. Complete eligible leggings/boots movement branches and Launch Bellows as paid physical launch support. Fallback follows armor table, not unconditional hover.
Red: No Elytra means no fusion; unfunded wing cannot continue powered flight; multiple speed sources obey movement ceilings; no sprint/launch stacking exploit.
Accept: Launch, glide/land and refuel in End; compare wings versus burrow specialist route; capture back model unfolded/folded and player skin openings.
Bounds: 4-piece fixed capability evaluation; no chunk tickets, client particles distance-capped.
Evidence: rules, game, visual

---

IF-065 Independent End settlement checkpoint
Milestone: M7
Owner: campaign
Depends: IF-064
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign"]
Contract: Survival action fixture starts with finite End kit, constructs local powered food/water/fuel chain, reconstructs first Shulker, cultures stock and enables R3. Separately equip burrow and wing branches; no free switching of same piece.
Red: Block Overworld/Nether supply after initial kit; omit one native prerequisite and progression names exact blockage; no R3 bootstrap bypass.
Accept: Three local cycles plus save/restart and a paid export leave restart reserve intact; two traversal builds complete different practical routes.
Bounds: <=512 organs and24000 ticks per run; probe/network/fuel ledgers captured.
Evidence: rules, game, visual, soak

---

IF-066 Safe passenger links between physical endpoints
Milestone: M8
Owner: transit
Depends: IF-065
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T6-01, T6-02
Scope: ["@module:transit"]
Contract: Transit Maw/Arrival Chamber link explicit owned endpoints and move one player only after loaded destination, safe volume and prepaid local charge validate. Link is base infrastructure, not inventory teleport; no implicit chunk loading.
Red: Unsafe exit, missing destination, ownership change, concurrent departure or insufficient charge refuses with player/source unchanged.
Accept: Walk through links across all three dimensions; arrival rotation and clearance are consistent; source/destination costs occur once; client animation reflects committed transit.
Bounds: <=16 registered links/team and256/server baseline; one traversal/player/s,16/server tick; loaded endpoint lookup only.
Evidence: rules, game, visual

---

IF-067 Freight with crash-safe cargo ownership
Milestone: M8
Owner: transit
Depends: IF-066
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T6-03, T6-04
Scope: ["@module:transit"]
Contract: Freight Gullet/Cargo Lock move bounded real items, fluids or BE through explicit links. Persistent transfer ID and state hold exactly one authoritative escrow until destination acknowledges; no distributed global inventory illusion. Native production checks stay where recipe runs.
Red: Crash/restart before and after source debit/destination acceptance/ack; both unloads, full destination and changed ownership never duplicate or delete escrow.
Accept: Pipe actual Nether/End exports to Overworld reserve and actual intermediates between native labs; cancel untouched reservation safely; committed cargo remains recoverable.
Bounds: <=8 escrow transfers/link,256/server; payload<=9 stacks or16000mB or100000BE;16 freight commits/server tick.
Evidence: rules, game, integration

---

IF-068 Bounded storage index and explicit requests
Milestone: M8
Owner: indexing
Depends: IF-067
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T4-12, T6-05, T6-06, T6-07
Scope: ["@module:indexing"]
Contract: Storage Cortex indexes loaded registered stores via immutable summaries; Request Cortex/Workshop Interface reserve actual ingredients and issue existing route jobs. Relay Ganglion relays bounded signals across freight links. No item creation or instantaneous all-world inventory scan.
Red: Two requests for final stack cannot both reserve; stale index or unloaded store refuses without negative count; query cannot load chunks.
Accept: Crafting request consumes reachable real contents; view reports unavailable remote inventory rather than pretending empty; base priority/reserves still apply.
Bounds: <=256 stores/Cortex,16 summary updates/server tick,16 rows/page;<=32 pending requests/Cortex and256/server; bounded stale entries pruned on deregistration.
Evidence: rules, game, visual

---

IF-069 Growth, relocatable organs and service foundations
Milestone: M8
Owner: construction
Depends: IF-068
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-20, T6-08, T6-09, T6-10
Scope: ["@module:construction"]
Contract: Structure Grower/Foundation Cyst build paid selected finite patterns from visible markers; Service Pedestal binds existing services; Reclamation Mouth recovers supported installed parts without resetting OrganHistory. Growth never copies another core's identity.
Red: Insufficient materials, protected/unloaded cell or blocked pattern pauses before placement; moving leveled core cannot yield both original and replacement.
Accept: Expand a workshop with a side automation bay and move a leveled organ to a new base; each removed cell/material accounted; no auto-resume construction in unloaded chunks.
Bounds: <=4096 planned cells/job,16 world placements/server tick shared with excavation; one retained job/core.
Evidence: rules, game, visual

---

IF-070 Cocoon, Recall and Death Bond preservation
Milestone: M8
Owner: equipment
Depends: IF-069
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: T6-11
Scope: ["@module:equipment"]
Contract: Implement permanent M5->M6 or M7 branches and Recall Nest4 berths. Default armor remains ordinary despawning loot. M5 loaded despawn/fire protection, M6 prepaid20/15/10kBU recall and M7 prepaid50/35/25kBU bond per piece follow armor rules; stored charges are not spendable tank fuel.
Red: Death + logout + respawn/restart and full/unloaded berth leave exactly one owner per piece; no duplicate grave/drop/equipped/bond item. Default armor is not protected for free.
Accept: Pay charges at base, die and observe each branch's exact result; fallback and cooldown visible; partial suit protection never retains unrelated inventory.
Bounds: At most4 pieces/death and4 pending bonded slots/player; one loaded lookup/piece; no recall retry queue or tickets.
Evidence: rules, game, integration

---

IF-071 Three-base cargo and recovery checkpoint
Milestone: M8
Owner: campaign
Depends: IF-070
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign"]
Contract: Two-client packaged fixture performs simultaneous cargo requests, endpoint unload/reload, mid-transfer stop/restart, armor recall and bond death. Record resource ownership at each durable transition.
Red: Inject stop at every escrow boundary and assert exactly-once recovery; an unsafe player endpoint cannot consume travel charge.
Accept: Actual native production in Nether/End supplies Overworld through freight, not creative stock; no unauthorized chunk tickets; normal death drops still despawn.
Bounds: <=16 links and256 stores fixture; finite fault matrix, no network retries outside declared escrow cap.
Evidence: rules, game, integration, soak

---

IF-072 Aquatic cultivation and finite fishing
Milestone: M9
Owner: husbandry
Depends: IF-071
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-12, T2-28
Scope: ["@module:husbandry"]
Contract: Aquaculture Bed grows allocated aquatic harvest from real starter/feed; Fishing Polyp performs a paid fishing operation only against valid loaded water. Use declared loot tables with explicit nonrenewable exclusions; no AFK loose-item stream.
Red: Invalid water, no bait, full result or duplicate completion cannot produce loot; a sealed one-cell puddle cannot impersonate valid fishery.
Accept: Compare crop and aquatic protein supply for DNA culture; harvest routes through ordinary Cilia/storage; water layout affects valid work area.
Bounds: <=64 assigned water cells/bed,4 probes/organ/s under cultivation quota;9 retained stacks; one catch/30s baseline.
Evidence: rules, game, visual

---

IF-073 Feeding, milking and animal incubation operations
Milestone: M9
Owner: husbandry
Depends: IF-072
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-25, T2-26, T2-27
Scope: ["@module:husbandry"]
Contract: Implement Feeding Trough, Milking Lobe and Incubation Basket as separately assigned animal jobs. Real food/empty containers are reserved; no free births or DNA extraction from living animals. One animal may have one husbandry reservation.
Red: Two milkers cannot use same reserved bucket/action; full baby berth prevents breeding consumption; animal death/unload clears reservation without a permanent UUID list.
Accept: Build separate cow/poultry supply lines with explicit assigned pens; retain milk bottles/eggs before exporting; ordinary player husbandry still works.
Bounds: <=16 assigned animals/station,64 husbandry checks/server tick; <=8 occupied incubator berths; no ambient whole-chunk mob scan.
Evidence: rules, game, visual

---

IF-074 Food and potion preparation
Milestone: M9
Owner: nutrition
Depends: IF-073
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-10, T2-11, T4-08
Scope: ["@module:nutrition"]
Contract: Ration Kitchen cooks real ingredients; Brewing Gland uses vanilla potion inputs/containers; Potion Infuser prepares the listed biological carriers. Keep food, player healing, armor feeding and biomass digestion distinct. All returned bottles reserve space.
Red: Potion effect NBT cannot inject arbitrary effects; food is not simultaneously eaten and digested; incompatible carriers preserve ingredients.
Accept: Produce food for C9 and serum for H5 using separate supply lines; vanilla brewing remains usable; JEI shows effect duration and output container.
Bounds: One batch/core,9 inventory slots; use whitelist recipe effects, no custom unbounded potion arrays.
Evidence: rules, game, visual

---

IF-075 Sensory and chemical-defense mutation ranks
Milestone: M9
Owner: equipment
Depends: IF-074
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:equipment"]
Contract: Complete H1/H3/H4/H5/H7/H9/H10 rank effects from Armor Evolution through the shared equipment capability and cooldown engine. H2/H6/H8 remain owned by lighting/thermal/burrowing implementations; exercise their compatibility without reimplementation.
Red: Filtered effect removal cannot exceed current duration; sensory overlays cannot reveal unseen inventories or ore; switching view mode cannot refund active upkeep.
Accept: Every listed rank has exact range, fuel, cooldown, slot/prerequisite and visible effect tests; inspect overlays in water, darkness and smoke. No always-on wall-through mob models.
Bounds: <=16 entities returned/query,64 sensory queries/server tick; fixed per-wearer cooldowns; no entity history collections.
Evidence: rules, game, visual

---

IF-076 Chest support, feeding and service mutation ranks
Milestone: M9
Owner: equipment
Depends: IF-075
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: T2-29
Scope: ["@module:equipment"]
Contract: Implement C1/C2/C3/C9/C10 and Restorative Tissue through the existing equipment service and fuel-priority interfaces. Capacity is not fuel generation; C3 heals the player while M1 heals armor; food reserve wins a same-slot race with C13.
Red: C9 and C13 cannot eat the same item; healing stops at full health and respects recent hostile damage; full recipient tool stops service transfer.
Accept: Compare manual food, automatic food and digestive fuel supplies with exact budgets, all ranks and starvation behavior; show selected slots and rates in menu.
Bounds: Fixed worn-piece state and shared equipment transaction/sensory budgets; no per-item inventory ticker.
Evidence: rules, game, visual

---

IF-077 Defensive cavity and shared armor graft ranks
Milestone: M9
Owner: equipment
Depends: IF-076
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:equipment"]
Contract: Complete M1-M4 ranks and C4/C7 alternatives from Armor Evolution. Residual physical/explosion reduction is capped20% with row-specific per-hit limits; cannot install both inflatable cavity plans.
Red: Vanilla protection applied before residual mutation reduction; zero damage spends no prevention fuel; C4/C7 sibling installation refuses unchanged.
Accept: Assert every rank's durability healing, prevention amount, fuel and metabolic load; mixed suits never bypass cumulative ceilings.
Bounds: Fixed worn-piece state and shared equipment transaction/sensory budgets; no per-item inventory ticker.
Evidence: rules, game, visual

---

IF-078 Leggings work and movement specialization ranks
Milestone: M9
Owner: equipment
Depends: IF-077
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:equipment"]
Contract: Complete L1-L6/L8 rank effects from Armor Evolution; L7 burrowing remains an existing owner. Use actual sprint/swim/work activity events and shared movement ceilings, never new parallel attribute bookkeeping.
Red: Attribute reapplication on swap/reload cannot accumulate speed; unpaid active work bonus stops; incompatible route is refused.
Accept: Parameterized tests exercise each listed rank, vanilla effect stacking and frame slot restrictions; capture sprint/swim/crouch movement.
Bounds: Fixed worn-piece state and shared equipment transaction/sensory budgets; no per-item inventory ticker.
Evidence: rules, game, visual

---

IF-079 Boots contact and landing specialization ranks
Milestone: M9
Owner: equipment
Depends: IF-078
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:equipment"]
Contract: Complete B1-B7 rank effects from Armor Evolution; B8 remains the existing burrowing owner. Separate contact/hot-floor protection, jump, landing and displacement actions; all fuel spending uses one equipment transaction.
Red: Landing fires once per actual landing; repeated collision callbacks cannot generate biomass or learning; no fall-reset infinite flight.
Accept: Test every rank on stairs, water, magma and controlled falls; compare rigid versus flexible frames and ensure exact speed/height ceilings.
Bounds: Fixed worn-piece state and shared equipment transaction/sensory budgets; no per-item inventory ticker.
Evidence: rules, game, visual

---

IF-080 Nursery, larva berths and bounded worker lifetime
Milestone: M9
Owner: helpers
Depends: IF-079
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-13, T2-14
Scope: ["@module:helpers"]
Contract: Implement Brood Nursery with3 distinct collision-free larva berths and Worker Waypoint. Mutation consumes a larva and real graft to create one assigned helper. Population reservations release on death, expiry, unload and station removal; idle workers return to assigned berth.
Red: Kill all workers then hatch again: slots free correctly. Occupied berth cannot spawn overlapping larvae; simultaneous hatch requests cannot exceed3.
Accept: Original bug-shaped models walk/climb ordinary stairs; Probe reassigns a loaded compatible waypoint and home nursery; no teleport across unloaded ground.
Bounds: <=3 workers/Nursery,24/team and96/server baseline; spawn only with free valid berth; one fixed home/task record/worker.
Evidence: rules, game, visual

---

IF-081 Courier jobs on explicit three-dimensional routes
Milestone: M9
Owner: helpers
Depends: IF-080
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:helpers"]
Contract: Courier takes one held item batch from assigned source to assigned compatible destination, using explicit tissue waypoints and bounded vanilla local path attempts. Reserve destination before pickup; on blockage retain cargo and return/idle with visible reason.
Red: Stairs, one-step hill, removed waypoint, full chest, owner change and worker death cannot duplicate cargo or leak permanent reservations.
Accept: Two helpers service separate beds sharing a store; show different throughput versus veins without requiring courier use. Reassign route in UI, no global autonomous job search.
Bounds: <=32 waypoints/route,32-block local search box,1 path request/worker/40ticks and4/server tick; one carried stack/worker.
Evidence: rules, game, visual, soak

---

IF-082 Nutrient Sail and paid chunk ownership
Milestone: M9
Owner: helpers
Depends: IF-081
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-15
Scope: ["@module:helpers"]
Contract: Implement Sail Roost/Nutrient Sail support and catalog late anchoring mutation. Base Sail stays in its home chunk without loading it; late enabled roost pre-pays20BU/s from1200BU buffer. Dead/absent sail, disable/removal and starvation release ticket; no self-wake.
Red: Sixteen admitted roosts reject17th; fifth/player refuses; duplicate chunk claims cannot double charge/tickets; reload with missing sail releases stale ticket.
Accept: Visible chunk boundary and fuel endurance in panel; loaded-factory soak counts actual engine-neighbor chunks, not only requested ticket. Killing drones does not permanently reduce hatch allowance.
Bounds: 4 active roosts/player,16/server across dimensions; persisted admitted records only; existing helpers/population limits.
Evidence: rules, game, visual, soak

---

IF-083 Reusable ground and air defenses
Milestone: M9
Owner: defense
Depends: IF-082
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-16, T2-17, T2-18, T2-19, T2-30
Scope: ["@module:defense"]
Contract: Implement Aerocyte Bloom one green projectile/shot, Spine Sentry, Repellent Crown, Lure Polyp and Restraining Tissue targeting hostile mobs only. Consume ammunition/biomass via actual reservoir/vein ports. Each organ performs one targeting/attack/support operation, not an autonomous defense network.
Red: Players/tamed pets never become targets; empty fuel, blocked muzzle, dense entities and unloaded target refuse shot; projectile hit cannot pay biomass twice.
Accept: Reservoir supplies Bloom through raised junction; test phantom/blaze flight and ground targets; particles emphasize one readable green shot rather than spam.
Bounds: <=24-block query radius,16 returned candidates/query,64 defensive queries/server tick; <=128 active colony projectiles/server and one shot/organ/40ticks baseline.
Evidence: rules, game, visual, soak

---

IF-084 End targeting and selective physical membranes
Milestone: M9
Owner: defense
Depends: IF-083
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T5-09, T5-10, T5-11
Scope: ["@module:defense"]
Contract: Targeting Eye supplies bounded chosen target to an attached defense; Selective Membrane collision uses authorized full-suit capability; Catching Membrane arrests a paid bounded fall. Neither grants remote entity lookup or unlimited flight.
Red: Unknown ownership or incomplete suit cannot bypass selective collision; catching without fuel does not reset fall indefinitely; all target queries remain loaded/local.
Accept: Walk/collide through membrane with qualified/unqualified suit; demonstrate falling safety net with actual refill and output state; Eye improves targeting without new projectiles.
Bounds: Existing defense/entity budgets; one catch/player/40ticks and64/server tick; no terrain or chunk lookup beyond local structure.
Evidence: rules, game, visual

---

IF-085 Temporary membrane projector and relocatable shelter
Milestone: M9
Owner: construction
Depends: IF-084
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-37, T4-18
Scope: ["@module:construction"]
Contract: Implement I124 manual projector and Temporary Membrane using the baseline in Items; no automatic fall-arrest or free-air placement. Folded Shelter unfolds only its paid retained parts into an empty bounded grounded footprint; this is portable construction, not a Fold dimension feature.
Red: Expiry or breaking membrane yields no item; reload cannot reset remaining lifetime; shelter cannot overwrite terrain, inventories or duplicate contents on packed-item copy.
Accept: Project visible adjacent support then cross it within lifetime; dismantle/refold shelter with original owned core and stored quantities preserved.
Bounds: 32 membrane cells/player,256/server;10s lifetime; <=125 shelter cells; shared16 construction edits/server tick.
Evidence: rules, game, visual

---

IF-086 Compound anatomy and paid culture services
Milestone: M9
Owner: synthesis
Depends: IF-085
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T7-01, T7-02, T7-03
Scope: ["@module:synthesis"]
Contract: Synthesis Heart composes installed Anatomy Sockets and Catalyst Lobes through small recipe-service ports. Each lobe owns one culture/feed stream; installed components retain their own counters, not copied into controller. Build catalog synthesis recipes using three-world intermediates.
Red: Missing lobe/feed/heat/space refuses; closing one socket isolates it without consuming neighbor stock; breaking host never duplicates donor histories.
Accept: Build two valid layouts sharing versus separating lobes; measure productivity/footprint/feed tradeoffs and keep native input chains necessary.
Bounds: <=16 sockets/Heart,one active recipe/core,4096-cell structure cap; recipe/route shared budgets.
Evidence: rules, game, visual

---

IF-087 Organ expression profiles and transplantation
Milestone: M9
Owner: synthesis
Depends: IF-086
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T7-04, T7-05
Scope: ["@module:synthesis"]
Contract: Expression Switch changes between2 installed compatible organ operating profiles only while drained/idle, after paid delay. Organ Transplanter moves one real leveled core into a compatible receiver. Armor mutation trees never gain profile switching.
Red: Working/undrained organ refuses switch; rollback cannot refund spent waste; donor and recipient cannot both retain history after restart or interrupted move.
Accept: Reconfigure shared batch refinery without rebuilding every part, then transplant mature Bio-Furnace preserving its identity/counts into larger assembly.
Bounds: <=2 installed profiles/host,one transfer/core; finite retained donor reservation; no copied counter banks.
Evidence: rules, game, visual

---

IF-088 Genome Vault and bounded knowledge replication
Milestone: M9
Owner: genetics
Depends: IF-087
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T7-06
Scope: ["@module:genetics"]
Contract: Implement Genome Vault grade/capacity from Block Catalog; explicitly selected owned peer sync merges coverage by maximum. Physical samples/stock still require freight. No Fold-specific species are enabled in campaign.
Red: Sending same genome repeatedly cannot multiply coverage; peer unload retains finite pending dirty flags, not message history; copied records cannot create stock.
Accept: Move bank to vault without losing coverage, synchronize one remote lab and continue native stock production locally; UI distinguishes knowledge from stock.
Bounds: <=128 species/Vault baseline,16 records/message and2 changed messages/s/peer;<=4 explicit peers, bounded dirty bitset.
Evidence: rules, game

---

IF-089 Limited interceptors and prepared siege fire
Milestone: M9
Owner: defense
Depends: IF-088
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T7-07, T7-08
Scope: ["@module:defense"]
Contract: Interceptor Nursery uses separately validated berths for temporary ground or air defenders; Siege Blossom fires paid ammunition only into local clear coverage with cooling available. Organic visuals, no profitable defender loot. Professions are permanent nursery specialization.
Red: No feed/berth/support means no deployment; expiry/death frees slot; full projectile quota blocks fire before spending; player/pet immunity remains.
Accept: Ground and air nursery variants have visibly distinct behavior; Siege shot connects to real steam/BE/cooling chain; compare with cheaper sentries.
Bounds: <=3 defenders/Nursery under96-server helper cap,120s lifetime; shared128-projectile ceiling, siege1shot/100ticks baseline.
Evidence: rules, game, visual, soak

---

IF-090 Catalog, guide and alternate-route completeness
Milestone: M10
Owner: campaign
Depends: IF-089
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign","scripts/check_content_coverage.py"]
Contract: Build executable coverage manifest for every in-scope block/item/armor rank and guide prerequisite. Recipes generated by each owner must be reachable without creative items. Verify two materially different supported refinery/base/armor routes; Fold Gateway/T8/T9/Fold grafts excluded explicitly.
Red: Delete one recipe, rename an item ID, omit a guide parent or leave a graft effect unimplemented: coverage fails at exact entry.
Accept: All in-scope catalog entries have registry/obtain/use/recipe/guide assertions, not empty registration checks. Record missing branch as failing implementation, never delete catalog entry to pass.
Bounds: Finite manifest and deterministic graph walk at build time; no runtime world scans.
Evidence: rules, game, integration

---

IF-091 Packaged compatibility and hostile-input matrix
Milestone: M10
Owner: integration
Depends: IF-090
Spec: docs/DEPENDENCIES.md, docs/ARCHITECTURE.md
Blocks: none
Scope: ["@module:integration"]
Contract: Run packaged base, required-library, JEI, Curios and combined profiles on dedicated server with two clients. Validate inventory capability simulation/commit against vanilla and synthetic hostile handlers; persist schema fixtures across each production schema version. No blanket claim for untested content mods.
Red: Missing optional mod, malformed packet/NBT, stale menu, malicious count, version mismatch and client-only class on server each has explicit assertion.
Accept: Original art loads without missing textures; joins, guide grants, transfers, armor and DNA work in all supported profiles; released JAR contains no test fixtures.
Bounds: Packet and collection caps from Performance are asserted under concurrent clients; fixed process/readiness deadlines.
Evidence: rules, game, visual, integration

---

IF-092 Dense three-world soak and recovery gate
Milestone: M10
Owner: campaign
Depends: IF-091
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign"]
Contract: Add bounded stress worlds with logistics cycles, max reservoirs,16 paid roosts,96 helpers,128 projectiles,8 excavators, full buffers and64 admitted spills across dimensions. Sample actual work counters, retained queues, chunk counts, TPS and memory; never infer TPS from quota arithmetic.
Red: At every configured ceiling, one excess operation refuses/defer safely; forced unload/restart does not grow queues, lose escrow or duplicate fuel.
Accept: Three30min fixed-seed runs report p50/p95/p99 MSPT and retained-state counts on named hardware. Acceptance: <=5ms p95 incremental mod tick cost versus equivalent idle fixture, no upward retained-job growth after warm-up; performance regressions remain failing until corrected.
Bounds: Hard budgets from owning services remain authoritative; no global cap silently raised to pass throughput; measured host-dependent target recorded separately.
Evidence: rules, game, soak

---

IF-093 Full Survival campaign and player-facing release candidate
Milestone: M10
Owner: campaign
Depends: IF-092
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign"]
Contract: Execute fresh-world Survival route through Overworld workshop, Nether export base, End self-supply and three-world synthesis using real actions. Validate second layout and distinct armor lineage. Capture screens/structures/all model faces in actual client; smart reviewer judges visibility and clipping, not only pixel diffs.
Red: Break a native bootstrap link, hide a required recipe or invert a logistics face and playthrough must fail at named action. No direct grants after starting fixture.
Accept: Both routes finish without administrative unlocks; record tuning deltas in existing recipe tables only after measured comparison. Required gameplay/features complete; no automatic public release/license grant.
Bounds: Finite scripted checkpoints with per-action timeouts and conservation ledgers; no real user saves. Final gate includes multiplayer and dense-base results.
Evidence: rules, game, visual, integration, soak
