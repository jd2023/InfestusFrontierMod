# Performance and server safety

Performance is correctness. Before scalable work is implemented, discuss its
cost and worst-case failure with the owner. No production budget is inherited
automatically from a successful small prototype.

Every system must declare its work unit, admission scope (dimension/team/network),
hard per-tick bound, loaded-block probes, maximum entities/geometry, persistent
state size, packet rate, overload behavior, unload/reload behavior and measurable
stress/soak acceptance criteria. Distinguish tested bounds from measured TPS/FPS.

## Mandatory constraints

- No unbounded loose item/XP/projectile output from assimilation, harvesting,
  defense or refused insertion. Insert into bounded storage, retain a capped
  buffer and pause. Explicit player harvesting may return bounded stacks.
- No implicit chunk loading for spread, routes, drones or dimension lookups.
  Cross-dimensional endpoints and any explicit chunk loader need separate approval,
  ownership, hard limits, upkeep, expiry and restart/removal tests.
- No recursive flood fills, whole-world scans, position-keyed unbounded caches,
  ever-growing queues, unlimited inventories or attacker-sized NBT allocations.
- Use shared admission budgets with bounded per-operation work. Rate limiting
  each machine alone does not bound a world full of machines.
- Idle systems should sleep; decorative blocks have no block entities. Rejected
  work pauses/retries without losing resources or multiplying pending work.
- Audit indirect work: vanilla neighbor updates can read unloaded chunks; leaf
  decay, drops, block cascades and third-party handlers can invalidate local bounds.
- Sync transitions/compact deltas, not unchanged state. Pure visual particles are
  client-local, distance-limited, short-lived and governed by a shared budget.
- Keep authoritative gameplay on the server. Clients cannot mint resources,
  select arbitrary targets, bypass ownership or supply trusted mutation results.

## Required adversarial cases

Full outputs; broken/split/merged structures; cycles; unloaded endpoints; restart
with in-flight resources; corrupted/versioned saves; simultaneous players;
ownership changes; stale/dead drone reservations; dense mobs; packet spam; item
storms under chunk loaders; unsafe teleport destinations; coordinate/time/integer
limits; shutdown during mutation/transfer. Conservation and safe refusal are tested.

Geometry/texture/animation limits and dense-scene frame times belong in visual
reviews. Large biological canopies and transparent multiblocks need specific
profiling, even when they add no server ticker.

## Bootstrap budget

The production mod currently registers only its entry point: no simulation loops,
content registries, world mutation, entities, network packets or custom rendering.
`:core` provides a constant-space, constant-time tick quota for future bounded
simulation. It is not yet a scheduler, fairness guarantee or world-wide budget.
The isolated test mod exercises this contract in Minecraft; it is not shipped.

Future tasks must attach profiler evidence where relevant. A few successful
GameTests are not a multiplayer, chunk-loader soak or server latency certification.
