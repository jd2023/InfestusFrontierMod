package org.jd.infestusfrontier.testmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.BlightedDeadBushBlock;
import org.jd.infestusfrontier.ecology.BlightedUndergrowthBlock;
import org.jd.infestusfrontier.ecology.DeadBerryBushBlock;
import org.jd.infestusfrontier.ecology.DeadShrubBlock;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.LivingSubstrateBlock;
import org.jd.infestusfrontier.ecology.SaplingSource;
import org.jd.infestusfrontier.ecology.SubstrateStage;
import org.jd.infestusfrontier.ecology.UndergrowthSource;
import org.jd.infestusfrontier.ecology.WitheredSaplingBlock;

/** Shrubs, short grass, ferns and saplings withered by substrate growth: VEGETATION_INFECTION.md. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class WitheredShrubTests {
    /** Its own batch: these tests spawn drops and force far chunks, as the tall plant tests do. */
    private static final String BATCH = "witheredShrubs";

    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos PLANT = GROUND.above();
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);

    /** The supported plants in the order the demo's plots show them. */
    private static final List<BlockState> LIVING = livingPlants();

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void cultureWithersEverySupportedPlantAndKeepsItsIdentity(GameTestHelper h) {
        Player player = EcologyTests.holding(h, GameType.CREATIVE, EcologyModule.SPORE_CULTURE.get(), 1);
        int x = 1;
        for (BlockState plant : LIVING) {
            BlockPos ground = new BlockPos(x, 1, 1 + (x & 1));
            h.setBlock(ground, Blocks.GRASS_BLOCK);
            h.setBlock(ground.above(), plant);
            EcologyTests.use(h, player, ground, Direction.UP);
            EcologyTests.assertStage(h, ground, SubstrateStage.BASIC);
            h.assertValueEqual(h.getBlockState(ground.above()), dead(plant), "the dead form of " + plant);
            x++;
        }
        h.runAfterDelay(5, () -> {
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void cultureIsUsedUpOnAShrub(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.GRASS_BLOCK);
        h.setBlock(PLANT, berry(3));
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
        EcologyTests.use(h, player, GROUND, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 0, "the culture was used");
        h.assertValueEqual(h.getBlockState(PLANT), deadBerry(3), "the bush keeps its age");
        h.runAfterDelay(5, () -> {
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void theWaveWithersTheShrubsItReaches(GameTestHelper h) {
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(CENTER.north(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.south(), Blocks.OAK_PLANKS);
        h.setBlock(CENTER.east(), Blocks.DIRT);
        h.setBlock(CENTER.east().above(), berry(2));
        h.setBlock(CENTER.west(), Blocks.GRASS_BLOCK);
        h.setBlock(CENTER.west().above(), Blocks.SPRUCE_SAPLING);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 1);
        EcologyTests.use(h, player, CENTER, Direction.UP);
        h.runAfterDelay(LivingSubstrateBlock.GROWTH_DELAY_TICKS + 2, () -> {
            EcologyTests.assertStage(h, CENTER.east(), SubstrateStage.BASIC);
            EcologyTests.assertStage(h, CENTER.west(), SubstrateStage.BASIC);
            h.assertValueEqual(h.getBlockState(CENTER.east().above()), deadBerry(2), "the bush");
            h.assertValueEqual(h.getBlockState(CENTER.west().above()), sapling(SaplingSource.SPRUCE), "the sapling");
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    /** Excluded and unsupported vegetation keeps its ground and stays alive; a sapling of another species too. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void excludedVegetationAndItsGroundStayIntact(GameTestHelper h) {
        List<Block> excluded = List.of(
                Blocks.AZALEA,
                Blocks.TORCHFLOWER,
                Blocks.CHERRY_SAPLING,
                Blocks.BROWN_MUSHROOM,
                Blocks.MANGROVE_PROPAGULE);
        Player culture = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), excluded.size());
        int x = 1;
        for (Block plant : excluded) {
            BlockPos ground = new BlockPos(x, 1, 1 + (x & 1));
            h.setBlock(ground, Blocks.DIRT);
            h.getLevel()
                    .setBlock(
                            h.absolutePos(ground.above()),
                            plant.defaultBlockState(),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            EcologyTests.use(h, culture, ground, Direction.UP);
            x++;
        }
        h.assertValueEqual(culture.getMainHandItem().getCount(), excluded.size(), "culture is refused, none used up");
        x = 1;
        for (Block plant : excluded) {
            BlockPos ground = new BlockPos(x, 1, 1 + (x & 1));
            h.assertBlockPresent(Blocks.DIRT, ground);
            h.assertBlockPresent(plant, ground.above());
            x++;
        }
        h.runAfterDelay(5, () -> {
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    /** A crop on farmland and a potted sapling keep their ground: the pot is solid cover, the crop a rooted plant. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void aCropAndAPottedSaplingAreLeftAlone(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.FARMLAND);
        h.setBlock(PLANT, Blocks.WHEAT);
        h.setBlock(GROUND.east(2), Blocks.GRASS_BLOCK);
        h.setBlock(PLANT.east(2), Blocks.POTTED_OAK_SAPLING);
        Player culture = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 2);
        EcologyTests.use(h, culture, GROUND, Direction.UP);
        h.assertBlockPresent(Blocks.FARMLAND, GROUND);
        h.assertBlockPresent(Blocks.WHEAT, PLANT);
        EcologyTests.use(h, culture, GROUND.east(2), Direction.UP);
        h.assertBlockPresent(Blocks.GRASS_BLOCK, GROUND.east(2));
        h.assertBlockPresent(Blocks.POTTED_OAK_SAPLING, PLANT.east(2));
        h.assertValueEqual(culture.getMainHandItem().getCount(), 2, "no culture used");
        h.runAfterDelay(5, () -> {
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    /** At every age the dead bush's break loot is the living bush's: nothing, nothing, one or two, two or three. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void aDeadBerryBushDropsTheBerriesItsAgeWouldHave(GameTestHelper h) {
        BlockPos at = h.absolutePos(PLANT);
        int[] least = {0, 0, 1, 2};
        int[] most = {0, 0, 2, 3};
        for (int age = 0; age < 4; age++) {
            Set<Integer> counts = new HashSet<>();
            for (int roll = 0; roll < 100; roll++) {
                List<ItemStack> drops = Block.getDrops(deadBerry(age), h.getLevel(), at, null, null, ItemStack.EMPTY);
                int berries = 0;
                for (ItemStack drop : drops) {
                    h.assertTrue(drop.is(Items.SWEET_BERRIES), "age " + age + " drops only berries, not " + drop);
                    berries += drop.getCount();
                }
                counts.add(berries);
            }
            Set<Integer> expected = new HashSet<>();
            for (int n = least[age]; n <= most[age]; n++) expected.add(n);
            h.assertValueEqual(counts, expected, "berries from an age " + age + " bush");
        }
        ItemStack fortune = new ItemStack(Items.DIAMOND_PICKAXE);
        fortune.enchant(
                h.getLevel()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.FORTUNE),
                3);
        int most3 = 0;
        for (int roll = 0; roll < 100; roll++) {
            int berries = 0;
            for (ItemStack drop : Block.getDrops(deadBerry(3), h.getLevel(), at, null, null, fortune))
                berries += drop.getCount();
            h.assertTrue(berries >= 2, "Fortune never takes from the base count: " + berries);
            most3 = Math.max(most3, berries);
        }
        h.assertTrue(most3 > 3, "Fortune adds berries to an age 3 bush: " + most3);
        h.succeed();
    }

    /** A player's break gives the harvest once; the bush is then gone and there is nothing left to take. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void breakingADeadBerryBushHarvestsItOnce(GameTestHelper h) {
        for (int age = 0; age < 4; age++) {
            for (int roll = 0; roll < 6; roll++) {
                standDead(h, berry(age));
                TestDrops.miner(h, ItemStack.EMPTY).gameMode.destroyBlock(h.absolutePos(PLANT));
                h.assertBlockPresent(Blocks.AIR, PLANT);
                Map<Item, Integer> dropped = TestDrops.dropped(h);
                int berries = dropped.getOrDefault(Items.SWEET_BERRIES, 0);
                h.assertValueEqual(dropped.size(), berries == 0 ? 0 : 1, "only berries, once: " + dropped);
                h.assertTrue(
                        berries >= (age == 2 ? 1 : age == 3 ? 2 : 0) && berries <= (age == 2 ? 2 : age == 3 ? 3 : 0),
                        "age " + age + " gave " + berries);
                TestDrops.clearDrops(h);
            }
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void aDeadBerryBushCannotBePickedGrownOrFed(GameTestHelper h) {
        for (int age = 0; age < 4; age++) {
            standDead(h, berry(age));
            Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            EcologyTests.use(h, player, PLANT, Direction.UP);
            Player boneMeal = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 3);
            EcologyTests.use(h, boneMeal, PLANT, Direction.UP);
            EcologyTests.use(h, boneMeal, GROUND, Direction.UP);
            h.assertValueEqual(boneMeal.getMainHandItem().getCount(), 3, "bone meal is kept");
            h.assertValueEqual(h.getBlockState(PLANT), deadBerry(age), "no berries picked, none grown");
            WitheredFlowerTests.assertNoItems(h);
        }
        h.succeed();
    }

    /** Walking through the dead bush neither slows nor hurts. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void aDeadBerryBushDoesNotHurtWhatWalksThroughIt(GameTestHelper h) {
        standDead(h, berry(3));
        Player walker = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos at = h.absolutePos(PLANT);
        walker.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        walker.xOld = at.getX() + 1.5;
        walker.zOld = at.getZ() + 1.5;
        float health = walker.getHealth();
        h.getBlockState(PLANT).entityInside(h.getLevel(), at, walker);
        h.assertValueEqual(walker.getHealth(), health, "no contact damage");
        h.assertTrue(
                h.getBlockState(PLANT).getCollisionShape(h.getLevel(), at).isEmpty(), "and it can be walked through");
        h.succeed();
    }

    /** Tools and loot conditions are the source's: shears, bare hands, Silk Touch and Fortune. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void everyToolGetsTheSourcePlantsDrops(GameTestHelper h) {
        var enchantments = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        ItemStack fortune = new ItemStack(Items.DIAMOND_PICKAXE);
        fortune.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), 3);
        BlockPos at = h.absolutePos(PLANT);
        List<ItemStack> tools = List.of(
                ItemStack.EMPTY,
                new ItemStack(Items.SHEARS),
                silk,
                fortune,
                new ItemStack(Items.IRON_AXE),
                new ItemStack(Items.IRON_SWORD));
        for (ItemStack tool : tools) {
            boolean shears = tool.is(Items.SHEARS);
            Map<Item, Integer> bush = TestDrops.describe(Block.getDrops(
                    EcologyModule.BLIGHTED_DEAD_BUSH.get().defaultBlockState(), h.getLevel(), at, null, null, tool));
            if (shears) h.assertValueEqual(bush, Map.of(Items.DEAD_BUSH, 1), "a dead bush with shears");
            // vanilla's dead_bush table rolls 0 to 2 sticks (set_count uniform, explosion_decay) and ignores Fortune
            else
                h.assertTrue(
                        Set.of(Items.STICK).containsAll(bush.keySet()) && bush.getOrDefault(Items.STICK, 0) <= 2,
                        "a dead bush with " + tool + " gives sticks at most, not " + bush);
            for (UndergrowthSource source : UndergrowthSource.values()) {
                Map<Item, Integer> drops =
                        TestDrops.describe(Block.getDrops(undergrowth(source), h.getLevel(), at, null, null, tool));
                if (shears) h.assertValueEqual(drops, Map.of(source.plant().asItem(), 1), source + " with shears");
                else
                    h.assertTrue(
                            Set.of(Items.WHEAT_SEEDS).containsAll(drops.keySet()),
                            source + " with " + tool + " gives a seed at most, not " + drops);
            }
            for (SaplingSource source : SaplingSource.values())
                h.assertValueEqual(
                        TestDrops.describe(Block.getDrops(sapling(source), h.getLevel(), at, null, null, tool)),
                        Map.of(source.sapling().asItem(), 1),
                        source + " comes back as itself with " + tool);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void breakingADeadShrubGivesTheSourceHarvestOnce(GameTestHelper h) {
        for (SaplingSource source : SaplingSource.values()) {
            standDead(h, sapling(source));
            TestDrops.miner(h, ItemStack.EMPTY).gameMode.destroyBlock(h.absolutePos(PLANT));
            h.assertBlockPresent(Blocks.AIR, PLANT);
            h.assertValueEqual(
                    TestDrops.dropped(h), Map.of(source.sapling().asItem(), 1), "one " + source + " sapling");
            TestDrops.clearDrops(h);
        }
        for (UndergrowthSource source : UndergrowthSource.values()) {
            standDead(h, undergrowth(source));
            TestDrops.miner(h, new ItemStack(Items.SHEARS)).gameMode.destroyBlock(h.absolutePos(PLANT));
            h.assertBlockPresent(Blocks.AIR, PLANT);
            h.assertValueEqual(
                    TestDrops.dropped(h), Map.of(source.plant().asItem(), 1), "one " + source + " with shears");
            TestDrops.clearDrops(h);
        }
        standDead(h, EcologyModule.BLIGHTED_DEAD_BUSH.get().defaultBlockState());
        TestDrops.miner(h, new ItemStack(Items.SHEARS)).gameMode.destroyBlock(h.absolutePos(PLANT));
        h.assertValueEqual(TestDrops.dropped(h), Map.of(Items.DEAD_BUSH, 1), "one dead bush with shears");
        h.succeed();
    }

    /** A creative player's break goes through the block's own hook and the removal after it: no drop at all. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void aCreativeBreakDropsNothing(GameTestHelper h) {
        Player creative = EcologyTests.holding(h, GameType.CREATIVE, Items.AIR, 0);
        for (BlockState plant : LIVING) {
            standDead(h, plant);
            BlockPos at = h.absolutePos(PLANT);
            BlockState state = h.getBlockState(PLANT);
            state.getBlock().playerWillDestroy(h.getLevel(), at, state, creative);
            h.getLevel().removeBlock(at, false);
            h.assertTrue(TestDrops.dropped(h).isEmpty(), "creative breaking drops nothing: " + TestDrops.dropped(h));
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void removingTheGroundDropsTheSourceHarvestOnce(GameTestHelper h) {
        standDead(h, sapling(SaplingSource.ACACIA));
        h.getLevel().destroyBlock(h.absolutePos(GROUND), true);
        h.assertBlockPresent(Blocks.AIR, PLANT);
        h.assertValueEqual(
                TestDrops.dropped(h),
                Map.of(Items.ACACIA_SAPLING, 1, EcologyModule.DEAD_TISSUE.get(), 1),
                "the sapling and the cell's Dead Tissue, once");
        TestDrops.clearDrops(h);
        standDead(h, deadBerry(0));
        h.getLevel().destroyBlock(h.absolutePos(GROUND), true);
        h.assertValueEqual(
                TestDrops.dropped(h), Map.of(EcologyModule.DEAD_TISSUE.get(), 1), "a young bush gives nothing more");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void theDeadFormsNeverGrowAndAnswerNoBoneMeal(GameTestHelper h) {
        for (Block block : List.of(
                EcologyModule.DEAD_BERRY_BUSH.get(),
                EcologyModule.BLIGHTED_DEAD_BUSH.get(),
                EcologyModule.BLIGHTED_UNDERGROWTH.get(),
                EcologyModule.WITHERED_SAPLING.get())) {
            h.assertFalse(block instanceof BonemealableBlock, block + " answers no bone meal");
            h.assertFalse(block instanceof SaplingBlock, block + " grows no tree");
            h.assertFalse(block.defaultBlockState().isRandomlyTicking(), block + " takes no random ticks");
            h.assertFalse(block.defaultBlockState().hasBlockEntity(), block + " has no block entity");
        }
        h.succeed();
    }

    /** Neither bone meal nor time makes a Withered Sapling a tree. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 150)
    public static void aWitheredSaplingNeverGrowsATree(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        for (Direction side : Direction.Plane.HORIZONTAL) h.setBlock(GROUND.relative(side), Blocks.OAK_PLANKS);
        h.setBlock(PLANT, sapling(SaplingSource.OAK));
        Player boneMeal = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 4);
        for (int i = 0; i < 4; i++) EcologyTests.use(h, boneMeal, PLANT, Direction.UP);
        h.assertValueEqual(boneMeal.getMainHandItem().getCount(), 4, "bone meal is kept");
        ServerLevel level = h.getLevel();
        for (int i = 0; i < 40; i++) {
            BlockPos at = h.absolutePos(PLANT);
            level.getBlockState(at).randomTick(level, at, level.random);
            level.getBlockState(at).tick(level, at, level.random);
        }
        h.runAfterDelay(100, () -> {
            h.assertValueEqual(h.getBlockState(PLANT), sapling(SaplingSource.OAK), "still the sapling");
            h.assertBlockPresent(Blocks.AIR, PLANT.above());
            h.assertBlockPresent(Blocks.AIR, PLANT.above(2));
            h.assertBlockPresent(Blocks.AIR, PLANT.above(3));
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void repeatedTreatmentOfADeadShrubYieldsNothing(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        for (Direction side : Direction.Plane.HORIZONTAL) h.setBlock(GROUND.relative(side), Blocks.OAK_PLANKS);
        for (BlockState plant : List.of(deadBerry(3), sapling(SaplingSource.BIRCH))) {
            standDeadOn(h, plant);
            Player culture = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 2);
            EcologyTests.use(h, culture, PLANT, Direction.UP);
            EcologyTests.use(h, culture, GROUND, Direction.UP);
            h.assertValueEqual(culture.getMainHandItem().getCount(), 2, "culture is kept");
            Player boneMeal = EcologyTests.holding(h, GameType.SURVIVAL, Items.BONE_MEAL, 2);
            EcologyTests.use(h, boneMeal, GROUND, Direction.UP);
            h.assertValueEqual(boneMeal.getMainHandItem().getCount(), 2, "bone meal is kept");
            h.assertValueEqual(h.getBlockState(PLANT), plant, "the dead plant is unchanged");
            EcologyTests.assertStage(h, GROUND, SubstrateStage.MATURE);
            EcologyTests.assertVigor(h, GROUND, 0);
        }
        h.runAfterDelay(5, () -> {
            WitheredFlowerTests.assertNoItems(h);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 150)
    public static void dormantSubstrateLeavesALaterShrubAlone(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        h.setBlock(GROUND.east(), Blocks.GRASS_BLOCK);
        h.setBlock(PLANT.east(), berry(3));
        h.runAfterDelay(100, () -> {
            h.assertBlockPresent(Blocks.GRASS_BLOCK, GROUND.east());
            h.assertValueEqual(h.getBlockState(PLANT.east()), berry(3), "the living bush stands");
            EcologyTests.assertVigor(h, GROUND, 0);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void pickBlockGivesThePlantItWas(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        for (BlockState plant : LIVING) {
            standDead(h, plant);
            BlockState state = h.getBlockState(PLANT);
            ItemStack picked = state.getBlock().getCloneItemStack(h.getLevel(), h.absolutePos(PLANT), state);
            Item expected = plant.is(Blocks.SWEET_BERRY_BUSH)
                    ? Items.SWEET_BERRIES
                    : plant.getBlock().asItem();
            h.assertTrue(picked.is(expected), "pick block gives " + expected + ", not " + picked);
        }
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void aDeadShrubBurnsAsReadilyAsTheLivingOne(GameTestHelper h) {
        h.setBlock(GROUND, EcologyTests.mature());
        BlockPos at = h.absolutePos(PLANT);
        for (BlockState plant : LIVING) {
            standDead(h, plant);
            BlockState dead = h.getBlockState(PLANT);
            h.assertTrue(
                    dead.getFlammability(h.getLevel(), at, Direction.UP) > 0
                            && dead.getFireSpreadSpeed(h.getLevel(), at, Direction.UP) > 0,
                    plant + " burns");
        }
        h.succeed();
    }

    /** A chunk written and read back, as a save does, holds every dead plant as it was, with the same look. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void reloadKeepsEveryDeadPlant(GameTestHelper h) {
        int x = 1;
        for (BlockState plant : LIVING) {
            BlockPos ground = new BlockPos(x, 1, 3);
            h.setBlock(ground, EcologyTests.mature());
            h.setBlock(ground.above(), dead(plant));
            x++;
        }
        ServerLevel level = h.getLevel();
        Map<ChunkPos, ProtoChunk> reads = new HashMap<>();
        x = 1;
        for (BlockState plant : LIVING) {
            BlockPos at = h.absolutePos(new BlockPos(x, 2, 3));
            LevelChunk chunk = level.getChunkAt(at);
            ProtoChunk read = reads.computeIfAbsent(
                    chunk.getPos(),
                    pos -> ChunkSerializer.read(
                            level,
                            level.getPoiManager(),
                            new RegionStorageInfo("withered_shrubs", level.dimension(), "chunk"),
                            pos,
                            ChunkSerializer.write(level, chunk)));
            h.assertValueEqual(read.getBlockState(at), dead(plant), plant + " read back");
            h.assertValueEqual(
                    read.getBlockState(at).getSeed(at), level.getBlockState(at).getSeed(at), "with the same look");
            x++;
        }
        h.succeed();
    }

    /** What is saved is the identity the harvest needs, and nothing of the look. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void onlyTheHarvestIdentityIsSaved(GameTestHelper h) {
        Map<BlockState, Set<String>> keys = Map.of(
                deadBerry(2), Set.of("age"),
                undergrowth(UndergrowthSource.FERN), Set.of("source"),
                sapling(SaplingSource.DARK_OAK), Set.of("source"),
                EcologyModule.BLIGHTED_DEAD_BUSH.get().defaultBlockState(), Set.of());
        keys.forEach((state, expected) -> {
            CompoundTag saved = NbtUtils.writeBlockState(state);
            h.assertValueEqual(saved.getCompound("Properties").getAllKeys(), expected, state + " saves its identity");
            h.assertValueEqual(
                    NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK), saved),
                    state,
                    "and reads back as the same state");
        });
        h.succeed();
    }

    /** Tints come from the position (or the species) alone: stable, varied, and the blockstates list several looks. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void looksAreStableAndVaried(GameTestHelper h) throws Exception {
        Set<Integer> berry = new HashSet<>();
        Set<Integer> bush = new HashSet<>();
        Set<Integer> grass = new HashSet<>();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockPos pos = new BlockPos(x, 40, z);
                h.assertValueEqual(DeadBerryBushBlock.tintAt(pos), DeadBerryBushBlock.tintAt(pos), "stable at " + pos);
                berry.add(DeadBerryBushBlock.tintAt(pos));
                bush.add(BlightedDeadBushBlock.tintAt(pos));
                grass.add(BlightedUndergrowthBlock.tintAt(pos));
            }
        }
        h.assertValueEqual(berry.size(), 4, "four berry bush tints across a patch");
        h.assertValueEqual(bush.size(), 4, "four dead bush tints across a patch");
        h.assertValueEqual(grass.size(), 4, "four undergrowth tints across a patch");
        Set<Integer> species = new HashSet<>();
        for (SaplingSource source : SaplingSource.values()) species.add(WitheredSaplingBlock.tintOf(sapling(source)));
        h.assertValueEqual(species.size(), 6, "each species has its own tone");
        for (String block :
                List.of("dead_berry_bush", "blighted_dead_bush", "blighted_undergrowth", "withered_sapling")) {
            JsonObject variants;
            try (InputStream in = WitheredSaplingBlock.class.getResourceAsStream(
                    "/assets/infestusfrontier/blockstates/" + block + ".json")) {
                variants = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .getAsJsonObject()
                        .getAsJsonObject("variants");
            }
            h.assertTrue(!variants.keySet().isEmpty(), block + " lists its states");
            for (String key : variants.keySet()) {
                JsonArray looks = variants.getAsJsonArray(key);
                h.assertTrue(looks.size() >= 4, block + " [" + key + "] has several looks");
            }
        }
        h.succeed();
    }

    // ---- the exhibit as the demo builds it ----

    // the shrub exhibit of captures/demo.json: a living plot, its dead twin, an excluded plot, a chest and signs
    private static final BlockPos EXHIBIT_MIN = new BlockPos(30, -62, -14);
    private static final BlockPos EXHIBIT_MAX = new BlockPos(46, -57, -2);
    private static final int FIRST_X = 32;
    private static final int LIVING_Z = -11;
    private static final int DEAD_Z = -7;
    private static final int EXCLUDED_Z = -3;
    private static final BlockPos CHEST = new BlockPos(32, -60, -13);
    private static final List<Block> EXCLUDED_PLANTS = List.of(
            Blocks.AZALEA,
            Blocks.FLOWERING_AZALEA,
            Blocks.TORCHFLOWER,
            Blocks.CHERRY_SAPLING,
            Blocks.POTTED_OAK_SAPLING,
            Blocks.WHEAT);

    private static DemoExhibit exhibit(int slot) {
        return new DemoExhibit(EXHIBIT_MIN, EXHIBIT_MAX, slot);
    }

    /** The player needs nothing but what the exhibit's chest holds: culture, bone meal and shears. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void theDemoExhibitSuppliesAndWithersTheShrubsAndLeavesTheRest(GameTestHelper h) {
        DemoExhibit yard = exhibit(0);
        yard.force(h);
        ServerLevel level = h.getLevel();
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    yard.build(h);
                    ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(yard.at(CHEST));
                    h.assertTrue(chest != null, "the supply chest stands");
                    h.assertTrue(chest.countItem(Items.BONE_MEAL) >= 4, "bone meal in the chest");
                    h.assertTrue(
                            chest.countItem(EcologyModule.SPORE_CULTURE.get()) >= LIVING.size(),
                            "culture for every plant");
                    h.assertTrue(chest.countItem(Items.SHEARS) >= 1, "shears for the undergrowth");
                    Player gardener = EcologyTests.holding(
                            h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), LIVING.size());
                    for (int i = 0; i < LIVING.size(); i++) {
                        BlockPos plant = yard.at(new BlockPos(FIRST_X + i, -60, LIVING_Z));
                        h.assertValueEqual(level.getBlockState(plant), LIVING.get(i), "the living plant " + i);
                        EcologyTests.useCulture(level, gardener, plant.below());
                        h.assertValueEqual(level.getBlockState(plant), dead(LIVING.get(i)), "its dead form " + i);
                        h.assertTrue(
                                level.getBlockState(plant.below()).is(EcologyModule.LIVING_SUBSTRATE.get()),
                                "over a cell");
                        h.assertValueEqual(
                                level.getBlockState(yard.at(new BlockPos(FIRST_X + i, -60, DEAD_Z))),
                                dead(LIVING.get(i)),
                                "the exhibit's dead twin " + i);
                    }
                    h.assertValueEqual(gardener.getMainHandItem().getCount(), 0, "one culture a plant");
                    Player spare = EcologyTests.holding(h, GameType.SURVIVAL, EcologyModule.SPORE_CULTURE.get(), 1);
                    for (int i = 0; i < EXCLUDED_PLANTS.size(); i++) {
                        BlockPos plant = yard.at(new BlockPos(FIRST_X + i, -60, EXCLUDED_Z));
                        h.assertTrue(level.getBlockState(plant).is(EXCLUDED_PLANTS.get(i)), "excluded plant " + i);
                        EcologyTests.useCulture(level, spare, plant.below());
                        h.assertTrue(level.getBlockState(plant).is(EXCLUDED_PLANTS.get(i)), "it stands, untouched");
                        h.assertFalse(
                                level.getBlockState(plant.below()).is(EcologyModule.LIVING_SUBSTRATE.get()),
                                "on ground the culture did not take");
                    }
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    List<ItemEntity> drops =
                            level.getEntitiesOfClass(ItemEntity.class, new AABB(yard.at(CHEST)).inflate(16));
                    h.assertTrue(drops.isEmpty(), "infection dropped nothing, not " + drops);
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }

    /** Harvest from the exhibit's dead plot: a sapling, berries by age, short grass with shears. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void theDemoExhibitsDeadPlantsHarvestOnce(GameTestHelper h) {
        DemoExhibit yard = exhibit(1);
        yard.force(h);
        ServerLevel level = h.getLevel();
        BlockPos oak = yard.at(new BlockPos(FIRST_X + 7, -60, DEAD_Z));
        BlockPos grass = yard.at(new BlockPos(FIRST_X + 5, -60, DEAD_Z));
        BlockPos young = yard.at(new BlockPos(FIRST_X, -60, DEAD_Z));
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    yard.build(h);
                    ServerPlayer miner = TestDrops.miner(h, new ItemStack(Items.SHEARS));
                    miner.gameMode.destroyBlock(oak);
                    miner.gameMode.destroyBlock(grass);
                    miner.gameMode.destroyBlock(young);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    Map<Item, Integer> sums = new TreeMap<>(
                            (a, b) -> BuiltInRegistries.ITEM.getKey(a).compareTo(BuiltInRegistries.ITEM.getKey(b)));
                    for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(grass).inflate(16)))
                        sums.merge(drop.getItem().getItem(), drop.getItem().getCount(), Integer::sum);
                    h.assertValueEqual(
                            sums, Map.of(Items.OAK_SAPLING, 1, Items.SHORT_GRASS, 1), "a sapling and a short grass");
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }

    // --- fixtures ---

    /** The four berry ages, a dead bush, short grass, a fern and the six saplings, as they stand in the demo. */
    private static List<BlockState> livingPlants() {
        List<BlockState> all = new ArrayList<>();
        for (int age = 0; age < 4; age++) all.add(berry(age));
        all.add(Blocks.DEAD_BUSH.defaultBlockState());
        all.add(Blocks.SHORT_GRASS.defaultBlockState());
        all.add(Blocks.FERN.defaultBlockState());
        for (SaplingSource source : SaplingSource.values())
            all.add(source.sapling().defaultBlockState());
        return List.copyOf(all);
    }

    private static BlockState berry(int age) {
        return Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, age);
    }

    private static BlockState deadBerry(int age) {
        return EcologyModule.DEAD_BERRY_BUSH.get().defaultBlockState().setValue(DeadBerryBushBlock.AGE, age);
    }

    private static BlockState undergrowth(UndergrowthSource source) {
        return EcologyModule.BLIGHTED_UNDERGROWTH
                .get()
                .defaultBlockState()
                .setValue(BlightedUndergrowthBlock.SOURCE, source);
    }

    private static BlockState sapling(SaplingSource source) {
        return EcologyModule.WITHERED_SAPLING.get().defaultBlockState().setValue(WitheredSaplingBlock.SOURCE, source);
    }

    /** The dead form that {@code plant} becomes. */
    private static BlockState dead(BlockState plant) {
        if (plant.is(Blocks.SWEET_BERRY_BUSH)) return deadBerry(plant.getValue(SweetBerryBushBlock.AGE));
        if (plant.is(Blocks.DEAD_BUSH))
            return EcologyModule.BLIGHTED_DEAD_BUSH.get().defaultBlockState();
        var grass = UndergrowthSource.of(plant.getBlock());
        if (grass.isPresent()) return undergrowth(grass.get());
        return sapling(SaplingSource.of(plant.getBlock()).orElseThrow());
    }

    /** The dead form of {@code plant}, or {@code plant} itself when it is already dead, on mature ground. */
    private static void standDead(GameTestHelper h, BlockState plant) {
        h.setBlock(GROUND, EcologyTests.mature());
        standDeadOn(h, plant);
    }

    private static void standDeadOn(GameTestHelper h, BlockState plant) {
        h.setBlock(PLANT, plant.getBlock() instanceof DeadShrubBlock ? plant : dead(plant));
    }
}
