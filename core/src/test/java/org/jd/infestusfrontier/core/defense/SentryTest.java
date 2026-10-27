package org.jd.infestusfrontier.core.defense;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SentryTest {
    @Test
    void aSentryStrikesOnlyWhileItCanPayAndKeep() {
        assertTrue(Sentry.works(10, 1), "ten BU and a free slot");
        assertFalse(Sentry.works(9, 8), "nine BU is not a strike");
        assertFalse(Sentry.works(1000, 0), "a full store stops it");
        assertTrue(Sentry.works(1000, 8));
    }

    @Test
    void reachIsABoxRoundTheOrgan() {
        assertTrue(Sentry.inReach(6.5, 0, 6.5));
        assertFalse(Sentry.inReach(6.6, 0, 0));
        assertTrue(Sentry.inReach(0, 3.5, 0));
        assertFalse(Sentry.inReach(0, 3.6, 0));
        assertTrue(Sentry.inReach(-6, -3, 6));
    }

    @Test
    void aTankIsAHundredStrikes() {
        assertTrue(Sentry.CAPACITY / Sentry.COST == 100);
    }
}
