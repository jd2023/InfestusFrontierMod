package org.jd.infestusfrontier.processing.digestion;

import com.mojang.logging.LogUtils;
import java.util.function.Supplier;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.processing.RecipeAdmission;
import org.jd.infestusfrontier.processing.digestion.api.DigestiveSac;

final class DigestiveSacEntity extends BlockEntity {
    private final DiscoveryObserver discovery;
    private UUID operator;
    private boolean pendingCredit;
    private DigestiveSac sac;
    private CompoundTag rejected;

    DigestiveSacEntity(BlockPos pos, BlockState state, Supplier<BlockEntityType<DigestiveSacEntity>> type,
            DiscoveryObserver discovery) {
        super(type.get(), pos, state);
        this.discovery = discovery;
        sac = DigestiveSac.create(DigestiveSac.STARTER_CAPACITY, this::admitCompletion);
    }

    private boolean admitCompletion() {
        return level != null && !level.isClientSide && RecipeAdmission.take(level.getServer());
    }

    void tick() {
        if (rejected != null || level == null || level.isClientSide) return;
        if (pendingCredit && level.getGameTime() % 20 == 0) {
            reconcileDiscovery(level.getServer().getPlayerList().getPlayer(operator));
        }
        if (sac.snapshot().activeBatch() == null) return;
        if (!getBlockState().canSurvive(level, worldPosition)) return;
        sac.advance(1);
        if (sac.snapshot().activeBatch() == null && operator != null) {
            pendingCredit = true;
            reconcileDiscovery(level.getServer().getPlayerList().getPlayer(operator));
        }
        setChanged();
        updateAppearance();
    }

    boolean interact(Player player, InteractionHand hand, Item biomassBucket) {
        if (rejected != null) {
            player.displayClientMessage(Component.translatable("message.infestusfrontier.biomass.invalid_save"), false);
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer) reconcileDiscovery(serverPlayer);
        var held = player.getItemInHand(hand);
        if (BiomassInteractions.transfer(sac.quantities(), player, hand, biomassBucket, discovery)
                != BiomassInteractions.TransferResult.PASS) {
            setChanged();
        } else {
            String feed = BiomassInteractions.feed(held);
            if (feed.isEmpty()) return false;
            if (pendingCredit) {
                player.displayClientMessage(Component.translatable(
                        "message.infestusfrontier.digestive_sac.pending_credit"), false);
                return true;
            }
            var result = sac.feed(feed);
            if (result instanceof DigestiveSac.Fed) {
                operator = player instanceof ServerPlayer ? player.getUUID() : null;
                held.consume(1, player);
                player.displayClientMessage(Component.translatable("message.infestusfrontier.digestive_sac.started"), false);
            } else {
                var refused = (DigestiveSac.FeedRefused) result;
                player.displayClientMessage(Component.translatable("message.infestusfrontier.digestive_sac.refused."
                        + refused.reason().name().toLowerCase(java.util.Locale.ROOT)), false);
            }
            setChanged();
            updateAppearance();
        }
        BiomassInteractions.status(player, sac.snapshot().biomass(),
                sac.snapshot().quantities().tanks().getFirst().capacity());
        return true;
    }

    private void reconcileDiscovery(ServerPlayer player) {
        if (!pendingCredit || player == null || !player.getUUID().equals(operator)) return;
        discovery.complete(player, DiscoveryObserver.Milestone.DIGESTIVE_SAC);
        pendingCredit = false;
        operator = null;
        setChanged();
    }

    void status(Player player) {
        if (rejected != null) {
            player.displayClientMessage(Component.translatable("message.infestusfrontier.biomass.invalid_save"), false);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) reconcileDiscovery(serverPlayer);
        BiomassInteractions.status(player, sac.snapshot().biomass(),
                sac.snapshot().quantities().tanks().getFirst().capacity());
    }

    DigestiveSac.State snapshot() { return sac.snapshot(); }

    private void updateAppearance() {
        boolean active = sac.snapshot().activeBatch() != null;
        if (getBlockState().getValue(DigestiveSacBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, getBlockState().setValue(DigestiveSacBlock.ACTIVE, active),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE, 0);
        }
    }

    @Override public void setChanged() {
        if (level != null && !level.isClientSide) level.blockEntityChanged(worldPosition);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (rejected != null) tag.merge(rejected);
        else {
            tag.put("sac", DigestionSave.writeSac(sac.snapshot()));
            if (operator != null) tag.putUUID("operator", operator);
            if (pendingCredit) tag.putBoolean("pendingCredit", true);
        }
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            if (!tag.contains("sac", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Missing Digestive Sac state");
            var state = DigestionSave.readSac(tag.getCompound("sac"));
            if (state.quantities().tanks().getFirst().capacity() != DigestiveSac.STARTER_CAPACITY
                    || (!state.quantities().tanks().getFirst().isEmpty()
                    && !state.quantities().tanks().getFirst().resource().equals("biomass"))) {
                throw new IllegalArgumentException("Invalid Digestive Sac tank");
            }
            var savedOperator = tag.hasUUID("operator") ? tag.getUUID("operator") : null;
            boolean savedPending = tag.getBoolean("pendingCredit");
            if ((tag.contains("operator") && savedOperator == null)
                    || (tag.contains("pendingCredit") && !tag.contains("pendingCredit", Tag.TAG_BYTE))
                    || (savedPending && (savedOperator == null || state.activeBatch() != null
                    || state.history().completedBatches() == 0))) {
                throw new IllegalArgumentException("Invalid Digestive Sac discovery credit");
            }
            sac = DigestiveSac.restore(state, this::admitCompletion);
            operator = savedOperator;
            pendingCredit = savedPending;
            rejected = null;
        } catch (RuntimeException exception) {
            rejected = tag.copy();
            LogUtils.getLogger().error("Rejected Digestive Sac at {}: {}; interactions disabled",
                    worldPosition, exception.getMessage());
        }
    }
}
