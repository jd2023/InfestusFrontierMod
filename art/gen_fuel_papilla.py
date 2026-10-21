"""Fuel Papilla: a substrate cell, flush with the ground, with a pore sunk 3 px into its top. The pore is floored with
the flowing biomass lumen and holds a pale nipple whose tip stays 1 px below the surface, so nothing stands proud of
the block. Skin, lumen and wall are the veins' textures; the nipple is the Mouth's ivory lip.

    python3 art/gen_fuel_papilla.py
"""
import json
from pathlib import Path

ASSETS = Path('src/main/resources/assets/infestusfrontier')
NS = 'infestusfrontier:block/'
SKIN, LUMEN, WALL, IVORY = (NS + 'living_substrate_mature_5', NS + 'vein_lumen_biomass', NS + 'vein_wall',
                            NS + 'mouth_lip_bone')
LO, HI, FLOOR = 4, 12, 13


def box(frm, to, faces):
    return {'from': frm, 'to': to, 'faces': faces}


def face(tex, uv, cull=None):
    out = {'uv': uv, 'texture': tex}
    if cull:
        out['cullface'] = cull
    return out


def main():
    sides = {d: face('#skin', [0, 0, 16, 16], d) for d in ('north', 'south', 'east', 'west', 'down')}
    base = box([0, 0, 0], [16, FLOOR, 16], dict(sides, up=face('#lumen', [LO, LO, HI, HI])))
    for d in ('north', 'south', 'east', 'west'):
        base['faces'][d] = face('#skin', [0, 16 - FLOOR, 16, 16], d)
    ring = [
        box([0, FLOOR, 0], [16, 16, LO], {'up': face('#skin', [0, 0, 16, LO], 'up'),
                                          'north': face('#skin', [0, 0, 16, 3], 'north'),
                                          'east': face('#skin', [16 - LO, 0, 16, 3], 'east'),
                                          'west': face('#skin', [0, 0, LO, 3], 'west'),
                                          'south': face('#wall', [LO, 0, HI, 3])}),
        box([0, FLOOR, HI], [16, 16, 16], {'up': face('#skin', [0, HI, 16, 16], 'up'),
                                           'south': face('#skin', [0, 0, 16, 3], 'south'),
                                           'east': face('#skin', [0, 0, LO, 3], 'east'),
                                           'west': face('#skin', [HI, 0, 16, 3], 'west'),
                                           'north': face('#wall', [LO, 0, HI, 3])}),
        box([0, FLOOR, LO], [LO, 16, HI], {'up': face('#skin', [0, LO, LO, HI], 'up'),
                                           'west': face('#skin', [LO, 0, HI, 3], 'west'),
                                           'east': face('#wall', [LO, 0, HI, 3])}),
        box([HI, FLOOR, LO], [16, 16, HI], {'up': face('#skin', [HI, LO, 16, HI], 'up'),
                                            'east': face('#skin', [LO, 0, HI, 3], 'east'),
                                            'west': face('#wall', [LO, 0, HI, 3])}),
    ]
    nipple = [
        box([6, FLOOR, 6], [10, FLOOR + 1, 10], {d: face('#ivory', [0, 0, 4, 1]) for d in ('north', 'south', 'east', 'west')}
            | {'up': face('#ivory', [6, 6, 10, 10])}),
        box([7, FLOOR + 1, 7], [9, 15, 9], {d: face('#ivory', [8, 0, 10, 1]) for d in ('north', 'south', 'east', 'west')}
            | {'up': face('#ivory', [7, 7, 9, 9])}),
    ]
    model = {'parent': 'minecraft:block/block',
             'textures': {'skin': SKIN, 'lumen': LUMEN, 'wall': WALL, 'ivory': IVORY, 'particle': SKIN},
             'elements': [base] + ring + nipple}
    (ASSETS / 'models/block/fuel_papilla.json').write_text(json.dumps(model, indent=2) + '\n')
    stale = ASSETS / 'textures/block/fuel_papilla.png'
    if stale.exists():
        stale.unlink()
    print('fuel_papilla: 1 model')


if __name__ == '__main__':
    main()
