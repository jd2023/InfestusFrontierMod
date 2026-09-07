package org.jd.infestusfrontier.screen.sync;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jd.infestusfrontier.block.entity.CorruptionCoreBlockEntity;

import java.util.List;

public class CorruptionCoreData extends SimpleContainerData {
    private final CorruptionCoreBlockEntity blockEntity;

    public CorruptionCoreData(CorruptionCoreBlockEntity blockEntity, int size) {
        super(size);
        this.blockEntity = blockEntity;
    }

    @Override
    public int get(int index) {
        return switch (index) {
            case 0 -> this.blockEntity.progress;
            case 1 -> this.blockEntity.maxProgress;
            case 2 -> this.blockEntity.biomass;
            case 3 -> this.blockEntity.maxBiomass;
            default -> throw new IllegalArgumentException("Unknown data index: " + index);
        };
    }

    @Override
    public void set(int index, int value) {
        switch (index) {
            case 0: this.blockEntity.progress = value;
            case 1: this.blockEntity.maxProgress = value;
            case 2:
                    if (value > 70) {
                        this.blockEntity.biomass = 70;
                    }else {
                        this.blockEntity.biomass = value;
                    }
            case 3 : this.blockEntity.maxBiomass = value;
            default: throw new IllegalArgumentException("Unknown data index: " + index);
        }
    }

}
