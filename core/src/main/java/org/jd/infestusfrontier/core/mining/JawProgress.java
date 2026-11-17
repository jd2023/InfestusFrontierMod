package org.jd.infestusfrontier.core.mining;

/**
 * How far a Fracture Jaw has come: the blocks it has cut, which is its history, and the loaded ticks waited towards
 * the next cut. A cut is done whole in one tick or not at all, so this is all the jaw needs to save about its work. A
 * value outside the limits does not construct, so impossible saved numbers are refused, not repaired.
 */
public record JawProgress(int cuts, int ticks) {
    public static final JawProgress START = new JawProgress(0, 0);

    public JawProgress {
        if (cuts < 0) throw new IllegalArgumentException("Cuts: " + cuts);
        if (ticks < 0 || ticks > Fracturing.CUT_TICKS) throw new IllegalArgumentException("Ticks: " + ticks);
    }

    /** True when the next cut may be tried. */
    public boolean due() {
        return ticks == Fracturing.CUT_TICKS;
    }

    /** One more loaded tick of waiting, never past the point where a cut is due. */
    public JawProgress waited() {
        return due() ? this : new JawProgress(cuts, ticks + 1);
    }

    /** Loaded ticks left before the next cut is due. */
    public int ticksLeft() {
        return Fracturing.CUT_TICKS - ticks;
    }

    /** A cut was made: one more in the history, which stops counting at its largest value, and the wait starts over. */
    public JawProgress cut() {
        return new JawProgress(Counts.more(cuts, 1), 0);
    }
}
