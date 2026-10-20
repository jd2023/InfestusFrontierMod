package org.jd.infestusfrontier.logistics;

import java.util.function.IntSupplier;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/**
 * The junction screen's server half: the tank as stored, capacity and whether it is biomass, and the six ports.
 * Nothing here changes a port; the probe does.
 */
public final class VascularJunctionMenu extends OrganMenu {
    private static final int STORED = 0;
    private static final int CAPACITY = 1;
    private static final int BIOMASS = 2;
    private static final int FIRST_PORT = 3;
    private static final int SYNCED = FIRST_PORT + Direction.values().length;

    /** Server side. */
    VascularJunctionMenu(int id, Inventory inventory, VascularJunctionBlockEntity junction) {
        this(id, inventory, reading(reads(junction)), at(junction));
    }

    /** Client side. The position arrives as extra data. */
    public VascularJunctionMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, new SimpleContainerData(SYNCED), at(inventory, extra));
    }

    private VascularJunctionMenu(int id, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(LogisticsModule.VASCULAR_JUNCTION_MENU.get(), id, access, LogisticsModule.VASCULAR_JUNCTION.get(), data);
        addPlayer(inventory, 2);
    }

    private static IntSupplier[] reads(VascularJunctionBlockEntity junction) {
        IntSupplier[] reads = new IntSupplier[SYNCED];
        reads[STORED] = () -> junction.fluid().getFluidAmount();
        reads[CAPACITY] = () -> junction.fluid().getCapacity();
        reads[BIOMASS] = () -> junction.fluid().isEmpty()
                        || BiomassFluids.isBiomass(junction.fluid().getFluid())
                ? 1
                : 0;
        for (Direction side : Direction.values())
            reads[FIRST_PORT + side.ordinal()] = () -> junction.port(side).ordinal();
        return reads;
    }

    @Override
    public int stored() {
        return synced(STORED);
    }

    @Override
    public int capacity() {
        return Math.max(1, synced(CAPACITY));
    }

    /** True when the tank holds biomass or nothing; false for water and other fluids. */
    public boolean holdsBiomass() {
        return synced(BIOMASS) == 1;
    }

    /** The port on {@code side}, by its serialized name, for the screen's labels. */
    public String port(Direction side) {
        Port[] ports = Port.values();
        return ports[Math.clamp(synced(FIRST_PORT + side.ordinal()), 0, ports.length - 1)].getSerializedName();
    }
}
