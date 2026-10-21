package org.jd.infestusfrontier.logistics;

import net.minecraft.util.StringRepresentable;

/** What a Mouth does with the store standing on it. */
public enum MouthMode implements StringRepresentable {
    /** The network feeds the store. */
    INTAKE("intake"),
    /** The network takes from the store. */
    OUTPUT("output"),
    /**
     * Both at once, for an organ: the network feeds its ingredients and takes the products it offers. On a plain store
     * the same items would leave as they come, unless the filter tells them apart.
     */
    BOTH("both"),
    /** Neither; the Mouth is still a passage of its network. */
    CLOSED("closed");

    private final String name;

    MouthMode(String name) {
        this.name = name;
    }

    /** The store receives from the network. */
    boolean takes() {
        return this == INTAKE || this == BOTH;
    }

    /** The network takes from the store. */
    boolean gives() {
        return this == OUTPUT || this == BOTH;
    }

    MouthMode next() {
        MouthMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
