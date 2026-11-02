package org.jd.infestusfrontier.core.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReservesTest {
    @Test
    void aCompartmentGivesWhatIsAboveItsReserveAndNeverLess() {
        assertEquals(36, Reserves.automatable(100, 64));
        assertEquals(0, Reserves.automatable(64, 64));
        assertEquals(0, Reserves.automatable(10, 64));
        assertEquals(10, Reserves.automatable(10, 0));
    }

    @Test
    void whatIsKeptAndWhatIsGivenAlwaysAddUpToWhatIsHeld() {
        for (int held = 0; held <= 70; held++)
            for (int reserve : List.of(0, 1, 4, 64))
                assertEquals(held, Reserves.reserved(held, reserve) + Reserves.automatable(held, reserve));
    }

    @Test
    void negativeCountsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> Reserves.automatable(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> Reserves.automatable(1, -1));
    }

    @Test
    void settingsAreTheMouthsAmountsUpToTheLimitAndThenTheLimit() {
        assertEquals(List.of(0, 1, 4, 8, 16, 32, 64), Reserves.steps(64));
        assertEquals(List.of(0, 1, 4, 8, 16, 32, 64, 128, 256), Reserves.steps(256));
        assertEquals(List.of(0, 1, 4, 8, 16, 32, 48), Reserves.steps(48));
        assertEquals(List.of(0, 1, 4, 8, 16), Reserves.steps(16));
        assertEquals(List.of(0, 1, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048), Reserves.steps(2048));
    }

    @Test
    void steppingUpWalksTheSettingsAndWrapsToZero() {
        assertEquals(1, Reserves.next(0, 64, true));
        assertEquals(64, Reserves.next(32, 64, true));
        assertEquals(0, Reserves.next(64, 64, true));
    }

    @Test
    void steppingDownWalksBackAndWrapsToTheLimit() {
        assertEquals(32, Reserves.next(64, 64, false));
        assertEquals(0, Reserves.next(1, 64, false));
        assertEquals(64, Reserves.next(0, 64, false));
    }

    @Test
    void aReserveBetweenSettingsMovesToTheNearestOneAskedFor() {
        assertEquals(8, Reserves.next(5, 64, true));
        assertEquals(4, Reserves.next(5, 64, false));
        assertEquals(64, Reserves.next(10_000, 64, false));
        assertEquals(0, Reserves.next(10_000, 64, true));
    }

    @Test
    void aReserveIsHeldToTheLimit() {
        assertEquals(16, Reserves.clamp(64, 16));
        assertEquals(0, Reserves.clamp(-3, 16));
        assertEquals(8, Reserves.clamp(8, 16));
    }
}
