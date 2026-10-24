package org.jd.infestusfrontier.logistics;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/** What a vein carries, and how big one fluid packet is in millibuckets. An Item Vein holds nothing itself. */
enum Carrier implements StringRepresentable {
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

    /** Whether this carrier ever carries {@code stack}: biomass, or any other cool fluid; never for items. */
    boolean carries(FluidStack stack) {
        return switch (this) {
            case BIOMASS -> BiomassFluids.isBiomass(stack);
            case FLUID -> !BiomassFluids.isBiomass(stack) && VascularJunctionBlockEntity.isCool(stack);
            case ITEM -> false;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
