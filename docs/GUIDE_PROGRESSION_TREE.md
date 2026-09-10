# The Waking Genome — progression and advancement tree

Draft of the complete proposed guide, from the first culture through the three-dimension campaign and the long-term Fold chapter. This specifies **what opens a page, what completes its advancement, and what becomes possible next**. It is not the current contents of the in-game book.

Block IDs and names refer to [Block Catalog](BLOCK_CATALOG.md). Tissue rules follow [Living Substrate](LIVING_SUBSTRATE_MUTATIONS.md); equipment requirements follow the newer [Armor Evolution](ARMOR_EVOLUTION.md), including its per-piece counters and bioactive fusion materials. The tree does not replace those recipes or numerical tables. [Item Catalog](ITEM_CATALOG.md) owns prepared reagents and graft production. Exact milestone recipes and numeric gates remain proposals; confirmed owner constraints are not optional alternatives.

## 1. How the tree works

The book is a branching map, not a list of mandatory quests. Completing a parent opens its children. Some children have several parents; some accept alternative parents. An unlocked page explains the task **before** the player must perform it, including its recipe, minimum structure, supplies, controls and failure conditions.

Every table below uses the same four columns:

- **Node:** stable ID and the title shown in the book. `T2-06`, for example, is also the Mutation Chamber's catalog ID.
- **Open after:** completed prerequisite nodes. `ALL(A,B)` requires both; `ANY(A,B)` accepts either. A single ID requires that one parent. `START` has no prerequisite.
- **Complete by:** the observable action that earns the advancement. Merely reading a page is never evidence of building, operating or understanding a machine.
- **Teaches / unlocks:** the new construction, behavior or choice explained by that node. Its child pages open when it completes; their precise dependencies are in their own rows.

### Book visibility is not the same as permission to use an item

1. **Guidance:** a node's parents control the order of full instructional pages. Direct children of an open page remain visible as named previews, with their prerequisites. Search/JEI can show ingredients, safety requirements and the path to unlock a future page; the book must not hide the information needed to reach it.
2. **Colony capability:** the rank badges `SR0–SR9` unlock commissioning templates for their chapter's new organ families. Placement, ordinary movement, vanilla portals and vanilla equipment are not rank-locked. Basic safety/configuration remains accessible even for a borrowed advanced machine.
3. **Actual operation:** a recipe still needs its materials, valid body, native bed, power and relevant genome. Armor also needs the **individual piece's** frame, counters and anatomy. A guide checkbox supplies none of these.

An ordinary node is not an additional secret crafting lock. Acquiring a valid item early reveals its own instructions and credits possession-based nodes; operation-based nodes need actual operation. A borrowed mature armor piece keeps its learned abilities, but does not transfer its previous wearer's body adaptation. Building it personally is not mandatory to wear it. Biological commissioning of a new advanced organ remains tied to its chapter license and physical requirements.

**Ranks are earned milestones, not XP bars.** They never erase on death, travel or machine removal. Armor counter rank R3 and colony rank SR3 are different things. No badge grants materials, free armor levels or a permanent all-purpose damage bonus.

### Completion and cooperation

- Use successful recipe, mutation, transfer and commissioning events. Obtaining ingredients reveals a recipe; it does not pretend that the player already ran it.
- Qualifying work done before its page opens is retained as bounded evidence and reconciled when the prerequisites complete. The player should not rebuild a working organ because they opened the book late.
- A shared workshop can credit its consenting participants. Each participant deliberately binds to the commissioned installation; standing elsewhere on the server is not participation. This is a proposed opt-in colony journal, not a dependency on FTB Teams or FTB Quests.
- Completing a genome unlocks that genome's recipes in the connected research context; a journal badge does not create consumable Genetic Stock or remote DNA Bank access.
- Advancements recognize first achievements. Repeating a recipe produces its normal output, not repeatable quest rewards.
- No required node demands injury, a deliberate spill, a mob flood, every genome, every suit, every miner, drones or a chunk loader.

## 2. Main path

```text
SR0 Contact
 ├─ first biomass + membrane + bone + storage → SR1 Circulation
 │   ├─ autonomous processing + replenishable feed → SR2 Directed Mutation
 │   │   ├─ one complete genome + one useful expression → SR3 Thermal Colony
 │   │   │   ├─ native Nether production + tempered plates → SR4 Precision
 │   │   │   │   ├─ precision process + controlled shutdown → SR5 End Colony
 │   │   │   │   │   ├─ native End production + conditioning → SR6 Distributed Colony
 │   │   │   │   │   │   ├─ three working sites + combined production → SR7 Synthesis
 │   │   │   │   │   │   │   ├─ compound organs and optimization [three-world capstone]
 │   │   │   │   │   │   │   └─ prepared Fold expedition → SR8 Foreign Symbiosis
 │   │   │   │   │   │   │       └─ local native production + adaptation → SR9 Manyfold
 │   │   │   │   │   │   │           └─ deliberate encounter → maintained boss culture
 │   │   │   │   │   │   │               └─ reciprocal designs; no SR10 god tier
 │   │   ├─ genetics: agriculture / husbandry / equipment / defense
 │   │   └─ mining: manual treatment / descending roots / later shafts and strips
 │   └─ circulation: farms / trees / storage / routing / local controls
 └─ living equipment, construction and manual leaching remain useful throughout
```

The arrows describe dependencies, not one required base layout. Optional branches can be pursued in parallel and revisited after later discoveries. The SR7 capstone stands on its own if the Fold expansion is not installed or not yet released.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| SR0 — Contact | START | Begin a world; initialize the personal guide journal. | T0 starter recipes and the first pages. |
| SR1 — Circulation | ALL(T0-03,T0-04,T0-05,T0-06) | Automatic badge after first biomass production, usable storage, a Membrane Sheet and a Bone Plate are recorded. | T1 circulation, farm and expandable-body commissioning. |
| SR2 — Directed Mutation | ALL(CI-01,CI-02) | Commission one supplied processing line and its replenishable organic feed. | T2 laboratory, directed genetics, planned excavation and optional helpers. |
| SR3 — Thermal Colony | ALL(GE-02,GE-04) | Complete one genome and use a researched trait in a useful plant, organ or equipment expression. | T3 native thermal and steam construction. No requirement to evolve a whole armor set first. |
| SR4 — Precision | ALL(T3-09,TH-01) | Produce tempered plates and commission continuing Nether-native lining production. | T4 electrical and precision commissioning. Steam transport is useful but not a compulsory parallel factory. |
| SR5 — End Colony | ALL(EL-01,EL-02) | Run one real precision process and demonstrate its controlled stop/resume with supplies retained. | T5 native End installations and spatial growth. Vanilla End entry remains independent. |
| SR6 — Distributed Colony | ALL(T5-06,EN-03) | Condition a spatial batch and commission recurring production in the End. | T6 stations, freight, district storage and services. |
| SR7 — Synthesis | ALL(TR-01,TR-02) | Connect the output of three working dimensional sites and finish a product using their contributions. | T7 compound organs; three-dimension capstone; optional Fold preparation. |
| SR8 — Foreign Symbiosis | ALL(T7-09,FO-01) | Establish a supplied first Fold foothold with its prepared return route. | T8 native Fold construction; no boss material required. |
| SR9 — The Manyfold | ALL(FO-02,FO-03) | Sustain local nursery production and make a useful native adaptive material. | T9 encounter preparations. The boss is not summoned automatically. |

## 3. Contact: the first working patch

The initial book is offered once on first join, retained as a pending claim if inventory is full. Its first page is also reachable through a rebindable guide action, so losing the item does not block discovery. A replacement physical book is crafted from **one ordinary book + one Spore Culture**. No claim creates loose items on the ground.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| ST-00 — The Waking Genome | SR0 | Open the guide or receive its first copy. | Starting ingredients, controlled growth and how to read prerequisites. No gameplay capability depends on reading this page. |
| ST-01 — A Culture in Your Hand | SR0 | Obtain Spore Culture; its recipe is rotten flesh + red mushroom + wheat seed. | Apply culture to exposed ground; replacement-book recipe. |
| ST-02 — Speak to an Organ | ST-01 | Craft a Synaptic Probe (bone + slime ball + Spore Culture) and use it on a valid target. | Deliberate configuration; input/output/closed faces; status and safe pause. Empty hands remain available for building. |
| T0-01 — Living Substrate | ST-01 | Convert one exposed eligible ground cell. | Basic → young → mature; selected cells only, no endless autonomous expansion. |
| ST-03 — A Mature Foundation | T0-01 | Deliberately mature one selected cell through its two growth treatments. | Functional tissue grafts; variety does not require destroying its vegetation. |
| T0-02 — Culture Bowl | T0-01 | Run one rooted culture batch. | Renewable construction cultures and the slow early Fusion Binder recipe. |
| T0-16 — Organ Bud | ST-01 | Craft or grow one bud. | Craft an organ as an item or grow it from a planted bud with the same ingredients. |
| T0-03 — Digestive Sac | ALL(T0-01,T0-16) | Digest one organic batch into retained biomass. | First fuel; woody-feed limits; an output-full pause rather than spills. |
| T0-04 — Biomass Bladder | ALL(T0-03,T0-16) | Store biomass and deliver a measured amount to a starter organ. | Buckets/direct connections, visible fill and the smaller leather construction option. |
| T0-05 — Membrane Rack | T0-01 | Produce a Membrane Sheet from supplied organic material, string and water. | Flexible building skins, equipment and organ walls. |
| T0-06 — Bone Loom | ALL(T0-03,T0-16) | Grow a Bone Plate from real calcium feed. | Structural parts; biomass binds mineral rather than creating it. |
| T0-07 — Bio-Furnace | ALL(T0-03,T0-05,T0-16) | Complete a normal smelting recipe. | A weak hand-fed furnace that can retain its history through later growth and multiblock expansion. |
| T0-08 — Awakening Cradle | ALL(T0-05,T0-06,T0-16) | Form the cradle and awaken one dormant equipment piece. | Early living armor/tools and host-grown rank-I grafts without a genetics laboratory. |
| T0-09 — Seed Pouch | T0-05 | Deposit planting stock and set a reserve. | Keep replanting material separate from food and digestion. |
| T0-10 — Living Skin | T0-05 | Place a small enclosure using at least two compatible shapes. | Biological walls, stairs and rounded corners; ordinary building remains unrestricted. |
| T0-11 — Rib Frame | ALL(T0-05,T0-06) | Construct one supported arch or a valid organ-frame corner. | Shared structural vocabulary for later multiblocks. |
| T0-12 — Membrane Window | T0-05 | Join two windows into one readable membrane surface. | Connected transparent walls, tinting and later lining variants. |
| T0-13 — Lumen Tissue | ST-03 | Graft one selected floor, wall or ceiling cell with light. | Flush illumination; grow Lumen Secretion in the Bowl from glow ink, glow berries or glowstone. |
| T0-14 — Leaching Gland | ALL(T0-03,T0-05,T0-16) | Produce a Leaching Nodule from a real batch. | Manual mining treatment before powered excavators. |
| T0-15 — Leached Rock | T0-14 | Treat exposed host rock and mine one treated block with a suitable tool. | Nearby ore remains ordinary ore; treatment neither extracts it nor grants passage through solid rock. |
| ST-04 — Grow, Then Reinforce | ALL(ST-03,T0-06) | Reinforce one mature cell while preserving its current function. | Bone ribs are independent of maturity and functional grafts; later tempered ribs replace their grade. |

