# Item catalog and biological ingredient preparation

Proposed items and recipes, grouped by first availability. T0–T9 are catalog
stages, not item levels. Raw materials construct bodies; prepared treatments
change their function.

## 1. What goes into an upgrade

```text
Raw materials ── crafting ───────────────────────→ basic organ body / frame
      │
      ├─ Culture Bowl / Activation Cyst ────────→ active secretion or gel
      │                                               │
      ├─ Gizzard → Kidney → Culture Vat ────────→ mineral fusion medium
      │                                               │
Specimen → Extractor → DNA Bank + Genetic Stock        │
                                  └─ Fusion Chrysalis ←┘
                                          │
                          target-specific consumable graft
                            ├─ organ or mature tissue
                            └─ chamber → existing equipment
```

Construction is allowed to use raw iron, glass, tools, wood and gems as actual structural parts. A functioning organ does not improve merely because an ingot or rabbit hide is clicked onto it. Functional changes consume the named prepared treatment. Dyes remain directly applicable cosmetics; actual seeds remain planting stock; bone meal remains a direct maturity treatment. These exceptions do not unlock an unrelated functional mutation.

A mineral medium is not universally substitutable: Ferrocyte Paste gives iron's structural properties, Auric Myelin supports signaling, and Vitreous Scute supports heavy reinforcement. A genome is knowledge; stock is physical biological material; neither creates its source's mineral loot.

### Processing organs and the first-batch route

| Organ | Earliest role | Input → output | Why it remains useful |
|---|---|---|---|
| Culture Bowl, T0-02 | Starter activation | Organic feed + culture + water → early gels, secretions and host rank-I grafts | Works before automation or DNA research; early recipes remain valid later. |
| Activation Cyst, T1-30 | Bulk activation | Listed raw ingredient + biological carrier → the corresponding active ingredient | Automates preparation and treats reactive inputs; never installs a mutation itself. |
| Mineral Gizzard + Washing Kidney, T2-23/24 | Mineral preparation | Real mineral → cleaned portion + accounted residue/wash water | Separate no-bonus ingot/gem granulation from ore-recovery recipes. |
| Reaction Polyp + Work Bed, T2-33/T1-32 | Apply a prepared treatment | Real held target + its listed reagent → treated target | Reagent preparation remains at the Bowl/Cyst; fracture and collection remain separate operations. |
| Genetic Culture Vat, T2-05 | Mineral binding and stock culture | Cleaned mineral + Binder → fusion medium; or genome + stock seed + feed → matching stock | Two explicit modes. Material mode needs no completed creature genome. |
| Fusion Chrysalis, T2-31 | Target-specific grafts | Active ingredients + Binder + relevant stock → one declared consumable graft | Selects the target and expression before installation; no universal upgrade paste. |
| Thermal Nursery / Mantle, T3 | Thermal growth / conditioning | Native heat + actual membrane/mineral → Thermal Lining / tempered products | Nether-native production, not an ingot-only crafting shortcut. |
| Ion Separator, T4-05 | Precision preparation | Accounted suspension/mineral + power → fine fractions or woven fibers | No second full yield from processing both concentrate and its tailings. |
| Spatial Nursery / Conditioner, T5 | Spatial growth / purpose selection | End-native culture → Spatial Membrane → passenger, cargo or precision form | The three conditioned forms are different items, not a GUI toggle after use. |
| Synthesis Heart / Adaptive Culture Loom, T7/T8 | Compound / native adaptive materials | Compatible prepared donors and feed → one compound body or adaptive membrane | Requires the earlier supply chains; does not replace them with raw materials. |

Starter bowl activation takes 60 s per batch; the Cyst's same recipe takes 20 s. The Bowl prepares only recipes marked **Bowl/Cyst** below. Reactive T1/T2 treatments require the Cyst. Faster processing does not increase listed yield. T0 host-graft preparation uses its separate armor timing.

Short recipe labels Bowl, Cyst, Vat, Chrysalis, Mantle and Conditioner mean Culture Bowl, Activation Cyst, Genetic Culture Vat, Fusion Chrysalis, Thermal Mantle and Spatial Conditioner. A precision Chamber is the Mutation Chamber with its specified precision service bay, not another uncataloged organ.

One BU equals one mB biomass. A dose/portion is an item, not an unspecified tank volume. Unless stated otherwise, one batch produces one listed item; batch ingredients are consumed, tools explicitly described as tools are retained, and empty bottles/buckets are returned to a reserved output slot. Prepared solids do not decay just because a chunk is unloaded. No inventory ticker is needed.

## 2. T0 — starter items, reagents and hand equipment

