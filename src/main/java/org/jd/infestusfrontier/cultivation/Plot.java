package org.jd.infestusfrontier.cultivation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * The plot of a planter or a harvester: the nine ground cells under and around its own foundation, and the nine
 * positions above them where the crops stand. The organ's own position is never a crop position.
 */
final class Plot {
    static final int SIZE = 9;

    private Plot() {}

    /** The ground cells, the organ's own foundation first. */
    static List<BlockPos> ground(BlockPos organ) {
        List<BlockPos> cells = new ArrayList<>(SIZE);
        BlockPos center = organ.below();
        cells.add(center);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) cells.add(center.offset(dx, 0, dz));
        return cells;
    }
}
