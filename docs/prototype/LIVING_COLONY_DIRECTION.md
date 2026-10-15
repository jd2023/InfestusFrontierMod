# Living colony: feedback and next decisions

## Visual rule

Anatomy should explain function: cuticle and shell, muscle and membrane, sensory organs and rhythmic circulation. Mechanical accents are possible, but metal frames, arrows, crates and generic material noise must not become the primary visual language. Generated art needs deliberate UV placement and an in-game silhouette review, not simply tiling across every body part.

This pass refines the **unmutated armor** as an ivory-carapace study, lowers the Gatherling into a segmented insect silhouette, replaces ladder-like conduit arrows with pulsing membranes, and removes internal reservoir ribs. The Nutrient Sail is intentionally unchanged. Its current role is still population support, not chunk loading.

The follow-up embeds resource veins inside recessed tissue and makes courier trails almost flush, with bounded explicit UVs and pulses traveling toward their outlets. Reservoirs now form a rounded rectangular bladder with an inset liquid surface and original pearlescent skin, not a sharp metal-framed glass box. Rounded geometry belongs to the outside of the formed structure, never every constituent cube. The grid remains rectangular for predictable building. Armor, helper models and gameplay are unchanged by this follow-up.

## Recommended division of labor — proposals, not implemented requirements

- Veins are the dependable everyday transport system. Harvesters credit a bounded internal buffer; veins move biomass into reservoirs and from reservoirs to consuming organs. Full destinations pause harvesting. No routine world-item drops. The existing generic fluid transport and the biomass resource reservoir are **not yet one connected biomass automation system**.
- Drones remain optional specialists for discrete physical jobs: collecting scattered objects, tending growth, maintaining an organ or operating a distant work site. Do not require a drone at every storage connection.
- Separate the hatching cradle from a drone's home perch and assigned work site. Start with one explicit job per drone and no arbitrary task queue. Idle drones return to the perch.
- Relocation should use an owned cocoon interaction retaining identity and bounded cargo. Reassignment validates the destination's ownership/capacity. Do not mint duplicate helper items or evade population reservations. Cross-dimensional relocation needs a separate transactional design.
- Start 3D drone movement with player-designated stair/ramp waypoints, a bounded route and a one-block rise limit. Stop on obstruction or unloaded chunks. Wall-climbing could be a later mutation. Do not start with a free-roaming whole-base pathfinder.

## Vein network semantics

The biomass-only [Vascular Junction experiment](VASCULAR_JUNCTION.md) now provides pumped joins, round-robin outward splits and optional reservoir storage on a one- or two-block organ. It does not implement item/fluid splitters or isolated same-cell crossings. The historical notes below describe the earlier single-outlet stage.

The current transport walker supports cardinal up/down steps; scheduled in-world tests carry items up two blocks and back down, and around a horizontal elbow. Two-port surface models now show inferred inlets, corner elbows, vertical wall traces and exposed end caps. Pulse animation is cosmetic breathing, not an authoritative throughput indicator. Dedicated branching and crossing controls remain separate work.

Next routing experiment should distinguish:

- **Join:** deliberately combine compatible incoming flows.
- **Split:** explicit output ports with fair distribution, filtering/priority only after fairness and back-pressure are proven.
- **Crossing:** two isolated internal channels, or an over/under bridge; touching geometry must not silently mix independent circuits.

Current cells have one outgoing direction. Multiple sources can converge, but dedicated junction controls, splitters and isolated same-cell crossings are not implemented or validated here. Preserve bounded discovery, per-dimension work budgets and item/fluid conservation across splits, loops, reloads, full outputs and changing topology. Large distributed colonies must remain viable without unlimited throughput or range.

## Nutrient Sail as a chunk loader — needs approval and a dedicated experiment

The concept fits a colony caretaker. Bind its loading authority to a **stationary owned anchor and explicit selected chunk**, not its changing entity position. The Sail can circle inside that chunk as a visible expression of the anchor's activity. A flying entity must never recursively acquire neighboring tickets.

Before implementation, agree on team/player and server-wide hard caps, permitted dimensions, offline behavior, biomass upkeep and ticket expiry. Recommended first test: one selected chunk per anchor, conservative global/team limits, offline loading disabled by default. These are proposed defaults, not implemented settings.

The anchor must validate and release tickets on removal, death, expiry, ownership changes and restart. Work crossing its boundary must pause unless the other chunk is independently loaded. Loading one chunk continuously multiplies every machine's cost inside it; profile the worst supported colony and third-party machines, not just the Sail. Never add chunk loading as an incidental mob-rendering change.

## Deferred / known limits

- Vascular lift is parked, not removed from old saves. No lift fix or replacement is claimed in this pass.
- There is currently no drone re-home command or 3D drone navigation. Shears retire an owned helper and recover cargo; a new cradle can hatch replacements.
- Lethal damage now immediately removes a helper and frees its reservation. The reproduced defect was a short death-animation delay. Persistent hatch refusals may instead be route, spawn-space, ownership, unloaded-population or quota failures; cradle messages now identify the blocker. Do not clear unloaded reservations blindly.
- Armor branch progression, biomass harvesting balance and final visual quality remain design decisions, not completed production systems.

## How to review

Run `./gradlew runColonyLab` for the armor, helper and vein studies; use the **unmutated suit** for the new armor. The two vertical demonstration columns are at X=103 and X=106, Z=6; the elbow starts at X=101, Z=0. `runColonyShowcase` captures ten phases, including two pulse phases and the elbow. Run `./gradlew runBiologyLab` for the connected reservoir. Both use the disposable Biology Lab save and replace its fixture inventory.

Original generated sources and exact built-in image prompts: [armor](art/colony/REFINED_ARMOR.md), [current reservoir skin](art/substrate/RESERVOIR_SKIN.md), [previous sparse membrane](art/substrate/RESERVOIR_MEMBRANE.md). Vein pulses are mechanical animation exports from the armor study's original membrane material; no vanilla art was substituted.

Earlier visual pass: 40 unit tests and 41 required GameTests passed, including the optional Curios-absent profile. The recessed-vein/rounded-reservoir follow-up passes 49 unit tests and the same 41 required GameTests. Colony captures cover nine phases; the biology fixture now covers eight, including full and empty reservoirs from different sides. Vein bounds/direction and coplanar reservoir surfaces were reproduced before correction. Iterative captures then caught folded rounded geometry and opaque liquid incorrectly entering transparent sorting; geometry tests and an in-game render-pass assertion cover these failure modes. These are functional and visual checks, not a multiplayer latency or dense-base performance certification. Run headless and graphical gates as separate Gradle invocations; combining both in one invocation currently exposes a generated-resource task-order validation issue.
