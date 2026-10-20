"""Seed Pouch: a leather sack on a root foot, its mouth drawn shut with a membrane cord, seeds showing at the rim.

    python3 art/gen_seed_pouch.py

Idle: the sack breathes slowly.
"""
from organ_model import Painter, islands, faces, pack, write, skin, folds, flat, ball, sampled, wave

SHEET = 64
IDLE = 5.0
LEATHER = [(70, 44, 30), (104, 66, 42), (138, 92, 58), (170, 120, 78), (196, 150, 104)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
CORD = [(150, 82, 92), (178, 100, 112), (204, 130, 134), (224, 160, 156)]
SEED = [(96, 110, 40), (140, 150, 60), (182, 186, 90), (220, 214, 130)]

BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [('foot', [-6, 0, -6], [12, 2, 12], None)]),
    ('sack', 'root', [0, 2, 0], [('sack', [-5, 2, -5], [10, 8, 10], None)]),
    ('neck', 'root', [0, 10, 0], [('neck', [-3, 10, -3], [6, 2, 6], None), ('cord', [-3.5, 10.5, -3.5], [7, 1, 7], None)]),
    ('rim', 'root', [0, 12, 0], [('rim', [-4, 12, -4], [8, 2, 8], None)]),
]


def textures(uv):
    p = Painter(SHEET, 401)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['foot'].items():
        p.ramp(rect, ROOT, flat(0.65) if face == 'up' else flat(0.4), grain=0.05, mottle=0.14)
    for face, rect in rects['sack'].items():
        p.ramp(rect, LEATHER, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + 0.05, mottle=0.28)
    for face, rect in rects['neck'].items():
        p.ramp(rect, LEATHER, lambda x, y, w, h: skin(x, y, w, h) - 0.1, mottle=0.2)
    for face, rect in rects['cord'].items():
        p.ramp(rect, CORD, flat(0.6), grain=0.05, mottle=0.1)
    for face, rect in rects['rim'].items():
        if face == 'up':
            p.ramp(rect, SEED, lambda x, y, w, h: 0.45 + 0.45 * ((x * 7 + y * 3) % 5 == 0) + 0.2 * ball(x, y, w, h), grain=0.06, mottle=0.15)
        else:
            p.ramp(rect, LEATHER, lambda x, y, w, h: skin(x, y, w, h) + 0.1, mottle=0.2)
    return p, rects['sack']['north']


def animations():
    idle = {
        'sack': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.02 * wave(t, IDLE), 1 - 0.015 * wave(t, IDLE), 1 + 0.02 * wave(t, IDLE)])},
        'rim': {'rotation': sampled(IDLE, 0.1, lambda t: [0, 3 * wave(t, IDLE), 0])},
    }
    return {'idle': (IDLE, True, idle)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('seed_pouch', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('seed_pouch: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
