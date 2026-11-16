package org.jd.infestusfrontier.core.mining;

import java.util.ArrayList;
import java.util.List;

/**
 * The fixed shape and limits of a Leaching Pod's work: one horizontal disc 7 blocks across a layer, at most
 * {@value #MAX_LAYERS} layers below the pod, one layer every {@value #LAYER_TICKS} loaded ticks, paid from
 * {@value #MAX_CHARGES} charges. Every number is a starting value, not measured balance.
 */
public final class Leaching {
    /** The disc reaches this many blocks from the pod's column in each horizontal direction. */
    public static final int RADIUS = 3;
    /** Disc cells satisfy {@code dx² + dz² <= RADIUS² + 1}: 37 cells inside a 7-wide square. */
    public static final int MAX_BLOCKS_PER_LAYER = 37;

    public static final int MAX_LAYERS = 16;
    public static final int MAX_CHARGES = 16;
    /** Loaded server ticks between two layers: one second. */
    public static final int LAYER_TICKS = 20;
    /** Hardest ground a pod weakens. Stone is 1.5; deepslate, at 3.0, stays out. */
    public static final float MAX_HARDNESS = 1.5F;

    /** A column offset from the pod. */
    public record Cell(int dx, int dz) {}

    /** The 37 columns of a layer, in a stable order. */
    public static final List<Cell> DISC = buildDisc();

    private Leaching() {}

    private static List<Cell> buildDisc() {
        List<Cell> cells = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++)
            for (int dz = -RADIUS; dz <= RADIUS; dz++)
                if (dx * dx + dz * dz <= RADIUS * RADIUS + 1) cells.add(new Cell(dx, dz));
        return List.copyOf(cells);
    }

    /** True for a block of this hardness; a negative hardness is unbreakable and never qualifies. */
    public static boolean softEnough(float hardness) {
        return hardness >= 0 && hardness <= MAX_HARDNESS;
    }
}
