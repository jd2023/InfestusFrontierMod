package org.jd.infestusfrontier.core.logistics;

import java.util.List;

/** What a Mouth's settings allow: the amounts and priorities it can be set to, and how much each side of a transfer takes. */
public final class Delivery {
    public static final int MIN_PRIORITY = -4;
    public static final int MAX_PRIORITY = 4;
    /** The amounts a Mouth can be set to, in order; 0 means no amount rule. */
    public static final List<Integer> AMOUNTS = List.of(0, 1, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 4096);

    private Delivery() {}

    /** One intake Mouth on the network. */
    public record Destination<T>(T id, int priority) {}

    /** How many an intake may add to a store holding {@code present}: up to {@code amount}, or any with 0. */
    public static int intakeRoom(int amount, int present, int offered) {
        if (amount == 0) return offered;
        return Math.max(0, Math.min(offered, amount - present));
    }

    /** How many an output may take from a store holding {@code present} while keeping {@code amount} there. */
    public static int outputAllowance(int amount, int present, int offered) {
        return Math.max(0, Math.min(offered, present - amount));
    }

    /** The next setting in {@link #AMOUNTS} after {@code amount}, wrapping; {@code up} false goes the other way. */
    public static int nextAmount(int amount, boolean up) {
        int index = AMOUNTS.indexOf(amount);
        if (index < 0) return 0;
        return AMOUNTS.get(Math.floorMod(index + (up ? 1 : -1), AMOUNTS.size()));
    }

    /** The next priority, stopping at the ends. */
    public static int nextPriority(int priority, boolean up) {
        return Math.clamp(priority + (up ? 1 : -1), MIN_PRIORITY, MAX_PRIORITY);
    }
}
