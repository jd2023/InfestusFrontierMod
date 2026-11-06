# Vegetation infection

Status: proposed, except the Withered Flower, tall-plant and shrub mappings, the infected timber blocks and the
infection of a standing tree, which ship
([Shipped: Withered Flower](#shipped-withered-flower),
[Shipped: tall plants](#shipped-tall-plants),
[Shipped: shrubs, grass and saplings](#shipped-shrubs-grass-and-saplings),
[Shipped: infected timber](#shipped-infected-timber-and-blighted-leaves),
[Shipped: tree infection](#shipped-tree-infection)). Mostly cosmetic conversion
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
| Rooted tree logs and leaves of any ordinary tree | Infected Log and Blighted Leaves | Preserves the admitted tree's geometry. Every trunk becomes the one Infected Log and every crown the one Blighted Leaves, whatever the tree was. Leaves keep vanilla decay; their harvest is common to all trees. Ships ([Shipped: tree infection](#shipped-tree-infection)). |

Tree support covers every ordinary tree whose trunk and leaves the data tags name, vanilla's and other mods',
that fits the bounded tree rules below ([Shipped: tree infection](#shipped-tree-infection)). A tag alone
does not authorize an arbitrarily large connected structure.

Leave crops, cultivated mod plants, potted plants, water plants, cactus, sugar cane,
bamboo, mushrooms, vines, moss, mangroves, azalea bushes, cherry petal patches,
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
  depend on the ground leaves the host free; any other rooted plant without a
  mapping (the bush family, so crops, torchflowers, azaleas, other saplings and
  mushrooms, plus sugar cane, bamboo and the big dripleaf) keeps its ground unconverted. Spore Culture is not
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

## Shipped: shrubs, grass and saplings

The one-block rows are implemented in `ecology` (`DeadBerryBushBlock`, `BlightedDeadBushBlock`,
`BlightedUndergrowthBlock` and `WitheredSaplingBlock`, all `DeadShrubBlock`; `UndergrowthSource`,
`SaplingSource`; `Vegetation`).

- Sweet berry bush becomes Dead Berry Bush, dead bush Blighted Dead Bush, short grass and fern
  Blighted Undergrowth, and oak, birch, spruce, jungle, acacia and dark oak saplings Withered Sapling.
  `Vegetation.convert` withers them as it does a small flower: the plant is replaced first, nothing
  drops, then the ground. A plant is matched by its exact vanilla block, so a cherry sapling, a
  mangrove propagule, an azalea, a torchflower, a crop, a mushroom or another mod's plant is rooted
  without a mapping and keeps its ground. A potted plant is solid cover and keeps its ground too.
- Each block keeps only what its harvest needs. Dead Berry Bush keeps `age` (0 to 3) as it was at
  conversion; Blighted Undergrowth and Withered Sapling keep `source`; Blighted Dead Bush keeps
  nothing. Pick block gives the plant: sweet berries, dead bush, short grass, fern or the sapling.
  There is no item.
- Harvest is one loot roll in the real loot context. Blighted Dead Bush, Blighted Undergrowth and
  Withered Sapling nest the source's own table (`minecraft:blocks/dead_bush`, `short_grass`, `fern`,
  `<species>_sapling`), so shears, bare hands, Silk Touch, Fortune and data-pack changes carry over.
  Dead Berry Bush has its own table, a copy of vanilla's with this block's `age`, because vanilla's
  tests the living block: age 3 gives 2 to 3 berries, age 2 gives 1 to 2, ages 0 and 1 give nothing,
  Fortune adds as it does to the bush. Breaking is the only harvest.
- The blocks are plain `BushBlock`s. A Dead Berry Bush cannot be picked, takes no random tick, never
  ages, deals no contact damage or slowness and answers no bone meal. A Withered Sapling is not a
  `SaplingBlock`: no growth stage, no tree. None has a block entity, a tick or a timer. They burn
  like flowers. Culture or bone meal on a dead plant, or on the cell under it, makes nothing.
- Appearance is never saved. The blockstate lists the shapes turned four ways and vanilla picks one by
  the position's seed; the block colour tints by position (berry bush, dead bush, undergrowth) or by
  species (sapling: oak brown, birch bone, spruce dark green-grey, jungle olive, acacia tan, dark oak
  umber). Shapes: a bare sprout (berry age 0 and 1), a bare bush (age 2), the bush with dried berries
  (age 3); two twig tangles; two tufts of dry blades and one curled frond; a bare stem and a stem with
  curled leaves.
- Guide: `contact/withered_shrubs`. Demo: the Shrubs plots at x 31 to 45, z -13 to -2 in `demo.json`
  (a living row, its dead twin and an excluded row), and the `withered_shrubs` capture.

## Shipped: infected timber and blighted leaves

The blocks a converted tree is made of are implemented in `ecology` (`InfectedLogBlock`, `BlightedLeavesBlock`). A
standing tree becomes them through tree infection ([Shipped: tree infection](#shipped-tree-infection)); no recipe makes
a log from anything. They are one material: no species, no tint by origin, no block entity, no data component.

- Infected Log is vanilla's `RotatedPillarBlock` with only `axis`. Breaking it, with any tool, drops one Infected Log
  (`loot_table/blocks/infected_log.json`); a log cut, put down and cut again is still one log. Axis survives rotation.
  It burns 300 ticks (`neoforge` fuel data map) and is mined with an axe. No recipe takes it; the item is in no item
  `logs` tag, so no recipe that takes ordinary logs takes it either. There is no premium: biomass value is that of
  ordinary timber and the Digestive Sac takes none.
- Infected Logs sit in the `logs_that_burn` block tag, which feeds `minecraft:logs`, the tag vanilla's `LeavesBlock`
  tests to count a neighbour as distance 0. That is the whole of the support: no leaf code of ours.
- Blighted Leaves are vanilla's `LeavesBlock` with nothing added. `distance`, `persistent` and `waterlogged` are
  vanilla's and survive every update, so decay, placement by a log, the 7-block range and water behave as for living
  leaves. A leaf at distance 7 and not persistent decays on a random tick, and cutting a trunk starts that. Breaking and
  decay roll `loot_table/blocks/blighted_leaves.json`: shears or Silk Touch give one Blighted Leaves, anything else
  gives nothing. No sapling, stick, apple or fruit of any tree. The item places persistent leaves; pick block gives it.
- Infected Planks are no longer made: no recipe, no creative tab entry. The block, item, loot, `planks` tags and fuel
  stay so old worlds and inventories keep them and use them as planks.
- A block state saved by an older version with `source` reads as the one block: vanilla's state codec ignores the
  unknown property and keeps axis, distance, persistence and water.
- Appearance is never saved. The log and leaves each have one model, one texture and one tint: grey bark with root
  threads and a grey sparse leaf cutout over vanilla's full cube. No tick, resin, fruit, regrowth or spread.
- Guide: `contact/infected_timber`. Demo: the Timber exhibit at x 47 to 75, z -13 to -2 in `demo.json` (six living
  trees, oak to cherry, each beside the same Infected Log and Blighted Leaves, lying logs, a living and an infected oak
  side by side, a furnace burning Infected Logs, a chest of logs and leaves and the tools) and the `infected_timber`
  capture.

## Shipped: tree infection

The infection of a standing tree is implemented in `ecology` (`TreeInfection`, `TreeInfections`) over the pure rules
in `core.ecology.TreeDiscovery`, with the budgets in `core.foundation.TickQuota`.

- Trees are data. A trunk log is a block in `infestusfrontier:infectable_logs` (by default vanilla's
  `minecraft:overworld_natural_logs`) and in neither `infestusfrontier:uninfectable_logs` (the mangrove log and the
  Infected Log) nor the common `c:stripped_logs` and `c:stripped_woods`, with an `axis`, a full collision cube and no
  block entity. Foliage is a `LeavesBlock` in `infestusfrontier:infectable_leaves` (by default `minecraft:leaves`) and
  not in `infestusfrontier:uninfectable_leaves` (mangrove leaves and Blighted Leaves), with vanilla's distance,
  persistence and water and no block entity. `minecraft:logs` and `logs_that_burn` are not used: they hold wood and
  stripped logs too. Mods that add trees fill the vanilla tags: Biomes O' Plenty, Oh The Biomes We've Gone and
  Atmospheric list their natural logs, without wood or stripped logs, in `overworld_natural_logs`, their leaves in
  `leaves` and their stripped logs and wood in the common tags. A pack adds or removes a tree through the four
  `infestusfrontier` tags. Every tag read is vanilla's, NeoForge's or the mod's own, so a game without tree mods
  loads as before.
- Contact. A cell the bone-meal wave converts or steps, and a cell Spore Culture makes, touches the trunk bases beside
  it: the four horizontal neighbours, level with the cell, one up or one down. A root is an upright (axis y) trunk log
  standing on a host block or a cell. Nothing else is contact: a lying log, a log on sand or planks, a stripped log,
  wood, planks, an Infected Log. The ground under the trunk is not made a host and is not converted; no other covered
  ground becomes eligible.
- Discovery. One job per root. The job keeps the root's block and state as contact found it; a root replaced since,
  by another log or the same log lying down, is not the tree, and the job ends with nothing turned. A block's kind is
  its registry id. Discovery reads the world through `TreeDiscovery`: trunk
  logs of the root's kind join through any of their 26 neighbours, as vanilla's diagonal branches do; a leaf belongs
  when it stands face to face with a log of the tree at leaf distance 1 or with a claimed leaf at one more than its
  distance, the gradient vanilla's own distance rule writes. The kinds of leaf the trunk holds at distance 1 are the
  tree's foliage; a gradient leaf of another kind is another tree's. Leaves never lead to logs, so touching crowns
  never carry a job into the next tree, and a leaf nearer another trunk keeps a lower distance than the gradient wants
  and is left alone; a leaf of the same kind the same distance from two trunks goes to whichever tree is discovered
  first. Every position is read once and counted, and the state read at each log and leaf is what that target must
  still be when it is replaced.
- Admission. The tree fits a 17x32x17 envelope (8 each way from the root, one below it, 30 above), has at most 128 logs
  and 512 leaves of at most 4 kinds and costs at most 2,048 reads; otherwise it is refused as oversized. A trunk log of
  another kind joined to it, or a fifth kind of leaf by the trunk, refuses it as ambiguous; a position in no loaded chunk
  refuses it as unloaded, and no chunk is loaded. A refused tree is left whole. A root a job already claims is not a
  second job; two jobs that discover one tree keep the one that converts first.
- Conversion. Logs first, in discovery order from the root, then leaves by rising distance: each block is read again
  and replaced only if it is still a trunk log or foliage in the block and state discovery read there, leaf distance
  aside (vanilla changes it as neighbours change). A block a player changed is kept, also one of the same block: a log
  laid on its side, leaves broken and placed again (persistent) or filled with water. A log becomes the Infected Log with its axis, a leaf Blighted Leaves with its distance,
  persistence and water. The replacement sends no neighbour updates (the new log is a log, the leaf keeps its
  distance), drops nothing and gives no experience. Infected Logs are no trunk logs, so a converted tree starts nothing
  and is never discovered again.
- Attached plants. A block that stands against a trunk log and survives there, but would not survive against the
  Infected Log, is removed with the log, without a drop, an item or experience. Cocoa pods are the case: they test
  vanilla's `jungle_logs` tag, the Infected Log is in no species tag, and the pod would otherwise drop on the next
  neighbour update. The test is the block's own `canSurvive`, read before and after the log is replaced, so a mod's
  attachment that tests its log's tag is handled the same way; a block that stands on the Infected Log (a torch, a
  button, a vine) stays. An infected tree grows no pods and becomes no farm. A log waits, like any target, until the chunks its
  attachments' survival checks may read (the log's chunk and the chunks 2 blocks round it) are loaded, and loads none.
- Budgets. At most 16 jobs a dimension, discovering and converting together; 128 discovery reads and 16 replacements a
  server tick, shared by every dimension through two `TickQuota`s on the overworld's data, handed round the jobs in
  turn. A tree touched while the jobs are full is refused for capacity; there is no queue, and the next wave or
  culture asks again. Existing wave limits are unchanged: a refused tree costs the wave nothing and the ground and
  plants it reaches convert as before.
- Feedback. Admission and every refusal (capacity, oversized, ambiguous, unloaded) are shown above the hotbar to the
  players within 32 blocks of the root, once per tree and reason in 200 ticks (`infestusfrontier.tree_infection.*`),
  naming the trunk's block. `EcologyModule.treeInfections` is the snapshot of a dimension's jobs and its last 16
  refusals.
- Persistence. Each dimension saves its jobs (`infestusfrontier_tree_infections`): root, the kind of its logs, the
  root's state, and the logs and leaves still to convert, each with the index of its expected state in a table of the
  job's distinct states (at most one a target); a job still discovering saves no targets and discovers again on load,
  building its targets and table anew. A job an older version saved with `species` (one of oak, birch, spruce, jungle,
  acacia and dark oak) reads as that species' vanilla log and leaves; it had no states, so its targets are checked by
  block alone (`identity_only`) and its root by block and upright axis, and it is saved so until it ends. Each record
  is read in exactly one format: a species and no field of the current format, or a log and no species. A mixture, an
  unknown species, indices or states that do not match, a saved state whose properties do not read back exactly (an
  invalid, unknown, missing or mistyped property, which vanilla would replace by its default), or a log or root state of
  another block keep the whole list as it was, written back on every save. A job whose next block is in no loaded
  chunk waits, asks again after 100 ticks and loads nothing; it goes on when the chunk is back, also after a save and
  reload.
- Guide: `contact/infected_timber`, pages "infect", "rules", "refused" and "packs". Demo: the Woodland plot at x 79 to
  115, z -13 to -2 in `demo.json` (an oak with a bone-meal dispenser and button at the end of a one-wide corridor, its
  infected twin, a birch and a cherry with touching crowns and a cell by each, a treehouse with planks, a stripped log,
  a chest and a fence against the trunk, a 2x2 spruce of 132 logs and 33 blocks that is refused, and a chest of bone
  meal, culture, an axe and shears), the `woodland_wave` frames and the `tree_infection` capture. The Jungle plot east of
  it at x 117 to 129, z -13 to -2 shows a living jungle tree with four cocoa pods, a cell under a bone-meal dispenser
  and button, its infected twin without pods and the same supplies; the `jungle_cocoa` and `jungle_cocoa_after` views
  photograph it before and after.

## Harvest rules

Infection is a replacement, not a harvest. It creates no loose items or experience.
One original plant leaves one replacement that can be harvested once.

Retain the minimum bounded source identity and properties needed for an explicit
supported loot mapping. For example, a dead berry bush retains its age. Infected
timber retains no source: its harvest is common to all trees. Random cosmetic
variation does not affect drops. A player cannot reroll age or gain a second harvest by applying more culture.

Use normal loot contexts, tool conditions, Fortune and Silk Touch where the source
supports them. Do not approximate all flowers or bushes as sticks. Do not invoke
both the source loot and replacement loot for one break. Two-block plants and leaf
decay need explicit conservation tests.

Dead flowers and shrubs return their ordinary source drops by default. They do not
add a second collection of craftable dead-flower items. Infected trees are the
exception: one Infected Log a log, and Blighted Leaves only to shears or Silk Touch. Thus an area looks dead
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

Convert admitted logs in place, preserving their axes and branch positions. Use one
infected bark for every tree. Do not replace every tree with the same generated
sculpture.

Convert admitted foliage to Blighted Leaves with sparse cutout textures. Keep the
existing leaf positions initially; holes in the texture expose the branches.
Avoid removing a random fraction of leaf blocks and showering the colony in drops.
Retain vanilla leaf-distance, persistence and decay behavior through supported
block behavior. Infected logs must support their blighted leaves normally.

The visual result is a mostly dead crown above a trunk still threaded with living
tissue. No resin production, fruit production or automatic branch regrowth comes
from infection alone.

### Tree boundaries

Start from an upright trunk log rooted on eligible ground. Discover a finite
connected log structure and associated foliage in loaded terrain before admitting
the conversion. Leaf contact alone must not carry infection into another tree.
Multiple roots reached by one wave must not create duplicate work on one tree.

Do not claim that vanilla blocks reliably identify a naturally generated tree.
A player-built structure using unstripped trunk logs and foliage can resemble
one. The guide must state the actual eligibility rules. Planks, stripped logs,
wooden mechanisms, block entities and arbitrary building materials are excluded.

Oversized, ambiguous or unloaded structures are left unchanged. Do not silently
infect the first part of a forest-sized connected structure. A rejected tree does
not cancel unrelated eligible ground and flower conversions.

Once admitted, conversion proceeds incrementally. Recheck each target before
replacement; player edits are not overwritten. Persist bounded progress and pause
on unload without loading chunks. Infection does not propagate from the converted
logs as an independent new wave.

First limits, shipped as starting values ([Shipped: tree infection](#shipped-tree-infection)): one tree job
admits at most 128 logs and 512 leaves inside a 17×32×17 envelope. Discovery
stops after 2,048 examined positions. At most 16 jobs exist per dimension, with
server-wide budgets of 128 discovery reads and 16 replacements per tick. These
are starting limits for testing, not measured safe capacity. At capacity, skip
new tree infection and explain the limit; do not add an unbounded retry queue.
Existing substrate-wave limits remain unchanged.

### Infected timber and Living Wood

Use **Infected Log** for harvested infected trunks. It is one material whatever the
tree was. It builds and burns as a log; it makes no planks, and no decorative wood
family is made from it.

Cutting yields infected timber instead of the original log, never both. Axes and
normal block-breaking behavior apply. Replacing a harvested log and infecting it
again cannot multiply wood. Initial infection adds no biomass-value premium over
equivalent ordinary timber.

Keep this material distinct from **Living Wood**, which the existing catalog uses
for cultivated production and biological construction. A later explicit Rooting Gel
treatment can turn one suitable Infected Log into one Living Wood, alongside the
existing ordinary-log treatment route. Its recipe belongs in the Item Catalog
when implemented and names the Infected Log item; the item is in no log item tag, so an ordinary-log recipe never takes it. Passive infection alone does not unlock Arbor Root production,
resin tapping or the Living Wood digestion yield.

This leaves a progression from damaged wild timber to deliberately cultivated
living wood.

## Implementation and demonstration

The implementation must use supported vanilla and NeoForge placement, support,
loot and decay behavior. If a mapping needs engine patches or suppressing ordinary
engine rules, revise that mapping before implementing it.

Add a labeled meadow and woodland exhibit to the default `scripts/demo.sh` world.
Include untreated plants, a player-triggerable substrate wave, varied dead flowers,
dead berry bushes at different source ages and a tree. Let the player harvest the
results. Demonstrate excluded vegetation and
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
