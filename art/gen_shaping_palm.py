"""Shaping Palm (BLOCK_CATALOG T1-41): a living hand that keeps a crafting grid. An open hand of pink muscle lies palm
up on a root foot, its heel to the front. Four fingers rise along the back of the palm and curl forward over it, and
a thumb splays from a fleshy mound at the side and turns in; every tip is ivory. In the cup of
the palm lies an ivory plate with nine studs in three rows of three: the crafting grid it keeps. The studs are the
cue, a crafting table's grid; the cupped fingers say it holds what it is given.

    python3 art/gen_shaping_palm.py

Idle: the fingers breathe slowly in turn and the palm swells a little; nothing is lit. Working (one 2 s Rote cycle):
the fingers and the thumb curl over the plate, little finger first, and open again, and a faint glow rises round the
studs while they are closed. The glow is a shell round each stud that the model texture leaves clear and the glowmask
fills; it is scaled to nothing outside the working clip, so the item, a manual craft and an idle Palm stay unlit.

Front is north (-Z). GeckoLib mirrors the file's x and negates rotations about x and y (art/geo_audit.py): in the
file, a positive turn about x leans a cube's top forward (north) and a positive turn about z leans it towards +x.
The fingers are chains of bones, each joint's pivot at the turned end of the segment before it, so a curl never
parts a segment from the one it grows from. Nothing reaches past 7.4 pixels sideways or stands higher than 11.
"""
import math

from organ_model import Painter, ball, faces, flat, islands, pack, sampled, skin, wave, write

SHEET = 64
IDLE, WORKING = 8.0, 2.0  # a Rote cycle is 40 ticks
MUSCLE = [(112, 40, 66), (152, 62, 90), (190, 92, 116), (220, 124, 142), (238, 160, 168), (250, 198, 198)]
BONE = [(96, 86, 74), (146, 134, 112), (190, 178, 150), (222, 212, 186), (244, 238, 218)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
VESSEL = (128, 34, 52)
CREASE = (104, 40, 62)
GLOW = (255, 226, 178)
GLOW_ALPHA = 90  # faint: a pale light over the studs, not a lamp

PLATE_TOP = 5.5
STUD_TOP = 6.25
STUD_X = (-2, 0, 2)
STUD_Z = (-3, -1, 1)


def turned(point, pivot, rx=0.0, rz=0.0):
    """Where `point` lands when its cube turns [rx, 0, rz] about `pivot`, in the file's space (see the docstring)."""
    x, y, z = (point[i] - pivot[i] for i in range(3))
    a = math.radians(-rx)  # about x first
    y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    b = math.radians(-rz)  # then about z
    x, y = x * math.cos(b) - y * math.sin(b), x * math.sin(b) + y * math.cos(b)
    return [round(pivot[0] + x, 4), round(pivot[1] + y, 4), round(pivot[2] + z, 4)]


def segment(island, pivot, length, rx=0.0, rz=0.0, sink=0.5):
    """A 2 by 2 finger segment standing on `pivot`, `sink` of it below the joint, turned about the joint.
    Returns the cube and the joint at its far end, where the next segment's bone pivots."""
    x, y, z = pivot
    cube = (island, [x - 1, y - sink, z - 1], [2, length, 2], ([rx, 0, rz], list(pivot)) if rx or rz else None)
    return cube, turned([x, y + length - sink, z], pivot, rx, rz)


# The fingers, from the little finger (-x in the file) to the index: x of the finger, the two segment lengths, how
# far each segment leans forward at rest, and the tip's lean towards the middle of the hand.
FINGERS = [
    ('little', -4.5, (3, 2), (-4, 34, 5)),
    ('ring', -1.5, (4, 3), (-2, 36, 3)),
    ('middle', 1.5, (4, 3), (-2, 38, -3)),
    ('index', 4.5, (3, 3), (-4, 34, -5)),
]
KNUCKLE_Y, KNUCKLE_Z = 5.25, 4.0
THUMB_BASE = [4.25, 4.25, -2.5]
THUMB = ((4, 3), (30, 4), (-6, 24))  # segment lengths, outward lean (about z) and forward lean (about x) of each


def finger_bones():
    """Each finger is two bones: the near segment on the pad and the tip on the near segment's far end. A tip also
    leans a few degrees towards the middle of the hand, so its flanks never lie in the planes of the near segment's."""
    bones = []
    for name, x, (near, far), (lean, curl, inward) in FINGERS:
        base = [x, KNUCKLE_Y, KNUCKLE_Z]
        first, joint = segment('phalanx_%d' % near, base, near, rx=lean, sink=1.0)
        second, _ = segment('tip_%d' % far, joint, far, rx=curl, rz=inward)
        bones.append((name, 'pad', base, [first]))
        bones.append((name + '_tip', name, joint, [second]))
    (near, far), (out_near, out_far), (fwd_near, fwd_far) = THUMB
    first, joint = segment('thumb', THUMB_BASE, near, rx=fwd_near, rz=out_near, sink=1.0)
    second, _ = segment('tip_%d' % far, joint, far, rx=fwd_far, rz=out_far)
    bones.append(('thumb', 'pad', THUMB_BASE, [first]))
    bones.append(('thumb_tip', 'thumb', joint, [second]))
    return bones


def claw(sx, sz):
    cx, cz = sx * 5.25, sz * 5.25
    return ('rootlet', [cx - 1, 0.5, cz - 2], [2, 1, 4], ([0, 45 * sx * sz, 0], [cx, 1, cz]))


STUDS = [('stud', [x - 0.5, STUD_TOP - 1, z - 0.5], [1, 1, 1], None) for z in STUD_Z for x in STUD_X]
# the glow: a shell a quarter pixel off every face of a stud, clear in the model texture
SHELLS = [('shell', [x - 0.75, STUD_TOP - 1.15, z - 0.75], [1.5, 1.5, 1.5], None) for z in STUD_Z for x in STUD_X]

BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('slab', [-6, 0, -6], [12, 2, 12], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
    ]),
    ('pad', 'root', [0, 1.75, 0], [
        ('palm', [-5, 1.75, -5], [10, 3, 9], None),
        ('heel', [-4, 1.5, -5.75], [8, 2, 2], None),
        ('hypothenar', [-5.25, 4, -4], [2, 2, 5], None),
        ('thenar', [3.5, 3.5, -4.5], [2, 2, 4], None),
        ('ridge', [-4.5, 4, 2.5], [9, 2, 2], None),
        ('plate', [-3, PLATE_TOP - 1, -4], [6, 1, 6], None),
    ] + STUDS),
    ('glow', 'pad', [0, PLATE_TOP, -1], SHELLS),
] + finger_bones()


