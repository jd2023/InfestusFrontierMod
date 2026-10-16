package org.jd.infestusfrontier.core.foundation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BiomassTest {
    @Test
    void arithmeticIsChecked() {
        assertEquals(Biomass.of(150), Biomass.of(100).plus(Biomass.of(50)));
        assertEquals(Biomass.of(50), Biomass.of(100).minus(Biomass.of(50)));
        assertThrows(
                ArithmeticException.class, () -> Biomass.of(Integer.MAX_VALUE).plus(Biomass.of(1)));
        assertThrows(IllegalArgumentException.class, () -> Biomass.of(10).minus(Biomass.of(11)));
        assertThrows(IllegalArgumentException.class, () -> Biomass.of(-1));
    }

    @Test
    void comparesByUnits() {
        assertTrue(Biomass.of(5).atLeast(Biomass.of(5)));
        assertFalse(Biomass.of(4).atLeast(Biomass.of(5)));
        assertEquals(Biomass.of(4), Biomass.of(4).min(Biomass.of(9)));
        assertTrue(Biomass.ZERO.isZero());
        assertEquals(1000, Biomass.BUCKET.units());
    }
}
