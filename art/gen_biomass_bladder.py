#!/usr/bin/env python3
"""The Biomass Bladder's model, animations, textures and Blockbench source, from one description.

Run from the repository root: python3 art/gen_biomass_bladder.py
Writes geo/block/biomass_bladder.geo.json, animations/block/biomass_bladder.animation.json,
textures/block_entity/biomass_bladder.png and _glowmask.png, textures/block/biomass_bladder.png (break particle)
and art/biomass_bladder.bbmodel.

A bladder (BLOCK_CATALOG T0-04): a translucent sac of feed swelling out of a lip of flesh on a clawed root foot and
closing in under a flesh collar, where a lipped port puckers over a dark throat; bone ribs bow down its four corners
over the seams where its membrane plates meet. The sac is no box: each side is a plate leaning out from the cup to the
waist and one leaning in to the collar, as the Seed Pouch's and the Compost Gland's are, and the ribs breathe with it.
The green biomass inside is its own bone, scaled to the block's fill level by the fill_0 to fill_4 animations, kept
low and narrow enough to stay inside the plates full (an art test walks out from it, and out from each rib). The
swelling sac clasped by bone, and the level seen through it, are the cues. Front is north (-Z).

GeckoLib mirrors the file's x and negates rotations about x and y (art/geo_audit.py): `lean()` and `diagonal()` take
that into account.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from organ_model import Painter, ball, bone_along, faces, flat, islands, pack, sampled, skin, wave, write

SHEET = 128
IDLE = 8.0
LEVELS = [0, 0.27, 0.52, 0.76, 1.0]
FLESH = [(84, 36, 52), (116, 52, 64), (148, 74, 80), (178, 100, 98), (204, 130, 120), (224, 160, 146)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
IVORY = [(112, 96, 84), (156, 140, 114), (194, 180, 148), (222, 212, 184), (240, 234, 212), (250, 248, 236)]
GREEN = [(22, 74, 50), (38, 112, 62), (64, 152, 72), (104, 192, 84), (156, 226, 108), (206, 250, 150)]
LIP = [(120, 40, 70), (160, 70, 96), (196, 110, 126), (222, 150, 156)]
VESSEL = (122, 34, 44)
MEMBRANE, MEMBRANE_VEIN, GLINT = (232, 204, 176), (176, 96, 110), (255, 250, 236)
SX, SZ = 1, -1
R0, Y0 = 5.5, 3  # the sac's foot: its plates hinge here, inside the cup
LOW, UP = 5, 5  # plate heights
OUT, IN = 12, 35  # the lower plates lean out to the waist, the upper ones in to the collar
WIDE = 10  # plate width; the bone ribs cover where neighbouring plates meet


def lean(sx, sz, deg):
    """Rotation tipping a part at (sx, sz), each -1, 0 or 1, outward at the top by `deg` (inward if negative)."""
    return [SZ * sz * deg, 0, SX * sx * deg]


def waist():
    a = math.radians(OUT)
    return R0 + LOW * math.sin(a), Y0 + LOW * math.cos(a)


def plates(axis, side):
    """One side of the sac: a membrane plate hinged on its outer foot inside the cup and leaning out to the waist, and
    one hinged on its top outer edge leaning in to the collar, so the sac swells; no plate is a face of a box."""
    wx, wy = waist()

    def box(out, y, h, w):
        lo = side * out if side < 0 else side * out - 1
        return ([lo, y, -w / 2], [1, h, w]) if axis == 'x' else ([-w / 2, y, lo], [w, h, 1])

    def at(out, y):
        return [side * out, y, 0] if axis == 'x' else [0, y, side * out]

    sx, sz = (side, 0) if axis == 'x' else (0, side)
    low, up = box(R0, Y0, LOW, WIDE), box(wx, wy, UP, WIDE - 1)  # a pixel apart, so their ends never share a plane
    return [('skin_low_' + axis, low[0], low[1], (lean(sx, sz, OUT), at(R0, Y0))),
            ('skin_up_' + axis, up[0], up[1], (lean(sx, sz, -IN), at(wx, wy)))]


def diagonal(sx, sz, deg):
    """Rotation turning a part by an eighth of a turn about y and tipping its top outward along the diagonal toward
    corner (sx, sz) by `deg` (inward if negative). GeckoLib turns a vertex about x first, then y, then z, with x and y
    negated (art/geo_audit.py), so a tilt about the part's own x, turned by the y, leans it along a diagonal; the turn
    picks which one (any odd eighth leaves a square section the same)."""
    return [-deg, -math.degrees(math.atan2(-sx, sz)), 0]


def rib(sx, sz):
    """A bone rib down one corner of the sac, over the seam where two plates meet: two diamonds' worth of bone, turned
    corner-out, the lower bowing out to the waist and the upper closing in under the collar."""
    wx, wy = waist()
    foot, belly, top = 4.75, wx - 0.75, 4.4
    rise = wy - Y0
    out_deg = math.degrees(math.atan(math.sqrt(2) * (belly - foot) / rise))
    in_deg = math.degrees(math.atan(math.sqrt(2) * (belly - top) / (11.9 - wy)))
    seg = []
    # the lower bone is a pixel slimmer than the upper, so where they overlap at the knee no faces share a plane
    for name, c, y, h, w, deg in (('rib_low', foot, Y0 - 0.25, LOW + 1, 2, out_deg), ('rib_up', belly, wy, UP, 3, -in_deg)):
        cx, cz = sx * c, sz * c
        seg.append((name, [cx - w / 2, y, cz - w / 2], [w, h, w], (diagonal(sx, sz, deg), [cx, y, cz])))
    return seg


def claw(sx, sz):
    cx, cz = sx * 5, sz * 5
    return ('rootlet', [cx - 1, 0.5, cz - 2], [2, 1, 4], ([0, 45 * sx * sz, 0], [cx, 1, cz]))


def cup(side, axis):
    """One lip of the flesh cup round the sac's foot, hinged on its outer foot and flaring out at the top."""
    lo = -7 if side < 0 else 4
    origin, size = ([lo, 0.75, -5], [3, 3, 10]) if axis == 'x' else ([-5, 0.75, lo], [10, 3, 3])
    pivot = [side * 7, 0.75, 0] if axis == 'x' else [0, 0.75, side * 7]
    sx, sz = (side, 0) if axis == 'x' else (0, side)
    return ('cup_' + axis, origin, size, (lean(sx, sz, 6), pivot))


