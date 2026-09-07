# Stable project context

Infestus Frontier: a distributed biological colony, mutation and original living
construction across Overworld, Nether and End. Read `VISION.md` and respect its
distinction between commitments, proposals and unresolved decisions.

Read `AGENTS.md`, `docs/DEVELOPER_GUIDE.md`, `docs/PERFORMANCE.md` and the owning
specification. Deep modules, acyclic boundaries, server authority, conservation,
hard bounds and a demonstrable feedback loop are mandatory. Low-level UI and
world infrastructure must not be mixed with feature logic.

Minecraft 1.21.1 / NeoForge 21.1.249 / Java 21. The root mod is a clean foundation;
`:core` is Java-only, and `testMod` is development-only. `legacy/forge-1.19.2` is
archived reference, not an active subproject. The sibling V3 experiment is not a
production specification. Do not edit it or copy its gameplay/assets/saves.

Advancements and an in-game guide stay. JEI and Curios are priorities. FTB Quests
is for pack authors. Dependency additions and gameplay scope need approval.

Authoritative local gate: `./.ktask/verify.sh`. Its log is in build/verification.
No automatic publication, merging to main, remote writes, license changes or
golden replacement. HUMAN gates are owner-only. This queue has no approved
implementation tasks yet.
