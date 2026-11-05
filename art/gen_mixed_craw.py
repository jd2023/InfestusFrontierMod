"""Mixed Craw: the mutation of a Capsule Stem pod that holds four kinds. A plump pod of bog-green hide, its creases
pushed up in pale bone, with a wide socket in its outer face behind a one-pixel rim. A low ivory septum crosses the
socket floor and divides it into four pockets in a square, each a dark wet cup with a lip, so the four kinds read even
while the pockets are empty. Two ivory ribs, knuckled at both ends, run down its flanks. Against a bulk pod (plum hide,
one deep pocket, a square back) the green, the cross and the ribs tell a Craw apart from four blocks away.

    python3 art/gen_mixed_craw.py

The neck, shelf and collar, the hull's front, the socket depth, the grades and the pivots are the bulk pod's
(art/gen_item_capsule.py), so a Craw sits on its stem where a pod did and the stock renderer's pockets fit it:
StorageModule's Pockets(2, 5.5, 10.5, 5, 3.4, 3.8, 2.4, 4.5) are four cells 4.8 pixels wide on a 5 pixel pitch inside
the 10 pixel socket this file leaves, the socket floor 4.5 pixels behind the block's front. The septum stands 0.4
pixels proud of the floor, behind the icons and counts the renderer draws 0.7 pixels in front of it. The hull keeps
the pod's 12 by 14 front and its outline (ItemCapsuleBlock's shapes); behind the front it rounds off in a haunch and
a shoulder instead of the pod's square back. The breath only swells the hull, so the rim never closes on a pocket.
The model texture is 64x64 and the break particle 64x64; the shared lining stays the pod's.
"""
import math

import gen_item_capsule as pod
from organ_model import Painter, faces, flat, islands, pack, sampled, wave, write

SHEET = 64
MID, BACK, FLOOR, DEPTH = pod.MID, pod.BACK, pod.FLOOR, pod.RIM_DEPTH
RIM = 1
# bog green, from the wet shadow to the lit crown
HIDE = [(18, 24, 16), (30, 40, 26), (44, 58, 34), (60, 76, 44), (78, 96, 56), (100, 118, 70)]
# the creases and the bone: pale, a little yellowed
CREASE = [(150, 146, 112), (196, 192, 156), (226, 222, 192)]
BONE = [(112, 104, 84), (158, 150, 122), (200, 192, 162), (228, 222, 196), (246, 242, 226)]
CUP = [(10, 12, 10), (20, 24, 18), (34, 40, 30), (54, 62, 44)]
STEM = pod.STEM

# the septum: a cross 0.6 pixels wide, its ends inside the rim, 0.4 proud of the floor (the cross bar a little less, so
# their fronts never share a plane); a knot where they meet
SEPTUM = [
    ('septum_v', [-0.3, MID - 5.5, FLOOR - 0.4], [0.6, 11, 0.8], None),
    ('septum_h', [-5.5, MID - 0.3, FLOOR - 0.25], [11, 0.6, 0.5], None),
    ('knot', [-0.6, MID - 0.6, FLOOR - 0.55], [1.2, 1.2, 0.7], None),
]
# a rib down each flank, half a pixel proud of the hull, knuckled where it meets the crown and the keel
RIB_Z = FLOOR + 1.25


def rib(side):
    x = -6.5 if side == 'w' else 5.5
    k = -6.7 if side == 'w' else 5.3
    return [
        ('rib', [x, MID - 5.5, RIB_Z], [1, 11, 1.5], None),
        ('knuckle', [k, MID + 4.3, RIB_Z - 0.45], [1.4, 1.5, 2.4], None),
        ('knuckle', [k, MID - 5.8, RIB_Z - 0.45], [1.4, 1.5, 2.4], None),
    ]


