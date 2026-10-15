# Rules for every agent

These rules apply to any agent: Codex, Claude or another tool. Read this file, then [BIG_PICTURE.md](BIG_PICTURE.md). Then read only what your role and task need:

| Role | Reads |
|---|---|
| Coder (planner, worker, repairer) | [ENGINEERING.md](ENGINEERING.md) and the one design document the task names |
| Reviewer | [REVIEW.md](REVIEW.md) |
| Nobody | `docs/archive/`. It is history for the owner. |

## Repository

- Work on branch `v4`. Push only to `origin/v4`.
- Never push to `main`. Never force-push. Never create a remote, a tag or a release.
- The commit author is the repository owner. Local git config already sets it. Do not change it.
- No `Co-Authored-By` lines. No "Generated with" lines. No agent or model names in commits.
- Commit message: one imperative line under 72 characters. Add a body only when the diff does not explain itself.
- Stage files by name. Never `git add -A`. Never commit `build/`, `runs/`, `.ktask/` or IDE files.
- Start from a clean committed tree. Do not touch files outside your task. Do not stash other people's work.
- Run every command in the foreground and wait for it. Do not start background jobs and end your turn.

## Roles

- **Owner** (human): makes product decisions. Nothing else goes to the owner. Tuning, API research and technical choices are engineering work.
- **Planner** (strong model, with the owner at milestone gates): writes tasks with exact names, numbers and interfaces. Reads the real code first.
- **Worker**: implements one task exactly as written. Does not redesign, does not widen scope.
- **Repairer**: fixes reviewer findings, one regression test each. Does not redo accepted work.
- **Reviewer**: reads the whole diff and the tests. Says `APPROVED` or `REJECTED` with numbered findings. Never edits.

## Two rules that stop a task

1. If the task cannot be done inside its listed files, stop and report which file or decision is missing.
2. If the task seems to need patching, bypassing or re-implementing engine behavior (placement, drops, chunk save, neighbor updates), stop and report. We use vanilla and NeoForge as they are.

## Writing rules

They apply to code, docs, tasks, reports and reviews.

- Short sentences. Plain words. Give the number, the file, the name.
- One fact lives in one place. Link to it. Do not repeat it.
- "Proposed" means not decided. A number in a design document is a starting value until play testing confirms it.
- No history, no process commentary and no praise inside a specification.
- Do not add a document. Add a section to the owner document instead.
