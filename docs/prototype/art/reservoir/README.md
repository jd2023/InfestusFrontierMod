# Living reservoir materials

Generated with the built-in imagegen tool on 2026-09-07. No vanilla textures were used. The user's reservoir illustration informed palette and anatomy; its mouth/opening was explicitly excluded. Geometry remains native, code-owned mesh/model data, not a generated tank image.

`living-materials.png` is the original four-material study. `transparent-veins.png` is the second, real-alpha membrane study, selected after in-game inspection exposed the first membrane's opacity. The first atlas's membrane quadrant is retained for provenance but no longer exported. The other three quadrants provide outer flesh, biomass and stalk materials.

Reproduce the mechanical crop/resize and native organ model export with:

```sh
java tools/ExportLivingReservoir.java
```

The exporter preserves generated alpha and reduces selected materials to 64×64. It does not procedurally paint substitute art. Existing `liquid_biomass` art used by other blocks is unchanged; the reservoir selects its own new biomass material.

## Exact atlas prompt

Create a square game texture material atlas, exactly 2 by 2 equal quadrants edge-to-edge with NO gutters, NO text. These are original biological living reservoir materials for Minecraft, NOT a picture of a tank, NOT perspective. Orthographic flat surface samples with coherent hand-painted readable large forms suitable for reduction to 64 pixels each. Upper left: burgundy purple fleshy chitin with broad layered sinewy ribs, rich dark recesses and mauve highlights, seamless edges. Upper right: pale muted mauve translucent-looking skin membrane, delicate branching dark purple capillary veins across broad empty pale areas, seamless edges. Lower left: luminous acid lime green viscous biomass, broad glowing cellular globules and subtle dark green inclusions, no bubbles outlines or symbols, seamless edges. Lower right: dark purple bundled vertical sinews with a vivid thin green vein running vertically down the center, broad ridged organic stalk material, seamless vertically. Opaque atlas, no checkerboard, no cast shadows, no metal, no machinery, no frames around quadrants, no teeth or mouth. Match rich purple flesh and luminous chartreuse palette. High-quality polished stylized game art, clear readable medium-scale detail.

## Exact membrane refinement prompt

Use case: stylized-concept. Asset type: seamless transparent game texture, thin living membrane veins for a Minecraft biological reservoir. Create a square flat orthographic texture on a TRUE TRANSPARENT BACKGROUND (alpha). Sparse branching dark burgundy-purple capillary veins with a few delicate mauve translucent connective threads. LARGE clear empty areas between veins: at least 80 percent of the area is transparent. Three or four broad winding main veins branch into thinner tributaries, natural organic asymmetry. Seamless tile edges. No solid pink background, no glass rectangle, no borders, no checkerboard, no shadows, no frame, no text, no object or tank illustration. All negative space must be genuine alpha transparency, not white. Readable painted detail that holds up downsampled to 64x64. Purple organic vascular filaments only, suitable for overlay on luminous green biomass.