ST-02 uses Item Catalog I010's Synaptic Probe recipe; no amethyst console is needed to configure the first organ. The Field Lancet and Sample Vial recipes appear with early specimen instructions. Inspection identifies a target; actual sampling consumes the declared supplies and does not complete a genome merely by looking at it.

Each producer's page lists its Item Catalog recipes, required ingredients and downstream consumers. Gels, fusion media and prepared grafts are recipe subpages of their owning organs, not extra rank badges. JEI links the same preparation chain in both directions.

## 4. Circulation, storage and a living landscape

For T1 and later, the chapter rank licenses commissioning; the listed parent nodes additionally determine the teaching sequence. A node that lists SR1 directly is a new branch root, not a demand to finish every other T1 page.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| T1-30 — Activation Cyst | ALL(SR1,T0-02,T0-05,T0-06) | Activate one ingredient into a named consumable gel or secretion. | Raw feed → biological treatment; construction and mutation have different inputs. |
| T1-01 — Biomass Vein | ALL(ALL(SR1,ST-02,ST-03),T1-30) | Graft and configure one recessed biomass route cell. | Green fuel circulation; directional pulses; source and destination remain separate things. |
| T1-02 — Fluid Vein | ALL(ALL(SR1,ST-02,ST-03),T1-30) | Graft a fluid route and select its liquid. | Water service; no automatic conversion between fluid and biomass. |
| T1-03 — Item Vein | ALL(ALL(SR1,ST-02,ST-03),T1-30) | Graft an item route and set its destination face. | Inventory transport without helper mobs. |
| T1-04 — Vascular Junction | T1-01 | Transfer biomass through an explicitly configured intake and outlet. | Join/split flow; a shared bidirectional store port; closed mouths remain visibly closed. |
| CI-03 — A Reservoir You Can Walk Under | T1-04 | Grow an unobstructed junction stalk to two blocks before mounting storage. | Optional tall support; growth does not move an occupied reservoir. |
| T1-05 — Septum Crossing | ANY(T1-01,T1-02,T1-03) | Route two distinguishable same-carrier streams through one crossing without mixing. | Crossing is not a junction; isolation is deliberate. |
| T1-06 — Vascular Stitch | ANY(T1-01,T1-02,T1-03) | Transfer across a visible terrain step using its two selected endpoints. | Natural hills, turns, rising and descending paths; no buried-block targeting. |
| T1-07 — Biomass Reservoir | ALL(T1-04,T0-04) | Upgrade a bladder into reservoir cells and form one valid connected store. | Grow a rectangular shared volume without interior walls or duplicated contents. |
| CI-04 — Expand Without Losing the Colony | T1-07 | Add a complete reservoir row/layer and confirm the previous contents remain. | An incomplete addition waits outside the valid body; it does not erase the old tank. |
| T1-08 — Fluid Cyst | ALL(SR1,T0-12) | Store and withdraw the same compatible fluid. | Separate water/process tanks; future hot-fluid lining. |
| T1-09 — Item Capsule | SR1 | Store two distinct item types and retrieve them unchanged. | Physical storage, legible contents and expansion without merging equipment histories. |
| T1-10 — Intake Mouth | ALL(SR1,T0-05) | Pull a permitted ingredient from a facing inventory into an organ input. | Hoppers, ordinary chests and compatible external storage are valid partners. |
| T1-11 — Output Mouth | ALL(SR1,T0-06) | Export a completed product without extracting reserved ingredients. | Output separation and full-inventory back-pressure. |
| T1-12 — Filter Valve | ANY(T1-01,T1-02,T1-03) | Admit a permitted stream while retaining a configured minimum reserve. | Filters, priority and reserve floors. |
| T1-13 — Overflow Valve | ALL(T1-12,T1-08) | Send a measured excess batch to an assigned spare tank. | Both destinations full means stop, not disappear or burst. |
| T1-14 — Sensor Polyp | SR1 | Measure a selected target and change an output signal at its configured threshold. | Local alarms; comparator ingredients may require trade or Nether resources. Not an early mandatory gate. |
| T1-15 — Nerve Tissue | ALL(ALL(SR1,ST-03),T1-30) | Deliver an on/off signal to one assigned organ. | Control is separate from transport and electricity. |
| T1-16 — Synaptic Console | ALL(T1-04,T1-14) | Inspect an actual blocked route, identify its reason and change a valid rule. | A local overview; the handheld tool still works without a console. |
| T1-17 — Cultivation Tissue | ALL(SR1,T0-09,ST-03) | Grow and harvest a planted crop while retaining replanting stock. | Food and colony feed compete for a real harvest. |
| T1-18 — Harvest Corolla | T1-17 | Harvest and replant one assigned crop bed into storage. | Bounded agriculture; no ground-item shower. |
| T1-19 — Compost Gland | SR1 | Recover fertilizer from an organic scrap batch. | Choose compost or digestion for the same material, not full yield from both. |
| T1-20 — Arbor Root | ALL(SR1,T0-16) | Bind a selected tree and choose cultivation or finite salvage. | Useful trees that stay part of the base; no automatic forest destruction. |
| T1-25 — Sapping Bush | ALL(SR1,T0-01) | Establish a bush and collect its selected product while retaining the plant. | Fiber, berries or feedstock; an inhabited ground layer rather than a bare carpet. |
| T1-26 — Living Wood | T1-20 | Deliberately harvest one accounted wood product from the assigned tree. | Preserve a productive trunk or choose salvage; do not award wood and full biomass twice. |
| T1-27 — Canopy Cyst | ALL(T1-20,T0-05) | Harvest one supplied canopy pod with the tree still viable. | Food, resin or planting-stock specialization; luminous canopy variant. |
| T1-28 — Sap Tap | T1-26 | Collect resin into a container without removing the trunk. | An alternative biological binder; multiple taps share the tree's production. |
| T1-21 — Digestive Tissue | ALL(ALL(SR1,ST-03,ST-02),T1-30) | Configure a small isolated patch, its export and safe bypass. Live damage is not required for the teaching advancement. | Armed defense is a separate surface function; explain actual-damage biomass and full-suit protection. |
| T1-22 — Travel Tissue | ALL(ALL(SR1,ST-03),T1-30) | Mark a route and inspect its exact suit prerequisites. | A prepared fast lane; functional use is completed separately under equipment. |
| T1-23 — Climbing Tendon | SR1 | Build and climb a short supported vertical route. | A reusable escape path needing no suit fuel; luminous variant for mines. |
| T1-24 — Hearth Lung | ALL(SR1,T0-07) | Attach an unobstructed lung and complete a host furnace batch. | Upgrade the existing furnace's body; a blocked lung removes its benefit, not the furnace. |
| T1-29 — Sphincter Door | ALL(SR1,T0-11) | Open and close a living doorway with a clear occupied-space check. | Ordinary access, nerve control and later process hatches. |
| CI-01 — One Line, No Hand Feeding | ALL(SR1,ST-02) | Run three consecutive batches in one organ with automatically replenished inputs and retained/exported output. Native mouths/veins, hoppers or compatible transports qualify. | First autonomous processing line; show the actual inputs, fuel and output path. |
| CI-02 — Feed Tomorrow's Colony | CI-01 | Supply that line from a replenishable organic source while retaining its seed/breeding/growth reserve. Complete three source-to-consumer batches. | SR2 route. Crops, tended trees, bushes, ordinary farms or husbandry can solve it; no prescribed farm shape. |
| CI-05 — The Store Is Full | CI-01 | Let a small destination become full; show clean refusal, free space, then resume one batch without material loss. | Back-pressure as normal operation. Optional readiness lesson, not a demand for destructive overflow. |

## 5. Genetics, food, husbandry and field construction

