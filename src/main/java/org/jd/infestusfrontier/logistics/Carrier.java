package org.jd.infestusfrontier.logistics;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/** Compatible media for each passive network. */
enum Carrier implements StringRepresentable {
    BIOMASS("biomass_vein"),
    FLUID("fluid_vein"),
    ITEM("item_vein");

    private final String id;

    Carrier(String id) {
        this.id = id;
    }

    String id() {
        return id;
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
