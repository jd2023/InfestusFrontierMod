# Repository instructions

This file is the single source of agent rules. Other documents add procedure or
specification; they do not restate these rules.

Roles: a planner (strong model, with the human at milestone gates) owns design,
interfaces and `.ktask/tasks.md`. A worker implements exactly one small packet.
A repairer fixes reviewer findings. A reviewer accepts or rejects. ktask alone
advances the queue.

1. One packet, one outcome. Change only the packet's `Files`. If it cannot be done
   within them, stop and report FAILED naming the missing file or decision. Do not
   widen the task, redesign an interface or add frameworks.
2. Frozen paths: `gradle/integration/**`, `.ktask/**`, `scripts/**`, `build.gradle`,
   `settings.gradle`, `gradle.properties`. Workers never edit them unless the packet
   lists them. A repairer may fix a genuine harness defect with a regression test;
   raising a limit or relaxing an assertion to pass is never a fix.
3. Tests first. Every behavior and every reviewer finding gets a test that fails
   without the change. Never weaken a check or replace a visual golden to pass.
4. Deep modularity: one behavior owner, small interfaces, inward acyclic
   dependencies, pure rules in `:core`. See `docs/DEVELOPER_GUIDE.md`.
5. Performance is correctness: hard bounds, back-pressure, loaded chunks only.
   See `docs/PERFORMANCE.md`.
6. Start from a clean committed checkpoint; preserve unrelated user files; commit
   one coherent scope and push to the configured branch. No force push, main
   merge, new remote, release or edits to the sibling prototype folder.
7. Never fabricate results, self-approve, edit queue progress, or stage, commit
   or stash anything under `.ktask/`. Stage the packet's `Files` by name, never
   `git add -A`. Human input is
   only for product decisions and external authority.

`VISION.md` is a draft: explicit user commitments constrain work; proposals and
open questions do not authorize scope changes. The sibling prototype folder is
read-only reference evidence, not production code or specification.
