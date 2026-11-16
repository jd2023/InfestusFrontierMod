package org.jd.infestusfrontier.core.mining;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TunnellingTest {
    @Test
    void theOutputHoldsOneStackInAll() {
        assertEquals(64, Tunnelling.OUTPUT_ITEMS);
        assertTrue(Tunnelling.fits(0, 64));
        assertTrue(Tunnelling.fits(62, 2));
        assertFalse(Tunnelling.fits(63, 2), "a cut that would pass one stack does not fit");
        assertFalse(Tunnelling.fits(0, 65));
        assertFalse(Tunnelling.fits(Integer.MAX_VALUE - 1, 1), "no overflow lets a huge count through");
        assertThrows(IllegalArgumentException.class, () -> Tunnelling.fits(-1, 1));
    }

    @Test
    void theLimitsAreTheStartingValues() {
        assertEquals(64, Tunnelling.MAX_CELLS);
        assertEquals(16, Tunnelling.MAX_SEGMENTS);
        assertEquals(20 * 20, Tunnelling.STEP_TICKS, "one step every twenty seconds");
        assertTrue(Tunnelling.STEP_BIOMASS_BU <= Tunnelling.BIOMASS_CAPACITY_BU);
    }

    @Test
    void everyStopHasItsOwnKeyUnderTheTunnelRoot() {
        List<String> keys = Tunnelling.STOPS.stream().map(stop -> stop.key()).toList();
        assertEquals(keys.size(), new HashSet<>(keys).size(), "no two stops share a key");
        for (String key : keys) assertTrue(key.startsWith("tunnel_root."), key);
        assertEquals(11, keys.size());
    }

    @Test
    void steeringCyclesStraightLeftRightAndReadsOnlyKnownNumbers() {
        assertEquals(Steer.LEFT, Steer.STRAIGHT.next());
        assertEquals(Steer.RIGHT, Steer.LEFT.next());
        assertEquals(Steer.STRAIGHT, Steer.RIGHT.next());
        assertEquals(Optional.of(Steer.RIGHT), Steer.at(2));
        assertEquals(Optional.empty(), Steer.at(3));
        assertEquals(Optional.empty(), Steer.at(-1));
    }
}
