"""Spine Sentry (BLOCK_CATALOG T2-30): a rooted lash. A muscular flesh mound on a root foot carries a bone socket from
which a jointed spine rises, three plated segments and a black thorn, coiled upright like a cobra. Idle it sways;
working it sweeps round the block, whipping down at what stands near, once a pulse.

    python3 art/gen_spine_sentry.py

The spine has no aim: the sweep turns a full circle each working cycle while the segments lash out and back, so from
any side the organ is seen striking. Front is north (-Z).

Rotation about x in the file: positive tips a part's top toward the front (north), as art/geo_audit.py poses it.
"""
from organ_model import Painter, faces, flat, folds, islands, pack, sampled, skin, squeeze, wave, write

SHEET = 64
IDLE, WORKING = 4.0, 1.0
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
VEIN = (112, 34, 66)
BONE = [(118, 104, 78), (168, 152, 118), (206, 192, 156), (236, 226, 198)]
THORN = [(14, 10, 10), (40, 30, 28), (72, 56, 50)]


def claw(sx, sz):
    cx, cz = sx * 4, sz * 4
    return ('rootlet', [cx - 1, 0.5, cz - 2], [2, 1, 4], ([0, 45 * sx * sz, 0], [cx, 1, cz]))


BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('slab', [-6, 0, -6], [12, 1, 12], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
    ]),
    # the mound: a flesh dome in two steps, the socket of bone sunk into its top
    ('mound', 'root', [0, 1, 0], [
        ('mound', [-5, 0.5, -5], [10, 4, 10], None),
        ('dome', [-3.5, 4, -3.5], [7, 2, 7], None),
        ('socket', [-2.5, 5.5, -2.5], [5, 2, 5], None),
    ]),
    # the spine: the sweep bone turns the whole lash about the socket; each segment hangs from the end of the last,
    # half a pixel into it, a size smaller, so no faces meet
    ('sweep', 'mound', [0, 7.5, 0], []),
    ('seg_a', 'sweep', [0, 7.5, 0], [('seg_a', [-2, 7, -2], [4, 5, 4], None)]),
    ('seg_b', 'seg_a', [0, 12, 0], [('seg_b', [-1.5, 11.5, -1.5], [3, 5, 3], None)]),
    ('seg_c', 'seg_b', [0, 16.5, 0], [('seg_c', [-1, 16, -1], [2, 5, 2], None)]),
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
    for name in ('mound', 'dome'):
        for face, rect in rects[name].items():
            # muscle: skin with folds, veined, darker low on the sides
            p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) - (0.15 * y / max(1, h - 1) if face not in ('up', 'down') else 0), mottle=0.3)
            if face not in ('up', 'down'):
                p.vein(rect, VEIN, 4, 170)
    for face, rect in rects['socket'].items():
        p.ramp(rect, BONE, lambda x, y, w, h: 0.75 if face == 'up' else 0.55 - 0.3 * (y == h - 1), grain=0.04, mottle=0.1)
    for name in ('seg_a', 'seg_b', 'seg_c'):
        for face, rect in rects[name].items():
            # bone plates ringed by a dark seam at every joint, flesh showing between the rings
            p.ramp(rect, BONE, lambda x, y, w, h: 0.78 if face == 'up' else 0.2 if y in (0, h - 1) else 0.62 - 0.1 * (x % 2),
                   grain=0.04, mottle=0.1)
    for face, rect in rects['thorn'].items():
        p.ramp(rect, THORN, lambda x, y, w, h: 0.75 if face == 'up' else 0.6 - 0.5 * y / max(1, h - 1), grain=0.02, mottle=0.05)
    return p, rects['mound']['north']


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
        # a full turn a cycle; the rotation ends at 360, where the loop closes
        'sweep': {'rotation': sampled(WORKING, 0.05, lambda t: [0, 360 * (t / WORKING), 0])},
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
