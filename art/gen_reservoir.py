"""The Reservoir cell: membrane, bone and flesh textures, the part models, the multipart blockstate and the item model.

    python3 art/gen_reservoir.py

A body of cells reads as one rounded tank. Every part shows only where the body ends: a membrane pane on each side
face that joins no cell, a lid on top, a fleshy base with a foot lip under the bottom layer, bone ribs up the outer
corners, a fleshy rim along the top edges and a knuckle where two top edges meet. The liquid is drawn by the block
entity renderer, so it can run seamlessly into joined cells. Each part is modelled once on the north (or north-west)
side and turned onto the others by the blockstate. No two faces share a plane: parts that meet are offset by a
fraction of a pixel, and faces that would only meet inside another part are left out.
"""
import json
import math
import random
from pathlib import Path
from PIL import Image

ROOT = Path('src/main/resources/assets/infestusfrontier')
TEXTURES = ROOT / 'textures/block'
MODELS = ROOT / 'models/block'
ITEMS = ROOT / 'models/item'
STATES = ROOT / 'blockstates'
NS = 'infestusfrontier:block/'
SIZE = 64
TAU = 2 * math.pi

MEMBRANE = [(96, 40, 70), (150, 82, 112), (200, 136, 160), (236, 190, 206), (252, 232, 238)]
VEIN = [(52, 8, 26), (96, 20, 48), (146, 40, 70), (196, 84, 104), (232, 150, 156)]
IVORY = [(92, 74, 60), (140, 118, 92), (186, 166, 130), (222, 206, 170), (246, 238, 214)]
FLESH = [(46, 12, 24), (88, 26, 44), (136, 50, 70), (182, 88, 102), (222, 140, 142), (244, 196, 186)]
LIGHT = (-0.55, -0.55, 0.63)  # from the upper left, toward the viewer


def ramp(colours, t):
    """The colour at t along a smooth ramp, interpolated between its stops."""
    t = max(0.0, min(1.0, t)) * (len(colours) - 1)
    i = min(int(t), len(colours) - 2)
    f = t - i
    return tuple(int(round(a + (b - a) * f)) for a, b in zip(colours[i], colours[i + 1]))


def lattice(seed, cells):
    """Smooth value noise that repeats every texture width: a wrapped lattice of cells x cells, smoothstepped."""
    rng = random.Random(seed)
    grid = [[rng.random() for _ in range(cells)] for _ in range(cells)]

    def noise(x, y):
        gx, gy = x * cells / SIZE, y * cells / SIZE
        ix, iy = int(math.floor(gx)), int(math.floor(gy))
        fx, fy = gx - ix, gy - iy
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        a, b = grid[iy % cells][ix % cells], grid[iy % cells][(ix + 1) % cells]
        c, d = grid[(iy + 1) % cells][ix % cells], grid[(iy + 1) % cells][(ix + 1) % cells]
        return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy
    return noise


def fbm(seed, octaves):
    layers = [(lattice(seed + i, cells), weight) for i, (cells, weight) in enumerate(octaves)]
    total = sum(w for _, w in layers)
    return lambda x, y: sum(n(x, y) * w for n, w in layers) / total


def cells(seed, count, stretch=1.0):
    """Wrapped Worley noise: (nearest, second nearest) distance in pixels to jittered points, one per grid cell."""
    rng = random.Random(seed)
    step = SIZE / count
    points = [((i + rng.uniform(0.15, 0.85)) * step, (j + rng.uniform(0.15, 0.85)) * step)
              for i in range(count) for j in range(count)]

    def distances(x, y):
        best = [1e9, 1e9]
        for px, py in points:
            dx = (x - px + SIZE / 2) % SIZE - SIZE / 2
            dy = (y - py + SIZE / 2) % SIZE - SIZE / 2
            d = math.hypot(dx, dy * stretch)
            if d < best[0]:
                best = [d, best[0]]
            elif d < best[1]:
                best[1] = d
        return best
    return distances


def lit(height, strength):
    """Lambert light from the upper left on a wrapped height field, 0..1 per pixel."""
    out = {}
    for y in range(SIZE):
        for x in range(SIZE):
            dx = (height[y][(x + 1) % SIZE] - height[y][(x - 1) % SIZE]) * strength
            dy = (height[(y + 1) % SIZE][x] - height[(y - 1) % SIZE][x]) * strength
            n = (-dx, -dy, 1.0)
            length = math.sqrt(n[0] ** 2 + n[1] ** 2 + 1)
            out[(x, y)] = max(0.0, (n[0] * LIGHT[0] + n[1] * LIGHT[1] + n[2] * LIGHT[2]) / length)
    return out


