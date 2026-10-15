# Colony experiment artwork

Original material generated with the built-in image-generation tool; no vanilla or other mod texture source. Selected source: `chitin-source.png` (128×128 downsample). Runtime shell: `src/main/resources/assets/infestusfrontier/textures/block/chitin_shell.png`. Armor UV atlases are in `textures/models/armor/`. `java tools/ExportColonyAssets.java` reproducibly exports the material, branch palette variants, and original code-authored geometry. This is a material/UV experiment, not a final hand-painted armor skin.

The export also derives ten colored inventory-trim materials from the same source, with original 3D band geometry. Worn armor intentionally uses Minecraft's native trim/glint layer as a compatibility feature; its underlying armor art is original. Source Java was formatted with [google-java-format 1.24.0](https://github.com/google/google-java-format/releases/tag/v1.24.0).

Exact generation prompt:

> Use case: stylized-concept. Asset type: tileable game material for an original biological armor, helper creatures and living conduits in Minecraft. A square flat texture sheet completely filled edge to edge with broad layered beetle-like chitin plates: warm ivory ridges, muted plum-purple shell, deep dark violet recesses, sparse luminous turquoise thin veins. Stylized hand-painted game art, large readable shapes suitable for downsampling to 128x128. Orthographic material only, even illumination, no object silhouette, no perspective, no shadows cast outside plates, no text, no border, no existing game characters or logos. Seamless repeating all four edges. Beautiful clean carved biological armor material, not gore, no tiny noisy grain.
