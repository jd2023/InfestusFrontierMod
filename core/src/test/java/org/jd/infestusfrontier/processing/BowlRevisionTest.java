package org.jd.infestusfrontier.processing;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.storage.api.QuantityStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BowlRevisionTest {
    @Test
    void startReservesCapacityForEveryWorkTickAndDeferredCompletion() {
        var fresh = freshBowl().state();
        var work = BatchWork.restore(atRevision(fresh, Long.MAX_VALUE - 1202), () -> false);
        var before = work.state();

        var refused = assertInstanceOf(BatchWork.Refused.class,
                work.start(new BatchWork.StartRequest("I007"), work.revision()));

        assertEquals(BatchWork.StartRefusal.REVISION_EXHAUSTED, refused.reason());
        assertEquals(before, refused.state());
        assertEquals(before, work.state());
    }

    @Test
    void restoreRejectsWorkWithoutCapacityForDeferredCompletion() {
        var work = freshBowl();
        work.start(new BatchWork.StartRequest("I007"), work.revision());
        var almostDone = work.advance(1199);

        assertThrows(IllegalArgumentException.class,
                () -> BatchWork.restore(atRevision(almostDone, Long.MAX_VALUE - 2), () -> false));
    }

    @Test
    void lastAdmissibleStartSurvivesSingleTickWorkQuotaRefusalAndReload() {
        var admitted = new AtomicBoolean(false);
        var work = BatchWork.restore(atRevision(freshBowl().state(), Long.MAX_VALUE - 1203), admitted::get);
        var original = work.state();
        assertInstanceOf(BatchWork.Started.class,
                work.start(new BatchWork.StartRequest("I007"), work.revision()));
        for (int tick = 0; tick < 1200; tick++) work.advance(1);

        var blocked = work.state();
        assertEquals(Long.MAX_VALUE - 2, blocked.revision());
        assertEquals(BatchWork.Status.COMPLETION_BLOCKED, blocked.status());
        assertEquals(original.quantities().itemSlots(), blocked.quantities().itemSlots());
        assertEquals(original.quantities().tanks(), blocked.quantities().tanks());
        assertEquals(original.history(), blocked.history());
        assertEquals(1, blocked.quantities().reservations().size());
        work = BatchWork.restore(blocked, admitted::get);
        assertEquals(blocked, work.advance(0));
        assertEquals(blocked, work.advance(1));

        admitted.set(true);
        var completed = work.advance(0);
        assertEquals(Long.MAX_VALUE - 1, completed.revision());
        assertEquals(BatchWork.Status.IDLE, completed.status());
        assertEquals(0, completed.quantities().itemCount("wheat"));
        assertEquals(0, completed.quantities().itemCount("carrot"));
        assertEquals(0, completed.quantities().fluidAmount("water"));
        assertEquals(2, completed.quantities().itemCount("nutrient_mash"));
        assertTrue(completed.quantities().reservations().isEmpty());
        assertEquals(1, completed.history().completedBatches());
        assertEquals(completed, work.advance(1));
        assertEquals(completed, BatchWork.restore(completed, admitted::get).advance(1200));
    }

    @Test
    void lastAdmissibleRestoreCanCancelAfterQuotaRefusalAndReload() {
        var work = freshBowl();
        var original = work.state();
        work.start(new BatchWork.StartRequest("I007"), work.revision());
        var almostDone = work.advance(1199);
        work = BatchWork.restore(atRevision(almostDone, Long.MAX_VALUE - 3), () -> false);
        var blocked = work.advance(1);
        assertEquals(BatchWork.Status.COMPLETION_BLOCKED, blocked.status());
        work = BatchWork.restore(blocked, () -> false);

        assertEquals(BatchWork.CancelResult.CANCELLED, work.cancel(work.revision()));
        var cancelled = work.state();
        assertEquals(Long.MAX_VALUE - 1, cancelled.revision());
        assertEquals(BatchWork.Status.IDLE, cancelled.status());
        assertEquals(original.quantities().itemSlots(), cancelled.quantities().itemSlots());
        assertEquals(original.quantities().tanks(), cancelled.quantities().tanks());
        assertTrue(cancelled.quantities().reservations().isEmpty());
        assertEquals(original.history(), cancelled.history());
        assertEquals(cancelled, BatchWork.restore(cancelled, () -> true).advance(1200));
    }

    private static BatchWork freshBowl() {
        return BatchWork.create(new QuantityStore(List.of(
                new QuantityStore.ItemSlot("wheat", 1, 64),
                new QuantityStore.ItemSlot("carrot", 1, 64),
                QuantityStore.ItemSlot.empty(64)),
                List.of(new QuantityStore.Tank("water", 100, 1000))), () -> true);
    }

    private static BatchWork.State atRevision(BatchWork.State state, long revision) {
        return new BatchWork.State(state.schema(), revision, state.nextBatchId(), state.activeBatch(),
                state.quantities(), state.history());
    }
}
