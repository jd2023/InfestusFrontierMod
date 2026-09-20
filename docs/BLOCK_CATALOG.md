# Block and organ catalog

Proposed recipes and stage assignments. **Create** lists construction ingredients;
**Assembly** lists component types. Consumable preparation: [Items](ITEM_CATALOG.md).

**T0–T9 are catalog groupings**, not player or base ranks. Operation requires the entry's materials, physical structure, native bed and supplies; there is no additional colony-level permission gate. **Organ level** is earned by using that particular organ; upgrading other organs does not change it. Unless an entry says otherwise, an active organ gains one count per completed batch, not per tick or failed attempt. L0 starts at zero counts; L1, L2 and L3 start at 32, 128 and 512 counts respectively. At those thresholds, choose one improvement from that organ's **Growth** options. Each choice gives +10% to the selected property, at most three choices total. New functions require the listed mutation or physical addition; levels alone do not add every function.

Normal dismantling of an organ preserves its counts, level, chosen improvements and mutations in the dropped block. In a multiblock, the core carries this history; breaking a wall does not copy it. Upgrading an existing core preserves its history. Passive walls, floors and conduits do not earn processing levels. Normal dismantling and invalid additions retain contents in recovered storage or remaining cells. Destructive failures follow the separate containment rules below.

Free-standing organs root into Living Substrate or a named specialized bed. Attached parts root through their host. The same organ can be hand-fed first, mutated later and expanded with physical parts without crafting a replacement core. Port configuration uses a tool or deliberate UI action; empty-hand clicks do not obstruct building.

## Building with operations

Each working organ performs one reusable operation. Installations combine organs
that can be shared, bypassed or independently specialized.

**Assembly** lists the required component types of a worked installation, not a shapeless crafting recipe. **Create** states quantities, placement and whether there is a distinct controller. Installed components remain physical and recoverable, with their own counters and mutations; an assembly never copies their histories into its controller. Only the organ doing the conversion earns processing counts. A planning core earns at most one count per completed finite section; transferring, waiting or circulating the same output earns none. Passive service/holding parts have no processing levels. Automatic input, output, planting, healing and fueling are separate jobs, not free side effects of gaining a level.

### Reliable sequencing, with room to optimize

- A basic organ can be hand-loaded and explicitly started. A supplied organ can be set to repeat its selected recipe whenever ready. Neither mode requires a scheduler or a redstone clock.
- Work Bed and organ faces expose **ready, working, result held, blocked**. A result remains visibly held until collected; completion is not a one-tick pulse the next machine can miss. Early vanilla redstone reads these stable states; Nerve Tissue carries the same states and bounded start/acknowledgment commands.
- Before a step starts, it reserves its target, ingredients, product space, waste space and returned containers. Only one actor can own a particular bed/batch. Holding a start signal high or repeating a command does not repeat the same job. Distinct readiness and completion connections prevent a jaw from breaking a block before treatment.
- A Reflex Knot combines local conditions; a Selector Ganglion assigns work to one ready branch. A later Scheduler Ganglion can express a bounded sequence. None lets the player skip a physical reaction, capacity limit or native bed. Vanilla comparators, latches and hoppers remain alternatives, not obsolete prerequisites.
- A blocked next step leaves the current product held. No generic catch-up work happens on chunk reload. The UI names the dependency: “waiting for treatment,” “tailings full,” “no planting stock,” or “destination unloaded.” A safe cancel releases untouched reservations; a committed conversion retains its actual intermediate, not both the input and output. Naturally hazardous thermal operations retain their own shutdown requirements.

The puzzle is where work waits, which equipment is shared, what gets priority, and which recovery stages are worth supplying. A timer may stagger starts or impose a feed budget; it must not be the only way to know that a chemical reaction finished.

### Defense targets

Colony weapons, Digestive Tissue and defensive restraints target hostile creatures,
never players or tamed pets. Husbandry restraint requires an explicitly assigned
animal. Environmental hazards, including spilled biomass, can still hurt players.

### Containment failures

An ordinary full tank refuses input; it does not burst at random. A damaged charged
vessel or an overheated pressure process with obstructed relief can rupture locally.
Pressure/heat readouts and alarms warn before failure; Overflow Valve, Relief
Chimney and Pressure Regulator prevent it through separate storage, relief and cutoff.

Escaped biomass is finite, not a renewable source fluid. It contaminates exposed
unsealed substrate, kills that tissue and can completely destroy contacted organs.
Dead tissue loses its function and must be replaced; feeding alone cannot revive it.
Destroyed organs lose their history and contents rather than dropping an intact core.
Sealed containment floors, isolation valves, Recovery Bed and Neutralization Gland
limit losses. A catchment must have room for the vessel's contents.

Initial spill budget: at most 64 occupied cells per connected spill, 250 mB per
cell; excess remains as finite fluid in the ruptured vessel. Each 250 mB dose can
damage one contacted cell after 10 seconds, then becomes spent contamination.
Recovery before that delay saves the cell. A casualty cannot spawn another spill
job or chain explosion: its fluids join the same bounded spill and its remaining
contents are lost without loose item drops. Expansion visits at most one loaded
neighbor per spill per tick, under a server-wide 16-probe/tick, 64-job ceiling.
At admission limits, fluid stays in its breached vessel; processing resumes when
budget becomes available. Bucket placement refuses unchanged if no spill slot is
available. Unloading pauses work without catching up elapsed damage.
These are first-playtest limits, not measured safe server capacity.

### The first mineral workshop

```text
real ore block → Grasping Root → Work Bed
                                  │
                         Reaction Polyp (optional)
                                  │ treatment complete
                             Fracture Jaw
                                  │ retained fragments/raw ore
                  Collection Cilia OR courier OR manual pickup
                                  │
                           capsule / item vein
                                  │
                    Mineral Gizzard → Bio-Furnace
                                  └→ Washing Kidney → furnace
                                           └→ retained tailings → Ion Separator (T4)
```

The root presents **one existing block and stops**. It does not identify an ore deposit, treat it, roll its drops or smelt it. A player may place a harvested ore block directly on the bed instead. The polyp applies a prepared penetrant; the jaw fractures the resulting material; the collector exports it. Bare rock can instead go directly from a jaw to a building-material store. Mineral-bearing intermediates never turn back into a fresh ore block after treatment.

| Build | Physical arrangement and control | What the player gains | What they pay or give up |
|---|---|---|---|
| Lean outpost | Manual leaching/pickaxe or direct jaw extraction → small capsule → ordinary furnace | Little feed, equipment and supervision; ordinary ore yield | No intact-block treatment bonus; more player work or a low-rate cutter |
| Careful serial refinery | One bed with root, polyp and jaw on separate service faces; ready/complete interlocks | Better recipe-defined recovery from a scarce deposit; low equipment count | Bed is occupied during treatment; reagent production and waste handling |
| Parallel nursery of beds | Several independent beds/polyp-jaw sets; selector assigns the next free lane; shared reagent store | Overlap reactions and keep extraction busy | More land and organs; proportionally more feed per minute, not inherently more feed per ore |
| Shared service line | Several beds feed one gizzard/kidney; capsules buffer bursts; selector alternates ready sources | Avoid duplicating an expensive mature processor; different ores use separate input reserves | Changeover and queueing; enough return/output space for each source; a bottleneck can idle all beds |
| Fast bulk mine | Skip intact treatment; reinforced jaws → large packets → parallel furnaces | High throughput with a simpler chemical supply chain | Less recovered mineral per excavated block and a larger terrain/spoil footprint |
| Recovery-focused refinery | Treatment, washing and later tailings separation; reuse suitable water/heat through actual recovery organs | Highest supported recovery for the chosen ore recipe | Longer residence, more power, biomass, cooling and waste capacity; extra stages need not pay off for cheap stone |

