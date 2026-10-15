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
- Ground textures are a 4x4-block macro pattern, 256x256 source cut into sixteen 64x64 tiles, so a 4x4 patch shows no repeat and no seam. Other blocks get random rotation in the blockstate.
- One light direction, top-left, across every texture. Soft shading, no flat fills, no dithering noise, no outlines inside a block face.
- An item icon is one object with a silhouette that reads at hotbar size, a thin dark outline, and a highlight on the lit side.
- A model has one bone per moving part. At rest no cube of one bone sits inside a cube of another bone, except plates named as layered. Every loop closes: the last key equals the first pose, and a rotating channel ends at a multiple of 360. No channel moves faster than 720 degrees per second. Emissive parts have a glowmask. Model textures are 64x64 per model.
- Model sources are `.bbmodel` files committed under `art/`, openable in Blockbench; the shipped `geo` and `animation` JSON are exported from them. The first model story also adds the lint that measures the four rules above.
- Evidence for a texture: `build/art/<name>-tiled.png` and the captures. For a model: front, side, back, dark and an animation frame sheet.

---

```text
ART-001  Textures for Spore Culture and the three Living Substrate stages
Status: ready
Milestone: M1
Owner: ecology
Kind: art
Depends: IF-008
Read: docs/design/BLOCK_CATALOG.md#t0-01--living-substrate, docs/design/LIVING_SUBSTRATE_MUTATIONS.md#1-grow-the-foundation
Files: src/main/resources/assets/infestusfrontier/textures/item/spore_culture.png, src/main/resources/assets/infestusfrontier/textures/block/living_substrate_basic_0.png to _15.png, src/main/resources/assets/infestusfrontier/textures/block/living_substrate_young_0.png to _15.png, src/main/resources/assets/infestusfrontier/textures/block/living_substrate_mature_0.png to _15.png, CREDITS.txt
Do:
  1. spore_culture.png, 32x32, `--size 32 --colors 0`. Prompt: "Use case: stylized-concept. Asset type: source for a 32x32 voxel-game item icon. Subject: one small cluster of six round pale-crimson fungal spore pods joined by a thin violet thread, centered, filling about 70 percent of the canvas. Style: hand-painted game sprite, bold readable silhouette, soft shading, thin dark outline. Constraints: square, true alpha transparency, one isolated object, no background, no border, no text, no watermark. Avoid: photorealism, scenery, loose particles, glow, lens flare."
  2. living_substrate_basic: one 256x256 source, `python3 scripts/texture.py <source> src/main/resources/assets/infestusfrontier/textures/block/living_substrate_basic.png --size 256 --tiles 4` writes the sixteen tiles. Prompt: "Use case: stylized-concept. Original seamless tileable flat top-down game terrain material, newly seeded alien biological ground: matte dark plum soil with a few sparse thin rusty crimson branching threads and two or three small pale pink membrane spots; most of the soil stays visible. Broad readable forms, low visual noise, hand-painted voxel fantasy material, no grid, no border, no bevel, no isolated central emblem. Threads continue across opposite edges. Square opaque orthographic texture swatch fills the entire image. No perspective, text or symbols. Designed as a large terrain pattern spanning four Minecraft blocks, 64 pixels per block."
  3. living_substrate_young: same export. Prompt: "Use case: stylized-concept. Original seamless tileable flat top-down game terrain material, growing alien biological ground: joined broad burgundy and coral vascular veins over dark plum soil, three or four small dark pores, a few scattered soft pink growths; about half of the soil still visible. Broad readable forms, low visual noise, hand-painted voxel fantasy material, no grid, no border, no bevel, no isolated central emblem. Veins continue across opposite edges. Square opaque orthographic texture swatch fills the entire image. No perspective, text or symbols. Designed as a large terrain pattern spanning four Minecraft blocks, 64 pixels per block."
  4. living_substrate_mature: same export. Prompt: "Use case: stylized-concept. Original seamless tileable flat top-down game terrain material, mature alien biological ground: lush layered plum and burgundy membranes with broad branching coral veins, pores of two sizes, a few small luminous golden nutrient nodes and a hint of violet bioluminescence; no soil visible. Broad readable forms, low visual noise, hand-painted voxel fantasy material, no grid, no border, no bevel, no isolated central emblem. Veins continue across opposite edges. Square opaque orthographic texture swatch fills the entire image. No perspective, text or symbols. Designed as a large terrain pattern spanning four Minecraft blocks, 64 pixels per block."
  5. Open each `build/art/<name>-tiled.png`: no seam between tiles or at the pattern edge, no repeated bright spot, and the three stages read as one material getting denser.
Tests: none. Assets only.
Done:
  - ./gradlew verifyCaptures
  - The reviewer compares build/captures/demo/hosts.png and build/captures/living_substrate/*.png with the Appearance column in LIVING_SUBSTRATE_MUTATIONS.md section 1.
Not: models, animations, item models, any other block.
Bounds: 49 files; 48 tiles at 64x64, one icon at 32x32.
Evidence: visual
```
