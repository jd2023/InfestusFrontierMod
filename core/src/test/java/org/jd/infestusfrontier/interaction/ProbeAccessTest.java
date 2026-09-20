package org.jd.infestusfrontier.interaction;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.jd.infestusfrontier.interaction.api.ProbeAccess;
import org.junit.jupiter.api.Test;

final class ProbeAccessTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID STRANGER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void authorizesOnlyLoadedOwnedLocalReachableTargets() {
        assertEquals(ProbeAccess.Refusal.NONE, ProbeAccess.check(OWNER, OWNER, true, true, 24.0, 6.0));
        assertEquals(ProbeAccess.Refusal.WRONG_OWNER, ProbeAccess.check(OWNER, STRANGER, true, true, 24.0, 6.0));
        assertEquals(ProbeAccess.Refusal.UNLOADED_TARGET, ProbeAccess.check(OWNER, OWNER, false, true, 1.0, 6.0));
        assertEquals(ProbeAccess.Refusal.REMOTE_TARGET, ProbeAccess.check(OWNER, OWNER, true, false, 1.0, 6.0));
        assertEquals(ProbeAccess.Refusal.OUT_OF_REACH, ProbeAccess.check(OWNER, OWNER, true, true, 36.01, 6.0));
        assertEquals(ProbeAccess.Refusal.MALFORMED, ProbeAccess.check(OWNER, OWNER, true, true, Double.NaN, 6.0));
    }
}
