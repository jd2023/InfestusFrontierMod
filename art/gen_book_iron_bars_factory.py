#!/usr/bin/env python3
"""The iron-bars factory's guide plate: the line the default demo builds (south, x 0 to 31), drawn left to right as the
route runs. Larder, Mineral Gizzard, Washing Kidney, Bio-Furnace, Shaping Palm and the output chest stand on one strip of
living substrate, each a shipped model drawn by art/book_scene.py at one scale. Under them run the lines the demo lays,
coloured from the lumen textures of the shipped veins: items (pink) on the one Item Vein bus, biomass (green) from the
Reservoir, water (blue) from the pool's Junction up to the Kidney. The bus is drawn as three lanes of the demo's
filtered Mouths: raw iron, Tailings, ingots and bars hop from organ to organ; the Gizzard's dust skips the Kidney to
reach the furnace, as the Kidney's own dust does; the Gizzard's Rock Residue and the Kidney's washed Tailings end in the
waste chest. The columns are evenly spaced; the demo's own hops are 4 to 6 blocks long.

Run from the repository root: python3 art/gen_book_iron_bars_factory.py
Writes src/main/resources/assets/infestusfrontier/textures/book/iron_bars_factory.png, 200x200 RGBA. Labels use
Minecraft's own font sheet, read from the client jar with the vanilla chest and item sprites (art/book_scene.py).
"""
import numpy as np
from PIL import Image, ImageDraw

import book_scene as scene
import gen_book_mineral_gizzard as book

OUT = book.ASSETS / 'textures/book/iron_bars_factory.png'
PITCH = 32  # between the columns, in book pixels
LEFT = 20  # the first column's centre
COLUMNS = [LEFT + PITCH * n for n in range(6)]
NAMES = ['Larder', 'Gizzard', 'Kidney', 'Furnace', 'Palm', 'Chest']
MODELS = [('geo', 'larder'), ('geo', 'mineral_gizzard'), ('geo', 'washing_kidney'), ('geo', 'bio_furnace'),
          ('geo', 'shaping_palm'), ('chest', None)]
ORGAN_BOX = (34, 40)
STRIP = (60, 70)  # the flesh the organs stand on, top and bottom
BIOMASS_Y, TRUNK_X = 76, 3
LANES = [96, 114, 132]  # the item lanes: the chain, the dust, the waste
LOWEST = [0, 2, 2, 1, 0, 0]  # the deepest lane each column's tap reaches
WASTE = (112, 120, 140)  # the waste chest's left edge, top and bottom
POOL = (168, 198)  # the water pool's top and bottom
ITEM_TAP, BIOMASS_RISE = -10, 6  # each line's x offset from a column's centre; the water rises at the Kidney's centre
INK = (62, 38, 30, 255)
OUTLINE = (30, 14, 22, 255)
FLESH = (104, 44, 64, 255)
_font = {}


def tap(column):
    return COLUMNS[column] + ITEM_TAP


def lumen(name):
    """The mean colour of a vein's lumen texture: what flows in it."""
    tex = scene.texture(f'infestusfrontier:block/vein_lumen_{name}')
    return tuple(int(c) for c in tex[..., :3].reshape(-1, 3).mean(axis=0))


def glyphs():
    if not _font:
        sheet = np.array(scene.vanilla('textures/font/ascii.png'))
        _font['sheet'] = sheet
        _font['cell'] = sheet.shape[0] // 16
    return _font


