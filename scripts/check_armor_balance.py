#!/usr/bin/env python3
"""Design arithmetic read from Armor Evolution; not Minecraft implementation."""
from fractions import Fraction
from pathlib import Path
import re
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
UNITS = 600  # Fixed-point representation, not a balance value.


def allocate_window(counts, capacity, raw):
    counts = {key: Fraction(value) for key, value in counts.items()}
    capacity = Fraction(capacity)
    raw = {key: Fraction(value) for key, value in raw.items()}
    if (capacity < 0 or any(value < 0 for value in counts.values())
            or any(value < 0 for value in raw.values())
            or not raw.keys() <= counts.keys() or sum(counts.values()) > capacity):
        raise ValueError('Invalid capacity, counter or activity credit')
    if any((value * UNITS).denominator != 1 for value in [capacity, *counts.values()]):
        raise ValueError('Counters must use fixed-point units')
    requested = {key: int(min(value, 1) * UNITS) for key, value in raw.items()}
    total = sum(requested.values())
    grant = min(UNITS, total, int((capacity - sum(counts.values())) * UNITS))
    if not grant:
        return counts
    exact = {key: Fraction(grant * amount, total) for key, amount in requested.items()}
    allocated = {key: int(amount) for key, amount in exact.items()}
    order = sorted(exact, key=lambda key: (-(exact[key] - allocated[key]), key))
    for key in order[:grant - sum(allocated.values())]:
        allocated[key] += 1
    return {key: value + Fraction(allocated.get(key, 0), UNITS) for key, value in counts.items()}


def read_balance(text):
    threshold_match = re.search(r'Thresholds are \*\*([\d /]+) points\*\*', text)
    if not threshold_match:
        raise ValueError('Missing learning thresholds')
    thresholds = [int(n) for n in re.findall(r'\d+', threshold_match[1])]
    section = text.split('### Learning capacity\n', 1)[1].split('### Fusion installation', 1)[0]
    capacities = dict((name, int(value)) for name, value in
                      re.findall(r'^\| ([^|]+?) \| (\d+) \|$', section, re.M))
    fusion = [tuple(map(int, row)) for row in re.findall(
        r'^\| G([234]) \| (\d+) \| (\d+) \| (\d+) \| (\d+) \|$', text, re.M)]
    seconds = int(re.search(r'Every (\d+) loaded server ticks', text)[1]) / 20
    rates = [int(n) for n in re.search(
        r'supply scenario of \*\*(\d+) / (\d+) / (\d+) BU/s\*\*', text).groups()]
    return thresholds, capacities, fusion, seconds, rates


def validate(text):
    thresholds, caps, fusion, seconds, rates = read_balance(text)
    if thresholds != sorted(set(thresholds)) or len(thresholds) != 5 or thresholds[0] <= 0:
        raise ValueError('Five increasing positive buff thresholds required')
    if caps.get('Dormant flesh') != 0:
        raise ValueError('Dormant armor must not learn')
    parents = {
        'Living frame': 'Dormant flesh', 'Iron ribs': 'Living frame',
        'Auric lattice': 'Living frame', 'Diamond carapace': 'Iron ribs',
        'Obsidian scutes': 'Iron ribs', 'Diamond tendon': 'Auric lattice',
        'Netherite lamellae': 'Diamond carapace',
        'Netherite-bonded scutes': 'Obsidian scutes',
        'Netherite mesh': 'Diamond tendon', 'Spatial weave': 'Diamond tendon',
    }
    if set(caps) != set(parents) | {'Dormant flesh'}:
        raise ValueError('Capacity table must cover every frame exactly')
    for child, parent in parents.items():
        if caps[child] <= caps[parent]:
            raise ValueError(f'Fusion reduces capacity: {parent} -> {child}')
    if thresholds[0] > caps['Living frame']:
        raise ValueError('Starter armor cannot earn its first buff')
    for frame in ('Netherite lamellae', 'Netherite-bonded scutes'):
        if caps[frame] >= thresholds[-1]:
            raise ValueError('Rigid frame can reach the flexible specialist threshold')
    for frame in ('Netherite mesh', 'Spatial weave'):
        if caps[frame] < thresholds[-1]:
            raise ValueError('Flexible terminal cannot specialize')
    if len(fusion) != 3 or [row[0] for row in fusion] != [2, 3, 4]:
        raise ValueError('Missing fusion installation level')
    if seconds <= 0 or any(v <= 0 for row in fusion for v in row) or any(r <= 0 for r in rates):
        raise ValueError('Nonpositive balance parameter')
    for before, after in zip(fusion, fusion[1:]):
        if any(a <= b for b, a in zip(before[1:], after[1:])):
            raise ValueError('Fusion inputs and chamber time must grow')
    supply_seconds = [Fraction(row[3], rate) for row, rate in zip(fusion, rates)]
    if any(a <= b for b, a in zip(supply_seconds, supply_seconds[1:])):
        raise ValueError('Later fusion becomes easier in the stated supply scenario')
    minutes = [t * seconds / 60 for t in thresholds]
    return f'Calculated focused-learning minutes: {minutes}; fusion supply seconds: {list(map(float, supply_seconds))}. Not playtest measurements.'


