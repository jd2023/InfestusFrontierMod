package org.jd.infestusfrontier.core.mineral;

/**
 * The residual state of one Mineral Tailings: the metal it came from and the units of that metal still in it. The
 * Gizzard makes parcels of {@link Grinding#TAILINGS_UNITS}; nothing raises the units and nothing turns them into dust.
 * A value: an out-of-range parcel cannot exist.
 */
public record Parcel(Metal metal, int units) {
    public Parcel {
        if (metal == null) throw new IllegalArgumentException("A parcel names its metal");
        if (units < 1 || units > Grinding.TAILINGS_UNITS)
            throw new IllegalArgumentException(
                    "A parcel holds 1 to " + Grinding.TAILINGS_UNITS + " units, not " + units);
    }

    /** The parcel one grind of {@code metal} leaves. */
    public static Parcel ground(Metal metal) {
        return new Parcel(metal, Grinding.TAILINGS_UNITS);
    }
}
