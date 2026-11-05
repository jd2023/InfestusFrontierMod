#!/usr/bin/env python3
"""The Shaping Palm's guide illustration: its model over a pool of living substrate, with the Membrane Sheet and the
Bone Plate it is made from laid out in front. The drawing is `gen_book_mineral_gizzard.plate`, seen from the front on
the thumb's side so the splayed thumb, the four fingers and the studded plate in the cup all show.

Run from the repository root: python3 art/gen_book_shaping_palm.py
Writes src/main/resources/assets/infestusfrontier/textures/book/shaping_palm.png, 200x200 RGBA.
"""
import gen_book_mineral_gizzard as book

if __name__ == '__main__':
    book.YAW, book.PITCH = -24.0, 30.0
    book.plate('shaping_palm', ['membrane_sheet', 'bone_plate'], book.ASSETS / 'textures/book/shaping_palm.png')
