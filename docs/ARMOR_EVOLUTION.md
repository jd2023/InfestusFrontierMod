# Armor evolution

Design draft for discussion. The progression, recipes and numbers below are proposals, not implemented behavior or final balance. This owns armor mechanics. Permanent branches, starvation pain before full symbiosis, auto-feeding, auto-healing and actual Elytra fusion follow owner decisions. Numbers and adaptation thresholds remain proposals. [Item Catalog](ITEM_CATALOG.md) owns ingredient preparation; [Guide Tree](GUIDE_PROGRESSION_TREE.md) owns instructional prerequisites.

## 1. What the player is building

A helmet, chestpiece, leggings and boots are four separate living items. Each has its own material frame, five practice counters, earned levels, allocated growth points, installed mutations, durability and biomass. Removing, trading, repairing or evolving a piece preserves its identity and learning. There is no separate suit experience bar.

The player starts with cheap, nearly useless flesh armor. Awakening makes it alive. Ordinary use teaches it. Biological processing makes minerals suitable for fusion. Fusion makes a better body with higher learning ceilings. DNA mutations give that body particular abilities. The player chooses which abilities fit, which can operate together, and how to supply them.

A complete suit connects its pieces to a shared metabolism. It does **not** require matching materials: a plated chest, gold-thread helmet and flexible boots can cooperate. Some abilities need four living pieces; others also need matching adaptations. A good helmet remains useful when worn with ordinary armor.

### Six independent properties

| Property | Changes through | Does not provide |
|---|---|---|
| Frame grade, G1–G4 | Biological material fusion | Earned practice or researched genes |
| Protection and maximum durability | Frame; bounded durability growth | Immunity to every damage source |
| Counter ranks, R0–R5 | Relevant use while worn | Mutation materials |
| Piece level, L0–L25 | Sum of that piece's five counter ranks | A second experience currency |
| Anatomy slots | Frame selection | Metabolic output or fuel |
| Installed mutations, I–III | Genome research, materials and chamber work | Automatic activation regardless of equipment or fuel |

“G3 helmet, L8, Thermal Travel R3, Lamp II” therefore describes four different things. The interface must show them separately.

## 2. First armor: cheap enough to replace, weak enough to outgrow

Dormant armor uses the familiar helmet/chest/leggings/boots crafting silhouettes, with rotten flesh in place of the armor material: **5 / 8 / 7 / 4 rotten flesh**. It has no counters, biomass consumption or mutations. Its protection and durability are deliberately poor.

Awaken **one existing piece** in an Awakening Cradle using one Spore Culture, one Membrane Sheet and 100 mB biomass. The cradle returns the same piece as a G1 living frame, with its existing durability percentage preserved, empty counters and no installed mutations. It does not fill the piece's fuel reserve. Awakening is the only initial identity conversion; subsequent upgrades retain that identity.

At G1:

- The piece can gain its first counter ranks and spend growth points immediately.
- Four anatomy slots permit a useful introductory combination, not a complete specialist.
- Each helmet, leggings or boots holds 10 biomass units; the chest holds 40. **One biomass unit, BU, equals one mB of the mod's biomass fluid.**
- Repair is a mutation, not free regeneration. An unfed, not-yet-symbiotic piece hurts its wearer and cannot power hungry features.
- A chamber, Repair Dock or compatible biomass service connection fills the internal reserve without spilling fluid into the world.
- A small lamp, better footing, a modest reserve or slow self-repair gives early value before material fusion.

The cradle previews starvation risk before awakening. Hunger pain is automatic before full symbiosis, not an optional graft. Section 11 defines recovery and the later pain-free state.

## 3. Fusion: stronger bodies, different compromises

### Material branches

Material lineage is permanent. Every non-root frame has exactly one parent:

```text
G0 Dormant flesh → G1 Living frame
  ├─ G2 Iron ribs
  │    ├─ G3 Diamond carapace → G4 Netherite lamellae
  │    └─ G3 Obsidian scutes → G4 Netherite-bonded scutes
  └─ G2 Auric lattice → G3 Diamond tendon
                           ├─ G4 Netherite mesh
                           └─ G4 Spatial weave
```

No backward fusion, sibling exchange or reconverging frame is available. A rigid veteran cannot become a flexible specialist: grow another piece. Compatible grafts can be added within remaining anatomy; committed exclusive traits and growth allocations cannot be exchanged. Dye, enabled abilities and fuel priorities remain settings.

Iron supports plating; gold supports flexible anatomy. Obsidian is a heavy alternative within the iron lineage, with its own terminal frame. The auric lineage trades protection for anatomy and higher learning ceilings. Fold adaptations never erase these choices.

### Frame statistics

Numbers in slash-separated columns are **helmet / chest / leggings / boots**. Protection uses armor points, not hearts. Toughness is contributed by each piece. Slot and rank ceilings apply independently to each piece of that frame.

| Frame | Grade | Protection | Durability | Toughness per piece | Anatomy slots | Each counter's cap | Maximum piece level |
|---|---|---|---|---|---|---|---|
| Dormant flesh | G0 | 0 / 1 / 1 / 0 | 33 / 48 / 45 / 39 | 0 | 0 | R0 | L0 |
| Living frame | G1 | 1 / 2 / 2 / 1 | 88 / 128 / 120 / 104 | 0 | 4 | R1 | L5 |
| Iron ribs | G2 | 2 / 5 / 4 / 2 | 176 / 256 / 240 / 208 | 0.25 | 6 | R2 | L10 |
| Auric lattice | G2 | 2 / 3 / 3 / 1 | 132 / 192 / 180 / 156 | 0 | 8 | R3 | L15 |
| Diamond carapace | G3 | 3 / 7 / 5 / 3 | 330 / 480 / 450 / 390 | 1.5 | 8 | R3 | L15 |
| Diamond tendon | G3 | 2 / 5 / 4 / 2 | 220 / 320 / 300 / 260 | 0.5 | 11 | R4 | L20 |
| Obsidian scutes | G3 | 3 / 7 / 6 / 3 | 385 / 560 / 525 / 455 | 2 | 6 | R3 | L15 |
| Netherite lamellae | G4 | 3 / 8 / 6 / 3 | 440 / 640 / 600 / 520 | 2 | 10 | R4 | L20 |
| Netherite-bonded scutes | G4 | 3 / 8 / 6 / 3 | 495 / 720 / 675 / 585 | 2.25 | 8 | R4 | L20 |
| Netherite mesh | G4 | 2 / 6 / 5 / 2 | 330 / 480 / 450 / 390 | 1 | 13 | R5 | L25 |
| Spatial weave | G4 | 2 / 5 / 4 / 2 | 264 / 384 / 360 / 312 | 0.5 | 14 | R5 | L25 |

Additional frame properties:

- **Iron ribs:** inexpensive protection; no additional movement penalty.
- **Auric lattice:** installing/upgrading Pump Heart (C2), Colony Reader (H10) or Potion Capillary (M4) consumes 25% less chamber biomass. This does not discount their field operation or grant extra output.
- **Diamond carapace:** no special environmental immunity. Its value is strong protection without obsidian's restrictions.
- **Diamond tendon:** no additional movement penalty; precision processing pays for the additional space and learning ceiling.
- **Obsidian scutes:** each equipped piece reduces blast knockback by 10% and land movement speed by 2%; complete set caps are 40% and 8%. It does not cancel blast damage or grant lava immunity. Water propulsion and climbing still work, but cannot erase the frame's land-speed penalty.
- **Netherite lamellae:** each piece reduces hostile knockback by 5%, capped at 20% for the set. No automatic fire immunity for the wearer.
- **Netherite-bonded scutes:** retain obsidian's land-speed and blast-knockback tradeoff; stronger protection does not make them flexible.
- **Netherite mesh:** weaker protection than lamellae, more anatomy and a longer learning career.
- **Spatial weave:** Blink and Burrowing mutations occupy one fewer slot on that piece, minimum two. This discount never applies to other movement or environmental mutations.

Protection from frames totals at most 20 points for a complete set. Growth points cannot purchase additional armor points or toughness. Specialized defensive mutations remain limited, fueled and conditional.

### Entering a new grade

