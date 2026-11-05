#!/usr/bin/env python3
"""The Shaping Palm's supply guide plate, in two panels. Above, a Capsule Tree on a Mouth, three Item Veins and a Mouth
with a Palm on it; below, a chest on a Mouth, three Item Veins and a Mouth with a Palm on it. Every block is a shipped
model (art/book_scene.py) on living substrate, so the plate shows the two supplies the default demo builds with the
veins as the only link between a store and its Palm.

Run from the repository root: python3 art/gen_book_shaping_palm_supply.py
Writes src/main/resources/assets/infestusfrontier/textures/book/shaping_palm_supply.png, 200x200 RGBA.
"""
import book_scene as scene
import gen_book_mineral_gizzard as book
from PIL import Image

OUT = book.ASSETS / 'textures/book/shaping_palm_supply.png'
PANELS = [(196, 108), (196, 84)]  # the tree's panel is the taller
LENGTH = 4  # cells from the store's Mouth to the Palm's Mouth
TREE_HEIGHT = 3
SKIN = 5  # how deep the ground is drawn, in sixteenths of a block


def line(tree):
    """One panel: the store over its Mouth at the left, veins, the Palm over its Mouth at the right. The strip runs along
    block x, so the left end is the high x."""
    x = lambda u: LENGTH - 1 - u
    cells = [(i, 0, k) for i in range(-1, LENGTH + 1) for k in range(3) if not (0 <= i < LENGTH and k == 1)]
    faces = [f for c in cells for f in scene.block_faces_of_cube(c, SKIN)]
    faces += scene.mouth_faces((x(0), 0, 1), 'both', False, True)
    for u in range(1, LENGTH - 1):
        faces += scene.vein_faces((x(u), 0, 1), True, True)
    faces += scene.mouth_faces((x(LENGTH - 1), 0, 1), 'both', True, False)
    faces += scene.geo_faces('shaping_palm', (x(LENGTH - 1), 1, 1))
    if tree:
        faces += scene.capsule_tree_faces((x(0), 1, 1), TREE_HEIGHT)
        faces += scene.geo_faces('item_capsule', (x(0), 2, 0))
    else:
        faces += scene.chest_faces((x(0), 1, 1))
    return faces


def main():
    book.YAW, book.PITCH = -22.0, 34.0
    panels = [line(True), line(False)]
    scale = min(scene.scale_for([faces], *box) for faces, box in zip(panels, PANELS))
    out = Image.new('RGBA', (book.SIZE, book.SIZE), (0, 0, 0, 0))
    y = 2
    for faces, (width, height) in zip(panels, PANELS):
        out.alpha_composite(scene.render(faces, width, height, scale), (2, y))
        y += height + 2
    OUT.parent.mkdir(parents=True, exist_ok=True)
    out.save(OUT, optimize=True)


if __name__ == '__main__':
    main()
