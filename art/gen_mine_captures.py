"""Capture scenarios for the mining revision: the Tunnel Root crawling straight and round both turns and leaning towards
a steered cut, the Collection Cilia at rest and drawing, and a finished mine's look: a lit, framed staircase of Root
Treads down to a junction, a narrow and a wide gallery off it, cut scars, and a route an accident broke. The fixture pre-carves passage shapes; supplied Structure Growers place every root, tread, rib, scar and light.
Collection Cilia export real bed contents through Mouths and Item Veins to a chest. Coordinates are the capture world's, away from every other
scenario.

    python3 art/gen_mine_captures.py
"""
import json
from pathlib import Path

OUT = Path('src/testMod/resources/captures')
SUBSTRATE = 'infestusfrontier:living_substrate[stage=mature]'


def head(facing, steer='straight', ticks=0, cells=3, segments=8, biomass=0):
    """A head that waits, for want of biomass or segments, until its view's trigger feeds it and makes it due."""
    stock = '{id:"infestusfrontier:root_segment",count:%d}' % segments if segments else '{}'
    return ('infestusfrontier:tunnel_root[facing=%s,steer=%s]{segments:%s,output:[{},{},{},{}],'
            'tank:{stored:%d,capacity:2000},cells:%d,ticks:%d}' % (facing, steer, stock, biomass, cells, ticks))


def sign(x, y, z, rotation, lines):
    text = ','.join("'\"%s\"'" % line for line in lines)
    return 'setblock %d %d %d oak_sign[rotation=%d]{is_waxed:1b,front_text:{messages:[%s]}}' % (x, y, z, rotation, text)


def tunnel_root():
    """Three heads in cutaway tunnels, each due when its view starts, and one at rest steered left."""
    c = ['forceload add 296 296 344 312', 'fill 296 -61 292 344 -50 312 air', 'fill 296 -62 292 344 -62 312 grass_block',
         'kill @e[type=item]']
    # straight: a tunnel north, open on its east side; the head at z 300 cuts z 299
    c += ['fill 300 -61 294 301 -58 304 stone', 'fill 301 -60 300 301 -59 304 air',
          'fill 301 -61 301 301 -61 304 infestusfrontier:root_cell[facing=north]', 'setblock 301 -61 300 ' + SUBSTRATE,
          'setblock 301 -60 300 ' + head('north')]
    # left: the head faces north and turns west into the stone, its tunnel open to the south
    c += ['fill 306 -61 296 310 -58 299 stone', 'fill 308 -61 300 310 -58 300 stone', 'fill 309 -60 300 309 -59 304 air',
          'fill 309 -61 301 309 -61 304 infestusfrontier:root_cell[facing=north]', 'setblock 309 -61 300 ' + SUBSTRATE,
          'fill 306 -61 300 307 -58 300 stone', 'setblock 309 -60 300 ' + head('north', 'left')]
    # right: the mirror image, turning east
    c += ['fill 316 -61 296 320 -58 299 stone', 'fill 318 -61 300 320 -58 300 stone', 'fill 317 -60 300 317 -59 304 air',
          'fill 317 -61 301 317 -61 304 infestusfrontier:root_cell[facing=north]', 'setblock 317 -61 300 ' + SUBSTRATE,
          'fill 316 -61 300 316 -58 300 stone', 'setblock 317 -60 300 ' + head('north', 'right')]
    # at rest, steered left: the snout leans left and the arrow marks the next cut, both through the open front
    c += ['fill 324 -61 296 330 -58 299 stone', 'fill 327 -60 300 327 -59 304 air', 'setblock 327 -61 300 ' + SUBSTRATE,
          'fill 327 -61 301 327 -61 304 infestusfrontier:root_cell[facing=north]', 'setblock 327 -60 300 ' + head('north', 'left', segments=0),
          'setblock 333 -60 300 ' + head('north', 'right', segments=0), 'setblock 333 -61 300 ' + SUBSTRATE,
          'fill 332 -61 296 334 -58 299 stone']
    due = 'data merge block %d -60 300 {ticks:400,tank:{stored:1000,capacity:2000}}'
    views = [
        # close beside each head, frames as fast as the client takes them (about two a second) over the 2 s crawl
        {'name': 'crawl_straight', 'x': 303.2, 'y': -58.3, 'z': 300.0, 'yaw': 90, 'pitch': 38, 'frames': 10,
         'frameTicks': 1, 'trigger': due % 301},
        {'name': 'crawl_left', 'x': 308.9, 'y': -58.5, 'z': 302.4, 'yaw': 180, 'pitch': 35, 'frames': 10,
         'frameTicks': 1, 'trigger': due % 309},
        {'name': 'crawl_right', 'x': 317.6, 'y': -58.5, 'z': 302.4, 'yaw': 180, 'pitch': 35, 'frames': 10,
         'frameTicks': 1, 'trigger': due % 317},
        {'name': 'aim_left', 'x': 327.5, 'y': -58.4, 'z': 302.2, 'yaw': 180, 'pitch': 35, 'frames': 4, 'frameTicks': 10},
        {'name': 'aim_right', 'x': 333.5, 'y': -58.4, 'z': 302.2, 'yaw': 180, 'pitch': 35},
        {'name': 'after_left', 'x': 308.5, 'y': -56.8, 'z': 302.0, 'yaw': 180, 'pitch': 60},
    ]
    return {'commands': c, 'views': views}


