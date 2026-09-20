# Implement the task

{{TASK}}

1. Inspect existing work and requirements. In build/evidence/<task-id>/notes.md,
   record the starting Git commit once; preserve that baseline across retries.
2. Write meaningful failing tests, implement, run focused and adjacent checks.
   Repair relevant code, tests, harnesses and configuration wherever needed.
   Scope paths are navigation hints, not permission boundaries.
3. Inspect the full diff: correctness, ownership, simplicity, isolation, test
   quality, performance and comments. Refactor and retest. Save normal logs,
   captures and measurements; summarize commands and results in the evidence note.
4. Commit the task's changes on the configured feature branch and push. Preserve
   unrelated user files. Use separate repair commits after a rejected attempt;
   do not reset the baseline or rewrite published history.
5. Write the native report at its supplied path, including the evidence-note path
   and commit. DONE means ready for ktask's independent tests and review.

ktask runs the full gate and a separate reviewer after handoff. Fix their concrete
findings in the same task; do not stop for file permissions or technical choices.
Do not fabricate results, suppress genuine failures, waive required gameplay,
change queue progress, launch another orchestrator or approve your own work.
Human input is only for new product decisions or external authority.

For HTTPS push authentication, use the existing login if needed:
`git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push origin HEAD:docs/progression-tree-armor-symbiosis`.
No global Git configuration changes, force pushes, main merges or releases.
