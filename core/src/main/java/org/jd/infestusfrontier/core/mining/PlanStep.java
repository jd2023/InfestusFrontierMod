package org.jd.infestusfrontier.core.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One step of a plan, numbered from 1: the root cell in its floor, the root cells beside it on a wide route, the tread
 * on the root cell on a staircase, the clear passage above, and the light when this step gets one. {@code arrived}
 * and {@code leaving} are the headings into and out of the cell, in quarter turns clockwise from the marker's forward;
 * they differ where a route turns.
 */
public record PlanStep(
        int number,
        Offset root,
        List<Offset> flanks,
        Optional<Offset> tread,
        List<Offset> passage,
        Optional<Offset> light,
        int arrived,
        int leaving) {
    public PlanStep {
        flanks = List.copyOf(flanks);
        passage = List.copyOf(passage);
    }

    /** True where the route turns: the root cell bends. */
    public boolean bend() {
        return arrived != leaving;
    }

    /** Every root cell of the step, the middle one first: each takes one Root Segment. */
    public List<Offset> roots() {
        List<Offset> roots = new ArrayList<>(1 + flanks.size());
        roots.add(root);
        roots.addAll(flanks);
        return List.copyOf(roots);
    }

    /**
     * Every block of the step that must be clear before anything is built in it: the root cells, the tread and the
     * passage. The light goes in the passage.
     */
    public List<Offset> cleared() {
        List<Offset> cells = new ArrayList<>(roots());
        tread.ifPresent(cells::add);
        cells.addAll(passage);
        return List.copyOf(cells);
    }
}
