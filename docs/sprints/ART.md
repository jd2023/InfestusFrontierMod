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
- An organ item's icon sits centred in its slot: its visual centroid within 1.5 px of the slot centre, nothing drawn outside the 16 px square. The gate measures it on the organs hotbar capture; the numbers live in the item model's `display.gui`.
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

## ART-003 — The Waking Genome artwork

```text
ART-003  The guide has a cover of its own and a painted illustration for every Contact entry
Status: accepted f162708 (art and pages); record and model wiring tidied in the following commit
Milestone: M2
Owner: discovery
Kind: art
Depends: IF-019
Read: docs/design/GUIDE_PROGRESSION_TREE.md#3-contact-the-first-working-patch, the organ captures under build/captures/demo/
Files: src/main/resources/assets/infestusfrontier/textures/item/the_waking_genome.png, src/main/resources/assets/infestusfrontier/textures/book/*.png, src/main/resources/assets/infestusfrontier/models/item/the_waking_genome.json, src/main/resources/data/infestusfrontier/modonomicon/books/the_waking_genome/book.json and entries/contact/*.json, src/testMod/resources/captures/demo.json, CREDITS.txt
Do:
  1. Cover icon, 32x32, exported with scripts/texture.py --size 32 --colors 0; book.json names it as the book model. The one line of Java this needs, registering that model on the client, was done by the planner in integration/modonomicon/GuidePages.java.
  2. Nine illustrations, one per Contact entry, generated with the shared prompt plus the entry subject below; export keeps the generated alpha, trims to the content, fits it in 192x104 and centers it on a transparent 200x200 canvas. Modonomicon image pages render a square, so the padding keeps the aspect.
  3. Each entry opens with its illustration and description, then its controls; batch recipes and crafting pages follow.
  4. demo.json photographs every entry's opening spread.
Tests: none. Assets only; the gate's captures are the evidence.
Done:
  - bash scripts/verify.sh
Not: organ models, gameplay, custom page renderers, more categories.
Bounds: 200x200 RGBA per illustration, 32x32 for the cover.
Evidence: visual
```

### Cover prompt

```text
Use case: stylized-concept. Asset type: source for a 32x32 voxel-game hotbar item icon, The Waking Genome guide book. Subject: one closed thick flesh-bound tome seen in three-quarter view from slightly above, spine on the left, angled diagonally. Its cover is supple wine-red and muted rose living membrane, with two broad branching coral veins, a small ivory bone clasp on the right edge, and a clearly visible cream page block. It must immediately read as a BOOK at tiny hotbar size. Style: high-quality hand-painted fantasy game sprite, bold simple silhouette, thin dark plum outer outline, broad clean shading lit from upper left. Palette: burgundy and dusty crimson flesh, deep plum shadows, ivory pages and clasp. Composition: one centered object filling 82 percent of the square canvas with generous transparent margin. True alpha transparency. Constraints: original artwork, no text, no lettering, no symbols, no face, no eyes, no teeth, no background, no floor shadow, no border, no watermark, no particles, no halo, no photorealistic gore. Few large readable details that survive reduction to 32x32.
```

### Shared illustration prompt

Append the entry-specific subject below to this prompt. Reference images supply the existing object shapes and the painted substrate material.

```text
Use case: stylized-concept. Asset type: an original painted field-guide illustration for a biological voxel-game mod, to be exported as a 200x200 transparent PNG with an approximately 200x100 horizontal vignette centered inside it. Composition: SQUARE transparent canvas; ALL artwork forms one WIDE shallow horizontal scene across the middle, about 90% canvas width and 45-50% canvas height. Leave generous true alpha space above and below. Three-quarter view unless specified. Broad readable hand-painted forms, soft upper-left light, rich burgundy and mulberry living flesh, coral raised vessels, ivory structural bone, olive/lime nutrients. Organic but retain the recognizable block-built silhouette of the reference game objects. No labels, no text, no signs, no frame, no arrows, no UI, no watermark, no humans, no photorealistic gore, no magical glow or floating particles. The reference screenshots show object shapes only: remove the signs, horizon and unrelated objects. Surface finish should match the lush, supple painted biological terrain reference, with quiet shapes and selective moist highlights; no noisy grain or black outlines inside surfaces. The scene ends in a clean organic vignette silhouette, no rectangular background.
```

### culture_bowl

```text
Subject: a Culture Bowl MID-BATCH, the low squat square open vessel in the game screenshot. A wide shallow burgundy fleshy bowl on a small island of living substrate, ivory rim, translucent pale olive culture medium with three small bubbles and one red mushroom cap with two tiny wheat-seed shoots immersed within. One small crimson spore cluster emerging at the back of the liquid. The low bowl is the dominant subject. Slightly elevated view clearly reveals the liquid surface and living folds along its sides. Warm alive, quietly industrious.
```

### digestive_sac

