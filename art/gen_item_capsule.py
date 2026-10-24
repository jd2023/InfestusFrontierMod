"""The Item Capsule cell: the pocket lining texture, the part models, the multipart blockstate and the item model.

    python3 art/gen_item_capsule.py

A body of cells reads as one fleshy cabinet. Every part shows only where the body ends: a flesh wall on each face that
joins no cell, the front on the facing side instead of its wall, and a bone rib along every edge between two such
faces. The front is a bone frame around four recessed pockets lined with dark flesh; the block entity renderer draws
each compartment's icon and count in its pocket. The walls and ribs share the Reservoir's flesh and bone, so the two
stores read as one organism. Walls sit half a pixel inside the block; ribs run along three axes with three different
cross-sections, so no two faces share a plane where parts meet.
"""
import json
import math
import random

from PIL import Image

from gen_reservoir import (ITEMS, MODELS, NS, SIZE, STATES, TEXTURES, box, fbm, lit, ramp, turned)

LINING = [(16, 4, 10), (32, 9, 20), (52, 16, 32), (78, 28, 46), (112, 50, 66), (150, 84, 96)]
DIRECTIONS = ['north', 'east', 'south', 'west', 'up', 'down']
HORIZONTAL = ['north', 'east', 'south', 'west']
TEXTURE_MAP = {
    'flesh': NS + 'reservoir_flesh',
    'bone': NS + 'reservoir_bone',
    'lining': NS + 'item_capsule_lining',
    'particle': NS + 'reservoir_flesh',
}

# the frame around the pockets, in pixels across the front: a 2 pixel border and a 1 pixel cross
BORDER = 2.0
DIVIDER = (7.5, 8.5)
FRAME_DEPTH = (0.5, 2.0)  # the frame's face and the pocket floor, behind the block's front face
LINING_DEPTH = 2.5


def lining():
    """Dark wet flesh: soft vertical folds, a faint mottle and a sheen on the fold crowns."""
    folds = fbm(71, [(4, 1.0), (8, 0.45)])
    mottle = fbm(73, [(16, 1.0), (32, 0.5)])
    height = [[0.9 * math.sin(math.tau * (x / SIZE * 3 + 0.6 * folds(x, y))) + 0.25 * mottle(x, y)
               for x in range(SIZE)] for y in range(SIZE)]
    light = lit(height, 1.2)
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            l = light[(x, y)]
            t = 0.12 + 0.5 * l + 0.12 * mottle(x + 7, y + 3)
            if l > 0.92:
                t += 0.2 * (l - 0.92) / 0.08
            img.putpixel((x, y), ramp(LINING, t) + (255,))
    img.save(TEXTURES / 'item_capsule_lining.png', optimize=True)


def wall(side):
    """A flesh plate half a pixel inside the face, showing only its outer side."""
    lo, hi = {
        'north': ((0, 0, 0.5), (16, 16, 1.5)),
        'south': ((0, 0, 14.5), (16, 16, 15.5)),
        'west': ((0.5, 0, 0), (1.5, 16, 16)),
        'east': ((14.5, 0, 0), (15.5, 16, 16)),
        'down': ((0, 0.5, 0), (16, 1.5, 16)),
        'up': ((0, 14.5, 0), (16, 15.5, 16)),
    }[side]
    return [box(lo, hi, 'flesh', [side], cull=[side])]


def front_north():
    """The front on the north side: a bone frame, its inner sides lining the pockets, over a dark floor."""
    z0, z1 = FRAME_DEPTH
    a, b = DIVIDER
    inner = ['north', 'east', 'west', 'up', 'down']
    pieces = [
        ((0, 0), (BORDER, 16)), ((16 - BORDER, 0), (16, 16)),  # the side borders
        ((BORDER, 0), (16 - BORDER, BORDER)), ((BORDER, 16 - BORDER), (16 - BORDER, 16)),  # bottom and top
        ((a, BORDER), (b, 16 - BORDER)),  # the upright of the cross
        ((BORDER, a), (a, b)), ((b, a), (16 - BORDER, b)),  # its arms
    ]
    elements = []
    for (x0, y0), (x1, y1) in pieces:
        # a face on the block's outer edge would only stick out past the side walls
        faces = [f for f in inner if not ((f == 'west' and x0 == 0) or (f == 'east' and x1 == 16)
                                          or (f == 'down' and y0 == 0) or (f == 'up' and y1 == 16))]
        elements.append(box((x0, y0, z0), (x1, y1, z1), 'bone', faces))
    elements.append(box((0, 0, z1), (16, 16, LINING_DEPTH), 'lining', ['north']))
    return elements


