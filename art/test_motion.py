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

    def test_every_organ_model_stands_in_its_block(self):
        """A block's model spans y 0..16 from its floor; nothing hangs below the block (tall organs rise past it)."""
        for path in sorted((ASSETS / 'geo/block').glob('*.geo.json')):
            for bone in geometry(path.name.removesuffix('.geo.json'))['bones']:
                for cube in bone.get('cubes', []):
                    self.assertGreaterEqual(cube['origin'][1], -0.5, path.name)
                    self.assertLessEqual(cube['origin'][1] + cube['size'][1], 32, path.name)

    # Organs that reach past their block's side faces on purpose, with the furthest point they may reach: the
    # Proboscis' rostrum and the Sentry's lash work on the blocks round them behind widened culling boxes, a pod hugs
    # its stem, and the Bio-Furnace's buttresses and the Cyst's chambers stand a little proud (older models).
    SIDEWAYS_REACH = {'planting_proboscis': 16.5, 'spine_sentry': 12.75, 'item_capsule': 11.5, 'bio_furnace': 8.6,
                      'activation_cyst': 8.1}

    def test_every_organ_model_stays_inside_its_block_sideways(self):
        """No cube reaches past the block's side faces (x and z within -8..8) in any keyframe of any clip, unless the
        organ is listed above with the reach its design needs; a sliver of bone emerging from a neighbour's face is a
        defect a player sees."""
        import geo_audit
        import numpy as np
        for path in sorted((ASSETS / 'geo/block').glob('*.geo.json')):
            model = path.name.removesuffix('.geo.json')
            limit = self.SIDEWAYS_REACH.get(model, 8.0)
            geo = geometry(model)
            anims = animations(model)
            for clip, t in geo_audit.poses(anims):
                for name, m, lo, hi in geo_audit.world_cubes(geo, anims[clip] if clip else None, t):
                    corners = np.array([[x, y, z, 1] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])])
                    world = (m @ corners.T).T[:, :3]
                    reach = max(abs(world[:, 0]).max(), abs(world[:, 2]).max())
                    self.assertLessEqual(reach, limit + 1e-6, (model, name, clip, t))

    # Sacs whose body swells at its waist: model -> (body bones, foot height, waist height, shoulder height)
    WAISTS = {'seed_pouch': (('sack', 'lobe_w', 'lobe_e'), 2.5, 5.5, 9.0),
              'compost_gland': (('core', 'flank_w', 'flank_e', 'flank_n', 'flank_s'), 2.5, 5.75, 9.0)}

    def test_sacs_swell_at_the_waist(self):
        """At rest the body reaches further from the centre at its waist than at its foot or its shoulders: plates
        tilted the wrong way (GeckoLib mirrors x and negates rotations about x) make an hourglass instead."""
        import geo_audit
        import numpy as np
        for model, (bones, foot, waist, shoulder) in self.WAISTS.items():
            points = []
            for name, m, lo, hi in geo_audit.world_cubes(geometry(model), None, 0):
                if name.split('[')[0] in bones:
                    grid = np.stack(np.meshgrid(*[np.linspace(lo[i], hi[i], 9) for i in range(3)]), -1).reshape(-1, 3)
                    points.append((m @ np.c_[grid, np.ones(len(grid))].T).T[:, :3])
            points = np.concatenate(points)

            def reach(y):
                band = points[abs(points[:, 1] - y) < 0.25]
                return max(abs(band[:, 0]).max(), abs(band[:, 2]).max())

            self.assertGreater(reach(waist), reach(foot) + 0.2, model)
            self.assertGreater(reach(waist), reach(shoulder) + 0.2, model)

    def test_bladder_holds_its_feed_inside_and_its_ribs_outside(self):
        """Walking out from the bladder's axis past a point of the biomass at any level, one meets the sac (membrane, a
        rib or the collar); walking out past the outer face of a bone rib, one meets no membrane; with the sac at either
        extreme of its breath. So the level stays inside and no bone is seen only through the membrane."""
        import geo_audit
        import numpy as np
        geo, anims = geometry('biomass_bladder'), animations('biomass_bladder')

        def covered(point, membrane):
            out = np.array([point[0], 0, point[2]])
            out = out / np.linalg.norm(out)
            return any(geo_audit.inside(point + out * s, membrane) for s in np.arange(0.0, 8.0, 0.05))

        for t in (0.0, 2.0, 6.0):
            cubes = geo_audit.world_cubes(geo, anims['idle'], t)
            membrane = [c for c in cubes if c[0].startswith('membrane')]
            sac = [c for c in cubes if c[0].startswith(('membrane', 'ribs', 'cap'))]
            for _, m, lo, hi in (c for c in cubes if c[0].startswith('ribs')):
                for f in (0.2, 0.5, 0.8):
                    # the corner of the rib that faces away from the axis, a little way along its length
                    corners = [(m @ np.array([x, lo[1] + f * (hi[1] - lo[1]), z, 1]))[:3] for x in (lo[0], hi[0]) for z in (lo[2], hi[2])]
                    outer = max(corners, key=lambda c: np.hypot(c[0], c[2]))
                    self.assertFalse(covered(outer + 0.01 * np.array([outer[0], 0, outer[2]]), membrane), ('rib behind membrane', t, outer))
            for level in range(1, 5):
                _, m, lo, hi = [c for c in geo_audit.world_cubes(geo, anims['fill_%d' % level], 0) if c[0].startswith('fluid')][0]
                for corner in ((x, y, z) for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])):
                    point = (m @ np.array([*corner, 1]))[:3]
                    if point[1] > 3.25:  # above the cup's floor
                        self.assertTrue(covered(point, sac), ('biomass past the sac', t, level, point))

    def test_bone_loom_shuttle_keeps_clear_of_the_ribs(self):
        """The shuttle's stroke and lift never put a corner of the slug or needle inside a rib, nor a rib's corner inside
        them, in any keyframe of the working clip; a rib tip sinking into the glowing slug shows at close range."""
        import geo_audit
        import numpy as np
        geo = geometry('bone_loom')
        anim = animations('bone_loom')['working']
        for _, t in geo_audit.poses({'working': anim}, every=True):
            cubes = geo_audit.world_cubes(geo, anim, t)
            shuttle = [c for c in cubes if c[0].startswith('shuttle')]
            ribs = [c for c in cubes if c[0].startswith('rib_')]
            for group, others in ((shuttle, ribs), (ribs, shuttle)):
                for name, m, lo, hi in group:
                    for corner in ((x, y, z) for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])):
                        point = (m @ np.array([*corner, 1]))[:3]
                        self.assertFalse(geo_audit.inside(point, others), (name, t))

    def test_no_geckolib_model_has_faces_fighting_for_depth(self):
        """Parallel faces closer than a tenth of a pixel flicker; organ models draw both sides of every face."""
        import geo_audit
        for path in sorted((ASSETS / 'geo/block').glob('*.geo.json')):
            model = path.name.removesuffix('.geo.json')
            self.assertEqual(geo_audit.audit(model), [], model)

    def test_storage_parts_and_items_have_no_overlapping_face_planes(self):
        for folder in ('block', 'item'):
            for prefix in ('capsule_stem', 'reservoir_cell'):
                for path in (ASSETS / 'models' / folder).glob(prefix + '*.json'):
                    self.assertEqual(coplanar_faces(json.loads(path.read_text()).get('elements', [])), [], str(path))

    def test_stem_parts_share_no_face_planes(self):
        """A stem's parts stacked as they grow, a branch beside them and a collar on a cell, never share a face plane."""
        parts = json.loads((ASSETS / 'blockstates/capsule_stem.json').read_text())['multipart']
        column = [((0, 0, 0), {'part': 'root', 'collar': 'false', 'facing': 'north'}),
                  ((0, 1, 0), {'part': 'trunk', 'collar': 'true', 'facing': 'north'}),
                  ((0, 2, 0), {'part': 'trunk', 'collar': 'false', 'facing': 'north'}),
                  ((1, 2, 0), {'part': 'branch', 'collar': 'false', 'facing': 'north'}),
                  ((1, 3, 0), {'part': 'bud', 'collar': 'true', 'facing': 'north'}),
                  ((0, 3, 0), {'part': 'bud', 'collar': 'false', 'facing': 'north'})]
        elements = []
        for cell, state in column:
            for part in parts:
                if not all(state[k] in v.split('|') for k, v in part.get('when', {}).items()):
                    continue
                name = part['apply']['model'].split(':')[1]
                for element in json.loads((ASSETS / 'models' / (name + '.json')).read_text()).get('elements', []):
                    element['from'] = [a + 16 * c for a, c in zip(element['from'], cell)]
                    element['to'] = [a + 16 * c for a, c in zip(element['to'], cell)]
                    elements.append(element)
        self.assertEqual(coplanar_faces(elements), [])

    def test_attached_parts_inherit_their_support_motion(self):
        for model, child, parent in (
                ('seed_pouch', 'neck', 'sack'), ('seed_pouch', 'rim', 'neck'),
                ('activation_cyst', 'throat', 'body'), ('activation_cyst', 'source', 'body'),
                ('activation_cyst', 'carrier', 'body'), ('biomass_bladder', 'cap', 'membrane'),
                ('harvest_corolla', 'heart', 'stem'), ('bio_furnace', 'ember', 'belly')):
            bones = {b['name']: b for b in geometry(model)['bones']}
            self.assertEqual(bones[child]['parent'], parent, (model, child))

    def test_joined_reservoir_edges_are_continuous_without_overlapping_faces(self):
        parts = json.loads((ASSETS / 'blockstates/reservoir_cell.json').read_text())['multipart']
        for edge, inset in (('rim', 2), ('foot', 1.5)):
            elements = []
            for cell in range(3):
                state = dict(north='false', south='false', up='false', down='false',
                             west=str(cell > 0).lower(), east=str(cell < 2).lower())
                for part in parts:
                    apply = part['apply']
                    if isinstance(apply, list) or apply.get('y', 0) != 0:
                        continue
                    name = apply['model'].split(':')[1]
                    if not name.startswith('block/reservoir_cell_' + edge):
                        continue
                    if not all(state[k] == v for k, v in part.get('when', {}).items()):
                        continue
                    for element in json.loads((ASSETS / 'models' / (name + '.json')).read_text())['elements']:
                        element['from'][0] += cell * 16
                        element['to'][0] += cell * 16
                        elements.append(element)
            spans = sorted((e['from'][0], e['to'][0]) for e in elements)
            self.assertEqual(spans[0][0], inset)
            self.assertEqual(spans[-1][1], 48 - inset)
            for first, second in zip(spans, spans[1:]):
                self.assertEqual(first[1], second[0], (edge, spans))
            self.assertEqual(coplanar_faces(elements), [])

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
                        if animation['loop'] is True:
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

    def test_door_transitions_meet_open_pose_and_closed_flaps_seal(self):
        import gen_openings as door
        self.assertTrue(door.covered())
        for door_name in door.VARIANTS:
            clips = animations(door_name)
            for name, channels in clips['open']['bones'].items():
                for channel, keys in channels.items():
                    opened = next(iter(keys.values()))
                    opening = clips['opening']['bones'][name][channel]
                    closing = clips['closing']['bones'][name][channel]
                    self.assertEqual(opening[max(opening, key=float)], opened)
                    self.assertEqual(closing[min(closing, key=float)], opened)

    def test_hardened_doors_keep_the_iris_and_add_bone(self):
        base = {bone['name'] for bone in geometry('sphincter_door')['bones']}
        for name in ('clenched_door', 'calcified_door'):
            self.assertEqual({bone['name'] for bone in geometry(name)['bones']}, base)
            self.assertGreater(len(geometry(name)['bones'][1]['cubes']), len(geometry('sphincter_door')['bones'][1]['cubes']))

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

    def test_cradle_petals_close_clear_of_the_cocoon_and_open_inside_the_block(self):
        """Closed in a bud, no petal touches the cocoon or its pulse; open, none leans past the block's edge."""
        import numpy as np
        import geo_audit
        geo = geometry('awakening_cradle')
        clips = animations('awakening_cradle')

        def boxes(clip, t):
            out = {}
            for name, m, lo, hi in geo_audit.world_cubes(geo, clips[clip], t):
                corners = np.array([[x, y, z, 1] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])])
                world = (m @ corners.T).T[:, :3]
                out[name] = (world.min(axis=0), world.max(axis=0))
            return out

        for clip in ('idle', 'working'):
            for t in sorted({float(k) for ch in clips[clip]['bones'].values() for keys in ch.values() for k in keys}):
                cubes = boxes(clip, t)
                for name, (lo, hi) in cubes.items():
                    if not name.startswith('petal_'):
                        continue
                    self.assertTrue(np.all(np.abs(lo[[0, 2]]) <= 8) and np.all(np.abs(hi[[0, 2]]) <= 8), (clip, t, name))
                    for other in [n for n in cubes if n.startswith(('cocoon', 'pulse'))]:
                        olo, ohi = cubes[other]
                        apart = np.any(hi <= olo) or np.any(ohi <= lo)
                        self.assertTrue(apart, (clip, t, name, other))

    def test_petals_hinge_above_the_heart_and_follow_the_stem(self):
        bones = {b['name']: b for b in geometry('harvest_corolla')['bones']}
        heart = bones['heart']['cubes'][0]
        top = heart['origin'][1] + heart['size'][1]
        for name, bone in bones.items():
            if name.startswith('petal_'):
                self.assertEqual(bone['parent'], 'stem')
                self.assertGreaterEqual(bone['pivot'][1], top)

    def test_digestive_sac_squeezes_sideways_so_the_gullet_stays_seated(self):
        for animation in animations('digestive_sac').values():
            for bone in ('belly', 'gullet'):
                for scale in animation['bones'].get(bone, {}).get('scale', {}).values():
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
