package org.jd.infestusfrontier.processing;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.Glyph;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.foundation.screen.Wells;
import org.jd.infestusfrontier.processing.api.PalmContents;
import org.jd.infestusfrontier.processing.api.PalmRote;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;
import org.jd.infestusfrontier.ui.SlotGrid;
import org.jetbrains.annotations.Nullable;

/**
 * Client view of the Palm: the nine grid wells it keeps, the result well beside them, a line saying whether it has
 * Reach (or, with Rote, what the cycle is doing) and the direction of its Mouth, and the three supply buttons under the grid. With Rote it
 * also has a biomass tank, Select and the four-slot tray on a third row.
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
    /** One line for each reach state of {@link ShapingPalmMenu#reachState}. */
    private static final String[] MOUTH_LINES = {
        "mouth.no_reach", "mouth.none", "mouth.closed", "mouth.takes", "mouth.gives", "mouth.both"
    };

    private static final int BUTTON_GAP = 2;
    /** Rows between the emblem and the Mouth symbol under it. */
    private static final int MOUTH_SYMBOL_GAP = 3;

    private static final int LINE = 9;

    ShapingPalmScreen(ShapingPalmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.strips());
    }

    @Override
    protected boolean showsBiomass() {
        return menu.hasRote();
    }

    @Override
    protected void addParts(PartRow row) {
        Bounds out = ShapingPalmMenu.resultWell(menu.hasRote());
        add(new Emblem(Sheet.PALM_EMBLEM));
        Bounds lips = Sheet.mouthSymbol(0);
        add(new Glyph(
                Sheet.EMBLEM_X + (Sheet.emblem(Sheet.PALM_EMBLEM).width() - lips.width()) / 2,
                Sheet.EMBLEM_Y + Sheet.emblem(Sheet.PALM_EMBLEM).height() + MOUTH_SYMBOL_GAP,
                lips.width(),
                lips.height(),
                this::mouthSymbol,
                () -> Component.translatable(KEY + "mouth.story")));
        add(new Wells(
                row,
                GRID,
                menu.slots.subList(ShapingPalmMenu.FIRST_CELL, ShapingPalmMenu.FIRST_CELL + PalmContents.CELLS),
                INGREDIENTS));
        add(new Well(out.x(), out.y() - LABEL_GAP, out.y(), menu.getSlot(ShapingPalmMenu.RESULT), RESULT)
                .story(RESULT_STORY));
        int textX = out.x() - (menu.hasRote() ? 0 : 16);
        int textY = out.y() + Sheet.SLOT.height() + 3;
        int width = Sheet.RIGHT_EDGE - textX;
        if (menu.hasRote()) {
            add(new Tally(
                    textX,
                    textY,
                    width,
                    () -> Component.translatable(menu.status().key(), menu.percent()),
                    () -> Component.translatable(menu.status().key() + ".story")));
        } else {
            add(new Tally(
                    textX,
                    textY,
                    width,
                    () -> Component.translatable(KEY + (menu.reachState() == 0 ? "no_reach" : "reach")),
                    () -> Component.translatable(KEY + "reach.story")));
        }
        add(new Tally(
                textX,
                textY + LINE - 1,
                width,
                () -> Component.translatable(KEY + MOUTH_LINES[menu.reachState()]),
                () -> Component.translatable(KEY + "mouth.story")));
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
        if (menu.hasRote()) addRote();
    }

    /** The lips for the Mouth's direction, in the order closed, takes, gives, both; none without a Mouth to show. */
    private @Nullable Bounds mouthSymbol() {
        int direction = menu.reachState() - ShapingPalmMenu.MOUTH_CLOSED;
        return direction < 0 ? null : Sheet.mouthSymbol(direction);
    }

    /** Select under the supply buttons and the four tray wells beside it. */
    private void addRote() {
        int y = ShapingPalmMenu.trayY();
        button(
                        Sheet.LEFT_X,
                        y + (Sheet.SLOT.height() - Sheet.BUTTON.height()) / 2,
                        Component.translatable(KEY + "select"),
                        b -> send(ShapingPalmMenu.SELECT),
                        Sheet.STUD)
                .setTooltip(Tooltip.create(Component.translatable(KEY + "select.story")));
        for (int slot = 0; slot < PalmRote.TRAY; slot++) {
            Bounds well = ShapingPalmMenu.trayWell(slot);
            add(new Well(
                            well.x(),
                            well.y(),
                            well.y(),
                            menu.getSlot(ShapingPalmMenu.FIRST_TRAY + slot),
                            Component.empty())
                    .mark(Sheet.trayMark(slot))
                    .story(Component.translatable(KEY + (slot == 0 ? "tray.result" : "tray.remainder"))));
        }
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
}
