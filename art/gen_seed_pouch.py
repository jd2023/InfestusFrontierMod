"""Seed Pouch (BLOCK_CATALOG T0-09): a seed sac. A plump pod of veined leathery skin, widest at its waist and
tapering to a pinched neck, squats on a clawed root foot; its front splits into two lobes that part a little as it
breathes; a membrane cord pinches the neck and the mouth flares open in four lips round a crowned heap of seeds,
two more spilt over its shoulders. The heaped mouth of seeds and the split lobes are the cues: grown, not potted.

    python3 art/gen_seed_pouch.py

The sac is a core with flank plates: each lower plate is sunk a pixel into the core and hinged on its outer foot,
leaning out, and each upper plate is hinged on the top outer edge of the one below, leaning in toward the neck, so it
swells and tapers with no ledges (an art test pins the waist). Idle: the sac breathes and the lobes part and close in turn. The
pouch does no visible work. Front is north (-Z).
"""
import math

from organ_model import Painter, ball, faces, flat, folds, islands, pack, sampled, skin, wave, write

SHEET = 64
IDLE = 8.0
LEATHER = [(62, 36, 30), (96, 58, 42), (132, 84, 58), (166, 114, 78), (194, 146, 104), (214, 172, 132)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
CORD = [(120, 60, 72), (150, 82, 92), (178, 100, 112), (204, 130, 134)]
LIP = [(110, 62, 56), (142, 86, 70), (172, 116, 90), (198, 148, 116)]
SEED = [(96, 110, 40), (140, 150, 60), (182, 186, 90), (220, 214, 130)]
VEIN = (104, 40, 56)
OUT, IN = 10, 35
# GeckoLib mirrors the file's x and negates rotations about x (art/geo_audit.py): these turn a plate's top outward
SX, SZ = 1, -1
LOW, UP = 4, 4  # plate heights


def lean(axis, side, deg):
    """Rotation tipping a plate on the side `side` (-1 or 1) of `axis` outward at the top by `deg` (inward if negative)."""
    return [SZ * side * deg, 0, 0] if axis == 'z' else [0, 0, SX * side * deg]


def plates(axis, side, width, upper=True):
    """A flank of the sac on one side: a lower plate sunk a pixel into the core and hinged on its outer foot, leaning
    out, and an upper plate hinged on the lower one's top outer edge, leaning in toward the neck, so the flank swells
    at the waist and tapers with no ledge between them."""
    a = math.radians(OUT)
    top_out, top_y = 6 + LOW * math.sin(a), 1.75 + LOW * math.cos(a)

    def box(out, y, thick, w, h):
        lo = side * out if side < 0 else side * out - thick
        return ([lo, y, -w / 2], [thick, h, w]) if axis == 'x' else ([-w / 2, y, lo], [w, h, thick])

    def at(out, y):
        return [side * out, y, 0] if axis == 'x' else [0, y, side * out]

    low = box(6, 1.75, 3, width, LOW)
    out = [('plate_low_' + axis + ('' if upper else '_front'), low[0], low[1], (lean(axis, side, OUT), at(6, 1.75)))]
    if upper:
        up = box(top_out, top_y, 2, width - 1, UP)
        out.append(('plate_up_' + axis, up[0], up[1], (lean(axis, side, -IN), at(top_out, top_y))))
    return out


def lobe(sx):
    """One half of the front's upper plate: hinged like the other upper plates, split from its twin at the seam."""
    a = math.radians(OUT)
    top_out, top_y = 6 + LOW * math.sin(a), 1.75 + LOW * math.cos(a)
    x0 = 0.25 if sx > 0 else -3.25
    return ('lobe', [x0, top_y, -top_out], [3, UP, 2], (lean('z', -1, -IN), [0, top_y, -top_out]))


def claw(sx, sz):
    cx, cz = sx * 4.5, sz * 4.5
    return ('rootlet', [cx - 1, 0.5, cz - 2], [2, 1, 4], ([0, 45 * sx * sz, 0], [cx, 1, cz]))


BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('slab', [-6, 0, -4.5], [12, 2, 9], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
    ]),
    # the sac: a core and its flanks, each a lower plate leaning out and an upper one leaning in to the neck; the
    # front's lower plate is narrower than the lobes above it, so a lobe swinging out never grazes its side
    ('sack', 'root', [0, 1.5, 0], [('core', [-4, 1.5, -4], [8, 8, 8], None)]
     + plates('x', -1, 7) + plates('x', 1, 7) + plates('z', 1, 7) + plates('z', -1, 6, upper=False)),
    # the front's upper plate is two lobes; each parts from the seam about its top
    ('lobe_w', 'sack', [-1.75, 9.25, -4.5], [lobe(-1)]),
    ('lobe_e', 'sack', [1.75, 9.25, -4.5], [lobe(1)]),
    # the neck, pinched by a thin cord sunk round it
    ('neck', 'sack', [0, 9, 0], [
        ('neck', [-2, 9, -2], [4, 2, 4], None),
        ('cord', [-3, 9.65, -3], [6, 1, 6], None),
        ('spilt', [-4.25, 9.5, 3.25], [1, 1, 1], ([0, 30, 0], [-3.75, 10, 3.75])),
        ('spilt', [3.25, 9.5, -4.25], [1, 1, 1], ([0, 30, 0], [3.75, 10, -3.75])),
    ]),
    # the rim: four lips sunk into the neck's top and flared outward, the seeds heaped to a crown inside
    ('rim', 'neck', [0, 11, 0], [
        ('lip_ns', [-2, 10.75, -2.75], [4, 2, 1], (lean('z', -1, 20), [0, 10.75, -1.75])),
        ('lip_ns', [-2, 10.75, 1.75], [4, 2, 1], (lean('z', 1, 20), [0, 10.75, 1.75])),
        ('lip_ew', [-2.75, 11, -2], [1, 2, 4], (lean('x', -1, 20), [-1.75, 11, 0])),
        ('lip_ew', [1.75, 11, -2], [1, 2, 4], (lean('x', 1, 20), [1.75, 11, 0])),
        ('heap', [-1.5, 10.75, -1.5], [3, 2, 3], None),
        ('crown', [-1, 12.6, -1], [2, 1, 2], ([0, 45, 0], [0, 13.1, 0])),
        ('spilt', [-0.5, 13.45, -0.5], [1, 1, 1], ([0, 30, 0], [0, 13.95, 0])),
    ]),
]


