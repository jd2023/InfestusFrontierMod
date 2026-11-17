package org.jd.infestusfrontier.core.mining;

import java.util.List;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * The fixed limits of a Tunnel Root and the reasons it stops. A step cuts the two blocks ahead of the head, lays one
 * root cell in the floor under the cut and moves the head into it. Every number is a starting value, not measured
 * balance.
 */
public final class Tunnelling {
    /** Loaded server ticks between two steps: twenty seconds. */
    public static final int STEP_TICKS = 400;
    /** Loaded ticks a stopped head waits before it looks again, unless the player changes its supplies first. */
    public static final int RETRY_TICKS = 20;
    /** Root cells one head lays in its whole life: the length of its root. */
    public static final int MAX_CELLS = 64;
    /** Root Segments the head holds. */
    public static final int MAX_SEGMENTS = 16;
    /** Biomass the head holds, in BU: two buckets, so a bucket fits before the tank runs dry. */
    public static final int BIOMASS_CAPACITY_BU = 2000;
    /** Biomass one step costs, in BU. */
    public static final int STEP_BIOMASS_BU = 25;
    /** Items the output buffer holds in all: one stack. */
    public static final int OUTPUT_ITEMS = 64;
    /** Kinds of item the output buffer keeps apart, so a cut of two different blocks fits an empty buffer. */
    public static final int OUTPUT_SLOTS = 4;

    /** The floor under the head is neither a root cell nor an organ foundation. */
    public static final Refusal UNROOTED = new Refusal("tunnel_root.unrooted");
    /** The root has all its cells. */
    public static final Refusal FULL_LENGTH = new Refusal("tunnel_root.full_length");

    public static final Refusal NO_SEGMENT = new Refusal("tunnel_root.no_segment");
    public static final Refusal NO_BIOMASS = new Refusal("tunnel_root.no_biomass");
    /** A chunk the step reads or changes is not loaded. */
    public static final Refusal UNLOADED = new Refusal("tunnel_root.unloaded");
    /** A block ahead needs a better pickaxe than stone, or cannot be broken at all. */
    public static final Refusal TOO_HARD = new Refusal("tunnel_root.too_hard");
    /** A block ahead holds a block entity, or the cut leaves the world. */
    public static final Refusal UNSUPPORTED = new Refusal("tunnel_root.unsupported");
    /** Fluid is in the cut or touches it. */
    public static final Refusal FLUID = new Refusal("tunnel_root.fluid");
    /** The floor under the cut is not plain ground a root cell may replace. */
    public static final Refusal NO_FLOOR = new Refusal("tunnel_root.no_floor");
    /** A block that falls stands on top of the cut. */
    public static final Refusal FALLING = new Refusal("tunnel_root.falling");
    /** The drops of the cut do not fit the output buffer. */
    public static final Refusal OUTPUT_FULL = new Refusal("tunnel_root.output_full");
    /** Loading by hand: the head already holds all the segments it can. */
    public static final Refusal SEGMENTS_FULL = new Refusal("tunnel_root.segments_full");

    /** Applying a construct: the head already has that mutation. */
    public static final Refusal MUTATION_INSTALLED = new Refusal("tunnel_root.mutation_installed");
    /** Applying a construct: the mutation needs a Rootstock, a head with Stairs. */
    public static final Refusal NOT_ROOTSTOCK = new Refusal("tunnel_root.not_rootstock");
    /** Applying a construct: the head's tank holds less biomass than the mutation costs. */
    public static final Refusal MUTATION_BIOMASS = new Refusal("tunnel_root.mutation_biomass");
    /** Applying a construct: only a Tunnel Root head takes it. */
    public static final Refusal NOT_A_HEAD = new Refusal("tunnel_root.not_a_head");

    /** Every stop, in a fixed order: a screen names one by its index. */
    public static final List<Refusal> STOPS = List.of(
            UNROOTED,
            FULL_LENGTH,
            NO_SEGMENT,
            NO_BIOMASS,
            UNLOADED,
            TOO_HARD,
            UNSUPPORTED,
            FLUID,
            NO_FLOOR,
            FALLING,
            OUTPUT_FULL);

    private Tunnelling() {}

    /** True when {@code incoming} more items fit a buffer holding {@code held}. */
    public static boolean fits(int held, int incoming) {
        if (held < 0 || incoming < 0) throw new IllegalArgumentException("Negative count");
        return Math.addExact(held, incoming) <= OUTPUT_ITEMS;
    }
}
