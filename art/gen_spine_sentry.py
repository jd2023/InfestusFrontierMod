"""Spine Sentry (BLOCK_CATALOG T2-30): a rooted lash. A muscle mound on a clawed root foot, its flanks sloping up and
four tendons straining up to a bone collar, carries a bone socket from which a jointed spine rises, three plated
segments, each barbed on its back, and a black thorn, coiled upright like a cobra. Idle it sways;
working it swings wide from side to side, whipping down at what stands near, once a pulse.

    python3 art/gen_spine_sentry.py

The spine has no aim: the sweep swings a hundred degrees each way each working cycle while the segments lash out and
back, so from most sides the organ is seen striking. Front is north (-Z).

Rotation about x in the file: positive tips a part's top toward the front (north), as art/geo_audit.py poses it.
"""
from organ_model import Painter, ball, faces, flat, islands, pack, sampled, skin, squeeze, wave, write

SHEET = 64
IDLE, WORKING = 4.0, 1.0
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
VEIN = (112, 34, 66)
BONE = [(118, 104, 78), (168, 152, 118), (206, 192, 156), (236, 226, 198)]
SINEW = [(132, 82, 86), (176, 128, 124), (212, 176, 162), (236, 214, 198)]
THORN = [(14, 10, 10), (40, 30, 28), (72, 56, 50)]


# GeckoLib mirrors the file's x and negates rotations about x (art/geo_audit.py): these turn a part's top outward
SX, SZ = 1, -1
TENDON = 39


def lean(axis, side, deg):
    """Rotation tipping a part on the side `side` (-1 or 1) of `axis` outward at the top by `deg` (inward if negative);
    a bar lying outward from its pivot dips its outer end."""
    return [SZ * side * deg, 0, 0] if axis == 'z' else [0, 0, SX * side * deg]


def flank(axis, side):
    """A flank plate sunk a pixel into the core, hinged on its outer foot and leaning in, so the mound slopes up from a
    wide foot to the collar."""
    lo = -6 if side < 0 else 3
    origin, size = ([lo, 1.5, -3.5], [3, 4, 7]) if axis == 'x' else ([-3.5, 1.5, lo], [7, 4, 3])
    pivot = [side * 6, 1.5, 0] if axis == 'x' else [0, 1.5, side * 6]
    return ('flank_' + axis, origin, size, (lean(axis, side, -15), pivot))


def tendon(axis, side):
    """A tendon lying on the mound from the collar down to its shoulder: a sinew 2 px wide, hinged at the foot of its
    inner end in the collar and dipping at TENDON degrees, its top face just clear of the core's top edge (r 4, y 6.25)
    and the flank's top (r 5, y 5.4) and its underside sunk into the mound, so it rides the surface its whole length,
    neither buried nor bridging."""
    lo = -5.5 if side < 0 else 2.5
    origin, size = ([lo, 6.3, -1], [3, 2, 2]) if axis == 'x' else ([-1, 6.3, lo], [2, 2, 3])
    pivot = [side * 2.5, 6.3, 0] if axis == 'x' else [0, 6.3, side * 2.5]
    return ('tendon_' + axis, origin, size, (lean(axis, side, TENDON), pivot))


def corner(sx, sz):
    """A pillar of muscle filling a corner between two flanks, turned nearly corner-out (not quite, so no face lies level
    with the root claws'), so the mound reads round."""
    cx, cz = sx * 4.25, sz * 4.25
    return ('haunch', [cx - 1.5, 1.25, cz - 1.5], [3, 4, 3], ([0, 40 * sx * sz, 0], [cx, 3.25, cz]))


def claw(sx, sz):
    cx, cz = sx * 4, sz * 4
    return ('rootlet', [cx - 1, 0.5, cz - 2], [2, 1, 4], ([0, 45 * sx * sz, 0], [cx, 1, cz]))


BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('slab', [-6, 0, -6], [12, 1, 12], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
    ]),
    # the mound: a muscle dome, a core with four flank plates tilted in at the top and a haunch filling each corner,
    # four tendons lying on it from a bone collar round the socket down over its shoulders
    ('mound', 'root', [0, 1, 0], [
        ('core', [-4, 1.25, -4], [8, 5, 8], None),
        flank('x', -1), flank('x', 1), flank('z', -1), flank('z', 1),
        corner(1, 1), corner(1, -1), corner(-1, 1), corner(-1, -1),
        tendon('x', -1), tendon('x', 1), tendon('z', -1), tendon('z', 1),
        ('socket', [-2.5, 6, -2.5], [5, 2, 5], None),
        ('collar', [-3, 6.75, -3], [6, 1, 1], None),
        ('collar', [-3, 6.75, 2], [6, 1, 1], None),
        ('collar_x', [-3, 7.1, -2], [1, 1, 4], None),
        ('collar_x', [2, 7.1, -2], [1, 1, 4], None),
    ]),
    # the spine: the sweep bone turns the whole lash about the socket; each segment hangs from the end of the last,
    # half a pixel into it, a size smaller, so no faces meet
    ('sweep', 'mound', [0, 7.5, 0], []),
    # each segment carries a black barb on its back (the outside of the coil), raked up toward the tip
    ('seg_a', 'sweep', [0, 7.5, 0], [('seg_a', [-2, 7, -2], [4, 5, 4], None),
                                     ('barb', [-0.5, 9.5, 1.5], [1, 1, 3], ([40, 0, 0], [0, 10, 1.5]))]),
    ('seg_b', 'seg_a', [0, 12, 0], [('seg_b', [-1.5, 11.5, -1.5], [3, 5, 3], None),
                                    ('barb', [-0.5, 14, 1], [1, 1, 3], ([40, 0, 0], [0, 14.5, 1]))]),
    ('seg_c', 'seg_b', [0, 16.5, 0], [('seg_c', [-1, 16, -1], [2, 5, 2], None),
                                      ('barb_tip', [-0.5, 18.5, 0.5], [1, 1, 2], ([40, 0, 0], [0, 19, 0.5]))]),
    ('thorn', 'seg_c', [0, 21, 0], [('thorn', [-0.5, 20.75, -0.5], [1, 4, 1], None)]),
]


