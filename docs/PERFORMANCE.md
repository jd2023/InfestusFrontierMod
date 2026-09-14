# Performance and server safety

Performance is correctness. Discuss scalable systems' costs and worst-case
failures before implementation.

Every system must declare its work unit, admission scope (dimension/team/network),
hard per-tick bound, loaded-block probes, maximum entities/geometry, persistent
state size, packet rate, overload behavior, unload/reload behavior and measurable
stress/soak acceptance criteria. Distinguish tested bounds from measured TPS/FPS.

## Mandatory constraints

- No unbounded loose item/XP/projectile output from assimilation, harvesting,
  defense or refused insertion. Insert into bounded storage, retain a capped
  buffer and pause. Explicit player harvesting may return bounded stacks.
- No implicit chunk loading for spread, routes, drones or dimension lookups.
  Explicit Sail Roost loading follows the Block Catalog's paid-loading contract;
  implement ownership, shared quotas, upkeep, expiry and restart/removal tests.
  Cross-dimensional lookups do not inherit permission to create tickets. Measure
  engine-loaded neighboring chunks as well as the requested ticking chunk; include
  their memory/work costs in loader admission and soak tests.
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

## Sample collection and equipment recovery

Sample collection is a death-event hook, not an ambient mob/item scan. Each death
can offer one sample to one selected destination. At most 64 collection attempts
per server tick; excess offers are discarded without extra drops. Containers use
the finite species/quality counters in Items; never store a persistent kill log.
Collector links are one explicit killer and one destination; revalidate loaded
endpoints without discovering neighbors or requesting tickets. Core normal loot
processing is unchanged. Deduplication is part of the one-death transaction, not
an ever-growing UUID cache.

Sample UI pages contain at most 16 species rows and 48 quality counts; send at
most two changed pages per second per open viewer. Server validates filters and
counts. At most four portable-supply transactions per wearer per second, subject
to a server-wide 64-transaction/tick budget; defer excess, preserve source contents.
Container schema/entry limits are validated before allocation, including malformed
saves and client requests.

Preservation examines at most four worn pieces on final death. Recall performs
one loaded-endpoint lookup per piece, never a retry queue or chunk ticket.
Death Bond retains at most one pending piece per equipment position per player.
No counterpart remains in drops, a grave, equipped armor or a recall berth.
Concurrent logout/respawn/server-stop paths must commit one authoritative outcome.

Native factory calculations are resource budgets only. Implement shared loaded-
cell/recipe admission and measure dense multi-dimension bases before claiming
their production rates are attainable at server limits.

## Required adversarial cases

Counter reduction, fusion and rescue must not duplicate points, items or charges
across interruption, swapping, concurrent users or reload. Sleep may recharge
rescue but must not erase learning; reducing counters must not undo symbiosis.

Spill budgets include neighbor updates, organ destruction and secondary fluid
release, not just the first fluid placement. Test multiple ruptures at admission
limits, full catchments, seal removal, chunk boundaries and reload. No recursive
failure cascade, item storm or offline damage catch-up.
Automatic feeding/refueling checks only configured slots on a bounded cadence.
Hunger damage needs one wearer cadence, not four independent damage timers.
Test the one-heart floor with four hungry pieces, fractional health and simultaneous
external damage; armor hunger cannot kill or heal, but external damage remains lethal.
XP deposits and ritual withdrawals share one reserved balance. Test concurrent use,
full stores, cancellation, player disconnect and cell removal without XP duplication.
Mnemonic Vessels process explicit transfers only, with no ambient orb scans or drops.
No item-wide decay ticker,
recursive portable inventory, or flight-induced chunk loading is authorized.

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
