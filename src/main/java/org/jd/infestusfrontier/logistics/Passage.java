package org.jd.infestusfrontier.logistics;

import java.util.function.Supplier;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The fluid side of a block that holds nothing: a fill passes straight on to the handler at the passage's far end,
 * found again on every call, so a cached handler never goes stale. It never drains and never holds fluid.
 */
final class Passage implements IFluidHandler {
    private final Carrier carrier;
    private final Supplier<@Nullable IFluidHandler> far;

    Passage(Carrier carrier, Supplier<@Nullable IFluidHandler> far) {
        this.carrier = carrier;
        this.far = far;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return carrier.packet();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return carrier.carries(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !carrier.carries(resource)) return 0;
        IFluidHandler target = far.get();
        return target == null ? 0 : target.fill(resource, action);
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
