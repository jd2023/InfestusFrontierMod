package org.jd.infestusfrontier.power;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

final class PowerScreen extends OrganScreen<PowerMenu> {
    private net.minecraft.client.gui.components.Button mode;
    private static final String KEY = "gui.infestusfrontier.power.";

    PowerScreen(PowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PowerMenu.STRIPS);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        if (menu.driver()) {
            add(new Tally(
                    Sheet.LEFT_X,
                    Sheet.LABEL_Y,
                    Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                    () -> Component.translatable(KEY + "store.2", menu.amount(2)),
                    () -> Component.translatable(KEY + "driver.story")));
            add(new Tally(
                    Sheet.LEFT_X,
                    Sheet.ROW2_Y,
                    Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                    () -> Component.translatable(KEY + "stop." + menu.stop()),
                    () -> Component.translatable(KEY + "driver.story")));
            return;
        }
        for (int i = 0; i < 4; i++)
            add(new Well(
                            Sheet.FIRST_PART_X + i * 32,
                            Sheet.LABEL_Y,
                            Sheet.PARTS_Y,
                            menu.getSlot(i),
                            Component.translatable(KEY + "slot." + i))
                    .story(Component.translatable(KEY + "slot." + i + ".story")));
        mode = button(Sheet.LEFT_X, Sheet.ROW2_Y, Component.translatable(KEY + "toggle"), b -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        });
        for (int i = 0; i < 4; i++) {
            int index = i;
            add(new Tally(
                    Sheet.LEFT_X,
                    Sheet.ROW2_Y + 24 + i * 11,
                    Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                    () -> Component.translatable(KEY + "store." + index, menu.amount(index)),
                    () -> Component.translatable(KEY + "store." + index, menu.amount(index))));
        }
        add(new Tally(
                Sheet.LEFT_X,
                Sheet.ROW2_Y + 70,
                Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                () -> Component.translatable(KEY + "stop." + menu.stop()),
                () -> Component.translatable(KEY + "mode." + (menu.discharge() ? "discharge" : "charge"))));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (mode != null)
            mode.setMessage(Component.translatable(KEY + (menu.discharge() ? "discharging" : "charging")));
    }
}
