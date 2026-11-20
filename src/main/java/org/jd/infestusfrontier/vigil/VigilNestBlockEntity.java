package org.jd.infestusfrontier.vigil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.core.vigil.Territory;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.organ.api.Organs;
import org.jd.infestusfrontier.organ.api.Steward;
import org.jd.infestusfrontier.organ.api.Stewarded;
import org.jd.infestusfrontier.storage.api.BiomassHandler;

final class VigilNestBlockEntity extends BlockEntity implements MenuProvider, BiomassHandler, Stewarded {
    static final int STARTUP_GRACE = 40;
    private NestContents contents = NestContents.EMPTY;
    private Optional<CompoundTag> unreadable = Optional.empty();
    private final Steward steward = new Steward();
    private int grace;
    private int stop;
    private boolean admitted;

    VigilNestBlockEntity(BlockPos pos, BlockState state) {
        super(VigilModule.NEST_ENTITY.get(), pos, state);
    }

    @Override
    public Steward steward() {
        return steward;
    }

    @Override
    public BiomassTank tank() {
        return contents.tank();
    }

    int mask() {
        return contents.mask();
    }

    int paid() {
        return contents.paid();
    }

    int stop() {
        return unreadable.isPresent() ? 8 : stop;
    }

    boolean enabled() {
        return contents.enabled();
    }

    boolean egg() {
        return contents.egg();
    }

    boolean owns(UUID id) {
        return unreadable.isEmpty()
                && enabled()
                && contents.keeper().filter(id::equals).isPresent();
    }

    @Override
    public Outcome<BiomassTank> insert(Biomass amount, boolean simulate) {
        return apply(tank().fill(amount), simulate);
    }

    @Override
    public Outcome<BiomassTank> extract(Biomass amount, boolean simulate) {
        return apply(tank().drain(amount), simulate);
    }

    private boolean live() {
        return !isRemoved()
                && level != null
                && level.hasChunkAt(worldPosition)
                && level.getBlockEntity(worldPosition) == this;
    }

    private Outcome<BiomassTank> apply(Outcome<BiomassTank> result, boolean simulate) {
        if (unreadable.isPresent()) return Outcome.refused(Persistence.CORRUPT);
        if (!live()) return Outcome.refused(new org.jd.infestusfrontier.core.foundation.Refusal("vigil.unloaded"));
        if (!simulate && result instanceof Outcome.Done<BiomassTank>(BiomassTank tank)) {
            contents = new NestContents(tank, mask(), paid(), enabled(), egg(), contents.keeper());
            setChanged();
        }
        return result;
    }

    boolean seed() {
        if (unreadable.isPresent() || egg()) return false;
        contents = new NestContents(tank(), mask(), 0, false, true, Optional.empty());
        setChanged();
        return true;
    }

