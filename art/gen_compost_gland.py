"""Compost Gland: a squat fermentation pouch, open scrap hopper and pale fertilizer outlet.

Run from the repository root: python3 art/gen_compost_gland.py.
The wall contracts slowly while the outlet presses out; the hopper follows its parent tissue.
"""
from organ_model import Painter, ball, faces, folds, islands, pack, sampled, skin, squeeze, wave, write

SHEET = 64
IDLE, WORKING = 9.0, 4.8
FLESH = [(56, 38, 28), (86, 58, 38), (118, 82, 50), (150, 110, 70), (184, 148, 98)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68)]
SCRAPS = [(36, 44, 20), (66, 78, 32), (100, 114, 48), (146, 150, 74)]
IVORY = [(112, 96, 84), (156, 140, 114), (194, 180, 148), (222, 212, 184)]
BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [('foot', [-6, 0, -5], [12, 2, 10], None)]),
    ('pouch', 'root', [0, 2, 0], [('pouch', [-5, 2, -4], [10, 6, 8], None)]),
    ('hopper', 'pouch', [0, 8, 0], [
        ('lip_ns', [-4, 8, -3], [8, 2, 1], None),
        ('lip_ns', [-4, 8, 2], [8, 2, 1], None),
        ('lip_ew', [-4, 8, -2], [1, 2, 4], None),
        ('lip_ew', [3, 8, -2], [1, 2, 4], None),
        ('scraps', [-3, 8, -2], [6, 1, 4], None),
    ]),
    ('outlet', 'pouch', [0, 4, -4], [('outlet', [-2, 3, -6], [4, 2, 2], None)]),
]


def textures(uv):
    p = Painter(SHEET, 433)
    for name, size in islands(BONES).items():
        for face, rect in faces(size, uv[name]).items():
            palette = ROOT if name == 'foot' else SCRAPS if name == 'scraps' else IVORY if name == 'outlet' else FLESH
            p.ramp(rect, palette, lambda x, y, w, h: 0.45 * skin(x, y, w, h) + 0.45 * ball(x, y, w, h)
                   + 0.5 * folds(x, y), grain=0.04, mottle=0.18)
    return p


def animations():
    idle = {'pouch': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.012 * wave(t, IDLE), 1, 1])}}
    working = {
        'pouch': {'scale': sampled(WORKING, 0.05, lambda t: [1 - 0.04 * squeeze(t, WORKING), 1, 1 + 0.025 * squeeze(t, WORKING)])},
        'outlet': {'scale': sampled(WORKING, 0.05, lambda t: [1, 1, 1 + 0.18 * squeeze(t, WORKING, 1.2)])},
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    uv = pack(BONES, SHEET)
    write('compost_gland', BONES, SHEET, textures(uv), animations(), uv['pouch'], bounds=(2, 1, (0, 0.25, 0)))


if __name__ == '__main__':
    main()
