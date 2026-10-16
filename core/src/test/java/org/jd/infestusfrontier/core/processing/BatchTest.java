package org.jd.infestusfrontier.core.processing;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BatchTest {
    @Test
    void advancesOneTickAndStopsWhenDone() {
        Batch batch = Batch.start("digestion/wheat", 3);
        assertEquals(0, batch.percent());
        batch = batch.advance().advance();
        assertEquals(66, batch.percent());
        assertFalse(batch.isDone());
        batch = batch.advance();
        assertTrue(batch.isDone());
        assertEquals(batch, batch.advance());
        assertEquals(100, batch.percent());
    }

    @Test
    void rejectsImpossibleBatches() {
        assertThrows(IllegalArgumentException.class, () -> Batch.start("", 3));
        assertThrows(IllegalArgumentException.class, () -> Batch.start("x", 0));
        assertThrows(IllegalArgumentException.class, () -> new Batch("x", 3, 4));
    }
}