def tubes(seed, count, wobble_seed, wobble, width, fine):
    """Vein strength 0..1 per pixel along the creases of wrapped Worley cells, bent by noise."""
    worley = cells(seed, count)
    bend = fbm(wobble_seed, [(4, 1.0), (8, 0.4)])
    out = {}
    for y in range(SIZE):
        for x in range(SIZE):
            ox = (bend(x, y) - 0.5) * wobble
            oy = (bend(y + 17, x + 5) - 0.5) * wobble
            f1, f2 = worley(x + ox, y + oy)
            w = width * (0.7 + 0.6 * fine(x, y))
            out[(x, y)] = max(0.0, 1 - (f2 - f1) / w)
    return out


def membrane():
    """A thin translucent membrane: a few broad veins, fine capillaries between them, all lit as tubes."""
    fine = fbm(45, [(16, 1.0), (32, 0.4)])
    trunks = tubes(41, 2, 43, 26, 2.0, fine)
    twigs = tubes(47, 4, 49, 30, 1.0, fine)
    height = [[1.8 * trunks[(x, y)] ** 1.5 + 0.7 * twigs[(x, y)] ** 1.5 + 0.2 * fine(x, y) for x in range(SIZE)]
              for y in range(SIZE)]
    light = lit(height, 2.4)
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            trunk, twig, l = trunks[(x, y)], twigs[(x, y)], light[(x, y)]
            if trunk > 0.15:
                colour = ramp(VEIN, 0.1 + 0.9 * l * (0.55 + 0.45 * trunk))
                alpha = int(170 + 85 * min(1.0, trunk * 1.5))
            elif twig > 0.35:
                colour = ramp(VEIN, 0.35 + 0.55 * l)
                alpha = int(96 + 70 * twig)
            else:
                colour = ramp(MEMBRANE, 0.3 + 0.4 * l + 0.2 * fine(x, y))
                alpha = int(84 + 44 * fine(x + 9, y + 3))
            img.putpixel((x, y), colour + (alpha,))
    img.save(TEXTURES / 'reservoir_membrane.png', optimize=True)


def bone():
    """Ivory with broad swells, faint growth lines along the rib and a few pores, lit from the upper left."""
    swell = fbm(53, [(2, 1.0), (4, 0.7), (8, 0.25)])
    lines = fbm(54, [(16, 1.0)])
    rng = random.Random(55)
    pits = [(rng.uniform(0, SIZE), rng.uniform(0, SIZE), rng.uniform(1.0, 2.0)) for _ in range(9)]
    height = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            h = 1.4 * swell(x, y) + 0.05 * math.sin(TAU * (5 * x / SIZE + 0.3 * swell(y, x))) + 0.06 * lines(x, y)
            for px, py, r in pits:
                dx = (x - px + SIZE / 2) % SIZE - SIZE / 2
                dy = (y - py + SIZE / 2) % SIZE - SIZE / 2
                d = math.hypot(dx, dy)
                if d < r:
                    h -= 0.45 * (1 - d / r) ** 2
            row.append(h)
        height.append(row)
    light = lit(height, 3.4)
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            t = 0.25 + 0.6 * light[(x, y)] + 0.25 * (height[y][x] - 0.7)
            img.putpixel((x, y), ramp(IVORY, t) + (255,))
    img.save(TEXTURES / 'reservoir_bone.png', optimize=True)


