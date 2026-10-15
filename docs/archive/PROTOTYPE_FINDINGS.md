# Prototype findings

For the owner. Agents do not read this. One section per V3 prototype experiment: what was built, what worked, what failed, and the decision that followed. Full records are in the `InfestusFrontierModV3` repository under `docs/`.

## Living Substrate and vegetation
- Built: three growth stages (seeded, veined, mature) with a 4×4 world-aligned surface pattern stored in block state. Bone meal advances a stage and converts one neighbor. Spore Culture infects trees: logs, leaves, grass and bushes convert into mod blocks that drop original species material only when a player harvests them.
- Worked: no block entity for rendering, no item storm from conversion, per-dimension work budget, loaded-chunk-only conversion. 73 unit tests and 72 GameTests passed.
- Failed or open: mirrored pattern edges give visible reflection symmetry. Player-built logs and natural logs look the same to the infection.
- Decision: substrate stays player-controlled, varied and useful (D011). Three-stage cells and function mutations are in `LIVING_SUBSTRATE_MUTATIONS.md`.

## Expandable biomass reservoir
- Built: solid cuboid up to 4×4×4, 1000 biomass per block, grows by complete rows or layers, contents survive invalid shapes and reload, bottle withdraws a 1000-unit capsule, custom model loader bakes one rounded bladder.
- Worked: conservation across every tested expansion and split. 1088 state models render.
- Failed or open: transfers pause during an incomplete expansion. No automatic growth or hopper access.
- Decision: Reservoir Cells, complete-row expansion and "invalid additions wait outside the body" are in `BLOCK_CATALOG.md` T1-07.

## Fixed DNA extractor
- Built: three-layer fixed multiblock, one specimen per click, 16 samples complete a family, pays 250 biomass into an adjacent reservoir.
- Worked: refuses without consuming the specimen when storage is full or unloaded.
- Decision: replaced in design by pouch → dock → Specimen Extractor → DNA Bank with coverage, not a 16-count family (`ITEM_CATALOG.md` samples section, `BLOCK_CATALOG.md` T2-01, T2-02).

## Symbiotic armor
- Built: four pieces (Crown, Thorax, Legguards, Treads) with an open-carapace model, dye like leather, native trims and enchantments, three cheap reversible branches (Strider, Gill, Bastion) applied by sneak-use with a catalyst, level 1 to 3, full-set gates, Curios Resonance Organ as optional bonus, suit biomass store with HUD meter.
- Worked: mutations keep enchantments, trims, damage and names. Full gate passes with and without Curios.
- Failed or open: reversible branches were a test shortcut. Trim UVs are not certified for every pose and skin.
- Decision: permanent per-piece branches, shared capacity, counters, fusion tree and hunger (`ARMOR_EVOLUTION.md`). Suit biomass display with half-transparent background and placement settings (D017). Activities must consume biomass later (D017).

## Vascular lift
- Built: sealed 3×3 shaft of Vascular Shell with lift organs top and bottom; look up or down to move. 8 magic damage per interval without a qualifying suit.
- Failed: the owner reported it did not work reliably in play. Parked, not fixed.
- Decision: not in the design catalogs. Climbing Tendon and Travel Tissue cover vertical movement instead.

## Tissue transport (veins, stitches, junction, console)
- Built: mature substrate specialized into item (rose), fluid (green) or courier (cyan) channels with a chosen outlet. Transport Organ moves one item or 250 mB about once per second over at most 64 cells. Vascular Stitch crosses a one-block ledge. Vascular Junction joins pumped inputs and serves four outputs round-robin, 250-unit buffer, optional reservoir on top, bone meal grows it two blocks tall. Synaptic Probe opens a console with Auto, In, Out, Shut, Trace and Pause per arm.
- Worked: 88 unit tests and 96 GameTests including conservation across patch → junction → reservoir → defense ammunition, nine simultaneous menus, chunk-edge isolation.
- Defects found and fixed: a paused junction still moved biomass through its capability; a changed arm kept a stale trace; a vein appearance update loaded a neighboring chunk through vanilla shape propagation; lab reload accumulated old mobs because block loading preceded entity loading.
- Open: isolated same-cell crossings, item and fluid splitters, remote diagnostics.
- Decision: Biomass, Fluid and Item Veins, Vascular Junction, Septum Crossing, Vascular Stitch, Filter and Overflow Valves and Synaptic Console are `BLOCK_CATALOG.md` T1-01 to T1-16. Pulses stay cosmetic. The chunk-edge lesson is in REVIEW.md "World access".

## Digestive Tissue and the biomass loop
- Built: fermented spider eye turns mature substrate into Digestive Tissue. Living entities without a full suit take 2 damage per second; 10 biomass per health point lost, 20 per contact at most, 250 per cell, patches up to 64 face-connected cells. Automated kills suppress loot and XP.
- Worked: patch → vein → reservoir → Aerocyte Bloom loop with real harvested fuel. 64-contact stress case passed.
- Decision: Digestive Tissue is `BLOCK_CATALOG.md` T1-21. Loot suppression on automated kills stays the rule for defense organs.

