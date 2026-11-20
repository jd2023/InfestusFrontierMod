package org.jd.infestusfrontier.core.power;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PowerStoreTest {
    @Test
    void oneBoilerBatchFeedsExactlyTwoGeneratorBatches() {
        PowerStore start = new PowerStore(10, 10, 0, 0, 0);
        assertEquals(new PowerStore(0, 0, 0, 80, 3), start.boil().generate().generate());
        assertEquals(PowerStore.EMPTY, PowerStore.EMPTY.boil().generate());
    }

    @Test
    void fullProductsAndMissingInputsChangeNothing() {
        for (PowerStore store : new PowerStore[] {
            new PowerStore(9, 10, 0, 0, 0), new PowerStore(10, 9, 0, 0, 0), new PowerStore(10, 10, 15961, 0, 0)
        }) assertSame(store, store.boil());
        for (PowerStore store : new PowerStore[] {new PowerStore(0, 0, 19, 0, 0), new PowerStore(0, 0, 20, 15961, 0)})
            assertSame(store, store.generate());
    }

    @Test
    void boundariesFitExactlyAndHistoryNeverOverflows() {
        assertEquals(16000, new PowerStore(10, 10, 15960, 0, 0).boil().steam());
        assertEquals(16000, new PowerStore(0, 0, 20, 15960, 0).generate().charge());
        assertEquals(
                Integer.MAX_VALUE,
                new PowerStore(10, 10, 0, 0, Integer.MAX_VALUE).boil().cycles());
        assertThrows(IllegalArgumentException.class, () -> new PowerStore(-1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PowerStore(0, 0, 0, 16001, 0));
    }
}
