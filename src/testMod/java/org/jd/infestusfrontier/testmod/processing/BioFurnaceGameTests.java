package org.jd.infestusfrontier.testmod.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.processing.api.BatchWork;
import org.jd.infestusfrontier.testmod.kit.OffhandUse;
import org.jd.infestusfrontier.testmod.kit.SaveReload;

@net.neoforged.fml.common.EventBusSubscriber(modid = "infestusfrontier_tests", bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class BioFurnaceGameTests {
    private static final ResourceLocation FURNACE = id("processing/bio_furnace");
    private static final ResourceLocation BIOMASS_BUCKET = id("storage/biomass_bucket");
    private static final ResourceLocation ORGAN_BUD = id("construction/organ_bud");
    private static final ResourceLocation MEMBRANE_SHEET = id("processing/membrane_sheet");

    private static final ResourceLocation NEIGHBOR_PROBE = ResourceLocation.fromNamespaceAndPath(
            "infestusfrontier_tests", "bio_furnace_neighbor_probe");
    private static int neighborProbes;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_scheduling")
    public static void furnaceWorkTestsDoNotShareOtherOrgansCompletionBudget(GameTestHelper helper) {
        // Manual work ticks do not advance the real server tick that owns the
        // completion quota. Only crafting tests are safe in the shared batch.
        for (var method : BioFurnaceGameTests.class.getDeclaredMethods()) {
            var test = method.getAnnotation(GameTest.class);
            if (test == null || method.getName().equals("exactRecipeCraftsOneBioFurnace")
                    || method.getName().equals("recipeWithOneSheetCraftsNothing")) continue;
            helper.assertTrue(test.batch().startsWith("bio_furnace_"),
                    "Manual furnace fixtures must be isolated from other organs' completion budget: " + method.getName());
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void exactRecipeCraftsOneBioFurnace(GameTestHelper helper) {
        var output = craft(helper, FURNACE, java.util.List.of(new ItemStack(Items.FURNACE),
                new ItemStack(item(ORGAN_BUD)), new ItemStack(item(MEMBRANE_SHEET), 2)));
        helper.assertTrue(output.is(item(FURNACE)) && output.getCount() == 1,
                "Bio-Furnace recipe produces exactly one Bio-Furnace");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void recipeWithOneSheetCraftsNothing(GameTestHelper helper) {
        var recipe = craftingRecipe(helper, FURNACE);
        var input = craftingInput(java.util.List.of(new ItemStack(Items.FURNACE), new ItemStack(item(ORGAN_BUD)),
                new ItemStack(item(MEMBRANE_SHEET))));
        helper.assertTrue(!recipe.matches(input, helper.getLevel()),
                "Bio-Furnace recipe rejects a crafting grid with only one Membrane Sheet");
        helper.succeed();
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void registerNeighborProbe(net.neoforged.neoforge.registries.RegisterEvent event) {
        event.register(net.minecraft.core.registries.Registries.BLOCK, NEIGHBOR_PROBE,
                () -> new Block(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()) {
                    @Override public void onNeighborChange(net.minecraft.world.level.block.state.BlockState state,
                            net.minecraft.world.level.LevelReader level, BlockPos pos, BlockPos neighbor) {
                        neighborProbes++;
                    }
                });
    }

    // These fixtures perform at most 13 completions in one real server tick,
    // below the shared limit of 16, without competing with other organs.
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void updatesNeitherProbeNeighborsNorLoadChunks(GameTestHelper helper) {
        var edge = org.jd.infestusfrontier.testmod.kit.ChunkEdge.prepare(helper);
        var pos = edge.edge().west();
        var level = helper.getLevel();
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(pos, BuiltInRegistries.BLOCK.get(FURNACE).defaultBlockState(), flags);
        level.setBlock(edge.edge(), BuiltInRegistries.BLOCK.get(NEIGHBOR_PROBE).defaultBlockState(), flags);
        org.jd.infestusfrontier.testmod.kit.ChunkEdge.assertNothingLoaded(helper, edge);
        neighborProbes = 0;
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        use(helper, pos, player, ItemStack.EMPTY);
        tick(helper, pos, 320);
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.getMainHandItem().is(Items.IRON_INGOT), "Edge furnace completes and collects its batch");
        helper.assertTrue(neighborProbes == 0, "Transfers and active ticks must never probe neighbors; observed " + neighborProbes);
        org.jd.infestusfrontier.testmod.kit.ChunkEdge.assertNothingLoaded(helper, edge);
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void smeltsRawIronInSixteenSecondsForFortyBiomass(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "A complete biomass bucket is returned");
        use(helper, pos, player, ItemStack.EMPTY);
        tick(helper, pos, 320);
        helper.assertTrue(work(helper, pos).state().quantities().itemCount("minecraft:iron_ingot") == 1
                        && work(helper, pos).state().quantities().fluidAmount("biomass") == 960,
                "One iron ingot completes after 320 work units and consumes exactly 40 BU");
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.getMainHandItem().is(Items.IRON_INGOT), "One raw iron smelts to one iron ingot");
        player.setShiftKeyDown(false);
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "960 BU leaves room for one measured bucket");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void refusesNonSmeltableAndPassesItThrough(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        var diamond = new ItemStack(Items.DIAMOND);
        var result = useResult(helper, pos, player, diamond);
        helper.assertTrue(result == net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                        && player.getMainHandItem().is(Items.DIAMOND),
                "A non-smeltable item passes through unchanged");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void refusalLeavesInputAndBiomassUnchanged(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        var work = fillOutputSlot(helper, pos);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        var beforeStart = work.state();
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(beforeStart.equals(work.state()), "An output-full start retains its loaded input and biomass");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void bucketReturnsOnlyWhenAllThousandFits(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "First full bucket is accepted");
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "Second full bucket exactly fills the tank");
        var full = work(helper, pos).state();
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        helper.assertTrue(player.getMainHandItem().is(item(BIOMASS_BUCKET)) && full.equals(work(helper, pos).state()),
                "A bucket is retained when all 1000 BU cannot fit");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void offhandPlacementPassesThrough(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        place(helper, relative);
        InteractionResult result = OffhandUse.useWithOffhand(helper, relative, new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(result.consumesAction(), "Bio-Furnace leaves empty-main-hand placement to the offhand");
        OffhandUse.assertPlacedBeside(helper, relative, Blocks.COBBLESTONE);
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void idleFurnaceDoesNoWork(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        var beforeTicks = work(helper, pos).state();
        tick(helper, pos, 100);
        helper.assertTrue(beforeTicks.equals(work(helper, pos).state()), "An idle furnace state does not change over 100 ticks");
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.getMainHandItem().is(Items.RAW_IRON), "An idle furnace never advances an unstarted batch");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void mainHandPlacementPassesThrough(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        player.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 2));
        var before = work(helper, pos).state();
        var result = player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(),
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        helper.assertTrue(result.consumesAction() && helper.getLevel().getBlockState(pos.above()).is(Blocks.DIRT)
                        && player.getMainHandItem().getCount() == 1,
                "Non-smeltable main-hand block must reach ordinary placement");
        helper.assertTrue(before.equals(work(helper, pos).state()), "Ordinary placement leaves furnace unchanged");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void fullInputNeverSpillsIntoOutput(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        for (int count = 0; count < 64; count++) use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        var full = work(helper, pos).state();
        var excess = new ItemStack(Items.RAW_IRON);
        use(helper, pos, player, excess);
        helper.assertTrue(excess.getCount() == 1 && full.equals(work(helper, pos).state())
                        && full.quantities().itemSlots().get(1).isEmpty(),
                "The 65th raw iron must remain held and leave the output slot empty");
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        use(helper, pos, player, ItemStack.EMPTY);
        tick(helper, pos, 320);
        var slots = work(helper, pos).state().quantities().itemSlots();
        helper.assertTrue(slots.get(0).count() == 63 && slots.get(1).resource().equals("minecraft:iron_ingot")
                        && slots.get(1).count() == 1, "A full input stack can still smelt into its separate output slot");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void matchingOutputCannotReceiveHandFedInput(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.COBBLESTONE));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        use(helper, pos, player, ItemStack.EMPTY);
        tick(helper, pos, 320);
        var before = work(helper, pos).state();
        helper.assertTrue(before.quantities().itemSlots().get(0).isEmpty()
                        && before.quantities().itemSlots().get(1).resource().equals("minecraft:stone"),
                "Fixture has finished stone in output and an empty input slot");
        var stone = new ItemStack(Items.STONE);
        use(helper, pos, player, stone);
        helper.assertTrue(stone.getCount() == 1 && before.equals(work(helper, pos).state()),
                "A transfer that would target matching output must refuse without consuming input");
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.getMainHandItem().is(Items.STONE) && player.getMainHandItem().getCount() == 1,
                "Finished output remains separately collectible");
        player.setShiftKeyDown(false);
        use(helper, pos, player, stone);
        helper.assertTrue(stone.isEmpty() && work(helper, pos).state().quantities().itemSlots().get(0)
                .resource().equals("minecraft:stone"), "Collecting output allows the next hand-fed input");
        use(helper, pos, player, ItemStack.EMPTY);
        tick(helper, pos, 320);
        helper.assertTrue(work(helper, pos).state().quantities().itemCount("minecraft:smooth_stone") == 1,
                "Refused input remains usable for its next recipe");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void componentBearingInputIsRefusedUnchanged(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        var named = new ItemStack(Items.RAW_IRON, 2);
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("Keep my name"));
        var damaged = new ItemStack(Items.IRON_PICKAXE);
        damaged.setDamageValue(17);
        for (var stack : new ItemStack[] {named, damaged}) {
            var original = stack.copy();
            var before = work(helper, pos).state();
            var result = useResult(helper, pos, player, stack);
            helper.assertTrue(result == net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                            && ItemStack.matches(original, player.getMainHandItem())
                            && before.equals(work(helper, pos).state()),
                    "Component-bearing smeltable input must retain count, components and furnace state");
            player.setShiftKeyDown(true);
            use(helper, pos, player, ItemStack.EMPTY);
            helper.assertTrue(player.getMainHandItem().isEmpty(), "Refused input creates no stripped duplicate");
            player.setShiftKeyDown(false);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void activeBatchSurvivesReloadAndFinishes(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        var pos = place(helper, relative);
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        use(helper, pos, player, ItemStack.EMPTY);
        tick(helper, pos, 100);
        SaveReload.reload(helper, relative);
        tick(helper, pos, 220);
        helper.assertTrue(work(helper, pos).state().quantities().itemCount("minecraft:iron_ingot") == 1,
                "A batch reloaded at tick 100 must finish at tick 320 total");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void collectingAwardsStoredExperienceOnce(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.RAW_GOLD));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        use(helper, pos, player, ItemStack.EMPTY);
        tick(helper, pos, 320);
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.totalExperience == 1, "Collecting one gold ingot awards its one stored experience point");
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.totalExperience == 1, "Collecting an empty furnace cannot award stored experience twice");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void fractionalExperienceCarriesOver(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        startIronBatch(helper, pos, player);
        tick(helper, pos, 320);
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.totalExperience == 0, "One iron ingot's 0.7 experience remains stored as a fraction");
        startIronBatch(helper, pos, player);
        tick(helper, pos, 320);
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.totalExperience == 1 && experience(helper, pos) == 40,
                "Two iron ingots award one point and retain 40 hundredths");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void experienceIsCappedAndNeverSpawnsOrbs(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        var pos = place(helper, relative);
        var player = player(helper);
        var saved = SaveReload.save(helper, relative);
        saved.getCompound("furnace").putInt("experience", 99_990);
        helper.getLevel().getBlockEntity(pos).loadWithComponents(saved, helper.getLevel().registryAccess());
        startIronBatch(helper, pos, player);
        tick(helper, pos, 320);
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.totalExperience == 1_000 && experience(helper, pos) == 0,
                "Experience credit caps at 1000 points and is fully awarded on collection");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ExperienceOrb.class,
                new AABB(pos).inflate(16)).isEmpty(), "Bio-Furnace collection never spawns experience orbs");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void experienceSurvivesReload(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        var pos = place(helper, relative);
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.RAW_GOLD));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        use(helper, pos, player, ItemStack.EMPTY);
        tick(helper, pos, 320);
        SaveReload.reload(helper, relative);
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.totalExperience == 1, "Stored Bio-Furnace experience survives reload before collection");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_experience_save")
    public static void legacySaveWithoutExperienceRemainsUsable(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        var pos = place(helper, relative);
        var player = player(helper);
        startIronBatch(helper, pos, player);
        tick(helper, pos, 100);
        var before = work(helper, pos).state();
        var legacy = SaveReload.save(helper, relative);
        legacy.getCompound("furnace").remove("experience");
        var level = helper.getLevel();
        level.removeBlockEntity(pos);
        var reloaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), legacy, level.registryAccess());
        helper.assertTrue(reloaded != null, "Legacy save reloads a Bio-Furnace");
        level.setBlockEntity(reloaded);
        helper.assertTrue(before.equals(work(helper, pos).state()),
                "Legacy save without experience restores its active batch and contents");
        helper.assertTrue(experience(helper, pos) == 0, "Legacy experience starts at zero");
        tick(helper, pos, 220);
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.getMainHandItem().is(Items.IRON_INGOT)
                        && player.getMainHandItem().getCount() == 1 && player.totalExperience == 0
                        && experience(helper, pos) == 70,
                "Legacy batch completes and its output can be collected with new fractional credit");
        SaveReload.reload(helper, relative);
        helper.assertTrue(experience(helper, pos) == 70, "Migrated experience survives another reload");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_experience_save")
    public static void negativeExperienceIsRetainedAndLocksControls(GameTestHelper helper) {
        assertInvalidExperienceIsRetainedAndLocked(helper, -1);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_experience_save")
    public static void overCapExperienceIsRetainedAndLocksControls(GameTestHelper helper) {
        assertInvalidExperienceIsRetainedAndLocked(helper, 100_001);
    }

    private static void assertInvalidExperienceIsRetainedAndLocked(GameTestHelper helper, int invalid) {
        var relative = new BlockPos(1, 1, 1);
        var pos = place(helper, relative);
        var player = player(helper);
        startIronBatch(helper, pos, player);
        tick(helper, pos, 320);
        startIronBatch(helper, pos, player);
        tick(helper, pos, 100);
        helper.assertTrue(work(helper, pos).isActive()
                        && work(helper, pos).state().quantities().itemSlots().get(1).count() == 1,
                "Invalid XP fixture has collectible output and active work");
        var hostile = SaveReload.save(helper, relative);
        hostile.getCompound("furnace").putInt("experience", invalid);
        var bytes = nbtBytes(hostile);
        SaveReload.assertRetainsRejectedData(helper, relative, hostile);
        assertLockedControls(helper, relative);
        SaveReload.reload(helper, relative);
        assertLockedControls(helper, relative);
        player.setShiftKeyDown(true);
        use(helper, pos, player, ItemStack.EMPTY);
        helper.assertTrue(player.totalExperience == 0 && player.getMainHandItem().isEmpty(),
                "Rejected experience never awards XP or output");
        helper.assertTrue(java.util.Arrays.equals(bytes, nbtBytes(SaveReload.save(helper, relative))),
                "Invalid experience bytes survive controls, ticks and reload unchanged");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void rejectedSaveIsRetainedAndLocksControls(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        var pos = place(helper, relative);
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        use(helper, pos, player, ItemStack.EMPTY);
        for (CompoundTag hostile : new CompoundTag[] {unsupportedSchema(helper, relative), oversizedItems(helper, relative), wrongBatchOutput(helper, relative)}) {
            SaveReload.assertRetainsRejectedData(helper, relative, hostile);
            assertLockedControls(helper, relative);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void unearnedFluidOutputIsRetainedAndLocked(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        var pos = place(helper, relative);
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        use(helper, pos, player, ItemStack.EMPTY);
        var hostile = SaveReload.save(helper, relative);
        var reservation = hostile.getCompound("furnace").getList("reservations", Tag.TAG_COMPOUND).getCompound(0);
        var output = new CompoundTag();
        output.putInt("index", 0);
        output.putString("resource", "biomass");
        output.putInt("count", 1_000);
        reservation.getList("fluidOutputs", Tag.TAG_COMPOUND).add(output);
        SaveReload.assertRetainsRejectedData(helper, relative, hostile);
        var before = work(helper, pos).state();
        tick(helper, pos, 320);
        helper.assertTrue(before.equals(work(helper, pos).state()),
                "Unearned fluid output must lock the batch instead of producing biomass and an ingot");
        helper.assertTrue(hostile.equals(SaveReload.save(helper, relative)), "Forged reservation is retained unchanged");
        assertLockedControls(helper, relative);
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void pendingAndRejectedDataIsNeverCloned(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        var pos = place(helper, relative);
        var oversized = oversizedItems(helper, relative).getCompound("furnace");
        var probe = new CopyProbeTag();
        for (var key : oversized.getAllKeys()) probe.put(key, oversized.get(key));
        var wrongType = new net.minecraft.nbt.ListTag();
        wrongType.add(probe);
        for (Tag raw : new Tag[] {probe, wrongType}) {
            var saved = SaveReload.save(helper, relative);
            saved.put("furnace", raw);
            var originalBytes = nbtBytes(saved);
            var level = helper.getLevel();
            var entity = level.getBlockEntity(pos);
            // Exercise the adapter's level-less pending path directly. Vanilla's
            // defensive load copies are deliberately outside this zero-copy check.
            probe.copies = 0;
            entity.setLevel(null);
            loadAdditional(entity, saved, level.registryAccess());
            helper.assertTrue(probe.copies == 0, "Pending/rejected load must not clone unbounded raw data");
            var pendingSave = entity.saveWithFullMetadata(level.registryAccess());
            helper.assertTrue(probe.copies == 0 && java.util.Arrays.equals(originalBytes, nbtBytes(pendingSave)),
                    "Level-less save preserves original bytes without cloning");
            entity.setLevel(level);
            var before = work(helper, pos).state();
            tick(helper, pos, 320);
            var rejectedSave = SaveReload.save(helper, relative);
            helper.assertTrue(probe.copies == 0 && java.util.Arrays.equals(originalBytes, nbtBytes(rejectedSave))
                            && before.equals(work(helper, pos).state()),
                    "Attaching and saving rejected data must retain bytes and never clone or advance it");
            SaveReload.assertRetainsRejectedData(helper, relative, saved);
            helper.assertTrue(probe.copies == 0, "Level-attached rejection and save must not clone raw data");
            var drops = Block.getDrops(level.getBlockState(pos), level, pos, level.getBlockEntity(pos));
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item(FURNACE))
                            && drops.getFirst().getCount() == 1,
                    "Rejected furnace recovery drops exactly one core and no separate contents");
            var core = drops.getFirst();
            helper.assertTrue(core.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA),
                    "Rejected recovery core retains block-entity data");
            replace(helper, pos, player(helper), core);
            helper.assertTrue(core.isEmpty(), "Survival placement consumes the single recovery core");
            helper.assertTrue(java.util.Arrays.equals(originalBytes, nbtBytes(SaveReload.save(helper, relative))),
                    "Actual drop and placement preserve the original rejected bytes");
            assertLockedControls(helper, relative);
        }
        helper.succeed();
    }

    private static void loadAdditional(BlockEntity entity, CompoundTag tag,
            net.minecraft.core.HolderLookup.Provider registries) {
        try {
            var method = entity.getClass().getDeclaredMethod("loadAdditional", CompoundTag.class,
                    net.minecraft.core.HolderLookup.Provider.class);
            method.setAccessible(true);
            method.invoke(entity, tag, registries);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Bio-Furnace raw persistence hook is unavailable", exception);
        }
    }

    private static byte[] nbtBytes(CompoundTag tag) {
        try {
            var bytes = new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.NbtIo.write(tag, new java.io.DataOutputStream(bytes));
            return bytes.toByteArray();
        } catch (java.io.IOException exception) {
            throw new AssertionError("Cannot serialize test NBT", exception);
        }
    }

    private static final class CopyProbeTag extends CompoundTag {
        private int copies;
        @Override public CompoundTag copy() {
            copies++;
            return super.copy();
        }
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void rejectedControlsPreserveReadyAndCompletedState(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        for (int fixture = 0; fixture < 3; fixture++) {
            helper.getLevel().removeBlock(helper.absolutePos(relative), false);
            var pos = place(helper, relative);
            var player = player(helper);
            if (fixture > 0) {
                use(helper, pos, player, new ItemStack(Items.RAW_IRON));
                use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
            }
            if (fixture == 2) {
                use(helper, pos, player, ItemStack.EMPTY);
                tick(helper, pos, 320);
            }
            var valid = SaveReload.save(helper, relative);
            for (var hostile : new CompoundTag[] {unsupportedSchema(helper, relative), oversizedItems(helper, relative)}) {
                helper.getLevel().getBlockEntity(pos).loadWithComponents(valid, helper.getLevel().registryAccess());
                SaveReload.assertRetainsRejectedData(helper, relative, hostile);
                assertLockedControls(helper, relative);
            }
        }
        helper.succeed();
    }

    private static void assertLockedControls(GameTestHelper helper, BlockPos relative) {
        var pos = helper.absolutePos(relative);
        var messages = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "RejectedFurnaceTest")) {
            @Override public void displayClientMessage(net.minecraft.network.chat.Component message, boolean overlay) {
                messages.add(message);
            }
        };
        player.setGameMode(GameType.SURVIVAL);
        var beforeWork = work(helper, pos).state();
        var beforeBytes = nbtBytes(SaveReload.save(helper, relative));
        var refusals = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        // Insertion, bucket transfer, start, collection. Each fixture keeps the
        // underlying valid state, so ordinary empty/active refusals cannot mask edits.
        var stacks = new ItemStack[] {new ItemStack(Items.RAW_IRON), new ItemStack(item(BIOMASS_BUCKET)),
                ItemStack.EMPTY, ItemStack.EMPTY};
        for (int control = 0; control < stacks.length; control++) {
            messages.clear();
            player.setShiftKeyDown(control == 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, stacks[control]);
            var held = stacks[control].copy();
            var inventory = player.getInventory().save(new net.minecraft.nbt.ListTag());
            use(helper, pos, player, stacks[control]);
            helper.assertTrue(ItemStack.matches(held, player.getMainHandItem())
                            && player.totalExperience == 0
                            && inventory.equals(player.getInventory().save(new net.minecraft.nbt.ListTag()))
                            && beforeWork.equals(work(helper, pos).state())
                            && java.util.Arrays.equals(beforeBytes, nbtBytes(SaveReload.save(helper, relative))),
                    "Rejected control " + control + " must retain held items, inventory, work and original NBT bytes");
            helper.assertTrue(messages.size() == 1, "Each rejected control must send exactly one refusal");
            refusals.add(messages.getFirst());
        }
        tick(helper, pos, 320);
        helper.assertTrue(beforeWork.equals(work(helper, pos).state()), "Rejected work never advances");
        helper.assertTrue(refusals.stream().allMatch(refusals.getFirst()::equals),
                "All rejected controls must use the same refusal");
        var contents = refusals.getFirst().getContents();
        helper.assertTrue(contents instanceof net.minecraft.network.chat.contents.TranslatableContents,
                "The refusal must be localized");
        var key = ((net.minecraft.network.chat.contents.TranslatableContents) contents).getKey();
        try (var stream = helper.getLevel().getBlockEntity(pos).getClass()
                .getResourceAsStream("/assets/infestusfrontier/lang/en_us.json")) {
            helper.assertTrue(stream != null, "Packaged English translations must be available");
            var translations = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(
                    stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            helper.assertTrue(translations.has(key)
                            && translations.get(key).getAsString().equals(
                                    "This organ retained invalid data and is locked for safe recovery."),
                    "Rejected controls must send a readable recovery refusal, not a raw translation key: " + key);
        } catch (java.io.IOException exception) {
            throw new AssertionError("Cannot read packaged translations", exception);
        }
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void breakAndReplaceCarriesOneCoreWithContents(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                helper.getLevel().getBlockEntity(pos));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(item(FURNACE))
                        && drops.getFirst().has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA),
                "Breaking a furnace drops exactly one core carrying its block-entity data");
        replace(helper, pos, player, drops.getFirst());
        helper.assertTrue(work(helper, pos).state().quantities().itemCount("minecraft:raw_iron") == 1
                        && work(helper, pos).state().quantities().fluidAmount("biomass") == 1_000,
                "Replacing the single core restores both stored input and biomass");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_work")
    public static void historySurvivesItemPlacement(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        use(helper, pos, player, ItemStack.EMPTY);
        tick(helper, pos, 320);
        var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                helper.getLevel().getBlockEntity(pos));
        replace(helper, pos, player, drops.getFirst());
        helper.assertTrue(work(helper, pos).state().history().completedBatches() == 1,
                "Completed-batch history survives actual block-item placement");
        helper.succeed();
    }

    // Manual work ticks share one real server tick. Isolate these six completions
    // from other recipe tests so the server-wide admission budget remains meaningful.
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_growth")
    public static void thirtySecondBatchOffersExactlyOneChoice(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        restoreHistory(helper, pos, 31);
        var player = player(helper);
        startIronBatch(helper, pos, player);
        tick(helper, pos, 320);
        var work = work(helper, pos);
        helper.assertTrue(work.state().history().completedBatches() == 32,
                "The thirty-second completed batch earns the first L1 choice");
        helper.assertTrue(work.choose(org.jd.infestusfrontier.organ.api.OrganHistory.GrowthChoice.INCUBATION,
                        work.revision()) == BatchWork.GrowthResult.CHOSEN,
                "The first earned choice must succeed");
        helper.assertTrue(work.choose(org.jd.infestusfrontier.organ.api.OrganHistory.GrowthChoice.WATER_ECONOMY,
                        work.revision()) == BatchWork.GrowthResult.LEVEL_CHOICE_ALREADY_USED,
                "L1 must allow exactly one choice");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_growth")
    public static void choiceCannotBeEarnedTwiceByReopeningBreakingOrReloading(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        var pos = place(helper, relative);
        restoreHistory(helper, pos, 31);
        var player = player(helper);
        startIronBatch(helper, pos, player);
        tick(helper, pos, 320);
        player.setShiftKeyDown(true);
        use(helper, pos, player, new ItemStack(Items.CLOCK));
        helper.assertTrue(work(helper, pos).state().history().choices().size() == 1,
                "The earned clock choice is retained before recovery");
        SaveReload.reload(helper, relative);
        var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                helper.getLevel().getBlockEntity(pos));
        replace(helper, pos, player, drops.getFirst());
        var work = work(helper, pos);
        helper.assertTrue(work.choose(org.jd.infestusfrontier.organ.api.OrganHistory.GrowthChoice.WATER_ECONOMY,
                        work.revision()) == BatchWork.GrowthResult.LEVEL_CHOICE_ALREADY_USED,
                "Reloading and replacing one core must not earn another L1 choice");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_growth")
    public static void speedChoiceShortensNextBatchToTwoHundredEightyEightTicks(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        restoreHistory(helper, pos, 31);
        var player = player(helper);
        startIronBatch(helper, pos, player);
        tick(helper, pos, 320);
        player.setShiftKeyDown(true);
        use(helper, pos, player, new ItemStack(Items.CLOCK));
        helper.assertTrue(player.getMainHandItem().is(Items.CLOCK), "A choice clock is not consumed");
        player.setShiftKeyDown(false);
        startIronBatch(helper, pos, player);
        helper.assertTrue(work(helper, pos).state().activeBatch().requiredWorkUnits() == 288,
                "One incubation choice reduces the next batch to 288 ticks");
        tick(helper, pos, 287);
        helper.assertTrue(work(helper, pos).isActive()
                        && work(helper, pos).state().history().completedBatches() == 32,
                "The speed-adjusted batch cannot complete before tick 288");
        tick(helper, pos, 1);
        helper.assertTrue(!work(helper, pos).isActive()
                        && work(helper, pos).state().history().completedBatches() == 33,
                "The speed-adjusted batch completes exactly once at tick 288");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_growth")
    public static void economyChoiceCostsThirtySixBiomass(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        restoreHistory(helper, pos, 31);
        var player = player(helper);
        startIronBatch(helper, pos, player);
        tick(helper, pos, 320);
        player.setShiftKeyDown(true);
        use(helper, pos, player, new ItemStack(Items.GLASS_BOTTLE));
        helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "A choice bottle is not consumed");
        player.setShiftKeyDown(false);
        startIronBatch(helper, pos, player);
        tick(helper, pos, 320);
        helper.assertTrue(work(helper, pos).state().quantities().fluidAmount("biomass") == 1_924,
                "One water-economy choice makes the second batch consume exactly 36 biomass");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_growth")
    public static void cancelledBatchEarnsNoHistory(GameTestHelper helper) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        var player = player(helper);
        startIronBatch(helper, pos, player);
        tick(helper, pos, 100);
        var work = work(helper, pos);
        helper.assertTrue(work.cancel(work.revision()) == BatchWork.CancelResult.CANCELLED,
                "Cancel the active batch through its work owner");
        helper.assertTrue(!work(helper, pos).isActive(), "The batch must actually be cancelled");
        tick(helper, pos, 320);
        helper.assertTrue(work(helper, pos).state().history().completedBatches() == 0,
                "Cancelling an in-progress batch cannot earn history");
        helper.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_growth")
    public static void sneakClockChoosesGrowthThroughPlayerInteraction(GameTestHelper helper) {
        assertGrowthThroughPlayerInteraction(helper, Items.CLOCK,
                org.jd.infestusfrontier.organ.api.OrganHistory.GrowthChoice.INCUBATION);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", batch = "bio_furnace_growth")
    public static void sneakBottleChoosesGrowthThroughPlayerInteraction(GameTestHelper helper) {
        assertGrowthThroughPlayerInteraction(helper, Items.GLASS_BOTTLE,
                org.jd.infestusfrontier.organ.api.OrganHistory.GrowthChoice.WATER_ECONOMY);
    }

    private static void assertGrowthThroughPlayerInteraction(GameTestHelper helper, net.minecraft.world.item.Item selector,
            org.jd.infestusfrontier.organ.api.OrganHistory.GrowthChoice choice) {
        var pos = place(helper, new BlockPos(1, 1, 1));
        restoreHistory(helper, pos, 32);
        var messages = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "FurnaceGrowthTest")) {
            @Override public void displayClientMessage(net.minecraft.network.chat.Component message, boolean overlay) {
                messages.add(message);
            }
        };
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
        player.setShiftKeyDown(true);
        var held = new ItemStack(selector, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, held.copy());
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (String result : new String[] {"chosen", "level_choice_already_used"}) {
            messages.clear();
            var interaction = player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(),
                    InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(work(helper, pos).state().history().choices().equals(java.util.List.of(choice)),
                    "Sneak-use through the player dispatcher must apply exactly one " + choice + " choice");
            helper.assertTrue(interaction.consumesAction(), "The furnace must handle the growth interaction");
            helper.assertTrue(ItemStack.matches(held, player.getMainHandItem()),
                    "Growth selection and repeated selection must preserve the entire held stack");
            helper.assertTrue(messages.size() == 1 && messages.getFirst().equals(net.minecraft.network.chat.Component
                            .translatable("message.infestusfrontier.processing.bio_furnace.growth." + result)),
                    "The player must receive the translated growth result: " + result);
        }
        helper.succeed();
    }

    private static void restoreHistory(GameTestHelper helper, BlockPos pos, long completedBatches) {
        var saved = helper.getLevel().getBlockEntity(pos).saveWithFullMetadata(helper.getLevel().registryAccess());
        var furnace = saved.getCompound("furnace");
        furnace.putLong("completed", completedBatches);
        furnace.putLong("lastCompleted", completedBatches);
        furnace.putLong("nextBatch", completedBatches + 1);
        saved.put("furnace", furnace);
        helper.getLevel().getBlockEntity(pos).loadWithComponents(saved, helper.getLevel().registryAccess());
    }

    private static void startIronBatch(GameTestHelper helper, BlockPos pos, net.minecraft.server.level.ServerPlayer player) {
        player.setShiftKeyDown(false);
        use(helper, pos, player, new ItemStack(Items.RAW_IRON));
        use(helper, pos, player, new ItemStack(item(BIOMASS_BUCKET)));
        use(helper, pos, player, ItemStack.EMPTY);
    }

    private static CompoundTag unsupportedSchema(GameTestHelper helper, BlockPos relative) {
        var hostile = SaveReload.save(helper, relative).copy();
        var furnace = hostile.getCompound("furnace");
        furnace.putInt("schema", 99);
        hostile.put("furnace", furnace);
        return hostile;
    }

    private static CompoundTag oversizedItems(GameTestHelper helper, BlockPos relative) {
        var hostile = SaveReload.save(helper, relative).copy();
        var furnace = hostile.getCompound("furnace");
        var items = furnace.getList("items", Tag.TAG_COMPOUND);
        items.add(new CompoundTag());
        hostile.put("furnace", furnace);
        return hostile;
    }

    private static CompoundTag wrongBatchOutput(GameTestHelper helper, BlockPos relative) {
        var hostile = SaveReload.save(helper, relative).copy();
        var furnace = hostile.getCompound("furnace");
        var active = furnace.getCompound("active");
        active.putString("output", "minecraft:diamond");
        furnace.put("active", active);
        hostile.put("furnace", furnace);
        return hostile;
    }

    private static void replace(GameTestHelper helper, BlockPos pos, net.minecraft.server.level.ServerPlayer player, ItemStack core) {
        helper.getLevel().removeBlock(pos, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, core);
        item(FURNACE).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false)));
    }

    private static BatchWork fillOutputSlot(GameTestHelper helper, BlockPos pos) {
        var work = work(helper, pos);
        work.insertItem("minecraft:raw_iron", 1, 64, work.revision());
        work.insertItem("minecraft:iron_ingot", 64, 64, work.revision());
        work.extractItemSlot(0, work.revision());
        return work;
    }

    private static BatchWork work(GameTestHelper helper, BlockPos pos) {
        try {
            var field = helper.getLevel().getBlockEntity(pos).getClass().getDeclaredField("work");
            field.setAccessible(true);
            return (BatchWork) field.get(helper.getLevel().getBlockEntity(pos));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Bio-Furnace work store is unavailable", exception);
        }
    }

    private static int experience(GameTestHelper helper, BlockPos pos) {
        return SaveReload.save(helper, pos.subtract(helper.absolutePos(BlockPos.ZERO))).getCompound("furnace").getInt("experience");
    }

    private static BlockPos place(GameTestHelper helper, BlockPos relative) {
        var pos = helper.absolutePos(relative);
        helper.getLevel().setBlock(pos.below(), BuiltInRegistries.BLOCK.get(id("ecology/living_substrate")).defaultBlockState(), 2);
        helper.getLevel().setBlock(pos, BuiltInRegistries.BLOCK.get(FURNACE).defaultBlockState(), 2);
        return pos;
    }

    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper helper) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "BioFurnaceTest"));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void use(GameTestHelper helper, BlockPos pos, net.minecraft.server.level.ServerPlayer player, ItemStack stack) {
        useResult(helper, pos, player, stack);
    }

    private static net.minecraft.world.ItemInteractionResult useResult(GameTestHelper helper, BlockPos pos,
            net.minecraft.server.level.ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        if (stack.isEmpty()) {
            player.setPos(Vec3.atCenterOf(pos).add(0, 0, -2));
            player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        return helper.getLevel().getBlockState(pos).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void tick(GameTestHelper helper, BlockPos pos, int ticks) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        var ticker = entity.getBlockState().getTicker(entity.getLevel(), entity.getType());
        for (int tick = 0; tick < ticks; tick++) ((net.minecraft.world.level.block.entity.BlockEntityTicker) ticker)
                .tick(entity.getLevel(), entity.getBlockPos(), entity.getBlockState(), entity);
    }

    private static ItemStack craft(GameTestHelper helper, ResourceLocation id, java.util.List<ItemStack> ingredients) {
        var recipe = craftingRecipe(helper, id);
        var input = craftingInput(ingredients);
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "Exact ingredients must match " + id);
        return recipe.assemble(input, helper.getLevel().registryAccess());
    }

    private static net.minecraft.world.item.crafting.CraftingRecipe craftingRecipe(GameTestHelper helper, ResourceLocation id) {
        var holder = helper.getLevel().getRecipeManager().byKey(id).orElseThrow();
        helper.assertTrue(holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe,
                "Expected crafting recipe " + id);
        return (net.minecraft.world.item.crafting.CraftingRecipe) holder.value();
    }

    private static net.minecraft.world.item.crafting.CraftingInput craftingInput(java.util.List<ItemStack> ingredients) {
        var slots = new java.util.ArrayList<ItemStack>();
        ingredients.forEach(stack -> {
            for (int count = 0; count < stack.getCount(); count++) slots.add(stack.copyWithCount(1));
        });
        while (slots.size() < 9) slots.add(ItemStack.EMPTY);
        return net.minecraft.world.item.crafting.CraftingInput.of(3, 3, slots);
    }

    private static net.minecraft.world.item.Item item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("infestusfrontier", path);
    }
}
