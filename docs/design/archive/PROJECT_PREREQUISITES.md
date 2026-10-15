# Historical planning input

Copied from the V3 prototype on 2026-09-06. This is an unratified checklist, not an implementation mandate. Current status lives in PLANNING.md; owner decisions live in DECISIONS.md.

# Infestus Frontier: Project Prerequisites

This document is the pre-implementation checklist for turning the prototype into the actual Infestus Frontier mod.

## Provisional north star

> Build a distributed living civilization across Minecraft's dimensions. The player grows, specializes, connects, and mutates it. Late-game power opens new decisions and constructions instead of eliminating gameplay.

This statement is provisional and must be ratified during the design interview.

## 1. Design constitution

Create a concise `VISION.md` that every contributor and automated worker treats as authoritative.

- [ ] Define the player's fantasy: what the player is becoming and building.
- [ ] Define the core gameplay loop.
- [ ] Ratify three to five design pillars.
- [ ] Record explicit non-goals.
- [ ] Define how the mod integrates with and preserves vanilla Minecraft.
- [ ] Set the intended campaign-length range.
- [ ] Define the intended sources of replayability.
- [ ] Define solo and multiplayer expectations.
- [ ] Define the difficulty and punishment philosophy.
- [ ] Decide how much automation is desirable.
- [ ] Decide how much randomness is acceptable.
- [ ] Define what endgame means.
- [ ] Establish the copyright boundary: inspiration is acceptable, but names, lore, silhouettes, sounds, and recognizable designs must be original.

Candidate design pillars to discuss:

- [ ] Grow infrastructure rather than manufacture ordinary machinery.
- [ ] Make mutation create choices and tradeoffs rather than linear upgrades.
- [ ] Require useful, functioning bases in every major dimension.
- [ ] Make construction modular and expressive—more like LEGO than a crafting checklist.
- [ ] Make powerful abilities require networks, specialization, or upkeep.
- [ ] Keep vanilla resources and systems relevant throughout progression.
- [ ] Prefer discovery and experimentation over repetitive grinding.

Candidate hard prohibitions to ratify:

- [ ] Do not use direct StarCraft terminology or recognizable designs.
- [ ] Do not create one mutation that is best in every situation.
- [ ] Do not create a final machine that provides unlimited everything.
- [ ] Do not make repetitive mob farming the primary progression gate.
- [ ] Do not add decorative content merely to inflate the block or recipe count.
- [ ] Do not ship a feature without an automated testing strategy or explicit human-only gate.

## 2. Original language and ontology

Create canonical `GLOSSARY.md`, `ONTOLOGY.md`, `CONTENT_CATALOG.md`, and `NAMING_GUIDE.md` documents.

For every concept, record:

- [ ] Stable registry ID.
- [ ] Player-facing name.
- [ ] One-sentence description.
- [ ] Ontological category.
- [ ] Source and production method.
- [ ] Consumers and uses.
- [ ] Outputs and unlocks.
- [ ] Dimension and progression stage.
- [ ] Physical form: item, block, fluid, entity, effect, trait, structure, or network.
- [ ] Whether and how it is renewable.
- [ ] Storage and transport rules.
- [ ] Relevant Minecraft tags.
- [ ] Visual identity and palette.
- [ ] Related advancement and guide entry.
- [ ] Compatibility expectations for similar vanilla or modded concepts.
- [ ] Names, imagery, and concepts it must not resemble.

Define the initial conceptual categories:

- [ ] Living substrate and ecological states.
- [ ] Biomass and nutrient resources.
- [ ] Genetic samples.
- [ ] Traits, genes, mutations, and instability.
- [ ] Enzymes or biological catalysts.
- [ ] Living structural components.
- [ ] Functional organs and modules.
- [ ] Network nodes and conduits.
- [ ] Equipment mutations.
- [ ] Environmental adaptations.
- [ ] Dimension-specific biological strains.
- [ ] Boss-derived or discovery-derived traits.

- [ ] Decide how to represent ontology relationships in machine-readable data.
- [ ] Use that data to validate recipes, guide pages, JEI categories, advancements, and content dependencies.

## 3. Gameplay system specifications

### 3.1 Mutation

