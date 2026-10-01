package org.jd.infestusfrontier.interaction.api;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Platform port implemented by an organ that exposes a probe menu. */
public interface ProbeTarget {
    UUID probeOwner();
    boolean claimProbeOwner(UUID player);
    void openProbeMenu(ServerPlayer player);
}
