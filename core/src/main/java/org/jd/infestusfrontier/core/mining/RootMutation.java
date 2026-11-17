package org.jd.infestusfrontier.core.mining;

import java.util.Optional;

/**
 * A permanent mutation of a Tunnel Root head. It permits an operation; it never supplies the organs, the materials or
 * the construction stock that operation uses. A head with {@link #STAIRS} is a Descending Rootstock. Every cost is a
 * starting value, not measured balance.
 */
public enum RootMutation {
    /** The route may descend one block a step. Turns a Tunnel Root head into a Descending Rootstock. */
    STAIRS("stairs", 500),
    /** The section is 3x3 instead of 1x2. Only a Rootstock takes it. */
    WIDER_SECTION("wider_section", 1000);

    private final String id;
    private final int biomassUnits;

    RootMutation(String id, int biomassUnits) {
        this.id = id;
        this.biomassUnits = biomassUnits;
    }

    /** The saved and translated name. */
    public String id() {
        return id;
    }

    /** The biomass applying it costs, in BU, paid from the head's own tank. */
    public int biomassUnits() {
        return biomassUnits;
    }

    /** The mutation a head must already have to take this one, or empty when any head takes it. */
    public Optional<RootMutation> requires() {
        return this == WIDER_SECTION ? Optional.of(STAIRS) : Optional.empty();
    }

    /** The mutation saved as {@code id}, or empty when none is. */
    public static Optional<RootMutation> byId(String id) {
        for (RootMutation mutation : values()) if (mutation.id.equals(id)) return Optional.of(mutation);
        return Optional.empty();
    }
}
