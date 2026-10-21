package org.jd.infestusfrontier.equipment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.equipment.api.Reserves;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

/**
 * The papilla's {@value #CAPACITY_BU} BU tank and its feeding: once a second, every player standing on the cell has
 * each worn awakened piece filled with up to {@value #RATE_BU} BU, as far as the tank goes. One entity query a
 * second over the block above, nothing while the tank is empty.
 */
final class FuelPapillaBlockEntity extends BlockEntity implements BiomassHandler {
    static final int CAPACITY_BU = 1000;
    static final Biomass RATE = Biomass.of(10);
    static final int FEED_TICKS = 20;
    private static final List<EquipmentSlot> ARMOR =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    private static final MapCodec<BiomassTank> SAVED = RecordCodecBuilder.mapCodec(
            instance -> instance.group(BiomassCodecs.TANK.fieldOf("tank").forGetter(tank -> tank))
                    .apply(instance, tank -> tank));

    private BiomassTank tank = BiomassTank.empty(Biomass.of(CAPACITY_BU));
    private Optional<CompoundTag> unreadable = Optional.empty();
    private int nextFeed;

    FuelPapillaBlockEntity(BlockPos pos, BlockState state) {
        super(EquipmentModule.FUEL_PAPILLA_ENTITY.get(), pos, state);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, FuelPapillaBlockEntity papilla) {
        if (--papilla.nextFeed > 0) return;
        papilla.nextFeed = FEED_TICKS;
        if (papilla.unreadable.isPresent() || papilla.tank.stored().isZero()) return;
        AABB above = new AABB(pos.above());
        for (Player player : level.getEntitiesOfClass(Player.class, above)) {
            for (EquipmentSlot slot : ARMOR) {
                if (papilla.tank.stored().isZero()) return;
                ItemStack piece = player.getItemBySlot(slot);
                Biomass taken = Reserves.fill(piece, RATE.min(papilla.tank.stored()));
                if (taken.isZero()) continue;
                papilla.tank = ((Outcome.Done<BiomassTank>) papilla.tank.drain(taken)).value();
                papilla.setChanged();
            }
        }
    }

    @Override
    public BiomassTank tank() {
        return tank;
    }

    @Override
    public Outcome<BiomassTank> insert(Biomass amount, boolean simulate) {
        return apply(tank.fill(amount), simulate);
    }

    @Override
    public Outcome<BiomassTank> extract(Biomass amount, boolean simulate) {
        return apply(tank.drain(amount), simulate);
    }

    private Outcome<BiomassTank> apply(Outcome<BiomassTank> change, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (!simulate && change instanceof Outcome.Done<BiomassTank> done) {
            tank = done.value();
            setChanged();
        }
        return change;
    }

    private static Optional<String> check(BiomassTank saved) {
        return saved.capacity().units() == CAPACITY_BU
                ? Optional.empty()
                : Optional.of("tank capacity " + saved.capacity().units());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<BiomassTank> read = Persistence.read(
                SAVED, tag, registries, EquipmentModule.LOGGER, "Fuel Papilla", FuelPapillaBlockEntity::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(saved -> tank = saved);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, tank, registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else components.set(StorageModule.TANK.get(), tank);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        BiomassTank carried = input.getOrDefault(StorageModule.TANK.get(), tank);
        if (check(carried).isEmpty()) tank = carried;
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("tank");
    }
}
