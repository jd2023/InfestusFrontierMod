package org.jd.infestusfrontier.processing;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.foundation.screen.Wells;
import org.jd.infestusfrontier.processing.api.PalmContents;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;
import org.jd.infestusfrontier.ui.SlotGrid;

/**
 * Client view of the Palm: the nine grid wells it keeps, the result well beside them, a line saying whether it has
 * Reach and a link, and the three supply buttons under the grid. It holds no biomass.
 */
final class ShapingPalmScreen extends OrganScreen<ShapingPalmMenu> {
    private static final SlotGrid GRID = SlotGrid.standard3x3();
    private static final Component INGREDIENTS = Component.translatable("gui.infestusfrontier.shaping_palm.grid");
    private static final Component RESULT = Component.translatable("gui.infestusfrontier.shaping_palm.result");
    private static final Component RESULT_STORY =
            Component.translatable("gui.infestusfrontier.shaping_palm.result.story");
    private static final int LABEL_GAP = Sheet.PARTS_Y - Sheet.LABEL_Y;
    private static final String KEY = "gui.infestusfrontier.shaping_palm.";
    private static final String[] BUTTONS = {"restock", "return", "store"};
    private static final int BUTTON_GAP = 2;
    private static final int LINE = 9;

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
        int textX = out.x() - 16;
        int textY = out.y() + Sheet.SLOT.height() + 3;
        int width = Sheet.RIGHT_EDGE - textX;
        add(new Tally(
                textX,
                textY,
                width,
                () -> Component.translatable(KEY + (menu.reachState() == 0 ? "no_reach" : "reach")),
                () -> Component.translatable(KEY + "reach.story")));
        add(new Tally(
                textX,
                textY + LINE - 1,
                width,
                () -> menu.reachState() == 2
                        ? Component.translatable(
                                KEY + "linked",
                                signed(menu.linkOffset(0)),
                                signed(menu.linkOffset(1)),
                                signed(menu.linkOffset(2)))
                        : Component.translatable(KEY + "not_linked"),
                () -> Component.translatable(KEY + "link.story")));
        int y = GRID.bounds().bottom() + 3;
        for (int id = 0; id < BUTTONS.length; id++) {
            int action = id;
            button(
                            Sheet.LEFT_X + id * (Sheet.BUTTON.width() + BUTTON_GAP),
                            y,
                            Component.translatable(KEY + BUTTONS[id]),
                            b -> send(action))
                    .setTooltip(Tooltip.create(Component.translatable(KEY + BUTTONS[id] + ".story")));
        }
    }

    private static String signed(int offset) {
        return offset > 0 ? "+" + offset : Integer.toString(offset);
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
}
