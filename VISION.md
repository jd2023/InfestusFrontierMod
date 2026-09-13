# Infestus Frontier

Grow a player-controlled living colony across the Overworld, Nether and End.
Prepare biological materials, capture genomes, build interconnected organs and
develop independently specialized equipment. A fourth dimension and boss are a
long-term continuation, not first-release requirements.

## Gameplay commitments

- Living Substrate grows where the player directs it. No indefinite autonomous expansion. Preserve useful grass, bushes and trees, rather than clearing the colony into bare ground.
- Ordinary crafting creates starter bodies. Prepared biological materials mutate organs and tissues; bioactive fusion media reinforce equipment. Raw ingots/gems are not direct functional upgrades.
- Armor pieces have independent fusion levels, permanent branches and activity counters. Each piece has one capacity shared by the sum of its counters. Learning cannot be paused; a paid ritual reduces a selected counter by a fixed amount to free capacity. Reduction does not exchange branches.
- Activities develop buffs; fusion raises capacity and protection/durability; DNA mutations supply abilities. Exact thresholds and costs belong in their recipes, not generic XP bars.
- Feed and heal living armor. Biomass powers active features and tissue healing. Empty reserves hurt the wearer before mutual symbiosis; afterwards unfunded features stop without armor hunger pain. Player feeding, player healing and armor fuel production are different functions.
- Include a death-linked single-use rescue. Its learning, charge and bed/reset rules remain unresolved.
- Equipment specialists outperform generalists at their selected jobs. Permanent material and mutation trees cannot be exchanged by changing presets.
- Reusable operations form production lines: extract, treat, fracture, collect, process, store. Some organs start as multiblocks; others gain attachments. Completed work develops organs, with history preserved when moved.
- Mine actual terrain. Manual Leaching weakens non-ore rock while leaving ores for tools. Automated downward mining creates lit, traversable access. Burrowing crosses ground without excavation and cannot provide open-air flight.
- Ore recovery and smelting efficiency improve separately. Players may save compatible dust for later processing. Combining all improvements requires demanding construction and energy supply; no ingot recycling multiplier.
- Mutated substrate carries resources through configured 3D routes, with joins, splits and isolated crossings. Fuel Papilla is refueling substrate; multiblock extensions improve its rate/efficiency. Dye is cosmetic.
- Built connections extend the colony through passenger travel and actual material exchange. Overworld, Nether and End must host continuing productive work, not one-time ingredient trips.
- Keep advancements and an in-game guide. JEI and Curios are priorities; FTB Quests belongs to pack authors.
- Use original names and assets. Armor exposes parts of the player; organs use bone supports, muscle, membranes and visible contents. Connected reservoirs hide internal seams; biomass veins remain legible.
- Performance and testability constrain all mechanics: finite jobs, bounded state/simulation, back-pressure, no unbounded drops or implicit chunk loading.

## Read the design

| Question | Owning document |
|---|---|
| What does the player build and do next? | [Progression](docs/PROGRESSION_MAP.md) |
| Which page/advancement opens next? | [Guide](docs/GUIDE_PROGRESSION_TREE.md) |
| What does an organ accept, do and produce? | [Blocks](docs/BLOCK_CATALOG.md) |
| How is an ingredient or consumable prepared? | [Items](docs/ITEM_CATALOG.md) |
| How does ground grow, mutate and reinforce? | [Substrate](docs/LIVING_SUBSTRATE_MUTATIONS.md) |
| How does an individual piece develop? | [Armor](docs/ARMOR_EVOLUTION.md) |
| What remains undecided? | [Open questions](docs/OPEN_QUESTIONS.md) |

Catalogs are proposals constrained by the commitments above, not implemented content.
A missing rule must reference its open-question ID. Do not substitute “advanced,”
“efficient” or “expensive” for a missing condition, value or recipe.
Label exact draft candidates; do not imply approval. Define a rule once in its owner.

The [decision history](docs/DECISIONS.md) explains changes, not a second current
specification. Engineering starts at the [developer guide](docs/DEVELOPER_GUIDE.md).
No production gameplay is implemented.
