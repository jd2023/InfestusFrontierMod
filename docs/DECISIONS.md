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
