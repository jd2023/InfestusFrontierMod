# Infestus Frontier developer guide

## Non-negotiable architecture rule: deep modularity

Infestus Frontier must be built from deep, cohesive, independently testable modules with small and stable interfaces. This applies to every subsystem: mutation rules, world growth, machines, inventories, persistence, networking, UI, rendering, animation, progression, world generation, and compatibility integrations.

A module is **deep** when it hides substantial policy and implementation behind a much smaller interface. A directory full of public classes is not a module. A thin wrapper that merely forwards calls is not a useful abstraction.

Code must not be organized only by technical type into global buckets where one feature is scattered among `block`, `menu`, `network`, `screen`, and `event` packages. A feature module should own its behavior and expose only the operations and immutable state that other modules actually need.

## Required properties of a module

Every module must have:

- one clearly stated responsibility;
- a small public API, preferably expressed through immutable values and narrow interfaces;
- hidden implementation details, using package-private visibility by default;
- explicit ownership of its data and invariants;
- explicit dependencies and a documented dependency direction;
- deterministic tests for its internal rules;
- contract tests at each Minecraft, network, persistence, or third-party boundary;
- no reliance on another module reaching into its internal state;
- diagnostics that explain failures without exposing or coupling callers to internals.

Public surface area is a maintenance cost. New public classes and methods require a concrete caller outside the module and review of the resulting coupling.

## Dependency direction

Dependencies must point inward toward stable rules:

```text
Minecraft / NeoForge / third-party APIs
                 |
          adapters and UI
                 |
      feature application API
                 |
       deterministic domain rules
```

Domain rules must not depend on screens, rendering, registries, packets, block entities, or optional mods. When practical, they should not depend on Minecraft classes at all.

Minecraft-facing code translates platform events and data into calls on a feature API. UI code observes immutable view state and sends explicit user intents. Optional-mod integrations are adapters at the outer edge; core behavior must compile and run without them.

Circular dependencies between modules are prohibited. If two modules appear to require each other's internals, introduce a smaller shared contract or revise ownership rather than adding callbacks until the cycle is hidden.

## Vertical feature modules

Production code should be organized primarily by cohesive feature, not only by framework class type. A representative structure is:

```text
org.jd.infestusfrontier
├── mutation
│   ├── api             # stable commands, queries, and immutable results
│   ├── domain          # mutation rules and invariants
│   └── neoforge        # item components, persistence, and platform adapters
├── substrate
│   ├── api
│   ├── domain
│   └── neoforge
├── chamber
│   ├── api
│   ├── domain
│   └── neoforge        # block, block entity, menu adapter
├── ui
│   ├── foundation      # internal UI framework and visual contracts
│   └── chamber         # chamber-specific composition
└── integration
    ├── jei
    ├── jade
    └── create
```

The exact names may change during production design. The ownership and dependency rules may not.

Use separate Gradle subprojects when a boundary benefits from compile-time dependency enforcement or independent test fixtures. Within one subproject, use package-private implementation types and architecture tests to enforce the same rule.

## Internal frameworks and low-level components

Low-level mechanics that support feature code must live in a dedicated internal module with a deliberate interface from their first production use. They must never be copied across features or mixed into gameplay classes.

This includes, but is not limited to:

- UI layout, themes, drawing primitives, widgets, input handling, tooltips, and screen capture;
- menu synchronization and common slot-routing policies;
- mutation serialization and compatibility checks;
- network message registration, validation, and dispatch;
- animation state selection and renderer utilities;
- multiblock discovery and validation;
- world-spread search and placement policies;
- advancement and recipe-discovery helpers;
- optional-mod detection and integration lifecycle.

An internal framework may start tiny. It should contain only required policy, not speculative generality, but it must establish a coherent boundary immediately. Later consumers extend or reuse that module; they are never allowed to copy its implementation.

### UI-specific rule

Using native NeoForge screens does not permit raw GUI code to spread through the mod. Native APIs are the platform boundary, not the architecture.

The internal UI foundation must own shared concerns such as palette/theme tokens, panels, slots, progress indicators, spacing, animation clocks, accessibility hooks, and deterministic capture support. Feature screens compose those components from feature-specific view state. They must not contain mutation rules, directly manipulate block entities, invent their own synchronization, or duplicate drawing primitives.

The prototype's `ui.foundation` now owns shared palette, panels, slots, progress/animation policy and native short-word synchronization. Its `BlockMenuAccess` owns block-menu identity, world/range validation, command cooldown and refresh admission; the Synaptic Console and mining console reuse it while retaining feature-specific permissions and held-tool rules. Mutation Chamber and these consoles compose the drawing primitives; the existing shared `client.capture` adapter owns acknowledged world/GUI captures. Extend these owners rather than copying infrastructure into the next feature. The [downward experiments](DOWNWARD_MINING.md) reuse `MiningConsole`/`MiningMenu`, `MiningCargo` and `MiningSafety`; the three state machines do not own their own packets, inventory codecs or loot infrastructure. See [UI_ARCHITECTURE.md](UI_ARCHITECTURE.md), [LOGISTICS_CONSOLE.md](LOGISTICS_CONSOLE.md) and [MINING_ORGANS.md](MINING_ORGANS.md).

## Simple interfaces

The `burrowing` module owns temporary traversal, not mining loot or armor fuel.
Package-private `PassageLease` owns bounded deadlines; `PassageLedger` owns native
save/restoration admission; `ShadowRock` owns reversible rock-state and fluid/piston
policy; `PassageMotion` validates and commits short native movement steps. Grafts
are private item components behind the registration facade. Reuse `WorkBudget` and
`BioArmor.spendFuel`; do not copy storage, networking, input or fuel accounting into
the next ability. The opt-in lab/capture remains separate. See [BURROWING.md](BURROWING.md).

