package org.jd.infestusfrontier.storage;

import net.minecraft.util.StringRepresentable;

/** The look of one Capsule Stem cell: the planted root, a trunk cell, the bud at a trunk's top, or a fork's branch. */
public enum StemPart implements StringRepresentable {
    ROOT("root"),
    TRUNK("trunk"),
    BUD("bud"),
    BRANCH("branch");

    private final String name;

    StemPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
