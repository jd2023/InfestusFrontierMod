"""Static transport organs: an attachment cuff, ribbed throat and open lips. North is the partner end."""
from organ_model import Painter, faces, folds, islands, pack, skin, write_static

SHEET = 64
FLESH = [(65, 27, 40), (106, 43, 60), (150, 73, 85), (190, 111, 116), (223, 154, 148)]
LIP = [(82, 29, 49), (127, 48, 73), (178, 87, 111), (224, 143, 154)]
IVORY = [(109, 91, 76), (166, 144, 112), (215, 197, 157), (244, 230, 197)]
INSIDE = [(25, 12, 23), (43, 18, 32), (69, 28, 46)]


def make(name, intake):
    cubes = [
        ('cuff', [-5, 3, 6], [10, 10, 2], None),
        ('throat', [-3, 5, -4], [6, 6, 10], None),
        ('upper', [-5, 11, -8], [10, 2, 4], None),
        ('lower', [-5, 3, -8], [10, 2, 4], None),
        ('left', [-5, 5, -8], [2, 6, 4], None),
        ('right', [3, 5, -8], [2, 6, 4], None),
        ('lumen', [-3, 5, -5], [6, 6, 1], None),
        ('rib', [-4, 4, 2], [8, 8, 1], None),
        ('rib', [-4, 4, -1], [8, 8, 1], None),
    ]
    if not intake:
        cubes += [('tooth', [-2, 10, -8], [1, 1, 3], None),
                  ('tooth', [1, 5, -8], [1, 1, 3], None)]
    bones = [('root', None, [0, 8, 0], cubes)]
    uv = pack(bones, SHEET)
    painter = Painter(SHEET, 227 if intake else 229)
    for island, size in islands(bones).items():
        for face, rect in faces(size, uv[island]).items():
            ramp = INSIDE if island == 'lumen' else IVORY if island == 'tooth' else LIP if island in ('upper', 'lower', 'left', 'right') else FLESH
            light = 0.14 if face == 'up' else -0.16 if face == 'down' else 0
            painter.ramp(rect, ramp, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) + light,
                         grain=0.06, mottle=0.22)
            if island in ('cuff', 'throat'):
                painter.vein(rect, (92, 28, 54), 3, 140)
    write_static(name, bones, SHEET, painter)


if __name__ == '__main__':
    make('intake_mouth', True)
    make('output_mouth', False)
