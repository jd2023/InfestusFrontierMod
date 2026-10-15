# Burrowing — dangerous temporary passage prototype

## Design and opinion

The owner's clarified idea is **non-excavating traversal**: hard terrain in front
of the wearer temporarily becomes shadowed/passable, then returns. Stopping for
seconds can trap and suffocate the wearer. Lighting, preparation and particular
mutations should matter. Vertical travel is possible **inside ground**, not as an
open-air flight ability. This replaces the earlier ambiguity about what burrowing
means; it does not replace Manual Leaching, which still prepares rock for tools.

This is a promising expedition specialty: plan an exit, carry fuel and a recovery
tool, choose a light mutation, and decide when to turn back. The prototype makes
the hazard visible and reproducible. After playtesting, the owner rejected the
permanent disable and the three-second window. The prototype now uses a **ten-second
grace period** and a **two-second cooldown after releasing use**, with no permanent
lock. Biomass and cooldown limit reactivation, even when embedded in solid rock.
Longer safe stopping windows should become progression rewards through upgrades
and mutations; those tiers are not implemented yet. These timings, speed, graft
ingredients and cost multiplier are experimental—not production balance.

## Try the prepared comparison

```bash
./gradlew runBurrowingLab
```

This leaves Minecraft open in the disposable **Biology Lab** save. It resets a
separate volume at **X700–730, Y220–238, Z−14–10** and the lab player's equipment
and inventory. It does not reset the excavation/reservoir fixtures. Unexpected
blocks, inventories or non-player entities refuse reset; do not build or store
valuables in this new fixture. Like the other lab profiles, first-time setup uses
the existing lab-world preparation task. Use `runClient` for persistent gameplay.

You arrive at **X706.5 Y226 Z−10.5**, facing the rock mass, wearing a full suit
with the burrowing graft and 1,000 biomass. Walk toward the wall before activating.

1. **Hold right-click with the Burrowing Ganglion** (slot 1). Look where you want
   to go. The organ moves you through the temporary passage at about 1.8 blocks/sec.
2. Try turning, looking down and looking up **while underground**. Aim into open
   sky outside the rock: it cannot lift you or sustain flight.
   Plan stable footing at the exit: a temporary vertical passage is still open
   below you until it hardens, so emerging into air does not provide a platform.
3. Compare the plain crown you wear with the **light-grafted crown in slot 2**.
   Swap them before entering, not while the passage is hardening. The light graft
   illuminates newly phased cells using actual block light (level 12); no night
   vision or external dynamic-light library is required. It is not a general torch
   outside the passage. Revisited unlit cells stay unlit until restored/rephased.
4. **Stop/release use underground.** The countdown expires after roughly ten
   seconds; the rock returns and vanilla suffocation starts. Restoration is budgeted,
   so a busy batch may take a few additional ticks. Releasing the button starts a
   two-second item cooldown (visible on its hotbar icon). When that finishes,
   **hold use again to reopen the rock**, provided the suit has biomass. You do
   not have to escape first. Switching through Spectator does not disable later
   burrowing, and old saved lock flags are automatically removed. If fuel runs
   out, the supplied diamond pickaxe can help you dig an emergency exit.
5. Fuel expenditure is real: **one biomass per successful step, two with the
   light graft**. Blocked or unfunded steps do not pay. These are traversal costs
   only; general combat, ordinary activities and self-mending remain future work.

The normal lab is dangerous Survival play. Do not test the suffocation consequence
with valuables. `runBurrowingShowcase` instead performs the automated eight-phase
comparison, including a deliberate suffocation hit and a restart inside hardened
rock, restores the test player's health/position outside, captures results, then exits.

## Assemble it outside the lab

- Craft a ganglion from a **Leaching Nodule + amethyst shard + bone**. Acquiring a
  nodule unlocks the recipe. The Waking Genome has a **Burrowing** entry.
- Wear all four Symbiotic armor pieces. With the ganglion main-hand and amethyst
  offhand, **sneak-use** to graft burrowing into the worn thorax. One shard is consumed.
