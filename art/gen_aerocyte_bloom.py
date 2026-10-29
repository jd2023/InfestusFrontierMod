"""Aerocyte Bloom: a rooted spore bladder with four opening bone petals and an upward throat.

    python3 art/gen_aerocyte_bloom.py

V3's bloom supplies the anatomy and material palette. This smaller model keeps its root in one block.
The bladder breathes slowly; a working pulse contracts it and parts the petals around the nozzle.
"""
import json
from pathlib import Path

from organ_model import Painter, ball, faces, islands, pack, sampled, skin, squeeze, wave, write

SHEET = 128
ROOT = [(40, 24, 34), (66, 32, 44), (92, 44, 56), (126, 65, 72)]
FLESH = [(74, 31, 48), (112, 45, 65), (148, 65, 79), (181, 92, 99), (209, 126, 126)]
BONE = [(108, 86, 69), (153, 128, 100), (195, 173, 137), (228, 215, 184), (246, 238, 211)]
SPORE = [(31, 69, 65), (53, 111, 95), (86, 148, 119), (137, 189, 151), (194, 225, 183)]
THROAT = [(15, 37, 35), (24, 55, 49), (41, 81, 66)]

BONES = [
    ('root', None, [0, 0, 0], []),
    ('foot', 'root', [0, 0, 0], [
        ('foot', [-5.5, 0, -5.5], [11, 1, 11], None),
        ('rootlet', [-7, 0.2, -1.5], [14, 2, 3], None),
        ('rootlet_z', [-1.5, 0.35, -7], [3, 2, 14], None),
    ]),
    ('bulb', 'root', [0, 1, 0], [
        ('bulb', [-3.5, 0.75, -3.5], [7, 7, 7], None),
        ('haunch', [-4.5, 1.5, -4.5], [9, 5, 9], ([0, 45, 0], [0, 4, 0])),
        ('shoulder', [-3, 7.3, -3], [6, 3, 6], None),
    ]),
    ('stem', 'root', [0, 8, 0], [
        ('stem', [-1.5, 9.8, -1.5], [3, 7, 3], None),
        ('sleeve', [-2, 11.1, -2], [4, 2, 4], None),
    ]),
    ('crown', 'root', [0, 16, 0], [
        ('cup', [-3.5, 15.5, -3.5], [7, 3, 7], None),
    ]),
    ('bladder', 'crown', [0, 16, 0], [
        ('sac', [-3, 17.2, -3], [6, 6, 6], None),
        ('sac_lobe', [-3.5, 18.1, -3.5], [7, 4, 7], ([0, 45, 0], [0, 20, 0])),
        ('nozzle', [-1.5, 22.9, -1.5], [3, 4, 3], None),
        ('throat', [-1, 26.2, -1], [2, 1, 2], None),
    ]),
]
# Each petal starts inside the cup and opens outward. Their tips stay inside the block.
for side, axis in ((-1, 'z'), (1, 'z'), (-1, 'x'), (1, 'x')):
    name = f'petal_{axis}_{"n" if side < 0 else "p"}'
    origin = [-2, 17, -4 if side < 0 else 2] if axis == 'z' else [-4 if side < 0 else 2, 17, -2]
    size = [4, 8, 2] if axis == 'z' else [2, 8, 4]
    pivot = [0, 17.5, side * 3] if axis == 'z' else [side * 3, 17.5, 0]
    BONES.append((name, 'crown', pivot, [(f'petal_{axis}', origin, size, None)]))


def textures(uv):
    p = Painter(SHEET, 743)
    rects = {name: faces(size, uv[name]) for name, size in islands(BONES).items()}
    for name, sides in rects.items():
        for face, rect in sides.items():
            if name in ('foot', 'rootlet', 'rootlet_z'):
                p.ramp(rect, ROOT, skin, grain=0.035, mottle=0.22)
            elif name in ('bulb', 'haunch', 'shoulder', 'stem'):
                p.ramp(rect, FLESH, lambda x,y,w,h: 0.35*skin(x,y,w,h)+0.55*ball(x,y,w,h), grain=0.03, mottle=0.25)
                if face not in ('up', 'down'): p.vein(rect, (100, 29, 63), 4)
            elif name in ('sac', 'sac_lobe', 'nozzle'):
                p.ramp(rect, SPORE, ball, grain=0.025, mottle=0.2)
                if face not in ('up', 'down'): p.vein(rect, (54, 105, 83), 3)
            elif name == 'throat':
                p.ramp(rect, THROAT, lambda x,y,w,h: 0.25, grain=0, mottle=0)
            else:
                p.ramp(rect, BONE, lambda x,y,w,h: (0.8 if face == 'up' else 0.72 - 0.25*y/max(1,h-1)) - 0.12*(x%3==0), grain=0.025, mottle=0.1)
    return p, rects['bulb']['north'][:2]


def animations():
    clips = {}
    for name, length in (('idle', 4.0), ('working', 2.0)):
        active = name == 'working'
        def pulse(t): return squeeze(t, length) if active else wave(t, length)
        bones = {
            'bulb': {'scale': sampled(length, 0.1, lambda t: [1+0.025*pulse(t), 1, 1+0.025*pulse(t)])},
            'bladder': {'scale': sampled(length, 0.1, lambda t: [1-(0.12 if active else 0.025)*pulse(t), 1-(0.08 if active else 0.015)*pulse(t), 1-(0.12 if active else 0.025)*pulse(t)])},
        }
        for side, axis in ((-1,'z'), (1,'z'), (-1,'x'), (1,'x')):
            key = f'petal_{axis}_{"n" if side<0 else "p"}'
            def angle(t, side=side, axis=axis):
                a = (15 + (8 if active else 2)*pulse(t))*side
                return [-a,0,0] if axis=='z' else [0,0,a]
            bones[key] = {'rotation': sampled(length, 0.1, angle)}
        clips[name] = (length, True, bones)
    return clips


def item_model():
    def display(rotation, translation, scale):
        return {'rotation': rotation, 'translation': translation, 'scale': [scale] * 3}
    model = {
        'parent': 'builtin/entity',
        'gui_light': 'side',
        'display': {
            'gui': display([30, 225, 0], [0, -3.7, 0], 0.48),
            'ground': display([0, 0, 0], [0, 3, 0], 0.25),
            'fixed': display([0, 0, 0], [0, 0, 0], 0.5),
            'thirdperson_righthand': display([75, 45, 0], [0, 2.5, 0], 0.375),
            'firstperson_righthand': display([0, 135, 0], [0, 0, 0], 0.4),
            'firstperson_lefthand': display([0, 225, 0], [0, 0, 0], 0.4),
        },
    }
    path = Path('src/main/resources/assets/infestusfrontier/models/item/aerocyte_bloom.json')
    path.write_text(json.dumps(model, indent=2) + '\n')


def main():
    p, particle = textures(pack(BONES, SHEET))
    write('aerocyte_bloom', BONES, SHEET, p, animations(), particle, bounds=(2, 2, (0, 0.85, 0)))
    item_model()
    print('aerocyte_bloom:', len(BONES), 'bones')


if __name__ == '__main__': main()
