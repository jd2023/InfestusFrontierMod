package org.jd.infestusfrontier.core.logistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The order an output Mouth offers a packet to destinations, and how much each side of a transfer allows. */
public final class Delivery {
    public static final int MIN_PRIORITY = -4;
    public static final int MAX_PRIORITY = 4;
    /** The amounts a Mouth can be set to, in order; 0 means no amount rule. */
    public static final List<Integer> AMOUNTS = List.of(0, 1, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 4096);

    private Delivery() {}

    /** One intake Mouth on the network. */
    public record Destination<T>(T id, int priority, int distance) {}

    /**
     * Highest priority first, then nearest. Destinations equal in both take turns: the group starts at
     * {@code turn} modulo its size. Ties keep their input order otherwise.
     */
    public static <T> List<T> order(List<Destination<T>> destinations, int turn) {
        List<Destination<T>> sorted = new ArrayList<>(destinations);
        sorted.sort(
                Comparator.<Destination<T>>comparingInt(d -> -d.priority()).thenComparingInt(Destination::distance));
        List<T> out = new ArrayList<>(sorted.size());
        int start = 0;
        while (start < sorted.size()) {
            int end = start;
            Destination<T> first = sorted.get(start);
            while (end < sorted.size()
                    && sorted.get(end).priority() == first.priority()
                    && sorted.get(end).distance() == first.distance()) end++;
            int size = end - start;
            int offset = Math.floorMod(turn, size);
            for (int i = 0; i < size; i++)
                out.add(sorted.get(start + (offset + i) % size).id());
            start = end;
        }
        return List.copyOf(out);
    }

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
