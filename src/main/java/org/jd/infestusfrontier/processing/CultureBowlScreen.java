package org.jd.infestusfrontier.processing;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Status;
import org.jd.infestusfrontier.foundation.screen.Trough;
import org.jd.infestusfrontier.foundation.screen.Unit;
import org.jd.infestusfrontier.foundation.screen.Vein;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.foundation.screen.Wells;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;
import org.jd.infestusfrontier.ui.SlotGrid;
import org.jd.infestusfrontier.ui.StatusKind;

/**
 * Client view of the Bowl on a tall panel: nine ingredient wells, the progress vein and the output well beside
 * them, the water trough and the Start button under them, and the biomass tank.
 */
final class CultureBowlScreen extends OrganScreen<CultureBowlMenu> {
    private static final int STRIPS = 3;
    private static final SlotGrid GRID = SlotGrid.standard3x3();
    private static final int RIGHT_COLUMN_X = GRID.bounds().right() + Sheet.GAP;
    private static final int TROUGH_Y = GRID.bounds().bottom() + Sheet.GAP;
    private static final int START_Y = TROUGH_Y + Sheet.TROUGH_EMPTY.height() + Sheet.GAP;
    private static final Component INGREDIENTS =
            Component.translatable("gui.infestusfrontier.culture_bowl.ingredients");
    private static final Component OUT = Component.translatable("gui.infestusfrontier.culture_bowl.out");
    private static final Component WATER = Component.translatable("gui.infestusfrontier.culture_bowl.water");
    private static final Component START = Component.translatable("gui.infestusfrontier.culture_bowl.start");
    private static final Component IDLE = Component.translatable("gui.infestusfrontier.culture_bowl.idle");
    private static final Component WORKING = Component.translatable("gui.infestusfrontier.culture_bowl.working");
    private static final Component PAUSED = Component.translatable("gui.infestusfrontier.culture_bowl.paused");
    private Button start;

    CultureBowlScreen(CultureBowlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, STRIPS);
    }

    @Override
    protected Status status() {
        return switch (menu.state()) {
            case CultureBowlMenu.WORKING -> new Status(StatusKind.ACTIVE, WORKING);
            case CultureBowlMenu.PAUSED -> new Status(StatusKind.BLOCKED, PAUSED);
            default -> new Status(StatusKind.IDLE, IDLE);
        };
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Wells(row, GRID, menu.slots.subList(0, GRID.count()), INGREDIENTS));
        add(new Vein(row, menu::percent));
        add(new Well(RIGHT_COLUMN_X, Sheet.ROW2_LABEL_Y, Sheet.ROW2_Y, menu.getSlot(CultureBowlMenu.OUTPUT_SLOT), OUT));
        add(new Trough(
                Sheet.FIRST_PART_X,
                TROUGH_Y,
                Sheet.STATUS_GLYPH_X,
                Sheet.RIGHT_EDGE,
                WATER,
                menu::water,
                () -> CultureBowlMenu.WATER_CAPACITY,
                Unit.WATER));
        start = button(
                Sheet.FIRST_PART_X,
                START_Y,
                START,
                button ->
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CultureBowlMenu.START_BUTTON));
    }

    @Override
    protected void containerTick() {
        start.active = menu.state() == CultureBowlMenu.IDLE;
    }
}
