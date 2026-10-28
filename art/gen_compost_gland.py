"""Compost Gland (BLOCK_CATALOG T1-19): a fermentation gut. A bloated belly of sickly olive-plum flesh, its flanks
bulging out over a clawed root foot, carries a thick-lipped maw on top with scraps mounded in it; low at the
front a bone spout has spilt a crust of bone meal over the foot's edge. The gaping maw of scraps above and the
meal-crusted spout below are the cues: what goes in and what comes out.

    python3 art/gen_compost_gland.py

The belly is a core with eight flank plates, the lower ones tilted out at the top and the upper ones tilted in,
so the body swells at its waist with no ledges. Idle: the belly breathes and its flanks swell and sink in turn.
Working: the belly churns in a wave from the core out, the lips clench, the spout presses and a crumb of meal
grows out of it, drops and fades. Front is north (-Z).
"""
from organ_model import Painter, ball, faces, flat, islands, pack, sampled, skin, squeeze, wave, write

SHEET = 64
IDLE, WORKING = 9.0, 4.8
# plum in the shadows, olive on the lit side: a gut gone sour, still of the mod's flesh
GUT = [(58, 28, 38), (92, 46, 50), (124, 72, 56), (150, 102, 64), (176, 134, 80), (198, 160, 104)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
SCRAPS = [(36, 44, 20), (66, 78, 32), (100, 114, 48), (146, 150, 74), (178, 176, 98)]
BONE = [(118, 104, 78), (168, 152, 118), (206, 192, 156), (236, 226, 198)]
MEAL = [(190, 182, 160), (222, 216, 198), (244, 240, 226), (255, 254, 246)]
LIP = [(96, 52, 56), (130, 74, 74), (160, 98, 90), (188, 128, 112), (206, 150, 130)]
VESSEL = (122, 34, 44)
TILT = 8


def claw(sx, sz):
    cx, cz = sx * 4.5, sz * 4.5
    return ('rootlet', [cx - 1, 0.5, cz - 2], [2, 1, 4], ([0, 45 * sx * sz, 0], [cx, 1, cz]))


def flank(side):
    """The two plates of one flank: lower tilts out at the top, upper tilts in, each hung on the core's side."""
    if side in ('w', 'e'):
        x = -6.5 if side == 'w' else 4.5
        sign = -1 if side == 'w' else 1
        return (f'flank_{side}', 'core', [sign * 5, 5.75, 0], [
            ('plate_side', [x, 2, -4], [2, 4, 8], ([0, 0, -sign * TILT], [sign * 4.5, 2, 0])),
            ('plate_side_up', [x, 5.5, -3.5], [2, 4, 7], ([0, 0, sign * TILT], [sign * 4.5, 9.5, 0])),
        ])
    z = -6.5 if side == 'n' else 4.5
    sign = -1 if side == 'n' else 1
    cubes = [('plate_fb_up', [-3.5, 5.5, z], [7, 4, 2], ([-sign * TILT, 0, 0], [0, 9.5, sign * 4.5]))]
    if side == 'n':
        # the front's lower plate is split so the spout shows between its halves
        cubes += [('plate_fb_half', [-4.5, 2, z], [3, 4, 2], ([sign * TILT, 0, 0], [0, 2, sign * 4.5])),
                  ('plate_fb_half', [1.5, 2, z], [3, 4, 2], ([sign * TILT, 0, 0], [0, 2, sign * 4.5]))]
    else:
        cubes.append(('plate_fb', [-4, 2, z], [8, 4, 2], ([sign * TILT, 0, 0], [0, 2, sign * 4.5])))
    return (f'flank_{side}', 'core', [0, 5.75, sign * 5], cubes)


BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('slab', [-6, 0, -5], [12, 2, 10], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
        # bone meal the spout has spilt, crusted over the foot's front edge under the spout
        ('crust', [-1, 1.25, -7.25], [2, 1, 2], None),
    ]),
    ('core', 'root', [0, 1.5, 0], [('core', [-5, 1.5, -4.5], [10, 8, 9], None)]),
    flank('w'), flank('e'), flank('n'), flank('s'),
    # the maw: four thick lips round a throat of scraps, sunk into the core's top, the corners left open
    ('maw', 'core', [0, 9.5, 0], [
        ('lip_ns', [-3, 9.25, -4.75], [6, 2, 2], None),
        ('lip_ns', [-3, 9.25, 2.75], [6, 2, 2], None),
        ('lip_ew', [-4.25, 9.5, -3], [2, 2, 6], None),
        ('lip_ew', [2.25, 9.5, -3], [2, 2, 6], None),
    ]),
    ('scraps', 'maw', [0, 9.5, 0], [('scraps', [-1.5, 9.9, -1.5], [3, 1, 3], None)]),
    # the spout: a bone teat low at the front between the plate halves, sunk into the core
    ('spout', 'core', [0, 4, -4.5], [('spout', [-1, 3, -6], [2, 2, 2], None)]),
    # the crumb of meal: wide and flat, a quarter pixel off the spout's tip
    ('puff', 'spout', [0, 4, -6.75], [('puff', [-2, 3, -7.25], [4, 2, 1], None)]),
]


