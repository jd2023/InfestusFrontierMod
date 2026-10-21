package org.jd.infestusfrontier.logistics;

/** What a vein carries, and how big one fluid packet is in millibuckets. An Item Vein holds nothing itself. */
enum Carrier {
    BIOMASS("biomass_vein", 250, 4),
    FLUID("fluid_vein", 250, 4),
    ITEM("item_vein", 0, 0);

    private final String id;
    private final int packet;
    private final int pulseTicks;

    Carrier(String id, int packet, int pulseTicks) {
        this.id = id;
        this.packet = packet;
        this.pulseTicks = pulseTicks;
    }

    String id() {
        return id;
    }

    /** The most one cell holds and moves on at once. */
    int packet() {
        return packet;
    }

    /** Ticks between two moves of a cell that is moving. */
    int pulseTicks() {
        return pulseTicks;
    }

    boolean isFluid() {
        return this != ITEM;
    }
}
