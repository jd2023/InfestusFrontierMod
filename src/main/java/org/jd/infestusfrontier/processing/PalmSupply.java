package org.jd.infestusfrontier.processing;

import java.util.ArrayList;
import java.util.List;
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
        ItemStack[] seen = snapshot(store);
        int moved = 0;
        for (ItemStack cell : grid) {
            if (cell.isEmpty()) continue;
            int used = draw(store, seen, cell, cell.getMaxStackSize() - cell.getCount(), spill);
            cell.grow(used);
            moved += used;
        }
        return moved;
    }

    /**
     * Restock for a cycle: brings every cell the pattern fills towards a full stack of its kind from the store, an empty
     * cell included. What the store has of a kind is shared out level by level ({@link #shares}), so no cell is
     * starved by the one before it. Same bounds and same honesty as {@link #restock}: each extraction asks the handler
     * again, so a reserve or a competing taker is simply not overridden. Returns how many items moved.
     */
    static int restockPattern(
            NonNullList<ItemStack> grid, List<ItemStack> kinds, IItemHandler store, Consumer<ItemStack> spill) {
        ItemStack[] seen = snapshot(store);
        int[] shares = shares(grid, kinds, seen);
        int moved = 0;
        for (int cell = 0; cell < shares.length; cell++) {
            if (shares[cell] <= 0) continue;
            ItemStack kind = kinds.get(cell);
            int used = draw(store, seen, kind, shares[cell], spill);
            if (used > 0) grid.set(cell, kind.copyWithCount(grid.get(cell).getCount() + used));
            moved += used;
        }
        return moved;
    }

    /** A copy of what the store shows in its first {@link ItemStores#STORE_SLOTS} slots. */
    private static ItemStack[] snapshot(IItemHandler store) {
        ItemStack[] seen = new ItemStack[Math.min(store.getSlots(), ItemStores.STORE_SLOTS)];
        for (int slot = 0; slot < seen.length; slot++)
            seen[slot] = store.getStackInSlot(slot).copy();
        return seen;
    }

    /**
     * Takes up to {@code want} items of {@code kind} from the store slots {@code seen} showed, asking the handler for
     * each. What it gave and is not of that kind or is more than wanted goes back; what cannot go back goes to
     * {@code spill}. {@code seen} is reduced by what was given. Returns how many items of the kind were used.
     */
    private static int draw(IItemHandler store, ItemStack[] seen, ItemStack kind, int want, Consumer<ItemStack> spill) {
        int used = 0;
        for (int slot = 0; slot < seen.length && used < want; slot++) {
            if (!ItemStack.isSameItemSameComponents(seen[slot], kind)) continue;
            int ask = Math.min(want - used, seen[slot].getCount());
            ItemStack got = store.extractItem(slot, ask, false);
            if (got.isEmpty()) continue;
            seen[slot].shrink(got.getCount());
            int taken = ItemStack.isSameItemSameComponents(got, kind) ? Math.min(got.getCount(), ask) : 0;
            used += taken;
            if (taken < got.getCount()) giveBack(store, got.copyWithCount(got.getCount() - taken), spill);
        }
        return used;
    }

    /**
     * How many items each grid cell is to get of the stock {@code seen} shows. The cells that the pattern gives one kind
     * (and that are empty or hold it) form a group; the group's stock is poured in from the lowest cell up, level by
     * level, so the cells end within one item of each other where the stock allows, none passes a full stack, and the
     * shares never sum to more than the stock. A cell holding another item gets nothing.
     */
    private static int[] shares(NonNullList<ItemStack> grid, List<ItemStack> kinds, ItemStack[] seen) {
        int[] shares = new int[grid.size()];
        boolean[] grouped = new boolean[grid.size()];
        for (int first = 0; first < shares.length; first++) {
            ItemStack kind = kinds.get(first);
            if (kind.isEmpty() || grouped[first]) continue;
            List<Integer> group = new ArrayList<>();
            for (int cell = first; cell < shares.length; cell++) {
                if (!ItemStack.isSameItemSameComponents(kinds.get(cell), kind)) continue;
                grouped[cell] = true;
                ItemStack held = grid.get(cell);
                if (held.isEmpty() || ItemStack.isSameItemSameComponents(held, kind)) group.add(cell);
            }
            int available = 0;
            for (ItemStack shown : seen)
                if (ItemStack.isSameItemSameComponents(shown, kind)) available += shown.getCount();
            pour(grid, group, kind.getMaxStackSize(), available, shares);
        }
        return shares;
    }

    /** Pours {@code available} items into the {@code group} cells, lowest first, up to {@code max} each. */
    private static void pour(NonNullList<ItemStack> grid, List<Integer> group, int max, int available, int[] shares) {
        while (available > 0) {
            int level = max;
            for (int cell : group) level = Math.min(level, grid.get(cell).getCount() + shares[cell]);
            List<Integer> lowest = new ArrayList<>();
            int next = max;
            for (int cell : group) {
                int have = grid.get(cell).getCount() + shares[cell];
                if (have == level && have < max) lowest.add(cell);
                else if (have > level) next = Math.min(next, have);
            }
            if (lowest.isEmpty()) return;
            int gap = next - level;
            if (available >= gap * lowest.size()) {
                for (int cell : lowest) shares[cell] += gap;
                available -= gap * lowest.size();
            } else {
                int each = available / lowest.size();
                int extra = available % lowest.size();
                for (int n = 0; n < lowest.size(); n++) shares[lowest.get(n)] += each + (n < extra ? 1 : 0);
                available = 0;
            }
        }
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
