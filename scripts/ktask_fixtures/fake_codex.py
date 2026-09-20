#!/usr/bin/env python3
"""Deterministic CLI double, usable only inside an explicitly named test repository."""

import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

root = Path.cwd()
assert str(root) == os.environ['KTASK_TEST_ROOT']
sys.path.insert(0, str(root / 'scripts'))
import ktask_workflow as flow
from ktask_contracts import CHECKS

prompt = sys.stdin.read()
session = root / '.ktask/session'
scenario = os.environ.get('KTASK_TEST_SCENARIO', 'success')
trace = session / 'fixture-events.jsonl'


def event(role, task):
    with trace.open('a') as stream:
        stream.write(json.dumps(dict(role=role, task=task)) + '\n')


if '--output-schema' in sys.argv:
    identity = re.search(r'^Task: (IF-\d{3})$', prompt, re.M)[1]
    candidate = re.search(r'^Candidate: ([a-f0-9]+)$', prompt, re.M)[1]
    event('review', identity)
    reject = scenario == 'rejected-review' and (root / 'owned.txt').read_text() != identity
    output = dict(task=identity, candidate=candidate, verdict='reject' if reject else 'accept',
                  findings=['owned.txt contains an incomplete result'] if reject else [],
                  checks={key: dict(status='fail' if reject else 'pass',
                                    evidence='Fixture owned.txt result and recorded assertion') for key in CHECKS})
    Path(sys.argv[sys.argv.index('-o') + 1]).write_text(json.dumps(output))
    sys.exit(0)

state = json.loads((session / 'active.json').read_text())
identity = state['task']
events = [json.loads(line) for line in trace.read_text().splitlines()] if trace.exists() else []
first = not any(row == dict(role='worker', task=identity) for row in events)
event('worker', identity)
print('fixture worker progress: ' + identity, flush=True)
index = re.search(r'\[Orchestrator context\] Task (\d+) of (\d+)', prompt)[1]
report = root / f'.ktask/queue/report-{index}.md'

if scenario == 'needs-input':
    report.write_text('KTASK_RESULT: NEEDS_INPUT\nSummary: Fixture product decision required.\nAction: Choose the fixture outcome.\n')
    sys.exit(0)

if scenario == 'interrupt' and first:
    child = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(120)'], start_new_session=True)
    (session / 'fixture-child.pid').write_text(str(child.pid))
    (root / 'owned.txt').write_text('unfinished')
    (session / 'fixture-waiting').touch()
    time.sleep(120)

tasks, policy = flow.load_project(root)
task = next(task for task in tasks if task['id'] == identity)
folder = session / 'evidence' / identity
(root / 'assert_result.py').write_text(
    'from pathlib import Path; assert Path("owned.txt").read_text().startswith(' + repr(identity) + '); print("result assertion passed")\n')
test = [sys.executable, 'assert_result.py']
candidate = flow.candidate_state(root, task, state, policy)[1]
if not (folder / 'red.json').exists():
    flow.record(folder, flow.evidence_binding(state), candidate, 'red', test, root, 10)
if scenario == 'failed-test' and first:
    flow.record(folder, flow.evidence_binding(state), candidate, 'green', test, root, 10)
    report.write_text('KTASK_RESULT: FAILED\nSummary: Actual fixture assertion failed.\n')
    sys.exit(1)
(root / 'owned.txt').write_text(identity + (' incomplete' if scenario == 'rejected-review' and first else ''))
candidate = flow.candidate_state(root, task, state, policy)[1]
green = flow.record(folder, flow.evidence_binding(state), candidate, 'green', test, root, 10)
flow.save(folder / 'evidence.json', dict(task=identity, baseline=state['baseline'],
                                      artifacts=dict(rules=[green['log']])))
report.write_text('KTASK_RESULT: DONE\nSummary: Fixture candidate ready for independent acceptance.\n')
