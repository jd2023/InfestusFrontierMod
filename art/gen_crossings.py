"""The Septum Crossing and the Vascular Stitch: part models, blockstates and item models.

    python3 art/gen_crossings.py

A crossing is a vein cell with two passages. On the two faces across its passages both lumens show: one runs straight
through a raised bone collar at the vein's depth, the other dives two pixels deeper under the collar. The other four
faces are the passages' mouths, drawn with the vein's own centre window. A stitch is a muscular tendon in the corner
of a step: anchored in bone over the side cell's lip, it runs down the side cell's face, turns along the floor and is
anchored again over the lower cell's opening; a stripe of the carried lumen runs along its outer side. The tendon's
striated texture is made here; the rest reuses the vein textures and the Reservoir's bone.
"""
import math
import random

from PIL import Image

from gen_veins import (ARMS, CORNERS, DEPTH, FACE_TURN, ITEMS, LUMENS, MID, MODELS, NS, SHARED, SIZE, SKIN, STATES,
                       TEXTURES, apply, arm_skin, arm_window, floor, model, ramp, turn_element, write)

CARRIERS = ['biomass', 'fluid', 'item']
DIVE = DEPTH + 2  # the diving passage's floor sits two pixels under the straight one
COLLAR = (2.5, 13.5)  # the bone collar's span on both axes
INSET = 0.5  # how far the collar reaches into the straight passage's edge
PROUD = 1  # how far the collar stands above the face
BONE = NS + 'reservoir_bone'
TENDON = NS + 'vascular_stitch_tendon'
MUSCLE = [(52, 14, 24), (84, 24, 36), (120, 40, 50), (158, 62, 68), (196, 98, 98), (226, 150, 140)]
AXIS_FACES = {'x': ('east', 'west'), 'y': ('up', 'down'), 'z': ('north', 'south')}


def uv(face, frm, to):
    x0, y0, z0 = frm
    x1, y1, z1 = to
    if face in ('up', 'down'):
        return [x0, z0, x1, z1]
    if face in ('north', 'south'):
        return [x0, 16 - y1, x1, 16 - y0]
    return [z0, 16 - y1, z1, 16 - y0]


def cube(frm, to, textures, skip=()):
    """A box with every face but {@code skip}; {@code textures} maps a face to its texture, '*' to the rest."""
    faces = {}
    for face in ('north', 'south', 'east', 'west', 'up', 'down'):
        if face in skip:
            continue
        faces[face] = {'uv': uv(face, frm, to), 'texture': textures.get(face, textures['*'])}
    return {'from': list(frm), 'to': list(to), 'faces': faces}


# --- the crossing, in the top-face frame: the x passage runs straight, the z passage dives under it ---

def septum_face():
    e = list(CORNERS)
    e.append(floor(MID, MID, True))
    e += arm_window('east') + arm_window('west')
    lo = 16 - DIVE
    north_end, south_end = COLLAR
    for zs in ((0, north_end), (south_end, 16)):
        e.append({'from': [MID[0], lo, zs[0]], 'to': [MID[1], lo, zs[1]],
                  'faces': {'up': {'uv': [MID[0], zs[0], MID[1], zs[1]], 'texture': '#lumen'}}})
        e.append({'from': [MID[0], lo, zs[0]], 'to': [MID[0], 16, zs[1]],
                  'faces': {'east': {'uv': [zs[0], 0, zs[1], DIVE], 'texture': '#wall'}}})
        e.append({'from': [MID[1], lo, zs[0]], 'to': [MID[1], 16, zs[1]],
                  'faces': {'west': {'uv': [zs[0], 0, zs[1], DIVE], 'texture': '#wall'}}})
    # the septum: a bone collar over the cell's centre; the straight passage runs through it, the other dives under
    xs = COLLAR
    e.append(cube([xs[0], lo, north_end], [xs[1], 16 + PROUD, MID[0] + INSET], {'*': '#bone'}, skip=('down',)))
    e.append(cube([xs[0], lo, MID[1] - INSET], [xs[1], 16 + PROUD, south_end], {'*': '#bone'}, skip=('down',)))
    return e


def crossing_blockstate():
    parts = []
    for carrier in CARRIERS:
        for axis, across in AXIS_FACES.items():
            when = {'carrier': carrier, 'axis': axis}
            for face in FACE_TURN:
                if face in across:
                    parts.append({'when': when, 'apply': apply('septum_crossing_' + carrier, face)})
                    continue
                parts.append({'when': when, 'apply': apply('vein_corners', face)})
                parts.append({'when': when, 'apply': apply(carrier + '_vein_center', face)})
                for arm in ARMS:
                    parts.append({'when': when, 'apply': apply('vein_arm_skin_' + arm, face)})
    write(STATES / 'septum_crossing.json', {'multipart': parts})


