package org.jd.infestusfrontier.storage.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jetbrains.annotations.Nullable;

/**
 * Biomass as the NeoForge fluid {@code infestusfrontier:biomass}. One BU is one millibucket. Every organ that holds
 * biomass exposes {@link #handler} as its fluid capability, or {@link #join} when it holds water too, so vanilla
 * buckets, other mods' pipes and {@link FluidUtil} work on it unchanged. Every organ block calls {@link #useOn}
 * first from its {@code useItemOn}.
 */
public final class BiomassFluids {
    public static final Refusal WRONG_FLUID = new Refusal("storage.wrong_fluid");

    private BiomassFluids() {}

    public static boolean isBiomass(FluidStack stack) {
        return stack.is(StorageModule.BIOMASS.get());
    }

    public static FluidStack of(Biomass amount) {
        return amount.isZero() ? FluidStack.EMPTY : new FluidStack(StorageModule.BIOMASS.get(), amount.units());
    }

    /** The fluid view of a biomass balance: one tank, biomass only, partial amounts for pipes. */
    public static IFluidHandler handler(BiomassHandler store) {
        return new Adapter(store);
    }

    /**
     * Several handlers as one block capability, tanks numbered in order. A fill goes to the first part that takes
     * the fluid; a drain by kind comes from the first part holding that fluid; a drain by amount comes from the
     * first part that can give the whole amount, else from the part holding most, so a bucket still takes biomass
     * when less than a bucket of water is left. For organs that hold water and biomass.
     */
    public static IFluidHandler join(IFluidHandler... parts) {
        return new Joined(List.of(parts));
    }

    /**
     * Moves fluid between the container in the player's hand and the block at {@code pos}, the way any fluid tank
     * does. Passes to the default block interaction when the stack is not a fluid container or the block has no
     * fluid capability. A refused move names its reason to the player.
     */
    public static ItemInteractionResult useOn(
            ItemStack stack, Level level, BlockPos pos, @Nullable Direction face, Player player, InteractionHand hand) {
        IFluidHandlerItem container = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (container == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        IFluidHandler block = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face);
        if (block == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, face)) return ItemInteractionResult.CONSUME;
        BiomassHandler store = level.getCapability(BiomassCapabilities.BLOCK, pos, face);
        Refusals.show(player, refusal(container.getFluidInTank(0), block, store));
        return ItemInteractionResult.FAIL;
    }

    /**
     * Why a whole bucket holding {@code held} cannot move into or out of the block. The organ's own biomass rule
     * speaks for biomass; every other fluid is judged by the block's tanks.
     */
    static Refusal refusal(FluidStack held, IFluidHandler block, @Nullable BiomassHandler store) {
        if (held.isEmpty()) {
            if (store != null && store.extract(Biomass.BUCKET, true) instanceof Outcome.Refused<?> refused)
                return refused.refusal();
            return BiomassTank.EMPTY;
        }
        if (isBiomass(held)) {
            if (store != null && store.insert(Biomass.BUCKET, true) instanceof Outcome.Refused<?> refused)
                return refused.refusal();
            return BiomassTank.FULL;
        }
        for (int tank = 0; tank < block.getTanks(); tank++) if (block.isFluidValid(tank, held)) return BiomassTank.FULL;
        return WRONG_FLUID;
    }

    private record Adapter(BiomassHandler store) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return of(store.tank().stored());
        }

        @Override
        public int getTankCapacity(int tank) {
            return store.tank().capacity().units();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return isBiomass(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isBiomass(resource)) return 0;
            int amount = Math.min(resource.getAmount(), store.tank().room().units());
            if (amount <= 0) return 0;
            return store.insert(Biomass.of(amount), action.simulate()) instanceof Outcome.Done<?> ? amount : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return isBiomass(resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            int amount = Math.min(maxDrain, store.tank().stored().units());
            if (amount <= 0) return FluidStack.EMPTY;
            return store.extract(Biomass.of(amount), action.simulate()) instanceof Outcome.Done<?>
                    ? of(Biomass.of(amount))
                    : FluidStack.EMPTY;
        }
    }

    private record Joined(List<IFluidHandler> parts) implements IFluidHandler {
        @Override
        public int getTanks() {
            int tanks = 0;
            for (IFluidHandler part : parts) tanks += part.getTanks();
            return tanks;
        }

        /** The part holding global tank {@code tank}, and the tank's index inside it. */
        private IFluidHandler part(int tank, int[] local) {
            int offset = tank;
            for (IFluidHandler part : parts) {
                if (offset < part.getTanks()) {
                    local[0] = offset;
                    return part;
                }
                offset -= part.getTanks();
            }
            throw new IndexOutOfBoundsException("No tank " + tank);
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            int[] local = new int[1];
            return part(tank, local).getFluidInTank(local[0]);
        }

        @Override
        public int getTankCapacity(int tank) {
            int[] local = new int[1];
            return part(tank, local).getTankCapacity(local[0]);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            int[] local = new int[1];
            return part(tank, local).isFluidValid(local[0], stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            for (IFluidHandler part : parts) {
                int filled = part.fill(resource, action);
                if (filled > 0) return filled;
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            for (IFluidHandler part : parts) {
                FluidStack drained = part.drain(resource, action);
                if (!drained.isEmpty()) return drained;
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            IFluidHandler fullest = null;
            int most = 0;
            for (IFluidHandler part : parts) {
                int available = part.drain(maxDrain, FluidAction.SIMULATE).getAmount();
                if (available == maxDrain) return part.drain(maxDrain, action);
                if (available > most) {
                    most = available;
                    fullest = part;
                }
            }
            return fullest == null ? FluidStack.EMPTY : fullest.drain(maxDrain, action);
        }
    }
}
