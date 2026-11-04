package org.jd.infestusfrontier.processing;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.jd.infestusfrontier.processing.api.PalmContents;

/**
 * Which items in a Palm's grid its Mouth fetched from the Item Vein network: per cell, a stack of the kind with the
 * count that came from the network. A craft or a cycle that spends one of them is a craft from storage. The count is
 * kept to what the cell still holds of that kind, so an item taken out by hand, or a cell given another kind, forgets
 * it. The network's items are spent before the hand's, so a restock that tops a cell up counts for the next craft.
 */
final class PalmFetched {
    private final NonNullList<ItemStack> cells = NonNullList.withSize(PalmContents.CELLS, ItemStack.EMPTY);

    /** Forgets what a cell no longer holds: another kind, or fewer items than were fetched. */
    void reconcile(List<ItemStack> grid) {
        for (int cell = 0; cell < PalmContents.CELLS; cell++) {
            ItemStack fetched = cells.get(cell);
            if (fetched.isEmpty()) continue;
            ItemStack held = grid.get(cell);
            if (!ItemStack.isSameItemSameComponents(fetched, held)) cells.set(cell, ItemStack.EMPTY);
            else if (fetched.getCount() > held.getCount()) fetched.setCount(held.getCount());
        }
    }

    /** {@code amount} items of the kind in {@code cell} just came from the network; the grid already holds them. */
    void received(List<ItemStack> grid, int cell, int amount) {
        reconcile(grid);
        ItemStack held = grid.get(cell);
        if (amount <= 0 || held.isEmpty()) return;
        int before = ItemStack.isSameItemSameComponents(cells.get(cell), held)
                ? cells.get(cell).getCount()
                : 0;
        cells.set(cell, held.copyWithCount(Math.min(held.getCount(), before + amount)));
    }

    /** Spends one fetched item of {@code cell}, as a craft takes one of each filled cell. True when it had one. */
    boolean spend(List<ItemStack> grid, int cell) {
        reconcile(grid);
        ItemStack fetched = cells.get(cell);
        if (fetched.isEmpty()) return false;
        fetched.shrink(1);
        if (fetched.isEmpty()) cells.set(cell, ItemStack.EMPTY);
        return true;
    }

    /** Spends one fetched item of every filled cell; true when any cell had one. */
    boolean spendAll(List<ItemStack> grid) {
        boolean any = false;
        for (int cell = 0; cell < PalmContents.CELLS; cell++)
            if (!grid.get(cell).isEmpty() && spend(grid, cell)) any = true;
        return any;
    }

    int of(int cell) {
        return cells.get(cell).getCount();
    }

    /** The saved form, or empty when nothing was fetched. */
    Optional<List<ItemStack>> saved() {
        return cells.stream().allMatch(ItemStack::isEmpty)
                ? Optional.empty()
                : Optional.of(cells.stream().map(ItemStack::copy).toList());
    }

    void load(Optional<List<ItemStack>> saved, List<ItemStack> grid) {
        List<ItemStack> list = saved.orElse(List.of());
        for (int cell = 0; cell < PalmContents.CELLS; cell++)
            cells.set(cell, cell < list.size() ? list.get(cell).copy() : ItemStack.EMPTY);
        reconcile(grid);
    }
}