Genetics has two outputs: **knowledge** in the DNA Bank and **consumable stock** in a container. Neither substitutes for the other. An incomplete genome card shows missing coverage and the relevant specimen sources before the player spends a rare sample.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| T2-31 — Fusion Chrysalis | ALL(SR2,T1-30,T2-04) | Prepare one target-specific graft from active ingredients and Binder. | Host preparation needs no creature genome; genetic grafts consume researched stock. |
| T2-01 — Specimen Extractor | SR2 | Process one labeled specimen into a fragment and matching stock. | Basic extraction and disclosed recovery; ordinary plant and mob sources both qualify. |
| T2-02 — DNA Bank | SR2 | Deposit one real fragment and inspect its source-specific coverage. | Persistent genome knowledge, missing portions and connected research access. |
| GE-01 — Follow the Missing Sequence | ALL(T2-01,T2-02) | Select a source and show a new fragment increasing previously missing coverage. | A fragment is consumed once; repeatedly reinserting the same evidence gives nothing. |
| GE-02 — One Complete Genome | GE-01 | Reach 100% coverage of any supported ordinary source. | That source's trait recipes; qualifies a route toward SR3. No requirement to complete every species. |
| T2-03 — Archive Lobe | T2-02 | Attach a lobe and assign one real research record. | Expand memory; moving a lobe retains its assigned records. |
| T2-04 — Sequencing Lens | ALL(T2-01,T2-02) | Resolve a selected missing region through a lens-assisted batch. | Better targeting of samples, not additional mob loot. |
| T2-05 — Genetic Culture Vat | ALL(SR2,T1-08,T2-04) | Grow one batch of matching Genetic Stock using a completed genome and retained seed stock. | Maintain discovered cultures; recipes cannot grow pearls, stars or metals. Bioactive mineral-carrier recipes do not themselves need a creature genome. |
| T2-06 — Mutation Chamber | ALL(SR2,T0-08,T2-04,T0-11,T0-12) | Form the valid chamber and inspect a target's mutation preview. No completed mutation is needed to reveal its instructions. | Reagents, anatomy, incompatible grafts and preserved target identity. |
| T2-07 — Memory Gland | ALL(SR2,T2-06) | Transfer one typed amount from a donor to a sample and then a compatible recipient, with the donor reduced accordingly. | Move learning without copying it or converting it to unrelated XP. |
| T2-08 — Repair Dock | ALL(SR2,T0-08) | Refill and repair a worn living piece using real supplies. | Expedition servicing. Armor repair-credit behavior follows Armor Evolution, not a second guide XP rule. |
| T2-09 — Grafting Bench | ALL(SR2,T0-06,T2-04) | Form a valid bench and inspect a compatible plant graft. | Plant specialization, separate parent stock and habitat needs. |
| GE-04 — A Trait Put to Work | ALL(GE-02,ANY(T2-06,T2-09)) | Install one researched genome-derived trait and demonstrate its stated function on a plant, organ or equipment piece. Host-only grafts do not satisfy this particular genome lesson. | SR3 route; player chooses the research specialty. |
| T2-10 — Ration Kitchen | ALL(SR2,T0-07) | Cook one chosen expedition ration from actual food. | Food production has value beyond digestion; travel and work meals are alternatives. |
| T2-11 — Brewing Gland | SR2 | Brew one valid potion batch from its normal ingredients. | Actual brewing-stand ingredients still matter; drinking remains useful alongside future infusion. |
| T2-12 — Aquaculture Bed | ALL(ALL(SR2,T1-17),T1-30) | Raise one selected aquatic crop or stock batch in its valid water habitat. | A parallel ocean economy, not mandatory Nether preparation. |
| T2-25 — Feeding Trough | ALL(SR2,T0-09) | Feed an assigned pen while respecting its target population and breeding reserve. | Controlled husbandry using real food. |
| T2-26 — Milking Lobe | T2-25 | Collect a permitted nonlethal animal product into its required container. | Service berths, animal recovery and later shearing grafts. |
| T2-27 — Incubation Basket | T2-25 | Hatch into one reserved valid berth; retain stock if no berth is available. | Populate a pen without flooding it with entities. |
| T2-28 — Fishing Polyp | SR2 | Catch a permitted fish from a marked valid water area into storage. | A shore-based alternative to cultivated aquaculture, with bait and rod wear. |
| T2-29 — Restorative Tissue | ALL(ALL(SR2,ST-03),T1-30) | Supply and configure a recovery cell for an authorized target. Healing an already injured target may complete it, but deliberate injury is not required. | Recovery costs biomass; it does not refill a specimen's sampling reserve. |
| T2-20 — Structure Grower | ALL(SR2,T0-10,T0-11) | Assemble a small marked pattern entirely from supplied parts. | Templates, obstruction refusal and physical construction costs. |
| GE-05 — A Deliberate Plant Line | ALL(GE-04,T2-09) | Grow and harvest one grafted plant with its stated conditions met. | A useful crop/tree expression; optional ecological specialization, not a mandatory combat genome. |
| GE-06 — A Research Supply Line | ALL(T2-05,CI-01) | Automatically replenish one culture's feed and export usable stock while retaining its seed reserve. | Reduce repeated specimen collection after discovery; rare cultures still need their own feed. |

## 6. Mining: choose the excavation, not just a bigger head

Manual Leaching remains available throughout. Automated methods remove real terrain. Every plan is finite and must leave a usable entrance, light and access. Completing one method never requires completing all the others.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| T2-22 — Surveyed Tissue | ALL(ALL(SR2,ST-02,ST-03),T1-30) | Mark a finite footprint/depth entirely on exposed ground-level tissue and confirm its preview. | Selected borders, access and an explicit stopping depth. |
| T2-21 — Descending Rootstock | ALL(T2-22,T0-14,T1-23,T0-13) | Finish one supplied descending staircase section and walk from entrance to its landing. | Lit stairs with safe headroom; forward continuation and side branches are separately authorized. |
| MI-01 — Another Section, Not Forever | T2-21 | Prepare the next landing/collar and approve one additional bounded branch or section. | A completed section does not automatically acquire another mine's worth of targets. |
| T2-23 — Mineral Gizzard | ALL(SR2,T0-06) | Process actual raw ore into its accounted mineral concentrate and residue. | Dry smelting versus a longer recovery chain. Armor-carrier preparation is a separate no-bonus recipe mode. |
| T2-24 — Washing Kidney | T2-23 | Wash a batch into separate useful and waste outputs. | Water logistics, retained mineral fractions and full-waste back-pressure. |
| MI-02 — A Mine That Stops Cleanly | ANY(T2-21,T3-11,T3-13,T4-13) | Pause a real job for a full output, absent access supplies or an unapproved boundary; resolve the cause and resume without losing its cargo. | Servicing a mine, not outrunning an uncontrollable quarry. |

Later mining pages are listed in their thermal and precision chapters: Descending Cradle, Boring Jaw, Digestion Crucible and the one-block-wide Strata Maw. Rootstock stairs, a moving shaft platform, a drained treatment pit and a surface trench solve different jobs; none is the mandatory replacement for Manual Leaching.

## 7. Defense and optional helpers

The ordinary campaign can be completed with stationary organs, normal equipment and other players. Helper pages are a side branch. A demonstration can use safe target observation or an authorized training target; progression never asks for an uncontrolled mob farm.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| T2-16 — Aerocyte Bloom | SR2 | Connect fuel, configure permitted targets and demonstrate a valid unobstructed target solution. | Single green shots, limited ammunition and sight lines; not automatic immunity to a dragon or Wither. |
| T2-30 — Spine Sentry | SR2 | Supply real ammunition, configure its ground approach and validate its firing lane. | Ground defense differs from aerial interception. |
| T2-17 — Repellent Crown | SR2 | Apply an appropriate scent to deter a susceptible ordinary target. | A nonlethal way to protect a work area; not universal boss/player repulsion. |
| T2-18 — Lure Polyp | SR2 | Bring a susceptible existing creature to a marked accessible destination. | Attraction needs a path, a real target and the appropriate scent. |
| T2-19 — Restraining Tissue | ALL(SR2,GE-11,ST-03) | Form a supplied sampling restraint and safely release its permitted occupant. | Spider-informed restraint; release on timeout or supply loss. |
| DF-01 — A Fed Defensive Position | ALL(T1-04,ANY(T1-21,T2-16,T2-30)) | Supply a defense while preserving a separate workshop/emergency reserve; demonstrate its safe stand-down. | Allocation between production and defense; damage-derived feed is optional. |
| T2-13 — Brood Nursery [optional] | SR2 | Hatch one larva into a clear reserved berth, within the station's population limit. | Individual berths, idle positions and an explicit future worker mutation. |
| T2-14 — Worker Waypoint [optional] | ALL(T2-13,T1-30) | Assign a short route with separate waiting/delivery positions and complete one safe delivery. | Reliable stairs/terrain routes; work stays local and loaded. |
| T2-15 — Sail Roost [optional] | T2-13 | House and feed one sail assigned to named nurseries. | Population/support monitoring, not implicit chunk loading. |
| DF-02 — Recall the Worker [optional] | T2-14 | Recall or relocate a worker with cargo preserved and its old berth released. | Ownership, full destination behavior and recovery after a worker dies. |

No chapter badge has T2-13, T2-14, T2-15 or DF-02 as a compulsory ancestor. The parked hostile-fluid lift has no progression node.

## 8. Thermal colony: build in the Nether

Nether scouting and ordinary brewing are not locked behind SR3. This chapter teaches how to make the Nether a working production site. Its initial nursery uses carried biomass and a neighboring magma block; it does not need a Steam Heart to manufacture the first Thermal Lining.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| TH-00 — Pack for a Thermal Outpost | GE-02 | Establish a vanilla Nether route and stage membranes, feed, tools, shelter parts and a return supply. | Build the first outpost without already owning thermal bio armor. Ordinary fire-resistance preparations remain valid. |
| T3-01 — Thermal Substrate | SR3 | Grow a local thermal bed in the Nether from the listed treatment and carried feed. | Native family differs from merely putting heat lining on an Overworld cell. |
| T3-02 — Thermal Nursery | T3-01 | Produce the first Thermal Lining on the required native bed using bootstrap heat. | Nether-native membrane production; keep imported feed and local conditions distinct. |
| T3-03 — Lava Siphon | ALL(T3-02,T1-02,T1-08) | Withdraw a measured amount from an actual marked lava source into a lined tank. | Finite hot-fluid extraction, receding source and shutoff. |
| T3-07 — Relief Chimney | ALL(T3-02,T1-12) | Install an unobstructed relief destination and validate its route before pressure operation. | Safe discharge is constructed before a boiler is started. |
| T3-04 — Steam Heart | ALL(T3-02,T3-07,T1-08) | Produce a controlled steam batch with water, real heat input and a working relief path. | The first pressure body; no first-batch electricity requirement. |
| T3-05 — Steam Vein | T3-04 | Deliver a steam batch through a heat-lined route with a designated condensate outlet. | Steam, fluid and signal paths are separate services. |
| T3-06 — Pressure Vesicle | T3-04 | Charge and discharge a finite steam reserve within its permitted pressure. | Burst work and reserve capacity, not a cold-fluid tank. |
| T3-08 — Condenser | T3-04 | Recover water from spent steam and supply make-up for the unrecovered fraction. | Cooling surface versus compact active cooling; no lossless self-powering loop. |
| T3-09 — Thermal Mantle | ALL(T3-02,T0-07,T0-11) | Enclose an existing Bio-Furnace on the native footing and make Tempered Bone Plates. | Same furnace history, new body and hotter recipes; SR4 prerequisite. |
| TH-02 — A Stronger Foundation | ALL(T3-09,ST-04) | Upgrade an existing ribbed functional cell to tempered ribs without losing its function. | Structural reinforcement is not an automatic upgrade to every organ above it. |
| T3-10 — Steam Muscle | ALL(T3-04,T3-02) | Drive one supported host operation and account for spent steam. | A powered attachment that cannot mine or process by itself. |
| T3-12 — Boring Jaw | ALL(T3-09,T3-10,T2-22) | Assemble a valid head with its declared cutting profile and supplied host connection. | Real cutting surfaces, harvest capability and a matching spoil route. |
| T3-11 — Descending Cradle | ALL(T3-12,T1-23,T2-22) | Descend one planned shaft section and return through its retained access route. | A moving supported platform; not arbitrary building relocation. |
| T3-13 — Digestion Crucible | ALL(T3-02,T0-14,T2-22,T1-23) | Treat a sealed finite pit, drain its process medium and enter the recovery space safely. | Exposed ores remain to recover; residue and spent medium must have destinations. |
| T3-14 — Heat-Exchange Gill | ALL(T3-04,T1-02) | Transfer heat between separated streams without mixing their contents. | Heat reuse and cooling layout. |
| T3-15 — Distillation Crown | ALL(T3-14,T2-11) | Concentrate one valid potion batch with its dose count conserved. | Compact chemistry for later infusion; not more effects from the same bottle. |
| T3-16 — Launch Bellows | ALL(T3-10,T3-06) | Charge a clear launch pad and validate its landing plan; a safe short launch can demonstrate it. | Prepared vertical movement and later glider takeoff; not free flight. |
| TH-01 — The Nether Makes Something | ALL(T3-02,CI-01) | Run three native lining batches with replenished inputs and outputs retained in the Nether workshop. | SR4 prerequisite. Compact imported-feed nursery or a larger local supply installation both qualify. |

