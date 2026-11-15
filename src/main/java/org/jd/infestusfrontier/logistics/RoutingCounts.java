package org.jd.infestusfrontier.logistics;

/**
 * Work the item routing does, counted where it is done, so that a test can bound it exactly. Counts only; nothing
 * reads them to decide anything. Server thread only.
 */
public final class RoutingCounts {
    public enum Step {
        /** Component nodes walked to find the Mouths of a network. */
        NODES,
        /** Block-entity lookups of a Mouth by position, including the chunk-loaded check. */
        LOOKUPS,
        /** Destinations put into an order: sorted and ranked. */
        RANKED,
        /** Whole-network endpoint indexes built. */
        BUILDS,
        /** Operations on a store: a simulated insert, an insert, an extraction. */
        INVENTORY
    }

    private static final long[] COUNTS = new long[Step.values().length];

    private RoutingCounts() {}

    static void add(Step step, int amount) {
        COUNTS[step.ordinal()] += amount;
    }

    public static long get(Step step) {
        return COUNTS[step.ordinal()];
    }

    public static void reset() {
        java.util.Arrays.fill(COUNTS, 0);
    }
}
