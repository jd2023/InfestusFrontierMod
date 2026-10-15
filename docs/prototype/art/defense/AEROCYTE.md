# Aerocyte Bloom material source

Generated with the built-in imagegen tool. Original source: `aerocyte-source.png`.
`tools/ExportDefenseAssets.java` mechanically downsamples it into a 128×128 atlas,
with four 64×64 materials, and maps chitin, muscle, spore membrane and roots to
their corresponding anatomy. No vanilla material art is used. Inventory and world
render the same animated geometry through the block-entity renderer.

## Exact generation prompt

```text
Use case: stylized-concept
Asset type: original hand-painted game texture atlas for a Minecraft biological air-defense organ; this is material artwork to map onto an animated 3D model, NOT a picture of the model.
Primary request: a square, completely filled, opaque texture atlas with four equal square quadrants and NO margins, NO gutters, NO labels. Upper-left quadrant: warm ivory chitin, broad overlapping curved shell growth bands with rose-brown seams; upper-right: deep wine-red living muscle with broad longitudinal fibers and soft fleshy folds; lower-left: pale mint-green spore-sac membrane, translucent-looking luminous lobules with darker teal capillaries (image itself opaque); lower-right: dark burgundy root cuticle with broad sinuous branching ridges and sparse ivory growth scars. The quadrants will be cropped individually and used as separate model materials.
Style: exquisite stylized hand-painted biological fantasy, readable broad features after downsampling to 64x64, smooth shading, sparse fine detail, rich but restrained contrast. Flat orthographic material studies, soft neutral illumination, no cast shadows across quadrants.
Constraints: each quadrant filled edge-to-edge with only its material; no rendered creature, no turret, no scenery, no frames, no metal or mechanical parts, no recognizable franchise design, no text, no logos, no watermark. Square composition.
```
