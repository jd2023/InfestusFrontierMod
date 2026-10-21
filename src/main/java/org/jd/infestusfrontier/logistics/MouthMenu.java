package org.jd.infestusfrontier.logistics;

import java.util.Optional;
import java.util.function.IntSupplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.foundation.menu.OrganMenu;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The Mouth screen's server half: nine filter slots that hold a copy of what is clicked into them and give nothing
 * back, and the settings as synced ints. The buttons send their ids to {@link #clickMenuButton}; the block entity
 * changes the setting.
 */
public final class MouthMenu extends OrganMenu {
    public static final int MODE_BUTTON = 0;
    public static final int LIST_BUTTON = 1;
    public static final int AMOUNT_UP = 2;
    public static final int AMOUNT_DOWN = 3;
    public static final int PRIORITY_UP = 4;
    public static final int PRIORITY_DOWN = 5;
    /** The filter is three rows of three wells; the menu puts their slots one pixel inside. */
    public static final int FILTER_COLUMNS = 3;

    public static final int PITCH = 18;
    static final int STRIPS = 2;
    private static final int MODE = 0;
    private static final int DENY = 1;
    private static final int AMOUNT = 2;
    private static final int PRIORITY = 3;
    private static final int CARRIED = 4;
    private static final int SYNCED = 5;

    private final Optional<MouthBlockEntity> mouth;

    /** Server side. */
    MouthMenu(int id, Inventory inventory, MouthBlockEntity mouth) {
        this(id, inventory, reading(reads(mouth)), at(mouth), mouth.filter(), Optional.of(mouth));
    }

    /** Client side. The position arrives as extra data. */
    public MouthMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(
                id,
                inventory,
                new SimpleContainerData(SYNCED),
                at(inventory, extra),
                new SimpleContainer(MouthBlockEntity.FILTER_SLOTS),
                Optional.empty());
    }

    private MouthMenu(
            int id,
            Inventory inventory,
            ContainerData data,
            ContainerLevelAccess access,
            Container filter,
            Optional<MouthBlockEntity> mouth) {
        super(LogisticsModule.MOUTH_MENU.get(), id, access, LogisticsModule.MOUTH.get(), data);
        this.mouth = mouth;
        for (int slot = 0; slot < MouthBlockEntity.FILTER_SLOTS; slot++)
            addSlot(new FilterSlot(
                    filter,
                    slot,
                    Sheet.SLOT_X + (slot % FILTER_COLUMNS) * PITCH,
                    Sheet.SLOT_Y + (slot / FILTER_COLUMNS) * PITCH));
        addPlayer(inventory, STRIPS);
    }

    private static IntSupplier[] reads(MouthBlockEntity mouth) {
        IntSupplier[] reads = new IntSupplier[SYNCED];
        reads[MODE] = () -> mouth.mode().ordinal();
        reads[DENY] = () -> mouth.deny() ? 1 : 0;
        reads[AMOUNT] = mouth::amount;
        reads[PRIORITY] = mouth::priority;
        reads[CARRIED] = mouth::carried;
        return reads;
    }

    /** A filter slot shows a copy; it is never taken from or placed into the normal way. */
    private static final class FilterSlot extends Slot {
        FilterSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    /** A click on a filter slot sets it to one of what the cursor holds, or clears it with an empty cursor. */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < MouthBlockEntity.FILTER_SLOTS) {
            if (mouth.map(MouthBlockEntity::isUnreadable).orElse(false)) return;
            Slot slot = slots.get(slotId);
            if (clickType == ClickType.PICKUP)
                slot.set(getCarried().isEmpty() ? ItemStack.EMPTY : getCarried().copyWithCount(1));
            else if (clickType == ClickType.QUICK_MOVE) slot.set(ItemStack.EMPTY);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof FilterSlot);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (mouth.isEmpty()) return id >= MODE_BUTTON && id <= PRIORITY_DOWN;
        MouthBlockEntity m = mouth.get();
        switch (id) {
            case MODE_BUTTON -> m.cycleMode();
            case LIST_BUTTON -> m.toggleList();
            case AMOUNT_UP, AMOUNT_DOWN -> m.stepAmount(id == AMOUNT_UP);
            case PRIORITY_UP, PRIORITY_DOWN -> m.stepPriority(id == PRIORITY_UP);
            default -> {
                return false;
            }
        }
        return true;
    }

    public MouthMode mode() {
        MouthMode[] all = MouthMode.values();
        return all[Math.clamp(synced(MODE), 0, all.length - 1)];
    }

    public boolean deny() {
        return synced(DENY) == 1;
    }

    public int amount() {
        return synced(AMOUNT);
    }

    public int priority() {
        return synced(PRIORITY);
    }

    /** Items on the way from this Mouth. */
    public int carried() {
        return synced(CARRIED);
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
