"""Bio-Furnace: a stomach on a flesh foot with a dark mouth on top; an ember band glows while it works.

    python3 art/gen_bio_furnace.py

Idle: the belly breathes slowly. Working: it churns, the throats pulse and the ember band shows.
Growth (block state `stage`, animations grown_0..2): stage 1 raises ridges around the belly; stage 2 adds side
lobes and a crown of chimneys. The grown bones are scaled to nothing below their stage.
"""
from organ_model import Painter, islands, faces, pack, write, skin, folds, ball, flat, sampled, wave

SHEET = 128
IDLE, WORKING = 4.0, 1.2
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
CHITIN = [(28, 18, 24), (46, 30, 36), (64, 42, 48), (84, 58, 62)]
EMBER = [(120, 36, 10), (200, 80, 18), (250, 150, 40), (255, 220, 120)]
VEIN = (112, 34, 66)

BONES = [
    ('root', None, [0, 0, 0], []),
    ('base', 'root', [0, 0, 0], [('foot', [-7, 0, -7], [14, 2, 14], None)]),
    ('belly', 'root', [0, 2, 0], [
        ('belly', [-6, 2, -6], [12, 9, 12], None),
        ('mouth', [-3, 11, -3], [6, 2, 6], None),
    ]),
    ('ember', 'root', [0, 2, 0], [('ember', [-6.5, 5, -6.5], [13, 3, 13], None)]),
    ('throat_l', 'belly', [-6, 6, 0], [('throat', [-8, 5, -2], [2, 4, 4], None)]),
    ('throat_r', 'belly', [6, 6, 0], [('throat', [6, 5, -2], [2, 4, 4], None)]),
    ('ridges', 'belly', [0, 2, 0], [
        ('ridge_x', [-7, 5, -2], [14, 2, 4], None),
        ('ridge_z', [-2, 5, -7], [4, 2, 14], None),
    ]),
    ('lobe_l', 'belly', [-6, 4, 0], [('lobe', [-8, 3, -4], [3, 6, 8], None)]),
    ('lobe_r', 'belly', [6, 4, 0], [('lobe', [5, 3, -4], [3, 6, 8], None)]),
    ('crown', 'belly', [0, 13, 0], [
        ('chimney_a', [-5, 12, -5], [3, 5, 3], None),
        ('chimney_b', [2, 12, 2], [3, 6, 3], None),
        ('chimney_c', [-5, 12, 2], [3, 4, 3], None),
        ('chimney_d', [2, 12, -5], [3, 3, 3], None),
    ]),
]
GROWN = {0: [], 1: ['ridges'], 2: ['ridges', 'lobe_l', 'lobe_r', 'crown']}


def textures(uv):
    p = Painter(SHEET, 91)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['foot'].items():
        if face == 'up':
            p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + 0.1, mottle=0.25)
            p.vein(rect, VEIN, 3, 140)
        else:
            p.ramp(rect, ROOT, flat(0.45), grain=0.05, mottle=0.15)
    for face, rect in rects['belly'].items():
        if face == 'up':
            p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + 0.15, mottle=0.2)
        else:
            p.ramp(rect, FLESH, lambda x, y, w, h: 0.25 + 0.45 * ball(x, y, w, h) + folds(x, y) * 0.5, mottle=0.3)
            p.vein(rect, VEIN, 4, 120)
    for face, rect in rects['mouth'].items():
        p.ramp(rect, CHITIN, lambda x, y, w, h: 0.2 + 0.6 * (1 - ball(x, y, w, h)) if face == 'up' else 0.45, grain=0.05, mottle=0.1)
    for face, rect in rects['ember'].items():
        p.ramp(rect, EMBER, lambda x, y, w, h: 0.35 + 0.4 * ball(x, y, w, h), grain=0.08, mottle=0.2)
        p.glow_rect(rect, 200)
    for name in ('throat', 'lobe'):
        for face, rect in rects[name].items():
            p.ramp(rect, FLESH, lambda x, y, w, h: 0.3 + 0.4 * ball(x, y, w, h), mottle=0.25)
    for name in ('ridge_x', 'ridge_z'):
        for face, rect in rects[name].items():
            p.ramp(rect, ROOT, lambda x, y, w, h: 0.4 + 0.4 * ball(x, y, w, h), grain=0.06, mottle=0.15)
    for name in ('chimney_a', 'chimney_b', 'chimney_c', 'chimney_d'):
        for face, rect in rects[name].items():
            if face == 'up':
                p.ramp(rect, CHITIN, flat(0.25), grain=0.05, mottle=0.1)
            else:
                p.ramp(rect, ROOT, lambda x, y, w, h: 0.5 + 0.3 * (1 - y / max(1, h - 1)), grain=0.06, mottle=0.15)
    return p, rects['belly']['north']


def still(scale):
    return sampled(1.0, 1.0, lambda t: [scale, scale, scale])


def animations():
    idle = {
        'belly': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.03 * wave(t, IDLE), 1 - 0.02 * wave(t, IDLE), 1 + 0.03 * wave(t, IDLE)])},
        'ember': {'scale': still(0)},
    }
    working = {
        'belly': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.07 * wave(t, WORKING), 1 - 0.05 * wave(t, WORKING), 1 + 0.07 * wave(t, WORKING)])},
        'ember': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.04 * wave(t, WORKING), 1 + 0.3 * wave(t, WORKING, 0.3), 1 + 0.04 * wave(t, WORKING)])},
        'throat_l': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.4 * wave(t, WORKING, 0.2), 1 + 0.2 * wave(t, WORKING, 0.2), 1])},
        'throat_r': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.4 * wave(t, WORKING, 0.8), 1 + 0.2 * wave(t, WORKING, 0.8), 1])},
    }
    out = {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}
    grown_bones = ['ridges', 'lobe_l', 'lobe_r', 'crown']
    for stage, shown in GROWN.items():
        out['grown_%d' % stage] = (1.0, True, {bone: {'scale': still(1 if bone in shown else 0)} for bone in grown_bones})
    return out


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('bio_furnace', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1.5, (0, 0.5, 0)))
    print('bio_furnace: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
