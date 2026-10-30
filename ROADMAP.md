# Roadmap

Each milestone ends in a playable build. Only the current sprint is planned in detail, in `docs/sprints/`. Later milestones stay at one line each here.

Now: M0 and M1 are done; S01 and S02 are accepted. M2 is in progress: the Bowl, the Sac, the Bladder, the Bud, the probe and the guide are in; the Rack, the Loom, the Cradle and the four awakened pieces are in; the batch organs run on their own and take hoppers; the Bio-Furnace smelts on biomass and grows through three stages. M3 is in progress (`docs/sprints/S04.md`): the Activation Cyst and the eight treatments are in; the three veins and the Vascular Junction move biomass, water and items; veins climb walls and fork; Mouths under chests and organs route items through Item Veins by filter, amount and priority; the farm plants, waters, harvests and composts on its own; the Sensor Polyp reads a block into redstone; the Fuel Papilla refuels worn armor at base. Final art: ART-004, ART-005, ART-006, ART-007.

## First meaningfully playable version — the living workshop

### Scope and finish line

Ship a complete Overworld Survival loop through M3: grow a workshop, automate its
food and water supply, keep a biomass surplus, and spend that surplus on useful
production. M4 research starts after this version. Completing the organ list alone
does not meet this scope; the connected loop must work from a fresh world.

The player finishes this slice when a colony replenishes its own operating supplies
and runs a Bio-Furnace from surplus while protecting its next farm cycle. They can
then expand production, change the layout or keep using it beside their vanilla base.
This is the first private playable build, not the full campaign or a public release.

### Player experience

1. Start beside a vanilla home. Discover Spore Culture, choose the colony's footprint
   and grow Living Substrate. All required inputs are obtainable in the Overworld.
2. Feed the Digestive Sac by hand. Store biomass, make starter components and run
   the first useful batch in the Bio-Furnace. Manual operation remains a recovery path.
3. Build one renewable crop line. Route planting stock back to the farm, retain
   player food and send surplus crops to digestion. Supply water automatically from
   a renewable vanilla source using the Vascular Junction.
4. Connect storage and consumers. Set ports, filters, priorities and reserves. Read
   why an organ is waiting and correct a blocked route without consulting source code.
5. Add furnace demand. See the surplus shrink, then increase production or limit
   consumption. Save, leave the area, return and resume the same colony.
6. Craft and awaken an armor piece, then fill it at the base. The four existing
   pieces remain available; completing a full suit is not required.

Target pacing for playtesting: first useful biomass-funded production within
30–45 minutes of starting the mod beside an established vanilla home; a working
automated colony in 2–4 hours. These are starting targets, not measured results.
Fresh-world testing also records the vanilla preparation time separately.

### Required content

| Area | Minimum for this version |
|---|---|
| Foundation | Deliberate substrate growth, maturity, placement and readable organ foundations |
| Workshop | Bowl, Sac, Bladder, Bud, Rack, Loom, Cradle, Bio-Furnace and Activation Cyst; their required ingredients and treatments |
| Renewable supply | One complete crop route using Cultivation Tissue, Seed Pouch, Planting Proboscis and Harvest Corolla; Compost Gland available for surplus seeds |
| Water | Automatic local collection and distribution; no recurring bucket delivery once the line is running |
| Transport | Biomass, fluid and item veins; Junction, Mouths, Feeding and Yield Lobes; branches, vertical routes, Septum Crossing and Vascular Stitch |
| Storage and control | Item Capsule, Biomass Reservoir, Filter Valve and fullness Sensor Polyp; protected planting, water and restart supplies |
| Equipment | Existing dormant/awakened pieces, Cradle awakening, Bladder filling and Fuel Papilla refueling |
| Discovery | Guide, recipe reveal, advancements and probe diagnostics covering the entire required route and the finish line |
| Presentation | Accepted art for required content; readable inventory icons, connections, flow direction, operating states and screens |

Recipes and balance values remain in the [item](docs/design/ITEM_CATALOG.md) and
[block](docs/design/BLOCK_CATALOG.md) catalogs. The current implementation plan is
[S04](docs/sprints/S04.md). This section defines the playable boundary, not new recipes.

Armor is an introductory project in this slice. Its current reserve and refueling
support do not establish a fuel-consuming ability. The furnace is the required
useful biomass sink; neither the guide nor acceptance claims armor learning,
self-healing or mutations before those behaviors exist.

### Decisions the player must make

- Divide harvest between planting stock, food, compost and biomass. One crop cannot
  pay every route at once.
- Reserve enough biomass and water to restart before feeding discretionary consumers.
- Trade space and construction cost for throughput. A larger store buffers demand;
  it does not fix a colony that consumes more than it produces.
- Choose routing and consumer priority. A full or unavailable destination must be
  understandable and recoverable through normal play.

### Outside this version

DNA collection and banks, genome completion, graft research and Mutation Chamber;
armor learning, permanent fusion branches and advanced abilities; ore treatment
and excavation; tree, aquatic and animal production branches; electricity, heat
and pressure; Nether and End industry; cross-dimensional travel and freight;
helpers, chunk loaders and combat systems. These remain later milestone work.

Only one renewable crop route must qualify. Extra species, alternate production
chains and advanced controls do not block this build. Vanilla redstone, hoppers
and containers remain valid tools. JEI and Curios are optional, not prerequisites.

### Acceptance: playable means all of these pass

1. **Survival bootstrap.** Complete the route in a fresh Survival world using the
   packaged mod and required dependencies. No commands, Creative items, prepared
   demo stock or developer instructions. Every prerequisite is obtainable before
   the recipe that needs it. The guide identifies the next project and its inputs.
2. **Sustained production.** Run three complete harvest-to-replant supply cycles
   after startup. Record starting and ending crops, seed reserves, water, total
   biomass across stores and routes, and useful output. Include all operating costs;
   moving a startup buffer into another store is not production. With discretionary
   consumers off, each cycle leaves a positive biomass surplus and retains enough
   supplies for the next cycle. Then run a declared furnace workload for three more
   cycles without manual feed, reserve depletion or a downward biomass trend.
3. **Useful reward.** The automated line produces smelted goods the player can use
   in ordinary Minecraft. The player also awakens and refuels one armor piece.
4. **Recovery.** Fill an output, interrupt water, break a route and unload the base.
   Each condition pauses the affected work without duplication or unintended loss.
   Restore the condition and resume. Save/reload with a batch and a transfer active;
   contents, settings and organ history survive. Retained reserves allow a restart.
5. **Layout choice.** Build a second materially different route with a vertical run,
   branch and isolated crossing. The loop works without copying the demo's geometry.
6. **Readable play.** The owner completes the loop using the guide and in-game
   feedback. Record confusing steps and long waits; fix progression blockers before
   acceptance. Required art meets the existing [art quality bar](docs/sprints/ART.md).
7. **Build and server health.** Pass the [engineering gate](ENGINEERING.md#testing)
   and M2/M3 evidence below. Smoke-test the packaged build on a dedicated server
   with two clients using the same stores and routes. The full campaign and broad
   multiplayer qualification remain M11.

The remaining work is to finish S04, qualify the connected Survival route, close
bootstrap and usability gaps, and accept the required art. M2 and M3 stay in
progress until their evidence passes. New systems do not substitute for that evidence.

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
