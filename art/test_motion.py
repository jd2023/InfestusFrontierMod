"""Regression checks for generated motion. Run: python3 -m unittest discover -s art -p 'test_*.py'."""
import unittest
import json
import math
import itertools
from pathlib import Path

import gen_biomass_fluid as fluid

ASSETS = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/infestusfrontier'


def animations(name):
    return json.loads((ASSETS / 'animations/block' / (name + '.animation.json')).read_text())['animations']


def geometry(name):
    return json.loads((ASSETS / 'geo/block' / (name + '.geo.json')).read_text())['minecraft:geometry'][0]


def coplanar_faces(elements):
    """Same-facing surfaces sharing positive area cause depth fighting; abutting edges do not."""
    collisions = []
    for (i, a), (j, b) in itertools.combinations(enumerate(elements), 2):
        if 'rotation' in a or 'rotation' in b:
            continue
        for axis, sides in enumerate((('west', 'east'), ('down', 'up'), ('north', 'south'))):
            for side, end in zip(sides, ('from', 'to')):
                if side not in a['faces'] or side not in b['faces']:
                    continue
                if abs(a[end][axis] - b[end][axis]) > 1e-6:
                    continue
                if all(min(a['to'][k], b['to'][k]) - max(a['from'][k], b['from'][k]) > 1e-6
                       for k in range(3) if k != axis):
                    collisions.append((i, j, side))
    return collisions


