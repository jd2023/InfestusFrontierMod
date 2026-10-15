# Task template

A strong model plans. A cheaper model implements. A task is ready only when a worker can finish it without making a design decision or reading a whole catalog.

## Size

One task, one kind, one outcome. About 12 files and 400 changed lines at most. If the title needs "and", split it.

| Kind | Contains | Evidence |
|---|---|---|
| rules | Pure `core` types and JUnit tests. No Minecraft types | rules |
| platform | Block, item, block entity, codec, menu wiring, GameTests. Placeholder assets allowed | game |
| data | Recipes, tags, loot, translations and their GameTests | game |
| art | Textures, models, blockstates, icons, CREDITS line. Assets only, no Java | visual |
| complete | Guide entry, advancement or discovery step | game, visual |
| qualify | A scripted route or soak over accepted content. No new behavior | game |
| repair | One reviewer finding with its regression test | as needed |

A content feature is a chain: platform with placeholder art, then art, then data, then complete.

## Fields

```text
IF-nnn  <one observable outcome>
Milestone: M1
Owner: processing            # a module from ARCHITECTURE.md
Kind: platform
Depends: IF-012              # accepted tasks only
Read: docs/design/BLOCK_CATALOG.md#t0-03--digestive-sac, src/main/java/.../processing/   # at most five
Files: src/main/java/.../processing/DigestiveSacBlock.java, ...   # exact allowlist, tests included
Do:
  1. Create class X with method Y(signature). Exact names, IDs and numbers.
  2. ...
Tests:
  - testName: what it asserts, and why it fails before the change
Done:
  - ./gradlew :core:test
  - bash scripts/verify.sh
Not: adjacent behavior that belongs to another task
Bounds: hard work and state limits, with numbers
Evidence: rules | game | visual
```

The planner decides every name, number and interface before the task enters the queue, after reading the real code so `Do` matches real signatures. Common invariants (save and reload, break and replace, refusal leaves state unchanged) are written into the task that owns them.

## Milestone gates

A `HUMAN:` line in the queue is a milestone gate. At the gate the owner plays the build, the planner triages follow-ups, and the planner writes the next batch of tasks. Plan only the next milestone in this detail. Later milestones stay as one-line outcomes in [../design/PROGRESSION_MAP.md](../design/PROGRESSION_MAP.md).

## Runner

The queue can run under `ktask` (`ktask init`, then `ktask resume`). Its folder `.ktask/` is local state and is never committed. Any runner works as long as it follows [AGENTS.md](../../AGENTS.md).
