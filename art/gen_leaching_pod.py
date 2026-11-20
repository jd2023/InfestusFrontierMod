"""Leaching Pod, Leached Rock and the pod's root. The pod is a plump green bulb on a flat foot with a pore at its crown;
finished, it is the same bulb with its pore closed: it stays an organ, not a husk. Leached Rock is one translucent cube
that does not glow. The root is a thick bark-wrapped pillar threaded with pale glowing veins that the pod grows down
its own column; it is the only light of the pit and the blockstate gives it its block light.

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
SEALED = [(60, 78, 36), (86, 110, 48)]

POD = [('root', None, [0, 8, 0], [
    ('foot', [-4, 0, -4], [8, 1, 8], None),
    ('rootlet', [-6, 0, -1], [2, 1, 2], None),
    ('rootlet', [4, 0, 0], [2, 1, 2], None),
    ('rootlet', [-1, 0, 4], [2, 1, 2], None),
    ('belly', [-4, 1, -4], [8, 5, 8], None),
    ('shoulder', [-3, 6, -3], [6, 3, 6], None),
    ('neck', [-2, 9, -2], [4, 2, 4], None),
])]


def painter(bones, bulb, foot, seed, open_pore=True):
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
                if face == 'up':
                    x0, y0, w, h = rect
                    for yy in range(h):
                        for xx in range(w):
                            if 1 <= xx < w - 1 and 1 <= yy < h - 1:
                                p.put(rect, xx, yy, PORE[1 + (xx + yy) % 2] if open_pore else SEALED[(xx + yy) % 2])
            else:
                p.ramp(rect, bulb, lambda x, y, w, h: 0.55 * ball(x, y, w, h) + 0.45 * skin(x, y, w, h) + light,
                       grain=0.05, mottle=0.22)
                p.vein(rect, bulb[1], 4, 200)
    return p


ROCK_SIZE = 16
ROCK_BASE = (156, 190, 176)
ROCK_CRACK = (86, 116, 106)


def rock_texture(look):
    """A translucent tile: a pale haze with darker, more opaque cracks."""
    rng = random.Random(1103)
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
    (ASSETS / 'textures/block').mkdir(parents=True, exist_ok=True)
    img.save(ASSETS / 'textures/block' / (look + '.png'), optimize=True)


def rock_model(look):
    """A full cube, translucent."""
    sides = {face: {'uv': [0, 0, 16, 16], 'texture': '#all', 'cullface': face}
             for face in ('down', 'up', 'north', 'south', 'west', 'east')}
    return {
        'render_type': 'minecraft:translucent',
        'ambientocclusion': False,
        'textures': {'all': 'infestusfrontier:block/' + look, 'particle': 'infestusfrontier:block/' + look},
        'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': sides}],
    }


def rock():
    rock_texture('leached_rock')
    (ASSETS / 'models/block/leached_rock.json').write_text(compact(rock_model('leached_rock')), encoding='utf-8')
    (ASSETS / 'blockstates/leached_rock.json').write_text(
        json.dumps({'variants': {'': {'model': 'infestusfrontier:block/leached_rock'}}}, indent=2) + '\n')
    (ASSETS / 'models/item/leached_rock.json').write_text(
        json.dumps({'parent': 'infestusfrontier:block/leached_rock'}, indent=2) + '\n')


BARK = [(40, 30, 22), (62, 44, 30), (88, 64, 42), (116, 88, 58), (146, 114, 76)]
GLOW = [(150, 210, 120), (190, 236, 150), (226, 252, 190)]
# the pillar's boxes, in Java block pixels; LeachingRootBlock.SHAPE matches them
ROOT_FROM, ROOT_TO = [4, 0, 4], [12, 16, 12]


def root_texture():
    """Bark that wraps the pillar in vertical strands, split by pale veins that glow; it tiles top to bottom."""
    rng = random.Random(3307)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            strand = (x * 5 + (y // 4) * 3) % 7
            img.putpixel((x, y), BARK[min(4, 1 + strand // 2 + (1 if rng.random() < 0.15 else 0))] + (255,))
    for vein in (3, 11):
        x = vein
        for y in range(16):
            img.putpixel((x % 16, y), GLOW[1] + (255,))
            if y % 5 == 2:
                img.putpixel(((x + 1) % 16, y), GLOW[2] + (255,))
            x += 1 if y % 8 == 3 else -1 if y % 8 == 7 else 0
    img.save(ASSETS / 'textures/block/leaching_root.png', optimize=True)


def root():
    """The pod's root column: one pillar element, its sides lit to full block light so the veins read as glowing."""
    root_texture()
    faces_ = {}
    for face in ('north', 'south', 'west', 'east'):
        faces_[face] = {'uv': [4, 0, 12, 16], 'texture': '#bark', 'neoforge_data': {'block_light': 15}}
    for face in ('up', 'down'):
        faces_[face] = {'uv': [4, 4, 12, 12], 'texture': '#bark', 'cullface': face}
    model = {
        'ambientocclusion': False,
        'textures': {'bark': 'infestusfrontier:block/leaching_root', 'particle': 'infestusfrontier:block/leaching_root'},
        'elements': [{'from': ROOT_FROM, 'to': ROOT_TO, 'faces': faces_}],
    }
    (ASSETS / 'models/block/leaching_root.json').write_text(compact(model), encoding='utf-8')
    (ASSETS / 'blockstates/leaching_root.json').write_text(
        json.dumps({'variants': {'': {'model': 'infestusfrontier:block/leaching_root'}}}, indent=2) + '\n')


def pod():
    write_static('leaching_pod', POD, SHEET, painter(POD, BULB, FOOT, 811))
    write_static('leaching_pod_spent', POD, SHEET, painter(POD, BULB, FOOT, 811, open_pore=False))
    variants = {'spent=false': {'model': 'infestusfrontier:block/leaching_pod'},
                'spent=true': {'model': 'infestusfrontier:block/leaching_pod_spent'}}
    (ASSETS / 'blockstates/leaching_pod.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')
    (ASSETS / 'models/item/leaching_pod.json').write_text(
        json.dumps({'parent': 'infestusfrontier:block/leaching_pod'}, indent=2) + '\n')


def main():
    pod()
    rock()
    root()
    print('leaching pod: 2 models, 2 textures; leached rock and root: 2 models, 2 textures; 3 blockstates, 2 item models')


if __name__ == '__main__':
    main()
