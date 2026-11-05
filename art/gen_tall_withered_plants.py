"""The tall dead plants: six textures, their models and the two blockstates.

    python3 art/gen_tall_withered_plants.py

Substrate growth kills a sunflower, lilac, rose bush, peony, tall grass or large fern into a two-block plant
(docs/design/VEGETATION_INFECTION.md, "Shipped: tall plants"). Three shapes cover all six: one dry tall-flower stalk
with a spent seed head for the four flowers, one dry tall-grass tuft and one large-fern clump. Each is drawn on one
64x128 grey canvas, so the stalk runs unbroken across the seam, and cut into a lower and an upper 64x64 cutout. The
block colour tints it by the lower half's position (ash, bone, brown, plum for flowers; straw, bone, ash, dry brown for
undergrowth), and the blockstate turns it a quarter turn by the same seed, so both halves always agree.
The image-model sources and export commands are recorded in docs/sprints/ART.md.
This generator preserves the finished textures.
"""
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw

import gen_withered_flower as small
from gen_withered_flower import EDGE, HEAD, KNOT, MODELS, NS, PETAL, POD, STALK, STATES, TEXTURES, blob, grey, stroke

WIDTH = 64
HEIGHT = 128

FLOWER = 'tall_withered_flower'
UNDERGROWTH = 'tall_blighted_undergrowth'
FLOWER_SOURCES = ['sunflower', 'lilac', 'rose_bush', 'peony']
UNDERGROWTH_SOURCES = ['tall_grass', 'large_fern']
# texture family of each undergrowth source
UNDERGROWTH_SHAPE = {'tall_grass': 'tall_blighted_grass', 'large_fern': 'tall_blighted_fern'}
SHAPES = [FLOWER, 'tall_blighted_grass', 'tall_blighted_fern']
HALVES = ['lower', 'upper']


def curve(p0, p1, p2, steps=14):
    """Points along the quadratic Bezier p0-p1-p2."""
    out = []
    for i in range(steps + 1):
        t = i / steps
        out.append((
            round((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]),
            round((1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]),
        ))
    return out


def flower(draw):
    """A dry stalk across both blocks, two limp leaves, a forked side stem and a heavy spent head bowed at the top."""
    stalk = [(32, 127), (31, 112), (33, 94), (31, 76), (32, 58), (31, 40), (35, 26), (43, 20)]
    stroke(draw, stalk, STALK, 3)
    stroke(draw, [(31, 108), (20, 114), (12, 124)], STALK, 2)  # limp leaves
    stroke(draw, [(33, 90), (44, 98), (50, 110)], STALK, 2)
    stroke(draw, [(31, 70), (24, 62), (18, 52)], STALK, 2)  # a forked side stem with its own small pod
    blob(draw, (11, 40, 22, 54), POD)
    draw.line([(16, 42), (16, 52)], fill=grey(POD - 60), width=1)
    blob(draw, (40, 14, 60, 36), HEAD)  # the spent head hangs under the bend
    blob(draw, (45, 19, 55, 29), HEAD - 70)  # a dark seed disc
    for dx, dy in ((47, 22), (52, 22), (49, 26), (53, 27), (46, 27)):
        draw.point((dx, dy), fill=grey(HEAD - 20))
    blob(draw, (28, 92, 34, 98), KNOT)
    small.roots(draw, 32, bottom=127)


def grass(draw):
    """Seven dry blades, the tallest across both blocks, tips limp or snapped, a dull knot at the foot."""
    blades = [
        ((30, 127), (26, 70), (14, 20), STALK, 4),
        ((33, 127), (38, 60), (50, 14), STALK, 4),
        ((32, 127), (31, 80), (33, 6), STALK + 14, 4),
        ((28, 127), (14, 100), (6, 64), STALK, 3),
        ((36, 127), (52, 98), (58, 62), STALK, 3),
        ((31, 127), (22, 108), (22, 84), STALK + 20, 3),  # a snapped blade
        ((34, 127), (42, 104), (40, 90), STALK + 20, 3),
    ]
    for start, mid, tip, value, width in blades:
        stroke(draw, curve(start, mid, tip, 18), value, width)
    stroke(draw, curve((33, 6), (38, 2), (44, 10), 6), STALK + 14, 2)  # the tallest blade bows over
    blob(draw, (28, 112, 34, 118), KNOT)
    small.roots(draw, 32, bottom=127)


def fern(draw):
    """Three arching fronds with paired dry pinnae, curled tips, a dull knot where they meet."""
    fronds = [
        ((32, 127), (30, 70), (12, 30), PETAL),
        ((33, 127), (40, 56), (54, 22), PETAL),
        ((32, 127), (32, 74), (36, 6), PETAL + 10),
    ]
    for start, mid, tip, value in fronds:
        rachis = curve(start, mid, tip, 20)
        stroke(draw, rachis, STALK, 3)
        for i in range(5, 19, 2):
            x, y = rachis[i]
            reach = 11 - (i - 5) * 0.5
            nx, ny = rachis[i + 1][0] - rachis[i - 1][0], rachis[i + 1][1] - rachis[i - 1][1]
            length = math.hypot(nx, ny) or 1.0
            px, py = -ny / length, nx / length
            for side in (-1, 1):
                end = (round(x + side * px * reach), round(y + side * py * reach + 3))  # pinnae droop a little
                stroke(draw, [(x, y), end], value, 1)
        tx, ty = rachis[-1]
        draw.arc((tx - 4, ty - 4, tx + 4, ty + 4), 40, 330, fill=grey(EDGE), width=2)  # the curled tip
    blob(draw, (29, 108, 35, 114), KNOT)
    small.roots(draw, 32, bottom=127)


PAINT = {FLOWER: flower, 'tall_blighted_grass': grass, 'tall_blighted_fern': fern}


def write_textures():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    for name, paint in PAINT.items():
        image = Image.new('RGBA', (WIDTH, HEIGHT), (0, 0, 0, 0))
        paint(ImageDraw.Draw(image))
        image.crop((0, HEIGHT // 2, WIDTH, HEIGHT)).save(TEXTURES / f'{name}_lower.png')
        image.crop((0, 0, WIDTH, HEIGHT // 2)).save(TEXTURES / f'{name}_upper.png')


def write_models():
    MODELS.mkdir(parents=True, exist_ok=True)
    for name in SHAPES:
        for half in HALVES:
            model = {
                'parent': 'minecraft:block/tinted_cross',
                'render_type': 'minecraft:cutout',
                'textures': {'cross': NS + f'{name}_{half}'},
            }
            (MODELS / f'{name}_{half}.json').write_text(json.dumps(model, indent=2) + '\n')


def looks(shape, half):
    """Four quarter turns of one half's model. Vanilla seeds both halves from the lower position: same turn."""
    out = []
    for turn in (0, 90, 180, 270):
        look = {'model': NS + f'{shape}_{half}'}
        if turn:
            look['y'] = turn
        out.append(look)
    return out


def write_blockstates():
    STATES.mkdir(parents=True, exist_ok=True)
    for block, sources, shape_of in (
        (FLOWER, FLOWER_SOURCES, lambda source: FLOWER),
        (UNDERGROWTH, UNDERGROWTH_SOURCES, UNDERGROWTH_SHAPE.get),
    ):
        variants = {
            f'half={half},source={source}': looks(shape_of(source), half) for source in sources for half in HALVES
        }
        (STATES / f'{block}.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')


def main():
    # Finished image-model textures are maintained independently.
    write_models()
    write_blockstates()


if __name__ == '__main__':
    main()
