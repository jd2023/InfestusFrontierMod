"""Larder: the mutation of a Mixed Craw that keeps a reserve. A plump pod of waxed amber hide, its creases pushed up in
pale bone, girt by an ivory strap along each flank that ends at the front in a latch hooked over the socket's lip. The
socket behind its one-pixel rim holds three pockets, two above one: the fourth quarter, low on the right as you face
it, is sealed flush with a plug of hardened resin, the room a Larder pays for its reserves. A low ridge of pale bone
parts the three pockets, each a dark wet cup with a lip, so they read as three even while empty. Against the Craw's
bog green, ivory cross and ribs, and the bulk pod's plum hide and single socket, the amber, the resin seal and the
latches tell a Larder apart from four blocks away.

    python3 art/gen_larder.py

The neck, shelf and collar, the hull's 12 by 14 front, the socket depth, the grades and the pivots are the bulk pod's
(art/gen_item_capsule.py), so a Larder sits on its stem where a pod did and the stock renderer's pockets fit it:
StorageModule's Pockets(2, 5.5, 10.5, 5, 3.4, 3.8, 2.4, 4.5) are three cells 4.8 pixels wide on a 5 pixel pitch inside
the 10 pixel socket this file leaves, the first two in the top row and the third below the first (the viewer's left,
the file's -x: GeckoLib mirrors x), the socket floor 4.5 pixels behind the block's front. The seal's inner faces lie
on the socket's middle lines, a tenth of a pixel outside the cells; the ridge stands 0.4 pixels proud of the floor,
behind the icons and counts the renderer draws 0.7 pixels in front of it. The breath only swells the hull about the
socket's middle, so neither the rim nor the seal closes on a pocket. The model texture is 64x64 and the break particle
64x64; the shared lining stays the pod's.
"""
import math

import gen_item_capsule as pod
from gen_mixed_craw import animations, bone_fill, creases, hide, trace, whole
from organ_model import Painter, faces, flat, islands, pack, write

SHEET = 64
MID, BACK, FLOOR, DEPTH = pod.MID, pod.BACK, pod.FLOOR, pod.RIM_DEPTH
RIM = 1
# waxed amber, from the wet shadow to the lit crown
HIDE = [(40, 20, 6), (70, 36, 10), (106, 60, 16), (146, 90, 28), (184, 124, 44), (214, 158, 70)]
# the wax's shine: a few texels on the lit faces
SHINE = [(236, 196, 120), (250, 226, 170)]
# the creases and the ridge: pale bone, a little yellowed
CREASE = [(150, 128, 92), (200, 180, 136), (230, 214, 176)]
BONE = [(112, 100, 80), (160, 148, 120), (204, 194, 164), (230, 224, 200), (246, 242, 228)]
# the resin seal: clear honey over a darker core
RESIN = [(92, 46, 8), (138, 76, 14), (184, 112, 26), (220, 152, 46), (242, 196, 98), (252, 230, 160)]
CUP = [(14, 9, 6), (26, 17, 10), (42, 28, 16), (62, 42, 24)]
STEM = pod.STEM

# the ridge: an L of pale bone 0.6 pixels wide parting the top pockets and the left ones, 0.4 proud of the floor (the
# cross bar a little less, so their fronts never share a plane), its far ends inside the rim and the seal
RIDGE = [
    ('ridge_v', [-0.3, MID + 0.1, FLOOR - 0.4], [0.6, 5.4, 0.8], None),
    ('ridge_h', [-5.5, MID - 0.3, FLOOR - 0.25], [5.7, 0.6, 0.5], None),
]
# the strap: a band of ivory on each flank, half a pixel proud of the hull, from the latch at the front to the haunch
STRAP_Y = MID - 0.75


