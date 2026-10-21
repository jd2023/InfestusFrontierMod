"""The port organs: a Feeding Lobe and a Yield Lobe. A bulb on a root foot reaches a fleshy duct north into the organ
it faces. The Feeding Lobe opens a lipped maw on top, where items go in; the Yield Lobe carries a ridged ivory spout
at its base, where products come out.

    python3 art/gen_lobes.py

North (-Z) is the organ side; the blockstate turns the model to face it.
"""
import json
from pathlib import Path
from organ_model import ASSETS, Painter, ball, bone_along, faces, folds, islands, pack, skin, write_static

SHEET = 64
FLESH = [(70, 26, 40), (112, 44, 62), (156, 74, 88), (196, 114, 120), (226, 158, 152)]
DUCT = [(62, 22, 40), (98, 36, 58), (140, 60, 80), (182, 96, 108)]
LIP = [(84, 28, 50), (128, 48, 74), (180, 88, 112), (226, 144, 156)]
IVORY = [(109, 91, 76), (166, 144, 112), (215, 197, 157), (244, 230, 197)]
INSIDE = [(22, 10, 20), (40, 16, 30), (66, 26, 44)]


def bones(feeds):
    cubes = [
        ('foot', [-5, 0, -4], [10, 2, 10], None),
        ('bulb', [-4, 2, -3], [8, 6, 8], None),
        ('crown', [-3, 8, -2], [6, 2, 6], None),
        ('duct', [-2, 3, -8], [4, 4, 5], None),
        ('collar', [-3, 2, -4], [6, 6, 1], None),
        ('cuff', [-3, 2, -8], [6, 6, 1], None),
    ]
    if feeds:
        cubes += [
            ('lip_ns', [-3, 10, -2], [6, 1, 1], None),
            ('lip_ns', [-3, 10, 3], [6, 1, 1], None),
            ('lip_we', [-3, 10, -1], [1, 1, 4], None),
            ('lip_we', [2, 10, -1], [1, 1, 4], None),
            ('maw', [-2, 9, -1], [4, 1, 4], None),
        ]
    else:
        cubes += [
            ('spout', [-2, 2, 5], [4, 3, 2], None),
            ('ridge', [-3, 2, 4], [1, 4, 1], None),
            ('ridge', [2, 2, 4], [1, 4, 1], None),
            ('cap', [-2, 10, -1], [4, 1, 4], None),
        ]
    return [('root', None, [0, 8, 0], cubes)]


def make(name, feeds, seed, lift):
    model = bones(feeds)
    uv = pack(model, SHEET)
    painter = Painter(SHEET, seed)
    for island, size in islands(model).items():
        for face, rect in faces(size, uv[island]).items():
            light = 0.14 if face == 'up' else -0.16 if face == 'down' else 0
            if island == 'maw':
                painter.ramp(rect, INSIDE, lambda x, y, w, h: 0.4, grain=0.08, mottle=0.2)
            elif island in ('lip_ns', 'lip_we'):
                painter.ramp(rect, LIP, lambda x, y, w, h: skin(x, y, w, h) + light, grain=0.06, mottle=0.2)
            elif island in ('spout', 'ridge'):
                painter.ramp(rect, IVORY, lambda x, y, w, h, v=island == 'ridge': bone_along(v)(x, y, w, h) + light,
                             grain=0.05, mottle=0.15)
            elif island in ('duct', 'cuff', 'collar'):
                painter.ramp(rect, DUCT, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + light,
                             grain=0.06, mottle=0.22)
                painter.vein(rect, (120, 30, 60), 3, 160)
            elif island in ('bulb', 'crown', 'cap'):
                painter.ramp(rect, FLESH, lambda x, y, w, h: 0.5 * ball(x, y, w, h) + 0.5 * skin(x, y, w, h)
                             + folds(x, y) + light, grain=0.06, mottle=0.24)
                painter.vein(rect, (92, 28, 54), 4, 140)
            else:
                painter.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) - 0.1 + light, grain=0.07, mottle=0.25)
    write_static(name, model, SHEET, painter)
    variants = {}
    for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        variant = {'model': 'infestusfrontier:block/' + name}
        if y:
            variant['y'] = y
        variants['facing=' + facing] = variant
    (ASSETS / 'blockstates' / (name + '.json')).write_text(json.dumps({'variants': variants}, indent=2) + '\n')
    (ASSETS / 'models/item' / (name + '.json')).write_text(
        json.dumps({'parent': 'infestusfrontier:block/' + name,
                    'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, lift, 0], 'scale': [0.75, 0.75, 0.75]},
                                'fixed': {'scale': [0.6, 0.6, 0.6]},
                                'ground': {'translation': [0, 3, 0], 'scale': [0.3, 0.3, 0.3]},
                                'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0],
                                                          'scale': [0.375, 0.375, 0.375]},
                                'firstperson_righthand': {'rotation': [0, 45, 0], 'scale': [0.4, 0.4, 0.4]}}},
                   indent=2) + '\n')


if __name__ == '__main__':
    make('feeding_lobe', True, 311, 1)
    make('yield_lobe', False, 313, 2)
    print('lobes: 2 models, 2 textures, 2 blockstates, 2 items')
