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


def check_assemblies(content):
    """Named assemblies may contain catalog parts, never themselves or unknown parts."""
    sections = re.split(r'^### (T\d+-\d+) —[^\n]*\n', content, flags=re.M)
    graph = {}
    for node, body in zip(sections[1::2], sections[2::2]):
        fields = re.findall(r'^- \*\*Assembly:\*\* (.+)$', body, re.M)
        if len(fields) > 1:
            raise ValueError(f'Duplicate assembly definition: {node}')
        components = []
        if fields:
            if not re.fullmatch(r'T\d+-\d+(?:, T\d+-\d+)*\.', fields[0]):
                raise ValueError(f'Invalid assembly parts at {node}: {fields[0]}')
            components = re.findall(r'T\d+-\d+', fields[0])
            unique(components, f'assembly component at {node}')
        graph[node] = components
    check_graph(graph)


def check_questions(register, documents):
    """The register contains questions only; references resolve to unique IDs."""
    if not register.startswith('# Open questions\n'):
        raise ValueError('Missing open-question register heading')
    ids = []
    for line in register.splitlines():
        if not line.strip() or re.fullmatch(r'#{1,2} [^?]+', line):
            continue
        match = re.fullmatch(
            r'- (Q-\d{3}): (?:What|Which|How|Who|When|Where|Why|Should|Can|Does|Do|Is|Are|Will|Must) [^?]+\?',
            line,
        )
        if not match:
            raise ValueError(f'Expected a question, not prose or an answer: {line}')
        ids.append(match[1])
    unique(ids, 'open question ID')
    for name, content in documents.items():
        unknown = set(re.findall(r'\bQ-\d{3}\b', content)) - set(ids)
        if unknown:
            raise ValueError(f'Unknown open questions in {name}: {unknown}')


def check_current_armor(content):
    for obsolete in ('L0–L25', 'allocated growth points', 'Each counter\'s cap',
                     'five practice counters', 'five active counter ranks'):
        if obsolete in content:
            raise ValueError(f'Superseded armor rule: {obsolete}')
    for required in ('sum(counters) <= learning_capacity', 'cannot be paused',
                     'partial counter reduction'):
        if required not in content:
            raise ValueError(f'Missing armor contract: {required}')


def check_catalog_references(documents, blocks, items):
    """Deleted catalog entries must not survive as recipe or prose references."""
    known = set(blocks) | set(items)
    for name, content in documents.items():
        unknown = set(re.findall(r'\b(?:T\d+-\d+|I\d{3})\b', content)) - known
        if unknown:
            raise ValueError(f'Unknown catalog references in {name}: {sorted(unknown)}')


def check_activity_owners(content):
    rows = re.findall(r'^\| ([HCLB]-[A-Z]) \| (Helmet|Chest|Leggings|Boots) \|', content, re.M)
    unique([counter for counter, _ in rows], 'activity counter')
    owners = {'H': 'Helmet', 'C': 'Chest', 'L': 'Leggings', 'B': 'Boots'}
    if not rows:
        raise ValueError('Missing activity ownership table')
    for counter, owner in rows:
        if owners[counter[0]] != owner:
            raise ValueError(f'Wrong activity owner: {counter}: {owner}')


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

    block_content = (docs / 'BLOCK_CATALOG.md').read_text()
    blocks = re.findall(r'^### (T\d+-\d+) —', block_content, re.M)
    unique(blocks, 'block ID')
    check_assemblies(block_content)
    guide_blocks = {node for node in graph if re.fullmatch(r'T\d+-\d+', node)}
    if set(blocks) != guide_blocks:
        raise ValueError(f"Block/guide coverage mismatch: {set(blocks) ^ guide_blocks}")

    armor = (docs / 'ARMOR_EVOLUTION.md').read_text()
    check_current_armor(armor)
    check_activity_owners(armor)
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
    question_path = docs / 'OPEN_QUESTIONS.md'
    if not question_path.exists():
        raise ValueError('Missing OPEN_QUESTIONS.md')
    check_catalog_references({str(document): document.read_text() for document in documents}, blocks, items)
    check_questions(question_path.read_text(), {
        str(document): document.read_text() for document in documents
    })
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
    return f'{len(blocks)} block/assembly entries; {len(items)} item rows/families; {len(mutations)} armor families; {len(rows)} guide nodes; assembly references and local links valid'


class CheckerTests(unittest.TestCase):
    def test_removed_catalog_references_rejected(self):
        check_catalog_references({'recipe': 'T2-03 and I044'}, ['T2-03'], ['I044'])
        for reference in ('T2-07', 'I043'):
            with self.assertRaises(ValueError):
                check_catalog_references({'recipe': reference}, ['T2-03'], ['I044'])

    def test_activity_ownership(self):
        valid = '| H-D | Helmet | Darkness | Visibility |\n| L-W | Leggings | Swimming | Speed |\n'
        check_activity_owners(valid)
        for content in ('', valid + valid, valid.replace('H-D | Helmet', 'H-D | Boots')):
            with self.assertRaises(ValueError):
                check_activity_owners(content)

    def test_question_register_rejects_drift(self):
        valid = '# Open questions\n\n- Q-035: Must core gameplay work without other content mods?\n'
        check_questions(valid, {'dependencies': 'See Q-035.'})
        for register, documents in (
            ('', {}), (valid + valid, {}),
            (valid + 'The capacity is 100.\n', {}),
            (valid + '- **Resolve in:** Armor.\n', {}),
            (valid.replace('Must core gameplay work without other content mods?', 'Compatibility is required?'), {}),
            (valid.replace('mods?', 'mods.'), {}),
            (valid, {'armor': 'See Q-099.'}),
        ):
            with self.assertRaises(ValueError):
                check_questions(register, documents)

    def test_question_register_can_be_cleared(self):
        cleared = '# Open questions\n'
        check_questions(cleared, {'armor': 'Current rules, no unresolved references.'})
        with self.assertRaises(ValueError):
            check_questions(cleared, {'armor': 'See Q-007.'})

    def test_superseded_armor_rules_rejected(self):
        valid = 'sum(counters) <= learning_capacity; cannot be paused; partial counter reduction'
        check_current_armor(valid)
        for content in (valid + ' L0–L25', valid + " Each counter's cap", ''):
            with self.assertRaises(ValueError):
                check_current_armor(content)

    def test_assembly_components_are_known_and_acyclic(self):
        check_assemblies('### T0-01 — Bed\n### T1-01 — Line\n- **Assembly:** T0-01.\n')
        for content in (
            '### T1-01 — Line\n- **Assembly:** T0-99.\n',
            '### T1-01 — Line\n- **Assembly:** T1-01.\n',
            '### T1-01 — A\n- **Assembly:** T1-02.\n### T1-02 — B\n- **Assembly:** T1-01.\n',
            '### T1-01 — Line\n- **Assembly:** unspecified parts\n',
        ):
            with self.assertRaises(ValueError):
                check_assemblies(content)

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
