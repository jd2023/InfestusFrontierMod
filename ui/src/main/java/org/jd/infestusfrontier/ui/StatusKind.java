package org.jd.infestusfrontier.ui;

/** The four states every organ's status line can show. Each has one glyph on the sheet. */
public enum StatusKind {
    /** Nothing to do and nothing held. */
    IDLE(Sheet.RING),
    /** Working, or holding something that can move. */
    ACTIVE(Sheet.ARROW),
    /** Stopped by a condition the player can fix: full, no foundation. */
    BLOCKED(Sheet.BARS),
    /** Saved data unreadable; the organ keeps it and refuses commands. */
    BROKEN(Sheet.CROSS);

    private final int glyph;

    StatusKind(int glyph) {
        this.glyph = glyph;
    }

    /** Index for {@link Sheet#glyph}. */
    public int glyph() {
        return glyph;
    }
}
