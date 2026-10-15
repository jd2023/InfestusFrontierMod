# Runtime art provenance

All shipped Infestus Frontier textures are original assets and do not reuse Minecraft or franchise textures. The active art was generated as source material with OpenAI's built-in image-generation tool, then mechanically exported with broad readable patterns at 64×64 for blocks and 32×32 for standalone items. Placeable block items inherit their block models so their inventory icons have the same shape and materials as the placed blocks.

## Biological Wisp

Runtime asset: `src/main/resources/assets/infestusfrontier/textures/particle/bio_wisp.png`

The transparent generated source was mechanically downsampled to 32×32 with a Lanczos filter. It is used only by Infestus Frontier's custom particle provider.

Final prompt:

> Use case: stylized-concept
> Asset type: source art for a tiny Minecraft particle sprite, later downsampled to 32x32
> Primary request: a single soft bioluminescent biological spore wisp, like a tiny living cell or glowing seed, suitable for a DNA double-helix particle effect
> Scene/backdrop: genuinely transparent background
> Subject: one centered round organic mote with a bright compact core, soft membrane halo, and three very short subtle radial filaments; no detached particles
> Style/medium: clean hand-painted game sprite source, bold readable silhouette, smooth gradients that survive downsampling, original biological sci-fantasy design
> Composition/framing: perfectly centered with generous transparent padding on every side, fills about 60 percent of canvas
> Lighting/mood: emissive inner glow
> Color palette: white-hot pale center, magenta and violet membrane with a slight acid-green fringe; high contrast
> Constraints: square image, true alpha transparency, one isolated sprite only, no background, no border, no text, no watermark, no hard rectangular edges
> Avoid: photorealism, detailed scene, multiple spores, stars, smoke cloud, lens flare, copyrighted symbols

## Living Substrate

The active terrain now uses three generated biological materials and a world-aligned 4×4-block pattern. Source exports, exact prompts, and the mechanical seam/tiling pipeline are recorded in [art/substrate/README.md](art/substrate/README.md). Each runtime tile is 64×64; `living_substrate.png` is the seamless seeded side material retained for compatibility. The older prompt below records the superseded initial prototype.

Liquid biomass also has original generated source art documented there, exported as a 64×256 four-frame animated texture. The reservoir and extractor frame models reuse our own original copper and bio-glass textures. The capsule has its own 3D vial geometry using those materials and the new liquid.

Runtime asset: `src/main/resources/assets/infestusfrontier/textures/block/living_substrate.png`

Final prompt:

> Use case: stylized-concept
> Asset type: source for a 16x16 voxel-game Living Substrate block texture
> Primary request: Create an original living ground surface with three very large matte black-plum membrane plates divided by one thick rust-red branching vein and two muted amber nutrient spots.
> Style/medium: extremely simple chunky pixel art; huge deliberate clusters; no more than 10 flat colors; designed for immediate recognition at 16x16
> Composition/framing: orthographic square material swatch, edge-to-edge, broad asymmetrical shapes, no focal object
> Constraints: opaque; strong dark-red identity; entirely original; no text; no symbols; no watermark; no recognizable existing game texture
> Avoid: fine grain, many pores, tiny details, dense noise, purple glow, sculk, eyes, faces, photorealism, cube render

## Mutation Chamber

Runtime material assets:

- `src/main/resources/assets/infestusfrontier/textures/block/mutation_chamber_copper.png`
- `src/main/resources/assets/infestusfrontier/textures/block/mutation_chamber_glass.png`
- `src/main/resources/assets/infestusfrontier/textures/block/mutation_chamber_biomass.png`
- `src/main/resources/assets/infestusfrontier/textures/block/mutation_chamber_spore.png`

The GeckoLib runtime atlas at `textures/block/mutation_chamber_geo.png` is a mechanical horizontal composition of those four 64×64 materials, producing a 256×64 atlas.

Copper prompt:

> Use case: stylized-concept
> Asset type: source for a 16x16 voxel-game mutation chamber copper material tile
> Primary request: Create an original hammered copper-and-organic-frame surface with broad orange-copper plates, dark brown seams, and only a few large turquoise patina marks.
> Style/medium: extremely simple chunky pixel art; huge deliberate pixel clusters; no more than 12 flat colors; designed to remain legible at 16x16
> Composition/framing: orthographic square material swatch, edge-to-edge, only 3 to 5 large structural shapes, no focal object
> Constraints: opaque; high contrast; entirely original; no text; no symbols; no watermark; no recognizable existing game texture
> Avoid: fine grain, tiny marks, dense noise, photorealism, gradients, purple-dominant palette, cube render, scenery

Glass prompt:

> Use case: stylized-concept
> Asset type: source for a 16x16 voxel-game mutation chamber glass material tile
> Primary request: Create an original pale cyan bio-glass surface with one broad white diagonal reflection, dark teal edge bands, and one faint pale-green internal streak.
> Style/medium: extremely simple chunky pixel art; huge deliberate pixel clusters; no more than 10 flat colors; designed to remain legible at 16x16
> Composition/framing: orthographic square material swatch, edge-to-edge, only 2 to 4 large features, no focal object
> Constraints: opaque visual suggestion of translucent glass; high contrast; entirely original; no text; no symbols; no watermark; no recognizable existing game texture
> Avoid: fine grain, tiny speckles, dense noise, photorealism, blur, purple-dominant palette, cube render, scenery

Biomass prompt:

> Use case: stylized-concept
> Asset type: source for a 16x16 voxel-game mutation chamber nutrient material tile
> Primary request: Create an original preserved nutrient-tissue surface using broad muted rose and warm tan organic bands secured by two dark red support strands.
> Style/medium: extremely simple chunky pixel art; huge deliberate pixel clusters; no more than 12 flat colors; designed to remain legible at 16x16
> Composition/framing: orthographic square material swatch, edge-to-edge, only 3 to 5 large smooth bands, no focal object
> Constraints: opaque; high contrast; abstract and non-graphic; entirely original; no text; no symbols; no watermark; no recognizable existing game texture
> Avoid: anatomy, gore, fine grain, pores, tiny veins, dense noise, face, eye, photorealism, purple-dominant palette, cube render, scenery

Spore-core prompt:

> Use case: stylized-concept
> Asset type: source for a 16x16 voxel-game mutation chamber spore-culture material tile
> Primary request: Create an original bright violet culture membrane with one large orchid central glow, three pale cream culture nodes, and a thick dark-plum border.
> Style/medium: extremely simple chunky pixel art; huge deliberate pixel clusters; no more than 12 flat colors; designed to remain legible at 16x16
> Composition/framing: orthographic square material swatch, edge-to-edge, one dominant central glow and exactly three large nodes
> Constraints: opaque; very high contrast; entirely original; no text; no symbols; no watermark; no recognizable existing game texture
> Avoid: fine grain, many bubbles, tiny dots, dense noise, eye, face, photorealism, blur, cube render, scenery

## Chitin Lattice

Runtime asset: `src/main/resources/assets/infestusfrontier/textures/block/chitin_lattice.png`

Final prompt:

> Use case: stylized-concept
> Asset type: source for a 16x16 voxel-game Chitin Lattice block texture
> Primary request: Create an original armor surface dominated by exactly five large warm-ivory chitin plates separated by thick dark burgundy joints, with one small broken reddish inner edge.
> Style/medium: extremely simple chunky pixel art; huge deliberate clusters; no more than 10 flat colors; designed for immediate recognition at 16x16
> Composition/framing: orthographic square material swatch, edge-to-edge, five irregular plates, no tiny repeated pattern
> Constraints: opaque; bright bone-colored identity; high contrast; entirely original; no text; no symbols; no watermark; no recognizable existing game texture
> Avoid: fine grain, honeycomb, scales, bricks, dense noise, purple-dominant palette, eyes, faces, photorealism, cube render

## Synaptic Core

