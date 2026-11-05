"""Washing Kidney (BLOCK_CATALOG T2-24): a bean of two lobes that rinses what the Gizzard left behind. Two soft blue-grey
lobes stand side by side on a root foot, each a bean (its belly swelling outward, its ends drawn in towards the middle,
a puff of muscle turned corner-out over its flank), parted by a narrow gap. A tall pale clean-water sac crowns the left
lobe and a squat murky dirty-water sac the right; a duct leads from each sac to the bone sieve that stands in the gap
between the lobes, a plate pitted all through so the light shows in its holes. At the front a bone tap drips onto a damp
patch of the foot. The clean sac, the dirty sac and the sieve between them are the cues: what is rinsed with, what it
leaves and what it passes through. The Mineral Gizzard beside it is a wide grinding mill; this one is two soft lobes
under two sacs.

    python3 art/gen_washing_kidney.py

Idle: the lobes breathe in turn, the sacs swell out of step and a drop hangs and shrinks at the tap. Working: the lobes
squeeze in a wave without parting from the foot, the sieve shivers between them, the dirty sac swells while the clean
one slackens, and a drop forms at the tap, falls to the patch and splashes it, twice a loop. Front is north (-Z). GeckoLib
mirrors the file's x and negates rotations about x and y (art/geo_audit.py). Nothing reaches past 7.95 pixels sideways
and nothing stands higher than 11.3.
"""
from organ_model import Painter, ball, faces, flat, islands, pack, sampled, skin, squeeze, wave, write

SHEET = 64
IDLE, WORKING = 8.0, 3.6
DROPS = 2  # drops per working loop
LOBE = [(38, 46, 62), (62, 76, 94), (88, 106, 124), (116, 136, 150), (144, 164, 174), (174, 190, 196)]
CLEAN = [(48, 92, 120), (72, 130, 160), (104, 168, 192), (146, 204, 218), (196, 232, 238), (238, 252, 252)]
DIRTY = [(30, 28, 20), (54, 48, 30), (84, 74, 44), (114, 100, 58), (142, 126, 74), (170, 152, 96)]
BONE = [(96, 86, 74), (146, 134, 112), (190, 178, 150), (222, 212, 186), (244, 238, 218)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
DAMP = [(40, 52, 66), (62, 82, 98), (88, 114, 128), (120, 148, 156)]
DUCT = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98)]
GRIT = [(58, 50, 34), (128, 114, 78), (176, 162, 114)]
VESSEL = (122, 34, 44)
MOUTH = [(30, 18, 26), (52, 30, 38)]


def claw(sx, sz):
    cx, cz = sx * 5, sz * 4.5
    return ('rootlet', [cx - 1, 0.5, cz - 2], [2, 1, 4], ([0, 45 * sx * sz, 0], [cx, 1, cz]))


def lobe(s):
    """One bean on side s (-1 left, 1 right): a belly swelling outward, the two ends drawn in towards the middle, and a
    puff turned corner-out over the flank so the belly reads rounded. The puff's planes stay off the belly's."""
    cx = 4.5 * s
    return [
        ('belly', [cx - 2.5, 1.5, -3], [5, 5, 6], None),
        ('end', [3.5 * s - 2, 1.5, -5], [4, 4, 2], None),
        ('end', [3.5 * s - 2, 1.5, 3], [4, 4, 2], None),
        ('puff', [cx - 2, 2, -2], [4, 4, 4], ([0, 45, 0], [cx, 4, 0])),
    ]


BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('slab', [-6.5, 0, -6], [13, 2, 12], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
    ]),
    ('lobe_l', 'root', [-4.5, 1.5, 0], lobe(-1)),
    ('lobe_r', 'root', [4.5, 1.5, 0], lobe(1)),
    # the clean sac: tall, a bulb of water under a small crown, a duct to the sieve; sunk a quarter pixel into its lobe
    ('clean', 'lobe_l', [-4.5, 6.25, 0], [
        ('c_body', [-6.5, 6.25, -2], [4, 4, 4], None),
        ('c_puff', [-6.5, 6.75, -2], [4, 4, 4], ([0, 45, 0], [-4.5, 8.75, 0])),
        ('c_crown', [-5.5, 10.25, -1], [2, 1, 2], None),
        ('duct', [-3.5, 6.5, -0.5], [3, 1, 1], None),
    ]),
    # the dirty sac: squat and wide, sagging over its lobe, a heavy lip and a duct from the sieve
    ('dirty', 'lobe_r', [4.5, 6.25, 0], [
        ('d_body', [1.75, 6.25, -2.5], [5, 3, 5], None),
        ('d_puff', [1.75, 6.75, -2.5], [5, 3, 5], ([0, 45, 0], [4.25, 8.25, 0])),
        ('d_crown', [3.25, 9.25, -1.5], [3, 1, 3], None),
        ('duct', [0.5, 6.5, -0.5], [3, 1, 1], None),
    ]),
    # the sieve: a bone plate in the gap between the lobes, pitted through; it shivers about its foot
    ('sieve', 'root', [0, 1.75, 0], [('plate', [-1, 1.75, -3.5], [2, 7, 7], None)]),
    # the tap: a bone stem at the front of the gap and a thin lip out over the foot's edge
    ('spout', 'root', [0, 1.75, -4.5], [
        ('stem', [-1, 1.75, -5.5], [2, 4, 2], None),
        ('lip', [-0.5, 4.5, -7], [1, 1, 3], None),
    ]),
    # one drop under the lip; it falls to the patch and is gone
    ('drop', 'root', [0, 3.75, -6.65], [('drop', [-0.5, 3.25, -7.15], [1, 1, 1], None)]),
    ('drip', 'root', [0, 0.25, -6.75], [('damp', [-2, 0.25, -7.75], [4, 1, 2], None)]),
]
DROP_FALL = 2.0  # pixels from the lip down to the patch


