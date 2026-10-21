"""Fuel Papilla: a substrate cell with a pale nipple and a green pore on top. Static model.

    python3 art/gen_fuel_papilla.py
"""
from organ_model import Painter, islands, faces, pack, write_static, skin, flat, ball

SHEET = 64
SKIN = [(96, 44, 64), (124, 60, 78), (150, 82, 92), (176, 110, 112), (198, 140, 134)]
PALE = [(170, 130, 130), (200, 160, 156), (224, 190, 184), (240, 214, 206)]
PORE = [(44, 92, 52), (96, 190, 78), (170, 255, 120), (228, 255, 198)]

BONES = [
    ('root', None, [0, 0, 0], []),
    ('cell', 'root', [0, 0, 0], [
        ('cube', [-8, 0, -8], [16, 14, 16], None),
        ('rim', [-8, 14, -8], [16, 2, 16], None),
        ('nipple', [-3, 16, -3], [6, 2, 6], None),
        ('pore', [-1, 18, -1], [2, 1, 2], None),
    ]),
]


def textures(uv):
    p = Painter(SHEET, 509)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for name in ('cube', 'rim'):
        for face, rect in rects[name].items():
            p.ramp(rect, SKIN, lambda x, y, w, h: skin(x, y, w, h) + 0.1, mottle=0.3)
    for face, rect in rects['nipple'].items():
        p.ramp(rect, PALE, lambda x, y, w, h: 0.4 + 0.4 * ball(x, y, w, h), grain=0.04, mottle=0.1)
    for face, rect in rects['pore'].items():
        p.ramp(rect, PORE, flat(0.7), grain=0.03, mottle=0.05)
    return p


def main():
    uv = pack(BONES, SHEET)
    write_static('fuel_papilla', BONES, SHEET, textures(uv))
    print('fuel_papilla: static')


if __name__ == '__main__':
    main()
