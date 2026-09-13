# Armor evolution

Per-piece development, permanent branches and metabolism.
Frame statistics and mutation values are proposed balance.
Preparation recipes: [Items](ITEM_CATALOG.md). Service structures: [Blocks](BLOCK_CATALOG.md).

## 1. Four independent pieces

| State | Meaning |
|---|---|
| Fusion level | This piece's position in its permanent material tree; increased by fusion, not practice. |
| Activity counters | This piece's eligible learned activities. Each activity has one owning piece. |
| Learning capacity | One limit on the sum of this piece's counters. |
| Installed mutations | Permanent branches; I→II→III grows the same branch. |
| Anatomy slots | Space occupied by installed mutations, independent of fuel and learning capacity. |
| Biomass reserve | Actual stored fuel belonging to this piece. |
| Tissue durability | Condition healed with fuel/services; zero means collapsed equipment. |

No suit-wide learning budget, general equipment XP bar or freely allocated growth
points. A full suit can share fuel/metabolic service, not counters or fusion levels.
Each item preserves its state when removed, moved or traded; death-item recovery
and wearer adaptation require Q-007.

## 2. Shared capacity and partial counter reduction

**Invariant:** `sum(counters) <= learning_capacity`, independently on each piece.
Fusion raises capacity without changing earned counter values.
Counters are nonnegative. At capacity none can increase. A credit that would cross
capacity can contribute only the remaining room; fractional credit policy is Q-002.
Activity counters **cannot be paused** by a player setting. No points accumulate
in a hidden reserve while capped or while the piece is not worn.

**Paid partial counter reduction:**

- Select one piece and one activity counter.
- One ritual removes its fixed reduction amount and consumes its recipe.
- Free exactly the removed amount of shared capacity; never make a counter negative.
- Repeat and pay again to remove more. Do not pause other counters or exchange branches.
- Fusion level, material ancestry and installed branch identities are retained.
- Reduction cost, minimum-count handling, buff consequences and point storage: Q-003.

## 3. Activity ownership and buffs

Proposed activity mapping. One event credits only its assigned piece;
burning and hot-floor damage must be classified separately. Rates and buffs: Q-002.

| ID | Piece | Eligible activity to specify | Buff to quantify in Q-002 |
|---|---|---|---|
| H-F | Helmet | Examine/sample a living target | Examination time or fuel cost |
| H-D | Helmet | Active travel in darkness | Low-light visibility |
| H-W | Helmet | Consume breathing reserve with head submerged | Breath endurance |
| H-C | Helmet | Survive an externally caused harmful chemical effect | Supported effect duration |
| H-S | Helmet | Survive loss of air, classified as drowning or solid suffocation | Suffocation tolerance |
| C-G | Chest | Receive a direct hostile hit | Physical hit mitigation |
| C-T | Chest | Burn, excluding classified hot-floor contact | Burning tolerance |
| C-R | Chest | Heal actual wear on the chestpiece | Chest tissue-healing economy |
| C-D | Chest | Actual death or lethal event, pending Q-004 | Single-use rescue |
| L-E | Leggings | Self-propelled grounded sprint | Sprint speed |
| L-W | Leggings | Self-propelled swimming | Swim speed |
| L-B | Leggings | Travel through eligible solid terrain while Burrowing | Burrowing fuel economy |
| L-S | Leggings | Complete manual tool work | Manual working speed |
| L-C | Leggings | Move while crouched | Crouched speed |
| L-F | Leggings | Glide/fly with incorporated wings | Flight steering or fuel economy |
| B-F | Boots | Traverse uneven ground without a jump/climb event | Step handling |
| B-A | Boots | Climb a contacted ladder, vine or supported wall | Climbing speed |
| B-L | Boots | Land after a fall | Fall-impact reduction |
| B-J | Boots | Jump during traversal | Jump height |
| B-H | Boots | Stand/walk on a damaging hot surface | Contact-heat tolerance |
| B-P | Boots | Complete an eligible short displacement | Blink cost or cooldown |

Practice improves use of anatomy; it does not create gills or wings.
Mutation learning prerequisites: Q-006.

### Death rescue

Death learning grants a single-use totem-like rescue. Trigger, cost and sleep/reset
behavior: Q-004.

## 4. Fusion tree

```text
G0 Dormant flesh → G1 Living frame
  ├─ G2 Iron ribs
  │    ├─ G3 Diamond carapace → G4 Netherite lamellae
  │    └─ G3 Obsidian scutes → G4 Netherite-bonded scutes
  └─ G2 Auric lattice → G3 Diamond tendon
                           ├─ G4 Netherite mesh
                           └─ G4 Spatial weave
```

No backward fusion, sibling exchange or reconverging frame is available. A rigid veteran cannot become a flexible specialist: grow another piece. Compatible grafts can be added within remaining anatomy; committed exclusive traits cannot be exchanged. Dye, enabled abilities and fuel priorities remain settings.

Iron supports plating; gold supports flexible anatomy. Obsidian is a heavy alternative within the iron lineage, with its own terminal frame. The auric lineage trades protection for anatomy and higher learning ceilings. Fold adaptations never erase these choices.

### Frame statistics

