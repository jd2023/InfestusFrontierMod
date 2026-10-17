package org.jd.infestusfrontier.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SlotGridTest {
    @Test
    void cellsRunRowMajorFromTheOrigin() {
        SlotGrid grid = SlotGrid.standard3x3();
        assertEquals(new Bounds(43, 34, 18, 18), grid.cell(0));
        assertEquals(new Bounds(61, 52, 18, 18), grid.cell(4));
        assertEquals(new Bounds(79, 70, 18, 18), grid.cell(8));
        assertEquals(9, grid.count());
        assertThrows(IndexOutOfBoundsException.class, () -> grid.cell(9));
    }

    @Test
    void theFirstSlotIsTheMenuSlotPosition() {
        Bounds first = SlotGrid.standard3x3().cell(0);
        assertEquals(Sheet.SLOT_X, first.x() + 1);
        assertEquals(Sheet.SLOT_Y, first.y() + 1);
    }

    @Test
    void aNineWellGridNeedsThreeBodyStrips() {
        Bounds grid = SlotGrid.standard3x3().bounds();
        assertEquals(new Bounds(43, 34, 54, 54), grid);
        assertTrue(grid.bottom() > Sheet.SPLIT_Y, "does not fit the standard panel");
        assertTrue(grid.bottom() + Sheet.GAP + Sheet.BUTTON.height() <= Sheet.SPLIT_Y + 3 * Sheet.BODY_STRIP.height());
        assertEquals(138, Sheet.inventoryY(3));
        assertEquals(196, Sheet.hotbarY(3));
        assertEquals(220, Sheet.panelHeight(3));
    }
}
