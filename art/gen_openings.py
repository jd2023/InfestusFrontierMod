"""The openings a living wall mutates into: the Membrane Window's block models and blockstate, and the Sphincter Door's
GeckoLib model, texture and animations.

    python3 art/gen_openings.py

A Membrane Window is translucent living membrane on every face of the cube with a bone bar along each of its twelve
edges. A bar shows only where neither face beside it continues into another window, so a pane of joined windows
keeps one frame round its outside and none inside. The models turn off ambient occlusion so the bars take light from
the open side, as the hardened walls do. The membrane and the bone are the Reservoir's painted textures.

A Sphincter Door is a muscular rim, a jamb on each side with bone bands, a lintel and a sill, round a doorway closed
by four membrane flaps that meet in the middle under a dark pucker. Raised folds run out from the pucker to the rim,
two on each flap, and each flap is painted with finer folds, so the closed door reads as one puckered orifice; the flaps tile the doorway exactly, and the
generator fails if any pixel between the lips is left open. Opening shrinks every flap into its outer corner, behind
the rim's fillets, with a little twist, and the pucker vanishes: the hole dilates from the centre. Closing reverses
it. The block entity plays closed or open as a loop, and opening or closing once when the door changes.
"""
import json
import math

from organ_model import Painter, faces, islands, pack, sampled, skin, folds, wave, write as write_organ
from gen_reservoir import MODELS, NS, STATES, ITEMS

# --- the window ---

MEMBRANE = NS + 'reservoir_membrane'
BONE = NS + 'reservoir_bone'
BAR = 2
PROUD = 0.05  # how far a bar's outer faces stand out of the block, in front of the membrane
DIRS = ['north', 'east', 'south', 'west', 'up', 'down']
AXIS = {'north': 'z', 'south': 'z', 'east': 'x', 'west': 'x', 'up': 'y', 'down': 'y'}
LOW = {'north': True, 'west': True, 'down': True, 'south': False, 'east': False, 'up': False}


def write(path, data):
    path.write_text(json.dumps(data, indent=2) + '\n')


def pane():
    """Membrane flush with every face, so the faces of joined windows meet with no gap."""
    faces_ = {d: {'uv': [0, 0, 16, 16], 'texture': '#membrane', 'cullface': d} for d in DIRS}
    return {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces_}


def span(direction):
    """The bar's extent on the axis of {@code direction}: hugging that face, its outer face a hair proud of the
    membrane so depth, not draw order, puts the bone in front."""
    return (-PROUD, BAR) if LOW[direction] else (16 - BAR, 16 + PROUD)


def bar(a, b):
    """The bone bar along the edge where faces {@code a} and {@code b} meet, inside the block; each face's texture is
    cut to its own size, a 2 by 16 strip along the bar and a 2 by 2 square at each end."""
    lo, hi = [0, 0, 0], [16, 16, 16]
    for d in (a, b):
        i = 'xyz'.index(AXIS[d])
        lo[i], hi[i] = span(d)
    size = [hi[i] - lo[i] for i in range(3)]
    plane = {'x': ('z', 'y'), 'y': ('x', 'z'), 'z': ('x', 'y')}
    faces_ = {}
    for d in DIRS:
        u, v = plane[AXIS[d]]
        face = {'uv': [0, 0, size['xyz'.index(u)], size['xyz'.index(v)]], 'texture': '#bone'}
        if d in (a, b):
            face['cullface'] = d
        faces_[d] = face
    return {'from': lo, 'to': hi, 'faces': faces_}


def edges():
    out = []
    for i, a in enumerate(DIRS):
        for b in DIRS[i + 1:]:
            if AXIS[a] != AXIS[b]:
                out.append((a, b))
    return out


def window_model(elements):
    return {'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent', 'ambientocclusion': False,
            'textures': {'membrane': MEMBRANE, 'bone': BONE, 'particle': MEMBRANE}, 'elements': elements}


def window():
    write(MODELS / 'membrane_window_pane.json', window_model([pane()]))
    parts = [{'apply': {'model': NS + 'membrane_window_pane'}}]
    for a, b in edges():
        name = 'membrane_window_edge_%s_%s' % (a, b)
        write(MODELS / (name + '.json'), window_model([bar(a, b)]))
        parts.append({'when': {a: 'false', b: 'false'}, 'apply': {'model': NS + name}})
    write(STATES / 'membrane_window.json', {'multipart': parts})
    return len(edges())


# --- the door: geo coordinates, +Y up, the front is north (-Z), x and z span -8..8, the doorway is two blocks high ---

