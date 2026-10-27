"""Planting Proboscis: a seed weevil. A bronze beetle on a root foot, six legs splayed to the slab, in three segments:
grooved wing covers over the abdomen, a dark thorax, and a head with two bulging red eyes from which a long jointed
rostrum reaches forward and down over the bed in front. A see-through sac on the wing covers holds the seeds it will
plant.

    python3 art/gen_planting_proboscis.py

It takes stock from its slot or from any neighbour, so its sides stay plain and low: a Seed Pouch or a mouth sits
against any face without hiding the sac. The organ has no facing and is never turned: the rostrum always points
north (-Z), over the bed cell in front, and reaches half a block into that cell, so nothing should stand there but
the bed and its crop. The seeds sit inside the see-through sac, as the Bladder's fill sits inside its membrane, and
the legs' feet stand half a pixel in the slab so they never float; those are the only cubes inside another bone's.

Idle: the rostrum hovers over the bed and sways; the sac breathes. Working: a seed appears at the tip, the rostrum
swings down and stabs it into the bed, the tip bending last, and lifts back; the seeds in the sac jostle and the sac
squeezes once a stroke.

Rotation about x in the file: positive tips a part's top toward the front (north), as art/geo_audit.py poses it.
"""
from organ_model import Painter, faces, flat, islands, pack, sampled, squeeze, wave, write

SHEET = 64
IDLE, WORKING = 5.0, 2.0
ROOT = [(30, 22, 18), (46, 34, 26), (66, 50, 36), (90, 70, 50)]
CHITIN = [(22, 18, 10), (48, 38, 18), (84, 66, 28), (124, 100, 40), (168, 140, 64), (206, 180, 100)]
JOINT = [(84, 36, 48), (122, 58, 68), (160, 90, 94), (196, 128, 122)]
SAC = (206, 222, 150)
SEED = [(96, 70, 30), (148, 110, 50), (196, 156, 80), (228, 200, 120)]
EYE = (46, 12, 10)
EYE_LIT = (150, 44, 28)
EYE_GLINT = (255, 200, 180)

MOUTH = (0, 5.5, -7)  # where the rostrum grows from the head
LEG_SPLAY = 30  # degrees each leg leans out from the body to the slab


def claw(sx, sz):
    cx, cz = sx * 4, sz * 4
    return ('rootlet', [cx - 1, 0.5, cz - 2], [2, 1, 4], ([0, 45 * sx * sz, 0], [cx, 1, cz]))


def leg(sx, z):
    # hangs from under the body's edge, its top inside the body, and leans out to the slab
    x = sx * 4.5 - 0.5
    return ('leg', [x, 0.5, z], [1, 3, 1], ([0, 0, -LEG_SPLAY * sx], [x + 0.5, 3.5, z + 0.5]))


BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('slab', [-6, 0, -6], [12, 1, 12], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
    ]),
    # the body in three segments, each a pixel inside the one behind it, a little higher, so no faces meet
    ('body', 'root', [0, 1, 0], [
        ('abdomen', [-5, 3, -2], [10, 7, 9], None),
        ('dome', [-4, 10, 0], [8, 1, 6], None),
        ('thorax', [-4, 3.5, -4], [8, 6, 3], None),
        ('head', [-3, 4, -7], [6, 5, 4], None),
        ('eye', [-4, 6, -8], [2, 2, 2], None),
        ('eye', [2, 6, -8], [2, 2, 2], None),
        leg(1, -3), leg(1, 1), leg(1, 5), leg(-1, -3), leg(-1, 1), leg(-1, 5),
    ]),
    # the seed sac on the wing covers, and the seeds inside it
    ('sac', 'body', [0, 10.8, 3], [('sac', [-2.5, 10.8, 0.5], [5, 3, 5], None)]),
    ('seeds', 'sac', [0, 11.3, 3], [
        ('seed', [-1.5, 11.3, 2], [1, 1, 1], None),
        ('seed', [0.5, 11.3, 3.5], [1, 1, 1], None),
        ('seed', [-0.5, 12.1, 2.5], [1, 1, 1], None),
        ('seed', [1, 12, 1.5], [1, 1, 1], None),
    ]),
    # the rostrum from the mouth, straight forward at rest: a fleshy base, a ringed shaft and a black tip, each
    # jointed to the last; the shaft starts inside the base, the tip and the seed each stand a quarter pixel clear of the part before
    ('snout', 'body', list(MOUTH), [('base', [-1, 4.5, -9.5], [2, 2, 3], None)]),
    ('shaft', 'snout', [0, 5.5, -9.25], [('shaft', [-0.5, 5, -13], [1, 1, 4], None)]),
    ('tip', 'shaft', [0, 5.5, -13], [('tip', [-0.5, 5.25, -15.25], [1, 1, 2], None)]),
    ('seed', 'tip', [0, 5.75, -15.25], [('seed', [-0.5, 5.25, -16.5], [1, 1, 1], None)]),
]


