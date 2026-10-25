package org.jd.infestusfrontier.storage;

import com.mojang.serialization.MapCodec;
import java.util.Map;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jd.infestusfrontier.core.storage.Box;
import org.jd.infestusfrontier.organ.api.Organs;

/**
 * One cell of an Item Capsule (BLOCK_CATALOG T1-09): a membrane cocoon of four compartments whose front, toward
 * {@link #FACING}, shows what they hold. Touching cells form one store only as a full rectangular box
 * ({@link org.jd.infestusfrontier.core.storage.Body}); the six side properties say which faces join a cell of the same
 * body, so walls and ribs show only where the body ends. Any hand opens the screen, sneaking builds against it.
 */
public final class ItemCapsuleBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final Map<Direction, BooleanProperty> JOINED = PipeBlock.PROPERTY_BY_DIRECTION;

    private static final MapCodec<ItemCapsuleBlock> CODEC = simpleCodec(ItemCapsuleBlock::new);

    ItemCapsuleBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
        for (BooleanProperty side : JOINED.values()) state = state.setValue(side, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        JOINED.values().forEach(builder::add);
    }

    @Override
    protected MapCodec<ItemCapsuleBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** A new body faces its builder; added cells inherit a neighbor, preferring the clicked support. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        Direction support = context.getClickedFace().getOpposite();
        Direction[] sides = Direction.values();
        for (int i = -1; i < sides.length; i++) {
            Direction side = i < 0 ? support : sides[i];
            if (i >= 0 && side == support) continue;
            BlockPos neighbor = context.getClickedPos().relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            BlockState state = level.getBlockState(neighbor);
            if (state.is(this)) return defaultBlockState().setValue(FACING, state.getValue(FACING));
        }
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** The look of the cell at {@code pos} in body {@code box}. */
    static BlockState shaped(BlockState state, BlockPos pos, Box box) {
        for (Map.Entry<Direction, BooleanProperty> side : JOINED.entrySet()) {
            BlockPos next = pos.relative(side.getKey());
            state = state.setValue(side.getValue(), box.contains(next.getX(), next.getY(), next.getZ()));
        }
        return state;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemCapsuleBlockEntity(pos, state);
    }

    /** A new cell looks for a body on the next tick, once its carried compartments are in. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) level.scheduleTick(pos, this, Bodies.FORM_TICKS);
    }

    /** A cell that leaves takes its own compartments; the rest of its body falls apart into cells that form again. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity cell)
            Bodies.leave(cell, Capsules.KIND);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity cell) Bodies.form(cell, Capsules.KIND);
    }

    /** A Creative player who breaks a cell holding items still gets them back, as with a shulker box. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide
                && player.isCreative()
                && level.getBlockEntity(pos) instanceof ItemCapsuleBlockEntity cell
                && !cell.isEmpty()) {
            ItemStack stack = new ItemStack(this);
            stack.applyComponents(cell.collectComponents());
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
        return Organs.openScreen(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Organs.openScreen(level, pos, player).result();
    }
}
