package org.jd.infestusfrontier.core.mining;

import java.util.List;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * The rules of a minehead's galleries: level routes cut from the landing of its finished staircase. The landing is the
 * minehead's first root junction. A route opens from a junction when its Surveyed Tissue marker lies in the floor
 * right beside the junction's root cell, on one of its three sides other than the way the root arrived, and faces away
 * from it; so a junction joins at most {@value #ROUTES_PER_JUNCTION} routes. The last root cell of a finished route is
 * a junction too. Each unfinished route is a working face with its own Structure Grower on the marker, one to three
 * Fracture Jaws facing the grower from its sides or from behind, and a Work Bed and Collection Cilia behind each jaw.
 * One minehead orders at most {@value #MAX_FACES} faces. A wide route, the Wider Section, needs the Rootstock's
 * mutation and {@value #WIDE_JAWS} jaws on its face.
 *
 * <p>The faces share whatever supply and export lines the player connects to them. The minehead admits their cuts in
 * turn: a face that has just been given a cut waits until each other face that can cut has been given one, so a busy
 * face never holds the others back and a stopped face never holds up the rest. Every number is a starting value, not
 * measured balance.
 */
public final class Galleries {
    /** Routes one root junction joins: its three sides other than the way its root arrived. */
    public static final int ROUTES_PER_JUNCTION = 3;
    /** Unfinished routes, working faces, one minehead orders at once. */
    public static final int MAX_FACES = 4;
    /** Routes, finished or not, one minehead looks at to find its faces; it refuses to work past that. */
    public static final int MAX_ROUTES = 13;
    /** Jaws a face of the Wider Section needs, and the most any face uses. */
    public static final int WIDE_JAWS = 3;

    /** More than {@link #MAX_FACES} faces open from the minehead's junctions. */
    public static final Refusal TOO_MANY_FACES = new Refusal("rootstock.too_many_faces");
    /** More than {@link #MAX_ROUTES} routes open from the minehead's junctions. */
    public static final Refusal TOO_MANY_ROUTES = new Refusal("rootstock.too_many_routes");
    /** A wide face needs a Rootstock with the Wider Section mutation. */
    public static final Refusal NEEDS_WIDER_SECTION = new Refusal("rootstock.needs_wider_section");
    /** A wide face needs {@link #WIDE_JAWS} jaws facing its grower. */
    public static final Refusal NEEDS_JAWS = new Refusal("rootstock.needs_jaws");

    /** The minehead's gallery stops, in a fixed order; a screen adds them after the minehead's own. */
    public static final List<Refusal> STOPS = List.of(TOO_MANY_FACES, TOO_MANY_ROUTES, NEEDS_WIDER_SECTION, NEEDS_JAWS);

    private Galleries() {}

    /**
     * The headings, in quarter turns clockwise, on which routes may open from a junction whose root arrived heading
     * {@code arrived}: left, straight on and right. Never back the way the root came.
     */
    public static List<Integer> sides(int arrived) {
        return List.of(Math.floorMod(arrived + 3, 4), Math.floorMod(arrived, 4), Math.floorMod(arrived + 1, 4));
    }

    /**
     * Whether face {@code asking} of {@code ready.length} faces is given a cut now, when it is {@code turn}'s turn: it
     * is, or no face from {@code turn} on before it can cut ({@code ready} is false for each). The asking face's own
     * readiness is not consulted: it asks because it can cut.
     */
    public static boolean admitted(int turn, int asking, boolean[] ready) {
        int faces = ready.length;
        if (asking < 0 || asking >= faces || turn < 0 || turn >= faces)
            throw new IllegalArgumentException("Face " + asking + ", turn " + turn + " of " + faces);
        for (int face = turn; face != asking; face = (face + 1) % faces) if (ready[face]) return false;
        return true;
    }

    /** Whose turn it is after face {@code given} of {@code faces} was given a cut: the next face's. */
    public static int after(int given, int faces) {
        if (given < 0 || given >= faces) throw new IllegalArgumentException("Face " + given + " of " + faces);
        return (given + 1) % faces;
    }
}
