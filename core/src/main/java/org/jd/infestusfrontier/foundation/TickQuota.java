package org.jd.infestusfrontier.foundation;

/** Constant-space admission for one owner on one thread; no queue or world references. */
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
        if (now != tick) {
            tick = now;
            used = 0;
        }
        if (used == limit) return false;
        used++;
        return true;
    }
}
