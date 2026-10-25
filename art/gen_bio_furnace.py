"""Bio-Furnace: a living kiln. A charred carapace of plates on a root foot, its seams glowing with heat that runs
hottest round the mouth; a deep firebox mouth framed by thick cheeks and a brow, bone fangs hanging over a bed of
coals; a two-tier dome and a ribbed flesh chimney whose lip glows. It must read as a furnace from across a room:
dark body, hot light in the mouth and the seams, smoke from the chimney (particles, BatchOrganBlock).

    python3 art/gen_bio_furnace.py

Idle: the fire is banked. The coals and the firebox walls glow dull red, the chimney breathes slowly. Working: a
curtain of fire rises from the coals behind the fangs and a brighter tongue of flame flickers in front of it; the
chimney pulses; the block gives light 13, flames and smoke come from the mouth and smoke from the chimney. Growth
(block state `stage`, animations grown_0..2): stage 1 braces the body with bone buttresses; stage 2 adds a bellows on
each side, pumping while the furnace works, and a second chimney. The grown bones are scaled to nothing below their
stage.

Front is north (-Z). The firebox is a cavity 8 wide, 6 high and 4 deep: the back block's face is its back wall, the
cheeks' inner sides, the brow's underside and the coal bed close it.
"""
import math

from organ_model import Painter, faces, islands, pack, sampled, wave, write

SHEET = 128
IDLE, WORKING = 7.2, 1.2
CHAR = [(20, 13, 14), (34, 22, 22), (50, 33, 31), (68, 46, 42), (88, 62, 54), (110, 80, 68), (136, 104, 88)]
FLESH = [(40, 10, 18), (66, 20, 30), (98, 36, 46), (130, 58, 66), (160, 86, 90)]
ROOT = [(22, 12, 14), (38, 22, 22), (58, 36, 32), (80, 54, 46)]
IVORY = [(70, 58, 46), (112, 96, 76), (152, 136, 108), (192, 180, 148), (226, 218, 192)]
HEAT = [(64, 12, 6), (110, 26, 8), (166, 54, 12), (220, 100, 24), (252, 158, 52), (255, 214, 120), (255, 244, 200)]
SOOT = [(8, 5, 5), (16, 10, 9), (26, 16, 14)]

BONES = [
    ('root', None, [0, 0, 0], []),
    ('base', 'root', [0, 0, 0], [('foot', [-7, 0, -7], [14, 1, 14], None)]),
    ('belly', 'root', [0, 1, 0], [
        ('back', [-6, 1, -3], [12, 9, 9], None),
        ('cheek', [-6, 1, -7], [2, 9, 4], None),
        ('cheek', [4, 1, -7], [2, 9, 4], None),
        ('brow', [-4, 8, -7], [8, 2, 4], None),
        ('hearth', [-4, 1, -7], [8, 1, 4], None),
        ('coals', [-4, 2, -6.5], [8, 1, 4], None),
        ('dome', [-5.5, 10, -5.5], [11, 2, 10], None),
        ('crest', [-4, 12, -4], [8, 1, 7], None),
    ]),
    # bone fangs hang from the brow over the firebox, a grate that is also a jaw
    ('fangs', 'belly', [0, 8, -6], [
        ('fang_short', [-3.4, 6.5, -6.5], [1, 2, 1], None),
        ('fang_long', [-1.4, 5.5, -6.5], [1, 3, 1], None),
        ('fang_long', [0.4, 5.5, -6.5], [1, 3, 1], None),
        ('fang_short', [2.4, 6.5, -6.5], [1, 2, 1], None),
    ]),
    # the fire curtain stands on the coals against the back wall; the flare licks up in front of it
    ('ember', 'belly', [0, 3, -3.65], [('fire', [-3.5, 3, -4.15], [7, 4, 1], None)]),
    ('flare', 'belly', [0, 3, -4.8], [('flame', [-3, 3, -5.3], [6, 3, 1], None)]),
    ('intake', 'belly', [0, 13, -2.5], [('intake', [-1, 13, -3.5], [2, 1, 2], None)]),
    ('flue', 'belly', [0, 13, 1.5], [
        ('chimney', [-2, 13, -0.5], [4, 2, 4], None),
        ('lip', [-2.5, 14.5, -1], [5, 1, 5], None),
    ]),
    ('ridges', 'belly', [0, 1, 0], [
        ('buttress', [-7, 1, -2.75], [1, 8, 2], None),
        ('buttress', [6, 1, -2.75], [1, 8, 2], None),
        ('buttress', [-7, 1, 3], [1, 8, 2], None),
        ('buttress', [6, 1, 3], [1, 8, 2], None),
        ('buttress_back', [-2, 1, 6], [4, 7, 1], None),
    ]),
    ('lobe_l', 'belly', [-6, 4.5, 1.25], []),
    ('breath_l', 'lobe_l', [-6, 4.5, 1.25], [('bellows', [-8, 2.5, -0.25], [2, 4, 3], None)]),
    ('lobe_r', 'belly', [6, 4.5, 1.25], []),
    ('breath_r', 'lobe_r', [6, 4.5, 1.25], [('bellows', [6, 2.5, -0.25], [2, 4, 3], None)]),
    ('crown', 'belly', [4.4, 12, 2.5], [
        ('chimney_small', [3.4, 11.5, 1.5], [2, 3, 2], None),
        ('lip_small', [3, 14.5, 1], [3, 1, 3], None),
    ]),
]
GROWN = {0: [], 1: ['ridges'], 2: ['ridges', 'lobe_l', 'lobe_r', 'crown']}

