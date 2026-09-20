# Repository instructions

Read `docs/DEVELOPER_GUIDE.md`, `docs/PERFORMANCE.md`, `VISION.md`, and the task's
owning specification before design or code changes. `VISION.md` is a draft:
explicit user commitments constrain work; proposals and open questions are not
authorization to change product scope. Propose and validate tuning values yourself;
reserve open questions for product choices, not numerical or technical assignments. Update current rules in their existing specification; do not maintain a history log.

Deep modularity is mandatory: one behavior owner, small interfaces, hidden
implementation, inward acyclic dependencies. Low-level infrastructure, including
UI primitives, belongs in tested internal modules, never scattered through features.

Performance is correctness. Discuss material costs before implementation and
enforce hard bounds, back-pressure and loaded-chunk-only behavior.

Start from a clean committed Git checkpoint. Scope edits narrowly; add meaningful
red-first tests and adjacent regressions. Acceptance runs `./.ktask/verify.sh`;
outside ktask, run it before delivery. Inspect the
complete diff, then commit one coherent scope. Never weaken checks or replace
visual goldens to obtain a pass. Use native `ktask run/resume/retry` for authorized
execution; workers may repair any relevant project code, tests or configuration,
including acceptance machinery, but may not waive requirements or self-approve.
Scope paths are guidance, not permissions. Workers do not commit/push. Accepted task
changes are committed and pushed by the adapter to the configured feature branch.
No public release, main merge, new remote or prototype copy is authorized.

The prototype in the sibling folder is reference evidence, not production code
or an authoritative specification. Never edit it as a side effect of work here.
