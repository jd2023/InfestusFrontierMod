package org.jd.infestusfrontier.ui;

import java.util.List;

/**
 * Where each part of an organ screen sits on the shared 256x256 GUI sheet {@code textures/gui/organ.png}, and where
 * every screen puts the parts all organs share. The sheet is drawn by {@code art/gen_gui_sheet.py}; the two files
 * change together. Slots sit one pixel inside their well.
 */
public final class Sheet {
    public static final int SIZE = 256;
    /** The whole panel with the 36 player inventory wells and an empty left column for the organ's emblem. */
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
    /** The organ emblems: 32x32, in two rows of seven under the body strip. */
    public static final int EMBLEM_COUNT = 8;

    private static final int EMBLEMS_PER_ROW = 7;

    private static final int EMBLEM_SHEET_X = 2;
    private static final int EMBLEM_SHEET_Y = 190;
    private static final int EMBLEM_SIZE = 32;
    private static final int EMBLEM_PITCH = 34;
    /** Flesh-dark ink on the ivory membrane. */
    public static final int INK = 0xFF3E262C;
    /** Ivory text on a flesh button. */
    public static final int IVORY = 0xFFF2ECD8;

    /** The emblems by organ. */
    public static final int SAC_EMBLEM = 0;

    public static final int BLADDER_EMBLEM = 1;
    public static final int BOWL_EMBLEM = 2;
    public static final int RACK_EMBLEM = 3;
    public static final int LOOM_EMBLEM = 4;
    public static final int CRADLE_EMBLEM = 5;
    public static final int FURNACE_EMBLEM = 6;
    public static final int CYST_EMBLEM = 7;

    /** The panel's left margin; the emblem column starts here. */
    public static final int LEFT_X = 8;
    /** An organ's emblem sits in the left column, level with the parts row. */
    public static final int EMBLEM_X = LEFT_X;

    public static final int EMBLEM_Y = 27;
    /** Labels sit on this row above the organ's parts. */
    public static final int LABEL_Y = 26;
    /** Top of the parts row: wells, veins and buttons are centered on this 18-pixel band. */
    public static final int PARTS_Y = 34;
    /** The first organ part sits right of the emblem column. */
    public static final int FIRST_PART_X = 43;
    /** Space between two parts on the row. */
    public static final int GAP = 4;
    /** A menu puts its first slot here, one pixel inside the first well. */
    public static final int SLOT_X = FIRST_PART_X + 1;

    public static final int SLOT_Y = PARTS_Y + 1;
    /** A second row of parts on a tall panel: its labels and its wells sit one well and one label lower. */
    public static final int ROW2_LABEL_Y = LABEL_Y + 28;

    public static final int ROW2_Y = PARTS_Y + 28;
    public static final int ROW2_SLOT_Y = ROW2_Y + 1;
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

    /** The emblem sprites in order: Sac, Bladder, Bowl, Rack, Loom, Cradle, Furnace, then Cyst on the second row. */
    public static Bounds emblem(int index) {
        if (index < 0 || index >= EMBLEM_COUNT) throw new IllegalArgumentException("No emblem " + index);
        return new Bounds(
                EMBLEM_SHEET_X + EMBLEM_PITCH * (index % EMBLEMS_PER_ROW),
                EMBLEM_SHEET_Y + EMBLEM_PITCH * (index / EMBLEMS_PER_ROW),
                EMBLEM_SIZE,
                EMBLEM_SIZE);
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
                emblem(0),
                emblem(1),
                emblem(2),
                emblem(3),
                emblem(4),
                emblem(5),
                emblem(6),
                emblem(7));
    }
}
