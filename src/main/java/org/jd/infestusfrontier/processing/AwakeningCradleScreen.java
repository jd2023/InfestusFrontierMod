package org.jd.infestusfrontier.processing;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Vein;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * Client view of the Cradle on a one-strip panel: the piece and culture wells and the progress vein on the row, the
 * output well and the Start button on the second row, and the biomass tank it draws on.
 */
final class AwakeningCradleScreen extends OrganScreen<BatchOrganMenu> {
    private static final int STRIPS = 1;
    private static final Component PIECE = Component.translatable("gui.infestusfrontier.awakening_cradle.piece");
    private static final Component OUT = Component.translatable("gui.infestusfrontier.batch_organ.out");
    private static final Component START = Component.translatable("gui.infestusfrontier.batch_organ.start");
    private Button start;

    AwakeningCradleScreen(BatchOrganMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, STRIPS);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Emblem(Sheet.CRADLE_EMBLEM));
        add(new Well(row, menu.getSlot(0), PIECE));
        add(new Well(row, menu.getSlot(1), Component.empty()));
        add(new Vein(row, menu::percent));
        Bounds out = menu.outputWell();
        add(new Well(
                out.x(), out.y() - (Sheet.PARTS_Y - Sheet.LABEL_Y), out.y(), menu.getSlot(menu.outputSlot()), OUT));
        start = button(
                out.right() + Sheet.GAP,
                out.y() + 1,
                START,
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BatchOrganMenu.START_BUTTON));
    }

    @Override
    protected void containerTick() {
        start.active = menu.state() == BatchOrganMenu.IDLE;
    }
}
