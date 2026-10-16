package org.jd.infestusfrontier.core.foundation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OutcomeTest {
    @Test
    void doneCarriesTheValueAndRefusedTheReason() {
        Outcome<Integer> done = Outcome.done(3);
        Outcome<Integer> refused = Outcome.refused(new Refusal("test.no"));
        assertTrue(done.isDone());
        assertFalse(refused.isDone());
        String text = switch (done) {
            case Outcome.Done<Integer> d -> "done " + d.value();
            case Outcome.Refused<Integer> r -> r.refusal().key();
        };
        assertEquals("done 3", text);
        assertEquals("test.no", ((Outcome.Refused<Integer>) refused).refusal().key());
        assertThrows(IllegalArgumentException.class, () -> new Refusal(" "));
    }
}
