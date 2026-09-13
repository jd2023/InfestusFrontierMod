# Open questions

This is the only active register of unresolved design and delivery decisions.
An entry stays open until the owner answers it and the named owning document is updated.
An exact draft value is a proposal, not an approved value. Missing values are not permission to guess.
Do not create a second question list in a catalog, task backlog or discussion notebook.

Resolve Q-001–Q-006 before specifying armor progression; Q-011–Q-015 before balancing industrial production.
References to these entries identify implementation blockers, not hidden player requirements.
When closing an entry, retain its ID, record the answer in its owning specification and link the decision in DECISIONS.md.
All entries below are **open**.

## Armor

### Q-001 — Shared learning capacity and fusion levels
- **Decision:** Choose each frame's numeric shared capacity, the unit counted, and the capacity increase at each fusion level. Confirm whether the existing G0–G4 material tree remains the complete level ladder or receives additional levels; specify amethyst ancestry. Flexible versus rigid capacity differences need numbers.
- **Resolve in:** [Armor](ARMOR_EVOLUTION.md). Independent pieces, a sum cap and fusion-raised capacity are settled; individual counter caps and rank-sum item levels are superseded.

### Q-002 — Activities, credit and buffs
- **Decision:** Ratify the proposed activity-to-piece table; specify each event's eligibility, points per event/distance/time, fractional-credit handling, concurrent-event classification and repeated-event limits. Define every counter threshold, resulting buff and stacking ceiling. Decide whether any counter also gates mutation installation.
- **Resolve in:** [Armor](ARMOR_EVOLUTION.md). No learning-pause switch. A single activity cannot teach multiple pieces. No hidden uncapped practice bank.

### Q-003 — Paid partial counter reduction
- **Decision:** Choose the ritual structure, fixed reduction amount, ingredient recipe, energy type/quantity, duration and minimum remaining count. If the selected counter has fewer points than one reduction, refuse or remove the remainder? Ratify loss of below-threshold buffs and treatment of installed mutations whose learning prerequisite is no longer met. Decide separately whether removed points can ever be stored/transferred.
- **Resolve in:** [Armor](ARMOR_EVOLUTION.md), then required entries in [Blocks](BLOCK_CATALOG.md) and [Items](ITEM_CATALOG.md). Ten points was an example, not an approved amount. Permanent branches and fusion level are not reset.

### Q-004 — Death learning and the single rescue
- **Decision:** Does an actual death teach a retained rescue ability, with sleep restoring its charge, or does sleep erase the death counter? Specify owning piece, threshold, first-death behavior, qualifying lethal damage, rescue health/effects, biomass cost, rearming bed/block interaction and cooldown. Define reset-block recipe if chosen.
- **Resolve in:** [Armor](ARMOR_EVOLUTION.md). Include Totem interaction, multiple pieces/players, swapping, death drops, keep-inventory, disconnection, bed use across dimensions and repeated bed-reset abuse. One rescue per charge, not passive unlimited immortality.

### Q-005 — Fusion cost and permanent ancestry
- **Decision:** Ratify every parent→child frame, frame statistics, mutation slots, fusion medium doses, biomass/other energy costs, chamber attachments and processing time. Specify an exponential cost schedule and how dimensional ingredients contribute, rather than leaving “exponentially hard” as a recipe.
- **Resolve in:** [Armor](ARMOR_EVOLUTION.md) and preparation recipes in [Items](ITEM_CATALOG.md). Raw ingots/gems cannot directly fuse. Upgrading raises capacity without awarding activity points.

### Q-006 — Mutation recipes, branches and access
- **Decision:** Ratify the existing I→II→III families and exclusions; choose multi-genome recipes, signature ingredients and fusion-level requirements. Define Berserk damage/speed benefit, defense penalty, duration, recovery, slots and fuel. Set late access for automatic feeding and Elytra flight. Resolve branch access after partial counter reduction under Q-003.
- **Resolve in:** [Armor](ARMOR_EVOLUTION.md), [Items](ITEM_CATALOG.md), then instructional nodes in [Guide](GUIDE_PROGRESSION_TREE.md). DNA combinations in Armor are proposals, not claims about vanilla mob powers.