- [ ] Decide whether mutation is deterministic, random, selectable, bred, or a controlled combination.
- [ ] Define what constitutes genetic input.
- [ ] Define how traits are discovered.
- [ ] Decide whether traits have levels.
- [ ] Define trait compatibility and exclusivity rules.
- [ ] Define benefits, costs, and tradeoffs.
- [ ] Define mutation capacity and diminishing returns.
- [ ] Decide whether instability, rejection, corruption, or side effects exist.
- [ ] Decide whether mutations can be extracted, copied, combined, removed, or inherited.
- [ ] Prevent experimentation from becoming blind RNG grinding.
- [ ] Define how outcomes and risks are communicated before player commitment.

### 3.2 Equipment mutation

- [ ] Define supported vanilla and modded item categories.
- [ ] Define mutation slots or capacity.
- [ ] Define trait combinations and conflicts.
- [ ] Define interactions with enchantments.
- [ ] Define interactions with durability and repair.
- [ ] Define interactions with smithing and armor trim.
- [ ] Decide how mutated equipment remains visually recognizable.
- [ ] Design mutation tooltips and comparison UI.
- [ ] Define death, loss, recovery, and duplication behavior.
- [ ] Define the server-authoritative storage format.
- [ ] Establish save-data versioning.
- [ ] Prevent one universally optimal equipment set.

### 3.3 Living construction grammar

Define a reusable vocabulary of components:

- [ ] Structural tissue.
- [ ] Armor and shell.
- [ ] Conduits.
- [ ] Nutrient or energy transport.
- [ ] Sensors.
- [ ] Processing organs.
- [ ] Storage cavities.
- [ ] Player and machine interfaces.
- [ ] Control cores.
- [ ] Environmental modules.
- [ ] Input and output membranes.
- [ ] Player access points.

Decide how a structure's behavior emerges:

- [ ] Which organs are present.
- [ ] How organs are arranged.
- [ ] Network topology.
- [ ] Available nutrients and energy.
- [ ] Environmental conditions.
- [ ] Installed genetic traits.
- [ ] Current dimension.
- [ ] Size, capacity, and efficiency tradeoffs.

- [ ] Decide where freeform construction ends and validated functional regions begin.
- [ ] Ensure that modular freedom remains teachable, balanceable, renderable, and testable.
- [ ] Avoid relying primarily on fixed ten-block multiblock recipes.

### 3.4 Living transit network

Design an original long-distance and cross-dimensional transport system without copying the Nydus Worm identity.

- [ ] Decide whether it transports players.
- [ ] Decide whether it transports items.
- [ ] Decide whether it transports fluids or biological resources.
- [ ] Decide whether it transports signals.
- [ ] Require a grown and activated endpoint at every destination.
- [ ] Define network topology and connection rules.
- [ ] Define the additional requirements for cross-dimensional links.
- [ ] Define throughput, capacity, travel time, cooldown, or biological cost.
- [ ] Decide how it behaves through unloaded chunks.
- [ ] Define interruption, damage, and recovery behavior.
- [ ] Design safe destination selection.
- [ ] Define ownership, access control, and multiplayer behavior.
- [ ] Preserve the value of exploration and geography.
- [ ] Decide whether every dimension requires a locally adapted endpoint.
- [ ] Define failure behavior for insufficient capacity or broken connections.
- [ ] Ensure the system enables distributed bases instead of becoming a free teleport block.

### 3.5 Ecology and spread

- [ ] Define which blocks and environments can be colonized.
- [ ] Specify true three-dimensional spread rules.
- [ ] Define chunk-loading and chunk-unloading behavior.
- [ ] Define environmental requirements.
- [ ] Provide player controls and containment mechanisms.
- [ ] Provide reclamation and cleanup mechanisms.
- [ ] Define interactions with crops.
- [ ] Define interactions with fluids.
- [ ] Define interactions with redstone.
- [ ] Define interactions with terrain and generated structures.
- [ ] Set server tick and memory performance budgets.
- [ ] Decide whether multiple biological strains compete.
- [ ] Define distinct Overworld, Nether, and End substrate behavior.
- [ ] Define griefing protection and configuration.

### 3.6 Resources and biological economy

Decide which inputs and constraints exist:

