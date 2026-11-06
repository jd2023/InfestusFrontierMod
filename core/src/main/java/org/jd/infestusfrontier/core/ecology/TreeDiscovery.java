package org.jd.infestusfrontier.core.ecology;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bounded, incremental discovery of one rooted tree: the logs joined to a root log and the foliage those logs hold
 * (VEGETATION_INFECTION.md, "Tree boundaries"). A tree's logs are of one kind, the root's: logs of that kind join
 * through any of their 26 neighbours, as vanilla's diagonal branches and leaning trunks do. A leaf belongs to the tree
 * when it stands face to face with a log of the tree at leaf distance 1, or with a claimed leaf at one more than that
 * leaf's distance: the gradient vanilla's own distance rule writes. The kinds of leaf the trunk touches at distance 1
 * are the tree's foliage, at most {@link Limits#foliage()} of them; a gradient leaf of any other kind is another
 * tree's. Leaves never lead to logs, so contact between crowns never carries discovery into another tree, and a leaf
 * nearer another tree's log than this tree's keeps a lower distance than the gradient wants and is left alone.
 *
 * <p>Every position is read at most once through the {@link Terrain} and the reads are counted; {@link #advance}
 * does at most the reads it is given, so a server budget bounds a tick. The outcome is one of: the tree is admitted
 * with its logs and leaves in discovery order; it is refused as oversized (a log or leaf outside the envelope, more
 * logs, leaves or reads than the limits allow), ambiguous (a log of another kind joined to it, or more kinds of
 * foliage than the limits allow), unloaded (a position in no loaded chunk) or missing (the root is not a log).
 */
public final class TreeDiscovery {
    /** A block position; the caller maps it to the world. */
    public record Position(int x, int y, int z) {
        Position offset(int dx, int dy, int dz) {
            return new Position(x + dx, y + dy, z + dz);
        }
    }

    /** What stands at a position. A kind names a block; two parts are of one kind when their kinds are equal. */
    public sealed interface Block permits Log, Leaf, Nothing, Unloaded {}

    /** A log a tree may be made of. Joined to a tree of another kind, it makes that tree ambiguous. */
    public record Log(String kind) implements Block {}

    /** Leaves a tree may hold, with their leaf distance (1 to 7). */
    public record Leaf(String kind, int distance) implements Block {}

    /** Anything else: air, ground, planks, a stripped log, wood, an infected log or infected leaves. */
    public record Nothing() implements Block {}

    /** A position in no loaded chunk. */
    public record Unloaded() implements Block {}

    /** The block at a position. Called once per examined position. */
    public interface Terrain {
        Block at(Position position);
    }

    /**
     * The envelope and counts one tree may have: {@code reach} blocks each way from the root horizontally (a width of
     * {@code 2 * reach + 1}), {@code height} blocks from one below the root upward, and at most {@code logs} logs,
     * {@code leaves} leaves, {@code foliage} kinds of leaf and {@code examined} reads.
     */
    public record Limits(int reach, int height, int logs, int leaves, int foliage, int examined) {
        public Limits {
            if (reach < 0 || height < 2 || logs < 1 || leaves < 0 || foliage < 0 || examined < 1)
                throw new IllegalArgumentException("tree limits");
        }

        /** The first limits: a 17x32x17 envelope, 128 logs, 512 leaves of at most 4 kinds, 2,048 reads. */
        public static final Limits DEFAULT = new Limits(8, 32, 128, 512, 4, 2048);
    }

    public enum Reason {
        OVERSIZED,
        AMBIGUOUS,
        UNLOADED,
        MISSING
    }

    public sealed interface Outcome permits Admitted, Refused {}

    /** A claimed log or leaf and the kind it was when read. */
    public record Part(Position position, String kind) {}

    /** The tree in discovery order: the root first; leaves by rising distance. Every log is of the root's kind. */
    public record Admitted(List<Part> logs, List<Part> leaves) implements Outcome {
        public Admitted {
            logs = List.copyOf(logs);
            leaves = List.copyOf(leaves);
        }
    }

    public record Refused(Reason reason) implements Outcome {}

    private record ClaimedLeaf(Position position, int distance) {}

    /** The six face neighbours first, so a face test is an index test, then the twelve edges and eight corners. */
    private static final int[][] OFFSETS = offsets();

    private static final int FACES = 6;

    private final Position root;
    private final Limits limits;
    private final Set<Position> claimed = new HashSet<>();
    private final Map<Position, Block> read = new HashMap<>();
    private final ArrayDeque<Position> logQueue = new ArrayDeque<>();
    private final ArrayDeque<ClaimedLeaf> leafQueue = new ArrayDeque<>();
    private final List<Part> logs = new ArrayList<>();
    private final List<Part> leaves = new ArrayList<>();
    private final Set<String> foliage = new HashSet<>();
    private String kind = "";
    private int examined;
    private boolean rootRead;
    private Optional<Position> expanding = Optional.empty();
    private int expandingDistance;
    private int nextOffset;
    private Optional<Outcome> outcome = Optional.empty();

    public TreeDiscovery(Position root, Limits limits) {
        this.root = root;
        this.limits = limits;
    }

    public Position root() {
        return root;
    }

    /** Positions read so far. */
    public int examined() {
        return examined;
    }

    /** True for a log or leaf discovered so far, admitted or not. */
    public boolean claims(Position position) {
        return claimed.contains(position);
    }

    public Optional<Outcome> outcome() {
        return outcome;
    }

    public boolean done() {
        return outcome.isPresent();
    }

    /** Reads at most {@code budget} positions and returns how many it read. Does nothing once done. */
    public int advance(int budget, Terrain terrain) {
        int used = 0;
        while (used < budget && outcome.isEmpty()) {
            if (!rootRead) {
                rootRead = true;
                used++;
                examined++;
                switch (terrain.at(root)) {
                    case Log l -> {
                        kind = l.kind();
                        claimLog(root);
                    }
                    case Unloaded u -> refuse(Reason.UNLOADED);
                    case Leaf f -> refuse(Reason.MISSING);
                    case Nothing n -> refuse(Reason.MISSING);
                }
                continue;
            }
            if (expanding.isEmpty() && !takeNext()) {
                outcome = Optional.of(new Admitted(logs, leaves));
                break;
            }
            Position node = expanding.get();
            boolean fromLog = expandingDistance == 0;
            int index = nextOffset++;
            if (nextOffset == (fromLog ? OFFSETS.length : FACES)) expanding = Optional.empty();
            Position next = node.offset(OFFSETS[index][0], OFFSETS[index][1], OFFSETS[index][2]);
            if (claimed.contains(next)) continue;
            Block block = read.get(next);
            if (block == null) {
                if (examined == limits.examined()) {
                    refuse(Reason.OVERSIZED);
                    break;
                }
                used++;
                examined++;
                block = terrain.at(next);
                read.put(next, block);
            }
            switch (block) {
                case Unloaded u -> refuse(Reason.UNLOADED);
                case Nothing n -> {}
                case Log l -> {
                    if (!l.kind().equals(kind)) refuse(Reason.AMBIGUOUS);
                    else if (fromLog) claimLog(next);
                }
                case Leaf f -> {
                    if (index < FACES && f.distance() == expandingDistance + 1) {
                        if (fromLog) holdFoliage(f.kind());
                        if (outcome.isEmpty() && foliage.contains(f.kind())) claimLeaf(next, f);
                    }
                }
            }
        }
        return used;
    }

    private boolean takeNext() {
        if (!logQueue.isEmpty()) {
            expanding = Optional.of(logQueue.poll());
            expandingDistance = 0;
        } else if (!leafQueue.isEmpty()) {
            ClaimedLeaf leaf = leafQueue.poll();
            expanding = Optional.of(leaf.position());
            expandingDistance = leaf.distance();
        } else {
            return false;
        }
        nextOffset = 0;
        return true;
    }

    private void claimLog(Position position) {
        if (!inEnvelope(position) || logs.size() == limits.logs()) {
            refuse(Reason.OVERSIZED);
            return;
        }
        claimed.add(position);
        read.remove(position);
        logs.add(new Part(position, kind));
        logQueue.add(position);
    }

    /** A kind of leaf the trunk touches at distance 1 is the tree's foliage, up to the limit. */
    private void holdFoliage(String leafKind) {
        if (foliage.contains(leafKind)) return;
        if (foliage.size() == limits.foliage()) refuse(Reason.AMBIGUOUS);
        else foliage.add(leafKind);
    }

    private void claimLeaf(Position position, Leaf leaf) {
        if (!inEnvelope(position) || leaves.size() == limits.leaves()) {
            refuse(Reason.OVERSIZED);
            return;
        }
        claimed.add(position);
        read.remove(position);
        leaves.add(new Part(position, leaf.kind()));
        leafQueue.add(new ClaimedLeaf(position, leaf.distance()));
    }

    private boolean inEnvelope(Position p) {
        return Math.abs(p.x() - root.x()) <= limits.reach()
                && Math.abs(p.z() - root.z()) <= limits.reach()
                && p.y() >= root.y() - 1
                && p.y() <= root.y() - 2 + limits.height();
    }

    private void refuse(Reason reason) {
        outcome = Optional.of(new Refused(reason));
    }

    private static int[][] offsets() {
        List<int[]> faces = new ArrayList<>();
        List<int[]> rest = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    int axes = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (axes == 1) faces.add(new int[] {dx, dy, dz});
                    else if (axes > 1) rest.add(new int[] {dx, dy, dz});
                }
        faces.addAll(rest);
        return faces.toArray(new int[0][]);
    }
}