BONES = [
    ('root', None, [0, MID, 0], []),
    ('neck', 'root', [0, MID, 5], [
        ('neck', [-2, MID - 2, 1.5], [4, 4, 10], None),
        ('shelf', [-1.5, MID - 3.7, 2], [3, 2, 8], None),
    ]),
    ('collar', 'root', [0, MID, 9], [('collar', [-3, MID - 3, 8.5], [6, 6, 2], None)]),
    # scaled by grade about the hull's back, as a pod is
    ('body', 'root', [0, MID, BACK], []),
    ('flesh', 'body', [0, MID, FLOOR], [
        ('hull', [-6, MID - 6, FLOOR], [12, 12, 6], None),
        ('haunch', [-5, MID - 5, FLOOR + 5.25], [10, 10, 3.75], None),
        ('shoulder', [-4, MID - 4, BACK], [8, 8, 1], None),
        ('crown', [-5, MID + 6, FLOOR + 0.5], [10, 1, 5.5], None),
        ('crown', [-5, MID - 7, FLOOR + 0.5], [10, 1, 5.5], None),
        # the rim round the socket, one pixel wide: its inner faces are the socket's walls
        ('brow', [-6, MID + 6 - RIM, FLOOR - DEPTH], [12, RIM, DEPTH], None),
        ('chin', [-6, MID - 6, FLOOR - DEPTH], [12, RIM, DEPTH], None),
        ('cheek_w', [-6, MID - 6 + RIM, FLOOR - DEPTH], [RIM, 12 - 2 * RIM, DEPTH], None),
        ('cheek_e', [6 - RIM, MID - 6 + RIM, FLOOR - DEPTH], [RIM, 12 - 2 * RIM, DEPTH], None),
        *SEPTUM,
        *rib('w'),
        *rib('e'),
    ]),
]


def hide(face):
    """Rounded shading for a face of the hull, lit from above and the upper left: bright in the middle, falling off to
    the edges, so the box reads as a swollen pod."""
    lift = {'up': 0.18, 'down': -0.2, 'west': 0.04, 'east': -0.04, 'south': -0.08}.get(face, 0)

    def value(x, y, w, h):
        dx, dy = (x + 0.5) / w - 0.45, (y + 0.5) / h - 0.4
        return 0.66 + lift - 0.9 * (dx * dx + dy * dy)
    return value


def trace(points):
    """The texels of a polyline through `points`, one per step along its longer axis."""
    out = []
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + (1 if (x1, y1) == points[-1] else 0)):
            out.append((x0 + round((x1 - x0) * i / n), y0 + round((y1 - y0) * i / n)))
    return out


def creases(p, rect, paths, hide=HIDE, crease=CREASE):
    """Pale bone creases: wandering ridges of hide pushed up, bright along the middle and duller at the ends, with a
    wet shadow under the lit side."""
    for path in paths:
        texels = trace(path)
        ridge = set(texels)
        for i, (x, y) in enumerate(texels):
            if (x, y + 1) not in ridge and 0 < i < len(texels) - 1:
                p.put(rect, x, y + 1, hide[0])
        for i, (x, y) in enumerate(texels):
            p.put(rect, x, y, crease[2 if 0 < i < len(texels) - 1 else 1])


def cups(p, rect):
    """The socket floor seen inside the rim: four dark cups, each with a lip and one wet glint at its upper left."""
    x0, y0, w, h = rect
    p.ramp(rect, CUP, flat(0.4), grain=0.03, mottle=0.15)
    for cx in (1, 6):
        for cy in (1, 6):
            for y in range(5):
                for x in range(5):
                    edge = x in (0, 4) or y in (0, 4)
                    p.put(rect, cx + x, cy + y, CUP[3 if edge and (x + y) % 3 else 2] if edge else CUP[0 if (x, y) != (2, 2) else 1])
            p.put(rect, cx + 1, cy + 1, CUP[3])


def bone_fill(p, rect, face, palette=BONE, along_y=False):
    """A bone surface from `palette`: the top face lit, the end texels along its length darker where it meets the
    hide, and a sparse speckle of bone grain."""
    def value(x, y, w, h):
        i, n = (y, h) if along_y else (x, w)
        end = min(i, n - 1 - i)
        return (0.78 if face == 'up' else 0.62) - 0.22 * (end == 0 and n > 2) - 0.12 * ((x + 2 * y) % 5 == 0)
    p.ramp(rect, palette, value, grain=0.03, mottle=0.1)


def whole(rect):
    """A face's texels: a fractional cube owns every texel it touches (organ_model.pack keeps them to itself)."""
    x, y, w, h = rect
    return math.floor(x), math.floor(y), math.ceil(x + w) - math.floor(x), math.ceil(y + h) - math.floor(y)


