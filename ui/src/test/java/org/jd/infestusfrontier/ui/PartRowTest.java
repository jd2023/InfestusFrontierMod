package org.jd.infestusfrontier.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PartRowTest {
    @Test
    void firstPartIsTheWellAroundTheMenuSlot() {
        PartRow row = PartRow.standard();
        assertEquals(Sheet.SLOT_X - 1, row.place(Sheet.SLOT.width()));
        assertEquals(Sheet.FIRST_PART_X + Sheet.SLOT.width() + Sheet.GAP, row.cursor());
    }

    @Test
    void aWellAndAVeinOrAButtonStayLeftOfTheTank() {
        PartRow sac = PartRow.standard();
        sac.place(Sheet.SLOT.width());
        int vein = sac.place(Sheet.VEIN_EMPTY.width());
        assertTrue(vein + Sheet.VEIN_EMPTY.width() <= Sheet.PARTS_END);
        PartRow bladder = PartRow.standard();
        bladder.place(Sheet.SLOT.width());
        int button = bladder.place(Sheet.BUTTON.width());
        assertTrue(button + Sheet.BUTTON.width() <= Sheet.PARTS_END);
    }

    @Test
    void refusesAPartThatReachesTheTank() {
        PartRow row = PartRow.standard();
        row.place(Sheet.SLOT.width());
        row.place(Sheet.VEIN_EMPTY.width());
        assertThrows(IllegalStateException.class, () -> row.place(Sheet.BUTTON.width()));
    }

    @Test
    void partsAreCenteredOnTheRow() {
        assertEquals(Sheet.PARTS_Y, Sheet.partY(Sheet.SLOT.height()));
        assertEquals(Sheet.PARTS_Y + 3, Sheet.partY(Sheet.VEIN_EMPTY.height()));
        assertEquals(Sheet.PARTS_Y + 1, Sheet.partY(Sheet.BUTTON.height()));
    }

    @Test
    void everyStatusKindHasItsOwnGlyph() {
        long distinct = java.util.Arrays.stream(StatusKind.values())
                .mapToInt(StatusKind::glyph)
                .distinct()
                .count();
        assertEquals(StatusKind.values().length, distinct);
        assertEquals(Sheet.GLYPH_COUNT, StatusKind.values().length);
    }
}
