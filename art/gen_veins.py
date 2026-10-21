"""The vein cells and the Mouth: lumen, wall, lip and throat textures, the part models and the four blockstates.

    python3 art/gen_veins.py

A vein cell is a shell of mature substrate skin. Every face is a 3x3 grid of tiles: four corners, four arms and a
centre. Where the lumen runs along a face, its tiles are a window recessed 2 px, floored with the lumen and walled.
A face shows an arm window toward each linked neighbour in its plane, and its centre window whenever any arm is open
or the lumen passes straight through toward it; the top face always shows its centre. Each part is modelled once in
the top-face frame and rotated onto the six faces by the blockstate.

Only the flat skin carries a cullface. A window's floor and walls are always drawn: a groove seen from a neighbour can
run into a face of this cell that a block covers, such as the foot of a climb under the next cell up, and the window
must still be lined there.
"""
import json
import math
import random
from pathlib import Path
from PIL import Image

ROOT = Path('src/main/resources/assets/infestusfrontier')
TEXTURES = ROOT / 'textures/block'
MODELS = ROOT / 'models/block'
ITEMS = ROOT / 'models/item'
STATES = ROOT / 'blockstates'
NS = 'infestusfrontier:block/'
SIZE = 64
FRAMES = 16
GREEN = [(30, 72, 40), (58, 128, 64), (96, 190, 90), (150, 236, 130), (214, 255, 190)]
WATER = [(22, 52, 112), (40, 96, 182), (84, 150, 226), (150, 206, 250), (214, 240, 255)]
MUSCLE = [(110, 48, 64), (150, 70, 84), (186, 100, 108), (214, 140, 140), (236, 186, 176)]
DRY = [(90, 74, 68), (122, 104, 92), (150, 132, 116), (178, 162, 144), (200, 188, 170)]
WALL = [(40, 20, 30), (62, 30, 42), (86, 44, 56), (112, 62, 72)]
SKIN = NS + 'living_substrate_mature_5'

LO, MID, HI = (0, 5), (5, 11), (11, 16)
DEPTH = 2
ARMS = ['north', 'east', 'south', 'west']
ARM_TILE = {'north': (MID, (0, 5)), 'south': (MID, (11, 16)), 'west': ((0, 5), MID), 'east': ((11, 16), MID)}


def ramp(colours, v):
    i = max(0, min(len(colours) - 1, int(v * len(colours))))
    return colours[i]


def lumen(name, colours, seed, ripple, flow):
    """A lumen floor, periodic in both axes. With flow it is a strip of frames whose crests travel along v."""
    rng = random.Random(seed)
    grain = [[rng.random() - 0.5 for _ in range(SIZE)] for _ in range(SIZE)]
    frames = FRAMES if flow else 1
    img = Image.new('RGBA', (SIZE, SIZE * frames))
    tau = 2 * math.pi
    for f in range(frames):
        phase = tau * f / FRAMES
        for y in range(SIZE):
            for x in range(SIZE):
                u, v = x / SIZE, y / SIZE
                val = 0.52 + 0.14 * math.sin(tau * (u + 3 * v)) * ripple + 0.08 * math.sin(tau * (2 * v - u) + 1.3)
                val += 0.08 * grain[y][x]
                val += 0.12 * math.cos(tau * (u - 0.5))  # brighter along the middle of the channel
                if flow:
                    val += 0.16 * math.cos(tau * 2 * v - phase) ** 3  # a crest every half block, travelling along v
                img.putpixel((x, y + f * SIZE), ramp(colours, val) + (255,))
    img.save(TEXTURES / (name + '.png'), optimize=True)
    meta = TEXTURES / (name + '.png.mcmeta')
    if flow:
        meta.write_text(json.dumps({'animation': {'frametime': 3, 'interpolate': True}}, indent=2) + '\n')
    elif meta.exists():
        meta.unlink()


