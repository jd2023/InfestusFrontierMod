"""The openings a living wall mutates into: the Membrane Window's block models and blockstate, and the Sphincter Door's
GeckoLib model, texture and animations.

    python3 art/gen_openings.py

A Membrane Window is translucent living membrane on every face of the cube with a bone bar along each of its twelve
edges. A bar shows only where neither face beside it continues into another window, so a pane of joined windows
keeps one frame round its outside and none inside. The models turn off ambient occlusion so the bars take light from
the open side, as the hardened walls do. The membrane and the bone are the Reservoir's painted textures.

A Sphincter Door is a muscular rim, a jamb on each side with bone bands, a lintel and a sill, round a doorway closed
by six membrane blades. Each blade hangs from a pivot on the rim and points past the centre with a slight twist, so
the closed iris reads as a spiral. Opening turns every blade further about its pivot and shrinks it into the rim;
closing reverses it. The block entity plays closed or open as a loop, and opening or closing once when the door
changes.
"""
import json
import math

from organ_model import Painter, faces, islands, pack, sampled, skin, folds, bone_along, wave, write as write_organ
from gen_reservoir import MODELS, NS, STATES, ITEMS

# --- the window ---

MEMBRANE = NS + 'reservoir_membrane'
BONE = NS + 'reservoir_bone'
BAR = 2
DIRS = ['north', 'east', 'south', 'west', 'up', 'down']
AXIS = {'north': 'z', 'south': 'z', 'east': 'x', 'west': 'x', 'up': 'y', 'down': 'y'}
LOW = {'north': True, 'west': True, 'down': True, 'south': False, 'east': False, 'up': False}


def write(path, data):
    path.write_text(json.dumps(data, indent=2) + '\n')


