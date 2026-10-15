# Biology lab prototype

Run `./gradlew runBiologyLab` to explore the experiment. On first use this copies `run/saves/New World` into `run/saves/Biology Lab`. Close other Minecraft development clients before making the initial copy. The lab rebuilds a demonstration platform at Y=230, replaces the lab player's inventory, and supplies infected armor on every launch. It changes only the copied lab save. It stays open for manual play.

Run `./gradlew runBiologyShowcase` for an automated run. The server signals ten explicit phases; the client waits for each signal, lets rendering settle, captures it, and exits after all ten. Phases seven/eight inspect a full and an empty reservoir; nine/ten show vegetation before and after assimilation. Images are written under `run/screenshots/biology-*.png`. Client ticks are deliberately not used to predict the server's phase.

The interactive lab starts you flying; double-tap Space to land and try the fast lane. Its hotbar contains reservoir blocks, flesh, bottles, bone meal, redstone, bones, an extractor, and substrate. The nearby extractor is already assembled. For the main reservoir, add a complete four-block end wall (two high and two deep) to expand the initial 3×2×2 box to 4×2×2.

## Living Substrate

### Vegetation experiment

The lab now has an oak at **2,231,8** and an untouched comparison tree at **-7,231,8**. In the interactive lab, real Spore Culture inoculates the ground under the first tree after four seconds. Grass/fern/dead-bush/berry examples are at **X=0/2/4/6, Y=231, Z=5**. Extra culture and an axe are supplied in inventory. Random tree growth is gradual; bone meal on an active infected log/canopy speeds it up. Land on the platform, then use `/gamemode survival` to test harvesting: Creative breaking intentionally drops nothing. The automated showcase uses real culture and bone meal for its before/after captures.

To reproduce elsewhere, use Spore Culture on the ground under vegetation, or bone-meal neighboring substrate until it reaches that ground. Short/tall grass and ferns become Spore Brush (one seed when broken); dead bushes give two sticks, berry bushes two berries. Tall plants yield one brush, not one per half. Logs become Colonized Logs: each harvested cell returns one of its original wood species. Infection climbs, descends and follows face-connected branches and foliage. Canopy Cysts replace leaves and never decay; breaking one gives rotten flesh, which can be fed into a reservoir for 250 biomass. **Conversion itself drops nothing**, so a loaded infected forest cannot produce an unattended item storm.

