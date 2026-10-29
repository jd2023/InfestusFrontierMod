"""Vascular Junction (BLOCK_CATALOG T1-04): a heart. A dark muscular body with four chambers swelling at its corners
and a crown bulging in two steps under the top mouth, laced with vessels, carries a sphincter on each of its six
faces: thick lips across, thin lips upright, open at the corners, over a dark throat sunk in the body; the ring
gapes to draw in, puckers to push out and shrinks to a bud when closed. The lobed body that beats and the lipped
mouths are the cues.

    python3 art/gen_vascular_junction.py

Animations: idle (the heart beats, lub-dub, twice a loop) and, for each face, <face>_closed, <face>_intake,
<face>_outlet; the block entity runs one controller per face and picks by the face's port in the block state.
"""
from organ_model import Painter, ball, faces, flat, islands, pack, sampled, skin, squeeze, write

SHEET = 64
IDLE, PULSE = 7.0, 2.5
LEAN = 9
HEART = [(52, 14, 30), (84, 26, 44), (118, 40, 58), (150, 62, 74), (178, 92, 96), (204, 126, 122)]
ROOT = [(46, 26, 34), (70, 38, 42), (98, 56, 54), (126, 78, 68), (154, 104, 86)]
LIP = [(120, 40, 70), (160, 70, 96), (196, 110, 126), (222, 150, 156)]
VESSEL = (176, 96, 102)

# mouth: name -> pivot on the body's face (the bone scales about it)
PIVOTS = {'up': [0, 12, 0], 'down': [0, 2, 0], 'north': [0, 7, -5], 'south': [0, 7, 5], 'east': [5, 7, 0], 'west': [-5, 7, 0]}


def ring(name):
    """The four lips of one mouth, sunk 0.15 px into the body's face: a thick pair across and a thin pair upright,
    the thin pair shorter so the corners stay open and the ring reads rounded, and half a pixel prouder, so no two
    lips share a face even shrunk to a bud."""
    if name in ('north', 'south'):
        z, zv = (-7.15, -7.65) if name == 'north' else (5.15, 5.65)
        return [('lip_z_h', [-2.5, 4, z], [5, 2, 2], None), ('lip_z_h', [-2.5, 8, z], [5, 2, 2], None),
                ('lip_z_v', [-2.75, 5.5, zv], [1, 3, 2], None), ('lip_z_v', [1.75, 5.5, zv], [1, 3, 2], None)]
    if name in ('east', 'west'):
        x, xv = (5.15, 5.65) if name == 'east' else (-7.15, -7.65)
        return [('lip_x_h', [x, 4, -2.5], [2, 2, 5], None), ('lip_x_h', [x, 8, -2.5], [2, 2, 5], None),
                ('lip_x_v', [xv, 5.5, -2.75], [2, 3, 1], None), ('lip_x_v', [xv, 5.5, 1.75], [2, 3, 1], None)]
    if name == 'up':
        return [('lip_u_a', [-2.5, 12, -2.5], [5, 3, 2], None), ('lip_u_a', [-2.5, 12, 0.5], [5, 3, 2], None),
                ('lip_u_b', [-2.75, 11.75, -1.5], [1, 3, 3], None), ('lip_u_b', [1.75, 11.75, -1.5], [1, 3, 3], None)]
    return [('lip_d_a', [-3, 0, -3], [6, 2, 2], None), ('lip_d_a', [-3, 0, 1], [6, 2, 2], None),
            ('lip_d_b', [-3.25, 0.15, -1], [1, 2, 2], None), ('lip_d_b', [2.25, 0.15, -1], [1, 2, 2], None)]


def throat(name):
    """The dark pucker behind a ring, in the body so the lips shrink and gape over it, turned a few degrees about
    the mouth's axis so none of its faces lies level with a lip's."""
    if name == 'north':
        return ('throat_z', [-1.5, 5.5, -5.4], [3, 3, 1], ([0, 0, 6], [0, 7, -5]))
    if name == 'south':
        return ('throat_z', [-1.5, 5.5, 4.4], [3, 3, 1], ([0, 0, 6], [0, 7, 5]))
    if name == 'east':
        return ('throat_x', [4.4, 5.5, -1.5], [1, 3, 3], ([6, 0, 0], [5, 7, 0]))
    if name == 'west':
        return ('throat_x', [-5.4, 5.5, -1.5], [1, 3, 3], ([6, 0, 0], [-5, 7, 0]))
    return ('throat_d', [-1.5, 1.75, -1.5], [3, 1, 3], ([0, 6, 0], [0, 2, 0]))


def lobe(sx, sz):
    """A chamber bulging from one vertical edge of the body: two diamonds, the upper one swelling further out, both
    turned so their corners point out and leaning outward, so the chamber reads rounded; their tops a step darker."""
    lean = [-sz * LEAN, 45, sx * LEAN]  # turned corner-out and leaning out, so no face of it lies level with a lip's
    return [('lobe_low', [sx * 4.05 - 2, 3, sz * 4.05 - 2], [4, 3, 4], (lean, [sx * 4.05, 3, sz * 4.05])),
            ('lobe_up', [sx * 4.75 - 1.5, 5.75, sz * 4.75 - 1.5], [3, 3, 3], (lean, [sx * 4.75, 5.75, sz * 4.75]))]