Dust ratios and conservation: [Items](ITEM_CATALOG.md#ore-dust-and-recovery).
Compare throughput, recovery, energy per product and footprint independently.

### Reuse beyond mining

| Workshop | Independent jobs | A different viable arrangement |
|---|---|---|
| Farm/orchard | Cultivation Tissue or Arbor Root grows; Planting Proboscis plants; Harvest Corolla cuts; Work Bed holds; Collection Cilia exports | Retain perennial bushes/trees and collect only mature pods; or replant annual beds from a shared reserved Seed Pouch. Manual harvest and optional workers remain valid. |
| Equipment service | Fuel Papilla transfers real biomass; Healing Dock mends; Item Capsule holds gear; mouths move it | A cheap fuel stop near a mine, a shared healing room, or a later Service Pedestal with both attached. Paying for healing need not also authorize filling every reserve. |
| DNA processing | A pouch/collector captures death loot; an archive holds samples; Extractor consumes them; lens/sequencer improves coverage; bank stores knowledge | Share a lab across species, reserve pristine samples for precision work, or maintain native laboratories near local production. |
| Heat and refining | Furnace performs the recipe; lung supplies airflow; muscle supplies pressure/work; gill exchanges heat; condenser recovers water | Passive cooling with more exposed area, compact supplied cooling, or deliberate safe venting where water is cheap. |
| Spatial/Fold work | Nursery grows material; resonator supplies a condition; conditioner commits a product; collector captures phase work; accumulator stores it | Batch one shared tuned room, isolate parallel rooms, or buffer native phase output instead of powering continuous retuning. |
| Large structures | Controller selects finite work; installed organs do it; mouths/veins carry results; sockets isolate services | A bigger body adds actual bays or support capacity, not a hidden copy of every function in the core. |

World-editing heads need bounded loaded-only work admission across all machines, not one unrestricted scan per organ. Beds and controllers keep finite current jobs, not ever-growing work histories. Cilia do not poll every inventory in a radius; drones use assigned pickup berths, not searches for thousands of dropped fragments. Treatment visuals are capped client displays of stored state. Exact budgets and stress tests must be agreed before implementation; these layouts are design options, not a server-performance certification.

## T0 — First organs: hand-fed growth and useful equipment

Available from ordinary Overworld materials. No completed genome, electricity or Nether trip is required.

### T0-01 — Living Substrate
- **Does:** Replaces exposed ground with living, mottled tissue. Basic, young and mature patches differ in pores, veins and undergrowth. Ordinary occupants are slowed; a full bio suit avoids the penalty.
- **Input → output:** Applied Spore Culture → converted exposed soil or stone. No autonomous unlimited spreading; applying culture does not produce terrain loot.
- **Create:** Apply Spore Culture to a visible eligible ground block; use bone meal to mature that cell. Cultured dirt is also craftable from 1 dirt + 1 Spore Culture.
- **Growth:** Mutate individual cells into conduits, farms, defense or travel surfaces. Each cell has one primary function; a fast path is not simultaneously a damaging floor.

### T0-02 — Culture Bowl
- **Does:** Grows small batches of construction cultures on a shallow, rooted dish. Its first batches are slow and loaded by hand.
- **Input → output:** 1 mushroom + 1 wheat seed + 100 mB water → 1 Spore Culture; 1 Spore Culture + 2 rotten flesh + 1 bone meal → an Organ Bud. No biomass input is needed to start this supply chain.
- **Create:** 1 flower pot + 1 Spore Culture + 1 wheat seed; place on substrate.
- **Growth:** Choose shorter incubation or lower water use. A Honey Culture treatment accepts honey as an alternative nutrient; an Intake Mouth later automates supplies.
- **Manual operation:** Use the reusable Synaptic Probe to open the Bowl screen, select one of its six recipes, start/cancel work and collect a named occupied slot. The screen shows the server revision, stored water, retained slots, progress and clear refusal. Ingredients may still be inserted one at a time and a water bucket deposits 1000 mB and returns its empty bucket; the original stick/empty-hand direct controls remain available when the offhand is empty. Sneak-use a clock or glass bottle to spend an earned choice on incubation or water economy respectively. An unrelated held block/item interaction, including offhand placement with an empty main hand, passes through so players can place beside the Bowl. All transfers pause during incubation; break and replace the Bowl to move its single saved core, owner, contents and history.
- **Baseline:** One batch requires 1200 loaded server ticks (60 seconds at 20 TPS). Each incubation or water choice reduces its base requirement by 10%, capped at 30% after three choices; round water down to whole mB. This gives pure L3 incubation 840 ticks or pure L3 water costs of 70/35 mB. These are calculated initial tuning values, not playtest timings. The Bowl holds nine item slots and one 2000 mB water tank so a whole bucket can refill discounted leftovers without discarding water, performs no export, and pauses when its substrate is absent. Recipes and quantities are owned by Items.

### T0-03 — Digestive Sac
- **Does:** Converts unwanted organic matter into the first usable biomass. Food-rich inputs give more than woody scraps; mineral blocks are not food.
- **Input → output:** Item Catalog's starter/native feed recipes → biomass with their exact water costs. No mineral feed or unlisted fish/leaf recipe; future fibrous diets require explicit recipes.
- **Create:** 1 Organ Bud + 2 rotten flesh + 1 bowl.
- **Growth:** Choose processing speed or biomass recovery. Elastic Gel treatment accepts fibrous leaves; add Intake/Output Mouths for a continuous feed line. It stops accepting batches when output has no room.
- **Hand controls:** Wheat, rotten flesh and biomass buckets operate the Sac. Other held items pass through to ordinary use/placement, including offhand placement with an empty main hand.
- **Baseline:** The starter Sac retains up to 1,000 mB biomass and owns one batch. Hand-feeding atomically reserves one item and its complete output before work starts; loaded work pauses when uprooted and never catches up offline. If the feeder disconnects before completion, the Sac retains their achievement credit and pauses new feeds until they return; stored biomass remains transferable.

### T0-04 — Biomass Bladder
- **Does:** A one-block, translucent feed tank for a first workshop. The contents visibly rise inside its fleshy shell.
- **Input → output:** Biomass from buckets or an attached organ → stored biomass, then bucket or organ output. A manual UI service slot accepts one bio-equipment piece; **Fill** transfers only its missing fuel from the tank, without healing or mutation. This is the T0 refueling route. Other fluids are refused.
- **Create:** 1 Organ Bud + 2 glass + 1 slime ball; an early leather replacement for the slime ball makes a smaller bladder.
- **Growth:** Skeletal Graft treatment reinforces its capacity. It can later become a Biomass Reservoir Cell without discarding its stored feed.
- **Hand controls:** Buckets transfer biomass; unrelated held items pass through to ordinary use/placement, including offhand placement with an empty main hand.
- **Baseline:** The slime-built starter Bladder holds 4,000 mB. Full tanks refuse unchanged. Bucket and equipment-fill actions share 64 portable transfers per server tick and four per player in any 20-tick window; excess is refused without a queue or resource loss.

### T0-05 — Membrane Rack
- **Does:** Stretches cultivated skin over a bone rack to make the common flexible construction material.
- **Input → output:** Rotten flesh + string + water → Membrane Sheets; leather can replace flesh for a slower, low-biomass recipe.
- **Create:** 2 sticks + 2 bones + 1 Spore Culture.
- **Hand controls:** Load ingredients and water by hand; empty-hand use starts a batch. Sneak-use with an empty hand collects one stack while idle, taking finished output first and otherwise returning unused inputs.
- **Growth:** Choose batch speed or water economy. Add a Hearth Lung for faster drying; thermal membranes are a later, distinct Nether-grown material, not an automatic level reward.

### T0-06 — Bone Loom
- **Does:** Grows curved plates and load-bearing ribs from existing calcium, rather than fabricating mineral mass from biomass.
- **Input → output:** 1 bone + 50 BU → 1 Bone Plate. Bone blocks require a separately accounted calcium recipe, not an assumed unpacking into nine bones. A calcite recipe uses extra biomass to supply the organic binder.
- **Create:** 1 Organ Bud + 2 bones + 2 sticks.
- **Hand controls:** Load bones and biomass buckets by hand; empty-hand use starts a batch. Sneak-use with an empty hand collects one stack while idle, taking finished output first and otherwise returning unused inputs. Biomass buckets share the portable-transfer admission and successful-transfer discovery rules.
- **Growth:** Choose speed or biomass economy. Ferrocyte Paste treatment produces reinforced plates using additional prepared iron; adding frame blocks increases batch size, not mineral yield.

### T0-07 — Bio-Furnace
- **Does:** A small stomach-like furnace. Initially slower and more fuel-hungry than a vanilla furnace, but it accumulates permanent processing experience.
- **Input → output:** A normal smelting ingredient + biomass → its normal smelting result. Starting target: 16 seconds per item; no free ore multiplication or extra XP from recooking outputs.
- **Create:** 1 furnace + 1 Organ Bud + 2 Membrane Sheets.
- **Growth:** Choose speed or biomass economy at each level. L1 permits a Char Gland Feed treatment for solid-fuel feeding; Hearth Lungs improve combustion. Mouths automate loading; a T3 Thermal Mantle enables hotter recipes on this same core.

### T0-08 — Awakening Cradle
- **Does:** Turns crafted dormant bio equipment into living equipment. Holds one armor piece, tool or weapon at a time; the body of the cradle folds around it.
- **Input → output:** Dormant equipment + Spore Culture + biomass → awakened equipment with the same identity. It does not supply free healing or an infinite personal fuel reserve.
- **Create:** 1 Organ Bud + 2 Bone Plates + 2 Membrane Sheets.
- **Growth:** Choose shorter awakening or lower biomass cost. Honey Culture treatment reduces awakening's processing biomass cost, not the wearer's starvation pain. Later mutation converts the cradle into a Healing Dock, preserving its history.

### T0-09 — Seed Pouch
- **Does:** A rooted, accessible store for seeds and planting stock; reserves a chosen amount for replanting before offering surplus to automation.
- **Input → output:** Seeds, saplings, carrots or potatoes → the same stored items, supplied on request.
- **Create:** 1 leather + 1 Membrane Sheet + 1 Spore Culture.
- **Growth:** Add neighboring pouches for more stock types. Contractile Fiber treatment adds named planting filters; it never changes one seed species into another.

### T0-10 — Living Skin
- **Does:** The basic biological building material: full blocks, slabs, stairs, rounded corners and thin wall coverings. Neighboring pieces blend their skin pattern.
- **Input → output:** No operating input; produces enclosure and walking surfaces, not biomass.
- **Create:** 1 Membrane Sheet + 1 dirt + 1 Spore Culture → 4 skin blocks. Cutting a block gives the corresponding smaller shapes without multiplying material volume.
- **Growth:** Dye changes coloration; bone meal changes surface maturity. Grafting bone adds decorative ridges. Functional structures still require their specified load-bearing parts.

### T0-11 — Rib Frame
- **Does:** A visibly curved skeletal block used as a beam, corner or arch support. The first reusable multiblock frame part.
- **Input → output:** No running input → structural support.
- **Create:** 2 Bone Plates + 1 Membrane Sheet → 2 frames.
- **Growth:** Ferrocyte Paste reinforces a frame; a Thermal Lining makes a heat-safe version. Upgrading an existing frame keeps the attached structure in place.

### T0-12 — Membrane Window
- **Does:** Transparent living skin held by thin ribs. Connected windows remove interior borders, so a large chamber looks like one enclosed body.
- **Input → output:** No running input → visibility through walls and machine chambers.
- **Create:** 1 glass + 1 Membrane Sheet + 1 bone → 2 windows.
- **Growth:** Dye tints the skin; glow ink highlights its outer ribs. Thermal Lining or Spatial Membrane enables the corresponding chamber wall variant.

### T0-13 — Lumen Tissue
- **Does:** A flush, softly luminous substrate surface. Can line a floor, wall or ceiling without a torch sticking into a tunnel.
- **Input → output:** No continuing fuel → local light; its initial graft is the material cost.
- **Create:** Apply 1 Lumen Secretion to a mature substrate cell; the Bowl prepares it from glow ink, glow berries or glowstone.
- **Growth:** Bone meal changes pore size and brightness. A later Nerve Tissue connection switches it on/off or makes it an alarm indicator.

### T0-14 — Leaching Gland
- **Does:** Prepares a manual mining treatment, not an automatic quarry. The player chooses which exposed rock face to treat.
- **Input → output:** Water + biomass + bone meal → Leaching Nodules. Each nodule treats a small patch of ordinary host rock, leaving ores untouched.
- **Create:** 1 Organ Bud + 1 slime ball + 2 Membrane Sheets.
- **Growth:** Choose nodule production speed or biomass economy. Spider-genome mutation reduces the treatment's dose requirement; it does not dissolve bedrock or increase ore drops.

### T0-15 — Leached Rock
- **Does:** Translucent, weakened stone that lets the player see the nearby ore shape and dig faster with less tool wear. It remains a mineable block, not a pass-through tunnel.
- **Input → output:** A player's tool → the original rock's normal material drop. Embedded ores remain ordinary ore blocks and need an appropriate tool.
- **Create:** Apply a Leaching Nodule to exposed eligible stone, deepslate or similar host rock. Not directly craftable from air.
- **Growth:** None. More advanced treatment increases the affected area or reduces reagent use; it does not turn this block into a resource producer.

### T0-16 — Organ Bud
- **Does:** A planted, dormant organ blank used to grow several early machines in place. A bud alone performs no processing.
- **Input → output:** The chosen organ's construction ingredients → that organ, occupying the bud's position.
- **Create:** Grow in the Culture Bowl, or craft 1 Spore Culture + 2 rotten flesh + 1 bone meal.
- **Growth:** Applying the remaining ingredients of a listed organ recipe is an alternative to crafting that organ as an item. Failed or incomplete grafts leave the bud intact.

## T1 — Circulation: farms, transport and expandable storage

The colony can feed several organs automatically. Upgrades here use T0 products; a genome laboratory is not required for basic logistics.

### T1-01 — Biomass Vein
- **Does:** A recessed green vein within substrate, carrying biomass along configured faces. Pulses follow actual transfer direction.
- **Input → output:** Biomass at an open intake → biomass at connected outlets; no conversion or generation.
- **Create:** Mutate mature substrate with 1 Capillary Gel. A junction supplies the pumping action.
- **Growth:** Bone reinforcement raises transfer capacity. Routes run on floors, walls and ceilings; corners are part of the same block, not separate straight-only pipes.

### T1-02 — Fluid Vein
- **Does:** Moves one selected liquid through a tissue route. Its visible inner fluid differs from the always-green biomass vein.
- **Input → output:** Water or another compatible liquid → the same liquid at a tank or consumer. Incompatible liquids do not silently mix.
- **Create:** Mutate mature substrate with 1 Filter Membrane + 1 Sealing Resin.
- **Growth:** Thermal Lining enables lava service. Changing the filter requires draining the old fluid or moving it to a recovery tank first.

### T1-03 — Item Vein
- **Does:** Moves item packets through buried tissue, including vertically. A stationary alternative to helper creatures.
- **Input → output:** Items accepted by a source mouth → unchanged items at a destination with space.
- **Create:** Mutate mature substrate with 1 Contractile Fiber.
- **Growth:** Elastic Gel treatment groups matching items into larger packets; a Filter Valve chooses destinations. Full outputs leave items at the source or in the vein's finite transit space.

### T1-04 — Vascular Junction
- **Does:** A configurable pumping, splitting and joining organ. A reservoir can sit above it while sources feed in from the side and consumers draw from another side.
- **Input → output:** Biomass, or one selected fluid on a separate junction → the mounted store and configured branches. A port can be intake, outlet, both or closed.
- **Create:** 1 Organ Bud + 2 Membrane Sheets + 1 slime ball.
- **Growth:** Bone meal grows its support stalk from one to two blocks high, leaving walking space under storage. Choose pumping rate or pumping economy; valves add priority and reserve rules. Open mouths visibly close when disabled.

### T1-05 — Septum Crossing
- **Does:** Lets two routes cross through the same substrate block without connecting their contents. Two separated lumens remain visible at the exposed faces.
- **Input → output:** Two independent same-carrier streams → their respective opposite exits, with no mixing.
- **Create:** Apply 1 Membrane Sheet + 1 Bone Plate to an existing vein; choose the second passage direction with the configuration tool.
- **Growth:** Thermal Lining permits hot-fluid crossings. Mutating to a junction deliberately joins routes; crossing is never assumed to mean splitting.

### T1-06 — Vascular Stitch
- **Does:** Bridges a one-block terrain step between two visible vein cells. A recessed tendon curves over the shared terrain edge; no buried target must be clicked.
- **Input → output:** One route's items or fluid → its continuation above, below or around the corner.
- **Create:** Apply 1 Contractile Fiber + 1 Membrane Sheet to the exposed edge between the selected cells.
- **Growth:** Bone reinforcement supports a short free-standing span. Longer hills use repeated visible steps; a stitch does not discover an invisible route automatically.

### T1-07 — Biomass Reservoir Cell
- **Does:** Forms a rounded rectangular reservoir with a fleshy bottom and transparent membrane sides. Adjacent cells share one visible level and remove internal walls.
- **Input → output:** Biomass through buckets or a supporting Vascular Junction → a shared reserve for connected consumers.
- **Create:** Mutate a Biomass Bladder with 2 Bone Plates + 2 Membrane Sheets. Additional cells extend the rectangular body.
- **Growth:** Add a complete row/layer to increase capacity. An incomplete extension waits outside the existing body until valid; existing stored biomass is not erased. A second junction provides another access point, not a second copy of the contents.

### T1-08 — Fluid Cyst
- **Does:** A visible storage cell for water and other non-biomass liquids. Joined cells form one rounded rectangular tank for one liquid.
- **Input → output:** Compatible fluid → stored fluid available through a Fluid Vein or bucket.
- **Create:** 1 Organ Bud + 2 Membrane Windows + 1 Bone Plate.
- **Growth:** Add cells for capacity; Thermal Lining permits hot liquids. Partitioned installations use separate tanks, so a water route cannot contaminate the lava supply.

### T1-09 — Item Capsule
- **Does:** Stores a small number of item types in a fleshy cabinet. A large icon on its front shows the selected contents; joined capsules provide more compartments.
- **Input → output:** Inserted items → the same items through hand access or Item Veins.
- **Create:** 1 chest + 2 Membrane Sheets + 1 Spore Culture.
- **Growth:** Add neighboring capsules for capacity. Skeletal Graft treatment improves compression of stackable items but does not merge different equipment histories. Display icons can be hidden per capsule.

### T1-10 — Intake Mouth
- **Does:** Attaches to an organ or store and pulls from the inventory immediately facing its mouth. Makes a hand-fed organ accessible to ordinary storage automation.
- **Input → output:** Selected items from a chest, hopper or Item Vein → a chosen input slot of the attached organ.
- **Create:** 1 Organ Bud + 1 hopper + 1 Membrane Sheet.
- **Growth:** Route Imprint treatment adds an item filter; a Nerve Tissue connection permits on/off control. Multiple mouths may serve separate ingredients, but never bypass the organ's recipe requirements.

### T1-11 — Output Mouth
- **Does:** Extracts completed products from a machine, without stealing its ingredients, fuel or reserved seed stock.
- **Input → output:** Completed items from the host → a neighboring inventory or Item Vein. No space means no extraction.
- **Create:** 1 Organ Bud + 1 hopper + 1 bone.
- **Growth:** Route Imprint treatment chooses output/byproduct filters. Pair with Intake Mouths to automate the original Bio-Furnace instead of replacing it with a new machine.

### T1-12 — Filter Valve
- **Does:** Gives a vein branch an explicit destination/filter and minimum source reserve. Its open or sealed throat is visible from outside.
- **Input → output:** A permitted item or fluid stream → admitted flow; rejected material stays upstream.
- **Create:** 1 Bone Plate + 1 slime ball + 1 paper, applied to a vein face.
- **Growth:** Synaptic Gel treatment adds signal control. Later electrical mutation offers proportional flow rather than only open/closed operation.

### T1-13 — Overflow Valve
- **Does:** Diverts excess to a selected spare tank or disposal line when the main store reaches its configured threshold.
- **Input → output:** Excess biomass or fluid → the relief destination. It never deletes material merely because both routes are full.
- **Create:** 1 Filter Valve + 1 comparator + 1 Membrane Sheet.
- **Growth:** Skeletal Graft treatment adds a reserved emergency outlet. A tank visible through the valve's window lets the player distinguish a full reserve from a blocked relief route.

### T1-14 — Sensor Polyp
- **Does:** Measures one attached organ or storage: fullness, low fuel, blocked output or completed batch. Useful before a central control system exists.
- **Input → output:** A selected measured value → redstone strength or a nerve signal.
- **Create:** 1 Organ Bud + 1 comparator + 1 spider eye.
- **Growth:** Survey Gel treatment gives separate upper/lower thresholds to prevent rapid switching. It observes its configured target, not every inventory in the base.

### T1-15 — Nerve Tissue
- **Does:** A thin, flush signal path on substrate. Carries commands separately from resource veins; intersections can be joined or insulated.
- **Input → output:** Redstone or organ-control signal → the configured connected outputs. No transport of items, fluids or electrical power.
- **Create:** Apply 1 Synaptic Gel to mature substrate.
- **Growth:** Membrane graft insulates a crossing. Glow ink gives visible pulses for diagnosis; turning those visuals off does not stop the signal.

### T1-16 — Synaptic Console
- **Does:** Opens a readable local-network UI: selected organ, port directions, reserves, current transfer and the reason a route has stopped.
- **Input → output:** Player configuration and measured network values → saved rules and a displayed result; no resource production.
- **Create:** 1 Organ Bud + 1 amethyst shard + 1 comparator + 1 Membrane Window.
- **Growth:** Add a Display Membrane later for wall readouts. Electrical mutation enables schedules; remote access requires an explicitly connected relay rather than unlimited world access.

### T1-17 — Cultivation Tissue
- **Does:** Replaces farmland with rooted crop beds. Maintains growing conditions; it does not plant, harvest or export the crop. Seed reserves belong to a Seed Pouch and routing rules, not an invisible inventory in every soil cell.
- **Input → output:** Seeds/plants + water + optional biomass → grown crops. Biomass accelerates growth but does not make the crop-to-biomass loop profitable by itself.
- **Create:** Hoe mature exposed Living Substrate, then apply 1 Rooting Gel and a real seed; the hoe is not consumed.
- **Growth:** Honey Culture treatment favors pollinated crops; bone meal favors rapid first growth. Upgraded beds can support unusual crops only after their genome and growing conditions are available.

### T1-18 — Harvest Corolla
- **Does:** Cuts one ready crop/pod in its assigned small plot per work cycle. It never replants or distributes its harvest. Its tendrils follow the selected plot, not a biome-wide search.
- **Input → output:** One eligible mature crop, bush harvest or canopy pod + biomass → that target's actual produce in a reserved adjacent Work Bed or capsule; the target becomes its declared harvested state. No free second seed or wood yield.
- **Create:** 1 Organ Bud + 1 iron hoe + 2 Membrane Sheets.
- **Growth:** Choose harvest speed or biomass economy. A skeletal cutting graft allows declared Living Wood pruning/salvage recipes; an aquatic graft serves supported submerged crops. A separate Planting Proboscis replants annual beds; Collection Cilia or a courier exports the harvest. Wider reach needs supplied physical tendril segments and a bounded selected plot, not extra simultaneous crops per free cycle.

### T1-19 — Compost Gland
- **Does:** Recovers fertilizer from low-value organic scraps. It complements digestion rather than being a second machine with the same biomass output.
- **Input → output:** Leaves, crop scraps and spoiled feed + water → bone meal. Food can be digested or composted, not processed through both for full yield.
- **Create:** 1 composter + 1 Organ Bud + 1 Membrane Sheet.
- **Growth:** Choose processing speed or water economy. Mushroom mutation accepts woody residue; an attached Output Mouth feeds the farm without dropping fertilizer on the ground. The T2 Spent Penetrant compost recipe returns fertilizer with real leaf/water input; ore treatment need not wait for T4 waste disposal.

### T1-20 — Arbor Root
- **Does:** Binds and tends one selected tree, preserving a trunk/canopy plan and allocating its growth between wood, pods and resin. It does not cut, collect or replant the tree.
- **Input → output:** A real planted tree + water/nutrients → supplied Living Wood and canopy growth. Mature branches/pods become harvestable by a player or Harvest Corolla; a Sap Tap draws resin from the same growth budget.
- **Create:** 1 Organ Bud + 1 Living Wood + 1 sapling + 1 iron axe; place against the selected trunk on substrate. Manual log grafting supplies the first Living Wood.
- **Growth:** Choose regrowth speed or nutrient economy. Species genomes unlock particular grafts. Marking finite salvage authorizes a suitable cutter to consume selected existing wood rather than maintain it; the root never pays the wood yield itself. A Planting Proboscis can establish a replacement from retained real stock.

### T1-21 — Digestive Tissue
- **Does:** A defensive floor that damages hostile creatures and gathers biomass from damage actually dealt; players and tamed pets are excluded.
- **Input → output:** Successful damage to a vulnerable living target → a small biomass reserve, extractable by an adjacent vein. Excluded or invulnerable targets give no biomass.
- **Create:** Mutate mature substrate with 1 Digestive Enzyme.
- **Growth:** Spider DNA permits a damage-versus-slowing specialization. Nerve Tissue provides a shutoff for moving assigned specimens across the floor. Full storage stops collection, not hostile-target damage.

### T1-22 — Travel Tissue
- **Does:** A deliberately marked fast surface for full bio suits with a locomotion mutation. Everyone else receives ordinary substrate movement, not a launch effect.
- **Input → output:** An eligible moving wearer and suit biomass → faster travel along the surface.
- **Create:** Mutate mature substrate with 1 Locomotor Gel.
- **Growth:** Rabbit-genome graft improves acceleration; slime-genome graft improves cornering. Wall/ceiling climbing requires its own suit mutation, not just a faster floor.

### T1-23 — Climbing Tendon
- **Does:** A slim, luminous climbing strip rooted flush into a shaft wall. Gives mines a reusable physical route even without specialized armor.
- **Input → output:** Player movement → climbing; no ongoing machine fuel.
- **Create:** 1 vine + 1 string + 1 Spore Culture → 4 tendon sections. Lumen Secretion adds the luminous form.
- **Growth:** Bone-plate graft creates resting ledges. Mining organs can place supplied sections behind their cutting front; a missing supply pauses work before access is lost.

### T1-24 — Hearth Lung
- **Does:** Supplies airflow to one attached furnace, drying rack or compatible culture chamber. Requires free space at its breathing face; it does not provide heat or perform the host recipe.
- **Input → output:** Air + the host's declared work/feed cost → airflow service. A furnace can burn its actual fuel more effectively; a rack can dry faster without receiving free furnace heat. No independent processing slots.
- **Create:** 2 leather + 1 Bone Plate + 1 Organ Bud; attach to a compatible host's service face.
- **Growth:** Add a second opposed lung for batch work. Contractile Fiber treatment favors speed; charcoal mutation favors fuel economy. Blocking a lung removes its benefit rather than destroying the furnace.

### T1-25 — Sapping Bush
- **Does:** Useful low ground cover for living paths and gardens. Its growth settings select berries, fiber or biomass feedstock, so the colony does not become a bare carpet.
- **Input → output:** Water, light and nutrients → the selected harvest; harvest reserves enough plant body for regrowth.
- **Create:** Graft a sweet berry bush with Rooting Gel, or plant its recovered cutting on substrate. Converting an existing bush retains a seed/cutting for the player.
- **Growth:** Contractile Fiber treatment favors fiber; honey favors berries. A Harvest Corolla gathers the crop into storage; decorative unharvested bushes remain alive without constant manual trimming.

### T1-26 — Living Wood
- **Does:** The mod's own wood resource: fibrous trunks with colored sap channels, usable for rooted organ frames and reinforced construction. Preserve the tree's branching shape, not its vanilla wood drops.
- **Input → output:** One harvested converted log → one Living Wood block. Sap/Canopy Cyst production requires a fed standing tree. Harvested wood can instead enter an explicit digestion recipe through supplied veins; conversion itself pays no biomass or extra vanilla logs.
- **Create:** Apply Rooting Gel to an exposed log, or assign an Arbor Root to convert a selected tree while supplied. One existing log becomes one Living Wood block. The root works only within a selected 9×9×24 volume, at most one loaded cell per second; insufficient feed or blocked access pauses it.
- **Growth:** Skeletal Graft makes reinforced structural wood: 1 Living Wood + 1 Skeletal Graft → 1 reinforced Rib Frame. The frame is a prepared-wood alternative to reinforcing a bone frame, not additional output. Regrowth requires water, nutrients and time.

### T1-27 — Canopy Cyst
- **Does:** A leaf replacement with hanging translucent pods. Maintains a tree's canopy while supplying one selected kind of fruit, resin or planting stock.
- **Input → output:** The linked tree's water/nutrients and light → its selected pod harvest. Breaking a pod yields its stored product, not an additional mature harvest.
- **Create:** Graft leaves belonging to an Arbor Root tree with a Membrane Sheet and Spore Culture.
- **Growth:** Glow ink creates a luminous canopy; honey mutation favors food pods. Later tree genomes unlock different products without turning every leaf into a universal item generator.

### T1-28 — Sap Tap
- **Does:** Collects resin from an assigned Living Wood trunk, leaving it standing. Resin is an alternative biological binder for recipes otherwise using slime balls.
- **Input → output:** A mature fed tree → resin, at the cost of part of that tree's other growth budget.
- **Create:** 1 bucket + 1 Membrane Sheet + 1 bone; attach to Living Wood.
- **Growth:** Choose collection speed or reduced nutrient demand. More taps on one tree share its production rather than multiplying it; a Fluid Cyst or Item Capsule receives the chosen resin form.

### T1-29 — Sphincter Door
- **Does:** A living doorway that retracts into its rim, keeping a biological room usable without mechanical-looking doors. Supports deliberate access control.
- **Input → output:** Player use, redstone or nerve command → open/closed doorway. No operating biomass for ordinary doors.
- **Create:** 2 Membrane Sheets + 1 Rib Frame → one two-block doorway.
- **Growth:** Thermal Lining makes a sealed process hatch; bone reinforcement makes a stronger gate. It does not automatically close on an occupied doorway to trap its authorized user.

### T1-30 — Activation Cyst
- **Does:** Cultures raw ingredients into consumable active gels, enzymes and secretions. Its side windows show separate source and carrier chambers.
- **Input → output:** Listed ingredient + biological carrier and feed → the Item Catalog's named active treatment; returned containers go to a reserved slot.
- **Create:** 1 Organ Bud + 2 Membrane Sheets + 1 Bone Plate. No advanced graft is needed to build it.
- **Growth:** Choose batch speed or biomass economy. Intake/Output Mouths automate it; one recipe mode at a time. Bowl-compatible recipes run faster, but give the same yield.

### T1-31 — Planting Proboscis
- **Does:** Plants one supplied seed, sapling or cutting into a selected eligible empty growing cell, then stops or waits for another ready cell. Separate from growth and harvesting.
- **Input → output:** Real planting stock from its slot/Seed Pouch + biomass + a valid prepared site → one planted crop/tree. Failed placement consumes neither seed nor biomass.
- **Create:** 1 Organ Bud + 1 wooden hoe + 1 Rooting Gel + 1 Membrane Sheet.
- **Growth:** Choose planting rate or biomass economy. A graft supports submerged planting; a shared Seed Pouch can feed several planters with species filters. Plot and occupancy checks prevent planting a sapling inside a mature canopy or replacing a player block.

### T1-32 — Work Bed
- **Does:** A flush mature-substrate work surface. Holds either one presented block or one finite item batch for nearby organs to work on. A translucent lip and visible sample show its state; there is no standing border around the mining site.
- **Input → output:** Hand placement, a mouth or Grasping Root → one retained target; processing changes that target in place. Proposed item mode: four ordinary stack slots, not a general-purpose storage network. Block mode occupies the space directly above the bed and excludes item mode. An idle mineral target can be wrapped as a Mineral Workpiece for transport to another bed; its process state is retained, not reset.
- **Create:** Mutate one exposed mature substrate cell with 1 Membrane Sheet + 1 Sealing Resin. Require one clear block above it for a block specimen.
- **Growth:** No processing XP. A Filter Membrane liner admits declared wet treatments; Thermal Lining admits hot ones. Additional beds create independent work positions, not one unlimited merged inventory. A local ready/working/result-held/blocked readout and comparator face are included; a tool selects the expected process sequence.

### T1-33 — Collection Cilia
- **Does:** Exports the completed contents of one facing Work Bed into one adjacent inventory or Item Vein. It does not harvest, break specimens or gather every item in a radius. The cilia lie against the bed's edge.
- **Input → output:** A released batch or wrapped Mineral Workpiece + accepting destination + biomass → those same items transferred, leaving the bed ready. Unreleased specimens and working targets are inaccessible; wrapping for transport never marks an untreated specimen as chemically treated.
- **Create:** 1 Organ Bud + 1 string + 1 Contractile Fiber + 1 Membrane Sheet; attach to a bed edge.
- **Growth:** Elastic Gel treatment increases packet size within its fixed limit. No counters from shuttling items. A courier may serve several beds instead, accepting travel delay and bounded pathfinding cost; neither method requires loose item entities.

### T1-34 — Reflex Knot
- **Does:** A small local control organ with one chosen function: ALL conditions, ANY condition, or set/reset latch. At most four named input states and one held output; combine knots for larger logic.
- **Input → output:** Redstone levels or connected ready/complete/blocked states → a stable permit/start condition. A latch retains its state across unloading; it does not replay elapsed ticks.
- **Create:** 1 Organ Bud + 2 redstone dust + 1 Synaptic Gel. No quartz, electricity or completed genome is required.
- **Growth:** No processing XP. Insulating membrane separates crossing signals. It has no recipe slots, script language, scanning radius or request queue. State changes trigger work; a delayed safety timeout reports a fault rather than forcing the next processing step.

### T1-35 — Selector Ganglion
- **Does:** Selects one branch at a split or one source at a merge: one common endpoint and at most three named branches. Select fixed priority with reserves or round-robin among ready branches; show which branch owns the exclusive grant. Transport remains a separate operation.
- **Input → output:** A source's held request and destination readiness → one exclusive grant until completion or safe cancellation. It carries no item/fluid itself; mouths, veins and valves perform transport.
- **Create:** 1 Reflex Knot + 1 Filter Valve + 1 Synaptic Gel.
- **Growth:** No processing XP. Add another selector for a separate work group, not an unbounded central queue. Round-robin prevents a busy common-ore line starving a rare-ore bed; fixed priority deliberately favors emergency feed. A stale grant is revalidated after reload, never awarded twice.

### T1-36 — Fuel Papilla
- **Does:** Mutated Living Substrate, flush with the ground, that feeds worn armor. No separate free-standing feeder, healing head or digestive function.
- **Input → output:** Colony biomass → biomass delivered to authorized selected worn pieces. Base rate10BU/s plus ceil(delivered/5)BU overhead per one-second batch. Works with partial suits; full pieces are skipped. Source reserve and maximum fill are configurable; it is not a hidden reservoir or a through-vein.
- **Create:** Apply 1 Capillary Gel + 1 Membrane Sheet to exposed mature substrate. Keep maturity and compatible reinforcement; drain/replace any previous primary function first.
- **Growth:** Add four adjacent Biomass Vein cells and four corner Rib Frames in a3×3 flush service pad:40BU/s with ceil(delivered/20)BU overhead per batch. One pad serves one wearer; no multiplicative stacking. No learning from filling/circulating biomass. Probe/UI starts filling without intercepting block placement. Healing Dock remains a separate service.

### T1-37 — Temporary Membrane
- **Does:** Weak translucent temporary support grown by the handheld Membrane Projector.
- **Input → output:** Armor biomass paid by the projector → one temporary support block → expiration with no renewable item output.
- **Create:** Membrane Projector placement only; recipe, fuel, support, lifetime and admission limits are defined with I124 in Items.
- **Growth:** No processing level, drops or permanent reinforcement. It cannot support another Temporary Membrane or provide automatic fall-arrest.

### T1-38 — Sample Dock
- **Does:** Unloads one inserted Sample Pouch to explicitly connected Specimen Archives.
- **Input → output:** Actual pouch samples + accepted destination → the same samples transferred; no extraction or global storage access.
- **Create:** 1 Organ Bud + 1 Filter Membrane + 1 Item Capsule.
- **Growth:** Passive service, no processing counts. Transfers at most 16 samples per second; all/selected/surplus modes preserve configured reserves.

### T1-39 — Dew Gland
- **Does:** Squeezes clean process water from allocated fresh harvest; no creature DNA or biomass production.
- **Input → output:** The native supply recipes in Items consume exactly two harvested items into 1000 mB water; retain water or pause.
- **Create:** 1 Organ Bud + 2 Membrane Sheets + 1 Filter Membrane; root on ordinary or the required native bed.
- **Growth:** Choose speed or biomass economy. Thermal Lining treatment permits Nether fruit; Spatial Membrane treatment permits chorus. No electricity required for first operation.

## T2 — Directed mutation: genomes, specialized organs and planned excavation

Build sample processing and a physical DNA bank. A genome is available only through a connected loaded compatible bank; no personal research level grants remote access. Collecting unrelated samples is not a substitute.

### T2-01 — Specimen Extractor
- **Does:** Separates usable genetic material from source-labeled mob drops, plants and tissue samples. The basic organ wastes more of a rare specimen than an upgraded laboratory would.
- **Input → output:** Identified specimen + water + biomass → source-specific Genetic Stock and a readable genome fragment. The batch also retains 50 mB spent process water; reserve this fluid output and any returned vial before consumption.
- **Create:** 1 Organ Bud + 2 glass bottles + 1 iron ingot + 2 Membrane Sheets.
- **Growth:** Choose biomass economy or speed. A Sequencing Lens previews remaining coverage; R2/R3 service configurations increase information recovery. Display expected coverage before consuming a valuable specimen.

### T2-02 — DNA Bank
- **Does:** Stores accumulated genome knowledge, shows missing coverage and makes completed genomes available to connected mutation organs. Knowledge is distinct from consumable Genetic Stock.
- **Input → output:** Extracted fragments → increased coverage for the correct source; research queries → available traits and compatible mutation recipes.
- **Create:** 1 Organ Bud + 1 amethyst shard + 2 glass + 2 Bone Plates.
- **Growth:** Archive Lobes add capacity, not permission to store more complex DNA. A Precision Sequencer service attachment upgrades the bank to precision storage; a Genome Vault supplies the exotic grade.

#### DNA storage grades

One slot holds one source's partial or complete genome. Coverage belongs to the
physical bank/lobe, not the player or guide. Unsupported fragments are refused
unchanged. Stock and specimen items remain separate inventories.

| Storage body | Base slots | DNA supported | Archive expansion |
|---|---|---|---|
| DNA Bank | 16 | Ordinary sources | +8 per lobe, up to 4 lobes |
| DNA Bank with Precision Sequencer service | 32 | Ordinary and complex sources | +8 per lobe, up to 4 lobes |
| Genome Vault | 128 | All three grades, including exotic sources | +16 per lobe, up to 8 lobes |

Complex sources: enderman, blaze, ghast, shulker, guardian, elder guardian, wither
skeleton and phantom. Exotic sources: Wither, Ender Dragon and Fold organisms.
All other currently listed vanilla sources are ordinary. New source definitions
declare their grade. A disconnected sequencing service preserves complex records
but cannot supply them to processes. Removing a lobe moves its records with it;
records beyond reduced capacity remain stored but inactive until capacity is
restored; insertion cannot displace them. One physical lobe holds at most 16 records.

#### Laboratory and native process requirements

All columns are simultaneous conditions checked before committing a batch.
R1/R2/R3 are laboratory configurations, not player ranks. The multiplier applies
only to coverage, not stock, ordinary loot or mineral recovery.

| Configuration | Coverage multiplier | Extractor level | Installed services | Extra cost per sample |
|---|---|---|---|---|
| R1 | 1 | L0 | Extractor and compatible DNA Bank | None beyond Item Catalog extraction recipe |
| R2 | 4 | L2 | Sequencing Lens and Precision Sequencer on separate service faces; Charge Sac supplying them | 100 BU and 2000 BE |
| R3 | 8 | L3 | R2 plus Ion Separator, Levitation Chamber and Cold Lobe in a sealed body | 400 BU, 16000 BE, 1 shulker stock and 100 mB cooling water per batch; retain 100 mB warmed water |

R3 consumes shulker stock; it never extracts shulker coverage from that stock.
The first shulker genome is resolved at R1/R2, before this service exists.
Precision Sequencer may provide a bank's complex-storage grade even when the
attached Extractor has not yet earned R2. A Genome Vault admits bosses at any
eligible extraction configuration; it needs no boss genome to construct.

| Source's native class | Species | Extraction and matching stock-culture conditions |
|---|---|---|
| Ordinary | Overworld creatures/plants, including enderman | Any dimension; ordinary supported bed and compatible storage grade |
| Nether | Blaze, ghast, magma cube, wither skeleton, Wither; crimson/warped fungus | Actual Nether; 3×3 Thermal Substrate bed; Extractor/Vat L1; thermal mutation made with 2 Thermal Linings + 200 BU per core |
| End | Shulker, Ender Dragon, chorus | Actual End; 3×3 Anchored Substrate bed and four corner Anchor Roots; Extractor/Vat L2; spatial mutation made with 2 Spatial Membranes + 400 BU per core |
| Fold | Native plants/creatures and Manyfold | Actual Fold; Adaptive Substrate bed, supplied Habitat Lung, Extractor/Vat L3; 1 Native Specimen Seal per extraction |

Class follows registered species, not the location of death. Moving a blaze into
the Overworld cannot change its processing requirements. Samples and completed
records travel; stocked material and finished grafts may be used elsewhere.
Continuing native stock culture maintains the need for each laboratory.
An imported native substrate block does not satisfy the dimension predicate.

A generic organ trait is not automatically a native mutation. The above thermal
and spatial installations consume their listed prepared materials at a Mutation
Chamber and preserve the core's earned levels; they supply process tolerance,
not a different species class. A Fold habitat supplies its external containment.

### T2-03 — Archive Lobe
- **Does:** A physical memory extension to a DNA Bank, showing stored species on its membrane. Later control organs use the same lobe for recipe or index records instead of needing a second memory-block family.
- **Input → output:** Bank records, or a controller's recipe/index records, assigned to its slots → retained records accessible through that host.
- **Create:** 1 Membrane Window + 1 amethyst shard + 1 Membrane Sheet.
- **Growth:** Attach more lobes to valid faces. Removing one preserves its assigned records in that lobe; the same records cannot remain as a second physical archive copy by accident.

### T2-04 — Sequencing Lens
- **Does:** A precision eye attached to an extractor. It reads the bank's remaining coverage and previews the next batch's information gain.
- **Input → output:** A selected specimen and accessible bank record → coverage/cost preview; installed precision services set the multiplier, not extra mob loot.
- **Create:** 2 glass + 1 amethyst shard + 1 spider eye + 1 Membrane Sheet.
- **Growth:** Lumen Secretion improves the visible readout; precision electrical service enables R2/R3 work. Multiple lenses cover different sample channels, not unlimited yield multipliers on one specimen.

### T2-05 — Genetic Culture Vat
- **Does:** Maintains consumable genetic cultures after the source genome is understood. Avoids requiring another boss kill for every routine use of an already-developed mutation.
- **Input → output:** Completed genome access + a seed of matching Genetic Stock + recipe-specific feed and biomass → more stock of that same culture. No bones, pearls, stars or other mob loot are produced. A separate material-binding mode grows Fusion Binder and mineral fusion media without creature DNA, using the Item Catalog recipes.
- **Create:** 1 Fluid Cyst + 1 Organ Bud + 1 Sequencing Lens.
- **Growth:** Choose culture speed or biomass economy. Rare cultures require later precision attachments and their own compatible feed; wheat alone is not a substitute for every source.

### T2-06 — Mutation Chamber
- **Does:** Applies chosen genetic and material changes to one equipment item or recoverable organ core. Shows the resulting properties and incompatible mutations before starting.
- **Input → output:** Target + prepared target-specific Mutation Graft or fusion medium + biomass + genome access → the same target with the chosen mutation; its counters and identity remain.
- **Create:** Core recipe: 1 Awakening Cradle + 1 Sequencing Lens + 2 Bone Plates. Form a 3×3 floor, corner Rib Frames and a two-block-high Membrane Window enclosure around the central treatment space.
- **Growth:** Choose treatment speed or biomass economy. Burrowing armor uses combined silverfish/enderman prepared grafts and Diamond-Fiber Matrix under Armor Evolution. Further burrowing mutations extend the safe stopping window and reduce biomass use, competing with other suit improvements. A potion-infusion bay is a separate addition; a larger body accepts larger organ cores. Precision Chamber means this core/body with a T4-07 Precision Sequencer service bay, electrical supply and access to T4-05 prepared outputs; it is not an unlisted new block.

### T2-08 — Healing Dock
- **Does:** Heals one authorized worn piece or stored bio-equipment target using colony supplies. It neither refuels the target nor moves inventory. A mine can have a fuel berth without installing this separate healing operation.
- **Input → output:** Damaged bio equipment + biomass + the healing recipe's materials → restored tissue durability on that same piece. Healing feed is spent on recovery, not also deposited into its reserve.
- **Create:** Mutate an Awakening Cradle with 2 Restorative Serum + 2 Membrane Sheets.
- **Growth:** Choose healing speed or biomass economy. Hanger-like frames hold a set serviced one piece at a time; more healing heads enable independent work. Add a Fuel Papilla for a separate fill operation and mouths for materials. C-R credits healing of the worn chestpiece only; dock service grants no armor-learning points.

### T2-09 — Grafting Bench
- **Does:** Joins a known plant trait to compatible planting stock, so crop and tree selection affects the farm layout and its products.
- **Input → output:** Seed/sapling + matching Genetic Stock + biomass + bank access → grafted planting stock with its stated nutrient, light and water requirements.
- **Create:** 1 Bone Loom + 1 iron hoe + 1 Sequencing Lens.
- **Growth:** Choose graft speed or biomass economy. Add two Seed Pouches to keep parent lines separate. New combinations need their species research and preparation recipes, not an assumed player-tier increase.

### T2-10 — Ration Kitchen
- **Does:** Turns a varied farm's products into useful expedition meals. A meal can emphasize saturation, travel endurance or suit feeding, with one selected recipe per batch.
- **Input → output:** Listed cooked food/crops + bowls or bottles + heat → the chosen ration and returned containers where appropriate. No biomass-to-human-food shortcut by default.
- **Create:** 1 Bio-Furnace + 1 bowl + 1 honeycomb + 1 Membrane Sheet.
- **Growth:** Choose cooking speed or fuel economy. Add ingredient mouths for mixed recipes; attach a Cold Lobe later for refrigerated ingredients. One food cannot grant all specialist benefits simultaneously.

### T2-11 — Brewing Gland
- **Does:** A biological brewing stand using ordinary potion ingredients and container rules. Makes colony-produced potions useful without requiring bio armor to abandon vanilla brewing.
- **Input → output:** Water bottles/potions + brewing ingredients + fuel → the corresponding potions. Unsupported effects are not invented from a creature's name.
- **Create:** 1 brewing stand + 1 Organ Bud + 2 Membrane Sheets; available once its vanilla ingredient is obtained.
- **Growth:** Choose batch speed or biomass supplement economy. A Potion Infuser later connects these products to mutation treatment; direct potion drinking remains valid.

### T2-12 — Aquaculture Bed
- **Does:** A submerged version of cultivation tissue for kelp, seagrass and selected aquatic stock. Keeps an ocean branch useful independently of Nether industry.
- **Input → output:** Existing aquatic planting/brood stock + valid water habitat + supplied nutrients → growth of that stock. It provides the habitat, not automatic planting, breeding, hatching or collection; plants do not become fish.
- **Create:** Mutate submerged mature substrate with 1 Aquaculture Graft.
- **Growth:** Fish-genome graft improves husbandry; kelp-genome graft improves plant production. Use an aquatic Planting Proboscis and Harvest Corolla for crops, or Feeding Trough/Incubation Basket service for supported animals. Shared population limits and reserved berths constrain every hatch; there is no independent fish-spawning timer per bed.

### T2-13 — Brood Nursery [optional helper branch]
- **Does:** Hatches a small set of larvae, then accepts a chosen worker mutation. A 3×3 living bed provides separate berths around the core rather than one shared spawn point.
- **Input → output:** Biomass + larval growth materials + a permitted worker graft → an assigned worker. A full roster or occupied berth blocks hatching.
- **Create:** 1 Organ Bud + 1 egg + 2 Membrane Sheets + 2 Bone Plates; install on a clear 3×3 substrate bed.
- **Growth:** First worker role: courier between assigned Work Beds and Item Capsules, moving held outputs without loose drops. It trades transit time and pathfinding for shared service across separated beds. Limit three workers per nursery, one carried stack each, and 16 route points per worker. Idle workers occupy separate nursery berths; death frees a berth, and recall relocates the worker with cargo retained. Harvesting is a separate future profession, not a courier side effect.

### T2-14 — Worker Waypoint [optional helper branch]
- **Does:** A nearly flush marked tissue cell defining a worker's route or waiting place. Routes can follow stairs, terraces and climb-capable paths.
- **Input → output:** Nursery assignment → a named route point and visible occupied/free state.
- **Create:** Mutate mature substrate with 1 Waymark Secretion, then link it to a nursery.
- **Growth:** Contractile Fiber treatment makes it a waiting perch for an eligible flying helper. Adding waypoints refines a route; it does not authorize a worker to search unloaded terrain.

### T2-15 — Sail Roost [optional helper branch]
- **Does:** Feeds and houses one Nutrient Sail. The player assigns the sail to named nearby nurseries, giving it a useful colony-support role rather than mandatory ownership of every chunk.
- **Input → output:** Biomass + an installed sail organism → supplied local worker coordination and status display.
- **Create:** 2 Rib Frames + 1 Organ Bud + 2 Membrane Sheets + 1 feather.
- **Growth:** Locomotor Gel improves feeding throughput; Survey Gel improves monitoring. A late spatial anchoring mutation provides explicitly enabled, paid chunk loading under the contract below.

#### Paid chunk loading

Mutate an occupied Sail Roost with 8 precision-conditioned Spatial Membranes,
4 Living Netherite Lamellae, 4 Fusion Binder and 32,000 BU. This is a late upgrade,
not a property of ordinary sails, routes, freight endpoints or nurseries.

One enabled roost keeps **its own chunk** ticking at **20 BU/s**, prepaid from a
local 1,200 BU buffer in one-second intervals. The sail remains inside that chunk.
A depleted buffer, absent/dead sail, removed roost or disabled setting releases
the ticket. A stopped roost cannot wake itself; revisit it to refuel/re-enable.
The panel previews the chunk boundary, fuel endurance and server quota refusal.

Initial hard ceiling: **4 active roosts per player and 16 per server**, across
dimensions; no overlapping tickets for the same chunk and no refund by swapping
owners. Persist only admitted roosts, revalidate their prepaid interval and actual
organ/sail on restart, then release stale tickets. No offline catch-up production.
Farms and freight must fit their ticking area or wait at unloaded boundaries.
Costs are gameplay tuning; ticket counts still require a loaded-factory soak test.

### T2-16 — Aerocyte Bloom
- **Does:** Air defense: tracks a selected hostile flying target and fires a single visible green biological pellet. It needs a clear shot and does not target everything through walls.
- **Input → output:** Biomass from its reserve or a vein → attacks on configured flying hostiles. No loose mob-farm reward stream is created by unattended defense.
- **Create:** 1 Organ Bud + 1 bow + 2 Bone Plates + 1 spider eye.
- **Growth:** Choose reload speed or biomass economy. Skeleton-genome mutation trades rate of fire for range; blaze-genome mutation changes ammunition resistance at higher cost. Boss damage remains bounded by the encounter's defenses.

### T2-17 — Repellent Crown
- **Does:** Keeps selected ordinary nuisance mobs away from a small protected work area. An alternative to killing every visitor at a farm or entrance.
- **Input → output:** Biomass + the selected scent ingredient → a local avoidance effect on susceptible targets.
- **Create:** 1 Organ Bud + 1 fermented spider eye + 1 flower + 1 Membrane Sheet.
- **Growth:** Choose scent duration or biomass economy. Cat-genome mutation supports phantom deterrence; target-specific recipes do not grant universal boss or player repulsion.

### T2-18 — Lure Polyp
- **Does:** Draws susceptible nearby creatures toward a marked feeding or defensive location. The creature still needs a navigable path.
- **Input → output:** Target-specific food/scent + biomass → attraction toward the polyp, not spawned mobs.
- **Create:** 1 Organ Bud + 1 honey bottle + 1 spider eye.
- **Growth:** Choose scent duration or nutrient economy. Rabbit, fish and livestock genomes unlock appropriate lures; a Nerve Tissue signal disables attraction when the destination pen is full.

### T2-19 — Restraining Tissue
- **Does:** Holds or heavily slows susceptible creatures on a small husbandry/defense floor. It is separate from lethal Digestive Tissue.
- **Input → output:** Supplied biomass + contact with a permitted target → restraint until duration or fuel runs out.
- **Create:** Mutate mature substrate with 1 Restraining Graft and spider-genome access.
- **Growth:** Slime-genome graft improves restraint strength; spider-genome graft improves duration. Stronger mobs resist more; players and bosses require explicit targeting rules, not automatic indefinite immobilization.

#### Initial defense recipes and effects

These baselines target hostile mobs only; players and tamed animals are excluded.
Crown, Lure and Restraining Tissue also exclude bosses. Attack organs respect
the target's ordinary defenses. Distances are blocks; damage is health points.

| Organ | Range | Paid operation | Initial effect |
|---|---|---|---|
| Aerocyte Bloom | 24 | 5 BU, one shot per 40 ticks | One green projectile; 4 damage to an eligible flying hostile |
| Spine Sentry | 16 | 2 BU + 1 Grown Spine, one shot per 40 ticks | One projectile; 4 damage to an eligible ground hostile |
| Repellent Crown | 8 | 5 BU + 1 Scent Concentrate per 40-tick pulse | Susceptible hostiles request a reachable local escape path; influence expires after 40 ticks |
| Lure Polyp | 8 | 5 BU + 1 Scent Concentrate per 40-tick pulse | One idle hostile approaches a reachable adjacent cell for at most 40 ticks; an existing attack target wins |
| Restraining Tissue | Contact | 2 BU per target per 20 ticks | 20% movement reduction for 20 ticks; overlapping cells do not stack or double-charge |

Refuse before spending if the operation has no eligible target, supply or shared
admission. No teleport, spawning, chunk loading or profitable projectile drops.
Lure selects nearest, then entity ID. Conflicting scent requests in one tick use
block-position order; keep one transient influence per target, not a request log.
Share at most 64 defense queries/tick, 16 returned candidates/query, 4 local
defense path requests/tick and 128 active colony projectiles/server.

Husbandry treatments replace the hostile Lure mode; they do not run alongside it.
At the Chrysalis, prepare one I053 Organ Trait Graft from 2 matching Genetic Stock
+ 1 matching feed + 1 Fusion Binder + 100 BU with complete genome access. Install
it on a Lure Polyp at L1 or higher. Supported treatments are rabbit,
cod, cow, pig, sheep and chicken. Each pulse costs 5 BU plus one matching feed:
carrot, raw cod, wheat, carrot, wheat or wheat seeds, respectively. Keep the same
8-block range and 40-tick pulse; one animal follows at a time. A disabled Nerve
input or full assigned pen refuses attraction. Existing player luring and attack
intent wins; tamed animals and players are excluded. Food is consumed as scent,
not as a simultaneous breeding or feeding action.

Restraining Tissue accepts one irreversible I053 treatment prepared in Chrysalis:
2 spider stock + cobweb + Binder + 100 BU gives 40-tick duration at the base 20%
reduction; 2 slime stock + slime ball + Binder + 100 BU gives 40% reduction for
20 ticks. Both require the matching complete genome; passive tissue has no counter
gate. Retain the 2 BU/target/20-tick cost. The selected mode targets either
hostiles or the six husbandry species, never players, pets or bosses. Overlapping
cells use the strongest funded effect, not summed strength or double charging.

### T2-20 — Structure Grower
- **Does:** Builds a selected small biological wall, room or repeated pattern from supplied parts. Useful for enclosing organs without hand-placing every decorative rib.
- **Input → output:** A player-marked template + actual blocks or declared prepared tissue grafts + biomass → construction at clear authorized positions, or mutation of eligible exposed support cells. This lets a mine grow flush light/access tissue without raised floor markers. Obstructions are reported, not consumed; grafting does not also award the replaced terrain's loot.
- **Create:** 1 Organ Bud + 1 crafting table + 1 amethyst shard + 2 Rib Frames.
- **Growth:** Choose placement speed or biomass economy. Skeletal Graft treatment supports larger spans; later electrical control adds material requests and multi-step plans. It never generates the template's blocks from biomass alone.

### T2-21 — Descending Rootstock
- **Does:** Plans and advances a finite descending staircase using attached cutting, holding, collection and construction organs. The core does not mine, place lights or process ore itself.
- **Input → output:** Approved section + supplied working organs → successive authorized cutting/access steps, ending at a prepared landing. The assembled result is a lit walkable passage with three clear blocks of headroom above each tread and actual materials retained.
- **Create:** Controller: 1 Organ Bud + 1 Survey Imprint + 2 Bone Plates. Plant at a Surveyed Tissue collar; attach a Reflex Knot, Fracture Jaw, Work Bed, Collection Cilia and Structure Grower with actual stair/light/tendon supplies. Grow supplied skin/ribs as its physical working root advances; no detached invisible cutting point.
- **Assembly:** T2-22, T2-34, T1-32, T1-33, T2-20, T0-13, T1-23, T1-34.
- **Growth:** Choose section-advance rate or controller biomass economy; cutting rate belongs to the jaw. Add a Grasping Root for intact eligible blocks, with treatment at a separate workshop. New collars authorize new finite sections. Lack of spoil space or access parts pauses before advancing beyond the last safe landing; dismantling recovers each organ's own history.

### T2-22 — Surveyed Tissue
- **Does:** Marks a mining boundary as part of the ground, without raised border blocks. A visible line and corner pores show the actual selected footprint.
- **Input → output:** Player corner/depth selections → a finite plan accepted by its mining organ; no resource output itself.
- **Create:** Apply 1 Survey Gel to exposed mature substrate; configure the resulting marked cells with the tool.
- **Growth:** A Synaptic Gel treatment adds a pause point; luminous graft marks shaft edges. Changing a plan shows its new extent before work resumes, including a one-block-wide strip option.

### T2-23 — Mineral Gizzard
- **Does:** Grinds supplied mineral material into concentrate. It neither extracts a world block nor performs the Work Bed's intact-block reaction. Its coarse residue is an output of grinding, not a second automatic recovery process.
- **Input → output:** Supported raw ore OR Mineral Fragments from a fractured treated block + biomass → source-specific concentrate and rock residue according to that input's recipe. Fragments have their own single recovery allowance, not a second raw-ore multiplier. Separate no-bonus granulation prepares ingots, diamonds and obsidian for fusion; Washing Kidney cleans those portions.
- **Create:** 1 Organ Bud + 1 iron pickaxe + 2 Bone Plates + 2 flint.
- **Growth:** Choose grinding speed or biomass economy. Add a Steam Muscle for bulk batches; a Faceted Chitin treatment permits harder ores. Concentrate carries the mineral amount recovered from its original input, not an unlimited multiplication opportunity.

### T2-24 — Washing Kidney
- **Does:** Washes crushed ore before smelting. The player chooses a simple dry furnace route or more recovery with a water supply and waste handling.
- **Input → output:** A four-unit Mineral Tailings parcel + water → one Washed Concentrate plus a three-unit residual parcel, at the Item Catalog cost. Wash water goes to a separate dirty-fluid output. Its starter filtration mode also cleans generator process water at the Item Catalog rate; that mode needs no ore or genome. Non-ore granulation washes one real mineral portion without a recovery multiplier.
- **Create:** 1 Organ Bud + 2 Membrane Sheets + 1 sand + 1 glass bottle.
- **Growth:** Choose washing speed or water economy. Elastic Gel treatment improves fine-particle capture; a later Ion Separator recovers selected tailings. Full waste output stops new batches.

### T2-25 — Feeding Trough
- **Does:** Supplies an assigned livestock pen from stored food, with a target population and a breeding reserve. The player chooses food production or animal growth as the pen's priority.
- **Input → output:** Actual breeding food + eligible animals → fed/breeding animals under the pen's population setting. Food is consumed normally.
- **Create:** 1 Seed Pouch + 1 cauldron + 1 Organ Bud.
- **Growth:** Choose feeding rate or lower biomass operating cost, not free breeding food. Species DNA unlocks better husbandry scheduling; no food is spent on a full pen.

### T2-26 — Milking Lobe
- **Does:** Attaches to a livestock service berth and collects supported animal products without sending items onto the ground.
- **Input → output:** A suitable mature animal + empty container + biomass → milk or another specifically supported nonlethal product. The animal must reach the berth and recover between harvests.
- **Create:** 1 Organ Bud + 1 bucket + 2 Membrane Sheets.
- **Growth:** Choose servicing speed or biomass economy. Sheep-genome mutation permits a shearing attachment using real shears; it does not make wool from a cow.

### T2-27 — Incubation Basket
- **Does:** Holds eggs and breeding stock through a controlled incubation cycle. Gives the player a way to populate pens without flooding the floor with loose eggs or chicks.
- **Input → output:** Fertile stock/eggs + warmth + feed → a hatchling in a reserved pen berth, or retained incubated stock if no berth is available.
- **Create:** 1 Organ Bud + 1 hay bale + 1 Membrane Sheet + 1 egg.
- **Growth:** Choose incubation speed or nutrient economy. Turtle or aquatic genomes unlock appropriate incubation recipes and habitat checks; no valid destination means no new entity is hatched.

### T2-28 — Fishing Polyp
- **Does:** Fishes a marked natural water area from the shore. It is an alternative to a controlled aquaculture installation, with less predictable output.
- **Input → output:** Bait + a fishing rod + biomass + valid water → allowed fishing catches in its inventory, with rod wear. Automated treasure recipes, if included, require their own open-water conditions.
- **Create:** 1 Organ Bud + 1 fishing rod + 1 kelp + 1 Membrane Sheet.
- **Growth:** Choose cast rate or biomass economy. Fish-genome mutation favors food catches; luck-oriented ingredients trade food rate for a different permitted catch table, not every rare item at once.

### T2-29 — Restorative Tissue
- **Does:** A safe recovery patch for an authorized wearer or husbandry berth. Useful near hazardous workshops, but not a free regeneration effect over the whole base.
- **Input → output:** Biomass + contact with an eligible injured target → gradual healing, consuming feed only when healing is delivered.
- **Create:** Mutate mature substrate with 1 Restorative Serum.
- **Growth:** Rabbit-genome graft favors recovery after movement; livestock genomes permit controlled pen care. It heals injuries; it neither produces samples nor awards genome coverage.

### T2-30 — Spine Sentry
- **Does:** A short-range ground-defense organ. It guards an approach with aimed physical spines rather than serving as another flying-target bloom.
- **Input → output:** Bones or grown Bone Plates + biomass → fired spines and damage to permitted visible targets.
- **Create:** 1 Organ Bud + 1 crossbow + 2 Bone Plates.
- **Growth:** Choose reload rate or biomass economy. Cactus mutation adds contact deterrence; skeleton DNA adds a piercing specialization. Shot material is not recovered as profitable automatic drops.

### T2-31 — Fusion Chrysalis
- **Does:** Fuses prepared signatures, carriers and stock into one target-specific graft before installation. It cannot replace a chamber's treatment space.
- **Input → output:** Named active ingredients + Fusion Binder + matching Genetic Stock and genome access where required → one typed organ, tissue or equipment graft.
- **Create:** 1 Activation Cyst + 1 Sequencing Lens + 2 Bone Plates; add Rib Frames and Membrane Windows around a clear two-block-high treatment cavity on a 3×3 mature bed.
- **Growth:** Choose batch speed or biomass economy. Precision attachment enables higher recipes; consumed donors and returned containers are reserved before processing. No complete genome is needed for host or mineral-only recipes.

### T2-32 — Grasping Root
- **Does:** Removes one explicitly targeted adjacent eligible block intact and presents it above an adjacent Work Bed. It stops while that bed is occupied. It does not survey for ores, break drops, treat the specimen or deliver ingots.
- **Input → output:** One real permitted block + biomass + an empty bed → that same block held at the bed, with the source position empty. No loot roll occurs during extraction. A hand-supplied block is a valid alternative.
- **Create:** 1 Organ Bud + 2 Bone Plates + 1 Contractile Fiber + 1 iron pickaxe.
- **Growth:** Choose extraction rate or biomass economy. Prepared cutting reinforcement admits harder explicitly supported blocks. A mining assembly can advance its physical mount along a finite plan; a free-standing root only reaches its facing block. Work Bed specimens and permitted building blocks are reusable targets; inventories, arbitrary block entities, protected blocks and unsupported modded states are refused.

### T2-33 — Reaction Polyp
- **Does:** Applies one prepared reagent recipe to one locked Work Bed target. The bed shows the changing specimen while other heads wait. A thermally lined head can instead dose one admitted exposed pit face under a Digestion Crucible's finite plan. It does not fracture, collect, grow cultures or prepare its own reagents.
- **Input → output:** Eligible target + named reagent + biomass and recipe water → a treated target plus retained spent reagent. Initial recipes: intact ore + Mineral Penetrant; ordinary log + Rooting Gel → one Living Wood block for construction, without a second wood drop; host rock + Leaching Nodule → Leached Rock. Later thermal service admits hot leaching recipes. Item Catalog owns these recipe states.
- **Create:** 1 Organ Bud + 1 glass bottle + 1 Filter Membrane + 1 Activation Cyst.
- **Growth:** Choose treatment rate or biomass economy. Add a fluid mouth, supported liner or pressure service for named recipes; those do not grant every treatment automatically. Several beds need actual separate heads or explicit sequential service; no radius-wide free treatment aura.

### T2-34 — Fracture Jaw
- **Does:** Breaks one presented specimen or one explicitly selected facing terrain block using its installed cutting grade. It is useful for mineral recovery, rock supply or bounded demolition without a mine controller.
- **Input → output:** Untreated eligible block + biomass → its permitted ordinary drops; treated ore → Mineral Fragments under that treatment's fixed recipe, never ordinary ore loot as well. Products stay in the Work Bed or a pre-reserved adjacent inventory. It has no ore search, sorting or smelting function.
- **Create:** 1 Organ Bud + 1 iron pickaxe + 2 flint + 2 Bone Plates.
- **Growth:** Choose break rate or biomass economy. Prepared reinforcement increases cutting grade. A thermal upgrade becomes a Boring Jaw and retains this jaw's counters. For a bed job, start requires the bed's declared treatment-complete state; direct breaking uses a separately selected mode. Occupied/protected targets, unsafe falling terrain and insufficient output room cause refusal.

### T2-35 — Specimen Archive
- **Does:** Stores grouped raw samples, separate from reconstructed knowledge and Genetic Stock.
- **Input → output:** Species/quality sample counts → exactly those counts available to docks, collectors and extractors.
- **Create:** 1 Item Capsule + 2 Filter Membranes + 1 Auric Myelin.
- **Growth:** No processing levels. The per-body capacity table in Items applies. Connect more bodies through actual routes, not an unbounded merged inventory. Full stores refuse new samples.

### T2-36 — Sample Collector
- **Does:** Routes sample loot from one explicitly linked owned killing organ into one adjacent archive.
- **Input → output:** That killer's qualifying death event → one accepted sample, with quality from its installed Dissector if present.
- **Create:** 1 Organ Bud + 1 Sample Pouch + 1 Synaptic Gel + 1 Filter Membrane.
- **Growth:** No creature searches or ground-loot collection. A Dissector service socket accepts one real weapon; the sampling rule is owned by the weapon, not duplicated here. Full output suppresses the additional sample, never ordinary loot.

## T3 — Thermal colony: a working Nether base and larger mines

The Nether grows Thermal Lining continuously. It is needed for hot-fluid service, high-temperature organ bodies and steam-driven attachments. Heat alone in another dimension does not replace the native growing bed.

### T3-01 — Thermal Substrate
- **Does:** A heat-tolerant living foundation that matures only in the Nether. Supports native thermal growth and protects its own tissue from ordinary local heat, not everything standing on it.
- **Input → output:** Starter substrate + local heat + biomass → mature thermal bed. No lava or ore is generated.
- **Create:** In the Nether, mutate mature Living Substrate with 1 Thermal Seed. An immature bed can be started with hand-carried biomass.
- **Growth:** Mature with continued local feeding. Removed cells retain their form but do not support Nether-native production outside the Nether; ordinary thermal-lined building parts remain usable elsewhere.

### T3-02 — Thermal Nursery
- **Does:** Grows Thermal Lining on a Nether-native bed. This is the continuing local supply for industrial organs, not a one-time dimension-unlock item.
- **Input → output:** Membrane Sheets + magma cream + biomass + controlled local heat → Thermal Lining; a locally grown Thermal Fruiting Body may replace the magma cream at 2 bodies per batch.
- **Create:** 1 Culture Bowl + 2 Bone Plates + 1 magma block; place on a 3×3 Thermal Substrate bed. Its first batches use a neighboring magma block, without requiring steam equipment.
- **Growth:** Choose growth speed or biomass economy. Add a hot-fluid jacket and condenser-fed water cooling for faster batches. Maintain its membrane and nutrient inputs from farms or imports.

### T3-03 — Lava Siphon
- **Does:** Withdraws actual lava from a marked local source and feeds a sealed hot-fluid route. Does not conjure replacement lava when the source is exhausted.
- **Input → output:** Reachable lava + biomass → stored lava in a lined Fluid Cyst; the extracted source volume is removed.
- **Create:** 1 Organ Bud + 1 bucket + 2 Thermal Linings + 1 Bone Plate.
- **Growth:** Choose extraction speed or biomass economy. Extend its physical intake downward to reach a receding lake. A low-level sensor lets the player stop the boiler before its local supply runs dry.

### T3-04 — Steam Heart
- **Does:** Boils water in a bone-braced pressure body. The starting heart is hand-fed; a larger jacket and automated supplies support continuous steam work.
- **Input → output:** Water + heat from consumed fuel or a configured lava process → pressurized steam and disclosed spent material. Heat and feedstock are not both returned intact.
- **Create:** 1 Bio-Furnace + 2 Thermal Linings + 2 Rib Frames; add a sealed Fluid Cyst and a Relief Chimney before pressurized operation.
- **Assembly:** T0-07, T1-08, T3-07, T0-11.
- **Growth:** Choose steam rate or fuel economy. Added jacket cells increase throughput and water inventory; they also increase the amount of steam that must be relieved after shutdown.

### T3-05 — Steam Vein
- **Does:** Carries steam through insulated substrate to muscles, processing jackets and generators. It remains distinct from ordinary liquid transport.
- **Input → output:** Steam at its inlet pressure → steam delivered with distance-dependent heat loss; condensate is collected at a low-point drain.
- **Create:** Mutate a Fluid Vein with 1 Thermal Lining + 1 Bone Plate.
- **Growth:** More lining reduces loss, not below zero. A Condenser connected at a low point drains pooled water; branches have explicit pressure priorities rather than free identical power at every outlet.

### T3-06 — Pressure Vesicle
- **Does:** Stores a finite steam reserve for burst operations. Its swelling membrane shows charge; it cannot be substituted for a cold liquid tank.
- **Input → output:** Pressurized steam → stored steam, then controlled discharge or condensate as it cools.
- **Create:** 1 Fluid Cyst + 2 Thermal Linings + 2 Bone Plates.
- **Growth:** Join reinforced Pressure Vesicles for capacity. A redstone-controlled valve sets a reserve for emergency mechanisms; high-pressure operation requires the later Pressure Regulator.

### T3-07 — Relief Chimney
- **Does:** A safe place to release surplus steam away from walkways. Its vent needs clear space; putting a ceiling directly over it blocks relief.
- **Input → output:** Surplus steam → dissipated heat and vapor, sacrificing useful work. It is not a biomass disposal port.
- **Create:** 2 Thermal Linings + 2 Rib Frames + 1 Filter Valve.
- **Growth:** Taller segments increase safe discharge capacity. A Condenser on the relief branch recovers some water at the cost of footprint and a lower relief rate.

### T3-08 — Condenser
- **Does:** Returns used steam to water for a workshop loop. Cooling placement matters: a large exposed surface and a compact actively cooled body are different builds.
- **Input → output:** Spent steam + available cooling → recovered water and released heat; recovery is below the original steam input.
- **Create:** 1 Organ Bud + 2 copper ingots + 2 Membrane Windows + 1 Thermal Lining.
- **Growth:** Choose condensation speed or recovery fraction, capped below a lossless loop. Add fin-like Rib Frames for passive cooling or a water jacket for denser installations.

### T3-09 — Thermal Mantle
- **Does:** Encloses an existing Bio-Furnace in a larger heat-safe body on mature Nether-native substrate. Unlocks tempered biological construction materials while retaining the furnace's counters and chosen traits; an ordinary unmantled furnace still works elsewhere.
- **Input → output:** 1 Bone Plate + 1 Thermal Lining + 100 BU + heat → 1 Tempered Bone Plate. Iron, copper or gold dust + the listed steam/biomass → corresponding ingots; release concentrate into accounted dust first. Item Catalog owns these yields; thermal processing does not require an additional raw iron reinforcement recipe.
- **Create:** Surround the furnace with an eight-block 3×3 Thermal Substrate ring, four lined corner Rib Frames and four Thermal Linings on its chamber faces. Leave its service and output faces accessible.
- **Assembly:** T0-07, T3-01, T0-11.
- **Growth:** A Steam Muscle adds batch pressure; additional lungs trade space for rate. Tempered plates reinforce large mining heads and electrical organs. Ordinary ingots do not become extra metal merely by passing through the mantle.

### T3-10 — Steam Muscle
- **Does:** A shared powered attachment for gizzards, large furnace bodies and moving mining assemblies. Visible contracting tissue explains the host's faster or heavier work.
- **Input → output:** Steam + a host's work request → mechanical work and spent steam for a condenser.
- **Create:** 1 Organ Bud + 2 Thermal Linings + 2 iron ingots + 1 slime ball.
- **Growth:** Choose work rate or steam economy. Mounting another muscle enables a larger valid host assembly; disconnected muscles cannot smelt or mine by themselves.

### T3-11 — Descending Cradle
- **Does:** Controls a supported platform's descent through a finite real shaft. Attached jaws cut; a separate grower installs access; held products leave through collection/logistics. The platform is a mount and safe movement controller, not an ore processor.
- **Input → output:** Clear authorized next slice + steam + biomass + completed access/collection acknowledgments → one downward movement of the specified platform and its bounded installed equipment. A rider may travel with it; the preserved shaft remains climbable after shutdown.
- **Create:** Core: 1 Organ Bud + 1 Survey Imprint + 2 Thermal Linings + 2 Bone Plates. Build a 3×3 Rib Frame deck with two Steam Muscles, a Boring Jaw below it, Work Bed, Collection Cilia and Structure Grower; supply a side Climbing Tendon and Lumen Tissue route.
- **Assembly:** T3-10, T3-12, T0-11, T1-32, T1-33, T2-20, T2-22, T1-23, T0-13.
- **Growth:** Choose movement rate or steam economy; cutter mutations govern cutting. Expand the supported head/deck within declared limits, or carry a Grasping Root for intact samples. It refuses unsafe passenger movement and unsupported relocation of arbitrary world blocks. Moving payload size, collision checks and update rates require specific performance tests before implementation.

### T3-12 — Boring Jaw
- **Does:** A reinforced Fracture Jaw with a supported multi-tooth cutting face for a cradle, fixed gallery or large specimen bed. It executes bounded individual cuts; it does not scan, advance the installation or carry a hidden inventory.
- **Input → output:** Host steam/work + one admitted target at its cutting face → that block's allowed products into reserved beds/inventories. Intact extraction still needs a Grasping Root; chemical recovery still needs treatment.
- **Create:** Upgrade a Fracture Jaw with 2 Tempered Bone Plates + 1 Thermal Lining + 1 Faceted Chitin; mount on rib-supported head segments. Its steam drive is a separate Steam Muscle.
- **Growth:** Choose cutting rate or steam economy using the retained jaw history. More teeth permit a wider planned face but share the assembly/server work allowance and need extra power/output capacity. Sorting and disposal remain downstream jobs.

### T3-13 — Digestion Crucible
- **Does:** A sealed finite-pit assembly: its controller assigns host-rock treatment, a lined Reaction Polyp doses the admitted face, and fluid outlets drain spent medium. It leaves ores for a player or separately installed recovery heads, rather than granting mined ore as a reaction byproduct.
- **Input → output:** Marked real host rock + Bioactive Leaching Charge + biomass → accounted host residue, exposed untouched ores and Spent Leach Cake. The finite internal medium is not a world-placeable fluid.
- **Create:** Controller: 1 Organ Bud + 1 Survey Imprint + 2 Thermal Linings + 2 Bone Plates. Enclose a Surveyed Tissue footprint in thermally lined Living Skin; install a lined Reaction Polyp, Fluid Vein and separate Fluid Cyst for spent medium. A Leaching Gland supplies charges from outside; provide luminous access tendons before work.
- **Assembly:** T2-22, T2-33, T0-10, T1-02, T1-08, T1-23, T0-13.
- **Growth:** Controller levels improve section-handling speed or feed economy; the polyp owns reagent economy. Additional lined cells enlarge the finite authorized pit, not reaction reach for free. Drained landings precede opening; full drains stop further dosing. Products are host residue, untouched ores and spent treatment, never both dissolved rock drops and full residue.

### T3-14 — Heat-Exchange Gill
- **Does:** Transfers heat between a hot process and a cooler fluid without mixing their contents. Makes a compact heat-reuse layout possible.
- **Input → output:** Hot stream + cold stream → cooler hot stream and warmer cold stream; no new heat is created.
- **Create:** 1 Organ Bud + 2 copper ingots + 2 Thermal Linings + 1 Membrane Sheet.
- **Growth:** Choose exchange rate or reduced transfer loss. Add gill panels for contact area. It can preheat boiler water from a kiln's waste heat but cannot power a closed loop indefinitely.

### T3-15 — Distillation Crown
- **Does:** Concentrates brewed liquids and separates recoverable ingredients from selected mixtures. Its outputs support potion infusion and advanced culture feed.
- **Input → output:** A supported potion mixture + steam + empty containers → concentrated doses and separated water/residue according to that recipe.
- **Create:** 1 Brewing Gland + 2 glass bottles + 2 Thermal Linings + 1 Heat-Exchange Gill.
- **Growth:** Choose distillation speed or steam economy. Add Condenser stages for higher recovery. Concentration reduces volume; it does not duplicate the original number of effect doses.

### T3-16 — Launch Bellows
- **Does:** A short-range vertical launcher for prepared landings, mine entrances or glider takeoff. It is not a free-flight field or a revived acid-filled lift.
- **Input → output:** A charged steam reserve + an occupant's deliberate activation → one directed launch, then recharge.
- **Create:** 1 Steam Muscle + 2 Rib Frames + 1 slime block; install on a clearly marked launch pad.
- **Initial operation:** Deliberate activation costs 250 mB steam, sets upward velocity to at most 1.0 block/tick and starts a 40-tick cooldown. Refuse if the next eight overhead cells are obstructed; one occupant per activation. These are initial tuning values, not measured flight-range guarantees.
- **Growth:** Choose recharge speed or steam economy. Contractile Fiber treatment softens takeoff; the destination still needs a safe landing surface. Covering the launch path prevents activation.

### T3-17 — Thermal Root
- **Does:** Supplies a fixed heat condition to one adjacent thermal cultivation/nursery body from actual Nether terrain.
- **Input → output:** Loaded Nether position rooted on a magma block → local heat condition; no lava, items, electricity or consumed magma.
- **Create:** 1 Organ Bud + 1 magma block + 2 Bone Plates; mature Thermal Substrate support.
- **Growth:** Passive heat interface, no processing levels. One supported host face; its operation consumes its own biomass. No heat flood-fill or heating an entire chunk.

### T3-18 — Thermal Cultivation Tissue
- **Does:** Mutates mature Thermal Substrate into a Nether fungus bed producing Thermal Fruiting Bodies.
- **Input → output:** Reserved crimson/warped fungus planting + water + biomass + Thermal Root heat → the finite native harvest specified in Items.
- **Create:** Apply 1 Rooting Gel + 1 crimson or warped fungus to exposed mature Thermal Substrate. Keep the fungus as the planted parent; the graft consumes Rooting Gel.
- **Growth:** Separate Harvest Corolla and Collection Cilia automate collection; the basal planting remains. Bone ribs support a 3×3 bed. No mob farm, End product or native genome is needed to start.

## T4 — Electrical precision: better recovery and controlled automation

Bioelectric organs make exact sampling, coordinated production and larger excavation practical. Electricity supplements biomass and physical materials; it does not replace genomes or native beds.

### T4-01 — Electrocyte Stack
- **Does:** Produces electricity through a living stack of charged membranes. A slow metabolic build and a steam-assisted build use the same core.
- **Input → output:** Biomass + water and optional steam assistance → electrical power, spent water and heat. Item Catalog's native supply recipes define conversion costs; no reverse electricity-to-biomass recipe.
- **Create:** 1 Organ Bud + 2 copper ingots + 2 Membrane Sheets + 2 Bone Plates + 1 redstone block; its starter metabolic form is available with T1 preparation and requires no electricity. Tempered plates and Thermal Lining unlock industrial conversion on the same core.
- **Growth:** Choose electrical output or biomass economy. Add membrane cells for capacity; Steam Muscles improve sustained output while adding steam demand. Physical cooling limits the largest stack.

### T4-02 — Conductive Tissue
- **Does:** Carries electrical power through substrate, separately from control signals. Branches draw from one actual supply rather than duplicating it.
- **Input → output:** Electrical input → delivered electrical power with transfer loss.
- **Create:** Mutate mature substrate with 1 Conductive Myelin.
- **Growth:** Gold graft lowers loss; Thermal Lining tolerates greater current. A Septum Crossing can separate two electrical circuits without shorting them together.

### T4-03 — Charge Sac
- **Does:** Stores electrical energy for burst operations and orderly shutdown. Visible luminous bands show charge independently of biomass fill.
- **Input → output:** Electrical charging → finite stored energy → discharge to consumers, with storage/conversion loss.
- **Create:** 1 Organ Bud + 2 copper ingots + 1 redstone block + 2 Membrane Sheets.
- **Growth:** Join Charge Sac cells for capacity. Gold mutation favors delivery rate; slime mutation favors retention. A reserve rule keeps a shutdown controller powered when the main line goes empty.

### T4-04 — Electrical Exchange Organ
- **Does:** Connects the colony's power circuit to compatible external energy equipment. Imports or exports electricity; it does not pretend that foreign energy is biomass.
- **Input → output:** Supported external energy ↔ colony electrical power, with declared conversion loss and direction settings.
- **Create:** 1 Electrocyte Stack + 2 copper ingots + 1 comparator.
- **Growth:** Choose throughput or reduced conversion loss. Opposite-direction exchanges cannot make a positive-energy loop. Foreign power cannot mature a Nether bed or supply missing genetic material.

### T4-05 — Ion Separator
- **Does:** Precisely separates selected minerals or genetic samples. Separate sealed recipe cartridges distinguish ore processing from specimen processing.
- **Input → output:** Washed concentrate, tailings or prepared genetic suspension + electricity + water → recipe-accounted mineral fractions or source-specific stock and waste.
- **Create:** 1 Washing Kidney + 1 Sequencing Lens + 2 copper ingots + 1 gold ingot.
- **Growth:** Choose separation speed or electrical economy. Gold mutation unlocks finer separation. Retreatment recovers only remaining material; repeatedly cycling clean output cannot create more mineral or genome coverage.

### T4-07 — Precision Sequencer
- **Does:** An extractor attachment for resolving difficult genomes from fewer valuable specimens. It makes saving a rare sample for a better laboratory worthwhile.
- **Input → output:** Host's rare specimen + electricity + completed related research where specified → high-quality genome fragments; actual sample material is still consumed.
- **Create:** 1 Sequencing Lens + 1 Ion Separator + 1 amethyst cluster + 1 gold ingot.
- **Growth:** Choose sequencing speed or electricity economy. Additional lenses let one laboratory handle several source families. It reveals expected coverage gain before consuming a scarce species-specific death sample.

### T4-08 — Potion Infuser
- **Does:** A mutation-chamber attachment that installs a limited temporary chemical load into eligible bio equipment, or applies a supported potion-based organ treatment.
- **Input → output:** Real potion doses + eligible target + biomass → stored charges of that specific effect; used bottles return empty.
- **Create:** 1 Distillation Crown + 1 Membrane Sheet + 1 gold ingot; attach to a Mutation Chamber bay.
- **Growth:** Choose infusion speed or biomass economy. A membrane mutation adds charge capacity, competing with another equipment property. It does not permanently grant every potion effect or remove ordinary potion use.

### T4-09 — Trait Regulator
- **Does:** Coordinates conflicting mutations in a chamber. Lets the player choose a stronger specialist combination, not erase its costs.
- **Input → output:** A target's selected trait plan + compatible Genetic Stock + electricity → a regulated mutation with shown benefits, load and exclusions.
- **Create:** 1 Archive Lobe + 1 comparator + 1 gold ingot + 1 Precision Sequencer.
- **Growth:** Choose processing speed or electricity economy. End-grown parts later permit more complex combinations. Nether resistance, ocean pressure adaptation and burrowing endurance still compete for the target's finite capacity.

### T4-10 — Scheduler Ganglion
- **Does:** Runs a bounded recipe sequence across named organs, using their ready/result-held acknowledgments. Draft limit: eight steps and one active sequence; parallel lines use actual additional controllers, beds and supplies. No general scripting or recursive job expansion.
- **Input → output:** Player-set conditions + electricity → reserved, acknowledged work steps. Optional delay/budget steps stagger starts; they never substitute for treatment completion. Power loss holds uncommitted work; committed jobs remain owned and visible.
- **Create:** 1 Synaptic Console + 1 clock + 1 comparator + 1 Organ Bud.
- **Growth:** Survey Gel treatment improves diagnostics and allows the declared step limit, not unlimited queues. Earlier Reflex Knots, selectors and vanilla circuits remain valid. Failed steps display their dependency; a restarted controller reconciles held work instead of replaying every start command.

### T4-11 — Display Membrane
- **Does:** A readable wall display for one machine or configured district: stock, flow, reserve, pressure or an alarm. Adjacent panels form a larger display.
- **Input → output:** Selected console/sensor values → labeled visual readout; no material output.
- **Create:** 1 Membrane Window + 1 glow ink sac + 1 redstone dust.
- **Growth:** Add panels for more rows or a route diagram. Dyes set the skin color, while warning shapes and text remain distinguishable without relying on color alone.

### T4-12 — Request Cortex
- **Does:** Requests a finished item from a connected workshop, arranging recipes through the existing veins and organs. Storage access and crafting are separate functions.
- **Input → output:** A requested item/count + available stock + registered organ recipes + power → reserved work orders and delivered products, or a list of missing ingredients.
- **Create:** 1 Scheduler Ganglion + 1 crafting table + 1 amethyst cluster + 1 Item Capsule.
- **Growth:** Add Archive Lobes for more recipe/order slots and Item Capsules for pending inputs. A furnace order still needs fuel, output space and the right physical furnace upgrades; the cortex cannot simulate the machine away.

### T4-13 — Strata Maw
- **Does:** A Rootstock controller specialized for a **one-block-wide long strip** descending from a surface line. Starts at a Climbing Tendon; orders separate cutting, clearing and access work. It does not include an ore separator just because precision tier is reached.
- **Input → output:** Finite surveyed strip + supplied working organs → successive accepted cuts and maintained lit resting/access points. Actual cutter products go through a Work Bed and collection line; treatment/refining is a separate choice.
- **Create:** Upgrade the Rootstock controller with 2 Thermal Linings + 1 Precision Probe + 1 Survey Imprint, retaining its section history. Install a Boring Jaw/Steam Muscle, Work Bed, Collection Cilia and Structure Grower beside a Surveyed Tissue line, spoil store and starting luminous tendon.
- **Assembly:** T3-12, T3-10, T1-32, T1-33, T2-20, T2-22, T1-23, T0-13.
- **Growth:** Choose section-handling speed or controller energy economy; jaw throughput is independent. Existing upgraded access/collection parts can be reused. Another surface segment needs explicit approval; end-of-plan never becomes an unlimited chunk quarry.

### T4-14 — Spoil Sorter
- **Does:** A named sorting layout, **not another processing core**. Filtered mouths separate ore, useful rock and selected surplus; capsules buffer each destination. Basic sorting is available at T1; T4 teaches a high-throughput mine layout.
- **Input → output:** Held mixed items → unchanged items through permitted outlets, or retained blocked contents. Sorting costs only its actual transport/control services; no fictitious recipe XP.
- **Create:** Place one input Item Capsule, at least two Output Mouths with Filter Valves and separate receiving capsules. The worked shared-trunk layout includes one Selector Ganglion; independent outlets can omit it. Electrical monitoring is optional.
- **Assembly:** T1-09, T1-11, T1-12.
- **Growth:** Add parallel output lanes or larger packets; reserve building stone before allowing explicit disposal. No aggregate sorter level exists. Taking it apart returns its component blocks, not a second boxed all-in-one sorter.

### T4-15 — Recovery Sump
- **Does:** Collects spilled biomass or escaped process liquid inside a player-built catchment. Gives hazardous-fluid installations a recoverable low point.
- **Input → output:** Collected finite liquid + pumping power → a recovery tank; mixed contamination is identified instead of becoming clean biomass automatically.
- **Create:** 1 Fluid Cyst + 1 Vascular Junction + 2 Thermal Linings.
- **Growth:** Choose pumping speed or electricity economy. Add lined floor channels to extend the catchment. It cannot drain a whole biome or create liquid from an empty basin.

### T4-16 — Neutralization Gland
- **Does:** Disposes of unwanted aggressive biomass and selected chemical waste when storing more is not worthwhile. A permanent drain costs ingredients.
- **Input → output:** Waste biomass/fluid + water + recipe-specific neutralizer such as bone meal → inert residue and dirty water, with less recoverable biological value.
- **Create:** 1 Digestive Sac + 1 Washing Kidney + 2 Thermal Linings.
- **Growth:** Choose disposal speed or neutralizer economy. Add a Condenser for water recovery. Residue cannot be redigested into as much biomass as the process destroyed.

### T4-17 — Pressure Regulator
- **Does:** Keeps a steam/process line between chosen pressure limits, reserves room for shutdown output and operates a specific relief valve.
- **Input → output:** Pressure/fullness measurements + electricity → throttled inlet, held reserve and controlled relief commands.
- **Create:** 1 Sensor Polyp + 1 Overflow Valve + 1 copper ingot + 1 comparator.
- **Growth:** Survey Gel treatment coordinates several connected vessels. A nearby Charge Sac keeps the shutdown path alive during power loss; the regulator does not make an undersized relief route adequate.

### T4-18 — Folded Shelter
- **Does:** A transportable biological building kit for a dangerous outpost. It unfolds into a small ribbed shelter with an entrance, service face and floor, using material packed into it beforehand.
- **Input → output:** Stored skin/frames + biomass + a clear selected footprint → those same parts assembled as a shelter. It provides ordinary cover, not dragon-proof invulnerability.
- **Create:** 1 Structure Grower + 2 Membrane Sheets + 1 piston; pack the desired building parts into its UI.
- **Growth:** Choose deployment speed or biomass economy. Add compartments for a Healing Dock and spare feed. A larger shelter consumes more packed parts and requires more clear ground; no End material is needed for the first End expedition.

### T4-19 — Cold Lobe
- **Does:** Refrigerates an attached specimen, food or culture store. Supplies a cold condition for recipes that require it; raw DNA samples have no passive spoilage.
- **Input → output:** Electricity + cooling fluid → a cold host compartment and warmed fluid. Stopping cooling pauses cold-dependent work and retains its contents; it starts no inventory decay timer.
- **Create:** 1 Heat-Exchange Gill + 1 packed ice + 1 Charge Sac.
- **Growth:** Choose cooling rate or electrical economy. Add insulated Membrane Windows for a larger cold room. Cooling produces no new genome information or additional stock.

### T4-20 — Mnemonic Vessel
- **Does:** Extracts and stores real Minecraft XP from a consenting player, separate from organ counters, armor learning and DNA. Supplies counter-reduction rituals or returns XP to the player for vanilla enchanting.
- **Input → output:** UI-controlled deposit/withdrawal transfers raw XP points, not levels, at up to 20 points/s while the player is within 4 blocks with the UI open. One vessel serves one player transfer at a time. Full storage or insufficient player XP stops transfer; closing the UI stops extraction. A connected Mutation Chamber can reserve the ritual's stated XP payment directly.
- **Create:** 1 Organ Bud + 1 enchanting table + 1 amethyst shard + 2 Membrane Sheets.
- **Growth:** Initial capacity 10,000 XP per vessel; join at most four cells for 40,000 XP, with each cell retaining its own contents. Survey Gel doubles player transfer rate to 40 XP/s, not capacity or yield. Normal dismantling preserves stored XP in that cell. No loose-orb output, ambient entity search, counter-to-XP conversion or free XP generation. Player and ritual withdrawals reserve against the same available balance.

## T5 — End colony: spatial materials and a defended settlement

Starter structures use T4 products plus local End materials. Mature spatial tissue must be grown and conditioned here, giving the End base continuing work after the dragon fight.

### T5-01 — Anchored Substrate
- **Does:** The End-native foundation for spatial growth. Rooted veins visibly hold separate fragments of the tissue together above the pale ground.
- **Input → output:** Living Substrate + End-local conditioning + biomass → mature anchored bed.
- **Create:** In the End, mutate mature substrate with 1 Anchor Seed.
- **Growth:** Continued native growth permits spatial nurseries. Moving it to another dimension preserves the block but suspends End-native production; a relocated bed is not a portable End workshop.

### T5-02 — Spatial Nursery
- **Does:** Grows Spatial Membranes for gates, precision organs and advanced chamber bays. Its supply chain needs both Nether lining and End-local growth.
- **Input → output:** Membrane Sheets + Thermal Lining + chorus fruit + biomass + electricity → Spatial Membranes, only on an End-native bed.
- **Create:** 1 Genetic Culture Vat + 2 Thermal Linings + 1 ender pearl; place on a 3×3 Anchored Substrate bed with an Anchor Root at each corner.
- **Growth:** Choose growth speed or electrical economy. A Chorus Resonator tunes growth; extra nursery bays increase production with matching feed demand. Its recipe does not require its own membrane output to start.

### T5-03 — Anchor Root
- **Does:** A structural restraint for spatial organs and End construction. Defines the physical limits within which a connected spatial process is allowed to operate.
- **Input → output:** Biomass while actively stabilizing a process → a stable supported chamber; no passive power or resources.
- **Create:** 1 Rib Frame + 1 end stone + 1 ender pearl + 1 Thermal Lining.
- **Growth:** Add root segments down into a solid support or outward along a platform. Spatial Membrane graft permits stronger chamber loads; it does not make every connected block indestructible.

### T5-04 — Void Tether
- **Does:** A local fall-arrest station for authorized expedition members. A linked wearer falling from its marked work area can be pulled to its prepared platform while the station has charge.
- **Input → output:** Stored electrical power + biomass + one rescue → a safe return to that station; insufficient charge means no promised rescue.
- **Create:** 1 Launch Bellows + 2 ender pearls + 1 Spatial Membrane; anchor it beside a clear landing.
- **Growth:** Choose recharge speed or biomass economy. More Anchor Roots enlarge the declared work area. It cannot retrieve a player anywhere in the dimension or rescue through a blocked arrival space.

### T5-05 — Chorus Resonator
- **Does:** Produces a tuned local pulse that changes which spatial growth recipe a nursery can run. The player lays out tuned rooms rather than stacking every pulse on one bed.
- **Input → output:** Electricity + a chosen chorus tuning → a local conditioning pulse consumed as a process condition by nearby assigned organs.
- **Create:** 1 Organ Bud + 2 chorus fruit + 1 amethyst cluster + 1 end rod.
- **Growth:** Choose pulse rate or electrical economy. Add resonant ribs to cover a larger room. Incompatible tunings require separate rooms or scheduled batches, not a universal combined frequency.

### T5-06 — Spatial Conditioner
- **Does:** Prepares grown membranes for a specific purpose: passenger passage, cargo passage or precision regulation. Choice of conditioning commits the batch to that use until reprocessed.
- **Input → output:** Spatial Membranes + electricity + a selected Chorus Resonator pulse → purpose-conditioned membranes; performed in the End.
- **Create:** 1 Mutation Chamber core + 2 ender pearls + 2 Thermal Linings; build a 3×3 anchored chamber with Membrane Windows and a resonator-facing service bay.
- **Growth:** Choose batch speed or electrical economy. Add isolated bays to alternate products without retuning the whole workshop. Reconditioning costs time and feed rather than yielding a second membrane.

### T5-07 — Levitation Chamber
- **Does:** A contactless processing bay for delicate cultures and precision grafts. It keeps the sample suspended while tools work around it, not while the player flies freely.
- **Input → output:** Host's target + electricity + levitation culture from a researched shulker source → reduced mechanical damage during an eligible precision recipe.
- **Create:** 1 Trait Regulator + 2 shulker shells + 2 Spatial Membranes; add above a chamber's treatment space.
- **Growth:** Choose processing speed or electrical economy. Taller chamber walls accept larger organ cores. It improves sample handling; it does not increase the target's unlimited mutation capacity.

### T5-08 — Chorus Orchard Tissue
- **Does:** Supports an End garden producing reliable nursery feed and construction stock. Can maintain an accessible low canopy rather than a tangled field of tall plants.
- **Input → output:** Chorus planting stock + nutrients + End-local conditions → chorus fruit and retained replanting stock.
- **Create:** Mutate mature Anchored Substrate with 1 Chorus Orchard Graft.
- **Growth:** Grafting Bench variants favor compact fruit production or larger structural growth. Add a Harvest Corolla for automated collection; its output remains ordinary chorus-derived material, not arbitrary End loot.

### T5-09 — Targeting Eye
- **Does:** A shared sighting attachment for a small configured defense group. Warns of selected approaching flying targets and assigns fire without every weapon picking a different target.
- **Input → output:** Power + visible target observations → aim assignments for linked blooms or sentries.
- **Create:** 1 Sensor Polyp + 1 ender eye + 1 Spatial Membrane + 1 amethyst shard.
- **Growth:** Choose scan frequency or electrical economy. Enderman-genome mutation improves tracking after short displacements; weapons still require their own range, clear shot, ammunition and fuel.

### T5-10 — Selective Membrane
- **Does:** A doorway-sized living barrier for a controlled workroom. Opens for configured authorized wearers; remains physical for others while powered.
- **Input → output:** Access decision + electricity → an open passage for an admitted entrant or a closed barrier. No damage by default.
- **Create:** Mutate a Sphincter Door with 1 Spatial Membrane + 1 ender pearl.
- **Growth:** Add a Nerve Tissue emergency-open control and a local reserve. It does not trap someone in a closing membrane or grant an entire base immunity to boss attacks.
- **Initial operation:** Keep the closed barrier powered for20BE/s, with a200BE internal reserve. Owner/team authorization controls passage, not armor completeness. Authorized contact or emergency-open opens for40ticks; occupied doorway prevents closure. Loss of power opens it without damage.

### T5-11 — Catching Membrane
- **Does:** A physical safety net beneath a work platform, catching falling items before they disappear into the void. It must actually occupy the catch area.
- **Input → output:** Items falling onto the membrane → retained items in an attached Item Capsule; a full capsule leaves them safely held within the net's finite storage.
- **Create:** 1 Spatial Membrane + 2 string + 1 Rib Frame → 4 net sections.
- **Growth:** Add supported sections for a wider net. Elastic Gel treatment cushions ordinary falls but is not a remote player-rescue ability. Breaking it preserves its held inventory rather than duplicating the items above.

### T5-12 — Phase Isolator
- **Does:** Separates incompatible spatial workshop pulses without requiring distant bases. Provides a real reason to enclose nursery and precision rooms differently.
- **Input → output:** Local resonator interference + a powered boundary → isolated chamber tuning; no new pulse energy.
- **Create:** 1 Spatial Membrane + 1 Membrane Window + 1 obsidian → 2 boundary panels.
- **Growth:** Complete walls, floor and ceiling provide stronger isolation than scattered panels. Doorways need compatible hatches. Adding isolation enables adjacent different processes, not increased output from one unchanged recipe.

## T6 — Distributed colony: travel, freight and shared production

Connect established workshops rather than replacing them. Transit consumes locally stored reserves; remote freight waits when its receiving workshop cannot accept delivery.

### T6-01 — Transit Maw
- **Does:** A living entrance connecting two constructed passenger stations, across continents or dimensions. Separate mouth size and landing space determine whether a route accepts only players or also mounts.
- **Input → output:** Authorized passenger + biomass at departure + a ready Arrival Chamber → that passenger at the linked station, with inventory unchanged.
- **Create:** Core: 1 Vascular Junction + 2 passenger-conditioned Spatial Membranes + 2 ender pearls. Build a ribbed mouth with a two-block-clear opening and a linked Arrival Chamber at each end.
- **Growth:** Choose transit recharge rate or biomass economy. Add a wider throat and landing to admit mounts; longer/cross-dimensional trips cost more. A destination that cannot be made ready refuses departure rather than dropping the traveler into rock or the void.
- **Initial operation:** Player only; reject mounted/passenger entities. Departure costs100BU+10BU×ceil(distance/256) in one dimension, or500BU across dimensions. Arrival reserves50BU+1000BE per traveler;20-tick cooldown per station. Distance is Euclidean between endpoints, computed without chunk lookup.

### T6-02 — Arrival Chamber
- **Does:** Defines a protected, inspectable landing volume for a Transit Maw. Keeps the arrival reserve separate from the workshop's production budget.
- **Input → output:** Arrival reservation + local biomass/electrical reserve → a clear landing and admission acknowledgment.
- **Create:** 1 Void Tether + 1 Biomass Bladder + 2 passenger-conditioned Spatial Membranes; assemble a 3×3 floor with at least three clear blocks above it.
- **Growth:** Add separate berths for higher traffic or mounts. A Healing Dock can serve the exit, but a full healing queue must not block arrivals. Departure is refused when every berth is occupied.

### T6-03 — Freight Gullet
- **Does:** Transfers reserved cargo between two built freight endpoints. Passenger throughput is not an unlimited bulk-storage connection.
- **Input → output:** A committed item batch or sealed fluid batch + biomass/electricity → the same batch in the receiver's reserved buffer.
- **Create:** 1 Vascular Junction + 2 cargo-conditioned Spatial Membranes + 1 Item Capsule + 1 Fluid Cyst.
- **Growth:** Choose dispatch speed or energy economy. Additional Cargo Locks increase batch size. Both endpoints must be available; a sleeping destination leaves the cargo accounted for locally rather than continuing invisible remote production.
- **Initial operation:** One bounded cargo-lock batch costs25BU+1000BE at source and10BU+500BE at destination, doubled for a cross-dimensional link;40-tick dispatch cadence/link. Reserve fuel separately from payload, including BE cargo. Pay once at durable pack; retries/acknowledgments never rebill or refund committed cargo. A return is a separately paid transfer.

### T6-04 — Cargo Lock
- **Does:** A physical staging buffer attached to a Freight Gullet. Separates “requested,” “packed” and “received” cargo so another machine cannot consume a half-prepared delivery.
- **Input → output:** Reserved items/fluid containers → a sealed dispatch batch, or the unchanged contents on cancellation.
- **Create:** 1 Item Capsule + 1 Sphincter Door + 1 Sensor Polyp + 1 cargo-conditioned Spatial Membrane.
- **Growth:** Add compartments for several independent destinations. A fluid compartment needs a compatible lined cyst; incompatible cargo does not share one invisible universal tank.

### T6-05 — Storage Cortex
- **Does:** Presents connected Item Capsules and tanks as one searchable local store, while the physical cells still hold the contents.
- **Input → output:** Player queries, deposits and withdrawals + electricity → indexed access and routed items through connected mouths/veins.
- **Create:** 1 Synaptic Console + 1 Archive Lobe + 1 precision-conditioned Spatial Membrane. A Request Cortex is a separate optional client; storage indexing alone does not require automated recipe planning.
- **Growth:** Attach Archive Lobes for more connected storage groups. Remote stock is shown separately with availability and freight cost; it cannot be withdrawn instantly from an unloaded dimension.

### T6-06 — Workshop Interface
- **Does:** Lets a compatible external storage or crafting system request one of the colony's declared processes, and receive the result through a physical buffer.
- **Input → output:** Recipe request + actual ingredient delivery → a queued workshop job and completed output, or a clear refusal.
- **Create:** 1 Request Cortex + 1 Intake Mouth + 1 Output Mouth + 1 comparator.
- **Growth:** Add buffer compartments for concurrent recipes. A request cannot bypass a furnace's maturity, a missing chamber attachment or a dimension-specific growing bed. Ordinary hoppers still work for simpler integration.

### T6-07 — Relay Ganglion
- **Does:** Links named district consoles, showing selected remote state and sending explicit orders through a constructed connection.
- **Input → output:** Authorized status requests/commands + power → current district reports or acknowledged actions. Unknown/offline values appear unavailable, not falsely healthy.
- **Create:** 1 Scheduler Ganglion + 1 ender pearl + 1 precision-conditioned Spatial Membrane.
- **Growth:** Add a linked Relay Ganglion for each additional district. Each district retains its own local rules and reserves, so a broken backbone does not make every farm wait for one central brain.

### T6-08 — Foundation Cyst
- **Does:** Deploys a pre-fed, finite patch of Living Substrate at an outpost. The player marks its boundary before activation; it cannot spread indefinitely.
- **Input → output:** Packed substrate/growth stock + a fixed biomass load → converted eligible exposed ground within the marked area, with unused stock retained.
- **Create:** 1 Structure Grower + 1 Biomass Bladder + 2 Membrane Sheets + 1 cargo-conditioned Spatial Membrane.
- **Growth:** Choose placement speed or biomass economy. Add compartments for a larger deployment, or include a Folded Shelter. It does not instantly produce mature native beds in the wrong dimension.

### T6-09 — Service Pedestal
- **Does:** A named shared service-berth layout, **not another all-purpose block**. Its selected attached service handles fuel, healing or authorized cargo transfer. These individual services are available earlier; T6 introduces coordinated station layouts.
- **Input → output:** One admitted wearer/set → acknowledged service requests in a chosen order. Each service pays its own costs and operates only on permitted targets/slots; stepping on a berth never authorizes emptying the player's whole inventory.
- **Create:** Frame a Fuel Papilla substrate floor cell with Living Skin and Rib Frames. The worked fuel-and-healing layout adds a Healing Dock, Item Capsule, Filter Valve and Reflex Knot. Add mouths or a Scheduler Ganglion for explicitly approved cargo/tool service.
- **Assembly:** T0-10, T0-11, T1-36, T2-08, T1-09, T1-12, T1-34.
- **Growth:** More separate berths support concurrent visitors; more service heads increase actual capacity. No pedestal counter or boxed core duplicates attached organs. A shared healing head is economical but queues visitors; dedicated heads cost space and supplies.

### T6-10 — Reclamation Mouth
- **Does:** Recovers a selected biological assembly for relocation. Removes it in a visible order after draining resources into reserved containers.
- **Input → output:** Player-approved structure + empty storage + power → recovered blocks, organ cores with their histories, and contained fluids.
- **Create:** 1 Structure Grower + 1 Output Mouth + 1 Cargo Lock.
- **Growth:** Choose recovery speed or energy economy. Add a larger cargo bay for complete rooms. A missing container, occupied chamber or unknown inventory stops removal; it does not erase the difficult part to finish the job.

### T6-11 — Recall Nest
- **Does:** Binds, charges and receives individual armor pieces with preservation anatomy.
- **Input → output:** One inserted eligible piece + Preservation Dose + the armor branch's commissioning biomass → one armed piece; a successful Recall transfers that same piece to its reserved berth.
- **Create:** 1 Item Capsule + 2 Cargo Membranes + 2 Sealing Resin + 1 Station Imprint; root on a 3×3 bone-ribbed substrate bed.
- **Growth:** Four equipment berths, one binding per berth. No processing counts or global item search. Owner authorization and an empty loaded berth are required at transfer time; no automatic chunk tickets. Death Bond charging uses the same service without reserving a receiving berth.

## T7 — Compound organs: configurable large structures and specialized power

The player designs assemblies with separate working, supply and control parts. More sockets allow more combinations, but their feed, cooling and trait limits still have to be met.

### T7-01 — Synthesis Heart
- **Does:** Builds large, compound organ cores from several compatible mature organs. Keeps each contributing organ's useful specialization visible in the resulting assembly.
- **Input → output:** Listed donor organs + Genetic Stock + thermal/spatial materials + biomass/electricity → one recipe-defined compound core, with donor histories assigned rather than copied.
- **Create:** 1 Mutation Chamber core + 1 Trait Regulator + 2 precision-conditioned Spatial Membranes. Build a 5×5 ribbed treatment body with separate donor, reagent and recovery bays.
- **Growth:** Choose assembly speed or energy economy. Add Anatomy Sockets for larger donor sets. A compound core specializes a declared reaction/support role; extraction, transport, treatment and storage do not collapse into its GUI. Shared bodies can shorten transfers or share cooling, but installed working organs still occupy sockets and retain separate supplies, outputs, counters and limits. A specific fusion recipe may consume donor cores once; it never leaves reusable donors plus a second copy of their histories.

### T7-02 — Anatomy Socket
- **Does:** A load-bearing, isolatable graft position in a compound organ. Separates one attachment's feed, output and control from neighboring attachments.
- **Input → output:** An installed compatible organ plus its assigned supplies → that organ's contribution to the host assembly.
- **Create:** 1 reinforced Rib Frame + 1 Vascular Junction + 1 precision-conditioned Spatial Membrane.
- **Growth:** Additional sockets permit another physical attachment where the host supports it. A closed socket allows service without draining the whole assembly; it cannot hide an organ outside the structure's footprint.

### T7-03 — Catalyst Lobe
- **Does:** Maintains a particular genetic catalyst for a Synthesis Heart or advanced chamber. One lobe handles one compatible culture at a time.
- **Input → output:** Matching Genetic Stock + its feed + biomass/electricity → catalyst service for a specified host recipe, gradually consuming the stock.
- **Create:** 1 Genetic Culture Vat + 1 Precision Sequencer + 1 Spatial Membrane.
- **Growth:** Choose replenishment speed or nutrient economy. More lobes support more simultaneous cultures; incompatible cultures need separate fluid paths instead of a shared mixed catalyst tank.

### T7-04 — Expression Switch
- **Does:** Lets an organ change between two installed, compatible operating profiles without pretending that both are active simultaneously. Useful for a batch refinery or a defense station sharing scarce power.
- **Input → output:** Explicit switch command + electricity + a drained/idle host → the other profile after its changeover delay.
- **Create:** 1 Trait Regulator + 1 Scheduler Ganglion + 2 precision-conditioned Spatial Membranes.
- **Growth:** Survey Gel treatment shortens changeover. A second profile still occupies anatomy and needs its own installed materials; switching cannot refund consumed ingredients or erase processing waste.

### T7-05 — Organ Transplanter
- **Does:** Moves one matured core from a temporary body into a prepared permanent assembly. Lets an early organ become the heart of a late workshop without destroying its accumulated value.
- **Input → output:** Recovered donor core + a compatible prepared receiver + biomass → the same living core installed with its counters and choices intact.
- **Create:** 1 Reclamation Mouth + 1 Anatomy Socket + 1 Levitation Chamber attachment. The intact core moves; counters cannot be extracted separately.
- **Growth:** Choose transplant speed or biomass economy. Extra Anatomy Sockets handle larger cores. Transplanting is not a chance-based reroll and does not duplicate donor organs.

### T7-06 — Genome Vault
- **Does:** Stores exotic genomes and larger collections under the DNA storage grades. Connect it to mutation organs or keep a separately populated backup vault.
- **Input → output:** Genome Fragments or exported Genome Records → stored source coverage. Knowledge copies never create Genetic Stock, activity counters or armor mutations.
- **Create:** 1 DNA Bank + 4 Archive Lobes + 2 obsidian + 1 precision-conditioned Spatial Membrane.
- **Growth:** Add Archive Lobes for capacity. A Relay Ganglion synchronizes selected knowledge with another owned vault; physical samples still require actual transport.

### T7-07 — Interceptor Nursery [optional helper branch]
- **Does:** A nursery extension for short-lived combat organisms. One build specializes in ground interception, another in aerial pursuit; ordinary item logistics do not require either.
- **Input → output:** Biomass + real growth stock/ammunition + selected combat Genetic Stock → a limited deployment of temporary defenders with no profitable death loot.
- **Create:** 1 Brood Nursery + 1 Aerocyte Bloom or Spine Sentry + 2 Thermal Linings + 1 Catalyst Lobe; provide separate deployment berths.
- **Growth:** Choose growth speed or feed economy. Extra valid berths increase the permitted squad within its station limit; support loss or lifespan expiry recalls/ends it rather than leaving permanent uncontrolled creatures.

Initial deployment requires a complete matching genome and consumes one Larval
Cyst, two matching stock and 200 BU over 20 s per defender: zombie for the
ground build (Spine Sentry donor), phantom for the air build (Aerocyte donor).
Each defender has 12 health, no armor/loot/XP/sample yield, and 2400 loaded ticks
of life. Ground defenders move at most 0.22 blocks/tick on navigable terrain;
air defenders move at most 0.25 blocks/tick through collision-clear cells.
Both stay within 16 blocks of their nursery and hit for 3 damage at range 1.5,
once per 30 ticks. Ground targets grounded hostiles; air targets airborne
hostiles. No players, pets or bosses. Losing support ends aggression and returns
to a berth; after 100 loaded ticks without support the defender expires without
refund. Unload preserves lifetime and population lease, never frees a spawn slot.
Reuse helper population/path admission and defense target queries. Three berths
maximum; ground/air is a permanent nursery choice, not a switchable command.

### T7-08 — Siege Blossom
- **Does:** A large stationary weapon for a prepared defensive position. Requires the enemy to enter its covered area; it cannot attack a boss anywhere in the dimension.
- **Input → output:** Grown spine ammunition + biomass + charged electricity/steam → a slow, powerful visible shot and spent heat.
- **Create:** 1 Spine Sentry + 1 Targeting Eye + 2 Steam Muscles + 1 Charge Sac; build a 5×5 rooted support body with a clear barrel-like corolla and cooling route.
- **Growth:** Choose reload rate or energy economy. Different genetic grafts favor armor penetration, aerial tracking or crowd suppression; installing one sacrifices another. A blocked muzzle or empty cooling path prevents firing.

Initial range is 32 blocks, nearest eligible hostile then entity ID; line of
sight and a clear muzzle are required. One non-explosive projectile costs
4 Grown Spines + 50 BU + 2000 BE + 250 mB steam. Reserve 250 mB cooling water and
dirty-water output before firing; convert it to 250 mB dirty water. Steam is
retained as spent steam for condensation. Reload is 100 ticks, projectile speed
1.2 blocks/tick, maximum life 60 ticks; hit once for 16 damage through ordinary
armor/encounter defenses. No terrain damage, piercing or recovered ammunition.

Permanent Siege grafts are prepared at Chrysalis and installed at Chamber:
4 each of the named stock, 2 Thermal Linings, 2 Binder and 1000 BU, 40 s;
all genomes complete. Precision (skeleton + silverfish) changes damage to 20
and reload to 140 ticks; it does not bypass boss immunity. Tracking (skeleton +
phantom) permits initial interception aiming at airborne hostiles, range 40,
damage 12, no homing. Suppression (skeleton + slime) deals 8 to the primary and
at most three additional eligible hostiles within 2 blocks at impact, reload140.
All grafts retain the base shot cost; exclusions and the global query/projectile
budgets apply independently to secondary hits. Only one graft per Blossom.

### T7-09 — Fold Gateway
- **Does:** Opens the route to a fourth dimension, provisionally called **the Fold**. It is a prepared expedition gate, not a random world rupture or an automatic end to the campaign.
- **Input → output:** Biomass/electric reserve + passenger-conditioned membranes + an attuned destination seed → a maintained passage to one expedition landing.
- **Create:** 1 Transit Maw + 1 Spatial Conditioner core + 2 Catalyst Lobes + 4 Anchor Roots. Assemble on an End-native 5×5 anchored bed with a working return reserve.
- **Growth:** Choose gate recharge or feed economy. Stabilizing a real Fold settlement permits a larger permanent route. Make the first Attuned Destination Seed using the Item Catalog recipe: an ender pearl, prepared passenger/precision membranes, silverfish/blaze/enderman stock and 500 BU in an End Spatial Conditioner, with all three genomes complete.

## T8 — The Fold: native adaptation and a new settlement

Long-term dimension branch. Its local growth alternates between two environmental phases: one favors absorbing nutrients, the other hardening tissue. The player can store products between phases or build rooms that regulate their local conditions. Neither phase respawns mined ore.

### T8-01 — Adaptive Substrate
- **Does:** A native foundation whose pores open during feeding conditions and close during hardening conditions. Supports Fold growth but cannot replace Thermal or Anchored Substrate for their native recipes.
- **Input → output:** A cultured sample of local ground + biomass + local phase exposure → mature adaptive bed.
- **Create:** Apply 1 Adaptive Seed to exposed Fold root-bearing ground and mature it locally. Ordinary Living Substrate supports the initial shelter while this bed matures.
- **Growth:** Local plant genomes tune its phase preference. Its current condition is visible; relocating it preserves the block but suspends native production outside the Fold.

### T8-02 — Founder Nursery
- **Does:** Grows the settlement's first local organs and a reusable Adaptive Gel. Can run slowly on imported feed before a local farm exists.
- **Input → output:** Local root cuttings + water + biomass + phase exposure → Adaptive Gel and retained starter cuttings.
- **Create:** 1 Thermal Nursery core + 1 Spatial Membrane + a gathered local root cutting; root it on a 3×3 Adaptive Substrate bed.
- **Growth:** Choose growth speed or biomass economy. Add phase-regulated rooms for reliable batches. No boss material is needed to maintain or restart this basic supply.

### T8-03 — Phase Shelter Skin
- **Does:** A wall/floor family that buffers an occupied room against the Fold's changing local growth conditions. Its inner surface visibly differs from its exposed outer surface.
- **Input → output:** Small Adaptive Gel maintenance doses during active regulation → reduced phase fluctuation inside a completed enclosure.
- **Create:** Mutate Living Skin with Adaptive Gel + 1 Thermal Lining.
- **Growth:** Add sealed corners and compatible doors to complete a habitat. A damaged section reduces regulation; it does not instantly delete everything in the room or make all outside hazards harmless.

### T8-04 — Habitat Lung
- **Does:** Actively controls a sealed room's moisture and biological atmosphere. Makes phase-sensitive culture rooms and protected animal/sample habitats possible.
- **Input → output:** Water + biomass + electricity + a selected room setting → maintained habitat conditions and waste heat/fluid.
- **Create:** 1 Hearth Lung + 1 Heat-Exchange Gill + Adaptive Gel + 1 Sensor Polyp.
- **Growth:** Choose regulation rate or energy economy. Larger rooms need more lung area or stronger cooling. Separate hostile and domestic habitats use separate circuits, not one universal base-wide effect.

### T8-05 — Adaptive Culture Loom
- **Does:** Grows replacement membranes that can change between two specified operating conditions. A material choice for a difficult organ, not a universal best armor ingredient.
- **Input → output:** Adaptive Gel + Thermal Lining + Spatial Membrane + a researched local genome → adaptive versions of those same membrane parts.
- **Create:** 1 Bone Loom + 1 Levitation Chamber attachment + 2 local root cuttings; build within a phase-controlled native room.
- **Growth:** Choose growth speed or nutrient economy. Feeding and hardening bays can alternate for output or run separately for reliability. The chosen two conditions remain incompatible with unrelated third specialties.

### T8-06 — Phase Accumulator
- **Does:** Stores phase work supplied by a separate Phase Collector. It is a specialized energy buffer, not simultaneously a generator or phase controller.
- **Input → output:** Supplied phase charge → finite retained charge → discharge to an attached Fold service. No charge is earned merely from standing inside a changing room.
- **Create:** Mutate 1 Charge Sac with 1 Adaptive Gel + 1 Spatial Membrane; brace it with 2 Anchor Roots. It can be built empty before the first collection cycle.
- **Growth:** No processing XP for charging/discharging. Prepared membrane treatments favor discharge rate or retention; joined cells add bounded capacity. More storage bridges longer quiet periods, while extra collectors increase admitted capture area, not capacity by themselves.

### T8-08 — Retuning Root
- **Does:** Changes a small assigned chamber's biological phase on demand. Lets the player pay for steady production instead of waiting for the environment.
- **Input → output:** Electricity + phase charge + biomass → a local feeding or hardening interval, followed by recovery.
- **Create:** 1 Expression Switch + 1 Phase Accumulator + Adaptive Gel + 1 Anchor Root.
- **Growth:** Choose retuning speed or charge economy. Additional roots serve larger completed enclosures. Retuning a workshop does not alter the whole dimension or its natural-resource deposits.

### T8-09 — Quarantine Gate
- **Does:** A controlled station between the native habitat and the return route. Keeps incompatible cargo, active specimens and contaminated containers from entering an ordinary workshop by mistake.
- **Input → output:** Declared cargo + inspection power → admitted packed cargo or a held/refused batch. It classifies declared supported cargo states; it does not secretly separate minerals, neutralize fluid or heal a specimen.
- **Create:** Controller: 1 Reflex Knot + 1 Precision Probe + 1 Adaptive Gel. Install a Cargo Lock and Selective Membrane with separate return and admitted routes; attach a Sensor Polyp for local lock status.
- **Assembly:** T6-04, T5-10, T1-14.
- **Growth:** Add separate lanes for specimens, liquids and equipment. Route a treatable refusal through actual suitable processing organs, then reinspect its new state. An unknown item stays recoverable; clean cargo must not be blocked forever behind a contaminated batch sharing its only buffer.

### T8-10 — Foldroot
- **Does:** A native branching ground plant. Its fleshy roots store the starting material for Adaptive Gel and remain useful after the settlement grows.
- **Input → output:** A mature root cluster → root cuttings; cultivated plants consume water and nutrients to regrow. A cutting can seed a new plant instead of being processed.
- **Create:** Found rooted in native Fold ground; cultivate a recovered cutting on Adaptive Substrate during its feeding phase.
- **Growth:** Grafting Bench variants favor fast cuttings or larger feed reserves. The hardened phase protects the plant but pauses its ordinary harvest cycle, encouraging stored nursery feed or controlled rooms.

### T8-11 — Pulse Reed
- **Does:** A native tall plant that bends and lights in response to environmental phase changes. Also serves as a natural visible indicator near outdoor fields.
- **Input → output:** Mature reed segments → fibers used in place of string and source-labeled plant specimens. Deliberate harvest leaves a basal segment for regrowth.
- **Create:** Found near native wet ground; plant a segment beside water on Adaptive Substrate.
- **Growth:** Local genome graft favors fiber yield or phase sensitivity. A Sensor Polyp can read its state; the reed itself does not power machinery or force a phase transition.

### T8-12 — Glassbloom
- **Does:** A low native flower whose translucent leaves harden during the second phase. Provides both living ground cover and useful chamber material.
- **Input → output:** Fed mature bloom + hardening-phase harvest → clear petals usable as glass in Membrane Window recipes, plus a planting seed. Harvesting the soft phase yields specimens instead of building petals.
- **Create:** Found on exposed native ridges; cultivate a seed on Adaptive Substrate with suitable light and nutrients.
- **Growth:** Grafting Bench variants favor structural petals or genetic sample recovery. Retuning a controlled room can schedule the harvest, but every batch still regrows from supplied material.

### T8-13 — Phase Collector
- **Does:** Captures work during a natural local phase transition across its assigned exposed tissue surface. Shares no storage or phase-changing function with an accumulator or Retuning Root.
- **Input → output:** One natural transition + maintained native collector tissue + an accepting Phase Accumulator → a capped amount of phase charge. One surface participates in one collector; overlapping assignments do not multiply capture.
- **Create:** 1 Chorus Resonator + 1 Adaptive Gel + 2 Anchor Roots; expose its supported collector surface in the Fold and attach a Phase Accumulator.
- **Growth:** Choose capture rate or biomass maintenance economy. More real surface permits more admitted work within the station limit; it also uses land exposed to local hazards. Full storage declines excess capture. Artificially retuned transitions yield no charge in this proposal, so a powered room cannot farm its own retuning energy. No offline transition replay or dimension-wide collector scan.

## T9 — The Manyfold encounter and post-boss specialization

Long-term boss branch within the Fold. **The Manyfold** is a proposed territorial organism awakened deliberately at a prepared site. Its defeat grants a new genetic template, not god armor or an infinite-material generator.

### T9-01 — Calling Corolla
- **Does:** Initiates the boss encounter after the player supplies its physical growth site and selects a readiness check. Ordinary base production does not accidentally activate it.
- **Input → output:** Adaptive Gel + Foldroot, Pulse Reed and Glassbloom Genetic Stock with completed genomes + charged local phase supply → one active Manyfold encounter at the prepared site.
- **Create:** 1 Founder Nursery core + 1 Chorus Resonator + 2 adaptive membranes. Install inside a clearly marked native clearing, separate from the return station.
- **Growth:** No processing levels from repeatedly summoning the boss. Reinforced surrounding construction protects services, but the corolla does not provide immunity or prevent the player from retreating.

### T9-02 — Resonance Sink
- **Does:** Receives a specific encounter pulse and makes one phase of the Manyfold vulnerable for a short interval. Placement, direction and which sink is supplied matter.
- **Input → output:** Correct incoming pulse + electricity + cooling → a vulnerability window and waste heat. Wrongly tuned or unfed sinks provide no opening.
- **Create:** 1 Phase Accumulator + 1 Heat-Exchange Gill + 1 Phase Isolator + 1 adaptive membrane.
- **Growth:** Choose recovery speed or cooling economy through actual noncombat pulse-handling recipes. Several physically separated sinks support different encounter strategies; copies in one corner do not stack unlimited damage vulnerability.

### T9-03 — Severing Root
- **Does:** A fixed offensive organ that cuts an exposed connection of the Manyfold during its vulnerable interval. An infrastructure-heavy alternative to delivering all damage personally.
- **Input → output:** Grown spine stock + phase charge + a clear vulnerable target → one severing strike, then a long recharge.
- **Create:** 1 Siege Blossom core + 1 Retuning Root + 2 adaptive membranes; anchor its firing direction at the encounter boundary.
- **Growth:** Choose recharge speed or charge economy. A tracking graft trades strike power for easier alignment. It cannot attack through walls, while untuned, or from another dimension.

### T9-04 — Pattern Incubator
- **Does:** Turns the recovered Manyfold genome into a maintainable culture. Makes post-boss work about designing new organisms rather than killing the same boss for every graft.
- **Input → output:** First boss-derived tissue sample + completed matching genome + Adaptive Gel + ordinary compatible culture feed → Manyfold Genetic Stock. No boss loot or additional unique reward is generated.
- **Create:** Mutate a Catalyst Lobe with the first recovered Manyfold tissue sample and 2 adaptive membranes; install in a regulated Fold habitat.
- **Growth:** Choose culture speed or biomass economy. Multiple bays support supply for several projects, but continued native materials and habitat service remain necessary.

### T9-05 — Reciprocal Graft
- **Does:** A compound-organ attachment that shares a fixed adaptation budget between two installed organs. Useful when a base alternates mining, processing and defense loads rather than running everything at peak power.
- **Input → output:** Two compatible donor profiles + Manyfold Genetic Stock + a selected allocation + power → more capacity for one profile and correspondingly less for the other.
- **Create:** 1 Expression Switch + 2 Anatomy Sockets + 1 Pattern Incubator culture dose + 2 adaptive membranes; grow through a Synthesis Heart recipe.
- **Growth:** Choose changeover speed or operating economy. The player can prioritize a furnace's throughput during production, then a weapon's recharge during defense; both cannot receive the full shared benefit simultaneously. Removing it restores the donors' original histories and choices, without duplicating their counters.
