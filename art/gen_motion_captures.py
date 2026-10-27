"""Visual fixtures for every organ clip, furnace stage, bladder fill and junction port.

The vanilla tick freeze command holds the displayed states; GeckoLib's render clock keeps running.
These fixtures judge rendering only. Gameplay captures and GameTests use live organs separately.
"""
import json
from pathlib import Path

OUT = Path('src/testMod/resources/captures')
MODELS = ('culture_bowl', 'digestive_sac', 'membrane_rack', 'bone_loom', 'awakening_cradle',
          'activation_cyst', 'planting_proboscis', 'harvest_corolla', 'compost_gland', 'seed_pouch', 'item_capsule',
          'spine_sentry')


# Views an organ's identity pass added to its scene, kept here so regenerating the scenes never loses them: a close
# front view of both blocks and a corner view, the identity cues at full size.
EXTRA = {
    'activation_cyst': [
        {'name': 'close', 'x': 0.5, 'y': -59.3, 'z': 1.6, 'yaw': 0, 'pitch': 22},
        {'name': 'corner', 'x': -1.0, 'y': -59.2, 'z': 1.5, 'yaw': -35, 'pitch': 25},
    ],
    'planting_proboscis': [
        # the stroke close up, a little over one working cycle, so the snout is seen unfolding and stabbing the bed
        {'name': 'close', 'x': 0.5, 'y': -59.3, 'z': 1.4, 'yaw': 0, 'pitch': 28, 'frames': 12, 'frameTicks': 4},
        {'name': 'corner', 'x': -1.2, 'y': -59.2, 'z': 1.4, 'yaw': -38, 'pitch': 25},
        # low from the side over one working cycle: the rostrum's stab in profile, the sac on the back, the legs
        {'name': 'low', 'x': 3.2, 'y': -59.8, 'z': 2.2, 'yaw': 65, 'pitch': 8, 'frames': 12, 'frameTicks': 4},
        # standing on the bed cell in front (a player set inside a block crawls, which drops the eye to the bed), looking
        # down, the view's far edge 39 degrees south of straight down: the organ's block column lies wholly outside the
        # view (its nearest corner is 43 degrees off) while the rostrum's tip, at z 2.55 and lower, lies inside, so the
        # tip draws only because the renderer's extent, not the block, is what is culled
        {'name': 'bed', 'x': 1.5, 'y': -60.0, 'z': 1.5, 'yaw': 0, 'pitch': 86},
    ],
}


def scene(name, left, right, frames=48):
    # Move while ticks run so the server tracks the fixture's chunks before freezing.
    spec = {
        'commands': [
            'tp @s 0.5 -59.5 0 0 22',
            'fill -8 -61 -4 8 -54 10 air', 'fill -8 -62 -4 8 -62 10 grass_block',
            'kill @e[type=item]', 'tick freeze',
            'fill -3 -61 1 3 -61 5 infestusfrontier:living_substrate[stage=mature]',
            f'setblock -1 -60 3 infestusfrontier:{left}', f'setblock 1 -60 3 infestusfrontier:{right}',
        ],
        'views': [
            {'name': 'motion', 'x': 0.5, 'y': -59.5, 'z': 0, 'yaw': 0, 'pitch': 22, 'frames': frames, 'frameTicks': 4},
            {'name': 'back', 'x': 0.5, 'y': -59.5, 'z': 7, 'yaw': 180, 'pitch': 22},
            {'name': 'side', 'x': 4.5, 'y': -59.5, 'z': 3.5, 'yaw': 90, 'pitch': 22},
        ] + EXTRA.get(name, []),
    }
    (OUT / f'motion_{name}.json').write_text(json.dumps(spec, indent=2) + '\n')


def main():
    for model in MODELS:
        suffix = '' if model in ('seed_pouch', 'item_capsule') else '[working=true]'
        scene(model, model, model + suffix)
    for stage in range(3):
        scene(f'furnace_{stage}', f'bio_furnace[stage={stage}]', f'bio_furnace[stage={stage},working=true]')
    for level in range(5):
        scene(f'bladder_{level}', f'biomass_bladder[fill={level}]', f'biomass_bladder[fill={level}]', 44)
    for mode in ('closed', 'intake', 'outlet'):
        state = ','.join(f'{face}={mode}' for face in ('up', 'down', 'north', 'south', 'east', 'west'))
        scene(f'junction_{mode}', f'vascular_junction[{state}]', f'vascular_junction[{state}]', 40)
    scene('reservoir', 'reservoir_cell[fill=2]', 'reservoir_cell[fill=4]', 70)
    door()
    armor()
    static_details()


