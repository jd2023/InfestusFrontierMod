package org.jd.infestusfrontier.interaction;

import static org.junit.jupiter.api.Assertions.*;

import org.jd.infestusfrontier.interaction.api.IntentAdmission;
import org.junit.jupiter.api.Test;

final class IntentAdmissionTest {
    @Test
    void admitsAtMostFourIntentsInAnyTwentyTickWindow() {
        var admission = new IntentAdmission();
        assertTrue(admission.take(100));
        assertTrue(admission.take(100));
        assertTrue(admission.take(101));
        assertTrue(admission.take(119));
        assertFalse(admission.take(119));
        assertTrue(admission.take(120));
        assertTrue(admission.take(121));
    }

    @Test
    void refusesBackwardsTicksAndRecoversWithoutAQueue() {
        var admission = new IntentAdmission();
        assertTrue(admission.take(10));
        assertFalse(admission.take(9));
        assertTrue(admission.take(30));
    }
}
