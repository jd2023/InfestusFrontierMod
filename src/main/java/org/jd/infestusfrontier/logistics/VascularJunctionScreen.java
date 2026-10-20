package org.jd.infestusfrontier.logistics;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Tally;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * Client view of the junction on a two-strip panel: one line per face naming its port, then the tank as a line,
 * biomass or water. The probe changes the ports; the screen only reads.
 */
final class VascularJunctionScreen extends OrganScreen<VascularJunctionMenu> {
    private static final int STRIPS = 2;
    private static final int LINE = 10;
    private static final Component PORTS_STORY = Component.translatable("gui.infestusfrontier.vascular_junction.ports");
    private static final Component TANK_STORY = Component.translatable("gui.infestusfrontier.vascular_junction.tank");

    VascularJunctionScreen(VascularJunctionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, STRIPS);
    }

    @Override
    protected boolean showsBiomass() {
        return false;
    }

    @Override
    protected void addParts(PartRow row) {
        int y = Sheet.LABEL_Y;
        int width = Sheet.RIGHT_EDGE - Sheet.FIRST_PART_X;
        for (Direction side : Direction.values()) {
            add(new Tally(
                    Sheet.FIRST_PART_X,
                    y,
                    width,
                    () -> Component.translatable(
                            "infestusfrontier.status.port",
                            Component.translatable("infestusfrontier.side." + side.getSerializedName()),
                            Component.translatable("infestusfrontier.port." + menu.port(side))),
                    () -> PORTS_STORY));
            y += LINE;
        }
        add(new Tally(
                Sheet.FIRST_PART_X,
                y + Sheet.GAP,
                width,
                () -> Component.translatable(
                        menu.holdsBiomass() ? "gui.infestusfrontier.biomass" : "gui.infestusfrontier.water",
                        menu.stored(),
                        menu.capacity()),
                () -> TANK_STORY));
    }
}
