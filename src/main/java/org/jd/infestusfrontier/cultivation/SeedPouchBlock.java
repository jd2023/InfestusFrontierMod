package org.jd.infestusfrontier.cultivation;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import org.jd.infestusfrontier.cultivation.api.PlantingStock;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jetbrains.annotations.Nullable;

/**
 * A rooted store for planting stock. Planting stock in hand goes in one at a time; a stick raises the reserve by
 * one, from 64 back to 0; anything else opens the screen; sneaking with an item builds against it. Breaking it spills the stock.
 */
public final class SeedPouchBlock extends BaseEntityBlock {
    private static final MapCodec<SeedPouchBlock> CODEC = simpleCodec(SeedPouchBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15);

    SeedPouchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SeedPouchBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SeedPouchBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SeedPouchBlockEntity pouch)
            pouch.dropStock(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
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
        if (!(level.getBlockEntity(pos) instanceof SeedPouchBlockEntity pouch))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.is(Items.STICK)) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            player.displayClientMessage(pouch.raiseReserve(), true);
            return ItemInteractionResult.CONSUME;
        }
        if (!PlantingStock.is(stack)) return Organs.openScreen(stack, level, pos, player);
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        ItemStack offered = player.getAbilities().instabuild ? stack.copy() : stack;
        return pouch.insert(offered) ? ItemInteractionResult.CONSUME : ItemInteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
