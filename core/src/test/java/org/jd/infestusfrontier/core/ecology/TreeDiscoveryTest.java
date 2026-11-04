package org.jd.infestusfrontier.core.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jd.infestusfrontier.core.ecology.TreeDiscovery.Admitted;
import org.jd.infestusfrontier.core.ecology.TreeDiscovery.Block;
import org.jd.infestusfrontier.core.ecology.TreeDiscovery.Limits;
import org.jd.infestusfrontier.core.ecology.TreeDiscovery.Position;
import org.jd.infestusfrontier.core.ecology.TreeDiscovery.Reason;
import org.jd.infestusfrontier.core.ecology.TreeDiscovery.Refused;
import org.junit.jupiter.api.Test;

class TreeDiscoveryTest {
    private static final Position ROOT = new Position(0, 0, 0);

    /** A world of blocks keyed by position; everything else is nothing. Counts the reads it answers. */
    private static final class World implements TreeDiscovery.Terrain {
        final Map<Position, Block> blocks = new HashMap<>();
        final Set<Position> unloaded = new HashSet<>();
        int reads;

        World log(int x, int y, int z) {
            blocks.put(new Position(x, y, z), new TreeDiscovery.Log());
            return this;
        }

        World leaf(int x, int y, int z, int distance) {
            blocks.put(new Position(x, y, z), new TreeDiscovery.Leaf(distance));
            return this;
        }

        World foreign(int x, int y, int z) {
            blocks.put(new Position(x, y, z), new TreeDiscovery.ForeignLog());
            return this;
        }

        @Override
        public Block at(Position position) {
            reads++;
            if (unloaded.contains(position)) return new TreeDiscovery.Unloaded();
            return blocks.getOrDefault(position, new TreeDiscovery.Nothing());
        }
    }

    /** A three-log trunk whose top holds a ring of leaves at distance 1 and a second ring at distance 2. */
    private static World oak() {
        World world = new World().log(0, 0, 0).log(0, 1, 0).log(0, 2, 0);
        world.leaf(1, 2, 0, 1)
                .leaf(-1, 2, 0, 1)
                .leaf(0, 2, 1, 1)
                .leaf(0, 2, -1, 1)
                .leaf(0, 3, 0, 1);
        world.leaf(2, 2, 0, 2).leaf(1, 2, 1, 2).leaf(1, 3, 0, 2);
        return world;
    }

    private static TreeDiscovery.Outcome run(World world, Limits limits) {
        TreeDiscovery discovery = new TreeDiscovery(ROOT, limits);
        int rounds = 0;
        while (!discovery.done()) {
            discovery.advance(7, world);
            if (++rounds > 10_000) throw new AssertionError("discovery never ends");
        }
        return discovery.outcome().orElseThrow();
    }

    @Test
    void admitsATrunkWithTheLeavesItHolds() {
        World world = oak();
        world.leaf(3, 2, 0, 3); // beyond the second ring: distance 3 from the trunk, consistent, claimed
        world.leaf(5, 0, 0, 1); // a stray leaf by no log of ours
        Admitted tree = assertInstanceOf(Admitted.class, run(world, Limits.DEFAULT));
        assertEquals(List.of(ROOT, new Position(0, 1, 0), new Position(0, 2, 0)), tree.logs(), "root first, up");
        assertEquals(9, tree.leaves().size(), "the two rings and the far leaf");
        assertFalse(tree.leaves().contains(new Position(5, 0, 0)), "a leaf held by nothing of ours is not claimed");
        assertEquals(new Position(3, 2, 0), tree.leaves().get(8), "leaves come by rising distance");
    }

    @Test
    void leavesNeverLeadIntoAnotherTree() {
        World world = oak();
        // another trunk two blocks east of the crown: its leaf at distance 1 touches our leaf at distance 2
        world.log(3, 2, 0)
                .log(3, 1, 0)
                .log(3, 0, 0)
                .leaf(2, 2, 1, 1)
                .leaf(3, 3, 0, 1)
                .leaf(2, 3, 0, 2);
        Admitted tree = assertInstanceOf(Admitted.class, run(world, Limits.DEFAULT));
        assertEquals(3, tree.logs().size(), "only the logs joined to the root");
        assertFalse(tree.leaves().contains(new Position(2, 2, 1)), "the other tree's near leaf stays its own");
        assertFalse(tree.leaves().contains(new Position(3, 3, 0)), "a leaf over the other trunk stays too");
        assertTrue(tree.leaves().contains(new Position(2, 2, 0)), "our second ring is ours");
        // the leaf over the gap is two from their trunk and would be three from ours: it is theirs
        assertFalse(tree.leaves().contains(new Position(2, 3, 0)), "a leaf nearer the other trunk is not taken");
        // the leaf beside our crown is two from both trunks: the first tree to discover it takes it
        assertTrue(tree.leaves().contains(new Position(1, 2, 1)), "a leaf the same distance from both is taken");
    }

    @Test
    void theGradientIsJudgedFromEveryClaimedNeighbour() {
        // a distance-3 leaf first met from a distance-1 leaf is not claimed then, but is from the distance-2 leaf
        World world = new World()
                .log(0, 0, 0)
                .leaf(0, 1, 0, 1)
                .leaf(1, 1, 0, 2)
                .leaf(0, 2, 0, 3)
                .leaf(1, 2, 0, 3);
        Admitted tree = assertInstanceOf(Admitted.class, run(world, Limits.DEFAULT));
        assertTrue(tree.leaves().contains(new Position(1, 2, 0)), "claimed from the distance-2 leaf");
        assertFalse(tree.leaves().contains(new Position(0, 2, 0)), "a leaf two steps past its neighbour is not");
    }

