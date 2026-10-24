"""Vascular Junction: a flesh hub with a mouth on each of its six faces. A closed mouth is a shrunken bud; an intake
mouth gapes and draws in; an outlet mouth pushes out in pulses.

    python3 art/gen_vascular_junction.py

Animations: idle (the hub breathes) and, for each face, <face>_closed, <face>_intake, <face>_outlet; the block entity
runs one controller per face and picks by the face's port in the block state.
"""
from organ_model import Painter, islands, faces, pack, write, skin, folds, flat, sampled, wave, squeeze

SHEET = 64
IDLE, PULSE = 7.0, 2.5
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
LIP = [(120, 40, 70), (160, 70, 96), (196, 110, 126), (222, 150, 156)]
VEIN = (112, 34, 66)

# mouth: name, pivot on the hub face, cube origin, size
MOUTHS = {
    'up': ([0, 12, 0], [-2.5, 12, -2.5], [5, 3, 5]),
    'down': ([0, 2, 0], [-3, 0, -3], [6, 2, 6]),
    'north': ([0, 7, -5], [-2.5, 4.5, -7], [5, 5, 2]),
    'south': ([0, 7, 5], [-2.5, 4.5, 5], [5, 5, 2]),
    'east': ([5, 7, 0], [5, 4.5, -2.5], [2, 5, 5]),
    'west': ([-5, 7, 0], [-7, 4.5, -2.5], [2, 5, 5]),
}

BONES = [('root', None, [0, 0, 0], []), ('hub', 'root', [0, 2, 0], [('hub', [-5, 2, -5], [10, 10, 10], None)])]
for name, (pivot, origin, size) in MOUTHS.items():
    BONES.append(('mouth_' + name, 'hub', pivot, [('mouth_' + name, origin, size, None)]))


def textures(uv):
    p = Painter(SHEET, 211)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['hub'].items():
        if face == 'up':
            p.ramp(rect, FLESH, flat(0.5), mottle=0.2)
        elif face == 'down':
            p.ramp(rect, ROOT, flat(0.4), grain=0.05, mottle=0.14)
        else:
            p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + 0.05, mottle=0.3)
            p.vein(rect, VEIN, 5, 150)
    for name in MOUTHS:
        outward = {'up': 'up', 'down': 'down', 'north': 'north', 'south': 'south', 'east': 'east', 'west': 'west'}[name]
        for face, rect in rects['mouth_' + name].items():
            if face == outward:
                p.ramp(rect, LIP, lambda x, y, w, h: 0.75 - 0.6 * (abs(x - (w - 1) / 2) < w / 4 and abs(y - (h - 1) / 2) < h / 4), grain=0.04, mottle=0.1)
            else:
                p.ramp(rect, LIP, lambda x, y, w, h: skin(x, y, w, h) - 0.05, mottle=0.2)
    return p, rects['hub']['north']


def axis(name):
    """Scale vector along the mouth's own axis: (x, y, z) weights."""
    return {'up': (0, 1, 0), 'down': (0, 1, 0), 'north': (0, 0, 1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (1, 0, 0)}[name]


def animations():
    out = {'idle': (IDLE, True, {'hub': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.02 * wave(t, IDLE), 1 + 0.03 * wave(t, IDLE), 1 + 0.02 * wave(t, IDLE)])}})}
    for index, name in enumerate(MOUTHS):
        bone = 'mouth_' + name
        ax = axis(name)
        lag = index * PULSE / len(MOUTHS)

        def closed(t, ax=ax):
            return [0.45 if ax[i] == 0 else 0.3 for i in range(3)]

        def intake(t, ax=ax, lag=lag):
            s = squeeze(t, PULSE, lag)
            return [1 + 0.3 * s if ax[i] == 0 else 1 - 0.25 * s for i in range(3)]

        def outlet(t, ax=ax, lag=lag):
            s = squeeze(t, PULSE, lag)
            return [1 - 0.15 * s if ax[i] == 0 else 1 + 0.15 * s for i in range(3)]

        out[name + '_closed'] = (PULSE, True, {bone: {'scale': sampled(PULSE, 0.5, closed)}})
        out[name + '_intake'] = (PULSE, True, {bone: {'scale': sampled(PULSE, 0.05, intake)}})
        out[name + '_outlet'] = (PULSE, True, {bone: {'scale': sampled(PULSE, 0.05, outlet)}})
    return out


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('vascular_junction', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('vascular_junction: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
