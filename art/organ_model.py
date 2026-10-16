"""Shared pipeline for the organ models: one Python description becomes the GeckoLib geo and animation JSON,
the box-UV texture and glowmask, the break-particle texture and a Blockbench source.

A description is a list of bones: (name, parent, pivot, cubes), each cube (island, origin, size, turn) where
turn is None or (rotation, pivot). Cubes naming the same island share its texels. Coordinates are those of the
geo file: +Y up, the block's front is north (-Z), the block spans -8..8 on X and Z.

Deterministic: fixed seeds and UUIDs, no timestamps.
"""
import base64
import io
import json
import math
import random
import re
import uuid
from pathlib import Path
from PIL import Image

ASSETS = Path('src/main/resources/assets/infestusfrontier')


def islands(bones):
    """island -> size, in first-use order."""
    out = {}
    for _, _, _, cubes in bones:
        for island, _, size, _ in cubes:
            assert out.setdefault(island, size) == size, island
    return out


def pack(bones, sheet):
    """Shelf-packs every island's box-UV footprint into the sheet; island -> (u, v)."""
    sizes = islands(bones)
    order = sorted(sizes, key=lambda n: (-(sizes[n][1] + sizes[n][2]), -(2 * sizes[n][0] + 2 * sizes[n][2]), n))
    uv, x, y, shelf = {}, 0, 0, 0
    for name in order:
        w, h, d = sizes[name]
        fw, fh = 2 * w + 2 * d, h + d
        if x + fw > sheet:
            x, y, shelf = 0, y + shelf + 1, 0
        uv[name] = (x, y)
        x += fw + 1
        shelf = max(shelf, fh)
    assert y + shelf <= sheet, (y + shelf, sheet)
    return uv


def faces(size, uv):
    """Box-UV rectangles (x, y, w, h) of one cube."""
    (w, h, d), (u, v) = size, uv
    return {
        'up': (u + d, v, w, d), 'down': (u + d + w, v, w, d),
        'east': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
        'west': (u + d + w, v + d, d, h), 'south': (u + d + w + d, v + d, w, h),
    }


