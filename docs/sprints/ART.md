# Art queue

The art track. Same story template as the sprints, kind `art`. Textures come from an image model (Astra); the owner saves the result to the path the story names. GeckoLib models and animations come from a strong model. Rules: [ENGINEERING.md](../../ENGINEERING.md#art-track).

Fixed for every texture:

- 16x16 PNG. Blocks are opaque and tile on all four edges. Items have a transparent background and a one pixel dark outline.
- Original biological look: cuticle, muscle, membrane, pores, veins. No metal, no arrows, no crates, no vanilla copies.
- Palette for Living Substrate: dark loam `#3b2a1e`, dull flesh `#a8646b`, vein violet `#6b3a6e`, pore shadow `#241a16`, pale thread `#d9c2b2`.
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
  1. spore_culture.png: a small cluster of five to seven pale spore pods on a thin violet thread, centered, transparent background. Prompt: "16x16 pixel art game item icon, cluster of pale fungal spore pods joined by a thin violet thread, dark outline, transparent background, flat shading, no text".
  2. living_substrate_basic.png: dark loam with a few pale threads crossing it; at least 60% of the ground still visible. Prompt: "16x16 seamless pixel art block texture, dark loam soil with three or four thin pale fungal threads crossing it, most soil visible, flat shading, tileable".
  3. living_substrate_young.png: joined violet veins, two or three small dark pores, a few soft pink spots; about 40% soil visible. Prompt: "16x16 seamless pixel art block texture, dark soil mostly covered by joined violet veins and dull pink flesh patches, two small dark pores, tileable".
  4. living_substrate_mature.png: interlocking dull flesh tissue with deeper folds and pores of two sizes; no soil visible. Prompt: "16x16 seamless pixel art block texture, dense interlocking pink-violet living tissue with folds and dark pores of two sizes, no soil, tileable".
  5. Check each block texture tiles: view a 3x3 repeat; no visible seam or repeated bright spot.
Tests: none. Assets only.
Done:
  - ./gradlew verifyCaptures
  - The reviewer compares build/captures/living_substrate/*.png with the Appearance column in LIVING_SUBSTRATE_MUTATIONS.md section 1.
Not: models, animations, item models, any other block.
Bounds: four files, 16x16 each.
Evidence: visual
```
