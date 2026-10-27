package org.jd.infestusfrontier.core.logistics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NetworkIndexTest {
    @Test
    void sharesACyclicComponentAndBoundsEachAdvance() {
        NetworkIndex<Integer> graph = new NetworkIndex<>(8, 32, 4);
        graph.request(0);
        assertEquals(2, graph.advance(2, n -> new NetworkIndex.Links<>(true, List.of((n + 1) % 4, (n + 3) % 4))));
        assertTrue(graph.request(0).isEmpty());
        assertEquals(2, graph.advance(2, n -> new NetworkIndex.Links<>(true, List.of((n + 1) % 4, (n + 3) % 4))));
        assertSame(graph.request(0).orElseThrow(), graph.request(3).orElseThrow());
        assertEquals(4, graph.cachedNodes());
    }

    @Test
    void refusesOversizedNetworksRatherThanReturningAPartialNetwork() {
        NetworkIndex<Integer> graph = new NetworkIndex<>(4, 16, 2);
        graph.request(0);
        assertEquals(4, graph.advance(100, n -> new NetworkIndex.Links<>(true, List.of(n + 1))));
        assertEquals(
                NetworkIndex.Status.TOO_LARGE, graph.request(0).orElseThrow().status());
        graph.invalidate(3);
        assertTrue(graph.request(0).isEmpty());
    }

    @Test
    void invalidationRestartsPartialWorkAndLeavesOtherNetworksCached() {
        NetworkIndex<Integer> graph = new NetworkIndex<>(8, 32, 4);
        Map<Integer, List<Integer>> links = Map.of(0, List.of(1), 1, List.of(0), 9, List.of());
        graph.request(9);
        graph.advance(1, n -> new NetworkIndex.Links<>(true, links.get(n)));
        Object independent = graph.request(9).orElseThrow();
        graph.request(0);
        graph.advance(1, n -> new NetworkIndex.Links<>(true, links.get(n)));
        graph.invalidate(1);
        graph.advance(4, n -> new NetworkIndex.Links<>(true, List.of()));
        assertEquals(List.of(0), graph.request(0).orElseThrow().nodes());
        assertSame(independent, graph.request(9).orElseThrow());
    }

    @Test
    void boundsTheQueueAndCache() {
        NetworkIndex<Integer> graph = new NetworkIndex<>(2, 3, 2);
        for (int n = 0; n < 100; n++) graph.request(n);
        assertEquals(2, graph.queued());
        graph.advance(100, n -> new NetworkIndex.Links<>(true, List.of()));
        for (int n = 0; n < 100; n++) {
            graph.request(n);
            graph.advance(1, x -> new NetworkIndex.Links<>(true, List.of()));
            assertTrue(graph.cachedNodes() <= 3);
        }
    }

    @Test
    void unavailableDiscoveryDoesNotExposeOtherMembers() {
        NetworkIndex<Integer> graph = new NetworkIndex<>(8, 32, 4);
        graph.request(0);
        graph.advance(8, n -> new NetworkIndex.Links<>(n == 0, n == 0 ? List.of(1) : List.of()));
        assertEquals(
                NetworkIndex.Status.UNAVAILABLE, graph.request(0).orElseThrow().status());
        assertTrue(graph.request(1).isEmpty());
    }

    @Test
    void fourFullNetworksShareDiscoveryWorkWithoutRepeatedScans() {
        NetworkIndex<Integer> graph = new NetworkIndex<>(1024, 8192, 256);
        for (int group = 0; group < 4; group++) graph.request(group * 1024);
        int visited = 0;
        long started = System.nanoTime();
        while (graph.queued() > 0) {
            int used = graph.advance(128, n -> {
                int start = (n / 1024) * 1024;
                return new NetworkIndex.Links<>(true, List.of(start + (n + 1) % 1024, start + (n + 1023) % 1024));
            });
            assertTrue(used <= 128);
            visited += used;
        }
        assertEquals(4096, visited);
        for (int n = 0; n < 4096; n++)
            assertEquals(1024, graph.request(n).orElseThrow().nodes().size());
        assertEquals(0, graph.queued());
        System.out.println(
                "4096-node discovery and cached lookup: " + (System.nanoTime() - started) / 1_000_000.0 + " ms");
    }

    @Test
    void aTenThousandNodeNetworkStopsAfterTheLocalLimit() {
        NetworkIndex<Integer> graph = new NetworkIndex<>(1024, 8192, 256);
        graph.request(0);
        assertEquals(1024, graph.advance(8192, n -> new NetworkIndex.Links<>(true, List.of((n + 1) % 10000))));
        assertEquals(
                NetworkIndex.Status.TOO_LARGE, graph.request(0).orElseThrow().status());
    }
}
