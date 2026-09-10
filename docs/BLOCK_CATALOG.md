# Block and organ catalog

Draft recipes and tier assignments for discussion. Entries describe proposed gameplay, not implementation status. Quantities in **Create** are crafting ingredients; a multiblock's additional parts are stated separately. Process yields and operating costs still need recipe-by-recipe balancing. [Item Catalog](ITEM_CATALOG.md) owns consumable names and preparation. Construction may use raw structural components; functional mutations use prepared treatments. Growth shorthand such as “rabbit-genome mutation” means a target-specific Organ Trait Graft, not applying a raw mob drop. Armor follows its separate irreversible tree: no regulator, transplanter, adaptive material or profile switch may exchange committed branches, refund choices or transfer incompatible-lineage learning. Armor presets change only settings of installed compatible anatomy.

**Tier** is when a block first becomes available. **Organ level** is earned by using that particular organ; it does not reset when the colony reaches another tier. Unless an entry says otherwise, an active organ gains one count per completed batch, not per tick or failed attempt. At 32, 128 and 512 counts, choose one improvement from that organ's **Growth** options. Each choice gives +10% to the selected property, at most three choices total. New functions require the listed mutation or physical addition; levels alone do not add every function.

Breaking an organ preserves its counts, level, chosen improvements and mutations in the dropped block. In a multiblock, the core carries this history; breaking a wall does not copy it. Upgrading an existing core preserves its history. Passive walls, floors and conduits do not earn processing levels. Storage contents must be retained in the recovered storage or its remaining cells, never sprayed into the world on invalidation.

Free-standing organs root into Living Substrate or a named specialized bed. Attached parts root through their host. The same organ can be hand-fed first, mutated later and expanded with physical parts without crafting a replacement core. Port configuration uses a tool or deliberate UI action; empty-hand clicks do not obstruct building.

One starting ingredient is an item, not a block: **Spore Culture = 1 rotten flesh + 1 red mushroom + 1 wheat seed**. Biomass is the colony's consumable feed, measured as fluid; one bucket holds 1,000 mB. Other new construction materials are introduced by the blocks that produce them below. Colored membranes and skin can use vanilla dyes; color does not change routing or function.

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

### T0-03 — Digestive Sac
- **Does:** Converts unwanted organic matter into the first usable biomass. Food-rich inputs give more than woody scraps; mineral blocks are not food.
- **Input → output:** Rotten flesh, surplus crops, leaves or fish + water → biomass. Bones are returned as mineral residue rather than silently converted into flesh.
- **Create:** 1 Organ Bud + 2 rotten flesh + 1 bowl.
- **Growth:** Choose processing speed or biomass recovery. Elastic Gel treatment accepts fibrous leaves; add Intake/Output Mouths for a continuous feed line. It stops accepting batches when output has no room.

### T0-04 — Biomass Bladder
- **Does:** A one-block, translucent feed tank for a first workshop. The contents visibly rise inside its fleshy shell.
- **Input → output:** Biomass from buckets or an attached organ → stored biomass, then bucket or organ output. Other fluids are refused.
- **Create:** 1 Organ Bud + 2 glass + 1 slime ball; an early leather replacement for the slime ball makes a smaller bladder.
- **Growth:** Skeletal Graft treatment reinforces its capacity. It can later become a Biomass Reservoir Cell without discarding its stored feed.

### T0-05 — Membrane Rack
- **Does:** Stretches cultivated skin over a bone rack to make the common flexible construction material.
- **Input → output:** Rotten flesh + string + water → Membrane Sheets; leather can replace flesh for a slower, low-biomass recipe.
- **Create:** 2 sticks + 2 bones + 1 Spore Culture.
- **Growth:** Choose batch speed or water economy. Add a Hearth Lung for faster drying; thermal membranes are a later, distinct Nether-grown material, not an automatic level reward.

### T0-06 — Bone Loom
- **Does:** Grows curved plates and load-bearing ribs from existing calcium, rather than fabricating mineral mass from biomass.
- **Input → output:** 1 bone + 50 BU → 1 Bone Plate. Bone blocks require a separately accounted calcium recipe, not an assumed unpacking into nine bones. A calcite recipe uses extra biomass to supply the organic binder.
- **Create:** 1 Organ Bud + 2 bones + 2 sticks.
- **Growth:** Choose speed or biomass economy. Ferrocyte Paste treatment produces reinforced plates using additional prepared iron; adding frame blocks increases batch size, not mineral yield.

### T0-07 — Bio-Furnace
- **Does:** A small stomach-like furnace. Initially slower and more fuel-hungry than a vanilla furnace, but it accumulates permanent processing experience.
- **Input → output:** A normal smelting ingredient + biomass → its normal smelting result. Starting target: 16 seconds per item; no free ore multiplication or extra XP from recooking outputs.
- **Create:** 1 furnace + 1 Organ Bud + 2 Membrane Sheets.
- **Growth:** Choose speed or biomass economy at each level. L1 permits a Char Gland Feed treatment for solid-fuel feeding; Hearth Lungs improve combustion. Mouths automate loading; a T3 Thermal Mantle enables hotter recipes on this same core.

