package org.jd.infestusfrontier.screen.sync;

import net.minecraft.world.inventory.SimpleContainerData;
import org.jd.infestusfrontier.block.entity.CorruptionCoreBlockEntity;

public class CorruptionCoreData extends SimpleContainerData {
    private final CorruptionCoreBlockEntity reservoirBlockEntity;

    public CorruptionCoreData(CorruptionCoreBlockEntity reservoirBlockEntity, int size) {
        super(size);
        this.reservoirBlockEntity = reservoirBlockEntity;
    }

    @Override
    public int get(int index) {
        return switch (index) {
            case 0 -> this.reservoirBlockEntity.progress;
            case 1 -> this.reservoirBlockEntity.maxProgress;
            case 2 -> this.reservoirBlockEntity.biomass;
            default -> throw new IllegalArgumentException("Unknown data index: " + index);
        };
    }

    @Override
    public void set(int index, int value) {
        switch (index) {
            case 0: this.reservoirBlockEntity.progress = value;
            case 1: this.reservoirBlockEntity.maxProgress = value;
            case 2:
                    if (value > 70) {
                        this.reservoirBlockEntity.biomass = 70;
                    }else {
                        this.reservoirBlockEntity.biomass = value;
                    }
            default: throw new IllegalArgumentException("Unknown data index: " + index);
        }
    }
}
