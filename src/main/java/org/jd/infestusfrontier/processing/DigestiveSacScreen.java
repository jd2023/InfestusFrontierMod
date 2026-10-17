package org.jd.infestusfrontier.processing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jd.infestusfrontier.foundation.screen.OrganScreen;
import org.jd.infestusfrontier.foundation.screen.Status;
import org.jd.infestusfrontier.foundation.screen.Vein;
import org.jd.infestusfrontier.foundation.screen.Well;
import org.jd.infestusfrontier.ui.PartRow;
import org.jd.infestusfrontier.ui.StatusKind;

/** Client view of the Sac: what it is doing, the feed slot and the progress vein. */
final class DigestiveSacScreen extends OrganScreen<DigestiveSacMenu> {
    private static final Component FEED = Component.translatable("gui.infestusfrontier.digestive_sac.feed");
    /** By {@link DigestiveSacBlockEntity.Status} ordinal. */
    private static final Component[] WORDS = {
        Component.translatable("gui.infestusfrontier.digestive_sac.idle"),
        Component.translatable("gui.infestusfrontier.digestive_sac.working"),
        Component.translatable("gui.infestusfrontier.digestive_sac.paused"),
        Component.translatable("gui.infestusfrontier.digestive_sac.full"),
        Component.translatable("gui.infestusfrontier.digestive_sac.unreadable"),
    };

    private static final StatusKind[] KIND = {
        StatusKind.IDLE, StatusKind.ACTIVE, StatusKind.BLOCKED, StatusKind.BLOCKED, StatusKind.BROKEN
    };

    DigestiveSacScreen(DigestiveSacMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected Status status() {
        int state = menu.status().ordinal();
        return new Status(KIND[state], WORDS[state]);
    }

    @Override
    protected void addParts(PartRow row) {
        add(new Well(row, menu.getSlot(DigestiveSacMenu.FEED_SLOT), FEED));
        add(new Vein(row, menu::percent));
    }
}
