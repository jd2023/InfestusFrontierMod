# Implementation packet contract

Expensive models plan; a cheap model implements. A packet is ready only when a
worker can finish it without making a design decision or reading a whole catalog.

## Size

One packet is one Kind and one outcome: at most about 12 files and 400 changed
lines. If the title needs "and", split it. A content feature is a chain:

| Kind | Contains | Evidence |
|---|---|---|
| rules | Pure `:core` types and JUnit tests; no Minecraft types | rules |
| platform | Block, item, block entity, codec, menu wiring, blockstate properties, capture scene code and `visual-setup.json`; GameTests; placeholder assets allowed | game |
| data | Recipes, tags, loot, translations and their GameTests | game |
| art | Textures, models, blockstates, item icons, CREDITS provenance. Assets only, never Java; made by the visual model | visual |
| complete | Guide entry, advancement or discovery step, `coverage.json` contribution and checkpoint advance | game, visual |
| qualify | A scripted route or soak over accepted content; no new behavior | game |
| repair | One reviewer finding or follow-up with its regression test | as needed |

Visual assets are a separate craft. The scene that photographs a feature is a
platform packet and comes first, with placeholder assets; the art packet then
replaces the assets and its review inspects the fresh captures. `.ktask/codex-by-kind`
routes art packets to the visual model.

Only the `complete` packet owns the catalog IDs in `Blocks` and
`.ktask/content-plan.json`, and advances `content-checkpoint.json` to its ID.
Earlier packets of the chain use `Blocks: none`.

## Fields

| Field | Meaning |
|---|---|
| IF-nnn title | One observable outcome |
| Milestone, Owner, Depends | As in ARCHITECTURE; Depends lists accepted packet IDs |
| Blocks | Catalog block IDs completed here, or `none` |
| Kind | One row of the table above |
| Read | At most five exact files or catalog entries the worker needs |
| Files | Exact files to create or modify, tests included. This is an allowlist |
| Do | Numbered steps with exact type names, signatures, IDs and numbers |
| Tests | Named tests written first, each with the assertion it makes |
| Done | Exact commands that must pass |
| Not | Adjacent behavior that belongs to another packet |
| Bounds | Hard work and state limits |
| Evidence | rules, game, visual, integration or soak |

The planner decides every name, number and interface before the packet enters
the queue, reading the existing code so that `Do` matches real signatures. It
consults `docs/REVIEW_CHECKLIST.md` and writes each applicable item as a `Tests`
line. Common invariants (save/reload, break/replace, refusal leaves state
unchanged) are written into the packet that owns them, not inherited silently.

## Milestones

Packets are executable only down to the next `HUMAN:` gate. Entries below it in
the old Contract/Red/Accept format are milestone outlines: inputs for the next
planning session, never handed to a worker. At each gate the human playtests, a
planning session triages `.ktask/logs/follow-ups.md`, expands the next milestone
into packets, keeps `.ktask/content-plan.json` consistent, leaves `.ktask/`
uncommitted, runs
`bash .ktask/accept.sh --checkpoint`, and the human runs `ktask ack`.
