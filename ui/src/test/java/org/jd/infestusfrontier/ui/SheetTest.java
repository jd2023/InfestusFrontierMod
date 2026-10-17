package org.jd.infestusfrontier.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class SheetTest {
    @Test
    void everySpriteFitsTheSheet() {
        Bounds sheet = new Bounds(0, 0, Sheet.SIZE, Sheet.SIZE);
        for (Bounds sprite : Sheet.sprites()) assertTrue(sheet.contains(sprite), sprite.toString());
    }

    @Test
    void spritesKeepApart() {
        List<Bounds> sprites = Sheet.sprites();
        for (int a = 0; a < sprites.size(); a++)
            for (int b = a + 1; b < sprites.size(); b++)
                assertFalse(sprites.get(a).overlaps(sprites.get(b)), sprites.get(a) + " overlaps " + sprites.get(b));
    }

    @Test
    void tankColumnSitsInsideItsFrame() {
        Bounds frame = new Bounds(Sheet.TANK_X, Sheet.TANK_Y, Sheet.TANK_WELL.width(), Sheet.TANK_WELL.height());
        assertTrue(frame.contains(Sheet.tankColumn(Sheet.TANK_X, Sheet.TANK_Y)));
        assertEquals(new Bounds(152, 22, 16, 36), Sheet.tankColumn(Sheet.TANK_X, Sheet.TANK_Y));
    }

    @Test
    void sharedLayoutStaysInsideThePanelAboveTheInventory() {
        Bounds top = new Bounds(0, 0, Sheet.PANEL.width(), 72);
        assertTrue(top.contains(
                new Bounds(Sheet.TANK_X, Sheet.TANK_Y, Sheet.TANK_WELL.width(), Sheet.TANK_WELL.height())));
        assertTrue(top.contains(new Bounds(Sheet.STATUS_GLYPH_X, Sheet.STATUS_GLYPH_Y, 9, 9)));
        assertTrue(Sheet.TANK_VALUE_Y + 9 <= 72);
        assertTrue(Sheet.RIGHT_EDGE <= Sheet.TANK_X + Sheet.TANK_WELL.width());
        assertTrue(Sheet.PARTS_Y + Sheet.SLOT.height() <= 72);
    }

    @Test
    void statusWordsStopBeforeTheTankLabel() {
        Bounds words = new Bounds(Sheet.STATUS_TEXT_X, Sheet.STATUS_TEXT_Y, Sheet.STATUS_TEXT_WIDTH, 9);
        Bounds label = new Bounds(Sheet.RIGHT_EDGE - 40, Sheet.TANK_LABEL_Y, 40, 9);
        assertFalse(words.overlaps(label));
    }

    @Test
    void glyphIndexIsBounded() {
        assertThrows(IllegalArgumentException.class, () -> Sheet.glyph(4));
        assertThrows(IllegalArgumentException.class, () -> Sheet.glyph(-1));
    }
}
