package org.jd.infestusfrontier.processing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Vein;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * Client view of the Loom on a one-strip panel: the bone well, the progress vein and the output well on the row, and
 * the biomass tank it draws on.
 */
final class BoneLoomScreen extends OrganScreen<BatchOrganMenu> {
    private static final int STRIPS = 1;
    private static final Component BONES = Component.translatable("gui.infestusfrontier.bone_loom.bones");
    private static final Component OUT = Component.translatable("gui.infestusfrontier.batch_organ.out");

    BoneLoomScreen(BatchOrganMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, STRIPS);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Emblem(Sheet.LOOM_EMBLEM));
        add(new Well(row, menu.getSlot(0), BONES));
        add(new Vein(row, menu::percent));
        add(new Well(row, menu.getSlot(menu.outputSlot()), OUT));
    }
}
