# Item catalog and biological ingredient preparation

Catalog of the proposed mod-owned items, grouped by first availability. This covers the current block, tissue, armor and progression designs, including long-term candidates; it is not a claim that these items are implemented or all belong in the first release. Names and recipe quantities are proposals. Existing vanilla items remain vanilla items.

**This document owns item identity, preparation recipes and consumable forms.** [Block Catalog](BLOCK_CATALOG.md) owns organs and their construction; [Armor Evolution](ARMOR_EVOLUTION.md) owns equipment counters, permanent branches and effects; [Substrate Mutations](LIVING_SUBSTRATE_MUTATIONS.md) owns tissue anatomy. [Guide Tree](GUIDE_PROGRESSION_TREE.md) gives discovery order. T0–T9 mean availability, not an item's earned level.

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
| I011 | Empty Sample Vial | Craft 1 glass bottle + 1 paper → 1 | One source-labeled specimen; later extractor handling. Returned after extraction where the recipe preserves the vial. |
| I012 | Field Lancet | Craft 1 flint + 1 bone + 1 Membrane Sheet → 1 | Manual sampling of permitted reachable plants/creatures, with vial and disclosed target damage/cooldown. No mob-loot generation. |
| I013 | Dormant Hood / Thorax / Legwraps / Treads | Four distinct armor items: 5 / 8 / 7 / 4 rotten flesh in vanilla armor silhouettes | One piece awakened in Cradle using Culture, Membrane Sheet and 100 BU. |
| I014 | Living Hood / Thorax / Legwraps / Treads | Awaken the corresponding existing dormant piece; empty fuel, preserved wear percentage | Four persistent equipment identities; full material and mutation tree in Armor Evolution. Frame variants are item data, not dozens of interchangeable crafting products. |
| I015 | Dormant Pick / Hatchet / Spade / Hoe | Familiar tool patterns with Bone Plates as heads and sticks as handles; proposed durability/harvest limits require the tool specification | Manual work; awaken one in Cradle using Culture, Sheet and 100 BU. |
| I016 | Living Pick / Hatchet / Spade / Hoe | Awaken corresponding I015 tool | Persistent tool counterparts. Mineral fusion must respect real harvest tier; area/precision branches remain proposed, not approved damage or yield rules. |
| I017 | Dormant Fang | 2 Bone Plates + 1 stick → 1 | Basic weak melee weapon; same cradle awakening route as tools. |
| I018 | Living Fang | Awaken I017 | Base for separately designed piercing/control weapon branches. |
| I050 | Skeletal Graft | Bone Loom: 1 Bone Plate + 1 Binder + 25 BU → 1 | Reinforce one cell or permitted organ skeleton; the plate is not duplicated. |
| I051 | Mutation Graft | Host rank I at T0; genetic/higher grafts from T2. Typed family under “Mutation grafts” below. | Installed once; target, equipment slot and rank are explicit. An installed armor graft cannot be extracted. |
| I019 | Biomass Bucket | Fill a vanilla bucket with exactly 1,000 mB from a compatible store | Real fluid transfer. Until finite spill rules are approved, transfer to compatible tanks only, not free world-fluid placement. |

## 3. T1 — active ingredients and tissue treatments

