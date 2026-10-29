package org.jd.infestusfrontier.core.defense;

/**
 * The Spine Sentry's arithmetic (BLOCK_CATALOG T2-30): a rooted lash that strikes one hostile within reach each
 * pulse, pays biomass for every strike, keeps what its kills drop in a small store, and rests while it cannot pay or
 * cannot keep.
 */
public final class Sentry {
    /** Blocks from the organ, on each axis, that a target may stand within. */
    public static final int REACH = 6;
    /** Blocks above or below the organ that a target may stand within. */
    public static final int RISE = 3;
    /** Ticks between strikes while a target is in reach. */
    public static final int PULSE_TICKS = 20;
    /** Ticks between looks while there is nothing to strike or the organ cannot. */
    public static final int RETRY_TICKS = 40;
    /** Damage of one strike: three hearts, an iron sword's. */
    public static final float DAMAGE = 6.0F;
    /** BU one strike costs. */
    public static final int COST = 10;
    /** BU the tank holds: a hundred strikes. */
    public static final int CAPACITY = 1000;
    /** Slots of the store the kills' drops go to. */
    public static final int STORE_SLOTS = 8;

    public static final int SCAN_LIMIT = 128;
    public static final int SIGHT_LIMIT = 16;

    private Sentry() {}

    /** Both defenders need enough biomass for their shot and at least one slot with room. */
    public static boolean works(int stored, int availableSlots, int cost) {
        return stored >= cost && availableSlots > 0;
    }

    /** Whether a point {@code dx, dy, dz} from the organ's centre lies within reach. */
    public static boolean inReach(double dx, double dy, double dz) {
        return Math.abs(dx) <= REACH + 0.5 && Math.abs(dz) <= REACH + 0.5 && Math.abs(dy) <= RISE + 0.5;
    }
}
