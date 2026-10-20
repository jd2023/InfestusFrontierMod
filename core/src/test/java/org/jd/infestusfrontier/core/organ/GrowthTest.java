package org.jd.infestusfrontier.core.organ;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class GrowthTest {
    @Test
    void stagesRiseAtEachThreshold() {
        Growth growth = Growth.at(64, 512);
        assertEquals(3, growth.stages());
        assertEquals(0, growth.stage(new OrganHistory(63)));
        assertEquals(1, growth.stage(new OrganHistory(64)));
        assertEquals(1, growth.stage(new OrganHistory(511)));
        assertEquals(2, growth.stage(new OrganHistory(512)));
        assertEquals(2, growth.stage(new OrganHistory(100000)));
    }

    @Test
    void countsBatchesToTheNextStage() {
        Growth growth = Growth.at(64, 512);
        assertEquals(64, growth.toNext(OrganHistory.NONE));
        assertEquals(1, growth.toNext(new OrganHistory(63)));
        assertEquals(448, growth.toNext(new OrganHistory(64)));
        assertEquals(0, growth.toNext(new OrganHistory(512)));
    }

    @Test
    void noThresholdsIsOneStage() {
        assertEquals(1, Growth.NONE.stages());
        assertEquals(0, Growth.NONE.stage(new OrganHistory(9999)));
        assertEquals(0, Growth.NONE.toNext(new OrganHistory(9999)));
    }

    @Test
    void thresholdsMustRise() {
        assertThrows(IllegalArgumentException.class, () -> Growth.at(64, 64));
        assertThrows(IllegalArgumentException.class, () -> Growth.at(0));
        assertThrows(IllegalArgumentException.class, () -> new Growth(List.of(10, 5)));
    }
}