def door():
    for name in ('sphincter_door', 'clenched_door', 'calcified_door'):
        commands = ['fill -8 -61 -4 8 -54 10 air', 'fill -8 -62 -4 8 -62 10 grass_block',
                    'fill -3 -61 1 3 -61 5 infestusfrontier:living_substrate[stage=mature]',
                    f'setblock 0 -60 3 infestusfrontier:{name}[half=lower,facing=north]',
                    f'setblock 0 -59 3 infestusfrontier:{name}[half=upper,facing=north]',
                    'kill @e[type=item]']
        views = []
        for view_name, z, yaw, frames, trigger in (
                ('motion_closed', 0, 0, 32, None),
                ('opening', 0, 0, 18, 'redstone_block'),
                ('closing', 0, 0, 18, 'infestusfrontier:living_substrate[stage=mature]'),
                ('back_opening', 7, 180, 18, 'redstone_block'),
                ('back_closing', 7, 180, 18, 'infestusfrontier:living_substrate[stage=mature]')):
            view = dict(name=view_name, x=0.5, y=-59.5, z=z, yaw=yaw, pitch=0, frames=frames,
                        frameTicks=1 if trigger else 4)
            if trigger:
                view['trigger'] = 'setblock 0 -61 3 ' + trigger
            views.append(view)
        (OUT / f'motion_{name}.json').write_text(json.dumps(dict(commands=commands, views=views), indent=2) + '\n')


