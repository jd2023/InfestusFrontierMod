package org.jd.infestusfrontier.processing;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import org.jd.infestusfrontier.organ.api.OrganHistory;
import org.jd.infestusfrontier.interaction.api.ProbeTarget;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.processing.api.CultureBowlRecipes;
import org.jd.infestusfrontier.processing.menu.BowlIntentPayload;
import org.jd.infestusfrontier.processing.menu.BowlMenuTarget;
import org.jd.infestusfrontier.processing.menu.BowlRefusal;
import org.jd.infestusfrontier.processing.menu.CultureBowlMenu;
import org.jd.infestusfrontier.processing.menu.CultureBowlMenuSnapshot;
import org.jd.infestusfrontier.storage.api.QuantityStore;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.function.Supplier;

final class CultureBowlEntity extends BlockEntity implements ProbeTarget, BowlMenuTarget {
    private BatchWork work;
    // Conflict identity advances on commands/completion, never on incubation progress.
    private long commandRevision;
    private String selected = "I000";
    private CompoundTag rejected;
    private UUID owner;
    private final DiscoveryObserver discovery;
    private boolean pendingBatchCredit;
    private boolean pendingBudCredit;
    CultureBowlEntity(BlockPos pos, BlockState state, Supplier<BlockEntityType<CultureBowlEntity>> entityType,
            DiscoveryObserver discovery) {
        super(entityType.get(), pos, state);
        this.discovery = discovery;
        work = BatchWork.create(new QuantityStore(Collections.nCopies(9, QuantityStore.ItemSlot.empty(64)),
                List.of(QuantityStore.Tank.empty(BowlSave.WATER_CAPACITY))), this::admit);
    }
    private boolean admit() { return level != null && !level.isClientSide && RecipeAdmission.take(level.getServer()); }
    void tick() {
        if (rejected != null || level == null || level.isClientSide) return;
        if (pendingBatchCredit && owner != null && level.getGameTime() % 20 == 0) {
            reconcileDiscovery(level.getServer().getPlayerList().getPlayer(owner));
        }
        if (!work.isActive()) return;
        if (!getBlockState().canSurvive(level, worldPosition)) return;
        work.advance(1);
        if (!work.isActive()) {
            commandRevision = work.revision();
            completeDiscovery();
        }
        setChanged();
        updateAppearance();
    }
    private void completeDiscovery() {
        pendingBatchCredit = owner != null;
        pendingBudCredit |= owner != null && selected.equals("I001");
        if (owner != null) reconcileDiscovery(level.getServer().getPlayerList().getPlayer(owner));
    }
    private void reconcileDiscovery(ServerPlayer player) {
        if (player == null || !player.getUUID().equals(owner) || !pendingBatchCredit) return;
        discovery.complete(player, DiscoveryObserver.Milestone.CULTURE_BOWL_BATCH);
        if (pendingBudCredit) discovery.complete(player, DiscoveryObserver.Milestone.ORGAN_BUD);
        pendingBatchCredit = false;
        pendingBudCredit = false;
        setChanged();
    }
    @Override public void setChanged() {
        // Only the owning loaded chunk is dirtied; no comparator/neighbor cascade is needed.
        if (level != null && !level.isClientSide) level.blockEntityChanged(worldPosition);
    }
    void interact(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide) return;
        if (rejected != null) { message(player, "invalid_save"); return; }
        if (owner != null && !owner.equals(player.getUUID())) { message(player, "wrong_owner"); return; }
        if (player instanceof ServerPlayer serverPlayer) reconcileDiscovery(serverPlayer);
        long before = work.revision();
        var stack = player.getItemInHand(hand);
        if (stack.is(Items.STICK)) {
            if (player.isShiftKeyDown()) {
                var cancelled = work.cancel(work.revision());
                message(player, "cancel." + cancelled.name().toLowerCase(java.util.Locale.ROOT));
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
                else {
                    message(player, "started");
                }
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
        if (work.revision() != before) commandRevision = work.revision();
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
        if (owner != null) tag.putUUID("owner", owner);
        if (pendingBatchCredit) tag.putBoolean("pendingBatchCredit", true);
        if (pendingBudCredit) tag.putBoolean("pendingBudCredit", true);
        if (rejected != null) tag.merge(rejected);
        else tag.put("bowl", BowlSave.write(work.state(), selected));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        pendingBatchCredit = tag.getBoolean("pendingBatchCredit");
        pendingBudCredit = tag.getBoolean("pendingBudCredit");
        try {
            if (!tag.contains("bowl", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Missing Bowl state");
            var loaded = BowlSave.read(tag.getCompound("bowl"));
            work = BatchWork.restore(loaded.state(), this::admit);
            commandRevision = work.revision();
            selected = loaded.selected();
            rejected = null;
        } catch (RuntimeException exception) {
            rejected = tag;
            LogUtils.getLogger().error("Rejected Culture Bowl at {}: {}; interactions disabled", worldPosition, exception.getMessage());
        }
    }

    @Override public UUID probeOwner() { return owner; }
    @Override public UUID menuOwner() { return owner; }
    @Override public BlockPos menuPosition() { return worldPosition; }

    @Override public boolean claimProbeOwner(UUID player) {
        if (owner != null && !owner.equals(player)) return false;
        if (owner == null) {
            owner = player;
            setChanged();
        }
        return true;
    }

    @Override public void openProbeMenu(ServerPlayer player) {
        if (level == null || level.isClientSide) return;
        reconcileDiscovery(player);
        var initial = menuSnapshot(BowlRefusal.NONE);
        player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new CultureBowlMenu(id, inventory, this),
                Component.translatable("screen.infestusfrontier.culture_bowl")), buffer -> {
            buffer.writeBlockPos(worldPosition);
            CultureBowlMenuSnapshot.CODEC.encode(buffer, initial);
        });
    }

    @Override public boolean isPresentIn(ServerLevel candidate) {
        return level == candidate && candidate.hasChunkAt(worldPosition) && candidate.getBlockEntity(worldPosition) == this;
    }

    @Override public CultureBowlMenuSnapshot menuSnapshot(BowlRefusal refusal) {
        if (rejected != null) {
            return new CultureBowlMenuSnapshot(0, Collections.nCopies(9, new CultureBowlMenuSnapshot.Slot("", 0)),
                    0, BowlSave.WATER_CAPACITY, CultureBowlMenuSnapshot.State.INVALID_SAVE, 0, 0, 0, selected, refusal);
        }
        var state = work.state();
        var slots = state.quantities().itemSlots().stream().map(slot -> slot.isEmpty()
                ? new CultureBowlMenuSnapshot.Slot("", 0)
                : new CultureBowlMenuSnapshot.Slot(net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(BowlResources.item(slot.resource())).toString(), slot.count())).toList();
        var active = state.activeBatch();
        var status = switch (state.status()) {
            case IDLE -> CultureBowlMenuSnapshot.State.IDLE;
            case WORKING -> CultureBowlMenuSnapshot.State.WORKING;
            case COMPLETION_BLOCKED -> CultureBowlMenuSnapshot.State.COMPLETION_BLOCKED;
        };
        return new CultureBowlMenuSnapshot(commandRevision, slots, state.quantities().fluidAmount("water"),
                state.quantities().tanks().getFirst().capacity(), status,
                active == null ? 0 : active.completedWorkUnits(), active == null ? 0 : active.requiredWorkUnits(),
                state.history().completedBatches(), selected, refusal);
    }

    @Override public BowlMenuTarget.ApplyResult applyMenuIntent(ServerPlayer player, BowlIntentPayload payload) {
        if (rejected != null) return BowlMenuTarget.ApplyResult.refused(BowlRefusal.REVISION_EXHAUSTED);
        if (payload.revision() != commandRevision) return BowlMenuTarget.ApplyResult.refused(BowlRefusal.STALE_REVISION);
        return switch (payload.intent()) {
            case START -> startFromMenu(player, payload.recipe(), work.revision());
            case CANCEL -> cancelFromMenu(work.revision());
            case EXTRACT_SLOT -> extractFromMenu(payload.slot(), work.revision());
        };
    }

    private BowlMenuTarget.ApplyResult startFromMenu(ServerPlayer player, String recipe, long revision) {
        if (recipe.isBlank()) return BowlMenuTarget.ApplyResult.refused(BowlRefusal.UNKNOWN_RECIPE);
        var result = work.start(new BatchWork.StartRequest(recipe), revision);
        if (result instanceof BatchWork.Started) {
            commandRevision = work.revision();
            selected = recipe;
            setChanged();
            updateAppearance();
            return BowlMenuTarget.ApplyResult.accepted();
        }
        var refusal = ((BatchWork.Refused) result).reason();
        return BowlMenuTarget.ApplyResult.refused(switch (refusal) {
            case STALE_REVISION -> BowlRefusal.STALE_REVISION;
            case ACTIVE_BATCH -> BowlRefusal.ACTIVE_BATCH;
            case UNKNOWN_RECIPE -> BowlRefusal.UNKNOWN_RECIPE;
            case INSUFFICIENT_ITEM -> BowlRefusal.INSUFFICIENT_ITEM;
            case INSUFFICIENT_FLUID -> BowlRefusal.INSUFFICIENT_FLUID;
            case OUTPUT_FULL -> BowlRefusal.OUTPUT_FULL;
            case RETURNED_CONTAINER_FULL -> BowlRefusal.RETURNED_CONTAINER_FULL;
            case IDENTIFIER_EXHAUSTED -> BowlRefusal.IDENTIFIER_EXHAUSTED;
            case REVISION_EXHAUSTED -> BowlRefusal.REVISION_EXHAUSTED;
        });
    }

    private BowlMenuTarget.ApplyResult cancelFromMenu(long revision) {
        var result = work.cancel(revision);
        if (result == BatchWork.CancelResult.CANCELLED) {
            commandRevision = work.revision();
            setChanged();
            updateAppearance();
            return BowlMenuTarget.ApplyResult.accepted();
        }
        return BowlMenuTarget.ApplyResult.refused(result == BatchWork.CancelResult.STALE_REVISION
                ? BowlRefusal.STALE_REVISION : BowlRefusal.IDLE);
    }

    private BowlMenuTarget.ApplyResult extractFromMenu(int slot, long revision) {
        if (slot < 0 || slot >= 9) return BowlMenuTarget.ApplyResult.refused(BowlRefusal.MALFORMED_SLOT);
        if (revision != work.revision()) return BowlMenuTarget.ApplyResult.refused(BowlRefusal.STALE_REVISION);
        if (work.isActive()) return BowlMenuTarget.ApplyResult.refused(BowlRefusal.ACTIVE_BATCH);
        var stored = work.state().quantities().itemSlots().get(slot);
        if (stored.isEmpty()) return BowlMenuTarget.ApplyResult.refused(BowlRefusal.EMPTY_SLOT);
        var extracted = new ItemStack(BowlResources.item(stored.resource()), stored.count());
        if (!work.extractItemSlot(slot, revision)) return BowlMenuTarget.ApplyResult.refused(BowlRefusal.REVISION_EXHAUSTED);
        commandRevision = work.revision();
        setChanged();
        return BowlMenuTarget.ApplyResult.extracted(extracted);
    }
}
