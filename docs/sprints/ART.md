# Art queue

The art track. Same story template as the sprints, kind `art`. Textures come from an image model (Astra); the owner saves the result to the path the story names. GeckoLib models and animations come from a strong model. Rules: [ENGINEERING.md](../../ENGINEERING.md#art-track).

Fixed for every texture:

- The image model draws the source at its own size. `python3 scripts/texture.py <source.png> <shipped.png> --size 64` downsamples it, caps the palette and writes `build/art/<name>-tiled.png`, a 3x3 repeat to check for seams and repeated bright spots. Only the shipped file is committed.
- Blocks are 64x64, opaque and seamless on all four edges: the prompt says "veins continue across opposite edges". Items are 32x32 on a transparent background, one centered object.
- Original biological look: membrane, muscle, veins, pores, nodules. Broad readable forms, low noise. No grid, no border, no bevel, no central emblem, no text, no copy of an existing game texture.
- Prompts follow the shape used in ART-001: use case, asset type, subject, style, composition, palette, constraints, avoid. 16x16 pixel-art prompts with a five color palette gave the weak placeholders; sources drawn large and downsampled are what read as living material.
- One `CREDITS.txt` line per story: file, who made it, tool, date.

Quality bar. The owner rejects anything below it, and a sprint is not done while its art story is open.

- A texture reads as its material from four blocks away in a capture, without a sign: flesh, membrane, bone, soil. The bar is the V3 substrate and the Astra living tissue from the model evaluation; weaker than those is rejected.
- Ground textures are a 4x4-block macro pattern, 256x256 source cut into sixteen 64x64 tiles, so a 4x4 patch shows no repeat and no seam. The stages of one ground share one vein network and one ground tone, derived from one master by a generator under `art/`. Other blocks get random rotation in the blockstate.
- One light direction, top-left, across every texture. Soft shading, no flat fills, no dithering noise, no outlines inside a block face.
- An item icon is one object with a silhouette that reads at hotbar size, a thin dark outline, and a highlight on the lit side.
- A model has one bone per moving part. At rest no cube of one bone sits inside a cube of another bone, except plates named as layered. Every loop closes: the last key equals the first pose, and a rotating channel ends at a multiple of 360. No channel moves faster than 720 degrees per second. Emissive parts have a glowmask. Model textures are 64x64 per model.
- Model sources are `.bbmodel` files committed under `art/`, openable in Blockbench; the shipped `geo` and `animation` JSON are exported from them. The first model story also adds the lint that measures the four rules above.
- Evidence for a texture: `build/art/<name>-tiled.png` and the captures. For a model: front, side, back, dark and an animation frame sheet.

---

```text
ART-001  Textures for Spore Culture and the three Living Substrate stages
Status: accepted 596db2a (tiles, bucket and probe icons); step 1 moved to ART-002; young and basic re-derived from the mature master by art/gen_substrate_stages.py on 2026-10-17 so the stages share one vein network
Milestone: M1
Owner: ecology
Kind: art
Depends: IF-008
Read: docs/design/BLOCK_CATALOG.md#t0-01--living-substrate, docs/design/LIVING_SUBSTRATE_MUTATIONS.md#1-grow-the-foundation
Files: src/main/resources/assets/infestusfrontier/textures/item/spore_culture.png, src/main/resources/assets/infestusfrontier/textures/block/living_substrate_basic_0.png to _15.png, src/main/resources/assets/infestusfrontier/textures/block/living_substrate_young_0.png to _15.png, src/main/resources/assets/infestusfrontier/textures/block/living_substrate_mature_0.png to _15.png, CREDITS.txt
Do:
  1. spore_culture.png, 32x32, `--size 32 --colors 0`. Prompt: "Use case: stylized-concept. Asset type: source for a 32x32 voxel-game item icon. Subject: one small cluster of six round pale-crimson fungal spore pods joined by a thin violet thread, centered, filling about 70 percent of the canvas. Style: hand-painted game sprite, bold readable silhouette, soft shading, thin dark outline. Constraints: square, true alpha transparency, one isolated object, no background, no border, no text, no watermark. Avoid: photorealism, scenery, loose particles, glow, lens flare."
  2. living_substrate_basic: one 256x256 source, `python3 scripts/texture.py <source> src/main/resources/assets/infestusfrontier/textures/block/living_substrate_basic.png --size 256 --tiles 4` writes the sixteen tiles. Prompt: basic under Substrate source prompts below.
  3. living_substrate_young: same export. Prompt: young under Substrate source prompts below.
  4. living_substrate_mature: same export. Prompt: mature under Substrate source prompts below.
  5. Open each `build/art/<name>-tiled.png`: no seam between tiles or at the pattern edge, no repeated bright spot, and the three stages read as one material getting denser.
Tests: none. Assets only.
Done:
  - ./gradlew verifyCaptures
  - The reviewer compares build/captures/demo/hosts.png and build/captures/living_substrate/*.png with the Appearance column in LIVING_SUBSTRATE_MUTATIONS.md section 1.
Not: models, animations, item models, any other block.
Bounds: 49 files; 48 tiles at 64x64, one icon at 32x32.
Evidence: visual
```

```text
ART-002  Spore Culture icon
Status: accepted 2c8b169
Milestone: M1
Owner: ecology
Kind: art
Depends: none
Read: docs/design/ITEM_CATALOG.md#2-t0--starter-items-reagents-and-hand-equipment
Files: src/main/resources/assets/infestusfrontier/textures/item/spore_culture.png, CREDITS.txt
Do:
  1. Generate the source with built-in image generation, true alpha transparency, at its own size. Prompt: "Use case: stylized-concept. Asset type: source for a 32x32 voxel-game item icon. Subject: one small cluster of six round pale-crimson fungal spore pods joined by a thin violet thread, centered, filling about 70 percent of the canvas. Style: hand-painted game sprite, bold readable silhouette, soft shading lit from upper left, thin dark outline. Palette: pale crimson and rose pods, plum shadows, one violet thread, one small ivory highlight per pod. Constraints: square, true alpha transparency, one isolated object, no background, no border, no text, no watermark. Avoid: photorealism, scenery, loose particles, glow, lens flare." It must sit beside the shipped Bucket of Biomass and Synaptic Probe icons as one family: same outline weight, same light from the upper left.
  2. Save the source outside src/, then run `python3 scripts/texture.py <source.png> src/main/resources/assets/infestusfrontier/textures/item/spore_culture.png --size 32 --colors 0`.
  3. Open `build/art/spore_culture-tiled.png`: the silhouette reads at hotbar size, the background is transparent, no stray pixels.
  4. Replace the Spore Culture line in CREDITS.txt with: file, built-in image generation, date, CC BY 4.0, the export command.
Tests: none. Assets only.
Done:
  - ./gradlew verifyCaptures
  - The reviewer looks at build/captures/demo/start.png (hotbar, first slot) and build/captures/demo/inventory.png.
Not: any other texture, model or Java.
Bounds: 1 icon at 32x32; one CREDITS line.
Evidence: visual
```

## Substrate source prompts

Built-in image generation, 2026-10-15. Generate mature first using the V3 mature substrate and Astra evaluation tissue as references. Derive young from mature and basic from young. Export with `scripts/texture.py --size 256 --tiles 4`. The unnumbered 64x64 textures support the existing models until IF-008 connects the macro tiles. Spore Culture remains pending.

### mature

```text
Use case: stylized-concept.
Asset type: original production terrain albedo texture for Living Substrate, mature stage, in a biological Minecraft mod. One square opaque seamless macro material spanning 4 by 4 blocks, to be reduced to 256x256 and sliced into sixteen 64x64 tiles.
References: image 1 is the earlier V3 mature material; preserve the beautiful plum/crimson biological identity but improve its variety and clarity. Image 2 is an unrelated pale tissue material: borrow only its broad readable shading and material depth, NOT its pale overall palette or repeating pebble cells.
Subject: a living colony carpet, fully covered in supple interlocking dark mulberry and wine-red membranes. Asymmetric flowing tissue folds of varied scale, a few plump coral vascular trunks branching into thinner red capillaries, small puckered pores, subtly translucent mauve membrane stretches and scattered tiny ochre nutrient nodules sunk into the tissue. Three scales of detail: wide quiet membrane fields, branching medium folds/vessels, sparse little pores. Rich dimensional painted shading, subtle moist highlights on raised vessel rims, never plastic.
Composition: flat orthographic top-down edge-to-edge material. Uniform density and brightness across entire image; organic irregular flow, no center of attention. Truly seamless opposite edges, veins continue across opposite edges. NO mirrored quadrants, no repeating cell template, no central medallion, no checkerboard or tile boundaries. At most a few tiny low-intensity warm nutrient accents; no bright repeated beacon. Tissue must read as soft living flesh, not rock, bark, cracks in dried mud, braided rope, noodles, scales or cobblestones.
Style: excellent hand-painted fantasy game terrain, broad readable forms which survive downsampling. Shading gently lit from upper left. No pixel grid baked into source, no grain or speckle noise, no photographic gore.
Palette: deep desaturated plum in recesses, burgundy membranes, muted rose/coral raised vessels, small dusty mauve highlights, sparse subdued golden nutrients. Enough midtone separation to remain readable on a large floor, no black pits and no white highlights. Original artwork, no text, no border, no watermark. Square texture ONLY, no mockup or scene.
```

### young

```text
Use case: precise-object-edit / stylized-concept.
Asset type: original seamless Living Substrate YOUNG-stage terrain material, square opaque orthographic game texture. This is a growth-stage variant of the attached mature texture. Keep the image dimensions and EXACT main vascular branch paths and their edge crossing positions. Keep the same scale, muted burgundy/plum/coral palette and upper-left painted lighting.
Change the mature living carpet into a young colony: retract most of the large fleshy lobes so roughly half the surface reveals cool dark plum earthy ground. Reduce the main coral vessels to thinner, less swollen joined burgundy-red veins, retaining their branching routes. Keep a few small soft mauve membrane growths along vessel junctions, occasional tiny round pores, and very few subdued nutrient buds (no large golden clusters). Some tissue islands should be thin translucent membranes spreading over the ground. Vary their size and coverage, no regular cobblestones.
Ground is matte charcoal-plum soil with broad soft tonal patches and a few compact earthy granules; NOT a densely cracked mud network. Biological tissue is subtly moist and warmer than soil.
Designed to downsample to a 256x256 macro pattern spanning four Minecraft blocks each direction. Broad readable forms, clear distinction from mature stage, low visual noise. Opposite edges seamlessly repeat; veins continue across opposite edges. No seam, no border, no central emblem, no perspective, no text, no glow, no pixel grid. Preserve the structural correspondence to the attached mature stage.
```

### basic

```text
Use case: precise-object-edit / stylized-concept.
Asset type: original seamless Living Substrate BASIC-stage terrain material, square opaque orthographic game texture. Edit the attached young-stage texture into the newly seeded stage of the SAME colony.
Keep the image dimensions, overall palette, upper-left painted lighting and exact branch ROUTES of the major vessels, but shrink these vessels into very sparse fine rusty-crimson threads. Most branches should be threadlike, dim and partly buried, with occasional short exposed soft rose segments. Reveal matte cool charcoal-plum soil across 85 to 90 percent of the surface. Only two or three small pale dusty-pink translucent membrane growths at irregular junctions, no large tissue islands, no pores, no gold nodes. Soil has broad low-contrast earthy shapes and a restrained fine loam texture. It must read as fertile ground beginning to become alive; not cracked stone, lava, red cobwebs or a pink carpet. No dense miniature vein network. Keep a few empty quiet patches.
This is the sparse first stage, visually much less developed than the attached young stage. Designed to downsample to a 256x256 macro pattern spanning four Minecraft blocks each direction. Readable hand-painted organic game material, low visual noise. Opposite edges seamlessly repeat; thin threads continue across opposite edges. No seam, no border, no central emblem, no perspective, no text, no glow, no pixel grid.
```


## Item source prompts

Built-in image generation with true alpha transparency. Export with `python3 scripts/texture.py <source.png> src/main/resources/assets/infestusfrontier/textures/item/<name>.png --size 32 --colors 0`.

### synaptic_probe

```text
Use case: stylized-concept. Asset type: source for a 32x32 voxel-game item icon. Subject: one hand tool, a pale ivory bone shaft held diagonally from lower left to upper right, tipped with a translucent violet nerve bulb that trails three fine crimson filaments. Style: hand-painted game sprite, bold silhouette, soft shading lit from upper left, thin dark outline. Palette: ivory bone, plum and violet bulb, crimson filaments, one small white highlight on the bulb. Constraints: square, true alpha transparency, one isolated object about 80 percent of the canvas, no background, no border, no text. Avoid: glow halo, metal, syringes, photorealism.
```
