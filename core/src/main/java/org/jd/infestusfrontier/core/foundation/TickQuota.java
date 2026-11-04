package org.jd.infestusfrontier.core.foundation;

/**
 * Constant-space admission for one owner on one thread. No queue, no world references. Admits one action with
 * {@link #take} or a count with {@link #grant}, for work that reads or writes many positions in one call.
 */
public final class TickQuota {
    private final int limit;
    private long tick = Long.MIN_VALUE;
    private int used;

    public TickQuota(int limit) {
        if (limit < 1) throw new IllegalArgumentException("Quota must be positive");
        this.limit = limit;
    }

    /** A changed simulation tick resets capacity, including after a clock rewind. */
    public boolean take(long now) {
        return grant(now, 1) == 1;
    }

    /**
     * Grants up to {@code wanted} units of this tick's capacity and returns how many were granted, 0 when none is
     * left. A changed simulation tick resets capacity, including after a clock rewind.
     */
    public int grant(long now, int wanted) {
        if (wanted < 0) throw new IllegalArgumentException("Negative request");
        if (now != tick) {
            tick = now;
            used = 0;
        }
        int granted = Math.min(wanted, limit - used);
        used += granted;
        return granted;
    }
}
