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
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.foundation.TickQuota;

final class CultureConversion {
    static final int CONVERSIONS_PER_SERVER_TICK = 16;

    private final Supplier<LivingSubstrateBlock> substrate;
    private final TickQuota quota;

    CultureConversion(Supplier<LivingSubstrateBlock> substrate) {
        this(substrate, new TickQuota(CONVERSIONS_PER_SERVER_TICK));
    }

    CultureConversion(Supplier<LivingSubstrateBlock> substrate, TickQuota quota) {
        this.substrate = substrate;
        this.quota = quota;
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
        if (!eligible(original) || !visible(level, pos, face)) {
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
        return InteractionResult.CONSUME;
    }

    private boolean eligible(BlockState state) {
        return !state.is(substrate.get()) && state.is(EcologyTags.CULTURE_ELIGIBLE);
    }

    private static boolean visible(Level level, BlockPos pos, Direction face) {
        BlockPos neighborPos = pos.relative(face);
        var neighborChunk = level.getChunkSource().getChunkNow(neighborPos.getX() >> 4, neighborPos.getZ() >> 4);
        if (neighborChunk == null) return false;
        BlockState neighbor = neighborChunk.getBlockState(neighborPos);
        return !neighbor.isFaceSturdy(level, neighborPos, face.getOpposite(), SupportType.FULL);
    }
}
