# Roadmap

Read this top to bottom. That is the order of work. Each phase finishes before
the next one starts, and inside a phase the rows are in build order.

Every milestone is one thing the player can do that they could not do before.
It is small enough to plan, build and test by itself. Designs, blocks and
numbers live in `docs/design`, not here.

- **Step** is the position in the order.
- **ID** is the milestone's permanent name. The prefix is its topic: BASE,
  MINE, WOOD, ANIMAL, EVOLVE, GENE, NETHER, POWER, END, LINK, FINISH, EXTRA.
- **Player can** is the goal. If the player cannot do it in a fresh Survival
  world, the milestone is not finished.
- **Done when** is the test that proves it.
- **Needs** lists the milestones that must be finished first.

Every milestone has one status:

- `none`: not started and not yet broken into tasks.
- `planned`: broken into tasks, work not finished.
- `done`: the behavior exists in the code. Balance and multiplayer are checked
  in the last phase.

## The order in one table

| Phase | What the player gains | Steps |
|---|---|---|
| Phase 1 | A colony that feeds itself, playable from nothing | 1–6 |
| Phase 2 | A dug pit and tunnel, a wood farm, an animal farm | 7–13 |
| Phase 3 | Upgrade paths, DNA research, mutation, an automated mine | 14–24 |
| Phase 4 | A Nether base, steam, paid chunk loading, heavy mining | 25–36 |
| Phase 5 | Electricity, better ore and DNA yield, the ore-seeking root | 37–42 |
| Phase 6 | An End base and space-bending materials | 43–46 |
| Phase 7 | Travel and freight between bases | 47–51 |
| Phase 8 | A finished, balanced, stable game | 52–56 |

## Phase 1 — The Overworld colony

A colony that feeds itself: living ground, a biomass workshop, an automated
farm, an ore line and defenses. The last step is playing that route from an
empty world, and nothing new is worth building until it works.

| Step | ID | Player can | Done when | Needs | Status |
|---|---|---|---|---|---|
| 1 | BASE-1 | Grow living ground and walls where they choose | A usable enclosed base is grown from starter materials | — | done |
| 2 | BASE-2 | Turn food scraps into biomass and grow workshop organs, a biomass furnace and living armor and tools | One organ is grown, one item smelted, one armor piece awakened | — | done |
| 3 | BASE-3 | Automate a crop farm and move biomass, water and items through the base | A wheat farm plants, harvests and digests with no hand work | — | done |
| 4 | BASE-4 | Turn hand-mined ore into crafted goods on an automated line | Raw iron in a store becomes iron bars with no hand work; full storage pauses the line | — | done |
| 5 | BASE-5 | Defend the base with organs that shoot ground and air threats | A supplied defender kills a mob and stops when its ammunition runs out | — | done |
| 6 | BASE-6 | Play from an empty world to a self-supplying farm using only the guide | A new player reaches BASE-3 in Survival without creative items or outside help, and the farm restarts by itself after its supply is cut | — | none |

Mining revision — completed for MINE-1 to MINE-4 in Ktask #71:
crawling and steering, accidents instead of hazard stops, death on lost support,
the pod's glowing root column, supplied lights and ribs, Root Treads, cut scars
and collection motion. The same rules bind MINE-5 to MINE-9 when they are built:
shafts, trenches and ore-seeking branches install their supplied access and
lighting then, as their rows below say.

## Phase 2 — First mining, wood and animals

The colony starts helping underground and gains two more renewable supplies. Everything here uses only what Phase 1 provides. The three groups are independent and can be built in any order.

