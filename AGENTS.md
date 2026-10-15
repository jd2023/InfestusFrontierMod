# Rules for every agent

These rules apply to any agent: Codex, Claude or another tool. Read this file, then [BIG_PICTURE.md](BIG_PICTURE.md). Then read only what your role and story need:

| Role | Reads |
|---|---|
| Coder (planner, worker, repairer) | [ENGINEERING.md](ENGINEERING.md) and the one design document the story names |
| Reviewer | [REVIEW.md](REVIEW.md) |
| Nobody | `docs/archive/`. It is history for the owner. |

## Repository

- Work on branch `v4`. Push only to `origin/v4`.
- Never push to `main`. Never force-push. Never create a remote, a tag or a release.
- The commit author is the repository owner. Local git config already sets it. Do not change it.
- No `Co-Authored-By` lines. No "Generated with" lines. No agent or model names in commits.
- Commit message: one imperative line under 72 characters. Add a body only when the diff does not explain itself.
- Stage files by name. Never `git add -A`. Never commit `build/`, `runs/`, `.ktask/` or IDE files.
- Start from a clean committed tree. Do not touch files outside your story's `Files`. The only exception is the `Status` line of your story in the sprint file. Do not stash other people's work.
- Run every command in the foreground and wait for it. Do not start background jobs and end your turn.

## Roles

- **Owner** (human): makes product decisions and plays the build at the end of each sprint. Nothing else goes to the owner. Tuning, API research and technical choices are engineering work.
- **Planner** (strong model, with the owner): sets direction, code structure and checks. Writes the sprint file: stories with exact names, numbers and interfaces. Reads the real code first.
- **Worker** (cheap model, Sonnet): implements one story exactly as written. Does not redesign, does not widen scope.
- **Repairer** (cheap model, Sonnet): fixes reviewer findings, one regression test each. Does not redo accepted work.
- **Reviewer** (strong model, never the same run as the worker): reads the whole diff and the tests. Says `APPROVED` or `REJECTED` with numbered findings. Never edits.
- **Artist** (separate track): delivers assets for `art` stories from `docs/sprints/ART.md`. Textures come from an image model (Astra). GeckoLib models and animations come from a strong model (Opus or Fable). Writes no Java.

## Running a story

Each role starts from one line. It reads the files the line names, in this order: this file, BIG_PICTURE.md, then its role document. One story per run. When the story is done, the run ends with a report or a verdict and does not start the next story.

| Role | Start line |
|---|---|
| Planner | `Plan sprint S02 into docs/sprints/S02.md. Follow AGENTS.md and ENGINEERING.md, section Stories and sprints.` |
| Worker | `Do story IF-014 from docs/sprints/S01.md. Follow AGENTS.md.` |
| Reviewer | `Review story IF-014 from docs/sprints/S01.md, commits <from>..<to>. Follow REVIEW.md.` |
| Repairer | `Repair story IF-014 from docs/sprints/S01.md. Findings are below. Follow AGENTS.md.` |
| Artist | `Do art story ART-003 from docs/sprints/ART.md. Follow AGENTS.md.` |

The sprint file is the queue. Today the owner pastes these lines by hand. An orchestration tool is being built on the side and will read the same file. Runner state such as `.ktask/` stays local and is never committed.

## Two rules that stop a story

1. If the story cannot be done inside its listed files, stop and report which file or decision is missing.
2. If the story seems to need patching, bypassing or re-implementing engine behavior (placement, drops, chunk save, neighbor updates), stop and report. We use vanilla and NeoForge as they are.

## Writing rules

They apply to code, docs, stories, reports and reviews.

- Short sentences. Plain words. Give the number, the file, the name.
- One fact lives in one place. Link to it. Do not repeat it.
- "Proposed" means not decided. A number in a design document is a starting value until play testing confirms it.
- No history, no process commentary and no praise inside a specification.
- Do not add a document. Add a section to the owner document instead.
