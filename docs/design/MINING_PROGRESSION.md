# Mining progression

Status: proposed. This document is the complete picture of mining: every stage
from the first hand-dug pit to an ore-seeking root, the organs each stage uses,
and how mined material reaches the workshop. It is self-contained. Every number
is a starting value, not measured balance.

## Principles

1. Mining is a progression. It starts manual and inefficient. Each stage removes
   one chore and adds one planning problem.
2. Mining removes real blocks and leaves passages the player can walk through.
3. More productive mining needs more planning: geometry, biomass and energy
   supply, output handling and the layout of specialized organs.
4. Mining uses tools, organs and multiblock assemblies. A controller never
   grants the function of a missing organ.
5. Mining yields each block's ordinary drops once. Ore multiplication belongs
   to ore processing.
6. Finding ore is the player's job until the last stage. Before it, the player
   chooses the geometry and the installation cuts only that.
7. A mine is also a tool for terraforming, underground building and exploration.
8. Every job is finite and player-authorized. Nothing scans, extends or loads
   chunks without a bound.
9. Earlier stages stay useful. A later method does not replace them.
10. Unloading never damages, kills or resets an organ. Save its state and job
    progress, pause while required chunks are unloaded, and resume when they load.

## The whole picture

Every mine does the same seven jobs. Progression changes who does each one.

| Job | 0 Hand tools | 1 Leaching Pod | 2 Tunnel Root | 3 Descending Root | 4 Galleries | 5 Bulk excavation | 6 Ore-sensing root |
|---|---|---|---|---|---|---|---|
| Find ore | Player | Player, seeing through leached rock | Player | Player | Player | Player | Root, inside an approved volume |
| Choose geometry | Player | Player places the pod | Player steers each step | Survey plan | Survey plan | Survey plan | Player approves a volume |
| Cut | Player | Player, by hand | Root head | Fracture Jaw | Several jaws | Boring Jaws | Boring Jaw |
| Collect | Player | Nothing drops | Player empties the head | Collection Cilia | Cilia per face | Cilia per face | Cilia |
| Carry out | Player | — | Player | Item Veins | Shared veins and junctions | Veins and buffers | Veins |
| Build access and light | Player | — | Root cells, optional light | Structure Grower | Structure Grower | Structure Grower | Structure Grower |
| Power | — | Loaded biomass | Hand-fed biomass | Biomass by vein | Biomass by vein | Steam and biomass | Electricity and biomass |

What leaves the mine is the same at every stage: ordinary drops, split into ore
and spoil.

```text
mine face → collection → Item Veins → sorting ─┬─ ore  → Mineral Gizzard → Washing Kidney → Bio-Furnace
                                               └─ spoil → storage → construction stock, or disposal
```

Ore processing is a separate factory with its own throughput. A mine that cuts
faster than the workshop accepts fills its buffers and pauses.

Where each stage becomes available:

| Stage | Needs |
|---|---|
| 1–2 | Starter workshop: Organ Buds, biomass, Bone Plates |
| 3–4 | Veins, Mouths and storage; the mutation laboratory |
| 5 | A Nether base producing heat and steam |
| 6 | Nether genomes and electrical power |

## Stage 1 — Leaching Pod

A single-use organ for early exploration. It does not excavate. It weakens soft
ground so the player can dig a wide pit by hand and see the ore in it.

- **Use:** grow it from an Organ Bud on Living Substrate; the growth materials
  pay for its 16 charges. It cannot be picked up again.
- **Work:** each second it converts one horizontal layer below it, a disc 7
  blocks in diameter (at most 37 blocks). The first layer includes the substrate
  it stands on.
- **Eligible blocks:** members of a block tag `infestusfrontier:leachable` whose
  hardness is at most 1.5. The tag is built from vanilla tags: dirt, sand, base
  stone of the Overworld and Nether, plus gravel, sandstone and Living
  Substrate. The hardness cap excludes deepslate, so the pod is useless at
  depth. Ores, fluids, blocks with a block entity and everything outside the tag
  stay untouched.
