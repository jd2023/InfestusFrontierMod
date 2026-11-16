# Architecture

Technical decisions that every feature must conform to. [VISION.md](../VISION.md) says what the game is; this file says how the system is built.

Not here: the behavior, recipes and numbers of a single block or item. Code, data and tests own those. A feature that needs to break a rule below needs an owner decision first.

## Modules

- `core/` holds pure gameplay rules: accounting, plans, quotas, outcomes. `ui/` holds layout arithmetic. Both are plain Java with an empty classpath; the build fails if either gains a dependency.
- The mod itself is one package per feature under `org.jd.infestusfrontier`. The entry point only wires the feature modules together.
- A feature owns its registrations, behavior and saved state. Other features reach it only through its `api` package, a capability or an event. Block entities are package-private.
- Feature packages have no dependency cycles.
- `foundation` holds what every feature shares: persistence, refusal display, the organ menu and the screen kit.
- Client code lives only in screens, `foundation.screen`, `foundation.render`, `integration` and module client setup.
- `ArchitectureTest` checks these rules on the compiled mod. Do not weaken it to fit a change.
- Shared behavior has one owner. Reuse the existing helper; add an abstraction only when a second real consumer exists.

## Engine boundary

- Use vanilla and NeoForge behavior as it is: placement, breaking, support checks, loot, block updates, scheduled ticks, saving, capabilities, menus. If a design needs the engine patched or its rules suppressed, change the design.
- No Mixins, access transformers, coremods or reflection. No static mutable state.
- Prefer extending vanilla block classes and joining vanilla tags, so vanilla behavior applies without code of our own.
- Ordinary vanilla behavior that comes with a vanilla class, such as leaf decay, is allowed. It does not count as autonomous work by the colony.

## Server authority

- The server decides everything. A screen shows a snapshot and sends an intent; the server checks reach, inputs, target and current state again before acting.
- Screens use vanilla menus: slots, synced integers and menu button ids. The mod defines no custom packets. Add one only when the menu protocol cannot carry the state.
- A command either completes or returns a refusal that changed nothing. Refusals carry a stable key (`<owner>.<condition>`) with a translation, and the player sees the specific reason.
- Several players may view one organ. Each action is validated against the state at that moment.
- Fake players, hoppers and other automation never earn player credit.

## Chunks and time

- Never load a chunk to do work. Create no chunk tickets.
- Check that a position is loaded before every world read, including indirect ones: the other half of a two-block object, the block a survival check will read.
- Do not let a block update propagate into an unloaded chunk.
- Unloaded means paused. Work counts loaded server ticks only. There is no offline catch-up and no burst on load.
- An operation that needs a far endpoint requires it loaded. Otherwise it refuses or waits, keeps the resource where it is, and names the cause.
- A job stopped at an unloaded chunk waits, re-asks on a bounded interval and resumes after the chunk returns, including after a restart.
- Derive time from saved progress, never from wall-clock time.

## Server work

- Every search, queue, inventory, entity count, retry and saved record has a hard limit.
- Limits are shared, not per machine: a budget per dimension under a ceiling for the whole server. Adding machines, ports or dimensions never adds server work beyond that ceiling. `TickQuota` and `WindowQuota` in `core.foundation` are the admission primitives.
- Idle costs nothing. Mass-placed tissue has no block entity and no random tick; it uses scheduled ticks only while active and goes dormant by itself.
- Work is driven by a change, not by polling. Refused work retries when its inputs change, or after a bounded delay when the other side cannot notify. `RetryGate` is the primitive.
- Never scan the world or a radius for something to do. Work on explicit, finite, loaded targets.
- Long work is a job: admitted whole or refused whole against a cap, then advanced by a per-tick quota dealt fairly across jobs.
- Above a limit, refuse and say so. Never act on a truncated search as if it were complete. Never grow a queue to absorb overload.
- A data tag alone never authorizes arbitrarily large work; size limits still apply.
- A limit is a named constant with a test that fails when the limit is exceeded. Do not raise one silently.
- A limit that has not been load-tested is a starting value. Measure the mechanic under a dense, representative colony before claiming it scales.

## Resources

- Every resource has exactly one owner. Routes, indexes, caches and reservations hold references, never a second balance.
- Validate before consuming. A refusal consumes nothing.
- A batch reserves its inputs, its room for every product and by-product, and its payment before it starts. Its results are fixed at the start and saved, so a reload cannot reroll or repeat them.
- A full output or missing supply pauses the producer, names the cause and resumes when the condition clears. Nothing spills as loose items, and nothing is deleted because something is full.
- Every by-product has a finite buffer and a sink available at the same stage.
- Check that the destination accepts before extracting from the source.
- A store enforces its own reserves at the point every extraction path passes through: hand, hopper, port, vein and another mod's handler.
- Transfers conserve. Conversion or loss happens only under an explicit rule. No arrangement of machines forms a loop that creates resources.
- Amounts are integers with checked arithmetic. `Biomass` is the model; biomass is also an ordinary fluid for buckets and other mods' pipes.
- Item stacks are copied across value boundaries. Transfers preserve item identity and data components.

## Saved state

- State lives on the object that owns it: a block entity, an item's data components, or one bounded saved-data record per dimension.
- Saved state is bounded and fixed in size. Keep counters and current jobs, not logs or histories.
- Unreadable saved data is kept byte for byte, written back unchanged, and the owner refuses every operation. Never replace it with empty state or repair it silently. `Persistence` implements this.
- Worlds saved by an earlier version keep loading. Retired blocks and items stay registered; old record formats stay readable.
- Changing a recipe or data file never alters already-placed blocks. A recipe that disappears mid-batch does not lose the batch.
- Do not save what can be computed. Appearance derives from position and state.
- Persistence changes are proven by a real save and load in a GameTest, not only a codec round trip.

