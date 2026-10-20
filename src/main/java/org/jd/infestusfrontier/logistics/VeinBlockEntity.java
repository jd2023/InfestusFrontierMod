package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.storage.api.BiomassFluids;
import org.jd.infestusfrontier.storage.api.BiomassHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The packet one vein cell holds and the side it came from. The cell receives through its capability and never
 * pulls. On each pulse it pushes the packet on, first into the consumers on its other sides, in the order up, down,
 * north, east, south, west, then into the next cells of the vein. A full downstream leaves the packet where it is
 * and the cell tries again every {@value VeinBlock#RETRY_TICKS} ticks.
 */
final class VeinBlockEntity extends BlockEntity implements BiomassHandler {
    private static final List<Direction> ORDER =
            List.of(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final MapCodec<Saved> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Direction.CODEC.optionalFieldOf("from").forGetter(Saved::from),
                    SimpleFluidContent.CODEC
                            .optionalFieldOf("fluid", SimpleFluidContent.EMPTY)
                            .forGetter(Saved::fluid),
                    ItemStack.OPTIONAL_CODEC
                            .optionalFieldOf("items", ItemStack.EMPTY)
                            .forGetter(Saved::items))
            .apply(instance, Saved::new));

    private final Carrier carrier;
    private Optional<Direction> from = Optional.empty();
    private Optional<CompoundTag> unreadable = Optional.empty();
    private final FluidTank fluid;
    private final ItemStackHandler items;

    VeinBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsModule.VEIN_ENTITY.get(), pos, state);
        this.carrier = ((VeinBlock) state.getBlock()).carrier();
        this.fluid = new FluidTank(carrier.isFluid() ? carrier.packet() : 0, this::takesFluid) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        this.items = new ItemStackHandler(1) {
            @Override
            public int getSlotLimit(int slot) {
                return carrier == Carrier.ITEM ? carrier.packet() : 0;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return carrier == Carrier.ITEM && unreadable.isEmpty();
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    private record Saved(Optional<Direction> from, SimpleFluidContent fluid, ItemStack items) {}

    Carrier carrier() {
        return carrier;
    }

    /** Biomass for a Biomass Vein; any other cool fluid for a sealed Fluid Vein. */
    private boolean takesFluid(FluidStack stack) {
        if (unreadable.isPresent()) return false;
        return switch (carrier) {
            case BIOMASS -> BiomassFluids.isBiomass(stack);
            case FLUID ->
                !BiomassFluids.isBiomass(stack)
                        && VascularJunctionBlockEntity.isCool(stack)
                        && getBlockState().getValue(VeinBlock.SEALED);
            case ITEM -> false;
        };
    }

    boolean isEmpty() {
        return unreadable.isEmpty()
                && fluid.isEmpty()
                && items.getStackInSlot(0).isEmpty();
    }

    private boolean holding() {
        return !fluid.isEmpty() || !items.getStackInSlot(0).isEmpty();
    }

    /** The fluid side of the cell as a neighbour on {@code side} sees it: it fills, it never drains. */
    @Nullable IFluidHandler fluidPort(@Nullable Direction side) {
        if (!carrier.isFluid()) return null;
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return fluid.getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return fluid.getCapacity();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return fluid.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                int filled = fluid.fill(resource, action);
                if (filled > 0 && action.execute()) received(side);
                return filled;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    /** The item side of the cell as a neighbour on {@code side} sees it: it takes one kind, it never gives. */
    @Nullable IItemHandler itemPort(@Nullable Direction side) {
        if (carrier != Carrier.ITEM) return null;
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(0);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                ItemStack rest = items.insertItem(0, stack, simulate);
                if (rest.getCount() < stack.getCount() && !simulate) received(side);
                return rest;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(0);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return items.isItemValid(0, stack);
            }
        };
    }

    private void received(@Nullable Direction side) {
        from = Optional.ofNullable(side);
        setChanged();
        wake();
    }

    /** Schedules the next pulse when the cell holds something and none is scheduled. */
    void wake() {
        if (level == null || level.isClientSide || !holding()) return;
        if (!level.getBlockTicks()
                .hasScheduledTick(worldPosition, getBlockState().getBlock()))
            level.scheduleTick(worldPosition, getBlockState().getBlock(), carrier.pulseTicks());
    }

    /** From inside a pulse, where the running tick still counts as scheduled: always schedules the next one. */
    private void reschedule(int ticks) {
        if (level == null || level.isClientSide || !holding()) return;
        level.scheduleTick(worldPosition, getBlockState().getBlock(), ticks);
    }

    /** One try to push the packet on. Six capability reads at most, all in loaded chunks. */
    void pulse() {
        if (level == null || unreadable.isPresent() || !holding()) return;
        boolean moved = false;
        for (boolean toVeins : new boolean[] {false, true}) {
            for (Direction side : ORDER) {
                if (from.isPresent() && from.get() == side) continue;
                BlockPos next = worldPosition.relative(side);
                if (!level.hasChunkAt(next)) continue;
                boolean vein = level.getBlockState(next).getBlock() instanceof VeinBlock;
                if (vein != toVeins) continue;
                moved |= push(side, next);
                if (!holding()) return;
            }
        }
        reschedule(moved ? carrier.pulseTicks() : VeinBlock.RETRY_TICKS);
    }

    private boolean push(Direction side, BlockPos next) {
        if (carrier.isFluid()) {
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, next, side.getOpposite());
            if (target == null) return false;
            return !FluidUtil.tryFluidTransfer(target, fluid, carrier.packet(), true)
                    .isEmpty();
        }
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, next, side.getOpposite());
        if (target == null) return false;
        ItemStack packet = items.getStackInSlot(0);
        ItemStack rest = ItemHandlerHelper.insertItem(target, packet.copy(), false);
        if (rest.getCount() == packet.getCount()) return false;
        items.setStackInSlot(0, rest);
        return true;
    }

    /** Spills the item packet when the cell is broken; the fluid packet travels on the item instead. */
    void dropItems(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, new SimpleContainer(items.getStackInSlot(0)));
        items.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        wake();
    }

    @Override
    public BiomassTank tank() {
        int stored = carrier == Carrier.BIOMASS ? fluid.getFluidAmount() : 0;
        return new BiomassTank(
                Biomass.of(stored), Biomass.ZERO, Biomass.of(carrier == Carrier.BIOMASS ? carrier.packet() : 0));
    }

    @Override
    public Outcome<BiomassTank> insert(Biomass amount, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (carrier != Carrier.BIOMASS) return Outcome.refused(BiomassFluids.WRONG_FLUID);
        if (fluid.fill(BiomassFluids.of(amount), IFluidHandler.FluidAction.SIMULATE) < amount.units())
            return Outcome.refused(BiomassTank.FULL);
        if (!simulate) {
            fluid.fill(BiomassFluids.of(amount), IFluidHandler.FluidAction.EXECUTE);
            received(null);
        }
        return Outcome.done(tank());
    }

    @Override
    public Outcome<BiomassTank> extract(Biomass amount, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (carrier != Carrier.BIOMASS || fluid.getFluidAmount() < amount.units())
            return Outcome.refused(BiomassTank.EMPTY);
        if (!simulate) fluid.drain(amount.units(), IFluidHandler.FluidAction.EXECUTE);
        return Outcome.done(tank());
    }

    /** Why {@code saved} cannot be this cell's packet, or empty when it can. */
    private Optional<String> check(Saved saved) {
        if (saved.fluid().getAmount() > fluid.getCapacity())
            return Optional.of(saved.fluid().getAmount() + " mB in a " + fluid.getCapacity() + " mB cell");
        if (!saved.fluid().isEmpty() && !takesFluidUnsealed(saved.fluid().copy()))
            return Optional.of("a fluid this vein does not carry");
        if (saved.items().getCount() > items.getSlotLimit(0))
            return Optional.of(saved.items().getCount() + " items in a cell of " + items.getSlotLimit(0));
        return Optional.empty();
    }

    /** The carrier rule without the seal: a sealed vein that lost its seal keeps its packet unreadable, not lost. */
    private boolean takesFluidUnsealed(FluidStack stack) {
        return switch (carrier) {
            case BIOMASS -> BiomassFluids.isBiomass(stack);
            case FLUID -> !BiomassFluids.isBiomass(stack) && VascularJunctionBlockEntity.isCool(stack);
            case ITEM -> false;
        };
    }

    private void restore(Saved saved) {
        from = saved.from();
        fluid.setFluid(saved.fluid().copy());
        items.setStackInSlot(0, saved.items().copy());
    }

    private Saved saved() {
        return new Saved(
                from,
                SimpleFluidContent.copyOf(fluid.getFluid()),
                items.getStackInSlot(0).copy());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<Saved> read =
                Persistence.read(SAVED, tag, registries, LogisticsModule.LOGGER, carrier.id(), this::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(this::restore);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, saved(), registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent()) {
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
            return;
        }
        if (!fluid.isEmpty())
            components.set(LogisticsModule.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(fluid.getFluid()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        SimpleFluidContent carried = input.get(LogisticsModule.FLUID_CONTENT.get());
        if (carried == null) return;
        Saved saved = new Saved(Optional.empty(), carried, ItemStack.EMPTY);
        Optional<String> problem = check(saved);
        if (problem.isPresent()) {
            LogisticsModule.LOGGER.warn("{} keeps unreadable carried state: {}", carrier.id(), problem.get());
            unreadable = Optional.of(
                    level == null ? new CompoundTag() : Persistence.encode(SAVED, saved, level.registryAccess()));
            return;
        }
        restore(saved);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("fluid");
        tag.remove("from");
        tag.remove("items"); // the item packet drops; it never rides along
    }
}
