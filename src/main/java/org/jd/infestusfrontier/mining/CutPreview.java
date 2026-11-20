package org.jd.infestusfrontier.mining;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * What a Tunnel Root head shows of its next step in the world: the two blocks it will cut, and an arrow lying on the
 * floor of that cut pointing the way it will go. Relative to the head's own block, from its block state alone; nothing
 * here reads the world.
 */
final class CutPreview {
    /** The part of the preview a box shows. */
    enum Part {
        CUT,
        ARROW
    }

    /** One box, relative to the head's own block. */
    record Box(AABB box, Part part) {}

    /** The arrow's boxes in its cell, as {forward from, forward to, right from, right to}, sixteenths. */
    private static final double[][] ARROW = {
        {2, 14, 7, 9}, {7, 9, 2, 4}, {9, 11, 4, 6}, {11, 13, 6, 10}, {9, 11, 10, 12}, {7, 9, 12, 14}
    };
    /** The arrow lies this high off the cut's floor, in sixteenths: flat, but clear of the floor's own face. */
    private static final double ARROW_LOW = 0.5;

    private static final double ARROW_HIGH = 1.5;
    private static final double SIXTEENTH = 1.0 / 16;

    private CutPreview() {}

    /** The next cut of the head whose block state is {@code state}. */
    static List<Box> of(BlockState state) {
        Direction facing = state.getValue(TunnelRootBlock.FACING);
        Direction way = MiningDirections.turn(
                facing, state.getValue(TunnelRootBlock.STEER).steer().quarters());
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(
                new AABB(way.getStepX(), 0, way.getStepZ(), way.getStepX() + 1, 2, way.getStepZ() + 1), Part.CUT));
        for (double[] piece : ARROW) boxes.add(new Box(arrow(way, piece), Part.ARROW));
        return boxes;
    }

    /** One piece of the arrow in the cut cell along {@code way}: forward and right are measured in that cell. */
    private static AABB arrow(Direction way, double[] piece) {
        Direction right = way.getClockWise();
        double centreX = way.getStepX() + 0.5;
        double centreZ = way.getStepZ() + 0.5;
        double[] xs = new double[2];
        double[] zs = new double[2];
        for (int corner = 0; corner < 2; corner++) {
            double forward = piece[corner] * SIXTEENTH - 0.5;
            double across = piece[2 + corner] * SIXTEENTH - 0.5;
            xs[corner] = centreX + forward * way.getStepX() + across * right.getStepX();
            zs[corner] = centreZ + forward * way.getStepZ() + across * right.getStepZ();
        }
        return new AABB(xs[0], ARROW_LOW * SIXTEENTH, zs[0], xs[1], ARROW_HIGH * SIXTEENTH, zs[1]);
    }
}
