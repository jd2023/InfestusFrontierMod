"""Checks for the infected timber models and atlas. Run: python3 -m unittest discover -s art -p 'test_*.py'."""
import io
import json
import re
import unittest
from pathlib import Path

from PIL import Image

REPO = Path(__file__).resolve().parents[1]
ASSETS = REPO / 'src/main/resources/assets/infestusfrontier'

import gen_infected_timber as gen  # noqa: E402  the generator's own functions, not its files
from test_motion import coplanar_faces  # noqa: E402

LOGS = list(gen.LOG_VARIANTS)
LEAVES = list(gen.LEAF_VARIANTS)


def model(name):
    return json.loads((ASSETS / 'models/block' / (name + '.json')).read_text())


def state(name):
    return json.loads((ASSETS / 'blockstates' / (name + '.json')).read_text())


def touching(a, b):
    """Two boxes share volume or a face of positive area; a shared edge or corner is no joint."""
    overlap = [min(a[1][k], b[1][k]) - max(a[0][k], b[0][k]) for k in range(3)]
    return all(o >= 0 for o in overlap) and sum(o > 0 for o in overlap) >= 2


def turned(boxes, quarters):
    """Boxes turned about the block's vertical axis, a quarter at a time as the blockstate's y rotation does."""
    for _ in range(quarters):
        boxes = [((16 - t[2], f[1], f[0]), (16 - f[2], t[1], t[0])) for f, t in boxes]
    return boxes


def shifted(boxes, axis, by):
    return [(tuple(c + by * (i == axis) for i, c in enumerate(f)), tuple(c + by * (i == axis) for i, c in enumerate(t)))
            for f, t in boxes]


