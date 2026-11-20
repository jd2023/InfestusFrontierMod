package org.jd.infestusfrontier.core.mining;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * The rules of a Descending Rootstock's minehead and the reasons it pauses. A minehead is a Tunnel Root head with the
 * Stairs mutation, facing a Surveyed Tissue marker with a Structure Grower on it, with a Fracture Jaw behind it whose
 * Work Bed Collection Cilia sweep. The Rootstock orders: it names the next block of the next step of the approved
 * staircase, top down, and only of that step, so the root never goes past the last built tread. The jaw cuts it, the
 * cilia export it and the grower builds the step once it is clear; the Rootstock counts each step built as its own
 * advance. It cuts, carries and builds nothing itself. It does not judge the ground: water, lava, falling blocks and
 * cavities are accidents the mine cuts into, not reasons to wait. Once the landing is built it orders its gallery faces
 * the same way, see {@link Galleries}. Every number is a starting value, not measured balance.
 */
public final class Rootstocking {
    /** Loaded ticks between two looks of a minehead at its parts and its next step, for its screen. */
    public static final int CHECK_TICKS = 20;

    /** The marker in front faces another way than the Rootstock. */
    public static final Refusal MARKER_TURNED = new Refusal("rootstock.marker_turned");
    /** The marker in front has no readable plan. */
    public static final Refusal NO_PLAN = new Refusal("rootstock.no_plan");
    /** The marker's plan is still a draft. */
    public static final Refusal NOT_APPROVED = new Refusal("rootstock.not_approved");
    /** The marker's plan is a level route: a Rootstock cuts staircases. */
    public static final Refusal NOT_STAIRCASE = new Refusal("rootstock.not_staircase");
    /** Every step of the section is built: the landing is reached. */
    public static final Refusal FINISHED = new Refusal("rootstock.finished");
    /** No readable Structure Grower stands on the marker. */
    public static final Refusal NO_GROWER = new Refusal("rootstock.no_grower");
    /** No readable Fracture Jaw stands behind the Rootstock facing it. */
    public static final Refusal NO_JAW = new Refusal("rootstock.no_jaw");
    /** No readable Work Bed stands behind the jaw. */
    public static final Refusal NO_BED = new Refusal("rootstock.no_bed");
    /** No readable Collection Cilia face the Work Bed. */
    public static final Refusal NO_CILIA = new Refusal("rootstock.no_cilia");
    /** A chunk the minehead or the next step reads is not loaded. */
    public static final Refusal UNLOADED = new Refusal("rootstock.unloaded");

    /** The minehead's own stops, in a fixed order; a screen adds the parts' stops after them. */
    public static final List<Refusal> STOPS = List.of(
            MARKER_TURNED,
            NO_PLAN,
            NOT_APPROVED,
            NOT_STAIRCASE,
            FINISHED,
            NO_GROWER,
            NO_JAW,
            NO_BED,
            NO_CILIA,
            UNLOADED);

    private Rootstocking() {}

    /** The blocks of {@code step} in the order the jaw cuts them: top down, so nothing above a cut is left hanging. */
    public static List<Offset> cutOrder(PlanStep step) {
        List<Offset> cells = new ArrayList<>(step.cleared());
        cells.sort(Comparator.comparingInt(Offset::up).reversed());
        return List.copyOf(cells);
    }

    /**
     * The one reason a minehead shows: its own stop first; then, while its step still has blocks to cut, the jaw's,
     * except that a full bed the cilia cannot empty shows the cilia's reason; once the step is clear, the grower's,
     * except that the step is not clear; then the cilia's. Empty while every organ works. A jaw that waits for an order
     * and a grower that waits for the cut are not stops.
     */
    public static Optional<Refusal> shown(
            Optional<Refusal> own,
            boolean cutting,
            Optional<Refusal> jaw,
            Optional<Refusal> grower,
            Optional<Refusal> cilia) {
        if (own.isPresent()) return own;
        if (cutting && jaw.isPresent() && !jaw.get().equals(Fracturing.NO_ORDER))
            return jaw.get().equals(Fracturing.BED_FULL) && cilia.isPresent() ? cilia : jaw;
        if (!cutting && grower.isPresent() && !grower.get().equals(Surveying.NOT_CLEAR)) return grower;
        return cilia;
    }
}
