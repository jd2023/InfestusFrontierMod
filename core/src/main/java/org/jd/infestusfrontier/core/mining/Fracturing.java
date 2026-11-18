package org.jd.infestusfrontier.core.mining;

import java.util.List;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * The fixed limits of a Fracture Jaw and its Work Bed, and the reasons a jaw stops. A jaw stands still and cuts the one
 * block in front of it into the Work Bed behind it; when a Descending Rootstock stands in front, the block that
 * Rootstock orders instead. Every number is a starting value, not measured balance.
 */
public final class Fracturing {
    /** Loaded server ticks between two cuts: three seconds. */
    public static final int CUT_TICKS = 60;
    /** Loaded ticks a stopped jaw waits before it looks again. */
    public static final int RETRY_TICKS = 20;
    /** Biomass the jaw holds, in BU: two buckets, so a bucket fits before the tank runs dry. */
    public static final int BIOMASS_CAPACITY_BU = 2000;
    /** Biomass one cut costs, in BU. */
    public static final int CUT_BIOMASS_BU = 10;
    /** Cuts the Fracture Jaws of one dimension make in one tick, together; a jaw over the limit tries next tick. */
    public static final int CUTS_PER_TICK = 8;
    /** Slots of a Work Bed. Each holds one stack. */
    public static final int BED_SLOTS = 9;

    /** The jaw does not stand on a loaded organ foundation. */
    public static final Refusal UNROOTED = new Refusal("fracture_jaw.unrooted");
    /** No readable Work Bed stands behind the jaw. */
    public static final Refusal NO_BED = new Refusal("fracture_jaw.no_bed");

    public static final Refusal NO_BIOMASS = new Refusal("fracture_jaw.no_biomass");
    /** A chunk the cut reads or changes is not loaded. */
    public static final Refusal UNLOADED = new Refusal("fracture_jaw.unloaded");
    /** Nothing stands in front of the jaw. */
    public static final Refusal NOTHING = new Refusal("fracture_jaw.nothing");
    /** The block in front needs a better pickaxe than stone, or cannot be broken at all. */
    public static final Refusal TOO_HARD = new Refusal("fracture_jaw.too_hard");
    /** The block in front holds a block entity, or is a piston arm. */
    public static final Refusal UNSUPPORTED = new Refusal("fracture_jaw.unsupported");
    /** The block in front holds fluid. */
    public static final Refusal FLUID = new Refusal("fracture_jaw.fluid");
    /** A fragile block, such as a torch or a plant, touches the block in front and could fall off loose. */
    public static final Refusal FRAGILE = new Refusal("fracture_jaw.fragile");
    /** The drops of the cut do not fit the Work Bed. */
    public static final Refusal BED_FULL = new Refusal("fracture_jaw.bed_full");
    /** The Rootstock in front orders no cut now: its screen says why. */
    public static final Refusal NO_ORDER = new Refusal("fracture_jaw.no_order");

    /** Every stop, in a fixed order: a screen names one by its index. */
    public static final List<Refusal> STOPS = List.of(
            UNROOTED, NO_BED, NO_BIOMASS, UNLOADED, NOTHING, TOO_HARD, UNSUPPORTED, FLUID, FRAGILE, BED_FULL, NO_ORDER);

    private Fracturing() {}
}
