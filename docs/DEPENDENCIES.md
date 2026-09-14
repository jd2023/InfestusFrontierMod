# Dependencies

## Active versions

| Component | Pin | Role |
|---|---|---|
| Minecraft | 1.21.1 | Target game |
| NeoForge | 21.1.249 | Loader/API; metadata intentionally supports only this tested pin |
| Java | 21 | Compilation/runtime toolchain |
| Gradle wrapper | 9.2.1 | Build entry point |
| ModDevGradle | 2.0.146 | Development runs and game artifacts |
| Parchment | 1.21.1 / 2024.11.17 | Named development mappings |
| JUnit BOM | 5.11.4 | Pure test stack only |

Change versions as an isolated task with cache-cold build, server and client checks.
No dynamic Maven versions or automatic upgrades. Build-tool/transitive dependencies
still need lock/verification metadata before release-grade reproducibility is claimed.

## Selected feature integrations

Selected for IF-001; not installed or production-validated by this planning change.
The required profile is Minecraft/NeoForge/this mod/Modonomicon/GeckoLib.
JEI is essential in the development/playtest pack but optional for server operation.
Curios is optional: armor and manual sample collection work without it.

| Integration | Exact artifact | Boundary |
|---|---|---|
| Modonomicon | com.klikli_dev:modonomicon-1.21.1-neoforge:1.120.4 | Required guide adapter; discovery owns prerequisites |
| GeckoLib | software.bernie.geckolib:geckolib-neoforge-1.21.1:4.9.2 | Required model library; client renderer owns animation, never server policy |
| JEI | mezz.jei:jei-1.21.1-neoforge:19.56.0.438 | Optional runtime; compileOnly common/neoforge API artifacts at same version |
| Curios | top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1 | compileOnly api classifier; optional runtime; separate accessory adapter |
| UI | Internal module over NeoForge/vanilla menus and widgets | Reusable layout/intents/snapshot framework; no external UI dependency |
| Multiblocks | Internal loaded-only bounded validator | Structure rules/data separate from controller and rendering |
| Jade | Deferred | Probe/console provide required diagnostics |
| FTB Quests | Excluded | Pack maintainers own quests; advancements remain |

IF-001 verifies dependency resolution, transitive requirements, packaged server,
client resource loading and optional presence/absence. Missing dependencies or
incompatible pins are engineering failures; do not silently select another version.
Add dependency verification metadata and retain it in review. No library swapping
inside a gameplay task.

Repositories: [JEI Maven](https://maven.blamejared.com/mezz/jei/jei-1.21.1-neoforge/19.56.0.438/jei-1.21.1-neoforge-19.56.0.438.pom),
[Curios setup](https://docs.illusivesoulworks.com/curios/getting-started),
[Modonomicon setup](https://klikli-dev.github.io/modonomicon/docs/getting-started/maven-dependencies),
[GeckoLib 4 setup](https://github.com/bernie-g/geckolib/wiki/Installation-%28Geckolib4%29).

Before distributing, inspect the exact resolved artifacts' license notices and
transitive notices; do not copy upstream art into our assets. Expected upstream
licenses: JEI/GeckoLib code MIT; Curios LGPL-3.0-or-later; Modonomicon code MIT with
separately licensed assets. Upstream notices, not this summary, control distribution.

Replacement costs stay local: guide adapter/data conversion; animation renderer
conversion; JEI category adapter; Curios accessory adapter. None may own genome,
armor, recipe or resource state. Compatibility claims name the tested profile;
third-party content mods/backpacks are not required for the core loop.
