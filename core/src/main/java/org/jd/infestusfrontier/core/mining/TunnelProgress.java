package org.jd.infestusfrontier.core.mining;

/**
 * How far a Tunnel Root has come: the root cells it has laid, which is its history, and the loaded ticks waited
 * towards the next step. A step is done whole in one tick or not at all, so this is all the head needs to save about
 * its work. A value outside the limits does not construct, so impossible saved numbers are refused, not repaired.
 */
public record TunnelProgress(int cells, int ticks) {
    public static final TunnelProgress START = new TunnelProgress(0, 0);

    public TunnelProgress {
        if (cells < 0 || cells > Tunnelling.MAX_CELLS) throw new IllegalArgumentException("Cells: " + cells);
        if (ticks < 0 || ticks > Tunnelling.STEP_TICKS) throw new IllegalArgumentException("Ticks: " + ticks);
    }

    /** True once the root has all its cells: the head takes no more steps. */
    public boolean finished() {
        return cells == Tunnelling.MAX_CELLS;
    }

    /** True when the next step may be tried. */
    public boolean due() {
        return ticks == Tunnelling.STEP_TICKS;
    }

    /** One more loaded tick of waiting, never past the point where a step is due. */
    public TunnelProgress waited() {
        return due() ? this : new TunnelProgress(cells, ticks + 1);
    }

    /** Loaded ticks left before the next step is due. */
    public int ticksLeft() {
        return Tunnelling.STEP_TICKS - ticks;
    }

    /** A step was taken: one more cell, and the wait starts over. */
    public TunnelProgress stepped() {
        if (finished()) throw new IllegalStateException("The root is at its full length");
        return new TunnelProgress(cells + 1, 0);
    }
}