| Operation | Required history of the individual piece | Required colony capability | What the player receives |
|---|---|---|---|
| G1 → G2 | L2 | Mutation Chamber; mineral preparation and biological culture | Chosen iron or gold frame, higher caps and G2 mutation access |
| G2 Iron → G3 carapace/scutes | L6 | Nether Thermal Mantle and Thermal Nursery products | Permanent plated diamond or obsidian lineage |
| G2 Auric → G3 tendon | L6 | Above, plus Ion Separator and precision chamber attachment | Flexible diamond body |
| G3 carapace → G4 lamellae | L10 | Precision chamber, Thermal Mantle, real netherite supply | Rigid netherite body |
| G3 scutes → G4 bonded scutes | L10 | Same infrastructure and prepared bonded scute medium | Terminal heavy scutes |
| G3 tendon → G4 mesh | L10 | Precision chamber, Thermal Mantle, real netherite supply | Flexible netherite body |
| G3 tendon → G4 Spatial Weave | L10 | End Spatial Nursery and Spatial Conditioner | End-grown flexible body |

L2 requires two first ranks; the player need not spend hours in weak armor. Later prerequisites require a career, but not mastery of every activity. A boot does not need Blink practice before it can become G4. Every piece can reach L10 through its non-spatial counters.

Fusion changes the ceiling, not the stored number: a counter at 240/500 remains 240/500. If it was stopped at a lower frame ceiling, further useful practice starts contributing after the ceiling rises. Nothing is awarded retroactively for actions taken at the cap.

Forward fusion retains history and installed anatomy. Every permitted child frame has at least its parent's anatomy and learning ceilings. The preview identifies permanently excluded siblings before accepting materials.

## 4. Turning minerals into living fusion materials

Raw iron, gold, diamond, obsidian and netherite are **not valid armor infusion ingredients**. Crushing alone is insufficient: mineral must be cleaned, incorporated into a biological carrier and conditioned into its destination architecture.

Existing production organs do the work: Mineral Gizzard, Washing Kidney, Genetic Culture Vat, Thermal Mantle, Ion Separator and Mutation Chamber. New names below are material items or recipe modes, not a new machine for every ingredient.

### Common reagents

| Reagent | Recipe and equipment | Use |
|---|---|---|
| Fusion Binder, 4 portions | Culture Bowl, 60 s; later Culture Vat, 15 s: 1 Membrane Sheet + 1 Spore Culture + 100 mB biomass | Living carrier; contains no replicated mineral; the bowl route makes first G1 mutations possible before a genetics laboratory |
| Washed iron/gold granules, 1 portion | Gizzard then Washing Kidney: 1 corresponding ingot + 100 mB water | Mineral input for one dose of its fusion medium |
| Washed diamond grains, 1 portion | Same route: 1 diamond + 100 mB water | Dense or flexible diamond medium |
| Washed obsidian grains, 1 portion | Same route: 1 obsidian block + 100 mB water | Obsidian medium |
| Prepared netherite lamina, 4 portions | Thermal Mantle with precision attachment: 1 netherite ingot + 1 Fusion Binder + 200 mB biomass | Four smaller portions, not four ingots; cannot be recombined into extra metal |

Wash recipes produce the stated cleaned portion and 100 mB spent wash fluid. There is no ore multiplication in this chain. Purifying spent wash recovers water but not another mineral portion. All outputs enter inventories/tanks; a full output pauses processing.

### Fusion media

One row is one batch. Times assume an unaccelerated organ; upgrades may reduce time, not multiply its listed mineral yield.

| Medium | Batch inputs | Processing | Batch output | Frame it produces |
|---|---|---|---|---|
| Ferrocyte Paste | 1 washed iron portion + 1 Binder + 100 mB biomass | Culture Vat, 30 s | 1 dose | Iron ribs |
| Auric Myelin | 1 washed gold portion + 1 Binder + 150 mB biomass | Culture Vat, 45 s | 1 dose | Auric lattice |
| Faceted Chitin | 1 washed diamond portion + 1 Binder + 1 Thermal Lining + 200 mB biomass | Thermal Mantle conditioning, then Culture Vat; 60 s total | 1 dose | Diamond carapace |
| Diamond-Fiber Matrix | 1 washed diamond portion + 2 Binder + 1 Membrane Sheet + 250 mB biomass | Ion Separator filament mode, then Culture Vat; 90 s total | 1 dose | Diamond tendon |
| Vitreous Scute | 1 washed obsidian portion + 1 Binder + 1 Thermal Lining + 200 mB biomass | Thermal Mantle sintering, then Culture Vat; 60 s total | 1 dose | Obsidian scutes |
| Living Netherite Lamella | 1 prepared netherite lamina + 1 Binder + 1 Thermal Lining + 300 mB biomass | Precision Mutation Chamber, 90 s | 1 dose | Netherite lamellae |
| Netherite-Bonded Scute | 1 Living Netherite Lamella + 1 Vitreous Scute + 1 Binder + 200 mB biomass | Precision Mutation Chamber, 60 s | 1 dose | Netherite-bonded scutes |
| Netherite Tendon Mesh | 1 prepared netherite lamina + 2 Binder + 1 Diamond-Fiber Matrix dose + 350 mB biomass | Ion Separator weaving, then precision chamber; 120 s total | 1 dose | Netherite mesh |
| Phase-Woven Matrix | 1 precision-conditioned Spatial Membrane + 1 Auric Myelin dose + 1 Diamond-Fiber Matrix dose + 400 mB biomass | End Spatial Conditioner, 120 s | 1 dose | Spatial weave |

Thermal Lining and Spatial Membrane retain the dimension-bound production rules in the block catalog. A chestpiece may be fused at home after importing those products; producing them still requires working Nether and End installations. The first netherite frame does not require a netherite-armored operator.

### Applying a medium

| Piece | Medium doses for one forward fusion | Binder portions | Chamber biomass, G2 / G3 / G4 |
|---|---|---|---|
| Helmet | 2 | 1 | 200 / 400 / 800 mB |
| Chest | 4 | 2 | 400 / 800 / 1,600 mB |
| Leggings | 3 | 2 | 300 / 600 / 1,200 mB |
| Boots | 2 | 1 | 200 / 400 / 800 mB |

Example: an iron-rib chest needs four Ferrocyte Paste doses, two additional Binder portions and 400 mB chamber biomass, plus the existing L2 living chest. The four doses contain four real iron ingots. Preparing those doses also consumes their listed reagents. Fusion does not fill the fuel tank or repair existing wear for free: it preserves the item's durability percentage, rounded down.

A complete first netherite-lamella set consumes eleven lamina portions: three actual netherite ingots produce twelve, leaving one portion. The earlier mineral frame remains incorporated, not returned as scrap or reusable fusion doses. No recipe recovers a previous frame or returns its fusion doses.

Applying one fusion takes 30 / 60 / 120 seconds for G2 / G3 / G4. An interrupted chamber retains the original piece and reserved inputs until resumed or deliberately cancelled. Cancellation returns the unmodified piece and unconsumed inputs; it cannot return both a finished upgrade and its ingredients.

## 5. Practice counters and piece levels

Every counter has R0–R5. R0 means no threshold reached. The five numbers in each row below are cumulative thresholds for R1, R2, R3, R4 and R5—not additional costs at each rank. Frame caps may stop a counter before R5.

Each new active rank adds one piece level and one growth point. Example: helmet ranks **2 / 1 / 0 / 3 / 0** give **L6 and six points**, not six levels in every branch. A helmet can specialize through thermal and field work while its swimming counter remains zero.

Practice accrues while that piece is worn, functional and involved in the activity. Inventory armor does not learn from the player's actions. The one service exception is C-R: a dock can award repair practice for eligible chest wear previously incurred while worn. It consumes that pending wear credit as it repairs; repeatedly servicing an intact chest gives nothing. Sprint distance can contribute to travel and exertion because these teach different properties. The same damage event does not award every kind of combat practice.

### Helmet counters

