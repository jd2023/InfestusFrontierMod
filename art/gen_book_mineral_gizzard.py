#!/usr/bin/env python3
"""The Mineral Gizzard's guide illustration: its own model, drawn from its geo and texture files, over a pool of living
substrate, with what it makes laid out in front (the three dusts, a Tailings and a Rock Residue, from their image-model icons).

Run from the repository root: python3 art/gen_book_mineral_gizzard.py
Deterministic. Writes src/main/resources/assets/infestusfrontier/textures/book/mineral_gizzard.png, 200x200 RGBA with a
transparent background, the square Modonomicon's image pages draw.

The renderer is orthographic with a painter's sort of every cube face, nearest-texel sampling at 8x and a Lanczos
reduction, so the texels soften into the painted look of the other book plates. File space is used throughout: seen
from the front (north), screen right is the file's +x, as the box UV expects.
"""
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ASSETS = Path('src/main/resources/assets/infestusfrontier')
OUT = ASSETS / 'textures/book/mineral_gizzard.png'
SS = 8  # supersample of the final model drawing
SIZE = 200
MODEL_SIZE = 150  # the model's drawing, in book pixels, before it is placed
YAW, PITCH = 32.0, 30.0
LIGHT = np.array([-0.45, 0.8, -0.4])
LIGHT = LIGHT / np.linalg.norm(LIGHT)


def rot_y(deg):
    a = math.radians(deg)
    return np.array([[math.cos(a), 0, math.sin(a)], [0, 1, 0], [-math.sin(a), 0, math.cos(a)]])


def rot_x(deg):
    a = math.radians(deg)
    return np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])


def rot_z(deg):
    a = math.radians(deg)
    return np.array([[math.cos(a), -math.sin(a), 0], [math.sin(a), math.cos(a), 0], [0, 0, 1]])


def cube_faces(cube, texture_size):
    """(normal, 4 corners, 4 uv points) for each face of a box-UV cube, in file space."""
    ox, oy, oz = cube['origin']
    w, h, d = cube['size']
    u, v = cube['uv']
    x0, y0, z0, x1, y1, z1 = ox, oy, oz, ox + w, oy + h, oz + d
    # corners are listed so that uv (left, top) -> (right, top) -> (right, bottom) -> (left, bottom) follows them
    faces = {
        'north': ((0, 0, -1), [(x0, y1, z0), (x1, y1, z0), (x1, y0, z0), (x0, y0, z0)], (u + d, v + d, w, h)),
        'south': ((0, 0, 1), [(x1, y1, z1), (x0, y1, z1), (x0, y0, z1), (x1, y0, z1)], (u + d + w + d, v + d, w, h)),
        'east': ((1, 0, 0), [(x1, y1, z0), (x1, y1, z1), (x1, y0, z1), (x1, y0, z0)], (u, v + d, d, h)),
        'west': ((-1, 0, 0), [(x0, y1, z1), (x0, y1, z0), (x0, y0, z0), (x0, y0, z1)], (u + d + w, v + d, d, h)),
        'up': ((0, 1, 0), [(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)], (u + d, v, w, d)),
        'down': ((0, -1, 0), [(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)], (u + d + w, v, w, d)),
    }
    out = []
    for normal, corners, (fu, fv, fw, fh) in faces.values():
        uvs = [(fu, fv), (fu + fw, fv), (fu + fw, fv + fh), (fu, fv + fh)]
        out.append((np.array(normal, dtype=float), np.array(corners, dtype=float), np.array(uvs, dtype=float)))
    return out


def world_faces(geo):
    """Every face of every cube at rest: normal, corners and uvs after the cube's own turn about its pivot."""
    faces = []
    description = geo['description']
    for bone in geo['bones']:
        for cube in bone.get('cubes', []):
            turn = np.eye(3)
            pivot = np.array(cube.get('pivot', [0, 0, 0]), dtype=float)
            if 'rotation' in cube:
                rx, ry, rz = cube['rotation']
                turn = rot_z(rz) @ rot_y(ry) @ rot_x(rx)
            for normal, corners, uvs in cube_faces(cube, description['texture_width']):
                corners = (corners - pivot) @ turn.T + pivot
                faces.append((turn @ normal, corners, uvs))
    return faces


def project(points):
    """File space to screen: yaw about y so the front shows its west flank, then pitch down; returns x, y (up), depth."""
    view = rot_x(-PITCH) @ rot_y(YAW)
    p = points @ view.T
    return p[:, 0], p[:, 1], p[:, 2]


