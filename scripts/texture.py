#!/usr/bin/env python3
"""Export a shipped texture from an image-model source.

    python3 scripts/texture.py SOURCE.png SHIPPED.png [--size 64] [--colors 96]

Downsamples SOURCE to size x size with a Lanczos filter, caps the palette (0 keeps every color), writes SHIPPED,
and writes build/art/<name>-tiled.png: the result repeated 3x3 at 4x, to check for seams and repeated bright spots.
Alpha is kept when the source has it. Only SHIPPED is committed; the source stays outside the repository.
"""
import argparse
import pathlib
import sys

from PIL import Image


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("source", type=pathlib.Path)
    parser.add_argument("shipped", type=pathlib.Path)
    parser.add_argument("--size", type=int, default=64, help="output width and height, default 64")
    parser.add_argument("--colors", type=int, default=96, help="palette cap, 0 keeps every color, default 96")
    args = parser.parse_args()

    image = Image.open(args.source)
    alpha = image.mode in ("RGBA", "LA") or "transparency" in image.info
    image = image.convert("RGBA" if alpha else "RGB")
    if image.width != image.height:
        print(f"source must be square, got {image.width}x{image.height}", file=sys.stderr)
        return 1
    image = image.resize((args.size, args.size), Image.LANCZOS)
    if args.colors > 0:
        method = Image.Quantize.FASTOCTREE if alpha else Image.Quantize.MEDIANCUT
        image = image.quantize(args.colors, method=method, dither=Image.Dither.NONE).convert("RGBA" if alpha else "RGB")
    args.shipped.parent.mkdir(parents=True, exist_ok=True)
    image.save(args.shipped)

    root = pathlib.Path(__file__).resolve().parent.parent
    preview_dir = root / "build" / "art"
    preview_dir.mkdir(parents=True, exist_ok=True)
    scale = 4
    big = image.resize((args.size * scale, args.size * scale), Image.NEAREST)
    tiled = Image.new(image.mode, (big.width * 3, big.height * 3), (40, 40, 40, 255) if alpha else (40, 40, 40))
    for ty in range(3):
        for tx in range(3):
            tiled.paste(big, (tx * big.width, ty * big.height), big if alpha else None)
    preview = preview_dir / f"{args.shipped.stem}-tiled.png"
    tiled.save(preview)
    colors = len(image.getcolors(1 << 24) or [])
    print(f"wrote {args.shipped} ({args.size}x{args.size}, {colors} colors) and {preview}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
