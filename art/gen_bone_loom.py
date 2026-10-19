"""Bone Loom: a flesh base holding a bone frame, with a plate growing between two ribs.

    python3 art/gen_bone_loom.py

Idle: the ribs breathe. Working: the plate between them thickens and the ribs press in.
"""
from organ_model import Painter, islands, faces, pack, write, skin, folds, ball, flat, sampled, wave

SHEET = 64
IDLE, WORKING = 4.0, 1.0
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
IVORY = [(112, 96, 84), (156, 140, 114), (194, 180, 148), (222, 212, 184), (240, 234, 212), (250, 248, 236)]
VEIN = (112, 34, 66)

BONES = [
    ('root', None, [0, 0, 0], []),
    ('base', 'root', [0, 0, 0], [
        ('foot', [-7, 0, -7], [14, 3, 14], None),
        ('collar', [-5, 3, -5], [10, 2, 10], None),
    ]),
    ('rib_l', 'root', [-5, 5, 0], [('rib', [-6, 5, -2], [2, 7, 4], None)]),
    ('rib_r', 'root', [5, 5, 0], [('rib', [4, 5, -2], [2, 7, 4], None)]),
    ('plate', 'root', [0, 5, 0], [('plate', [-4, 6, -1], [8, 5, 2], None)]),
]


def textures(uv):
    p = Painter(SHEET, 83)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['foot'].items():
        if face == 'up':
            p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + 0.1, mottle=0.25)
            p.vein(rect, VEIN, 3, 140)
        else:
            p.ramp(rect, ROOT, flat(0.45), grain=0.05, mottle=0.15)
    for face, rect in rects['collar'].items():
        p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + 0.05, mottle=0.3)
    for face, rect in rects['rib'].items():
        p.ramp(rect, IVORY, lambda x, y, w, h: 0.55 + 0.25 * ball(x, y, w, h) - 0.1 * (face == 'down'), grain=0.06, mottle=0.12)
    for face, rect in rects['plate'].items():
        p.ramp(rect, IVORY, lambda x, y, w, h: 0.7 + 0.2 * ball(x, y, w, h), grain=0.04, mottle=0.08)
    return p, rects['foot']['north']


def animations():
    idle = {
        'rib_l': {'rotation': sampled(IDLE, 0.1, lambda t: [0, 0, 2 * wave(t, IDLE)])},
        'rib_r': {'rotation': sampled(IDLE, 0.1, lambda t: [0, 0, -2 * wave(t, IDLE)])},
    }
    working = {
        'rib_l': {'rotation': sampled(WORKING, 0.05, lambda t: [0, 0, -6 * wave(t, WORKING)])},
        'rib_r': {'rotation': sampled(WORKING, 0.05, lambda t: [0, 0, 6 * wave(t, WORKING)])},
        'plate': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.08 * wave(t, WORKING), 1 + 0.15 * wave(t, WORKING, 0.5), 1])},
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('bone_loom', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('bone_loom: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
