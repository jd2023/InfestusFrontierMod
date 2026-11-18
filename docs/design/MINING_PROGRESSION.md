# Mining progression

Status: proposed. This document is the complete picture of mining: every stage
from the first hand-dug pit to an ore-seeking root, the organs each stage uses,
and how mined material reaches the workshop. It is self-contained. Every number
is a starting value, not measured balance. The rules below include the owner’s
mining revisions; they are requirements, not a claim that the current game
already implements them.

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
11. Accidents are part of mining. Do not stop before water, lava, falling blocks
    or missing ground. Do not add automatic bridges, seals or hazard detours.
12. A mining organ dies and breaks when its supporting block is gone. This is
    destruction, not a recoverable pause. A planned movement may transfer it
    between supported positions; it must not create support over a gap to
    rescue itself. Unloaded support is unknown, not missing.
13. From the first automated staircase onward, every mining method requires a
    physical multiblock with separate cutting, collection, construction and
    transport parts. Mutations alone cannot turn one block into a whole mine.

## The whole picture

Every mine does the same seven jobs. Progression changes who does each one.

| Job | 0 Hand tools | 1 Leaching Pod | 2 Tunnel Root | 3 Descending Root | 4 Galleries | 5 Bulk excavation | 6 Ore-sensing root |
|---|---|---|---|---|---|---|---|
| Find ore | Player | Player, seeing through leached rock | Player | Player | Player | Player | Root, inside an approved volume |
| Choose geometry | Player | Player places the pod | Player steers each step | Survey plan | Survey plan | Survey plan | Player approves a volume |
| Cut | Player | Player, by hand | Root head | Fracture Jaw | Several jaws | Boring Jaws | Boring Jaw |
| Collect | Player | Nothing drops | Player empties the head | Collection Cilia | Cilia per face | Cilia per face | Cilia |
| Carry out | Player | — | Player | Item Veins | Shared veins and junctions | Veins and buffers | Veins |
| Build access and light | Player | Glowing root column | Root cells, optional light | Structure Grower | Structure Grower | Structure Grower | Structure Grower |
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
  almost instantly by hand and drops nothing. Leached Rock does not glow.
  Leached Rock is solid, so no fluid moves until the player breaks it.
- **Root and light:** as treatment moves down, the pod grows a continuous root
  column beneath itself. The column lights the treated depth and remains after
  work ends. It is the installation’s only light source; there are no glowing
  rock variants. It uses ordinary block lighting, not light through solid rock.
- **Cost and end:** a layer that converts at least one block uses one biomass
  charge. The pod holds 16 charges and never works more than 16 layers below
  itself. When the charges or the layers run out it stops permanently, leaving
  the pod and glowing root column in their final state. It cannot be recharged.
- **Support:** breaking the block directly below the pod destroys the pod with
  no drops, whether it is working or finished. The column must not make the
  pod exempt from this rule. Column survival after a lower section is broken
  remains to be decided; it must not duplicate materials or restore the pod.
- **Unloaded chunks:** pause until the required chunks load, then resume from
  saved progress without repeating conversions or charges.

Done when: a pod planted on a substrate patch over stone leaves a pit the player
can clear with bare hands, with every ore block in it still in place and
visible before the player reaches it. The finished root column lights the
cleared depth. Removing the pod’s support destroys it without drops.

The root column must leave ores untouched. Its placement through ores, cavities
and fluids needs a concrete design before implementation; it cannot silently
destroy ore or gain hazard protection. Cull faces between adjacent Leached Rock
as glass does.

## Stage 2 — Tunnel Root

The first excavating organ. One head cuts a level tunnel one block wide and two
high. The player supplies every step.

- **Parts:** a Tunnel Root head, grown from an Organ Bud, and Root Segments.
  The head stands at the working face. Each step consumes one segment, which
  becomes a flush root cell in the tunnel floor under the head's new place,
  behind it after the next step. The root therefore runs the length of the
  tunnel.
- **Supply:** the player loads segments (up to 16) and biomass into the head by
  hand. It has no vein connection.
- **Work:** one step every 20 seconds. A step breaks the two blocks ahead, puts
  their ordinary drops in the head's buffer, lays one root cell and moves the
  head forward one block.
- **Steering:** separate left, straight and right buttons replace the cycling
  button. Left and right are relative to the head, never the camera. Show the
  current heading, the next heading and an in-world arrow for the next cut.
  The tunnel stays level and follows the four horizontal directions.
