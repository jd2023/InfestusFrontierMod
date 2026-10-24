#!/usr/bin/env python3
"""The Culture Bowl's model, animations, textures and Blockbench source, from one description.

Run from the repository root: python3 art/gen_culture_bowl.py
Writes geo/block/culture_bowl.geo.json, animations/block/culture_bowl.animation.json,
textures/block_entity/culture_bowl.png and _glowmask.png, textures/block/culture_bowl.png (break particle)
and art/culture_bowl.bbmodel.

A low basin on a root slab: four fleshy walls with an ivory lip, a shallow pool of culture inside that glows
faintly, and three bubbles on its surface. Idle, the pool breathes over four seconds. Working, the bubbles bob
once a second and the pool sways.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from organ_model import Painter, ball, faces, flat, folds, islands, pack, sampled, skin, wave, write

SHEET = 64
IDLE, WORKING = 6.0, 3.6
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
IVORY = [(112, 96, 84), (156, 140, 114), (194, 180, 148), (222, 212, 184), (240, 234, 212), (250, 248, 236)]
GREEN = [(22, 74, 50), (38, 112, 62), (64, 152, 72), (104, 192, 84), (156, 226, 108), (206, 250, 150)]
CULTURE = [(58, 74, 60), (92, 118, 78), (132, 160, 96), (172, 198, 118), (206, 226, 150), (234, 246, 196)]
VEIN = (112, 34, 66)

BONES = [
    ('root', None, [0, 0, 0], []),
    ('basin', 'root', [0, 0, 0], [
        ('slab', [-7, 0, -7], [14, 2, 14], None),
        ('wall_ns', [-7, 2, -7], [14, 4, 1], None),
        ('wall_ns', [-7, 2, 6], [14, 4, 1], None),
        ('wall_ew', [-7, 2, -6], [1, 4, 12], None),
        ('wall_ew', [6, 2, -6], [1, 4, 12], None),
    ]),
    ('pool', 'root', [0, 2, 0], [('pool', [-6, 2, -6], [12, 3, 12], None)]),
]
for n, (x, z) in enumerate(((-3, -2), (2, 1), (0, 3))):
    BONES.append((f'bubble_{n}', 'pool', [x + 0.5, 5, z + 0.5], [('bubble', [x, 5, z], [1, 1, 1], None)]))


def textures(uv):
    p = Painter(SHEET, 57)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    slab = rects['slab']
    for face in ('east', 'north', 'west', 'south'):
        p.ramp(slab[face], ROOT, lambda x, y, w, h: 0.62 - 0.3 * y - 0.12 * ((x * 5 + 2) % 4 == 0))
    p.ramp(slab['up'], ROOT, flat(0.7))
    p.ramp(slab['down'], ROOT, flat(0.3), grain=0.05, mottle=0.12)
    for wall in ('wall_ns', 'wall_ew'):
        for face, rect in rects[wall].items():
            if face == 'up':
                p.ramp(rect, IVORY, flat(0.78), grain=0.05, mottle=0.1)
            elif face == 'down':
                p.ramp(rect, FLESH, flat(0.3), mottle=0.2)
            else:
                p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y), mottle=0.28)
                p.vein(rect, VEIN, 3, 160)
    # the pool: pale culture broth, brighter in the middle of the surface, faintly emissive so it reads at night
    for face, rect in rects['pool'].items():
        if face == 'up':
            p.ramp(rect, CULTURE, lambda x, y, w, h: 0.55 + 0.35 * ball(x, y, w, h), grain=0.05, mottle=0.2, alpha=220)
            for _ in range(5):
                p.put(rect, p.rng.randrange(rect[2]), p.rng.randrange(rect[3]), CULTURE[5], 235)
        else:
            p.ramp(rect, CULTURE, lambda x, y, w, h: 0.5 - 0.2 * y / max(1, h - 1), grain=0.05, mottle=0.2, alpha=200)
        p.glow_rect(rect, 60)
    for face, rect in rects['bubble'].items():
        p.ramp(rect, GREEN, ball, grain=0.04, mottle=0.1)
        p.glow_rect(rect, 200)
    return p, rects['wall_ns']['north']


def animations():
    idle = {
        'pool': {'scale': sampled(IDLE, 0.1, lambda t: [1, 1 + 0.02 * wave(t, IDLE), 1])},
    }
    working = {
        'pool': {'scale': sampled(WORKING, 0.05, lambda t: [1, 1 + 0.04 * wave(t, WORKING), 1])},
    }
    for n in range(3):
        idle[f'bubble_{n}'] = {'scale': sampled(IDLE, 0.1, lambda t, lag=n * IDLE / 3: [0.8 + 0.1 * wave(t, IDLE, lag)] * 3)}
        working[f'bubble_{n}'] = {'scale': sampled(WORKING, 0.05, lambda t, lag=n * WORKING / 3: [0.75 + 0.35 * wave(t, WORKING, lag)] * 3)}
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('culture_bowl', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('culture_bowl: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
