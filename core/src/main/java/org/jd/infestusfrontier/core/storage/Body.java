package org.jd.infestusfrontier.core.storage;

import java.util.Optional;

/**
 * The rules of a storage body (BLOCK_CATALOG T1-07 and T1-09): touching cells form one body only as a full
 * rectangular box, grown by whole rows and layers; a cell that would leave a gap waits outside on its own. Contents
 * live in the cells; a body fills its bottom layer first, evenly across it, and drains its top layer first, so every
 * cell shows the one level.
 */
public final class Body {
    /** The longest side a body may have. A starting value. */
    public static final int MAX_SIDE = 4;

    private Body() {}

    /** What a body knows of the cell at a position: absent, or the box of the body it belongs to. */
    @FunctionalInterface
    public interface Cells {
        Optional<Box> at(int x, int y, int z);
    }

    /**
     * The box {@code body} and {@code joining} form together, when it is full: every position inside holds a cell,
     * and every such cell's own box lies inside it. Empty when the union would leave a gap, take part of another
     * body, or grow past {@code maxSide}. Reads the union's volume, at most {@code maxSide}³ cells.
     */
    public static Optional<Box> grow(Box body, Box joining, Cells cells, int maxSide) {
        Box union = body.union(joining);
        if (!union.fits(maxSide)) return Optional.empty();
        for (int x = union.minX(); x <= union.maxX(); x++)
            for (int y = union.minY(); y <= union.maxY(); y++)
                for (int z = union.minZ(); z <= union.maxZ(); z++) {
                    Optional<Box> cell = cells.at(x, y, z);
                    if (cell.isEmpty() || !union.contains(cell.get())) return Optional.empty();
                }
        return Optional.of(union);
    }

    /**
     * Puts up to {@code amount} into {@code stored} (layers bottom first, cells of one layer side by side), each cell
     * holding at most {@code capacity}: the lowest layer with room first, and within it the emptiest cells first, so
     * the cells of a layer never differ by more than one. Returns what went in; changes {@code stored}.
     */
    public static int fill(int[][] stored, int capacity, int amount) {
        int left = amount;
        for (int l = 0; l < stored.length && left > 0; l++) left -= level(stored[l], capacity, left, true);
        return amount - left;
    }

    /**
     * Takes up to {@code amount} from {@code stored}: the highest layer holding anything first, and within it the
     * fullest cells first. Returns what came out; changes {@code stored}.
     */
    public static int drain(int[][] stored, int capacity, int amount) {
        int left = amount;
        for (int l = stored.length - 1; l >= 0 && left > 0; l--) left -= level(stored[l], capacity, left, false);
        return amount - left;
    }

    /**
     * Moves up to {@code amount} into ({@code up}) or out of one layer, a step at a time: the cells at the lowest
     * (highest) value move together to the next value; a step that does not fit is split evenly, one unit more to the
     * first cells. At most one step per distinct value, so a layer of n cells takes at most n steps.
     */
    private static int level(int[] layer, int capacity, int amount, boolean up) {
        int moved = 0;
        while (moved < amount) {
            int edge = up ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            for (int cell : layer)
                if (up ? cell < capacity : cell > 0) edge = up ? Math.min(edge, cell) : Math.max(edge, cell);
            if (edge == Integer.MAX_VALUE || edge == Integer.MIN_VALUE) break;
            int next = up ? capacity : 0;
            int count = 0;
            for (int cell : layer) {
                if (cell == edge) count++;
                else if (up ? cell > edge && cell < next : cell < edge && cell > next) next = cell;
            }
            int step = Math.abs(next - edge);
            int move = (int) Math.min((long) step * count, amount - moved);
            int each = move / count;
            int extra = move % count;
            for (int i = 0; i < layer.length; i++) {
                if (layer[i] != edge) continue;
                int delta = each + (extra-- > 0 ? 1 : 0);
                layer[i] += up ? delta : -delta;
            }
            moved += move;
        }
        return moved;
    }
}