- Repeat with **glow ink offhand** to graft illumination into the worn crown.
  One ink sac is consumed; already-installed grafts do not consume another ingredient.
- Feed the full suit by sneak-using main-hand rotten flesh in the air, as before.
- Hold use with the ganglion to travel; release it to stop. Grafts are saved on
  their equipment, not in a global player unlock, and coexist with existing branches.
  These simple catalysts stand in for future mutation/research progression.

## Supported terrain and limitations

Stone, deepslate, granite, diorite, andesite and tuff are eligible. Deepslate's
original axis is preserved. This is a block-type allowlist, **not natural-generation
provenance**: player-placed instances of these rocks are also eligible. Ores,
bedrock, containers, dirt/grass, unknown blocks and other unsupported materials stop
the prototype. Adjacent fluid/falling-block hazards and unloaded boundaries refuse
entry. Soil at the surface may therefore require a prepared entry/exit.

Terrain is temporarily replaced with a registered shadow state, then restored;
it is not harvested, moved upward, converted into loot or permanently hollowed.
The proxy cannot be mined in Survival, moved by pistons or displaced by fluids.
Native world permission and cancellable break-event checks run before phasing.
This is not certification of every claim mod. Other players/entities can physically
enter the same temporary volume: this is **shared terrain**, not private per-player
noclip. Cooperative passage and conflicting lease renewals need more multiplayer
design/testing before production. Ordinary Creative/admin edits are not undone.

No flight flag or global noclip mode is granted. Server-validated, short movement
steps use native position synchronization. Gravity is suspended only during a
live underground passage and restored on exit/expiry/equipment loss. There is no
automatic emergency teleport in gameplay; that would remove the requested hazard.
The first prototype's position corrections are intentionally simple; multiplayer
latency, client movement feel and the hold-to-travel control still need review.

## Ownership, bounds and evidence

`burrowing` owns graft admission, native held-use input, eligibility, movement and
restoration. `PassageLease` is pure bounded deadline/accounting policy;
`PassageLedger` adapts it to per-dimension saved data; `ShadowRock` owns the reversible
host state and fluid/piston boundary. Fuel remains in `bioarmor`, accessed only
through its server-only whole-cost debit. Existing `WorkBudget`, native item/block
sync and shared `PhaseCapture` are reused; no custom packet family, mob, chunk
loader or second suit-fuel store is added.

The ledger retains at most **256 positions per dimension**, including expired
unloaded positions. Four admitted wearers share at most four movement operations
per tick; each checks at most 36 body/swept cells and their six neighbors. Restoration
is limited to 16 writes per dimension tick, with at most 16 ledger entries reviewed
per tick. Native scheduled block callbacks provide an orphan-restoration fallback;
they share that write quota. Unloaded leases consume capacity until their chunk
loads naturally. No scan or ticket loads it for restoration. See [PERFORMANCE.md](PERFORMANCE.md).

Pure tests cover cap/renewal, bounded fair review, deadline serialization, malformed
data, atomic fuel expenditure and the six-quad translucent model. Native tests cover
six directions, no flight, both grafts, fuel/no-fuel behavior, exact restoration,
native suffocation and successful restart, finite release cooldown, recovery after
Spectator/Creative changes and legacy lock cleanup, denied protection, nearby water,
ore/missing-suit refusal, fluid/piston immunity, proxy serialization/orphan recovery,
later world edits and bounded wearer admission/release. The client capture driver
checks dark versus light-12 passage, upward movement, countdown, damage, restart
inside hardened rock and open-air refusal. Its revised eight-phase run is pending:
the owner's open lab save prevented this turn's capture. See [TESTING.md](TESTING.md).

This is local functional/visual evidence, **not** a dense-server TPS/latency soak,
complete multi-player collision test or arbitrary crash-point durability guarantee.
Ledger and chunk saves are native but not one atomic disk transaction. State/codecs
and orphan fallback are tested; a complete process-kill/rejoin matrix is still open.

Screenshots: `run/screenshots/burrowing-{entrance,dark-passage,lumen-passage,ascending,warning,hardened,restarted,no-flight}.png`.
