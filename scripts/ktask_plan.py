"""Catalog coverage and explicit producer-before-consumer task contracts."""

import hashlib
import json


def bind_contracts(tasks, plan):
    """Bind each task's own metadata without invalidating unrelated accepted work."""
    bound = []
    for task in tasks:
        identity = task['id']
        obligations = {section: sorted(symbol for symbol, owner in plan[section].items() if owner == identity)
                       for section in ('items', 'mutations', 'services')}
        requirements = sorted(plan['requires'].get(identity, []))
        if any(obligations.values()) or requirements:
            value = json.dumps([task['digest'], obligations, requirements], sort_keys=True)
            task = dict(task, digest=hashlib.sha256(value.encode()).hexdigest())
        bound.append(task)
    return bound


def validate_plan(tasks, plan, items, families):
    """Every obligation has one owner; declared inputs exist at its checkpoint."""
    if len(tasks) > 4096:
        raise ValueError('Task plan exceeds4096 packets')
    expected = {'items', 'mutations', 'services', 'requires', 'excluded_items'}
    if not isinstance(plan, dict) or set(plan) != expected:
        raise ValueError('Invalid content-plan sections')
    if any(not isinstance(plan[key], dict) for key in expected - {'excluded_items'}):
        raise ValueError('Content-plan mappings required')
    if not isinstance(plan['excluded_items'], list) or any(not isinstance(x, str) for x in plan['excluded_items']):
        raise ValueError('Invalid excluded item list')
    excluded = set(plan['excluded_items'])
    supplied = set(plan['items'])
    if supplied & excluded or supplied | excluded != items or len(excluded) != len(plan['excluded_items']):
        raise ValueError(f'Item ownership mismatch: missing={items - supplied - excluded}, '
                         f'extra={(supplied | excluded) - items}, overlap={supplied & excluded}')
    ranks = {f'{family}.{rank}' for family in families for rank in ('I', 'II', 'III')}
    if set(plan['mutations']) != ranks:
        raise ValueError(f'Mutation ownership mismatch: {ranks ^ set(plan["mutations"])}')
    providers, ancestors = {}, {}
    for task in tasks:
        identity = task['id']
        parents = task['dependencies']
        if any(parent not in ancestors for parent in parents):
            raise ValueError(f'{identity}: dependency must precede task')
        ancestors[identity] = set(parents).union(*(ancestors[parent] for parent in parents))
        for block in [] if task['Blocks'] == 'none' else task['Blocks'].split(', '):
            if block in providers:
                raise ValueError(f'Duplicate block owner: {block}')
            providers[block] = identity
    for section in ('items', 'mutations', 'services'):
        for symbol, owner in plan[section].items():
            if not isinstance(owner, str) or owner not in ancestors:
                raise ValueError(f'Unknown owner {owner}: {symbol}')
            if symbol in providers:
                raise ValueError(f'Duplicate content symbol: {symbol}')
            providers[symbol] = owner
    edges = 0
    for consumer, inputs in plan['requires'].items():
        if consumer not in ancestors or not isinstance(inputs, list) or any(not isinstance(x, str) for x in inputs):
            raise ValueError(f'Invalid consumer inputs: {consumer}')
        edges += len(inputs)
        if edges > 16384 or len(inputs) != len(set(inputs)):
            raise ValueError('Duplicate or excessive content dependency edges')
        for symbol in inputs:
            producer = providers.get(symbol)
            if producer is None:
                raise ValueError(f'{consumer}: unknown input {symbol}')
            if producer != consumer and producer not in ancestors[consumer]:
                raise ValueError(f'{consumer}: input {symbol} requires earlier dependency {producer}')
