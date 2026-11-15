package org.jd.infestusfrontier.core.logistics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class CandidatesTest {
    private static final int LIMIT = 16;

    private static List<Integer> range(int size) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < size; i++) out.add(i);
        return out;
    }

    /** Attempts until one asks a usable candidate, or -1 when none does within {@code max}. */
    private static int attempts(Candidates cursor, List<Integer> order, Predicate<Integer> usable, int max) {
        for (int attempt = 1; attempt <= max; attempt++) {
            List<Integer> window = cursor.window(order, LIMIT);
            if (window.stream().anyMatch(usable)) return attempt;
            cursor.refused(window.size());
        }
        return -1;
    }

    @Test
    void aSmallNetworkIsAskedWhole() {
        Candidates cursor = new Candidates();
        assertEquals(range(5), cursor.window(range(5), LIMIT));
        cursor.refused(5);
        assertEquals(range(5).size(), cursor.window(range(5), LIMIT).size());
        assertEquals(List.of(), cursor.window(List.<Integer>of(), LIMIT));
    }

    @Test
    void anAttemptNeverAsksMoreThanTheLimit() {
        assertEquals(LIMIT, new Candidates().window(range(1024), LIMIT).size());
    }

    @Test
    void aRefusingPrefixOfAnyLengthIsPassedWithinCeilSizeOverLimitAttempts() {
        for (int size = 1; size <= 80; size++)
            for (int usable = 0; usable < size; usable++) {
                int at = usable;
                int bound = (size + LIMIT - 1) / LIMIT;
                int found = attempts(new Candidates(), range(size), i -> i == at, bound);
                assertTrue(found > 0 && found <= bound, "size " + size + " usable " + usable + " found " + found);
            }
    }

    @Test
    void theWindowWrapsPastTheEndAndKeepsItsLimit() {
        Candidates cursor = new Candidates();
        List<Integer> order = range(20);
        cursor.refused(LIMIT);
        List<Integer> window = cursor.window(order, LIMIT);
        assertEquals(LIMIT, window.size());
        assertEquals(List.of(16, 17, 18, 19, 0, 1), window.subList(0, 6));
    }

    @Test
    void aMoveSendsTheNextAttemptBackToTheTop() {
        Candidates cursor = new Candidates();
        List<Integer> order = range(40);
        cursor.refused(LIMIT);
        cursor.refused(LIMIT);
        assertEquals(32, cursor.window(order, LIMIT).get(0));
        cursor.moved();
        assertEquals(0, cursor.window(order, LIMIT).get(0));
    }

    @Test
    void aShorterOrderKeepsTheCursorInside() {
        Candidates cursor = new Candidates();
        cursor.refused(30);
        assertEquals(List.of(2, 3, 4, 5, 6, 0, 1), cursor.window(range(7), LIMIT));
    }
}