def textures(uv):
    p = Painter(SHEET, 641)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}

    def muscle(rect, face, lift=0.0):
        # pink muscle, lit from the upper left, a wet sheen on the tops
        p.ramp(rect, MUSCLE, lambda x, y, w, h: 0.3 * skin(x, y, w, h) + 0.55 * ball(x, y, w, h) + 0.12 + lift
               + (0.1 if face == 'up' else 0), grain=0.03, mottle=0.35)

    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.6 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(5):
                p.vein(rect, ROOT[0], 5)
    for face, rect in rects['rootlet'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.7 if face == 'up' else 0.45) - 0.15 * (x % 3 == 1))
    for name in ('palm', 'heel', 'hypothenar', 'thenar', 'ridge'):
        for face, rect in rects[name].items():
            muscle(rect, face, 0.05 if name in ('thenar', 'hypothenar') else 0)
            if face in ('east', 'west', 'south') and name == 'palm':
                p.vein(rect, VESSEL, 3, 150)
        if name == 'palm':
            # the lines of the palm run across its top, round the plate
            up = rects['palm']['up']
            for x in range(1, up[2] - 1):
                p.put(up, x, 1 + (x % 5 == 0), CREASE)
                p.put(up, x, up[3] - 2, CREASE, 200)
    for face, rect in rects['ridge'].items():
        if face in ('south', 'up'):
            # knuckle creases on the back of the hand, between the fingers
            for x in (2, 5, 7):
                for y in range(rect[3]):
                    p.put(rect, x if face == 'up' else rect[2] - 1 - x, y, CREASE, 220)
    for name in [n for n in rects if n.startswith(('phalanx_', 'thumb'))]:
        for face, rect in rects[name].items():
            muscle(rect, face, 0.05)
            if face in ('south', 'north') and rect[3] > 2:
                p.put(rect, 0, rect[3] // 2, CREASE)
                p.put(rect, 1, rect[3] // 2, CREASE)
    for name in [n for n in rects if n.startswith('tip_')]:
        for face, rect in rects[name].items():
            # pink at the joint, ivory over the last two pixels and on the end
            _, _, w, h = rect
            if face == 'up':
                p.ramp(rect, BONE, flat(0.9), grain=0.03, mottle=0.08)
            elif face == 'down':
                muscle(rect, face)
            else:
                # a side face's first rows are the segment's far end
                p.ramp(rect, MUSCLE, lambda x, y, w, h: 0.4 + 0.25 * (y / max(1, h - 1)), grain=0.03, mottle=0.3)
                for y in range(2 if h > 2 else 1):
                    for x in range(w):
                        p.put(rect, x, y, BONE[4 if y == 0 and x == 0 else 3 if y == 0 else 2])
    for face, rect in rects['plate'].items():
        # an ivory plate, darker at its rim
        if face == 'up':
            value = lambda x, y, w, h: 0.52 - 0.2 * (x in (0, w - 1) or y in (0, h - 1))
        else:
            value = flat(0.6)
        p.ramp(rect, BONE, value, grain=0.04, mottle=0.1)
    for face, rect in rects['stud'].items():
        p.ramp(rect, BONE, flat(0.98 if face == 'up' else 0.72), grain=0.0, mottle=0.0)
    # the shell: clear in the model texture, a faint warm light in the glowmask
    u, v = uv['shell']
    w, h, d = sizes['shell']
    for y in range(v, v + math.ceil(h + d)):
        for x in range(u, u + math.ceil(2 * w + 2 * d)):
            p.glow.putpixel((x, y), GLOW + (GLOW_ALPHA,))
    return p


def particle():
    """The break particle: a 64 by 64 patch of the palm's muscle with a crease and an ivory fleck or two."""
    p = Painter(64, 643)
    rect = (0, 0, 64, 64)
    p.ramp(rect, MUSCLE, lambda x, y, w, h: 0.5 + 0.25 * math.sin(x / 9.0 + y / 13.0) * math.cos(y / 11.0), grain=0.05, mottle=0.5)
    for _ in range(6):
        p.vein(rect, VESSEL, 18, 200)
    for x in range(64):
        y = int(30 + 6 * math.sin(x / 10.0))
        p.put(rect, x, y, CREASE)
    for cx, cy in ((14, 12), (46, 48)):
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if dx * dx + dy * dy <= 4:
                    p.put(rect, cx + dx, cy + dy, BONE[3 if dx + dy < 0 else 2])
    return p.img


def animations():
    def flex(lag, amount, axis=0):
        def fn(t):
            v = [0, 0, 0]
            v[axis] = amount * wave(t, IDLE, lag)
            return v
        return {'rotation': sampled(IDLE, 0.2, fn)}

    idle = {
        'pad': {'scale': sampled(IDLE, 0.4, lambda t: [1 + 0.015 * wave(t, IDLE), 1, 1 + 0.015 * wave(t, IDLE)])},
        'glow': {'scale': sampled(1.0, 1.0, lambda t: [0, 0, 0])},
        'thumb': flex(IDLE * 0.8, 3, 2),
        'thumb_tip': flex(IDLE * 0.9, 3),
    }
    for i, (name, _, _, _) in enumerate(FINGERS):
        idle[name] = flex(IDLE * 0.15 * i, 3)
        idle[name + '_tip'] = flex(IDLE * 0.15 * i + 0.6, 4)

    def close(t, lag=0.0):
        """0 open .. 1 closed and back over one cycle: the fingers close for the first half and open for the second."""
        k = ((t - lag) % WORKING) / WORKING
        return (1 - math.cos(2 * math.pi * k)) / 2

    def curl(lag, near, far):
        return ({'rotation': sampled(WORKING, 0.05, lambda t: [near * close(t, lag), 0, 0])},
                {'rotation': sampled(WORKING, 0.05, lambda t: [far * close(t, lag), 0, 0])})

    working = {
        'pad': {'scale': sampled(WORKING, 0.1, lambda t: [1 - 0.02 * close(t), 1, 1 - 0.02 * close(t)])},
        'glow': {'scale': sampled(WORKING, 0.05, lambda t: [1, 0.86 + 0.14 * close(t, 0.1), 1])},
        'thumb': {'rotation': sampled(WORKING, 0.05, lambda t: [6 * close(t, 0.12), 0, -24 * close(t, 0.12)])},
        'thumb_tip': {'rotation': sampled(WORKING, 0.05, lambda t: [0, 0, -18 * close(t, 0.12)])},
    }
    for i, (name, _, _, _) in enumerate(FINGERS):
        working[name], working[name + '_tip'] = curl(0.04 * i, 26, 30)
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter = textures(pack(BONES, SHEET))
    write('shaping_palm', BONES, SHEET, painter, animations(), particle(), bounds=(2, 1, (0, 0.5, 0)))
    print('shaping_palm: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
