package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jd.infestusfrontier.core.mining.Leaching;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jetbrains.annotations.Nullable;

/**
 * A one-use organ that weakens the ground below it, a layer a second, so a pit can be dug by hand and its ore seen,
 * and grows a glowing root down its own column that lights the pit. It is grown from an Organ Bud on a foundation and
 * cannot be picked up again: broken, it leaves nothing. It dies with the block below it, its root or its foundation,
 * whether working or finished, and that too leaves nothing. Any use reads its progress; sneaking builds against it.
 * {@link #SPENT} is a pod that finished its work: it stays, with its pore closed, and no longer ticks.
 */
public final class LeachingPodBlock extends MiningOrganBlock {
    public static final BooleanProperty SPENT = BooleanProperty.create("spent");

    /** Soft enough to clear by hand; unlike an organ it gives nothing back. */
    static final float HARDNESS = 1.0F;

    static final float BLAST_RESISTANCE = 3.0F;
    private static final MapCodec<LeachingPodBlock> CODEC = simpleCodec(LeachingPodBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 11, 13);

    LeachingPodBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SPENT, false));
    }

    @Override
    protected MapCodec<LeachingPodBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPENT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Organs.placedBy(level, pos, placer);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LeachingPodBlockEntity(pos, state);
    }

    /** A spent pod does nothing, so it does not tick. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(SPENT)) return null;
        return createTickerHelper(type, MiningModule.LEACHING_POD_ENTITY.get(), LeachingPodBlockEntity::serverTick);
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
        InteractionResult result = useWithoutItem(state, level, pos, player, hit);
        return result == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof LeachingPodBlockEntity pod)) return InteractionResult.PASS;
        if (pod.unreadable()) Refusals.show(player, Persistence.CORRUPT);
        else {
            if (pod.steward().claim(player)) pod.setChanged();
            player.displayClientMessage(status(pod), true);
        }
        return InteractionResult.CONSUME;
    }

    private static Component status(LeachingPodBlockEntity pod) {
        String key = pod.spent() ? "spent" : pod.waitingForGround() ? "waiting" : "working";
        return Component.translatable(
                "infestusfrontier.leaching_pod." + key, pod.layers(), Leaching.MAX_LAYERS, pod.charges());
    }
}
