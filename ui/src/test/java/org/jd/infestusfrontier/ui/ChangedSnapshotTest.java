package org.jd.infestusfrontier.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChangedSnapshotTest {
    @Test void progressAndRefusalsShareCadenceAndIdleStaysSilent() {
        var updates = new ChangedSnapshot<>("idle", 100);
        for (int tick = 101; tick < 110; tick++) assertFalse(updates.take("working" + tick, tick));
        assertTrue(updates.take("working110", 110));
        assertFalse(updates.take("refusal", 111));
        assertTrue(updates.take("idle", 120));
        for (int tick = 121; tick < 1000; tick++) assertFalse(updates.take("idle", tick));
        assertTrue(updates.take("working", 1000));
    }
}
