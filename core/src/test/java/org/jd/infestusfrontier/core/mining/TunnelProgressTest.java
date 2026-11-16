package org.jd.infestusfrontier.core.mining;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TunnelProgressTest {
    private static TunnelProgress waitOut(TunnelProgress progress) {
        while (!progress.due()) progress = progress.waited();
        return progress;
    }

    @Test
    void aFreshHeadHasLaidNoCellsAndWaits() {
        assertEquals(0, TunnelProgress.START.cells());
        assertFalse(TunnelProgress.START.due());
        assertFalse(TunnelProgress.START.finished());
        assertEquals(Tunnelling.STEP_TICKS, TunnelProgress.START.ticksLeft());
    }

    @Test
    void aStepIsDueAfterTwentySecondsOfLoadedTicks() {
        TunnelProgress progress = TunnelProgress.START;
        for (int tick = 1; tick < Tunnelling.STEP_TICKS; tick++) {
            progress = progress.waited();
            assertFalse(progress.due(), "tick " + tick);
        }
        assertTrue(progress.waited().due());
        assertEquals(0, progress.waited().ticksLeft());
        assertEquals(progress.waited(), progress.waited().waited(), "waiting never passes the due point");
    }

    @Test
    void aStepLaysOneCellAndRestartsTheWait() {
        TunnelProgress stepped = waitOut(TunnelProgress.START).stepped();
        assertEquals(1, stepped.cells());
        assertEquals(0, stepped.ticks());
    }

    @Test
    void theRootStopsAtSixtyFourCellsAndRefusesAnotherStep() {
        TunnelProgress progress = TunnelProgress.START;
        for (int cell = 0; cell < Tunnelling.MAX_CELLS; cell++) {
            assertFalse(progress.finished(), "cell " + cell);
            progress = waitOut(progress).stepped();
        }
        assertEquals(64, progress.cells());
        assertTrue(progress.finished());
        TunnelProgress full = waitOut(progress);
        assertThrows(IllegalStateException.class, full::stepped);
    }

    @Test
    void impossibleSavedNumbersDoNotConstruct() {
        assertThrows(IllegalArgumentException.class, () -> new TunnelProgress(Tunnelling.MAX_CELLS + 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new TunnelProgress(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new TunnelProgress(0, Tunnelling.STEP_TICKS + 1));
        assertThrows(IllegalArgumentException.class, () -> new TunnelProgress(0, -1));
        assertDoesNotThrow(() -> new TunnelProgress(Tunnelling.MAX_CELLS, Tunnelling.STEP_TICKS));
    }
}
