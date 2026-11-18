package org.jd.infestusfrontier.core.mining;

import java.util.List;
import org.jd.infestusfrontier.core.foundation.Refusal;

/**
 * The fixed limits of a surveyed mine plan and of the Structure Grower that builds one, and the reasons a grower
 * stops. A plan is a finite section the player approves on a Surveyed Tissue marker: a staircase that descends from
 * the marker, or a level route with at most two turns. Every number is a starting value, not measured balance.
 */
public final class Surveying {
    /** Blocks a staircase section descends at most. */
    public static final int MAX_DEPTH = 32;
    /** Cells a level route runs at most. */
    public static final int MAX_ROUTE_LENGTH = 64;
    /** Steps a plan of either shape has at most: a full route; a full staircase is its treads and its landing. */
    public static final int MAX_STEPS = Math.max(MAX_DEPTH + 1, MAX_ROUTE_LENGTH);
    /** Clear blocks over each tread and over the landing of a staircase. */
    public static final int STAIR_HEADROOM = 3;
    /** Clear blocks over each root cell of a level route, as in a Tunnel Root's tunnel. */
    public static final int ROUTE_HEADROOM = 2;
    /** Clear blocks over each root cell of a wide route: the Wider Section is three across and three high. */
    public static final int WIDE_HEADROOM = 3;
    /** A lit plan puts a light on every step whose number is a multiple of this. */
    public static final int LIGHT_SPACING = 8;
    /** How far one long press of a length or turn button moves it. */
    public static final int LONG_EDIT = 8;

    /** Loaded server ticks between two steps a grower builds: one second. */
    public static final int GROW_TICKS = 20;
    /** Loaded ticks a stopped grower waits before it looks again, unless its stock changes first. */
    public static final int RETRY_TICKS = 20;
    /** Steps the Structure Growers of one dimension build in one tick, together; a grower over it tries next tick. */
    public static final int STEPS_PER_TICK = 4;
    /** Root Segments, treads and lights the grower holds, each: one stack. */
    public static final int MAX_STOCK = 64;

    /** Editing: the plan was approved and no longer changes. */
    public static final Refusal APPROVED = new Refusal("survey.approved");
    /** Approving: part of the plan lies above or below the world. */
    public static final Refusal OUTSIDE_WORLD = new Refusal("survey.outside_world");

    /** No readable Surveyed Tissue marker is under the grower. */
    public static final Refusal NO_PLAN = new Refusal("structure_grower.no_plan");
    /** The marker's plan is still a draft. */
    public static final Refusal NOT_APPROVED = new Refusal("structure_grower.not_approved");
    /** Every step of the plan is built. */
    public static final Refusal FINISHED = new Refusal("structure_grower.finished");
    /** A chunk the next step reads or changes is not loaded. */
    public static final Refusal UNLOADED = new Refusal("structure_grower.unloaded");
    /** Fluid stands in the next step. */
    public static final Refusal FLUID = new Refusal("structure_grower.fluid");
    /** A block stands in the next step: it is not cut yet, or the player put something there. */
    public static final Refusal NOT_CLEAR = new Refusal("structure_grower.not_clear");
    /** No solid ground is under the next root cell: building it would bridge a cavity. */
    public static final Refusal NO_GROUND = new Refusal("structure_grower.no_ground");
    /** A creature stands where the next root cell or tread goes. */
    public static final Refusal CREATURE = new Refusal("structure_grower.creature");

    public static final Refusal NO_SEGMENT = new Refusal("structure_grower.no_segment");
    public static final Refusal NO_TREAD = new Refusal("structure_grower.no_tread");
    public static final Refusal NO_LIGHT = new Refusal("structure_grower.no_light");
    /** Neither wall beside the next light can hold it. */
    public static final Refusal NO_WALL = new Refusal("structure_grower.no_wall");
    /** Loading by hand: the grower already holds a full stack of that stock, or another kind of it. */
    public static final Refusal STOCK_FULL = new Refusal("structure_grower.stock_full");

    /** Every stop, in a fixed order: a screen names one by its index. */
    public static final List<Refusal> STOPS = List.of(
            NO_PLAN,
            NOT_APPROVED,
            FINISHED,
            UNLOADED,
            FLUID,
            NOT_CLEAR,
            NO_GROUND,
            CREATURE,
            NO_SEGMENT,
            NO_TREAD,
            NO_LIGHT,
            NO_WALL);

    private Surveying() {}
}
