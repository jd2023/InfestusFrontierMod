package org.jd.infestusfrontier.mining;

import net.minecraft.core.Direction;

/** Horizontal headings shared by mining plans, movement, menus and previews. */
final class MiningDirections {
    private MiningDirections() {}

    /** The heading after clockwise quarter turns, relative to the organ rather than the player. */
    static Direction turn(Direction forward, int quarters) {
        Direction way = forward;
        for (int n = 0; n < quarters; n++) way = way.getClockWise();
        return way;
    }
}
