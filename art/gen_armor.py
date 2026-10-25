"""The two living armor sets, Dormant and Awakened: base layers, growths, their texture, the growth file and the icons.

    python3 art/gen_armor.py

Each set is a vanilla armor set with growths on top. The base is two 64x32 layers in the vanilla humanoid armor
layout, so vanilla dye, trims and the glint fall on it as on any armor; growths are separate parts on the six bones,
with their own 128x128 box-UV sheet, written to assets/infestusfrontier/armor/<set>.json for
foundation/render/ArmorGrowths. Every texel is painted from its 3D position on the body, so patterns run on across
cube edges. Transparent texels are holes: the face, the hands and seams between plates show the wearer's skin.

Dormant flesh is the raw start: hide panels of rotten flesh sewn with sinew, a cowl, bandage wraps on the limbs,
folded hide pads on the shoulders, a rope belt and bone toggles. Plain, but built with care. The Awakened set is
alive: wine-red membrane with veins, a bone rib cage over the chest with a breathing olive pore, a vertebral ridge and
a dorsal sac, a heavy brow and crest, a bone pauldron on the right shoulder and a pored one on the left, bone bands,
knee caps, shin plates and heel spurs. Membrane growths take the dye; bone and pores keep their colours.

The icons are the pieces themselves: each slot's base boxes and growths, rendered from a three-quarter view with the
same textures, so an icon can never drift from what is worn. Dyed texels go to layer0 (tinted like leather), the rest
to layer1.

Coordinates are vanilla model units, y down, relative to each bone's pivot, as ModelPart uses them: head box
(-4,-8,-4) 8x8x8; body (-4,0,-2) 8x12x4; right arm (-3,-2,-2) about pivot (-5,2,0); left arm (-1,-2,-2) about (5,2,0);
legs (-2,0,-2) about (-1.9,12,0) and (1.9,12,0). The outer armor (hood, thorax, treads) inflates these by 1 (legs
0.9), the inner (legwraps) by 0.5 (legs 0.4); growths keep clear of those shells.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ASSETS = Path('src/main/resources/assets/infestusfrontier')
NS = 'infestusfrontier'
SHEET = 128

# --- palettes ---
HIDE = [(34, 16, 14), (60, 28, 22), (90, 44, 34), (118, 62, 46), (146, 84, 62), (172, 110, 82)]
HIDE_PALE = [(56, 36, 28), (86, 58, 44), (114, 82, 62), (142, 108, 82), (166, 134, 104)]
ROT = [(46, 50, 28), (68, 74, 38), (92, 98, 52), (116, 120, 70)]
SINEW = [(30, 16, 12), (72, 46, 32), (118, 88, 62), (160, 132, 96)]
MEMBRANE = [(26, 6, 14), (48, 12, 26), (72, 22, 40), (98, 36, 56), (126, 56, 74), (158, 86, 100)]
VEIN = (54, 8, 24)
CAPILLARY = (150, 52, 72)
IVORY = [(78, 64, 50), (118, 100, 78), (156, 138, 108), (190, 176, 142), (218, 208, 176)]
OLIVE = [(38, 44, 16), (64, 74, 26), (94, 106, 40), (128, 138, 64), (166, 172, 100)]
CLEAR = (0, 0, 0, 0)


def ramp(colours, t):
    t = max(0.0, min(1.0, t)) * (len(colours) - 1)
    i = min(int(t), len(colours) - 2)
    f = t - i
    return tuple(int(round(a + (b - a) * f)) for a, b in zip(colours[i], colours[i + 1]))


def hash3(x, y, z, seed):
    n = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791) ^ (seed * 2654435761)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def noise3(x, y, z, seed):
    """Smooth value noise in 3D, one lattice cell per unit."""
    xi, yi, zi = math.floor(x), math.floor(y), math.floor(z)
    fx, fy, fz = x - xi, y - yi, z - zi
    sx, sy, sz = (f * f * (3 - 2 * f) for f in (fx, fy, fz))
    total = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (sx if dx else 1 - sx) * (sy if dy else 1 - sy) * (sz if dz else 1 - sz)
                total += w * hash3(xi + dx, yi + dy, zi + dz, seed)
    return total


def fbm3(x, y, z, seed, scale=1.0, octaves=3):
    total, weight, norm = 0.0, 1.0, 0.0
    for o in range(octaves):
        f = scale * (2 ** o)
        total += weight * noise3(x * f, y * f, z * f, seed + o * 31)
        norm += weight
        weight *= 0.5
    return total / norm


# --- box UV: the texel layout of a cube and each texel's point on it ---

def box_faces(u, v, w, h, d):
    """Texel rectangles (x, y, w, h) of a cube's faces, Minecraft box UV: right (-x), front (-z), left (+x) and back
    (+z) in a row below top and bottom."""
    return {
        'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
        'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h), 'back': (u + d + w + d, v + d, w, h),
    }


def face_point(face, s, t, origin, size):
    """The model point under texel (s, t) of a face, at the texel's centre; the faces unfold round the cube."""
    x0, y0, z0 = origin
    w, h, d = size
    s, t = s + 0.5, t + 0.5
    if face == 'front':
        return x0 + s, y0 + t, z0
    if face == 'back':
        return x0 + w - s, y0 + t, z0 + d
    if face == 'right':
        return x0, y0 + t, z0 + d - s
    if face == 'left':
        return x0 + w, y0 + t, z0 + s
    if face == 'up':
        return x0 + s, y0, z0 + d - t
    return x0 + s, y0 + h, z0 + d - t


