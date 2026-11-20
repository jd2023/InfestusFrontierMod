package org.jd.infestusfrontier.vigil;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.vigil.Territory;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

final class VigilNestScreen extends OrganScreen<VigilNestMenu> {
    private static final String KEY = "gui.infestusfrontier.vigil.";
    private Button wake;
    private final List<Button> chunks = new ArrayList<>();

    VigilNestScreen(VigilNestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, VigilNestMenu.STRIPS);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        chunks.clear();
        wake = button(row, Component.translatable(KEY + "toggle"), b -> send(9));
        for (int i = 0; i < 9; i++) {
            int index = i;
            Button cell =
                    button(Sheet.LEFT_X + (i % 3) * 48, Sheet.ROW2_Y + (i / 3) * 16, 44, label(i), b -> send(index));
            cell.setTooltip(Tooltip.create(Component.translatable(KEY + "chunk.story", i % 3 - 1, i / 3 - 1)));
            chunks.add(cell);
        }
        add(new Tally(
                Sheet.LEFT_X,
                Sheet.ROW2_Y + 50,
                Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                () -> Component.translatable(KEY + "territory", Integer.bitCount(menu.mask()), menu.allowance()),
                () -> Component.translatable(
                        KEY + "upkeep", Integer.bitCount(menu.mask()) * Territory.UPKEEP_PER_CHUNK)));
        add(new Tally(
                Sheet.LEFT_X,
                Sheet.ROW2_Y + 72,
                Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                () -> menu.stop() != 0
                        ? Component.translatable(KEY + "stop." + menu.stop())
                        : Component.translatable(
                                KEY + "state." + (menu.enabled() ? "active" : menu.egg() ? "resting" : "egg"),
                                menu.paid() / 20),
                () -> Component.translatable(KEY + "state.story")));
        add(new Tally(
                Sheet.LEFT_X,
                Sheet.ROW2_Y + 61,
                Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                () -> Component.translatable(
                        KEY + "fuel", menu.stored(), Integer.bitCount(menu.mask()) * Territory.UPKEEP_PER_CHUNK),
                () -> Component.translatable(
                        KEY + "upkeep", Integer.bitCount(menu.mask()) * Territory.UPKEEP_PER_CHUNK)));
    }

    private Component label(int index) {
        return Component.translatable(KEY + "chunk." + index, (menu.mask() & (1 << index)) != 0 ? "+" : "−");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (wake != null) wake.setMessage(Component.translatable(KEY + (menu.enabled() ? "rest" : "wake")));
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setMessage(label(i));
            chunks.get(i).active = !menu.enabled() && i != 4;
        }
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
}
