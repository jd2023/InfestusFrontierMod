package org.jd.infestusfrontier.equipment;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/** Which worn pieces the papilla fills and how full: one button per piece and one for the limit. */
final class FuelPapillaScreen extends OrganScreen<FuelPapillaMenu> {
    private static final String KEY = "gui.infestusfrontier.fuel_papilla.";
    private static final String[] PIECES = {"head", "body", "legs", "feet"};
    private static final int PITCH = 18;

    private final List<Button> pieces = new ArrayList<>();
    private Button limit;

    FuelPapillaScreen(FuelPapillaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, FuelPapillaMenu.STRIPS);
    }

    @Override
    protected void addParts(PartRow row) {
        pieces.clear();
        int y = Sheet.partY(Sheet.BUTTON.height());
        for (int index = 0; index < PIECES.length; index++) {
            int id = index;
            Button button = button(Sheet.FIRST_PART_X, y + index * PITCH, Component.empty(), b -> send(id));
            button.setTooltip(Tooltip.create(Component.translatable(KEY + "piece.story")));
            pieces.add(button);
        }
        limit = button(
                Sheet.FIRST_PART_X,
                y + PIECES.length * PITCH,
                Component.empty(),
                b -> send(FuelPapillaMenu.LIMIT_BUTTON));
        limit.setTooltip(Tooltip.create(Component.translatable(KEY + "limit.story")));
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

    private void label() {
        for (int index = 0; index < pieces.size(); index++)
            pieces.get(index)
                    .setMessage(Component.translatable(KEY + PIECES[index] + (menu.fills(index) ? ".on" : ".off")));
        limit.setMessage(Component.translatable(KEY + "limit", menu.limit()));
    }
}
