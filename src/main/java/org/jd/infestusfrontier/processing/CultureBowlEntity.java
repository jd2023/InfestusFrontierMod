package org.jd.infestusfrontier.processing;

import java.util.Collections;
import java.util.List;
import com.mojang.logging.LogUtils;
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
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.organ.api.OrganHistory;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.processing.api.CultureBowlRecipes;
import org.jd.infestusfrontier.storage.api.QuantityStore;

final class CultureBowlEntity extends BlockEntity {
    private BatchWork work;
    private String selected = "I000";
    private CompoundTag rejected;
    CultureBowlEntity(BlockPos pos, BlockState state) {
        super(ProcessingModule.BOWL_ENTITY.get(), pos, state);
        work = BatchWork.create(new QuantityStore(Collections.nCopies(9, QuantityStore.ItemSlot.empty(64)),
                List.of(QuantityStore.Tank.empty(BowlSave.WATER_CAPACITY))), this::admit);
    }
    private boolean admit() { return level != null && !level.isClientSide && RecipeAdmission.take(level.getServer()); }
    void tick() {
        if (rejected != null || !work.isActive() || level == null || level.isClientSide) return;
        if (!getBlockState().canSurvive(level, worldPosition)) return;
        work.advance(1);
        setChanged();
        updateAppearance();
    }
    @Override public void setChanged() {
        // Only the owning loaded chunk is dirtied; no comparator/neighbor cascade is needed.
        if (level != null && !level.isClientSide) level.blockEntityChanged(worldPosition);
    }
    void interact(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide) return;
        if (rejected != null) { message(player, "invalid_save"); return; }
        var stack = player.getItemInHand(hand);
        if (stack.is(Items.STICK)) {
            if (player.isShiftKeyDown()) {
                message(player, "cancel." + work.cancel(work.revision()).name().toLowerCase(java.util.Locale.ROOT));
            } else if (!work.isActive()) {
                var ids = List.copyOf(CultureBowlRecipes.all().keySet());
                selected = ids.get((ids.indexOf(selected) + 1) % ids.size());
                showRecipe(player);
            }
        } else if (player.isShiftKeyDown() && (stack.is(Items.CLOCK) || stack.is(Items.GLASS_BOTTLE))) {
            var choice = stack.is(Items.CLOCK) ? OrganHistory.GrowthChoice.INCUBATION : OrganHistory.GrowthChoice.WATER_ECONOMY;
            message(player, "growth." + work.choose(choice, work.revision()).name().toLowerCase(java.util.Locale.ROOT));
        } else if (stack.isEmpty()) {
            if (player.isShiftKeyDown()) collect(player, hand);
            else {
                var result = work.start(new BatchWork.StartRequest(selected), work.revision());
                if (result instanceof BatchWork.Refused refused) message(player, "start." + refused.reason().name().toLowerCase(java.util.Locale.ROOT));
                else message(player, "started");
            }
        } else if (stack.is(Items.WATER_BUCKET) && stack.getComponentsPatch().isEmpty()) {
            if (work.insertWater(1000, work.revision())) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            else message(player, "transfer_refused");
        } else {
            String resource = BowlResources.key(stack.getItem());
            if (!resource.isEmpty() && stack.getComponentsPatch().isEmpty()
                    && work.insertItem(resource, 1, stack.getMaxStackSize(), work.revision())) stack.shrink(1);
            else message(player, "transfer_refused");
        }
        setChanged();
        updateAppearance();
        var state = work.state();
        player.displayClientMessage(Component.translatable("message.infestusfrontier.bowl.status",
                BowlResources.item(CultureBowlRecipes.recipe(selected).outputs().keySet().iterator().next()).getDescription(),
                state.quantities().fluidAmount("water"), state.quantities().tanks().getFirst().capacity(), state.history().completedBatches()), true);
    }
    private void showRecipe(Player player) {
        var recipe = CultureBowlRecipes.recipe(selected);
        var description = Component.empty();
        for (var alternative : recipe.itemInputAlternatives()) {
            if (!description.getString().isEmpty()) description.append(Component.translatable("message.infestusfrontier.bowl.or"));
            for (var entry : alternative.entrySet()) {
                description.append(entry.getValue() + " × ").append(BowlResources.item(entry.getKey()).getDescription()).append("; ");
            }
        }
        if (recipe.fluidInputs().containsKey("water")) description.append(Component.translatable(
                "message.infestusfrontier.bowl.base_water", recipe.fluidInputs().get("water")));
        player.displayClientMessage(description, false);
    }
    private void collect(Player player, InteractionHand hand) {
        var slots = work.state().quantities().itemSlots();
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            if (slot.isEmpty()) continue;
            var stack = new ItemStack(BowlResources.item(slot.resource()), slot.count());
            if (work.extractItemSlot(i, work.revision())) player.setItemInHand(hand, stack);
            else message(player, "transfer_refused");
            return;
        }
        message(player, "empty");
    }
    private void updateAppearance() {
        boolean active = work.isActive();
        if (getBlockState().getValue(CultureBowlBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, getBlockState().setValue(CultureBowlBlock.ACTIVE, active),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE, 0);
        }
    }
    private void message(Player player, String key) {
        player.displayClientMessage(Component.translatable("message.infestusfrontier.bowl." + key), false);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (rejected != null) tag.merge(rejected);
        else tag.put("bowl", BowlSave.write(work.state(), selected));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            if (!tag.contains("bowl", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Missing Bowl state");
            var loaded = BowlSave.read(tag.getCompound("bowl"));
            work = BatchWork.restore(loaded.state(), this::admit);
            selected = loaded.selected();
            rejected = null;
        } catch (RuntimeException exception) {
            rejected = tag;
            LogUtils.getLogger().error("Rejected Culture Bowl at {}: {}; interactions disabled", worldPosition, exception.getMessage());
        }
    }
}
