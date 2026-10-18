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
        Bounds emblem = new Bounds(
                Sheet.EMBLEM_X,
                Sheet.EMBLEM_Y,
                Sheet.emblem(0).width(),
                Sheet.emblem(0).height());
        assertTrue(top.contains(emblem));
        assertTrue(emblem.right() <= Sheet.FIRST_PART_X);
        assertTrue(Sheet.TANK_VALUE_Y + 9 <= 72);
        assertTrue(Sheet.RIGHT_EDGE <= Sheet.TANK_X + Sheet.TANK_WELL.width());
        assertTrue(Sheet.PARTS_Y + Sheet.SLOT.height() <= 72);
    }

    @Test
    void emblemIndexIsBounded() {
        assertThrows(IllegalArgumentException.class, () -> Sheet.emblem(Sheet.EMBLEM_COUNT));
        assertThrows(IllegalArgumentException.class, () -> Sheet.emblem(-1));
    }
}
