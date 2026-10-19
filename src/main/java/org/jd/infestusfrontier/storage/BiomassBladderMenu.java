package org.jd.infestusfrontier.storage;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The Bladder screen's server half: one service slot for a piece of living equipment, the Fill button, and the
 * balance as two synced ints. Nothing here decides a transfer; the block entity does.
 */
public final class BiomassBladderMenu extends OrganMenu {
    public static final int FILL_BUTTON = 0;
    static final int SERVICE_SLOT = 0;
    private static final int STORED = 0;
    private static final int CAPACITY = 1;

    private final Container service = new SimpleContainer(1);

    /** Server side. */
    BiomassBladderMenu(int id, Inventory inventory, BiomassBladderBlockEntity bladder) {
        this(
                id,
                inventory,
                reading(
                        () -> bladder.tank().stored().units(),
                        () -> bladder.tank().capacity().units()),
                at(bladder));
    }

    /** Client side. The position arrives as extra data. */
    public BiomassBladderMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, new SimpleContainerData(2), at(inventory, extra));
    }

    private BiomassBladderMenu(int id, Inventory inventory, ContainerData balance, ContainerLevelAccess access) {
        super(StorageModule.BIOMASS_BLADDER_MENU.get(), id, access, StorageModule.BIOMASS_BLADDER.get(), balance);
        addSlot(new Slot(service, 0, Sheet.SLOT_X, Sheet.SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
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

    public boolean hasServiceItem() {
        return !service.getItem(0).isEmpty();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access().execute((level, pos) -> clearContainer(player, service));
    }
}
