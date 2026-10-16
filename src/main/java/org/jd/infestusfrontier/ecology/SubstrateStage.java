package org.jd.infestusfrontier.ecology;

import java.util.Optional;
import net.minecraft.util.StringRepresentable;

/** Maturity of one Living Substrate cell. Grows one step per growth tick while it has vigor, never on its own. */
public enum SubstrateStage implements StringRepresentable {
    BASIC("basic"),
    YOUNG("young"),
    MATURE("mature");

    private final String name;

    SubstrateStage(String name) {
        this.name = name;
    }

    /** The stage one treatment later; empty for the last stage. */
    public Optional<SubstrateStage> next() {
        return switch (this) {
            case BASIC -> Optional.of(YOUNG);
            case YOUNG -> Optional.of(MATURE);
            case MATURE -> Optional.empty();
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
