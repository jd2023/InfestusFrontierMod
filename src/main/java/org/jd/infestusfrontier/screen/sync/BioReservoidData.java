package org.jd.infestusfrontier.screen.sync;

import net.minecraft.world.inventory.SimpleContainerData;
import org.jd.infestusfrontier.block.entity.BioReservoirBlockEntity;

public class BioReservoidData extends SimpleContainerData {
    private final BioReservoirBlockEntity reservoirBlockEntity;

    public BioReservoidData(BioReservoirBlockEntity reservoirBlockEntity, int size) {
        super(size);
        this.reservoirBlockEntity = reservoirBlockEntity;
    }

    @Override
    public int get(int index) {
        return switch (index) {
            case 0 -> this.reservoirBlockEntity.progress;
            case 1 -> this.reservoirBlockEntity.maxProgress;
            default -> throw new IllegalArgumentException("Unknown data index: " + index);
        };
    }

    @Override
    public void set(int index, int value) {
        switch (index) {
            case 0 -> this.reservoirBlockEntity.progress = value;
            case 1 -> this.reservoirBlockEntity.maxProgress = value;
            default -> throw new IllegalArgumentException("Unknown data index: " + index);
        }
    }
}
