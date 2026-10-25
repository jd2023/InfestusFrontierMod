"""The Item Capsule cell: its textures, the part models, the multipart blockstate and the item model.

    python3 art/gen_item_capsule.py

A body of cells reads as one chunky storage organ, drawn at sixteen texels a block like vanilla. A segmented bone
frame, two pixels thick, runs along every outer edge of the body and meets in a corner knuckle; inside the frame
each outer face is a mottled hide sunk one pixel, continuous across joined cells. The front is a flesh lattice of
four deep pockets a cell, lined with dark flesh; the block entity renderer draws each compartment's icon and count
in its pocket and a breathing membrane on its floor (FrontStockRenderer, whose pocket numbers live in
StorageModule and must match POCKETS here).

Every part touches its neighbours only edge to edge: the frame's raised steps stop where the carapace starts, the
frame ends where a corner knuckle begins, so no two faces share a plane.
"""
import json

from PIL import Image

from gen_reservoir import ITEMS, MODELS, NS, STATES, TEXTURES, box, turned

DIRECTIONS = ['north', 'east', 'south', 'west', 'up', 'down']
HORIZONTAL = ['north', 'east', 'south', 'west']
TEXTURE_MAP = {
    'skin': NS + 'item_capsule_skin',
    'bone': NS + 'item_capsule_bone',
    'flesh': NS + 'item_capsule_flesh',
    'lining': NS + 'item_capsule_lining',
    'particle': NS + 'item_capsule_skin',
}
AXIS = {'west': 0, 'east': 0, 'down': 1, 'up': 1, 'north': 2, 'south': 2}
FAR = {'east', 'up', 'south'}
OPPOSITE = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east', 'up': 'down', 'down': 'up'}
LOW = {0: 'west', 1: 'down', 2: 'north'}
HIGH = {0: 'east', 1: 'up', 2: 'south'}
EDGES = [(f, g) for i, f in enumerate(DIRECTIONS) for g in DIRECTIONS[i + 1:] if AXIS[f] != AXIS[g]]
CORNERS = [(x, y, z) for x in ('west', 'east') for y in ('down', 'up') for z in ('north', 'south')]

FRAME = 2   # the frame is two pixels thick and stands one pixel proud of the carapace
SINK = 1    # the carapace lies one pixel under the outer surface
# pockets, in block pixels seen from in front: two by two, five wide, floors three pixels under the lattice
POCKETS = {'starts': (2, 9), 'size': 5, 'floor': 4}

SKIN = [(50, 24, 34), (88, 46, 60), (124, 72, 86), (158, 104, 112), (192, 144, 144)]
FLESH = [(70, 28, 40), (104, 46, 60), (138, 70, 84), (170, 100, 110), (200, 140, 144)]
BONE = [(118, 98, 76), (164, 146, 114), (204, 190, 156), (230, 220, 190), (246, 240, 220)]
LINING = [(30, 8, 14), (48, 14, 22), (68, 22, 32), (92, 34, 44)]


# the carapace is a quiet mottled hide: a vein wandering down the tile, a short capillary, small domed nodules lit
# from the top left, and spiracle pores with a pale lip. Tones index SKIN; the base is 2.
VEIN = ((8, 0), (8, 1), (7, 2), (7, 3), (6, 4), (6, 5), (6, 6), (6, 7), (7, 8), (7, 9), (8, 10), (9, 11), (9, 12),
        (9, 13), (8, 14), (8, 15), (12, 2), (12, 3), (13, 4), (13, 5), (14, 6), (15, 7))
NODULES = ((2, 0), (14, 1), (3, 5), (10, 6), (1, 10), (12, 9), (4, 13), (12, 13))
PORES = ((10, 1), (0, 6), (4, 9), (14, 11))
# the front lattice is plain flesh with a few soft swellings and creases
SWELLINGS = ((2, 2), (9, 4), (5, 10), (13, 12), (11, 0))
CREASES = ((6, 6), (14, 7), (1, 13))
# bone: the outer two-pixel strip of each edge carries the frame, bevelled and notched into vertebrae
BONE_PORES = ((5, 6), (10, 9), (7, 12), (11, 4), (4, 10))
# lining: soft vertical folds, four pixels apart, with a few wet glints
FOLD = (1, 2, 2, 0)
GLINTS = ((2, 3), (9, 6), (13, 12), (6, 14))


