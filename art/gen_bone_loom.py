"""Bone Loom (BLOCK_CATALOG T0-06): a ribcage loom. A muscle mound on a root foot carries a short vertebral spine at
its back; three curved ribs rise from each flank through the edges of a bone plate woven in three strips between them
and curl in over it, an open cage, with a marrow shuttle and its bone needle riding the plate. The curved ribs and the plate being woven
are the cues; the Membrane Rack beside it is two straight posts and a skin.

    python3 art/gen_bone_loom.py

Idle: the ribs breathe apart and together, the shuttle rests where the weaving stopped. Working: the ribs clench inward in a
wave from front to back, the plate thickens under the shuttle, and the shuttle crosses the plate and comes back.
Front is north (-Z). Rotation about z in the file: positive tips a part's top toward west (-X) as the game shows it (GeckoLib mirrors x),
checked with art/geo_audit.py.
"""
from organ_model import Painter, ball, bone_along, faces, flat, folds, islands, pack, sampled, skin, squeeze, wave, write

SHEET = 64
IDLE, WORKING = 6.0, 2.4
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
IVORY = [(112, 96, 84), (156, 140, 114), (194, 180, 148), (222, 212, 184), (240, 234, 212), (250, 248, 236)]
BONE = [(118, 104, 78), (168, 152, 118), (206, 192, 156), (236, 226, 198)]
PLATE = [(206, 180, 168), (228, 214, 196), (244, 238, 222), (252, 250, 240)]
MARROW = [(96, 22, 30), (150, 38, 44), (204, 70, 62), (236, 120, 96)]
VEIN = (112, 34, 66)

# the ribs stand at three depths; each is a lower segment leaning out and an upper one curving in over the plate
RIB_Z = (-3.5, 0, 3.5)
LEAN, CURL = 10, 30


def claw(sx, sz):
    cx, cz = sx * 5, sz * 5
    return ('rootlet', [cx - 1, 0.5, cz - 2], [2, 1, 4], ([0, 45 * sx * sz, 0], [cx, 1, cz]))


def rib(side, index):
    """One rib on flank `side` (-1 west, +1 east) at depth RIB_Z[index]: a bone pivoting where it leaves the mound.
    The east ribs stand half a pixel behind their west partners, so no two ribs share a plane where their tips meet."""
    x = side * 5.5
    z = RIB_Z[index] + (0.5 if side > 0 else 0)
    lower = ('rib_low', [x - 1, 4, z - 0.5], [2, 6, 1], ([0, 0, side * LEAN], [x, 4, z]))
    # the upper segment hangs from the lower's outward-leaning tip, half a pixel into it and a quarter pixel back so no
    # face is shared, thinner, and curls in over the plate's edge without reaching the shuttle's path
    top_x = x + side * 6 * 0.1736  # sin 10 degrees
    top_y = 4 + 6 * 0.9848
    upper = ('rib_high', [top_x - 0.5, top_y - 0.5, z - 0.25], [1, 5, 1], ([0, 0, -side * CURL], [top_x, top_y - 0.5, z]))
    return (f'rib_{"w" if side < 0 else "e"}{index}', 'mound', [x, 4, z], [lower, upper])


BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('slab', [-7, 0, -7], [14, 2, 14], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
    ]),
    # the mound: a muscle dome sunk into the foot, two haunches of flesh where the ribs root
    ('mound', 'root', [0, 1.5, 0], [
        ('mound', [-6, 1.5, -5], [12, 4, 10], None),
        ('haunch', [-7, 3, -4.25], [3, 3, 8], ([0, 0, -20], [-5.5, 3, 0])),
        ('haunch', [4, 3, -4.25], [3, 3, 8], ([0, 0, 20], [5.5, 3, 0])),
    ]),
    # the spine: three vertebrae stacked at the back, each half a pixel into the last, alternating in girth
    ('spine', 'mound', [0, 5, 4.75], [
        ('vertebra', [-2, 4.5, 3.25], [4, 3, 3], None),
        ('disc', [-1.5, 7, 3.75], [3, 3, 2], None),
        ('vertebra', [-2, 9.5, 3.25], [4, 3, 3], None),
    ]),
    rib(-1, 0), rib(-1, 1), rib(-1, 2),
    rib(1, 0), rib(1, 1), rib(1, 2),
    # the plate, woven from the front: a full strip, a half-woven one and one just begun at the back, the outer two
    # tilted to follow the arch of the ribs, which pass through the plate's edges
    ('plate', 'mound', [0, 11, 0], [
        ('strip_full', [-6, 10, -5.5], [12, 1, 3], ([-14, 0, 0], [0, 10.5, -4])),
        ('strip_half', [-6, 10.5, -1.5], [8, 1, 3], None),
        ('strip_begun', [-6, 10, 2.5], [4, 1, 3], ([14, 0, 0], [0, 10.5, 4])),
    ]),
    # the shuttle: a marrow slug with a bone needle laid across it, riding a quarter pixel over the middle strip
    ('shuttle', 'plate', [0.5, 11.9, 0], [
        ('slug', [-1, 11.9, -1.5], [3, 1, 3], None),
        ('needle', [0, 12.4, -2.5], [1, 1, 5], None),
    ]),
]


