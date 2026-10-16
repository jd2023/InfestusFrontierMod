# Infestus Frontier V4

NeoForge 1.21.1 mod. Start with [VISION.md](VISION.md).

| Reader | Read |
|---|---|
| Everyone | [VISION.md](VISION.md), then [AGENTS.md](AGENTS.md) |
| Planner | [ROADMAP.md](ROADMAP.md), then [ENGINEERING.md](ENGINEERING.md) |
| Coder | [ENGINEERING.md](ENGINEERING.md) and the one design document the story names |
| Reviewer | [REVIEW.md](REVIEW.md) |
| Owner | `docs/archive/` |

| Document | Owns |
|---|---|
| [VISION.md](VISION.md) | What we are building and the rules that do not change. The owner edits it |
| [ROADMAP.md](ROADMAP.md) | Milestones and their acceptance evidence |
| [docs/sprints/](docs/sprints/) | Stories of the current sprint and the art queue |
| [docs/design/PROGRESSION_MAP.md](docs/design/PROGRESSION_MAP.md) | Player projects SR0 to SR9 |
| [docs/design/GUIDE_PROGRESSION_TREE.md](docs/design/GUIDE_PROGRESSION_TREE.md) | Guide pages and advancements; species atlas in section 15; tools and weapons in section 17 |
| [docs/design/BLOCK_CATALOG.md](docs/design/BLOCK_CATALOG.md) | Every organ, structure and construction block, by tier |
| [docs/design/ITEM_CATALOG.md](docs/design/ITEM_CATALOG.md) | Every item, ingredient and preparation recipe |
| [docs/design/ARMOR_EVOLUTION.md](docs/design/ARMOR_EVOLUTION.md) | The four armor pieces, their branches, fuel and recovery |
| [docs/design/LIVING_SUBSTRATE_MUTATIONS.md](docs/design/LIVING_SUBSTRATE_MUTATIONS.md) | Substrate growth, function and reinforcement |
| [ENGINEERING.md](ENGINEERING.md) | How we build: modules, code style, limits, tests, stories |
| [REVIEW.md](REVIEW.md) | How we accept work |
| [AGENTS.md](AGENTS.md) | Rules for anyone who changes this repository |

Numbers in the design documents are starting values until play testing confirms them.

```bash
bash scripts/verify.sh       # the gate
./gradlew spotlessApply      # format
bash scripts/demo.sh         # play the demo world
./gradlew runClient          # play your own world
```
