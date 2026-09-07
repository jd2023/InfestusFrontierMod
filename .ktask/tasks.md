# Production readiness gates — no autonomous implementation approved yet
HUMAN: Review VISION.md and docs/DECISIONS.md with the owner. Record decisions
about colony control, research ownership, the first 30 minutes and the first
playable slice. Resolve the license mismatch before any release. Do not treat
prototype tuning or draft concepts as approval. Only the owner may acknowledge.

---

HUMAN: Approve the first implementation specification, required libraries and
acceptance tests. Choose the ktask executor/model and a completion adapter that
independently runs .ktask/verify.sh; require branch and evidence verification.
Then add independently scoped implementation tasks using docs/TASK_TEMPLATE.md.
Do not launch workers merely because this skeleton exists.
