package org.jd.infestusfrontier.discovery;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;

final class AdvancementDiscovery implements DiscoveryObserver {
    @Override
    public void complete(ServerPlayer player, Milestone milestone) {
        var id = ResourceLocation.fromNamespaceAndPath("infestusfrontier", "discovery/" + milestone.path());
        var advancement = player.getServer().getAdvancements().get(id);
        if (advancement == null) {
            LogUtils.getLogger().error("Missing discovery advancement {}", id);
            return;
        }
        player.getAdvancements().award(advancement, "observed");
    }
}
