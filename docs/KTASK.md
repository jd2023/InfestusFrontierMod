# Task execution

Run `ktask resume` from this repository. ktask directly launches the worker and
owns the queue, reports, retries, timeouts, terminal and remote-branch check.

Workers implement, test, commit and push on the configured feature branch.
Necessary code, test, harness and configuration repairs stay in the same task.
Scope paths are suggestions, not file permissions. Preserve unrelated user work.

The native verification command, `bash .ktask/accept.sh`, runs the mod gate and a
fresh read-only smart-model review. Rejection returns to native repair; only ktask
marks tasks complete. Repair commits do not require rollback or new task setup.

Use ordinary logs, captures and a short `build/evidence/<task-id>/notes.md` with
the original baseline commit, commands and results. Keep that baseline across
retries. No receipts, evidence schema or project-side runtime state is required.
Native reports/logs live in .ktask/queue and .ktask/logs; reviewer findings are in
.ktask/logs/review.md. Old .ktask/session files are unused historical evidence.

Models and execution limits live in .ktask/config.toml; the review invocation is
in .ktask/accept.sh. The shared ktask installation is not modified by this project.
