#!/usr/bin/env python3
"""The Mixed Craw's guide illustration: its model beside a bulk Item Capsule pod over a pool of living substrate, with
the Elastic Gel that turns one into the other laid in front. Both models are drawn by `gen_book_mineral_gizzard`'s
renderer at one scale, each turned a little towards the other, so the Craw's bog-green hide, ivory ribs and four
pockets stand against the pod's plum hide and single socket.

Run from the repository root: python3 art/gen_book_mixed_craw.py
Writes src/main/resources/assets/infestusfrontier/textures/book/mixed_craw.png, 200x200 RGBA.
"""
from PIL import Image

import gen_book_mineral_gizzard as book

OUT = book.ASSETS / 'textures/book/mixed_craw.png'


def model(name, yaw, size):
    book.YAW, book.PITCH, book.MODEL_SIZE = yaw, 26.0, size
    return book.render_model(name)


def main():
    out = Image.new('RGBA', (book.SIZE, book.SIZE), (0, 0, 0, 0))
    out.alpha_composite(book.pool())
    pod = model('item_capsule', 34.0, 104)
    craw = model('mixed_craw', -30.0, 116)
    out.alpha_composite(pod, (-8, 22))
    out.alpha_composite(craw, (book.SIZE - craw.width + 6, 30))
    gel = Image.open(book.ASSETS / 'textures/item/elastic_gel.png').convert('RGBA')
    out.alpha_composite(gel, ((book.SIZE - gel.width) // 2, 152))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    out.save(OUT, optimize=True)


if __name__ == '__main__':
    main()
