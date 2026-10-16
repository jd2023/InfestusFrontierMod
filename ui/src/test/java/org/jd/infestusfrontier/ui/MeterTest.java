package org.jd.infestusfrontier.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MeterTest {
    @Test
    void emptyAndFullAreExact() {
        assertEquals(0, new Meter(0, 4000).filled(52));
        assertEquals(52, new Meter(4000, 4000).filled(52));
    }

    @Test
    void partialNeverLooksEmptyOrFull() {
        assertEquals(1, new Meter(1, 4000).filled(52));
        assertEquals(51, new Meter(3999, 4000).filled(52));
        assertEquals(26, new Meter(2000, 4000).filled(52));
    }

    @Test
    void paintsFromTheBottom() {
        assertEquals(new Bounds(10, 46, 16, 26), new Meter(2000, 4000).fillFromBottom(new Bounds(10, 20, 16, 52)));
    }

    @Test
    void rejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new Meter(5, 4));
        assertThrows(IllegalArgumentException.class, () -> new Meter(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Meter(-1, 4));
    }
}