## 9. Electrical precision and controlled production

First electrical production can be metabolic or steam-assisted. A complete steam power network is not compulsory. An external energy source can power compatible consumers, but cannot provide a missing native material or replace proof of the supported process.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| T4-01 — Electrocyte Stack | SR4 | Generate and retain usable electricity from real biomass/water, with optional steam assistance. | Bootstrap power without electricity as an input to its first operation. |
| T4-02 — Conductive Tissue | ALL(ALL(SR4,ST-03),T1-30) | Connect a bounded electrical route and deliver power from a supported source. | Electricity is neither Nerve Tissue signaling nor biomass. |
| T4-03 — Charge Sac | SR4 | Charge a real reserve and discharge it into a supported load. | Finite energy storage and protected shutdown reserve. |
| T4-04 — Electrical Exchange Organ | ALL(SR4,T4-01) | Exchange energy with an available compatible external system in the selected direction. | Optional integration branch; absent external mods never block a badge. |
| T4-05 — Ion Separator | ALL(SR4,T2-24,T2-04) | Run one mineral or genetic separation recipe with power, water and retained waste. | Separate recipe cartridges; recover only the remaining mineral/sample value. |
| T4-06 — Live Sampling Cradle | ALL(T4-05,T2-01,T2-19) | Take a permitted sample from a housed target, then respect its recovery reserve. | Nonlethal/controlled sampling is not unlimited collection from an instantly healed creature. |
| T4-07 — Precision Sequencer | ALL(T4-05,T2-04) | Process a supported valuable sample with its expected recovery preview. | Fewer wasted rare specimens; no mandatory repeated boss kills for ordinary mutation stock. |
| T4-08 — Potion Infuser | ALL(SR4,T3-15,T2-06) | Install a supported chemical interface and load one real potion dose into eligible equipment. | Limited charged effects; returned empty bottles; ordinary drinking remains useful. |
| T4-09 — Trait Regulator | ALL(T4-07,T2-07) | Process a compatible specialist plan and retain its stated anatomy/metabolic exclusions. | Regulation does not erase the protection-versus-flexibility tradeoff. |
| T4-10 — Scheduler Ganglion | ALL(SR4,T1-16,T4-03) | Complete a short sequence: reserve inputs, run a real organ, wait, then export. | Explicit dependencies and a visible blocked step. |
| T4-11 — Display Membrane | ALL(SR4,T1-14) | Display one actual machine value with a readable threshold/alarm state. | Optional wall monitoring; labels and shape cues as well as color. |
| T4-12 — Request Cortex | T4-10 | Request one item whose recipe uses at least two real operations and deliver the result. | Physical machines execute the order; shortages remain visible. |
| T4-13 — Strata Maw | ALL(SR4,T2-21,T4-05,T1-23) | Complete a finite **one-block-wide** surveyed strip from the surface down, starting at its climbing route. | Deep-strip excavation; real spoil handling and access remain. It does not require first completing the Cradle or Crucible. |
| T4-14 — Spoil Sorter | ALL(SR4,T1-09,T1-11,T1-12) | Route a real mixed batch into ore, useful rock and retained surplus. | A mine's output architecture matters as much as its cutting speed. |
| T4-15 — Recovery Sump | ALL(SR4,T1-08,T1-04,T3-02) | Recover a small measured test volume inside an isolated catchment. | Local fluid recovery; no need to rupture a live storage tank. |
| T4-16 — Neutralization Gland | ALL(SR4,T0-03,T2-24,T3-02) | Neutralize a supplied waste batch with accounted residue and dirty water. | Destructive disposal is explicit, costs reagents and cannot close a profitable digestion loop. |
| T4-17 — Pressure Regulator | ALL(SR4,T1-14,T1-13,T4-03) | Control a selected vessel and complete a safe stop while preserving shutdown reserve. | Automate limits; an undersized or obstructed relief route is still invalid. |
| T4-18 — Folded Shelter | ALL(SR4,T2-20) | Pack and deploy a small shelter from actual parts into a clear footprint. | First End/Fold expeditions use imported building stock, not materials from their destination. |
| T4-19 — Cold Lobe | ALL(SR4,T3-14,T4-03) | Maintain one valid cold compartment through a processing interval. | Preserve a valuable specimen/food batch; cooling does not reverse existing spoilage. |
| T4-20 — Mnemonic Vessel | SR4 | Deposit and withdraw the same ordinary Minecraft XP amount. | XP storage is separate from genomes, organ counters and armor counters. |
| EL-01 — Precision With a Purpose | ANY(T4-05,T4-06,T4-07) | Deliver one useful precision output into storage or its next legitimate process. | SR5 prerequisite; mineral recovery, sample recovery and live sampling are alternatives. |
| EL-02 — Stop Without Losing the Batch | EL-01 | In a small supported job, close its output or pause its supply, observe safe refusal, then restore service and finish with materials retained. | SR5 prerequisite. Native controls, simple redstone or a scheduler can solve it; no mandatory accident. |

Hazardous free-flowing biomass remains a separate design decision. Recovery and neutralization pages can teach supplied waste handling without making random tank bursting a campaign requirement.

## 10. End settlement and spatial production

The book shows the expedition plan before departure. It does not invent a free pre-dragon return portal or assume a shelter is dragon-proof. A world where another team already defeated the dragon remains fully playable; the dragon egg is never required.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| EN-00 — Plan the First End Expedition | SR4 | Stage an equipment, building, fuel and ordinary return/recovery plan in an expedition inventory. | Vanilla preparation or a packed supported outpost. No End-grown gear required. |
| EN-01 — Beyond the Portal | EN-00 | Reach the End alive. | Survey a build site and locate local materials; rank does not restrict ordinary portal use. |
| EN-02 — The Dragon Is Not the Ending [optional] | EN-01 | Participate in a dragon defeat, or inspect an already opened exit in a world where it is defeated. | Post-encounter settlement and recovery; no exclusive egg ownership or repeated kill demand. |
| T5-01 — Anchored Substrate | SR5 | Mature a local anchored bed in the End using imported supplies and local ingredients. | A native spatial foundation, not an Overworld floor with a special appearance. |
| T5-03 — Anchor Root | SR5 | Construct and supply the physical corner restraints of a proposed spatial body. | First anchors use thermal products and End materials, not Spatial Membrane. |
| T5-02 — Spatial Nursery | ALL(T5-01,T5-03,T2-05) | Grow the first Spatial Membrane with real feed and electricity on its End bed. | A continuing three-material chain: ordinary membrane, Nether lining, End growth. |
| T5-04 — Void Tether | ALL(T5-02,T3-16) | Register a clear local rescue berth, charge it and validate its bounded work area. No fall into the void is required. | A later local rescue service; never assumed available for the first arrival. |
| T5-05 — Chorus Resonator | ALL(SR5,T5-01) | Establish one selected local conditioning pulse. | Purpose-specific rooms; incompatible tuning must be separated or scheduled. |
| T5-06 — Spatial Conditioner | ALL(T5-02,T5-05,T2-06) | Finish one purpose-conditioned membrane batch in the End. | Passenger, cargo and precision branches. Reconditioning consumes work, not another original membrane. |
| EN-04 — Passenger Conditioning | T5-06 | Produce a passenger-conditioned batch. | Transit mouths and arrival anatomy. |
| EN-05 — Cargo Conditioning | T5-06 | Produce a cargo-conditioned batch. | Freight gullets and staging locks. |
| EN-06 — Precision Conditioning | T5-06 | Produce a precision-conditioned batch. | Spatial armor, district control and compound-organ treatment. |
| T5-07 — Levitation Chamber | ALL(SR5,T4-09,T5-02,GE-18) | Run one supported delicate treatment in a properly supplied suspension bay. | Shulker-informed sample handling, not a free-flight chamber. |
| T5-08 — Chorus Orchard Tissue | ALL(T5-01,T1-18) | Harvest an End-grown chorus crop with planting stock retained. | Local nursery feed and an accessible cultivated canopy. |
| T5-09 — Targeting Eye | ALL(T5-02,T1-14,ANY(T2-16,T2-30)) | Assign a valid observed target to an authorized defense without bypassing its range or sight line. | Coordinated defense rather than another independent weapon. |
| T5-10 — Selective Membrane | ALL(T5-02,T1-29) | Admit an authorized entrant and demonstrate the configured safe emergency opening. | Controlled rooms without trapping their occupants. |
| T5-11 — Catching Membrane | T5-02 | Catch a deliberately dropped low-value item over a safe test floor and retain it in connected storage. | Physical collection net; not universal player rescue. |
| T5-12 — Phase Isolator | ALL(T5-02,T5-05) | Run two nearby differently tuned rooms with their boundaries separating the processes. | Compact spatial districts; a complete enclosure has a job. |
| EN-03 — The End Makes Something | ALL(T5-02,CI-01) | Complete three native nursery batches from replenished inputs into local storage. | SR6 prerequisite. A fed, productive site, not merely possession of chorus fruit. |

## 11. Three worlds, one production system

Local automation is required; perpetual remote ticking is not. An unloaded workshop pauses. Each arrival station needs a clear actual destination, and each cargo transfer needs real receiver capacity.

