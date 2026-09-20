package org.jd.infestusfrontier.interaction.api;

import java.util.LinkedHashMap;
import org.jd.infestusfrontier.foundation.TickQuota;

/** Server-thread admission across menus, targets and reconnects. No queued work. */
public final class ServerIntentAdmission<K> {
    private static final int MAX_PLAYERS = 1024;
    private final LinkedHashMap<K, Window> players = new LinkedHashMap<>();
    private final TickQuota server = new TickQuota(64);

    public Result take(K player, long tick) {
        // Expiry order follows last admission. At most 64 removals per request.
        for (int i = 0; i < 64 && !players.isEmpty(); i++) {
            if (tick - players.firstEntry().getValue().lastAdmission < IntentAdmission.WINDOW_TICKS) break;
            players.pollFirstEntry();
        }
        var window = players.get(player);
        if (window == null) {
            if (players.size() == MAX_PLAYERS) return Result.SERVER_LIMIT;
            window = new Window();
        }
        if (!window.admission.take(tick)) return Result.PLAYER_LIMIT;
        players.remove(player);
        window.lastAdmission = tick;
        players.put(player, window);
        return server.take(tick) ? Result.ACCEPTED : Result.SERVER_LIMIT;
    }

    private static final class Window {
        final IntentAdmission admission = new IntentAdmission();
        long lastAdmission;
    }
    public enum Result { ACCEPTED, PLAYER_LIMIT, SERVER_LIMIT }
}