| ID | Item | Obtain / batch recipe | Consumed by or used for |
|---|---|---|---|
| I000 | Spore Culture | Craft 1 rotten flesh + 1 red mushroom + 1 wheat seed → 1; Bowl renewal: 1 mushroom + 1 wheat seed + 100 mB water → 1 | Selected ground conversion, Organ Buds, early activation. |
| I001 | Organ Bud | Craft or Bowl: 1 Culture + 2 rotten flesh + 1 bone meal → 1 | Placeable T0-16 body precursor; same item as that block, not a second duplicate bud. |
| I002 | Membrane Sheet | Membrane Rack: 1 rotten flesh + 1 string + 100 mB water → 1; leather may substitute at twice the processing time | Flexible construction, Binder and graft carriers. |
| I003 | Bone Plate | Bone Loom: 1 bone + 50 BU → 1 | Structural ribs and prepared skeletal grafts. Bone blocks unpack into their normal bone-meal equivalent, not nine bones. Calcite alternative needs a separately balanced calcium recipe. |
| I004 | Fusion Binder | Bowl: 1 Membrane Sheet + 1 Culture + 100 BU → 4 portions, 60 s; Vat 15 s | Universal biological carrier, never a substitute for a signature ingredient. |
| I005 | Elastic Gel | Bowl/Cyst: 1 slime ball + 1 Culture + 50 mB water → 2 doses | Contour soles, impact/landing bladders and flexible organ treatments. |
| I006 | Lumen Secretion | Bowl/Cyst: 1 glow ink sac **or** 2 glow berries **or** 1 glowstone dust + 1 Binder + 25 BU → 2 doses | Lumen Tissue and Lantern Gland. These sources do not give different invisible light tiers. |
| I007 | Nutrient Mash | Bowl/Cyst: 1 wheat + 1 carrot + 100 mB water → 2 portions | Culture feed and Feeding Lobe graft preparation; not edible player food or free biomass. |
| I008 | Leaching Nodule | Leaching Gland: 1 bone meal + 100 mB water + 50 BU → 1 | Treat up to one 3×3 exposed eligible host-rock face; ores and block entities unchanged. Partial obstruction does not refund a whole dose. Exact reach is proposed balance. |
| I009 | The Waking Genome | 1 ordinary book + 1 Culture → 1 guide; first-join claim is a separate once-only delivery | Persistent access to instructions; losing a book loses no research. |
| I010 | Synaptic Probe | Craft 1 bone + 1 slime ball + 1 Culture → 1 | Inspect/configure reachable organs, show port direction, mark bounded areas. It is a tool, not an upgrade ingredient. |
| I011 | Empty Sample Vial | Craft 1 glass bottle + 1 paper → 1 | Manually withdraw one I040 sample from a pouch/archive or package one harvested plant specimen. Extraction returns this vial; directly collected samples need no vial. |
| I012 | Sample Pouch | Craft 2 Membrane Sheets + 1 string + 1 Sealing Resin → 1 | Standalone sample collection; capacities and upgrades below. No armor required. |
| I013 | Dormant Hood / Thorax / Legwraps / Treads | Four distinct armor items: 5 / 8 / 7 / 4 rotten flesh in vanilla armor silhouettes | One piece awakened in Cradle using Culture, Membrane Sheet and 100 BU. |
| I014 | Living Hood / Thorax / Legwraps / Treads | Awaken the corresponding existing dormant piece; empty fuel, preserved wear percentage | Four persistent equipment identities; full material and mutation tree in Armor Evolution. Frame variants are item data, not dozens of interchangeable crafting products. |
| I015 | Dormant Pick / Hatchet / Spade / Hoe | Familiar tool patterns with Bone Plates as heads and sticks as handles; proposed durability/harvest limits require the tool specification | Manual work; awaken one in Cradle using Culture, Sheet and 100 BU. |
| I016 | Living Pick / Hatchet / Spade / Hoe | Awaken corresponding I015 tool | Persistent tool counterparts. Mineral fusion must respect real harvest tier; area/precision branches remain proposed, not approved damage or yield rules. |
| I017 | Dormant Fang | 2 Bone Plates + 1 stick → 1 | Basic weak melee weapon; same cradle awakening route as tools. |
| I018 | Living Fang | Awaken I017 | Base for separately designed piercing/control weapon branches. |
| I050 | Skeletal Graft | Bone Loom: 1 Bone Plate + 1 Binder + 25 BU → 1 | Reinforce one cell or permitted organ skeleton; the plate is not duplicated. |
| I051 | Mutation Graft | Host rank I at T0; genetic/higher grafts from T2. Typed family under “Mutation grafts” below. | Installed once; target, equipment slot and rank are explicit. An installed armor graft cannot be extracted. |
| I019 | Biomass Bucket | Fill a vanilla bucket with exactly 1,000 mB from a compatible store | Real fluid transfer. Tank transfer or deliberate placement into a finite spill; containment and damage follow the Block Catalog's containment rules. |

## 3. T1 — active ingredients and tissue treatments

| ID | Item | Obtain / batch recipe | Consumed by or used for |
|---|---|---|---|
| I020 | Resin | Sap Tap: one supplied tree's allocated growth harvest → one resin portion | Seals and adhesive culture. Exact tree growth budget belongs to the ecology specification; tapping is not free wood plus free biomass plus free resin. |
| I021 | Capillary Gel | Cyst: 1 Elastic Gel + 1 Binder + 50 BU → 2 | Biomass Vein and Fuel Papilla tissue grafts; Nutrient Intake; organic pumping upgrades. |
| I022 | Filter Membrane | Cyst: 1 sand + 1 Sheet + 50 mB water + 25 BU → 1 | Washing/filtration attachments and Fluid Vein treatment. Sand becomes embedded filter material. |
| I023 | Contractile Fiber | Cyst: 1 string + 1 leather + 1 Binder + 50 BU → 2 | Item Veins, terrain stitches, transport-organ upgrades. |
| I024 | Synaptic Gel | Cyst: 1 redstone dust + 1 string + 1 Binder + 50 BU → 2 | Nerve Tissue and control treatments. Not electrical fuel. |
| I026 | Digestive Enzyme | Cyst: 1 fermented spider eye + 1 Elastic Gel + 1 Binder + 50 BU → 2 | Digestive Tissue; digestive-organ upgrades; C13 Digestive Crop graft preparation with zombie stock. No DNA required for the simple defensive floor; armor has its own genome gate. |
| I027 | Locomotor Gel | Cyst: 1 sugar + 1 rabbit hide + 1 Binder + 50 BU → 2 | Travel Tissue; later DNA-specific acceleration treatments. |
| I028 | Sealing Resin | Cyst: 1 resin or Elastic Gel + 1 Sheet + 25 BU → 2 | Chemical lining; sealed containers. Does not provide thermal/spatial protection. |
| I029 | Char Gland Feed | Bowl/Cyst: 1 charcoal + 1 Binder + 25 BU → 1 | Unlock solid-fuel feeding on a sufficiently practiced Bio-Furnace; operating charcoal is still consumed separately. |
| I030 | Honey Culture | Bowl/Cyst: 1 honey bottle + 1 Culture → 2, returning bottle | Honey-feed adaptation, first Cradle economy treatment. It is not a free healing potion. |
| I033 | Rooting Gel | Bowl/Cyst: 1 wheat seed + 1 Culture + 50 mB water → 2 | Selected crop-bed preparation, bush/tree graft attachment. Planting still needs real seed/sapling/cutting. |
| I034 | Sapping Bush Cutting | One reserved cutting from deliberate bush conversion or a mature propagation harvest | Replant T1-25; conversion yields at most one retained starter, not repeated berry drops. |
| I035 | Canopy Pod | Arbor/Canopy Cyst fills one pod from an assigned harvest allocation | Holds one declared resin, fiber or fruit batch. Open once; no second harvest from breaking its empty shell. |
| I036 | Sealed Biomass Ampoule | Empty: 1 Sheet + 1 Sealing Resin → 1; fill from storage up to 250 mB | Hold use to refuel selected worn pieces, or assign to C12 automatic intake. Retain partial contents on the same unstackable item. T0 armor can instead use the Bladder's manual Fill slot. |
| I037 | Route Imprint | Probe copies a bounded configuration onto 1 paper + 1 Synaptic Gel | Applies port/route settings to compatible owned tissue; never contains items, power, research or chunk-loading permission. |

Early Lumen treatment is made in the Bowl; all T1 transport treatments use the Cyst. Its own construction requires only a Bud, Sheets and a Plate, so a first Cyst needs no transport graft. A player can hand-feed it before automating it.

### Temporary construction tool

| ID | Item | Obtain / recipe | Consumer / constraints |
|---|---|---|---|
| I124 | Membrane Projector [held candidate] | Handheld construction organ; body recipe and access not yet specified | Spends armor biomass to place Temporary Membrane T1-37 at a permitted target. Range, lifespan, attachment and fall-arrest behavior unresolved. T1 grouping is provisional. |