# --- the vanilla armor layout: which box owns which texels ---

BONES = {
    'head': dict(pivot=(0, 0, 0), origin=(-4, -8, -4), size=(8, 8, 8), uv=(0, 0)),
    'hat': dict(pivot=(0, 0, 0), origin=(-4, -8, -4), size=(8, 8, 8), uv=(32, 0)),
    'body': dict(pivot=(0, 0, 0), origin=(-4, 0, -2), size=(8, 12, 4), uv=(16, 16)),
    'right_arm': dict(pivot=(-5, 2, 0), origin=(-3, -2, -2), size=(4, 12, 4), uv=(40, 16)),
    'right_leg': dict(pivot=(-1.9, 12, 0), origin=(-2, 0, -2), size=(4, 12, 4), uv=(0, 16)),
}
# what each layer shows for each slot, as vanilla's armor layer sets part visibility
LAYER_SLOTS = {1: {'head': 'head', 'hat': 'head', 'body': 'chest', 'right_arm': 'chest', 'right_leg': 'feet'},
               2: {'body': 'legs', 'right_leg': 'legs'}}


def base_layer(paint, layer):
    img = Image.new('RGBA', (64, 32), CLEAR)
    for bone, slot in LAYER_SLOTS[layer].items():
        spec = BONES[bone]
        for face, (fx, fy, fw, fh) in box_faces(*spec['uv'], *spec['size']).items():
            for t in range(fh):
                for s in range(fw):
                    x, y, z = face_point(face, s, t, spec['origin'], spec['size'])
                    colour = paint(slot, bone, face, x, y, z, s, t, fw, fh)
                    if colour:
                        img.putpixel((fx + s, fy + t), colour if len(colour) == 4 else colour + (255,))
    return img


# --- growths ---

class Growths:
    """Parts on bones for slots, each a list of cubes with a material; packs their box UVs and paints them."""

    def __init__(self):
        self.parts = []

    def add(self, slot, bone, cubes, material, pivot=(0, 0, 0), rotation=None, breath=None, lag=0.0, dyed=False):
        """cubes: [(origin relative to pivot, size, inflate, mirror)]; pivot relative to the bone's pivot."""
        part = dict(slot=slot, bone=bone, pivot=list(pivot), cubes=[
            dict(origin=list(o), size=list(sz), inflate=inf, mirror=mir) for o, sz, inf, mir in cubes],
            material=material, dyed=dyed)
        if rotation:
            part['rotation'] = list(rotation)
        if breath:
            part['breath'], part['lag'] = breath, lag
        self.parts.append(part)

    def settle(self):
        """No growth face may lie in the plane of a base armor face it overlaps: with both sides drawn, the two would
        flicker. A growth resting on the shell sinks 0.1 into it; one ending flush with a shell edge swells by 0.05, which
        moves its faces and keeps its texture, as vanilla's own cube deformation does."""
        step = 0.1
        for part in self.parts:
            if 'rotation' in part:
                continue
            lo, hi = shell(part['bone'], part['slot'])
            for cube in part['cubes']:
                inf = cube['inflate']
                for axis in range(3):
                    others = [a for a in range(3) if a != axis]
                    clo = [part['pivot'][i] + cube['origin'][i] - inf for i in range(3)]
                    chi = [clo[i] + cube['size'][i] + 2 * inf for i in range(3)]
                    if not all(min(chi[a], hi[a]) - max(clo[a], lo[a]) > 1e-6 for a in others):
                        continue
                    if abs(chi[axis] - lo[axis]) < 1e-6:
                        cube['origin'][axis] += step  # resting on the low face: sink in
                    elif abs(clo[axis] - hi[axis]) < 1e-6:
                        cube['origin'][axis] -= step  # resting on the high face: sink in
                    elif abs(clo[axis] - lo[axis]) < 1e-6 or abs(chi[axis] - hi[axis]) < 1e-6:
                        cube['inflate'] = round(cube['inflate'] + step / 2, 4)  # flush with an edge: stand proud
                    cube['origin'][axis] = round(cube['origin'][axis], 4)

    def pack(self):
        """Shelf-packs every cube's box UV into the sheet."""
        cubes = [c for p in self.parts for c in p['cubes']]
        order = sorted(range(len(cubes)), key=lambda i: -(cubes[i]['size'][1] + cubes[i]['size'][2]))
        x = y = shelf = 0
        for i in order:
            w, h, d = (math.ceil(v) for v in cubes[i]['size'])
            fw, fh = 2 * w + 2 * d, h + d
            if x + fw > SHEET:
                x, y, shelf = 0, y + shelf + 1, 0
            cubes[i]['uv'] = [x, y]
            x += fw + 1
            shelf = max(shelf, fh)
        assert y + shelf <= SHEET, 'growths overflow the sheet'

    def paint(self, painters):
        img = Image.new('RGBA', (SHEET, SHEET), CLEAR)
        for part in self.parts:
            px, py, pz = part['pivot']
            bx, by, bz = BONES['right_arm' if part['bone'] == 'left_arm' else
                               'right_leg' if part['bone'] == 'left_leg' else part['bone']]['pivot']
            for cube in part['cubes']:
                size = [math.ceil(v) for v in cube['size']]
                origin = [px + cube['origin'][0], py + cube['origin'][1], pz + cube['origin'][2]]
                for face, (fx, fy, fw, fh) in box_faces(*cube['uv'], *size).items():
                    for t in range(fh):
                        for s in range(fw):
                            x, y, z = face_point(face, s, t, origin, size)
                            colour = painters[part['material']](face, x + bx, y + by, z + bz, s, t, fw, fh, size)
                            if colour:
                                img.putpixel((fx + s, fy + t), colour if len(colour) == 4 else colour + (255,))
        return img

    def json(self, name):
        parts = []
        for p in self.parts:
            out = {k: v for k, v in p.items() if k != 'material'}
            out['cubes'] = [{k: v for k, v in c.items() if not (k == 'mirror' and not v)} for c in p['cubes']]
            parts.append(out)
        return {'texture': f'{NS}:textures/models/armor/{name}_growths.png', 'size': [SHEET, SHEET], 'parts': parts}


