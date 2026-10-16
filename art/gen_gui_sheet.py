#!/usr/bin/env python3
"""The organ screens' shared GUI sheet (256x256), from one description.

Run from the repository root: python3 art/gen_gui_sheet.py [preview.png]
Deterministic: fixed seeds, no timestamps. Writes
  src/main/resources/assets/infestusfrontier/textures/gui/organ.png
The optional argument writes a 4x enlarged copy to look at.

Every organ screen draws its panel from this sheet. The regions below are mirrored by
ui/src/main/java/org/jd/infestusfrontier/ui/Sheet.java; change both together.

  0,0     176x166  panel: ivory membrane, plum flesh rim, the 36 player inventory wells, studs and veins
  176,0   18x18    one slot well, for an organ's own slots
  176,20  20x56    tank frame; the fill column goes inside it at +2,+2
  200,22  16x52    biomass column, full (the screen draws its lower part)
  220,22  16x1     biomass surface line
  176,80  52x16    button
  176,96  52x16    button, hovered or focused (ivory rim)
  176,112 52x16    button, disabled (sunken, grey)
  176,128 44x12    progress vein, empty
  176,144 44x12    progress vein, full (the screen draws its left part)
  176,160 4 x 9x9  status glyphs at a 10 px pitch: idle ring, working arrow, blocked cross, paused bars
"""
import math
import random
import sys
from pathlib import Path
from PIL import Image

OUT = Path('src/main/resources/assets/infestusfrontier/textures/gui/organ.png')
SHEET = 256
W, H = 176, 166
# the substrate's plum flesh, the chitin's ivory, the biomass green
FLESH = [(96, 44, 64), (124, 60, 78), (150, 82, 92), (176, 110, 112), (198, 140, 134)]
IVORY = [(150, 138, 112), (188, 176, 148), (222, 212, 186), (242, 236, 216)]
GREEN = [(44, 92, 52), (96, 190, 78), (170, 255, 120), (228, 255, 198)]
GREY = [(84, 76, 80), (110, 102, 104), (132, 124, 124)]
OUTLINE = (52, 30, 40)
WELL = (76, 44, 56)
PIT = (42, 24, 34)

INVENTORY = [(8 + c * 18, 84 + r * 18) for r in range(3) for c in range(9)] + [(8 + c * 18, 142) for c in range(9)]
SLOT = (176, 0)
TANK_WELL = (176, 20, 20, 56)
TANK_FILL = (200, 22, 16, 52)
SURFACE = (220, 22)
BUTTON = (176, 80)
BUTTON_HOVER = (176, 96)
BUTTON_OFF = (176, 112)
BUTTON_SIZE = (52, 16)
VEIN_EMPTY = (176, 128, 44, 12)
VEIN_FULL = (176, 144, 44, 12)
GLYPHS = (176, 160)
# where screens write and put their own parts; veins and pores keep out of these
TEXT = [(6, 4, 164, 12), (6, 70, 164, 12)]

GLYPH_ROWS = [
    ["..#####..", ".#.....#.", "#.......#", "#.......#", "#.......#", "#.......#", "#.......#", ".#.....#.", "..#####.."],
    ["....#....", "....##...", "....###..", "########.", "#########", "########.", "....###..", "....##...", "....#...."],
    ["##.....##", "###...###", ".###.###.", "..#####..", "...###...", "..#####..", ".###.###.", "###...###", "##.....##"],
    [".###.###."] * 9,
]


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def put(img, x, y, colour):
    img.putpixel((x, y), tuple(colour) + (255,))


def noise(seed, w, h, cell):
    """Smooth value noise in 0..1."""
    rng = random.Random(seed)
    gw, gh = w // cell + 2, h // cell + 2
    grid = [[rng.random() for _ in range(gw)] for _ in range(gh)]

    def at(x, y):
        gx, gy = x / cell, y / cell
        ix, iy = int(gx), int(gy)
        fx, fy = gx - ix, gy - iy
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        top = grid[iy][ix] + (grid[iy][ix + 1] - grid[iy][ix]) * fx
        bottom = grid[iy + 1][ix] + (grid[iy + 1][ix + 1] - grid[iy + 1][ix]) * fx
        return top + (bottom - top) * fy
    return at


def in_rects(x, y, rects, grow=0):
    return any(rx - grow <= x < rx + rw + grow and ry - grow <= y < ry + rh + grow for rx, ry, rw, rh in rects)


