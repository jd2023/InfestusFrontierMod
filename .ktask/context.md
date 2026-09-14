# Implementation context

Production: InfestusFrontierModV3_dev. Sibling InfestusFrontierModV3 is read-only
prototype evidence. Never copy its code/assets/saves or edit it.

Read AGENTS.md, docs/DEVELOPER_GUIDE.md, docs/PERFORMANCE.md, VISION.md,
docs/ARCHITECTURE.md, then the task's named specification sections.
Rules have one owner; do not duplicate gameplay prose across documents.
This queue implements M0–M10, not Fold Gateway/T8/T9/Fold grafts, attack waves
or village transformation.

Coordinator owns design, task contracts and boundaries. Worker implements the
packet. Independent reviewer checks the whole candidate. Adapter runs acceptance,
commits and pushes to the authorized feature branch.
Workers never commit/push, edit queue markers, change gates or widen scope.
Routine engineering failures return to the stronger resolver, not the user.

Gate: bash .ktask/verify.sh.
Evidence: .ktask/session/evidence/<task-id>/.
Report: exact path in the orchestrator header, not a guessed queue path.
Status: python3 scripts/ktask_workflow.py status.