def crossing_item():
    """The biomass crossing with its passages across the top, as the graft's item."""
    elements = []
    for face, (x, y) in FACE_TURN.items():
        if face in AXIS_FACES['y']:
            parts = septum_face()
        else:
            parts = list(CORNERS) + [floor(MID, MID, False)] + [e for arm in ARMS for e in arm_skin(arm)]
        elements += [turn_element(e, x, y) for e in parts]
    textures = dict(SHARED, lumen=NS + LUMENS['biomass'], bone=BONE, particle=SKIN)
    write(ITEMS / 'septum_crossing.json', {'parent': 'minecraft:block/block', 'textures': textures,
                                           'elements': elements})


# --- the stitch, facing north on a floor: the side cell is north of it, the lower cell under it ---

def tendon_texture(seed):
    """Striated muscle running along v, with paler sinew threads, for the stitch's sheath."""
    rng = random.Random(seed)
    phase = [rng.random() * 6.28 for _ in range(SIZE)]
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            u, v = x / SIZE, y / SIZE
            fibre = 0.5 + 0.5 * math.sin(2 * math.pi * 9 * u + 0.6 * math.sin(2 * math.pi * v * 2 + phase[x // 7]))
            val = 0.28 + 0.42 * fibre + 0.10 * math.sin(2 * math.pi * 3 * v + u * 4) + 0.08 * (rng.random() - 0.5)
            if (x * 7 + int(9 * math.sin(v * 6.3))) % 23 == 0:
                val += 0.25  # a sinew thread
            img.putpixel((x, y), ramp(MUSCLE, val) + (255,))
    img.save(TEXTURES / 'vascular_stitch_tendon.png', optimize=True)


def tendon():
    """A muscular tube 6 px wide and 4 px thick: over the lip, down the face, round a stepped bend, along the floor;
    bone anchors at both ends and a stripe of the carried lumen along its outer side."""
    sheath = {'*': '#tendon'}
    bone = {'*': '#bone'}
    lumen = {'*': '#lumen'}
    return [
        cube([4, 14.5, 0], [12, 16.5, 3], bone, skip=('north',)),         # anchored over the side cell's lip
        cube([5, 5, 0], [11, 14.5, 4], sheath, skip=('north',)),          # down the side cell's face
        cube([5, 3, 0], [11, 5, 5], sheath, skip=('north',)),             # the bend, in two steps
        cube([5, 0, 0], [11, 3, 6], sheath, skip=('north', 'down')),
        cube([5, 0, 6], [11, 4, 9], sheath, skip=('down',)),              # along the floor
        cube([4, 0, 9], [12, 1.5, 12], bone, skip=('down',)),             # anchored over the lower cell's opening
        cube([7, 5, 4], [9, 14.5, 4.25], lumen, skip=('north',)),         # the lumen stripe, down and along
        cube([7, 3, 5], [9, 5, 5.25], lumen, skip=('north',)),
        cube([7, 4, 5.25], [9, 4.25, 9], lumen, skip=('down',)),
    ]


def stitch_blockstate():
    variants = {}
    for carrier in CARRIERS:
        for facing, y in zip(ARMS, (0, 90, 180, 270)):
            for face in ('floor', 'ceiling'):
                entry = {'model': NS + 'vascular_stitch_' + carrier}
                turn_y = y if face == 'floor' else (y + 180) % 360
                if face == 'ceiling':
                    entry['x'] = 180
                if turn_y:
                    entry['y'] = turn_y
                variants['carrier=%s,face=%s,facing=%s' % (carrier, face, facing)] = entry
    write(STATES / 'vascular_stitch.json', {'variants': variants})


def main():
    tendon_texture(21)
    for carrier in CARRIERS:
        textures = dict(SHARED, lumen=NS + LUMENS[carrier], bone=BONE)
        write(MODELS / ('septum_crossing_' + carrier + '.json'), model(textures, septum_face()))
        stitch = {'lumen': NS + LUMENS[carrier], 'tendon': TENDON, 'bone': BONE, 'particle': TENDON}
        write(MODELS / ('vascular_stitch_' + carrier + '.json'), {'textures': stitch, 'elements': tendon()})
    crossing_blockstate()
    stitch_blockstate()
    crossing_item()
    write(ITEMS / 'vascular_stitch.json', {
        'parent': NS + 'vascular_stitch_biomass',
        'display': {
            'gui': {'rotation': [30, 135, 0], 'translation': [1.1, 1.7, 0], 'scale': [0.85, 0.85, 0.85]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.5, 0.5, 0.5]},
            'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [0.75, 0.75, 0.75]},
            'thirdperson_righthand': {'rotation': [75, 135, 0], 'translation': [0, 2.5, 0],
                                      'scale': [0.5, 0.5, 0.5]},
            'firstperson_righthand': {'rotation': [0, 135, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
        }})
    print('crossings: 1 texture, 6 part models, 2 blockstates, 2 items')


if __name__ == '__main__':
    main()
