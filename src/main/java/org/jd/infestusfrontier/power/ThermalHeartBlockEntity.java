package org.jd.infestusfrontier.power;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.power.PowerStore;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.organ.api.Steward;
import org.jd.infestusfrontier.organ.api.Stewarded;
import org.jd.infestusfrontier.power.api.SteamDrive;

final class ThermalHeartBlockEntity extends BlockEntity implements MenuProvider, Stewarded, SteamDrive {
    static final int PULSE_TICKS = 20;
    private PowerStore store = PowerStore.EMPTY;
    private Optional<CompoundTag> unreadable = Optional.empty();
    private boolean discharge;
    private int ticks;
    private int stop;
    private final Steward steward = new Steward();
    private final PowerItems items = new PowerItems();

    private final class PowerItems extends ItemStackHandler {
        PowerItems() {
            super(4);
        }

        void restore(int slot, ItemStack stack) {
            super.setStackInSlot(slot, stack.copy());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return live()
                    && unreadable.isEmpty()
                    && !driver()
                    && (slot == 0
                                    && (stack.is(PowerModule.HEAT_CAPSULE.get())
                                            || stack.is(PowerModule.HOT_CAPSULE.get()))
                            || slot == 1
                                    && (stack.is(PowerModule.CELL.get()) || stack.is(PowerModule.CHARGED_CELL.get())));
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return super.getStackInSlot(slot).copy();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return !live() || unreadable.isPresent() ? stack.copy() : super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return !live() || unreadable.isPresent() ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (live() && unreadable.isEmpty() && stack.getCount() <= 1) super.setStackInSlot(slot, stack.copy());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    }

    ThermalHeartBlockEntity(BlockPos pos, BlockState state) {
        super(PowerModule.CORE_ENTITY.get(), pos, state);
    }

    @Override
    public Steward steward() {
        return steward;
    }

    PowerStore store() {
        return store;
    }

    ItemStackHandler items() {
        return items;
    }

    boolean discharge() {
        return discharge;
    }

    boolean driver() {
        return getBlockState().is(PowerModule.DRIVER.get());
    }

    boolean receiver() {
        return getBlockState().is(PowerModule.RECEIVER.get());
    }

    int stop() {
        return unreadable.isPresent() ? 10 : driver() ? !usable() ? 1 : store.steam() < CUT_STEAM ? 7 : 0 : stop;
    }

    void toggle() {
        if (unreadable.isEmpty() && !driver()) {
            discharge = !discharge;
            setChanged();
        }
    }

    private boolean live() {
        return !isRemoved()
                && level != null
                && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this;
    }

    boolean usable() {
        return unreadable.isEmpty() && live() && Organs.hasFoundation(level, worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) return;
        for (Direction side : Direction.values()) {
            BlockPos pos = worldPosition.relative(side);
            if (level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof PowerPartBlock)
                level.invalidateCapabilities(pos);
        }
    }

    boolean hasPart(Block part) {
        return part(part).isPresent();
    }

    private Optional<BlockPos> part(Block part) {
        if (level == null) return Optional.empty();
        for (Direction side : Direction.values()) {
            BlockPos pos = worldPosition.relative(side);
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.is(part) && state.getValue(PowerPartBlock.FACING) == side.getOpposite()) return Optional.of(pos);
        }
        return Optional.empty();
    }

    static void tick(Level level, BlockPos pos, BlockState state, ThermalHeartBlockEntity heart) {
        if (!(level instanceof ServerLevel server) || heart.unreadable.isPresent() || heart.driver()) return;
        if (heart.ticks > 0) {
            heart.ticks--;
            heart.setChanged();
            return;
        }
        if (!server.getData(PowerModule.DIMENSION_WORK).take(server.getGameTime())
                || !server.getServer()
                        .overworld()
                        .getData(PowerModule.SERVER_WORK)
                        .take(server.getServer().overworld().getGameTime())) return;
        heart.ticks = PULSE_TICKS - 1;
        heart.setChanged();
        heart.pulse(server);
    }

