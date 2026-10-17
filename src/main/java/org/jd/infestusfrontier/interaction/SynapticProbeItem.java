package org.jd.infestusfrontier.interaction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

/** Opens the screen of the organ it is used on, or reports the organ's biomass when the organ has no screen. */
final class SynapticProbeItem extends Item {
    SynapticProbeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        var pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            if (!level.isClientSide && player instanceof ServerPlayer server) server.openMenu(provider, pos);
            return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        BiomassHandler handler = level.getCapability(BiomassCapabilities.BLOCK, pos, null);
        if (handler == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player != null) {
            var tank = handler.tank();
            player.displayClientMessage(
                    Component.translatable(
                            "infestusfrontier.status.probe.biomass",
                            tank.stored().units(),
                            tank.capacity().units()),
                    true);
        }
        return InteractionResult.CONSUME;
    }
}
