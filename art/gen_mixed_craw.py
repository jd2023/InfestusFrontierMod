"""Mixed Craw: the mutation of a Capsule Stem pod that holds four kinds. The Item Capsule's body in dark bog-green
hide with pale bone creases, a wider socket (a one-pixel rim) for four small pockets in a square, and two ivory ribs
on its flanks that read as the partition between kinds. The hull, neck, collar, grades and breath are the pod's own
(art/gen_item_capsule.py); only the hide, the rim and the ribs differ, so a Craw and a bulk pod read apart from
across a room and by the four pockets on their faces.

    python3 art/gen_mixed_craw.py

Temporary procedural art (credited in CREDITS.txt; finishing task in ktask). Pockets: StorageModule's Pockets(2, ...) follow the
10 pixel socket this file leaves.
"""
from PIL import Image

import gen_item_capsule as pod
from organ_model import pack, write

CRAW_RIM = 1
# the hide: dark bog green, and the creases in pale bone
HIDE = [(12, 30, 26), (20, 50, 42), (32, 76, 62), (48, 102, 82), (62, 122, 98)]
CREASE = [(120, 138, 104), (170, 188, 150), (208, 222, 184)]
# two ribs of ivory bone across the flanks, a half pixel proud of the hull and partly inside it
RIBS = [
    ('rib_w', [-6.5, pod.MID - 4, pod.FLOOR + 2], [1, 8, 6], None),
    ('rib_e', [5.5, pod.MID - 4, pod.FLOOR + 2], [1, 8, 6], None),
]


def main():
    bones = pod.bones(CRAW_RIM, RIBS)
    painter, particle = pod.textures(pack(bones, pod.SHEET), bones, HIDE, CREASE, seed=907)
    write('mixed_craw', bones, pod.SHEET, painter, pod.animations(), (particle[0], particle[1]))
    print('mixed_craw: bones', len(bones), 'cubes', sum(len(b[3]) for b in bones))


if __name__ == '__main__':
    main()
