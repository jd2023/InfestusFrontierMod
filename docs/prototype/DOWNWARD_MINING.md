# Three downward mining experiments

These are playable **coexisting progression candidates**, not three sizes of the horizontal miner and not approved production progression. They run in this prototype repository; the production `_dev` project is unchanged.

**New ground-level comparison:** run `./gradlew runExcavationLab` for an immediately visible **Manual Leaching** station, root stairs and **deep-strip excavation**, with mutated terrain instead of raised perimeter ribs. See [EXCAVATION_SHOWCASE.md](EXCAVATION_SHOWCASE.md). The older launcher below keeps the cradle/crucible comparison; it is not the new ground-level showcase.

## Try them

Run `./gradlew runDownMiningLab`. This uses the disposable **Biology Lab** save, resets three geology fixtures and your lab inventory, and waits for ordinary mining to prepare them. Do not build anything you want to keep in these fixture regions. Other lab profiles are unchanged. The preparation may take about half a minute after joining; it teleports you while giving the machines ordinary start commands.

All surface controllers are at **Y230, Z0**, facing south:

| X | Organ | Prepared experiment | Try next |
|---|---|---|---|
| 320 | Descending Cradle | Six-block shaft, platform and climbable tendon | Stand near the platform center. Probe its glowing central tile and choose Continue. |
| 352 | Excavating Rootstock | Luminous substrate stairs and two side arms, 16 blocks deep | Select a completed side tip, Extend, then Continue: another eight steps, to 24m. |
| 384 | Digestive Crucible | One sealed three-layer batch, 75 actual blocks held inside | Probe the controller, choose Recover batch, then climb into the opened chamber. |

The hotbar contains the **Synaptic Probe**, rotten flesh, Boring Ribs and all three organs. The rear chests contain actual collected terrain, not prefilled outputs. Supply biomass through the existing biomass endpoint, or use **Feed rotten flesh** in the console: one flesh supplies 250 biomass. Feeding consumes the item even in Creative. Full storage pauses production instead of ejecting items.

The hotbar also has **Leaching Nodules** and an iron pickaxe. Try the untreated demonstration wall at **X360 Y232 Z0**. Apply a nodule to the middle stone block, then switch to a pickaxe and harvest the weakened rock; compare its effort with the ore behind it.

`./gradlew runDownMiningShowcase` prepares the same fixtures, captures twelve acknowledged views/screens, recovers the crucible after capturing its sealed state, demonstrates manual leaching, and exits. Images: `run/screenshots/down-mining-*.png`. This profile also resets the disposable fixture. Normal `runClient` does neither reset nor automatic construction.

The Waking Genome has three new entries. Holding Spore Culture discovers the three ordinary crafting-table recipes, including Creative inventory insertion. Recipes are experimental costs, not final progression: five bones, one culture, two iron ingots and one slime ball for the cradle; spider eye instead of slime for rootstock; two diamonds and magma cream instead for the crucible.

## Build a fresh site

Place the controller facing into the excavation. At **the controller's height**, build a 7×7 square **perimeter** of 24 Boring Ribs. Its near edge is one block in front of the controller; its center is four blocks ahead. Leave the 5×5 interior at that height empty. Do not place ribs across the interior. There should be real terrain underneath it.

The same inexpensive collar is intentionally shared for comparative testing. The three mechanisms below are different; their final anatomy, recipes and dimension-specific substrates are not settled.

Open controls using the probe, not an empty-hand click. The console shows biomass, actual mined count, progress, output slots, a diagnostic and the blocked coordinates. Rear storage means the single block directly behind the controller, opposite its facing. Existing extraction capabilities also expose the nine output slots; no inventory can insert ordinary items through this output port.

## 1. Descending Cradle — move the worksite

The core grows a 3×3 deck over the shaft. Continue clears the next 5×5 terrain layer, then moves the platform down one block and stops at a new landing. Repeat to a hard maximum of 12 blocks. The same platform is physically present at its new location; this is not a teleport-only shaft or an upward-facing horizontal miner.

Stand close to the center, with both feet on the deck, to descend with it. Jumping, riding another entity, carrying passengers, standing too near the edge, an obstructing entity or a crowd at the eight-result inspection limit blocks movement. Up to seven safely positioned players can be admitted; this ceiling is not a claim that seven-player latency has been tested.

The green central deck tile opens the **same surface-owned inventory and controls**. It does not duplicate storage or move arbitrary block entities. That menu becomes invalid when its landing moves. Reopen it on the next landing. The control link resolves at most 52 loaded positions and never loads a chunk.

