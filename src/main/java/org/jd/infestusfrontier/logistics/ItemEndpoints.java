package org.jd.infestusfrontier.logistics;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jd.infestusfrontier.core.logistics.Delivery;
import org.jd.infestusfrontier.core.logistics.NetworkIndex;
import org.jd.infestusfrontier.core.logistics.Ranking;

/**
 * The Mouths of one item network that take and that give, each ranked by priority once. Membership, mode and priority
 * are a snapshot of the loaded Mouths when it was built; whether a Mouth is loaded, what it allows and what its store
 * holds are always read live from the candidates a call asks. Kept by {@link NetworkTopology}, which drops it with its
 * component (a split, a merge, a replaced Mouth, a mode change, the load of a chunk the network stands in) and when a
 * priority changes. A Mouth in an unloaded chunk is left out: that chunk loading replaces the component.
 */
record ItemEndpoints(Ranking<BlockPos> takers, Ranking<BlockPos> givers) {
    /** Reads every Mouth node of {@code component}: the one full pass over a network, paid for by the caller. */
    static ItemEndpoints build(ServerLevel level, NetworkIndex.Component<NetworkTopology.Node> component) {
        List<Delivery.Destination<BlockPos>> takers = new ArrayList<>();
        List<Delivery.Destination<BlockPos>> givers = new ArrayList<>();
        RoutingCounts.add(RoutingCounts.Step.NODES, component.nodes().size());
        for (NetworkTopology.Node node : component.nodes()) {
            if (node.faces() != (63 ^ NetworkTopology.bit(Direction.UP))) continue;
            RoutingCounts.add(RoutingCounts.Step.LOOKUPS, 1);
            if (!level.hasChunkAt(node.pos())) continue;
            if (!(level.getBlockEntity(node.pos()) instanceof MouthBlockEntity mouth)) continue;
            Delivery.Destination<BlockPos> entry = new Delivery.Destination<>(node.pos(), mouth.priority());
            if (mouth.mode().takes()) takers.add(entry);
            if (mouth.mode().gives()) givers.add(entry);
        }
        RoutingCounts.add(RoutingCounts.Step.RANKED, takers.size() + givers.size());
        RoutingCounts.add(RoutingCounts.Step.BUILDS, 1);
        return new ItemEndpoints(new Ranking<>(takers), new Ranking<>(givers));
    }
}
