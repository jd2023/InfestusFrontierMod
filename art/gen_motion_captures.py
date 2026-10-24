"""Visual fixtures for every organ clip, furnace stage, bladder fill and junction port.

The vanilla tick freeze command holds the displayed states; GeckoLib's render clock keeps running.
These fixtures judge rendering only. Gameplay captures and GameTests use live organs separately.
"""
import json
from pathlib import Path

OUT = Path('src/testMod/resources/captures')
MODELS = ('culture_bowl', 'digestive_sac', 'membrane_rack', 'bone_loom', 'awakening_cradle',
          'activation_cyst', 'planting_proboscis', 'harvest_corolla', 'compost_gland', 'seed_pouch')


def scene(name, left, right, frames=48):
    spec = {
        'commands': [
            'fill -8 -61 -4 8 -54 10 air', 'fill -8 -62 -4 8 -62 10 grass_block',
            'kill @e[type=item]', 'tick freeze',
            'fill -3 -61 1 3 -61 5 infestusfrontier:living_substrate[stage=mature]',
            f'setblock -1 -60 3 infestusfrontier:{left}', f'setblock 1 -60 3 infestusfrontier:{right}',
        ],
        'views': [
            {'name': 'motion', 'x': 0.5, 'y': -59.5, 'z': 0, 'yaw': 0, 'pitch': 22, 'frames': frames, 'frameTicks': 4},
            {'name': 'back', 'x': 0.5, 'y': -59.5, 'z': 7, 'yaw': 180, 'pitch': 22},
            {'name': 'side', 'x': 4.5, 'y': -59.5, 'z': 3.5, 'yaw': 90, 'pitch': 22},
        ],
    }
    (OUT / f'motion_{name}.json').write_text(json.dumps(spec, indent=2) + '\n')


def main():
    for model in MODELS:
        suffix = '' if model == 'seed_pouch' else '[working=true]'
        scene(model, model, model + suffix)
    for stage in range(3):
        scene(f'furnace_{stage}', f'bio_furnace[stage={stage}]', f'bio_furnace[stage={stage},working=true]')
    for level in range(5):
        scene(f'bladder_{level}', f'biomass_bladder[fill={level}]', f'biomass_bladder[fill={level}]', 44)
    for mode in ('closed', 'intake', 'outlet'):
        state = ','.join(f'{face}={mode}' for face in ('up', 'down', 'north', 'south', 'east', 'west'))
        scene(f'junction_{mode}', f'vascular_junction[{state}]', f'vascular_junction[{state}]', 40)
    scene('reservoir', 'reservoir_cell[fill=2]', 'reservoir_cell[fill=4]', 70)
    armor()
    static_details()


def armor():
    commands = ['kill @e[type=armor_stand,tag=visual_audit]', 'fill -8 -61 -4 8 -54 10 air',
                'fill -8 -62 -4 8 -61 10 grass_block', 'kill @e[type=item]']
    for x, kind in ((-1, 'dormant'), (1, 'awakened')):
        items = ','.join('{id:"infestusfrontier:' + kind + '_' + piece + '",count:1}'
                         for piece in ('treads', 'legwraps', 'thorax', 'hood'))
        commands.append(f'summon armor_stand {x} -60 3 '
                        '{Tags:["visual_audit"],NoGravity:1b,ShowArms:1b,NoBasePlate:1b,'
                        f'ArmorItems:[{items}],Rotation:[180f,0f],'
                        'Pose:{LeftArm:[-35f,0f,-15f],RightArm:[-70f,0f,15f],LeftLeg:[-20f,0f,0f],RightLeg:[20f,0f,0f]}}')
    spec = {'commands': commands, 'views': [
        {'name': 'front', 'x': 0.5, 'y': -59.4, 'z': -1.5, 'yaw': 0, 'pitch': 16},
        {'name': 'back', 'x': 0.5, 'y': -59.4, 'z': 7.5, 'yaw': 180, 'pitch': 16},
        {'name': 'side', 'x': 4.5, 'y': -59.4, 'z': 3.0, 'yaw': 90, 'pitch': 16},
    ]}
    (OUT / 'visual_armor.json').write_text(json.dumps(spec, indent=2) + '\n')


def static_details():
    commands = ['kill @e[type=armor_stand,tag=visual_audit]', 'fill -8 -61 -4 12 -54 10 air',
                'fill -8 -62 -4 12 -62 10 grass_block',
                'fill -3 -61 1 11 -61 5 infestusfrontier:living_substrate[stage=mature]']
    views = []
    for x, block, y in ((0, 'organ_bud', -60), (3, 'filter_valve', -60),
                        (6, 'fuel_papilla', -61), (9, 'mouth', -61)):
        commands.append(f'setblock {x} {y} 3 infestusfrontier:{block}')
        views.extend([
            {'name': block + '_front', 'x': x + 0.5, 'y': -59.5, 'z': 0.5, 'yaw': 0, 'pitch': 30},
            {'name': block + '_back', 'x': x + 0.5, 'y': -59.5, 'z': 6.5, 'yaw': 180, 'pitch': 30},
        ])
    (OUT / 'visual_static.json').write_text(json.dumps({'commands': commands, 'views': views}, indent=2) + '\n')


if __name__ == '__main__':
    main()
