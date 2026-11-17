package org.jd.infestusfrontier.core.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A surveyed section, relative to its marker: its shape, its length (the blocks a staircase descends, or the cells a
 * route runs), a route's two turn slots and whether it is lit. It is geometry only: it names the blocks to clear and
 * what goes in them, never what the ground holds. A value outside the limits does not construct, so impossible saved
 * numbers are refused, not repaired.
 *
 * <p>A staircase of depth {@code d} has {@code d} steps, each a root cell with a tread on it and
 * {@value Surveying#STAIR_HEADROOM} clear blocks over the tread, one block further on and one lower each time, then a
 * landing: a root cell {@code d} blocks under the marker with the same headroom. A route of length {@code n} has
 * {@code n} root cells level with the marker, each with {@value Surveying#ROUTE_HEADROOM} clear blocks over it, turning
 * a quarter at each turn's cell.
 */
public record MinePlan(PlanShape shape, int length, Turn first, Turn second, boolean lit) {
    /** A new marker's draft: a short unlit staircase. */
    public static final MinePlan START = new MinePlan(PlanShape.STAIRCASE, 8, Turn.NONE, Turn.NONE, false);

    public MinePlan {
        if (length < 1 || length > longest(shape)) throw new IllegalArgumentException("Length: " + length);
        if (shape == PlanShape.STAIRCASE && (first.present() || second.present()))
            throw new IllegalArgumentException("A staircase does not turn");
        for (Turn turn : List.of(first, second))
            if (turn.present() && turn.at() >= length) throw new IllegalArgumentException("Turn past the end: " + turn);
        if (first.present() && second.present() && first.at() >= second.at())
            throw new IllegalArgumentException("Turns out of order: " + first + ", " + second);
    }

    /** The greatest length a plan of {@code shape} may have. */
    public static int longest(PlanShape shape) {
        return shape == PlanShape.STAIRCASE ? Surveying.MAX_DEPTH : Surveying.MAX_ROUTE_LENGTH;
    }

    // ---- geometry ----

    /** Every step in building order. */
    public List<PlanStep> steps() {
        return shape == PlanShape.STAIRCASE ? staircase() : route();
    }

    private List<PlanStep> staircase() {
        List<PlanStep> steps = new ArrayList<>(length + 1);
        for (int number = 1; number <= length + 1; number++) {
            boolean landing = number == length + 1;
            Offset root = new Offset(number, 0, landing ? -length : -number);
            Optional<Offset> tread = landing ? Optional.empty() : Optional.of(root.above(1));
            Offset floor = tread.orElse(root);
            steps.add(step(number, root, tread, floor, Surveying.STAIR_HEADROOM, 0, 0));
        }
        return steps;
    }

    private List<PlanStep> route() {
        List<PlanStep> steps = new ArrayList<>(length);
        Offset at = new Offset(0, 0, 0);
        int heading = 0;
        for (int number = 1; number <= length; number++) {
            at = at.along(heading);
            int leaving = heading;
            for (Turn turn : List.of(first, second))
                if (turn.present() && turn.at() == number) leaving += turn.quarter();
            leaving = Math.floorMod(leaving, 4);
            steps.add(step(number, at, Optional.empty(), at, Surveying.ROUTE_HEADROOM, heading, leaving));
            heading = leaving;
        }
        return steps;
    }

    /** A step whose walking surface is {@code floor}; its light, if any, is at head height, two blocks over it. */
    private PlanStep step(
            int number, Offset root, Optional<Offset> tread, Offset floor, int headroom, int arrived, int leaving) {
        List<Offset> passage = new ArrayList<>(headroom);
        for (int up = 1; up <= headroom; up++) passage.add(floor.above(up));
        Optional<Offset> light =
                lit && number % Surveying.LIGHT_SPACING == 0 ? Optional.of(floor.above(2)) : Optional.empty();
        return new PlanStep(number, root, tread, passage, light, arrived, leaving);
    }

    /** The root cell of the last step: the landing of a staircase, the end of a route. */
    public Offset end() {
        List<PlanStep> steps = steps();
        return steps.get(steps.size() - 1).root();
    }

    /** What the whole plan takes to build. */
    public Needs needs() {
        int segments = 0;
        int treads = 0;
        int lights = 0;
        for (PlanStep step : steps()) {
            segments++;
            if (step.tread().isPresent()) treads++;
            if (step.light().isPresent()) lights++;
        }
        return new Needs(segments, treads, lights);
    }

    /** Root Segments, treads and lights. */
    public record Needs(int segments, int treads, int lights) {}

    // ---- editing a draft ----

    /** This draft changed as asked, by one block or by {@value Surveying#LONG_EDIT} when {@code far}, or unchanged. */
    public MinePlan edit(PlanEdit edit, boolean far) {
        int by = far ? Surveying.LONG_EDIT : 1;
        return switch (edit) {
            case SHAPE ->
                shape == PlanShape.STAIRCASE
                        ? new MinePlan(PlanShape.ROUTE, length, Turn.NONE, Turn.NONE, lit)
                        : new MinePlan(
                                PlanShape.STAIRCASE, Math.min(length, Surveying.MAX_DEPTH), Turn.NONE, Turn.NONE, lit);
            case SHORTER -> resized(length - by);
            case LONGER -> resized(length + by);
            case FIRST_WAY -> withFirst(nextWay(first, true));
            case FIRST_EARLIER -> withFirst(moved(first, -by, true));
            case FIRST_LATER -> withFirst(moved(first, by, true));
            case SECOND_WAY -> withSecond(nextWay(second, false));
            case SECOND_EARLIER -> withSecond(moved(second, -by, false));
            case SECOND_LATER -> withSecond(moved(second, by, false));
            case LIGHTS -> new MinePlan(shape, length, first, second, !lit);
        };
    }

    /** At {@code wanted} cells, kept within the shape's limits; a turn the new end passes is dropped. */
    private MinePlan resized(int wanted) {
        int clamped = Math.max(1, Math.min(longest(shape), wanted));
        return new MinePlan(shape, clamped, within(first, clamped), within(second, clamped), lit);
    }

    private static Turn within(Turn turn, int length) {
        return turn.present() && turn.at() >= length ? Turn.NONE : turn;
    }

    private MinePlan withFirst(Turn turn) {
        return new MinePlan(shape, length, turn, second, lit);
    }

    private MinePlan withSecond(Turn turn) {
        return new MinePlan(shape, length, first, turn, lit);
    }

    /** The lowest and highest cells the slot may turn at, after the other turn's limits; low > high when none. */
    private int low(boolean isFirst) {
        return isFirst || !first.present() ? 1 : first.at() + 1;
    }

    private int high(boolean isFirst) {
        return isFirst && second.present() ? second.at() - 1 : length - 1;
    }

    /** The slot's next way. A new turn starts halfway along the free cells; with no free cell the slot stays empty. */
    private Turn nextWay(Turn turn, boolean isFirst) {
        if (shape == PlanShape.STAIRCASE) return turn;
        return switch (turn.way()) {
            case STRAIGHT -> {
                int low = low(isFirst);
                int high = high(isFirst);
                yield low > high ? turn : new Turn(Steer.LEFT, (low + high + 1) / 2);
            }
            case LEFT -> new Turn(Steer.RIGHT, turn.at());
            case RIGHT -> Turn.NONE;
        };
    }

    private Turn moved(Turn turn, int by, boolean isFirst) {
        if (!turn.present()) return turn;
        int at = Math.max(low(isFirst), Math.min(high(isFirst), turn.at() + by));
        return new Turn(turn.way(), at);
    }
}