def flesh():
    """Lobed muscle: broad soft swells, narrow dark creases with a vein in some, fine wrinkles, wet sheen."""
    worley = cells(61, 3, stretch=1.25)
    wobble = fbm(63, [(4, 1.0), (8, 0.5)])
    fine = fbm(65, [(16, 1.0), (32, 0.6)])
    rng = random.Random(67)
    pores = [(rng.uniform(0, SIZE), rng.uniform(0, SIZE)) for _ in range(10)]
    height, crease = [], {}
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            ox = (wobble(x, y) - 0.5) * 24
            oy = (wobble(y + 11, x + 23) - 0.5) * 24
            f1, f2 = worley(x + ox, y + oy)
            edge = f2 - f1
            c = max(0.0, 1 - edge / (1.0 + 1.4 * fine(x + 31, y)))
            crease[(x, y)] = c
            h = math.sqrt(min(1.0, edge / 14.0)) + 0.16 * fine(x, y) + 0.05 * math.sin(TAU * (x + 2 * y) / 16)
            for px, py in pores:
                dx = (x - px + SIZE / 2) % SIZE - SIZE / 2
                dy = (y - py + SIZE / 2) % SIZE - SIZE / 2
                d = math.hypot(dx, dy)
                if d < 1.5:
                    h -= 0.3 * (1 - d / 1.5)
            row.append(h)
        height.append(row)
    light = lit(height, 1.8)
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            l = light[(x, y)]
            t = 0.08 + 0.62 * l + 0.2 * (height[y][x] - 0.6) - 0.35 * crease[(x, y)]
            if l > 0.9:
                t += 0.15 * (l - 0.9) / 0.1
            img.putpixel((x, y), ramp(FLESH, t) + (255,))
    img.save(TEXTURES / 'reservoir_flesh.png', optimize=True)


def face(texture, uv, cull=None):
    out = {'texture': '#' + texture, 'uv': [round(c, 4) for c in uv]}
    if cull:
        out['cullface'] = cull
    return out


def box(lo, hi, texture, faces, cull=()):
    """An element from lo to hi with only the named faces, mapped to the block's own pixels."""
    (x0, y0, z0), (x1, y1, z1) = lo, hi
    uvs = {
        'north': (16 - x1, 16 - y1, 16 - x0, 16 - y0),
        'south': (x0, 16 - y1, x1, 16 - y0),
        'west': (z0, 16 - y1, z1, 16 - y0),
        'east': (16 - z1, 16 - y1, 16 - z0, 16 - y0),
        'up': (x0, z0, x1, z1),
        'down': (x0, 16 - z1, x1, 16 - z0),
    }
    clamp = lambda uv: tuple(max(0.0, min(16.0, c)) for c in uv)
    return {
        'from': list(lo),
        'to': list(hi),
        'faces': {f: face(texture, clamp(uvs[f]), f if f in cull else None) for f in faces},
    }


SIDES = ['north', 'south', 'east', 'west']
PARTS = {
    # under the bottom layer: the fleshy floor of the tank
    'base': [box((0, 0, 0), (16, 2, 16), 'flesh', ['up', 'down'] + SIDES, cull=['down'] + SIDES)],
    # a rounded lip along a bottom edge that faces out
    'foot': [box((0, 0, -0.375), (16, 2.5, 1.125), 'flesh', ['north', 'up', 'south'])],
    # a side membrane over the base, and one running the full height above the bottom layer
    'pane_low': [box((0, 2, 0), (16, 16, 0.5), 'membrane', ['north', 'south'], cull=['north'])],
    'pane_tall': [box((0, 0, 0), (16, 16, 0.5), 'membrane', ['north', 'south'], cull=['north'])],
    'lid': [box((0, 15.5, 0), (16, 16, 16), 'membrane', ['up', 'down'], cull=['up'])],
    'rim': [box((0, 14.5, -0.375), (16, 16.25, 1.125), 'flesh', ['north', 'up', 'down', 'south'])],
    # a corner rib rounded by two cross bars over a square core; every face plane differs from the panes and the rims
    'rib': [box((-0.125, 0, -0.125), (1.5, 16, 1.5), 'bone', SIDES),
            box((-0.3125, 0, 0.3125), (1.8125, 16, 1.0), 'bone', SIDES),
            box((0.3125, 0, -0.3125), (1.0, 16, 1.8125), 'bone', SIDES)],
    # a domed cap: the lower cross bar shows round the edge of the upper one
    'knuckle': [box((-0.5, 14.75, -0.125), (2, 16.75, 1.625), 'bone', ['up', 'down'] + SIDES),
                box((-0.125, 14.875, -0.5), (1.625, 16.625, 2), 'bone', ['up', 'down'] + SIDES)],
}
TEXTURE_MAP = {
    'membrane': NS + 'reservoir_membrane',
    'bone': NS + 'reservoir_bone',
    'flesh': NS + 'reservoir_flesh',
    'particle': NS + 'reservoir_membrane',
}
# blockstate y turns: the north side onto each side, the north-west corner onto each corner
TURNS = [('north', 'west', 0), ('east', 'north', 90), ('south', 'east', 180), ('west', 'south', 270)]


