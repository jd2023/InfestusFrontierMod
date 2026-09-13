# Developer guide

## Authority and scope

Explicit requirements and `VISION.md` constrain every implementation. A draft idea,
prototype behavior, or unchecked backlog item is not a ratified requirement.
Ask when a decision changes scope, progression, player risk, dependencies or saves.

Design ownership: `PROGRESSION_MAP.md` owns end-to-end player projects;
`ITEM_CATALOG.md` owns items/preparation; `BLOCK_CATALOG.md` owns organs/construction;
`LIVING_SUBSTRATE_MUTATIONS.md` owns cell anatomy; `ARMOR_EVOLUTION.md` owns equipment;
`GUIDE_PROGRESSION_TREE.md` owns teaching dependencies. `OPEN_QUESTIONS.md` is the
product-decision register. It contains only questions about intended gameplay,
scope or policy that require product direction—not balancing or implementation work.

Before adding a document, identify a responsibility not already owned. Prefer a
section in the existing owner. No parallel notebooks, reviews or prerequisite
checklists that repeat gameplay. Keep technical contracts separate from gameplay.

Use defined terms, exact conditions and observable results. Research comparable
systems, propose initial recipes, numbers and progression, and validate them with
calculations, experiments and playtesting. This is design/development work; do not
ask the player to supply tuning values or technical solutions. Keep baseline values
in the feature specification, with brief supporting evidence and tuning targets.
Distinguish calculated results from measured gameplay. An unfinished calculation
does not become a product question.

Ask only when alternatives change intended experience, scope or policy. Include
a recommendation and its tradeoff when discussing that choice; keep the question
register itself questions-only. Put resolved rules in their specifications and
remove the questions. State rules directly, without attribution or change history.
Design proposals do not authorize gameplay implementation or external actions.

The bootstrap establishes build/test infrastructure only. It does not begin the
content roadmap. Do not promote prototype implementations wholesale.

## Deep modularity: mandatory

A module hides substantial policy behind a small interface. A directory of public
classes or pass-through services does not meet this requirement. Each module owns
one cohesive behavior, its invariants, persistence and failure semantics. Prefer
package-private internals and immutable results; public methods need real callers.

Dependencies point inward: platform/third-party adapters → feature commands and
queries → deterministic rules. No cycles, reflection into sibling internals,
service locators, exposed mutable collections or client-only classes in server code.
Use build boundaries when they provide meaningful enforcement. `:core` is Java-only
and must never acquire Minecraft, NeoForge, UI or optional-mod dependencies.

Organize gameplay vertically by feature, not global block/item/menu buckets.
Extract shared low-level policy at its first production use: UI foundation,
serialization, packet validation, bounded simulation, discovery, capture timing and
integration lifecycle. Features compose these modules; they do not copy them.
Do not create empty frameworks for imagined consumers.

UI primitives—theme, layout, widgets, drawing, accessibility and capture—must be a
tested internal module before the first gameplay screen. Screens observe immutable
state and submit validated intents; they do not implement mutation or inventory rules.

Each module documents ownership, public API, allowed dependencies, invariants,
failure diagnostics, performance bounds and tests. Follow `ARCHITECTURE.md`.

## Change and validation discipline

1. Read instructions and owning specification; inspect clean Git state/checkpoint.
2. State the outcome, owning module, allowed files, non-goals and adjacent risks.
3. Discuss material server/client costs under `PERFORMANCE.md`.
4. Write the smallest meaningful failing test; prove it detects the missing behavior.
5. Implement behind the module boundary; do not expand scope opportunistically.
6. Run focused tests, then adjacent regressions and `./.ktask/verify.sh`.
7. Run the required client/visual/integration/performance checks for affected features.
8. Review the entire staged diff, including generated outputs and assets.
9. Commit the coherent scope and report evidence, omissions and remaining risk.

Dirty unrelated work belongs to the user; preserve it. A failed or flaky test is
evidence to investigate, not a reason to delete assertions, skip a check or retry
until lucky. Shared-budget tests must isolate functional fixtures and separately
exercise contention. Do not expand a module boundary just to make a test convenient.

## Feedback loop

Pure tests cover rules and limits. Contract tests cover commands and invariants.
Platform tests cover registry/data/save/network boundaries. GameTests exercise real
Minecraft state. Client fixtures verify resource loading and visible behavior.
Human review judges appearance, readability, feel and approved screenshot goldens.
Automated comparison is regression evidence, not proof of artistic quality.

Test mods and fixtures are development-only and must not enter the shipped JAR.
Use isolated disposable worlds; no copying, deleting or rebuilding ordinary saves.
No gameplay feature is done without automation or an explicitly approved
human-only exception with a documented partial automated check.

## Content and integration

Names and IDs come from an approved catalog. Recipes, tags, translations,
advancements and the guide must explain how players discover and use the feature.
Use original biological art with provenance, deliberate UVs and readable silhouettes.
Items for blocks should render their block models. Preserve vanilla relevance;
bio-armor enchantment support is unresolved (Q-008), not permission
to remove enchantments or change ordinary vanilla equipment. Cross-mod behavior
belongs in optional adapters.

FTB Quests is outside the mod's responsibility. Advancements remain required.
Dependencies are added only for a concrete feature with absence/presence tests and
an explicit required/optional decision. See `DEPENDENCIES.md`.

## Delivery

No automatic remote creation, pushes, releases, license grants or migration promises.
The local authoritative gate is the same entry point future CI/ktask must call.
Workers may not acknowledge human gates or turn proposals into approved decisions.
