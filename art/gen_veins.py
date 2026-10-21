"""The vein cells: lumen and wall textures, the part models and the three blockstates.

    python3 art/gen_veins.py

A vein cell is a shell of mature substrate skin. Every face is a 3x3 grid of tiles: four corners, four arms and a
centre. Where the lumen runs along a face, its tiles are a window recessed 2 px, floored with the lumen and walled.
A face shows an arm window toward each linked neighbour in its plane, and its centre window whenever any arm is open
or the lumen passes straight through toward it; the top face always shows its centre. Each part is modelled once in
the top-face frame and rotated onto the six faces by the blockstate. Where a face touches a linked neighbour, the
2 px rim of each arm tile stays open: it is the mouth of the perpendicular face's window, so a groove continues from
cell to cell without a wall across it.
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
ARM_RIM = {'north': (MID, (0, 2)), 'south': (MID, (14, 16)), 'west': ((0, 2), MID), 'east': ((14, 16), MID)}
ARM_INNER = {'north': (MID, (2, 5)), 'south': (MID, (11, 14)), 'west': ((2, 5), MID), 'east': ((11, 14), MID)}


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
    face = {'uv': [xs[0], zs[0], xs[1], zs[1]], 'texture': '#lumen', 'cullface': 'up'}
    if along_x:
        face = {'uv': [zs[0], xs[0], zs[1], xs[1]], 'texture': '#lumen', 'cullface': 'up', 'rotation': 90}
    return {'from': [xs[0], 16 - DEPTH, zs[0]], 'to': [xs[1], 16 - DEPTH, zs[1]], 'faces': {'up': face}}


def wall_x(x, zs, facing):
    """A window wall in the plane x = const, facing east or west into the window."""
    return {'from': [x, 16 - DEPTH, zs[0]], 'to': [x, 16, zs[1]],
            'faces': {facing: {'uv': [zs[0], 0, zs[1], DEPTH], 'texture': '#wall', 'cullface': 'up'}}}


def wall_z(z, xs, facing):
    """A window wall in the plane z = const, facing north or south into the window."""
    return {'from': [xs[0], 16 - DEPTH, z], 'to': [xs[1], 16, z],
            'faces': {facing: {'uv': [xs[0], 0, xs[1], DEPTH], 'texture': '#wall', 'cullface': 'up'}}}


def arm_window(arm):
    xs, zs = ARM_TILE[arm]
    if arm in ('north', 'south'):
        return [floor(xs, zs, False), wall_x(MID[0], zs, 'east'), wall_x(MID[1], zs, 'west')]
    return [floor(xs, zs, True), wall_z(MID[0], xs, 'south'), wall_z(MID[1], xs, 'north')]


def arm_skin(arm):
    """The closed arm's inner skin and the centre window's wall on that side; the wall hides under skin when the
    centre is closed too."""
    xs, zs = ARM_INNER[arm]
    edge = {'north': wall_z(MID[0], MID, 'south'), 'south': wall_z(MID[1], MID, 'north'),
            'west': wall_x(MID[0], MID, 'east'), 'east': wall_x(MID[1], MID, 'west')}[arm]
    return [skin(xs, zs), edge]


def rim(arm):
    xs, zs = ARM_RIM[arm]
    return [skin(xs, zs)]


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


def apply(name, face, uvlock=False):
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
            parts.append({'when': {side: 'false', face: 'false'}, 'apply': apply('vein_rim_' + arm, face)})
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
            parts += [skin(MID, MID)] + [e for arm in ARMS for e in arm_skin(arm) + rim(arm)]
        elements += [turn_element(e, x, y) for e in parts]
    textures = dict(SHARED, lumen=NS + LUMENS[prefix])
    write(ITEMS / (vein + '.json'), {'parent': 'minecraft:block/block', 'textures': dict(textures, particle=SKIN),
                                     'elements': elements})


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
        write(MODELS / ('vein_rim_' + arm + '.json'), model(SHARED, rim(arm)))
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
    print('veins: 4 lumens, 1 wall, 30 part models, 3 blockstates, 3 items')


if __name__ == '__main__':
    main()