def textures(uv):
    p = Painter(SHEET, 409)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.6 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(4):
                p.vein(rect, ROOT[0], 5)
    for face, rect in rects['rootlet'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.7 if face == 'up' else 0.45) - 0.15 * (x % 3 == 1))

    def covers(x, y, w, h, face):
        """Wing covers: a dark seam down the back parts two halves grooved along their length, a sheen on each; the
        sides darken downward and end in a lit rim over the legs."""
        if face == 'up':
            if x in (w // 2 - 1, w // 2):
                return 0.1
            groove = (x - w // 2) % 2 == 0
            sheen = x in (2, w - 3)
            return 0.92 if sheen else 0.52 if groove else 0.72
        if face == 'down':
            return 0.15
        rim = y == h - 2
        return (0.68 - 0.35 * y / max(1, h - 1)) + (0.25 if rim else 0) - (0.25 if y == h - 1 else 0)

    for name in ('abdomen', 'dome'):
        for face, rect in rects[name].items():
            p.ramp(rect, CHITIN, lambda x, y, w, h, face=face: covers(x, y, w, h, face), grain=0.03, mottle=0.08)
    for face, rect in rects['thorax'].items():
        # darker than the covers, with a pale band across the top
        p.ramp(rect, CHITIN, lambda x, y, w, h, face=face: 0.62 if face == 'up' else 0.4 - 0.2 * y / max(1, h - 1),
               grain=0.04, mottle=0.1)
    for face, rect in rects['head'].items():
        # dark and glossy, a pale brow, and a dark mouth on the front the rostrum grows from
        p.ramp(rect, CHITIN, lambda x, y, w, h, face=face: (0.5 if face == 'up' else 0.42 - 0.22 * y / max(1, h - 1))
               + (0.3 if face == 'north' and y == 0 else 0), grain=0.03, mottle=0.08)
        if face == 'north':
            for x in range(1, rect[2] - 1):
                p.put(rect, x, 2, CHITIN[0])
    for face, rect in rects['eye'].items():
        # red and glossy: the front and outer faces catch a glint
        for y in range(rect[3]):
            for x in range(rect[2]):
                p.put(rect, x, y, EYE_LIT if face in ('north', 'east', 'west', 'up') else EYE)
        if face in ('north', 'east', 'west'):
            p.put(rect, 0, 0, EYE_GLINT)
    for face, rect in rects['leg'].items():
        p.ramp(rect, CHITIN, lambda x, y, w, h: 0.35 - 0.2 * (y == 1), grain=0.03, mottle=0.1)
    for face, rect in rects['sac'].items():
        # a clear green membrane, thicker at the rim, with a wet highlight; the seeds show through
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                rim = x in (0, w - 1) or y in (0, h - 1)
                p.put(rect, x, y, SAC, alpha=190 if rim else 100)
        if face not in ('up', 'down'):
            p.put(rect, 1, 1, (255, 255, 240), alpha=210)
            p.put(rect, w - 2, h - 2, (120, 140, 70), alpha=200)
    for face, rect in rects['seed'].items():
        p.ramp(rect, SEED, flat(0.8 if face == 'up' else 0.5), grain=0.05, mottle=0.1)
    for face, rect in rects['base'].items():
        p.ramp(rect, JOINT, lambda x, y, w, h: 0.6 - 0.3 * (y % 2), grain=0.03, mottle=0.1)
    for face, rect in rects['shaft'].items():
        # chitin rings along the shaft
        p.ramp(rect, CHITIN, lambda x, y, w, h: 0.5 - 0.35 * (x % 2), grain=0.03, mottle=0.05)
    for face, rect in rects['tip'].items():
        p.ramp(rect, CHITIN, flat(0.08), grain=0.02, mottle=0.05)
    return p, rects['abdomen']['north']


# poses of the rostrum, degrees about x, positive down toward the bed in front
REST = {'snout': 25, 'shaft': 0, 'tip': 0}
STAB = {'snout': 50, 'shaft': 10, 'tip': 14}


def animations():
    gone = sampled(1.0, 1.0, lambda t: [0, 0, 0])
    idle = {
        # the rostrum hovers over the bed and sways, the tip a little behind the base
        'snout': {'rotation': sampled(IDLE, 0.1, lambda t: [REST['snout'] + 3 * wave(t, IDLE), 0, 0])},
        'tip': {'rotation': sampled(IDLE, 0.1, lambda t: [2 * wave(t, IDLE, 0.5), 0, 0])},
        'sac': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.04 * wave(t, IDLE), 1 + 0.03 * wave(t, IDLE), 1 + 0.04 * wave(t, IDLE)])},
        'seed': {'scale': gone},
    }

    def pose(part):
        """The rostrum swings from rest to the stab and back once a stroke, a little later toward the tip."""
        lag = {'snout': 0.0, 'shaft': 0.05, 'tip': 0.1}[part] * WORKING
        return sampled(WORKING, 0.05, lambda t: [REST[part] + (STAB[part] - REST[part]) * squeeze(t, WORKING, lag), 0, 0])

    working = {
        'snout': {'rotation': pose('snout')},
        'shaft': {'rotation': pose('shaft')},
        'tip': {'rotation': pose('tip')},
        # the seed shows at the tip as the stroke starts and is left in the bed at the bottom of the stab
        'seed': {'scale': sampled(WORKING, 0.05, lambda t: [1 if (t % WORKING) / WORKING < 0.35 else 0] * 3)},
        'seeds': {'position': sampled(WORKING, 0.05, lambda t: [0, 0.4 * squeeze(t, WORKING, WORKING / 2), 0])},
        'sac': {'scale': sampled(WORKING, 0.05, lambda t: [1 - 0.08 * squeeze(t, WORKING, WORKING / 3), 1, 1 - 0.08 * squeeze(t, WORKING, WORKING / 3)])},
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('planting_proboscis', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.5, 0)))
    print('planting_proboscis: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