| ID | Item | Obtain / batch recipe | Consumed by or used for |
|---|---|---|---|
| I020 | Resin | Sap Tap: one supplied tree's allocated growth harvest → one resin portion | Seals and adhesive culture. Exact tree growth budget belongs to the ecology specification; tapping is not free wood plus free biomass plus free resin. |
| I021 | Capillary Gel | Cyst: 1 Elastic Gel + 1 Binder + 50 BU → 2 | Biomass Vein graft; Nutrient Intake; organic pumping upgrades. |
| I022 | Filter Membrane | Cyst: 1 sand + 1 Sheet + 50 mB water + 25 BU → 1 | Washing/filtration attachments and Fluid Vein treatment. Sand becomes embedded filter material. |
| I023 | Contractile Fiber | Cyst: 1 string + 1 leather + 1 Binder + 50 BU → 2 | Item Veins, terrain stitches, transport-organ upgrades. |
| I024 | Synaptic Gel | Cyst: 1 redstone dust + 1 string + 1 Binder + 50 BU → 2 | Nerve Tissue and control treatments. Not electrical fuel. |
| I026 | Digestive Enzyme | Cyst: 1 fermented spider eye + 1 Elastic Gel + 1 Binder + 50 BU → 2 | Digestive Tissue; digestive-organ processing upgrades. No DNA required for first simple floor. |
| I027 | Locomotor Gel | Cyst: 1 sugar + 1 rabbit hide + 1 Binder + 50 BU → 2 | Travel Tissue; later DNA-specific acceleration treatments. |
| I028 | Sealing Resin | Cyst: 1 resin or Elastic Gel + 1 Sheet + 25 BU → 2 | Chemical lining; sealed containers. Does not provide thermal/spatial protection. |
| I029 | Char Gland Feed | Bowl/Cyst: 1 charcoal + 1 Binder + 25 BU → 1 | Unlock solid-fuel feeding on a sufficiently practiced Bio-Furnace; operating charcoal is still consumed separately. |
| I030 | Honey Culture | Bowl/Cyst: 1 honey bottle + 1 Culture → 2, returning bottle | Honey-feed adaptation, first Cradle economy treatment. It is not a free healing potion. |
| I033 | Rooting Gel | Bowl/Cyst: 1 wheat seed + 1 Culture + 50 mB water → 2 | Selected crop-bed preparation, bush/tree graft attachment. Planting still needs real seed/sapling/cutting. |
| I034 | Sapping Bush Cutting | One reserved cutting from deliberate bush conversion or a mature propagation harvest | Replant T1-25; conversion yields at most one retained starter, not repeated berry drops. |
| I035 | Canopy Pod | Arbor/Canopy Cyst fills one pod from an assigned harvest allocation | Holds one declared resin, fiber or fruit batch. Open once; no second harvest from breaking its empty shell. |
| I036 | Sealed Biomass Ampoule | Empty: 1 Sheet + 1 Sealing Resin → 1; fill from storage up to 250 mB | Portable armor refueling. Partial contents remain on the same unstackable item; no automatic drinking or spilling. |
| I037 | Route Imprint | Probe copies a bounded configuration onto 1 paper + 1 Synaptic Gel | Applies port/route settings to compatible owned tissue; never contains items, power, research or chunk-loading permission. |

Early Lumen treatment is made in the Bowl; all T1 transport treatments use the Cyst. Its own construction requires only a Bud, Sheets and a Plate, so a first Cyst needs no transport graft. A player can hand-feed it before automating it.

## 4. T2 — genetics, prepared grafts, food and helpers

