"""The three living wall tiers: the wall flesh texture, the block models, blockstates and item models.

    python3 art/gen_walls.py

A Living Wall is a full cube of dense muscular tissue: broad lobes parted by dark creases, with faint fibres running
up the wall. Its blockstate turns the cube four ways at random, so a long wall shows no repeat. A Ribbed Wall keeps
that flesh and grows two upright bone ribs on each side face, unevenly spaced and running the full height, so stacked
walls show unbroken ribs. A Calcified Wall is plated: bevelled bone plates on every face with staggered flesh seams,
a different layout on each side; it turns four ways at random, so neighbouring blocks never line up into a grid.
Bone parts stand proud and cull against a neighbouring wall, so joined walls show no bone between them. The bone is
the Reservoir's painted bone; the flesh texture is generated here until ART-010 paints it.
"""
import json
import math
import random

from PIL import Image

from gen_reservoir import ITEMS, MODELS, NS, SIZE, STATES, TEXTURES, cells, fbm, lit, ramp

WALL = [(38, 10, 20), (72, 22, 36), (112, 40, 56), (150, 64, 76), (190, 102, 108), (226, 152, 150)]
BONE = NS + 'reservoir_bone'
FLESH = NS + 'living_wall'
SIDES = ['north', 'east', 'south', 'west']
CULL = {'north': 'north', 'south': 'south', 'east': 'east', 'west': 'west', 'up': 'up', 'down': 'down'}


def wall_texture():
    """Muscular lobes with dark creases and a faint upward grain, seamless on all four edges."""
    worley = cells(301, 4, stretch=1.4)
    wobble = fbm(303, [(4, 1.0), (8, 0.5)])
    grain = fbm(305, [(16, 1.0), (32, 0.5)])
    rng = random.Random(307)
    height = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            ox = (wobble(x, y) - 0.5) * 20
            oy = (wobble(y + 17, x + 5) - 0.5) * 20
            f1, f2 = worley(x + ox, y + oy)
            lobe = min(1.0, (f2 - f1) / 9.0)
            fibre = 0.5 + 0.5 * math.sin(2 * math.pi * (x / SIZE * 9 + grain(x, y) * 1.5))
            row.append(lobe * 0.8 + fibre * 0.12 + grain(x, y) * 0.08)
        height.append(row)
    light = lit(height, 6.0)
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            v = 0.15 + 0.55 * height[y][x] + 0.35 * (light[(x, y)] - 0.5) + 0.04 * (rng.random() - 0.5)
            img.putpixel((x, y), ramp(WALL, v) + (255,))
    img.save(TEXTURES / 'living_wall.png', optimize=True)


def inside(lo, hi):
    """The span lo..hi moved, keeping its width, to lie inside the 16-pixel sprite."""
    if lo < 0:
        return 0, hi - lo
    if hi > 16:
        return 16 - (hi - lo), 16
    return lo, hi


def uv(face, frm, to):
    """The face's texture square projected from the block; a part standing outside the block keeps its size and is
    moved into the sprite, so no face samples a neighbouring sprite on the atlas."""
    x0, y0, z0 = frm
    x1, y1, z1 = to
    if face in ('up', 'down'):
        u0, u1 = inside(x0, x1)
        v0, v1 = inside(z0, z1)
        return [u0, v0, u1, v1]
    u0, u1 = inside(*((x0, x1) if face in ('north', 'south') else (z0, z1)))
    v0, v1 = inside(16 - y1, 16 - y0)
    return [u0, v0, u1, v1]


def cube():
    faces = {f: {'uv': [0, 0, 16, 16], 'texture': '#flesh', 'cullface': f} for f in CULL}
    return {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces}


def proud(frm, to, toward):
    """A bone part standing out of the face {@code toward}; every face of it culls against a wall there."""
    faces = {}
    for f in CULL:
        if f == {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east', 'up': 'down',
                 'down': 'up'}[toward]:
            continue
        faces[f] = {'uv': uv(f, frm, to), 'texture': '#bone', 'cullface': toward}
    return {'from': list(frm), 'to': list(to), 'faces': faces}


def on_top(x0, z0, x1, z1, up, depth):
    y0, y1 = (16, 16 + depth) if up else (-depth, 0)
    return proud([x0, y0, z0], [x1, y1, z1], 'up' if up else 'down')


# rib positions across each side face: the same on every block, so stacked walls show unbroken ribs
RIBS = {'north': ((3, 5), (10, 12.5)), 'east': ((2, 4), (9, 11)), 'south': ((4, 6.5), (12, 14)),
        'west': ((2.5, 4.5), (11, 13))}
