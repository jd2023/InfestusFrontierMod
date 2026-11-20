package org.jd.infestusfrontier.mining;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A small luminous organ a Structure Grower grows in a mine from supplied living-light stock: a glowing bulb on a short
 * stalk, hung under the ceiling or on a wall. Ordinary block light; no block entity, no ticks. Vanilla's attachment
 * rules apply: when the block that holds it goes, it breaks. Grown tissue: it gives nothing back.
 */
public final class LumenNodeBlock extends FaceAttachedHorizontalDirectionalBlock {
    /** The light it gives: a torch's less one. */
    static final int LIGHT = 13;

    private static final MapCodec<LumenNodeBlock> CODEC = simpleCodec(LumenNodeBlock::new);
    private static final VoxelShape FLOOR = Block.box(5, 0, 5, 11, 7, 11);
    private static final VoxelShape CEILING = Block.box(5, 9, 5, 11, 16, 11);
    /** On a wall, by the way the node faces, away from the wall. */
    private static final Map<Direction, VoxelShape> WALL = Map.of(
            Direction.NORTH, Block.box(5, 5, 9, 11, 11, 16),
            Direction.SOUTH, Block.box(5, 5, 0, 11, 11, 7),
            Direction.EAST, Block.box(0, 5, 5, 7, 11, 11),
            Direction.WEST, Block.box(9, 5, 5, 16, 11, 11));

    LumenNodeBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.CEILING));
    }

    @Override
    protected MapCodec<LumenNodeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    /** The side of its block where the block that holds it is. */
    static Direction support(BlockState state) {
        return getConnectedDirection(state).getOpposite();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR;
            case CEILING -> CEILING;
            case WALL -> WALL.getOrDefault(state.getValue(FACING), Shapes.block());
        };
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
