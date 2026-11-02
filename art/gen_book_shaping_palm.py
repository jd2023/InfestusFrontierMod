#!/usr/bin/env python3
"""The Shaping Palm's guide illustration: its model over a pool of living substrate, with the Membrane Sheet and the
Bone Plate it is made from laid out in front. The drawing is `gen_book_mineral_gizzard.plate`.

Run from the repository root: python3 art/gen_book_shaping_palm.py
Writes src/main/resources/assets/infestusfrontier/textures/book/shaping_palm.png, 200x200 RGBA.
"""
from gen_book_mineral_gizzard import ASSETS, plate

if __name__ == '__main__':
    plate('shaping_palm', ['membrane_sheet', 'bone_plate'], ASSETS / 'textures/book/shaping_palm.png')
