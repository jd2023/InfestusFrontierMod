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
        Bounds frame = new Bounds(78, 14, Sheet.TANK_WELL.width(), Sheet.TANK_WELL.height());
        assertTrue(frame.contains(Sheet.tankColumn(78, 14)));
        assertEquals(new Bounds(80, 16, 16, 52), Sheet.tankColumn(78, 14));
    }

    @Test
    void glyphIndexIsBounded() {
        assertThrows(IllegalArgumentException.class, () -> Sheet.glyph(4));
        assertThrows(IllegalArgumentException.class, () -> Sheet.glyph(-1));
    }
}
