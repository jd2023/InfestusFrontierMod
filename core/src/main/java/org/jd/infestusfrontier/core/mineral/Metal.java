package org.jd.infestusfrontier.core.mineral;

import java.util.Optional;

/** The three metals the automated mineral workshop handles. The id is the one dust, Tailings and recipes name. */
public enum Metal {
    IRON("iron"),
    GOLD("gold"),
    COPPER("copper");

    private final String id;

    Metal(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<Metal> of(String id) {
        for (Metal metal : values()) if (metal.id.equals(id)) return Optional.of(metal);
        return Optional.empty();
    }
}
