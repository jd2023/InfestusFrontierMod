package org.jd.infestusfrontier.mining;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The shape of a block model drawn facing north, turned to each horizontal facing as its blockstate turns the model. */
final class FacingShapes {
    private FacingShapes() {}

    /** {@code boxes}, each {x1, y1, z1, x2, y2, z2} in model pixels facing north, as one shape per facing. */
    static Map<Direction, VoxelShape> of(double[][] boxes) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] box : boxes) shape = Shapes.or(shape, turned(box, facing));
            shapes.put(facing, shape.optimize());
        }
        return shapes;
    }

    /** {@code box} turned clockwise, seen from above, by the quarter turns that take north to {@code facing}. */
    private static VoxelShape turned(double[] box, Direction facing) {
        double x1 = box[0], z1 = box[2], x2 = box[3], z2 = box[5];
        int turns = Math.floorMod(facing.get2DDataValue() - Direction.NORTH.get2DDataValue(), 4);
        for (int turn = 0; turn < turns; turn++) {
            double nx1 = 16 - z2, nz1 = x1, nx2 = 16 - z1, nz2 = x2;
            x1 = nx1;
            z1 = nz1;
            x2 = nx2;
            z2 = nz2;
        }
        return Block.box(x1, box[1], z1, x2, box[4], z2);
    }
}