def textures(uv):
    p = Painter(SHEET, 401)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.6 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(4):
                p.vein(rect, ROOT[0], 5)
    for face, rect in rects['rootlet'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.7 if face == 'up' else 0.45) - 0.15 * (x % 3 == 1))
    for name in ('core', 'plate_low_x', 'plate_low_z', 'plate_low_z_front', 'plate_up_x', 'plate_up_z', 'lobe'):
        for face, rect in rects[name].items():
            # leathery skin lit from above in soft blotches, faintly creased, with plum veins
            p.ramp(rect, LEATHER, lambda x, y, w, h: 0.4 * skin(x, y, w, h) + 0.45 * ball(x, y, w, h) + 0.2 * folds(x, y), grain=0.04, mottle=0.32)
            if face not in ('up', 'down'):
                p.vein(rect, VEIN, 5, 150)
    for face, rect in rects['neck'].items():
        p.ramp(rect, LEATHER, lambda x, y, w, h: skin(x, y, w, h) - 0.15, mottle=0.2)
    for face, rect in rects['cord'].items():
        p.ramp(rect, CORD, lambda x, y, w, h: (0.35 if face == 'up' else 0.6) - 0.25 * (x % 2 == 0 and face not in ('up', 'down')), grain=0.05, mottle=0.1)
    for name in ('lip_ns', 'lip_ew'):
        for face, rect in rects[name].items():
            p.ramp(rect, LIP, lambda x, y, w, h: skin(x, y, w, h) + (0.15 if face == 'up' else 0), mottle=0.25)
    for name in ('heap', 'crown', 'spilt'):
        for face, rect in rects[name].items():
            p.ramp(rect, SEED, lambda x, y, w, h: 0.4 + 0.45 * ((x * 7 + y * 3) % 5 == 0) + 0.2 * ball(x, y, w, h), grain=0.06, mottle=0.15)
    return p, rects['core']['north']


def animations():
    idle = {
        'sack': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.012 * wave(t, IDLE), 1 - 0.008 * wave(t, IDLE), 1 + 0.012 * wave(t, IDLE)])},
        # each lobe swings its foot out from the seam about its top, in turn
        'lobe_w': {'rotation': sampled(IDLE, 0.1, lambda t: [0, 0, SX * 8 * max(0.0, wave(t, IDLE))])},
        'lobe_e': {'rotation': sampled(IDLE, 0.1, lambda t: [0, 0, -SX * 8 * max(0.0, wave(t, IDLE, IDLE / 2))])},
    }
    return {'idle': (IDLE, True, idle)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('seed_pouch', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('seed_pouch: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
