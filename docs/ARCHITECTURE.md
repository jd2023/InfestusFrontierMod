# Production architecture — bootstrap

## Implemented boundaries

```text
Minecraft / NeoForge
        |
root production mod (composition and future platform adapters)
        |
:core (Java-only deterministic infrastructure/rules)

testMod → production mod + core       development only
legacy/forge-1.19.2                   outside the build
```

`:core` exposes `foundation.TickQuota`: one bounded, constant-space admission
policy. It has no world, thread pool, persistent queue, game registry, third-party
dependency or automatic simulation. The limit's owning feature must choose its
scope and ratify a budget. The class is single-owner/single-thread, not thread-safe.
Pure tests and a build-time empty-classpath check enforce this initial boundary.

The root mod is only a composition root. It has no blocks, items, UI or world
callbacks. The development mod in `src/testMod` asserts real loader/core behavior
in a Minecraft server. It is loaded only for GameTests and excluded from the
release JAR, which is inspected by `verifyDistribution`.

Core classes are included in the production JAR and grouped into the production
mod's development source sets. The test source set gets its own metadata, mod ID
and run directory. This follows the source-set/run mechanisms documented in
[ModDevGradle](https://github.com/neoforged/ModDevGradle#isolated-source-sets);
the pinned plugin and actual GameTest run are the compatibility check.

## Rules for growth

Add feature modules only with their first approved use: ecology, genetics,
equipment evolution, construction, transport, progression. The names are a map
of responsibilities, not a mandate to create empty packages or generic services.

Pure rules stay independent of Minecraft when practical. Each feature hides
storage and platform adapters behind commands, queries and immutable outcomes.
Optional-mod code stays at the edge. Introduce additional Gradle subprojects where
compile-time isolation justifies the packaging cost; enforce package boundaries
within platform code as those features appear.

Before the first menu, establish and test a small UI foundation. Before scalable
growth, establish shared admission, bounded traversal and chunk-safe placement
contracts. Do not paste prototype infrastructure into gameplay modules.

No production data format, networking protocol, content registry catalog or world
generation contract is finalized by this bootstrap. Each needs its own task and
failure/migration tests. `infestusfrontier` is retained as the mod ID; this does not
make old saves or prototype registries compatible.
