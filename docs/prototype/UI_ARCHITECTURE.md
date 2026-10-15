# Block UI architecture decision

Decision date: 2026-09-05

## Decision

Use Minecraft/NeoForge's native `AbstractContainerMenu` and `AbstractContainerScreen` for the prototype and for ordinary inventory-like production machines.

The Mutation Chamber is the trial implementation. Its inventory and processing remain server-authoritative, only slots and `ContainerData` cross the menu boundary, shift-click behavior is covered by GameTest, and the actual screen is captured by the visual showcase run.

This chooses a platform API, not permission to distribute low-level UI implementation among feature screens. Shared drawing, layout, theme, widget, synchronization, and capture behavior must be hidden behind the internal UI foundation described in [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md). The initial single-screen prototype debt was extracted for the Synaptic Console; new screens must reuse those tested primitives.

## Why

- NeoForge documents this pair as the standard backend and screen for inventory GUIs, including server/client constructors, `stillValid`, `quickMoveStack`, slot interaction, and synced menu data.
- It adds no dependency or additional version-compatibility surface.
- Headless GameTests can exercise the state-changing half of the UI. A deterministic client capture covers layout, labels, slots, items, and progress rendering.
- A small machine screen does not need a layout engine, RPC abstraction, stylesheet system, editor framework, or another persistence system.

## Library spike result

[LDLib2](https://github.com/Low-Drag-MC/LDLib2/tree/1.21) is the strongest library candidate found for NeoForge 1.21.1. It offers responsive layout, stylesheets, reusable elements, data binding, server synchronization, node graphs, and in-game editors. Its own integration instructions currently publish the recommended 1.21.1 artifact from a snapshots repository, and the library intentionally covers UI, rendering, synchronization, persistence, and editing as one broad framework.

That is valuable if Infestus Frontier later needs a genome graph editor, a visual mutation-tree designer, or other highly dynamic screens. It is unnecessary coupling for a two-slot chamber, so it was researched but not added to the runtime dependency set.

## Revisit trigger

### Synaptic console implementation (2026-09-07)

The prototype now has a shared `ui.foundation`: palette/panels, slot backgrounds, progress and synchronized animation policy, and an overflow-safe 32-bit-to-native-short menu adapter. Mutation Chamber reuses these primitives without changing its slot layout. Pure tests cover progress saturation/overflow, animation phase and negative/large data round trips. The shared acknowledged capture driver supports GUI readiness predicates as well as world poses.

The fixed logistics console uses native buttons and bounded `ControlPort` query/intent data; it adds no dependency. It does not yet need dynamic reflow, editable graphs or a content editor. See [LOGISTICS_CONSOLE.md](LOGISTICS_CONSOLE.md) for the module boundary and current scope. Library migration remains gated by the triggers below.

Run an isolated LDLib2 dependency and UI spike before building the first screen that requires at least two of these:

- dynamic/reflowing layout;
- editable node graphs;
- reusable form controls across many screens;
- complex client/server-bound interactive widgets;
- an in-game content editor.

The spike must compare startup compatibility, packaged size, maintenance/version risk, JEI interaction, keyboard/controller behavior, and the amount of automated coverage possible against the native implementation. Do not migrate existing screens until that gate passes.

## Sources

- [NeoForge 1.21.1 menus](https://docs.neoforged.net/docs/1.21.1/gui/menus/)
- [NeoForge 1.21.1 screens](https://docs.neoforged.net/docs/1.21.1/gui/screens/)
- [LDLib2 1.21 branch and integration instructions](https://github.com/Low-Drag-MC/LDLib2/tree/1.21)
- [LDLib2 server-synchronized screen and menu design](https://github.com/Low-Drag-MC/LowDragMC-Doc/blob/v2/docs/en/ldlib2/ui/preliminary/screen_and_menu.md)
