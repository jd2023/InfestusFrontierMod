package org.jd.infestusfrontier.storage.api;

import java.util.LinkedHashMap;
import java.util.Objects;
import org.jd.infestusfrontier.foundation.TickQuota;

/** Bounded portable-transfer admission shared by every tank in one server. */
public final class PortableTransferAdmission<K> {
    public static final int TRANSFERS_PER_SERVER_TICK = 64;
    public static final int TRANSFERS_PER_PLAYER_WINDOW = 4;
    public static final int WINDOW_TICKS = 20;
    private static final int MAX_PLAYERS = 1_024;

    private final TickQuota server = new TickQuota(TRANSFERS_PER_SERVER_TICK);
    private final LinkedHashMap<K, Window> players = new LinkedHashMap<>();

    public Result take(K player, long tick) {
        Objects.requireNonNull(player, "player");
        for (int removed = 0; removed < 64 && !players.isEmpty(); removed++) {
            if (tick - players.firstEntry().getValue().lastAdmission < WINDOW_TICKS) break;
            players.pollFirstEntry();
        }
        var window = players.get(player);
        if (window == null) {
            if (players.size() == MAX_PLAYERS) return Result.SERVER_LIMIT;
            window = new Window();
        }
        if (!window.take(tick)) return Result.PLAYER_LIMIT;
        players.remove(player);
        players.put(player, window);
        return server.take(tick) ? Result.ACCEPTED : Result.SERVER_LIMIT;
    }

    private static final class Window {
        private final long[] admissions = new long[TRANSFERS_PER_PLAYER_WINDOW];
        private int first;
        private int size;
        private long lastAdmission = Long.MIN_VALUE;

        boolean take(long tick) {
            if (tick < 0 || tick < lastAdmission) return false;
            while (size > 0 && tick - admissions[first] >= WINDOW_TICKS) {
                first = (first + 1) % admissions.length;
                size--;
            }
            if (size == admissions.length) return false;
            admissions[(first + size) % admissions.length] = tick;
            size++;
            lastAdmission = tick;
            return true;
        }
    }

    public enum Result { ACCEPTED, PLAYER_LIMIT, SERVER_LIMIT }
}
