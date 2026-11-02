package org.jd.infestusfrontier.storage;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.storage.PodKind;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/** One pod on the plain panel: its compartment wells, the treatment well, and its grade and fill on the strip below. */
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
        boolean craw = menu.kind() == PodKind.CRAW;
        for (int compartment = 0; compartment < menu.compartments(); compartment++)
            add(new Well(
                    row,
                    menu.getSlot(ItemCapsulePodMenu.COMPARTMENT + compartment),
                    craw
                            ? Component.literal(Integer.toString(compartment + 1))
                            : Component.translatable(KEY + "holds")));
        row.place(ItemCapsulePodMenu.spacer(menu.compartments())); // room for the label over the compartment
        add(new Well(
                row,
                menu.getSlot(ItemCapsulePodMenu.TREATMENT),
                Component.translatable(KEY + (craw ? "treatment.short" : "treatment"))));
        int width = Sheet.RIGHT_EDGE - Sheet.LEFT_X;
        add(new Tally(
                Sheet.LEFT_X,
                Sheet.ROW2_Y,
                width,
                () -> Component.translatable(
                        KEY + (craw ? "craw.grade" : "grade"),
                        menu.grade() + 1,
                        menu.kind().stacks(menu.grade())),
                () -> Component.translatable(KEY + (craw ? "craw.treatment.story" : "treatment.story"))));
        add(new Tally(
                Sheet.LEFT_X,
                Sheet.ROW2_Y + LINE + 1,
                width,
                () -> craw
                        ? Component.translatable(KEY + "craw.fill", menu.used(), menu.compartments(), menu.count())
                        : Component.translatable(KEY + "fill", menu.count(0), menu.limit(0)),
                () -> Component.translatable(KEY + (craw ? "craw.fill.story" : "fill.story"))));
    }
}
