#!/usr/bin/env python3
"""The Mineral Tailings' tint layer, taken from the shipped icon.

Run from the repository root after exporting textures/item/mineral_tailings.png from its image-model source:

    python3 art/derive_tailings_flecks.py

Writes textures/item/mineral_tailings_flecks.png: the icon's own bright, unsaturated mineral flecks as grey on clear.
The item model draws it as layer 1, and a client colour handler (`MineralModule`) tints it with the metal of the
stack's `parcel`, so the flecks of one clod read iron, gold or copper while the clod itself stays the image-model
artwork. The layer is cut from the icon's pixels, so it always lies on the silhouette and the flecks it tints.
"""
from pathlib import Path

from PIL import Image

ITEM = Path('src/main/resources/assets/infestusfrontier/textures/item')
FLECK_SHARE = 0.12  # the brightest share of the clod's pixels, if they are also nearly grey


def main():
    icon = Image.open(ITEM / 'mineral_tailings.png').convert('RGBA')
    pixels = icon.load()
    opaque = [(x, y) for y in range(icon.height) for x in range(icon.width) if pixels[x, y][3] > 200]
    luma = {p: 0.299 * pixels[p][0] + 0.587 * pixels[p][1] + 0.114 * pixels[p][2] for p in opaque}
    cut = sorted(luma.values())[int(len(opaque) * (1 - FLECK_SHARE))]
    flecks = Image.new('RGBA', icon.size, (0, 0, 0, 0))
    out = flecks.load()
    for p in opaque:
        r, g, b, _ = pixels[p]
        if luma[p] >= cut and max(r, g, b) - min(r, g, b) < 60:
            v = min(255, int(luma[p] * 1.15))
            out[p] = (v, v, v, 255)
    flecks.save(ITEM / 'mineral_tailings_flecks.png', optimize=True)
    print('flecks', sum(1 for p in opaque if out[p][3]), 'of', len(opaque), 'pixels')


if __name__ == '__main__':
    main()
