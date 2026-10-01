package org.jd.infestusfrontier.processing.digestion;

import net.minecraft.server.level.ServerPlayer;
import org.jd.infestusfrontier.discovery.api.DiscoveryObserver;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.storage.api.EquipmentFuelPort;
import org.jd.infestusfrontier.storage.api.EquipmentFueling;
import org.jd.infestusfrontier.storage.api.QuantityStore;

final class BiomassBladderEntity extends BlockEntity implements EquipmentFueling {
    static final int CAPACITY = 4_000;
    private final DiscoveryObserver discovery;
    private QuantityStore store = emptyStore();
    private CompoundTag rejected;

    BiomassBladderEntity(BlockPos pos, BlockState state,
            Supplier<BlockEntityType<BiomassBladderEntity>> type,
            DiscoveryObserver discovery) {
        super(type.get(), pos, state);
        this.discovery = discovery;
    }

    boolean interact(Player player, InteractionHand hand, Item biomassBucket) {
        if (rejected != null) return false;
        var result = BiomassBucketTransfer.transfer(store, player, hand, biomassBucket, discovery);
        if (result != BiomassBucketTransfer.Result.PASS) {
            if (result == BiomassBucketTransfer.Result.FILLED_BUCKET
                    && player instanceof ServerPlayer serverPlayer) {
                discovery.complete(serverPlayer,
                        DiscoveryObserver.Milestone.BIOMASS_BLADDER);
            }
            setChanged();
            BiomassInteractions.status(player, store.fluidAmount("biomass"), CAPACITY);
            return true;
        }
        return false;
    }

    QuantityStore.Snapshot snapshot() { return store.snapshot(); }

    @Override
    public EquipmentFuelPort.Result fillEquipment(java.util.UUID player, EquipmentFuelPort.Target target) {
        if (rejected != null) return EquipmentFuelPort.Result.INVALID_TARGET;
        if (level == null || level.isClientSide || TransferAdmission.take(level.getServer(), player)
                != org.jd.infestusfrontier.storage.api.PortableTransferAdmission.Result.ACCEPTED) {
            return EquipmentFuelPort.Result.RATE_LIMITED;
        }
        var result = new EquipmentFuelPort(store).fill(target);
        if (result == EquipmentFuelPort.Result.FILLED) setChanged();
        return result;
    }

    @Override public void setChanged() {
        if (level != null && !level.isClientSide) {
            level.blockEntityChanged(worldPosition);
            updateAppearance();
        }
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && rejected == null) updateAppearance();
    }

    private void updateAppearance() {
        int amount = store.fluidAmount("biomass");
        int fill = amount == 0 ? 0 : (amount * 4 + CAPACITY - 1) / CAPACITY;
        if (getBlockState().getValue(BiomassBladderBlock.FILL) != fill) {
            level.setBlock(worldPosition, getBlockState().setValue(BiomassBladderBlock.FILL, fill),
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                            | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE, 0);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (rejected != null) tag.merge(rejected);
        else tag.put("bladder", DigestionSave.writeStore(store.snapshot()));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        try {
            if (!tag.contains("bladder", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("Missing Bladder state");
            var loaded = DigestionSave.readStore(tag.getCompound("bladder"), 0, 1);
            if (loaded.tanks().getFirst().capacity() != CAPACITY
                    || (!loaded.tanks().getFirst().isEmpty()
                    && !loaded.tanks().getFirst().resource().equals("biomass"))
                    || !loaded.reservations().isEmpty()) {
                throw new IllegalArgumentException("Invalid Bladder store");
            }
            store = QuantityStore.restore(loaded);
            rejected = null;
        } catch (RuntimeException exception) {
            rejected = tag.copy();
            LogUtils.getLogger().error("Rejected Biomass Bladder at {}: {}; interactions disabled",
                    worldPosition, exception.getMessage());
        }
    }

    private static QuantityStore emptyStore() {
        return new QuantityStore(List.of(), List.of(QuantityStore.Tank.empty(CAPACITY)));
    }
}
