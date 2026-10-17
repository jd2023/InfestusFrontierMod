package org.jd.infestusfrontier.processing;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.OrganScreen;
import org.jd.infestusfrontier.ui.Meter;
import org.jd.infestusfrontier.ui.Sheet;

/** Client view of the Sac: what it is doing, the feed slot, the progress vein with its percent, and the tank. */
final class DigestiveSacScreen extends OrganScreen<DigestiveSacMenu> {
    private static final Component FEED = Component.translatable("gui.infestusfrontier.digestive_sac.feed");
    private static final Component[] STATUS = {
        Component.translatable("gui.infestusfrontier.digestive_sac.idle"),
        Component.translatable("gui.infestusfrontier.digestive_sac.working"),
        Component.translatable("gui.infestusfrontier.digestive_sac.paused"),
        Component.translatable("gui.infestusfrontier.digestive_sac.full"),
        Component.translatable("gui.infestusfrontier.digestive_sac.unreadable"),
    };
    private static final int[] GLYPH = {Sheet.RING, Sheet.ARROW, Sheet.BARS, Sheet.CROSS, Sheet.CROSS};
    private static final int VEIN_X = DigestiveSacMenu.SLOT_X + 22;
    private static final int VEIN_Y = Sheet.PARTS_Y + 2;

    DigestiveSacScreen(DigestiveSacMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected int statusGlyph() {
        return GLYPH[menu.status().ordinal()];
    }

    @Override
    protected Component statusText() {
        return STATUS[menu.status().ordinal()];
    }

    @Override
    protected Meter biomass() {
        return new Meter(Math.min(menu.stored(), menu.capacity()), menu.capacity());
    }

    @Override
    protected Component biomassShort() {
        return Component.translatable("gui.infestusfrontier.biomass_short", menu.stored());
    }

    @Override
    protected Component biomassLong() {
        return Component.translatable("gui.infestusfrontier.biomass", menu.stored(), menu.capacity());
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY) {
        well(graphics, DigestiveSacMenu.SLOT_X, DigestiveSacMenu.SLOT_Y);
        vein(graphics, VEIN_X, VEIN_Y, new Meter(menu.percent(), 100));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        slotLabel(graphics, FEED, DigestiveSacMenu.SLOT_X);
        centered(
                graphics,
                Component.translatable("gui.infestusfrontier.percent", menu.percent()),
                VEIN_X + Sheet.VEIN_EMPTY.width() / 2,
                Sheet.LABEL_Y);
    }
}
