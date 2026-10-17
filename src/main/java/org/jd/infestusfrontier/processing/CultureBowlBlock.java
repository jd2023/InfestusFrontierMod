package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jetbrains.annotations.Nullable;

/**
 * The hand-loaded culturing bowl. Buckets move water and biomass, ingredients go in one at a time, an empty sneaking
 * hand takes them back, an empty hand reports. Anything else passes through so the player can build against it.
 */
public final class CultureBowlBlock extends BaseEntityBlock {
    private static final MapCodec<CultureBowlBlock> CODEC = simpleCodec(CultureBowlBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 6, 15);

    CultureBowlBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<CultureBowlBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CultureBowlBlockEntity(pos, state);
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
        ItemInteractionResult fluid = BiomassFluids.useOn(stack, level, pos, hit.getDirection(), player, hand);
        if (fluid != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return fluid;
        if (!(level.getBlockEntity(pos) instanceof CultureBowlBlockEntity bowl) || !bowl.accepts(stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        ItemStack offered = player.getAbilities().instabuild ? stack.copy() : stack;
        var refusal = bowl.insert(offered);
        if (refusal.isPresent()) {
            Refusals.show(player, refusal.get());
            return ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof CultureBowlBlockEntity bowl)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) bowl.collect(player);
        else bowl.report(player);
        return InteractionResult.CONSUME;
    }
}
