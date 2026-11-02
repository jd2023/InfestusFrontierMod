"""Mineral Gizzard (BLOCK_CATALOG T2-23): a grinding muscle. A squat grey-plum belly on a root foot, wrapped in two
courses of ridged muscle, carries a thick-lipped hopper on top with two bone millstones in it, and a low bone chute at
the front where the ground dust drifts out over the foot. The two millstones in their lipped hopper and the drift of
dust at the chute are the cues: what goes in and what comes out. The Compost Gland beside it is a tall bloated sac; this
one lies wide and low.

    python3 art/gen_mineral_gizzard.py

Idle: the belly breathes, the ridges swell in turn and the millstones rock a few degrees. Working: the millstones grind
against each other (swinging 50 degrees to opposite sides), the ridges clench in a wave round the belly and the drift of
dust builds and slumps. The stones are 3x3 and swing inside a 9x5.5 opening, so no corner reaches a lip. Front is north (-Z).
GeckoLib mirrors the file's x and negates rotations about x and y (art/geo_audit.py).
"""
from organ_model import Painter, ball, faces, flat, islands, pack, sampled, skin, squeeze, wave, write

SHEET = 64
IDLE, WORKING = 8.0, 3.6
# grey-plum in the shadows, stone grey on the lit side: muscle that has been grinding rock
BELLY = [(46, 36, 48), (74, 58, 68), (102, 82, 90), (130, 108, 112), (156, 132, 128), (182, 158, 148)]
RIDGE = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
LIP = [(70, 40, 54), (104, 62, 74), (138, 88, 94), (170, 118, 116), (198, 148, 136)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
STONE = [(96, 86, 74), (146, 134, 112), (190, 178, 150), (222, 212, 186), (244, 238, 218)]
GRIT = [(54, 48, 50), (104, 96, 94), (156, 148, 142), (204, 196, 184), (232, 226, 214)]
FLECK = [(214, 150, 70), (232, 200, 84), (206, 112, 66), (214, 218, 224)]
MOUTH = [(30, 18, 26), (52, 30, 38), (84, 50, 54)]
VESSEL = (122, 34, 44)


def claw(sx, sz):
    cx, cz = sx * 5, sz * 4.5
    return ('rootlet', [cx - 1, 0.5, cz - 2], [2, 1, 4], ([0, 45 * sx * sz, 0], [cx, 1, cz]))


def ridges(y):
    """One course of ridge plates round the belly; the front's low course is left to the chute. Each plate sinks a pixel
    into the belly so no face lies in the belly's."""
    cubes = [
        ('ridge_ns', [-5, y, 3.75], [10, 2, 2], None),
        ('ridge_ew', [-7.5, y, -4], [2, 2, 8], None),
        ('ridge_ew', [5.5, y, -4], [2, 2, 8], None),
    ]
    if y > 4:
        cubes.append(('ridge_ns', [-5, y, -5.75], [10, 2, 2], None))
    return cubes


BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('slab', [-6.5, 0, -6], [13, 2, 12], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
    ]),
    ('core', 'root', [0, 1.5, 0], [('belly', [-7, 1.5, -5], [14, 6, 10], None)]),
    ('ridge_low', 'core', [0, 3.5, 0], ridges(2.5)),
    ('ridge_high', 'core', [0, 6, 0], ridges(5)),
    # the hopper: four thick lips sunk into the belly's top, the corners left open
    ('hopper', 'core', [0, 7.5, 0], [
        ('lip_ns', [-4.5, 7, -4.75], [9, 3, 2], None),
        ('lip_ns', [-4.5, 7, 2.75], [9, 3, 2], None),
        ('lip_ew', [-6.5, 7, -2.75], [2, 3, 6], None),
        ('lip_ew', [4.5, 7, -2.75], [2, 3, 6], None),
    ]),
    # two bone millstones in the opening, a quarter pixel above the belly's top; they turn about their own centres
    ('stone_w', 'hopper', [-2.3, 8.75, 0], [('stone', [-3.8, 7.75, -1.5], [3, 2, 3], None)]),
    ('stone_e', 'hopper', [2.3, 8.75, 0], [('stone', [0.8, 7.75, -1.5], [3, 2, 3], None)]),
    # the chute: a bone throat low at the front, sunk into the belly, its dark mouth to the north
    ('chute', 'core', [0, 3, -5], [('chute', [-2.5, 2, -7], [5, 2, 3], None)]),
    # the dust it has drifted over the foot's front edge
    ('drift', 'root', [0, 0.25, -6.75], [('drift', [-2, 0.25, -7.75], [4, 1, 2], None)]),
]