Runtime asset: `src/main/resources/assets/infestusfrontier/textures/block/synaptic_core.png`

Final prompt:

> Use case: stylized-concept
> Asset type: source for a 16x16 voxel-game Synaptic Core block texture
> Primary request: Create an original energized neural surface with one broad electric-magenta Y-shaped nerve over a saturated royal-violet field and three large pale orchid pulse nodes.
> Style/medium: extremely simple chunky pixel art; huge deliberate clusters; no more than 10 flat colors; designed for immediate recognition at 16x16
> Composition/framing: orthographic square material swatch, edge-to-edge, one dominant Y shape and exactly three large nodes
> Constraints: opaque; very bright purple-magenta identity; high contrast; entirely original; no text; no symbols; no watermark; no recognizable existing game texture
> Avoid: fine grain, tiny veins, many dots, dense noise, eye, face, brain, photorealism, cube render

## Sinew Tendril

Runtime assets:

- `src/main/resources/assets/infestusfrontier/textures/block/sinew_tendril.png`
- `src/main/resources/assets/infestusfrontier/textures/block/sinew_tendril_cap.png`

The cap is a 64×64 export of the previously generated matching cap study.

Final side prompt:

> Use case: stylized-concept
> Asset type: source for a 16x16 voxel-game Sinew Tendril block texture
> Primary request: Create an original vertical braided support material with two very broad crimson strands crossing around three thick pale-ivory chitin bands over a nearly black red background.
> Style/medium: extremely simple chunky pixel art; huge deliberate clusters; no more than 10 flat colors; designed for immediate recognition at 16x16
> Composition/framing: orthographic square material swatch, edge-to-edge, strong vertical direction, only five major bands
> Constraints: opaque; clear red-and-ivory identity; high contrast; entirely original; no text; no symbols; no watermark; no recognizable existing game texture
> Avoid: fine grain, tiny fibers, dense noise, plant vine, eye, face, photorealism, cube render

## Waking Genome guide

Runtime asset: `src/main/resources/assets/infestusfrontier/textures/item/waking_genome.png`

Final prompt:

> Use case: stylized-concept
> Asset type: standalone 2D item sprite source for a 16x16 voxel-game field guide named Waking Genome
> Primary request: Create one unmistakable closed field-guide book silhouette: broad rectangular turquoise cover, thick warm-copper spine on the left, two large pale-ivory branching genome strokes centered on the cover, and one copper corner clasp.
> Style/medium: extremely simple chunky pixel art; hard square pixel clusters; no anti-aliasing; no more than 10 flat colors; readable as a book at 16x16
> Composition/framing: single centered object, slight three-quarter tilt, fills about 75 percent of the square, generous transparent padding on every side
> Background: fully transparent alpha
> Constraints: entirely original; strong cyan-and-copper identity; opaque object; no written text; no letters; no watermark; no recognizable existing game asset
> Avoid: purple-dominant palette, round seed shape, oval silhouette, fine grain, tiny details, glow, scenery, mockup, inventory frame

## Spore Culture

Runtime asset: `src/main/resources/assets/infestusfrontier/textures/item/spore_culture.png`

Final prompt:

> Use case: stylized-concept
> Asset type: standalone 2D item sprite source for a 16x16 voxel-game Spore Culture
> Primary request: Create one unmistakable culture-vial silhouette: squat transparent cyan glass ampoule with a narrow neck and copper stopper, containing one large luminous lime-green spore mass with exactly three pale cream nodes.
> Style/medium: extremely simple chunky pixel art; hard square pixel clusters; no anti-aliasing; no more than 10 flat colors; readable as a vial at 16x16
> Composition/framing: single centered upright object, fills about 75 percent of the square, generous transparent padding on every side
> Background: fully transparent alpha
> Constraints: entirely original; strong green-cyan identity; opaque outlined silhouette; no written text; no symbols; no watermark; no recognizable existing game asset
> Avoid: purple-dominant palette, book shape, oval seed, fine grain, many bubbles, tiny details, blur, scenery, mockup, inventory frame
