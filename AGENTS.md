# Repository instructions

Read `docs/DEVELOPER_GUIDE.md`, `docs/PERFORMANCE.md`, `VISION.md`, and the task's
owning specification before design or code changes. `VISION.md` is a draft:
explicit user commitments constrain work; proposals and open questions are not
authorization to invent gameplay. Record decisions in `docs/DECISIONS.md`.

Deep modularity is mandatory: one behavior owner, small interfaces, hidden
implementation, inward acyclic dependencies. Low-level infrastructure, including
UI primitives, belongs in tested internal modules, never scattered through features.

Performance is correctness. Discuss material costs before implementation and
enforce hard bounds, back-pressure and loaded-chunk-only behavior.

Start from a clean committed Git checkpoint. Scope edits narrowly; add meaningful
red-first tests, adjacent regressions and run `./.ktask/verify.sh`. Inspect the
complete diff, then commit one coherent scope. Never weaken checks or replace
visual goldens to obtain a pass. Do not run AI workers, acknowledge HUMAN gates,
publish, push, create a remote, or copy prototype gameplay/saves without approval.

The prototype in the sibling folder is reference evidence, not production code
or an authoritative specification. Never edit it as a side effect of work here.
