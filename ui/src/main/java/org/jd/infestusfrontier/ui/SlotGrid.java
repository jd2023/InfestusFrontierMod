package org.jd.infestusfrontier.ui;

/** Rows of wells at one pitch. {@code origin} is the first well; a menu's slot sits one pixel inside each cell. */
public record SlotGrid(Bounds origin, int columns, int rows, int pitch) {
    public SlotGrid {
        if (columns < 1 || rows < 1 || pitch < origin.width()) throw new IllegalArgumentException("Bad grid");
    }

    /** The grid every organ with nine ingredients uses: 3x3 wells from the first part position. */
    public static SlotGrid standard3x3() {
        return new SlotGrid(
                new Bounds(Sheet.FIRST_PART_X, Sheet.PARTS_Y, Sheet.SLOT.width(), Sheet.SLOT.height()), 3, 3, 18);
    }

    public int count() {
        return columns * rows;
    }

    /** Cell {@code index}, row-major from the top left. */
    public Bounds cell(int index) {
        if (index < 0 || index >= count()) throw new IndexOutOfBoundsException("No cell " + index);
        return new Bounds(
                origin.x() + pitch * (index % columns),
                origin.y() + pitch * (index / columns),
                origin.width(),
                origin.height());
    }

    /** The whole grid. */
    public Bounds bounds() {
        return new Bounds(
                origin.x(), origin.y(), pitch * (columns - 1) + origin.width(), pitch * (rows - 1) + origin.height());
    }
}
