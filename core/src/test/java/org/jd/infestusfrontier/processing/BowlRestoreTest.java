package org.jd.infestusfrontier.processing;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.jd.infestusfrontier.organ.api.OrganHistory;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.storage.api.QuantityStore;
import org.junit.jupiter.api.Test;

class BowlRestoreTest {
    @Test void overflowAllocationsAreRejectedBeforeAnyConsumption() {
        var wheat = new QuantityStore.ItemSlot("wheat", 1, 64);
        assertThrows(IllegalArgumentException.class, () -> QuantityStore.restore(new QuantityStore.Snapshot(0, 2,
                List.of(wheat, QuantityStore.ItemSlot.empty(64)), List.of(), List.of(
                new QuantityStore.ReservationSnapshot(1, List.of(new QuantityStore.ItemAllocation(0, "wheat", 1)),
                        List.of(), List.of(new QuantityStore.ItemAllocation(1, "nutrient_mash", Integer.MAX_VALUE),
                        new QuantityStore.ItemAllocation(1, "nutrient_mash", Integer.MAX_VALUE),
                        new QuantityStore.ItemAllocation(1, "nutrient_mash", 3)), List.of())))));
        assertEquals(1, wheat.count());
    }

    @Test void allocationListsAreBoundedBeforeCopying() {
        assertThrows(IllegalArgumentException.class, () -> new QuantityStore.ReservationSnapshot(1,
                java.util.Collections.nCopies(10, new QuantityStore.ItemAllocation(0, "wheat", 1)),
                List.of(), List.of(), List.of()));
    }

