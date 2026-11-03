package org.jd.infestusfrontier.testmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.LivingSubstrateBlock;
import org.jd.infestusfrontier.ecology.SubstrateStage;
import org.jd.infestusfrontier.ecology.TallBlightedUndergrowthBlock;
import org.jd.infestusfrontier.ecology.TallFlowerSource;
import org.jd.infestusfrontier.ecology.TallUndergrowthSource;
import org.jd.infestusfrontier.ecology.TallWitheredFlowerBlock;

/** Tall plants withered by substrate growth: VEGETATION_INFECTION.md, the two-block mappings. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class TallWitheredPlantTests {
    /**
     * Its own batch: these tests force far chunks and spawn drops, and in the default batch they would move the
     * network tests' neighbours around, so a Palm test that needs one of the server's 64 network operations a tick
     * would meet a different crowd than it was written against.
     */
    private static final String BATCH = "tallWitheredPlants";

    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos LOWER = GROUND.above();
    private static final BlockPos UPPER = LOWER.above();
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);

    private static final List<Block> FLOWERS = List.of(Blocks.SUNFLOWER, Blocks.LILAC, Blocks.ROSE_BUSH, Blocks.PEONY);
    private static final List<Block> UNDERGROWTH = List.of(Blocks.TALL_GRASS, Blocks.LARGE_FERN);
    private static final List<Block> PLANTS = concat(FLOWERS, UNDERGROWTH);

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void cultureWithersEveryTallPlantWholeAndWithoutDrops(GameTestHelper h) {
        Player player = EcologyTests.holding(h, GameType.CREATIVE, EcologyModule.SPORE_CULTURE.get(), 1);
        int x = 1;
        for (Block plant : PLANTS) {
            BlockPos ground = new BlockPos(x, 1, 1 + (x & 1));
            h.setBlock(ground, Blocks.GRASS_BLOCK);
            grow(h, ground.above(), plant);
            EcologyTests.use(h, player, ground, Direction.UP);
            EcologyTests.assertStage(h, ground, SubstrateStage.BASIC);
            assertDead(h, ground.above(), plant);
            x++;
        }
        h.runAfterDelay(5, () -> {
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void cultureIsUsedUpOnATallPlant(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.GRASS_BLOCK);
        grow(h, LOWER, Blocks.PEONY);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        EcologyTests.use(h, player, GROUND, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 0, "the culture was used");
        assertDead(h, LOWER, Blocks.PEONY);
        h.runAfterDelay(5, () -> {
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void theWaveWithersTheTallPlantsItReaches(GameTestHelper h) {
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(CENTER.north(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.south(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.east(), Blocks.DIRT);
        grow(h, CENTER.east().above(), Blocks.SUNFLOWER);
        h.setBlock(CENTER.west(), Blocks.GRASS_BLOCK);
        grow(h, CENTER.west().above(), Blocks.LARGE_FERN);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 1);
        EcologyTests.use(h, player, CENTER, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 0, "the bone meal was used");
        h.runAfterDelay(LivingSubstrateBlock.GROWTH_DELAY_TICKS + 2, () -> {
            EcologyTests.assertStage(h, CENTER.east(), SubstrateStage.BASIC);
            EcologyTests.assertStage(h, CENTER.west(), SubstrateStage.BASIC);
            assertDead(h, CENTER.east().above(), Blocks.SUNFLOWER);
            assertDead(h, CENTER.west().above(), Blocks.LARGE_FERN);
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    /** Break order, tool and loot rules: one harvest across both halves, whichever half is hit. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void breakingEitherHalfGivesTheSourceHarvestOnce(GameTestHelper h) {
        for (Block plant : FLOWERS) {
            for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                BlockPos hit = half == DoubleBlockHalf.LOWER ? LOWER : UPPER;
                standDead(h, plant);
                ServerPlayer miner = miner(h, ItemStack.EMPTY, hit);
                h.assertTrue(miner.gameMode.destroyBlock(h.absolutePos(hit)), plant + " " + half + " breaks");
                h.assertBlockPresent(Blocks.AIR, LOWER);
                h.assertBlockPresent(Blocks.AIR, UPPER);
                h.assertValueEqual(
                        dropped(h), Map.of(plant.asItem(), 1), "one " + plant + " for the " + half + " half alone");
                clearDrops(h);
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void shearsOnEitherHalfOfDeadUndergrowthGiveTwoOfTheShortPlantOnce(GameTestHelper h) {
        for (Block plant : UNDERGROWTH) {
            Item shortPlant = plant == Blocks.TALL_GRASS ? Items.SHORT_GRASS : Items.FERN;
            for (BlockPos hit : List.of(LOWER, UPPER)) {
                standDead(h, plant);
                ServerPlayer miner = miner(h, new ItemStack(Items.SHEARS), hit);
                miner.gameMode.destroyBlock(h.absolutePos(hit));
                h.assertBlockPresent(Blocks.AIR, LOWER);
                h.assertBlockPresent(Blocks.AIR, UPPER);
                h.assertValueEqual(dropped(h), Map.of(shortPlant, 2), "shears on " + plant + " at " + hit);
                clearDrops(h);
            }
        }
        h.succeed();
    }

    /** Without shears the vanilla table gives a seed one time in eight, and never the plant or a second roll. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void bareHandsOnDeadUndergrowthGiveAtMostOneSeed(GameTestHelper h) {
        for (Block plant : UNDERGROWTH) {
            for (BlockPos hit : List.of(LOWER, UPPER)) {
                for (int roll = 0; roll < 12; roll++) {
                    standDead(h, plant);
                    miner(h, ItemStack.EMPTY, hit).gameMode.destroyBlock(h.absolutePos(hit));
                    Map<Item, Integer> dropped = dropped(h);
                    h.assertTrue(
                            dropped.isEmpty() || dropped.equals(Map.of(Items.WHEAT_SEEDS, 1)),
                            plant + " at " + hit + " dropped " + dropped);
                    clearDrops(h);
                }
            }
        }
        h.succeed();
    }

    /** A creative player's break goes through the block's own hook and the removal that follows it: no drop at all. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void aCreativeBreakDropsNothingFromEitherHalf(GameTestHelper h) {
        Player creative = EcologyTests.holding(h, GameType.CREATIVE, Items.AIR, 0);
        for (BlockPos hit : List.of(LOWER, UPPER)) {
            standDead(h, Blocks.LILAC);
            BlockPos at = h.absolutePos(hit);
            BlockState state = h.getBlockState(hit);
            state.getBlock().playerWillDestroy(h.getLevel(), at, state, creative);
            h.getLevel().removeBlock(at, false);
            h.assertBlockPresent(Blocks.AIR, LOWER);
            h.assertBlockPresent(Blocks.AIR, UPPER);
            h.assertTrue(dropped(h).isEmpty(), "creative breaking drops nothing: " + dropped(h));
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void removingTheGroundDropsTheSourceHarvestOnce(GameTestHelper h) {
        for (Block plant : FLOWERS) {
            standDead(h, plant);
            h.getLevel().destroyBlock(h.absolutePos(GROUND), true);
            h.assertBlockPresent(Blocks.AIR, LOWER);
            h.assertBlockPresent(Blocks.AIR, UPPER);
            h.assertValueEqual(
                    dropped(h),
                    Map.of(plant.asItem(), 1, EcologyModule.DEAD_TISSUE.get(), 1),
                    plant + " and the cell's Dead Tissue, once");
            clearDrops(h);
        }
        for (Block plant : UNDERGROWTH) {
            standDead(h, plant);
            h.getLevel().destroyBlock(h.absolutePos(GROUND), true);
            h.assertBlockPresent(Blocks.AIR, LOWER);
            h.assertBlockPresent(Blocks.AIR, UPPER);
            Map<Item, Integer> dropped = dropped(h);
            h.assertValueEqual(dropped.get(EcologyModule.DEAD_TISSUE.get()), 1, "the cell's Dead Tissue in " + dropped);
            dropped.remove(EcologyModule.DEAD_TISSUE.get());
            h.assertTrue(
                    dropped.isEmpty() || dropped.equals(Map.of(Items.WHEAT_SEEDS, 1)),
                    plant + " without its ground drops at most one seed, not " + dropped);
            clearDrops(h);
        }
        h.succeed();
    }

    /**
     * Tool and loot conditions are the living plant's: for every tool the dead plant's lower half drops what the
     * living one does and the upper half drops nothing by itself, as in vanilla. Fortune and Silk Touch change nothing
     * where the source ignores them.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void everyToolGetsTheLivingPlantsDrops(GameTestHelper h) {
        var enchantments = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        ItemStack fortune = new ItemStack(Items.DIAMOND_PICKAXE);
        fortune.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), 3);
        BlockPos deadAt = h.absolutePos(LOWER);
        BlockPos liveAt = h.absolutePos(LOWER.east(2));
        h.setBlock(GROUND.east(2), Blocks.GRASS_BLOCK);
        for (Block plant : FLOWERS) {
            standDead(h, plant);
            grow(h, LOWER.east(2), plant);
            for (ItemStack tool : List.of(
                    ItemStack.EMPTY, new ItemStack(Items.SHEARS), silk, fortune, new ItemStack(Items.IRON_AXE))) {
                List<ItemStack> dead = Block.getDrops(h.getBlockState(LOWER), h.getLevel(), deadAt, null, null, tool);
                List<ItemStack> live =
                        Block.getDrops(h.getBlockState(LOWER.east(2)), h.getLevel(), liveAt, null, null, tool);
                h.assertValueEqual(describe(dead), describe(live), plant + " lower half with " + tool);
                h.assertValueEqual(describe(dead), Map.of(plant.asItem(), 1), plant + " lower half with " + tool);
                h.assertTrue(
                        Block.getDrops(h.getBlockState(UPPER), h.getLevel(), deadAt.above(), null, null, tool)
                                .isEmpty(),
                        plant + " upper half is not a harvest by itself");
            }
        }
        for (Block plant : UNDERGROWTH) {
            standDead(h, plant);
            grow(h, LOWER.east(2), plant);
            Item shortPlant = plant == Blocks.TALL_GRASS ? Items.SHORT_GRASS : Items.FERN;
            for (BlockPos half : List.of(LOWER, UPPER)) {
                BlockPos deadHalf = h.absolutePos(half);
                BlockPos liveHalf = h.absolutePos(half.east(2));
                ItemStack shears = new ItemStack(Items.SHEARS);
                h.assertValueEqual(
                        describe(Block.getDrops(h.getBlockState(half), h.getLevel(), deadHalf, null, null, shears)),
                        describe(Block.getDrops(
                                h.getBlockState(half.east(2)), h.getLevel(), liveHalf, null, null, shears)),
                        plant + " " + half + " with shears");
                h.assertValueEqual(
                        describe(Block.getDrops(h.getBlockState(half), h.getLevel(), deadHalf, null, null, shears)),
                        Map.of(shortPlant, 2),
                        plant + " " + half + " with shears");
                for (ItemStack tool : List.of(ItemStack.EMPTY, silk, fortune)) {
                    Map<Item, Integer> drops =
                            describe(Block.getDrops(h.getBlockState(half), h.getLevel(), deadHalf, null, null, tool));
                    h.assertTrue(
                            drops.isEmpty() || drops.equals(Map.of(Items.WHEAT_SEEDS, 1)),
                            plant + " " + half + " with " + tool + " drops " + drops);
                }
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void aTallPlantWithoutItsUpperHalfKeepsItsGround(GameTestHelper h) {
        Player culture = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), PLANTS.size());
        int x = 1;
        for (Block plant : PLANTS) {
            BlockPos ground = new BlockPos(x, 1, 1 + (x & 1));
            h.setBlock(ground, Blocks.GRASS_BLOCK);
            // set without shape updates, so the lower half stands alone as a save, /setblock or another mod can leave
            // it
            h.getLevel()
                    .setBlock(
                            h.absolutePos(ground.above()),
                            plant.defaultBlockState(),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            EcologyTests.use(h, culture, ground, Direction.UP);
            x++;
        }
        h.assertValueEqual(culture.getMainHandItem().getCount(), PLANTS.size(), "culture is refused, none used up");
        x = 1;
        for (Block plant : PLANTS) {
            BlockPos ground = new BlockPos(x, 1, 1 + (x & 1));
            h.assertBlockPresent(Blocks.GRASS_BLOCK, ground);
            h.assertBlockPresent(plant, ground.above());
            h.assertBlockPresent(Blocks.AIR, ground.above(2));
            x++;
        }
        h.runAfterDelay(5, () -> {
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void aTallPlantWithAMismatchedUpperHalfKeepsItsGround(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.GRASS_BLOCK);
        silently(h, LOWER, Blocks.LILAC.defaultBlockState());
        silently(h, UPPER, Blocks.SUNFLOWER.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        h.setBlock(GROUND.east(2), Blocks.GRASS_BLOCK);
        silently(h, LOWER.east(2), Blocks.TALL_GRASS.defaultBlockState());
        silently(h, UPPER.east(2), Blocks.TALL_GRASS.defaultBlockState()); // a lower half where an upper belongs
        h.setBlock(GROUND.east(4), Blocks.GRASS_BLOCK);
        silently(h, LOWER.east(4), Blocks.ROSE_BUSH.defaultBlockState());
        silently(
                h,
                UPPER.east(4),
                Blocks.PEONY.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
        Player culture = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 3);
        for (int dx : new int[] {0, 2, 4}) EcologyTests.use(h, culture, GROUND.east(dx), Direction.UP);
        h.assertValueEqual(culture.getMainHandItem().getCount(), 3, "culture is refused under a mismatched plant");
        for (int dx : new int[] {0, 2, 4}) h.assertBlockPresent(Blocks.GRASS_BLOCK, GROUND.east(dx));
        h.assertBlockPresent(Blocks.LILAC, LOWER);
        h.assertBlockPresent(Blocks.SUNFLOWER, UPPER);
        h.assertBlockPresent(Blocks.TALL_GRASS, LOWER.east(2));
        h.assertBlockPresent(Blocks.TALL_GRASS, UPPER.east(2));
        h.assertBlockPresent(Blocks.ROSE_BUSH, LOWER.east(4));
        h.assertBlockPresent(Blocks.PEONY, UPPER.east(4));
        h.succeed();
    }

    /** The top of the world has no block over a lower half to read, so the plant cannot be whole. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void aTallPlantAtTheTopOfTheWorldKeepsItsGround(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos base = h.absolutePos(GROUND);
        BlockPos ground = new BlockPos(base.getX(), level.getMaxBuildHeight() - 2, base.getZ());
        BlockPos lower = ground.above();
        level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(lower, Blocks.SUNFLOWER.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        try {
            Player player = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
            ItemStack culture = player.getMainHandItem();
            InteractionResult result = useCulture(level, player, ground);
            h.assertValueEqual(result, InteractionResult.PASS, "culture is refused where the upper half cannot be");
            h.assertValueEqual(culture.getCount(), 1, "and not used up");
            h.assertTrue(level.getBlockState(ground).is(Blocks.GRASS_BLOCK), "the ground is unchanged");
            h.assertTrue(level.getBlockState(lower).is(Blocks.SUNFLOWER), "and so is the plant");
        } finally {
            level.setBlock(lower, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlock(ground, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        h.succeed();
    }

    /** A plant that was whole when a wave was started but is not when the wave reaches it is left unconverted. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void aWaveSkipsAPlantThatLostItsUpperHalfMeanwhile(GameTestHelper h) {
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(CENTER.north(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.south(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.east(), Blocks.DIRT);
        grow(h, CENTER.east().above(), Blocks.PEONY);
        h.setBlock(CENTER.west(), Blocks.GRASS_BLOCK);
        grow(h, CENTER.west().above(), Blocks.TALL_GRASS);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 1);
        EcologyTests.use(h, player, CENTER, Direction.UP);
        h.runAfterDelay(10, () -> {
            // the east plant is cut down to a lone lower half; the west one is whole
            silently(h, CENTER.east().above(2), Blocks.AIR.defaultBlockState());
        });
        h.runAfterDelay(LivingSubstrateBlock.GROWTH_DELAY_TICKS + 2, () -> {
            h.assertBlockPresent(Blocks.DIRT, CENTER.east());
            h.assertBlockPresent(Blocks.PEONY, CENTER.east().above());
            h.assertBlockPresent(Blocks.AIR, CENTER.east().above(2));
            EcologyTests.assertStage(h, CENTER.west(), SubstrateStage.BASIC);
            assertDead(h, CENTER.west().above(), Blocks.TALL_GRASS);
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void groundUnderAnotherTallPlantStaysUnconverted(GameTestHelper h) {
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(CENTER.north(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.south(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.east(), Blocks.DIRT);
        grow(h, CENTER.east().above(), Blocks.PITCHER_PLANT); // a tall flower outside the mapping
        h.setBlock(CENTER.west(), Blocks.GRASS_BLOCK);
        h.setBlock(CENTER.west().above(), Blocks.SHORT_GRASS); // a later mapping, not implemented yet
        Player boneMeal = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 1);
        EcologyTests.use(h, boneMeal, CENTER, Direction.UP);
        h.assertValueEqual(boneMeal.getMainHandItem().getCount(), 1, "no host to grow onto, so the bone meal is kept");
        Player culture = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 2);
        EcologyTests.use(h, culture, CENTER.east(), Direction.UP);
        EcologyTests.use(h, culture, CENTER.west(), Direction.UP);
        h.assertValueEqual(culture.getMainHandItem().getCount(), 2, "culture is refused under them");
        h.runAfterDelay(LivingSubstrateBlock.GROWTH_DELAY_TICKS + 2, () -> {
            h.assertBlockPresent(Blocks.DIRT, CENTER.east());
            h.assertBlockPresent(Blocks.PITCHER_PLANT, CENTER.east().above());
            h.assertBlockPresent(Blocks.PITCHER_PLANT, CENTER.east().above(2));
            h.assertBlockPresent(Blocks.GRASS_BLOCK, CENTER.west());
            h.assertBlockPresent(Blocks.SHORT_GRASS, CENTER.west().above());
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void repeatedTreatmentOfADeadTallPlantYieldsNothing(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        for (Direction side : Direction.Plane.HORIZONTAL) h.setBlock(GROUND.relative(side), Blocks.OAK_PLANKS);
        standDeadOn(h, Blocks.SUNFLOWER);
        Player boneMeal = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 3);
        EcologyTests.use(h, boneMeal, LOWER, Direction.UP);
        EcologyTests.use(h, boneMeal, UPPER, Direction.UP);
        EcologyTests.use(h, boneMeal, GROUND, Direction.UP);
        h.assertValueEqual(boneMeal.getMainHandItem().getCount(), 3, "bone meal is kept");
        Player culture = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 3);
        EcologyTests.use(h, culture, LOWER, Direction.UP);
        EcologyTests.use(h, culture, UPPER, Direction.UP);
        EcologyTests.use(h, culture, GROUND, Direction.UP);
        h.assertValueEqual(culture.getMainHandItem().getCount(), 3, "culture is kept");
        assertDead(h, LOWER, Blocks.SUNFLOWER);
        EcologyTests.assertStage(h, GROUND, SubstrateStage.MATURE);
        EcologyTests.assertVigor(h, GROUND, 0);
        h.runAfterDelay(5, () -> {
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void theDeadFormsAreNotBonemealable(GameTestHelper h) {
        for (Block block :
                List.of(EcologyModule.TALL_WITHERED_FLOWER.get(), EcologyModule.TALL_BLIGHTED_UNDERGROWTH.get())) {
            h.assertFalse(
                    block instanceof net.minecraft.world.level.block.BonemealableBlock,
                    block + " answers no bone meal");
            h.assertFalse(block.defaultBlockState().isRandomlyTicking(), block + " takes no random ticks");
            h.assertFalse(block.defaultBlockState().hasBlockEntity(), block + " has no block entity");
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 150)
    public static void dormantSubstrateLeavesALaterTallPlantAlone(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        h.setBlock(GROUND.east(), Blocks.GRASS_BLOCK);
        grow(h, LOWER.east(), Blocks.TALL_GRASS);
        h.runAfterDelay(100, () -> {
            h.assertBlockPresent(Blocks.GRASS_BLOCK, GROUND.east());
            h.assertBlockPresent(Blocks.TALL_GRASS, LOWER.east());
            h.assertBlockPresent(Blocks.TALL_GRASS, UPPER.east());
            EcologyTests.assertVigor(h, GROUND, 0);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void pickBlockGivesThePlantItWas(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        for (Block plant : PLANTS) {
            standDeadOn(h, plant);
            for (BlockPos half : List.of(LOWER, UPPER)) {
                BlockState state = h.getBlockState(half);
                ItemStack picked = state.getBlock().getCloneItemStack(h.getLevel(), h.absolutePos(half), state);
                h.assertTrue(picked.is(plant.asItem()), "pick block gives " + plant + ", not " + picked);
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void aDeadTallPlantBurnsAsReadilyAsTheLivingOne(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        for (Block plant : PLANTS) {
            standDeadOn(h, plant);
            BlockPos at = h.absolutePos(LOWER);
            BlockState dead = h.getBlockState(LOWER);
            h.assertValueEqual(
                    dead.getFlammability(h.getLevel(), at, Direction.UP),
                    plant.defaultBlockState().getFlammability(h.getLevel(), at, Direction.UP),
                    plant + " is as flammable as the plant it was");
            h.assertValueEqual(
                    dead.getFireSpreadSpeed(h.getLevel(), at, Direction.UP),
                    plant.defaultBlockState().getFireSpreadSpeed(h.getLevel(), at, Direction.UP),
                    plant + " catches fire as readily");
        }
        h.succeed();
    }

    /** A chunk written and read back, as a save does, holds both halves of every plant at the same positions. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void reloadKeepsBothHalves(GameTestHelper h) {
        int x = 1;
        for (Block plant : PLANTS) {
            BlockPos ground = new BlockPos(x, 1, 3);
            h.setBlock(ground, EcologyTests.mature());
            standDeadOn(h, plant, ground.above());
            x++;
        }
        ServerLevel level = h.getLevel();
        // the row may cross a chunk border, so each plant is read from the chunk it stands in
        Map<ChunkPos, ProtoChunk> reads = new HashMap<>();
        x = 1;
        for (Block plant : PLANTS) {
            for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                BlockPos at = h.absolutePos(new BlockPos(x, half == DoubleBlockHalf.LOWER ? 2 : 3, 3));
                LevelChunk chunk = level.getChunkAt(at);
                ProtoChunk read = reads.computeIfAbsent(
                        chunk.getPos(),
                        pos -> ChunkSerializer.read(
                                level,
                                level.getPoiManager(),
                                new RegionStorageInfo("tall_withered_plants", level.dimension(), "chunk"),
                                pos,
                                ChunkSerializer.write(level, chunk)));
                h.assertValueEqual(read.getBlockState(at), dead(plant, half), plant + " " + half + " read back");
                h.assertValueEqual(
                        read.getBlockState(at).getSeed(at),
                        level.getBlockState(at).getSeed(at),
                        plant + " " + half + " looks the same");
            }
            x++;
        }
        h.succeed();
    }

    /** Appearance comes from the position alone, the lower half's: not saved, the same for both halves. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void bothHalvesLookAlikeAndNothingOfTheLookIsSaved(GameTestHelper h) {
        BlockPos at = h.absolutePos(LOWER);
        for (Block plant : PLANTS) {
            BlockState lower = dead(plant, DoubleBlockHalf.LOWER);
            BlockState upper = dead(plant, DoubleBlockHalf.UPPER);
            CompoundTag saved = NbtUtils.writeBlockState(upper);
            h.assertValueEqual(
                    saved.getCompound("Properties").getAllKeys(),
                    Set.of("source", "half"),
                    "the source and the half are all it saves");
            h.assertValueEqual(
                    NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK), saved),
                    upper,
                    "and read back as the same state");
            h.assertValueEqual(lower.getSeed(at), upper.getSeed(at.above()), "the halves' look seeds agree");
            h.assertFalse(lower.hasBlockEntity(), "no block entity");
        }
        BlockState flowerLower = dead(Blocks.SUNFLOWER, DoubleBlockHalf.LOWER);
        BlockState flowerUpper = dead(Blocks.SUNFLOWER, DoubleBlockHalf.UPPER);
        BlockState grassLower = dead(Blocks.TALL_GRASS, DoubleBlockHalf.LOWER);
        BlockState grassUpper = dead(Blocks.TALL_GRASS, DoubleBlockHalf.UPPER);
        Set<Integer> flowerTints = new HashSet<>();
        Set<Integer> grassTints = new HashSet<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockPos pos = new BlockPos(x, 40, z);
                h.assertValueEqual(
                        TallWitheredFlowerBlock.tintAt(flowerLower, pos),
                        TallWitheredFlowerBlock.tintAt(flowerUpper, pos.above()),
                        "a flower is one tint over both halves at " + pos);
                h.assertValueEqual(
                        TallBlightedUndergrowthBlock.tintAt(grassLower, pos),
                        TallBlightedUndergrowthBlock.tintAt(grassUpper, pos.above()),
                        "undergrowth is one tint over both halves at " + pos);
                flowerTints.add(TallWitheredFlowerBlock.tintAt(flowerLower, pos));
                grassTints.add(TallBlightedUndergrowthBlock.tintAt(grassLower, pos));
            }
        }
        h.assertValueEqual(flowerTints.size(), 4, "all four flower tints appear across a patch");
        h.assertValueEqual(grassTints.size(), 4, "all four undergrowth tints appear across a patch");
        h.succeed();
    }

    /** The blockstate lists the same turns for a plant's lower and upper half, so vanilla's shared seed picks one look. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void blockstatesTurnBothHalvesAlike(GameTestHelper h) throws Exception {
        for (String block : List.of("tall_withered_flower", "tall_blighted_undergrowth")) {
            JsonObject variants;
            try (InputStream in = TallWitheredFlowerBlock.class.getResourceAsStream(
                    "/assets/infestusfrontier/blockstates/" + block + ".json")) {
                variants = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .getAsJsonObject()
                        .getAsJsonObject("variants");
            }
            int checked = 0;
            for (String key : variants.keySet()) {
                if (!key.startsWith("half=lower,")) continue;
                JsonArray lower = variants.getAsJsonArray(key);
                JsonArray upper = variants.getAsJsonArray(key.replace("half=lower", "half=upper"));
                h.assertValueEqual(turns(lower), turns(upper), block + " " + key + " turns both halves alike");
                h.assertTrue(lower.size() > 1, block + " " + key + " has more than one look");
                checked++;
            }
            h.assertTrue(checked > 0, block + " lists its states");
        }
        h.succeed();
    }

    private static List<Integer> turns(JsonArray looks) {
        List<Integer> turns = new ArrayList<>();
        for (var look : looks) {
            var object = look.getAsJsonObject();
            turns.add(object.has("y") ? object.get("y").getAsInt() : 0);
        }
        return turns;
    }

    // ---- the exhibit as the demo builds it ----

    private static final Pattern SET = Pattern.compile("^setblock (-?\\d+) (-?\\d+) (-?\\d+) (\\S.*)$");
    private static final Pattern FILL =
            Pattern.compile("^fill (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (-?\\d+) (\\S.*)$");
    // the tall-plant exhibit of captures/demo.json: a living plot, its dead twin, a supply chest and two signs
    private static final int MIN_X = 30;
    private static final int MAX_X = 47;
    private static final int MIN_Y = -62;
    private static final int MAX_Y = -57;
    private static final int MIN_Z = -27;
    private static final int MAX_Z = -23;
    private static final int SHIFT_X = 246000;
    private static final int SHIFT_Y = 160;
    private static final int SHIFT_Z = 246000;
    private static final int SLOT_Z = 64;
    /** The demo's plot: each plant's lower half at x, z = -25, over the grass at y = -61. */
    private static final int LIVING_FIRST_X = 32;

    private static final int DEAD_FIRST_X = 41;
    private static final int PLOT_Z = -25;
    private static final BlockPos CHEST = new BlockPos(32, -60, -27);
    private static final BlockPos PITCHER = new BlockPos(38, -60, -25);

    private static List<String> exhibitCommands() {
        List<String> found = new ArrayList<>();
        try (InputStream in = TallWitheredPlantTests.class.getResourceAsStream("/captures/demo.json")) {
            if (in == null) throw new IllegalStateException("demo.json is not on the test classpath");
            for (JsonElement command : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject()
                    .getAsJsonArray("commands")) {
                String text = command.getAsString();
                Matcher set = SET.matcher(text);
                Matcher fill = FILL.matcher(text);
                if (set.matches() && inBox(set, 1)) found.add(text);
                else if (fill.matches() && inBox(fill, 1) && inBox(fill, 4)) found.add(text);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return found;
    }

    private static boolean inBox(Matcher matcher, int first) {
        int x = Integer.parseInt(matcher.group(first));
        int y = Integer.parseInt(matcher.group(first + 1));
        int z = Integer.parseInt(matcher.group(first + 2));
        return x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y && z >= MIN_Z && z <= MAX_Z;
    }

    private static String shifted(String command, int shiftZ) {
        Matcher set = SET.matcher(command);
        if (set.matches())
            return "setblock %d %d %d %s"
                    .formatted(
                            Integer.parseInt(set.group(1)) + SHIFT_X,
                            Integer.parseInt(set.group(2)) + SHIFT_Y,
                            Integer.parseInt(set.group(3)) + shiftZ,
                            set.group(4));
        Matcher fill = FILL.matcher(command);
        if (!fill.matches()) throw new IllegalStateException(command);
        return "fill %d %d %d %d %d %d %s"
                .formatted(
                        Integer.parseInt(fill.group(1)) + SHIFT_X,
                        Integer.parseInt(fill.group(2)) + SHIFT_Y,
                        Integer.parseInt(fill.group(3)) + shiftZ,
                        Integer.parseInt(fill.group(4)) + SHIFT_X,
                        Integer.parseInt(fill.group(5)) + SHIFT_Y,
                        Integer.parseInt(fill.group(6)) + shiftZ,
                        fill.group(7));
    }

    private static BlockPos inYard(BlockPos demo, int shiftZ) {
        return new BlockPos(demo.getX() + SHIFT_X, demo.getY() + SHIFT_Y, demo.getZ() + shiftZ);
    }

    /** Loads the exhibit's chunks and their entities. The saved test world may hold an earlier run's items there. */
    private static List<ChunkPos> forceYard(GameTestHelper h, int shiftZ) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = (MIN_X + SHIFT_X) >> 4; x <= (MAX_X + SHIFT_X) >> 4; x++)
            for (int z = (MIN_Z + shiftZ) >> 4; z <= (MAX_Z + shiftZ) >> 4; z++) chunks.add(new ChunkPos(x, z));
        for (ChunkPos chunk : chunks) TestChunks.force(h.getLevel(), chunk);
        return chunks;
    }

    /** Ticks the entities of a forced chunk take to load from a saved world. */
    private static final int ENTITIES_LOAD_TICKS = 10;

    /** Empties the exhibit's blocks, block entities and the items around them, dropping nothing. */
    private static void tidy(GameTestHelper h, int shiftZ) {
        ServerLevel level = h.getLevel();
        for (BlockPos at : BlockPos.betweenClosed(
                MIN_X + SHIFT_X - 1,
                MIN_Y + SHIFT_Y - 1,
                MIN_Z + shiftZ - 1,
                MAX_X + SHIFT_X + 1,
                MAX_Y + SHIFT_Y + 3,
                MAX_Z + shiftZ + 1)) {
            level.removeBlockEntity(at); // a saved world still holds an earlier run's chest
            level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
        }
        AABB yard = new AABB(
                        MIN_X + SHIFT_X,
                        MIN_Y + SHIFT_Y,
                        MIN_Z + shiftZ,
                        MAX_X + SHIFT_X + 1,
                        MAX_Y + SHIFT_Y + 3,
                        MAX_Z + shiftZ + 1)
                .inflate(16);
        level.getEntitiesOfClass(ItemEntity.class, yard).forEach(ItemEntity::discard);
    }

    /** The demo exhibit, built from its own commands over an emptied yard. */
    private static void buildExhibit(GameTestHelper h, int shiftZ) {
        tidy(h, shiftZ);
        ServerLevel level = h.getLevel();
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level);
        for (String command : exhibitCommands()) {
            try {
                level.getServer().getCommands().getDispatcher().execute(shifted(command, shiftZ), source);
            } catch (CommandSyntaxException e) {
                h.fail("the demo's command failed: " + command + ": " + e.getMessage());
            }
        }
    }

    private static void forget(GameTestHelper h, List<ChunkPos> chunks) {
        for (ChunkPos chunk : chunks) h.getLevel().setChunkForced(chunk.x, chunk.z, false);
    }

    /** The player needs nothing but what the exhibit's chest holds: culture, bone meal and shears. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void theDemoExhibitSuppliesAndWithersTheTallPlants(GameTestHelper h) {
        int shiftZ = SHIFT_Z;
        List<ChunkPos> chunks = forceYard(h, shiftZ);
        ServerLevel level = h.getLevel();
        h.startSequence()
                .thenIdle(ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    buildExhibit(h, shiftZ);
                    h.assertTrue(exhibitCommands().size() > 30, "the demo holds the exhibit");
                    ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(inYard(CHEST, shiftZ));
                    h.assertTrue(chest != null, "the supply chest stands");
                    h.assertTrue(chest.countItem(Items.BONE_MEAL) >= 4, "bone meal in the chest");
                    h.assertTrue(
                            chest.countItem(EcologyModule.SPORE_CULTURE.get()) >= PLANTS.size(),
                            "culture for every plant");
                    h.assertTrue(chest.countItem(Items.SHEARS) >= 1, "shears for the undergrowth");
                    Player gardener = EcologyTests.holding(
                            h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), PLANTS.size());
                    for (int i = 0; i < PLANTS.size(); i++) {
                        BlockPos lower = inYard(new BlockPos(LIVING_FIRST_X + i, -60, PLOT_Z), shiftZ);
                        h.assertValueEqual(
                                level.getBlockState(lower),
                                PLANTS.get(i).defaultBlockState(),
                                "the living " + PLANTS.get(i));
                        useCulture(level, gardener, lower.below());
                        h.assertValueEqual(
                                level.getBlockState(lower),
                                dead(PLANTS.get(i), DoubleBlockHalf.LOWER),
                                "the dead lower half");
                        h.assertValueEqual(
                                level.getBlockState(lower.above()),
                                dead(PLANTS.get(i), DoubleBlockHalf.UPPER),
                                "and upper half");
                        h.assertTrue(
                                level.getBlockState(lower.below()).is(EcologyModule.LIVING_SUBSTRATE.get()),
                                "over a cell");
                    }
                    h.assertValueEqual(gardener.getMainHandItem().getCount(), 0, "one culture a plant");
                    BlockPos pitcher = inYard(PITCHER, shiftZ);
                    Player spare = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
                    h.assertValueEqual(
                            useCulture(level, spare, pitcher.below()),
                            InteractionResult.PASS,
                            "the pitcher plant stays");
                    h.assertTrue(level.getBlockState(pitcher.below()).is(Blocks.GRASS_BLOCK), "on its grass");
                    for (int i = 0; i < PLANTS.size(); i++) {
                        BlockPos lower = inYard(new BlockPos(DEAD_FIRST_X + i, -60, PLOT_Z), shiftZ);
                        h.assertValueEqual(
                                level.getBlockState(lower),
                                dead(PLANTS.get(i), DoubleBlockHalf.LOWER),
                                "the exhibit's dead plant");
                        h.assertValueEqual(
                                level.getBlockState(lower.above()),
                                dead(PLANTS.get(i), DoubleBlockHalf.UPPER),
                                "whole");
                    }
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    for (int i = 0; i < PLANTS.size(); i++) {
                        BlockPos lower = inYard(new BlockPos(LIVING_FIRST_X + i, -60, PLOT_Z), shiftZ);
                        h.assertValueEqual(
                                level.getBlockState(lower), dead(PLANTS.get(i), DoubleBlockHalf.LOWER), "it stays");
                    }
                    List<ItemEntity> drops =
                            level.getEntitiesOfClass(ItemEntity.class, new AABB(inYard(CHEST, shiftZ)).inflate(16));
                    h.assertTrue(drops.isEmpty(), "infection dropped nothing, not " + drops);
                    tidy(h, shiftZ);
                    forget(h, chunks);
                })
                .thenSucceed();
    }

    /** Harvest from the exhibit's dead plot with shears: two short grass from one half hit, a sunflower from another. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void theDemoExhibitsDeadPlantsHarvestOnce(GameTestHelper h) {
        int shiftZ = SHIFT_Z + SLOT_Z;
        List<ChunkPos> chunks = forceYard(h, shiftZ);
        ServerLevel level = h.getLevel();
        BlockPos grass = inYard(new BlockPos(DEAD_FIRST_X + 4, -60, PLOT_Z), shiftZ);
        BlockPos sunflower = inYard(new BlockPos(DEAD_FIRST_X, -60, PLOT_Z), shiftZ);
        h.startSequence()
                .thenIdle(ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    buildExhibit(h, shiftZ);
                    ServerPlayer miner = miner(h, new ItemStack(Items.SHEARS), grass);
                    miner.gameMode.destroyBlock(grass.above());
                    miner.gameMode.destroyBlock(sunflower);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    h.assertTrue(
                            level.getBlockState(grass).isAir()
                                    && level.getBlockState(grass.above()).isAir(),
                            "the grass is gone");
                    Map<Item, Integer> sums = new TreeMap<>(
                            (a, b) -> BuiltInRegistries.ITEM.getKey(a).compareTo(BuiltInRegistries.ITEM.getKey(b)));
                    for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(grass).inflate(16)))
                        sums.merge(drop.getItem().getItem(), drop.getItem().getCount(), Integer::sum);
                    h.assertValueEqual(
                            sums,
                            Map.of(Items.SHORT_GRASS, 2, Items.SUNFLOWER, 1),
                            "two short grass and one sunflower");
                    tidy(h, shiftZ);
                    forget(h, chunks);
                })
                .thenSucceed();
    }

    private static InteractionResult useCulture(ServerLevel level, Player player, BlockPos ground) {
        ItemStack culture = player.getMainHandItem();
        return culture.useOn(new UseOnContext(
                level,
                player,
                InteractionHand.MAIN_HAND,
                culture,
                new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false)));
    }

    // --- fixtures ---

    private static List<Block> concat(List<Block> a, List<Block> b) {
        List<Block> all = new ArrayList<>(a);
        all.addAll(b);
        return List.copyOf(all);
    }

    /** The dead plant that {@code plant} becomes, in {@code half}. */
    static BlockState dead(Block plant, DoubleBlockHalf half) {
        BlockState state;
        var flower = TallFlowerSource.of(plant);
        if (flower.isPresent())
            state = EcologyModule.TALL_WITHERED_FLOWER
                    .get()
                    .defaultBlockState()
                    .setValue(TallWitheredFlowerBlock.SOURCE, flower.get());
        else
            state = EcologyModule.TALL_BLIGHTED_UNDERGROWTH
                    .get()
                    .defaultBlockState()
                    .setValue(
                            TallBlightedUndergrowthBlock.SOURCE,
                            TallUndergrowthSource.of(plant).orElseThrow());
        return state.setValue(DoublePlantBlock.HALF, half);
    }

    /** A living, whole plant with its lower half at {@code lower}. */
    private static void grow(GameTestHelper h, BlockPos lower, Block plant) {
        clear(h, lower);
        DoublePlantBlock.placeAt(h.getLevel(), plant.defaultBlockState(), h.absolutePos(lower), Block.UPDATE_ALL);
    }

    /** A dead, whole plant on mature substrate at {@link #GROUND}, nothing else changed. */
    private static void standDead(GameTestHelper h, Block plant) {
        h.setBlock(GROUND, EcologyTests.mature());
        standDeadOn(h, plant);
    }

    private static void standDeadOn(GameTestHelper h, Block plant) {
        standDeadOn(h, plant, LOWER);
    }

    private static void standDeadOn(GameTestHelper h, Block plant, BlockPos lower) {
        clear(h, lower);
        DoublePlantBlock.placeAt(
                h.getLevel(), dead(plant, DoubleBlockHalf.LOWER), h.absolutePos(lower), Block.UPDATE_ALL);
    }

    /** Empties both blocks of a plant's place, so a different plant that stood there cannot drag the new one down. */
    private static void clear(GameTestHelper h, BlockPos lower) {
        silently(h, lower, Blocks.AIR.defaultBlockState());
        silently(h, lower.above(), Blocks.AIR.defaultBlockState());
    }

    private static void silently(GameTestHelper h, BlockPos at, BlockState state) {
        h.getLevel().setBlock(h.absolutePos(at), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    static void assertDead(GameTestHelper h, BlockPos lower, Block plant) {
        h.assertValueEqual(h.getBlockState(lower), dead(plant, DoubleBlockHalf.LOWER), "lower half of " + plant);
        h.assertValueEqual(
                h.getBlockState(lower.above()), dead(plant, DoubleBlockHalf.UPPER), "upper half of " + plant);
    }

    private static ServerPlayer miner(GameTestHelper h, ItemStack tool, BlockPos target) {
        ServerPlayer player = new ServerPlayer(
                h.getLevel().getServer(),
                h.getLevel(),
                new GameProfile(UUID.randomUUID(), "gardener"),
                ClientInformation.createDefault());
        player.setGameMode(GameType.SURVIVAL);
        Vec3 feet = h.absolutePos(new BlockPos(3, 1, 1)).getCenter().add(0, -0.5, 0);
        player.moveTo(feet.x, feet.y, feet.z, 0F, 0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    /** What lies on the ground, item by item. */
    private static Map<Item, Integer> dropped(GameTestHelper h) {
        Map<Item, Integer> sums =
                new TreeMap<>((a, b) -> BuiltInRegistries.ITEM.getKey(a).compareTo(BuiltInRegistries.ITEM.getKey(b)));
        for (ItemEntity drop :
                h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(1)))
            sums.merge(drop.getItem().getItem(), drop.getItem().getCount(), Integer::sum);
        return sums;
    }

    private static Map<Item, Integer> describe(List<ItemStack> drops) {
        Map<Item, Integer> sums =
                new TreeMap<>((a, b) -> BuiltInRegistries.ITEM.getKey(a).compareTo(BuiltInRegistries.ITEM.getKey(b)));
        for (ItemStack drop : drops) sums.merge(drop.getItem(), drop.getCount(), Integer::sum);
        return sums;
    }

    private static void clearDrops(GameTestHelper h) {
        h.getLevel()
                .getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(1))
                .forEach(ItemEntity::discard);
    }
}