def shell(bone, slot):
    """The base armor box a slot draws on a bone, in bone units: vanilla inflates the outer armor by 1 and the inner
    (legwraps) by 0.5, legs 0.1 less."""
    spec = LOCAL[bone]
    (x, y, z), (w, h, d) = spec['origin'], spec['size']
    inflate = (0.5 if slot == 'legs' else 1.0) - (0.1 if bone.endswith('leg') else 0)
    return (x - inflate, y - inflate, z - inflate), (x + w + inflate, y + h + inflate, z + d + inflate)


# --- shading helpers ---

def edge_light(face, s, t, w, h):
    """Lit from the upper left: top edges brighter, bottom edges darker; tops brighter than sides."""
    v = 0.0
    if t == 0:
        v += 0.12
    if t == h - 1:
        v -= 0.14
    if face == 'up':
        v += 0.1
    elif face == 'down':
        v -= 0.18
    elif face in ('right', 'back'):
        v -= 0.06
    return v


# ============================================================================== Dormant flesh

def dormant_seam(bone, face, x, y, z):
    """Where the hide panels are sewn, as (distance across the seam, position along it): down the chest and back,
    round the waist, down the hood's crown, along the outer side of each sleeve and leg. None off any seam."""
    seams = []
    if bone == 'body':
        if face in ('front', 'back') and y < 8:
            seams.append((x + 1, y))
        if face not in ('up', 'down'):
            seams.append((y - 8.5, x + z))
    elif bone == 'head' and face in ('up', 'back'):
        seams.append((x, y + z))
    elif bone in ('right_arm', 'right_leg') and face == 'right':
        seams.append((z, y))
    for across, along in seams:
        if abs(across) < 1.6:
            return abs(across), along
    return None


def dormant_base(slot, bone, face, x, y, z, s, t, w, h):
    if bone == 'hat':
        return None
    seed = 11
    if bone == 'head':
        # an open face: the cowl frames it, brow down to row 2, cheeks at the edges
        if face == 'front' and 2 <= t <= 7 and 1 <= s <= 6:
            return None
        if face == 'down':
            return None
    if bone == 'right_arm' and y > 5.5:
        return None  # bare forearm and hand below the sleeve
    if bone == 'right_leg':
        if slot == 'feet' and y < 7:
            return None
        if slot == 'legs' and y > 10:
            return None
    if bone == 'body' and slot == 'legs' and y < 8:
        return None
    # hide panels: each panel of the body has its own tone, parted by sewn seams; a few patches of rot
    field = fbm3(x * 0.22, y * 0.22, z * 0.22, seed + 5) * 3
    panel = int(field)
    base = 0.46 + 0.1 * fbm3(x, y, z, seed, 0.35) + 0.04 * panel + edge_light(face, s, t, w, h)
    colour = ramp(HIDE if panel != 1 else HIDE_PALE, base)
    rot = fbm3(x, y, z, seed + 9, 0.3)
    if rot > 0.7:
        colour = ramp(ROT, 0.3 + (rot - 0.7) * 1.5)
    seam = dormant_seam(bone, face, x, y, z)
    if seam:
        across, along = seam
        if across < 0.6:
            colour = ramp(SINEW, 0.12)  # the dark sewn join
        elif math.floor(along) % 2 == 0:
            colour = ramp(SINEW, 0.9)  # a pale stitch crossing it
    # bandage wraps on the limbs: pale strips three rows high, each with a shadowed lower edge
    if bone in ('right_arm', 'right_leg') and face not in ('up', 'down') and not seam:
        row = int(y) % 4
        if row in (1, 2):
            colour = ramp(HIDE_PALE, 0.52 + (0.08 if row == 1 else 0) + 0.06 * fbm3(x, y, z, seed + 2))
        elif row == 3:
            colour = ramp(HIDE, 0.2)
    # the head's crown seam
    if bone == 'head' and face in ('up', 'back') and abs(x) < 0.6:
        colour = ramp(SINEW, 0.75 if t % 2 else 0.2)
    return colour


