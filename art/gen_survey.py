"""Surveyed Tissue and the Structure Grower: temporary functional art. Surveyed Tissue is a full, flush block of
pale living tissue over packed earth; its top carries a dark chevron that points the way the section runs (north in
the model; the blockstate turns it) and a row of pale survey dots. The grower is a squat bark-brown body on a broad
foot with three pale spinnerets on top. Its boxes match StructureGrowerBlock.BOXES.

    python3 art/gen_survey.py
"""
import json
import random
from PIL import Image
from organ_model import ASSETS, Painter, ball, faces, folds, islands, pack, skin, write_static

SHEET = 64
TISSUE = [(108, 120, 78), (132, 146, 96), (156, 170, 116), (182, 194, 140)]
SOIL = [(46, 36, 28), (62, 48, 36), (78, 62, 46), (96, 78, 58)]
MARK = (52, 30, 34)
DOT = (232, 220, 186)
BARK = [(44, 30, 22), (66, 46, 32), (92, 64, 44), (120, 86, 58), (150, 112, 76), (178, 140, 98)]
FOOT = [(36, 28, 22), (54, 42, 32), (74, 58, 42), (96, 76, 54)]
PALE = [(150, 108, 98), (190, 146, 130), (222, 184, 164), (242, 216, 196)]


def box(island, x1, y1, z1, x2, y2, z2):
    """A cube from Java block pixels (north is z = 0) to the model's coordinates (-8..8 on x and z)."""
    return (island, [x1 - 8, y1, z1 - 8], [x2 - x1, y2 - y1, z2 - z1], None)


GROWER = [('grower', None, [0, 8, 0], [
    box('foot', 1, 0, 1, 15, 2, 15),
    box('body', 3, 2, 3, 13, 10, 13),
    box('spinneret', 4, 10, 4, 7, 14, 7),
    box('spinneret_low', 9, 10, 5, 12, 13, 8),
    box('spinneret_tall', 6, 10, 9, 9, 15, 12),
])]


def tile(ramp, seed, speckle):
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            shade = min(len(ramp) - 1, int(rng.random() * 2.2 + (1 if (x * 5 + y * 3) % speckle == 0 else 0)))
            img.putpixel((x, y), ramp[shade] + (255,))
    return img


def top():
    """Tissue with a chevron pointing north (towards y = 0) and survey dots along the way it points."""
    img = tile(TISSUE, 6101, 7)
    for row in range(5):  # the chevron: two arms meeting at the north
        y = 4 + row
        for dx in (0, 1):
            img.putpixel((7 - row - dx + 1, y), MARK + (255,))
            img.putpixel((8 + row + dx - 1, y), MARK + (255,))
    for y in (11, 13, 15):  # survey dots running back from the chevron
        img.putpixel((7, y), DOT + (255,))
        img.putpixel((8, y), DOT + (255,))
    img.putpixel((7, 1), DOT + (255,))
    img.putpixel((8, 1), DOT + (255,))
    return img


def side():
    """Packed earth with a band of tissue along the top edge."""
    img = tile(SOIL, 6203, 11)
    rng = random.Random(6204)
    for x in range(16):
        depth = 3 + (1 if rng.random() < 0.4 else 0)
        for y in range(depth):
            img.putpixel((x, y), TISSUE[min(3, 1 + (y == 0) + (rng.random() < 0.3))] + (255,))
    return img


def marker():
    out = ASSETS / 'textures/block'
    top().save(out / 'surveyed_tissue_top.png', optimize=True)
    side().save(out / 'surveyed_tissue_side.png', optimize=True)
    model = {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
        'top': 'infestusfrontier:block/surveyed_tissue_top', 'bottom': 'infestusfrontier:block/root_cell_side',
        'side': 'infestusfrontier:block/surveyed_tissue_side', 'particle': 'infestusfrontier:block/surveyed_tissue_side'}}
    (ASSETS / 'models/block/surveyed_tissue.json').write_text(json.dumps(model, indent=2) + '\n')
    variants = {f'facing={facing}': {'model': 'infestusfrontier:block/surveyed_tissue', **({'y': y} if y else {})}
                for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}
    (ASSETS / 'blockstates/surveyed_tissue.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')
    (ASSETS / 'models/item/surveyed_tissue.json').write_text(
        json.dumps({'parent': 'infestusfrontier:block/surveyed_tissue'}, indent=2) + '\n')


def grower():
    p = Painter(SHEET, 6311)
    uv = pack(GROWER, SHEET)
    for island, size in islands(GROWER).items():
        for face, rect in faces(size, uv[island]).items():
            light = 0.14 if face == 'up' else -0.16 if face == 'down' else 0
            if island == 'foot':
                p.ramp(rect, FOOT, lambda x, y, w, h: skin(x, y, w, h) - 0.05 + light, grain=0.08, mottle=0.25)
            elif island.startswith('spinneret'):
                p.ramp(rect, PALE, lambda x, y, w, h: 0.5 * ball(x, y, w, h) + 0.5 * skin(x, y, w, h) + light,
                       grain=0.05, mottle=0.2)
            else:
                p.ramp(rect, BARK, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + light, grain=0.06,
                       mottle=0.24)
                p.vein(rect, BARK[0], 5, 220)
    write_static('structure_grower', GROWER, SHEET, p)
    (ASSETS / 'blockstates/structure_grower.json').write_text(
        json.dumps({'variants': {'': {'model': 'infestusfrontier:block/structure_grower'}}}, indent=2) + '\n')
    (ASSETS / 'models/item/structure_grower.json').write_text(
        json.dumps({'parent': 'infestusfrontier:block/structure_grower'}, indent=2) + '\n')


def main():
    marker()
    grower()
    print('surveyed tissue: 1 model, 2 textures; structure grower: 1 model, 1 texture; 2 blockstates, 2 item models')


if __name__ == '__main__':
    main()
