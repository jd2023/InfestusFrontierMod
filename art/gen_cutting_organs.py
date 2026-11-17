"""Fracture Jaw, Work Bed and Collection Cilia: temporary functional block-built models. The jaw is a squat bone-white
body on a dark foot with two heavy jaw plates that point the way it cuts (north in the model; the blockstate turns
it). The bed is a fleshy trough with a raised bone rim. The cilia are a pink body on a pale base with three tall
fronds reaching forward. The boxes match FractureJawBlock.BOXES, WorkBedBlock.BOXES and CollectionCiliaBlock.BOXES.

    python3 art/gen_cutting_organs.py
"""
import json
from organ_model import ASSETS, Painter, ball, faces, folds, islands, pack, skin, write_static

SHEET = 64
BONE = [(122, 112, 96), (156, 146, 124), (186, 176, 152), (212, 204, 180), (232, 226, 206)]
FOOT = [(40, 30, 28), (58, 44, 40), (78, 60, 54), (98, 78, 70)]
FLESH = [(96, 42, 52), (130, 62, 72), (164, 88, 96), (196, 122, 126), (222, 160, 158)]
FROND = [(150, 70, 104), (184, 98, 132), (214, 132, 160), (238, 172, 194)]


def box(island, x1, y1, z1, x2, y2, z2):
    """A cube from Java block pixels (north is z = 0) to the model's coordinates (-8..8 on x and z)."""
    return (island, [x1 - 8, y1, z1 - 8], [x2 - x1, y2 - y1, z2 - z1], None)


JAW = [('jaw', None, [0, 8, 0], [
    box('foot', 1, 0, 2, 15, 2, 15),
    box('body', 2, 2, 4, 14, 12, 15),
    box('upper', 3, 8, 0, 13, 12, 4),
    box('lower', 3, 2, 0, 13, 5, 4),
])]

BED = [('bed', None, [0, 8, 0], [
    box('floor', 0, 0, 0, 16, 3, 16),
    box('rim_ns', 0, 3, 0, 16, 8, 2),
    box('rim_ns', 0, 3, 14, 16, 8, 16),
    box('rim_ew', 0, 3, 2, 2, 8, 14),
    box('rim_ew', 14, 3, 2, 16, 8, 14),
])]

CILIA = [('cilia', None, [0, 8, 0], [
    box('base', 2, 0, 2, 14, 3, 14),
    box('body', 4, 3, 5, 12, 10, 13),
    box('frond', 3, 5, 0, 5, 13, 6),
    box('frond_tall', 7, 5, 0, 9, 14, 6),
    box('frond', 11, 5, 0, 13, 13, 6),
])]


def paint(bones, seed, skins):
    """Paints each island from its ramp: skins maps island -> (ramp, value function)."""
    p = Painter(SHEET, seed)
    uv = pack(bones, SHEET)
    for island, size in islands(bones).items():
        ramp, value = skins[island]
        for face, rect in faces(size, uv[island]).items():
            light = 0.14 if face == 'up' else -0.16 if face == 'down' else 0
            p.ramp(rect, ramp, lambda x, y, w, h, v=value, l=light: v(x, y, w, h) + l, grain=0.06, mottle=0.22)
    return p


def rough(x, y, w, h):
    return skin(x, y, w, h) + folds(x, y)


def soft(x, y, w, h):
    return 0.5 * ball(x, y, w, h) + 0.5 * skin(x, y, w, h)


def facing_blockstate(name):
    variants = {f'facing={facing}': {'model': 'infestusfrontier:block/' + name, **({'y': y} if y else {})}
                for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}
    (ASSETS / f'blockstates/{name}.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')


def item(name):
    (ASSETS / f'models/item/{name}.json').write_text(
        json.dumps({'parent': 'infestusfrontier:block/' + name}, indent=2) + '\n')


def jaw():
    p = paint(JAW, 5113, {'foot': (FOOT, rough), 'body': (BONE, rough), 'upper': (BONE, soft),
                          'lower': (BONE, soft)})
    uv = pack(JAW, SHEET)
    for island in ('upper', 'lower'):  # a dark seam of teeth along each jaw's biting edge
        x, y, w, h = faces(islands(JAW)[island], uv[island])['north']
        row = y + h - 1 if island == 'upper' else y
        for dx in range(w):
            if dx % 2 == 0:
                p.put((x, y, w, h), dx, row - y, FOOT[0])
    write_static('fracture_jaw', JAW, SHEET, p)
    facing_blockstate('fracture_jaw')
    item('fracture_jaw')


def bed():
    p = paint(BED, 5227, {'floor': (FLESH, soft), 'rim_ns': (BONE, rough), 'rim_ew': (BONE, rough)})
    write_static('work_bed', BED, SHEET, p)
    (ASSETS / 'blockstates/work_bed.json').write_text(
        json.dumps({'variants': {'': {'model': 'infestusfrontier:block/work_bed'}}}, indent=2) + '\n')
    item('work_bed')


def cilia():
    p = paint(CILIA, 5341, {'base': (BONE, rough), 'body': (FLESH, soft), 'frond': (FROND, soft),
                            'frond_tall': (FROND, soft)})
    write_static('collection_cilia', CILIA, SHEET, p)
    facing_blockstate('collection_cilia')
    item('collection_cilia')


def main():
    jaw()
    bed()
    cilia()
    print('fracture jaw, work bed, collection cilia: 3 models, 3 textures, 3 blockstates, 3 item models')


if __name__ == '__main__':
    main()
