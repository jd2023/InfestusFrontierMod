package org.jd.infestusfrontier.core.mining;

/**
 * One turn of a level route: at cell {@code at} the route turns {@code way} and goes on from there. {@code STRAIGHT}
 * means the slot holds no turn; its cell is then 0.
 */
public record Turn(Steer way, int at) {
    public static final Turn NONE = new Turn(Steer.STRAIGHT, 0);

    public Turn {
        if (way == Steer.STRAIGHT ? at != 0 : at < 1) throw new IllegalArgumentException("Turn " + way + " at " + at);
    }

    public boolean present() {
        return way != Steer.STRAIGHT;
    }

    /** Quarter turns clockwise this turn adds to the heading. */
    int quarter() {
        return way.quarters();
    }
}
