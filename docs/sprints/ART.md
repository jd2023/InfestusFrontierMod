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
- A model has one bone per moving part. At rest no cube of one bone sits inside a cube of another bone, except plates named as layered. Every loop closes: the last key equals the first pose, and a rotating channel ends at a multiple of 360. No channel moves faster than 720 degrees per second. Emissive parts have a glowmask. No two parallel faces of different cubes overlap closer than a tenth of a pixel, facing either way, in any pose (`art/geo_audit.py`; organ models draw both sides of every face). Model textures are 64x64 per model; living armor growth sheets are 128x64 (both sets' growths share one texel per unit, as the vanilla armor layers do).
- Model sources are `.bbmodel` files committed under `art/`, openable in Blockbench; the shipped `geo` and `animation` JSON are exported from them. The first model story also adds the lint that measures the four rules above.
- Evidence for a texture: `build/art/<name>-tiled.png` and the captures. For a model: front, side, back, dark and an animation frame sheet.

### Model and animation verification

The [capture generator](../../art/gen_motion_captures.py) covers every shipped
GeckoLib clip, including each furnace stage, bladder fill and junction port mode.
Each organ scene shows idle and working side by side, followed by back and side
views. The Seed Pouch and storage scenes show their available resting states.
Timed frames and their capture timestamps are kept in `build/captures/`.
[Contact sheets](../../art/motion_contact_sheets.py) summarize them under
`build/art/motion/`; inspect the source frames for fine detail.

| Models | Visual checks |
|---|---|
| Culture Bowl | Pool stays inside its walls; bubbles form independently at the surface |
| Digestive Sac | Reads as a stomach: the acid window is the cue, the body stays quiet; the gullet stays seated while the sac squeezes; mouth and glands stay attached |
| Biomass Bladder | Empty is empty; all fill levels stay inside the membrane; frame has no overlapping faces |
| Membrane Rack | Skin stays between posts; the droplet resets while invisible |
| Bone Loom | Ribs clear their contents through the working cycle |
| Awakening Cradle | Reads as something being brought to life: the glowing cocoon is the cue; closed petals clear the cocoon and its pulse, open ones stay inside the block |
| Bio-Furnace | All three stages; reads as a furnace from across a room, day and night: dark body, hot mouth and seams, fire and light only while it works; the fangs, coals, chimney, buttresses, bellows and second chimney remain distinct |
| Activation Cyst, Seed Pouch | Chambers, neck, rim and throat follow their supporting tissue |
| Planting Proboscis, Harvest Corolla | Moving parts remain attached; petals clear the heart; tips clear the floor |
| Compost Gland | Distinct scrap hopper and fertilizer outlet, with a slow compression cycle |
| Vascular Junction | All six faces in all three modes; bottom port stays on the foundation |
| Reservoir | Joined bodies, exterior corners, fill surfaces and inventory models |
| Capsule Stem, Item Capsule | `capsule_stem`: a grown tree with a fork, pods with stock on their faces, three grades, the root's screen; `motion_item_capsule`: the pod breathes |
| Bud, veins, Mouth, lobes, cultivation, Polyp, Valve, Papilla | Existing static scenes plus `visual_static`: front/back views, recessed surfaces and connections |
| Dormant and awakened armor | `visual_armor`: front/back/side views with bent arms and legs; dyed, trimmed and enchanted sets; an adult and a baby zombie wearing them |
| Living walls, windows, crossings and stitches | `walls`, `openings`, `crossings`: all tiers, joined frames and routed faces |
| Sphincter Door | `motion_sphincter_door`: closed pulse and redstone opening/closing from both sides; sealed flaps and distinct edge planes |

[Asset regression checks](../../art/test_motion.py) run in the engineering gate.
They cover resource references, UV bounds, bone references, loop closure, rotation
speed, overlapping surface planes and the motion constraints above. In-game visual
review remains required; those checks cannot judge appearance by themselves.

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

## ART-004 — Membrane Rack and Membrane Sheet artwork

```text
ART-004  The Sheet and the Plate have their icons, the Rack and the Loom their guide illustrations
Status: accepted ca4840d
Milestone: M2
Owner: processing
Kind: art
Depends: the Rack commit on v4
Read: docs/design/BLOCK_CATALOG.md#t0-05--membrane-rack and #t0-06--bone-loom, docs/design/ITEM_CATALOG.md (I002, I003), build/captures/demo/bowl.png and loom.png (the Rack stands at x=28, the Loom at x=30)
Files: src/main/resources/assets/infestusfrontier/textures/item/membrane_sheet.png and bone_plate.png, src/main/resources/assets/infestusfrontier/textures/book/membrane_rack.png and bone_loom.png, the_waking_genome/entries/contact/membrane_rack.json and bone_loom.json, textures/book/*.png and entries/contact/the_waking_genome.json for step 4, CREDITS.txt
Do:
  1. membrane_sheet.png, 32x32: one pale pinkish stretched skin sheet, slightly curled at one corner, thin dark outline, lit top-left. Replaces the planner's placeholder; export with scripts/texture.py --size 32 --colors 0.
  2. bone_plate.png, 32x32: one curved ivory plate with a faint rib line, thin dark outline, lit top-left. Same export.
  3. membrane_rack.png and bone_loom.png, 200x200 canvases with a 200x100 vignette like the other Contact illustrations: the bone rack with a skin stretched between its posts, a drop falling from it, on a small island of substrate. Then the entry's first page becomes a modonomicon:image page like the others.
  4. While there: the ART-003 follow-ups, taller vignettes so picture and caption sit closer, and the root entry's icon as the new cover.
Tests: none. Assets only; the gate's captures are the evidence.
Done:
  - bash scripts/verify.sh
Not: the Rack and Loom models (generated), other icons.
Bounds: 32x32 for the icon, 200x200 RGBA for the illustration.
Evidence: visual
```

## ART-005 — Armor pieces and the Cradle artwork

```text
ART-005  The eight armor pieces have icons and worn textures, the Cradle its guide illustration
Status: accepted ca4840d; armor superseded 2026-10-24 by S04 F15 (art/gen_armor.py), the Cradle illustration stands
Milestone: M2
Owner: equipment
Kind: art
Depends: the Cradle commit on v4
Read: docs/design/ARMOR_EVOLUTION.md#4-fusion-tree (Dormant flesh, Living frame), docs/design/ITEM_CATALOG.md (I013, I014), docs/design/BLOCK_CATALOG.md#t0-08--awakening-cradle
Files: src/main/resources/assets/infestusfrontier/textures/item/dormant_*.png and awakened_*.png (8), src/main/resources/assets/infestusfrontier/textures/models/armor/dormant_layer_1.png, dormant_layer_2.png, awakened_layer_1.png, awakened_layer_2.png, src/main/resources/assets/infestusfrontier/textures/book/awakening_cradle.png, the_waking_genome/entries/contact/awakening_cradle.json, CREDITS.txt
Do:
  1. Eight 32x32 icons in the vanilla armor silhouettes: dormant pieces are dull rotten-flesh red, awakened pieces the mod's wine-red living membrane with one small olive pore. Export with scripts/texture.py --size 32 --colors 0.
  2. Worn armor layers, 64x32 each, in the vanilla humanoid armor layout: layer_1 holds hood, thorax and treads, layer_2 the legwraps. Dormant: slack flesh with stitches; awakened: living membrane with faint veins and one olive pore on the chest.
  3. awakening_cradle.png, 200x200 canvas with a 200x100 vignette like the other Contact illustrations: the flesh cup with its two lobes folding around a dormant thorax, the pale core glowing. Then the entry's first page becomes a modonomicon:image page.
Tests: none. Assets only; the gate's captures are the evidence, plus one screenshot of a worn set from runClient.
Done:
  - bash scripts/verify.sh
Not: the Cradle model (generated), mutation or fusion looks.
Bounds: 32x32 icons, 64x32 layers, 200x200 RGBA for the illustration.
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

## ART-006 — Bio-Furnace illustration

Status: accepted ca4840d. Owner of the asset: Astra.
Files: src/main/resources/assets/infestusfrontier/textures/book/bio_furnace.png, the_waking_genome/entries/contact/bio_furnace.json, CREDITS.txt

- bio_furnace.png, a 200x200 canvas with a 200x100 vignette like the other Contact illustrations: a plum stomach-shaped organ on a flesh foot, a dark mouth on top, an orange ember band glowing across its middle, a faint shimmer of heat above it, on a small island of substrate. Then the entry's first page becomes a modonomicon:image page like the others.
- Quality bar: ART.md Quality bar. The organ model, its stage looks (ridges, lobes, chimneys) and the screen emblem come from art/gen_bio_furnace.py and art/gen_gui_sheet.py and are not part of this story.

## ART-007 — Treatment icons

Status: accepted ca4840d. Owner of the asset: Astra.
Files: src/main/resources/assets/infestusfrontier/textures/item/{fusion_binder,elastic_gel,rooting_gel,capillary_gel,contractile_fiber,filter_membrane,sealing_resin,synaptic_gel}.png, CREDITS.txt

- Eight 32x32 icons, `--size 32 --colors 0`, one family with the Spore Culture icon: same outline weight, light from the upper left. Fusion Binder: a pale amber dollop in a thin membrane skin. Elastic Gel: a wobbling green-grey bead. Rooting Gel: an olive drop with a tiny root thread hanging from it. Capillary Gel: a bright green bead with fine red capillaries. Contractile Fiber: a coiled pink-red muscle strand. Filter Membrane: a square of sandy membrane with pores. Sealing Resin: a dark amber tear with a sheen. Synaptic Gel: a red bead with a violet spark inside.
- Prompt shape as ART-002; one object, true alpha, no background. The placeholders are flat two-tone discs drawn by the builder.

## ART-008 — Storage tissue: Reservoir and Item Capsule textures

Status: accepted e7870a3. Owner of the asset: Astra. Since S04 F20 (2026-10-25) the Item Capsule paints its own tiles and uses none of these.
Files: src/main/resources/assets/infestusfrontier/textures/block/{reservoir_membrane,reservoir_bone,reservoir_flesh,item_capsule_lining}.png, CREDITS.txt

- Four 64x64 block textures, seamless on all four edges, one family with the Living Substrate tiles. They replace procedural placeholders from art/gen_reservoir.py and art/gen_item_capsule.py; the part models, the blockstates and the liquid stay with those generators, so a repaint changes no model.
- reservoir_membrane: a thin translucent pink membrane (alpha about 90 to 130), a few broad crimson veins and fine capillaries between them, veins continuing across opposite edges and nearly opaque. It is the side panes and the lid of the Reservoir; the biomass behind it must show through. The blockstate turns it four ways at random, so no vein may end at an edge.
- reservoir_bone: smooth ivory bone, broad soft swells, faint growth lines running one way, a few small pores. Opaque. The Reservoir's ribs and knuckles, the Item Capsule's ribs and the frame around its four pockets are cut from it at 1 to 2 pixels wide, so it must read as bone even in a narrow strip.
- reservoir_flesh: lobed pink-red muscle, broad swells parted by narrow dark creases, a fine wrinkle and a wet sheen on the crowns. Opaque. The Reservoir's base and rims and every wall of the Item Capsule; a 4x2 capsule wall shows it 8 times side by side.
- item_capsule_lining: dark wet flesh, almost maroon-black, soft vertical folds with a faint sheen. Opaque. The floor of the capsule's pockets, behind the item icons and their white counts, so it stays dark and quiet.
- Prompt shape as ART-001; evidence `build/art/<name>-tiled.png` and the captures reservoir and item_capsule.

## ART-009 — Dead Tissue and Surgical Blade icons

Status: open. Owner of the asset: Astra.
Files: src/main/resources/assets/infestusfrontier/textures/item/{dead_tissue,surgical_blade}.png, CREDITS.txt

- Two 32x32 icons, `--size 32 --colors 0`, one family with the Spore Culture and treatment icons: same outline weight, light from the upper left. Dead Tissue: a limp grey-brown scrap of flesh, dry and wrinkled, a dull pink fleck where it tore. Surgical Blade: a short pale bone handle bound with a strip of pink membrane, a thin curved steel blade, held diagonally from lower left to upper right like the probe; the item renders as a handheld tool.
- Prompt shape as the synaptic_probe source prompt; one object, true alpha, no background. The placeholders are flat two-tone discs drawn by the builder.

## ART-010 — Living wall tile and graft icons

Status: open. Owner of the asset: Astra.
Files: src/main/resources/assets/infestusfrontier/textures/block/living_wall.png, textures/item/{skeletal_graft,calcifying_graft}.png, CREDITS.txt

- living_wall: a 64x64 block texture, seamless on all four edges, one family with the Living Substrate tiles and reservoir_flesh. Dense dark muscular tissue: broad lobes parted by narrow dark creases, a faint grain of fibres running one way, a wet sheen on the crowns. Opaque. It covers every face of the three wall tiers; the Ribbed Wall's ribs and the Calcified Wall's plates are model parts cut from reservoir_bone and stay with art/gen_walls.py. The blockstate turns the Living Wall four ways at random, about the vertical and the horizontal axis, so the edges must still match after a 90-degree turn and no feature may end at an edge. The Ribbed Wall is not turned (its ribs must line up), so its flesh repeats every block and must not show an obvious motif.
- skeletal_graft and calcifying_graft: two 32x32 icons, `--size 32 --colors 0`, one family with the treatment icons. Skeletal Graft: a small curved bone splint wrapped in a pink membrane strip. Calcifying Graft: a chalky white lump of bone paste with a hard, cracked crust.
- Prompt shapes as ART-001 and the synaptic_probe source prompt; evidence `build/art/living_wall-tiled.png` and the capture walls. The placeholders are art/gen_walls.py's generated flesh and flat two-tone discs.

## ART-011 — Window and door graft icons

Status: open. Owner of the asset: Astra.
Files: src/main/resources/assets/infestusfrontier/textures/item/{membrane_window,sphincter_door}.png, CREDITS.txt

- Two 32x32 icons, `--size 32 --colors 0`, one family with the treatment icons. Membrane Window: a small square of pale pink translucent membrane with red veins in a thin ivory bone frame. Sphincter Door: a closed iris of six overlapping pink membrane petals spiralling into a dark centre, ringed by a muscular red rim.
- Prompt shape as the synaptic_probe source prompt; one object, true alpha, no background. The placeholders are flat two-tone discs; the window block and the door model come from art/gen_openings.py and are not part of this story.

### Capsule Stem and Item Capsule pods

Owner, 2026-10-25: three box capsules rejected as not organic; the storage stem concept chosen ("It looks good"). `art/gen_capsule_stem.py` owns the stem's static parts and sixteen-texel textures: a squared maroon column with a dark transport groove down every face and two ivory strips up its north and south faces, a stepped root foot, a bud at a trunk's top, a branch elbow where the trunk forks (its horizontal piece reaching back into the parent's block) and a bone collar on any cell that carries pods. `art/gen_item_capsule.py` owns the pod as a GeckoLib organ: a plump squared hull of dark plum hide with muted salmon folds, a flat dark face toward the player, a fleshy neck into the stem's block gripped by a bone collar over a bone shelf. `grade_0..2` scale the `body` bone about the face's centre (0.72, 0.86, 1), so the face stays where `FrontStockRenderer` draws the stock as an overlay of the organ renderer; the child `flesh` breathes. The generator also paints `item_capsule_lining`, the membrane under the icon. Stem parts stacked, branched and collared share no face plane (`test_stem_parts_share_no_face_planes`).

Evidence: the `capsule_stem` capture: a seed-7 tree grown from data (`grow:40`) with its fork, pods showing stock, three standalone grades, the root's screen, the hotbar icons and a breath; `motion_item_capsule`.
