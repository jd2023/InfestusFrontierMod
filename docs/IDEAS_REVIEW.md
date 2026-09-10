# Review of the owner's ideas — 2026-09-07

Read this first, then the expanded [design notebook](ideas_and_progression_feedback.md).
This is candid design feedback, not a list of approved mechanics. The unedited
owner document is preserved in Git commit `5b536bc` at its original path.

## What the ideas reveal

### Update after prototype experiments — 2026-09-08

The notebook is now version 0.3. The strongest new result is **Manual Leaching**:
preparing non-ore rock into a translucent, easily mined host lets players see and
extract real deposits themselves. The owner explicitly wants this mechanic in the
actual mod. It gives evolving tools a job alongside later automation, rather than
making them irrelevant. Its instant/no-wear prototype behavior and reagent recipe
are not yet balance decisions. Preserve the reveal-and-extract experience while
testing costs; do not bury it in repeated treatment chores.

Downward mining now has concrete comparisons: a descending work platform, branching
stair excavation, contained batch digestion/recovery, and a surface-to-depth strip.
These differ by worksite, access, recovery and supply, not only cross-section.
The owner wants multiple progression options, likes flush mutated-substrate
boundaries and lit access, accepts finite one-shot sites, and prefers a later
one-block-wide strip with climbing-tendon entry. The current wide strip is not
that requested narrow geometry. Do not select exact levels by copying test limits.

The suit has a tested persistent reserve and configurable half-transparent HUD.
It does **not** yet have general activity/combat/repair drain. The owner's request
for future metabolism should become one coherent accounting design, not scattered
fuel deductions in unrelated events. Decide depleted-fuel behavior before tying
life-critical travel to it.

**Promising but risky:** aggressive flowing biomass and overflow incidents can
reward buffer sizing, secondary consumers, containment and emergency isolation.
The owner proposed random reservoir rupture; my recommendation is instead a
warned local failure caused by the affected organ's operating state. Neither rule
is approved. Safe early back-pressure and later residual-production risk can
coexist. A finite conserved spill, hard shared work limits, claim protection and
recoverable cleanup are prerequisites, not details to add after a disaster demo.
The crucible's solid digestate is not a tested fluid implementation.

**Burrowing clarification:** the owner confirmed temporary, non-excavating passage through ground in all directions, with fuel/cooldown and a 10-second introductory stopping window. Staying embedded risks suffocation. No permanent disabling or guaranteed escape is intended. Manual Leaching remains separate.

## Original assessment

The strongest identity is **a relationship between the player and a living base**.
Your equipment begins as a poor imitation of ordinary gear, awakens, learns from
your expeditions and depends on the colony you construct. The colony, in turn,
needs the specimens, minerals and environmental capabilities you bring back.
Eventually its organs do industrial work better than your hands, freeing you to
explore, design and establish another frontier. This is much more specific than
“research DNA, unlock next tier.”

The previous map supplied constraints and a plausible sequence but repeatedly
described what a stage *ought to achieve*, instead of what the player would actually
do. Its commissioning tests read like implementation acceptance criteria. We need
to lead with scenes: a hungry chestplate, a flooded descending mine, an extractor
that destroys a rare sample, a greenhouse competing with a mutation chamber for
feed, a Nether organ that earns its floor space. Tests belong underneath those
experiences, not in place of them.

## Highlights worth building around

1. **Awakening is a strong first transformation.** Weak crafted flesh becoming a
   useful organism can supply a memorable, visible first-session reward. The first
   result needs immediate utility; don't require a complete factory to make bad
   armor merely equivalent to cheap vanilla equipment.
2. **Equipment history creates attachment.** Preserving learning across forward evolution
   makes the suit feel like a long-lived companion. Extractable experience also
   supports retirement, backups and trade, provided extraction transfers rather
   than copies valuable progress.
3. **Material reinforcement and DNA do different jobs.** A mineralized frame can
   carry a stronger organ; knowing a useful genome tells you what organ to grow.
   This keeps Minecraft mining relevant without making another ladder of metal sets.
4. **A colony can visibly express its logistics.** Filled sacs, contracting veins,
   supported chambers and busy insects communicate a system people want to inhabit.
   The visual language can also teach supply, direction and starvation.
