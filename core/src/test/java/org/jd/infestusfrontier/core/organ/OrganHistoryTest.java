package org.jd.infestusfrontier.core.organ;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OrganHistoryTest {
    @Test
    void countsCompletedBatches() {
        assertEquals(new OrganHistory(2), OrganHistory.NONE.completed().completed());
        assertThrows(IllegalArgumentException.class, () -> new OrganHistory(-1));
        assertThrows(ArithmeticException.class, () -> new OrganHistory(Integer.MAX_VALUE).completed());
    }
}