def text(out, at, string, colour=INK, bold=False):
    """`string` in Minecraft's font at 1x, its top left at `at`; bold draws each glyph twice, a pixel apart, as the game
    does. Returns the width."""
    font = glyphs()
    sheet, cell = font['sheet'], font['cell']
    x, y = at
    for ch in string:
        if ch == ' ':
            x += 4
            continue
        gx, gy = (ord(ch) % 16) * cell, (ord(ch) // 16) * cell
        glyph = sheet[gy:gy + cell, gx:gx + cell, 3] > 0
        columns = np.where(glyph.any(axis=0))[0]
        if not len(columns):
            continue
        for dx in (0, 1) if bold else (0,):
            for row, col in zip(*np.where(glyph)):
                px = x + col - columns[0] + dx
                if 0 <= px < out.width and 0 <= y + row < out.height:
                    out.putpixel((px, y + row), colour)
        x += columns[-1] - columns[0] + 2 + bold
    return x - at[0] - 1


def width(string):
    return text(Image.new('RGBA', (400, 16)), (0, 0), string, bold=True)


def label(out, centre, y, string):
    w = width(string)
    text(out, (min(max(centre - w // 2, 1), book.SIZE - w - 1), y), string, bold=True)


def organ(kind, name):
    return scene.chest_faces((0, 0, 0)) if kind == 'chest' else scene.geo_faces(name, (0, 0, 0))


def reservoir():
    """The Reservoir cell on its own, as the demo places it: base and lid, and on each side a foot, pane, rim, rib and knuckle."""
    faces = scene.block_faces('reservoir_cell_base', (0, 0, 0)) + scene.block_faces('reservoir_cell_lid_0', (0, 0, 0))
    for turn in (0, 90, 180, 270):
        for part in ('foot', 'pane_low_0', 'rim', 'rib', 'knuckle'):
            faces += scene.block_faces(f'reservoir_cell_{part}', (0, 0, 0), turn)
    return faces


def item_icon(name):
    """An item's 16 px icon: vanilla's own sprite, or the mod's icon reduced."""
    if name.startswith('minecraft:'):
        short = name.split(':')[1]
        return scene.vanilla('textures/block/iron_bars.png' if short == 'iron_bars' else f'textures/item/{short}.png')
    return book.item_icon(name).resize((16, 16), Image.LANCZOS)


def line(d, p0, p1, colour, thick=4):
    """A vein drawn as the lumen colour in a dark wall, so it reads on the strip and on the page."""
    (x0, y0), (x1, y1) = p0, p1
    r = thick // 2
    box = (min(x0, x1) - r - 1, min(y0, y1) - r - 1, max(x0, x1) + r, max(y0, y1) + r)
    d.rectangle(box, fill=OUTLINE)
    inner = (box[0] + 1, box[1] + 1, box[2] - 1, box[3] - 1)
    d.rectangle(inner, fill=colour + (255,))


def arrow(d, tip, direction, colour):
    x, y = tip
    pts = {'right': [(x, y), (x - 5, y - 4), (x - 5, y + 4)], 'up': [(x, y), (x - 4, y + 5), (x + 4, y + 5)]}[direction]
    d.polygon(pts, fill=colour + (255,), outline=OUTLINE)


def main():
    book.YAW, book.PITCH = -22.0, 28.0
    items, biomass, water = lumen('item'), lumen('biomass'), lumen('fluid')
    sets = [organ(kind, name) for kind, name in MODELS]
    scale = scene.scale_for(sets, *ORGAN_BOX)
    out = Image.new('RGBA', (book.SIZE, book.SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(out)
    top = STRIP[1] - 4
    kidney = COLUMNS[2]

    # the strip of living substrate under the organs, and the pool of water under the Kidney
    d.rounded_rectangle((2, STRIP[0], 197, STRIP[1]), 6, fill=(72, 28, 46, 255))
    d.rounded_rectangle((4, STRIP[0] + 2, 195, STRIP[1] - 2), 5, fill=FLESH)
    d.rectangle((kidney - 22, POOL[0], kidney + 22, POOL[1]), fill=(112, 112, 116, 255), outline=OUTLINE)
    d.rectangle((kidney - 19, POOL[0] + 3, kidney + 19, POOL[1] - 3), fill=water + (255,), outline=(36, 64, 120, 255))

    # biomass: up from the Reservoir, along under the strip, into the four organs that draw it
    line(d, (TRUNK_X, BIOMASS_Y), (TRUNK_X, 150), biomass)
    line(d, (TRUNK_X, BIOMASS_Y), (COLUMNS[4] + BIOMASS_RISE, BIOMASS_Y), biomass)
    for x in COLUMNS[1:5]:
        line(d, (x + BIOMASS_RISE, top), (x + BIOMASS_RISE, BIOMASS_Y), biomass)
    line(d, (kidney, top), (kidney, POOL[0] - 14), water)

    # items: one tap under each organ, and the lanes the filtered Mouths send things along
    for column, lane in enumerate(LOWEST):
        line(d, (tap(column), top), (tap(column), LANES[lane]), items)
    hops = [(0, 0, 1), (0, 1, 2), (0, 3, 4), (0, 4, 5), (1, 1, 3), (2, 1, None)]
    for lane, start, end in hops:
        stop = tap(end) if end is not None else WASTE[0]
        line(d, (tap(start), LANES[lane]), (stop, LANES[lane]), items)
        arrow(d, (stop - 2, LANES[lane]), 'right', items)
    gaps = {0: tap(0) + 8, 1: tap(1) + 8, 2: kidney + 4, 3: tap(3) + 8, 4: tap(4) + 8}
    icons = [(0, 0, 'minecraft:raw_iron'), (0, 1, 'mineral_tailings'), (0, 3, 'minecraft:iron_ingot'),
             (0, 4, 'minecraft:iron_bars'), (1, 1, 'iron_dust'), (1, 2, 'iron_dust'), (2, 1, 'rock_residue'),
             (2, 2, 'mineral_tailings')]
    for lane, gap, name in icons:
        out.alpha_composite(item_icon(name), (gaps[gap], LANES[lane] - 8))

    # the organs, the waste chest, the Reservoir and the Junction
    for x, faces in zip(COLUMNS, sets):
        piece = scene.render(faces, ORGAN_BOX[0] + 8, ORGAN_BOX[1] + 8, scale)
        out.alpha_composite(piece, (x - piece.width // 2, STRIP[0] - piece.height + 12))
    chest = scene.chest_faces((0, 0, 0))
    piece = scene.render(chest, 36, 36, scene.scale_for([chest], 28, 28))
    out.alpha_composite(piece, (WASTE[0], (WASTE[1] + WASTE[2]) // 2 - 18 + LANES[2] - (WASTE[1] + WASTE[2]) // 2))
    big = scene.render(reservoir(), 40, 40, scene.scale_for([reservoir()], 40, 40))
    out.alpha_composite(big, (-2, 148))
    junction = scene.geo_faces('vascular_junction', (0, 0, 0))
    jr = scene.render(junction, 28, 28, scene.scale_for([junction], 28, 28))
    out.alpha_composite(jr, (kidney - 14, POOL[0] - 24))

    for n, (x, string) in enumerate(zip(COLUMNS, NAMES)):
        label(out, x, 2 + (n % 2) * 10, string)
    text(out, (6, 139), 'Biomass', bold=True)
    label(out, 29, 190, 'Reservoir')
    text(out, (166, 104), 'Items', bold=True)
    text(out, (WASTE[0] + 2, WASTE[2] + 6), 'Waste', bold=True)
    text(out, (kidney + 28, (POOL[0] + POOL[1]) // 2 - 4), 'Water', bold=True)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    out.save(OUT, optimize=True)


if __name__ == '__main__':
    main()