### Q-007 — Symbiosis, starvation and equipment loss
- **Decision:** Choose adaptation ownership (wearer, piece or both), whether its counter shares the learning budget, progress conditions and threshold. Set hunger damage, cadence, difficulty behavior and fatality; define collapsed-item healing, death/drop loss and recovery. Specify fuel warnings and ability priority ties.
- **Resolve in:** [Armor](ARMOR_EVOLUTION.md). Hunger pain before symbiosis and unfunded-feature shutdown afterwards are settled. The death-rescue question is Q-004, not an assumed recovery guarantee.

### Q-008 — Enchantments, XP and potion interactions
- **Decision:** Choose armor/tool/weapon enchantment support; define Mending, Unbreaking, Protection, Fortune and movement interactions, anvil/grindstone behavior and data preservation through fusion. Decide whether Minecraft XP is consumed by any ritual or fusion recipe. Ratify supported potion effects, delivery priority and dose accounting.
- **Resolve in:** [Armor](ARMOR_EVOLUTION.md) for equipment; [Items](ITEM_CATALOG.md) for doses/recipes; [Dependencies](DEPENDENCIES.md) for adapters. Minecraft XP is not automatically interchangeable with activity points.

### Q-009 — Fuel reserves and operating budgets
- **Decision:** Ratify per-frame reserves, complete/partial-suit output, ability costs and concurrent-load limits. Specify free passive effects versus fueled buffs, exact portable transfer rates and emergency reserve policy. Profile full-suit movement, sensing, lighting and Burrowing under [Performance](PERFORMANCE.md).
- **Resolve in:** [Armor](ARMOR_EVOLUTION.md). Current numeric mutation effects/costs are retained draft candidates, not a complete balanced loadout specification.

### Q-010 — Personal digestion and player feeding
- **Decision:** Choose permitted feed/food items, per-item stationary reference yield, personal conversion yield/rate, remainder rounding, slot reservations and starting level. Ratify C9 player feeding, C12 container intake and C13 digestive anatomy/access without conflating their outputs.
- **Resolve in:** [Armor](ARMOR_EVOLUTION.md) and [Items](ITEM_CATALOG.md). Digestion uses matter, not player health; container intake transfers existing biomass.

## Production, construction and storage

### Q-011 — Ore recovery and stored dust
- **Decision:** Map eligible ore blocks/raw ore to a processing input unit for each metal. Assign recovery steps 4→5→6→7→8 dust and smelting steps 4→3→2→1 dust per ingot to actual organs/attachments; define byproducts, batch sizes and Fortune/Silk Touch/imported-ore accounting. Decide which treatment states share the same dust item.
- **Resolve in:** [Items](ITEM_CATALOG.md) and [Blocks](BLOCK_CATALOG.md). Stockpiling compatible dust for later efficient smelting is intended. Maximum combination yields eight ingots per defined input unit; it must not create an ingot recycling loop.

### Q-012 — Energy and expensive processing routes
- **Decision:** Define biomass, heat, steam and electricity units; conversions, generation losses, storage/rate limits and exact per-batch costs for every ore improvement. Choose FE import/export policy. Compare throughput, total energy per ingot and footprint for bypass, partial and maximum recovery routes.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md), [Items](ITEM_CATALOG.md), adapter boundary in [Dependencies](DEPENDENCIES.md). More infrastructure and operating energy are required; “high energy” alone is not an implementation rule.

### Q-013 — Biomass from manual to industrial
- **Decision:** Choose feed families, yields, water use, digestion times and organ learning upgrades. Decide whether thermal pretreatment/catalysts form an industrial route, with exact native producer, recipe and consumption. Verify renewable farm surplus after all operating costs and reserves; no closed recycling gain.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md), yields in [Items](ITEM_CATALOG.md). One bulk biomass fluid is the current draft, not an approved family of stronger colored fuels.