def textures(uv):
    p = Painter(SHEET, 611)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.6 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(4):
                p.vein(rect, ROOT[0], 5)
    for face, rect in rects['rootlet'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.7 if face == 'up' else 0.45) - 0.15 * (x % 3 == 1))
    for face, rect in rects['belly'].items():
        # taut grey muscle: one soft light from the upper left, large blotches, a few red vessels
        p.ramp(rect, BELLY, lambda x, y, w, h: 0.3 * skin(x, y, w, h) + 0.55 * ball(x, y, w, h) + 0.1, grain=0.03, mottle=0.4)
        if face not in ('up', 'down'):
            for _ in range(2):
                p.vein(rect, VESSEL, 4, 170)
    for name in ('ridge_ns', 'ridge_ew'):
        for face, rect in rects[name].items():
            # a corded band: a bright crest with a dark groove either side, running along the plate's length
            along_x = name == 'ridge_ns' and face in ('north', 'south', 'up', 'down')
            p.ramp(rect, RIDGE, lambda x, y, w, h: skin(x, y, w, h) + 0.14 * ((x if not along_x else y) % 3 == 1), mottle=0.25)
    for name in ('lip_ns', 'lip_ew'):
        for face, rect in rects[name].items():
            p.ramp(rect, LIP, lambda x, y, w, h: skin(x, y, w, h) + (0.2 if face == 'up' else 0), mottle=0.25)
    for face, rect in rects['stone'].items():
        # an ivory millstone: the top is cut in parallel grooves, the sides are smooth
        groove = (lambda x, y, w, h: 0.78 - 0.4 * (x % 2 == 0)) if face == 'up' else flat(0.6)
        p.ramp(rect, STONE, groove, grain=0.05, mottle=0.12)
    for face, rect in rects['chute'].items():
        if face == 'north':
            p.ramp(rect, STONE, lambda x, y, w, h: 0.7 - 0.12 * (y == 0), grain=0.05, mottle=0.1)
            for y in range(1, rect[3]):
                for x in range(1, rect[2] - 1):
                    p.put(rect, x, y, MOUTH[0 if y > 0 and x in range(2, rect[2] - 2) else 1])
        else:
            p.ramp(rect, STONE, lambda x, y, w, h: 0.55 + 0.25 * ball(x, y, w, h), grain=0.05, mottle=0.1)
    for face, rect in rects['drift'].items():
        p.ramp(rect, GRIT, lambda x, y, w, h: (0.65 if face == 'up' else 0.35) + 0.15 * ((x * 5 + y * 3) % 4 == 0), grain=0.1, mottle=0.3)
        for _ in range(5):
            p.put(rect, p.rng.randrange(rect[2]), p.rng.randrange(rect[3]), p.rng.choice(FLECK))
    return p, rects['belly']['north']


def animations():
    def breathe(lag, amount):
        return {'scale': sampled(IDLE, 0.2, lambda t: [1 + amount * max(0.0, wave(t, IDLE, lag)), 1, 1 + amount * max(0.0, wave(t, IDLE, lag))])}

    def rock(sign):
        return {'rotation': sampled(IDLE, 0.4, lambda t: [0, sign * 6 * wave(t, IDLE), 0])}

    idle = {
        'core': {'scale': sampled(IDLE, 0.2, lambda t: [1 + 0.01 * wave(t, IDLE), 1, 1 + 0.01 * wave(t, IDLE)])},
        'ridge_low': breathe(0.0, 0.03),
        'ridge_high': breathe(IDLE / 2, 0.03),
        'stone_w': rock(1),
        'stone_e': rock(-1),
        'drift': {'scale': sampled(IDLE, 0.4, lambda t: [1, 0.6, 1])},
    }

    def turn(sign):
        """The stones grind against each other: 50 degrees to one side and back, mirrored between them."""
        return {'rotation': sampled(WORKING, 0.1, lambda t: [0, sign * 50 * wave(t, WORKING), 0])}

    def clench(lag, out):
        return {'scale': sampled(WORKING, 0.1, lambda t: [1 + out * squeeze(t, WORKING, lag), 1, 1 + out * squeeze(t, WORKING, lag)])}

    def pile(t):
        """Builds along the loop and slumps in its last fifth."""
        phase = t / WORKING
        return 0.6 + 0.8 * phase / 0.8 if phase < 0.8 else 1.4 - 0.8 * (phase - 0.8) / 0.2

    working = {
        'core': clench(0.0, -0.03),
        'ridge_low': clench(0.1 * WORKING, 0.05),
        'ridge_high': clench(0.3 * WORKING, 0.05),
        'hopper': {'scale': sampled(WORKING, 0.1, lambda t: [1 - 0.03 * squeeze(t, WORKING, 0.4 * WORKING), 1, 1 - 0.03 * squeeze(t, WORKING, 0.4 * WORKING)])},
        'stone_w': turn(1),
        'stone_e': turn(-1),
        'chute': {'scale': sampled(WORKING, 0.1, lambda t: [1, 1 + 0.08 * squeeze(t, WORKING, 0.5 * WORKING), 1])},
        'drift': {'scale': sampled(WORKING, 0.1, lambda t: [1, pile(t), 1])},
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('mineral_gizzard', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('mineral_gizzard: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
