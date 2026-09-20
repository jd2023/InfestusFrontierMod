# Implementation context

Production: InfestusFrontierModV3_dev. Sibling InfestusFrontierModV3 is read-only
prototype evidence. Never copy its code/assets/saves or edit it.

Read AGENTS.md, docs/DEVELOPER_GUIDE.md, docs/PERFORMANCE.md, VISION.md,
docs/ARCHITECTURE.md, then the task's named specification sections.
Rules have one owner; do not duplicate gameplay prose across documents.
This queue implements M0–M10, not Fold Gateway/T8/T9/Fold grafts, attack waves
or village transformation.
I038's proposed weapon bodies and I087/I088/I089 Curios candidates are not approved
content; optional guide leaves remain hidden until a supported implementation exists.
Curios presence/absence and the independent sample-pouch slot remain required.

Planning defines the intended product. Workers implement and autonomously repair
necessary code, tests, harnesses, documentation and project configuration across
modules. Scope paths are starting points, never permission barriers. Technical
gaps are engineering work. Independent review checks every repair for relevance,
correctness and preserved requirements. Workers commit and push; ktask verifies
the remote, runs the mod tests and obtains independent review before advancing.
Workers never self-approve, edit the live queue/progress or fabricate evidence.

Gate: bash .ktask/verify.sh.
Evidence: build/evidence/<task-id>/notes.md and ordinary test artifacts.
Report: exact path in the orchestrator header, not a guessed queue path.
Status: ktask status.
