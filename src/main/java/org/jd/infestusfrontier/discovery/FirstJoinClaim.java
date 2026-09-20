package org.jd.infestusfrontier.discovery;

import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;

/** Persists one tri-state Boolean field: absent is new, true is pending, false is delivered. */
final class FirstJoinClaim {
    static final String PENDING = "infestusfrontier.discovery.guide_pending";
    private final DiscoveryObserver discovery;

    FirstJoinClaim(DiscoveryObserver discovery) {
        this.discovery = discovery;
    }

    void join(ServerPlayer player) {
        discovery.complete(player, DiscoveryObserver.Milestone.CONTACT);
        var data = player.getPersistentData();
        if (!data.contains(PENDING, Tag.TAG_BYTE)) data.putBoolean(PENDING, true);
        deliver(player);
    }

    void deliver(ServerPlayer player) {
        var data = player.getPersistentData();
        if (!data.contains(PENDING, Tag.TAG_BYTE) || !data.getBoolean(PENDING)) return;
        var guide = GuideAccess.wakingGenome();
        // Inventory.add discards overflow in Creative; claim delivery requires a real slot.
        int slot = player.getInventory().getFreeSlot();
        if (slot < 0) return;
        player.getInventory().setItem(slot, guide);
        data.putBoolean(PENDING, false);
        discovery.complete(player, DiscoveryObserver.Milestone.FIRST_COPY);
    }

    void cloneState(ServerPlayer original, ServerPlayer replacement) {
        var source = original.getPersistentData();
        if (source.contains(PENDING, Tag.TAG_BYTE)) {
            replacement.getPersistentData().putBoolean(PENDING, source.getBoolean(PENDING));
        }
    }
}