def strap(side):
    """One flank's strap and latch; the latch plate hooks half a pixel over the rim's outer lip and stands a fifth of a
    pixel proud of it, and a pin heads it."""
    sign = -1 if side == 'w' else 1

    def x(near, width):
        """A cube's origin x `near` pixels from the middle on this side, `width` wide outward."""
        return -near - width if sign < 0 else near

    return [
        ('strap', [x(5.9, 0.9), STRAP_Y, FLOOR - 0.2], [0.9, 1.5, 6], None),
        ('latch', [x(5.5, 1.5), MID - 1.5, FLOOR - DEPTH - 0.2], [1.5, 3, 2.4], None),
        ('pin', [x(6.1, 0.7), MID - 0.5, FLOOR - DEPTH - 0.5], [0.7, 1, 0.6], None),
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
        # the plump middle: each flank swells half a pixel past the front, under a full haunch and shoulder
        ('flank', [-6.5, MID - 5, FLOOR + 0.75], [0.5, 10, 4.5], None),
        ('flank', [6, MID - 5, FLOOR + 0.75], [0.5, 10, 4.5], None),
        ('haunch', [-5.5, MID - 5.5, FLOOR + 6], [11, 11, 3], None),
        ('shoulder', [-4, MID - 4, BACK], [8, 8, 1], None),
        ('crown', [-5, MID + 6, FLOOR + 0.5], [10, 1, 6], None),
        ('crown', [-5, MID - 7, FLOOR + 0.5], [10, 1, 6], None),
        # the rim round the three pockets, one pixel wide: its inner faces are the socket's walls
        ('brow', [-6, MID + 6 - RIM, FLOOR - DEPTH], [12, RIM, DEPTH], None),
        ('cheek_w', [-6, MID - 6, FLOOR - DEPTH], [RIM, 11, DEPTH], None),
        ('chin', [-6 + RIM, MID - 6, FLOOR - DEPTH], [5, RIM, DEPTH], None),
        ('cheek_e', [6 - RIM, MID, FLOOR - DEPTH], [RIM, 5, DEPTH], None),
        # the resin seal filling the fourth quarter to the hull's edges, flush with the rim: its inner faces on the
        # socket's middle lines
        ('seal', [0, MID - 6, FLOOR - DEPTH], [6, 6, DEPTH], None),
        *RIDGE,
        *strap('w'),
        *strap('e'),
    ]),
]


def shine(p, rect, spots):
    """The wax: a bright texel and a softer one beside it where the light from the upper left catches the hide."""
    for x, y in spots:
        p.put(rect, x, y, SHINE[1])
        p.put(rect, x + 1, y, SHINE[0])


def cups(p, rect):
    """The socket floor seen inside the rim: three dark cups, two above one, each with a lip and one wet glint at its
    upper left. The quarter behind the seal stays plain."""
    p.ramp(rect, CUP, flat(0.4), grain=0.03, mottle=0.15)
    for cx, cy in ((1, 1), (6, 1), (1, 6)):
        for y in range(5):
            for x in range(5):
                edge = x in (0, 4) or y in (0, 4)
                p.put(rect, cx + x, cy + y, CUP[3 if edge and (x + y) % 3 else 2] if edge else CUP[0 if (x, y) != (2, 2) else 1])
        p.put(rect, cx + 1, cy + 1, CUP[3])


def resin(face):
    """The seal: clear honey, brightest at its upper left, with a darker core showing through towards the lower right."""
    def value(x, y, w, h):
        dx, dy = (x + 0.5) / w - 0.35, (y + 0.5) / h - 0.3
        return (0.78 if face == 'north' else 0.5) - 0.7 * (dx * dx + dy * dy)
    return value


