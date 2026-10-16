package org.jd.infestusfrontier.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BoundsTest {
    @Test
    void containsIsHalfOpen() {
        var b = new Bounds(10, 20, 5, 5);
        assertTrue(b.contains(10, 20));
        assertTrue(b.contains(14, 24));
        assertFalse(b.contains(15, 24));
        assertFalse(b.contains(14, 25));
        assertFalse(b.contains(9, 20));
    }

    @Test
    void overlapNeedsSharedArea() {
        var a = new Bounds(0, 0, 10, 10);
        assertTrue(a.overlaps(new Bounds(9, 9, 10, 10)));
        assertFalse(a.overlaps(new Bounds(10, 0, 10, 10)));
        assertFalse(a.overlaps(new Bounds(0, 10, 10, 10)));
        assertTrue(a.contains(new Bounds(0, 0, 10, 10)));
        assertFalse(a.contains(new Bounds(1, 1, 10, 10)));
    }

    @Test
    void centeringKeepsSizeAndStaysInsideALargerArea() {
        var panel = new Bounds(0, 0, 176, 166).centeredIn(new Bounds(0, 0, 427, 240));
        assertEquals(new Bounds(125, 37, 176, 166), panel);
        assertTrue(new Bounds(0, 0, 427, 240).contains(panel));
    }

    @Test
    void rejectsNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> new Bounds(0, 0, -1, 0));
    }

    @Test
    void bottomSliceKeepsTheBottomEdge() {
        assertEquals(new Bounds(10, 62, 16, 10), new Bounds(10, 20, 16, 52).bottomSlice(10));
        assertEquals(new Bounds(10, 72, 16, 0), new Bounds(10, 20, 16, 52).bottomSlice(0));
        assertThrows(IllegalArgumentException.class, () -> new Bounds(10, 20, 16, 52).bottomSlice(53));
    }
}
