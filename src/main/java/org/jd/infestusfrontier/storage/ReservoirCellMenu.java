package org.jd.infestusfrontier.storage;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;

/** The cell screen's server half: the body's balance and its size, synced as ints. */
public final class ReservoirCellMenu extends OrganMenu {
    private static final int STORED = 0;
    private static final int CAPACITY = 1;
    private static final int SIZE_X = 2;
    private static final int SIZE_Y = 3;
    private static final int SIZE_Z = 4;

    /** Server side. */
    ReservoirCellMenu(int id, Inventory inventory, ReservoirCellBlockEntity cell) {
        this(
                id,
                inventory,
                reading(
                        () -> cell.tank().stored().units(),
                        () -> cell.tank().capacity().units(),
                        () -> cell.body().sizeX(),
                        () -> cell.body().sizeY(),
                        () -> cell.body().sizeZ()),
                at(cell));
    }

    /** Client side. The position arrives as extra data. */
    public ReservoirCellMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, new SimpleContainerData(5), at(inventory, extra));
    }

    private ReservoirCellMenu(int id, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
        super(StorageModule.RESERVOIR_CELL_MENU.get(), id, access, StorageModule.RESERVOIR_CELL.get(), data);
        addPlayer(inventory, 0);
    }

    @Override
    public int stored() {
        return synced(STORED);
    }

    @Override
    public int capacity() {
        return Math.max(1, synced(CAPACITY));
    }

    int sizeX() {
        return synced(SIZE_X);
    }

    int sizeY() {
        return synced(SIZE_Y);
    }

    int sizeZ() {
        return synced(SIZE_Z);
    }

    int cells() {
        return sizeX() * sizeY() * sizeZ();
    }
}
