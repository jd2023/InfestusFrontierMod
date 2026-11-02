package org.jd.infestusfrontier.processing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.foundation.screen.Trough;
import org.jd.infestusfrontier.foundation.screen.Unit;
import org.jd.infestusfrontier.foundation.screen.Vein;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.Bounds;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * Client view of the Washing Kidney on a two-strip panel: the Tailings well, the progress vein and the dust well on the
 * row, the washed Tailings and Rock Residue wells on the second row, the clean and the dirty water troughs under them,
 * and the biomass tank it draws on.
 */
final class WashingKidneyScreen extends OrganScreen<BatchOrganMenu> {
    private static final int STRIPS = 2;
    private static final int CLEAN_Y = Sheet.ROW2_Y + Sheet.SLOT.height() + Sheet.GAP;
    private static final int DIRTY_Y = CLEAN_Y + 12;
    private static final Component TAILINGS_IN = Component.translatable("gui.infestusfrontier.washing_kidney.in");
    private static final Component DUST = Component.translatable("gui.infestusfrontier.washing_kidney.dust");
    private static final Component TAILINGS = Component.translatable("gui.infestusfrontier.washing_kidney.tailings");
    private static final Component RESIDUE = Component.translatable("gui.infestusfrontier.washing_kidney.residue");
    private static final Component CLEAN = Component.translatable("gui.infestusfrontier.washing_kidney.clean");
    private static final Component DIRTY = Component.translatable("gui.infestusfrontier.washing_kidney.dirty");

    WashingKidneyScreen(BatchOrganMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, STRIPS);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Emblem(Sheet.KIDNEY_EMBLEM));
        add(new Well(row, menu.getSlot(0), TAILINGS_IN));
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
                        : Component.translatable("gui.infestusfrontier.washing_kidney.waiting"),
                () -> menu.blocked() == 0
                        ? Component.translatable("gui.infestusfrontier.washing_kidney.roomy")
                        : Component.translatable("gui.infestusfrontier.washing_kidney.waiting.story", full())));
        add(new Trough(
                Sheet.FIRST_PART_X,
                CLEAN_Y,
                Sheet.LEFT_X,
                Sheet.RIGHT_EDGE,
                CLEAN,
                menu::water,
                menu::waterCapacity,
                Unit.WATER));
        add(new Trough(
                Sheet.FIRST_PART_X,
                DIRTY_Y,
                Sheet.LEFT_X,
                Sheet.RIGHT_EDGE,
                DIRTY,
                menu::dirty,
                menu::dirtyCapacity,
                Unit.DIRTY_WATER));
    }

    /** The trays and the tank that hold a batch back, named, for the tooltip. */
    private Component full() {
        List<Component> names = new ArrayList<>();
        if ((menu.blocked() & 1) != 0) names.add(DUST);
        if ((menu.blocked() & 2) != 0) names.add(TAILINGS);
        if ((menu.blocked() & 4) != 0) names.add(RESIDUE);
        if ((menu.blocked() & BatchOrganBlockEntity.DIRTY_FULL) != 0) names.add(DIRTY);
        return ComponentUtils.formatList(names, Component.literal(", "));
    }
}
