package org.jd.infestusfrontier.core.vigil;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TerritoryTest {
    @Test
    void everySelectionKeepsTheNestAndChargesEveryChunk() {
        for (int mask = 0; mask <= Territory.ALL; mask++) {
            if ((mask & Territory.CENTRE) == 0) continue;
            Territory territory = new Territory(mask);
            assertEquals(Integer.bitCount(mask) * 10, territory.payment());
            for (int i = 0; i < 9; i++) {
                assertTrue(territory.toggle(i).includes(4));
                assertEquals(territory, territory.toggle(i).toggle(i));
            }
        }
    }

    @Test
    void bothSharedCapsRefuseTheFirstExcessChunk() {
        Territory all = new Territory(Territory.ALL);
        assertTrue(all.fits(18, 45));
        assertFalse(all.fits(19, 45));
        assertFalse(all.fits(18, 46));
        assertFalse(all.fits(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(27, Territory.DIMENSION_CHUNKS);
        assertEquals(54, Territory.SERVER_CHUNKS);
    }

    @Test
    void onlyRealFeedingPartsExpandTheAllowance() {
        for (int feeders = 0; feeders <= 4; feeders++) assertEquals(1 + feeders * 2, Territory.capacity(feeders));
        assertThrows(IllegalArgumentException.class, () -> Territory.capacity(5));
        assertThrows(IllegalArgumentException.class, () -> new Territory(0));
        assertThrows(IllegalArgumentException.class, () -> new Territory(1023));
    }
}
