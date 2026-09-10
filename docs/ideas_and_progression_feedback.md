# Living colony design notebook — ideas and progression

Specific designs: [Item Catalog](ITEM_CATALOG.md) owns ingredients and preparation; [Block Catalog](BLOCK_CATALOG.md) owns organs; [Armor Evolution](ARMOR_EVOLUTION.md) owns permanent equipment branches; [Guide Tree](GUIDE_PROGRESSION_TREE.md) owns the chapter dependency map. This notebook explores play and alternatives, not a second competing recipe table.

Raw ingredients build basic bodies; Culture Bowl/Activation Cyst prepare active treatments, mineral organs prepare fusion media, and Fusion Chrysalis makes target-specific consumable grafts. Materials are biologically processed before functional improvement, not merely clicked onto equipment. Ordinary food, fuel, planting stock, dyes and maturity bone meal remain direct supplies.

Version 0.3, updated 2026-09-08. **Working design, not approved implementation scope.**
Owner: etf. Read the [assessment](IDEAS_REVIEW.md) first for highlights, objections
and the reasoning behind substantial changes.

The original 153-line owner submission is preserved verbatim in Git commit
`5b536bc`, at this same path. To compare: `git show 5b536bc:docs/ideas_and_progression_feedback.md`.
The expansion preserves the subjects and intent, not the original wording.
Nothing here imports prototype code, recipes, quantities or names into production.

## 0. How to read and maintain this document

**Owner direction** identifies explicit requests or ideas in the submission/chat.
An owner idea phrased as a question remains a question. **Proposal** is an assistant
design recommendation, even when written concretely to make the experience imaginable.
**Open** marks a consequential choice still needing discussion. All working names,
numbers, organ arrangements and stage assignments are proposals unless explicitly
recorded as a user requirement in [DECISIONS.md](DECISIONS.md).

This notebook leads discussion of *play experience and connected systems*.
[VISION.md](../VISION.md) holds established direction; the earlier
[PROGRESSION_MAP.md](PROGRESSION_MAP.md) remains a reference map, not a competing
approved recipe tree. Where this notebook offers a different approach, discuss it
and record the accepted choice rather than silently merging contradictory rules.
No section is a release promise. “Complete” here means a substantial design
candidate with consequences and alternatives, not a finished implementation spec.

Read in four passes:

- **Experience:** §§1–2, then the Nether/End chapters.
- **Player biology:** armor, tools, weapons, DNA, mutation, food and chemistry.
- **Colony:** substrate, ecology, resources, farming, mining, logistics and organs.
- **Planning:** operations, ownership, failure, integration, catalog and decision queue.

### Navigation