def textures(uv):
    p = Painter(SHEET, 431)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.6 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(4):
                p.vein(rect, ROOT[0], 5)
    for face, rect in rects['rootlet'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.7 if face == 'up' else 0.45) - 0.15 * (x % 3 == 1))
    for name in ('core', 'flank_x', 'flank_z', 'haunch'):
        for face, rect in rects[name].items():
            # muscle: taut skin lit from above in soft blotches, veined, darker low on the sides
            p.ramp(rect, FLESH, lambda x, y, w, h: 0.45 * skin(x, y, w, h) + 0.4 * ball(x, y, w, h) - (0.12 * y / max(1, h - 1) if face not in ('up', 'down') else 0), grain=0.04, mottle=0.35)
            if face not in ('up', 'down'):
                p.vein(rect, VEIN, 4, 170)
    for name in ('tendon_x', 'tendon_z'):
        for face, rect in rects[name].items():
            # sinew: pale and glossy, striped along its length, darker on its flanks
            stripe = (lambda x, y: y % 2) if name == 'tendon_x' else (lambda x, y: x % 2)
            p.ramp(rect, SINEW, lambda x, y, w, h: (0.85 - 0.3 * stripe(x, y)) if face == 'up' else 0.35, grain=0.03, mottle=0.08)
    for name in ('socket', 'collar', 'collar_x'):
        for face, rect in rects[name].items():
            p.ramp(rect, BONE, lambda x, y, w, h: (0.75 if face == 'up' else 0.5) - 0.3 * (name == 'socket' and face == 'up' and 0 < x < w - 1 and 0 < y < h - 1), grain=0.04, mottle=0.1)
    for name in ('seg_a', 'seg_b', 'seg_c'):
        for face, rect in rects[name].items():
            # bone plates ringed by a dark seam at every joint, flesh showing between the rings
            p.ramp(rect, BONE, lambda x, y, w, h: 0.78 if face == 'up' else 0.2 if y in (0, h - 1) else 0.62 - 0.1 * (x % 2),
                   grain=0.04, mottle=0.1)
    for name in ('barb', 'barb_tip'):
        for face, rect in rects[name].items():
            p.ramp(rect, THORN, lambda x, y, w, h: 0.7 if face == 'up' else 0.55 - 0.4 * x / max(1, w - 1), grain=0.02, mottle=0.05)
    for face, rect in rects['thorn'].items():
        p.ramp(rect, THORN, lambda x, y, w, h: 0.75 if face == 'up' else 0.6 - 0.5 * y / max(1, h - 1), grain=0.02, mottle=0.05)
    return p, rects['core']['north']


# the coil at rest: the spine leans back and curls over, cobra fashion; the lash: it flings forward and down
COIL = {'seg_a': -14, 'seg_b': 42, 'seg_c': 58, 'thorn': 30}
LASH = {'seg_a': 48, 'seg_b': 54, 'seg_c': 42, 'thorn': 24}


def animations():
    idle = {
        'sweep': {'rotation': sampled(IDLE, 0.1, lambda t: [0, 25 * wave(t, IDLE), 0])},
        'seg_a': {'rotation': sampled(IDLE, 0.1, lambda t: [COIL['seg_a'] + 3 * wave(t, IDLE, 0.3), 0, 0])},
        'seg_b': {'rotation': sampled(IDLE, 0.1, lambda t: [COIL['seg_b'] + 4 * wave(t, IDLE, 0.6), 0, 0])},
        'seg_c': {'rotation': sampled(IDLE, 0.1, lambda t: [COIL['seg_c'] + 5 * wave(t, IDLE, 0.9), 0, 0])},
        'thorn': {'rotation': sampled(IDLE, 0.1, lambda t: [COIL['thorn'] + 6 * wave(t, IDLE, 1.2), 0, 0])},
        'mound': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.02 * wave(t, IDLE), 1 - 0.02 * wave(t, IDLE), 1 + 0.02 * wave(t, IDLE)])},
    }

    def lash(part):
        lag = {'seg_a': 0.0, 'seg_b': 0.05, 'seg_c': 0.1, 'thorn': 0.15}[part] * WORKING
        return sampled(WORKING, 0.05, lambda t: [COIL[part] + (LASH[part] - COIL[part]) * squeeze(t, WORKING, lag), 0, 0])

    working = {
        # a wide swing from side to side, a hundred degrees each way, back where it started each cycle
        'sweep': {'rotation': sampled(WORKING, 0.05, lambda t: [0, 100 * wave(t, WORKING), 0])},
        'seg_a': {'rotation': lash('seg_a')},
        'seg_b': {'rotation': lash('seg_b')},
        'seg_c': {'rotation': lash('seg_c')},
        'thorn': {'rotation': lash('thorn')},
        'mound': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.06 * squeeze(t, WORKING), 1 - 0.06 * squeeze(t, WORKING), 1 + 0.06 * squeeze(t, WORKING)])},
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('spine_sentry', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(3, 2, (0, 0.5, 0)))
    print('spine_sentry: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
