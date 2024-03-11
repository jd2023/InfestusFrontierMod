package org.jd.infestusfrontier.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jd.infestusfrontier.ZgBlockEntities;
import org.jd.infestusfrontier.ZgItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BioReservoirBlockEntity extends BlockEntity {
    public int progress = 0;
    public int maxProgress = 26;

    public int biomass=0;


    public final ItemStackHandler itemHandler = new ItemStackHandler(1) {
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

    }



    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
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
        nbt.putInt("bioreservoir.biomass", biomass);
        nbt.putInt("bioreservoir.progress", progress);
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        biomass = nbt.getInt("bioreservoir.biomass");
        progress = nbt.getInt("bioreservoir.progress");
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
            setChanged(level, pos, state);
            if (entity.progress >= entity.maxProgress) {
                entity.itemHandler.extractItem(0, 1, false);
                if (entity.biomass < 70) {
                    entity.biomass++;
                }

                entity.resetProgress();
            }
        }else{
            entity.resetProgress();
            setChanged(level, pos, state);
        }


    }
}