def draw_polygon(canvas, depth, texture, xy, uvs, z, shade):
    """Fills one projected quad (two triangles) by affine texture lookup, in front of what is already there."""
    for tri in ((0, 1, 2), (0, 2, 3)):
        pts = xy[list(tri)]
        uv = uvs[list(tri)]
        zs = z[list(tri)]
        x0, y0 = np.floor(pts.min(axis=0)).astype(int)
        x1, y1 = np.ceil(pts.max(axis=0)).astype(int)
        x0, y0 = max(x0, 0), max(y0, 0)
        x1, y1 = min(x1, canvas.shape[1] - 1), min(y1, canvas.shape[0] - 1)
        if x1 < x0 or y1 < y0:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        (ax, ay), (bx, by), (cx, cy) = pts
        det = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if abs(det) < 1e-9:
            continue
        l1 = ((by - cy) * (gx - cx) + (cx - bx) * (gy - cy)) / det
        l2 = ((cy - ay) * (gx - cx) + (ax - cx) * (gy - cy)) / det
        l3 = 1 - l1 - l2
        inside = (l1 >= -1e-6) & (l2 >= -1e-6) & (l3 >= -1e-6)
        if not inside.any():
            continue
        u = l1 * uv[0][0] + l2 * uv[1][0] + l3 * uv[2][0]
        v = l1 * uv[0][1] + l2 * uv[1][1] + l3 * uv[2][1]
        dz = l1 * zs[0] + l2 * zs[1] + l3 * zs[2]
        tx = np.clip(np.floor(u).astype(int), 0, texture.shape[1] - 1)
        ty = np.clip(np.floor(v).astype(int), 0, texture.shape[0] - 1)
        texel = texture[ty, tx].astype(float)
        visible = inside & (texel[..., 3] > 0) & (dz < depth[y0:y1 + 1, x0:x1 + 1])
        region = canvas[y0:y1 + 1, x0:x1 + 1]
        colour = texel[..., :3] * shade
        region[visible] = np.concatenate([np.clip(colour[visible], 0, 255), np.full((visible.sum(), 1), 255.0)], axis=1)
        depth[y0:y1 + 1, x0:x1 + 1][visible] = dz[visible]


def render_model():
    geo = json.loads((ASSETS / 'geo/block/mineral_gizzard.geo.json').read_text())['minecraft:geometry'][0]
    texture = np.array(Image.open(ASSETS / 'textures/block_entity/mineral_gizzard.png').convert('RGBA'))
    faces = world_faces(geo)
    allp = np.concatenate([f[1] for f in faces])
    px, py, _ = project(allp)
    scale = MODEL_SIZE * SS / max(px.max() - px.min(), py.max() - py.min())
    side = (MODEL_SIZE + 12) * SS
    canvas = np.zeros((side, side, 4))
    depth = np.full((side, side), np.inf)
    cx, cy = (px.max() + px.min()) / 2, (py.max() + py.min()) / 2
    # nearest faces win the depth test; faces that point away are skipped
    view = rot_x(-PITCH) @ rot_y(YAW)
    for normal, corners, uvs in faces:
        n = view @ normal
        if n[2] > 1e-6:  # facing away from a camera looking along +z
            continue
        x, y, z = project(corners)
        xy = np.stack([(x - cx) * scale + side / 2, (cy - y) * scale + side / 2], axis=1)
        shade = 0.62 + 0.38 * max(0.0, float(np.dot(normal, LIGHT)))
        draw_polygon(canvas, depth, texture, xy, uvs, z, shade)
    return Image.fromarray(canvas.astype(np.uint8), 'RGBA').resize((side // SS, side // SS), Image.LANCZOS)


def pool():
    """A soft pool of plum flesh the organ stands in, with a vein or two running out of it."""
    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((14, 96, 186, 168), fill=(72, 28, 46, 255))
    d.ellipse((22, 100, 178, 158), fill=(104, 44, 64, 255))
    d.ellipse((36, 106, 164, 150), fill=(132, 62, 80, 255))
    for x0, y0, x1, y1 in ((20, 126, 54, 140), (150, 120, 186, 134), (92, 150, 120, 166)):
        d.ellipse((x0, y0, x1, y1), fill=(104, 44, 64, 255))
    return img.filter(ImageFilter.GaussianBlur(1.1))


def main():
    out = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    out.alpha_composite(pool())
    model = render_model()
    out.alpha_composite(model, ((SIZE - model.width) // 2, 6))
    row = ['iron_dust', 'gold_dust', 'copper_dust', 'mineral_tailings', 'rock_residue']
    x = (SIZE - (len(row) * 34 - 2)) // 2
    for name in row:
        icon = Image.open(ASSETS / f'textures/item/{name}.png').convert('RGBA')
        if name == 'mineral_tailings':
            flecks = Image.open(ASSETS / 'textures/item/mineral_tailings_flecks.png').convert('RGBA')
            metal = (198, 200, 206)  # the iron tint, as the client colour handler multiplies it into the grey flecks
            tinted = flecks.copy()
            tinted.putdata([(g * metal[0] // 255, g * metal[1] // 255, g * metal[2] // 255, a) for g, _, _, a in flecks.getdata()])
            icon.alpha_composite(tinted)
        out.alpha_composite(icon, (x, 150))
        x += 34
    OUT.parent.mkdir(parents=True, exist_ok=True)
    out.save(OUT, optimize=True)


if __name__ == '__main__':
    main()