class BalanceTests(unittest.TestCase):
    def test_shared_budget_splits_not_multiplies(self):
        self.assertEqual(allocate_window({'a': 0, 'b': 0}, 100, {'a': 1, 'b': 1}),
                         {'a': Fraction(1, 2), 'b': Fraction(1, 2)})

    def test_capacity_includes_fractions(self):
        counts = {'a': Fraction(799, 10), 'b': 20}
        result = allocate_window(counts, 100, {'a': 1, 'b': 1})
        self.assertEqual(sum(result.values()), 100)
        self.assertEqual(result['a'] - counts['a'], Fraction(1, 20))
        self.assertEqual(allocate_window(result, 100, {'a': 1}), result)

    def test_no_invented_credit_or_hidden_overflow(self):
        self.assertEqual(allocate_window({'a': 0}, 100, {'a': Fraction(1, 10)})['a'], Fraction(1, 10))
        full = allocate_window({'a': 99}, 100, {'a': 99999})
        self.assertEqual(full['a'], 100)
        self.assertEqual(allocate_window(full, 300, {})['a'], 100)

    def test_rounding_and_ties_are_deterministic(self):
        result = allocate_window(dict.fromkeys('abcdefg', 0), 100, dict.fromkeys('gfedcba', 1))
        self.assertEqual(sum(result.values()), 1)
        self.assertEqual(result['a'], Fraction(86, 600))
        self.assertEqual(result['g'], Fraction(85, 600))

    def test_invalid_inputs_refused(self):
        for counts, cap, raw in [({'a': -1}, 1, {}), ({'a': 2}, 1, {}),
                                  ({'a': 0}, 1, {'b': 1}), ({'a': 0}, 1, {'a': -1})]:
            with self.assertRaises(ValueError):
                allocate_window(counts, cap, raw)

    def test_real_spec_and_broken_progression(self):
        text = (ROOT / 'docs/ARMOR_EVOLUTION.md').read_text()
        validate(text)
        for broken in (
            text.replace('| Iron ribs | 300 |', '| Iron ribs | 50 |'),
            text.replace('| Spatial weave | 2400 |', '| Spatial weave | 1000 |'),
            text.replace('| G4 | 64 | 32 | 256000 | 1920 |', '| G4 | 64 | 32 | 64000 | 1920 |'),
        ):
            with self.assertRaises(ValueError):
                validate(broken)


if __name__ == '__main__':
    if '--self-test' in sys.argv:
        unittest.main(argv=[sys.argv[0]])
    else:
        try:
            print(validate((ROOT / 'docs/ARMOR_EVOLUTION.md').read_text()))
        except (ValueError, IndexError, AttributeError) as error:
            sys.exit(str(error))
