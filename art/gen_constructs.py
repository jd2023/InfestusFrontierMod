"""Stairs Construct and Wider Section Construct icons: temporary functional art. A construct is a sealed pod of
living membrane, banded with the purple helix of the genome it came from, with its mutation's sign set into it in
bone: a flight of three steps for Stairs, a hollow square for Wider Section. Both share the pod so they read as one
kind of item, and differ only by the sign.

    python3 art/gen_constructs.py
"""
import json
from PIL import Image
from organ_model import ASSETS

OUTLINE = (52, 30, 48)
POD = [(104, 52, 84), (138, 76, 112), (172, 108, 140), (204, 146, 170)]
HELIX = [(70, 30, 96), (118, 64, 160)]
BONE = [(186, 176, 152), (232, 226, 206)]

# The pod: rows 2..13, narrower at the tips. Each row is (first x, last x).
ROWS = {2: (6, 9), 3: (5, 10), 4: (4, 11), 5: (3, 12), 6: (3, 12), 7: (3, 12), 8: (3, 12), 9: (3, 12),
        10: (3, 12), 11: (4, 11), 12: (5, 10), 13: (6, 9)}

# Three steps, each two texels wide and two high, falling to the right.
STEPS = [(x, y) for y in range(5, 11) for x in range(5, 7 + 2 * ((y - 5) // 2))]
SQUARE = [(x, 5) for x in range(5, 11)] + [(x, 10) for x in range(5, 11)] + \
         [(5, y) for y in range(6, 10)] + [(10, y) for y in range(6, 10)]


def pod():
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, (x1, x2) in ROWS.items():
        for x in range(x1, x2 + 1):
            edge = x in (x1, x2) or y in (2, 13) or (y - 1 in ROWS and not ROWS[y - 1][0] <= x <= ROWS[y - 1][1]) \
                or (y + 1 in ROWS and not ROWS[y + 1][0] <= x <= ROWS[y + 1][1])
            if edge:
                c = OUTLINE
            else:  # lit from the upper left
                shade = 3 - min(3, (x - x1 + y - 2) // 4)
                c = POD[max(0, min(3, 3 - shade + 1)) if x + y < 12 else 1 if x + y > 18 else 2]
            img.putpixel((x, y), c + (255,))
    for x, y in ((4, 11), (5, 12), (11, 4), (10, 3)):  # the helix band crossing the pod
        img.putpixel((x, y), HELIX[0] + (255,))
    for x, y in ((5, 11), (10, 4)):
        img.putpixel((x, y), HELIX[1] + (255,))
    img.putpixel((5, 4), POD[3] + (255,))  # a glint
    return img


def icon(name, sign):
    img = pod()
    for x, y in sign:  # a tread or top edge catches the light; the rest is in shade
        img.putpixel((x, y), BONE[0 if (x, y - 1) in sign else 1] + (255,))
    (ASSETS / 'textures/item').mkdir(parents=True, exist_ok=True)
    img.save(ASSETS / f'textures/item/{name}.png', optimize=True)
    (ASSETS / f'models/item/{name}.json').write_text(json.dumps(
        {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'infestusfrontier:item/{name}'}},
        indent=2) + '\n')


def main():
    icon('stairs_construct', STEPS)
    icon('wider_section_construct', SQUARE)
    print('constructs: 2 icons, 2 item models')


if __name__ == '__main__':
    main()
