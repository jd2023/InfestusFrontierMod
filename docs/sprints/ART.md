# Art queue

The art track. Same story template as the sprints, kind `art`. Textures come from an image model (Astra); the owner saves the result to the path the story names. GeckoLib models and animations come from a strong model. Rules: [ENGINEERING.md](../../ENGINEERING.md#art-track).

Fixed for every texture:

- The image model draws the source at its own size. `python3 scripts/texture.py <source.png> <shipped.png> --size 64` downsamples it, caps the palette and writes `build/art/<name>-tiled.png`, a 3x3 repeat to check for seams and repeated bright spots. Only the shipped file is committed.
- Blocks are 64x64, opaque and seamless on all four edges: the prompt says "veins continue across opposite edges". Items are 32x32 on a transparent background, one centered object.
- Original biological look: membrane, muscle, veins, pores, nodules. Broad readable forms, low noise. No grid, no border, no bevel, no central emblem, no text, no copy of an existing game texture.
- Prompts follow the shape used in ART-001: use case, asset type, subject, style, composition, palette, constraints, avoid. 16x16 pixel-art prompts with a five color palette gave the weak placeholders; sources drawn large and downsampled are what read as living material.
- One `CREDITS.txt` line per story: file, who made it, tool, date.

---

```text
ART-001  Textures for Spore Culture and the three Living Substrate stages
Status: ready
Milestone: M1
Owner: ecology
Kind: art
Depends: IF-001
Read: docs/design/BLOCK_CATALOG.md#t0-01--living-substrate, docs/design/LIVING_SUBSTRATE_MUTATIONS.md#1-grow-the-foundation
Files: src/main/resources/assets/infestusfrontier/textures/item/spore_culture.png, src/main/resources/assets/infestusfrontier/textures/block/living_substrate_basic.png, src/main/resources/assets/infestusfrontier/textures/block/living_substrate_young.png, src/main/resources/assets/infestusfrontier/textures/block/living_substrate_mature.png, CREDITS.txt
Do:
  1. spore_culture.png, 32x32, `--size 32 --colors 0`. Prompt: "Use case: stylized-concept. Asset type: source for a 32x32 voxel-game item icon. Subject: one small cluster of six round pale-crimson fungal spore pods joined by a thin violet thread, centered, filling about 70 percent of the canvas. Style: hand-painted game sprite, bold readable silhouette, soft shading, thin dark outline. Constraints: square, true alpha transparency, one isolated object, no background, no border, no text, no watermark. Avoid: photorealism, scenery, loose particles, glow, lens flare."
  2. living_substrate_basic.png, 64x64. Prompt: "Use case: stylized-concept. Original seamless tileable flat top-down game terrain material, newly seeded alien biological ground: matte dark plum soil with a few sparse thin rusty crimson branching threads and two or three small pale pink membrane spots; most of the soil stays visible. Broad readable forms, low visual noise, hand-painted voxel fantasy material, no grid, no border, no bevel, no isolated central emblem. Threads continue across opposite edges. Square opaque orthographic texture swatch fills the entire image. No perspective, text or symbols. Designed for downsampling to 64x64."
  3. living_substrate_young.png, 64x64. Prompt: "Use case: stylized-concept. Original seamless tileable flat top-down game terrain material, growing alien biological ground: joined broad burgundy and coral vascular veins over dark plum soil, three or four small dark pores, a few scattered soft pink growths; about half of the soil still visible. Broad readable forms, low visual noise, hand-painted voxel fantasy material, no grid, no border, no bevel, no isolated central emblem. Veins continue across opposite edges. Square opaque orthographic texture swatch fills the entire image. No perspective, text or symbols. Designed for downsampling to 64x64."
  4. living_substrate_mature.png, 64x64. Prompt: "Use case: stylized-concept. Original seamless tileable flat top-down game terrain material, mature alien biological ground: lush layered plum and burgundy membranes with broad branching coral veins, pores of two sizes, a few small luminous golden nutrient nodes and a hint of violet bioluminescence; no soil visible. Broad readable forms, low visual noise, hand-painted voxel fantasy material, no grid, no border, no bevel, no isolated central emblem. Veins continue across opposite edges. Square opaque orthographic texture swatch fills the entire image. No perspective, text or symbols. Designed for downsampling to 64x64."
  5. Open each `build/art/<name>-tiled.png`: no seam, no repeated bright spot, and the three stages read as one material getting denser.
Tests: none. Assets only.
Done:
  - ./gradlew verifyCaptures
  - The reviewer compares build/captures/demo/hosts.png and build/captures/living_substrate/*.png with the Appearance column in LIVING_SUBSTRATE_MUTATIONS.md section 1.
Not: models, animations, item models, any other block.
Bounds: four files; three at 64x64, one at 32x32.
Evidence: visual
```
