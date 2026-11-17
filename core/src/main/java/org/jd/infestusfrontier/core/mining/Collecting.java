package org.jd.infestusfrontier.core.mining;

import java.util.List;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * The fixed limits of Collection Cilia and the reasons they stop. Cilia stand on a Mouth facing a Work Bed and send
 * what the bed holds into the Mouth's network. Every number is a starting value, not measured balance.
 */
public final class Collecting {
    /** Loaded server ticks between two sweeps: one second. */
    public static final int SWEEP_TICKS = 20;
    /** Items one sweep offers to the network, at most. */
    public static final int SWEEP_ITEMS = 16;
    /** Biomass the cilia hold, in BU: one bucket. */
    public static final int BIOMASS_CAPACITY_BU = 1000;
    /** Biomass a sweep that moved something costs, in BU. A sweep must have it before it starts. */
    public static final int SWEEP_BIOMASS_BU = 2;

    /** The cilia do not stand on a loaded organ foundation. */
    public static final Refusal UNROOTED = new Refusal("collection_cilia.unrooted");
    /** The foundation under the cilia is not a Mouth. */
    public static final Refusal NO_MOUTH = new Refusal("collection_cilia.no_mouth");
    /** No readable Work Bed stands in front of the cilia, or its chunk is not loaded. */
    public static final Refusal NO_BED = new Refusal("collection_cilia.no_bed");

    public static final Refusal NO_BIOMASS = new Refusal("collection_cilia.no_biomass");
    /** The network was asked and no Mouth took anything the bed holds. */
    public static final Refusal STORAGE_FULL = new Refusal("collection_cilia.storage_full");

    /** The cilia's own stops, in a fixed order; a screen adds the Mouth's refusals after them. */
    public static final List<Refusal> STOPS = List.of(UNROOTED, NO_MOUTH, NO_BED, NO_BIOMASS, STORAGE_FULL);

    private Collecting() {}

    /**
     * How many of {@code held} items to offer in one sweep, taking slots in order: the first {@link #SWEEP_ITEMS} in
     * all. Each element of the result is the part of the matching slot offered.
     */
    public static int[] offered(int[] held) {
        int[] offered = new int[held.length];
        int left = SWEEP_ITEMS;
        for (int slot = 0; slot < held.length && left > 0; slot++) {
            if (held[slot] < 0) throw new IllegalArgumentException("Negative count");
            offered[slot] = Math.min(held[slot], left);
            left -= offered[slot];
        }
        return offered;
    }
}
