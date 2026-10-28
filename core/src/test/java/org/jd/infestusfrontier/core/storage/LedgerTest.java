package org.jd.infestusfrontier.core.storage;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LedgerTest {
    @Test
    void theOpenEntryTakesARowOfItsOwnOnlyWhenTheKindsFillTheLast() {
        assertEquals(1, Ledger.rows(0));
        assertEquals(1, Ledger.rows(7));
        assertEquals(2, Ledger.rows(8));
        assertEquals(4, Ledger.rows(24));
        assertEquals(6, Ledger.rows(42));
    }

    @Test
    void aViewScrollsOnlyPastThreeRows() {
        assertEquals(0, Ledger.lastRow(0));
        assertEquals(0, Ledger.lastRow(16));
        assertEquals(0, Ledger.lastRow(23));
        assertEquals(1, Ledger.lastRow(24));
        assertEquals(3, Ledger.lastRow(42));
    }

    @Test
    void rowsAreKeptInRange() {
        assertEquals(0, Ledger.clampRow(-1, 42));
        assertEquals(3, Ledger.clampRow(9, 42));
        assertEquals(2, Ledger.clampRow(2, 42));
        assertEquals(0, Ledger.clampRow(2, 16));
    }

    @Test
    void cellsMapToEntriesRowByRow() {
        assertEquals(0, Ledger.entry(0, 0));
        assertEquals(8, Ledger.entry(1, 0));
        assertEquals(31, Ledger.entry(1, 23));
        assertThrows(IllegalArgumentException.class, () -> Ledger.entry(0, Ledger.VIEW));
        assertThrows(IllegalArgumentException.class, () -> Ledger.rows(-1));
    }
}
