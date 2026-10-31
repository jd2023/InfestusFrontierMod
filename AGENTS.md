# Rules for every agent

These rules apply to any agent: Codex, Claude or another tool. Read this file, then [VISION.md](VISION.md). Then read only what your role and story need:

| Role | Reads |
|---|---|
| Builder | [ROADMAP.md](ROADMAP.md), [ENGINEERING.md](ENGINEERING.md) and the design documents the work needs |
| Worker | [ENGINEERING.md](ENGINEERING.md) and the one design document the task names |
| Reviewer | [REVIEW.md](REVIEW.md) |
| Nobody | `docs/archive/`. It is history for the owner. |

## Repository

- Work on branch `v4`. Push only to `origin/v4`.
- Never push to `main`. Never force-push. Never create a remote, a tag or a release.
- Every commit, including ktask checkpoints, must use `Illya Yalovyy <yalovoy@gmail.com>` ([IllyaYalovyy](https://github.com/IllyaYalovyy)) as its author. Verify the effective author before committing. Never substitute a tool identity. Preserve this identity in local git config and runner settings.
- No `Co-Authored-By` lines. No "Generated with" lines. No agent or model names in commits.
- Commit message: one imperative line under 72 characters. Add a body only when the diff does not explain itself.
- Stage files by name. Never `git add -A`. Never commit `build/`, `runs/`, `.ktask/` or IDE files.
- Start from a clean committed tree. Touch only what the change needs; when a story lists `Files`, stay inside them. Do not stash other people's work.
- Run every command in the foreground and wait for it. Do not start background jobs and end your turn.

## Roles

- **Owner** (human): makes product decisions and plays the build when a milestone's organs are in. Nothing else goes to the owner. Tuning, API research and technical choices are engineering work.
- **Builder** (strong model, with the owner): takes a goal, its constraints and one line of acceptance from the owner, says how in a few lines, then implements, gates, and hands the commits to a reviewer. Reads the real code first. Hands a cheap model only work a machine judges.
- **Worker** (cheap model): a mechanical task from the builder: data files, a further copy of an existing pattern, renames, test ports. Never a framework API shape or a design decision.
- **Reviewer** (strong model, a fresh run, never the builder's): reads the whole diff and the tests. Says `APPROVED` or `REJECTED` with numbered findings. Never edits. One review per feature; a repair goes back to the same reviewer.
- **Artist** (separate track): delivers assets for `art` stories from `docs/sprints/ART.md`. Textures come from an image model (Astra). GeckoLib models and animations come from a strong model (Opus or Fable). Writes no Java.

## Running work

The builder runs from one session: the owner states the goal; the builder implements in the main checkout on `v4` (no worktrees; the owner's uncommitted files are never staged), runs `bash scripts/verify.sh`, commits as the owner, spawns a fresh reviewer on the commits (`Review commits <from>..<to>. Follow REVIEW.md.`), repairs the findings, and pushes on `APPROVED`. The owner pastes nothing between runs and plays the build when a milestone's organs are in. A sprint file in `docs/sprints/` is written only when a milestone needs a plan the owner will read; a change needs no story. Art keeps its queue: `Do art story ART-004 from docs/sprints/ART.md. Follow AGENTS.md.`

## Feature completion

- A task that adds a block, organ or item also updates the playable demo launched by `scripts/demo.sh`. Add a labeled, usable exhibit to the appropriate scenario under `src/testMod/resources/captures/`; update `scripts/demo.sh` when launch behavior needs to change. The default demo must make the new content discoverable without commands.
- Keep the in-game book current in the same task. Add or update its entry, recipes, prerequisites and discovery links to match shipped behavior. Changes to existing content also update affected book text and demo exhibits.
- Include demo and book files in the task's scope and acceptance criteria. Verify the demo exhibit and book entry through the existing capture workflow. A feature is incomplete while either is missing or stale. Separate book tasks are for broader editorial work, not deferred documentation of new content.

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
