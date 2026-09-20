# Implementation queue — M0–M10
# Contract format and shared requirements: docs/TASK_TEMPLATE.md.
# ktask owns status markers; packet identities exclude those markers.

[DONE] IF-094 Bounded progression graph validation
Milestone: M0
Owner: integration
Depends: none
Spec: docs/ARCHITECTURE.md, docs/TASK_TEMPLATE.md
Blocks: none
Scope: ["scripts/check_design_docs.py"]
Contract: Replace check_graph's recursive traversal with iterative validation of dict[str,list[str]|tuple[str,...]]. Return None without changing input for valid directed acyclic graphs, including disconnected components, repeated edges and shared dependencies. Raise ValueError for malformed node/reference/container types, unknown references, self-edges and cycles; reference/cycle errors name the offending node. Accept empty graphs. Reject more than 4096 nodes or 16384 total input edges (count repetitions) before traversing edges; do not change parents parsing, catalog data or other validators.
Red: Extend the existing CheckerTests first: a 4096-node chain succeeds without RecursionError; 4097 nodes and 16385 edges fail with ValueError. Capture actual failures against the existing validator with python3 scripts/check_design_docs.py --self-test.
Accept: Tests cover exact node/edge limits, a long-chain back edge, shared dependencies, unknown node, malformed inputs, list/tuple adjacency and input immutability on success/failure. Preserve existing guide/assembly/milestone checks and diagnostics' useful node identity. Record green using the same self-test command; run python3 scripts/check_design_docs.py plus the full gate.
Bounds: O(V+E) time, O(V+E) maximum auxiliary memory within admitted limits; no recursion, network, subprocesses, new dependencies or runtime gameplay changes.
Evidence: rules

---

