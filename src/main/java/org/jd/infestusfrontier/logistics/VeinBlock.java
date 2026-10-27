package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jetbrains.annotations.Nullable;

/** Passive network tissue. Connection properties shape the model; no vein owns contents or ticks. */
public final class VeinBlock extends Block {
    /** A Fluid Vein carries nothing until Sealing Resin seals it; the other two veins are sealed from the start. */
    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");

    private final Carrier carrier;
    private final MapCodec<VeinBlock> codec;

    VeinBlock(Carrier carrier, Properties properties) {
        super(properties);
        this.carrier = carrier;
        this.codec = simpleCodec(p -> new VeinBlock(carrier, p));
        BlockState state = stateDefinition.any();
        for (BooleanProperty side : PipeBlock.PROPERTY_BY_DIRECTION.values()) state = state.setValue(side, false);
        registerDefaultState(state.setValue(SEALED, carrier != Carrier.FLUID));
    }

    Carrier carrier() {
        return carrier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // runs from the super constructor, before the carrier is known: every vein has the property
        builder.add(
                PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN, SEALED);
    }

    @Override
    protected MapCodec<VeinBlock> codec() {
        return codec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * True when {@code other} on {@code side} is a cell of the same vein, a crossing or stitch of its carrier open
     * toward this cell, or a Mouth beside or under an Item Vein. The links only shape the model: a vein still fills an
     * organ or a Junction beside it, but its groove stops at that block's face. A Mouth's top face holds its store, so
     * a vein above a Mouth does not join it.
     */
    private boolean connects(Direction side, BlockState other) {
        if (Routes.opens(other, side.getOpposite(), carrier)) return true;
        return carrier == Carrier.ITEM && other.getBlock() instanceof MouthBlock && side != Direction.DOWN;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Block.updateFromNeighbourShapes(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), connects(direction, neighborState));
    }

    @Override
    protected void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
        LogisticsModule.routesChanged(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
        if (old.getBlock() != this || old.getValue(SEALED) != state.getValue(SEALED))
            LogisticsModule.routesChanged(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) LogisticsModule.routesChanged(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** A bucket moves fluid like on any tank; everything else builds against the cell, it has no screen. */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!carrier.isFluid()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (player.getMainHandItem().is(Organs.READERS)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return BiomassFluids.useOn(stack, level, pos, hit.getDirection(), player, hand);
    }
}
