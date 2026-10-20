package org.jd.infestusfrontier.processing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Vein;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * Client view of the Bio-Furnace on the plain panel: the smelting well, the progress vein and the output well on
 * the row, the stage line under them, and the biomass tank it burns from.
 */
final class BioFurnaceScreen extends OrganScreen<BatchOrganMenu> {
    private static final Component SMELT = Component.translatable("gui.infestusfrontier.bio_furnace.smelt");
    private static final Component OUT = Component.translatable("gui.infestusfrontier.batch_organ.out");

    BioFurnaceScreen(BatchOrganMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Emblem(Sheet.FURNACE_EMBLEM));
        add(new Well(row, menu.getSlot(0), SMELT));
        add(new Vein(row, menu::percent));
        add(new Well(row, menu.getSlot(menu.outputSlot()), OUT));
        add(new Tally(
                Sheet.FIRST_PART_X,
                Sheet.ROW2_LABEL_Y,
                Sheet.PARTS_END - Sheet.FIRST_PART_X,
                () -> Component.translatable("gui.infestusfrontier.stage", menu.stage() + 1),
                () -> menu.toNextStage() == 0
                        ? Component.translatable("gui.infestusfrontier.stage.last")
                        : Component.translatable("gui.infestusfrontier.stage.story", menu.toNextStage())));
    }
}
