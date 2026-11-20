package org.jd.infestusfrontier.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jd.infestusfrontier.core.mining.Offset;

/** Horizontal headings shared by mining plans, movement, menus and previews. */
final class MiningDirections {
    private MiningDirections() {}

    /** Converts plan coordinates to a world position, using the marker's heading on both server and client. */
    static BlockPos at(BlockPos origin, Direction forward, Offset offset) {
        return origin.relative(forward, offset.forward())
                .relative(forward.getClockWise(), offset.right())
                .above(offset.up());
    }

    /** The heading after clockwise quarter turns, relative to the organ rather than the player. */
    static Direction turn(Direction forward, int quarters) {
        Direction way = forward;
        for (int n = 0; n < quarters; n++) way = way.getClockWise();
        return way;
    }
}
