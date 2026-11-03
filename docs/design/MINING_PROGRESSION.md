# Mining progression

Status: proposed. This document defines the mining experience and its playable
phases. Organ specifications and recipes remain in the
[block catalog](BLOCK_CATALOG.md) and [item catalog](ITEM_CATALOG.md).

## Player challenge

Build a mine that reaches useful terrain, keeps its working face supplied and
exports its material without losing access or overwhelming the workshop.

Mining removes real terrain. The player chooses the excavation and authorizes
finite sections. There is no automatic ore search or ore-vein chasing. The
installation performs repetitive cutting after the player has designed and
supplied it.

Difficulty comes from excavation geometry, supply chains, output handling and
the interaction between specialized organs. Components may have little value
alone. The assembled mine must provide a useful playable result.

## First mine: a descending root

The first assembly excavates a slow, finite staircase from a surface entrance to
an underground landing. The player chooses its direction and depth before work
starts. A physical working root grows along the route. It leaves a lit passage
the player can walk through to inspect the cutting face and reach the landing.

The controller sequences separate cutting, holding, collection and construction
operations. Each operation needs its actual working organ and supplies. A Jaw
without output space waits. A root without construction or access supplies stops
before advancing beyond the last safe position.

| Part | Job | Player decision |
|---|---|---|
| Surveyed Tissue | Marks the permitted excavation | Entrance, direction, depth and stopping point |
| Descending Rootstock | Sequences work and advances the physical root | Which finite section to authorize next |
| Fracture Jaw | Breaks reachable supported blocks | Cutting grade, rate and number of heads |
| Work Bed and collection parts | Hold and remove excavated material | Buffer capacity and collection rate |
| Structure Grower and access parts | Extend the root and place supplied access and lighting | Construction stock and a usable route behind the face |
| Mouths, veins and storage | Supply biomass and export real outputs | Ore routing, spoil capacity and supply reserves |

The controller does not grant the functions of missing organs. Installed parts
retain their own histories and upgrades. Adding cutting capacity can expose a
collection, transport, construction or biomass bottleneck.

### A first expedition

1. Explore and choose an entrance. Mark a short descending section and inspect
   its preview. The plan defines geometry, not the locations of valuable ore.
2. Assemble the minehead. Connect biomass, construction supplies and accepting
   destinations for ore and spoil.
3. Start the descent. One slow Jaw cuts successive faces. Collection clears the
   work area. Construction extends the physical root and maintains access.
4. Inspect interruptions. Fluids, an exposed cavity or unsupported terrain pause
   the affected work with a specific reason. Missing supplies or full outputs
   also pause it. The player walks down to resolve the condition and resumes.
5. Reach a prepared landing. The player can explore or mine nearby by hand,
   authorize another descending section, or later build horizontal galleries.

Encountering a cavity does not authorize an automatic bridge or an unplanned
extension. The player must prepare a safe continuation. Fluid handling and
specialized excavation methods belong to later capabilities.

## Playable phases

| Phase | Capability | Main challenge |
|---|---|---|
| Reach underground | One slow, supplied staircase assembly | Build a dependable minehead and reach a chosen depth |
| Develop a mine | Horizontal galleries, junctions and additional cutting heads | Choose coverage and share supplies between working faces |
| Excavate at scale | Larger cutting bodies, shafts and specialized extraction methods | Sustain bulk throughput, handle spoil and justify costly recovery |

### Reach underground

The next proposed milestone delivers one complete descending assembly. Its
cutting, collection, supply, construction and access requirements all work
together. Completing a finite section leaves a useful underground landing and
retains the excavated material.

The player can stop, inspect and resume the mine. Full storage, interrupted fuel,
missing access supplies and unavailable chunks stop work without duplication or
unintended loss. Save and reload retain the authorized plan and committed work.
The mine loads no chunks on its own.

Standalone components can be implemented separately, but a list of registered
organs is not the playable milestone. The default `scripts/demo.sh` world must
include a labeled, usable descending assembly cutting real terrain and exporting
its outputs. The in-game guide explains assembly, supply and recovery. Verification
belongs to each implementation task. Placeholder artwork does not block play.

### Develop a mine

Horizontal galleries begin at an established landing. The player chooses finite
routes at a useful elevation, gallery spacing and the positions of junctions,
buffers and services. The installation excavates only the approved geometry.

Several working faces may share a supply line or processing workshop. More heads
increase cutting demand as well as output. A long route and a shared destination
must remain understandable when one branch stops or storage fills.

### Excavate at scale

Later assemblies support shafts, larger excavation faces and specialized methods.
They reuse working organs, transport and construction services where appropriate.
Their cost includes the supply infrastructure needed to sustain them.

Large-scale mining must not be a free increase in excavation volume. More terrain
means more cutting work, operating resources, construction, collection and spoil.
Ore processing remains a separate factory with its own throughput and recovery
choices.

## Upgrade choices

| Upgrade | Benefit | Continuing requirement |
|---|---|---|
| Jaw reinforcement | Admits harder supported material | Correct cutting grade and operating supplies |
| Additional cutting heads | More work can proceed concurrently | More biomass, collection and export capacity |
| Larger collection buffers | Absorb temporary output bursts | Sustained export must still match production |
| Improved construction services | Support more demanding excavation shapes | Actual access, lighting and structural materials |
| Intact extraction | Preserves eligible specimens for treatment | Additional handling and processing instead of immediate ordinary drops |

Growth improves an organ's rate or efficiency. Mutations enable specific
operations. Physical additions supply actual working capacity. The exact costs,
rates, bounds and supported block sets must be specified before their respective
implementation tasks; this proposal does not establish measured balance values.

A mature single-head assembly remains useful for a small branch or remote
outpost. Later methods need not replace every earlier installation.

## Component reuse

A stationary Fracture Jaw can break the block presented directly in front of it.
Possible independent uses include a vanilla cobblestone generator and later
fracturing specimens at a Work Bed. It does not advance into terrain by itself.

The Rootstock provides planned advancement. Collection provides export.
Construction provides the physical continuation and access. These separate
operations make the mine an installation the player designs and services.

World interaction must use supported vanilla and NeoForge behavior. Moving the
working assembly must not require bypassing placement, breaking, protection or
save rules. Any unsupported requirement must be resolved before implementation.
