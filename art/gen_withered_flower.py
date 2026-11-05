"""The Withered Flower family: models, blockstate and guide icon for three image-model silhouette textures.

    python3 art/gen_withered_flower.py

One block family stands in for every small flower substrate growth kills (docs/design/VEGETATION_INFECTION.md,
"Visual variety with few assets"). Three silhouettes: a bent stalk with a hanging spent head, a forked dry stalk
with small seed pods, and a low collapsed bloom with curled petals. Each is a grey cutout on a transparent 64x64
tile, exported from the image-model sources recorded in docs/sprints/ART.md; the block colour tints it ash, bone, brown or plum
by position, so the textures carry shape and shading only. The blockstate lists the three models turned four ways
and lets vanilla pick one by the position's seed, which never changes: no block state property, no block entity.
A few dark root threads at the foot and one dull knot on the stalk are the only signs of infection.
"""
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path('src/main/resources/assets/infestusfrontier')
TEXTURES = ROOT / 'textures/block'
MODELS = ROOT / 'models/block'
STATES = ROOT / 'blockstates'
BOOK = ROOT / 'textures/book'
NS = 'infestusfrontier:block/'
SIZE = 64

SHAPES = ['hanging', 'forked', 'collapsed']
EDGE = 96  # the dark rim round every part, so the shape reads against grass and substrate
STALK = 168
HEAD = 214
PETAL = 226
POD = 198
KNOT = 118
ROOT_THREAD = 70


def grey(value):
    return (value, value, value, 255)


def stroke(draw, points, value, width):
    """A dark-rimmed line: the rim two pixels wider under the fill."""
    draw.line(points, fill=grey(EDGE), width=width + 2, joint='curve')
    draw.line(points, fill=grey(value), width=width, joint='curve')


def blob(draw, box, value):
    x0, y0, x1, y1 = box
    draw.ellipse((x0 - 1, y0 - 1, x1 + 1, y1 + 1), fill=grey(EDGE))
    draw.ellipse(box, fill=grey(value))


def roots(draw, foot_x, bottom=63):
    """Dark threads creeping out of the ground at the foot of the stalk, ending on row {bottom}."""
    for dx, dy in ((-9, 4), (7, 5), (-3, 7), (11, 2)):
        draw.line(
            [(foot_x, bottom - 3), (foot_x + dx, bottom - dy), (foot_x + dx * 2, bottom)],
            fill=grey(ROOT_THREAD),
            width=1,
        )


def hanging(draw):
    stalk = [(32, 63), (31, 50), (30, 36), (32, 22), (38, 14), (46, 12)]
    stroke(draw, stalk, STALK, 3)
    stroke(draw, [(31, 46), (22, 52), (16, 60)], STALK, 2)  # a limp leaf
    stroke(draw, [(30, 38), (39, 44), (44, 52)], STALK, 2)
    blob(draw, (41, 12, 53, 28), HEAD)  # the spent head hangs under the bend
    draw.ellipse((44, 15, 50, 21), fill=grey(HEAD - 30))
    blob(draw, (28, 40, 34, 45), KNOT)
    roots(draw, 32)


def forked(draw):
    stroke(draw, [(32, 63), (32, 50), (32, 36)], STALK, 3)
    stroke(draw, [(32, 36), (26, 26), (20, 14)], STALK, 2)
    stroke(draw, [(32, 36), (38, 24), (44, 12)], STALK, 2)
    stroke(draw, [(32, 36), (33, 26), (31, 18)], STALK, 2)
    for box in ((16, 8, 24, 18), (40, 6, 48, 16), (27, 12, 35, 22)):
        blob(draw, box, POD)
        x0, y0, x1, y1 = box
        draw.line([(x0 + 4, y0 + 2), (x0 + 4, y1 - 2)], fill=grey(POD - 60), width=1)  # the split seam
    blob(draw, (29, 34, 36, 40), KNOT)
    roots(draw, 32)


def collapsed(draw):
    stroke(draw, [(32, 63), (32, 54), (33, 46)], STALK, 3)
    for tip in ((12, 58), (18, 62), (28, 63), (40, 62), (48, 59), (54, 54)):
        mid = ((33 + tip[0]) // 2, (46 + tip[1]) // 2 - 4)
        stroke(draw, [(33, 46), mid, tip], PETAL, 3)
    blob(draw, (28, 42, 38, 50), HEAD - 40)
    blob(draw, (30, 51, 35, 55), KNOT)
    roots(draw, 32)


def write_textures():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    for name, paint in zip(SHAPES, (hanging, forked, collapsed)):
        image = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
        paint(ImageDraw.Draw(image))
        image.save(TEXTURES / f'withered_flower_{name}.png')


def write_models():
    MODELS.mkdir(parents=True, exist_ok=True)
    for name in SHAPES:
        model = {
            'parent': 'minecraft:block/tinted_cross',
            'render_type': 'minecraft:cutout',
            'textures': {'cross': NS + f'withered_flower_{name}'},
        }
        (MODELS / f'withered_flower_{name}.json').write_text(json.dumps(model, indent=2) + '\n')


def write_blockstate():
    """Every state draws one of twelve looks; the position's seed picks it, so it is stable across reloads."""
    looks = []
    for name in SHAPES:
        for turn in (0, 90, 180, 270):
            look = {'model': NS + f'withered_flower_{name}'}
            if turn:
                look['y'] = turn
            looks.append(look)
    STATES.mkdir(parents=True, exist_ok=True)
    (STATES / 'withered_flower.json').write_text(json.dumps({'variants': {'': looks}}, indent=2) + '\n')


def write_icon():
    """The guide entry's icon: the hanging silhouette in plum, 16x16, since the block has no item to show."""
    tint = (0x76, 0x4C, 0x62)
    source = Image.open(TEXTURES / 'withered_flower_hanging.png').convert('RGBA')
    tinted = Image.new('RGBA', source.size, (0, 0, 0, 0))
    for x in range(SIZE):
        for y in range(SIZE):
            r, g, b, a = source.getpixel((x, y))
            if a:
                tinted.putpixel((x, y), (r * tint[0] // 255, g * tint[1] // 255, b * tint[2] // 255, a))
    BOOK.mkdir(parents=True, exist_ok=True)
    tinted.resize((16, 16), Image.Resampling.BOX).save(BOOK / 'withered_flowers.png')


def main():
    # Finished image-model textures are maintained independently.
    write_models()
    write_blockstate()
    write_icon()


if __name__ == '__main__':
    main()