ROWS = (3, 2, 3, 2, 3, 2)  # plate rows of uneven height, top down


def ramp(colours, t):
    t = max(0.0, min(1.0, t)) * (len(colours) - 1)
    i = min(int(t), len(colours) - 2)
    f = t - i
    return tuple(int(round(a + (b - a) * f)) for a, b in zip(colours[i], colours[i + 1]))


def plate(x, y, seed=0):
    """Charred plates in rows of uneven height with staggered joins: the plate's shade, or None on a seam."""
    top, row = 0, 0
    while top + ROWS[(row + seed) % len(ROWS)] <= y:
        top += ROWS[(row + seed) % len(ROWS)]
        row += 1
    height = ROWS[(row + seed) % len(ROWS)]
    if y == top + height - 1:
        return None  # the seam under each row
    width = 4 + (row + seed) % 3
    if (x + row * 3 + seed) % width == width - 1:
        return None  # a join between plates
    col = (x + row * 3 + seed) // width
    jitter = ((row * 7 + col * 13 + seed) % 5) * 0.03
    return 0.48 + jitter + (0.14 if y == top else 0)


def hot(p, rect, x, y, t):
    """A texel glowing with heat t (0 dull red .. 1 white): painted and copied into the glowmask, so it is lit
    whatever the light; the glow is additive, so dull colours glow dimly."""
    p.put(rect, x, y, ramp(HEAT, t), glow=255)


def carapace(p, rect, heat, seed=0, top=False):
    """Charred plates whose seams glow by heat(x, y, w, h); cooler seams are dark flesh. Top faces are ashen."""
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            v = plate(x, y, seed)
            if top and v is not None:
                v += 0.1
            if v is None:
                t = heat(x, y, w, h) + p.rng.uniform(-0.05, 0.05)
                if t > 0.3:
                    hot(p, rect, x, y, t - 0.15)
                else:
                    p.put(rect, x, y, ramp(FLESH, 0.15 + p.rng.uniform(0, 0.15)))
                continue
            p.put(rect, x, y, ramp(CHAR, v - 0.12 * (1 - y / max(1, h - 1)) * (not top) + p.rng.uniform(-0.05, 0.05)))


def firebox(p, rect, floor_at_bottom=True):
    """A firebox wall, lit by the coals: red heat at the floor fading to dark soot above, with brighter streaks."""
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            rise = (y / max(1, h - 1)) if floor_at_bottom else 0.0
            t = 0.4 * rise ** 2 + 0.06 * math.sin(x * 1.9 + y * 0.7) + p.rng.uniform(-0.04, 0.04)
            if t < 0.1:
                p.put(rect, x, y, ramp(SOOT, p.rng.uniform(0, 1)))
            else:
                hot(p, rect, x, y, t)


