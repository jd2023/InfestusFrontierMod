#!/usr/bin/env python3
"""The Shaping Palm's Rote guide plate: the Palm's model over a pool of living substrate, and under it the Rote tray as
the screen draws it, four wells cut into an ivory plate. A cake, the result of the pattern, lies in the first well; the
other three show the engraved marks of the remainder wells (bucket, bottle, bowl) from the GUI sheet.

Run from the repository root: python3 art/gen_book_shaping_palm_rote.py
Writes src/main/resources/assets/infestusfrontier/textures/book/shaping_palm_rote.png, 200x200 RGBA. The wells and marks
are cropped from textures/gui/organ.png; the cake sprite is vanilla's, read from Minecraft's client jar (art/book_scene.py).
"""
from PIL import Image, ImageDraw

import book_scene as scene
import gen_book_mineral_gizzard as book
import gen_gui_sheet as sheet

OUT = book.ASSETS / 'textures/book/shaping_palm_rote.png'
ZOOM = 2
PITCH = 40  # between the wells of the tray, in book pixels
PLATE = (14, 142, 172, 50)  # the ivory tray plate


def crop(gui, x, y, w, h):
    return gui.crop((x, y, x + w, y + h)).resize((w * ZOOM, h * ZOOM), Image.NEAREST)


def tray(gui):
    """The tray plate with its four wells, the cake in the first and the remainder marks in the others."""
    out = Image.new('RGBA', (book.SIZE, book.SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(out)
    x, y, w, h = PLATE
    d.rounded_rectangle((x, y, x + w - 1, y + h - 1), 4, fill=sheet.IVORY[2] + (255,), outline=sheet.OUTLINE + (255,))
    d.rounded_rectangle((x + 1, y + 1, x + w - 2, y + h - 2), 3, outline=sheet.FLESH[2] + (255,))
    well = crop(gui, sheet.SLOT[0], sheet.SLOT[1], 18, 18)
    left = x + (w - (3 * PITCH + well.width)) // 2
    top = y + (h - well.height) // 2
    for n in range(len(sheet.TRAY_MARKS)):
        at = (left + n * PITCH, top)
        out.alpha_composite(well, at)
        if n == 0:
            cake = scene.vanilla('textures/item/cake.png').resize((16 * ZOOM, 16 * ZOOM), Image.NEAREST)
            out.alpha_composite(cake, (at[0] + ZOOM, at[1] + ZOOM))
        else:
            mx, my = sheet.TRAY_MARKS[n]
            out.alpha_composite(crop(gui, mx, my, sheet.TRAY_MARK, sheet.TRAY_MARK), (at[0] + ZOOM, at[1] + ZOOM))
    return out


def main():
    gui = Image.open(book.ASSETS / 'textures/gui/organ.png').convert('RGBA')
    book.YAW, book.PITCH, book.MODEL_SIZE = -24.0, 30.0, 124
    out = Image.new('RGBA', (book.SIZE, book.SIZE), (0, 0, 0, 0))
    out.alpha_composite(book.pool())
    palm = book.render_model('shaping_palm')
    out.alpha_composite(palm, ((book.SIZE - palm.width) // 2, 4))
    out.alpha_composite(tray(gui))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    out.save(OUT, optimize=True)


if __name__ == '__main__':
    main()
