"""What a Structure Grower grows on a mine's walls besides its floor: the Lumen Node, a small luminous organ, a pale
green bulb on a dark stalk; the Bone Rib, a flat ivory band across a passage's walls and ceiling, thin or (on the Wider
Section) thick, its ends knuckled like a spine; and the Cut Scar, a few claw-scrapes in the rock with shreds of pink
tissue caught in them. Ribs and scars are flat decals on the face they grow on, as glow lichen is; the band of a rib
on the ceiling runs across the passage, so its blockstate turns it by the passage's axis.

    python3 art/gen_mine_growths.py
"""
import json
import random
from PIL import Image
from organ_model import ASSETS, compact

STALK = [(46, 34, 30), (66, 50, 42), (88, 68, 54)]
BULB = [(120, 190, 96), (168, 228, 132), (210, 248, 176), (240, 255, 220)]
BONE = [(150, 138, 112), (186, 174, 146), (214, 204, 178), (236, 230, 210)]
SCRAPE = [(30, 26, 24), (52, 46, 42)]
TISSUE = [(150, 64, 80), (190, 96, 110), (222, 140, 150)]


def save(img, name):
    (ASSETS / 'textures/block').mkdir(parents=True, exist_ok=True)
    img.save(ASSETS / 'textures/block' / (name + '.png'), optimize=True)


def lumen_texture():
    """Top rows: the stalk; the rest: the bulb, brightest at its heart."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    rng = random.Random(611)
    for y in range(16):
        for x in range(16):
            if y < 4:
                img.putpixel((x, y), STALK[(x + y) % 3] + (255,))
            else:
                d = abs(x - 7.5) / 8 + abs(y - 10) / 8
                shade = 3 if d < 0.35 else 2 if d < 0.65 else 1 if d < 0.9 else 0
                if rng.random() < 0.12:
                    shade = max(0, shade - 1)
                img.putpixel((x, y), BULB[shade] + (255,))
    save(img, 'lumen_node')


def lumen_model():
    """Attached to the floor below it (the blockstate turns it onto a ceiling or a wall): a stalk and a bulb, the
    bulb's faces lit to full so it reads as the light it gives."""
    lit = {'neoforge_data': {'block_light': 15, 'sky_light': 15}}

    def element(lo, hi, v0, v1, glow):
        faces = {}
        for face in ('north', 'south', 'west', 'east'):
            faces[face] = {'uv': [lo[0], v0, hi[0], v1], 'texture': '#node', **(lit if glow else {})}
        faces['up'] = {'uv': [lo[0], lo[2], hi[0], hi[2]], 'texture': '#node', **(lit if glow else {})}
        faces['down'] = {'uv': [lo[0], lo[2], hi[0], hi[2]], 'texture': '#node'}
        return {'from': lo, 'to': hi, 'faces': faces}

    model = {
        'ambientocclusion': False,
        'textures': {'node': 'infestusfrontier:block/lumen_node', 'particle': 'infestusfrontier:block/lumen_node'},
        'elements': [element([7, 0, 7], [9, 2, 9], 0, 4, False), element([5.5, 2, 5.5], [10.5, 7, 10.5], 6, 14, True)],
    }
    (ASSETS / 'models/block/lumen_node.json').write_text(compact(model), encoding='utf-8')
    # vanilla's lever turns: the model stands on the floor; a wall turns it x 90, a ceiling x 180
    variants = {}
    for facing, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        variants[f'face=floor,facing={facing}'] = {'model': 'infestusfrontier:block/lumen_node', **({'y': y} if y else {})}
        variants[f'face=wall,facing={facing}'] = {'model': 'infestusfrontier:block/lumen_node', 'x': 90, **({'y': y} if y else {})}
        variants[f'face=ceiling,facing={facing}'] = {'model': 'infestusfrontier:block/lumen_node', 'x': 180,
                                                     **({'y': (y + 180) % 360} if (y + 180) % 360 else {})}
    (ASSETS / 'blockstates/lumen_node.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')


def rib_texture(name, half):
    """A vertical ivory band down the middle of the tile, `half` texels either side, darker at its edges, with a
    knuckle every few texels; the rest is clear."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(16):
        knuckle = y % 5 == 0
        reach = half + (1 if knuckle else 0)
        for x in range(16):
            d = abs(x + 0.5 - 8)
            if d > reach:
                continue
            edge = d > reach - 1
            shade = 0 if edge else 3 if d < reach / 3 else 2 if not knuckle else 1
            img.putpixel((x, y), BONE[shade] + (255,))
    save(img, name)


def scar_texture():
    """Three diagonal scrapes in dark, with shreds of tissue caught along them; the rest is clear."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    rng = random.Random(1709)
    for start in (2, 6, 10):
        x = start
        for y in range(2, 14):
            img.putpixel((x % 16, y), SCRAPE[y % 2] + (235,))
            if rng.random() < 0.22:
                img.putpixel(((x + 1) % 16, y), TISSUE[rng.randrange(3)] + (255,))
            if y % 3 == 0:
                x += 1
    save(img, 'cut_scar')


def decal(name, texture, rotation=0):
    """One flat face against the north side of its block, as glow lichen's model, seen from inside the block."""
    face = {'uv': [0, 0, 16, 16], 'texture': '#decal', **({'rotation': rotation} if rotation else {})}
    model = {
        'ambientocclusion': False,
        'render_type': 'minecraft:cutout',
        'textures': {'decal': 'infestusfrontier:block/' + texture, 'particle': 'infestusfrontier:block/' + texture},
        'elements': [{'from': [0, 0, 0.1], 'to': [16, 16, 0.1], 'shade': False,
                      'faces': {'north': face, 'south': face}}],
    }
    (ASSETS / 'models/block' / (name + '.json')).write_text(compact(model), encoding='utf-8')


# glow lichen's turns of the north decal onto each face
TURNS = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}}


def ribs():
    rib_texture('bone_rib', 1.5)
    rib_texture('bone_rib_thick', 3)
    parts = []
    for thick in (False, True):
        suffix = '_thick' if thick else ''
        decal('bone_rib' + suffix, 'bone_rib' + suffix)
        # on the ceiling the band must run across the passage: turned a quarter when the passage runs along z
        decal('bone_rib_across' + suffix, 'bone_rib' + suffix, rotation=90)
        for face, turn in TURNS.items():
            if face == 'up':
                for axis, model in (('x', 'bone_rib'), ('z', 'bone_rib_across')):
                    parts.append({'when': {'up': 'true', 'axis': axis, 'thick': str(thick).lower()},
                                  'apply': {'model': 'infestusfrontier:block/' + model + suffix, **turn}})
            else:
                parts.append({'when': {face: 'true', 'thick': str(thick).lower()},
                              'apply': {'model': 'infestusfrontier:block/bone_rib' + suffix, **turn}})
    (ASSETS / 'blockstates/bone_rib.json').write_text(json.dumps({'multipart': parts}, indent=2) + '\n')


def scars():
    scar_texture()
    decal('cut_scar', 'cut_scar')
    parts = [{'when': {face: 'true'}, 'apply': {'model': 'infestusfrontier:block/cut_scar', **turn}}
             for face, turn in TURNS.items() if face != 'up']
    (ASSETS / 'blockstates/cut_scar.json').write_text(json.dumps({'multipart': parts}, indent=2) + '\n')


def main():
    lumen_texture()
    lumen_model()
    ribs()
    scars()
    print('lumen node: 1 model; bone rib: 4 models, 2 textures; cut scar: 1 model, 1 texture; 3 blockstates')


if __name__ == '__main__':
    main()
