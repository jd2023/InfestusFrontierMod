package org.jd.infestusfrontier.core.storage;

/**
 * The compartments of an item store (BLOCK_CATALOG T1-09): each holds one kind of item, up to {@value #STACKS} stacks
 * of it. Items of a kind go to the compartments already holding it, in order, and only then to the first empty one, so
 * a kind never spreads over compartments while one of them has room.
 *
 * <p>A store is read as one count per compartment for the kind in question: {@link #OTHER} for a compartment holding
 * another kind, 0 for an empty one, and how many it holds of this kind otherwise.
 */
public final class Compartments {
    /** Stacks of its kind one compartment holds: 1024 of a kind that stacks to 64. A starting value. */
    public static final int STACKS = 16;
    /** A compartment that holds another kind. */
    public static final int OTHER = -1;

    private Compartments() {}

    /** How many of a kind that stacks to {@code maxStackSize} one compartment holds. */
    public static int limit(int maxStackSize) {
        if (maxStackSize < 1) throw new IllegalArgumentException("Stack size " + maxStackSize);
        return Math.multiplyExact(STACKS, maxStackSize);
    }

    /**
     * Where up to {@code amount} of one kind goes: the compartments holding it in order, then the empty ones in order,
     * none past {@code limit}. Returns what each compartment takes; their sum is what fits. Changes nothing.
     */
    public static int[] plan(int[] counts, int limit, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Amount " + amount);
        int[] taken = new int[counts.length];
        int left = amount;
        for (int pass = 0; pass < 2 && left > 0; pass++)
            for (int i = 0; i < counts.length && left > 0; i++) {
                boolean holds = counts[i] > 0;
                if (pass == 0 ? !holds : counts[i] != 0) continue;
                int room = Math.max(0, limit - counts[i]);
                taken[i] = Math.min(room, left);
                left -= taken[i];
            }
        return taken;
    }

    /**
     * The one empty compartment a new item of the kind may open: the first empty one, and only while no compartment
     * holding the kind has room. -1 when there is none.
     */
    public static int opening(int[] counts, int limit) {
        int empty = -1;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0 && counts[i] < limit) return -1;
            if (counts[i] == 0 && empty < 0) empty = i;
        }
        return empty;
    }

    /**
     * How many of the kind compartment {@code slot} takes on its own: its room when it holds the kind, a whole
     * compartment when it is the {@link #opening}, otherwise none.
     */
    public static int room(int[] counts, int limit, int slot) {
        if (counts[slot] > 0) return Math.max(0, limit - counts[slot]);
        return counts[slot] == 0 && slot == opening(counts, limit) ? limit : 0;
    }
}
