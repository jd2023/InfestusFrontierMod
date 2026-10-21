package org.jd.infestusfrontier.core.logistics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class DeliveryTest {
    private static Delivery.Destination<String> d(String id, int priority, int distance) {
        return new Delivery.Destination<>(id, priority, distance);
    }

    @Test
    void priorityBeatsDistance() {
        assertEquals(
                List.of("far-high", "near-low"), Delivery.order(List.of(d("near-low", 0, 1), d("far-high", 2, 9)), 0));
    }

    @Test
    void nearerComesFirstAtEqualPriority() {
        assertEquals(List.of("a", "b"), Delivery.order(List.of(d("b", 0, 5), d("a", 0, 2)), 0));
    }

    @Test
    void equalDestinationsTakeTurns() {
        var group = List.of(d("a", 0, 3), d("b", 0, 3), d("c", 0, 3), d("z", -1, 1));
        assertEquals(List.of("a", "b", "c", "z"), Delivery.order(group, 0));
        assertEquals(List.of("b", "c", "a", "z"), Delivery.order(group, 1));
        assertEquals(List.of("c", "a", "b", "z"), Delivery.order(group, 5));
        assertEquals(List.of("c", "a", "b", "z"), Delivery.order(group, -1));
    }

    @Test
    void intakeFillsUpToItsAmount() {
        assertEquals(8, Delivery.intakeRoom(0, 1000, 8), "no rule");
        assertEquals(3, Delivery.intakeRoom(64, 61, 8));
        assertEquals(0, Delivery.intakeRoom(64, 64, 8));
        assertEquals(0, Delivery.intakeRoom(64, 90, 8));
    }

    @Test
    void outputKeepsItsAmount() {
        assertEquals(8, Delivery.outputAllowance(0, 20, 8), "no rule keeps nothing back");
        assertEquals(4, Delivery.outputAllowance(16, 20, 8));
        assertEquals(0, Delivery.outputAllowance(16, 16, 8));
        assertEquals(0, Delivery.outputAllowance(16, 3, 8));
    }

    @Test
    void settingsCycleAndClamp() {
        assertEquals(1, Delivery.nextAmount(0, true));
        assertEquals(0, Delivery.nextAmount(4096, true));
        assertEquals(4096, Delivery.nextAmount(0, false));
        assertEquals(0, Delivery.nextAmount(7, true), "an unknown amount resets");
        assertEquals(4, Delivery.nextPriority(4, true));
        assertEquals(-4, Delivery.nextPriority(-4, false));
        assertEquals(1, Delivery.nextPriority(0, true));
    }
}
