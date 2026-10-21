"""Filter Valve: a ribbed sphincter of flesh held in a collar of bone plates, standing on a root foot, its throat
facing north toward the store it guards. Static model; the blockstate turns it.

    python3 art/gen_filter_valve.py
"""
import json
from organ_model import ASSETS, Painter, ball, bone_along, faces, folds, islands, pack, skin, write_static

SHEET = 64
FLESH = [(72, 26, 42), (112, 44, 62), (154, 72, 88), (194, 112, 120), (226, 156, 150)]
BONE = [(109, 91, 76), (166, 144, 112), (215, 197, 157), (244, 230, 197)]
THROAT = [(18, 8, 16), (36, 14, 26), (60, 24, 40)]
ROOT = [(52, 20, 34), (80, 32, 48), (112, 48, 62), (146, 72, 84)]

BONES = [('root', None, [0, 8, 0], [
    ('foot', [-5, 0, -4], [10, 1, 9], None),
    ('body', [-4, 1, -3], [8, 8, 7], None),
    ('collar_ns', [-5, 2, -5], [10, 6, 2], None),
    ('collar_side', [-5, 2, -3], [1, 6, 4], None),
    ('collar_side', [4, 2, -3], [1, 6, 4], None),
    ('throat', [-2, 3, -6], [4, 4, 1], None),
    ('crest', [-3, 9, -2], [6, 2, 5], None),
    ('rib', [-4, 3, 4], [8, 1, 1], None),
    ('rib', [-4, 6, 4], [8, 1, 1], None),
])]


def painter(uv):
    p = Painter(SHEET, 523)
    for island, size in islands(BONES).items():
        for face, rect in faces(size, uv[island]).items():
            light = 0.14 if face == 'up' else -0.16 if face == 'down' else 0
            if island == 'throat':
                p.ramp(rect, THROAT, lambda x, y, w, h: 0.5 - 0.3 * ball(x, y, w, h), grain=0.06, mottle=0.1)
            elif island in ('collar_ns', 'collar_side', 'rib'):
                p.ramp(rect, BONE, lambda x, y, w, h, v=island != 'collar_ns': bone_along(v)(x, y, w, h) + light,
                       grain=0.05, mottle=0.15)
            elif island == 'foot':
                p.ramp(rect, ROOT, lambda x, y, w, h: skin(x, y, w, h) - 0.05 + light, grain=0.07, mottle=0.25)
            else:
                p.ramp(rect, FLESH, lambda x, y, w, h: 0.5 * ball(x, y, w, h) + 0.5 * skin(x, y, w, h)
                       + folds(x, y) + light, grain=0.06, mottle=0.24)
                p.vein(rect, (110, 32, 62), 4, 160)
    return p


def main():
    uv = pack(BONES, SHEET)
    write_static('filter_valve', BONES, SHEET, painter(uv))
    variants = {}
    for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        for reserve in range(8):
            variant = {'model': 'infestusfrontier:block/filter_valve'}
            if y:
                variant['y'] = y
            variants[f'facing={facing},reserve={reserve}'] = variant
    (ASSETS / 'blockstates/filter_valve.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')
    (ASSETS / 'models/item/filter_valve.json').write_text(json.dumps(
        {'parent': 'infestusfrontier:block/filter_valve',
         'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 1.5, 0], 'scale': [0.75, 0.75, 0.75]},
                     'ground': {'translation': [0, 3, 0], 'scale': [0.3, 0.3, 0.3]},
                     'fixed': {'scale': [0.6, 0.6, 0.6]},
                     'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0],
                                               'scale': [0.375, 0.375, 0.375]},
                     'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]}}}, indent=2) + '\n')
    print('filter_valve: 1 model, 1 texture, 1 blockstate, 1 item')


if __name__ == '__main__':
    main()
