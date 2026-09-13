# End-to-end gameplay progression

This document owns the sequence of player projects. [Blocks](BLOCK_CATALOG.md)
owns operations, [Items](ITEM_CATALOG.md) recipes, [Substrate](LIVING_SUBSTRATE_MUTATIONS.md)
ground anatomy, [Armor](ARMOR_EVOLUTION.md) equipment and [Guide](GUIDE_PROGRESSION_TREE.md)
page/advancement dependencies. Do not copy their numeric rules here.

**Status:** proposed campaign under [Vision](../VISION.md).
SR0–SR9 are chapter coordinates; T0–T9 are catalog groupings. Whether a player rank,
colony capability or upgraded structure gates operations is [Q-025](OPEN_QUESTIONS.md),
not an established permission system. [Open questions](OPEN_QUESTIONS.md) is the
only unresolved-decision register. References below identify blocked specifications.

## Vocabulary

| Term | Meaning |
|---|---|
| Biomass / BU | Processed biological fuel; draft unit 1 BU = 1 mB. |
| Specimen | Source-attributed physical material consumed during analysis. |
| Genome coverage | Retained species knowledge, not consumed by making a graft. |
| Genetic Stock | Physical species-matched material consumed by culture/graft recipes. |
| Bioactive medium | Prepared ingredient used in fusion or functional mutation. |
| Fusion level | Level of one equipment piece's material frame, not a player level. |
| Activity counter | Points from one eligible activity, owned by one armor piece. |
| Learning capacity | Limit on the sum of that piece's activity counters. |
| Mutation branch | Permanent anatomy with forward children, not a settings preset. |
| Organ level | Development from completed organ work, separate from armor learning. |
| Native bed | Substrate requiring its home dimension for named production. |
| Production line | Connected reusable operations with actual inputs and held outputs. |

## 1. SR0 — Start beside a vanilla home

**Build:** make Spore Culture from ordinary ingredients. Establish selected Living
Substrate, Culture Bowl, Digestive Sac and Biomass Bladder. Prepare membrane and
bone components with Membrane Rack and Bone Loom.

**First loop:** hand-supply permitted organic matter and water to the Sac; store
biomass; spend it on selected ground growth, preparation or equipment. Keep food
and planting stock instead of digesting the whole inventory.

Craft one dormant piece, awaken it in the Cradle, fill it at the Bladder and wear
it during ordinary activity. Its counters share capacity; activity alone does not
fuse a new frame. A first host-grown graft can provide light or armor healing
without a completed creature genome.

**Alternative project:** use Leaching Gland to weaken selected non-ore rock.
See ore through the translucent host, then mine with tools. Leaching does not
pay the ore output or grant Burrowing.

**Recovery:** retain ordinary gear, planting stock and manual feed access.
Remove/feed hungry armor to address its pre-symbiosis pain; this does not rescue
a player from an unrelated environmental hazard.

**Result:** one fed useful piece and a workshop preparing its own starter inputs.
Onboarding: Q-032/Q-034; equipment parameters: Q-001–Q-010.

## 2. SR1 — Automate replenishment

```text
Seed Pouch → Planting Proboscis → Cultivation Tissue
                                      ↓
                                Harvest Corolla
                                      ↓
                             Work Bed → Collection Cilia
                                      ↓
                                  filtered split
                         ┌────────────┼─────────────┐
                   seed reserve   player food   surplus feed
                                                    ↓
                                              Digestive Sac
                                                    ↓
                                                Reservoir
                                          ┌─────────┴──────────┐
                                      workshop            Fuel Papilla
```

A Compost Gland uses a separate share of scraps for fertilizer. The same input
cannot produce its full compost and digestion yields. Collection exports held
output; drones and loose item drops are not prerequisites.

**Source choices:** crop beds, managed tree growth and later aquatic habitats.
Arbor Root allocates a selected tree's growth among wood, pods and sap; harvesting
and replanting stay separate. Wild vegetation conversion is Q-017, not automatic
canopy clearance.

**Circulation:** configure Vascular Junction ports and recessed green veins.
Vascular Stitch crosses exposed terrain steps; a junction joins routes; Septum
Crossing keeps passages isolated. Mount a reservoir above a junction, optionally
raising its stalk to retain a walkway.

**Storage:** mutate Bladders into Reservoir Cells and extend by complete rows/layers.
Invalid additions wait outside the valid body without erasing its contents.
Fuel Papilla fills worn pieces from stored supply; extension anatomy is Q-019.

**Control:** protect seed/restart reserves, then distribute surplus. Full destinations
pause suppliers. Reflex Knot and Selector Ganglion use ready/held/blocked states;
one-tick timing is unnecessary for correctness.

**Result:** the farm replenishes biomass after its own operating costs. Add demand
and choose more growing area, better recovery, another line or scheduled consumers.
Net-positive source recipes and growing conditions: Q-013/Q-016.

## 3. SR2 — Research a selected specialization

Choose a purpose before collecting: aquatic travel, climbing, a plant trait,
thermal preparation or an organ upgrade. The guide identifies relevant species
and sample/preparation routes.