- **Result:** each converted block becomes Leached Rock: translucent, breaks
  almost instantly by hand, drops nothing. A random share of blocks glow
  faintly. Leached Rock is solid, so no fluid moves until the player breaks it.
- **Cost and end:** a layer that converts at least one block uses one biomass
  charge. The pod holds 16 charges and never works more than 16 layers below
  itself. When the charges or the layers run out it dies and leaves a husk.
- **Unloaded chunks:** pause until the required chunks load, then resume from
  saved progress without repeating conversions or charges.

Done when: a pod planted on a substrate patch over stone leaves a pit the player
can clear with bare hands, with every ore block in it still in place and
visible before the player reaches it.

Implementation is cheap: one block entity, at most 37 block changes per second
for 16 seconds, no entities and no drops. Use a few blockstate glow variants
with low light levels, not per-block random light, and cull faces between
adjacent Leached Rock as glass does.

## Stage 2 — Tunnel Root

The first excavating organ. One head cuts a level tunnel one block wide and two
high. The player supplies every step.

- **Parts:** a Tunnel Root head and Root Segments. The head stands at the
  working face. Each step consumes one segment, which becomes a flush root cell
  in the tunnel floor behind the head. The root therefore runs the length of
  the tunnel.
- **Supply:** the player loads segments (up to 16) and biomass into the head by
  hand. It has no vein connection.
- **Work:** one step every 20 seconds. A step breaks the two blocks ahead, puts
  their ordinary drops in the head's buffer, lays one root cell and moves the
  head forward one block.
- **Steering:** before a step the player sets straight, left or right. The
  tunnel stays level and follows the four horizontal directions.
- **Cutting grade:** equal to a stone pickaxe. Harder blocks stop the head.
- **Limits:** 64 root cells per root. A one-stack buffer; the player empties it.
- **Pauses, with a reason shown on the head:** no segment or biomass; full
  buffer; a block above its grade or with a block entity; fluid touching the
  next cut; no floor ahead; a falling block above the cut; an unloaded chunk.
  The player fixes the cause and the head continues. Nothing is lost.

Done when: a player can drive a tunnel with at least one turn, walk its full
length, collect its drops, and recover from each pause reason.

## Stage 3 — Descending Root

The first automated mine. An assembly cuts a finite staircase from a surface
entrance to an underground landing and exports what it cuts. Each stage 2
chore is taken over by a separate organ.

| Stage 2 chore | Stage 3 organ | What it does | Player decision |
|---|---|---|---|
| Steering each step | Surveyed Tissue | Substrate cells that mark the approved section and show its preview | Entrance, direction, depth, stopping point |
| Starting each step | Descending Rootstock | Controller. Orders cut, collect and build for each step, then advances the root. Does none of that work itself | Which finite section to authorize next |
| The head's weak cutter | Fracture Jaw | Breaks one block in front of it at its cutting grade, into a Work Bed | Cutting grade, rate, number of jaws |
| Emptying the head | Work Bed and Collection Cilia | The bed holds cut material; cilia move it to a Mouth | Buffer size and collection rate |
| Feeding segments | Structure Grower | Places supplied root cells, treads and lights | Construction stock |
| Carrying biomass and drops | Mouths, Item and Biomass Veins, storage | Run along the root, supply the face and export drops | Ore routing, spoil capacity, reserves |

The Rootstock is a Tunnel Root head with the Stairs mutation, expanded into a
multiblock. It keeps the head's history. Installed organs keep their own
histories and upgrades.

The first Stairs genetic construct uses a completed pig genome and Overworld
materials. Applying it consumes the construct and biomass; the genome is kept.

The result is a lit staircase with three blocks of headroom over each tread.
One section descends at most 32 blocks. A new section needs a new approval.

