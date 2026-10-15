# Connected reservoir membrane

Built-in image generation created `reservoir-membrane-source.png`. `java tools/ExportBiologicalAssets.java` resamples it to the 64×64 RGBA runtime asset `textures/block/reservoir_membrane.png`. Geometry renders it only on the outside of a formed cuboid. The 12 outer frame edges replace the old repeated cell lattice; internal liquid faces are culled without hiding a liquid surface below an empty cell.

Exact prompt:

> Use case: stylized-concept. Asset type: transparent tileable texture for the transparent outer membrane of a living biomass storage multiblock in Minecraft. Flat square face-on material, no perspective. Almost completely clear glass-like biological membrane on a genuinely transparent background, only two very faint thin pale mint curved organic reflection streaks near the center, delicate translucent film, restrained clean game art. No border or frame: this must tile across several blocks without a visible grid. No bubbles, no cells, no veins, no opaque fill, no objects, no checkerboard, no text. Preserve true transparency. The membrane should be barely visible so liquid behind it remains clearly visible.
