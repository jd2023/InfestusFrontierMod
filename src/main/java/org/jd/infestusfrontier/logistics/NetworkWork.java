package org.jd.infestusfrontier.logistics;

import net.minecraft.server.level.ServerLevel;

/** Shared work admission, held on the overworld for the server and on each dimension for local use. */
final class NetworkWork {
    private long tick = Long.MIN_VALUE;
    private int operations;
    private int discoveries;
    private boolean inside;

    private void at(long now) {
        if (tick == now) return;
        tick = now;
        operations = 0;
        discoveries = 0;
    }

    static boolean take(ServerLevel level) {
        long now = level.getServer().getTickCount();
        NetworkWork local = level.getData(LogisticsModule.NETWORK_WORK);
        NetworkWork total = level.getServer().overworld().getData(LogisticsModule.SERVER_WORK);
        local.at(now);
        total.at(now);
        if (local.operations >= 64 || total.operations >= 256) return false;
        local.operations++;
        total.operations++;
        return true;
    }

    static int discovery(ServerLevel level, int requested) {
        NetworkWork local = level.getData(LogisticsModule.NETWORK_WORK);
        NetworkWork total = level.getServer().overworld().getData(LogisticsModule.SERVER_WORK);
        local.at(level.getServer().getTickCount());
        total.at(level.getServer().getTickCount());
        int allowed = Math.min(requested, Math.min(2048 - local.discoveries, 8192 - total.discoveries));
        return allowed;
    }

    static void discovered(ServerLevel level, int used) {
        level.getData(LogisticsModule.NETWORK_WORK).discoveries += used;
        level.getServer().overworld().getData(LogisticsModule.SERVER_WORK).discoveries += used;
    }

    static boolean enter(ServerLevel level) {
        NetworkWork local = level.getData(LogisticsModule.NETWORK_WORK);
        if (local.inside) return false;
        local.inside = true;
        return true;
    }

    static void leave(ServerLevel level) {
        level.getData(LogisticsModule.NETWORK_WORK).inside = false;
    }
}
