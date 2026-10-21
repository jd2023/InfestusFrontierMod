package org.jd.infestusfrontier.logistics;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The Mouth's settings: the filter as three rows of wells, and a column of buttons for the mode, the list kind, the
 * amount and the priority. A shift-click steps the amount and the priority down. The screen sends button ids; the
 * server decides.
 */
final class MouthScreen extends OrganScreen<MouthMenu> {
    private static final int GRID = MouthMenu.FILTER_COLUMNS * MouthMenu.PITCH;
    private static final int BUTTONS_X = Sheet.FIRST_PART_X + GRID + 2 * Sheet.GAP;
    private static final String KEY = "gui.infestusfrontier.mouth.";

    private Button mode;
    private Button list;
    private Button amount;
    private Button priority;

    MouthScreen(MouthMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MouthMenu.STRIPS);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Tally(
                Sheet.FIRST_PART_X,
                Sheet.LABEL_Y,
                GRID,
                () -> Component.translatable(KEY + "filter"),
                () -> Component.translatable(KEY + "filter.story")));
        for (int slot = 0; slot < MouthBlockEntity.FILTER_SLOTS; slot++) {
            int y = Sheet.PARTS_Y + (slot / MouthMenu.FILTER_COLUMNS) * MouthMenu.PITCH;
            add(new Well(
                    Sheet.FIRST_PART_X + (slot % MouthMenu.FILTER_COLUMNS) * MouthMenu.PITCH,
                    y,
                    y,
                    menu.slots.get(slot),
                    Component.empty()));
        }
        add(new Tally(
                BUTTONS_X,
                Sheet.LABEL_Y,
                Sheet.RIGHT_EDGE - BUTTONS_X,
                () -> Component.translatable(KEY + "carried", menu.carried()),
                () -> Component.translatable(KEY + "carried.story")));
        int y = Sheet.partY(Sheet.BUTTON.height());
        mode = button(BUTTONS_X, y, Component.empty(), b -> send(MouthMenu.MODE_BUTTON));
        mode.setTooltip(Tooltip.create(Component.translatable(KEY + "mode.story")));
        list = button(BUTTONS_X, y + MouthMenu.PITCH, Component.empty(), b -> send(MouthMenu.LIST_BUTTON));
        list.setTooltip(Tooltip.create(Component.translatable(KEY + "list.story")));
        amount = button(
                BUTTONS_X,
                y + 2 * MouthMenu.PITCH,
                Component.empty(),
                b -> send(Screen.hasShiftDown() ? MouthMenu.AMOUNT_DOWN : MouthMenu.AMOUNT_UP));
        amount.setTooltip(Tooltip.create(Component.translatable(KEY + "amount.story")));
        priority = button(
                BUTTONS_X,
                y + 3 * MouthMenu.PITCH,
                Component.empty(),
                b -> send(Screen.hasShiftDown() ? MouthMenu.PRIORITY_DOWN : MouthMenu.PRIORITY_UP));
        priority.setTooltip(Tooltip.create(Component.translatable(KEY + "priority.story")));
        label();
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        label();
    }

    /** The buttons name the settings the server last sent. */
    private void label() {
        MouthMode current = menu.mode();
        mode.setMessage(Component.translatable(KEY + "mode." + current.getSerializedName()));
        list.setMessage(Component.translatable(KEY + (menu.deny() ? "deny" : "allow")));
        amount.setMessage(
                menu.amount() == 0
                        ? Component.translatable(KEY + "amount.none")
                        : Component.translatable(
                                KEY + (current == MouthMode.OUTPUT ? "amount.keep" : "amount.fill"), menu.amount()));
        priority.setMessage(Component.translatable(KEY + "priority", signed(menu.priority())));
        priority.active = current.takes();
        amount.active = current != MouthMode.CLOSED;
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }
}
