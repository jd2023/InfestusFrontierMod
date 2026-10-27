package org.jd.infestusfrontier.core.logistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** Incremental connected components. One completed result is shared by every member. */
public final class NetworkIndex<N> {
    public enum Status {
        READY,
        TOO_LARGE,
        UNAVAILABLE
    }

    public record Links<N>(boolean available, List<N> neighbors) {
        public Links {
            neighbors = List.copyOf(neighbors);
        }
    }

    public record Component<N>(Status status, List<N> nodes) {
        public Component {
            nodes = List.copyOf(nodes);
        }
    }

    private final int limit;
    private final int cacheLimit;
    private final int queueLimit;
    private final Map<N, Component<N>> cache = new HashMap<>();
    private final Map<N, Set<Component<N>>> dependencies = new HashMap<>();
    private int weight;
    private final LinkedHashSet<N> requests = new LinkedHashSet<>();
    private final LinkedHashSet<N> seen = new LinkedHashSet<>();
    private final ArrayDeque<N> frontier = new ArrayDeque<>();
    private Optional<N> origin = Optional.empty();

    public NetworkIndex(int limit, int cacheLimit, int queueLimit) {
        if (limit < 1 || cacheLimit < limit + 1 || queueLimit < 1) throw new IllegalArgumentException("network bounds");
        this.limit = limit;
        this.cacheLimit = cacheLimit;
        this.queueLimit = queueLimit;
    }

    /** Empty means queued or refused because the bounded discovery queue is full. Retry later. */
    public Optional<Component<N>> request(N node) {
        Component<N> found = cache.get(node);
        if (found != null) return Optional.of(found);
        if (!seen.contains(node) && requests.size() < queueLimit) requests.add(node);
        return Optional.empty();
    }

    /** Expands at most budget nodes, across all waiting networks. Returns expansions used. */
    public int advance(int budget, Function<N, Links<N>> links) {
        int used = 0;
        while (used < budget) {
            if (origin.isEmpty()) {
                if (requests.isEmpty()) break;
                N start = requests.removeFirst();
                if (cache.containsKey(start)) continue;
                origin = Optional.of(start);
                seen.add(start);
                frontier.add(start);
            }
            N node = frontier.removeFirst();
            Links<N> next = links.apply(node);
            used++;
            if (!next.available()) {
                finish(Status.UNAVAILABLE);
                continue;
            }
            boolean overflow = false;
            for (N neighbor : next.neighbors()) {
                if (!seen.add(neighbor)) continue;
                if (seen.size() > limit) {
                    overflow = true;
                    break;
                }
                frontier.addLast(neighbor);
            }
            if (overflow) finish(Status.TOO_LARGE);
            else if (frontier.isEmpty()) finish(Status.READY);
        }
        return used;
    }

    private void finish(Status status) {
        Component<N> result = new Component<>(status, new ArrayList<>(seen));
        if (weight + seen.size() > cacheLimit) {
            cache.clear();
            dependencies.clear();
            weight = 0;
        }
        for (N node : seen)
            dependencies
                    .computeIfAbsent(node, ignored -> Collections.newSetFromMap(new IdentityHashMap<>()))
                    .add(result);
        weight += seen.size();
        // Partial failures are answers only for the origin, never proof of another node's component.
        if (status == Status.READY) for (N node : seen) cache.put(node, result);
        else cache.put(origin.orElseThrow(), result);
        requests.removeAll(status == Status.READY ? seen : Set.of(origin.orElseThrow()));
        origin = Optional.empty();
        seen.clear();
        frontier.clear();
    }

    /** Caller invalidates a changed node and its potential neighbors, including separated crossing passages. */
    public void invalidate(N node) {
        Set<Component<N>> affected = dependencies.get(node);
        if (affected != null)
            for (Component<N> component : List.copyOf(affected)) {
                for (N member : component.nodes()) {
                    if (cache.get(member) == component) cache.remove(member);
                    Set<Component<N>> owners = dependencies.get(member);
                    if (owners != null) {
                        owners.remove(component);
                        if (owners.isEmpty()) dependencies.remove(member);
                    }
                }
                weight -= component.nodes().size();
            }
        if (seen.contains(node)) {
            N restart = origin.orElseThrow();
            origin = Optional.empty();
            seen.clear();
            frontier.clear();
            if (requests.size() == queueLimit) requests.removeLast();
            requests.addFirst(restart);
        }
    }

    /** Discards derived state only. Pending callers retry through normal admission. */
    public void clear() {
        cache.clear();
        dependencies.clear();
        requests.clear();
        seen.clear();
        frontier.clear();
        origin = Optional.empty();
        weight = 0;
    }

    public int cachedNodes() {
        return cache.size();
    }

    public int queued() {
        return requests.size() + (origin.isPresent() ? 1 : 0);
    }
}