A first expedition:

1. Choose an entrance. Mark a short section and inspect its preview. The plan
   defines geometry, not ore locations.
2. Assemble the minehead. Connect biomass, construction stock and destinations
   for ore and spoil.
3. Start. The jaw cuts each face, collection clears it, the grower extends the
   root, treads and light.
4. Resolve stoppages. The pause reasons of stage 2 apply, plus missing
   construction stock and full export. A jaw without output space waits. The
   root never advances past the last safe tread. An exposed cavity pauses the
   job; it does not authorize a bridge or a detour.
5. Reach the landing. Explore, mine by hand, or authorize another section.

Done when: one supplied assembly finishes a section, the player walks to the
landing, and all cut material is in storage. Full storage, missing fuel, missing
construction stock and unavailable chunks stop work without loss or
duplication. Save and reload keep the plan and the committed work. The default
`scripts/demo.sh` world contains a labeled working assembly. The in-game guide
explains assembly, supply and recovery.

## Stage 4 — Galleries

Level routes begin at an established landing. This is where a staircase becomes
a mine: the player covers an ore-bearing level with tunnels.

- **Route:** a Rootstock section without the Stairs operation. The player marks
  it with Surveyed Tissue at the landing: direction, length (at most 64 blocks)
  and up to two turns.
- **Junction:** a root cell where up to three routes meet. Veins from every
  route join there, so one supply line and one export line serve all faces.
- **Faces:** each working face needs its own jaw, bed, cilia and grower. One
  minehead supports at most four faces.
- **Sharing:** faces draw biomass and construction stock from the shared line in
  turn. When supply is short, each face pauses with its own reason; none is
  starved silently.
- **Wider section:** this mutation makes a route 3×3. It needs three jaws on the
  face and triples cutting, spoil and biomass per block of advance.
- **Spoil:** output now exceeds what one chest absorbs. The player adds sorting
  at the surface and decides what to keep, build with or discard.

Done when: two faces fed from one landing run at once, one is deliberately
starved and resumes, and no material is lost or duplicated.

## Stage 5 — Bulk excavation

Steam from the Nether base drives larger methods. Each solves a different job
and reuses the stage 3 organs. All remain finite, surveyed plans.

| Method | What it is | Shape and limit | Needs |
|---|---|---|---|
| Boring Jaw | A Fracture Jaw upgraded with thermal parts. Keeps the jaw's history | Cuts up to diamond-pickaxe grade; serves any method below or a gallery | Steam, larger output buffers |
| Descending Cradle | A 3×3 platform that lowers itself one layer at a time with jaws underneath. Carries a rider | A vertical 3×3 shaft, at most 64 deep per approval, with a climbable lit side | Steam, access and light stock |
| Strata Maw | A Rootstock specialized for trenching from a surface line | A strip 1 wide, at most 32 long and 32 deep per approval | Boring Jaw, spoil storage |
| Digestion Crucible | A sealed pit that leaches host rock and leaves ore standing. The stage 1 pod as an industrial process | A lined pit at most 9×9 and 16 deep; ore is then mined by hand or by a jaw | Leaching charges, fluid drains for spent medium |

More volume always costs more cutting work, energy, construction stock,
collection and spoil handling. No method is a free increase in volume.

Done when: each method completes one approved plan, stops cleanly on full
output or lost steam, and resumes without loss. The Cradle additionally needs a
measured performance test of its moving platform before it is built.

## Stage 6 — Ore-sensing root

The only stage that looks for ore. A sensing mutation lets a root choose its
own route toward ore it detects.

- **Approval:** the player marks a finite volume, at most 32×32×32, around a
  gallery landing. The root never leaves it.
- **Sensing:** the head detects ore blocks within 8 blocks, checking a fixed
  number of positions per tick. It reports what it sensed before it moves.