5. **Base-supported weapons tie power to preparation.** Exceptional local firepower
   becomes a reason to build, route ammunition and shape an approach, instead of
   making a portable sword erase combat everywhere.
6. **Industrial organs can change the player's job.** Early hand mining becoming
   descending, walkable excavation changes exploration and base design. The mined
   space is a second useful output, not collateral damage beside a loot counter.
7. **Dimensional production is more compelling than dimensional shopping.** A
   native thermal nursery or spatial-conditioning workshop makes a Nether/End base
   productive. Local services should also make living there enjoyable.

## Lowlights and points of tension

### 1. Hundreds of Nether Stars is the wrong baseline

The *precision curve* is promising; that extreme resource bill is not. It would
teach players to avoid early research, mass-produce a boss, or look up the single
efficient machine before experimenting. An extractor can explicitly say “this
sample is too complex to resolve; preserve it for a better instrument.” A first
rare encounter should yield a useful discovery, while later precision recovers
the remaining structure from a few valuable samples or bounded observation.
Large volumes belong mainly to renewable feedstock processing, not repeated boss
kills. Exact counts require playtests; the proposed alternative does not ratify
any number of stars.

### 2. Sustainability should be difficult to design, not impossible to achieve

If a good farm never becomes reliably self-supporting, the player has not earned
automation. Conversely, one tiny wheat farm should not permanently feed every
industrial process. Distinguish a stable basic colony from expanding demand,
specialized feed preparation and burst loads. Once solved, a district can rest or
run safely; new ambitions introduce new constraints. No forced daily chores.

### 3. Broad slowing is promising; universal hostility is not

Ordinary substrate could be mildly adhesive, with attuned armor neutralizing it.
This gives the suit an immediate relationship with home. Strong slowing, damage,
paralysis and crowd movement should be deliberate expressions with visible borders.
Otherwise guests, pets, villagers and livestock make every doorway a configuration
problem. Armor is physiological compatibility, **not ownership permission**.
The owner explicitly asks whether a separate fast lane is necessary; it stays open.

### 4. A mandatory sail per chunk makes geography answer the design puzzle

It encourages drawing chunk-aligned districts because Minecraft chunks happen to
be square, not because the biological layout is good. A local diagnostic/support
role may justify a sail without making it essential. Any chunk-loading mutation
is a separate explicit feature with ownership, quotas and safe depletion behavior.
The colony cannot need an unapproved chunk loader to pass its ordinary progression.

### 5. Drones should solve jobs veins cannot conveniently solve

Small-area harvesting, gathering and delivery over irregular terrain give insects
a reason to exist. Compulsory item-carrying entities for every stationary machine
connection multiply pathfinding and management costs. Recommend veins as the
dependable bulk backbone, with optional bounded workers for spatial jobs. Do not
promise mining/building/general intelligence for the first worker.

### 6. Steam and electricity must change the puzzle

If the recipe is biomass → steam → electricity and every new machine just needs
more of the newest bar, we have made a familiar factory with biological textures.
Give steam a pressure/batch/exchange role and electricity a precision/control role.
Biomass still supplies living material and upkeep while those services enable
different work. This is speculative game biology, not a claim that ordinary
organisms run industrial steam systems.

### 7. “Tools become redundant” needs a scale qualifier

Yes for bulk excavation, mass harvesting and repetitive building. No for emergency
repairs, selective harvesting, sampling and expedition work away from infrastructure.
Otherwise tool evolution becomes an investment the game later invalidates.

### 8. Storage can be excellent without recreating all of AE2

Start with physical expandable stores, useful local access, filters, reservations
and inventory interfaces. Add remote requests only when the logistics exist.
Recursive autocrafting, global search, cross-dimensional indexing and portable
infinite inventory are separate projects, not incidental features of a chest.
Displaying an icon is mainly a client question; synchronizing live contents and
querying a large network are server questions. Neither deserves a blanket “cheap.”

### 9. Armor that eats its owner is distinctive and easy to make unpleasant

