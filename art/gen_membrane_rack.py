"""Membrane Rack: two bone posts and a crossbar on a flesh foot, a skin stretched between them.

    python3 art/gen_membrane_rack.py

Writes geo, animations, texture, glowmask and bbmodel for the rack. Idle: the skin breathes. Working: the skin
pulls taut and a drop of water falls from it.
"""
from organ_model import Painter, islands, faces, pack, write, skin, folds, ball, flat, sampled, wave

SHEET = 64
IDLE, WORKING = 7.0, 3.0
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
IVORY = [(112, 96, 84), (156, 140, 114), (194, 180, 148), (222, 212, 184), (240, 234, 212), (250, 248, 236)]
WATER = [(28, 58, 118), (58, 118, 198), (120, 188, 240), (204, 236, 255)]
VEIN = (112, 34, 66)

BONES = [
    ('root', None, [0, 0, 0], []),
    ('frame', 'root', [0, 0, 0], [
        ('foot', [-7, 0, -3], [14, 1, 6], None),
        ('post', [-7, 1, -1], [2, 13, 2], None),
        ('post', [5, 1, -1], [2, 13, 2], None),
        ('bar', [-5, 13, -1], [10, 1, 2], None),
    ]),
    ('skin', 'root', [0, 13, 0], [('sheet', [-5, 4, -0.5], [10, 9, 1], None)]),
    ('drop', 'root', [0, 3, 0], [('drip', [-0.5, 3, 0.5], [1, 1, 1], None)]),
]


def textures(uv):
    p = Painter(SHEET, 71)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['foot'].items():
        p.ramp(rect, ROOT, flat(0.72) if face == 'up' else flat(0.4), grain=0.05, mottle=0.12)
    for name in ('post', 'bar'):
        for face, rect in rects[name].items():
            p.ramp(rect, IVORY, lambda x, y, w, h: 0.62 + 0.18 * ball(x, y, w, h) - 0.1 * (face == 'down'), grain=0.06, mottle=0.1)
    for face, rect in rects['sheet'].items():
        if face in ('north', 'south'):
            p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + 0.15, mottle=0.25, alpha=225)
            p.vein(rect, VEIN, 4, 150)
        else:
            p.ramp(rect, FLESH, flat(0.45), alpha=225)
    for face, rect in rects['drip'].items():
        p.ramp(rect, WATER, ball, grain=0.03, alpha=230)
        p.glow_rect(rect, 90)
    return p, rects['post']['north']


def animations():
    idle = {
        'skin': {'scale': sampled(IDLE, 0.1, lambda t: [0.96 + 0.01 * wave(t, IDLE), 0.96 + 0.01 * wave(t, IDLE, 0.3), 1])},
        'drop': {'scale': sampled(IDLE, 0.1, lambda t: [0, 0, 0])},
    }
    working = {
        'skin': {'scale': sampled(WORKING, 0.05, lambda t: [0.95 + 0.02 * wave(t, WORKING), 0.95 - 0.02 * wave(t, WORKING), 1])},
        'drop': {
            'position': sampled(WORKING, 0.05, lambda t: [0, -2.0 * min(1, (t / WORKING / 0.8) ** 2), 0]),
            'scale': sampled(WORKING, 0.05, lambda t: [drop_size(t / WORKING)] * 3),
        },
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def drop_size(phase):
    """Grow, fall, shrink into the foot, then reset only while invisible."""
    if phase < 0.2:
        u = phase / 0.2
        return u * u * (3 - 2 * u)
    if phase < 0.65:
        return 1
    if phase < 0.8:
        u = (phase - 0.65) / 0.15
        return 1 - u * u * (3 - 2 * u)
    return 0


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('membrane_rack', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 2, (0, 0.5, 0)))
    print('membrane_rack: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
