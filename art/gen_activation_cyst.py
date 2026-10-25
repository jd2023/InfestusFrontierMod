"""Activation Cyst: a living alembic. A sinew stalk rises from a root foot to a hub that carries three see-through
chambers: the source chamber on one side (amber), the carrier chamber on the other (green), and above the hub the
mixing bubble where the treatment is cultured. Tubes run from each side chamber up into the mixing bubble. It must
read as something brewing: glowing liquids behind membranes, bubbles rising.

    python3 art/gen_activation_cyst.py

Idle: the side chambers breathe; the mixing bubble holds a low, dim pool. Working: the side chambers squeeze in turn,
the pool in the mixing bubble rises and bubbles rise through it, and from the top come swirls and the brewing
stand's bubbling, with a soft light (CystBrew). The bone names keep their parts: `source` and `carrier` are the side
chambers, `throat` the mixing bubble.
"""
from organ_model import Painter, faces, flat, islands, pack, sampled, squeeze, wave, write

SHEET = 128
IDLE, WORKING = 6.4, 2.4
ROOT = [(26, 22, 26), (42, 36, 40), (62, 54, 56), (86, 76, 74)]
SINEW = [(52, 58, 66), (76, 84, 92), (104, 114, 120), (136, 146, 150), (170, 178, 178)]
IVORY = [(112, 96, 84), (156, 140, 114), (194, 180, 148), (222, 212, 184), (240, 234, 212)]
AMBER = [(110, 60, 10), (170, 100, 18), (224, 150, 36), (248, 196, 84), (255, 232, 160)]
GREEN = [(20, 84, 50), (36, 132, 70), (72, 186, 96), (140, 226, 140), (210, 250, 200)]
BREW = [(20, 90, 100), (30, 140, 150), (60, 196, 196), (140, 236, 226), (220, 255, 248)]
MEMBRANE = (220, 232, 230)


def claw(sx, sz):
    cx, cz = sx * 5.5, sz * 5.5
    return ('claw', [cx - 1, 0.5, cz - 2.5], [2, 2, 5], ([0, 45 * sx * sz, 0], [cx, 1.5, cz]))


BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('slab', [-6, 0, -6], [12, 1, 12], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
    ]),
    # the stalk and the hub; the arms reach out to the side chambers
    ('body', 'root', [0, 1, 0], [
        ('stalk', [-1.5, 1, -1.5], [3, 6, 3], None),
        ('hub', [-3, 7, -3], [6, 2, 6], None),
        ('collar', [-2.5, 9, -2.5], [5, 1, 5], None),
        ('arm', [-6, 7.5, -1], [3, 1, 2], None),
        ('arm', [3, 7.5, -1], [3, 1, 2], None),
        ('tube_up', [-6.5, 8.5, -0.5], [1, 3, 1], None),
        ('tube_up', [5.5, 8.5, -0.5], [1, 3, 1], None),
        ('tube_in', [-5.5, 10.5, -0.5], [2, 1, 1], None),
        ('tube_in', [3.5, 10.5, -0.5], [2, 1, 1], None),
    ]),
    # the side chambers hang from the arms: a membrane round a pool of liquid
    ('source', 'body', [-6, 7.5, 0], [
        ('chamber', [-8, 2.5, -2.5], [4, 5, 5], None),
        ('amber', [-7.5, 3, -2], [3, 3, 4], None),
    ]),
    ('carrier', 'body', [6, 7.5, 0], [
        ('chamber', [4, 2.5, -2.5], [4, 5, 5], None),
        ('green', [4.5, 3, -2], [3, 3, 4], None),
    ]),
    # the mixing bubble on the collar, its pool and the bubbles rising in it
    ('throat', 'body', [0, 10, 0], [('bubble', [-3, 10, -3], [6, 4, 6], None)]),
    ('brew', 'throat', [0, 10.5, 0], [('brew', [-2.5, 10.5, -2.5], [5, 2, 5], None)]),
    ('rise', 'throat', [0, 11, 0], [
        ('bead', [-1.5, 11, -0.5], [1, 1, 1], None),
        ('bead', [0.5, 11, 0.5], [1, 1, 1], None),
    ]),
    ('lid', 'throat', [0, 14, 0], [('lid', [-1.5, 13.5, -1.5], [3, 1, 3], None)]),
]


def ramp(colours, t):
    t = max(0.0, min(1.0, t)) * (len(colours) - 1)
    i = min(int(t), len(colours) - 2)
    f = t - i
    return tuple(int(round(a + (b - a) * f)) for a, b in zip(colours[i], colours[i + 1]))


