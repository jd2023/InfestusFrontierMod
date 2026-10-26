package org.jd.infestusfrontier.storage;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/** A page of the whole tree's store, reached from any cell or pod, with fixed wells and ordinary inventory controls. */
final class ItemCapsuleScreen extends OrganScreen<ItemCapsuleMenu> {
    private static final String KEY = "gui.infestusfrontier.item_capsule.";
    private final List<Well> wells = new ArrayList<>();
    private Button previous;
    private Button next;

    ItemCapsuleScreen(ItemCapsuleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ItemCapsuleMenu.STRIPS);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        wells.clear();
        add(new Tally(
                Sheet.LEFT_X,
                Sheet.LABEL_Y - 3,
                Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                () -> Component.translatable(KEY + "body", menu.compartments(), menu.used()),
                () -> Component.translatable(KEY + "body.story")));
        for (int slot = 0; slot < ItemCapsuleMenu.PAGE_SIZE; slot++) {
            int y = ItemCapsuleMenu.GRID_Y + slot / ItemCapsuleMenu.COLUMNS * ItemCapsuleMenu.PITCH;
            wells.add(add(new Well(
                    Sheet.LEFT_X + slot % ItemCapsuleMenu.COLUMNS * ItemCapsuleMenu.PITCH,
                    y,
                    y,
                    menu.getSlot(slot),
                    Component.empty())));
        }
        previous = button(
                Sheet.LEFT_X,
                ItemCapsuleMenu.BUTTON_Y,
                Component.translatable(KEY + "previous"),
                b -> send(ItemCapsuleMenu.PREVIOUS));
        next = button(
                Sheet.RIGHT_EDGE - Sheet.BUTTON.width(),
                ItemCapsuleMenu.BUTTON_Y,
                Component.translatable(KEY + "next"),
                b -> send(ItemCapsuleMenu.NEXT));
        previous.setTooltip(Tooltip.create(Component.translatable(KEY + "page.story")));
        next.setTooltip(Tooltip.create(Component.translatable(KEY + "page.story")));
        add(new Tally(
                66,
                ItemCapsuleMenu.BUTTON_Y + 4,
                44,
                () -> Component.translatable(KEY + "page", menu.page() + 1, menu.pages()),
                () -> Component.translatable(KEY + "page.story")));
        updateControls();
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateControls();
    }

    private void updateControls() {
        previous.active = menu.page() > 0 && menu.getCarried().isEmpty();
        next.active = menu.page() + 1 < menu.pages() && menu.getCarried().isEmpty();
        for (int slot = 0; slot < wells.size(); slot++)
            wells.get(slot).visible = menu.getSlot(slot).isActive();
    }
}
