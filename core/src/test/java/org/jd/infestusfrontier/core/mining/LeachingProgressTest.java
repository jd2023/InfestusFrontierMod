package org.jd.infestusfrontier.core.mining;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LeachingProgressTest {
    private static LeachingProgress waitOut(LeachingProgress progress) {
        while (!progress.due()) progress = progress.waited();
        return progress;
    }

    @Test
    void aFreshPodHasAllItsChargesAndNoLayers() {
        assertEquals(0, LeachingProgress.START.layers());
        assertEquals(Leaching.MAX_CHARGES, LeachingProgress.START.charges());
        assertFalse(LeachingProgress.START.finished());
        assertFalse(LeachingProgress.START.due());
    }

    @Test
    void aLayerIsDueAfterOneSecondOfLoadedTicks() {
        LeachingProgress progress = LeachingProgress.START;
        for (int tick = 1; tick < Leaching.LAYER_TICKS; tick++) {
            progress = progress.waited();
            assertFalse(progress.due(), "tick " + tick);
        }
        assertTrue(progress.waited().due());
        assertEquals(progress.waited(), progress.waited().waited(), "waiting never passes the due point");
    }

    @Test
    void aProductiveLayerCostsOneChargeAndRestartsTheWait() {
        LeachingProgress worked = waitOut(LeachingProgress.START).worked(37);
        assertEquals(1, worked.layers());
        assertEquals(Leaching.MAX_CHARGES - 1, worked.charges());
        assertEquals(0, worked.ticks());
    }

    @Test
    void aLayerThatConvertedNothingUsesALayerButNoCharge() {
        LeachingProgress worked = LeachingProgress.START.worked(0);
        assertEquals(1, worked.layers());
        assertEquals(Leaching.MAX_CHARGES, worked.charges());
    }

    @Test
    void aBlockedLayerChangesNothingButTheWait() {
        LeachingProgress due = waitOut(new LeachingProgress(5, 4, 0));
        assertEquals(new LeachingProgress(5, 4, 0), due.blocked());
    }

    @Test
    void sixteenProductiveLayersFinishTheWorkWithTheLayersAndTheChargesTogether() {
        LeachingProgress progress = LeachingProgress.START;
        for (int layer = 1; layer <= Leaching.MAX_LAYERS; layer++) {
            assertFalse(progress.finished(), "layer " + layer + " is still to come");
            progress = progress.worked(1);
        }
        assertTrue(progress.finished());
        assertEquals(Leaching.MAX_LAYERS, progress.layers());
        assertEquals(0, progress.charges());
    }

    @Test
    void emptyLayersRunOutTheLayersBeforeTheCharges() {
        LeachingProgress progress = LeachingProgress.START;
        for (int layer = 0; layer < Leaching.MAX_LAYERS; layer++) progress = progress.worked(0);
        assertTrue(progress.finished());
        assertEquals(Leaching.MAX_CHARGES, progress.charges());
    }

    @Test
    void aPodNeverWorksAPastTheLastLayerOrChargeAndOutOfRangeNumbersAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new LeachingProgress(Leaching.MAX_LAYERS, 1, 0).worked(1));
        assertThrows(IllegalArgumentException.class, () -> new LeachingProgress(3, 0, 0).worked(1));
        assertThrows(IllegalArgumentException.class, () -> new LeachingProgress(-1, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> new LeachingProgress(Leaching.MAX_LAYERS + 1, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> new LeachingProgress(0, Leaching.MAX_CHARGES + 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new LeachingProgress(0, 5, Leaching.LAYER_TICKS + 1));
        assertThrows(IllegalArgumentException.class, () -> LeachingProgress.START.worked(38));
    }

    @Test
    void thePodIsFinishedOnlyWhenItsWorkIs() {
        assertFalse(new LeachingProgress(15, 1, 7).finished(), "one layer and one charge left");
        assertTrue(new LeachingProgress(16, 3, 0).finished());
        assertTrue(new LeachingProgress(9, 0, 0).finished());
    }
}