- [Play experience](#1-what-it-feels-like-to-play) · [Progression](#2-progression-changes-in-responsibility-not-just-bigger-machines)
- [Armor](#3-armor-an-organism-with-a-history) · [Tools](#4-tools-field-precision-survives-industrial-automation) · [Weapons/defense](#5-weapons-defense-and-offense-power-with-a-place-and-purpose)
- [Substrate](#6-living-substrate-the-colonys-ground-not-a-universal-spell-carpet) · [Ecology](#7-ecology-vegetation-and-terraforming) · [Biomass](#8-biomass-a-useful-economy-not-a-universal-answer) · [Power](#9-steam-water-lava-and-electricity)
- [DNA collection](#10-dna-acquisition-material-evidence-and-precision) · [DNA bank](#11-dna-bank-and-research-ownership) · [Mutations](#12-mutation-engineering-a-construction-language-for-organisms)
- [Farms](#13-farming-and-husbandry-sustainability-as-an-earned-design) · [Food](#14-food-preparation-identity-and-a-reason-not-to-digest-everything) · [Fishing](#15-fishing-and-aquaculture) · [Brewing](#16-brewing-and-temporary-chemistry) · [Enchantments](#17-enchantments-and-minecraft-xp)
- [Mining](#18-mining-descend-into-a-place-you-can-explore) · [Ore processing](#19-ore-processing-recovery-choices-not-free-multiplication-loops) · [Logistics](#20-tissue-logistics-convenient-connections-interesting-allocation) · [Storage](#21-storage-a-physical-colony-with-useful-access)
- [Automation](#22-automation-and-control-simple-habits-before-programming) · [Multiblocks](#23-multiblock-anatomy-how-the-lego-promise-becomes-real) · [Sail](#24-nutrient-sail-useful-presence-optional-population) · [Helpers](#25-supportive-organisms-small-reliable-jobs-before-general-helpers)
- [Travel](#26-travel-flight-burrowing-and-long-range-expansion) · [Nether](#27-nether-a-thermal-workshop-you-want-to-return-to) · [End](#28-end-survive-arrival-then-establish-a-spatial-workshop) · [Post-End](#29-post-end-play-and-the-fourth-dimension-horizon)
- [Building](#30-building-assistance-and-safe-relocation) · [Operations](#31-operations-monitoring-and-ergonomic-controls) · [Multiplayer](#32-multiplayer-ownership-and-trade) · [Recovery](#33-failure-recovery-and-intentional-danger)
- [Integrations](#34-integrations-and-pack-author-boundaries) · [Story/teaching](#35-discovery-story-and-accessibility) · [Visuals](#36-visual-identity-and-biological-construction)
- [Organ catalog](#37-candidate-organ-catalog-families-not-a-shopping-list) · [Eight gameplay scenarios](#38-connected-scenarios-to-judge-before-making-implementation-tasks) · [Dependency review](#39-dependency-safety-and-scope-review) · [Discussion queue](#40-discussion-queue-and-source-coverage)

### Recent discussion and prototype evidence — 2026-09-08

The latest discussion develops §§3, 8, 18, 26 and 33. Read those sections before
turning the older organ catalog or rank map into tasks. **Tested in the prototype**
does not mean shipped, balanced, approved for a particular tier, or implemented in
this production repository. Evidence is in the sibling prototype's
[downward mining notes](../../InfestusFrontierModV3/docs/DOWNWARD_MINING.md),
[excavation showcase](../../InfestusFrontierModV3/docs/EXCAVATION_SHOWCASE.md) and
[testing record](../../InfestusFrontierModV3/docs/TESTING.md); these relative links
require the sibling checkout. Prototype checkpoint `1e1a9bb` contains the latest
HUD changes. Keep production recipes, names, limits and save formats uncommitted.

| Subject | Evidence / current state | Planning status |
|---|---|---|
| Manual Leaching | Exposed host rock becomes translucent and easy to mine; ores stay unchanged | **Owner explicitly wants this mechanic in the mod**; tier and balance open |
| Three downward approaches | Descending platform, branching root/stair excavation, contained digestion/recovery | Playable experiments, not three approved production machines |
| Surface-to-depth strip | Finite real excavation, output storage and lit climbing access | Owner likes multiple progression options; prefers a narrow one-block strip later |
| Ground-level boundaries and access | Mutated substrate perimeter, luminous stairs/tendons and increased root headroom | Preferred construction language; final geometry open |
| Suit biomass and HUD | Persistent reserve, explicit feeding, full-set display, half-transparent movable/hideable HUD | Display experiment tested; broad activity/repair costs requested for future work |
| Aggressive biomass spills | Discussed only; no flowing spill or rupture test | Owner idea; pressure, containment and failure policy are proposals |
| Burrowing | Ground-only temporary solid passage | Owner-confirmed movement; finite fuel/cooldown, 10-second introductory stopping window; suffocation risk remains |

### Working vocabulary

| Term | One intended meaning | Must not silently mean |
|---|---|---|
| Living Substrate | Player-grown structural/ecological ground supporting the colony | Every liquid or generic “substance” |
| Biomass | Consumable processed biological feedstock | Genetic knowledge, all minerals or universal energy |
| Specimen | A physical sample with a known source and analyzable material | A permanently learned genome |
| Genetic material | Consumable material recovered from specimens | A second copy of research progress |
| Genome coverage | Persistent knowledge of a particular organism's genetic pattern | An inventory stack consumed by crafting |
| Trait | A biological capability suggested by research | Every ability/immunity the source creature has |
| Mutation | A deliberate installed expression of one or more traits | A random stat reroll |
| Practice | Bounded, typed equipment learning from meaningful activity | Unlimited generic Minecraft XP |
| Anatomy | Physical capacity and compatibility of an item/organ | A fuel tank that eventually fits every ability |
| Organ | A useful living machine or component | Necessarily one block, one entity or one new material |
| Colony rank | A readable summary of demonstrated capability | A universal permission to use vanilla Minecraft |
| Native bed | Specialized substrate matured/operated in its home dimension | A portable shortcut that replaces a dimensional base |

Use “substrate” for the owner's “living substances” ground concept. Use “substances”
for biomass, water, steam and other media. “Infection” describes an optional aesthetic
or local transformation, never a hostile autonomous takeover. Provisional helper
names below replace the franchise placeholders in the original notes; no registry
IDs or final naming choices are approved.

## 1. What it feels like to play

**Owner direction:** a powerful, long, biological construction game; not grinding,
not ten fixed machines, not a universal endgame suit.

**Proposal — the first session.** You find ordinary organic ingredients and make a
small culture. The guide shows a sheltered patch beside your existing home; you
do not abandon vanilla equipment or build a laboratory first. A dormant flesh
chestpiece looks folded and unresponsive. You awaken it in a simple fed cradle.
Its seams flex; installing Repair Membrane allows visible mending using real fuel.
Before awakening, the guide warns that empty reserves cause pain until full
symbiosis. The player learns to fill the piece before wearing it, not by a forced
injury demonstration.

You immediately face a small choice: spend the next biomass on the chestpiece's
reserve, extend the substrate to your garden, or save enough to awaken boots.
All are useful. A bone-supported reservoir lets the garden surplus feed the cradle
while you mine. Coming home now means returning to something that sustains your gear.

**Proposal — a middle-game evening.** Your descending borer stops at water.
You walk its lit passage, inspect the obstruction and choose a retaining wall,
a drainage solution or a turn around the pocket. Its real stone fills the spoil
store. Back at the colony, the extractor reports that abundant crude samples
are no longer its bottleneck: resolving one rare trait needs better separation.
You could enlarge the slow line or build a thermal attachment in the Nether.
Your old boots remain good; you extract their learned movement pattern before
growing a more specialized pair.

**Proposal — a late-game project.** The End workshop conditions components for
transit and precision mutation. You want a new remote excavation outpost and an
ocean expedition suit. Building everything at once overloads your thermal supply.
One design uses large reserves and scheduled batches; another grows a second
compact line; a third improves the extraction recipe and moves less freight.
None asks you to click “complete quest” after delivering a million generic items.
When the network works, you can leave it alone and undertake the expedition.

**Play-feel test:** describe an evening at this stage without saying “wait for the
bar,” “repeat the same boss,” or “craft the next casing.” If we cannot, the stage
needs another design pass.

**Open:** tone of hungry equipment, desired campaign length and how soon the first
useful awakened piece should appear. “First session” is a pacing target to test,
not a fixed time promise.

## 2. Progression: changes in responsibility, not just bigger machines

Use SR0–SR9 as the chapter coordinates defined in the Guide Tree. The scenes below illustrate play within each chapter, not additional mandatory rank gates. A player's visible
rank should follow useful achievements and understandable research, not be a
separate bureaucracy. Several routes can meet each capability milestone; teams
should not repeat the same infrastructure ceremony for every member.

| Reference era | A concrete first reward | What the player now designs | A useful proof of readiness |
|---|---|---|---|
| SR0 Contact | Awaken one useful equipment piece | A fed patch and a safe first repair | Make and use the piece; understand its food |
| SR1 Circulation | Automatic refilling and basic harvest support | Shared supply with a protected starter reserve | Two useful services replenish without hand feeding |
| SR2 Directed Mutation | First directed specialist adaptation | Sample sorting, research and a configurable chamber | Resolve a chosen common genome and grow a usable expression |
| SR3 Thermal Colony | Larger downward excavation and thermal processing | A Nether-native nursery plus heat/steam support | Export a useful native product without stranding the outpost |
| SR4 Precision | Better rare-sample recovery and regulated processes | Electrical support, separation and competing service demands | A valuable precision batch survives an input interruption |
| SR5 End Colony | Local anchoring and spatial conditioning | An accessible, defensible End workshop | Produce locally and preserve a safe return method |
| SR6 Distributed colony | Constructed transit and bounded freight | Several autonomous buffered districts | Complete a useful cross-world order through staged operation |
| SR7 Synthesis | Richer organ assemblies and coordinated loadouts | Efficiency, resilience, footprint or expedition readiness | Finish a chosen ambitious build, not one mandatory megafactory |
| SR8–SR9 Horizon | A distinct alien ecology and new boss chapter | Responsive local habitats and combined combat/infrastructure | Establish a foothold, understand and overcome an optional encounter |

**Important revision proposal:** steam and electrical precision give the owner's
resource ideas a stronger role than the earlier map's long list of invented fluids.
Do not retain every old reagent on top of these services by default. Older names
are role descriptions: early precision material maps to Prepared Genetic Suspension; later catalyst service maps to Catalyst Culture; Fold regulatory material maps to Adaptive Gel in the Item Catalog, not additional unnamed currencies.

Three campaign styles illustrate choices without becoming classes:

- A **cultivator** invests in stable renewable supply and efficient extraction,
  tolerating a larger garden and lower peak output.
- An **expedition builder** uses portable reserves and vanilla preparation to
  explore sooner, then brings high-value specimens home for targeted upgrades.
- A **process designer** invests in buffers, automation and precision, reducing
  sample waste while accepting greater construction and commissioning complexity.

All eventually need productive Nether and End sites. None needs every optional
fish, animal, weapon or armor branch. The reason to automate previous work is
that new processes make overlapping demands that hand feeding cannot conveniently
serve—not that the game insists every optional earlier machine be built.

Ranks retain knowledge through death and shutdown. An offline colony is suspended,
not failing an invisible advancement test. Trading and external automation are
legitimate; native environmental conditions and material accounting still matter.

## 3. Armor: an organism with a history

**Owner direction:** weak rotten-flesh gear; awakening; self-repair that can draw
on the wearer; a mutable biomass reserve; usage adaptation; material reinforcement;
limited improvement budget; extractable/reusable counters and XP.

### Lifecycle proposal

1. **Dormant shell.** Cheap, visually incomplete and weak. It is a scaffold for
   something better, not a trap requiring hours of use before it has value.
2. **Awakened symbiont.** A simple cradle, culture and finite food supply activate
   one piece. It gains a modest useful property and a clear metabolic display.
3. **Provisioned companion.** Grow a reservoir or feeder attachment. Stored biomass
   powers repair and installed abilities; returning to colony supply refills it.
4. **Reinforced anatomy.** Fuse real structural material through a disclosed
   process. Bone, mineral or metal can enable alternative support architectures;
   no arbitrary list of new metals is needed.
5. **Directed specialist.** Install researched expressions that solve a chosen job.
6. **Practiced specialist.** Ordinary use improves handling or efficiency within
   that chosen design. A forward fusion preserves relevant learning without exchanging the chosen lineage.
7. **Mature lineage.** Archive compatible learning or grow another specialized piece. New architecture does not erase the relationship with the old equipment.

### Ways to evolve it, and why they are not interchangeable

| Evolution route | What changes | What it cannot buy |
|---|---|---|
| Awakening/maturation | Activates functions and a bounded growth stage | Every genetic trait |
| Meaningful practice | Efficiency, stability, handling of used functions | Unknown genes or unlimited protection |
| Directed DNA mutation | New behavior and alternative physiology | Unlimited compatible anatomy |
| Material fusion | Structural support, durability or a particular interface | Knowledge or free fuel |
| Reservoir growth | Field endurance | Higher instantaneous output or universal specialization |
| Metabolic development | Sustainable output and recovery behavior | A second incompatible body plan |
| Colony-assisted conditioning | Reliable forward growth in a chamber | Passive permanent upgrades from merely standing at home |
| Consumable chemistry | Temporary expedition preparation | A permanent mutation or duplicated potion |
| Curios support organ | A useful adjunct or control function | Unlimited extra suit capacity |
| Memory transfer | Moves earned, compatible learning | Cloned progress or conversion of running into combat mastery |

Use a small fixed set of activity categories, not thousands of event counters.
Movement might distinguish travel, climbing and aquatic work; combat distinguishes
legitimate actions from self-inflicted repair loops. Count successful useful events,
cap practice contributions and show the next benefit. Do not require perfect
anti-AFK surveillance or store every visited coordinate.

### Constraints that keep specialization real

Separate **anatomical space**, **metabolic output** and **fuel reserve**. A larger
tank extends an expedition but does not let regeneration, blink and maximum shielding
run together without a power tradeoff. High-tier frames retain a hard compatibility
ceiling. A full thermal architecture and a full aquatic architecture compete for
physical interfaces; plenty of fuel does not remove that conflict.

A lava-working suit needs more identity than Fire Resistance: stable work, recovery
from heat exposure and integration with thermal organs are possible distinguishing
features. An ocean suit can emphasize breathing, maneuvering, underwater visibility
and accurate mining. Heavy defensive armor can hold a position but sacrifice rapid
traversal. A generalist remains pleasant for ordinary trips but inferior at the
specialists' demanding sustained tasks.

Individual pieces provide local benefits. Full-suit functions require a coherent
attuned set and specified traits, not any four vaguely biological items. Mixed
sets cannot collect several complete-set bonuses. Fusion previews show permanent exclusions before the player commits. Dye, visible player skin, trims where supported,
durability, name and relevant item data remain explicit compatibility requirements;
mutation should not silently wipe cosmetic identity.

### Feeding, learning transfer and loss

**Owner direction:** empty biomass reserves hurt the wearer until full mutual symbiosis. Afterwards, armor no longer causes hunger pain; unpaid biological features stop. This is not an opt-in health converter. Warn before reserves run out; teach refueling and removal without promising safety when already inside rock or lava.

Each armor piece has permanent material and trait branches, its own counters and finite anatomy. Start another piece for an incompatible specialization. Auto-feeding consumes real food, auto-healing spends biomass, and Nutrient Intake transfers real stored biomass. Elytral flight incorporates an actual Elytra. [Armor Evolution](ARMOR_EVOLUTION.md) owns the complete proposed counters, material tree and ability costs; its wearer-versus-piece adaptation representation remains an open proposal.

An archive capsule should transfer typed learning in a chamber. The source loses
the transferred amount in the same operation that the destination gains it.
Copying a blueprint copies the *design*, not its earned practice. Restore learning
only into compatible anatomy; dormant knowledge can wait without supplying an
active bonus. Minecraft XP, if used at all, is a separate disclosed catalyst,
not an exchange rate turning any counter into any ability.

Forward evolution preserves progress and permanent choices; death consequences remain open. Recommend a
recoverable dormant item and optional prepared memory backup over erasing weeks
of practice. Backup recovery must consume/reconcile the archived value, not duplicate
it while the original suit is still usable. Swapping gear must preserve cooldowns.

**Player puzzle:** carry a larger tank and fewer utility organs, or rely on a route
of refill outposts and install richer traversal functions. Both create base/travel
decisions rather than one mathematically dominant armor bar.

**Open:** hunger-pain severity and symbiosis ownership/thresholds; practice versus Minecraft XP semantics; archive
loss/cost; exact enchantment policy. Creature-derived armor grafts require complete matching DNA; host grafts do not.
No illustrative capacity or training threshold is yet balance data.

### Metabolic play

A complete suit displays combined fuel and metabolic output with movable, hideable HUD and half-transparent background. Each piece retains its own real reserve. Low fuel names the abilities that will pause; warning controls are separate from routine HUD visibility.

Pay for useful powered actions and actual repair, not attempted actions or repeated callbacks. Self-inflicted injury and fuel transfer do not create practice. Armor's five counters and maturation are bounded state, not a log of every action. Shared fuel, priorities and early hunger pain follow Armor Evolution. Burrowing interruption may leave a player inside terrain: prepare light, fuel, stability and an exit; there is no guaranteed rescue.

## 4. Tools: field precision survives industrial automation

**Owner direction:** tools evolve through the same biological principles as armor;
large organs should eventually do the bulk job better.

**Proposal:** reuse the equipment lifecycle and persistence rules, but give each
tool a clear working anatomy. A cutting tool favors plants/wood, a boring tool
mineral work, and a sampling instrument controlled collection. They may share a
base chassis or switch selected profiles in a workshop; they should not all become
the same omni-tool with different icons.

Early improvements solve small annoyances: a little self-repair, deliberate specimen
collection or controlled harvesting without trampling a garden. Later alternatives
include precise block selection, careful sample yield, a bounded multi-block cut,
and efficient underwater work. Whole-tree actions must have an explicit selected
extent, output reservation and refusal rules; no recursive forest deletion.

Practice rewards completed work, not placing and breaking the same cheap block
forever. Material fusion supports an appropriate native mining tier. More speed
does not bypass the material requirement, claims, container protection or correct
loot semantics.

At industrial scale, a mining organ wins on sustained throughput and hauling.
The handheld tool still wins for inspecting an unexpected cave, clearing a refused
block, selecting a rare specimen, field repairs and working without a supply line.
The player graduates from doing every stroke to choosing and maintaining the job.

**Example:** take a fast area-cutting tool for a planned service room, or a slower
sample-preserving tool to prepare a rare vein for precision processing. Do not
force the player to swing a special tool once at every ore before automation counts.

**Failure/recovery:** insufficient output capacity pauses a bulk action; no spill.
A disabled biological tool should retain a documented safe baseline or dormant
recovery path. A destroyed tool must not invalidate researched recipes.

**Open:** separate tool families versus a refittable chassis; bounded area-cut
geometry; which ordinary vanilla enchantments coexist with biological functions.

## 5. Weapons, defense and offense: power with a place and purpose

**Owner direction:** evolving weapons and exceptionally powerful base-supported
weapons that require logistics and bringing the enemy within the installation's reach.

**Proposal — portable weapons:** three functional directions are enough to start
discussion. A piercing limb rewards deliberate single-target timing; a flexible
grasping weapon controls space but has lower direct damage; a ranged gland consumes
prepared ammunition and competes for suit metabolism. These are original working
concepts, not approved item names. Boss DNA can unlock an interesting interaction,
not every attack or immunity of that boss.

**Proposal — colony defense:** separate detection, response and supply.

- A lookout senses eligible targets in a bounded defended area.
- A ground organ covers an approach; an aerial organ tracks airborne threats.
- Digestive tissue is an explicitly armed surface, not the default garden floor.
- A reservoir and regulated supply line serve repair, ammunition or discharge.
- A control organ sets priority, authorized targets and safe stand-down.

A thermal launcher might deliver a strong slow shot; an electrical restraint
might briefly interrupt eligible ordinary mobs; a biomass-fed interceptor might
prefer small flying attackers. Treat these as alternative expressions in one or
two organ families, not a commitment to three independent weapon trees.

**The puzzle:** a high-output emplacement needs line of sight, a firing lane and
reliable resupply. Building a fortress around a Nether approach is different from
carrying that power everywhere. A remote hunt might require constructing a small
forward installation or luring an enemy, while a mobile specialist accepts lower
sustained power. Do not spawn surprise raids solely to justify defense.

Attraction should influence a disclosed subset of ordinary mobs, not compel every
boss to stand at a turret. Dragon and Wither interactions require separate explicit
tests; a flying label does not make every attack valid against multipart or
special-purpose entities. Vanilla equipment and allied players remain legitimate
contributors. Avoid secret blanket immunities to other mods.

Damage-derived biomass follows actual successful health loss, not attempted hits.
Immune, invulnerable or already-dead targets yield nothing. Healing, armor repair,
summoning, breeding and conversion loops require a combined economic audit; damage
alone is not proof that a resource loop cannot mint value.

**Combat readability:** one legible biological projectile or discharge, clear
charging/cooldown, understandable range, modest effects and directional sound.
The owner preferred restrained green aerial shots in the prototype; retain that
visual lesson without copying its implementation.

**Open:** PvP opt-in, friendly-fire defaults, allowable boss control, degree of base
damage and whether temporary combat organisms add enough value over fixed organs.

## 6. Living Substrate: the colony's ground, not a universal spell carpet

**Owner direction:** organs grow on substrate; mature ground interacts with armor,
mobs and logistics; growth stays under player control; the owner is reconsidering
a separate fast lane.

**Proposal — three independent properties:** local maturity, chosen functional
expression and physiological attunement. Do not multiply every combination into
a new catalog entry unless its appearance/behavior needs one.

A young patch establishes the colony. Mature patches develop readable structure,
ground cover and veins. Maturity improves supported functions but does not spread
the patch indefinitely. Nutrition sustains active services; starvation pauses work
without turning an established garden into a dead desert.

### Basic slowing versus fast lanes

Recommendation: ordinary substrate may have mild adhesion, neutralized by basic
attuned armor. More developed armor can gain modest home-terrain mobility. Reserve
strong acceleration or directed transit for an optional engineered expression
*if playtests show it creates useful route choices*. This addresses the owner's
wish to avoid an unnecessary block while retaining room for logistics corridors.

Compare two explicit alternatives before choosing:

- **Armor-led mobility:** simple building and a strong feeling of belonging; risks
  making every patch equally optimal for travel and weakening route planning.
- **Tissue-led mobility:** more architectural decisions and public transit options;
  risks cluttering the mutation catalog and forcing repetitive path painting.

Do not implement both at full strength by default. Movement should follow contact
events/cached state, not scan every entity from every substrate block.

### Candidate expressions and boundaries

| Expression | Useful job | Necessary limit or distinction |
|---|---|---|
| Adhesive | Slow an approach | Mild ordinary effect; strong slowing is deliberate |
| Digestive | Damage eligible intruders and recover some feedstock | Armed area, target rules, actual damage, storage back-pressure |
| Conductive/vascular | Route a selected fluid or electrical service | Visible channel, capacity, ports and isolation |
| Item-carrying | Dependable bulk item transport | No permanent moving item entity per parcel |
| Guiding | Move eligible livestock or workers toward a station | Bounded path/area and no involuntary player conveyor by default |
| Restorative | A supplied recovery station | Reserved resource and explicit eligibility, not free global regeneration |
| Luminous | Safe navigation and readable districts | Mostly passive visuals; avoid a ticker per light |
| Lifting | Vertical assistance at an engineered location | Defined exits and safe refusal; the failed custom-fluid lift stays parked |
| Restraining | Brief paralysis/stun in an armed area | Duration/cooldown limits, no permanent boss lockdown |
| Signal tissue | Local sensing/control information | Shared protocol and bounded updates, not per-tick global network messages |

Attraction/repulsion fit an organ with a readable radius better than an invisible
property on every block. Their normal purpose can be animal handling, worker
coordination or perimeter control, not only combat.

**Foundation rule to discuss:** major living organs need compatible substrate or
a permitted living support frame. Simple tools, temporary seedlings and native
inventory adapters need sensible bootstrap exceptions. If “every organ must sit
on substrate” prevents a wall-mounted port or a tall reservoir stalk, the rule
needs structural support semantics, not an arbitrary exception for each model.

**Open:** magnitude of basic adhesion; whether simple partial armor neutralizes it;
visitor/public-corridor settings; functional expression per face versus per cell.
Full-suit high-tier abilities and colony access permission remain separate.

## 7. Ecology, vegetation and terraforming

**Owner direction:** the grown area should remain varied, alive and functionally
useful. Grass, bushes and trees change; exact tree behavior is explicitly undecided.

**Proposal:** let the player choose a treatment rather than have every ground
placement silently process the canopy. Preserve three visible layers: ground cover,
undergrowth and substantial trunks/canopy. Functional options might be:

- **Coexist:** keep the original plant with a small local symbiotic role.
- **Convert:** deliberately establish a specialized living form.
- **Harvest/assimilate:** consume an explicitly selected mature plant for recoverable
  matter and samples, with appropriate replanting or restoration options.

For trees, compare a managed sap/wood producer, a membrane-canopy support tree and
a one-time biomass reclamation treatment. A tree should not automatically provide
all three at maximum efficiency. A compact non-tree organ can substitute for some
services at a different material/space cost, so builders are not forced into a
single approved forest layout. These are proposals for the promised tree discussion.

Conversion may recover some seeds/wood once, reserved into storage or a bounded
player result. It cannot repeatedly reclassify the same trunk for free output.
Planting and harvesting new trees remains a material/time process. Do not turn
leaf decay into a large incidental item storm.

Terraforming begins as deliberate, previewed editing: prepare foundation strips,
retain slopes, carve service entrances or regrow controlled ground cover. Larger
jobs use supplied construction organs and declared regions. No passive biome-wide
rewrite, automatic ore generation or hostile outward spread.

**Player puzzle:** a terraced canopy garden provides certain support services and
leaves room for paths but limits dense construction; a compact cultivated court
uses specialized exchange organs and more supply. Both can be productive and
attractive. “Plant one of every tree in a checkerboard” is not the intended ecology.

**Failure/recovery:** an interrupted transformation stops at committed cells;
ownership, remaining inputs and selected extent persist. Removing substrate does
not silently delete neighboring player trees or inventories.

**Open:** actual roles of each vegetation type, regrowth rate, maximum selectable
tree extent, treatment consent, and whether dormant converted wood is a building
material with its own recipe uses.

## 8. Biomass: a useful economy, not a universal answer

**Owner direction:** biomass is the early resource and feeds substrate, organs
and equipment. Later progression introduces distinct services/materials without
adding an excessive number of new ingredients.

**Proposal:** keep bulk biomass a legible common resource. Separate raw organic
inputs from processed biomass so harvesting, sorting and digestion have purpose.
Recipe-defined inputs and yields can support plant, animal and salvage economies;
there is no assertion that every biological-looking item converts equally.

Early manual digestion is forgiving. Automation exposes input rate, conversion
loss, waste handling where meaningful, and a reserve policy. A routine resting
colony should have very low or zero consumption for decorative tissue; active
work, repair and deliberate growth cost material. No creeping starvation tax on
every block the player has ever placed.

Give food, seeds, research samples and construction stock separate claims on farm
output. The digester should not automatically eat the last seed or the expedition
meal. A reserve rule is more interesting than inventing three almost identical
grades of slurry just to add recipes.

**Illustrative budgeting exercise, not tuning:** a garden supplies 12 units of
usable organic input per cycle. Replanting/feed requires 3, normal services 4,
and emergency reserve replenishment 1. That leaves 4 for growth or industrial
demand. Adding a precision line that consumes 6 creates a real choice: improve
yield, expand, alternate batches or reduce another demand. All values are
normalized thought-experiment units; actual crops and time must be measured.

A closed recycling chain must not create net feedstock or work from its own outputs.
A self-sustaining farm is different: growing organisms use explicit environmental
inputs and time. Credit that external contribution instead of describing it as
an impossible lossless closed loop.

**Storage baseline:** visible fill, deliberate priorities, bounded ports, clear
overflow refusal and portable safe handling. The hazardous-mode proposal below
does not yet replace ordinary safe refusal. Existing prototype buckets/bottles
are reference experiments, not production promises. Decide a consistent manual
container early so players can bootstrap without a network.

**Open:** raw-input families; whether any refined nutrient grade is necessary;
manual container sizes; reserve defaults; treatment of food from other mods.

### New direction to explore: biomass as a hazardous living fluid

**Owner idea, 2026-09-08:** biomass could physically flow and be aggressive when
spilled. Excess production with no remaining storage might burst a reservoir and
release biomass, making overflow disposal and mitigation a base-design puzzle.
The owner suggested a random storage failure as one possibility. This is not yet
an approved rule, and no rupture/spill system was tested in the latest experiments.

**Assistant recommendation, not ratified:** make the incident causal and readable.
A particular overloaded organ swells and leaks before rupturing; an unrelated tank
should not fail arbitrarily because some other district filled up. Separate
**capacity** from **pressure/activity**: a full idle reservoir can be safe, while a
still-metabolizing high-output culture may need space for its residual production.
Pressure is one candidate gameplay state, not a commitment to a hydraulic simulator.

An early safe producer can stop at a full output. A later efficient culture could
require a controlled shutdown interval. The player then chooses among:

- extra reserve capacity sized for the unfinished batch;
- priority overflow routing into another useful consumer;
- a relief organ that sacrifices material to stop escalation;
- emergency solidification or neutralization;
- a sacrificial basin, lined channel or isolated storage district containing a leak.

These are candidate components, not five mandatory machines. A passive safety
solution should remain viable; a beginner must not need sophisticated control
logic merely to leave their first base safely. More efficient dangerous processes
must earn their extra complexity through a real benefit.

**Playable scene:** a Nether culture is finishing a high-yield batch when the
receiving workshop stops. The reservoir becomes visibly tense. Your buffer absorbs
some output; a relief organ routes the rest into a protected neutralization bed.
You lose some yield but keep the base. Another player avoids the loss with larger
storage and scheduling; a third keeps a secondary process ready to consume overflow.
No single layout wins on space, efficiency, construction cost and fault tolerance.

**Safety contract to specify before coding:** a released quantity comes out of
stored/in-flight material exactly once; it cannot make infinite source blocks or
grow by digesting itself. Bound active spill cells, work per tick across all spills,
lifetime, affected area, reactions and saved state. Loaded chunks only, no hidden
catch-up explosion on restart, no uncontrolled item/XP drops or cascading reactions.
Decide whether the spill attacks entities, natural terrain, constructed blocks or
some explicit combination. Claims and ownership must constrain damage; a bio suit
does not give permission to harm someone else's base.

**Open:** is all biomass physically hazardous, or only an active/refined state?
What neutralizes it; can it be reclaimed; how much may be lost; what happens at a
chunk border or when a reservoir splits? Does warning give time to act without
requiring constant attendance? Is failure deterministic, or is there disclosed
local risk after clear thresholds? Containment acceptance cases live in §33.

## 9. Steam, water, lava and electricity

**Owner direction:** investigate biomass → steam → electricity as increasing
capability, with water/lava handling and eventual external energy integration.

**Proposal:** this is a progression of available *services*, not replacement fuels.
Advanced organisms still need living feedstock. Heat can drive bulk work; steam
can carry a contained pressure cycle; electricity supports precise separation,
sensing and controlled activation. Avoid making every machine consume all services.

An early thermal sac accepts a declared heat input and water into a sealed process.
A diaphragm exchanges that contained work with a chamber or borer. A condenser
recovers some water and heat into explicit local outputs. A later electrocyte
assembly converts a disclosed biological/thermal input into charge, with losses.
None of these descriptions is a claim about real industrial physiology.

Two steam layouts illustrate the game: a large low-pressure buffer supports steady
excavation, while a compact pulsed system serves intermittent work with stricter
release/recovery timing. More pressure should enable a different operating envelope,
not be a hidden multiplier attached to the newest casing. A safety interlock and
recovery path must be obtainable before the first hazardous setting.

For the Nether, ordinary exposed-water behavior cannot be wished away. Choose a
sealed imported-water circuit, a dry first-stage heat process, or both. Recommend
dry bootstrap and sealed water-based optimization, so commissioning does not require
an impossible local water farm. Lava is a legitimate local input; whether it is
consumed, circulated or used through a heat interface needs explicit accounting.

Electricity makes advanced extraction more selective, not intrinsically omnipotent.
A capacitor-like living sac handles bursts; a stable feed supports precise work.
The player may choose slower tolerant operation or faster operation needing a more
stable supply. Do not simulate electromagnetic fields or pressure in every vein.
Use bounded machine/network state with readable units and operating profiles.

**Integration choice:** an FE adapter could import/export a defined electrical
service. External power must not satisfy native-bed requirements, fabricate genetic
material or bypass separation anatomy. Unrestricted imported power would invalidate
an “own generator mandatory” gate; choose whether the gate tests process capability
or a modpack-enforced internal economy. We cannot honestly promise both simultaneously.

**Open:** electricity's first indispensable use, steam representation as fluid versus
bounded service, FE conversion policy, pressure hazards and whether one additional
advanced reagent earns a genuinely different role. No conversion ratios are approved.

## 10. DNA acquisition: material, evidence and precision

**Owner direction:** collect substantial samples of organisms; use mob/plant drops;
track amount and variety; reconstruct complete genomes; improved extractors waste
less; investigate live, possibly damage-based sampling.

### A proposed five-step research loop

1. **Identify.** A field interaction or compatible drop identifies a candidate
   organism and a useful trait hypothesis. Tell the player what it might enable.
2. **Collect.** Gather a finite physical specimen with a declared source and quality.
   Plant harvest, animal byproduct, remains and live sampling are alternative routes.
3. **Prepare.** Separate suitable material from bulk matter; preserve rare evidence
   instead of letting the general digester consume it.
4. **Resolve.** An extractor converts the specimen into some recoverable genetic
   material and some new coverage of that organism's pattern, within its precision.
5. **Verify.** Complete enough evidence to obtain a reliable template. The bank
   keeps the knowledge; growing an expression still consumes material and services.

Preparation and verification may be modes/attachments of one chamber early on,
not four compulsory new blocks. Add distinct organs only when they create a useful
layout or scheduling decision.

### Quantities that should be visible

**Amount** says how much physical material is available. **Coverage** says how much
of this genome is understood. **Precision** belongs to the extraction process.
**Variety** describes useful differences in evidence, not an unlimited currency.
The UI should say “many repeats; separation precision is the limit,” not consume
another stack while silently making zero progress.

Use a small authored set of evidence categories per research target, with finite
completion. Repeated legitimate material contributes where it remains informative;
different source routes can reduce repetition or unlock a particular missing trait.
Do not require a unique UUID, biome tour or real-world date for every specimen.
A common farm can supply volume, while a prepared expedition or improved instrument
supplies information that the farm's current sample route cannot resolve.

There are two reasonable coverage models. **Deterministic coverage** maps specimen
categories and instrument capability to progress; it is readable and avoids rerolls.
**Bounded stochastic coverage** makes repeated batches uncertain but must guarantee
eventual progress and show expected outcomes. Recommend deterministic foundations
first; surprise should come from discoveries and combinations, not wasted rare loot.

### Bosses and hard-to-replace specimens

The owner's Wither example demonstrates the intended difference between crude and
precise extraction, but hundreds of Nether Stars conflicts with the anti-grind goal.
Recommendation: crude instruments reveal a limited useful feature and explain
their precision ceiling; rare unresolved samples remain preservable. Advanced
preparation can recover more information from the same kind of valuable material.

Possible alternatives include bounded encounter observation, a collected residue,
a completed encounter plus precise analysis, or a carefully contained live sample.
These are alternatives to discuss, not newly approved boss drops. Completion cannot
depend on owning a unique dragon egg or farming a boss indefinitely. A traded
legitimate specimen should have a clear policy rather than be mysteriously invalid.

### Live sampling without an infinite creature battery

A sampling organ reserves output, identifies an eligible ordinary organism and
performs a bounded operation. If it harms the target, successful damage is one
condition, not the entire economic proof. Healing costs, feeding, sample recovery
and processing must be audited together. Sampling must not emit a creature's full
death loot table on each pulse or multiply a Nether Star through repeated injury.

Options are a finite per-target sampling reserve with food/time recovery, a process
that consumes a real renewable byproduct, or nonlethal observation that earns
knowledge but not significant bulk material. Use a fixed small amount of entity
state, not a world-wide history of every creature ever sampled. A live-sampling
fixture must handle unloading, death, transfer, copied data and multiple samplers.

**Open:** completion model; how partial knowledge is useful; amount recovered as
material; source attribution for shared drops; whether live sampling is required
or an optional route. A bone can have several vanilla sources: species-specific
research must not pretend an ambiguous inventory stack proves its origin.

## 11. DNA bank and research ownership

**Owner direction:** a bank for captured DNA, enabling mutation and preservation.

**Proposal:** the bank presents three deliberately separate views: a specimen inbox,
a finite genome atlas, and available consumable genetic stock. The atlas is knowledge,
not an enormous inventory of one item per discovered fact. Finishing a genome
does not erase its knowledge when a mutation spends material.

A species page shows known traits, unresolved categories, suitable instruments,
sample sources and likely next uses. A mutation preview can link back to the exact
missing evidence. Completed common genomes should remain useful across equipment
and organs rather than become decorative checkmarks.

Storage grades improve work organization and bounded capacity, not turn a database
into a million ticking blocks. A bank might expose a limited active research queue
and attach specimen storage through ordinary ports. Duplicate samples can become
material or be refused under a configured rule; the bank should not hoard useless
stacks forever.

**Learning versus commerce:** recommend shared colony research under explicit team
permissions, with personal gear practice remaining personal/transferable. A sample
or authorized template can be traded. Whether importing a full template grants
research, only production access, or a shorter verification task is an important
economy decision; no server-global unlock is assumed.

**Example:** one player explores oceans while another refines thermal samples.
They share an atlas and improve different suits without each repeating the other's
expedition. A solo player can complete those paths sequentially. Leaving the team
has a defined treatment of earned knowledge and shared specimens before code exists.

**Failure/recovery:** dismantling a machine must not accidentally delete the only
copy of a team's knowledge. Choose a persistent knowledge owner and explicit export/
backup rules; physical specimen buffers still conserve material. Do not imply
unlimited portable research duplication unless that is the accepted sharing policy.

**Open:** player/team/world ownership; access control; transferable templates;
completion credit for helpers; maximum active queue and supported genome catalog.

## 12. Mutation engineering: a construction language for organisms

**Owner direction:** combine captured DNA, use and materials to evolve equipment
and organs in several directions within a meaningful budget.

**Proposal:** separate a researched trait from its expression. A locomotion trait
could become efficient boots, a gripping worker limb or a transport-organ lining.
Those consume different materials and occupy different anatomy. One genome is
useful in several systems without supplying the same bonus everywhere.

A mutation plan specifies target anatomy, required knowledge, material stock,
process services, installed expression and incompatibilities. The chamber shows
before/after comparisons, lost functions, retained practice, peak consumption,
and unavailable requirements. Start commits a bounded job; pause/refusal never
rerolls the result or duplicates its inputs.

Grow assemblies around competing physical functions: a thicker protection layer
uses exchange area; a larger reservoir consumes room; a selective membrane reduces
throughput but saves rare material. A stabilizing attachment might permit a
demanding expression at the cost of productive chamber volume. Several layouts
can meet the requirement, not one hidden “perfect” configuration.

Propose deterministic mutation plans for core progression. Optional experimentation
could discover *new known alternatives* through controlled tests, with previewed
cost and finite outcomes, but not random permanently ruined armor. A failed process
should teach an understandable condition. Experiment logs are capped summaries,
not endless per-tick histories.

**Example:** a thermal suit uses a structural heat-tolerant shell plus an exchange
lining. An alternative uses lower protection and faster recovery, needing more
regular replenishment. A researched regulation trait can make switching deliberate
modes easier; it must not activate incompatible modes simultaneously.

### Worked expressions to make the genetic language concrete

These are **authored game proposals**, not claims that extracting Minecraft DNA
automatically reproduces an entity's native behavior. The named organisms are
candidate research sources; alternatives should exist where a required source
would make a route excessively rare or unavailable in a pack.

| Intended build | Candidate evidence + construction | What the player gains | What competes with it |
|---|---|---|---|
| Cavern explorer boots | Spider-related grip + reinforced contact surfaces + movement practice | Controlled climbing/secure footing on eligible terrain | Fast flat-ground travel or heavy bracing; not unrestricted wall phasing |
| Thermal working suit | Magma-cube-related heat expression + Nether-grown exchange lining | Sustained hot-work endurance with a visible reserve | Aquatic exchange area and agile all-purpose armor |
| Pelagic survey suit | Aquatic-organism evidence + selective membranes + swim practice | Breathing/work or agile swimming, according to anatomy | Heavy armor and thermal endurance; not both aquatic maxima for free |
| Short-displacement harness | Complete enderman-related research + End-conditioned support | Deliberate short movement to a validated endpoint | Continuous shielding/output and reservoir space; cannot bypass claims |
| Precision sampler | Structural reinforcement + selected collection expression + actual sampling practice | More useful preparation from eligible specimens | High-speed bulk break mode and broad destructive area |
| Efficient nursery lining | Relevant plant research + exchange membrane + cultivation profile | Better use of a declared feed stream | Peak throughput, footprint or flexibility across unrelated crops |

For example, the spider-derived expression can be shared knowledge while two
players build different boots: one prioritizes climbing with a small field reserve;
another combines modest grip with a bulky work brace. Running practice can refine
movement economy, but does not create enderman research. A thermal organ might use
a related heat trait through a completely different expression than armor.

Not everything in the world needs fictional DNA. Organisms supply biological
patterns; stone/metal are analyzed as materials used for reinforcement or process
chemistry. A machine or mixed-origin vanilla drop is not automatically a species.
This distinction keeps “sample everything useful” broad without emptying the word
genome of meaning.

**Open:** exact anatomy dimensions, mutation reversibility/cost, organ genetics
versus equipment genetics, authored combinations versus player-composed expressions,
and whether discoveries have any controlled randomness.

## 13. Farming and husbandry: sustainability as an earned design

**Owner direction:** meaningful plant/animal farming puzzles; sustainable operation
should require thought, not simply placing an all-purpose farming block.

**Proposal:** introduce one additional design constraint at a time. Early farming
uses familiar crops, water and ordinary harvesting. A nursery or rooted tender
adds reliable replanting and measured extraction. Later choices concern seed/feed
reserves, growth support, harvesting rhythm, mixed outputs and competing uses.
Do not simulate a real ecology in every soil cell.

Three plant layouts can be viable:

- **Field system:** larger footprint, simple support, forgiving stock and predictable
  material flow. Good for dependable bulk input.
- **Intensive beds:** smaller footprint and higher demand on prepared inputs/support;
  attractive near workshops but less forgiving of interruption.
- **Mixed habitat:** a few designed complementary functions reduce some purchased
  inputs, with more space/routing and lower peak specialization. Avoid a universal
  “one of every species” diversity bonus.

Animal systems need explicit pen registration, feed reserves, adult/young handling,
population cap and harvest/byproduct policy. A guiding organ can move eligible
animals to a station while leaving a service route for the player. Breeding stops
at capacity. No worker repeatedly searches all nearby mobs or fills the world
with babies because an output chest is full.

Offer **byproduct-first husbandry** and **controlled harvest** as distinct routes
where vanilla resources permit them. Do not force hostile-mob slaughter for every
biological technology. Nonlethal routes may cost more habitat/support or have lower
bulk yield. They should remain a viable campaign style, not a moral trap or a
secretly inferior path in every metric.

**A concrete farm problem:** the nursery, kitchen and digester all want the same
harvest. Give replanting first reserve, meals a target stock and digestion the
surplus. A bad layout starves the next crop cycle; a good one refills itself and
serves the colony. Increase industrial demand and compare a larger field, more
efficient digestion and scheduled mining. Sustainability is possible; expansion
is what reopens the puzzle.

**Failure/recovery:** full stores stop harvest without destroying crops; feed
shortage stops breeding before creating an unmanageable herd. Living decoration
survives routine shutdown. The emergency restart path uses common seed/feed and
low-tech manual interaction already available at that stage.

**Open:** crop mutation inheritance, degree of habitat synergy, direct organ work
versus optional drones, and whether seasonal mechanics add enough value. No
seasons, random disease or global soil depletion are assumed.

## 14. Food: preparation, identity and a reason not to digest everything

**Owner direction:** food production belongs in the mod.

**Proposal:** distinguish player food, equipment feed and process material. Biomass
need not automatically be an appetizing meal. A kitchen organ transforms actual
edible inputs using heat/fermentation-like profiles and ordinary recipes, retaining
clear outputs and familiar food behavior.

Give food a small number of useful roles: ordinary nourishment, expedition-friendly
provisions and an optional temporary biological conditioning effect. A meal might
help recovery or suit-feed efficiency for a finite period; it does not permanently
raise anatomy or replace an aquatic breathing apparatus. Good ordinary Minecraft
food remains useful. Avoid a mandatory nutrition dashboard with six minerals.

A kitchen can choose batch efficiency or flexible small orders. The player reserves
meals before sending surplus to digestion, and configures portable expedition stock
without throwing leftovers into the world. A sealed ration is convenient because
of its recipe/handling, not because we introduced arbitrary offline food rot.

**Integration opportunity:** accept tagged ingredients and expose recipes to food
mods instead of recreating their entire cuisine. Decorative cooking stations can
look like warm sacs, suspended trays and living shelves with mechanical accents.

**Open:** acceptable culinary tone, whether any special food effects are needed,
modded-food conversion rules and desired relationship with potion preparation.

## 15. Fishing and aquaculture

**Owner direction:** fishing should be represented, not lost inside generic farming.

**Proposal:** provide field fishing/sampling and a bounded cultivated-water route.
A landing organ works on a selected water habitat with clear bait, output and
eligibility conditions. An aquaculture nursery grows a known stock from real
starters/feed and time. It does not summon arbitrary treasure because a machine
touches one water block.

Different designs emphasize ordinary food yield, selected aquatic specimens or
specialized membrane inputs. A coastal installation has more habitat space and
travel exposure; an inland managed pool pays additional support/material cost.
Keep the early route possible without already owning the ocean suit its samples
help develop.

Harvest either interacts with a declared bounded population or resolves an explicit
cultivation recipe. Do not spawn hundreds of fish as internal process tokens.
Distinguish actual captured fish, cultured output and vanilla fishing treasure;
automating one does not automatically grant all three loot categories.

**Open:** real-entity husbandry versus abstract contained cultivation, required
habitat dimensions, renewable feed and whether open-ocean geography is advantageous
or required for any optional specialization.

## 16. Brewing and temporary chemistry

**Owner direction:** potion/brewing integration should offer meaningful opportunities.

**Proposal:** brewing remains the source of temporary chemical capabilities, while
DNA supplies stable anatomical options. A biological still can prepare batches,
store a finite compatible dose or route a treatment to equipment. It consumes actual
ingredients/potions and preserves bottle/container accounting.

Consider a chamber attachment with alternative preparation profiles: longer delivery
at reduced intensity where an effect supports it; a short concentrated burst; or
efficient batch handling with lower flexibility. Do not claim all mod effects can
be scaled safely. Unknown effects use conservative ordinary delivery or are refused
with a readable reason until an adapter declares support.

Suit infusion adds convenient activation, not an infinite potion from one bottle.
Installed anatomy, temporary potion effects and Curios utilities need a combined
activation budget. A thermal specialist should still have useful work capabilities
when another player drinks Fire Resistance. Do not nerf ordinary vanilla potions
just to rescue an indistinguishable armor design.

**Player puzzle:** prepare several single-use emergency doses for a flexible trip,
or install a delivery organ and devote suit capacity to sustained use. A base
treatment station is economical locally but cannot follow the expedition for free.

**Open:** supported initial effects, dose storage, interaction priority, cleansing
and negative-effect handling. Never turn a harmful mixture into an unexplained
permanent equipment mutation.

## 17. Enchantments and Minecraft XP

**Owner direction:** enchantments are liked, but bio-armor support is explicitly
undecided. Do not remove them from the mod or vanilla through this document.

Compare three policies with actual builds: ordinary support, curated compatibility,
or non-enchantable biological armor. Recommendation remains testing ordinary support
first against specialist constraints, then using explicit targeted rules if needed.
The most isolated balance system is not automatically the best Minecraft experience.

Evaluate interactions by function: Mending and biological repair; Unbreaking and
maintenance demand; Protection with physiological defenses; mobility enchantments
with tissue/suit movement; Fortune/Silk Touch with tool mutation and ore processing.
An additive-looking feature may multiply across these systems. Document the intended
result before implementation rather than patching every strong combination afterward.

Separate typed practice from vanilla XP. If the owner's transferable “XP” means
equipment learning, call it that in the UI. If Minecraft XP is also extractable,
define its source, destination, losses and interaction with existing XP stores.
No conversion of cheap repair loops into unlimited knowledge or transferable combat
practice. This choice affects other mods and therefore needs explicit approval.

**Open:** armor policy; separate tool/weapon policy; imported enchanted gear; anvil,
grindstone and repair behavior; whether any recipe consumes ordinary XP. Existing
names, dyes, trims and allowed enchantments must survive unrelated mutations.

## 18. Mining: descend into a place you can explore

**Owner direction:** real resources come from actual terrain; progress from manual
mining to major excavation. Downward mining is primary. Tunnels or shafts must be
traversable; the exact descent method remains open. The horizontal prototype is
evidence of a mechanism, not a sufficient production design.

**Proposal — three scales:**

1. **Field prospecting and directed hand work.** Improved tools and a modest survey
   instrument help choose a site. Survey results concern a bounded inspected region,
   not an omniscient ore list for the dimension.
2. **Exploration excavation.** A small fed organ prepares a safe walkable descending
   route, collecting real blocks. The reward is access plus resources. Direction,
   stopping depth, extent and spoil destination are chosen before activation.
3. **Industrial excavation.** A larger configurable organ builds galleries, connected
   descents or a shaft with an access structure, backed by thermal services and
   bulk hauling. It handles more sustained work, not infinite world scanning.

Descent candidates: a stepped ramp with landings, a spiral around a central service
void, or a straight shaft with deliberately supplied access. Keep all three in
discussion; **do not silently pick a staircase pitch now**. Any proposed shaft
must include how the player gets down and back up using available equipment, and
how a power failure leaves a nonfatal return route. An empty vertical hole is not
the requested experience.

The player previews the job, reserves output and supplies any lining, lights,
steps or access parts. Secreted tissue may be funded by biomass, but wood, metal
or minerals are not fabricated from an empty input. Excavation pauses at fluids,
unsafe structures, protected areas, full spoil stores and unloaded boundaries.
Later specialized drainage/shoring is an optional controlled process, not permission
for early machines to flood caves or drop falling blocks unchecked.

**A useful interruption:** the borer exposes a cavern. You walk to the face, explore
it, gather a specimen and choose whether to bridge, detour, secure or end the job.
Do not demand a manual click on every ordinary ore, but don't automatically swallow
containers, structures or dangerous surprises either. A preview should show what
will remain as floors/walls and how the result joins earlier passages.

Finite jobs, native mining tiers, correct loot, no loose automated output and
bounded committed edits are mandatory engineering constraints. Relocation preserves
stored resources but resets/revalidates the selected job. Removing a controller
does not leave an invisible worker destroying terrain elsewhere.

**Open:** descent geometry, turns/branch planning, survey disclosure, allowed natural
terrain, hazard policies, lining costs and reclaiming obsolete excavation organs.

### What the downward experiments taught us

The owner asked for fundamentally different methods, not three tunnel sizes.
The prototype now provides a comparison vocabulary:

| Approach / working name | What the player does and receives | Distinctive design question |
|---|---|---|
| Descending Cradle | Supply a moving excavation deck; descend with the work and keep climbing access | How do the moving worksite, rider safety and hauling cooperate? |
| Excavating Rootstock | Authorize finite branching descents; walk the luminous stair route | Which branch/site should receive the next section and supply? |
| Digestive Crucible | Digest a bounded batch into captured material, then recover it | How much material can be held safely, and when should a batch be recovered? |
| Strata Maw / deep strip | Open a long surface footprint downward and retain a climbing spine | How do terrain impact, depth, spoil capacity and supply constrain a large site? |
| Manual Leaching | Weaken visible host rock, inspect through it, then hand-mine | Where is selective preparation more useful than bulk excavation? |

The first three were the distinct downward experiments; strip mining and manual
leaching are additions, not replacements. Captured digestate in the crucible is
currently **solid, non-flowing** material—not proof that aggressive liquid spills
work. Local functional/capture tests do not certify multiplayer rider prediction,
all claim mods, maximum-depth soaks or many loaded mines.

**Owner feedback to preserve:**

- Multiple methods should coexist according to player progression, rather than
  selecting one winner for the whole campaign. Exact levels remain open.
- Mutate exposed Living Substrate **at ground level** to define an area. Raised
  border blocks feel less seamless. Do not require targeting hidden blocks.
- Excavation should introduce light. Angled descents need stairs and enough
  headroom to walk comfortably, including beneath the entry boundary.
- Finite, one-shot structures are acceptable: another site can be built to go
  farther. Automatic endless extension is not a requirement. Preserve useful
  access from earlier jobs; restarting cannot erase or duplicate their output.
- The preferred long-strip form is **one block wide**, beginning with the liked
  ladder-like climbing tendon. The owner explicitly deferred changing the existing
  showcase: its current 5×24 width/length is evidence, **not** the desired final width.
  How narrow-strip access remains comfortable needs a later geometry test.

### Manual Leaching is a keeper, not just a disposable experiment

**Explicit owner selection, 2026-09-08:** keep the mechanic that dissolves/weakens
non-ore rock for the actual mod. Its distinctive contribution is preparing terrain
for the player's tools while leaving valuable deposits intact. This is a selected
feature concept, not approval of an exact recipe, area, tier or implementation.

**What was actually tested:** a Leaching Nodule treats up to a clicked-face-aligned
3×3 plane of six supported host-rock types. Rock becomes translucent/porous but
keeps solid collision until mined. Ores and unknown materials are untouched. A
correct pickaxe removes treated host rock instantly with no ordinary abrasion and
salvages its host material; ores retain normal tool tier, time and durability.
The demonstration works on walls, floors or ceilings, not just horizontal ground.
Its visual perimeter is a label/fixture, not a required leaching multiblock.
Nearby unsafe geology, protection or unloaded targets can refuse treatment.

This is **not** a volumetric ore scanner, automated ore lift, fluid solvent or
device that removes all surrounding stone by itself. The earlier idea of moving
ores upward into a randomized collection column was discussed but not implemented.
Do not silently substitute that much more automatic mechanic for manual leaching.

**Proposed play loop:** choose an exposed face, spend a reagent to prepare it,
read the nearby geology through the weakened material, remove the host rock with
your tool, and extract the revealed ore normally. Carry it for selective work
even after acquiring bulk excavators. A prepared cavern wall or precise sampling
site can justify hand work without forcing the player to hand-mine an industrial pit.

The prototype's instant/zero-wear host removal is deliberately strong. Balance
can involve reagent production, eligible geology, treatment extent or slower
breaking, but must preserve the satisfying reveal-and-extract experience. More
repeated clicks or waiting are not automatically better balance. Test survival
tool use, salvage, ore visibility and return trips—not Creative breaking alone.

**Candidate progression, not fixed ranks:** manual leaching can provide an early
useful expedition tool; modest finite descents can establish the first mine;
branching routes, moving work platforms or batch recovery can introduce different
mid-game planning roles; large/deep strip jobs can demand sustained supply and
automated spoil storage later. Not every player must build every method in order.
An advanced reagent or optional suit synergy could extend manual leaching's niche
without granting free ore, universal wall passage or bypassing mining tiers.

**Next design questions:** settle leaching's first acquisition and cost; choose
which automated family joins it first; define light/access guarantees and cave
interruptions; compare relocation versus new one-shot organs; test the narrow-strip
entry before selecting its geometry. Production caps must follow performance
review rather than copying the prototype's depth presets.

## 19. Ore processing: recovery choices, not free multiplication loops

**Owner direction:** ore processing should connect to real mining and automation.

**Proposal:** begin with useful biological sorting/preparation compatible with
ordinary smelting. Later processing separates material more efficiently using
heat, water and precise membranes. A concentrated ore sample remains materially
related to its input; no organ invents a mineral deposit from location alone.

Three profiles can justify different layouts: **fast bulk treatment** with modest
recovery; **careful separation** with better recovery and more support; **selective
byproduct recovery** for an actually declared constituent or recipe output. Do not
invent an independent processing tier for every new ingot or require all profiles.

**Example:** a mine supplies mixed stone and ore. One design sorts near the face,
sending only valuable material uphill; another uses cheap bulk transport and
central sorting. A precision refinery improves yield but generates a secondary
stream that needs reserved capacity. Closing that stream pauses processing before
consuming a rare ore, rather than dumping sludge across the floor.

Any increase above vanilla output needs explicit recipe accounting and comparison
with Fortune, Silk Touch and external ore multipliers. Processed outputs must not
re-enter as fresh ore at a profit. Tags describe compatible materials; they do not
automatically make every recipe cycle safe. If byproducts add only disposal chores,
remove them or give them a useful bounded sink available at the same tier.

**Open:** initial metals, raw-ore versus block processing, yield/support tradeoffs,
smelting integration and whether one specialized mineral binder is enough to support
several armor/organ reinforcement recipes.

## 20. Tissue logistics: convenient connections, interesting allocation

**Owner direction:** veins belong inside substrate, work in 3D, join/split/cross,
look biological and expose their state. An optional growable connection organ can
support a reservoir. The owner is weighing drones against item veins.

**Proposal:** use tissue as the dependable backbone for fluids and ordinary bulk
items. Reserve workers for jobs with an actual spatial task. Convenience should
remove placement/configuration friction, not remove choices about capacity,
priority, separation and where processing lives.

There are three different physical situations: a straight embedded channel, a
junction that deliberately joins routes, and a crossing that keeps them isolated.
Mere adjacency should not always mix networks. A visible bridge/graft or selected
port state distinguishes contact from connection. Natural hills require a tool
that can graft across the exposed step without asking the player to click hidden
blocks. A vertical vein needs readable entry/exit on its exposed faces.

An early route can have one resource role and simple direction. Later regulating
organs allocate destinations, reserves and priorities. If fluid types share a
carrier, incompatible mixtures are refused or explicitly processed; do not silently
overwrite the old contents. Electrical and control tissue are distinct interfaces,
not another arbitrary liquid color. Preserve the green biomass convention, with
shape/pulse cues so color is not the only identifier.

### The reservoir connection organ

The owner's source → connection organ → consumer arrangement remains a strong
physical concept. The organ can accept, distribute and buffer, while a mounted
reservoir supplies capacity. Bidirectional ability is useful, but each transfer
needs a resolved source/destination and a rule against pumping the same material
back and forth. Per-face input/output/shut/automatic behavior can provide that
control without requiring two otherwise identical organ families.

Optional growth raises the reservoir on a stalk to preserve a walkway. Growth
needs clearance checks and a meaningful construction cost; bone meal is a promising
interaction, not an immutable production recipe. The storage owns its contents;
the stalk must not duplicate them or become the only place capacity information lives.

### What the actual puzzle is

Not “can you click all six faces without opening the wrong menu?” Instead: should
you buffer near consumers, run a shared trunk with priority, or split into isolated
districts? A workshop values stable supply; an excavator can tolerate a pause;
emergency repair should retain a reserve. A narrow shared route may meet average
demand and still fail during simultaneous bursts. Show that in a readable trace.

Do not simulate a travelling item entity or fluid cell for every unit. Server-side
transactions and bounded buffers own matter; client pulses illustrate accepted
flow in the correct direction. Fixed update/route bounds and sleeping idle nodes
are part of the design, not deferred optimization. Failed paths do not delete cargo.

**Open:** directed versus auto-resolved routes, exposed-face appearance, channel
capacity, crossing anatomy and the minimum controls needed before advanced UI.

## 21. Storage: a physical colony with useful access

**Owner question:** should the mod own storage, can it grow like the reservoir,
and are visible item icons affordable?

**Recommendation:** yes to a biological physical-storage family; no automatic
commitment to recreating AE2. Storage is foundational for refusing output safely,
organizing specimens and planning automation. A colony should be usable without
a separate storage mod, while welcoming one through an adapter.

Start with an expandable item capsule, the fluid/biomass reservoir family, a
filter/intake function and a local access organ. Let physical capacity matter.
Grow or add compatible segments within bounded shapes. An unmatched addition
should preferably remain independent; if it invalidates formation, contents must
remain accessible/recoverable without volume loss. Do not merge incompatible
fluids or erase item data to make formation convenient.

Specialization can be by storage policy rather than many new blocks: bulk single-
material lobes, mixed workshop stores and specimen-preserving compartments. A
memory capsule or unique mutated item retains its metadata; the renderer should
not need to draw every contained item as a distinct world object.

### Progression of access

1. Direct local opening and native inventory interaction.
2. Filtered input/output, target reserves and a shared local view.
3. Requests routed through real bounded transport to a delivery point.
4. Optional scheduled production requests, if that feature earns its complexity.

A terminal is a view of actual stores, not permission to materialize remote items.
Unloaded storage is shown as unavailable or a clearly labelled last-known snapshot;
its contents cannot be spent until a valid transfer occurs. A request can reserve
resources without duplicating them, and cancellation/restart releases or returns
those reservations deterministically.

### Icons and visible fill

A selected item emblem per capsule face is a plausible client feature. Its cost
depends on visible count, geometry, lighting and render path. Network cost comes
from synchronization and query work, not the existence of an icon itself. Send
changed summaries only, cap visible labels, use distance culling and avoid complex
arbitrary item rendering in every compartment. A cheap generic silhouette is a
fallback; exact icons appear nearby or in the UI. Profile a dense store wall before
promising negligible impact.

**Player puzzle:** put bulk stores near the mine and refine locally, or concentrate
sorting by the main base. A research district reserves rare specimens while a
construction store absorbs stone. Easy access must not make all geography and
stock allocation disappear.

**Open:** supported shapes, per-cell capacity, physical identity on splits, scope of
the shared inventory view and whether advanced autocrafting belongs here at all.

## 22. Automation and control: simple habits before programming

**Owner direction:** later goals demand automation and optimization of earlier work.

**Proposal:** start with built-in policies: maintain a reserve, stop when output is
full, refill a consumer, harvest only mature targets. Then introduce sensors,
thresholds with hysteresis, priorities and selectable process modes. A compact
conditional controller can come later if ordinary settings cannot express a useful
case. Do not make players learn a programming language to refill armor.

Examples of useful instructions: “reserve enough biomass for one restart,” “feed
the nursery before the digester,” “run the precision chamber only after its buffer
is ready,” and “disable industrial demand while the arrival station is occupied.”
The UI should explain these as behaviors, not expose raw packet/state identifiers.

Separate continuous stock maintenance from a finite order. A finite order knows
its requested outputs and reserved inputs; a stock policy is bounded and sleeps
when satisfied. Neither creates an ever-growing queue when a resource is missing.
Redstone and compatible external controllers may operate the same safe interfaces.

**Automation gates:** use the first successful useful process as evidence whenever
possible. A player should want the product regardless of advancement credit.
Observe the organ's bounded inputs/outputs and response to demand, not whether
every upstream block is “our” machine. A giant stockpile can mask some missing
automation; finite observation cannot prove all upstream resources are renewable.
Treat unattended repeatability as an acceptance target, not an infallible anti-cheat.

**Open:** local controller language, commissioning credit, preset sharing and how
much diagnostic history is useful. A short bounded trace is a stronger first feature
than global dashboards with indefinite historical storage.

## 23. Multiblock anatomy: how the LEGO promise becomes real

**Owner direction:** large organs and reusable components should combine into many
solutions, not a fixed list of ten-block machines.

**Proposal:** each organ family has a recognizable core anatomy and a limited
number of meaningful attachments. Growth chambers contain subjects; membranes
separate streams; exchange surfaces handle heat/nutrients; storage provides reserve;
regulators choose timing and allocation. Several families can reuse these roles
without every attachment becoming universally valid everywhere.

A stable chamber has a taught minimal form. The player can extend its chamber
volume, exchange area or support attachments within explicit limits. A reservoir
is an expandable capacity structure. A production district connects several
organs but is not one unlimited multiblock scan spanning the base.

**Illustrative chamber comparison:** given a fixed enclosure envelope, one design
uses more productive space and a large external reserve; another sacrifices internal
volume for better exchange; a third uses two smaller staggered chambers. The first
has high burst output, the second stable processing, the third flexible scheduling.
Every additional surface must have a meaningful cost/function; decorative curvature
does not secretly change yield unless the guide says it does.

Do not turn shape validation into a guessing game. The probe shows missing
requirements, obstructed service space and the specific support limit. Preview
several legal shapes, including vertical/terraced installations where appropriate.
Keep realistic maintenance access for players and optional workers. “Looks like
one animal” matters: connected internal borders should disappear, membranes need
sensible inward surfaces, and functional attachments should read as anatomy.

**Open:** which properties derive from structure versus installed mutation, maximum
size per family, supported transformations and a minimal reusable part vocabulary.
Numbers must come from prototypes and profiling rather than the appeal of enormous
structures in a design document.

## 24. Nutrient Sail: useful presence, optional population

**Owner direction/question:** make the slow flying support organism useful and
evolvable; investigate a reason for one per chunk and possible chunk loading.

**Recommendation:** do not make one per chunk mandatory. A biological service
district can span several chunks or occupy part of one. Technical loading boundaries
should be visible to administrators when relevant, not dictate every floor plan.

Candidate roles, best introduced one at a time:

- **Observer:** a visible local state indicator with a nearby probe/monitor interface.
- **Coordinator:** improves a bounded set of worker stations through explicit
  assignments, rather than issuing world-wide orders to every drone.
- **Field support:** carries a small defined reserve or marks a prepared landing
  site; it does not become an infinite flying store or mandatory armor charger.
- **Loading mutation, separately approved:** maintains a strictly limited owned
  region while funded and permitted by server policy.

These roles are alternatives, not a promise to stack all advantages. A stationary
organ should cover essential coordination/monitoring so players who dislike mobs
can build a complete colony. The sail can offer visibility, mobility within a
declared patrol or a different footprint tradeoff.

If explicit chunk loading is chosen, it needs a separate quota, owner/team limits,
fuel source, expiry/depletion semantics, restart behavior and observable tickets.
A loading organ may sustain a legitimately fuelled region; it must not recursively
authorize more loaders or retain tickets after removal. Its resource depletion
must pause local processes safely. Current proposals must work without it.

**Open:** mandatory versus optional status (recommend optional), chunk versus
district scope, actual support ability, movement limits and loader policy. No
production chunk-loading permission is created by this notebook.

## 25. Supportive organisms: small reliable jobs before general helpers

**Owner direction/question:** drones are visually appealing but previous mob-based
automation has often disappointed; investigate harvesting, transfer, gathering,
fertilizing, replanting, perhaps mining/building, and temporary combat helpers.

**Proposal:** a station owns jobs, territory, output reservations and a small
population cap. The insect carries out one bounded job at a time. Explicit source,
destination, work area and optional waypoints beat “find something useful anywhere.”
Idle workers return to distinct positions around their station; hatch clearance
and population accounting are authoritative and survive death/unload/reload.

First useful worker: collect a permitted item or harvest a supported mature plant,
then deliver it to reserved storage. It can traverse supported stairs and terrain
within a declared route. If the route fails, it returns or waits visibly and reports
the blocked point; it does not retry expensive pathfinding every tick forever.
Station/UI configuration should make its task as understandable as a vein's ports.

Relocation is essential if workers exist: recall to station, cancel/reconcile its
job, preserve carried items, then move the station or transfer the resting organism
in an explicit transport item. No instant teleport while keeping two copies of
its cargo or work reservation. Recovery of a worker stranded by terrain edits
needs a humane, practical rule that respects loaded chunks and ownership.

Miners and builders are later professions only if their spatial behavior adds
something beyond the corresponding stationary organs. Do not launch with one
worker capable of all tasks. Eating/fertilizing may be maintenance or a profession,
but endless feeding chores would undermine automation. Routine logistics workers
need not die on a timer; temporary combat organisms are a different lifecycle.

Original working labels for combat roles: **Bristle Runner** for a short-lived
ground interceptor and **Spinewing** for an aerial interceptor. Names are disposable
proposals, not approved ontology. Prefer one temporary form first, with fixed
lifespan/population, visible cost and no profitable death loot. Captured powerful
DNA changes a role; it does not justify copying familiar franchise units.

**Server contract:** bounded station jobs, shared pathfinding admission, backoff on
failure, limited candidates, capped entities/packets and no chunk-loading pursuit.
Observe navigation in actual stairs, doors, vertical routes, collisions and crowded
workstations; a flat-floor success is not enough. Retain vein-only alternatives.

**Open:** whether helpers belong in the core campaign, first profession, stationary
substitutes, recovery policy and what the sail adds without raising population
beyond an independently approved server budget.

## 26. Travel, flight, burrowing and long-range expansion

**Owner direction:** local movement, downward access, long-distance and eventually
cross-dimensional connections; explore flight and moving underground/through walls.

**Proposal:** give travel methods different jobs.

| Method | Best use | Limit preserving another method's value |
|---|---|---|
| Attuned walking/climbing | Routine base and field movement | Cannot cross a continent instantly |
| Prepared vertical access | Repeated mine/shaft travel | Requires a built safe route and exits |
| Gliding or assisted leaps | Early geographic exploration | Landing, elevation and endurance matter |
| Powered biological flight | Specialized later expeditions | Anatomy/fuel/output cost; not universal armor |
| Short burrowing/displacement | Tactical traversal of inspected terrain | Bounded path/endpoint; protection and hazard checks |
| Local transit organ | Regular travel between constructed sites | Endpoint cost and valid arrival space |
| Cross-dimensional gate | Connect established colonies | Native conditioning, endpoint ownership and safe transaction |
| Freight link | Move bulk production | Independent throughput, buffers and reservations |

**Burrowing is confirmed non-excavating travel.** While active inside eligible ground, nearby hard terrain appears shadowed/transparent and the player can move through it in all directions, including up and down. No mined items or permanent tunnel are produced. It cannot become open-air flight.

Stopping permits terrain to return; remaining embedded can cause suffocation. The introductory proposed stopping window is 10 seconds, with later mutations extending it. Fuel and cooldown limit use, with no permanent disabled state after entrapment, spectator recovery or mode changes. Light is a separate helmet capability, not automatically supplied by Stone Sense.

Manual Leaching remains a different tool: weaken real host rock and leave ores to mine normally. Leaching is not a compulsory Burrowing prerequisite. Safe excavated stairs/tunnels remain valuable for repeated traffic. Exact eligible terrain, protection checks and implementation budgets still require specification. The hostile-fluid lift remains parked.

An advanced gate connects prepared, authorized endpoints. Portable field anchors
could speed deployment but must not remove the reason to build a permanent station.
Passenger travel is not bulk freight capacity. Public/private access, mounts,
passengers, inventory safety and return behavior must be explicit.

**Loading problem to decide before implementation:** refusing an unloaded gate
destination is safe but may make a distant solo-player link unusable. Alternatives
are player-driven vanilla-style arrival loading through a strictly bounded approved
gate policy, an explicitly maintained destination loader, or a requirement that
another player already loads it. Do not hide this cost behind “the gate checks the
destination.” Ordinary freight can buffer until both sides are active; no fake
off-screen production. Any new ticket policy requires separate owner approval.

**Player puzzle:** an expedition network of small refill/return stations supports
light gear; a long-range suit needs fewer stops but sacrifices other anatomy. A
large gate hub simplifies access but creates congestion and a central dependency.
Regional links cost more construction but isolate failures. Safe walkable tunnels
remain useful even when personal travel becomes fast because logistics, discovery
and construction still occupy real space.

**Open:** flight timing, gate loading policy, emergency return guarantees, allowed
burrowing terrain and movement of mounts/other players. The custom hostile-fluid
lift remains parked; a useful vertical route does not require reviving it.

## 27. Nether: a thermal workshop you want to return to

**Owner direction:** resources unlock useful new capabilities; some substrate and
organs exist productively only here; the player builds a real base, not a shopping stop.

**Proposal — first foothold:** arrive by an ordinary portal with common construction
materials, a nutrient reserve and a nursery kit made entirely from earlier work.
Secure the return route and a small work area before trying to grow a thermal
organ. Initial protection can be vanilla preparation, existing basic gear or a
limited earlier adaptation; do not require the finished thermal suit to acquire
the first material needed to make that suit.

A native thermal bed matures here using local resources and imported starter
culture. Its nursery produces a heat-tolerant lining used in larger excavation,
better equipment and thermal exchange. That lining is a working material family,
not approval for several nearly identical ingots. Exported stock is useful in
other dimensions; growing/conditioning the advanced lining remains a native job.

### A playable first project

You choose a sheltered chamber near a controllable heat source. A basic dry process
makes an initial batch slowly. Adding a sealed exchange loop increases output,
but a shared nutrient route can now starve the suit-repair station. You either
reserve supply, add a local buffer or run production in batches. The installation
earns its place immediately by preparing gear and better borer support, not only
by manufacturing the next permission token.

### Why it becomes a base

Local heat services, thermal research, safe staging for specimens/minerals and
periodic advanced lining/catalyst production justify workshops, storage, repair,
defense and routes. A lava-side compact station and a sheltered buffered hall can
be viable alternatives. Grounded defense helps hold ordinary approaches; aerial
coverage needs its own visibility and supply. The environment influences layout
without arbitrary random raids on unattended players.

Low-rate restart uses earlier-tier starter inputs and local ordinary resources.
Imported refined feed and improved membranes optimize it. A broken Overworld
shipment cannot make the Nether nursery require its own missing advanced product
to restart. Dormant native substrate moved elsewhere remains recoverable, not
deleted or secretly productive in an Overworld lava room.

**Open:** native lining identity, specific local growth inputs, dry versus sealed
water bootstrap, how many advanced processes must remain here and desired fortress
versus garden visual language. Lava availability alone is not a proof of balanced
power production.

## 28. End: survive arrival, then establish a spatial workshop

**Owner direction:** first arrival may mean immediate dragon danger; allow more than
one strategy; maintain strong reasons for an End base after the dragon is gone.

**Proposal:** distinguish the arrival problem from the mature-colony problem.
The initial expedition kit uses existing materials and includes a conventional
fallback plan. The player must be able to fight, evade or cooperate before owning
any End-native mutation. Do not put the starter kit's recipe behind its own output.

### Arrival strategies to investigate

- **Prepared vanilla expedition:** existing combat/traversal preparations, a clear
  retreat/recovery plan and later settlement after the encounter.
- **Compact supported foothold:** a rapidly assembled *eligible* shelter, finite
  reserve and a small support/defense installation. Whether blocks survive dragon
  interaction must be tested; “biological” is not an implicit immunity flag.
- **Mobile distraction/support:** a bounded temporary helper or deployment organ
  buys time if such helpers are accepted. It cannot be the only viable solo route.

An End arrival is not equivalent to a Nether portal with an immediate return beside
it. Any proposed emergency return item, death-recovery support or pre-fight gate
is a deliberate balance decision, not an assumed vanilla capability. Keep ordinary
vanilla encounter/travel rules intact until that decision is made.

**World-state cases:** another team already killed the dragon; the dragon is
respawned during settlement; an End-overhaul mod changes arrival; a player reaches
outer islands early; the egg is unavailable. None should permanently prevent the
campaign. Use renewable/nonexclusive encounter evidence or alternate verification
where research needs the dragon, with clear rules rather than routine repeated kills.

### The post-dragon workshop

An End-native bed conditions spatial membranes and supports precise transfer/anchor
organs. A first local landing station or working platform helps players build and
explore immediately. An advanced spatial workshop supplies parts for long-range
links and high-precision mutation elsewhere; its operation requires active native
conditions, not just a chorus fruit in an Overworld chamber.

One architecture is a compact protected island base with consolidated buffers.
Another uses several small work platforms and short physical/biological links,
accepting more reserves and construction in return for access and isolation.
Void-facing work needs service rails, safe landing checks and fail-safe equipment;
do not introduce invisible low-fuel instant death as the balance for spatial power.

**Recurring useful work:** condition new link membranes, prepare spatial expressions,
stage exploration, process relevant specimens and build better landing/transfer
systems. Consumption follows actual advanced activity. An idle completed colony
does not demand daily dimension errands or erase its research when unloaded.

**Open:** legitimate early return, dragon interaction with structures/defenses,
native output identity, alternate research evidence and how End-overhaul adapters
declare arrival conditions. No requirement for a permanently loaded End is assumed.

## 29. Post-End play and the fourth-dimension horizon

**Owner direction:** a long game beyond the End, competitive design/optimization,
and eventually a new dimension with a new boss. No godlike armor.

**Proposal:** make the three-dimension campaign satisfying without the expansion.
Mastery projects include a distributed expedition network, a compact efficient
refinery, a self-supporting habitat district, a major traversable mine and an
excellent specialist equipment lineage. Each is a thing someone would want to
show another player, not only a large ingredient receipt.

Optional comparison scenarios specify input quality, available research, service
demand, imported support, footprint and loaded regions. Compare throughput,
efficiency, recovery or expedition performance separately. There can be a best
design for one fixed problem; replayability comes from different useful problems
and contexts, not the impossible promise that no optimum will ever exist.

### A distinct fourth-world proposition

The earlier map's layered alien biosphere remains one candidate. Its new rule can
be complementary habitats: two useful organ modes need different local conditions,
so players design buffered shelters or scheduled transitions. Keep this local and
authored, not a simulation of every plant on the planet. This adds a new relation
between components rather than a fourth color of ore with larger numbers.

An expedition brings a first nursery and a guaranteed *designed* return provision
using existing capabilities. Low-rate foothold growth does not need the new boss
loot. The environment should reward exploring something unfamiliar before combat
becomes the only objective.

The boss tests observation, prepared infrastructure and execution. Several finite
counterconditions create openings; ordinary attacks contribute when appropriate.
A solo player can preconfigure support. Failed attempts consume bounded local
supplies, not the entire interdimensional civilization. Disconnects and unloads
pause or reset under a declared rule; they do not create duplicate rewards.

Rewards offer alternative organ arrangements, controlled mode switching and visible
prestige. They do not fit every armor physiology together, create free resources
or demand infinite rematches for routine upkeep. A new high-grade substance is
justified only if it enables this distinct operation.

**Open:** whether this is an expansion, its defining ecology, boss encounter rules,
reward semantics and replayable objectives. None belongs in the first release by
default; all provisional names in the old map remain replaceable.

## 30. Building assistance and safe relocation

**Owner direction:** help players build beautiful large biological bases using
templates, growth and possibly helpers, without removing the puzzle.

**Proposal:** tools automate repetition after the player chooses a design. Early
assistance previews a supported organ, lists missing parts and helps align a
foundation on a slope. Later support repeats selected ribs, grafts an exposed
vein step, constructs a supplied corridor or grows a bounded template region.
Organ shapes should preserve paths and readable service entrances.

A blueprint stores structure and optional settings, not cargo, research progress
or live entities. Copying a good design is legitimate; printing its materials for
free is not. Show total/available/reserved resources, protected obstructions and
the order of construction. Unknown or special blocks require explicit support,
not blind duplication of all block-entity data.

Growth can have a visible order—support, membrane, functional core, activation—
without needing a server entity per visual tendril. Interrupted builds resume from
a bounded job record; finished sections are real blocks. The player can pause,
cancel and recover unused reserved materials. No automatic overwrite of inhabited
terrain or another player's structures.

Relocation of filled organs should be practical but controlled: drain/quiesce,
reserve a safe destination, move a bounded portable representation, and revalidate
native conditions before activation. If full multiblock movement is too complex,
preserve contents through explicit disassembly containers rather than pretending
all blocks can be picked up safely. Cosmetic rebuilding must not reset genetic
history or duplicate storage volume.

**Player puzzle:** adapt a proven greenhouse to a cliff with vertical exchange and
walkways, rather than flatten a huge slab. Templates teach anatomy; attachments,
placement and supply determine performance. A construction worker is optional if
a stationary grower/tool already handles the useful repetitive task.

**Open:** free-form versus curated blueprints, maximum edit region, settings copy
permissions, decommissioning rules and safe recovery of partly built structures.

## 31. Operations, monitoring and ergonomic controls

**Owner direction:** a convenient configuration/monitoring tool, useful organ UIs,
visible ports/flow and possibly organs that represent network state.

**Proposal:** keep one recognizable interaction language. Ordinary placement and
empty-hand interaction remain predictable. A probe deliberately enters inspection;
organs open rich controls through that tool; exposed connector faces support a
small quick action with visible confirmation and undo where safe. Do not require
a full menu for every turn of a conduit, nor hide advanced settings in tooltip walls.

Probe progression can add functions rather than unlock basic usability: local
status first, bounded route tracing next, then profiles/blueprints and scoped
diagnostic summaries. New grades must not make the early tool useless at opening
doors or explaining why a beginner organ stopped.

An organ screen answers five questions immediately: **what is it trying to do;
what is present; what is missing; where is the problem; what will my change cost?**
Recipes, material reservations, active mutation, supply and output are separate
panels, with optional detail rather than all gauges visible at once.

Examples: “stopped: output store full,” “waiting: destination unloaded,” “sample
resolved to this instrument's precision,” “3 ribs missing,” “reserve protected:
industrial branch paused.” A target coordinate can be highlighted nearby. “Error 6”
or a green particle that could mean anything is not a diagnostic.

Local monitor organs show reservoir fill, selected branch throughput or warnings.
They observe capped cached summaries and synchronize changes. Large historical
graphs and whole-network discovery are separate expensive features, not free UI.
Advanced controls share one tested UI foundation and safe server command contract.

**Open:** gestures/keybinds, quick-action undo, colorblind shape cues, controller
presets, public inspection permissions and how much route detail fits comfortably.

## 32. Multiplayer, ownership and trade

**Added gap:** nearly every attractive system needs a policy for more than one player.

**Proposal:** distinguish owner/team permission, physiological compatibility and
temporary public access. Wearing bio armor may allow safe substrate contact; it
does not authorize opening a DNA bank, redirecting a vein, moving a filled reservoir
or teleporting into a private room. Permission is checked server-side.

Decide separately who can inspect, configure, withdraw, build, move organisms,
activate defenses and use gates. Useful defaults are private control with explicitly
shared team facilities and optional public transit/refill areas. Guest corridors
should not become lethal because a resident matured the surrounding tissue.

Team research avoids duplicate chores. Personal practice, shared specimens and
tradeable templates need distinct accounting. A new player joining an advanced team
can benefit from its facilities without automatically receiving every personal
experience counter. Conversely, requiring that newcomer to reconstruct the entire
factory in a corner defeats cooperative play.

Trading can specialize colonies: one supplies selected genomes, another refined
components or equipment. State whether a blueprint conveys knowledge, permission
to manufacture, or just a design. Price/value is emergent; do not add an economy
currency before ordinary material and information trade is coherent.

**Failure cases:** owner offline or leaving team, station transferred, public gate
destination privatized, claim boundary through a multiblock, two players mutating
one item, recovered equipment with an old backup. Safe refusal cannot depend on
the client honestly reporting ownership. Future claim-mod adapters require explicit
tests; a native break-event check is not certification for every protection system.

**Open:** team identity provider, research after team separation, PvP/default defense
targets, trading knowledge and offline operation. No singleton “global colony owner.”

## 33. Failure, recovery and intentional danger

**Added gap:** complex living systems need a recovery experience as designed as success.

**Proposal:** distinguish an unavailable service, a bad configuration and a deliberate
hazard. Full outputs, missing chunks and server restarts normally pause. A wrong
configuration is explained and repairable. A dangerous operating mode may cause a
bounded, warned local incident only when the player has chosen to run it under
understood conditions. Hardware lag is not a biological disaster mechanic.

Organ starvation reduces demanding work, protects a declared reserve and leaves the colony recoverable. Armor separately inflicts hunger pain before full symbiosis. An expensive batch needs a disclosed suspension/abort rule.
Buffers, valves and starter reserves are available before the dangerous process.
The restoration path must not require the product the stopped machine uniquely makes.

Keep an emergency toolkit useful throughout the game: ordinary construction tools,
manual feed/container access, a return route and retained research. Dismantling an
organ deliberately accounts for contents; accidental structural invalidation does
not delete capacity or scatter thousands of stacks. The visual state can become
quiet/dormant without covering the base in ugly decay.

Do not infer that “alive” requires random disease, parasitic rebellion, aging away
all equipment or mandatory repairs to every block. Those would be major separate
tone/risk decisions and conflict with safe player-controlled colonies if hidden.

**Open:** acceptable local damage, whether player mistakes can destroy valuable
batches, gear death loss, backup costs and warning/override policy.

### Containment and recovery scenario for the biomass proposal

Apply §8's hypothetical hazardous-fluid system without silently changing the
safe-full-output baseline. A proposed incident sequence is: consumer stalls →
residual production fills reserve → local organ signals stress → relief diverts
excess, or a bounded leak occurs → player isolates and neutralizes/reclaims it.
A warning must identify the affected organ and cause, not merely display a generic
network alarm. Flesh tension, pulse changes and a gauge can agree; color alone
is insufficient. Sensors and an emergency manual control should work together.

Before approving this mechanic, test scenarios on paper and then in an isolated world:

- [ ] A full idle reservoir remains safe under the proposed pressure policy.
- [ ] A routine stopped consumer cannot cause an unexplained remote rupture.
- [ ] Available buffer/relief capacity can absorb the disclosed shutdown output.
- [ ] Cutting power, disconnecting a vein or losing a chunk does not erase the
  incident state, duplicate material or turn a paused process into hidden production.
- [ ] A leak stays within its amount, area, lifetime and shared processing limits;
  dangerous reactions cannot generate an unlimited chain or loose-item storm.
- [ ] Another player's claim, protected build or shared public route is not an
  unrestricted spill target. Network access and damage permissions are distinct.
- [ ] Recovery is possible with equipment available before the dangerous process;
  a failed advanced machine is not the sole source of its own antidote.
- [ ] Restart and save/load preserve containment and expose what remains to clean up.

These are proposed acceptance questions, **not completed tests**. We still need the
owner's choice on rupture rules, destructive severity and safe versus hazardous
biomass states. No random disaster behavior or fluid-simulation dependency is approved.

## 34. Integrations and pack-author boundaries

**Owner direction:** vanilla fit, JEI discovery, Curios support, useful guide and
advancements; FTB Quests is a pack-author responsibility.

**Proposal — prioritize contracts:**

| Integration area | Value | Boundary to protect |
|---|---|---|
| JEI | Recipes, mutation/process ingredients and discoverability | A visible recipe must explain native conditions, not just items |
| Guide + advancements | Teaching, choices, relevant next steps | Not a mandatory external quest framework |
| Curios | Auxiliary biological organs and readable equipment identity | Shared specialization/activation limits, no unlimited extra anatomy |
| Native inventories/fluids | Chests, hoppers and external automation | Simulated transfers, back-pressure, metadata and conservation |
| FE, if approved | Electrical exchange with technical mods | No replacement for genes, native beds or physical feedstock |
| AE2 and similar storage | External requests and storage access | Defined processing inputs/outputs; no fabricated remote inventory |
| Food/farming mods | Broader inputs and culinary options | Data-driven support, conversion loops and sample attribution |
| Claim/team systems | Safe shared servers | Per-action permission semantics and absence tests |

The presence of a familiar mod is not proof of compatibility. Some interactions
change balance even when APIs work: external infinite power, full-loot mob farms,
unlimited healing and cross-dimensional inventories can bypass intended scarcity.
Declare **supported base-mod behavior** separately from **pack difficulty presets**.
A pack can enforce tighter gates; our general interfaces should not arbitrarily
refuse legitimate external machines to disguise an economic dependency problem.

Maintain a small supported adapter matrix and test both presence and absence.
Unknown items/effects/genomes use explicit safe fallbacks, not reflection-based
guessing. Avoid a material explosion from creating unique intermediates for every
modded crop/ore. Tags and recipes should be editable within validated bounds.

**Open:** finalized required/optional libraries, first supported external mod set,
pack override schema and electrical/loot conversion policies. No dependency is
added by this design pass.

## 35. Discovery, story and accessibility

**Added gap:** the game needs a motive and teaching voice, not only an organ catalog.

**Proposal:** story comes from successive observations of a lifeform's capabilities.
The first culture is useful before its origin is explained. Thermal and spatial
forms reveal how the same biological language responds to different environments.
Optional records, specimen descriptions and visible anatomy imply a larger history;
the player remains the designer, not an assistant waiting for a scripted NPC.

The guide supplies a minimal working example, why it works, an alternative and
what might go wrong. Advancements recognize meaningful firsts and point to relevant
pages. A new recipe unlocked by a sample explains what that sample can become.
Already-unlocked pages and local diagnostics remain accessible without this chat.

Do not equate mystery with hidden ingredients or missing UI. Unknown capabilities
can be shown as hypotheses with discoverable evidence sources. Required early
inputs have common-resource fallbacks; optional rare exploration offers different
expressions, not an unavoidable lottery.

Readable silhouettes, 32/64-pixel original textures, differentiated material cues,
labels and non-color-only states matter in inventories and dense scenes. Offer
reduced motion/particle settings, restrained repetitive sound and clear subtitle/
warning support where relevant. A living base can breathe slowly without flashing
every active block. Tone settings for unpleasant organic imagery are worth discussing.

**Open:** first discovery story, guide voice, how explicit mutation hints should be,
sound/gore intensity and supported accessibility settings. These choices influence
art and onboarding specifications before asset production.

## 36. Visual identity and biological construction

**Owner direction:** exquisite readable gear, original art, interesting living ground,
natural organs, coherent flowing biomass and translucent fleshy storage without
internal seams or overlapping surfaces.

**Proposal:** assign each material a job: bone/cuticle supports, muscle drives,
membrane separates, translucent sacs store, luminous signals report state. Mechanical
elements can brace or connect but should not dominate the silhouette. Recipes and
function should explain enough of the appearance that a player can guess its role.

Large storage should read as one supported body, with interior membranes visible
sensibly through the near wall. Framing and flesh should hide structural joins
without obstructing walkways. Terrain texture variation is spatially coherent and
seamless, with stages adding ecological detail instead of random noisy tiles.
Conduit pulses mean accepted flow, not merely “block exists.”

Mutation changes can alter accents, silhouette, posture or a few attachment forms
without demanding a unique full model for every numeric combination. Dyes preserve
material contrast; the whole suit should not become unreadable flat color. Open
areas expose some player identity and anatomy should fit movement/crouching.

Use client-local bounded effects and passive models where possible. A moving visual
does not require a ticking server entity. Check density, all important sides,
transparency, item renders and actual player poses. Automated image checks catch
regressions; the owner still judges whether something is beautiful and biological.

**Open:** approved palette/shape language, mapping expressions to visible anatomy,
texture scale, sound library and human-owned visual acceptance samples.

## 37. Candidate organ catalog: families, not a shopping list

**Owner request:** a full working list of organs, levels and power requirements,
without too many new materials. This is the catalog implied by this notebook,
not approval to implement every row. Some rows are modes/attachments of the same
family; the final ontology should merge any that do not create distinct play.

Abbreviations: **B** biomass; **T** heat/contained steam service; **E** electrical
service; **S** spatial conditioning/constructed endpoint support. S is not a proposed
universal new energy currency. All advanced living organs may need B for actual
growth/operation, but resting decoration does not consume per-block upkeep.
Earliest eras are provisional; native foundations still apply where specified.

| Family / working function | Earliest era | Operating service | Material in → useful out | Configuration that earns its existence |
|---|---|---|---|---|
| Awakening cradle | SR0 | Small B batch/manual organic feed | Dormant item + culture → awakened item | Safe first process; later chamber mode candidate |
| Digestion organ | SR1 | Consumes organic input; optional B starter | Permitted surplus → B | Feed protection, yield/rate, accepted inputs |
| Reservoir | SR1 | Passive storage | B or compatible fluid in → same fluid out | Shape, reserve and access; no transmutation |
| Vascular junction/stalk | SR1 | Small active B cost if justified | Routed material → selected destinations | Direction, split, priority, reservoir mounting |
| Item capsule/access organ | SR1 | Passive local storage; powered remote requests later | Items in → identical data-bearing items out | Bulk/mixed policy, filters, growth, access |
| Rooted tender | SR1 | B; real water/feed where required | Mature permitted crops → harvest/replanted plot | Work area and seed reserve; drone alternative |
| Husbandry station | SR1–SR2 | B + feed | Managed animals → byproducts/controlled harvest | Population, young/adult policy, output reserve |
| Aquaculture nursery | SR2 optional | B + real feed/water habitat | Stock/bait → fish or declared specimens | Food versus specimen profile; no free treasure |
| Kitchen/conditioning chamber | SR2 optional | B and optional T | Actual food ingredients → meals/provisions | Batch versus flexible orders; chemistry attachment later |
| Sample preparation attachment | SR2 | B; T/E improve defined profiles | Specimens → prepared material | Separation/yield, protect rare evidence |
| Genome resolving chamber | SR2 | B; T/E for precision modes | Prepared specimens → material + new knowledge | Precision, throughput, evidence categories |
| DNA bank/archive | SR2 | Passive retained knowledge; powered active work | Specimens/templates in; authorized queries/transfers out | Finite queue, atlas, ownership; not a resource generator |
| Mutation chamber | SR2 | B; T/E for demanding expressions | Target + prepared graft/medium + fuel → modified target | Anatomy, exchange/support attachments, previews |
| Memory transfer attachment | SR2 | B; E optional advanced handling | Typed learning in source → compatible archive/target | Atomic transfer, retained identity; no progress cloning |
| Repair/refill station | SR0 Cradle refueling; SR2 Repair Dock | B | Stored feed → repaired/refuelled eligible equipment | Reserve/priority and access; shares chamber anatomy |
| Exploration borer | SR2 | B | Real selected terrain → loot + traversable descent | Extent, depth, route, spoil and safe access |
| Industrial excavator | SR3–SR4 | B + T; E optional precision | Real terrain + supplied access parts → galleries/shaft access | Profile, lining, branches and sustained hauling |
| Mineral separation chamber | SR2–SR4 | B; T then E by profile | Actual ore → recipe-accounted outputs | Speed, recovery, byproduct choice and support |
| Native thermal nursery | SR3, Nether only | B + local heat | Starter culture/local inputs → thermal lining | Native maturity; dry bootstrap and improved exchange |
| Heat/steam exchange organ | SR3 | B + heat input; water for steam mode | Declared thermal/water inputs → work and recovery outputs | Buffer, pressure/profile, relief and recovery |
| Condensing/recovery attachment | SR3 | Uses supplied spent process stream | Spent steam/medium → bounded recovered inputs | Useful recovery versus footprint; no positive-energy loop |
| Electrocyte generator | SR4 | B and a defined metabolic/thermal input | Actual feed/work → E with losses | Steady versus burst support; no E needed to bootstrap itself |
| Charge sac/interface | SR4 | E storage/exchange | Charge in → bounded charge out | Reserve and optional approved FE adapter |
| Regulator/sensor organ | SR1 simple; SR4 advanced | Passive/simple signal; B/E for advanced service | Local state → bounded commands | Threshold, hysteresis, priority and safe shutdown |
| Watch/attraction/repulsion organ | SR2 optional | B; E for selected expression | Supplied service → eligible local behavior | Targets/radius/cooldown; not universal boss control |
| Ground/aerial defense family | SR2; stronger SR3–SR4 | B, T or E by expression; real ammunition if used | Supplies → bounded attacks/control | Range, line of sight, target and reload tradeoffs |
| Restorative/restraint tissue support | SR2–SR4 optional | B; E/T only if function needs it | Supplies → local eligible effect | Deliberate active area; safe public corridors |
| Brood station | SR2 optional | B + growth materials | Reserved population slots → bounded workers | Assigned jobs, hatch clearance, recall and relocation |
| Temporary interceptor nursery | SR7 optional | B + selected material | Finite deployment stock → temporary helper | Lifetime/cap/role; may be a brood profile |
| Sail roost/support station | SR2+ optional | B while actively supporting | Supplies → selected local support | Stationary alternative; loader is separate unapproved mode |
| Native spatial nursery/conditioner | SR5, End only | B + prepared thermal/precision inputs | Earlier starter + local inputs → spatial membranes | Local substrate, phase/profile and safe suspension |
| Landing/vertical transit organ | SR3 local; SR5 spatial | B; S only for spatial mode | Actual supplied service → safe movement | Declared exits, blocked-arrival refusal and fallback |
| Constructed gate | SR6 | B + conditioned components/S | Authorized passenger → prepared destination | Loading/access/arrival transaction; not freight throughput |
| Freight transfer organ | SR6 | B + conditioned components/S | Reserved cargo → receiving buffer | Capacity, priority and unload-safe reconciliation |
| Construction grower | SR2 assistance; SR4 larger jobs | B; T/E only for defined advanced work | Supplied parts/template → bounded real build | Region, order, obstruction, cancellation/recovery |
| Synthesis assembly | SR7 | Selected combination of B/T/E/S | Defined earlier products → demanding expression/components | Several legal attachment/topology solutions |
| Native adaptive habitat family | SR8–SR9 horizon | Existing support + native local process | Declared inputs → alternative regulation capability | Complementary modes, safe transitions; boss-independent bootstrap |

This is deliberately a *family* inventory. A kitchen, sample preparer and mutation
chamber may share frame parts while remaining different processes; memory transfer
may simply be a chamber mode. No dedicated block should exist only to add another
crafting step. The first release must select a much smaller coherent subset.

### Minimal proposed new-material vocabulary

Start with culture, B, source-labelled specimens/genetic stock, and the reusable
support/membrane construction family. Later investigate **one thermal lining** and
**one spatial membrane** with several uses, rather than a special dust for each
organ. Ordinary water/lava, bones, minerals and food keep their Minecraft identities.
Steam and E are services/carriers, not reasons to introduce six parallel ingots.

Consumed genetic stock should retain whatever source/family compatibility a mutation
needs. A completed rare genome must not imply that unlimited wheat-derived generic
DNA can cheaply express every rare trait. Conversely, requiring a boss consumable
for every routine repair is unacceptable. Choose a renewable compatible propagation
route or restrict rare material to commissioning/refits; knowledge remains persistent.

For instance, a resolved rare template could seed a *working culture* maintained
with declared compatible common feed and a precision process. It supplies bounded
expression material, not the creature's valuable death loot. This preserves a
reason to build the laboratory without resummoning a Wither to mend a boot. Whether
that propagation route is the right economy is open; its startup must not require
its own mature output, and stopping supply stops new production safely.

Anatomy definitions and data recipes, not prose names alone, will decide which
intermediates exist. Review any material that is used by only one recipe: can an
existing resource or an explicit process condition express the same requirement?

## 38. Connected scenarios to judge before making implementation tasks

These are proposed playtests and paper-design examples, not promises that one
numeric simulation has already proved the economy.

### A. The first colony feeds its owner and itself

**Goal:** maintain a crop patch, refill one living item and supply occasional growth.
**Choices:** large forgiving field + small stores; compact intensive bed + careful
reserves; ordinary external farm + local digestion. **Interruption:** output fills.
**Expected experience:** the harvest pauses, the seed reserve remains protected,
the player reads the reason, empties or expands storage and work resumes. The
solved system no longer needs hand feeding. If optimal play is clicking every
harvest manually, the automatic organ has not earned its recipe.

### B. One rare specimen, two research plans

**Goal:** obtain a selected useful expression without destroying a rare opportunity.
**Choices:** prepare a limited early observation and use a modest trait now; preserve
the specimen while upgrading precision; collect a different compatible evidence
route. **Interruption:** insufficient precision or filled material output.
**Expected experience:** the UI explains the limit before waste; the player can
continue another useful project. A higher instrument unlocks better information,
not a requirement to repeat the same boss a hundred times.

### C. The descending mine meets the colony economy

**Goal:** create an accessible route to useful depth while supplying construction
stone and ore. **Choices:** shallow ramp over a longer route; compact shaft with
provided access; staged galleries around terrain. **Interruption:** water and full
spoil storage. **Expected experience:** the player can walk to the face, secure or
redirect work, reserve stone for building and route ore to processing. A stationary
vein spine or optional gatherer handles different spatial jobs; neither loses items.

### D. A workshop competes with construction

**Goal:** finish a precise mutation while growing a new store. **Choices:** central
reserve with priority; independent consumer buffers; staggered batches. For an
illustrative common input budget, compare completed useful jobs and lowest safe
reserve, not just maximum output of either organ. **Interruption:** a temporary feed
loss. **Expected experience:** at least two designs remain sensible at different
footprints/latencies, and the scarce job is protected by an understandable policy.

### E. Two suits and one expedition

**Goal:** explore a wet cave that leads toward a thermal work site. **Choices:**
specialist suits with a prepared changing/refill stop; generalist suit plus limited
potions; one specialist and a deliberately safer physical route. **Interruption:**
low biomass and gear swap. **Expected experience:** the better prepared solution
has an advantage, but cooldown/resource limits survive swapping. No loadout gains
all peak defenses from cheap potion or Curios stacking.

### F. Three bases with no permanently loaded worlds

**Goal:** complete a useful precision/transit order with native Overworld, Nether
and End work. **Choices:** buffered staged visits, cooperative players, or an
explicit separately approved loading mode. **Interruption:** a receiving site unloads
mid-shipment. **Expected experience:** production pauses or cargo remains reserved
in one authoritative location, then resumes. The player can explain what is
available, suspended and needed next. Remote output is not invented while unloaded.
Passenger travel follows its separately settled arrival-loading rule.

### G. A late colony supports an encounter without becoming invulnerable

**Goal:** supply a defended field installation and fight a difficult eligible target.
**Choices:** buffered high-output turret; mobile player-led control with lighter
support; coordinated limited interceptors if those exist. **Interruption:** a blocked
supply branch. **Expected experience:** warnings and reserves allow a tactical
response; the fight is not automatically won by owning high-tier armor. The server
does not pay for unlimited projectiles, mobs or dropped loot.

### H. A beautiful base is not the mechanically wrong answer

**Goal:** meet a declared service demand in a terraced, planted colony with walkways.
**Compare:** a compact bare footprint, a retained-canopy layout and a vertically
layered workshop. Count actual supporting infrastructure and imports. **Expected
experience:** ecology/access offer genuine utility and at least one attractive
layout is competitive on a relevant objective. Decoration should not secretly
be required in one arbitrary arrangement, nor always be a pure efficiency penalty.

## 39. Dependency, safety and scope review

Before ratifying a stage, walk through it from empty stores and after failure.
The following are design invariants to carry into feature specifications:

- The awakening cradle can be fed manually before automated digestion exists.
- Initial DNA work does not require the genome its output is meant to unlock.
- A thermal nursery starts without its own advanced lining; Nether water handling
  has an achievable bootstrap rather than assuming exposed water works normally.
- Electrical generation can be commissioned using earlier services, not its own
  unavailable electrical output.
- End arrival and first nursery do not require End-native gear, unique egg ownership
  or an already-built destination gate.
- A rare first sample can be preserved. Instrument refusal is not silent consumption.
- Advanced substrate functions do not force every ally/pet to own a full endgame suit.
- Farming can stabilize; passive rest is not an ever-growing maintenance liability.
- Every productive loop accounts for real environmental/material/work inputs.
- A new controller, restored archive, respawned worker or split store cannot duplicate
  conserved progress, cargo or population capacity.
- Dangerous processes have relief/recovery at their own tier and pause safely at
  unload/restart; ordinary lag does not create a catastrophe.
- No required progression stage assumes unapproved chunk loading or mandatory mobs.
- Native beds make useful local production necessary, not frequent empty errands.
- If external power/farms remove a cost, either the base design still has distinct
  constraints or the tighter gate is explicitly a pack policy—not a hidden API ban.
- First production scope is a coherent subset, not all 37 catalog rows at once.

### Performance and testability are acceptance criteria, not flavor

Each owning module specifies finite job/region/structure sizes, per-dimension or
team shared work admission, packet/entity/render limits, persistence bounds and
overload behavior before coding. Idle plants/decoration need no routine block
tickers. Dynamic storage and drones are major cost discussions. A handful of
successful machines is not evidence for hundreds of loaded colonies.

Tests need pure accounting/compatibility rules; native recipes/items/menus; actual
in-world processes; save/load and simultaneous ownership operations; wrong/full/
unloaded targets; plus client evidence of construction, gear poses, readable UIs
and dense scenes. Gameplay commissioning tests must not silently replace human
judgment of whether a puzzle is enjoyable. Measure time spent on interesting
choices, building, meaningful exploration, repetition and passive waiting separately.

This writing pass changes no runtime behavior. The foundation verifier is a
repository-integrity gate, **not validation of the proposed balance or mechanics**.
Production implementation remains HUMAN-gated under the existing ktask process.

## 40. Discussion queue and source coverage

### Follow up on the latest experiments

- [x] Keep Manual Leaching's selective host-rock preparation concept for the actual
  mod (owner selection only; production implementation remains future work).
- [ ] Choose its starting tier, acquisition, reagent cost, area and host-rock policy;
  preserve normal ore extraction and a useful late-game selective-work niche.
- [ ] Assign roles/levels to multiple downward mining methods, rather than replacing
  them all with one increasingly large tunnel. Decide the first automated family.
- [ ] Prototype the preferred one-block-wide strip with climbing-tendon entry;
  verify headroom, lighting, return access and finite next-site construction.
- [ ] Specify suit costs for activities, combat and actual self-mending; define
  empty-reserve priorities and an understandable refill loop. HUD is not metabolism.
- [ ] Decide whether biomass spills become a feature, which processes can become
  hazardous, and whether local warned pressure failure replaces the random-tank idea.
- [ ] Specify eligible terrain and protection rules for confirmed non-excavating Burrowing; do not require Manual Leaching.

### Decide first: choices with the most downstream consequences

- [ ] **Symbiosis tuning:** decide damage severity, adaptation ownership and thresholds for automatic early hunger pain; the dangerous premise is confirmed.
- [ ] **Research economy:** distinguish coverage, consumed compatible genetic stock,
  practice and Minecraft XP; select the initial deterministic evidence model or
  a clearly described alternative.
- [ ] **First useful slice:** choose the first awakened piece, one farm/salvage route,
  one reserve/storage problem and two useful solutions.
- [ ] **Anatomy model:** choose the constraints that preserve specialists even with
  ample fuel, potions, enchantments and Curios.
- [ ] **Construction language:** pick the first reusable membrane/exchange/storage
  relationships before adding named organ families.
- [ ] **Substrate identity:** ordinary adhesion and armor-led speed versus deliberately
  engineered transit tissue; visitor and livestock treatment.
- [ ] **Power policy:** thermal service and electrical precision roles; FE access and
  the distinction between base-mod progression and pack restrictions.
- [ ] **Mob decision:** vein-only viable baseline; first optional worker job; stationary
  substitute for sail support. Chunk loading remains a separate decision.
- [ ] **Ownership/recovery:** team research, equipment history, safe disassembly and
  progress transfer, before settling databases or item components.
- [ ] **Dimensional contract:** first local useful product in each dimension and a
  non-circular restart; settle actual gate destination-loading/return rules.
- [ ] **Downward excavation:** choose the first traversable descent profile and
  stopping/turning/hazard behavior without inheriting the horizontal prototype limit.
- [ ] **Long campaign:** derive release milestones from enjoyable connected slices;
  defer the fourth dimension, broad mob roster and full autocrafting until warranted.

### Coverage of the owner's submitted areas

| Original subject | Expanded home |
|---|---|
| Armor, counters/XP, fusion, awakening | §3; compatibility §17; recovery §33 |
| Tools | §4; descending excavation §18 |
| Weapons, defense and offense | §5; temporary helpers §25; encounter §29 |
| Living substances and proposed effects | §6; ecological transformation §7 |
| Nutrient Sail | §24; ownership §32; loading/travel §26 |
| Supportive mobs and every suggested profession | §25; farm/husbandry §13; building §30 |
| Farming, animals and plants | §13; renewable economy §8 |
| Food | §14; brewing §16 |
| Fishing | §15 |
| Mining, including subsequent downward clarification | §18 |
| Biomass, DNA amount/variety, steam and electricity | §§8–12 |
| Storage and item icons | §21; visual acceptance §36 |
| Flying, teleportation and burrowing | §26 |
| Nether | §27 |
| End and dragon arrival | §28 |
| Full organ list, levels and power | §37; readable era sequence §2 |
| Operations, configuration tools, monitors | §31; control policies §22 |
| Building assistance and terraforming | §§7, 23, 30 |
| Ore processing | §19 |
| Mod integration and enchantments | §§17, 34 |
| Transportation and long-range base expansion | §§20, 26–28 |
| Automation and storage management/design | §§20–23 |
| Progression levels | §2; specific connected experiences §38 |

**For future conversations:** load §0, the relevant feature section, its open choices,
and the decision log. Preserve owner statements versus proposals when editing.
Move a choice into the accepted log only after an explicit answer. Keep a short
change note identifying what new feedback altered; do not let the longest or most
recent assistant paragraph become authority merely by repetition.

### Revision record

- **0.1 — owner submission:** preserved at `5b536bc`; source subjects and questions
  are mapped above. No original question is treated as a blanket approval.
- **0.2 — assistant expansion:** adds concrete play scenes, developed feature
  candidates, critique, cross-system economies, organ catalog, safety/recovery,
  ownership, integration and a decision queue. All new mechanics remain proposals.
  The factual inspiration references and their limits are in [IDEAS_REVIEW.md](IDEAS_REVIEW.md).
- **0.3 — experiment/discussion update, 2026-09-08:** records the owner's explicit
  selection of Manual Leaching; distinguishes five tested mining approaches and
  ground-level access/site feedback; documents the tested suit reserve/HUD versus
  future activity costs; develops the unimplemented hazardous-biomass proposal and
  its recovery questions; records the earlier burrowing discussion; current direction is non-excavating ground travel
  direction. No prototype code, balance or recipe is promoted to production.
