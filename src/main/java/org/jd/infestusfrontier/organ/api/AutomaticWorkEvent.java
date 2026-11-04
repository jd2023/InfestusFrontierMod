package org.jd.infestusfrontier.organ.api;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * Posted on the NeoForge bus once each time an organ does a unit of work for its {@link Steward}: a seed planted, a
 * ripe crop cut, a batch finished on food a pipe delivered. Failed and paused work posts nothing.
 */
public final class AutomaticWorkEvent extends Event {
    private final ServerLevel level;
    private final ResourceLocation organ;
    private final UUID steward;

    public AutomaticWorkEvent(ServerLevel level, ResourceLocation organ, UUID steward) {
        this.level = level;
        this.organ = organ;
        this.steward = steward;
    }

    public ServerLevel level() {
        return level;
    }

    public ResourceLocation organ() {
        return organ;
    }

    public UUID steward() {
        return steward;
    }
}
