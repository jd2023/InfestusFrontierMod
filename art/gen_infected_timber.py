"""Infected timber: Infected Log, Blighted Leaves and the Infected Planks old worlds keep.

    python3 art/gen_infected_timber.py

Deterministic: a rerun writes the same atlas, planks, models, blockstates, item models and guide art, byte for byte.

Every infected tree is the same two blocks drawn from one 64x64 atlas (textures/block/infected_timber.png), one texel
per model pixel like any 16x16 block. The trunk is one cross-section shared by every log model: charcoal bark plates
recessed one or two pixels, an ivory rib flush with the block on each face and a cleft four pixels deep, all within
the block, with the same section on all four sides, so a quarter turn, a stacked log, a lying log and the four logs
of a 2x2 trunk always meet on the same outline. Four log models dress that section differently (which plate shows
maroon tissue or a sealed scar, which cleft is black or maroon, and a hooked twig or a broken stub out of a cleft);
the blockstate picks one by position with a turn about the log's own axis. InfectedLogBlock.OCCLUSION is the same
section, so a block beside a groove keeps its face. The crown is three sparse cutout models of hooked dry twigs and
torn hanging membrane with most of the block left open, turned the same way; its item is a flat 16x16 sprite of the
same twigs and membrane. The planks are one untinted 16x16 tile in the same palette. The guide icon is drawn here
too. Nothing is tinted, ticks or is saved.
"""
import json
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path('src/main/resources/assets/infestusfrontier')
TEXTURES = ROOT / 'textures/block'
MODELS = ROOT / 'models/block'
ITEM_MODELS = ROOT / 'models/item'
STATES = ROOT / 'blockstates'
BOOK = ROOT / 'textures/book'
ITEM_TEXTURES = ROOT / 'textures/item'
NS = 'infestusfrontier:block/'
ATLAS = 64
CELL = 16

# The restrained palette: charcoal bark, ivory rib, black cleft, dull maroon tissue, grey-brown twig, dull membrane.
BARK_DARK = (30, 24, 22)
BARK = (46, 37, 32)
BARK_MID = (60, 48, 40)
BARK_LIGHT = (77, 63, 51)
CLEFT_BLACK = (13, 9, 10)
CLEFT = (24, 16, 17)
MAROON_DARK = (56, 22, 26)
MAROON = (80, 33, 35)
MAROON_LIGHT = (104, 48, 46)
IVORY_SHADOW = (146, 134, 108)
IVORY = (190, 180, 150)
IVORY_LIGHT = (214, 205, 176)
HEART = (88, 72, 58)
HEART_RING = (70, 56, 46)
TWIG_DARK = (38, 31, 27)
TWIG = (66, 56, 47)
TWIG_LIGHT = (88, 76, 63)
MEMBRANE_DARK = (74, 56, 54)
MEMBRANE = (104, 84, 78)
MEMBRANE_LIGHT = (126, 106, 94)
CLEAR = (0, 0, 0, 0)

# Atlas cells (column, row), 16x16 texels each.
BARK_CELLS = {'plain': (0, 0), 'tissue': (1, 0), 'scar': (2, 0)}
RIB_CELLS = {'high': (3, 0), 'low': (3, 2)}
CLEFT_CELLS = {'black': (0, 1), 'maroon': (1, 1)}
END_CELL = (2, 1)
TWIG_CELL = (3, 1)
MEMBRANE_CELLS = ((0, 2), (1, 2))
STUB_END_CELL = (2, 2)

# How deep each column of the north face is cut, west to east, in pixels: corner plates 2, plates 1, the rib 0 (flush
# with the block), the cleft 4. The other three faces are the same profile turned, so the section has 4-fold symmetry.
PROFILE = (2, 2, 1, 1, 0, 0, 1, 2, 4, 4, 4, 2, 1, 1, 2, 2)
RIB, CLEFT_DEPTH = 0, 4


def rgba(colour, alpha=255):
    return tuple(colour) + (alpha,)


# ----------------------------------------------------------------------------------------------------------------------
# Atlas


