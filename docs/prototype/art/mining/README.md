# Mining organ material study

Generated with the built-in image-generation tool; source: `materials.png`.

Prompt:

> Use case: stylized-concept. Asset type: original Minecraft biological mining-organ material atlas, square image. A production-ready 2 by 2 texture atlas of exactly four equal square material tiles. Top left: smooth ivory insect carapace with broad amber growth bands and restrained mineral scratches. Top right: deep wine-purple living muscle with broad flowing parallel fibers. Bottom left: luminous acid-green moist biological membrane with large readable translucent lobules. Bottom right: smoky mauve mineralized chitin with broad layered ridges. Orthographic flat surface textures only, all tiles completely filled, no objects, perspective, shadows cast between tiles or margins. Deliberately readable at 64x64 per tile, polished hand-painted game material style, organic not mechanical, seamless edges within each tile where possible. Four equal quadrants aligned exactly at image center. No labels, no text, no dividers, no transparency, no logos. This is original alien biology, no recognizable franchise symbols or designs.

`java tools/ExportMiningAssets.java` crops/downsamples the four quadrants into dedicated 64-pixel materials and exports bounded native block models. Existing armor, reservoirs and vein art are untouched.