def cilia():
    """Cilia on a Mouth facing a Work Bed: actual sweeps into a chest drive the animation."""
    c = ['forceload add 296 316 344 332', 'fill 296 -61 316 344 -50 332 air', 'fill 296 -62 316 344 -62 332 grass_block',
         'kill @e[type=item]', 'tick unfreeze',
         'setblock 300 -61 320 infestusfrontier:mouth[mode=output]', 'setblock 300 -60 320 infestusfrontier:collection_cilia[facing=north]',
         'setblock 300 -61 319 ' + SUBSTRATE, 'setblock 300 -60 319 infestusfrontier:work_bed',
         'setblock 304 -61 320 infestusfrontier:mouth[mode=output]',
         'setblock 304 -60 320 infestusfrontier:collection_cilia[facing=north]{tank:{stored:1000,capacity:1000},sent:0}',
         'setblock 304 -61 319 ' + SUBSTRATE, 'setblock 304 -60 319 infestusfrontier:work_bed',
         'fill 305 -61 320 306 -61 320 infestusfrontier:item_vein',
         'setblock 307 -61 320 infestusfrontier:mouth[mode=intake]', 'setblock 307 -60 320 chest']
    feed = 'data merge block 304 -60 319 {slots:[' + ','.join(['{id:"minecraft:cobblestone",count:64}'] * 9) + '],received:576}'
    views = [
        {'name': 'rest', 'x': 301.3, 'y': -59.3, 'z': 321.6, 'yaw': 150, 'pitch': 25, 'frames': 6, 'frameTicks': 8},
        {'name': 'drawing', 'x': 305.3, 'y': -59.3, 'z': 321.6, 'yaw': 150, 'pitch': 25, 'frames': 10, 'frameTicks': 2, 'trigger': feed},
        {'name': 'drawing_side', 'x': 306.4, 'y': -60.8, 'z': 320.1, 'yaw': 90, 'pitch': 16, 'frames': 10,
         'frameTicks': 2, 'trigger': feed},
    ]
    return {'commands': c, 'views': views}


# ---- pre-carved fixtures; real supplied growers build every mine growth ----

STAIR_HEADROOM, ROUTE_HEADROOM, WIDE_HEADROOM = 3, 2, 3
DIRS = ['north', 'east', 'south', 'west']
STEP = {'north': (0, -1), 'east': (1, 0), 'south': (0, 1), 'west': (-1, 0)}


def turn(facing, quarters):
    return DIRS[(DIRS.index(facing) + quarters) % 4]


class Mine:
    """Carves fixture passages only. The real Structure Grower places all tissue and lights."""

    def __init__(self):
        self.cmds = []
        self.cleared = set()

    def set(self, pos, block):
        self.cmds.append('setblock %d %d %d %s' % (pos[0], pos[1], pos[2], block))

    def staircase(self, marker, facing, depth):
        """A staircase down from the marker; returns the landing's root cell."""
        mx, my, mz = marker
        sx, sz = STEP[facing]
        steps = []
        for n in range(1, depth + 2):
            landing = n == depth + 1
            root = (mx + sx * n, my - (depth if landing else n), mz + sz * n)
            floor = root if landing else (root[0], root[1] + 1, root[2])
            passage = [(floor[0], floor[1] + u, floor[2]) for u in range(1, STAIR_HEADROOM + 1)]
            steps.append((n, landing, root, None if landing else floor, passage))
        for n, landing, root, tread, passage in steps:
            for cell in [root] + ([tread] if tread else []) + passage:
                self.cleared.add(cell)
        for n, landing, root, tread, passage in steps:
            for cell in passage:
                self.set(cell, 'air')
            self.set(root, 'air')
            if tread:
                self.set(tread, 'air')
        return steps[-1][2]

    def route(self, marker, facing, length, wide=False, broken=()):
        """A level route from the marker, straight; `broken` names steps whose root cell an accident took."""
        mx, my, mz = marker
        sx, sz = STEP[facing]
        headroom = WIDE_HEADROOM if wide else ROUTE_HEADROOM
        left, right = STEP[turn(facing, 3)], STEP[turn(facing, 1)]
        steps = []
        for n in range(1, length + 1):
            at = (mx + sx * n, my, mz + sz * n)
            columns = [at] + ([(at[0] + left[0], my, at[2] + left[1]), (at[0] + right[0], my, at[2] + right[1])] if wide else [])
            steps.append((n, columns))
            for col in columns:
                self.cleared.add(col)
                for u in range(1, headroom + 1):
                    self.cleared.add((col[0], col[1] + u, col[2]))
        for n, columns in steps:
            passage = []
            for col in columns:
                for u in range(1, headroom + 1):
                    cell = (col[0], col[1] + u, col[2])
                    passage.append(cell)
                    self.set(cell, 'air')
                if n in broken:
                    self.set(col, 'air')
                    self.set((col[0], col[1] - 1, col[2]), 'air')
                else:
                    self.set(col, 'air')