### T0-08 — Awakening Cradle
- **Does:** Turns crafted dormant bio equipment into living equipment. Holds one armor piece, tool or weapon at a time; the body of the cradle folds around it.
- **Input → output:** Dormant equipment + Spore Culture + biomass → awakened equipment with the same identity. It does not supply free repair or an infinite personal fuel reserve.
- **Create:** 1 Organ Bud + 2 Bone Plates + 2 Membrane Sheets.
- **Growth:** Choose shorter awakening or lower biomass cost. Honey Culture treatment reduces awakening's processing biomass cost, not the wearer's starvation pain. Later mutation converts the cradle into a Repair Dock, preserving its history.

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
- **Does:** Replaces farmland with rooted crop beds. Separate settings reserve water, seed stock and harvest surplus, so a field need not consume all of its own food.
- **Input → output:** Seeds/plants + water + optional biomass → grown crops. Biomass accelerates growth but does not make the crop-to-biomass loop profitable by itself.
- **Create:** Hoe mature exposed Living Substrate, then apply 1 Rooting Gel and a real seed; the hoe is not consumed.
- **Growth:** Honey Culture treatment favors pollinated crops; bone meal favors rapid first growth. Upgraded beds can support unusual crops only after their genome and growing conditions are available.

### T1-18 — Harvest Corolla
- **Does:** Harvests a player-marked small field and replants from reserved seed stock. Its tendrils reach only the configured bed, not an entire biome.
- **Input → output:** Mature crops + replanting items + biomass → harvested produce in its output buffer and replanted beds.
- **Create:** 1 Organ Bud + 1 iron hoe + 2 Membrane Sheets.
- **Growth:** Choose harvest speed or biomass economy. Add adjacent Cultivation Tissue and a second intake to serve a larger field or multiple crop types; new area costs more operating feed.

### T1-19 — Compost Gland
- **Does:** Recovers fertilizer from low-value organic scraps. It complements digestion rather than being a second machine with the same biomass output.
- **Input → output:** Leaves, crop scraps and spoiled feed + water → bone meal. Food can be digested or composted, not processed through both for full yield.
- **Create:** 1 composter + 1 Organ Bud + 1 Membrane Sheet.
- **Growth:** Choose processing speed or water economy. Mushroom mutation accepts woody residue; an attached Output Mouth feeds the farm without dropping fertilizer on the ground.

### T1-20 — Arbor Root
- **Does:** Grafts a selected tree into the colony. The player chooses either a living resource tree or one-time salvage, rather than automatically stripping every nearby tree.
- **Input → output:** Sapling/log, water and nutrients → a tended tree; pruning yields branches/wood while preserving a chosen trunk and canopy. Salvage consumes existing wood and routes that finite harvest to storage.
- **Create:** 1 Organ Bud + 1 sapling + 1 iron axe; place against the selected trunk on substrate.
- **Growth:** Choose regrowth speed or nutrient economy. Birch, oak and other completed tree genomes unlock species-specific grafts and alternate canopy outputs.

### T1-21 — Digestive Tissue
- **Does:** A defensive floor that damages occupants lacking the required full bio suit and gathers biomass from damage actually dealt.
- **Input → output:** Successful damage to a vulnerable living target → a small biomass reserve, extractable by an adjacent vein. Armor-protected or invulnerable contact gives no biomass.
- **Create:** Mutate mature substrate with 1 Digestive Enzyme.
- **Growth:** Spider DNA permits a damage-versus-slowing specialization. Connecting a Nerve Tissue shutoff lets a public path be opened safely; it does not become permanently safe merely because storage is full.

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
- **Does:** A bellows-like attachment for the Bio-Furnace. Improves airflow but requires free space at its breathing face.
- **Input → output:** Air + a host's ongoing fuel consumption → increased furnace heat delivery. It has no independent smelting slots.
- **Create:** 2 leather + 1 Bone Plate + 1 Organ Bud; attach to a furnace side.
- **Growth:** Add a second opposed lung for batch work. Contractile Fiber treatment favors speed; charcoal mutation favors fuel economy. Blocking a lung removes its benefit rather than destroying the furnace.

### T1-25 — Sapping Bush
- **Does:** Useful low ground cover for living paths and gardens. Its growth settings select berries, fiber or biomass feedstock, so the colony does not become a bare carpet.
- **Input → output:** Water, light and nutrients → the selected harvest; harvest reserves enough plant body for regrowth.
- **Create:** Graft a sweet berry bush with Rooting Gel, or plant its recovered cutting on substrate. Converting an existing bush retains a seed/cutting for the player.
- **Growth:** Contractile Fiber treatment favors fiber; honey favors berries. A Harvest Corolla gathers the crop into storage; decorative unharvested bushes remain alive without constant manual trimming.

### T1-26 — Living Wood
- **Does:** The grafted trunk of a colony tree and a reusable building material. Axis, branches and original wood species remain recognizable.
- **Input → output:** Deliberate harvest → the original species' wood plus a retained planting option at the Arbor Root; it does not also pay a second full biomass harvest.
- **Create:** An Arbor Root transforms the selected tree gradually while supplied, or Rooting Gel is applied manually to an exposed log.
- **Growth:** Skeletal Graft treatment makes reinforced structural wood. The player can keep a productive trunk or choose salvage, but regrowing harvested wood needs time, water and nutrients.

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

## T2 — Directed mutation: genomes, specialized organs and planned excavation

Build sample processing and a DNA bank. Individual genomes unlock particular grafts; collecting unrelated samples is not a substitute.

### T2-01 — Specimen Extractor
- **Does:** Separates usable genetic material from source-labeled mob drops, plants and tissue samples. The basic organ wastes more of a rare specimen than an upgraded laboratory would.
- **Input → output:** Identified specimen + water + biomass → source-specific Genetic Stock and a readable genome fragment. Remaining organic material goes to a byproduct slot.
- **Create:** 1 Organ Bud + 2 glass bottles + 1 iron ingot + 2 Membrane Sheets.
- **Growth:** Choose recovery or speed. Add a Sequencing Lens for more informative fragments; electrical separation later improves rare-sample recovery. Display expected coverage before consuming a valuable specimen.