### Q-014 — Reservoir capacity and district storage
- **Decision:** Set bladder/cell capacities, reservoir minimum/maximum shapes, transfer rates and contents ownership on split/break/reformation. Ratify skeletal, thermal or spatial capacity upgrades and their recipes; none has an approved giga-capacity value. Set Storage Cortex group and query limits.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md). Separate capacity, transfer rate and reserve endurance; several physical stores may share a view, never duplicated contents.

### Q-015 — Hazardous biomass
- **Decision:** Choose whether ordinary biomass or an advanced process can spill; specify triggering conditions, warnings, affected targets, shutdown output, neutralization/recovery recipes and loss. Resolve random-storage rupture versus causal local failure. Define conserved volume, maximum active spill cells, lifetime and ownership restrictions.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md), [Items](ITEM_CATALOG.md), budgets in [Performance](PERFORMANCE.md). Until resolved, full outputs pause; no automatic destructive overflow.

### Q-016 — Farms, husbandry and food
- **Decision:** Specify crop/tree/aquatic recipes and growing conditions, retained seed/feed quantities, yield versus growth mutations, harvest rates, habitat/pen sizes and population caps. Choose entity versus contained aquaculture, bait and treasure eligibility; define food recipes/effects and supported modded inputs.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md) and [Items](ITEM_CATALOG.md). Retain field, grove and aquatic alternatives; no mandatory mob slaughter or unapproved seasons/disease simulation.

### Q-017 — Vegetation transformation
- **Decision:** For grass, bushes, logs and leaves, choose coexistence, conversion and salvage actions; list transformed forms, seed/wood/biomass yields, useful ongoing products, regrowth and maximum selected extent. Define conversion consent and one-time recovery to prevent repeated conversion payout.
- **Resolve in:** [Substrate](LIVING_SUBSTRATE_MUTATIONS.md), output recipes in [Items](ITEM_CATALOG.md). Preserve useful ground cover and canopy; exact tree behavior is not settled.

### Q-018 — Substrate maturity, reinforcement and pigment
- **Decision:** Set each maturity treatment dose/time, valid source blocks, reinforcement resistance/transfer ratings and functional-graft upgrade values. Decide ordinary adhesion versus explicit Travel Tissue, partial/full-suit eligibility, native compatibility and lining replacement. Specify dye palette, application area, persistence and preserved signal colors.
- **Resolve in:** [Substrate](LIVING_SUBSTRATE_MUTATIONS.md). Dye is cosmetic; it cannot change resource identity, ownership or functional connections.

### Q-019 — Fuel Papilla extension
- **Decision:** Set the single-cell transfer rate and biomass overhead, the extension parts/valid shapes, efficiency and throughput formula, user capacity and permissions. Account for source withdrawal = armor delivery + declared operating consumption.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md), referenced by [Substrate](LIVING_SUBSTRATE_MUTATIONS.md) and [Armor](ARMOR_EVOLUTION.md). The basic feeder is mutated substrate; multiblock support improves it, not a replacement free-standing feeder.

### Q-020 — Transport and multiblock anatomy
- **Decision:** Ratify per-carrier capacities/rates, direction/priority resolution, reservation rules, crossings and exposed-step grafts. For each multiblock specify minimum anatomy, legal extensions, maximum envelope, blocked service faces and content-preserving teardown. Set work budgets across many networks, not only per block.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md); cell anatomy in [Substrate](LIVING_SUBSTRATE_MUTATIONS.md). Existing ready/working/held/blocked operations and no-loose-output baseline remain.

### Q-021 — Mining routes and Manual Leaching
- **Decision:** Assign Manual Leaching, Descending Rootstock, Descending Cradle, Digestion Crucible and Strata Maw their preparation/access dependencies, material lists, costs, speed, finite job size and recovery. Specify stair headroom/pitch, lighting, turn/extension rules, one-block strip geometry, tendon entry, fluid/protection refusal and leached-block persistence.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md), states/reagents in [Items](ITEM_CATALOG.md). Manual Leaching is retained; Burrowing is non-excavating and not a required preliminary mining step.