Client HUD preferences belong to package-private `bioarmor.client.SuitHudOptions`. The pure `ui.foundation.HudLayout` owns GUI-coordinate clamping and non-overlapping panel geometry; it has no NeoForge or armor dependencies. Client composition registers NeoForge's existing `ConfigurationScreen` framework for editing/saving preferences, rather than introducing feature-local widgets or config persistence. Settings never cross into server-side fuel policy.

Suit fuel belongs to `bioarmor`: package-private pure `SuitReserve` owns bounds/dose rules, and package-private `SuitFuel` owns the item component and explicit native feeding interaction. It registers through the existing armor component registry. `BioArmor.fuel` exposes only an immutable readout (absent for an incomplete set). `bioarmor.client.SuitHud` composes that readout through the generic `ui.foundation.HudMeter`; neither renderer owns refueling, persistence or ability rules. The optional HUD capture composes `PhaseCapture` and is isolated from ordinary gameplay. Extend these owners for later suit metabolism instead of storing another fuel value in a player/HUD/Curios adapter.

Ground excavation adds package-private `StripPlan` (pure bounded geometry) and `StripCycle` (finite plan/cursor policy), behind the existing `DownwardMining` facade and `DownCell` platform adapter. It reuses the same mining safety, transaction, cargo, biomass endpoint and native console owners. `ExcavationSurfaces` owns both manual treatments and their shared admission/cooldown boundary; its native host identity check does not depend on the composition registry. `groundFrame` is a construction query used by the disposable lab/tests, not a public mutator. `ExcavationLab` owns only its explicitly enabled fixed fixture; `ExcavationCapture` composes the existing shared capture protocol. Do not put fixture top-ups or acceleration into gameplay state machines.

Mining surface mutations are owned by package-private `ExcavationSurfaces`: passive stairs, fixed host identity and bounded manual leaching. It reuses `MiningSafety` for the terrain/protection boundary and `MiningEdits` for loaded-state preflight and bounded commit/rollback. `DownCell` shares that transaction owner; do not copy another rollback loop into a future organ. Root section authorization belongs in `RootCycle`, not its screen. The native menu sends only the existing explicit action intent. These modules add no public world-editing API.

Prefer APIs shaped as commands and queries:

- commands express intent, validate inputs, and return an explicit result;
- queries return immutable snapshots rather than mutable handlers or internal collections;
- persistence uses versioned codecs owned by the module;
- events describe facts that already occurred and do not expose mutable internals.

Avoid service locators, global mutable state, reflection between our own modules, public fields, and interfaces that mirror every implementation method. Do not leak NeoForge or third-party types through a domain API unless the module's purpose is specifically to adapt that API.

## Testing requirements

Each module needs a layered feedback loop:

1. Pure tests validate rules, invariants, edge cases, and failure behavior in milliseconds.
2. Contract tests validate the public API without relying on implementation details.
3. Adapter tests validate serialization, networking, registries, menus, and third-party boundaries.
4. GameTests validate server-authoritative behavior in a running Minecraft world.
5. Deterministic client captures validate screens, models, particles, and animations where pixels matter.
6. Compatibility profiles validate optional adapters both with and without their target mod.

A test that requires another module's internals indicates either a missing public contract or an incorrect module boundary. Tests must not weaken encapsulation for convenience.

Every defect fix begins with the narrowest regression test that can reproduce it. Every module also retains broader integration coverage for adjacent behavior.

## Change discipline

Before changing a module:

1. Start from a clean, committed Git state.
2. State the module and public contract in scope.
3. Identify callers and adjacent regression risks.
4. Add or update the narrowest meaningful test.
5. Change internals without expanding the public interface unless necessary.
6. Run module tests, adapter tests, then the authoritative full gate.
7. Review the staged diff for boundary violations and unrelated changes.
8. Commit one coherent change.

Cross-module changes require an explicit reason and must preserve dependency direction. Opportunistic refactoring outside the declared scope belongs in a separate task and commit.

## Review checklist

A change is not complete until all answers are satisfactory:

- Is there one obvious module that owns the new behavior?
- Is the public interface smaller and more stable than the hidden implementation?
- Are invariants enforced inside the owning module?
- Can callers use the behavior without knowing storage, rendering, networking, or library details?
- Did any low-level code leak into a feature or get duplicated?
- Does dependency direction remain acyclic and inward?
- Are failure modes explicit and tested?
- Can the module be tested quickly without launching Minecraft where appropriate?
- Are platform and optional-mod boundaries tested separately?
- Did the full verification gate pass from the committed structure?

If ownership is ambiguous or the interface grows with every feature, stop implementation and revise the architecture first.

## Performance and server safety

Visual development must also follow the anatomy-first direction in [LIVING_COLONY_DIRECTION.md](LIVING_COLONY_DIRECTION.md): readable biological form and deliberately mapped material art, with in-game captures to assess it. A mechanical accent is permitted; a mechanical silhouette disguised by a noisy texture is not the target.

Performance is part of correctness and must be discussed for every change that can materially affect server work, memory, networking, persistence, chunk loading, entity counts, or client rendering. All scalable work requires explicit bounds and a stress case. In particular, automatic systems may not create unbounded loose item entities or depend on chunk unloads to control their cost.

Follow the mandatory budgets, failure analysis, and verification rules in [PERFORMANCE.md](PERFORMANCE.md).
