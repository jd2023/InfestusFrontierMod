"""Tread Receptor: a substrate cell, flush with the ground, whose top is a sunken pad of nerve tissue set with nine pale
receptor bulbs in a three by three grid. At rest the pad lies 1 px down and the bulbs' tops are level with the
ground, so nothing stands proud of the block. Pressed, the pad sinks to 2 px, the bulbs go down with it and their
tops light up (NeoForge per-face light), as the Sensor Polyp's iris does. The skin is the mature substrate, the
recess walls the veins' wall; the pad and the bulbs are painted here on one 16x16 sheet.

    python3 art/gen_tread_receptor.py
"""
import json
import math
from pathlib import Path

from organ_model import Painter

ASSETS = Path('src/main/resources/assets/infestusfrontier')
NS = 'infestusfrontier:block/'
SKIN, WALL = NS + 'living_substrate_mature_5', NS + 'vein_wall'
NAME = 'tread_receptor'
PAD = [(40, 16, 46), (64, 28, 72), (92, 46, 104), (124, 72, 138)]
NERVE = (132, 88, 158)
DIM = [(70, 40, 84), (98, 62, 116), (128, 88, 146), (156, 116, 172)]
BULB = [(150, 112, 170), (186, 150, 206), (218, 190, 232), (240, 226, 248)]
SOCKET = (28, 10, 32)
LIP = [(96, 58, 100), (130, 86, 132)]
LO, HI = 3, 13  # the pad spans x and z from LO to HI
BULBS = [(x, z) for z in (4, 7, 10) for x in (4, 7, 10)]  # each bulb is 2 by 2, one pixel apart
# sheet regions: the pad floor in 0..10 x 0..10; a bulb's top at rest at 12..14 x 0..2, lit at 12..14 x 6..8; its
# sides at 12..14 x 3..5
PAD_UV, TOP_UV, LIT_UV, SIDE_UV = (0, 0, 10, 10), (12, 0, 14, 2), (12, 6, 14, 8), (12, 3, 14, 5)


def texture():
    p = Painter(16, 311)
    # the pad: dark nerve flesh, lighter towards the middle, with nerves running out from each bulb
    cx = cy = 5
    p.ramp(PAD_UV[:2] + (10, 10), PAD,
           lambda x, y, w, h: 0.55 - 0.05 * math.hypot(x + 0.5 - cx, y + 0.5 - cy), grain=0.05, mottle=0.25)
    # one nerve from each outer bulb to the middle one, drawn in the gaps between the bulbs
    rect = PAD_UV[:2] + (10, 10)
    for i in range(10):
        for x, y in ((i, 4), (i, 5), (4, i), (5, i)):
            if not (3 <= i <= 6):
                p.put(rect, x, y, NERVE)
    for d in range(3):
        for x, y in ((3 - d, 3 - d), (6 + d, 3 - d), (3 - d, 6 + d), (6 + d, 6 + d)):
            p.put(rect, x, y, NERVE)
    # a dark socket ring round each bulb, so the bulbs sit in the pad instead of on it
    for bx, bz in BULBS:
        x, y = bx - LO, bz - LO
        for dx, dy in ((-1, 0), (-1, 1), (2, 0), (2, 1), (0, -1), (1, -1), (0, 2), (1, 2)):
            p.put(rect, x + dx, y + dy, SOCKET)
    # a bulb is lit from its top-left texel; at rest dull, pressed bright
    round_ = lambda x, y, w, h: 0.85 if (x, y) == (0, 0) else 0.55 if x + y == 1 else 0.3
    p.ramp((12, 0, 2, 2), DIM, round_, grain=0.03, mottle=0.1)
    p.ramp((12, 6, 2, 2), BULB, round_, grain=0.03, mottle=0.1)
    p.ramp((12, 3, 2, 2), LIP, lambda x, y, w, h: 0.7 - 0.4 * y, grain=0.03, mottle=0.1)
    p.img.save(ASSETS / 'textures/block' / (NAME + '.png'), optimize=True)


def face(tex, uv, cull=None, lit=False):
    out = {'uv': list(uv), 'texture': tex}
    if cull:
        out['cullface'] = cull
    if lit:
        out['neoforge_data'] = {'block_light': 15, 'sky_light': 15}
    return out


def box(frm, to, faces):
    return {'from': frm, 'to': to, 'faces': faces}


def model(depth, lit):
    """depth: how far the pad lies below the top; the bulbs' tops stand 1 px above the pad."""
    floor = 16 - depth
    out = []
    # the cell below the pad, its sides full height on the skin
    sides = {d: face('#skin', (0, depth, 16, 16), d) for d in ('north', 'south', 'east', 'west')}
    out.append(box([0, 0, 0], [16, floor, 16], dict(sides, down=face('#skin', (0, 0, 16, 16), 'down'))))
    # the rim round the pad: four boxes from the pad up to the top, skin outside, vein wall inside
    for frm, to, inner in (([0, floor, 0], [16, 16, LO], 'south'), ([0, floor, HI], [16, 16, 16], 'north'),
                           ([0, floor, LO], [LO, 16, HI], 'east'), ([HI, floor, LO], [16, 16, HI], 'west')):
        faces = {'up': face('#skin', (frm[0], frm[2], to[0], to[2]), 'up')}
        for d in ('north', 'south', 'east', 'west'):
            outer = (d == 'north' and frm[2] == 0) or (d == 'south' and to[2] == 16) or \
                    (d == 'west' and frm[0] == 0) or (d == 'east' and to[0] == 16)
            if outer:
                u0, u1 = (frm[0], to[0]) if d in ('north', 'south') else (frm[2], to[2])
                faces[d] = face('#skin', (u0, 0, u1, depth), d)
            elif d == inner:
                u0, u1 = (frm[0], to[0]) if d in ('north', 'south') else (frm[2], to[2])
                faces[d] = face('#wall', (u0, 0, u1, depth))
        out.append(box(frm, to, faces))
    out.append(box([LO, floor, LO], [HI, floor, HI], {'up': face('#pad', PAD_UV)}))
    for bx, bz in BULBS:
        top = floor + 1
        faces = {'up': face('#pad', LIT_UV if lit else TOP_UV, lit=lit)}
        for d in ('north', 'south', 'east', 'west'):
            faces[d] = face('#pad', (SIDE_UV[0], SIDE_UV[1], SIDE_UV[2], SIDE_UV[1] + 1))
        out.append(box([bx, floor, bz], [bx + 2, top, bz + 2], faces))
    return {'parent': 'minecraft:block/block',
            'textures': {'skin': SKIN, 'wall': WALL, 'pad': NS + NAME, 'particle': SKIN},
            'elements': out}


def main():
    texture()
    for suffix, depth, lit in (('', 1, False), ('_pressed', 2, True)):
        (ASSETS / 'models/block' / (NAME + suffix + '.json')).write_text(
            json.dumps(model(depth, lit), indent=2) + '\n')
    (ASSETS / 'blockstates' / (NAME + '.json')).write_text(json.dumps({'variants': {
        'powered=false': {'model': NS + NAME}, 'powered=true': {'model': NS + NAME + '_pressed'}}}, indent=2) + '\n')
    (ASSETS / 'models/item' / (NAME + '.json')).write_text(json.dumps({'parent': NS + NAME}, indent=2) + '\n')
    print('tread receptor: models, blockstate, item model and texture')


if __name__ == '__main__':
    main()
