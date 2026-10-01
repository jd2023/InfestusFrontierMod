package org.jd.infestusfrontier.construction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Connected pane whose straight interior cells omit the center rib. */
final class MembraneWindowBlock extends IronBarsBlock {
    private static final VoxelShape ISOLATED_SHAPE = Shapes.or(
            box(0, 0, 7.75, 16, 16, 8.25), box(0, 0, 7.25, .75, 16, 8.75),
            box(15.25, 0, 7.25, 16, 16, 8.75), box(7.25, 0, 7.25, 8.75, 16, 8.75));
    static final BooleanProperty UP = BooleanProperty.create("up");
    static final BooleanProperty DOWN = BooleanProperty.create("down");
    static final BooleanProperty POST = BooleanProperty.create("post");

    MembraneWindowBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POST, true).setValue(UP, false).setValue(DOWN, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POST, UP, DOWN);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!placementAreaLoaded(level, pos)) return null;
        return withPost(super.getStateForPlacement(context))
                .setValue(UP, level.hasChunkAt(pos.above()) && level.getBlockState(pos.above()).is(this))
                .setValue(DOWN, level.hasChunkAt(pos.below()) && level.getBlockState(pos.below()).is(this));
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        var updated = withPost(super.updateShape(state, direction, neighbor, level, pos, neighborPos));
        if (direction == Direction.UP) updated = updated.setValue(UP, neighbor.is(this));
        if (direction == Direction.DOWN) updated = updated.setValue(DOWN, neighbor.is(this));
        return updated;
    }

    static boolean placementAreaLoaded(Level level, BlockPos pos) {
        // Four non-loading chunk predicates at most, including the diagonal corner.
        for (int x = (pos.getX() - 1) >> 4; x <= (pos.getX() + 1) >> 4; x++) {
            for (int z = (pos.getZ() - 1) >> 4; z <= (pos.getZ() + 1) >> 4; z++) {
                if (!level.hasChunk(x, z)) return false;
            }
        }
        return true;
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isolated(state) ? ISOLATED_SHAPE : super.getShape(state, level, pos, context);
    }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isolated(state) ? ISOLATED_SHAPE : super.getCollisionShape(state, level, pos, context);
    }

    private static boolean isolated(BlockState state) {
        return !state.getValue(NORTH) && !state.getValue(SOUTH) && !state.getValue(EAST) && !state.getValue(WEST);
    }

    private static BlockState withPost(BlockState state) {
        boolean north = state.getValue(NORTH);
        boolean south = state.getValue(SOUTH);
        boolean east = state.getValue(EAST);
        boolean west = state.getValue(WEST);
        boolean straight = north && south && !east && !west || east && west && !north && !south;
        return state.setValue(POST, !straight);
    }
}