SHEET = 128
FLESH = [(70, 22, 36), (104, 36, 50), (138, 56, 66), (170, 80, 86), (200, 110, 108), (226, 146, 136)]
LIP = [(60, 14, 30), (92, 26, 46), (124, 42, 62), (156, 64, 80)]
IVORY = [(168, 150, 124), (200, 184, 152), (226, 214, 186), (242, 234, 212), (252, 248, 236)]
PUCKER = [(58, 16, 30), (84, 28, 44), (112, 42, 60)]
PETAL = [(96, 30, 52), (136, 52, 74), (176, 84, 100), (208, 122, 132), (232, 166, 168)]
VEIN = (120, 28, 52)
CENTRE = (0.0, 15.0)
OPENING = (-4, 2, 4, 28)  # x0, y0, x1, y1 between the lips
# flaps: name, origin, size, the pivot at its outer corner, the twist it turns as it shrinks there
FLAPS = [
    ('flap_tl', [-4.125, 15.125, -0.5], [5, 13, 1], [-4, 28, 0], 20),
    ('flap_tr', [-0.875, 15.25, -0.75], [5, 13, 1], [4, 28, 0], -20),
    ('flap_bl', [-4.25, 1.875, -0.6], [5, 15, 1], [-4, 2, 0], -20),
    ('flap_br', [-0.75, 1.75, -0.85], [5, 15, 1], [4, 2, 0], 20),
]
OPEN_SCALE = 0.08
# raised folds on each flap's front, radiating from the pucker: degrees from the vertical, length
RIDGES = ((12, 12), (38, 6))
RIDGE_Z = -1.35
SHUT, IDLE = 0.5, 6.5


def covered():
    """True when the closed flaps cover every pixel of the doorway between the lips."""
    x0, y0, x1, y1 = OPENING
    for x in range(x0, x1):
        for y in range(y0, y1):
            if not any(o[0] <= x and x + 1 <= o[0] + sz[0] and o[1] <= y and y + 1 <= o[1] + sz[1]
                       for _, o, sz, _, _ in FLAPS):
                return False
    return True


def bones():
    frame = [
        ('jamb', [-8, 0, -3], [3, 32, 6], None),
        ('jamb', [5, 0, -3], [3, 32, 6], None),
        ('lintel', [-5, 29, -3], [10, 3, 6], None),
        ('sill', [-5, 0, -3], [10, 1, 6], None),
        ('lip_v', [-5, 1, -2], [1, 28, 4], None),
        ('lip_v', [4, 1, -2], [1, 28, 4], None),
        ('lip_h', [-4, 28, -2], [8, 1, 4], None),
        ('lip_h', [-4, 1, -2], [8, 1, 4], None),
        ('fillet', [-4, 2, -1], [2, 3, 2], None),
        ('fillet', [2, 2, -1], [2, 3, 2], None),
        ('fillet', [-4, 25, -1], [2, 3, 2], None),
        ('fillet', [2, 25, -1], [2, 3, 2], None),
        ('band', [-7.125, 6, -4], [2, 2, 8], None),
        ('band', [5.125, 6, -4], [2, 2, 8], None),
        ('band', [-7.125, 23, -4], [2, 2, 8], None),
        ('band', [5.125, 23, -4], [2, 2, 8], None),
    ]
    out = [('root', None, [0, 0, 0], []), ('frame', 'root', [0, 0, 0], frame)]
    for name, origin, size, pivot, _ in FLAPS:
        cubes = [(name, origin, size, None)]
        side = 1 if name.endswith('r') else -1  # +x for the flaps on the right of the model
        up = name[5] == 't'
        for angle, length in RIDGES:
            # the cube runs from the centre along +y (top flaps) or -y (bottom ones) and turns out into its flap;
            # geo turns about +z clockwise as seen from the front
            y0 = CENTRE[1] if up else CENTRE[1] - length
            turn = side * angle if up else -side * angle
            cubes.append(('ridge_%d' % length, [CENTRE[0] - 0.5, y0, RIDGE_Z], [1, length, 1],
                          ([0, 0, -turn], [CENTRE[0], CENTRE[1], RIDGE_Z])))
        out.append((name, 'root', pivot, cubes))
    # the pucker where the flaps meet, standing in front of and behind them; it shrinks away as the doorway dilates
    out.append(('pucker', 'root', [0, 15, 0], [('pucker', [-1, 14, -1.5], [2, 2, 3], None)]))
    return out


def pleats(cx, cy):
    """Folds radiating from the texel (cx, cy), the doorway's centre, darker towards it."""
    def value(x, y, w, h):
        dx, dy = x - cx, y - cy
        dist = math.hypot(dx, dy)
        angle = math.atan2(dy, dx)
        v = 0.5 + 0.24 * math.cos(9 * angle) * min(1.0, dist / 3) + 0.012 * dist
        return v - 0.3 * max(0.0, 1 - dist / 4)
    return value