| Step | ID | Player can | Done when | Needs | Status |
|---|---|---|---|---|---|
| 7 | MINE-1 | Plant a one-use organ that turns soft ground below it into see-through rock that breaks by hand | The player clears a 7-wide pit with bare hands and sees every ore in it before reaching it | BASE-2 | done |
| 8 | MINE-2 | Drive a slow level tunnel by feeding a root one step at a time and steering it | The player cuts a tunnel with a turn, walks it, collects the drops and recovers from every stop reason | BASE-2 | done |
| 9 | WOOD-1 | Keep a grove that replants a sapling after each tree is cut by hand | The player fells a tree, walks away and returns to a new sapling | BASE-3 | none |
| 10 | WOOD-2 | Leave a grove to fell its own trees and send the logs to storage | Four trees are harvested ten times with no hand work and the sapling stock never runs out | WOOD-1 | none |
| 11 | ANIMAL-1 | Keep a pen that feeds and breeds its animals up to a set number | Two cows become the set number from supplied wheat, then wheat use stops | BASE-3 | none |
| 12 | ANIMAL-2 | Collect milk, wool and eggs from a pen with no hand work | Milk and sorted wool arrive in storage; collection stops when storage is full | ANIMAL-1 | none |
| 13 | ANIMAL-3 | Have a pen cull its surplus animals for meat, hides and DNA samples | A cow pen delivers beef and leather for an hour and keeps its herd between the two set numbers | ANIMAL-1 | none |

## Phase 3 — Experience, DNA and the automated mine

Organs and tools start to improve with use, and the player starts researching DNA. Mutation then unlocks the automated mine and cultured meat. Animal culling in Phase 2 supplies the first DNA samples.

| Step | ID | Player can | Done when | Needs | Status |
|---|---|---|---|---|---|
| 14 | EVOLVE-1 | Earn experience on the furnace and the pick by using them, and pick one permanent upgrade path for each | Two furnaces on different paths behave differently; the choice survives saving and moving the block and cannot be undone | BASE-2 | none |
| 15 | GENE-1 | Collect DNA samples from creatures and plants and see each species' research progress | One species reaches fully researched from samples the player gathered | BASE-3 | planned |
| 16 | GENE-2 | Produce the material for one mutation from a researched species and apply it to an organ or tool | The target gains the new ability; the material and energy are used up; the research is kept | GENE-1 | planned |
| 17 | MINE-3 | Mark a staircase, supply a mine and let it dig to a chosen depth while it sends the rock to storage | One marked section is dug with no hand work; the player walks to the bottom; all rock is in storage; the mine stops and resumes cleanly | MINE-2, GENE-2 | done |
| 18 | MINE-4 | Dig level tunnels from the bottom of a staircase, with several cutting faces sharing one supply line | Two faces run at once from one supply; one is starved on purpose and resumes; nothing is lost | MINE-3 | done |
| 19 | ANIMAL-4 | Grow meat in an organ with no animals, once that animal's DNA is fully researched | Beef is produced with no cow loaded anywhere | GENE-1 | none |
| 20 | WOOD-3 | Keep one tree alive and fed, and take special wood, resin and fruit from it without cutting it down | A fed tree yields all three at the split the player set, stops when unfed and resumes | WOOD-2 | none |
| 21 | EVOLVE-4 | Grow the ore grinder into a larger body with more capacity, and rebuild it in a new shape | The larger grinder handles more ore per minute; taking it apart and rebuilding keeps its experience and path | BASE-4, EVOLVE-1 | none |
| 22 | EVOLVE-2 | Do the same for every living tool and each armor piece | Each tool and armor piece has at least two paths with a real trade-off | EVOLVE-1 | none |
| 23 | EVOLVE-3 | Use armor abilities that spend the suit's biomass, such as healing | An ability works, drains biomass at a shown rate and stops when the suit is empty | EVOLVE-2 | none |
| 24 | GENE-3 | Make a mutation that needs two researched species | A two-species mutation is produced and applied | GENE-2 | none |

## Phase 4 — The Nether base

The player builds a second, larger base where heat is free. It gives heat-proof materials and steam, and steam unlocks the heavy mining methods.

Paid chunk loading arrives with steam, before heavy mining, so the Nether
supply base can work while the player is elsewhere. It is a separate, supplied
service with a bounded area; miners and transport never load chunks themselves.
Task 90 delivers the approved nest-and-creature service and Nether power
plant. The guide owns its current costs, limits and operation. Energy capsules
travel through ordinary portals first; automated portal exchange remains planned.
Aura districts remain undecided. The owner brought this power and loading work
forward; local Nether food and heat-proof materials remain later work.
Steam now upgrades the existing jaw. The complete heavy-mining route in MINE-5
and the first electrical consumer in POWER-1 remain unfinished.

