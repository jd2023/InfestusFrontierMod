package org.jd.infestusfrontier.core.mining;

import java.util.Optional;

/** One change the player asks of a draft plan at its marker's screen. */
public enum PlanEdit {
    /** Staircase to route and back. */
    SHAPE,
    SHORTER,
    LONGER,
    /** The first turn: none, left, right, none again. */
    FIRST_WAY,
    FIRST_EARLIER,
    FIRST_LATER,
    SECOND_WAY,
    SECOND_EARLIER,
    SECOND_LATER,
    /** A route one column wide and three wide, the Wider Section, and back. */
    WIDTH;

    /** The edit at {@code index}, or empty when no edit has it: a screen button id. */
    public static Optional<PlanEdit> at(int index) {
        return index >= 0 && index < values().length ? Optional.of(values()[index]) : Optional.empty();
    }
}
