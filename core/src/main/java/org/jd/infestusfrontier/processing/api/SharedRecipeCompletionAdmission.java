package org.jd.infestusfrontier.processing.api;

import java.util.Objects;
import java.util.function.LongSupplier;
import org.jd.infestusfrontier.foundation.TickQuota;

/** One server-scoped completion admission shared by every recipe organ in every dimension. */
public final class SharedRecipeCompletionAdmission implements BatchWork.CompletionAdmission {
    public static final int COMPLETIONS_PER_SERVER_TICK = 16;

    private final LongSupplier serverTick;
    private final TickQuota quota = new TickQuota(COMPLETIONS_PER_SERVER_TICK);

    public SharedRecipeCompletionAdmission(LongSupplier serverTick) {
        this.serverTick = Objects.requireNonNull(serverTick, "serverTick");
    }

    @Override
    public boolean take() {
        return quota.take(serverTick.getAsLong());
    }
}