def neighbours():
    """Place the shipped blockstate variants beside one another along each axis."""
    variants = []
    for entry in state('blighted_leaves')['variants']['']:
        name = entry['model'].split('/')[-1]
        wood = [(e['from'], e['to']) for e in model(name)['elements']
                if all(e['to'][axis] > e['from'][axis] for axis in range(3))]
        variants.append((name, entry.get('y', 0), turned(wood, entry.get('y', 0) // 90)))
    for name_a, turn_a, first in variants:
        for name_b, turn_b, second in variants:
            for axis in range(3):
                yield first, shifted(second, axis, 16), (name_a, turn_a), (name_b, turn_b), axis


def longest_run(boxes):
    """The longest straight line of texels the boxes fill, along any axis."""
    cells = {(x, y, z) for f, t in boxes for x in range(f[0], t[0]) for y in range(f[1], t[1]) for z in range(f[2], t[2])}
    best = 0
    for x, y, z in cells:
        for axis in range(3):
            before = (x - (axis == 0), y - (axis == 1), z - (axis == 2))
            if before in cells:
                continue
            length, cell = 0, (x, y, z)
            while cell in cells:
                length += 1
                cell = (cell[0] + (axis == 0), cell[1] + (axis == 1), cell[2] + (axis == 2))
            best = max(best, length)
    return best


def components(boxes):
    """The face-connected groups of boxes, as lists of box indexes."""
    left, groups = set(range(len(boxes))), []
    while left:
        group, frontier = set(), [left.pop()]
        while frontier:
            i = frontier.pop()
            group.add(i)
            joined = [j for j in left if touching(boxes[i], boxes[j])]
            left -= set(joined)
            frontier += joined
        groups.append(sorted(group))
    return groups


def scrap_attached(scrap, boxes):
    """A scrap's top row lies inside a box that its plane and its width both cross."""
    name, axis, at, left, top = scrap
    width = gen.SCRAPS[name][1][2]
    a = 2 if axis == 'x' else 0  # the plane's axis along the scrap's width
    n = 0 if axis == 'x' else 2
    return any(f[1] <= top - 1 < t[1] and f[n] <= at <= t[n] and f[a] < left + width and t[a] > left
               for f, t in boxes)


def png_bytes(image):
    out = io.BytesIO()
    image.save(out, 'PNG')
    return out.getvalue()


class InfectedTimberTest(unittest.TestCase):
    def test_a_rerun_reproduces_every_shipped_file(self):
        self.assertEqual(png_bytes(gen.atlas()), (ASSETS / 'textures/block/infected_timber.png').read_bytes())
        self.assertEqual(png_bytes(gen.planks()), (ASSETS / 'textures/block/infected_planks.png').read_bytes())
        self.assertEqual(png_bytes(gen.leaves_sprite()), (ASSETS / 'textures/item/blighted_leaves.png').read_bytes())
        for name, spec in gen.LOG_VARIANTS.items():
            self.assertEqual(gen.log_model(spec), model(name), name)
        for name, spec in gen.LEAF_VARIANTS.items():
            self.assertEqual(gen.leaf_model(spec), model(name), name)
        for name in ('infected_log', 'blighted_leaves'):
            self.assertTrue(state(name)['variants'], name)
        for old in ('infected_log.png', 'infected_log_top.png', 'blighted_leaves.png'):
            self.assertFalse((ASSETS / 'textures/block' / old).exists(), 'the 64x64 tile is gone: ' + old)
        self.assertFalse((ASSETS / 'models/block/infected_log_horizontal.json').exists())

    def test_one_small_atlas_in_a_restrained_palette(self):
        atlas = Image.open(ASSETS / 'textures/block/infected_timber.png').convert('RGBA')
        self.assertEqual(atlas.size, (64, 64))
        colours = {rgba[:3] for _, rgba in atlas.getcolors(4096) if rgba[3]}
        self.assertLessEqual(len(colours), 24, 'palette')
        planks = Image.open(ASSETS / 'textures/block/infected_planks.png').convert('RGBA')
        self.assertEqual(planks.size, (16, 16))
        self.assertEqual(planks.getchannel('A').getextrema(), (255, 255), 'the planks are opaque')
        with Image.open(ASSETS / 'textures/book/infected_timber.png') as icon:
            self.assertEqual(icon.size, (16, 16))
        for name in LOGS + LEAVES:
            refs = set(model(name)['textures'].values())
            self.assertEqual(refs, {'infestusfrontier:block/infected_timber'}, name + ' draws from the one atlas')

    def test_every_face_shows_one_texel_per_pixel_inside_the_block(self):
        for name in LOGS + LEAVES:
            for element in model(name)['elements']:
                f, t = element['from'], element['to']
                for k in range(3):
                    self.assertTrue(0 <= f[k] <= t[k] <= 16, f'{name} stays in the block: {f} {t}')
                for direction, face in element['faces'].items():
                    u0, v0, u1, v1 = face['uv']
                    axes = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1),
                            'up': (0, 2), 'down': (0, 2)}[direction]
                    size = [t[a] - f[a] for a in axes]
                    texels = [abs(u1 - u0) * 4, abs(v1 - v0) * 4]
                    self.assertEqual(size, texels, f'{name} {direction} {f} {t}')
                    self.assertNotIn('tintindex', face, name + ' is drawn untinted')

    def test_no_two_faces_fight_for_depth(self):
        for name in LOGS + LEAVES:
            self.assertEqual(coplanar_faces(model(name)['elements']), [], name)

    def test_every_log_meets_every_other_on_one_outline(self):
        """Caps and faces flush with the block are the same in every model, under every quarter turn."""
        def boundary(name):
            found = set()
            for element in model(name)['elements']:
                for direction, face in element['faces'].items():
                    if 'cullface' in face:
                        found.add((direction, tuple(element['from']), tuple(element['to'])))
            return found

        first = boundary(LOGS[0])
        self.assertTrue(first)
        for name in LOGS[1:]:
            self.assertEqual(boundary(name), first, name)
        grid, _ = gen.section()
        for x in range(16):
            for z in range(16):
                self.assertEqual(grid[x][z], grid[15 - z][x], 'the section is the same after a quarter turn')
        variants = state('infected_log')['variants']
        self.assertEqual({tuple(sorted(v)) for v in variants['axis=y']}, {('model', 'weight'), ('model', 'weight', 'y')})
        self.assertEqual({(v.get('x'), v.get('y', 0)) for v in variants['axis=z']}, {(90, 0), (270, 180)})
        self.assertEqual({(v.get('x'), v.get('y')) for v in variants['axis=x']}, {(90, 90), (270, 270)})
        for axis in variants.values():
            self.assertEqual({v['model'].split('/')[-1] for v in axis}, set(LOGS), 'every model in every axis')

    def test_the_block_occludes_the_section_the_generator_cuts(self):
        java = (REPO / 'src/main/java/org/jd/infestusfrontier/ecology/InfectedLogBlock.java').read_text()
        found = re.search(r'PROFILE = \{([^}]*)\}', java)
        self.assertEqual(tuple(int(v) for v in found.group(1).split(',')), gen.PROFILE)

    def test_no_twig_is_a_straight_rail_and_the_membrane_is_sparse(self):
        """Wood bends: no straight run of texels is longer than 7 in a model or than 12 across any two neighbouring
        blocks under any turns. At most one scrap hangs in a model; some models are bare and the scraps differ."""
        for name, spec in gen.LEAF_VARIANTS.items():
            self.assertLessEqual(longest_run(spec['twigs']), 7, f'{name} has a straight rail')
            self.assertLessEqual(len(spec['scraps']), 1, name)
        for a, b, k, m, axis in neighbours():
            self.assertLessEqual(longest_run(list(a) + list(b)), 12, f'{k} {m} {axis}')
        self.assertTrue(any(not spec['scraps'] for spec in gen.LEAF_VARIANTS.values()), 'one crown is bare')
        self.assertGreaterEqual(len({scrap[0] for spec in gen.LEAF_VARIANTS.values() for scrap in spec['scraps']}), 4)

    def test_every_crown_is_one_connected_cluster_with_its_scrap_attached(self):
        for name, spec in gen.LEAF_VARIANTS.items():
            self.assertEqual(len(components(spec['twigs'])), 1, name + ' has floating pieces')
            for scrap in spec['scraps']:
                self.assertTrue(scrap_attached(scrap, spec['twigs']), f'{name} scrap {scrap} hangs from nothing')

    def test_the_checks_reject_floating_twigs_and_a_scrap_hanging_from_nothing(self):
        """The first draft's bare crown: four clusters strictly inside the block."""
        floating = (((7, 10, 1), (8, 11, 6)), ((7, 9, 5), (8, 10, 10)), ((7, 7, 9), (8, 9, 10)),
                    ((2, 3, 10), (7, 4, 11)), ((6, 4, 10), (7, 6, 11)),
                    ((10, 2, 5), (13, 3, 6)), ((12, 3, 5), (13, 5, 6)),
                    ((11, 12, 11), (14, 13, 12)), ((13, 13, 11), (14, 15, 12)))
        self.assertGreater(len(components(floating)), 1)
        self.assertFalse(scrap_attached(('tab', 'z', 7.5, 12, 3), ((( 0, 7, 7), (4, 9, 9)),)))
        self.assertEqual(len(components((((0, 0, 0), (2, 2, 2)), ((2, 2, 2), (4, 4, 4))))), 2, 'a shared corner is not a joint')

    def test_wood_meets_the_block_face_only_in_a_centre_pad_on_each_of_the_six_faces(self):
        """The pad is the same 2x2 square, one texel deep and centred on the face, which no quarter turn moves."""
        for name, spec in gen.LEAF_VARIANTS.items():
            pads = set()
            for f, t in spec['twigs']:
                for axis in range(3):
                    for edge in (0, 16):
                        if (f[axis] if edge == 0 else t[axis]) == edge:
                            pads.add((axis, edge))
                            self.assertEqual(
                                [(f[o], t[o]) for o in range(3) if o != axis], [(7, 9), (7, 9)], f'{name} {f} {t}')
                            self.assertEqual(t[axis] - f[axis], 1, f'{name} {f} {t} is a pad one texel deep')
            self.assertEqual(len(pads), 6, name + ' meets every neighbour')

    def test_every_pair_of_neighbouring_crowns_joins_under_every_turn(self):
        """Models are picked and turned about the vertical for each block on its own. Each model is moved to the
        cell beside another along x, y and z, and a twig of one meets a twig of the other across the boundary."""
        count = 0
        for a, b, k, m, axis in neighbours():
            self.assertTrue(any(touching(p, q) for p in a for q in b), f'{k} {m} {axis}: the crowns do not meet')
            count += 1
        self.assertEqual(count, len(LEAVES) ** 2 * 16 * 3)

    def test_the_join_check_rejects_the_first_drafts_hooks_pair(self):
        """Two unrotated hooks of the first draft, east and west: the east block reaches its west boundary nowhere."""
        hooks = (((12, 7, 7), (16, 9, 9)), ((8, 6, 7), (12, 8, 9)), ((8, 6, 9), (10, 8, 13)), ((7, 7, 12), (9, 9, 16)),
                 ((9, 8, 8), (10, 12, 9)), ((9, 11, 5), (10, 12, 8)))
        east = shifted(hooks, 0, 16)
        self.assertFalse(any(touching(p, q) for p in hooks for q in east))

    def test_the_scraps_fit_their_cells_without_touching(self):
        taken = {}
        for name, (cell, (left, top, width, height), hangs) in gen.SCRAPS.items():
            self.assertLessEqual(width, 8, name)
            self.assertEqual(len(hangs), width, name)
            self.assertTrue(left + width <= 16 and top + height <= 16, name)
            for x in range(left, left + width):
                for y in range(top, top + height):
                    self.assertNotIn((cell, x, y), taken, f'{name} overlaps {taken.get((cell, x, y))}')
                    taken[(cell, x, y)] = name
        for cell, tile in gen.membranes().items():
            alpha = tile.getchannel('A')
            for x in range(16):
                for y in range(16):
                    scrap = taken.get((cell, x, y))
                    if alpha.getpixel((x, y)) and not scrap:
                        self.fail(f'membrane texel outside every scrap: {cell} {x} {y}')

    def test_bone_is_a_modest_exposure_and_not_a_bright_rail(self):
        """The ribs are bone in segments broken by bark, in a muted tone, never lighter than the cleft's maroon or the
        membrane by a wide margin."""
        atlas = Image.open(ASSETS / 'textures/block/infected_timber.png').convert('RGBA')
        for cell in gen.RIB_CELLS.values():
            tile = atlas.crop((cell[0] * 16, cell[1] * 16, cell[0] * 16 + 16, cell[1] * 16 + 16))
            bone = {gen.IVORY, gen.IVORY_SHADOW, gen.IVORY_LIGHT}
            column = [tile.getpixel((5, y))[:3] in bone for y in range(16)]
            self.assertLessEqual(sum(column), 11, 'bone shows on at most 11 of 16 rows')
            self.assertGreaterEqual(sum(1 for a, b in zip(column, column[1:] + column[:1]) if a != b), 4,
                                    'the bone is broken into at least two segments, wrapped')
        brightest = max(sum(c) for c in (gen.IVORY, gen.IVORY_LIGHT))
        self.assertLessEqual(brightest, 3 * 175, 'bone is muted')

    def test_the_crown_is_mostly_open(self):
        """Seen along any axis, a leaf model covers well under half of the block's face, and nothing is a full face."""
        for name in LEAVES:
            elements = model(name)['elements']
            for axis, (a, b) in ((0, (2, 1)), (1, (0, 2)), (2, (0, 1))):
                covered = set()
                for element in elements:
                    f, t = element['from'], element['to']
                    self.assertFalse(t[a] - f[a] == 16 and t[b] - f[b] == 16, f'{name}: a full face')
                    for i in range(int(f[a]), int(t[a]) if t[a] > f[a] else int(f[a]) + 1):
                        for j in range(int(f[b]), int(t[b]) if t[b] > f[b] else int(f[b]) + 1):
                            covered.add((i, j))
                self.assertLess(len(covered), 0.5 * 256, f'{name} seen along axis {axis}: {len(covered)} of 256')
            self.assertEqual(model(name)['render_type'], 'minecraft:cutout_mipped', name)
        self.assertEqual({v['model'].split('/')[-1] for v in state('blighted_leaves')['variants']['']}, set(LEAVES))


if __name__ == '__main__':
    unittest.main()