## Helpers (Cystling, Gatherling, Nutrient Sail)
- Built: Brood Cradle on a 3×3 floor hatches up to three helpers (six with a Sail within 12 blocks), each walking a flat cyan trail of 4 to 64 cells with eight items. 20-minute lifespan. Dimension cap of 64 symbionts. Shears retire a helper and return cargo.
- Failed or open: helpers pass through each other; no climbing, no re-home command, no 3D paths. A short death-animation delay held reservations.
- Decision: helpers are optional specialists, never required for stationary logistics (`BLOCK_CATALOG.md` T2-13, T2-14, `PROGRESSION_MAP.md` SR2). Sail chunk loading needs an owned stationary anchor, one explicit chunk, caps and upkeep before it exists (T6 Sail Roost).

## Aerocyte Bloom (aerial defense)
- Built: fed by flesh or 250 biomass per eight shots, 64-shot magazine, targets the `aerial_threats` tag within 24 blocks with line of sight, one shot per 40 ticks for 4 damage. Instant hit with a cosmetic pellet.
- Worked: real damage to a Blaze, obstruction, persistence and a 64-organ stress case in GameTests.
- Decision: defense organs are deferred to M9. Kill-loot suppression and tag-based targeting carry over.

## Horizontal mining organs (Root Borer, Tunnel Maw, Vault Maw)
- Built: three fixed frames of Boring Ribs, cutting 1×2, 3×3 and 7×5 passages to 24, 48 and 96 blocks, one cell per operation, native loot without global loot modifiers, cargo in a nine-slot buffer and rear chest, owner online in the same dimension.
- Worked: 28, 98 and 434 real blocks mined in the lab; conservation from reservoir through vein to chest.
- Failed or open: straight horizontal jobs only. Cannot tell a player's stone wall from geology.
- Decision: horizontal boring is evidence, not a production limit (D013). Downward and traversable excavation is primary.

## Downward mining (Descending Cradle, Excavating Rootstock, Digestive Crucible)
- Built: shared 7×7 collar. Cradle lowers a 3×3 deck one layer at a time to 12 blocks with a lit climbing tendon. Rootstock cuts 3-wide stairs with 4-block headroom, branches left and right, each tip extendable once to 24 m, work focused or shared between tips. Crucible digests a sealed 5×5×3 batch into inert digestate and recovers it later.
- Worked: all three with real terrain, native loot, rider movement, reload and partial recovery.
- Owner feedback: wants exposed ground-level markers, light, stair-like access, usable headroom. Likes the climbing tendon. Prefers a long one-block-wide strip.
- Decision: keep several mining methods with different roles (D015). Descending Rootstock, Descending Cradle, Digestion Crucible and strips are in `BLOCK_CATALOG.md` T2 to T4 and `PROGRESSION_MAP.md` SR2 to SR3.

## Ground-level excavation (Surveyed Substrate, Strata Maw, Manual Leaching)
- Built: flush Surveyed Substrate markers replace raised rib collars. Strata Maw cuts a 5×24 open strip to 16, 64 or 160 depth, 6 biomass per block, lit climbing tendon per layer; 1,920 blocks mined in the showcase. Leaching Nodule treats a 3×3 plane of host rock into translucent porous rock that breaks instantly with zero tool wear; ores stay normal.
- Open from the record: is a 9-block preview too informative, is instant removal too generous, does repeated application feel like busywork.
- Decision: Manual Leaching is in the mod (D016) as Leaching Gland and Leached Rock (`BLOCK_CATALOG.md` T0-14, T0-15). Flush markers are the construction style. Depth presets stand in for progression.

## Burrowing
- Built: Burrowing Ganglion grafted into a full suit. Hold use to move 1.8 blocks per second through stone-type rock, which turns into a shadow state and hardens back after a 10-second grace. Suffocation is real. 2-second cooldown, 1 biomass per step (2 with the light graft at light level 12). Vertical travel only inside ground; no flight. Ledger of at most 256 positions per dimension, 16 restorations per tick.
- Owner feedback: rejected a permanent lock and a 3-second window. Accepted the 10-second grace and 2-second cooldown.
- Open: shared terrain means other entities can enter the passage; multiplayer latency and cooperative passage untested.
- Decision: burrowing crosses ground without extracting resources (VISION). The Burrowing contract and full-set requirement are in `ARMOR_EVOLUTION.md` section 10.

## UI
- Decision: native `AbstractContainerMenu` and `AbstractContainerScreen`. Shared palette, panels, slots, progress, animation and short-integer sync live in one ui foundation module; features compose it. Revisit a UI library only before a screen that needs two of: reflowing layout, editable node graphs, reusable form controls across many screens, complex bound widgets, an in-game editor.

## Art
- Decision: original biological art with recorded provenance. Anatomy explains function: cuticle, muscle, membrane, sensory organs, pulsing circulation. Mechanical frames, arrows and crates are not the visual language. Rounded geometry on the outside of a formed structure only; the grid stays rectangular. The old prompt files and source PNGs were dropped from V4 because all art will be made new.

## Process lessons (from V3_dev, see LESSONS_LEARNED.md)
- A plan is good when it enables cheap reliable progress, not when it is complete.
- Plan in detail only the next milestone. Keep tasks to one independently acceptable outcome.
- Let repairs cross module boundaries; review scope, not file lists.
- Keep orchestration smaller than the work. Validate behavior, not ceremony.
- Qualify the process with a small real batch before expanding the queue.
