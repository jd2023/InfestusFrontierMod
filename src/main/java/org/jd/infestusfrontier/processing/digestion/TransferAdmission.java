package org.jd.infestusfrontier.processing.digestion;

import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import org.jd.infestusfrontier.storage.api.PortableTransferAdmission;

/** Server identity owns the cross-dimension portable transaction budget. */
final class TransferAdmission {
    private static final WeakHashMap<MinecraftServer, PortableTransferAdmission<UUID>> SERVERS = new WeakHashMap<>();

    static PortableTransferAdmission.Result take(MinecraftServer server, UUID player) {
        return SERVERS.computeIfAbsent(server, ignored -> new PortableTransferAdmission<>())
                .take(player, Integer.toUnsignedLong(server.getTickCount()));
    }

    private TransferAdmission() {}
}
