"""Tunnel Root, Root Cells, Root Treads and the Root Segment. The head is a squat, bark-brown root tip on a broad foot
with a pale, fleshy snout that points the way it cuts (north in the model; the block entity turns it) and two small
hooks either side of the snout that grip the rock. A Root Cell is a full block of packed earth threaded with roots; its
top shows the living root running straight along the tunnel or round a bend. A Root Tread is a stair of the same
earth with a living root along its nose. The Root Segment icon is a short knotted root.

    python3 art/gen_tunnel_root.py

The head is a GeckoLib organ. Its rest pose matches the shape in TunnelRootBlock.BOXES. It rests in one of three
loops: idle, or leaning towards the next cut when the player steers it left or right (aim_left, aim_right). After a
step it plays its crawl once: its body still stands in the cell it left, the snout reaches forward into the cut on a
neck that stretches behind it, the hooks close on the rock, and the body pulls itself in after it. On a turn the body
starts turned the old way and swings round to the new heading while it pulls. GeckoLib mirrors x and negates
rotations about x and y (art/geo_audit.py): a positive file y turn is clockwise seen from above, and a position's x is
mirrored, so the left of a head facing north is +x in the file.
"""
import json
import math
import random
from PIL import Image
from organ_model import ASSETS, Painter, ball, compact, faces, folds, islands, pack, sampled, skin, wave, write

SHEET = 64
BARK = [(44, 30, 22), (66, 46, 32), (92, 64, 44), (120, 86, 58), (150, 112, 76), (178, 140, 98)]
FOOT = [(36, 28, 22), (54, 42, 32), (74, 58, 42), (96, 76, 54)]
SNOUT = [(120, 62, 64), (160, 88, 86), (196, 124, 112), (226, 168, 150), (244, 206, 186)]
HOOK = [(150, 132, 104), (186, 168, 136), (214, 200, 170), (234, 226, 204)]

IDLE = 4.0
CRAWL = 2.0
# crawl beats, seconds: the snout reaches into the cut, the hooks close, the body pulls itself in, it settles
REACH, GRIP, PULLED = 0.6, 0.85, 1.6
CELL = 16  # the head crawls one block: its body starts this far behind its new place
AIM = 20  # how far the snout leans towards a steered cut, degrees; it draws back a little so it stays in its block
AIM_BACK = 1.2

# (bone, parent, pivot, cubes); each cube (island, origin [x from -8, y, z from -8], size, turn); north is -z
BONES = [
    ('root', None, [0, 0, 0], []),
    ('body', 'root', [0, 5, 1], [
        ('foot', [-6, 0, -5], [12, 2, 12], None),
        ('belly', [-5, 2, -4], [10, 7, 10], None),
        ('crown', [-4, 9, -2], [8, 3, 7], None),
    ]),
    ('snout', 'root', [0, 4.5, -5.5], [
        ('snout', [-3, 2.1, -7.25], [6, 5, 4], None),
        ('tip', [-2, 3, -7.75], [4, 3, 1], None),
    ]),
    # hidden inside the snout and the belly at rest; it stretches back from the snout while the body follows
    ('neck', 'snout', [0, 4.5, -4.3], [('neck', [-2, 3.25, -4.3], [4, 2, 1], None)]),
    ('hook_l', 'snout', [3, 4.5, -6.5], [('hook', [2.4, 3.5, -7.9], [1, 2, 2], None)]),
    ('hook_r', 'snout', [-3, 4.5, -6.5], [('hook', [-3.4, 3.5, -7.9], [1, 2, 2], None)]),
]


def head_painter(uv):
    p = Painter(SHEET, 1931)
    for island, size in islands(BONES).items():
        for face, rect in faces(size, uv[island]).items():
            light = 0.14 if face == 'up' else -0.16 if face == 'down' else 0
            if island == 'foot':
                p.ramp(rect, FOOT, lambda x, y, w, h: skin(x, y, w, h) - 0.05 + light, grain=0.08, mottle=0.25)
            elif island in ('snout', 'tip', 'neck'):
                p.ramp(rect, SNOUT, lambda x, y, w, h: 0.5 * ball(x, y, w, h) + 0.5 * skin(x, y, w, h) + light,
                       grain=0.05, mottle=0.2)
                if island == 'neck':
                    p.vein(rect, SNOUT[0], 2, 200)
            elif island == 'hook':
                p.ramp(rect, HOOK, lambda x, y, w, h: skin(x, y, w, h) + light, grain=0.04, mottle=0.15)
            else:
                p.ramp(rect, BARK, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + light, grain=0.06,
                       mottle=0.24)
                p.vein(rect, BARK[0], 5, 220)
    return p


def keys(points):
    """Keyframes from (time, value) points, held flat between them by the linear interpolation GeckoLib uses."""
    return {('%.2f' % t if t else '0.0'): [round(v, 4) for v in value] for t, value in points}


def breathing(t):
    return [1 + 0.015 * wave(t, IDLE), 1 - 0.02 * wave(t, IDLE), 1 + 0.015 * wave(t, IDLE)]


def rest(lean):
    """A resting loop: the body breathes, the hooks twitch, and the snout and body lean `lean` degrees about y."""
    return (IDLE, True, {
        'body': {'scale': sampled(IDLE, 0.25, breathing),
                 'rotation': keys([(0, [0, lean / 4, 0]), (IDLE, [0, lean / 4, 0])])},
        'snout': {'rotation': sampled(IDLE, 0.25, lambda t: [2 * wave(t, IDLE, 0.5), lean, 0]),
                  'position': keys([(0, [0, 0, AIM_BACK if lean else 0]), (IDLE, [0, 0, AIM_BACK if lean else 0])])},
        'hook_l': {'rotation': sampled(IDLE, 0.25, lambda t: [0, -6 * max(0.0, wave(t, IDLE)), 0])},
        'hook_r': {'rotation': sampled(IDLE, 0.25, lambda t: [0, 6 * max(0.0, wave(t, IDLE)), 0])},
    })


