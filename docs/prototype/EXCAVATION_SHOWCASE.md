# Ground-level excavation showcase

These are **coexisting progression candidates**, not an either/or choice. Manual leaching, branching stair excavation and surface-to-depth strip excavation solve different problems. This experiment makes them playable together; player-level gates and final balance are deliberately left for the next discussion. The older cradle, crucible and horizontal miners remain available.

## Start here

Run from this prototype repository:

```bash
./gradlew runExcavationLab
```

Use this **new launcher**, not `runDownMiningLab`, for the ground-level comparison. It leaves Minecraft open. Wait for **EXCAVATION LAB READY**; preparation may take around a minute after joining and briefly moves your player while issuing ordinary start commands. You arrive looking at the **MANUAL LEACHING** station.

On first use, the shared lab preparation task copies the existing `New World` save into `Biology Lab`. If neither save exists, create `New World` once through `runClient`, leave it, then launch the lab. Existing Biology Lab saves are not recopied.

This is a disposable **Biology Lab** fixture. Every launch replaces terrain at **X436–518, Y202–235, Z−7–28**, resets the two known mining controllers/output chests and your lab inventory, and replaces its three labels. Do not keep builds, cargo or player equipment here. Unknown block entities, loose items or mobs in the reset area stop preparation instead of being silently deleted. Use ordinary `runClient` for persistent gameplay without fixture resets.

If you see **EXCAVATION RESET SKIPPED**, the preflight check found added content it must not overwrite. The message identifies the first blocking block and coordinates (or an entity obstruction). No reset has occurred: your blocks, fixture chests and inventory remain intact, and you can keep playing. Preparation stays disabled for that session—removing the obstruction does not unexpectedly trigger a reset. To keep your edits, use `runClient` next time. To deliberately rebuild the showcase, move added blocks/items/mobs outside the reset region before relaunching `runExcavationLab`. A canceled reset cannot be resumed by a stale capture acknowledgement.

| Station | Position | What to try |
|---|---|---|
| Manual leaching, untreated | X448 Y229 Z0 | Apply a nodule to the central grey stone's **top face**. |
| Manual leaching, treated | X440 Y229 Z0 | Inspect the ore visible through the porous rock; mine the rock, then the ore. |
| Finite root excavation | X480 Y230 Z0 | Walk down the substrate stairs, inspect the branch junction, use the probe on the surface organ. |
| Deep-strip excavation | X512 Y230 Z0 | Explore a real 5×24×16 open excavation and its lit climbing spine; inspect output and console. |

All area borders are **mutated blocks in the ground at Y229**, flush with surrounding terrain. Only the controllers and their rear output chests stand above ground; they are not perimeter markers. The floating labels are lab instructions, not required structure parts.

Hotbar: **1** Leaching Nodules, **2** iron pickaxe, **3** Synaptic Probe, **4** Survey Culture, **5** Spore Culture, **6** rotten flesh, **7** Strata Maw, **8** Excavating Rootstock.

## Manual leaching: the hands-on test

1. Start with the grey 3×3 rock patch at X448. Right-click its middle top face with a **Leaching Nodule**. One dose treats the exposed plane; nothing is placed above it.
2. The stone becomes translucent, porous **leached rock**. It remains solid underfoot. The ore directly beneath it is unchanged and visible through the membrane.
3. For a meaningful mining comparison, stand on the ground and run `/gamemode survival`. Creative already breaks blocks instantly and suppresses ordinary drops, hiding the benefit being tested.
4. Use the iron pickaxe. Treated host rock breaks instantly, returns its normal host material and causes no ordinary pickaxe abrasion. Ores still take their normal mining time, tool tier and durability.
5. Compare with untreated stone elsewhere. `/gamemode creative` restores flight for viewing the other fixtures.

The patch at X440 is already treated through the actual nodule interaction, not just a visually similar mockup. Use it as a reference. You can also treat walls or ceilings: the clicked face determines the plane. Treatment supports stone, deepslate, granite, diorite, andesite and tuff; it skips ores and unknown blocks. Nearby fluids, falling terrain, protection or unloaded boundaries can refuse treatment without consuming the nodule. A ten-tick cooldown bounds attempts.

**No boundary is mechanically required for manual leaching.** Its flush frame in this showcase simply identifies the test patch. Craft four nodules from one Spore Culture, one slime ball and two rotten flesh. A separate **Manual Leaching** entry is now in the Waking Genome.

