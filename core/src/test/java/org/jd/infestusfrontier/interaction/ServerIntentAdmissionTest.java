package org.jd.infestusfrontier.interaction;

import org.jd.infestusfrontier.interaction.api.ServerIntentAdmission;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerIntentAdmissionTest {
    @Test void playerWindowSurvivesMenuAndTargetChanges() {
        var admission = new ServerIntentAdmission<String>();
        for (int i = 0; i < 4; i++) assertEquals(ServerIntentAdmission.Result.ACCEPTED, admission.take("player", 0));
        assertEquals(ServerIntentAdmission.Result.PLAYER_LIMIT, admission.take("player", 19));
        assertEquals(ServerIntentAdmission.Result.ACCEPTED, admission.take("player", 20));
    }
    @Test void allPlayersShareExactly64IntentsPerTick() {
        var admission = new ServerIntentAdmission<Integer>();
        for (int i = 0; i < 64; i++) assertEquals(ServerIntentAdmission.Result.ACCEPTED, admission.take(i, 0));
        assertEquals(ServerIntentAdmission.Result.SERVER_LIMIT, admission.take(64, 0));
        assertEquals(ServerIntentAdmission.Result.ACCEPTED, admission.take(64, 1));
    }
    @Test void tableRefusesOverflowAndReclaimsExpiredPlayersInBoundedSteps() {
        var admission = new ServerIntentAdmission<Integer>();
        for (int tick = 0; tick < 16; tick++) for (int i = 0; i < 64; i++)
            assertEquals(ServerIntentAdmission.Result.ACCEPTED, admission.take(tick * 64 + i, tick));
        assertEquals(ServerIntentAdmission.Result.SERVER_LIMIT, admission.take(1024, 16));
        assertEquals(ServerIntentAdmission.Result.ACCEPTED, admission.take(1024, 20));
        assertEquals(ServerIntentAdmission.Result.ACCEPTED, admission.take(0, 40));
    }
}