def armor():
    """The armor audit (S04 F15): dormant and awakened stands, a dyed set and a trimmed glinting set, an adult and a
    baby zombie wearing them, the player in a worn set with every piece in the hotbar; close, breath and inventory
    views. Kept here in full so regenerating the scenes never loses what the audit shows."""
    commands = [
        'kill @e[type=armor_stand,tag=visual_audit]',
        'fill -8 -61 -4 8 -54 10 air',
        'fill -8 -62 -4 8 -61 10 grass_block',
        'kill @e[type=item]',
        'summon armor_stand -1 -60 3 {Tags:["visual_audit"],NoGravity:1b,ShowArms:1b,NoBasePlate:1b,ArmorItems:[{id:"infestusfrontier:dormant_treads",count:1},{id:"infestusfrontier:dormant_legwraps",count:1},{id:"infestusfrontier:dormant_thorax",count:1},{id:"infestusfrontier:dormant_hood",count:1}],Rotation:[180f,0f],Pose:{LeftArm:[-35f,0f,-15f],RightArm:[-70f,0f,15f],LeftLeg:[-20f,0f,0f],RightLeg:[20f,0f,0f]}}',
        'summon armor_stand 1 -60 3 {Tags:["visual_audit"],NoGravity:1b,ShowArms:1b,NoBasePlate:1b,ArmorItems:[{id:"infestusfrontier:awakened_treads",count:1},{id:"infestusfrontier:awakened_legwraps",count:1},{id:"infestusfrontier:awakened_thorax",count:1},{id:"infestusfrontier:awakened_hood",count:1}],Rotation:[180f,0f],Pose:{LeftArm:[-35f,0f,-15f],RightArm:[-70f,0f,15f],LeftLeg:[-20f,0f,0f],RightLeg:[20f,0f,0f]}}',
        'summon armor_stand -3 -60 3 {Tags:["visual_audit"],NoGravity:1b,ShowArms:1b,NoBasePlate:1b,ArmorItems:[{id:"infestusfrontier:dormant_treads",count:1,components:{"minecraft:dyed_color":{rgb:4890623}}},{id:"infestusfrontier:dormant_legwraps",count:1,components:{"minecraft:dyed_color":{rgb:4890623}}},{id:"infestusfrontier:dormant_thorax",count:1,components:{"minecraft:dyed_color":{rgb:4890623}}},{id:"infestusfrontier:dormant_hood",count:1,components:{"minecraft:dyed_color":{rgb:4890623}}}],Rotation:[180f,0f],Pose:{LeftArm:[-35f,0f,-15f],RightArm:[-70f,0f,15f],LeftLeg:[-20f,0f,0f],RightLeg:[20f,0f,0f]}}',
        'summon armor_stand 3 -60 3 {Tags:["visual_audit"],NoGravity:1b,ShowArms:1b,NoBasePlate:1b,ArmorItems:[{id:"infestusfrontier:awakened_treads",count:1,components:{"minecraft:trim":{pattern:"minecraft:coast",material:"minecraft:gold"},"minecraft:enchantment_glint_override":true}},{id:"infestusfrontier:awakened_legwraps",count:1,components:{"minecraft:trim":{pattern:"minecraft:coast",material:"minecraft:gold"},"minecraft:enchantment_glint_override":true}},{id:"infestusfrontier:awakened_thorax",count:1,components:{"minecraft:trim":{pattern:"minecraft:coast",material:"minecraft:gold"},"minecraft:enchantment_glint_override":true}},{id:"infestusfrontier:awakened_hood",count:1,components:{"minecraft:trim":{pattern:"minecraft:coast",material:"minecraft:gold"},"minecraft:enchantment_glint_override":true}}],Rotation:[180f,0f],Pose:{LeftArm:[-35f,0f,-15f],RightArm:[-70f,0f,15f],LeftLeg:[-20f,0f,0f],RightLeg:[20f,0f,0f]}}',
        'difficulty easy',
        'kill @e[type=zombie,tag=visual_audit]',
        'summon zombie 5 -60 3 {Tags:["visual_audit"],Glowing:1b,NoAI:1b,PersistenceRequired:1b,Silent:1b,Rotation:[180f,0f],ArmorItems:[{id:"infestusfrontier:awakened_treads",count:1},{id:"infestusfrontier:awakened_legwraps",count:1},{id:"infestusfrontier:awakened_thorax",count:1},{id:"infestusfrontier:awakened_hood",count:1}],ArmorDropChances:[0f,0f,0f,0f]}',
        'summon zombie -5 -60 3 {Tags:["visual_audit"],NoAI:1b,PersistenceRequired:1b,Silent:1b,IsBaby:1b,Rotation:[180f,0f],ArmorItems:[{id:"infestusfrontier:dormant_treads",count:1},{id:"infestusfrontier:dormant_legwraps",count:1},{id:"infestusfrontier:dormant_thorax",count:1},{id:"infestusfrontier:dormant_hood",count:1}],ArmorDropChances:[0f,0f,0f,0f]}',
        'gamemode survival @s',
        'item replace entity @s armor.head with infestusfrontier:awakened_hood',
        'item replace entity @s armor.chest with infestusfrontier:awakened_thorax',
        'item replace entity @s armor.legs with infestusfrontier:awakened_legwraps',
        'item replace entity @s armor.feet with infestusfrontier:awakened_treads',
        'item replace entity @s hotbar.0 with infestusfrontier:dormant_hood',
        'item replace entity @s hotbar.1 with infestusfrontier:dormant_thorax[minecraft:dyed_color={rgb:4890623}]',
        'item replace entity @s hotbar.2 with infestusfrontier:dormant_legwraps',
        'item replace entity @s hotbar.3 with infestusfrontier:dormant_treads',
        'item replace entity @s hotbar.4 with infestusfrontier:awakened_hood',
        'item replace entity @s hotbar.5 with infestusfrontier:awakened_thorax',
        'item replace entity @s hotbar.6 with infestusfrontier:awakened_legwraps',
        'item replace entity @s hotbar.7 with infestusfrontier:awakened_treads',
    ]
    views = [
        {'name': 'front', 'x': 0.5, 'y': -59.2, 'z': -4.5, 'yaw': 0, 'pitch': 10},
        {'name': 'back', 'x': 0.5, 'y': -59.4, 'z': 7.5, 'yaw': 180, 'pitch': 16},
        {'name': 'side', 'x': -1.2, 'y': -59.1, 'z': 0.9, 'yaw': -40, 'pitch': 12},
        {'name': 'close_awakened', 'x': 1.5, 'y': -59.0, 'z': 0.6, 'yaw': 0, 'pitch': 12},
        {'name': 'close_dormant', 'x': -0.5, 'y': -59.0, 'z': 0.6, 'yaw': 0, 'pitch': 12},
        {'name': 'zombie_adult', 'x': 5.0, 'y': -59.0, 'z': 0.4, 'yaw': 0, 'pitch': 10},
        {'name': 'zombie_baby', 'x': -5.0, 'y': -59.5, 'z': 0.6, 'yaw': 0, 'pitch': 10},
        {'name': 'breath', 'x': 1.5, 'y': -59.0, 'z': 0.9, 'yaw': 0, 'pitch': 10, 'frames': 20, 'frameTicks': 4},
        {'name': 'inventory', 'x': 0.5, 'y': -59.4, 'z': -4.5, 'yaw': 0, 'pitch': 10, 'gui': True, 'inventory': True},
    ]
    (OUT / 'visual_armor.json').write_text(json.dumps({'commands': commands, 'views': views}, indent=2) + '\n')


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
