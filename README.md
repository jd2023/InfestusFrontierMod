# Infestus Frontier

Production foundation for a biological progression and construction mod.
**No production gameplay content is implemented yet.** Start with [VISION.md](VISION.md).

Toolchain: Minecraft **1.21.1**, NeoForge **21.1.249**, Java **21**,
Gradle **9.2.1**, ModDevGradle **2.0.146**.

## Development

Install Java 21, Git, Bash, Python 3 and ripgrep; first Gradle use requires dependency access.
Import the root Gradle project in your IDE. From this directory:

```bash
./gradlew :core:test           # fast, Minecraft-free tests
./.ktask/verify.sh             # authoritative build + core + JAR + GameTest gate
./gradlew runClient            # normal client, isolated runs/client directory
./gradlew runServer            # normal dedicated server, runs/server directory
```

Read and accept Minecraft's EULA yourself if a normal server prompts for it.
Tests use `runs/gametest`; no prototype or personal save is copied. Do not install
this foundation JAR alongside the prototype: both use the `infestusfrontier` ID.
No old-save migration or compatibility is promised at this stage.

The deliverable is `build/libs/infestusfrontier-0.1.0-dev.1.jar`. It includes the
pure core, but not the development test mod or archived Forge content.

## Project map

- [Vision](VISION.md): gameplay principles.
- **Start reading gameplay:** [Progression](docs/PROGRESSION_MAP.md), from hand-fed organs through dimensional production and storage.
- [Open questions](docs/OPEN_QUESTIONS.md): the single register of unresolved decisions.
- [Block catalog](docs/BLOCK_CATALOG.md): organs, structures and construction.
- [Item catalog](docs/ITEM_CATALOG.md): ingredients, biological preparation and consumables.
- [Substrate mutations](docs/LIVING_SUBSTRATE_MUTATIONS.md): growth, functions and reinforcement.
- [Armor evolution](docs/ARMOR_EVOLUTION.md): permanent per-piece branches, counters and metabolism.
- [Guide tree](docs/GUIDE_PROGRESSION_TREE.md): proposed page prerequisites and observable unlocks.
- [Developer guide](docs/DEVELOPER_GUIDE.md): mandatory deep modularity and workflow.
- [Performance](docs/PERFORMANCE.md): bounds, back-pressure and adversarial cases.
- [Architecture](docs/ARCHITECTURE.md): module ownership and test-mod isolation.
- [Dependencies](docs/DEPENDENCIES.md): active pins and feature-library candidates.
- [Testing](docs/TESTING.md): current evidence and gates still to build.
- [ktask](docs/KTASK.md): task automation and human gates.

## Release constraint

`LICENSE.txt` specifies CC BY 4.0; legacy mod metadata specifies All Rights Reserved.
Source and asset licensing must be resolved before publication (Q-037).
