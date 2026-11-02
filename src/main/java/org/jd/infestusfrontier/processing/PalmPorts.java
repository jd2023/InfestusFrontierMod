package org.jd.infestusfrontier.processing;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.processing.api.PalmContents;
import org.jd.infestusfrontier.processing.api.PalmRote;

/**
 * A Palm with Rote as hoppers, pipes and Lobes see it: the nine grid slots take only the item and components their
 * pattern slot holds, shared out among the cells of one kind, and give nothing back; the four tray slots after them give what a cycle made and take nothing.
 * Every slot answers what it holds. It reads and writes the Palm's own lists, so it keeps no state of its own.
 */
record PalmPorts(ShapingPalmBlockEntity palm) implements IItemHandler {
    private static final int GRID = PalmContents.CELLS;

    @Override
    public int getSlots() {
        return GRID + PalmRote.TRAY;
    }

    private boolean isGrid(int slot) {
        return slot < GRID;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return isGrid(slot) ? palm.grid().get(slot) : palm.tray().get(slot - GRID);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isGrid(slot)) return stack;
        int moved = Math.min(allowance(slot, stack), stack.getCount());
        if (moved <= 0) return stack;
        if (!simulate) {
            ItemStack held = palm.grid().get(slot);
            if (held.isEmpty()) palm.grid().set(slot, stack.copyWithCount(moved));
            else held.grow(moved);
            palm.touch();
        }
        return moved == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
    }

    /**
     * How many of {@code stack} grid cell {@code slot} takes now: none unless its pattern slot holds that kind. When
     * the pattern holds the kind in several cells, a cell takes items only while it holds no more than the others, and
     * then up to the next lowest one (at least one), so a hopper that fills the first slot that accepts still feeds
     * every cell instead of filling the first to a stack.
     */
    private int allowance(int slot, ItemStack stack) {
        if (!palm.takes(slot, stack) || !fits(palm.grid().get(slot), stack)) return 0;
        int held = palm.grid().get(slot).getCount();
        int room = stack.getMaxStackSize() - held;
        int lowestOther = Integer.MAX_VALUE;
        for (int cell = 0; cell < GRID; cell++) {
            ItemStack other = palm.grid().get(cell);
            if (cell == slot || !palm.takes(cell, stack) || !fits(other, stack)) continue;
            if (other.getCount() < stack.getMaxStackSize()) lowestOther = Math.min(lowestOther, other.getCount());
        }
        if (lowestOther == Integer.MAX_VALUE) return Math.max(room, 0);
        if (held > lowestOther) return 0;
        return Math.min(Math.max(room, 0), Math.max(1, lowestOther - held));
    }

    private static boolean fits(ItemStack held, ItemStack stack) {
        return held.isEmpty() || ItemStack.isSameItemSameComponents(held, stack);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (isGrid(slot) || amount <= 0 || !palm.readable()) return ItemStack.EMPTY;
        NonNullList<ItemStack> tray = palm.tray();
        ItemStack held = tray.get(slot - GRID);
        if (held.isEmpty()) return ItemStack.EMPTY;
        int moved = Math.min(amount, held.getCount());
        ItemStack out = held.copyWithCount(moved);
        if (!simulate) {
            held.shrink(moved);
            if (held.isEmpty()) tray.set(slot - GRID, ItemStack.EMPTY);
            palm.touchTray();
        }
        return out;
    }

    @Override
    public int getSlotLimit(int slot) {
        return getStackInSlot(slot).isEmpty() ? 64 : getStackInSlot(slot).getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isGrid(slot) && palm.takes(slot, stack) && fits(palm.grid().get(slot), stack);
    }
}
