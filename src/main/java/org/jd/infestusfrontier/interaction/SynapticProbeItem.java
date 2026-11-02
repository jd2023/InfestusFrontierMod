package org.jd.infestusfrontier.interaction;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;
import org.jd.infestusfrontier.logistics.api.PortSwitch;
import org.jd.infestusfrontier.processing.api.PalmLink;
import org.jd.infestusfrontier.processing.api.PalmLinking;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

/**
 * Reads the biomass of the organ it is used on without opening its screen (it is an organ reader, tag
 * organ_readers). On a block with ports it cycles the port of the face it clicks instead; on a Mouth, or on the store
 * standing on one, it opens the Mouth's screen. Used on a Shaping Palm that has Reach it picks that Palm; used next on
 * a block it asks the Palm to link to it, and a use on the air lets it go. Sneaking always reads.
 */
final class SynapticProbeItem extends Item {
    /** The picked Palm and the block offered are in different dimensions. */
    private static final Refusal OTHER_DIMENSION = new Refusal("processing.palm_link_dimension");

    SynapticProbeItem(Properties properties) {
        super(properties);
    }

    /** Runs before the clicked block's own use, so a chest on a Mouth does not open instead. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = context.getLevel();
        InteractionResult linked = linkPalm(stack, level, context.getClickedPos(), player);
        if (linked != InteractionResult.PASS) return linked;
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

    /**
     * The two clicks of a supply link. A Palm with Reach is picked; any other block, while a Palm is picked, is the
     * store offered to it. A refusal says why and keeps the pick, so the next block may be tried; a Palm that is gone
     * lets the pick go.
     */
    private static InteractionResult linkPalm(ItemStack stack, Level level, BlockPos clicked, Player player) {
        PalmLink palm = level.getCapability(PalmLinking.PALM_LINK, clicked, null);
        GlobalPos source = stack.get(InteractionModule.PALM_SOURCE.get());
        if (palm == null && source == null) return InteractionResult.PASS;
        if (palm != null && !palm.hasReach()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (palm != null) {
            stack.set(InteractionModule.PALM_SOURCE.get(), GlobalPos.of(level.dimension(), clicked));
            player.displayClientMessage(Component.translatable("infestusfrontier.status.palm.picked"), true);
            return InteractionResult.CONSUME;
        }
        Optional<Refusal> why = source.dimension().equals(level.dimension())
                ? PalmLinking.link(level, source.pos(), clicked)
                : Optional.of(OTHER_DIMENSION);
        if (why.isEmpty()) {
            stack.remove(InteractionModule.PALM_SOURCE.get());
            player.displayClientMessage(Component.translatable("infestusfrontier.status.palm.linked"), true);
            return InteractionResult.CONSUME;
        }
        if (why.get().equals(PalmLinking.GONE)) stack.remove(InteractionModule.PALM_SOURCE.get());
        Refusals.show(player, why.get());
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        GlobalPos source = stack.get(InteractionModule.PALM_SOURCE.get());
        if (source == null) return;
        BlockPos at = source.pos();
        lines.add(
                Component.translatable("item.infestusfrontier.synaptic_probe.picked", at.getX(), at.getY(), at.getZ()));
    }

    /** A use on the air lets a picked Palm go. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.has(InteractionModule.PALM_SOURCE.get())) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            stack.remove(InteractionModule.PALM_SOURCE.get());
            player.displayClientMessage(Component.translatable("infestusfrontier.status.palm.let_go"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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
        var network = level.getCapability(LogisticsCapabilities.NETWORK_STATUS, pos, context.getClickedFace());
        if (network != null) {
            if (!level.isClientSide && player != null)
                player.displayClientMessage(
                        Component.translatable(
                                "infestusfrontier.status.network",
                                Component.translatable("infestusfrontier.status.network." + network.state()),
                                network.cells(),
                                network.limit()),
                        true);
            return InteractionResult.sidedSuccess(level.isClientSide);
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
