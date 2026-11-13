# Documentation review and consolidation

Review date: 2026-11-12. This is a temporary cleanup report, not a gameplay specification. Remove it when the consolidation is complete.

## Current state

Before this report, the working tree contains 21 Markdown files with about 113,300 words, including the historical roadmap and local process report. Eight design files contain 83,735 words. The five deleted sprint files contained another 23,697 words.

The main problem is duplicated authority. Gameplay, implementation details, proposals, task history and acceptance records share the same documents. Reducing file count alone would leave the same reading burden.

Recent changes establish a clearer direction:

- VISION separates experience, permanent evolution, genetic mutation and physical expansion.
- Samples complete species genomes. Genomes enable production of consumable genetic constructs; applying mutations also requires energy.
- Evolution branches are permanent for organs and equipment. Multiblock assemblies can be dismantled and rebuilt without resetting the core.
- Automatic growth is allowed with bounded server work. Simple technical solutions and load testing remain priorities.
- ROADMAP now includes first evolution branches and an expanded Mineral Gizzard.
- The sprint folder has been deleted in the working tree. Five concrete follow-ups were previously added to ktask. ROADMAP_OLD is retained for the owner's comparison.

## Inventory and disposition

Word counts are approximate. Paths below identify source material; deleted files remain available in Git history.

### Direction and working rules

| File | Words | Contains | Recommendation |
|---|---:|---|---|
| VISION.md | 784 | Product identity, core loop, evolution, mutation, expansion and permanent constraints | Keep as the highest gameplay authority. |
| ROADMAP.md | 2,198 | Milestones, implementation status, player steps and finish conditions | Keep; own delivery order and status here only. |
| ROADMAP_OLD.md | 4,787 | Previous roadmap, detailed acceptance evidence and verification proposal | Historical comparison only. Remove after owner review; do not implement from it. |
| README.md | 243 | Reading map and launch/build commands | Keep a short entry point linking the surviving documents. |
| AGENTS.md | 989 | Repository restrictions, roles, execution process and completion rules | Make this the sole engineering/workflow authority after consolidation. |
| ENGINEERING.md | 11,643 | Architecture, limits, tests, demo inventories, art rules, dependency pins and task process | Move durable constraints into AGENTS; remove code inventories, demo coordinates and execution history. |
| REVIEW.md | 835 | Review procedure and defect checklist | Merge a short checklist into AGENTS; delete the separate file. |
| CLAUDE.md | 12 | Redirect to AGENTS | Keep as a compatibility pointer, with no independent rules. |
| PLAYTEST_WALKTHROUGH.md | 1,287 | Starter workshop walkthrough and showcase | Put player instructions in the shipped guide; retain unfinished player outcomes in ROADMAP. Then remove. |

### Gameplay specifications

| File under docs/design/ | Words | Contains | Recommendation |
|---|---:|---|---|
| BLOCK_CATALOG.md | 33,100 | Organ catalog, logistics, containment, expansion proposals and future dimensions | Extract unique mechanics into a concise system specification. Remove implementation narratives and duplicate progression. |
| ITEM_CATALOG.md | 13,538 | Ingredients, recipes, stock, grafts, tools and dimensional processing | Preserve material roles and production relationships; avoid a prose copy of every shipped recipe. |
| GUIDE_PROGRESSION_TREE.md | 15,884 | Book dependencies, advancement triggers, species atlas and equipment progression | Preserve unique design decisions; keep shipped page structure and triggers in game resources. Remove this parallel authority. |
| ARMOR_EVOLUTION.md | 9,405 | Learning, branches, mutations, fuel, hunger, movement and recovery | Condense into the equipment section of DESIGN. Reconcile reset mechanics with permanent evolution. |
| PROGRESSION_MAP.md | 2,540 | Player projects across dimensions | Merge unique outcomes into ROADMAP and mechanics into DESIGN; delete. |
| LIVING_SUBSTRATE_MUTATIONS.md | 2,468 | Maturity, functions, reinforcement, lining and rebuilding | Merge into DESIGN's construction section. |
| VEGETATION_INFECTION.md | 5,647 | Conversion mappings, harvesting, tree boundaries and shipped implementation reports | Keep behavior and mapping tables; remove code narration and repeated evidence. |
| MINING_PROGRESSION.md | 1,153 | Descending mine, galleries, larger excavation and component roles | Merge sequence into ROADMAP and operating rules into DESIGN. |
| diagrams/living_substrate_mutations.svg | — | Diagram of substrate maturity, function and reinforcement | Keep only as an illustration of the surviving specification. |

### Historical and supporting material

| File | Contains | Recommendation |
|---|---|---|
| Deleted docs/sprints/S01.md | Accepted substrate work and pilot records | History only. |
| Deleted docs/sprints/S02.md | Accepted workshop tasks, API details and execution records | History only. |
| Deleted docs/sprints/S03.md | Equipment outline already substantially implemented | Do not recreate its task list. |
| Deleted docs/sprints/S04.md | Automation ledger, old transport rules, art history and follow-ups | Retain only unresolved useful tasks in ktask. |
| Deleted docs/sprints/S05.md | Completed passive-network redesign and compatibility notes | Current code owns the implementation; preserve the unresolved legacy-world decision. |
| docs/archive/DECISIONS.md | Historical fixed and open owner decisions | Reconcile open decisions before retiring the file. |
| docs/archive/PROTOTYPE_FINDINGS.md | Earlier technical experiments | Historical reference only; not current requirements. |
| docs/archive/LESSONS_LEARNED.md | General planning retrospective | Remove from active project documentation. |
| .local/ktask-process-review.md | Runner failures, recovery history and tooling proposals | Local tooling history; do not promote into tracked product documentation. |
| CREDITS.txt | Asset authorship, licensing, source records and prompts | Preserve as an asset ledger, outside routine reading. |
| LICENSE.txt | License text | Keep independently. |