def pane():
    faces_ = {d: {'uv': [0, 0, 16, 16], 'texture': '#membrane', 'cullface': d} for d in DIRS}
    return {'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces_}


def span(direction):
    """The bar's extent on the axis of {@code direction}: hugging that face."""
    return (0, BAR) if LOW[direction] else (16 - BAR, 16)


def bar(a, b):
    """The bone bar along the edge where faces {@code a} and {@code b} meet, inside the block."""
    lo, hi = [0, 0, 0], [16, 16, 16]
    for d in (a, b):
        i = 'xyz'.index(AXIS[d])
        lo[i], hi[i] = span(d)
    faces_ = {}
    for d in DIRS:
        face = {'uv': [0, 0, 16, BAR] if AXIS[d] not in (AXIS[a], AXIS[b]) else [0, 0, BAR, BAR], 'texture': '#bone'}
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
PUCKER = [(30, 8, 16), (52, 14, 28), (80, 24, 42)]
PETAL = [(96, 30, 52), (136, 52, 74), (176, 84, 100), (208, 122, 132), (232, 166, 168)]
VEIN = (120, 28, 52)
CENTRE = (0.0, 15.0)
RIM = (4.5, 13.5)
# blades: angle of the pivot on the rim (degrees from +x), width
BLADES = [(90, 6), (270, 6), (30, 7), (150, 7), (210, 7), (330, 7)]
TWIST = 25
OPEN_TURN = 55
OPEN_SCALE = 0.25
SHUT, IDLE = 0.5, 4.0


def blade_cube(index, angle, width):
    px = RIM[0] * math.cos(math.radians(angle)) + CENTRE[0]
    py = RIM[1] * math.sin(math.radians(angle)) + CENTRE[1]
    dx, dy = CENTRE[0] - px, CENTRE[1] - py
    length = round(math.hypot(dx, dy) + 3)
    # the cube hangs down from its pivot; this turn points it at the centre, plus the twist of the iris
    # geo turns about +z clockwise as seen from the front, the opposite of the maths angle
    phi = -(math.degrees(math.atan2(dx, -dy)) + TWIST)
    z = -1.5 + 0.6 * index
    pivot = [round(px, 3), round(py, 3), 0]
    cube = ('blade_%d' % width + '_%d' % length, [round(px - width / 2, 3), round(py - length, 3), round(z, 3)],
            [width, length, 1], ([0, 0, round(phi, 3)], [pivot[0], pivot[1], round(z, 3)]))
    return pivot, cube


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
        ('band', [-7, 6, -4], [2, 2, 8], None),
        ('band', [5, 6, -4], [2, 2, 8], None),
        ('band', [-7, 23, -4], [2, 2, 8], None),
        ('band', [5, 23, -4], [2, 2, 8], None),
    ]
    out = [('root', None, [0, 0, 0], []), ('frame', 'root', [0, 0, 0], frame)]
    for i, (angle, width) in enumerate(BLADES):
        pivot, cube = blade_cube(i, angle, width)
        out.append(('blade_%d' % i, 'root', pivot, [cube]))
    # the pucker where the blades meet, in front of and behind them; it shrinks away as the iris opens
    out.append(('pucker', 'root', [0, 15, 0], [('pucker', [-1, 14, -2.5], [2, 2, 5], None)]))
    return out


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
        p.ramp(rect, PUCKER, lambda x, y, w, h: 0.3, grain=0.03, mottle=0.1)
    for name in sizes:
        if not name.startswith('blade'):
            continue
        for face, rect in rects[name].items():
            if face in ('north', 'south'):
                # dark under the leading edge where the next blade overlaps it, a light crest along the trailing one
                lead = 0 if face == 'north' else None

                def petal(x, y, w, h, flip=(face == 'south')):
                    t = (w - 1 - x if flip else x) / max(1, w - 1)
                    v = 0.25 + 0.55 * t + 0.08 * (y / max(1, h - 1))
                    if t == 0:
                        v = 0.05
                    elif t >= 0.99:
                        v = 0.95
                    return v
                p.ramp(rect, PETAL, petal, grain=0.04, mottle=0.12, alpha=245)
                p.vein(rect, VEIN, rect[3] // 2, alpha=245)
            else:
                p.ramp(rect, LIP, lambda x, y, w, h: 0.4, mottle=0.1)
    return p, rects['jamb']['north']


def blade_keys(turn, scale):
    return {'rotation': {'0.0': [0, 0, turn]}, 'scale': {'0.0': [scale, scale, scale]}}


def animations(model_bones):
    blades = [name for name, _, _, _ in model_bones if name.startswith('blade')]  # the pucker follows them
    closed = {b: {'scale': sampled(IDLE, 0.25, lambda t, i=i: [1 + 0.015 * wave(t, IDLE, i * 0.3)] * 3)}
              for i, b in enumerate(blades)}
    opened = {b: blade_keys(OPEN_TURN, OPEN_SCALE) for b in blades}
    closed['pucker'] = {'scale': {'0.0': [1, 1, 1]}}
    opened['pucker'] = {'scale': {'0.0': [0, 0, 0]}}
    opening = {b: {'rotation': {'0.0': [0, 0, 0], str(SHUT): [0, 0, OPEN_TURN]},
                   'scale': {'0.0': [1, 1, 1], str(SHUT): [OPEN_SCALE] * 3}} for b in blades}
    closing = {b: {'rotation': {'0.0': [0, 0, OPEN_TURN], str(SHUT): [0, 0, 0]},
                   'scale': {'0.0': [OPEN_SCALE] * 3, str(SHUT): [1, 1, 1]}} for b in blades}
    opening['pucker'] = {'scale': {'0.0': [1, 1, 1], str(SHUT / 2): [0, 0, 0]}}
    closing['pucker'] = {'scale': {'0.0': [0, 0, 0], str(SHUT): [1, 1, 1]}}
    return {'closed': (IDLE, True, closed), 'open': (IDLE, True, opened),
            'opening': (SHUT, False, opening), 'closing': (SHUT, False, closing)}


def door():
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
