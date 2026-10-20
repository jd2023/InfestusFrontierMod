package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Facing points from the host, through the mouth, to the partner. */
final class MouthBlock extends BaseEntityBlock {
    private final boolean intake;
    private final MapCodec<MouthBlock> codec;

    MouthBlock(boolean intake, Properties properties) {
        super(properties);
        this.intake = intake;
        this.codec = simpleCodec(p -> new MouthBlock(intake, p));
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    boolean intake() {
        return intake;
    }

    @Override
    protected MapCodec<MouthBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(BlockStateProperties.FACING).getAxis()) {
            case X -> Block.box(0, 3, 3, 16, 13, 13);
            case Y -> Block.box(3, 0, 3, 13, 16, 13);
            case Z -> Block.box(3, 3, 0, 13, 13, 16);
        };
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        BlockPos host = context.getClickedPos().relative(face.getOpposite());
        return context.getLevel().hasChunkAt(host) && !context.getLevel().isEmptyBlock(host)
                ? defaultBlockState().setValue(BlockStateProperties.FACING, face)
                : null;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        return side == state.getValue(BlockStateProperties.FACING).getOpposite() && neighbor.isAir()
                ? Blocks.AIR.defaultBlockState()
                : state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide && !old.is(this)) level.scheduleTick(pos, this, MouthBlockEntity.PULSE_TICKS);
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
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof MouthBlockEntity mouth)
            mouth.dropPending();
        super.onRemove(state, level, pos, next, moved);
    }

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
}
