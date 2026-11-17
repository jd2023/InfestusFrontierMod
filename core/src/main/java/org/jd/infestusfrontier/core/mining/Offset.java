package org.jd.infestusfrontier.core.mining;

/**
 * A block of a plan relative to its marker: {@code forward} the way the marker faces, {@code right} a quarter turn
 * clockwise of that, {@code up} up. The marker itself is the origin.
 */
public record Offset(int forward, int right, int up) {
    /** This offset moved one block along {@code heading}, in quarter turns clockwise from forward. */
    Offset along(int heading) {
        return switch (Math.floorMod(heading, 4)) {
            case 0 -> new Offset(forward + 1, right, up);
            case 1 -> new Offset(forward, right + 1, up);
            case 2 -> new Offset(forward - 1, right, up);
            default -> new Offset(forward, right - 1, up);
        };
    }

    /** This offset {@code blocks} higher. */
    public Offset above(int blocks) {
        return new Offset(forward, right, up + blocks);
    }
}
