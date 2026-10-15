# Infestus Frontier V4

NeoForge mod. Read [BIG_PICTURE.md](BIG_PICTURE.md) first, then [AGENTS.md](AGENTS.md).

Toolchain: Minecraft 1.21.1, NeoForge 21.1.249, Java 21, Gradle 9.2.1, ModDevGradle 2.0.146.
Pins: [docs/process/DEPENDENCIES.md](docs/process/DEPENDENCIES.md).

## Commands

```bash
./gradlew :core:test :ui:test     # fast, no Minecraft
bash scripts/verify.sh            # the gate: build, boundaries, JAR check, GameTests
./gradlew runClient               # client in runs/client
./gradlew runClient -PwithJei -PwithCurios   # with the optional mods
./gradlew runServer               # dedicated server in runs/server
```

Output JAR: `build/libs/infestusfrontier-0.1.0-dev.1.jar`. It contains the mod, `core` and `ui`. It never contains the test mod.

## Layout

| Path | Holds |
|---|---|
| `core/` | Pure rules. Plain Java, no Minecraft. JUnit tests. |
| `ui/` | Screen geometry and palette. Plain Java. JUnit tests. |
| `src/main/` | NeoForge mod: entry point, feature modules, adapters, resources. |
| `src/testMod/` | Development-only GameTests. Never shipped. |
| `docs/design/` | Gameplay design: vision, progression, catalogs, armor, guide tree. |
| `docs/process/` | Architecture, performance limits, testing, task template, review checklist. |
| `docs/prototype/` | Records of the V3 prototype experiments. Evidence, not specification. |
| `scripts/` | `verify.sh`, the one gate. |

## Status

M0. The build, the boundaries and one GameTest work. No gameplay content yet.
