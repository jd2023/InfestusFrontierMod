"""Finds depth fighting in the GeckoLib models: two parallel faces of different cubes that overlap closer than a
tenth of a pixel, facing the same way or opposite (organ models draw both sides of every face), where the overlap
can be seen from outside. A tenth of a pixel is what the depth buffer resolves at 64 blocks, as far as block
entities are drawn. Checks the rest pose and every keyframe of every animation.

    python3 art/geo_audit.py [--all] [model ...]

The organ models' tests run it on every model; a hit prints the model, animation, time and both cubes.
"""
import json
import sys
from pathlib import Path

import numpy as np

ASSETS = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/infestusfrontier'
NEAR = 0.1  # pixels: the depth buffer's resolution at 64 blocks
SAMPLES = 5


def rotation(degrees):
    """GeckoLib turns a bone or cube about z, then y, then x, with x and y negated from the Bedrock file."""
    x, y, z = np.radians([-degrees[0], -degrees[1], degrees[2]])
    rx = np.array([[1, 0, 0], [0, np.cos(x), -np.sin(x)], [0, np.sin(x), np.cos(x)]])
    ry = np.array([[np.cos(y), 0, np.sin(y)], [0, 1, 0], [-np.sin(y), 0, np.cos(y)]])
    rz = np.array([[np.cos(z), -np.sin(z), 0], [np.sin(z), np.cos(z), 0], [0, 0, 1]])
    return rx @ ry @ rz


def affine(pivot, rot=(0, 0, 0), scale=(1, 1, 1), offset=(0, 0, 0)):
    """translate(offset) . translate(pivot) . rotate . scale . translate(-pivot), as a 4x4 matrix."""
    m = np.eye(4)
    m[:3, :3] = rotation(rot) @ np.diag(scale)
    p = np.array(pivot, dtype=float)
    m[:3, 3] = p - m[:3, :3] @ p + np.array(offset, dtype=float)
    return m


def channel(keys, t):
    """A channel's value at t: linear between keyframes, held past the ends."""
    if isinstance(keys, list):
        return keys
    times = sorted((float(k), v) for k, v in keys.items())
    vals = [(k, v['post'] if isinstance(v, dict) and 'post' in v else v.get('vector') if isinstance(v, dict) else v)
            for k, v in times]
    if t <= vals[0][0]:
        return vals[0][1]
    for (t0, a), (t1, b) in zip(vals, vals[1:]):
        if t <= t1:
            f = (t - t0) / (t1 - t0) if t1 > t0 else 0
            return [x + (y - x) * f for x, y in zip(a, b)]
    return vals[-1][1]


POSES_PER_ANIMATION = 16  # evenly chosen keyframes; --all checks every one


def poses(anims, every=False):
    """(animation, time) for the rest pose and the keyframe times of every animation."""
    yield None, 0.0
    for name, anim in anims.items():
        times = {0.0}
        for channels in anim.get('bones', {}).values():
            for keys in channels.values():
                if isinstance(keys, dict):
                    times.update(float(k) for k in keys)
        times = sorted(times)
        if not every and len(times) > POSES_PER_ANIMATION:
            step = (len(times) - 1) / (POSES_PER_ANIMATION - 1)
            times = [times[round(i * step)] for i in range(POSES_PER_ANIMATION)]
        for t in times:
            yield name, t


def world_cubes(geo, anim, t):
    """Every cube in the game's space at time t. GeckoLib mirrors a model in x as it bakes it: pivots and cube origins
    are mirrored (an origin x becomes -(x + width)) and rotations about x and y negated (see rotation)."""
    bones = {b['name']: b for b in geo['bones']}
    matrices = {}

    def matrix(name):
        if name in matrices:
            return matrices[name]
        bone = bones[name]
        ch = (anim or {}).get('bones', {}).get(name, {})
        rot = list(bone.get('rotation', [0, 0, 0]))
        if 'rotation' in ch:
            rot = [a + b for a, b in zip(rot, channel(ch['rotation'], t))]
        scale = channel(ch['scale'], t) if 'scale' in ch else [1, 1, 1]
        if isinstance(scale, (int, float)):
            scale = [scale] * 3
        pos = channel(ch['position'], t) if 'position' in ch else [0, 0, 0]
        px, py, pz = bone.get('pivot', [0, 0, 0])
        local = affine([-px, py, pz], rot, scale, [-pos[0], pos[1], pos[2]])
        parent = bone.get('parent')
        matrices[name] = (matrix(parent) if parent else np.eye(4)) @ local
        return matrices[name]

    out = []
    for name, bone in bones.items():
        m = matrix(name)
        if np.allclose(m[:3, :3], 0):
            continue
        for i, cube in enumerate(bone.get('cubes', [])):
            inf = cube.get('inflate', 0)
            ox, oy, oz = cube['origin']
            lo = np.array([-(ox + cube['size'][0]), oy, oz], dtype=float) - inf
            hi = lo + np.array(cube['size'], dtype=float) + 2 * inf
            turn = None
            if 'rotation' in cube:
                cx, cy, cz = cube['pivot']
                turn = affine([-cx, cy, cz], cube['rotation'])
            cm = m @ (turn if turn is not None else np.eye(4))
            if abs(np.linalg.det(cm[:3, :3])) < 1e-9:
                continue
            out.append((f'{name}[{i}]', cm, lo, hi))
    return out


