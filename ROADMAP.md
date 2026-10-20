# Roadmap

Each milestone ends in a playable build. Only the current sprint is planned in detail, in `docs/sprints/`. Later milestones stay at one line each here.

Now: M0 and M1 are done; S01 and S02 are accepted. M2 is in progress: the Bowl, the Sac, the Bladder, the Bud, the probe and the guide are in; the Rack, the Loom, the Cradle and the four awakened pieces are in; the batch organs run on their own and take hoppers; the Bio-Furnace smelts on biomass and grows through three stages. M3 is in progress (`docs/sprints/S04.md`): the Activation Cyst and the eight treatments are in. Final art: ART-004, ART-005, ART-006, ART-007.

## Milestones

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

## Acceptance evidence

Each milestone needs this evidence beyond the full gate (`bash scripts/verify.sh`). The owner plays the build at the end of each sprint.

| M | Evidence |
|---|---|
| M0 | Clean build; core and ui tests; one GameTest in a real server; `core` and `ui` have an empty classpath |
| M1 | Spore Culture converts eligible exposed ground and bone meal matures it, in GameTests; recipes and guide pages exist; client captures of all three stages, front, back and inventory scale, pass the capture checks and are read by the reviewer; final textures accepted at the ART.md quality bar; the owner repeats it by hand in a fresh world |
| M2 | Fresh Survival acquisition and recipe reveal, empty/full inventory, insufficient biomass, break/reload preserving one organ history; client models render at all faces and inventory scale |
| M3 | Three complete reference supply cycles with a positive measured surplus; seed/water/restart reserves retained; 3D branches/crossings and port direction inspected visually; full outputs and unloaded routes cannot spill or duplicate |
| M4 | All sample qualities and capacities, last-slot contention, no-drop vanilla species, duplicate death callbacks, relocation across dimensions, copied-record merge, consumed sample never recreated; no ordinary-slot pollution or nested pack duplication |
| M5 | Every activity has one owner, sum-cap clipping, partial clearing costs, permanent branch rejection, one-heart hunger floor, sleep/rescue persistence; ore conservation against direct mining/Fortune and retained dust |
| M6 | Start with the declared finite kit, replace every recurring fuel/water input locally, manufacture/export lining, then restart without Overworld fuel; wrong dimension/bed/level/mutation each independently refuses unchanged |
| M7 | Walk every excavation type including descent, turn, section continuation and light/access; cavities/fluids/protected blocks/output-full/chunk boundary stop safely; bounded spill containment tested under shared load |
| M8 | First End materials need no shulker genome; first shulker genome needs no R3 service; local powered cultivation restarts from reserve; burrowing cannot fly or erase terrain, and wing fuel exhaustion leaves the specified fallback |
| M9 | Concurrent freight, crash/restart mid-transfer, both endpoint unloads, changed ownership, unsafe destination and full stores; each preserved armor piece has exactly one owner after death/respawn; no unrequested chunk tickets |
| M10 | Catalog-to-registry/recipe/guide coverage for every in-scope entry; helpers obey population/path budgets and release dead reservations; connected storage renders without internal seams; optional-mod and no-content-mod profiles |
| M11 | Fresh-world playthrough plus a second materially different layout; packaged dedicated server with two clients; worst-case shared-budget soak at declared limits; restart/save recovery, dense-base TPS/FPS profiles and human appearance/usability review |

## Outside M0 to M11

The Fold dimension, its boss, and every Fold-dependent recipe and graft. If it comes, it follows M11 as its own sequence: a playable foothold, a native sustainable habitat, adaptive processing, the Manyfold encounter, reconstructed boss stock. Its first habitat cannot require boss loot.
