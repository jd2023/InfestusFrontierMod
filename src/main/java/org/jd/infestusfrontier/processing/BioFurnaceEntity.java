package org.jd.infestusfrontier.processing;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.processing.api.BioFurnaceRecipes;
import org.jd.infestusfrontier.processing.digestion.BiomassBucketTransfer;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;
import org.jd.infestusfrontier.storage.api.BiomassTransfer;
import org.jd.infestusfrontier.storage.api.QuantityStore;

final class BioFurnaceEntity extends BlockEntity {
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int ITEM_CAPACITY = 64;
    private static final int BIOMASS_CAPACITY = 2_000;
    private BatchWork work;
    private final DiscoveryObserver discovery;
    private Tag rejected;
    private CompoundTag pending;

    BioFurnaceEntity(BlockPos pos, BlockState state, Supplier<BlockEntityType<BioFurnaceEntity>> type,
            DiscoveryObserver discovery) {
        super(type.get(), pos, state);
        this.discovery = discovery;
        work = BatchWork.create(new QuantityStore(List.of(
                QuantityStore.ItemSlot.empty(ITEM_CAPACITY),
                QuantityStore.ItemSlot.empty(ITEM_CAPACITY)),
                List.of(QuantityStore.Tank.empty(BIOMASS_CAPACITY))), BioFurnaceRecipes.catalog(this::resultOf), this::admit);
    }

    boolean admit() { return level != null && !level.isClientSide && RecipeAdmission.take(level.getServer()); }

    void tick() {
        if (level == null || level.isClientSide || rejected != null || !work.isActive()) return;
        work.advance(1);
        setChanged();
    }

    @Override public void setChanged() {
        if (level != null && !level.isClientSide) level.blockEntityChanged(worldPosition);
    }

    @Override public void setLevel(Level level) {
        super.setLevel(level);
        if (pending != null) {
            var saved = pending;
            pending = null;
            restore(saved);
        }
    }

    boolean interact(Player player, InteractionHand hand) {
        if (rejected != null) {
            message(player, "refused.rejected_save");
            return true;
        }
        var stack = player.getItemInHand(hand);
        long before = work.revision();
        if (stack.isEmpty()) {
            if (player.isShiftKeyDown()) collect(player, hand);
            else start(player);
        } else if (stack.is(BioFurnaceResources.item("infestusfrontier:storage/biomass_bucket"))) {
            BiomassBucketTransfer.empty(player, hand, BioFurnaceResources.item("infestusfrontier:storage/biomass_bucket"),
                    () -> work.insertFluid(BioFurnaceRecipes.BIOMASS, BiomassTransfer.BUCKET_AMOUNT, work.revision()),
                    discovery);
        } else {
            var input = BioFurnaceResources.key(stack.getItem());
            if (!stack.getComponentsPatch().isEmpty() || input.isEmpty() || !canSmelt(input)
                    || !inputSlotAccepts(input, stack.getMaxStackSize())
                    || !work.insertItem(input, 1, stack.getMaxStackSize(), work.revision())) return false;
            stack.shrink(1);
        }
        if (work.revision() != before) setChanged();
        return true;
    }

    private boolean inputSlotAccepts(String resource, int capacity) {
        var slots = work.state().quantities().itemSlots();
        var input = slots.get(INPUT_SLOT);
        if ((!input.isEmpty() && !input.resource().equals(resource))
                || input.count() >= Math.min(input.capacity(), capacity)) return false;
        // Generic insertion prefers matching stacks. Refuse until output is collected
        // if it would receive this item instead of the designated input slot.
        var output = slots.get(OUTPUT_SLOT);
        return !input.isEmpty() || !output.resource().equals(resource)
                || output.count() >= Math.min(output.capacity(), capacity);
    }

    private void start(Player player) {
        var input = work.state().quantities().itemSlots().get(INPUT_SLOT);
        if (input.isEmpty() || !canSmelt(input.resource())) {
            message(player, "refused.unknown_recipe");
            return;
        }
        var result = work.start(new BatchWork.StartRequest(input.resource()), work.revision());
        if (result instanceof BatchWork.Refused refused) message(player,
                "refused." + refused.reason().name().toLowerCase(java.util.Locale.ROOT));
        else message(player, "started");
    }

    private void collect(Player player, InteractionHand hand) {
        for (int slot : new int[] {OUTPUT_SLOT, INPUT_SLOT}) {
            var held = work.state().quantities().itemSlots().get(slot);
            if (held.isEmpty()) continue;
            if (work.extractItemSlot(slot, work.revision())) {
                player.setItemInHand(hand, new ItemStack(BioFurnaceResources.item(held.resource()), held.count()));
                return;
            }
        }
        message(player, "empty");
    }

    private boolean canSmelt(String inputItemId) { return resultOf(inputItemId).isPresent(); }

    Optional<String> resultOf(String inputItemId) {
        if (level == null || level.isClientSide) return Optional.empty();
        var inputId = ResourceLocation.tryParse(inputItemId);
        if (inputId == null) return Optional.empty();
        var inputItem = BuiltInRegistries.ITEM.getOptional(inputId);
        if (inputItem.isEmpty()) return Optional.empty();
        var single = new SingleRecipeInput(new ItemStack(inputItem.get()));
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, single, level)
                .map(holder -> holder.value().assemble(single, level.registryAccess()))
                .filter(result -> !result.isEmpty())
                .map(result -> BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
    }

    private void message(Player player, String key) {
        player.displayClientMessage(Component.translatable("message.infestusfrontier.processing.bio_furnace." + key), false);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (rejected != null) tag.put("furnace", rejected.copy());
        else if (pending != null) tag.put("furnace", pending.copy());
        else tag.put("furnace", BioFurnaceSave.write(work.state(), this));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        rejected = null;
        pending = null;
        if (!tag.contains("furnace")) return;
        Tag saved = tag.get("furnace");
        if (!(saved instanceof CompoundTag furnace)) {
            rejected = saved.copy();
        } else if (level == null) {
            pending = furnace.copy();
        } else {
            restore(furnace);
        }
    }

    private void restore(CompoundTag saved) {
        try {
            var restored = BioFurnaceSave.read(saved, this);
            work = BatchWork.restore(restored, BioFurnaceRecipes.catalog(this::resultOf), this::admit);
        } catch (RuntimeException exception) {
            rejected = saved.copy();
        }
    }
}