def textures(uv):
    p = Painter(SHEET, 907)
    sizes = islands(BONES)
    rects = {name: {face: whole(rect) for face, rect in faces(sizes[name], uv[name]).items()} for name in sizes}
    for face, rect in rects['hull'].items():
        if face == 'north':
            cups(p, rect)
            continue
        p.ramp(rect, HIDE, hide(face), grain=0.05, mottle=0.3)
        if face in ('west', 'east'):
            creases(p, rect, [[(1, 1), (2, 3), (2, 5)], [(4, 6), (3, 8), (4, 10)]])
        elif face in ('up', 'down'):
            creases(p, rect, [[(0, 4), (3, 3), (5, 4)]])
    for face, rect in rects['haunch'].items():
        p.ramp(rect, HIDE, lambda x, y, w, h, face=face: hide(face)(x, y, w, h) - 0.08, grain=0.05, mottle=0.3)
        if face in ('west', 'east', 'up', 'down'):
            creases(p, rect, [[(1, 2), (2, 5), (1, 7)]])
        elif face == 'south':
            creases(p, rect, [[(1, 2), (4, 3), (6, 2)], [(3, 6), (6, 7), (8, 6)]])
    for face, rect in rects['shoulder'].items():
        p.ramp(rect, HIDE, hide(face), grain=0.04, mottle=0.25)
        if face == 'south':
            creases(p, rect, [[(1, 4), (3, 3), (6, 4)]])
    for face, rect in rects['crown'].items():
        p.ramp(rect, HIDE, flat(0.86 if face == 'up' else 0.24 if face == 'down' else 0.56), grain=0.05, mottle=0.3)
        if face in ('up', 'down'):
            creases(p, rect, [[(1, 1), (4, 2), (6, 4)], [(8, 0), (9, 2)]])
    # the rim: a lit lip of hide round the socket, its inner walls the cups' dark flesh
    walls = {'brow': 'down', 'chin': 'up', 'cheek_w': 'east', 'cheek_e': 'west'}
    for name, inner in walls.items():
        for face, rect in rects[name].items():
            if face == inner:
                p.ramp(rect, CUP, flat(0.62), grain=0.03, mottle=0.15)
            elif face == 'north':
                p.ramp(rect, HIDE, lambda x, y, w, h: 0.86 - 0.12 * ((x + y) % 4 == 0), grain=0.04, mottle=0.2)
            else:
                p.ramp(rect, HIDE, flat(0.74 if face == 'up' else 0.3 if face == 'down' else 0.58), grain=0.05, mottle=0.3)
    for face, rect in rects['neck'].items():
        p.ramp(rect, STEM, lambda x, y, w, h: 0.55 - 0.2 * (x % 2 == 0), grain=0.03, mottle=0.1)
    for name in ('shelf', 'collar'):
        for face, rect in rects[name].items():
            p.ramp(rect, pod.BONE, lambda x, y, w, h, face=face: (0.8 if face == 'up' else 0.55) - 0.15 * ((x + 2 * y) % 5 == 0),
                   grain=0.03, mottle=0.1)
    for name in ('septum_v', 'septum_h', 'knot', 'knuckle'):
        for face, rect in rects[name].items():
            bone_fill(p, rect, face, along_y=name == 'septum_v')
    for face, rect in rects['rib'].items():
        bone_fill(p, rect, face, along_y=True)
    return p


def particle():
    """The break particle: a 64 by 64 patch of the bog-green hide with its pale creases and a fleck of rib bone."""
    p = Painter(64, 911)
    rect = (0, 0, 64, 64)
    p.ramp(rect, HIDE, lambda x, y, w, h: 0.48 + 0.2 * math.sin(x / 8.0 + y / 15.0) * math.cos(y / 10.0), grain=0.05, mottle=0.45)
    creases(p, rect, [[(3, 8), (8, 5), (13, 7), (17, 4)], [(28, 3), (31, 8), (36, 10)], [(10, 22), (15, 25), (21, 23)],
                      [(40, 20), (45, 24), (51, 22), (56, 26)], [(4, 44), (9, 40), (12, 41)], [(26, 46), (31, 49), (37, 47)],
                      [(46, 52), (50, 56), (55, 55)], [(18, 58), (23, 60)], [(58, 36), (61, 40)]])
    for x, y in ((52, 10), (53, 10), (52, 11), (16, 34), (17, 34)):
        p.put(rect, x, y, BONE[3])
    return p.img


def animations():
    """The pod's breath and grades. The hull only swells, between its rest size and 2.4 per cent wider and taller, so
    the rim's inner faces never move in over the pockets; front to back it breathes as a pod does."""
    idle = {'flesh': {'scale': sampled(pod.BREATH, 0.1, lambda t: [
        1.012 + 0.012 * wave(t, pod.BREATH), 1.01 + 0.01 * wave(t, pod.BREATH), 1 + 0.025 * wave(t, pod.BREATH)])}}
    out = {'idle': (pod.BREATH, True, idle)}
    for grade, scale in enumerate(pod.GRADES):
        out['grade_%d' % grade] = (1.0, True, {'body': {'scale': pod.still(scale)}})
    return out


def main():
    painter = textures(pack(BONES, SHEET))
    write('mixed_craw', BONES, SHEET, painter, animations(), particle())
    print('mixed_craw: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
