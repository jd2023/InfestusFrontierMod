# Vegetation infection

Status: proposed, except the Withered Flower and tall-plant mappings, which ship
([Shipped: Withered Flower](#shipped-withered-flower),
[Shipped: tall plants](#shipped-tall-plants)). Mostly cosmetic conversion
of vegetation reached by deliberate Living Substrate growth. This document owns the
conversion mappings and visual rules. Existing substrate growth remains defined in
[Living Substrate mutations](LIVING_SUBSTRATE_MUTATIONS.md#1-grow-the-foundation).

## Purpose

Keep the shape of the landscape while changing its character. Flowers wither,
shrubs become dry tangles and trees acquire infected bark and sparse crowns.
A colony grown through a meadow or woodland should inherit its irregular layout.
The player should not need to place every decorative stalk and stump.

Most infected vegetation is dead. It has no production cycle, fuel tank, inventory,
combat effect or ongoing spread. Trees supply useful timber when cut, but do not
become automatic farms.

## When infection happens

Infection accompanies player-funded ground conversion: Spore Culture, a bone-meal
growth wave or a later explicitly supplied ground-growth tool. It does not run
from random ticks or continue spreading after the authorized work ends.

Converting eligible ground converts supported plants rooted on it. A conversion
must account for the plant before ordinary support updates can uproot it. It must
not both drop the original plant and leave a harvestable replacement.

A wave reaching the base of a supported tree may start one finite tree infection.
Contact with the trunk at ground level is sufficient; the wave need not convert
the solid ground beneath every log. Existing ground eligibility normally excludes
solid cover, so tree-base contact needs its own narrow rule. It must not make all
covered ground eligible for substrate growth.

Dormant substrate does not continually attack plants placed later. A later
deliberate growth treatment may infect newly eligible vegetation. Repeating a
treatment on already infected vegetation creates no extra items or growth.

## Proposed mappings

| Existing vegetation | Result | Harvest and behavior |
|---|---|---|
| Dandelion, poppy, blue orchid, allium, azure bluet, tulips, oxeye daisy, cornflower and lily of the valley | Withered Flower | Inert. Normal break yields the original flower under its normal loot conditions. Source identity is retained. |
| Sunflower, lilac, rose bush and peony | Tall Withered Flower | One two-block plant with a dry stalk and spent seed heads. Breaking either half resolves one harvest, never two. Retains the source plant's normal drop rules. |
| Short grass and fern | Blighted Undergrowth | Inert dry blades or curled fronds. Retains the original grass/fern harvest rules, including tools and chance drops. |
| Tall grass and large fern | Tall Blighted Undergrowth | The tall equivalent, with one harvest across both halves. |
| Sweet berry bush | Dead Berry Bush | Retains the age present at infection for its normal break loot. No regrowth, berry picking or bone-meal production after infection. |
| Dead bush | Blighted Dead Bush | Cosmetic darkening and root threads. Retains ordinary dead-bush drops. |
| Oak, spruce, birch, jungle, acacia and dark oak saplings | Withered Sapling | No growth. Breaking returns the original sapling through normal harvest rules. |
| Supported rooted tree logs and leaves | Infected Logs and Blighted Leaves | Preserves the admitted tree's geometry. Logs provide infected timber. Leaves retain source-specific harvest rules and vanilla decay behavior. |

Initial tree support covers ordinary, dry-land oak, birch, spruce, jungle, acacia
and dark oak forms that fit the bounded tree rules below. Support is explicit;
a tree's species alone does not authorize an arbitrarily large connected structure.

Leave crops, cultivated mod plants, potted plants, water plants, cactus, sugar cane,
bamboo, mushrooms, vines, moss, mangrove roots, azalea systems, cherry petal patches,
torchflowers and pitcher plants outside the initial mapping. Unsupported vegetation
does not receive a guessed conversion or guessed loot rule. If converting its
support would destroy it, leave that ground unconverted for this feature.

Later support can add mappings without requiring a new model for every species.
No wildcard conversion of third-party plants or blocks with inventories.

## Shipped: Withered Flower

The small-flower row is implemented in `ecology` (`WitheredFlowerBlock`, `FlowerSource`,
`Vegetation`).

- Spore Culture and the bone-meal wave convert a host through one routine,
  `Vegetation.convert`. What stands on the host decides: a supported flower is
  replaced by its dead form first and the ground after it, so vanilla's support
  update finds a dead flower standing on substrate; air or a block that does not
  depend on the ground leaves the host free; any other rooted plant (the bush
  family, so saplings, grass, crops, bushes and mushrooms too, plus sugar cane,
  bamboo and the big dripleaf) keeps its ground unconverted. Spore Culture is not
  used up on such ground and the wave does not count it as a host. Existing growth
  limits are unchanged ([ENGINEERING.md](../../ENGINEERING.md#performance-limits),
  "Vegetation infection").
- The block keeps one property, `source`, the flower it was. Its loot table rolls
  that flower's own table through a nested `minecraft:loot_table` entry in the real
  loot context, so tools, Silk Touch and data-pack changes to the flower carry over.
  Pick block gives the flower. There is no Withered Flower item.
- Appearance is never saved. The blockstate lists the three silhouettes turned four
  ways and vanilla picks one by the position's seed; the block colour picks ash,
  bone, brown or plum from the same seed. The same position always shows the same
  look, on both sides of a chunk border and after every reload.
- Dormant cells never scan. Bone meal and culture on a dead flower, or on the cell
  under it, make nothing.
- Guide: `contact/withered_flowers`. Demo: the Meadow and Withered Flower plots in
  row B of `demo.json`, and the `withered_flowers` capture.

## Shipped: tall plants

The two-block rows are implemented in `ecology` (`TallWitheredFlowerBlock` and
`TallBlightedUndergrowthBlock`, both `DeadTallPlantBlock`; `TallFlowerSource`,
`TallUndergrowthSource`; `Vegetation`).

- Sunflower, lilac, rose bush and peony become Tall Withered Flower. Tall grass and large
  fern become Tall Blighted Undergrowth. Each is vanilla's `DoublePlantBlock` with a `half`
  and a `source`, the plant it was. The halves support each other and vanish together
  through vanilla's own update and break paths. There is no item; pick block gives the
  source plant.
- `Vegetation.convert` withers a tall plant only when it is whole: the block over the host
  is a mapped plant's lower half, and the block over that, in a loaded chunk, is the same
  plant's upper half. A lone lower half, a mismatched pair, an unloaded or out-of-world upper
  half, or a mapped plant seen from its upper half leaves the host unconverted, as every
  other unsupported rooted plant does: Spore Culture is refused and a wave does not count the
  host. The check runs when the wave scans and again when it converts, so a plant broken
  between the two keeps its ground.
- A conversion replaces both halves before the ground, without shape updates (either half
  alone would tell the other its partner is gone), and drops nothing. The ground change then
  finds a dead plant standing on substrate.
- Harvest is one loot table per family, written with vanilla's own pools and conditions for
  each source: a flower drops itself from its lower half only; tall grass and large fern drop
  two short plants with shears, otherwise a seed one time in eight, from whichever half still
  has its partner. Vanilla's order does the rest: whichever half breaks first rolls the table
  once and the cascade that removes the other half rolls it to nothing. Fortune and Silk Touch
  change nothing, as they change nothing for the living plants. A data pack that changes a
  vanilla plant's table does not change the dead one: the vanilla tables test the living
  block, so they cannot be nested as the small flower's are.
- Appearance is never saved. Vanilla seeds a double plant's look from its lower half, so the
  blockstate's four quarter turns and the block colour agree on both halves. One flower model
  serves all four flowers; undergrowth has one dry-grass and one fern shape. The tints are ash,
  bone, brown and plum for flowers, straw, bone, ash and dry brown for undergrowth.
- No block entity, tick or bone-meal response. Culture or bone meal on a dead tall plant, or on
  the cell under it, makes nothing. They burn like flowers and grass.
- Guide: `contact/withered_flowers`, page "tall". Demo: the Tall Plants plots south of the
  Meadow, at x 31 to 46, z -27 to -23, and the `tall_withered_plants` capture.

## Harvest rules

Infection is a replacement, not a harvest. It creates no loose items or experience.
One original plant leaves one replacement that can be harvested once.

Retain the minimum bounded source identity and properties needed for an explicit
supported loot mapping. For example, a dead berry bush retains its age and
blighted leaves retain their species. Random cosmetic variation does not affect
drops. A player cannot reroll age or gain a second harvest by applying more culture.

Use normal loot contexts, tool conditions, Fortune and Silk Touch where the source
supports them. Do not approximate all flowers or bushes as sticks. Do not invoke
both the source loot and replacement loot for one break. Two-block plants and leaf
decay need explicit conservation tests.

Dead flowers and shrubs return their ordinary source drops by default. They do not
add a second collection of craftable dead-flower items. Thus an area looks dead
without permanently removing its useful plants from the player's inventory.

## Visual variety with few assets

Use one Withered Flower block family, not one bespoke model per flower species.
Source identity controls drops; appearance is a separate selection.

Proposed shared flower silhouettes:

- A bent stalk with a hanging spent head.
- A forked dry stalk with small seed pods.
- A low, collapsed flower with curled petals.

Choose among these with stable position-based model variation. Combine that choice
with quarter-turn rotation and a small set of muted ash, bone, brown and plum
colors. Tall flowers use one additional two-block stalk family. Grass and shrubs
reuse their own small silhouette families rather than stretching flower models.

The appearance stays stable across save/reload and chunk boundaries. It does not
change every frame or require a ticking block entity. Ground cover remains small
enough that adjacent patches do not form a solid visual wall.

Most shapes are dry and still. Infection appears as occasional dark root threads
and dull fleshy knots. Avoid making every flower glow or pulse. The contrast with
working organs should make the organs easier to read.

Placeholder shapes and textures are sufficient for implementation and demos.
Record the few shared final assets in the existing placeholder list for a later
batch art pass. Final artwork is not a development prerequisite.

## Trees

### Preserve the silhouette

Convert admitted logs in place, preserving their axes and branch positions. Use a
shared infected-bark family with restrained source-dependent tones. Do not replace
every tree with the same generated sculpture.

Convert admitted foliage to Blighted Leaves with sparse cutout textures. Keep the
existing leaf positions initially; holes in the texture expose the branches.
Avoid removing a random fraction of leaf blocks and showering the colony in drops.
Retain vanilla leaf-distance, persistence and decay behavior through supported
block behavior. Infected logs must support their blighted leaves normally.

The visual result is a mostly dead crown above a trunk still threaded with living
tissue. No resin production, fruit production or automatic branch regrowth comes
from infection alone.

### Tree boundaries

Start from a supported upright trunk rooted on eligible ground. Discover a finite
connected log structure and associated foliage in loaded terrain before admitting
the conversion. Leaf contact alone must not carry infection into another tree.
Multiple roots reached by one wave must not create duplicate work on one tree.

Do not claim that vanilla blocks reliably identify a naturally generated tree.
A player-built structure using supported unstripped logs and foliage can resemble
one. The guide must state the actual eligibility rules. Planks, stripped logs,
wooden mechanisms, block entities and arbitrary building materials are excluded.

Oversized, ambiguous or unloaded structures are left unchanged. Do not silently
infect the first part of a forest-sized connected structure. A rejected tree does
not cancel unrelated eligible ground and flower conversions.

Once admitted, conversion proceeds incrementally. Recheck each target before
replacement; player edits are not overwritten. Persist bounded progress and pause
on unload without loading chunks. Infection does not propagate from the converted
logs as an independent new wave.

Proposed first limits: one tree job admits at most 128 logs and 512 leaves inside
a 17×32×17 envelope. Discovery stops after 2,048 examined positions. At most 16
jobs exist per dimension, with server-wide budgets of 128 discovery reads and 16
replacements per tick. These are starting limits for testing, not measured safe
capacity. At capacity, skip new tree infection and explain the limit; do not add
an unbounded retry queue. Existing substrate-wave limits remain unchanged.

### Infected timber and Living Wood

Use **Infected Log** for harvested infected trunks. Proposed basic recipe:
one Infected Log makes four Infected Planks. The first crafting scope includes
planks and ordinary wooden ingredients such as sticks and crafting-table recipes
through explicit compatible tags. Do not grant every special log recipe by default.

Cutting yields infected timber instead of the original log, never both. Axes and
normal block-breaking behavior apply. Replacing a harvested log and infecting it
again cannot multiply wood. Initial infection adds no biomass-value premium over
equivalent ordinary timber.

Keep this material distinct from **Living Wood**, which the existing catalog uses
for cultivated production and biological construction. A later explicit Rooting Gel
treatment can turn one suitable Infected Log into one Living Wood, alongside the
existing ordinary-log treatment route. Its recipe belongs in the Item Catalog
when implemented. Passive infection alone does not unlock Arbor Root production,
resin tapping or the Living Wood digestion yield.

This gives infected trees immediate crafting value while leaving a progression
from damaged wild timber to deliberately cultivated living wood.

## Implementation and demonstration

The implementation must use supported vanilla and NeoForge placement, support,
loot and decay behavior. If a mapping needs engine patches or suppressing ordinary
engine rules, revise that mapping before implementing it.

Add a labeled meadow and woodland exhibit to the default `scripts/demo.sh` world.
Include untreated plants, a player-triggerable substrate wave, varied dead flowers,
dead berry bushes at different source ages and a supported tree. Let the player
harvest the results and craft infected timber. Demonstrate excluded vegetation and
a tree that exceeds a limit without damaging either.

The guide must explain deliberate infection, normal plant drops, inert vegetation,
tree eligibility and the difference between infected timber and Living Wood.
Verification belongs to implementation: no conversion drops, one harvest per
source, tall-plant conservation, stable visuals, interrupted tree conversion,
loaded-only bounded work and preservation of unrelated player edits.

Design decisions and framework integration require a strong model. Do not assign
them to a mechanical worker. Small explicit data mappings and copies of an accepted
asset pattern may be delegated only after the behavior is settled. Artwork stays
on the placeholder list until the owner requests a batch finishing pass.