| Code | Counter; one unit means | R1 / R2 / R3 / R4 / R5 | Qualification |
|---|---|---|---|
| H-F | Field study; one completed living-target examination | 8 / 24 / 64 / 160 / 320 | Use the colony's sampling/inspection tool while wearing the helmet. Same target no more than once per 10 minutes; at most one credit per 30 seconds. A completed examination costs 1 BU from helmet/suit. No mutation is required. |
| H-D | Dark travel; one horizontal or vertical meter | 100 / 500 / 2,000 / 6,000 / 15,000 | Travel through naturally dark surroundings. The helmet's own lamp does not invalidate learning; daylight and well-lit colony corridors do. |
| H-W | Submerged travel; one meter | 100 / 500 / 2,000 / 6,000 / 15,000 | Head underwater. Normal swimming, boats excluded; potions are allowed. |
| H-T | Thermal travel; one meter | 100 / 500 / 2,000 / 6,000 / 15,000 | Active Nether travel or movement while exposed to actual environmental heat. Standing in lava is not practice. |
| H-C | Chemical defense; one harmful-effect second successfully removed/prevented | 20 / 100 / 400 / 1,200 / 3,000 | Filter mutation, milk, or an appropriate curative potion used against an externally caused effect. Self-administered poison does not count. Credit is capped at 20 seconds per application. |

### Chest counters

| Code | Counter; one unit means | R1 / R2 / R3 / R4 / R5 | Qualification |
|---|---|---|---|
| C-G | Guarding; one hostile hit that wears the chest | 12 / 48 / 160 / 400 / 900 | Actual hostile attack, not hunger, suffocation, self-harm or falling. Same attacker contributes at most once per 10 seconds. |
| C-M | Metabolic work; one BU productively consumed | 100 / 1,000 / 5,000 / 20,000 / 60,000 | Work by worn mutations: movement, protection, meaningful sensing or eligible repair. A lamp counts during travel/work, not idle illumination; a scan must resolve a target. Refilling, venting, deliberate waste, starvation pain and container transfer do not count. |
| C-R | Tissue repair; one point of chest durability restored | 40 / 200 / 800 / 2,400 / 6,000 | Repair membrane or Repair Dock restoring actual wear from eligible activity. Breaking and replacing the item does not manufacture wear. |
| C-T | Thermal travel; one meter | 100 / 500 / 2,000 / 6,000 / 15,000 | Same environment rule as H-T; each worn piece has its own record. |
| C-W | Submerged work; one active underwater second | 60 / 300 / 1,200 / 3,600 / 9,000 | Swimming, mining, building or fighting with torso submerged. Standing idle does not count; ordinary air and potion-assisted work qualify. |

### Leggings counters

| Code | Counter; one unit means | R1 / R2 / R3 / R4 / R5 | Qualification |
|---|---|---|---|
| L-T | Overland travel; one meter | 250 / 1,500 / 6,000 / 18,000 / 50,000 | Self-propelled travel on ground, not a mount, cart, teleport, creative flight or conveyor. |
| L-E | Exertion; one sprinted meter | 150 / 800 / 3,000 / 9,000 / 24,000 | Actual grounded sprint movement; no progress from holding sprint against a wall. |
| L-W | Aquatic motion; one swum meter | 100 / 500 / 2,000 / 6,000 / 15,000 | Self-propelled swimming, not a water current carrying an idle player. |
| L-B | Burrowing; one meter traveled through eligible solid terrain | 50 / 250 / 1,000 / 3,000 / 8,000 | Fueled burrowing only. Open caves and repeated stationary toggles do not count. |
| L-S | Working stance; one qualifying tool operation | 32 / 160 / 640 / 1,600 / 4,000 | A block actually harvested with a tool, or a completed deliberate heavy-tool action. Automated machines and instantly broken decorative spam do not count. |

### Boots counters

| Code | Counter; one unit means | R1 / R2 / R3 / R4 / R5 | Qualification |
|---|---|---|---|
| B-F | Terrain footing; one meter over uneven ground | 150 / 800 / 3,000 / 9,000 / 24,000 | Self-propelled travel using steps, slopes or changes in ground height. Flat-floor circuits do not count. |
| B-A | Ascent; one meter of elevation gained | 30 / 150 / 600 / 1,800 / 4,800 | Walking uphill, stairs, ladders or powered wall climbing. Jumping on the spot, flight and teleportation do not count. |
| B-L | Landing control; one meter beyond the first three of a landing | 20 / 100 / 400 / 1,000 / 2,400 | A real descent ending on a surface; max 12 credits per landing and one qualifying landing per 10 seconds. Water/creative flight negating the landing gives no credit. |
| B-W | Aquatic propulsion; one swum meter | 100 / 500 / 2,000 / 6,000 / 15,000 | Same movement rule as L-W. Requires the boots worn, not a fin mutation. |
| B-P | Spatial displacement; one meter of successful displacement | 32 / 160 / 640 / 1,600 / 4,000 | Armor Blink, or an ender-pearl trip paid for by a consumed pearl. Colony gates, commands and aborted blinks give no credit. |

These are career counters, not required chores. No main progression gate requires every counter capped. Early mutations have rank-I forms that need **R0**, so a player is never required to master an ability before installing the first version that teaches it.

There is no claim that every player-built training course can be outlawed. The important limits are finite counter ceilings, no idle accrual, bounded repeated-event credit, and no material/protection reward from an infinite damage-and-repair loop. Ordinary traversal and real work remain the intended source of progress.

### Spending a level

Every point is spent on one track belonging to that piece. Allocation limits per track are **2 at G1, 4 at G2, 6 at G3, 8 at G4**. There are forty possible allocations per piece but at most twenty-five points: even a fully practiced soft frame cannot maximize everything.

| Track | Available on | Benefit per point | Eight-point ceiling |
|---|---|---|---|
| Tissue integrity | All pieces | +3% base maximum durability | +24%; no extra armor points |
| Economy | All pieces | −1.5% that piece's mutation biomass costs | −12%; does not reduce metabolic load |
| Recovery | All pieces | −2.5% local activated-ability cooldown | −20%; never shortens emergency environmental recovery timers |
| Focus | Helmet | +1 block to installed Field Lens/Scent range | +8 blocks, subject to the absolute sensing bound below |
| Light discipline | Helmet | −3% Lamp operating biomass cost | −24%; requires a Lamp |
| Reserve tissue | Chest | +50 BU usable capacity | +400 BU; requires Reservoir mutation |
| Regenerative tissue | Chest | +0.5 durability per Repair Membrane pulse | +4 per pulse; pay for every point actually repaired |
| Stride | Leggings | +0.5% grounded sprint speed | +4%, subject to total speed cap |
| Steering | Leggings | +2% powered swim/burrow turning response | +16%; no increase in top speed |
| Balance | Boots | +0.25 block of landing allowance | +2 blocks; cannot cancel arbitrary falls |
| Traction | Boots | +2% acceleration toward permitted movement speed | +16%; no increase in top speed |

Fractional repair credit accumulates within that item and cannot be cashed out as materials. Discounts multiply, rather than adding to 100% free operation. For a multi-piece ability, costs are assigned to its named owning piece; another piece's Economy does not discount the same cost again. Its cooldown uses the owning piece's Recovery.

Growth allocations are permanent. The preview shows the resulting property, cap and remaining points before confirmation. Counter extraction may make an allocation inactive; it never refunds it for spending in another property.

## 6. Installing mutations

An anatomy slot is a unit of internal space, not a separate inventory item slot. A mutation using four slots can coexist with a three-slot mutation in an eight-slot frame, leaving one free. Each mutation appears at most once on a piece. A higher rank replaces its lower rank; costs and effects are not stacked across ranks.

The **Awakening Cradle can install host-grown rank-I mutations** using the recipes and times below. This is the early route for Lamp, Reservoir, Repair, Endurance and Contour Sole; it needs neither a Mutation Chamber nor a complete creature genome. Creature-derived mutations, rank-II/III growth and material fusion require the Mutation Chamber. Availability of a cradle recipe does not bypass its material ingredients or piece prerequisites.

### Three access schedules

| Schedule | Mutation I requires | Mutation II requires | Mutation III requires |
|---|---|---|---|
| Basic (B) | G1, named counter R0 | G2, named counter R2 | G3, named counter R3 |
| Advanced (A) | G3, named counter R0 | G4, named counter R2 | G4, named counter R4 |
| Spatial (S) | G4, named counter R0, End-conditioned material | G4, named counter R2 | G4, named counter R4 |

