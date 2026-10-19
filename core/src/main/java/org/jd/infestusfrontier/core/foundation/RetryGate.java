package org.jd.infestusfrontier.core.foundation;

import java.util.Optional;

/**
 * Remembers the state a refused attempt saw, so idle work retries only when something changed. {@code S} is a
 * value: two states are the same when {@code equals} says so.
 */
public final class RetryGate<S> {
    private Optional<S> refusedWith = Optional.empty();

    /** True unless the last attempt was refused with exactly this state. */
    public boolean due(S state) {
        return refusedWith.isEmpty() || !refusedWith.get().equals(state);
    }

    public void refused(S state) {
        refusedWith = Optional.of(state);
    }

    public void clear() {
        refusedWith = Optional.empty();
    }
}