## Organs

- An organ stands on a loaded foundation block and works only while it has one.
- All organs share one interaction rule: a hand or ordinary item opens the screen, an applicable tool or ingredient performs its action, sneaking lets the player build against the organ.
- An organ broken by a player comes out whole, carrying its contents and its history. Grown tissue is different: it never moves, and breaking it does not return it.
- An organ's history is what it has done. It is assigned to one organ and never copied or merged.
- Evolution branches are permanent. No operation undoes one, switches one or reopens a rejected one. Settings, physical layout and substrate grafts are separate and may change.
- Capability comes from the object's real installed state, checked at use. The guide and advancements teach and record; they never gate operation.
- Advancements fire from server-confirmed events and grant no items, recipes or experience. Earned progress is never revoked.
- Automatic work is credited to the organ's steward, a single player. Credit owed to an offline player waits in a bounded store.

## Transport

- A vein network is passive connectivity, not moving cargo. Producers offer and consumers request through loaded endpoints; the transfer is direct between two stores. Veins hold no contents.
- Biomass, fluids and items are separate carriers that never mix. A crossing keeps its passages independent; only a junction joins.
- Adjacency is not a connection. Stores are reached through explicit ports with open faces.
- Known topology is saved separately from inventories. A known connection may be used across unloaded tissue; an unloaded inventory is never read.
- A network is limited by cells per connected component. An oversized network refuses; it never returns a partial result.
- One request is one admission from the shared budget, examines a capped number of endpoints and slots, and really moves what it reports before returning.
- Every rule of a port applies on every path through it. A closed port is closed to capabilities too.
- There is no global inventory. Stock is where it physically is.
- Vanilla hoppers, containers and redstone stay valid partners. Standard NeoForge item and fluid handlers are the boundary with other mods; respect their sides and rules.

## World changes

- The colony changes the world only as finite work the player authorized and supplied. Automatic growth is allowed inside those bounds. Nothing spreads without limit, and dormant tissue does not act on blocks placed later.
- Replacing a world block is not a harvest: no loose items, no experience. One original gives one replacement.
- Check a target when scanning and again immediately before replacing it. If it changed, leave it. Player edits are never overwritten.
- Replace a dependent block before its support, so vanilla's update finds a valid state.
- Unsupported content gets no guessed conversion and no guessed loot. Leave it and do not consume the player's input. Never convert a block that has an inventory or block entity.
- Drops come from loot tables in the real loot context, so tools and enchantments apply. One break rolls one table.
- The engine cannot tell a player's build from natural terrain. Eligibility must be an explicit block rule, usually a tag.
- Excavation removes real terrain and stops with a named reason at fluids, cavities, protected blocks, full outputs and unloaded chunks.

## Client

- Animation, particles and sound follow real state and need no server work or packets of their own. Turning visuals off changes no behavior.
- Put state the client needs for appearance into block state where possible.
- No block entity or ticking for appearance alone. No renderer reads the world beyond its own block.
- Collision and selection shapes match the visible model and are never animated.
- State is readable without relying on color alone.
- Screens are composed from the shared screen kit; a screen draws nothing itself and decides nothing.

## Data and compatibility

- Recipes are data. Organ batches, organ growth and digestion have their own recipe types; reuse vanilla recipe types where the behavior is vanilla's.
- Membership is a tag, never a hard-coded list. Other mods' content is supported through tags and explicit mappings.
- Keep the mod's items out of broad vanilla tags when that would let ordinary recipes consume them.
- Required mods are NeoForge, GeckoLib and Modonomicon. Anything else is optional and stays behind an adapter under `integration`; the game is complete without it.
- Do not change vanilla rules or other mods' rules. Vanilla items, recipes and automation keep working beside the colony.
- Add a library only with the feature that needs it.
- Test-mod classes never ship in the runtime JAR.

## Every feature survives

Nothing is duplicated, nothing is lost, and the player is told why work stopped, in each of these cases:

- full output, full waste buffer or missing supply;
- a broken, split, merged or looping structure or route;
- an unloaded source, destination or middle;
- save and restart in the middle of an operation;
- a block broken and replaced in the middle of an operation;
- corrupt, unknown or older saved data;
- two players acting at once, or a player disconnecting;
- a client claiming a result it did not earn;
- a data reload that changes a recipe in use;
- an exhausted work budget;
- an optional mod absent;
- integer, coordinate and time limits.

## Planned, not built

These are intended directions. Each needs a reviewed design under [design/](design/) before work starts.

- **Multiblock organs.** The original core keeps identity, history, branch and contents. Parts are real placed blocks that store only their role and their core. Only the core ticks. All ports share the core's reservations and work budget. A missing or unloaded part pauses the body; it is never dismantled automatically. Rebuilding never resets or duplicates the core.
- **Chunk loading.** The only acceptable form is a separate, explicit, paid, capped service the player builds. Travel, freight and routes never load chunks, and progression never requires it.
- **Freight between sites.** A reserved batch with one accounted owner that waits locally until both ends are loaded. This is a different model from the passive vein network. Each base runs on local production and does not need another base loaded.
- **Ownership.** The steward decides credit only. There is no access or team model yet; do not assume one.
