package org.jd.infestusfrontier.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jd.infestusfrontier.processing.digestion.api.DigestiveSac;
import org.junit.jupiter.api.Test;

final class DigestiveSacTest {
    @Test
    void staleIdleBatchCounterIsRejectedBeforeAnotherFeedCanCommit() {
        var sac = DigestiveSac.create(1_000, () -> true);
        sac.feed("wheat");
        sac.advance(40);
        sac.feed("wheat");
        sac.advance(40);
        var saved = sac.snapshot();
        for (long stale : new long[] {1, saved.history().lastCompletedBatchId()}) {
            var corrupt = new DigestiveSac.State(saved.schema(), saved.revision(), stale,
                    null, saved.quantities(), saved.history());
            assertThrows(IllegalArgumentException.class, () -> DigestiveSac.restore(corrupt, () -> true));
        }
        var restored = DigestiveSac.restore(saved, () -> true);
        assertInstanceOf(DigestiveSac.Fed.class, restored.feed("rotten_flesh"));
        restored.advance(160);
        assertEquals(250, restored.snapshot().biomass());
        assertEquals(3, restored.snapshot().history().completedBatches());
    }

    @Test
    void rottenFleshStartsOnlyWhenItsCompleteOutputFits() {
        var sac = DigestiveSac.create(1_000, () -> true);
        assertInstanceOf(DigestiveSac.Fed.class, sac.feed("wheat"));
        sac.advance(40);
        assertEquals(100, sac.snapshot().biomass());

        for (int i = 0; i < 9; i++) {
            assertInstanceOf(DigestiveSac.Fed.class, sac.feed("wheat"));
            sac.advance(40);
        }
        var before = sac.snapshot();
        assertInstanceOf(DigestiveSac.FeedRefused.class, sac.feed("rotten_flesh"));
        assertEquals(before, sac.snapshot(), "a full output must not take rotten flesh or alter state");
    }

    @Test
    void loadedBatchRestoresAndCommitsExactlyOnce() {
        var sac = DigestiveSac.create(1_000, () -> true);
        assertInstanceOf(DigestiveSac.Fed.class, sac.feed("rotten_flesh"));
        sac.advance(80);

        var restored = DigestiveSac.restore(sac.snapshot(), () -> true);
        assertEquals(0, restored.snapshot().biomass());
        assertEquals(80, restored.snapshot().activeBatch().completedWorkUnits());
        restored.advance(80);
        assertEquals(50, restored.snapshot().biomass());
        assertEquals(1, restored.snapshot().history().completedBatches());
        restored.advance(160);
        assertEquals(50, restored.snapshot().biomass());
        assertEquals(1, restored.snapshot().history().completedBatches());
    }

    @Test
    void wheatAndRottenFleshUseCatalogYieldsAndTimes() {
        assertEquals(100, DigestiveSac.recipe("wheat").biomass());
        assertEquals(40, DigestiveSac.recipe("wheat").workUnits());
        assertEquals(50, DigestiveSac.recipe("rotten_flesh").biomass());
        assertEquals(160, DigestiveSac.recipe("rotten_flesh").workUnits());
    }
}