Protection and durability columns list helmet / chest / leggings / boots.

| Frame | Fusion level | Protection | Durability | Toughness per piece | Anatomy slots |
|---|---|---|---|---|---|
| Dormant flesh | G0 | 0 / 1 / 1 / 0 | 33 / 48 / 45 / 39 | 0 | 0 |
| Living frame | G1 | 1 / 2 / 2 / 1 | 88 / 128 / 120 / 104 | 0 | 4 |
| Iron ribs | G2 | 2 / 5 / 4 / 2 | 176 / 256 / 240 / 208 | 0.25 | 6 |
| Auric lattice | G2 | 2 / 3 / 3 / 1 | 132 / 192 / 180 / 156 | 0 | 8 |
| Diamond carapace | G3 | 3 / 7 / 5 / 3 | 330 / 480 / 450 / 390 | 1.5 | 8 |
| Diamond tendon | G3 | 2 / 5 / 4 / 2 | 220 / 320 / 300 / 260 | 0.5 | 11 |
| Obsidian scutes | G3 | 3 / 7 / 6 / 3 | 385 / 560 / 525 / 455 | 2 | 6 |
| Netherite lamellae | G4 | 3 / 8 / 6 / 3 | 440 / 640 / 600 / 520 | 2 | 10 |
| Netherite-bonded scutes | G4 | 3 / 8 / 6 / 3 | 495 / 720 / 675 / 585 | 2.25 | 8 |
| Netherite mesh | G4 | 2 / 6 / 5 / 2 | 330 / 480 / 450 / 390 | 1 | 13 |
| Spatial weave | G4 | 2 / 5 / 4 / 2 | 264 / 384 / 360 / 312 | 0.5 | 14 |

Frame effects: Obsidian Scutes reduce blast knockback
by 10% and grounded movement speed by 2% per piece (set limits 40% and 8%).
Netherite Lamellae reduce hostile knockback by 5% per piece (20% per set).
Neither effect grants lava or fire immunity.

Shared capacities: Q-001. Fusion recipes and amethyst placement: Q-005.

### Turning minerals into living fusion materials

Mineral Gizzard → Washing Kidney → culture/thermal/precision preparation →
bioactive fusion medium. Raw ingots and gems cannot fuse directly.
Fusion consumes prepared media and biomass; cost increases exponentially with
level. Recipes: Q-005. Minecraft XP: Q-008.

## 5. Mutation installation

Each branch occupies its stated slots on the piece receiving it. A higher
expression replaces the preceding expression's cost/effect; it does not stack
I, II and III. Forward descendants and the explicit exclusions below are permanent.
Disabled anatomy still occupies slots. Settings may turn an ability off, **not pause
an activity counter** or replace a branch.

Access prerequisites: Q-006.

