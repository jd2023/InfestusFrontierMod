"""Fracture Jaw, Work Bed and Collection Cilia: temporary functional block-built models. The jaw is a squat bone-white
body on a dark foot with two heavy jaw plates that point the way it cuts (north in the model; the blockstate turns
it). The bed is a fleshy trough with a raised bone rim. The cilia are a pink body on a pale base with three tall
fronds reaching forward over a dark intake. The boxes match FractureJawBlock.BOXES, WorkBedBlock.BOXES and
CollectionCiliaBlock.BOXES at rest.

    python3 art/gen_cutting_organs.py

The cilia are a GeckoLib organ (north is -z; the block entity turns it). At rest they sway. While a sweep moves
items (drawing, one loop a sweep) the fronds reach out and pull back, three cut fragments ride in from the bed to the
intake and shrink into it, and a bulge runs down the body into the Mouth they stand on: the material going into the
vein. GeckoLib negates rotations about x, so a positive file x tips a frond's top out towards the bed.
"""
import json
from organ_model import ASSETS, Painter, ball, faces, folds, islands, pack, sampled, skin, squeeze, wave, write, write_static

SHEET = 64
BONE = [(122, 112, 96), (156, 146, 124), (186, 176, 152), (212, 204, 180), (232, 226, 206)]
FOOT = [(40, 30, 28), (58, 44, 40), (78, 60, 54), (98, 78, 70)]
FLESH = [(96, 42, 52), (130, 62, 72), (164, 88, 96), (196, 122, 126), (222, 160, 158)]
FROND = [(150, 70, 104), (184, 98, 132), (214, 132, 160), (238, 172, 194)]


def box(island, x1, y1, z1, x2, y2, z2):
    """A cube from Java block pixels (north is z = 0) to the model's coordinates (-8..8 on x and z)."""
    size = [x2 - x1, y2 - y1, z2 - z1]
    assert all(float(v).is_integer() for v in size), size  # box UV needs whole sizes
    return (island, [x1 - 8, y1, z1 - 8], [int(v) for v in size], None)


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

SWEEP = 1.0  # one drawing loop is one sweep, a second
IDLE = 4.0
FRAGMENT = [(92, 88, 84), (120, 116, 108), (150, 146, 136)]
INTAKE = [(40, 14, 22), (64, 26, 36), (88, 40, 50)]

CILIA = [
    ('cilia', None, [0, 0, 0], [box('base', 2, 0, 2, 14, 3, 14), box('body', 4, 3, 5, 12, 10, 13)]),
    ('intake', 'cilia', [0, 6, -3], [box('intake', 5.5, 4.5, 4.5, 10.5, 8.5, 5.5)]),
    # each frond hinges on its front foot, so its tip can lean out to the bed and back without leaving the block
    ('frond_l', 'cilia', [4, 5, -7], [box('frond', 3, 5, 1, 5, 13, 7)]),
    ('frond_c', 'cilia', [0, 5, -7], [box('frond_tall', 7, 5, 1, 9, 14, 7)]),
    ('frond_r', 'cilia', [-4, 5, -7], [box('frond', 11, 5, 1, 13, 13, 7)]),
    # three cut fragments, hidden at rest, that ride in from the bed; and the bulge that runs down into the Mouth
    ('bit_a', 'cilia', [-1.5, 6, -7], [box('bit', 6, 5.5, 0.5, 7, 6.5, 1.5)]),
    ('bit_b', 'cilia', [1.5, 7, -7], [box('bit', 9, 6.5, 0.5, 10, 7.5, 1.5)]),
    ('bit_c', 'cilia', [0, 5, -7], [box('bit', 7.5, 4.5, 0.5, 8.5, 5.5, 1.5)]),
    ('bulge', 'cilia', [0, 8.75, 1], [box('bulge', 3.5, 8.35, 5.6, 12.5, 9.35, 13.6)]),
]


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