def textures(uv):
    p = Painter(SHEET, 913)
    sizes = islands(BONES)
    rects = {name: {face: whole(rect) for face, rect in faces(sizes[name], uv[name]).items()} for name in sizes}
    for face, rect in rects['hull'].items():
        if face == 'north':
            cups(p, rect)
            continue
        p.ramp(rect, HIDE, hide(face), grain=0.04, mottle=0.2)
        if face in ('west', 'east'):
            creases(p, rect, [[(1, 1), (3, 2), (4, 1)]], hide=HIDE, crease=CREASE)
        elif face in ('up', 'down'):
            creases(p, rect, [[(1, 4), (4, 3), (6, 4)]], hide=HIDE, crease=CREASE)
            if face == 'up':
                shine(p, rect, [(2, 1), (8, 2)])
    for face, rect in rects['flank'].items():
        p.ramp(rect, HIDE, lambda x, y, w, h, face=face: hide(face)(x, y, w, h) + 0.04, grain=0.04, mottle=0.2)
        if face in ('west', 'east'):
            creases(p, rect, [[(1, 7), (3, 8)]], hide=HIDE, crease=CREASE)
            if face == 'west':
                shine(p, rect, [(1, 1)])
    for face, rect in rects['haunch'].items():
        p.ramp(rect, HIDE, lambda x, y, w, h, face=face: hide(face)(x, y, w, h) - 0.06, grain=0.04, mottle=0.2)
        if face == 'south':
            creases(p, rect, [[(1, 2), (4, 3), (7, 2)], [(3, 7), (6, 8), (9, 7)]], hide=HIDE, crease=CREASE)
        elif face == 'up':
            shine(p, rect, [(3, 0)])
    for face, rect in rects['shoulder'].items():
        p.ramp(rect, HIDE, hide(face), grain=0.04, mottle=0.25)
        if face == 'south':
            creases(p, rect, [[(1, 4), (3, 3), (6, 4)]], hide=HIDE, crease=CREASE)
    for face, rect in rects['crown'].items():
        p.ramp(rect, HIDE, flat(0.86 if face == 'up' else 0.24 if face == 'down' else 0.58), grain=0.04, mottle=0.2)
        if face in ('up', 'down'):
            creases(p, rect, [[(1, 2), (4, 3), (6, 2)]], hide=HIDE, crease=CREASE)
            if face == 'up':
                shine(p, rect, [(2, 1), (7, 3)])
    # the rim: a lit lip of hide round the socket, its inner walls the cups' dark flesh
    walls = {'brow': 'down', 'chin': 'up', 'cheek_w': 'east', 'cheek_e': 'west'}
    for name, inner in walls.items():
        for face, rect in rects[name].items():
            if face == inner:
                p.ramp(rect, CUP, flat(0.62), grain=0.03, mottle=0.15)
            elif face == 'north':
                p.ramp(rect, HIDE, lambda x, y, w, h: 0.9 - 0.12 * ((x + y) % 4 == 0), grain=0.04, mottle=0.2)
            else:
                p.ramp(rect, HIDE, flat(0.76 if face == 'up' else 0.3 if face == 'down' else 0.6), grain=0.05, mottle=0.3)
    # the seal: clear honey with a glint at its upper left and a pale crease where it set; its walls down into the
    # pockets the darker resin of its core
    for face, rect in rects['seal'].items():
        if face in ('west', 'up'):
            p.ramp(rect, RESIN, flat(0.3), grain=0.03, mottle=0.15)
            continue
        p.ramp(rect, RESIN, resin(face), grain=0.03, mottle=0.12)
        if face == 'north':
            for x, y in ((1, 1), (2, 1), (1, 2)):
                p.put(rect, x, y, RESIN[5])
            p.put(rect, 3, 1, RESIN[4])
            for x, y in trace([(2, 4), (3, 4), (4, 3)]):
                p.put(rect, x, y, CREASE[1])
    for face, rect in rects['neck'].items():
        p.ramp(rect, STEM, lambda x, y, w, h: 0.55 - 0.2 * (x % 2 == 0), grain=0.03, mottle=0.1)
    for name in ('shelf', 'collar'):
        for face, rect in rects[name].items():
            p.ramp(rect, pod.BONE, lambda x, y, w, h, face=face: (0.8 if face == 'up' else 0.55) - 0.15 * ((x + 2 * y) % 5 == 0),
                   grain=0.03, mottle=0.1)
    for name in ('ridge_v', 'ridge_h'):
        for face, rect in rects[name].items():
            bone_fill(p, rect, face, CREASE, along_y=name == 'ridge_v')
    for face, rect in rects['strap'].items():
        bone_fill(p, rect, face, BONE)
    for face, rect in rects['latch'].items():
        bone_fill(p, rect, face, BONE, along_y=True)
    for face, rect in rects['pin'].items():
        p.ramp(rect, BONE, flat(0.9 if face in ('north', 'up') else 0.5), grain=0.02, mottle=0.05)
    return p


def particle():
    """The break particle: a 64 by 64 patch of the waxed amber hide with its pale creases, the wax's shine and a fleck
    of strap bone."""
    p = Painter(64, 917)
    rect = (0, 0, 64, 64)
    p.ramp(rect, HIDE, lambda x, y, w, h: 0.5 + 0.2 * math.sin(x / 9.0 + y / 13.0) * math.cos(y / 11.0), grain=0.05, mottle=0.45)
    creases(p, rect, [[(3, 8), (8, 5), (13, 7), (17, 4)], [(28, 3), (31, 8), (36, 10)], [(10, 22), (15, 25), (21, 23)],
                      [(40, 20), (45, 24), (51, 22), (56, 26)], [(4, 44), (9, 40), (12, 41)], [(26, 46), (31, 49), (37, 47)],
                      [(46, 52), (50, 56), (55, 55)], [(18, 58), (23, 60)], [(58, 36), (61, 40)]], hide=HIDE, crease=CREASE)
    shine(p, rect, [(6, 14), (22, 10), (48, 6), (34, 30), (12, 34), (52, 42), (28, 56)])
    for x, y in ((52, 12), (53, 12), (52, 13), (18, 32), (19, 32)):
        p.put(rect, x, y, BONE[3])
    return p.img


def main():
    painter = textures(pack(BONES, SHEET))
    write('larder', BONES, SHEET, painter, animations(), particle())
    print('larder: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
