package org.jd.infestusfrontier.core.storage;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CompartmentsTest {
    private static final int O = Compartments.OTHER;

    @Test
    void aCompartmentHoldsSixteenStacks() {
        assertEquals(1024, Compartments.limit(64));
        assertEquals(256, Compartments.limit(16));
        assertEquals(16, Compartments.limit(1));
    }

    @Test
    void theKindsOwnCompartmentFillsBeforeAnEmptyOne() {
        assertArrayEquals(new int[] {0, 0, 8, 0}, Compartments.plan(new int[] {0, O, 10, 0}, 64, 8));
    }

    @Test
    void aFullCompartmentOverflowsIntoTheFirstEmptyOne() {
        assertArrayEquals(new int[] {0, 4, 4, 0}, Compartments.plan(new int[] {O, 60, 0, 0}, 64, 8));
    }

    @Test
    void whatDoesNotFitIsLeftOut() {
        int[] taken = Compartments.plan(new int[] {O, 1020, O, O}, 1024, 10);
        assertArrayEquals(new int[] {0, 4, 0, 0}, taken);
    }

    @Test
    void theRoomOfEveryHoldingCompartmentIsUsedBeforeAnyEmptyOne() {
        assertArrayEquals(new int[] {5, 2, 0, 3, 0}, Compartments.plan(new int[] {0, 62, O, 61, 0}, 64, 10));
    }

    @Test
    void aNewKindOpensOnlyTheFirstEmptyCompartment() {
        int[] counts = {O, 0, O, 0};
        assertEquals(1, Compartments.opening(counts, 64));
        assertEquals(64, Compartments.room(counts, 64, 1));
        assertEquals(0, Compartments.room(counts, 64, 3), "a second empty compartment waits");
        assertEquals(0, Compartments.room(counts, 64, 0), "another kind's compartment takes none");
    }

    @Test
    void noEmptyCompartmentOpensWhileTheKindHasRoom() {
        int[] counts = {0, 0, 30, 0};
        assertEquals(-1, Compartments.opening(counts, 64));
        assertEquals(0, Compartments.room(counts, 64, 0));
        assertEquals(34, Compartments.room(counts, 64, 2));
    }

    @Test
    void aFullKindOpensTheFirstEmptyCompartment() {
        int[] counts = {O, 64, 0, 0};
        assertEquals(2, Compartments.opening(counts, 64));
        assertEquals(0, Compartments.room(counts, 64, 1));
        assertEquals(64, Compartments.room(counts, 64, 2));
    }

    @Test
    void aFullStoreOpensNothing() {
        assertEquals(-1, Compartments.opening(new int[] {O, 64, O}, 64));
        assertArrayEquals(new int[] {0, 0, 0}, Compartments.plan(new int[] {O, 64, O}, 64, 5));
    }
}