**Solo route:** the player can carry accounted batches through ordinary available portals between already automated local sites. This can satisfy TR-01/TR-02. Freight Gullets and Transit Maws offer another route when their endpoints can legitimately operate. No badge forces both distant sites to remain loaded, requires a Nutrient Sail, or silently authorizes chunk tickets. The passenger-gate destination-loading policy still needs a separate decision before implementation.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| T6-02 — Arrival Chamber | ALL(SR6,EN-04,T5-04) | Reserve and validate a supplied clear arrival berth. | Arrival before departure; production cannot consume its protected reserve. |
| T6-01 — Transit Maw | T6-02 | Complete one authorized trip between ready valid stations. | Original living passenger transit; unavailable destination refuses departure. |
| T6-04 — Cargo Lock | ALL(SR6,EN-05,T1-09) | Pack and cancel one real staged batch without losing or duplicating cargo. | Requested, reserved, packed and received are different states. |
| T6-03 — Freight Gullet | ALL(T6-04,T1-04,T1-08) | Deliver one acknowledged cargo batch between ready endpoints. | Freight capacity and passenger capacity are independent. Offline receivers leave cargo safely local. |
| T6-05 — Storage Cortex | ALL(SR6,EN-06,T4-12,T1-09) | Find and withdraw a real item from connected physical storage through its routed interface. | Searchable local storage; remote/offline stock is not instantly withdrawable. |
| T6-06 — Workshop Interface [optional integration] | ALL(SR6,T4-12,T1-10,T1-11) | Complete a compatible external process request using delivered ingredients and a real organ. | Adapter branch; no external mod is required for the native progression. |
| T6-07 — Relay Ganglion | ALL(SR6,EN-06,T4-10) | Query or command an authorized connected district and show an honest unavailable state when it is offline. | Local autonomy plus selective remote oversight. |
| T6-08 — Foundation Cyst | ALL(SR6,EN-05,T2-20,T0-04) | Deploy a packed finite marked patch and retain unused supplies. | Controlled outpost expansion; no limitless spreading or instantly mature native beds. |
| T6-09 — Service Pedestal | ALL(SR6,T2-08,T1-09,T1-12) | Complete a selected suit/tool/cargo service using its actual attached store and organ. | Service hubs beside mines and gates; no all-purpose free service machine. |
| T6-10 — Reclamation Mouth | ALL(SR6,T2-20,T1-11,T6-04) | Recover a selected unoccupied small assembly into reserved containers. | Safe relocation with core history, fluid and contents accounted for. |
| TR-01 — Three Useful Addresses | ALL(SR6,TH-01,EN-03,CI-01) | Deliver one real Nether-native batch and one real End-native batch to an owned working Overworld workshop. Record the producing sites; hand carriage through available vanilla portals or acknowledged compatible freight qualifies. | Three-dimensional supply network without mandatory remote chunk loading. |
| TR-02 — A Product No Single World Could Make | TR-01 | Finish one supported product whose actual chain used Overworld construction membrane, Nether Thermal Lining and End-grown/conditioned Spatial Membrane. Local production steps run automatically; transport may use the chosen route. | SR7. A Transit Maw, spatial armor medium or another declared pre-SR7 recipe can demonstrate the chain. |
| TR-03 — Two Routes, One Colony [optional] | ALL(T6-03,T6-07) | Redirect one bounded shipment after an endpoint becomes unavailable, retaining the original reservation correctly. | Redundant districts and rerouting; no off-screen production claim. |

## 12. Synthesis and the three-dimension capstone

This chapter rewards design choices: which organs are combined, which services are shared, and which processes should remain separate. A large assembly is not automatically a better version of every smaller one.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| T7-02 — Anatomy Socket | ALL(SR7,EN-06,T1-04,T0-11) | Install and isolate one supported attachment in a prepared compound body. | Physical attachment space with separate supply, output and control. |
| T7-01 — Synthesis Heart | ALL(SR7,T4-09,T2-06,EN-06) | Form its body and grow one supported compound core from actual donor organs. | Donor histories are assigned, not copied; the resulting recipe defines its capabilities. |
| T7-03 — Catalyst Lobe | ALL(SR7,T2-05,T4-07,T5-02) | Maintain one matching catalyst culture through a host recipe. | Advanced culture feed and real catalyst consumption. |
| T7-04 — Expression Switch | ALL(SR7,T4-09,T4-10,EN-06) | Complete one legal drained/idle changeover between two installed organ profiles. | Alternate roles, not both at full strength simultaneously. |
| T7-05 — Organ Transplanter | ALL(SR7,T6-10,T2-07,T5-07) | Move one seasoned core into a prepared compatible body with its history unchanged. | A starter organ can remain valuable inside a late installation. |
| T7-06 — Genome Vault | ALL(SR7,T2-02,T2-03,EN-06) | Store a knowledge backup and a separate real Memory Sample; inspect their different recovery rules. | Knowledge can be copied under ownership rules; physical experience and stock cannot. |
| T7-07 — Interceptor Nursery [optional] | ALL(SR7,T2-13,T7-03,ANY(T2-16,T2-30)) | Deploy and recall one bounded defender group with accounted growth stock. | Temporary combat organisms; no requirement for ordinary freight or boss access. |
| T7-08 — Siege Blossom | ALL(SR7,T2-30,T5-09,T3-10,T4-03) | Commission a supported, cooled firing position and deliver one valid supplied shot. | Strong stationary defense that needs a clear target and a working base. |
| SY-01 — One Body, Two Jobs | ALL(T7-01,T7-02) | Complete two supported operations through a compound assembly using its actual installed working parts. | A practical compound design, not an arbitrary organ collection. |
| SY-02 — Choose the Better Design | ALL(SY-01,T4-11) | Record two finite production runs for the same useful output and compare throughput, feed and occupied footprint. Keep the chosen design. | Optional optimization page; no universal leaderboard formula or forced “best” layout. |
| SY-03 — A Colony With a Future | ALL(SR7,SY-01) | Finish one useful commissioned order while retaining supply for the next and a protected service/return reserve. | Three-dimension capstone advancement. Research, alternative builds and new outposts remain open; the Fold is not required. |
| T7-09 — Fold Gateway [long-term] | ALL(SR7,T5-06,T6-02,T7-03,GE-17,GE-19,GE-15) | Prepare the End-native gateway, its two catalyst services, imported landing stock and return reserve; create a destination seed from silverfish, blaze and enderman stock using their complete genomes. | First deliberate Fold expedition. No Fold plant, Adaptive Gel or boss material is needed to open the first route. |

## 13. The Fold: a fourth settlement, not a loot room

The first shelter and return mechanism use imported parts. Natural feeding and hardening phases can be waited for; later regulation is an alternative that costs infrastructure and power. Nothing here makes harvested ore regenerate.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| FO-01 — A Foothold, Not a Trap | T7-09 | Arrive at the prepared Fold landing, establish the packed shelter and validate its still-supplied return mechanism. | SR8. Basic survival does not depend on obtaining the first native output under an invisible deadline. |
| T8-10 — Foldroot | SR8 | Gather a local cutting and reserve one for planting. | Starting nursery feed and a regenerating ground plant. |
| T8-11 — Pulse Reed | SR8 | Harvest a reed segment while retaining its base or planting stock. | Fiber and a visible local phase indicator. |
| T8-12 — Glassbloom | SR8 | Obtain soft-phase specimen material or hardened petals, with planting stock retained. | Different useful harvests from different natural phases. |
| T8-01 — Adaptive Substrate | ALL(SR8,T8-10) | Culture local root-bearing ground and mature it under native conditions. | Imported ordinary tissue cannot replace the native growing environment. |
| T8-02 — Founder Nursery | ALL(T8-01,T8-10,T3-02) | Produce the first Adaptive Gel using imported water/feed and native cuttings. | Native supply before any boss or closed environmental room exists. |
| T8-03 — Phase Shelter Skin | ALL(T8-02,T0-10) | Enclose a small room and show its supported buffering behavior through a natural phase change. | Biological room boundaries and maintenance, not a universal hazard shield. |
| T8-04 — Habitat Lung | ALL(T8-02,T8-03,T3-14,T1-14) | Maintain a selected room condition while accounting for water, feed, power and waste. | Local habitat control; separate rooms can serve different organisms. |
| T8-05 — Adaptive Culture Loom | ALL(T8-04,T5-07,ANY(GE-20,GE-21,GE-22)) | Grow an adaptive membrane for two specified operating conditions. | Local plant knowledge plus thermal/spatial materials; no requirement to capture an advanced local creature first. |
| T8-06 — Phase Accumulator | ALL(T8-02,T5-05,T4-03) | Capture a real natural transition and spend the retained charge on a supported load. | Storage for intermittent native work, not a self-powering forced-phase loop. |
| T8-07 — Foreign Specimen Cocoon | ALL(T8-05,T4-06,T8-04) | House and sample one supported native creature while retaining its habitat and containment. | Optional advanced creature research. Plants provide the bootstrap genomes before this cocoon exists. |
| T8-08 — Retuning Root | ALL(T8-06,T7-04) | Pay for a local feeding/hardening interval in an assigned enclosure. | Scheduled native production instead of waiting; not changing the whole dimension. |
| T8-09 — Quarantine Gate | ALL(T8-02,T6-04,T5-10,T4-05) | Inspect one marked cargo batch, admit a compatible load and safely retain a refused one. | Import discipline without deleting unknown specimens or contaminated fluid. |
| FO-02 — The Foothold Feeds Itself | ALL(T8-02,CI-01) | Run three nursery batches with reserved/replanted local cuttings and replenished water/biomass. Imported biomass is permitted; local Gel production is mandatory. | SR9 prerequisite; an ongoing settlement, not a single plucked plant. |
| FO-03 — Work With the New Biology | T8-05 | Use a native adaptive membrane in a supported functional part or equipment interface. | SR9 prerequisite; a material must become useful, not merely sit in a trophy chest. |
| FO-04 — Separate the Seasons [optional] | ALL(T8-08,T8-05) | Run feeding and hardening recipes in separately supplied rooms or a deliberately scheduled shared room. | Two viable architectures for native production. |

## 14. The Manyfold and what follows

The boss is summoned deliberately at a prepared site, away from the return station. Its chapter is about opening a vulnerability and exploiting it, not a prerequisite to maintain the settlement that makes the attempt possible.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| T9-01 — Calling Corolla | ALL(SR9,GE-20,GE-21,GE-22) | Build the corolla with its required native stock and inspect its ready-to-call state without activating it. | Encounter site, supplies and deliberate summon control. |
| T9-02 — Resonance Sink | ALL(SR9,T8-06,T3-14,T5-12) | Handle a supplied noncombat calibration pulse with cooling and correct tuning. | Open a future vulnerability window; no need to summon the boss before learning the mechanism. |
| T9-03 — Severing Root [optional] | ALL(SR9,T7-08,T8-08) | Prepare an aligned, charged severing organ and validate its target lane. | Infrastructure-heavy damage route. Personal weapons and allies remain a complete alternative. |
| BS-01 — Ready to Call | ALL(T9-01,T9-02) | Validate return/service reserves, cooling, a clear encounter boundary and either a prepared personal combat loadout or supplied offensive organs. | Enables the deliberate summon action; no prescribed armor branch or compulsory Siege Blossom. |
| BS-02 — Make It Vulnerable | BS-01 | During a deliberately started encounter, route the correct pulse through a supplied sink and open a vulnerability window. | The encounter's central puzzle; raw damage alone does not replace it. |
| BS-03 — Sever the Manyfold | BS-02 | Defeat the encounter through personal attacks, a supplied Severing Root or their combination. | Guaranteed recoverable research tissue; no armor invulnerability reward. |
| GE-23 — Understand the Manyfold | ALL(BS-03,T4-07,T2-02) | Resolve the boss genome with precision processing, retaining the stock needed to seed its culture. | The first successful encounter provides enough recoverable material for this research route; no routine series of identical kills. |
| T9-04 — Pattern Incubator | ALL(GE-23,T7-03,T8-04) | Grow a first maintained batch of Manyfold Genetic Stock. | Repeatable post-boss graft materials from real feed and native habitat, not repeatable unique boss loot. |
| T9-05 — Reciprocal Graft | ALL(T9-04,T7-04,T7-02,T7-01) | Install a shared adaptation budget and operate each legal allocation separately. | More capacity for one role means less for the other; no simultaneous maximums. |
| BS-04 — A New Relationship | T9-05 | Complete a useful production/defense cycle using the chosen reciprocal arrangement, with both donors' histories retained. | Final journal capstone; alternate anatomy, colonies and efficient designs remain the long game. |

