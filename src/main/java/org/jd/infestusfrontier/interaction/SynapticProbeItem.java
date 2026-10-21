package org.jd.infestusfrontier.interaction;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;
import org.jd.infestusfrontier.logistics.api.PortSwitch;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

/**
 * Reads the biomass of the organ it is used on without opening its screen (it is an organ reader, tag
 * organ_readers). On a block with ports it cycles the port of the face it clicks instead; on a Mouth, or on the store
 * standing on one, it opens the Mouth's screen. Sneaking always reads.
 */
final class SynapticProbeItem extends Item {
    SynapticProbeItem(Properties properties) {
        super(properties);
    }

    /** Runs before the clicked block's own use, so a chest on a Mouth does not open instead. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = context.getLevel();
        // a block with ports keeps the probe for its ports, even when it stands on a Mouth
        if (level.getCapability(LogisticsCapabilities.PORT_SWITCH, context.getClickedPos(), context.getClickedFace())
                != null) return InteractionResult.PASS;
        for (BlockPos at :
                List.of(context.getClickedPos(), context.getClickedPos().below())) {
            MenuProvider mouth = level.getCapability(LogisticsCapabilities.MOUTH_SCREEN, at, null);
            if (mouth == null) continue;
            if (player instanceof ServerPlayer server) server.openMenu(mouth, at);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        var pos = context.getClickedPos();
        PortSwitch ports = level.getCapability(LogisticsCapabilities.PORT_SWITCH, pos, context.getClickedFace());
        if (ports != null && player != null && !player.isShiftKeyDown()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            player.displayClientMessage(ports.cycle(context.getClickedFace()), true);
            return InteractionResult.CONSUME;
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
