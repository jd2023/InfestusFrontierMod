package org.jd.infestusfrontier.equipment;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.equipment.api.Reserves;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/**
 * An awakened piece as a fluid container that takes biomass, so a Bladder or a bucket can feed it through the
 * engine's own item fluid capability. It never drains: spending fuel is the piece's own business.
 */
final class ReserveHandler implements IFluidHandlerItem {
    private final ItemStack piece;

    ReserveHandler(ItemStack piece) {
        this.piece = piece;
    }

    @Override
    public ItemStack getContainer() {
        return piece;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return Reserves.of(piece)
                .map(reserve -> BiomassFluids.of(reserve.stored()))
                .orElse(FluidStack.EMPTY);
    }

    @Override
    public int getTankCapacity(int tank) {
        return Reserves.of(piece).map(reserve -> reserve.capacity().units()).orElse(0);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return BiomassFluids.isBiomass(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!isFluidValid(0, resource)) return 0;
        Biomass offered = Biomass.of(resource.getAmount());
        if (action.simulate())
            return Reserves.of(piece)
                    .map(reserve -> reserve.room().min(offered).units())
                    .orElse(0);
        return Reserves.fill(piece, offered).units();
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return FluidStack.EMPTY;
    }
}
