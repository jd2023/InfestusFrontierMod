package org.jd.infestusfrontier.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jd.infestusfrontier.block.InfestusBlockEntities;
import org.jd.infestusfrontier.block.custom.MutationPoolBlock;
import org.jd.infestusfrontier.screen.MutationPoolMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

public class MutationPoolBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler=new ItemStackHandler(8);
    //Input slots are 0-6
    //Output slot is 7
    private LazyOptional<IItemHandler> lazyItemHandler=LazyOptional.empty();
    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress=78;
    private int biomass=0;
    private int maxBiomass=500;
    public MutationPoolBlockEntity(BlockPos pos, BlockState state) {
        super(InfestusBlockEntities.MUTATION_POOL_ENTITY.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int PIndex) {
                return switch (PIndex) {
                    case 0->MutationPoolBlockEntity.this.progress;
                    case 1->MutationPoolBlockEntity.this.maxProgress;
                    case 2->MutationPoolBlockEntity.this.biomass;
                    case 3->MutationPoolBlockEntity.this.maxBiomass;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index){
                    case 0->MutationPoolBlockEntity.this.progress=value;
                    case 1->MutationPoolBlockEntity.this.maxProgress=value;
                    case 2->MutationPoolBlockEntity.this.biomass=value;
                    case 3->MutationPoolBlockEntity.this.maxBiomass=value;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i=0; i>itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
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
        super.onLoad();
        lazyItemHandler = LazyOptional.of(()->itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infestusfrontier.mutation_pool");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MutationPoolMenu(id, inv, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("mutation_pool.inventory", itemHandler.serializeNBT());
        tag.putInt("mutation_pool.progress", progress);
        tag.putInt("mutation_pool.biomass", biomass);

        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("mutation_pool.inventory"));
        progress=tag.getInt("mutation_pool.progress");
        biomass=tag.getInt("mutation_pool.biomass");
    }

    public void tick(Level pLevel1, BlockPos pPos, BlockState pState1) {

    }
}
