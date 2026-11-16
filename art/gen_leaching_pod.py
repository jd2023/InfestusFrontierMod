"""Leaching Pod and Leached Rock. The pod is a plump green bulb on a flat foot with a pore at its crown; spent, the same
organ is a dry, slumped husk. Leached Rock is a translucent cube in three looks, none glowing, a dim one and a brighter
one; the pod picks the look and the blockstate keeps it. The glowing looks light their specks to a faint block light.

    python3 art/gen_leaching_pod.py
"""
import json
import random
from PIL import Image
from organ_model import ASSETS, Painter, ball, compact, faces, folds, pack, skin, write_static

SHEET = 64
BULB = [(48, 64, 30), (74, 98, 40), (108, 138, 56), (146, 174, 78), (188, 206, 108), (222, 232, 150)]
FOOT = [(40, 34, 24), (62, 52, 34), (88, 72, 46), (118, 98, 62)]
PORE = [(30, 40, 20), (52, 70, 30), (84, 108, 44)]
HUSK = [(46, 42, 36), (70, 64, 54), (98, 90, 76), (130, 120, 100), (160, 148, 124)]
HUSK_FOOT = [(34, 30, 24), (52, 46, 36), (74, 66, 50)]

POD = [('root', None, [0, 8, 0], [
    ('foot', [-4, 0, -4], [8, 1, 8], None),
    ('rootlet', [-6, 0, -1], [2, 1, 2], None),
    ('rootlet', [4, 0, 0], [2, 1, 2], None),
    ('rootlet', [-1, 0, 4], [2, 1, 2], None),
    ('belly', [-4, 1, -4], [8, 5, 8], None),
    ('shoulder', [-3, 6, -3], [6, 3, 6], None),
    ('neck', [-2, 9, -2], [4, 2, 4], None),
])]
SPENT = [('root', None, [0, 8, 0], [
    ('foot', [-4, 0, -4], [8, 1, 8], None),
    ('rootlet', [-6, 0, -1], [2, 1, 2], None),
    ('rootlet', [4, 0, 0], [2, 1, 2], None),
    ('rootlet', [-1, 0, 4], [2, 1, 2], None),
    ('belly', [-3, 1, -3], [6, 3, 6], None),
    ('neck', [-2, 4, -2], [4, 2, 4], None),
])]


def painter(bones, bulb, foot, seed):
    uv = pack(bones, SHEET)
    p = Painter(SHEET, seed)
    from organ_model import islands
    for island, size in islands(bones).items():
        for face, rect in faces(size, uv[island]).items():
            light = 0.14 if face == 'up' else -0.16 if face == 'down' else 0
            if island in ('foot', 'rootlet'):
                p.ramp(rect, foot, lambda x, y, w, h: skin(x, y, w, h) - 0.05 + light, grain=0.07, mottle=0.25)
            elif island == 'neck':
                p.ramp(rect, bulb, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + light, grain=0.05, mottle=0.2)
                if face == 'up' and bulb is BULB:
                    x0, y0, w, h = rect
                    for yy in range(h):
                        for xx in range(w):
                            if 1 <= xx < w - 1 and 1 <= yy < h - 1:
                                p.put(rect, xx, yy, PORE[1 + (xx + yy) % 2])
            else:
                p.ramp(rect, bulb, lambda x, y, w, h: 0.55 * ball(x, y, w, h) + 0.45 * skin(x, y, w, h) + light,
                       grain=0.05, mottle=0.22)
                p.vein(rect, bulb[1], 4, 200)
    return p


ROCK_SIZE = 16
ROCK_BASE = (156, 190, 176)
ROCK_CRACK = (86, 116, 106)
ROCK_SPECK = (176, 255, 226)
# looks: (name, specks that glow, face block light)
ROCK_LOOKS = [('leached_rock', 0, 0), ('leached_rock_dim', 6, 5), ('leached_rock_glow', 14, 9)]


def rock_texture(look, specks):
    """A translucent tile: a pale haze with darker, more opaque cracks, and light specks in the glowing looks."""
    rng = random.Random(1103)  # the same haze and cracks in every look, so a glowing block is the dull one plus specks
    img = Image.new('RGBA', (ROCK_SIZE, ROCK_SIZE))
    for y in range(ROCK_SIZE):
        for x in range(ROCK_SIZE):
            shade = rng.uniform(-14, 14)
            img.putpixel((x, y), tuple(int(max(0, min(255, c + shade))) for c in ROCK_BASE) + (rng.randint(58, 86),))
    for _ in range(5):
        x, y = rng.randrange(ROCK_SIZE), rng.randrange(ROCK_SIZE)
        dx, dy = rng.choice((-1, 1)), rng.choice((-1, 0, 1))
        for _ in range(rng.randint(4, 8)):
            img.putpixel((x % ROCK_SIZE, y % ROCK_SIZE), ROCK_CRACK + (170,))
            x += dx
            y += dy if rng.random() < 0.6 else 0
            if rng.random() < 0.3:
                y += rng.choice((-1, 1))
    spots = random.Random(2207)
    for _ in range(specks):
        img.putpixel((spots.randrange(ROCK_SIZE), spots.randrange(ROCK_SIZE)), ROCK_SPECK + (235,))
    (ASSETS / 'textures/block').mkdir(parents=True, exist_ok=True)
    img.save(ASSETS / 'textures/block' / (look + '.png'), optimize=True)


def rock_model(look, light):
    """A full cube, translucent, with the glowing looks lit to a faint block light on every face."""
    sides = {}
    for face in ('down', 'up', 'north', 'south', 'west', 'east'):
        side = {'uv': [0, 0, 16, 16], 'texture': '#all', 'cullface': face}
        if light:
            side['neoforge_data'] = {'block_light': light}
        sides[face] = side
    return {
        'render_type': 'minecraft:translucent',
        'ambientocclusion': False,
        'textures': {'all': 'infestusfrontier:block/' + look, 'particle': 'infestusfrontier:block/' + look},
        'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': sides}],
    }


def rock():
    for index, (look, specks, light) in enumerate(ROCK_LOOKS):
        rock_texture(look, specks)
        (ASSETS / 'models/block' / (look + '.json')).write_text(compact(rock_model(look, light)), encoding='utf-8')
    variants = {f'glow={i}': {'model': 'infestusfrontier:block/' + look} for i, (look, _, _) in enumerate(ROCK_LOOKS)}
    (ASSETS / 'blockstates/leached_rock.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')
    (ASSETS / 'models/item/leached_rock.json').write_text(
        json.dumps({'parent': 'infestusfrontier:block/leached_rock'}, indent=2) + '\n')


def pod():
    write_static('leaching_pod', POD, SHEET, painter(POD, BULB, FOOT, 811))
    write_static('leaching_pod_spent', SPENT, SHEET, painter(SPENT, HUSK, HUSK_FOOT, 977))
    variants = {'spent=false': {'model': 'infestusfrontier:block/leaching_pod'},
                'spent=true': {'model': 'infestusfrontier:block/leaching_pod_spent'}}
    (ASSETS / 'blockstates/leaching_pod.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')
    (ASSETS / 'models/item/leaching_pod.json').write_text(
        json.dumps({'parent': 'infestusfrontier:block/leaching_pod'}, indent=2) + '\n')


def main():
    pod()
    rock()
    print('leaching pod: 2 models, 2 textures; leached rock: 3 models, 3 textures; 2 blockstates, 2 item models')


if __name__ == '__main__':
    main()