    private void pulse(ServerLevel server) {
        if (!usable()) {
            stop = 1;
            return;
        }
        PowerStore before = store;
        stop = 0;
        if (!receiver()) {
            Optional<BlockPos> collector = part(PowerModule.COLLECTOR.get());
            if (!server.dimension().equals(Level.NETHER)) stop = 2;
            else if (collector.isEmpty()) stop = 3;
            else if (!server.hasChunkAt(collector.get().below())
                    || !server.getFluidState(collector.get().below()).is(FluidTags.LAVA)) stop = 4;
            else if (store.heat() <= PowerStore.CAPACITY - PowerStore.HEAT_PER_PULSE)
                store = store.heat(Math.addExact(store.heat(), PowerStore.HEAT_PER_PULSE));
            else stop = 8;
        }
        capsules();
        if (hasPart(PowerModule.BOILER.get())) {
            if (store.canBoil()) store = store.boil();
            else stop = store.water() < PowerStore.BOILER_WATER ? 5 : store.heat() < PowerStore.BOILER_HEAT ? 6 : 8;
        }
        if (hasPart(PowerModule.GENERATOR.get())) {
            if (!hasPart(PowerModule.BOILER.get())) stop = 9;
            else if (store.canGenerate()) store = store.generate();
            else stop = store.steam() < PowerStore.GENERATOR_STEAM ? 7 : 8;
        }
        cells();
        if (!store.equals(before)) {
            setChanged();
            steward.worked(server, PowerModule.HEART.getId());
        }
    }

    private void capsules() {
        ItemStack input = items.getStackInSlot(0);
        if (input.isEmpty()) return;
        if (!items.getStackInSlot(2).isEmpty()) {
            stop = 11;
            return;
        }
        if (receiver()
                && input.is(PowerModule.HOT_CAPSULE.get())
                && store.heat() <= PowerStore.CAPACITY - PowerStore.CELL_CAPACITY) {
            convert(0, 2, PowerModule.HEAT_CAPSULE.get());
            store = store.heat(Math.addExact(store.heat(), PowerStore.CELL_CAPACITY));
        } else if (!receiver()
                && input.is(PowerModule.HEAT_CAPSULE.get())
                && store.heat() >= PowerStore.CELL_CAPACITY) {
            convert(0, 2, PowerModule.HOT_CAPSULE.get());
            store = store.heat(Math.subtractExact(store.heat(), PowerStore.CELL_CAPACITY));
        }
    }

    private void cells() {
        ItemStack input = items.getStackInSlot(1);
        if (input.isEmpty()) return;
        if (!items.getStackInSlot(3).isEmpty()) {
            stop = 12;
            return;
        }
        if (discharge
                && input.is(PowerModule.CHARGED_CELL.get())
                && store.charge() <= PowerStore.CAPACITY - PowerStore.CELL_CAPACITY) {
            convert(1, 3, PowerModule.CELL.get());
            store = store.charge(Math.addExact(store.charge(), PowerStore.CELL_CAPACITY));
        } else if (!discharge && input.is(PowerModule.CELL.get()) && store.charge() >= PowerStore.CELL_CAPACITY) {
            convert(1, 3, PowerModule.CHARGED_CELL.get());
            store = store.charge(Math.subtractExact(store.charge(), PowerStore.CELL_CAPACITY));
        }
    }

    private void convert(int input, int output, Item result) {
        ItemStack converted = items.getStackInSlot(input).transmuteCopy(result, 1);
        items.setStackInSlot(input, ItemStack.EMPTY);
        items.setStackInSlot(output, converted);
    }

    @Override
    public Outcome<Integer> pay(boolean simulate) {
        if (!usable()) return Outcome.refused(SteamDrive.UNROOTED);
        if (!driver() || store.steam() < CUT_STEAM) return Outcome.refused(SteamDrive.NO_STEAM);
        if (!simulate) {
            store = store.steam(Math.subtractExact(store.steam(), CUT_STEAM));
            setChanged();
        }
        return Outcome.done(CUT_STEAM);
    }

    IFluidHandler fluids(boolean waterPort) {
        return fluids(waterPort, () -> true);
    }

