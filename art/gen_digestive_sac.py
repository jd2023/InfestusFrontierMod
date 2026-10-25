#!/usr/bin/env python3
"""The Digestive Sac's model, animations, textures and Blockbench source, from one description.

Run from the repository root: python3 art/gen_digestive_sac.py
Writes geo/block/digestive_sac.geo.json, animations/block/digestive_sac.animation.json,
textures/block_entity/digestive_sac.png and _glowmask.png, textures/block/digestive_sac.png (break particle)
and art/digestive_sac.bbmodel.

A stomach on a root slab: three plum belly rings stacked on a fleshy mound, a glowing green gland belt between
the lower rings, an ivory-lipped mouth on top that the player feeds, and green pustules on the skin. Idle, it
breathes slowly. Working, a contraction travels from the bottom up and opens the mouth.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from organ_model import Painter, ball, bone_along, faces, flat, folds, islands, pack, sampled, squeeze, wave, write

SHEET = 128
IDLE, WORKING = 6.0, 3.0
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
IVORY = [(112, 96, 84), (156, 140, 114), (194, 180, 148), (222, 212, 184), (240, 234, 212), (250, 248, 236)]
GREEN = [(22, 74, 50), (38, 112, 62), (64, 152, 72), (104, 192, 84), (156, 226, 108), (206, 250, 150)]
LINING = [(34, 14, 26), (50, 20, 34), (68, 28, 42), (88, 40, 52)]
VEIN = (112, 34, 66)


def claw(sx, sz):
    cx, cz = sx * 5.5, sz * 5.5
    return ('claw', [cx - 1, 0.5, cz - 2.5], [2, 2, 5], ([0, 45 * sx * sz, 0], [cx, 1.5, cz]))


BONES = [
    ('root', None, [0, 0, 0], []),
    ('base', 'root', [0, 0, 0], [
        ('slab', [-7, 0, -7], [14, 2, 14], None),
        ('mound', [-6, 2, -6], [12, 2, 12], None),
    ]),
    ('roots', 'root', [0, 0, 0], [claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1)]),
    ('belly_low', 'root', [0, 4, 0], [
        ('belly_low', [-6, 4, -6], [12, 3, 12], None),
        ('pustule', [-8, 5, 1], [2, 2, 2], None),
    ]),
    ('gland', 'root', [0, 7, 0], [('gland', [-5, 7, -5], [10, 1, 10], None)]),
    ('belly_mid', 'root', [0, 8, 0], [
        ('belly_mid', [-5, 8, -5], [10, 3, 10], None),
        ('bulge', [-3, 8.5, -6], [6, 2, 1], None),
        ('pustule', [5, 9, -1], [2, 2, 2], None),
        ('pustule', [-1, 9, 5], [2, 2, 2], None),
    ]),
    ('belly_high', 'root', [0, 11, 0], [('belly_high', [-4, 11, -4], [8, 2, 8], None)]),
    ('mouth', 'belly_high', [0, 13, 0], [('pit', [-2, 13, -2], [4, 1, 4], None)]),
    ('lip_north', 'mouth', [0, 13, -3], [('lip_long', [-3, 13, -3.2], [6, 1, 1], None)]),
    ('lip_south', 'mouth', [0, 13, 3], [('lip_long', [-3, 13, 2.2], [6, 1, 1], None)]),
    ('lip_west', 'mouth', [-3, 13, 0], [('lip_short', [-3.2, 13, -2], [1, 1, 4], None)]),
    ('lip_east', 'mouth', [3, 13, 0], [('lip_short', [2.2, 13, -2], [1, 1, 4], None)]),
]
LIPS = {'lip_north': ([-1, 0, 0]), 'lip_south': ([1, 0, 0]), 'lip_west': ([0, 0, 1]), 'lip_east': ([0, 0, -1])}


def textures(uv):
    p = Painter(SHEET, 21)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}

    slab = rects['slab']
    for face in ('east', 'north', 'west', 'south'):
        p.ramp(slab[face], ROOT, lambda x, y, w, h: 0.62 - 0.3 * y - 0.12 * ((x * 5 + 2) % 4 == 0))
    p.ramp(slab['up'], ROOT, flat(0.7))
    p.ramp(slab['down'], ROOT, flat(0.3), grain=0.05, mottle=0.12)
    for face, rect in rects['claw'].items():
        long_way = face in ('east', 'west', 'up', 'down')
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.78 if face == 'up' else 0.6 - 0.2 * y / max(1, h - 1))
               - (0.14 * ((x if long_way else 9) % 3 == 1)))
    mound = rects['mound']
    for face in ('east', 'north', 'west', 'south'):
        p.ramp(mound[face], FLESH, lambda x, y, w, h: 0.56 - 0.26 * y + folds(x, 0), mottle=0.3)
    p.ramp(mound['up'], FLESH, flat(0.7))
    p.ramp(mound['down'], ROOT, flat(0.3))

    # the belly rings: plum skin, lit from above, creased, with dark veins; the top of each ring is a lip of skin
    for name, (top, height) in (('belly_low', (0, 9)), ('belly_mid', (3, 9)), ('belly_high', (6, 9)), ('bulge', (3, 9))):
        for face, rect in rects[name].items():
            if face == 'down':
                p.ramp(rect, LINING, flat(0.5))
            elif face == 'up':
                p.ramp(rect, FLESH, lambda x, y, w, h: 0.78 - (0.12 if x in (0, w - 1) or y in (0, h - 1) else 0), mottle=0.3)
            else:
                p.ramp(rect, FLESH, lambda x, y, w, h: 0.76 - 0.3 * (y + top) / height + folds(x, y + top)
                       - (0.07 if x in (0, w - 1) and w > 3 else 0), grain=0.09, mottle=0.34)
                if rect[2] > 4:
                    for _ in range(2):
                        p.vein(rect, VEIN, 3)
    # the gland belt glows green through the crease
    for face, rect in rects['gland'].items():
        p.ramp(rect, GREEN, lambda x, y, w, h: 0.7 - 0.15 * (x % 3 == 0), grain=0.04, mottle=0.12)
        p.glow_rect(rect, 170)
    for face, rect in rects['pustule'].items():
        p.ramp(rect, GREEN, ball if face != 'down' else flat(0.3), grain=0.04, mottle=0.1)
        if face != 'down':
            p.put(rect, 0, 0, GREEN[5])
            p.glow_rect(rect, 255)
    for face, rect in rects['pit'].items():
        p.ramp(rect, LINING, flat(0.4))
    for name, vertical in (('lip_long', False), ('lip_short', True)):
        for face, rect in rects[name].items():
            p.ramp(rect, IVORY, bone_along(vertical), grain=0.05, mottle=0.1)
    return p, rects['belly_low']['north']


def animations():
    idle, working = {}, {}
    # Ring heights stay fixed at their shared seams; only the soft radial tissue contracts.
    for bone, lag in (('belly_low', 0), ('gland', 0.3), ('belly_mid', 0.45), ('belly_high', 0.9)):
        idle[bone] = {'scale': sampled(IDLE, 0.1, lambda t, lag=lag: [1 + 0.02 * wave(t, IDLE, lag), 1, 1 + 0.02 * wave(t, IDLE, lag)])}
        working[bone] = {'scale': sampled(WORKING, 0.05, lambda t, lag=lag: [1 - 0.05 * squeeze(t, WORKING, lag), 1, 1 - 0.05 * squeeze(t, WORKING, lag)])}
    for lip, axis in LIPS.items():
        idle[lip] = {'rotation': sampled(IDLE, 0.1, lambda t, a=axis: [v * 3 * wave(t, IDLE, 0.6) for v in a])}
        working[lip] = {'rotation': sampled(WORKING, 0.05, lambda t, a=axis: [v * 12 * squeeze(t, WORKING, lag=1.05) for v in a])}
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('digestive_sac', BONES, SHEET, painter, animations(), (particle[0], particle[1]))
    print('digestive_sac: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
