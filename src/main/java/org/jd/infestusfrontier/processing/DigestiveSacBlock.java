package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassBuckets;
import org.jetbrains.annotations.Nullable;

/**
 * Turns one hand-fed item at a time into biomass. Feed items start a batch, buckets take biomass out, empty hands
 * report the state, anything else passes through so the player can build against it.
 */
public final class DigestiveSacBlock extends BaseEntityBlock {
    private static final MapCodec<DigestiveSacBlock> CODEC = simpleCodec(DigestiveSacBlock::new);

    DigestiveSacBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<DigestiveSacBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DigestiveSacBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(
                type, ProcessingModule.DIGESTIVE_SAC_ENTITY.get(), DigestiveSacBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (BiomassBuckets.isBucket(stack))
            return BiomassBuckets.useOn(stack, level, pos, hit.getDirection(), player, hand);
        if (!(level.getBlockEntity(pos) instanceof DigestiveSacBlockEntity sac))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return sac.feed(player, hand, stack);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof DigestiveSacBlockEntity sac) sac.report(player);
        return InteractionResult.CONSUME;
    }
}
