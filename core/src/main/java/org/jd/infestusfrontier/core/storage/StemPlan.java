package org.jd.infestusfrontier.core.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * The grown shape of one Capsule Stem (BLOCK_CATALOG T1-09): the stem cells and the pods a root has grown, as offsets
 * from the root, in the order they grew. A root plants as one cell and grows one step at a time; every step is drawn
 * from the plan's seed, so the same root in the same room grows the same tree, and a tree whose cells were broken is
 * regrown from the plan cell by cell.
 *
 * <p>The main trunk rises from the root. From tier one up, each tier carries two to four pods on its free sides,
 * never on a side the tier below used, so pods stagger round the stem. At a drawn tier of three to five the trunk
 * forks once: a branch cell grows out to one side and a second trunk rises from it. The shorter trunk grows first.
 * A tree stops at {@value #MAX_HEIGHT} tiers and {@value #MAX_PODS} pods.
 */
public record StemPlan(long seed, List<Cell> cells, List<Pod> pods) {
    /** Tiers above the root a trunk may reach. */
    public static final int MAX_HEIGHT = 6;

    public static final int MAX_PODS = 16;
    /** Pods a tier wants, drawn from this many choices from {@value #LEAST_PODS}. */
    public static final int LEAST_PODS = 2;

    public static final int POD_CHOICES = 3;
    /** The first tier a fork may grow from, drawn from this many choices. */
    public static final int FIRST_FORK = 3;

    public static final int FORK_CHOICES = 3;
    public static final int SIDES = 4;
    /** Side s runs along ({@code DX[s]}, {@code DZ[s]}): north, east, south, west. */
    public static final int[] DX = {0, 1, 0, -1};

    public static final int[] DZ = {-1, 0, 1, 0};

    public enum Part {
        ROOT,
        TRUNK,
        BRANCH
    }

    /**
     * One stem cell at an offset from the root. {@code parent} is the index of the cell it grew from, -1 for the root;
     * {@code side} the side a branch grew out to, -1 otherwise; {@code trunk} 0 for the main trunk, 1 for the fork;
     * {@code tier} its height in its trunk, the branch cell being tier 0 of the fork.
     */
    public record Cell(int x, int y, int z, Part part, int side, int parent, int trunk, int tier) {
        public boolean at(int x, int y, int z) {
            return this.x == x && this.y == y && this.z == z;
        }
    }

    /** One pod at an offset from the root, hanging on the side {@code side} of the cell at index {@code cell}. */
    public record Pod(int x, int y, int z, int side, int cell) {
        public boolean at(int x, int y, int z) {
            return this.x == x && this.y == y && this.z == z;
        }
    }

    /** Whether an offset from the root may be grown into. */
    @FunctionalInterface
    public interface Free {
        boolean test(int x, int y, int z);
    }

    /** One growth: a stem cell or a pod. */
    public sealed interface Step {
        record Grow(Cell cell) implements Step {}

        record Sprout(Pod pod) implements Step {}
    }

    public StemPlan {
        cells = List.copyOf(cells);
        pods = List.copyOf(pods);
        if (cells.isEmpty() || cells.getFirst().part() != Part.ROOT) throw new IllegalArgumentException("no root");
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            boolean rooted = i == 0 ? cell.parent() == -1 && cell.at(0, 0, 0) : cell.parent() >= 0 && cell.parent() < i;
            if (!rooted || (i > 0 && cell.part() == Part.ROOT))
                throw new IllegalArgumentException("cell " + i + " grew from nowhere");
            if (cell.side() < -1 || cell.side() >= SIDES) throw new IllegalArgumentException("cell " + i + " side");
        }
        for (Pod pod : pods) {
            if (pod.cell() < 0 || pod.cell() >= cells.size()) throw new IllegalArgumentException("pod on no cell");
            if (pod.side() < 0 || pod.side() >= SIDES) throw new IllegalArgumentException("pod side");
        }
    }

    /** A freshly planted root. */
    public static StemPlan planted(long seed) {
        return new StemPlan(seed, List.of(new Cell(0, 0, 0, Part.ROOT, -1, -1, 0, 0)), List.of());
    }

    public Optional<Cell> cellAt(int x, int y, int z) {
        for (Cell cell : cells) if (cell.at(x, y, z)) return Optional.of(cell);
        return Optional.empty();
    }

    public Optional<Pod> podAt(int x, int y, int z) {
        for (Pod pod : pods) if (pod.at(x, y, z)) return Optional.of(pod);
        return Optional.empty();
    }

    public int indexOf(Cell cell) {
        return cells.indexOf(cell);
    }

    private boolean planned(int x, int y, int z) {
        return cellAt(x, y, z).isPresent() || podAt(x, y, z).isPresent();
    }

    /** True in the column a trunk may still rise through: no pod grows in a trunk's way. */
    private boolean reserved(int x, int y, int z) {
        for (int trunk = 0; trunk < 2; trunk++) {
            Optional<Cell> top = top(trunk);
            if (top.isEmpty()) continue;
            if (top.get().x() == x && top.get().z() == z && y > top.get().y() && y <= MAX_HEIGHT) return true;
        }
        return false;
    }

    /** The highest cell of a trunk, or empty while the trunk has not forked yet. */
    public Optional<Cell> top(int trunk) {
        Cell top = null;
        for (Cell cell : cells) if (cell.trunk() == trunk && (top == null || cell.tier() > top.tier())) top = cell;
        return Optional.ofNullable(top);
    }

    private int tiers(int trunk) {
        return top(trunk).map(cell -> cell.tier() + 1).orElse(0);
    }

    private boolean forked() {
        return top(1).isPresent();
    }

    private Random draw(int trunk, int tier, int salt) {
        return new Random(seed * 31 + trunk * 1_000_003L + tier * 7_919L + salt * 104_729L);
    }

    private int[] order(Random random) {
        int[] sides = {0, 1, 2, 3};
        for (int i = sides.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = sides[i];
            sides[i] = sides[j];
            sides[j] = swap;
        }
        return sides;
    }

    /** The sides of the pods hanging on the cell at index {@code cell}. */
    private boolean[] sidesUsed(int cell) {
        boolean[] used = new boolean[SIDES];
        for (Pod pod : pods) if (pod.cell() == cell) used[pod.side()] = true;
        return used;
    }

    private int podsOn(int cell) {
        int count = 0;
        for (Pod pod : pods) if (pod.cell() == cell) count++;
        return count;
    }

    /** The pod the top of {@code trunk} wants next, if its tier still wants one and a side has room. */
    private Optional<Step> sprout(Cell top, Free free) {
        if (top.part() == Part.BRANCH || top.tier() < 1 || pods.size() >= MAX_PODS) return Optional.empty();
        int index = indexOf(top);
        int quota = LEAST_PODS + draw(top.trunk(), top.tier(), 0).nextInt(POD_CHOICES);
        if (podsOn(index) >= quota) return Optional.empty();
        boolean[] used = sidesUsed(index);
        boolean[] barred = top.parent() >= 0 ? sidesUsed(top.parent()) : new boolean[SIDES];
        for (int side : order(draw(top.trunk(), top.tier(), 1))) {
            int x = top.x() + DX[side], z = top.z() + DZ[side];
            if (used[side] || barred[side] || planned(x, top.y(), z) || reserved(x, top.y(), z)) continue;
            if (!free.test(x, top.y(), z)) continue;
            return Optional.of(new Step.Sprout(new Pod(x, top.y(), z, side, index)));
        }
        return Optional.empty();
    }

    /** The fork off the main trunk's top, once it is high enough and a side has room for the branch and its rise. */
    private Optional<Step> fork(Cell top, Free free) {
        if (top.trunk() != 0 || forked()) return Optional.empty();
        int forkTier = FIRST_FORK + draw(0, 0, 2).nextInt(FORK_CHOICES);
        if (top.tier() < forkTier) return Optional.empty();
        boolean[] used = sidesUsed(indexOf(top));
        for (int side : order(draw(0, 0, 3))) {
            int x = top.x() + DX[side], z = top.z() + DZ[side];
            if (used[side] || planned(x, top.y(), z) || planned(x, top.y() + 1, z)) continue;
            if (!free.test(x, top.y(), z) || !free.test(x, top.y() + 1, z)) continue;
            return Optional.of(new Step.Grow(new Cell(x, top.y(), z, Part.BRANCH, side, indexOf(top), 1, 0)));
        }
        return Optional.empty();
    }

    private Optional<Step> rise(Cell top, Free free) {
        if (top.y() >= MAX_HEIGHT || planned(top.x(), top.y() + 1, top.z())) return Optional.empty();
        if (!free.test(top.x(), top.y() + 1, top.z())) return Optional.empty();
        return Optional.of(new Step.Grow(
                new Cell(top.x(), top.y() + 1, top.z(), Part.TRUNK, -1, indexOf(top), top.trunk(), top.tier() + 1)));
    }

    /**
     * The next growth, or empty when the tree is full or nothing fits: the shorter trunk first, its top tier's next pod,
     * else the fork, else one cell up; then the other trunk the same way.
     */
    public Optional<Step> next(Free free) {
        if (pods.size() >= MAX_PODS) return Optional.empty();
        int[] trunks = forked() && tiers(1) < tiers(0) ? new int[] {1, 0} : new int[] {0, 1};
        for (int trunk : trunks) {
            Optional<Cell> top = top(trunk);
            if (top.isEmpty()) continue;
            Optional<Step> step =
                    sprout(top.get(), free).or(() -> fork(top.get(), free)).or(() -> rise(top.get(), free));
            if (step.isPresent()) return step;
        }
        return Optional.empty();
    }

    /** The plan with {@code step} grown. */
    public StemPlan with(Step step) {
        List<Cell> grownCells = new ArrayList<>(cells);
        List<Pod> grownPods = new ArrayList<>(pods);
        switch (step) {
            case Step.Grow grow -> grownCells.add(grow.cell());
            case Step.Sprout sprout -> grownPods.add(sprout.pod());
        }
        return new StemPlan(seed, grownCells, grownPods);
    }

    /**
     * A pod pressed by hand on side {@code side} of the cell at index {@code cell}, kept in the plan so it is grown
     * again when lost; the plan is unchanged when a pod is already planned there, or a cell is, as where a branch
     * stood.
     */
    public StemPlan adopt(int cell, int side) {
        Cell on = cells.get(cell);
        int x = on.x() + DX[side], z = on.z() + DZ[side];
        if (planned(x, on.y(), z)) return this;
        return with(new Step.Sprout(new Pod(x, on.y(), z, side, cell)));
    }
}
