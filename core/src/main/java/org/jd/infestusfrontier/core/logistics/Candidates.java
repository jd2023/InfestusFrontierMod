package org.jd.infestusfrontier.core.logistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Where an endpoint's next attempt starts in the ordered candidates of its network, so that a bounded attempt reaches
 * every candidate. An attempt asks at most {@code limit} candidates from the cursor on, wrapping. When it moved
 * nothing, the cursor passes the candidates it asked, so a refusing prefix of any length (equal or mixed priority)
 * stops hiding the rest: while the order is unchanged, a usable candidate is asked within
 * {@code ceil(size / limit)} such attempts. An attempt that moved something sends the cursor back to the top, so the
 * highest priority is preferred again whenever it can transfer. State is kept per endpoint and never saved.
 */
public final class Candidates {
    private int next;

    /** The candidates the next attempt may ask: at most {@code limit}, from the cursor on and wrapping. */
    public <T> List<T> window(List<T> ordered, int limit) {
        int size = ordered.size();
        if (size == 0 || limit <= 0) return List.of();
        next = Math.floorMod(next, size);
        int count = Math.min(size, limit);
        List<T> window = new ArrayList<>(count);
        for (int i = 0; i < count; i++) window.add(ordered.get((next + i) % size));
        return List.copyOf(window);
    }

    /** An attempt asked {@code asked} candidates of its window and moved nothing: the next one starts after them. */
    public void refused(int asked) {
        next += asked;
    }

    /** An attempt moved something: the next one starts at the top again. */
    public void moved() {
        next = 0;
    }
}
