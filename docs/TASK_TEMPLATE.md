# Implementation packet contract

One packet is one reviewable behavior at one committed dependency checkpoint.
Tasks are ordered; Depends lists accepted prerequisite IDs, not permission to
reach into their internals. No research, architecture selection or human interview
is an implementation task.

Required fields in `.ktask/tasks.md`:

| Field | Meaning |
|---|---|
| IF-nnn title | One observable implementation outcome |
| Milestone | Playable checkpoint M0–M10, not a player tier |
| Owner | Architecture module responsible for policy and state |
| Depends | Earlier accepted packets; none only for the first task |
| Spec | Existing authoritative files; Contract identifies the relevant entries |
| Blocks | Primary ownership of catalog blocks; each in-scope entry occurs once |
| Scope | Exact paths/globs; shared-file edits only for this feature's wiring/data |
| Contract | Commands, state transitions, inputs/outputs and refusal behavior |
| Red | Behavioral assertion that fails before implementation |
| Accept | Observable success and adjacent regression outcomes |
| Bounds | Hard work/state limits and overload behavior |
| Evidence | Required kinds: rules, game, visual, integration, soak |

`.ktask/content-plan.json` assigns each in-scope item family and armor family/rank
to one completing task; block ownership remains in Blocks. Requires names actual
input IDs or named services and must resolve to the task or an accepted dependency.
This file contains ownership and dependency edges, never recipe costs or prose.
Keep it synchronized when splitting/reordering tasks. The validator rejects
missing/duplicate obligations, unknown inputs and consumers preceding producers.
It checks declared edges, not inferred natural-language recipe meaning; readiness
must also inspect every recipe's real ingredients and installed services.
Each packet fingerprint includes its own content obligations and required inputs;
changing them invalidates that task's acceptance, not unrelated task receipts.

Scope profile `@module:name` expands the path convention once, in
`scripts/ktask_contracts.py`: that module's core, platform, client, tests,
namespaced data/assets and composition bridge. Shared translation/vanilla-tag
files permit only its own entries. Campaign scope excludes production sources.
Additional paths explicitly grant task-specific cross-module integration work.
Inspect exact expanded paths with
`python3 scripts/ktask_workflow.py scope IF-003`; process-control paths are always
forbidden to workers. Do not duplicate the expansion in every packet.

For each content packet, include its recipes/tags/translations, original readable
assets, guide/advancement entries and discovery tests for the content actually
introduced. Never register an empty placeholder to satisfy catalog coverage.
Extend the incremental coverage contributor created by IF-108 for every introduced
entry: actual registry representation, obtainable recipe, useful operation and
guide assertion. Catalog aliases and transaction-state rows map to their existing
representation; they must not become duplicate items. Pure future-rank rule tests
do not prove Survival obtainability; the producer task must add its real recipe
and exercise the assembled route when its materials become available.
Common requirements are inherited, not repeated as identical paragraphs per task:
versioned save/reload, break/replacement, ownership/concurrency, full outputs,
unloaded endpoints and invalid network input must preserve stated invariants.

Tests cover public behavior and one meaningful negative case, then adjacent
regressions. The authoritative gate is always `bash .ktask/verify.sh`; a task may
add checks, not replace it. Client-visible changes require inspected captures;
resource/process changes require conservation assertions; scalable work requires
shared-budget contention and measured performance.
Multi-behavior packets need independent success and refusal assertions for every
behavior. Split them when those outcomes can be delivered separately. A stronger
reviewer cannot compensate for an undefined implementation contract.

Coordinator owns packet readiness, numerical baselines and design corrections.
Worker implements the supplied contract; reviewer cannot waive missing behavior.
If a packet proves too large, coordinator splits it before another attempt, keeping
one primary owner per catalog entry and preserving accepted predecessor receipts.
