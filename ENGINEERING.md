# Engineering

For the coder: planner, worker and repairer. Rules about git and roles are in [AGENTS.md](AGENTS.md).

## Toolchain and commands

Minecraft 1.21.1, NeoForge 21.1.249, Java 21, Gradle 9.2.1, ModDevGradle 2.0.146, Parchment 2024.11.17. Exact pins: [Dependencies](#dependencies).

```bash
./gradlew spotlessApply                     # format everything; run before every commit
./gradlew :core:test :ui:test               # fast, no Minecraft
bash scripts/verify.sh                      # the gate; log in build/verification/full-gate.log
./gradlew verifyCaptures                    # client screenshots into build/captures, then pixel checks
./gradlew runClient                         # client in runs/client
./gradlew runClient -PwithJei -PwithCurios  # with the optional mods
./gradlew runServer                         # dedicated server in runs/server
bash scripts/demo.sh                        # fresh demo world with every feature on show, in runs/demo
```

Output: `build/libs/infestusfrontier-0.1.0-dev.1.jar`. It holds the mod, `core` and `ui`. It never holds the test mod.

| Path | Holds |
|---|---|
| `core/` | Pure rules in `org.jd.infestusfrontier.core.<owner>`. Plain Java, JUnit. No Minecraft. |
| `ui/` | Screen geometry and palette. Plain Java, JUnit. |
| `src/main/java/org/jd/infestusfrontier/` | The NeoForge mod: entry point, one folder per owner module. |
| `src/main/templates/` | `neoforge.mods.toml` with the version expanded at build time. |
| `src/testMod/` | Development-only GameTests and the capture client, mod id `infestusfrontier_tests`. |
| `src/testMod/resources/captures/` | One JSON per capture scenario; `golden/` holds approved screenshots. |
| `docs/design/` | Gameplay design. One owner document per subject. |
| `docs/sprints/` | One file per sprint (`S01.md`, ...) and the art queue `ART.md`. |
| `scripts/verify.sh` | The gate. |

## Architecture

```text
InfestusFrontier (composition root)      registers modules, owns no gameplay
  └── <owner>/                           NeoForge adapters: blocks, items, block entities, menus, packets, screens
        └── <owner>/api                  feature commands and immutable queries
              └── core/.../<owner>/      pure rules, JUnit tests
ui/                                      geometry and palette, used by screens only
```

- Arrows point inward only. No cycles. `core` and `ui` import only the JDK; the build fails otherwise.
- One behavior has one owner module. One folder per owner. No global `block/`, `item/`, `menu/` buckets.
- Each owner registers its own content. The entry point calls one `register` per module.
- Public types live in `<owner>/api`, plus one `<Owner>Module` class with `register(IEventBus)` and the module's registry handles. Everything else is package-private.
- Registry ids are flat `infestusfrontier:<snake_case>`, the catalog name in snake case: `living_substrate`, `spore_culture`. Block state property names are nouns: `stage`, `facing`.
- Recipes, tags, loot tables, models, blockstates, advancements and guide pages are JSON written by hand under `src/main/resources`. No data generators until a story shows more than 50 files of one kind.
- A feature that needs another feature declares a small port interface in its own `api`. The composition root connects it.
- Commands return success or a refusal. A refusal names the unmet condition and changes nothing.
- Queries return immutable snapshots, never a live collection.
- Screens read snapshots and send intents. Every decision runs on the server.
- Rendering is client-only and never decides whether a resource moved.
- Use vanilla and NeoForge behavior as it is. Never patch, bypass or re-implement engine rules. If a story seems to need that, stop and report.
- Shared low-level code (UI primitives, serialization, packet validation, bounded simulation, discovery) lives in one module from its first use. Features compose it, never copy it. Do not build it before the first consumer.
- Comments state a contract. Narrating comments are defects.

## Code style

The formatter decides layout. `./gradlew spotlessApply` formats. `check`, and so the gate, fails on unformatted code. Do not fight the formatter and do not add style exceptions.

- Palantir Java Format: 4 spaces, 120 columns, no unused imports. `.editorconfig` tells editors the same.
- `core` and `ui` compile with `-Xlint:all -Werror`. A warning is a build failure. Fix the cause, do not suppress it.
- A class is `final` unless a story says it is extended. A field is `private final` unless it must change.
- A value is a `record`. A result with several shapes is a `sealed` interface with record cases, read with a pattern `switch`, no `default` branch.
- A command returns a result with one success case and one refusal case. The refusal names the unmet condition. The first story that needs it creates the type in `core`.
- No `null` across a public boundary. Return `Optional` or an empty snapshot. Accept nothing nullable.
- No static mutable state. No reflection, Mixins, access transformers or coremods.
- `var` only when the type is written on the same line.
- Names: registry ids `snake_case`; Java `UpperCamelCase` types, `lowerCamelCase` members, `UPPER_SNAKE_CASE` constants. Suffixes: `XxxBlock`, `XxxBlockEntity`, `XxxItem`, `XxxMenu`, `XxxScreen`, `XxxPayload`, `XxxRecipe`. GameTest classes: `<Owner>Tests`.
- A test name states the behavior: `refusesWhenFull`, `restoresBatchAfterReload`. One behavior per test. No sleeps, no unseeded randomness, no shared mutable fixtures.
- One logger per owner module. No logging inside tick methods.

### Owners

Names are project contracts. Create a module with its first story, not earlier.

| Owner | Owns |
|---|---|
| foundation (`core`) | Shared work admission (`TickQuota`), checked quantities, bounded identifiers |
| organ | Organ identity, completed-batch counts, earned choices |
| processing | Bowl, digestion, rack, loom, furnace, activation: reserved batch execution |
| storage | Item, fluid and biomass balances; reservoir cell accounting |
| construction | Loaded-only structure validation, shell parts, paid placement, transplanting |
| ecology | Substrate maturity, function, lining, reinforcement, dye, deliberate conversion |
| cultivation | Crop and tree growth, allocated harvest, planting reserves |
| logistics | Routes, ports, filters, reservations and movement of items, fluids and biomass |
| control | Signals, interlocks, selection, finite schedules |
| genetics | Samples, species and quality counts, genome coverage, native eligibility, cultured stock |
| equipment | Armor piece identity, fusion tree, counters, mutations, fuel, death preservation |
| portable | Carry Sac and pouch slots; refuses nesting; adds no armor abilities |
| mineral | Workpiece stages, treatment, fracture, dust and tailings |
| excavation | Finite survey and cut plans, safe stepping, access and light |
| thermal | Heat, pressure, steam, cooling |
| energy | Biological electricity and explicit external energy exchange |
| containment | Spill volume, damage delay, isolation and recovery |
| spatial | End-native beds, anchoring, conditioning |
| transit | Endpoint links, player travel, durable freight escrow |
| indexing | Store summaries and bounded material requests; holds no item copies |
| husbandry | Animal work, feed, containers, berths |
| nutrition | Food and potion recipes; runs through processing ports |
| helpers | Nursery population, jobs, routes, paid Sail chunk tickets |
| defense | Hostile targeting, firing, projectile limits |
| synthesis | Sockets, lobes, idle profile switching |
| ui (`ui/`) | Layout, widgets, focus, theme, tooltips |
| interaction | Probe targeting, reach and ownership checks, dispatch to feature intents |
| discovery | Recipe reveal, advancements, guide prerequisites |
| integration | Modonomicon, GeckoLib, JEI, Curios adapters; no domain state |

### Invariants

- Processing reserves inputs, outputs, containers, waste and target before it starts. One active batch per organ. Saved stage, not wall time, drives recovery.
- Organ history has one owner. Wall blocks never copy a core's history.
- Balances have one owner (`storage`). Routes and indexes hold reservations, not totals. Integer math is checked.
- `equipment` owns every suit predicate and every fuel spend. Nobody rebuilds the full-set check.
- `genetics` owns native-class predicates. Being in a dimension, owning a genome and having a native bed are three separate conditions.
- Freight has one owner at every step and a bounded escrow.
- Optional adapters load only when their mod is present. Test mod code never ships.

## Performance limits

Performance is correctness. A feature that can stall a server is a broken feature.

Before implementation, the story states: the work unit, who shares the budget (dimension, team, network), the hard per-tick bound, which blocks it reads, maximum entities and geometry, persistent state size, packet rate, behavior on overload, behavior on chunk unload and reload. Each has a test.

- No unbounded loose items, XP orbs or projectiles. Insert into bounded storage, keep a capped buffer, then pause. A player harvesting by hand may get bounded stacks.
- No implicit chunk loading. No spread, route, helper or cross-dimension lookup creates a ticket. A paid loader (Sail Roost) has owner, quota, upkeep, expiry and restart tests, and counts the engine-loaded neighbors too.
- No recursive flood fill, whole-world scan, position-keyed unbounded cache, ever-growing queue, unlimited inventory or attacker-sized NBT allocation.
- Budgets are shared through `TickQuota` or a sibling in `core`. Rate limiting one machine does not bound a world of machines.
- Idle systems sleep. Decorative blocks have no block entity. Refused work pauses and retries without losing items or multiplying pending work.
- Check `isLoaded` before any neighbor read. Vanilla neighbor updates, leaf decay, drops and third-party handlers can reach unloaded chunks.
- Sync changes only, as compact deltas. Particles are client-side, distance-limited, short-lived, and share one budget.
- The server owns all gameplay state. A client cannot mint resources, pick arbitrary targets, skip ownership checks or supply a mutation result.

| Rule from the design | Limit |
|---|---|
| Sample collection | One death offers one sample to one destination. At most 64 offers per server tick. No kill log |
| Sample UI page | 16 species rows, 48 quality counts, 2 changed pages per second per viewer |
| Portable supply | 4 transactions per wearer per second, 64 per server tick in total |
| Death preservation | Looks at the 4 worn pieces once. One loaded-endpoint lookup per piece. No retry queue |
| Death Bond | At most one pending piece per equipment slot per player |
| Hunger damage | One cadence per wearer, not one per piece. Floor at one heart |

Every feature survives: full outputs; broken, split or merged structures; cycles; unloaded endpoints; restart with resources in flight; corrupt or old saves; two players at once; ownership change; dead helper reservations; dense mobs; packet spam; unsafe teleport target; integer, coordinate and time limits; shutdown during a transfer. Nothing duplicates, nothing is lost, and the refusal is explained.

## Testing

`bash scripts/verify.sh` runs `./gradlew verifyAll`. Gradle decides pass or fail. The script does not parse log text.

| Layer | Command | Proves |
|---|---|---|
| Pure tests | `./gradlew :core:test :ui:test` | Rules, limits, edge cases. Milliseconds, no Minecraft |
| Boundaries | `:core:verifyBoundary`, `:ui:verifyBoundary` | Those modules have an empty classpath |
| Distribution | `verifyDistribution` | The JAR holds the entry point, `core`, `ui`, expanded metadata, and no test mod |
| GameTests | `runGameTestServer` | Server-side behavior across real ticks, in `runs/gametest` |
| Client captures | `./gradlew verifyCaptures` | Real client screenshots in `build/captures/<scenario>/<view>.png`. Checked for missing textures and blank frames, compared with goldens when present. The reviewer opens the images |
| Demo world | `bash scripts/demo.sh` | Every shipped block, item and recipe in one fresh world, labeled. The owner plays it after each sprint |
| Manual play | `runClient`, `runServer` | Feel and usability. Owner only, at milestone gates |

- Every behavior gets a test that fails without it. Write the test first.
- Every reviewer finding gets a regression test.
- Test save and reload through a real save, not only the codec.
- Shared-budget tests isolate one fixture, then test contention separately.
- A flaky test is a defect to investigate. Never rerun until green. Never delete the assertion. Never raise a limit or replace a golden image to get green.
- Fixtures and the test mod stay in `src/testMod`. Use disposable worlds under `runs/`. Never touch a personal save.
- Run graphical and headless runs as separate Gradle invocations. `scripts/verify.sh` runs `verifyAll` and then `verifyCaptures`.

### Captures

The capture client is part of the test mod. `./gradlew verifyCaptures` starts the real client, creates a fresh flat Creative world in `runs/capture`, runs each scenario, writes the screenshots and exits. Then a Gradle task checks every expected image.

A scenario is one JSON file in `src/testMod/resources/captures/<scenario>.json`:

```json
{
  "commands": ["setblock 0 -60 3 infestusfrontier:living_substrate[stage=basic]"],
  "views": [
    {"name": "front", "x": 0.5, "y": -60, "z": 0.5, "yaw": 0, "pitch": 20},
    {"name": "hotbar", "x": 0.5, "y": -60, "z": 0.5, "yaw": 0, "pitch": 20, "gui": true}
  ]
}
```

- The harness sets noon, clear weather, frozen time and weather, no clouds, no view bobbing, an empty hand, and hides the GUI unless `gui` is true. `commands` run as the player, one per tick, before the views.
- A view teleports the player to `x y z` with `yaw pitch`. On the ground the player stands; in the air it flies and is pinned there. It waits until every chunk section around the player is rendered, and saves `build/captures/<scenario>/<view>.png`.
- A capture fails when the image is missing, when more than 0.2% of its pixels are the missing-texture magenta, when the whole frame is one flat color, or when a golden exists in `src/testMod/resources/captures/golden/<scenario>/<view>.png` and more than 0.5% of pixels differ by more than 32 in a channel.
- Goldens are added only after the owner accepts the art. A golden is replaced only in a story that says so.
- The reviewer reads the PNG files. A passing check is not an accepted picture.

### Demo world

`bash scripts/demo.sh` builds the mod, deletes `runs/demo/saves/demo`, creates a fresh flat Creative world, runs the commands of the capture scenario `src/testMod/resources/captures/demo.json`, teleports the player to its first view and leaves the game open to play. The same scenario is photographed by `verifyCaptures`, so the gate sees the demo world too. Each exhibit is the blocks or items plus a waxed sign that names it.

Every story that adds or changes a block, item, recipe, screen or guide entry adds its exhibit to `demo.json` and lists the file in `Files`. Exhibits stand north of spawn, from z=-37 to z=-31, so they never enter another scenario's view. The owner must be able to see and try every feature there without commands.

Not covered yet: menu and screen captures, recipe tests through a crafting menu, optional-mod profiles, multiplayer soak, crash recovery, measured TPS and FPS on a dense base. Each arrives with the feature that needs it.

## Stories and sprints

A strong model plans. A cheap model implements. The owner plays at the end of each sprint.

A sprint is one milestone from ROADMAP.md, or one slice of it, and ends in a playable build. Its stories live in one file, `docs/sprints/S<nn>.md`: milestone, goal in one sentence, the acceptance line from ROADMAP.md, then the stories in order. Only the current sprint is written at this depth. Later milestones stay at one line each in ROADMAP.md.

A story is ready only when a worker can finish it without making a design decision or reading a whole catalog.

One story, one kind, one outcome. About 12 files and 400 changed lines at most. If the title needs "and", split it.

| Kind | Contains | Evidence |
|---|---|---|
| rules | Pure `core` types and JUnit tests. No Minecraft types | rules |
| platform | Block, item, block entity, codec, menu wiring, GameTests. Placeholder assets allowed | game |
| data | Recipes, tags, loot, translations and their GameTests | game |
| art | Textures, models, blockstates, icons, CREDITS line. Assets only, no Java | visual |
| complete | Guide entry, advancement or discovery step | game, visual |
| qualify | A scripted route or soak over accepted content. No new behavior | game |
| repair | One reviewer finding with its regression test | as needed |
| harness | Build, test or capture tooling in Gradle, `scripts/` or `src/testMod`. No gameplay | gate |

A content feature is a chain: platform with placeholder art, then art, then data, then complete.

```text
IF-nnn  <one observable outcome>
Status: ready                # ready | doing | review <commit> | accepted <commit> | blocked <reason>
Milestone: M1
Owner: processing            # a module from the Owners table
Kind: platform
Depends: IF-012              # accepted stories only
Read: docs/design/BLOCK_CATALOG.md#t0-03--digestive-sac, src/main/java/.../processing/   # at most five
Files: src/main/java/.../processing/DigestiveSacBlock.java, ...   # exact allowlist, tests included
Do:
  1. Create class X with method Y(signature). Exact names, IDs and numbers.
Tests:
  - testName: what it asserts, and why it fails before the change
Done:
  - ./gradlew :core:test
  - bash scripts/verify.sh
Not: adjacent behavior that belongs to another story
Bounds: hard work and state limits, with numbers
Evidence: rules | game | visual
```

`Status` moves `ready`, `doing`, `review`, `accepted`. The worker writes `doing` and `review` with the commit hash. The planner writes `accepted` or `blocked`. The sprint file is the one file a story may edit beyond its `Files`, and only its own `Status` line.

How to do a story:

1. Read the story. If it cannot be done inside `Files`, stop and report which file or decision is missing. Otherwise set `Status: doing`.
2. Write `Tests` first. Run them. See them fail for the stated reason.
3. Implement in the owning module. Keep the public interface small.
4. Run `./gradlew spotlessApply`, then `Done`, then `bash scripts/verify.sh`.
5. Walk through the checklist in [REVIEW.md](REVIEW.md) against your diff.
6. Set `Status: review <commit>`. Commit, push to `origin/v4`, and report: what you did, what you did not do, what you measured.

The planner decides every name, number and interface before the story enters the sprint, after reading the real code. Common invariants (save and reload, break and replace, refusal leaves state unchanged) are written into the story that owns them.

`Evidence` names what the reviewer looks at: `rules` is JUnit output, `game` is the GameTest line in the gate log, `visual` is the capture PNGs, `gate` is the gate log itself.

The sprint loop:

1. The planner reads the real code and ROADMAP.md and writes the sprint file: 5 to 12 stories, each with `Depends` on accepted or earlier stories only.
2. The worker takes the first `ready` story whose `Depends` are all `accepted`. One story per run.
3. The reviewer gives the verdict. On `REJECTED` a repairer fixes the findings in one commit per round. Three rejected rounds block the story and send it back to the planner.
4. When every story is `accepted`, the owner plays the build. What is broken becomes `repair` stories in this sprint. What is missing becomes stories in the next sprint.
5. The planner writes the next sprint.

Sprint S01 is also the pilot. Before S02 is written, the planner records in the S01 file: cost per story, review rounds per story, manual interventions, and what changed in the template because of them.

### Art track

Art is its own queue, `docs/sprints/ART.md`, with the same story template and kind `art`. It runs beside the code sprints. An art story depends only on the `platform` story that registers its content.

Two kinds of art story. A texture story gives an image model the prompt, the size and the output path; the owner saves the result. A model story gives a strong model the GeckoLib `geo` and `animation` JSON to write, with bone names, dimensions and animation names fixed in the story. Never mix the two in one story.

- A `platform` story ships placeholder assets: a generated texture at the shipped size and a vanilla cube or item model. The game must run and the reviewer must see the block before any art exists.
- Shipped assets live in `src/main/resources/assets/infestusfrontier/`. File name is the registry id. Block textures are 64x64 and item icons 32x32, exported from image-model sources with `scripts/texture.py`, unless the story says otherwise. Animated organs use GeckoLib models in `geo/` and `animations/`.
- Image masters and renders stay outside the repository; prompts live in the art story. Model sources (`.bbmodel`) are committed under `art/`, next to any generator that bakes them.
- Every asset adds one line to `CREDITS.txt`: file, author, license.
- An art story is accepted on a client capture judged against the catalog description and the Quality bar in `docs/sprints/ART.md`, by the checklist in [REVIEW.md](REVIEW.md) "Visuals". Placeholders never reach the owner's build: a sprint is done only when its art stories are accepted.

## Dependencies

Change a pin only in its own story, with a cold build, a server run and a client run. No dynamic versions.

| Component | Pin |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.249 (metadata accepts only this version) |
| Java | 21 |
| Gradle wrapper | 9.2.1 |
| ModDevGradle | 2.0.146 |
| Parchment | 1.21.1 / 2024.11.17 |
| JUnit BOM | 5.11.4 |

| Mod | Artifact | Status |
|---|---|---|
| Modonomicon | `com.klikli_dev:modonomicon-1.21.1-neoforge:1.120.4` | Required. Renders the guide. `discovery` owns prerequisites |
| GeckoLib | `software.bernie.geckolib:geckolib-neoforge-1.21.1:4.9.2` | Required. Client animation only |
| JEI | `mezz.jei:jei-1.21.1-neoforge:19.56.0.438` | Optional. API on compile classpath. Runtime with `-PwithJei` |
| Curios | `top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1` | Optional. API on compile classpath. Runtime with `-PwithCurios` |
| Jade, FTB Quests | none | Not used. Probe and console give diagnostics. Pack authors own quests |

Optional mods live behind an adapter in `integration/`. The mod works without them, and a test proves it. Before a release, read the license notice of every resolved artifact (expected: JEI and GeckoLib MIT, Curios LGPL-3.0, Modonomicon MIT with separately licensed assets). `LICENSE.txt` says CC BY 4.0 and old metadata said All Rights Reserved; the owner decides before any release.
