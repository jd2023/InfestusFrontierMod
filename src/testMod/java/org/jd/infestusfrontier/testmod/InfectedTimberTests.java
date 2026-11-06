package org.jd.infestusfrontier.testmod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.ecology.InfectedLogBlock;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;

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
    private static final BlockPos GROVE_MIN = new BlockPos(131, -61, -13);
    private static final BlockPos GROVE_MAX = new BlockPos(165, -48, -2);
    /** The infected grove stands this far east of its living twin. */
    private static final int GROVE_SHIFT = 14;

    private static final List<String> LOG_MODELS =
            List.of("infected_log", "infected_log_twig", "infected_log_stub", "infected_log_wound");

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

    // --- the Capsule Stem ---

    /** The recipe of the Capsule Stem: an Infected Log, a chest, two Membrane Sheets and a Spore Culture. */
    private static final ResourceLocation STEM_RECIPE =
            ResourceLocation.fromNamespaceAndPath("infestusfrontier", "capsule_stem");

    /** The five stem parts with {@code log} as the timber, in a 3x3 grid in the given order, then empty cells. */
    static List<ItemStack> stemParts(ItemStack log) {
        return List.of(
                log,
                new ItemStack(Items.CHEST),
                new ItemStack(ProcessingModule.MEMBRANE_SHEET.get()),
                new ItemStack(ProcessingModule.MEMBRANE_SHEET.get()),
                new ItemStack(EcologyModule.SPORE_CULTURE.get()));
    }

    /** The result of {@code parts} laid in the first cells of a 3x3 grid; empty when no recipe matches. */
    static ItemStack craftStem(GameTestHelper h, List<ItemStack> parts) {
        List<ItemStack> cells = new ArrayList<>(parts);
        while (cells.size() < 9) cells.add(ItemStack.EMPTY);
        return craft(h, 3, 3, cells);
    }

    /**
     * A demo Capsule Stem station: a crafting table beside a chest holding the parts, an oak log to be refused and, when
     * {@code withLog}, an Infected Log, so a player crafts a stem there; without it the player cuts the log from an
     * infected tree first. The chest's oak log, put in the log's place, makes nothing.
     */
    static void assertStemStation(GameTestHelper h, DemoExhibit yard, BlockPos table, BlockPos chest, boolean withLog) {
        ServerLevel level = h.getLevel();
        h.assertTrue(level.getBlockState(yard.at(table)).is(Blocks.CRAFTING_TABLE), "a crafting table at " + table);
        ChestBlockEntity parts = (ChestBlockEntity) level.getBlockEntity(yard.at(chest));
        h.assertTrue(parts != null, "a chest of stem parts at " + chest);
        h.assertTrue(
                (parts.countItem(EcologyModule.INFECTED_LOG_ITEM.get()) >= 1) == withLog
                        && parts.countItem(Items.CHEST) >= 1
                        && parts.countItem(ProcessingModule.MEMBRANE_SHEET.get()) >= 2
                        && parts.countItem(EcologyModule.SPORE_CULTURE.get()) >= 1
                        && parts.countItem(Items.OAK_LOG) >= 1,
                "a chest, two sheets and a culture to craft with, an oak log to be refused, a log only if offered");
        h.assertTrue(
                craftStem(h, stemParts(new ItemStack(EcologyModule.INFECTED_LOG_ITEM.get())))
                        .is(StorageModule.CAPSULE_STEM_ITEM.get()),
                "the chest's parts craft a stem");
        h.assertTrue(craftStem(h, stemParts(new ItemStack(Items.OAK_LOG))).isEmpty(), "its oak log does not");
    }

    /**
     * The Capsule Stem takes the one Infected Log of any infected tree with a chest, two Membrane Sheets and a Spore
     * Culture, in any arrangement, and leaves nothing behind.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void theCapsuleStemRecipeTakesTheInfectedLogWithItsFourPartsAndNothingRemains(GameTestHelper h) {
        var recipe = h.getLevel().getRecipeManager().byKey(STEM_RECIPE);
        h.assertTrue(recipe.isPresent(), "the recipe loads");
        h.assertValueEqual(recipe.get().value().getIngredients().size(), 5, "five ingredients");
        h.assertValueEqual(
                recipe.get()
                        .value()
                        .getResultItem(h.getLevel().registryAccess())
                        .getCount(),
                1,
                "one stem a craft");
        ItemStack log = new ItemStack(EcologyModule.INFECTED_LOG_ITEM.get());
        ItemStack stem = craftStem(h, stemParts(log.copy()));
        h.assertTrue(stem.is(StorageModule.CAPSULE_STEM_ITEM.get()) && stem.getCount() == 1, "one stem: " + stem);
        List<ItemStack> shuffled = new ArrayList<>(stemParts(log.copy()));
        java.util.Collections.reverse(shuffled);
        h.assertTrue(craftStem(h, shuffled).is(StorageModule.CAPSULE_STEM_ITEM.get()), "in the reverse order");
        List<ItemStack> scattered = new ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        List<ItemStack> parts = stemParts(log.copy());
        int[] cells = {8, 0, 5, 2, 4};
        for (int i = 0; i < parts.size(); i++) scattered.set(cells[i], parts.get(i));
        h.assertTrue(craftStem(h, scattered).is(StorageModule.CAPSULE_STEM_ITEM.get()), "scattered over the grid");
        CraftingInput input = CraftingInput.of(3, 3, scattered);
        var crafted = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
        h.assertTrue(crafted.isPresent() && crafted.get().id().equals(STEM_RECIPE), "the stem recipe matched");
        h.assertTrue(
                crafted.get().value().getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),
                "the craft leaves no container behind: it takes exactly the five items");
        h.succeed();
    }

    /** An ordinary log of any kind, a stripped log, wood, planks and the legacy Infected Plank are not the log. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void theCapsuleStemRecipeRefusesOrdinaryWoodAndOldPlanks(GameTestHelper h) {
        int tried = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack wood = new ItemStack(item);
            if (!wood.is(ItemTags.LOGS) && !wood.is(ItemTags.PLANKS)) continue;
            tried++;
            h.assertTrue(craftStem(h, stemParts(wood)).isEmpty(), "no stem from " + item);
        }
        h.assertTrue(tried > 30, "every log and plank item was tried: " + tried);
        h.assertTrue(
                craftStem(h, stemParts(new ItemStack(EcologyModule.INFECTED_PLANKS_ITEM.get())))
                        .isEmpty(),
                "no stem from an Infected Plank");
        h.assertTrue(craftStem(h, stemParts(new ItemStack(Items.OAK_LOG))).isEmpty(), "no stem from an oak log");
        h.succeed();
    }

    /** Each part is needed, none twice: the old three-part stem, a missing sheet, a second log and a second chest fail. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void theCapsuleStemRecipeNeedsEveryPartOnce(GameTestHelper h) {
        ItemStack log = new ItemStack(EcologyModule.INFECTED_LOG_ITEM.get());
        List<ItemStack> parts = stemParts(log.copy());
        h.assertTrue(
                craftStem(h, parts.subList(1, 5)).isEmpty(), "a chest, two sheets and a culture, the old recipe, fail");
        for (int missing = 0; missing < parts.size(); missing++) {
            List<ItemStack> fewer = new ArrayList<>(parts);
            fewer.remove(missing);
            h.assertTrue(craftStem(h, fewer).isEmpty(), "no stem without part " + missing);
        }
        for (int extra = 0; extra < parts.size(); extra++) {
            List<ItemStack> more = new ArrayList<>(parts);
            more.add(parts.get(extra).copy());
            h.assertTrue(craftStem(h, more).isEmpty(), "no stem with a second of part " + extra);
        }
        h.assertTrue(
                craft(h, 2, 2, new ArrayList<>(parts.subList(0, 4))).isEmpty(),
                "four items cannot make it: it needs 3x3");
        h.succeed();
    }

    // --- planks, kept for old worlds ---

    /** Nothing makes Infected Planks or Infected Logs any more, and only the Capsule Stem recipe takes an Infected Log. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void noRecipeMakesPlanksOrLogsOrTakesALog(GameTestHelper h) {
        ItemStack log = new ItemStack(EcologyModule.INFECTED_LOG_ITEM.get());
        for (RecipeHolder<?> holder : h.getLevel().getRecipeManager().getRecipes()) {
            ItemStack result = holder.value().getResultItem(h.getLevel().registryAccess());
            h.assertFalse(result.is(log.getItem()), holder.id() + " makes Infected Logs");
            h.assertFalse(result.is(EcologyModule.INFECTED_PLANKS_ITEM.get()), holder.id() + " makes Infected Planks");
            if (!holder.id().getNamespace().equals("infestusfrontier")) continue;
            h.assertValueEqual(
                    holder.value().getIngredients().stream().anyMatch(i -> i.test(log)),
                    holder.id().equals(STEM_RECIPE),
                    holder.id() + " takes an Infected Log only if it is the Capsule Stem");
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

    /**
     * The cut bark is only the model's: collision, outline and light stay the full log's, so a trunk walks, mines and
     * shades as any log does, whatever its axis.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void aLogIsAFullBlockToCollisionOutlineAndLight(GameTestHelper h) {
        BlockPos at = h.absolutePos(AT);
        for (Direction.Axis axis : Direction.Axis.values()) {
            h.getLevel().setBlockAndUpdate(at, log(axis));
            BlockState state = h.getLevel().getBlockState(at);
            h.assertTrue(same(state.getCollisionShape(h.getLevel(), at), Shapes.block()), "full collision " + axis);
            h.assertTrue(same(state.getShape(h.getLevel(), at), Shapes.block()), "full outline " + axis);
            h.assertValueEqual(state.getLightBlock(h.getLevel(), at), 15, "stops all light " + axis);
            h.assertTrue(state.canOcclude(), "hides what its section covers " + axis);
        }
        h.succeed();
    }

    /**
     * Every log model draws one section, and the block occludes exactly that section along its axis: where the bark
     * is cut, a neighbour's face is kept, so no groove opens onto a missing face.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH)
    public static void theLogOccludesTheSectionItsModelsDraw(GameTestHelper h) {
        boolean[][] section = null;
        for (String model : LOG_MODELS) {
            boolean[][] caps = caps(model);
            if (section == null) section = caps;
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    h.assertValueEqual(caps[x][z], section[x][z], model + " shares the section at " + x + "," + z);
        }
        int wood = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (section[x][z]) wood++;
                double u = (x + 0.5) / 16, v = (z + 0.5) / 16;
                h.assertValueEqual(
                        holds(InfectedLogBlock.OCCLUSION.get(Direction.Axis.Y), new Vec3(u, 0.5, v)),
                        section[x][z],
                        "upright section at " + x + "," + z);
                h.assertValueEqual(
                        holds(InfectedLogBlock.OCCLUSION.get(Direction.Axis.Z), new Vec3(u, v, 0.5)),
                        section[x][z],
                        "laid x 90 along z at " + x + "," + z);
                h.assertValueEqual(
                        holds(InfectedLogBlock.OCCLUSION.get(Direction.Axis.X), new Vec3(0.5, v, u)),
                        section[x][z],
                        "laid along x at " + x + "," + z);
            }
        }
        h.assertTrue(wood > 128 && wood < 256, "a cut section, not the full block: " + wood);
        // grass under a trunk keeps its top; a stacked log hides the end it sits on, as a vanilla trunk does
        BlockPos ground = h.absolutePos(AT);
        BlockPos above = ground.above();
        h.getLevel().setBlockAndUpdate(ground, Blocks.GRASS_BLOCK.defaultBlockState());
        h.getLevel().setBlockAndUpdate(above, log(Direction.Axis.Y));
        h.assertTrue(
                Block.shouldRenderFace(
                        Blocks.GRASS_BLOCK.defaultBlockState(), h.getLevel(), ground, Direction.UP, above),
                "the grass top under the grooves is drawn");
        h.getLevel().setBlockAndUpdate(above, Blocks.OAK_LOG.defaultBlockState());
        h.assertFalse(
                Block.shouldRenderFace(
                        Blocks.GRASS_BLOCK.defaultBlockState(), h.getLevel(), ground, Direction.UP, above),
                "under a vanilla log it is hidden");
        for (Direction.Axis axis : Direction.Axis.values()) {
            Direction along = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            BlockPos next = ground.relative(along);
            h.getLevel().setBlockAndUpdate(ground, log(axis));
            h.getLevel().setBlockAndUpdate(next, log(axis));
            h.assertFalse(
                    Block.shouldRenderFace(log(axis), h.getLevel(), ground, along, next),
                    "the end between two " + axis + " logs is hidden");
            h.getLevel().setBlockAndUpdate(next, Blocks.AIR.defaultBlockState());
        }
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
                    assertStemStation(h, yard, new BlockPos(62, -60, -4), new BlockPos(63, -60, -4), true);
                    ChestBlockEntity refused =
                            (ChestBlockEntity) level.getBlockEntity(yard.at(new BlockPos(63, -60, -4)));
                    h.assertTrue(
                            refused.countItem(EcologyModule.INFECTED_PLANKS_ITEM.get()) >= 1,
                            "an old Infected Plank to be refused");
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

    /**
     * The Grove sets a living grove (a 2x2 dark oak with lying branches, an oak with a low limb, a birch, a spruce)
     * beside the same grove in Infected Logs and Blighted Leaves, log for log and leaf for leaf, and a frame built of
     * Infected Logs in all three axes with an old plank, each with its sign.
     */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void theDemoGroveSetsLivingTreesBesideTheirInfectedTwinsAndABuiltFrame(GameTestHelper h) {
        DemoExhibit yard = new DemoExhibit(GROVE_MIN, GROVE_MAX, 21);
        yard.force(h);
        ServerLevel level = h.getLevel();
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    yard.build(h);
                    int logs = 0, leaves = 0, eyeLevel = 0;
                    for (BlockPos demo : BlockPos.betweenClosed(132, -60, -11, 143, -50, -2)) {
                        BlockState living = level.getBlockState(yard.at(demo));
                        BlockState infected = level.getBlockState(yard.at(demo.east(GROVE_SHIFT)));
                        if (living.is(BlockTags.LOGS)) {
                            h.assertValueEqual(
                                    infected, log(living.getValue(RotatedPillarBlock.AXIS)), "a log's twin at " + demo);
                            logs++;
                        } else if (living.is(BlockTags.LEAVES)) {
                            h.assertTrue(infected.is(EcologyModule.BLIGHTED_LEAVES.get()), "a leaf's twin at " + demo);
                            leaves++;
                            if (demo.getY() <= -58 && demo.getZ() == -11) eyeLevel++;
                        } else {
                            h.assertTrue(infected.isAir(), "open where the living grove is open at " + demo);
                        }
                    }
                    h.assertTrue(logs >= 40 && leaves >= 100, logs + " logs and " + leaves + " leaves");
                    h.assertTrue(eyeLevel >= 3, "foliage at eye level by the path: " + eyeLevel);
                    for (int dx = 0; dx < 2; dx++)
                        for (int dz = 0; dz < 2; dz++)
                            for (int y = -60; y <= -54; y++)
                                h.assertValueEqual(
                                        level.getBlockState(yard.at(new BlockPos(150 + dx, y, -8 + dz))),
                                        log(Direction.Axis.Y),
                                        "a 2x2 infected trunk");
                    for (Direction.Axis lying : List.of(Direction.Axis.X, Direction.Axis.Z)) {
                        long branches = BlockPos.betweenClosedStream(146, -60, -11, 157, -50, -2)
                                .filter(p -> level.getBlockState(yard.at(p)).equals(log(lying)))
                                .count();
                        h.assertTrue(branches >= 2, "branches lying along " + lying + ": " + branches);
                    }
                    for (Direction.Axis axis : Direction.Axis.values()) {
                        long built = BlockPos.betweenClosedStream(159, -60, -10, 163, -57, -6)
                                .filter(p -> level.getBlockState(yard.at(p)).equals(log(axis)))
                                .count();
                        h.assertTrue(built >= 4, "the frame uses logs along " + axis + ": " + built);
                    }
                    h.assertTrue(
                            level.getBlockState(yard.at(new BlockPos(161, -60, -8)))
                                    .is(EcologyModule.INFECTED_PLANKS.get()),
                            "an old plank in the frame");
                    for (int x : List.of(133, 147, 151, 160, 162))
                        h.assertTrue(
                                level.getBlockState(yard.at(new BlockPos(x, -60, -12)))
                                        .is(BlockTags.SIGNS),
                                "a sign at " + x);
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }

    /** The storage row has a crafting table and a chest of stem parts beside the demo's Capsule Stem. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void theDemoStorageRowHasAStemStation(GameTestHelper h) {
        DemoExhibit yard = new DemoExhibit(new BlockPos(75, -61, -38), new BlockPos(82, -50, -33), 20);
        yard.force(h);
        h.startSequence()
                .thenIdle(DemoExhibit.ENTITIES_LOAD_TICKS)
                .thenExecute(() -> {
                    yard.build(h);
                    h.assertTrue(
                            h.getLevel()
                                    .getBlockState(yard.at(new BlockPos(76, -60, -36)))
                                    .is(StorageModule.CAPSULE_STEM.get()),
                            "the Capsule Stem the station feeds");
                    assertStemStation(h, yard, new BlockPos(80, -60, -33), new BlockPos(81, -60, -33), true);
                    yard.tidy(h);
                    yard.forget(h);
                })
                .thenSucceed();
    }

    // --- fixtures ---

    /** Where a model's end caps cover the upright section, read from the shipped model file. */
    private static boolean[][] caps(String model) {
        Path file = ModList.get()
                .getModFileById("infestusfrontier")
                .getFile()
                .findResource("assets", "infestusfrontier", "models", "block", model + ".json");
        boolean[][] covered = new boolean[16][16];
        try (var in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
            for (JsonElement element : json.getAsJsonArray("elements")) {
                JsonObject box = element.getAsJsonObject();
                JsonObject up = box.getAsJsonObject("faces").getAsJsonObject("up");
                if (up == null
                        || !"up".equals(up.has("cullface") ? up.get("cullface").getAsString() : null)) continue;
                var from = box.getAsJsonArray("from");
                var to = box.getAsJsonArray("to");
                for (int x = from.get(0).getAsInt(); x < to.get(0).getAsInt(); x++)
                    for (int z = from.get(2).getAsInt(); z < to.get(2).getAsInt(); z++) covered[x][z] = true;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return covered;
    }

    private static boolean holds(VoxelShape shape, Vec3 point) {
        return shape.toAabbs().stream().anyMatch(box -> box.contains(point));
    }

    private static boolean same(VoxelShape a, VoxelShape b) {
        return !Shapes.joinIsNotEmpty(a, b, BooleanOp.NOT_SAME);
    }

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
