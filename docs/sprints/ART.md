# Art placeholder list

The one list of temporary assets, and the specifications, prompts and provenance for their finals. Development and playable milestone qualification use functional temporary or procedural artwork and never wait for final art; a task adds its placeholders to the list below in the same change and files no art follow-up. Missing final art, its quality or its generation provenance alone never blocks a task or a milestone. The quality bar below applies to a finishing pass the owner requests separately; such a task is queued after all existing work. Textures come from an image model (Astra) and are saved and exported to the path the story names. GeckoLib models and animations come from a strong model. Rules: [ENGINEERING.md](../../ENGINEERING.md#art-track).

## Placeholder list

Every temporary asset still in the build. Each row gives the paths, the intended appearance, the image or model capability a final needs, and the captures that show it. A row leaves the list when its final is accepted; the specification (below, by story or section) stays.

| Assets | Intended appearance | Capability | Captures | Specification |
|---|---|---|---|---|
| `textures/item/dead_tissue.png`, `textures/item/surgical_blade.png` | Dead Tissue: a limp grey-brown scrap of flesh, a dull pink fleck where it tore. Surgical Blade: a short pale bone handle bound in pink membrane, a thin curved steel blade, held lower left to upper right. Flat two-tone discs now | Image model (icons, 32x32) | `visual_static`, `walls` (`hotbar`) | [ART-009, Dead Tissue and Surgical Blade](#art-009--dead-tissue-and-surgical-blade-icons) |
| `textures/block/living_wall.png`, `textures/item/skeletal_graft.png`, `textures/item/calcifying_graft.png` | Dense dark muscular tile with a faint fibre grain; a bone splint in a pink strip; a chalky cracked bone-paste lump. The tile is generated flesh (`art/gen_walls.py`), the icons flat discs | Image model (one seamless 64x64 tile, two 32x32 icons) | `walls` (`front`, `close_ribbed`, `hotbar`) | [ART-010](#art-010--living-wall-tile-and-graft-icons) |
| `textures/item/membrane_window.png`, `textures/item/sphincter_door.png` | A pale translucent veined membrane square in an ivory frame; a closed iris of six pink petals in a red rim. Flat discs | Image model (two 32x32 icons) | `openings` (`windows`, `hotbar`) | [ART-011](#art-011--window-and-door-graft-icons) |
| `art/gen_washing_kidney.py` model (`geo/block/washing_kidney.geo.json`, `animations/block/washing_kidney.animation.json`, `textures/block_entity/washing_kidney*.png`, `textures/block/washing_kidney.png`, `art/washing_kidney.bbmodel`), the emblem (`Sheet.KIDNEY_EMBLEM`, slot 10 of `textures/gui/organ.png`) and the guide plate (`textures/book/washing_kidney.png`) | Two soft blue-grey bean lobes, a pale clean sac and a murky dirty sac that read apart at hotbar size, a bone sieve between them, a low drip spout, at the Gizzard's finish | Strong model for GeckoLib; no image model | `motion_washing_kidney`, `mineral` (`kidney_screen`, `kidneys`, `hotbar`), `demo` (`kidney_line`, `book_washing_kidney`) | [Washing Kidney](#washing-kidney) |
| `art/gen_shaping_palm.py` model (`geo/block/shaping_palm.geo.json`, `animations/block/shaping_palm.animation.json`, `textures/block_entity/shaping_palm*.png`, `textures/block/shaping_palm.png`, `art/shaping_palm.bbmodel`), the emblem (`Sheet.PALM_EMBLEM`, slot 11 of `textures/gui/organ.png`) and the guide plate (`textures/book/shaping_palm.png`) | An open hand of pink muscle, palm up, on a root foot: four fingers and a thumb stand up round the rim with bone tips curled inward, and in the cup lies an ivory plate with nine bone studs in three rows of three, the crafting grid it keeps; the fingertips curl and open slowly. Reads as a hand that holds a grid, not as a table, from across a room | Strong model for GeckoLib; no image model | `motion_shaping_palm`, `demo` (`palm_line`, `palm_screen`, `palm_empty_screen`, `palm_rote_line`, `palm_rote_screen`, `book_shaping_palm`) | [Shaping Palm](#shaping-palm) |
| The Palm's Rote stage has no assets of its own: it plays the `idle` clip while its `working` block state is set (`textures/block_entity/shaping_palm*.png` unchanged), its screen is drawn from the shared sheet's wells, button and tank in `textures/gui/organ.png`, and its three guide pages carry text only | While a cycle runs the fingers close slowly over the plate and open when it delivers, with a faint wet shine on the bone studs that rises with the cycle and fades at the end (`working` clip and a `shaping_palm_glowmask.png` pulse); the tray wells and the Select button each get a small engraved mark (a held stone, a pressed stud) instead of a plain well and button; the guide gets one plate of a Palm with a cake in its tray | Strong model for GeckoLib for the clip; image model for the three marks and the plate | `demo` (`palm_rote_line`, `palm_rote_screen`, `book_shaping_palm_rote`) | [Shaping Palm](#shaping-palm) |
| The Palm's Mouth line on its screen (`gui.infestusfrontier.shaping_palm.mouth.*`, text only) and the two Palm supply exhibits (vanilla signs and chests, shipped organs, a vein row flush with the floor, no drawn asset; the guide page "Where to see it" is text only) | A small engraved lip beside the Mouth line that shows the Mouth's direction (open for takes, pushing for gives, both, shut for closed); one guide plate of a Palm on a Mouth with a vein running to a tree and to a chest | Image model for the lip and the plate | `demo` (`palm_tree_overview`, `palm_tree_pods`, `palm_tree_screen`, `palm_tree_mouth_screen`, `palm_chest_overview`, `palm_chest_screen`, `palm_signpost`, `book_shaping_palm_reach`, `book_shaping_palm_rote`) | [Shaping Palm](#shaping-palm) |
| `art/gen_mixed_craw.py` model (`geo/block/mixed_craw.geo.json`, `animations/block/mixed_craw.animation.json`, `textures/block_entity/mixed_craw*.png`, `textures/block/mixed_craw.png`, `art/mixed_craw.bbmodel`) and the Item Capsule lining it shares (`textures/block/item_capsule_lining.png`) | A bulk pod's plump squared hull in bog-green hide with pale bone creases, a wide socket with a one-pixel rim holding four small pockets in a square, two ivory ribs on the flanks that read as the partition between kinds; half the room of a bulk pod, a visible cue that it holds several kinds | Strong model for GeckoLib (model and animations); an image model for a hide texture on the finished model | `motion_mixed_craw`, `mixed_craw` (`front`, `grades`, `hotbar`), `demo` (`craw_line`, `craw_screen`) | [Mixed Craw](#mixed-craw) |
| `textures/book/` plates: none for the Craw | The Craw's guide entry is text and the Elastic Gel recipe page, with the Craw item as its icon; a finished entry could add a plate of the pod beside a bulk pod | Strong model (render from the model) | `demo` (`book_mixed_craw`, `book_mixed_craw_recipe`) | [Mixed Craw](#mixed-craw) |
| `art/gen_larder.py` model (`geo/block/larder.geo.json`, `animations/block/larder.animation.json`, `textures/block_entity/larder*.png`, `textures/block/larder.png`, `art/larder.bbmodel`) sharing the Item Capsule lining | The Craw's squared hull in waxed amber hide with pale bone creases and a one-pixel rim, a strap of ivory bone across each flank like a latch, the socket holding up to three small pockets (two over one); reads as a sealed pantry pod beside the Craw's bog-green ribs | Strong model for GeckoLib (model and animations); an image model for a hide texture on the finished model | `motion_larder`, `larder` (`front`, `grades`, `hotbar`, `craw_beside`), `demo` (`larder_line`, `larder_screen`, `larder_tower`) | [Larder](#larder) |
| `textures/book/` plates: none for the Larder | The Larder's guide entry is three text pages and the Sealing Resin recipe page, with the Larder item as its icon; a finished entry could add a plate of the pod beside a Craw | Strong model (render from the model) | `demo` (`book_larder`, `book_larder_recipe`) | [Larder](#larder) |
| `art/gen_withered_flower.py` output: `textures/block/withered_flower_hanging.png`, `withered_flower_forked.png`, `withered_flower_collapsed.png`, the three `models/block/withered_flower_*.json`, `blockstates/withered_flower.json` and the entry icon `textures/book/withered_flowers.png` | Three dead small-flower silhouettes as grey cutouts the block colour tints ash, bone, brown or plum by position: a bent stalk with a hanging spent head, a forked dry stalk with split seed pods, a low collapsed bloom with curled petals; dark root threads at the foot and one dull knot on the stalk; still, no glow. Drawn as rimmed line art by the generator now | Image model (three 64x64 RGBA cutouts in greys so the tint reads, one 16x16 icon) | `withered_flowers` (`front`, `back`, `close`, `above`), `demo` (`meadow`, `withered_flowers`, `withered_flowers_close`, `book_withered_flowers`) | [Withered Flower](#withered-flower) |
| `textures/book/iron_bars_factory.png` (not yet drawn); the factory's signs are waxed vanilla oak signs and its entry icon is vanilla `minecraft:iron_bars` | The Iron Bars Factory entry is six text pages and the vanilla iron-bars recipe page. A finished entry opens on a plate: the line as a left-to-right schematic (Larder, Gizzard, Kidney, Bio-Furnace, Palm, chest of bars) in the organ palette, with the Reservoir and the water pool under it. Finished sign plaques could replace the oak signs | Image model for the plate (200x200 RGBA); a strong model may render it from the demo exhibit | `demo` (`iron_bars_overview`, `iron_bars_supply`, `iron_bars_mill`, `iron_bars_press`, `book_iron_bars_factory`) | [Iron Bars Factory](#iron-bars-factory) |

The Mineral Gizzard has no placeholder left: its model, emblem and plate are the finished work of [CREDITS.txt](../../CREDITS.txt).

Recoverable unfinished Kidney artwork waits in the git stash named `ktask: task #17 attempt #1` (a finer model and animations, texture, emblem and plate, and edits to this file). A finishing pass may recover it with `git stash list` and `git stash show -p`; development does not apply it and does not redo the deferred art.

Fixed for every texture:

- The image model draws the source at its own size. `python3 scripts/texture.py <source.png> <shipped.png> --size 64` downsamples it, caps the palette and writes `build/art/<name>-tiled.png`, a 3x3 repeat to check for seams and repeated bright spots. Only the shipped file is committed.
- Blocks are 64x64, opaque and seamless on all four edges: the prompt says "veins continue across opposite edges". Items are 32x32 on a transparent background, one centered object.
- Original biological look: membrane, muscle, veins, pores, nodules. Broad readable forms, low noise. No grid, no border, no bevel, no central emblem, no text, no copy of an existing game texture.
- Prompts follow the shape used in ART-001: use case, asset type, subject, style, composition, palette, constraints, avoid. 16x16 pixel-art prompts with a five color palette gave the weak placeholders; sources drawn large and downsampled are what read as living material.
- One `CREDITS.txt` line per story: file, who made it, tool, date.

Quality bar. Applies to a finishing pass the owner requests; the owner rejects a final below it. Temporary assets are not judged against it and never hold a task or a milestone.

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
| Biomass Bladder | Reads as a bladder: a translucent sac swelling out of a lip of flesh and closing in under a lipped collar, bone ribs bowed down its corners, the level seen through it; no straight frame, no box; empty is empty; all fill levels stay inside the membrane (`motion_bladder_2` close, corner; `motion_bladder_0`..`4`) |
| Membrane Rack | Skin stays between posts; the droplet resets while invisible |
| Bone Loom | Reads as a ribcage loom: curved ribs gripping a plate woven in unequal strips is the cue; the ribs breathe and clench without parting from the mound; the shuttle stays on its strip and clear of the ribs (`motion_bone_loom` close, corner, back) |
| Awakening Cradle | Reads as something being brought to life: the glowing cocoon is the cue; closed petals clear the cocoon and its pulse, open ones stay inside the block |
| Bio-Furnace | All three stages; reads as a furnace from across a room, day and night: dark body, hot mouth and seams, fire and light only while it works; the fangs, coals, chimney, buttresses, bellows and second chimney remain distinct |
| Activation Cyst, Seed Pouch | Chambers, neck, rim and throat follow their supporting tissue |
| Planting Proboscis | Reads as a seed weevil: the long rostrum over the bed in front and the sac of seeds on its back are the cues; the stab reaches the bed and the seed is left in it; the rostrum never enters the head; legs and claws stay on the slab (`motion_planting_proboscis`: close, corner and low views) |
| Harvest Corolla | Moving parts remain attached; petals clear the heart; tips clear the floor |
| Spine Sentry | Reads as a rooted lash: the plated, barbed spine coiled over a muscle mound that slopes up to its bone collar, tendons pulling at it, is the cue; working, it swings from side to side and whips out without the segments parting; the thorn clears the mound (`motion_spine_sentry` close, corner; `sentry`: a zoglin beside it, the store screen) |
| Mineral Gizzard | Reads as a grinding muscle: two bone millstones in a lipped hopper on a wide, low belly wrapped in ridged muscle, and a bone chute with a drift of dust, are the cues; the stones swing against each other inside the hopper without reaching a lip; the ridges clench in a wave; the drift builds and slumps below the chute (`motion_mineral_gizzard` close, corner, top; `mineral`: screens, icons) |
| Washing Kidney | Reads as a rinsing organ: two soft blue-grey lobes under a pale clean-water sac and a murky dirty-water sac, a bone sieve between them and a low drip spout, are the cues; the lobes squeeze in a wave without parting, the sieve shivers between the sacs, the dirty sac swells while working, the drip stays under the spout and inside the block (`motion_washing_kidney` close, corner; `mineral`: screens `kidney_screen`, `full_kidney_screen`, `kidneys`) |
| Compost Gland | Reads as a fermentation gut of the mod's flesh: the gaping maw of scraps above and the meal-crusted spout below are the cues; the belly churns in a wave without its plates parting; the flanks swell at rest; the crumb of meal shows only while the spout presses, at its largest in `close_008` (`motion_compost_gland` close, corner, back) |
| Seed Pouch | Reads as a seed sac: the crowned heap of seeds in flared lips and the split lobes are the cues; the body swells at its waist and tapers to the neck with no ledge; the lobes part without leaving the core; the cord stays round the neck (`motion_seed_pouch` close, corner, side) |
| Vascular Junction | Reads as a heart: the lobed body that beats and the lipped sphincters are the cues; all six rings in all three modes gape, pucker and shrink without their lips parting; the bottom ring stays on the foundation (`motion_junction_intake` close, corner; `motion_junction_closed`, `motion_junction_outlet`) |
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

Owner, 2026-10-25: three box capsules rejected as not organic; the storage stem concept chosen ("It looks good"). `art/gen_capsule_stem.py` owns the stem's static parts and sixteen-texel textures: a squared maroon column with a dark transport groove down every face and two ivory strips up its north and south faces, a stepped root foot, a bud at a trunk's top, a branch elbow where the trunk forks (its horizontal piece reaching back into the parent's block) and a bone collar on any cell that carries pods. `art/gen_item_capsule.py` owns the pod as a GeckoLib organ: a plump squared hull of dark plum hide with short muted salmon creases, a socket of dark flesh in its outer face behind a two-pixel rim, a fleshy neck into the stem's block gripped by a bone collar over a bone shelf that reaches the stem's collar. `grade_0..2` scale the `body` bone about the hull's back (0.72, 0.86, 1), so a young pod hugs the stem and grows outward; `FrontStockRenderer` draws the stock in the socket as an overlay of the organ renderer, its pocket scaled about the same point (`Pockets.scaled`), so membrane and icon stay inside the rim at every grade and show no sliver from the side; the child `flesh` breathes about the socket floor so the membrane never fights it. The generator also paints `item_capsule_lining`, the membrane under the icon. Owner, 2026-10-26, from play: the first look had the membrane a pixel high and floating off a face that shrank with the grade (young pods read as black boxes), the creases chained into one diagonal seam, the shelf stopped short of the collar, and young pods stood off the stem on a long neck; all five fixed here. Stem parts stacked, branched and collared share no face plane (`test_stem_parts_share_no_face_planes`).

Evidence: the `capsule_stem` capture: a seed-7 tree grown from data (`grow:40`) with its fork, pods showing stock, three standalone grades, the root's screen, the hotbar icons and a breath; `motion_item_capsule`.

## Living equipment icons

Five living items share ivory bone heads, burgundy sinew grips, pink tendons and a dark plum outline. The dormant icons share the living heads and use dry bone, plain walnut wood and a small tan binding at the joint. Contractile Pick, Hatchet and Spade have thicker purple muscle and broad pale-violet contraction bands. Grasping Fang has three large backward-facing hooks and a purple grip. The hoe has no mutation. The `infestusfrontier:tool_mutation` item predicate selects the four variants.

All fourteen sources are original image-model artwork. Export each with `python3 scripts/texture.py <source> src/main/resources/assets/infestusfrontier/textures/item/<name>.png --size 30 --colors 0`, then paste that RGBA image at `(1, 1)` on a transparent 32x32 canvas, without a paste mask so alpha is preserved. After padding, set alpha below 128 to 0 and alpha at least 128 to 255. The binary edge avoids translucent fringes on held-item geometry, and the padding leaves a transparent pixel around the icon. Sources stay outside the repository, under `/home/etf/.codex/generated_images/01a1864b-612e-7030-9aee-75498ce7e660/`.

| Shipped name | Source file |
|---|---|
| `dormant_pick` | `exec-662888f8-543d-4067-ba57-110bc9d905ea.png` |
| `dormant_hatchet` | `exec-04c9378f-1af2-4b60-b51b-85ce389089df.png` |
| `dormant_spade` | `exec-0cf9dcf6-9b5f-4ed0-8a9c-4cabf4b8ee50.png` |
| `dormant_hoe` | `exec-883209de-8632-4d2d-8a7c-1bdc8f947fb7.png` |
| `dormant_fang` | `exec-2194b770-0b66-4667-8181-9b204d6e9855.png` |
| `living_pick` | `exec-8c80b491-5e22-42d6-baa8-f80659acbb6d.png` |
| `living_hatchet` | `exec-c216f760-5368-458e-b626-fb9382c9fa27.png` |
| `living_spade` | `exec-28e23695-5cd2-495a-b6d4-b9daf0e25b52.png` |
| `living_hoe` | `exec-cf0fbad2-2085-4d72-8421-8a2e1ba42aca.png` |
| `living_fang` | `exec-f7ad27eb-7123-46a1-8f4b-55f684996833.png` |
| `living_pick_contractile` | `exec-e6d0e945-7362-470f-8c31-05ab5cbfa25b.png` |
| `living_hatchet_contractile` | `exec-a1bc7000-7898-46eb-a85f-1cff3b0942ad.png` |
| `living_spade_contractile` | `exec-96e7077e-aa37-4e4b-bf63-f6dff842229c.png` |
| `living_fang_grasping` | `exec-d51ec080-e25d-4efd-a072-2191106b6601.png` |

Prompt contract: one isolated original biological Minecraft inventory icon, diagonal grip bottom-left and head top-right, broad chunky shading legible at 32 pixels, ivory lit upper-left and tan shadows, burgundy muscle, thin dark plum outline, true alpha, clear margin, no metal, jewel, text, scene or ground shadow. Pick: two hooked bone prongs. Hatchet: one broad crescent blade and a short blunt poll. Spade: pointed shoulder-blade scoop along the shaft. Hoe: one thick flat blade at a right angle to the shaft, no opposite prong, with three broad muscle sections on the living handle. Fang: a long curved tooth blade and a short grip. Generate the Pick first and use it as the family reference for the other four. Each Contractile edit preserves its head and adds thick purple muscle and three broad pale-violet tendon bands; use the mutated Pick as their colour reference. The Grasping edit preserves the Fang and adds three broad backward hooks to the lower blade edge and a purple tendon grip. For each dormant precursor, preserve the living head and replace all muscle with a plain walnut shaft and a small dry tan binding only at the joint; remove decorative bands and pommels, keep broad matte ivory/tan bone shading, and preserve the same upper-left light. Dormant Fang keeps a long smooth tooth blade and a short grip.

Evidence: `equipment_showcase` captures the tools and mutations held in game. Compare silhouette, transparent edges and mutation colour at hotbar size; the Grasping Fang must show its hooked edge.

## ART-009 — Mineral workshop item icons

Five 32x32 item icons for the automated mineral workshop: `iron_dust`, `gold_dust`, `copper_dust`, `rock_residue`, `mineral_tailings`. Original image-model artwork (built-in image generation, 2026-11-01), one isolated object on true alpha, outline and light as the Spore Culture icon. Sources (1254x1254 RGBA, kept outside the repository) and their shipped names:

| Shipped name | Source file |
|---|---|
| `iron_dust` | `exec-5c36c16d-79be-4ec6-b3e1-85ca38aa8497.png` |
| `gold_dust` | `exec-43776cfd-c965-4b38-9a0b-ebb082e43d1a.png` |
| `copper_dust` | `exec-0da768da-febb-4874-be2b-f8bf4fc58e21.png` |
| `rock_residue` | `exec-5ff7521d-834f-4c01-99bf-71f5c9cad48e.png` |
| `mineral_tailings` | `exec-cb5e68e5-c556-4ea2-86cb-71039c02eb8b.png` |

Fit each source to its square with `python3 art/fit_icon_source.py <source> <fitted>` (trim to content, 84 percent fill, visual centroid on the centre, so the icon sits centred in its slot), then export each with `python3 scripts/texture.py <fitted> src/main/resources/assets/infestusfrontier/textures/item/<name>.png --size 32 --colors 0`. Then `python3 art/derive_tailings_flecks.py` cuts `mineral_tailings_flecks.png` from the shipped Tailings icon: its brightest nearly grey pixels, grey on clear. The item model draws it as layer 1 and `MineralModule` tints it with the metal of the stack's `parcel` (iron, gold or copper), so the clod stays image-model artwork. `python3 art/gen_book_mineral_gizzard.py` then redraws the guide plate, which lays the five icons under the organ.

Prompt, one subject line each, the rest shared:

```text
Use case: stylized-concept. Asset type: source for a 32x32 voxel-game item icon. Subject: <subject>, centered, filling about 75 percent of the canvas. Style: hand-painted game sprite, bold readable silhouette, soft shading lit from upper left, thin dark outline. Palette: <palette>. Constraints: square, true alpha transparency, one isolated object, no background, no border, no text, no watermark. Avoid: photorealism, scenery, loose particles, glow, lens flare.
```

| Icon | Subject | Palette |
|---|---|---|
| `iron_dust` | one small low heap of fine pale steel-grey iron dust, a soft mound of fine grains with a few brighter silvery sparkles | cool slate and pale steel grey, deep charcoal shadows, one small near-white highlight |
| `gold_dust` | one small low heap of fine golden dust, a soft mound of fine grains with a few brighter glinting sparkles | warm gold and amber, deep brown shadows, one small pale-yellow highlight |
| `copper_dust` | one small low heap of fine reddish copper dust, a soft mound of fine grains with a few brighter glinting sparkles | orange-copper and terracotta, deep umber shadows, one small pale-peach highlight |
| `rock_residue` | one small cluster of three chunky angular pale grey rock chips piled together, faceted and slightly chipped | pale stone grey, slate shadows, one small off-white highlight per chip |
| `mineral_tailings` | one lumpy clod of dark grey-brown crushed grit and mud-like gravel, rough and uneven, with a scattering of tiny bright pale-grey metallic flecks embedded across its surface | dark umber and grey-brown, charcoal shadows, pale silvery-grey flecks |

Evidence: the `mineral` scenario's `hotbar` and screen views, the demo's `mineral_items` and `mineral_line`, and the Gizzard book spreads. Check the silhouettes at hotbar size, the transparent edges, the three Tailings tints and the book plate.

## Aerocyte Bloom

`art/gen_aerocyte_bloom.py` writes the model, Blockbench source, atlas, break particle and `idle`/`working` clips. A burgundy rooted bulb narrows into a muscle stalk. Above it, four ivory petals open around a mint spore bladder and upward throat. The root remains still; a four-second idle breath gently changes the bladder and petals. The two-second working pulse squeezes the bladder and opens the petals. All parts stay inside the block sideways; the highest resting point is 27.2 pixels.

The v3 Aerocyte Bloom supplies the anatomical reference and ivory/burgundy/mint palette. Geometry, UVs, textures and looped keyframes are newly generated through the v4 shared organ pipeline. No v3 asset file is copied. The material source prompt is retained in v3 `art/defense/AEROCYTE.md`; the v4 generator defines its own pixel ramps and veins.

Capture checks: the mint bladder must be visible between the four petals from ground level; the throat must read as a dark opening from above; no petal crosses another or detaches from the cup through the working pulse. The defensive shot leaves the nozzle at block centre, 1.75 blocks above the floor.

## Washing Kidney

`art/gen_washing_kidney.py` writes the model, Blockbench source, atlas, break particle and `idle`/`working` clips through the shared organ pipeline; `art/gen_gui_sheet.py` draws the emblem (`Sheet.KIDNEY_EMBLEM`) and `art/gen_book_washing_kidney.py` renders the guide plate from the model and the existing mineral icons. All three are procedural placeholders written with the functional task. The Kidney needs no new item icon: it makes the Gizzard's dust, Tailings and Rock Residue.

Specialist work to do, by an artist with a strong model for GeckoLib (no image model needed):

- Replace the procedural model with a finished one. Cues to keep: two lobes (bean), a clean sac and a dirty sac that read as different at hotbar size and from across a room, a sieve plate between them, a drip spout. Keep the block's footprint (`Block.box(1, 0, 1, 15, 12, 15)`), the bone names `lobe_l`, `lobe_r`, `clean`, `dirty`, `sieve`, `spout`, `drip` only if the clips keep using them, and the clips `idle` and `working`; every rule of the quality bar and `art/test_motion.py` applies.
- Redraw the emblem (32x32 at slot 10 of the organ GUI sheet) and the guide plate (200x200 RGBA, `textures/book/washing_kidney.png`) at the Gizzard's finish.
- Update `CREDITS.txt` for the new files.

Capture acceptance: the `mineral` scenario's `kidney_screen`, `full_kidney_screen`, `kidneys` and `hotbar` views; the `motion_washing_kidney` scene (motion, back, side, close, corner); the demo's `kidney_line`, `kidney_screen`, `kidney_items` and the book views `book_washing_kidney`, `book_washing_kidney_washing`, `book_washing_kidney_settling`, `book_washing_kidney_pause`. The reviewer reads the PNGs; the hotbar icon's centroid stays within 1.5 px of its slot centre.

## Mixed Craw

`art/gen_mixed_craw.py` writes the model, Blockbench source, atlas, break particle and `idle`/`grade_0..2` clips through the shared organ pipeline, reusing the Item Capsule's hull, neck, collar, grades and breath (`art/gen_item_capsule.py`, now parametrised on the rim width, the hide palette and extra cubes). It is temporary procedural art written with the functional task: dark bog-green hide with pale bone creases, a one-pixel rim round the socket (10 pixels across) and two ivory flank ribs. `StorageModule` draws the stock in four pockets in a square, each 4.8 pixels across on a 5 pixel pitch, over `item_capsule_lining`; the pockets follow the socket, so a final model keeps the 10 pixel socket and its floor 4.5 pixels behind the front.

Specialist work to do, by an artist with a strong model for GeckoLib:

- Replace the procedural model with a finished one. Cues to keep: the block's footprint and the pod's neck, collar and shelf into the stem (the same grades, 0.72, 0.86 and 1 about the hull's back), a socket that shows four separate pockets, a hide and rim that tell it from a bulk pod at a glance and from across a room. Keep the clips `idle`, `grade_0`, `grade_1`, `grade_2`; every rule of the quality bar and `art/test_motion.py` applies.
- Optionally, an image-model hide texture on the finished model and a guide plate (200x200 RGBA, `textures/book/mixed_craw.png`) beside a bulk pod.
- Update `CREDITS.txt` for the new files.

Capture acceptance: `motion_mixed_craw` (motion, back, side), the `mixed_craw` scenario's `front`, `grades` and `hotbar`, the demo's `craw_line` and `craw_screen`, and the book views `book_mixed_craw` and `book_mixed_craw_recipe`. The reviewer reads the PNGs; the hotbar icon's centroid stays within 1.5 px of its slot centre.

## Larder

`art/gen_larder.py` writes the model, Blockbench source, atlas, break particle and `idle`/`grade_0..2` clips through the shared organ pipeline, reusing the Item Capsule's hull, neck, collar, socket, grades and breath (`art/gen_item_capsule.py`) as the Craw does. It is temporary procedural art written with the functional task: waxed amber hide with pale bone creases, a one-pixel rim round the socket and two ivory flank straps. `StorageModule` draws the stock in the Craw's pockets (two over one for three kinds), 4.8 pixels across on a 5 pixel pitch, over `item_capsule_lining`. The reserve and the label have no model of their own: they live on the screen. The pod screen's label wells, reserve and Withdraw buttons are the shared sheet's wells and buttons.

Specialist work to do, by an artist with a strong model for GeckoLib:

- Replace the procedural model with a finished one. Cues to keep: the block's footprint and the pod's neck, collar and shelf into the stem (the same grades, 0.72, 0.86 and 1 about the hull's back), a socket that shows up to three pockets, a hide and straps that tell it from a Craw and a bulk pod at a glance and from across a room. Keep the clips `idle`, `grade_0`, `grade_1`, `grade_2`; every rule of the quality bar and `art/test_motion.py` applies.
- Optionally, an image-model hide texture on the finished model and a guide plate (200x200 RGBA, `textures/book/larder.png`) beside a Craw.
- Update `CREDITS.txt` for the new files.

Capture acceptance: `motion_larder` (motion, back, side), the `larder` scenario's `front`, `grades`, `hotbar` and `craw_beside`, the demo's `larder_line`, `larder_screen`, `larder_tower` and `larder_tower_screen`, and the book views `book_larder` and `book_larder_recipe`. The reviewer reads the PNGs; the hotbar icon's centroid stays within 1.5 px of its slot centre.

## Shaping Palm

`art/gen_shaping_palm.py` writes the model, Blockbench source, atlas, break particle and `idle` clip through the shared organ pipeline; `art/gen_gui_sheet.py` draws the emblem (`Sheet.PALM_EMBLEM`) and `art/gen_book_shaping_palm.py` renders the guide plate from the model and the Membrane Sheet and Bone Plate icons. All three are procedural placeholders written with the functional task. The Palm needs no new item icon: it is an organ item drawn from its model, and it makes nothing of its own.

Specialist work to do, by an artist with a strong model for GeckoLib (no image model needed):

- Replace the procedural model with a finished one. Cues to keep: a hand (palm, fingers, thumb) cupped round a plate of nine studs that reads as the crafting grid, bone tips, no glow (it works only by hand). Keep the block's footprint (`Block.box(1, 0, 1, 15, 10, 15)`), the clip `idle`, and the bone names `tip_*`, `pad`, `plate` only if the clip keeps using them; every rule of the quality bar and `art/test_motion.py` applies. Reach (stage 2) has no model change: the Palm's screen shows Reach and its Mouth's direction as text, and its buttons use the shared sheet button. A working clip is not part of this model.
- Redraw the emblem (32x32 at slot 11 of the organ GUI sheet) and the guide plate (200x200 RGBA, `textures/book/shaping_palm.png`).
- Update `CREDITS.txt` for the new files.
- Rote (stage 3) placeholders: add the `working` clip the block state `working` already selects (today the `idle` clip plays), a glow-mask pulse on the studs, an engraved mark on each of the four tray wells and on the Select button (`Sheet` entries drawn by `art/gen_gui_sheet.py`), and a guide plate for the Rote pages. Keep the screen's geometry: `ShapingPalmMenu.trayWell` and the third body strip.

Capture acceptance: the `motion_shaping_palm` scene (motion, back, side, close, corner); the demo's `palm_line`, `palm_parts`, `palm_screen`, `palm_empty_screen`, `palm_reach_line`, `palm_reach_screen`, `palm_larder_screen` and the book views `book_shaping_palm`, `book_shaping_palm_keeps`, `book_shaping_palm_reach` (the Reach and supply pages as one spread), `palm_rote_line`, `palm_rote_kit_line`, `palm_rote_screen`, `book_shaping_palm_rote` (the exhibit and Rote pages), `book_shaping_palm_recipe`, and the Mouth-supplied exhibits `palm_tree_overview`, `palm_tree_pods`, `palm_tree_screen`, `palm_tree_mouth_screen`, `palm_chest_overview`, `palm_chest_screen`, `palm_signpost`. The reviewer reads the PNGs.

## Iron Bars Factory

The factory is built from shipped organs and vanilla signs and chests, so it adds no model, texture or sound. Its one temporary asset is the missing guide plate: the entry has text pages only, icon `minecraft:iron_bars`. A finishing pass may add `textures/book/iron_bars_factory.png` (200x200 RGBA, a schematic of the line as the row above describes) and an image page at the head of `entries/circulation/iron_bars_factory.json`. Update `CREDITS.txt` for the file.

Capture acceptance: the demo's `iron_bars_overview`, `iron_bars_supply`, `iron_bars_mill`, `iron_bars_press`, the screens `iron_bars_larder_screen`, `iron_bars_gizzard_screen`, `iron_bars_kidney_screen`, `iron_bars_furnace_screen`, `iron_bars_palm_screen` and the book views `book_iron_bars_factory` (the line and the route), `book_iron_bars_factory_2` (costs and supplies), `book_iron_bars_factory_4` (reserves and parts), `book_iron_bars_factory_6` (the recipe). The reviewer reads the PNGs.

## Withered Flower

Placeholder: `art/gen_withered_flower.py` draws the three silhouettes as rimmed grey line art and the 16x16 entry
icon. The block colour tints every look from the position's seed, so a final keeps the textures grey.

Texture story, three sources of 64x64 RGBA on a transparent background, exported with
`python3 scripts/texture.py <source.png> <shipped.png> --size 64 --colors 0`:

1. `textures/block/withered_flower_hanging.png`. Prompt: "Use case: stylized-concept. Asset type: source for a 64x64 voxel-game cross-plane plant texture, seen from the side, transparent background. Subject: one dead small flower, a thin stalk rising from the bottom centre and bending over at the top so the spent seed head hangs down, two limp leaves, a few dark root threads at the foot and one dull fleshy knot on the stalk. Style: hand-painted game sprite, soft shading lit from the upper left, thin dark rim. Palette: greys only, mid grey stalk, pale grey head, near-black rim; the game tints it. Constraints: the stalk touches the bottom edge, nothing touches the side or top edges, true alpha transparency, no background, no text. Avoid: colour, glow, petals in bloom, a pot."
2. `textures/block/withered_flower_forked.png`. Same prompt with the subject: "one dead small flower, a thin dry stalk that forks twice into three stems, each ending in a small split seed pod, one dull knot at the fork, dark root threads at the foot."
3. `textures/block/withered_flower_collapsed.png`. Same prompt with the subject: "one dead small flower collapsed to the ground, a short stalk and a dry centre with six curled petals splayed outward and down round it, one dull knot at the base, dark root threads at the foot; the whole plant stays in the lower half of the canvas."
4. `textures/book/withered_flowers.png`: the hanging silhouette tinted plum (`#764C62`), 16x16.

Checks: `withered_flowers` captures `front`, `close` and `above`, and `demo` `withered_flowers_close`: the three
silhouettes and all four tints appear across the patch and read apart at four blocks; the cutout shows no black
patch and no missing texture.
