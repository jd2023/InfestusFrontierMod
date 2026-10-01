# Review checklist

Defect classes that reviewers actually rejected in this project. Workers check
the applicable items before handoff; planners turn each into a `Tests` line when
writing a packet; a new recurring class is added here at the milestone gate.

## World access
- Every read of a neighbor or target position checks `isLoaded` first. This
  includes vanilla superclasses: stair shape, pane/bar connections and
  `getStateForPlacement` read neighbors and will load chunks at a chunk edge.
- Validation that spans ticks re-checks earlier observations before trusting them.
- Model shape, collision shape and targeting shape agree.

## Persistence
- Unsupported, corrupt or oversized saved data keeps the original bytes and
  refuses edits. It is never replaced by an empty state or silently skipped.
- Restored work must match a real recipe, its outputs and its containers.
- Tested through an actual save and reload, not only the codec.

## Transactions
- Validate everything, then consume. A refusal leaves every input unchanged.
- Earned credit and outputs survive logout, full inventories and Creative mode.
- Rate limits and shared budgets cannot be bypassed by reopening a menu or by a
  second route such as buckets.
- An organ can never be clogged: every admitted input has an exit.

## Player interaction
- Empty-main-hand use passes through when the offhand holds something usable, and
  never blocks normal building against the block.
- The server re-derives what the client claims: face, reach, selection, visibility.

## Visuals (packets with visual evidence)
- Every new block and item appears in a capture, blocks from front and back.
- Textures with transparency use a cutout or translucent render type; an opaque
  black patch means the wrong render layer.
- The catalog's described geometry exists. A vanilla-textured cube is not art.
- Guide entries have unique coordinates and text that fits its page.

## Content completion (Kind: complete)
- Recipe, tag, translation, advancement or discovery step, and guide entry exist.
- Guide and JEI render recipes from the authoritative recipe data, never copies.

## Ownership
- Reusable UI primitives live in `:ui`; features contain no widget code.
- Every `Tests` and `Done` line of the packet ran and is named in the notes.
