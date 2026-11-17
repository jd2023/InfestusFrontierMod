package org.jd.infestusfrontier.core.genetics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExtractionTest {
    @Test
    void anExtractionIsDoneAfterItsTicksAndWaitingNeverPassesThem() {
        Extraction extraction = Extraction.start("minecraft:pig", 10, 3);
        assertFalse(extraction.done());
        assertEquals(3, extraction.ticksLeft());
        extraction = extraction.waited().waited();
        assertFalse(extraction.done());
        Extraction done = extraction.waited();
        assertTrue(done.done());
        assertEquals(0, done.ticksLeft());
        assertEquals(done, done.waited());
    }

    @Test
    void impossibleNumbersDoNotConstruct() {
        assertThrows(IllegalArgumentException.class, () -> new Extraction("minecraft:pig", 0, 1, 0));
        assertThrows(
                IllegalArgumentException.class, () -> new Extraction("minecraft:pig", Genetics.COMPLETE + 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Extraction("minecraft:pig", 10, 0, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Extraction("minecraft:pig", 10, Genetics.MAX_EXTRACTION_TICKS + 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Extraction("minecraft:pig", 10, 5, 6));
        assertThrows(IllegalArgumentException.class, () -> new Extraction("minecraft:pig", 10, 5, -1));
        assertThrows(IllegalArgumentException.class, () -> new Extraction("", 10, 5, 0));
    }
}
