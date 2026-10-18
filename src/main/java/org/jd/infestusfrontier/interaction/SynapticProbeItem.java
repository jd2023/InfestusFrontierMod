package org.jd.infestusfrontier.interaction;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

/** Reads the biomass of the organ it is used on without opening its screen (it is an organ reader, tag organ_readers). */
final class SynapticProbeItem extends Item {
    SynapticProbeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        var pos = context.getClickedPos();
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
