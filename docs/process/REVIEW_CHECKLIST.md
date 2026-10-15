# Review checklist

Defect classes reviewers have rejected before. Workers check the items that apply before handoff. Planners turn each applicable item into a `Tests` line. Add a new class at the milestone gate.

## World access
- Every neighbor or target read checks `isLoaded` first, including in vanilla superclasses: stair shapes, pane connections, `getStateForPlacement`.
- Validation that spans ticks re-checks what it observed earlier.
- Model shape, collision shape and targeting shape agree.

## Persistence
- Unknown, corrupt or oversized saved data keeps the original bytes and refuses edits. It is never replaced by empty state.
- Restored work matches a real recipe, its outputs and its containers.
- Tested through a real save and reload.

## Transactions
- Validate everything, then consume. A refusal leaves every input unchanged.
- Earned credit and outputs survive logout, full inventories and Creative mode.
- Rate limits cannot be bypassed by reopening a menu or using a second route such as buckets.
- An organ can never clog: every admitted input has an exit.

## Player interaction
- Empty main hand passes through when the offhand holds something usable, and never blocks normal building against the block.
- The server re-derives what the client claims: face, reach, selection, visibility.

## Visuals (tasks with captures)
- Every new block and item appears in a capture, blocks from front and back.
- Transparent textures use a cutout or translucent render type. An opaque black patch is the wrong layer.
- The geometry the catalog describes exists. A vanilla-textured cube is not art.
- Guide entries have unique coordinates and text that fits the page.

## Content completion
- Recipe, tag, translation, advancement or discovery step, and guide entry exist.
- Guide and JEI render recipes from the recipe data, never from copies.

## Ownership
- Reusable UI primitives live in `ui/`. Features hold no widget code.
- Every `Tests` and `Done` line of the task ran, and the notes name them.
