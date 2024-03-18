package org.jd.infestusfrontier.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class Network {
    private static final String BIOMASS_STORAGE = "biomass_storage";
    private BiomassStorage biomassStorage;

    public BiomassStorage getBiomassStorage() {
        return biomassStorage;
    }

    public void setBiomassStorage(BiomassStorage biomassStorage) {
        this.biomassStorage = biomassStorage;
    }

    public Tag save() {
        CompoundTag biomass = new CompoundTag();
        biomass.put(BIOMASS_STORAGE, this.biomassStorage.save());
        return biomass;
    }

    public static Network load(CompoundTag compound) {
        Network network = new Network();
        CompoundTag biomass = compound.getCompound(BIOMASS_STORAGE);
        network.setBiomassStorage(BiomassStorage.load(biomass));
        return network;
    }

    public void dump(StringBuilder builder) {
        builder.append("{")
                .append("biomassStorageTotalCapacity=")
                .append(this.biomassStorage.getTotalCapacity())
                .append(", biomassStorageUsedCapacity=")
                .append(this.biomassStorage.getUsedCapacity())
                .append("}, ");
    }
}