## 4. T2 — genetics, prepared grafts, food and helpers

| ID | Item or bounded family | Obtain / recipe | Consumer / constraints |
|---|---|---|---|
| I031 | Survey Gel | Cyst, SR2 recipe: 1 amethyst shard + 1 bone meal + 1 Binder + 25 BU → 2 | Surveyed Tissue and visible finite mining markers. |
| I032 | Waymark Secretion | Cyst, SR2 recipe: 1 paper + 1 Contractile Fiber + 25 BU → 2 | Worker Waypoints; linking remains a configuration action. |
| I040 | Labeled Specimen | Species-specific creature death loot, or one consumed harvested plant source packaged in I011 | Grouped species/quality counts in a Sample Pouch or Specimen Archive; extraction only. Creature samples cannot be obtained from living targets or cultured stock. |
| I041 | Genome Fragment | Extractor consumes one specimen + 50 mB water + 25 BU in 20 s → coverage specified below, 1 matching Genetic Stock and 50 mB spent process water | Deposit once in a compatible physical DNA Bank. Coverage is a scalar per species, capped at its target; no random missing-region lottery. |
| I042 | Genetic Stock | Extraction supplies the seed; Vat culture: 1 stock + 2 Protein Culture Feed (animals) or 2 matching harvested plant items (plants) + 100 BU, 30 s → 3 matching stock, with complete genome | Net 2 new stock. Extraction and culture obey that species' native process requirements. Cannot produce specimens, knowledge, pearls, stars, bones or minerals. |
| I044 | Genome Record | Export a known source record to 1 paper + 1 Synaptic Gel at DNA Bank | Import into a compatible DNA Bank/Vault grade; holding the record grants no genome access. Neither physical stock nor transferable practice. |
| I045 | Antitoxin Serum | Cyst: 250 mB milk + 1 Filter Membrane + 50 BU → 1 | H5 graft preparation; process 1 milk bucket as four batches, returning one bucket, never four buckets. |
| I046 | Scent Concentrate | Cyst: 1 rotten flesh + 1 Binder + 25 BU → 2 | H7 grafts; source-specific repellent/lure recipes additionally require their declared scent/food. |
| I047 | Restorative Serum | Cyst: 1 golden apple + 1 honey bottle + 1 Binder + 100 BU → 2, returning bottle | Healing Dock mutation and Restorative Tissue, not directly drunk as a universal cure. |
| I048 | Restraining Graft | Chrysalis: 2 spider stock + 1 cobweb + 1 Elastic Gel + 1 Binder + 100 BU → 1 | One mature Restraining Tissue cell; spider genome required. |
| I049 | Aquaculture Graft | Cyst: 1 kelp + 1 prismarine shard + 1 Binder + 50 BU → 1 | One submerged mature tissue cell. Creature production still requires brood stock. |
| I052 | Plant Graft | Grafting Bench: 1 seed/sapling + 2 matching stock + 1 Rooting Gel + 100 BU → 1 modified planting item | Retains species and chosen trait. No conversion of wheat into an unrelated tree species. |
| I053 | Organ Trait Graft | Chrysalis: 2 matching stock + 1 listed active signature ingredient + 1 Binder + 100 BU → 1 named organ treatment | Applies only the organ's listed trait after its counter gate; no armor respecialization. Exact higher-rank recipe belongs with that trait. |
| I128 | Preservation Dose | Chrysalis: 2 spider stock + 2 turtle stock + 2 Sealing Resin + 500 BU, both genomes complete → 1 | Load one armor preservation cavity; Armor Evolution owns its consumption and descendants |
| I054 | Protein Culture Feed | Cyst: 1 raw fish or rotten flesh + 1 Nutrient Mash + 50 mB water → 2 | Animal-stock recipes, larvae. Not edible player food. |
| I055 | Expedition Ration | Ration Kitchen: 1 cooked beef + 1 baked potato + 1 carrot → 1 ration | Proposed 10 hunger, 12 saturation; real food for player or Feeding Lobe. No extra potion effect. |
| I056 | Travel Ration | Kitchen: 1 bread + 1 cooked fish + 1 honey bottle → 1, returning bottle | Proposed 8 hunger, 10 saturation; lighter farm recipe, not free speed mutation. |
| I057 | Mineral Concentrate | Gizzard recipe for named raw ore OR treated Mineral Fragments → that input's accounted concentrate + rock residue | Kidney/smelting. A fragment recipe uses its own remaining recovery allowance, not a second raw-ore bonus. Source-specific yields remain to balance. |
| I058 | Washed Concentrate | Kidney washes I057 with a declared water dose | Smelting/Separator; mineral removed into tailings is deducted from this output. |
| I059 | Mineral Tailings | Washing byproduct with bounded source/grade | Separator may recover only its remaining mineral fraction. Spent tailings are inert; no repeat extraction. |
| I060 | Rock Residue | Gizzard's accounted non-mineral fraction | Building aggregate or deliberate disposal, not a second ore product or organic biomass feed. |
| I061 | Washed Iron / Gold / Diamond / Obsidian Portion | Four distinct forms: corresponding ingot/gem/block → Gizzard granulation → Kidney with 100 mB water | One cleaned portion and 100 mB spent wash per source item. Fusion media below, no yield multiplier. |
| I103 | Mineral Workpiece | Work Bed wraps one idle intact/conditioned mineral specimen with 1 Membrane Sheet → one unstackable workpiece | Another Work Bed unwraps the same target and returns that one Sheet into reserved space. Item Veins, capsules and couriers can carry it. Fixed source/state fields; not a placeable fresh ore item or an arbitrary block-entity container. Wrapping grants no processing counts. |
| I107 | Mineral Penetrant | Cyst, SR2: 1 Digestive Enzyme + 1 Filter Membrane + 100 mB water + 50 BU → 2 doses | Reaction Polyp consumes one dose per eligible intact ore specimen; not interchangeable with host-rock Leaching Nodules. |
| I108 | Mineral Fragments | Fracture Jaw consumes one fully conditioned ore specimen → its registered mineral-fragment batch | Gizzard; fixed source/grade, remaining recovery and process-stage fields. Never placeable as fresh ore or eligible for Fortune. Ordinary drops are not also awarded. |
| I109 | Spent Penetrant | One committed Mineral Penetrant treatment → one retained spent dose | Compost Gland, T2 recipe: 2 spent doses + 1 leaf block + 100 mB water → 1 bone meal. Alternatively retain it for later neutralization. Inert sealed item; no metal recovery, free reactivation or biomass-feed recipe. |
| I062 | Ferrocyte Paste | Vat: washed iron + Binder + 100 BU → 1 dose, 30 s | Iron armor frame and structural organ treatments. |
| I063 | Auric Myelin | Vat: washed gold + Binder + 150 BU → 1 dose, 45 s | Auric frame, pump/control graft signatures. |
| I064 | Larval Cyst [optional] | Nursery: 1 egg + 2 Protein Feed + 250 BU → 1 retained larval cyst | Hatch only with a free reserved berth; at most three local workers in the first nursery proposal. Cyst and larva never exist simultaneously. |
| I065 | Courier Graft [optional] | Chrysalis: 2 bee stock + 1 Contractile Fiber + Binder + 100 BU → 1 | Mutate a real larva into a delivery helper. No spawn egg or additional population allowance. |
| I066 | Harvester Graft [optional] | Chrysalis: 2 cow stock + 1 Rooting Gel + Binder + 100 BU → 1 | Larval harvest profession; actual harvesting tool and destination required. |
| I067 | Sail Cyst [optional] | Nursery: 1 Larval Cyst + 1 feather + 2 Sheets + 500 BU → 1 | One Nutrient Sail at a free roost. No automatic chunk loading. |
| I068 | Grown Spine | Bone Loom: 1 Bone Plate + 25 BU → 8 spines | Spine Sentry / ranged weapons; shots consume ammunition and do not drop reusable infinite spines. |
| I038 | Piercing Fang / Grasping Limb / Spitter [proposed weapon forms] | Chrysalis prepares a named weapon graft from 2 relevant stock (skeleton / spider / slime), Bone Plate / Contractile Fiber / Digestive Enzyme respectively, Binder and 100 BU; Chamber applies it to one Living Fang for 100 BU | Three different resulting weapon bodies, not three simultaneous modes. Spitter consumes real Grown Spines plus fuel. Exact damage, counter gates and future branch policy require the tool/weapon specification; these are optional guide leaves. |
| I069 | Growth Template | Structure Grower records a player-selected bounded layout onto paper + Route Imprint | Placement still consumes real blocks; no copied organ contents/counters. |

