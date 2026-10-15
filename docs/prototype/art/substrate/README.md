# Substrate and biomass source art

Generated with the built-in image-generation tool. The committed sources are mechanically downsampled, palette-reduced exports of the generated originals. `java tools/ExportBiologicalAssets.java` mirrors the source to make periodic macro textures, exports 64×64 tiles, blends stage edges into a common seeded boundary, and emits block models. Liquid frames are small periodic translations of the generated liquid source, with interpolation in Minecraft. This exporter is the reproducible source of the runtime assets.

Source files: `seeded-source.png`, `veined-source.png`, `mature-source.png`, `biomass-source.png` in this directory. Runtime files: `src/main/resources/assets/infestusfrontier/textures/block/substrate_*.png`, `living_substrate.png`, and `liquid_biomass.png`.

## Final prompts

### Seeded

Use case: stylized-concept. Original seamless tileable flat top-down game terrain material, newly seeded alien biological ground: matte dark plum soil with broad sparse rusty crimson branching root veins and a few muted pink membrane islands. Broad readable forms, low visual noise, hand-painted voxel fantasy material, no grid, no border, no bevel, no isolated central emblem. Veins continue across opposite edges. Square opaque orthographic texture swatch fills entire image. No perspective, text, symbols, copyright characters. Designed as a large terrain pattern spanning four Minecraft blocks, 64 pixels per block.

### Veined

Use case: stylized-concept. Original seamless tileable flat top-down game terrain material, growing alien biological ground: interwoven broad burgundy and coral vascular roots linking asymmetrical dark plum membrane plates, a few soft amber nutrient nodules. Bold flowing readable forms, low visual noise, hand-painted voxel fantasy material, no grid, no border, no bevel, no isolated central emblem. Veins continue across opposite edges. Square opaque orthographic texture swatch fills entire image. No perspective, text, symbols, copyright characters. Designed as a large terrain pattern spanning four Minecraft blocks, 64 pixels per block.

### Mature

Use case: stylized-concept. Original seamless tileable flat top-down game terrain material, mature alien biological ground: lush layered plum and burgundy organic membranes with broad branching warm coral veins and small luminous golden nutrient nodes, subtle violet bioluminescence. Bold flowing readable forms, low visual noise, hand-painted voxel fantasy material, no grid, no border, no bevel, no isolated central emblem. Veins continue across opposite edges. Square opaque orthographic texture swatch fills entire image. No perspective, text, symbols, copyright characters. Designed as a large terrain pattern spanning four Minecraft blocks, 64 pixels per block.

### Liquid biomass

Use case: stylized-concept. Asset type: original opaque seamless game texture for a pool of liquid biomass in a biological storage tank. Flat orthographic top-down square swatch filling entire canvas. Thick glowing lime-green and amber nutrient liquid with two or three broad gentle ripples, subtle tiny suspended golden nutrients, smooth readable large shapes, restrained detail. Opposite edges seamlessly repeat. Hand-painted fantasy game material, soft luminous color, no border, no frame, no grid, no central emblem, no perspective, no text, no recognizable existing game texture. Must read as liquid rather than scales, stones, cells, or solid flesh. Designed for downsampling to 64x64.
