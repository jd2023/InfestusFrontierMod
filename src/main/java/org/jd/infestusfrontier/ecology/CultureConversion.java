package org.jd.infestusfrontier.ecology;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.foundation.TickQuota;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;

final class CultureConversion {
    static final int CONVERSIONS_PER_SERVER_TICK = 16;

    private final Supplier<LivingSubstrateBlock> substrate;
    private final TickQuota quota;
    private final DiscoveryObserver discovery;

    CultureConversion(Supplier<LivingSubstrateBlock> substrate, DiscoveryObserver discovery) {
        this(substrate, new TickQuota(CONVERSIONS_PER_SERVER_TICK), discovery);
    }

    CultureConversion(Supplier<LivingSubstrateBlock> substrate, TickQuota quota, DiscoveryObserver discovery) {
        this.substrate = substrate;
        this.quota = quota;
        this.discovery = discovery;
    }

    InteractionResult apply(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        ItemStack culture = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null || culture.isEmpty()) return InteractionResult.FAIL;
        var target = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (target == null) return InteractionResult.FAIL;
        BlockState original = target.getBlockState(pos);
        if (!eligible(original) || !SelectedGroundVisibility.visible(context)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerLevel serverLevel = (ServerLevel) level;
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, culture)) {
            return InteractionResult.FAIL;
        }

        SubstrateOwnership ownership = SubstrateOwnership.get(serverLevel);
        if (!ownership.canClaim(pos) || !quota.take(serverLevel.getServer().getTickCount())) {
            return InteractionResult.FAIL;
        }

        BlockState replacement = substrate.get().defaultBlockState();
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        if (!level.setBlock(pos, replacement, flags, 0)) return InteractionResult.FAIL;
        if (!ownership.claim(pos, player.getUUID())) {
            level.setBlock(pos, original, flags, 0);
            return InteractionResult.FAIL;
        }
        culture.consume(1, player);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            discovery.complete(serverPlayer, DiscoveryObserver.Milestone.LIVING_SUBSTRATE);
        }
        return InteractionResult.CONSUME;
    }

    private boolean eligible(BlockState state) {
        return !state.is(substrate.get()) && state.is(EcologyTags.CULTURE_ELIGIBLE);
    }

}
