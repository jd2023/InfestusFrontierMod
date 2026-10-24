"""Regression checks for generated motion. Run: python3 -m unittest discover -s art -p 'test_*.py'."""
import unittest

import gen_biomass_fluid as fluid


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
