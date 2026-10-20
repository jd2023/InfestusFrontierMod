"""Planting Proboscis: a flesh bulb with a long curled trunk that unrolls to poke a seed into the bed.

    python3 art/gen_planting_proboscis.py

Idle: the trunk hangs curled and sways. Working: it unrolls, dips and comes back, over and over.
"""
from organ_model import Painter, islands, faces, pack, write, skin, folds, flat, ball, sampled, wave, squeeze

SHEET = 64
IDLE, WORKING = 4.0, 1.0
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
GREEN = [(40, 86, 50), (70, 130, 70), (110, 176, 100), (160, 214, 140)]
VEIN = (112, 34, 66)

BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [('foot', [-6, 0, -6], [12, 2, 12], None)]),
    ('bulb', 'root', [0, 2, 0], [('bulb', [-5, 2, -5], [10, 7, 10], None)]),
    ('trunk_a', 'root', [0, 7, -5], [('trunk_a', [-2, 5, -9], [4, 4, 4], None)]),
    ('trunk_b', 'trunk_a', [0, 5, -7], [('trunk_b', [-1.5, 1, -8.5], [3, 4, 3], None)]),
    ('tip', 'trunk_b', [0, 1, -7], [('tip', [-1, -1, -8], [2, 2, 2], None)]),
    ('crown', 'root', [0, 9, 0], [('crown', [-2, 9, -2], [4, 3, 4], None)]),
]


def textures(uv):
    p = Painter(SHEET, 409)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['foot'].items():
        p.ramp(rect, ROOT, flat(0.65) if face == 'up' else flat(0.4), grain=0.05, mottle=0.14)
    for face, rect in rects['bulb'].items():
        p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + 0.05, mottle=0.3)
        p.vein(rect, VEIN, 4, 150)
    for name in ('trunk_a', 'trunk_b', 'tip'):
        for face, rect in rects[name].items():
            p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + 0.1 - 0.15 * (name == 'tip'), mottle=0.2)
    for face, rect in rects['crown'].items():
        p.ramp(rect, GREEN, lambda x, y, w, h: 0.5 + 0.35 * ball(x, y, w, h), grain=0.05, mottle=0.12)
    return p, rects['bulb']['north']


def animations():
    idle = {
        'trunk_a': {'rotation': sampled(IDLE, 0.1, lambda t: [-20 + 4 * wave(t, IDLE), 0, 0])},
        'trunk_b': {'rotation': sampled(IDLE, 0.1, lambda t: [-70 + 5 * wave(t, IDLE, 0.5), 0, 0])},
        'crown': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.04 * wave(t, IDLE)] * 3)},
    }
    working = {
        'trunk_a': {'rotation': sampled(WORKING, 0.05, lambda t: [-20 + 45 * squeeze(t, WORKING), 0, 0])},
        'trunk_b': {'rotation': sampled(WORKING, 0.05, lambda t: [-70 + 60 * squeeze(t, WORKING, 0.1), 0, 0])},
        'tip': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.3 * squeeze(t, WORKING, 0.2)] * 3)},
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('planting_proboscis', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('planting_proboscis: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