Eight vanilla log species, dry mangrove roots and vanilla leaves are supported. Flowers, saplings, crops, stripped wood, waterlogged vegetation and unknown mod blocks are not converted. Player-built logs are not distinguished from natural ones: leave an air gap from wooden structures. This is not yet plant DNA capture or automated forestry/logistics. Trees can spread their infection naturally; the ground itself still spreads only with bone meal. Work is globally budgeted per dimension, and unloaded chunks are never forced open. See [performance bounds](PERFORMANCE.md#vegetation-assimilation).

### Ground appearance and movement

Vegetation verification (2026-09-06): `prototypeCheck` passes with and without Curios (73 unit tests, 72 required GameTests). The ten-phase `runBiologyShowcase` passes and its final before/after captures were inspected. These gates include original-species Survival harvest callbacks, block-state persistence, six-axis propagation, dormant/rearmed branches, a 64-plant quota/retry fixture and conversion at a loaded/unloaded chunk edge. They do not certify large-forest FPS/TPS or a multiplayer soak. Assets can be reproduced with `java tools/ExportVegetationAssets.java`; the exporter reuses existing original materials and writes only vegetation models/blockstates.

- Three stages: seeded (dark, sparse), veined (coral vascular network), mature (violet membranes and nutrient nodes).
- A world-aligned 4×4-block surface pattern uses sixteen 64×64 tiles per stage. Coordinate selection includes negative coordinates and is stored in block state; rendering requires no block entity or per-frame world search.
- Macro-pattern edges mirror continuously. Stage variants retain a common boundary band so neighboring stages meet without a hard color seam. The tradeoff is some visible reflection symmetry, which remains open for art direction.
- Side textures also tile; their scale is one block. They do not yet wrap the top macro pattern around cliffs.
- Normal random ticks can advance maturity, with no autonomous spread. Bone meal advances one stage and can also colonize one supported neighboring natural block in three dimensions.
- Apply redstone to mature tissue to specialize it as a cyan-marked fast lane. Wear four Symbiotic armor pieces, each Strider-mutated or level 2+, and walk on it for Speed I (Speed II with the Curios Resonance Organ). It fades within half a second after leaving. Existing stronger Speed effects are preserved. See [COLONY_LAB.md](COLONY_LAB.md).

## Fixed DNA extractor

Build a three-layer structure:

```text
Top (Y+1)       Machine level (Y)      Floor (Y-1)
 . . .             C . C                S S S
 . N .             . E R                S S S
 . . .             C . C                S S S
```

`E` = DNA Extractor, `C` = Chitin Lattice, `N` = Synaptic Core, `S` = Living Substrate. `R` is an adjacent reservoir port; any horizontal side works. Dots are open spaces in this example, not required air. The fixed template checks nine floor blocks, four corner blocks, and one crown.

Right-click the extractor with one specimen: rotten flesh → Rot, bone → Bone, wheat seeds → Flora, ender pearl → Rift. It records one sample and deposits 250 biomass into an adjacent reservoir. Sixteen samples complete that family. The names, counts, and specimen shortcuts are prototype rules, not the final DNA ontology. Captured DNA does not yet unlock additional equipment abilities.

The extractor gives the first missing part and its offset when incomplete. Full, invalid, unloaded, or temporarily busy storage pauses extraction without consuming the specimen. Empty-hand interaction reports the structure and all four sample counters. There is a half-second interaction cooldown and no background production loop. The controller emits a short light pulse on success; its crown uses the existing Synaptic Core animation.

## Expandable reservoir

- Assemble a **solid cuboid**, from one block up to **4×4×4**. Hollow shells and L shapes are invalid.
- Each block contributes **1000 biomass capacity**. Every part reports and accesses the same total.
- Right-click any part with rotten flesh to insert 250 units, or with a biomass capsule to insert 1000.
- Right-click with a glass bottle to withdraw a 1000-unit capsule into available inventory space. A full inventory pauses withdrawal; no item is thrown into the world.
- Buckets do not carry this biological resource. Use a glass bottle to withdraw a capsule, then use the capsule on another reservoir. The capsule is consumed on deposit; this prototype does not return an empty bottle.

The reservoir now has a continuous muscular underside beneath a warm translucent membrane. Only the bottom layer grows the sole; upper cells do not repeat it. Flesh replaces membrane faces instead of overlaying them, preserving connected surfaces and separated liquid rendering. The original muscle material is mechanically cropped by `tools/ExportReservoirFlesh.java` from `art/defense/aerocyte-source.png`.

Tree infection is **not implemented**: Spore Culture accepts dirt-tag blocks, mud, netherrack and End stone, not logs or leaves. Bone meal matures substrate and converts at most one eligible neighboring ground block. Waiting near a tree will not infect its trunk or canopy. Plant/tree assimilation and DNA harvesting need their own bounded conversion experiment.
- Empty-hand interaction shows dimensions, stored biomass, and capacity. Exact quantities remain authoritative; the visible liquid height is quantized to sixteenths of a block.
- Add a complete row, column, or layer to grow it. During an incomplete expansion, transfers pause. Completing the rectangle redistributes contents without loss. A larger capacity lowers the visible surface for the same amount.
- Cells persist bounded shares of the contents. Breaking a block in Survival yields one block item with that share attached; placing it restores the share. The extractor likewise retains its sample counts in its dropped block item. Creative destruction and commands that replace blocks without drops are destructive, as in vanilla.
- There is no automatic growth, external fluid capability, hopper integration, or remote tissue transport yet. These are choices for subsequent prototypes.

The reservoir forms one rounded rectangular bladder, with no internal frames or membrane walls. Only the multiblock's outside is rounded; adding cells does not leave per-cell scallops. Its inset biomass surface is separate from its pearlescent translucent skin. The placement, collision and selection grid remains block-shaped; Minecraft's black targeted-block outline is not a rendered frame. The extractor is unchanged.

Sparse flesh patches now hug the outside corners, using the original mature-substrate art. They replace selected membrane polygons rather than adding overlapping layers, so geometry counts are unchanged. An incomplete non-rectangular expansion still temporarily displays separate cells and pauses transfers. The delayed-expansion regression verifies that 2750 stored units survive the invalid-shape fallback, per-cell serialization and subsequent reformation into a larger 6000-unit reservoir. No biomass or per-cell data is discarded by this fallback; operational capacity resumes once the cuboid is complete. Keeping the previous valid substructure operational during construction is deferred, not claimed here.

Original [skin art and its exact built-in imagegen prompt](art/substrate/RESERVOIR_SKIN.md) are committed alongside the exporter. Liquid biomass retains its original animated texture. No additional rendering library is required: the owning `biomass.client` module uses a [NeoForge custom model loader](https://docs.neoforged.net/docs/1.21.1/resources/client/models/modelloaders/) to bake bounded, pure surface geometry. Native composite models keep opaque biomass out of the translucent skin's sorting pass. Obsolete cube/frame model files were replaced; Git retains them.

## Crafting and discovery

Obtain a Spore Culture to discover the reservoir, extractor, lattice, and core recipes, including through the Creative inventory path. New entries in **The Waking Genome** explain tissue specialization and both structures. The lab supplies the blocks and materials directly for testing.

## Boundaries and future design

The graphical gate waits until no loading screen is open and captures after a rendered frame. Reservoir captures additionally assert the client-visible upper-layer fill, presence of expansion blocks, all 1088 state models, the block-shaped inventory model, and separation of opaque/translucent render passes. The capture profile prioritizes nearby chunk mesh updates so background terrain compilation cannot leave an old structure image in the result. Pure geometry tests cover layer separation, inward-only rounding, face winding, waterline coverage, shared seams for every supported cuboid size, and explicit cell/catalog/maximum-structure geometry budgets.

`multiblock` owns pure bounded discovery and fixed-pattern validation. `biomass` owns storage, conservation, transfers, work limits, persistence, and interaction. `extraction` depends only on the structure framework and reservoir's public transfer contract. `substrate` owns coordinate selection and equipment-gated walking; the existing block adapter retains growth/interaction entry points. No feature reaches into another feature's inventory or controller data.

Damaging/sampling tissue, connective transport tissue, and self-growing reservoirs remain design proposals. Before implementing them, define friendly-fire and player consent, sampling cooldowns, ownership, resource accounting, cycle handling, and bounded transfer work. Automatic sampling should credit a genome bank or bounded storage, never scatter one item per organism or converted block.