# bone plates on each side face as (u0, y0, u1, y1): staggered seams, a different layout on every side
PLATES = {
    'north': ((0.5, 0.5, 9.5, 7), (10.5, 0.5, 15.5, 7.5), (0.5, 8, 6, 15.5), (7, 8.5, 15.5, 15.5)),
    'east': ((0.5, 0.5, 5.5, 9), (6.5, 0.5, 15.5, 6.5), (6.5, 7.5, 15.5, 15.5), (0.5, 10, 5.5, 15.5)),
    'south': ((0.5, 0.5, 7.5, 5.5), (8.5, 0.5, 15.5, 10), (0.5, 6.5, 7.5, 15.5), (8.5, 11, 15.5, 15.5)),
    'west': ((0.5, 0.5, 11, 8), (12, 0.5, 15.5, 15.5), (0.5, 9, 5, 15.5), (6, 9, 11, 15.5)),
}
TOP_PLATES = ((0.5, 0.5, 9, 7.5), (10, 0.5, 15.5, 15.5), (0.5, 8.5, 9, 15.5))


def side_box(side, u0, u1, y0, y1, depth):
    """A box {@code depth} pixels proud of side face {@code side}, u0..u1 across it and y0..y1 up it."""
    if side == 'north':
        return proud([16 - u1, y0, -depth], [16 - u0, y1, 0], side)
    if side == 'south':
        return proud([u0, y0, 16], [u1, y1, 16 + depth], side)
    if side == 'west':
        return proud([-depth, y0, u0], [0, y1, u1], side)
    return proud([16, y0, 16 - u1], [16 + depth, y1, 16 - u0], side)


def ribbed():
    """Upright bone ribs over the full height, so stacked walls show unbroken ribs; each rib has a raised ridge."""
    e = [cube()]
    for side in SIDES:
        for u0, u1 in RIBS[side]:
            e.append(side_box(side, u0, u1, 0, 16, 1))
            mid = (u0 + u1) / 2
            e.append(side_box(side, mid - 0.5, mid + 0.5, 0, 16, 1.5))
    return e


def calcified():
    """Bevelled bone plates with staggered flesh seams on every face."""
    e = [cube()]
    for side in SIDES:
        for u0, y0, u1, y1 in PLATES[side]:
            e.append(side_box(side, u0, u1, y0, y1, 1))
            e.append(side_box(side, u0 + 0.75, u1 - 0.75, y0 + 0.75, y1 - 0.75, 1.5))
    for up in (True, False):
        for x0, z0, x1, z1 in TOP_PLATES:
            e.append(on_top(x0, z0, x1, z1, up, 1))
            e.append(on_top(x0 + 0.75, z0 + 0.75, x1 - 0.75, z1 - 0.75, up, 1.5))
    return e


def write(path, data):
    path.write_text(json.dumps(data, indent=2) + '\n')


def model(elements, occlusion=True):
    """A wall model. Bone parts turn off ambient occlusion: with it, vanilla lights a part's side faces from the
    neighbouring wall they point into, which is dark; without it, every face takes its light from its cullface side."""
    out = {'parent': 'minecraft:block/block', 'textures': {'flesh': FLESH, 'bone': BONE, 'particle': FLESH},
           'elements': elements}
    if not occlusion:
        out['ambientocclusion'] = False
    return out


def main():
    wall_texture()
    write(MODELS / 'living_wall.json', model([cube()]))
    write(MODELS / 'ribbed_wall.json', model(ribbed(), occlusion=False))
    write(MODELS / 'calcified_wall.json', model(calcified(), occlusion=False))
    turns = [{'model': NS + 'living_wall'}, {'model': NS + 'living_wall', 'x': 90},
             {'model': NS + 'living_wall', 'x': 180}, {'model': NS + 'living_wall', 'x': 90, 'y': 90}]
    write(STATES / 'living_wall.json', {'variants': {'': turns}})
    write(STATES / 'ribbed_wall.json', {'variants': {'': {'model': NS + 'ribbed_wall'}}})
    plates = [dict({'model': NS + 'calcified_wall'}, **({'y': y} if y else {})) for y in (0, 90, 180, 270)]
    write(STATES / 'calcified_wall.json', {'variants': {'': plates}})
    for name in ('living_wall', 'ribbed_wall', 'calcified_wall'):
        write(ITEMS / (name + '.json'), {'parent': NS + name})
    print('walls: 1 texture, 3 models, 3 blockstates, 3 items')


if __name__ == '__main__':
    main()
