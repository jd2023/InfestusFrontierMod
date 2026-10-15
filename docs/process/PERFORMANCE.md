# Performance limits

Performance is correctness. A feature that can stall a server is a broken feature.

## Every system declares

Before implementation, the task states: the work unit, who shares the budget (dimension, team, network), the hard per-tick bound, which blocks it reads, maximum entities and geometry, persistent state size, packet rate, what happens on overload, and what happens on chunk unload and reload. Each of these has a test.

## Hard rules

- No unbounded loose items, XP orbs or projectiles. Insert into bounded storage, keep a capped buffer, then pause. A player harvesting by hand may get bounded stacks.
- No implicit chunk loading. No spread, route, helper or cross-dimension lookup creates a ticket. A paid loader (Sail Roost) has owner, quota, upkeep, expiry and restart tests, and counts the engine-loaded neighbors too.
- No recursive flood fill, whole-world scan, position-keyed unbounded cache, ever-growing queue, unlimited inventory, or attacker-sized NBT allocation.
- Budgets are shared through `TickQuota` or a sibling in `core`. Rate limiting one machine does not bound a world of machines.
- Idle systems sleep. Decorative blocks have no block entity. Refused work pauses and retries without losing items or multiplying pending work.
- Check `isLoaded` before any neighbor read. Vanilla neighbor updates, leaf decay, drops and third-party handlers can reach unloaded chunks.
- Sync changes only, as compact deltas. Particles are client-side, distance-limited, short-lived, and share one budget.
- The server owns all gameplay state. A client cannot mint resources, pick arbitrary targets, skip ownership checks or supply a mutation result.

## Fixed numbers from the design

| Rule | Limit |
|---|---|
| Sample collection | One death offers one sample to one destination. At most 64 offers per server tick. No kill log |
| Sample UI page | 16 species rows, 48 quality counts, 2 changed pages per second per viewer |
| Portable supply | 4 transactions per wearer per second, 64 per server tick in total |
| Death preservation | Looks at the 4 worn pieces once. One loaded-endpoint lookup per piece. No retry queue |
| Death Bond | At most one pending piece per equipment slot per player |
| Hunger damage | One cadence per wearer, not one per piece. Floor at one heart |

## Cases every feature must survive

Full outputs. Broken, split or merged structures. Cycles. Unloaded endpoints. Restart with resources in flight. Corrupt or old saves. Two players at once. Ownership change. Dead helper reservations. Dense mobs. Packet spam. Unsafe teleport target. Integer, coordinate and time limits. Shutdown during a transfer.
In every case: nothing is duplicated, nothing is lost, and the refusal is explained.

## Current state

The mod registers only its entry point. `core` provides `TickQuota`: constant space, constant time, one owner per instance. It is not yet a scheduler or a world-wide budget.
