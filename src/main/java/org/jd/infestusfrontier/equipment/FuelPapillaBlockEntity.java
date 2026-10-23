package org.jd.infestusfrontier.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jd.infestusfrontier.core.equipment.Refuel;
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
 * The papilla's one-packet buffer of {@value #CAPACITY_BU} BU and its feeding (BLOCK_CATALOG T1-36): once a second,
 * one living player standing on the cell has the selected worn awakened pieces filled up to the set limit, at most
 * {@value Refuel#RATE} BU in all, paying ceil(delivered / 5) BU more as overhead. It is no reservoir: veins, a
 * Junction or a bucket keep the buffer topped up, and a Filter Valve keeps the source's reserve. One entity query a
 * second over the block above, nothing while the buffer is empty.
 */
final class FuelPapillaBlockEntity extends BlockEntity implements BiomassHandler, MenuProvider {
    static final int CAPACITY_BU = 250;
    static final int FEED_TICKS = 20;
    /** The pieces in the order the screen lists them; bit i of {@link #pieces} selects piece i. */
    static final List<EquipmentSlot> ARMOR =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private static final int ALL_PIECES = 0b1111;

    private record Saved(BiomassTank tank, int pieces, int limit) {}

    private static final MapCodec<Saved> SAVED = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomassCodecs.TANK.fieldOf("tank").forGetter(Saved::tank),
                    Codec.INT.optionalFieldOf("pieces", ALL_PIECES).forGetter(Saved::pieces),
                    Codec.INT.optionalFieldOf("limit", 100).forGetter(Saved::limit))
            .apply(instance, Saved::new));

    private BiomassTank tank = BiomassTank.empty(Biomass.of(CAPACITY_BU));
    private int pieces = ALL_PIECES;
    private int limit = 100;
    private Optional<CompoundTag> unreadable = Optional.empty();
    private int nextFeed;

    FuelPapillaBlockEntity(BlockPos pos, BlockState state) {
        super(EquipmentModule.FUEL_PAPILLA_ENTITY.get(), pos, state);
    }

    int pieces() {
        return pieces;
    }

    int limit() {
        return limit;
    }

    boolean isUnreadable() {
        return unreadable.isPresent();
    }

    void togglePiece(int index) {
        if (unreadable.isPresent() || index < 0 || index >= ARMOR.size()) return;
        pieces ^= 1 << index;
        setChanged();
    }

    void cycleLimit() {
        if (unreadable.isPresent()) return;
        limit = Refuel.nextLimit(limit);
        setChanged();
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, FuelPapillaBlockEntity papilla) {
        if (--papilla.nextFeed > 0) return;
        papilla.nextFeed = FEED_TICKS;
        if (papilla.unreadable.isPresent() || papilla.tank.stored().isZero()) return;
        List<Player> standing =
                level.getEntitiesOfClass(Player.class, new AABB(pos.above()), EntitySelector.NO_SPECTATORS);
        for (Player player : standing) {
            if (!player.isAlive()) continue;
            papilla.feed(player);
            return; // one pad serves one wearer
        }
    }

    private void feed(Player wearer) {
        int budget = Refuel.affordable(tank.stored().units());
        int delivered = 0;
        for (int index = 0; index < ARMOR.size() && delivered < budget; index++) {
            if ((pieces & 1 << index) == 0) continue;
            ItemStack piece = wearer.getItemBySlot(ARMOR.get(index));
            Optional<BiomassTank> reserve = Reserves.of(piece);
            if (reserve.isEmpty()) continue;
            int wanted = Refuel.wanted(
                    reserve.get().stored().units(), reserve.get().capacity().units(), limit);
            int amount = Math.min(wanted, budget - delivered);
            if (amount > 0)
                delivered += Reserves.fill(piece, Biomass.of(amount)).units();
        }
        if (delivered == 0) return;
        tank = ((Outcome.Done<BiomassTank>) tank.drain(Biomass.of(Refuel.cost(delivered)))).value();
        setChanged();
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

    private static Optional<String> check(Saved saved) {
        if (saved.tank().capacity().units() != CAPACITY_BU)
            return Optional.of("tank capacity " + saved.tank().capacity().units());
        if (saved.pieces() < 0 || saved.pieces() > ALL_PIECES) return Optional.of("pieces " + saved.pieces());
        if (!Refuel.LIMITS.contains(saved.limit())) return Optional.of("limit " + saved.limit());
        return Optional.empty();
    }

    private void restore(Saved saved) {
        tank = saved.tank();
        pieces = saved.pieces();
        limit = saved.limit();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Optional<Saved> read = Persistence.read(
                SAVED, tag, registries, EquipmentModule.LOGGER, "Fuel Papilla", FuelPapillaBlockEntity::check);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        read.ifPresent(this::restore);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else Persistence.write(SAVED, new Saved(tank, pieces, limit), registries, tag);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else components.set(StorageModule.TANK.get(), tank);
    }

    /** A carried tank that fails its check is kept as unreadable bytes, never replaced, as the Bladder does. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag kept = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (kept != null) {
            unreadable = Optional.of(kept.copy());
            return;
        }
        BiomassTank carried = input.getOrDefault(StorageModule.TANK.get(), tank);
        Saved saved = new Saved(carried, pieces, limit);
        Optional<String> problem = check(saved);
        if (problem.isPresent()) {
            EquipmentModule.LOGGER.warn("Fuel Papilla keeps unreadable carried state: {}", problem.get());
            unreadable = Optional.of(
                    level == null ? new CompoundTag() : Persistence.encode(SAVED, saved, level.registryAccess()));
            return;
        }
        tank = carried;
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("tank");
        tag.remove("pieces");
        tag.remove("limit");
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FuelPapillaMenu(id, inventory, this);
    }
}
