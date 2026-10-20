package org.jd.infestusfrontier.processing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Trough;
import org.jd.infestusfrontier.foundation.screen.Unit;
import org.jd.infestusfrontier.foundation.screen.Vein;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * Client view of the Cyst on a two-strip panel: three input wells under one label on the row, the output well and
 * the progress vein on the second row, the water trough under them, and the biomass tank it draws on.
 */
final class ActivationCystScreen extends OrganScreen<BatchOrganMenu> {
    private static final int STRIPS = 2;
    private static final int TROUGH_Y = Sheet.ROW2_Y + Sheet.SLOT.height() + Sheet.GAP;
    private static final Component INPUTS = Component.translatable("gui.infestusfrontier.batch_organ.inputs");
    private static final Component OUT = Component.translatable("gui.infestusfrontier.batch_organ.out");
    private static final Component WATER = Component.translatable("gui.infestusfrontier.batch_organ.water");

    ActivationCystScreen(BatchOrganMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, STRIPS);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Emblem(Sheet.CYST_EMBLEM));
        add(new Well(row, menu.getSlot(0), INPUTS));
        add(new Well(row, menu.getSlot(1), Component.empty()));
        add(new Well(row, menu.getSlot(2), Component.empty()));
        Bounds out = menu.outputWell();
        add(new Well(out.x(), Sheet.ROW2_LABEL_Y, out.y(), menu.getSlot(menu.outputSlot()), OUT));
        add(new Vein(out.right() + Sheet.GAP, Sheet.ROW2_LABEL_Y, menu::percent));
        add(new Trough(
                Sheet.FIRST_PART_X,
                TROUGH_Y,
                Sheet.LEFT_X,
                Sheet.PARTS_END,
                WATER,
                menu::water,
                menu::waterCapacity,
                Unit.WATER));
    }
}
