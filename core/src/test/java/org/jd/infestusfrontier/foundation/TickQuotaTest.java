package org.jd.infestusfrontier.foundation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TickQuotaTest {
    @Test void enforcesTheSameTickCeiling() {
        var quota = new TickQuota(2);
        assertTrue(quota.take(8));
        assertTrue(quota.take(8));
        for (int i = 0; i < 1000; i++) assertFalse(quota.take(8));
    }
    @Test void resetsOnNewTimeIncludingClockRewindAndExtremes() {
        var quota = new TickQuota(1);
        for (long tick : new long[] {Long.MIN_VALUE, 0, 10, 9, Long.MAX_VALUE}) {
            assertTrue(quota.take(tick));
            assertFalse(quota.take(tick));
        }
    }
    @Test void rejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> new TickQuota(0));
        assertThrows(IllegalArgumentException.class, () -> new TickQuota(-1));
    }
}
