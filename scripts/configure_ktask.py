#!/usr/bin/env python3
"""Apply this checkout's role reading map to its local ktask configuration."""
import argparse
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
START = '[Infestus role instructions]\n'
END = '\n[/Infestus role instructions]\n\n'


def call(*args):
    result = subprocess.run(
        ['ktask', '--json', '--project', str(ROOT), '--actor-kind', 'ai',
         '--actor', 'infestus-role-config', *args],
        check=True, capture_output=True, text=True)
    return json.loads(result.stdout)['data']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--refresh-pending', action='store_true',
                        help='Repin ready pending tasks to the updated category templates')
    args = parser.parse_args()
    config = json.loads((ROOT / 'config/ktask-roles.json').read_text())
    for name in config['categories']:
        category = call('category', 'show', name)['category']
        updated = {}
        for role, instruction in config['roles'].items():
            original = category['templates'][role]
            base = original
            if base.startswith(START):
                base = base.split(END, 1)[1]
            template = START + instruction + END + base
            if template != original:
                updated[role] = template
        if updated:
            command = ['category', 'save', name, '--scope', 'project']
            if category['scope'] == 'project':
                command += ['--if-version', str(category['version'])]
            for role, template in updated.items():
                command += ['--' + role, template]
            call(*command)
        print(f'{name}: role instructions configured')
    if args.refresh_pending:
        for task in call('list')['tasks']:
            if task['state'] != 'pending' or task.get('pending_revision') is not None:
                continue
            if task['category'] not in config['categories']:
                continue
            call('edit', str(task['id']), '--if-version', str(task['version']),
                 '--category', task['category'])
            print(f'Task #{task["id"]}: category refreshed')


if __name__ == '__main__':
    main()
