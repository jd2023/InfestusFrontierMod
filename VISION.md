# Infestus Frontier — vision draft 0.2

Status: **first draft for discussion, not a ratified content specification**.
Owner: etf. Established: 2026-09-06. Decisions live in `docs/DECISIONS.md`.

Updated with subsequent owner clarifications. The companion
[progression map](docs/PROGRESSION_MAP.md) explores possible ranks and routes;
its named content and mechanics remain proposals.

## North star

**Grow a distributed living civilization across Minecraft's dimensions. Discover
life, capture its genetic possibilities, and evolve yourself and your colony into
something you designed. Power should open new constructions and decisions—not
remove the reasons to play.**

The player is a builder and biological experimenter, not simply an operator of
machines reskinned as flesh. A colony is an interconnected, evolving organism:
terrain becomes tissue, organs perform work, transport sustains distant outposts,
and the player's equipment becomes another living part of that system.

## Established direction from our discussions

- Biology, mutation and DNA manipulation are the mod's central language.
- Living Substrate expands under player control in three dimensions and visibly
  transforms its ecology. No indefinite autonomous expansion or hostile takeover.
  Manual growth remains the baseline; a finite-area, biomass-loaded deployment
  spore is a candidate, not an approved implementation.
- The colony becomes a varied, functional living ecosystem, not a cleared desert.
  Preserve ground cover, undergrowth and canopy. The exact useful roles and
  transformation rules for grass, bushes and trees require a separate discussion.
- Genetic research accumulates samples toward a complete capture. A DNA bank
  enables directed evolution. Exact sample counts and catalogs are not decided.
- Living armor, weapons and tools evolve through **use** and **captured DNA**.
  Running can inform mobility adaptation; a captured organism can unlock a new
  kind of ability. The owner has reopened enchantment support on bio armor;
  removal is not decided. Potion integration is a promising design direction.
- Equipment supports multiple evolutionary directions. Some colony functions
  require a complete bio suit with appropriate maturity or mutations.
- Environmental specialists must remain meaningfully stronger at their jobs than
  generalists. Even late-game armor must not become universally protective.
- Player levels should unlock more capable multiblocks and mutations. Long,
  demanding progression should require automation, base planning and design/DNA
  optimization, not inflated resource bills or repeated exercise grinding.
- Overworld, Nether and End must form one sustained progression. The player needs
  real reasons to build and operate bases in the Nether and End, not merely visit
  them for ingredients. Reaching the End is not the end of meaningful play.
- Long-distance and cross-dimensional living transit should connect the colony.
  Its original identity, costs, endpoint rules and failure behavior remain to design.
- Construction is compositional: reusable organs, tissues, structures and networks
  should combine into many solutions. Depth is not a large list of fixed recipes.
- Mutated substrate carries resources as biological conduits. Useful paths must
  work across natural terrain and in 3D. Joins, splits and crossings matter.
- Visuals should look living: muscle, cuticle, membrane, sacs, pores and circulation.
  Shapes should express function; legible art, seamless variation and meaningful
  animation matter. Mechanical accents are secondary.
- Compatibility is essential. JEI discovery and Curios integration are priorities.
  Retain advancements and an in-game guide. **FTB Quests belongs to pack authors**.
- Testability, feedback-driven iteration, deep modularity and performance are
  conditions of implementation, not cleanup work for the end of the project.
- All names, art, lore, sounds and distinctive designs must be original. The
  biological-swarm inspiration is not a license to reproduce another game's identity.

## Five proposed design pillars

### 1. Grow a colony, not a factory with different textures

Organism, environment, nutrition and genetics should explain how things work.
Ecology should have gameplay value: assimilation can recover matter or knowledge,
and living terrain can become specialized infrastructure. The player should be
able to read the colony's state through its appearance and clear diagnostics.

### 2. Evolution creates choices

Mutation adds new behaviors and combinations, not only larger numbers. Different
equipment and colony builds should excel at different jobs. Costs, incompatibility,
capacity and reversibility are candidate ways to preserve decisions—not yet rules.
Experimentation should be informed, understandable and worth trying.

### 3. Dimensions remain interdependent

Every major dimension contributes an ongoing function that cannot be reduced to
one shopping trip. Mature colonies have useful local operations connected by
purpose-built transit and logistics. Geography should shape designs even after
travel becomes convenient. We must avoid repetitive dimension errands as upkeep.

### 4. A construction language, not a checklist

Teach a small vocabulary of components that can be recombined. Arrangement,
topology, environment and mutations should produce different viable solutions.
Stable multiblocks and expandable storage can coexist, but neither should reduce
the entire mod to memorizing fixed ten-block structures. Borrow the systemic
depth admired in Create and AE2 without copying their appearance or mechanics.

