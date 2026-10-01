package org.jd.infestusfrontier.storage.api;

import java.util.Map;
import java.util.Objects;

/** Exact, reversible bucket reservations against one finite biomass tank. */
public final class BiomassTransfer {
    public static final String BIOMASS = "biomass";
    public static final int BUCKET_AMOUNT = 1_000;

    public static ReserveResult reserveBucketFill(QuantityStore source) {
        return reserve(source, new QuantityStore.ReservationRequest(
                Map.of(), Map.of(BIOMASS, BUCKET_AMOUNT), Map.of(), Map.of()));
    }

    public static ReserveResult reserveBucketEmpty(QuantityStore destination) {
        return reserve(destination, new QuantityStore.ReservationRequest(
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(BIOMASS, BUCKET_AMOUNT)));
    }

    static ReserveResult reserveExtract(QuantityStore source, int amount) {
        if (amount < 1) throw new IllegalArgumentException("Transfer amount must be positive");
        return reserve(source, new QuantityStore.ReservationRequest(
                Map.of(), Map.of(BIOMASS, amount), Map.of(), Map.of()));
    }

    private static ReserveResult reserve(QuantityStore store, QuantityStore.ReservationRequest request) {
        Objects.requireNonNull(store, "store");
        var preview = store.preview(request, store.revision());
        if (preview instanceof QuantityStore.PreviewRefused refused) {
            return new Refused(refused.reason());
        }
        var reserved = store.reserve(request, store.revision());
        if (reserved instanceof QuantityStore.ReservationRefused refused) {
            return new Refused(refused.reason());
        }
        return new Pending(store, ((QuantityStore.Reserved) reserved).reservationId());
    }

    public sealed interface ReserveResult permits Pending, Refused {}

    public record Refused(QuantityStore.ReserveRefusal reason) implements ReserveResult {}

    public static final class Pending implements ReserveResult {
        private final QuantityStore store;
        private final long reservationId;
        private boolean finished;

        private Pending(QuantityStore store, long reservationId) {
            this.store = store;
            this.reservationId = reservationId;
        }

        public CommitResult commit() {
            if (finished) return CommitResult.ALREADY_FINISHED;
            finished = true;
            return store.commit(reservationId) == QuantityStore.CommitResult.COMMITTED
                    ? CommitResult.COMMITTED : CommitResult.ALREADY_FINISHED;
        }

        public CancelResult cancel() {
            if (finished) return CancelResult.ALREADY_FINISHED;
            finished = true;
            return store.release(reservationId) == QuantityStore.ReleaseResult.RELEASED
                    ? CancelResult.CANCELLED : CancelResult.ALREADY_FINISHED;
        }
    }

    public enum CommitResult { COMMITTED, ALREADY_FINISHED }
    public enum CancelResult { CANCELLED, ALREADY_FINISHED }

    private BiomassTransfer() {}
}
