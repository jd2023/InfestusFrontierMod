#!/usr/bin/env python3
"""The biomass fluid's two animated textures, from one description.

Run from the repository root: python3 art/gen_biomass_fluid.py
Deterministic: fixed seed, no timestamps. Writes
  src/main/resources/assets/infestusfrontier/textures/block/biomass_still.png  (+ .mcmeta)  32 x 32, 16 frames
  src/main/resources/assets/infestusfrontier/textures/block/biomass_flow.png   (+ .mcmeta)  64 x 64, 16 frames

Thick green biomass: a dark olive body, slow pale blobs under the surface, a few bright specks. Every frame
tiles with its neighbours and the last frame leads back to the first. The flow sheet is the same material at
twice the size, drifting down the way vanilla flow textures do.
"""
import json
import math
import random
from pathlib import Path
from PIL import Image

OUT = Path('src/main/resources/assets/infestusfrontier/textures/block')
FRAMES = 16
# dark to light
BODY = [(28, 52, 30), (38, 72, 38), (52, 96, 46), (74, 128, 56), (108, 168, 72), (160, 214, 110)]
SPECK = (214, 250, 160)


def lattice(seed, n, frames):
    rng = random.Random(seed)
    return [[[rng.random() for _ in range(n)] for _ in range(n)] for _ in range(frames)]


def smooth(t):
    return t * t * (3 - 2 * t)


def noise(grid, x, y, f):
    """Periodic value noise in space and in time; x, y in cells, f in frames."""
    frames, n = len(grid), len(grid[0])
    ix, iy, i_f = int(math.floor(x)), int(math.floor(y)), int(math.floor(f))
    fx, fy, ff = smooth(x - ix), smooth(y - iy), smooth(f - i_f)
    value = 0.0
    for dz, wz in ((0, 1 - ff), (1, ff)):
        g = grid[(i_f + dz) % frames]
        for dy, wy in ((0, 1 - fy), (1, fy)):
            for dx, wx in ((0, 1 - fx), (1, fx)):
                value += g[(iy + dy) % n][(ix + dx) % n] * wx * wy * wz
    return value


def frame(size, index, drift, seed):
    big, small, specks = lattice(seed, 4, FRAMES), lattice(seed + 1, 8, FRAMES), lattice(seed + 2, 16, FRAMES)
    img = Image.new('RGBA', (size, size))
    for y in range(size):
        for x in range(size):
            u, v = x / size, (y / size + drift * index / FRAMES) % 1.0
            t = index
            a = noise(big, u * 4, v * 4, t * 4 / FRAMES * 4)
            b = noise(small, u * 8, v * 8, t * 8 / FRAMES * 8)
            value = 0.25 + 0.5 * a + 0.3 * (b - 0.5)
            shade = BODY[max(0, min(len(BODY) - 1, int(value * len(BODY))))]
            if noise(specks, u * 16, v * 16, t * 16 / FRAMES * 16) > 0.93 and b > 0.55:
                shade = SPECK
            img.putpixel((x, y), shade + (255,))
    return img


def sheet(name, size, drift, seed, frametime):
    strip = Image.new('RGBA', (size, size * FRAMES))
    for i in range(FRAMES):
        strip.paste(frame(size, i, drift, seed), (0, i * size))
    OUT.mkdir(parents=True, exist_ok=True)
    strip.save(OUT / (name + '.png'), optimize=True)
    (OUT / (name + '.png.mcmeta')).write_text(json.dumps({'animation': {'frametime': frametime}}) + '\n')


def main():
    sheet('biomass_still', 32, 0.0, 7, 4)
    sheet('biomass_flow', 64, 1.0, 7, 3)


if __name__ == '__main__':
    main()
