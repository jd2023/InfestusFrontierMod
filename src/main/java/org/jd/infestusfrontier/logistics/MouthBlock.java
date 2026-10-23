package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jetbrains.annotations.Nullable;

/**
 * A cell of living substrate with lips on its top face. The store standing on it is its partner; the Item Veins and
 * Mouths joined to its other five faces are its network. The six link properties feed the model; {@link #MODE} is the
 * setting the screen changes.
 */
public final class MouthBlock extends BaseEntityBlock {
    public static final EnumProperty<MouthMode> MODE = EnumProperty.create("mode", MouthMode.class);
    public static final MapCodec<MouthBlock> CODEC = simpleCodec(MouthBlock::new);

    MouthBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(MODE, MouthMode.INTAKE);
        for (BooleanProperty side : PipeBlock.PROPERTY_BY_DIRECTION.values()) state = state.setValue(side, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                MODE, PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN);
    }

    @Override
    protected MapCodec<MouthBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Whether items pass between a network cell and {@code state} through the face {@code face} of {@code state}: a
     * Mouth through any face but its top, which holds its store; an Item Vein, item crossing or item stitch through
     * the faces it opens.
     */
    static boolean opens(BlockState state, Direction face) {
        if (state.getBlock() instanceof MouthBlock) return face != Direction.UP;
        return Routes.opens(state, face, Carrier.ITEM);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Block.updateFromNeighbourShapes(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    /** The top face holds the partner, never a link. */
    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        return state.setValue(
                PipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                direction != Direction.UP
                        && (neighborState.getBlock() instanceof MouthBlock
                                || opens(neighborState, direction.getOpposite())));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (level.isClientSide) return;
        if (!old.is(this) || old.getValue(MODE) != state.getValue(MODE)) LogisticsModule.routesChanged(level);
        // the block entity still holds the old state here, so the new state decides whether pulses start
        if (state.getValue(MODE).gives() && !level.getBlockTicks().hasScheduledTick(pos, this))
            level.scheduleTick(pos, this, MouthBlockEntity.PULSE_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof MouthBlockEntity mouth) mouth.pulse();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MouthBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock())) {
            if (level.getBlockEntity(pos) instanceof MouthBlockEntity mouth) mouth.dropPackets();
            if (!level.isClientSide) LogisticsModule.routesChanged(level);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    /** A Creative player who breaks a Mouth whose save could not be read still gets those bytes back on its item. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide
                && player.isCreative()
                && level.getBlockEntity(pos) instanceof MouthBlockEntity mouth
                && mouth.isUnreadable()) {
            ItemStack stack = new ItemStack(this);
            mouth.saveToItem(stack, level.registryAccess());
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Any hand opens the screen, like a chest; sneaking with an item builds against the cell instead. */
    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (stack.is(Organs.READERS)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return Organs.openScreen(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
