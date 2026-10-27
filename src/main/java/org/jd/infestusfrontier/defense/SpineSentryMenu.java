package org.jd.infestusfrontier.defense;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.core.defense.Sentry;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/** The server half of the sentry's screen: its store, take-only, in two rows of four, and the tank. */
public final class SpineSentryMenu extends OrganMenu {
    static final int ROW = 4;
    private static final int STORED = 0;
    private static final int CAPACITY = 1;

    /** Server side. */
    SpineSentryMenu(int id, Inventory inventory, SpineSentryBlockEntity sentry) {
        this(
                id,
                inventory,
                sentry.store(),
                reading(
                        () -> sentry.tank().stored().units(),
                        () -> sentry.tank().capacity().units()),
                at(sentry));
    }

    /** Client side. */
    public SpineSentryMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(id, inventory, new ItemStackHandler(Sentry.STORE_SLOTS), new SimpleContainerData(2), at(inventory, extra));
    }

    private SpineSentryMenu(
            int id, Inventory inventory, IItemHandler store, ContainerData data, ContainerLevelAccess access) {
        super(DefenseModule.SPINE_SENTRY_MENU.get(), id, access, DefenseModule.SPINE_SENTRY.get(), data);
        for (int slot = 0; slot < Sentry.STORE_SLOTS; slot++) {
            int x = Sheet.SLOT_X + (slot % ROW) * (Sheet.SLOT.width() + Sheet.GAP);
            int y = slot < ROW ? Sheet.SLOT_Y : Sheet.ROW2_SLOT_Y;
            addSlot(new SlotItemHandler(store, slot, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addPlayer(inventory, 1);
    }

    @Override
    public int stored() {
        return synced(STORED);
    }

    @Override
    public int capacity() {
        return Math.max(1, synced(CAPACITY));
    }
}
