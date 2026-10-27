package org.jd.infestusfrontier.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.storage.api.CapsuleStock;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/** One pod on the plain panel: its compartment well, the treatment well, and its grade and fill on the strip below. */
final class ItemCapsulePodScreen extends OrganScreen<ItemCapsulePodMenu> {
    private static final String KEY = "gui.infestusfrontier.item_capsule.pod.";
    private static final int LINE = 9;

    ItemCapsulePodScreen(ItemCapsulePodMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ItemCapsulePodMenu.STRIPS);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Well(row, menu.getSlot(ItemCapsulePodMenu.COMPARTMENT), Component.translatable(KEY + "holds")));
        row.place(ItemCapsulePodMenu.SPACER); // room for the label over the compartment
        add(new Well(row, menu.getSlot(ItemCapsulePodMenu.TREATMENT), Component.translatable(KEY + "treatment")));
        int width = Sheet.RIGHT_EDGE - Sheet.LEFT_X;
        add(new Tally(
                Sheet.LEFT_X,
                Sheet.ROW2_Y,
                width,
                () -> Component.translatable(KEY + "grade", menu.grade() + 1, CapsuleStock.stacks(menu.grade())),
                () -> Component.translatable(KEY + "treatment.story")));
        add(new Tally(
                Sheet.LEFT_X,
                Sheet.ROW2_Y + LINE + 1,
                width,
                () -> Component.translatable(KEY + "fill", menu.count(), menu.limit()),
                () -> Component.translatable(KEY + "fill.story")));
    }
}
