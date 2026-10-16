package org.jd.infestusfrontier.core.foundation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WindowQuotaTest {
    @Test
    void allowsTheLimitInsideOneWindowThenRefuses() {
        var quota = new WindowQuota(4, 20);
        for (int i = 0; i < 4; i++) assertTrue(quota.take(100 + i * 3));
        assertFalse(quota.take(119));
        assertTrue(quota.take(120));
    }

    @Test
    void aRewoundClockStartsANewWindow() {
        var quota = new WindowQuota(1, 20);
        assertTrue(quota.take(50));
        assertFalse(quota.take(60));
        assertTrue(quota.take(10));
    }

    @Test
    void rejectsInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> new WindowQuota(0, 20));
        assertThrows(IllegalArgumentException.class, () -> new WindowQuota(1, 0));
    }
}
