"""Activation Cyst: a flesh bulb on a root foot with a window on each flank, amber for the source chamber and green
for the carrier chamber, and a throat on top.

    python3 art/gen_activation_cyst.py

Idle: the bulb breathes and the windows glow faintly. Working: the two chambers pulse in turn and the throat squeezes.
"""
from organ_model import Painter, islands, faces, pack, write, skin, folds, ball, flat, sampled, wave, squeeze

SHEET = 64
IDLE, WORKING = 6.4, 3.2
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
AMBER = [(96, 62, 24), (150, 104, 36), (204, 150, 56), (236, 196, 110), (250, 232, 180)]
GREEN = [(32, 76, 44), (62, 128, 70), (104, 182, 104), (166, 222, 150), (222, 248, 206)]
VEIN = (112, 34, 66)

BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [('foot', [-7, 0, -7], [14, 2, 14], None)]),
    ('body', 'root', [0, 2, 0], [('body', [-5, 2, -5], [10, 8, 10], None)]),
    ('source', 'body', [-5, 6, 0], [('amber', [-7, 4, -3], [2, 5, 6], None)]),
    ('carrier', 'body', [5, 6, 0], [('green', [5, 4, -3], [2, 5, 6], None)]),
    ('throat', 'body', [0, 10, 0], [('throat', [-2, 10, -2], [4, 3, 4], None)]),
]


def textures(uv):
    p = Painter(SHEET, 131)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['foot'].items():
        p.ramp(rect, ROOT, flat(0.65) if face == 'up' else flat(0.4), grain=0.05, mottle=0.14)
    for face, rect in rects['body'].items():
        if face == 'up':
            p.ramp(rect, FLESH, flat(0.5), mottle=0.2)
        else:
            p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + 0.05, mottle=0.3)
            p.vein(rect, VEIN, 4, 150)
    for name, ramp in (('amber', AMBER), ('green', GREEN)):
        for face, rect in rects[name].items():
            p.ramp(rect, ramp, lambda x, y, w, h: 0.45 + 0.4 * ball(x, y, w, h), grain=0.03, mottle=0.1)
            p.glow_rect(rect, 110)
    for face, rect in rects['throat'].items():
        p.ramp(rect, FLESH, flat(0.15) if face == 'up' else lambda x, y, w, h: skin(x, y, w, h) - 0.1, mottle=0.2)
    return p, rects['body']['north']


def animations():
    idle = {
        'body': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.02 * wave(t, IDLE), 1 + 0.03 * wave(t, IDLE), 1 + 0.02 * wave(t, IDLE)])},
        'source': {'scale': sampled(IDLE, 0.1, lambda t: [1, 1 + 0.04 * wave(t, IDLE), 1 + 0.04 * wave(t, IDLE)])},
        'carrier': {'scale': sampled(IDLE, 0.1, lambda t: [1, 1 + 0.04 * wave(t, IDLE, IDLE / 2), 1 + 0.04 * wave(t, IDLE, IDLE / 2)])},
    }
    working = {
        'source': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.25 * squeeze(t, WORKING), 1 + 0.12 * squeeze(t, WORKING), 1 + 0.12 * squeeze(t, WORKING)])},
        'carrier': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.25 * squeeze(t, WORKING, WORKING / 2), 1 + 0.12 * squeeze(t, WORKING, WORKING / 2), 1 + 0.12 * squeeze(t, WORKING, WORKING / 2)])},
        'throat': {'scale': sampled(WORKING, 0.05, lambda t: [1 - 0.15 * squeeze(t, WORKING, WORKING / 4), 1 + 0.2 * squeeze(t, WORKING, WORKING / 4), 1 - 0.15 * squeeze(t, WORKING, WORKING / 4)])},
        'body': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.03 * wave(t, WORKING), 1 - 0.03 * wave(t, WORKING), 1 + 0.03 * wave(t, WORKING)])},
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('activation_cyst', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('activation_cyst: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
