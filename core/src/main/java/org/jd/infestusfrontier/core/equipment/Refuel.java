package org.jd.infestusfrontier.core.equipment;

import java.util.List;

/**
 * The Fuel Papilla's arithmetic (BLOCK_CATALOG T1-36): a one-second batch delivers at most {@link #RATE} BU and costs
 * the delivered amount plus ceil(delivered / 5) BU of overhead.
 */
public final class Refuel {
    /** BU delivered to one wearer in one second, at most. */
    public static final int RATE = 10;
    /** The fill limits a pad can be set to, as percent of a piece's capacity. */
    public static final List<Integer> LIMITS = List.of(25, 50, 75, 100);

    private Refuel() {}

    /** What delivering {@code delivered} BU costs the pad. */
    public static int cost(int delivered) {
        if (delivered < 0) throw new IllegalArgumentException("Negative delivery");
        return delivered + (delivered + 4) / 5;
    }

    /** The most a pad holding {@code stored} BU can deliver this second. */
    public static int affordable(int stored) {
        int delivered = Math.min(RATE, Math.max(0, stored));
        while (delivered > 0 && cost(delivered) > stored) delivered--;
        return delivered;
    }

    /** How much a piece holding {@code stored} of {@code capacity} still wants under a limit of {@code percent}. */
    public static int wanted(int stored, int capacity, int percent) {
        long target = (long) capacity * percent / 100;
        return (int) Math.max(0, target - stored);
    }

    /** The next fill limit after {@code percent}, wrapping; an unknown value starts again at the first. */
    public static int nextLimit(int percent) {
        int index = LIMITS.indexOf(percent);
        return LIMITS.get(index < 0 ? 0 : (index + 1) % LIMITS.size());
    }
}