```text
specimen → Specimen Extractor → retained genome coverage in DNA Bank
                   └────────→ consumable Genetic Stock
complete genome + actual feed → culture additional matching stock
stock + prepared ingredients → Fusion Chrysalis → target-specific graft
graft + existing target → Mutation Chamber → same target, forward mutation
```

Sample amount, genome coverage and extraction precision are separate; Q-023 sets
their exact relationship. Cultured stock never manufactures mineral or boss loot.

**Armor project:** prepare mineral fusion media and select a permanent branch.
Fusion raises shared capacity without awarding practice. Activities cannot be
paused. Paid partial counter reduction frees selected capacity without branch
exchange; its recipe and below-threshold behavior are Q-003.

**Other projects:** Grafting Bench for plant traits; Feeding Trough plus
Incubation Basket for bounded husbandry; Aquaculture Bed/Fishing Polyp for aquatic
production; Ration Kitchen for meals before surplus is allocated to digestion.

**Mining:** mark a Descending Rootstock job with flush Surveyed Tissue. Supply
access steps, light and output storage. Walk the result to explore a cavern or
address a refused face. Job geometry, fluids and extension policy are Q-021.

**Mineral workshop:** Grasping Root moves one intact specimen to Work Bed;
Reaction Polyp treats it; Fracture Jaw creates held fragments; Collection Cilia
exports to Mineral Gizzard, Washing Kidney and Bio-Furnace. Hand mining can bypass
treatment. Multiple beds may share an applicator or fracture head.

**Result:** a chosen specialization and repeatable preparation, not every genome
or every armor mutation. Optional helpers do not gate stationary logistics.

## 4. SR3 — Establish the Nether thermal workshop

Bring earlier culture equipment, biomass, membrane inputs, ordinary construction/
protection and a return route. Bootstrap cannot require the nursery's own output.

Grow Thermal Substrate in the Nether. Thermal Nursery uses earlier supplies and
local heat to produce Thermal Lining. Its initial magma-block heating route comes
before hot-fluid jackets and condenser-fed water cooling.

A native Thermal Mantle produces Tempered Bone Plates. Thermal Lining also enables
hot-fluid handling, Steam Heart, Pressure Vesicle, Steam Muscle and Heat Exchange
Gill assemblies. Lava Siphon removes actual lava; exhausted sources are not replaced.

**Choices:** export lining/plates to an Overworld workshop, move bulk processing
near heat, or operate separate local lines. Water-based processes use sealed
supplied water with defined recovery, not an assumed exposed Nether water farm.

**Excavation choices:** Descending Cradle supports a shaft workstation; Boring Jaw
creates a traversable route; Digestion Crucible processes a contained volume of
host rock. These are different jobs, not merely different drill sizes.

**Biomass route under review:** thermal pretreatment of fibrous feed → cooling →
specialized digestion. Native catalyst/lining consumption and actual yield/cost
are Q-012/Q-013; no industrial digestion recipe is approved by this description.

**Result:** repeated Nether manufacturing for ongoing colony work. Keep local
storage and manual restart supplies rather than making each visit an ingredient raid.

## 5. SR4 — Choose how much recovery to pay for

Electrocyte Stack converts defined feed to electricity; Charge Sac buffers it.
Conductive Tissue carries it. Precision Sequencer and Ion Separator perform their
listed preparation tasks, not every earlier operation.

| Independent improvement | Owner's proposed steps | Choice |
|---|---|---|
| Dust recovery | 4, 5, 6, 7, 8 dust per defined ore input | Additional processing versus lower-cost recovery |
| Smelting recovery | 4, 3, 2, 1 dust per ingot | More processing energy versus current smelting |

Save compatible dust for a later efficient smelter. Maximum combination yields
eight ingots per defined input unit; Q-011 must define that unit per ore source
and prevent Fortune/recycling multiplication. Exact energy recipes are Q-012.

**Layouts:** direct smelting, partial treatment plus stockpiling, parallel specialist
lines or shared treatment equipment. Compare energy per ingot, ingots per time,
construction material and occupied space. Maximum recovery must have costs on
other measures rather than dominate every route.

Sensors and Scheduler Ganglion control finite jobs. Display Membrane reports
full stores, protected reserves and blocked work. Byproducts have held outputs
and an available same-stage disposal route. Hazardous residual production is
Q-015; ordinary full outputs still pause.

**Result:** a chosen refinery, a supplied power source and a demonstrated
input-interruption recovery path.

## 6. SR5 — Build an End settlement

Bring return supplies, biomass, thermal products, electricity support and
construction stock. Ordinary blocks provide first shelter.

Grow Anchored Substrate and Anchor Roots around Spatial Nursery. Earlier
membranes, Nether Thermal Lining, chorus fruit, biomass and electricity produce
Spatial Membranes. Spatial Conditioner prepares their passenger, cargo or precision
use. Native production stays in the End.

Chorus Orchard Tissue provides local growing work. Local biomass versus imported
fuel remains a layout choice; no mandatory identical farm is assumed.

