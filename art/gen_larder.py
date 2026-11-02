"""Larder: the mutation of a Mixed Craw that keeps a reserve. The Craw's body in amber, waxed hide with pale bone
creases and a bone strap across each flank, so a Larder reads as a sealed pod beside the Craw's bog-green ribs. The
hull, neck, collar, socket, grades and breath are the pod's own (art/gen_item_capsule.py); only the hide and the straps
differ. Three pockets, the Craw's, show what it holds.

    python3 art/gen_larder.py

Temporary procedural art (docs/sprints/ART.md, placeholder list). Pockets: StorageModule's Pockets(2, ...) follow the
10 pixel socket this file leaves, the Craw's.
"""
from PIL import Image

import gen_item_capsule as pod
from organ_model import pack, write

LARDER_RIM = 1
# the hide: waxed amber, and the creases in pale bone
HIDE = [(48, 26, 8), (84, 46, 14), (122, 72, 24), (160, 104, 40), (192, 136, 62)]
CREASE = [(150, 112, 60), (196, 160, 96), (228, 200, 140)]
# a strap of ivory bone across each flank, a half pixel proud of the hull and partly inside it
STRAPS = [
    ('rib_w', [-6.5, pod.MID + 1, pod.FLOOR + 2], [1, 2, 8], None),
    ('rib_e', [5.5, pod.MID + 1, pod.FLOOR + 2], [1, 2, 8], None),
]


def main():
    bones = pod.bones(LARDER_RIM, STRAPS)
    painter, particle = pod.textures(pack(bones, pod.SHEET), bones, HIDE, CREASE, seed=913)
    write('larder', bones, pod.SHEET, painter, pod.animations(), (particle[0], particle[1]))
    print('larder: bones', len(bones), 'cubes', sum(len(b[3]) for b in bones))


if __name__ == '__main__':
    main()
