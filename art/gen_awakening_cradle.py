"""Awakening Cradle: a flesh cup with two lobes that fold around the piece inside, and a pale core.

    python3 art/gen_awakening_cradle.py

Idle: the lobes rest open and breathe. Working: they fold in over the core and the core glows.
"""
from organ_model import Painter, islands, faces, pack, write, skin, folds, ball, flat, sampled, wave

SHEET = 64
IDLE, WORKING = 8.0, 5.0
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
IVORY = [(112, 96, 84), (156, 140, 114), (194, 180, 148), (222, 212, 184), (240, 234, 212), (250, 248, 236)]
CORE = [(96, 60, 96), (140, 90, 150), (190, 140, 200), (226, 196, 236), (246, 232, 250)]
VEIN = (112, 34, 66)

BONES = [
    ('root', None, [0, 0, 0], []),
    ('cup', 'root', [0, 0, 0], [
        ('foot', [-7, 0, -7], [14, 2, 14], None),
        ('bowl', [-6, 2, -6], [12, 4, 12], None),
    ]),
    ('lobe_l', 'root', [-6, 6, 0], [('lobe', [-6, 6, -5], [3, 5, 10], None)]),
    ('lobe_r', 'root', [6, 6, 0], [('lobe', [3, 6, -5], [3, 5, 10], None)]),
    ('core', 'root', [0, 6, 0], [('core', [-1, 6, -2], [2, 4, 4], None)]),
]


def textures(uv):
    p = Painter(SHEET, 97)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['foot'].items():
        p.ramp(rect, ROOT, flat(0.65) if face == 'up' else flat(0.4), grain=0.05, mottle=0.14)
    for name in ('bowl', 'lobe'):
        for face, rect in rects[name].items():
            if face == 'up' and name == 'bowl':
                p.ramp(rect, FLESH, flat(0.3), mottle=0.2)
            else:
                p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + 0.05, mottle=0.3)
                p.vein(rect, VEIN, 3, 150)
    for face, rect in rects['core'].items():
        p.ramp(rect, CORE, lambda x, y, w, h: 0.5 + 0.4 * ball(x, y, w, h), grain=0.03, mottle=0.1)
        p.glow_rect(rect, 140)
    return p, rects['bowl']['north']


def animations():
    idle = {
        'lobe_l': {'rotation': sampled(IDLE, 0.1, lambda t: [0, 0, -25 + 3 * wave(t, IDLE)])},
        'lobe_r': {'rotation': sampled(IDLE, 0.1, lambda t: [0, 0, 25 - 3 * wave(t, IDLE)])},
        'core': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.03 * wave(t, IDLE), 1 + 0.03 * wave(t, IDLE), 1 + 0.03 * wave(t, IDLE)])},
    }
    working = {
        'lobe_l': {'rotation': sampled(WORKING, 0.05, lambda t: [0, 0, 10 + 6 * wave(t, WORKING)])},
        'lobe_r': {'rotation': sampled(WORKING, 0.05, lambda t: [0, 0, -10 - 6 * wave(t, WORKING)])},
        'core': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.15 * wave(t, WORKING, 0.5)] * 3)},
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('awakening_cradle', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('awakening_cradle: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
