package org.jd.infestusfrontier.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.jd.infestusfrontier.logistics.api.ItemAccess;
import org.jd.infestusfrontier.logistics.api.LogisticsCapabilities;
import org.jd.infestusfrontier.processing.api.PalmContents;

/**
 * What a Palm may do with the Mouth under it (BLOCK_CATALOG T1-41, stage 2). The Palm keeps no store, link or
 * position: the Mouth under it is its one door to the Item Vein network, and logistics decides what that door gives and
 * takes ({@link ItemAccess}). Each action is one transaction of at most nine requests. It moves only what the
 * network really gave or took, so a refused or partial action loses and duplicates nothing.
 */
final class PalmSupply {
    static final Refusal NO_REACH = new Refusal("processing.palm_no_reach");
    static final Refusal NO_MOUTH = new Refusal("processing.palm_no_mouth");
    static final Refusal GRID_EMPTY = new Refusal("processing.palm_grid_empty");
    static final Refusal GRID_FULL = new Refusal("processing.palm_grid_full");
    static final Refusal STORE_SHORT = new Refusal("processing.palm_store_short");
    static final Refusal STORE_FULL = new Refusal("processing.palm_store_full");
    static final Refusal NO_RESULT = new Refusal("processing.palm_no_result");

    private PalmSupply() {}

    /** The door the Palm has now, or the reason it has none. */
    sealed interface Reached {
        record Door(ItemAccess access) implements Reached {}

        record Refused(Refusal why) implements Reached {}
    }

    /**
     * What an action did: how many items moved, whether the network was consulted, and why it was not or gave nothing.
     * {@code fetched} is what each grid cell received, for a restock; none for any other action.
     */
    record Moved(int items, boolean consulted, Optional<Refusal> why, int[] fetched) {
        Moved(int items, boolean consulted, Optional<Refusal> why) {
            this(items, consulted, why, new int[PalmContents.CELLS]);
        }
    }

    /** The Mouth under {@code palm}, if it is there and loaded. Nothing is searched and no chunk is loaded. */
    static Reached reach(Level level, BlockPos palm) {
        BlockPos below = palm.below();
        if (!level.hasChunkAt(below)) return new Reached.Refused(NO_MOUTH);
        ItemAccess access = level.getCapability(LogisticsCapabilities.ITEM_ACCESS, below, null);
        return access == null ? new Reached.Refused(NO_MOUTH) : new Reached.Door(access);
    }

    /** The cells of one kind: those that are empty or hold it, and what the kind wants to fill them to a stack. */
    private record Group(ItemStack kind, List<Integer> cells, int room, int held) {}

    /**
     * Restock: tops every non-empty grid cell up to a full stack with the same item and components from the network,
     * and never fills an empty cell. Returns what moved.
     */
    static Moved restock(NonNullList<ItemStack> grid, ItemAccess door) {
        List<ItemStack> kinds = new ArrayList<>();
        for (ItemStack cell : grid) kinds.add(cell.isEmpty() ? ItemStack.EMPTY : cell.copyWithCount(1));
        return restockPattern(grid, kinds, door);
    }

    /**
     * Restock for a cycle: brings every cell the pattern fills towards a full stack of its kind, an empty cell
     * included. What the network gives of a kind is shared out level by level ({@link #pour}), so no cell is starved by
     * the one before it and none passes a full stack.
     */
    static Moved restockPattern(NonNullList<ItemStack> grid, List<ItemStack> kinds, ItemAccess door) {
        List<Group> groups = groups(grid, kinds);
        if (groups.isEmpty()) return new Moved(0, true, Optional.empty());
        ItemAccess.Drawn drawn = door.pull(groups.stream()
                .map(g -> new ItemAccess.Want(g.kind(), g.room(), g.held()))
                .toList());
        int moved = 0;
        int[] fetched = new int[grid.size()];
        for (int n = 0; n < groups.size(); n++) {
            Group group = groups.get(n);
            int[] shares = new int[grid.size()];
            pour(
                    grid,
                    group.cells(),
                    group.kind().getMaxStackSize(),
                    drawn.stacks().get(n).getCount(),
                    shares);
            for (int cell : group.cells()) {
                if (shares[cell] <= 0) continue;
                grid.set(cell, group.kind().copyWithCount(grid.get(cell).getCount() + shares[cell]));
                moved += shares[cell];
                fetched[cell] += shares[cell];
            }
        }
        return new Moved(moved, drawn.consulted(), drawn.why(), fetched);
    }

    /** The groups the pattern's kinds make: one per kind, in the order its first cell comes. */
    private static List<Group> groups(NonNullList<ItemStack> grid, List<ItemStack> kinds) {
        List<Group> groups = new ArrayList<>();
        boolean[] grouped = new boolean[grid.size()];
        for (int first = 0; first < grid.size(); first++) {
            ItemStack kind = kinds.get(first);
            if (kind.isEmpty() || grouped[first]) continue;
            List<Integer> cells = new ArrayList<>();
            int room = 0;
            int held = 0;
            for (int cell = first; cell < grid.size(); cell++) {
                if (!ItemStack.isSameItemSameComponents(kinds.get(cell), kind)) continue;
                grouped[cell] = true;
                ItemStack in = grid.get(cell);
                if (!in.isEmpty() && !ItemStack.isSameItemSameComponents(in, kind)) continue;
                cells.add(cell);
                room += kind.getMaxStackSize() - in.getCount();
                held += in.getCount();
            }
            if (room > 0) groups.add(new Group(kind.copyWithCount(1), cells, room, held));
        }
        return groups;
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
     * Return: sends each non-empty grid cell to the network as far as it takes it. What no Mouth takes stays in its
     * cell. Returns what moved.
     */
    static Moved giveAll(NonNullList<ItemStack> grid, ItemAccess door) {
        List<Integer> cells = new ArrayList<>();
        List<ItemAccess.Offer> offers = new ArrayList<>();
        for (int cell = 0; cell < grid.size(); cell++) {
            ItemStack stack = grid.get(cell);
            if (stack.isEmpty()) continue;
            int held = 0;
            for (ItemStack other : grid) if (ItemStack.isSameItemSameComponents(other, stack)) held += other.getCount();
            cells.add(cell);
            offers.add(new ItemAccess.Offer(stack, held));
        }
        ItemAccess.Sent sent = door.push(offers, false);
        int moved = 0;
        for (int n = 0; n < cells.size(); n++) {
            ItemStack rest = sent.rests().get(n);
            moved += offers.get(n).stack().getCount() - rest.getCount();
            grid.set(cells.get(n), rest);
        }
        return new Moved(moved, sent.consulted(), sent.why());
    }

    /** Why the grid cannot be restocked, given that nothing moved. */
    static Refusal restockRefusal(NonNullList<ItemStack> grid) {
        Optional<ItemStack> open = grid.stream()
                .filter(cell -> !cell.isEmpty() && cell.getCount() < cell.getMaxStackSize())
                .findAny();
        if (open.isPresent()) return STORE_SHORT;
        return grid.stream().allMatch(ItemStack::isEmpty) ? GRID_EMPTY : GRID_FULL;
    }
}
