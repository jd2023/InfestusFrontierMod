# Synaptic Probe source

Created with the built-in imagegen tool, using the imagegen skill. Selected source: `synaptic-probe.png` (source PNG kept in the V3 prototype repository under `art/logistics/`). Runtime export: `src/main/resources/assets/infestusfrontier/textures/item/synaptic_probe.png`.

Prompt:

> Use case: stylized-concept. Asset type: Minecraft mod inventory item icon, downsampled to 64x64. A single original living sensory probe: short burgundy sinew handle, two smooth ivory chitin prongs framing a large luminous lime-green sensory pearl. Diagonal upright handheld silhouette, bold broad shapes, polished painted game icon, minimal fine grain. Entire object visible occupying 85 percent of square canvas. Genuinely transparent background with alpha, no cast shadow, no text, no border, no scene. Biological rather than mechanical; no existing game designs.

`java tools/ExportLogisticsControl.java` mechanically scales the selected original to 64×64 and exports code-native port geometry. `java tools/ExportColonyAssets.java biomass-pulse` regenerates only the two green biomass pulse sprites from the existing original material atlas and its established animation formula. Other channels and armor exports are untouched.