def approved(facing, shape, length, wide=False):
    """An approved marker with no construction done yet."""
    return ('infestusfrontier:surveyed_tissue[facing=%s]{shape:%d,length:%d,first_way:0,first_at:0,second_way:0,'
            'second_at:0,approved:1b,built:0,wide:%s}' % (facing, shape, length, '1b' if wide else '0b'))


def appearance():
    """A hill of stone with the mine inside, seen as a player walks it: the entrance from above, down the stairs, at
    the junction, along both galleries and into the damaged route. Underground only the mine's own lights light it."""
    mine = Mine()
    c = ['tick unfreeze', 'forceload add 344 292 392 340', 'fill 344 -62 292 392 -40 340 air', 'fill 344 -62 292 392 -62 340 grass_block',
         'fill 350 -61 300 382 -46 330 stone', 'fill 350 -45 300 382 -42 325 stone', 'kill @e[type=item]']
    marker = (364, -46, 326)  # flush in the hilltop, in front of the cliff the stairs cut into
    landing = mine.staircase(marker, 'north', 6)
    mine.set(marker, approved('north', 0, 6))
    growers = [marker]
    lx, ly, lz = landing
    routes = [((lx - 1, ly, lz), 'west', 6, False, ()), ((lx, ly, lz - 1), 'north', 8, True, ()),
              ((lx + 1, ly, lz), 'east', 6, False, (3, 4))]
    for route_marker, facing, length, wide, broken in routes:
        mine.route(route_marker, facing, length, wide=wide, broken=broken)
        mine.set(route_marker, approved(facing, 1, length, wide))
        growers.append(route_marker)
        # where the face's grower stood over its marker: open once the route is done and the grower taken away
        for up in range(1, (WIDE_HEADROOM if wide else ROUTE_HEADROOM) + 1):
            mine.set((route_marker[0], route_marker[1] + up, route_marker[2]), 'air')
    for x, y, z in growers:
        mine.set((x, y + 1, z), 'infestusfrontier:structure_grower{segments:{id:"infestusfrontier:root_segment",count:64},treads:{id:"infestusfrontier:root_tread",count:64},lights:{id:"minecraft:glow_lichen",count:16},ribs:{id:"infestusfrontier:bone_plate",count:32},steps:0,ticks:0}')
    c += mine.cmds
    ex, ey, ez = routes[2][0]
    # the accident: water broke into the end of the east route and gravel fell in; two root cells and their floor
    # went with the cave-in (left out above)
    c += ['setblock %d %d %d water' % (ex + 6, ey + 1, ez), 'setblock %d %d %d gravel' % (ex + 5, ey + 1, ez)]
    c += [sign(366, -45, 328, 8, ['FINISHED MINE', 'walk down: treads,', 'ribs and lights;', 'east: a wreck']),
          sign(362, -45, 328, 8, ['3 GALLERIES', 'west narrow,', 'north wide,', 'east broken'])]
    views = [
        {'name': 'growing', 'x': 364.5, 'y': -43.6, 'z': 329.5, 'yaw': 180, 'pitch': 22, 'frames': 12, 'frameTicks': 20},
        {'name': 'entrance', 'x': 364.5, 'y': -43.6, 'z': 329.5, 'yaw': 180, 'pitch': 22, 'trigger': 'fill 350 -61 300 382 -42 330 air replace infestusfrontier:structure_grower'},
        {'name': 'staircase', 'x': 364.5, 'y': -45.4, 'z': 324.6, 'yaw': 180, 'pitch': 32},
        {'name': 'junction', 'x': 364.5, 'y': -49.4, 'z': 321.6, 'yaw': 180, 'pitch': 18},
        {'name': 'wide_gallery', 'x': 364.5, 'y': -50.0, 'z': 317.9, 'yaw': 180, 'pitch': 6},
        {'name': 'narrow_gallery', 'x': 363.2, 'y': -50.4, 'z': 319.5, 'yaw': 90, 'pitch': 6},
        {'name': 'damaged_route', 'x': 364.8, 'y': -50.4, 'z': 319.5, 'yaw': -90, 'pitch': 12},
    ]
    return {'commands': c, 'views': views}


def write(name, scenario):
    (OUT / (name + '.json')).write_text(json.dumps(scenario, indent=2) + '\n')


def main():
    write('motion_tunnel_root', tunnel_root())
    write('motion_collection_cilia', cilia())
    write('mine_appearance', appearance())
    print('3 capture scenarios: motion_tunnel_root, motion_collection_cilia, mine_appearance')


if __name__ == '__main__':
    main()
