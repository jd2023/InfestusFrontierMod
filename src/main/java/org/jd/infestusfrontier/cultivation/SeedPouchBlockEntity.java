package org.jd.infestusfrontier.cultivation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jd.infestusfrontier.cultivation.api.PlantingStock;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Four kinds of planting stock, one kind a slot, up to a stack each, and one reserve: hoppers, mouths and planters
 * take only what a slot holds beyond it. A stick raises the reserve by one, a sneaking stick lowers it; it runs 0 to
 * {@value #MAX_RESERVE} and starts at {@value #FIRST_RESERVE}. The screen takes stock out past the reserve.
 */
final class SeedPouchBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {
    static final int SLOTS = 4;
    static final int MAX_RESERVE = 64;
    static final int FIRST_RESERVE = 1;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final MapCodec<Saved> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ItemStack.OPTIONAL_CODEC.listOf().fieldOf("stock").forGetter(Saved::stock),
                    Codec.intRange(0, MAX_RESERVE).fieldOf("reserve").forGetter(Saved::reserve))
            .apply(instance, Saved::new));

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private Optional<CompoundTag> unreadable = Optional.empty();
    private int reserve = FIRST_RESERVE;
    private final ItemStackHandler stock = new ItemStackHandler(SLOTS) {
        /** One kind a slot: planting stock that no other slot holds. */
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (unreadable.isPresent() || !PlantingStock.is(stack)) return false;
            for (int other = 0; other < SLOTS; other++)
                if (other != slot && ItemStack.isSameItemSameComponents(getStackInSlot(other), stack)) return false;
            return true;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    SeedPouchBlockEntity(BlockPos pos, BlockState state) {
        super(CultivationModule.SEED_POUCH_ENTITY.get(), pos, state);
    }

    private record Saved(List<ItemStack> stock, int reserve) {}

    ItemStackHandler stock() {
        return stock;
    }

    int reserve() {
        return reserve;
    }

    /** The pouch for hoppers, mouths and planters: everything goes in, only the surplus above the reserve comes out. */
    IItemHandler surplus() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return SLOTS;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return stock.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stock.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                int spare = stock.getStackInSlot(slot).getCount() - reserve;
                if (spare <= 0) return ItemStack.EMPTY;
                return stock.extractItem(slot, Math.min(amount, spare), simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return stock.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stock.isItemValid(slot, stack);
            }
        };
    }

    /** Moves the reserve one step, within its range, and says where it is now. */
    Component adjustReserve(int by) {
        reserve = Math.clamp(reserve + by, 0, MAX_RESERVE);
        setChanged();
        return Component.translatable("infestusfrontier.status.reserve", reserve);
    }

    /** Puts one item of {@code stack} into its slot; false when no slot takes it. */
    boolean insert(ItemStack stack) {
        if (unreadable.isPresent()) return false;
        ItemStack one = stack.copyWithCount(1);
        for (int pass = 0; pass < 2; pass++)
            for (int slot = 0; slot < SLOTS; slot++) {
                if ((pass == 0) == stock.getStackInSlot(slot).isEmpty()) continue;
                if (stock.insertItem(slot, one, false).isEmpty()) {
                    stack.shrink(1);
                    return true;
                }
            }
        return false;
    }

    boolean isEmpty() {
        if (unreadable.isPresent()) return false;
        for (int slot = 0; slot < SLOTS; slot++) if (!stock.getStackInSlot(slot).isEmpty()) return false;
        return true;
    }

    /** Spills the stock when the pouch is broken. */
    void dropStock(Level level, BlockPos pos) {
        NonNullList<ItemStack> held = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int slot = 0; slot < SLOTS; slot++) held.set(slot, stock.getStackInSlot(slot));
        Containers.dropContents(level, pos, new SimpleContainer(held.toArray(new ItemStack[0])));
        for (int slot = 0; slot < SLOTS; slot++) stock.setStackInSlot(slot, ItemStack.EMPTY);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "pouch", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    private static Optional<String> check(Saved saved) {
        if (saved.stock().size() > SLOTS) return Optional.of(saved.stock().size() + " slots");
        for (ItemStack stack : saved.stock())
            if (!stack.isEmpty() && !PlantingStock.is(stack)) return Optional.of("an item that is not planting stock");
        return Optional.empty();
    }

    private void restore(Saved saved) {
        for (int slot = 0; slot < SLOTS; slot++)
            stock.setStackInSlot(
                    slot, slot < saved.stock().size() ? saved.stock().get(slot).copy() : ItemStack.EMPTY);
        reserve = saved.reserve();
    }

    private Saved saved() {
        List<ItemStack> held = new java.util.ArrayList<>(SLOTS);
        for (int slot = 0; slot < SLOTS; slot++)
            held.add(stock.getStackInSlot(slot).copy());
        return new Saved(held, reserve);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<Saved> read = Persistence.read(
                SAVED, tag, registries, CultivationModule.LOGGER, "Seed Pouch", SeedPouchBlockEntity::check);
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
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) unreadable = Optional.of(kept.copy());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("stock");
        tag.remove("reserve");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infestusfrontier.seed_pouch");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SeedPouchMenu(id, inventory, this);
    }
}
