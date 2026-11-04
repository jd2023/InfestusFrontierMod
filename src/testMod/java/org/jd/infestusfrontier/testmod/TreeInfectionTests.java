package org.jd.infestusfrontier.testmod;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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
import org.jd.infestusfrontier.ecology.TreeSource;
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

    /** A tree as placed: the trunk and the leaves with their distances. */
    private record Tree(TreeSource species, List<BlockPos> logs, Map<BlockPos, Integer> leaves) {
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
     * Only an upright supported log on host ground is a root: a lying log, a log on sand, a stripped log, wood, planks
     * and an Infected Log start nothing, and ground under any of them is still no host.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH + "3", timeoutTicks = 100)
    public static void onlyAnUprightRootedSupportedLogStartsAJob(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        List<BlockState> notRoots = List.of(
                Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X),
                Blocks.STRIPPED_OAK_LOG.defaultBlockState(),
                Blocks.OAK_WOOD.defaultBlockState(),
                Blocks.OAK_PLANKS.defaultBlockState(),
                EcologyModule.INFECTED_LOG.get().defaultBlockState(),
                Blocks.CHERRY_LOG.defaultBlockState(),
                Blocks.MANGROVE_LOG.defaultBlockState());
        Player gardener =
                EcologyTests.holding(h, GameType.CREATIVE, EcologyModule.SPORE_CULTURE.get(), notRoots.size() + 1);
        for (int i = 0; i < notRoots.size(); i++) {
            BlockPos trunk = new BlockPos(2 + 2 * i, 2, 4);
            h.setBlock(trunk, notRoots.get(i));
            h.setBlock(trunk.above(), notRoots.get(i));
            EcologyTests.useCulture(level, gardener, h.absolutePos(trunk.below().north()));
            EcologyTests.assertStage(h, trunk.below().north(), SubstrateStage.BASIC);
            h.assertValueEqual(h.getBlockState(trunk), notRoots.get(i), "stands as it was: " + notRoots.get(i));
        }
        // an upright oak log on sandstone is not rooted
        h.setBlock(new BlockPos(2, 1, 9), Blocks.SANDSTONE);
        h.setBlock(new BlockPos(2, 2, 9), Blocks.OAK_LOG);
        EcologyTests.useCulture(level, gardener, h.absolutePos(new BlockPos(1, 1, 9)));
        h.runAfterDelay(10, () -> {
            for (TreeInfectionSnapshot.Job job : snapshot(h).jobs())
                h.assertFalse(h.getBounds().contains(job.root().getCenter()), "a job on a trunk here: " + job);
            for (TreeInfectionSnapshot.Refusal refusal : snapshot(h).refusals())
                h.assertFalse(h.getBounds().contains(refusal.root().getCenter()), "a refusal here: " + refusal);
            h.assertBlockPresent(Blocks.OAK_LOG, new BlockPos(2, 2, 9));
            h.assertBlockPresent(Blocks.SANDSTONE, new BlockPos(2, 1, 9));
            for (int i = 0; i < notRoots.size(); i++) {
                BlockPos trunk = new BlockPos(2 + 2 * i, 2, 4);
                h.assertValueEqual(h.getBlockState(trunk), notRoots.get(i), "still stands: " + notRoots.get(i));
                h.assertBlockPresent(Blocks.GRASS_BLOCK, trunk.below());
            }
            h.succeed();
        });
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
        Tree[] standing = {new Tree(oak.species(), oak.logs(), rest)};
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
                    assertConverted(h, settled(level, new Tree(oak.species(), oak.logs(), rest)));
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
        Tree oak = new Tree(grown.species(), grown.logs(), rest);
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
                TreeSource.OAK,
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

    // --- harvest ---

    /** Cutting a turned oak gives one Infected Log a log and, with shears, one oak leaves a leaf: one harvest each. */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = BATCH + "13",
            timeoutTicks = 300)
    public static void aTurnedTreeHarvestsOnceAsInfectedTimberAndItsOwnLeaves(GameTestHelper h) {
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
                                    Items.OAK_LEAVES,
                                    oak.leaves().size()),
                            "one item a block, as the living tree gives with shears");
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
                    // the giant spruce, the second birch and the treehouse's planks were never touched by this wave
                    h.assertTrue(
                            level.getBlockState(exhibit.at(new BlockPos(112, -60, -9)))
                                    .is(Blocks.SPRUCE_LOG),
                            "spruce");
                    h.assertTrue(
                            level.getBlockState(exhibit.at(new BlockPos(101, -60, -8)))
                                    .is(Blocks.BIRCH_LOG),
                            "birch");
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
        return tree(level, base, TreeSource.OAK, 5, List.of(2, 3), 4);
    }

    /** The demo's six-log birch. */
    private static Tree birch(ServerLevel level, BlockPos base) {
        return tree(level, base, TreeSource.BIRCH, 6, List.of(3, 4), 5);
    }

    /** The tree with the leaf distances vanilla has settled on since it was placed. */
    private static Tree settled(ServerLevel level, Tree tree) {
        Map<BlockPos, Integer> leaves = new HashMap<>();
        tree.leaves()
                .forEach((pos, distance) ->
                        leaves.put(pos, level.getBlockState(pos).getValue(LeavesBlock.DISTANCE)));
        return new Tree(tree.species(), tree.logs(), leaves);
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
        return place(level, new Tree(TreeSource.DARK_OAK, logs, leaves));
    }

    private static Tree tree(
            ServerLevel level, BlockPos base, TreeSource species, int height, List<Integer> wide, int top) {
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
        return place(level, new Tree(species, logs, leaves));
    }

    private static Tree place(ServerLevel level, Tree tree) {
        for (BlockPos log : tree.logs())
            level.setBlock(log, tree.species().log().defaultBlockState(), Block.UPDATE_ALL);
        tree.leaves()
                .forEach((pos, distance) -> level.setBlock(
                        pos,
                        tree.species()
                                .leaves()
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

    /** Every log is an Infected Log of the species with its axis, every leaf Blighted Leaves with its distance. */
    private static void assertConverted(GameTestHelper h, Tree tree) {
        ServerLevel level = h.getLevel();
        for (BlockPos log : tree.logs()) {
            BlockState state = level.getBlockState(log);
            h.assertTrue(state.getBlock() instanceof InfectedLogBlock, "an infected log at " + log + ": " + state);
            h.assertValueEqual(state.getValue(InfectedLogBlock.SOURCE), tree.species(), "species at " + log);
            h.assertValueEqual(state.getValue(RotatedPillarBlock.AXIS), Direction.Axis.Y, "axis at " + log);
        }
        tree.leaves().forEach((pos, distance) -> {
            BlockState state = level.getBlockState(pos);
            h.assertTrue(state.getBlock() instanceof BlightedLeavesBlock, "blighted leaves at " + pos + ": " + state);
            h.assertValueEqual(state.getValue(BlightedLeavesBlock.SOURCE), tree.species(), "species at " + pos);
            h.assertValueEqual(state.getValue(LeavesBlock.DISTANCE), distance, "distance at " + pos);
            h.assertValueEqual(state.getValue(LeavesBlock.PERSISTENT), false, "not persistent at " + pos);
        });
    }

    private static void assertUntouched(GameTestHelper h, Tree tree) {
        ServerLevel level = h.getLevel();
        for (BlockPos log : tree.logs())
            h.assertTrue(level.getBlockState(log).is(tree.species().log()), "a living log at " + log);
        tree.leaves()
                .forEach((pos, distance) ->
                        h.assertTrue(level.getBlockState(pos).is(tree.species().leaves()), "living leaves at " + pos));
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