def dormant_growths():
    g = Growths()
    # hood: a stiff hide brim over the brow, a sinew-wrapped bone nose guard, tie knots at the temples
    g.add('head', 'head', [((-4.5, -9.75, -5.75), (9, 1, 2), 0.0, False)], 'hide', dyed=True)
    g.add('head', 'head', [((-0.5, -7.5, -5.6), (1, 4, 1), 0.0, False)], 'toggle')
    for x in (-5.75, 4.75):
        g.add('head', 'head', [((x, -6.5, -1), (1, 1, 1), 0.0, False)], 'knot')
    # thorax: folded hide pads tied on the shoulders, a bone toggle at the collar, a stitched patch on the breast
    g.add('chest', 'right_arm', [((-4.5, -3.5, -3.25), (5, 2, 6), 0.0, False)], 'hide', dyed=True)
    g.add('chest', 'right_arm', [((-4.6, -1.5, -3.1), (2, 2, 6), 0.0, False)], 'hide', dyed=True)
    g.add('chest', 'left_arm', [((-0.5, -3.5, -3.25), (5, 2, 6), 0.0, False)], 'hide', dyed=True)
    g.add('chest', 'left_arm', [((2.6, -1.5, -3.1), (2, 2, 6), 0.0, False)], 'hide', dyed=True)
    for arm, x in (('right_arm', -4.25), ('left_arm', 3.25)):
        g.add('chest', arm, [((x, 1, -3.5), (1, 1, 7), 0.0, False)], 'cord')
    g.add('chest', 'body', [((-1, -0.25, -3.75), (2, 1, 1), 0.0, False)], 'toggle')
    g.add('chest', 'body', [((-3.5, 2, -3.5), (3, 4, 1), 0.0, False)], 'patch', dyed=True)
    for arm, x0 in (('right_arm', -3), ('left_arm', -1)):
        g.add('chest', arm, [((x0, 6, -2), (4, 3, 4), 0.55, False)], 'wrap', dyed=True)
        g.add('chest', arm, [((x0, 5, -2), (4, 1, 4), 1.2, False)], 'cord')
    # legwraps: a rope belt with a knot, a wrap below each knee
    g.add('legs', 'body', [((-4.5, 10.5, -2.5), (9, 1, 5), 0.6, False)], 'cord')
    g.add('legs', 'body', [((-2.5, 10, -3.9), (2, 2, 1), 0.0, False)], 'knot')
    for leg in ('right_leg', 'left_leg'):
        g.add('legs', leg, [((-2.5, 5, -2.5), (5, 1, 5), 0.3, False)], 'cord')
        g.add('legs', leg, [((-1.5, 3.5, -3.1), (3, 2, 1), 0.0, False)], 'patch', dyed=True)
    # treads: wrapped feet, a toe pad and a bone toggle at the ankle
    for leg in ('right_leg', 'left_leg'):
        g.add('feet', leg, [((-3, 8.5, -3), (6, 1, 6), 0.3, False)], 'cord')
        g.add('feet', leg, [((-2.5, 11, -3.9), (5, 2, 1), 0.0, False)], 'hide', dyed=True)
        g.add('feet', leg, [((-0.5, 9.5, -3.75), (1, 1, 1), 0.0, False)], 'toggle')
    return g


