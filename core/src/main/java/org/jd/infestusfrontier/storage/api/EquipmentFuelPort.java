package org.jd.infestusfrontier.storage.api;

import java.util.Objects;

/** Bladder-side port; equipment owns compatibility and its reserved fuel destination. */
public final class EquipmentFuelPort {
    private final QuantityStore source;

    public EquipmentFuelPort(QuantityStore source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    public Result fill(Target target) {
        Objects.requireNonNull(target, "target");
        int available = source.availableFluid(BiomassTransfer.BIOMASS);
        var targetReservation = target.reserveBiomass(available);
        if (targetReservation == null) return Result.INCOMPATIBLE;
        if (available == 0) {
            targetReservation.cancel();
            return Result.EMPTY_SOURCE;
        }
        if (targetReservation.amount() < 1 || targetReservation.amount() > available) {
            targetReservation.cancel();
            return Result.INVALID_TARGET;
        }
        var sourceReservation = BiomassTransfer.reserveExtract(source, targetReservation.amount());
        if (!(sourceReservation instanceof BiomassTransfer.Pending pending)) {
            targetReservation.cancel();
            return Result.EMPTY_SOURCE;
        }
        if (pending.commit() != BiomassTransfer.CommitResult.COMMITTED) {
            targetReservation.cancel();
            return Result.EMPTY_SOURCE;
        }
        targetReservation.commit();
        return Result.FILLED;
    }

    @FunctionalInterface
    public interface Target {
        FuelReservation reserveBiomass(int available);

        static Target incompatible() {
            return available -> null;
        }
    }

    public interface FuelReservation {
        int amount();
        /** Finalizes destination ownership after source commit; it must not refuse or throw. */
        void commit();
        void cancel();
    }

    public enum Result { FILLED, INCOMPATIBLE, EMPTY_SOURCE, INVALID_TARGET, RATE_LIMITED }
}
