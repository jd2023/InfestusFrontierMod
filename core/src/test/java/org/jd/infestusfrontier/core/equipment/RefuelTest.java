package org.jd.infestusfrontier.core.equipment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RefuelTest {
    @Test
    void overheadIsAFifthRoundedUp() {
        assertEquals(0, Refuel.cost(0));
        assertEquals(2, Refuel.cost(1));
        assertEquals(6, Refuel.cost(5));
        assertEquals(8, Refuel.cost(6));
        assertEquals(12, Refuel.cost(10));
        assertThrows(IllegalArgumentException.class, () -> Refuel.cost(-1));
    }

    @Test
    void aPadDeliversWhatItCanPayFor() {
        assertEquals(10, Refuel.affordable(250));
        assertEquals(10, Refuel.affordable(12));
        assertEquals(9, Refuel.affordable(11));
        assertEquals(1, Refuel.affordable(2));
        assertEquals(0, Refuel.affordable(1));
        assertEquals(0, Refuel.affordable(0));
        for (int stored = 0; stored < 40; stored++)
            assertTrue(Refuel.cost(Refuel.affordable(stored)) <= stored, "never overspends at " + stored);
    }

    @Test
    void aPieceWantsUpToItsLimit() {
        assertEquals(500, Refuel.wanted(0, 1000, 50));
        assertEquals(0, Refuel.wanted(600, 1000, 50));
        assertEquals(1000, Refuel.wanted(0, 1000, 100));
    }

    @Test
    void limitsCycle() {
        assertEquals(50, Refuel.nextLimit(25));
        assertEquals(25, Refuel.nextLimit(100));
        assertEquals(25, Refuel.nextLimit(7));
    }
}