A permanent, illuminated climbing tendon remains alongside the shaft. It uses Minecraft's climbable-block behavior and works without biomass. A missing platform tile or an interrupted tendon stops further excavation. No moving chest/contraption system, upward platform travel, smooth interpolation or passenger AI is implemented. Reach the surface by climbing; it is not necessary to return merely to press Continue.

Actual mining costs six biomass per terrain block; each platform descent costs another 24. Initialization grows inert, non-harvestable deck/access material. Pausing, fuel exhaustion or full cargo leaves the last deck and completed access in place. Excavation can be partway through the next layer, but movement waits for the whole layer to clear.

## 2. Excavating Rootstock — allocate work between branches

The surface core remains stationary. Its first tip cuts a three-wide passage with **four-block headroom**, descending one block per horizontal block. Native half-step collision makes ascent possible without jumping. The floor itself becomes luminous substrate stairs, rather than protruding roots placed in passage air. Each replaced tread's original material goes into output and costs the same six biomass as excavation. Air gaps/unsupported terrain stop tread growth. The initial arm has eight steps. These passive substrate mutations do not spread or assimilate vegetation.

At its end, **Branch** creates two new arms, one left and one right, starting from the junction. Each has another eight steps. Three fixed tip records are the complete job: there is no wandering, ore search, hidden vein detection, pathfinding mob or automatic recursive branching.

- **Focus / share** switches between feeding the selected tip and alternating work between unfinished tips.
- **Next tip** selects an arm and shows its depth.
- **Extend** authorizes a completed side tip's second eight-step section. Select that tip, Extend, then Continue. Each side tip can extend once, reaching 24m below the original surface; the central trunk cannot extend past its branching point. Approval survives reload, does not create a second inventory, and does not remove fuel/output limits.
- **Continue** resumes a paused organ. If a focused tip is finished, choose another tip or switch to shared work, then Continue.
- A blocked shared tip does not prevent the other arm receiving its next scheduled turn. Focus deliberately concentrates attempts on its selected arm.

This tests a distributed work-allocation problem, not just shaft dimensions. Branch locations/directions are intentionally constrained in this version. Arbitrary waypoint drawing, more branch mutations, bridges over caves and selective geology profiles remain proposals. Unsupported treads pause rather than turning a walkable stair into an unsafe fall.

## 3. Digestive Crucible — excavation and recovery are separate

Continue verifies a solid enclosure around the next 5×5×3 batch and caps its opening. It replaces actual terrain, one block at a time, with inert **captured digestate**. Native drops go into a separate nine-slot pending inventory. They cannot be exported while digestion is in progress.

When the batch is ready it stops spending simulation work until you choose **Recover batch**. Recovery removes captured digestate incrementally, separates the held material into output, opens the lid and grows climbing access. Continue extends the opened chamber by another three layers, to a maximum depth of 12.

Recovery can be requested early, including when pending storage fills. Only fully completed layers gain access. Continue resumes the **unprocessed suffix**, not the already paid terrain. A saved recovery cursor prevents later drain operations from trying to digest the access grown after an earlier partial recovery. Empty output to release separation back-pressure.

The medium is a solid, non-flowing representation of captured rock. It is **not a fluid simulation**, does not chemically multiply ore and does not spread into caves. Different reagents, fractionation recipes, heat and containment mutations remain future experiments. Native block drops are conserved without XP or global loot modifiers, as with the existing horizontal mining safety boundary.

## 4. Leaching Nodule — manual entry, not automatic extraction

Craft four nodules from one Spore Culture, one slime ball and two rotten flesh. Holding culture discovers the recipe. Apply one nodule to an exposed rock face: it treats at most a **3×3 plane**, aligned with the clicked face, not a volume or an ore search. Stone, deepslate, granite, diorite, andesite and tuff become porous leached rock. All ores, unknown mod blocks and other materials remain unchanged. Adjacent fluids/falling terrain, unloaded boundaries or denied protection abort treatment before conversion. There is a ten-tick cooldown and shared mining admission.

The porous membrane reveals the immediate geology behind it. It still has solid collision until the player mines it. A correct pickaxe salvages one original host-rock drop instantly with **zero abrasion**, using native hardness-zero behavior; ore still needs its ordinary tool tier, time and wear. This is deliberately a strong, visible reagent-for-labor tradeoff, not settled balance. Wrong tools lose salvage as with native tool-gated blocks. No automatic loot, lifting column, ore multiplication, XP reward, Fortune bonus or autonomous treatment is added. Identity is held in six fixed block states, without a block entity.

