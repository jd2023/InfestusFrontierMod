# Production readiness gates — no autonomous implementation approved yet
HUMAN: Review VISION.md and the end-to-end docs/PROGRESSION_MAP.md.
Review its milestone scope and acceptance criteria. Numerical recipes are
initial playtest baselines. Publication requires the licensing
approval described in README.md. Draft concepts are not implementation approval.
Only a human may acknowledge this gate.

---

HUMAN: Approve the first implementation specification, required libraries and
acceptance tests for a playable end-to-end loop. Choose the ktask executor/model and a completion
adapter that independently runs .ktask/verify.sh; require branch and evidence verification.
Then add independently scoped implementation tasks using docs/TASK_TEMPLATE.md.
Do not launch workers merely because this skeleton exists.
