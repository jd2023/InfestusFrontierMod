package org.jd.infestusfrontier.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/** Client view of a capsule cell on the plain panel: its four compartment wells and the body's size and use. No tank. */
final class ItemCapsuleScreen extends OrganScreen<ItemCapsuleMenu> {
    private static final Component COMPARTMENTS =
            Component.translatable("gui.infestusfrontier.item_capsule.compartments");

    ItemCapsuleScreen(ItemCapsuleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Well(row, menu.getSlot(0), COMPARTMENTS));
        for (int slot = 1; slot < ItemCapsuleBlockEntity.COMPARTMENTS; slot++)
            add(new Well(row, menu.getSlot(slot), Component.empty()));
        add(new Tally(
                Sheet.FIRST_PART_X,
                Sheet.ROW2_LABEL_Y,
                Sheet.RIGHT_EDGE - Sheet.FIRST_PART_X,
                () -> Component.translatable(
                        "gui.infestusfrontier.item_capsule.body",
                        menu.sizeX(),
                        menu.sizeY(),
                        menu.sizeZ(),
                        menu.used(),
                        menu.compartments()),
                () -> Component.translatable("gui.infestusfrontier.item_capsule.body.story")));
    }
}