- **Routing:** it cuts a walkable 1×2 passage toward the nearest sensed ore,
  mines the connected ore blocks at ordinary yield, then senses again. With
  nothing in range it advances along the player's fallback direction.
- **Stops:** the pause reasons of stage 2, the volume boundary, and a total
  length of 256 root cells.
- **Cost:** a completed Nether genome and its genetic construct to mutate the
  root; continuous electrical power while sensing; biomass for cutting. Running
  it costs several times a Descending Root per block cut.

Done when: in an approved volume the root reaches and mines sensed ore, leaves
only walkable passages, stays inside the volume and its scan budget, and stops
when power is cut.

## Later extensions

Not designed yet. Each needs its own method on top of the stages above.

| Place | New problem |
|---|---|
| Oceans and flooded ground | Sealing and draining a face before cutting |
| Nether | Lava, heat-resistant parts, open caverns |
| End | Cutting over the void, anchored access |
| Later dimensions | To be defined with the dimension |

## Mutations and upgrades

A mutation permits an operation. It never supplies the materials or the working
organ. Each one adds a continuing cost, so the player chooses when it pays off.

| Mutation | Permits | Continuing cost | From stage |
|---|---|---|---|
| Lighting | The root places a light every 8 blocks | Lumen Secretion per light | 2 |
| Stairs | The route may descend one block per step | Tread material, more cuts per block of advance | 3 |
| Wider section | 1×2 becomes 3×3 | Triple cuts, spoil and biomass; three jaws | 4 |
| Ore sense | Stage 6 routing | Nether genome, electrical power | 6 |

Physical upgrades add capacity:

| Upgrade | Benefit | Continuing requirement |
|---|---|---|
| Jaw reinforcement | Cuts harder material: stone, then iron, then diamond grade | Matching supplies |
| More jaws | More faces or a faster face | More biomass, collection and export |
| Larger buffers | Absorb output bursts | Export must still match production |
| Grasping Root | Lifts a block intact onto a Work Bed for treatment | Handling and processing instead of ordinary drops |

Growth improves an organ's rate or economy. A mature single-jaw assembly stays
useful for a small branch or a remote outpost.

## Component reuse

A stationary Fracture Jaw breaks the block in front of it. It can serve a
vanilla cobblestone generator or fracture specimens at a Work Bed. It does not
advance by itself. The Rootstock provides planned advance, collection provides
export, construction provides the root and access.

## Prototype evidence

An earlier prototype built most of these methods against real terrain. What it
showed, and what the owner said about it:

- **Level cutting works.** Fixed frames cut 1×2, 3×3 and 7×5 passages up to 96
  blocks with native loot and no loss. This supports the Tunnel Root and the
  wider-section mutation.
- **Stairs work.** A root cut 3-wide stairs with 4 blocks of headroom and
  branched left and right.
- **A lowering platform works.** A 3×3 deck descended 12 blocks with a rider and
  a lit climbing tendon. It was not tested deeper or under load.
- **A sealed leaching pit works.** A 5×5×3 batch was digested and recovered.
- **A strip trench works.** A 5×24 strip was cut at 6 biomass per block.
- **Leaching by hand was too fiddly.** Nodules treated a 3×3 plane into rock
  that broke instantly with no tool wear. Open questions were whether instant
  breaking is too generous and whether repeated application is busywork. The
  Leaching Pod answers the second with one placement; the first still needs
  play testing.
- **Owner preferences.** Downward mining comes first. Markers are flush with the
  ground, not raised frames. Passages have light, stair access and usable
  headroom. The climbing tendon and a long one-block-wide strip are liked.
  Several methods with different roles are kept.
- **Not mining.** Burrowing armor crosses ground without harvesting it. It is a
  separate ability and gives no resources.

## Engine rules

World changes use supported vanilla and NeoForge behavior. Moving a head or an
assembly must not bypass placement, breaking, protection or save rules. A
requirement the engine does not support is resolved before implementation.
