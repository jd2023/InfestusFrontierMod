#!/usr/bin/env python3
"""The Infected Timber guide plate: a living oak beside the same tree infected, and the logs cut from it.

Left, an oak of vanilla's oak log and leaves (read from the client jar, the leaves in the plains foliage colour); right,
the same blocks as Infected Logs under Blighted Leaves, drawn from the shipped models (art/gen_infected_timber.py) with
turns the blockstate can give them; between them, three cut Infected Logs, two lying along z and one across them. Every
block stands on a skin of grass, so the plate shows the full cube crown against the open dead one.

Run from the repository root: python3 art/gen_book_infected_timber.py
Writes src/main/resources/assets/infestusfrontier/textures/book/infected_timber_grove.png, 200x200 RGBA.
"""
import numpy as np
from PIL import Image

import book_scene as scene
import gen_infected_timber as gen
import gen_book_mineral_gizzard as book

OUT = book.ASSETS / 'textures/book/infected_timber_grove.png'
FOLIAGE = (0x77, 0xAB, 0x2F)  # vanilla's plains foliage colour, which tints the grey oak leaves
GRASS = (0x91, 0xBD, 0x59)
SKIN = 3  # how deep the ground is drawn, in sixteenths of a block

TRUNK = ((1, 0, 1), (1, 1, 1), (1, 2, 1))
CROWN = ([(x, y, z) for y in (2, 3) for x in range(3) for z in range(3) if (x, y, z) not in TRUNK]
         + [(1, 4, 1), (0, 4, 1), (2, 4, 1), (1, 4, 0), (1, 4, 2)])
LIVING_SHIFT = 4  # cells east of the infected oak: scene x is mirrored, so the living oak stands on the left


def tinted(ref, colour):
    texture = scene.texture(ref).astype(float)
    texture[..., :3] *= np.array(colour) / 255.0
    return texture.astype(np.uint8)


def cube(cell, sides, top=None, bottom=None, depth=16):
    """A cube in cell (i, j, k) with its own side, top and bottom textures (arrays), or only its top `depth` units."""
    i, j, k = cell
    faces = []
    for direction, (nx, ny, nz) in scene.DIRECTIONS.items():
        tex = {'up': top, 'down': bottom}.get(direction)
        tex = sides if tex is None else tex
        w, h = tex.shape[1], tex.shape[0]
        rows = h if direction in ('up', 'down') else h * depth / 16
        corners = scene.face_corners(direction, (0, 16 - depth, 0), (16, 16, 16))
        uvs = np.array([(0, 0), (w, 0), (w, rows), (0, rows)], dtype=float)
        world = np.array([(-(16 * i + x), 16 * j + y, 16 * k + z) for x, y, z in corners], dtype=float)
        faces.append((np.array([-nx, ny, nz], dtype=float), world, uvs, tex))
    return faces


def living_oak():
    log_side, log_end = scene.texture('minecraft:block/oak_log'), scene.texture('minecraft:block/oak_log_top')
    leaves = tinted('minecraft:block/oak_leaves', FOLIAGE)
    faces = []
    for x, y, z in TRUNK:
        faces += cube((x + LIVING_SHIFT, y, z), log_side, log_end, log_end)
    for x, y, z in CROWN:
        faces += cube((x + LIVING_SHIFT, y, z), leaves)
    return faces


def infected_oak():
    """The same tree in the shipped models, each block with a model and a quarter turn as the blockstate may pick."""
    faces = []
    logs = ('infected_log', 'infected_log_twig', 'infected_log_wound')
    for n, (x, y, z) in enumerate(TRUNK):
        faces += scene.block_faces(logs[n], (x, y, z), turn=90 * n)
    crowns = tuple(gen.LEAF_VARIANTS)
    for n, (x, y, z) in enumerate(CROWN):
        faces += scene.block_faces(crowns[n % len(crowns)], (x, y, z), turn=90 * (n % 4))
    return faces


def harvest():
    """Cut Infected Logs between the trees: two lying along z, one across them along x."""
    faces = scene.block_faces('infected_log', (3, 0, -1), tilt=90)
    faces += scene.block_faces('infected_log_stub', (3, 0, 0), tilt=90)
    faces += scene.block_faces('infected_log_twig', (3, 1, -1), turn=90, tilt=90)
    return faces


def ground():
    top = tinted('minecraft:block/grass_block_top', GRASS)
    side = scene.texture('minecraft:block/dirt')
    cells = [(i, -1, k) for i in range(-1, 8) for k in range(-2, 4)]
    return [f for c in cells for f in cube(c, side, top, side, SKIN)]


def main():
    book.YAW, book.PITCH = -24.0, 24.0
    faces = ground() + living_oak() + infected_oak() + harvest()
    scale = scene.scale_for([faces], 196, 196)
    out = Image.new('RGBA', (book.SIZE, book.SIZE), (0, 0, 0, 0))
    out.alpha_composite(scene.render(faces, 196, 196, scale), (2, 2))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    out.save(OUT, optimize=True)


if __name__ == '__main__':
    main()
