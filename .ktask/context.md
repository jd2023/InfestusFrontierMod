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

Coordinator owns design, task contracts and boundaries. Worker implements the
packet. Independent reviewer checks the whole candidate. Adapter runs acceptance,
commits and pushes to the authorized feature branch.
Workers never commit/push, edit queue markers, change gates or widen scope.
Routine engineering failures return to the stronger resolver, not the user.

Gate: bash .ktask/verify.sh.
Evidence: .ktask/session/evidence/<task-id>/.
Report: exact path in the orchestrator header, not a guessed queue path.
Status: ktask status.
