package org.jd.infestusfrontier.equipment.api;

import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.equipment.EquipmentModule;

/** The biomass an awakened piece carries. Every fuel spend and fill goes through here. */
public final class Reserves {
    private Reserves() {}

    /** The reserve on {@code stack}, or empty when it is not an awakened piece. */
    public static Optional<BiomassTank> of(ItemStack stack) {
        return Optional.ofNullable(stack.get(EquipmentModule.RESERVE.get()));
    }

    /** Puts up to {@code amount} into the piece and returns what it took; nothing for a full piece or a non-piece. */
    public static Biomass fill(ItemStack stack, Biomass amount) {
        Optional<BiomassTank> reserve = of(stack);
        if (reserve.isEmpty()) return Biomass.ZERO;
        Biomass taken = reserve.get().room().min(amount);
        if (taken.isZero()) return Biomass.ZERO;
        Outcome<BiomassTank> filled = reserve.get().fill(taken);
        if (!(filled instanceof Outcome.Done<BiomassTank> done)) return Biomass.ZERO;
        stack.set(EquipmentModule.RESERVE.get(), done.value());
        return taken;
    }
}
