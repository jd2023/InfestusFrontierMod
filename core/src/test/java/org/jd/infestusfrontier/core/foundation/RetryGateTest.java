package org.jd.infestusfrontier.core.foundation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RetryGateTest {
    private record State(int stored, String item) {}

    @Test
    void retriesOnlyWhenTheStateChanged() {
        RetryGate<State> gate = new RetryGate<>();
        State full = new State(1000, "wheat");
        assertTrue(gate.due(full));
        gate.refused(full);
        assertFalse(gate.due(new State(1000, "wheat")), "same state, no retry");
        assertTrue(gate.due(new State(900, "wheat")), "the tank changed");
        assertTrue(gate.due(new State(1000, "flesh")), "the feed changed");
        gate.clear();
        assertTrue(gate.due(full));
    }
}
