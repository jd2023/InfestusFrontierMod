package org.jd.infestusfrontier.core.logistics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReachTest {
    @Test
    void visitsNearestFirstWithDistances() {
        Map<String, List<String>> net =
                Map.of("a", List.of("b", "c"), "b", List.of("a", "d"), "c", List.of("a"), "d", List.of("b"));
        var result = Reach.walk("a", net::get, 10);
        assertEquals(
                List.of(
                        new Reach.Reached<>("a", 0),
                        new Reach.Reached<>("b", 1),
                        new Reach.Reached<>("c", 1),
                        new Reach.Reached<>("d", 2)),
                result.reached());
        assertFalse(result.truncated());
    }

    @Test
    void aCycleEnds() {
        Map<Integer, List<Integer>> ring =
                Map.of(0, List.of(1, 3), 1, List.of(2, 0), 2, List.of(3, 1), 3, List.of(0, 2));
        assertEquals(4, Reach.walk(0, ring::get, 100).reached().size());
    }

    @Test
    void stopsAtTheLimitAndSaysSo() {
        int[] calls = {0};
        var result = Reach.walk(
                0,
                n -> {
                    calls[0]++;
                    return List.of(n + 1);
                },
                256);
        assertEquals(256, result.reached().size());
        assertEquals(255, result.reached().getLast().distance());
        assertTrue(result.truncated());
        assertEquals(256, calls[0], "one call per visited node");
    }

    @Test
    void rejectsAnEmptyLimit() {
        assertThrows(IllegalArgumentException.class, () -> Reach.walk(0, n -> List.of(), 0));
    }
}