Prepare a target/expression-specific Mutation Graft, then install it on the existing
piece. [Item Catalog](ITEM_CATALOG.md#mutation-grafts) owns the recipe quantities.
Host-grown introductory grafts have a Bowl/Cradle route; creature-derived grafts
require complete matching genomes and physical stock. Wings incorporate a real
Elytra once. Automatic player feeding and powered flight are late progression.

## 6. Mutations available on multiple pieces

These occupy anatomy on **each piece that receives them**. Installing a lining on the chest does not install it on the other three pieces.

| ID; mutation | Slots I / II / III | DNA; signature ingredient | Effect and operating cost |
|---|---|---|---|
| M1 Healing Membrane | 1 / 1 / 2 | Host; Membrane Sheet | Restore 1 / 3 / 6 durability every 10 s to this piece. Costs 2 BU per durability actually restored; reserves load 1 / 2 / 4 while healing. Pauses during starvation; does not restore player health. |
| M2 Thermal Lining | 2 / 3 / 4 | Magma cube; Thermal Lining | One quarter of a sealed thermal suit. Full-set effects are below; individual incomplete linings confer no player fire immunity. |
| M3 Pelagic Lining | 2 / 3 / 4 | Turtle; Membrane Sheet | One quarter of a coordinated aquatic suit. Enables the underwater handling and efficiency benefits below, not free breathing by itself. |
| M4 Potion Capillary | 1 / 2 / 3 | Witch; Potion Vesicle | Hold 1 / 2 / 3 prepared potion doses on this piece. Dispense manually or against one configured trigger. One actual dose is consumed per activation; 5 BU and load 3 for 1 s. |

M2 and M3 are mutually exclusive on the same piece. A two-thermal/two-pelagic suit completes neither system. Mixing their ranks is allowed, but a complete system works at its **lowest installed rank**. Higher-rank parts are investments toward the next complete set, not an excuse to count four bonuses on one chest.

M4 stores normal-duration, normal-strength doses supplied by the Potion Infuser. It neither extends their duration nor permits normally incompatible effects to stack. Changing armor cannot reset a dose already consumed. Across all worn pieces and Curios, automatic dispensing has one shared 10-second cooldown. Removing an effect with milk does not refund its dose.

### Thermal suit operation

Four M2 linings, a complete living suit and available metabolism enable Thermal Mode:

| Effective rank | Protection while fueled | Biomass cost | Load | Limitation |
|---|---|---|---|---|
| I | Prevent ordinary burning/fire damage; survive up to 5 s of continuous lava immersion | 2 BU/s in fire; 8 BU/s in lava | 4 in fire; 8 in lava | After the lava allowance, only ordinary armor/potions remain. Recover allowance after 30 s completely out of lava. |
| II | Same fire protection; lava allowance 30 s | 2 BU/s in fire; 6 BU/s in lava | 4 / 8 | Same 30 s recovery. Enough for crossings and emergency work, not an unlimited lava expedition. |
| III | Sustained lava work while fueled | 2 BU/s in fire; 5 BU/s in lava | 4 / 8 | Does not protect against suffocation, explosions, Wither, hostile melee or every future thermal hazard. |

Standing in safety costs nothing for an inactive lining. A clear remaining-lava-time warning appears before I/II expires. Leaving and instantly re-entering lava does not refill the allowance. Thermal Exchange can improve fuel endurance, but cannot extend the I/II time allowance. Thermal cost belongs to the chest for fuel accounting.

A Fire Resistance potion remains useful as an independent emergency measure. Armor stops spending fuel to prevent damage that the active potion already prevents; it does not cancel the potion to force its own mechanic.

### Aquatic suit operation

Four M3 linings and a complete living suit enable Pelagic Mode:

| Effective rank | Whole-body benefit | Mode cost | Load |
|---|---|---|---|
| I | Reduce Gill Bellows operating cost by 15%; deliberate stop/hover in water without drifting from the suit's own thrust | 0.2 BU/s while actively swimming or using gills | 1 |
| II | Gill cost −25%; maintain ordinary directional tool use during powered swimming rather than forcing the player to stop | 0.3 BU/s during aquatic work | 2 |
| III | Gill cost −35%; raise powered swimming bonus ceiling from +75% to +100% | 0.5 BU/s during aquatic work | 3 |

These are proposed suit movement modes, not claims that vanilla has a pressure simulation. No new universal depth-damage mechanic is introduced here. Gill Bellows supply air; Aquatic Eyes supply visibility; tool choice and Working Tendons supply excavation ability. A sealed suit with none of these is not automatically a complete ocean expedition kit.

Pelagic Mode cost belongs to the chest. Its locomotion benefits stop when it cannot be powered; ordinary swimming still works. Breathing has priority over propulsion.

## 7. Helmet branches

The helmet develops **observation, illumination, aquatic vision, chemical defense, thermal sensing and underground navigation**. It does not carry every traversal ability itself.

In the fuel column, `0.2 / 1` means 0.2 BU per second and load 1 while active. Where a value is the same at every rank, it is written once.

| ID; branch | Slots I / II / III | DNA; signature ingredient | Effect I / II / III | Fuel and load |
|---|---|---|---|---|
| H1 Field Lens | 1 / 2 / 3 | Cow; Membrane Sheet | Examine a visible target from 12 / 20 / 28 blocks; show its species, sampling coverage and currently observable condition. Does not disclose unseen inventory or an entire mod's private mob state. | 1 BU per examination; load 1 for its 1 s focus |
| H2 Lantern Gland | 1 / 2 / 3 | Host; Lumen Secretion | A directed biological light cone reaching 6 / 9 / 12 blocks. Illuminates nearby tunnel surfaces; never makes solid terrain see-through by itself. | 0.1 / 0.2 / 0.3 BU/s; load 1 |
| H3 Nocturnal Membrane | 1 / 2 / 3 | Bat; Membrane Sheet | Low-light vision within 8 / 12 / 16 blocks. Preserves silhouette and movement visibility, with reduced color information. Does not light the area for other players. | 0.1 / 0.15 / 0.2 BU/s; load 1 / 1 / 2 |
| H4 Aquatic Eyes | 1 / 2 / 3 | Squid; Membrane Sheet | Underwater clarity target of 8 / 16 / 24 blocks, limited by actual terrain visibility. No seeing ores through walls. | 0.2 / 0.4 / 0.6 BU/s; load 1 / 2 / 2 |
| H5 Toxin Filter | 1 / 2 / 3 | Cave spider; Antitoxin Serum | Remove up to 4 / 8 / 12 s of Poison per activation, with a 20 / 15 / 10 s cooldown. Excess Poison remains. Milk containers return during serum preparation. | 4 / 6 / 8 BU per activation; load 3 for 1 s |
| H6 Thermal Sight | 2 / 3 / 4 | Blaze; Thermal Lining | Distinguish exposed hot targets and hot surfaces through smoke/fire overlays within 12 / 20 / 28 blocks. Cannot look through opaque rock. | 0.2 / 0.3 / 0.5 BU/s; load 2 |
| H7 Scent Pits | 2 / 3 / 4 | Zombie; Scent Concentrate | Indicate direction and rough distance band of nearby living creatures within 6 / 10 / 14 blocks. Works around thin obstacles, but gives no exact wall-through model or species-perfect ore-like outline. | 1 / 2 / 3 BU per pulse, every 4 s; load 2 |
| H8 Stone Sense | 3 / 4 / 5 | Enderman; Diamond-Fiber Matrix | Burrowing adaptation: distinguish passable rock and nearby air pockets within 3 / 5 / 7 blocks while inside ground. Reveals cavities, not ore identities or block inventories. | Included in the coordinated Burrowing cost; alone, 1 BU per deliberate pulse, load 2 |
| H9 Wither Sieve | 2 / 3 / 4 | Wither skeleton; Thermal Lining | Remove up to 4 / 8 / 12 s of Wither, cooldown 20 s. Does not prevent the attack, cancel boss damage or permanently immunize the wearer. | 10 / 15 / 20 BU per activation; load 5 for 1 s |
| H10 Colony Reader | 1 / 2 / 3 | Bee; Auric Myelin | Show selected organ status and recent flow, and issue an already-authorized helper order, within 12 / 20 / 28 blocks. Higher rank improves reach, not drone population. | 1 BU per query/order; load 1 for 1 s |

H2 and H3 can coexist. H3, H4 and H6 are installed anatomy, not exchangeable branches; only one visual display mode is active at a time. H7 uses a directional cue. H10 cannot read unloaded chunks or bypass ownership. Counter-buff interactions and reach ceilings are Q-002/Q-009.

Stone Sense alone is useful for examining an adjacent underground cavity, but does not allow passage. Its coordinated Burrowing function requires the other three adaptations below.

## 8. Chest branches

The chest develops **fuel storage, metabolic throughput, healing, impact defense, environmental exchange and whole-body transport**. This is where apparently compatible abilities compete for operating capacity.

| ID; branch | Slots I / II / III | DNA; signature ingredient | Effect I / II / III | Fuel and load |
|---|---|---|---|---|
| C1 Reservoir | 1 / 2 / 3 | Host; Membrane Sheet | Add 100 / 600 / 1,500 BU capacity to the chest's base 40. Capacity is not fuel generation. | No operating cost/load |
| C2 Pump Heart | 1 / 2 / 3 | Host; Auric Myelin | Add 4 / 7 / 10 to complete-suit output. Does not multiply reserve capacity. | 0.1 / 0.2 / 0.4 BU/s only while demand exceeds the unmodified output; no additional load |
| C3 Regrowth Lobe | 2 / 3 / 4 | Axolotl; Membrane Sheet | Restore 1 health point every 10 / 6 / 4 s, starting 5 s after the last hostile hit. Healing stops at full health and is not damage immunity. | 10 BU per health point; load 4 while healing |
| C4 Impact Bladder | 2 / 3 / 4 | Slime; Elastic Gel | Reduce residual physical hit damage by 10% / 15% / 20%, at most 2 / 3 / 4 health points prevented per hit. | 2 / 3 / 4 BU per health point prevented; load 4 / 6 / 8 while armed |
| C5 Thermal Exchange | 2 / 3 / 4 | Magma cube; Thermal Lining | Reduce Thermal Mode biomass cost by 10% / 20% / 30%. No conversion of environmental heat into free suit biomass. | No additional BU cost; load 2 / 3 / 4 while exchanging |
| C6 Gill Bellows | 2 / 3 / 4 | Turtle; Membrane Sheet | Supply up to 10 / 30 / 60 s of breathing per pulse. Refills only missing air; unused supply is not banked beyond the current air capacity. | Full pulse costs 3 / 6 / 10 BU, prorated for missing air; load 2 / 3 / 4 while operating |
| C7 Blast Baffles | 2 / 3 / 4 | Creeper; Vitreous Scute | Reduce residual explosion damage by 10% / 15% / 20%, at most 3 / 5 / 7 health points per explosion. | 3 BU per health point prevented; load 4 / 6 / 8 while armed |
| C8 Burrow Mantle | 3 / 4 / 5 | Enderman; Diamond-Fiber Matrix | Whole-body passage support. Together with H8/L7/B8 gives a 10 / 20 / 35 s safe stopping window. | Whole Burrowing mode: 24 / 20 / 18 BU/s; load 12 / 10 / 8 |
| C9 Feeding Lobe | 1 / 2 / 3 | Host; Nutrient Mash | Automatically consume permitted real food from 1 / 2 / 3 selected inventory slots when hunger is at or below 14 / 16 / 18 points. At most one food per 10 s; normal food effects apply. | 1 BU per food; load 1 for 1 s. No biomass-to-food conversion. |
| C10 Service Tendril | 1 / 2 / 3 | Bee; Membrane Sheet | Transfer stored biomass at 2 / 4 / 6 BU/s to one explicitly selected compatible tool or helper within 2 blocks. Stops when its tank is full. | Actual transferred BU; load 2; transfers give no activity credit |
| C11 Elytral Wings | 3 / 4 / 5 | Phantom; precision-conditioned Spatial Membrane; actual Elytra at I only | I: controlled descending glide. II: powered forward flight and climbing. III: controlled takeoff and hover. Driven speed ceiling 6 / 8 / 10 m/s. | 2 / 6 / 10 BU/s; load 5 / 8 / 12 |
| C12 Nutrient Intake | 1 / 2 / 3 | Host; Capillary Gel | Refill from 1 / 2 / 3 selected sealed biomass containers at up to 2 / 5 / 10 BU/s, stopping at capacity and retaining partial containers. | Transfers actual biomass; no output increase or practice credit. |
| C13 Digestive Crop | 2 / 3 / 4 | Zombie; Digestive Enzyme | Automatically digest permitted biological materials from 1 / 2 / 3 selected inventory slots into chest biomass. Proposed net yield: 60% / 70% / 80% of the material's declared basic stationary digestion reference; delivery capped at 1 / 2 / 4 BU/s. | Load 1 / 2 / 2 while digesting; no starting biomass required. Consumes real feed, not player health or hunger. Conversion itself grants no practice. |

C4 and C7 are alternative uses of the same inflatable defensive cavity and cannot be installed together. C8 and C11 are incompatible body plans and cannot be installed together. Installed anatomy must fit the selected frame. A large fuel tank never removes slot or metabolic limits.

C4/C7 apply after ordinary armor calculations. The sum of additional physical/explosion reduction from living-armor mutations is capped at 20% of the residual damage, before the row's per-hit limit. Potion and enchantment stacking: Q-008.

C3 is **auto-healing**; M1 is armor self-healing; C9 **feeds the player**; C12 **transfers prepared biomass**; C13 **digests carried biological materials into biomass**. C13 is a separate permanent branch, not a C12 upgrade or a requirement for container intake. Coexistence follows the current anatomy/load rules; no new mutual exclusion is assumed. No health-to-biomass conversion exists. Healing hunger injury grants no activity points. Food/feed filters default empty: the player chooses supplies rather than losing valuable items silently.

C6 stores its expanded air allowance on the worn piece and draws from it while submerged. Removing the piece removes access to that allowance, without refilling it. Ordinary water-breathing effects are respected: the suit does not spend fuel while one is already supplying breath.

C11 requires a complete suit and permanently excludes C8. The actual Elytra is incorporated and cannot be recovered while retaining wings. It does not duplicate the Elytra's durability or enchantment effects; enchantment migration remains part of the open enchantment decision.

Without fuel, powered flight stops. An intact incorporated wing retains an ordinary descending glide, never hover or climb. Collapsed wings provide no promised fall rescue. Warnings show landing reserves; flight never force-loads chunks.

### Digestive Crop: a field supply, not a pocket factory

The player configures permitted feed slots and a minimum retained count for each material. Proposed first feeds are rotten flesh, wheat and kelp. Each needs an explicit armor-digestion recipe with a net BU yield; an “organic” tag alone cannot authorize consuming DNA samples, grafts, organs, equipment, containers or arbitrary modded items. Potion effects and the food's player-nutrition value are not also awarded when it becomes armor fuel.

When C9 and C13 share a selected slot, a due player-feeding action and its food reserve take priority. Digestion may consume only the permitted remainder after rechecking that slot; the same item is never eaten and digested twice. The player can instead keep separate meal and armor-feed slots.

C13 has no generic Metabolic Work counter. Its fusion-level and any learning requirements are Q-006; its input yields are Q-010. Filling, digestion and fuel export do not themselves award activity points.

One chest processes at most one item at a time. Before consuming it, reserve room for the entire recipe yield in the chest; a recipe larger than available capacity waits. Deliver that reserved amount gradually at the rank's rate. Existing transfers must respect this reservation. Pausing, unequipping, death or reload retains only the remaining current batch on the same piece; it never returns both feed and produced fuel. No offline digestion, extra hidden tank or queued stack of meals. The HUD distinguishes available fuel from digestion still in progress.

Container intake trades expedition packing for predictable transfer with no conversion loss. Digestion uses found/farmed feed but takes more anatomy and time, returns less biomass per material than base processing, and may compete for metabolic output. At these rates, C13's maximum 4 BU/s is below C8's minimum 18 BU/s; carried feed alone cannot sustain Burrowing indefinitely. Manual and tissue refueling remain available on either specialization. Feed yields: Q-010.

## 9. Leggings and boots branches

### Leggings: drive, endurance and work

| ID; branch | Slots I / II / III | DNA; signature ingredient | Effect I / II / III | Fuel and load |
|---|---|---|---|---|
| L1 Running Tendons | 1 / 2 / 3 | Rabbit; Membrane Sheet | +10% / 20% / 30% grounded sprint speed. | 0.5 / 1 / 2 BU/s while sprinting; load 1 / 2 / 3 |
| L2 Endurance Mesh | 1 / 2 / 3 | Host; Membrane Sheet | Reduce sprint-related hunger expenditure by 15% / 30% / 45%; does not reduce starvation damage or replace food. | 0.1 / 0.2 / 0.4 BU/s while sprinting; load 1 |
| L3 Swimming Muscles | 1 / 2 / 3 | Dolphin; Membrane Sheet | +15% / 30% / 45% self-propelled swimming speed. | 0.4 / 0.8 / 1.2 BU/s; load 2 / 3 / 4 |
| L4 Bracing Tendons | 1 / 2 / 3 | Turtle; Bone Plate | While deliberately braced on a surface and using a tool, reduce hostile knockback by 20% / 40% / 60%. Walking releases the brace. | 0.2 / 0.4 / 0.6 BU/s while braced; load 1 / 2 / 3 |
| L5 Working Tendons | 2 / 3 / 4 | Zombie; Bone Plate | +10% / 20% / 30% manual tool working speed. Does not change harvest tier, loot, ore identity, durability cost per action or machine speed. | 0.3 / 0.6 / 1 BU/s during actual tool work; load 2 / 3 / 4 |
| L6 Stalking Fibers | 1 / 2 / 3 | Fox; Membrane Sheet | +20% / 40% / 60% crouched movement speed. Does not make the wearer invisible or silence every action. | 0.1 / 0.2 / 0.3 BU/s while moving crouched; load 1 |
| L7 Burrowing Muscles | 3 / 4 / 5 | Enderman; Diamond-Fiber Matrix | Coordinated Burrowing travel speed 0.8 / 1.2 / 1.6 m/s through eligible terrain, including deliberate upward/downward movement. | Included in C8 cost/load |
| L8 Fast-Lane Tendons | 2 / 3 / 4 | Host; Auric Myelin | +40% / 70% / 100% grounded travel speed on a compatible mutated fast-lane tissue. Needs a complete living suit and Surface Key II or III. | 0.5 / 1 / 2 BU/s on the active lane; load 2 / 3 / 4 |

L1 and L8 are alternative tendon architectures and cannot be installed together. L8 does nothing on ordinary stone, so a colony commuter and a wilderness explorer prefer different leggings. L4 and L5 can cooperate, but neither lets the player mine protected blocks or harvest an ore their tool cannot harvest.

### Boots: contact, climbing, landings and displacement

| ID; branch | Slots I / II / III | DNA; signature ingredient | Effect I / II / III | Fuel and load |
|---|---|---|---|---|
| B1 Contour Sole | 1 / 2 / 3 | Host; Elastic Gel | Deliberately step onto ledges up to 1 / 1.5 / 2 blocks high when there is full body clearance. Toggle off for precise building. | 0.05 / 0.1 / 0.2 BU per assisted step; load 1 during step |
| B2 Climbing Hooks | 2 / 3 / 4 | Spider; Bone Plate | Climb a contacted wall at 0.8 / 1.2 / 1.6 m/s. No climbing air, remote walls or unlimited hanging without fuel. | 0.4 / 0.8 / 1.2 BU/s; load 2 / 3 / 4 |
| B3 Landing Bladders | 1 / 2 / 3 | Slime; Elastic Gel | Absorb an additional 3 / 6 / 10 blocks of fall distance beyond normal allowance. Remaining fall distance still causes its usual consequences. | 1 BU per additional block actually absorbed; load 3 for 1 s |
| B4 Propulsive Fins | 2 / 3 / 4 | Squid; Membrane Sheet | +20% / 35% / 50% swimming speed. | 0.3 / 0.6 / 1 BU/s while swimming; load 2 / 3 / 4 |
| B5 Spring Heel | 2 / 3 / 4 | Rabbit; Bone Plate | Charged ground jump reaching 1.5 / 2.5 / 3.5 blocks above takeoff. Requires surface contact; not a midair second jump. | 3 / 6 / 10 BU; load 3 for 1 s; cooldown 6 s |
| B6 Blink Tendon | 3 / 4 / 5 | Enderman; precision-conditioned Spatial Membrane | Relocate up to 4 / 7 / 10 blocks to a visible clear supported destination. Does not pass through opaque terrain. | 20 / 30 / 45 BU; load 6 for 1 s; cooldown 12 / 10 / 8 s |
| B7 Surface Key | 1 / 2 / 3 | Host; Spore Culture | Store 1 / 2 / 4 consented colony tissue signatures. I identifies wearer to compatible defensive tissue; II also enables Fast-Lane Tendons; III adds capacity for multiple colonies, not immunity to every hostile biomass fluid. | No operating cost/load |
| B8 Ground Anchor | 3 / 4 / 5 | Enderman; Diamond-Fiber Matrix | Coordinate body contact for Burrowing in all directions. Effective rank limits the complete burrowing system. Does not supply air or light. | Included in C8 cost/load |

B2 and B4 cannot occupy the same foot anatomy: hooks and full fins are alternatives. Contour Sole, Landing Bladders and either one can coexist if they fit. B7 grants biological recognition only; it does not grant chest access, block-breaking permission or ownership of another player's colony.

For B6, both origin and destination must be loaded and permitted. The destination needs clear body space and a supporting surface. It cannot cross a protected boundary, bypass an opaque wall, or repeatedly reset falling into unlimited flight. Failed validation consumes no charge and gives no practice. The boots own its cooldown and operating cost.

### Movement ceilings

Movement bonuses add within their category; they do not multiply one another indefinitely. These ceilings apply to the contribution of this armor system, not to unapproved changes to every other mod's movement rules:

- Ordinary grounded sprint bonus: at most **+50%** from living armor, including learned buffs.
- Prepared fast-lane bonus: at most **+100%**, only on the lane with its complete-suit prerequisites.
- Powered swimming bonus: at most **+75%**, or **+100%** with complete Pelagic III.
- Additional hostile knockback resistance: at most **80%** from frames and grafts combined. Environmental movement, explosions and scripted boss movement are not all the same effect.
- Burrowing speed: at most **1.6 m/s** in this progression. Running, lanes and swimming do not accelerate it.

Heavy-frame land penalties apply after the armor's positive movement bonus. A movement mutation failing or switching off restores ordinary movement, not a player-wide permanent disabled state.

## 10. Fuel, output and complete-suit abilities

### How the player refuels

Proposed interactions; transferring prepared biomass and digesting feed are distinct:

| Route | Player action | Source and result |
|---|---|---|
| T0 manual station | Open a Biomass Bladder, place one awakened piece in its service slot, press **Fill**, then retrieve it. | Transfer only the needed stored biomass into that same piece. No mutation, full suit or healing service required; awakening itself still returns empty armor. |
| T1 portable supply | Fill a Sealed Biomass Ampoule from storage; hold use while wearing bio armor. | Transfer its real contents to worn pieces, retaining the unused amount. One action serves the selected worn pieces; no need to remove each one. |
| T1 Fuel Papilla tissue | Mutate a mature substrate cell, supply it through adjacent biomass logistics, stand on it and deliberately start filling. | Transfer from colony supply into authorized worn pieces. Rate, operating loss and multiblock improvement belong to T1-36/Q-019. No digestion or armor healing; partial suits work. |
| **C12 Nutrient Intake** | Install the mutation and select carried sealed-container slots. | Automatically transfer already-produced biomass, within C12's rate. |
| **C13 Digestive Crop** | Install the separate mutation and select permitted feed slots/reserves. | Automatically convert real biological material over time, within C13's yield, capacity and output limits. |

For multi-piece filling, use one configured order (default chest, helmet, leggings, boots), skip full/ineligible pieces and show each reserve. Allocation is a transfer into those actual items, not an extra suit tank. Filling uses no armor biomass and grants no counters. Empty sources or full targets stop transfer without spills.

### Two operating limits, not two fuels

**Biomass reserve** determines how long the equipment can work. **Metabolic output** determines how much it can do at once. Load is an operating reservation, not another consumable or an experience counter.

Starting reserves: 10 BU each for helmet, leggings and boots;
40 BU for chest, before a Reservoir graft. Capacity contains no free fuel.
Reserve progression: Q-009.

The chest establishes complete-suit output: **G1 = 6, G2 = 10, G3 = 14, G4 = 18 load units**. Pump Heart adds its listed output. A G4 chest with Pump III supports 28 load, not unlimited simultaneous abilities. The other pieces do not contribute another three chest outputs.

With fewer than four living pieces, each worn piece can power local mutations up to load 2 from its own reserve. Ordinary protection, capacity and free passive mutations still work. This allows a Lamp or introductory Gill Bellows without a full suit, but not Burrowing, powered flight, Fast-Lane operation or a full habitat system.

A complete suit can draw from all four real reserves. The HUD shows their sum and the current chest-controlled output. When a piece is removed, its remaining fuel leaves with it; fuel is not recreated in the other pieces. Installing a larger reserve creates empty space, not biomass.

Continuous load lasts while an ability is active. A triggered ability reserves its specified load for the listed interval. If the interval overlaps another activation, both reservations count. A player cannot evade output limits by alternating two buttons within one tick. Inactive mutations retain their slots but reserve no load.

### Mutual symbiosis and hunger pain

A worn awakened piece without available biomass hurts its wearer until full mutual
symbiosis. Afterwards, unpaid biological abilities and armor healing pause without
armor hunger pain. Ordinary protection and unpowered movement remain.

Symbiosis tracking and hunger damage: Q-007.

Refuel or remove hungry equipment to stop that armor condition. Doing so inside
rock or underwater does not remove environmental danger. Q-004 separately defines
death rescue; no guaranteed rescue is implied.

### Priority and starvation

The player can arrange optional features into priority groups. Life-support priorities remain explicit and protected:

1. Current breathing, current thermal protection, or current Burrowing passage, as appropriate to the actual environment.
2. A requested emergency exit/Blink, if valid and compatible with the current state.
3. Armed defense and selected work/movement abilities.
4. Regrowth, armor healing and decorative/optional sensing.

If output is insufficient, lower-priority abilities pause before activation and the HUD names what paused. Already absorbed damage is not retrospectively applied. If biomass is insufficient, no effect is promised for which the suit cannot pay. Environmental warnings show both current reserve and estimated survival time; the player can reserve a chosen amount of fuel from optional consumption.

Examples: underwater propulsion yields to breathing; healing yields to Burrowing; an Impact Bladder cannot silently consume the last fuel reserved for lava protection. Life support cannot be made free simply by giving it top priority.

Completely worn-out armor retains its item, counters and installed anatomy as a **collapsed piece**. It contributes no protection and cannot operate mutations until healed at a dock or chamber. This prevents accidental loss of a long-lived lineage without making it useful at zero durability. A collapsed piece breaks complete-suit requirements. Death, item despawning and world hazards can still destroy the dropped item; there is no universal soulbinding promise.

### Burrowing contract

Required: four functional living pieces with **H8 Stone Sense, C8 Burrow Mantle, L7 Burrowing Muscles and B8 Ground Anchor**. All four adaptations are required; effective rank is their minimum. Their combined slot cost is paid across four items, but C8 owns the combined fuel, load and activation cooldown.

1. Aim into contacted eligible solid terrain and activate Burrowing. The suit checks the complete anatomy, remaining biomass and the immediate body envelope.
2. Nearby traversable blocks become shadowed/transparent to the burrowing player. Movement through them does not mine blocks, drop items, duplicate ore, or make a tunnel for other players.
3. The player can move forward, sideways, up or down while supported by contact with eligible solid ground. There is no air thrust. Entering an open cave restores ordinary gravity and ordinary movement.
4. Stopping inside ground starts a visible **10 / 20 / 35 second** stability countdown. Meaningful travel through terrain—at least half a block—refreshes it. Camera movement or tiny position jitter does not.
5. The suit continues paying the full operating cost while stationary inside ground. Running out of biomass or letting stability expire removes passage support. Ordinary collision/suffocation danger resumes. Blocks are not permanently replaced or destroyed.
6. There is **no persistent “burrowing disabled” flag**. After a failed or ended session, a 6-second activation cooldown applies. Once it expires, a properly equipped and fueled player can activate again even if trapped. Creative/spectator switching does not create an enduring lock.

Improving to II/III grants both more time to think and more economical travel. It does not permit unlimited stationary shelter in stone.

Bedrock, inventories, block entities, protected structures and designated unburrowable materials are excluded. Fluids are not treated as harmless passable stone. A player meeting an excluded block must turn, dig normally or retreat. No ability force-loads a destination chunk.

Stone Sense shows nearby cavities; it is not a headlamp. Bring Lantern Gland, Nocturnal Membrane or another valid source of visibility. If fuel is low, a player may choose darkness over turning off passage—but must still navigate out. Blink cannot be used from an embedded position without an independently valid clear line of sight and destination; it is not a guaranteed escape from every mountain.

### Full-set requirements at a glance

| Capability | Required armor combination | What mixed equipment loses |
|---|---|---|
| Shared reserves/output | Any four functional awakened pieces | Shared metabolism; each remaining piece works within its local limit |
| Defensive tissue recognition | Complete living suit + B7 + a consented matching tissue signature | Tissue does not treat a partial suit as fully protected |
| Fast lane | Complete living suit + L8 + B7 II or III + compatible lane | Lane boost, not ordinary walking |
| Thermal Mode | Complete suit, all four M2 | Biological fire/lava protection; independent potions still work |
| Pelagic Mode | Complete suit, all four M3 | Coordinated handling/efficiency, not ordinary swimming |
| Burrowing | H8 + C8 + L7 + B8 on a complete suit | All solid-terrain passage |
| Elytral flight | Complete suit + C11; rank controls glide/climb/hover | Powered control; intact wings retain only ordinary glide without fuel |
| Blink | B6 and enough complete-suit output for its load 6 | Blink; a low-power partial suit cannot supply it |

## 11. Persistence, display and compatibility

The item preserves frame, activity values, installed anatomy, actual biomass,
tissue condition, pigment and trims. Cooldowns survive quick swaps, dimension
changes and reconnects. Symbiosis representation and death-item loss are Q-007;
counter transfer is Q-003. No operation may duplicate the item or stored learning.

Forward fusion is transactional: validate target/parent, inputs, output and capacity
before committing. Refusal leaves the item unchanged. Cancellation cannot return
both a completed upgrade and its consumed ingredients.

Each armor tab shows fusion level, shared usage/capacity, each activity value,
next buff threshold, installed anatomy and fuel. Ritual preview shows points
removed, capacity freed, buffs lost and exact cost before confirmation. No learning
pause control or free point allocation.

The full-suit panel shows actual fuel, output/load and missing anatomy.
A movable/scalable/hideable half-transparent HUD has separately configured
critical warnings. Empty reserve is not the same state as missing anatomy.

Dye and trims are cosmetic and persist. Leave visible face/skin/joints in the
models; mutation features show lamp, gills, fins, hooks and reserves. No trim
secretly upgrades the material frame.

Curios control/reserve/potion accessories use the same resource accounting and
cooldowns, never supply a missing armor piece or extra learning capacity.
Exact accessory limits and enchantment/potion interactions are Q-008/Q-009.

Adaptive Interface and Reciprocal Control Graft are later settings-only proposals:
they never exchange armor branches or hold a second invisible set of anatomy.
Preparation: Items. Access and cost: Q-006/Q-031.

## 12. Safety and implementation boundaries

Learning uses a finite authored counter set and fixed-size event suppression,
not a log of visited coordinates or lifetime attackers. Q-002 must set event
bounds; Q-036 must set shared budgets. Charge, ritual and fusion transactions
must be tested across full outputs, simultaneous users, interruption and reload.

Visual work limits: Burrowing checks a 3×3×3 body
neighborhood; Stone Sense samples at most 64 positions per pulse and two pulses
per second; lamp reach is at most 12 blocks without repeated world light-block
placement. These still require profiling under [Performance](PERFORMANCE.md).
No scan or movement force-loads chunks.

C9/C12/C13 use selected slots only, at most three per function, with one shared
inventory pass per wearer per second. Digestion saves one
current batch, not a queue or nested inventory. Apply server-wide admission as
well as per-wearer limits before implementation.
