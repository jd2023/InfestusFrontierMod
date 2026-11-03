package org.jd.infestusfrontier.processing;

/**
 * Why a Palm with Rote is doing what it does, in the order the Palm judges it. The ordinal is the screen's synced int;
 * the lang key is {@code gui.infestusfrontier.shaping_palm.status.<name>}.
 */
public enum PalmStatus {
    /** The Palm has not been given Rote. */
    NO_ROTE,
    /** Select has recorded nothing yet. */
    NO_PATTERN,
    /** A cycle is under way. */
    WORKING,
    /** Everything is in place; the Palm waits its turn among the Palms starting this tick. */
    READY,
    /** A pattern slot lacks its ingredient, or holds another item. */
    MISSING,
    /** Fewer than 10 BU. */
    NO_BIOMASS,
    /** The tray cannot hold the result and every remainder. */
    TRAY_FULL,
    /** The recipe or its result is no longer what Select recorded. */
    RECIPE_CHANGED,
    /** The grid needs a restock and the Mouth under the Palm cannot reach the network. */
    NO_SUPPLY,
    /** No organ foundation under the Palm. */
    NO_FOUNDATION,
    /** The saved bytes could not be read. */
    UNREADABLE;

    public static PalmStatus of(int ordinal) {
        PalmStatus[] all = values();
        return ordinal < 0 || ordinal >= all.length ? NO_ROTE : all[ordinal];
    }

    public String key() {
        return "gui.infestusfrontier.shaping_palm.status." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
