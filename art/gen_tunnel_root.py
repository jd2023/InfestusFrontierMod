"""Tunnel Root, Root Cells and the Root Segment. The head is a squat, bark-brown root tip on a broad foot with a pale,
fleshy snout that points the way it cuts (north in the model; the blockstate turns it). A Root Cell is a full block of
packed earth threaded with roots; its top shows the living root running straight along the tunnel or round a bend.
The Root Segment icon is a short knotted root. The head's boxes match the shape in TunnelRootBlock.BOXES.

    python3 art/gen_tunnel_root.py
"""
import json
import math
import random
from PIL import Image
from organ_model import ASSETS, Painter, ball, compact, faces, folds, islands, pack, skin, write_static

SHEET = 64
BARK = [(44, 30, 22), (66, 46, 32), (92, 64, 44), (120, 86, 58), (150, 112, 76), (178, 140, 98)]
FOOT = [(36, 28, 22), (54, 42, 32), (74, 58, 42), (96, 76, 54)]
SNOUT = [(120, 62, 64), (160, 88, 86), (196, 124, 112), (226, 168, 150), (244, 206, 186)]

# (island, origin [x from -8, y, z from -8], size, turn); north is -z
HEAD = [('root', None, [0, 8, 0], [
    ('foot', [-6, 0, -5], [12, 2, 12], None),
    ('body', [-5, 2, -4], [10, 7, 10], None),
    ('crown', [-4, 9, -2], [8, 3, 7], None),
    ('snout', [-3, 2, -7], [6, 5, 3], None),
    ('tip', [-2, 3, -8], [4, 3, 1], None),
])]


def head_painter():
    p = Painter(SHEET, 1931)
    uv = pack(HEAD, SHEET)
    for island, size in islands(HEAD).items():
        for face, rect in faces(size, uv[island]).items():
            light = 0.14 if face == 'up' else -0.16 if face == 'down' else 0
            if island == 'foot':
                p.ramp(rect, FOOT, lambda x, y, w, h: skin(x, y, w, h) - 0.05 + light, grain=0.08, mottle=0.25)
            elif island in ('snout', 'tip'):
                p.ramp(rect, SNOUT, lambda x, y, w, h: 0.5 * ball(x, y, w, h) + 0.5 * skin(x, y, w, h) + light,
                       grain=0.05, mottle=0.2)
            else:
                p.ramp(rect, BARK, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + light, grain=0.06,
                       mottle=0.24)
                p.vein(rect, BARK[0], 5, 220)
    return p


def head():
    write_static('tunnel_root', HEAD, SHEET, head_painter())
    variants = {f'facing={facing}': {'model': 'infestusfrontier:block/tunnel_root', **({'y': y} if y else {})}
                for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}
    (ASSETS / 'blockstates/tunnel_root.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')
    (ASSETS / 'models/item/tunnel_root.json').write_text(
        json.dumps({'parent': 'infestusfrontier:block/tunnel_root'}, indent=2) + '\n')


SOIL = [(46, 36, 28), (62, 48, 36), (78, 62, 46), (96, 78, 58)]
ROOT = [(92, 54, 46), (128, 78, 64), (164, 108, 86), (196, 142, 114)]


def soil(seed):
    rng = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), SOIL[min(3, int(rng.random() * 2.2 + (1 if (x * 7 + y * 3) % 11 == 0 else 0)))] + (255,))
    for _ in range(4):  # thin rootlets through the earth
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(3, 6)):
            img.putpixel((x % 16, y % 16), ROOT[0] + (255,))
            x += rng.choice((-1, 0, 1))
            y += 1
    return img


def band(img, inside):
    """Paints the living root where inside(x, y) gives its depth across the band (0 edge .. 1 middle), else None."""
    for y in range(16):
        for x in range(16):
            depth = inside(x, y)
            if depth is None:
                continue
            shade = 3 if depth > 0.7 else 2 if depth > 0.35 else 1
            if (x + 2 * y) % 7 == 0 and shade > 1:
                shade -= 1  # bark creases across the root
            img.putpixel((x, y), ROOT[shade] + (255,))
    return img


HALF = 3.2  # half the root's width, in texels


def straight(x, y):
    """Runs north-south down the middle of the tile."""
    d = abs(x + 0.5 - 8)
    return 1 - d / HALF if d <= HALF else None


def bent(x, y):
    """A quarter circle from the north edge round to the east edge, centred on the north-east corner."""
    d = abs(math.hypot(x + 0.5 - 16, y + 0.5) - 8)
    return 1 - d / HALF if d <= HALF else None


def cells():
    out = ASSETS / 'textures/block'
    soil(4021).save(out / 'root_cell_side.png', optimize=True)
    band(soil(4021), straight).save(out / 'root_cell_top.png', optimize=True)
    band(soil(4021), bent).save(out / 'root_cell_bend.png', optimize=True)
    for name, top in (('root_cell', 'root_cell_top'), ('root_cell_bend', 'root_cell_bend')):
        model = {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
            'top': 'infestusfrontier:block/' + top, 'bottom': 'infestusfrontier:block/root_cell_side',
            'side': 'infestusfrontier:block/root_cell_side', 'particle': 'infestusfrontier:block/root_cell_side'}}
        (ASSETS / 'models/block' / (name + '.json')).write_text(json.dumps(model, indent=2) + '\n')
    variants = {}
    for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for bend in ('false', 'true'):
            model = 'infestusfrontier:block/' + ('root_cell_bend' if bend == 'true' else 'root_cell')
            variants[f'bend={bend},facing={facing}'] = {'model': model, **({'y': y} if y else {})}
    (ASSETS / 'blockstates/root_cell.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')
    (ASSETS / 'models/item/root_cell.json').write_text(
        json.dumps({'parent': 'infestusfrontier:block/root_cell'}, indent=2) + '\n')


def segment():
    """A short root lying corner to corner, knotted in the middle, with a pale cut end."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for t in range(3, 13):
        x, y = t, 15 - t
        for dx, dy, shade in ((0, 0, 2), (1, 0, 2), (0, -1, 3), (1, -1, 3), (-1, 0, 1), (0, 1, 1), (-1, 1, 0),
                              (1, 1, 0), (2, -1, 1)):
            img.putpixel((x + dx, y + dy), ROOT[shade] + (255,))
    for dx, dy in ((0, 0), (1, 0), (0, -1), (1, -1), (2, 0), (-1, -1)):  # the knot
        img.putpixel((7 + dx, 8 + dy), ROOT[3] + (255,))
    for dx, dy in ((0, 0), (1, 0), (0, -1), (1, -1), (1, 1)):  # the cut end, pale flesh
        img.putpixel((12 + dx, 3 + dy), SNOUT[3] + (255,))
    for x, y in ((2, 13), (3, 14), (4, 13)):  # trailing rootlets
        img.putpixel((x, y), ROOT[0] + (255,))
    (ASSETS / 'textures/item').mkdir(parents=True, exist_ok=True)
    img.save(ASSETS / 'textures/item/root_segment.png', optimize=True)
    (ASSETS / 'models/item/root_segment.json').write_text(json.dumps(
        {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'infestusfrontier:item/root_segment'}},
        indent=2) + '\n')


def main():
    head()
    cells()
    segment()
    print('tunnel root: 1 model, 1 texture; root cell: 2 models, 3 textures; root segment: 1 icon; 2 blockstates')


if __name__ == '__main__':
    main()
