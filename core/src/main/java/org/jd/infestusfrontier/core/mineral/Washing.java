package org.jd.infestusfrontier.core.mineral;

import java.util.Optional;

/**
 * The Washing Kidney's accounting. A wash turns one residual unit of a freshly ground parcel into one dust and leaves a
 * parcel of {@link #WASHED_UNITS}; the wash adds {@link #DIRTY_PER_WASH} mB of dirty water, and a settle turns
 * {@link #DIRTY_PER_SETTLE} mB of it into one Rock Residue. A parcel that was washed is refused, so one raw item gives
 * at most {@link #DUST_PER_WASHED_RAW} dust. Every figure is checked integer arithmetic.
 */
public final class Washing {
    public static final int DUST_PER_WASH = 1;
    public static final int WASHED_UNITS = Grinding.TAILINGS_UNITS - DUST_PER_WASH;
    public static final int CLEAN_PER_WASH = 250;
    public static final int DIRTY_PER_WASH = 250;
    public static final int DIRTY_PER_SETTLE = 1000;
    public static final int DUST_PER_WASHED_RAW = Grinding.DUST_PER_RAW + DUST_PER_WASH;

    private Washing() {}

    /** What one wash gives: the dust and the parcel that remains. */
    public record Washed(Metal metal, int dust, Parcel remaining) {}

    /** The wash of {@code parcel}, or empty when it is not a fresh grind: a parcel can be washed once. */
    public static Optional<Washed> wash(Parcel parcel) {
        if (parcel.units() != Grinding.TAILINGS_UNITS) return Optional.empty();
        return Optional.of(new Washed(parcel.metal(), DUST_PER_WASH, new Parcel(parcel.metal(), WASHED_UNITS)));
    }

    /** What a Kidney line holds after washing {@code washes} fresh parcels and settling what fills a whole settle. */
    public record Line(
            long washes, long dust, long cleanMb, long dirtyMb, long settles, long residue, long dirtyLeft) {}

    /** The line's ledger for {@code washes} washes: all the dirty water that fills {@link #DIRTY_PER_SETTLE} is settled. */
    public static Line line(long washes) {
        if (washes < 0) throw new IllegalArgumentException("Negative washes " + washes);
        long dirty = Math.multiplyExact(washes, DIRTY_PER_WASH);
        long settles = dirty / DIRTY_PER_SETTLE;
        return new Line(
                washes,
                Math.multiplyExact(washes, DUST_PER_WASH),
                Math.multiplyExact(washes, CLEAN_PER_WASH),
                dirty,
                settles,
                settles,
                dirty % DIRTY_PER_SETTLE);
    }
}
