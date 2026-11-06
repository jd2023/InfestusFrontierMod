package org.jd.infestusfrontier.testmod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
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
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.InfectedLogBlock;

/** Infected Logs, Blighted Leaves and the Infected Planks old worlds keep: VEGETATION_INFECTION.md, "Trees". */
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
        for (Direction.Axis axis : Direction.Axis.values()) {
            for (ItemStack tool : tools) {
                h.setBlock(AT, log(axis));
                TestDrops.miner(h, tool.copy()).gameMode.destroyBlock(h.absolutePos(AT));
                h.assertBlockPresent(Blocks.AIR, AT);
                h.assertValueEqual(
                        TestDrops.dropped(h),
                        Map.of(EcologyModule.INFECTED_LOG_ITEM.get(), 1),
                        "one Infected Log from " + axis + " with " + tool);
                TestDrops.clearDrops(h);
            }
        }
        h.succeed();
    }

    /** Cutting a log, putting its item down and cutting it again never gives more than one log a cut. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void replacingAndCuttingALogNeverMultipliesWood(GameTestHelper h) {
        h.setBlock(AT, log(Direction.Axis.Y));
        int inventory = 0;
        for (int cut = 0; cut < 8; cut++) {
            TestDrops.miner(h, new ItemStack(Items.IRON_AXE)).gameMode.destroyBlock(h.absolutePos(AT));
            Map<Item, Integer> dropped = TestDrops.dropped(h);
            h.assertValueEqual(dropped, Map.of(EcologyModule.INFECTED_LOG_ITEM.get(), 1), "cut " + cut);
            inventory += dropped.get(EcologyModule.INFECTED_LOG_ITEM.get());
            TestDrops.clearDrops(h);
            // the item is placed again as the block item places it: the one log, whatever tree it came from
            h.setBlock(AT, EcologyModule.INFECTED_LOG.get());
            inventory--;
            h.assertValueEqual(inventory, 0, "nothing left over after cut " + cut);
        }
        h.succeed();
    }

    /** One block, one item, one state per axis: the log and leaves carry no tree and no data component. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void oneMaterialWhateverTheTree(GameTestHelper h) {
        h.assertValueEqual(
                List.copyOf(
                        EcologyModule.INFECTED_LOG.get().getStateDefinition().getProperties()),
                List.of(RotatedPillarBlock.AXIS),
                "an Infected Log keeps only its axis");
        h.assertValueEqual(
                List.copyOf(
                        EcologyModule.BLIGHTED_LEAVES.get().getStateDefinition().getProperties()),
                List.copyOf(Blocks.OAK_LEAVES.getStateDefinition().getProperties()),
                "Blighted Leaves keep only what vanilla leaves keep");
        for (Item item : List.of(EcologyModule.INFECTED_LOG_ITEM.get(), EcologyModule.BLIGHTED_LEAVES_ITEM.get()))
            h.assertValueEqual(
                    new ItemStack(item).getComponentsPatch(),
                    DataComponentPatch.EMPTY,
                    item + " carries nothing of the tree");
        for (String path : List.of("infected_log", "blighted_leaves")) {
            var id = ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(id) && BuiltInRegistries.ITEM.containsKey(id), path);
            long kin = BuiltInRegistries.BLOCK.keySet().stream()
                    .filter(key -> key.getNamespace().equals("infestusfrontier")
                            && key.getPath().contains(path))
                    .count();
            h.assertValueEqual(kin, 1L, "one " + path + " block, no species copies");
        }
        h.succeed();
    }

    /** The log keeps its axis and the leaves their state: written as a block state and in a chunk, read back. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void logsAndLeavesKeepTheirStateThroughASave(GameTestHelper h) {
        List<BlockState> states = new ArrayList<>();
        for (Direction.Axis axis : Direction.Axis.values()) states.add(log(axis));
        for (int distance = 1; distance <= 7; distance += 3)
            for (boolean persistent : List.of(false, true))
                for (boolean waterlogged : List.of(false, true)) states.add(leaves(distance, persistent, waterlogged));
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

    /** A state written by an older version with its tree's {@code source} reads as the one block with its state. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void anOldStateWithASourceReadsAsTheOneBlock(GameTestHelper h) {
        var blocks = h.getLevel().holderLookup(Registries.BLOCK);
        CompoundTag log = NbtUtils.writeBlockState(log(Direction.Axis.Z));
        log.getCompound("Properties").putString("source", "birch");
        h.assertValueEqual(NbtUtils.readBlockState(blocks, log), log(Direction.Axis.Z), "the log keeps its axis");
        CompoundTag leaf = NbtUtils.writeBlockState(leaves(4, true, true));
        leaf.getCompound("Properties").putString("source", "dark_oak");
        h.assertValueEqual(NbtUtils.readBlockState(blocks, leaf), leaves(4, true, true), "the leaf keeps its state");
        h.succeed();
    }

    // --- planks, kept for old worlds ---

    /** Nothing makes Infected Planks or Infected Logs any more, and no recipe of the mod takes an Infected Log. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void noRecipeMakesPlanksOrLogsOrTakesALog(GameTestHelper h) {
        ItemStack log = new ItemStack(EcologyModule.INFECTED_LOG_ITEM.get());
        for (RecipeHolder<?> holder : h.getLevel().getRecipeManager().getRecipes()) {
            ItemStack result = holder.value().getResultItem(h.getLevel().registryAccess());
            h.assertFalse(result.is(log.getItem()), holder.id() + " makes Infected Logs");
            h.assertFalse(result.is(EcologyModule.INFECTED_PLANKS_ITEM.get()), holder.id() + " makes Infected Planks");
            if (!holder.id().getNamespace().equals("infestusfrontier")) continue;
            h.assertFalse(
                    holder.value().getIngredients().stream().anyMatch(i -> i.test(log)),
                    holder.id() + " takes an Infected Log");
        }
        h.assertTrue(craft(h, 1, 1, List.of(log)).isEmpty(), "a log alone crafts nothing");
        h.assertFalse(log.is(ItemTags.LOGS), "not an ordinary log item");
        h.assertFalse(log.is(ItemTags.OAK_LOGS), "not an oak log item");
        h.assertFalse(log.is(ItemTags.LOGS_THAT_BURN), "not a burning-log item");
        h.succeed();
    }

    /** Infected Planks an old world holds still stand in for ordinary planks: sticks, crafting table, chest. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void oldInfectedPlanksStillCraftSticksTablesAndChests(GameTestHelper h) {
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
        TestDrops.miner(h, new ItemStack(Items.IRON_AXE)).gameMode.destroyBlock(h.absolutePos(AT));
        h.assertValueEqual(TestDrops.dropped(h), Map.of(EcologyModule.INFECTED_PLANKS_ITEM.get(), 1), "a placed plank");
        TestDrops.clearDrops(h);
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
                log(Direction.Axis.Y),
                EcologyModule.INFECTED_PLANKS.get().defaultBlockState(),
                leaves(7, false, false))) {
            h.assertFalse(state.hasBlockEntity(), state + " has no block entity");
            h.assertFalse(state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock, "inert");
            h.assertTrue(state.getFlammability(h.getLevel(), h.absolutePos(AT), Direction.UP) > 0, state + " burns");
        }
        h.assertFalse(log(Direction.Axis.Y).isRandomlyTicking(), "a log takes no random tick, never spreads");
        h.assertFalse(EcologyModule.INFECTED_PLANKS.get().defaultBlockState().isRandomlyTicking(), "nor do planks");
        h.assertFalse(leaves(1, false, false).isRandomlyTicking(), "leaves held by a log do not tick");
        h.succeed();
    }

    // --- leaves ---

    /** An infected log holds Blighted Leaves at distance 1 through vanilla's own log check; leaves chain it on. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void infectedLogsSupportBlightedLeavesThroughTheNormalMechanism(GameTestHelper h) {
        h.assertTrue(log(Direction.Axis.Y).is(BlockTags.LOGS), "the log tag is what vanilla leaves check");
        h.setBlock(AT.east(), leaves(7, false, false));
        h.setBlock(AT.east(2), leaves(7, false, false));
        h.setBlock(AT, log(Direction.Axis.Y));
        h.runAfterDelay(6, () -> {
            h.assertValueEqual(h.getBlockState(AT.east()), leaves(1, false, false), "next to the log");
            h.assertValueEqual(h.getBlockState(AT.east(2)), leaves(2, false, false), "one further");
            h.succeed();
        });
    }

    /** Vanilla leaves mix with blighted ones: an infected log holds an oak leaf, a plain log holds a blighted one. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void blightedAndLivingLeavesShareSupport(GameTestHelper h) {
        h.setBlock(AT.east(), Blocks.OAK_LEAVES.defaultBlockState());
        h.setBlock(AT.west(), leaves(7, false, false));
        h.setBlock(AT.west(3), leaves(7, false, false));
        h.setBlock(AT, log(Direction.Axis.Y));
        h.setBlock(AT.west(2), Blocks.OAK_LOG);
        h.runAfterDelay(6, () -> {
            h.assertValueEqual(h.getBlockState(AT.east()).getValue(LeavesBlock.DISTANCE), 1, "oak leaves by the log");
            h.assertValueEqual(h.getBlockState(AT.west()).getValue(LeavesBlock.DISTANCE), 1, "blighted by the logs");
            h.assertValueEqual(
                    h.getBlockState(AT.west(3)).getValue(LeavesBlock.DISTANCE), 1, "blighted by an ordinary log");
            h.succeed();
        });
    }

    /** Distance, persistence and waterlogging survive every update. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void blightedLeavesKeepPersistenceAndWater(GameTestHelper h) {
        h.setBlock(AT.above(), leaves(7, true, true));
        h.setBlock(AT.north(), leaves(7, false, true));
        h.setBlock(AT, log(Direction.Axis.X));
        h.runAfterDelay(6, () -> {
            BlockState up = h.getBlockState(AT.above());
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

    /** Leaves of a cut tree reach distance 7 and decay on a random tick as vanilla leaves do, dropping nothing. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void leavesDecayOnceTheirLogIsGoneAndPersistentOnesDoNot(GameTestHelper h) {
        h.setBlock(AT.east(), leaves(7, false, false));
        h.setBlock(AT.east(2), leaves(7, false, false));
        h.setBlock(AT.west(), leaves(7, true, false));
        h.setBlock(AT, log(Direction.Axis.Y));
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
            h.assertValueEqual(TestDrops.dropped(h), Map.of(), "decay drops nothing");
            BlockState kept = h.getBlockState(AT.west());
            h.assertValueEqual(kept.getValue(LeavesBlock.DISTANCE), 7, "the placed leaf is cut off too");
            h.assertFalse(kept.isRandomlyTicking(), "persistent leaves never decay");
            h.succeed();
        });
    }

    /**
     * Over many rolls, a blighted leaf broken by hand, an axe, a hoe or a Fortune III hoe, or left to decay, drops
     * nothing at all: no sapling, stick, apple or fruit of any tree.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void blightedLeavesDropNothingWithoutShearsOrSilkTouch(GameTestHelper h) {
        BlockPos at = h.absolutePos(AT);
        List<ItemStack> tools = List.of(
                ItemStack.EMPTY,
                new ItemStack(Items.IRON_AXE),
                new ItemStack(Items.IRON_HOE),
                enchanted(h, Items.IRON_HOE, Enchantments.FORTUNE, 3));
        for (ItemStack tool : tools)
            for (int roll = 0; roll < ROLLS; roll++)
                h.assertValueEqual(
                        Block.getDrops(leaves(7, false, false), h.getLevel(), at, null, null, tool),
                        List.of(),
                        "nothing with " + tool);
        for (int roll = 0; roll < 200; roll++) {
            h.setBlock(AT, leaves(7, false, false));
            h.getBlockState(AT).randomTick(h.getLevel(), at, h.getLevel().random);
            h.assertBlockPresent(Blocks.AIR, AT);
            h.assertValueEqual(TestDrops.dropped(h), Map.of(), "a decayed leaf leaves nothing");
        }
        for (ItemStack tool : tools) {
            h.setBlock(AT, leaves(2, true, false));
            TestDrops.miner(h, tool.copy()).gameMode.destroyBlock(at);
            h.assertValueEqual(TestDrops.dropped(h), Map.of(), "a player's break with " + tool);
        }
        h.succeed();
    }

    /** Shears and Silk Touch give one Blighted Leaves, never the living leaves, a log or a second item. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void shearsAndSilkTouchGiveBlightedLeavesOnce(GameTestHelper h) {
        BlockPos at = h.absolutePos(AT);
        for (ItemStack tool :
                List.of(new ItemStack(Items.SHEARS), enchanted(h, Items.IRON_HOE, Enchantments.SILK_TOUCH, 1))) {
            h.assertValueEqual(
                    TestDrops.describe(Block.getDrops(leaves(7, false, false), h.getLevel(), at, null, null, tool)),
                    Map.of(EcologyModule.BLIGHTED_LEAVES_ITEM.get(), 1),
                    "with " + tool);
            h.setBlock(AT, leaves(2, true, true));
            TestDrops.miner(h, tool.copy()).gameMode.destroyBlock(at);
            h.assertValueEqual(
                    TestDrops.dropped(h),
                    Map.of(EcologyModule.BLIGHTED_LEAVES_ITEM.get(), 1),
                    "a player's break, once");
            TestDrops.clearDrops(h);
        }
        h.succeed();
    }

    /** Placed by hand, the foliage is persistent leaves that never decay, as placed vanilla leaves are. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void placedBlightedLeavesArePersistent(GameTestHelper h) {
        BlockState placed = EcologyModule.BLIGHTED_LEAVES
                .get()
                .getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(
                        h.getLevel(),
                        null,
                        net.minecraft.world.InteractionHand.MAIN_HAND,
                        new ItemStack(EcologyModule.BLIGHTED_LEAVES_ITEM.get()),
                        new net.minecraft.world.phys.BlockHitResult(
                                h.absolutePos(AT).getCenter(), Direction.UP, h.absolutePos(AT), false)));
        h.assertTrue(placed != null && placed.getValue(LeavesBlock.PERSISTENT), "persistent: " + placed);
        h.succeed();
    }

    /** Pick block gives the log item and the Blighted Leaves item; the pillar keeps its axis on rotation. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void pickBlockAndRotationFollowTheEngine(GameTestHelper h) {
        h.assertTrue(pick(h, leaves(3, false, false)).is(EcologyModule.BLIGHTED_LEAVES_ITEM.get()), "leaves pick");
        BlockState x = log(Direction.Axis.X);
        h.assertValueEqual(
                x.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90),
                log(Direction.Axis.Z),
                "a quarter turn swaps x and z");
        h.assertTrue(pick(h, log(Direction.Axis.Y)).is(EcologyModule.INFECTED_LOG_ITEM.get()), "log pick block");
        h.succeed();
    }

    // --- the demo ---

    private static DemoExhibit exhibit(int slot) {
        return new DemoExhibit(EXHIBIT_MIN, EXHIBIT_MAX, slot);
    }

    /**
     * The exhibit sets six living trees beside what each becomes, the same log and leaves, a living and an infected
     * oak, logs in a furnace and the tools; cutting the infected trunk gives one log.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void theDemoExhibitShowsTheTimberAndSuppliesIt(GameTestHelper h) {
        DemoExhibit yard = exhibit(0);
        yard.force(h);
        ServerLevel level = h.getLevel();
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    yard.build(h);
                    List<Block> living = List.of(
                            Blocks.OAK_LOG,
                            Blocks.BIRCH_LOG,
                            Blocks.SPRUCE_LOG,
                            Blocks.JUNGLE_LOG,
                            Blocks.ACACIA_LOG,
                            Blocks.CHERRY_LOG);
                    for (int i = 0; i < living.size(); i++) {
                        BlockPos before = yard.at(new BlockPos(50 + 2 * i, -60, -11));
                        BlockPos after = yard.at(new BlockPos(50 + 2 * i, -60, -8));
                        h.assertTrue(level.getBlockState(before).is(living.get(i)), "a living " + living.get(i));
                        h.assertTrue(level.getBlockState(before.above(2)).is(BlockTags.LEAVES), "its leaves");
                        h.assertValueEqual(level.getBlockState(after), log(Direction.Axis.Y), "what it becomes");
                        h.assertValueEqual(
                                level.getBlockState(after.above(2)), leaves(1, true, false), "and its crown");
                    }
                    BlockState fuel = level.getBlockState(yard.at(new BlockPos(58, -60, -4)));
                    h.assertTrue(fuel.is(Blocks.FURNACE), "a furnace");
                    var furnace = (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)
                            level.getBlockEntity(yard.at(new BlockPos(58, -60, -4)));
                    h.assertTrue(
                            furnace != null && furnace.getItem(1).is(EcologyModule.INFECTED_LOG_ITEM.get()),
                            "Infected Logs as its fuel");
                    ChestBlockEntity supplies =
                            (ChestBlockEntity) level.getBlockEntity(yard.at(new BlockPos(59, -60, -4)));
                    h.assertTrue(
                            supplies != null
                                    && supplies.countItem(EcologyModule.INFECTED_LOG_ITEM.get()) >= 32
                                    && supplies.countItem(EcologyModule.BLIGHTED_LEAVES_ITEM.get()) >= 8
                                    && supplies.countItem(EcologyModule.INFECTED_PLANKS_ITEM.get()) == 0,
                            "logs and leaves to build with, no planks");
                    ChestBlockEntity tools =
                            (ChestBlockEntity) level.getBlockEntity(yard.at(new BlockPos(60, -60, -4)));
                    h.assertTrue(
                            tools != null
                                    && tools.countItem(Items.IRON_AXE) == 1
                                    && tools.countItem(Items.SHEARS) == 1
                                    && tools.countItem(Items.IRON_HOE) == 2,
                            "an axe, shears and two hoes");
                    // the infected oak stands as the living one does, with the same leaf distances
                    BlockPos livingOak = new BlockPos(65, -60, -8);
                    BlockPos infected = new BlockPos(71, -60, -8);
                    for (BlockPos offset : BlockPos.betweenClosed(-2, 0, -2, 2, 5, 2)) {
                        BlockState before = level.getBlockState(yard.at(livingOak.offset(offset)));
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

    private static BlockState log(Direction.Axis axis) {
        return EcologyModule.INFECTED_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState leaves(int distance, boolean persistent, boolean waterlogged) {
        return EcologyModule.BLIGHTED_LEAVES
                .get()
                .defaultBlockState()
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