def textures(uv):
    p = Painter(SHEET, 433)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.6 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(4):
                p.vein(rect, ROOT[0], 5)
    for face, rect in rects['rootlet'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.7 if face == 'up' else 0.45) - 0.15 * (x % 3 == 1))
    for name in ('core', 'plate_side', 'plate_side_up', 'plate_fb', 'plate_fb_up', 'plate_fb_half'):
        for face, rect in rects[name].items():
            # taut sour flesh: one soft light from the upper left, large blotches, no creases, red vessels
            p.ramp(rect, GUT, lambda x, y, w, h: 0.3 * skin(x, y, w, h) + 0.55 * ball(x, y, w, h) + 0.1, grain=0.03, mottle=0.4)
            if face not in ('up', 'down'):
                for _ in range(2):
                    p.vein(rect, VESSEL, 4, 170)
    for name in ('lip_ns', 'lip_ew'):
        for face, rect in rects[name].items():
            p.ramp(rect, LIP, lambda x, y, w, h: skin(x, y, w, h) + (0.15 if face == 'up' else 0), mottle=0.25)
    for face, rect in rects['scraps'].items():
        p.ramp(rect, SCRAPS, lambda x, y, w, h: 0.35 + 0.4 * ((x * 7 + y * 3) % 5 == 0) + 0.2 * ((x + y) % 3 == 0), grain=0.08, mottle=0.2)
    for face, rect in rects['spout'].items():
        p.ramp(rect, BONE, lambda x, y, w, h: 0.55 + 0.3 * ball(x, y, w, h) - 0.4 * (face == 'north' and 0 < x < w - 1 and 0 < y < h - 1), grain=0.05, mottle=0.1)
    for name in ('crust', 'puff'):
        for face, rect in rects[name].items():
            p.ramp(rect, MEAL, lambda x, y, w, h: 0.8 - 0.2 * (face == 'down') - 0.1 * ((x + y) % 2), grain=0.06, mottle=0.1)
    return p, rects['core']['north']


def animations():
    def swell(lag):
        return {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.05 * max(0.0, wave(t, IDLE, lag)), 1, 1 + 0.05 * max(0.0, wave(t, IDLE, lag))])}

    idle = {
        'core': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.01 * wave(t, IDLE), 1, 1 + 0.01 * wave(t, IDLE)])},
        'flank_w': swell(0.0), 'flank_e': swell(IDLE / 2), 'flank_n': swell(IDLE / 4), 'flank_s': swell(3 * IDLE / 4),
        'puff': {'scale': sampled(IDLE, 0.1, lambda t: [0, 0, 0])},
    }

    def churn(lag, out):
        return {'scale': sampled(WORKING, 0.05, lambda t: [1 + out * squeeze(t, WORKING, lag), 1 + 0.03 * squeeze(t, WORKING, lag), 1 + out * squeeze(t, WORKING, lag)])}

    def crumb(t):
        """Grows out of the spout as it presses, drops, fades by shrinking; reset while unseen."""
        phase = (t / WORKING - 0.45) % 1.0
        if phase > 0.4:
            return 0.0
        u = phase / 0.4
        return 1.0 - abs(2 * u - 1)

    working = {
        'core': churn(0.0, -0.04),
        'flank_w': churn(0.15 * WORKING, 0.05), 'flank_e': churn(0.15 * WORKING, 0.05),
        'flank_n': churn(0.3 * WORKING, 0.05), 'flank_s': churn(0.3 * WORKING, 0.05),
        'maw': {'scale': sampled(WORKING, 0.05, lambda t: [1 - 0.06 * squeeze(t, WORKING, 0.35 * WORKING), 1, 1 - 0.06 * squeeze(t, WORKING, 0.35 * WORKING)])},
        'spout': {'scale': sampled(WORKING, 0.05, lambda t: [1, 1, 1 + 0.2 * squeeze(t, WORKING, 0.45 * WORKING)])},
        'puff': {
            'scale': sampled(WORKING, 0.05, lambda t: [crumb(t)] * 3),
            'position': sampled(WORKING, 0.05, lambda t: [0, -1.5 * ((t / WORKING - 0.45) % 1.0), 0]),
        },
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('compost_gland', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('compost_gland: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
