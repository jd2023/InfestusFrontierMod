package org.jd.infestusfrontier.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Scrollbar;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Wells;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;
import org.jetbrains.annotations.Nullable;

/**
 * The whole tree's ledger, reached from any stem cell: three rows of kinds with a scrollbar beside them, the pods
 * line and the growth line under them. The wheel over the view and the bar scrolls; the menu decides the row.
 */
final class ItemCapsuleScreen extends OrganScreen<ItemCapsuleMenu> {
    private static final String KEY = "gui.infestusfrontier.item_capsule.";
    private Scrollbar scrollbar;

    ItemCapsuleScreen(ItemCapsuleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ItemCapsuleMenu.STRIPS);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        PartRow ledger = new PartRow(Sheet.LEFT_X, Sheet.GAP, Sheet.RIGHT_EDGE);
        add(new Wells(
                ledger,
                ItemCapsuleMenu.GRID,
                menu.slots.subList(0, ItemCapsuleMenu.VIEW),
                Component.translatable(KEY + "held")));
        scrollbar = add(new Scrollbar(
                ledger.place(Sheet.SCROLL_TRACK.width()),
                ItemCapsuleMenu.GRID_Y,
                menu::shownRow,
                menu::lastRow,
                this::scrollTo,
                () -> Component.translatable(KEY + "held.story")));
        add(new Tally(
                Sheet.LEFT_X,
                ItemCapsuleMenu.BODY_Y,
                Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                () -> Component.translatable(KEY + "body", menu.compartments(), menu.used()),
                () -> Component.translatable(KEY + "body.story")));
        add(new Tally(
                Sheet.LEFT_X,
                ItemCapsuleMenu.GROWTH_Y,
                Sheet.RIGHT_EDGE - Sheet.LEFT_X,
                () -> menu.cost() > 0
                        ? Component.translatable(KEY + "growth", menu.fed(), menu.cost())
                        : Component.translatable(KEY + "grown"),
                () -> Component.translatable(KEY + "growth.story")));
    }

    /** Asks the server for a view from {@code row}; the menu keeps the row asked for until the server shows it. */
    private void scrollTo(int row) {
        if (menu.scrollTo(row) && minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, menu.shownRow());
    }

    /** A click on a cell is neither predicted nor sent while the server has yet to show the row asked for. */
    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot != null && slot.index < ItemCapsuleMenu.VIEW && menu.pending()) return;
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    /** The wheel over the view or the bar moves the view a row. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Bounds view = ItemCapsuleMenu.GRID.bounds();
        if (scrollY != 0
                && isHovering(view.x(), view.y(), Sheet.RIGHT_EDGE - view.x(), view.height(), mouseX, mouseY)) {
            scrollbar.scrollBy(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** A container screen keeps drags from its widgets; the thumb gets its own. */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbar.dragging()) return scrollbar.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
