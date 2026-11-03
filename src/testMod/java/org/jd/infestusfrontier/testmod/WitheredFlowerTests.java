package org.jd.infestusfrontier.testmod;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.FlowerSource;
import org.jd.infestusfrontier.ecology.LivingSubstrateBlock;
import org.jd.infestusfrontier.ecology.SubstrateStage;
import org.jd.infestusfrontier.ecology.WitheredFlowerBlock;

/** Flowers withered by substrate growth: VEGETATION_INFECTION.md, the Withered Flower mapping. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class WitheredFlowerTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos FLOWER = GROUND.above();
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void cultureWithersTheFlowerOnTheGround(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.GRASS_BLOCK);
        h.setBlock(FLOWER, Blocks.POPPY);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        EcologyTests.use(h, player, GROUND, Direction.UP);
        EcologyTests.assertStage(h, GROUND, SubstrateStage.BASIC);
        assertWithered(h, FLOWER, FlowerSource.POPPY);
        h.assertValueEqual(player.getMainHandItem().getCount(), 0, "the culture was used");
        h.runAfterDelay(5, () -> {
            assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void everySupportedFlowerWithersAndKeepsItsName(GameTestHelper h) {
        Player player = EcologyTests.holding(h, GameType.CREATIVE, EcologyModule.SPORE_CULTURE.get(), 1);
        int x = 1;
        for (FlowerSource source : FlowerSource.values()) {
            BlockPos ground = new BlockPos(x, 1, 1 + (x & 1));
            h.setBlock(ground, Blocks.GRASS_BLOCK);
            h.setBlock(ground.above(), source.flower());
            EcologyTests.use(h, player, ground, Direction.UP);
            EcologyTests.assertStage(h, ground, SubstrateStage.BASIC);
            assertWithered(h, ground.above(), source);
            x++;
        }
        h.runAfterDelay(5, () -> {
            assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void theWaveWithersTheFlowersItReaches(GameTestHelper h) {
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(CENTER.north(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.south(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.east(), Blocks.DIRT);
        h.setBlock(CENTER.east().above(), Blocks.ALLIUM);
        h.setBlock(CENTER.west(), Blocks.GRASS_BLOCK);
        h.setBlock(CENTER.west().above(), Blocks.CORNFLOWER);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 1);
        EcologyTests.use(h, player, CENTER, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 0, "the bone meal was used");
        h.runAfterDelay(LivingSubstrateBlock.GROWTH_DELAY_TICKS + 2, () -> {
            EcologyTests.assertStage(h, CENTER.east(), SubstrateStage.BASIC);
            EcologyTests.assertStage(h, CENTER.west(), SubstrateStage.BASIC);
            assertWithered(h, CENTER.east().above(), FlowerSource.ALLIUM);
            assertWithered(h, CENTER.west().above(), FlowerSource.CORNFLOWER);
            assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void groundUnderAnUnsupportedPlantStaysUnconverted(GameTestHelper h) {
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(CENTER.south(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.east(), Blocks.DIRT);
        h.setBlock(CENTER.east().above(), Blocks.OAK_SAPLING);
        h.setBlock(CENTER.west(), Blocks.GRASS_BLOCK);
        h.setBlock(CENTER.west().above(), Blocks.SHORT_GRASS); // a later mapping, not implemented yet
        h.setBlock(CENTER.north(), Blocks.DIRT);
        h.setBlock(CENTER.north().above(), Blocks.TORCHFLOWER); // a flower outside the mapping
        Player boneMeal = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 1);
        EcologyTests.use(h, boneMeal, CENTER, Direction.UP);
        h.assertValueEqual(boneMeal.getMainHandItem().getCount(), 1, "no host to grow onto, so the bone meal is kept");
        Player culture = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 3);
        for (BlockPos ground : List.of(CENTER.east(), CENTER.west(), CENTER.north()))
            EcologyTests.use(h, culture, ground, Direction.UP);
        h.assertValueEqual(culture.getMainHandItem().getCount(), 3, "culture is refused under a rooted plant");
        h.runAfterDelay(LivingSubstrateBlock.GROWTH_DELAY_TICKS + 2, () -> {
            h.assertBlockPresent(Blocks.DIRT, CENTER.east());
            h.assertBlockPresent(Blocks.OAK_SAPLING, CENTER.east().above());
            h.assertBlockPresent(Blocks.GRASS_BLOCK, CENTER.west());
            h.assertBlockPresent(Blocks.SHORT_GRASS, CENTER.west().above());
            h.assertBlockPresent(Blocks.DIRT, CENTER.north());
            h.assertBlockPresent(Blocks.TORCHFLOWER, CENTER.north().above());
            assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void breakingGivesTheSourceFlowerOnce(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        h.setBlock(FLOWER, withered(FlowerSource.ALLIUM));
        h.getLevel().destroyBlock(h.absolutePos(FLOWER), true);
        h.runAfterDelay(2, () -> {
            h.assertBlockPresent(Blocks.AIR, FLOWER);
            assertDrops(h, List.of(Items.ALLIUM));
            h.succeed();
        });
    }

    /** The source's own loot table answers the real tool context: Silk Touch, Fortune and tools alike. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void anyToolGivesTheSourceFlowerOnce(GameTestHelper h) {
        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(
                h.getLevel()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SILK_TOUCH),
                1);
        BlockPos at = h.absolutePos(FLOWER);
        for (FlowerSource source : FlowerSource.values()) {
            for (ItemStack tool : List.of(ItemStack.EMPTY, new ItemStack(Items.SHEARS), silk)) {
                List<ItemStack> drops = Block.getDrops(withered(source), h.getLevel(), at, null, null, tool);
                h.assertTrue(
                        drops.size() == 1
                                && drops.getFirst().is(source.flower().asItem())
                                && drops.getFirst().getCount() == 1,
                        source + " with " + tool + " gives one " + source.flower() + ", not " + drops);
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void removingTheGroundDropsTheSourceFlowerOnce(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        h.setBlock(FLOWER, withered(FlowerSource.CORNFLOWER));
        h.getLevel().destroyBlock(h.absolutePos(GROUND), true);
        h.runAfterDelay(2, () -> {
            h.assertBlockPresent(Blocks.AIR, FLOWER);
            assertDrops(h, List.of(Items.CORNFLOWER, EcologyModule.DEAD_TISSUE.get()));
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void pickBlockGivesTheSourceFlower(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        h.setBlock(FLOWER, withered(FlowerSource.PINK_TULIP));
        ItemStack picked = EcologyModule.WITHERED_FLOWER
                .get()
                .getCloneItemStack(h.getLevel(), h.absolutePos(FLOWER), h.getBlockState(FLOWER));
        h.assertTrue(picked.is(Items.PINK_TULIP), "pick block gives the source flower, not " + picked);
        h.succeed();
    }

    /** Appearance is chosen from the position alone, so it is not saved, cannot be lost and never changes. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void appearanceIsNotSavedStateAndNeedsNoTicks(GameTestHelper h) {
        BlockState poppy = withered(FlowerSource.POPPY);
        CompoundTag saved = NbtUtils.writeBlockState(poppy);
        h.assertValueEqual(
                saved.getCompound("Properties").getAllKeys(), Set.of("source"), "the source is all it saves");
        h.assertValueEqual(
                NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK), saved),
                poppy,
                "and reads back as the same state");
        BlockPos at = h.absolutePos(FLOWER);
        h.assertValueEqual(
                poppy.getSeed(at), withered(FlowerSource.DANDELION).getSeed(at), "the look's seed is the position's");
        h.assertFalse(poppy.hasBlockEntity(), "no block entity");
        h.assertFalse(poppy.isRandomlyTicking(), "no random ticks");
        h.succeed();
    }

    /** A chunk written and read back, as a save does, holds the same flower at the same position. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void reloadKeepsTheFlower(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        h.setBlock(FLOWER, withered(FlowerSource.AZURE_BLUET));
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(FLOWER);
        LevelChunk chunk = level.getChunkAt(at);
        CompoundTag bytes = ChunkSerializer.write(level, chunk);
        ProtoChunk read = ChunkSerializer.read(
                level,
                level.getPoiManager(),
                new RegionStorageInfo("withered_flowers", level.dimension(), "chunk"),
                chunk.getPos(),
                bytes);
        BlockState restored = read.getBlockState(at);
        h.assertValueEqual(restored, withered(FlowerSource.AZURE_BLUET), "the flower read back from the chunk bytes");
        h.assertValueEqual(restored.getSeed(at), h.getBlockState(FLOWER).getSeed(at), "with the same look");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 150)
    public static void dormantSubstrateLeavesALaterFlowerAlone(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        h.setBlock(GROUND.east(), Blocks.GRASS_BLOCK);
        h.setBlock(GROUND.east().above(), Blocks.POPPY);
        h.runAfterDelay(100, () -> {
            h.assertBlockPresent(Blocks.GRASS_BLOCK, GROUND.east());
            h.assertBlockPresent(Blocks.POPPY, GROUND.east().above());
            EcologyTests.assertVigor(h, GROUND, 0);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void repeatedTreatmentOfADeadFlowerYieldsNothing(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        h.setBlock(FLOWER, withered(FlowerSource.DANDELION));
        for (Direction side : Direction.Plane.HORIZONTAL) h.setBlock(GROUND.relative(side), Blocks.OAK_PLANKS);
        Player boneMeal = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 2);
        EcologyTests.use(h, boneMeal, FLOWER, Direction.UP);
        EcologyTests.use(h, boneMeal, GROUND, Direction.UP);
        h.assertValueEqual(boneMeal.getMainHandItem().getCount(), 2, "bone meal is kept");
        Player culture = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 2);
        EcologyTests.use(h, culture, FLOWER, Direction.UP);
        EcologyTests.use(h, culture, GROUND, Direction.UP);
        h.assertValueEqual(culture.getMainHandItem().getCount(), 2, "culture is kept");
        assertWithered(h, FLOWER, FlowerSource.DANDELION);
        EcologyTests.assertStage(h, GROUND, SubstrateStage.MATURE);
        EcologyTests.assertVigor(h, GROUND, 0);
        h.runAfterDelay(5, () -> {
            assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aWitheredFlowerBurnsLikeAFlower(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        h.setBlock(FLOWER, withered(FlowerSource.OXEYE_DAISY));
        BlockPos at = h.absolutePos(FLOWER);
        BlockState state = h.getBlockState(FLOWER);
        h.assertValueEqual(
                state.getFlammability(h.getLevel(), at, Direction.UP),
                Blocks.OXEYE_DAISY.defaultBlockState().getFlammability(h.getLevel(), at, Direction.UP),
                "as flammable as the flower it was");
        h.assertValueEqual(
                state.getFireSpreadSpeed(h.getLevel(), at, Direction.UP),
                Blocks.OXEYE_DAISY.defaultBlockState().getFireSpreadSpeed(h.getLevel(), at, Direction.UP),
                "and catches fire as readily");
        h.succeed();
    }

    /**
     * Bamboo and the big dripleaf are rooted plants outside the bush family. Their collision keeps the wave away;
     * culture reaches their ground through a side face, and the plant on top is what refuses it.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void groundUnderBambooOrADripleafStaysUnconverted(GameTestHelper h) {
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(CENTER.north(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.south(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.east(), Blocks.GRASS_BLOCK);
        h.setBlock(CENTER.east().above(), Blocks.BAMBOO);
        h.setBlock(CENTER.west(), Blocks.DIRT);
        h.setBlock(CENTER.west().above(), Blocks.BIG_DRIPLEAF);
        Player boneMeal = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 1);
        EcologyTests.use(h, boneMeal, CENTER, Direction.UP);
        h.assertValueEqual(boneMeal.getMainHandItem().getCount(), 1, "no host to grow onto, so the bone meal is kept");
        Player culture = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 2);
        EcologyTests.use(h, culture, CENTER.east(), Direction.NORTH);
        EcologyTests.use(h, culture, CENTER.west(), Direction.NORTH);
        h.assertValueEqual(culture.getMainHandItem().getCount(), 2, "culture is refused under bamboo and a dripleaf");
        h.runAfterDelay(LivingSubstrateBlock.GROWTH_DELAY_TICKS + 2, () -> {
            h.assertBlockPresent(Blocks.GRASS_BLOCK, CENTER.east());
            h.assertBlockPresent(Blocks.BAMBOO, CENTER.east().above());
            h.assertBlockPresent(Blocks.DIRT, CENTER.west());
            h.assertBlockPresent(Blocks.BIG_DRIPLEAF, CENTER.west().above());
            assertNoItems(h);
            h.succeed();
        });
    }

    static BlockState withered(FlowerSource source) {
        return EcologyModule.WITHERED_FLOWER.get().defaultBlockState().setValue(WitheredFlowerBlock.SOURCE, source);
    }

    static void assertWithered(GameTestHelper h, BlockPos at, FlowerSource source) {
        h.assertBlockPresent(EcologyModule.WITHERED_FLOWER.get(), at);
        h.assertBlockState(
                at,
                (BlockState s) -> s.getValue(WitheredFlowerBlock.SOURCE) == source,
                () -> "expected a withered " + source.getSerializedName());
    }

    static void assertNoItems(GameTestHelper h) {
        List<ItemEntity> drops =
                h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(1));
        h.assertTrue(drops.isEmpty(), "infection spawned " + drops);
    }

    /** Exactly one item entity per expected item, each a stack of one. */
    static void assertDrops(GameTestHelper h, List<Item> expected) {
        List<ItemEntity> drops =
                h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(1));
        h.assertValueEqual(drops.size(), expected.size(), "drops " + drops);
        for (Item item : expected) {
            long count = drops.stream()
                    .filter(drop -> drop.getItem().is(item) && drop.getItem().getCount() == 1)
                    .count();
            h.assertValueEqual((int) count, 1, "one " + item + " among " + drops);
        }
    }
}
