# Mining organs: three playable experiments

These are finite, player-started excavation multiblocks, not mineral generators. They consume biomass, remove actual terrain and collect that terrain's tool-dependent loot. They leave usable passages instead of invisible depleted deposits. This is prototype work in `InfestusFrontierModV3`, not the production `_dev` project.

## Try them

Run `./gradlew runMiningLab`. It opens the **disposable Biology Lab save** with three prepared rock masses and automatically starts the ordinary mining machinery. It resets this fixture on each launch; do not build anything valuable in the lab. After approximately 45 seconds of excavation you can explore the tunnels, inspect their chests and open each core with the **Synaptic Probe**. The lab supplies fuel, not prefilled ore inventory.

Controllers are at **220 230 0**, **238 230 0**, and **256 230 0**, all digging south. Their output chests are immediately north, recessed into the platform. Fly down to floor level to click the core. The hotbar contains the probe, rotten flesh, ribs and all three cores. `runMiningShowcase` runs the same machinery, captures seven acknowledged views and exits; `runMiningLab` stays open.

| Organ | Passage profile | Maximum length | Boring Ribs | Native tool | Biomass/block | Scheduled interval |
| --- | --- | ---: | ---: | --- | ---: | ---: |
| Root Borer | 1 wide × 2 high | 24 | 9 | Stone pickaxe | 4 | 12 ticks |
| Tunnel Maw | 3 × 3 arch; upper row 1 wide | 48 | 15 | Iron pickaxe | 6 | 6 ticks |
| Vault Maw | 7 × 5 vault; upper row 3 wide | 96 | 27 | Diamond pickaxe | 8 | 2 ticks |

At most one target cell is processed per admitted operation. Larger organs remove more ground, handle harder ores and have higher block throughput; they do not instantly erase a whole slice. Maximum cut volumes are **48, 336 and 2,976 blocks**. Empty cells cost no biomass and yield nothing. Quotas, missing chunks, hazards and back-pressure can reduce throughput.

## Build one in your own world

1. Choose a horizontal direction and inspect the entire intended footprint. The experiment can remove player-placed natural stone too: it cannot distinguish your stone wall from geology. Do not point it through a base.
2. Place the core **in the floor**, looking toward the rock you want to excavate. It digs away from you, beginning one block beyond the frame. The console title shows its compass direction. Moving or replacing a core requires an explicit restart.
3. Build the appropriate frame in the same vertical plane as the core. `R` is Boring Rib, `C` is the core and `.` is the open passage. Blank outside corners need no block. Clear the entrance manually. Rib axis is cosmetic; place like a log to align its plates.

```text
Root Borer       Tunnel Maw         Vault Maw
   RRR              RRR               RRRRR
   R.R             RR.RR            RRR...RRR
   R.R             R...R            R.......R
   RCR             R...R            R.......R
                   RRCRR            R.......R
                                    R.......R
                                    RRRRCRRRR
```

4. Put a chest directly **behind** the core, at the core's height. Alternatively, extract its nine-slot buffer with a hopper or existing item logistics. Never rely on dropping output into the world.
5. Supply biomass through a green biomass vein terminating at the core. The existing reservoir/organ/vein system supplies it through the standard biomass endpoint. Or carry rotten flesh, open the console and use **Feed rotten flesh**: one flesh supplies 250 biomass, up to a 4,000-unit tank. The button requires room for the whole batch.
6. Use the **Synaptic Probe** on the core, then press **Start**. Empty hands do not configure it or steal block-placement interactions. The console displays fuel, progress, mined count, cargo, direction, target coordinates and a specific reason when work cannot proceed. Missing-frame coordinates identify the next required rib.

The frame is stationary; the cutting cursor advances into the rock, roof-first. Every fourth slice can leave a small glowing green ceiling membrane, secreted using the same mining fuel. It has no collision, ticker or harvestable drop. It does not produce an ore or a free building material.

## Progression and recipes

Picking up Spore Culture unlocks all four recipes in the native recipe book, including Creative inventory insertion. Actual ingredients gate crafting; this is not yet a research-tree implementation.

- **Boring Rib:** two bones and one slime ball, shapeless → four ribs.
- **Root Borer:** `BRB / ICI / BRB`, where B=bone, R=redstone, I=iron ingot, C=Spore Culture.
- **Tunnel Maw:** `DED / BCB / OIO`, where D=diamond, E=ender pearl, B=blaze rod, C=culture, O=obsidian, I=iron ingot.
- **Vault Maw:** `SNS / DCD / BEB`, where S=echo shard, N=netherite scrap, D=diamond block, C=culture, B=bone block, E=dragon breath.