def textures():
    """Four sixteen-pixel tiles, seamless on every edge, in a few flat tones each."""
    skin = Image.new('RGB', (16, 16), SKIN[2])
    for x, y in VEIN:
        skin.putpixel((x, y), SKIN[1])
    for x, y in NODULES:
        for (dx, dy), tone in (((0, 0), 4), ((1, 0), 3), ((0, 1), 3), ((1, 1), 1)):
            skin.putpixel(((x + dx) % 16, (y + dy) % 16), SKIN[tone])
    for x, y in PORES:
        skin.putpixel((x, y), SKIN[0])
        skin.putpixel((x, (y + 1) % 16), SKIN[3])
    skin.save(TEXTURES / 'item_capsule_skin.png', optimize=True)

    flesh = Image.new('RGB', (16, 16), FLESH[2])
    for x, y in SWELLINGS:
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            flesh.putpixel(((x + dx) % 16, (y + dy) % 16), FLESH[3])
        flesh.putpixel(((x + 1) % 16, (y + 1) % 16), FLESH[1])
    for x, y in CREASES:
        flesh.putpixel((x, y), FLESH[1])
    flesh.save(TEXTURES / 'item_capsule_flesh.png', optimize=True)

    bone = Image.new('RGB', (16, 16), BONE[2])
    for y in range(16):
        for x in range(16):
            across = min(x, 15 - x)   # distance from the left or right edge
            down = min(y, 15 - y)     # distance from the top or bottom edge
            if min(across, down) <= 1:
                # in a strip: light outside, mid inside, a dark notch every four pixels along it
                along = x if down <= 1 and across > 1 else y if across <= 1 and down > 1 else None
                tone = 3 if min(across, down) == 0 else 2
                if along is not None and along % 4 == 1:
                    tone = 1
                if across <= 1 and down <= 1:
                    tone = 4 if (across, down) == (0, 0) else 3
            else:
                tone = 2
            bone.putpixel((x, y), BONE[tone])
    for x, y in BONE_PORES:
        bone.putpixel((x, y), BONE[1])
        bone.putpixel((x, y + 1), BONE[3])
    bone.save(TEXTURES / 'item_capsule_bone.png', optimize=True)

    lining = Image.new('RGB', (16, 16))
    for y in range(16):
        for x in range(16):
            lining.putpixel((x, y), LINING[FOLD[x % 4]])
    for x, y in GLINTS:
        lining.putpixel((x, y), LINING[3])
    lining.save(TEXTURES / 'item_capsule_lining.png', optimize=True)


def region(depths, texture, faces):
    """A box given as pixel ranges measured inward from the named faces; other axes span the whole cell."""
    lo, hi = [0.0, 0.0, 0.0], [16.0, 16.0, 16.0]
    for face, (a, b) in depths.items():
        k = AXIS[face]
        lo[k], hi[k] = (16 - b, 16 - a) if face in FAR else (a, b)
    return box(tuple(lo), tuple(hi), texture, faces)


def inward(face):
    """The direction a face of a box points when it faces into the cell from {@code face}."""
    return OPPOSITE[face]


def skin(side):
    """The carapace of an outer face: a slab one pixel under the surface across the whole cell."""
    return [region({side: (SINK, SINK + 1)}, 'skin', [side])]


def along(f, g):
    return ({0, 1, 2} - {AXIS[f], AXIS[g]}).pop()


def rib(f, g, ends):
    """The frame along the edge between outer faces f and g, as two boxes that meet edge to edge: the step proud of
    f, and the step proud of g. It runs to the cell's end where the body goes on and stops at the knuckle where it
    does not; {@code ends} bit 0 is the low end joined, bit 1 the high end."""
    k = along(f, g)
    lo = 0 if ends & 1 else FRAME
    hi = 0 if ends & 2 else FRAME
    return [
        region({f: (0, SINK), g: (0, FRAME), LOW[k]: (lo, 16 - hi)}, 'bone', [f, g, inward(g)]),
        region({f: (SINK, FRAME), g: (0, SINK), LOW[k]: (lo, 16 - hi)}, 'bone', [g, inward(f)]),
    ]


def knuckle(corner):
    """The frame's corner where three outer faces meet."""
    return [region({side: (0, FRAME) for side in corner}, 'bone', list(corner))]


