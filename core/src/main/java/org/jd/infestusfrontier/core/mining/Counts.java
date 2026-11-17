package org.jd.infestusfrontier.core.mining;

/** History counters of the mining organs: they only grow, and stop at their largest value instead of overflowing. */
public final class Counts {
    private Counts() {}

    /** {@code count} grown by {@code more}, or {@link Integer#MAX_VALUE} when the sum would pass it. */
    public static int more(int count, int more) {
        if (count < 0 || more < 0) throw new IllegalArgumentException("Negative count");
        long sum = (long) count + more;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }
}
