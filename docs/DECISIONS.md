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

## D015 — mining experiments and construction feedback (user direction / details open, 2026-09-08)

Keep multiple mining methods with different progression roles rather than selecting
one universal method. Prefer exposed, ground-level mutated-substrate area markers;
provide light, stair-like angled access and usable headroom. Finite, one-shot sites
with another structure for further excavation are acceptable, not a mandatory rule
for every organ. The owner prefers a one-block-wide long strip beginning with the
liked climbing tendon, but explicitly deferred changing the wider prototype.
Exact level assignments, geometry, extensions and production families remain open.

## D016 — retain Manual Leaching (user requirement / balance open, 2026-09-08)

The owner explicitly wants the tested non-ore dissolving/weakening mechanic in
the actual mod. The relevant prototype prepares host rock into translucent, easily
mined material while leaving ores for ordinary tool extraction. Keep that feature
concept in planning. This is not approval of the prototype's recipe, 3×3 plane,
instant/zero-wear tuning, fixed host list or a production nodule/multiblock format.
Automatic ore lifting, fluid simulation and through-wall travel are not included
in this selection. Production implementation and progression specification follow.

## D017 — suit biomass and display (user direction / metabolism deferred, 2026-09-08)

Show biomass when wearing a full bio suit; use a half-transparent background and
offer placement/visibility settings. The prototype now tests that display and
explicit refueling. The owner wants activities, including combat and self-mending,
to consume biomass in future development. The current readout does not implement
those costs. Activity categories, capacities, feeding rates, repair policy and
empty-reserve behavior are open, not inherited from test tuning.

## D018 — hazardous biomass and overflow (owner idea / assistant alternatives open, 2026-09-08)

Explore biomass as aggressive flowing material whose overflow/leak would create
containment and disposal puzzles. The owner suggested a random storage rupture.
The assistant recommends a visible local pressure/operating-state failure, safe
early producers, optional later residual-production risk and multiple relief
solutions. Neither failure rule is ratified or implemented. Existing safe refusal
remains the baseline; decide destructive severity, finite conserved spills, shared
budgets, ownership and recovery before authorizing gameplay changes.

## D019 — renewed burrowing discussion (open, 2026-09-08)

The owner's "borrowing capability" question was interpreted as burrowing, matching
the earlier underground-travel interest, but that interpretation and exact behavior
still need confirmation. Excavating a real tunnel and crossing terrain without
harvesting are distinct candidates. Efficient suit movement through leached rock
is a new assistant proposal, not an accepted mutation or tested capability.

## D020 — confirmed Burrowing (user direction, 2026-09-09)

Supersedes the interpretation question in D019. Burrowing is temporary non-excavating
passage through eligible ground, in all directions, not open-air flight. Nearby
solid terrain appears shadowed/transparent. Standing still allows restoration and
can cause suffocation. The introductory test window is 10 seconds; later mutations
can extend it. Use fuel and cooldown, never a persistent disabled flag after
entrapment or mode changes. Leaching remains a separate mining mechanic.

## D021 — permanent per-piece armor evolution (user direction, 2026-09-09)

Each piece owns its counters, learning caps, anatomy and chosen evolutionary
branches. Branches cannot be exchanged; a different specialization requires another
piece. Material fusion uses biologically prepared ingredients, raises limits and
protection/durability, and retains earned history. Rigid protection competes with
flexibility and mutation capacity. No late-game profile switch may bypass those
choices. The proposed exact frame tree and counter thresholds remain reviewable.

## D022 — hungry armor and mutual symbiosis (user direction / tuning open)

Resolves D017's empty-reserve question: early reserves are very small. Hungry armor
hurts its wearer until mutual symbiosis is achieved. Afterwards it stops causing
armor hunger pain; unfunded biological features stop. Self-mending and powered
movement, light and related abilities consume biomass. Add automatic feeding,
healing and flight after incorporating an actual Elytra. Food and suit fuel are
distinct supplies. Enchantment policy in D012 remains open.

The proposed four wearer tracks plus per-piece maturation, fed-use thresholds,
damage cadence/severity, fatality and flight-rank numbers are not owner-approved
rules. No optional health-to-biomass converter substitutes for the confirmed pain.

## D023 — item catalog and prepared biological upgrades (user direction)

Maintain a catalog covering the planned mod items. Use organs to bioactivate and
fuse ingredients into treatments consumable by organs, tissues and equipment;
direct raw-item application is not the universal improvement mechanism. Normal
crafting remains useful for weak starter bodies. The Item Catalog owns preparation,
with proposed Activation Cyst/Fusion Chrysalis roles and explicit early Bowl routes.
Exact item recipes, source yields and availability require review before coding.

## D024 — documentation review and publication (task-specific authorization)

The owner requested cross-document consistency, a review PR, and inclusion of the
item catalog and updated ingredient chains in that PR. Publish the documentation
feature branch against V3_1.21.1; do not merge, change main or implement gameplay.
This approval does not authorize unrelated future remote actions.
