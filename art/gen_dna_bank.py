"""DNA Bank, Sampling Needle and Pig DNA Sample: temporary functional art. The bank is a block-built organ: a bone
foot, a fleshy body banded with a purple double helix, and a pale glassy crown of stored vials ringed in dark bands.
The boxes match DnaBankBlock.BOXES. The needle is a bone grip, a glass barrel and an iron point lying corner to
corner; the sample is a small stoppered vial of pink fluid.

    python3 art/gen_dna_bank.py
"""
import json
import math
from PIL import Image
from organ_model import ASSETS, Painter, ball, faces, folds, islands, pack, skin, write_static

SHEET = 64
BONE = [(122, 112, 96), (156, 146, 124), (186, 176, 152), (212, 204, 180), (232, 226, 206)]
FLESH = [(84, 40, 66), (112, 58, 88), (142, 80, 112), (172, 108, 136), (204, 142, 164)]
HELIX = [(70, 30, 96), (104, 52, 140), (150, 96, 190)]
GLASS = [(120, 150, 160), (150, 182, 190), (184, 212, 216), (214, 236, 236)]
RING = (52, 36, 48)
IRON = [(92, 92, 98), (150, 150, 158), (206, 206, 214)]
PINK = [(170, 70, 96), (214, 110, 132), (240, 160, 176)]
CORK = [(110, 74, 44), (150, 106, 66)]


def box(island, x1, y1, z1, x2, y2, z2):
    """A cube from Java block pixels (north is z = 0) to the model's coordinates (-8..8 on x and z)."""
    return (island, [x1 - 8, y1, z1 - 8], [x2 - x1, y2 - y1, z2 - z1], None)


BANK = [('bank', None, [0, 8, 0], [
    box('foot', 1, 0, 1, 15, 2, 15),
    box('body', 2, 2, 2, 14, 10, 14),
    box('crown', 4, 10, 4, 12, 15, 12),
])]


def rough(x, y, w, h):
    return skin(x, y, w, h) + folds(x, y)


def soft(x, y, w, h):
    return 0.5 * ball(x, y, w, h) + 0.5 * skin(x, y, w, h)


def glassy(x, y, w, h):
    return 0.75 - 0.25 * (y / max(1, h - 1)) + (0.15 if x % 3 == 1 else 0)


def bank():
    p = Painter(SHEET, 6113)
    uv = pack(BANK, SHEET)
    skins = {'foot': (BONE, rough), 'body': (FLESH, soft), 'crown': (GLASS, glassy)}
    for island, size in islands(BANK).items():
        ramp, value = skins[island]
        for face, rect in faces(size, uv[island]).items():
            light = 0.14 if face == 'up' else -0.16 if face == 'down' else 0
            p.ramp(rect, ramp, lambda x, y, w, h, v=value, l=light: v(x, y, w, h) + l, grain=0.05, mottle=0.18)
    body = faces(islands(BANK)['body'], uv['body'])
    for face in ('north', 'south', 'east', 'west'):  # a double helix wound round the body
        rect = body[face]
        _, _, w, h = rect
        for y in range(h):
            for strand, phase in ((0, 0.0), (1, math.pi)):
                x = int(round((w - 1) / 2 + (w / 2 - 1.5) * math.sin(y * 0.9 + phase)))
                p.put(rect, x, y, HELIX[1 + strand])
            if y % 2 == 0:  # the rungs between the strands
                a = int(round((w - 1) / 2 + (w / 2 - 1.5) * math.sin(y * 0.9)))
                b = int(round((w - 1) / 2 + (w / 2 - 1.5) * math.sin(y * 0.9 + math.pi)))
                for x in range(min(a, b) + 1, max(a, b)):
                    p.put(rect, x, y, HELIX[0])
    crown = faces(islands(BANK)['crown'], uv['crown'])
    for face in ('north', 'south', 'east', 'west'):  # dark bands: the stoppers and the rack of stored vials
        rect = crown[face]
        _, _, w, h = rect
        for x in range(w):
            p.put(rect, x, 0, RING)
            p.put(rect, x, h - 1, RING)
        for x in range(0, w, 3):
            for y in range(1, h - 1):
                p.put(rect, x, y, RING)
    top = crown['up']
    for x in range(top[2]):
        for y in range(top[3]):
            if (x + y) % 3 == 0:
                p.put(top, x, y, CORK[1])
    write_static('dna_bank', BANK, SHEET, p)
    (ASSETS / 'blockstates/dna_bank.json').write_text(
        json.dumps({'variants': {'': {'model': 'infestusfrontier:block/dna_bank'}}}, indent=2) + '\n')
    (ASSETS / 'models/item/dna_bank.json').write_text(
        json.dumps({'parent': 'infestusfrontier:block/dna_bank'}, indent=2) + '\n')


def icon(name, img):
    (ASSETS / 'textures/item').mkdir(parents=True, exist_ok=True)
    img.save(ASSETS / f'textures/item/{name}.png', optimize=True)
    (ASSETS / f'models/item/{name}.json').write_text(json.dumps(
        {'parent': 'minecraft:item/handheld' if name == 'sampling_needle' else 'minecraft:item/generated',
         'textures': {'layer0': f'infestusfrontier:item/{name}'}}, indent=2) + '\n')


def needle():
    """Bone grip at the lower left, a glass barrel with a pink fill in the middle, an iron point to the upper right."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for t in range(1, 15):
        x, y = t, 15 - t
        if t <= 4:  # the grip, two texels thick, with a dark joint
            for dx, dy, c in ((0, 0, BONE[3]), (1, 0, BONE[2]), (0, -1, BONE[4]), (-1, 0, BONE[1])):
                img.putpixel((x + dx, y + dy), c + (255,))
        elif t <= 10:  # the barrel: glass rims, pink inside
            for dx, dy, c in ((0, 0, PINK[1] if t <= 8 else GLASS[2]), (1, 0, GLASS[1]), (0, -1, GLASS[3]),
                              (-1, 0, GLASS[0]), (1, -1, GLASS[2]), (-1, 1, GLASS[0])):
                img.putpixel((x + dx, y + dy), c + (255,))
        else:  # the iron point thins to one texel
            img.putpixel((x, y), IRON[2 if t < 14 else 1] + (255,))
            if t < 13:
                img.putpixel((x - 1, y), IRON[0] + (255,))
    img.putpixel((5, 9), RING + (255,))  # the plunger ring between grip and barrel
    img.putpixel((4, 10), RING + (255,))
    icon('sampling_needle', img)


def sample():
    """A small upright vial: dark cork, clear neck, pink fluid with a pale glint and a dark glass outline."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(2, 4):
        for x in range(6, 10):
            img.putpixel((x, y), CORK[(x + y) % 2] + (255,))
    for y in range(4, 14):
        for x in range(5, 11):
            edge = x in (5, 10) or y == 13
            if edge:
                c = GLASS[0]
            elif y < 7:
                c = GLASS[3]
            else:
                c = PINK[2] if x == 6 else PINK[1] if y < 12 else PINK[0]
            img.putpixel((x, y), c + (255,))
    for x, y in ((7, 8), (9, 8), (8, 9), (7, 10), (9, 10), (8, 11)):  # a twist of the helix
        img.putpixel((x, y), HELIX[0] + (255,))
    for x in range(6, 10):
        img.putpixel((x, 14), RING + (255,))
    icon('pig_dna_sample', img)


def main():
    bank()
    needle()
    sample()
    print('dna bank: 1 model, 1 texture, 1 blockstate, 1 item model; sampling needle, pig dna sample: 2 icons')


if __name__ == '__main__':
    main()
