package org.jd.infestusfrontier.integration;

import com.mojang.logging.LogUtils;
import java.util.Comparator;
import java.util.stream.Collectors;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

/** Emits the loader-owned mod IDs and versions used by bootstrap evidence. */
public final class RuntimeIdentity {
    private static final Logger LOGGER = LogUtils.getLogger();

    private RuntimeIdentity() {}

    public static void install() {
        if (!Boolean.getBoolean("infestus.integration")) return;
        NeoForge.EVENT_BUS.addListener(RuntimeIdentity::playerJoined);
        NeoForge.EVENT_BUS.addListener(RuntimeIdentity::playerLeft);
        logLoadedMods();
    }

    private static void logLoadedMods() {
        String identities = ModList.get().getMods().stream()
                .sorted(Comparator.comparing(info -> info.getModId()))
                .map(info -> info.getModId() + "=" + info.getVersion())
                .collect(Collectors.joining(","));
        LOGGER.info("INFESTUS_INTEGRATION_RUNTIME {}", identities);
    }

    private static void playerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        LOGGER.info("INFESTUS_INTEGRATION_PLAYER_JOIN name={} uuid={}",
                event.getEntity().getGameProfile().getName(), event.getEntity().getUUID());
    }

    private static void playerLeft(PlayerEvent.PlayerLoggedOutEvent event) {
        LOGGER.info("INFESTUS_INTEGRATION_PLAYER_LEAVE name={} uuid={}",
                event.getEntity().getGameProfile().getName(), event.getEntity().getUUID());
    }
}
