package org.jd.infestusfrontier.core.mining;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GrowerProgressTest {
    @Test
    void aStepIsDueAfterOneSecondOfLoadedTicks() {
        GrowerProgress progress = GrowerProgress.START;
        for (int tick = 1; tick < Surveying.GROW_TICKS; tick++) {
            progress = progress.waited();
            assertFalse(progress.due(), "tick " + tick);
        }
        assertTrue(progress.waited().due());
        assertEquals(progress.waited(), progress.waited().waited(), "waiting never passes the due point");
    }

    @Test
    void aStepCountsOnceAndRestartsTheWait() {
        GrowerProgress grown = new GrowerProgress(4, Surveying.GROW_TICKS).grown();
        assertEquals(new GrowerProgress(5, 0), grown);
        assertEquals(
                Integer.MAX_VALUE,
                new GrowerProgress(Integer.MAX_VALUE, 0).grown().steps(),
                "it stops counting");
    }

    @Test
    void impossibleSavedNumbersDoNotConstruct() {
        assertThrows(IllegalArgumentException.class, () -> new GrowerProgress(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new GrowerProgress(0, Surveying.GROW_TICKS + 1));
        assertThrows(IllegalArgumentException.class, () -> new GrowerProgress(0, -1));
    }
}
