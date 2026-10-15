# Growable junction and living reservoir prototype

## Try it

Run `./gradlew runReservoirLab` for the prepared tall reservoir and a short companion organ. This uses the **disposable Biology Lab save**, replaces its fixture area/inventory, and does not modify ordinary survival saves. The tall organ is at **100, 231, 0**, with the reservoir beginning at Y=233. The fixture is a visual study, not an automatically supplied production line.

In an ordinary world, craft the **Vascular Junction** with two bones, two slime balls and one Spore Culture. Picking up culture discovers its recipe. The Waking Genome has a Vascular Junction entry.

1. Place an amber Conductive Tissue cell in the ground, then the junction above it. Placement points that center cell upward so incoming routes terminate at the organ.
2. Lead an existing source pump's amber route toward the center cell. Multiple pumped branches can join here.
3. For an output, start a neighboring ground vein pointing **away** from the center. Continue its directed route; put the consumer above the final vein. Outputs need no additional pump. The first output step must be horizontal; later steps can use existing turns, vertical tissue and Vascular Stitches.
4. Optionally place reservoir storage immediately above the junction. Spare incoming biomass enters it; output draws from it when the junction buffer is empty.
5. For walk-under clearance, use **one bone meal before placing storage**. The organ becomes two blocks high, leaving two-block headroom away from the stalk. Height is capped at two; occupied space is refused without consuming bone meal. Existing reservoirs are never lifted or dismantled by growth.

```text
                    reservoir (optional)
                           ↕
source pump → veins → junction → veins → consumer
                           └──→ veins → another consumer
```

The junction serves one of four outward directions per accepted pulse, rotating even when a branch is blocked. This is round-robin opportunity, not a guarantee of equal long-term volume under unequal supply, endpoint capacity or server overload. Missing/full/invalid destinations retain biomass; the organ holds at most 250 units without storage. Incoming pumps retain their own bounded cargo when it fills. This experiment handles **biomass only**, not items or ordinary fluids, and does not replace the old Transport Organ.

Breaking the grown base removes the passive upper stalk and returns one data-bearing base item. Mounted storage remains where it was. Breaking only the upper stalk shortens the base without dropping another organ. The remaining buffer survives both shortening and save/load.

## Visual and architectural decisions

- No reservoir mouth or dedicated opening: the separate organ provides the connection.
- Rounded, connected outer shell with a muscular sole and thicker outer-edge flesh. Connected cells have no internal frames or membranes.
- Original 64×64 materials, with genuine-alpha purple vein membranes and green biomass. Baked vertex alpha is not a substitute for texture alpha in the native block renderer.
- Outer walls have matching inward-facing geometry: the rear membrane and inner flesh remain visible through the front. Opposite winding provides one visible face per viewing side, without a second offset/coplanar overlay.
- Opaque flesh/biomass write depth separately from translucent membrane. The biomass has a baked light floor (11/15); this is a cosmetic emissive appearance, **not a light source illuminating nearby blocks**, and needs no server ticker or lighting updates.

`junction` owns the buffer, routing cadence, growth, persistence and Minecraft interactions. Its only public facade is registration/item exposure; endpoints use the existing `logistics.BiomassPorts` contract. Dependencies point from junction to transport, logistics and simulation, never back into junction. Reservoir allocation remains owned by `biomass`. No capability callback traverses another endpoint, so linked organs cannot cause recursive network evaluation.

## Feedback loop

`./gradlew runReservoirShowcase` captures ten acknowledged poses: front, back, both remaining corners, overhead, underneath, night, empty, inside, and a short-organ close-up. Each server pose waits for the preceding rendered capture; slow rendering cannot skip a pose. The inside shot uses a verified spectator position so collision cannot push the camera outside. Images are written to `run/screenshots/reservoir-*.png`.

The first pass revealed an overly opaque membrane and weak glow. A new generated alpha material and a stronger, still bounded emissive floor corrected those. The inside-camera check caught and corrected a fixture error separately from the model. Final asset review reproduced overlapping exposed organ-crown faces; shortening the hidden stalk and dividing the crown into nonoverlapping pieces corrected them. Mesh tests cover every connection/fill state, seams, winding, inset separation and geometry ceilings. Asset tests check real alpha, crown overlap and bounded original organ models. GameTests cover split transfer/conservation, vanilla bone meal, standing clearance, occupancy refusal, drops, retained storage, blocked/cyclic routes, persistence and the shared transport quota.

The authoritative headless gate is `./gradlew prototypeCheck --console=plain`. Run graphical profiles separately. These checks are not a dense-base FPS/TPS benchmark, multiplayer soak, shader-pack compatibility certification, or proof that every viewing angle is artifact-free.

### Verification record — 2026-09-07

- Final normal gate: **83 unit tests and all 81 required GameTests passed**, including generated-resource/JAR verification.
- Final focused showcase: **all ten captures passed** after material, inside-camera and organ-crown refinements. Reviewed the nine reservoir views and short-organ close-up.
- Adjacent Biology showcase: **all ten captures passed on retry**, including empty/partial/full/expanded reservoirs. The first attempt failed its legacy fixed-time phase deadline before the overview rendered; that older timing fragility remains, and is not a model failure or a claimed fix in this change.
- Final `prototypeCheck -PwithoutCurios`: **83 unit tests and all 81 required GameTests passed**; core junction code does not depend on Curios.
