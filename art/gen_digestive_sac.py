#!/usr/bin/env python3
"""The Digestive Sac's model, animations, textures and Blockbench source, from one description.

Run from the repository root: python3 art/gen_digestive_sac.py
Writes geo/block/digestive_sac.geo.json, animations/block/digestive_sac.animation.json,
textures/block_entity/digestive_sac.png and _glowmask.png, textures/block/digestive_sac.png (break particle)
and art/digestive_sac.bbmodel.

A stomach. A pale, bloated sac sits on gnarled root claws, swollen at the sides; a veined membrane window in its
front shows the green acid inside, glowing, with half-digested lumps floating in it; a ribbed gullet rises from its
shoulder to a thick-lipped mouth that the player feeds; green glands bead its skin. It must read as a stomach from
across a room: the window of glowing acid is the cue, the body stays pale and quiet around it.

Idle: the sac breathes slowly and the acid barely sways. Working: a squeeze travels up the sac, the acid sloshes,
the gullet swallows and the lips gulp; bubbles pop at the mouth and it gurgles (DigestiveSacBlock.animateTick).
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from organ_model import Painter, ball, bone_along, faces, flat, islands, pack, sampled, squeeze, wave, write

SHEET = 128
IDLE, WORKING = 6.0, 3.0
SKIN = [(74, 28, 44), (104, 42, 60), (136, 64, 82), (164, 92, 104), (190, 122, 126), (210, 154, 150)]
LIP = [(64, 20, 34), (92, 34, 48), (124, 54, 66), (156, 80, 88), (184, 110, 112)]
ROOT = [(30, 18, 22), (48, 30, 32), (70, 46, 44), (96, 66, 58), (122, 90, 76)]
ACID = [(18, 70, 36), (34, 110, 48), (62, 156, 60), (104, 200, 74), (160, 234, 104), (214, 252, 156)]
LINING = [(30, 10, 18), (48, 16, 28), (70, 26, 38), (94, 40, 52)]
VEIN = [(84, 22, 40), (110, 34, 54)]
MEMBRANE = (226, 214, 176)
LUMP = [(58, 44, 22), (84, 66, 30), (110, 90, 44)]


def claw(sx, sz):
    cx, cz = sx * 5.5, sz * 5.5
    return ('claw', [cx - 1, 0.5, cz - 2.5], [2, 2, 5], ([0, 45 * sx * sz, 0], [cx, 1.5, cz]))


BONES = [
    ('root', None, [0, 0, 0], []),
    ('base', 'root', [0, 0, 0], [
        ('slab', [-7, 0, -7], [14, 1, 14], None),
        ('stem', [-5, 1, -4], [10, 1, 8], None),
    ]),
    ('roots', 'root', [0, 0, 0], [claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1)]),
    # the sac is bean-shaped: the great lobe on the left rises high under the gullet, the small lobe on the right
    # sags low and narrows towards the outlet; glands bead the skin
    ('belly', 'root', [-1, 2, 0], [
        ('lobe', [-6, 2, -5], [9, 9, 10], None),
        ('cap', [-5, 11, -4], [7, 1, 8], None),
        ('swell', [-7, 3, -4], [1, 7, 8], None),
        ('swell_back', [-5, 3, 5], [7, 7, 1], None),
        ('small_lobe', [3, 2, -4], [4, 6, 8], None),
        ('outlet', [4.5, 1.5, -1], [2, 1, 2], None),
        ('gland', [-7.5, 8.5, 1.5], [2, 2, 2], None),
        ('gland', [5.5, 6.5, -3.5], [2, 2, 2], None),
        ('gland', [0.5, 9.5, 4.5], [2, 2, 2], None),
    ]),
    # the window: a see-through membrane bulging from the great lobe's front, the acid inside it
    ('window', 'belly', [-1.5, 3, -5], [('membrane', [-5, 3, -6.5], [7, 6, 2], None)]),
    ('acid', 'belly', [-1.5, 3.5, -5.5], [('acid', [-4.5, 3.5, -6], [6, 4, 1], None)]),
    ('gullet', 'belly', [-1.5, 12, 0], [
        ('gullet', [-3, 12, -1.5], [3, 2, 3], None),
        ('ring', [-3.5, 12.5, -2], [4, 1, 4], None),
    ]),
    ('mouth', 'gullet', [-1.5, 14, 0], [('pit', [-3.5, 14, -2], [4, 1, 4], None)]),
    ('lip_north', 'mouth', [-1.5, 14, -3], [('lip_long', [-4.5, 14, -3.2], [6, 1, 1], None)]),
    ('lip_south', 'mouth', [-1.5, 14, 3], [('lip_long', [-4.5, 14, 2.2], [6, 1, 1], None)]),
    ('lip_west', 'mouth', [-4.5, 14, 0], [('lip_short', [-4.7, 14, -2], [1, 1, 4], None)]),
    ('lip_east', 'mouth', [1.5, 14, 0], [('lip_short', [0.7, 14, -2], [1, 1, 4], None)]),
]
LIPS = {'lip_north': ([-1, 0, 0]), 'lip_south': ([1, 0, 0]), 'lip_west': ([0, 0, 1]), 'lip_east': ([0, 0, -1])}


def skin(p, rect, top=0.0, veins=2):
    """Stretched stomach skin, lit from above, darker low, creased by shallow folds that run round it; a few dark
    veins wander over it."""
    x0, y0, w, h = rect
    p.ramp(rect, SKIN, lambda x, y, w, h: 0.74 - 0.34 * (y / max(1, h - 1)) + top
           - (0.06 if x in (0, w - 1) and w > 3 else 0) - (0.1 if (y + x // 4) % 3 == 2 else 0),
           grain=0.06, mottle=0.28)
    if w > 3 and h > 2:
        for _ in range(veins):
            p.vein(rect, VEIN[p.rng.randrange(2)], h)


def textures(uv):
    p = Painter(SHEET, 21)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}

    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.55 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(6):
                p.vein(rect, ROOT[0], 6)
    for face, rect in rects['claw'].items():
        long_way = face in ('east', 'west', 'up', 'down')
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.78 if face == 'up' else 0.6 - 0.2 * y / max(1, h - 1))
               - (0.14 * ((x if long_way else 9) % 3 == 1)))
    for face, rect in rects['stem'].items():
        p.ramp(rect, SKIN, flat(0.3), grain=0.06, mottle=0.3)

    for name in ('lobe', 'cap', 'swell', 'swell_back', 'small_lobe', 'outlet'):
        for face, rect in rects[name].items():
            if face == 'down':
                p.ramp(rect, LINING, flat(0.5))
            elif face == 'up':
                p.ramp(rect, SKIN, lambda x, y, w, h: 0.8 - (0.1 if x in (0, w - 1) or y in (0, h - 1) else 0), mottle=0.3)
            else:
                skin(p, rect, top=0.06 if name == 'cap' else 0.0, veins=3 if 'lobe' in name else 1)
        if name == 'lobe':
            # behind the window the sac's inside shows: dark lining above the acid
            x0, y0, w, h = rects['lobe']['north']
            p.ramp((x0 + 1, y0 + 2, 7, 6), LINING, flat(0.35), grain=0.05, mottle=0.2)

    for face, rect in rects['gland'].items():
        p.ramp(rect, ACID, ball if face != 'down' else flat(0.3), grain=0.04, mottle=0.1)
        if face != 'down':
            p.put(rect, 0, 0, ACID[5])
            p.glow_rect(rect, 200)

    for face, rect in rects['membrane'].items():
        # a stretched see-through membrane: pale and faint, thicker at its rim, with opaque veins across it
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                rim = x in (0, w - 1) or y in (0, h - 1)
                p.put(rect, x, y, MEMBRANE, alpha=130 if rim else 34)
        if face == 'north':
            p.put(rect, 1, 1, (250, 248, 236), alpha=200)  # a wet highlight
            p.put(rect, 2, 1, (250, 248, 236), alpha=140)
        elif face == 'up':
            p.ramp(rect, SKIN, flat(0.6), grain=0.04, mottle=0.2)

    for face, rect in rects['acid'].items():
        # glowing green acid: brighter at the surface, half-digested lumps floating in it, bubbles
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                t = 0.5 + 0.3 * (y / max(1, h - 1)) + 0.08 * ((x + y) % 3 == 0)
                if face == 'up':
                    t = 0.8 + 0.1 * ((x * 3 + y) % 2)
                p.put(rect, x, y, ramp(ACID, t + p.rng.uniform(-0.05, 0.05)), glow=230)
        if face in ('north', 'south'):
            for lx, ly in ((1, 2), (4, 3)):
                p.put(rect, lx, ly, LUMP[p.rng.randrange(3)])  # half-digested lumps
            p.put(rect, 3, 0, ACID[5], glow=255)  # bubbles at the surface
            p.put(rect, 5, 1, ACID[5], glow=255)

    for name in ('gullet', 'ring'):
        for face, rect in rects[name].items():
            if face in ('up', 'down'):
                p.ramp(rect, LINING, flat(0.4))
            else:
                p.ramp(rect, SKIN, lambda x, y, w, h: 0.62 - 0.12 * (y % 2) + (0.08 if name == 'ring' else 0),
                       grain=0.05, mottle=0.2)
    for face, rect in rects['pit'].items():
        p.ramp(rect, LINING, flat(0.25))
        if face == 'up':
            x0, y0, w, h = rect
            p.put(rect, 1, 2, ACID[2], glow=160)  # a glint of acid down the throat
            p.put(rect, 2, 1, ACID[1], glow=120)
    for name, vertical in (('lip_long', False), ('lip_short', True)):
        for face, rect in rects[name].items():
            p.ramp(rect, LIP, bone_along(vertical), grain=0.05, mottle=0.15)  # thick fleshy lips
    return p, rects['lobe']['east']


def ramp(colours, t):
    t = max(0.0, min(1.0, t)) * (len(colours) - 1)
    i = min(int(t), len(colours) - 2)
    f = t - i
    return tuple(int(round(a + (b - a) * f)) for a, b in zip(colours[i], colours[i + 1]))


def animations():
    idle = {
        'belly': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.015 * wave(t, IDLE), 1, 1 + 0.015 * wave(t, IDLE)])},
        'acid': {'scale': sampled(IDLE, 0.1, lambda t: [1, 0.96 + 0.02 * wave(t, IDLE / 2), 1])},
    }
    working = {
        # a squeeze runs up the sac; the acid sloshes; the gullet swallows behind it; the lips gulp
        'belly': {'scale': sampled(WORKING, 0.05, lambda t: [1 - 0.04 * squeeze(t, WORKING), 1, 1 - 0.04 * squeeze(t, WORKING)])},
        'acid': {'scale': sampled(WORKING, 0.05, lambda t: [1, 0.9 + 0.08 * wave(t, WORKING / 2, 0.2), 1]),
                 'rotation': sampled(WORKING, 0.05, lambda t: [0, 0, 2.5 * wave(t, WORKING / 2)])},
        'gullet': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.1 * squeeze(t, WORKING, 0.9), 1, 1 + 0.1 * squeeze(t, WORKING, 0.9)])},
    }
    for lip, axis in LIPS.items():
        idle[lip] = {'rotation': sampled(IDLE, 0.1, lambda t, a=axis: [v * 3 * wave(t, IDLE, 0.6) for v in a])}
        working[lip] = {'rotation': sampled(WORKING, 0.05, lambda t, a=axis: [v * 14 * squeeze(t, WORKING, lag=1.2) for v in a])}
    return {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('digestive_sac', BONES, SHEET, painter, animations(), (particle[0], particle[1]))
    print('digestive_sac: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
