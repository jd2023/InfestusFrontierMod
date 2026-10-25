"""The Item Capsule cell: the part models, the multipart blockstate and the item model.

    python3 art/gen_item_capsule.py

A body of cells reads as one delicate storage cocoon. Every part shows only where the body ends: a flesh wall on each face that
joins no cell, the front on the facing side instead of its wall, and a bone rib along every edge between two such
faces. The front has four shallow clipped pouches with thin membrane lips; the block entity renderer draws
each compartment's icon and count in its pocket. A dedicated quiet membrane wraps the body; fine ivory ribs mark only outer edges. Walls retreat at exposed edges; ribs run along three axes with three different
cross-sections, so no two faces share a plane where parts meet.
"""
import json

from gen_reservoir import (ITEMS, MODELS, NS, STATES, box, turned)

DIRECTIONS = ['north', 'east', 'south', 'west', 'up', 'down']
HORIZONTAL = ['north', 'east', 'south', 'west']
TEXTURE_MAP = {
    'flesh': NS + 'item_capsule_membrane',
    'bone': NS + 'reservoir_bone',
    'lining': NS + 'item_capsule_lining',
    'particle': NS + 'item_capsule_membrane',
}

# Uniform eight-pixel pocket spacing continues across cell boundaries.
ADJACENT = {
    'north': ('west', 'east', 'down', 'up'), 'south': ('east', 'west', 'down', 'up'),
    'west': ('south', 'north', 'down', 'up'), 'east': ('north', 'south', 'down', 'up'),
    'up': ('west', 'east', 'north', 'south'), 'down': ('west', 'east', 'south', 'north'),
}


def orient(element, side):
    if side in HORIZONTAL:
        return turned(element, HORIZONTAL.index(side))
    def point(p):
        x,y,z = p
        return (x, 16-z, y) if side == 'up' else (x, z, 16-y)
    corners = [point(p) for p in (element['from'], element['to'])]
    lo = [min(p[k] for p in corners) for k in range(3)]
    hi = [max(p[k] for p in corners) for k in range(3)]
    mapped = {'north':'up','south':'down','up':'south','down':'north','east':'east','west':'west'} if side == 'up' else {'north':'down','south':'up','up':'north','down':'south','east':'east','west':'west'}
    return box(lo,hi,'flesh',[mapped[d] for d in element['faces']])


def wall(side, mask):
    """Only the body's outer edges turn inward; stepped surfaces have closed risers."""
    def bands(low_joined, high_joined):
        low = [(0, 1.4375, 0)] if low_joined else [(0.5, 0.9375, 0.75), (0.9375, 1.4375, 0.35)]
        high = [(14.5625, 16, 0)] if high_joined else [(14.5625, 15.0625, 0.35), (15.0625, 15.5, 0.75)]
        return low + [(1.4375, 14.5625, 0)] + high
    patches = [(u0,v0,u1,v1,0.3125+max(du,dv))
               for u0,u1,du in bands(mask & 1, mask & 2)
               for v0,v1,dv in bands(mask & 4, mask & 8)]
    elements = [box((x0,y0,z),(x1,y1,2.5),'flesh',['north']) for x0,y0,x1,y1,z in patches]
    for x0,y0,x1,y1,z in patches:
        for a,b,c,d,other in patches:
            if z == other: continue
            near, far = min(z,other), max(z,other)
            if x1 == a and y0 == b and y1 == d and min(y1,14.9375) > max(y0,1.0625):
                elements.append(box((x1,max(y0,1.0625),near),(x1,min(y1,14.9375),far),'flesh',['east' if z < other else 'west']))
            if y1 == b and x0 == a and x1 == c and min(x1,14.9375) > max(x0,1.0625):
                elements.append(box((max(x0,1.0625),y1,near),(min(x1,14.9375),y1,far),'flesh',['up' if z < other else 'down']))
    return [orient(e,side) for e in elements]


def pocket(x, y, radius):
    """A clipped-corner pouch centered in an eight-pixel module."""
    dx, dy = abs(x - 4), abs(y - 4)
    return max(dx, dy) <= radius and dx + dy <= 2 * radius - 1


def front_north():
    """Thin membrane lips around four shallow pouches; no per-cell border or bone cross."""
    runs = []
    for row in range(32):
        y = row * 0.5
        line = []
        for col in range(32):
            x = col * 0.5
            px, py = (x + 0.25) % 8, (y + 0.25) % 8
            line.append(None if pocket(px, py, 2.75) else 0.55 if pocket(px, py, 3.25) else 0.9)
        start = 0
        while start < 32:
            end = start + 1
            while end < 32 and line[end] == line[start]: end += 1
            if line[start] is not None:
                runs.append([start * 0.5, y, end * 0.5, y + 0.5, line[start]])
            start = end
    merged = []
    for x0, y0, x1, y1, depth in runs:
        previous = next((r for r in merged if r[0] == x0 and r[2] == x1 and r[3] == y0 and r[4] == depth), None)
        if previous is not None: previous[3] = y1
        else: merged.append([x0,y0,x1,y1,depth])
    elements = [box((x0,y0,depth), (x1,y1,1.75), 'flesh', ['north'] + (['west'] if x0 > 0 else []) + (['east'] if x1 < 16 else []) + (['down'] if y0 > 0 else []) + (['up'] if y1 < 16 else []))
                for x0,y0,x1,y1,depth in merged]
    elements.append(box((0,0,1.9), (16,16,2.0), 'lining', ['north']))
    return elements


# per axis of a rib: its core and its cross bars, measured inward from the edge; three axes, three sizes
SECTIONS = {
    'y': ((0.40, 1.10), (0.28, 1.22), (0.60, 0.85)),
    'x': ((0.42, 1.12), (0.30, 1.24), (0.62, 0.83)),
    'z': ((0.44, 1.14), (0.32, 1.26), (0.64, 0.81)),
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
    out = {f'wall_{d}_{mask}': wall(d, mask) for d in DIRECTIONS for mask in range(16)}
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
        for mask in range(16):
            when = {d: 'false', **{adjacent: str(bool(mask & (1 << i))).lower() for i, adjacent in enumerate(ADJACENT[d])}}
            if d in HORIZONTAL:
                when['facing'] = '|'.join(o for o in HORIZONTAL if o != d)
            parts.append({'when': when, 'apply': apply(f'wall_{d}_{mask}')})
        if d in HORIZONTAL:
            parts.append({'when': {d: 'false', 'facing': d}, 'apply': apply(f'front_{d}')})
    for f, g in EDGES:
        parts.append({'when': {f: 'false', g: 'false'}, 'apply': apply(f'rib_{f}_{g}')})
    (STATES / 'item_capsule.json').write_text(json.dumps({'multipart': parts}, indent=2) + '\n')


def write_item(models):
    """A lone cell facing north: its walls, its front and all twelve ribs."""
    names = ['front_north'] + [f'wall_{d}_0' for d in DIRECTIONS if d != 'north'] + [f'rib_{f}_{g}' for f, g in EDGES]
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
    models = parts()
    write_parts(models)
    write_blockstate()
    write_item(models)
    print('item_capsule: %d part models, 1 blockstate, 1 item model' % (len(models) + 1))


if __name__ == '__main__':
    main()
