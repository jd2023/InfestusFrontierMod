# Infestus Frontier: the big picture

One page. Read it first. When a story and this page disagree, this page wins until the owner changes it.

## What we are building

A Minecraft mod for NeoForge 1.21.1. The player grows a living colony made of muscle, bone and membrane.
The colony eats organic matter, stores biomass, and turns biomass into organs, machines, armor and tools.
The player decides where it grows. It never spreads on its own.

## The core loop

1. **Feed.** Put organic matter and water into a Digestive Sac. Get biomass.
2. **Grow.** Spend biomass to grow Living Substrate on chosen ground and to build organs on it.
3. **Automate.** Farms, veins, reservoirs and valves keep biomass flowing without the player.
4. **Research.** Collect species samples from kills and harvests. Complete a genome in a DNA Bank.
5. **Mutate.** Combine genomes and prepared ingredients into grafts. Install grafts into armor, tools, plants and organs.
6. **Expand.** Build a bigger base in the Nether, then a spatial base in the End. Connect all three.

## Three worlds, three jobs

| World | Job | Makes locally |
|---|---|---|
| Overworld | Farm and starter workshop | Food, crops, wood, starter biomass and power |
| Nether | Industrial base, bigger than the Overworld one | Heat, power, refined materials, Nether DNA |
| End | Spatial base | Spatial membranes, End DNA, End power |

Each world makes its own fuel and power. Freight moves materials between them. Travel and freight never load chunks for free.

## Rules that do not change

- The player controls growth. Nothing spreads by itself.
- Mining removes real terrain and leaves tunnels a player can walk through.
- Each armor piece evolves on its own, along permanent branches. A specialist beats a generalist at its job.
- Late game needs automation and supply chains, not more hand grinding.
- Every system has a hard limit: no unbounded drops, scans, queues, inventories or chunk loading.
- Original names and art. Vanilla items stay useful. Advancements and an in-game guide ship with the mod.
- We use vanilla and NeoForge behavior as it is. We do not patch, bypass or re-implement the engine.

## Not in scope now

Attack waves. Hostiles that hunt organs. Village transformation. The Fold dimension and its boss. The backpack stores items and gives no armor abilities.

## Delivery order

Each milestone ends in a playable build. M0 is done. We are in M1.

| M | Playable result |
|---|---|
| M0 | Clean build, one tested core rule, one tested ui primitive, one GameTest in a real server |
| M1 | The feedback loop. Spore Culture and Living Substrate are in the game with recipes and guide pages. Every story is proven by tests and by screenshots from the real game, and an agent reads both without a human |
| M2 | Feed a living workshop: culture bowl, sac, bladder, rack, loom, cradle, one awakened armor piece |
| M3 | Self-supplying Overworld: farms, veins, reservoirs, controls, refuel at base |
| M4 | First designed trait: samples, extractor, DNA bank, graft, mutation chamber |
| M5 | Specialist equipment and manual industry: armor branches, leaching, ore treatment |
| M6 | Nether industrial base with local heat, power and DNA |
| M7 | Precision processing and major excavation |
| M8 | End base with local spatial production |
| M9 | One distributed colony: passenger and freight links, storage index, armor recovery |
| M10 | Remaining production branches: aquatic, husbandry, food, helpers, defense, synthesis |
| M11 | Full campaign from a fresh world, two different layouts, dedicated server with two clients |

## Where the details are

| Document | Owns |
|---|---|
| [docs/design/VISION.md](docs/design/VISION.md) | Fixed principles (under review; may fold into this page) |
| [docs/design/PROGRESSION_MAP.md](docs/design/PROGRESSION_MAP.md) | Player projects SR0 to SR9 and the acceptance evidence per milestone |
| [docs/design/GUIDE_PROGRESSION_TREE.md](docs/design/GUIDE_PROGRESSION_TREE.md) | Guide pages and advancements; species atlas in section 15; tools and weapons in section 17 |
| [docs/design/BLOCK_CATALOG.md](docs/design/BLOCK_CATALOG.md) | Every organ, structure and construction block, by tier |
| [docs/design/ITEM_CATALOG.md](docs/design/ITEM_CATALOG.md) | Every item, ingredient and preparation recipe |
| [docs/design/ARMOR_EVOLUTION.md](docs/design/ARMOR_EVOLUTION.md) | The four armor pieces, their branches, fuel and recovery |
| [docs/design/LIVING_SUBSTRATE_MUTATIONS.md](docs/design/LIVING_SUBSTRATE_MUTATIONS.md) | Substrate growth, function and reinforcement |
| [ENGINEERING.md](ENGINEERING.md) | How we build: modules, code style, limits, tests, stories, pins |
| [REVIEW.md](REVIEW.md) | How we accept work |
| [AGENTS.md](AGENTS.md) | Rules for anyone who changes this repository |

Numbers in the design documents are starting values until play testing confirms them.
