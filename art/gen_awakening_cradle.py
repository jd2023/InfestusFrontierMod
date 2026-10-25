"""Awakening Cradle: a violet flower of four petals on a rooted bowl, cradling a pale silk cocoon, the piece being
awakened. It must read as something being brought to life: the cocoon glows, and while it works the petals close
into a bud around it and it beats like a heart.

    python3 art/gen_awakening_cradle.py

Idle: the petals stand open and breathe; the cocoon glows faintly. Working: the petals fold in into a bud, a bright
pulse beats through the cocoon (the `pulse` shell, scaled to nothing at idle), the block gives light 7, specks of
the culture are drawn into it and it beats (CradleGlow). Bone spurs stand between the petals.
"""
from organ_model import Painter, ball, faces, flat, islands, pack, sampled, wave, write

SHEET = 128
IDLE, WORKING = 8.0, 1.6
ROOT = [(30, 18, 26), (48, 28, 40), (70, 44, 58), (96, 64, 80)]
PETAL = [(40, 14, 44), (62, 24, 66), (88, 38, 92), (116, 56, 118), (146, 80, 144), (176, 108, 170)]
INNER = [(110, 56, 116), (146, 86, 150), (182, 124, 184), (210, 162, 210), (232, 200, 232)]
SILK = [(140, 112, 164), (180, 156, 204), (214, 196, 232), (238, 228, 248), (252, 248, 255)]
HEART = [(170, 90, 230), (206, 140, 250), (236, 196, 255), (252, 236, 255)]
IVORY = [(112, 96, 84), (156, 140, 114), (194, 180, 148), (222, 212, 184), (240, 234, 212)]
VEIN = (60, 20, 70)


def petal(name, side):
    """A petal hinged on the bowl's top near its rim: a broad blade narrowing to a point, one pixel thick."""
    pivot = {'n': [0, 4, -4], 's': [0, 4, 4], 'w': [-4, 4, 0], 'e': [4, 4, 0]}[side]
    if side in 'ns':
        z = -4.5 if side == 'n' else 3.5
        cubes = [('blade_x', [-3, 4, z], [6, 5, 1], None), ('tip_x', [-2, 9, z], [4, 2, 1], None),
                 ('point_x', [-1, 11, z], [2, 1, 1], None)]
    else:
        x = -4.5 if side == 'w' else 3.5
        cubes = [('blade_z', [x, 4, -3], [1, 5, 6], None), ('tip_z', [x, 9, -2], [1, 2, 4], None),
                 ('point_z', [x, 11, -1], [1, 1, 2], None)]
    return (name, 'root', pivot, cubes)


BONES = [
    ('root', None, [0, 0, 0], []),
    ('cup', 'root', [0, 0, 0], [
        ('foot', [-7, 0, -7], [14, 1, 14], None),
        ('bowl', [-5, 1, -5], [10, 3, 10], None),
        ('spur', [-5.25, 3, -5.25], [1, 3, 1], None),
        ('spur', [4.25, 3, -5.25], [1, 3, 1], None),
        ('spur', [-5.25, 3, 4.25], [1, 3, 1], None),
        ('spur', [4.25, 3, 4.25], [1, 3, 1], None),
    ]),
    petal('petal_n', 'n'), petal('petal_s', 's'), petal('petal_w', 'w'), petal('petal_e', 'e'),
    ('cocoon', 'root', [0, 4, 0], [
        ('cocoon', [-2, 4, -2], [4, 5, 4], None),
        ('cocoon_cap', [-1.5, 9, -1.5], [3, 1, 3], None),
    ]),
    ('pulse', 'cocoon', [0, 6.5, 0], [('pulse', [-2.5, 4.5, -2.5], [5, 4, 5], None)]),
]
# outward lean of each petal at rest, as (axis, sign): rotating by +sign degrees about the axis opens it, in the
# game's space (GeckoLib mirrors x, so the west and east signs differ from the file's)
OPEN = {'petal_n': (0, 1), 'petal_s': (0, -1), 'petal_w': (2, -1), 'petal_e': (2, 1)}


