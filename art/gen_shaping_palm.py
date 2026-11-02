"""Shaping Palm (BLOCK_CATALOG T1-41): an open hand of pink muscle, palm up, on a root foot. Four fingers and a thumb
stand up round the rim and curl a little inward, and in the cup of the palm lies a bone plate studded with nine bone
studs in three rows of three: the crafting grid it keeps. The studs are the cue, a crafting table's grid; the fingers say
it holds what it is given. Nothing on it is lit: it makes nothing by itself.

    python3 art/gen_shaping_palm.py

Temporary procedural art for the functional task; the final model is an art task. Idle: the fingertips curl and
open in turn, the palm breathes. Front is north (-Z). GeckoLib mirrors the file's x and negates rotations about x and y
(art/geo_audit.py).
"""
from organ_model import Painter, ball, faces, flat, islands, pack, sampled, skin, wave, write

SHEET = 64
IDLE = 8.0
FLESH = [(70, 30, 46), (104, 48, 66), (138, 72, 86), (170, 102, 108), (198, 134, 130), (222, 170, 160)]
BONE = [(96, 86, 74), (146, 134, 112), (190, 178, 150), (222, 212, 186), (244, 238, 218)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
VESSEL = (122, 34, 44)
LEAN = 22


def claw(sx, sz):
    cx, cz = sx * 5.5, sz * 4.5
    return ('rootlet', [cx - 1, 0.5, cz - 2], [2, 1, 4], ([0, 45 * sx * sz, 0], [cx, 1, cz]))


def finger(name, side, z):
    """A finger's base on the pad and its tip, a bone of its own bent in by LEAN degrees at the top of the base."""
    x = side * 6
    base = ('knuckle', [x - 1, 1.0, z - 1], [2, 6, 2], None)
    tip = (name, 'pad', [x, 7.0, z], [('claw', [x - 1 + (-0.75 * side), 7.0, z - 0.5], [2, 3, 1], ([0, 0, side * LEAN], [x, 7.0, z]))])
    return base, tip


west_a, tip_wa = finger('tip_wa', -1, -2.5)
west_b, tip_wb = finger('tip_wb', -1, 2.5)
east_a, tip_ea = finger('tip_ea', 1, -2.5)
east_b, tip_eb = finger('tip_eb', 1, 2.5)
STUDS = [('stud', [-2.5 + 2 * i, 6.5, -2.5 + 2 * j], [1, 1, 1], None) for j in range(3) for i in range(3)]

BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('slab', [-7.5, 0, -6], [15, 2, 12], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
    ]),
    ('pad', 'root', [0, 1.5, 0], [
        ('pad', [-5.5, 1.5, -5], [11, 4, 10], None),
        west_a, west_b, east_a, east_b,
        ('thumb', [-1, 1.0, 3.5], [2, 5, 2], None),
    ]),
    ('plate', 'pad', [0, 5.5, 0], [('plate', [-3.5, 4.5, -3.5], [7, 2, 7], None)] + STUDS),
    tip_wa, tip_wb, tip_ea, tip_eb,
    ('tip_t', 'pad', [0, 6.0, 4.5], [('claw', [-1, 6.0, 4.0], [2, 3, 1], ([-LEAN, 0, 0], [0, 6.0, 4.5]))]),
]


def textures(uv):
    p = Painter(SHEET, 811)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.6 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
    for face, rect in rects['rootlet'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.7 if face == 'up' else 0.45) - 0.15 * (x % 3 == 1))
    for name in ('pad', 'knuckle', 'thumb'):
        for face, rect in rects[name].items():
            # muscle with a few red vessels, one soft light from the upper left
            p.ramp(rect, FLESH, lambda x, y, w, h: 0.3 * skin(x, y, w, h) + 0.55 * ball(x, y, w, h) + 0.1, grain=0.03, mottle=0.4)
            if face not in ('up', 'down') and name == 'pad':
                p.vein(rect, VESSEL, 4, 170)
    for face, rect in rects['claw'].items():
        p.ramp(rect, BONE, lambda x, y, w, h: 0.5 + 0.3 * (y == 0) - 0.12 * (y == h - 1), grain=0.05, mottle=0.1)
    for face, rect in rects['plate'].items():
        # an ivory plate: the top is ruled into nine cells by faint grooves
        if face == 'up':
            value = lambda x, y, w, h: 0.74 - 0.3 * (x % 2 == 0 and y % 2 == 0)
        else:
            value = flat(0.6)
        p.ramp(rect, BONE, value, grain=0.04, mottle=0.1)
    for face, rect in rects['stud'].items():
        p.ramp(rect, BONE, flat(0.95 if face == 'up' else 0.7), grain=0.0, mottle=0.0)
    north = rects['pad']['north']
    return p, (min(north[0], SHEET - 16), min(north[1], SHEET - 16))


def animations():
    def curl(lag, amount):
        return {'rotation': sampled(IDLE, 0.4, lambda t: [0, 0, amount * wave(t, IDLE, lag)])}

    def curl_x(lag, amount):
        return {'rotation': sampled(IDLE, 0.4, lambda t: [amount * wave(t, IDLE, lag), 0, 0])}

    idle = {
        'tip_wa': curl(0.0, 5),
        'tip_wb': curl(IDLE * 0.2, 5),
        'tip_ea': curl(IDLE * 0.4, -5),
        'tip_eb': curl(IDLE * 0.6, -5),
        'tip_t': curl_x(IDLE * 0.8, 5),
        'pad': {'scale': sampled(IDLE, 0.4, lambda t: [1 + 0.012 * wave(t, IDLE), 1, 1 + 0.012 * wave(t, IDLE)])},
    }
    return {'idle': (IDLE, True, idle)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('shaping_palm', BONES, SHEET, painter, animations(), particle, bounds=(2, 1, (0, 0.5, 0)))
    print('shaping_palm: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
