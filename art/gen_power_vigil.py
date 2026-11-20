"""Original temporary power organs and Vigil assets; no external images.
Run from repository root. Boxes are the editable source and match Java selection shapes.
"""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw
from organ_model import ASSETS, Painter, pack, islands, faces, skin, write_static
from gen_cutting_organs import box, BONE, FOOT, FLESH, rough, soft


def save_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n')


def organ(name, boxes, ramp, directional=False):
    bones = [(name, None, [0, 8, 0], [box('box' + str(i), *bounds) for i, bounds in enumerate(boxes)])]
    painter = Painter(128, 884)
    uv = pack(bones, 128)
    for i, (island, size) in enumerate(islands(bones).items()):
        for face, rect in faces(size, uv[island]).items():
            painter.ramp(rect, BONE if i == 0 else ramp, rough if i == 0 else soft, grain=0.06, mottle=0.22)
    write_static(name, bones, 128, painter)
    if directional:
        # The connection is the base face (down); rotate it towards the core.
        rotations = {'down': {}, 'up': {'x': 180}, 'north': {'x': 90}, 'south': {'x': 270}, 'west': {'x': 90, 'y': 270}, 'east': {'x': 90, 'y': 90}}
        variants = {'facing=' + side: {'model': 'infestusfrontier:block/' + name, **turn} for side, turn in rotations.items()}
    else:
        variants = {'': {'model': 'infestusfrontier:block/' + name}}
    save_json(ASSETS / 'blockstates' / (name + '.json'), {'variants': variants})
    save_json(ASSETS / 'models/item' / (name + '.json'), {'parent': 'infestusfrontier:block/' + name})


def main():
    heart = [(1, 0, 1, 15, 2, 15), (2, 2, 2, 14, 10, 14), (4, 10, 4, 12, 15, 12)]
    heat = [(76, 32, 26), (128, 48, 30), (184, 84, 40), (224, 142, 70), (248, 206, 118)]
    electric = [(34, 48, 50), (48, 82, 82), (64, 122, 116), (102, 168, 146), (170, 220, 182)]
    organ('thermal_heart', heart, heat)
    organ('thermal_receiver', heart, FLESH)
    organ('steam_driver', heart, electric)
    organ('vigil_nest', [(1, 0, 1, 15, 3, 15), (3, 3, 3, 13, 8, 13)], FLESH)
    # Full cube parts, with a bone connection band and a readable working surface.
    for name, ramp in [('heat_collector', heat), ('boiler_chamber', FLESH), ('generating_chamber', electric), ('pressure_outlet', FOOT), ('vigil_feeder', FLESH)]:
        organ(name, [(0, 0, 0, 16, 2, 16), (0, 2, 0, 16, 16, 16)], ramp, name != 'vigil_feeder')
    for name, color in [('heat_capsule', (150, 106, 80)), ('hot_heat_capsule', (242, 146, 58)), ('charge_cell', (66, 112, 110)), ('charged_cell', (144, 222, 166)), ('vigil_egg', (192, 114, 144))]:
        img = Image.new('RGBA', (16, 16)); d = ImageDraw.Draw(img)
        d.ellipse((3, 1, 12, 14), fill=(*FOOT[1], 255))
        d.ellipse((4, 2, 11, 13), fill=(*color, 255))
        d.line((5, 4, 5, 10), fill=(*BONE[-1], 255), width=1)
        d.line((4, 11, 11, 11), fill=(*BONE[1], 255))
        dest = ASSETS / 'textures/item' / (name + '.png'); dest.parent.mkdir(parents=True, exist_ok=True); img.save(dest)
        save_json(ASSETS / 'models/item' / (name + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'infestusfrontier:item/' + name}})
    img = Image.new('RGBA', (64, 64))
    for y in range(64):
        for x in range(64):
            wave = (math.sin(x * 0.45) + math.cos(y * 0.6)) * 12
            color = FLESH[2] if y < 24 else BONE[2] if x < 44 else FOOT[2]
            img.putpixel((x, y), tuple(max(0, min(255, int(c + wave))) for c in color) + (255,))
    d = ImageDraw.Draw(img); d.rectangle((14, 9, 17, 12), fill=(238, 216, 166, 255)); d.rectangle((26, 9, 29, 12), fill=(238, 216, 166, 255))
    dest = ASSETS / 'textures/entity/vigil_keeper.png'; dest.parent.mkdir(parents=True, exist_ok=True); img.save(dest)

if __name__ == '__main__':
    main()
