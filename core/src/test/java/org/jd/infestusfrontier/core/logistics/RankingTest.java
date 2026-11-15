package org.jd.infestusfrontier.core.logistics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RankingTest {
    private static Delivery.Destination<String> d(String id, int priority) {
        return new Delivery.Destination<>(id, priority);
    }

    /** Destinations as {@code id priority} pairs, e.g. {@code of("a", 1, "b", 0)}. */
    private static Ranking<String> of(Object... idsAndPriorities) {
        List<Delivery.Destination<String>> destinations = new ArrayList<>();
        for (int i = 0; i < idsAndPriorities.length; i += 2)
            destinations.add(d((String) idsAndPriorities[i], (Integer) idsAndPriorities[i + 1]));
        return new Ranking<>(destinations);
    }

    @Test
    void priorityBeatsDiscoveryOrder() {
        assertEquals(
                List.of("far-high", "near-low"),
                of("near-low", 0, "far-high", 2).ordered(0));
    }

    @Test
    void equalPriorityStartsInDiscoveryOrder() {
        assertEquals(List.of("b", "a"), of("b", 0, "a", 0).ordered(0));
    }

    @Test
    void equalDestinationsTakeTurns() {
        Ranking<String> group = of("a", 0, "b", 0, "c", 0, "z", -1);
        assertEquals(List.of("a", "b", "c", "z"), group.ordered(0));
        assertEquals(List.of("b", "c", "a", "z"), group.ordered(1));
        assertEquals(List.of("c", "a", "b", "z"), group.ordered(5));
        assertEquals(List.of("c", "a", "b", "z"), group.ordered(-1));
    }

    @Test
    void groupsOfEveryPriorityTurnTogether() {
        Ranking<String> ranking = of("a", 1, "b", 0, "c", 1, "d", 0, "e", 0, "f", -4);
        assertEquals(List.of("a", "c", "b", "d", "e", "f"), ranking.ordered(0));
        assertEquals(List.of("c", "a", "d", "e", "b", "f"), ranking.ordered(1));
    }

    @Test
    void anEmptyRankingIsEmpty() {
        assertEquals(List.of(), new Ranking<String>(List.of()).ordered(3));
        assertEquals(0, new Ranking<String>(List.of()).size());
    }

    @Test
    void everyDestinationAppearsOnceHighestPriorityFirst() {
        Random random = new Random(7);
        List<Delivery.Destination<Integer>> all = new ArrayList<>();
        for (int i = 0; i < 1023; i++)
            all.add(new Delivery.Destination<>(
                    i, random.nextInt(Delivery.MAX_PRIORITY * 2 + 1) - Delivery.MAX_PRIORITY));
        Ranking<Integer> ranking = new Ranking<>(all);
        for (int turn : new int[] {-3, 0, 1, 40}) {
            List<Integer> order = ranking.ordered(turn);
            assertEquals(1023, order.size());
            assertEquals(1023, new HashSet<>(order).size(), "no destination twice, turn " + turn);
            for (int i = 1; i < order.size(); i++)
                assertTrue(
                        all.get(order.get(i - 1)).priority()
                                >= all.get(order.get(i)).priority(),
                        "turn " + turn);
        }
    }

    @Test
    void aWindowOfTheOrderReadsOnlyTheWindow() {
        var all = new ArrayList<Delivery.Destination<Integer>>();
        for (int i = 0; i < 1000; i++) all.add(new Delivery.Destination<>(i, 0));
        List<Integer> window = new Candidates().window(new Ranking<>(all).ordered(5), 16);
        assertEquals(16, window.size());
        assertEquals(5, window.getFirst());
        assertEquals(20, window.getLast());
    }
}
