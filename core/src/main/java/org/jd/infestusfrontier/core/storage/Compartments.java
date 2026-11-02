package org.jd.infestusfrontier.core.storage;

/**
 * The compartments of an item store (BLOCK_CATALOG T1-09): each holds one kind of item, up to its own limit of
 * stacks of it, since a grown capsule holds more than a young one. Items of a kind go to the compartments already
 * holding it, in order, and only then to the first empty one, so a kind never spreads over compartments while one of
 * them has room.
 *
 * <p>A store is read as one count per compartment for the kind in question: {@link #OTHER} for a compartment holding
 * another kind, 0 for an empty one, {@link #LABELLED} for an empty one labelled for this kind, and how many it holds of
 * this kind otherwise; and one limit per compartment, the
 * most of the kind it may hold.
 */
public final class Compartments {
    /** A compartment that holds another kind, or is empty and labelled for another. */
    public static final int OTHER = -1;

    /** An empty compartment labelled for this kind: it opens before an unlabelled empty one. */
    public static final int LABELLED = -2;

    private Compartments() {}

    /** How many of a kind that stacks to {@code maxStackSize} a compartment of {@code stacks} stacks holds. */
    public static int limit(int stacks, int maxStackSize) {
        if (stacks < 1 || maxStackSize < 1) throw new IllegalArgumentException(stacks + " stacks of " + maxStackSize);
        return Math.multiplyExact(stacks, maxStackSize);
    }

    /**
     * Where up to {@code amount} of one kind goes: the compartments holding it in order, then the empty ones in order,
     * none past its limit. Returns what each compartment takes; their sum is what fits. Changes nothing.
     */
    public static int[] plan(int[] counts, int[] limits, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount " + amount);
        if (limits.length != counts.length) throw new IllegalArgumentException("a limit for every compartment");
        int[] taken = new int[counts.length];
        int left = amount;
        int[] passes = {1, LABELLED, 0};
        for (int pass = 0; pass < passes.length && left > 0; pass++)
            for (int i = 0; i < counts.length && left > 0; i++) {
                if (pass == 0 ? counts[i] <= 0 : counts[i] != passes[pass]) continue;
                int room = Math.max(0, limits[i] - Math.max(0, counts[i]));
                taken[i] = Math.min(room, left);
                left -= taken[i];
            }
        return taken;
    }

    /**
     * The one empty compartment a new item of the kind may open: the first empty one, and only while no compartment
     * holding the kind has room; an empty compartment labelled for the kind comes before any unlabelled one. -1 when
     * there is none.
     */
    public static int opening(int[] counts, int[] limits) {
        int empty = -1;
        int labelled = -1;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0 && counts[i] < limits[i]) return -1;
            if (counts[i] == 0 && empty < 0) empty = i;
            if (counts[i] == LABELLED && labelled < 0) labelled = i;
        }
        return labelled >= 0 ? labelled : empty;
    }

    /**
     * How many of the kind compartment {@code slot} takes on its own: its room when it holds the kind, its whole limit
     * when it is the {@link #opening}, otherwise none.
     */
    public static int room(int[] counts, int[] limits, int slot) {
        if (counts[slot] > 0) return Math.max(0, limits[slot] - counts[slot]);
        return counts[slot] <= 0 && counts[slot] != OTHER && slot == opening(counts, limits) ? limits[slot] : 0;
    }
}
