package org.jd.infestusfrontier.core.mining;

import java.util.Optional;

/** Which way the next step of a Tunnel Root goes, relative to where the head faces. One step, then straight again. */
public enum Steer {
    STRAIGHT,
    LEFT,
    RIGHT;

    /** The setting after this one, as a single screen button cycles them. */
    public Steer next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** The setting at {@code index}, or empty when no setting has it: a screen button id or a saved number. */
    public static Optional<Steer> at(int index) {
        return index >= 0 && index < values().length ? Optional.of(values()[index]) : Optional.empty();
    }
}
