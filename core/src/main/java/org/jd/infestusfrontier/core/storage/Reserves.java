package org.jd.infestusfrontier.core.storage;

import java.util.ArrayList;
import java.util.List;
import org.jd.infestusfrontier.core.logistics.Delivery;

/**
 * The reserve of a Larder compartment (BLOCK_CATALOG T1-09): a count of its kind that no external extraction takes. A
 * compartment gives {@link #automatable} and keeps {@link #reserved}; the two always add up to what it holds.
 */
public final class Reserves {
    private Reserves() {}

    /** What an external extraction may take from a compartment holding {@code held} with {@code reserve}: never below 0. */
    public static int automatable(int held, int reserve) {
        if (held < 0 || reserve < 0) throw new IllegalArgumentException(held + " held, reserve " + reserve);
        return Math.max(0, held - reserve);
    }

    /** What the reserve keeps in a compartment holding {@code held}: the reserve, or all of it when it holds less. */
    public static int reserved(int held, int reserve) {
        return held - automatable(held, reserve);
    }

    /** The settings up to {@code limit}: the Mouth's amounts that fit, then the limit itself. */
    public static List<Integer> steps(int limit) {
        if (limit < 0) throw new IllegalArgumentException("limit " + limit);
        List<Integer> steps = new ArrayList<>();
        for (int amount : Delivery.AMOUNTS) if (amount < limit) steps.add(amount);
        steps.add(limit);
        return List.copyOf(steps);
    }

    /**
     * The setting after {@code reserve} within {@code limit}, wrapping; {@code up} false goes the other way. A reserve
     * between two settings moves to the nearer one in the direction asked.
     */
    public static int next(int reserve, int limit, boolean up) {
        List<Integer> steps = steps(limit);
        int index = -1;
        if (up) {
            for (int i = 0; i < steps.size() && index < 0; i++) if (steps.get(i) > reserve) index = i;
            return steps.get(index < 0 ? 0 : index);
        }
        for (int i = steps.size() - 1; i >= 0 && index < 0; i--) if (steps.get(i) < reserve) index = i;
        return steps.get(index < 0 ? steps.size() - 1 : index);
    }

    /** {@code reserve} held to {@code limit}. */
    public static int clamp(int reserve, int limit) {
        return Math.clamp(reserve, 0, limit);
    }
}
