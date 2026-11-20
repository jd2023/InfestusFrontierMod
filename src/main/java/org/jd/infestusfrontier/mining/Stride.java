package org.jd.infestusfrontier.mining;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import org.jd.infestusfrontier.core.mining.Steer;

/**
 * A way relative to a Tunnel Root head, as its block state shows it to the client: the way its next step is steered,
 * and the way its last step went while it still crawls into its cell, or {@link #REST} once it has settled.
 */
public enum Stride implements StringRepresentable {
    REST,
    STRAIGHT,
    LEFT,
    RIGHT;

    /** The stride of a step steered {@code way}. */
    static Stride of(Steer way) {
        return switch (way) {
            case STRAIGHT -> STRAIGHT;
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
        };
    }

    /** The steering this stride shows; {@link #REST} shows none and reads as straight. */
    Steer steer() {
        return switch (this) {
            case REST, STRAIGHT -> Steer.STRAIGHT;
            case LEFT -> Steer.LEFT;
            case RIGHT -> Steer.RIGHT;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