- [ ] Biomass.
- [ ] Nutrients.
- [ ] Heat.
- [ ] Light.
- [ ] Experience.
- [ ] Fluids.
- [ ] Environmental exposure.
- [ ] Mob-derived samples.
- [ ] Dimension-specific matter.
- [ ] Time.
- [ ] Structural capacity.

- [ ] Design production chains that create logistical and architectural choices.
- [ ] Avoid ordinary energy-conversion chains with biological textures pasted onto them.

## 4. Progression and dimensional roles

Create `PROGRESSION.md` and `DIMENSIONS.md`.

For every progression era, specify:

- [ ] New player capability.
- [ ] New construction vocabulary.
- [ ] New decisions introduced.
- [ ] Required discoveries.
- [ ] Dimension involved.
- [ ] Expected playtime range.
- [ ] Primary challenge.
- [ ] What becomes easier.
- [ ] What must remain relevant.
- [ ] Alternative routes.
- [ ] Replayability mechanism.
- [ ] Boss or major encounter, if any.
- [ ] Exit criteria into the next era.

For every dimension, specify:

- [ ] Exclusive resources.
- [ ] Unique environmental constraints.
- [ ] Required infrastructure.
- [ ] Traits obtainable there.
- [ ] Processes that work better or only there.
- [ ] Risks.
- [ ] Reasons to maintain a permanent base.
- [ ] Connections to other dimensions.

- [ ] Ensure the Nether base is operationally useful rather than just a portal room.
- [ ] Turn the End into sustained gameplay rather than a final visit.
- [ ] Add another dimension only if it has a unique systemic purpose beyond more ores and another boss.

## 5. Replayability and long-term power

Design replayability around:

- [ ] Branching biological specializations.
- [ ] Mutually exclusive adaptations.
- [ ] World-dependent discoveries.
- [ ] Multiple viable network layouts.
- [ ] Environmental optimization.
- [ ] Modular machine combinations.
- [ ] Limited mutation capacity.
- [ ] Base geography.
- [ ] Alternative dimension-colonization strategies.
- [ ] Cooperative player roles.
- [ ] Reversible but costly specialization.

Define late-game power primarily through:

- [ ] Greater reach.
- [ ] New transformations.
- [ ] More sophisticated automation.
- [ ] Better logistics.
- [ ] Environmental control.
- [ ] New construction possibilities.
- [ ] Specialized advantages.

Ratify endgame constraints:

- [ ] Automation still requires designed infrastructure.
- [ ] Fast transport requires a maintained network.
- [ ] Strong equipment carries specialization costs.
- [ ] Resource multiplication has bounded efficiency or meaningful inputs.
- [ ] No single block replaces mining, farming, storage, travel, and combat.
- [ ] Vertical progression must not erase hunger, danger, movement, resources, building, and decision-making simultaneously.

## 6. Story and discovery

Create a narrative specification.

- [ ] Define the original premise.
- [ ] Explain why this life form exists.
- [ ] Explain why the player can interact with it.
- [ ] Explain why it spans dimensions.
- [ ] Define what the player is trying to accomplish.
- [ ] Decide whether the organism is a tool, symbiote, ecosystem, intelligence, or intentionally ambiguous.
- [ ] Define major discoveries and narrative turns.
- [ ] Define how bosses relate to the biological system.
- [ ] Define what changes after reaching the End.
- [ ] Decide whether the story concludes or opens a postgame.

Choose and integrate delivery mechanisms:

- [ ] Advancements.
- [ ] Modonomicon guide entries.
- [ ] Environmental structures.
- [ ] Scannable specimens.
- [ ] Equipment and block tooltips.
- [ ] Dimension changes.
- [ ] Boss encounters.
- [ ] Ensure the story guides discovery without turning the sandbox into a rigid quest chain.

## 7. Content and milestone interview

Conduct the design interview in this order:

- [ ] Player fantasy and tone.
- [ ] First discovery and the first 30 minutes.
- [ ] Early substrate and mutation loop.
- [ ] Equipment mutation philosophy.
- [ ] Modular living-machine grammar.
- [ ] Resource and logistics economy.
- [ ] Overworld progression.
- [ ] Nether colonization.
- [ ] Long-distance living network.
- [ ] End colonization and post-End progression.
- [ ] New dimension, if systemically justified.
- [ ] Threats, helpers, drones, mobs, and bosses.
- [ ] Endgame capabilities.
- [ ] Multiplayer and ownership.
- [ ] Replayability and alternative builds.
- [ ] Compatibility expectations.
- [ ] Accessibility, configuration, and difficulty.
- [ ] Art, animation, sound, and narrative tone.

