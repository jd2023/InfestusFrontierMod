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

## Feature integrations: not installed in this empty foundation

The playable test profile is Minecraft, NeoForge, this mod and its declared required
libraries. Content-mod integrations use separate presence/absence profiles; none
substitutes for an unimplemented core gameplay loop. Version selection and adapter
design are engineering work.

| Integration | Direction | Decision before implementation |
|---|---|---|
| JEI | Essential recipe/process discovery | Pin 1.21.1 API/runtime; client adapter and process tests |
| Curios | Equipment integration | Version and required/optional status |
| Modonomicon | Guide candidate | Guide requirements, version and API boundary |
| GeckoLib | Animation candidate | Renderer requirements, version and API boundary |
| Jade | Candidate diagnostics integration | Approve scope and optional adapter |
| UI/multiblock libraries | Open | Evaluate against a concrete feature; internal module regardless of implementation |
| FTB Quests | Excluded | Pack maintainers own quest chains; advancements stay |

Before adding a library record its source/license, exact artifact, side,
required/optional status, replacement cost, API boundary and test profiles. Popular
mod compatibility needs explicit targets and tests, not a blanket promise.