    IFluidHandler fluids(boolean waterPort, java.util.function.BooleanSupplier attached) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                int amount = waterPort ? store.water() : store.steam();
                return amount == 0
                        ? FluidStack.EMPTY
                        : new FluidStack(waterPort ? Fluids.WATER : PowerModule.STEAM.get(), amount);
            }

            @Override
            public int getTankCapacity(int tank) {
                return PowerStore.CAPACITY;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return waterPort ? stack.is(Fluids.WATER) : stack.is(PowerModule.STEAM.get());
            }

            private boolean open() {
                return attached.getAsBoolean()
                        && usable()
                        && (driver()
                                ? !waterPort
                                : hasPart(waterPort ? PowerModule.BOILER.get() : PowerModule.OUTLET.get()));
            }

            @Override
            public int fill(FluidStack fluid, FluidAction action) {
                if (!open() || !isFluidValid(0, fluid) || !waterPort && !driver()) return 0;
                int amount =
                        Math.min(fluid.getAmount(), PowerStore.CAPACITY - (waterPort ? store.water() : store.steam()));
                if (amount > 0 && action.execute()) {
                    store = waterPort
                            ? store.water(Math.addExact(store.water(), amount))
                            : store.steam(Math.addExact(store.steam(), amount));
                    setChanged();
                }
                return amount;
            }

            @Override
            public FluidStack drain(FluidStack fluid, FluidAction action) {
                return isFluidValid(0, fluid) ? drain(fluid.getAmount(), action) : FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                if (!open() || maxDrain <= 0) return FluidStack.EMPTY;
                int amount = Math.min(maxDrain, waterPort ? store.water() : store.steam());
                if (amount == 0) return FluidStack.EMPTY;
                if (action.execute()) {
                    store = waterPort
                            ? store.water(Math.subtractExact(store.water(), amount))
                            : store.steam(Math.subtractExact(store.steam(), amount));
                    setChanged();
                }
                return new FluidStack(waterPort ? Fluids.WATER : PowerModule.STEAM.get(), amount);
            }
        };
    }

    IEnergyStorage energy(java.util.function.BooleanSupplier attached) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return 0;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                if (!attached.getAsBoolean() || !usable() || !hasPart(PowerModule.GENERATOR.get()) || maxExtract <= 0)
                    return 0;
                int amount = Math.min(maxExtract, store.charge());
                if (!simulate && amount > 0) {
                    store = store.charge(Math.subtractExact(store.charge(), amount));
                    setChanged();
                }
                return amount;
            }

            @Override
            public int getEnergyStored() {
                return store.charge();
            }

            @Override
            public int getMaxEnergyStored() {
                return PowerStore.CAPACITY;
            }

            @Override
            public boolean canExtract() {
                return attached.getAsBoolean() && usable() && hasPart(PowerModule.GENERATOR.get());
            }

            @Override
            public boolean canReceive() {
                return false;
            }
        };
    }

    PowerContents contents() {
        return new PowerContents(
                store,
                List.of(
                        items.getStackInSlot(0),
                        items.getStackInSlot(1),
                        items.getStackInSlot(2),
                        items.getStackInSlot(3)),
                discharge,
                ticks);
    }

    private void restore(PowerContents value) {
        store = value.store();
        discharge = value.discharge();
        ticks = value.ticks();
        for (int i = 0; i < 4; i++) items.restore(i, value.items().get(i));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<PowerContents> read = Persistence.read(
                PowerContents.CODEC,
                tag,
                registries,
                PowerModule.LOGGER,
                "Power organ " + worldPosition,
                PowerContents::problem);
        unreadable = Optional.empty();
        restore(read.orElse(PowerContents.EMPTY));
        if (read.isEmpty()) unreadable = Optional.of(tag.copy());
        steward.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else {
            Persistence.write(PowerContents.CODEC, contents(), registries, tag);
            steward.save(tag);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else components.set(PowerModule.CONTENTS.get(), contents());
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag raw = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (raw != null) {
            unreadable = Optional.of(raw.copy());
            return;
        }
        PowerContents carried = input.get(PowerModule.CONTENTS.get());
        if (carried != null) {
            if (carried.problem().isPresent())
                unreadable = Optional.of(
                        level == null
                                ? new CompoundTag()
                                : Persistence.encode(PowerContents.CODEC, carried, level.registryAccess()));
            else restore(carried);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        for (String key : List.of("power", "items", "discharge", "ticks")) tag.remove(key);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PowerMenu(id, inventory, this);
    }
}