def corner_cut(x, y, w, h):
    dx, dy = min(x, w - 1 - x), min(y, h - 1 - y)
    return dx + dy < 2


def panel(img):
    rng = random.Random(1001)
    tone, grain, rim = noise(1002, W, H, 12), noise(1003, W, H, 3), noise(1004, W, H, 5)
    for y in range(H):
        for x in range(W):
            if corner_cut(x, y, W, H):
                continue
            d = min(x, y, W - 1 - x, H - 1 - y)
            lit = x + y < W - 1 - x + H - 1 - y
            if d == 0 or (d == 1 and corner_cut(x - 1, y - 1, W - 2, H - 2)):
                put(img, x, y, OUTLINE)
            elif d <= 3:
                base = FLESH[2] if d == 2 else (FLESH[4] if lit else FLESH[0]) if d == 1 else (FLESH[1] if lit else FLESH[3])
                put(img, x, y, mix(base, FLESH[1], 0.35 * rim(x, y)))
            elif d == 4:
                put(img, x, y, mix(IVORY[0], OUTLINE, 0.25))
            else:
                t = 0.55 * tone(x, y) + 0.45 * grain(x, y)
                put(img, x, y, mix(IVORY[2], IVORY[3], (t - 0.5) * 1.2) if t > 0.5 else mix(IVORY[2], IVORY[1], (0.5 - t) * 0.5))
    keep_out = TEXT + [(x - 2, y - 2, 20, 20) for x, y in INVENTORY]
    vein = mix(IVORY[2], FLESH[2], 0.55)
    for _ in range(30):
        side = rng.randrange(4)
        x, y = [(5, rng.randrange(8, H - 8)), (W - 6, rng.randrange(8, H - 8)),
                (rng.randrange(8, W - 8), 5), (rng.randrange(8, W - 8), H - 6)][side]
        angle = [0, math.pi, math.pi / 2, -math.pi / 2][side] + rng.uniform(-0.5, 0.5)
        fx, fy = float(x), float(y)
        length = rng.randrange(10, 34)
        for step in range(length):
            px, py = round(fx), round(fy)
            if not (5 <= px < W - 5 and 5 <= py < H - 5) or in_rects(px, py, keep_out):
                break
            here = img.getpixel((px, py))[:3]
            put(img, px, py, mix(here, vein, 0.9 * (1 - step / length) + 0.1))
            angle += rng.uniform(-0.45, 0.45)
            fx += math.cos(angle)
            fy += math.sin(angle)
    for _ in range(90):
        x, y = rng.randrange(6, W - 6), rng.randrange(6, H - 6)
        if not in_rects(x, y, keep_out, 1):
            put(img, x, y, IVORY[1])
    for x, y in [(3, 3), (W - 6, 3), (3, H - 6), (W - 6, H - 6)]:
        nodule(img, x, y, 3, 3)
    for n, (x, y) in enumerate(INVENTORY):
        well(img, x - 1, y - 1, 18, 18, WELL, 1100 + n)


def nodule(img, x, y, w, h):
    """An ivory chitin plate: lit from the upper left, shaded lower right, outlined."""
    for j in range(-1, h + 1):
        for i in range(-1, w + 1):
            edge = i in (-1, w) or j in (-1, h)
            if (i in (-1, w)) and (j in (-1, h)):
                continue
            if edge:
                put(img, x + i, y + j, mix(OUTLINE, FLESH[1], 0.4))
            elif i == 0 or j == 0:
                put(img, x + i, y + j, IVORY[3])
            elif i == w - 1 or j == h - 1:
                put(img, x + i, y + j, IVORY[0])
            else:
                put(img, x + i, y + j, IVORY[2])


def well(img, x, y, w, h, inner, seed=None):
    """A sunken rectangle: dark upper left edge, pale lower right edge."""
    rng = random.Random(seed)
    for j in range(h):
        for i in range(w):
            if i == 0 or j == 0:
                colour = OUTLINE
            elif i == w - 1 or j == h - 1:
                colour = IVORY[3]
            else:
                colour = mix(inner, PIT, 0.5) if i == 1 or j == 1 else inner
                if seed is not None and rng.random() < 0.12:
                    colour = mix(colour, FLESH[0], 0.35)
            put(img, x + i, y + j, colour)
    put(img, x + w - 1, y, IVORY[0])
    put(img, x, y + h - 1, IVORY[0])


def vein_mask(i, j, w, h):
    r = h / 2
    cx = min(max(i + 0.5, r), w - r)
    return math.hypot(i + 0.5 - cx, j + 0.5 - r) <= r