### T2-02 — DNA Bank
- **Does:** Stores accumulated genome knowledge, shows missing coverage and makes completed genomes available to connected mutation organs. Knowledge is distinct from consumable Genetic Stock.
- **Input → output:** Extracted fragments → increased coverage for the correct source; research queries → available traits and compatible mutation recipes.
- **Create:** 1 Organ Bud + 1 amethyst shard + 2 glass + 2 Bone Plates.
- **Growth:** Attach Archive Lobes for more genome slots. Survey Gel treatment compares related fragments; it does not award coverage for repeatedly inserting the same previously consumed fragment.

### T2-03 — Archive Lobe
- **Does:** A physical memory extension to a DNA Bank, showing stored species on its membrane. Later control organs use the same lobe for recipe or index records instead of needing a second memory-block family.
- **Input → output:** Bank records, or a controller's recipe/index records, assigned to its slots → retained records accessible through that host.
- **Create:** 1 Membrane Window + 1 amethyst shard + 1 Membrane Sheet.
- **Growth:** Attach more lobes to valid faces. Removing one preserves its assigned records in that lobe; the same records cannot remain as a second physical archive copy by accident.

### T2-04 — Sequencing Lens
- **Does:** A precision eye attached to an extractor. It selects which missing portion of a specimen's genome the next batch should resolve.
- **Input → output:** A specimen being processed by the host + bank's missing-coverage selection → better-targeted fragments, not extra mob loot.
- **Create:** 2 glass + 1 amethyst shard + 1 spider eye + 1 Membrane Sheet.
- **Growth:** Lumen Secretion treatment improves weak-sample visibility; electrical drive supports finer rare-genome work. Multiple lenses cover different sample channels, not unlimited yield multipliers on one specimen.

### T2-05 — Genetic Culture Vat
- **Does:** Maintains consumable genetic cultures after the source genome is understood. Avoids requiring another boss kill for every routine use of an already-developed mutation.
- **Input → output:** Completed genome access + a seed of matching Genetic Stock + recipe-specific feed and biomass → more stock of that same culture. No bones, pearls, stars or other mob loot are produced. A separate material-binding mode grows Fusion Binder and mineral fusion media without creature DNA, using the Item Catalog recipes.
- **Create:** 1 Fluid Cyst + 1 Organ Bud + 1 Sequencing Lens.
- **Growth:** Choose culture speed or biomass economy. Rare cultures require later precision attachments and their own compatible feed; wheat alone is not a substitute for every source.

### T2-06 — Mutation Chamber
- **Does:** Applies chosen genetic and material changes to one equipment item or recoverable organ core. Shows the resulting properties and incompatible mutations before starting.
- **Input → output:** Target + prepared target-specific Mutation Graft or fusion medium + biomass + genome access → the same target with the chosen mutation; its counters and identity remain.
- **Create:** Core recipe: 1 Awakening Cradle + 1 Sequencing Lens + 2 Bone Plates. Form a 3×3 floor, corner Rib Frames and a two-block-high Membrane Window enclosure around the central treatment space.
- **Growth:** Choose treatment speed or biomass economy. Burrowing armor uses enderman-derived prepared grafts and Diamond-Fiber Matrix under Armor Evolution. Further burrowing mutations extend the safe stopping window and reduce biomass use, competing with other suit improvements. Add a Memory Gland or later a potion-infusion bay; a larger body accepts larger organ cores. Precision Chamber means this core/body with a T4-07 Precision Sequencer service bay, electrical supply and access to T4-05 prepared outputs; it is not an unlisted new block.

### T2-07 — Memory Gland
- **Does:** An attachment that transfers earned equipment or organ experience into a reusable physical memory sample. It is a way to retain invested play, not a universal XP converter.
- **Input → output:** Source + empty glass bottle + biomass → a typed Memory Sample; the extracted counts are removed from the source. Compatible sample + target → transferred counts, consuming that amount from the sample; capped remainders stay in the sample. Armor additionally requires matching permanent lineage and slot, with no transfer of symbiosis or grafts.
- **Create:** 1 Organ Bud + 1 amethyst shard + 1 book + 1 Membrane Sheet.
- **Growth:** Choose transfer speed or biomass economy. Enderman-genome mutation preserves more specialized categories. Furnace processing history cannot be installed as running history in boots.

### T2-08 — Repair Dock
- **Does:** Refills and self-mends worn bio equipment using colony supplies. Supports one wearer or one stored equipment set; it is useful beside mine exits and travel stations.
- **Input → output:** Damaged bio equipment + biomass + any recipe-specific repair material → repaired equipment and a filled suit reserve.
- **Create:** Mutate an Awakening Cradle with 2 Restorative Serum + 2 Membrane Sheets.
- **Growth:** Choose repair speed or biomass economy. Add hanger-like Rib Frames for a stored set; an extra mouth reserves repair material separately from suit fuel. Only eligible chest wear can grant bounded C-R repair practice under Armor Evolution; service grants no unrelated movement/combat counters.