Spatial preparations also support selected permanent armor branches. Flight needs
an actual Elytra; underground travel needs fuel, stability, visibility and air.
A spatial ingredient does not automatically provide all of these.

**Result:** repeated End production supplying equipment, freight and synthesis.
The dragon encounter does not substitute for this workshop.

## 7. SR6 — Connect three productive sites

| Site | Local catalog work | Export | Imports/support |
|---|---|---|---|
| Overworld supply district | Farming, digestion, biological preparation | Feed, biomass, membranes and biological stock | Thermal products; spatial components for advanced work |
| Nether thermal district | Thermal Nursery and native Thermal Mantle | Thermal Lining and Tempered Bone Plates | Membranes, biological supplies, local or imported fuel |
| End spatial district | Spatial Nursery and Spatial Conditioner | Purpose-conditioned Spatial Membranes | Thermal Lining, membranes, biomass and electricity support |

No Overworld-exclusive culture recipe is defined; Q-025 tracks whether one is
required. Ordinary cultivation is not claimed to be dimension-locked.

Transit Maw/Arrival Chamber move passengers. Freight Gullet/Cargo Locks move
reserved real item/fluid batches. They may share one settlement gateway layout;
shared-body anatomy and solo destination loading are Q-026.

**Choose:** centralized biomass exports, local fuel autonomy, or local essential
reserves plus industrial imports. Storage Cortex indexes local physical stores;
remote availability and freight remain separate, not an instantly spendable global tank.

**Unloading:** ordinary freight waits with accounted cargo until both endpoints
are available. Remote farms do not run invisibly. No implicit chunk loader is
authorized; the passenger-loading decision cannot be hidden in an arrival check.

**Result:** make a useful product from all three sites' contributions. Preserve
restart reserves so staged visits and shipments work without permanently loaded worlds.

## 8. SR7 — Scale production and strategic reserves

Supply parallel processing lines from farms and native products. Protect seeds,
meals, research stock and emergency fuel from bulk consumers. Develop individual
organs through actual completed work; there is no automatic district-wide bonus.

Synthesis Heart, Anatomy Sockets and Catalyst Lobes combine installed operations.
Expression Switch selects supported organ process profiles, never armor branches.

A reservoir district contains several finite bodies, isolation valves, production
buffers, export buffers and a protected reserve. Capacity and throughput are
separate. Display reserve endurance at current net drain. Thermal/spatial capacity
reinforcement and every giga-capacity value remain Q-014.

**Completion projects:** a traversable deep mine feeding a selected ore route;
an expedition/refueling network; a supplied specialist equipment workshop; or
production meeting a specified energy/space/output target. Q-034 sets release
targets. Completing every optional catalog entry is not assumed.

## 9. SR8–SR9 — Fold and Manyfold horizon

The proposed Fold has native feeding and hardening phases. Bring ordinary shelter
and a return provision. Establish a local bed and Founder Nursery for Adaptive Gel
without boss loot.

**Layouts:** buffer products grown during favorable natural phases, or supply
Habitat Lung/Retuning Root support for a bounded working habitat. Foldroot,
Pulse Reed and Glassbloom have different outputs. Phase Collector gathers native
charge; Phase Accumulator stores it. Retuning cannot generate its own consumed charge.

Adaptive Culture Loom combines thermal, spatial and native products. Equipment
presets change settings within installed anatomy, not a hidden second loadout.

Manyfold is player-initiated at a prepared site: observe its state → supply a
countercondition → attack during the opening. Solo preparation must work.
Failure cannot destroy the three-world colony. A first reward supplies a template
maintained by culturing rather than routine boss slaughter. Exact phases,
counterconditions, costs and rewards are Q-031, not a finished encounter.

## Other connected projects

- **Burrowing:** move inside ground without mining, supply light/air separately and respect stopping stability; no open-air thrust. Terrain and budgets: Q-009/Q-021.
- **Temporary scaffolding:** a handheld organ spends armor biomass on weak translucent support that expires. Attachment, fall arrest and midair placement: Q-022.
- **Defense:** supplied ground/air organs and deliberately armed digestive tissue protect approaches; target policy, damage recovery and boss behavior: Q-029.
- **Helpers:** bounded station jobs, separate idle berths, recall and cargo recovery; Sail loading is independent: Q-028.
- **Building:** copy a layout, not cargo or earned counters. Reserve real parts for a selected region; preserve finished construction and unused supplies on interruption: Q-027.
- **Tools, weapons, meals and potions:** retain manual field work and prepared expedition options alongside automation; exact trees/compatibility: Q-008/Q-016/Q-030.

## Success and interruption

Operations reserve inputs, target and outputs before committing. Full output,
missing input or an unavailable chunk pauses without voiding material.
Ready/held states let other operations take over without redstone timing races.
Dangerous processes need an approved failure contract before overriding this baseline.

A diagnostic names the blocked organ, missing condition and current reservation.
The configuration tool exposes it without empty-hand actions obstructing placement.
Safety information is available before the player encounters the hazard.

Settling Q-025 determines whether guide milestones also authorize operations.
Until then, no hidden global tier or chapter license is implementation-ready.