Design gap: renewable local food and water, heat-proof materials and later exports still need design before NETHER-1. Nether heat and power are the first implemented exports.

| Step | ID | Player can | Done when | Needs | Status |
|---|---|---|---|---|---|
| 25 | NETHER-1 | Grow colony ground in the Nether and farm fungus on it for biomass and water | A fungus farm covers its own biomass and water use for three harvests | BASE-6 | none |
| 26 | NETHER-2 | Grow a heat-proof lining that can only be made in the Nether | A batch is made from local fungus and local heat | NETHER-1 | none |
| 27 | WOOD-4 | Grow wood in the Nether from giant fungus | A Nether grove supplies stems with no logs brought from the Overworld | WOOD-2, NETHER-1 | none |
| 28 | NETHER-3 | Rebuild the furnace into a hotter one that makes hardened plates and smelts faster | A hardened plate is made; the furnace keeps its experience and path | NETHER-2, EVOLVE-1 | none |
| 29 | NETHER-4 | Boil water with lava to make steam and run a machine on it | One machine runs on steam; full stores pause production without losing inputs | NETHER-2 | done |
| 30 | LINK-4 | Keep a chosen Nether supply outpost running while working in another dimension, for a running cost | Only the chosen capped area stays loaded; its plant works and pays locally while the player is in the Overworld; unpaid loading stops, and paid loading restores correctly after a server restart | NETHER-4 | done |
| 31 | NETHER-5 | Research Nether creatures' DNA in a lab built in the Nether | One Nether species is fully researched there | NETHER-1, GENE-1 | none |
| 32 | MINE-5 | Cut hard rock that the first cutter cannot, using a steam-driven cutter | A staircase mine passes through rock that stopped MINE-3 | MINE-3, NETHER-4 | none |
| 33 | MINE-6 | Sink a vertical shaft on a platform that lowers itself and carries the player | A supplied multiblock digs a shaft to a marked depth and installs lights and climbing access; the player rides down and climbs out | MINE-5 | none |
| 34 | MINE-7 | Cut a long narrow trench down from a line on the surface | A supplied multiblock digs a marked trench to its depth, installs lights and access, and stops there; the player walks in and climbs out | MINE-5 | none |
| 35 | MINE-8 | Dissolve the rock in a sealed pit and leave the ore standing | A marked pit is cleared of rock; every ore block remains; spent liquid is drained to storage | MINE-1, NETHER-2 | none |
| 36 | NETHER-6 | Run the Nether base with no deliveries from the Overworld and send goods back | The base runs an hour and restarts after a full shutdown on its own reserves, and exports lining and plates | NETHER-1 to NETHER-4 | none |

## Phase 5 — Electricity

Electricity is the third energy after biomass and steam. It runs the machines that get more out of the same ore and the same DNA sample, and the End base cannot run without it.

Open decision for POWER-5: whether a vessel ruptures at random or fails visibly under pressure with ways to relieve it.

| Step | ID | Player can | Done when | Needs | Status |
|---|---|---|---|---|---|
| 37 | POWER-1 | Expand the Nether steam plant into electrical generation, carry and store charge, and run one machine on it | A machine runs from stored charge after the generator is turned off, then stops when the charge is gone | BASE-3 | none |
| 38 | POWER-2 | Get more metal from each ore by adding an electric separation step | The same ore gives more ingots with the step than without; the step costs electricity, water and waste space, and skipping it stays cheaper per ingot | POWER-1, BASE-4, NETHER-3 | none |
| 39 | POWER-3 | Get more DNA progress from each sample by adding an electric reading step | The same sample gives more research progress with the step than without | POWER-1, GENE-1 | none |
| 40 | POWER-4 | Start and stop machines on a schedule and read the factory's state on a display | A line runs only in its set window; a display shows which store is full and which machine is blocked | POWER-1 | none |
| 41 | POWER-5 | Contain a biomass leak from an overloaded vessel | An overloaded vessel fails in the way the design decides; a catch basin collects the spill and nothing outside it is harmed | NETHER-4 | none |
| 42 | MINE-9 | Let a root find and follow ore by itself inside a volume the player marks | The multiblock branches toward sensed ore within the volume, builds connected lit passages the player can explore, and stops sensing when electricity is cut | MINE-4, POWER-1, NETHER-5 | none |