These costs are provisional experimental progression: early passage → Nether-access machinery → deep-world/End expedition machinery. Final production DNA prerequisites, multiblock mutations, recipe costs and fuel economy remain design decisions. Each core is crafted independently: an upgrade recipe must not accidentally consume a data-bearing old core and destroy its cargo.

## Safety and operating limits

- Full output, insufficient fuel, broken frame, unsupported blocks, protected terrain, unloaded boundaries and nearby fluids/falling blocks stop excavation **without consuming the target or its mining fuel**. Resolve the obstruction to resume; the console gives its coordinates.
- Default boreable terrain is an explicit tag of vanilla geological blocks and ores. Containers, block entities, bedrock, unknown blocks and ores above the organ's native tool tier are refused. Secure lava/water and gravel manually; this version does not drain or shore up hazards.
- The placer owns the organ. Only that player can configure it. Its owner must remain online **in the same dimension**; no fake player or chunk loader is created. Being far away does not itself stop it if the chunks are otherwise loaded. Offline/team ownership is future design work.
- Normal breaking produces **one portable core item containing its fuel and cargo**. Native replacement resets the finite cursor and starts paused under the new placer's ownership. It does not spill the nine cargo slots. Creative destruction retains vanilla's no-drop behavior: use normal survival harvesting when you need to preserve a filled core.
- Pausing stops automatic mining and rear export, but does not disable external fuel insertion or output extraction. On finite completion, use the console/hopper/logistics to empty any remaining internal output.
- No mining XP or loose output entities are spawned. Native block loot tables are used through bounded raw streaming. **NeoForge global loot modifiers are deliberately not applied**: their convenience path constructs an unbounded output list. Third-party mining/claim compatibility is not certified merely because the native break event is honored.
- Jobs are straight and horizontal. Branches require separate placements; there is no moving head, automatic steering, vertical quarry, liquid handling or moving full frame yet. A preview/blueprint overlay would be a useful next usability step.

## Architecture and precedent

The earlier `InfestusFrontier` excavator inspired real-world removal and increasing scale. Its radius/height cursor, hardness-versus-tier comparison and loose item/XP output were not copied. Here, package-private `BorePlan` owns finite geometry, `MiningSafety` owns world/loot admission, and `MiningCell` owns fuel/cargo/cursor transactions. `MiningOrgans` is the registration and small command/query/frame facade. Native menu and screen adapters use shared `ui.foundation` synchronization, drawing and `BlockMenuAccess`; logistics reuses the same menu guard rather than retaining a second implementation. Shared `FixedPattern`, storage/biomass ports and `WorkBudget` remain their respective infrastructure owners.

The image-generation skill supplied **original biological bitmap materials**, not vanilla texture recolors. Models use bounded native cuboids and 64×64 sprites. Source image, exact prompt, built-in-tool provenance and reproducible Java exporter are in [art/mining/README.md](art/mining/README.md). Active cores have a distinct open pose; this experiment does not claim a continuously animated travelling drill.

## Feedback loop

Run the authoritative gate `./gradlew prototypeCheck`, followed by `./gradlew prototypeCheck -PwithoutCurios`. Then run graphical tasks separately: `./gradlew runMiningShowcase` and `./gradlew runLogisticsShowcase`. Screenshots go to `run/screenshots/mining-*.png` and `logistics-*.png`; inspect them, not merely their existence.

Pure tests exercise finite geometry and native asset constraints. Running-world tests cover all three real cuts and retained arch corners, native ore loot, exact fuel accounting, full-output recovery, fixed-slot persistence, hazards/tool tier, missing ribs and canceled break events, the per-dimension budget, unloaded boundaries, finite completion and non-farmable ceiling light, probe/menu authorization, recipes/discovery, portable placement, offline ownership, and **reservoir → green vein → organ → rear chest** conservation.

The lab excavates **28 / 98 / 434 real blocks** from its three 14-block-deep masses before accepting captures. Iteration corrected repetitive ribs, ceiling-light selection bounds, camera height/framing and an ambiguous output caption. A geometric audit then found coplanar outward surfaces in ribs/cores: a new assertion failed against the old models, and the exporter now recesses the underlying flesh/plates to prevent competing faces. A later close inspection also found vanilla chest contents spilling during fixture reset; reset now clears exactly the three disposable chests, bounds stale-item cleanup, waits for entity data and rejects any loose-item capture. Earlier contaminated captures are not valid zero-item evidence. One early run logged an OpenAL shutdown error; this is not claimed to be a mining defect or silently treated as a clean run.

This is local functional and selected visual validation, not dense-server TPS/long-soak, multiplayer-latency, arbitrary datapack, shader-pack or third-party protection certification. See [PERFORMANCE.md](PERFORMANCE.md) and [TESTING.md](TESTING.md) for bounds and final validation evidence.