class Painter:
    def __init__(self, sheet, seed):
        self.rng = random.Random(seed)
        self.size = sheet
        self.img = Image.new('RGBA', (sheet, sheet), (0, 0, 0, 0))
        self.glow = Image.new('RGBA', (sheet, sheet), (0, 0, 0, 0))
        self.lattice = [[self.rng.random() for _ in range(sheet // 3 + 2)] for _ in range(sheet // 3 + 2)]

    def blotch(self, x, y):
        """Smooth noise over the sheet, three texels to a cell."""
        gx, gy = x / 3, y / 3
        ix, iy = int(gx), int(gy)
        fx, fy = gx - ix, gy - iy
        a, b = self.lattice[iy][ix], self.lattice[iy][ix + 1]
        c, d = self.lattice[iy + 1][ix], self.lattice[iy + 1][ix + 1]
        return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy

    def ramp(self, rect, ramp, value, grain=0.07, mottle=0.22, alpha=255):
        """Fills rect from the ramp; value(x, y, w, h) -> 0..1 before the noise is added."""
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                v = value(x, y, w, h) + mottle * (self.blotch(x0 + x, y0 + y) - 0.5) + grain * (self.rng.random() - 0.5) * 2
                i = max(0, min(len(ramp) - 1, int(v * len(ramp))))
                self.img.putpixel((x0 + x, y0 + y), ramp[i] + (alpha,))

    def put(self, rect, x, y, colour, alpha=255, glow=None):
        x0, y0, w, h = rect
        if 0 <= x < w and 0 <= y < h:
            self.img.putpixel((x0 + x, y0 + y), colour + (alpha,))
            if glow:
                self.glow.putpixel((x0 + x, y0 + y), colour + (glow,))

    def vein(self, rect, colour, length, alpha=255):
        """A wandering line, mostly downwards."""
        _, _, w, h = rect
        x, y = self.rng.randrange(w), self.rng.randrange(max(1, h // 2))
        for _ in range(length):
            self.put(rect, x, y, colour, alpha)
            x += self.rng.choice((-1, 0, 0, 0, 1))
            y += 1

    def glow_rect(self, rect, alpha=255):
        """Copies the painted rect into the glowmask."""
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                self.glow.putpixel((x0 + x, y0 + y), self.img.getpixel((x0 + x, y0 + y))[:3] + (alpha,))


def skin(x, y, w, h):
    """Lit from above: lighter at the top edge, darker towards the bottom and at the bottom edge."""
    v = 0.66 - 0.3 * (y / max(1, h - 1))
    if y == 0:
        v += 0.1
    if y == h - 1 and h > 1:
        v -= 0.12
    if x in (0, w - 1) and w > 2:
        v -= 0.06
    return v


def folds(x, y):
    """Shallow creases running down the skin, a little off the vertical."""
    return 0.07 * math.sin(1.7 * x + 0.5 * y) - 0.05 * ((x + y // 3) % 4 == 0)


def flat(level):
    return lambda x, y, w, h: level - (0.08 if x in (0, w - 1) or y in (0, h - 1) else 0)


def bone_along(vertical):
    """An ivory segment: bright in the middle of its length, dark at both ends where the joints are."""
    def value(x, y, w, h):
        i, n = (y, h) if vertical else (x, w)
        end = min(i, n - 1 - i)
        return (0.3, 0.72, 0.9)[min(end, 2)]
    return value


def ball(x, y, w, h):
    """A rounded body lit from the upper left."""
    dx, dy = (x + 0.5) / w - 0.4, (y + 0.5) / h - 0.36
    return 0.92 - 1.5 * math.hypot(dx, dy)


def geo(name, bones, uv, sheet, bounds=(2, 2, (0, 0.5, 0))):
    out = []
    for bone, parent, pivot, cubes in bones:
        entry = {'name': bone}
        if parent:
            entry['parent'] = parent
        entry['pivot'] = pivot
        if cubes:
            entry['cubes'] = []
            for island, origin, size, turn in cubes:
                cube = {'origin': origin, 'size': size, 'uv': list(uv[island])}
                if turn:
                    cube['pivot'], cube['rotation'] = turn[1], turn[0]
                entry['cubes'].append(cube)
        out.append(entry)
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.' + name, 'texture_width': sheet, 'texture_height': sheet,
                        'visible_bounds_width': bounds[0], 'visible_bounds_height': bounds[1],
                        'visible_bounds_offset': list(bounds[2])},
        'bones': out}]}


def r(value):
    value = round(value, 3)
    return 0 if value == 0 else value


def sampled(length, step, fn):
    """Keyframes every `step` seconds of fn(t) -> [x, y, z]; the last one repeats the first so the loop closes."""
    count = round(length / step)
    return {str(round(i * step, 2)) if i else '0.0': [r(v) for v in fn(0 if i == count else i * step)] for i in range(count + 1)}


def wave(t, length, lag=0.0):
    return math.sin(2 * math.pi * (t - lag) / length)


SQUEEZE = [(0.0, 0.0), (0.2, 1.0), (0.35, 0.92), (0.6, -0.14), (0.8, 0.05), (1.0, 0.0)]


def squeeze(t, length=1.0, lag=0.0):
    """A quick contraction over one loop: squeeze, short hold, release past rest, settle."""
    t = ((t - lag) % length) / length
    for (t0, v0), (t1, v1) in zip(SQUEEZE, SQUEEZE[1:]):
        if t <= t1:
            k = (1 - math.cos(math.pi * (t - t0) / (t1 - t0))) / 2
            return v0 + (v1 - v0) * k
    return 0.0


def animation_file(animations):
    """{name: (length, loop, {bone: {channel: keys}})} -> animation JSON."""
    return {'format_version': '1.8.0', 'animations': {
        name: {'loop': loop, 'animation_length': length, 'bones': bones}
        for name, (length, loop, bones) in animations.items()}}


def uid(model, name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'infestusfrontier:' + model + '/' + name))


def data_uri(img):
    out = io.BytesIO()
    img.save(out, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(out.getvalue()).decode('ascii')


def flip(v):
    """Geo pivot -> Blockbench origin: X is mirrored."""
    return [-v[0], v[1], v[2]]


def flip_rotation(v):
    return [-v[0], -v[1], v[2]]


def bbmodel(model, bones, uv, sheet, img, glow, anims):
    elements, groups = [], {}
    for name, parent, pivot, cubes in bones:
        children = []
        for i, (island, origin, size, turn) in enumerate(cubes):
            (w, h, d), (u, v) = size, uv[island]
            element = {
                'name': island if len(cubes) == 1 else '%s_%d' % (island, i + 1), 'type': 'cube',
                'uuid': uid(model, 'cube/%s/%d' % (name, i)),
                'box_uv': True, 'rescale': False, 'locked': False, 'render_order': 'default', 'allow_mirror_modeling': True,
                'from': [-(origin[0] + w), origin[1], origin[2]], 'to': [-origin[0], origin[1] + h, origin[2] + d],
                'autouv': 0, 'color': 2, 'inflate': 0, 'mirror_uv': False, 'uv_offset': [u, v],
                'origin': flip(turn[1]) if turn else flip(pivot),
                'rotation': flip_rotation(turn[0]) if turn else [0, 0, 0],
                'faces': {
                    'east': {'uv': [u, v + d, u + d, v + d + h], 'texture': 0},
                    'north': {'uv': [u + d, v + d, u + d + w, v + d + h], 'texture': 0},
                    'west': {'uv': [u + d + w, v + d, u + 2 * d + w, v + d + h], 'texture': 0},
                    'south': {'uv': [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], 'texture': 0},
                    'up': {'uv': [u + d + w, v + d, u + d, v], 'texture': 0},
                    'down': {'uv': [u + d + 2 * w, v, u + d + w, v + d], 'texture': 0},
                },
            }
            elements.append(element)
            children.append(element['uuid'])
        groups[name] = {
            'name': name, 'uuid': uid(model, 'bone/' + name), 'origin': flip(pivot), 'rotation': [0, 0, 0],
            'color': 0, 'export': True, 'mirror_uv': False, 'isOpen': True, 'locked': False, 'visibility': True, 'autouv': 0,
            'children': children,
        }
        if parent:
            groups[parent]['children'].append(groups[name])

    def texture(name, image):
        return {
            'path': '', 'name': name, 'folder': 'block_entity', 'namespace': 'infestusfrontier', 'id': name[:-4],
            'width': sheet, 'height': sheet, 'uv_width': sheet, 'uv_height': sheet, 'particle': False, 'layers_enabled': False,
            'render_mode': 'default', 'render_sides': 'auto', 'frame_time': 1, 'frame_order_type': 'loop', 'frame_order': '',
            'frame_interpolate': False, 'visible': True, 'internal': True, 'saved': True, 'uuid': uid(model, 'texture/' + name),
            'relative_path': '../src/main/resources/assets/infestusfrontier/textures/block_entity/' + name, 'source': data_uri(image),
        }

    bb_animations = []
    for name, animation in anims['animations'].items():
        animators = {}
        for bone, channels in animation['bones'].items():
            keyframes = []
            for channel, keys in channels.items():
                for time, value in keys.items():
                    keyframes.append({
                        'channel': channel, 'data_points': [{'x': str(value[0]), 'y': str(value[1]), 'z': str(value[2])}],
                        'uuid': uid(model, 'key/%s/%s/%s/%s' % (name, bone, channel, time)), 'time': float(time), 'color': -1,
                        'interpolation': 'linear',
                    })
            animators[uid(model, 'bone/' + bone)] = {'name': bone, 'type': 'bone', 'keyframes': keyframes}
        bb_animations.append({
            'uuid': uid(model, 'animation/' + name), 'name': name, 'loop': 'loop' if animation['loop'] else 'once',
            'override': False, 'length': animation['animation_length'], 'snapping': 20, 'selected': name == 'idle',
            'anim_time_update': '', 'blend_weight': '', 'start_delay': '', 'loop_delay': '', 'animators': animators,
        })

    return {
        'meta': {'format_version': '4.10', 'model_format': 'animated_entity_model', 'box_uv': True},
        'name': model, 'model_identifier': '', 'geckolib_format_version': 2,
        'visible_box': [2, 2, 0.5], 'variable_placeholders': '', 'variable_placeholder_buttons': [],
        'timeline_setups': [], 'unhandled_root_fields': {},
        'resolution': {'width': sheet, 'height': sheet},
        'elements': elements, 'outliner': [groups[bones[0][0]]],
        'textures': [texture(model + '.png', img), texture(model + '_glowmask.png', glow)],
        'animations': bb_animations,
    }


def compact(value):
    """Indented JSON with each vector on one line."""
    text = json.dumps(value, indent=2)
    return re.sub(r'\[\s+([^\[\]{}]*?)\s+\]', lambda m: '[' + re.sub(r'\s+', ' ', m.group(1)) + ']', text) + '\n'


def write(model, bones, sheet, painter, anims, particle_rect, bounds=(2, 2, (0, 0.5, 0)), max_cubes=60, max_bones=16):
    """Writes every shipped file of one model and its Blockbench source."""
    uv = pack(bones, sheet)
    out = ASSETS / 'textures/block_entity'
    out.mkdir(parents=True, exist_ok=True)
    (ASSETS / 'geo/block').mkdir(parents=True, exist_ok=True)
    (ASSETS / 'animations/block').mkdir(parents=True, exist_ok=True)
    painter.img.save(out / (model + '.png'), optimize=True)
    painter.glow.save(out / (model + '_glowmask.png'), optimize=True)
    # the break particle: one 16x16 patch of the skin
    px, py = particle_rect
    painter.img.crop((px, py, px + 16, py + 16)).save(ASSETS / 'textures/block' / (model + '.png'), optimize=True)
    animation_json = animation_file(anims)
    (ASSETS / 'geo/block' / (model + '.geo.json')).write_text(compact(geo(model, bones, uv, sheet, bounds)), encoding='utf-8')
    (ASSETS / 'animations/block' / (model + '.animation.json')).write_text(compact(animation_json), encoding='utf-8')
    Path('art/' + model + '.bbmodel').write_text(
        json.dumps(bbmodel(model, bones, uv, sheet, painter.img, painter.glow, animation_json), separators=(',', ':')) + '\n',
        encoding='utf-8')
    cubes = sum(len(bone[3]) for bone in bones)
    assert cubes <= max_cubes and len(bones) <= max_bones, (cubes, len(bones))
    return uv
