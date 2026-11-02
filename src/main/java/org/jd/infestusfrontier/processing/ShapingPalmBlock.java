package org.jd.infestusfrontier.processing;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
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
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jetbrains.annotations.Nullable;

/**
 * The Shaping Palm (BLOCK_CATALOG T1-41, stages 1 and 2): a crafting table that keeps its grid. It stands on an organ
 * foundation, and any hand opens its screen, like a chest. A Contractile Fiber used on it gives it Reach, one supply
 * link. It has no state beyond its block entity, no ticks, and no item handler: nothing but the player at its screen
 * puts anything in or takes anything out.
 */
public final class ShapingPalmBlock extends BaseEntityBlock {
    private static final MapCodec<ShapingPalmBlock> CODEC = simpleCodec(ShapingPalmBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);

    ShapingPalmBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ShapingPalmBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Organs.hasFoundation(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShapingPalmBlockEntity(pos, state);
    }

    /** A Creative player who breaks a loaded Palm still gets it back with its grid, as with a shulker box. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide
                && player.isCreative()
                && level.getBlockEntity(pos) instanceof ShapingPalmBlockEntity palm
                && !palm.isEmpty()) {
            ItemStack stack = new ItemStack(this);
            palm.saveToItem(stack, level.registryAccess());
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
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
        if (stack.is(ProcessingModule.PALM_GRAFTS)) return graft(stack, level, pos, player);
        return Organs.openScreen(stack, level, pos, player);
    }

    /** One Reach graft on a Palm: spent only when the Palm takes it, kept by a Creative player. */
    private static ItemInteractionResult graft(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof ShapingPalmBlockEntity palm))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        Optional<Refusal> why = palm.grantReach();
        if (why.isPresent()) {
            Refusals.show(player, why.get());
            return ItemInteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.translatable("infestusfrontier.status.palm.reach"), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
