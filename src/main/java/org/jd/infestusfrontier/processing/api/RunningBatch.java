package org.jd.infestusfrontier.processing.api;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/** What every organ's running batch can do. The lifecycle in {@code processing} drives it the same way for each. */
public interface RunningBatch<T extends RunningBatch<T>> {
    /** One loaded tick of work. */
    T advance();

    boolean isDone();

    /** The player who started it, when one did. */
    Optional<UUID> starter();

    /** The recipe the batch runs, when it names one that a data pack can address. */
    default Optional<ResourceLocation> recipe() {
        return Optional.empty();
    }
}
