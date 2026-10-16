package org.jd.infestusfrontier.ui;

import java.util.List;

/**
 * Where each part of an organ screen sits on the shared 256x256 GUI sheet {@code textures/gui/organ.png}. The sheet
 * is drawn by {@code art/gen_gui_sheet.py}; the two files change together. Slots sit one pixel inside their well.
 */
public final class Sheet {
    public static final int SIZE = 256;
    /** The whole panel with the 36 player inventory wells baked in. */
    public static final Bounds PANEL = new Bounds(0, 0, 176, 166);
    /** One well for an organ's own slot; place it at the slot's position minus one. */
    public static final Bounds SLOT = new Bounds(176, 0, 18, 18);
    /** The tank frame; the column goes inside it at {@code +2, +2}. */
    public static final Bounds TANK_WELL = new Bounds(176, 20, 20, 56);

    public static final Bounds TANK_FILL = new Bounds(200, 22, 16, 52);
    public static final Bounds TANK_SURFACE = new Bounds(220, 22, 16, 1);
    public static final Bounds BUTTON = new Bounds(176, 80, 52, 16);
    public static final Bounds BUTTON_HOVER = new Bounds(176, 96, 52, 16);
    public static final Bounds BUTTON_OFF = new Bounds(176, 112, 52, 16);
    public static final Bounds VEIN_EMPTY = new Bounds(176, 128, 44, 12);
    public static final Bounds VEIN_FULL = new Bounds(176, 144, 44, 12);
    public static final int GLYPH_COUNT = 4;
    private static final int GLYPH_X = 176;
    private static final int GLYPH_Y = 160;
    private static final int GLYPH_SIZE = 9;
    private static final int GLYPH_PITCH = 10;
    /** Flesh-dark ink on the ivory membrane. */
    public static final int INK = 0xFF3E262C;
    /** Ivory text on a flesh button. */
    public static final int IVORY = 0xFFF2ECD8;

    private Sheet() {}

    /** The status glyphs in order: idle ring, working arrow, blocked cross, paused bars. */
    public static Bounds glyph(int index) {
        if (index < 0 || index >= GLYPH_COUNT) throw new IllegalArgumentException("No glyph " + index);
        return new Bounds(GLYPH_X + GLYPH_PITCH * index, GLYPH_Y, GLYPH_SIZE, GLYPH_SIZE);
    }

    /** The column inside a tank frame placed at {@code x, y}. */
    public static Bounds tankColumn(int x, int y) {
        return new Bounds(x + 2, y + 2, TANK_FILL.width(), TANK_FILL.height());
    }

    /** Every sprite, for checks that they fit the sheet and keep apart. */
    public static List<Bounds> sprites() {
        return List.of(
                PANEL,
                SLOT,
                TANK_WELL,
                TANK_FILL,
                TANK_SURFACE,
                BUTTON,
                BUTTON_HOVER,
                BUTTON_OFF,
                VEIN_EMPTY,
                VEIN_FULL,
                glyph(0),
                glyph(1),
                glyph(2),
                glyph(3));
    }
}
