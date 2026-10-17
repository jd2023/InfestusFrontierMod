#!/usr/bin/env python3
"""The Organ Bud's model and texture, from one description.

Run from the repository root: python3 art/gen_organ_bud.py
Writes models/block/organ_bud.json (a static vanilla model; the bud has no block entity) and
textures/block/organ_bud.png.

A dormant blank: a plum bulb with shallow folds on a dark root pad, a tighter crown and a pale tip on top where an
organ would open, three green nodules on its skin. It never moves.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from organ_model import Painter, ball, faces, flat, folds, islands, pack, skin, write_static

SHEET = 64
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
IVORY = [(112, 96, 84), (156, 140, 114), (194, 180, 148), (222, 212, 184), (240, 234, 212), (250, 248, 236)]
GREEN = [(22, 74, 50), (38, 112, 62), (64, 152, 72), (104, 192, 84), (156, 226, 108), (206, 250, 150)]
VEIN = (112, 34, 66)

BONES = [
    ('bud', None, [0, 0, 0], [
        ('pad', [-6, 0, -6], [12, 2, 12], None),
        ('bulb', [-5, 2, -5], [10, 6, 10], None),
        ('crown', [-3, 8, -3], [6, 1, 6], None),
        ('tip', [-1, 9, -1], [2, 1, 2], None),
        ('nodule', [-6, 4, -1], [1, 2, 2], None),
        ('nodule', [5, 3, 0], [1, 2, 2], None),
        ('nodule_n', [-1, 5, -6], [2, 2, 1], None),
    ]),
]


def textures(uv):
    p = Painter(SHEET, 41)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    pad = rects['pad']
    for face in ('east', 'north', 'west', 'south'):
        p.ramp(pad[face], ROOT, lambda x, y, w, h: 0.62 - 0.3 * y - 0.12 * ((x * 5 + 2) % 4 == 0))
    p.ramp(pad['up'], ROOT, flat(0.7))
    p.ramp(pad['down'], ROOT, flat(0.3), grain=0.05, mottle=0.12)
    for face, rect in rects['bulb'].items():
        if face == 'up':
            p.ramp(rect, FLESH, flat(0.72), mottle=0.3)
        elif face == 'down':
            p.ramp(rect, FLESH, flat(0.3), mottle=0.2)
        else:
            p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + 0.12, mottle=0.28)
            p.vein(rect, VEIN, 5, 170)
    for face, rect in rects['crown'].items():
        p.ramp(rect, FLESH, flat(0.58) if face != 'up' else flat(0.7), mottle=0.3)
    for face, rect in rects['tip'].items():
        p.ramp(rect, IVORY, flat(0.8) if face == 'up' else flat(0.55), grain=0.05, mottle=0.1)
    for name in ('nodule', 'nodule_n'):
        for face, rect in rects[name].items():
            p.ramp(rect, GREEN, ball, grain=0.04, mottle=0.1)
    return p


def main():
    uv = pack(BONES, SHEET)
    write_static('organ_bud', BONES, SHEET, textures(uv))
    print('organ_bud: cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
