package org.jd.infestusfrontier.ui;

import java.util.Objects;

/** Per-viewer presentation cadence: changes coalesce; unchanged state never emits. */
public final class ChangedSnapshot<T> {
    private T sent;
    private long sentTick;
    public ChangedSnapshot(T initial, long tick) { sent = initial; sentTick = tick; }
    public boolean take(T current, long tick) {
        if (Objects.equals(current, sent) || tick - sentTick < 10) return false;
        sent = current;
        sentTick = tick;
        return true;
    }
}
