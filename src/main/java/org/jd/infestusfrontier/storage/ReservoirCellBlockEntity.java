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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.core.storage.Box;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.storage.api.BiomassCodecs;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

/**
 * One cell's share of its body, at most {@value #CAPACITY_BU} BU, and the box of the body it belongs to. As a
 * {@link BiomassHandler} it speaks for the whole body through {@link Reservoir}: a bucket, a vein or a junction on any
 * cell fills the body from the bottom layer and drains it from the top.
 */
final class ReservoirCellBlockEntity extends BlockEntity implements BiomassHandler, MenuProvider, Bodies.Cell {
    static final int CAPACITY_BU = 4000;
    static final Biomass CAPACITY = Biomass.of(CAPACITY_BU);

    private record Saved(BiomassTank tank, Optional<Box> body) {}

    private static final MapCodec<Saved> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomassCodecs.TANK.fieldOf("tank").forGetter(Saved::tank),
                    Bodies.BOX.optionalFieldOf("body").forGetter(Saved::body))
            .apply(instance, Saved::new));

    private BiomassTank tank = BiomassTank.empty(CAPACITY);
    private Box body;
    private Optional<CompoundTag> unreadable = Optional.empty();

    ReservoirCellBlockEntity(BlockPos pos, BlockState state) {
        super(StorageModule.RESERVOIR_CELL_ENTITY.get(), pos, state);
        body = Bodies.single(pos);
    }

    /** This cell's own share, as it would leave on the item. */
    BiomassTank share() {
        return tank;
    }

    void setShare(BiomassTank share) {
        tank = share;
        setChanged();
    }

    @Override
    public Box body() {
        return body;
    }

    @Override
    public void setBody(Box next) {
        body = next;
        setChanged();
    }

    @Override
    public boolean isReadable() {
        return unreadable.isEmpty();
    }

    boolean isEmpty() {
        return unreadable.isEmpty() && tank.stored().isZero();
    }

    /** The body's balance; this cell's own share while part of the body is out of reach. */
    @Override
    public BiomassTank tank() {
        return Reservoir.tank(this);
    }

    @Override
    public Outcome<BiomassTank> insert(Biomass amount, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        return Reservoir.insert(this, amount, simulate);
    }

    @Override
    public Outcome<BiomassTank> extract(Biomass amount, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        return Reservoir.extract(this, amount, simulate);
    }

    /**
     * Every cell looks at its body once it is back: a body that changed while the cell was unloaded falls apart and
     * forms again, and a waiting cell joins a body whose chunk loaded after it.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            level.scheduleTick(worldPosition, getBlockState().getBlock(), Bodies.FORM_TICKS);
    }

    private static Optional<String> check(Saved saved) {
        if (!saved.tank().capacity().equals(CAPACITY))
            return Optional.of("tank capacity " + saved.tank().capacity().units());
        if (saved.tank().reserved().units() != 0)
            return Optional.of("reserved " + saved.tank().reserved().units());
        return Optional.empty();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<Saved> read = Persistence.read(
                SAVED, tag, registries, StorageModule.LOGGER, "Reservoir Cell", ReservoirCellBlockEntity::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        tank = read.map(Saved::tank).orElse(tank);
        body = read.flatMap(Saved::body)
                .filter(box -> Bodies.holds(box, worldPosition))
                .orElse(Bodies.single(worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else
            Persistence.write(
                    SAVED, new Saved(tank, body.single() ? Optional.empty() : Optional.of(body)), registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else components.set(StorageModule.TANK.get(), tank);
    }

    /** A carried share that fails its check is kept as unreadable bytes, never replaced, as the Bladder does. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        BiomassTank carried = input.getOrDefault(StorageModule.TANK.get(), tank);
        Saved saved = new Saved(carried, Optional.empty());
        Optional<String> problem = check(saved);
        if (problem.isPresent()) {
            StorageModule.LOGGER.warn("Reservoir Cell keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null ? new CompoundTag() : Persistence.encode(SAVED, saved, level.registryAccess()));
            return;
        }
        tank = carried;
    }

    /** The share travels as a component; the body never leaves its place. */
    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("tank");
        tag.remove("body");
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ReservoirCellMenu(id, inventory, this);
    }
}
