# Task execution

Run `ktask resume`. ktask launches one worker per packet, in strict order, and
owns the queue, reports, retries and timeouts. Configuration: `.ktask/config.toml`.

1. Worker (cheap model) implements the packet from `.ktask/prompt.md`.
2. `bash .ktask/accept.sh` runs the mod gate (`.ktask/verify.sh`, which also
   replays every module's capture scene) and asks a read-only strong-model
   reviewer using `.ktask/review.md`.
3. On rejection the repairer (strong model, `.ktask/autoresolve.md`) fixes each
   finding with a regression test. Up to six repair rounds follow.

The reviewer has memory: `.ktask/logs/review-state/<task-id>/` keeps the baseline
commit and every round's findings, and each new review receives them. Round two
onward verifies earlier findings first and may not reopen unchanged code except
for severe defects. Non-blocking remarks collect in `.ktask/logs/follow-ups.md`.

The baseline is the previously accepted HEAD, recorded by accept.sh. After any
commit made outside a packet run `bash .ktask/accept.sh --checkpoint` so it is
not attributed to the next packet.

`HUMAN:` entries in `.ktask/tasks.md` are milestone gates; see
`docs/TASK_TEMPLATE.md`. Nothing under `.ktask/` is ever committed by planners,
workers or repairers: the queue, its status markers, prompts, configuration and
logs are local working state. Never stash them either; a stash hides progress
from ktask. Old `.ktask/session` files are unused historical evidence.
