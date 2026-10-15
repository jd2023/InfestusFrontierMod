# Logistics console prototype

## Try it

Run `./gradlew runLogisticsLab` for a playable replica of the reported layout in the disposable **Biology Lab** save. It places a 5×5 digestive patch, green ground-level veins, a two-block Vascular Junction, and an initially empty raised reservoir. A single extra-health husk actually takes digestive damage; harvested biomass is pulled into storage. No second pump and no prefilled reservoir are used. The fixture replaces only its designated area around (160, 231, 0), and resets that area each launch.

Run `./gradlew runLogisticsShowcase` for acknowledged world/UI captures under `run/screenshots/logistics-*.png`. This exits after completion. The fixture waits for saved entities before clearing old lab mobs and spawning its specimen; captures require exactly one specimen. Do not run two clients against the lab save simultaneously.

Craft a **Synaptic Probe** from **bone + slime ball + Spore Culture** (shapeless), or find it in the creative tab. Culture unlocks its recipe. Use the probe on a junction (either stalk part), vein, Transport Organ, or Vascular Stitch. Keep it held while using the console. Ordinary empty-hand interaction no longer changes logistics, and held blocks can be placed normally without sneaking.

## Junction

The screen has four explicit world-direction rows: North, East, South, West.

- **Auto:** a first vein pointing toward the center is an intake; pointing away is an output.
- **In / Out:** explicitly select a role. A mismatching first arrow refuses transfer; configuration does not silently rewrite the route.
- **Shut:** disables this arm. The green opening is replaced by a closed fleshy cap.
- **Trace:** inspects that arm without moving resources, showing route status, node count and endpoint coordinates.
- **Pause:** stops scheduled transfers, rejects external biomass deposits/withdrawals (including previously acquired capability handles), and closes all four visual apertures. Resume preserves the existing buffer.

First veins sit beside the center ground cell, **one level below the organ base**. Place the reservoir directly above the short organ, or grow the organ with bone meal **before** mounting storage to leave standing room underneath.

An intake follows the veins backward to their first cell. Its source must be either directly above that cell or directly behind its outlet. Both present, multiple incoming branches, broken channels, loops and unloaded boundaries refuse rather than guess. Place a junction to combine branches deliberately. Stitches retain their existing visible uphill/downhill endpoint rules.

One arm gets a turn every 20–24 ticks; each of four arms therefore gets an opportunity roughly every four seconds. The local buffer holds 250 biomass. Full storage can leave biomass safely buffered in the organ. Output routes need a compatible consumer above their final cell. Mounted storage absorbs spare input and supplies outputs.

The In/Out counters are **actual cumulative scheduled arm transfers**, not an instantaneous rate. They persist, saturate at the integer limit, and do not count direct capability deposits or internal mounted-storage exchanges. Decorative vein pulses indicate direction/channel; they are not proof of throughput. Current diagnostics are local, not a claim to inspect an entire base or every third-party capability.

Changing an arm's mode clears its previous trace; pausing/resuming clears all four. Trace again, or wait for that arm's scheduled turn, for a fresh result. Arm modes control scheduled horizontal routes; the legacy pump's central/bottom scalar connection is not a fifth configurable arm. Use **Pause** to isolate the entire organ, including that connection.

## Other controls

Veins provide direct channel and six-direction outlet buttons. Green is biomass/fluid, rose is items, cyan is courier trail. The biomass and ordinary-fluid transport paths remain separate; green tissue does not convert one into the other. Pumps offer above-pump or behind-first-vein intake. Stitches offer explicit uphill/downhill configuration. Veins have no storage or measured per-cell counters.

## Boundaries and next grades

This is the first usable probe, not a finished upgrade tree. Later grades could add route overlays, throughput histories, named endpoints, comparative bottleneck analysis, or remote inspection. Those need explicit range, retention, authorization and work budgets before implementation. No whole-network graph, remote editing, wireless logistics, new chunk loader or persistent topology cache is added here.

`logistics.ControlPort` is the inward shared query/intent contract. `junction` and `transport` own configuration and routing. `control` owns the held-tool interaction and native menu validation; `control.client` only composes immutable synchronized data. `ui.foundation` owns palette, drawing, progress/animation policy and short-word integer synchronization. The existing shared `client.capture` driver owns screenshot scheduling, now also usable by GUI fixtures. Feature modules never depend on their screens.

Native screens are sufficient for this fixed console; no new library is added. The LDLib2 revisit trigger in UI_ARCHITECTURE.md still applies before graph editors or substantially dynamic forms.
