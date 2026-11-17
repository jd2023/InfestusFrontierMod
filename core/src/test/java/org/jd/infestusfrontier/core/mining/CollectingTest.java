package org.jd.infestusfrontier.core.mining;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CollectingTest {
    @Test
    void aSweepOffersSlotsInOrderUpToItsLimit() {
        assertArrayEquals(new int[] {10, 6, 0}, Collecting.offered(new int[] {10, 64, 3}));
        assertArrayEquals(new int[] {0, 3, 2}, Collecting.offered(new int[] {0, 3, 2}));
    }

    @Test
    void aSweepNeverOffersMoreThanItsLimitInAll() {
        int[] offered = Collecting.offered(new int[] {64, 64, 64, 64, 64, 64, 64, 64, 64});
        int total = 0;
        for (int count : offered) total += count;
        assertEquals(Collecting.SWEEP_ITEMS, total);
    }

    @Test
    void anEmptyBedIsOfferedNothing() {
        assertArrayEquals(new int[9], Collecting.offered(new int[9]));
    }

    @Test
    void negativeCountsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> Collecting.offered(new int[] {-1}));
    }

    @Test
    void historyCountsStopAtTheirLargestValue() {
        assertEquals(7, Counts.more(3, 4));
        assertEquals(Integer.MAX_VALUE, Counts.more(Integer.MAX_VALUE - 1, 5));
        assertThrows(IllegalArgumentException.class, () -> Counts.more(-1, 1));
    }

    @Test
    void everyStopHasItsOwnKeyUnderTheCilia() {
        assertEquals(
                Collecting.STOPS.size(),
                Collecting.STOPS.stream().map(stop -> stop.key()).distinct().count());
        assertTrue(Collecting.STOPS.stream().allMatch(stop -> stop.key().startsWith("collection_cilia.")));
    }
}
