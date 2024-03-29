package org.jd.infestusfrontier.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
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
import org.jd.infestusfrontier.recipe.CorruptionCoreBiomassConversionRecipe;
import org.jd.infestusfrontier.utils.Infester;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class CorruptionCoreBlockEntity extends BlockEntity {
    public int progress = 0;
    public int maxProgress = 26;
    public int maxBiomass = 70;
    public int biomass=0;
    private String networkId;
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private static final Logger LOGGER = LogUtils.getLogger();
    private Infester infester;
    public final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {

            if (level != null) {
                return switch (slot) {
                    case 0 -> true;
                    default -> super.isItemValid(slot, stack);
                };

            } else {
                System.out.println("Level Is Null");
                return false;
            }
        }
    };



    public CorruptionCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ZgBlockEntities.CORRUPTION_CORE_ENTITY_TYPE.get(), pos, state);
        this.infester = new Infester(4, 4);
        LOGGER.warn("Creating CorruptionCoreBlockEntity at {}", pos);


    }
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        if (level.isClientSide()) return;
        if (level instanceof ServerLevel serverLevel) {
            if (be instanceof CorruptionCoreBlockEntity entity) {
                entity.infester.infestNext(serverLevel, pos);
                if (hasRecipe(entity)) {
                    entity.progress++;
                    setChanged(level, pos, state);
                    if (entity.progress >= entity.maxProgress) {
                        if (entity.biomass < entity.maxBiomass) {
                        SimpleContainer inventory = new SimpleContainer(entity.itemHandler.getSlots());
                        for (int i = 0; i<entity.itemHandler.getSlots(); i++) {
                            inventory.setItem(i, entity.itemHandler.getStackInSlot(i));
                        }
                        Optional<CorruptionCoreBiomassConversionRecipe> recipe = level.getRecipeManager()
                                .getRecipeFor(CorruptionCoreBiomassConversionRecipe.Type.INSTANCE, inventory, level);
                        entity.itemHandler.extractItem(0, 1, false);

                            entity.biomass+=recipe.get().getBiomassValue();
                            entity.resetProgress();
                        }else{
                            entity.biomass=entity.maxBiomass;

                        }

                        entity.resetProgress();
                    }
                }else{
                    entity.resetProgress();
                    setChanged(level, pos, state);
                }
            }
        }

    }
    private static boolean hasRecipe(CorruptionCoreBlockEntity entity) {
        Level level = entity.level;
        SimpleContainer inventory = new SimpleContainer(entity.itemHandler.getSlots());
        for (int i = 0; i<entity.itemHandler.getSlots(); i++) {
            inventory.setItem(i, entity.itemHandler.getStackInSlot(i));
        }
        Optional<CorruptionCoreBiomassConversionRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(CorruptionCoreBiomassConversionRecipe.Type.INSTANCE, inventory, level);
        return recipe.isPresent();
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        nbt.put(Infester.TAG, infester.serializeNBT());
        super.saveAdditional(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        infester.deserializeNBT(nbt.getCompound(Infester.TAG));
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        biomass = nbt.getInt("corruption_core.biomass");
        progress = nbt.getInt("corruption_core.progress");
    }
    @Override
    public void onLoad() {
        lazyItemHandler = LazyOptional.of(()->itemHandler);
    }
    @Override
    public void invalidateCaps() {
        lazyItemHandler.invalidate();
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
    public void remove(BlockState state, ServerLevel level, BlockPos pos) {
        infester.downgradeNetwork(state, level, pos);
    }
}
