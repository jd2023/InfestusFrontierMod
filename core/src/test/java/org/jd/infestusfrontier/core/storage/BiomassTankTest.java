package org.jd.infestusfrontier.core.storage;

import static org.junit.jupiter.api.Assertions.*;

import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.junit.jupiter.api.Test;

class BiomassTankTest {
    private static final BiomassTank EMPTY = BiomassTank.empty(Biomass.of(1000));

    private static BiomassTank value(Outcome<BiomassTank> outcome) {
        return switch (outcome) {
            case Outcome.Done<BiomassTank> d -> d.value();
            case Outcome.Refused<BiomassTank> r ->
                fail("refused: " + r.refusal().key());
        };
    }

    @Test
    void fillsWholeOrNothing() {
        BiomassTank tank = value(EMPTY.fill(Biomass.of(950)));
        assertEquals(Biomass.of(50), tank.room());
        Outcome<BiomassTank> refused = tank.fill(Biomass.of(51));
        assertEquals(Outcome.refused(BiomassTank.FULL), refused);
        assertEquals(Biomass.of(1000), value(tank.fill(Biomass.of(50))).stored());
    }

    @Test
    void drainsOnlyWhatIsStored() {
        BiomassTank tank = value(EMPTY.fill(Biomass.of(999)));
        assertEquals(Outcome.refused(BiomassTank.EMPTY), tank.drain(Biomass.BUCKET));
        assertEquals(Biomass.ZERO, value(tank.drain(Biomass.of(999))).stored());
    }

    @Test
    void reservedRoomIsNotStoredAndNotFree() {
        BiomassTank tank = value(EMPTY.reserve(Biomass.of(100)));
        assertEquals(Biomass.ZERO, tank.stored());
        assertEquals(Biomass.of(900), tank.room());
        assertEquals(Outcome.refused(BiomassTank.FULL), tank.fill(Biomass.of(901)));
        assertEquals(Outcome.refused(BiomassTank.EMPTY), tank.drain(Biomass.of(1)));
        BiomassTank committed = tank.commit(Biomass.of(100));
        assertEquals(Biomass.of(100), committed.stored());
        assertEquals(Biomass.ZERO, committed.reserved());
        assertEquals(EMPTY, tank.release(Biomass.of(100)));
        assertThrows(IllegalArgumentException.class, () -> tank.commit(Biomass.of(101)));
    }

    @Test
    void capacityIsAHardCeiling() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BiomassTank(Biomass.of(600), Biomass.of(500), Biomass.of(1000)));
        assertTrue(value(EMPTY.fill(Biomass.of(1000))).isFull());
    }
}
