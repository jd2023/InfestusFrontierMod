# Fresh-world Survival playtest and showcase

Start in a fresh Survival world. The finish line is a living workshop that farms wheat, makes biomass, and powers its own furnace. Each step gives a visible result to check.

## 1. Establish a small vanilla base near water

Make tools, a furnace, a bucket, and a wheat patch. Gather red mushrooms, rotten flesh, wheat seeds, bones, string, clay, sand, and slime balls as you explore. Keep some wheat for the colony.

**Check:** Note whether finding mushrooms or slime makes the opening feel stalled.

## 2. Make your first Spore Culture

Craft **rotten flesh + red mushroom + wheat seeds**. Craft the guide, *The Waking Genome*, from **a book + Spore Culture**.

Use another culture on exposed dirt, grass, mud, or stone.

**Check:** The targeted block becomes Living Substrate. Leave it alone briefly—it should not spread without your input.

## 3. Grow a workshop foundation

Apply bone meal to the substrate. Inspect the mature center and younger surrounding cells; feed the edges to expand.

Leave room for several machines and paths between them. Most organs need Living Substrate underneath.

**Check:** Growth follows your actions. Break one expendable cell: it should become Dead Tissue rather than a movable substrate block.

## 4. Start producing and storing biomass

Craft an **Organ Bud** from **Spore Culture + two rotten flesh + bone meal**. Use buds to make:

- **Digestive Sac:** Bud + two rotten flesh + bowl.
- **Biomass Bladder:** Bud + two glass + slime ball.

Place them beside each other on substrate. Feed wheat to the Sac.

**Check:** Wheat becomes biomass—100 BU per wheat—and the adjacent Bladder fills automatically. Try taking out a biomass bucket once you have 1,000 BU.

## 5. Build the material-making workshop

Add these organs, using the guide's recipes:

| Organ | First operation to try |
|---|---|
| Culture Bowl | Red mushroom + wheat seeds + water → Spore Culture |
| Membrane Rack | Rotten flesh + string + water → Membrane Sheet |
| Bone Loom | Bone + biomass → Bone Plate |

Watch each machine idle, start working, finish, and wait for more ingredients. Try feeding one through a hopper.

**Check:** Machines start automatically when supplied. Ingredients, fluids, progress, and outputs should be understandable from their screens.

## 6. Put the biomass to useful work

Craft a **Bio-Furnace** from **furnace + Organ Bud + two Membrane Sheets**. Supply biomass and smelt some cobblestone or raw ore.

Queue **64 items** while you continue building: its second growth stage begins after 64 completed smelts.

**Check:** It produces normal furnace results, spends biomass, and visibly changes as it grows. Collect the output and its experience.

## 7. Exercise the Item Capsule properly

Craft capsules from **chest + two Membrane Sheets + Spore Culture**. Build one cell, then a row, then a **2×2×2 solid box**. Walk around while placing cells, changing the direction you face.

Insert several item types and more than one stack of a common item. Open the capsule from different sides and browse both inventory pages.

**Check:** One consistent front, continuous outer walls, shared inventory, and correct counts. Break one filled cell and replace it; its contents should travel with it.

## 8. Build an Activation Cyst and your first transport lines

Craft the Cyst from **Organ Bud + two Membrane Sheets + Bone Plate**. Use the guide to make the treatments.

Grow these from exposed mature substrate:

- **Capillary Gel → Biomass Vein**
- **Contractile Fiber → Item Vein**
- **Filter Membrane, then Sealing Resin → working Fluid Vein**

Make a **Vascular Junction** and **Synaptic Probe**. Configure a Junction intake toward a biomass store and an outlet toward a vein leading to the furnace.

**Check:** Biomass reaches the furnace without bucket trips. Closed ports stop transfer; changing them restores it.

## 9. Demonstrate item routing with two containers

Put a Mouth under a source chest and another under a destination capsule. Connect their sides with Item Veins.

Set the source Mouth to **Output**, the destination to **Intake**, and allow only one item type at the destination. Put a mixed assortment in the source.

**Check:** Only the allowed item moves. Fill the destination and verify that transport waits without spilling items. Free space and confirm it resumes.

## 10. Build a small farm with overlapping working areas

Use Rooting Gel on mature substrate to make Cultivation Tissue. Supply water and try planting wheat manually first.

Then build this layout:

```text
    B B B
  S P H P S
    B Y B
```

`B` = crop bed in the ground. The other letters are organs standing above ground:

- `S` = Seed Pouch.
- `P` = Planting Proboscis.
- `H` = Harvest Corolla.
- `Y` = Yield Lobe facing the Corolla.

Supply biomass to both planters and the harvester. Put 16 seeds in each pouch and set a reserve of 4.

**Check:** Planters plant empty beds. The Corolla harvests ripe crops. Pouches retain their protected seeds. Bone meal can speed up a demonstration; also observe a naturally grown harvest.

## 11. Connect the farm into a supply loop

Put an Output Mouth under the Yield Lobe. Route:

- Wheat seeds back to Intake Mouths under the pouches; allow seeds and target 16.
- Wheat to an Intake Mouth under the Digestive Sac; allow wheat.
- Biomass from storage back to the farm and furnace.
- Water from a separate Junction and sealed Fluid Veins to the beds.

Use a replenishing water-source pool. Add a **Filter Valve** to protect **1,000 BU** in storage.

**Check:** Watch three harvest/replant cycles. Record biomass before and after, allowing production to settle. Seeds should return, the protected reserve should remain available, and the workshop should produce a surplus.

## 12. Show off larger storage and controls

Expand biomass storage into a rectangular Reservoir body. Inspect its front, back, corners, and liquid surface as it fills and drains.

Point a **Sensor Polyp** at storage and connect its redstone output to a visible indicator.

**Check:** Joined cells look like one tank. The signal changes with fullness. Draining through the Filter Valve stops at its reserve.

## 13. Awaken and refuel living armor

Craft a dormant armor piece using rotten flesh in the usual armor pattern. Build an **Awakening Cradle**, then supply the piece, Spore Culture, and biomass.

Fill the awakened piece through the Bladder's equipment slot. Wear it and inspect it in third person. Later, supply a **Fuel Papilla** through a biomass line and stand on it wearing an underfilled piece.

**Check:** Awakening changes the piece, its fuel count survives removal and re-equipping, and the Papilla fills it to the selected limit.

## 14. Give the workshop a living exterior

Sneak-use bone meal on mature substrate to grow a Living Wall; grow it upward. Mutate sections into Membrane Windows and a Sphincter Door.

Add a Tread Receptor in front of the doorway. Try the hardened doors with redstone controls.

**Check:** Windows join, doors animate cleanly, and the receptor opens the entrance when stepped on. Remember that mobs can also activate it.

## 15. Finish with a reliability test

While the workshop is working, save and quit. Reload, check inventories and reserves, then let production continue. Temporarily fill an output container and remove a transport connection; restore both afterward.

Use a **Surgical Blade** to move an organ that has contents or completed work.

**Check:** No duplicated or missing stock, no reset growth history, no loose-item flood, and production resumes when its supplies and output space return.

## Final showcase

Demonstrate one continuous sequence: **harvest wheat → route it to the Sac → store biomass → power the furnace → refuel your armor**, with the capsule, reservoir, and living doorway visible around it.
