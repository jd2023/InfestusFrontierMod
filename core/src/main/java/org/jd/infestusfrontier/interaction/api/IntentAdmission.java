package org.jd.infestusfrontier.interaction.api;

/** Four-entry sliding window owned by one player across menus; rejected intents are never queued. */
public final class IntentAdmission {
    public static final int WINDOW_TICKS = 20;
    public static final int MAX_INTENTS = 4;
    private final long[] admitted = new long[MAX_INTENTS];
    private int size;
    private int first;
    private long lastTick = Long.MIN_VALUE;

    public boolean take(long tick) {
        if (tick < 0 || tick < lastTick) return false;
        lastTick = tick;
        while (size > 0 && tick - admitted[first] >= WINDOW_TICKS) {
            first = (first + 1) % MAX_INTENTS;
            size--;
        }
        if (size == MAX_INTENTS) return false;
        admitted[(first + size) % MAX_INTENTS] = tick;
        size++;
        return true;
    }
}