BONES = [
    ('root', None, [0, 0, 0], []),
    # the root foot and the flesh cup the sac sits in
    ('base', 'root', [0, 0, 0], [
        ('slab', [-6, 0, -6], [12, 1, 12], None),
        claw(1, 1), claw(1, -1), claw(-1, 1), claw(-1, -1),
        ('floor', [-5.5, 1.25, -5.5], [11, 2, 11], None),
        cup(-1, 'x'), cup(1, 'x'), cup(-1, 'z'), cup(1, 'z'),
    ]),
    # the biomass: low enough under the shoulders to stay inside the plates full, its quarter level clear of the cup
    ('fluid', 'root', [0, 2.875, 0], [('fluid', [-3.5, 2.875, -3.5], [7, 7, 7], None)]),
    # the sac: membrane plates swelling from the cup to the waist and closing in to the collar
    ('membrane', 'root', [0, 3, 0], plates('x', -1) + plates('x', 1) + plates('z', -1) + plates('z', 1)),
    # the bone ribs clasp its corners and breathe with it
    ('ribs', 'membrane', [0, 3, 0], rib(1, 1) + rib(1, -1) + rib(-1, 1) + rib(-1, -1)),
    # the collar rides the sac; a port of four lips round a dark throat on top, the corners open
    ('cap', 'membrane', [0, 12, 0], [
        ('collar', [-4.5, 11.5, -4.5], [9, 2, 9], None),
        ('throat', [-1.5, 13.25, -1.5], [3, 1, 3], None),
        ('lip_ns', [-2, 13.25, -3], [4, 2, 1], (lean(0, -1, 15), [0, 13.25, -2])),
        ('lip_ns', [-2, 13.25, 2], [4, 2, 1], (lean(0, 1, 15), [0, 13.25, 2])),
        ('lip_ew', [-3, 13.5, -2], [1, 2, 4], (lean(-1, 0, 15), [-2, 13.5, 0])),
        ('lip_ew', [2, 13.5, -2], [1, 2, 4], (lean(1, 0, 15), [2, 13.5, 0])),
    ]),
    ('pustules', 'membrane', [0, 3, 0], [
        ('pustule', [-5.25, 12, -1], [1, 1, 2], None),
        ('pustule', [4.25, 12.25, 0.5], [1, 1, 2], None),
    ]),
]