def bark_base(seed):
    """Charcoal plates in vertical strips, each broken by dark cracks into chunks lit on their top row. Wraps
    vertically, so a log stacked on any other log continues the strips."""
    rng = random.Random(seed)
    image = Image.new('RGBA', (CELL, CELL), rgba(BARK))
    px = image.load()
    x = 0
    for width in (3, 2, 3, 3, 2, 3):
        shade = BARK_MID if rng.random() < 0.5 else BARK
        cracks = sorted(rng.sample(range(CELL), 2))
        for y in range(CELL):
            for dx in range(width):
                px[x + dx, y] = rgba(shade)
        for crack in cracks:  # a two-row dark crack across the strip, the chunk above it lit on top
            for dx in range(width):
                px[x + dx, crack] = rgba(BARK_DARK)
                px[x + dx, (crack + 1) % CELL] = rgba(BARK_LIGHT if dx < width - 1 else shade)
        for y in range(CELL):  # the strip's right edge in shadow, in runs, never a full hairline
            if (y // 3 + x) % 2 == 0 and px[x + width - 1, y][:3] != BARK_DARK:
                px[x + width - 1, y] = rgba(BARK_DARK if shade == BARK else BARK)
        x += width
    return image


def bark_plain():
    return bark_base(7)


def bark_tissue():
    """A split in the bark of the widest plate (texels 12-15, the north face's west corner) with dull maroon beneath."""
    image = bark_base(7)
    px = image.load()
    lens = {5: (13, 14), 6: (12, 15), 7: (12, 15), 8: (12, 15), 9: (12, 15), 10: (13, 14)}
    for y, (x0, x1) in lens.items():
        for x in range(x0, x1 + 1):
            px[x, y] = rgba(MAROON)
    for x, y in ((13, 6), (14, 6), (13, 7)):
        px[x, y] = rgba(MAROON_LIGHT)
    for x, y in ((14, 8), (14, 9), (13, 9), (13, 10)):
        px[x, y] = rgba(MAROON_DARK)
    for x in (13, 14):  # the bark lips over the split
        px[x, 4] = rgba(BARK_DARK)
        px[x, 11] = rgba(BARK_DARK)
    return image


def bark_scar():
    """A sealed wound across the east plate (texels 0-4): a stepped black seam, its upper lip raised and lit, one
    maroon bead pressed out of it."""
    image = bark_base(7)
    px = image.load()
    seam = [(0, 9), (1, 9), (2, 8), (3, 8), (4, 7)]
    for x, y in seam:
        px[x, y] = rgba(CLEFT_BLACK)
        px[x, y + 1] = rgba(BARK_DARK)
        px[x, y - 1] = rgba(BARK_LIGHT)
    px[2, 9] = rgba(MAROON)
    px[3, 9] = rgba(MAROON_DARK)
    return image


def rib(band, joints):
    """Ivory bone in knuckled segments, a shadowed joint under a lit lip, a few coarse pits, and one band of bark
    grown over it, so the ribs read as bone breaking through the bark. Wraps vertically."""
    image = Image.new('RGBA', (CELL, CELL), rgba(IVORY))
    px = image.load()
    for y in range(CELL):
        for x in range(CELL):
            if y in joints:
                px[x, y] = rgba(IVORY_SHADOW)
            elif (y + 1) % CELL in joints:
                px[x, y] = rgba(IVORY_LIGHT)
    for x, y in ((10, joints[0] + 2), (11, joints[0] + 3), (2, joints[1] + 2), (3, joints[1] + 3)):
        px[x, y % CELL] = rgba(IVORY_SHADOW)
    top, bottom = band
    for y in range(top, bottom + 1):
        for x in range(CELL):
            px[x, y] = rgba(BARK_DARK if y in (top, bottom) else BARK_MID if (x // 2 + y) % 3 else BARK)
    for x in range(0, CELL, 3):  # the bark's ragged edge over the bone
        px[x, top - 1] = rgba(BARK_DARK)
    return image


def cleft(maroon):
    """The floor of a cleft, a sealed wound: near black with a seam of dark beads, or dull maroon tissue in folds.
    The floor shows texels 5-7; the cleft's walls show texels 2-3 and 12-13."""
    image = Image.new('RGBA', (CELL, CELL), rgba(CLEFT_BLACK))
    px = image.load()
    if maroon:
        for y in range(CELL):
            for x in range(4, 9):
                px[x, y] = rgba(MAROON if (y // 3) % 2 == 0 else MAROON_DARK)
            if y % 6 in (1, 2):
                px[6, y] = rgba(MAROON_LIGHT)
        for x, y in ((5, 5), (7, 11), (6, 14)):
            px[x, y] = rgba(CLEFT)
    else:
        for y in range(CELL):
            px[6, y] = rgba(CLEFT)
            if y % 5 in (2, 3):
                px[6, y] = rgba(MAROON_DARK)
                px[5, y] = rgba(CLEFT)
    for y in range(0, CELL, 4):  # the cleft's walls go dark in coarse runs
        for x in (2, 3, 12, 13):
            px[x, y] = rgba(CLEFT)
            px[x, y + 1] = rgba(CLEFT)
    return image


def end():
    """A cut end: a charcoal rim, two dull growth rings, a maroon core with two dark radial cracks."""
    image = Image.new('RGBA', (CELL, CELL), rgba(BARK_DARK))
    draw = ImageDraw.Draw(image)
    draw.rectangle((2, 2, 13, 13), fill=rgba(HEART))
    draw.rectangle((4, 4, 11, 11), outline=rgba(HEART_RING))
    draw.rectangle((5, 5, 10, 10), fill=rgba(HEART))
    draw.rectangle((6, 6, 9, 9), fill=rgba(MAROON))
    draw.rectangle((7, 7, 8, 8), fill=rgba(MAROON_DARK))
    for x, y in ((3, 3), (3, 4), (4, 3), (12, 12), (12, 11), (11, 12)):
        draw.point((x, y), fill=rgba(BARK_DARK))
    for x, y in ((9, 2), (9, 3), (10, 4), (2, 10), (3, 9)):
        draw.point((x, y), fill=rgba(HEART_RING))
    return image


def twig():
    """Grey-brown twig bark with dark nodes on a diagonal, so a one-pixel twig along any axis is not one flat colour."""
    image = Image.new('RGBA', (CELL, CELL))
    px = image.load()
    for y in range(CELL):
        for x in range(CELL):
            k = (x + y) % 5
            px[x, y] = rgba(TWIG_DARK if k == 0 else TWIG_LIGHT if k == 2 else TWIG)
    return image


# A torn membrane scrap: (left, top, width, height) in its cell and the rows each column hangs to, two columns at a time.
SCRAPS = {
    'wide': ((0, 2), (0, 0, 7, 9), (9, 7, 8, 6, 7, 4, 5)),
    'short': ((0, 2), (8, 0, 8, 7), (5, 7, 6, 3, 6, 7, 4, 2)),
    'tatter': ((1, 2), (0, 0, 4, 12), (12, 8, 11, 6)),
    'sheet': ((1, 2), (5, 0, 11, 10), (7, 10, 9, 8, 10, 9, 6, 5, 8, 6, 3)),
}
HOLES = {'wide': ((2, 3),), 'short': (), 'tatter': ((1, 5),), 'sheet': ((3, 3), (7, 5))}


def membranes():
    """The four scraps on two cells: a dark seam where each hangs from its twig, a ragged lower edge, a vein, a hole."""
    cells = {cell: Image.new('RGBA', (CELL, CELL), CLEAR) for cell in MEMBRANE_CELLS}
    for name, (cell, (left, top, width, height), hangs) in SCRAPS.items():
        px = cells[cell].load()
        for dx in range(width):
            bottom = min(hangs[dx], height)
            for dy in range(bottom):
                colour = MEMBRANE_LIGHT if (dx + dy // 3) % 4 == 0 else MEMBRANE
                if dy == 0:
                    colour = MEMBRANE_DARK
                elif dy == bottom - 1:
                    colour = MEMBRANE_DARK if dx % 2 else MEMBRANE
                px[left + dx, top + dy] = rgba(colour)
        vein = left + width // 2
        for dy in range(1, min(hangs[width // 2], height) - 1):  # one vein down the middle
            px[vein, top + dy] = rgba(MEMBRANE_DARK)
        for dx, dy in ((1, 1), (2, 1), (1, 2)):  # a maroon stain at the seam
            if dx < width:
                px[left + dx, top + dy] = rgba(MAROON)
        for hx, hy in HOLES[name]:  # a torn hole, two by two
            for dx in (0, 1):
                for dy in (0, 1):
                    px[left + hx + dx, top + hy + dy] = CLEAR
    return cells


def stub_end():
    """A broken branch end: ivory splinters round a maroon core, in the cell's top-left 4x4."""
    image = Image.new('RGBA', (CELL, CELL), CLEAR)
    px = image.load()
    rows = ((IVORY_LIGHT, IVORY, IVORY, IVORY_SHADOW), (IVORY, MAROON, MAROON_DARK, IVORY_SHADOW),
            (IVORY, MAROON_DARK, MAROON, IVORY_SHADOW), (IVORY_SHADOW, IVORY_SHADOW, IVORY, IVORY_SHADOW))
    for y, row in enumerate(rows):
        for x, colour in enumerate(row):
            px[x, y] = rgba(colour)
    return image


def atlas():
    image = Image.new('RGBA', (ATLAS, ATLAS), CLEAR)

    def put(cell, tile):
        image.paste(tile, (cell[0] * CELL, cell[1] * CELL))

    put(BARK_CELLS['plain'], bark_plain())
    put(BARK_CELLS['tissue'], bark_tissue())
    put(BARK_CELLS['scar'], bark_scar())
    put(RIB_CELLS['high'], rib((5, 7), (2, 11)))
    put(RIB_CELLS['low'], rib((12, 14), (4, 9)))
    put(CLEFT_CELLS['black'], cleft(False))
    put(CLEFT_CELLS['maroon'], cleft(True))
    put(END_CELL, end())
    put(TWIG_CELL, twig())
    for cell, tile in membranes().items():
        put(cell, tile)
    put(STUB_END_CELL, stub_end())
    return image


def planks():
    """Four charcoal boards with dark seams, staggered joints, coarse grain and one maroon-stained knot; untinted."""
    rng = random.Random(11)
    image = Image.new('RGBA', (CELL, CELL))
    px = image.load()
    for board in range(4):
        shade = (BARK_MID, BARK, BARK_MID, BARK_LIGHT)[board]
        joint = (5, 12, 2, 9)[board]
        for y in range(board * 4, board * 4 + 4):
            for x in range(CELL):
                colour = shade
                if y == board * 4 + 3:
                    colour = BARK_DARK
                elif x == joint:
                    colour = BARK_DARK
                elif y == board * 4:
                    colour = BARK_LIGHT if shade != BARK_LIGHT else HEART
                px[x, y] = rgba(colour)
        for _ in range(2):  # grain, two texels long
            gx, gy = rng.randrange(0, CELL - 2), board * 4 + rng.randrange(1, 3)
            if joint not in (gx, gx + 1):
                px[gx, gy] = px[gx + 1, gy] = rgba(BARK_DARK if shade != BARK else BARK_MID)
    for x, y in ((10, 5), (11, 5), (10, 6)):
        px[x, y] = rgba(MAROON_DARK)
    px[11, 6] = rgba(MAROON)
    return image


# ----------------------------------------------------------------------------------------------------------------------
# Geometry


def uv(cell, u0, v0, u1, v1):
    """Texels (u0, v0)-(u1, v1) of an atlas cell in model UV units: 16 units span the 64-texel atlas, 4 per cell."""
    scale = 16 * CELL / ATLAS / CELL
    col, row = cell
    return [round(col * 4 + u0 * scale, 4), round(row * 4 + v0 * scale, 4),
            round(col * 4 + u1 * scale, 4), round(row * 4 + v1 * scale, 4)]


def face_texels(direction, f, t):
    """The texels a face shows at one texel per pixel, placed by where the face is in the block like vanilla's
    default UV, so neighbouring faces and blocks continue the same tile."""
    (x0, y0, z0), (x1, y1, z1) = f, t
    return {
        'north': (16 - x1, 16 - y1, 16 - x0, 16 - y0), 'south': (x0, 16 - y1, x1, 16 - y0),
        'west': (z0, 16 - y1, z1, 16 - y0), 'east': (16 - z1, 16 - y1, 16 - z0, 16 - y0),
        'up': (x0, z0, x1, z1), 'down': (x0, 16 - z1, x1, 16 - z0),
    }[direction]


def face(direction, f, t, cell, cull=None):
    entry = {'uv': uv(cell, *face_texels(direction, f, t)), 'texture': '#atlas'}
    if cull:
        entry['cullface'] = cull
    return entry


def section():
    """The trunk's cross-section as a 16x16 grid seen from above, grid[x][z] True where wood is, and for each cut
    cell every (side, column, depth from that face) that cut it."""
    grid = [[True] * 16 for _ in range(16)]
    cut = {}
    for side in range(4):  # north, east, south, west: the profile turned a quarter clockwise each time
        for column, depth in enumerate(PROFILE):
            for d in range(depth):
                x, z = turn(column, d, side)
                grid[x][z] = False
                cut.setdefault((x, z), []).append((side, column, d))
    return grid, cut


def turn(x, z, quarters):
    """A cell of the north face's frame turned clockwise, seen from above, onto another side."""
    for _ in range(quarters % 4):
        x, z = 15 - z, x
    return x, z


SIDES = ('north', 'east', 'south', 'west')
STEP = {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)}


def wall_kind(x, z, direction, cut):
    """What the wall of wood cell (x, z) facing `direction` shows, and on which side. Facing out of a side it is that
    column's surface: rib, plate or cleft floor. Across a side (a step between columns) it is the cleft's black wall
    beside the cleft, the rib's flank beside the rib, and otherwise the plate it belongs to."""
    dx, dz = STEP[direction]
    hole = (x + dx, z + dz)
    if hole not in cut:  # outside the block: only a rib reaches the block's face
        return SIDES.index(direction), 'rib'
    for side, column, _ in cut[hole]:
        if SIDES[side] == direction:
            depth = PROFILE[column]
            return side, 'rib' if depth == RIB else 'cleft' if depth == CLEFT_DEPTH else 'plate'
    side, column, _ = cut[hole][0]
    own_column, _ = turn(x, z, -side)
    if PROFILE[column] == CLEFT_DEPTH:
        return side, 'cleft'
    return side, 'rib' if PROFILE[own_column] == RIB else 'plate'


def trunk_walls(dress):
    """Every visible vertical wall of the section, merged into runs of one material, as zero-thickness elements.
    `dress` names the bark cell of each side's plates and the cleft cell of each side's cleft."""
    grid, cut = section()
    runs = {}
    for x in range(16):
        for z in range(16):
            if not grid[x][z]:
                continue
            for direction, (dx, dz) in STEP.items():
                nx, nz = x + dx, z + dz
                if 0 <= nx < 16 and 0 <= nz < 16 and grid[nx][nz]:
                    continue
                side, kind = wall_kind(x, z, direction, cut)
                cell = {
                    'rib': RIB_CELLS[dress['rib'][SIDES[side]]],
                    'plate': BARK_CELLS[dress['bark'][SIDES[side]]],
                    'cleft': CLEFT_CELLS[dress['cleft'][SIDES[side]]],
                }[kind]
                plane = {'north': z, 'south': z + 1, 'west': x, 'east': x + 1}[direction]
                along = x if direction in ('north', 'south') else z
                runs.setdefault((direction, plane, cell), []).append(along)
    elements = []
    for (direction, plane, cell), cells in sorted(runs.items()):
        cells.sort()
        start = cells[0]
        for i, c in enumerate(cells):
            if i + 1 < len(cells) and cells[i + 1] == c + 1:
                continue
            a, b = start, c + 1
            if direction in ('north', 'south'):
                f, t = [a, 0, plane], [b, 16, plane]
            else:
                f, t = [plane, 0, a], [plane, 16, b]
            cull = direction if plane in (0, 16) else None
            elements.append({'from': f, 'to': t, 'faces': {direction: face(direction, f, t, cell, cull)}})
            if i + 1 < len(cells):
                start = cells[i + 1]
    return elements


def rectangles():
    """The section as a few rectangles (x0, z0, x1, z1), greedily, for the end caps."""
    grid, _ = section()
    free = [[grid[x][z] for z in range(16)] for x in range(16)]
    found = []
    for z in range(16):
        for x in range(16):
            if not free[x][z]:
                continue
            x1 = x
            while x1 + 1 < 16 and free[x1 + 1][z]:
                x1 += 1
            z1 = z
            while z1 + 1 < 16 and all(free[i][z1 + 1] for i in range(x, x1 + 1)):
                z1 += 1
            for i in range(x, x1 + 1):
                for j in range(z, z1 + 1):
                    free[i][j] = False
            found.append((x, z, x1 + 1, z1 + 1))
    return found


def trunk_caps():
    elements = []
    for x0, z0, x1, z1 in rectangles():
        f, t = [x0, 0, z0], [x1, 16, z1]
        elements.append({'from': f, 'to': t, 'faces': {
            'up': face('up', f, t, END_CELL, 'up'),
            'down': face('down', f, t, END_CELL, 'down'),
        }})
    return elements


def box(f, t, cell, skip=(), ends=None):
    """A solid box with every face but `skip`; `ends` maps faces to another cell (a broken end)."""
    faces = {}
    for direction in ('north', 'south', 'west', 'east', 'up', 'down'):
        if direction in skip:
            continue
        faces[direction] = face(direction, f, t, (ends or {}).get(direction, cell))
    return {'from': list(f), 'to': list(t), 'faces': faces}


def stub_face(direction, f, t):
    """The broken end of a stub, from the stub-end cell's top-left corner whatever its size."""
    (x0, y0, z0), (x1, y1, z1) = f, t
    w = {'north': x1 - x0, 'south': x1 - x0, 'west': z1 - z0, 'east': z1 - z0}[direction]
    return {'uv': uv(STUB_END_CELL, 0, 0, w, y1 - y0), 'texture': '#atlas'}


# The cleft of the north face is x 8-11, from z 0 to its floor at z 4. Projections stay inside it: inside the block.
# Maroon shows in few places: the stub's cleft, one wound cleft and one tissue split a model.
LOG_VARIANTS = {
    'infected_log': {
        'bark': {'north': 'plain', 'east': 'tissue', 'south': 'plain', 'west': 'scar'},
        'rib': {'north': 'high', 'east': 'low', 'south': 'low', 'west': 'high'},
        'cleft': {'north': 'black', 'east': 'black', 'south': 'black', 'west': 'black'},
        'parts': (),
        'weight': 3,
    },
    'infected_log_twig': {  # a hooked dry twig out of the north cleft
        'bark': {'north': 'scar', 'east': 'plain', 'south': 'plain', 'west': 'plain'},
        'rib': {'north': 'low', 'east': 'high', 'south': 'high', 'west': 'low'},
        'cleft': {'north': 'black', 'east': 'black', 'south': 'black', 'west': 'black'},
        'parts': (('twig', (9, 9, 0), (10, 10, 4)), ('twig', (9, 10, 0), (10, 13, 1))),
        'weight': 1,
    },
    'infected_log_stub': {  # a snapped branch in the north cleft, its broken end out
        'bark': {'north': 'plain', 'east': 'scar', 'south': 'plain', 'west': 'plain'},
        'rib': {'north': 'high', 'east': 'high', 'south': 'low', 'west': 'low'},
        'cleft': {'north': 'maroon', 'east': 'black', 'south': 'black', 'west': 'black'},
        'parts': (('stub', (8, 4, 1), (10, 6, 4)),),
        'weight': 1,
    },
    'infected_log_wound': {  # a maroon wound and a twig hooking down out of the north cleft
        'bark': {'north': 'plain', 'east': 'plain', 'south': 'scar', 'west': 'tissue'},
        'rib': {'north': 'low', 'east': 'low', 'south': 'high', 'west': 'high'},
        'cleft': {'north': 'black', 'east': 'maroon', 'south': 'black', 'west': 'black'},
        'parts': (('twig', (10, 5, 0), (11, 6, 4)), ('twig', (10, 3, 0), (11, 5, 1))),
        'weight': 1,
    },
}


def log_model(spec):
    elements = trunk_walls(spec) + trunk_caps()
    for kind, f, t in spec['parts']:
        if kind == 'twig':
            elements.append(box(f, t, TWIG_CELL, skip=('south',) if t[2] == 4 else ()))
        else:
            element = box(f, t, TWIG_CELL, skip=('south',))
            element['faces']['north'] = stub_face('north', f, t)
            elements.append(element)
    return {
        'parent': 'minecraft:block/block',
        'textures': {'atlas': NS + 'infected_timber', 'particle': NS + 'infected_timber'},
        'elements': elements,
    }


# The crown: per model, twigs as 1x1 boxes (from, to), stepping like a dry limb and hooked at their ends, and torn
# membrane hanging from inside a twig as a plane (scrap name, axis of the plane's normal, position on that axis,
# left, top); a scrap's size is its texels. Most of the block stays open.
LEAF_VARIANTS = {
    'blighted_leaves': {  # a limb stepping up across the block, a spur hooked down, two thin twigs
        'twigs': (((0, 6, 6), (5, 8, 7)), ((4, 8, 6), (9, 9, 7)), ((8, 9, 6), (13, 10, 7)), ((12, 10, 6), (16, 11, 7)),
                  ((8, 9, 7), (9, 10, 12)), ((8, 6, 11), (9, 9, 12)), ((8, 6, 10), (9, 7, 11)),
                  ((2, 13, 0), (3, 14, 9)), ((2, 14, 8), (3, 16, 9)),
                  ((13, 3, 9), (14, 4, 16)), ((13, 4, 9), (14, 6, 10))),
        'scraps': (('short', 'z', 6.5, 5, 9),),
    },
    'blighted_leaves_hooks': {  # bare twigs: a high twig with a hook up, a stepped twig hooked down, a low fork
        'twigs': (((5, 13, 0), (6, 14, 16)), ((5, 14, 3), (6, 16, 4)),
                  ((6, 12, 12), (10, 13, 13)), ((9, 11, 12), (13, 12, 13)), ((12, 10, 12), (16, 11, 13)),
                  ((12, 7, 12), (13, 10, 13)),
                  ((0, 5, 3), (9, 6, 4)), ((8, 6, 3), (9, 8, 4)), ((2, 2, 3), (3, 5, 4)),
                  ((13, 0, 7), (14, 6, 8)), ((12, 5, 7), (13, 6, 8))),
        'scraps': (('tatter', 'x', 5.5, 6, 14),),
    },
    'blighted_leaves_tatters': {  # one long twig with hooks both ways, a high twig, two scraps
        'twigs': (((0, 8, 11), (16, 9, 12)), ((2, 9, 11), (3, 12, 12)), ((12, 5, 11), (13, 8, 12)),
                  ((7, 14, 0), (8, 15, 11)), ((7, 12, 0), (8, 14, 1)),
                  ((8, 14, 4), (12, 15, 5)), ((11, 13, 4), (15, 14, 5)), ((14, 11, 4), (15, 13, 5))),
        'scraps': (('short', 'z', 11.5, 3, 9), ('wide', 'x', 7.5, 3, 15)),
    },
}


def scrap(name, axis, at, left, top):
    """A torn membrane hanging below `top` as a two-sided plane, its texels one per pixel."""
    cell, (cl, ct, width, height), _ = SCRAPS[name]
    texels = (cl, ct, cl + width, ct + height)
    if axis == 'z':
        f, t = [left, top - height, at], [left + width, top, at]
        sides = ('north', 'south')
    else:
        f, t = [at, top - height, left], [at, top, left + width]
        sides = ('west', 'east')
    faces = {}
    for side in sides:
        u0, v0, u1, v1 = texels
        if side in ('north', 'east'):  # seen from the other side the scrap is mirrored, as a sheet is
            u0, u1 = u1, u0
        faces[side] = {'uv': uv(cell, u0, v0, u1, v1), 'texture': '#atlas'}
    return {'from': f, 'to': t, 'shade': True, 'faces': faces}


def leaf_model(spec):
    elements = [box(f, t, TWIG_CELL) for f, t in spec['twigs']]
    elements += [scrap(*s) for s in spec['scraps']]
    return {
        'parent': 'minecraft:block/block',
        'ambientocclusion': False,
        'render_type': 'minecraft:cutout_mipped',
        'textures': {'atlas': NS + 'infected_timber', 'particle': NS + 'infected_timber'},
        'elements': elements,
    }


# ----------------------------------------------------------------------------------------------------------------------
# Files


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')


def write_textures():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    atlas().save(TEXTURES / 'infected_timber.png')
    planks().save(TEXTURES / 'infected_planks.png')


def write_models():
    for name, spec in LOG_VARIANTS.items():
        dump(MODELS / f'{name}.json', log_model(spec))
    for name, spec in LEAF_VARIANTS.items():
        dump(MODELS / f'{name}.json', leaf_model(spec))
    dump(MODELS / 'infected_planks.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': NS + 'infected_planks'}})
    # The cube items sit with their visual weight centred in the hotbar slot (the gate measures it); the darker
    # planks need a little more lift. One-pixel twigs vanish at slot size, so the leaves item is a flat sprite.
    def gui(lift):
        return {'gui': {'rotation': [30, 225, 0], 'translation': [0.8, lift, 0], 'scale': [0.55, 0.55, 0.55]}}

    dump(ITEM_MODELS / 'infected_log.json', {'parent': NS + 'infected_log', 'display': gui(-1.2)})
    dump(ITEM_MODELS / 'infected_planks.json', {'parent': NS + 'infected_planks', 'display': gui(-2.2)})
    dump(ITEM_MODELS / 'blighted_leaves.json', {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': 'infestusfrontier:item/blighted_leaves'}})


def turns(models, rotations):
    """Every model under every rotation, weighted by the model, as one list the blockstate picks from by position."""
    return [dict({'model': NS + name, 'weight': weight}, **rotation)
            for name, weight in models for rotation in rotations]


def write_blockstates():
    logs = [(name, spec['weight']) for name, spec in LOG_VARIANTS.items()]
    leaves = [(name, 1) for name in LEAF_VARIANTS]
    upright = [{}, {'y': 90}, {'y': 180}, {'y': 270}]
    dump(STATES / 'infected_log.json', {'variants': {
        # Lying logs turn half a turn about their own axis: x 270 then y 180 is x 90 turned about z.
        'axis=x': turns(logs, [{'x': 90, 'y': 90}, {'x': 270, 'y': 270}]),
        'axis=y': turns(logs, upright),
        'axis=z': turns(logs, [{'x': 90}, {'x': 270, 'y': 180}]),
    }})
    dump(STATES / 'blighted_leaves.json', {'variants': {'': turns(leaves, upright)}})
    dump(STATES / 'infected_planks.json', {'variants': {'': {'model': NS + 'infected_planks'}}})


def write_icon():
    """The guide entry's icon, 16x16: a dead trunk with an ivory rib and a black cleft under a ragged twig crown
    with one membrane scrap, drawn texel by texel in the atlas palette."""
    BOOK.mkdir(parents=True, exist_ok=True)
    rows = [
        '....T.....T.....',
        '.T..T..T..T..T..',
        '..T.T.T...TT.T..',
        '...TTT..T.T.TT..',
        'TT..T...TTT.T...',
        '..T.TT.TT.MMT..T',
        '...TTTT..TMM.TT.',
        '.....TTT.TMm.T..',
        '......BRBkB..m..',
        '......BRBkB.....',
        '......bRBkM.....',
        '......BRBkB.....',
        '......BRbkB.....',
        '......BRBkB.....',
        '.....bBRBkBb....',
        '....bBBRBkBBb...',
    ]
    colours = {
        'T': TWIG, 'B': BARK_MID, 'b': BARK_DARK, 'R': IVORY, 'k': CLEFT_BLACK, 'M': MEMBRANE, 'm': MEMBRANE_DARK,
    }
    icon = Image.new('RGBA', (16, 16), CLEAR)
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key in colours:
                icon.putpixel((x, y), rgba(colours[key]))
    icon.putpixel((10, 10), rgba(MAROON))
    icon.save(BOOK / 'infected_timber.png')


def leaves_sprite():
    """The Blighted Leaves item, 16x16: a stepped dry branch hooked at its tips, torn membrane hanging from it."""
    rows = [
        '..........t.....',
        '...t......T...t.',
        '...T.....TT..T..',
        '...TT...TT..TT..',
        '....T..TT..TT...',
        '....TT.T..TT....',
        '.....TTT.TT.....',
        '......TTTTMMMm..',
        '.....TTT..MrMMm.',
        '....TT.T..mMM.m.',
        '...TT..T...M.M..',
        '..TT...TMM..M...',
        '..T...MMrM......',
        '.TT...mMM.m.....',
        '.T.....M..m.....',
        'T...............',
    ]
    colours = {'T': TWIG, 't': TWIG_DARK, 'M': MEMBRANE_LIGHT, 'm': MEMBRANE_DARK, 'r': MAROON}
    sprite = Image.new('RGBA', (16, 16), CLEAR)
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            if key in colours:
                sprite.putpixel((x, y), rgba(colours[key]))
    return sprite


def write_item_textures():
    ITEM_TEXTURES.mkdir(parents=True, exist_ok=True)
    leaves_sprite().save(ITEM_TEXTURES / 'blighted_leaves.png')


def main():
    write_textures()
    write_item_textures()
    write_models()
    write_blockstates()
    write_icon()


if __name__ == '__main__':
    main()