def chevron(i, j, h):
    return (i + abs(j - (h - 1) / 2) - 1) % 11 < 2 and 1 <= j <= h - 2


def progress(img):
    x0, y0, w, h = VEIN_EMPTY
    for j in range(-1, h + 1):
        for i in range(-1, w + 1):
            if vein_mask(i, j, w, h):
                continue
            if any(vein_mask(i + di, j + dj, w, h) for di in (-1, 0, 1) for dj in (-1, 0, 1)):
                put(img, x0 + i, y0 + j, OUTLINE if j < h / 2 else IVORY[3])
    for j in range(h):
        for i in range(w):
            if not vein_mask(i, j, w, h):
                continue
            empty = mix(WELL, PIT, 0.6) if j < 2 else WELL
            put(img, x0 + i, y0 + j, mix(empty, PIT, 0.7) if chevron(i, j, h) else empty)
            shade = abs(j - (h - 1) / 2) / (h / 2)
            full = mix(GREEN[2], GREEN[1], shade)
            if j == 2:
                full = GREEN[3]
            if chevron(i, j, h):
                full = GREEN[0]
            put(img, VEIN_FULL[0] + i, VEIN_FULL[1] + j, full)


def tank(img):
    x0, y0, w, h = TANK_WELL
    well(img, x0 + 1, y0 + 1, w - 2, h - 2, PIT)
    for i in range(w):
        put(img, x0 + i, y0, IVORY[3])
        put(img, x0 + i, y0 + h - 1, IVORY[1])
    for j in range(h):
        put(img, x0, y0 + j, mix(OUTLINE, FLESH[1], 0.4))
        put(img, x0 + w - 1, y0 + j, mix(OUTLINE, FLESH[1], 0.4))
    # ticks every quarter, inside the left edge of the frame
    for quarter in range(1, 4):
        y = y0 + h - 2 - quarter * (h - 4) // 4
        put(img, x0 + 1, y, IVORY[3])
        put(img, x0 + 2, y, IVORY[1])
    rng = random.Random(1010)
    fx, fy, fw, fh = TANK_FILL
    bubbles = {(rng.randrange(2, fw - 2), rng.randrange(3, fh - 1)) for _ in range(11)}
    for j in range(fh):
        for i in range(fw):
            colour = mix(GREEN[1], GREEN[0], j / (fh - 1))
            if i in (1, 2):
                colour = mix(colour, GREEN[2], 0.5)
            elif i >= fw - 2:
                colour = mix(colour, GREEN[0], 0.5)
            if (i, j) in bubbles:
                colour = GREEN[3]
            put(img, fx + i, fy + j, colour)
    for i in range(fw):
        put(img, SURFACE[0] + i, SURFACE[1], GREEN[3] if 1 <= i < fw - 1 else GREEN[2])


def glyphs(img):
    for n, rows in enumerate(GLYPH_ROWS):
        for j, row in enumerate(rows):
            for i, c in enumerate(row):
                if c == '#':
                    put(img, GLYPHS[0] + n * 10 + i, GLYPHS[1] + j, OUTLINE)


def button(img, origin, hovered, disabled=False):
    w, h = BUTTON_SIZE
    for j in range(h):
        for i in range(w):
            d = min(i, j, w - 1 - i, h - 1 - j)
            if min(i, w - 1 - i) + min(j, h - 1 - j) == 0:
                continue
            if d == 0:
                colour = IVORY[3] if hovered else OUTLINE
            elif d == 1:
                lit = (i == 1 or j == 1) and i < w - 2 and j < h - 2
                colour = (GREY[2] if lit else PIT) if disabled else (FLESH[3] if lit else PIT)
            else:
                colour = mix(GREY[1], GREY[0], j / (h - 1)) if disabled else mix(FLESH[1], FLESH[0], j / (h - 1))
            put(img, origin[0] + i, origin[1] + j, colour)


def main():
    img = Image.new('RGBA', (SHEET, SHEET), (0, 0, 0, 0))
    panel(img)
    well(img, SLOT[0], SLOT[1], 18, 18, WELL, 1099)
    progress(img)
    tank(img)
    glyphs(img)
    button(img, BUTTON, False)
    button(img, BUTTON_HOVER, True)
    button(img, BUTTON_OFF, False, True)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT, optimize=True)
    if len(sys.argv) > 1:
        img.resize((SHEET * 4, SHEET * 4), Image.NEAREST).save(sys.argv[1])


if __name__ == '__main__':
    main()
