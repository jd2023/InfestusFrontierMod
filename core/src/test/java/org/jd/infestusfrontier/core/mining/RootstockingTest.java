package org.jd.infestusfrontier.core.mining;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.jd.infestusfrontier.core.foundation.Refusal;
import org.junit.jupiter.api.Test;

class RootstockingTest {
    private static final MinePlan DEEPEST =
            new MinePlan(PlanShape.STAIRCASE, Surveying.MAX_DEPTH, Turn.NONE, Turn.NONE, false);

    @Test
    void aTreadIsCutTopDownFromItsHeadroomToItsRootCell() {
        PlanStep first = DEEPEST.steps().get(0);
        assertEquals(
                List.of(
                        new Offset(1, 0, 3),
                        new Offset(1, 0, 2),
                        new Offset(1, 0, 1),
                        new Offset(1, 0, 0),
                        new Offset(1, 0, -1)),
                Rootstocking.cutOrder(first),
                "three headroom blocks, the tread, then the root cell");
        assertEquals(new Offset(1, 0, 4), Rootstocking.overhead(first));
        assertEquals(new Offset(1, 0, -2), Rootstocking.ground(first));
    }

    @Test
    void theDeepestSectionDescendsThirtyTwoWithThreeClearOverEveryTreadAndTheLanding() {
        List<PlanStep> steps = DEEPEST.steps();
        assertEquals(Surveying.MAX_DEPTH + 1, steps.size(), "32 treads and a landing");
        for (int n = 0; n < Surveying.MAX_DEPTH; n++) {
            PlanStep step = steps.get(n);
            List<Offset> cuts = Rootstocking.cutOrder(step);
            assertEquals(5, cuts.size(), "headroom, tread and root cell");
            Offset tread = step.tread().orElseThrow();
            for (int up = 1; up <= 3; up++) assertTrue(step.passage().contains(tread.above(up)), "headroom " + up);
            assertEquals(-(n + 1), step.root().up(), "one lower each step");
            assertEquals(n + 1, step.root().forward(), "one further each step");
        }
        PlanStep landing = steps.get(Surveying.MAX_DEPTH);
        assertTrue(landing.tread().isEmpty());
        assertEquals(-Surveying.MAX_DEPTH, landing.root().up(), "the landing is 32 down");
        assertEquals(4, Rootstocking.cutOrder(landing).size(), "the landing's cell and its headroom");
        assertThrows(
                IllegalArgumentException.class,
                () -> new MinePlan(PlanShape.STAIRCASE, Surveying.MAX_DEPTH + 1, Turn.NONE, Turn.NONE, false),
                "no section goes deeper");
    }

    @Test
    void theMineheadShowsItsOwnStopThenTheWorkingOrgansOnly() {
        Optional<Refusal> none = Optional.empty();
        Optional<Refusal> jaw = Optional.of(Fracturing.BED_FULL);
        Optional<Refusal> grower = Optional.of(Surveying.NO_TREAD);
        Optional<Refusal> cilia = Optional.of(Collecting.STORAGE_FULL);
        Optional<Refusal> own = Optional.of(Rootstocking.NO_JAW);
        assertEquals(own, Rootstocking.shown(own, true, jaw, grower, cilia), "a missing part first");
        assertEquals(cilia, Rootstocking.shown(none, true, jaw, grower, cilia), "a full bed is the full storage's");
        assertEquals(jaw, Rootstocking.shown(none, true, jaw, grower, none), "a full bed the cilia still empty");
        Optional<Refusal> dry = Optional.of(Fracturing.NO_BIOMASS);
        assertEquals(dry, Rootstocking.shown(none, true, dry, grower, cilia), "the jaw while there is a cut");
        assertEquals(grower, Rootstocking.shown(none, false, jaw, grower, cilia), "the grower once it is clear");
        assertEquals(
                cilia,
                Rootstocking.shown(none, true, Optional.of(Fracturing.NO_ORDER), grower, cilia),
                "a jaw waiting for its order is not stopped");
        assertEquals(
                none,
                Rootstocking.shown(
                        none, true, Optional.of(Fracturing.NO_ORDER), Optional.of(Surveying.NOT_CLEAR), none),
                "a grower waiting for the cut is not stopped either");
        assertEquals(none, Rootstocking.shown(none, false, none, none, none), "working");
    }

    @Test
    void everyStopHasItsOwnKey() {
        assertEquals(
                Rootstocking.STOPS.size(),
                Rootstocking.STOPS.stream().map(Refusal::key).distinct().count());
        assertTrue(Rootstocking.STOPS.stream().allMatch(stop -> stop.key().startsWith("rootstock.")));
    }
}
