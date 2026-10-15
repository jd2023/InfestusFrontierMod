# Review

For the reviewer. You read; you never edit, commit, push or run models.

## Procedure

1. Read the story in its sprint file. Judge the story as written. Behavior listed under `Not` or owned by a later story is not a defect here.
2. Read the whole diff from the previous accepted commit to HEAD, the surrounding code and the tests.
3. Confirm the gate log `build/verification/full-gate.log` shows `BUILD SUCCESSFUL` and the GameTest line `All N required tests passed`.
4. For stories with captures, open the images yourself. Look at them.
5. From round two on, first check that every earlier finding is fixed and has a test or a named capture that fails without the fix. Then report new blocking defects only in code changed since the last round, unless the defect is severe (data loss, crash, unbounded work).

## Verdict format

First line: `APPROVED` or `REJECTED`. Then numbered findings. Each finding names the file and symbol, gives the evidence, and states the correction. Everything not blocking goes under a final `FOLLOW-UP:` heading.

Never approve because the build passed or the worker said it works.

## Blocking defects

- A `Do`, `Tests` or `Done` item is missing, untested or was not run.
- Data loss, item duplication, a crash, or a previously accepted behavior broke.
- Unbounded work, implicit chunk loading, or another limit in ENGINEERING.md "Performance limits" violated.
- An edit outside the story's `Files`, other than its own `Status` line. A raised limit, a weakened assertion or a replaced golden image without a regression test and an explained reason.
- A visual defect a player would notice, for stories with captures.
- Engine behavior patched, bypassed or re-implemented.

A concern is not a blocker because a bad outcome can be imagined. Show the reachable scenario.

## Checklist

Defect classes found in this project before. Check the ones that apply to the diff.

**World access**
- Every neighbor or target read checks `isLoaded` first, including reads inside vanilla superclasses: stair shapes, pane connections, `getStateForPlacement`.
- Validation that spans ticks re-checks what it observed earlier.
- Model shape, collision shape and targeting shape agree.

**Persistence**
- Unknown, corrupt or oversized saved data keeps the original bytes and refuses edits. It is never replaced by empty state.
- Restored work matches a real recipe, its outputs and its containers.
- Tested through a real save and reload, not only the codec.

**Transactions**
- Validate everything, then consume. A refusal leaves every input unchanged.
- Earned credit and outputs survive logout, full inventories and Creative mode.
- Rate limits cannot be bypassed by reopening a menu or using a second route such as buckets.
- An organ can never clog: every admitted input has an exit.

**Player interaction**
- Empty main hand passes through when the offhand holds something usable, and never blocks normal building against the block.
- The server re-derives what the client claims: face, reach, selection, visibility.

**Visuals** (stories with captures)
- Every new block and item appears in a capture, blocks from front and back.
- Transparent textures use a cutout or translucent render type. An opaque black patch is the wrong layer.
- The geometry the catalog describes exists. A vanilla-textured cube is not art.
- Guide entries have unique coordinates and text that fits the page.

**Content completion**
- Recipe, tag, translation, advancement or discovery step, and guide entry exist.
- Guide and JEI render recipes from the recipe data, never from copies.

**Ownership**
- Reusable UI primitives live in `ui/`. Features hold no widget code.
- Every `Tests` and `Done` line of the story ran, and the report names them.
- Comments state a contract. Narrating comments are defects.