For every interview section:

- [ ] Record ratified decisions.
- [ ] Record unresolved questions.
- [ ] Record rejected alternatives and reasoning.
- [ ] Define milestone acceptance criteria.

## 8. Technical foundation

Before creating the production repository, finalize:

- [ ] Minecraft version.
- [ ] NeoForge version and supported version range.
- [ ] Java toolchain.
- [ ] Gradle and ModDevGradle versions.
- [ ] Mappings.
- [ ] NeoForge-only versus eventual multi-loader support.
- [ ] Package namespace and immutable mod ID.
- [ ] Source license.
- [ ] Asset license.
- [ ] Versioning policy.
- [ ] Save-data compatibility policy.
- [ ] Data-generation approach.
- [ ] Configuration system.
- [ ] Networking architecture.
- [ ] Client/server authority boundaries.
- [ ] Registry conventions.
- [ ] Data component and attachment conventions.
- [ ] World-generation architecture.
- [ ] Integration-adapter architecture.
- [ ] Ratify `DEVELOPER_GUIDE.md` as a mandatory production-code contract.
- [ ] Define the production module map, ownership, public interfaces, and allowed dependency graph.
- [ ] Enforce acyclic module boundaries with package visibility, build boundaries, and architecture tests.
- [ ] Require every module to have fast rule tests plus contract tests at its platform boundaries.
- [ ] Extract low-level UI primitives into a tested internal UI foundation before any additional production UI work.
- [ ] Data-pack extensibility.
- [ ] Resource-pack extensibility.
- [ ] Logging and diagnostic commands.
- [ ] Ratify explicit per-system work, entity, packet, persistence, and chunk-loading budgets.
- [ ] Prohibit unbounded loose-item output and define back-pressure for every automated producer.
- [ ] Define stress scenarios and profiler-based acceptance limits for scalable mechanics.
- [ ] Separate pure biological rules from Minecraft integration.
- [ ] Make mutation selection, compatibility, costs, and progression testable without launching Minecraft.

## 9. Dependency decisions

Evaluate the initial candidate set:

- [ ] NeoForge.
- [ ] JEI for recipe and process discovery.
- [ ] Modonomicon for the in-game guide.
- [ ] GeckoLib for animated blocks and future organisms.

Resolve the remaining dependency questions:

- [ ] Decide whether a multiblock library is worth its constraints.
- [ ] Decide whether Jade or WTHIT integration is important at launch.
- [ ] Decide whether Curios or Accessories integration is needed.
- [ ] Decide whether configuration requires anything beyond NeoForge facilities.
- [ ] Decide whether GeckoLib is used selectively or throughout the mod.
- [ ] Choose which major mods receive explicit compatibility.
- [ ] Prefer tags and optional adapters over unnecessary hard dependencies.

For every dependency, record:

- [ ] Exact pinned version.
- [ ] License.
- [ ] Maintenance status.
- [ ] Server/client requirement.
- [ ] API stability.
- [ ] Available test support.
- [ ] Feature it enables.
- [ ] Cost of replacing it.
- [ ] Behavior when absent.
- [ ] Required or optional status.

## 10. Testing and feedback infrastructure

Build these layers before scaling content production:

- [ ] Pure Java unit tests.
- [ ] Asset and JSON contract tests.
- [ ] Data-generation staleness checks.
- [ ] NeoForge GameTests.
- [ ] Recipe tests.
- [ ] Advancement unlock tests.
- [ ] Menu and container state tests.
- [ ] World save/reload persistence tests.
- [ ] World-generation tests.
- [ ] Dedicated-server smoke tests.
- [ ] Automated client launch and clean shutdown.
- [ ] Missing-model and missing-texture detection.
- [ ] Automated screenshot capture.
- [ ] Human-approved screenshot goldens.
- [ ] Automated screenshot-difference detection.
- [ ] Animation-state captures.
- [ ] Performance budgets for substrate spread.
- [ ] Performance budgets for living networks.
- [ ] Chunk load/unload tests.
- [ ] Deterministic seeded randomness.
- [ ] Compatibility runs with essential mods present and absent.
- [ ] Crash and severe-log scanning.

