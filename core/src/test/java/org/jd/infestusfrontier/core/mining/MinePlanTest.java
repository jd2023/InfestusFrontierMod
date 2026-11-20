package org.jd.infestusfrontier.core.mining;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MinePlanTest {
    private static MinePlan stairs(int depth) {
        return new MinePlan(PlanShape.STAIRCASE, depth, Turn.NONE, Turn.NONE);
    }

    private static MinePlan route(int length, Turn first, Turn second) {
        return new MinePlan(PlanShape.ROUTE, length, first, second);
    }

    private static MinePlan edits(MinePlan plan, PlanEdit... edits) {
        for (PlanEdit edit : edits) plan = plan.edit(edit, false);
        return plan;
    }

    @Test
    void aStaircaseDescendsOneBlockAStepToALandingItsDepthBelowTheMarker() {
        List<PlanStep> steps = stairs(3).steps();
        assertEquals(4, steps.size(), "three treads and a landing");
        for (int n = 1; n <= 3; n++) {
            PlanStep step = steps.get(n - 1);
            assertEquals(n, step.number());
            assertEquals(new Offset(n, 0, -n), step.root(), "the root cell, one further and one lower");
            assertEquals(Optional.of(new Offset(n, 0, -n + 1)), step.tread(), "the tread stands on the root cell");
            assertEquals(
                    List.of(new Offset(n, 0, -n + 2), new Offset(n, 0, -n + 3), new Offset(n, 0, -n + 4)),
                    step.passage(),
                    "three clear blocks over the tread");
            assertFalse(step.bend());
        }
        PlanStep landing = steps.get(3);
        assertEquals(new Offset(4, 0, -3), landing.root(), "the landing is the depth below the marker");
        assertEquals(Optional.empty(), landing.tread());
        assertEquals(
                List.of(new Offset(4, 0, -2), new Offset(4, 0, -1), new Offset(4, 0, 0)),
                landing.passage(),
                "three clear blocks over the landing");
        assertEquals(new Offset(4, 0, -3), stairs(3).end());
        assertEquals(new MinePlan.Needs(4, 3, 1, 2), stairs(3).needs(), "a light and ribs at the landing");
    }

    @Test
    void aStaircaseDescendsAtMostThirtyTwoBlocks() {
        MinePlan deepest = stairs(Surveying.MAX_DEPTH);
        assertEquals(-32, deepest.end().up());
        int lowest = 0;
        for (PlanStep step : deepest.steps()) for (Offset cell : step.cleared()) lowest = Math.min(lowest, cell.up());
        assertEquals(-32, lowest, "nothing is cleared below the landing");
        assertThrows(IllegalArgumentException.class, () -> stairs(Surveying.MAX_DEPTH + 1));
        assertThrows(IllegalArgumentException.class, () -> stairs(0));
        assertEquals(deepest, deepest.edit(PlanEdit.LONGER, false), "longer stops at the limit");
        assertEquals(deepest, deepest.edit(PlanEdit.LONGER, true));
    }

    @Test
    void aRouteStaysLevelAndRunsAtMostSixtyFourCells() {
        MinePlan longest = route(Surveying.MAX_ROUTE_LENGTH, Turn.NONE, Turn.NONE);
        List<PlanStep> steps = longest.steps();
        assertEquals(64, steps.size());
        for (PlanStep step : steps) {
            assertEquals(new Offset(step.number(), 0, 0), step.root(), "straight ahead, level with the marker");
            assertEquals(Optional.empty(), step.tread());
            assertEquals(List.of(step.root().above(1), step.root().above(2)), step.passage(), "two clear blocks");
        }
        assertEquals(new MinePlan.Needs(64, 0, 8, 2), longest.needs(), "eight lights, ribs at both ends");
        assertThrows(IllegalArgumentException.class, () -> route(Surveying.MAX_ROUTE_LENGTH + 1, Turn.NONE, Turn.NONE));
        assertEquals(longest, longest.edit(PlanEdit.LONGER, true), "longer stops at the limit");
    }

    @Test
    void aRouteTurnsAQuarterAtEachTurnCellWithABendThere() {
        MinePlan plan = route(6, new Turn(Steer.RIGHT, 2), new Turn(Steer.LEFT, 4));
        List<PlanStep> steps = plan.steps();
        assertEquals(
                List.of(
                        new Offset(1, 0, 0),
                        new Offset(2, 0, 0),
                        new Offset(2, 1, 0),
                        new Offset(2, 2, 0),
                        new Offset(3, 2, 0),
                        new Offset(4, 2, 0)),
                steps.stream().map(PlanStep::root).toList());
        assertTrue(steps.get(1).bend());
        assertEquals(0, steps.get(1).arrived());
        assertEquals(1, steps.get(1).leaving(), "right is a quarter clockwise");
        assertTrue(steps.get(3).bend());
        assertEquals(1, steps.get(3).arrived());
        assertEquals(0, steps.get(3).leaving(), "then left again");
        assertFalse(steps.get(0).bend());
        assertFalse(steps.get(5).bend());
    }

    @Test
    void routesTurnAtMostTwiceInsideTheirLengthAndNeverCrossThemselves() {
        assertThrows(IllegalArgumentException.class, () -> route(5, new Turn(Steer.LEFT, 5), Turn.NONE));
        assertThrows(IllegalArgumentException.class, () -> route(5, new Turn(Steer.LEFT, 3), new Turn(Steer.LEFT, 3)));
        assertThrows(IllegalArgumentException.class, () -> route(5, new Turn(Steer.LEFT, 3), new Turn(Steer.LEFT, 2)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MinePlan(PlanShape.STAIRCASE, 5, new Turn(Steer.LEFT, 2), Turn.NONE));
        for (Steer a : List.of(Steer.LEFT, Steer.RIGHT))
            for (Steer b : List.of(Steer.LEFT, Steer.RIGHT))
                for (int first = 1; first < 9; first++)
                    for (int second = first + 1; second < 10; second++) {
                        MinePlan plan = route(10, new Turn(a, first), new Turn(b, second));
                        Set<Offset> cells = new HashSet<>();
                        for (PlanStep step : plan.steps())
                            for (Offset cell : step.cleared())
                                assertTrue(cells.add(cell), plan + " clears " + cell + " twice");
                    }
    }

    @Test
    void everyPlanPutsALightAtHeadHeightOnEveryEighthStepAndItsLast() {
        List<PlanStep> steps = stairs(16).steps();
        for (PlanStep step : steps) {
            boolean lit = step.number() % Surveying.LIGHT_SPACING == 0 || step.number() == steps.size();
            assertEquals(lit, step.light().isPresent(), "step " + step.number());
            if (lit) {
                assertEquals(step.passage().get(2), step.light().get(), "the top of the passage, under the ceiling");
                assertTrue(step.cleared().contains(step.light().get()));
            }
        }
        assertEquals(new MinePlan.Needs(17, 16, 3, 2), stairs(16).needs(), "the landing is lit too");
        assertEquals(3, route(20, Turn.NONE, Turn.NONE).needs().lights(), "steps 8 and 16 and the route's end");
        assertEquals(2, route(16, Turn.NONE, Turn.NONE).needs().lights(), "the end is one of the eighth steps");
        assertEquals(1, route(1, Turn.NONE, Turn.NONE).needs().lights(), "a one-cell route is its own junction");
    }

    @Test
    void ribsFrameTheEntranceAndTheJunctionAndEveryFourthStepOfAWideRoute() {
        List<PlanStep> stair = stairs(6).steps();
        for (PlanStep step : stair)
            assertEquals(step.number() == 1 || step.number() == 7, step.framed(), "stair step " + step.number());
        List<PlanStep> narrow = route(10, Turn.NONE, Turn.NONE).steps();
        for (PlanStep step : narrow)
            assertEquals(step.number() == 1 || step.number() == 10, step.framed(), "route step " + step.number());
        List<PlanStep> wide = wide(10, Turn.NONE, Turn.NONE).steps();
        for (PlanStep step : wide)
            assertEquals(
                    step.number() == 1 || step.number() == 10 || step.number() % Surveying.RIB_SPACING == 0,
                    step.framed(),
                    "wide step " + step.number());
        assertEquals(12, wide(10, Turn.NONE, Turn.NONE).needs().ribs(), "a rib for each column of four framed steps");
    }

    @Test
    void everyThirdStepIsScarredOnAlternateWalls() {
        List<PlanStep> steps = route(12, Turn.NONE, Turn.NONE).steps();
        for (PlanStep step : steps)
            assertEquals(step.number() % Surveying.SCAR_SPACING == 0, step.scarred(), "step " + step.number());
        assertEquals(3, steps.get(2).scarSide(), "the first scar on the left");
        assertEquals(1, steps.get(5).scarSide(), "the second on the right");
        assertEquals(3, steps.get(8).scarSide());
        assertThrows(IllegalStateException.class, () -> steps.get(0).scarSide(), "an unscarred step has no wall");
    }

    private static MinePlan wide(int length, Turn first, Turn second) {
        return new MinePlan(PlanShape.ROUTE, length, first, second, true);
    }

    @Test
    void aWideRouteIsThreeAcrossAndThreeHighWithARootCellUnderEachColumn() {
        MinePlan longest = wide(Surveying.MAX_ROUTE_LENGTH, Turn.NONE, Turn.NONE);
        List<PlanStep> steps = longest.steps();
        assertEquals(64, steps.size(), "as long as a narrow route");
        for (PlanStep step : steps) {
            int n = step.number();
            assertEquals(new Offset(n, 0, 0), step.root());
            assertEquals(List.of(new Offset(n, -1, 0), new Offset(n, 1, 0)), step.flanks(), "a root left and right");
            assertEquals(9, step.passage().size(), "three columns three high");
            for (Offset root : step.roots())
                for (int up = 1; up <= Surveying.WIDE_HEADROOM; up++)
                    assertTrue(step.passage().contains(root.above(up)), "clear over " + root + " at " + up);
            assertEquals(12, step.cleared().size(), "four times the cuts of a narrow step");
        }
        MinePlan narrow = route(Surveying.MAX_ROUTE_LENGTH, Turn.NONE, Turn.NONE);
        assertEquals(3, narrow.steps().get(0).cleared().size(), "a narrow step cuts three");
        assertEquals(192, longest.needs().segments(), "three segments a step");
        assertEquals(64, narrow.needs().segments(), "one segment a step");
        assertThrows(
                IllegalArgumentException.class,
                () -> new MinePlan(PlanShape.STAIRCASE, 4, Turn.NONE, Turn.NONE, true),
                "a staircase is never wide");
    }

    @Test
    void aWideRouteTurnsWithoutPlanningABlockTwice() {
        MinePlan plan = wide(6, new Turn(Steer.RIGHT, 3), Turn.NONE);
        Set<Offset> seen = new HashSet<>();
        for (PlanStep step : plan.steps())
            for (Offset cell : step.cleared()) assertTrue(seen.add(cell), "planned once: " + cell);
        PlanStep corner = plan.steps().get(2);
        assertTrue(corner.bend());
        assertEquals(List.of(new Offset(4, 0, 0)), corner.flanks(), "the turn's outer root; its inner one is held");
        PlanStep after = plan.steps().get(3);
        assertEquals(new Offset(3, 1, 0), after.root(), "on to the right");
        assertEquals(List.of(new Offset(4, 1, 0)), after.flanks(), "beside it, the root not held yet");
        assertEquals(16, plan.needs().segments(), "a segment for each root cell planned");
        // two turns a cell apart double back onto the first leg's columns
        assertThrows(IllegalArgumentException.class, () -> wide(6, new Turn(Steer.RIGHT, 2), new Turn(Steer.RIGHT, 3)));
        assertEquals(
                route(6, new Turn(Steer.RIGHT, 2), new Turn(Steer.RIGHT, 3)),
                route(6, new Turn(Steer.RIGHT, 2), new Turn(Steer.RIGHT, 3)).edit(PlanEdit.WIDTH, false),
                "a route that would double back on itself is not widened");
        MinePlan apart = wide(8, new Turn(Steer.RIGHT, 2), new Turn(Steer.RIGHT, 4));
        assertEquals(apart, apart.edit(PlanEdit.SECOND_EARLIER, false), "nor is a wide one's turn moved onto it");
        assertEquals(8, apart.steps().size(), "two cells apart, the legs share only their flanks");
    }

    @Test
    void theWidthIsARouteSettingThatLightsTheOuterWall() {
        MinePlan narrow = route(16, Turn.NONE, Turn.NONE);
        MinePlan widened = narrow.edit(PlanEdit.WIDTH, false);
        assertTrue(widened.wide());
        assertFalse(widened.edit(PlanEdit.WIDTH, false).wide(), "and back");
        assertFalse(widened.edit(PlanEdit.SHAPE, false).wide(), "a staircase drops the width");
        assertEquals(stairs(8), stairs(8).edit(PlanEdit.WIDTH, false), "a staircase has none");
        assertEquals(
                Optional.of(new Offset(8, -1, 3)), widened.steps().get(7).light(), "on the left column, at the top");
        assertEquals(
                Optional.of(new Offset(8, 0, 2)), narrow.steps().get(7).light(), "a narrow route's top is head high");
        assertEquals(widened.edit(PlanEdit.LONGER, false).length(), 17, "it is edited like any route");
    }

    @Test
    void theDraftIsEditedWithinItsLimits() {
        MinePlan plan = MinePlan.START;
        assertEquals(PlanShape.STAIRCASE, plan.shape());
        assertEquals(9, plan.edit(PlanEdit.LONGER, false).length());
        assertEquals(16, plan.edit(PlanEdit.LONGER, true).length());
        assertEquals(1, plan.edit(PlanEdit.SHORTER, true).length(), "never shorter than one");
        assertEquals(plan, plan.edit(PlanEdit.FIRST_WAY, false), "a staircase does not turn");

        MinePlan route = plan.edit(PlanEdit.SHAPE, false);
        assertEquals(PlanShape.ROUTE, route.shape());
        MinePlan turned = edits(route, PlanEdit.FIRST_WAY);
        assertEquals(new Turn(Steer.LEFT, 4), turned.first(), "a new turn starts halfway");
        assertEquals(new Turn(Steer.RIGHT, 4), edits(turned, PlanEdit.FIRST_WAY).first());
        assertEquals(
                Turn.NONE, edits(turned, PlanEdit.FIRST_WAY, PlanEdit.FIRST_WAY).first());
        MinePlan both = edits(turned, PlanEdit.SECOND_WAY);
        assertEquals(new Turn(Steer.LEFT, 6), both.second(), "the second turn starts halfway after the first");
        assertEquals(5, both.edit(PlanEdit.FIRST_LATER, true).first().at(), "the first turn stays before the second");
        assertEquals(1, both.edit(PlanEdit.FIRST_EARLIER, true).first().at());
        assertEquals(5, both.edit(PlanEdit.SECOND_EARLIER, true).second().at(), "the second stays after the first");
        assertEquals(7, both.edit(PlanEdit.SECOND_LATER, true).second().at(), "and before the end");
        MinePlan cut = both.edit(PlanEdit.SHORTER, false).edit(PlanEdit.SHORTER, false);
        assertEquals(6, cut.length());
        assertEquals(Turn.NONE, cut.second(), "a turn the end passes is dropped");
        assertEquals(new Turn(Steer.LEFT, 4), cut.first());
        MinePlan back = both.edit(PlanEdit.SHAPE, false);
        assertEquals(PlanShape.STAIRCASE, back.shape());
        assertEquals(Turn.NONE, back.first(), "a staircase keeps no turns");
        MinePlan long_ = new MinePlan(PlanShape.ROUTE, 50, Turn.NONE, Turn.NONE);
        assertEquals(Surveying.MAX_DEPTH, long_.edit(PlanEdit.SHAPE, false).length(), "a staircase is cut to 32");
        MinePlan single = new MinePlan(PlanShape.ROUTE, 1, Turn.NONE, Turn.NONE);
        assertEquals(single, single.edit(PlanEdit.FIRST_WAY, false), "a one-cell route has nowhere to turn");
        MinePlan full = new MinePlan(PlanShape.ROUTE, 3, new Turn(Steer.LEFT, 1), new Turn(Steer.LEFT, 2));
        assertEquals(full, full.edit(PlanEdit.FIRST_LATER, false), "no room between the turns");
    }

    @Test
    void thePlanReadsNothingButItsOwnNumbers() {
        assertEquals(stairs(5).steps(), stairs(5).steps(), "the same plan gives the same geometry");
        for (var component : MinePlan.class.getRecordComponents())
            assertTrue(
                    List.of(PlanShape.class, int.class, Turn.class, boolean.class)
                            .contains(component.getType()),
                    "geometry from shape, length, turns and width only: " + component);
    }
}