### 5. Earn power without exhausting the game

Early play offers useful capabilities quickly. Long play offers greater reach,
specialization, automation and architectural freedom. Automation should eliminate
solved repetition while exposing new design problems—not demand constant chores.
Avoid a late-game state that simultaneously trivializes resources, travel, danger,
equipment and construction. The intended campaign is meaningfully long; **no hour
count is committed yet**. More grinding is not more depth.

## Proposed core loop

Explore and observe → gather specimens and material → establish local living
infrastructure → complete genetic knowledge → choose adaptations → build useful
systems and evolving equipment → overcome a new environmental or logistical
constraint → connect another viable colony site → discover new combinations.

These activities should overlap and branch. This is not a mandatory quest chain.
Repeating specimens must involve worthwhile play; standing at a trivial farm or
AFK-running into a wall must not be the optimal experience. Exact anti-exploit and
research-diversity rules need discussion before implementation.

## What replayability should come from

Different terrain, base locations, specialized organisms, equipment branches,
network layouts, dimension strategies and cooperative roles. The same component
should solve several problems; a problem should admit several workable designs.
New campaigns should invite new plans without requiring rerolls or blind RNG.

For each major feature ask: what new decision does it introduce; what existing
system can it combine with; what alternative remains viable; what does the player
learn; what remains interesting after the basic resource cost is affordable?

## Vanilla and pack integration

Vanilla resources, exploration, building, enchantments and environmental hazards
remain relevant. Biological systems should complement existing worlds rather than
require a replacement game. Use tags and explicit resource interfaces for cross-mod
interactions; avoid unnecessary hard dependencies or universal free conversions.

Provide discoverable recipes/processes, advancements, useful tooltips and a coherent
guide. A player should not need this chat to learn how to begin or why progress
stopped. Pack authors own quest chains and can tune supported data-driven systems.

## Promising concepts, not approved release scope

- Mobile helper organisms with explicit jobs, relocation and dependable 3D movement.
  The prototype was encouraging; population, pathfinding and management complexity
  still need a decision. Helpers must not become mandatory for ordinary logistics.
- A slow aerial support organism. Making it a chunk loader is a separate safety
  and ownership decision, not an incidental ability.
- Living aerial defense, armor-gated digestive terrain and biomass circulation.
- A fixed mutation or DNA chamber, expandable fleshy storage and configurable organs.
- A living vertical transit mechanism. The prototype lift is **parked**, not an
  approved production feature or a problem that must be solved with custom fluid.
- A special dimension and new boss are now requested as a very-long-term design
  horizon. Their proposed roles appear in the progression map; neither is a
  first-release commitment or authorization to begin implementation.

Prototype names, models, recipes, thresholds, hard caps and save formats are
experiments. They are not the production ontology or a migration commitment.

## Non-goals and guardrails

- Not a clone of another franchise; not a conventional energy factory in bio art.
- Not a race to unlimited resources, an all-purpose machine or one best mutation.
- Not compulsory quest-pack infrastructure; advancements are explicitly retained.
- Not a huge mob roster or new dimension just to increase content count.
- Not uncontrolled world destruction or multiplayer griefing by accidental design.
- Not a promise that every existing popular mod will work without explicit testing.
- Not shipping unbounded drops, scans, entities, packets, queues or chunk loading.
- Not promoting attractive prototype code without production design and tests.

## Decisions for the next conversation

- [ ] Ratify or rewrite the north star and five pillars.
- [x] Colony growth remains under player control; no indefinite autonomous spread.
- [ ] Define bounded vegetation transformation and any optional deployment spore.
- [ ] Who owns a colony, its research, networks and equipment in multiplayer?
- [ ] What does the player discover and accomplish in the first 30 minutes?
- [ ] What are the continuing jobs of the Overworld, Nether and End bases?
- [ ] How do usage-based evolution and directed DNA mutation interact?
- [ ] Decide bio-armor enchantment policy and potion interactions after testing
  specialist/generalist combinations; keep ordinary vanilla systems intact.
- [ ] How costly/reversible is specialization, and how much randomness is welcome?
- [ ] What construction components create the first genuinely different solutions?
- [ ] What danger, loss, recovery and containment experience do we want?
- [ ] What belongs in the first playable vertical slice, versus the first release?
- [ ] Set a campaign-length target after mapping interesting progression—not before.

## First milestone proposal

After design approval, build one end-to-end slice: a discoverable biological start,
one taught ecological interaction, bounded resource storage/transfer, one meaningful
equipment adaptation and a visible reason to expand. Specify its exact content in
an interview before queuing implementation. Validate it in Survival, save/reload,
dedicated server and a deterministic client fixture. It is a foundation for the
larger multi-dimensional game, not a claim to have delivered that game.