def textures(uv):
    p = Painter(SHEET, 83)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.6 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(4):
                p.vein(rect, ROOT[0], 5)
    for face, rect in rects['rootlet'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.7 if face == 'up' else 0.45) - 0.15 * (x % 3 == 1))
    for name in ('mound', 'haunch'):
        for face, rect in rects[name].items():
            p.ramp(rect, FLESH, lambda x, y, w, h: skin(x, y, w, h) + folds(x, y) - (0.12 * y / max(1, h - 1) if face not in ('up', 'down') else 0), mottle=0.3)
            if face != 'down':
                p.vein(rect, VEIN, 4, 170)
    for name in ('vertebra', 'disc'):
        for face, rect in rects[name].items():
            p.ramp(rect, BONE, lambda x, y, w, h: 0.5 + 0.25 * ball(x, y, w, h) - 0.15 * (name == 'disc'), grain=0.06, mottle=0.12)
    for name in ('rib_low', 'rib_high'):
        for face, rect in rects[name].items():
            p.ramp(rect, BONE, lambda x, y, w, h: bone_along(True)(x, y, w, h) - 0.08 * (face == 'down'), grain=0.06, mottle=0.1)
    for name in ('strip_full', 'strip_half', 'strip_begun'):
        for face, rect in rects[name].items():
            # the plate: smooth, woven in faint cross lines, its edges still raw and the newest strip pinkest
            raw = {'strip_full': 0.0, 'strip_half': 0.22, 'strip_begun': 0.45}[name]
            p.ramp(rect, PLATE, lambda x, y, w, h: (0.7 if face == 'up' else 0.5) - raw - 0.12 * (x % 3 == 0 and face in ('up', 'down')) - 0.3 * (x in (0, w - 1) and face in ('up', 'down')), grain=0.03, mottle=0.1, alpha=240)
    for face, rect in rects['slug'].items():
        p.ramp(rect, MARROW, lambda x, y, w, h: 0.4 + 0.3 * ball(x, y, w, h), grain=0.04, mottle=0.1)
        p.glow_rect(rect, 110)
    for face, rect in rects['needle'].items():
        p.ramp(rect, IVORY, lambda x, y, w, h: 0.8 - 0.25 * (face == 'down'), grain=0.02, mottle=0.05)
    return p, rects['mound']['north']


def animations():
    def breathe(side, index, length, amount):
        lag = index * 0.15 * length
        return {'rotation': sampled(length, 0.1, lambda t: [0, 0, side * amount * wave(t, length, lag)])}

    def clench(side, index):
        lag = index * 0.12 * WORKING
        return {'rotation': sampled(WORKING, 0.05, lambda t: [0, 0, -side * 5 * squeeze(t, WORKING, lag)])}

    idle = {'shuttle': {'position': sampled(IDLE, 0.1, lambda t: [0, 0.1 * wave(t, IDLE), 0])}}
    working = {
        'plate': {'scale': sampled(WORKING, 0.05, lambda t: [1, 1 + 0.3 * squeeze(t, WORKING, 0.5 * WORKING), 1])},
        # along the half-woven strip and back: two pixels over the first half, home over the second
        'shuttle': {'position': sampled(WORKING, 0.05, lambda t: [-2 * (1 - abs(2 * t / WORKING - 1)), 0.2 * squeeze(t, WORKING), 0])},
    }
    for side in (-1, 1):
        for index in range(len(RIB_Z)):
            name = f'rib_{"w" if side < 0 else "e"}{index}'
            idle[name] = breathe(side, index, IDLE, 2)
            working[name] = clench(side, index)
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('bone_loom', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 2, (0, 0.5, 0)))
    print('bone_loom: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