# per axis of a rib: its core and its cross bars, measured inward from the edge; three axes, three sizes
SECTIONS = {
    'y': ((-0.25, 1.75), (-0.4375, 1.9375), (0.25, 1.25)),
    'x': ((-0.1875, 1.6875), (-0.375, 1.875), (0.3125, 1.1875)),
    'z': ((-0.125, 1.625), (-0.3125, 1.8125), (0.375, 1.125)),
}
FACE_AXIS = {'north': 'z', 'south': 'z', 'west': 'x', 'east': 'x', 'down': 'y', 'up': 'y'}
FAR = {'south', 'east', 'up'}
EDGES = [(f, g) for i, f in enumerate(DIRECTIONS) for g in DIRECTIONS[i + 1:] if FACE_AXIS[f] != FACE_AXIS[g]]


def span(face, inward):
    """Block coordinates of the range {@code inward} measured in from {@code face}."""
    lo, hi = inward
    return (16 - hi, 16 - lo) if face in FAR else (lo, hi)


def rib(f, g):
    """A rounded bone bar made of five disjoint strips, with no overlapping end caps."""
    axis = ({'x', 'y', 'z'} - {FACE_AXIS[f], FACE_AXIS[g]}).pop()
    core, wide, narrow = SECTIONS[axis]
    elements = []
    sections = [(core, core)]
    for wing in ((wide[0], core[0]), (core[1], wide[1])):
        sections.extend(((wing, narrow), (narrow, wing)))
    for sf, sg in sections:
        lo, hi = [0.0, 0.0, 0.0], [16.0, 16.0, 16.0]
        for face, inward in ((f, sf), (g, sg)):
            k = 'xyz'.index(FACE_AXIS[face])
            lo[k], hi[k] = span(face, inward)
        elements.append(box(tuple(lo), tuple(hi), 'bone', DIRECTIONS))
    return elements


def parts():
    out = {f'wall_{d}': wall(d) for d in DIRECTIONS}
    north = front_north()
    for quarter, d in enumerate(HORIZONTAL):
        out[f'front_{d}'] = [turned(e, quarter) for e in north]
    for f, g in EDGES:
        out[f'rib_{f}_{g}'] = rib(f, g)
    return out


def write_parts(models):
    for name, elements in models.items():
        model = {'textures': TEXTURE_MAP, 'elements': elements}
        (MODELS / f'item_capsule_{name}.json').write_text(json.dumps(model, indent=2) + '\n')
    # every state draws at least this, so a cell joined on all sides still has its break particles
    core = {'textures': {'particle': TEXTURE_MAP['particle']}}
    (MODELS / 'item_capsule_core.json').write_text(json.dumps(core, indent=2) + '\n')


def write_blockstate():
    def apply(name):
        return {'model': NS + 'item_capsule_' + name}
    parts = [{'apply': apply('core')}]
    for d in DIRECTIONS:
        if d in HORIZONTAL:
            others = '|'.join(o for o in HORIZONTAL if o != d)
            parts.append({'when': {d: 'false', 'facing': others}, 'apply': apply(f'wall_{d}')})
            parts.append({'when': {d: 'false', 'facing': d}, 'apply': apply(f'front_{d}')})
        else:
            parts.append({'when': {d: 'false'}, 'apply': apply(f'wall_{d}')})
    for f, g in EDGES:
        parts.append({'when': {f: 'false', g: 'false'}, 'apply': apply(f'rib_{f}_{g}')})
    (STATES / 'item_capsule.json').write_text(json.dumps({'multipart': parts}, indent=2) + '\n')


def write_item(models):
    """A lone cell facing north: its walls, its front and all twelve ribs."""
    names = ['front_north'] + [f'wall_{d}' for d in DIRECTIONS if d != 'north'] + [f'rib_{f}_{g}' for f, g in EDGES]
    elements = [json.loads(json.dumps(e)) for name in names for e in models[name]]
    for element in elements:
        for spec in element['faces'].values():
            spec.pop('cullface', None)
    model = {
        'textures': TEXTURE_MAP,
        'elements': elements,
        'display': {
            'gui': {'rotation': [30, 225, 0], 'scale': [0.625, 0.625, 0.625]},
            'ground': {'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
            'fixed': {'scale': [0.5, 0.5, 0.5]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'scale': [0.4, 0.4, 0.4]},
        },
    }
    (ITEMS / 'item_capsule.json').write_text(json.dumps(model, indent=2) + '\n')


def main():
    random.seed(0)
    lining()
    models = parts()
    write_parts(models)
    write_blockstate()
    write_item(models)
    print('item_capsule: 1 texture, %d part models, 1 blockstate, 1 item model' % (len(models) + 1))


if __name__ == '__main__':
    main()
