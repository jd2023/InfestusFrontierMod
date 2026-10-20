package org.jd.infestusfrontier.logistics;

import net.minecraft.util.StringRepresentable;

/** What one face of a Vascular Junction does. The probe cycles a face through the three in this order. */
public enum Port implements StringRepresentable {
    CLOSED("closed"),
    INTAKE("intake"),
    OUTLET("outlet");

    private final String name;

    Port(String name) {
        this.name = name;
    }

    public Port next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
