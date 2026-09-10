#!/usr/bin/env python3
"""Structural checks for the planning documents; not gameplay verification."""

from pathlib import Path
import re
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
NODE = re.compile(r"^\| ((?:[A-Z]{2,}-[A-Z]?\d+|T\d+-\d+|SR\d+)) — [^|]+\| ([^|]+)\|")
TOKEN = re.compile(r"ALL|ANY|START|(?:[A-Z]{2,}-[A-Z]?\d+|T\d+-\d+|SR\d+)(?:\.I{1,3})?|[(),]")


def parents(expression):
    tokens = TOKEN.findall(expression)
    if ''.join(tokens) != re.sub(r"\s+", '', expression):
        raise ValueError(f"Invalid prerequisite: {expression}")
    position = 0
    refs = []

    def take():
        nonlocal position
        if position >= len(tokens):
            raise ValueError(f"Incomplete prerequisite: {expression}")
        token = tokens[position]
        position += 1
        return token

    def visit():
        token = take()
        if token in ('ALL', 'ANY'):
            if take() != '(':
                raise ValueError(expression)
            visit()
            count = 1
            while position < len(tokens) and tokens[position] == ',':
                take()
                visit()
                count += 1
            if take() != ')' or count < 2:
                raise ValueError(expression)
        elif token == 'START':
            if len(tokens) != 1:
                raise ValueError(expression)
        elif token not in ('(', ')', ','):
            refs.append(token)
        else:
            raise ValueError(expression)

    visit()
    if position != len(tokens):
        raise ValueError(expression)
    return refs


def check_graph(graph):
    visiting, finished = set(), set()

    def visit(node):
        if node not in graph:
            raise ValueError(f"Unknown guide node: {node}")
        if node in visiting:
            raise ValueError(f"Guide cycle at {node}")
        if node in finished:
            return
        visiting.add(node)
        for parent in graph[node]:
            visit(parent)
        visiting.remove(node)
        finished.add(node)

    for node in graph:
        visit(node)


def unique(values, label):
    if len(values) != len(set(values)):
        raise ValueError(f"Duplicate {label}")


def check_tables(content, label):
    runs, run = [], []
    for line in content.splitlines() + ['']:
        if line.startswith('|'):
            run.append(line)
        elif run:
            runs.append(run)
            run = []
    for rows in runs:
        if len(rows) < 2 or not re.fullmatch(r'[| :\-]+', rows[1]):
            raise ValueError(f"Detached table rows in {label}: {rows[0][:80]}")
        widths = {len(re.split(r'(?<!\\)\|', row)) for row in rows}
        if len(widths) != 1:
            raise ValueError(f"Unequal table columns in {label}: {rows[0][:80]}")


def check(root):
    docs = root / 'docs'
    guide = (docs / 'GUIDE_PROGRESSION_TREE.md').read_text()
    rows = [match.groups() for line in guide.splitlines() if (match := NODE.match(line))]
    unique([node for node, _ in rows], 'guide ID')
    graph = {node: parents(expression) for node, expression in rows}
    for node in list(graph):
        if node.startswith('AM-'):
            graph[node + '.I'] = [node]
            graph[node + '.II'] = [node + '.I']
            graph[node + '.III'] = [node + '.II']
    check_graph(graph)

    blocks = re.findall(r'^### (T\d+-\d+) —', (docs / 'BLOCK_CATALOG.md').read_text(), re.M)
    unique(blocks, 'block ID')
    guide_blocks = {node for node in graph if re.fullmatch(r'T\d+-\d+', node)}
    if set(blocks) != guide_blocks:
        raise ValueError(f"Block/guide coverage mismatch: {set(blocks) ^ guide_blocks}")

    armor = (docs / 'ARMOR_EVOLUTION.md').read_text()
    mutations = re.findall(r'^\| ([MHCLB]\d+) [^|]+\|', armor, re.M)
    unique(mutations, 'armor mutation ID')
    guide_mutations = {node[3:] for node in graph if node.startswith('AM-') and '.' not in node}
    if set(mutations) != guide_mutations:
        raise ValueError(f"Armor/guide coverage mismatch: {set(mutations) ^ guide_mutations}")

    material_parent = {'AR-06': 'AR-03', 'AR-07': 'AR-03', 'AR-08': 'AR-06',
                       'AR-09': 'AR-06', 'AR-10': 'AR-07', 'AR-11': 'AR-08',
                       'AR-12': 'AR-10', 'AR-13': 'AR-10', 'AR-27': 'AR-09'}
    material_ids = set(material_parent)
    for node, required in material_parent.items():
        actual = set(graph[node]) & (material_ids | {'AR-03'})
        if actual != {required}:
            raise ValueError(f"Non-tree armor lineage at {node}: {actual}")

    items = re.findall(r'^\| (I\d+) \|', (docs / 'ITEM_CATALOG.md').read_text(), re.M)
    unique(items, 'item ID')
    if not items:
        raise ValueError('Empty item catalog')

    # Check tracked-document locations, not external websites or prototype paths.
    documents = [root / 'README.md', root / 'VISION.md', root / 'AGENTS.md']
    documents += list(docs.glob('*.md')) + list((root / '.ktask').glob('*.md'))
    for document in documents:
        check_tables(document.read_text(), document.name)
        for target in re.findall(r'\[[^\]\n]+\]\(([^)\s]+)\)', document.read_text()):
            if re.match(r'[a-z]+://', target):
                continue
            path, _, fragment = target.partition('#')
            destination = document.parent / path if path else document
            if not destination.exists():
                raise ValueError(f"Broken local link: {document.name}: {target}")
            if fragment and destination.suffix == '.md':
                headings = re.findall(r'^#{1,6} (.+)$', destination.read_text(), re.M)
                slugs = [re.sub(r'[^\w\- ]', '', heading.lower()).replace(' ', '-') for heading in headings]
                if fragment not in slugs:
                    raise ValueError(f"Broken heading link: {document.name}: {target}")
    return f'{len(blocks)} blocks; {len(items)} item rows/families; {len(mutations)} armor families; {len(rows)} guide nodes; local links valid'


class CheckerTests(unittest.TestCase):
    def test_table_breakage_rejected(self):
        check_tables('| A | B |\n|---|---|\n| x | y |', 'valid')
        for content in ('| x | y |', '| A | B |\n|---|---|\n| x |'):
            with self.assertRaises(ValueError):
                check_tables(content, 'broken')

    def test_nested_prerequisites(self):
        self.assertEqual(parents('ALL(SR2,ANY(T2-01,AM-B7.II))'), ['SR2', 'T2-01', 'AM-B7.II'])

    def test_malformed_rejected(self):
        for expression in ('ALL(SR2)', 'ANY(SR2,)', 'SR2 garbage', 'ALL(START,SR2)'):
            with self.assertRaises(ValueError):
                parents(expression)

    def test_cycles_and_unknown_nodes_rejected(self):
        for graph in ({'a': ['b'], 'b': ['a']}, {'a': ['missing']}):
            with self.assertRaises(ValueError):
                check_graph(graph)
        check_graph({'root': [], 'child': ['root']})


if __name__ == '__main__':
    if '--self-test' in sys.argv:
        unittest.main(argv=[sys.argv[0]])
    else:
        try:
            print(check(ROOT))
        except ValueError as error:
            sys.exit(str(error))