Questions for playtesting: is a nine-block preview too informative, is instant host removal too generous, and does repeated reagent application feel like a useful tool or busywork? Candidate tuning is fewer cells per dose, a more costly reagent or slower manual harvesting with a separately designed wear rule. Do not hide these tradeoffs behind arbitrary waiting timers.

## Light and the next worksite

Root stairs emit light 10 without ticking; cradle/crucible access tendons already emit light 8, and horizontal bores retain light-10 ceiling secretions every fourth slice. No torch item entities or light-placement scans are created. These are illumination sources, not a blanket hostile-spawn guarantee across every cave exposed along the route.

Rootstock now demonstrates explicit section authorization. Other hard caps intentionally remain: the cradle and crucible stop at 12m and horizontal miners at their existing lengths. Beyond those caps, **relocate the material-bearing core and rebuild its collar at a prepared landing**, then Start. The cradle's old solid deck must be cleared from the new digging opening, and any abandoned digestate recovered before moving the crucible. Existing access remains passive; no automatic cleanup erases it. A deep landing must have room for the 7×7 collar, so prepare a side alcove before moving. Seamless deep relay organs/automatic collar growth are still a design experiment, not claimed functionality. Old already-finished root jobs retain their old geometry; use a fresh site/lab reset for new stairs.

## Stops, persistence and limitations

- The owner must remain online in the same dimension. Other players cannot commandeer controls. Shared inventories can still be extracted through their advertised capability.
- Targets, frame and relevant neighbors must already be loaded. No chunk tickets or background continuation are created.
- Unknown/unsupported terrain, block entities, native protection denial, nearby fluid/falling blocks, world bounds, full cargo or insufficient fuel stop work safely. Default diamond-pickaxe eligibility is a testing choice, not three production tool tiers.
- Structural edits also check ownership/world permission and cancellable break events at their positions. The prototype does not establish compatibility with every third-party claim system or placement-specific policy; it does not emit a complete player block-placement event sequence for secreted tissue.
- Cargo, pending material, owner and bounded machine state are persisted. Moving the core preserves its material in one data-bearing core item but resets the job and requires an explicit start. Abandoned deck, roots, seals and digestate are inert, have no loot and require manual cleanup. There is no automatic teardown that might erase player changes.
- Biological models reuse the prototype's original custom mining-material art, not vanilla tiles. These native block models establish distinct controls and anatomy; they are not final sculpted machine art or a polished moving-structure renderer.
- Crash-at-arbitrary-instruction transactional durability, hostile third-party loot/capability hooks, dense-server frame/tick times and dedicated-server rider prediction have not been certified.

## Ownership and verification

The `mining` module owns all three behaviors. `CradleCycle`, `RootCycle` and `CrucibleCycle` own their separate state machines; `DownGeometry` owns finite coordinates. `DownCell` adapts them to world safety, ownership, material commits and scheduling. `MiningCargo` now also serves the original miners, centralizing fixed-slot persistence, prepared loot batches, rear export and extraction-only ports. `MiningConsole` composes the existing native menu and shared `ui.foundation` admission/synchronization. Low-level GUI or capture infrastructure was not copied.

Reproduce the native art exports with `javac -d build/art-tools tools/ExportMiningAssets.java tools/ExportDownMiningAssets.java`, then `java -cp build/art-tools ExportDownMiningAssets`. This reuses the existing original mining textures without rewriting them.

For the new surface/nodule assets, compile `tools/ExportExcavationAssets.java` alongside `tools/ExportMiningAssets.java`, then run `java -cp build/art-tools ExportExcavationAssets /path/to/minecraft_1.21.1_client.jar`. It reuses native stair transforms/geometry and existing **original mod textures**, not Minecraft's texture art. No raster art is changed.

Verification includes geometry bounds, custom asset/coplanar-face checks, real terrain and native loot, rider movement and platform control, passive access, branch focus, batch retention/recovery, reload, partial recovery/resume, full output, core drops, structural protection, altered deck, unsealed cave, ownership, native menu replay/range and the shared mining quota. The adjacent original mining tests remain in the full gate. See [TESTING.md](TESTING.md) for recorded runs and [PERFORMANCE.md](PERFORMANCE.md) for hard limits.
