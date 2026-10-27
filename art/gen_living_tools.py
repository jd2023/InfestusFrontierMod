"""Icons for the bone tools and the fang (ITEM_CATALOG I015 to I018): Dormant and Living Pick, Hatchet, Spade, Hoe and
Fang.

    python3 art/gen_living_tools.py

Each icon is one tool on a diagonal handle, drawn as smooth shapes at four times the size and brought down to 32
pixels, in the light of every other icon: top-left lit, a dark rim round the silhouette. A dormant tool is a bone
head on a bare stick. A living tool's stick is wrapped in red sinew with a pale vein, a green bead where the sinew
meets the head, and the bone itself has taken a faint pink flush along its edge.
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path('src/main/resources/assets/infestusfrontier/textures/item')
SIZE, SUPER = 32, 4
BONE = ((240, 228, 204), (206, 186, 150), (150, 126, 90))
STICK = ((138, 98, 56), (100, 68, 38), (62, 40, 22))
SINEW = ((222, 134, 124), (170, 64, 74), (108, 32, 46))
VEIN = (244, 196, 188)
BEAD = (128, 196, 96)
FLUSH = (214, 120, 128)
RIM = (46, 32, 22)

# the handle runs from the bottom-left corner up to the head
FOOT, HEAD = (2.6, 13.4), (9.8, 6.2)
U = ((HEAD[0] - FOOT[0]) / math.hypot(HEAD[0] - FOOT[0], HEAD[1] - FOOT[1]),
     (HEAD[1] - FOOT[1]) / math.hypot(HEAD[0] - FOOT[0], HEAD[1] - FOOT[1]))
V = (-U[1], U[0])  # left of the handle when walking up it

# heads in handle coordinates: u along the handle past its top, v across it
HEADS = {
    'pick': [(-0.6, -4.6), (0.9, -3.0), (1.5, 0.0), (0.9, 3.0), (-0.6, 4.6), (-1.7, 3.4), (-0.5, 0.0), (-1.7, -3.4)],
    'hatchet': [(-1.6, -0.4), (0.6, -0.4), (2.0, -3.6), (0.4, -4.8), (-2.3, -4.0), (-1.9, -1.4)],
    'spade': [(-1.3, -2.3), (1.6, -2.3), (3.3, 0.0), (1.6, 2.3), (-1.3, 2.3)],
    'hoe': [(-0.6, -0.7), (1.1, -0.7), (1.1, 0.7), (1.3, 0.7), (1.3, 4.6), (0.1, 4.6), (0.1, 0.7), (-0.6, 0.7)],
    # a long curved tooth along the handle, hooked toward its left, with a bone guard across the top of the grip
    'fang': [(-0.8, -1.2), (0.4, -1.2), (2.4, -1.4), (4.2, -1.0), (5.4, 0.2), (4.6, 0.6), (3.0, 0.2), (1.2, 0.3), (-0.8, 0.5)],
}
GUARDS = {'fang': [(-1.0, -1.8), (-0.2, -1.8), (-0.2, 1.4), (-1.0, 1.4)]}  # the guard across the grip
POLLS = {'hatchet': [(-1.5, -0.4), (0.5, -0.4), (0.5, 1.5), (-1.5, 1.5)]}  # the hatchet's flat back


GROW = 1.3  # heads drawn this much larger than their handle coordinates say, so they read at hotbar size


def to_xy(u, v):
    u, v = u * GROW, v * GROW
    return (HEAD[0] + U[0] * u + V[0] * v, HEAD[1] + U[1] * u + V[1] * v)


def scaled(points):
    return [(x * SUPER * SIZE / 16, y * SUPER * SIZE / 16) for x, y in points]


def stroke(a, b, width):
    """A rectangle round the segment a-b, width across."""
    dx, dy = b[0] - a[0], b[1] - a[1]
    n = math.hypot(dx, dy)
    nx, ny = -dy / n * width / 2, dx / n * width / 2
    return [(a[0] + nx, a[1] + ny), (b[0] + nx, b[1] + ny), (b[0] - nx, b[1] - ny), (a[0] - nx, a[1] - ny)]


def shade(mask, ramp, flush=None):
    """Fills mask (an L image at SUPER scale) with the ramp lit from the top-left: light, mid, then dark toward the
    bottom-right, a one-texel highlight on the lit edge and a darker band on the shaded edge."""
    big = SIZE * SUPER
    out = Image.new('RGBA', (big, big), (0, 0, 0, 0))
    px = mask.load()
    pix = out.load()
    edge = SUPER  # one icon texel
    for y in range(big):
        for x in range(big):
            if px[x, y] == 0:
                continue
            t = (x + y) / (2 * big)
            lit = px[max(0, x - edge), max(0, y - edge)] == 0
            dark = px[min(big - 1, x + edge), min(big - 1, y + edge)] == 0
            if lit:
                colour = ramp[0]
            elif dark:
                colour = ramp[2]
            else:
                k = min(1.0, max(0.0, (t - 0.25) / 0.5))
                a, b = ramp[0], ramp[1]
                if k > 0.5:
                    a, b, k = ramp[1], ramp[2], (k - 0.5) * 2
                else:
                    k = k * 2
                colour = tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))
            if flush and dark and (x + y) % (3 * SUPER) < SUPER:
                colour = flush
            pix[x, y] = colour + (255,)
    return out


def icon(tool, living):
    big = SIZE * SUPER
    foot, head = scaled([FOOT])[0], scaled([HEAD])[0]
    handle = Image.new('L', (big, big), 0)
    ImageDraw.Draw(handle).polygon(stroke(foot, head, 1.8 * SUPER * SIZE / 16), fill=255)
    head_mask = Image.new('L', (big, big), 0)
    draw = ImageDraw.Draw(head_mask)
    draw.polygon(scaled([to_xy(u, v) for u, v in HEADS[tool]]), fill=255)
    if tool in POLLS:
        draw.polygon(scaled([to_xy(u, v) for u, v in POLLS[tool]]), fill=255)
    if tool in GUARDS:
        draw.polygon(scaled([to_xy(u, v) for u, v in GUARDS[tool]]), fill=255)
    layers = [shade(handle, SINEW if living else STICK), shade(head_mask, BONE, FLUSH if living else None)]
    art = Image.new('RGBA', (big, big), (0, 0, 0, 0))
    for layer in layers:
        art.alpha_composite(layer)
    if living:
        detail = ImageDraw.Draw(art)
        # the sinew wrap: pale vein threads crossing the handle, and a green bead at the head
        for k in range(1, 7):
            f = k / 7
            cx, cy = foot[0] + (head[0] - foot[0]) * f, foot[1] + (head[1] - foot[1]) * f
            a = (cx - V[0] * 0.75 * SUPER * 2, cy - V[1] * 0.75 * SUPER * 2)
            b = (cx + V[0] * 0.75 * SUPER * 2, cy + V[1] * 0.75 * SUPER * 2)
            detail.line([a, b], fill=VEIN + (255,), width=SUPER)
        bead = scaled([to_xy(-1.2, 0)])[0]
        r = 0.7 * SUPER * SIZE / 16
        detail.ellipse([bead[0] - r, bead[1] - r, bead[0] + r, bead[1] + r], fill=BEAD + (255,))
    small = art.resize((SIZE, SIZE), Image.LANCZOS)
    return rim(small)


def rim(img):
    """A dark one-pixel rim round the silhouette, under the art, as vanilla icons have."""
    px = img.load()
    out = Image.new('RGBA', img.size, (0, 0, 0, 0))
    o = out.load()
    for y in range(img.height):
        for x in range(img.width):
            if px[x, y][3] > 96:
                continue
            near = any(0 <= x + dx < img.width and 0 <= y + dy < img.height and px[x + dx, y + dy][3] > 96
                       for dx in (-1, 0, 1) for dy in (-1, 0, 1))
            if near:
                o[x, y] = RIM + (255,)
    for y in range(img.height):
        for x in range(img.width):
            if px[x, y][3] > 96:
                o[x, y] = px[x, y][:3] + (255,)
    return out


def main():
    for tool in HEADS:
        for state, living in (('dormant', False), ('living', True)):
            icon(tool, living).save(OUT / f'{state}_{tool}.png')
    print('living tools: 10 icons')


if __name__ == '__main__':
    main()
