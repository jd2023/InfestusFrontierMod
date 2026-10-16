package org.jd.infestusfrontier.storage.api;

import java.util.Optional;
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
import org.jd.infestusfrontier.foundation.Refusals;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jetbrains.annotations.Nullable;

/**
 * Biomass as the NeoForge fluid {@code infestusfrontier:biomass}. One BU is one millibucket. Every organ that holds
 * biomass exposes {@link #handler} as its fluid capability, so vanilla buckets, other mods' pipes and
 * {@link FluidUtil} work on it unchanged. Every organ block calls {@link #useOn} first from its {@code useItemOn}.
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
     * Moves fluid between the container in the player's hand and the block at {@code pos}, the way any fluid tank
     * does. Passes to the default block interaction when the stack is not a fluid container. A refused move names
     * its reason to the player.
     */
    public static ItemInteractionResult useOn(
            ItemStack stack, Level level, BlockPos pos, @Nullable Direction face, Player player, InteractionHand hand) {
        IFluidHandlerItem container = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (container == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        BiomassHandler store = level.getCapability(BiomassCapabilities.BLOCK, pos, face);
        if (store == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, face)) return ItemInteractionResult.CONSUME;
        refusal(container, store).ifPresent(refusal -> Refusals.show(player, refusal));
        return ItemInteractionResult.FAIL;
    }

    /** Why a whole bucket cannot move between {@code container} and {@code store}; empty when the fluids allow it. */
    private static Optional<Refusal> refusal(IFluidHandlerItem container, BiomassHandler store) {
        FluidStack held = container.getFluidInTank(0);
        Outcome<?> move;
        if (held.isEmpty()) move = store.extract(Biomass.BUCKET, true);
        else if (isBiomass(held)) move = store.insert(Biomass.BUCKET, true);
        else return Optional.of(WRONG_FLUID);
        return move instanceof Outcome.Refused<?> refused ? Optional.of(refused.refusal()) : Optional.empty();
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
}
