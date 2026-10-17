package org.jd.infestusfrontier.processing.api;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/** Posted on the NeoForge bus once when an organ finishes a batch that a player started. */
public final class OrganBatchCompletedEvent extends Event {
    private final ServerLevel level;
    private final ResourceLocation organ;
    private final UUID feeder;

    public OrganBatchCompletedEvent(ServerLevel level, ResourceLocation organ, UUID feeder) {
        this.level = level;
        this.organ = organ;
        this.feeder = feeder;
    }

    public ServerLevel level() {
        return level;
    }

    public ResourceLocation organ() {
        return organ;
    }

    public UUID feeder() {
        return feeder;
    }
}
