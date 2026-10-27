package org.jd.infestusfrontier.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jd.infestusfrontier.core.logistics.NetworkIndex;

/** Saved knowledge of passive connections; contains no inventory or block-entity references. */
final class NetworkTopology extends SavedData {
    static final int MAX_CELLS = 1024;
    static final int MAX_KNOWN = 65536;
    private static final String NAME = "infestusfrontier_networks";
    private static final SavedData.Factory<NetworkTopology> FACTORY =
            new SavedData.Factory<>(NetworkTopology::new, (tag, registries) ->
                    (NetworkTopology) LogisticsModule.restoreNetworks(tag, registries));

    record Node(BlockPos pos, Carrier carrier, int faces) {}

    record Tissue(Carrier carrier, List<Integer> passages) {
        static final Codec<Tissue> CODEC = RecordCodecBuilder.create(i -> i.group(
                        Codec.STRING
                                .comapFlatMap(
                                        name -> {
                                            try {
                                                return com.mojang.serialization.DataResult.success(
                                                        Carrier.valueOf(name));
                                            } catch (IllegalArgumentException error) {
                                                return com.mojang.serialization.DataResult.error(
                                                        () -> "unknown carrier " + name);
                                            }
                                        },
                                        Carrier::name)
                                .fieldOf("carrier")
                                .forGetter(Tissue::carrier),
                        Codec.intRange(1, 63).listOf(1, 2).fieldOf("passages").forGetter(Tissue::passages))
                .apply(i, Tissue::new));
    }

    private record Observed(BlockPos pos, Tissue tissue) {
        static final Codec<Observed> CODEC = RecordCodecBuilder.create(i -> i.group(
                        BlockPos.CODEC.fieldOf("pos").forGetter(Observed::pos),
                        Tissue.CODEC.fieldOf("tissue").forGetter(Observed::tissue))
                .apply(i, Observed::new));
    }

    private final Map<BlockPos, Tissue> known = new HashMap<>();
    private final Map<Long, Set<Node>> watchers = new HashMap<>();
    private final NetworkIndex<Node> graph = new NetworkIndex<>(MAX_CELLS, MAX_KNOWN, 256);
    private Optional<Tag> unreadable = Optional.empty();
    private int watched;
    private boolean observationRefused;
    private final Set<Node> frontierNodes = new HashSet<>();
    private final Map<NetworkIndex.Component<Node>, Long> frontierRetries = new IdentityHashMap<>();
    private final Set<Long> loading = new HashSet<>();
    private final Map<NetworkIndex.Component<Node>, Integer> turns = new IdentityHashMap<>();

    int turn(NetworkIndex.Component<Node> component) {
        return turns.getOrDefault(component, 0);
    }

    void turn(NetworkIndex.Component<Node> component, int value) {
        if (turns.size() >= 256) turns.clear();
        turns.put(component, value);
    }

    record Port(BlockPos pos, Direction side) {}

    private final Map<Node, List<Port>> boundaries = new HashMap<>();
    private final Map<NetworkIndex.Component<Node>, List<Port>> endpoints = new IdentityHashMap<>();

    List<Port> endpoints(NetworkIndex.Component<Node> component) {
        List<Port> ports = endpoints.get(component);
        if (ports != null) return ports;
        if (endpoints.size() >= 256) endpoints.clear();
        List<Port> result = new ArrayList<>();
        Set<Port> seen = new HashSet<>();
        for (Node node : component.nodes())
            for (Port port : boundaries.getOrDefault(node, List.of())) if (seen.add(port)) result.add(port);
        ports = List.copyOf(result);
        endpoints.put(component, ports);
        return ports;
    }

    static NetworkTopology get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    static int bit(Direction side) {
        return 1 << side.ordinal();
    }