Common mutations use piece-level gates instead: I at G1/L0, II at G2/L4, III at G3/L8, unless their row specifies a later grade. No rank automatically rises when a counter does: the chamber must grow the next expression.

### Recipe rule used by the mutation tables

Every row names a DNA source and a **prepared** signature ingredient. Make a rank- and slot-specific Mutation Graft first, then apply that consumable to the existing piece. [Item Catalog](ITEM_CATALOG.md#mutation-grafts) defines preparation.

For rank I / II / III, preparation consumes **2 / 4 / 8 matching Genetic Stock**, **1 / 2 / 4 of each listed prepared signature material**, **1 / 2 / 3 Fusion Binder**, and **60 / 150 / 360 BU**, taking **12 / 24 / 48 seconds** in a Fusion Chrysalis. Installation consumes one graft and **40 / 100 / 240 BU**, taking **8 / 16 / 32 seconds** in a Mutation Chamber. Total treatment remains 100 / 250 / 600 BU and 20 / 40 / 80 seconds; signature activation has its separate listed cost.

Host-grown rank-I grafts can be prepared in the Culture Bowl using Spore Culture instead of Genetic Stock, then installed in the Awakening Cradle. Higher host ranks use the Chrysalis. Two-genome recipes need stock from each source. Complete matching genomes are required for preparation and installation, except host recipes.

II requires the same piece already at I; III requires II. Elytral Wings consume an actual Elytra **once**, during rank-I graft preparation, not again at II or III.

Full genomes are prerequisites for new abilities, not a reason to delay awakening or material fusion. Stock can be cultured after genome completion, with the catalog's feed requirements; it cannot generate metals, gems, netherite or potion doses.

Mutation effects below list **I / II / III**. “Free” means no operating fuel, not a free installation. Loads and costs are defined in section 11.

## 7. Mutations available on multiple pieces

These occupy anatomy on **each piece that receives them**. Installing a lining on the chest does not install it on the other three pieces.

| ID; mutation | Slots I / II / III | Access | DNA; signature ingredient | Effect and operating cost |
|---|---|---|---|---|
| M1 Repair Membrane | 1 / 1 / 2 | Common level gates | Host; Membrane Sheet | Restore 1 / 3 / 6 durability every 10 s to this piece. Costs 2 BU per durability actually restored; reserves load 1 / 2 / 4 while repairing. Pauses during starvation; does not restore player health. |
| M2 Thermal Lining | 2 / 3 / 4 | I: G3/L6; II: G4/L10; III: G4/L14 | Magma cube; Thermal Lining | One quarter of a sealed thermal suit. Full-set effects are below; individual incomplete linings confer no player fire immunity. |
| M3 Pelagic Lining | 2 / 3 / 4 | I: G2/L2; II: G3/L6; III: G4/L10 | Turtle; Membrane Sheet | One quarter of a coordinated aquatic suit. Enables the underwater handling and efficiency benefits below, not free breathing by itself. |
| M4 Potion Capillary | 1 / 2 / 3 | I: G2/L2; II: G3/L6; III: G4/L10; requires Potion Infuser for every install | Witch; Potion Vesicle | Hold 1 / 2 / 3 prepared potion doses on this piece. Dispense manually or against one configured trigger. One actual dose is consumed per activation; 5 BU and load 3 for 1 s. |

M2 and M3 are mutually exclusive on the same piece. A two-thermal/two-pelagic suit completes neither system. Mixing their ranks is allowed, but a complete system works at its **lowest installed rank**. Higher-rank parts are investments toward the next complete set, not an excuse to count four bonuses on one chest.

M4 stores normal-duration, normal-strength doses supplied by the Potion Infuser. It neither extends their duration nor permits normally incompatible effects to stack. Changing armor cannot reset a dose already consumed. Across all worn pieces and Curios, automatic dispensing has one shared 10-second cooldown. Removing an effect with milk does not refund its dose.

### Thermal suit operation

Four M2 linings, a complete living suit and available metabolism enable Thermal Mode:

| Effective rank | Protection while fueled | Biomass cost | Load | Limitation |
|---|---|---|---|---|
| I | Prevent ordinary burning/fire damage; survive up to 5 s of continuous lava immersion | 2 BU/s in fire; 8 BU/s in lava | 4 in fire; 8 in lava | After the lava allowance, only ordinary armor/potions remain. Recover allowance after 30 s completely out of lava. |
| II | Same fire protection; lava allowance 30 s | 2 BU/s in fire; 6 BU/s in lava | 4 / 8 | Same 30 s recovery. Enough for crossings and emergency work, not an unlimited lava expedition. |
| III | Sustained lava work while fueled | 2 BU/s in fire; 5 BU/s in lava | 4 / 8 | Does not protect against suffocation, explosions, Wither, hostile melee or every future thermal hazard. |

Standing in safety costs nothing for an inactive lining. A clear remaining-lava-time warning appears before I/II expires. Leaving and instantly re-entering lava does not refill the allowance. Thermal Exchange can improve fuel endurance, but cannot extend the I/II time allowance. Thermal cost belongs to the chest for Economy calculations.

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

## 8. Helmet branches

The helmet develops **observation, illumination, aquatic vision, chemical defense, thermal sensing and underground navigation**. It does not carry every traversal ability itself.

In the fuel column, `0.2 / 1` means 0.2 BU per second and load 1 while active. Where a value is the same at every rank, it is written once.

| ID; branch | Schedule; counter | Slots I / II / III | DNA; signature ingredient | Effect I / II / III | Fuel and load |
|---|---|---|---|---|---|
| H1 Field Lens | B; H-F | 1 / 2 / 3 | Cow; Membrane Sheet | Examine a visible target from 12 / 20 / 28 blocks; show its species, sampling coverage and currently observable condition. Does not disclose unseen inventory or an entire mod's private mob state. | 1 BU per examination; load 1 for its 1 s focus |
| H2 Lantern Gland | B; H-D | 1 / 2 / 3 | Host; Lumen Secretion | A directed biological light cone reaching 6 / 9 / 12 blocks. Illuminates nearby tunnel surfaces; never makes solid terrain see-through by itself. | 0.1 / 0.2 / 0.3 BU/s; load 1 |
| H3 Nocturnal Membrane | B; H-D | 1 / 2 / 3 | Bat; Membrane Sheet | Low-light vision within 8 / 12 / 16 blocks. Preserves silhouette and movement visibility, with reduced color information. Does not light the area for other players. | 0.1 / 0.15 / 0.2 BU/s; load 1 / 1 / 2 |
| H4 Aquatic Eyes | B; H-W | 1 / 2 / 3 | Squid; Membrane Sheet | Underwater clarity target of 8 / 16 / 24 blocks, limited by actual terrain visibility. No seeing ores through walls. | 0.2 / 0.4 / 0.6 BU/s; load 1 / 2 / 2 |
| H5 Toxin Filter | B; H-C | 1 / 2 / 3 | Cave spider; Antitoxin Serum | Remove up to 4 / 8 / 12 s of Poison per activation, with a 20 / 15 / 10 s cooldown. Excess Poison remains. Milk containers return during serum preparation. | 4 / 6 / 8 BU per activation; load 3 for 1 s |
| H6 Thermal Sight | A; H-T | 2 / 3 / 4 | Blaze; Thermal Lining | Distinguish exposed hot targets and hot surfaces through smoke/fire overlays within 12 / 20 / 28 blocks. Cannot look through opaque rock. | 0.2 / 0.3 / 0.5 BU/s; load 2 |
| H7 Scent Pits | B; H-F | 2 / 3 / 4 | Zombie; Scent Concentrate | Indicate direction and rough distance band of nearby living creatures within 6 / 10 / 14 blocks. Works around thin obstacles, but gives no exact wall-through model or species-perfect ore-like outline. | 1 / 2 / 3 BU per pulse, every 4 s; load 2 |
| H8 Stone Sense | A; H-D | 3 / 4 / 5 | Enderman; Diamond-Fiber Matrix | Burrowing adaptation: distinguish passable rock and nearby air pockets within 3 / 5 / 7 blocks while inside ground. Reveals cavities, not ore identities or block inventories. | Included in the coordinated Burrowing cost; alone, 1 BU per deliberate pulse, load 2 |
| H9 Wither Sieve | A; H-C | 2 / 3 / 4 | Wither skeleton; Thermal Lining | Remove up to 4 / 8 / 12 s of Wither, cooldown 20 s. Does not prevent the attack, cancel boss damage or permanently immunize the wearer. | 10 / 15 / 20 BU per activation; load 5 for 1 s |
| H10 Colony Reader | B; H-F | 1 / 2 / 3 | Bee; Auric Myelin | Show selected organ status and recent flow, and issue an already-authorized helper order, within 12 / 20 / 28 blocks. Higher rank improves reach, not drone population. | 1 BU per query/order; load 1 for 1 s |

H2 and H3 can coexist, but active vision modes H3, H4 and H6 are mutually exclusive: the player selects one, or permits automatic water/heat switching. H7 is a sparse directional cue, not a second rendered world. Growth Focus increases H1 and H7 reach only: absolute ceilings are **36 blocks for H1, 22 for H7**. H10 cannot reach into unloaded chunks or bypass ownership.

Stone Sense alone is useful for examining an adjacent underground cavity, but does not allow passage. Its coordinated Burrowing function requires the other three adaptations below.

## 9. Chest branches

The chest develops **fuel storage, metabolic throughput, repair, impact defense, environmental exchange and whole-body transport**. This is where apparently compatible abilities compete for operating capacity.

| ID; branch | Schedule; counter | Slots I / II / III | DNA; signature ingredient | Effect I / II / III | Fuel and load |
|---|---|---|---|---|---|
| C1 Reservoir | B; C-M | 1 / 2 / 3 | Host; Membrane Sheet | Add 100 / 600 / 1,500 BU capacity to the chest's base 40. Capacity is not fuel generation. | No operating cost/load |
| C2 Pump Heart | B; C-M | 1 / 2 / 3 | Host; Auric Myelin | Add 4 / 7 / 10 to complete-suit output. Does not multiply reserve capacity. | 0.1 / 0.2 / 0.4 BU/s only while demand exceeds the unmodified output; no additional load |
| C3 Regrowth Lobe | B; C-R | 2 / 3 / 4 | Axolotl; Membrane Sheet | Restore 1 health point every 10 / 6 / 4 s, starting 5 s after the last hostile hit. Healing stops at full health and is not damage immunity. | 10 BU per health point; load 4 while healing |
| C4 Impact Bladder | B; C-G | 2 / 3 / 4 | Slime; Elastic Gel | Reduce residual physical hit damage by 10% / 15% / 20%, at most 2 / 3 / 4 health points prevented per hit. | 2 / 3 / 4 BU per health point prevented; load 4 / 6 / 8 while armed |
| C5 Thermal Exchange | A; C-T | 2 / 3 / 4 | Magma cube; Thermal Lining | Reduce Thermal Mode biomass cost by 10% / 20% / 30%. No conversion of environmental heat into free suit biomass. | No additional BU cost; load 2 / 3 / 4 while exchanging |
| C6 Gill Bellows | B; C-W | 2 / 3 / 4 | Turtle; Membrane Sheet | Supply up to 10 / 30 / 60 s of breathing per pulse. Refills only missing air; unused supply is not banked beyond the current air capacity. | Full pulse costs 3 / 6 / 10 BU, prorated for missing air; load 2 / 3 / 4 while operating |
| C7 Blast Baffles | A; C-G | 2 / 3 / 4 | Creeper; Vitreous Scute | Reduce residual explosion damage by 10% / 15% / 20%, at most 3 / 5 / 7 health points per explosion. | 3 BU per health point prevented; load 4 / 6 / 8 while armed |
| C8 Burrow Mantle | A; C-M | 3 / 4 / 5 | Enderman; Diamond-Fiber Matrix | Whole-body passage support. Together with H8/L7/B8 gives a 10 / 20 / 35 s safe stopping window. | Whole Burrowing mode: 24 / 20 / 18 BU/s; load 12 / 10 / 8 |
| C9 Feeding Lobe | B; C-M | 1 / 2 / 3 | Host; Nutrient Mash | Automatically consume permitted real food from 1 / 2 / 3 selected inventory slots when hunger is at or below 14 / 16 / 18 points. At most one food per 10 s; normal food effects apply. | 1 BU per food; load 1 for 1 s. No biomass-to-food conversion. |
| C10 Service Tendril | B; C-M | 1 / 2 / 3 | Bee; Membrane Sheet | Transfer stored biomass at 2 / 4 / 6 BU/s to one explicitly selected compatible tool or helper within 2 blocks. Stops when its tank is full. | Actual transferred BU; load 2; transfers give no Metabolic Work credit |
| C11 Elytral Wings | S; C-M; auric-derived G4 frame | 3 / 4 / 5 | Phantom; precision-conditioned Spatial Membrane; actual Elytra at I only | I: controlled descending glide. II: powered forward flight and climbing. III: controlled takeoff and hover. Driven speed ceiling 6 / 8 / 10 m/s. | 2 / 6 / 10 BU/s; load 5 / 8 / 12 |
| C12 Nutrient Intake | B; C-M | 1 / 2 / 3 | Host; Capillary Gel | Refill from 1 / 2 / 3 selected sealed biomass containers at up to 2 / 5 / 10 BU/s, stopping at capacity and retaining partial containers. | Transfers actual biomass; no output increase or practice credit. |

C4 and C7 are alternative uses of the same inflatable defensive cavity and cannot be installed together. C8 and C11 are incompatible body plans and cannot be installed together. A soft chest can still install reserve, pump and a mobility plan, but rarely their maximum ranks plus regeneration and a full habitat lining.

C4/C7 apply after ordinary armor calculations. The sum of additional physical/explosion reduction from living-armor mutations is capped at 20% of the residual damage, before the row's per-hit limit. Potion and enchantment stacking requires the separate compatibility decision in section 15; it is not silently another multiplicative defense layer.

C3 is **auto-healing**; M1 is armor self-mending; C9 **feeds the player**; C12 **refuels armor**. No health-to-biomass conversion exists. Healing hunger injury awards no Guarding, repair or Metabolic Work credit. Food filters default empty: the player chooses supplies rather than losing rare food silently.

C6 stores its expanded air allowance on the worn piece and draws from it while submerged. Removing the piece removes access to that allowance, without refilling it. Ordinary water-breathing effects are respected: the suit does not spend fuel while one is already supplying breath.

C11 requires a complete suit and permanently excludes C8. The actual Elytra is incorporated and cannot be recovered while retaining wings. It does not duplicate the Elytra's durability or enchantment effects; enchantment migration remains part of the open enchantment decision.

Without fuel, powered flight stops. An intact incorporated wing retains an ordinary descending glide, never hover or climb. Collapsed wings provide no promised fall rescue. Warnings show landing reserves; flight never force-loads chunks.

## 10. Leggings and boots branches

### Leggings: drive, endurance and work

| ID; branch | Schedule; counter | Slots I / II / III | DNA; signature ingredient | Effect I / II / III | Fuel and load |
|---|---|---|---|---|---|
| L1 Running Tendons | B; L-E | 1 / 2 / 3 | Rabbit; Membrane Sheet | +10% / 20% / 30% grounded sprint speed. | 0.5 / 1 / 2 BU/s while sprinting; load 1 / 2 / 3 |
| L2 Endurance Mesh | B; L-T | 1 / 2 / 3 | Host; Membrane Sheet | Reduce sprint-related hunger expenditure by 15% / 30% / 45%; does not reduce starvation damage or replace food. | 0.1 / 0.2 / 0.4 BU/s while sprinting; load 1 |
| L3 Swimming Muscles | B; L-W | 1 / 2 / 3 | Dolphin; Membrane Sheet | +15% / 30% / 45% self-propelled swimming speed. | 0.4 / 0.8 / 1.2 BU/s; load 2 / 3 / 4 |
| L4 Bracing Tendons | B; L-S | 1 / 2 / 3 | Turtle; Bone Plate | While deliberately braced on a surface and using a tool, reduce hostile knockback by 20% / 40% / 60%. Walking releases the brace. | 0.2 / 0.4 / 0.6 BU/s while braced; load 1 / 2 / 3 |
| L5 Working Tendons | B; L-S | 2 / 3 / 4 | Zombie; Bone Plate | +10% / 20% / 30% manual tool working speed. Does not change harvest tier, loot, ore identity, durability cost per action or machine speed. | 0.3 / 0.6 / 1 BU/s during actual tool work; load 2 / 3 / 4 |
| L6 Stalking Fibers | B; L-T | 1 / 2 / 3 | Fox; Membrane Sheet | +20% / 40% / 60% crouched movement speed. Does not make the wearer invisible or silence every action. | 0.1 / 0.2 / 0.3 BU/s while moving crouched; load 1 |
| L7 Burrowing Muscles | A; L-B | 3 / 4 / 5 | Enderman; Diamond-Fiber Matrix | Coordinated Burrowing travel speed 0.8 / 1.2 / 1.6 m/s through eligible terrain, including deliberate upward/downward movement. | Included in C8 cost/load |
| L8 Fast-Lane Tendons | B; L-T | 2 / 3 / 4 | Host; Auric Myelin | +40% / 70% / 100% grounded travel speed on a compatible mutated fast-lane tissue. Needs a complete living suit and Surface Key II or III. | 0.5 / 1 / 2 BU/s on the active lane; load 2 / 3 / 4 |

L1 and L8 are alternative tendon architectures and cannot be installed together. L8 does nothing on ordinary stone, so a colony commuter and a wilderness explorer prefer different leggings. L4 and L5 can cooperate, but neither lets the player mine protected blocks or harvest an ore their tool cannot harvest.

### Boots: contact, climbing, landings and displacement

| ID; branch | Schedule; counter | Slots I / II / III | DNA; signature ingredient | Effect I / II / III | Fuel and load |
|---|---|---|---|---|---|
| B1 Contour Sole | B; B-F | 1 / 2 / 3 | Host; Elastic Gel | Deliberately step onto ledges up to 1 / 1.5 / 2 blocks high when there is full body clearance. Toggle off for precise building. | 0.05 / 0.1 / 0.2 BU per assisted step; load 1 during step |
| B2 Climbing Hooks | B; B-A | 2 / 3 / 4 | Spider; Bone Plate | Climb a contacted wall at 0.8 / 1.2 / 1.6 m/s. No climbing air, remote walls or unlimited hanging without fuel. | 0.4 / 0.8 / 1.2 BU/s; load 2 / 3 / 4 |
| B3 Landing Bladders | B; B-L | 1 / 2 / 3 | Slime; Elastic Gel | Absorb an additional 3 / 6 / 10 blocks of fall distance beyond normal allowance. Remaining fall distance still causes its usual consequences. | 1 BU per additional block actually absorbed; load 3 for 1 s |
| B4 Propulsive Fins | B; B-W | 2 / 3 / 4 | Squid; Membrane Sheet | +20% / 35% / 50% swimming speed. | 0.3 / 0.6 / 1 BU/s while swimming; load 2 / 3 / 4 |
| B5 Spring Heel | B; B-A | 2 / 3 / 4 | Rabbit; Bone Plate | Charged ground jump reaching 1.5 / 2.5 / 3.5 blocks above takeoff. Requires surface contact; not a midair second jump. | 3 / 6 / 10 BU; load 3 for 1 s; cooldown 6 s |
| B6 Blink Tendon | S; B-P | 3 / 4 / 5 | Enderman; precision-conditioned Spatial Membrane | Relocate up to 4 / 7 / 10 blocks to a visible clear supported destination. Does not pass through opaque terrain. | 20 / 30 / 45 BU; load 6 for 1 s; cooldown 12 / 10 / 8 s |
| B7 Surface Key | B; B-F | 1 / 2 / 3 | Host; Spore Culture | Store 1 / 2 / 4 consented colony tissue signatures. I identifies wearer to compatible defensive tissue; II also enables Fast-Lane Tendons; III adds capacity for multiple colonies, not immunity to every hostile biomass fluid. | No operating cost/load |
| B8 Ground Anchor | A; B-F | 3 / 4 / 5 | Enderman; Diamond-Fiber Matrix | Coordinate body contact for Burrowing in all directions. Effective rank limits the complete burrowing system. Does not supply air or light. | Included in C8 cost/load |

B2 and B4 cannot occupy the same foot anatomy: hooks and full fins are alternatives. Contour Sole, Landing Bladders and either one can coexist if they fit. B7 grants biological recognition only; it does not grant chest access, block-breaking permission or ownership of another player's colony.

For B6, both origin and destination must be loaded and permitted. The destination needs clear body space and a supporting surface. It cannot cross a protected boundary, bypass an opaque wall, or repeatedly reset falling into unlimited flight. Failed validation consumes no charge and gives no practice. The boots own its cooldown and operating cost.

### Movement ceilings

Movement bonuses add within their category; they do not multiply one another indefinitely. These ceilings apply to the contribution of this armor system, not to unapproved changes to every other mod's movement rules:

- Ordinary grounded sprint bonus: at most **+50%** from living armor, including growth.
- Prepared fast-lane bonus: at most **+100%**, only on the lane with its complete-suit prerequisites.
- Powered swimming bonus: at most **+75%**, or **+100%** with complete Pelagic III.
- Additional hostile knockback resistance: at most **80%** from frames and grafts combined. Environmental movement, explosions and scripted boss movement are not all the same effect.
- Burrowing speed: at most **1.6 m/s** in this progression. Running, lanes and swimming do not accelerate it.

Heavy-frame land penalties apply after the armor's positive movement bonus. A movement mutation failing or switching off restores ordinary movement, not a player-wide permanent disabled state.

## 11. Fuel, output and complete-suit abilities

### Two operating limits, not two fuels

**Biomass reserve** determines how long the equipment can work. **Metabolic output** determines how much it can do at once. Load is an operating reservation, not another consumable or an experience counter.

The chest establishes complete-suit output: **G1 = 6, G2 = 10, G3 = 14, G4 = 18 load units**. Pump Heart adds its listed output. A G4 chest with Pump III supports 28 load, not unlimited simultaneous abilities. The other pieces do not contribute another three chest outputs.

With fewer than four living pieces, each worn piece can power local mutations up to load 2 from its own reserve. Ordinary protection, capacity and free passive mutations still work. This allows a Lamp or introductory Gill Bellows without a full suit, but not Burrowing, powered flight, Fast-Lane operation or a full habitat system.

A complete suit can draw from all four real reserves. The HUD shows their sum and the current chest-controlled output. When a piece is removed, its remaining fuel leaves with it; fuel is not recreated in the other pieces. Installing a larger reserve creates empty space, not biomass.

Continuous load lasts while an ability is active. A triggered ability reserves its specified load for the listed interval. If the interval overlaps another activation, both reservations count. A player cannot evade output limits by alternating two buttons within one tick. Inactive mutations retain their slots but reserve no load.

### Mutual symbiosis and hunger pain

**Confirmed:** starting reserves are tiny. A worn awakened piece without available biomass hurts its wearer until mutual symbiosis is complete. This is automatic and disclosed, not an opt-in emergency mutation. After full symbiosis, empty reserves cause no armor hunger pain: unpaid biological abilities and self-mending pause. Ordinary protection and unpowered movement remain.

**Proposed ownership model, pending owner choice:** the player has four body-adaptation tracks, one per equipment slot; each piece has one maturation counter. Both must mature for that slot to be fully symbiotic. A practiced wearer matures new equipment faster, but a purchased mature piece does not instantly adapt a new wearer. This is separate from the five practice counters and awards no growth points.

Candidate tuning: one adaptation credit per minute containing eligible fed activity, capped at 60 credits per wearer track and 30 per piece. An adapted wearer doubles piece maturation credit. Travel, work and productive abilities qualify; idle time, starvation, deliberate injury and repair loops do not. Pain is a risk to manage, not an XP currency.

Before full symbiosis, propose **one health point every five seconds per wearer**, not per piece, while any worn hungry piece remains unadapted. Shared suit fuel feeds all four pieces; partial equipment uses local reserves. Quick swaps/relog do not reset the cadence; missed offline damage never accumulates into a burst. Whether pain can kill and how difficulty affects it remain decisions before implementation.

Refuel or remove the hungry equipment to stop that pain condition. Running out inside rock or underwater still risks ordinary environmental damage after symbiosis. Removing essential equipment is not a guaranteed escape. Collapsed armor is metabolically inactive and causes no hunger pain.

Death preserves body adaptation; trading preserves piece maturation but transfers no wearer track. Fusion and repair reset neither. Store no growing wearer–item UUID history. HUD distinguishes empty fuel, body adaptation, piece maturation and full symbiosis. These representation and tuning details remain proposals.

### Priority and starvation

The player can arrange optional features into priority groups. Life-support priorities remain explicit and protected:

1. Current breathing, current thermal protection, or current Burrowing passage, as appropriate to the actual environment.
2. A requested emergency exit/Blink, if valid and compatible with the current state.
3. Armed defense and selected work/movement abilities.
4. Regrowth, armor repair and decorative/optional sensing.

If output is insufficient, lower-priority abilities pause before activation and the HUD names what paused. Already absorbed damage is not retrospectively applied. If biomass is insufficient, no effect is promised for which the suit cannot pay. Environmental warnings show both current reserve and estimated survival time; the player can reserve a chosen amount of fuel from optional consumption.

Examples: underwater propulsion yields to breathing; repair yields to Burrowing; an Impact Bladder cannot silently consume the last fuel reserved for lava protection. Life support cannot be made free simply by giving it top priority.

Completely worn-out armor retains its item, counters and installed anatomy as a **collapsed piece**. It contributes no protection and cannot operate mutations until repaired at a dock or chamber. This prevents accidental loss of a long-lived lineage without making it useful at zero durability. A collapsed piece breaks complete-suit requirements. Death, item despawning and world hazards can still destroy the dropped item; there is no universal soulbinding promise.

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

## 12. Example builds that actually fit

These examples use **zero growth-point bonuses** so their basic arithmetic is visible. Mutation prerequisites still have to be earned. The slot total shown is occupied anatomy on that individual piece, not across the set. “Repair II” means M1 II, not a separate fifth armor item.

### A. First useful living suit

Four G1 frames: four slots per piece, six total armor points, output 6.

| Piece | Installed mutations, with slot costs | Used / available |
|---|---|---|
| Helmet | Lamp I (1), Repair I (1) | 2 / 4 |
| Chest | Reservoir I (1), Repair I (1) | 2 / 4 |
| Leggings | Endurance I (1), Repair I (1) | 2 / 4 |
| Boots | Contour Sole I (1), Repair I (1) | 2 / 4 |

Capacity is **170 BU**: 40 base chest + 100 reservoir + three 10-BU local reserves. Lamp and Endurance together consume 0.2 BU/s and load 2 during a run. Repair adds real cost only when there is damage to mend. The armor is still poor in a fight, but offers light, modest travel convenience and a reason to maintain the same pieces.

All five installed mutation types here are host-grown; this first benefit does not depend on already completing several creature genomes. The pieces can reach their first ranks through ordinary travel, work, sampling and minor wear.

### B. Heavy Nether worker

Four G4 Netherite Lamella frames: ten slots per piece, twenty total armor points, output 18. This is a long-term specialist, not the first Nether trip.

| Piece | Installed mutations, with slot costs | Used / available |
|---|---|---|
| Helmet | Thermal III (4), Thermal Sight I (2), Repair II (1) | 7 / 10 |
| Chest | Thermal III (4), Reservoir II (2), Thermal Exchange I (2), Repair II (1) | 9 / 10 |
| Leggings | Thermal III (4), Working Tendons II (3), Repair II (1) | 8 / 10 |
| Boots | Thermal III (4), Contour Sole II (2), Landing Bladders II (2), Repair II (1) | 9 / 10 |

Capacity is **670 BU**. During lava immersion with Thermal Sight active, load is **8 + 2 + 2 = 12** and fuel use is **5 × 0.9 + 0.2 = 4.7 BU/s**. A full reserve lasts about **143 seconds**, without tool work or repair. Working Tendons raises that to load 15 and 5.3 BU/s. Multiple repairs may pause while the suit is working.

It is protected and capable in lava, but has no gills, underwater propulsion, Blink or Burrowing. Thermal linings cannot be replaced by pelagic linings: grow separate ocean pieces. Better colony refueling matters more than another arbitrary protection multiplier.

### C. Ocean exploration and manual mining

Four G4 Netherite Mesh frames: thirteen slots per piece, fifteen total armor points. Pump I raises output from 18 to 22 when needed.

| Piece | Installed mutations, with slot costs | Used / available |
|---|---|---|
| Helmet | Pelagic III (4), Aquatic Eyes III (3), Field Lens II (2), Repair II (1) | 10 / 13 |
| Chest | Pelagic III (4), Gill Bellows III (4), Reservoir III (3), Pump I (1), Repair II (1) | 13 / 13 |
| Leggings | Pelagic III (4), Swimming Muscles III (3), Working Tendons III (4), Repair II (1) | 12 / 13 |
| Boots | Pelagic III (4), Propulsive Fins III (4), Landing Bladders II (2), Repair II (1) | 11 / 13 |

Capacity is **1,570 BU**. Powered swimming with eyes and gills active uses load **3 + 2 + 4 + 4 + 4 = 17**. The +45% muscles and +50% fins give a **+95%** armor swimming bonus, permitted by Pelagic III. Working Tendons adds load 4, bringing demand to 21 and activating the pump's 0.1 BU/s overhead. Repair and optional examinations may pause during that work.

This suit breathes, sees, maneuvers and works underwater. It has lower physical protection than the Nether worker, no lava seal, no wall hooks and no burrowing mantle. Its full chest leaves no room for an Impact Bladder or Regrowth without sacrificing something.

### D. First deep-rock explorer

Four G3 Diamond Tendon frames: eleven slots per piece, thirteen total armor points, output 14.

| Piece | Installed mutations, with slot costs | Used / available |
|---|---|---|
| Helmet | Stone Sense I (3), Lamp II (2), Repair II (1) | 6 / 11 |
| Chest | Burrow Mantle I (3), Reservoir III (3), Repair II (1) | 7 / 11 |
| Leggings | Burrowing Muscles I (3), Working Tendons II (3), Repair II (1) | 7 / 11 |
| Boots | Ground Anchor I (3), Landing Bladders II (2), Contour Sole II (2), Repair II (1) | 8 / 11 |

The complete I burrowing system moves at **0.8 m/s**, has a **10-second stopping window**, and with Lamp II draws **24.2 BU/s at load 13**. Its 1,570 BU reserve provides about **64 seconds** of passage, or at most about **51 meters** of continuous straight travel before other costs. The player must plan a destination, retreat margin and safe cavities. This is not unlimited underground travel simply because the slots fit.

Later forward Spatial Weave fusions provide higher caps and reduce each Burrowing graft's slot cost by one. Upgrading all four grafts to II improves speed, stability and fuel efficiency. A player who upgrades only the chest still operates a rank-I system until the other adaptations catch up.

### E. Ordinary expedition generalist

Four G3 Diamond Carapace frames: eight slots per piece, eighteen total armor points, output 14.

| Piece | Installed mutations, with slot costs | Used / available |
|---|---|---|
| Helmet | Lamp II (2), Aquatic Eyes II (2), Toxin Filter II (2), Repair II (1) | 7 / 8 |
| Chest | Reservoir II (2), Regrowth II (3), Impact Bladder I (2), Repair II (1) | 8 / 8 |
| Leggings | Running Tendons II (2), Endurance II (2), Working Tendons II (3), Repair II (1) | 8 / 8 |
| Boots | Contour Sole II (2), Landing Bladders II (2), Climbing Hooks II (3), Repair II (1) | 8 / 8 |

This is a useful everyday suit: decent protection, travel, modest recovery and several conveniences. It is not deliberately miserable. Its limit is specialization: no sustained lava work, air supply, high-speed diving, fast-lane architecture or solid-rock passage. It can take a potion and make a short excursion, but cannot replace the specialist's anatomy with a universal “environment resistance” upgrade.

## 13. A piece's history: repair, transfer and rebuilding

### What persists on the item

The item retains its lineage, five counters, growth allocations, installed mutations, maturation, biomass, durability, dye and trims. Cooldowns survive quick swaps, dimension changes and reconnects. A dropped item can still be lost.

### Permanent choices and safe treatment

There is no mutation-extraction recipe or Recovered Graft. Disabled mutations still occupy anatomy and exclude siblings. I → II → III advances the same mutation. Rejected fusion changes nothing; cancellation returns only the unchanged piece and unconsumed reserved inputs. Preview material costs, permanent exclusions and protection tradeoffs. Full outputs pause work without dropping items.

### Moving learning into a successor

Memory Gland extraction removes one named practice counter amount from the donor into a physical, lineage-tagged Memory Sample. Recipient slot and counter must match; committed material and exclusive trait choices must match the donor or be legal forward descendants. Uncommitted donor learning may seed either future branch. No rigid-to-flexible laundering.

Transfer stops at the recipient's ceiling; the remainder stays in the sample. It transfers no frame, graft, maturation or wearer adaptation. Reduced donor ranks can make allocations/grafts dormant, but never refund or erase choices. A DNA Bank records knowledge, not copies of practice.

### Keeping specialists

Diamond Carapace can advance to Netherite Lamellae, never Diamond Tendon. Start another G1 piece, choose Auric Lattice and train it for the flexible route. Swapping owned pieces is normal equipment use; rewriting their committed identities is not.

## 14. Late End and Fold adaptations

G4 is the final material/learning grade in this proposal. Netherite does not lead to a succession of fictional metals with endlessly increasing armor points. Later development changes capabilities and production demands.

### End specialization

Spatial Weave and Netherite Mesh are terminal choices of Diamond Tendon. Elytral Wings require a real Elytra and flexible G4 chest, permanently excluding Burrow Mantle. Native End materials support both families without making them interchangeable.

### Fold adaptation: reserved long-term branch

An **Adaptive Interface** is prepared as the Item Catalog's I119 consumable using two Adaptive Gel, one Thermal Lining, one precision-conditioned Spatial Membrane, four Binder and 1,000 BU. Installation consumes that graft, occupies two permanent slots and adds no second ingredient or fuel charge. It stores two control presets for the **same installed compatible grafts**: priorities, triggers and reserve thresholds. The union of installed anatomy plus interface must fit. No hidden second anatomy, thermal/pelagic exchange or aerial/burrowing switch is permitted.

Proposed activation: 30 seconds at a supplied Service Pedestal, costing 200 BU. A Manyfold-derived **Reciprocal Control Graft** permits this settings-only change in the field: 10 seconds stationary, 100 BU, five-minute shared cooldown, interrupted by hostile damage. It adds no slots, counters or effects. The Item Catalog owns its consumable recipe. Physical organ Expression Switches have separate production-profile rules; they cannot alter armor anatomy.

## 15. Appearance, controls and compatibility boundaries

### The body should visibly express the build

- **Dormant:** thin irregular tissue straps, exposed gaps and small protective patches. It looks incomplete because it is incomplete.
- **Awakened:** thicker living seams and subtle breathing, with the face, hands and selected joints still visible.
- **Iron ribs:** mineralized ribs supporting tissue plates; not an iron chestplate recolored red.
- **Auric lattice:** fine warm conductive fibers beneath translucent membranes, with open joints and less bulk.
- **Diamond carapace:** overlapping faceted scutes. **Diamond tendon:** narrow mineral fibers following muscle lines rather than large plates.
- **Obsidian:** broad dark glass-like scutes and reinforced edges; visibly heavier than the woven paths.
- **Netherite lamellae:** dense overlapping plates with living seams. **Netherite mesh:** a restrained dark web supporting exposed flexible tissue.
- **Spatial weave:** thin layered membranes, slight edge shimmer during spatial activity, not a permanent opaque particle cloud.
- Installed Lamp, gills, reservoir, fins and climbing hooks have small readable physical features. Mutation rank refines that feature; it need not replace the whole armor model.

Dye changes the main tissue pigment, while a separate restrained accent preserves readable biomass/thermal/potion cues. Dyeing never resets stats or counters. A trim is cosmetic and survives awakening and forward fusion when present. It cannot secretly grant another material reinforcement tier. Helmets should leave identifiable parts of the player's face visible; reinforced variants need not become completely closed masks.

### Armor interface

One consistent armor screen has four piece tabs. Each tab shows frame stats, all five actual counter values and next thresholds, allocated growth points, anatomy occupied/available, and installed grafts with clear prerequisites. Selecting a proposed fusion previews the exact differences before any material is consumed.

The combined-suit panel shows real fuel, maximum output, reserved load, paused abilities and the missing piece/rank for any incomplete combination. Example: **“Burrowing I: boots Ground Anchor I limits the system. Upgrade all four adaptations to II for 20 s stability.”** Not merely “invalid suit.”

The normal HUD is compact, with a half-transparent background, movable position, scale and hide options. It expands for low fuel, approaching lava/stability limits or a deliberately requested status view. Turning the routine HUD off does not silently remove critical safety warnings; those have their own explicit setting.

### Curios

Proposed accessory roles are a control organ, a small emergency reserve or a potion dispenser. None grants armor points, missing full-suit pieces, counter ranks or unrestricted anatomy. A reserve accessory may contribute fuel but not output. Multiple copies of the same accessory role do not stack. Any accessory supplying an armor function must use the same fuel ledger, priority rules and potion cooldown—not a second independent loophole.

Accessory recipes and proposed capacities are in Item Catalog I087–I089. Armor must remain usable without requiring every Curios slot to be filled.

### Enchantments and potions

**Whether living armor accepts ordinary enchantments remains an owner decision.** This draft's protection, durability and specialist examples assume no enchantment bonuses. It neither bans enchanting nor promises full unrestricted stacking.

The options to decide are: ordinary enchanting with explicit stacking limits; a limited set of compatible enchantments; or biological equivalents without direct armor enchanting. Whichever is chosen, forward evolution must not silently delete existing enchantment data. If an enchantment becomes inactive on a destination frame, the preview must say so before confirmation.

Potion support is more direct: drinkable effects remain useful, and Potion Capillaries automate consumption of real doses. A potion can temporarily cover a specialist's weakness without permanently occupying anatomy. It does not make the suit universal without preparation, supplies and limited dose storage.

### Expensive behaviors and hard boundaries

Burrowing, dynamic illumination and wall-adjacent sensing are the material performance risks in this design. Their eventual implementation needs separate approval of cost and measured limits; the gameplay does not authorize a world-wide scan or terrain-replacement flood.

- Counter storage is exactly five typed counters per piece, bounded ranks and fixed mutation entries—not a growing record of every block visited or mob fought. Recent-event suppression uses a bounded cache, at most 32 identities per equipped piece, and its cooldown must survive a quick unequip/re-equip.
- Scent considers at most 16 targets per four-second pulse, within its capped range and loaded chunks. It never discovers the entire nearby population before applying a cosmetic display limit.
- Burrowing's immediate collision/passability work is limited to a **3 × 3 × 3 body-neighborhood** per movement check, with early rejection when the next movement envelope is not loaded. Stone Sense inspects at most 64 candidate positions per pulse, at most two pulses per second; it is an intentionally incomplete local cavity cue, not a promise to map every block in its radius.
- The lamp is a bounded visual light effect, maximum 12-block reach. It must not repeatedly place/remove light blocks or cause chunk-light recalculation storms as the player runs.
- Colony Reader requests the selected organ's existing status snapshot. It does not traverse an entire logistics network to draw one helmet label.
- Refueling and forward evolution use bounded tanks/inventories with back-pressure. No armor operation vents excess fluid or throws items into the world as its automatic overflow behavior.

These bounds apply per equipped player in multiplayer. If they cannot deliver a readable effect, redesign the effect within a measured budget.