def textures(uv):
    p = Painter(SHEET, 93)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}

    for face, rect in rects['foot'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: 0.55 if face == 'up' else 0.3, grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(6):
                p.vein(rect, ROOT[0], 6)

    def low(base, gain):
        return lambda x, y, w, h: base + gain * (y / max(1, h - 1))

    for face, rect in rects['back'].items():
        carapace(p, rect, low(-0.1, 0.5), seed=1, top=face == 'up')
        if face == 'north':
            x0, y0, w, h = rect
            firebox(p, (x0 + 2, y0 + 2, 8, 6))  # the firebox's back wall, between the cheeks and under the brow
    for face, rect in rects['cheek'].items():
        if face in ('east', 'west'):
            # one side of each cheek faces into the firebox; the other faces out. Both get the firebox's heat on
            # their front half (the side faces run front to back across the texture), the outer one dimmer.
            x0, y0, w, h = rect
            carapace(p, rect, low(-0.1, 0.5), seed=2)
            for y in range(4, 7):
                for x in range(w):
                    if p.rng.random() < 0.6:
                        hot(p, (x0, y0 + 1, w, h), x, y, 0.06 + 0.25 * (y - 4) / 2)
        elif face == 'north':
            carapace(p, rect, low(0.2, 0.45), seed=3)
        else:
            carapace(p, rect, low(-0.1, 0.45), seed=2, top=face == 'up')
    for face, rect in rects['brow'].items():
        if face == 'down':
            firebox(p, rect, floor_at_bottom=False)
        elif face == 'north':
            x0, y0, w, h = rect
            carapace(p, rect, low(0.3, 0.2), seed=4)
            for x in range(w):  # the lip over the mouth, scorched hot
                hot(p, rect, x, h - 1, 0.38 + 0.08 * math.sin(x * 2.1))
        else:
            carapace(p, rect, low(-0.2, 0.2), seed=4, top=face == 'up')
    for face, rect in rects['hearth'].items():
        p.ramp(rect, CHAR, lambda x, y, w, h: 0.35, grain=0.05, mottle=0.2)
        if face == 'up':
            x0, y0, w, h = rect
            for x in range(w):
                hot(p, rect, x, 0, 0.12 + 0.06 * math.sin(x * 1.7))  # ash glowing at the coals' feet
    for face, rect in rects['coals'].items():
        # lumps of coal: dark crust, glowing cracks between them, brightest at the back where the fire stands
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                lump = (x * 5 + y * 3 + (x // 2) * 7) % 5
                t = 0.1 + 0.06 * lump + p.rng.uniform(-0.05, 0.05)
                if face == 'up':
                    t += 0.08 * (1 - y / max(1, h - 1))
                if p.rng.random() < 0.12:
                    t += 0.25
                if lump == 0 and p.rng.random() < 0.7:
                    p.put(rect, x, y, ramp(SOOT, p.rng.uniform(0.3, 1)))
                else:
                    hot(p, rect, x, y, t)
    for face, rect in rects['dome'].items():
        carapace(p, rect, low(-0.3, 0.2), seed=5, top=face == 'up')
    for face, rect in rects['crest'].items():
        carapace(p, rect, low(-0.3, 0.1), seed=6, top=face == 'up')
    for name in ('fang_short', 'fang_long'):
        for face, rect in rects[name].items():
            x0, y0, w, h = rect
            for y in range(h):
                for x in range(w):
                    tip = y / max(1, h - 1)  # 1 at the tip, over the fire
                    if face in ('up', 'down'):
                        tip = 1.0 if face == 'down' else 0.0
                    if tip > 0.7:
                        hot(p, rect, x, y, 0.2 + 0.15 * tip)  # tips glowing from the fire below
                    else:
                        p.put(rect, x, y, ramp(IVORY, 0.75 - 0.3 * tip + p.rng.uniform(-0.05, 0.05)))
    for face, rect in rects['fire'].items():
        # tall tongues of flame: white-yellow at the coals, orange then red as they rise, gaps between tongues
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                rise = 1 - y / max(1, h - 1)  # 1 at the bottom row
                tongue = 0.5 + 0.5 * math.sin(x * 2.2 + 0.6)
                heat = rise * 0.75 + tongue * 0.5 - 0.3
                if heat < 0.08 and y < h - 1:
                    continue
                hot(p, rect, x, y, 0.45 + heat * 0.55)
    for face, rect in rects['flame'].items():
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                rise = 1 - y / max(1, h - 1)
                tongue = 0.5 + 0.5 * math.sin(x * 2.6 + 2.0)
                heat = rise * 0.8 + tongue * 0.5 - 0.35
                if heat < 0.1 and y < h - 1:
                    continue
                hot(p, rect, x, y, 0.65 + heat * 0.35)
    for face, rect in rects['intake'].items():
        p.ramp(rect, FLESH, lambda x, y, w, h: 0.1 if face == 'up' and 0 < x < w - 1 and 0 < y < h - 1 else 0.45,
               grain=0.05, mottle=0.15)
    for name in ('chimney', 'chimney_small'):
        for face, rect in rects[name].items():
            p.ramp(rect, FLESH, lambda x, y, w, h: 0.45 + 0.18 * (y % 2) - 0.2 * (y / max(1, h - 1)), grain=0.06,
                   mottle=0.2)
    for name in ('lip', 'lip_small'):
        for face, rect in rects[name].items():
            x0, y0, w, h = rect
            if face == 'up':
                for y in range(h):
                    for x in range(w):
                        edge = min(x, y, w - 1 - x, h - 1 - y)
                        if edge == 0:
                            p.put(rect, x, y, ramp(CHAR, 0.45 + p.rng.uniform(-0.05, 0.05)))
                        elif edge == 1 and w > 3:
                            hot(p, rect, x, y, 0.45 + p.rng.uniform(-0.08, 0.08))  # the hot throat
                        else:
                            p.put(rect, x, y, ramp(SOOT, p.rng.uniform(0, 0.6)))
            else:
                carapace(p, rect, lambda x, y, w, h: 0.0, seed=7)
    for name in ('buttress', 'buttress_back'):
        for face, rect in rects[name].items():
            p.ramp(rect, IVORY, lambda x, y, w, h: 0.6 + (0.15 if y == 0 else 0) - 0.3 * (y / max(1, h - 1)),
                   grain=0.05, mottle=0.12)
    for face, rect in rects['bellows'].items():
        # pleated hide: dark folds across
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                pleat = 0.25 if y % 2 else 0.6
                p.put(rect, x, y, ramp(FLESH, pleat + p.rng.uniform(-0.06, 0.06)))
    return p, rects['back']['east']


def still(scale):
    return sampled(1.0, 1.0, lambda t: list(scale) if isinstance(scale, (list, tuple)) else [scale] * 3)


def animations():
    idle = {
        'ember': {'scale': still(0)},
        'flare': {'scale': still(0)},
        'flue': {'scale': sampled(IDLE, 0.1, lambda t: [1 + 0.04 * wave(t, IDLE), 1, 1 + 0.04 * wave(t, IDLE)])},
    }
    working = {
        # the curtain flickers from the coal bed up, the flare in front faster and out of step; the chimney pulses
        # with the draught; the bellows pump in turn
        'ember': {'scale': sampled(WORKING, 0.05, lambda t: [1, 0.72 + 0.28 * wave(t, WORKING) * wave(t, WORKING / 3, 0.4), 1])},
        'flare': {'scale': sampled(WORKING, 0.05, lambda t: [1, 0.6 + 0.4 * abs(wave(t, WORKING / 2, 0.2)), 1])},
        'flue': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.1 * wave(t, WORKING, 0.5), 1, 1 + 0.1 * wave(t, WORKING, 0.5)])},
        'breath_l': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.3 * wave(t, WORKING), 1 + 0.06 * wave(t, WORKING), 1])},
        'breath_r': {'scale': sampled(WORKING, 0.05, lambda t: [1 + 0.3 * wave(t, WORKING, 0.5), 1 + 0.06 * wave(t, WORKING, 0.5), 1])},
    }
    out = {'idle': (IDLE, True, idle), 'working': (WORKING, True, working)}
    grown_bones = ['ridges', 'lobe_l', 'lobe_r', 'crown']
    for stage, shown in GROWN.items():
        out['grown_%d' % stage] = (1.0, True, {bone: {'scale': still(1 if bone in shown else 0)} for bone in grown_bones})
    return out


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('bio_furnace', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1.5, (0, 0.5, 0)))
    print('bio_furnace: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
