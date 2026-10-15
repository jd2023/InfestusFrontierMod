# Colony experiments

Target remains Minecraft **1.21.1 / NeoForge**. The user's mention of 1.20.1 does not initiate a downgrade. Do not use 1.21.2+ equipment JSON APIs here.

Run `./gradlew runColonyLab` for manual testing; `./gradlew runColonyShowcase` captures 24 phases (including seven real-player armor portraits) and exits. These profiles use the disposable **Biology Lab** save, rebuild a platform near X=100/Y=230, and replace the lab player's inventory. Close other clients using that save first. Ordinary `runClient` never installs these fixtures. Spore Culture unlocks the prototype equipment/organ recipes, including through direct Creative insertion; The Waking Genome explains these experiments. See [living-colony feedback and next decisions](LIVING_COLONY_DIRECTION.md) for the current visual studies and deferred mechanics.

## Symbiotic armor

All four variants now share an **open carapace** anatomy with a 512×256 original material atlas. Split crowns leave the face visible; layered, tapered pectorals and thighs, ribs, shoulder lamellae, bracers and dorsal vertebrae leave deliberate openings for the player's skin/clothes. Strider adds sensory fins, Gill adds lateral membranes, and Bastion adds raised horns. The complete dark under-suit and old full-box branch shells are removed.

The four pieces are Crown, Thorax, Legguards and Treads. Wear them normally; right-click without sneaking still equips them. Vanilla durability, armor attributes, repair with rotten flesh, enchantability and smithing trims remain native. Inventory previews have colored bands for ten vanilla trim materials; worn armor retains the native trim/glint passes. Custom geometry reuses native trim UVs: tailored placement for every pattern, every player skin, slim arms and all swimming/riding poses is not certified.

**Dye it like leather armor:** craft a piece with one or more dyes in a crafting grid. Dye again to mix with the existing color; use the piece on a water cauldron to remove dye (one water level). Only the shell is tinted; horn, tendons, organs and smithing trims retain their own colors. Undyed shells use the branch's default palette. Damage, names, mutations, trims and enchantments survive crafting, washing and saving. The in-game guide and item tooltip explain dyeing. [Original art, exact generation prompt and exporter](art/colony/OPEN_CARAPACE.md).

The showcase adds actual-player front/back, red Strider, blue Gill, crouching Bastion, Gill-back and green Bastion captures at `run/screenshots/colony-player-*.png`. It asserts loaded worn textures, open base geometry, trim preview selection and synchronized inventory/worn dye color. Screenshot inspection is still required for clipping; passing resource checks alone is not visual approval.

Hold an armor piece in the main hand, a catalyst in the offhand, and **sneak-right-click in the air**:

- Redstone: **Strider** (fins; fast tissue access).
- Prismarine shard: **Gill** (lateral gills; caustic lift access).
- Bone: **Bastion** (shoulder spines; visual branch proof, combat specialization is not implemented).

Repeating the same branch advances its level to 3. Changing branches resets the level to 1. This cheap reversible experiment is not the final progression design. Mutations modify only the module-owned component, retaining enchantments, trims, damage and names. Level 2 on a piece satisfies either experimental access gate; otherwise that piece must have the appropriate branch. **All four correct bio armor pieces must qualify individually.** Old carapace-mutated vanilla armor no longer activates fast tissue.