def textures(uv):
    p = Painter(SHEET, 733)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.6 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(4):
                p.vein(rect, ROOT[0], 5)
    for face, rect in rects['rootlet'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.7 if face == 'up' else 0.45) - 0.15 * (x % 3 == 1))
    for name in ('belly', 'end', 'puff'):
        for face, rect in rects[name].items():
            # taut blue-grey muscle: one soft light from the upper left, a wet sheen on the crowns, a few red vessels
            p.ramp(rect, LOBE, lambda x, y, w, h: 0.3 * skin(x, y, w, h) + 0.55 * ball(x, y, w, h) + 0.1 + (0.12 if face == 'up' else 0), grain=0.03, mottle=0.4)
            if face not in ('up', 'down') and name != 'puff':
                p.vein(rect, VESSEL, 4, 120)
            if face == 'up':
                for _ in range(2):
                    p.put(rect, p.rng.randrange(rect[2]), p.rng.randrange(rect[3]), LOBE[5])
    for name in ('c_body', 'c_puff', 'c_crown'):
        for face, rect in rects[name].items():
            # a bulb of clean water: bright crown, glints on the lit side, a deeper blue belly
            p.ramp(rect, CLEAN, lambda x, y, w, h: 0.25 * skin(x, y, w, h) + 0.6 * ball(x, y, w, h) + 0.25, grain=0.03, mottle=0.25)
            for _ in range(2 if face not in ('up', 'down') else 1):
                p.put(rect, p.rng.randrange(max(1, rect[2] // 2)), p.rng.randrange(max(1, rect[3] // 2)), CLEAN[5])
    for name in ('d_body', 'd_puff', 'd_crown'):
        for face, rect in rects[name].items():
            # murky water: sediment settles in the lower rows, grit hangs in it, the crown is a dull skin
            low = lambda x, y, w, h: -0.22 * (y / max(1, h - 1)) ** 2
            p.ramp(rect, DIRTY, lambda x, y, w, h: 0.2 * skin(x, y, w, h) + 0.5 * ball(x, y, w, h) + 0.12 + low(x, y, w, h), grain=0.08, mottle=0.4)
            for _ in range(3 if face not in ('up', 'down') else 2):
                p.put(rect, p.rng.randrange(rect[2]), p.rng.randrange(rect[3]), p.rng.choice(GRIT))
    for face, rect in rects['duct'].items():
        p.ramp(rect, DUCT, lambda x, y, w, h: 0.35 + 0.4 * ball(x, y, w, h), grain=0.04, mottle=0.2)
    for face, rect in rects['plate'].items():
        # an ivory sieve: pitted through on both flanks, solid at its rim and on its edges
        p.ramp(rect, BONE, lambda x, y, w, h: 0.74 + 0.15 * ball(x, y, w, h) - 0.12 * (y == h - 1), grain=0.05, mottle=0.14)
        if face in ('east', 'west'):
            _, _, w, h = rect
            for x in range(1, w - 1, 2):
                for y in range(1, h - 2, 2):
                    # a hole in a grid, symmetric about the plate's middle so a west hole lies behind its east one
                    p.img.putpixel((rect[0] + x, rect[1] + y), (0, 0, 0, 0))
    for face, rect in rects['stem'].items():
        p.ramp(rect, BONE, lambda x, y, w, h: 0.55 + 0.3 * ball(x, y, w, h) - 0.12 * (y == 0), grain=0.05, mottle=0.1)
    for face, rect in rects['lip'].items():
        p.ramp(rect, BONE, lambda x, y, w, h: 0.6 + 0.2 * ball(x, y, w, h), grain=0.05, mottle=0.1)
        if face == 'north':
            p.put(rect, 0, 0, MOUTH[0])
    for face, rect in rects['drop'].items():
        p.ramp(rect, CLEAN, flat(0.8), grain=0.02, mottle=0.05)
        p.put(rect, 0, 0, CLEAN[5])
    for face, rect in rects['damp'].items():
        p.ramp(rect, DAMP, lambda x, y, w, h: (0.65 if face == 'up' else 0.35) + 0.15 * ((x * 5 + y * 3) % 4 == 0), grain=0.1, mottle=0.3)
        if face == 'up':
            p.put(rect, 1, 0, CLEAN[4])
            p.put(rect, 2, 1, CLEAN[4])
    return p, rects['belly']['north']


def animations():
    def breathe(lag, amount):
        return {'scale': sampled(IDLE, 0.2, lambda t: [1 + amount * max(0.0, wave(t, IDLE, lag)), 1, 1 + amount * max(0.0, wave(t, IDLE, lag))])}

    def swell(lag, amount):
        return {'scale': sampled(IDLE, 0.4, lambda t: [1 + 0.6 * amount * wave(t, IDLE, lag), 1 + amount * wave(t, IDLE, lag), 1 + 0.6 * amount * wave(t, IDLE, lag)])}

    def hang(t):
        """A drop swells at the lip and shrinks back, never falling."""
        return 0.45 + 0.3 * wave(t, IDLE, -IDLE / 4)

    idle = {
        'lobe_l': breathe(0.0, 0.02),
        'lobe_r': breathe(IDLE / 2, 0.02),
        'clean': swell(0.0, 0.03),
        'dirty': swell(IDLE / 2, 0.03),
        'sieve': {'rotation': sampled(IDLE, 0.4, lambda t: [0, 0, 1.2 * wave(t, IDLE)])},
        'drop': {'scale': sampled(IDLE, 0.4, lambda t: [hang(t)] * 3)},
        'drip': {'scale': sampled(IDLE, 0.4, lambda t: [1, 0.6, 1])},
    }

    def clench(lag, out):
        return {'scale': sampled(WORKING, 0.1, lambda t: [1 + out * squeeze(t, WORKING, lag), 1, 1 + out * squeeze(t, WORKING, lag)])}

    def cycle(t):
        """Phase of the drop's loop, 0..1, DROPS times along the clip."""
        return (t / WORKING * DROPS) % 1.0

    def size(t):
        """Forms at the lip over the first half, hangs, then falls and is gone before the loop restarts."""
        phase = cycle(t)
        if phase < 0.5:
            return 0.1 + 0.9 * phase / 0.5
        if phase < 0.85:
            return 1.0
        return max(0.02, 1.0 - (phase - 0.85) / 0.1)

    def fall(t):
        phase = cycle(t)
        if phase < 0.5:
            return 0.0
        if phase < 0.9:
            k = max(0.0, (phase - 0.6) / 0.3)
            return -DROP_FALL * k * k
        return 0.0 if phase >= 0.95 else -DROP_FALL * (0.95 - phase) / 0.05

    def splash(t):
        """The patch rises a little as a drop lands and slackens."""
        phase = cycle(t)
        return 1 + 0.2 * max(0.0, 1 - abs(phase - 0.88) / 0.12)

    working = {
        'lobe_l': clench(0.0, -0.05),
        'lobe_r': clench(0.4 * WORKING, -0.05),
        'clean': {'scale': sampled(WORKING, 0.1, lambda t: [1, 1 - 0.1 * squeeze(t, WORKING), 1])},
        'dirty': {'scale': sampled(WORKING, 0.1, lambda t: [1 + 0.05 * squeeze(t, WORKING, 0.4 * WORKING), 1 + 0.12 * squeeze(t, WORKING, 0.4 * WORKING), 1 + 0.05 * squeeze(t, WORKING, 0.4 * WORKING)])},
        'sieve': {'rotation': sampled(WORKING, 0.05, lambda t: [0, 0, 4 * wave(t, WORKING / 3)])},
        'spout': {'scale': sampled(WORKING, 0.1, lambda t: [1, 1 + 0.06 * squeeze(t, WORKING, 0.5 * WORKING), 1])},
        'drop': {'scale': sampled(WORKING, 0.05, lambda t: [size(t)] * 3),
                 'position': sampled(WORKING, 0.05, lambda t: [0, fall(t), 0])},
        'drip': {'scale': sampled(WORKING, 0.05, lambda t: [splash(t), 1, splash(t)])},
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('washing_kidney', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('washing_kidney: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
