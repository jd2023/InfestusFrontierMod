package org.jd.infestusfrontier.storage;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Status;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.StatusKind;

/** Client view of the Bladder: empty, holding or full, the service slot and the Fill button. */
final class BiomassBladderScreen extends OrganScreen<BiomassBladderMenu> {
    private static final Component PIECE = Component.translatable("gui.infestusfrontier.biomass_bladder.piece");
    private static final Component FILL = Component.translatable("gui.infestusfrontier.biomass_bladder.fill");
    private static final Component EMPTY = Component.translatable("gui.infestusfrontier.biomass_bladder.empty");
    private static final Component HOLDING = Component.translatable("gui.infestusfrontier.biomass_bladder.holding");
    private static final Component FULL = Component.translatable("gui.infestusfrontier.biomass_bladder.full");
    private static final Component NOTHING_TO_FILL = Refusals.text(new Refusal("equipment.nothing_to_fill"));
    private Button fill;

    BiomassBladderScreen(BiomassBladderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected Status status() {
        if (menu.stored() == 0) return new Status(StatusKind.IDLE, EMPTY);
        if (menu.stored() >= menu.capacity()) return new Status(StatusKind.BLOCKED, FULL);
        return new Status(StatusKind.ACTIVE, HOLDING);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Well(row, menu.getSlot(BiomassBladderMenu.SERVICE_SLOT), PIECE));
        fill = button(
                row,
                FILL,
                button -> minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, BiomassBladderMenu.FILL_BUTTON));
        fill.setTooltip(Tooltip.create(NOTHING_TO_FILL));
    }

    @Override
    protected void containerTick() {
        fill.active = menu.hasServiceItem();
    }
}
