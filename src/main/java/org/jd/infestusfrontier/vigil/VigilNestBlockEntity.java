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
            contents = contents.withTank(tank);
            setChanged();
        }
        return result;
    }

    boolean seed() {
        if (unreadable.isPresent() || egg()) return false;
        if (level == null || !Organs.hasFoundation(level, worldPosition)) {
            stop = 1;
            return false;
        }
        Outcome<BiomassTank> growth = tank().drain(Biomass.of(NestContents.GROWTH_COST));
        if (!(growth instanceof Outcome.Done<BiomassTank> done)) {
            stop = 10;
            return false;
        }
        contents =
                new NestContents(done.value(), mask(), 0, false, true, Optional.empty(), NestContents.FULL_HEALTH, 0);
        stop = 0;
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
            contents = new NestContents(
                    tank(), next.mask(), 0, false, egg(), Optional.empty(), contents.health(), contents.hungry());
            stop = 0;
            setChanged();
        } else if (button == 9) {
            if (enabled()) {
                if (grace > 0 && contents.keeper().map(server::getEntity).isEmpty()) {
                    stop = 11;
                    return;
                }
                shutdown(server, false, 0);
            } else start(server);
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
        keeper.setHealth(contents.health());
        keeper.bind(worldPosition);
        keeper.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 2.5, worldPosition.getZ() + 0.5);
        if (!server.addFreshEntity(keeper)) {
            tickets.release(server, worldPosition);
            stop = 5;
            return;
        }
        BiomassTank after = ((Outcome.Done<BiomassTank>) tank().drain(Biomass.of(territory.payment()))).value();
        contents = new NestContents(
                after,
                mask(),
                Territory.PAYMENT_TICKS,
                true,
                true,
                Optional.of(keeper.getUUID()),
                NestContents.FULL_HEALTH,
                0);
        admitted = true;
        tickets.acquire(server, worldPosition, territory);
        stop = 0;
        setChanged();
        steward.worked(server, VigilModule.NEST.getId());
    }

    /** Startup spends either prepaid time or the finite starvation ticket allowance. */
    boolean resume(Territory territory) {
        if (unreadable.isPresent() || !enabled() || contents.keeper().isEmpty() || mask() != territory.mask())
            return false;
        grace = STARTUP_GRACE;
        int remaining = paid();
        int hungry = contents.hungry();
        BiomassTank reserve = tank();
        if (hungry == 0 && remaining <= STARTUP_GRACE) {
            Outcome<BiomassTank> payment = reserve.drain(Biomass.of(territory.payment()));
            if (payment instanceof Outcome.Done<BiomassTank> done) {
                reserve = done.value();
                remaining = Territory.PAYMENT_TICKS;
            } else hungry = 1;
        }
        if (hungry > 0) {
            if (hungry > NestContents.HUNGER_TICKETS - STARTUP_GRACE) return false;
            hungry = Math.addExact(hungry, STARTUP_GRACE);
        }
        remaining = Math.max(0, remaining - STARTUP_GRACE);
        contents =
                new NestContents(reserve, mask(), remaining, true, egg(), contents.keeper(), contents.health(), hungry);
        admitted = true;
        setChanged();
        return true;
    }

    static void tick(Level level, BlockPos pos, BlockState state, VigilNestBlockEntity nest) {
        if (!(level instanceof ServerLevel server) || nest.unreadable.isPresent() || !nest.enabled()) return;
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
        Territory territory = new Territory(nest.mask());
        if (!Organs.hasFoundation(server, pos)) {
            nest.starve(server, keeper, 1);
            return;
        }
        if (territory.chunks() > nest.capacity()) {
            nest.starve(server, keeper, 3);
            return;
        }
        int paid = nest.paid();
        BiomassTank tank = nest.tank();
        if (paid == 0) {
            Outcome<BiomassTank> payment = tank.drain(Biomass.of(territory.payment()));
            if (!(payment instanceof Outcome.Done<BiomassTank> done)) {
                nest.starve(server, keeper, 4);
                return;
            }
            tank = done.value();
            paid = Territory.PAYMENT_TICKS;
        }
        if (!nest.admitted) {
            VigilTickets tickets = VigilTickets.forLevel(server);
            if (!tickets.admit(server, pos, territory)) {
                nest.shutdown(server, false, 6);
                return;
            }
            tickets.acquire(server, pos, territory);
            nest.admitted = true;
        }
        nest.contents = new NestContents(
                tank,
                nest.mask(),
                Math.decrementExact(paid),
                true,
                true,
                nest.contents.keeper(),
                NestContents.FULL_HEALTH,
                0);
        nest.stop = 0;
        nest.setChanged();
    }

    private void starve(ServerLevel server, VigilKeeper keeper, int reason) {
        int hungry = Math.min(NestContents.HUNGER_TICKETS, Math.incrementExact(contents.hungry()));
        contents = new NestContents(
                tank(),
                mask(),
                Math.max(0, paid() - 1),
                true,
                true,
                contents.keeper(),
                NestContents.FULL_HEALTH,
                hungry);
        stop = reason;
        // World time keeps damage cadence across saves without charging for offline time.
        if (server.getGameTime() % NestContents.DAMAGE_INTERVAL == 0)
            keeper.hurt(server.damageSources().starve(), 1);
        if (!keeper.isAlive()) {
            shutdown(server, true, 7);
            return;
        }
        // Even external healing cannot turn starvation into an unlimited free loader.
        if (admitted && hungry >= NestContents.HUNGER_TICKETS) {
            VigilTickets.forLevel(server).release(server, worldPosition);
            admitted = false;
        }
        setChanged();
    }

    int health() {
        return egg() ? (int) Math.ceil(shelteredHealth()) : 0;
    }

    boolean hungry() {
        return enabled() && contents.hungry() > 0;
    }

    private float shelteredHealth() {
        if (enabled() && level instanceof ServerLevel server) {
            Entity creature = contents.keeper().map(server::getEntity).orElse(null);
            if (creature instanceof VigilKeeper keeper && keeper.isAlive()) return keeper.getHealth();
        }
        return contents.health();
    }

    /** A dead or missing keeper cannot be sheltered by breaking the nest between server ticks. */
    private boolean sheltered() {
        if (!egg() || !enabled()) return egg();
        if (!(level instanceof ServerLevel server)) return false;
        Optional<Entity> creature = contents.keeper().map(server::getEntity);
        if (creature.isPresent()) return creature.get().isAlive();
        return false;
    }

    void shutdown(ServerLevel server, boolean dead, int reason) {
        boolean sheltered = !dead && sheltered();
        float health = shelteredHealth();
        VigilTickets.forLevel(server).release(server, worldPosition);
        contents.keeper()
                .map(server::getEntity)
                .filter(entity -> entity instanceof VigilKeeper keeper && keeper.belongsTo(worldPosition))
                .filter(Entity::isAlive)
                .ifPresent(Entity::discard);
        contents = new NestContents(tank(), mask(), 0, false, sheltered, Optional.empty(), health, contents.hungry());
        admitted = false;
        grace = 0;
        stop = reason;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        if ((admitted || enabled()) && level instanceof ServerLevel server) shutdown(server, false, 0);
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
        grace = enabled() ? STARTUP_GRACE : 0;
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
                    new NestContents(
                            tank(),
                            mask(),
                            0,
                            false,
                            sheltered(),
                            Optional.empty(),
                            shelteredHealth(),
                            contents.hungry()));
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
        for (String key : List.of("tank", "mask", "paid", "enabled", "egg", "keeper", "health", "hungry"))
            tag.remove(key);
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
