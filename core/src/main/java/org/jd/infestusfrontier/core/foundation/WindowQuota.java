package org.jd.infestusfrontier.core.foundation;

/** Constant-space admission of at most {@code limit} actions in any window of {@code windowTicks} ticks. */
public final class WindowQuota {
    private final int limit;
    private final long windowTicks;
    private long windowStart = Long.MIN_VALUE;
    private int used;

    public WindowQuota(int limit, long windowTicks) {
        if (limit < 1 || windowTicks < 1) throw new IllegalArgumentException("Quota and window must be positive");
        this.limit = limit;
        this.windowTicks = windowTicks;
    }

    /** A clock that moved backwards starts a new window, so a rewind never blocks forever. */
    public boolean take(long now) {
        long elapsed = now - windowStart;
        if (now < windowStart || elapsed < 0 || elapsed >= windowTicks) {
            windowStart = now;
            used = 0;
        }
        if (used == limit) return false;
        used++;
        return true;
    }
}
