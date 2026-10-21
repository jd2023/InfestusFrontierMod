"""Sensor Polyp: a flesh stalk with a violet eye on top, looking north (the blockstate turns it). Static model.

    python3 art/gen_sensor_polyp.py
"""
from organ_model import Painter, islands, faces, pack, write_static, skin, folds, flat, ball

SHEET = 64
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
EYE = [(40, 20, 60), (96, 60, 140), (150, 110, 200), (210, 190, 240), (250, 246, 255)]
PUPIL = (18, 8, 24)

BONES = [
    ('root', None, [0, 0, 0], []),
    ('body', 'root', [0, 0, 0], [
        ('foot', [-5, 0, -5], [10, 2, 10], None),
        ('stalk', [-2, 2, -2], [4, 4, 4], None),
        ('eye', [-3, 6, -3], [6, 4, 6], None),
    ]),
]


def textures(uv):
    p = Painter(SHEET, 503)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['foot'].items():
        p.ramp(rect, FLESH, flat(0.4), grain=0.05, mottle=0.15)
    for face, rect in rects['stalk'].items():
        p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y), mottle=0.25)
    for face, rect in rects['eye'].items():
        if face == 'north':
            p.ramp(rect, EYE, lambda x, y, w, h: 0.4 + 0.5 * ball(x, y, w, h), grain=0.03, mottle=0.05)
            x0, y0, w, h = rect
            for yy in range(1, h - 1):
                for xx in range(2, w - 2):
                    p.put(rect, xx, yy, PUPIL)
        else:
            p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + 0.15, mottle=0.2)
    return p


def main():
    uv = pack(BONES, SHEET)
    write_static('sensor_polyp', BONES, SHEET, textures(uv))
    print('sensor_polyp: static')


if __name__ == '__main__':
    main()
