package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jetbrains.annotations.Nullable;

/**
 * One cell of a vein: mature Living Substrate with a recessed lumen that carries one {@link Carrier}. The six
 * connection properties say which neighbours are cells of the same vein, for the model; the Fluid Vein's
 * {@link #SEALED} says whether Sealing Resin closed it. A fluid cell never ticks on its own: it schedules a tick when
 * it receives a packet and goes quiet when it has nothing to move. An Item Vein is a passage of an item network; the
 * Mouths move the items.
 */
public final class VeinBlock extends BaseEntityBlock {
    /** A Fluid Vein carries nothing until Sealing Resin seals it; the other two veins are sealed from the start. */
    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");
    /** Ticks between two tries when the last try moved nothing. */
    public static final int RETRY_TICKS = 20;

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
     * True when {@code other} on {@code side} is a cell of the same vein, or a Mouth beside or under an Item Vein. The
     * links only shape the model: a vein still fills an organ or a Junction beside it, but its groove stops at that
     * block's face. A Mouth's top face holds its store, so a vein above a Mouth does not join it.
     */
    private boolean connects(Direction side, BlockState other) {
        if (other.getBlock() instanceof VeinBlock vein) return vein.carrier == carrier;
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

    /** A neighbour changed: a held packet may have somewhere to go now. */
    @Override
    protected void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof VeinBlockEntity vein) vein.wake();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof VeinBlockEntity vein) vein.pulse();
    }

    /** An Item Vein is a passage and holds nothing, so it has no block entity. */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return carrier == Carrier.ITEM ? null : new VeinBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
        if (carrier == Carrier.ITEM && !level.isClientSide && !old.is(this)) LogisticsModule.routesChanged(level);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (carrier == Carrier.ITEM && !level.isClientSide && !state.is(newState.getBlock()))
            LogisticsModule.routesChanged(level);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** A Creative player who breaks a loaded cell still gets it back, as with a shulker box. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide
                && player.isCreative()
                && level.getBlockEntity(pos) instanceof VeinBlockEntity vein
                && !vein.isEmpty()) {
            ItemStack stack = new ItemStack(this);
            vein.saveToItem(stack, level.registryAccess());
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
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