Establish the visual gate:

- [ ] Require human approval for the first appearance of every new visual surface.
- [ ] Pin each approved appearance as a screenshot golden.
- [ ] Prevent unexpected visual changes automatically.
- [ ] Make golden replacement a human-only, explicitly documented action.

## 11. Repository and project setup

- [ ] Create the production Git repository and remote.
- [ ] Establish a protected main branch.
- [ ] Create and commit a clean initial baseline.
- [ ] Add a complete `.gitignore`.
- [ ] Add `README.md`.
- [ ] Add `VISION.md`.
- [ ] Add `AGENTS.md`.
- [ ] Add `GLOSSARY.md`.
- [ ] Add `ONTOLOGY.md`.
- [ ] Add `CONTENT_CATALOG.md`.
- [ ] Add `NAMING_GUIDE.md`.
- [ ] Add `PROGRESSION.md`.
- [ ] Add `DIMENSIONS.md`.
- [ ] Add `ART_DIRECTION.md`.
- [ ] Add `TECHNICAL_ARCHITECTURE.md`.
- [ ] Add and ratify `DEVELOPER_GUIDE.md`.
- [ ] Add `TESTING.md`.
- [ ] Add `COMPATIBILITY.md`.
- [ ] Add `DESIGN_DECISIONS.md`.
- [ ] Add an RFC template.
- [ ] Add art and audio provenance records.
- [ ] Add issue and task templates.
- [ ] Add a changelog and release procedure.
- [ ] Pin the Gradle wrapper and dependencies.
- [ ] Add CI that runs the same authoritative gate as local development.
- [ ] Define ownership and regeneration instructions for art sources, prompts, Blockbench files, animations, and runtime exports.

## 12. ktask setup

- [ ] Put stable vision, facts, and cross-cutting invariants in `.ktask/context.md`.
- [ ] Put the strict implementation workflow in `.ktask/prompt.md`.
- [ ] Create an ordered queue of independently verifiable tasks.
- [ ] Create one authoritative `.ktask/verify.sh`.
- [ ] Define the structured task-report format.
- [ ] Configure bounded remediation.
- [ ] Add `HUMAN:` gates for design decisions and visual approval.
- [ ] Select models according to task risk and gate strength.
- [ ] Define task-branch, commit, merge, and push rules.
- [ ] Prohibit workers from silently making product decisions.
- [ ] Prohibit workers from weakening tests or verification.
- [ ] Require full staged-diff review before every commit.
- [ ] Require disclosure of skipped checks and remaining risk.

Require every queued implementation task to state:

- [ ] One concrete outcome.
- [ ] Acceptance criteria.
- [ ] Required red-first regression test.
- [ ] In-scope files or subsystem.
- [ ] Explicit non-goals.
- [ ] Adjacent behavior requiring regression verification.
- [ ] Required human approval, if any.
- [ ] Completion gate.

## 13. Definition of ready

Do not begin large-scale content implementation until all of these are complete:

- [ ] Ratified vision and non-goals.
- [ ] Initial ontology and naming rules.
- [ ] First complete progression map.
- [ ] Defined roles for every target dimension.
- [ ] Mutation-system specification.
- [ ] Modular-construction principles.
- [ ] First vertical-slice milestone.
- [ ] Dependency and version matrix.
- [ ] Technical architecture.
- [ ] Git and ktask workflow.
- [ ] Full test-gate skeleton.
- [ ] Human-approved visual direction.
- [ ] Explicit IP and licensing rules.

The following should remain adjustable milestone by milestone:

- [ ] Exact recipes.
- [ ] Balance numbers.
- [ ] Individual mutations.
- [ ] Final block and item counts.
- [ ] Content sequencing within ratified progression boundaries.

## Guiding implementation principle

- [ ] Design a small number of systems that multiply one another.
- [ ] Make mutation, environment, topology, dimension, equipment, and living-machine components interact meaningfully.
- [ ] Measure depth by viable player constructions and strategies, not by raw block count.
