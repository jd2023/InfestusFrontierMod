package org.jd.infestusfrontier.core.mining;

import java.util.Optional;

/** The two shapes of a surveyed section. */
public enum PlanShape {
    /** Descends one block a step, straight ahead, to a landing. */
    STAIRCASE,
    /** Stays level and may turn twice. */
    ROUTE;

    /** The shape at {@code index}, or empty when no shape has it: a saved or synced number. */
    public static Optional<PlanShape> at(int index) {
        return index >= 0 && index < values().length ? Optional.of(values()[index]) : Optional.empty();
    }
}