- **Movement:** show the head reaching forward, gripping and pulling its body
  into the next cell. Turns visibly rotate toward the next cut. The working
  cycle must read as crawling, not a block disappearing and reappearing.
- **Screen:** put heading and steering first, then the next-step progress,
  biomass, segments and output. Show the full stop reason directly, rather
  than hiding the useful explanation in a tooltip.
- **Cutting grade:** equal to a stone pickaxe. Harder blocks stop the head.
- **Limits:** 64 root cells per root. A one-stack buffer; the player empties it.
- **Pauses, with a reason shown on the head:** no segment or biomass; full
  buffer; a block above its grade or with a block entity; an unloaded chunk.
  The player fixes these causes and the head continues.
- **Accidents:** water, lava, falling blocks and a missing floor do not veto a
  step. Let normal world behavior happen. If the head advances without a
  supporting block, it dies and breaks. A Root Segment does not create a
  floating floor to save it. Recovery from an accident is the player’s job.

Done when: a player can steer a tunnel with a clear preview and visible crawling,
walk its intact length, collect its drops, and recover from each supply pause.
Opening water or unstable ground is allowed. Removing support destroys the
organ rather than displaying a pause reason.

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
4. Resolve supply stoppages. Missing construction stock and full export pause
   work. A jaw without output space waits. Environmental hazards do not
   pause work: opening a cavity or fluid pocket can wreck the installation.
   Construction follows the marked staircase; it does not add a rescue bridge
   or detour. Any organ that loses support dies and breaks.
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
and reuses the stage 3 organs in a physical multiblock. All remain finite,
surveyed plans. Shafts and trenches must leave a route the player can walk
through and climb out of. The player supplies ladders or climbing parts,
torches or living lights, and the materials for landings and access. The
Structure Grower installs them as work advances; missing stock pauses work.
This builds the planned access, not protection from mining accidents.

| Method | What it is | Shape and limit | Needs |
|---|---|---|---|
| Boring Jaw | A Fracture Jaw upgraded with thermal parts. Keeps the jaw's history | Cuts up to diamond-pickaxe grade; serves any method below or a gallery | Steam, larger output buffers |
| Descending Cradle | A 3×3 platform that lowers itself one layer at a time with jaws underneath. Carries a rider | A vertical 3×3 shaft, at most 64 deep per approval, with a climbable lit side | Steam, access and light stock |
| Strata Maw | A Rootstock multiblock specialized for trenching from a surface line | A strip 1 wide, at most 32 long and 32 deep per approval, with lit access and a climbable way out | Boring Jaw, spoil storage, access and light stock |
| Digestion Crucible | A lined multiblock pit with treatment and drain organs that leaches host rock and leaves ore standing. The stage 1 pod as an industrial process | A lined pit at most 9×9 and 16 deep; ore is then mined by hand or by a jaw | Leaching charges, fluid drains for spent medium |

More volume always costs more cutting work, energy, construction stock,
collection and spoil handling. No method is a free increase in volume.

Done when: each multiblock completes one approved plan, installs its supplied
access and lights, stops cleanly on full output, missing stock or lost steam,
and resumes without loss from those supply pauses. The player can explore and
exit the intact shafts and trenches. Hazards can damage access and organs;
loss of support destroys an organ. The Cradle additionally needs a
measured performance test of its moving platform before it is built.

## Stage 6 — Ore-sensing root

The only stage that looks for ore. A sensing mutation adds ore detection to a
mining multiblock; it is not a standalone automatic miner. The installation
still needs a jaw, collection, a Structure Grower, transport and supplied
access and lighting.

- **Approval:** the player marks a finite volume, at most 32×32×32, around a
  gallery landing. The root never leaves it.
- **Sensing:** the head detects ore blocks within 8 blocks, checking a fixed
  number of positions per tick. It reports what it sensed before it moves.
- **Routing:** it cuts a walkable 1×2 passage toward the nearest sensed ore,
  branches from the existing tunnel to reach ore, mines connected ore blocks
  at ordinary yield, then senses again. Every branch remains connected and
  large enough for the player to walk through and explore ahead of the mine.
  Vertical routes need supplied stairs or ladders; all routes need supplied
  lights. No inaccessible ore holes or isolated teleported cuts. With nothing
  in range it advances along the player's fallback direction.
- **Stops:** the pause reasons of stage 2, the volume boundary, and a total
  length of 256 root cells across the connected branches. Environmental
  hazards remain accidents; sensing does not grant hazard avoidance.
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
| Oceans and flooded ground | Player-built sealing and drainage; ordinary miners do not automatically avoid floods |
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