### T2-09 — Grafting Bench
- **Does:** Joins a known plant trait to compatible planting stock, so crop and tree selection affects the farm layout and its products.
- **Input → output:** Seed/sapling + matching Genetic Stock + biomass + bank access → grafted planting stock with its stated nutrient, light and water requirements.
- **Create:** 1 Bone Loom + 1 iron hoe + 1 Sequencing Lens.
- **Growth:** Choose graft speed or biomass economy. Add two Seed Pouches to keep parent lines separate; advancing to a new tier unlocks combinations, not a seed that grows every resource.

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
- **Input → output:** Planting stock + water habitat + nutrients → aquatic crops or raised fish, using separate recipes. Plants do not become fish without brood stock.
- **Create:** Mutate submerged mature substrate with 1 Aquaculture Graft.
- **Growth:** Fish-genome graft improves husbandry; kelp-genome graft improves plant production. Add sheltered cells and a Harvest Corolla for automation; crowding reduces output rather than generating unlimited fish entities.

### T2-13 — Brood Nursery [optional helper branch]
- **Does:** Hatches a small set of larvae, then accepts a chosen worker mutation. A 3×3 living bed provides separate berths around the core rather than one shared spawn point.
- **Input → output:** Biomass + larval growth materials + a permitted worker graft → an assigned worker. A full roster or occupied berth blocks hatching.
- **Create:** 1 Organ Bud + 1 egg + 2 Membrane Sheets + 2 Bone Plates; install on a clear 3×3 substrate bed.
- **Growth:** Choose hatching speed or nutrient economy. Add a profession bay for harvesting or delivery. Idle workers return here; death frees their berth, and recall permits safe relocation with cargo retained.

### T2-14 — Worker Waypoint [optional helper branch]
- **Does:** A nearly flush marked tissue cell defining a worker's route or waiting place. Routes can follow stairs, terraces and climb-capable paths.
- **Input → output:** Nursery assignment → a named route point and visible occupied/free state.
- **Create:** Mutate mature substrate with 1 Waymark Secretion, then link it to a nursery.
- **Growth:** Contractile Fiber treatment makes it a waiting perch for an eligible flying helper. Adding waypoints refines a route; it does not authorize a worker to search unloaded terrain.

### T2-15 — Sail Roost [optional helper branch]
- **Does:** Feeds and houses one Nutrient Sail. The player assigns the sail to named nearby nurseries, giving it a useful colony-support role rather than mandatory ownership of every chunk.
- **Input → output:** Biomass + an installed sail organism → supplied local worker coordination and status display.
- **Create:** 2 Rib Frames + 1 Organ Bud + 2 Membrane Sheets + 1 feather.
- **Growth:** Locomotor Gel treatment increases feeding throughput; Survey Gel treatment improves its monitoring role. Chunk loading, if selected later, is a separate mutation with a separate upkeep requirement, not a free roost effect.

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
- **Does:** Draws susceptible nearby creatures toward a marked feeding or sampling location. The creature still needs a navigable path.
- **Input → output:** Target-specific food/scent + biomass → attraction toward the polyp, not spawned mobs.
- **Create:** 1 Organ Bud + 1 honey bottle + 1 spider eye.
- **Growth:** Choose scent duration or nutrient economy. Rabbit, fish and livestock genomes unlock appropriate lures; a Nerve Tissue signal disables attraction when the destination pen is full.

### T2-19 — Restraining Tissue
- **Does:** Holds or heavily slows susceptible creatures on a small sampling/defense floor. It is separate from lethal Digestive Tissue.
- **Input → output:** Supplied biomass + contact with a permitted target → restraint until duration or fuel runs out.
- **Create:** Mutate mature substrate with 1 Restraining Graft and spider-genome access.
- **Growth:** Slime-genome graft improves restraint strength; spider-genome graft improves duration. Stronger mobs resist more; players and bosses require explicit targeting rules, not automatic indefinite immobilization.

### T2-20 — Structure Grower
- **Does:** Builds a selected small biological wall, room or repeated pattern from supplied parts. Useful for enclosing organs without hand-placing every decorative rib.
- **Input → output:** A player-marked template + actual blocks + biomass → those blocks placed at clear authorized positions. Obstructions are reported, not consumed.
- **Create:** 1 Organ Bud + 1 crafting table + 1 amethyst shard + 2 Rib Frames.
- **Growth:** Choose placement speed or biomass economy. Skeletal Graft treatment supports larger spans; later electrical control adds material requests and multi-step plans. It never generates the template's blocks from biomass alone.

### T2-21 — Descending Rootstock
- **Does:** Mines a lit, walkable staircase into real ground. Its tip can be assigned a new branch at a prepared landing; a finite section finishes and then waits.
- **Input → output:** Biomass + stair/tendon/light supplies + real terrain → mined materials in storage and a descending passage with three clear blocks of headroom above each tread.
- **Create:** 1 Organ Bud + 1 iron pickaxe + 2 Bone Plates + 1 Leaching Gland; plant at a Surveyed Tissue collar.
- **Growth:** Choose cutting speed or biomass economy. Ferrocyte Paste treatment accepts tougher host rock; additional root collars authorize another section. Missing spoil space or access parts stops excavation before the next cut.

### T2-22 — Surveyed Tissue
- **Does:** Marks a mining boundary as part of the ground, without raised border blocks. A visible line and corner pores show the actual selected footprint.
- **Input → output:** Player corner/depth selections → a finite plan accepted by its mining organ; no resource output itself.
- **Create:** Apply 1 Survey Gel to exposed mature substrate; configure the resulting marked cells with the tool.
- **Growth:** A Synaptic Gel treatment adds a pause point; luminous graft marks shaft edges. Changing a plan shows its new extent before work resumes, including a one-block-wide strip option.