def textures(model_bones, uv):
    p = Painter(SHEET, 307)
    sizes = islands(model_bones)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['jamb'].items():
        p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y), mottle=0.3)
        p.vein(rect, VEIN, 12)
    for name in ('lintel', 'sill'):
        for rect in rects[name].values():
            p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + 0.05, mottle=0.25)
    for name in ('lip_v', 'lip_h', 'fillet'):
        for rect in rects[name].values():
            p.ramp(rect, LIP, lambda x, y, w, h: 0.55 + 0.1 * math.sin(x + y), mottle=0.2)
    for face, rect in rects['band'].items():
        p.ramp(rect, IVORY, lambda x, y, w, h: 0.62 + 0.15 * (y == 0) - 0.12 * (y == h - 1), grain=0.04, mottle=0.1)
    for rect in rects['pucker'].values():
        p.ramp(rect, PUCKER, lambda x, y, w, h: 0.1 if 0 < x < w - 1 and 0 < y < h - 1 else 0.7, grain=0.03, mottle=0.1)
    for name in sizes:
        if name.startswith('ridge'):
            for face, rect in rects[name].items():
                crest = face in ('north', 'south')
                p.ramp(rect, PETAL, lambda x, y, w, h, c=crest: (0.78 if c else 0.3) - 0.2 * (y / max(1, h - 1)),
                       grain=0.03, mottle=0.08)
    for name, origin, size, _, _ in FLAPS:
        x0, y0 = origin[0], origin[1]
        x1, y1 = x0 + size[0], y0 + size[1]
        for face, rect in rects[name].items():
            if face == 'north':  # seen from the front, the face's left edge is the flap's +x edge
                p.ramp(rect, PETAL, pleats(x1 - CENTRE[0] - 0.5, y1 - CENTRE[1] - 0.5), grain=0.03, mottle=0.1)
                p.vein(rect, VEIN, rect[3] // 2)
            elif face == 'south':  # seen from behind, the left edge is the flap's -x edge
                p.ramp(rect, PETAL, pleats(CENTRE[0] - x0 - 0.5, y1 - CENTRE[1] - 0.5), grain=0.03, mottle=0.1)
                p.vein(rect, VEIN, rect[3] // 2)
            else:
                p.ramp(rect, LIP, lambda x, y, w, h: 0.4, mottle=0.1)
    return p, rects['jamb']['north']


def animations(model_bones):
    flaps = [name for name, _, _, _, _ in FLAPS]
    twists = {name: twist for name, _, _, _, twist in FLAPS}
    closed = {f: {'scale': sampled(IDLE, 0.25, lambda t, i=i: [1 + 0.012 * wave(t, IDLE, i * 0.4)] * 3)}
              for i, f in enumerate(flaps)}
    opened = {f: {'rotation': {'0.0': [0, 0, twists[f]]}, 'scale': {'0.0': [OPEN_SCALE] * 3}} for f in flaps}
    opening = {f: {'rotation': {'0.0': [0, 0, 0], str(SHUT): [0, 0, twists[f]]},
                   'scale': {'0.0': [1, 1, 1], str(SHUT): [OPEN_SCALE] * 3}} for f in flaps}
    closing = {f: {'rotation': {'0.0': [0, 0, twists[f]], str(SHUT): [0, 0, 0]},
                   'scale': {'0.0': [OPEN_SCALE] * 3, str(SHUT): [1, 1, 1]}} for f in flaps}
    closed['pucker'] = {'scale': {'0.0': [1, 1, 1]}}
    opened['pucker'] = {'scale': {'0.0': [0, 0, 0]}}
    opening['pucker'] = {'scale': {'0.0': [1, 1, 1], str(SHUT / 2): [0, 0, 0]}}
    closing['pucker'] = {'scale': {'0.0': [0, 0, 0], str(SHUT / 2): [0, 0, 0], str(SHUT): [1, 1, 1]}}
    return {'closed': (IDLE, True, closed), 'open': (IDLE, True, opened),
            'opening': (SHUT, False, opening), 'closing': (SHUT, False, closing)}


def door():
    assert covered(), 'the closed flaps leave part of the doorway open'
    model_bones = bones()
    painter, particle = textures(model_bones, pack(model_bones, SHEET))
    write_organ('sphincter_door', model_bones, SHEET, painter, animations(model_bones), (particle[0], particle[1]),
                bounds=(2, 3, (0, 1.5, 0)))
    write(MODELS / 'sphincter_door.json', {'textures': {'particle': NS + 'sphincter_door'}})
    write(STATES / 'sphincter_door.json', {'variants': {'': {'model': NS + 'sphincter_door'}}})
    return len(model_bones), sum(len(b[3]) for b in model_bones)


def main():
    bars = window()
    bone_count, cubes = door()
    print('openings: window pane and %d edge bars; door bones %d, cubes %d' % (bars, bone_count, cubes))


if __name__ == '__main__':
    main()
