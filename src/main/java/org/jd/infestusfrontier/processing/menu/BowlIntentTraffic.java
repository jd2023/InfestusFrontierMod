package org.jd.infestusfrontier.processing.menu;

import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import org.jd.infestusfrontier.foundation.TickQuota;

/** One shared 64-intent admission per server tick, without a retry queue. */
final class BowlIntentTraffic {
    private static final WeakHashMap<MinecraftServer, TickQuota> SERVERS = new WeakHashMap<>();
    static boolean take(MinecraftServer server) {
        return SERVERS.computeIfAbsent(server, ignored -> new TickQuota(64))
                .take(Integer.toUnsignedLong(server.getTickCount()));
    }
    private BowlIntentTraffic() {}
}
