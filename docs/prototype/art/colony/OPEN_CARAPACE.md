# Open carapace material study — 2026-09-07

Generated with the built-in image generation tool. Original source:
`open-carapace-materials.png` (source PNG kept in the V3 prototype repository under `art/colony/`).
No vanilla or third-party armor textures were used.

## Exact generation prompt

Use case: stylized-concept. Asset type: original game armor material atlas, NOT a character illustration. Create a square texture sheet divided into four equal square quadrants with NO gutters or lettering. Top left: neutral pearl-white insect chitin with broad overlapping curved growth striations, delicate charcoal edge shading, soft nacre highlights, attractive smooth detail rather than speckled noise. Top right: deep charcoal teal flexible tendon with orderly curved ribbing, nearly black recesses. Bottom left: living pale mint-green luminous membrane with fine branching veins and gentle bright nodes, dark teal undertones. Bottom right: warm ivory horn with layered curved growth rings. All four quadrants fill their region entirely as flat top-down material surfaces, diffuse illumination, no perspective, no objects, no armor figure, no cast shadows, no frames, no border, no text, no logos. Designed for UV mapping onto small open segmented biological Minecraft armor at 512x256 atlas resolution; large readable material patterns with refined smooth edges, original biological aesthetic, not metal, no familiar franchise designs.

## Runtime export

`java tools/ExportArmorAssets.java` resamples and packs the original four material
regions into 512×256 atlases. Shell pixels belong to the dyeable layer; tendon,
organ and horn pixels belong to the natural-color overlay. Their alpha coverage
is disjoint, avoiding coplanar opaque color passes. All four branches share this
material language, with different anatomy and undyed shell colors.

The exporter also supplies private inventory materials and shell tint indices.
It does not modify colony block, reservoir, drone or vein textures. The broader
legacy colony exporter delegates these armor outputs to this tool after generating
its inventory geometry. Material source and runtime files are committed together.
