package org.jd.infestusfrontier.processing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.Emblem;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Vein;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.Sheet;

/** Client view of the Sac: its emblem, the feed slot and the progress vein. */
final class DigestiveSacScreen extends OrganScreen<DigestiveSacMenu> {
    private static final Component FEED = Component.translatable("gui.infestusfrontier.digestive_sac.feed");

    DigestiveSacScreen(DigestiveSacMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Emblem(Sheet.SAC_EMBLEM));
        add(new Well(row, menu.getSlot(DigestiveSacMenu.FEED_SLOT), FEED));
        add(new Vein(row, menu::percent));
    }
}