### Samples, genomes and collection

Creature DNA comes only from species-specific death loot. A registered creature
death offers one Fragmented sample; player kills route it directly to the killer's
active Sample Pouch. An explicitly linked killing organ routes it to its Sample
Collector. One death uses one route and offers at most one sample, unaffected by
Looting or Fortune. Environmental deaths without an assigned collector offer none.
Vanilla loot remains unchanged. Every supported species, including bats and
silverfish, has its own sample identity even if it has no ordinary useful drop.
Farmed kills are valid; duplicate callbacks cannot award a second sample.

Unaccepted samples are not created as loose entities or diverted into ordinary
inventory. The pouch warns when full or filtered. Players can deliberately
withdraw samples into vials; those are ordinary droppable items. Samples have no
idle decay. Plant extraction consumes identified harvested material: wheat,
oak/birch saplings, kelp and sweet berries identify their corresponding sources.
Harvested plant specimens are Fragmented; Dissector does not improve them.
Generic wood, leather or rotten flesh cannot identify an arbitrary creature.

| Container | Species entries | Total samples | Upgrade / access |
|---|---|---|---|
| Sample Pouch | 16 | 256 | I012; one active pouch, ordinary inventory or collection accessory slot |
| Expanded Sample Pouch | 32 | 1024 | Cyst: existing pouch + 2 Filter Membranes + 2 Sheets + 250 BU |
| Reinforced Sample Pouch | 64 | 4096 | Chrysalis: existing expanded pouch + 2 Tempered Bone Plates + 2 Auric Myelin + 1000 BU |
| Specimen Archive | 128 | 16384 | T2-35; each additional connected archive remains a separate finite store |

Each species row holds three integer counts: Fragmented ×1, Intact ×2,
Pristine ×4. No per-kill records, timestamps or individual specimen histories.
The UI offers species filters, priority ordering and a configurable reserved
capacity for boss sources. Reserved capacity counts inside, not above, the total.
Only one pouch is active even if others are carried. Sample Dock moves the real
pouch's contents into a connected archive: all, selected species, or surplus above
a retained count. Extraction requests a selected species/quality through real
logistics; low-quality-first is the default and pristine can be reserved.

Genome coverage is retained by the bank, not the pouch, player or advancement.
A fragment contributes min(remaining coverage, quality × laboratory multiplier).
A copied Genome Record merges by maximum coverage, never addition. Reimporting
or cycling an exported record cannot complete an unfinished genome.

| Source class | Coverage target | Examples |
|---|---|---|
| Plant | 16 | Wheat, oak, birch, kelp, sweet berry bush |
| Ordinary creature | 32 | Cow, rabbit, bat, spider, silverfish, turtle |
| Complex creature | 128 | Enderman, blaze, shulker, guardian, phantom |
| Major boss | 1000 | Wither, Ender Dragon, Manyfold |
| Other Fold source | 256 | Foldroot, Pulse Reed, Glassbloom |

Major boss takes precedence over other source classes; other Fold sources take
precedence over the plant/ordinary/complex defaults. Storage grade and native
process requirements are independent of sample quality.
Block Catalog defines laboratory configurations R1/R2/R3 with multipliers 1/4/8.
Each uses its own listed extra cost, not the sum of lower configurations' costs.
Organ speed/economy choices do not further multiply information.

| Species | Coverage target | Laboratory multiplier | Quality multiplier | Samples to complete |
|---|---|---|---|---|
| Wither | 1000 | 1 | 1 | 1000 |
| Wither | 1000 | 4 | 2 | 125 |
| Wither | 1000 | 8 | 4 | 32 |

These are calculated baseline counts, not measured campaign pacing. Keep scarce
samples for a better lab; improve both collection and extraction rather than
requiring the least efficient route. A major boss genome remains a long project.
First boss victory grants its normal loot, not a completed genome.

### Weapon sampling specialization

A Living Fang chooses one permanent first branch: an I038 combat body or
**Dissector**. The Dissector line cannot become a Spitter or other combat body.
Its damage remains the unfused Living Fang baseline; its upgrades improve sample
quality, not damage, attack speed or ordinary drops.

| Grade | Preparation in Chrysalis; install at Chamber | Qualifying lethal hit |
|---|---|---|
| Dissector I | Living Fang target; prepare 2 spider stock + 2 silverfish stock + 2 Filter Membranes + 2 Binder + 500 BU; both genomes complete | Intact sample instead of Fragmented |
| Dissector II | Dissector I target; prepare 4 each spider, silverfish and enderman stock + 2 Diamond-Fiber Matrices + 4 Binder + 4000 BU; all genomes complete | Pristine sample instead of Intact |

The target is not consumed during graft preparation. The graft consumes only
its reagents; installation transforms the
same weapon with no recoverable second body. Only the actual killing weapon
sets quality. Merely carrying it or landing an earlier hit gives no bonus.
A killer's linked Sample Collector may hold one real Dissector in its service
slot for that organ's lethal-hit quality; the weapon cannot also
be wielded. Collector upgrades do not improve quality or increase loot count.
Ranged damage, unsupported external killers and shared kills use Fragmented
unless an explicit adapter identifies one eligible killing source.

