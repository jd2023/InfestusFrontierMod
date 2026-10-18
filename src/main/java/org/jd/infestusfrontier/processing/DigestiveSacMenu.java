package org.jd.infestusfrontier.processing;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jd.infestusfrontier.foundation.screen.BiomassView;
import org.jd.infestusfrontier.ui.Sheet;

/**
 * The Sac screen's server half: the feed slot, backed by the block entity's own slot so items stay with the block,
 * and three synced ints: percent done, biomass stored and capacity. Nothing here starts a batch; the block
 * entity takes from the slot on its own tick.
 */
public final class DigestiveSacMenu extends AbstractContainerMenu implements BiomassView {
    static final int FEED_SLOT = 0;
    private static final int PERCENT = 0;
    private static final int STORED = 1;
    private static final int CAPACITY = 2;
    private static final int INVENTORY_START = 1;
    private static final int HOTBAR_START = INVENTORY_START + 27;
    private static final int SLOT_END = HOTBAR_START + 9;

    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Server side. */
    DigestiveSacMenu(int id, Inventory inventory, DigestiveSacBlockEntity sac) {
        this(
                id,
                inventory,
                sac.feed(),
                new SacData(sac),
                ContainerLevelAccess.create(sac.getLevel(), sac.getBlockPos()));
    }

    /** Client side. The position arrives as extra data. */
    public DigestiveSacMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extra) {
        this(
                id,
                inventory,
                new ItemStackHandler(1),
                new SimpleContainerData(3),
                ContainerLevelAccess.create(inventory.player.level(), extra.readBlockPos()));
    }

    private DigestiveSacMenu(
            int id, Inventory inventory, IItemHandler feed, ContainerData data, ContainerLevelAccess access) {
        super(ProcessingModule.DIGESTIVE_SAC_MENU.get(), id);
        this.data = data;
        this.access = access;
        addSlot(new SlotItemHandler(feed, 0, Sheet.SLOT_X, Sheet.SLOT_Y));
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public int percent() {
        return Math.clamp(data.get(PERCENT), 0, 100);
    }

    @Override
    public int stored() {
        return data.get(STORED);
    }

    @Override
    public int capacity() {
        return Math.max(1, data.get(CAPACITY));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        boolean moved;
        if (index == FEED_SLOT) moved = moveItemStackTo(stack, INVENTORY_START, SLOT_END, true);
        else if (slots.get(FEED_SLOT).mayPlace(stack)) moved = moveItemStackTo(stack, FEED_SLOT, FEED_SLOT + 1, false);
        else if (index < HOTBAR_START) moved = moveItemStackTo(stack, HOTBAR_START, SLOT_END, false);
        else moved = moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ProcessingModule.DIGESTIVE_SAC.get());
    }

    /** Reads the live Sac on the server; vanilla syncs changed ints to the client. */
    private static final class SacData implements ContainerData {
        private final DigestiveSacBlockEntity sac;

        SacData(DigestiveSacBlockEntity sac) {
            this.sac = sac;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case PERCENT -> sac.percent();
                case STORED -> sac.tank().stored().units();
                default -> sac.tank().capacity().units();
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 3;
        }
    }

    static boolean isSac(BlockEntity entity) {
        return entity instanceof DigestiveSacBlockEntity;
    }
}
