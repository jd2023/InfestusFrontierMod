package org.jd.infestusfrontier.processing;

import net.minecraft.client.gui.components.Button;
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
 * Client view of the Rack on a one-strip panel: two input wells under one label, the progress vein and the output well on
 * the row, the water trough and the Start button under them. The Rack holds no biomass, so no tank.
 */
final class MembraneRackScreen extends OrganScreen<BatchOrganMenu> {
    private static final int STRIPS = 1;
    private static final int TROUGH_Y = Sheet.PARTS_Y + Sheet.SLOT.height() + Sheet.GAP;
    private static final int START_Y = TROUGH_Y + Sheet.TROUGH_EMPTY.height() + Sheet.GAP;
    private static final Component INPUTS = Component.translatable("gui.infestusfrontier.batch_organ.inputs");
    private static final Component OUT = Component.translatable("gui.infestusfrontier.batch_organ.out");
    private static final Component WATER = Component.translatable("gui.infestusfrontier.batch_organ.water");
    private static final Component START = Component.translatable("gui.infestusfrontier.batch_organ.start");
    private Button start;

    MembraneRackScreen(BatchOrganMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, STRIPS);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Emblem(Sheet.RACK_EMBLEM));
        add(new Well(row, menu.getSlot(0), INPUTS));
        add(new Well(row, menu.getSlot(1), Component.empty()));
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
        start = button(
                Sheet.FIRST_PART_X,
                START_Y,
                START,
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BatchOrganMenu.START_BUTTON));
    }

    @Override
    protected void containerTick() {
        start.active = menu.state() == BatchOrganMenu.IDLE;
    }
}
