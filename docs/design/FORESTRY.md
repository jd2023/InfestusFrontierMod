# Forestry

Status: proposed. This document is the complete picture of wood production: from
cutting a tree by hand to a managed living grove. It is self-contained. Every
number is a starting value, not measured balance.

Shipped today: substrate growth infects standing trees into Infected Logs and
Blighted Leaves. That is a one-time conversion of dead timber, not a farm.
Cultivation Tissue, the Planting Proboscis, the Harvest Corolla, veins, Mouths
and the Digestive Sac exist and are reused here.

## Principles

1. Wood is renewable only through real trees that grow, are cut and are
   replanted or regrown. Nothing produces logs from biomass alone.
2. Each stage removes one chore and adds one planning problem.
3. Vanilla trees grow for free but need space, light and time. Managed living
   trees grow faster and yield special products but must be fed.
4. A fed tree costs more biomass than digesting its wood returns. Managed trees
   make materials, not a fuel loop. Free-growing groves are the fuel source.
5. Every tree job is bounded: at most 128 logs and 512 leaves inside a 17×32×17
   envelope, the limits tree infection already uses. Larger trees are refused
   whole, never cut in part.
6. Cutting puts drops into a buffer, never on the ground.
7. Earlier stages stay useful. A sapling grove needs no laboratory.

## The whole picture

| Job | 0 Hand | 1 Sapling grove | 2 Felling grove | 3 Living grove | 4 Native groves |
|---|---|---|---|---|---|
| Plant | Player | Planting Proboscis | Planting Proboscis | Once; the tree is kept | Once |
| Grow | Vanilla | Vanilla | Vanilla | Arbor Root, fed | Arbor Root, fed and heated or powered |
| Cut | Player | Player | Felling Corolla | Felling Corolla prunes | Felling Corolla prunes |
| Collect and carry | Player | Player | Mouths and Item Veins | Mouths and Item Veins | Mouths and Item Veins |
| Keep planting stock | Player | Player refills the pouch | Saplings from leaves return to the pouch | Not needed | Not needed |
| Power | — | — | Biomass | Biomass, water | Biomass, water, heat or electricity |
| Product | Logs | Logs | Logs, saplings, sticks, apples | Living Wood, resin, pods | Stems, chorus, local variants |

Where wood goes:

```text
grove → Item Veins → sorting ─┬─ logs        → building, planks, charcoal in the Bio-Furnace
                              ├─ surplus     → Digestive Sac → biomass (woody feed digests slowly)
                              ├─ saplings    → Seed Pouch → replanting
                              ├─ Living Wood → organ frames, reinforced Rib Frames
                              └─ resin       → binder in place of slime balls
```

## Stage 0 — Hand forestry

Axes and saplings. Infected trees give Infected Logs once, for the Capsule Stem.
One Infected Log treated with Rooting Gel becomes one Living Wood, which is the
first Living Wood the player owns.

## Stage 1 — Sapling grove

Replanting is automated. Cutting is not.

- **Layout:** Cultivation Tissue cells marked as tree cells, at least two blocks
  apart with clear air above. A plot holds at most 16 tree cells.
- **Planting:** a Planting Proboscis places saplings from a Seed Pouch on empty
  tree cells. One sapling per cell; the six single-sapling Overworld species.
  2×2 trees are not supported.
- **Growth:** vanilla, including bone meal. No biomass cost.
- **Cutting:** the player, with an axe.

Done when: a player fells a tree in the grove, walks away, and finds a sapling
replanted from the pouch without touching it.

## Stage 2 — Felling grove

The grove runs unattended and sustains its own planting stock.

- **Organ:** a Felling Corolla, grown from a Harvest Corolla with Bone Plates
  and an iron axe. One Corolla serves one plot.
- **Work:** when a tree cell holds a grown tree, the Corolla discovers the whole
  tree within the bounds above, then removes one log every second and its
  leaves four per second. Each block's ordinary drops go into its buffer.
  Biomass is spent per block removed.
- **Planting stock:** leaves drop saplings by their normal chance. The buffer
  exports through a Mouth; saplings are routed back to the Seed Pouch. A tree
  that drops none draws down the pouch reserve.
- **Pauses, with a reason:** full buffer, no biomass, a tree over the bounds,
  an unloaded chunk. A paused Corolla leaves the tree standing.
- **Limits:** one tree in work per Corolla. A one-stack buffer per item kind,
  four kinds.

The planning problems are the sapling reserve, the split of logs between
building, charcoal and digestion, and grove size against growth time.

Done when: a plot of four oaks runs for ten harvests with no player action, the
pouch never empties, and logs arrive in storage. Emptying the pouch on purpose
stops planting with a reason and nothing else breaks.

## Stage 3 — Living grove

A tree is kept alive and pruned instead of felled. It yields the mod's own
materials.

- **Arbor Root:** an organ placed against a trunk on substrate. It binds that
  one tree and converts its logs to Living Wood, one block per second, inside a
  9×9×24 volume. One existing log becomes one Living Wood.
- **Feed:** water and biomass by vein. An unfed tree stops growing; it does not
  die.
- **Growth budget:** the tree produces one growth point every 60 seconds while
  fed. The player divides the budget between three products:

| Product | Organ | Yield | Use |
|---|---|---|---|
| Wood | Arbor Root regrows pruned branches; a Felling Corolla prunes them | One Living Wood per growth point | Organ frames, reinforced Rib Frames |
| Resin | Sap Tap on the trunk, into a Fluid Cyst or Item Capsule | One resin per two growth points | Binder in place of slime balls |
| Pods | Canopy Cyst grafted onto the tree's leaves; a Harvest Corolla picks them | One pod per two growth points | Fruit or saplings of that species |

- **Limits:** one Arbor Root per tree. Several Sap Taps share the budget; they
  do not multiply it. The trunk and marked main branches are never pruned.
- **Trade-off:** no replanting and no bare ground, but a continuous feed line.
  By principle 4 the feed costs more than the wood's digestion value.

Done when: one fed tree supplies Living Wood, resin and pods at the chosen
split for ten minutes, stops when its feed is cut, and resumes when it returns.

## Stage 4 — Native groves

Each dimension grows its own wood so a base does not import logs.

| Place | Tree | Extra need | Product |
|---|---|---|---|
| Nether | Crimson and warped huge fungi on Thermal Substrate | Heat from a Thermal Root | Stems, wart blocks, shroomlight |
| End | Chorus plants on Anchored Substrate | Electrical power | Chorus fruit and flowers |
| Later dimensions | To be defined with the dimension | — | — |

Stages 1 and 2 apply to huge fungi with fungus as the planting stock and nylium
in place of dirt. Stage 3 applies through a species graft on the Arbor Root.

Done when: a Nether base replaces its own wood and fungus stock without an
Overworld delivery.

## Mutations and upgrades

| Change | Permits | Continuing cost | From stage |
|---|---|---|---|
| Species graft on the Arbor Root | Binding that species; its pod product | The species genome, once | 3 |
| Skeletal Graft on Living Wood | Reinforced Rib Frames | One graft per frame | 3 |
| Luminous canopy | Canopy Cysts that give light | Glow ink, once | 3 |
| Second Corolla on a plot | Two trees in work | Double biomass and export | 2 |

Growth improves an organ's rate or economy.

## Engine rules

Trees grow by vanilla sapling and fungus growth. Leaves keep vanilla decay.
Cutting uses each block's own loot table once. No job loads chunks or works in
unloaded ones.