| ID | Item or bounded family | Obtain / recipe | Consumer / constraints |
|---|---|---|---|
| I031 | Survey Gel | Cyst, SR2 recipe: 1 amethyst shard + 1 bone meal + 1 Binder + 25 BU → 2 | Surveyed Tissue and visible finite mining markers. |
| I032 | Waymark Secretion | Cyst, SR2 recipe: 1 paper + 1 Contractile Fiber + 25 BU → 2 | Worker Waypoints; linking remains a configuration action. |
| I040 | Labeled Specimen | Lancet or approved harvest puts one real source sample in I011 | Extractor; immutable species/source category, bounded quality fields. A generic rotten flesh item is not automatically a high-quality enderman sample. |
| I041 | Genome Fragment | Extractor consumes specimen + 50 mB water + 25 BU → one fragment and one matching stock seed | DNA Bank records actual coverage; repeated covered region gives no invented new region. Quality/coverage tables remain source-specific. |
| I042 | Genetic Stock | Extraction above; culture: 1 stock seed + 2 source-compatible feed portions + 100 BU → 3 stock, with complete matching genome | Two net new stocks; species retained. Does not produce pearls, stars, bones or minerals. Plant feed uses actual planting/produce; animal culture uses compatible protein feed. |
| I043 | Memory Sample | Memory Gland removes a selected typed amount into a vial, consuming 50 BU | One compatible organ/item learns that amount at donor's expense. Armor slot, counter and permanent lineage restrictions survive transfer. |
| I044 | Genome Record | Export a known source record to 1 paper + 1 Synaptic Gel at DNA Bank | Knowledge sharing under permission rules; neither physical stock nor transferable practice. |
| I045 | Antitoxin Serum | Cyst: 250 mB milk + 1 Filter Membrane + 50 BU → 1 | H5 graft preparation; process 1 milk bucket as four batches, returning one bucket, never four buckets. |
| I046 | Scent Concentrate | Cyst: 1 rotten flesh + 1 Binder + 25 BU → 2 | H7 grafts; source-specific repellent/lure recipes additionally require their declared scent/food. |
| I047 | Restorative Serum | Cyst: 1 golden apple + 1 honey bottle + 1 Binder + 100 BU → 2, returning bottle | Repair Dock mutation and Restorative Tissue, not directly drunk as a universal cure. |
| I048 | Restraining Graft | Chrysalis: 2 spider stock + 1 cobweb + 1 Elastic Gel + 1 Binder + 100 BU → 1 | One mature Restraining Tissue cell; spider genome required. |
| I049 | Aquaculture Graft | Cyst: 1 kelp + 1 prismarine shard + 1 Binder + 50 BU → 1 | One submerged mature tissue cell. Creature production still requires brood stock. |
| I052 | Plant Graft | Grafting Bench: 1 seed/sapling + 2 matching stock + 1 Rooting Gel + 100 BU → 1 modified planting item | Retains species and chosen trait. No conversion of wheat into an unrelated tree species. |
| I053 | Organ Trait Graft | Chrysalis: 2 matching stock + 1 listed active signature ingredient + 1 Binder + 100 BU → 1 named organ treatment | Applies only the organ's listed trait after its counter gate; no armor respecialization. Exact higher-rank recipe belongs with that trait. |
| I054 | Protein Culture Feed | Cyst: 1 raw fish or rotten flesh + 1 Nutrient Mash + 50 mB water → 2 | Animal-stock recipes, larvae. Not edible player food. |
| I055 | Expedition Ration | Ration Kitchen: 1 cooked beef + 1 baked potato + 1 carrot → 1 ration | Proposed 10 hunger, 12 saturation; real food for player or Feeding Lobe. No extra potion effect. |
| I056 | Travel Ration | Kitchen: 1 bread + 1 cooked fish + 1 honey bottle → 1, returning bottle | Proposed 8 hunger, 10 saturation; lighter farm recipe, not free speed mutation. |
| I057 | Mineral Concentrate | Gizzard recipe for a named ore → accounted recoverable mineral + rock residue | Kidney/smelting. Source-specific yields must be specified before implementation; never feed an ingot through an ore-bonus recipe. |
| I058 | Washed Concentrate | Kidney washes I057 with a declared water dose | Smelting/Separator; mineral removed into tailings is deducted from this output. |
| I059 | Mineral Tailings | Washing byproduct with bounded source/grade | Separator may recover only its remaining mineral fraction. Spent tailings are inert; no repeat extraction. |
| I060 | Rock Residue | Gizzard's accounted non-mineral fraction | Building aggregate or deliberate disposal, not a second ore product or organic biomass feed. |
| I061 | Washed Iron / Gold / Diamond / Obsidian Portion | Four distinct forms: corresponding ingot/gem/block → Gizzard granulation → Kidney with 100 mB water | One cleaned portion and 100 mB spent wash per source item. Fusion media below, no yield multiplier. |
| I062 | Ferrocyte Paste | Vat: washed iron + Binder + 100 BU → 1 dose, 30 s | Iron armor frame and structural organ treatments. |
| I063 | Auric Myelin | Vat: washed gold + Binder + 150 BU → 1 dose, 45 s | Auric frame, pump/control graft signatures. |
| I064 | Larval Cyst [optional] | Nursery: 1 egg + 2 Protein Feed + 250 BU → 1 retained larval cyst | Hatch only with a free reserved berth; at most three local workers in the first nursery proposal. Cyst and larva never exist simultaneously. |
| I065 | Courier Graft [optional] | Chrysalis: 2 bee stock + 1 Contractile Fiber + Binder + 100 BU → 1 | Mutate a real larva into a delivery helper. No spawn egg or additional population allowance. |
| I066 | Harvester Graft [optional] | Chrysalis: 2 cow stock + 1 Rooting Gel + Binder + 100 BU → 1 | Larval harvest profession; actual harvesting tool and destination required. |
| I067 | Sail Cyst [optional] | Nursery: 1 Larval Cyst + 1 feather + 2 Sheets + 500 BU → 1 | One Nutrient Sail at a free roost. No automatic chunk loading. |
| I068 | Grown Spine | Bone Loom: 1 Bone Plate + 25 BU → 8 spines | Spine Sentry / ranged weapons; shots consume ammunition and do not drop reusable infinite spines. |
| I038 | Piercing Fang / Grasping Limb / Spitter [proposed weapon forms] | Chrysalis prepares a named weapon graft from 2 relevant stock (skeleton / spider / slime), Bone Plate / Contractile Fiber / Digestive Enzyme respectively, Binder and 100 BU; Chamber applies it to one Living Fang for 100 BU | Three different resulting weapon bodies, not three simultaneous modes. Spitter consumes real Grown Spines plus fuel. Exact damage, counter gates and future branch policy require the tool/weapon specification; these are optional guide leaves. |
| I069 | Growth Template | Structure Grower records a player-selected bounded layout onto paper + Route Imprint | Placement still consumes real blocks; no copied organ contents/counters. |

### Mutation grafts

