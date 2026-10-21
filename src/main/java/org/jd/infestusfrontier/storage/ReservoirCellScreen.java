package org.jd.infestusfrontier.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/** Client view of a cell on the plain panel: the Bladder's emblem, the body's size and the body's tank. */
final class ReservoirCellScreen extends OrganScreen<ReservoirCellMenu> {
    ReservoirCellScreen(ReservoirCellMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Emblem(Sheet.BLADDER_EMBLEM));
        add(new Tally(
                Sheet.FIRST_PART_X,
                Sheet.PARTS_Y,
                Sheet.PARTS_END - Sheet.FIRST_PART_X,
                () -> Component.translatable(
                        "gui.infestusfrontier.reservoir_cell.body", menu.sizeX(), menu.sizeY(), menu.sizeZ()),
                () -> Component.translatable("gui.infestusfrontier.reservoir_cell.body.story", menu.cells())));
    }
}
