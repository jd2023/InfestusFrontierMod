package org.jd.infestusfrontier.mining;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The Work Bed's screen: its slots as two rows of wells under one label, a line for what it holds and one for what it
 * has received.
 */
final class WorkBedScreen extends OrganScreen<WorkBedMenu> {
    private static final String KEY = "gui.infestusfrontier.work_bed.";
    private static final int STATUS_Y = Sheet.ROW2_Y + Sheet.SLOT.height() + 4;
    private static final int LINE = 11;

    WorkBedScreen(WorkBedMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WorkBedMenu.STRIPS);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        Component story = Component.translatable(KEY + "slots.story", Fracturing.BED_SLOTS);
        for (int slot = 0; slot < Fracturing.BED_SLOTS; slot++) {
            Component label = slot == 0 ? Component.translatable(KEY + "slots") : Component.empty();
            if (slot < WorkBedMenu.ROW) add(new Well(row, menu.getSlot(slot), label).story(story));
            else
                add(new Well(
                                Sheet.FIRST_PART_X + (slot % WorkBedMenu.ROW) * WorkBedMenu.PITCH,
                                Sheet.ROW2_LABEL_Y,
                                Sheet.ROW2_Y,
                                menu.getSlot(slot),
                                Component.empty())
                        .story(story));
        }
        int width = Sheet.RIGHT_EDGE - Sheet.FIRST_PART_X;
        add(new Tally(
                Sheet.FIRST_PART_X,
                STATUS_Y,
                width,
                () -> Component.translatable(KEY + "held", menu.held()),
                () -> Component.translatable(KEY + "held.story")));
        add(new Tally(
                Sheet.FIRST_PART_X,
                STATUS_Y + LINE,
                width,
                () -> Component.translatable(KEY + "received", menu.received()),
                () -> Component.translatable(KEY + "received.story")));
    }
}
