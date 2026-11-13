# Infestus Frontier

## What we are building

A Minecraft mod for NeoForge 1.21.1, inspired by the StarCraft Zerg race. No StarCraft or Zerg names or references appear in the mod.

The player grows a living colony of organs and connecting tissue. It consumes organic matter and turns it into biomass, energy, construction materials, equipment and new organs. Most organs, blocks and items grow or mutate from simpler living forms.

The challenge is to build, manage and optimize a biological factory: balance production and consumption, supply specialized organs, handle byproducts and solve the problems created by their arrangement.

The colony spans the Overworld, Nether and End. Each dimension needs a developed base with local production and useful exports. The Nether becomes a larger industrial base than the Overworld workshop. The End supports its own spatial industry. Visiting a dimension to collect resources does not replace building there.

## Experience, evolution, mutation and expansion

- **Experience** comes from useful work performed by an organ, tool or equipment piece. It enables that individual to advance.
- **Evolution** follows a fixed branching graph. The player chooses a permanent specialization at each junction. A branch may strengthen basic functions or support more complex mutations. Every choice has a tradeoff; no final form is best at everything. Branches cannot be undone or switched, for organs, tools, armor or other evolving items.
- **Mutation** adds a specific capability. The player collects DNA samples to complete a species genome. One completed genome, or a combination of completed genomes, enables production of a **genetic construct**: the physical genetic material for a particular mutation. Applying the mutation consumes its construct and the required energy. Biomass supplies early mutations; later mutations can require other energy sources or prepared biological materials. Completed genome records remain available for further production.
- **Multiblock expansion** adds physical parts to an organ: working chambers, inputs, outputs and automation connections. It enables more complex processing, byproduct handling and greater capacity or efficiency, with the space and supplies to support them. The player can dismantle and rebuild the assembly to correct its layout. This does not undo the core organ's evolution or let it switch branches.

## The core loop

1. **Feed.** Put organic matter and water into a Digestive Sac. Get biomass.
2. **Grow.** Establish Living Substrate and grow the first organs from basic living forms.
3. **Automate.** Connect farms, veins, storage and processing so the workshop replenishes its supplies.
4. **Evolve.** Earn experience through work and choose permanent branches for individual organs and equipment.
5. **Research.** Collect DNA samples and complete species genomes in a DNA Bank.
6. **Mutate.** Produce a genetic construct from the required genomes and biological materials. Supply energy to apply its mutation to a compatible host.
7. **Build out.** Expand organs into multiblocks, arrange their ports and processing parts, and balance the larger factory's supplies and outputs.
8. **Expand.** Build productive Nether and End bases and connect all three dimensions.

## Three worlds, three jobs

| World | Job | Makes locally |
|---|---|---|
| Overworld | Farm and starter workshop | Food, crops, wood, starter biomass and power |
| Nether | Industrial base, bigger than the Overworld one | Gas, heat, power, refined materials, Nether DNA |
| End | Spatial base | Spatial membranes, End DNA, End power, flight |

## Rules that do not change

- The player controls colony growth. Automatic growth is allowed; its server load must stay bounded.
- Load-test mechanics. Performance and server stability are priorities at every stage. Prefer simple technical solutions.
- Mining removes real terrain and leaves tunnels a player can walk through.
- Each armor piece evolves independently. A specialist beats a generalist at its job.
- Late game needs automation and supply chains, not more hand grinding (factory management).
- Every system has a hard limit: no unbounded drops, scans, queues, inventories or chunk loading.
- Original names and art. Vanilla items stay useful. Advancements and an in-game guide ship with the mod.
- We use vanilla and NeoForge behavior as it is. We do not patch, bypass or re-implement the engine.
- Keep early machinery useful through alternative production routes. Later advancement must remain harder even with better technology; it requires automation, coordinated supply chains and increasing resources, not repeated manual grinding.
- Establish local renewable biomass and power in every dimension. The Overworld is the starter/agricultural base; the Nether becomes the larger industrial base, and the End develops native spatial industry. Connections carry both players and materials, not permanent Overworld fuel dependence.

## Not in scope now

Attack waves. Hostiles that hunt organs. Village transformation. The Fold dimension and its boss. The backpack stores items and gives no armor abilities.