### Q-022 — Temporary membrane scaffolding
- **Decision:** Choose handheld organ recipe, armor-fuel cost, range, surface attachment, collision/strength, lifespan, active-block cap and expiration across unload/reload. Decide separately whether it can form beneath a falling player, arrest a fall or support repeated midair placement.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md) and [Items](ITEM_CATALOG.md). Weak translucent temporary support is the proposed function; unrestricted early flight is not approved.

## Research and dimensional play

### Q-023 — DNA acquisition and coverage
- **Decision:** Ratify species and source attribution, specimen yields, coverage thresholds, precision limits, partial-genome uses and deterministic versus stochastic completion. Define renewable Genetic Stock recipes and live/boss sampling eligibility, rates and recovery. Shared vanilla drops cannot prove a species without a defined attribution rule.
- **Resolve in:** [Items](ITEM_CATALOG.md), preparation organs in [Blocks](BLOCK_CATALOG.md), discovery sequence in [Guide](GUIDE_PROGRESSION_TREE.md).

### Q-024 — Ownership, trade and recovery
- **Decision:** Choose player/team/world research owner, team provider, joining/leaving policy, template trade semantics and permissions for inspection, withdrawal, configuration, defense and transit. Define safe knowledge recovery after bank destruction and claim-boundary behavior.
- **Resolve in:** [Progression](PROGRESSION_MAP.md), feature-specific contracts in their catalogs and adapters in [Dependencies](DEPENDENCIES.md). Gear practice belongs to its item, not an automatically shared team skill pool.

### Q-025 — What progression gates actually control
- **Decision:** Choose personal rank, colony commissioning capability, upgraded evolution structure, or physical recipe/research prerequisites without a universal rank. Define multiplayer credit and what happens after dismantling or borrowing infrastructure. Specify continuing dimension-native operations and their import/export recipes.
- **Resolve in:** [Progression](PROGRESSION_MAP.md), then [Guide](GUIDE_PROGRESSION_TREE.md). SR and T identifiers are document coordinates until this is resolved; they do not establish a player permission system.

### Q-026 — Passenger and freight connections
- **Decision:** Choose shared gateway anatomy versus separate passenger/freight bodies, exact costs, payload/rate limits, arrival clearance and return reserves. Specify authorized destination loading for solo travel, mounts/passengers, cancellation, restart and unavailable endpoints.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md), teaching in [Guide](GUIDE_PROGRESSION_TREE.md). Actual cargo transfer is required for colony extension; no implicit remote production or unbounded chunk tickets.

### Q-027 — Configuration and monitoring
- **Decision:** Set probe actions, block-menu gestures, port state cues, controller presets, trace length/query radius, reservation diagnostics and permission scope. Specify blueprint contents, size, resource reservation, placement order, cancellation and safe relocation. Ratify Scheduler/Request Cortex bounds.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md), handhelds in [Items](ITEM_CATALOG.md). Ordinary empty-hand placement must not be intercepted by configuration actions.

### Q-028 — Helpers and Nutrient Sail
- **Decision:** Choose release inclusion, first worker jobs, station population, hatch separation, path/waypoint bounds, recall/relocation and carried-item recovery. Select Sail service and whether chunk loading exists; any loader needs owner/team quotas, fuel, expiry and restart/removal rules.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md), budgets in [Performance](PERFORMANCE.md). Stationary collection remains a complete logistics alternative.

### Q-029 — Defense and hostile interactions
- **Decision:** Set target lists, range, line of sight, damage, projectile count, costs and cooldowns for defense organs/tissue. Specify friendly-fire/PvP permissions and Dragon/Wither handling. Audit damage-to-biomass with healing, breeding and summoning; choose whether temporary defenders exist.
- **Resolve in:** [Blocks](BLOCK_CATALOG.md), ammunition in [Items](ITEM_CATALOG.md). No automatic raids, compulsory slaughter route or universal boss-control assumption.

