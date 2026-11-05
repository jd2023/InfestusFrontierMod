#!/usr/bin/env python3
"""The Larder's guide illustration: its model beside a Mixed Craw over a pool of living substrate, with the Sealing
Resin that turns one into the other laid in front. Both models are drawn by `gen_book_mineral_gizzard`'s renderer at one
scale, each turned a little towards the other, so the Larder's waxed amber hide, ivory latches, resin seal and three
pockets stand against the Craw's bog-green hide, ivory cross and four.

Run from the repository root: python3 art/gen_book_larder.py
Writes src/main/resources/assets/infestusfrontier/textures/book/larder.png, 200x200 RGBA.
"""
from PIL import Image

import gen_book_mineral_gizzard as book

OUT = book.ASSETS / 'textures/book/larder.png'


def model(name, yaw, size):
    book.YAW, book.PITCH, book.MODEL_SIZE = yaw, 26.0, size
    return book.render_model(name)


def main():
    out = Image.new('RGBA', (book.SIZE, book.SIZE), (0, 0, 0, 0))
    out.alpha_composite(book.pool())
    craw = model('mixed_craw', 34.0, 104)
    larder = model('larder', -30.0, 116)
    out.alpha_composite(craw, (-8, 22))
    out.alpha_composite(larder, (book.SIZE - larder.width + 6, 30))
    resin = Image.open(book.ASSETS / 'textures/item/sealing_resin.png').convert('RGBA')
    out.alpha_composite(resin, ((book.SIZE - resin.width) // 2, 152))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    out.save(OUT, optimize=True)


if __name__ == '__main__':
    main()