def wall(seed):
    rng = random.Random(seed)
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            v = 0.5 - 0.3 * (y / SIZE) + 0.1 * math.sin(x / 4.0 + y / 7.0) + 0.1 * (rng.random() - 0.5)
            img.putpixel((x, y), ramp(WALL, v) + (255,))
    img.save(TEXTURES / 'vein_wall.png', optimize=True)


# --- geometry in the top-face frame: the face is y = 16, windows reach down to y = 14 ---

def skin(xs, zs):
    return {'from': [xs[0], 16, zs[0]], 'to': [xs[1], 16, zs[1]],
            'faces': {'up': {'uv': [xs[0], zs[0], xs[1], zs[1]], 'texture': '#skin', 'cullface': 'up'}}}


def floor(xs, zs, along_x):
    face = {'uv': [xs[0], zs[0], xs[1], zs[1]], 'texture': '#lumen'}
    if along_x:
        face = {'uv': [zs[0], xs[0], zs[1], xs[1]], 'texture': '#lumen', 'rotation': 90}
    return {'from': [xs[0], 16 - DEPTH, zs[0]], 'to': [xs[1], 16 - DEPTH, zs[1]], 'faces': {'up': face}}


def wall_x(x, zs, facing):
    """A window wall in the plane x = const, facing east or west into the window."""
    return {'from': [x, 16 - DEPTH, zs[0]], 'to': [x, 16, zs[1]],
            'faces': {facing: {'uv': [zs[0], 0, zs[1], DEPTH], 'texture': '#wall'}}}


def wall_z(z, xs, facing):
    """A window wall in the plane z = const, facing north or south into the window."""
    return {'from': [xs[0], 16 - DEPTH, z], 'to': [xs[1], 16, z],
            'faces': {facing: {'uv': [xs[0], 0, xs[1], DEPTH], 'texture': '#wall'}}}


def arm_window(arm):
    xs, zs = ARM_TILE[arm]
    if arm in ('north', 'south'):
        return [floor(xs, zs, False), wall_x(MID[0], zs, 'east'), wall_x(MID[1], zs, 'west')]
    return [floor(xs, zs, True), wall_z(MID[0], xs, 'south'), wall_z(MID[1], xs, 'north')]


def arm_skin(arm):
    """The closed arm's skin and the centre window's wall on that side; the wall hides under skin when the centre is
    closed too."""
    xs, zs = ARM_TILE[arm]
    edge = {'north': wall_z(MID[0], MID, 'south'), 'south': wall_z(MID[1], MID, 'north'),
            'west': wall_x(MID[0], MID, 'east'), 'east': wall_x(MID[1], MID, 'west')}[arm]
    return [skin(xs, zs), edge]


CORNERS = [skin(LO, LO), skin(HI, LO), skin(LO, HI), skin(HI, HI)]


def model(textures, elements):
    return {'textures': dict(textures, particle=SKIN), 'elements': elements}


SHARED = {'skin': SKIN, 'wall': NS + 'vein_wall'}
LUMENS = {'biomass': 'vein_lumen_biomass', 'fluid': 'vein_lumen_fluid', 'fluid_dry': 'vein_lumen_dry',
          'item': 'vein_lumen_item'}


def write(path, data):
    path.write_text(json.dumps(data, indent=2) + '\n')


# --- the blockstate rotations, as vanilla applies them: x first, then y, each turning clockwise looking along
# the positive axis toward the origin ---

VEC = {'up': (0, 1, 0), 'down': (0, -1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0),
       'west': (-1, 0, 0)}
