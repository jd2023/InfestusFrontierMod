#!/usr/bin/env python3
"""Living Substrate stage tiles from one master.

    python3 art/gen_substrate_stages.py

Reads art/living_substrate_master.png, the 256x256 mature tissue (4x4 blocks), and writes the sixteen 64x64 tiles
of every stage to textures/block/living_substrate_<stage>_<i>.png. Young and basic are derived from the master, so all
three stages share one vein network and one ground: a vein that enters a mature cell continues through its young and
basic neighbours. Evidence goes to build/art/living_substrate-stages.png (the three macros) and
build/art/living_substrate_<stage>-tiled.png (each macro repeated 2x2).

Stage recipe (docs/design/LIVING_SUBSTRATE_MUTATIONS.md, Appearance):
  basic   ground grain with the trunk veins only, dark red
  young   the whole network at medium weight, faint tissue pads, the pores
  mature  the master, veins toned down a little and the pads pulled toward red
"""
import pathlib

import numpy as np
from PIL import Image, ImageFilter

ROOT = pathlib.Path(__file__).resolve().parents[1]
MASTER = ROOT / "art" / "living_substrate_master.png"
TEXTURES = ROOT / "src" / "main" / "resources" / "assets" / "infestusfrontier" / "textures" / "block"
EVIDENCE = ROOT / "build" / "art"
SIZE, TILES = 256, 4

GROUND = np.array([0.17, 0.085, 0.10])
VEIN_DARK = np.array([0.42, 0.12, 0.16])
VEIN_MID = np.array([0.62, 0.22, 0.26])
VEIN_LIGHT = np.array([0.74, 0.48, 0.42])


def blur(a, radius):
    """Gaussian blur that wraps, so the macro keeps tiling without a seam."""
    big = np.tile(a, (3, 3) if a.ndim == 2 else (3, 3, 1))
    img = Image.fromarray((np.clip(big, 0, 1) * 255).astype("uint8"))
    out = np.asarray(img.filter(ImageFilter.GaussianBlur(radius))).astype(float) / 255
    return out[SIZE : 2 * SIZE, SIZE : 2 * SIZE]


def mix(base, mask, colour):
    return base * (1 - mask[..., None]) + colour * mask[..., None]


def luminance(rgb):
    return rgb @ np.array([0.299, 0.587, 0.114])


def stages(master):
    lum = luminance(master)
    vein = np.clip((lum - 0.42) / 0.25, 0, 1)
    trunk = np.clip((blur(vein, 2.5) - 0.25) / 0.3, 0, 1)
    pad = np.clip((master[..., 0] - master[..., 1] - 0.15) / 0.25, 0, 1) * (1 - vein)
    pore = np.clip((lum - 0.55) / 0.2, 0, 1) * (1 - trunk)

    grain = lum - blur(lum, 4)
    relief = blur(lum, 12) - blur(lum, 12).mean()
    ground = GROUND + grain[..., None] * np.array([1.2, 0.8, 0.9]) + relief[..., None] * 0.5
    ground = np.clip(ground, 0, 1)

    basic = mix(ground, trunk * 0.9, VEIN_DARK)
    basic = mix(basic, np.clip(vein - trunk, 0, 1) * 0.35, VEIN_DARK)

    young = mix(ground, pad * 0.55, master * 0.9)
    young = mix(young, vein * 0.85, VEIN_MID)
    young = mix(young, trunk * 0.6, VEIN_LIGHT)
    young = mix(young, pore * 0.6, VEIN_LIGHT)

    mature = master.copy()
    mature[..., 2] *= 0.9
    mature = mix(mature, vein, master * 0.88 + 0.02) * 0.95
    return {"basic": basic, "young": young, "mature": mature}


def image(a):
    return Image.fromarray((np.clip(a, 0, 1) * 255).astype("uint8"))


def main():
    master = np.asarray(Image.open(MASTER).convert("RGB")).astype(float) / 255
    assert master.shape == (SIZE, SIZE, 3), master.shape
    out = stages(master)
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGB", (3 * SIZE + 32, SIZE), (40, 40, 40))
    tile = SIZE // TILES
    for column, (stage, data) in enumerate(out.items()):
        macro = image(data)
        for i in range(TILES * TILES):
            x, y = (i % TILES) * tile, (i // TILES) * tile
            macro.crop((x, y, x + tile, y + tile)).save(TEXTURES / f"living_substrate_{stage}_{i}.png")
        sheet.paste(macro, (column * (SIZE + 16), 0))
        tiled = Image.new("RGB", (2 * SIZE, 2 * SIZE))
        for dx in (0, SIZE):
            for dy in (0, SIZE):
                tiled.paste(macro, (dx, dy))
        tiled.save(EVIDENCE / f"living_substrate_{stage}-tiled.png")
        print(f"{stage}: mean brightness {luminance(data).mean() * 255:.0f}")
    sheet.save(EVIDENCE / "living_substrate-stages.png")


if __name__ == "__main__":
    main()
