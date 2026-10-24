"""Harvest Corolla: a flesh stem with four petal blades that open like a flower and snap shut on a ripe crop.

    python3 art/gen_harvest_corolla.py

Idle: the petals rest half open and breathe. Working: they fling wide and clap together.
"""
from organ_model import Painter, islands, faces, pack, write, skin, folds, flat, ball, sampled, wave, squeeze

SHEET = 64
IDLE, WORKING = 5.6, 2.8
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
PETAL = [(150, 60, 90), (190, 90, 120), (222, 130, 150), (244, 180, 190)]
EDGE = [(190, 180, 150), (222, 212, 184), (244, 238, 216)]

BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [('foot', [-5, 0, -5], [10, 2, 10], None)]),
    ('stem', 'root', [0, 2, 0], [('stem', [-2, 2, -2], [4, 7, 4], None)]),
    ('heart', 'stem', [0, 9, 0], [('heart', [-2.5, 9, -2.5], [5, 3, 5], None)]),
    ('petal_n', 'stem', [0, 12, -2.5], [('petal', [-2, 12, -7.5], [4, 1, 5], None)]),
    ('petal_s', 'stem', [0, 12, 2.5], [('petal', [-2, 12, 2.5], [4, 1, 5], None)]),
    ('petal_e', 'stem', [2.5, 12, 0], [('petal_x', [2.5, 12, -2], [5, 1, 4], None)]),
    ('petal_w', 'stem', [-2.5, 12, 0], [('petal_x', [-7.5, 12, -2], [5, 1, 4], None)]),
]


def textures(uv):
    p = Painter(SHEET, 419)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['foot'].items():
        p.ramp(rect, ROOT, flat(0.65) if face == 'up' else flat(0.4), grain=0.05, mottle=0.14)
    for face, rect in rects['stem'].items():
        p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y), mottle=0.25)
    for face, rect in rects['heart'].items():
        p.ramp(rect, FLESH, lambda x, y, w, h: 0.3 + 0.3 * ball(x, y, w, h), mottle=0.2)
    for name in ('petal', 'petal_x'):
        for face, rect in rects[name].items():
            if face in ('up', 'down'):
                p.ramp(rect, PETAL, lambda x, y, w, h: 0.45 + 0.4 * ball(x, y, w, h), grain=0.04, mottle=0.12)
            else:
                p.ramp(rect, EDGE, flat(0.6), grain=0.04, mottle=0.08)
    return p, rects['stem']['north']


def animations():
    idle, working = {}, {}
    for name, axis, sign in (('petal_n', 0, -1), ('petal_s', 0, 1), ('petal_e', 2, -1), ('petal_w', 2, 1)):
        def rot(angle, axis=axis):
            v = [0, 0, 0]
            v[axis] = angle
            return v
        idle[name] = {'rotation': sampled(IDLE, 0.1, lambda t, s=sign: rot(s * (35 + 5 * wave(t, IDLE))))}
        working[name] = {'rotation': sampled(WORKING, 0.05, lambda t, s=sign: rot(s * (50 - 55 * squeeze(t, WORKING))))}
    working['stem'] = {'scale': sampled(WORKING, 0.05, lambda t: [1, 1 - 0.1 * squeeze(t, WORKING, 0.15), 1])}
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('harvest_corolla', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1.5, (0, 0.5, 0)))
    print('harvest_corolla: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
