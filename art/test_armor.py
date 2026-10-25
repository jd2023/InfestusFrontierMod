"""Checks on the generated living armor. Run: python3 -m unittest discover -s art -p 'test_*.py'."""
import json
import unittest
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / 'src/main/resources/assets/infestusfrontier'
SETS = ('dormant', 'awakened')
PIECES = {'hood': 'head', 'thorax': 'chest', 'legwraps': 'legs', 'treads': 'feet'}
BONES = {'head': ((-4, -8, -4), (8, 8, 8)), 'body': ((-4, 0, -2), (8, 12, 4)),
         'right_arm': ((-3, -2, -2), (4, 12, 4)), 'left_arm': ((-1, -2, -2), (4, 12, 4)),
         'right_leg': ((-2, 0, -2), (4, 12, 4)), 'left_leg': ((-2, 0, -2), (4, 12, 4))}
SLOT_BONES = {'head': {'head'}, 'chest': {'body', 'right_arm', 'left_arm'},
              'legs': {'body', 'right_leg', 'left_leg'}, 'feet': {'right_leg', 'left_leg'}}


def shell(bone, slot):
    """The base armor box a slot draws on a bone: vanilla inflates the outer armor by 1 and the inner by 0.5, legs
    0.1 less."""
    (x, y, z), (w, h, d) = BONES[bone]
    inflate = (0.5 if slot == 'legs' else 1.0) - (0.1 if bone.endswith('leg') else 0)
    return (x - inflate, y - inflate, z - inflate), (x + w + inflate, y + h + inflate, z + d + inflate)


def growths(name):
    return json.loads((ASSETS / 'armor' / f'{name}.json').read_text())


class ArmorTest(unittest.TestCase):
    def test_growths_stay_on_the_sheet_and_on_their_slots_bones(self):
        for name in SETS:
            data = growths(name)
            width, height = data['size']
            for part in data['parts']:
                self.assertIn(part['bone'], SLOT_BONES[part['slot']], (name, part))
                for cube in part['cubes']:
                    w, h, d = (int(-(-v // 1)) for v in cube['size'])
                    u, v = cube['uv']
                    self.assertLessEqual(u + 2 * w + 2 * d, width, (name, cube))
                    self.assertLessEqual(v + h + d, height, (name, cube))

    def test_growth_cubes_have_whole_sizes(self):
        """ModelPart reads a cube's size straight into its box UV, so a fractional size smears the next face's texels
        in; swelling a cube goes through inflate instead."""
        for name in SETS:
            for part in growths(name)['parts']:
                for cube in part['cubes']:
                    self.assertTrue(all(v == int(v) for v in cube['size']), (name, cube))

    def test_no_growth_face_lies_in_a_base_shell_face(self):
        """A growth face in the same plane as the base armor's face, overlapping it, flickers in game."""
        for name in SETS:
            for part in growths(name)['parts']:
                lo, hi = shell(part['bone'], part['slot'])
                if 'rotation' in part:
                    continue
                for cube in part['cubes']:
                    inf = cube.get('inflate', 0)
                    clo = [part['pivot'][i] + cube['origin'][i] - inf for i in range(3)]
                    chi = [part['pivot'][i] + cube['origin'][i] + cube['size'][i] + inf for i in range(3)]
                    for axis in range(3):
                        others = [a for a in range(3) if a != axis]
                        overlap = all(min(chi[a], hi[a]) - max(clo[a], lo[a]) > 1e-6 for a in others)
                        for c in (clo[axis], chi[axis]):
                            for s in (lo[axis], hi[axis]):
                                self.assertFalse(overlap and abs(c - s) < 1e-6, (name, part['slot'], part['bone'], cube))

    def test_every_piece_has_a_dyed_and_a_plain_icon_layer(self):
        for name in SETS:
            for piece in PIECES:
                model = json.loads((ASSETS / 'models/item' / f'{name}_{piece}.json').read_text())
                self.assertEqual(set(model['textures']), {'layer0', 'layer1'})
                for suffix in ('', '_overlay'):
                    img = Image.open(ASSETS / 'textures/item' / f'{name}_{piece}{suffix}.png')
                    self.assertEqual(img.size, (32, 32))
                both = Image.alpha_composite(Image.open(ASSETS / 'textures/item' / f'{name}_{piece}.png').convert('RGBA'),
                                             Image.open(ASSETS / 'textures/item' / f'{name}_{piece}_overlay.png').convert('RGBA'))
                self.assertGreater(sum(1 for p in both.getdata() if p[3]), 150, f'{name} {piece} icon is too small')


if __name__ == '__main__':
    unittest.main()