# membrane parts come in four texture turns, picked at random per block, so a wide wall shows no repeat
TURNED = ('pane_low', 'pane_tall', 'lid')


def write_parts():
    for name, elements in PARTS.items():
        for turn in range(4 if name in TURNED else 1):
            spun = [dict(e, faces={f: dict(spec, rotation=90 * turn) if turn else spec for f, spec in e['faces'].items()})
                    for e in elements]
            model = {'render_type': 'minecraft:translucent', 'textures': TEXTURE_MAP, 'elements': spun}
            suffix = f'_{turn}' if name in TURNED else ''
            (MODELS / f'reservoir_cell_{name}{suffix}.json').write_text(json.dumps(model, indent=2) + '\n')
    # every state draws at least this, so a cell joined on all sides still has its break particles
    core = {'render_type': 'minecraft:translucent', 'textures': {'particle': TEXTURE_MAP['particle']}}
    (MODELS / 'reservoir_cell_core.json').write_text(json.dumps(core, indent=2) + '\n')


def applied(model, turn):
    """One model, or the four texture turns of a membrane part as random choices, turned onto a side."""
    names = [f'{model}_{k}' for k in range(4)] if model in TURNED else [model]
    choices = [dict({'model': NS + 'reservoir_cell_' + name}, **({'y': turn} if turn else {})) for name in names]
    return choices if len(choices) > 1 else choices[0]


def write_blockstate():
    parts = [{'apply': applied('core', 0)}, {'when': {'down': 'false'}, 'apply': applied('base', 0)},
             {'when': {'up': 'false'}, 'apply': applied('lid', 0)}]
    for side, left, turn in TURNS:
        def part(model, when):
            parts.append({'when': when, 'apply': applied(model, turn)})
        part('foot', {'down': 'false', side: 'false'})
        part('pane_low', {'down': 'false', side: 'false'})
        part('pane_tall', {'down': 'true', side: 'false'})
        part('rim', {'up': 'false', side: 'false'})
        part('rib', {side: 'false', left: 'false'})
        part('knuckle', {'up': 'false', side: 'false', left: 'false'})
    (STATES / 'reservoir_cell.json').write_text(json.dumps({'multipart': parts}, indent=2) + '\n')


FACE_TURN = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north', 'up': 'up', 'down': 'down'}


def turned(element, quarter):
    """The element turned clockwise seen from above by quarter right angles about the block centre."""
    for _ in range(quarter):
        (x0, y0, z0), (x1, y1, z1) = element['from'], element['to']
        element = {
            'from': [16 - z1, y0, x0],
            'to': [16 - z0, y1, x1],
            'faces': {FACE_TURN[f]: dict(spec, **({'cullface': FACE_TURN[spec['cullface']]} if 'cullface' in spec else {}))
                      for f, spec in element['faces'].items()},
        }
    return element


def write_item():
    """A lone cell, half full: every part of an unjoined cell and a block of still biomass inside."""
    elements = list(PARTS['base']) + list(PARTS['lid'])
    for _, _, turn in TURNS:
        for name in ('foot', 'pane_low', 'rim', 'rib', 'knuckle'):
            elements += [turned(e, turn // 90) for e in PARTS[name]]
    liquid = box((0.75, 2, 0.75), (15.25, 9.5, 15.25), 'liquid', ['up'] + SIDES)
    elements.append(liquid)
    for element in elements:
        for spec in element['faces'].values():
            spec.pop('cullface', None)
    model = {
        'render_type': 'minecraft:translucent',
        'textures': dict(TEXTURE_MAP, liquid=NS + 'biomass_still'),
        'elements': elements,
        'display': {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, -0.25, 0], 'scale': [0.58, 0.58, 0.58]},
            'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]},
        },
    }
    (ITEMS / 'reservoir_cell.json').write_text(json.dumps(model, indent=2) + '\n')


def main():
    membrane()
    bone()
    flesh()
    write_parts()
    write_blockstate()
    write_item()
    print('reservoir_cell: 3 textures, %d part models, 1 blockstate, 1 item model' % (len(PARTS) + 1 + 3 * len(TURNED)))


if __name__ == '__main__':
    main()
