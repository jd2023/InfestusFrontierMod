package org.jd.infestusfrontier.processing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Vein;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * Client view of the Mineral Gizzard on a one-strip panel: the raw-item well, the progress vein and the dust well on
 * the row, the Tailings and Rock Residue wells on the second row, and the biomass tank it draws on.
 */
final class MineralGizzardScreen extends OrganScreen<BatchOrganMenu> {
    private static final int STRIPS = 1;
    private static final Component RAW = Component.translatable("gui.infestusfrontier.mineral_gizzard.raw");
    private static final Component DUST = Component.translatable("gui.infestusfrontier.mineral_gizzard.dust");
    private static final Component TAILINGS = Component.translatable("gui.infestusfrontier.mineral_gizzard.tailings");
    private static final Component RESIDUE = Component.translatable("gui.infestusfrontier.mineral_gizzard.residue");

    MineralGizzardScreen(BatchOrganMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, STRIPS);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Emblem(Sheet.GIZZARD_EMBLEM));
        add(new Well(row, menu.getSlot(0), RAW));
        add(new Vein(row, menu::percent));
        add(new Well(row, menu.getSlot(menu.outputSlot()), DUST));
        Bounds tailings = menu.outputWell(1);
        add(new Well(tailings.x(), Sheet.ROW2_LABEL_Y, tailings.y(), menu.getSlot(menu.outputSlot() + 1), TAILINGS));
        Bounds residue = menu.outputWell(2);
        add(new Well(residue.x(), Sheet.ROW2_LABEL_Y, residue.y(), menu.getSlot(menu.outputSlot() + 2), RESIDUE));
        int x = residue.x() + Sheet.SLOT.width() + Sheet.GAP;
        add(new Tally(
                x,
                residue.y() + (Sheet.SLOT.height() - 9) / 2,
                Sheet.PARTS_END - x,
                () -> menu.blocked() == 0
                        ? Component.empty()
                        : Component.translatable("gui.infestusfrontier.mineral_gizzard.waiting"),
                () -> menu.blocked() == 0
                        ? Component.translatable("gui.infestusfrontier.mineral_gizzard.roomy")
                        : Component.translatable("gui.infestusfrontier.mineral_gizzard.waiting.story", full())));
    }

    /** The trays that are full, named, for the tooltip. */
    private Component full() {
        List<Component> names = new ArrayList<>();
        if ((menu.blocked() & 1) != 0) names.add(DUST);
        if ((menu.blocked() & 2) != 0) names.add(TAILINGS);
        if ((menu.blocked() & 4) != 0) names.add(RESIDUE);
        return ComponentUtils.formatList(names, Component.literal(", "));
    }
}