    static Optional<Tissue> tissue(BlockState state) {
        if (state.getBlock() instanceof MouthBlock)
            return Optional.of(new Tissue(Carrier.ITEM, List.of(63 ^ bit(Direction.UP))));
        Carrier carrier;
        if (state.getBlock() instanceof VeinBlock vein) {
            if (!state.getValue(VeinBlock.SEALED)) return Optional.empty();
            return Optional.of(new Tissue(vein.carrier(), List.of(63)));
        }
        if (!Routes.isRoute(state)) return Optional.empty();
        carrier = state.getValue(Routes.CARRIER);
        if (state.getBlock() instanceof SeptumCrossingBlock) {
            List<Integer> passages = new ArrayList<>();
            for (Direction.Axis axis : Direction.Axis.values()) {
                if (!SeptumCrossingBlock.passes(state, axis)) continue;
                int mask = 0;
                for (Direction side : Direction.values()) if (side.getAxis() == axis) mask |= bit(side);
                passages.add(mask);
            }
            return Optional.of(new Tissue(carrier, List.copyOf(passages)));
        }
        int mask = 0;
        for (Direction side : VascularStitchBlock.ends(state)) mask |= bit(side);
        return Optional.of(new Tissue(carrier, List.of(mask)));
    }

    private Optional<Tissue> observe(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return Optional.ofNullable(known.get(pos));
        Optional<Tissue> current = tissue(level.getBlockState(pos));
        if (current.isEmpty()) {
            if (known.remove(pos) != null) setDirty();
        } else if (!current.get().equals(known.get(pos))) {
            if (!known.containsKey(pos) && known.size() == MAX_KNOWN) {
                observationRefused = true;
                return Optional.empty();
            }
            known.put(pos.immutable(), current.get());
            setDirty();
        }
        return current;
    }

    Optional<NetworkIndex.Component<Node>> network(ServerLevel level, BlockPos pos, Direction entry) {
        refreshLoaded(level);
        if (unreadable.isPresent() || !level.hasChunkAt(pos)) return Optional.empty();
        Optional<Tissue> tissue = observe(level, pos);
        if (tissue.isEmpty()) return Optional.empty();
        for (int faces : tissue.get().passages()) {
            if (entry != null && (faces & bit(entry)) == 0) continue;
            Node node = new Node(pos.immutable(), tissue.get().carrier(), faces);
            Optional<NetworkIndex.Component<Node>> result = graph.request(node);
            if (result.isPresent()
                    && (result.get().status() == NetworkIndex.Status.UNAVAILABLE
                            || result.get().nodes().stream().anyMatch(frontierNodes::contains))) {
                if (frontierRetries.size() >= 256) frontierRetries.clear();
                long retry = frontierRetries.computeIfAbsent(result.get(), ignored -> level.getGameTime() + 20);
                if (level.getGameTime() >= retry) {
                    graph.invalidate(node);
                    result = graph.request(node);
                }
            }
            if (result.isEmpty()) {
                NetworkWork.discovered(
                        level, graph.advance(NetworkWork.discovery(level, 64), next -> links(level, next)));
                result = graph.request(node);
            }
            return result;
        }
        return Optional.empty();
    }

    private NetworkIndex.Links<Node> links(ServerLevel level, Node node) {
        observationRefused = false;
        Optional<Tissue> here = observe(level, node.pos());
        if (here.isEmpty()
                || here.get().carrier() != node.carrier()
                || !here.get().passages().contains(node.faces())) return new NetworkIndex.Links<>(false, List.of());
        watch(node.pos(), node);
        frontierNodes.remove(node);
        List<Node> links = new ArrayList<>();
        List<Port> ports = new ArrayList<>();
        for (Direction side : Direction.values()) {
            if ((node.faces() & bit(side)) == 0) continue;
            BlockPos next = node.pos().relative(side);
            watch(next, node);
            if (!level.hasChunkAt(next) && !known.containsKey(next)) frontierNodes.add(node);
            Optional<Tissue> other = observe(level, next);
            if (other.isEmpty()) {
                if (node.carrier().isFluid()
                        && level.hasChunkAt(next)
                        && level.getCapability(Capabilities.FluidHandler.BLOCK, next, side.getOpposite()) != null)
                    ports.add(new Port(next, side.getOpposite()));
                continue;
            }
            if (other.get().carrier() != node.carrier()) continue;
            for (int faces : other.get().passages())
                if ((faces & bit(side.getOpposite())) != 0) links.add(new Node(next, node.carrier(), faces));
        }
        boundaries.put(node, List.copyOf(ports));
        return new NetworkIndex.Links<>(!observationRefused, links);
    }

