package org.jd.infestusfrontier.core.equipment;

/**
 * The living tools' arithmetic (ITEM_CATALOG I015, I016): a dormant bone tool wears out fast; woken, it lasts longer
 * and mends itself from the biomass it carries; a Contractile graft lets it break the blocks around the one it mines,
 * each for a price from the same reserve.
 */
public final class LivingTool {
    /** Blocks a dormant tool breaks before it is gone. */
    public static final int DORMANT_USES = 96;
    /** Blocks a living tool breaks before it is gone, if it is never fed. */
    public static final int LIVING_USES = 256;
    /** Points of wear a carried living tool mends in one second, one BU each. */
    public static final int MEND_PER_SECOND = 4;
    /** Blocks around the mined one a Contractile tool breaks at most: the 3x3 face minus its centre. */
    public static final int SPREAD = 8;
    /** BU a Contractile tool pays for each block it breaks beside the mined one. */
    public static final int SPREAD_COST = 2;

    private LivingTool() {}

    /** The points a tool worn by {@code damage} mends this second from a reserve of {@code stored} BU. */
    public static int mend(int damage, int stored) {
        return Math.max(0, Math.min(MEND_PER_SECOND, Math.min(damage, stored)));
    }

    /** How many blocks beside the mined one a reserve of {@code stored} BU pays for, at most {@link #SPREAD}. */
    public static int spreads(int stored) {
        return Math.max(0, Math.min(SPREAD, stored / SPREAD_COST));
    }
}
