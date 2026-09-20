package org.jd.infestusfrontier.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jd.infestusfrontier.storage.api.QuantityStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuantityStoreTest {
    @Test
    void reserveThenCommitIsConservativeAndReloadable() {
        var store = new QuantityStore(
                List.of(
                        new QuantityStore.ItemSlot("honey_bottle", 1, 64),
                        new QuantityStore.ItemSlot("spore_culture", 1, 64),
                        QuantityStore.ItemSlot.empty(64),
                        QuantityStore.ItemSlot.empty(64)),
                List.of());
        var before = store.snapshot();
        assertInstanceOf(
                QuantityStore.PreviewAvailable.class,
                store.preview(
                        new QuantityStore.ReservationRequest(
                                Map.of("honey_bottle", 1, "spore_culture", 1),
                                Map.of(),
                                Map.of("honey_culture", 2),
                                Map.of("glass_bottle", 1)),
                        store.revision()));
        assertEquals(before, store.snapshot());
        var reserved = assertInstanceOf(
                QuantityStore.Reserved.class,
                store.reserve(
                        new QuantityStore.ReservationRequest(
                                Map.of("honey_bottle", 1, "spore_culture", 1),
                                Map.of(),
                                Map.of("honey_culture", 2),
                                Map.of("glass_bottle", 1)),
                        store.revision()));
        assertEquals(before.itemCount("honey_bottle"), store.snapshot().itemCount("honey_bottle"));
        assertEquals(before.itemCount("spore_culture"), store.snapshot().itemCount("spore_culture"));

        var restored = QuantityStore.restore(store.snapshot());
        assertEquals(QuantityStore.CommitResult.COMMITTED, restored.commit(reserved.reservationId()));
        assertEquals(0, restored.snapshot().itemCount("honey_bottle"));
        assertEquals(0, restored.snapshot().itemCount("spore_culture"));
        assertEquals(2, restored.snapshot().itemCount("honey_culture"));
        assertEquals(1, restored.snapshot().itemCount("glass_bottle"));
        assertEquals(QuantityStore.CommitResult.UNKNOWN_RESERVATION, restored.commit(reserved.reservationId()));
    }

    @Test
    void aFailedReservationDoesNotMutateAnySlot() {
        var slots = new ArrayList<QuantityStore.ItemSlot>();
        slots.add(new QuantityStore.ItemSlot("honey_bottle", 1, 64));
        slots.add(new QuantityStore.ItemSlot("spore_culture", 1, 64));
        slots.add(new QuantityStore.ItemSlot("honey_culture", 63, 64));
        slots.add(new QuantityStore.ItemSlot("glass_bottle", 64, 64));
        for (int index = 0; index < 5; index++) slots.add(new QuantityStore.ItemSlot("filler_" + index, 64, 64));
        var store = new QuantityStore(slots, List.of());
        var before = store.snapshot();

        var refused = assertInstanceOf(
                QuantityStore.ReservationRefused.class,
                store.reserve(
                        new QuantityStore.ReservationRequest(
                                Map.of("honey_bottle", 1, "spore_culture", 1),
                                Map.of(),
                                Map.of("honey_culture", 2),
                                Map.of("glass_bottle", 1)),
                        store.revision()));

        assertEquals(QuantityStore.ReserveRefusal.RETURNED_CONTAINER_FULL, refused.reason());
        assertEquals(before, store.snapshot());
    }

    @Test
    void storeBoundsAreHardAndValidatedOnRestore() {
        var tenSlots = new ArrayList<QuantityStore.ItemSlot>();
        for (int index = 0; index < 10; index++) tenSlots.add(QuantityStore.ItemSlot.empty(64));
        assertThrows(IllegalArgumentException.class, () -> new QuantityStore(tenSlots, List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new QuantityStore(
                        List.of(),
                        List.of(
                                QuantityStore.Tank.empty(1000),
                                QuantityStore.Tank.empty(1000),
                                QuantityStore.Tank.empty(1000))));
    }

    @Test
    void destinationsPreferMatchingStacksBeforeClaimingAnEmptySlot() {
        var store = new QuantityStore(
                List.of(
                        QuantityStore.ItemSlot.empty(64),
                        new QuantityStore.ItemSlot("honey_culture", 63, 64),
                        new QuantityStore.ItemSlot("glass_bottle", 63, 64)),
                List.of());

        var reserved = assertInstanceOf(
                QuantityStore.Reserved.class,
                store.reserve(
                        new QuantityStore.ReservationRequest(
                                Map.of(), Map.of(), Map.of("honey_culture", 2), Map.of("glass_bottle", 1)),
                        store.revision()));
        assertEquals(QuantityStore.CommitResult.COMMITTED, store.commit(reserved.reservationId()));
        assertEquals(65, store.snapshot().itemCount("honey_culture"));
        assertEquals(64, store.snapshot().itemCount("glass_bottle"));
    }
}
