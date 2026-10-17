package org.jd.infestusfrontier.testmod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.processing.CultureBowlMenu;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.CultureBowlContents;
import org.jd.infestusfrontier.storage.StorageModule;

/** The Culture Bowl: ingredients, water and biomass loaded by hand, collected again, kept when broken. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class CultureBowlTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos BOWL = GROUND.above();

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void bowlPlacesOnlyOnLivingSubstrate(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.DIRT);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, ProcessingModule.CULTURE_BOWL_ITEM.get(), 1);
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(Blocks.AIR, BOWL);
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(ProcessingModule.CULTURE_BOWL.get(), BOWL);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void ingredientsStackIntoSlots(GameTestHelper h) {
        placeBowl(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.RED_MUSHROOM, 8);
        for (int i = 0; i < 3; i++) EcologyTests.use(h, player, BOWL, Direction.UP);
        CultureBowlContents contents = contents(h, BOWL);
        h.assertValueEqual(contents.ingredients().get(0).getCount(), 3, "slot 0 holds three");
        h.assertTrue(contents.ingredients().get(0).is(Items.RED_MUSHROOM), "slot 0 holds mushrooms");
        h.assertValueEqual(player.getMainHandItem().getCount(), 5, "three taken from the hand");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void unknownItemsPassThrough(GameTestHelper h) {
        placeBowl(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.STONE, 1);
        EcologyTests.use(h, player, BOWL, Direction.UP);
        h.assertBlockPresent(Blocks.STONE, BOWL.above());
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void waterBucketFillsTheTank(GameTestHelper h) {
        placeBowl(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.WATER_BUCKET, 1);
        EcologyTests.use(h, player, BOWL, Direction.UP);
        h.assertValueEqual(StorageTests.fluidHandler(h, BOWL).getFluidInTank(0).getAmount(), 1000, "one bucket");
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "hand holds the empty bucket");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        EcologyTests.use(h, player, BOWL, Direction.UP);
        h.assertValueEqual(StorageTests.fluidHandler(h, BOWL).getFluidInTank(0).getAmount(), 2000, "two buckets");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        EcologyTests.use(h, player, BOWL, Direction.UP);
        h.assertValueEqual(StorageTests.fluidHandler(h, BOWL).getFluidInTank(0).getAmount(), 2000, "third refused");
        h.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "the water bucket stays in the hand");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void biomassBucketFillsTheTank(GameTestHelper h) {
        placeBowl(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, player, BOWL, Direction.UP);
        h.assertValueEqual(ProcessingTests.handler(h, BOWL).tank().stored(), Biomass.BUCKET, "one bucket of biomass");
        h.assertValueEqual(StorageTests.fluidHandler(h, BOWL).getFluidInTank(0).getAmount(), 0, "no water");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void sneakEmptyHandReturnsAnIngredientStack(GameTestHelper h) {
        placeBowl(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.RED_MUSHROOM, 8);
        for (int i = 0; i < 3; i++) EcologyTests.use(h, player, BOWL, Direction.UP);
        Player taker = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        taker.setShiftKeyDown(true);
        EcologyTests.use(h, taker, BOWL, Direction.UP);
        h.assertTrue(taker.getInventory().countItem(Items.RED_MUSHROOM) == 3, "three mushrooms came back");
        h.assertTrue(contents(h, BOWL).ingredients().get(0).isEmpty(), "slot 0 is empty");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void brokenBowlKeepsItsContents(GameTestHelper h) {
        placeBowl(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.RED_MUSHROOM, 8);
        for (int i = 0; i < 3; i++) EcologyTests.use(h, player, BOWL, Direction.UP);
        Player water = EcologyTests.holding(h, GameType.SURVIVAL, Items.WATER_BUCKET, 1);
        EcologyTests.use(h, water, BOWL, Direction.UP);
        h.getLevel().destroyBlock(h.absolutePos(BOWL), true);
        h.runAfterDelay(2, () -> {
            List<ItemEntity> drops = h.getEntities(EntityType.ITEM, BOWL, 2.0);
            h.assertValueEqual(drops.size(), 1, "one dropped bowl");
            ItemStack dropped = drops.get(0).getItem();
            drops.get(0).discard();
            BlockPos ground2 = GROUND.east(2);
            h.setBlock(ground2, EcologyModule.LIVING_SUBSTRATE.get());
            Player placer = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            placer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dropped.copy());
            h.placeAt(placer, placer.getMainHandItem(), ground2, Direction.UP);
            CultureBowlContents placed = contents(h, ground2.above());
            h.assertValueEqual(placed.ingredients().get(0).getCount(), 3, "three mushrooms kept");
            h.assertValueEqual(placed.waterMb(), 1000, "1000 mB kept");
            h.succeed();
        });
    }

    private static final int BATCH_TICKS = 1200;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1300)
    public static void cultureBatchYieldsOneCultureAfter1200Ticks(GameTestHelper h) {
        placeBowl(h);
        loadCultureInputs(h, true);
        startBatch(h);
        CultureBowlContents started = contents(h, BOWL);
        h.assertTrue(started.ingredients().stream().allMatch(ItemStack::isEmpty), "ingredients consumed at once");
        h.assertValueEqual(started.waterMb(), 900, "100 mB of water consumed at once");
        h.assertTrue(started.batch().isPresent(), "a batch runs");
        h.runAtTickTime(BATCH_TICKS - 2, () -> {
            h.assertTrue(contents(h, BOWL).output().isEmpty(), "output still empty at tick 1198");
            h.assertTrue(contents(h, BOWL).batch().isPresent(), "batch still runs at tick 1198");
        });
        h.runAtTickTime(BATCH_TICKS + 2, () -> {
            ItemStack out = contents(h, BOWL).output();
            h.assertTrue(out.is(EcologyModule.SPORE_CULTURE.get()), "spore culture in the output at tick 1202");
            h.assertValueEqual(out.getCount(), 1, "exactly one");
            h.assertTrue(contents(h, BOWL).batch().isEmpty(), "batch finished");
            h.assertValueEqual(contents(h, BOWL).history().completedBatches(), 1, "history counts the batch");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void startRefusesWithoutWater(GameTestHelper h) {
        placeBowl(h);
        loadCultureInputs(h, false);
        startBatch(h);
        CultureBowlContents after = contents(h, BOWL);
        h.assertTrue(after.ingredients().get(0).is(Items.RED_MUSHROOM), "mushroom still there");
        h.assertTrue(after.ingredients().get(1).is(Items.WHEAT_SEEDS), "seed still there");
        h.assertTrue(after.batch().isEmpty(), "no batch");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void startRefusesWhileWorking(GameTestHelper h) {
        placeBowl(h);
        loadCultureInputs(h, true);
        startBatch(h);
        loadCultureInputs(h, false);
        h.runAfterDelay(20, () -> {
            startBatch(h);
            CultureBowlContents after = contents(h, BOWL);
            h.assertTrue(after.ingredients().stream().allMatch(ItemStack::isEmpty), "nothing was loaded meanwhile");
            h.assertValueEqual(after.waterMb(), 900, "no second spend of water");
            h.assertTrue(after.batch().orElseThrow().batch().ticksDone() >= 20, "the first batch kept its progress");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void budRecipeNeedsFourItems(GameTestHelper h) {
        placeBowl(h);
        load(h, EcologyModule.SPORE_CULTURE.get(), 1);
        load(h, Items.ROTTEN_FLESH, 1);
        load(h, Items.BONE_MEAL, 1);
        startBatch(h);
        h.assertTrue(contents(h, BOWL).batch().isEmpty(), "three items do not start the bud");
        h.assertTrue(contents(h, BOWL).ingredients().get(1).is(Items.ROTTEN_FLESH), "items stay");
        load(h, Items.ROTTEN_FLESH, 1);
        startBatch(h);
        CultureBowlContents started = contents(h, BOWL);
        h.assertTrue(started.batch().isPresent(), "four items start the bud");
        h.assertTrue(
                started.batch()
                        .orElseThrow()
                        .result()
                        .is(org.jd.infestusfrontier.organ.OrganModule.ORGAN_BUD_ITEM.get()),
                "the bud is promised");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void reloadKeepsContentsAndBatch(GameTestHelper h) {
        placeBowl(h);
        loadCultureInputs(h, true);
        startBatch(h);
        h.runAfterDelay(20, () -> {
            var registries = h.getLevel().registryAccess();
            BlockEntity live = h.getBlockEntity(BOWL);
            CompoundTag tag = live.saveWithoutMetadata(registries);
            BlockEntity fresh = ((EntityBlock) ProcessingModule.CULTURE_BOWL.get())
                    .newBlockEntity(
                            h.absolutePos(BOWL),
                            ProcessingModule.CULTURE_BOWL.get().defaultBlockState());
            fresh.loadWithComponents(tag, registries);
            CultureBowlContents loaded = fresh.collectComponents().get(ProcessingModule.BOWL_CONTENTS.get());
            h.assertTrue(loaded != null, "the fresh entity has contents");
            h.assertValueEqual(loaded.waterMb(), 900, "water kept");
            h.assertTrue(loaded.output().isEmpty(), "output empty");
            h.assertTrue(loaded.batch().isPresent(), "batch kept");
            h.assertValueEqual(
                    loaded.batch().orElseThrow().batch(),
                    contents(h, BOWL).batch().orElseThrow().batch(),
                    "batch ticks kept");
            h.assertTrue(loaded.batch().orElseThrow().batch().ticksDone() > 0, "the batch had progressed");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void unreadableSaveRefusesWater(GameTestHelper h) {
        placeBowl(h);
        var registries = h.getLevel().registryAccess();
        BlockEntity bowl = h.getBlockEntity(BOWL);
        CompoundTag tag = bowl.saveWithoutMetadata(registries);
        tag.getCompound("contents").putInt("water", 5000);
        bowl.loadWithComponents(tag, registries);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.WATER_BUCKET, 1);
        EcologyTests.use(h, player, BOWL, Direction.UP);
        h.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET), "the bucket stays full in hand");
        h.assertValueEqual(StorageTests.fluidHandler(h, BOWL).getFluidInTank(0).getAmount(), 0, "no water entered");
        h.assertTrue(
                bowl.saveWithoutMetadata(registries).getCompound("contents").getInt("water") == 5000, "tag kept");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void creativeBreakDropsTheLoadedBowl(GameTestHelper h) {
        placeBowl(h);
        load(h, Items.RED_MUSHROOM, 3);
        Player creative = EcologyTests.holding(h, GameType.CREATIVE, Items.AIR, 0);
        var state = h.getLevel().getBlockState(h.absolutePos(BOWL));
        state.getBlock().playerWillDestroy(h.getLevel(), h.absolutePos(BOWL), state, creative);
        List<ItemEntity> drops = h.getEntities(EntityType.ITEM, BOWL, 2.0);
        h.assertValueEqual(drops.size(), 1, "one dropped bowl");
        ItemStack dropped = drops.get(0).getItem();
        h.assertTrue(dropped.is(ProcessingModule.CULTURE_BOWL_ITEM.get()), "it is a bowl");
        CultureBowlContents carried = dropped.get(ProcessingModule.BOWL_CONTENTS.get());
        h.assertTrue(carried != null, "it carries the contents");
        h.assertValueEqual(carried.ingredients().get(0).getCount(), 3, "three mushrooms carried");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void outputFullRefusesStart(GameTestHelper h) {
        placeBowl(h);
        List<ItemStack> held =
                new ArrayList<>(List.of(new ItemStack(Items.RED_MUSHROOM), new ItemStack(Items.WHEAT_SEEDS)));
        while (held.size() < 9) held.add(ItemStack.EMPTY);
        CultureBowlContents preset = new CultureBowlContents(
                held,
                new ItemStack(EcologyModule.SPORE_CULTURE.get(), 64),
                100,
                BiomassTank.empty(Biomass.of(2000)),
                OrganHistory.NONE,
                Optional.empty());
        h.getBlockEntity(BOWL)
                .applyComponents(
                        DataComponentMap.builder()
                                .set(ProcessingModule.BOWL_CONTENTS.get(), preset)
                                .build(),
                        DataComponentPatch.EMPTY);
        startBatch(h);
        CultureBowlContents after = contents(h, BOWL);
        h.assertTrue(after.batch().isEmpty(), "no batch");
        h.assertTrue(after.ingredients().get(0).is(Items.RED_MUSHROOM), "ingredients stay");
        h.assertValueEqual(after.waterMb(), 100, "water stays");
        h.assertValueEqual(after.output().getCount(), 64, "output untouched");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void menuReportsWaterBiomassAndProgress(GameTestHelper h) {
        placeBowl(h);
        loadCultureInputs(h, true);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        CultureBowlMenu idle = menu(h, player);
        h.assertValueEqual(idle.water(), 1000, "water before the start");
        h.assertValueEqual(idle.state(), CultureBowlMenu.IDLE, "idle before the start");
        startBatch(h);
        CultureBowlMenu menu = menu(h, player);
        h.assertValueEqual(menu.water(), 900, "water after the start");
        h.assertValueEqual(menu.stored(), 0, "no biomass");
        h.assertValueEqual(menu.capacity(), 2000, "biomass capacity");
        h.assertValueEqual(menu.state(), CultureBowlMenu.WORKING, "working");
        h.runAfterDelay(120, () -> {
            h.assertTrue(menu.percent() > 0, "progress grows");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void startButtonStartsABatch(GameTestHelper h) {
        placeBowl(h);
        loadCultureInputs(h, true);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        CultureBowlMenu menu = menu(h, player);
        h.assertTrue(menu.clickMenuButton(player, CultureBowlMenu.START_BUTTON), "the click is handled");
        h.assertTrue(contents(h, BOWL).batch().isPresent(), "a batch runs");
        h.runAfterDelay(10, () -> {
            int done = contents(h, BOWL).batch().orElseThrow().batch().ticksDone();
            h.assertTrue(menu.clickMenuButton(player, CultureBowlMenu.START_BUTTON), "a second click is handled");
            h.assertValueEqual(
                    contents(h, BOWL).batch().orElseThrow().batch().ticksDone(), done, "the first batch stays");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1300)
    public static void insertWaitsDuringABatch(GameTestHelper h) {
        placeBowl(h);
        loadCultureInputs(h, true);
        startBatch(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.RED_MUSHROOM, 1);
        EcologyTests.use(h, player, BOWL, Direction.UP);
        h.assertTrue(player.getMainHandItem().is(Items.RED_MUSHROOM), "the mushroom stays in the hand");
        h.assertTrue(contents(h, BOWL).ingredients().stream().allMatch(ItemStack::isEmpty), "no slot filled");
        h.runAtTickTime(BATCH_TICKS + 2, () -> {
            EcologyTests.use(h, player, BOWL, Direction.UP);
            h.assertTrue(player.getMainHandItem().isEmpty(), "the mushroom is accepted after the batch");
            h.assertTrue(contents(h, BOWL).ingredients().get(0).is(Items.RED_MUSHROOM), "slot 0 holds it");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void reloadKeepsIngredients(GameTestHelper h) {
        placeBowl(h);
        load(h, Items.RED_MUSHROOM, 3);
        load(h, Items.BONE_MEAL, 1);
        var registries = h.getLevel().registryAccess();
        CompoundTag tag = h.getBlockEntity(BOWL).saveWithoutMetadata(registries);
        BlockEntity fresh = ((EntityBlock) ProcessingModule.CULTURE_BOWL.get())
                .newBlockEntity(
                        h.absolutePos(BOWL), ProcessingModule.CULTURE_BOWL.get().defaultBlockState());
        fresh.loadWithComponents(tag, registries);
        CultureBowlContents loaded = fresh.collectComponents().get(ProcessingModule.BOWL_CONTENTS.get());
        h.assertTrue(loaded != null, "the fresh entity has contents");
        h.assertValueEqual(loaded.ingredients().get(0).getCount(), 3, "slot 0 holds three mushrooms");
        h.assertTrue(loaded.ingredients().get(0).is(Items.RED_MUSHROOM), "slot 0 holds mushrooms");
        h.assertValueEqual(loaded.ingredients().get(1).getCount(), 1, "slot 1 holds one bone meal");
        h.assertTrue(loaded.ingredients().get(1).is(Items.BONE_MEAL), "slot 1 holds bone meal");
        h.succeed();
    }

    private static CultureBowlMenu menu(GameTestHelper h, Player player) {
        // No GameTest player can receive a screen, so the menu is built the way the block entity builds it.
        MenuProvider provider = (MenuProvider) h.getBlockEntity(BOWL);
        return (CultureBowlMenu) provider.createMenu(1, player.getInventory(), player);
    }

    private static void load(GameTestHelper h, net.minecraft.world.item.Item item, int count) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, item, count);
        for (int i = 0; i < count; i++) EcologyTests.use(h, player, BOWL, Direction.UP);
    }

    private static void loadCultureInputs(GameTestHelper h, boolean withWater) {
        load(h, Items.RED_MUSHROOM, 1);
        load(h, Items.WHEAT_SEEDS, 1);
        if (withWater) load(h, Items.WATER_BUCKET, 1);
    }

    private static void startBatch(GameTestHelper h) {
        EcologyTests.use(h, EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0), BOWL, Direction.UP);
    }

    private static void placeBowl(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(BOWL, ProcessingModule.CULTURE_BOWL.get());
    }

    private static CultureBowlContents contents(GameTestHelper h, BlockPos pos) {
        CultureBowlContents contents =
                h.getBlockEntity(pos).collectComponents().get(ProcessingModule.BOWL_CONTENTS.get());
        if (contents == null) h.fail("no bowl contents", pos);
        return contents;
    }
}
