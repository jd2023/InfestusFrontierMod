package org.jd.infestusfrontier.processing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.foundation.screen.Wells;
import org.jd.infestusfrontier.processing.api.PalmContents;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;
import org.jd.infestusfrontier.ui.SlotGrid;

/** Client view of the Palm: the nine grid wells it keeps and the result well beside them. It holds no biomass. */
final class ShapingPalmScreen extends OrganScreen<ShapingPalmMenu> {
    private static final SlotGrid GRID = SlotGrid.standard3x3();
    private static final Component INGREDIENTS = Component.translatable("gui.infestusfrontier.shaping_palm.grid");
    private static final Component RESULT = Component.translatable("gui.infestusfrontier.shaping_palm.result");
    private static final Component RESULT_STORY =
            Component.translatable("gui.infestusfrontier.shaping_palm.result.story");
    private static final int LABEL_GAP = Sheet.PARTS_Y - Sheet.LABEL_Y;

    ShapingPalmScreen(ShapingPalmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ShapingPalmMenu.strips());
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        Bounds out = ShapingPalmMenu.resultWell();
        add(new Emblem(Sheet.PALM_EMBLEM));
        add(new Wells(
                row,
                GRID,
                menu.slots.subList(ShapingPalmMenu.FIRST_CELL, ShapingPalmMenu.FIRST_CELL + PalmContents.CELLS),
                INGREDIENTS));
        add(new Well(out.x(), out.y() - LABEL_GAP, out.y(), menu.getSlot(ShapingPalmMenu.RESULT), RESULT)
                .story(RESULT_STORY));
    }
}
