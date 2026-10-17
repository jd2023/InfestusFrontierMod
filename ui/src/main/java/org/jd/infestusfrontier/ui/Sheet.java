package org.jd.infestusfrontier.ui;

import java.util.List;

/**
 * Where each part of an organ screen sits on the shared 256x256 GUI sheet {@code textures/gui/organ.png}, and where
 * every screen puts the parts all organs share. The sheet is drawn by {@code art/gen_gui_sheet.py}; the two files
 * change together. Slots sit one pixel inside their well.
 */
public final class Sheet {
    public static final int SIZE = 256;
    /** The whole panel with the 36 player inventory wells and the bud ornament baked in. */
    public static final Bounds PANEL = new Bounds(0, 0, 176, 166);
    /** One well for an organ's own slot; place it at the slot's position minus one. */
    public static final Bounds SLOT = new Bounds(176, 0, 18, 18);
    /** The tank frame; the column goes inside it at {@code +2, +2}. */
    public static final Bounds TANK_WELL = new Bounds(176, 20, 20, 40);

    public static final Bounds TANK_FILL = new Bounds(200, 22, 16, 36);
    public static final Bounds TANK_SURFACE = new Bounds(220, 22, 16, 1);
    public static final Bounds BUTTON = new Bounds(176, 64, 52, 16);
    public static final Bounds BUTTON_HOVER = new Bounds(176, 80, 52, 16);
    public static final Bounds BUTTON_OFF = new Bounds(176, 96, 52, 16);
    public static final Bounds VEIN_EMPTY = new Bounds(176, 112, 44, 12);
    public static final Bounds VEIN_FULL = new Bounds(176, 128, 44, 12);
    /** A low horizontal vessel, filled from the left: water in a bowl. */
    public static final Bounds TROUGH_EMPTY = new Bounds(176, 160, 54, 8);

    public static final Bounds TROUGH_FULL = new Bounds(176, 170, 54, 8);
    /** Eighteen rows of panel body with both frame sides; stacked at {@link #SPLIT_Y} to make a taller panel. */
    public static final Bounds BODY_STRIP = new Bounds(0, 168, 176, 18);
    /** Where the panel splits: rows above hold the organ's parts, rows from here hold the player inventory. */
    public static final int SPLIT_Y = 72;
    /** A menu puts the player's inventory rows here on the standard panel; the hotbar is 58 lower. */
    public static final int INVENTORY_Y = 84;

    public static final int HOTBAR_Y = 142;
    public static final int GLYPH_COUNT = 4;
    private static final int GLYPH_X = 176;
    private static final int GLYPH_Y = 144;
    private static final int GLYPH_SIZE = 9;
    private static final int GLYPH_PITCH = 10;
    /** Flesh-dark ink on the ivory membrane. */
    public static final int INK = 0xFF3E262C;
    /** Ivory text on a flesh button. */
    public static final int IVORY = 0xFFF2ECD8;

    /** The glyphs by meaning. */
    public static final int RING = 0;

    public static final int ARROW = 1;
    public static final int CROSS = 2;
    public static final int BARS = 3;

    /** The status line: a glyph under the title with its words to the right. */
    public static final int STATUS_GLYPH_X = 8;

    public static final int STATUS_GLYPH_Y = 15;
    public static final int STATUS_TEXT_X = 20;
    public static final int STATUS_TEXT_Y = 16;
    /** The status words may run this far; longer text wraps into the tooltip. Ends short of the tank label. */
    public static final int STATUS_TEXT_WIDTH = 106;
    /** Labels sit on this row above the organ's parts. */
    public static final int LABEL_Y = 26;
    /** Top of the parts row: wells, veins and buttons are centered on this 18-pixel band. */
    public static final int PARTS_Y = 34;
    /** The first organ part sits right of the bud ornament. */
    public static final int FIRST_PART_X = 43;
    /** Space between two parts on the row. */
    public static final int GAP = 4;
    /** A menu puts its first slot here, one pixel inside the first well. */
    public static final int SLOT_X = FIRST_PART_X + 1;

    public static final int SLOT_Y = PARTS_Y + 1;
    /** The biomass tank on the right, its label above and its amount below, both right-aligned to the edge. */
    public static final int TANK_X = 150;

    public static final int TANK_Y = 20;
    public static final int TANK_LABEL_Y = 10;
    public static final int TANK_VALUE_Y = 62;
    public static final int RIGHT_EDGE = 168;
    /** No part reaches this column; the tank starts one gap right of it. */
    public static final int PARTS_END = TANK_X - GAP;

    private Sheet() {}

    /** Top of a part {@code height} tall, centered on the parts row. */
    public static int partY(int height) {
        return PARTS_Y + (SLOT.height() - height) / 2;
    }

    /** Height of a panel with {@code strips} body strips under its parts. */
    public static int panelHeight(int strips) {
        return PANEL.height() + strips * BODY_STRIP.height();
    }

    /** Where a menu puts its first player inventory row on a panel with {@code strips} body strips. */
    public static int inventoryY(int strips) {
        return INVENTORY_Y + strips * BODY_STRIP.height();
    }

    public static int hotbarY(int strips) {
        return HOTBAR_Y + strips * BODY_STRIP.height();
    }

    /** The status glyphs in order: ring, arrow, cross, bars. */
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
                TROUGH_EMPTY,
                TROUGH_FULL,
                BODY_STRIP,
                glyph(0),
                glyph(1),
                glyph(2),
                glyph(3));
    }
}
