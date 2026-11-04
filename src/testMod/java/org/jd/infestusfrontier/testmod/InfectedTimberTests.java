package org.jd.infestusfrontier.testmod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.BlightedLeavesBlock;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.InfectedLogBlock;
import org.jd.infestusfrontier.ecology.TreeSource;

/** Infected Logs, Infected Planks and Blighted Leaves: VEGETATION_INFECTION.md, "Trees". */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class InfectedTimberTests {
    /** Its own batch: these tests spawn drops and force far chunks. */
    private static final String BATCH = "infectedTimber";

    private static final BlockPos AT = new BlockPos(3, 1, 3);
    private static final int ROLLS = 3000;

    private static final BlockPos EXHIBIT_MIN = new BlockPos(47, -61, -13);
    private static final BlockPos EXHIBIT_MAX = new BlockPos(75, -50, -2);

    // --- logs ---

    /** One break of any infected log, in any axis, with any tool, drops one Infected Log and nothing else. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void breakingAnInfectedLogDropsOneInfectedLogAndNothingElse(GameTestHelper h) {
        List<ItemStack> tools = List.of(
                ItemStack.EMPTY,
                new ItemStack(Items.IRON_AXE),
                new ItemStack(Items.SHEARS),
                enchanted(h, Items.IRON_AXE, Enchantments.SILK_TOUCH, 1),
                enchanted(h, Items.IRON_AXE, Enchantments.FORTUNE, 3));
        for (TreeSource source : TreeSource.values()) {
            for (Direction.Axis axis : Direction.Axis.values()) {
                for (ItemStack tool : tools) {
                    h.setBlock(AT, log(source, axis));
                    TestDrops.miner(h, tool.copy()).gameMode.destroyBlock(h.absolutePos(AT));
                    h.assertBlockPresent(Blocks.AIR, AT);
                    h.assertValueEqual(
                            TestDrops.dropped(h),
                            Map.of(EcologyModule.INFECTED_LOG_ITEM.get(), 1),
                            "one Infected Log from " + source + " " + axis + " with " + tool);
                    TestDrops.clearDrops(h);
                }
            }
        }
        h.succeed();
    }

    /** Cutting a log, putting its item down and cutting it again never gives more than one log a cut. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void replacingAndCuttingALogNeverMultipliesWood(GameTestHelper h) {
        h.setBlock(AT, log(TreeSource.DARK_OAK, Direction.Axis.Y));
        int inventory = 0;
        for (int cut = 0; cut < 8; cut++) {
            TestDrops.miner(h, new ItemStack(Items.IRON_AXE)).gameMode.destroyBlock(h.absolutePos(AT));
            Map<Item, Integer> dropped = TestDrops.dropped(h);
            h.assertValueEqual(dropped, Map.of(EcologyModule.INFECTED_LOG_ITEM.get(), 1), "cut " + cut);
            inventory += dropped.get(EcologyModule.INFECTED_LOG_ITEM.get());
            TestDrops.clearDrops(h);
            // the item is placed again as the block item places it: its default state, whatever tree it came from
            h.setBlock(AT, EcologyModule.INFECTED_LOG.get());
            inventory--;
            h.assertValueEqual(inventory, 0, "nothing left over after cut " + cut);
        }
        h.succeed();
    }

    /** The log keeps axis and species through every state: save and reload, a chunk written and read back. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void logsAndLeavesKeepTheirStateThroughASave(GameTestHelper h) {
        List<BlockState> states = new ArrayList<>();
        for (TreeSource source : TreeSource.values()) {
            for (Direction.Axis axis : Direction.Axis.values()) states.add(log(source, axis));
            states.add(leaves(source, 3, true, source == TreeSource.BIRCH));
        }
        ServerLevel level = h.getLevel();
        for (int i = 0; i < states.size(); i++) h.setBlock(new BlockPos(1 + (i % 6), 2 + i / 6, 3), states.get(i));
        for (BlockState state : states) {
            CompoundTag saved = NbtUtils.writeBlockState(state);
            h.assertValueEqual(
                    NbtUtils.readBlockState(h.getLevel().holderLookup(Registries.BLOCK), saved),
                    state,
                    state + " reads back");
        }
        Map<ChunkPos, ProtoChunk> reads = new java.util.HashMap<>();
        for (int i = 0; i < states.size(); i++) {
            BlockPos at = h.absolutePos(new BlockPos(1 + (i % 6), 2 + i / 6, 3));
            LevelChunk chunk = level.getChunkAt(at);
            ProtoChunk read = reads.computeIfAbsent(
                    chunk.getPos(),
                    pos -> ChunkSerializer.read(
                            level,
                            level.getPoiManager(),
                            new RegionStorageInfo("infected_timber", level.dimension(), "chunk"),
                            pos,
                            ChunkSerializer.write(level, chunk)));
            h.assertValueEqual(read.getBlockState(at), states.get(i), "chunk read back " + i);
        }
        h.succeed();
    }

    // --- planks and recipes ---

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void oneInfectedLogMakesFourInfectedPlanks(GameTestHelper h) {
        ItemStack result = craft(h, 1, 1, List.of(new ItemStack(EcologyModule.INFECTED_LOG_ITEM.get())));
        h.assertTrue(result.is(EcologyModule.INFECTED_PLANKS_ITEM.get()), "planks: " + result);
        h.assertValueEqual(result.getCount(), 4, "four planks");
        List<RecipeHolder<?>> makers = new ArrayList<>();
        for (RecipeHolder<?> holder : h.getLevel().getRecipeManager().getRecipes())
            if (holder.id().getNamespace().equals("infestusfrontier")) makers.add(holder);
        long planks = makers.stream()
                .filter(r -> r.value()
                        .getResultItem(h.getLevel().registryAccess())
                        .is(EcologyModule.INFECTED_PLANKS_ITEM.get()))
                .count();
        h.assertValueEqual(planks, 1L, "one recipe makes Infected Planks");
        h.succeed();
    }

    /** No recipe turns planks, or anything, back into an Infected Log, and no mod recipe takes a log but the planks one. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void noRecipeMakesLogsOrGivesLogsSpecialUses(GameTestHelper h) {
        ItemStack log = new ItemStack(EcologyModule.INFECTED_LOG_ITEM.get());
        for (RecipeHolder<?> holder : h.getLevel().getRecipeManager().getRecipes()) {
            h.assertFalse(
                    holder.value().getResultItem(h.getLevel().registryAccess()).is(log.getItem()),
                    holder.id() + " makes Infected Logs");
            if (!holder.id().getNamespace().equals("infestusfrontier")
                    || holder.id().getPath().equals("infected_planks")) continue;
            h.assertFalse(
                    holder.value().getIngredients().stream().anyMatch(i -> i.test(log)),
                    holder.id() + " takes an Infected Log");
        }
        h.assertFalse(log.is(ItemTags.LOGS), "not an ordinary log item");
        h.assertFalse(log.is(ItemTags.OAK_LOGS), "not an oak log item");
        h.assertFalse(log.is(ItemTags.LOGS_THAT_BURN), "not a burning-log item");
        ItemStack oneLog = craft(h, 1, 1, List.of(log));
        h.assertFalse(oneLog.is(Items.OAK_PLANKS), "an Infected Log does not make oak planks");
        h.succeed();
    }

    /** Infected Planks stand in for ordinary planks through the planks tag: sticks, crafting table, chest, mixed with oak. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void infectedPlanksCraftSticksTablesAndChests(GameTestHelper h) {
        ItemStack planks = new ItemStack(EcologyModule.INFECTED_PLANKS_ITEM.get());
        h.assertTrue(planks.is(ItemTags.PLANKS), "planks tag");
        h.assertTrue(h.getBlockState(AT).isAir(), "an empty spot");
        h.setBlock(AT, EcologyModule.INFECTED_PLANKS.get());
        h.assertTrue(h.getBlockState(AT).is(BlockTags.PLANKS), "block planks tag");
        ItemStack sticks = craft(h, 1, 2, List.of(planks.copy(), planks.copy()));
        h.assertTrue(sticks.is(Items.STICK) && sticks.getCount() == 4, "four sticks: " + sticks);
        ItemStack mixed = craft(h, 1, 2, List.of(planks.copy(), new ItemStack(Items.OAK_PLANKS)));
        h.assertTrue(mixed.is(Items.STICK) && mixed.getCount() == 4, "sticks from mixed planks: " + mixed);
        ItemStack table = craft(h, 2, 2, List.of(planks.copy(), planks.copy(), planks.copy(), planks.copy()));
        h.assertTrue(table.is(Items.CRAFTING_TABLE), "crafting table: " + table);
        List<ItemStack> ring = new ArrayList<>();
        for (int i = 0; i < 9; i++) ring.add(i == 4 ? ItemStack.EMPTY : planks.copy());
        h.assertTrue(craft(h, 3, 3, ring).is(Items.CHEST), "chest");
        h.succeed();
    }

    /** Both burn as wood, 300 ticks, and they gain no biomass or other value: only the planks tag and fuel data. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void timberBurnsLikeWoodAndCarriesNothingElse(GameTestHelper h) {
        h.assertValueEqual(
                new ItemStack(EcologyModule.INFECTED_LOG_ITEM.get()).getBurnTime(RecipeType.SMELTING), 300, "log fuel");
        h.assertValueEqual(
                new ItemStack(EcologyModule.INFECTED_PLANKS_ITEM.get()).getBurnTime(RecipeType.SMELTING),
                300,
                "planks fuel");
        for (BlockState state : List.of(
                log(TreeSource.OAK, Direction.Axis.Y),
                EcologyModule.INFECTED_PLANKS.get().defaultBlockState(),
                leaves(TreeSource.OAK, 7, false, false))) {
            h.assertFalse(state.hasBlockEntity(), state + " has no block entity");
            h.assertFalse(state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock, "inert");
            h.assertTrue(state.getFlammability(h.getLevel(), h.absolutePos(AT), Direction.UP) > 0, state + " burns");
        }
        h.assertFalse(
                log(TreeSource.OAK, Direction.Axis.Y).isRandomlyTicking(), "a log takes no random tick, never spreads");
        h.assertFalse(EcologyModule.INFECTED_PLANKS.get().defaultBlockState().isRandomlyTicking(), "nor do planks");
        h.assertFalse(leaves(TreeSource.OAK, 1, false, false).isRandomlyTicking(), "leaves held by a log do not tick");
        h.succeed();
    }

    // --- leaves ---

    /** An infected log holds Blighted Leaves at distance 1 through vanilla's own log check; leaves chain it on. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void infectedLogsSupportBlightedLeavesThroughTheNormalMechanism(GameTestHelper h) {
        h.assertTrue(
                log(TreeSource.OAK, Direction.Axis.Y).is(BlockTags.LOGS), "the log tag is what vanilla leaves check");
        h.setBlock(AT.east(), leaves(TreeSource.SPRUCE, 7, false, false));
        h.setBlock(AT.east(2), leaves(TreeSource.SPRUCE, 7, false, false));
        h.setBlock(AT, log(TreeSource.SPRUCE, Direction.Axis.Y));
        h.runAfterDelay(6, () -> {
            h.assertValueEqual(
                    h.getBlockState(AT.east()), leaves(TreeSource.SPRUCE, 1, false, false), "next to the log");
            h.assertValueEqual(h.getBlockState(AT.east(2)), leaves(TreeSource.SPRUCE, 2, false, false), "one further");
            h.succeed();
        });
    }

    /** Vanilla leaves mix with blighted ones: an infected log holds an oak leaf, a plain log holds a blighted one. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void blightedAndLivingLeavesShareSupport(GameTestHelper h) {
        h.setBlock(AT.east(), Blocks.OAK_LEAVES.defaultBlockState());
        h.setBlock(AT.west(), leaves(TreeSource.OAK, 7, false, false));
        h.setBlock(AT.west(3), leaves(TreeSource.BIRCH, 7, false, false));
        h.setBlock(AT, log(TreeSource.OAK, Direction.Axis.Y));
        h.setBlock(AT.west(2), Blocks.OAK_LOG);
        h.runAfterDelay(6, () -> {
            h.assertValueEqual(h.getBlockState(AT.east()).getValue(LeavesBlock.DISTANCE), 1, "oak leaves by the log");
            h.assertValueEqual(h.getBlockState(AT.west()).getValue(LeavesBlock.DISTANCE), 1, "blighted by the logs");
            h.assertValueEqual(
                    h.getBlockState(AT.west(3)).getValue(LeavesBlock.DISTANCE), 1, "blighted by an ordinary log");
            h.succeed();
        });
    }

    /** Distance, persistence, waterlogging and species survive every update. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void blightedLeavesKeepSpeciesPersistenceAndWater(GameTestHelper h) {
        BlockState wet = leaves(TreeSource.JUNGLE, 7, true, true);
        h.setBlock(AT.above(), wet);
        h.setBlock(AT.north(), leaves(TreeSource.JUNGLE, 7, false, true));
        h.setBlock(AT, log(TreeSource.JUNGLE, Direction.Axis.X));
        h.runAfterDelay(6, () -> {
            BlockState up = h.getBlockState(AT.above());
            h.assertValueEqual(up.getValue(BlightedLeavesBlock.SOURCE), TreeSource.JUNGLE, "species");
            h.assertValueEqual(up.getValue(LeavesBlock.PERSISTENT), true, "persistent stays");
            h.assertValueEqual(up.getValue(LeavesBlock.WATERLOGGED), true, "waterlogged stays");
            h.assertValueEqual(up.getValue(LeavesBlock.DISTANCE), 1, "distance follows the log");
            h.assertTrue(up.getFluidState().isSource() && up.getFluidState().is(Fluids.WATER), "it holds water");
            BlockState north = h.getBlockState(AT.north());
            h.assertValueEqual(north.getValue(LeavesBlock.WATERLOGGED), true, "second leaf waterlogged");
            h.assertValueEqual(north.getValue(LeavesBlock.PERSISTENT), false, "second leaf not persistent");
            h.succeed();
        });
    }

    /** Leaves of a cut tree reach distance 7, decay on a random tick as vanilla leaves do and drop their species' loot. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void leavesDecayOnceTheirLogIsGoneAndPersistentOnesDoNot(GameTestHelper h) {
        h.setBlock(AT.east(), leaves(TreeSource.ACACIA, 7, false, false));
        h.setBlock(AT.east(2), leaves(TreeSource.ACACIA, 7, false, false));
        h.setBlock(AT.west(), leaves(TreeSource.ACACIA, 7, true, false));
        h.setBlock(AT, log(TreeSource.ACACIA, Direction.Axis.Y));
        h.runAfterDelay(6, () -> {
            h.assertValueEqual(h.getBlockState(AT.east(2)).getValue(LeavesBlock.DISTANCE), 2, "supported");
            h.assertFalse(h.getBlockState(AT.east()).isRandomlyTicking(), "held leaves do not tick");
            TestDrops.miner(h, new ItemStack(Items.IRON_AXE)).gameMode.destroyBlock(h.absolutePos(AT));
            TestDrops.clearDrops(h);
        });
        h.runAfterDelay(20, () -> {
            for (BlockPos leaf : List.of(AT.east(), AT.east(2))) {
                h.assertValueEqual(h.getBlockState(leaf).getValue(LeavesBlock.DISTANCE), 7, "cut off");
                h.assertTrue(h.getBlockState(leaf).isRandomlyTicking(), "now ticks");
                BlockState state = h.getBlockState(leaf);
                state.randomTick(h.getLevel(), h.absolutePos(leaf), h.getLevel().random);
                h.assertBlockPresent(Blocks.AIR, leaf);
            }
            BlockState kept = h.getBlockState(AT.west());
            h.assertValueEqual(kept.getValue(LeavesBlock.DISTANCE), 7, "the placed leaf is cut off too");
            h.assertFalse(kept.isRandomlyTicking(), "persistent leaves never decay");
            h.succeed();
        });
    }

    /** What falls from a blighted leaf, over many rolls, is exactly what the living leaves of its tree drop. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void blightedLeavesDropTheirSpeciesLoot(GameTestHelper h) {
        BlockPos at = h.absolutePos(AT);
        for (TreeSource source : TreeSource.values()) {
            Set<Item> living = new HashSet<>();
            Set<Item> blighted = new HashSet<>();
            for (int roll = 0; roll < ROLLS; roll++) {
                for (ItemStack drop : Block.getDrops(
                        source.leaves().defaultBlockState(), h.getLevel(), at, null, null, ItemStack.EMPTY))
                    living.add(drop.getItem());
                for (ItemStack drop :
                        Block.getDrops(leaves(source, 7, false, false), h.getLevel(), at, null, null, ItemStack.EMPTY))
                    blighted.add(drop.getItem());
            }
            h.assertValueEqual(blighted, living, source + " leaves drop what its living leaves do");
            h.assertTrue(blighted.contains(Items.STICK), source + " drops sticks");
            h.assertTrue(
                    blighted.stream()
                            .anyMatch(i -> BuiltInRegistries.ITEM
                                    .getKey(i)
                                    .getPath()
                                    .equals(source.getSerializedName() + "_sapling")),
                    source + " drops its own sapling: " + blighted);
            h.assertValueEqual(
                    blighted.contains(Items.APPLE),
                    source == TreeSource.OAK || source == TreeSource.DARK_OAK,
                    source + " apples");
            for (Item item : blighted)
                h.assertFalse(
                        BuiltInRegistries.ITEM.getKey(item).getPath().contains("infected"),
                        "no infected item from leaves");
        }
        h.succeed();
    }

    /** Shears and Silk Touch give the living leaves once; a blighted leaf never gives itself, a log or a second item. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void shearsAndSilkTouchGiveTheLivingLeavesOnce(GameTestHelper h) {
        BlockPos at = h.absolutePos(AT);
        for (TreeSource source : TreeSource.values()) {
            for (ItemStack tool :
                    List.of(new ItemStack(Items.SHEARS), enchanted(h, Items.IRON_HOE, Enchantments.SILK_TOUCH, 1))) {
                h.assertValueEqual(
                        TestDrops.describe(
                                Block.getDrops(leaves(source, 7, false, false), h.getLevel(), at, null, null, tool)),
                        Map.of(source.leaves().asItem(), 1),
                        source + " with " + tool);
                h.setBlock(AT, leaves(source, 2, true, false));
                TestDrops.miner(h, tool.copy()).gameMode.destroyBlock(at);
                h.assertValueEqual(TestDrops.dropped(h), Map.of(source.leaves().asItem(), 1), "a player's break, once");
                TestDrops.clearDrops(h);
            }
        }
        h.succeed();
    }

    /** Fortune raises the sapling chance of a blighted leaf as it does the living one. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void fortuneAppliesToBlightedLeaves(GameTestHelper h) {
        BlockPos at = h.absolutePos(AT);
        ItemStack fortune = enchanted(h, Items.IRON_HOE, Enchantments.FORTUNE, 3);
        int plain = 0;
        int lucky = 0;
        for (int roll = 0; roll < ROLLS; roll++) {
            for (ItemStack drop : Block.getDrops(
                    leaves(TreeSource.OAK, 7, false, false), h.getLevel(), at, null, null, ItemStack.EMPTY))
                if (drop.is(Items.OAK_SAPLING)) plain += drop.getCount();
            for (ItemStack drop :
                    Block.getDrops(leaves(TreeSource.OAK, 7, false, false), h.getLevel(), at, null, null, fortune))
                if (drop.is(Items.OAK_SAPLING)) lucky += drop.getCount();
        }
        h.assertTrue(lucky > plain, "Fortune III gave " + lucky + " saplings against " + plain);
        h.succeed();
    }

    /** A decaying leaf drops its tree's loot once and leaves no blighted item behind. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void decayingLeavesDropTheirSpeciesLootOnce(GameTestHelper h) {
        Set<Item> seen = new HashSet<>();
        for (int roll = 0; roll < 800; roll++) {
            h.setBlock(AT, leaves(TreeSource.BIRCH, 7, false, false));
            h.getBlockState(AT).randomTick(h.getLevel(), h.absolutePos(AT), h.getLevel().random);
            h.assertBlockPresent(Blocks.AIR, AT);
            Map<Item, Integer> dropped = TestDrops.dropped(h);
            h.assertTrue(
                    Set.of(Items.BIRCH_SAPLING, Items.STICK).containsAll(dropped.keySet()), "birch loot: " + dropped);
            h.assertTrue(dropped.getOrDefault(Items.BIRCH_SAPLING, 0) <= 1, "one sapling at most: " + dropped);
            h.assertTrue(dropped.getOrDefault(Items.STICK, 0) <= 2, "two sticks at most: " + dropped);
            seen.addAll(dropped.keySet());
            TestDrops.clearDrops(h);
        }
        h.assertTrue(seen.contains(Items.BIRCH_SAPLING) && seen.contains(Items.STICK), "birch loot: " + seen);
        h.assertFalse(seen.contains(Items.OAK_SAPLING) || seen.contains(Items.APPLE), "only birch loot: " + seen);
        h.succeed();
    }

    /** Pick block gives the log item, and for leaves the living leaves; the pillar keeps its axis on rotation. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void pickBlockAndRotationFollowTheEngine(GameTestHelper h) {
        for (TreeSource source : TreeSource.values()) {
            h.assertTrue(
                    pick(h, leaves(source, 3, false, false)).is(source.leaves().asItem()),
                    source + " leaves pick block");
        }
        BlockState x = log(TreeSource.BIRCH, Direction.Axis.X);
        h.assertValueEqual(
                x.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90),
                log(TreeSource.BIRCH, Direction.Axis.Z),
                "a quarter turn swaps x and z and keeps the species");
        h.assertTrue(
                pick(h, log(TreeSource.OAK, Direction.Axis.Y)).is(EcologyModule.INFECTED_LOG_ITEM.get()),
                "log pick block");
        h.succeed();
    }

    // --- the demo ---

    private static DemoExhibit exhibit(int slot) {
        return new DemoExhibit(EXHIBIT_MIN, EXHIBIT_MAX, slot);
    }

    /** The exhibit shows each species' log and leaves, a living and an infected oak, and supplies a table and tools. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void theDemoExhibitShowsTheTimberAndSuppliesIt(GameTestHelper h) {
        DemoExhibit yard = exhibit(0);
        yard.force(h);
        ServerLevel level = h.getLevel();
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    yard.build(h);
                    TreeSource[] sources = TreeSource.values();
                    for (int i = 0; i < sources.length; i++) {
                        h.assertValueEqual(
                                level.getBlockState(yard.at(new BlockPos(50 + 2 * i, -60, -11))),
                                log(sources[i], Direction.Axis.Y),
                                "the log of " + sources[i]);
                        h.assertValueEqual(
                                level.getBlockState(yard.at(new BlockPos(50 + 2 * i, -60, -8))),
                                leaves(sources[i], 1, true, false),
                                "the leaves of " + sources[i]);
                    }
                    h.assertTrue(
                            level.getBlockState(yard.at(new BlockPos(58, -60, -4)))
                                    .is(Blocks.CRAFTING_TABLE),
                            "a crafting table");
                    ChestBlockEntity supplies =
                            (ChestBlockEntity) level.getBlockEntity(yard.at(new BlockPos(59, -60, -4)));
                    h.assertTrue(
                            supplies != null && supplies.countItem(EcologyModule.INFECTED_LOG_ITEM.get()) >= 32,
                            "logs to craft");
                    ChestBlockEntity tools =
                            (ChestBlockEntity) level.getBlockEntity(yard.at(new BlockPos(60, -60, -4)));
                    h.assertTrue(
                            tools != null
                                    && tools.countItem(Items.IRON_AXE) == 1
                                    && tools.countItem(Items.SHEARS) == 1
                                    && tools.countItem(Items.IRON_HOE) == 3,
                            "an axe, shears and three hoes");
                    // the infected oak stands as the living one does, with the same leaf distances
                    BlockPos living = new BlockPos(65, -60, -8);
                    BlockPos infected = new BlockPos(71, -60, -8);
                    for (BlockPos offset : BlockPos.betweenClosed(-2, 0, -2, 2, 5, 2)) {
                        BlockState before = level.getBlockState(yard.at(living.offset(offset)));
                        BlockState after = level.getBlockState(yard.at(infected.offset(offset)));
                        h.assertValueEqual(before.isAir(), after.isAir(), "same silhouette at " + offset);
                        if (before.getBlock() == Blocks.OAK_LEAVES)
                            h.assertValueEqual(
                                    after.getValue(LeavesBlock.DISTANCE),
                                    before.getValue(LeavesBlock.DISTANCE),
                                    "same leaf distance at " + offset);
                    }
                    BlockPos trunk = yard.at(infected);
                    h.assertTrue(
                            level.getBlockState(trunk).getBlock() instanceof InfectedLogBlock, "an infected trunk");
                    // cutting the trunk of the infected oak: the log drops once
                    ServerPlayer miner = TestDrops.miner(h, new ItemStack(Items.IRON_AXE));
                    miner.moveTo(trunk.getX() + 0.5, trunk.getY(), trunk.getZ() - 1.5, 0F, 0F);
                    miner.gameMode.destroyBlock(trunk);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    BlockPos trunk = yard.at(new BlockPos(71, -60, -8));
                    List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(trunk).inflate(4));
                    h.assertValueEqual(drops.size(), 1, "one drop from the trunk: " + drops);
                    h.assertTrue(drops.get(0).getItem().is(EcologyModule.INFECTED_LOG_ITEM.get()), "an Infected Log");
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }

    // --- fixtures ---

    private static BlockState log(TreeSource source, Direction.Axis axis) {
        return EcologyModule.INFECTED_LOG
                .get()
                .defaultBlockState()
                .setValue(InfectedLogBlock.SOURCE, source)
                .setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState leaves(TreeSource source, int distance, boolean persistent, boolean waterlogged) {
        return EcologyModule.BLIGHTED_LEAVES
                .get()
                .defaultBlockState()
                .setValue(BlightedLeavesBlock.SOURCE, source)
                .setValue(LeavesBlock.DISTANCE, distance)
                .setValue(LeavesBlock.PERSISTENT, persistent)
                .setValue(LeavesBlock.WATERLOGGED, waterlogged);
    }

    private static ItemStack enchanted(
            GameTestHelper h,
            Item item,
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key,
            int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(
                h.getLevel()
                        .registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(key),
                level);
        return stack;
    }

    private static ItemStack pick(GameTestHelper h, BlockState state) {
        return state.getBlock().getCloneItemStack(h.getLevel(), h.absolutePos(AT), state);
    }

    /** The crafting result of {@code width} by {@code height} cells, empty when no recipe matches. */
    private static ItemStack craft(GameTestHelper h, int width, int height, List<ItemStack> cells) {
        CraftingInput input = CraftingInput.of(width, height, cells);
        return h.getLevel()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(holder -> holder.value().assemble(input, h.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }
}