### T2-23 — Mineral Gizzard
- **Does:** Crushes mined raw ore and separates coarse host rock. The first ore-processing branch adds recovery at the cost of another organ and more handling.
- **Input → output:** Supported raw ore + biomass → source-specific mineral concentrate and stone residue. Ingots and metal blocks are not accepted as fresh ore for another yield bonus. A separate granulation mode prepares ingots, diamonds and obsidian for fusion with no yield bonus; Washing Kidney cleans each portion.
- **Create:** 1 Organ Bud + 1 iron pickaxe + 2 Bone Plates + 2 flint.
- **Growth:** Choose grinding speed or biomass economy. Add a Steam Muscle for bulk batches; a Faceted Chitin treatment permits harder ores. Concentrate carries the mineral amount recovered from its original input, not an unlimited multiplication opportunity.

### T2-24 — Washing Kidney
- **Does:** Washes crushed ore before smelting. The player chooses a simple dry furnace route or more recovery with a water supply and waste handling.
- **Input → output:** Mineral concentrate + water → washed concentrate + tailings containing the unrecovered mineral fraction. Wash water goes to a separate dirty-fluid output.
- **Create:** 1 Organ Bud + 2 Membrane Sheets + 1 sand + 1 glass bottle.
- **Growth:** Choose washing speed or water economy. Elastic Gel treatment improves fine-particle capture; a later Ion Separator recovers selected tailings. Full waste output stops new batches.

### T2-25 — Feeding Trough
- **Does:** Supplies an assigned livestock pen from stored food, with a target population and a breeding reserve. The player chooses food production, sampling or animal growth as the pen's priority.
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
- **Growth:** Rabbit-genome graft favors recovery after movement; livestock genomes permit controlled pen care. It cannot instantly refill a live specimen's depleted sampling reserve.

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

## T3 — Thermal colony: a working Nether base and larger mines

The Nether grows Thermal Lining continuously. It is needed for hot-fluid service, high-temperature organ bodies and steam-driven attachments. Heat alone in another dimension does not replace the native growing bed.

### T3-01 — Thermal Substrate
- **Does:** A heat-tolerant living foundation that matures only in the Nether. Supports native thermal growth and protects its own tissue from ordinary local heat, not everything standing on it.
- **Input → output:** Starter substrate + local heat + biomass → mature thermal bed. No lava or ore is generated.
- **Create:** In the Nether, mutate mature Living Substrate with 1 Thermal Seed. An immature bed can be started with hand-carried biomass.
- **Growth:** Mature with continued local feeding. Removed cells retain their form but do not support Nether-native production outside the Nether; ordinary thermal-lined building parts remain usable elsewhere.

### T3-02 — Thermal Nursery
- **Does:** Grows Thermal Lining on a Nether-native bed. This is the continuing local supply for industrial organs, not a one-time dimension-unlock item.
- **Input → output:** Membrane Sheets + magma cream + biomass + controlled local heat → Thermal Lining sheets.
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
- **Input → output:** 1 Bone Plate + 1 Thermal Lining + 100 BU + heat → 1 Tempered Bone Plate. Washed iron, copper or gold concentrate + heat → the corresponding ingots and retained residue, according to the concentrate's mineral content. Item Catalog owns these yields; thermal processing does not require an additional raw iron reinforcement recipe.
- **Create:** Surround the furnace with an eight-block 3×3 Thermal Substrate ring, four lined corner Rib Frames and four Thermal Linings on its chamber faces. Leave its service and output faces accessible.
- **Growth:** A Steam Muscle adds batch pressure; additional lungs trade space for rate. Tempered plates reinforce large mining heads and electrical organs. Ordinary ingots do not become extra metal merely by passing through the mantle.

### T3-10 — Steam Muscle
- **Does:** A shared powered attachment for gizzards, large furnace bodies and moving mining assemblies. Visible contracting tissue explains the host's faster or heavier work.
- **Input → output:** Steam + a host's work request → mechanical work and spent steam for a condenser.
- **Create:** 1 Organ Bud + 2 Thermal Linings + 2 iron ingots + 1 slime ball.
- **Growth:** Choose work rate or steam economy. Mounting another muscle enables a larger valid host assembly; disconnected muscles cannot smelt or mine by themselves.

### T3-11 — Descending Cradle
- **Does:** A mining platform that descends through a real vertical shaft. The player can ride it or walk the preserved shaft access after work stops.
- **Input → output:** Steam + biomass + access/lining supplies + actual terrain below → a deeper shaft, stored mined blocks and a maintained climbing route.
- **Create:** Core: 1 Organ Bud + 1 diamond pickaxe + 2 Thermal Linings + 2 Bone Plates. Build a 3×3 rib-supported deck with two Steam Muscles, a matching Boring Jaw below it and a side Climbing Tendon.
- **Growth:** Choose descent speed or steam economy. Expand the deck with a matching supported head to increase shaft width. It refuses to move passengers into an obstruction and does not leave them without an exit.

### T3-12 — Boring Jaw
- **Does:** A shaped mining head attached to a cradle or fixed gallery organ. Teeth cut a declared cross-section; the head itself does not contain a second miner or independent inventory.
- **Input → output:** Host power + physical contact with marked terrain → cut blocks delivered to the host's output route.
- **Create:** 2 Tempered Bone Plates + 2 iron pickaxes + 1 Thermal Lining; assemble head segments around the chosen clear passage.
- **Growth:** Faceted Chitin treatment accepts harder ores; membrane mutation favors clean host-rock separation. A wider head requires more power and spoil handling rather than providing a free size increase.

