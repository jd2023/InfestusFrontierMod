"""Capsule Stem: the stem the Item Capsule pods grow on. A squared maroon vascular column with a dark transport
groove down the middle of every face and two one-pixel ivory strips up its north and south faces; a stepped root foot at the bottom, a bud at a trunk's top, an elbow where the trunk forks, and a bone collar
round any cell that carries pods.

    python3 art/gen_capsule_stem.py

Static part models by block state: `part` (root, trunk, bud, branch) and `collar`; a branch faces the way it grew,
its horizontal piece reaching back into the parent trunk's block. Parts stacked in a column share no face plane
(`test_stem_parts_share_no_face_planes`).
"""
import json
import random

from PIL import Image

from gen_reservoir import ITEMS, MODELS, NS, STATES, TEXTURES, box

SIDES = ['north', 'south', 'west', 'east']
ALL = SIDES + ['up', 'down']
NAMES = ('stem', 'root', 'bone')
TEX = {name: NS + 'capsule_stem_' + name for name in NAMES}
TEX['particle'] = TEX['stem']

STEM = [(38, 12, 18), (66, 22, 32), (92, 36, 44), (114, 50, 56)]
ROOT = [(44, 20, 20), (70, 32, 30), (98, 46, 42), (124, 62, 54)]
BONE = [(172, 154, 124), (208, 192, 160), (232, 220, 196), (246, 240, 226)]
# two ivory reinforcing strips, one pixel wide, at the edges of the north and south faces, as (x0, z0), (x1, z1)
STRIPS = (((4, 3), (5, 4)), ((11, 3), (12, 4)), ((4, 12), (5, 13)), ((11, 12), (12, 13)))


def field(seed, cells):
    """Smooth value noise, 0..1, wrapping at 16 pixels."""
    rng = random.Random(seed)
    grid = [[rng.random() for _ in range(cells)] for _ in range(cells)]

    def at(x, y):
        fx, fy = x / 16 * cells, y / 16 * cells
        ix, iy = int(fx) % cells, int(fy) % cells
        tx, ty = fx - int(fx), fy - int(fy)
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a, b = grid[iy][ix], grid[iy][(ix + 1) % cells]
        c, d = grid[(iy + 1) % cells][ix], grid[(iy + 1) % cells][(ix + 1) % cells]
        return (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty
    return at


def mottle(seed, palette, cuts=(0.3, 0.5, 0.75)):
    coarse, fine = field(seed, 4), field(seed + 1, 8)
    img = Image.new('RGB', (16, 16))
    for y in range(16):
        for x in range(16):
            v = 0.6 * coarse(x, y) + 0.4 * fine(x, y)
            img.putpixel((x, y), palette[sum(v >= c for c in cuts)])
    return img


def textures():
    # the stem: maroon with the transport groove down the middle of every face
    stem = mottle(21, STEM)
    for y in range(16):
        for x in range(16):
            if x in (7, 8):
                stem.putpixel((x, y), STEM[0])
            elif x in (6, 9):
                stem.putpixel((x, y), STEM[1] if (x + y) % 3 else STEM[0])
    stem.save(TEXTURES / 'capsule_stem_stem.png', optimize=True)
    # the root foot: dark brown-maroon with ivory flecks where bone shows through
    root = mottle(31, ROOT)
    for y in range(16):
        for x in range(16):
            if (x * 5 + y * 3) % 17 == 0:
                root.putpixel((x, y), BONE[1])
    root.save(TEXTURES / 'capsule_stem_root.png', optimize=True)
    # bone: ivory with faint growth lines
    bone = mottle(41, BONE, cuts=(0.25, 0.55, 0.85))
    for y in range(16):
        for x in range(16):
            if (x + 2 * y) % 7 == 0:
                bone.putpixel((x, y), BONE[1])
    bone.save(TEXTURES / 'capsule_stem_bone.png', optimize=True)


def column(y0, y1):
    """Closed core and strips: every cell stays solid when exposed by growth or a missing neighbor."""
    faces = ALL
    out = [box((4, y0, 4), (12, y1, 12), 'stem', faces)]
    for (x0, z0), (x1, z1) in STRIPS:
        out.append(box((x0, y0, z0), (x1, y1, z1), 'bone', faces))
    return out


def root():
    return column(4, 16) + [
        box((0, 0, 0), (16, 2, 16), 'root', ALL),
        box((2, 2, 2), (14, 4, 14), 'root', ALL),
    ]


def trunk():
    return column(0, 16)


def bud():
    return column(0, 12) + [
        box((5, 12, 5), (11, 14, 11), 'stem', ALL),
        box((6, 14, 6), (10, 16, 10), 'bone', ALL),
    ]


def branch():
    """Facing north: grown out of a parent to the south, whose core it reaches back into, then rising."""
    out = column(4, 16)
    out.append(box((5, 4, 12), (11, 12, 20), 'stem', ALL))
    for (x0, z0), (x1, z1) in (((4, 13), (5, 20)), ((11, 13), (12, 20))):
        out.append(box((x0, 12, z0), (x1, 13, z1), 'bone', ALL))
        out.append(box((x0, 3, z0), (x1, 4, z1), 'bone', ALL))
    out.append(box((3, 5, 13), (13, 7, 19), 'bone', ALL))
    return out


def collar():
    return [box((2.5, 7, 2.5), (13.5, 9, 13.5), 'bone', ALL)]


def parts():
    return {'root': root(), 'trunk': trunk(), 'bud': bud(), 'branch': branch(), 'collar': collar()}


def write_parts(models):
    for old in MODELS.glob('capsule_stem_*.json'):
        old.unlink()
    for name, elements in models.items():
        model = {'parent': 'minecraft:block/block', 'textures': TEX, 'elements': elements}
        (MODELS / f'capsule_stem_{name}.json').write_text(json.dumps(model, indent=2) + '\n')


def write_blockstate():
    def apply(name, y=0):
        out = {'model': NS + 'capsule_stem_' + name}
        if y:
            out['y'] = y
        return out
    states = []
    for part in ('root', 'trunk', 'bud'):
        states.append({'when': {'part': part}, 'apply': apply(part)})
    for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        states.append({'when': {'part': 'branch', 'facing': facing}, 'apply': apply('branch', y)})
    states.append({'when': {'collar': 'true'}, 'apply': apply('collar')})
    (STATES / 'capsule_stem.json').write_text(json.dumps({'multipart': states}, indent=2) + '\n')


def write_item():
    model = {
        'parent': NS + 'capsule_stem_root',
        'display': {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, 1.6, 0], 'scale': [0.625, 0.625, 0.625]},
        },
    }
    (ITEMS / 'capsule_stem.json').write_text(json.dumps(model, indent=2) + '\n')


def main():
    textures()
    models = parts()
    write_parts(models)
    write_blockstate()
    write_item()
    print('capsule_stem: %d part models' % len(models))


if __name__ == '__main__':
    main()