### Separate biological backpack

| ID | Item | Creation / upgrade | Purpose |
|---|---|---|---|
| I126 | Carry Sac | Craft 4 Sheets + 2 Contractile Fibers + 1 Sealing Resin → 9 ordinary slots; Chrysalis upgrade with 4 Sheets + 2 Bone Plates + 500 BU → 18; then 4 Tempered Bone Plates + 2 Auric Myelin + 2000 BU → 27 | Separate portable storage, usable without armor; no feeding, healing, lighting, flight or processing |

One active Carry Sac; ordinary slots reject backpacks and other container-bearing
items. Its dedicated sample socket accepts exactly one Sample Pouch, preserving
the same contents and identity. Undock that pouch before nesting restrictions
would be violated. The socket is the only container exception.
No backpack chunk loading, auto-pickup scans or extra armor slots.
C9/C12/C13 can use up to their existing three explicitly selected supply slots in
the active Carry Sac; the pack itself grants none of those abilities.
Pack contents normally share its dropped item's fate; armor preservation protects
only its particular piece. Third-party packs are optional adapters, not a required
dependency or an excuse to change their storage rules silently.

### Ore dust and recovery

Recovery produces 4→5→6→7→8 dust per defined ore input; smelting
uses 4→3→2→1 dust per ingot. These are independent upgrades, not paired stages.
The maximum combination is eight ingots per defined input unit. Each ore recipe
must identify that unit and its processing steps. Do not multiply an ordinary block's mining drops and intact
treatment recovery together.

**Stockpiling is intended:** compatible dust produced earlier can feed a more
efficient smelter later. Its stored identity does not restrict it to the machine
that created it. Additional conditioning may be an alternative route, but must
not retroactively invalidate already compatible dust.

A recipe requiring four dust waits for four actual units; no fractional ingot or
rounding up. A 5-dust output can leave one dust after a four-dust smelt, to combine
with a later batch. Recipe outputs reserve all residues and containers.

Granulating an ingot for biological fusion produces Washed Portions, not
yield-eligible ore dust. Unsupported imported materials are refused until their
source and recycling paths are declared. Each processing recipe requires explicit energy, duration and demand;
missing costs are not zero-cost operations.

| ID | Item or family | Obtain / recipe | Consumer / constraints |
|---|---|---|---|
| I125 | Ore Dust [metal-specific family] | Actual eligible ore/raw material through metal-specific recovery recipes | Stored for any compatible current/later smelting recipe. Metal identity retained; eligible materials and treatment compatibility not yet specified. Never obtained as multiply-smeltable dust from ingots. |

### Mutation grafts

I / II / III graft preparation: 2 / 4 / 8
matching Genetic Stock from each required species; 1 / 2 / 4 of each listed
prepared signature; 1 / 2 / 3 Fusion Binder; 60 / 150 / 360 BU; 12 / 24 / 48 s.
Installation uses the same target piece, one graft and 40 / 100 / 240 BU over
8 / 16 / 32 s. Host I uses Spore Culture instead of Genetic Stock in the Bowl.