    @Test
    void logsJoinThroughEdgesAndCornersAsBranchesDo() {
        World world =
                new World().log(0, 0, 0).log(1, 1, 0).log(2, 2, 1).log(4, 2, 1).leaf(2, 2, 2, 1);
        Admitted tree = assertInstanceOf(Admitted.class, run(world, Limits.DEFAULT));
        assertEquals(3, tree.logs().size(), "the diagonal branch, not the log two away");
        assertEquals(List.of(new Position(2, 2, 2)), tree.leaves(), "the leaf on the branch end");
    }

    @Test
    void aLeafNeedsAFaceAgainstItsLog() {
        World world = new World().log(0, 0, 0).leaf(1, 1, 0, 1).leaf(1, 0, 0, 1);
        Admitted tree = assertInstanceOf(Admitted.class, run(world, Limits.DEFAULT));
        assertEquals(List.of(new Position(1, 0, 0)), tree.leaves(), "the corner leaf is not held by this log");
    }

    @Test
    void refusesALogOfAnotherSpeciesJoinedToTheTree() {
        World world = oak().foreign(1, 1, 1);
        assertEquals(new Refused(Reason.AMBIGUOUS), run(world, Limits.DEFAULT));
    }

    @Test
    void refusesWhenAnyPositionIsUnloaded() {
        World world = oak();
        world.unloaded.add(new Position(1, 3, 1));
        assertEquals(new Refused(Reason.UNLOADED), run(world, Limits.DEFAULT));
        World rootless = new World();
        rootless.unloaded.add(ROOT);
        assertEquals(new Refused(Reason.UNLOADED), run(rootless, Limits.DEFAULT));
    }

    @Test
    void refusesARootThatIsNotALog() {
        assertEquals(new Refused(Reason.MISSING), run(new World().leaf(0, 0, 0, 1), Limits.DEFAULT));
        assertEquals(new Refused(Reason.MISSING), run(new World(), Limits.DEFAULT));
    }

    @Test
    void refusesWhatLeavesTheEnvelope() {
        World wide = new World();
        for (int x = 0; x <= 9; x++) wide.log(x, 0, 0);
        assertEquals(new Refused(Reason.OVERSIZED), run(wide, Limits.DEFAULT), "nine east is out of reach");
        World tall = new World();
        for (int y = 0; y <= 31; y++) tall.log(0, y, 0);
        assertEquals(new Refused(Reason.OVERSIZED), run(tall, Limits.DEFAULT), "32 logs up leave the 32 height");
        World deep = new World().log(0, 0, 0).log(0, -1, 0).log(0, -2, 0);
        assertEquals(new Refused(Reason.OVERSIZED), run(deep, Limits.DEFAULT), "two below the root is out");
        World fits = new World();
        for (int y = -1; y <= 30; y++) fits.log(0, y, 0);
        for (int x = -8; x <= 8; x++) fits.log(x, 5, 0);
        assertInstanceOf(Admitted.class, run(fits, new Limits(8, 32, 128, 512, 4096)), "the envelope itself fits");
    }

    @Test
    void refusesMoreLogsLeavesOrReadsThanTheLimits() {
        assertEquals(new Refused(Reason.OVERSIZED), run(oak(), new Limits(8, 32, 2, 512, 2048)), "three logs");
        assertEquals(new Refused(Reason.OVERSIZED), run(oak(), new Limits(8, 32, 128, 7, 2048)), "eight leaves");
        assertEquals(new Refused(Reason.OVERSIZED), run(oak(), new Limits(8, 32, 128, 512, 40)), "many reads");
        assertInstanceOf(Admitted.class, run(oak(), new Limits(8, 32, 3, 8, 2048)), "exactly the limits fit");
    }

    @Test
    void readsEveryPositionOnceAndNoMoreThanTheBudgetACall() {
        World world = oak();
        TreeDiscovery discovery = new TreeDiscovery(ROOT, Limits.DEFAULT);
        assertEquals(5, discovery.advance(5, world));
        assertEquals(5, discovery.examined());
        assertEquals(5, world.reads);
        assertFalse(discovery.done());
        assertTrue(discovery.claims(ROOT));
        int used = 5;
        while (!discovery.done()) used += discovery.advance(20, world);
        assertEquals(world.reads, used, "every read is counted");
        assertEquals(world.reads, discovery.examined());
        assertEquals(0, discovery.advance(20, world), "nothing after the outcome");
        // the trunk's 26-neighbourhoods and the leaves' faces overlap; a position is read once however often met
        Set<Position> distinct = new HashSet<>();
        for (int x = -3; x <= 3; x++)
            for (int y = -1; y <= 4; y++) for (int z = -3; z <= 3; z++) distinct.add(new Position(x, y, z));
        assertTrue(world.reads < distinct.size(), "fewer reads than the box: " + world.reads);
        assertTrue(world.reads >= 45, "but the whole 3x3x5 neighbourhood of the trunk: " + world.reads);
    }
}
