package org.jd.infestusfrontier.core.mineral;

/**
 * The first release's mineral accounting. One raw item is ground into {@link #DUST_PER_RAW} dust, one Tailings of
 * {@link #TAILINGS_UNITS} residual units and one Rock Residue; four dust smelt to one ingot. Every figure is checked
 * integer arithmetic, so a ledger that overflows throws instead of wrapping.
 */
public final class Grinding {
    public static final int DUST_PER_RAW = 4;
    public static final int TAILINGS_UNITS = 4;
    public static final int DUST_PER_INGOT = 4;

    private Grinding() {}

    /** What {@code raw} ground raw items hold in every product. */
    public record Ground(long raw, long dust, long tailings, long residualUnits, long residue) {
        /** Metal units in the raw items: every one ends as dust or as residual units of a Tailings. */
        public long recovered() {
            return Math.addExact(dust, residualUnits);
        }
    }

    /** The products of grinding {@code raw} raw items. */
    public static Ground grind(long raw) {
        if (raw < 0) throw new IllegalArgumentException("Negative raw count " + raw);
        return new Ground(
                raw, Math.multiplyExact(raw, DUST_PER_RAW), raw, Math.multiplyExact(raw, TAILINGS_UNITS), raw);
    }

    /** What {@code dust} dust smelts to: whole ingots, and the dust that waits for the next batch. */
    public record Smelt(long ingots, long leftover) {}

    public static Smelt smelt(long dust) {
        if (dust < 0) throw new IllegalArgumentException("Negative dust " + dust);
        return new Smelt(dust / DUST_PER_INGOT, dust % DUST_PER_INGOT);
    }
}