I051 is a **closed family**, not arbitrary NBT-programmable effects. Its allowed members are exactly the armor rows **M1–M7, H1–H10, C1–C13, L1–L8 and B1–B8**, ranks I–III. Common M grafts specify their destination slot; other letters already identify the slot. Each member uses the DNA/signatures and fusion-level requirements in [Armor evolution](ARMOR_EVOLUTION.md#5-mutation-installation). A tooltip names the exact outcome, not “mutation essence.”

C13's operating feed is not a new item family: explicitly supported ordinary biological materials are consumed as fuel, not applied as upgrades. Armor Evolution owns its proposed conversion rates and per-material acceptance rules. Samples, grafts and other valuable biological items are not implicitly edible. C12 accepts measured biomass containers instead; C9 consumes food for player hunger. One item cannot fund two of these operations.

The same family rule does not invent tool/weapon effects: their grafts require separately approved recipes. A treatment to advance an already committed branch is not a removable upgrade module. Item catalogs may list prepared grafts; there is deliberately **no Recovered Armor Graft** item.

Host rank I can be cultured in the Bowl and applied at the Cradle. Other ranks use the Chrysalis and Chamber. Elytral Wings I reserves an actual Elytra along with prepared materials; it cannot be grown solely from phantom stock. A cancelled batch cannot return both the Elytra and a finished wing graft.

## 5. T3–T4 — thermal and precision ingredients

| ID | Item | Preparation | Consumer |
|---|---|---|---|
| I025 | Conductive Myelin | Cyst, starter electrical recipe: 1 copper ingot + 1 redstone dust + 1 Binder + 100 BU → 2 | Conductive Tissue; no raw copper directly installed as a mutation. |
| I127 | Thermal Fruiting Body | Thermal Cultivation Tissue's finite native harvest | Nether biomass, water recovery and nursery feed; not magma cream, a creature sample or mineral loot |
| I070 | Thermal Seed | Cyst: 1 magma cream + 1 netherrack + 1 Binder + 50 BU → 1 | Mature selected substrate in the Nether into Thermal Substrate. Its ingredients can travel, native maturation cannot. |
| I071 | Thermal Lining | Thermal Nursery: 1 Sheet + (1 magma cream or 2 Thermal Fruiting Bodies) + 100 BU, with local Nether heat → 1 | Native thermal recipes, steam and heat lining, armor signatures. |
| I072 | Tempered Bone Plate | Thermal Mantle: 1 Bone Plate + 1 Thermal Lining + 100 BU → 1 | Heavy structure, machines and reinforcement. No additional bone output. |
| I073 | Faceted Chitin | Washed diamond + Binder + Thermal Lining + 200 BU; Mantle then Vat, 60 s total → 1 | Diamond Carapace fusion. |
| I074 | Vitreous Scute | Washed obsidian + Binder + Thermal Lining + 200 BU; Mantle then Vat, 60 s → 1 | Obsidian armor and blast-defense graft signature. |
| I075 | Tempered Skeletal Graft | Bone Loom: 1 Tempered Bone Plate + Binder + 25 BU → 1 | Upgrade existing bone-ribbed tissue or eligible structure. |
| I076 | Diamond-Fiber Matrix | Washed diamond + 2 Binder + Sheet + 250 BU; Ion Separator then Vat, 90 s → 1 | Flexible diamond armor and burrowing grafts. Requires real electricity. |
| I077 | Prepared Netherite Lamina | Mantle with precision attachment: 1 netherite ingot + Binder + 200 BU → 4, not four ingots | Terminal fusion recipes; cannot reconstruct extra netherite. |
| I078 | Living Netherite Lamella | Precision Chamber: 1 lamina + Binder + Thermal Lining + 300 BU, 90 s → 1 | Netherite Lamellae frame. |
| I079 | Netherite Tendon Mesh | Separator then precision Chamber: lamina + 2 Binder + Diamond-Fiber Matrix + 350 BU, 120 s → 1 | Netherite Mesh frame. |
| I080 | Netherite-Bonded Scute | Precision Chamber: Living Netherite Lamella + Vitreous Scute + Binder + 200 BU, 60 s → 1 | Terminal obsidian-lineage frame; distinct from lamellae. |
| I081 | Potion Vesicle | Cyst: 1 glass bottle + 1 Filter Membrane + Binder + 50 BU → 1 | Potion Capillary graft carrier. Glass is embodied in the vesicle, so this recipe returns no bottle. |
| I082 | Prepared Potion Dose | Infuser transfers one real potion into an empty vesicle | Same effect, duration and strength; one consumed dose returns one empty vesicle. Original potion bottle returns during loading only. |
| I083 | Survey Imprint | Upgraded Probe writes an authorized finite mining plan onto a Route Imprint | Rootstock, Strata Maw, bounded excavation. No terrain/item snapshot or copied resource yield. |
| I084 | Neutralizing Salts | Cyst: 1 bone meal + 1 sand + 100 mB water → 2 | Neutralization Gland's waste treatment. Actual waste recipes conserve fluid and produce accounted residue. |
| I085 | Inert Sludge | Neutralization Gland's spent organic residue | Deliberate disposal/approved building recipe only; cannot recover original biomass energy. |
| I086 | Precision Probe | Fuse existing Synaptic Probe + Auric Myelin + Survey Gel + 100 BU in Chrysalis | Adds bounded flow history and selected-path fault diagnostics. Preserves settings; does not scan every network in loaded worlds. |
| I087 | Neural Control Knot [Curios candidate] | Chrysalis: Organ Bud + 2 Synaptic Gel + Auric Myelin + 100 BU | Access to authorized configuration, no armor slot substitute. |
| I088 | Reserve Cyst [Curios candidate] | Chrysalis: sealed ampoule + 2 Sheets + Capillary Gel + 100 BU | Proposed 500-BU accessory reserve; actual refilling required, one reserve role, no extra metabolic output. |
| I089 | Chemical Bladder [Curios candidate] | Chrysalis: 2 empty Potion Vesicles + Sheet + Auric Myelin + 100 BU | Holds two real doses; shares armor dispensing cooldown. No duplicate effects from multiple accessories. |
| I104 | Prepared Genetic Suspension | Vat, SR4: 2 matching Genetic Stock + Filter Membrane + Thermal Lining + 100 mB water + 50 BU → 1 sealed source-specific batch | Ion Separator precision-genetics mode. Recovery never exceeds the two consumed stock portions; produces no genome coverage or death specimens. |
| I105 | Bioactive Leaching Charge | Leaching Gland with Thermal Mantle heat, SR3: 4 Leaching Nodules + Thermal Lining + 200 BU → 1 | Digestion Crucible's finite host-rock batch. Proposed at most 36 eligible host cells; contains no mined material before use. |
| I106 | Spent Leach Cake | Crucible retains the spent I105 treatment separately from accounted recovered host residue | Neutralization/disposal. No second terrain harvest or full-charge regeneration; the internal treatment medium is not a world-placeable fluid. |

These ingredients have useful non-armor consumers. Prepared iron reinforces load-bearing organs; gold supports pumps and signals; diamond supports cutting and precise fibers. Higher mineral yield, counter-cap changes and new processing recipes remain separate explicit organ mutations, not automatic consequences of feeding a better metal.

### Renewable native biomass and power

BU is biomass volume; **BE** is the colony's electrical energy unit. These are
proposed unmodified reference recipes, before speed/economy mutations. Growth
draws on native environmental conditions and real planting stock; biomass is
not conserved energy recovered solely from the previously supplied BU.
There is no reverse electrical-to-biomass recipe.

One reference module has one cultivated bed, separate Harvest Corolla/Collection
Cilia, Dew Gland, Digestive Sac, Electrocyte Stack and finite water/biomass/charge
buffers, plus a Washing Kidney for returned generator water. Reserve the basal
planting and next cycle's water/fuel before export.
The growing organ holds the yield until harvested; nothing drops on a timer.

| Native production mode | Bed / condition | Cycle | Harvest | Grow / harvest / dew cost | Digestive Sac yield per feed | Stack fuel → electricity |
|---|---|---|---|---|---|---|
| Overworld | 5×5 Cultivation Tissue; wheat planted, light ≥9 | 60 s | 8 wheat; basal seed retained | 40 / 20 / 40 BU | 100 BU per wheat | 200 BU → 4000 BE |
| Nether | 3×3 Thermal Cultivation Tissue; bone ribs and one Thermal Root on actual magma | 60 s | 12 Thermal Fruiting Bodies; fungus retained | 200 / 40 / 60 BU | 300 BU per body | 1200 BU → 24000 BE |
| End | 5×5 Chorus Orchard Tissue; four Anchor Roots, Chorus Resonator in cultivation tuning | 120 s | 12 chorus fruit; flower retained | 600 / 40 / 60 BU | 500 BU per fruit | 2000 BU → 40000 BE |

Cycle denotes steady-state harvest cadence, not first-batch latency. These fresh-
feed digestion recipes take 2 s/item, dew recovery 10 s/batch and generator
conversion the listed cycle duration. Separate organs overlap their work;
startup pauses until the next stage has its reserved supplies.

Every cycle allocates exactly **two** harvest items to Dew Gland → 1000 mB water;
the remaining six/ten/ten items go to digestion. The growth stage uses 500 mB,
the generator uses 100 mB and retains 100 mB dirty water; 400 mB clean water remains.
Digestive Sac requires no additional water for these fresh-harvest recipes.
Dirty water is held separately; Washing Kidney filtration consumes 10 BE/mB and
returns the same 100 mB clean water, with no consumed disposable filter.
Its 1000 BE cost is included below. Refused waste space stops the generator.

End cultivation consumes 12000 BE per cycle in its resonator; End routing and
pump services consume a further 1000 BE. Nether heat-control/pump services consume
5000 BE. Overworld basic passive routing has no additional electrical demand.
These service allowances cover the stated fixed assembly only, not arbitrary
remote routes or extra machines. Expansion must budget its real added loads.

| Dimension | Cycle seconds | Gross BU | Internal BU cost | Exportable BU | Gross BE | Internal BE cost | Exportable BE |
|---|---|---|---|---|---|---|---|
| Overworld | 60 | 600 | 300 | 300 | 4000 | 1000 | 3000 |
| Nether | 60 | 3000 | 1500 | 1500 | 24000 | 6000 | 18000 |
| End | 120 | 5000 | 2700 | 2300 | 40000 | 14000 | 26000 |

Arithmetic: Nether exports 25 BU/s and 300 BE/s per reference module versus
Overworld 5 BU/s and 50 BE/s. End exports about 19.17 BU/s and 216.67 BE/s,
but requires longer-cycle storage and powered native cultivation. Nether is the
bulk industrial source; End has valuable spatial production, not a mandatory
replacement for all Nether industry. These are calculated surpluses, not TPS,
measured throughput or a claim that the entire factory is self-powered.

Bootstrapping needs 1000 mB water, one cycle's internal BU and planting stock;
Nether also needs 6000 BE and End 14000 BE stored charge. Retain those amounts before distributing
surplus. Extra lining, culture, DNA, freight and armor loads are charged separately.
A disconnected exporter cannot consume restart reserves. Unloaded farms neither
produce nor spend fuel; no offline catch-up.

The starter Electrocyte Stack works in any dimension at the Overworld recipe.
Industrial conversion requires its core at L1, 2 Tempered Bone Plates + 2 Thermal
Linings installed at Chamber and a Thermal Mantle/Heat-Exchange Gill cooling
service. End conversion additionally requires 2 Spatial Membranes and an Anchor
Root service; first membrane production can use imported charge or starter stacks.
Native digestion and dew recipes require their core's matching thermal/spatial
mutation and actual native bed/dimension. Stockpiling imported fruit cannot
reproduce the native industrial yield in the Overworld.

For local construction, Membrane Rack consumes 2 Thermal Fruiting Bodies or
2 chorus fruit + 100 mB water → 1 Sheet on the corresponding native bed.
Cyst consumes the same two fruits + 100 mB water → 2 Protein Culture Feed;
Bowl consumes them + 100 mB water → 1 Spore Culture. Each is an alternative
use of the harvest, not an extra co-product. Thermal Nursery's local-fruit recipe
removes continuous magma-cream imports without producing magma-cube DNA.

Fold's long-term loop uses reserved Foldroot planting and Pulse Reed harvest:
Dew Gland consumes two reeds + 100 BU → 1000 mB water; Digestive Sac consumes one
mature harvested Foldroot + 50 mB water → 800 BU on an Adaptive bed.
Founder Nursery's growth and phase upkeep consume that supply. Phase Collector
provides native charge; Phase Accumulator buffers it. Retuning spends stored
charge and cannot drive its own collector. Establish all three local supplies
before boss-dependent recipes; do not require an Overworld fuel delivery to
continue a settled Fold habitat.

## 6. T5–T7 — spatial products and distributed construction

| ID | Item | Preparation | Consumer |
|---|---|---|---|
| I090 | Anchor Seed | Cyst: end stone + chorus fruit + ender pearl + Binder + 100 BU → 1 | Mature Anchored Substrate in the End; a native bed remains necessary. |
| I091 | Spatial Membrane | Spatial Nursery: Sheet + Thermal Lining + chorus fruit + 200 BU + electricity → 1 | Unconditioned End-grown material; insufficient for cargo/passenger recipes until conditioned. |
| I092 | Passenger Membrane | End Conditioner: Spatial Membrane + 100 BU + electricity + passenger resonator setting → 1 | Transit Maw, Arrival Chamber, Fold travel; consumed in construction/maintenance as specified. |
| I093 | Cargo Membrane | Same with cargo setting → 1 | Freight Gullet, Cargo Lock, Foundation Cyst. No conversion after incorporation without a declared recovery recipe. |
| I094 | Precision Membrane | Same with precision setting → 1 | Precision-conditioned Spatial Membrane in other documents; one canonical item, not two materials. |
| I095 | Phase-Woven Matrix | End Conditioner: Precision Membrane + Auric Myelin + Diamond-Fiber Matrix + 400 BU, 120 s → 1 | Spatial Weave terminal armor frame. |
| I096 | Chorus Orchard Graft | Cyst: chorus flower + bone meal + Binder + 50 BU → 1 | Mature Anchored Substrate orchard in the End. |
| I097 | Levitation Culture | Vat: researched shulker stock culture using its normal source feed | Shulker Genetic Stock under a role name, not a new species or free levitation potion. |
| I098 | Station Imprint | Probe records an explicitly authorized station identity onto Route Imprint | Pairing/diagnosis only; no remote block scan or forced chunk load. |
| I099 | Sealed Freight Batch | Cargo Lock reserves real cargo in its bounded internal buffer | **Transaction state, not a portable item**; cancelling returns the same goods. Avoid nested containers containing copies of a transaction. |
| I100 | Compound Organ Core | Synthesis Heart consumes a listed donor-body recipe | Same placeable item as the resulting named organ, with one designated history owner. No universal core substituting for every recipe. |
| I101 | Catalyst Culture | Catalyst Lobe's matching stock in a recipe-specific suspension | Recipe reagent service, not an immortal reusable catalyst. Stock and feed are consumed at declared rates. |
| I102 | Attuned Destination Seed | End Conditioner: 1 ender pearl + 1 Passenger Membrane + 1 Precision Membrane + 2 each silverfish, blaze and enderman stock + 500 BU, with all three genomes complete and a surveyed Fold resonance | Prepared Fold Gateway destination; no Fold-native material or boss loot needed for first entry. |

## 7. T8–T9 — Fold ecology and post-boss materials

| ID | Item | Obtain / preparation | Consumer |
|---|---|---|---|
| I110 | Foldroot Cutting | Gather from an existing Foldroot; nursery reserves propagation stock | Founder Nursery, native planting and first Fold plant specimen. |
| I111 | Pulse Reed Segment | Deliberate native harvest leaving basal growth | Planting, specimen or fiber processing; spend it once, not on all three. |
| I112 | Pulse Fiber | Membrane Rack: 1 mature Reed Segment → 2 fibers | Approved string substitute in biological construction, not arbitrary compatibility with every vanilla recipe. |
| I113 | Glassbloom Seed | One reserved seed from mature hardening-phase harvest | Cultivate Glassbloom; no automatic endless item-drop clock. |
| I114 | Glass Petal | Same harvest's allocated petal output | Glass substitute in Membrane Windows, or consume as specimen; soft-phase harvest yields specimen instead. |
| I115 | Adaptive Gel | Founder Nursery: 1 root cutting + 100 mB water + 200 BU during local feeding phase → 2 Gel + 1 retained cutting | Native bed, habitat upkeep, adaptive materials. Retained cutting is the next seed, not free harvest of the same plant again. |
| I116 | Adaptive Seed | Bowl/Cyst: Culture + Spatial Membrane + Binder + 100 BU → 1 | Apply to native Fold root-bearing ground; mature locally. No Adaptive Gel required for the first nursery's bed. |
| I117 | Adaptive Membrane | Adaptive Loom: Gel + Thermal Lining + Spatial Membrane + 2 researched local stock + 200 BU → 1 | Fold habitat parts and encounter structures. Dual process tolerance does not unlock incompatible armor branches. |
| I118 | Native Specimen Seal | Cyst: 1 Filter Membrane + 1 Sealing Resin + 100 BU → 1 | Consumed per Fold extraction batch to contain native material; not a captured creature or portable habitat. |
| I119 | Adaptive Interface Graft | Chrysalis in supplied Fold habitat: 2 Gel + Thermal Lining + Precision Membrane + 4 Binder + 1,000 BU → 1 | Two-slot armor control-presets interface; same installed compatible anatomy in both presets. |
| I120 | Manyfold Tissue Sample | Manyfold species variant of I040, awarded by a qualifying boss death under the same collection rules | Not a second sample family or a guarantee of complete DNA from one victory. Fold-native extraction and an exotic-grade bank required. |
| I121 | Manyfold Genetic Stock | Pattern Incubator: completed genome + sample seed/stock + Gel + Protein Feed + 200 BU → 3 stock per consumed stock seed | Net two renewable stocks; does not duplicate boss loot or unique trophies. |
| I122 | Reciprocal Control Graft | Synthesis Heart: 2 Manyfold Stock + Adaptive Membrane + Precision Membrane + 2 Binder + 500 BU → 1 | Upgrades I119's **settings-only** changeover to the armor's field-control procedure. No anatomy exchange; distinct from block T9-05 Reciprocal Graft. |
| I123 | Living Encounter Trophy [optional cosmetic] | One declared victory reward, not culture output | Display only; never another armor stat tier or required repeat-kill currency. Visual design remains open. |

## 8. Block items, fluids and non-items

Every recoverable block in [Block Catalog](BLOCK_CATALOG.md) has its own placeable block item, under that catalog's exact name and T-ID. This includes cores, attachments, all vein/tissue forms, storage cells, native foundations and decorative shapes. Their recipes are not duplicated here. The item renders the block's model at inventory scale. Organ Bud (I001/T0-16) and Compound Organ Core (I100) explicitly alias those block items.

Exceptions: **Thermal Mantle (T3-09), Spoil Sorter (T4-14) and Service Pedestal (T6-09) name assembled bodies/layouts, not additional block items.** Recover their actual installed parts and existing working cores. Other assemblies recover their distinct controller where specified plus installed components; dismantling never returns both a boxed whole machine and its parts. Leached Rock drops its recorded original host material, not a universal leached-rock item. Sapping Bush Cuttings, Foldroot Cuttings, Reed Segments and Glassbloom Seeds are planting items, not a second free block alongside a seed. Canopy Cyst drops only its retained product/body, never a second complete harvest.

### Workpieces are not another ore loot roll

| Work Bed recipe/state | What is consumed or retained | Allowed next operation |
|---|---|---|
| Intact ore presentation | Grasping Root moves one supported real block; no drops yet. Alternatively the player places one actual ore-block item. | Direct fracture for the ordinary permitted yield, or optional chemical treatment. The cutting grade and supported block list still apply. |
| Intact ore conditioning | One Mineral Penetrant + 25 BU over a proposed 40 s → one conditioned specimen + one Spent Penetrant. Lock the specimen and reserve waste before starting. | Jaw fractures to I108. Already treated specimens refuse another dose; they cannot return as pristine ore. |
| Host-rock weakening | One Leaching Nodule on one supported held host block → Leached Rock, without ore or additional rock drops. The manual exposed-face recipe remains valid. | Player/tool or jaw performs the actual break. Leaching and ore conditioning are separate recipes. |
| Timber grafting | One ordinary supported log + one Rooting Gel + 25 BU → one Living Wood block, no separate wood byproduct. | Release for construction or a declared wood recipe. This treats a block; it does not create a living tree without planting/growth services. |

One source block has one owner throughout extraction, treatment and fracture. Bed visuals represent that held workpiece; breaking the visible specimen uses the bed's guarded operation, not an independent vanilla loot table. Normal dismantling/relocation of an occupied bed retains **one** bounded workpiece or its real item batch in the recovered bed, cancels stale links and never also drops the represented ore. Reject arbitrary block-entity contents and nested occupied work beds; do not serialize a piece of the world into an unrestricted item.

An untreated hand-placed ore item may be recovered unchanged. Field extraction deliberately introduces intact handling at T2; whether it may export ordinary Silk-Touch ore items is **not** implicitly approved. The proposed baseline keeps field-extracted specimens in beds or Mineral Workpieces until fracture; ordinary logistics can move the wrapped workpiece between workshops without erasing its state. A tool-breaking fallback for a conditioned specimen returns the same accounted fragments, never Fortune loot plus chemical recovery. Every ore recipe must compare direct mining, Fortune, Silk Touch and supported external processing before its yields are approved. If a modded source lacks a safe state/loot mapping, refuse it rather than guess.

Fusion level, activity-counter value, learning capacity, a genome's coverage, world phase, pressure, electricity, steam and network packets are not inventory items. The green Aerocyte projectile is an entity/effect made by spending biomass; it is not free collectible ammunition.

| Process fluid | Producer | Consumer / accounting |
|---|---|---|
| Biomass | Digestive Sac or actual successful Digestive Tissue collection | Feed and listed activation recipes; buckets/ampoules contain measured real volume. |
| Spent wash fluid | Washing Kidney | Recovery/neutralization; output space reserved before mineral consumption. |
| Dirty process water | Cooling, animal/chemical and waste recipes | Declared filtration/recovery route; not automatically clean water. |
| Condensate | Steam work/cooling | Boiler feed after allowed treatment; never simultaneously retained steam energy. |
| Genetic suspension | Extractor/Vat process state | Source-specific stock/separation; not a bucket of arbitrary DNA effects. |

Bucket forms for non-biomass custom fluids are deferred until their finite world-spill semantics are approved. A tank transfer does not require a world-placeable fluid block. Source-specific reagents, concentrates, fragments and grafts use a finite registered recipe/schema set, not unrestricted user-created variants.

## 9. Recipe and inventory acceptance checks

- Every prepared item has a producer and at least one named consumer; recipes display both in JEI and the guide.
- Refused/incompatible targets consume nothing. Preparing a graft does not promise that the selected piece meets its permanent lineage, level, genome and anatomy requirements.
- Reserve all solid/fluid outputs, returned containers and the target before a batch starts. Full outputs pause without loose item drops; cancellation/restart cannot produce both result and ingredients.
- No ore/ingot reprocessing multiplier, stock-to-mineral-loot recipe, potion bottle duplication, duplicated learning points or removable Elytra-plus-wings output.
- Inventory metadata has fixed fields and hard size limits. No per-tick decay across every stored item, unbounded recipe expansion or nested portable factory/world snapshots.