### Q-030 — Tools and weapons
- **Decision:** Choose permanent tool/weapon trees versus refittable tool profiles, independent learning rules, costs, working geometry and material harvest requirements. Define portable piercing, grasping and ranged roles and their prepared ammunition. Decide whether armor's learning rules also apply to tools; do not copy them by implication.
- **Resolve in:** [Items](ITEM_CATALOG.md), supported preparation in [Blocks](BLOCK_CATALOG.md).

### Q-031 — Fold and boss horizon
- **Decision:** Ratify Fold ecology, native phase periods, shelter/return bootstrap, Adaptive Gel role and process recipes. Define Manyfold arena, attack/response states, valid player counterconditions, solo operation, interruption, reward and rematch rules; choose expansion scope.
- **Resolve in:** [Progression](PROGRESSION_MAP.md), [Blocks](BLOCK_CATALOG.md), [Items](ITEM_CATALOG.md). No branch-exchange reward, infinite fuel or boss-required first foothold.

### Q-032 — First discovery and guide delivery
- **Decision:** Specify guide acquisition/recovery, first ingredient discovery, chapter visibility, late-found item access, cooperative evidence and exact completion triggers. Choose story voice and origin reveals. Decide introductory survival slice and observable success criteria before assigning release milestones.
- **Resolve in:** [Guide](GUIDE_PROGRESSION_TREE.md), overview in [Progression](PROGRESSION_MAP.md). Advancements remain; FTB Quests is pack-author scope.

## Delivery constraints

### Q-033 — Visual and accessibility acceptance
- **Decision:** Approve palette, anatomy silhouettes, mutation attachments, texture resolution, translucency interiors, reduced-motion/particle settings, sound/gore intensity, subtitles and non-color-only indicators. Choose human-owned reference captures and supported views/poses.
- **Resolve in:** Feature catalogs for appearance; [Testing](TESTING.md) for capture acceptance. No visual quality claim follows merely from a passing structural test.

### Q-034 — Release scope and pacing
- **Decision:** Select the first complete survival loop, included organ/item families and subsequent milestones. Set measured pacing targets for first armor, automation, each dimensional settlement and maximum processing. Identify optional routes and required capability demonstrations; do not make every catalog row mandatory.
- **Resolve in:** [Progression](PROGRESSION_MAP.md); approved implementation tasks use [Task template](TASK_TEMPLATE.md).

### Q-035 — Dependencies and compatibility
- **Decision:** Ratify exact artifacts, required/optional status, licenses and absence behavior for JEI, Curios, guide, animation, diagnostics and UI/multiblock tooling. Choose first storage/energy/food/claims integrations and pack recipe override schema.
- **Resolve in:** [Dependencies](DEPENDENCIES.md). Existing bootstrap pins are not a finalized gameplay integration matrix.

### Q-036 — Production verification and bounded simulation
- **Decision:** Set feature work/state/packet/entity/geometry budgets, shared admission scopes and stress targets. Specify save versions/migrations, transaction recovery, client capture/data-generation gates, packaged deployment tests and ktask independent completion verification. Approve CI, branch protection and executor policy separately.
- **Resolve in:** [Performance](PERFORMANCE.md), [Testing](TESTING.md), [Architecture](ARCHITECTURE.md), [ktask](KTASK.md). No gameplay, autonomous workers, publication or HUMAN-gate acknowledgment is authorized by documentation cleanup.

### Q-037 — Source and asset licensing
- **Decision:** Resolve the root license versus archived metadata mismatch and choose source/asset licensing and provenance requirements before release. Preserve existing licenses until the owner decides.
- **Resolve in:** [Dependencies](DEPENDENCIES.md) and [Decisions](DECISIONS.md). This is a release blocker, not authority to relicense.