## Build with mutated ground, not raised ribs

First apply **Spore Culture** to supported ground to create Living Substrate. Then apply **Survey Culture** to each exposed substrate cell in the perimeter. It becomes a passive **Surveyed Substrate** marker: a full ground block with a continuous macro-pattern and small luminous pores. It does not spread, tick or raise the walking surface. Craft eight Survey Culture from one Spore Culture and one bone meal.

Place the controller above ground, facing into the intended dig. Its near perimeter edge is **one block ahead and one block below the controller**. Center the footprint on its facing direction:

| Controller | Outer ground perimeter | Marker count | Interior |
|---|---|---|---|
| Excavating Rootstock | 7 wide × 8 long | 26 | Extra length lets four-block headroom pass beneath the far border. |
| Descending Cradle / Digestive Crucible | 7×7 | 24 | 5×5 working opening. |
| Strata Maw | 7×26 | 62 | 5×24 open strip. |

The old raised Boring Rib collars remain supported for the three older downward organs. Use one complete style, not a mixture. Strata Maw requires the ground perimeter. The probe console identifies a missing boundary position. Markers cannot be harvested as placeable blocks; mutate a new ground cell when replacing one.

## Deep strip: one approved, finite excavation

The **Strata Maw** cuts a surface footprint downward, finishing one 120-cell layer at a time. It is a large open cut, not an underground tunnel or an ore generator. The showcase contains a completed **16-block depth** example; all **1,920 blocks** were actually mined and their ordinary output placed in the organ/rear chest.

For a new site, use the **Synaptic Probe** on its controller. While paused, before any work, the depth button cycles **16 → 64 → 160 → 16**. Check the selected depth in the footer, supply biomass and choose **Start**. Depth locks once any work has advanced. Pausing/resuming is allowed; enlarging a spent plan is not. Build a fresh site farther along for another strip. These selectable limits are experimental stand-ins for future progression, not implemented player levels.

The 160-depth preset needs terrain above the dimension's minimum height. It does not bypass bedrock, fluid hazards, protected blocks or unloaded chunks. If a plan reaches those boundaries, work stops there with a diagnostic; it does not promise an unobstructed route to its nominal depth. The floating lab geology is only 28 blocks thick, so it is a 16-depth visual fixture, **not** a complete 160-depth terrain demonstration. Test deeper fresh sites in appropriate natural terrain.

Each actually removed block costs **six biomass**. A completely solid 16-depth strip costs 11,520; a solid 160-depth strip has at most 19,200 targets and costs 115,200. Empty air yields no material and costs no mining fuel. The organ holds 4,000 biomass; use an existing biomass connection or the console's Feed rotten flesh action (250 per item). The lab temporarily tops up its two organs during preparation only; after READY, normal resource rules apply.

Automated output never spills into the world. There are nine internal output slots plus extraction capability and export to compatible storage immediately **behind** the controller. Full output pauses mining. A single rear chest cannot hold the largest job: automation and capacity planning matter. The lab's prepared resources are real output, not prefilled loot.

A passive, light-emitting **climbing tendon** grows on the near corner after each completed layer. In this south-facing showcase it is at **X514, Z2**. Walk against it and use native climbing movement to return to the surface. It does not need continued fuel. The far end of a large cut is not guaranteed spawn-proof; add lighting where needed. No moving platform, terrain stairway, automatic containment or chunk loader is included in this strip option.

## Feedback loop and next discussion

`./gradlew runExcavationShowcase` resets and prepares the same fixture, captures eight acknowledged views/native consoles, and exits. Images are `run/screenshots/excavation-*.png`. It is an automated visual check, not the stay-open launcher.

The functional tests cover complete 16-depth resource conservation, saved progress at the 160-depth boundary, authoritative depth selection and budget refusal, native recipes/unlocks, actual ground mutation and root headroom beneath the flush border. The older manual-leaching tests cover harvesting, ore preservation and durability. See [TESTING.md](TESTING.md) and [PERFORMANCE.md](PERFORMANCE.md) for evidence and limits.

After trying these together, decide which belong at early, middle and advanced levels; which require reagents, DNA or dimension-specific substrate; how a new site should be established; and whether the planning/automation payoff justifies each mechanism. No option has been removed or assigned a final tier in this iteration.