### T3-13 — Digestion Crucible
- **Does:** Dissolves host rock inside a sealed, finite excavation pit, then drains the medium so ores can be recovered. This trades a continuous tunnel for a prepared recovery site.
- **Input → output:** Marked real host rock + Bioactive Leaching Charge + biomass → accounted host residue, exposed untouched ores and Spent Leach Cake. The finite internal medium is not a world-placeable fluid.
- **Create:** 1 Leaching Gland + 2 Thermal Linings + 2 Bone Plates as the core; line a Surveyed Tissue footprint with Living Skin grafted with one Thermal Lining per cell, and reserve a drain route.
- **Growth:** Choose treatment speed or reagent economy. More lining increases depth; access tendons and a drained recovery landing are required before opening the pit. Full drains pause treatment.

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
- **Growth:** Choose recharge speed or steam economy. Contractile Fiber treatment softens takeoff; the destination still needs a safe landing surface. Covering the launch path prevents activation.

## T4 — Electrical precision: better recovery and controlled automation

Bioelectric organs make exact sampling, coordinated production and larger excavation practical. Electricity supplements biomass and physical materials; it does not replace genomes or native beds.

### T4-01 — Electrocyte Stack
- **Does:** Produces electricity through a living stack of charged membranes. A slow metabolic build and a steam-assisted build use the same core.
- **Input → output:** Biomass + water and optional steam assistance → electrical power, spent water and heat. Its output is below the energy needed to recreate its consumed supplies.
- **Create:** 1 Organ Bud + 2 copper ingots + 2 Membrane Sheets + 2 Tempered Bone Plates + 1 redstone block; its first operation requires no electricity.
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

### T4-06 — Live Sampling Cradle
- **Does:** Takes controlled samples from a housed living creature rather than consuming only its death drops. Restraint and target recovery become part of the pen design.
- **Input → output:** A permitted living specimen + biomass + electricity → a source-labeled tissue sample while dealing disclosed damage to that specimen.
- **Create:** 1 Specimen Extractor + 1 Ion Separator + 2 Rib Frames; add a clear target berth with Restraining Tissue and an access gate.
- **Growth:** Choose recovery quality or lower damage per sample. A target has a replenishing sample reserve; healing it does not instantly refill that reserve. Boss support is species-specific, not an unlimited Nether Star substitute machine.

### T4-07 — Precision Sequencer
- **Does:** An extractor attachment for resolving difficult genomes from fewer valuable specimens. It makes saving a rare sample for a better laboratory worthwhile.
- **Input → output:** Host's rare specimen + electricity + completed related research where specified → high-quality genome fragments; actual sample material is still consumed.
- **Create:** 1 Sequencing Lens + 1 Ion Separator + 1 amethyst cluster + 1 gold ingot.
- **Growth:** Choose sequencing speed or electricity economy. Additional lenses let one laboratory handle several source families. It reveals expected coverage gain before consuming a Nether Star or other scarce input.

### T4-08 — Potion Infuser
- **Does:** A mutation-chamber attachment that installs a limited temporary chemical load into eligible bio equipment, or applies a supported potion-based organ treatment.
- **Input → output:** Real potion doses + eligible target + biomass → stored charges of that specific effect; used bottles return empty.
- **Create:** 1 Distillation Crown + 1 Membrane Sheet + 1 gold ingot; attach to a Mutation Chamber bay.
- **Growth:** Choose infusion speed or biomass economy. A membrane mutation adds charge capacity, competing with another equipment property. It does not permanently grant every potion effect or remove ordinary potion use.

### T4-09 — Trait Regulator
- **Does:** Coordinates conflicting mutations in a chamber. Lets the player choose a stronger specialist combination, not erase its costs.
- **Input → output:** A target's selected trait plan + compatible Genetic Stock + electricity → a regulated mutation with shown benefits, load and exclusions.
- **Create:** 1 Memory Gland + 1 comparator + 1 gold ingot + 1 Precision Sequencer.
- **Growth:** Choose processing speed or electricity economy. End-grown parts later permit more complex combinations. Nether resistance, ocean pressure adaptation and burrowing endurance still compete for the target's finite capacity.

### T4-10 — Scheduler Ganglion
- **Does:** Sends a short configured sequence to attached organs: reserve materials, run a batch, wait for output, then start the next batch.
- **Input → output:** Player-set steps + sensor conditions + electricity → commands to named connected organs. It does not manufacture missing inputs.
- **Create:** 1 Synaptic Console + 1 clock + 1 comparator + 1 Organ Bud.
- **Growth:** Survey Gel treatment supports more conditions; additional ganglia divide a workshop into independently scheduled lines. A failed step displays its blocked dependency instead of silently skipping it.

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
- **Does:** Cuts a **one-block-wide long strip** from the surface toward a chosen depth. Starts at a Climbing Tendon and preserves lit resting/access points as it descends.
- **Input → output:** Real marked terrain + biomass + steam/electric drive + tendon/light supplies → recovered blocks and an accessible deep trench.
- **Create:** Core: 1 Descending Rootstock + 2 Thermal Linings + 1 Ion Separator. Install beside a ground-level Surveyed Tissue line, a spoil store and the starting tendon.
- **Growth:** Choose cutting speed or energy economy. Extend the surveyed surface line to authorize another finite strip section. It does not turn into an unlimited chunk quarry when its first plan finishes.