Summoning again never awards another colony rank. Losing a battle does not remove SR9, destroy learned recipes or make the existing Founder Nursery require boss stock. A new attempt still pays its material preparation cost.

## 15. The genetic atlas: species lead to actual choices

All ordinary source cards are searchable after GE-01, with habitat and collection instructions. They do not need to be hunted in this table's order. Each completion below means **100% of that named genome in the accessible DNA Bank**, from valid source-labeled evidence, not simply holding its familiar drop.

A compatible sampler or extraction recipe must provide a source-labeled specimen route even for a creature with no suitable ordinary drop. Live Sampling Cradle is the later improved route, not the only way to discover silverfish, bats or other no-drop sources. Unknown/mixed specimens display that limitation rather than silently becoming whichever species the player needs.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| GE-03 — Know What You Collected | T2-01 | Inspect a supported specimen's source label and the extractor's predicted outputs. | Source identity, ambiguous drops, material stock versus genome coverage. |
| GE-07 — Cow | GE-01 | Complete cow DNA from identified livestock specimens. | Field Lens; compatible husbandry expressions. |
| GE-08 — Rabbit | GE-01 | Complete rabbit DNA. | Running Tendons, Spring Heel and travel-tissue acceleration. |
| GE-09 — Slime | GE-01 | Complete slime DNA. | Impact Bladder, Landing Bladders, route handling and restraint variants. |
| GE-10 — Squid | GE-01 | Complete squid DNA. | Aquatic Eyes and Propulsive Fins. |
| GE-11 — Spider | GE-01 | Complete spider DNA; cave-spider coverage is separate. | Climbing Hooks, Restraining Tissue and specialized digestive-floor control. |
| GE-12 — Cave Spider | GE-01 | Complete cave-spider DNA. | Toxin Filter; controlled chemical-defense branch. |
| GE-13 — Bat | GE-01 | Complete bat DNA through its supported sampling route. | Nocturnal Membrane; ordinary darkness traversal remains possible without it. |
| GE-14 — Magma Cube | GE-01 | Complete magma-cube DNA. | Thermal suit lining and Thermal Exchange once native materials and frame requirements are available. |
| GE-15 — Enderman | GE-01 | Complete enderman DNA. | Burrowing anatomy, later Blink and tracking; finding endermen in the Overworld is a legitimate early research route. |
| GE-16 — Turtle | GE-01 | Complete turtle DNA through permitted specimens. | Gill Bellows, Pelagic Lining, Bracing Tendons and aquatic incubation. |
| GE-17 — Silverfish | GE-01 | Complete silverfish DNA through identified specimens. | One Fold destination-seed component; it is not the armor Burrowing gene in the newer armor design. |
| GE-18 — Shulker | GE-01 | Complete shulker DNA after reaching a valid source. | Levitation Chamber; the first End nursery does not need this genome. |
| GE-19 — Blaze | GE-01 | Complete blaze DNA. | Thermal Sight, thermal ammunition expressions and Fold destination-seed stock. |
| GE-20 — Foldroot | ALL(GE-01,T8-10) | Complete Foldroot DNA from ordinary extracted native plant specimens. | Adaptive growth, cutting/feed variants and one Calling Corolla culture. |
| GE-21 — Pulse Reed | ALL(GE-01,T8-11) | Complete Pulse Reed DNA. | Fiber/phase-sensitivity variants and one Calling Corolla culture. |
| GE-22 — Glassbloom | ALL(GE-01,T8-12) | Complete Glassbloom DNA. | Structural/sample variants and one Calling Corolla culture. |
| GE-24 — Zombie | GE-01 | Complete zombie DNA. | Scent Pits and Working Tendons. |
| GE-25 — Axolotl | GE-01 | Complete axolotl DNA through permitted specimens. | Regrowth Lobe, with fuel and combat-recovery limits. |
| GE-26 — Creeper | GE-01 | Complete creeper DNA. | Blast Baffles; choosing them excludes the chest's Impact Bladder cavity. |
| GE-27 — Phantom | GE-01 | Complete phantom DNA. | Later Elytral Wings; genome possession supplies neither a real Elytra nor End-conditioned materials. |
| GE-28 — Bee | GE-01 | Complete bee DNA through permitted specimens. | Colony Reader and Service Tendril. Helpers are not needed to use the organ-status reader. |
| GE-29 — Fox | GE-01 | Complete fox DNA. | Stalking Fibers; not invisibility. |
| GE-30 — Dolphin | GE-01 | Complete dolphin DNA through permitted specimens. | Swimming Muscles. |
| GE-31 — Wither Skeleton | GE-01 | Complete wither-skeleton DNA. | Wither Sieve; the skull is not assumed to be the only usable specimen. |
| GE-32 — Witch | GE-01 | Complete witch DNA. | Potion Capillary after the actual infuser is available. |
| GE-33 — Wheat | GE-01 | Complete wheat DNA from plant specimens. | Ordinary cultivated-crop grafts; a noncombat first-genome route. |
| GE-34 — Oak | GE-01 | Complete oak DNA from identified tree specimens. | Oak-specific root/canopy expressions; no need to destroy every tree to sample it. |
| GE-35 — Cod | GE-01 | Complete cod DNA from identified aquatic specimens. | Fish husbandry and targeted food-fishing expressions. |
| GE-36 — Sheep | GE-01 | Complete sheep DNA. | Supported shearing/husbandry branch using actual animals and tools. |
| GE-37 — Cat | GE-01 | Complete cat DNA through permitted specimens. | Targeted Repellent Crown expressions, including the proposed phantom-deterrence route. |
| GE-38 — Kelp | GE-01 | Complete kelp DNA. | Aquaculture plant-production variants. |
| GE-39 — Birch | GE-01 | Complete birch DNA. | Birch-specific arbor choices; it does not substitute for oak coverage. |
| GE-40 — Sweet Berry Bush | GE-01 | Complete the original bush genome from identified specimens. | Compatible berry/fiber husbandry choices; a planted bush is still useful before DNA completion. |

GE-23, the Manyfold genome, is defined in the boss chapter. For every additional supported animal, plant, tree or modded organism, the atlas adds the same finite card family:

```text
Supported source discovered / inspected
  → identified specimens → missing regions resolved → complete source genome
       ├─ matching consumable-stock culture
       ├─ its listed compatible organ/plant expressions
       └─ its listed compatible equipment mutations
```

The card is generated from that source's declared recipes, not an assumption that every creature grants every behavior associated with its name. A completed genome with no implemented expression must say so. Missing optional mods remove their own source cards; they do not remove a main-path prerequisite. Boss and Fold sources stay inside their own revealed chapter instead of spoiling the final encounter at first contact.

## 16. Equipment path: four independent living pieces

The book has helmet, chest, leggings and boots tabs. Each tracks the selected item's actual state. A journal advancement for making one iron piece does not silently upgrade the other three pieces or unlock their counters. Armor frame G1–G4, item level L0–L25 and counter rank R0–R5 retain their meanings from Armor Evolution.

### Material and service branches

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| AR-01 — A Dormant Skin | ST-01 | Craft or obtain one dormant armor piece from its familiar rotten-flesh silhouette recipe. | A weak starting body; awakening is not diamond protection. |
| AR-02 — Your First Living Piece | ALL(AR-01,T0-08) | Awaken a piece and inspect its five zeroed counters, frame, anatomy and empty fuel reserve. | Independent history for each equipment slot. |
| AR-03 — It Learns by Being Used | AR-02 | Earn one counter rank on a worn piece and allocate the resulting growth point. | Relevant activity, frame ceilings and choosing a property instead of receiving every bonus. |
| AR-04 — A Living Binder | ALL(T0-02,T0-03,T0-05) | Make Fusion Binder through the starter bowl recipe. | Host-grown introductory grafts before a DNA laboratory. |
| AR-05 — Metal That Can Join Tissue | ALL(SR2,T2-06,T2-23,T2-24) | Use the culture-vat material mode to make Ferrocyte Paste or Auric Myelin from prepared real mineral. | Biological activation is mandatory; no raw ingot applied directly to armor. This recipe does not require a completed creature genome. |
| AR-06 — Iron Ribs | ALL(AR-03,AR-05) | Fuse an individual G1 piece at L2+ with its required Ferrocyte doses. | G2 rigid protection, six slots and R2 counter ceiling. |
| AR-07 — Auric Lattice | ALL(AR-03,AR-05) | Fuse an individual G1 piece at L2+ with Auric Myelin. | G2 flexible alternative, eight slots and R3 ceiling; not the mandatory step after iron. |
| AR-08 — Diamond Carapace | ALL(AR-06,T3-09,T3-02) | Fuse an Iron Ribs L6+ piece with Faceted Chitin. | G3 permanent plated-diamond child; no auric-to-carapace recipe. |
| AR-09 — Obsidian Scutes | ALL(AR-06,T3-09,T3-02) | Fuse an Iron Ribs L6+ piece with Vitreous Scute. | G3 heavy permanent child; distinct terminal bonded-scute path. |
| AR-10 — Diamond Tendon | ALL(AR-07,T3-09,T4-05) | Fuse an Auric Lattice L6+ piece with Diamond-Fiber Matrix. | G3 flexible child, eleven slots and R4 ceiling. |
| AR-11 — Netherite Lamellae | ALL(AR-08,T4-05,T3-09) | Fuse Diamond Carapace at L10+ using Living Netherite Lamella. | Permanent G4 plating, ten slots and R4 ceiling. |
| AR-12 — Netherite Mesh | ALL(AR-10,T4-05,T3-09) | Fuse Diamond Tendon at L10+ using Netherite Tendon Mesh. | G4 flexible terminal choice, thirteen slots and R5 ceiling. |
| AR-13 — Spatial Weave | ALL(AR-10,EN-06) | Fuse Diamond Tendon at L10+ using Phase-Woven Matrix. | G4 spatial terminal choice, fourteen slots and R5 ceiling. |
| AR-14 — Four Pieces, One Metabolism | AR-02 | Equip four functional awakened pieces, fill real reserves and inspect combined load/output. | Shared fuel and coordinated abilities; materials may differ across pieces. |
| AR-15 — Commit to a Lineage | ALL(ANY(AR-06,AR-07),T2-06) | Inspect a committed piece's valid forward children and permanently excluded sibling. | Plan another independently grown piece for the other lineage; no branch exchange. |
| AR-16 — Learning Has a Cost to Move | ALL(AR-02,T2-07) | Move typed armor learning into a compatible recipient while reducing the donor. | No copied experience or conversion of boot ascent into helmet observation. |
| AR-17 — A Supporting Organ [optional Curios] | AR-14 | Equip and use one supported control/reserve/chemical accessory through the suit's normal resource rules. | Accessories supplement a suit; they do not supply a missing armor piece or unlimited anatomy. |
| AR-18 — Adaptive Interface [long-term] | ALL(ANY(AR-11,AR-12,AR-13,AR-27),T8-02,EN-06,T6-09) | Install Adaptive Interface and select two control presets for the same compatible installed anatomy. | All installed grafts plus interface must fit together; presets change settings, never branches. |
| AR-19 — Reciprocal Controls | ALL(AR-18,T9-04) | Install Reciprocal Control Graft and perform its permitted settings-only field change. | Same anatomy and exclusions; interruption and cooldown still apply. |
| AR-27 — Netherite-Bonded Scutes | ALL(AR-09,T4-05,T3-09) | Fuse an Obsidian Scutes L10+ piece with Netherite-Bonded Scute medium. | Terminal heavy branch; eight slots, R4 ceiling, retained mobility penalty. |
| AR-28 — Learn to Live Together | AR-02 | Perform ordinary fed work while wearing an awakened piece; inspect adaptation and hunger warnings. | Tiny early reserves; no deliberate injury required for adaptation. |
| AR-29 — Full Symbiosis | AR-28 | Reach the approved wearer/piece adaptation thresholds through eligible use. | Empty fuel no longer causes armor hunger pain; unpaid features still stop. Ownership model and thresholds remain draft choices. |
| AR-30 — A Real Pair of Wings | SR5 | Obtain an actual Elytra. | A physical ingredient for Wings I, not an ability fabricated solely from phantom DNA. |

