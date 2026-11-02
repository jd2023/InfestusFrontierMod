#!/usr/bin/env python3
"""The Washing Kidney's guide illustration: its model over a pool of living substrate, with a Tailings, the dust it
gives and the Rock Residue it settles to laid out in front. The drawing is `gen_book_mineral_gizzard.plate`.

Run from the repository root: python3 art/gen_book_washing_kidney.py
Writes src/main/resources/assets/infestusfrontier/textures/book/washing_kidney.png, 200x200 RGBA.
"""
from gen_book_mineral_gizzard import ASSETS, plate

if __name__ == '__main__':
    plate('washing_kidney', ['mineral_tailings', 'iron_dust', 'rock_residue'], ASSETS / 'textures/book/washing_kidney.png')
