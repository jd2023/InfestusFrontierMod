"""The dead shrub families: Dead Berry Bush, Blighted Dead Bush, Blighted Undergrowth and Withered Sapling.

    python3 art/gen_withered_shrubs.py

A few shared shapes stand in for every shrub, grass, fern and sapling substrate growth kills
(docs/design/VEGETATION_INFECTION.md, "Visual variety with few assets"). Each is a grey cutout on a transparent 64x64
tile exported from the image-model sources recorded in docs/sprints/ART.md; the block colour tints it by position (or, for a
sapling, by species), so the textures carry shape and shading only. Models are the vanilla tinted cross. The
blockstates list the shapes turned four ways and let vanilla pick one by the position's seed, which never changes: no
saved appearance, no block entity. Dark root threads at the foot and one dull knot are the only signs of infection.

    dead berry bush: a bare low sprout (age 0 and 1), a bare twiggy bush (age 2), the same with dried berries (age 3)
    blighted dead bush: two tangles of snapped twigs
    blighted undergrowth: two tufts of dry blades, one curled fern frond
    withered sapling: a bare stem with limp leaves, a bent stem with curled leaves
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

from gen_withered_flower import BOOK, EDGE, KNOT, MODELS, NS, SIZE, STALK, STATES, TEXTURES, blob, grey, roots, stroke

TWIG = 150
LEAF = 205
BERRY = 120


def berry_small(draw):
    stroke(draw, [(32, 63), (31, 52), (28, 42)], STALK, 2)
    stroke(draw, [(31, 52), (37, 46), (40, 38)], STALK, 2)
    stroke(draw, [(31, 56), (24, 52), (20, 46)], STALK, 2)
    blob(draw, (36, 52, 40, 56), KNOT)
    roots(draw, 32)


def berry_large(draw):
    stroke(draw, [(32, 63), (31, 46), (30, 30), (26, 14)], STALK, 3)
    stroke(draw, [(31, 46), (40, 38), (46, 24)], TWIG, 2)
    stroke(draw, [(31, 50), (22, 42), (14, 32)], TWIG, 2)
    stroke(draw, [(30, 32), (36, 24), (38, 12)], TWIG, 2)
    stroke(draw, [(40, 38), (50, 36), (56, 30)], TWIG, 2)
    stroke(draw, [(22, 42), (14, 46), (8, 44)], TWIG, 2)
    blob(draw, (29, 38, 35, 44), KNOT)
    roots(draw, 32)


def berry_fruited(draw):
    berry_large(draw)
    for x, y in ((46, 24), (14, 32), (38, 12), (56, 30), (26, 14), (8, 44)):
        blob(draw, (x - 3, y, x + 3, y + 6), BERRY)  # dried berries still hanging, shrivelled and dull
    for x, y in ((40, 38), (22, 42)):
        blob(draw, (x - 2, y + 2, x + 2, y + 6), BERRY)


def dead_bush_a(draw):
    stroke(draw, [(32, 63), (32, 48), (30, 36)], STALK, 3)
    stroke(draw, [(30, 36), (22, 28), (14, 14)], TWIG, 2)
    stroke(draw, [(30, 36), (34, 24), (30, 8)], TWIG, 2)
    stroke(draw, [(32, 48), (42, 40), (52, 30)], TWIG, 2)
    stroke(draw, [(22, 28), (12, 28), (6, 22)], TWIG, 1)
    stroke(draw, [(34, 24), (44, 18), (48, 8)], TWIG, 1)
    blob(draw, (29, 44, 35, 50), KNOT)
    roots(draw, 32)


def dead_bush_b(draw):
    stroke(draw, [(28, 63), (28, 50), (24, 40)], STALK, 3)
    stroke(draw, [(36, 63), (38, 50), (42, 38)], STALK, 3)
    stroke(draw, [(24, 40), (14, 34), (8, 22)], TWIG, 2)
    stroke(draw, [(24, 40), (26, 26), (20, 12)], TWIG, 2)
    stroke(draw, [(42, 38), (50, 28), (54, 14)], TWIG, 2)
    stroke(draw, [(42, 38), (38, 24), (42, 10)], TWIG, 2)
    stroke(draw, [(26, 26), (34, 22), (36, 14)], TWIG, 1)
    blob(draw, (31, 54, 36, 59), KNOT)
    roots(draw, 32)


def blades_a(draw):
    for base, mid, tip in (((26, 63), (24, 44), (14, 22)), ((32, 63), (32, 40), (40, 14)),
                           ((38, 63), (42, 46), (52, 30)), ((30, 63), (28, 50), (22, 42))):
        stroke(draw, [base, mid, tip], LEAF, 2)
    stroke(draw, [(34, 63), (36, 52), (34, 46)], LEAF - 40, 2)  # a snapped blade
    roots(draw, 32)


def blades_b(draw):
    for base, mid, tip in (((22, 63), (20, 48), (10, 34)), ((28, 63), (28, 40), (24, 16)),
                           ((36, 63), (38, 44), (46, 22)), ((42, 63), (46, 50), (56, 44))):
        stroke(draw, [base, mid, tip], LEAF - 20, 2)
    stroke(draw, [(32, 63), (31, 54)], LEAF - 50, 2)
    blob(draw, (30, 56, 34, 60), KNOT)
    roots(draw, 32)


def frond(draw):
    for tip, bow in (((10, 24), (16, 34)), ((32, 10), (30, 30)), ((54, 26), (48, 36))):
        stroke(draw, [(32, 63), bow, tip], STALK, 2)
        for t in (0.35, 0.55, 0.75):
            x = int(32 + (tip[0] - 32) * t)
            y = int(63 + (tip[1] - 63) * t)
            stroke(draw, [(x, y), (x - 5, y + 5)], LEAF - 20, 1)
            stroke(draw, [(x, y), (x + 5, y + 5)], LEAF - 20, 1)
        stroke(draw, [tip, (tip[0] + 3, tip[1] + 5)], LEAF, 1)  # the curled tip
    blob(draw, (29, 56, 35, 62), KNOT)
    roots(draw, 32)


def sapling_bare(draw):
    stroke(draw, [(32, 63), (32, 46), (31, 30)], STALK, 3)
    stroke(draw, [(32, 46), (24, 40), (20, 46)], LEAF, 2)  # limp leaves
    stroke(draw, [(32, 40), (40, 34), (44, 40)], LEAF, 2)
    stroke(draw, [(31, 30), (26, 24), (22, 28)], LEAF - 20, 2)
    blob(draw, (29, 34, 34, 39), KNOT)
    roots(draw, 32)


def sapling_curled(draw):
    stroke(draw, [(32, 63), (33, 48), (38, 36), (34, 24)], STALK, 3)
    blob(draw, (22, 30, 34, 42), LEAF - 30)  # curled, dried leaves
    blob(draw, (36, 40, 48, 50), LEAF - 40)
    blob(draw, (28, 16, 40, 26), LEAF - 10)
    draw.line([(24, 36), (30, 34)], fill=grey(EDGE), width=1)
    roots(draw, 32)


SHAPES = {
    'dead_berry_bush_small': berry_small,
    'dead_berry_bush_large': berry_large,
    'dead_berry_bush_fruited': berry_fruited,
    'blighted_dead_bush_a': dead_bush_a,
    'blighted_dead_bush_b': dead_bush_b,
    'blighted_grass_a': blades_a,
    'blighted_grass_b': blades_b,
    'blighted_fern': frond,
    'withered_sapling_bare': sapling_bare,
    'withered_sapling_curled': sapling_curled,
}


def write_models():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    MODELS.mkdir(parents=True, exist_ok=True)
    for name in SHAPES:
        # Finished image-model textures are maintained independently.
        model = {
            'parent': 'minecraft:block/tinted_cross',
            'render_type': 'minecraft:cutout',
            'textures': {'cross': NS + name},
        }
        (MODELS / f'{name}.json').write_text(json.dumps(model, indent=2) + '\n')


def turned(*names):
    looks = []
    for name in names:
        for turn in (0, 90, 180, 270):
            look = {'model': NS + name}
            if turn:
                look['y'] = turn
            looks.append(look)
    return looks


def write_blockstates():
    STATES.mkdir(parents=True, exist_ok=True)
    states = {
        'dead_berry_bush': {
            'age=0': turned('dead_berry_bush_small'),
            'age=1': turned('dead_berry_bush_small'),
            'age=2': turned('dead_berry_bush_large'),
            'age=3': turned('dead_berry_bush_fruited'),
        },
        'blighted_dead_bush': {'': turned('blighted_dead_bush_a', 'blighted_dead_bush_b')},
        'blighted_undergrowth': {
            'source=short_grass': turned('blighted_grass_a', 'blighted_grass_b'),
            'source=fern': turned('blighted_fern'),
        },
        'withered_sapling': {
            f'source={species}': turned('withered_sapling_bare', 'withered_sapling_curled')
            for species in ('oak', 'birch', 'spruce', 'jungle', 'acacia', 'dark_oak')
        },
    }
    for block, variants in states.items():
        (STATES / f'{block}.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')


def write_icon():
    """The guide entry's icon: the fruited bush in plum, 16x16, since the blocks have no item to show."""
    tint = (0x76, 0x4C, 0x62)
    source = Image.open(TEXTURES / 'dead_berry_bush_fruited.png').convert('RGBA')
    tinted = Image.new('RGBA', source.size, (0, 0, 0, 0))
    for x in range(SIZE):
        for y in range(SIZE):
            r, g, b, a = source.getpixel((x, y))
            if a:
                tinted.putpixel((x, y), (r * tint[0] // 255, g * tint[1] // 255, b * tint[2] // 255, a))
    BOOK.mkdir(parents=True, exist_ok=True)
    tinted.resize((16, 16), Image.Resampling.BOX).save(BOOK / 'withered_shrubs.png')


def main():
    write_models()
    write_blockstates()
    write_icon()


if __name__ == '__main__':
    main()
