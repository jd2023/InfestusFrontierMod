package org.jd.infestusfrontier.processing;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.processing.api.PreparationRecipes;
import org.jd.infestusfrontier.processing.digestion.BiomassBucketTransfer;
import org.jd.infestusfrontier.storage.api.BiomassTransfer;
import org.jd.infestusfrontier.storage.api.QuantityStore;

final class PreparationBlockEntity extends BlockEntity {
    private final PreparationOrgan organ;
    private final DiscoveryObserver discovery;
    private BatchWork work;
    private CompoundTag rejected;

    PreparationBlockEntity(BlockPos pos, BlockState state, Supplier<BlockEntityType<PreparationBlockEntity>> type,
            PreparationOrgan organ, DiscoveryObserver discovery) {
        super(type.get(), pos, state);
        this.organ = organ;
        this.discovery = discovery;
        work = BatchWork.create(new QuantityStore(
                Collections.nCopies(PreparationSave.ITEM_SLOTS, QuantityStore.ItemSlot.empty(64)),
                List.of(QuantityStore.Tank.empty(PreparationSave.TANK_CAPACITY))),
                PreparationRecipes.catalog(), this::admit);
    }

    private boolean admit() { return level != null && !level.isClientSide && RecipeAdmission.take(level.getServer()); }

    void tick() {
        if (rejected != null || level == null || level.isClientSide || !work.isActive()) return;
        if (!getBlockState().canSurvive(level, worldPosition)) return;
        work.advance(1);
        setChanged();
        updateAppearance();
    }

    boolean interact(Player player, InteractionHand hand) {
        if (rejected != null) { message(player, "invalid_save"); return true; }
        var stack = player.getItemInHand(hand);
        long before = work.revision();
        if (stack.isEmpty()) {
            if (player.isShiftKeyDown()) collect(player, hand);
            else {
                var result = work.start(new BatchWork.StartRequest(organ.recipeId), work.revision());
                if (result instanceof BatchWork.Refused refused) message(player,
                        "refused." + refused.reason().name().toLowerCase(java.util.Locale.ROOT));
                else message(player, "started");
            }
        } else if (acceptFluid(stack)) {
            if (organ == PreparationOrgan.BONE_LOOM) {
                BiomassBucketTransfer.empty(player, hand,
                        PreparationResources.item("biomass_bucket"),
                        () -> work.insertFluid(organ.fluid,
                                BiomassTransfer.BUCKET_AMOUNT, work.revision()), discovery);
            } else if (work.insertFluid(organ.fluid, 1000, work.revision())) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            } else message(player, "transfer_refused");
        } else {
            String resource = PreparationResources.key(stack.getItem());
            if (!resource.isEmpty() && allowedInput(resource) && stack.getComponentsPatch().isEmpty()
                    && work.insertItem(resource, 1, stack.getMaxStackSize(), work.revision())) stack.shrink(1);
            else return false;
        }
        if (work.revision() != before) setChanged();
        updateAppearance();
        return true;
    }

    private boolean acceptFluid(ItemStack stack) {
        return organ == PreparationOrgan.MEMBRANE_RACK
                ? stack.is(Items.WATER_BUCKET) && stack.getComponentsPatch().isEmpty()
                : stack.is(PreparationResources.item("biomass_bucket")) && stack.getComponentsPatch().isEmpty();
    }

    private boolean allowedInput(String resource) {
        return PreparationRecipes.recipe(organ.recipeId).routes().stream()
                .anyMatch(route -> route.itemInputs().containsKey(resource));
    }

    private void collect(Player player, InteractionHand hand) {
        var slots = work.state().quantities().itemSlots();
        int selected = -1;
        for (int index = 0; index < slots.size(); index++) {
            var slot = slots.get(index);
            if (slot.isEmpty()) continue;
            if (selected < 0) selected = index;
            if (slot.resource().equals(organ.output)) { selected = index; break; }
        }
        if (selected >= 0) {
            var slot = slots.get(selected);
            var stack = new ItemStack(PreparationResources.item(slot.resource()), slot.count());
            if (work.extractItemSlot(selected, work.revision())) player.setItemInHand(hand, stack);
            else message(player, "transfer_refused");
            return;
        }
        message(player, "empty");
    }

    private void updateAppearance() {
        boolean active = work.isActive();
        if (getBlockState().getValue(PreparationBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, getBlockState().setValue(PreparationBlock.ACTIVE, active),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE, 0);
        }
    }

    private void message(Player player, String key) {
        player.displayClientMessage(Component.translatable("message.infestusfrontier.preparation." + key), false);
    }

    BatchWork.State snapshot() { return work.snapshot(); }

    @Override public void setChanged() {
        if (level != null && !level.isClientSide) level.blockEntityChanged(worldPosition);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (rejected != null) tag.merge(rejected);
        else tag.put("preparation", PreparationSave.write(work.state()));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            if (!tag.contains("preparation", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Missing preparation state");
            work = BatchWork.restore(PreparationSave.read(tag.getCompound("preparation"), organ),
                    PreparationRecipes.catalog(), this::admit);
            rejected = null;
        } catch (RuntimeException exception) {
            rejected = tag.copy();
            LogUtils.getLogger().error("Rejected {} at {}: {}; interactions disabled", organ, worldPosition, exception.getMessage());
        }
    }
}
