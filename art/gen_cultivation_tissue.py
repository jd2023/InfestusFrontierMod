"""Cultivation Tissue top: mature substrate tilled into soft dark furrows with pale root threads, 64x64.

    python3 art/gen_cultivation_tissue.py
"""
import math
import random
from pathlib import Path
from PIL import Image

OUT = Path('src/main/resources/assets/infestusfrontier/textures/block/cultivation_tissue_top.png')
SIZE = 64
SOIL = [(44, 26, 28), (66, 40, 38), (92, 58, 50), (118, 80, 64), (140, 100, 78)]
THREAD = (196, 176, 150)


def ramp(colours, v):
    return colours[max(0, min(len(colours) - 1, int(v * len(colours))))]


def main():
    rng = random.Random(7)
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            furrow = 0.5 + 0.35 * math.sin(y / 16 * 2 * math.pi * 2)
            v = furrow + 0.1 * math.sin(x / 7.0 + y / 11.0) + 0.1 * (rng.random() - 0.5)
            img.putpixel((x, y), ramp(SOIL, v) + (255,))
    for _ in range(18):
        x, y = rng.randrange(SIZE), rng.randrange(SIZE)
        for _ in range(rng.randrange(4, 10)):
            img.putpixel((x % SIZE, y % SIZE), THREAD + (255,))
            x += rng.choice((-1, 0, 1))
            y += 1
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT, optimize=True)
    print('cultivation_tissue_top')


if __name__ == '__main__':
    main()