### T4-14 — Spoil Sorter
- **Does:** Keeps large mines from blocking their ore output with common stone. Separates ore, useful building rock and selected surplus before transport.
- **Input → output:** A miner's mixed output + electricity → filtered item routes and retained excess. Destructive disposal is a separate explicit destination.
- **Create:** 1 Item Capsule + 2 Output Mouths + 1 Filter Valve + 1 comparator.
- **Growth:** Choose sort speed or electrical economy. Add capsules for stock reserves. An overflow line can send surplus stone to construction or residue processing without dropping it around the miner.

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
- **Growth:** Choose deployment speed or biomass economy. Add compartments for a Repair Dock and spare feed. A larger shelter consumes more packed parts and requires more clear ground; no End material is needed for the first End expedition.

### T4-19 — Cold Lobe
- **Does:** Refrigerates an attached specimen, food or culture store. Preserves a vulnerable batch while the player improves the laboratory, rather than forcing immediate processing.
- **Input → output:** Electricity + cooling fluid → a cold host compartment and warmed fluid. Stopping cooling preserves the items but resumes their disclosed spoilage clock.
- **Create:** 1 Heat-Exchange Gill + 1 packed ice + 1 Charge Sac.
- **Growth:** Choose cooling rate or electrical economy. Add insulated Membrane Windows for a larger cold room. Cooling does not restore already-spoiled specimens or manufacture new genome information.

### T4-20 — Mnemonic Vessel
- **Does:** Stores ordinary Minecraft experience separately from typed organ/equipment counters. Gives an enchanting corner a place to bank experience without loose XP orbs.
- **Input → output:** Experience deliberately deposited by a player → stored experience points → deliberate withdrawal or an attached permitted enchanting process.
- **Create:** 1 Organ Bud + 1 enchanting table + 1 amethyst shard + 2 Membrane Sheets.
- **Growth:** Join Mnemonic Vessels for capacity. Survey Gel treatment improves transfer rate. It cannot turn furnace maturity into player XP; bio-armor enchantment support remains a separate choice, not a consequence of owning this block.

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

### T6-02 — Arrival Chamber
- **Does:** Defines a protected, inspectable landing volume for a Transit Maw. Keeps the arrival reserve separate from the workshop's production budget.
- **Input → output:** Arrival reservation + local biomass/electrical reserve → a clear landing and admission acknowledgment.
- **Create:** 1 Void Tether + 1 Biomass Bladder + 2 passenger-conditioned Spatial Membranes; assemble a 3×3 floor with at least three clear blocks above it.
- **Growth:** Add separate berths for higher traffic or mounts. A Repair Dock can serve the exit, but a full repair queue must not block arrivals. Departure is refused when every berth is occupied.

### T6-03 — Freight Gullet
- **Does:** Transfers reserved cargo between two built freight endpoints. Passenger throughput is not an unlimited bulk-storage connection.
- **Input → output:** A committed item batch or sealed fluid batch + biomass/electricity → the same batch in the receiver's reserved buffer.
- **Create:** 1 Vascular Junction + 2 cargo-conditioned Spatial Membranes + 1 Item Capsule + 1 Fluid Cyst.
- **Growth:** Choose dispatch speed or energy economy. Additional Cargo Locks increase batch size. Both endpoints must be available; a sleeping destination leaves the cargo accounted for locally rather than continuing invisible remote production.

### T6-04 — Cargo Lock
- **Does:** A physical staging buffer attached to a Freight Gullet. Separates “requested,” “packed” and “received” cargo so another machine cannot consume a half-prepared delivery.
- **Input → output:** Reserved items/fluid containers → a sealed dispatch batch, or the unchanged contents on cancellation.
- **Create:** 1 Item Capsule + 1 Sphincter Door + 1 Sensor Polyp + 1 cargo-conditioned Spatial Membrane.
- **Growth:** Add compartments for several independent destinations. A fluid compartment needs a compatible lined cyst; incompatible cargo does not share one invisible universal tank.

### T6-05 — Storage Cortex
- **Does:** Presents connected Item Capsules and tanks as one searchable local store, while the physical cells still hold the contents.
- **Input → output:** Player queries, deposits and withdrawals + electricity → indexed access and routed items through connected mouths/veins.
- **Create:** 1 Synaptic Console + 1 Request Cortex + 1 precision-conditioned Spatial Membrane.
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
- **Does:** A shared docking point at a station, mine entrance or workshop. Gives priority to an explicitly selected service: suit fuel, repair, cargo unloading or tool exchange.
- **Input → output:** A docked wearer/set + attached reserves and service organs → the selected refuel, repair or inventory transfer.
- **Create:** 1 Repair Dock + 1 Item Capsule + 1 Filter Valve + 1 Nerve Tissue segment.
- **Growth:** Add another service face for a second user or machine. The pedestal delegates repairs to its actual dock and stock to its capsules; it is not a new all-purpose machine generating services from nothing.

### T6-10 — Reclamation Mouth
- **Does:** Recovers a selected biological assembly for relocation. Removes it in a visible order after draining resources into reserved containers.
- **Input → output:** Player-approved structure + empty storage + power → recovered blocks, organ cores with their histories, and contained fluids.
- **Create:** 1 Structure Grower + 1 Output Mouth + 1 Cargo Lock.
- **Growth:** Choose recovery speed or energy economy. Add a larger cargo bay for complete rooms. A missing container, occupied chamber or unknown inventory stops removal; it does not erase the difficult part to finish the job.

## T7 — Compound organs: configurable large structures and specialized power

The player designs assemblies with separate working, supply and control parts. More sockets allow more combinations, but their feed, cooling and trait limits still have to be met.

