# Decisions

Use explicit status: **user requirement**, **bootstrap choice**, **proposal** or
**open**. Only the owner can ratify product decisions. Include reason and impact;
do not silently rewrite an accepted entry when changing direction.

## D001 — repository and branch (user requirement, 2026-09-06)

Use `jd2023/InfestusFrontierMod`, working locally in `InfestusFrontierModV3_dev`.
The descriptive working branch is `feature/production-foundation-vision`, based
on `origin/main` at `772c5cb`. No remote mainline rewrite, merge or release.

## D002 — fresh production foundation (bootstrap choice)

Preserve the old Forge 1.19.2 project under `legacy/forge-1.19.2`; do not mix it
with the new source tree. The V3 prototype remains a separate read-only reference.
Retain its known-working 1.21.1 / NeoForge / Java 21 toolchain pins for continuity.
Do not assume prototype content, assets, bugs, tuning or save formats are approved.

## D003 — architecture and safety (user requirement)

Deep independently tested modules, small interfaces and isolated infrastructure
are mandatory. Performance is correctness; unbounded drops/scans/chunk loading
are prohibited. Gates must exercise real game behavior where pure tests cannot.

## D004 — guidance and integrations (user requirement / implementation open)

Keep advancements and an in-game guide. JEI integration and Curios support are
priorities. FTB Quests is pack-author responsibility. Specific production API
versions, required/optional status and feature acceptance tests are not finalized.

## D005 — bootstrap dependencies (bootstrap choice)

Only NeoForge is a runtime mod dependency for the empty foundation. Defer feature
libraries until their owning features and boundaries are approved. This is not a
decision to drop JEI, Curios, the guide or animation support.

## D006 — original identity (user requirement)

Original names, art, lore, sounds and distinctive designs. Inspiration must not
become recognizable copying. Keep source/asset provenance and review it before
publication. No production art is imported in this bootstrap.

## D007 — licensing mismatch (open; release blocker)

The existing root `LICENSE.txt` says CC BY 4.0; archived mod metadata says All
Rights Reserved. Preserve both records; do not silently relicense existing work.
Current metadata points readers to the license and flags the pending decision.
The owner must choose/document source and asset licensing before publishing.

## D008 — vision and gameplay ownership (open)

`VISION.md` is a first draft. Colony control/containment and team-versus-player
research ownership are interview questions. No answer is assumed from prototype
behavior. Neither a new dimension nor helper mobs are promised for first release.

## D009 — automation (bootstrap choice)

Track stable ktask instructions and HUMAN gates in Git; ignore execution logs and
mutable runner artifacts. Model selection and hard gate enforcement in the executor
are still to approve. No AI worker runs or human-gate acknowledgments in bootstrap.

## D010 — repository scope clarification (user requirement)

The owner rejected the unrequested rearrangement of existing repository code
described in D002. That historical record is not authority for future moves or
replacement. The owner subsequently requested publishing the then-current state
to `V3_1.21.1`; that branch is now the working branch. This does not authorize
further repository rearrangement, mainline changes or automatic future pushes.

## D011 — controlled growth and living ecology (user requirement)

Supersedes the colony-control question in D008: the colony stays under player
control. No indefinite autonomous ground expansion. Keep the manual-growth
baseline; a biomass-loaded spore with finite area and fuel is a candidate only.
The transformed landscape should be a varied, useful living ecosystem, not bare
decorative ground. Exact tree and vegetation behavior remains to discuss.

## D012 — progression and equipment clarification (user requirement / design open)

Draft a long, difficult, puzzle-oriented progression with player levels, automation,
powerful multiblocks, DNA combinations and persistent dimensional bases. Armor
must remain specialized; a universal loadout sacrifices specialist capability.
The earlier commitment to enchantment support on bio armor is reopened, not
replaced with a decision to prohibit it. Explore potion integration. Include a
special dimension and boss as a very-long-term horizon, not first-release scope.
`PROGRESSION_MAP.md` proposes one detailed map and alternatives; its ranks, names,
gates, quantities and mechanisms are not yet ratified or implementation tasks.

## D013 — downward mining and traversable excavation (user requirement, 2026-09-07)

Downward mining is a primary capability, not an optional addition to horizontal
excavation. Players must be able to explore/traverse the resulting tunnels or
shafts. The owner deliberately leaves the descent/access method open. Staircase,
spiral, shaft, depth, dimensions, turning and supporting transport proposals are
not approved implementations. Resources still come from actual terrain. The
horizontal prototype is evidence, not an acceptable production limitation.

## D014 — owner ideas as leading planning input (user request / proposals open)

The owner found the earlier progression map too generic to convey play experience
and supplied `ideas_and_progression_feedback.md`. Preserve that original submission
(Git checkpoint `5b536bc`), critically assess it, and expand each subject with
concrete experiences, missing systems, alternatives and consequences for ongoing
discussion. `IDEAS_REVIEW.md` and the expanded notebook fulfill that planning task.
Their assistant recommendations, names, material families, levels and quantities
are not ratified mechanics or authorization to start gameplay implementation.
Questions in the owner's source—including helpers, sails, enchantments and exact
resource/extraction mechanics—remain questions until answered explicitly.
