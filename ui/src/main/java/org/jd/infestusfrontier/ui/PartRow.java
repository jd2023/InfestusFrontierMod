package org.jd.infestusfrontier.ui;

/**
 * Places an organ's parts left to right on the parts row with one gap between them, and refuses a part that would
 * reach the limit. Mutable; one per screen build.
 */
public final class PartRow {
    private final int gap;
    private final int limit;
    private int next;

    public PartRow(int start, int gap, int limit) {
        if (gap < 0 || start > limit) throw new IllegalArgumentException("Bad row");
        this.gap = gap;
        this.limit = limit;
        this.next = start;
    }

    /** The row every organ screen uses: from the first part to the biomass tank. */
    public static PartRow standard() {
        return new PartRow(Sheet.FIRST_PART_X, Sheet.GAP, Sheet.PARTS_END);
    }

    /** The x of the next part, {@code width} wide. Throws when the part would reach the limit. */
    public int place(int width) {
        if (width < 0) throw new IllegalArgumentException("Negative width");
        int x = next;
        if (x + width > limit)
            throw new IllegalStateException("A part " + width + " wide at " + x + " reaches " + limit);
        next = x + width + gap;
        return x;
    }

    /** Where the next part would start. */
    public int cursor() {
        return next;
    }
}
