package org.jd.infestusfrontier.processing.menu;

import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import org.jd.infestusfrontier.interaction.api.ServerIntentAdmission;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/** One shared 64-intent admission per server tick, without a retry queue. */
final class BowlIntentTraffic {
    private static final WeakHashMap<MinecraftServer, ServerIntentAdmission<UUID>> SERVERS = new WeakHashMap<>();
    static ServerIntentAdmission.Result take(ServerPlayer player) {
        return SERVERS.computeIfAbsent(player.server, ignored -> new ServerIntentAdmission<>())
                .take(player.getUUID(), Integer.toUnsignedLong(player.server.getTickCount()));
    }
    private BowlIntentTraffic() {}
}
