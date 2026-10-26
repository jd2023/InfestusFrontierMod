package org.jd.infestusfrontier.core.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jd.infestusfrontier.core.storage.StemPlan.Cell;
import org.jd.infestusfrontier.core.storage.StemPlan.Part;
import org.jd.infestusfrontier.core.storage.StemPlan.Pod;
import org.jd.infestusfrontier.core.storage.StemPlan.Step;
import org.junit.jupiter.api.Test;

class StemPlanTest {
    private static final StemPlan.Free OPEN = (x, y, z) -> true;

    /** Grows until nothing fits, at most {@code limit} steps. */
    private static StemPlan grown(StemPlan plan, StemPlan.Free free, int limit) {
        for (int step = 0; step < limit; step++) {
            Optional<Step> next = plan.next(free);
            if (next.isEmpty()) return plan;
            plan = plan.with(next.get());
        }
        return plan;
    }

    @Test
    void aPlantedRootRisesFirstAndThenSproutsPodsOnTierOne() {
        StemPlan plan = StemPlan.planted(7);
        Step first = plan.next(OPEN).orElseThrow();
        assertTrue(first instanceof Step.Grow grow
                && grow.cell().at(0, 1, 0)
                && grow.cell().part() == Part.TRUNK);
        plan = plan.with(first);
        Step second = plan.next(OPEN).orElseThrow();
        assertTrue(second instanceof Step.Sprout sprout
                && sprout.pod().y() == 1
                && sprout.pod().cell() == 1);
    }

    @Test
    void everyTreeStaysWithinItsLimitsAndStaggersItsPods() {
        for (long seed = 0; seed < 200; seed++) {
            StemPlan plan = grown(StemPlan.planted(seed), OPEN, 200);
            assertTrue(plan.pods().size() >= 12 && plan.pods().size() <= StemPlan.MAX_PODS, "seed " + seed + " pods");
            for (Cell cell : plan.cells()) assertTrue(cell.y() <= StemPlan.MAX_HEIGHT, "seed " + seed);
            List<int[]> taken = new ArrayList<>();
            for (Cell cell : plan.cells()) taken.add(new int[] {cell.x(), cell.y(), cell.z()});
            for (Pod pod : plan.pods()) taken.add(new int[] {pod.x(), pod.y(), pod.z()});
            for (int i = 0; i < taken.size(); i++)
                for (int j = i + 1; j < taken.size(); j++)
                    assertFalse(
                            taken.get(i)[0] == taken.get(j)[0]
                                    && taken.get(i)[1] == taken.get(j)[1]
                                    && taken.get(i)[2] == taken.get(j)[2],
                            "seed " + seed + " grows twice into one place");
            for (Pod pod : plan.pods()) {
                Cell on = plan.cells().get(pod.cell());
                assertTrue(on.tier() >= 1 && on.part() != Part.BRANCH, "pods hang on trunk tiers");
                for (Pod other : plan.pods())
                    assertFalse(
                            other.cell() == on.parent() && other.side() == pod.side(),
                            "seed " + seed + " stacks a pod over the one below");
            }
            assertTrue(plan.cells().stream().anyMatch(cell -> cell.part() == Part.BRANCH), "seed " + seed + " forks");
        }
    }

    @Test
    void theForkRisesFromItsBranchAndEveryCellGrowsFromAnEarlierOne() {
        StemPlan plan = grown(StemPlan.planted(3), OPEN, 200);
        for (int i = 1; i < plan.cells().size(); i++) {
            Cell cell = plan.cells().get(i);
            assertTrue(cell.parent() >= 0 && cell.parent() < i, "parents come first");
            Cell parent = plan.cells().get(cell.parent());
            if (cell.part() == Part.BRANCH)
                assertTrue(cell.y() == parent.y() && parent.trunk() == 0 && cell.trunk() == 1 && cell.tier() == 0);
            else assertTrue(cell.x() == parent.x() && cell.z() == parent.z() && cell.y() == parent.y() + 1);
        }
    }

    @Test
    void theSameSeedInTheSameRoomGrowsTheSameTree() {
        StemPlan.Free walled = (x, y, z) -> x < 2 && y < 4;
        assertEquals(grown(StemPlan.planted(11), walled, 200), grown(StemPlan.planted(11), walled, 200));
        assertFalse(grown(StemPlan.planted(11), OPEN, 200).equals(grown(StemPlan.planted(12), OPEN, 200)));
    }

    @Test
    void nothingGrowsWhereNothingIsFree() {
        StemPlan plan = StemPlan.planted(5);
        assertTrue(plan.next((x, y, z) -> false).isEmpty());
        StemPlan low = grown(plan, (x, y, z) -> y <= 2, 200);
        assertTrue(low.cells().stream().allMatch(cell -> cell.y() <= 2));
        assertTrue(low.next((x, y, z) -> y <= 2).isEmpty());
    }

    @Test
    void aHandPressedPodJoinsThePlanOnce() {
        StemPlan plan = StemPlan.planted(1).with(plan0().next(OPEN).orElseThrow());
        StemPlan adopted = plan.adopt(1, 2);
        assertEquals(1, adopted.pods().size());
        assertTrue(adopted.pods().getFirst().at(0, 1, 1));
        assertEquals(adopted, adopted.adopt(1, 2));
        StemPlan forked = grown(StemPlan.planted(7), OPEN, 200);
        Cell branch = forked.cells().stream()
                .filter(c -> c.part() == Part.BRANCH)
                .findFirst()
                .orElseThrow();
        assertEquals(forked, forked.adopt(branch.parent(), branch.side()), "no pod where a branch is planned");
    }

    private static StemPlan plan0() {
        return StemPlan.planted(1);
    }
}
