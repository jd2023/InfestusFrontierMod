package org.jd.infestusfrontier.construction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Passive arch support with collision following its open skeletal silhouette. */
final class RibFrameBlock extends RotatedPillarBlock {
    private static final VoxelShape[] SHAPES = shapes();

    RibFrameBlock(Properties properties) { super(properties); }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AXIS).ordinal()];
    }

    private static VoxelShape[] shapes() {
        var result = new VoxelShape[] {Shapes.empty(), Shapes.empty(), Shapes.empty()};
        double[][] segments = {{0, 0, 3, 6}, {1, 5, 4, 10}, {2, 9, 6, 13}, {4, 12, 8, 15},
                {7, 13, 9, 16}, {8, 12, 12, 15}, {10, 9, 14, 13}, {12, 5, 15, 10}, {13, 0, 16, 6}};
        for (var b : segments) {
            result[Direction.Axis.Y.ordinal()] = Shapes.or(result[Direction.Axis.Y.ordinal()], box(b[0], b[1], 6, b[2], b[3], 10));
            result[Direction.Axis.Z.ordinal()] = Shapes.or(result[Direction.Axis.Z.ordinal()], box(b[0], 6, 16 - b[3], b[2], 10, 16 - b[1]));
            result[Direction.Axis.X.ordinal()] = Shapes.or(result[Direction.Axis.X.ordinal()], box(b[1], 6, b[0], b[3], 10, b[2]));
        }
        return result;
    }
}