    @Test void fabricatedRecipeReservationCannotMintProductsOrHistory() {
        var free = new QuantityStore.ReservationSnapshot(1, List.of(), List.of(),
                List.of(new QuantityStore.ItemAllocation(0, "nutrient_mash", 64)), List.of());
        var state = new BatchWork.State(1, 0, 2, new BatchWork.ActiveBatch(1, "I007", 1, 0, 1),
                new QuantityStore.Snapshot(0, 2, List.of(QuantityStore.ItemSlot.empty(64)), List.of(), List.of(free)),
                new OrganHistory.Snapshot(0, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> BatchWork.restore(state, () -> true));
    }

    @Test void matchingWorkCannotHideChangedInputsFluidsOutputsOrReturns() {
        var slots = new java.util.ArrayList<QuantityStore.ItemSlot>();
        slots.add(new QuantityStore.ItemSlot("honey_bottle", 1, 16));
        slots.add(new QuantityStore.ItemSlot("spore_culture", 1, 64));
        while (slots.size() < 9) slots.add(QuantityStore.ItemSlot.empty(64));
        var work = BatchWork.create(new QuantityStore(slots, List.of(QuantityStore.Tank.empty(1000))), () -> true);
        work.start(new BatchWork.StartRequest("I030"), work.revision());
        var good = work.state();
        var q = good.quantities(); var r = q.reservations().getFirst();
        var variants = List.of(
                new QuantityStore.ReservationSnapshot(r.reservationId(), List.of(), r.inputFluids(), r.itemOutputs(), r.returnedContainers()),
                new QuantityStore.ReservationSnapshot(r.reservationId(), r.inputItems(), r.inputFluids(), List.of(), r.returnedContainers()),
                new QuantityStore.ReservationSnapshot(r.reservationId(), r.inputItems(), r.inputFluids(), r.itemOutputs(), List.of()));
        for (var bad : variants) {
            var altered = new BatchWork.State(good.schema(), good.revision(), good.nextBatchId(), good.activeBatch(),
                    new QuantityStore.Snapshot(q.revision(), q.nextReservationId(), q.itemSlots(), q.tanks(), List.of(bad)), good.history());
            assertThrows(IllegalArgumentException.class, () -> BatchWork.restore(altered, () -> true));
        }
        var a = good.activeBatch();
        var wrongWork = new BatchWork.State(good.schema(), good.revision(), good.nextBatchId(),
                new BatchWork.ActiveBatch(a.batchId(), a.recipeId(), a.reservationId(), 0, 840), q, good.history());
        assertThrows(IllegalArgumentException.class, () -> BatchWork.restore(wrongWork, () -> true));
        assertEquals(good, BatchWork.restore(good, () -> true).state());
    }

    @Test void transfersAreAtomicBoundedAndLockedByTheBatch() {
        var work = BatchWork.create(new QuantityStore(java.util.Collections.nCopies(9, QuantityStore.ItemSlot.empty(64)),
                List.of(QuantityStore.Tank.empty(1000))), () -> true);
        assertTrue(work.insertItem("wheat", 1, 64, work.revision()));
        assertTrue(work.insertItem("carrot", 1, 64, work.revision()));
        assertTrue(work.insertWater(1000, work.revision()));
        var full = work.state();
        assertFalse(work.insertWater(1, work.revision()));
        assertFalse(work.insertItem("wheat", 1, 64, work.revision() - 1));
        assertEquals(full, work.state());
        assertInstanceOf(BatchWork.Started.class, work.start(new BatchWork.StartRequest("I007"), work.revision()));
        var active = work.state();
        assertFalse(work.insertWater(1, work.revision()));
        assertFalse(work.insertItem("wheat", 1, 64, work.revision()));
        assertFalse(work.extractItemSlot(0, work.revision()));
        assertEquals(active, work.state());
        work.advance(1200);
        assertEquals(2, work.state().quantities().itemCount("nutrient_mash"));
        int slot = 0;
        while (!work.state().quantities().itemSlots().get(slot).resource().equals("nutrient_mash")) slot++;
        assertTrue(work.extractItemSlot(slot, work.revision()));
        assertEquals(0, work.state().quantities().itemCount("nutrient_mash"));
        assertFalse(work.extractItemSlot(slot, work.revision()));
    }

    @Test void exhaustedCountersCannotStrandAReservedBatch() {
        var work = BatchWork.create(new QuantityStore(List.of(
                new QuantityStore.ItemSlot("wheat", 1, 64), new QuantityStore.ItemSlot("carrot", 1, 64), QuantityStore.ItemSlot.empty(64)),
                List.of(new QuantityStore.Tank("water", 100, 1000))), () -> true);
        var fresh = work.state();
        var nearLimit = BatchWork.restore(new BatchWork.State(1, Long.MAX_VALUE - 2, 1, null, fresh.quantities(), fresh.history()), () -> true);
        var before = nearLimit.state();
        assertEquals(BatchWork.StartRefusal.REVISION_EXHAUSTED,
                assertInstanceOf(BatchWork.Refused.class, nearLimit.start(new BatchWork.StartRequest("I007"), nearLimit.revision())).reason());
        assertEquals(before, nearLimit.state());
        work.start(new BatchWork.StartRequest("I007"), work.revision());
        var active = work.state();
        assertThrows(IllegalArgumentException.class, () -> BatchWork.restore(new BatchWork.State(1, Long.MAX_VALUE - 2, 2,
                active.activeBatch(), active.quantities(), active.history()), () -> true));
    }

    @Test void pureChoicesAtEveryLevelPreserveTimeWaterAndChoiceCapsAfterReload() {
        long[] thresholds = {32, 128, 512};
        for (int level = 1; level <= 3; level++) {
            for (var choice : OrganHistory.GrowthChoice.values()) {
                long count = thresholds[level - 1];
                var history = new OrganHistory.Snapshot(count, count, java.util.Collections.nCopies(level, choice));
                var work = BatchWork.create(new QuantityStore(List.of(new QuantityStore.ItemSlot("spore_culture", 1, 64),
                        new QuantityStore.ItemSlot("wheat_seeds", 1, 64), QuantityStore.ItemSlot.empty(64)),
                        List.of(new QuantityStore.Tank("water", 1000, 1000))), history, () -> true);
                assertNotEquals(BatchWork.GrowthResult.CHOSEN, work.choose(choice, work.revision()));
                work.start(new BatchWork.StartRequest("I033"), work.revision());
                int expectedTime = choice == OrganHistory.GrowthChoice.INCUBATION ? 1200 - level * 120 : 1200;
                int expectedWater = choice == OrganHistory.GrowthChoice.WATER_ECONOMY ? 50 - level * 5 : 50;
                assertEquals(expectedTime, work.state().activeBatch().requiredWorkUnits());
                work = BatchWork.restore(work.state(), () -> true);
                work.advance(expectedTime);
                assertEquals(1000 - expectedWater, work.state().quantities().fluidAmount("water"));
                assertEquals(count + 1, work.state().history().completedBatches());
                assertEquals(level, work.state().history().choices().size());
            }
        }
    }
}