def dormant_painters():
    def hide(face, x, y, z, s, t, w, h, size):
        v = 0.45 + 0.2 * fbm3(x, y, z, 21, 0.7) + edge_light(face, s, t, w, h)
        c = ramp(HIDE, v)
        if t == h - 1 and face not in ('up', 'down') and s % 2 == 0:
            c = ramp(SINEW, 0.7)  # a hem stitched along the bottom
        return c

    def patch(face, x, y, z, s, t, w, h, size):
        border = s == 0 or t == 0 or s == w - 1 or t == h - 1
        if border and face == 'front':
            return ramp(SINEW, 0.75 if (s + t) % 2 else 0.2)
        return ramp(HIDE_PALE, 0.45 + 0.2 * fbm3(x, y, z, 23) + edge_light(face, s, t, w, h))

    def cord(face, x, y, z, s, t, w, h, size):
        twist = (s + t + int(y * 2)) % 3
        return ramp(SINEW, 0.35 + 0.18 * twist + edge_light(face, s, t, w, h))

    def wrap(face, x, y, z, s, t, w, h, size):
        if face in ('up', 'down'):
            return None  # a bandage round the bare forearm, open at both ends
        band = (t + s // 2) % 3
        return ramp(HIDE_PALE, 0.48 + (0.1 if band == 0 else -0.12 if band == 2 else 0) + edge_light(face, s, t, w, h))

    def knot(face, x, y, z, s, t, w, h, size):
        return ramp(SINEW, 0.55 + edge_light(face, s, t, w, h))

    def toggle(face, x, y, z, s, t, w, h, size):
        v = 0.6 + 0.15 * fbm3(x, y, z, 25, 1.5) + edge_light(face, s, t, w, h)
        c = ramp(IVORY, v)
        if face in ('front', 'back') and size[1] > 2 and t % 2 == 1:
            c = ramp(SINEW, 0.4)  # sinew wound round the bone
        return c

    return dict(hide=hide, patch=patch, cord=cord, knot=knot, toggle=toggle, wrap=wrap)


# ============================================================================== Awakened, the living frame

def awakened_base(slot, bone, face, x, y, z, s, t, w, h):
    if bone == 'hat':
        return None
    seed = 41
    if bone == 'head':
        if face == 'front' and 2 <= t <= 7 and 1 <= s <= 6:
            return None
        if face == 'down':
            return None
    if bone == 'right_arm' and y > 4.5:
        return None
    if bone == 'right_leg':
        if slot == 'feet' and y < 6:
            return None
        if slot == 'legs' and y > 10:
            return None
    if bone == 'body' and slot == 'legs' and y < 7:
        return None
    # living membrane in muscle bands: each band lit on its top row and shadowed on its bottom one, dark veins
    period = 3 if bone in ('right_arm', 'right_leg') else 4
    r = (y + (0.5 if bone == 'right_leg' else 0)) % period
    band = 0.0 if face in ('up', 'down') else (0.13 if r < 1 else -0.15 if r > period - 1 else 0.0)
    lobe = fbm3(x, y, z, seed, 0.3)
    fibre = 0.025 if (s + (t // 3)) % 2 == 0 else -0.025
    v = 0.4 + 0.1 * lobe + band + fibre + edge_light(face, s, t, w, h)
    colour = ramp(MEMBRANE, v)
    # between limb bands the membrane parts on the sides and back, and the skin shows
    if bone in ('right_arm', 'right_leg') and face in ('right', 'left', 'back') and r > period - 1 \
            and fbm3(x, y, z, seed + 7, 0.8) > 0.45:
        return None
    # seams between plates show the skin: a split down the sternum, the sides of the waist
    if bone == 'body' and face == 'front' and abs(x) < 0.6 and 1 < y < 9:
        return None
    if bone == 'body' and face in ('right', 'left') and 5 < y < 9 and slot == 'chest':
        return None
    # pale growth rims at the open edges of the hood and the sleeves
    if bone == 'head' and face == 'front' and (t == 1 or (t > 1 and s in (0, 7))):
        colour = ramp(MEMBRANE, 0.78)
    if bone == 'right_arm' and 3.5 < y <= 4.5:
        colour = ramp(MEMBRANE, 0.72)
    return colour


def awakened_growths():
    g = Growths()
    # hood: a stepped brow, membrane over bone; three crest spines rising towards the back; cheek plates; two olive
    # sensory nodules at the temples; shingled nape plates
    g.add('head', 'head', [((-5, -8.5, -5.6), (10, 1, 1), 0.0, False)], 'plate', dyed=True)
    g.add('head', 'head', [((-4, -7.5, -5.8), (8, 1, 1), 0.0, False)], 'bone')
    for i, (z, hgt) in enumerate(((-3, 1), (-0.5, 2), (2, 3))):
        g.add('head', 'head', [((-0.5, -9 - hgt, z), (1, hgt, 1), 0.0, False)], 'bone')
    for x in (-5.75, 4.75):
        g.add('head', 'head', [((x, -6, -4.5), (1, 4, 3), 0.0, False)], 'plate', dyed=True)
        g.add('head', 'head', [((x, -2.25, -3.5), (1, 1, 2), 0.0, False)], 'bone')
    for i, x in enumerate((-4.75, 3.75)):
        g.add('head', 'head', [((-0.5, -0.5, -0.5), (1, 1, 1), 0.0, False)], 'pore',
              pivot=(x + 0.5, -7.75, -4.5), breath=0.3, lag=i * 0.8)
    for i, (y, w) in enumerate(((-6.75, 8), (-4.75, 7), (-2.75, 6))):
        g.add('head', 'head', [((-w / 2, y, 4.8 + i * 0.15), (w, 2, 1), 0.0, False)], 'plate', dyed=True)
    # thorax: a rib cage of bone over the membrane, a sternum, a breathing olive heart-pore, belly segments,
    # a vertebral ridge, shoulder blades and a dorsal sac on the back
    g.add('chest', 'body', [((-0.5, 0.25, -3.9), (1, 8, 1), 0.0, False)], 'bone')
    for i, y in enumerate((1.5, 3.5, 5.5)):
        width = 4 - (i == 2)
        g.add('chest', 'body', [((-0.5 - width, y, -3.7), (width, 1, 1), 0.0, False)], 'bone')
        g.add('chest', 'body', [((0.5, y, -3.7), (width, 1, 1), 0.0, False)], 'bone')
    g.add('chest', 'body', [((-1, -1, -0.5), (2, 2, 1), 0.0, False)], 'pore', pivot=(2.5, 4.5, -3.75),
          breath=0.22)
    for i, y in enumerate((8, 9.5, 11)):
        g.add('chest', 'body', [((-3.5, -0.5, -0.5), (7, 1, 1), 0.0, False)], 'plate', dyed=True,
              pivot=(0, y + 0.5, -3.3 - i * 0.1), breath=0.05, lag=0.6 + i * 0.3)
    for y in (0.5, 2.5, 4.5, 6.5, 8.5):
        g.add('chest', 'body', [((-1, y, 3), (2, 1, 1), 0.0, False)], 'bone')
    for x in (-4.5, 1.5):
        g.add('chest', 'body', [((x, 0.5, 3.2), (3, 4, 1), 0.0, False)], 'plate', dyed=True)
    g.add('chest', 'body', [((-1.5, -1.5, 0), (3, 3, 2), 0.0, False)], 'pore', pivot=(0, 10.5, 3.3),
          breath=0.15, lag=1.4)
    # shoulders: three membrane shingles stepping down and out each shoulder; bone claws on the right,
    # a cluster of olive pores on the left
    for i in range(3):
        g.add('chest', 'right_arm', [((-4.4 - i * 0.2, -3.6 + i * 1.2, -3.1 + i * 0.15), (4 - i, 1, 6), 0.0,
                                      False)], 'plate', dyed=True)
        g.add('chest', 'left_arm', [((0.4 + i * 1.2, -3.6 + i * 1.2, -3.1 + i * 0.15), (4 - i, 1, 6), 0.0,
                                     False)], 'plate', dyed=True)
    for z in (-2, 0.5):
        g.add('chest', 'right_arm', [((-3.5, -5, z), (1, 2, 1), 0.0, False)], 'bone')
    g.add('chest', 'right_arm', [((-4.75, -2.5, -1), (1, 1, 2), 0.0, False)], 'bone')
    for i, (x, z, sz) in enumerate(((1.5, -1.5, 2), (2.8, 0.75, 1), (0.6, 1, 1))):
        g.add('chest', 'left_arm', [((-sz / 2, -sz, -sz / 2), (sz, sz, sz), 0.0, False)], 'pore',
              pivot=(x, -3.5, z), breath=0.3, lag=0.4 + i * 0.7)
    for arm, x0 in (('right_arm', -3), ('left_arm', -1)):
        g.add('chest', arm, [((x0, 1.5, -2), (4, 2, 4), 1.3, False)], 'plate', dyed=True)
        g.add('chest', arm, [((x0, 5.5, -2), (4, 3, 4), 0.45, False)], 'sleeve', dyed=True)
        g.add('chest', arm, [((x0, 6, -2), (4, 1, 4), 0.8, False)], 'bone')
    # legwraps: a sinew belt, two shingled thigh rings, a small bone knee cap
    g.add('legs', 'body', [((-4, 10.5, -2), (8, 1, 4), 0.9, False)], 'bone')
    for leg in ('right_leg', 'left_leg'):
        g.add('legs', leg, [((-2, 0.5, -2), (4, 2, 4), 0.75, False)], 'plate', dyed=True)
        g.add('legs', leg, [((-2, 2.5, -2), (4, 1, 4), 0.95, False)], 'plate', dyed=True)
        g.add('legs', leg, [((-1, 4.25, -3.1), (2, 2, 1), 0.0, False)], 'bone')
        g.add('legs', leg, [((-2, 7, -2), (4, 1, 4), 0.75, False)], 'plate', dyed=True)
    # treads: shin shingles, an ankle ring, a membrane toe cap with three bone claws, a heel spur
    for leg in ('right_leg', 'left_leg'):
        g.add('feet', leg, [((-1.5, 6.25, -3.75), (3, 2, 1), 0.0, False)], 'plate', dyed=True)
        g.add('feet', leg, [((-1, 8, -3.9), (2, 1, 1), 0.0, False)], 'bone')
        g.add('feet', leg, [((-2, 9.25, -2), (4, 1, 4), 1.25, False)], 'plate', dyed=True)
        g.add('feet', leg, [((-2.5, 11, -3.8), (5, 2, 1), 0.0, False)], 'plate', dyed=True)
        for x in (-2, -0.5, 1):
            g.add('feet', leg, [((x, 12, -4.8), (1, 1, 1), 0.0, False)], 'bone')
        g.add('feet', leg, [((-0.5, 10.5, 2.9), (1, 1, 2), 0.0, False)], 'bone')
    return g


def awakened_painters():
    def bone(face, x, y, z, s, t, w, h, size):
        v = 0.55 + 0.18 * fbm3(x, y, z, 51, 1.2) + edge_light(face, s, t, w, h)
        if (s == 0 or s == w - 1) and face in ('front', 'back') and w > 2:
            v -= 0.1
        return ramp(IVORY, v)

    def plate(face, x, y, z, s, t, w, h, size):
        v = 0.42 + 0.25 * fbm3(x, y, z, 53, 0.8) + edge_light(face, s, t, w, h)
        c = ramp(MEMBRANE, v)
        if t == 0 and face not in ('up', 'down'):
            c = ramp(MEMBRANE, 0.85)  # a pale growth rim on the top edge
        return c

    def pore(face, x, y, z, s, t, w, h, size):
        centre = (s in (w // 2 - (w % 2 == 0), w // 2)) and (t in (h // 2 - (h % 2 == 0), h // 2))
        if centre and w > 1 and h > 1 and face in ('front', 'back', 'up'):
            return ramp(OLIVE, 0.12)
        return ramp(OLIVE, 0.55 + 0.2 * fbm3(x, y, z, 55, 1.5) + edge_light(face, s, t, w, h))

    def sleeve(face, x, y, z, s, t, w, h, size):
        if face in ('up', 'down'):
            return None  # a living bracer round the bare forearm, open at both ends
        v = 0.4 + 0.2 * fbm3(x, y, z, 57, 0.8) + (0.12 if t == 0 else -0.14 if t == h - 1 else 0)
        return ramp(MEMBRANE, v)

    return dict(bone=bone, plate=plate, pore=pore, sleeve=sleeve)


# ============================================================================== icons

SLOT_BONES = {'head': ['head'], 'chest': ['body', 'right_arm', 'left_arm'], 'legs': ['body', 'right_leg', 'left_leg'],
              'feet': ['right_leg', 'left_leg']}
PIVOTS = {'head': (0, 0, 0), 'body': (0, 0, 0), 'right_arm': (-5, 2, 0), 'left_arm': (5, 2, 0),
          'right_leg': (-1.9, 12, 0), 'left_leg': (1.9, 12, 0)}
LOCAL = {'head': BONES['head'], 'body': BONES['body'], 'right_arm': BONES['right_arm'],
         'left_arm': dict(origin=(-1, -2, -2), size=(4, 12, 4), uv=(40, 16)),
         'right_leg': BONES['right_leg'], 'left_leg': BONES['right_leg']}
FACE_NORMALS = {'front': (0, 0, -1), 'back': (0, 0, 1), 'right': (-1, 0, 0), 'left': (1, 0, 0), 'up': (0, -1, 0),
                'down': (0, 1, 0)}


def rot_matrix(rx, ry, rz):
    """Rotation in degrees about x, then y, then z, as ModelPart applies its rotations (z first on the vector)."""
    def rx_(a):
        c, s = math.cos(a), math.sin(a)
        return [[1, 0, 0], [0, c, -s], [0, s, c]]

    def ry_(a):
        c, s = math.cos(a), math.sin(a)
        return [[c, 0, s], [0, 1, 0], [-s, 0, c]]

    def rz_(a):
        c, s = math.cos(a), math.sin(a)
        return [[c, -s, 0], [s, c, 0], [0, 0, 1]]

    def mul(a, b):
        return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return mul(mul(rz_(math.radians(rz)), ry_(math.radians(ry))), rx_(math.radians(rx)))


def apply(m, p):
    return tuple(sum(m[i][k] * p[k] for k in range(3)) for i in range(3))


def cube_quads(origin, size, inflate, uv, texture, mirror, transform, source):
    """The six faces of a cube as (corners in view space, texel rectangle, texture, source)."""
    x0, y0, z0 = (o - inflate for o in origin)
    w, h, d = (s + 2 * inflate for s in size)
    tw, th, td = (math.ceil(s) for s in size)
    rects = box_faces(*uv, tw, th, td)
    corners = {
        'front': [(x0, y0, z0), (x0 + w, y0, z0), (x0 + w, y0 + h, z0), (x0, y0 + h, z0)],
        'back': [(x0 + w, y0, z0 + d), (x0, y0, z0 + d), (x0, y0 + h, z0 + d), (x0 + w, y0 + h, z0 + d)],
        'right': [(x0, y0, z0 + d), (x0, y0, z0), (x0, y0 + h, z0), (x0, y0 + h, z0 + d)],
        'left': [(x0 + w, y0, z0), (x0 + w, y0, z0 + d), (x0 + w, y0 + h, z0 + d), (x0 + w, y0 + h, z0)],
        'up': [(x0, y0, z0 + d), (x0 + w, y0, z0 + d), (x0 + w, y0, z0), (x0, y0, z0)],
        'down': [(x0, y0 + h, z0 + d), (x0 + w, y0 + h, z0 + d), (x0 + w, y0 + h, z0), (x0, y0 + h, z0)],
    }
    out = []
    for face, quad in corners.items():
        rect = rects[face]
        if mirror and face in ('right', 'left'):
            rect = rects['left' if face == 'right' else 'right']
        out.append(([transform(p) for p in quad], rect, texture, source, mirror))
    return out


def view(yaw, pitch):
    m = rot_matrix(pitch, yaw, 0)
    return lambda p: apply(m, p)


def render_icon(quads, size=32, supersample=12):
    """Draws textured quads, nearest first last, orthographically, then fits the result in a size x size icon."""
    pts = [p for q in quads for p in q[0]]
    minx, maxx = min(p[0] for p in pts), max(p[0] for p in pts)
    miny, maxy = min(p[1] for p in pts), max(p[1] for p in pts)
    span = max(maxx - minx, maxy - miny)
    scale = (size - 2) * supersample / span
    big = size * supersample
    ox = (big - (maxx - minx) * scale) / 2 - minx * scale
    oy = (big - (maxy - miny) * scale) / 2 - miny * scale
    colour = Image.new('RGBA', (big, big), CLEAR)
    owner = [[None] * big for _ in range(big)]
    depth = [[1e9] * big for _ in range(big)]
    cpx = colour.load()
    for corners, (rx, ry, rw, rh), texture, source, mirror in quads:
        a, b, _, d = [(p[0] * scale + ox, p[1] * scale + oy, p[2]) for p in corners]
        e1 = (b[0] - a[0], b[1] - a[1])
        e2 = (d[0] - a[0], d[1] - a[1])
        det = e1[0] * e2[1] - e1[1] * e2[0]
        if det <= 1e-6:  # facing away (or edge on)
            continue
        xs = [a[0], b[0], d[0], b[0] + e2[0]]
        ys = [a[1], b[1], d[1], b[1] + e2[1]]
        tex = texture.load()
        z0 = sum(p[2] for p in corners) / 4
        for py in range(max(0, int(min(ys))), min(big, int(max(ys)) + 1)):
            for px in range(max(0, int(min(xs))), min(big, int(max(xs)) + 1)):
                qx, qy = px + 0.5 - a[0], py + 0.5 - a[1]
                u = (qx * e2[1] - qy * e2[0]) / det
                v = (e1[0] * qy - e1[1] * qx) / det
                if not (0 <= u < 1 and 0 <= v < 1):
                    continue
                tu = u if not mirror else 1 - u
                c = tex[rx + min(rw - 1, int(tu * rw)), ry + min(rh - 1, int(v * rh))]
                if c[3] < 128 or z0 >= depth[py][px]:
                    continue
                depth[py][px] = z0
                cpx[px, py] = c
                owner[py][px] = source
    return colour, owner, supersample


def crop(colour, owner, supersample, size=32):
    """Fits what was drawn, not the boxes' extent, into the icon: crop to the opaque samples, centre, scale."""
    box = colour.getbbox()
    big = size * supersample
    w, h = box[2] - box[0], box[3] - box[1]
    k = min((size - 2) * supersample / w, (size - 2) * supersample / h)
    out = Image.new('RGBA', (big, big), CLEAR)
    owned = [[None] * big for _ in range(big)]
    src, dst = colour.load(), out.load()
    ox, oy = (big - w * k) / 2, (big - h * k) / 2
    for y in range(big):
        for x in range(big):
            sx, sy = int((x - ox) / k) + box[0], int((y - oy) / k) + box[1]
            if box[0] <= sx < box[2] and box[1] <= sy < box[3] and owner[sy][sx] is not None:
                dst[x, y] = src[sx, sy]
                owned[y][x] = owner[sy][sx]
    return out, owned


def downsample(colour, owner, supersample, size=32):
    """Majority vote per icon pixel: the most common opaque colour source wins, averaged over its samples."""
    colour, owner = crop(colour, owner, supersample, size)
    cpx = colour.load()
    layers = {True: Image.new('RGBA', (size, size), CLEAR), False: Image.new('RGBA', (size, size), CLEAR)}
    for y in range(size):
        for x in range(size):
            samples = []
            for sy in range(supersample):
                for sx in range(supersample):
                    px, py = x * supersample + sx, y * supersample + sy
                    if owner[py][px] is not None:
                        samples.append((cpx[px, py], owner[py][px]))
            if len(samples) < supersample * supersample * 0.4:
                continue
            dyed = sum(1 for _, o in samples if o) * 2 >= len(samples)
            chosen = [c for c, o in samples if o == dyed]
            avg = tuple(sum(c[i] for c in chosen) // len(chosen) for i in range(3)) + (255,)
            layers[dyed].putpixel((x, y), avg)
    return layers


def outline(layers, size=32):
    """A dark rim round the silhouette, as vanilla icons have, on the plain layer so dye does not tint it."""
    a, b = layers[True].load(), layers[False].load()
    solid = [[a[x, y][3] > 0 or b[x, y][3] > 0 for x in range(size)] for y in range(size)]
    for y in range(size):
        for x in range(size):
            if solid[y][x]:
                continue
            near = any(0 <= x + dx < size and 0 <= y + dy < size and solid[y + dy][x + dx]
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if near:
                b[x, y] = (24, 10, 14, 255)


ICON_SHOULDER = 3  # how far down the arm the chest icon shows, in arm units from the shoulder pivot


def icon_pivot(bone, slot):
    """Where a bone sits in its icon: as worn, but legs set apart, as vanilla draws leggings and boots."""
    x, y, z = PIVOTS[bone]
    if bone.endswith('arm'):
        x += -1 if bone.startswith('right') else 1
    if bone.endswith('leg'):
        x += (-1.2 if bone.startswith('right') else 1.2) * (2 if slot == 'feet' else 1)
    return x, y, z


def icon(set_name, piece, slot, layer_imgs, growth_img, growths):
    layer = 2 if slot == 'legs' else 1
    base_tex = layer_imgs[layer]
    inner = layer == 2
    yaw, pitch = (-24, 14) if slot == 'head' else (-12, 6)
    to_view = view(yaw, pitch)
    quads = []
    for bone in SLOT_BONES[slot]:
        spec = LOCAL[bone]
        pivot = icon_pivot(bone, slot)
        inflate = (0.5 if inner else 1.0) - (0.1 if 'leg' in bone else 0)
        origin = tuple(o + p for o, p in zip(spec['origin'], pivot))
        size = spec['size']
        if slot == 'chest' and bone.endswith('arm'):
            size = (size[0], ICON_SHOULDER - spec['origin'][1], size[2])  # the upper arm only
        quads += cube_quads(origin, size, inflate, spec['uv'], base_tex, bone.startswith('left'), to_view, True)
    for part in growths.parts:
        if part['slot'] != slot:
            continue
        pivot = icon_pivot(part['bone'], slot)
        if slot == 'chest' and part['bone'].endswith('arm') and part['pivot'][1] + part['cubes'][0]['origin'][1] > ICON_SHOULDER:
            continue  # bracers below the shoulder stay off the icon
        m = rot_matrix(*(part.get('rotation') or (0, 0, 0)))
        ppx, ppy, ppz = part['pivot']
        for cube in part['cubes']:
            def place(p, m=m):
                r = apply(m, p)
                return to_view((r[0] + ppx + pivot[0], r[1] + ppy + pivot[1], r[2] + ppz + pivot[2]))
            quads += cube_quads(cube['origin'], cube['size'], cube['inflate'], cube['uv'], growth_img,
                                cube.get('mirror', False), place, part['dyed'])
    colour, owner, ss = render_icon(quads)
    layers = downsample(colour, owner, ss)
    outline(layers)
    out = ASSETS / 'textures/item'
    layers[True].save(out / f'{set_name}_{piece}.png', optimize=True)
    layers[False].save(out / f'{set_name}_{piece}_overlay.png', optimize=True)
    (ASSETS / 'models/item' / f'{set_name}_{piece}.json').write_text(json.dumps({
        'parent': 'minecraft:item/generated',
        'textures': {'layer0': f'{NS}:item/{set_name}_{piece}', 'layer1': f'{NS}:item/{set_name}_{piece}_overlay'}},
        indent=2) + '\n')


PIECES = (('hood', 'head'), ('thorax', 'chest'), ('legwraps', 'legs'), ('treads', 'feet'))


def write_set(name, base_paint, growths, painters):
    layers = {n: base_layer(base_paint, n) for n in (1, 2)}
    armor = ASSETS / 'textures/models/armor'
    for n, img in layers.items():
        img.save(armor / f'{name}_layer_{n}.png', optimize=True)
    growths.settle()
    growths.pack()
    growth_img = growths.paint(painters)
    growth_img.save(armor / f'{name}_growths.png', optimize=True)
    (ASSETS / 'armor').mkdir(parents=True, exist_ok=True)
    (ASSETS / 'armor' / f'{name}.json').write_text(json.dumps(growths.json(name), indent=1) + '\n')
    for piece, slot in PIECES:
        icon(name, piece, slot, layers, growth_img, growths)
    return sum(len(p['cubes']) for p in growths.parts)


def main():
    d = write_set('dormant', dormant_base, dormant_growths(), dormant_painters())
    a = write_set('awakened', awakened_base, awakened_growths(), awakened_painters())
    print(f'armor: dormant {d} growth cubes, awakened {a}; layers, growth sheets, growth files and icons')


if __name__ == '__main__':
    main()