The Resonance Organ goes into a Curios **symbiont** slot and raises fast tissue from Speed I to Speed II, only with an already qualifying full suit. Carrying it in the normal inventory does nothing. Core gameplay remains usable without Curios; use `./gradlew prototypeCheck -PwithoutCurios` for the absent-mod profile. The adapter uses only the public API, following [Curios development setup](https://docs.illusivesoulworks.com/curios/getting-started), pinned to 9.5.1+1.21.1.

## Vascular lift

**Parked after player feedback that it did not work reliably in their session.** The controlled fixture is not evidence that the user's case is fixed. Existing blocks and saves are retained; no lift redesign is included in this visual pass.

Build a 3×3 outer shaft: a one-block-wide interior, 4–24 blocks high, surrounded by Vascular Shell on all eight neighboring positions at every height. Place Vascular Lift Organs immediately below and above the interior and seal the surrounding eight floor/roof positions with Shell. Replace two cardinal wall blocks vertically with Exit Membranes for each doorway; at least two doorways are required. Keep the two blocks directly outside every doorway empty. Corners must remain shell. Intermediate-floor exits are allowed.

Empty-hand right-click the bottom organ to fill it; sneak-right-click that organ to drain it. The lab supplies an eight-block-high example, with an upper landing. Wear a full Gill-mutated (or individually level 2+) suit. **Look up/down to rise/descend; look horizontally to hover; sneak to descend.** Ordinary horizontal movement passes through the exit membrane. Without qualifying armor the suspension deals 8 magic damage per accepted vanilla damage interval; Creative remains invulnerable.

This is a contained, non-flowing biological medium, not a registered water-like fluid. It never floods the base, does not drown a qualified wearer, and requires a sealed structure and armor access. It is intentionally different from water + swimming speed: controlled vertical motion and designated exits, rather than ordinary swimming. Breaking the enclosure stops further propulsion; remaining suspension is still hazardous and can be drained or broken. Fluid tags, buckets, Depth Strider, swimming pose and fluid-mod interactions are not claimed by this prototype.

Each lift structure validation probes at most 26 interior/cap cells, 16 floor/roof ring cells, 192 shell cells and 96 exterior doorway cells. Movement additionally searches down at most 24 cells. Up to 16 validations per dimension per tick share a hard quota; excess attempts skip propulsion. No block entity, fluid tick, forced chunk load or traversal cache exists. Each occupant is handled once per game tick, even when overlapping two cells. Movement uses vanilla velocity synchronization, capped by the same work quota; multiplayer latency/anti-flight behavior still needs a dedicated soak.

`bioarmor` owns equipment identity, the bounded evolution component and full-set gate. `integration.curios` owns optional library access. `substrate` consumes those small queries; it does not inspect armor NBT or Curios slots itself. Native client armor rendering owns geometry and changes no server state. Every access check examines four armor slots; Curios lookup is only needed after that gate succeeds. No armor ticker, persistent collection or custom packet is added.

The lab is a diagnostic fixture, not an example of gameplay world scanning. All platform edits are opt-in and bounded.

On reopening the lab, entity fixtures are rebuilt after 60 server ticks so saved entity chunks can finish loading before their old population reservations are released. The graphical capture checks native armor trim-model selection and actual lift ascent, and fails on disconnect or timeout instead of waiting indefinitely. Repeat the showcase on the same save to exercise this reload path.

## Tissue transport, not pipes

Mature Living Substrate can be specialized by right-clicking with amethyst shard (**item channel**, rose), slime ball (**fluid channel**, green), or wheat seeds (**courier trail**, mint/cyan). The macro-pattern stays aligned with adjacent substrate. Membranes are recessed inside the tissue, with explicit bounded UVs. The cosmetic pulse travels toward the configured outlet (not a measurement of actual throughput). Use a **Synaptic Probe** to select the channel and one of six outlet directions in the console; empty hands no longer configure logistics. To make a turn, lay an L and turn the corner's outlet toward the next cell. Its inlet is inferred from a same-channel neighbor pointing into it. Native models now draw the elbow, vertical wall traces and exposed end caps; faces between tissue cells are hidden. The model projects the same two-port vessel onto its exposed faces, not six independent outputs. The console confirms the selected outlet. Multiple inputs select one deterministic visual inlet. Biomass splitting/joining now uses Vascular Junctions; crossing isolation remains future work. Existing saved cells update their appearance on a neighboring block change or outlet click.

Place a **Transport Organ on the first tissue cell**, a source chest/fluid container **above the organ**, and a destination **above the last tissue cell**. Point every tissue outlet to the next cell. The final cell points to a non-tissue block/air beyond the route. Paths may turn or change height, but a route must keep one channel. Each organ moves at most one item or 250 mB roughly once per second. Routes are limited to 64 cells; loops, wrong channels and unloaded chunks pause work. Use the Synaptic Probe to configure the organ's intake.

### Visible hill routes

On each one-block ledge, first mutate the exposed ground below and above to the same item/fluid channel. Place a **Vascular Stitch against the visible vertical riser**. The graft grows a curved muscle strip from the lower surface to the upper edge; placement configures both endpoint outlets uphill. Use the Synaptic Probe on the graft and choose Downhill to reverse both outlets. Repeat over a hill. Flat parts and elbows use ordinary tissue. No buried block selection, automatic excavation or invisible resource jump is involved. Stitches are non-colliding so the strip does not obstruct walking. Place the source organ on a flat cell before the first stitch, not in the stitch's space. Toggling a stitch deliberately reorients its two endpoints; turn a terrace corner afterward if needed.

Stitches count toward the existing 64-node limit. They reject missing, mismatched or unloaded endpoints and do not support cyan drone trails. A tall cliff needs exposed intermediate landings for now; suspended strands, organic bridges and 3D drone movement are separate experiments. The grassy fixture runs over the hill near Z=-12. The showcase includes a hill overview and ledge close-up.

The supplied **Fluid Bladder** is a real NeoForge fluid-capability endpoint, holds 4000 mB, accepts buckets, and displays its approximate fill in four steps. Its generic membrane visual is not the contained fluid's actual texture. The older Biomass Reservoir is a biological resource store, not a fluid tank, and is intentionally not silently converted into one. Standard chest and compatible capability storage work through the same bounded adapter. Only the first 54 inventory slots and the endpoint's bottom face are addressed in this experiment.

If output is full, an organ retains at most one item or 250 mB in persisted transit storage and stops taking new input. Breaking it in Survival produces one block item carrying that buffer, not loose cargo. Changing the channel while cargo is buffered pauses until the original channel is restored. There is no in-world item spray, fluid spill, chunk loading, branching/filtering UI, crossing channel or remote lookup.

`routing` owns the pure, bounded directed walk. `logistics` owns item/fluid capability access. `transport` owns route state, conversion interaction, scheduled organs, buffers and diagnostics. Courier helpers can consume its public trail query and storage adapter without accessing transport block entities. Decorative tissue has no block entity or ticker.

## Cystlings, Gatherlings and Nutrient Sails

Build a **3×3 Living Substrate / tissue floor**, with a **Brood Cradle above its center**. The center is the first cell of a flat cyan trail, 4–64 cells long; place a destination chest above the last cell. The second trail cell is the pickup spot: drop items there. Leave two air blocks above the eight surrounding nursery positions and all walking cells. Routes can turn horizontally, but not climb, cross gaps/dimensions, or choose branches. This is a footprint requirement, not a new multiblock controller.

Each helper reserves its own nursery position. Hatching checks block and living-entity clearance and refuses without consuming flesh or population capacity when no clear berth exists. Idle workers stay at their berth; an available pickup job sends them along a bounded nursery-perimeter approach, then the cyan trail, and they return to their berth after delivery. They still may pass through one another in transit: traffic reservations and collision-free multi-courier scheduling are not implemented. Do not route the main trail back through the nursery. Old cradles need the new floor before further hatching; existing larvae are not automatically repositioned. Workers rebuild their approach when active. The disposable lab rebuilds a complete nursery on launch.

- Feed one rotten flesh per hatch. Maximum **three Cystlings** at once; no automatic offspring.
- Redstone on your Cystling makes a **Gatherling**. It carries up to eight items, physically follows the trail, inserts into storage, and returns. Full output prevents pickup; output filling mid-trip leaves cargo in the helper.
- A ghast tear makes a **Nutrient Sail**, slowly circling above the cradle with animated fins/tendrils. It does no logistics. One alive, loaded Sail within 12 blocks raises that cradle's worker/larva allowance from three to six. Only one Sail per cradle; without support, workers beyond the first three pause.
- Empty-hand interaction reports activity. Shears retire your helper and return cargo directly into a free inventory slot. Other players cannot mutate or retire it.
- Lifespan: 20 minutes of world time. Empty expired helpers disappear without drops; carrying helpers finish delivery first. Blocked cargo can retain a reservation indefinitely rather than lose items. Unloaded helpers retain reservations until loaded/removed.

The dimension-wide cap is **64 total symbionts**, recorded in bounded saved data. No spawn egg or normal summon entry point bypasses admission. Death releases a reservation and returns at most one exceptional cargo stack; routine work and expiry create no loose items. The lab supplies one initial pickup stack, not an item-production loop.

Death now removes the helper and releases capacity immediately, without accumulating lingering corpses. A lethal-damage regression covers killing all three and hatching a replacement in the same tick. This fixes a short death-animation delay, not a proven explanation for every persistent refusal. Cradle interaction now reports the specific hatch blocker. There is no re-home interaction yet: shears retire an owned helper and recover cargo; rebuilding its cradle elsewhere does not relocate the original helper.

`brood` owns admission, ownership, morphology, lifecycle, cargo, movement and activity. Its package-private `NurseryLayout` owns pure perimeter geometry; `NurserySite` owns bounded world clearance; the roster owns persisted berth reservations. It consumes public tissue routes and the bounded storage adapter. This is explicit-route movement, not a general-purpose pathfinder. Arbitrary terrain needs a separate experiment.

## Aerocyte Bloom: aerial defense experiment

Craft an **Aerocyte Bloom** after discovering Spore Culture (bone, phantom membrane, blaze rods, slime and culture). Leave the crown's overhead space clear. Feed rotten flesh to its base: eight shots per item, 64 maximum. A feed is refused when fewer than eight slots remain; it never silently discards excess fuel. Empty-hand interaction reports charges, shots, biomass and the current blocker. Its saved block item retains unused ammunition and biomass. **250 biomass also produces eight shots**: an empty bloom draws from a directly touching reservoir, or a Transport Organ can supply it through an amber route. It retains at most 250 additional biomass while the magazine is full; partial batches wait without loss.

Default targets are phantoms, blazes, ghasts, withers and dragons, selected by the additive `infestusfrontier:aerial_threats` entity-type tag. Players and owned entities are always excluded. Targets need clear terrain line of sight and must be within 24 blocks of the muzzle. The organ fires at most once per 40 ticks for four base damage. Dragon body reduction still applies. A custom damage type explicitly participates in vanilla's dragon-damage tag; no fake player or invulnerability bypass is used. Wither spawn protection is respected; its arrow-only armored immunity does not apply to this non-arrow spore attack. Normal boss movement, retaliation, healing and terrain destruction remain intact. This is not proof that one organ can win a boss fight.

The spore lance deals an instantaneous hit; one green pellet follows as a cosmetic, non-collidable visual, with no particle trail. The rooted sac breathes, the crown aims, and shell petals open during firing. Inventory and world render the same model. [Original model art](art/defense/AEROCYTE.md) and [green spore art and exact built-in imagegen prompt](art/defense/GREEN_SPORE.md) are committed with their exporters.

**Defense kills suppress normal item loot and experience**, including player-assisted kills finished by the organ. This prevents loaded defenses from becoming uncontrolled item/orb farms. A future bounded collection organ needs a separate contract. Vanilla scripted boss outcomes such as End progression are not replaced.

The lab places the manually fed bloom at **112, 231, 11**, with a clearly named, no-AI Blaze specimen with extra health for repeated visual testing. It is a lab target, not a buff to normal Blazes. A second bloom at **100, 231, 0** receives only harvested biomass through the new loop below. The showcase captures 24 phases, including hill logistics, defense motion, the crown and digestion. Real mob damage, obstruction, ammunition persistence, loot suppression and the shared 64-organ stress case are covered by GameTests; complete moving-boss fights and multiplayer performance are not certified.

## Digestive Tissue and the biomass loop

### Ground-level patch → vein → reservoir

A reservoir-side conduit organ that pulls through the vein is an alternative under discussion, not implemented by this pass. It should reuse transport quotas and retained-cargo rules, with explicit source/junction selection rather than an unbounded nearby-source search. The working option below places the organ at the source end.

An amber vein touching a digestive patch is not a pump by itself. To keep both at the same height:

1. Start the green vein **immediately beside a Digestive Tissue cell** (green pores), not beside an intervening ordinary substrate cell. Point its first outlet **away from the patch**, along the route toward storage.
2. Place a **Transport Organ on top of that first vein**. Use a **Synaptic Probe** on the organ and select **Intake behind first vein**. The source is the ground cell behind the first outlet, one block below and one block sideways from the organ.
3. Keep the reservoir **on top of the final vein cell**. Point each vein toward the next; the final outlet points beyond the route. At least two vein cells are required. The patch can be irregular but must be face-connected and no larger than 64 cells.
4. The probe opens its configuration screen; automatic pulses run about once per second. Only actual damage generates biomass, so a fresh patch with no injured occupants is empty.

Choose **Intake above pump** to restore the original intake. Existing organs and saves keep that default. The selected mode and any retained cargo survive saving and the organ's portable block drop. Changing intake does not discard a blocked batch; it must be delivered before extracting from the new source. The first outlet must be horizontal in surface mode; later parts of the route may turn or climb using the existing rules. The organ does not search for a source or silently choose between multiple nearby inventories. Ordinary fluid/item endpoints can use the same position option, with the existing bottom-face capability access unchanged.

Use a **fermented spider eye on mature Living Substrate**, or craft substrate with the eye. Green pores identify the defensive surface. Living entities without all four correctly equipped Symbiotic pieces take two base damage at most once per second. Any complete bio suit protects; no branch or level is required here. **Players and pets are vulnerable too.** Creative invulnerability and ordinary armor mitigation remain. Digestion causes no knockback. Only actual health lost yields biomass (ten units per health point, rounded down, maximum twenty per contact); resisted hits and absorption generate none.

Each cell buffers at most 250 units. A full cell still defends, but excess yield is not generated. Face-connected Digestive Tissue forms a harvesting patch of at most 64 cells, including irregular shapes. Larger patches or unloaded boundaries refuse extraction without losing contents. Ordinary tissue can separate patches. Cells have no ticker; there is no automatic spread, DNA collection or plant assimilation. Automated mob kills suppress loot and XP; normal player death inventory drops are preserved.

The existing green channel accepts **biomass between biological endpoints**, or ordinary fluid between fluid endpoints. These are separate resources: a Fluid Bladder or vanilla bucket cannot contain biomass. A blocked destination retains at most 250 biomass inside the Transport Organ, and stops new extraction. Changing to the item channel pauses that retained biomass. No physical items are ejected. Break drops preserve local/buffered quantities in one block item.

The original below-floor arrangement also remains available:

1. Put a Transport Organ **under an edge cell of the Digestive Tissue floor**, then put green tissue beneath the organ. The floor is its source, and the pump can collect from the connected patch.
2. Lay a directed green route; put a reservoir **above the final tissue cell**.
3. Build a second route with a Transport Organ below another cell of that reservoir, green tissue beneath that organ, and the bloom above the final tissue cell. Keep the bloom's crown clear.

The lab now demonstrates surface intake: its 3×3 patch is at **X=85–87, Y=231, Z=-2–0**, with a stationary, extra-health Husk specimen. The first vein is **88,231,-1**, the surface-mode organ is **88,232,-1**, and the two-cell reservoir starts at **96,232,-1**. The patch and first route share the same height. The second pump remains **96,231,0**, using default above-organ intake to feed the bloom. The specimen keeps normal physics with its goals removed—`NoAI` would stop the real ground-contact test. No source biomass is seeded in this client loop. The capture asserts the second bloom really received harvested fuel.

`digestion` owns contact, yield and patch storage; pure `Digestion` rules own yield/cooldown limits. `multiblock.BoundedRegion` owns bounded six-neighbor discovery. `logistics.BiomassPorts` owns the scalar capability contract, consumed by transport and implemented by reservoir, digestion and defense. `combat.DefensiveDamage` owns shared automated damage/reward policy. Dependencies stay inward; no feature reads another feature's private NBT or buffers.

`defense` owns aerial target policy, ammunition, pending fuel and persistence. It consumes the shared combat/reward policy and biomass port contract. Its package-private rules have no world dependencies; its client adapter consumes the owning module's state and the shared particle-effect framework. Other modules use registration, the item/block handles and an immutable inspection view. No feature reaches into its stored ammunition or animation implementation.

Hill/defense validation, 2026-09-06: `prototypeCheck` passed **61 unit tests and 54 required GameTests**, plus generated-resource/JAR checks. A separate `runColonyShowcase` completed all fifteen captures with no error log entries; hill close-up, firing trail, inventory preview and crown views were visually inspected. These captures establish the current prototype appearance, not production art quality or measured frame time.

Visual polish validation, 2026-09-06: **62 unit tests / 54 GameTests** passed. The eight-phase biology showcase and fifteen-phase colony showcase completed; empty/full reservoir views and the single green pellet in flight were inspected. Defense captures now wait for shot age 5–7 instead of accidentally capturing between shots. This is still cosmetic flight after instantaneous server damage.

Connected biomass validation, 2026-09-06: `prototypeCheck` passes **70 unit tests / 64 required GameTests** in both default and `-PwithoutCurios` profiles, including resource/JAR verification. Coverage includes real ground contact, partial/full suit protection, no-knockback contact, zero yield from invulnerability, automated mob loot/XP suppression, player-drop event preservation, catalyst mutation, partial-fuel persistence, magazine limits, blocked-route recovery, bottle transfer, oversized patch refusal, a 64-contact stress case and the harvest → reservoir → bloom loop. The seventeen-phase client showcase completed without error log entries; the unseeded live biomass loop and digestive surface were visually inspected. These checks are not a multiplayer, moving-boss or chunk-loader soak certification.

## Base-building direction (proposal)

Build colonies from specialized tissue regions and organs. The prototypes establish three spatial constraints: sealed lifts, directed resource paths, and walkable helper trails. Next candidates are selective junction organs, one-way valves, channel isolation and local nutrient/pressure budgets. Compact builds could rely on routing; spacious builds could use parallel paths and couriers. Higher tiers should add coordination options rather than erase constraints with unlimited throughput/range. Ownership, upkeep and defense rules still need discussion; these are not final progression requirements.

## Validation snapshot — 2026-09-06

- `prototypeCheck`: 38 unit tests and 38 required GameTests passed; generated-resource/JAR verification passed.
- `prototypeCheck -PwithoutCurios`: the full gate also passed without the optional runtime mod.
- `runColonyShowcase`: seven captured phases passed, including repeated opening of the same saved lab. Native armor model/trim selection and actual lift ascent are asserted in the client.
- `runBiologyShowcase` and `runVisualShowcase`: prior multiblocks, substrate, animated blocks and chamber UI regression captures passed and were visually inspected.
- Real defects found during iteration included missing native armor-enchantment tags, stale generated guide/language content in the JAR, undersized shared GameTest fixtures, and asynchronous entity loading during lab reinitialization. Those cases now have executable checks or a repeated graphical reproduction.

These checks do not establish final artwork quality, arbitrary-terrain pathfinding, every third-party storage contract, or multiplayer latency/TPS performance. The material UVs, vertical conduit direction icons and final mutation balance remain explicitly experimental.
