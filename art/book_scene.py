#!/usr/bin/env python3
"""A small block diorama for the guide plates, drawn from the shipped block models.

`gen_book_mineral_gizzard` draws one GeckoLib model; this module adds what a scene of several blocks needs: block
models and their blockstates read from `models/block/*.json` (elements, per-face UVs, the y turn of a multipart
entry), GeckoLib models placed in a cell, and the chest, whose model vanilla keeps in code, rebuilt from vanilla's chest
texture. Every face goes through the same painter, view and light as the single-model plates, so the plates match.

Scene space is the file space of the geo renderer: file x is the block x mirrored (a viewer in front of a north-facing
block sees west on the right), y is up, z is block z. A cell (i, j, k) is block x i, height j, block z k.

The vanilla chest texture and the cake sprite are read from Minecraft's client jar, not committed; `client_jar()`
finds it in the Gradle cache, or take it from the MINECRAFT_CLIENT_JAR environment variable.
"""
import json
import os
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

import gen_book_mineral_gizzard as book

ASSETS = book.ASSETS
SUBSTRATE = 'infestusfrontier:block/living_substrate_mature_5'
DIRECTIONS = {
    'up': (0, 1, 0), 'down': (0, -1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (-1, 0, 0)}
_textures = {}


def client_jar():
    """Minecraft 1.21.1's client jar: $MINECRAFT_CLIENT_JAR, else the one NeoForm leaves in the Gradle cache."""
    found = os.environ.get('MINECRAFT_CLIENT_JAR')
    if found:
        return Path(found)
    cache = Path.home() / '.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar'
    if not cache.is_file():
        raise SystemExit('Minecraft 1.21.1 client jar not found; set MINECRAFT_CLIENT_JAR')
    return cache


def vanilla(path):
    """One vanilla asset, such as 'textures/item/cake.png', as an RGBA image."""
    with zipfile.ZipFile(client_jar()) as jar, jar.open('assets/minecraft/' + path) as f:
        return Image.open(f).convert('RGBA')


def texture(ref):
    """A block texture by id as an RGBA array; an animated strip gives its first frame."""
    if ref not in _textures:
        namespace, _, path = ref.partition(':')
        image = vanilla(f'textures/{path}.png') if namespace == 'minecraft' else Image.open(ASSETS / f'textures/{path}.png').convert('RGBA')
        if image.height > image.width:
            image = image.crop((0, 0, image.width, image.width))
        _textures[ref] = np.array(image)
    return _textures[ref]


def model(name):
    return json.loads((ASSETS / f'models/block/{name}.json').read_text())


def turn_y(point, degrees, centre=8.0):
    """A block-space point turned clockwise seen from above by a multiple of 90 degrees about the cell's axis."""
    x, y, z = point
    for _ in range((degrees // 90) % 4):
        x, z = centre - (z - centre), centre + (x - centre)
    return (x, y, z)


def face_corners(direction, f, t):
    (x0, y0, z0), (x1, y1, z1) = f, t
    return {
        'up': [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        'down': [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
        'north': [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        'south': [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        'west': [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        'east': [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
    }[direction]


def default_uv(direction, f, t):
    (x0, y0, z0), (x1, y1, z1) = f, t
    return {
        'up': [x0, z0, x1, z1], 'down': [x0, 16 - z1, x1, 16 - z0],
        'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0], 'south': [x0, 16 - y1, x1, 16 - y0],
        'west': [z0, 16 - y1, z1, 16 - y0], 'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0],
    }[direction]


def block_faces(name, cell, turn=0, textures=None):
    """Every face of block model `name` in cell (i, j, k), as (normal, corners, uvs, texture) in scene space."""
    data = model(name)
    refs = dict(data.get('textures', {}), **(textures or {}))
    i, j, k = cell
    faces = []
    for element in data['elements']:
        f, t = element['from'], element['to']
        for direction, face in element['faces'].items():
            tex = texture(refs[face['texture'][1:]])
            u0, v0, u1, v1 = face.get('uv') or default_uv(direction, f, t)
            sx, sy = tex.shape[1] / 16, tex.shape[0] / 16
            uvs = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
            shift = (face.get('rotation', 0) // 90) % 4
            uvs = uvs[-shift:] + uvs[:-shift] if shift else uvs
            uvs = np.array([(u * sx, v * sy) for u, v in uvs])
            corners = [turn_y(p, turn) for p in face_corners(direction, f, t)]
            nx, ny, nz = turn_y(tuple(DIRECTIONS[direction]), turn, 0.0)
            world = np.array([(-(16 * i + x), 16 * j + y, 16 * k + z) for x, y, z in corners], dtype=float)
            faces.append((np.array([-nx, ny, nz], dtype=float), world, uvs, tex))
    return faces


def geo_faces(name, cell, texture_name=None):
    """The GeckoLib model `name` standing in cell (i, j, k), as scene-space faces. Its origin is the cell's floor centre."""
    geo = json.loads((ASSETS / f'geo/block/{name}.geo.json').read_text())['minecraft:geometry'][0]
    tex = np.array(Image.open(ASSETS / f'textures/block_entity/{texture_name or name}.png').convert('RGBA'))
    i, j, k = cell
    shift = np.array([-(16 * i + 8), 16 * j, 16 * k + 8], dtype=float)
    return [(normal, corners + shift, uvs, tex) for normal, corners, uvs in book.world_faces(geo)]


def chest_faces(cell):
    """A vanilla chest facing north in cell (i, j, k): body, lid and latch boxes with the chest texture's own layout."""
    tex = np.array(vanilla('textures/entity/chest/normal.png'))
    cubes = [
        {'origin': [-7, 0, -7], 'size': [14, 10, 14], 'uv': [0, 19]},
        {'origin': [-7, 9, -7], 'size': [14, 5, 14], 'uv': [0, 0]},
        {'origin': [-1, 7, -8], 'size': [2, 4, 1], 'uv': [0, 0]},
    ]
    i, j, k = cell
    shift = np.array([-(16 * i + 8), 16 * j, 16 * k + 8], dtype=float)
    faces = []
    for cube in cubes:
        for normal, corners, uvs in book.cube_faces(cube, 64):
            faces.append((normal, corners + shift, uvs, tex))
    return faces


def block_faces_of_cube(cell, depth=16, ref=SUBSTRATE):
    """A cube of `ref` in cell (i, j, k), or only its top `depth` units: a skin of ground."""
    i, j, k = cell
    faces = []
    tex = texture(ref)
    w, h = tex.shape[1], tex.shape[0]
    for direction in DIRECTIONS:
        corners = face_corners(direction, (0, 16 - depth, 0), (16, 16, 16))
        nx, ny, nz = DIRECTIONS[direction]
        rows = h if direction in ('up', 'down') else h * depth / 16
        uvs = np.array([(0, 0), (w, 0), (w, rows), (0, rows)], dtype=float)
        world = np.array([(-(16 * i + x), 16 * j + y, 16 * k + z) for x, y, z in corners], dtype=float)
        faces.append((np.array([-nx, ny, nz], dtype=float), world, uvs, tex))
    return faces


def vein_faces(cell, east, west, kind='item_vein'):
    """An Item Vein cell joined to its east and/or west neighbour, by `item_vein`'s own multipart entries."""
    faces = block_faces('vein_corners', cell)
    faces += block_faces(f'{kind}_center' if east or west else 'vein_center_skin', cell)
    for side, joined in (('north', False), ('east', east), ('south', False), ('west', west)):
        faces += block_faces(f'{kind}_arm_{side}' if joined else f'vein_arm_skin_{side}', cell)
    return faces


def mouth_faces(cell, mode, east, west):
    """A Mouth cell in `mode` with Item Veins on its east and/or west side."""
    faces = block_faces(f'mouth_top_{mode}', cell)
    for side, turn, joined in (('north', 0, False), ('east', 90, east), ('south', 180, False), ('west', 270, west)):
        faces += block_faces('mouth_stub_open' if joined else 'mouth_stub_skin', cell, turn)
    return faces


def capsule_tree_faces(cell, height=4):
    """A Capsule Tree standing on a Mouth: root, trunk with collars and a bud, as the world builds it."""
    i, j, k = cell
    faces = block_faces('capsule_stem_root', (i, j, k))
    for step in range(1, height - 1):
        faces += block_faces('capsule_stem_trunk', (i, j + step, k)) + block_faces('capsule_stem_collar', (i, j + step, k))
    return faces + block_faces('capsule_stem_bud', (i, j + height - 1, k))


def scale_for(face_sets, width, height):
    """Pixels per unit that fit every set in a width x height box."""
    scales = []
    for faces in face_sets:
        x0, x1, y0, y1 = book.bounds(faces)
        scales.append(min(width / (x1 - x0), height / (y1 - y0)))
    return min(scales)


render = book.render_faces