The shipped book, advancements and translations are product content, not planning documents. Keep them. Capture options, build configuration, generated reports and logs are not additional specifications.

## Overlap and conflicts to resolve

| Subject | Current overlap | Single owner after cleanup |
|---|---|---|
| Product identity and permanent choices | VISION, armor specification, catalogs, historical decisions | VISION |
| Player journey and delivery order | ROADMAP, progression map, mining plan, guide tree, walkthrough | ROADMAP |
| Organ behavior and material flows | Block and item catalogs, ENGINEERING, sprint history | DESIGN |
| Evolution and mutation | Armor specification, item catalog, guide tree, VISION | VISION defines principles; DESIGN specifies the mechanics |
| Construction and ecology | Substrate, vegetation and block documents | DESIGN |
| Architecture, verification and review | AGENTS, ENGINEERING, REVIEW | AGENTS |
| Implementation details and exact shipped values | Catalogs, sprint reports, code and recipes | Code, recipe data and tests |
| Work remaining | Sprint tables, roadmap and task queue | ROADMAP for milestones; ktask for concrete implementation tasks |

Specific corrections needed:

1. Replace old claims that growth never proceeds automatically. The requirement is player control and bounded server load.
2. Separate experience, permanent evolution branches, genetic constructs and rebuildable multiblock bodies throughout the specifications.
3. Remove any reset that would undo evolution or reopen a rejected branch. Decide separately whether reducing an activity counter has a useful role; it must not act as a branch reset.
4. Reconcile old graft terminology with genetic constructs. A physical graft and its genetic instructions need not be the same item; avoid renaming everything without checking its role.
5. Remove the obsolete packet-based transport instructions from active requirements. The current network uses passive connectivity and loaded endpoints.
6. Resolve mining order. MINING_PROGRESSION calls a descending assembly the next proposed milestone; ROADMAP puts excavation later.
7. Correct REVIEW's blanket statement that hand-only organs are defective. The Palm deliberately starts as a manual crafting organ.
8. Remove ENGINEERING's rule that later milestones get only one roadmap line. It conflicts with the approved simple milestone structure.
9. Repair reading maps and links to deleted sprints. Two catalog links target the removed mineral-workshop heading, and one targets the removed verification proposal. Do not repair them by making ROADMAP_OLD authoritative.
10. Reconcile historical open decisions about licensing and containment. Historical labels do not prove either issue is still unresolved or has since been approved.

The mutation pool and recycling leftover DNA into culture medium remain proposals, not approved requirements. Permanent evolution is a game rule; documentation does not need to justify it as a claim about real biology.

## Recommended final document set

Four files govern development. README remains a short entrance.

| File | Responsibility | Editing budget |
|---|---|---:|
| VISION.md | What the game is and the rules that define it | About 800 words |
| ROADMAP.md | Ordered milestones, current status and player outcomes | 2,000–3,000 words |
| DESIGN.md | Approved mechanics, dependencies, choices and operating rules by system | 6,000–10,000 words |
| AGENTS.md | Repository rules, architecture boundaries, commands and completion/review requirements | 1,500–2,500 words |

These budgets guide compression; they do not authorize dropping a unique owner requirement. Do not concatenate the existing catalogs into DESIGN.

Each DESIGN section should state:

- Purpose: the problem the player solves.
- Parts: blocks and items, each with a short role.
- Operation: inputs, actions, outputs and blocked conditions.
- Choices: benefits, costs and permanent exclusions.
- Dependencies: required materials, genomes, evolution branches and physical parts.

Keep design-critical ratios where they explain a choice. Keep exact shipped tuning in code and recipe data. Planned values must be labeled as starting values. Do not duplicate complete recipe tables, registry lists, class inventories or test transcripts.

Retain LICENSE, CREDITS and the CLAUDE pointer separately. They are supporting records, not additional design authorities.

## Cleanup sequence

1. Extract unique owner requirements from the existing designs and historical decisions. List actual conflicts for resolution; do not silently choose a convenient interpretation.
2. Write DESIGN around the updated VISION. Start with growth, experience, evolution, genetics and multiblock expansion; then cover production, transport, equipment and dimensional bases.
3. Consolidate engineering and review rules into AGENTS. Remove task choreography, copied implementation details and history.
4. Update README and remaining links in the same change that removes superseded specifications.
5. Remove retired specifications from the working tree once their useful decisions have been preserved. Use Git history rather than another mandatory archive.
6. Remove ROADMAP_OLD after owner comparison. Remove this report when the cleanup is complete.

The sprint salvage created these implementation tasks: #7 repeated-graft refusal, #8 ineffective Fang grip charges, #9 capsule shedding after stem removal, #10 repeated menu scans, and #11 capture-output preservation. These are creation records, not a claim about their current execution status. They contain goals and acceptance criteria without depending on deleted sprint files.

Legacy-world conversion from stored vein fluids and traveling items remains a compatibility decision. Do not create a migration project unless those worlds must be supported. Art-finishing notes and completed qualification work are not new implementation tasks.

## Preventing accumulation

- A lasting fact has one owner document. Link instead of copying it.
- Human-approved design changes update that document directly. Implementation reports cannot silently expand scope.
- Concrete work goes into ktask as a goal and a few acceptance criteria.
- New standing documents require an explicit purpose that the existing set cannot serve.
- Temporary review documents have an explicit removal condition.
- Code, test results and the in-game guide describe what ships. ROADMAP distinguishes implementation from remaining work.
