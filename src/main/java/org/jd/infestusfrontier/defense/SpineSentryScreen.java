package org.jd.infestusfrontier.defense;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.defense.Sentry;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/** Client view of the sentry: its store as two rows of wells under one label, and the biomass tank. */
final class SpineSentryScreen extends OrganScreen<SpineSentryMenu> {
    SpineSentryScreen(SpineSentryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 1);
    }

    @Override
    protected void addParts(PartRow row) {
        Component label = Component.translatable("gui.infestusfrontier.spine_sentry.kept");
        for (int slot = 0; slot < Sentry.STORE_SLOTS; slot++) {
            if (slot < SpineSentryMenu.ROW)
                add(new Well(row, menu.getSlot(slot), slot == 0 ? label : Component.empty()));
            else {
                int x = Sheet.FIRST_PART_X + (slot % SpineSentryMenu.ROW) * (Sheet.SLOT.width() + Sheet.GAP);
                add(new Well(x, Sheet.ROW2_LABEL_Y, Sheet.ROW2_Y, menu.getSlot(slot), Component.empty()));
            }
        }
    }
}
