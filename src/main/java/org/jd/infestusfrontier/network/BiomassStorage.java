package org.jd.infestusfrontier.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class BiomassStorage {
    private int totalCapacity;
    private int usedCapacity;

    public BiomassStorage(int totalCapacity, int usedCapacity) {
        this.totalCapacity = totalCapacity;
        this.usedCapacity = usedCapacity;
    }

    public static BiomassStorage load(CompoundTag biomass) {
        return new BiomassStorage(biomass.getInt("totalCapacity"), biomass.getInt("usedCapacity"));
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public int getUsedCapacity() {
        return usedCapacity;
    }

    public void addTotalCapacity(String networkId, int additionalCapacity) {
        this.totalCapacity += additionalCapacity;
    }

    public void reduceTotalCapacity(String networkId, int additionalCapacity) {
        this.totalCapacity -= additionalCapacity;
        if (this.totalCapacity < 0) {
            this.totalCapacity = 0;
        }
    }

    public void addBiomass(String networkId, int amount) {
        this.usedCapacity += amount;
        if (this.usedCapacity > this.totalCapacity) {
            this.usedCapacity = this.totalCapacity;
        }
    }

    public void reduceBiomass(String networkId, int amount) {
        this.usedCapacity -= amount;
        if (this.usedCapacity < 0) {
            this.usedCapacity = 0;
        }
    }

    public Tag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("totalCapacity", this.totalCapacity);
        nbt.putInt("usedCapacity", this.usedCapacity);
        return nbt;
    }
}