def textures(uv):
    p = Painter(SHEET, 33)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for face, rect in rects['slab'].items():
        p.ramp(rect, ROOT, flat(0.6 if face == 'up' else 0.3), grain=0.06, mottle=0.3)
        if face == 'up':
            for _ in range(4):
                p.vein(rect, ROOT[0], 5)
    for face, rect in rects['rootlet'].items():
        p.ramp(rect, ROOT, lambda x, y, w, h: (0.7 if face == 'up' else 0.45) - 0.15 * (x % 3 == 1))
    for name in ('floor', 'cup_x', 'cup_z', 'collar'):
        for face, rect in rects[name].items():
            # taut flesh lit from above in soft blotches, red vessels on the sides
            p.ramp(rect, FLESH, lambda x, y, w, h: 0.4 * skin(x, y, w, h) + 0.45 * ball(x, y, w, h) + 0.05, grain=0.04, mottle=0.3)
            if face not in ('up', 'down'):
                p.vein(rect, VESSEL, 4, 170)
    for name in ('rib_low', 'rib_up'):
        for face, rect in rects[name].items():
            p.ramp(rect, IVORY, bone_along(face not in ('up', 'down')), grain=0.05, mottle=0.1)
    # the biomass: a green body, brighter at the top, with a few bright specks; faintly emissive so it reads at night
    for face, rect in rects['fluid'].items():
        p.ramp(rect, GREEN, lambda x, y, w, h: 0.75 - 0.3 * y / max(1, h - 1) if face != 'up' else 0.85, grain=0.05, mottle=0.2, alpha=215)
        for _ in range(3):
            p.put(rect, p.rng.randrange(rect[2]), p.rng.randrange(rect[3]), GREEN[5], 230)
        p.glow_rect(rect, 70)
    # the membrane: thin and see-through, a little denser low where it sits in the cup, veined, one glint per plate;
    # no rim, so no plate draws a pane's outline
    for name in ('skin_low_x', 'skin_low_z', 'skin_up_x', 'skin_up_z'):
        for face, rect in rects[name].items():
            x0, y0, w, h = rect
            for y in range(h):
                for x in range(w):
                    p.put(rect, x, y, MEMBRANE, 58 + int(18 * y / max(1, h - 1)) if name.startswith('skin_low') else 58)
            if face in ('east', 'west', 'north', 'south') and max(w, h) > 2:
                for _ in range(2):
                    p.vein(rect, MEMBRANE_VEIN, 6, 160)
                if name.startswith('skin_up'):
                    p.put(rect, 2, 1, GLINT, 160)
    for face, rect in rects['throat'].items():
        p.ramp(rect, FLESH, lambda x, y, w, h: 0.05 + 0.1 * (x in (0, w - 1) or y in (0, h - 1)), grain=0.02, mottle=0.05)
    for name in ('lip_ns', 'lip_ew'):
        for face, rect in rects[name].items():
            p.ramp(rect, LIP, lambda x, y, w, h: 0.45 + 0.5 * ball(x, y, w, h) - 0.3 * (x in (0, w - 1) or y in (0, h - 1)), grain=0.04, mottle=0.12)
    for face, rect in rects['pustule'].items():
        p.ramp(rect, GREEN, ball, grain=0.04, mottle=0.1)
        p.glow_rect(rect, 255)
    return p, rects['collar']['north']


def animations():
    out = {'idle': (IDLE, True, {
        'membrane': {'scale': sampled(IDLE, 0.1, lambda t: [0.97 + 0.01 * wave(t, IDLE), 1, 0.97 + 0.01 * wave(t, IDLE)])},
    })}
    for n, level in enumerate(LEVELS):
        out['fill_%d' % n] = (IDLE, True, {
            'fluid': {'scale': sampled(IDLE, 0.1, lambda t, lv=level: [1 if lv else 0, lv, 1 if lv else 0])},
        })
    return out


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('biomass_bladder', BONES, SHEET, painter, animations(), (particle[0], particle[1]))
    print('biomass_bladder: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
