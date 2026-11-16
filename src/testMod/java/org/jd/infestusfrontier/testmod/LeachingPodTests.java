package org.jd.infestusfrontier.testmod;

import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.done;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.id;
import static org.jd.infestusfrontier.testmod.StarterAdvancementTests.player;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.mining.Leaching;
import org.jd.infestusfrontier.discovery.PendingCredits;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.mining.LeachedRockBlock;
import org.jd.infestusfrontier.mining.LeachingPodBlock;
import org.jd.infestusfrontier.mining.MiningModule;
import org.jd.infestusfrontier.organ.OrganModule;

/**
 * The Leaching Pod and Leached Rock: which ground a pod changes and which it leaves, the discs and layers it works, its
 * progress through a real chunk unload, and the way it is grown, taught and credited. Each pod stands in a chunk of its
 * own, far from the test structures: it digs down further than a structure is tall.
 */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class LeachingPodTests {
    private static final int TOP = 100;
    private static final int BOX = 4;
    /** Ticks a whole run of layers takes, with a margin. */
    private static final int RUN_TICKS = 700;

    /** A pod's yard: the pod at {@code pod}, in the middle of a chunk, with its own chunk forced. */
    private record Site(ServerLevel level, BlockPos pod, ChunkPos chunk) {
        static Site of(GameTestHelper h, int slot) {
            BlockPos origin = h.absolutePos(BlockPos.ZERO);
            BlockPos pod =
                    new BlockPos((origin.getX() & ~15) + 192008, TOP, (origin.getZ() & ~15) + 230408 + slot * 64);
            Site site = new Site(h.getLevel(), pod, new ChunkPos(pod));
            TestChunks.force(site.level, site.chunk);
            site.clear();
            return site;
        }

        BlockPos at(int dx, int below, int dz) {
            return pod.offset(dx, -below, dz);
        }

        void set(int dx, int below, int dz, BlockState state) {
            level.setBlockAndUpdate(at(dx, below, dz), state);
        }

        /** Every cell of the yard from one layer above the pod to {@code layers} below it, as one block. */
        void fill(int fromBelow, int toBelow, BlockState state) {
            for (int below = fromBelow; below <= toBelow; below++)
                for (int dx = -BOX; dx <= BOX; dx++) for (int dz = -BOX; dz <= BOX; dz++) set(dx, below, dz, state);
        }

        /** A pod on a substrate cell over {@code layers} layers of {@code ground}. */
        void plant(int layers, BlockState ground) {
            fill(1, layers + 1, ground);
            set(0, 1, 0, EcologyTests.mature());
            level.setBlockAndUpdate(pod, MiningModule.LEACHING_POD.get().defaultBlockState());
        }

        void clear() {
            for (int below = -2; below <= Leaching.MAX_LAYERS + 3; below++)
                for (int dx = -BOX - 1; dx <= BOX + 1; dx++)
                    for (int dz = -BOX - 1; dz <= BOX + 1; dz++) {
                        BlockPos cell = at(dx, below, dz);
                        level.removeBlockEntity(cell);
                        level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
        }

        void release() {
            clear();
            level.setChunkForced(chunk.x, chunk.z, false);
        }

        CompoundTag saved() {
            return level.getBlockEntity(pod).saveWithFullMetadata(level.registryAccess());
        }

        boolean spent() {
            return level.getBlockState(pod).getValue(LeachingPodBlock.SPENT);
        }

        /** Leached Rock in the yard, below the pod. */
        int leached() {
            int count = 0;
            for (int below = 0; below <= Leaching.MAX_LAYERS + 3; below++)
                for (int dx = -BOX; dx <= BOX; dx++)
                    for (int dz = -BOX; dz <= BOX; dz++)
                        if (level.getBlockState(at(dx, below, dz)).is(MiningModule.LEACHED_ROCK.get())) count++;
            return count;
        }

        /** Leached Rock in layer {@code below} below the pod. */
        int leachedIn(int below) {
            int count = 0;
            for (int dx = -BOX; dx <= BOX; dx++)
                for (int dz = -BOX; dz <= BOX; dz++)
                    if (level.getBlockState(at(dx, below, dz)).is(MiningModule.LEACHED_ROCK.get())) count++;
            return count;
        }
    }

    private static BlockState state(Block block) {
        return block.defaultBlockState();
    }

    // ---- what a pod changes ----

    /**
     * Sixteen layers of mixed ground. A layer is a disc of 37 columns inside the 7-wide square; tagged ground no
     * harder than stone becomes Leached Rock; ore, deepslate, a chest, fluids, obsidian, a block with a block entity
     * and air stay; a layer that changes nothing costs no charge; the layer after the sixteenth and everything outside
     * the discs stays untouched, and the pod is spent only when its layers are.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = RUN_TICKS + 100)
    public static void aPodLeachesOnlyTaggedSoftGroundInSevenWideDiscsDownToSixteenLayers(GameTestHelper h) {
        Site site = Site.of(h, 0);
        site.fill(2, Leaching.MAX_LAYERS, state(Blocks.STONE));
        site.fill(Leaching.MAX_LAYERS + 1, Leaching.MAX_LAYERS + 1, state(Blocks.DIRT));
        site.fill(0, 1, state(Blocks.DIRT));
        site.set(0, 1, 0, EcologyTests.mature());
        site.set(0, 0, 0, state(Blocks.AIR));
        // layer 2: ore, a chest, water and obsidian stand in the stone
        site.set(1, 2, 0, state(Blocks.IRON_ORE));
        site.set(-2, 2, 1, state(Blocks.DIAMOND_ORE));
        site.set(0, 2, 2, state(Blocks.CHEST));
        ((ChestBlockEntity) site.level.getBlockEntity(site.at(0, 2, 2))).setItem(0, new ItemStack(Items.BONE, 7));
        site.set(2, 2, -1, state(Blocks.WATER));
        site.set(0, 2, -2, state(Blocks.OBSIDIAN));
        // layer 3: deepslate throughout, a tagged block that is too hard
        site.fill(3, 3, state(Blocks.DEEPSLATE));
        // layer 4: loose and sandy ground, and a block with a block entity
        site.fill(4, 4, state(Blocks.SAND));
        for (int dx = -BOX; dx <= BOX; dx += 2)
            for (int dz = -BOX; dz <= BOX; dz += 2) site.set(dx, 4, dz, state(Blocks.GRAVEL));
        site.set(0, 4, 1, state(Blocks.SANDSTONE));
        site.set(1, 4, 1, state(Blocks.SUSPICIOUS_SAND));
        // layer 5: the Nether's ground, and a quartz ore
        site.fill(5, 5, state(Blocks.NETHERRACK));
        site.set(1, 5, 0, state(Blocks.BASALT));
        site.set(-1, 5, 0, state(Blocks.BLACKSTONE));
        site.set(0, 5, 1, state(Blocks.NETHER_QUARTZ_ORE));
        // layer 6: the other base stones, a cave and lava
        site.fill(6, 6, state(Blocks.ANDESITE));
        site.set(1, 6, 0, state(Blocks.GRANITE));
        site.set(-1, 6, 0, state(Blocks.DIORITE));
        site.set(0, 6, 1, state(Blocks.TUFF));
        site.set(1, 6, -1, state(Blocks.AIR));
        site.set(-1, 6, -1, state(Blocks.LAVA));
        // layer 7: dirt of several kinds
        site.fill(7, 7, state(Blocks.DIRT));
        site.set(1, 7, 0, state(Blocks.GRASS_BLOCK));
        site.set(-1, 7, 0, state(Blocks.PODZOL));
        site.set(0, 7, 1, state(Blocks.COARSE_DIRT));
        site.level.setBlockAndUpdate(site.pod, MiningModule.LEACHING_POD.get().defaultBlockState());
        List<BlockState> before = snapshot(site);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(site.spent(), "the pod has done its work"))
                .thenExecute(() -> {
                    int[] expected = new int[Leaching.MAX_LAYERS + 2];
                    expected[1] = Leaching.MAX_BLOCKS_PER_LAYER;
                    expected[2] = Leaching.MAX_BLOCKS_PER_LAYER - 5;
                    expected[3] = 0;
                    expected[4] = Leaching.MAX_BLOCKS_PER_LAYER - 1;
                    expected[5] = Leaching.MAX_BLOCKS_PER_LAYER - 1;
                    expected[6] = Leaching.MAX_BLOCKS_PER_LAYER - 2;
                    expected[7] = Leaching.MAX_BLOCKS_PER_LAYER;
                    for (int below = 8; below <= Leaching.MAX_LAYERS; below++)
                        expected[below] = Leaching.MAX_BLOCKS_PER_LAYER;
                    int total = 0;
                    for (int below = 1; below <= Leaching.MAX_LAYERS; below++) {
                        h.assertValueEqual(site.leachedIn(below), expected[below], "layer " + below + " leached");
                        total += expected[below];
                    }
                    h.assertValueEqual(site.leached(), total, "no Leached Rock outside the sixteen discs");
                    int index = 0;
                    for (int below = -1; below <= Leaching.MAX_LAYERS + 2; below++)
                        for (int dx = -BOX; dx <= BOX; dx++)
                            for (int dz = -BOX; dz <= BOX; dz++) {
                                BlockPos cell = site.at(dx, below, dz);
                                BlockState was = before.get(index++);
                                boolean inDisc = Leaching.DISC.contains(new Leaching.Cell(dx, dz));
                                boolean worked = inDisc && below >= 1 && below <= Leaching.MAX_LAYERS;
                                if (!worked || !was.is(MiningModule.LEACHABLE) || !isSoft(site, cell, was))
                                    h.assertTrue(
                                            site.level.getBlockState(cell).equals(was) || cell.equals(site.pod),
                                            "untouched: " + cell + " was " + was);
                            }
                    h.assertTrue(
                            site.level.getBlockState(site.at(1, 2, 0)).is(Blocks.IRON_ORE), "the iron ore is in place");
                    h.assertTrue(
                            site.level.getBlockState(site.at(-2, 2, 1)).is(Blocks.DIAMOND_ORE),
                            "the diamond ore is in place");
                    h.assertTrue(
                            ((ChestBlockEntity) site.level.getBlockEntity(site.at(0, 2, 2))).countItem(Items.BONE) == 7,
                            "the chest still holds its bones");
                    h.assertTrue(site.level.getBlockState(site.at(2, 2, -1)).is(Blocks.WATER), "the water stays water");
                    h.assertTrue(site.level.getBlockState(site.at(-1, 6, -1)).is(Blocks.LAVA), "the lava stays lava");
                    h.assertTrue(
                            site.level.getBlockState(site.at(0, 3, 0)).is(Blocks.DEEPSLATE),
                            "deepslate is too hard for a pod");
                    h.assertTrue(
                            site.level.getBlockState(site.at(1, 4, 1)).is(Blocks.SUSPICIOUS_SAND),
                            "a block with a block entity stays");
                    h.assertTrue(
                            site.level.getBlockState(site.at(0, 1, 0)).is(MiningModule.LEACHED_ROCK.get()),
                            "the first layer includes the substrate under the pod");
                    h.assertTrue(
                            site.level
                                    .getBlockState(site.at(0, Leaching.MAX_LAYERS + 1, 0))
                                    .is(Blocks.DIRT),
                            "the layer after the sixteenth is untouched");
                    h.assertTrue(
                            site.level.getBlockState(site.at(3, 1, 3)).is(Blocks.DIRT),
                            "a corner of the square is not in the disc");
                    h.assertTrue(
                            site.level.getBlockState(site.at(4, 1, 0)).is(Blocks.DIRT),
                            "the disc is seven wide, not nine");
                    CompoundTag tag = site.saved();
                    h.assertValueEqual(tag.getInt("layers"), Leaching.MAX_LAYERS, "sixteen layers worked");
                    h.assertValueEqual(
                            tag.getInt("charges"), 1, "fifteen charges paid: the layer that changed nothing was free");
                    boolean glowing = false;
                    for (int below = 1; below <= Leaching.MAX_LAYERS && !glowing; below++)
                        for (int dx = -3; dx <= 3; dx++)
                            for (int dz = -3; dz <= 3; dz++) {
                                BlockState rock = site.level.getBlockState(site.at(dx, below, dz));
                                if (rock.is(MiningModule.LEACHED_ROCK.get())
                                        && rock.getValue(LeachedRockBlock.GLOW) > 0) glowing = true;
                            }
                    h.assertTrue(glowing, "a share of the rock glows faintly");
                    site.release();
                })
                .thenSucceed();
    }

    /** What the pod's rule says about {@code was} at {@code cell}: soft enough, no fluid, no block entity. */
    private static boolean isSoft(Site site, BlockPos cell, BlockState was) {
        return Leaching.softEnough(was.getDestroySpeed(site.level, cell))
                && was.getFluidState().isEmpty()
                && !was.hasBlockEntity();
    }

    /** Every cell of the box from one layer above the pod to two below the sixteenth, in a fixed order. */
    private static List<BlockState> snapshot(Site site) {
        List<BlockState> cells = new java.util.ArrayList<>();
        for (int below = -1; below <= Leaching.MAX_LAYERS + 2; below++)
            for (int dx = -BOX; dx <= BOX; dx++)
                for (int dz = -BOX; dz <= BOX; dz++) cells.add(site.level.getBlockState(site.at(dx, below, dz)));
        return cells;
    }

    // ---- Leached Rock ----

    /** Translucent and solid, breaks in a few ticks by hand, drops nothing even with a pick, glows faintly at most. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void leachedRockIsTranslucentSolidBreaksByHandAndDropsNothing(GameTestHelper h) {
        BlockPos cell = new BlockPos(1, 1, 1);
        h.setBlock(cell, MiningModule.LEACHED_ROCK.get());
        BlockPos at = h.absolutePos(cell);
        BlockState rock = h.getBlockState(cell);
        h.assertFalse(rock.canOcclude(), "it does not hide what is behind it");
        h.assertTrue(rock.propagatesSkylightDown(h.getLevel(), at), "light passes through it, like glass");
        h.assertTrue(rock.isCollisionShapeFullBlock(h.getLevel(), at), "it is a solid cube");
        for (Direction side : Direction.values())
            h.assertTrue(rock.isFaceSturdy(h.getLevel(), at, side), "no fluid moves through its " + side + " face");
        h.assertTrue(rock.getFluidState().isEmpty(), "it holds no fluid");
        Player hand = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        hand.setOnGround(true);
        int ticks = (int) Math.ceil(1 / rock.getDestroyProgress(hand, h.getLevel(), at));
        h.assertTrue(ticks <= 4, "it breaks almost at once by hand, in " + ticks + " ticks");
        for (ItemStack tool : List.of(ItemStack.EMPTY, new ItemStack(Items.DIAMOND_PICKAXE)))
            h.assertTrue(
                    Block.getDrops(rock, h.getLevel(), at, null, null, tool).isEmpty(), "it drops nothing for " + tool);
        for (int glow = 0; glow <= 2; glow++) {
            int light = rock.setValue(LeachedRockBlock.GLOW, glow).getLightEmission();
            h.assertTrue(light <= 4, "glow " + glow + " is faint: " + light);
            if (glow > 0)
                h.assertTrue(
                        light > rock.setValue(LeachedRockBlock.GLOW, glow - 1).getLightEmission(),
                        "glow " + glow + " is brighter than the one before");
        }
        ServerPlayer miner = TestDrops.miner(h, ItemStack.EMPTY);
        h.assertTrue(miner.gameMode.destroyBlock(at), "a player breaks it");
        h.assertTrue(h.getBlockState(cell).isAir(), "and it is gone");
        h.assertValueEqual(TestDrops.dropped(h).size(), 0, "nothing fell");
        h.succeed();
    }

    // ---- save and load ----

    /**
     * A pod whose chunk leaves and returns: the pod is still a pod, it did not work while gone, and it neither repeats
     * a layer nor pays for one twice. It is spent only when its sixteen layers are done.
     */
    @GameTest(
            templateNamespace = "infestusfrontier_tests",
            template = "empty",
            batch = "leaching_unload",
            timeoutTicks = 1500)
    public static void aPodKeepsItsProgressThroughItsChunkLeavingAndReturning(GameTestHelper h) {
        Site site = Site.of(h, 1);
        site.plant(Leaching.MAX_LAYERS, state(Blocks.STONE));
        int[] before = new int[2];
        h.startSequence()
                .thenWaitUntil(
                        () -> h.assertTrue(site.saved().getInt("layers") >= 3, "the pod has leached three layers"))
                .thenExecute(() -> {
                    CompoundTag tag = site.saved();
                    before[0] = tag.getInt("layers");
                    before[1] = tag.getInt("charges");
                    site.level.setChunkForced(site.chunk.x, site.chunk.z, false);
                })
                .thenWaitUntil(() -> h.assertFalse(site.level.hasChunkAt(site.pod), "the pod's chunk is gone"))
                .thenIdle(5 * Leaching.LAYER_TICKS)
                .thenExecute(() -> TestChunks.force(site.level, site.chunk))
                .thenWaitUntil(() -> {
                    h.assertTrue(site.level.hasChunkAt(site.pod), "the chunk is back");
                    h.assertTrue(site.level.getBlockState(site.pod).is(MiningModule.LEACHING_POD.get()), "the pod");
                    h.assertTrue(site.level.getBlockEntity(site.pod) != null, "with its block entity");
                })
                .thenExecute(() -> {
                    CompoundTag after = site.saved();
                    h.assertFalse(site.spent(), "an unloaded pod is not a spent one");
                    h.assertTrue(
                            after.getInt("layers") >= before[0] && after.getInt("layers") <= before[0] + 1,
                            "it did not work while its chunk was gone: " + before[0] + " then "
                                    + after.getInt("layers"));
                    h.assertValueEqual(
                            after.getInt("charges"),
                            Leaching.MAX_CHARGES - after.getInt("layers"),
                            "one charge a layer, none twice");
                    h.assertValueEqual(
                            site.leached(),
                            after.getInt("layers") * Leaching.MAX_BLOCKS_PER_LAYER,
                            "the ground matches the progress: no layer repeated, none lost");
                })
                .thenWaitUntil(() -> h.assertTrue(site.spent(), "it finishes its work"))
                .thenExecute(() -> {
                    CompoundTag done = site.saved();
                    h.assertValueEqual(done.getInt("layers"), Leaching.MAX_LAYERS, "sixteen layers");
                    h.assertValueEqual(done.getInt("charges"), 0, "sixteen charges, each paid once");
                    h.assertValueEqual(
                            site.leached(),
                            Leaching.MAX_LAYERS * Leaching.MAX_BLOCKS_PER_LAYER,
                            "every layer leached once");
                    site.release();
                })
                .thenSucceed();
    }

    /** A pod loaded from its own saved tag carries on from it; one whose numbers are impossible waits, untouched. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 300)
    public static void aPodLoadsItsSavedProgressAndKeepsUnreadableDataUntouched(GameTestHelper h) {
        Site site = Site.of(h, 2);
        site.plant(4, state(Blocks.STONE));
        CompoundTag progress = site.saved();
        progress.putInt("layers", 2);
        progress.putInt("charges", 5);
        progress.putInt("ticks", 0);
        site.level.getBlockEntity(site.pod).loadWithComponents(progress, site.level.registryAccess());
        Site bad = Site.of(h, 3);
        bad.plant(4, state(Blocks.STONE));
        CompoundTag impossible = bad.saved();
        impossible.putInt("layers", 99);
        impossible.putInt("charges", 3);
        impossible.putInt("ticks", 0);
        bad.level.getBlockEntity(bad.pod).loadWithComponents(impossible, bad.level.registryAccess());
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(site.saved().getInt("layers") >= 3, "it works on from its layers"))
                .thenExecute(() -> {
                    CompoundTag now = site.saved();
                    h.assertValueEqual(
                            now.getInt("charges"), 5 - (now.getInt("layers") - 2), "paying from its charges");
                    h.assertValueEqual(site.leachedIn(1), 0, "layer one was done before, so it is not redone");
                    h.assertValueEqual(site.leachedIn(3), Leaching.MAX_BLOCKS_PER_LAYER, "layer three is the next");
                })
                .thenIdle(2 * Leaching.LAYER_TICKS)
                .thenExecute(() -> {
                    h.assertValueEqual(bad.leached(), 0, "an unreadable pod converts nothing");
                    CompoundTag kept = bad.saved();
                    h.assertValueEqual(kept.getInt("layers"), 99, "and keeps its saved bytes");
                    h.assertValueEqual(kept.getInt("charges"), 3, "all of them");
                    h.assertFalse(bad.spent(), "and is not spent");
                    site.release();
                    bad.release();
                })
                .thenSucceed();
    }

    // ---- how it is obtained, credited and taught ----

    /** A Bud on Living Substrate grows a pod from starter materials; the pod cannot be picked up again. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void aBudGrowsAPodFromSurvivalMaterialsAndABrokenPodLeavesNothing(GameTestHelper h) {
        Site site = Site.of(h, 4);
        site.set(0, 1, 0, EcologyTests.mature());
        site.level.setBlockAndUpdate(site.pod, OrganModule.ORGAN_BUD.get().defaultBlockState());
        Player gardener = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        for (ItemStack material : List.of(
                new ItemStack(Items.SPIDER_EYE),
                new ItemStack(Items.ROTTEN_FLESH, 4),
                new ItemStack(Items.BONE_MEAL, 4))) {
            h.assertTrue(site.level.getBlockState(site.pod).is(OrganModule.ORGAN_BUD.get()), "still a bud");
            gardener.setItemInHand(InteractionHand.MAIN_HAND, material);
            site.level
                    .getBlockState(site.pod)
                    .useItemOn(
                            material,
                            site.level,
                            gardener,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(
                                    Vec3.atCenterOf(site.pod).add(0, 0.5, 0), Direction.UP, site.pod, false));
            h.assertTrue(gardener.getMainHandItem().isEmpty(), "the bud takes " + material);
        }
        h.assertTrue(site.level.getBlockState(site.pod).is(MiningModule.LEACHING_POD.get()), "the pod has grown");
        h.assertFalse(site.spent(), "a new pod is not spent");
        CompoundTag fresh = site.saved();
        h.assertValueEqual(fresh.getInt("layers"), 0, "no layers yet");
        h.assertValueEqual(fresh.getInt("charges"), Leaching.MAX_CHARGES, "all its charges");
        h.assertTrue(
                Block.getDrops(
                                site.level.getBlockState(site.pod),
                                site.level,
                                site.pod,
                                site.level.getBlockEntity(site.pod),
                                null,
                                new ItemStack(Items.DIAMOND_PICKAXE))
                        .isEmpty(),
                "a broken pod leaves nothing");
        site.release();
        h.succeed();
    }

    /** The steward earns Ground Gone Soft when the pod converts its first layer, online or through the credit. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aPodCreditsItsStewardWhenItLeachesItsFirstLayer(GameTestHelper h) {
        Site site = Site.of(h, 5);
        site.plant(3, state(Blocks.STONE));
        ServerPlayer steward = player(h);
        ServerPlayer bystander = player(h);
        PendingCredits credits = PendingCredits.get(h.getLevel());
        BlockState pod = site.level.getBlockState(site.pod);
        pod.getBlock().setPlacedBy(site.level, site.pod, pod, steward, ItemStack.EMPTY);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(site.leachedIn(1) > 0, "the pod leached a layer"))
                .thenExecute(() -> {
                    h.assertTrue(
                            credits.holdsAutomatic(steward.getUUID(), id("leaching_pod")), "the steward is credited");
                    h.assertFalse(credits.holdsAutomatic(bystander.getUUID(), id("leaching_pod")), "nobody else is");
                    credits.award(steward);
                    h.assertTrue(done(h, steward, "contact/leaching_pod"), "Ground Gone Soft is earned");
                    h.assertFalse(done(h, bystander, "contact/leaching_pod"), "only by the steward");
                    site.release();
                })
                .thenSucceed();
    }

    // ---- the demo ----

    /**
     * The demo's Leaching Pod exhibit shows the whole route: a chest of materials and three buds that grow a pod, a
     * live pod on stone and ore that starts leaching, and a finished pit whose ore stands in place inside Leached Rock
     * and whose hand-dug column leaves nothing behind.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void theDemoExhibitShowsTheStarterRouteFromBudToPit(GameTestHelper h) {
        DemoExhibit yard = new DemoExhibit(new BlockPos(30, -63, -61), new BlockPos(60, -54, -52), 60);
        yard.force(h);
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    yard.build(h);
                    ServerLevel level = h.getLevel();
                    var chest = (ChestBlockEntity) level.getBlockEntity(yard.at(new BlockPos(36, -60, -60)));
                    for (var material : List.of(
                            Items.SPIDER_EYE, Items.ROTTEN_FLESH, Items.BONE_MEAL, OrganModule.ORGAN_BUD_ITEM.get()))
                        h.assertTrue(chest != null && chest.countItem(material) > 0, "the chest holds " + material);
                    BlockPos fresh = yard.at(new BlockPos(30, -60, -60));
                    BlockPos half = yard.at(new BlockPos(32, -60, -60));
                    BlockPos fed = yard.at(new BlockPos(34, -60, -60));
                    for (BlockPos bud : List.of(fresh, half, fed))
                        h.assertTrue(level.getBlockState(bud).is(OrganModule.ORGAN_BUD.get()), "a bud at " + bud);
                    Player gardener = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
                    ItemStack meal = new ItemStack(Items.BONE_MEAL, 4);
                    gardener.setItemInHand(InteractionHand.MAIN_HAND, meal);
                    level.getBlockState(half)
                            .useItemOn(
                                    meal,
                                    level,
                                    gardener,
                                    InteractionHand.MAIN_HAND,
                                    new BlockHitResult(
                                            Vec3.atCenterOf(half).add(0, 0.5, 0), Direction.UP, half, false));
                    h.assertTrue(
                            level.getBlockState(half).is(MiningModule.LEACHING_POD.get()),
                            "the half-fed bud needs only its bone meal");
                    gardener.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    level.getBlockState(fed)
                            .useWithoutItem(
                                    level,
                                    gardener,
                                    new BlockHitResult(Vec3.atCenterOf(fed).add(0, 0.5, 0), Direction.UP, fed, false));
                    h.assertTrue(
                            level.getBlockState(fed).is(MiningModule.LEACHING_POD.get()),
                            "the fed bud grows when clicked");
                    BlockPos husk = yard.at(new BlockPos(56, -54, -56));
                    h.assertTrue(level.getBlockState(husk).getValue(LeachingPodBlock.SPENT), "the pit's pod is spent");
                    BlockPos centre = yard.at(new BlockPos(56, -56, -56));
                    h.assertTrue(level.getBlockState(centre).isAir(), "the pit's column is dug");
                    for (BlockPos ore : List.of(
                            new BlockPos(58, -57, -56),
                            new BlockPos(54, -58, -57),
                            new BlockPos(55, -60, -54),
                            new BlockPos(58, -59, -58),
                            new BlockPos(59, -59, -56))) {
                        BlockState state = level.getBlockState(yard.at(ore));
                        h.assertTrue(
                                state.getBlock().getDescriptionId().contains("_ore"), ore + " is still ore: " + state);
                        boolean seen = false;
                        for (Direction side : Direction.values())
                            seen |= level.getBlockState(yard.at(ore).relative(side))
                                    .is(MiningModule.LEACHED_ROCK.get());
                        h.assertTrue(seen, "the ore at " + ore + " lies against Leached Rock");
                    }
                    ServerPlayer miner = TestDrops.miner(h, ItemStack.EMPTY);
                    BlockPos wall = yard.at(new BlockPos(54, -56, -56));
                    h.assertTrue(level.getBlockState(wall).is(MiningModule.LEACHED_ROCK.get()), "the pit's wall");
                    h.assertTrue(miner.gameMode.destroyBlock(wall), "a bare hand breaks the wall");
                    h.assertValueEqual(TestDrops.dropped(h).size(), 0, "and nothing falls");
                    BlockPos live = yard.at(new BlockPos(45, -54, -56));
                    h.assertTrue(
                            level.getBlockState(live).is(MiningModule.LEACHING_POD.get())
                                    && !level.getBlockState(live).getValue(LeachingPodBlock.SPENT),
                            "the live pod waits to work");
                    h.assertValueEqual(
                            level.getBlockState(yard.at(new BlockPos(45, -55, -56)))
                                    .is(EcologyModule.LIVING_SUBSTRATE.get()),
                            true,
                            "on its substrate");
                })
                .thenWaitUntil(() -> h.assertTrue(
                        h.getLevel()
                                .getBlockState(yard.at(new BlockPos(45, -55, -56)))
                                .is(MiningModule.LEACHED_ROCK.get()),
                        "the live pod has leached its first layer"))
                .thenExecute(() -> {
                    int leached = 0;
                    for (int dx = -4; dx <= 4; dx++)
                        for (int dz = -4; dz <= 4; dz++)
                            if (h.getLevel()
                                    .getBlockState(yard.at(new BlockPos(45 + dx, -55, -56 + dz)))
                                    .is(MiningModule.LEACHED_ROCK.get())) leached++;
                    h.assertValueEqual(leached, Leaching.MAX_BLOCKS_PER_LAYER, "a whole seven-wide disc");
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }

    /** The ground a pod may change is a tag of vanilla tags; the hardness and block-entity rules do the rest. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theLeachableTagNamesSoftGroundAndLeavesOutOreAndFluids(GameTestHelper h) {
        for (Block ground : List.of(
                Blocks.DIRT,
                Blocks.GRASS_BLOCK,
                Blocks.SAND,
                Blocks.RED_SAND,
                Blocks.GRAVEL,
                Blocks.SANDSTONE,
                Blocks.RED_SANDSTONE,
                Blocks.STONE,
                Blocks.GRANITE,
                Blocks.DIORITE,
                Blocks.ANDESITE,
                Blocks.TUFF,
                Blocks.DEEPSLATE,
                Blocks.NETHERRACK,
                Blocks.BASALT,
                Blocks.BLACKSTONE,
                EcologyModule.LIVING_SUBSTRATE.get()))
            h.assertTrue(ground.defaultBlockState().is(MiningModule.LEACHABLE), ground + " is leachable");
        for (Block other : List.of(
                Blocks.IRON_ORE,
                Blocks.DEEPSLATE_DIAMOND_ORE,
                Blocks.COAL_ORE,
                Blocks.WATER,
                Blocks.LAVA,
                Blocks.OBSIDIAN,
                Blocks.BEDROCK,
                Blocks.CHEST,
                Blocks.OAK_LOG,
                Blocks.GLASS,
                MiningModule.LEACHED_ROCK.get()))
            h.assertFalse(other.defaultBlockState().is(MiningModule.LEACHABLE), other + " is not leachable");
        h.assertTrue(
                Leaching.softEnough(Blocks.STONE.defaultBlockState().getDestroySpeed(h.getLevel(), BlockPos.ZERO)),
                "stone is soft enough");
        h.assertFalse(
                Leaching.softEnough(Blocks.DEEPSLATE.defaultBlockState().getDestroySpeed(h.getLevel(), BlockPos.ZERO)),
                "deepslate is not");
        h.succeed();
    }
}
