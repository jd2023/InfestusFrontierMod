package org.jd.infestusfrontier.testmod;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.IOUtilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.BlightedLeavesBlock;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.InfectedLogBlock;
import org.jd.infestusfrontier.ecology.LivingSubstrateBlock;
import org.jd.infestusfrontier.ecology.SubstrateStage;
import org.jd.infestusfrontier.ecology.api.TreeInfectionSnapshot;

/**
 * Standing trees infected by substrate growth: VEGETATION_INFECTION.md, "Trees". Trees here are built as the demo
 * builds them: a trunk on grass, leaf layers at the vanilla distances. Each test clears what it built in the far yard
 * it used, so the saved test world holds no tree the next run could find.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class TreeInfectionTests {
    /**
     * One batch a test, so they run one after another: tests of a batch run together, and these share one dimension's
     * job list, the server's budgets and the far chunks they force and release.
     */
    private static final String BATCH = "treeInfection";

    private static final int JOBS = 16;
    private static final int REPLACEMENTS_PER_TICK = 16;
    private static final int STEP = LivingSubstrateBlock.GROWTH_DELAY_TICKS;
    /** The far yard: unloaded until a test forces it, 64 blocks along z per slot. */
    private static final int YARD_X = 230000;

    private static final int YARD_Z = 230000;
    private static final int YARD_Y = 80;

    /** A tree as placed: its log and leaf blocks, the trunk and the leaves with their distances. */
    private record Tree(Block logBlock, Block leafBlock, List<BlockPos> logs, Map<BlockPos, Integer> leaves) {
        int size() {
            return logs.size() + leaves.size();
        }
    }

    // --- one tree ---

    /** Culture on the ground beside a trunk base turns the whole oak: logs keep their axis, leaves their distance. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH + "1", timeoutTicks = 200)
    public static void cultureBesideTheTrunkTurnsTheWholeTreeAndDropsNothing(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        Tree oak = oak(level, h.absolutePos(new BlockPos(7, 2, 7)));
        Player gardener = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        EcologyTests.useCulture(level, gardener, h.absolutePos(new BlockPos(6, 1, 7)));
        h.assertValueEqual(gardener.getMainHandItem().getCount(), 0, "the culture converted the ground");
        EcologyTests.assertStage(h, new BlockPos(6, 1, 7), SubstrateStage.BASIC);
        h.assertTrue(snapshot(h).works(oak.logs().getFirst()), "a job on the oak's root");
        h.startSequence()
                .thenWaitUntil(() -> assertConverted(h, oak))
                .thenExecute(() -> {
                    h.assertFalse(snapshot(h).works(oak.logs().getFirst()), "the job is done and gone");
                    for (TreeInfectionSnapshot.Refusal refusal : snapshot(h).refusals())
                        h.assertFalse(refusal.root().equals(oak.logs().getFirst()), "refused: " + refusal);
                    assertNothingSpawned(h);
                })
                .thenIdle(5)
                .thenExecute(() -> assertNothingSpawned(h))
                .thenSucceed();
    }

    /** A bone-meal wave that reaches the trunk two cells on turns the tree, and converts the ground as before. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH + "2", timeoutTicks = 300)
    public static void theWaveReachingATrunkTurnsIt(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        // a corridor: sandstone each side, so the wave walks from the cell at 4 over 5 to 6 and touches the trunk at 7
        for (int x = 3; x <= 6; x++) {
            h.setBlock(new BlockPos(x, 1, 6), Blocks.SANDSTONE);
            h.setBlock(new BlockPos(x, 1, 8), Blocks.SANDSTONE);
        }
        h.setBlock(new BlockPos(3, 1, 7), Blocks.SANDSTONE);
        h.setBlock(new BlockPos(4, 1, 7), EcologyModule.LIVING_SUBSTRATE.get());
        Tree oak = oak(level, h.absolutePos(new BlockPos(7, 2, 7)));
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 1);
        EcologyTests.use(h, player, new BlockPos(4, 1, 7), Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 0, "the bone meal was used");
        h.startSequence()
                .thenExecuteAfter(STEP + 2, () -> {
                    EcologyTests.assertStage(h, new BlockPos(5, 1, 7), SubstrateStage.BASIC);
                    h.assertFalse(snapshot(h).works(oak.logs().getFirst()), "the wave has not reached the trunk yet");
                    assertUntouched(h, oak);
                })
                .thenExecuteAfter(STEP, () -> {
                    EcologyTests.assertStage(h, new BlockPos(6, 1, 7), SubstrateStage.BASIC);
                    h.assertTrue(snapshot(h).works(oak.logs().getFirst()), "the cell beside the trunk touched it");
                })
                .thenWaitUntil(() -> assertConverted(h, oak))
                .thenExecute(() -> assertNothingSpawned(h))
                .thenSucceed();
    }

    /**
     * Only an upright trunk log on host ground is a root. A lying log, a log on sand, planks and an Infected Log start
     * nothing; nor do the logs that the log tags hold beside trunks: stripped logs (vanilla's and another mod's, by the
     * common tag), wood (vanilla's and another mod's, in {@code logs_that_burn} only), the mangrove's, a log a pack
     * excludes, a thin branch and a log with a block entity. Ground under any of them is still no host.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH + "3", timeoutTicks = 100)
    public static void onlyAnUprightRootedTrunkLogStartsAJob(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        List<BlockState> notRoots = List.of(
                Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X),
                Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                Blocks.OAK_WOOD.defaultBlockState(),
                Blocks.OAK_PLANKS.defaultBlockState(),
                EcologyModule.INFECTED_LOG.get().defaultBlockState(),
                Blocks.MANGROVE_LOG.defaultBlockState(),
                TestTrees.WOOD.get().defaultBlockState(),
                TestTrees.STRIPPED_LOG.get().defaultBlockState(),
                TestTrees.EXCLUDED_LOG.get().defaultBlockState(),
                TestTrees.BRANCH.get().defaultBlockState(),
                TestTrees.ENTITY_LOG.get().defaultBlockState());
        Player gardener =
                EcologyTests.holding(h, GameType.CREATIVE, EcologyModule.SPORE_CULTURE.get(), notRoots.size() + 1);
        for (int i = 0; i < notRoots.size(); i++) {
            BlockPos trunk = notRoot(i);
            h.setBlock(trunk, notRoots.get(i));
            h.setBlock(trunk.above(), notRoots.get(i));
            EcologyTests.useCulture(level, gardener, h.absolutePos(trunk.below().north()));
            EcologyTests.assertStage(h, trunk.below().north(), SubstrateStage.BASIC);
            h.assertValueEqual(h.getBlockState(trunk), notRoots.get(i), "stands as it was: " + notRoots.get(i));
        }
        // an upright oak log on sandstone is not rooted
        h.setBlock(new BlockPos(2, 1, 13), Blocks.SANDSTONE);
        h.setBlock(new BlockPos(2, 2, 13), Blocks.OAK_LOG);
        EcologyTests.useCulture(level, gardener, h.absolutePos(new BlockPos(1, 1, 13)));
        h.runAfterDelay(10, () -> {
            for (TreeInfectionSnapshot.Job job : snapshot(h).jobs())
                h.assertFalse(h.getBounds().contains(job.root().getCenter()), "a job on a trunk here: " + job);
            for (TreeInfectionSnapshot.Refusal refusal : snapshot(h).refusals())
                h.assertFalse(h.getBounds().contains(refusal.root().getCenter()), "a refusal here: " + refusal);
            h.assertBlockPresent(Blocks.OAK_LOG, new BlockPos(2, 2, 13));
            h.assertBlockPresent(Blocks.SANDSTONE, new BlockPos(2, 1, 13));
            for (int i = 0; i < notRoots.size(); i++) {
                BlockPos trunk = notRoot(i);
                h.assertValueEqual(h.getBlockState(trunk), notRoots.get(i), "still stands: " + notRoots.get(i));
                h.assertBlockPresent(Blocks.GRASS_BLOCK, trunk.below());
            }
            h.succeed();
        });
    }

    /** Where the {@code i}th look-alike stands in that test: six a row, two rows. */
    private static BlockPos notRoot(int i) {
        return new BlockPos(2 + 2 * (i % 6), 2, 4 + 5 * (i / 6));
    }

    /**
     * Any tree whose trunk the tags name is a root: a cherry, which no species list held, a log of another mod in
     * {@code overworld_natural_logs} and a log a pack adds to {@code infestusfrontier:infectable_logs}. Each turns
     * into the same Infected Log and Blighted Leaves as an oak.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = BATCH + "15",
            timeoutTicks = 300)
    public static void anyTreeTheTagsNameTurnsIntoTheSameTimber(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Yard yard = new Yard(h, 4, 48, 16);
        List<Tree> trees = List.of(
                tree(level, yard.at(6, 1, 8), Blocks.CHERRY_LOG, Blocks.CHERRY_LEAVES, 5, List.of(2, 3), 4),
                tree(level, yard.at(20, 1, 8), TestTrees.LOG.get(), TestTrees.LEAVES.get(), 5, List.of(2, 3), 4),
                tree(level, yard.at(34, 1, 8), TestTrees.INCLUDED_LOG.get(), Blocks.OAK_LEAVES, 5, List.of(2, 3), 4));
        Player gardener = EcologyTests.holding(h, GameType.CREATIVE, EcologyModule.SPORE_CULTURE.get(), trees.size());
        for (Tree tree : trees) {
            EcologyTests.useCulture(
                    level, gardener, tree.logs().getFirst().below().west());
            h.assertValueEqual(
                    job(h, tree.logs().getFirst()).log(),
                    BuiltInRegistries.BLOCK.getKey(tree.logBlock()).toString(),
                    "a job on the tree of " + tree.logBlock());
        }
        h.startSequence()
                .thenWaitUntil(() -> {
                    for (Tree tree : trees) assertConverted(h, tree);
                })
                .thenWaitUntil(() -> {
                    for (Tree tree : trees)
                        h.assertFalse(snapshot(h).works(tree.logs().getFirst()), "jobs done");
                })
                .thenExecute(() -> {
                    for (Tree tree : trees)
                        h.assertTrue(
                                level.getEntitiesOfClass(
                                                ItemEntity.class,
                                                new AABB(tree.logs().getFirst()).inflate(6))
                                        .isEmpty(),
                                "nothing dropped by " + tree.logBlock());
                    yard.clear(h);
                })
                .thenSucceed();
    }

    /**
     * An oak and another mod's tree stand so close that their crowns share a column: the shared leaves are the other
     * tree's, at the distance the oak's gradient wants. The cell by the oak turns the oak and leaves the other tree
     * whole; a cell by the other tree then turns it.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = BATCH + "16",
            timeoutTicks = 300)
    public static void touchingCrownsOfTwoKindsOfTreeStayApart(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        Tree grown = oak(level, h.absolutePos(new BlockPos(4, 2, 7)));
        Tree other = tree(
                level,
                h.absolutePos(new BlockPos(8, 2, 7)),
                TestTrees.LOG.get(),
                TestTrees.LEAVES.get(),
                5,
                List.of(2, 3),
                4);
        Map<BlockPos, Integer> oakLeaves = new HashMap<>(grown.leaves());
        other.leaves().keySet().forEach(oakLeaves::remove);
        BlockPos shared = grown.logs().getFirst().offset(2, 2, 0);
        h.assertTrue(
                level.getBlockState(shared).is(TestTrees.LEAVES.get())
                        && level.getBlockState(shared).getValue(LeavesBlock.DISTANCE) == 2
                        && level.getBlockState(shared.west()).is(Blocks.OAK_LEAVES)
                        && level.getBlockState(shared.west()).getValue(LeavesBlock.DISTANCE) == 1,
                "the other tree's leaf stands where the oak's gradient goes on");
        Tree[] trees = {new Tree(grown.logBlock(), grown.leafBlock(), grown.logs(), oakLeaves), other};
        Player gardener = EcologyTests.holding(h, GameType.CREATIVE, EcologyModule.SPORE_CULTURE.get(), 2);
        h.startSequence()
                .thenExecuteAfter(4, () -> {
                    trees[0] = settled(level, trees[0]);
                    trees[1] = settled(level, trees[1]);
                    EcologyTests.useCulture(level, gardener, h.absolutePos(new BlockPos(3, 1, 7)));
                })
                .thenWaitUntil(() -> assertConverted(h, trees[0]))
                .thenWaitUntil(
                        () -> h.assertFalse(snapshot(h).works(grown.logs().getFirst()), "the oak's job is done"))
                .thenExecute(() -> {
                    assertUntouched(h, trees[1]);
                    assertNothingSpawned(h);
                    EcologyTests.useCulture(level, gardener, h.absolutePos(new BlockPos(9, 1, 7)));
                })
                .thenWaitUntil(() -> assertConverted(h, trees[1]))
                .thenExecute(() -> {
                    assertConverted(h, trees[0]);
                    assertNothingSpawned(h);
                })
                .thenSucceed();
    }

    /**
     * In another mod's tree, a leaf of a kind a pack excludes and a wood block against the trunk are not the tree's:
     * they stay, and the rest turns.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = BATCH + "17",
            timeoutTicks = 200)
    public static void excludedLeavesAndWoodInATreeStay(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        BlockPos base = h.absolutePos(new BlockPos(7, 2, 7));
        Tree grown = tree(level, base, TestTrees.LOG.get(), TestTrees.LEAVES.get(), 5, List.of(2, 3), 4);
        // the cap over the trunk and a corner of a wide layer: no leaf of the tree is held only through them
        Map<BlockPos, BlockState> kept = Map.of(
                base.above(5), TestTrees.EXCLUDED_LEAVES.get().defaultBlockState(),
                base.offset(2, 2, 1), TestTrees.EXCLUDED_LEAVES.get().defaultBlockState(),
                base.north().above(), TestTrees.WOOD.get().defaultBlockState());
        kept.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_ALL));
        Map<BlockPos, Integer> rest = new HashMap<>(grown.leaves());
        kept.keySet().forEach(rest::remove);
        Tree[] standing = {new Tree(grown.logBlock(), grown.leafBlock(), grown.logs(), rest)};
        Player gardener = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        h.startSequence()
                .thenExecuteAfter(4, () -> {
                    standing[0] = settled(level, standing[0]);
                    EcologyTests.useCulture(level, gardener, base.west().below());
                })
                .thenWaitUntil(() -> assertConverted(h, standing[0]))
                .thenWaitUntil(() -> h.assertFalse(snapshot(h).works(base), "the job is done"))
                .thenExecute(() -> {
                    kept.forEach((pos, state) -> h.assertValueEqual(
                            level.getBlockState(pos).getBlock(), state.getBlock(), "kept at " + pos));
                    assertNothingSpawned(h);
                })
                .thenSucceed();
    }

    /** The planks, stripped log, chest and fence built against a trunk stay; the oak's own logs and leaves turn. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH + "4", timeoutTicks = 200)
    public static void constructionAgainstTheTrunkStaysWhileTheTreeTurns(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        BlockPos base = h.absolutePos(new BlockPos(7, 2, 7));
        Tree oak = oak(level, base);
        Map<BlockPos, BlockState> built = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, 3, -1), base.offset(1, 3, 1)))
            if (!pos.equals(base.above(3))) built.put(pos.immutable(), Blocks.OAK_PLANKS.defaultBlockState());
        built.put(base.east(), Blocks.STRIPPED_OAK_LOG.defaultBlockState());
        built.put(base.west(), Blocks.CHEST.defaultBlockState());
        built.put(base.south(), Blocks.OAK_FENCE.defaultBlockState());
        built.put(base.north().above(), Blocks.OAK_WOOD.defaultBlockState());
        built.put(base.above(5).east(), Blocks.BIRCH_LEAVES.defaultBlockState());
        built.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_ALL));
        Map<BlockPos, Integer> rest = new HashMap<>(oak.leaves());
        built.keySet().forEach(rest::remove);
        Tree[] standing = {new Tree(oak.logBlock(), oak.leafBlock(), oak.logs(), rest)};
        Player gardener = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        h.startSequence()
                .thenExecuteAfter(4, () -> {
                    // vanilla has settled the leaf distances round the planks by now
                    standing[0] = settled(level, standing[0]);
                    EcologyTests.useCulture(level, gardener, base.north().below());
                })
                .thenWaitUntil(() -> assertConverted(h, standing[0]))
                .thenExecute(() -> {
                    built.forEach((pos, state) -> h.assertValueEqual(
                            level.getBlockState(pos).getBlock(),
                            state.getBlock(),
                            "built against the trunk at " + pos));
                    assertNothingSpawned(h);
                })
                .thenSucceed();
    }

    // --- boundaries ---

    /** Of two birches whose crowns touch, only the one whose trunk the cell touches turns; the other keeps every block. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH + "5", timeoutTicks = 200)
    public static void leafContactAloneNeverCarriesInfectionIntoTheNextTree(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        Tree near = birch(level, h.absolutePos(new BlockPos(4, 2, 7)));
        Tree far = birch(level, h.absolutePos(new BlockPos(9, 2, 7)));
        h.assertTrue(
                level.getBlockState(near.logs().getFirst().offset(2, 3, 0)).is(Blocks.BIRCH_LEAVES)
                        && level.getBlockState(far.logs().getFirst().offset(-2, 3, 0))
                                .is(Blocks.BIRCH_LEAVES),
                "the crowns stand face to face");
        Player gardener = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        EcologyTests.useCulture(level, gardener, h.absolutePos(new BlockPos(3, 1, 7)));
        h.startSequence()
                .thenWaitUntil(() -> assertConverted(h, near))
                .thenIdle(5)
                .thenExecute(() -> {
                    assertUntouched(h, far);
                    h.assertFalse(snapshot(h).works(near.logs().getFirst()), "the one job is done");
                    h.assertFalse(snapshot(h).works(far.logs().getFirst()), "the far birch never had one");
                    assertNothingSpawned(h);
                })
                .thenSucceed();
    }

    /** Two cells touching two base logs of one dark oak in the same tick make one conversion, not two. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH + "6", timeoutTicks = 200)
    public static void twoTouchesOfOneTreeMakeOneJob(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        Tree darkOak = darkOak(level, h.absolutePos(new BlockPos(6, 2, 6)));
        Player gardener = EcologyTests.holding(h, GameType.CREATIVE, EcologyModule.SPORE_CULTURE.get(), 2);
        EcologyTests.useCulture(level, gardener, h.absolutePos(new BlockPos(5, 1, 6)));
        EcologyTests.useCulture(level, gardener, h.absolutePos(new BlockPos(8, 1, 7)));
        List<BlockPos> roots = List.of(darkOak.logs().getFirst(), darkOak.logs().get(3));
        h.assertTrue(snapshot(h).works(roots.get(0)) && snapshot(h).works(roots.get(1)), "two touches, two jobs");
        int[] mostJobs = {0};
        h.startSequence()
                .thenWaitUntil(() -> {
                    TreeInfectionSnapshot now = snapshot(h);
                    long converting = now.jobs().stream()
                            .filter(j ->
                                    roots.contains(j.root()) && j.phase() == TreeInfectionSnapshot.Phase.CONVERTING)
                            .count();
                    mostJobs[0] = Math.max(mostJobs[0], (int) converting);
                    h.assertFalse(now.works(roots.get(0)) || now.works(roots.get(1)), "both touches are settled");
                })
                .thenExecute(() -> {
                    assertConverted(h, darkOak);
                    h.assertValueEqual(mostJobs[0], 1, "never two jobs converting the one tree");
                    for (TreeInfectionSnapshot.Refusal refusal : snapshot(h).refusals())
                        h.assertFalse(roots.contains(refusal.root()), "a touch was refused: " + refusal);
                    assertNothingSpawned(h);
                })
                .thenSucceed();
    }

    /** A block a player changes while the tree is turning keeps the player's change; the rest turns. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH + "7", timeoutTicks = 200)
    public static void aPlayerEditDuringConversionIsKept(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        Tree oak = oak(level, h.absolutePos(new BlockPos(7, 2, 7)));
        BlockPos root = oak.logs().getFirst();
        BlockPos[] stone = {BlockPos.ZERO};
        BlockPos[] gone = {BlockPos.ZERO};
        Player gardener = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        EcologyTests.useCulture(level, gardener, h.absolutePos(new BlockPos(6, 1, 7)));
        h.startSequence()
                .thenWaitUntil(() -> {
                    TreeInfectionSnapshot.Job job = job(h, root);
                    h.assertTrue(job.phase() == TreeInfectionSnapshot.Phase.CONVERTING, "admitted");
                    h.assertTrue(job.leavesLeft() > 20, "with leaves still to turn");
                })
                .thenExecute(() -> {
                    // two leaves the job has not reached: they become a player's stone and a player's gap
                    List<BlockPos> living = oak.leaves().keySet().stream()
                            .filter(pos -> level.getBlockState(pos).is(Blocks.OAK_LEAVES))
                            .sorted()
                            .toList();
                    h.assertTrue(living.size() > 20, "leaves not yet turned: " + living.size());
                    stone[0] = living.getFirst();
                    gone[0] = living.getLast();
                    level.setBlock(stone[0], Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(gone[0], Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                })
                .thenWaitUntil(() -> h.assertFalse(snapshot(h).works(root), "the job is done"))
                .thenExecute(() -> {
                    h.assertTrue(level.getBlockState(stone[0]).is(Blocks.STONE), "the stone stays");
                    h.assertTrue(level.getBlockState(gone[0]).isAir(), "the gap stays");
                    Map<BlockPos, Integer> rest = new HashMap<>(oak.leaves());
                    rest.remove(stone[0]);
                    rest.remove(gone[0]);
                    assertConverted(h, settled(level, new Tree(oak.logBlock(), oak.leafBlock(), oak.logs(), rest)));
                })
                .thenSucceed();
    }

    /** A spruce of 132 logs over 33 blocks and an oak joined to a birch log are refused with their reasons and stand whole. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH + "8", timeoutTicks = 300)
    public static void oversizedAndAmbiguousTreesAreRefusedAndLeftWhole(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Yard yard = new Yard(h, 0, 32, 48);
        BlockPos giant = yard.at(4, 1, 8);
        List<BlockPos> column = new ArrayList<>();
        for (int y = 0; y < 33; y++)
            for (BlockPos pos :
                    BlockPos.betweenClosed(giant.above(y), giant.above(y).offset(1, 0, 1))) column.add(pos.immutable());
        for (BlockPos pos : column) level.setBlock(pos, Blocks.SPRUCE_LOG.defaultBlockState(), Block.UPDATE_ALL);
        Tree grown = oak(level, yard.at(20, 1, 8));
        BlockPos birchLog = grown.logs().get(2).east();
        level.setBlock(birchLog, Blocks.BIRCH_LOG.defaultBlockState(), Block.UPDATE_ALL);
        Map<BlockPos, Integer> rest = new HashMap<>(grown.leaves());
        rest.remove(birchLog);
        Tree oak = new Tree(grown.logBlock(), grown.leafBlock(), grown.logs(), rest);
        Player gardener = EcologyTests.holding(h, GameType.CREATIVE, EcologyModule.SPORE_CULTURE.get(), 2);
        EcologyTests.useCulture(level, gardener, giant.below().north());
        EcologyTests.useCulture(level, gardener, oak.logs().getFirst().below().north());
        h.assertTrue(snapshot(h).works(giant) && snapshot(h).works(oak.logs().getFirst()), "both start discovery");
        h.startSequence()
                .thenWaitUntil(() -> h.assertFalse(
                        snapshot(h).works(giant) || snapshot(h).works(oak.logs().getFirst()), "discovery settles both"))
                .thenExecute(() -> {
                    List<TreeInfectionSnapshot.Refusal> refusals = snapshot(h).refusals();
                    h.assertTrue(
                            refusals.contains(
                                    new TreeInfectionSnapshot.Refusal(giant, TreeInfectionSnapshot.Reason.OVERSIZED)),
                            "the giant is too large: " + refusals);
                    h.assertTrue(
                            refusals.contains(new TreeInfectionSnapshot.Refusal(
                                    oak.logs().getFirst(), TreeInfectionSnapshot.Reason.AMBIGUOUS)),
                            "the oak joins a birch: " + refusals);
                    for (BlockPos pos : column)
                        h.assertTrue(level.getBlockState(pos).is(Blocks.SPRUCE_LOG), "the column stands at " + pos);
                    assertUntouched(h, oak);
                    h.assertTrue(level.getBlockState(birchLog).is(Blocks.BIRCH_LOG), "the birch log too");
                    yard.clear(h);
                })
                .thenSucceed();
    }

    /** A tree whose crown crosses into an unloaded chunk is refused as unloaded, stands whole, and loads nothing. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH + "9", timeoutTicks = 600)
    public static void aTreeReachingUnloadedLandIsRefusedAndLoadsNothing(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // the yard's chunk is forced and keeps two chunks east of it loaded as its border; the trunk stands in the
        // last column of the second, so its crown reaches the third, which is loaded only while it is built
        Yard yard = new Yard(h, 1, 16, 16);
        BlockPos base = yard.at(47, 1, 8);
        ChunkPos east = new ChunkPos(base.east());
        TestChunks.force(level, east);
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-3, -1, -3), base.offset(3, -1, 3)))
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        Tree oak = oak(level, base);
        level.setChunkForced(east.x, east.z, false);
        Player gardener = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(
                        level.hasChunkAt(base) && !level.hasChunkAt(base.east()),
                        "the trunk's chunk stays, the east chunk unloads"))
                .thenExecute(() -> {
                    EcologyTests.useCulture(level, gardener, base.below().west());
                    h.assertTrue(snapshot(h).works(base), "the trunk was touched");
                })
                .thenWaitUntil(() -> h.assertFalse(snapshot(h).works(base), "settled"))
                .thenExecute(() -> {
                    h.assertTrue(
                            snapshot(h)
                                    .refusals()
                                    .contains(new TreeInfectionSnapshot.Refusal(
                                            base, TreeInfectionSnapshot.Reason.UNLOADED)),
                            "refused as unloaded: " + snapshot(h).refusals());
                    h.assertFalse(level.hasChunkAt(base.east()), "and the east chunk " + east + " is still unloaded");
                    for (BlockPos log : oak.logs())
                        h.assertTrue(level.getBlockState(log).is(Blocks.OAK_LOG), "the trunk stands");
                    for (BlockPos leaf : oak.leaves().keySet())
                        if (level.hasChunkAt(leaf))
                            h.assertTrue(level.getBlockState(leaf).is(Blocks.OAK_LEAVES), "the crown stands");
                    TestChunks.force(level, east);
                    for (BlockPos pos : BlockPos.betweenClosed(base.offset(-3, -1, -3), base.offset(3, 8, 3)))
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    level.setChunkForced(east.x, east.z, false);
                    yard.clear(h);
                })
                .thenSucceed();
    }

    // --- budgets ---

    /**
     * Seventeen trees touched in one tick: sixteen get jobs, the seventeenth is refused for capacity and its wave
     * still converts the ground and withers the flower beside it. Never more than sixteen blocks turn in a tick.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = BATCH + "10",
            timeoutTicks = 600)
    public static void sixteenJobsADimensionSixteenReplacementsATickAndAReadableRefusal(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Yard yard = new Yard(h, 2, 112, 16);
        List<Tree> trees = new ArrayList<>();
        for (int i = 0; i <= JOBS; i++) trees.add(oak(level, yard.at(3 + 6 * i, 1, 8)));
        Tree last = trees.getLast();
        BlockPos lastCell = last.logs().getFirst().below().west();
        BlockPos flower = lastCell.west().above();
        level.setBlock(flower, Blocks.POPPY.defaultBlockState(), Block.UPDATE_ALL);
        // sandstone each side of the cell: the wave's one host is the ground under the flower
        level.setBlock(lastCell.north(), Blocks.SANDSTONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(lastCell.south(), Blocks.SANDSTONE.defaultBlockState(), Block.UPDATE_ALL);
        Player gardener = EcologyTests.holding(h, GameType.CREATIVE, EcologyModule.SPORE_CULTURE.get(), JOBS);
        for (int i = 0; i < JOBS; i++)
            EcologyTests.useCulture(
                    level, gardener, trees.get(i).logs().getFirst().below().west());
        for (int i = 0; i < JOBS; i++)
            h.assertTrue(snapshot(h).works(trees.get(i).logs().getFirst()), "a job on tree " + i);
        // the seventeenth is touched by a wave: bone meal on a cell beside its trunk
        level.setBlock(lastCell, EcologyModule.LIVING_SUBSTRATE.get().defaultBlockState(), Block.UPDATE_ALL);
        h.assertTrue(
                BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, lastCell), "bone meal takes on the cell");
        int[] turned = {0};
        int[] mostInATick = {0};
        h.startSequence()
                .thenWaitUntil(() -> {
                    int now = 0;
                    for (Tree tree : trees) now += infectedBlocks(level, tree);
                    mostInATick[0] = Math.max(mostInATick[0], now - turned[0]);
                    turned[0] = now;
                    for (Tree tree : trees)
                        h.assertFalse(
                                snapshot(h).works(tree.logs().getFirst()),
                                "a job left on " + tree.logs().getFirst());
                    h.assertTrue(level.getBlockState(lastCell).getValue(LivingSubstrateBlock.VIGOR) == 0, "wave done");
                })
                .thenExecute(() -> {
                    h.assertTrue(mostInATick[0] <= REPLACEMENTS_PER_TICK, "turned a tick: " + mostInATick[0]);
                    h.assertTrue(mostInATick[0] > 1, "and more than one tree at a time: " + mostInATick[0]);
                    for (int i = 0; i < JOBS; i++) assertConverted(h, trees.get(i));
                    assertUntouched(h, last);
                    h.assertTrue(
                            snapshot(h)
                                    .refusals()
                                    .contains(new TreeInfectionSnapshot.Refusal(
                                            last.logs().getFirst(), TreeInfectionSnapshot.Reason.CAPACITY)),
                            "the seventeenth was refused for capacity: "
                                    + snapshot(h).refusals());
                    h.assertTrue(
                            level.getBlockState(flower).is(EcologyModule.WITHERED_FLOWER.get()),
                            "the wave still withered the flower beside the refused tree");
                    h.assertTrue(
                            level.getBlockState(flower.below()).is(EcologyModule.LIVING_SUBSTRATE.get()),
                            "and converted its ground");
                    yard.clear(h);
                })
                .thenSucceed();
    }

    // --- persistence ---

    /**
     * A tree half turned when its chunk unloads waits, loading nothing; its job survives a real save and reload of
     * the dimension's data and finishes once the chunk is back, turning every block once.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = BATCH + "11",
            timeoutTicks = 900)
    public static void conversionPausesOnUnloadSurvivesASaveAndResumesOnReload(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Yard yard = new Yard(h, 3, 16, 16);
        ChunkPos chunk = new ChunkPos(yard.at(8, 1, 8));
        // a kept chunk three chunks on, so the tree's chunk may go while the yard's row stays reachable
        ChunkPos kept = new ChunkPos(chunk.x + 3, chunk.z);
        TestChunks.force(level, kept);
        // a tall oak of 20 logs and 353 leaves, 23 ticks of work: the chunk is unloaded long before the job ends
        Tree oak = tree(
                level,
                yard.at(8, 1, 8),
                Blocks.OAK_LOG,
                Blocks.OAK_LEAVES,
                20,
                List.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18),
                19);
        BlockPos root = oak.logs().getFirst();
        Player gardener = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        EcologyTests.useCulture(level, gardener, root.below().west());
        int[] leftWhenPaused = {0};
        h.startSequence()
                .thenWaitUntil(() -> {
                    TreeInfectionSnapshot.Job job = job(h, root);
                    h.assertTrue(job.phase() == TreeInfectionSnapshot.Phase.CONVERTING, "admitted");
                    h.assertTrue(job.leavesLeft() > 300, "with most leaves still to turn");
                })
                .thenExecute(() -> level.setChunkForced(chunk.x, chunk.z, false))
                .thenWaitUntil(() -> {
                    // a busy test server defers unloads until it has spare time; give the chunk source that time now
                    level.getChunkSource().tick(() -> true, false);
                    h.assertFalse(level.hasChunkAt(root), "the tree's chunk unloaded");
                })
                .thenExecute(() -> {
                    leftWhenPaused[0] = job(h, root).leavesLeft();
                    h.assertTrue(leftWhenPaused[0] > 0, "the job waits with leaves left");
                })
                .thenIdle(120)
                .thenExecute(() -> {
                    h.assertFalse(level.hasChunkAt(root), "waiting loaded nothing");
                    TreeInfectionSnapshot now = snapshot(h);
                    h.assertValueEqual(job(h, root).leavesLeft(), leftWhenPaused[0], "and turned nothing");
                    // a real save: written to disk by a data storage, read back by another, put in the level's place
                    CompoundTag saved = EcologyModule.saveTreeInfections(level);
                    SavedData restored = saveAndReload(h, saved);
                    level.getDataStorage().set(EcologyModule.TREE_INFECTIONS, restored);
                    TreeInfectionSnapshot reloaded = snapshot(h);
                    h.assertTrue(
                            reloaded.jobs().contains(job(h, root)) && reloaded.works(root),
                            "the job reads back as it was: " + reloaded.jobs());
                    h.assertValueEqual(job(h, root).leavesLeft(), leftWhenPaused[0], "with its leaves");
                    h.assertTrue(reloaded.refusals().isEmpty(), "refusals are not saved");
                    TestChunks.force(level, chunk);
                })
                .thenWaitUntil(() -> h.assertFalse(snapshot(h).works(root), "the job finished after the reload"))
                .thenExecute(() -> {
                    assertConverted(h, oak);
                    h.assertTrue(
                            level.getEntitiesOfClass(ItemEntity.class, new AABB(root).inflate(12))
                                    .isEmpty(),
                            "nothing dropped");
                    yard.clear(h);
                    level.setChunkForced(kept.x, kept.z, false);
                })
                .thenSucceed();
    }

    /** An unreadable saved job list is kept as it was and written back; a job list past its bounds is such a list. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH + "12")
    public static void anUnreadableSavedJobListIsKeptAndWrittenBack(GameTestHelper h) {
        CompoundTag saved = new CompoundTag();
        saved.putString("jobs", "not a list");
        SavedData restored = saveAndReload(h, saved);
        CompoundTag again = restored.save(new CompoundTag(), h.getLevel().registryAccess());
        h.assertValueEqual(again.getString("unreadable_jobs"), "not a list", "kept as bytes");
        h.assertValueEqual(again.getList("jobs", 10).size(), 0, "and no job was invented");
        h.succeed();
    }

    /**
     * A chunk and a job saved by the build that told six species apart, as the files were written: Infected Logs and
     * Blighted Leaves with {@code source}, a chest of Infected Logs and Planks, placed planks, and an oak half turned
     * whose job names the species. The chunk is put in the world's storage and loaded by the engine; the blocks keep
     * axis, distance, persistence and water and lose nothing else; the job reads from its file, goes through a save of
     * the current format, and turns the rest of the oak, leaving two leaves a player changed meanwhile as they are.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = BATCH + "18",
            timeoutTicks = 400)
    public static void anOldSaveLoadsItsTimberAndFinishesItsJob(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ChunkPos chunk = new ChunkPos(LEGACY_ORIGIN);
        h.assertFalse(level.hasChunk(chunk.x, chunk.z), "the old chunk is not loaded before it is restored");
        // queued in the chunk storage's worker, which answers the load below from the pending write
        level.getChunkSource().chunkMap.write(chunk, fixture(h, "chunk.nbt"));
        TestChunks.force(level, chunk);
        BlockPos root = LEGACY_ORIGIN.offset(8, 1, 8);
        Tree oak = legacyOak(root);
        Map<BlockPos, BlockState> old = Map.of(
                LEGACY_ORIGIN.offset(1, 1, 1), log(Direction.Axis.X),
                LEGACY_ORIGIN.offset(2, 1, 1), log(Direction.Axis.Z),
                LEGACY_ORIGIN.offset(3, 1, 1), log(Direction.Axis.Y),
                LEGACY_ORIGIN.offset(1, 2, 1), blighted(1, false, false),
                LEGACY_ORIGIN.offset(4, 1, 4), blighted(3, true, true),
                LEGACY_ORIGIN.offset(5, 1, 1),
                        EcologyModule.INFECTED_PLANKS.get().defaultBlockState());
        BlockPos chestAt = LEGACY_ORIGIN.offset(6, 1, 1);
        AABB area = new AABB(LEGACY_ORIGIN).expandTowards(16, 16, 16).inflate(16);
        old.forEach((pos, state) -> h.assertValueEqual(level.getBlockState(pos), state, "old " + pos));
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(chestAt);
        h.assertTrue(
                chest != null
                        && chest.countItem(EcologyModule.INFECTED_LOG_ITEM.get()) == 5
                        && chest.countItem(EcologyModule.INFECTED_PLANKS_ITEM.get()) == 7,
                "the old chest keeps its logs and planks");
        BlockPos[] edits = new BlockPos[2];
        h.startSequence()
                // the saved test world may hold an earlier run's items here; they load with the chunk's entities
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
                    for (BlockPos log : oak.logs())
                        h.assertValueEqual(
                                level.getBlockState(log), log(Direction.Axis.Y), "the old job turned " + log);
                    List<BlockPos> living = oak.leaves().keySet().stream()
                            .filter(pos -> level.getBlockState(pos).is(Blocks.OAK_LEAVES))
                            .sorted()
                            .toList();
                    h.assertTrue(living.size() > 20, "the old job left leaves: " + living.size());
                    // the old job as its file holds it, read by a data storage, saved in today's format and read back
                    SavedData read = legacyJobs(h, fixtureFile(h));
                    SavedData restored = saveAndReload(h, read.save(new CompoundTag(), level.registryAccess()));
                    level.getDataStorage().set(EcologyModule.TREE_INFECTIONS, restored);
                    TreeInfectionSnapshot.Job job = job(h, root);
                    h.assertValueEqual(job.log(), "minecraft:oak_log", "the species reads as the oak's log");
                    h.assertValueEqual(job.phase(), TreeInfectionSnapshot.Phase.CONVERTING, "still converting");
                    h.assertValueEqual(job.leavesLeft(), living.size(), "with the leaves it left");
                    // a player's edits since the save: a stone and a leaf of another tree
                    edits[0] = living.getFirst();
                    edits[1] = living.getLast();
                    level.setBlock(edits[0], Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(edits[1], Blocks.BIRCH_LEAVES.defaultBlockState(), Block.UPDATE_ALL);
                })
                .thenWaitUntil(() -> h.assertFalse(snapshot(h).works(root), "the old job finished"))
                .thenExecute(() -> {
                    h.assertTrue(level.getBlockState(edits[0]).is(Blocks.STONE), "the stone stays");
                    h.assertTrue(level.getBlockState(edits[1]).is(Blocks.BIRCH_LEAVES), "the birch leaf stays");
                    for (BlockPos pos : oak.leaves().keySet())
                        if (!pos.equals(edits[0]) && !pos.equals(edits[1]))
                            h.assertTrue(
                                    level.getBlockState(pos).is(EcologyModule.BLIGHTED_LEAVES.get()),
                                    "blighted at " + pos + ": " + level.getBlockState(pos));
                    List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, area);
                    h.assertTrue(dropped.isEmpty(), "nothing dropped: " + dropped);
                    // the chunk as it is now, written and read back by the engine's own chunk codec
                    LevelChunk loaded = level.getChunk(chunk.x, chunk.z);
                    ProtoChunk again = ChunkSerializer.read(
                            level,
                            level.getPoiManager(),
                            new RegionStorageInfo("legacy_timber", level.dimension(), "chunk"),
                            chunk,
                            ChunkSerializer.write(level, loaded));
                    for (BlockPos pos : BlockPos.betweenClosed(LEGACY_ORIGIN, LEGACY_ORIGIN.offset(15, 8, 15)))
                        h.assertValueEqual(again.getBlockState(pos), level.getBlockState(pos), "saved again " + pos);
                    for (BlockPos pos :
                            BlockPos.betweenClosed(LEGACY_ORIGIN.above(), LEGACY_ORIGIN.offset(15, 8, 15))) {
                        level.removeBlockEntity(pos); // the chest goes without spilling its logs
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
                    level.setChunkForced(chunk.x, chunk.z, false);
                })
                .thenSucceed();
    }

    /**
     * Every saved job is read in exactly one format or not at all. An old job reads as its vanilla blocks with no
     * invented states, a job of today's format reads as saved; a species the old build never had, a mixture of the two
     * formats, malformed current data or identities that contradict the job's log keep the whole list as it was,
     * through repeated saves, and invent no job.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH + "19")
    public static void oldAndMalformedJobsReadExactlyOrStayOpaque(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        CompoundTag file = fixtureFile(h);
        CompoundTag discovering = file.getCompound("data").copy();
        CompoundTag first = discovering.getList("jobs", 10).getCompound(0);
        first.putBoolean("discovering", true);
        first.put("logs", new ListTag());
        first.put("leaves", new ListTag());
        ListTag rediscovers = EcologyModule.restoreTreeInfections(discovering, registries)
                .save(new CompoundTag(), registries)
                .getList("jobs", 10);
        h.assertValueEqual(rediscovers.size(), 1, "the discovering job reads");
        h.assertValueEqual(rediscovers.getCompound(0).getString("log"), "minecraft:oak_log", "as an oak");
        h.assertTrue(rediscovers.getCompound(0).getBoolean("discovering"), "and discovers again");

        CompoundTag unknown = file.getCompound("data").copy();
        unknown.getList("jobs", 10).getCompound(0).putString("species", "cherry");
        assertKeptOpaque(h, unknown);

        // today's format, as the old job is saved once read: it round-trips exactly
        CompoundTag current = legacyJobs(h, file).save(new CompoundTag(), registries);
        CompoundTag again =
                EcologyModule.restoreTreeInfections(current, registries).save(new CompoundTag(), registries);
        h.assertValueEqual(again.get("jobs"), current.get("jobs"), "a current job reads as it was saved");
        h.assertTrue(current.getList("jobs", 10).getCompound(0).getBoolean("identity_only"), "with no invented states");
        h.assertFalse(current.getList("jobs", 10).getCompound(0).contains("root_state"), "nor an invented root");
        // and with the root contact found, as a job of this version saves it
        CompoundTag rooted = current.copy();
        rooted.getList("jobs", 10)
                .getCompound(0)
                .put("root_state", NbtUtils.writeBlockState(Blocks.OAK_LOG.defaultBlockState()));
        h.assertValueEqual(
                EcologyModule.restoreTreeInfections(rooted, registries)
                        .save(new CompoundTag(), registries)
                        .get("jobs"),
                rooted.get("jobs"),
                "a job with its root state reads too");

        // a current job that also names a valid species is of two formats: kept, never read as the old one
        CompoundTag mixed = current.copy();
        mixed.getList("jobs", 10).getCompound(0).putString("species", "oak");
        assertKeptOpaque(h, mixed);
        // an old job with a field of today's format is the same mixture
        CompoundTag oldMixed = file.getCompound("data").copy();
        oldMixed.getList("jobs", 10).getCompound(0).put("states", new ListTag());
        assertKeptOpaque(h, oldMixed);
        // malformed current data is not retried as anything else
        CompoundTag mismatched = current.copy();
        ListTag one = new ListTag();
        one.add(IntTag.valueOf(1));
        mismatched.getList("jobs", 10).getCompound(0).put("leaf_states", one);
        assertKeptOpaque(h, mismatched);
        CompoundTag mismatchedWithSpecies = mismatched.copy();
        mismatchedWithSpecies.getList("jobs", 10).getCompound(0).putString("species", "oak");
        assertKeptOpaque(h, mismatchedWithSpecies);
        // identities that contradict the job's log: a log target expecting a birch log, a birch root
        CompoundTag withLog = current.copy();
        CompoundTag logJob = withLog.getList("jobs", 10).getCompound(0);
        ListTag logPositions = new ListTag();
        logPositions.add(logJob.get("root").copy());
        logJob.put("logs", logPositions);
        logJob.putIntArray("log_states", new int[] {0});
        h.assertValueEqual(
                EcologyModule.restoreTreeInfections(withLog, registries)
                        .save(new CompoundTag(), registries)
                        .getList("jobs", 10)
                        .size(),
                1,
                "a log target expecting the job's own log reads");
        CompoundTag birchLogs = withLog.copy();
        birchLogs
                .getList("jobs", 10)
                .getCompound(0)
                .getList("states", 10)
                .set(0, NbtUtils.writeBlockState(Blocks.BIRCH_LOG.defaultBlockState()));
        assertKeptOpaque(h, birchLogs);
        CompoundTag birchRoot = current.copy();
        birchRoot
                .getList("jobs", 10)
                .getCompound(0)
                .put("root_state", NbtUtils.writeBlockState(Blocks.BIRCH_LOG.defaultBlockState()));
        assertKeptOpaque(h, birchRoot);
        // a record of neither format
        CompoundTag neither = current.copy();
        neither.getList("jobs", 10).getCompound(0).remove("log");
        neither.getList("jobs", 10).getCompound(0).remove("identity_only");
        neither.getList("jobs", 10).getCompound(0).remove("states");
        neither.getList("jobs", 10).getCompound(0).remove("log_states");
        neither.getList("jobs", 10).getCompound(0).remove("leaf_states");
        assertKeptOpaque(h, neither);
        // saved states are read exactly: a log target expecting a lying log, the root upright, both read as written
        CompoundTag lying = withLog.copy();
        properties(lying, "states", 0).putString("axis", "x");
        lying.getList("jobs", 10)
                .getCompound(0)
                .put("root_state", NbtUtils.writeBlockState(Blocks.OAK_LOG.defaultBlockState()));
        h.assertValueEqual(
                EcologyModule.restoreTreeInfections(lying, registries)
                        .save(new CompoundTag(), registries)
                        .get("jobs"),
                lying.get("jobs"),
                "a non-default state reads as written");
        // a property vanilla would replace with its default is no expectation at all: the list is kept as it was
        CompoundTag badRoot = lying.copy();
        properties(badRoot, "root_state", -1).putString("axis", "invalid");
        assertKeptOpaque(h, badRoot);
        CompoundTag badLog = lying.copy();
        properties(badLog, "states", 0).putString("axis", "invalid");
        assertKeptOpaque(h, badLog);
        CompoundTag badDistance = lying.copy();
        properties(badDistance, "states", 1).putString("distance", "9");
        assertKeptOpaque(h, badDistance);
        CompoundTag badPersistence = lying.copy();
        properties(badPersistence, "states", 1).putString("persistent", "maybe");
        assertKeptOpaque(h, badPersistence);
        CompoundTag unknownProperty = lying.copy();
        properties(unknownProperty, "states", 1).putString("colour", "red");
        assertKeptOpaque(h, unknownProperty);
        CompoundTag unknownRootProperty = lying.copy();
        properties(unknownRootProperty, "root_state", -1).putString("source", "oak");
        assertKeptOpaque(h, unknownRootProperty);
        CompoundTag missingProperty = lying.copy();
        properties(missingProperty, "states", 1).remove("waterlogged");
        assertKeptOpaque(h, missingProperty);
        CompoundTag typed = lying.copy();
        properties(typed, "states", 1).putInt("distance", 3);
        assertKeptOpaque(h, typed);
        h.succeed();
    }

    /**
     * A root replaced after contact, by another kind of log or by the same log lying down, is never discovered as the
     * tree: both jobs survive a real save while discovering and then end with nothing turned.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = BATCH + "20",
            timeoutTicks = 200)
    public static void aRootChangedBeforeDiscoveryIsNeverAdopted(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        Tree first = oak(level, h.absolutePos(new BlockPos(3, 2, 3)));
        Tree second = oak(level, h.absolutePos(new BlockPos(11, 2, 11)));
        BlockPos firstRoot = first.logs().getFirst();
        BlockPos secondRoot = second.logs().getFirst();
        Player gardener = EcologyTests.holding(h, GameType.CREATIVE, EcologyModule.SPORE_CULTURE.get(), 2);
        EcologyTests.useCulture(level, gardener, firstRoot.below().west());
        EcologyTests.useCulture(level, gardener, secondRoot.below().west());
        h.assertTrue(snapshot(h).works(firstRoot) && snapshot(h).works(secondRoot), "two jobs, discovering");
        SavedData restored = saveAndReload(h, EcologyModule.saveTreeInfections(level));
        level.getDataStorage().set(EcologyModule.TREE_INFECTIONS, restored);
        h.assertTrue(snapshot(h).works(firstRoot) && snapshot(h).works(secondRoot), "both read back");
        BlockState birch = Blocks.BIRCH_LOG.defaultBlockState();
        BlockState lying = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        level.setBlock(firstRoot, birch, Block.UPDATE_ALL);
        level.setBlock(secondRoot, lying, Block.UPDATE_ALL);
        h.startSequence()
                .thenWaitUntil(() -> h.assertFalse(
                        snapshot(h).works(firstRoot) || snapshot(h).works(secondRoot), "both jobs ended"))
                .thenIdle(5)
                .thenExecute(() -> {
                    h.assertValueEqual(level.getBlockState(firstRoot), birch, "the birch stays");
                    h.assertValueEqual(level.getBlockState(secondRoot), lying, "the lying log stays");
                    for (Tree tree : List.of(first, second)) {
                        for (BlockPos log : tree.logs().subList(1, tree.logs().size()))
                            h.assertTrue(level.getBlockState(log).is(Blocks.OAK_LOG), "a living log at " + log);
                        for (BlockPos leaf : tree.leaves().keySet())
                            h.assertTrue(level.getBlockState(leaf).is(Blocks.OAK_LEAVES), "living leaves at " + leaf);
                    }
                    assertNothingSpawned(h);
                })
                .thenSucceed();
    }

    /**
     * A player's change to a target that keeps its block, a log laid on its side, leaves placed by hand in a leaf's
     * place and a leaf filled with water, is kept, also after a real save and reload; every target left as discovery
     * read it turns.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = BATCH + "21",
            timeoutTicks = 600)
    public static void sameBlockEditsDuringConversionAreKept(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Yard yard = new Yard(h, 5, 16, 16);
        // 28 logs: more than a tick of replacements, so logs are still waiting when the job is admitted
        Tree oak = tree(
                level,
                yard.at(8, 1, 8),
                Blocks.OAK_LOG,
                Blocks.OAK_LEAVES,
                28,
                List.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
                27);
        BlockPos root = oak.logs().getFirst();
        BlockPos top = oak.logs().getLast();
        BlockPos[] leaves = new BlockPos[2];
        BlockState[] edited = new BlockState[1];
        Player gardener = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        EcologyTests.useCulture(level, gardener, root.below().west());
        boolean[] done = {false};
        h.startSequence()
                // the edits are made in the first tick the job is seen admitted, before another tick's replacements
                .thenWaitUntil(() -> {
                    if (done[0]) return;
                    TreeInfectionSnapshot.Job job = job(h, root);
                    h.assertTrue(job.phase() == TreeInfectionSnapshot.Phase.CONVERTING, "admitted");
                    // a job admitted with its logs already turned never ends this wait, and the test times out
                    h.assertTrue(job.logsLeft() > 0, "logs still waiting: " + job);
                    h.assertTrue(level.getBlockState(top).is(Blocks.OAK_LOG), "the top log waits");
                    List<BlockPos> far = oak.leaves().entrySet().stream()
                            .filter(entry -> entry.getValue() == 3)
                            .map(Map.Entry::getKey)
                            .sorted()
                            .toList();
                    leaves[0] = far.getFirst();
                    leaves[1] = far.getLast();
                    h.assertTrue(
                            level.getBlockState(leaves[0]).is(Blocks.OAK_LEAVES)
                                    && level.getBlockState(leaves[1]).is(Blocks.OAK_LEAVES),
                            "both leaves still oak");
                    done[0] = true;
                    edited[0] = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
                    level.setBlock(top, edited[0], Block.UPDATE_ALL);
                    BlockState placed = level.getBlockState(leaves[0]).setValue(LeavesBlock.PERSISTENT, true);
                    BlockState watered = level.getBlockState(leaves[1]).setValue(LeavesBlock.WATERLOGGED, true);
                    level.setBlock(leaves[0], placed, Block.UPDATE_ALL);
                    level.setBlock(leaves[1], watered, Block.UPDATE_ALL);
                    // a real save in the middle, read back in place: the expected states go with the job
                    SavedData restored = saveAndReload(h, EcologyModule.saveTreeInfections(level));
                    level.getDataStorage().set(EcologyModule.TREE_INFECTIONS, restored);
                })
                .thenExecute(() -> h.assertTrue(snapshot(h).works(root), "the job reads back"))
                .thenWaitUntil(() -> h.assertFalse(snapshot(h).works(root), "the job is done"))
                .thenExecute(() -> {
                    h.assertValueEqual(level.getBlockState(top), edited[0], "the lying log stays");
                    BlockState placed = level.getBlockState(leaves[0]);
                    h.assertTrue(
                            placed.is(Blocks.OAK_LEAVES) && placed.getValue(LeavesBlock.PERSISTENT),
                            "the placed leaves stay: " + placed);
                    BlockState watered = level.getBlockState(leaves[1]);
                    h.assertTrue(
                            watered.is(Blocks.OAK_LEAVES) && watered.getValue(LeavesBlock.WATERLOGGED),
                            "the watered leaf stays: " + watered);
                    Map<BlockPos, Integer> rest = new HashMap<>(oak.leaves());
                    rest.remove(leaves[0]);
                    rest.remove(leaves[1]);
                    assertConverted(
                            h,
                            settled(
                                    level,
                                    new Tree(
                                            oak.logBlock(),
                                            oak.leafBlock(),
                                            oak.logs().subList(0, oak.logs().size() - 1),
                                            rest)));
                    yard.clear(h);
                })
                .thenSucceed();
    }

    /**
     * Old jobs still discovering find today's tree. An oak whose trunk holds birch leaves first, then spruce, jungle
     * and oak, four kinds, is admitted, survives a save in the middle and turns every leaf of all four; an oak holding
     * five kinds is refused as ambiguous and stays whole.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = BATCH + "22",
            timeoutTicks = 400)
    public static void anOldDiscoveringJobFindsEveryKindOfLeafItsTrunkHolds(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        Tree four = mixedOak(level, h.absolutePos(new BlockPos(4, 2, 4)), false);
        Tree five = mixedOak(level, h.absolutePos(new BlockPos(11, 2, 11)), true);
        BlockPos fourRoot = four.logs().getFirst();
        BlockPos fiveRoot = five.logs().getFirst();
        // the old file, its job turned into two still discovering at these roots
        CompoundTag file = fixtureFile(h);
        CompoundTag oldJob = file.getCompound("data").getList("jobs", 10).getCompound(0);
        ListTag jobs = new ListTag();
        for (BlockPos root : List.of(fourRoot, fiveRoot)) {
            CompoundTag job = oldJob.copy();
            job.putIntArray("root", new int[] {root.getX(), root.getY(), root.getZ()});
            job.putBoolean("discovering", true);
            job.put("logs", new ListTag());
            job.put("leaves", new ListTag());
            jobs.add(job);
        }
        file.getCompound("data").put("jobs", jobs);
        level.getDataStorage().set(EcologyModule.TREE_INFECTIONS, legacyJobs(h, file));
        h.assertTrue(snapshot(h).works(fourRoot) && snapshot(h).works(fiveRoot), "both old jobs read");
        Map<BlockPos, BlockState> fiveBefore = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(fiveRoot.offset(-2, 0, -2), fiveRoot.offset(2, 6, 2)))
            fiveBefore.put(pos.immutable(), level.getBlockState(pos));
        h.startSequence()
                // refusals are not saved, so the five-kind oak's is read before the save below
                .thenWaitUntil(() -> h.assertFalse(snapshot(h).works(fiveRoot), "the five-kind oak settled"))
                .thenExecute(() -> h.assertTrue(
                        snapshot(h)
                                .refusals()
                                .contains(new TreeInfectionSnapshot.Refusal(
                                        fiveRoot, TreeInfectionSnapshot.Reason.AMBIGUOUS)),
                        "five kinds are refused: " + snapshot(h).refusals()))
                .thenWaitUntil(() -> {
                    TreeInfectionSnapshot.Job job = job(h, fourRoot);
                    h.assertTrue(job.phase() == TreeInfectionSnapshot.Phase.CONVERTING, "admitted");
                })
                .thenExecute(() -> {
                    h.assertTrue(job(h, fourRoot).leavesLeft() > 0, "leaves still to turn");
                    SavedData restored = saveAndReload(h, EcologyModule.saveTreeInfections(level));
                    level.getDataStorage().set(EcologyModule.TREE_INFECTIONS, restored);
                })
                .thenWaitUntil(() ->
                        h.assertFalse(snapshot(h).works(fourRoot) || snapshot(h).works(fiveRoot), "both settled"))
                .thenExecute(() -> {
                    assertConverted(h, settled(level, four));
                    fiveBefore.forEach((pos, state) -> h.assertValueEqual(
                            level.getBlockState(pos).getBlock(), state.getBlock(), "the five-kind oak at " + pos));
                    assertNothingSpawned(h);
                })
                .thenSucceed();
    }

    /**
     * The demo's oak, whose trunk holds on its first leaf layer birch leaves to the west (the first face discovery
     * reads), spruce to the north, jungle to the south and, with {@code fifth}, acacia to the east: four kinds with the
     * oak's own, or five.
     */
    private static Tree mixedOak(ServerLevel level, BlockPos base, boolean fifth) {
        Tree oak = oak(level, base);
        Map<Direction, Block> sides = new HashMap<>(Map.of(
                Direction.WEST,
                Blocks.BIRCH_LEAVES,
                Direction.NORTH,
                Blocks.SPRUCE_LEAVES,
                Direction.SOUTH,
                Blocks.JUNGLE_LEAVES));
        if (fifth) sides.put(Direction.EAST, Blocks.ACACIA_LEAVES);
        sides.forEach((side, leaves) -> level.setBlock(
                base.above(2).relative(side),
                leaves.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1).setValue(LeavesBlock.PERSISTENT, false),
                Block.UPDATE_ALL));
        return oak;
    }

    // --- harvest ---

    /** Cutting a turned oak gives one Infected Log a log and, with shears, one Blighted Leaves a leaf: one each. */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = BATCH + "13",
            timeoutTicks = 300)
    public static void aTurnedTreeHarvestsOnceAsInfectedTimberAndBlightedLeaves(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        Tree oak = oak(level, h.absolutePos(new BlockPos(7, 2, 7)));
        Player gardener = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        EcologyTests.useCulture(level, gardener, h.absolutePos(new BlockPos(6, 1, 7)));
        h.startSequence()
                .thenWaitUntil(() -> assertConverted(h, oak))
                .thenExecute(() -> {
                    assertNothingSpawned(h);
                    ServerPlayer shearer = TestDrops.miner(h, new ItemStack(Items.SHEARS));
                    for (BlockPos leaf : oak.leaves().keySet()) shearer.gameMode.destroyBlock(leaf);
                    ServerPlayer cutter = TestDrops.miner(h, new ItemStack(Items.IRON_AXE));
                    for (int i = oak.logs().size() - 1; i >= 0; i--)
                        cutter.gameMode.destroyBlock(oak.logs().get(i));
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    Map<Item, Integer> dropped = TestDrops.dropped(h);
                    h.assertValueEqual(
                            dropped,
                            Map.of(
                                    EcologyModule.INFECTED_LOG_ITEM.get(),
                                    oak.logs().size(),
                                    EcologyModule.BLIGHTED_LEAVES_ITEM.get(),
                                    oak.leaves().size()),
                            "one item a block, never the oak's own");
                    TestDrops.clearDrops(h);
                })
                .thenSucceed();
    }

    // --- the demo ---

    /** The Woodland plot's button bone-meals the cell; the wave walks the corridor and the living oak turns. */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = BATCH + "14",
            timeoutTicks = 400)
    public static void theDemoWoodlandOakTurnsFromItsOwnSupplies(GameTestHelper h) {
        DemoExhibit exhibit = new DemoExhibit(new BlockPos(79, -61, -13), new BlockPos(115, -20, -2), 0);
        exhibit.force(h);
        ServerLevel level = h.getLevel();
        BlockPos cell = exhibit.at(new BlockPos(81, -61, -8));
        BlockPos root = exhibit.at(new BlockPos(84, -60, -8));
        BlockPos chest = exhibit.at(new BlockPos(80, -60, -10));
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    exhibit.build(h);
                    ChestBlockEntity supplies = (ChestBlockEntity) level.getBlockEntity(chest);
                    h.assertTrue(
                            supplies != null
                                    && supplies.countItem(Items.BONE_MEAL) >= 8
                                    && supplies.countItem(EcologyModule.SPORE_CULTURE.get()) >= 4
                                    && supplies.countItem(Items.IRON_AXE) == 1
                                    && supplies.countItem(Items.SHEARS) == 1,
                            "bone meal, culture, an axe and shears in the chest");
                    h.assertTrue(level.getBlockState(root).is(Blocks.OAK_LOG), "a living oak");
                    h.assertTrue(
                            level.getBlockState(exhibit.at(new BlockPos(90, -60, -8)))
                                            .getBlock()
                                    instanceof InfectedLogBlock,
                            "its infected twin");
                    h.assertTrue(
                            level.getBlockState(cell.above()).is(Blocks.DISPENSER),
                            "a dispenser of bone meal on the cell");
                    // the button: a redstone block where it would power the dispenser
                    level.setBlock(cell.above().west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                })
                .thenWaitUntil(() -> h.assertTrue(
                        level.getBlockState(cell).getValue(LivingSubstrateBlock.VIGOR) > 0, "the cell was fed"))
                .thenWaitUntil(() -> h.assertTrue(
                        level.getBlockState(root).getBlock() instanceof InfectedLogBlock, "the oak turned"))
                .thenWaitUntil(() -> h.assertFalse(snapshot(h).works(root), "and the job finished"))
                .thenExecute(() -> {
                    for (BlockPos offset : BlockPos.betweenClosed(-2, 0, -2, 2, 5, 2)) {
                        BlockState turned = level.getBlockState(root.offset(offset));
                        BlockState twin = level.getBlockState(
                                exhibit.at(new BlockPos(90, -60, -8)).offset(offset));
                        h.assertValueEqual(
                                turned.getBlock(), twin.getBlock(), "the turned oak matches its twin at " + offset);
                    }
                    // the giant spruce, the birch, the cherry and the treehouse were never touched by this wave
                    h.assertTrue(
                            level.getBlockState(exhibit.at(new BlockPos(112, -60, -9)))
                                    .is(Blocks.SPRUCE_LOG),
                            "spruce");
                    h.assertTrue(
                            level.getBlockState(exhibit.at(new BlockPos(96, -60, -8)))
                                    .is(Blocks.BIRCH_LOG),
                            "birch");
                    h.assertTrue(
                            level.getBlockState(exhibit.at(new BlockPos(101, -60, -8)))
                                    .is(Blocks.CHERRY_LOG),
                            "cherry");
                    exhibit.tidy(h);
                    exhibit.forget(h);
                })
                .thenSucceed();
    }

    // --- fixtures ---

    /** Grass over the whole structure floor, at y 1. */
    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
    }

    /** A far, forced plot of {@code width} by {@code depth} grass at y {@value #YARD_Y}, one slot per test. */
    private static final class Yard {
        private final BlockPos origin;
        private final int width;
        private final int depth;
        private final List<ChunkPos> chunks = new ArrayList<>();

        Yard(GameTestHelper h, int slot, int width, int depth) {
            this.origin = new BlockPos(YARD_X, YARD_Y, YARD_Z + 64 * slot);
            this.width = width;
            this.depth = depth;
            for (int x = origin.getX() >> 4; x <= (origin.getX() + width - 1) >> 4; x++)
                for (int z = origin.getZ() >> 4; z <= (origin.getZ() + depth - 1) >> 4; z++)
                    chunks.add(new ChunkPos(x, z));
            for (ChunkPos chunk : chunks) TestChunks.force(h.getLevel(), chunk);
            for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(width - 1, 0, depth - 1)))
                h.getLevel().setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }

        BlockPos at(int x, int y, int z) {
            return origin.offset(x, y, z);
        }

        void clear(GameTestHelper h) {
            for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(width - 1, 40, depth - 1)))
                h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (ChunkPos chunk : chunks) h.getLevel().setChunkForced(chunk.x, chunk.z, false);
        }
    }

    /** The demo's five-log oak: two wide layers, a small cap. */
    private static Tree oak(ServerLevel level, BlockPos base) {
        return tree(level, base, Blocks.OAK_LOG, Blocks.OAK_LEAVES, 5, List.of(2, 3), 4);
    }

    /** The demo's six-log birch. */
    private static Tree birch(ServerLevel level, BlockPos base) {
        return tree(level, base, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 6, List.of(3, 4), 5);
    }

    /** The tree with the leaf distances vanilla has settled on since it was placed. */
    private static Tree settled(ServerLevel level, Tree tree) {
        Map<BlockPos, Integer> leaves = new HashMap<>();
        tree.leaves()
                .forEach((pos, distance) ->
                        leaves.put(pos, level.getBlockState(pos).getValue(LeavesBlock.DISTANCE)));
        return new Tree(tree.logBlock(), tree.leafBlock(), tree.logs(), leaves);
    }

    /** A dark oak: a 2x2 trunk of four logs high under a 6x6 crown. */
    private static Tree darkOak(ServerLevel level, BlockPos base) {
        List<BlockPos> logs = new ArrayList<>();
        for (int y = 0; y < 4; y++)
            for (BlockPos pos :
                    BlockPos.betweenClosed(base.above(y), base.above(y).offset(1, 0, 1))) logs.add(pos.immutable());
        Map<BlockPos, Integer> leaves = new HashMap<>();
        for (int y = 3; y <= 4; y++)
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-2, y, -2), base.offset(3, y, 3))) {
                int dx = Math.max(0, Math.max(base.getX() - pos.getX(), pos.getX() - base.getX() - 1));
                int dz = Math.max(0, Math.max(base.getZ() - pos.getZ(), pos.getZ() - base.getZ() - 1));
                int distance = dx + dz + (y == 4 ? 1 : 0);
                if (distance == 0 || distance > 3) continue;
                leaves.put(pos.immutable(), distance);
            }
        return place(level, new Tree(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES, logs, leaves));
    }

    private static Tree tree(
            ServerLevel level, BlockPos base, Block log, Block leaf, int height, List<Integer> wide, int top) {
        List<BlockPos> logs = new ArrayList<>();
        for (int y = 0; y < height; y++) logs.add(base.above(y));
        Map<BlockPos, Integer> leaves = new HashMap<>();
        for (int y : wide)
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    int distance = Math.abs(dx) + Math.abs(dz);
                    if (distance == 0 || distance > 3) continue;
                    leaves.put(base.offset(dx, y, dz), distance);
                }
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                int distance = Math.abs(dx) + Math.abs(dz);
                if (distance == 0) continue;
                leaves.put(base.offset(dx, top, dz), distance);
            }
        leaves.put(base.above(top + 1), 1);
        for (Direction side : Direction.Plane.HORIZONTAL)
            leaves.put(base.above(top + 1).relative(side), 2);
        return place(level, new Tree(log, leaf, logs, leaves));
    }

    private static Tree place(ServerLevel level, Tree tree) {
        for (BlockPos log : tree.logs()) level.setBlock(log, tree.logBlock().defaultBlockState(), Block.UPDATE_ALL);
        tree.leaves()
                .forEach((pos, distance) -> level.setBlock(
                        pos,
                        tree.leafBlock()
                                .defaultBlockState()
                                .setValue(LeavesBlock.DISTANCE, distance)
                                .setValue(LeavesBlock.PERSISTENT, false),
                        Block.UPDATE_ALL));
        return tree;
    }

    private static TreeInfectionSnapshot snapshot(GameTestHelper h) {
        return EcologyModule.treeInfections(h.getLevel());
    }

    /** The job on the tree rooted at {@code root}; the test fails when there is none. */
    private static TreeInfectionSnapshot.Job job(GameTestHelper h, BlockPos root) {
        return snapshot(h).jobs().stream()
                .filter(job -> job.root().equals(root))
                .findFirst()
                .orElseGet(() -> {
                    h.fail("no job on " + root + ": " + snapshot(h).jobs());
                    return null;
                });
    }

    /** Every log is the one Infected Log with its axis, every leaf the one Blighted Leaves with its distance. */
    private static void assertConverted(GameTestHelper h, Tree tree) {
        ServerLevel level = h.getLevel();
        for (BlockPos log : tree.logs()) {
            BlockState state = level.getBlockState(log);
            h.assertValueEqual(
                    state, EcologyModule.INFECTED_LOG.get().defaultBlockState(), "an infected log at " + log);
            h.assertValueEqual(state.getValue(RotatedPillarBlock.AXIS), Direction.Axis.Y, "axis at " + log);
        }
        tree.leaves().forEach((pos, distance) -> {
            BlockState state = level.getBlockState(pos);
            h.assertTrue(state.getBlock() instanceof BlightedLeavesBlock, "blighted leaves at " + pos + ": " + state);
            h.assertValueEqual(state.getValue(LeavesBlock.DISTANCE), distance, "distance at " + pos);
            h.assertValueEqual(state.getValue(LeavesBlock.PERSISTENT), false, "not persistent at " + pos);
        });
    }

    private static void assertUntouched(GameTestHelper h, Tree tree) {
        ServerLevel level = h.getLevel();
        for (BlockPos log : tree.logs())
            h.assertTrue(level.getBlockState(log).is(tree.logBlock()), "a living log at " + log);
        tree.leaves()
                .forEach((pos, distance) ->
                        h.assertTrue(level.getBlockState(pos).is(tree.leafBlock()), "living leaves at " + pos));
    }

    private static int infectedBlocks(ServerLevel level, Tree tree) {
        int count = 0;
        for (BlockPos log : tree.logs()) if (level.getBlockState(log).getBlock() instanceof InfectedLogBlock) count++;
        for (BlockPos leaf : tree.leaves().keySet())
            if (level.getBlockState(leaf).getBlock() instanceof BlightedLeavesBlock) count++;
        return count;
    }

    /** No item and no experience inside the structure: conversion is a replacement, never a harvest. */
    private static void assertNothingSpawned(GameTestHelper h) {
        AABB box = h.getBounds().inflate(1);
        List<ItemEntity> items = h.getLevel().getEntitiesOfClass(ItemEntity.class, box);
        h.assertTrue(items.isEmpty(), "infection dropped " + items);
        List<ExperienceOrb> orbs = h.getLevel().getEntitiesOfClass(ExperienceOrb.class, box);
        h.assertTrue(orbs.isEmpty(), "infection gave experience " + orbs);
    }

    /** Where the old build's fixture chunk stood: its own chunk, far from every yard. */
    private static final BlockPos LEGACY_ORIGIN = new BlockPos(262144, 80, 262144);

    /** The oak the old build half turned in the fixture: the demo's five-log oak at {@code base}. */
    private static Tree legacyOak(BlockPos base) {
        List<BlockPos> logs = new ArrayList<>();
        for (int y = 0; y < 5; y++) logs.add(base.above(y));
        Map<BlockPos, Integer> leaves = new HashMap<>();
        for (int y = 2; y <= 3; y++)
            for (int dx = -2; dx <= 2; dx++)
                for (int dz = -2; dz <= 2; dz++) {
                    int distance = Math.abs(dx) + Math.abs(dz);
                    if (distance > 0 && distance <= 3) leaves.put(base.offset(dx, y, dz), distance);
                }
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                if (dx != 0 || dz != 0) leaves.put(base.offset(dx, 4, dz), Math.abs(dx) + Math.abs(dz));
        leaves.put(base.above(5), 1);
        for (Direction side : Direction.Plane.HORIZONTAL)
            leaves.put(base.above(5).relative(side), 2);
        return new Tree(Blocks.OAK_LOG, Blocks.OAK_LEAVES, logs, leaves);
    }

    private static BlockState log(Direction.Axis axis) {
        return EcologyModule.INFECTED_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState blighted(int distance, boolean persistent, boolean waterlogged) {
        return EcologyModule.BLIGHTED_LEAVES
                .get()
                .defaultBlockState()
                .setValue(LeavesBlock.DISTANCE, distance)
                .setValue(LeavesBlock.PERSISTENT, persistent)
                .setValue(LeavesBlock.WATERLOGGED, waterlogged);
    }

    /** A file of the old build's fixture, {@code data/infestusfrontier_tests/fixture/legacy_timber/<name>}. */
    private static CompoundTag fixture(GameTestHelper h, String name) {
        var id = ResourceLocation.fromNamespaceAndPath("infestusfrontier_tests", "fixture/legacy_timber/" + name);
        try (InputStream in = h.getLevel()
                .getServer()
                .getResourceManager()
                .getResourceOrThrow(id)
                .open()) {
            return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    /** The old build's saved job file, as the world's data folder held it. */
    private static CompoundTag fixtureFile(GameTestHelper h) {
        return fixture(h, "tree_infections.dat");
    }

    /** The job file {@code file}, read through a data storage as a world's data folder is. */
    private static SavedData legacyJobs(GameTestHelper h, CompoundTag file) {
        try {
            var directory = Files.createTempDirectory("infestus-legacy-trees-");
            Path saved = directory.resolve(EcologyModule.TREE_INFECTIONS + ".dat");
            try {
                NbtIo.writeCompressed(file, saved);
                var registries = h.getLevel().registryAccess();
                var factory = new SavedData.Factory<>(
                        () -> EcologyModule.restoreTreeInfections(new CompoundTag(), registries),
                        EcologyModule::restoreTreeInfections);
                var storage = new DimensionDataStorage(
                        directory.toFile(), h.getLevel().getServer().getFixerUpper(), registries);
                SavedData loaded = storage.get(factory, EcologyModule.TREE_INFECTIONS);
                h.assertTrue(loaded != null, "the old file loads");
                return loaded;
            } finally {
                // the storage's read queues a clean-up of this folder on the IO worker; it must finish first
                IOUtilities.waitUntilIOWorkerComplete();
                Files.deleteIfExists(saved);
                Files.delete(directory);
            }
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    /**
     * The {@code Properties} of a state in the first saved job: {@code key} is {@code root_state} ({@code index} unused)
     * or {@code states}, entry {@code index}. Created when the state has none.
     */
    private static CompoundTag properties(CompoundTag saved, String key, int index) {
        CompoundTag job = saved.getList("jobs", 10).getCompound(0);
        CompoundTag state =
                index < 0 ? job.getCompound(key) : job.getList(key, 10).getCompound(index);
        if (!state.contains("Properties")) state.put("Properties", new CompoundTag());
        return state.getCompound("Properties");
    }

    /** {@code saved} reads as no job, and its list is kept as it was through two saves and reloads. */
    private static void assertKeptOpaque(GameTestHelper h, CompoundTag saved) {
        var registries = h.getLevel().registryAccess();
        CompoundTag again =
                EcologyModule.restoreTreeInfections(saved, registries).save(new CompoundTag(), registries);
        h.assertValueEqual(again.get("unreadable_jobs"), saved.get("jobs"), "kept as it was: " + saved);
        h.assertValueEqual(again.getList("jobs", 10).size(), 0, "and no job was invented");
        CompoundTag twice =
                EcologyModule.restoreTreeInfections(again, registries).save(new CompoundTag(), registries);
        h.assertValueEqual(twice.get("unreadable_jobs"), saved.get("jobs"), "and kept through a second save");
        h.assertValueEqual(twice.getList("jobs", 10).size(), 0, "still with no job");
    }

    /** Writes {@code saved} to disk through a data storage of its own, reads it back with another, and returns it. */
    private static SavedData saveAndReload(GameTestHelper h, CompoundTag saved) {
        try {
            var directory = Files.createTempDirectory("infestus-tree-infections-");
            try {
                var registries = h.getLevel().registryAccess();
                var fixer = h.getLevel().getServer().getFixerUpper();
                var factory = new SavedData.Factory<>(
                        () -> EcologyModule.restoreTreeInfections(new CompoundTag(), registries),
                        EcologyModule::restoreTreeInfections);
                var storage = new DimensionDataStorage(directory.toFile(), fixer, registries);
                var data = EcologyModule.restoreTreeInfections(saved, registries);
                data.setDirty();
                storage.set("trees", data);
                storage.save();
                IOUtilities.waitUntilIOWorkerComplete();
                var reopened = new DimensionDataStorage(directory.toFile(), fixer, registries);
                var loaded = reopened.get(factory, "trees");
                h.assertTrue(loaded != null, "the file exists and loads");
                return loaded;
            } finally {
                IOUtilities.waitUntilIOWorkerComplete();
                Files.deleteIfExists(directory.resolve("trees.dat"));
                Files.delete(directory);
            }
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
}
