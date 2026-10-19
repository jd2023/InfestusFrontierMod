package org.jd.infestusfrontier.processing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Trough;
import org.jd.infestusfrontier.foundation.screen.Unit;
import org.jd.infestusfrontier.foundation.screen.Vein;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.foundation.screen.Wells;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;
import org.jd.infestusfrontier.ui.SlotGrid;

/**
 * Client view of the Bowl on a two-strip panel: nine ingredient wells, the progress vein and the output well beside
 * them, the water trough under them, and the biomass tank.
 */
final class CultureBowlScreen extends OrganScreen<BatchOrganMenu> {
    private static final int STRIPS = 2;
    private static final SlotGrid GRID = SlotGrid.standard3x3();
    private static final int RIGHT_COLUMN_X = GRID.bounds().right() + Sheet.GAP;
    private static final int TROUGH_Y = GRID.bounds().bottom() + Sheet.GAP;
    private static final Component INGREDIENTS = Component.translatable("gui.infestusfrontier.batch_organ.ingredients");
    private static final Component OUT = Component.translatable("gui.infestusfrontier.batch_organ.out");
    private static final Component WATER = Component.translatable("gui.infestusfrontier.batch_organ.water");

    CultureBowlScreen(BatchOrganMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, STRIPS);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Emblem(Sheet.BOWL_EMBLEM));
        add(new Wells(row, GRID, menu.slots.subList(0, menu.ingredientSlots()), INGREDIENTS));
        add(new Vein(row, menu::percent));
        add(new Well(RIGHT_COLUMN_X, Sheet.ROW2_LABEL_Y, Sheet.ROW2_Y, menu.getSlot(menu.outputSlot()), OUT));
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
