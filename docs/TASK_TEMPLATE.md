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
| Scope | Suggested starting paths/globs, not editing permissions |
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
files remain organized by behavior owner. Campaign and qualification tasks may
repair the production behavior or harness they exercise.
Inspect exact expanded paths with
`python3 scripts/ktask_workflow.py scope IF-003`. Necessary code, test, harness,
build, documentation and project-configuration repairs need no separate permission.
Do not duplicate the expansion in every packet.

For each content packet, include its recipes/tags/translations, original readable
assets, guide/advancement entries and discovery tests for the content actually
introduced. Never register an empty placeholder to satisfy catalog coverage.
Advance the shared tracked content-checkpoint.json to this packet's ID whenever
it introduces content; never lower it or remove existing obligations.
Extend the incremental coverage contributor created by IF-108 for every introduced
entry: actual registry representation, obtainable recipe, useful operation and
guide assertion. Before IF-004, owner guide data and assertions are staged and
schema-checked; IF-004 activates actual client checks for all prior content.
Obtain/use assertions run immediately. Catalog aliases and transaction-state rows map to their existing
representation; they must not become duplicate items. Pure future-rank rule tests
do not prove Survival obtainability; the producer task must add its real recipe
and exercise the assembled route when its materials become available.
Common requirements are inherited, not repeated as identical paragraphs per task:
versioned save/reload, break/replacement, ownership/concurrency, full outputs,
unloaded endpoints and invalid network input must preserve stated invariants.

Tests cover public behavior and one meaningful negative case, then adjacent
regressions. The authoritative gate is always `bash .ktask/verify.sh`; a task may
repair faulty checks with regression evidence while preserving their intended
acceptance outcomes. Client-visible changes require inspected captures;
resource/process changes require conservation assertions; scalable work requires
shared-budget contention and measured performance.
Multi-behavior packets need independent success and refusal assertions for every
behavior. Split them when those outcomes can be delivered separately. A stronger
reviewer cannot compensate for an undefined implementation contract.

Planning owns packet readiness, numerical baselines and initial design. Workers
resolve engineering gaps and repair prerequisites within the current task.
Reviewers require concrete corrections, not a separate permission round trip.
Neither role may waive missing behavior or rewrite live queue progress.