```text
Subject: the DIGESTIVE SAC AT WORK, the upright stepped wine-red organ at the right of the reference workshop screenshot. It is a squat upright fleshy digestive vessel with three irregular muscular tiers, an open ivory-lipped feeding mouth on top, a small green side pore and a flared rooted base. Three wheat stalks enter its upper mouth, tissue folds visibly swollen with digestion. A short thick olive biomass bead at its low outlet hints at output, no mess. A small patch of burgundy living substrate roots it to the ground. The single Sac dominates the horizontal vignette; no other machines.
```

### biomass_bladder

```text
Subject: the BIOMASS BLADDER FILLING, the ivory-framed translucent green organ in the center of the workshop reference. A squat near-cubic transparent biological reservoir, wine-red fleshy base and cap, four slim ivory bone corner struts. Its semitransparent membrane reveals an olive yellow-green liquid surface about two-thirds full, small pale suspended flecks, a few bubbles rising. One short fleshy inlet at its lower left delivers a trickle of green biomass from beyond the vignette; no pipes or other organ in view. Rooted on a small irregular burgundy living-substrate patch. Clearly a biological storage organ with a visible fluid level.
```

### living_substrate

```text
Subject: a PLAYER-FED LIVING SUBSTRATE WAVE viewed directly from overhead. A single small irregular island of soil, with a clearly mature dark mulberry and wine-red fleshy core in the middle-left, a younger rose-veined ring around it and a sparse basic edge of crimson tendrils over brown earth and a few small grey stones on the right. Preserve the branching vascular network of the supplied substrate texture. A small modest sprinkling of ivory bone-meal grains on the mature core communicates the feeding that caused this growth. Three stages are connected by continuous veins, not three separate tiles. It must read as one patch of living ground growing outward, not a tumor, rock pile, diagram or disconnected icons. No organ structures, no geometric grid, no arrows. Flat overhead horizontal landscape vignette.
```

### organ_bud

```text
Subject: one ORGAN BUD just awakening on living substrate. Match the small block-like burgundy organ bud at the left of the workshop screenshot: a low squat rounded-square fleshy mound on a flat tissue foot, a raised central rose square cap, and two small recessed olive-green nutrient pores. Give it tender overlapping membrane folds, muted pink new tissue on top and crimson veins joining its base into the substrate. Several tiny pale rootlets at the edge. The single bud is centered in a wide horizontal vignette on its own small patch of living ground. No other organ, no signs, no plants, no eyes, no face.
```

### a_culture_in_your_hand

```text
Subject: the SPORE CULTURE starter, a small cluster of six round pale-crimson and rose spore pods of different sizes joined by a fine violet thread, matching the provided icon. A cutaway-like little clod of earthy soil below the pods shows the first fine red root threads entering it; one red mushroom cap and three tiny wheat seeds rest next to the cluster, showing its biological beginnings. Compose the pods, earth, mushroom and seeds as one intimate horizontal still-life vignette. No literal hand, no human, no recipe grid, no arrows, no detached particles. The pods are clearly the dominant focus and have broad soft highlights.
```

### the_waking_genome

```text
Subject: THE WAKING GENOME, a closed flesh-bound field-guide tome resting at a slight angle on a small island of living burgundy substrate, with a modest six-pod crimson spore culture growing beside its lower corner. Match the supplied book cover: wine-red living membrane, branching coral veins, one ivory bone clasp and cream page block. The book lies low, face visible in three-quarter view, and the roots and spore cluster extend sideways to make one horizontal vignette. The book and tiny starter culture evoke the beginning of a colony, no other props. No letters anywhere on the cover.
```

### speak_to_an_organ

```text
Subject: a SYNAPTIC PROBE touching a single olive-green sensory pore on the flank of a small burgundy living organ. The tool is a slim pale ivory bone shaft, lying diagonally and mostly horizontally across the vignette, with a translucent violet nerve bulb at its tip and three fine crimson filaments making contact with the pore. One small white specular highlight on the bulb, no emitted light. Show only a modest rounded-square fleshy organ flank on the right and its short rooted base; the long probe extends to the left and is the focal point. This is a gentle sensory contact, no stabbing, no damage, no hand. Match the supplied probe icon and painted membrane texture.
```

### biomass_bucket

```text
Subject: one standard PLAIN GREY METAL BUCKET filled with thick olive yellow-green biomass, with a thin grey handle arcing behind it, seen from slightly above so the bulging fluid surface and two tiny pale specks are visible. It rests beside a low shallow ivory-lipped living receptacle on a small horizontal patch of crimson substrate, ready to transfer biomass by hand. The bucket is the dominant subject, with familiar simple flared vanilla-family metal bucket silhouette, steel grey bands and a clear dark rim; the small organic receptacle beside it provides context. No second bucket, no large machine, no drips, no pouring, no hand, no arrows. Soft clean upper-left light, painted steel highlights and rich olive fluid.
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
