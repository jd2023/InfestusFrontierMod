package org.jd.infestusfrontier.storage;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Bladder screen's server half: one service slot for a piece of living equipment, the Fill button, and the
 * balance as two synced ints. Nothing here decides a transfer; the block entity does.
 */
public final class BiomassBladderMenu extends AbstractContainerMenu {
    public static final int FILL_BUTTON = 0;
    static final int SERVICE_SLOT = 0;
    private static final int STORED = 0;
    private static final int CAPACITY = 1;
    private static final int INVENTORY_START = 1;
    private static final int HOTBAR_START = INVENTORY_START + 27;
    private static final int SLOT_END = HOTBAR_START + 9;

    private final Container service = new SimpleContainer(1);
    private final ContainerData balance;
    private final ContainerLevelAccess access;

    /** Server side. */
    BiomassBladderMenu(int id, Inventory inventory, BiomassBladderBlockEntity bladder) {
        this(
                id,
                inventory,
                new BalanceData(bladder),
                ContainerLevelAccess.create(bladder.getLevel(), bladder.getBlockPos()));
    }

    /** Client side. The position arrives as extra data. */
    public BiomassBladderMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(
                id,
                inventory,
                new SimpleContainerData(2),
                ContainerLevelAccess.create(inventory.player.level(), extra.readBlockPos()));
    }

    private BiomassBladderMenu(int id, Inventory inventory, ContainerData balance, ContainerLevelAccess access) {
        super(StorageModule.BIOMASS_BLADDER_MENU.get(), id);
        this.balance = balance;
        this.access = access;
        addSlot(new Slot(service, 0, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(balance);
    }

    public int stored() {
        return balance.get(STORED);
    }

    public int capacity() {
        return Math.max(1, balance.get(CAPACITY));
    }

    public boolean hasServiceItem() {
        return !service.getItem(0).isEmpty();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        boolean moved;
        if (index == SERVICE_SLOT) moved = moveItemStackTo(stack, INVENTORY_START, SLOT_END, true);
        else if (slots.get(SERVICE_SLOT).mayPlace(stack))
            moved = moveItemStackTo(stack, SERVICE_SLOT, SERVICE_SLOT + 1, false);
        else if (index < HOTBAR_START) moved = moveItemStackTo(stack, HOTBAR_START, SLOT_END, false);
        else moved = moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return before;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, service));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, StorageModule.BIOMASS_BLADDER.get());
    }

    /** Reads the live balance on the server; vanilla syncs changed ints to the client. */
    private static final class BalanceData implements ContainerData {
        private final BiomassBladderBlockEntity bladder;

        BalanceData(BiomassBladderBlockEntity bladder) {
            this.bladder = bladder;
        }

        @Override
        public int get(int index) {
            return index == STORED
                    ? bladder.tank().stored().units()
                    : bladder.tank().capacity().units();
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 2;
        }
    }

    static boolean isBladder(BlockEntity entity) {
        return entity instanceof BiomassBladderBlockEntity;
    }
}
