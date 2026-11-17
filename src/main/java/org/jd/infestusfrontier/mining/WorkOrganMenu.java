package org.jd.infestusfrontier.mining;

import java.util.List;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.Block;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.core.mining.Collecting;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.foundation.Persistence;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.logistics.api.ItemAccess;

/**
 * The server half of the Fracture Jaw's and the Collection Cilia's screens. They hold no slots: as synced ints they
 * show the biomass, the stop, the history count and the seconds to the next try.
 */
public final class WorkOrganMenu extends OrganMenu {
    /** Which organ the screen shows. */
    public enum Kind {
        JAW(Fracturing.STOPS),
        CILIA(Stream.concat(
                        Collecting.STOPS.stream(),
                        Stream.of(
                                ItemAccess.NOT_GIVING,
                                ItemAccess.NETWORK,
                                ItemAccess.BUSY,
                                ItemAccess.TURNED_AWAY,
                                Persistence.CORRUPT))
                .toList());

        /** Every stop the organ names, in a fixed order: the menu sends one by its index. */
        final List<Refusal> stops;

        Kind(List<Refusal> stops) {
            this.stops = stops;
        }

        MenuType<WorkOrganMenu> menuType() {
            return this == JAW ? MiningModule.FRACTURE_JAW_MENU.get() : MiningModule.COLLECTION_CILIA_MENU.get();
        }

        Block block() {
            return this == JAW ? MiningModule.FRACTURE_JAW.get() : MiningModule.COLLECTION_CILIA.get();
        }
    }

    private static final int STORED = 0;
    private static final int CAPACITY = 1;
    private static final int STOP = 2;
    private static final int COUNT = 3;
    private static final int SECONDS = 4;
    private static final int SYNCED = 5;
    private static final int TICKS_PER_SECOND = 20;

    private final Kind kind;

    /** Server side, for a jaw. */
    WorkOrganMenu(int id, Inventory inventory, FractureJawBlockEntity jaw) {
        this(
                Kind.JAW,
                id,
                inventory,
                reading(reads(
                        Kind.JAW,
                        () -> jaw.tank().stored().units(),
                        () -> jaw.tank().capacity().units(),
                        jaw::stop,
                        () -> jaw.progress().cuts(),
                        () -> jaw.progress().ticksLeft())),
                at(jaw));
    }

    /** Server side, for cilia. */
    WorkOrganMenu(int id, Inventory inventory, CollectionCiliaBlockEntity cilia) {
        this(
                Kind.CILIA,
                id,
                inventory,
                reading(reads(
                        Kind.CILIA,
                        () -> cilia.tank().stored().units(),
                        () -> cilia.tank().capacity().units(),
                        cilia::stop,
                        cilia::sent,
                        cilia::waiting)),
                at(cilia));
    }

    /** Client side. The position arrives as extra data. */
    public WorkOrganMenu(Kind kind, int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(kind, id, inventory, new SimpleContainerData(SYNCED), at(inventory, extra));
    }

    private WorkOrganMenu(Kind kind, int id, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(kind.menuType(), id, access, kind.block(), data);
        this.kind = kind;
        addPlayer(inventory, 0);
    }

    private static IntSupplier[] reads(
            Kind kind,
            IntSupplier stored,
            IntSupplier capacity,
            Supplier<Optional<Refusal>> stop,
            IntSupplier count,
            IntSupplier ticksLeft) {
        IntSupplier[] reads = new IntSupplier[SYNCED];
        reads[STORED] = stored;
        reads[CAPACITY] = capacity;
        reads[STOP] = () -> stop.get().map(why -> kind.stops.indexOf(why) + 1).orElse(0);
        reads[COUNT] = count;
        reads[SECONDS] = () -> (ticksLeft.getAsInt() + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
        return reads;
    }

    Kind kind() {
        return kind;
    }

    @Override
    public int stored() {
        return synced(STORED);
    }

    @Override
    public int capacity() {
        return Math.max(1, synced(CAPACITY));
    }

    /** Why the organ stopped, as last synced, or empty while it works or waits. */
    public Optional<Refusal> stop() {
        int index = synced(STOP) - 1;
        return index >= 0 && index < kind.stops.size() ? Optional.of(kind.stops.get(index)) : Optional.empty();
    }

    /** The translation key of the reason the organ stopped, or empty while it works or waits. */
    public Optional<String> stopKey() {
        return stop().map(stop -> "infestusfrontier.refusal." + stop.key());
    }

    /** The organ's history: blocks cut, or items sent. */
    public int count() {
        return synced(COUNT);
    }

    /** Seconds until the next try. */
    public int seconds() {
        return synced(SECONDS);
    }
}
