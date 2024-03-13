package org.jd.infestusfrontier.network;

public class Summary {
    int totalCapacity;
    int usedCapacity;

    public Summary(int totalCapacity, int usedCapacity) {
        this.totalCapacity = totalCapacity;
        this.usedCapacity = usedCapacity;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public int getUsedCapacity() {
        return usedCapacity;
    }
}