def textures(uv):
    p = Painter(SHEET, 131)
    sizes = {n: s for n, s in islands(BONES).items()}
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.55 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(5):
                p.vein(rect, ROOT[0], 5)
    for face, rect in rects['claw'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.75 if face == 'up' else 0.55 - 0.2 * y / max(1, h - 1))
               - 0.14 * (x % 3 == 1))
    for name in ('stalk', 'arm', 'tube_up', 'tube_in'):
        for face, rect in rects[name].items():
            # sinew: grey-blue cords running along the part, a darker groove between them
            p.ramp(rect, SINEW, lambda x, y, w, h: 0.62 - (0.22 if x % 2 else 0) - 0.15 * y / max(1, h - 1),
                   grain=0.05, mottle=0.15)
    for name in ('hub', 'collar'):
        for face, rect in rects[name].items():
            p.ramp(rect, IVORY, lambda x, y, w, h: 0.7 - 0.3 * y / max(1, h - 1) - 0.1 * (x in (0, w - 1)),
                   grain=0.05, mottle=0.12)
    for name in ('chamber', 'bubble'):
        for face, rect in rects[name].items():
            # a clear membrane, faint in the middle and thicker at its rim, with a wet highlight
            x0, y0, w, h = rect
            for y in range(h):
                for x in range(w):
                    rim = x in (0, w - 1) or y in (0, h - 1)
                    p.put(rect, x, y, MEMBRANE, alpha=120 if rim else 30)
            if face not in ('up', 'down'):
                p.put(rect, 1, 1, (255, 255, 255), alpha=190)
    for name, colours in (('amber', AMBER), ('green', GREEN), ('brew', BREW)):
        for face, rect in rects[name].items():
            x0, y0, w, h = rect
            for y in range(h):
                for x in range(w):
                    t = 0.45 + 0.3 * (y / max(1, h - 1)) + 0.08 * ((x + 2 * y) % 3 == 0)
                    if face == 'up':
                        t = 0.78 + 0.08 * ((x + y) % 2)
                    p.put(rect, x, y, ramp(colours, t + p.rng.uniform(-0.04, 0.04)), glow=200)
    for face, rect in rects['bead'].items():
        p.ramp(rect, BREW, flat(0.95), grain=0.02, mottle=0.05)
        p.glow_rect(rect, 255)
    for face, rect in rects['lid'].items():
        p.ramp(rect, IVORY, flat(0.5 if face == 'up' else 0.35), grain=0.04, mottle=0.1)
    return p, rects['stalk']['north']


def animations():
    still_zero = sampled(1.0, 1.0, lambda t: [0, 0, 0])
    idle = {
        'source': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.03 * wave(t, IDLE), 1 + 0.03 * wave(t, IDLE), 1 + 0.03 * wave(t, IDLE)])},
        'carrier': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.03 * wave(t, IDLE, IDLE / 2), 1 + 0.03 * wave(t, IDLE, IDLE / 2), 1 + 0.03 * wave(t, IDLE, IDLE / 2)])},
        'brew': {'scale': sampled(1.0, 1.0, lambda t: [1, 0.4, 1])},
        'rise': {'scale': still_zero},
    }
    working = {
        'source': {'scale': sampled(WORKING, 0.05, lambda t: [1 - 0.12 * squeeze(t, WORKING), 1 - 0.06 * squeeze(t, WORKING), 1 - 0.12 * squeeze(t, WORKING)])},
        'carrier': {'scale': sampled(WORKING, 0.05, lambda t: [1 - 0.12 * squeeze(t, WORKING, WORKING / 2), 1 - 0.06 * squeeze(t, WORKING, WORKING / 2), 1 - 0.12 * squeeze(t, WORKING, WORKING / 2)])},
        'brew': {'scale': sampled(WORKING, 0.05, lambda t: [1, 0.9 + 0.1 * wave(t, WORKING / 2), 1])},
        # two beads rise through the pool and vanish at the top, then start again from the bottom unseen
        'rise': {
            'position': sampled(WORKING, 0.05, lambda t: [0, 2.0 * ((t % (WORKING / 2)) / (WORKING / 2)), 0]),
            'scale': sampled(WORKING, 0.05, lambda t: [0 if (t % (WORKING / 2)) / (WORKING / 2) > 0.9 else 1] * 3),
        },
        'throat': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.03 * wave(t, WORKING / 2), 1, 1 + 0.03 * wave(t, WORKING / 2)])},
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('activation_cyst', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.5, 0)))
    print('activation_cyst: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
