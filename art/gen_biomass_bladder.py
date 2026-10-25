#!/usr/bin/env python3
"""The Biomass Bladder's model, animations, textures and Blockbench source, from one description.

Run from the repository root: python3 art/gen_biomass_bladder.py
Writes geo/block/biomass_bladder.geo.json, animations/block/biomass_bladder.animation.json,
textures/block_entity/biomass_bladder.png and _glowmask.png, textures/block/biomass_bladder.png (break particle)
and art/biomass_bladder.bbmodel.

A translucent membrane sac held in an ivory rib cradle on a root slab, a fleshy cap with a bucket port on top.
The green biomass inside is its own bone, scaled to the block's fill level by the fill_0 to fill_4 animations;
the membrane breathes in the idle animation. Both are drawn translucent, the biomass before the membrane.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from organ_model import Painter, ball, bone_along, faces, flat, folds, islands, pack, sampled, wave, write

SHEET = 128
IDLE = 8.0
LEVELS = [0, 0.27, 0.52, 0.76, 1.0]
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
IVORY = [(112, 96, 84), (156, 140, 114), (194, 180, 148), (222, 212, 184), (240, 234, 212), (250, 248, 236)]
GREEN = [(22, 74, 50), (38, 112, 62), (64, 152, 72), (104, 192, 84), (156, 226, 108), (206, 250, 150)]
MEMBRANE, MEMBRANE_RIM, MEMBRANE_VEIN, GLINT = (232, 204, 176), (242, 222, 196), (176, 96, 110), (255, 250, 236)

BONES = [
    ('root', None, [0, 0, 0], []),
    ('base', 'root', [0, 0, 0], [('slab', [-7, 0, -7], [14, 2, 14], None)]),
    ('ribs', 'root', [0, 2, 0], [
        ('rib', [-7, 2, -7], [1, 10, 1], None),
        ('rib', [6, 2, -7], [1, 10, 1], None),
        ('rib', [-7, 2, 6], [1, 10, 1], None),
        ('rib', [6, 2, 6], [1, 10, 1], None),
        ('rail', [-7, 12, -7], [14, 1, 1], None),
        ('rail', [-7, 12, 6], [14, 1, 1], None),
        ('rail_side', [-7, 12, -6], [1, 1, 12], None),
        ('rail_side', [6, 12, -6], [1, 1, 12], None),
    ]),
    ('fluid', 'root', [0, 2.125, 0], [('fluid', [-5, 2.125, -5], [10, 10, 10], None)]),
    ('membrane', 'root', [0, 2, 0], [('membrane', [-6, 2, -6], [12, 11, 12], None)]),
    ('cap', 'membrane', [0, 13, 0], [
        ('cap', [-4, 13, -4], [8, 2, 8], None),
        ('port', [-2, 15, -2], [4, 1, 4], None),
    ]),
    ('pustules', 'membrane', [0, 2, 0], [
        ('pustule', [-7, 3, -1], [1, 2, 2], None),
        ('pustule', [6, 5, 1], [1, 2, 2], None),
    ]),
]


def textures(uv):
    p = Painter(SHEET, 33)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    slab = rects['slab']
    for face in ('east', 'north', 'west', 'south'):
        p.ramp(slab[face], ROOT, lambda x, y, w, h: 0.62 - 0.3 * y - 0.12 * ((x * 5 + 2) % 4 == 0))
    p.ramp(slab['up'], ROOT, flat(0.7))
    p.ramp(slab['down'], ROOT, flat(0.3), grain=0.05, mottle=0.12)
    for name, vertical in (('rib', True), ('rail', False), ('rail_side', True)):
        for face, rect in rects[name].items():
            p.ramp(rect, IVORY, bone_along(vertical), grain=0.05, mottle=0.1)
    # the biomass: a green body, brighter at the top, with a few bright specks; faintly emissive so it reads at night
    for face, rect in rects['fluid'].items():
        p.ramp(rect, GREEN, lambda x, y, w, h: 0.75 - 0.3 * y / max(1, h - 1) if face != 'up' else 0.85, grain=0.05, mottle=0.2, alpha=215)
        for _ in range(3):
            p.put(rect, p.rng.randrange(rect[2]), p.rng.randrange(rect[3]), GREEN[5], 230)
        p.glow_rect(rect, 70)
    # the membrane: thin and see-through, thicker at the rim, veined, with one glint per face
    for face, rect in rects['membrane'].items():
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                rim = x in (0, w - 1) or y in (0, h - 1)
                p.put(rect, x, y, MEMBRANE_RIM if rim else MEMBRANE, 125 if rim else 62)
        if face not in ('up', 'down'):
            for _ in range(2):
                p.vein(rect, MEMBRANE_VEIN, 7, 150)
            p.put(rect, 2, 1, GLINT, 170)
            p.put(rect, 3, 1, GLINT, 110)
    for face, rect in rects['cap'].items():
        p.ramp(rect, FLESH, lambda x, y, w, h: 0.7 - 0.25 * y / max(1, h - 1) + folds(x, y) if face != 'up' else 0.78, mottle=0.3)
    for face, rect in rects['port'].items():
        p.ramp(rect, IVORY, flat(0.8) if face == 'up' else flat(0.55), grain=0.05, mottle=0.1)
    for face, rect in rects['pustule'].items():
        p.ramp(rect, GREEN, ball, grain=0.04, mottle=0.1)
        p.glow_rect(rect, 255)
    return p, rects['cap']['north']


def animations():
    out = {'idle': (IDLE, True, {
        'membrane': {'scale': sampled(IDLE, 0.1, lambda t: [0.97 + 0.01 * wave(t, IDLE), 1, 0.97 + 0.01 * wave(t, IDLE)])},
    })}
    for n, level in enumerate(LEVELS):
        out['fill_%d' % n] = (IDLE, True, {
            'fluid': {'scale': sampled(IDLE, 0.1, lambda t, lv=level: [1 if lv else 0, lv, 1 if lv else 0])},
        })
    return out


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('biomass_bladder', BONES, SHEET, painter, animations(), (particle[0], particle[1]))
    print('biomass_bladder: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
