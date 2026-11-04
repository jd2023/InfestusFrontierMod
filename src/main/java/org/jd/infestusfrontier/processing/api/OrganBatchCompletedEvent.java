package org.jd.infestusfrontier.processing.api;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * Posted on the NeoForge bus once when an organ finishes a batch that a player started. It names the recipe the batch
 * ran when there is one, so an advancement can tell a grinding from a washing in the same organ.
 */
public final class OrganBatchCompletedEvent extends Event {
    private final ServerLevel level;
    private final ResourceLocation organ;
    private final UUID feeder;
    private final Optional<ResourceLocation> recipe;

    public OrganBatchCompletedEvent(ServerLevel level, ResourceLocation organ, UUID feeder) {
        this(level, organ, feeder, Optional.empty());
    }

    public OrganBatchCompletedEvent(
            ServerLevel level, ResourceLocation organ, UUID feeder, Optional<ResourceLocation> recipe) {
        this.level = level;
        this.organ = organ;
        this.feeder = feeder;
        this.recipe = recipe;
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

    public Optional<ResourceLocation> recipe() {
        return recipe;
    }
}
