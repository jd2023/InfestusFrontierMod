package org.jd.infestusfrontier.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.checkerframework.checker.units.qual.C;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.ZgItems;
import org.jd.infestusfrontier.screen.BioReservoirMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BioReservoirBlockEntity extends BlockEntity implements MenuProvider {
    private int progress = 0;
    private int maxProgress = 26;
    protected final ContainerData data;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (level != null) {
                return switch (slot) {
                    case 0 -> stack.getItem() == ZgItems.CORRUPT_CHUNK.get() || stack.getItem() == Items.ROTTEN_FLESH;
                    default -> super.isItemValid(slot, stack);
                };

            } else {
                System.out.println("Level Is Null");
                return false;
            }
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public BioReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(ZgBlockEntities.BIOMASS_RESERVOIR.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0-> BioReservoirBlockEntity.this.progress;
                    case 1-> BioReservoirBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0-> BioReservoirBlockEntity.this.progress = value;
                    case 1-> BioReservoirBlockEntity.this.maxProgress = value;
                };
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }
    public int getProgress() {
        return this.progress;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Bio Reservoir");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ID, Inventory inventory, Player player) {
        return new BioReservoirMenu(ID, inventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        lazyItemHandler = LazyOptional.of(()->itemHandler);
    }

    @Override
    public void invalidateCaps() {
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
    }
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
    private void resetProgress() {
        this.progress = 0;
    }

    public static  void tick(Level level, BlockPos pos, BlockState state, BioReservoirBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }
        if (entity.itemHandler.getStackInSlot(0).getItem() == Items.ROTTEN_FLESH ||entity.itemHandler.getStackInSlot(0).getItem() == ZgItems.CORRUPT_CHUNK.get()) {
            entity.progress++;
            System.out.println(entity.getProgress());
            setChanged(level, pos, state);

            if (entity.progress >= entity.maxProgress) {
                entity.itemHandler.extractItem(0, 1, false);
                entity.resetProgress();
            }
        }else{
            entity.resetProgress();
            setChanged(level, pos, state);
        }


    }
}