def cilia_animations():
    def frond(lag):
        # lean out a little towards the bed (positive file x), then sweep back hard towards the intake, then settle
        return {'rotation': sampled(SWEEP, 0.05, lambda t: [6 * squeeze(t, SWEEP, lag) - 22 * squeeze(t, SWEEP, lag + 0.35), 0, 0])}

    def bit(lag):
        def at(t):
            k = ((t - lag) % SWEEP) / SWEEP
            return [0, -0.6 * k, 4.2 * k]  # from the bed's side to the intake

        def size(t):
            k = ((t - lag) % SWEEP) / SWEEP
            shown = 1.0 if k < 0.75 else max(0.0, 1 - (k - 0.75) / 0.2)
            return [shown] * 3
        return {'position': sampled(SWEEP, 0.05, at), 'scale': sampled(SWEEP, 0.05, size)}

    def bulge(t):
        k = t / SWEEP
        return [0, -8.0 * k, 0]

    def bulge_size(t):
        k = t / SWEEP
        swell = 0.0 if k > 0.9 else min(1.0, k / 0.15) * (1 - max(0.0, k - 0.7) / 0.2)
        return [0.94 + 0.12 * swell, swell, 0.94 + 0.12 * swell]

    hidden = {'scale': {'0.0': [0, 0, 0]}}
    idle = {
        'cilia': {'scale': sampled(IDLE, 0.25, lambda t: [1 + 0.01 * wave(t, IDLE), 1 - 0.012 * wave(t, IDLE), 1 + 0.01 * wave(t, IDLE)])},
        'frond_l': {'rotation': sampled(IDLE, 0.25, lambda t: [-3 - 3 * wave(t, IDLE), 0, 0])},
        'frond_c': {'rotation': sampled(IDLE, 0.25, lambda t: [-3 - 3 * wave(t, IDLE, 0.4), 0, 0])},
        'frond_r': {'rotation': sampled(IDLE, 0.25, lambda t: [-3 - 3 * wave(t, IDLE, 0.8), 0, 0])},
        'bit_a': hidden, 'bit_b': hidden, 'bit_c': hidden, 'bulge': hidden,
    }
    drawing = {
        'frond_l': frond(0.0), 'frond_c': frond(0.08), 'frond_r': frond(0.16),
        'intake': {'scale': sampled(SWEEP, 0.05, lambda t: [1 + 0.25 * squeeze(t, SWEEP, 0.55)] * 2 + [1])},
        'bit_a': bit(0.0), 'bit_b': bit(0.33), 'bit_c': bit(0.66),
        'bulge': {'position': sampled(SWEEP, 0.05, bulge), 'scale': sampled(SWEEP, 0.05, bulge_size)},
    }
    return {'idle': (IDLE, True, idle), 'drawing': (SWEEP, True, drawing)}


def cilia():
    p = paint(CILIA, 5341, {'base': (BONE, rough), 'body': (FLESH, soft), 'frond': (FROND, soft),
                            'frond_tall': (FROND, soft), 'intake': (INTAKE, soft), 'bit': (FRAGMENT, rough),
                            'bulge': (FLESH, soft)})
    uv = pack(CILIA, SHEET)
    particle = faces(islands(CILIA)['body'], uv['body'])['north']
    write('collection_cilia', CILIA, SHEET, p, cilia_animations(), (particle[0], particle[1]))
    (ASSETS / 'blockstates/collection_cilia.json').write_text(
        json.dumps({'variants': {'': {'model': 'infestusfrontier:block/collection_cilia'}}}, indent=2) + '\n')
    (ASSETS / 'models/block/collection_cilia.json').write_text(
        json.dumps({'textures': {'particle': 'infestusfrontier:block/collection_cilia'}}, indent=2) + '\n')


def main():
    jaw()
    bed()
    cilia()
    print('fracture jaw, work bed: 2 models, 2 textures, 2 blockstates, 2 item models; collection cilia: 1 GeckoLib model')


if __name__ == '__main__':
    main()
