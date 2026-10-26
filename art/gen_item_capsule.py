"""Item Capsule: the pod of a Capsule Stem. A plump squared seed pod of dark plum hide with muted salmon folds, its
flat outer face toward the player (where the stock renderer draws what it holds), hung on a short fleshy neck that a
bone collar grips where it meets the stem, with a bone shelf under the neck. The pod breathes; its grade sets its
size: a young pod is small, each graft swells it toward the stem, its face staying put.

    python3 art/gen_item_capsule.py

Front is north (-Z), the pod's outer face; the neck runs south into the stem's block. The block spans y 0..16, so
the pod sits about y 8, where the stem's collar and the stock renderer's membrane are. The `body` bone scales with
the grade about the face's centre (grade_0..2) while its child `flesh` breathes, so the two never fight over one bone.
It also paints `textures/block/item_capsule_lining.png`, the dark membrane the stock renderer draws over the face.
"""
from pathlib import Path

from PIL import Image

from organ_model import Painter, faces, flat, islands, pack, sampled, wave, write

SHEET = 128
BREATH = 5.0
GRADES = (0.72, 0.86, 1.0)
POD = [(34, 13, 18), (56, 20, 28), (80, 32, 40), (104, 46, 52), (124, 58, 62)]
SALMON = [(148, 76, 66), (192, 112, 94), (222, 146, 120)]
STEM = [(38, 12, 18), (66, 22, 32), (92, 36, 44), (114, 50, 56)]
BONE = [(172, 154, 124), (208, 192, 160), (232, 220, 196), (246, 240, 226)]
LINING = [(16, 6, 10), (28, 10, 16), (44, 18, 26), (70, 30, 40)]
FACE = [(22, 8, 12), (40, 14, 20), (60, 24, 30), (84, 36, 42)]

MID = 8
BONES = [
    ('root', None, [0, MID, 0], []),
    # the neck into the stem, a shelf of bone under it and the collar that grips it at the stem
    ('neck', 'root', [0, MID, 5], [
        ('neck', [-2, MID - 2, 1.5], [4, 4, 10], None),
        ('shelf', [-2, MID - 4.5, 1.5], [4, 2, 7], None),
    ]),
    ('collar', 'root', [0, MID, 9], [('collar', [-3, MID - 3, 8.5], [6, 6, 2], None)]),
    # the pod: scaled by grade about the centre of its face, so the face stays where the stock is drawn
    ('body', 'root', [0, MID, -6.5], []),
    ('flesh', 'body', [0, MID, -6.5], [
        ('hull', [-6, MID - 6, -5.5], [12, 12, 11], None),
        ('crown', [-5, MID + 6, -4.5], [10, 1, 8], None),
        ('keel', [-5, MID - 7, -4.5], [10, 1, 8], None),
        ('face', [-4, MID - 4, -6.5], [8, 8, 1], None),
        ('shoulder', [-4, MID - 4, 5.5], [8, 8, 1], None),
    ]),
]


def folds(p, rect, streaks):
    """Muted salmon folds: short diagonal streaks of hide pushed up, a crease beside each."""
    x0, y0, w, h = rect
    for sx, sy, length in streaks:
        for i in range(length):
            x, y = sx + i, sy + i
            if 0 <= x < w - 2 and 0 <= y < h:
                p.put(rect, x, y, SALMON[2 if i in (1, 2) else 1])
                p.put(rect, x + 1, y, SALMON[0 if i in (0, length - 1) else 1])
                p.put(rect, x + 2, y, POD[0])


def textures(uv):
    p = Painter(SHEET, 811)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['hull'].items():
        p.ramp(rect, POD, flat(0.72 if face == 'up' else 0.3 if face == 'down' else 0.55), grain=0.05, mottle=0.35)
        if face in ('west', 'east'):
            folds(p, rect, [(1, 1, 5), (6, 5, 4)])
        elif face in ('up', 'down'):
            folds(p, rect, [(2, 2, 4), (7, 4, 3)])
        elif face == 'south':
            folds(p, rect, [(3, 2, 4)])
    for name in ('crown', 'keel'):
        for face, rect in rects[name].items():
            p.ramp(rect, POD, flat(0.8 if face == 'up' else 0.25 if face == 'down' else 0.5), grain=0.05, mottle=0.3)
    for face, rect in rects['shoulder'].items():
        p.ramp(rect, POD, flat(0.4), grain=0.04, mottle=0.3)
    for face, rect in rects['face'].items():
        # the face: a slightly lighter rim round the quiet dark hide the membrane sits on
        p.ramp(rect, FACE, lambda x, y, w, h: 0.75 if x in (0, w - 1) or y in (0, h - 1) else 0.45, grain=0.03, mottle=0.2)
    for face, rect in rects['neck'].items():
        p.ramp(rect, STEM, lambda x, y, w, h: 0.55 - 0.2 * (x % 2 == 0), grain=0.03, mottle=0.1)
    for name in ('shelf', 'collar'):
        for face, rect in rects[name].items():
            p.ramp(rect, BONE, lambda x, y, w, h, face=face: (0.8 if face == 'up' else 0.55) - 0.15 * ((x + 2 * y) % 5 == 0),
                   grain=0.03, mottle=0.1)
    return p, rects['hull']['north']


def still(scale):
    return sampled(1.0, 1.0, lambda t: [scale, scale, scale])


def animations():
    idle = {
        'flesh': {'scale': sampled(BREATH, 0.1, lambda t: [1 + 0.025 * wave(t, BREATH), 1 + 0.02 * wave(t, BREATH), 1 + 0.025 * wave(t, BREATH)])},
    }
    out = {'idle': (BREATH, True, idle)}
    for grade, scale in enumerate(GRADES):
        out['grade_%d' % grade] = (1.0, True, {'body': {'scale': still(scale)}})
    return out


def lining():
    """The display membrane: near-black wet flesh, a pale rim round the window and one wet highlight."""
    img = Image.new('RGB', (16, 16))
    for y in range(16):
        for x in range(16):
            tone = 1 if (x * 7 + y * 3) % 5 == 0 else 0
            if (x in (4, 11) and 4 <= y <= 11) or (y in (4, 11) and 4 <= x <= 11):
                tone = 3
            img.putpixel((x, y), LINING[tone])
    img.putpixel((5, 5), LINING[3])
    img.putpixel((6, 5), LINING[2])
    img.save(Path('src/main/resources/assets/infestusfrontier/textures/block/item_capsule_lining.png'), optimize=True)


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('item_capsule', BONES, SHEET, painter, animations(), (particle[0], particle[1]))
    lining()
    print('item_capsule: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
