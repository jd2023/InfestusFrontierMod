package org.jd.infestusfrontier.cultivation;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;

/** Client view of a plot organ on the plain panel: its wells under one label and the biomass tank. */
final class PlotOrganScreen extends OrganScreen<PlotOrganMenu> {
    private final Component label;

    private PlotOrganScreen(PlotOrganMenu menu, Inventory inventory, Component title, Component label) {
        super(menu, inventory, title);
        this.label = label;
    }

    static PlotOrganScreen planter(PlotOrganMenu menu, Inventory inventory, Component title) {
        return new PlotOrganScreen(
                menu, inventory, title, Component.translatable("gui.infestusfrontier.planting_proboscis.seeds"));
    }

    static PlotOrganScreen harvester(PlotOrganMenu menu, Inventory inventory, Component title) {
        return new PlotOrganScreen(
                menu, inventory, title, Component.translatable("gui.infestusfrontier.harvest_corolla.harvest"));
    }

    @Override
    protected void addParts(PartRow row) {
        for (int slot = 0; slot < menu.slotCount(); slot++)
            add(new Well(row, menu.getSlot(slot), slot == 0 ? label : Component.empty()));
    }
}
