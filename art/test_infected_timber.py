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