    int capacity() {
        if (level == null) return 1;
        int feeders = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos pos = worldPosition.relative(side);
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(VigilModule.FEEDER.get())) feeders++;
        }
        return Territory.capacity(feeders);
    }

    void action(int button) {
        if (unreadable.isPresent() || !(level instanceof ServerLevel server)) return;
        if (button >= 0 && button < 9) {
            if (enabled()) {
                stop = 9;
                return;
            }
            Territory next = new Territory(mask()).toggle(button);
            if (next.chunks() > capacity()) {
                stop = 3;
                return;
            }
            contents = new NestContents(tank(), next.mask(), 0, false, egg(), Optional.empty());
            stop = 0;
            setChanged();
        } else if (button == 9) {
            if (enabled()) shutdown(server, false, 0);
            else start(server);
        }
    }

    private void start(ServerLevel server) {
        Territory territory = new Territory(mask());
        if (!Organs.hasFoundation(server, worldPosition)) {
            stop = 1;
            return;
        }
        if (!egg()) {
            stop = 2;
            return;
        }
        if (territory.chunks() > capacity()) {
            stop = 3;
            return;
        }
        if (tank().stored().units() < territory.payment()) {
            stop = 4;
            return;
        }
        if (!server.noCollision(VigilModule.KEEPER
                .get()
                .getDimensions()
                .makeBoundingBox(worldPosition.getX() + 0.5, worldPosition.getY() + 2.5, worldPosition.getZ() + 0.5))) {
            stop = 5;
            return;
        }
        VigilTickets tickets = VigilTickets.forLevel(server);
        if (!tickets.admit(server, worldPosition, territory)) {
            stop = 6;
            return;
        }
        VigilKeeper keeper = VigilModule.KEEPER.get().create(server);
        if (keeper == null) {
            tickets.release(server, worldPosition);
            stop = 5;
            return;
        }
        keeper.bind(worldPosition);
        keeper.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 2.5, worldPosition.getZ() + 0.5);
        if (!server.addFreshEntity(keeper)) {
            tickets.release(server, worldPosition);
            stop = 5;
            return;
        }
        BiomassTank after = ((Outcome.Done<BiomassTank>) tank().drain(Biomass.of(territory.payment()))).value();
        contents = new NestContents(after, mask(), Territory.PAYMENT_TICKS, true, true, Optional.of(keeper.getUUID()));
        admitted = true;
        tickets.acquire(server, worldPosition, territory);
        stop = 0;
        setChanged();
        steward.worked(server, VigilModule.NEST.getId());
    }

    /** Called only by the bounded persisted-ticket validator. Startup uses real prepaid ticks. */
    boolean resume(Territory territory) {
        if (unreadable.isPresent() || !enabled() || contents.keeper().isEmpty() || mask() != territory.mask())
            return false;
        int remaining = paid();
        BiomassTank reserve = tank();
        if (remaining <= STARTUP_GRACE) {
            Outcome<BiomassTank> payment = reserve.drain(Biomass.of(territory.payment()));
            if (!(payment instanceof Outcome.Done<BiomassTank> done)) return false;
            reserve = done.value();
            remaining = Territory.PAYMENT_TICKS;
        }
        contents = new NestContents(
                reserve, mask(), Math.subtractExact(remaining, STARTUP_GRACE), true, egg(), contents.keeper());
        grace = STARTUP_GRACE;
        admitted = true;
        setChanged();
        return true;
    }

    static void tick(Level level, BlockPos pos, BlockState state, VigilNestBlockEntity nest) {
        if (!(level instanceof ServerLevel server) || nest.unreadable.isPresent() || !nest.enabled()) return;
        if (!nest.admitted) {
            nest.shutdown(server, false, 6);
            return;
        }
        if (!Organs.hasFoundation(server, pos)) {
            nest.shutdown(server, false, 1);
            return;
        }
        if (new Territory(nest.mask()).chunks() > nest.capacity()) {
            nest.shutdown(server, false, 3);
            return;
        }
        Entity creature = nest.contents.keeper().map(server::getEntity).orElse(null);
        if (!(creature instanceof VigilKeeper keeper) || !keeper.isAlive() || !keeper.belongsTo(pos)) {
            if (nest.grace > 0 && creature == null) {
                nest.grace--;
                return;
            }
            nest.shutdown(server, true, 7);
            return;
        }
        nest.grace = 0;
        int paid = nest.paid();
        BiomassTank tank = nest.tank();
        if (paid == 0) {
            Outcome<BiomassTank> payment = tank.drain(Biomass.of(new Territory(nest.mask()).payment()));
            if (!(payment instanceof Outcome.Done<BiomassTank> done)) {
                nest.shutdown(server, false, 4);
                return;
            }
            tank = done.value();
            paid = Territory.PAYMENT_TICKS;
        }
        nest.contents =
                new NestContents(tank, nest.mask(), Math.decrementExact(paid), true, true, nest.contents.keeper());
        nest.setChanged();
    }

    /** A dead or missing keeper cannot be sheltered by breaking the nest between server ticks. */
    private boolean sheltered() {
        if (!egg() || !enabled()) return egg();
        if (!(level instanceof ServerLevel server)) return false;
        Optional<Entity> creature = contents.keeper().map(server::getEntity);
        if (creature.isPresent()) return creature.get().isAlive();
        return grace > 0 && !server.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(worldPosition).toLong());
    }

    void shutdown(ServerLevel server, boolean dead, int reason) {
        boolean sheltered = !dead && sheltered();
        VigilTickets.forLevel(server).release(server, worldPosition);
        contents.keeper()
                .map(server::getEntity)
                .filter(entity -> entity instanceof VigilKeeper keeper && keeper.belongsTo(worldPosition))
                .ifPresent(Entity::discard);
        contents = new NestContents(tank(), mask(), 0, false, sheltered, Optional.empty());
        admitted = false;
        grace = 0;
        stop = reason;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if (admitted && level instanceof ServerLevel server) shutdown(server, false, 0);
        super.loadAdditional(tag, registries);
        Optional<NestContents> read = Persistence.read(
                NestContents.CODEC,
                tag,
                registries,
                VigilModule.LOGGER,
                "Vigil Nest " + worldPosition,
                NestContents::problem);
        unreadable = read.isPresent() ? Optional.empty() : Optional.of(tag.copy());
        contents = read.orElse(NestContents.EMPTY);
        steward.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (unreadable.isPresent()) tag.merge(unreadable.get());
        else {
            Persistence.write(NestContents.CODEC, contents, registries, tag);
            steward.save(tag);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (unreadable.isPresent())
            components.set(OrganModule.UNREADABLE_SAVE.get(), unreadable.get().copy());
        else
            components.set(
                    VigilModule.CONTENTS.get(),
                    new NestContents(tank(), mask(), 0, false, sheltered(), Optional.empty()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag raw = input.get(OrganModule.UNREADABLE_SAVE.get());
        if (raw != null) {
            unreadable = Optional.of(raw.copy());
            return;
        }
        NestContents carried = input.get(VigilModule.CONTENTS.get());
        if (carried != null) {
            if (carried.problem().isPresent()
                    || carried.enabled()
                    || carried.keeper().isPresent()) {
                unreadable = Optional.of(
                        level == null
                                ? new CompoundTag()
                                : Persistence.encode(NestContents.CODEC, carried, level.registryAccess()));
            } else contents = carried;
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        for (String key : List.of("tank", "mask", "paid", "enabled", "egg", "keeper")) tag.remove(key);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new VigilNestMenu(id, inventory, this);
    }
}