def front_north(mask):
    """The flesh lattice and its four pockets, each walled and floored with dark lining. Where the body ends beside
    the front ({@code mask} bits 1, 2, 4, 8: west, east, down, up joined) it stops at the carapace's plane, so no
    lattice shows in the sunken pixel above a side's carapace."""
    a, b = POCKETS['starts']
    size, floor = POCKETS['size'], POCKETS['floor']
    bars = [(0, a), (a + size, b), (b + size, 16)]
    gaps = [(a, a + size), (b, b + size)]
    elements = []
    # rows below, between and above the pockets; only the stretches over a pocket wall it above or below
    for y0, y1 in bars:
        for x0, x1 in sorted(bars + gaps):
            faces = ['north']
            if (x0, x1) in gaps:
                faces += (['up'] if y1 < 16 else []) + (['down'] if y0 > 0 else [])
            elements.append(box((x0, y0, SINK), (x1, y1, floor), 'flesh', faces))
    # posts between the pockets in each row of pockets, with walls facing the pockets beside them
    for y0, y1 in gaps:
        for x0, x1 in bars:
            faces = ['north'] + (['east'] if x1 < 16 else []) + (['west'] if x0 > 0 else [])
            elements.append(box((x0, y0, SINK), (x1, y1, floor), 'flesh', faces))
    for element in elements:
        for name, spec in element['faces'].items():
            if name != 'north':
                spec['texture'] = '#lining'
    elements.append(box((0, 0, floor), (16, 16, floor + 0.1), 'lining', ['north']))
    lo = [0 if mask & 1 else SINK, 0 if mask & 4 else SINK]
    hi = [16 if mask & 2 else 16 - SINK, 16 if mask & 8 else 16 - SINK]
    trimmed = []
    for element in elements:
        x0, y0, z0 = element['from']
        x1, y1, z1 = element['to']
        a = (max(x0, lo[0]), max(y0, lo[1]), z0)
        b = (min(x1, hi[0]), min(y1, hi[1]), z1)
        if a[0] >= b[0] or a[1] >= b[1]:
            continue
        clipped = box(a, b, 'flesh', list(element['faces']))
        for name, spec in clipped['faces'].items():
            spec['texture'] = element['faces'][name]['texture']
        trimmed.append(clipped)
    return trimmed


TURN = {'north': 'east', 'east': 'south', 'south': 'west', 'west': 'north'}


def beside(facing):
    """The world sides a front facing {@code facing} sees as its west, east, down and up."""
    quarter = HORIZONTAL.index(facing)
    west, east = 'west', 'east'
    for _ in range(quarter):
        west, east = TURN[west], TURN[east]
    return west, east, 'down', 'up'


def parts():
    out = {f'skin_{d}': skin(d) for d in DIRECTIONS}
    for mask in range(16):
        north = front_north(mask)
        for quarter, d in enumerate(HORIZONTAL):
            out[f'front_{d}_{mask}'] = [turned(json.loads(json.dumps(e)), quarter) for e in north]
    for f, g in EDGES:
        for ends in range(4):
            out[f'rib_{f}_{g}_{ends}'] = rib(f, g, ends)
    for corner in CORNERS:
        out['knuckle_' + '_'.join(corner)] = knuckle(corner)
    return out


def write_parts(models):
    for old in MODELS.glob('item_capsule_*.json'):
        old.unlink()
    for name, elements in models.items():
        model = {'textures': TEXTURE_MAP, 'elements': elements}
        (MODELS / f'item_capsule_{name}.json').write_text(json.dumps(model, indent=2) + '\n')
    # every state draws at least this, so a cell joined on all sides still has its break particles
    core = {'textures': {'particle': TEXTURE_MAP['particle']}}
    (MODELS / 'item_capsule_core.json').write_text(json.dumps(core, indent=2) + '\n')


def write_blockstate():
    def apply(name):
        return {'model': NS + 'item_capsule_' + name}
    states = [{'apply': apply('core')}]
    for d in DIRECTIONS:
        when = {d: 'false'}
        if d in HORIZONTAL:
            when['facing'] = '|'.join(o for o in HORIZONTAL if o != d)
            for mask in range(16):
                when_front = {d: 'false', 'facing': d}
                when_front.update({side: str(bool(mask & (1 << i))).lower() for i, side in enumerate(beside(d))})
                states.append({'when': when_front, 'apply': apply(f'front_{d}_{mask}')})
        states.append({'when': when, 'apply': apply(f'skin_{d}')})
    for f, g in EDGES:
        k = along(f, g)
        for ends in range(4):
            when = {f: 'false', g: 'false', LOW[k]: str(bool(ends & 1)).lower(), HIGH[k]: str(bool(ends & 2)).lower()}
            states.append({'when': when, 'apply': apply(f'rib_{f}_{g}_{ends}')})
    for corner in CORNERS:
        states.append({'when': {side: 'false' for side in corner}, 'apply': apply('knuckle_' + '_'.join(corner))})
    (STATES / 'item_capsule.json').write_text(json.dumps({'multipart': states}, indent=2) + '\n')


def write_item(models):
    """A lone cell facing north: its front, five carapaces, twelve frame edges and eight knuckles."""
    names = ['front_north_0'] + [f'skin_{d}' for d in DIRECTIONS if d != 'north'] + \
        [f'rib_{f}_{g}_0' for f, g in EDGES] + ['knuckle_' + '_'.join(c) for c in CORNERS]
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
    textures()
    models = parts()
    write_parts(models)
    write_blockstate()
    write_item(models)
    print('item_capsule: 4 textures, %d part models, 1 blockstate, 1 item model' % (len(models) + 1))


if __name__ == '__main__':
    main()
