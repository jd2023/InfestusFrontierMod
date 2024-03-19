package org.jd.infestusfrontier.network;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

public class BiomassStorage {

    private static final Logger LOGGER = LogUtils.getLogger();
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

    public void addTotalCapacity(int additionalCapacity) {
        this.totalCapacity += additionalCapacity;
    }

    public void reduceTotalCapacity(int additionalCapacity) {
        this.totalCapacity -= additionalCapacity;
        if (this.totalCapacity < 0) {
            this.totalCapacity = 0;
        }
    }

    public void addBiomass(int amount) {
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
