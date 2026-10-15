# Architecture

## Dependency direction

```text
src/main/.../InfestusFrontier          composition root: registers modules, owns no gameplay
  └── src/main/.../<owner>/            NeoForge adapters: blocks, items, block entities, menus, packets, screens
        └── src/main/.../<owner>/api   feature commands and immutable queries
              └── core/.../<owner>/    pure rules: plain Java, JUnit tests
ui/                                    screen geometry and palette: plain Java, used by screens only
```

Arrows point inward only. `core` and `ui` import nothing but the JDK. The build fails otherwise.
A feature needing another feature declares a small port interface in its own `api` package. The composition root connects ports to implementations.

## Layout rules

- One folder per owner under `src/main/java/org/jd/infestusfrontier/`. No global `block/`, `item/`, `menu/` buckets.
- Each owner registers its own content. The entry point calls one `register` per module.
- Public types live in `<owner>/api`. Everything else is package-private.
- Commands return a success or a refusal. A refusal names the unmet condition and changes nothing.
- Queries return immutable snapshots. Never a live collection.
- Screens read snapshots and send intents. All decisions run on the server.
- Rendering is client-only and never decides whether a resource moved.

## Owners

The names are contracts for this project. Create a module with its first task. Do not create it earlier.

| Owner | Owns |
|---|---|
| foundation (`core`) | Shared work admission (`TickQuota`), checked quantities, bounded identifiers |
| organ | Organ identity, completed-batch counts, earned choices |
| processing | Bowl, digestion, rack, loom, furnace, activation: reserved batch execution |
| storage | Item, fluid and biomass balances; reservoir cell accounting |
| construction | Loaded-only structure validation, shell parts, paid placement, transplanting |
| ecology | Substrate maturity, function, lining, reinforcement, dye, deliberate conversion |
| cultivation | Crop and tree growth, allocated harvest, planting reserves |
| logistics | Routes, ports, filters, reservations and movement of items, fluids and biomass |
| control | Signals, interlocks, selection, finite schedules |
| genetics | Samples, species and quality counts, genome coverage, native eligibility, cultured stock |
| equipment | Armor piece identity, fusion tree, counters, mutations, fuel, death preservation |
| portable | Carry Sac and pouch slots; refuses nesting; adds no armor abilities |
| mineral | Workpiece stages, treatment, fracture, dust and tailings |
| excavation | Finite survey and cut plans, safe stepping, access and light requirements |
| thermal | Heat, pressure, steam, cooling |
| energy | Biological electricity and explicit external energy exchange |
| containment | Spill volume, damage delay, isolation and recovery |
| spatial | End-native beds, anchoring, conditioning |
| transit | Endpoint links, player travel, durable freight escrow |
| indexing | Store summaries and bounded material requests; holds no item copies |
| husbandry | Animal work, feed, containers, berths |
| nutrition | Food and potion recipes; runs through processing ports |
| helpers | Nursery population, jobs, routes, paid Sail chunk tickets |
| defense | Hostile targeting, firing, projectile limits |
| synthesis | Sockets, lobes, idle profile switching |
| ui (`ui/`) | Layout, widgets, focus, theme, tooltips |
| interaction | Probe targeting, reach and ownership checks, dispatch to feature intents |
| discovery | Recipe reveal, advancements, guide prerequisites |
| integration | Modonomicon, GeckoLib, JEI, Curios adapters; no domain state |

## Invariants

- Processing reserves inputs, outputs, containers, waste and target before it starts. One active batch per organ. Saved stage, not wall time, drives recovery.
- Organ history has one owner. Wall blocks never copy a core's history.
- Balances have one owner (`storage`). Routes and indexes hold reservations, not totals. Integer math is checked.
- `equipment` owns every suit predicate and every fuel spend. Nobody rebuilds the full-set check.
- `genetics` owns native-class predicates. Being in a dimension, owning a genome and having a native bed are three separate conditions.
- Freight has one owner at every step and a bounded escrow.
- Optional adapters load only when their mod is present. Test mod code never ships.
