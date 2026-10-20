"""The vein cells' textures: the lumen floors of the three carriers and the groove wall, 64x64 each.

    python3 art/gen_veins.py

The cell's skin is the mature substrate tile; the block models under models/block/vein_* carve the groove.
"""
import math
import random
from pathlib import Path
from PIL import Image

OUT = Path('src/main/resources/assets/infestusfrontier/textures/block')
SIZE = 64
GREEN = [(30, 72, 40), (58, 128, 64), (96, 190, 90), (150, 236, 130), (214, 255, 190)]
WATER = [(22, 52, 112), (40, 96, 182), (84, 150, 226), (150, 206, 250), (214, 240, 255)]
MUSCLE = [(110, 48, 64), (150, 70, 84), (186, 100, 108), (214, 140, 140), (236, 186, 176)]
DRY = [(90, 74, 68), (122, 104, 92), (150, 132, 116), (178, 162, 144), (200, 188, 170)]
WALL = [(40, 20, 30), (62, 30, 42), (86, 44, 56), (112, 62, 72)]


def ramp(colours, v):
    i = max(0, min(len(colours) - 1, int(v * len(colours))))
    return colours[i]


def lumen(name, colours, seed, ripple):
    rng = random.Random(seed)
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            v = 0.55 + 0.25 * math.sin((x + 3 * y) / 9.0) * ripple + 0.12 * math.sin((y - x) / 5.0) + 0.08 * (rng.random() - 0.5)
            v += 0.15 * (1 - abs(x - 31.5) / 32)  # a soft highlight along the middle of the channel
            img.putpixel((x, y), ramp(colours, v) + (255,))
    img.save(OUT / (name + '.png'), optimize=True)


def wall(seed):
    rng = random.Random(seed)
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            v = 0.5 - 0.3 * (y / SIZE) + 0.1 * math.sin(x / 4.0 + y / 7.0) + 0.1 * (rng.random() - 0.5)
            img.putpixel((x, y), ramp(WALL, v) + (255,))
    img.save(OUT / 'vein_wall.png', optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    lumen('vein_lumen_biomass', GREEN, 1, 1.0)
    lumen('vein_lumen_fluid', WATER, 2, 1.0)
    lumen('vein_lumen_item', MUSCLE, 3, 0.4)
    lumen('vein_lumen_dry', DRY, 4, 0.2)
    wall(5)
    print('veins: 5 textures')


if __name__ == '__main__':
    main()
