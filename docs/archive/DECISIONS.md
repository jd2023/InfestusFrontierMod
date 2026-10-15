# Owner decisions, September 2026

For the owner. Distilled from the V3_dev decision log. Status: **fixed** = owner requirement, **open** = still to decide. Rules marked fixed are already written into BIG_PICTURE.md or the design documents.

| ID | Decision | Status |
|---|---|---|
| D001 | Work in `jd2023/InfestusFrontierMod`. No rewrite of `main`, no release. V4 now uses branch `v4`. | fixed |
| D002, D010 | Each version is a fresh start. Prototype content, assets, tuning and saves are not approved by existing. Do not rearrange old repository code. | fixed |
| D003 | Deep modules, small interfaces, performance is correctness, no unbounded drops, scans or chunk loading. Gates run real game behavior. | fixed |
| D004, D005 | Advancements and an in-game guide ship with the mod. JEI and Curios are priorities. FTB Quests is for pack authors. Libraries enter only with the feature that needs them. | fixed |
| D006 | Original names, art, lore and sounds. Record provenance. | fixed |
| D007 | `LICENSE.txt` says CC BY 4.0; old metadata said All Rights Reserved. Decide before any release. | open |
| D008, D011 | The colony stays under player control. No autonomous ground expansion. The landscape becomes a varied, useful living ecosystem. Exact tree and vegetation behavior is design work. | fixed |
| D012 | Long, difficult, puzzle-like progression with automation, multiblocks, DNA combination and persistent bases in each dimension. Armor stays specialized. Enchantments on bio armor: supported unless evidence says otherwise. Potions: explore. Fourth dimension and boss: long-term only. | fixed, details in design docs |
| D013 | Downward mining is primary. Tunnels and shafts must be walkable. Resources come from real terrain. The descent method is open to design. | fixed |
| D014 | The owner's idea notebook was planning input, now obsolete and removed. | closed |
| D015 | Keep several mining methods with different roles. Flush ground-level markers, light, stair access, headroom. Owner likes the climbing tendon and a long one-block-wide strip. | fixed, tiers open |
| D016 | Manual Leaching is in the mod: host rock becomes easy to mine, ores stay normal. Recipe, 3×3 plane and zero-wear tuning are not approved. | fixed, tuning open |
| D017 | Show suit biomass when a full bio suit is worn, half-transparent, with placement settings. Combat and self-mending will consume biomass. Rates open. | fixed, rates open |
| D018 | Biomass overflow and leaks as a containment puzzle. Owner idea: random rupture. Assistant proposal: visible pressure failure with relief options. Not decided. | open |
| D019 | Burrowing: cross terrain without harvesting it. Confirmed by the burrowing prototype and VISION. | fixed |
