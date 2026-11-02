#!/usr/bin/env python3
"""Fits an image-model icon source to its square before export, so the icon sits centred in its slot.

    python3 art/fit_icon_source.py SOURCE.png FITTED.png [--fill 0.84]

Trims the source to its opaque content, scales it so the longer side fills FILL of a square canvas, and places it so
the content's visual centroid (the alpha-weighted mean) is the canvas centre, as far as the canvas allows. The result
goes to `scripts/texture.py`. The capture gate measures an item icon's centroid within 1.5 px of its slot centre.
"""
import argparse

import numpy as np
from PIL import Image


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('source')
    parser.add_argument('fitted')
    parser.add_argument('--fill', type=float, default=0.84)
    args = parser.parse_args()
    image = Image.open(args.source).convert('RGBA')
    image = image.crop(image.getchannel('A').point(lambda a: 255 if a > 8 else 0).getbbox())
    side = 1254
    scale = args.fill * side / max(image.size)
    image = image.resize((round(image.width * scale), round(image.height * scale)), Image.LANCZOS)
    alpha = np.asarray(image.getchannel('A'), dtype=float)
    ys, xs = np.mgrid[0:image.height, 0:image.width]
    cx, cy = (alpha * xs).sum() / alpha.sum(), (alpha * ys).sum() / alpha.sum()
    x = min(max(round(side / 2 - cx), 0), side - image.width)
    y = min(max(round(side / 2 - cy), 0), side - image.height)
    canvas = Image.new('RGBA', (side, side), (0, 0, 0, 0))
    canvas.alpha_composite(image, (x, y))
    canvas.save(args.fitted)


if __name__ == '__main__':
    main()
