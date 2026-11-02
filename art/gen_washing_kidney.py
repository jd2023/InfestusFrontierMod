"""Washing Kidney (BLOCK_CATALOG T2-24): a bean of two lobes that rinses what the Gizzard left behind. Two lobes of
blue-grey muscle lie side by side on a root foot, a pale clean-water sac stands on the left lobe and a murky dirty-water
sac on the right, a bone sieve plate lies between them, and a low spout at the front drips onto a damp patch of the foot.
The clean sac, the dirty sac and the sieve are the cues: what is rinsed with, what it leaves and what it passes through.
The Mineral Gizzard beside it is a wide grinding mill; this one is two soft lobes under two sacs.

    python3 art/gen_washing_kidney.py

Temporary procedural art for the functional task; the final model is an art task. Idle: the lobes breathe in turn.
Working: the lobes squeeze in a wave, the sieve shivers and the drip pulses. Front is north (-Z). GeckoLib mirrors the
file's x and negates rotations about x and y (art/geo_audit.py).
"""
from organ_model import Painter, ball, faces, flat, islands, pack, sampled, skin, squeeze, wave, write

SHEET = 64
IDLE, WORKING = 8.0, 3.6
LOBE = [(38, 46, 62), (62, 76, 94), (88, 106, 124), (116, 136, 150), (144, 164, 174), (174, 190, 196)]
CLEAN = [(48, 92, 120), (72, 130, 160), (104, 168, 192), (146, 204, 218), (196, 232, 238)]
DIRTY = [(40, 38, 26), (66, 60, 38), (96, 86, 52), (126, 114, 70), (156, 142, 92)]
BONE = [(96, 86, 74), (146, 134, 112), (190, 178, 150), (222, 212, 186), (244, 238, 218)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
DAMP = [(40, 52, 66), (62, 82, 98), (88, 114, 128), (120, 148, 156)]
VESSEL = (122, 34, 44)


def claw(sx, sz):
    cx, cz = sx * 5, sz * 4.5
    return ('rootlet', [cx - 1, 0.5, cz - 2], [2, 1, 4], ([0, 45 * sx * sz, 0], [cx, 1, cz]))


BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('slab', [-6.5, 0, -6], [13, 2, 12], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
    ]),
    ('lobe_l', 'root', [-3.5, 1.5, 0], [('lobe_a', [-7.5, 1.5, -4], [7, 6, 8], None)]),
    ('lobe_r', 'root', [3.5, 1.5, 0], [('lobe_b', [0.5, 1.5, -4], [7, 5, 8], None)]),
    # the hilum: where the clean line enters and the dirty line leaves, a small ridge across the front seam
    ('hilum', 'root', [0, 3, -4.5], [('hilum', [-1.5, 2.5, -5], [3, 3, 2], None)]),
    ('clean', 'lobe_l', [-3.5, 7.5, 0], [('sac_clean', [-6.5, 7.5, -2], [5, 3, 4], None)]),
    ('dirty', 'lobe_r', [3.5, 6.5, 0], [('sac_dirty', [1.5, 6.5, -2], [5, 3, 4], None)]),
    ('sieve', 'root', [0, 7.5, 0], [('plate', [-1, 6.75, -3], [2, 1, 6], None)]),
    ('spout', 'root', [0, 2, -5.5], [('spout', [-1, 1.5, -6.5], [2, 2, 2], None)]),
    ('drip', 'root', [0, 0.25, -7.25], [('damp', [-2, 0.25, -7.75], [4, 1, 2], None)]),
]


def textures(uv):
    p = Painter(SHEET, 733)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.6 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
    for face, rect in rects['rootlet'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.7 if face == 'up' else 0.45) - 0.15 * (x % 3 == 1))
    for name in ('lobe_a', 'lobe_b', 'hilum'):
        for face, rect in rects[name].items():
            # taut blue-grey muscle with a few red vessels, one soft light from the upper left
            p.ramp(rect, LOBE, lambda x, y, w, h: 0.3 * skin(x, y, w, h) + 0.55 * ball(x, y, w, h) + 0.1, grain=0.03, mottle=0.4)
            if face not in ('up', 'down') and name != 'hilum':
                p.vein(rect, VESSEL, 4, 170)
    for name, ramp in (('sac_clean', CLEAN), ('sac_dirty', DIRTY)):
        for face, rect in rects[name].items():
            # a translucent bladder: bright crown, darker belly
            p.ramp(rect, ramp, lambda x, y, w, h: 0.3 * skin(x, y, w, h) + 0.6 * ball(x, y, w, h) + 0.08, grain=0.04, mottle=0.3)
    for face, rect in rects['plate'].items():
        # an ivory sieve: the top is pitted
        value = (lambda x, y, w, h: 0.78 - 0.4 * ((x + y) % 2 == 0)) if face == 'up' else flat(0.6)
        p.ramp(rect, BONE, value, grain=0.05, mottle=0.12)
    for face, rect in rects['spout'].items():
        p.ramp(rect, BONE, lambda x, y, w, h: 0.55 + 0.25 * ball(x, y, w, h), grain=0.05, mottle=0.1)
    for face, rect in rects['damp'].items():
        p.ramp(rect, DAMP, lambda x, y, w, h: (0.65 if face == 'up' else 0.35) + 0.15 * ((x * 5 + y * 3) % 4 == 0), grain=0.1, mottle=0.3)
    return p, rects['lobe_a']['north']


def animations():
    def breathe(lag, amount):
        return {'scale': sampled(IDLE, 0.2, lambda t: [1 + amount * max(0.0, wave(t, IDLE, lag)), 1, 1 + amount * max(0.0, wave(t, IDLE, lag))])}

    idle = {
        'lobe_l': breathe(0.0, 0.02),
        'lobe_r': breathe(IDLE / 2, 0.02),
        'clean': {'scale': sampled(IDLE, 0.4, lambda t: [1, 1 + 0.03 * wave(t, IDLE), 1])},
        'dirty': {'scale': sampled(IDLE, 0.4, lambda t: [1, 1 + 0.03 * wave(t, IDLE, IDLE / 2), 1])},
        'drip': {'scale': sampled(IDLE, 0.4, lambda t: [1, 0.6, 1])},
    }

    def clench(lag, out):
        return {'scale': sampled(WORKING, 0.1, lambda t: [1 + out * squeeze(t, WORKING, lag), 1, 1 + out * squeeze(t, WORKING, lag)])}

    working = {
        'lobe_l': clench(0.0, -0.05),
        'lobe_r': clench(0.4 * WORKING, -0.05),
        'clean': {'scale': sampled(WORKING, 0.1, lambda t: [1, 1 - 0.1 * squeeze(t, WORKING), 1])},
        'dirty': {'scale': sampled(WORKING, 0.1, lambda t: [1, 1 + 0.12 * squeeze(t, WORKING, 0.4 * WORKING), 1])},
        'sieve': {'rotation': sampled(WORKING, 0.1, lambda t: [0, 4 * wave(t, WORKING), 0])},
        'spout': {'scale': sampled(WORKING, 0.1, lambda t: [1, 1 + 0.1 * squeeze(t, WORKING, 0.5 * WORKING), 1])},
        'drip': {'scale': sampled(WORKING, 0.1, lambda t: [1, 0.6 + 0.4 * (t / WORKING), 1])},
    }
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('washing_kidney', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('washing_kidney: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
