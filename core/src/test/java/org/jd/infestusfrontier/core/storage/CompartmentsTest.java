package org.jd.infestusfrontier.core.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CompartmentsTest {
    private static final int O = Compartments.OTHER;

    private static int[] limits(int count, int limit) {
        int[] limits = new int[count];
        Arrays.fill(limits, limit);
        return limits;
    }

    @Test
    void aCompartmentHoldsItsStacksOfTheKind() {
        assertEquals(512, Compartments.limit(8, 64));
        assertEquals(256, Compartments.limit(16, 16));
        assertEquals(32, Compartments.limit(32, 1));
    }

    @Test
    void theKindsOwnCompartmentFillsBeforeAnEmptyOne() {
        assertArrayEquals(new int[] {0, 0, 8, 0}, Compartments.plan(new int[] {0, O, 10, 0}, limits(4, 64), 8));
    }

    @Test
    void aFullCompartmentOverflowsIntoTheFirstEmptyOne() {
        assertArrayEquals(new int[] {0, 4, 4, 0}, Compartments.plan(new int[] {O, 60, 0, 0}, limits(4, 64), 8));
    }

    @Test
    void whatDoesNotFitIsLeftOut() {
        int[] taken = Compartments.plan(new int[] {O, 1020, O, O}, limits(4, 1024), 10);
        assertArrayEquals(new int[] {0, 4, 0, 0}, taken);
    }

    @Test
    void theRoomOfEveryHoldingCompartmentIsUsedBeforeAnyEmptyOne() {
        assertArrayEquals(new int[] {5, 2, 0, 3, 0}, Compartments.plan(new int[] {0, 62, O, 61, 0}, limits(5, 64), 10));
    }

    @Test
    void everyCompartmentKeepsItsOwnLimit() {
        int[] limits = {64, 128, 64, 256};
        assertArrayEquals(new int[] {0, 68, 0, 32}, Compartments.plan(new int[] {O, 60, O, 0}, limits, 100));
        assertEquals(256, Compartments.room(new int[] {O, 128, O, 0}, limits, 3));
    }

    @Test
    void aNewKindOpensOnlyTheFirstEmptyCompartment() {
        int[] counts = {O, 0, O, 0};
        assertEquals(1, Compartments.opening(counts, limits(4, 64)));
        assertEquals(64, Compartments.room(counts, limits(4, 64), 1));
        assertEquals(0, Compartments.room(counts, limits(4, 64), 3), "a second empty compartment waits");
        assertEquals(0, Compartments.room(counts, limits(4, 64), 0), "another kind's compartment takes none");
    }

    @Test
    void noEmptyCompartmentOpensWhileTheKindHasRoom() {
        int[] counts = {0, 0, 30, 0};
        assertEquals(-1, Compartments.opening(counts, limits(4, 64)));
        assertEquals(0, Compartments.room(counts, limits(4, 64), 0));
        assertEquals(34, Compartments.room(counts, limits(4, 64), 2));
    }

    @Test
    void aFullKindOpensTheFirstEmptyCompartment() {
        int[] counts = {O, 64, 0, 0};
        assertEquals(2, Compartments.opening(counts, limits(4, 64)));
        assertEquals(0, Compartments.room(counts, limits(4, 64), 1));
        assertEquals(64, Compartments.room(counts, limits(4, 64), 2));
    }

    @Test
    void aFullStoreOpensNothing() {
        assertEquals(-1, Compartments.opening(new int[] {O, 64, O}, limits(3, 64)));
        assertArrayEquals(new int[] {0, 0, 0}, Compartments.plan(new int[] {O, 64, O}, limits(3, 64), 5));
    }
}