def crawl(turn):
    """One step's crawl into the cell. `turn` is the old heading relative to the new, in file degrees about y: 90 when
    the head turned left (it faced east before facing north), -90 when it turned right, 0 straight on."""
    side = 0 if turn == 0 else (1 if turn > 0 else -1)
    # where the snout starts: on the front of the body in the old cell, facing the old way
    start = [-5.5 * side, 0, CELL + 5.5 * abs(side)]
    stretch = 20 if side else 17  # neck length, in its own pixels, that spans from the cut back to the old cell
    return (CRAWL, False, {
        'body': {
            'position': keys([(0, [0, 0, CELL]), (GRIP, [0, 0, CELL]), ((GRIP + PULLED) / 2, [0, 0.6, CELL / 2]),
                              (PULLED, [0, 0, 0]), (CRAWL, [0, 0, 0])]),
            'rotation': keys([(0, [0, turn, 0]), (GRIP, [0, turn, 0]), (PULLED, [0, 0, 0]), (CRAWL, [0, 0, 0])]),
            'scale': keys([(0, [1, 1, 1]), (GRIP, [1, 1, 1]), ((GRIP + PULLED) / 2, [0.96, 0.9, 1.1]),
                           (PULLED, [1.02, 0.96, 1.02]), (CRAWL, [1, 1, 1])]),
        },
        'snout': {
            'position': keys([(0, start), (REACH, [0, 0, 0]), (CRAWL, [0, 0, 0])]),
            'rotation': keys([(0, [0, turn, 0]), (REACH, [6, 0, 0]), (GRIP, [0, 0, 0]), (CRAWL, [0, 0, 0])]),
        },
        'neck': {'scale': keys([(0, [1, 1, 1]), (REACH, [1, 1, stretch]), (GRIP, [1, 1, stretch]),
                                (PULLED, [1, 1, 1]), (CRAWL, [1, 1, 1])])},
        'hook_l': {'rotation': keys([(0, [0, 0, 0]), (REACH * 0.6, [0, 32, 0]), (REACH, [0, 32, 0]),
                                     (GRIP, [0, -6, 0]), (PULLED, [0, -6, 0]), (CRAWL, [0, 0, 0])])},
        'hook_r': {'rotation': keys([(0, [0, 0, 0]), (REACH * 0.6, [0, -32, 0]), (REACH, [0, -32, 0]),
                                     (GRIP, [0, 6, 0]), (PULLED, [0, 6, 0]), (CRAWL, [0, 0, 0])])},
    })


def head():
    uv = pack(BONES, SHEET)
    painter = head_painter(uv)
    anims = {'idle': rest(0), 'aim_left': rest(-AIM), 'aim_right': rest(AIM),
             'crawl': crawl(0), 'crawl_left': crawl(90), 'crawl_right': crawl(-90)}
    particle = faces(islands(BONES)['belly'], uv['belly'])['north']
    write('tunnel_root', BONES, SHEET, painter, anims, (particle[0], particle[1]), bounds=(3, 2, (0, 0.5, 0)))
    (ASSETS / 'blockstates/tunnel_root.json').write_text(
        json.dumps({'variants': {'': {'model': 'infestusfrontier:block/tunnel_root'}}}, indent=2) + '\n')
    (ASSETS / 'models/block/tunnel_root.json').write_text(
        json.dumps({'textures': {'particle': 'infestusfrontier:block/tunnel_root'}}, indent=2) + '\n')


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


def tread():
    """A Root Tread: vanilla's stair shapes in root-cell earth, the living root running along each step's top."""
    for name, parent in (('root_tread', 'stairs'), ('root_tread_inner', 'inner_stairs'),
                         ('root_tread_outer', 'outer_stairs')):
        model = {'parent': 'minecraft:block/' + parent, 'textures': {
            'bottom': 'infestusfrontier:block/root_cell_side', 'top': 'infestusfrontier:block/root_cell_top',
            'side': 'infestusfrontier:block/root_cell_side', 'particle': 'infestusfrontier:block/root_cell_side'}}
        (ASSETS / 'models/block' / (name + '.json')).write_text(json.dumps(model, indent=2) + '\n')
    # vanilla's stair blockstate: the y turn of each facing, and the extra turn of each corner shape
    turns = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
    variants = {}
    for facing, y in turns.items():
        for half in ('bottom', 'top'):
            for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                model = 'infestusfrontier:block/root_tread' + (
                    '_inner' if shape.startswith('inner') else '_outer' if shape.startswith('outer') else '')
                turn = y + (-90 if shape.endswith('left') else 0)
                if half == 'top':
                    turn += 90 if shape != 'straight' else 0
                variant = {'model': model}
                if half == 'top':
                    variant['x'] = 180
                if turn % 360:
                    variant['y'] = turn % 360
                if half == 'top' or turn % 360:
                    variant['uvlock'] = True
                variants[f'facing={facing},half={half},shape={shape}'] = variant
    (ASSETS / 'blockstates/root_tread.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')
    (ASSETS / 'models/item/root_tread.json').write_text(
        json.dumps({'parent': 'infestusfrontier:block/root_tread'}, indent=2) + '\n')


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
    tread()
    segment()
    print('tunnel root: 1 GeckoLib model, 6 clips; root cell: 2 models, 3 textures; root tread: 3 models; '
          'root segment: 1 icon; 3 blockstates')


if __name__ == '__main__':
    main()
