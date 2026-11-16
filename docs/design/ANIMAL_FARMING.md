# Animal farming

Status: proposed. This document is the complete picture of animal products: a
ranch of real animals, then cultured tissue grown from their DNA. It is
self-contained. Every number is a starting value, not measured balance.

Nothing here is shipped. The ranch reuses Mouths, Item and Fluid Veins and
storage. The cultured route depends on the DNA Bank and genomes, which are
planned.

## Principles

1. There are two routes. The ranch keeps real animals and takes real products
   from them. The culture route grows meat in an organ once the species genome
   is complete.
2. The ranch comes first and feeds the second route: culling supplies the DNA
   samples that complete a genome.
3. Each stage removes one chore and adds one planning problem.
4. Animals are entities, so a ranch is capped. The culture route has no
   entities and is the one that scales.
5. Culture grows meat only. Milk, wool, eggs, leather and feathers always need
   live animals, so the ranch stays useful.
6. No food loop is free. Breeding spends real breeding food. Cultured meat
   costs more biomass than digesting it returns.
7. Products go into buffers, never onto the ground. A full buffer pauses work.
8. Nothing spawns without a free berth in a pen.

## The whole picture

| Job | 0 Hand | 1 Fed pen | 2 Collected pen | 3 Ranch | 4 Tissue culture |
|---|---|---|---|---|---|
| Feed and breed | Player | Feeding Trough | Feeding Trough | Feeding Trough | No animals |
| Hold population | Player | Trough target | Trough target | Trough target and culling | — |
| Milk, shear, gather eggs | Player | Player | Milking Lobe, Shearing Lobe, Nest Basket | Same | Not available |
| Slaughter | Player | Player | Player | Culling Lobe | Not needed |
| Meat | Player | Player | Player | Culling Lobe | Flesh Vat |
| DNA samples | Player | Player | Player | Culling Lobe, one per cull | Genome already complete |
| Carry | Player | Player | Veins | Veins | Veins |
| Power | — | Biomass | Biomass | Biomass | Biomass, water, culture feed |

Where products go:

```text
crops → Feeding Trough → pen ─┬─ milk, wool, eggs  → storage, kitchen, crafting
                              ├─ meat, leather     → storage, kitchen, crafting
                              └─ DNA samples       → Specimen Extractor → DNA Bank
                                                                             │ genome complete
biomass + water + culture feed → Flesh Vat ←─────────────────────────────────┘
                                     └─ raw meat → kitchen, storage
```

## Stage 0 — Hand husbandry

Vanilla breeding, shearing, milking and slaughter.

## Stage 1 — Fed pen

Feeding and breeding are automated. Everything else is manual.

- **Pen:** a Feeding Trough defines it: the enclosed floor area around the
  trough, at most 9×9. The player builds the fence. One species per pen: cow,
  sheep, pig, chicken or rabbit.
- **Target:** the player sets a population target, at most 12 animals.
- **Work:** every 10 seconds the trough counts animals in the pen. Below the
  target, it feeds one ready pair their normal breeding food from its
  inventory. At the target it spends nothing.
- **Supply:** breeding food by hand or by Mouth. A small biomass cost per
  feeding.
- **Reserve:** the trough never feeds if that would leave fewer than two
  adults.

Done when: a pen started with two cows reaches its target from supplied wheat
and then stops consuming wheat.

## Stage 2 — Collected pen

Non-lethal products are collected without the player.

| Organ | Animal | Takes | Gives | Rate |
|---|---|---|---|---|
| Milking Lobe | Cow, goat | Empty bucket or a Fluid Vein | Milk | One per animal per 5 minutes |
| Shearing Lobe | Sheep | Shears, which wear | Wool, by the sheep's colour | When the wool has regrown |
| Nest Basket | Chicken | — | Eggs laid inside the pen | As laid |

- Each organ stands on the pen edge, works on an animal within two blocks, and
  holds its product in a one-stack buffer.
- Each costs biomass per collection.
- A Nest Basket set to hatch turns an egg into a chick, but only while the pen
  is below its target.

Done when: a sheep pen delivers sorted wool to storage and a cow pen delivers
milk, with no player action, and each stops cleanly on a full buffer.

## Stage 3 — Ranch

The pen produces meat and hides on its own, and becomes a source of DNA.

- **Organ:** a Culling Lobe on the pen edge.
- **Work:** when the pen holds more adults than the player's keep number, the
  lobe kills one adult within two blocks, at most one per 30 seconds. The
  animal's ordinary loot goes into its buffer. No experience is produced.
- **Samples:** each cull also yields one DNA sample of that species.
- **Balance:** the trough's target and the lobe's keep number set the herd.
  Target above keep gives a steady flow. The lobe never cuts below two adults.
- **Limits:** at most four pens within one chunk. A pen over its entity cap
  stops breeding before it stops culling.

The planning problems are the feed supply from the crop farm, the split of
crops between animals, digestion and the player, and the breeding reserve.

Done when: a cow ranch fed by an automated wheat plot delivers beef and leather
to storage for one hour, holds its herd between keep and target, and recovers
after its wheat supply is cut and restored.

## Stage 4 — Tissue culture

Meat without animals. This is the scalable route.

- **Requirement:** the species genome is complete in the DNA Bank. Samples come
  from stage 3 culls or from hunting.
- **Organ:** a Flesh Vat connected to the bank.
- **Recipe:** one Genetic Stock of the species as a retained seed, plus culture
  feed, water and biomass, gives raw meat of that species every 60 seconds. The
  seed is not consumed.
- **Species:** beef, pork, mutton, chicken and rabbit. Cod and salmon follow
  with the fish genomes.
- **Cost:** by principle 6, the biomass and feed for one meat exceed what one
  meat digests into. A vat is for food and recipes, not fuel.
- **Trade-off against the ranch:** no pen, no entity cap and steady output, but
  it needs the laboratory and a continuous supply, and it gives meat only.
- **Pauses, with a reason:** no feed, water or biomass; full buffer; the bank
  disconnected.

Done when: with a complete cow genome, a supplied vat delivers beef to storage
with no cow loaded anywhere, stops when the bank is disconnected, and resumes.

## Later extensions

Not designed yet.

| Extension | New problem |
|---|---|
| Fish and aquatic pens | A water habitat instead of a fenced floor |
| Bees | Hives, flowers and honey without angering the hive |
| Nether animals: hoglins, striders | Hostile or lava-bound stock; warped fungus as control |
| Cultured hides or wool | Whether culture should ever replace the ranch for these |

## Mutations and upgrades

| Change | Permits | Continuing cost | From stage |
|---|---|---|---|
| Species genome on the trough | Feeding only animals ready to breed, wasting no food | The genome, once | 1 |
| Second lobe on a pen | Double collection or culling rate | Double biomass and export | 2 |
| Careful culling | A second DNA sample per cull | More biomass per cull | 3 |
| Vat expansion body | More meat per cycle | More feed, water and biomass | 4 |

Growth improves an organ's rate or economy.

## Engine rules

Breeding, growing up, wool regrowth and egg laying are vanilla behavior. Culling
uses the animal's own loot table once. Pen scans are bounded to the pen area on
a fixed interval. No pen loads chunks or works in unloaded ones.
