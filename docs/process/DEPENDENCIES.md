# Dependencies

Exact pins. Change a pin only in its own task, with a cold build, a server run and a client run.

| Component | Pin | Role |
|---|---|---|
| Minecraft | 1.21.1 | Target game |
| NeoForge | 21.1.249 | Loader and API. Metadata accepts only this version |
| Java | 21 | Toolchain |
| Gradle wrapper | 9.2.1 | Build |
| ModDevGradle | 2.0.146 | Runs and game artifacts |
| Parchment | 1.21.1 / 2024.11.17 | Named mappings |
| JUnit BOM | 5.11.4 | Pure tests in `core` and `ui` |

| Mod | Artifact | Status |
|---|---|---|
| Modonomicon | `com.klikli_dev:modonomicon-1.21.1-neoforge:1.120.4` | Required. Renders the in-game guide. Discovery owns prerequisites |
| GeckoLib | `software.bernie.geckolib:geckolib-neoforge-1.21.1:4.9.2` | Required. Client animation only. Never server policy |
| JEI | `mezz.jei:jei-1.21.1-neoforge:19.56.0.438` | Optional. API on compile classpath. Runtime with `-PwithJei` |
| Curios | `top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1` | Optional. API on compile classpath. Runtime with `-PwithCurios` |
| Jade | none | Not used. The probe and console give the diagnostics |
| FTB Quests | none | Not shipped. Pack authors own quests. Advancements ship with the mod |

Rules:

- `core` and `ui` have no dependencies at all. The build fails if one appears.
- Optional mods live behind an adapter in `integration/`. The mod works without them, and a test proves it.
- No dynamic versions. No automatic upgrades.
- Before a release, read the license notice of every resolved artifact. Expected: JEI and GeckoLib MIT, Curios LGPL-3.0, Modonomicon MIT with separately licensed assets.

Open: `LICENSE.txt` says CC BY 4.0. Older mod metadata said All Rights Reserved. The owner decides before any release.
