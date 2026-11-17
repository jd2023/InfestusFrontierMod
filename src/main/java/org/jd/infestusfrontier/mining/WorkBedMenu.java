package org.jd.infestusfrontier.mining;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.core.mining.Fracturing;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The Work Bed screen's server half: its slots, take-only, in two rows, and as synced ints the items it holds and has
 * received. It holds no biomass.
 */
public final class WorkBedMenu extends OrganMenu {
    static final int ROW = 5;
    static final int STRIPS = 2;
    static final int PITCH = Sheet.SLOT.width() + Sheet.GAP;
    private static final int HELD = 0;
    private static final int RECEIVED = 1;
    private static final int SYNCED = 2;

    /** Server side. */
    WorkBedMenu(int id, Inventory inventory, WorkBedBlockEntity bed) {
        this(id, inventory, bed.slots(), reading(bed::held, bed::received), at(bed));
    }

    /** Client side. The position arrives as extra data. */
    public WorkBedMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(
                id,
                inventory,
                new ItemStackHandler(Fracturing.BED_SLOTS),
                new SimpleContainerData(SYNCED),
                at(inventory, extra));
    }

    private WorkBedMenu(
            int id, Inventory inventory, IItemHandler slots, ContainerData data, ContainerLevelAccess access) {
        super(MiningModule.WORK_BED_MENU.get(), id, access, MiningModule.WORK_BED.get(), data);
        for (int slot = 0; slot < Fracturing.BED_SLOTS; slot++)
            addSlot(
                    new SlotItemHandler(
                            slots,
                            slot,
                            Sheet.SLOT_X + (slot % ROW) * PITCH,
                            slot < ROW ? Sheet.SLOT_Y : Sheet.ROW2_SLOT_Y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }
                    });
        addPlayer(inventory, STRIPS);
    }

    /** Items the bed holds, as last synced. */
    public int held() {
        return synced(HELD);
    }

    /** Items the bed has received in all, as last synced. */
    public int received() {
        return synced(RECEIVED);
    }

    @Override
    public int stored() {
        return 0;
    }

    @Override
    public int capacity() {
        return 1;
    }
}