The AR material nodes are **first-achievement cards**, not permission to cross an individual item's lineage. AR-28/29 teach automatic early hunger pain and later full symbiosis; they never demand deliberate starvation. Repeat their operation independently for any helmet, chest, leggings or boots. A completed AR-11 cannot satisfy L10 or a mutation's counter threshold on another item.

### Counter branches shown inside each piece tab

Each row below displays its complete **R0 → R1 → R2 → R3 → R4 → R5** path and exact numeric thresholds from Armor Evolution. A rank opens only when this specific piece has both the relevant activity and a frame permitting that rank. Frame fusion raises the ceiling; it grants no practice retrospectively.

| Piece | Counter cards | What earns their progress |
|---|---|---|
| Helmet | H-F Field Study; H-D Dark Travel; H-W Submerged Travel; H-T Thermal Travel; H-C Chemical Defense | Valid examinations; actual dark travel; submerged distance; thermal-environment travel; externally caused harmful-effect seconds removed. |
| Chest | C-G Guarding; C-M Metabolic Work; C-R Tissue Repair; C-T Thermal Travel; C-W Submerged Work | Eligible hostile hits; productive fuel use; eligible repaired wear; thermal travel; active underwater work. |
| Leggings | L-T Overland Travel; L-E Exertion; L-W Aquatic Motion; L-B Burrowing; L-S Working Stance | Ground distance; sprint distance; swimming; actual solid-terrain passage; qualifying tool work. |
| Boots | B-F Terrain Footing; B-A Ascent; B-L Landing Control; B-W Aquatic Propulsion; B-P Spatial Displacement | Uneven terrain; elevation gained; qualifying landings; swimming; successful armor Blink or paid pearl travel. |

For each piece: its five active counter ranks sum to **L0 → L1 → … → L25**, with one allocatable growth point per level. The piece tab unfolds each intermediate threshold on demand; the main colony map does not acquire one hundred repetitive global level toasts. These are item conditions, not a second collection of colony-rank locks.

### All armor mutation families

The rows below are the branch roots. Each family produces three connected book cards **`.I → .II → .III`**; all current families have all three ranks. For common M-family grafts, the cards appear independently in all four equipment tabs.

The exact expansion is:

```text
Family's listed parents completed
  + a valid selected piece
  → family.I instructions
       → install/use rank I on that piece → family.I complete
           → family.II instructions, with its grade/counter requirements visible
               → upgrade/use rank II on that piece → family.II complete
                   → family.III instructions
                       → upgrade/use rank III on that piece → family.III complete
```

The **B / A / S** schedules, common level gates, slot costs, genomes, material quantities and active-effect rules are those in Armor Evolution. They are shown on the card itself, not hidden behind an unexplained “adaptation required.” Rank-I versions accept R0 where specified, so learning Burrowing does not require an already trained Burrowing counter.

For the dependency tables, a bare family ID such as `AM-H8` means its rank-I milestone; `.II` or `.III` explicitly requires that higher-rank milestone. The family table's **Open after** applies to its first card. Advancements acknowledge a valid first installation/use; later operation always checks the worn item's own state.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| AM-M1 — Repair Membrane | ALL(AR-02,AR-04) | Install/use M1 I, then follow common level gates for II/III on each selected piece. | Self-mending spends biomass and restores real wear. |
| AM-M2 — Thermal Lining | ALL(AR-02,GE-14,T3-02,ANY(AR-08,AR-09,AR-10)) | Install M2 I at G3/L6+, II at G4/L10+, III at G4/L14+, individually. | All four linings are required for Thermal Mode. |
| AM-M3 — Pelagic Lining | ALL(AR-02,GE-16,ANY(AR-06,AR-07)) | Install M3 I at G2/L2+, II at G3/L6+, III at G4/L10+, individually. | Aquatic coordination excludes Thermal Lining on the same piece. |
| AM-M4 — Potion Capillary | ALL(AR-02,GE-32,T4-08) | Install M4 I/II/III at G2/L2+, G3/L6+, G4/L10+, then load a real dose. | Limited chemical charges, actual potion ingredients and shared dispensing cooldown. |
| AM-H1 — Field Lens | ALL(AR-02,GE-07,T2-06) | Helmet; B schedule, H-F. | Directed examination and source information. |
| AM-H2 — Lantern Gland | ALL(AR-02,AR-04) | Helmet; B schedule, H-D. | Fueled biological light; an early host-grown graft. |
| AM-H3 — Nocturnal Membrane | ALL(AR-02,GE-13,T2-06) | Helmet; B schedule, H-D. | Low-light vision, distinct from illuminating the world. |
| AM-H4 — Aquatic Eyes | ALL(AR-02,GE-10,T2-06) | Helmet; B schedule, H-W. | Underwater clarity, not wall-through ore vision. |
| AM-H5 — Toxin Filter | ALL(AR-02,GE-12,T2-06) | Helmet; B schedule, H-C. | Limited removal of Poison; higher rank improves the response. |
| AM-H6 — Thermal Sight | ALL(AR-02,GE-19,T3-02,T2-06) | Helmet; A schedule, H-T. | Read exposed hot targets and surfaces through distracting fire/smoke overlays. |
| AM-H7 — Scent Pits | ALL(AR-02,GE-24,T2-06) | Helmet; B schedule, H-F. | Sparse nearby creature direction cues, not unrestricted enemy vision. |
| AM-H8 — Stone Sense | ALL(AR-02,GE-15,T4-05,T2-06) | Helmet; A schedule, H-D. | Local cavity/navigation information for Burrowing; no ore identity reveal. |
| AM-H9 — Wither Sieve | ALL(AR-02,GE-31,T3-02,T2-06) | Helmet; A schedule, H-C. | Limited Wither removal, not boss immunity. |
| AM-H10 — Colony Reader | ALL(AR-02,GE-28,AR-05,T2-06) | Helmet; B schedule, H-F. | Selected organ status and already-authorized helper orders. |
| AM-C1 — Reservoir | ALL(AR-02,AR-04) | Chest; B schedule, C-M. | More stored fuel, not more instantaneous metabolic output. |
| AM-C2 — Pump Heart | ALL(AR-02,AR-05) | Chest; B schedule, C-M. | Higher whole-suit output with its actual operating overhead. |
| AM-C3 — Regrowth Lobe | ALL(AR-02,GE-25,T2-06) | Chest; B schedule, C-R. | Delayed player healing, distinct from armor repair. |
| AM-C4 — Impact Bladder | ALL(AR-02,GE-09,T2-06) | Chest; B schedule, C-G. | Conditional physical-hit reduction; excludes Blast Baffles. |
| AM-C5 — Thermal Exchange | ALL(AR-02,GE-14,T3-02,T2-06) | Chest; A schedule, C-T. | Better thermal fuel endurance, not limitless lava time at lower lining ranks. |
| AM-C6 — Gill Bellows | ALL(AR-02,GE-16,T2-06) | Chest; B schedule, C-W. | Fueled air supply; ordinary water-breathing potions remain valid. |
| AM-C7 — Blast Baffles | ALL(AR-02,GE-26,T3-02,T2-06) | Chest; A schedule, C-G. | Explosion specialist; excludes Impact Bladder. |
| AM-C8 — Burrow Mantle | ALL(AR-02,GE-15,T4-05,T2-06) | Chest; A schedule, C-M. | Owns coordinated passage fuel/load and its stopping window. |
| AM-C9 — Feeding Lobe | ALL(AR-02,AR-04) | Chest; B schedule, C-M. Configure allowed food slots and feed from a real item. | Feeding Lobe supplies player hunger, not armor fuel; it creates no food. |
| AM-C10 — Service Tendril | ALL(AR-02,GE-28,T2-06) | Chest; B schedule, C-M. | Feed one selected nearby compatible helper/tool; transfer is not free fuel or practice. |
| AM-C12 — Nutrient Intake | ALL(AR-02,AR-04,T1-30) | Chest; B schedule, C-M; load a permitted sealed biomass container. | Automatic actual fuel transfer, distinct from food and healing; no transfer practice. |
| AM-C11 — Elytral Wings | ALL(AR-02,GE-27,EN-06,ANY(AR-12,AR-13),AR-30,T2-31) | Prepare Wings I with an actual Elytra and install on flexible G4 chest; advance I/II/III. | Glide → powered flight/climb → takeoff/hover; permanent exclusion of Burrow Mantle. |
| AM-L1 — Running Tendons | ALL(AR-02,GE-08,T2-06) | Leggings; B schedule, L-E. | General grounded sprint improvement; excludes Fast-Lane Tendons. |
| AM-L2 — Endurance Mesh | ALL(AR-02,AR-04) | Leggings; B schedule, L-T. | Reduce sprint hunger at a biomass cost. |
| AM-L3 — Swimming Muscles | ALL(AR-02,GE-30,T2-06) | Leggings; B schedule, L-W. | Powered swimming within the suit's limits. |
| AM-L4 — Bracing Tendons | ALL(AR-02,GE-16,T2-06) | Leggings; B schedule, L-S. | Stable deliberate tool work; movement releases the brace. |
| AM-L5 — Working Tendons | ALL(AR-02,GE-24,T2-06) | Leggings; B schedule, L-S. | Faster manual work without changing harvest tier or loot. |
| AM-L6 — Stalking Fibers | ALL(AR-02,GE-29,T2-06) | Leggings; B schedule, L-T. | Better crouched movement, not invisibility. |
| AM-L7 — Burrowing Muscles | ALL(AR-02,GE-15,T4-05,T2-06) | Leggings; A schedule, L-B. | Supported travel through eligible ground in all directions. |
| AM-L8 — Fast-Lane Tendons | ALL(AR-02,AR-05,T1-22) | Leggings; B schedule, L-T. | A colony commuter's alternative to Running Tendons. Actual use needs Surface Key II and a full suit. |
| AM-B1 — Contour Sole | ALL(AR-02,AR-04) | Boots; B schedule, B-F. | Step over terrain edges with real headroom. |
| AM-B2 — Climbing Hooks | ALL(AR-02,GE-11,T2-06) | Boots; B schedule, B-A. | Contact-based wall climbing; excludes Propulsive Fins. |
| AM-B3 — Landing Bladders | ALL(AR-02,GE-09,T2-06) | Boots; B schedule, B-L. | A finite fall allowance, not immunity to arbitrary falls. |
| AM-B4 — Propulsive Fins | ALL(AR-02,GE-10,T2-06) | Boots; B schedule, B-W. | Aquatic propulsion; excludes Climbing Hooks. |
| AM-B5 — Spring Heel | ALL(AR-02,GE-08,T2-06) | Boots; B schedule, B-A. | A deliberate powered ground jump, not a second midair jump. |
| AM-B6 — Blink Tendon | ALL(AR-02,GE-15,EN-06,T2-06) | Boots; S schedule, B-P. | Short travel to a clear visible supported destination, with real cooldown and fuel. |
| AM-B7 — Surface Key | ALL(AR-02,AR-04) | Boots; B schedule, B-F. | Consented tissue recognition; rank II enables compatible fast-lane operation. |
| AM-B8 — Ground Anchor | ALL(AR-02,GE-15,T4-05,T2-06) | Boots; A schedule, B-F. | Ground contact coordination for the complete Burrowing anatomy. |