    private void watch(BlockPos pos, Node node) {
        Set<Node> nodes = watchers.computeIfAbsent(ChunkPos.asLong(pos), ignored -> new HashSet<>());
        if (nodes.add(node)) watched++;
    }

    void advance(ServerLevel level) {
        refreshLoaded(level);
        if (unreadable.isPresent()) return;
        // Watch entries have the same finite lifecycle as the discovery cache.
        if (watched > MAX_KNOWN * 4 || boundaries.size() > MAX_KNOWN) {
            graph.clear();
            watchers.clear();
            boundaries.clear();
            endpoints.clear();
            turns.clear();
            frontierNodes.clear();
            frontierRetries.clear();
            watched = 0;
        }
        int budget = NetworkWork.discovery(level, 2048);
        NetworkWork.discovered(level, graph.advance(budget, node -> links(level, node)));
    }

    /** No world reads: safe even before the loaded chunk has reached FULL status. */
    void chunkLoaded(ChunkPos chunk) {
        Set<Node> nodes = watchers.get(chunk.toLong());
        if (nodes != null) for (Node node : nodes) graph.invalidate(node);
        if (loading.size() >= 256) {
            graph.clear();
            loading.clear();
        }
        loading.add(chunk.toLong());
    }

    private void refreshLoaded(ServerLevel level) {
        java.util.Iterator<Long> it = loading.iterator();
        while (it.hasNext()) {
            long chunk = it.next();
            if (!level.hasChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk))) continue;
            Set<Node> nodes = watchers.get(chunk);
            if (nodes != null) for (Node node : nodes) graph.invalidate(node);
            it.remove();
        }
    }

    void changed(BlockPos pos) {
        if (known.remove(pos) != null) setDirty();
        invalidateAt(pos);
        for (Direction side : Direction.values()) invalidateAt(pos.relative(side));
    }

    private void invalidateAt(BlockPos pos) {
        // Every possible route passage, including a state that has just been removed.
        for (Carrier carrier : Carrier.values()) {
            graph.invalidate(new Node(pos, carrier, 63));
            graph.invalidate(new Node(pos, carrier, 63 ^ bit(Direction.UP)));
            for (Direction a : Direction.values())
                for (Direction b : Direction.values())
                    if (a.ordinal() < b.ordinal()) graph.invalidate(new Node(pos, carrier, bit(a) | bit(b)));
        }
    }

    static NetworkTopology load(CompoundTag tag, HolderLookup.Provider registries) {
        NetworkTopology data = new NetworkTopology();
        Tag entries = tag.get("connections");
        if (entries == null) return data;
        Observed.CODEC
                .listOf(0, MAX_KNOWN)
                .parse(NbtOps.INSTANCE, entries)
                .result()
                .ifPresentOrElse(
                        list -> list.forEach(entry -> data.known.put(entry.pos(), entry.tissue())),
                        () -> data.unreadable = Optional.of(entries.copy()));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (unreadable.isPresent()) tag.put("connections", unreadable.get().copy());
        else
            tag.put(
                    "connections",
                    Observed.CODEC
                            .listOf(0, MAX_KNOWN)
                            .encodeStart(
                                    NbtOps.INSTANCE,
                                    known.entrySet().stream()
                                            .map(e -> new Observed(e.getKey(), e.getValue()))
                                            .toList())
                            .getOrThrow());
        return tag;
    }
}
