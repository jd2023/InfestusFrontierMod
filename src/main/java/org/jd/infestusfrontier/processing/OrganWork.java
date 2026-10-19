package org.jd.infestusfrontier.processing;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.jd.infestusfrontier.processing.api.OrganBatchCompletedEvent;
import org.jd.infestusfrontier.processing.api.RunningBatch;

/** The batch lifecycle every organ shares: one tick of progress, then one hand-over of the finished batch. */
final class OrganWork {
    private OrganWork() {}

    /**
     * {@code batch} one tick later. A finished batch goes to {@code finish}, which returns true once it took it and
     * false to keep it waiting (no room yet); the organ keeps the finished batch until then.
     */
    static <T extends RunningBatch<T>> Optional<T> tick(Optional<T> batch, Predicate<T> finish) {
        if (batch.isEmpty()) return batch;
        T advanced = batch.get().advance();
        if (advanced.isDone() && finish.test(advanced)) return Optional.empty();
        return Optional.of(advanced);
    }

    /** Tells the world that {@code organ} finished {@code batch}, once, for the player who started it. */
    static void completed(Level level, ResourceLocation organ, RunningBatch<?> batch) {
        if (level instanceof ServerLevel server)
            batch.starter()
                    .ifPresent(
                            starter -> NeoForge.EVENT_BUS.post(new OrganBatchCompletedEvent(server, organ, starter)));
    }
}
