package org.jd.infestusfrontier.storage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
import org.jd.infestusfrontier.storage.api.BiomassHandler;
import org.jetbrains.annotations.Nullable;

/** The Bladder's balance. Holds 4000 BU; the block's {@code fill} follows the content. */
final class BiomassBladderBlockEntity extends BlockEntity implements BiomassHandler, MenuProvider {
    static final Biomass CAPACITY = Biomass.of(4000);
    private static final MapCodec<BiomassTank> SAVED = RecordCodecBuilder.mapCodec(
            instance -> instance.group(BiomassCodecs.TANK.fieldOf("tank").forGetter(tank -> tank))
                    .apply(instance, tank -> tank));

    private BiomassTank tank = BiomassTank.empty(CAPACITY);
    private Optional<CompoundTag> unreadable = Optional.empty();

    BiomassBladderBlockEntity(BlockPos pos, BlockState state) {
        super(StorageModule.BIOMASS_BLADDER_ENTITY.get(), pos, state);
    }

    @Override
    public BiomassTank tank() {
        return tank;
    }

    @Override
    public Outcome<BiomassTank> insert(Biomass amount) {
        return apply(tank.fill(amount));
    }

    @Override
    public Outcome<BiomassTank> extract(Biomass amount) {
        return apply(tank.drain(amount));
    }

    private Outcome<BiomassTank> apply(Outcome<BiomassTank> change) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (change instanceof Outcome.Done<BiomassTank> done) {
            tank = done.value();
            setChanged();
            if (level != null) {
                BlockState shown = getBlockState().setValue(BiomassBladderBlock.FILL, fillLevel(tank));
                if (shown != getBlockState()) level.setBlock(worldPosition, shown, Block.UPDATE_CLIENTS);
            }
        }
        return change;
    }

    /** 0 when empty, 4 when full, otherwise 1 to 3 so any content shows. */
    static int fillLevel(BiomassTank tank) {
        if (tank.stored().isZero()) return 0;
        if (tank.isFull()) return 4;
        return Math.clamp(1 + (long) tank.stored().units() * 3 / tank.capacity().units(), 1, 3);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<BiomassTank> read = Persistence.read(SAVED, tag, StorageModule.LOGGER, "Biomass Bladder");
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(saved -> tank = saved);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, tank, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(StorageModule.TANK.get(), tank);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        BiomassTank carried = input.getOrDefault(StorageModule.TANK.get(), tank);
        if (carried.capacity().equals(CAPACITY)) tank = carried;
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("tank");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.infestusfrontier.biomass_bladder");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BiomassBladderMenu(id, inventory, this);
    }
}