All non-host armor graft cards also require the actual Fusion Chrysalis recipe and prepared I051 consumable. The recipes, not just the family parents, decide material availability: an AR-05 badge earned with iron does not create Auric Myelin for a gold-dependent graft. A G1 piece may have an advanced family's page open while its G3 requirement remains visibly unmet.

### Whole-suit demonstrations

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| AR-20 — Ready for Heat | ALL(AR-14,AM-M2) | Equip four valid thermal linings and show an adequately fueled Thermal Mode readiness result. | Lowest lining rank governs the set; the guide displays actual fire/lava limits before exposure. |
| AR-21 — An Ocean Is a Worksite | ALL(AR-14,AM-M3,AM-C6,AM-H4) | Perform a short planned underwater job with breathing, visibility and the selected aquatic mode operating. | Build an ocean specialist without requiring the Nether specialist first. |
| AR-22 — Through the Ground | ALL(AR-14,AM-H8,AM-C8,AM-L7,AM-B8) | Burrow through a short prepared eligible rock volume into a known adjacent clear exit. | Ground-only movement, fuel and the 10-second rank-I stopping window; no deliberate entombment required. |
| AR-23 — Use the Colony's Roads | ALL(AR-14,T1-22,AM-L8,AM-B7.II) | Traverse a compatible fast lane with the actual required full suit and mutations. | The floor alone does not grant speed to every player. |
| AR-24 — A Flight With a Landing | ALL(AR-14,AM-C11) | Glide to a prepared landing; II/III follow-ups demonstrate powered climbing and hovering with actual fuel. | Landing reserves, intact wing fallback and no flight chunk loading. |
| AR-25 — A Safe Short Blink | ALL(AR-14,AM-B6) | Perform a valid charged Blink without bypassing terrain or destination checks. | Failed attempts consume no travel charge and do not train displacement. |
| AR-26 — Two Jobs, Two Builds | AR-14 | Compare two separately grown owned pieces with different permanent branches. | Owning multiple specialists; preview their anatomy, fuel and protection tradeoffs. |

Completing a suit demonstration acknowledges its first working rank. Its II and III follow-up checks require the actually equipped coordinated adaptations at that rank; a better chest alone does not upgrade Burrowing. No whole-suit experience pool is created.

## 17. Handheld tools and weapons

These are proposed gameplay branches from the design notebook. Item Catalog I015–I018/I038 now supplies candidate item forms and preparation recipes; per-tool counters, harvest/damage values and final branch policy still need the tool/weapon specification. They remain optional leaves here; no colony rank relies on an undefined weapon or tool. The book can guide their purpose and dependencies without claiming that their detailed item design is settled.

| Node | Open after | Complete by | Teaches / unlocks |
|---|---|---|---|
| EQ-01 — A Living Working Tool | T0-08 | Awaken a supported dormant tool and complete one eligible manual operation. | The same lifecycle as armor, with a tool's own history. |
| EQ-02 — A Careful Cutting Tool | ALL(EQ-01,T2-06,T2-09) | Install a supported cutting/plant-preservation graft and perform one deliberate harvest with planting material retained. | A garden/wood specialist rather than a universal mining tool. |
| EQ-03 — A Mineral Working Tool | ALL(EQ-01,AR-05,T2-23) | Fuse a supported bioactive mineral form and harvest a block within the resulting tool tier. | Real harvest tier, useful manual work beside automated mines. |
| EQ-04 — Preserve the Sample | ALL(EQ-01,T4-07) | Collect a supported valuable field specimen using the precision expression, with its actual recovery accounted for. | Careful sampling trades against a bulk-clearing tool. |
| EQ-05 — Clear a Small Worksite | ALL(EQ-03,T4-05) | Execute one previewed bounded area cut with space for every recovered output. | Small room/service excavation; no recursive whole-forest or whole-vein deletion. |
| EQ-06 — A Living Weapon | T0-08 | Awaken a supported weapon and complete an eligible attack. | Portable offense has its own counters and material limits. |
| EQ-07 — Piercing Limb | ALL(EQ-06,T2-06) | Install a supported piercing expression and use a deliberate single-target attack. | Timing and penetration, not the strongest attack against every target. |
| EQ-08 — Grasping Weapon | ALL(EQ-06,T2-06,GE-11) | Apply a permitted control effect to a susceptible target and release it safely. | Space control trades damage; no permanent boss/player immobilization. |
| EQ-09 — Ranged Gland | ALL(EQ-06,T2-06,T1-09) | Fire supplied ammunition at a valid visible target with its operating cost paid. | A portable ranged branch, weaker in sustained output than a supplied emplacement. |
| EQ-10 — One Weapon Is Not Every Weapon | ANY(EQ-07,EQ-08,EQ-09) | Inspect a valid alternate expression and its incompatible allocation. | Compare separate weapon forms, not a mandate to craft all three. Tool/weapon branch policy remains a separate decision. |

## 18. What the player sees when progress stops

Each unlocked page includes a small working example, the required cells/parts and a checklist tied to the selected machine or item. Failed conditions report their actual cause:

| Situation | Message and next link |
|---|---|
| A blueprint is outside the current colony chapter | “Commissioning requires SR4. Complete **The Nether Makes Something** and **Thermal Mantle**.” Link to the unmet parent; still show supplies needed for planning. |
| A young cell cannot become a vein | “Mature this selected cell first.” Link to ST-03; show the next growth treatment. |
| A route is blocked | “East outlet closed” or “Destination full,” with the selected endpoint. Link to T1-04/T1-12 or CI-05, not a generic “network invalid.” |
| A raw ingot is placed in an armor treatment | “Needs Ferrocyte Paste, not an iron ingot.” Link to AR-05 and its preparation recipe. |
| Raw ingredients are offered as a tissue upgrade | “Prepare Capillary Gel in Activation Cyst.” Link to T1-30 and the exact Item Catalog recipe, without consuming the rejected ingredient. |
| Hungry armor injures its wearer | “Fuel empty; symbiosis incomplete.” Show the affected pieces, refueling route and adaptation status. After full symbiosis the same warning says hungry abilities are paused, without armor pain. |
| A mutation's DNA is incomplete | “Turtle genome: missing coverage shown in this bank.” Link to GE-16 and its supported specimen routes. Do not ask for unrelated samples. |
| The piece's learning is capped | “Current frame permits R2. More activity will not count until a permitted forward fusion.” Link to its valid material alternatives. |
| Four pieces are equipped but Burrowing will not start | List the absent/wrong-rank H8/C8/L7/B8 graft, collapsed item, insufficient fuel or load conflict. Link to AR-22 and the specific piece tab. |
| Native production is moved to the wrong dimension | “Thermal Nursery requires a mature Nether-native bed in the Nether.” Link to T3-01/T3-02; importing finished lining remains valid. |
| A remote destination is unavailable | “Destination not ready; cargo retained here.” Link to T6-03 and the current endpoint state. Never report fabricated remote production. |
| An optional dependency is absent | Hide only its adapter-specific action and suggest the native route. Never leave an unfinishable mandatory card. |

### Recovery and completion rules

Losing the book, dying, evolving equipment, unloading an outpost or breaking a multiblock does not remove earned journal completion. Operational permissions still follow the real remaining structure and supplies. A completed page can reopen its tutorial and diagnostics without resetting the world or crafting a duplicate machine.

For commissioning evidence, retain only the named milestone and its small capped demonstration state: the three required batch receipts, the selected sites or the current bounded trial. Do not keep a lifetime log of every transfer, mined block, participant position or visited chunk. The guide observes server-confirmed events; it must not scan whole worlds to decide which boxes to tick.

The tree supplies native guidance and advancements. Pack authors can wrap it in their own quests, recipes and extra challenges, but FTB Quests is not part of this mod's required progression.