IF-001 Pin library profiles and validate client/server bootstrap
Milestone: M0
Owner: integration
Depends: IF-094
Spec: docs/DEPENDENCIES.md, docs/ARCHITECTURE.md
Blocks: none
Scope: ["@module:integration","build.gradle","gradle.properties","settings.gradle","gradle/**","src/main/templates/**"]
Contract: Install DEPENDENCIES pins and verification metadata; implement the Integration bootstrap harness contract in ARCHITECTURE. Own its first-use process supervisor, evidence evaluator and fixtures under gradle/integration/**, runtime adapters under the integration module. Retain core classpath isolation and compilation; do not add a custom source analyzer. Add integrationHarnessTest, profileSmoke, captureClient and packagedServerSmoke. -PmodProfile=required|jei|curios|combined selects the required libraries, +JEI, +Curios or +both; default required, invalid values fail before launch. Both required libraries remain in all supported profiles. -PomitRequired=modonomicon|geckolib is only a negative profileSmoke loader fixture. Capture uses -PcaptureDir and -Pscenario=bootstrap. Packaged server uses the release JAR and an actual disposable Minecraft client join/disconnect, never testMod on the server. verifyAll includes fast harness fixtures and required-profile smoke; preserve all existing checks. IF-127 owns full matrix qualification after this harness is accepted.
Red: Record supervisor/evaluator failures for readiness timeout, failed player connection, missing capture and shutdown timeout, each asserting child cleanup and no success receipt. Omit each required library in turn and require its named loader diagnostic rather than a generic launch failure.
Accept: On Java21 run ./gradlew :core:verifyBoundary :core:compileJava integrationHarnessTest, ./gradlew profileSmoke -PmodProfile=required, ./gradlew captureClient -PmodProfile=required -Pscenario=bootstrap -PcaptureDir=build/integration/evidence/required/capture, and ./gradlew packagedServerSmoke -PmodProfile=required. Run ./gradlew profileSmoke -PmodProfile=required -PomitRequired=modonomicon and again with geckolib; the negative-fixture wrapper succeeds only after proving the expected loader failure and cleanup. Inspect fresh bootstrap PNGs and runtime ID/version evidence. Assert real GameTest server state, player join then disconnect, clean stop and release JAR exclusion of testMod/fixture classes. Run ./.ktask/verify.sh before handoff; the acceptance hook independently reruns it. Commands and artifact schema are specified in ARCHITECTURE and must exist before IF-127.
Bounds: Serial scenarios; at most one disposable dedicated server, one disposable client and one owned world concurrently per profile, plus bounded launcher/display helpers. Readiness 120 s/process, join/capture/disconnect 30 s each, graceful shutdown 30 s then forced cleanup/reaping 5 s. Whole profileSmoke <=180 s; captureClient or packagedServerSmoke <=420 s including cleanup; fast fixtures <=60 s. No ordinary saves or background runtime processes. Required-profile evidence and both negative fixtures have <=1800 s runtime budget; build preparation and full gate remain within the unchanged 7200 s worker budget.
Evidence: integration, visual

---

IF-127 Qualify the four bootstrap library profiles
Milestone: M0
Owner: integration
Depends: IF-001
Spec: docs/DEPENDENCIES.md, docs/ARCHITECTURE.md
Blocks: none
Scope: ["src/testMod/resources/integration/bootstrap-profiles.json"]
Contract: Add the fixed qualification manifest consumed by the accepted IF-001 harness: schema 1, scenario bootstrap, ordered profiles required/jei/curios/combined, seed 11 and the ARCHITECTURE deadlines. Freeze the candidate before evidence. Run the existing harness and evaluators for all four profiles; no harness, pin, production or gate changes. This packet completes all four-profile smoke, capture and packaged-server obligations; missing-required-library fixtures remain negative tests, never extra supported profiles.
Red: Run ./gradlew integrationHarnessTest -PintegrationFixture=wrong-mod-version; the production evaluator must exit nonzero for a synthetic runtime version differing from the pin, naming the mod ID. The ordinary fast suite asserts that rejection as a passing regression. Do not alter library pins or the final qualification profile to manufacture a failure.
Accept: Run ./gradlew :core:verifyBoundary :core:compileJava integrationHarnessTest and, for each p in required, jei, curios, combined, ./gradlew profileSmoke -PmodProfile=<p>, ./gradlew captureClient -PmodProfile=<p> -Pscenario=bootstrap -PcaptureDir=build/integration/evidence/<p>/capture, and ./gradlew packagedServerSmoke -PmodProfile=<p>. Each command consumes -PintegrationProfileFile=src/testMod/resources/integration/bootstrap-profiles.json and rejects disagreement with the CLI. Inspect both captures for each profile; require exact selected loaded-mod IDs/versions, absent unselected optionals, real GameTests, actual player join/disconnect, readiness and clean stop. Re-run both IF-001 negative loader fixtures through fast regression coverage; retain IF-001 real loader evidence. Run ./.ktask/verify.sh and verifyDistribution. Artifacts identify the frozen candidate and release JAR digest; no success claim for a partial matrix or stale capture.
Bounds: Same topology and per-command deadlines as IF-001. Four serial profiles cost at most 4*(180+420+420)=4080 s; allow 600 s preparation inside a 4800 s matrix deadline including cleanup. Reserve 1200 s for the existing full gate within the configured 7200 s worker budget; no retries or deadline increases. Engineering failures return for scoped repair, never reduced assertions or omitted profiles.
Evidence: integration, visual

---

IF-108 Incremental content coverage harness
Milestone: M0
Owner: integration
Depends: IF-127
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:integration","scripts/check_content_coverage.py","scripts/test_content_coverage.py","build.gradle","src/testMod/resources/content-checkpoint.json"]
Contract: Implement scripts/check_content_coverage.py with --self-test and --through IF-nnn plus Gradle checkContentCoverage. Initialize tracked src/testMod/resources/content-checkpoint.json to IF-108. Each content packet advances it to its own ID with its contributor; default gate reads this file from the current working tree, including uncommitted candidate changes, never ignored receipts. Explicit --through/-PcontentThrough may require a later checkpoint, never lower it. Each owner supplies src/testMod/resources/<owner>/coverage.json mapping task/catalog IDs to actual registry IDs and named obtain/use/guide assertions in its testMod contributor. Discover these exact paths; require every obligation through the independent checkpoint and reject unknown/later contributions. No runtime scanning or production placeholders. Before IF-004, schema-check staged guide data/assertions for implemented content; obtain/use checks execute immediately. At IF-004 and later, execute all eligible guide assertions, including the staged predecessor entries.
Red: With a synthetic owner contribution, remove an item, rank, recipe-use assertion or required producer: gate fails at exact identifier. An empty initial gameplay registry passes only while no content task is accepted.
Accept: Wire checkContentCoverage into verifyAll and execute named contributor assertions through actual GameTests/client scenarios. A clean clone uses the tracked checkpoint. Empty IF-108 coverage asserts no gameplay registrations. Tests delete the only/latest contributor under the default invocation, omit an assertion, remove a producer, lower the explicit checkpoint and add future content: each fails by ID. An uncommitted candidate checkpoint/contributor must be checked without launcher flags. Aliases map to one representation; no registration-only substitute.
Bounds: Build-time bounded4096 identifiers and16384 edges; no world scans or AI in gate.
Evidence: rules

---

IF-095 Craft the initial culture and dormant Organ Bud
Milestone: M0
Owner: construction
Depends: IF-108
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T0-16
Scope: ["@module:construction"]
Contract: Implement I000 crafting and I001 as the actual placeable T0-16 item using their exact vanilla-input recipes. Bud holds no processing state. Its construction port replaces the bud only with a registered recipe's remaining paid ingredients; unavailable recipes refuse, without placeholder organs.
Red: Invalid or incomplete construction leaves bud and inputs unchanged; crafting produces exactly one Culture or Bud.
Accept: Craft and place a Bud from ordinary materials; a duplicate application cannot spend twice. Register only these entries; Bowl renewal and later construction recipes attach through the port in their own tasks.
Bounds: One visible target/use; shared16 placement admissions/server tick; idle Bud has no ticker.
Evidence: rules, game, visual

---

IF-005 Player-selected substrate and living visual stages
Milestone: M1
Owner: ecology
Depends: IF-095
Spec: docs/LIVING_SUBSTRATE_MUTATIONS.md, docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T0-01
Scope: ["@module:ecology"]
Contract: Implement base/mature functional host anatomy from LIVING_SUBSTRATE_MUTATIONS; Culture converts only visible selected eligible ground. Preserve reinforcement, dye and ownership through compatible mutations. Render deterministic connected variants with original 32/64px seamless textures; no autonomous spread.
Red: Hidden blocks, protected cells, unpaid conversion and incompatible host mutation refuse without changing target or inventory.
Accept: Horizontal, wall and underside neighboring cells connect without UV seams; mature stages remain distinguishable. Soil conversion cannot consume trees or grass implicitly.
Bounds: One requested conversion/use under 16 conversions/server tick; static cells have no block entity/ticker; variant selection uses position, not stored random history.
Evidence: rules, game, visual

---

IF-002 Culture Bowl: one server-owned batch
Milestone: M0
Owner: processing
Depends: IF-005
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T0-02
Scope: ["@module:processing","@module:storage","core/src/main/java/org/jd/infestusfrontier/organ/**","core/src/test/java/org/jd/infestusfrontier/organ/**","core/src/main/java/org/jd/infestusfrontier/foundation/**","core/src/test/java/org/jd/infestusfrontier/foundation/**"]
Contract: Implement Culture Bowl with I000 renewal, I001 Bowl recipe and I005/I007/I030/I033 Bowl recipes from Items; no Binder-dependent recipes until IF-096. BatchWork.start(request, expectedRevision) returns Started or typed Refused; advance(workUnits) returns immutable state. Reserve inputs, outputs and returned containers before consumption. Introduce OrganHistory and QuantityStore here for Bowl history, balances and reservations only. Wire TickQuota as shared completion admission, not a per-organ quota.
Red: Full returned-bottle slot, insufficient water and duplicate start leave inventory/counts unchanged; a midway reload completes exactly once.
Accept: Culture renewal, Bud, Elastic Gel, Nutrient Mash, Honey Culture and Rooting Gel batches match Items; one completion earns one count; dismantling retains one history; pure L1/L2/L3 choices preserve their caps. No auto-export or ingredients registered ahead of their producer.
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
Contract: Implement I009 craft and once-only first-join claim; Modonomicon adapter teaches the implemented Bowl recipes and JEI displays their inputs, output, duration and BU. Advancement completion is server-owned; guide visibility follows GUIDE_PROGRESSION_TREE. Add only nodes whose content exists. Activate staged owner guide entries/assertions from earlier content packets, using one discovery-owned integration fixture that iterates their existing contributors. Before this packet those assets are schema-checked only; afterward every eligible introduced entry must have executed client guide assertions.
Red: Full inventory cannot lose or duplicate a claim; repeated join cannot grant a second book; creative give does not accidentally satisfy a completion-only node.
Accept: Fresh Survival player can craft Culture, find Bowl and renew Culture from the guide alone. Lost book recrafts without resetting progress; JEI absent leaves the guide usable.
Bounds: One pending claim boolean/player, no delivery queue or world item spawn.
Evidence: rules, game, visual

---

IF-006 Hand-fed biomass and portable transfers
Milestone: M1
Owner: storage
Depends: IF-004
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T0-03, T0-04
Scope: ["@module:storage","src/main/java/org/jd/infestusfrontier/processing/digestion/**","core/src/main/java/org/jd/infestusfrontier/processing/digestion/**"]
Contract: Implement Digestive Sac, Bladder and I019 Biomass Bucket with Items feed yields; BU is exactly mB. Reuse QuantityStore.preview/reserve/commit. Bladder Fill exposes a compatible-equipment fueling port; actual armor is introduced in IF-010. Bucket holds1000mB; world placement remains unavailable until containment. Ampoules are introduced in IF-097, after Sealing Resin exists.
Red: Full receiving store and interrupted tank/bucket transfer conserve exact mB; rotten flesh is consumed only when its complete output fits.
Accept: Digest a batch, fill and empty a bucket between tanks; rejected output remains held; use real vanilla inputs and existing Bud. Equipment port refusal is tested without inventing an armor item.
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
Contract: Implement Membrane Rack/Bone Loom and I002/I003 recipes through BatchWork. Leather substitution doubles rack time; bone blocks never become nine bones. Skeletal Grafts arrive in IF-096 after Binder.
Red: Full outputs and missing water preserve inputs; leather route doubles duration without doubling output; one bone block cannot mint bones.
Accept: Original readable item art and static block models; ingredients, byproducts and times agree between recipes, guide and JEI.
Bounds: One batch/organ and existing 16 shared completion budget; no new tick scheduler.
Evidence: rules, game, visual

---

IF-096 Bowl binders, lumen secretion and skeletal grafts
Milestone: M1
Owner: processing
Depends: IF-007
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:processing"]
Contract: Add I004 Fusion Binder and I006 Lumen Secretion at the existing Bowl, I029 Char Gland Feed Bowl recipe and I050 Skeletal Graft at the existing Loom. Read exact quantities from Items; no alternate machine engine.
Red: Insufficient BU, full output or bottle return refuses unchanged; partial completion across reload cannot mint a second Binder batch.
Accept: Use existing Rack Sheet and Sac biomass to make Binder, then actual Lumen Secretion and Skeletal Graft; guide and JEI read the same recipe definitions.
Bounds: Existing one-batch/core and shared16 completions/tick.
Evidence: rules, game, visual

---

IF-008 Bud placement and biological shell parts
Milestone: M1
Owner: construction
Depends: IF-096
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T0-09, T0-10, T0-11, T0-12, T0-13
Scope: ["@module:construction"]
Contract: Attach paid organ-construction recipes to the existing Bud port; implement skin, ribs, windows, lumen and seed storage. Introduce Structure.inspect(origin, ruleId, budget) returning Valid/Invalid/Deferred; inspect only loaded positions and never recursively inspect a connected colony.
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
Contract: Create original articulated armor models with visible face/skin openings, dye layer, vanilla trims and the documented enchantment policy. Rendering reads immutable appearance data; GeckoLib stays in the client adapter. Register the optional Curios sample slot without a placeholder pouch; IF-027 supplies the real pouch and validates slot insertion. Curios never supplies required armor stats.
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
Contract: Implement Cyst construction from starter products and the exact Cyst recipes I021/I022/I023/I024/I026/I027/I028 plus existing Bowl/Cyst recipes. Prepare I031/I032/I045/I046/I047/I049/I054/I107 where Items assigns Cyst, with their existing input items. I020 Resin is harvested vegetation owned by IF-022; I025 Conductive Myelin belongs to IF-121. Spent Penetrant and its Compost Gland recovery recipe wait for IF-039. Reuse BatchWork; no second machine engine.
Red: A first Cyst constructs from starter products only; inactive minerals cannot substitute for prepared grafts; refused conversion retains containers.
Accept: Each Cyst recipe has original item art, guide and JEI entry; hand-feeding works before Item Veins; resin's Elastic Gel alternative avoids a tree bootstrap cycle.
Bounds: One batch/core, existing shared completion limit.
Evidence: rules, game

---

IF-097 Sealed ampoules with partial armor refueling
Milestone: M2
Owner: storage
Depends: IF-013
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:storage","src/main/java/org/jd/infestusfrontier/platform/equipment/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/equipment/**"]
Contract: Implement I036 after Rack and Cyst outputs exist. Empty ampoule uses the Items recipe; hold-use transfers actual stored biomass to selected worn pieces through Equipment's port, capped250mB, retaining partial contents.
Red: A full recipient, empty ampoule, simultaneous Fill and repeated use cannot lose or duplicate biomass; interrupted use retains the same partial container.
Accept: Craft, fill from Bladder, partially feed real armor and empty the remainder into a compatible store; no world placement or automatic intake.
Bounds: 4 portable transactions/player/s under64 transfers/server tick; unstackable finite container.
Evidence: rules, game, visual

---

IF-014 Embedded biomass veins and configured ports
Milestone: M2
Owner: logistics
Depends: IF-097
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-01, T1-04
Scope: ["@module:logistics"]
Contract: Implement biomass tissue edges and Vascular Junction. RouteService.transfer(source, destination, amount) plans then commits exact BU using QuantityStore ports. Probe configures six faces as closed/input/output/both; junction can support a reservoir and grow from one to two blocks with bone meal.
Red: Cycle A-B-A cannot transfer twice; blocked output or unloaded endpoint retains source; both-direction port cannot withdraw the same reserved amount twice.
Accept: Green pulses follow committed flow and remain inside all exposed faces. Bladder -> junction -> Culture Bowl works with junction at either height; screenshots include underside and vertical segment. Reservoir attachment is verified when IF-016 introduces it.
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
Accept: Connected inner glass/flesh faces are culled; rounded fleshy base and restrained glow render from inside/outside, below, and through opposite wall. Junction underneath fills and drains it. Test source -> junction -> consumer with the reservoir above both one-block and two-block junctions.
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

IF-022 Useful infected bushes and trees
Milestone: M2
Owner: cultivation
Depends: IF-020
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-20, T1-25, T1-26, T1-27, T1-28
Scope: ["@module:cultivation"]
Contract: Implement deliberate bush/tree conversion and the exact Managed vegetation allocation table in Items. Tree wood/resin/planting-stock and bush berry/fiber propagation are separate paid allocations; taps never also award the whole wood harvest. Manual harvest and capsule outputs work before automated harvesters. Retain one basal cutting; pods transfer their held product once.
Red: Re-converting a living tree, breaking an emptied pod or loading a canopy twice cannot create extra cuttings/wood; protected trees and foreign block entities remain untouched.
Accept: A cultivated grove visibly retains foliage, varied wood and luminous pods; choosing wood versus resin gives different useful supply chains. No barren automatic clearcut.
Bounds: <=256 explicitly assigned loaded tree cells/Arbor, <=4 cell probes/s under cultivation quota; cap held harvest to 9 stacks.
Evidence: rules, game, visual

---

IF-125 Reusable held batches and separate collection
Milestone: M2
Owner: mineral
Depends: IF-022
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-32, T1-33
Scope: ["@module:mineral"]
Contract: Implement Work Bed item mode with4 stacks and ready/working/released/blocked states; Collection Cilia exports only released contents of its facing Bed to one adjacent inventory or typed route. Expose a reservation-based batch deposit port; IF-021 wires Harvest Corolla to it without exposing cultivation internals. Cilia moves at most4 items/20ticks for1BU; Elastic Gel raises8 with the same cost. Native reference harvest cost includes that collection charge: reserve it from the listed harvest budget, not an unlisted extra debit. IF-038 adds exclusive block mode; no placeholder Workpiece items yet.
Red: Unreleased target, full destination and simultaneous Cilia calls cannot lose or clone contents. No nearby loose-item pickup. Refused harvest cannot also retain a second Bed copy.
Accept: Hand-place a real crop item batch, mark it released and collect through Cilia to adjacent storage, then feed existing digestion manually. Capture held/working/result states and stable comparator interlocks. IF-021 owns automated crop deposit.
Bounds: 4 stacks/Bed,one facing target and one recipient/Cilia; shared64 transfer admissions/tick, no search/ticker on idle Bed.
Evidence: rules, game, visual

---

IF-021 Crop beds, planting and independent harvesting
Milestone: M2
Owner: cultivation
Depends: IF-125
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-17, T1-18, T1-19, T1-31
Scope: ["@module:cultivation"]
Contract: Implement Cultivation Tissue, Planting Proboscis, Harvest Corolla and Compost Gland as separate operations. Grower reserves water/feed and real seed; harvester holds its result; planter requires an empty eligible cell. Match Overworld reference wheat loop in Items. Route harvest into the existing Work Bed through its deposit reservation port; Cilia owns subsequent export. Capsule output remains a direct alternative.
Red: Missing seed, unloaded crop, full seed/output reserve and same-tick harvest requests cannot delete crops or mint harvests; vanilla random tick never awards a second allocated harvest.
Accept: Manual, serial and two-bed shared-harvester layouts work; reserve seed before selling output; crops visually change through actual growth.
Bounds: <=64 assigned cells/organ, 4 loaded probes/organ/s under 256 cultivation probes/server tick; <=16 harvest commits/server tick.
Evidence: rules, game, visual

---

IF-023 Dew water recovery and furnace airflow
Milestone: M2
Owner: processing
Depends: IF-021
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-24, T1-39
Scope: ["@module:processing"]
Contract: Implement Dew Gland with ordinary wheat recovery from Items and Hearth Lung as an airflow attachment, not a generator. An unobstructed Lung reduces Rack/Bio-Furnace loaded batch duration to4/5, rounded up, without changing inputs/yields; maximum one benefit per host. Advanced native Dew modes attach in their native cultivation tasks.
Red: Full water output preserves Dew feed; blocked Lung removes speed benefit; two lungs cannot stack speed; power extraction from Lung is unsupported.
Accept: Hand-feed Dew's two-wheat recipe and measure1000mB output plus listed40BU/10s cost. Compare the same Rack/Furnace batch with open/blocked Lung and unchanged resource ledger. No autonomous power or full reference-loop claim here.
Bounds: One batch/Dew, one attached Lung benefit/host; event-driven obstruction updates, existing shared16 batch completions/tick.
Evidence: rules, game

---

IF-121 Starter electricity and finite charge routing
Milestone: M2
Owner: energy
Depends: IF-023
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T4-01, T4-02, T4-03
Scope: ["@module:energy"]
Contract: Implement starter Electrocyte Stack, Conductive Tissue, Charge Sac and I025 Cyst recipe through existing processing/route/storage ports. Starter Stack converts200BU+100mB water over60s into4000BE+100mB dirty water, retaining both outputs. Charge Sac holds1000000BE. Direct adjacent energy port and selected tissue routes have no idle drain; delivered transfers lose ceil(amount/100)BE once per route, capped at source amount; direct adjacency is lossless. Reference module uses direct adjacency. No FE adapter or industrial conversion yet.
Red: Full charge/waste output refuses feed; simulate never spends; an unloaded receiver pauses. At one remaining unit transfer cannot round up energy or repeatedly round-trip into gain.
Accept: Craft all three entries from existing starter products, produce charge, retain waste and supply a finite synthetic load through direct/tissue routes. Actual filtration consumer arrives next; no phantom Hearth electricity. Add BE balances through QuantityStore, not a second resource store.
Bounds: 1000000BE/Sac,4000BE Stack output buffer,1000mB water/waste each; shared64 route commits/tick; one batch/core.
Evidence: rules, game, visual

---

IF-122 Starter process-water filtration
Milestone: M2
Owner: mineral
Depends: IF-121
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-24
Scope: ["@module:mineral"]
Contract: Craft Washing Kidney and implement only the Items process-water filtration recipe through existing BatchWork and energy ports. Reserve clean-water output before spending dirty water/BE. No ore products or artificial mineral registry placeholders; IF-040 adds mineral washing to this same organ.
Red: Insufficient1000BE, unavailable100mB output room and repeated start each leave inputs unchanged. Mid-batch reload completes one output.
Accept: Use actual starter Stack dirty water/charge and recover100mB clean water; assert total reference charge delta3000BE with direct adjacency. Retain water and stock for the next cycle.
Bounds: One batch/core,1000mB dirty/clean tanks,existing shared recipe/transfer budgets.
Evidence: rules, game

---

IF-024 Ground-based fueling and equipment service
Milestone: M2
Owner: equipment
Depends: IF-122
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
Accept: Connect hostile damage tissue through3D vein to reservoir and consumer; movement and door silhouettes stay readable. Travel Tissue refuses advanced boost for current unqualified armor. Positive full-set B7.II/L8 integration waits for IF-079, not fixture-only late grafts.
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
Contract: Automate the declared reference farm using separate planter/grower/harvester/collector/storage/control operations; validate positive reserves under full-output and chunk pause. Add route visuals and performance counters to reusable lab harness. Use existing Bed/Cilia, starter Stack and filtration Kidney; direct adjacent electrical coupling makes the stated lossless reference ledger applicable. Tissue-route losses are measured separately.
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
Scope: ["@module:genetics","src/main/java/org/jd/infestusfrontier/integration/curios/**"]
Contract: Implement I040 species/quality samples and I012 base/expanded pouch recipes with16x256 and32x1024 capacities from Items. Pure storage tests include64x4096; reinforced recipe is added in IF-112 after thermal ingredients exist. One death offers one sample to one active destination. Curios slot from IF-011 accepts the actual pouch and absence preserves ordinary inventory use; no individual specimen IDs.
Red: Duplicate death dispatch, full pouch, two eligible pouches and a no-loot species cannot duplicate output or fill normal inventory with refused samples.
Accept: Normal kills collect Fragmented samples; configured weapon quality can later replace quality, not quantity. Plant packaging consumes real harvest; vial withdrawal returns actual specimen.
Bounds: 64 collection attempts/server tick; excess discarded with bounded diagnostic; 128 registered species maximum initial catalog; no ambient entity scan.
Evidence: rules, game

---

IF-028 Sample docking into explicit destinations
Milestone: M3
Owner: genetics
Depends: IF-027
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T1-38
Scope: ["@module:genetics"]
Contract: Implement Sample Dock and explicit pouch docking as finite counted transfers. Preserve species/quality and ownership; expose its typed sample port for Extraction. Archive and killer-linked Collector construction follow their Auric ingredient in IF-118.
Red: Last-slot competition, oversized NBT, changed ownership, unloaded destination and duplicate death transactions refuse safely without secondary loose drops.
Accept: Move mixed-quality samples pouch -> dock -> withdrawn vial through real items; counts survive break/reload with no inventory noise. An extractor attaches in IF-029.
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
Accept: Reconstruct an ordinary genome, inspect partial coverage, merge a copy safely and withdraw stock. Test boss arithmetic at1000 without spawning1000 mobs. Consume real samples delivered through the existing dock, not fixture-only extractor inventory.
Bounds: 16 species/ordinary Bank,32 with precision service; each Archive Lobe adds8 within the catalog assembly limit. One extraction batch/core; existing UI and recipe budgets. Removing expansion never deletes records: excess records become inactive until capacity is restored.
Evidence: rules, game, visual

---

IF-110 Standalone sequencing preview
Milestone: M3
Owner: genetics
Depends: IF-029
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-04
Scope: ["@module:genetics"]
Contract: Craft Sequencing Lens with the exact catalog recipe; attach to R1 Extractor and read existing bank coverage through Genetics interfaces. No precision multiplier or electricity service yet.
Red: Wrong species, unavailable bank and depleted sample produce a named unavailable preview; opening the UI cannot consume or duplicate specimens.
Accept: Craft/place Lens; compare preview gain/cost against the next actual R1 batch for incomplete and completed ordinary genomes. Retain its actual item for the consuming recipes in IF-030.
Bounds: One selected sample/bank record per preview; existing paginated UI and packet budgets.
Evidence: rules, game, visual

---

IF-030 Stock cultivation and multi-species mutation chamber
Milestone: M3
Owner: genetics
Depends: IF-110
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-05, T2-06
Scope: ["@module:genetics"]
Contract: Implement Culture Vat stock culture and its material-mode recipe port; implement Mutation Chamber reservation/installation service. Stock recipe is I042 using existing I054. Complete genome and native predicates are independent. Material recipes attach in IF-040; physical genetic-graft preparation and its first full installation are owned by IF-098.
Red: Incomplete genome, wrong dimension, missing species or insufficient power each independently refuses unchanged; two users cannot withdraw same last stock.
Accept: Craft Vat and Chamber using the actual Lens. Extract an ordinary stock seed, complete its genome, culture it with actual Protein Feed, reserve/cancel an installation target without spending or cloning it; no placeholder grafts. First genetic-graft installation is tested by IF-098.
Bounds: One reserved batch/chamber, <=8 distinct species/recipe; shared completion quota.
Evidence: rules, game, visual

---

IF-031 Base living tools and Living Fang
Milestone: M3
Owner: equipment
Depends: IF-030
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:equipment"]
Contract: Implement I015-I018 base dormant/awakened tools and Fang only. Use vanilla wooden speed/harvest tier/damage, durability59; awakening preserves wear, tank10BU, heals one durability for1BU. No I038 combat bodies, Dissector upgrades, area mining or Fortune multiplication in this packet.
Red: Unqualified tools cannot mine higher-tier blocks; awakening cannot heal wear for free; duplicate awakening cannot clone a tool.
Accept: Each tool performs its normal manual action and spends actual fuel only on healing; all identities/wear survive reload. Dissector I is IF-098; II waits for IF-101.
Bounds: No inventory-wide ticker; equipment checks active hand only; collection uses existing global budget.
Evidence: rules, game, visual

---

IF-098 Chrysalis preparation and Dissector I installation
Milestone: M3
Owner: equipment
Depends: IF-031
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-31
Scope: ["@module:equipment","src/main/java/org/jd/infestusfrontier/platform/genetics/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/genetics/**","@module:genetics"]
Contract: Implement Fusion Chrysalis using catalog construction and the existing standalone Lens, without R2 services. Prepare Dissector I exactly as Item Catalog's Weapon sampling specialization row and install through the existing Mutation Chamber port on an existing Living Fang. Complete spider and silverfish genomes required. Expose the paid preparation port used by this Dissector; armor and organ graft recipes attach in their completing tasks.
Red: Incomplete genome or missing carrier refuses before spending; installation and repeated lethal-hit callbacks cannot clone Fang or sample; copied prepared graft has no installed counter state.
Accept: Complete sample -> extraction -> Vat culture -> Chrysalis -> Chamber -> Dissector I -> Intact sample through real items. Implement no proposed combat body. UI previews permanent sampling choice.
Bounds: One reserved piece/graft per batch; existing genetic/sample/recipe budgets.
Evidence: rules, game, visual

---

IF-032 Independent Carry Sac and pouch socket
Milestone: M3
Owner: portable
Depends: IF-098
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:portable"]
Contract: Implement I126 Carry Sac9-slot crafting and18-slot Chrysalis upgrade, one optional pouch socket and nesting prohibition from Items. Pure storage admission tests also cover27 slots; the27-slot Survival recipe belongs to IF-112 after tempered materials exist. Carry Sac never adds armor abilities or expands an installed pouch's capacity.
Red: Nested Sac, shulker box or portable handler cannot create recursion; rapid swapping and disconnect during move conserve items; Curios absent retains manual pouch use.
Accept: Craft/use9- and18-slot grades while changing armor;27-slot admission is pure-rule coverage only here. Pouch collection consumes no general slots; UI shows actual fixed capacity and disables refused insertion.
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

IF-038 Work Bed and one-block extraction presentation
Milestone: M4
Owner: mineral
Depends: IF-033
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-32
Scope: ["@module:mineral"]
Contract: Extend existing Work Bed with exclusive block mode and I103 wrapped workpiece plus stage, not a duplicate world ore; prohibit item/block modes occupying it simultaneously. Grasping Root moves exactly one existing eligible block to one bed then stops. Block entities, protected blocks and unsupported modded ores refuse. Vanilla placing a real ore on a bed remains possible.
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
Blocks: T2-33, T2-34
Scope: ["@module:mineral","src/main/java/org/jd/infestusfrontier/platform/cultivation/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/cultivation/**"]
Contract: Reaction Polyp treats a held block once using I107 prepared penetrant, retaining I109 Spent Penetrant. Add its exact Compost Gland recovery recipe through the existing data-driven recipe port. Fracture Jaw consumes the final workpiece stage into retained I108 fragments; Existing Collection Cilia alone moves retained output to storage. Honor stable interlocks; no loose automatic item spawning.
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
Blocks: T2-23
Scope: ["@module:mineral","src/main/java/org/jd/infestusfrontier/platform/genetics/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/genetics/**"]
Contract: Implement Mineral Gizzard and add mineral washing/retained tailings recipes to existing Washing Kidney. Start iron/gold/copper at4 dust/raw ore and4 dust/ingot; use the exact recovery/smelting table in Items; here enable base4-dust and Kidney5-dust recovery plus4-dust smelting only, never automatic multiply then apply vanilla Fortune. Dust fragments remain items/storage until a complete recipe fits. Include I061 washed iron/gold/diamond/obsidian portions, I062 Ferrocyte Paste and I063 Auric Myelin; use the existing Vat material mode, not a second genetic processor.
Red: All routes reject reprocessing treated intermediates as pristine ore; incomplete dust batches are retained; swapping recipes cannot erase fractional progress.
Accept: Compare raw ore/direct furnace, simple dust and washed chain using exact resource ledgers; guide/JEI expose total recovery and waste, not misleading per-stage multipliers.
Bounds: One batch/processor; finite9-slot waste/output buffers; no floating-point production amounts.
Evidence: rules, game

---

IF-118 Raw specimen archive and killer collection links
Milestone: M4
Owner: genetics
Depends: IF-040
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-35, T2-36
Scope: ["@module:genetics"]
Contract: Craft Specimen Archive using actual Auric Myelin; implement Sample Collector binding one explicit killer and adjacent destination. Reuse Dock's sample port. Archive capacity128 species and16384 total samples summed across species/quality counters; all quality categories are counted.
Red: Last-slot competition, duplicate death callback, foreign killer and unloaded destination cannot clone samples or add fallback loose drops. Full archive refuses without deleting input.
Accept: Manufacture materials, build archive, dock pouch, withdraw vials and feed existing Extractor. Linked Collector consumes one existing killer event; later defense organs use the same port.
Bounds: 16 species/page,48 quality counters/page,2 changed pages/s; shared collection quota; no entity searches.
Evidence: rules, game, visual

---

IF-120 Precision Probe and bounded diagnostic history
Milestone: M4
Owner: control
Depends: IF-118
Spec: docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:control"]
Contract: Produce I086 in Chrysalis using the canonical recipe and preserve existing Probe settings. Query RouteService for one selected path, retaining at most32 readings in the tool's diagnostic session; no network-wide history. Expose authorized finite survey selection for IF-116.
Red: Wrong owner, unloaded path, excess requested rows and copied settings never authorize new access; a full history evicts oldest reading rather than growing.
Accept: Upgrade an actual Probe, inspect a blocked configured path, resolve its named fault, and prove tool relocation/reload preserves configuration but not live contents or access authority.
Bounds: 32 transient samples/session,2 updates/s,16 rows/page,4KiB response; existing query quota; no sampling when UI closed.
Evidence: rules, game, visual

---

IF-034 Paid fusion and irreversible frame trees
Milestone: M4
Owner: equipment
Depends: IF-120
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:equipment","src/main/java/org/jd/infestusfrontier/platform/genetics/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/genetics/**"]
Contract: Extend the existing Chrysalis with fusion graft recipes and implement the equipment-owned permanent frame transition service used by Mutation Chamber installation. Implement/test all G1->G2->G3->G4 edges from Armor Evolution as pure rules; enable Survival installation recipes only for G2 Ferrocyte/Auric media from the existing Gizzard -> Kidney -> Vat chain. Later producer tasks enable their own G3/G4 recipes; do not register placeholder ingredients or waive native services. Consume prepared medium, Binder and BU with listed costs/times; reject raw ingots/diamonds. Preserve identity, wear ratio, counters and installed branches.
Red: Backward/sibling transitions refuse; cap increase does not allocate learning points; full output/disconnect cannot clone a frame or charge twice.
Accept: Pure tests prove every frame's exact defense, durability, summed learning cap and mutation slots. Actual Survival fusion covers G2 iron/Auric only; UI previews permanent successors and unavailable prepared inputs without making G3/G4 obtainable.
Bounds: One piece and one reserved fusion transaction/Chrysalis; checked integer BU arithmetic.
Evidence: rules, game

---

IF-035 Graft installation and first survival mutations
Milestone: M4
Owner: equipment
Depends: IF-034
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:equipment"]
Contract: Apply Host I at the existing Cradle; all other legal armor grafts install through the existing Mutation Chamber equipment port. Implement obtainable M1.I, M3.I, H1.I, H2.I, H3.I and H4.I using Items graft recipes. Test higher-rank H2 cone rules without registering their later recipes. No plant grafting, M2 before Nether tolerance, extraction or branch swapping; IF-075/077 own later ranks.
Red: Wrong slot, missing predecessor, mutually exclusive branch and insufficient slots refuse without consuming graft; repeated confirmation cannot install twice.
Accept: Early self-healing, basic protection and lighting draw actual fuel; underwater vision does not supply air. UI shows exact cost/benefit and permanent exclusions; advanced ranks remain discoverable but unavailable.
Bounds: At most catalog family/rank slots per piece; action query returns immutable capability snapshot.
Evidence: rules, game, visual

---

IF-123 Permanent plant grafts with cultivation tradeoffs
Milestone: M4
Owner: cultivation
Depends: IF-035
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-09
Scope: ["@module:cultivation"]
Contract: Build Grafting Bench and prepare I052 using the exact Plant graft traits table. Require complete matching genome and real stock via genetics port; planting item retains exactly one permanent water-thrift or rapid-growth trait. Armor grafting remains Cradle/Chamber owned. Support wheat, oak, birch and berry here; kelp installation follows IF-072 aquatic production and chorus follows IF-100 native genetic service. Pure trait rules cover all declared source IDs without claiming those later recipes obtainable.
Red: Wrong/partial genome, sibling trait replacement and cancelled batch cannot spend/clone planting stock. Apply a modifier once; grafted seed yield cannot duplicate basal stock.
Accept: Graft and plant each supported source through actual managed cultivation. Compare plain/water-thrift/rapid-growth cost, duration and identical yield; harvest/replant/save/load retains species and chosen trait.
Bounds: One batch/Bench,one trait per planting item; existing bounded cultivation plan and recipe budgets.
Evidence: rules, game, visual

---

IF-036 Fuel priority, hunger, symbiosis and feeding branches
Milestone: M4
Owner: equipment
Depends: IF-123
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: T2-08
Scope: ["@module:equipment"]
Contract: Implement self-healing, Healing Dock and fuel priority/starvation from Armor Evolution; hunger has one wearer cadence and one-heart floor until that piece earns100 learning. C12 intake consumes configured containers; C13 Digestive Crop consumes only listed bio-materials using prepared grafts. Include C6 Gill Bellows air pulses before burrowing is enabled; H4 is underwater vision, not breathing. Obtain/install only C6.I/C12.I/C13.I here; IF-076 owns their higher ranks and complete routes.
Red: Four hungry pieces cannot bypass health floor; armor hunger neither kills nor heals; external damage remains lethal. Empty automatic source stops features rather than creating fuel.
Accept: Manual feed, Papilla and Dock share Equipment service; internal tanks differ by frame/mutations; C13 field production matches capped table rather than a portable factory.
Bounds: 4 supply transactions/player/s and64/server tick; inspect configured slots only; one hunger cadence/player.
Evidence: rules, game, visual

---

IF-041 Manual leaching with ground-level boundaries
Milestone: M4
Owner: excavation
Depends: IF-036
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
Contract: Add Survival scenarios for two different armor lineages, safe hunger/refueling and manual leaching -> bed -> reaction -> fracture -> dust -> ingot. Use guide-discovered recipes without unlock commands.
Red: Disable a learned buff or allow a sibling fusion and tests fail at exact step; full waste store halts refinery without deleting ore.
Accept: Measure consumables, armor BU and retained dust for lean versus recovery-focused layouts; implemented M4 guide nodes correspond to real interactions.
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
Contract: Implement Thermal Substrate/Nursery/Root/Cultivation using native bed contracts. Root provides heat only; charged thermal controls use actual BE; thermal fruit needs real seed, feed and local water production. Own I071 Thermal Lining at Nursery; first lining uses actual imported magma cream, then install Dew/Sac thermal treatments and demonstrate local-fruit recipe. Reserve5000BE per reference cycle for heat-control/pump operation, not for a fictitious Root generator. Specialized soil is created only in actual Nether; imported soil does not spoof dimension.
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
Blocks: T3-03, T3-04, T3-07, T3-09
Scope: ["@module:thermal","src/main/java/org/jd/infestusfrontier/platform/mineral/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/mineral/**"]
Contract: Implement Lava Siphon, Steam Heart and Thermal Mantle as separate intake/generation/application operations. Lava must be removed from a real loaded source or supplied by a real tank. Use Thermal Nursery's actual Thermal Lining to craft Relief Chimney; implement metered clear-vent relief before pressurized Steam Heart operation. Bind temperature/pressure to ThermalProcess state; recipes reserve water, steam and condensate capacity. Add the Mantle3-dust/ingot row in Items through the mineral recipe port; powered2/1 rows wait for IF-051.
Red: No water, full steam store or removed heat face stops start without disappearing lava; unloading pauses work rather than accumulating unlimited heat catch-up.
Accept: Produce heat/steam and manufacture Tempered Bone Plate through its actual Mantle recipe; blocked/unloaded relief independently prevents safe operation; unsafe temperatures are visible before damage; no mineral output from empty space.
Bounds: One source cell probe/start,16 thermal steps/server tick, one batch/core; max100000 checked pressure units/process baseline.
Evidence: rules, game, visual

---

IF-045 Steam transport, pressure storage and relief
Milestone: M5
Owner: thermal
Depends: IF-044
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T3-05, T3-06, T3-08
Scope: ["@module:thermal"]
Contract: Steam Vein uses typed RouteService payload; Pressure Vesicle stores finite steam/pressure; reuse existing Relief Chimney's metered dissipation; Condenser returns actual cooling output. Pressure state is thermal-owned, not a generic item-pipe field.
Red: Closed outlet/full condenser raises specified pressure but never routes through closed face; unloaded relief cannot be treated as open; simulation never spends steam.
Accept: Build bypass/relief/condensation loops with visible direction and warning states; normal full uncharged biomass storage still merely refuses.
Bounds: Existing routing and thermal quotas; finite16000 mB steam/Vesicle; relief processes one reserved dose/step.
Evidence: rules, game, visual

---

IF-115 Vanilla-compatible brewing organ
Milestone: M5
Owner: nutrition
Depends: IF-045
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-11
Scope: ["@module:nutrition"]
Contract: Craft Brewing Gland from a real brewing stand, Bud and Sheets. Use vanilla potion inputs, brewing fuel and container rules through the shared processing boundary. Add no biological serum or new effect recipes.
Red: Wrong ingredient, missing fuel and full returned-container output each independently refuses without item loss; crafted effect NBT cannot inject unsupported effects.
Accept: Brew awkward then healing potion from real ingredients and compare to vanilla output; reload mid-batch preserves inputs/progress exactly. Provide the actual organ required by Distillation Crown.
Bounds: One batch/core,9 slots; registered effects only; shared recipe completion budget.
Evidence: rules, game, visual

---

IF-116 Ground-level survey markers and paid survey imprint
Milestone: M5
Owner: excavation
Depends: IF-115
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-22
Scope: ["@module:excavation"]
Contract: Apply actual I031 Survey Gel to exposed mature substrate; tool selects corners/depth without raised posts. Produce I083 Survey Imprint from the catalog's paid survey transaction. This task marks and serializes plans; it does not cut blocks.
Red: Hidden or unloaded selection, oversized volume and repeated imprint confirmation refuse without spending or cloning output.
Accept: Mark a visible rectangle, preview its depth and create an actual imprint retaining only the authorized plan. Reset a selection without mutating buried terrain. IF-046 tests consumption in Crucible construction.
Bounds: At most4096 cells/plan; two corners and depth stored, no permanent per-cell list; shared structure-probe quota.
Evidence: rules, game, visual

---

IF-046 Mechanical work and thermal material processing
Milestone: M5
Owner: thermal
Depends: IF-116
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T3-10, T3-13, T3-14, T3-15
Scope: ["@module:thermal"]
Contract: Steam Muscle consumes steam to expose one bounded work impulse; Crucible performs one digestion reaction; Heat-Exchange Gill exchanges real process heat with cooling fluid; Distillation Crown separates reserved products. No automatic extraction, fuel production or free cooling side jobs.
Red: Work impulse without steam fails; cooling output-full refuses transfer; distillation never emits only valuable output while discarding unreserved waste.
Accept: Construct Crucible using actual Surveyed Tissue and Imprint; construct Crown using existing Brewing Gland. Create thermal bioactive fusion medium and cooling products from Items; drive one existing processor with Steam Muscle via a port, not direct internals.
Bounds: One impulse or batch/core, shared16 thermal steps/tick;9 retained slots and2 finite tanks/organ.
Evidence: rules, game, visual

---

IF-112 Tempered portable-storage upgrades
Milestone: M5
Owner: portable
Depends: IF-046
Spec: docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: none
Scope: ["@module:portable","@module:genetics"]
Contract: Add the reinforced Sample Pouch and27-slot Carry Sac recipes exactly from Items, through existing Chrysalis and storage ports. Reuse existing schemas/capacity admission; no duplicate pouch state or counters.
Red: Missing Tempered Bone Plate or Auric Myelin refuses; full outputs/reload/concurrent upgrade cannot duplicate contents, pouch identity or Carry Sac contents.
Accept: Produce actual thermal ingredients, upgrade filled base containers, and preserve exact species/quality and ordinary-item totals; Curios and standalone pouch behavior remain equivalent.
Bounds: 64 species and4096 total samples;27 ordinary stacks plus one independent pouch socket; existing transaction/page bounds.
Evidence: rules, game, integration

---

IF-047 Nether-native extraction and stock culture
Milestone: M5
Owner: genetics
Depends: IF-112
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:genetics"]
Contract: Enable Nether-class genetics only on real Nether3x3 thermal beds with Extractor/Vat L1 and2 Thermal Linings plus200BU native surcharge. Species class derives from the registered sample, not current loot name. Ordinary enderman remains Overworld-processable; Wither is Nether-class.
Red: Individually remove dimension/bed/level/lining/power and each refuses unchanged; migrating a bank does not make an Overworld extractor Nether-native.
Accept: Process magma-cube samples and culture completed stock in the native Nether lab. Blaze samples refuse until a precision bank service exists; Wither samples refuse until a Genome Vault exists. IF-099 and IF-102 own successful complex/boss routes.
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
Contract: Implement M2 thermal lining and C5 thermal exchange ranks plus Nether rigid frame recipes. Test pure higher-rank rules; enable each recipe only when its listed material/service exists. Prepare I073/I074 using actual Thermal Mantle service; G4 waits for IF-052. H6/H9 effects and complex-source grafts are IF-075 after IF-099. No immunity flag substitutes for paid thermal capabilities. M2/C5 rankIII remains IF-077.
Red: Lava drains exact BU and kills an unfunded unprotected wearer normally; fire resistance does not imply underwater breathing or arbitrary damage immunity.
Accept: A thermal-specialist suit completes a measured work window and refuels at Nether Papilla; mixed generalist pieces cannot equal every dedicated set.
Bounds: Fixed4-piece evaluation and listed upkeep cadence; no chunk tickets or extra armor inventory ticker.
Evidence: rules, game, visual

---

IF-050 Industrial conversion and external energy exchange
Milestone: M5
Owner: energy
Depends: IF-048
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T4-04
Scope: ["@module:energy"]
Contract: Extend existing Stack with the exact industrial upgrade from Items: L1 core,2 Tempered Bone Plates,2 Thermal Linings and actual Mantle/Gill cooling. Enable Nether1200BU->24000BE recipe only with native services. Implement Exchange Organ1:1 BE/FE at its own ports; existing route losses remain separate and cannot be refunded. No duplicated generator/storage/tissue implementation. Industrial upgrade expands the same Stack output buffer from4000 to40000BE; downgrade refuses while contents exceed target capacity. End conversion remains disabled until IF-126 supplies native services.
Red: Round trip through two exchange organs cannot increase either balance; full Charge Sac and unsupported FE handler refuse; absent external mods leave native loop functional.
Accept: Upgrade the actual starter Stack, provide thermal cooling/water and run a native industrial batch with all retained waste. Supply a real Kidney and standard FE test handler; reverse direction without gain. Industrial service removal falls back to starter mode only when idle, never reinterprets active inputs.
Bounds: <=64 energy commits/server tick under shared transfer admission; checked capacity1000000BE/Sac.
Evidence: rules, game, integration

---

IF-049 Nether base sustains itself and exports
Milestone: M5
Owner: campaign
Depends: IF-050
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign"]
Contract: Automate three native60s reference cycles starting from the finite Items kit. Use exact recipe-table durations and actual deltas, including1000BE filtration plus5000BE heat-control/pump charge, planting/resin reserves and1500 netBU/declared cycle. Export only after local restart reserves.
Red: Disconnect Overworld supply before start; remove local seed/water reserve and restart assertion fails. No creative refueling after initial kit.
Accept: Nether manufactures Thermal Lining, DNA stock and surplus fuel locally, restarts after save/reload and outproduces declared Overworld reference at stated cost/footprint.
Bounds: <=512 organ fixture, finite24000 tick deadline; measure shared-budget throttling separately from recipe arithmetic.
Evidence: rules, game, visual, soak

---

IF-051 Independent recovery and smelting efficiency paths
Milestone: M6
Owner: mineral
Depends: IF-049
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T4-05
Scope: ["@module:mineral","src/main/java/org/jd/infestusfrontier/platform/equipment/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/equipment/**"]
Contract: Ion Separator recovers retained tailings through recipe-defined chemistry/BE. Complete4->5->6->7->8 dust recovery and4->3->2->1 dust/ingot branches from Items as separate installed services; add only the Separator6/7/8 recovery and powered2/1 smelting rows here, reusing earlier4/5 recovery and4/3 smelting; every stronger recipe consumes its listed additional reagents/energy. Include I076 Diamond-Fiber Matrix preparation and enable G3 flexible fusion through the existing equipment service. G4 remains unavailable until precision service exists.
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
Blocks: T4-07, T4-19
Scope: ["@module:genetics","src/main/java/org/jd/infestusfrontier/platform/equipment/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/equipment/**"]
Contract: Reuse Sequencing Lens; implement Precision Sequencer/Cold Lobe and R2: Extractor L2 plus lens/precision service yields quality*4 coverage with additional100BU+2000BE/sample. Stock yield remains1; cooling material is consumed by actual recipe, not presence of a decorative block. Add I077/I078/I079/I080 precision material recipes and enable matching G4 rigid/mesh installations using equipment's existing transition API; do not duplicate frame statistics.
Red: Removing one precision component mid-batch cannot grant boosted coverage for R1 cost; high quality is not multiplied into extra free stock.
Accept: An existing L2 Extractor upgrades in place with history retained; same sample completes more coverage at measured higher expense; dismantling precision service preserves held batch safely.
Bounds: Bounded structure query and existing genetics budgets; one active precision job/service.
Evidence: rules, game

---

IF-037 Partial learning reduction and death rescue
Milestone: M6
Owner: equipment
Depends: IF-052
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: T4-09, T4-20
Scope: ["@module:equipment"]
Contract: Implement Trait Regulator ritual removing25 points from one selected eligible counter for catalog BU/XP/ingredients, never negative; Mnemonic Vessel stores explicit raw XP transfers. C-D actual-death learning unlocks one rescue; sleeping recharges rescue without clearing learned counters.
Red: Insufficient points/XP/output room, simultaneous deposit+ritual, logout and repeat confirmation cannot duplicate XP/charges; prevented death earns no actual-death count.
Accept: Reduce unwanted points and let another activity fill freed capacity; rescue fires once and remains spent after reload until qualifying sleep.
Bounds: One reservation per piece/Vessel;10000 raw XP/cell, at most4 joined cells and40000 total;20 XP/s player transfer or40 with Survey Gel while authorized UI is open within4 blocks. No orb scans or spawned XP.
Evidence: rules, game

---

IF-099 Complex-source DNA with precision storage
Milestone: M6
Owner: genetics
Depends: IF-037
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:genetics"]
Contract: Enable bank complex-storage service and run ordinary enderman plus Nether blaze/ghast/wither-skeleton sample routes through actual R1/R2 and native Vat. Use Block Catalog storage/native tables unchanged. No Wither/dragon support without Vault.
Red: Removing precision service retains complex records but makes them inactive; wrong native bed or full spent-water output refuses without consuming a sample.
Accept: Complete one complex genome and culture its stock; compare identical quality samples at R1/R2 exact BU/BE costs. Preview why boss samples remain refused.
Bounds: Existing DNA storage grades:16 ordinary/32 precision species slots plus8 per attached Lobe;16-record UI pages and one reserved batch/core. No Vault-sized implicit capacity.
Evidence: rules, game

---

IF-101 Dissector II with prepared diamond medium
Milestone: M6
Owner: equipment
Depends: IF-099
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:equipment"]
Contract: Implement Dissector II using Item Catalog's exact row: spider/silverfish/enderman stock, Diamond-Fiber Matrices, Binder and BU. Preserve Dissector I identity and damage; no combat alternatives or raw diamond shortcut.
Red: Missing Dissector I, incomplete any genome or insufficient prepared matrix refuses unchanged; no post-death quality reroll.
Accept: Culture all three stock sources, prepare the graft and upgrade the actual Fang; one qualifying kill offers one Pristine sample. Intact fallback remains Dissector I, not extra loot.
Bounds: Existing one-equipment transaction and global64 collection attempts/tick.
Evidence: rules, game, visual

---

IF-053 Bounded sequencing and readable production displays
Milestone: M6
Owner: control
Depends: IF-101
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T4-10, T4-11
Scope: ["@module:control"]
Contract: Scheduler Ganglion executes <=8 steps containing wait-state/start/ack/branch and optional bounded delay; no arbitrary script language. Display Membrane shows paged supplied readings. Schedule uses stable completion IDs, not redstone tick timing.
Red: Cyclic schedule yields after each step; held start never creates duplicate work; unload/reload resumes waiting on same batch, not replaying committed conversion.
Accept: Configure two alternative ore layouts, one shared processor and one parallel line; both remain reliable under changed processing speed.
Bounds: 1 schedule step/Ganglion/tick,64/server tick; <=8 steps and8 local bindings; display2 changed pages/s.
Evidence: rules, game, visual

---

IF-117 Reusable paid structure growth
Milestone: M6
Owner: construction
Depends: IF-053
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-20
Scope: ["@module:construction"]
Contract: Craft Structure Grower with its catalog recipe. Execute selected finite patterns from actual blocks or paid prepared grafts through Construction's placement interface. Support supplied stairs, flush light tissue and climbing tendons required by mines; no cutting or ore processing.
Red: Missing materials, blocked/protected/unloaded cell and restart during reservation cannot place free blocks or double-spend.
Accept: Build a small stair/light/tendon pattern, pause on an obstruction, resume after removal, and verify every supplied item and placed cell. Expose this same operation to excavation controllers.
Bounds: 4096 planned cells/job;16 placements/server tick shared with excavation; one retained job/core; no chunk tickets.
Evidence: rules, game, visual

---

IF-054 Finite walkable descending staircase
Milestone: M6
Owner: excavation
Depends: IF-117
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-21
Scope: ["@module:excavation"]
Contract: Use existing Surveyed Tissue/Imprint and Structure Grower. Descending Rootstock advances one finite staircase section with three clear blocks of headroom above every tread, actual stair blocks, Lumen inserts and continuous reachable access. Assemble the catalog's cutting/holding/collection organs; core only sequences their operations. Initial section16 treads,3 blocks wide,1 retained entrance and bottom landing.
Red: Unloaded boundary, fluid, protected block, missing light/access supply or full output pauses before unsafe cut; a second start cannot duplicate the section.
Accept: Player descends, climbs out, lights shaft and starts next one-shot section from a reachable landing. Show actual removed blocks and access costs.
Bounds: One <=512-cell approved work envelope/core; <=16 excavation/construction world changes/server tick shared across all mines; one retained plan/core.
Evidence: rules, game, visual

---

IF-055 Finite vertical shaft and descending platform
Milestone: M6
Owner: excavation
Depends: IF-054
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T3-11, T3-12
Scope: ["@module:excavation","src/main/java/org/jd/infestusfrontier/platform/mineral/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/mineral/**"]
Contract: Build the catalog Descending Cradle:3x3 Rib Frame deck, two Steam Muscles, Boring Jaw, Work Bed, Cilia and Grower. Select16 downward slices of a5x5 shaft:3x3 working deck plus side access/service clearance. Jaw performs individual real cuts through the mineral cutter port; Grower installs paid side Climbing Tendon and Lumen inserts every4 levels before deck descent. Move only the registered deck/payload identities, at most16 components; no arbitrary block entities. Platform moves one block only after next slice, cargo capacity, access and rider collision checks acknowledge. Retain a bottom landing and continuous climbable exit; use a fresh Cradle for another section.
Red: Lava, protection, unloaded next slice, full Bed/output, absent tendon/light supply or obstructed rider headroom pauses before movement. Crash/reload at cut/access/move boundaries preserves one core, one payload and each paid block; two cutters cannot own one cell.
Accept: Mine straight down16 levels, ride one admitted player safely with3 clear blocks above deck, climb out by tendon, and resume a paused slice after reload. Removal/shutdown leaves the shaft lit and climbable. This is a vertical shaft, not another staircase.
Bounds: At most512 selected world cells and16 known payload components/core; one slice pending; one rider/deck. Each removal/placement/relocation destination consumes the shared16 world-edit admissions/tick; loaded cells only, no free block-moving bypass.
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
Contract: Implement Recovery Sump, Neutralization Gland and Pressure Regulator with catalog containment rules. Enable intentional I019 placement only through finite spill admission. Ordinary full tanks refuse; only specified charged/damaged process ruptures. Excess volume remains in breached vessel. Own I084 Neutralizing Salts Cyst recipe and I085 retained Inert Sludge; neutralize250mB spent biomass +1 Salt over5s into1 Sludge +250mB dirty water, never reusable BU. Spent Penetrant/Leach Cake use1 Salt per item ->1 Sludge over5s, no metal or BU.
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

IF-119 Paid steam launch support
Milestone: M6
Owner: equipment
Depends: IF-058
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T3-16
Scope: ["@module:equipment","src/main/java/org/jd/infestusfrontier/platform/thermal/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/thermal/**"]
Contract: Craft Launch Bellows from real Steam Muscle, Rib Frames and slime block. Use the catalog's initial launch cost, velocity and cooldown. Reuse Thermal's paid work port and Equipment movement ceiling; no continuous flight.
Red: Blocked launch path, empty reserve, cooldown, unloaded source and duplicate activation each refuses unchanged. Client cannot authorize its own velocity.
Accept: Charge through actual steam supply, launch to a prepared safe landing and recharge; expose the crafted Bellows needed by Void Tether. Verify ceiling collisions never launch through solid blocks.
Bounds: One occupant/activation; inspect at most8 overhead cells; existing movement/thermal quotas; no chunk tickets.
Evidence: rules, game, visual

---

IF-059 End anchoring and first local nursery
Milestone: M7
Owner: spatial
Depends: IF-119
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
Scope: ["@module:cultivation","src/main/java/org/jd/infestusfrontier/platform/processing/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/processing/**"]
Contract: Chorus Resonator and Orchard Tissue implement native reference cultivation: real planting stock, local Dew water, powered growth, separate harvest and generation. Outputs remain physical fruit/stock, not spontaneous biomass. Install actual Spatial Membrane treatments into existing Dew/Sac recipes through their processing ports; preserve ordinary variants. Complete the powered orchard using supplied stored charge; the native Stack upgrade is IF-126.
Red: No End feed without seed/water/BE; repeating harvest callback cannot award a second fruit allocation; Overworld dimension refuses native recipe.
Accept: Harvest one actual120s native cycle with finite supplied water/charge; route two fruit to Dew and ten to Sac with exact native costs/yields, retaining basal flower. Do not assert positive whole-loop BE before IF-126 supplies native generation.
Bounds: Existing shared cultivation/recipe budgets; explicit fixed plot and finite reserve buffer.
Evidence: rules, game, visual

---

IF-126 Native End electrical conversion
Milestone: M7
Owner: energy
Depends: IF-060
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:energy"]
Contract: Upgrade the existing industrial Stack with2 Spatial Membranes and actual Anchor Root service in the End. Use the Items2000BU/100mB water/120s ->40000BE/100mB dirty-water recipe within its40000BE buffer. Add no new power/storage organ or generic dimension bypass.
Red: Remove dimension, membrane upgrade, native anchor or output reservation independently: refuse without fuel loss. First membranes must be obtainable with starter/imported charge, not this upgrade's own output.
Accept: Install upgrade using actual Spatial Nursery products; run three120s cycles of the assembled End reference module with paid resonator/pump/filtration loads. Assert2300 netBU/26000 netBE per cycle, preserve restart kit, then disconnect external supply and reload safely.
Bounds: One batch/Stack,40000BE output capacity,existing shared recipe and transfer quotas; no catch-up or chunk tickets.
Evidence: rules, game

---

IF-100 First End-native shulker research
Milestone: M7
Owner: genetics
Depends: IF-126
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:genetics","src/main/java/org/jd/infestusfrontier/platform/cultivation/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/cultivation/**"]
Contract: Install End native Extractor/Vat tolerance with exactly2 I091 Spatial Membranes +400BU per core at L2 on the End3x3 bed with4 Anchor Roots. No Conditioner output, levitation culture or R3 required. Species/native/storage conditions follow Block Catalog. Also enable ordinary End-native chorus genome/stock from a consumed chorus fruit specimen and attach I052 chorus trait recipes to the existing Bench via cultivation adapter; complete16-coverage plant genome required.
Red: Remove dimension, bed, anchor, organ level or spatial treatment independently: refuse unchanged; first-shulker route cannot require existing shulker stock.
Accept: Reconstruct first Shulker with R1/R2 precision storage, then culture matching stock locally; I097 names that stock's levitation role, not an extra material.
Bounds: Existing native genetics, one-batch and shared structure budgets.
Evidence: rules, game

---

IF-061 Spatial conditioning and levitation work service
Milestone: M7
Owner: spatial
Depends: IF-100
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T5-06, T5-07, T5-12
Scope: ["@module:spatial"]
Contract: Spatial Conditioner creates bioactive spatial intermediates; Levitation Chamber supplies paid recipe service; Phase Isolator bounds phase process to formed structure. No free cargo teleport or player flight in these organs.
Red: Missing phase isolation, native bed, coolant or energy independently refuses unchanged; unloaded service cannot count as formed.
Accept: Manufacture I092 Passenger Membrane, I093 Cargo Membrane, I094 Precision Membrane and I095 Phase-Woven Matrix through their exact native recipes; feed Levitation Chamber real I097 shulker stock from IF-100. No R3 required. UI/control report actual service state.
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
Contract: Extend the existing End-native lab with R3 only: Extractor L3, R2 plus Ion Separator/Levitation Chamber/Cold Lobe, additional400BU+16000BE+1 shulker stock+100mB cooling/sample; coverage quality*8, stock yield1. Native tolerance continues to use I091 Spatial Membranes from IF-100; no duplicate recipe or Spatial Fiber item.
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
Blocks: none
Scope: ["@module:equipment"]
Contract: Implement C11 wing tree using a real consumed Elytra and canonical Armor Evolution rates/caps, mutually exclusive with C8. Reuse existing Launch Bellows; test existing locomotion effects but do not duplicate the L/B families owned by IF-078/079. Preserve the armor-table exhaustion fallback.
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
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md, docs/ARCHITECTURE.md
Blocks: T6-03, T6-04
Scope: ["@module:transit","core/src/main/java/org/jd/infestusfrontier/storage/freight/**","core/src/main/java/org/jd/infestusfrontier/storage/api/**","core/src/test/java/org/jd/infestusfrontier/storage/freight/**","src/main/java/org/jd/infestusfrontier/storage/freight/**","src/testMod/java/org/jd/infestusfrontier/testmod/storage/freight/**"]
Contract: Freight Gullet/Cargo Lock move bounded real items, fluids or BE through explicit links. Use the Freight persistence protocol in Architecture: storage owns durable Cargo Lock balances/escrow; transit owns link policy and state transitions. No distributed global inventory illusion. Native production checks stay where recipe runs.
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
Blocks: T6-08, T6-09, T6-10
Scope: ["@module:construction"]
Contract: Foundation Cyst extends the existing Structure Grower with paid larger service patterns from visible markers; Service Pedestal binds existing services; Reclamation Mouth recovers supported installed parts without resetting OrganHistory. Growth never copies another core's identity.
Red: Insufficient materials, protected/unloaded cell or blocked pattern pauses before placement; moving leveled core cannot yield both original and replacement.
Accept: Expand a workshop with a side automation bay and move a leveled organ to a new base; each removed cell/material accounted; no auto-resume construction in unloaded chunks.
Bounds: <=4096 planned cells/job,16 world placements/server tick shared with excavation; one retained job/core.
Evidence: rules, game, visual

---

IF-070 Cocoon and Recall preservation
Milestone: M8
Owner: equipment
Depends: IF-069
Spec: docs/ARMOR_EVOLUTION.md, docs/ITEM_CATALOG.md, docs/BLOCK_CATALOG.md
Blocks: T6-11
Scope: ["@module:equipment"]
Contract: Implement permanent M5->M6 branches and Recall Nest4 berths. Default armor remains ordinary despawning loot. M5 protection and M6 recall costs/fallback follow Armor Evolution; stored charges are not tank fuel. M7 Death Bond is IF-111 after actual Wither research.
Red: Death, logout, respawn/restart and full/unloaded berth leave exactly one owner per piece; default armor is not protected. Reject Death Bond installation until IF-111 provides its complete route.
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
Contract: Two-client packaged fixture performs simultaneous cargo requests, endpoint unload/reload, mid-transfer stop/restart, armor cocoon and recall death. Record resource ownership at each durable transition.
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
Scope: ["@module:husbandry","src/main/java/org/jd/infestusfrontier/platform/cultivation/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/cultivation/**"]
Contract: Aquaculture Bed grows allocated aquatic harvest from real starter/feed; Fishing Polyp performs a paid fishing operation only against valid loaded water. Use declared loot tables with explicit nonrenewable exclusions; no AFK loose-item stream. Attach the existing I052 kelp traits to Grafting Bench via its cultivation port; test both modifiers on actual Aquaculture harvests, retaining source/seed identity.
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
Blocks: T2-10, T4-08
Scope: ["@module:nutrition"]
Contract: Ration Kitchen cooks real ingredients; reuse Brewing Gland's vanilla potion inputs/containers; Potion Infuser prepares the listed biological carriers. Keep food, player healing, armor feeding and biomass digestion distinct. All returned bottles reserve space.
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
Contract: Complete H1/H2/H3/H4/H5/H6/H7/H9/H10 rank effects from Armor Evolution through the shared equipment capability and cooldown engine. H2.I remains implemented by lighting; add obtainable H2.II/III recipes and reuse its effect engine. H8 remains owned by burrowing; test compatibility without reimplementation.
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
Contract: Implement C1/C2/C3/C9/C10/C14 plus obtainable C6/C12/C13 ranksII/III and Restorative Tissue through the existing equipment service and fuel-priority interfaces. Capacity is not fuel generation; C3 heals the player while M1 heals armor; food reserve wins a same-slot race with C13. Berserk follows the exact C14 burst, paid-hit, incoming-risk and cooldown rules; test every rank including unequip/reload and unpaid-hit refusal.
Red: C9 and C13 cannot eat the same item; healing stops at full health and respects recent hostile damage; a full recipient tool stops service transfer. C14 separately proves fully charged hit admission, insufficient activation/per-hit fuel refusal and persistent90s cooldown.
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
Contract: Complete M1-M4 ranks and C4/C7 alternatives plus obtainable C5.III from Armor Evolution. Residual physical/explosion reduction is capped20% with row-specific per-hit limits; cannot install both inflatable cavity plans.
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
Accept: Test every rank on stairs, water, magma and controlled falls; compare rigid/flexible frames and exact ceilings. Construct B7.II plus L8 on an actual complete suit and verify IF-025 Travel Tissue's positive path and immediate loss of bonus on unequip.
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
Contract: Implement Brood Nursery with3 distinct collision-free larva berths and Worker Waypoint. Mutation consumes a larva and real graft to create one assigned helper. Persistent population leases count loaded and unloaded workers. Death/expiry releases the matching ID exactly once; unload pauses lifetime and retains its lease. Normal station removal refuses while leases remain; explicit retirement ends loaded idle workers after cargo handoff, and pending unloaded workers retain their slots until loaded and retired. Idle workers return to assigned berth.
Red: Kill all workers then hatch again: slots free exactly once. Unload workers then request new hatches: quotas remain occupied; reload cannot duplicate leases. Occupied berth cannot spawn overlapping larvae; simultaneous requests cannot exceed3.
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

IF-109 Assigned harvest-helper profession
Milestone: M9
Owner: helpers
Depends: IF-081
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:helpers"]
Contract: Implement I066 Harvester Graft using Items recipe at existing Chrysalis and mutate an actual larva into a permanent harvester profession. Assign one existing cultivated plot, compatible held tool and destination through existing waypoint/job service. Invoke cultivation's harvest transaction, never duplicate growth or drop logic.
Red: No tool, exhausted tool, full destination, occupied target or second same-crop claim preserves crop and reservations. Death cannot duplicate carried harvest.
Accept: Helper walks stairs to assigned crop, harvests one ripe allocation, delivers via existing courier transfer and returns to berth. Compare against stationary Corolla; no unassigned work search.
Bounds: Existing3/nursery,24/team,96/server population,32 route waypoints and4 path requests/server tick.
Evidence: rules, game, visual, soak

---

IF-082 Nutrient Sail and paid chunk ownership
Milestone: M9
Owner: helpers
Depends: IF-109
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
Blocks: T2-16
Scope: ["@module:defense"]
Contract: Implement Aerocyte Bloom and its shared targeting/projectile service at first use: hostile flying targets only, one green projectile/shot, actual reservoir/vein fuel. Baseline range24, one shot/40ticks, damage4 health points,5BU/shot; no profitable projectile drops.
Red: Players/tamed pets never become targets; empty fuel, blocked muzzle, dense entities and unloaded target refuse shot; projectile hit cannot pay biomass twice.
Accept: Reservoir supplies Bloom through raised junction. Phantom/blaze targets take exactly one hit per projectile; ground-only hostiles are not selected. Inspect trajectory/pulse direction and run60s at128-projectile admission limit without retained growth.
Bounds: <=24-block query radius,16 returned candidates/query,64 defensive queries/server tick; <=128 active colony projectiles/server and one shot/organ/40ticks baseline.
Evidence: rules, game, visual, soak

---

IF-103 Spine Sentry ground defense
Milestone: M9
Owner: defense
Depends: IF-083
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-30
Scope: ["@module:defense"]
Contract: Reuse targeting/projectile service for ground-hostile Spine Sentry. Range16blocks, damage4 health points, one shot/40ticks,2BU+1 I068 Grown Spine/shot. Prepare I068 via existing Loom recipe; bones/plates must first become spines.
Red: Flying-only targets, players/pets, obstructed muzzle or insufficient ammunition/fuel cannot spend or fire; duplicate hit delivers no second damage.
Accept: Feed through reservoir/vein and compare grounded zombie selection against Bloom's phantom selection; inspect original model and projectile; simulate sixty seconds of shared-cap contention.
Bounds: Shared128 projectile cap and64 target queries/tick;16 candidates/query.
Evidence: rules, game, visual, soak

---

IF-104 Repellent Crown territorial exclusion
Milestone: M9
Owner: defense
Depends: IF-103
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-17
Scope: ["@module:defense"]
Contract: Implement hostile-only Repellent Crown with8block radius, one pulse/40ticks,5BU+1 I046 Scent Concentrate/pulse. Pulse requests local path movement away for40ticks; cannot teleport, alter ownership or affect bosses. Reuse bounded targeting.
Red: No path, unloaded escape cell, boss, player/pet or missing supply produces no forced movement or repeated charge.
Accept: Zombie leaves reachable radius while player/pet/boss ignores pulse; blocked exit reports blocked rather than rescanning. Removal/reload clears transient influence. Capture pulse; sixty-second contention shares target budget.
Bounds: 16 candidates/query;64 shared defense queries/tick; one transient expiry per affected entity, no persistent history.
Evidence: rules, game, visual, soak

---

IF-105 Lure Polyp local target attraction
Milestone: M9
Owner: defense
Depends: IF-104
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-18
Scope: ["@module:defense"]
Contract: Implement hostile-only Lure Polyp:8block radius,5BU+1 I046 Scent Concentrate/40tick pulse; attract at most one non-boss hostile to an accessible adjacent cell for40ticks. No spawns, chunk search, teleport or guaranteed override of an active attack target. Nearest then entity-ID tie-break.
Red: Unreachable berth, current attack target, player/pet/boss or exhausted feed refuses lure without spending; duplicate pulses cannot queue paths.
Accept: Idle zombie approaches reachable polyp while an attacking zombie keeps its target; removal expires influence. Capture60s shared-budget contention with Crown; newest equal-tick conflict resolves by block-position order.
Bounds: One target/polyp and one local path request/40ticks; at most4 defense path requests/server tick.
Evidence: rules, game, visual, soak

---

IF-106 Restraining Tissue with paid hostile contact
Milestone: M9
Owner: defense
Depends: IF-105
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T2-19
Scope: ["@module:defense"]
Contract: Implement Restraining Tissue using I048 graft preparation and actual substrate mutation. A grounded hostile on supplied tissue receives20% movement reduction for20ticks, once/target/20ticks for2BU. Player/pet/boss unaffected. Contact strength never stacks across cells; no web block placement.
Red: Two adjacent cells cannot charge/slow the same target twice in one cadence; no biomass, full global admission or partial suit does not change target eligibility.
Accept: Walk zombie across joined cells and stairs, leave tissue and verify expiry; players/pets retain movement; supply through real vein. Inspect flush surface and60s dense-contact contention.
Bounds: Shared64 defense queries/tick and16 candidates/query; one transient expiry/target, no persisted victim log.
Evidence: rules, game, visual, soak

---

IF-113 Husbandry scent and restraint treatments
Milestone: M9
Owner: defense
Depends: IF-106
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:defense","src/main/java/org/jd/infestusfrontier/platform/genetics/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/genetics/**"]
Contract: Prepare and install I053 Organ Trait Grafts for the catalog-defined rabbit, cod, cow, pig, sheep and chicken Lure treatments and spider/slime Restraining Tissue treatments through the existing prepared-stock service. Reuse defense targeting, influence and supply interfaces; no independent animal ticker or new item family.
Red: Wrong species, incomplete genome, occupied/full pen, disabled nerve input, existing player lure/attack intent and duplicate treatment each refuses without double charge. Tamed animals and players remain excluded.
Accept: Prepare each treatment through real Chrysalis processing, attract its supported animal through a reachable pen entrance and stop at its capacity signal. Independently test duration and strength restraint branches, nonstacking overlap and escape after loss of fuel.
Bounds: Shared64 defense queries/tick,16 candidates/query,4 path requests/tick; one transient influence/target; no spawning, breeding, chunk loading or global animal scan.
Evidence: rules, game, visual

---

IF-084 End targeting and selective physical membranes
Milestone: M9
Owner: defense
Depends: IF-113
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T5-09, T5-10, T5-11
Scope: ["@module:defense"]
Contract: Targeting Eye supplies one loaded local target to attached defense. Selective Membrane follows T5-10 authorization/power/emergency rules; no added armor prerequisite. Catching Membrane catches actual colliding item entities into an attached Capsule, retaining refused overflow in its own9-stack buffer. Remove only the count successfully reserved. At full net/capsule leave entity ownership unchanged. Elastic Gel treatment additionally cushions an ordinary living-entity fall by subtracting3 blocks from damage distance once per landing; no fuel-driven aerial reset, remote rescue or flight.
Red: Unauthorized entry is blocked while powered; occupied doorway, emergency-open and power loss cannot trap an entrant; full Capsule/net and duplicate contact callbacks cannot delete/clone items. Breaking/reloading net retains one inventory. Repeated collision while airborne cannot reset fall distance.
Accept: Authorized/unauthorized passage, power draw and fail-open behavior match T5-10 independent of armor. Drop actual items over net and collect through Capsule; repeat with full Capsule, overflow and dismantling. Compare ordinary and Gel-treated landings; Eye uses the existing target/projectile service.
Bounds: 9 retained stacks/net section; shared64 contact/collection admissions/tick, at most16 candidates/query; passive collision callbacks, no dropped-item world scan or chunk loading.
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
Contract: Implement the two exact Compound-organ commissioning recipes and I101 source-specific Catalyst Culture from Items; I100 aliases the primary donor item in its compound-body form. Attach separate physical Anatomy Sockets and Catalyst Lobes via service ports. Preserve both original donor identities/history; no donor copies or universal super-machine. Initial Economy profile belongs here; Throughput installation/switching is IF-087.
Red: Wrong donor/level/native bed, missing actual culture/feed or reserved outputs each refuses before consumption. Repeated completion/dismantling cannot leave packed body plus donor copies; isolating one socket cannot debit another.
Accept: Commission both catalog bodies from actual donors and three-world intermediates. Compare separate physical layouts, run Economy-profile batches and show independent supplies/results/history. Deplete the Lobe then refill to resume paid service without duplicating remaining service ticks.
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
Contract: Install Throughput profile and switch Economy/Throughput on the two existing compound bodies using Items' exact costs, delays and rounding. Reject all unsupported profile pairs and armor targets. Organ Transplanter moves one real Bio-Furnace/Gizzard core into a matching empty socket using the listed transaction; construction owns placement, organ owns history, synthesis owns reservation.
Red: Working/undrained organ refuses switch; rollback cannot refund spent waste; donor and recipient cannot both retain history after restart or interrupted move.
Accept: Run identical actual input batches under each profile and compare exact unchanged yields, cost/time tradeoff, switching downtime and paid catalyst upkeep. Transplant both supported core types and verify same identity/counts, output reservations and compatible structure; no copying a packed compound donor.
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

IF-102 Boss genomes in native laboratories
Milestone: M9
Owner: genetics
Depends: IF-088
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:genetics"]
Contract: With the existing Genome Vault, enable Wither and Ender Dragon knowledge and matching native extraction/culture using unchanged storage/native tables. Physical boss loot is still required; higher lab quality changes coverage only.
Red: Ordinary/precision Bank refuses exotic records; Wither in Overworld and dragon outside End refuse without consumption; record import cannot create stock.
Accept: Consume actual labeled boss specimens at R1/R2/R3 with1000 coverage target, complete both genomes using bounded counted fixtures, and culture stock only in their native laboratories. No need to spawn1000 bosses.
Bounds: Existing128-species Vault, bounded sample counts, one-batch and global completion budgets.
Evidence: rules, game

---

IF-111 Death Bond after native boss research
Milestone: M9
Owner: equipment
Depends: IF-102
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: none
Scope: ["@module:equipment"]
Contract: Implement all M7 ranks from Armor Evolution after M5 III and actual Wither stock exist. Prepay50/35/25kBU per piece; stored death charge is not tank fuel. M6/M7 remain mutually exclusive descendants.
Red: Death+logout+respawn/restart commits one owner; cannot leave both equipped and dropped pieces. Missing boss genome/stock or sibling Recall branch refuses unchanged.
Accept: Prepare, charge, die, respawn and recover the bonded piece with actual native materials; cooldown and fallback follow armor tables; unrelated inventory drops normally.
Bounds: 4 pieces/death;4 pending bonded slots/player; no retries or chunk tickets.
Evidence: rules, game, integration

---

IF-089 Temporary ground and air interceptors
Milestone: M9
Owner: defense
Depends: IF-111
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T7-07
Scope: ["@module:defense","src/main/java/org/jd/infestusfrontier/platform/helpers/**","src/testMod/java/org/jd/infestusfrontier/testmod/platform/helpers/**"]
Contract: Implement ground/air Interceptor Nursery specializations exactly under Block Catalog T7-07 deployment costs, movement, targets, damage and lifetime. Consume actual larvae and matching stock through Catalyst service. Reuse helpers population leases and path jobs; defense owns attack selection. Offspring cannot yield specimens, XP, ordinary loot or tissue biomass.
Red: No feed/berth/support means no deployment. Death/expiry releases one lease; unload retains it. Exceeding path/population quota waits without spending. Players/pets/bosses never targeted; death cannot farm DNA or biomass.
Accept: Produce zombie/phantom stock, commission each permanently specialized Nursery and deploy into distinct berths. Ground navigates stairs; air navigates collision-clear local routes. Measure shared contention with existing workers, expiry/support loss and re-hatching after death.
Bounds: 3 defenders/Nursery within existing24/team and96/server total helper leases;2400 loaded ticks life; existing4 defense path requests/server tick,16 candidates/query and64 queries/tick. No chunk tickets.
Evidence: rules, game, visual, soak

---

IF-124 Paid siege shots and permanent targeting grafts
Milestone: M9
Owner: defense
Depends: IF-089
Spec: docs/BLOCK_CATALOG.md, docs/ITEM_CATALOG.md
Blocks: T7-08
Scope: ["@module:defense"]
Contract: Implement Siege Blossom's exact T7-08 shot, cooling and three permanent graft recipes. Reserve ammo, BU, BE, steam and cooling outputs before projectile admission. Reuse hostile targeting/projectile service; separate supplier organs remain necessary. Prepare grafts through existing Chrysalis/Chamber ports, not direct raw-item clicks.
Red: Blocked muzzle, protected/nonhostile target, full spent-fluid output or projectile ceiling refuses before spending. Duplicate hit callback cannot damage twice; unsupported sibling graft cannot replace installed branch; secondary targets obey exclusions.
Accept: Feed real Spine/charge/steam/water through separate routes, fire base and each specialization at controlled eligible targets and measure exact damage/range/reload/cost. Verify no terrain damage, projectile recovery or bypass of vanilla boss immunity; compare with cheap Sentry.
Bounds: Shared128 projectiles/server,64 target queries/tick and16 returned candidates/query; suppression at most4 total hit entities;60-tick projectile lifetime.
Evidence: rules, game, visual, soak

---

IF-090 Catalog, guide and alternate-route completeness
Milestone: M10
Owner: campaign
Depends: IF-124
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign","scripts/check_content_coverage.py","build.gradle"]
Contract: Audit and complete the existing incremental executable coverage manifest for every in-scope block/item/armor rank and guide prerequisite. Recipes generated by each owner must be reachable without creative items. Verify two materially different supported refinery/base/armor routes; exclusions are the proposal/Fold entries in the implementation context, not missing approved branches.
Red: Delete one recipe, rename an item ID, omit a guide parent or leave a graft effect unimplemented: coverage fails at exact entry.
Accept: All in-scope catalog entries have registry/obtain/use/recipe/guide assertions, not empty registration checks. Record missing branch as failing implementation, never delete catalog entry to pass. Only add this deterministic coverage check to verifyAll in build.gradle; do not alter existing gates or dependencies.
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
Contract: Extend IF-001 profileSmoke/captureClient/packagedServerSmoke for the same required/jei/curios/combined four-profile matrix, using a dedicated server and two disposable clients. Missing required libraries remain negative fixtures, never a supported fifth profile. Validate inventory capability simulation/commit against vanilla and synthetic hostile handlers; persist schema fixtures across each production schema version. No blanket claim for untested content mods.
Red: Missing optional mod, malformed packet/NBT, stale menu, malicious count, version mismatch and client-only class on server each has explicit assertion.
Accept: Original art loads without missing textures; joins, guide grants, transfers, armor and DNA work in all supported profiles; released JAR contains no test fixtures.
Bounds: Packet and collection caps from Performance are asserted under concurrent clients; fixed process/readiness deadlines.
Evidence: rules, game, visual, integration

---

IF-092 Dense three-world soak and recovery gate
Milestone: M10
Owner: campaign
Depends: IF-091
Spec: docs/PROGRESSION_MAP.md, docs/PERFORMANCE.md
Blocks: none
Scope: ["@module:campaign","build.gradle"]
Contract: Implement the dense three-world stress harness with maximum declared organs/entities/spills and a short deterministic stress regression in verifyAll. Add separate qualification command running three30-minute fixed-seed trials against an equivalent idle baseline. This packet implements the harness; IF-107 runs long qualification against the frozen candidate. Commands: ./gradlew stressRegression (verifyAll dependency) and ./gradlew qualifyColony -PtrialSeeds=11,29,47 -PidleSeconds=300 -PactiveSeconds=1800 -PoutputDir=<evidence>. Add only these harness hooks to build.gradle; never relax other gates.
Red: At every configured ceiling, one excess operation refuses/defer safely; forced unload/restart does not grow queues, lose escrow or duplicate fuel.
Accept: Short runs verify calibrated timing/percentile collection, resource ledgers, warm-up exclusion, retained-job/chunk/memory accounting and injected budget failures. Publish exact named command/options and hardware metadata schema for IF-107; do not claim90-minute qualification from short tests.
Bounds: Hard budgets from owning services remain authoritative; no global cap silently raised to pass throughput; measured host-dependent target recorded separately.
Evidence: rules, game

---

IF-107 Qualify the frozen dense three-world harness
Milestone: M10
Owner: campaign
Depends: IF-092
Spec: docs/PROGRESSION_MAP.md, docs/PERFORMANCE.md
Blocks: none
Scope: ["@module:campaign"]
Contract: Add a fixed qualification scenario/profile under testMod campaign resources, then freeze candidate and run the existing harness for three fixed-seed trials, each5 minutes idle plus30 minutes active; exclude first60s idle and first5 minutes active from metric summaries. No harness design or production edits. Record soak with a7200s inner deadline; task worker budget10800s.
Red: An intentionally impossible tick-cost threshold makes the same qualification result evaluator fail at its named metric; use a short run to demonstrate that assertion before the final profile.
Accept: All three runs report p50/p95/p99 MSPT, memory/chunk counts and retained jobs. Require<=5ms p95 incremental mod tick cost and no upward retained-job growth after warm-up. Test artifacts name final candidate, hardware, seeds and run durations. Any edit invalidates all qualification receipts.
Bounds: Existing server quotas unchanged; finite126000 loaded ticks total (105 minutes), including the matched idle references. Task budget10800s; no production tuning.
Evidence: rules, game, soak

---

IF-093 Full Survival campaign and player-facing release candidate
Milestone: M10
Owner: campaign
Depends: IF-107
Spec: docs/PROGRESSION_MAP.md, docs/GUIDE_PROGRESSION_TREE.md
Blocks: none
Scope: ["@module:campaign"]
Contract: Implement reusable campaign action fixtures, then execute fresh-world Survival route through Overworld workshop, Nether export base, End self-supply and three-world synthesis using real actions. Validate second layout and distinct armor lineage. Capture screens/structures/all model faces in actual client; smart reviewer judges visibility and clipping, not only pixel diffs.
Red: Break a native bootstrap link, hide a required recipe or invert a logistics face and playthrough must fail at named action. No direct grants after starting fixture.
Accept: Both routes finish without administrative unlocks. Measured failures remain failing and return to the coordinator for a narrowly owned tuning/fix task; this campaign packet cannot change production recipes or balance. Required gameplay/features complete; no automatic public release/license grant.
Bounds: Finite scripted checkpoints with per-action timeouts and conservation ledgers; no real user saves. Final gate includes multiplayer and dense-base results.
Evidence: rules, game, visual, integration, soak