I051 is a **closed family**, not arbitrary NBT-programmable effects. Its allowed members are exactly the armor rows **M1–M4, H1–H10, C1–C12, L1–L8 and B1–B8**, ranks I–III. Common M grafts specify their destination slot; other letters already identify the slot. Each member uses the DNA, prepared signatures, amounts and access gates in [Armor Evolution §6–10](ARMOR_EVOLUTION.md#6-installing-mutations). A tooltip names the exact outcome, not “mutation essence.”

The same family rule does not invent tool/weapon effects: their grafts require separately approved recipes. A treatment to advance an already committed branch is not a removable upgrade module. Item catalogs may list prepared grafts; there is deliberately **no Recovered Armor Graft** item.

Host rank I can be cultured in the Bowl and applied at the Cradle. Other ranks use the Chrysalis and Chamber. Elytral Wings I reserves an actual Elytra along with prepared materials; it cannot be grown solely from phantom stock. A cancelled batch cannot return both the Elytra and a finished wing graft.

## 5. T3–T4 — thermal and precision ingredients

| ID | Item | Preparation | Consumer |
|---|---|---|---|
| I025 | Conductive Myelin | Cyst, SR4 recipe: 1 copper ingot + 1 redstone dust + 1 Binder + 100 BU → 2 | Conductive Tissue; no raw copper directly installed as a mutation. |
| I070 | Thermal Seed | Cyst: 1 magma cream + 1 netherrack + 1 Binder + 50 BU → 1 | Mature selected substrate in the Nether into Thermal Substrate. Its ingredients can travel, native maturation cannot. |
| I071 | Thermal Lining | Thermal Nursery: 1 Sheet + 1 magma cream + 100 BU, with local Nether heat → 1 | Native thermal recipes, steam and heat lining, armor signatures. |
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
| I104 | Prepared Genetic Suspension | Vat, SR4: 2 matching Genetic Stock + Filter Membrane + Thermal Lining + 100 mB water + 50 BU → 1 sealed source-specific batch | Ion Separator precision-genetics mode. Recovery never exceeds the two consumed stock portions; no generic loot reagent. |
| I105 | Bioactive Leaching Charge | Leaching Gland with Thermal Mantle heat, SR3: 4 Leaching Nodules + Thermal Lining + 200 BU → 1 | Digestion Crucible's finite host-rock batch. Proposed at most 36 eligible host cells; contains no mined material before use. |
| I106 | Spent Leach Cake | Crucible retains the spent I105 treatment separately from accounted recovered host residue | Neutralization/disposal. No second terrain harvest or full-charge regeneration; the internal treatment medium is not a world-placeable fluid. |

These ingredients have useful non-armor consumers. Prepared iron reinforces load-bearing organs; gold supports pumps and signals; diamond supports cutting and precise fibers. Higher mineral yield, counter-cap changes and new processing recipes remain separate explicit organ mutations, not automatic consequences of feeding a better metal.

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
| I118 | Contained Foreign Specimen | Foreign Specimen Cocoon's approved specimen recovery | Precision sequencing of its real source; not a portable mob prison of unbounded NBT. |
| I119 | Adaptive Interface Graft | Chrysalis in supplied Fold habitat: 2 Gel + Thermal Lining + Precision Membrane + 4 Binder + 1,000 BU → 1 | Two-slot armor control-presets interface; same installed compatible anatomy in both presets. |
| I120 | Manyfold Tissue Sample | First valid boss victory guarantees enough sample material for its finite genome coverage and a retained culture seed | Precision Sequencer/Pattern Incubator. No mandatory repeated boss kills for random missing pages. Exact batch count follows the boss specification. |
| I121 | Manyfold Genetic Stock | Pattern Incubator: completed genome + sample seed/stock + Gel + Protein Feed + 200 BU → 3 stock per consumed stock seed | Net two renewable stocks; does not duplicate boss loot or unique trophies. |
| I122 | Reciprocal Control Graft | Synthesis Heart: 2 Manyfold Stock + Adaptive Membrane + Precision Membrane + 2 Binder + 500 BU → 1 | Upgrades I119's **settings-only** changeover to the armor's field-control procedure. No anatomy exchange; distinct from block T9-05 Reciprocal Graft. |
| I123 | Living Encounter Trophy [optional cosmetic] | One declared victory reward, not culture output | Display only; never another armor stat tier or required repeat-kill currency. Visual design remains open. |

## 8. Block items, fluids and non-items

Every recoverable block in [Block Catalog](BLOCK_CATALOG.md) has its own placeable block item, under that catalog's exact name and T-ID. This includes cores, attachments, all vein/tissue forms, storage cells, native foundations and decorative shapes. Their recipes are not duplicated here. The item renders the block's model at inventory scale. Organ Bud (I001/T0-16) and Compound Organ Core (I100) explicitly alias those block items.

Exceptions: Leached Rock is a temporary world-state replacement and drops its recorded original host material when mined, not a craftable universal leached-rock item. Multi-cell organ bodies have one recovered core history; shell pieces never clone it. Sapping Bush Cuttings, Foldroot Cuttings, Reed Segments and Glassbloom Seeds are their planting items, not a second free block alongside the seed. Canopy Cyst drops only its retained product/body as declared, never a second complete harvest.

Frame grade, counter rank, a genome's coverage, world phase, pressure, electricity, steam and network packets are not inventory items. The green Aerocyte projectile is an entity/effect made by spending biomass; it is not free collectible ammunition.

| Process fluid | Producer | Consumer / accounting |
|---|---|---|
| Biomass | Digestive Sac or actual successful Digestive Tissue collection | Feed and listed activation recipes; buckets/ampoules contain measured real volume. |
| Spent wash fluid | Washing Kidney | Recovery/neutralization; output space reserved before mineral consumption. |
| Dirty process water | Cooling, animal/chemical and waste recipes | Declared filtration/recovery route; not automatically clean water. |
| Condensate | Steam work/cooling | Boiler feed after allowed treatment; never simultaneously retained steam energy. |
| Genetic suspension | Extractor/Vat process state | Source-specific stock/separation; not a bucket of arbitrary DNA effects. |

Bucket forms for non-biomass custom fluids are deferred until their finite world-spill semantics are approved. A tank transfer does not require a world-placeable fluid block. Source-specific reagents, concentrates, fragments and grafts use a finite registered recipe/schema set, not unrestricted user-created variants.

## 9. How existing upgrade recipes read

| Old shorthand | Actual consumable now | Preparation / placement rule |
|---|---|---|
| Glow ink / glow berries / glowstone for light | Lumen Secretion | Bowl/Cyst; apply to mature tissue, or use as H2 graft signature. Cosmetic glow-ink markings may still use ink directly. |
| Slime ball for elasticity / transport | Elastic Gel / Capillary Gel | Elastic Gel for mechanical flexibility; Capillary Gel specifically for biomass transport. |
| String + leather for Item Vein | Contractile Fiber | One dose per mature cell. |
| Glass bottle + Sheet for Fluid Vein | Filter Membrane + Sealing Resin | One each per mature cell; no empty bottle installed as a functional organ. |
| Redstone + string for nerve | Synaptic Gel | One dose per mature cell. |
| Copper + redstone for conduction | Conductive Myelin | SR4 treatment; one dose per mature cell. |
| Fermented eye + slime for digestive floor | Digestive Enzyme | One dose per mature cell. |
| Sugar + rabbit hide for travel floor | Locomotor Gel | One dose per mature cell; suit eligibility remains independent. |
| Golden apple + honey for restoration | Restorative Serum | One dose per cell; two doses for Repair Dock conversion. |
| Bone / iron / diamond reinforcement | Skeletal Graft / Ferrocyte Paste / declared diamond medium | Use the target's listed dose; no direct raw mineral mutation. |
| Magma cream + netherrack / End ingredients | Thermal Seed / Anchor Seed | Grow the selected native bed in its own dimension. |
| Generic creature ingredient for better behavior | Named Organ Trait Graft | Complete matching DNA, physical stock and the behavior's prepared signature; not every dropped mob item carries complete DNA. |

Construction recipes still list physical components, not these replacements unless explicitly marked **mutate**, **graft** or **upgrade**. Operating food, fuel, potion ingredients and cosmetic dyes remain valid direct supplies. This distinction avoids requiring an advanced activation machine merely to build its own basic body.

## 10. Recipe and inventory acceptance checks

- Every prepared item has a producer and at least one named consumer; recipes display both in JEI and the guide.
- Refused/incompatible targets consume nothing. Preparing a graft does not promise that the selected piece meets its permanent lineage, level, genome and anatomy requirements.
- Reserve all solid/fluid outputs, returned containers and the target before a batch starts. Full outputs pause without loose item drops; cancellation/restart cannot produce both result and ingredients.
- No ore/ingot reprocessing multiplier, stock-to-mineral-loot recipe, potion bottle duplication, copied Memory Sample or removable Elytra-plus-wings output.
- Inventory metadata has fixed fields and hard size limits. No per-tick decay across every stored item, unbounded recipe expansion or nested portable factory/world snapshots.
- Before implementation, publish exact yields for each ore, ecology source, target mutation and tool branch still marked proposed here. Catalog coverage is not evidence that these balances are already complete.
