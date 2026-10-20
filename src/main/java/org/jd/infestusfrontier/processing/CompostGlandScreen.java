package org.jd.infestusfrontier.processing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Trough;
import org.jd.infestusfrontier.foundation.screen.Unit;
import org.jd.infestusfrontier.foundation.screen.Vein;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * Client view of the Compost Gland on the plain panel: the scrap well, the progress vein and the output well on the
 * row, the water trough under them. It holds no biomass, so no tank.
 */
final class CompostGlandScreen extends OrganScreen<BatchOrganMenu> {
    private static final int TROUGH_Y = Sheet.PARTS_Y + Sheet.SLOT.height() + Sheet.GAP;
    private static final Component SCRAPS = Component.translatable("gui.infestusfrontier.compost_gland.scraps");
    private static final Component OUT = Component.translatable("gui.infestusfrontier.batch_organ.out");
    private static final Component WATER = Component.translatable("gui.infestusfrontier.batch_organ.water");

    CompostGlandScreen(BatchOrganMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Emblem(Sheet.COMPOST_EMBLEM));
        add(new Well(row, menu.getSlot(0), SCRAPS));
        add(new Vein(row, menu::percent));
        add(new Well(row, menu.getSlot(menu.outputSlot()), OUT));
        add(new Trough(
                Sheet.FIRST_PART_X,
                TROUGH_Y,
                Sheet.LEFT_X,
                Sheet.RIGHT_EDGE,
                WATER,
                menu::water,
                menu::waterCapacity,
                Unit.WATER));
    }
}
