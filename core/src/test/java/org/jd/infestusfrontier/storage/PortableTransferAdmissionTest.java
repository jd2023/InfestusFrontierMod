package org.jd.infestusfrontier.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jd.infestusfrontier.storage.api.PortableTransferAdmission;
import org.junit.jupiter.api.Test;

final class PortableTransferAdmissionTest {
    @Test
    void enforcesPlayerAndServerBoundsWithoutQueuing() {
        var admission = new PortableTransferAdmission<Integer>();
        for (int i = 0; i < 4; i++) {
            assertEquals(PortableTransferAdmission.Result.ACCEPTED, admission.take(1, 0));
        }
        assertEquals(PortableTransferAdmission.Result.PLAYER_LIMIT, admission.take(1, 19));
        assertEquals(PortableTransferAdmission.Result.ACCEPTED, admission.take(1, 20));

        var global = new PortableTransferAdmission<Integer>();
        for (int player = 0; player < 64; player++) {
            assertEquals(PortableTransferAdmission.Result.ACCEPTED, global.take(player, 100));
        }
        assertEquals(PortableTransferAdmission.Result.SERVER_LIMIT, global.take(64, 100));
        assertEquals(PortableTransferAdmission.Result.ACCEPTED, global.take(64, 101));
    }
}
