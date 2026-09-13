# Production readiness gates — no autonomous implementation approved yet
HUMAN: Review VISION.md and the end-to-end docs/PROGRESSION_MAP.md.
Resolve first-slice questions through docs/OPEN_QUESTIONS.md, including Q-024,
Q-025, Q-032 and Q-034 and the selected feature's own blockers. Q-037 blocks
release. Do not treat prototype tuning or draft concepts as approval. Only a human may acknowledge this gate.

---

HUMAN: Approve the first implementation specification, required libraries and
acceptance tests through Q-035/Q-036. Choose the ktask executor/model and a completion
adapter that independently runs .ktask/verify.sh; require branch and evidence verification.
Then add independently scoped implementation tasks using docs/TASK_TEMPLATE.md.
Do not launch workers merely because this skeleton exists.
