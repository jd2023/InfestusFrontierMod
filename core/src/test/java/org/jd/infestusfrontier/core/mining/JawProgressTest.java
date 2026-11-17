package org.jd.infestusfrontier.core.mining;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JawProgressTest {
    @Test
    void aFreshJawHasCutNothingAndWaits() {
        assertEquals(0, JawProgress.START.cuts());
        assertFalse(JawProgress.START.due());
        assertEquals(Fracturing.CUT_TICKS, JawProgress.START.ticksLeft());
    }

    @Test
    void aCutIsDueAfterThreeSecondsOfLoadedTicksAndWaitingNeverPassesIt() {
        JawProgress progress = JawProgress.START;
        for (int tick = 1; tick < Fracturing.CUT_TICKS; tick++) {
            progress = progress.waited();
            assertFalse(progress.due(), "tick " + tick);
        }
        JawProgress due = progress.waited();
        assertTrue(due.due());
        assertEquals(0, due.ticksLeft());
        assertEquals(due, due.waited());
    }

    @Test
    void aCutCountsOnceAndRestartsTheWait() {
        JawProgress cut = new JawProgress(4, Fracturing.CUT_TICKS).cut();
        assertEquals(5, cut.cuts());
        assertEquals(0, cut.ticks());
    }

    @Test
    void theHistoryStopsAtItsLargestValueInsteadOfOverflowing() {
        JawProgress full = new JawProgress(Integer.MAX_VALUE, Fracturing.CUT_TICKS);
        assertEquals(Integer.MAX_VALUE, full.cut().cuts());
    }

    @Test
    void impossibleNumbersDoNotConstruct() {
        assertThrows(IllegalArgumentException.class, () -> new JawProgress(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new JawProgress(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new JawProgress(0, Fracturing.CUT_TICKS + 1));
    }

    @Test
    void everyStopHasItsOwnKeyUnderTheJaw() {
        assertEquals(
                Fracturing.STOPS.size(),
                Fracturing.STOPS.stream().map(stop -> stop.key()).distinct().count());
        assertTrue(Fracturing.STOPS.stream().allMatch(stop -> stop.key().startsWith("fracture_jaw.")));
    }
}
