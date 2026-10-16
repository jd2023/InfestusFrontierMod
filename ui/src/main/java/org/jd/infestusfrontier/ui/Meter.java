package org.jd.infestusfrontier.ui;

/** A value against its maximum, for gauges. Immutable. */
public record Meter(int value, int max) {
    public Meter {
        if (max < 1 || value < 0 || value > max) throw new IllegalArgumentException("Meter out of range");
    }

    /** Pixels of {@code extent} to fill. Only an empty meter fills nothing; only a full meter fills everything. */
    public int filled(int extent) {
        if (extent < 0) throw new IllegalArgumentException("Negative extent");
        if (value == 0 || extent == 0) return 0;
        if (value == max) return extent;
        return Math.clamp((long) value * extent / max, 1, extent - 1);
    }

    /** The bottom part of {@code area} that a vertical gauge paints. */
    public Bounds fillFromBottom(Bounds area) {
        return area.bottomSlice(filled(area.height()));
    }
}