The owner confirmed automatic hunger pain before full mutual symbiosis, followed by pain-free suspension of hungry abilities when fuel is empty. Teach the risk before awakening and show refueling/removal options. Pain cannot generate biomass or profitable practice. Exact damage severity and body-versus-piece adaptation ownership still need approval; making the entire mechanic opt-in would contradict the decision.

### 10. Too many simultaneous currencies would bury the choices

Rank, genome coverage, DNA volume, DNA diversity, equipment practice, Minecraft XP,
anatomy, fuel, steam and electrical charge cannot all act as independent tolls on
every action. Each gets one meaning. Most actions should expose only two or three
relevant constraints; advanced dashboards can reveal the rest.

## Missing areas now developed in the notebook

- The player's first useful session and several concrete middle/late-game situations.
- A common vocabulary distinguishing substrate, fluids, specimens, knowledge and traits.
- Equipment maturation, training transfer, death recovery, permanent specialization and abuse cases.
- Species identification versus material quantity, precision, coverage and boss evidence.
- Ecology choices for grass/bushes/trees without silently choosing a tree mechanic.
- Farm budgets, herd management, fish production, cooking and brewing as different systems.
- Descending mine access, geological hazards, spoil storage and non-circular ore processing.
- Multiblock composition, path crossings, flow priorities and restart reserves.
- Item/fluid storage responsibilities, bounded orders and failure diagnostics.
- Actual roles for local, long-distance and interdimensional travel.
- Arrival before/after the dragon, unavailable bosses and multiplayer progression.
- Claim/ownership rules, public corridors, trade, recovery and safe decommissioning.
- Visual/audio/accessibility standards and a boundary between art and server simulation.
- Organ families with level, service inputs, outputs and a reason for existing.
- Dependency/circularity checks, integration-bypass risks and a prioritized interview queue.

## Recommendations to challenge in discussion

My strongest recommendation is **separate useful effort from repeatable quantity**:
an expedition earns information; a farm supplies material; an extractor resolves
that information; a mutation installs a chosen expression; ordinary use refines it.
Do not collapse these into one huge generic DNA meter.

Second, choose a small number of deep construction relationships before expanding
the organ catalog. Separation, exchange, storage, routing and control can make many
valid assemblies. Five good interacting parts are more valuable than twenty fixed
machines, even if each machine has beautiful art.

Third, establish the economy and gear loop before ratifying ten ranks. The notebook
keeps the old ranks as reference coordinates but gives them concrete experiences.
It does not approve a longer checklist or promise a campaign hour count.

## Reference check, not dependency selection

Ars Nouveau's documented Starbuncle combines inventory links, visible assignments
and limited harvesting behavior. The lesson here is explicit jobs and readable
assignment, not an assertion that its AI is universally reliable or cheap.
[Starbuncle guide](https://ars.guide/book/crafting/starbuncle_charm/)

Its Drygmy can produce loot from nearby creatures without killing them. That is
useful context for the owner's interest in live sampling, but copying a boss's
whole loot table indefinitely conflicts with our proposed specimen/biomass accounting.
[Drygmy guide](https://ars.guide/book/crafting/drygmy_charm/)

AE2 documents processing patterns as input/output contracts for machinery and
notes a limitation around recursive recipes. This supports narrow external
automation interfaces and separate consideration of recipe cycles; it does not
mean we should implement AE2's planner ourselves.
[AE2 processing patterns](https://guide.appliedenergistics.org/1.21/ae2-mechanics/autocrafting)

Mekanism's documented processing tiers provide inspiration for several supporting
processes contributing to mineral recovery. Our proposed variable yield/throughput
tradeoffs are original design proposals, not a compatibility claim or copied ratios.
[Mekanism ore processing](https://wiki.aidancbrady.com/wiki/Ore_Processing)

The Ars feature actually called **Burrowing** in the checked guide is an excavation
ritual, not confirmation of the particular through-wall travel spell remembered
in the notes. Keep the desired traversal fantasy; identify the exact spell/addon
later rather than attributing unverified behavior.
[Burrowing ritual](https://ars.guide/book/ritual_index/ritual_burrowing/)

These sources were checked on 2026-09-07. They inform design interpretation only;
production 1.21.1 APIs, versions and dependencies still need their own review.
