# Rules for every agent

These rules apply to any agent: Codex, Claude, or another tool. Read this file, then [BIG_PICTURE.md](BIG_PICTURE.md), then only what your task names.

## Repository

- Work on branch `v4`. Push only to `origin/v4`.
- Never push to `main`. Never force-push. Never create a remote, a tag or a release.
- The commit author is the repository owner. Local git config already sets it. Do not change it.
- No `Co-Authored-By` lines. No "Generated with" lines. No agent or model names in commits.
- Commit message: one imperative line under 72 characters. Add a body only when the diff does not explain itself.
- Stage files by name. Never `git add -A`. Never commit `build/`, `runs/`, `.ktask/` or IDE files.
- Start from a clean committed tree. Do not touch files outside your task. Do not stash other people's work.

## Roles

- **Owner** (human): makes product decisions. Nothing else goes to the owner. Tuning, API research and technical choices are engineering work.
- **Planner** (strong model, with the owner at milestone gates): writes tasks with exact names, numbers and interfaces. Reads the real code first.
- **Worker**: implements one task exactly as written. Does not redesign, does not widen scope.
- **Reviewer**: reads the whole diff and the tests. Says `APPROVED` or `REJECTED` with numbered findings. Never edits.

## How to do a task

1. Read the task. If it cannot be done inside its listed files, stop and report which file or decision is missing.
2. Write the tests first. Run them. See them fail for the stated reason.
3. Implement in the owning module. Keep the public interface small.
4. Run the task's `Done` commands. Then run `bash scripts/verify.sh`. It must pass.
5. Walk through [docs/process/REVIEW_CHECKLIST.md](docs/process/REVIEW_CHECKLIST.md) against your diff.
6. Commit, push to `origin/v4`, and report: what you did, what you did not do, what you measured.

Run every command in the foreground and wait for it. Do not start background jobs and end your turn.

## Code rules

- One behavior has one owner module. See [docs/process/ARCHITECTURE.md](docs/process/ARCHITECTURE.md).
- Dependencies point inward: NeoForge adapters → feature API → pure rules. No cycles.
- `core/` and `ui/` are plain Java. They never import Minecraft, NeoForge or any library. The build enforces this.
- Every loop, scan, queue, buffer, inventory and packet has a hard limit, written in code and checked by a test. See [docs/process/PERFORMANCE.md](docs/process/PERFORMANCE.md).
- No chunk loading unless a design document names it and the player pays for it.
- Check `isLoaded` before reading any neighbor block. Vanilla superclasses also read neighbors.
- The server decides everything. The client sends intents and shows snapshots.
- Use vanilla and NeoForge behavior as it is. Do not patch, bypass or re-implement engine rules such as placement, drops, chunk save or neighbor updates. If a task seems to need this, stop and report.
- Validate everything, then change state. A refusal leaves every input unchanged.
- Never weaken a test, raise a limit, skip a check or replace a golden image to get green.
- Comments state a contract. Comments that narrate the code are defects.

## Writing rules

They apply to code, docs, tasks, reports and reviews.

- Short sentences. Plain words. Give the number, the file, the name.
- One fact lives in one place. Link to it. Do not repeat it.
- "Proposed" means not decided. A number in a design document is a starting value until play testing confirms it.
- No history, no process commentary and no praise inside a specification.

## Review rules

Blocking, reject the task:

- A `Do`, `Tests` or `Done` item is missing, untested or was not run.
- Data loss, item duplication, a crash, or a previously accepted behavior broke.
- Unbounded work, implicit chunk loading or another PERFORMANCE.md violation.
- An edit outside the task's `Files`, or a changed limit or assertion without a regression test.
- A visual defect a player would notice, for tasks with captures.

Everything else goes under a final `FOLLOW-UP:` heading and never blocks. The planner triages follow-ups at the milestone gate.

Never approve because the build passed or the worker said it works. Read the diff and the tests.
