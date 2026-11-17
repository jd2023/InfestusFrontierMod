package org.jd.infestusfrontier.genetics.api;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * Posted on the NeoForge bus once when a DNA Bank's extraction completes a species' genome, for the bank's steward. A
 * bank with no steward posts nothing; a genome completes in one bank only once, since research never goes down.
 */
public final class GenomeCompletedEvent extends Event {
    private final ServerLevel level;
    private final ResourceLocation species;
    private final UUID steward;

    public GenomeCompletedEvent(ServerLevel level, ResourceLocation species, UUID steward) {
        this.level = level;
        this.species = species;
        this.steward = steward;
    }

    public ServerLevel level() {
        return level;
    }

    public ResourceLocation species() {
        return species;
    }

    public UUID steward() {
        return steward;
    }
}
