package org.jd.infestusfrontier.core.foundation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TickQuotaTest {
    @Test
    void enforcesTheSameTickCeiling() {
        var quota = new TickQuota(2);
        assertTrue(quota.take(8));
        assertTrue(quota.take(8));
        for (int i = 0; i < 1000; i++) assertFalse(quota.take(8));
    }

    @Test
    void resetsOnNewTimeIncludingClockRewindAndExtremes() {
        var quota = new TickQuota(1);
        for (long tick : new long[] {Long.MIN_VALUE, 0, 10, 9, Long.MAX_VALUE}) {
            assertTrue(quota.take(tick));
            assertFalse(quota.take(tick));
        }
    }

    @Test
    void rejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> new TickQuota(0));
        assertThrows(IllegalArgumentException.class, () -> new TickQuota(-1));
        assertThrows(IllegalArgumentException.class, () -> new TickQuota(1).grant(1, -1));
    }

    @Test
    void grantsUpToTheLimitWithinOneTick() {
        var quota = new TickQuota(10);
        assertEquals(6, quota.grant(1, 6));
        assertEquals(4, quota.grant(1, 6), "only what is left");
        assertEquals(0, quota.grant(1, 1), "spent");
        assertFalse(quota.take(1));
    }

    @Test
    void aNewTickRestoresTheGrant() {
        var quota = new TickQuota(3);
        assertEquals(3, quota.grant(5, 3));
        assertEquals(3, quota.grant(6, 3), "the next tick");
        assertEquals(3, quota.grant(2, 3), "a clock rewind is a new tick too");
    }

    @Test
    void takeIsAGrantOfOne() {
        var quota = new TickQuota(2);
        assertTrue(quota.take(1));
        assertEquals(1, quota.grant(1, 5));
        assertFalse(quota.take(1));
    }
}