BONES = [
    ('root', None, [0, 0, 0], []),
    # the body: a block of muscle a quarter pixel up off the root mouth, a dome under the crown mouth
    ('hub', 'root', [0, 2, 0], [
        ('body', [-5, 2.25, -5], [10, 9, 10], None),
        ('dome', [-4, 10.25, -4], [8, 2, 8], None),
        ('crown', [-3, 11.25, -3], [6, 2, 6], ([0, 6, 0], [0, 12, 0])),
        throat('north'), throat('south'), throat('east'), throat('west'), throat('down'),
    ]),
    ('lobes', 'hub', [0, 6.5, 0], lobe(-1, -1) + lobe(1, -1) + lobe(-1, 1) + lobe(1, 1)),
]
for name, pivot in PIVOTS.items():
    BONES.append(('mouth_' + name, 'hub', pivot, ring(name)))


def textures(uv):
    p = Painter(SHEET, 211)
    sizes = islands(BONES)
    rects = {name: faces(sizes[name], uv[name]) for name in sizes}
    for name in ('body', 'dome', 'crown', 'lobe_low', 'lobe_up'):
        for face, rect in rects[name].items():
            if face == 'down' and name == 'body':
                p.ramp(rect, ROOT, flat(0.4), grain=0.05, mottle=0.14)
                continue
            # taut dark muscle, lit from above in soft blotches; the chambers' tops a step darker, so no cap catches light
            lobe = name.startswith('lobe')
            p.ramp(rect, HEART, lambda x, y, w, h: (0.4 if lobe and face == 'up' else 0.4 * skin(x, y, w, h) + 0.45 * ball(x, y, w, h) + (0.08 if lobe else 0)), grain=0.04, mottle=0.35)
            if face not in ('up', 'down'):
                vessels(p, rect, 2 if name == 'body' else 1)
    for name in sizes:
        if name.startswith('throat'):
            for face, rect in rects[name].items():
                p.ramp(rect, HEART, lambda x, y, w, h: 0.05 + 0.1 * (x in (0, w - 1) or y in (0, h - 1)), grain=0.02, mottle=0.05)
        if not name.startswith('lip'):
            continue
        for face, rect in rects[name].items():
            # lips: rounded, pale and wet along their outer ridge, dark where they meet the throat
            p.ramp(rect, LIP, lambda x, y, w, h: 0.45 + 0.5 * ball(x, y, w, h) - 0.3 * (x in (0, w - 1) or y in (0, h - 1)), grain=0.04, mottle=0.12)
    return p, rects['body']['north']


def vessels(p, rect, count):
    """Two-texel-wide vessels in a mid tone, branching down a face a little off the vertical."""
    _, _, w, h = rect
    for _ in range(count):
        x, y = p.rng.randrange(max(1, w - 1)), 0
        for _ in range(h):
            for dx in (0, 1):
                p.put(rect, x + dx, y, VESSEL, 210)
            x += p.rng.choice((-1, 0, 0, 1))
            y += 1


def axis(name):
    """Scale vector along the mouth's own axis: (x, y, z) weights."""
    return {'up': (0, 1, 0), 'down': (0, 1, 0), 'north': (0, 0, 1), 'south': (0, 0, 1), 'east': (1, 0, 0), 'west': (1, 0, 0)}[name]


def beat(t):
    """Lub-dub: a strong contraction and a weaker one close behind, twice a loop; at most 1."""
    return (squeeze(t, IDLE / 2) + 0.5 * squeeze(t, IDLE / 2, 0.22 * IDLE / 2)) / 1.4


def animations():
    out = {'idle': (IDLE, True, {
        # the body beats sideways only, so the root mouth keeps its foot on the foundation
        'hub': {'scale': sampled(IDLE, 0.05, lambda t: [1 + 0.01 * beat(t), 1, 1 + 0.01 * beat(t)])},
        'lobes': {'scale': sampled(IDLE, 0.05, lambda t: [1 + 0.04 * beat(t), 1 + 0.03 * beat(t), 1 + 0.04 * beat(t)])},
    })}
    for index, name in enumerate(PIVOTS):
        bone = 'mouth_' + name
        ax = axis(name)
        lag = index * PULSE / len(PIVOTS)

        def closed(t, ax=ax, name=name):
            return [1 if name == 'down' and i == 1 else 0.45 if ax[i] == 0 else 0.3 for i in range(3)]

        def intake(t, ax=ax, lag=lag, name=name):
            s = squeeze(t, PULSE, lag)
            return [1 if name == 'down' and i == 1 else 1 + 0.3 * s if ax[i] == 0 else 1 - 0.25 * s for i in range(3)]

        def outlet(t, ax=ax, lag=lag, name=name):
            s = squeeze(t, PULSE, lag)
            return [1 if name == 'down' and i == 1 else 1 - 0.15 * s if ax[i] == 0 else 1 + 0.1 * s for i in range(3)]

        out[name + '_closed'] = (PULSE, True, {bone: {'scale': sampled(PULSE, 0.5, closed)}})
        out[name + '_intake'] = (PULSE, True, {bone: {'scale': sampled(PULSE, 0.05, intake)}})
        out[name + '_outlet'] = (PULSE, True, {bone: {'scale': sampled(PULSE, 0.05, outlet)}})
    return out


def main():
    painter, particle = textures(pack(BONES, SHEET))
    write('vascular_junction', BONES, SHEET, painter, animations(), (particle[0], particle[1]), bounds=(2, 1, (0, 0.25, 0)))
    print('vascular_junction: bones', len(BONES), 'cubes', sum(len(b[3]) for b in BONES))


if __name__ == '__main__':
    main()
