package org.jd.infestusfrontier.core.storage;

/**
 * The view of an item store's kinds (BLOCK_CATALOG T1-09): every kind the store holds is one entry with its whole
 * count, the entries sorted and laid in rows of {@value #COLUMNS}, with one open entry after the last kind for a new
 * kind. {@value #ROWS} rows show at a time; the view starts at a row and never scrolls past the open entry.
 */
public final class Ledger {
    public static final int COLUMNS = 8;
    public static final int ROWS = 3;
    /** Entries shown at a time. */
    public static final int VIEW = COLUMNS * ROWS;

    private Ledger() {}

    /** Rows that {@code kinds} kinds and the open entry after them take. */
    public static int rows(int kinds) {
        if (kinds < 0) throw new IllegalArgumentException("Kinds " + kinds);
        return kinds / COLUMNS + 1;
    }

    /** The last row a view may start at; 0 while everything fits in one view. */
    public static int lastRow(int kinds) {
        return Math.max(0, rows(kinds) - ROWS);
    }

    /** {@code row} kept between the first and the last row. */
    public static int clampRow(int row, int kinds) {
        return Math.clamp(row, 0, lastRow(kinds));
    }

    /** The entry that cell {@code cell} of a view starting at {@code row} shows. */
    public static int entry(int row, int cell) {
        if (row < 0 || cell < 0 || cell >= VIEW) throw new IllegalArgumentException("Row " + row + " cell " + cell);
        return row * COLUMNS + cell;
    }
}