def textures(uv):
    p = Painter(SHEET, 97)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['foot'].items():
        p.ramp(rect, ROOT, flat(0.55 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(6):
                p.vein(rect, ROOT[0], 6)
    for face, rect in rects['bowl'].items():
        if face == 'up':
            p.ramp(rect, INNER, lambda x, y, w, h: 0.25 + 0.3 * ball(x, y, w, h), mottle=0.2)
        else:
            p.ramp(rect, PETAL, lambda x, y, w, h: 0.5 - 0.25 * y / max(1, h - 1), grain=0.06, mottle=0.25)
    for face, rect in rects['spur'].items():
        p.ramp(rect, IVORY, lambda x, y, w, h: 0.75 - 0.4 * y / max(1, h - 1), grain=0.04, mottle=0.1)
    # petals: dark plum outside, pale lilac inside with glowing veins; which face is inside depends on the side
    inside = {n + a: f for a, f in (('_x', ('south', 'north')), ('_z', ('east', 'west'))) for n in ('blade', 'tip', 'point')}
    for name in inside:
        for face, rect in rects[name].items():
            x0, y0, w, h = rect
            if face in inside[name]:
                # both broad faces are painted alike, the inner colours on both: a petal's back shows only from
                # outside, where its darker rim and veins still read
                p.ramp(rect, INNER, lambda x, y, w, h: 0.35 + 0.4 * (y / max(1, h - 1)) - 0.12 * (x in (0, w - 1)),
                       grain=0.05, mottle=0.2)
                mid = w // 2
                for y in range(h):
                    p.put(rect, mid, y, HEART[0], glow=150)  # the midrib glows faintly
                for y in range(1, h, 2):
                    p.put(rect, mid - 1 - (y % 4 == 1), y, PETAL[3])
                    p.put(rect, mid + 1 + (y % 4 == 3), y, PETAL[3])
            else:
                p.ramp(rect, PETAL, lambda x, y, w, h: 0.55 - 0.2 * y / max(1, h - 1), grain=0.05, mottle=0.2)
    for name in ('cocoon', 'cocoon_cap'):
        for face, rect in rects[name].items():
            # silk wound round and round: pale bands, faintly glowing from the life inside
            x0, y0, w, h = rect
            for y in range(h):
                for x in range(w):
                    band = 0.55 + 0.25 * ((y + (x + y) // 3) % 2) + p.rng.uniform(-0.05, 0.05)
                    p.put(rect, x, y, ramp(SILK, band), glow=110)
    for face, rect in rects['pulse'].items():
        # the heartbeat: bright strands across a see-through shell
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                if (x + 2 * y) % 3 == 0 or y == h // 2:
                    p.put(rect, x, y, ramp(HEART, 0.4 + 0.5 * (y == h // 2)), alpha=200, glow=255)
    return p, rects['bowl']['north']


def ramp(colours, t):
    t = max(0.0, min(1.0, t)) * (len(colours) - 1)
    i = min(int(t), len(colours) - 2)
    f = t - i
    return tuple(int(round(a + (b - a) * f)) for a, b in zip(colours[i], colours[i + 1]))


def beat(t, length):
    """A heartbeat over one loop: a strong beat, a softer one just after, then rest."""
    t = (t % length) / length
    def bump(c, width):
        d = (t - c) / width
        return max(0.0, 1 - d * d)
    return max(bump(0.1, 0.08), 0.6 * bump(0.28, 0.07))


def lean(axis, sign, degrees):
    v = [0, 0, 0]
    v[axis] = sign * degrees
    return v


def animations():
    idle = {
        bone: {'rotation': sampled(IDLE, 0.1, lambda t, a=a, s=s: lean(a, s, 20 + 3 * wave(t, IDLE)))}
        for bone, (a, s) in OPEN.items()
    }
    idle['pulse'] = {'scale': sampled(1.0, 1.0, lambda t: [0, 0, 0])}
    idle['cocoon'] = {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.02 * wave(t, IDLE), 1, 1 + 0.02 * wave(t, IDLE)])}
    working = {
        bone: {'rotation': sampled(WORKING, 0.05, lambda t, a=a, s=s: lean(a, s, -6 + 2 * beat(t, WORKING)))}
        for bone, (a, s) in OPEN.items()
    }
    working['pulse'] = {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.12 * beat(t, WORKING)] * 3)}
    working['cocoon'] = {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.05 * beat(t, WORKING), 1, 1 + 0.05 * beat(t, WORKING)])}
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('awakening_cradle', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.5, 0)))
    print('awakening_cradle: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
