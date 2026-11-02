package org.jd.infestusfrontier.processing;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.processing.api.PalmLinking;
import org.jd.infestusfrontier.storage.api.ItemStores;

/**
 * What a Palm may do with its one supply link (BLOCK_CATALOG T1-41, stage 2). The store is the block's own item
 * handler, found at the linked position and nowhere else; a Larder's reserves and a pod's rules are enforced by that
 * handler, so nothing here counts, caches or keeps stock. Each action reads at most {@link ItemStores#STORE_SLOTS} store slots
 * and touches at most nine grid cells; each moves only what the store or the grid really gave, so a refused or
 * partial action loses and duplicates nothing.
 */
final class PalmSupply {
    static final Refusal NO_REACH = new Refusal("processing.palm_no_reach");
    static final Refusal NOT_LINKED = new Refusal("processing.palm_not_linked");
    static final Refusal OUT_OF_RANGE = new Refusal("processing.palm_link_range");
    static final Refusal UNLOADED = new Refusal("processing.palm_link_unloaded");
    static final Refusal NO_STORE = new Refusal("processing.palm_link_no_store");
    static final Refusal GRID_EMPTY = new Refusal("processing.palm_grid_empty");
    static final Refusal GRID_FULL = new Refusal("processing.palm_grid_full");
    static final Refusal STORE_SHORT = new Refusal("processing.palm_store_short");
    static final Refusal STORE_FULL = new Refusal("processing.palm_store_full");
    static final Refusal NO_RESULT = new Refusal("processing.palm_no_result");

    private PalmSupply() {}

    /** A store the link reaches now, or the reason it does not. */
    sealed interface Reached {
        record Store(IItemHandler handler) implements Reached {}

        record Refused(Refusal why) implements Reached {}
    }

    /**
     * The item handler at {@code target} for a Palm at {@code palm}. A position past {@link PalmLinking#RANGE}, in a
     * chunk that is not loaded, or without an item handler is refused; no chunk is loaded and nothing is searched.
     */
    static Reached reach(Level level, BlockPos palm, BlockPos target) {
        if (target.equals(palm)) return new Reached.Refused(NO_STORE);
        if (palm.distSqr(target) > PalmLinking.RANGE * PalmLinking.RANGE) return new Reached.Refused(OUT_OF_RANGE);
        IItemHandler handler = ItemStores.at(level, target, null);
        if (handler != null) return new Reached.Store(handler);
        return new Reached.Refused(level.hasChunkAt(target) ? NO_STORE : UNLOADED);
    }

    /**
     * Restock: tops every non-empty grid cell up to a full stack with the same item and components from the store.
     * Returns how many items moved; 0 changes nothing. Each real extraction asks the handler again, so stock a reserve
     * or a competing taker has claimed since the store was read is simply not given. An over-give or a kind that does
     * not match what was asked goes back; what cannot go back goes to {@code spill}.
     */
    static int restock(NonNullList<ItemStack> grid, IItemHandler store, Consumer<ItemStack> spill) {
        int slots = Math.min(store.getSlots(), ItemStores.STORE_SLOTS);
        ItemStack[] seen = new ItemStack[slots];
        for (int slot = 0; slot < slots; slot++)
            seen[slot] = store.getStackInSlot(slot).copy();
        int moved = 0;
        for (ItemStack cell : grid) {
            if (cell.isEmpty()) continue;
            for (int slot = 0; slot < slots && cell.getCount() < cell.getMaxStackSize(); slot++) {
                if (seen[slot].isEmpty() || !ItemStack.isSameItemSameComponents(seen[slot], cell)) continue;
                int want = Math.min(cell.getMaxStackSize() - cell.getCount(), seen[slot].getCount());
                ItemStack got = store.extractItem(slot, want, false);
                if (got.isEmpty()) continue;
                seen[slot].shrink(got.getCount());
                int used = ItemStack.isSameItemSameComponents(got, cell) ? Math.min(got.getCount(), want) : 0;
                cell.grow(used);
                moved += used;
                if (used < got.getCount()) giveBack(store, got.copyWithCount(got.getCount() - used), spill);
            }
        }
        return moved;
    }

    /**
     * Return: moves each non-empty grid cell into the store as far as the store takes it. What does not fit stays in
     * its cell. Returns how many items moved.
     */
    static int giveAll(NonNullList<ItemStack> grid, IItemHandler store) {
        int moved = 0;
        for (int cell = 0; cell < grid.size(); cell++) {
            ItemStack stack = grid.get(cell);
            if (stack.isEmpty()) continue;
            ItemStack rest = ItemStores.insert(store, stack, false);
            moved += stack.getCount() - rest.getCount();
            grid.set(cell, rest);
        }
        return moved;
    }

    /** Returns {@code stack}, taken from the store and not used, to the store; what does not fit goes to {@code spill}. */
    private static void giveBack(IItemHandler store, ItemStack stack, Consumer<ItemStack> spill) {
        ItemStack rest = ItemStores.insert(store, stack, false);
        if (!rest.isEmpty()) spill.accept(rest);
    }

    /** Why the grid cannot be restocked, given that {@code moved} items moved. Only for {@code moved == 0}. */
    static Refusal restockRefusal(NonNullList<ItemStack> grid) {
        Optional<ItemStack> open = grid.stream()
                .filter(cell -> !cell.isEmpty() && cell.getCount() < cell.getMaxStackSize())
                .findAny();
        if (open.isPresent()) return STORE_SHORT;
        return grid.stream().allMatch(ItemStack::isEmpty) ? GRID_EMPTY : GRID_FULL;
    }
}