## Phase 6 — The End base

The third base. The End gives materials that bend space, which make travel and freight possible.

| Step | ID | Player can | Done when | Needs | Status |
|---|---|---|---|---|---|
| 43 | END-1 | Anchor colony ground over the void and farm chorus on it with electricity | A chorus farm covers its own biomass and water use for three harvests | POWER-1, NETHER-2 | none |
| 44 | END-2 | Make the End's own space-bending membrane locally | A batch is made from local chorus; it does not require End DNA research | END-1 | none |
| 45 | END-3 | Research End creatures' DNA and give equipment a spatial ability | One End species is fully researched and one armor piece gains a spatial ability | END-1, GENE-2, EVOLVE-2 | none |
| 46 | END-4 | Run the End base with no deliveries from the other bases | The base runs an hour and restarts after a full shutdown on its own reserves | END-1, END-2 | none |

## Phase 7 — Connecting the bases

Three bases become one colony when players and goods move between them.

| Step | ID | Player can | Done when | Needs | Status |
|---|---|---|---|---|---|
| 47 | LINK-1 | Travel between two bases through a built gate | A player steps in at one base and arrives safely at the other | END-2 | none |
| 48 | LINK-2 | Ship items and fluids between bases | A batch sent while the far base is unloaded arrives once, whole, when it loads | LINK-1 | none |
| 49 | LINK-3 | See what is stored at every base from one place | The view shows local stock and stock in transit separately | LINK-2 | none |
| 50 | LINK-5 | Recall lost armor to a base | An armor piece dropped on death returns to its nest | END-3 | none |
| 51 | LINK-6 | Make one product that needs all three bases | The product is made with one input from each dimension, shipped by LINK-2 | LINK-2, NETHER-6, END-4 | none |

## Phase 8 — The complete game

Built features are not a finished game until the whole route is played, balanced and stable.

| Step | ID | Player can | Done when | Needs | Status |
|---|---|---|---|---|---|
| 52 | FINISH-1 | The full route plays in Survival | One player goes from an empty world to LINK-6 following only the guide | — | none |
| 53 | FINISH-2 | Costs and rates are balanced | No stage is skipped because a cheaper earlier method beats it, and no stage needs hand grinding | — | none |
| 54 | FINISH-3 | Two players share a colony on a dedicated server | Two players build, supply and travel together with no loss or duplication | — | none |
| 55 | FINISH-4 | A large colony stays stable | A dense colony keeps server tick time within the set limit and survives restarts and cut supplies | — | none |
| 56 | FINISH-5 | The license is decided | `LICENSE.txt` and the mod metadata agree. Required before any release | — | none |

## Optional — any time after its needs are met

More ways to supply the colony. None is required to finish, and none blocks a phase.

| ID | Player can | Done when | Needs | Status |
|---|---|---|---|---|
| EXTRA-1 | Farm fish and water plants | A water farm delivers fish and kelp to storage | ANIMAL-1 | none |
| EXTRA-2 | Cook rations and brew potions on automated lines | A meal and a potion are made from stored ingredients with no hand work | BASE-3 | none |
| EXTRA-3 | Hatch small helpers that carry items between two set points | A helper moves a stack between two workstations and returns when recalled | BASE-3 | none |
| EXTRA-4 | Add new attack types to base defenses by mutation | A mutated defender does something the plain one cannot | BASE-5, GENE-2 | none |

## Not planned

Attack waves. Hostiles that hunt organs. Village transformation. The Fold
dimension and its boss.
