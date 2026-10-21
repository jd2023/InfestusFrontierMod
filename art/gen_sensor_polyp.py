"""Sensor Polyp: a rooted stalk that leans toward what it watches, ending in an eye bulb with a lid ridge and a slit
pupil. North (-Z) is where it looks; the blockstate turns it. A second model lights the iris (NeoForge per-face
light) while the polyp reads anything above zero.

    python3 art/gen_sensor_polyp.py
"""
import json
from organ_model import ASSETS, Painter, ball, faces, folds, islands, pack, skin, write_static

SHEET = 64
FLESH = [(70, 28, 44), (104, 44, 60), (140, 66, 80), (174, 94, 104), (204, 128, 128), (228, 164, 154)]
ROOT = [(52, 20, 34), (80, 32, 48), (112, 48, 62), (146, 72, 84)]
LID = [(90, 34, 54), (130, 56, 78), (176, 92, 112), (214, 136, 148)]
IRIS = [(46, 22, 70), (90, 52, 140), (140, 98, 196), (196, 170, 236), (240, 230, 252)]
PUPIL = (14, 6, 20)

BONES = [('root', None, [0, 8, 0], [
    ('foot', [-4, 0, -3], [8, 1, 8], None),
    ('rootlet', [-6, 0, 0], [2, 1, 2], None),
    ('rootlet', [4, 0, 1], [2, 1, 2], None),
    ('rootlet', [-1, 0, 5], [2, 1, 2], None),
    ('stalk', [-2, 1, 0], [4, 3, 4], None),
    ('stalk', [-2, 4, -1], [4, 3, 4], None),
    ('neck', [-2, 7, -2], [4, 2, 4], None),
    ('bulb', [-3, 8, -4], [6, 5, 6], None),
    ('lid', [-3, 13, -4], [6, 1, 4], None),
    ('iris', [-2, 9, -5], [4, 3, 1], None),
])]


def painter(uv):
    p = Painter(SHEET, 509)
    for island, size in islands(BONES).items():
        for face, rect in faces(size, uv[island]).items():
            light = 0.14 if face == 'up' else -0.16 if face == 'down' else 0
            if island == 'iris':
                p.ramp(rect, IRIS, lambda x, y, w, h: 0.35 + 0.55 * ball(x, y, w, h), grain=0.04, mottle=0.08)
                if face == 'north':
                    x0, y0, w, h = rect
                    for yy in range(h):
                        for xx in (w // 2 - 1, w // 2):
                            p.put(rect, xx, yy, PUPIL)
            elif island in ('foot', 'rootlet'):
                p.ramp(rect, ROOT, lambda x, y, w, h: skin(x, y, w, h) - 0.05 + light, grain=0.07, mottle=0.25)
            elif island == 'lid':
                p.ramp(rect, LID, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + light, grain=0.05, mottle=0.2)
            elif island == 'bulb':
                p.ramp(rect, FLESH, lambda x, y, w, h: 0.55 * ball(x, y, w, h) + 0.45 * skin(x, y, w, h) + light,
                       grain=0.05, mottle=0.22)
                p.vein(rect, (110, 34, 64), 4, 170)
            else:
                p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + light, grain=0.06, mottle=0.24)
                p.vein(rect, (96, 30, 56), 3, 150)
    return p


def lit_copy():
    """The same model with the iris element lit to full block and sky light."""
    path = ASSETS / 'models/block/sensor_polyp.json'
    model = json.loads(path.read_text())
    for element in model['elements']:
        if element['from'] == [6, 9, 3] and element['to'] == [10, 12, 4]:
            for face in element['faces'].values():
                face['neoforge_data'] = {'block_light': 15, 'sky_light': 15}
    (ASSETS / 'models/block/sensor_polyp_lit.json').write_text(json.dumps(model, indent=2) + '\n')


def blockstate():
    """Horizontal facings turn the polyp; facing down roots it in the block it reads, upright. It never faces up."""
    turns = {'north': 0, 'east': 90, 'south': 180, 'west': 270, 'down': 0, 'up': 0}
    variants = {}
    for facing, y in turns.items():
        for power in range(16):
            variant = {'model': 'infestusfrontier:block/sensor_polyp' + ('_lit' if power else '')}
            if y:
                variant['y'] = y
            variants[f'facing={facing},power={power}'] = variant
    (ASSETS / 'blockstates/sensor_polyp.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')


def main():
    uv = pack(BONES, SHEET)
    write_static('sensor_polyp', BONES, SHEET, painter(uv))
    lit_copy()
    blockstate()
    print('sensor_polyp: 2 models, 1 texture, 1 blockstate')


if __name__ == '__main__':
    main()
