# Implementation readiness

Read repository instructions and the packet's owning specification sections.
Inspect the actual prerequisite implementations and their public interfaces.
You are the planning model, not the implementation worker. Do not edit files.

Return ready only when this task can be implemented at this checkpoint:
- One reviewable outcome; concrete inputs, outputs, transitions and error cases.
- Required producers/APIs exist, or this packet explicitly owns their first use.
- Numerical rules and recipe prerequisites agree with their authoritative tables.
- Every required edit/test/registration/gate hook fits the allowed scope.
- Required checks have runnable entry points, actual assertions and bounded fixtures.
- No unresolved architecture, crash-recovery protocol or product choice is assigned
  to the worker. Later-tier materials may be fixture inputs only when their item
  definition already exists; never invent placeholders to satisfy acceptance.

Return retry with an exact engineering/design correction if not ready. Return
external only for a genuine product decision or missing external authority.
Do not ask the user for numerical tuning or implementation design. Be concise.