FACES = {  # face: (axis, which end, the two in-plane axes)
    'west': (0, 0, (1, 2)), 'east': (0, 1, (1, 2)), 'down': (1, 0, (0, 2)),
    'up': (1, 1, (0, 2)), 'north': (2, 0, (0, 1)), 'south': (2, 1, (0, 1)),
}


def faces(cube):
    name, m, lo, hi = cube
    for face, (axis, end, (u, v)) in FACES.items():
        corner = lo.copy()
        corner[axis] = hi[axis] if end else lo[axis]
        du = np.zeros(3)
        du[u] = hi[u] - lo[u]
        dv = np.zeros(3)
        dv[v] = hi[v] - lo[v]
        o = (m @ np.append(corner, 1))[:3]
        eu = m[:3, :3] @ du
        ev = m[:3, :3] @ dv
        n = np.cross(eu, ev)
        if np.linalg.norm(n) < 1e-9:
            continue
        n = n / np.linalg.norm(n)
        if np.dot(n, m[:3, :3] @ (np.eye(3)[axis] * (1 if end else -1))) < 0:
            n = -n
        corners = np.array([o, o + eu, o + ev, o + eu + ev])
        yield face, o, eu, ev, n, corners.min(axis=0) - NEAR, corners.max(axis=0) + NEAR


def inside(point, cubes, skip=()):
    for name, m, lo, hi in cubes:
        if name in skip:
            continue
        local = np.linalg.solve(m, np.append(point, 1))[:3]
        if np.all(local > lo + 1e-4) and np.all(local < hi - 1e-4):
            return True
    return False


def overlap(fa, fb):
    """The points of face a, sampled on a grid, that land on face b within NEAR of its plane."""
    _, oa, ua, va, na, alo, ahi = fa
    _, ob, ub, vb, nb, blo, bhi = fb
    if np.any(alo > bhi) or np.any(blo > ahi):
        return []
    if abs(np.dot(na, nb)) < 0.999:
        return []
    if abs(np.dot(oa - ob, nb)) > NEAR:
        return []
    basis = np.array([ub, vb]).T
    points = []
    for a in np.linspace(0.1, 0.9, SAMPLES):
        for b in np.linspace(0.1, 0.9, SAMPLES):
            p = oa + ua * a + va * b
            coords, *_ = np.linalg.lstsq(basis, p - ob, rcond=None)
            if 0.001 < coords[0] < 0.999 and 0.001 < coords[1] < 0.999:
                points.append(p)
    return points


def seen(points, fa, fb, cubes, a_owner, b_owner):
    """True when some overlapping point can be seen past either face: just off it, no cube encloses the point."""
    for p in points:
        for n, owner in ((fa[4], a_owner), (fb[4], b_owner)):
            if not inside(p + n * 0.01, cubes, (owner,)):
                return True
    return False


def audit(model, every=False):
    geo = json.loads((ASSETS / 'geo/block' / f'{model}.geo.json').read_text())['minecraft:geometry'][0]
    anim_path = ASSETS / 'animations/block' / f'{model}.animation.json'
    anims = json.loads(anim_path.read_text())['animations'] if anim_path.exists() else {}
    hits = {}
    for name, t in poses(anims, every):
        cubes = world_cubes(geo, anims.get(name), t)
        facelists = [(c[0], list(faces(c))) for c in cubes]
        for i, (an, af) in enumerate(facelists):
            for bn, bf in facelists[i + 1:]:
                for fa in af:
                    for fb in bf:
                        key = (an, fa[0], bn, fb[0])
                        if key in hits:
                            continue
                        points = overlap(fa, fb)
                        if points and seen(points, fa, fb, cubes, an, bn):
                            hits[key] = (name or 'rest', t)
    return [(k, v) for k, v in hits.items()]


def main(args):
    every = '--all' in args
    models = [a for a in args if not a.startswith('--')]
    models = models or sorted(p.name.removesuffix('.geo.json') for p in (ASSETS / 'geo/block').glob('*.geo.json'))
    total = 0
    for model in models:
        hits = audit(model, every)
        total += len(hits)
        for (a, fa, b, fb), (anim, t) in hits:
            print(f'{model}: {a}.{fa} fights {b}.{fb} ({anim} at {t:.2f}s)')
    print(f'{total} fighting face pairs')
    return total


if __name__ == '__main__':
    sys.exit(1 if main(sys.argv[1:]) else 0)
