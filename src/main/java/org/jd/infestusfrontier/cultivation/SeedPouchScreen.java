package org.jd.infestusfrontier.cultivation;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/** Client view of the pouch on the plain panel: four stock wells and the reserve line. No tank. */
final class SeedPouchScreen extends OrganScreen<SeedPouchMenu> {
    private static final Component STOCK = Component.translatable("gui.infestusfrontier.seed_pouch.stock");
    private static final Component RESERVE_STORY =
            Component.translatable("gui.infestusfrontier.seed_pouch.reserve.story");

    SeedPouchScreen(SeedPouchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Well(row, menu.getSlot(0), STOCK));
        for (int slot = 1; slot < SeedPouchBlockEntity.SLOTS; slot++)
            add(new Well(row, menu.getSlot(slot), Component.empty()));
        add(new Tally(
                Sheet.FIRST_PART_X,
                Sheet.ROW2_LABEL_Y,
                Sheet.RIGHT_EDGE - Sheet.FIRST_PART_X,
                () -> Component.translatable("infestusfrontier.status.reserve", menu.reserve()),
                () -> RESERVE_STORY));
    }
}