### T7-01 — Synthesis Heart
- **Does:** Builds large, compound organ cores from several compatible mature organs. Keeps each contributing organ's useful specialization visible in the resulting assembly.
- **Input → output:** Listed donor organs + Genetic Stock + thermal/spatial materials + biomass/electricity → one recipe-defined compound core, with donor histories assigned rather than copied.
- **Create:** 1 Mutation Chamber core + 1 Trait Regulator + 2 precision-conditioned Spatial Membranes. Build a 5×5 ribbed treatment body with separate donor, reagent and recovery bays.
- **Growth:** Choose assembly speed or energy economy. Add Anatomy Sockets for larger donor sets. Combining incompatible organs shows the conflict before consuming anything; it never means “put any machines together and gain all outputs.”

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
- **Create:** 1 Reclamation Mouth + 1 Memory Gland + 1 Levitation Chamber attachment.
- **Growth:** Choose transplant speed or biomass economy. Extra Anatomy Sockets handle larger cores. Transplanting is not a chance-based reroll and does not duplicate donor organs.

### T7-06 — Genome Vault
- **Does:** Stores an explicit backup of discovered genome knowledge and holds separate physical Memory Samples for replacement equipment. Valuable before risky expeditions and large refits.
- **Input → output:** Authorized bank records → a knowledge backup; deposited Memory Samples → retained typed learning samples. Knowledge copies do not create Genetic Stock or duplicate stored experience samples.
- **Create:** 1 DNA Bank + 4 Archive Lobes + 2 obsidian + 1 precision-conditioned Spatial Membrane.
- **Growth:** Add Archive Lobes for capacity. A Relay Ganglion synchronizes selected knowledge with another owned vault; physical samples still require actual transport.

### T7-07 — Interceptor Nursery [optional helper branch]
- **Does:** A nursery extension for short-lived combat organisms. One build specializes in ground interception, another in aerial pursuit; ordinary item logistics do not require either.
- **Input → output:** Biomass + real growth stock/ammunition + selected combat Genetic Stock → a limited deployment of temporary defenders with no profitable death loot.
- **Create:** 1 Brood Nursery + 1 Aerocyte Bloom or Spine Sentry + 2 Thermal Linings + 1 Catalyst Lobe; provide separate deployment berths.
- **Growth:** Choose growth speed or feed economy. Extra valid berths increase the permitted squad within its station limit; support loss or lifespan expiry recalls/ends it rather than leaving permanent uncontrolled creatures.

### T7-08 — Siege Blossom
- **Does:** A large stationary weapon for a prepared defensive position. Requires the enemy to enter its covered area; it cannot attack a boss anywhere in the dimension.
- **Input → output:** Grown spine ammunition + biomass + charged electricity/steam → a slow, powerful visible shot and spent heat.
- **Create:** 1 Spine Sentry + 1 Targeting Eye + 2 Steam Muscles + 1 Charge Sac; build a 5×5 rooted support body with a clear barrel-like corolla and cooling route.
- **Growth:** Choose reload rate or energy economy. Different genetic grafts favor armor penetration, aerial tracking or crowd suppression; installing one sacrifices another. A blocked muzzle or empty cooling path prevents firing.

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
- **Does:** Stores work captured from a changing local phase for later organ operation. Smooths an intermittent native supply; it does not generate energy from an unchanging room.
- **Input → output:** A real local phase transition + maintained collector tissue → stored phase charge, later spent by Fold machinery.
- **Create:** 1 Charge Sac + 1 Chorus Resonator + Adaptive Gel + 2 Anchor Roots.
- **Growth:** Choose capture rate or retention. Join Phase Accumulator cells to store a longer reserve. Forcing a transition electrically costs more than the charge recovered, preventing a self-powering loop.

### T8-07 — Foreign Specimen Cocoon
- **Does:** Holds a dangerous local specimen in the conditions needed to study it. Lets the player research before choosing which native organisms to cultivate.
- **Input → output:** A captured permissible specimen + habitat supplies + precision sampling → source-labeled samples while maintaining containment.
- **Create:** 1 Live Sampling Cradle + 2 adaptive membranes + 1 Habitat Lung; enclose a clear berth in Phase Shelter Skin.
- **Growth:** Choose sampling speed or lower specimen stress/damage. A larger cocoon accepts larger supported creatures; escape-resistant construction is still required, and the campaign boss is not an ordinary farm specimen.

### T8-08 — Retuning Root
- **Does:** Changes a small assigned chamber's biological phase on demand. Lets the player pay for steady production instead of waiting for the environment.
- **Input → output:** Electricity + phase charge + biomass → a local feeding or hardening interval, followed by recovery.
- **Create:** 1 Expression Switch + 1 Phase Accumulator + Adaptive Gel + 1 Anchor Root.
- **Growth:** Choose retuning speed or charge economy. Additional roots serve larger completed enclosures. Retuning a workshop does not alter the whole dimension or its natural-resource deposits.

### T8-09 — Quarantine Gate
- **Does:** A controlled station between the native habitat and the return route. Keeps incompatible cargo, active specimens and contaminated containers from entering an ordinary workshop by mistake.
- **Input → output:** Declared cargo + inspection power → permitted packed cargo, or a clearly held batch awaiting treatment.
- **Create:** 1 Cargo Lock + 1 Selective Membrane + 1 Ion Separator + Adaptive Gel.
- **Growth:** Add specimen, fluid and equipment lanes with their own treatment recipes. It does not erase unknown cargo to declare it clean; the player can recover or redirect a refused batch.

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
