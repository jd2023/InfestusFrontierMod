"""Infected timber: Infected Log, Infected Planks and Blighted Leaves.

    python3 art/gen_infected_timber.py

Models and guide icon for the finished image-model textures (CREDITS.txt).
The log has one grey bark tile and one grey end tile; the block colour tints both by the species the tree was (oak brown, birch bone, spruce dark green-grey, jungle
olive, acacia tan, dark oak umber), so six species share two textures and one model per axis. The planks are one
untinted tile. The leaves are one sparse grey cutout tile over vanilla's full cube, tinted by species the same way,
so the holes show the branches behind them. Dark root threads and a few dull knots are the only signs of infection.
No block entity, no saved appearance.
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
NS = 'infestusfrontier:block/'
SIZE = 64
SPECIES = ('oak', 'birch', 'spruce', 'jungle', 'acacia', 'dark_oak')
ROOT_THREAD = 52
KNOT = 96


def grey(value, alpha=255):
    return (value, value, value, alpha)


def bark():
    """Vertical bark ridges with dark seams, two dull knots and a few root threads."""
    rng = random.Random(7)
    image = Image.new('RGBA', (SIZE, SIZE), grey(195))
    draw = ImageDraw.Draw(image)
    x = 0
    while x < SIZE:
        width = rng.randint(4, 8)
        shade = rng.randint(180, 235)
        draw.rectangle((x, 0, x + width - 2, SIZE - 1), fill=grey(shade))
        draw.line([(x + width - 1, 0), (x + width - 1, SIZE - 1)], fill=grey(88), width=1)
        for _ in range(3):  # a broken seam inside the ridge
            y = rng.randint(0, SIZE - 8)
            draw.line([(x + 1, y), (x + 1, y + rng.randint(3, 7))], fill=grey(shade - 40), width=1)
        x += width
    for cx, cy in ((14, 20), (45, 47)):
        draw.ellipse((cx - 4, cy - 3, cx + 4, cy + 3), fill=grey(KNOT), outline=grey(60))
    for sx in (6, 30, 54):  # root threads crawling over the bark
        points = [(sx, 0)]
        for y in range(8, SIZE, 8):
            points.append(((points[-1][0] + rng.randint(-3, 3)) % SIZE, y))
        draw.line(points, fill=grey(ROOT_THREAD), width=1)
    return image


def end():
    """Growth rings round a pale heart, dark where the infection runs through."""
    image = Image.new('RGBA', (SIZE, SIZE), grey(190))
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, SIZE - 1, SIZE - 1), fill=grey(150))  # the bark rim
    draw.rectangle((4, 4, SIZE - 5, SIZE - 5), fill=grey(205))
    for i, radius in enumerate(range(26, 2, -5)):
        draw.ellipse((32 - radius, 32 - radius, 32 + radius, 32 + radius), outline=grey(150 + (i % 2) * 25))
    draw.ellipse((28, 28, 36, 36), fill=grey(KNOT))
    for dx, dy in ((20, -8), (-18, 14), (6, 22)):  # root threads through the rings
        draw.line([(32, 32), (32 + dx // 2, 32 + dy // 2), (32 + dx, 32 + dy)], fill=grey(ROOT_THREAD), width=1)
    return image


def planks():
    """Four boards with dark seams, grain and a warm grey cast; untinted."""
    rng = random.Random(11)
    image = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 255))
    draw = ImageDraw.Draw(image)
    for row in range(4):
        y0 = row * 16
        base = rng.randint(118, 138)
        draw.rectangle((0, y0, SIZE - 1, y0 + 14), fill=(base + 20, base + 4, base - 24, 255))
        for _ in range(7):  # grain
            gx = rng.randint(0, SIZE - 12)
            gy = y0 + rng.randint(1, 13)
            draw.line([(gx, gy), (gx + rng.randint(5, 12), gy)], fill=(base - 4, base - 14, base - 40, 255))
        draw.line([(0, y0 + 15), (SIZE - 1, y0 + 15)], fill=(66, 58, 52, 255))
        joint = rng.randint(8, SIZE - 8)
        draw.line([(joint, y0), (joint, y0 + 14)], fill=(66, 58, 52, 255))
    draw.ellipse((40, 22, 48, 28), fill=(92, 84, 76, 255), outline=(62, 54, 48, 255))
    draw.line([(10, 63), (14, 52), (12, 40)], fill=(70, 62, 56, 255), width=1)
    return image


def leaves():
    """Sparse leaf clumps on twigs with many see-through holes; about half the pixels are opaque."""
    rng = random.Random(23)
    image = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    for _ in range(9):  # twigs crossing the tile, wrapping so the cube's faces meet
        x, y = rng.randint(0, SIZE - 1), rng.randint(0, SIZE - 1)
        dx, dy = rng.choice((-1, 1)) * rng.randint(2, 4), rng.randint(-3, 3)
        points = [(x, y)]
        for _ in range(4):
            x, y = (x + dx) % SIZE, (y + dy) % SIZE
            if abs(x - points[-1][0]) > 8 or abs(y - points[-1][1]) > 8:
                break
            points.append((x, y))
        if len(points) > 1:
            draw.line(points, fill=grey(120), width=2)
    for _ in range(46):  # a dry leaf clump, a small rimmed blob
        cx, cy = rng.randint(0, SIZE - 1), rng.randint(0, SIZE - 1)
        w, h = rng.randint(3, 6), rng.randint(3, 5)
        shade = rng.randint(195, 250)
        box = (cx - w, cy - h, cx + w, cy + h)
        draw.ellipse(box, fill=grey(shade), outline=grey(82))
        draw.line([(cx - w + 1, cy), (cx + w - 1, cy)], fill=grey(shade - 45), width=1)
    for cx, cy in ((12, 50), (40, 12), (54, 40)):  # dull knots
        draw.ellipse((cx - 2, cy - 2, cx + 2, cy + 2), fill=grey(KNOT), outline=grey(60))
    # keep it sparse: clear a scatter of whole holes so the branches behind show
    for _ in range(14):
        hx, hy = rng.randint(0, SIZE - 6), rng.randint(0, SIZE - 6)
        draw.rectangle((hx, hy, hx + rng.randint(3, 5), hy + rng.randint(3, 5)), fill=(0, 0, 0, 0))
    return image


def write_textures():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    bark().save(TEXTURES / 'infected_log.png')
    end().save(TEXTURES / 'infected_log_top.png')
    planks().save(TEXTURES / 'infected_planks.png')
    leaves().save(TEXTURES / 'blighted_leaves.png')


def write_models():
    MODELS.mkdir(parents=True, exist_ok=True)
    ITEM_MODELS.mkdir(parents=True, exist_ok=True)
    textures = {
        'particle': NS + 'infected_log',
        'end': NS + 'infected_log_top',
        'side': NS + 'infected_log',
    }

    def face(texture, cull, **extra):
        return {'uv': [0, 0, 16, 16], 'texture': texture, 'tintindex': 0, 'cullface': cull, **extra}

    vertical = {
        'down': face('#end', 'down'),
        'up': face('#end', 'up'),
        'north': face('#side', 'north'),
        'south': face('#side', 'south'),
        'west': face('#side', 'west'),
        'east': face('#side', 'east'),
    }
    horizontal = dict(vertical)
    horizontal['up'] = face('#end', 'up', rotation=180)
    for name, faces in (('infected_log', vertical), ('infected_log_horizontal', horizontal)):
        model = {
            'parent': 'minecraft:block/block',
            'textures': textures,
            'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces}],
        }
        (MODELS / f'{name}.json').write_text(json.dumps(model, indent=2) + '\n')
    planks_model = {'parent': 'minecraft:block/cube_all', 'textures': {'all': NS + 'infected_planks'}}
    (MODELS / 'infected_planks.json').write_text(json.dumps(planks_model, indent=2) + '\n')
    leaves_model = {
        'parent': 'minecraft:block/leaves',
        'render_type': 'minecraft:cutout_mipped',
        'textures': {'all': NS + 'blighted_leaves'},
    }
    (MODELS / 'blighted_leaves.json').write_text(json.dumps(leaves_model, indent=2) + '\n')
    for name in ('infected_log', 'infected_planks'):
        # Keep the painted cube's visual weight centered inside the hotbar slot.
        model = {'parent': NS + name, 'display': {'gui': {
            'rotation': [30, 225, 0], 'translation': [0.8, -1.2, 0], 'scale': [0.55, 0.55, 0.55],
        }}}
        (ITEM_MODELS / f'{name}.json').write_text(json.dumps(model, indent=2) + '\n')


def write_blockstates():
    STATES.mkdir(parents=True, exist_ok=True)
    log = {
        'variants': {
            'axis=x': {'model': NS + 'infected_log_horizontal', 'x': 90, 'y': 90},
            'axis=y': {'model': NS + 'infected_log'},
            'axis=z': {'model': NS + 'infected_log_horizontal', 'x': 90},
        }
    }
    (STATES / 'infected_log.json').write_text(json.dumps(log, indent=2) + '\n')
    (STATES / 'infected_planks.json').write_text(
        json.dumps({'variants': {'': {'model': NS + 'infected_planks'}}}, indent=2) + '\n'
    )
    (STATES / 'blighted_leaves.json').write_text(
        json.dumps({'variants': {'': {'model': NS + 'blighted_leaves'}}}, indent=2) + '\n'
    )


def write_icon():
    """The guide entry's icon: the log's bark face in bone grey, 16x16."""
    BOOK.mkdir(parents=True, exist_ok=True)
    tint = (0xA8, 0x9C, 0x84)
    source = Image.open(TEXTURES / 'infected_log.png').convert('RGBA')
    tinted = Image.new('RGBA', source.size)
    for x in range(SIZE):
        for y in range(SIZE):
            r, g, b, a = source.getpixel((x, y))
            tinted.putpixel((x, y), (r * tint[0] // 255, g * tint[1] // 255, b * tint[2] // 255, a))
    tinted.resize((16, 16), Image.Resampling.BOX).save(BOOK / 'infected_timber.png')


def main():
    # Finished image-model textures are maintained independently.
    write_models()
    write_blockstates()
    write_icon()


if __name__ == '__main__':
    main()