class OrganMotionTest(unittest.TestCase):
    def test_all_local_model_and_texture_references_resolve(self):
        def resource(value, folder, suffix):
            if value.startswith('infestusfrontier:'):
                target = ASSETS / folder / (value.split(':', 1)[1] + suffix)
                self.assertTrue(target.is_file(), str(target))

        def visit(node):
            if isinstance(node, dict):
                if 'model' in node:
                    resource(node['model'], 'models', '.json')
                if 'parent' in node:
                    resource(node['parent'], 'models', '.json')
                for value in node.get('textures', {}).values():
                    resource(value, 'textures', '.png')
                for value in node.values():
                    visit(value)
            elif isinstance(node, list):
                for value in node:
                    visit(value)

        for folder in ('blockstates', 'models'):
            for path in (ASSETS / folder).rglob('*.json'):
                visit(json.loads(path.read_text()))

    def test_geo_hierarchy_uvs_and_overlapping_faces(self):
        for path in (ASSETS / 'geo/block').glob('*.geo.json'):
            geo = geometry(path.name.removesuffix('.geo.json'))
            bones = geo['bones']
            names = {b['name'] for b in bones}
            self.assertEqual(len(names), len(bones), path.name)
            elements = []
            for bone in bones:
                if 'parent' in bone:
                    self.assertIn(bone['parent'], names, path.name)
                for cube in bone.get('cubes', []):
                    w, h, d = cube['size']
                    u, v = cube['uv']
                    self.assertGreater(min(w, h, d), 0, path.name)
                    self.assertLessEqual(u + 2 * (w + d), geo['description']['texture_width'], path.name)
                    self.assertLessEqual(v + h + d, geo['description']['texture_height'], path.name)
                    if 'rotation' not in cube:
                        elements.append({'from': cube['origin'], 'to': [a + b for a, b in zip(cube['origin'], cube['size'])],
                                         'faces': ('west', 'east', 'down', 'up', 'north', 'south')})
            self.assertEqual(coplanar_faces(elements), [], path.name)

    def test_storage_parts_and_items_have_no_overlapping_face_planes(self):
        for folder in ('block', 'item'):
            for prefix in ('item_capsule', 'reservoir_cell'):
                for path in (ASSETS / 'models' / folder).glob(prefix + '*.json'):
                    self.assertEqual(coplanar_faces(json.loads(path.read_text()).get('elements', [])), [], str(path))

    def test_attached_parts_inherit_their_support_motion(self):
        for model, child, parent in (
                ('seed_pouch', 'neck', 'sack'), ('seed_pouch', 'rim', 'neck'),
                ('activation_cyst', 'throat', 'body'), ('activation_cyst', 'source', 'body'),
                ('activation_cyst', 'carrier', 'body'), ('biomass_bladder', 'cap', 'membrane'),
                ('harvest_corolla', 'heart', 'stem'), ('bio_furnace', 'ember', 'belly')):
            bones = {b['name']: b for b in geometry(model)['bones']}
            self.assertEqual(bones[child]['parent'], parent, (model, child))

    def test_all_animation_bones_and_loop_endpoints(self):
        for path in (ASSETS / 'animations/block').glob('*.animation.json'):
            name = path.name.removesuffix('.animation.json')
            bones = {b['name'] for b in geometry(name)['bones']}
            for clip, animation in animations(name).items():
                for bone, channels in animation['bones'].items():
                    self.assertIn(bone, bones, (name, clip, bone))
                    for channel, keys in channels.items():
                        values = sorted((float(t), v) for t, v in keys.items())
                        self.assertEqual(values[0][0], 0, (name, clip, bone, channel))
                        self.assertLessEqual(values[-1][0], animation['animation_length'])
                        self.assertEqual(values[0][1], values[-1][1], (name, clip, bone, channel))
                        for t, value in values:
                            self.assertTrue(all(math.isfinite(v) for v in value))
                            if channel == 'scale':
                                self.assertTrue(all(v >= 0 for v in value))
                        if channel == 'rotation':
                            for (t0, a), (t1, b) in zip(values, values[1:]):
                                self.assertLessEqual(max(abs(y - x) / (t1 - t0) for x, y in zip(a, b)), 720)

    def test_compost_has_its_own_silhouette_and_source(self):
        self.assertNotEqual(geometry('compost_gland')['bones'], geometry('bone_loom')['bones'])
        self.assertTrue((ASSETS.parents[4] / 'art/compost_gland.bbmodel').is_file())

    def test_drop_is_invisible_on_both_sides_of_its_reset(self):
        drop = animations('membrane_rack')['working']['bones']['drop']['scale']
        values = sorted((float(t), v) for t, v in drop.items())
        self.assertEqual(values[0][1], [0, 0, 0])
        self.assertEqual(values[-2][1], [0, 0, 0])
        self.assertEqual(values[-1][1], [0, 0, 0])

    def test_pool_never_expands_into_bowl_walls(self):
        for animation in animations('culture_bowl').values():
            for scale in animation['bones']['pool']['scale'].values():
                self.assertEqual(scale[0], 1)
                self.assertEqual(scale[2], 1)

    def test_rack_sheet_stays_between_posts(self):
        for animation in animations('membrane_rack').values():
            for scale in animation['bones']['skin']['scale'].values():
                self.assertLessEqual(scale[0], 1)
                self.assertLessEqual(scale[1], 1)

    def test_empty_bladder_has_no_visible_fluid(self):
        for scale in animations('biomass_bladder')['fill_0']['bones']['fluid']['scale'].values():
            self.assertEqual(scale, [0, 0, 0])

    def test_cradle_working_lobes_clear_the_core_through_their_sweep(self):
        bones = {b['name']: b for b in geometry('awakening_cradle')['bones']}
        left = bones['lobe_l']
        width, height, _ = left['cubes'][0]['size']
        clip = animations('awakening_cradle')['working']['bones']
        for time, angle in clip['lobe_l']['rotation'].items():
            radians = math.radians(abs(angle[2]))
            inner = left['pivot'][0] + width * math.cos(radians) + height * math.sin(radians)
            core_half = bones['core']['cubes'][0]['size'][0] / 2 * clip['core']['scale'][time][0]
            self.assertLess(inner, -core_half)

    def test_petals_hinge_above_the_heart_and_follow_the_stem(self):
        bones = {b['name']: b for b in geometry('harvest_corolla')['bones']}
        heart = bones['heart']['cubes'][0]
        top = heart['origin'][1] + heart['size'][1]
        for name, bone in bones.items():
            if name.startswith('petal_'):
                self.assertEqual(bone['parent'], 'stem')
                self.assertGreaterEqual(bone['pivot'][1], top)

    def test_digestive_rings_do_not_separate_vertically(self):
        for animation in animations('digestive_sac').values():
            for bone in ('belly_low', 'belly_mid', 'belly_high', 'gland'):
                for scale in animation['bones'][bone]['scale'].values():
                    self.assertEqual(scale[1], 1)

    def test_junction_bottom_port_keeps_its_foot_on_the_foundation(self):
        clips = animations('vascular_junction')
        for scale in clips['idle']['bones']['hub']['scale'].values():
            self.assertEqual(scale[1], 1)
        for mode in ('closed', 'intake', 'outlet'):
            for scale in clips['down_' + mode]['bones']['mouth_down']['scale'].values():
                self.assertEqual(scale[1], 1)


class BiomassMotionTest(unittest.TestCase):
    def test_surface_evolves_continuously_including_wrap(self):
        frames = [list(fluid.frame(16, i, 0, 7).getdata()) for i in range(fluid.FRAMES)]
        for i, first in enumerate(frames):
            second = frames[(i + 1) % len(frames)]
            changes = [abs(a[c] - b[c]) for a, b in zip(first, second) for c in range(3)]
            self.assertLess(sum(changes) / len(changes), 2.5, f'frame {i}')
            self.assertLess(max(changes), 20, f'frame {i}')


if __name__ == '__main__':
    unittest.main()
