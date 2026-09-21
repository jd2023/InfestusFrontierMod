package org.jd.infestusfrontier.discovery;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;

/** Owns bounded claim delivery and server-authored guide progression. */
public final class DiscoveryModule {
    private static final ResourceLocation CULTURE = id("construction/spore_culture");
    private static final ResourceLocation BUD = id("construction/organ_bud");
    private final DiscoveryObserver observer = new AdvancementDiscovery();
    private final FirstJoinClaim claim = new FirstJoinClaim(observer);

    public void register() {
        NeoForge.EVENT_BUS.addListener(this::playerJoined);
        NeoForge.EVENT_BUS.addListener(this::playerTicked);
        NeoForge.EVENT_BUS.addListener(this::playerCloned);
        NeoForge.EVENT_BUS.addListener(this::itemCrafted);
    }

    public DiscoveryObserver observer() {
        return observer;
    }

    private void playerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) claim.join(player);
    }

    private void playerTicked(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) claim.deliver(player);
    }

    private void playerCloned(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer replacement) {
            claim.cloneState(original, replacement);
        }
    }

    private void itemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (GuideAccess.isWakingGenome(event.getCrafting())) {
            observer.complete(player, DiscoveryObserver.Milestone.FIRST_COPY);
        }
        var id = BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem());
        if (CULTURE.equals(id)) observer.complete(player, DiscoveryObserver.Milestone.SPORE_CULTURE);
        if (BUD.equals(id)) observer.complete(player, DiscoveryObserver.Milestone.ORGAN_BUD);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
    }
}
