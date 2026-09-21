package org.jd.infestusfrontier.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import org.jd.infestusfrontier.storage.api.BiomassTransfer;
import org.jd.infestusfrontier.storage.api.EquipmentFuelPort;
import org.jd.infestusfrontier.storage.api.QuantityStore;
import org.junit.jupiter.api.Test;

final class BiomassTransferTest {
    @Test
    void interruptedBucketFillRetainsEveryMillibucket() {
        var tank = tank(4_000, 1_500);
        var pending = assertInstanceOf(BiomassTransfer.Pending.class,
                BiomassTransfer.reserveBucketFill(tank));
        assertEquals(1_500, tank.fluidAmount(BiomassTransfer.BIOMASS));

        assertEquals(BiomassTransfer.CancelResult.CANCELLED, pending.cancel());
        assertEquals(1_500, tank.fluidAmount(BiomassTransfer.BIOMASS));
        assertEquals(0, tank.snapshot().reservations().size());
    }

    @Test
    void bucketMovesExactlyOneThousandBetweenFiniteTanks() {
        var source = tank(4_000, 1_500);
        var destination = tank(4_000, 3_000);
        var fill = assertInstanceOf(BiomassTransfer.Pending.class,
                BiomassTransfer.reserveBucketFill(source));
        assertEquals(BiomassTransfer.CommitResult.COMMITTED, fill.commit());
        assertEquals(500, source.fluidAmount(BiomassTransfer.BIOMASS));

        var empty = assertInstanceOf(BiomassTransfer.Pending.class,
                BiomassTransfer.reserveBucketEmpty(destination));
        assertEquals(BiomassTransfer.CommitResult.COMMITTED, empty.commit());
        assertEquals(4_000, destination.fluidAmount(BiomassTransfer.BIOMASS));

        assertInstanceOf(BiomassTransfer.Refused.class,
                BiomassTransfer.reserveBucketEmpty(destination));
        assertEquals(4_000, destination.fluidAmount(BiomassTransfer.BIOMASS));
    }

    @Test
    void equipmentPortRefusesUnknownEquipmentWithoutInventingArmor() {
        var tank = tank(4_000, 2_000);
        var port = new EquipmentFuelPort(tank);
        assertEquals(EquipmentFuelPort.Result.INCOMPATIBLE,
                port.fill(EquipmentFuelPort.Target.incompatible()));
        assertEquals(2_000, tank.fluidAmount(BiomassTransfer.BIOMASS));
    }

    @Test
    void equipmentPortTransfersOnlyTheTargetsReservedMissingFuel() {
        var tank = tank(4_000, 2_000);
        var accepted = new int[1];
        var target = (EquipmentFuelPort.Target) available -> new EquipmentFuelPort.FuelReservation() {
            @Override public int amount() { return Math.min(750, available); }
            @Override public void commit() { accepted[0] += amount(); }
            @Override public void cancel() { throw new AssertionError("valid reservation cancelled"); }
        };

        assertEquals(EquipmentFuelPort.Result.FILLED, new EquipmentFuelPort(tank).fill(target));
        assertEquals(750, accepted[0]);
        assertEquals(1_250, tank.fluidAmount(BiomassTransfer.BIOMASS));
    }

    private static QuantityStore tank(int capacity, int amount) {
        return new QuantityStore(List.of(), List.of(new QuantityStore.Tank(
                amount == 0 ? "" : BiomassTransfer.BIOMASS, amount, capacity)));
    }
}
