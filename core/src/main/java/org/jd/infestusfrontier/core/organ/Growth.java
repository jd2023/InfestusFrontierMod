package org.jd.infestusfrontier.core.organ;

import java.util.List;

/**
 * How an organ grows with the batches it completes: stage 0 until the first threshold, one stage more at each
 * threshold after it. An organ that does not grow has no thresholds and one stage.
 */
public record Growth(List<Integer> thresholds) {
    public static final Growth NONE = new Growth(List.of());

    public Growth {
        thresholds = List.copyOf(thresholds);
        int last = 0;
        for (int threshold : thresholds) {
            if (threshold <= last) throw new IllegalArgumentException("Thresholds must rise: " + thresholds);
            last = threshold;
        }
    }

    public static Growth at(int... thresholds) {
        return new Growth(java.util.Arrays.stream(thresholds).boxed().toList());
    }

    /** How many stages there are, the first one included. */
    public int stages() {
        return thresholds.size() + 1;
    }

    /** The stage an organ with {@code history} is at, from 0. */
    public int stage(OrganHistory history) {
        int stage = 0;
        for (int threshold : thresholds) if (history.completedBatches() >= threshold) stage++;
        return stage;
    }

    /** Batches still to complete before the next stage, or 0 at the last stage. */
    public int toNext(OrganHistory history) {
        int stage = stage(history);
        return stage == thresholds.size() ? 0 : thresholds.get(stage) - history.completedBatches();
    }
}
