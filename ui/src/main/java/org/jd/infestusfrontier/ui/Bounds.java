package org.jd.infestusfrontier.ui;

/** An axis-aligned rectangle in GUI pixels. Immutable. */
public record Bounds(int x, int y, int width, int height) {
    public Bounds {
        if (width < 0 || height < 0) throw new IllegalArgumentException("Negative size");
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(int px, int py) {
        return px >= x && py >= y && px < right() && py < bottom();
    }

    public boolean contains(Bounds other) {
        return other.x >= x && other.y >= y && other.right() <= right() && other.bottom() <= bottom();
    }

    public boolean overlaps(Bounds other) {
        return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
    }

    /** This rectangle moved so that its center sits on the center of {@code area}. */
    public Bounds centeredIn(Bounds area) {
        return new Bounds(area.x + (area.width - width) / 2, area.y + (area.height - height) / 2, width, height);
    }
}
