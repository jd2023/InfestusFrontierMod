package org.jd.infestusfrontier.core.mining;

import java.util.Optional;

/**
 * Which way the next step of a Tunnel Root goes, relative to where the head faces, never to where the player looks. One
 * step, then straight again.
 */
public enum Steer {
    STRAIGHT,
    LEFT,
    RIGHT;

    /** Quarter turns clockwise this way adds to the heading. */
    public int quarters() {
        return switch (this) {
            case STRAIGHT -> 0;
            case LEFT -> 3;
            case RIGHT -> 1;
        };
    }

    /** The setting at {@code index}, or empty when no setting has it: a screen button id or a saved number. */
    public static Optional<Steer> at(int index) {
        return index >= 0 && index < values().length ? Optional.of(values()[index]) : Optional.empty();
    }
}
