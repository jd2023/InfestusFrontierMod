package org.jd.infestusfrontier.core.logistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A breadth-first walk over a network that stops after a fixed number of nodes. It holds no state between calls;
 * the caller caches the result.
 */
public final class Reach {
    private Reach() {}

    /** A node and its distance in steps from the start. */
    public record Reached<T>(T node, int distance) {}

    /** The nodes reached, nearest first, and whether the walk stopped at the limit with nodes left unvisited. */
    public record Result<T>(List<Reached<T>> reached, boolean truncated) {
        public Result {
            reached = List.copyOf(reached);
        }
    }

    /**
     * Walks from {@code start} through {@code links} and visits at most {@code limit} nodes, the start included. Each
     * node is visited once, so cycles end. {@code links} is called once per visited node.
     */
    public static <T> Result<T> walk(T start, Function<T, List<T>> links, int limit) {
        if (limit < 1) throw new IllegalArgumentException("Limit must be positive");
        Map<T, Integer> seen = new HashMap<>();
        ArrayDeque<T> queue = new ArrayDeque<>();
        List<Reached<T>> reached = new ArrayList<>();
        seen.put(start, 0);
        queue.add(start);
        boolean truncated = false;
        while (!queue.isEmpty()) {
            T node = queue.poll();
            int distance = seen.get(node);
            reached.add(new Reached<>(node, distance));
            for (T next : links.apply(node)) {
                if (seen.containsKey(next)) continue;
                if (seen.size() == limit) {
                    truncated = true;
                    continue;
                }
                seen.put(next, distance + 1);
                queue.add(next);
            }
        }
        return new Result<>(reached, truncated);
    }
}