NAME = {v: k for k, v in VEC.items()}
OPPOSITE = {'up': 'down', 'down': 'up', 'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}


def turn(direction, x, y):
    vx, vy, vz = VEC[direction]
    for _ in range(x // 90):  # x = 90 takes up to north
        vy, vz = vz, -vy
    for _ in range(y // 90):  # y = 90 takes north to east
        vx, vz = -vz, vx
    return NAME[(vx, vy, vz)]


FACE_TURN = {'up': (0, 0), 'down': (180, 0), 'north': (90, 0), 'south': (90, 180), 'east': (90, 90),
             'west': (90, 270)}
for _face, (_x, _y) in FACE_TURN.items():
    assert turn('up', _x, _y) == _face, (_face, turn('up', _x, _y))


def apply(name, face):
    x, y = FACE_TURN[face]
    out = {'model': NS + name}
    if x:
        out['x'] = x
    if y:
        out['y'] = y
    return out


def blockstate(vein, lumens):
    """lumens: [(model prefix, extra condition)] — the fluid vein picks its lumen by the sealed property."""
    parts = []
    for face in FACE_TURN:
        x, y = FACE_TURN[face]
        parts.append({'apply': apply('vein_corners', face)})
        in_plane = [turn(arm, x, y) for arm in ARMS]
        for prefix, extra in lumens:
            if face == 'up':
                part = {'when': dict(extra)} if extra else {}
                part['apply'] = apply(prefix + '_center', face)
                parts.append(part)
            else:
                reasons = in_plane + [OPPOSITE[face]]
                parts.append({'when': {'OR': [dict(extra, **{r: 'true'}) for r in reasons]},
                              'apply': apply(prefix + '_center', face)})
        if face != 'up':
            parts.append({'when': {d: 'false' for d in in_plane + [OPPOSITE[face]]},
                          'apply': apply('vein_center_skin', face)})
        for arm, side in zip(ARMS, in_plane):
            for prefix, extra in lumens:
                parts.append({'when': dict(extra, **{side: 'true'}), 'apply': apply(prefix + '_arm_' + arm, face)})
            parts.append({'when': {side: 'false'}, 'apply': apply('vein_arm_skin_' + arm, face)})
    write(STATES / (vein + '.json'), {'multipart': parts})


# --- the item: a cell with four top arms, built by turning the same parts in Python ---

def turn_element(element, x, y):
    def point(p):
        px, py, pz = p[0] - 8, p[1] - 8, p[2] - 8
        for _ in range(x // 90):
            py, pz = pz, -py
        for _ in range(y // 90):
            px, pz = -pz, px
        return [px + 8, py + 8, pz + 8]
    a, b = point(element['from']), point(element['to'])
    out = {'from': [min(a[i], b[i]) for i in range(3)], 'to': [max(a[i], b[i]) for i in range(3)], 'faces': {}}
    for direction, face in element['faces'].items():
        face = dict(face)
        if 'cullface' in face:
            face['cullface'] = turn(face['cullface'], x, y)
        out['faces'][turn(direction, x, y)] = face
    return out


def item(vein, prefix):
    elements = []
    for face, (x, y) in FACE_TURN.items():
        parts = list(CORNERS)
        if face == 'up':
            parts += [floor(MID, MID, False)] + [e for arm in ARMS for e in arm_window(arm)]
        else:
            parts += [skin(MID, MID)] + [e for arm in ARMS for e in arm_skin(arm)]
        elements += [turn_element(e, x, y) for e in parts]
    textures = dict(SHARED, lumen=NS + LUMENS[prefix])
    write(ITEMS / (vein + '.json'), {'parent': 'minecraft:block/block', 'textures': dict(textures, particle=SKIN),
                                     'elements': elements})


# --- the Mouth: a vein cell whose top face is lips around a throat; its store stands on it ---

PINK_LIP = [(96, 34, 52), (140, 56, 74), (184, 88, 104), (222, 130, 140), (246, 178, 178)]
BONE_LIP = [(110, 92, 78), (160, 138, 110), (204, 186, 150), (232, 218, 186), (250, 242, 220)]
THROAT = [(14, 6, 12), (30, 12, 22), (52, 20, 34), (80, 32, 48)]
RIM, RING = 3, 5  # the border is 0..3, the lip ring 3..13 around the throat 5..11


def lip(name, colours, seed, shut):
    rng = random.Random(seed)
    img = Image.new('RGBA', (SIZE, SIZE))
    tau = 2 * math.pi
    for y in range(SIZE):
        for x in range(SIZE):
            u, v = x / SIZE, y / SIZE
            fold = math.sin(tau * 6 * u + 1.7 * math.sin(tau * v)) * 0.5 + 0.5  # puckered folds round the ring
            val = 0.35 + 0.4 * fold * (0.6 + 0.4 * math.sin(tau * v)) + 0.08 * (rng.random() - 0.5)
            if shut and abs(y - 31.5) < 2.5:
                val = 0.05  # the seam where the closed lips meet
            img.putpixel((x, y), ramp(colours, val) + (255,))
    img.save(TEXTURES / (name + '.png'), optimize=True)


def throat(seed):
    rng = random.Random(seed)
    img = Image.new('RGBA', (SIZE, SIZE))
    for y in range(SIZE):
        for x in range(SIZE):
            val = 0.75 - 0.6 * (y / SIZE) + 0.12 * math.sin(x / 3.0) * (1 - y / SIZE) + 0.06 * (rng.random() - 0.5)
            img.putpixel((x, y), ramp(THROAT, val) + (255,))
    img.save(TEXTURES / 'mouth_throat.png', optimize=True)


def quad(lo, hi, y, tex, cull=False):
    face = {'uv': [lo[0], lo[1], hi[0], hi[1]], 'texture': tex}
    if cull:
        face['cullface'] = 'up'
    return {'from': [lo[0], y, lo[1]], 'to': [hi[0], y, hi[1]], 'faces': {'up': face}}


def vwall(axis, at, span, ys, facing, tex):
    """A vertical quad in the plane x = at (axis 'x') or z = at (axis 'z'), spanning {@code span} along the other
    horizontal axis and {@code ys} in height."""
    if axis == 'x':
        frm, to = [at, ys[0], span[0]], [at, ys[1], span[1]]
    else:
        frm, to = [span[0], ys[0], at], [span[1], ys[1], at]
    return {'from': frm, 'to': to,
            'faces': {facing: {'uv': [span[0], 16 - ys[1], span[1], 16 - ys[0]], 'texture': tex}}}


def mouth_top(open_throat):
    """Lips in two steps around a throat with cut corners: the border at 16, the outer lip at 15, the inner lip at 14,
    the throat floor at 10. Closed lips cover the throat at 14."""
    hi = 16 - RIM
    e = []
    for lo_x, hi_x in ((0, RIM), (hi, 16)):  # the four border corners
        for lo_z, hi_z in ((0, RIM), (hi, 16)):
            e.append(quad((lo_x, lo_z), (hi_x, hi_z), 16, '#skin', True))
    for a, b in ((RIM, RING), (16 - RING, hi)):  # border beside each stub, and the outer lip's step there
        e += [quad((a, 0), (b, RIM), 16, '#skin', True), quad((a, hi), (b, 16), 16, '#skin', True),
              quad((0, a), (RIM, b), 16, '#skin', True), quad((hi, a), (16, b), 16, '#skin', True)]
        e += [vwall('z', RIM, (a, b), (15, 16), 'south', '#lip'), vwall('z', hi, (a, b), (15, 16), 'north', '#lip'),
              vwall('x', RIM, (a, b), (15, 16), 'east', '#lip'), vwall('x', hi, (a, b), (15, 16), 'west', '#lip')]
    o, i = RIM + 1, 16 - RIM - 1  # the inner lip ring spans o..i
    e += [quad((RIM, RIM), (hi, o), 15, '#lip'), quad((RIM, i), (hi, hi), 15, '#lip'),
          quad((RIM, o), (o, i), 15, '#lip'), quad((i, o), (hi, i), 15, '#lip')]
    e += [vwall('z', o, (o, i), (14, 15), 'south', '#lip'), vwall('z', i, (o, i), (14, 15), 'north', '#lip'),
          vwall('x', o, (o, i), (14, 15), 'east', '#lip'), vwall('x', i, (o, i), (14, 15), 'west', '#lip')]
    if not open_throat:
        e.append(quad((o, o), (i, i), 14, '#lip'))
        return e
    t0, t1 = RING, 16 - RING  # the throat 5..11, its corners cut by one pixel
    e += [quad((o, o), (i, t0), 14, '#lip'), quad((o, t1), (i, i), 14, '#lip'),
          quad((o, t0), (t0, t1), 14, '#lip'), quad((t1, t0), (i, t1), 14, '#lip')]
    e.append(quad((t0, t0), (t1, t1), 10, '#throat'))
    mid = (t0 + 1, t1 - 1)
    e += [vwall('x', t0, mid, (10, 14), 'east', '#throat'), vwall('x', t1, mid, (10, 14), 'west', '#throat'),
          vwall('z', t0, mid, (10, 14), 'south', '#throat'), vwall('z', t1, mid, (10, 14), 'north', '#throat')]
    for cx in (t0, t1 - 1):
        for cz in (t0, t1 - 1):
            e.append(quad((cx, cz), (cx + 1, cz + 1), 14, '#lip'))
            x_wall = cx + 1 if cx == t0 else cx
            z_wall = cz + 1 if cz == t0 else cz
            e.append(vwall('x', x_wall, (cz, cz + 1), (10, 14), 'east' if cx == t0 else 'west', '#throat'))
            e.append(vwall('z', z_wall, (cx, cx + 1), (10, 14), 'south' if cz == t0 else 'north', '#throat'))
    return e


def mouth_stub(open_):
    """The north stub of the border, between the edge and the lip ring; the blockstate turns it to the other sides."""
    xs = (RING, 16 - RING)
    if not open_:
        return [quad((xs[0], 0), (xs[1], RIM), 16, '#skin', True), vwall('z', RIM, xs, (15, 16), 'south', '#lip')]
    return [quad((xs[0], 0), (xs[1], RIM), 16 - DEPTH, '#lumen'),
            vwall('x', xs[0], (0, RIM), (16 - DEPTH, 16), 'east', '#wall'),
            vwall('x', xs[1], (0, RIM), (16 - DEPTH, 16), 'west', '#wall'),
            vwall('z', RIM, xs, (16 - DEPTH, 15), 'north', '#wall')]


def mouth_blockstate():
    parts = []
    for mode in ('intake', 'output', 'both', 'closed'):
        top = 'mouth_top_' + mode
        parts.append({'when': {'mode': mode}, 'apply': {'model': NS + top}})
    for arm, y in zip(ARMS, (0, 90, 180, 270)):
        for value, name in (('true', 'mouth_stub_open'), ('false', 'mouth_stub_skin')):
            entry = {'model': NS + name}
            if y:
                entry['y'] = y
            parts.append({'when': {arm: value}, 'apply': entry})
    for face in FACE_TURN:
        if face == 'up':
            continue
        x, y = FACE_TURN[face]
        parts.append({'apply': apply('vein_corners', face)})
        in_plane = [turn(arm, x, y) for arm in ARMS]
        reasons = in_plane + [OPPOSITE[face]]
        parts.append({'when': {'OR': [{r: 'true'} for r in reasons]}, 'apply': apply('item_vein_center', face)})
        parts.append({'when': {d: 'false' for d in reasons}, 'apply': apply('vein_center_skin', face)})
        for arm, side in zip(ARMS, in_plane):
            parts.append({'when': {side: 'true'}, 'apply': apply('item_vein_arm_' + arm, face)})
            parts.append({'when': {side: 'false'}, 'apply': apply('vein_arm_skin_' + arm, face)})
    write(STATES / 'mouth.json', {'multipart': parts})


def mouth_models():
    lip('mouth_lip', PINK_LIP, 11, False)
    lip('mouth_lip_bone', BONE_LIP, 12, False)
    lip('mouth_lip_shut', PINK_LIP, 13, True)
    lip('mouth_lip_both', [PINK_LIP[0], PINK_LIP[2], BONE_LIP[2], PINK_LIP[3], BONE_LIP[4]], 15, False)
    throat(14)
    base = dict(SHARED, throat=NS + 'mouth_throat', lumen=NS + LUMENS['item'])
    write(MODELS / 'mouth_top_intake.json', model(dict(base, lip=NS + 'mouth_lip'), mouth_top(True)))
    write(MODELS / 'mouth_top_output.json', model(dict(base, lip=NS + 'mouth_lip_bone'), mouth_top(True)))
    write(MODELS / 'mouth_top_both.json', model(dict(base, lip=NS + 'mouth_lip_both'), mouth_top(True)))
    write(MODELS / 'mouth_top_closed.json', model(dict(base, lip=NS + 'mouth_lip_shut'), mouth_top(False)))
    write(MODELS / 'mouth_stub_open.json', model(dict(base, lip=NS + 'mouth_lip'), mouth_stub(True)))
    write(MODELS / 'mouth_stub_skin.json', model(dict(base, lip=NS + 'mouth_lip'), mouth_stub(False)))
    mouth_blockstate()
    elements = list(mouth_top(True))
    for y in (0, 90, 180, 270):
        elements += [turn_element(e, 0, y) for e in mouth_stub(False)]
    for face, (x, y) in FACE_TURN.items():
        if face != 'up':
            parts = list(CORNERS) + [skin(MID, MID)] + [e for arm in ARMS for e in arm_skin(arm)]
            elements += [turn_element(e, x, y) for e in parts]
    write(ITEMS / 'mouth.json', {'parent': 'minecraft:block/block',
                                 'textures': dict(base, lip=NS + 'mouth_lip', particle=SKIN), 'elements': elements})


def main():
    lumen('vein_lumen_biomass', GREEN, 1, 1.0, True)
    lumen('vein_lumen_fluid', WATER, 2, 1.0, True)
    lumen('vein_lumen_item', MUSCLE, 3, 0.4, True)
    lumen('vein_lumen_dry', DRY, 4, 0.2, False)
    wall(5)
    for old in ('biomass_vein', 'biomass_vein_open', 'fluid_vein', 'fluid_vein_open', 'fluid_vein_unsealed',
                'item_vein', 'item_vein_open', 'vein_cap'):
        (MODELS / (old + '.json')).unlink(missing_ok=True)
    write(MODELS / 'vein_corners.json', model(SHARED, CORNERS))
    write(MODELS / 'vein_center_skin.json', model(SHARED, [skin(MID, MID)]))
    for arm in ARMS:
        write(MODELS / ('vein_arm_skin_' + arm + '.json'), model(SHARED, arm_skin(arm)))
        (MODELS / ('vein_rim_' + arm + '.json')).unlink(missing_ok=True)
    for prefix, texture in LUMENS.items():
        textures = dict(SHARED, lumen=NS + texture)
        write(MODELS / (prefix + '_vein_center.json'), model(textures, [floor(MID, MID, False)]))
        for arm in ARMS:
            write(MODELS / (prefix + '_vein_arm_' + arm + '.json'), model(textures, arm_window(arm)))
    blockstate('biomass_vein', [('biomass_vein', {})])
    blockstate('fluid_vein', [('fluid_vein', {'sealed': 'true'}), ('fluid_dry_vein', {'sealed': 'false'})])
    blockstate('item_vein', [('item_vein', {})])
    item('biomass_vein', 'biomass')
    item('fluid_vein', 'fluid')
    item('item_vein', 'item')
    mouth_models()
    print('veins and mouth: 9 textures, 32 part models, 4 blockstates, 4 items')


if __name__ == '__main__':
    main()
