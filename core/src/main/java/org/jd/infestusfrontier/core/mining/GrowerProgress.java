package org.jd.infestusfrontier.core.mining;

/**
 * How far a Structure Grower has come: the steps it has built, which is its history, and the loaded ticks waited
 * towards the next step. A step is built whole in one tick or not at all; where in a plan it stands is the marker's.
 * A value outside the limits does not construct.
 */
public record GrowerProgress(int steps, int ticks) {
    public static final GrowerProgress START = new GrowerProgress(0, 0);

    public GrowerProgress {
        if (steps < 0) throw new IllegalArgumentException("Steps: " + steps);
        if (ticks < 0 || ticks > Surveying.GROW_TICKS) throw new IllegalArgumentException("Ticks: " + ticks);
    }

    /** True when the next step may be tried. */
    public boolean due() {
        return ticks == Surveying.GROW_TICKS;
    }

    /** One more loaded tick of waiting, never past the point where a step is due. */
    public GrowerProgress waited() {
        return due() ? this : new GrowerProgress(steps, ticks + 1);
    }

    /** A step was built: one more in the history, which stops counting at its largest value, and the wait restarts. */
    public GrowerProgress grown() {
        return new GrowerProgress(Counts.more(steps, 1), 0);
    }
}
