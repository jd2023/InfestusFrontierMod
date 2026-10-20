package org.jd.infestusfrontier.testmod;

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
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.foundation.Outcome;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.processing.Batch;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.organ.OrganModule;
import org.jd.infestusfrontier.processing.BatchOrganBlock;
import org.jd.infestusfrontier.processing.BatchOrganMenu;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.processing.api.OrganBatch;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/** The Bio-Furnace: vanilla smelting on biomass, slow at first, faster as it grows. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class BioFurnaceTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos FURNACE = GROUND.above();
    private static final int STAGE0_TICKS = 320;
    private static final int STAGE1_TICKS = 240;
    private static final String RAW_IRON_RECIPE = "minecraft:iron_ingot_from_smelting_raw_iron";

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void rawIronAndFortyBiomassSmeltAnIngotInSixteenSeconds(GameTestHelper h) {
        placeFurnace(h);
        load(h, Items.RAW_IRON, 1);
        load(h, StorageModule.BIOMASS_BUCKET.get(), 1);
        h.runAfterDelay(1, () -> {
            BatchOrganContents started = contents(h);
            h.assertTrue(started.batch().isPresent(), "a batch runs on its own");
            h.assertValueEqual(started.batch().orElseThrow().batch().recipe(), RAW_IRON_RECIPE, "the vanilla recipe");
            h.assertValueEqual(started.batch().orElseThrow().batch().ticksRequired(), STAGE0_TICKS, "16 seconds");
            h.assertValueEqual(started.biomass().stored().units(), 960, "40 BU burnt");
            h.assertTrue(started.ingredients().get(0).isEmpty(), "the raw iron is taken");
        });
        h.runAtTickTime(
                STAGE0_TICKS - 2, () -> h.assertTrue(contents(h).output().isEmpty(), "not done early"));
        h.runAtTickTime(STAGE0_TICKS + 2, () -> {
            BatchOrganContents done = contents(h);
            h.assertTrue(done.output().is(Items.IRON_INGOT), "an iron ingot");
            h.assertValueEqual(done.history().completedBatches(), 1, "counted");
            h.assertTrue(done.experience() > 0.69f && done.experience() < 0.71f, "the recipe's 0.7 experience waits");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void onlySmeltableItemsGoIn(GameTestHelper h) {
        placeFurnace(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.STICK, 1);
        EcologyTests.use(h, player, FURNACE, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "a stick stays in the hand");
        h.assertTrue(contents(h).ingredients().get(0).isEmpty(), "and out of the furnace");
        Player water = EcologyTests.holding(h, GameType.SURVIVAL, Items.WATER_BUCKET, 1);
        EcologyTests.use(h, water, FURNACE, Direction.UP);
        h.assertTrue(water.getMainHandItem().is(Items.WATER_BUCKET), "water stays in the bucket");
        h.assertValueEqual(
                BiomassFluids.refusal(
                        new FluidStack(Fluids.WATER, 1000),
                        h.getLevel()
                                .getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(FURNACE), Direction.UP),
                        h.getLevel().getCapability(BiomassCapabilities.BLOCK, h.absolutePos(FURNACE), Direction.UP)),
                BiomassFluids.WRONG_FLUID,
                "water is the wrong fluid for a furnace");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void experienceGoesToWhoeverTakesTheResult(GameTestHelper h) {
        placeFurnace(h);
        preset(h, new ItemStack(Items.IRON_INGOT, 3), OrganHistory.NONE, Optional.empty(), 7.0f);
        var registries = h.getLevel().registryAccess();
        BlockEntity live = h.getBlockEntity(FURNACE);
        BlockEntity fresh = ((EntityBlock) ProcessingModule.BIO_FURNACE.get())
                .newBlockEntity(
                        h.absolutePos(FURNACE),
                        ProcessingModule.BIO_FURNACE.get().defaultBlockState());
        fresh.loadWithComponents(live.saveWithoutMetadata(registries), registries);
        BatchOrganContents saved = fresh.collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
        h.assertValueEqual(saved.experience(), 7.0f, "the experience survives a save");
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        player.setPos(Vec3.atBottomCenterOf(h.absolutePos(FURNACE.above())));
        BatchOrganMenu menu = menu(h, player);
        menu.clicked(menu.outputSlot(), 0, ClickType.QUICK_MOVE, player);
        h.assertValueEqual(player.getInventory().countItem(Items.IRON_INGOT), 3, "shift-click took three ingots");
        h.assertValueEqual(experienceAround(h), 7, "seven points of experience for the shift-click");
        h.assertValueEqual(contents(h).experience(), 0f, "nothing left in the furnace");
        preset(h, new ItemStack(Items.IRON_INGOT, 2), OrganHistory.NONE, Optional.empty(), 5.0f);
        BatchOrganMenu again = menu(h, player);
        again.clicked(again.outputSlot(), 0, ClickType.PICKUP, player);
        h.assertValueEqual(again.getCarried().getCount(), 2, "a plain click picks the two ingots up");
        h.assertValueEqual(experienceAround(h), 12, "five more points for the click");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void tooMuchExperienceIsUnreadable(GameTestHelper h) {
        placeFurnace(h);
        var registries = h.getLevel().registryAccess();
        BlockEntity furnace = h.getBlockEntity(FURNACE);
        CompoundTag tag = furnace.saveWithoutMetadata(registries);
        tag.getCompound("contents").putFloat("experience", 1001f);
        furnace.loadWithComponents(tag, registries);
        h.assertTrue(
                h.getBlockEntity(FURNACE).collectComponents().get(OrganModule.UNREADABLE_SAVE.get()) != null,
                "1001 points cannot be a furnace's contents");
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.RAW_IRON, 1);
        EcologyTests.use(h, player, FURNACE, Direction.UP);
        h.assertValueEqual(player.getMainHandItem().getCount(), 1, "an unreadable furnace takes nothing");
        h.succeed();
    }

    /** Two test-mod smelting recipes say -1 and 5000 experience; the furnace keeps 0 and 1000 and saves throughout. */
    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void aDataPacksWildExperienceIsClampedNotCrashed(GameTestHelper h) {
        placeFurnace(h);
        BlockPos ground2 = GROUND.east(2);
        BlockPos furnace2 = ground2.above();
        h.setBlock(ground2, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(furnace2, ProcessingModule.BIO_FURNACE.get());
        load(h, Items.ROTTEN_FLESH, 1);
        load(h, StorageModule.BIOMASS_BUCKET.get(), 1);
        Player loader = EcologyTests.holding(h, GameType.SURVIVAL, Items.STRING, 1);
        EcologyTests.use(h, loader, furnace2, Direction.UP);
        Player bucket = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, bucket, furnace2, Direction.UP);
        var registries = h.getLevel().registryAccess();
        h.runAfterDelay(2, () -> {
            for (BlockPos pos : List.of(FURNACE, furnace2)) {
                BatchOrganContents running =
                        h.getBlockEntity(pos).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
                h.assertTrue(running.batch().isPresent(), "a batch runs at " + pos);
                h.getBlockEntity(pos).saveWithoutMetadata(registries); // throws without the clamp
            }
        });
        h.runAtTickTime(STAGE0_TICKS + 2, () -> {
            BatchOrganContents sour =
                    h.getBlockEntity(FURNACE).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
            h.assertTrue(sour.output().is(Items.LEATHER), "the sour recipe still smelts");
            h.assertValueEqual(sour.experience(), 0f, "and leaves no debt");
            BatchOrganContents rich =
                    h.getBlockEntity(furnace2).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
            h.assertTrue(rich.output().is(Items.STICK), "the rich recipe smelts too");
            h.assertValueEqual(rich.experience(), BatchOrganContents.MAX_EXPERIENCE, "capped at 1000");
            h.getBlockEntity(furnace2).saveWithoutMetadata(registries);
            h.succeed();
        });
    }

    /** Every point in the orbs around the furnace; two orbs of one value merge into one orb with a count of two. */
    private static int experienceAround(GameTestHelper h) {
        List<ExperienceOrb> orbs = h.getEntities(EntityType.EXPERIENCE_ORB, FURNACE, 3.0);
        return orbs.stream()
                .mapToInt(orb ->
                        orb.getValue() * orb.saveWithoutId(new CompoundTag()).getInt("Count"))
                .sum();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void sixtyFourBatchesGrowTheFurnaceAndSpeedItUp(GameTestHelper h) {
        placeFurnace(h);
        OrganBatch almostDone = new OrganBatch(
                new Batch(RAW_IRON_RECIPE, STAGE0_TICKS, STAGE0_TICKS - 1),
                new ItemStack(Items.IRON_INGOT),
                Optional.empty(),
                0.7f);
        preset(h, ItemStack.EMPTY, new OrganHistory(63), Optional.of(almostDone), 0f);
        h.assertBlockProperty(FURNACE, BatchOrganBlock.STAGE, 0);
        h.runAfterDelay(3, () -> {
            BatchOrganContents grown = contents(h);
            h.assertValueEqual(grown.history().completedBatches(), 64, "the 64th batch is done");
            h.assertBlockProperty(FURNACE, BatchOrganBlock.STAGE, 1);
            h.assertTrue(grown.batch().isPresent(), "the next batch started from the waiting raw iron");
            h.assertValueEqual(grown.batch().orElseThrow().batch().ticksRequired(), STAGE1_TICKS, "12 seconds now");
            h.succeed();
        });
    }

    /** A furnace holding one raw iron, a bucket of biomass and whatever else the test says. */
    private static void preset(
            GameTestHelper h, ItemStack output, OrganHistory history, Optional<OrganBatch> batch, float experience) {
        h.getBlockEntity(FURNACE)
                .applyComponents(
                        DataComponentMap.builder()
                                .set(
                                        ProcessingModule.BATCH_CONTENTS.get(),
                                        new BatchOrganContents(
                                                List.of(new ItemStack(Items.RAW_IRON)),
                                                output,
                                                0,
                                                ((Outcome.Done<BiomassTank>) BiomassTank.empty(Biomass.of(2000))
                                                                .fill(Biomass.BUCKET))
                                                        .value(),
                                                history,
                                                batch,
                                                Optional.empty(),
                                                experience))
                                .build(),
                        DataComponentPatch.EMPTY);
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBrokenFurnaceDropsItselfWithItsContents(GameTestHelper h) {
        placeFurnace(h);
        preset(h, new ItemStack(Items.IRON_INGOT, 3), new OrganHistory(70), Optional.empty(), 2.0f);
        h.getLevel().destroyBlock(h.absolutePos(FURNACE), true);
        h.runAfterDelay(2, () -> {
            List<ItemEntity> drops = h.getEntities(EntityType.ITEM, FURNACE, 2.0);
            h.assertValueEqual(drops.size(), 1, "one dropped furnace");
            h.assertTrue(drops.get(0).getItem().is(ProcessingModule.BIO_FURNACE_ITEM.get()), "it is the furnace");
            BatchOrganContents carried = drops.get(0).getItem().get(ProcessingModule.BATCH_CONTENTS.get());
            h.assertTrue(carried != null, "it carries its contents");
            h.assertValueEqual(carried.history().completedBatches(), 70, "with its history");
            h.assertValueEqual(carried.output().getCount(), 3, "and its three ingots");
            h.succeed();
        });
    }

    private static void placeFurnace(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(FURNACE, ProcessingModule.BIO_FURNACE.get());
    }

    private static void load(GameTestHelper h, Item item, int count) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, item, count);
        for (int i = 0; i < count; i++) EcologyTests.use(h, player, FURNACE, Direction.UP);
    }

    private static BatchOrganMenu menu(GameTestHelper h, Player player) {
        MenuProvider provider = (MenuProvider) h.getBlockEntity(FURNACE);
        return (BatchOrganMenu) provider.createMenu(1, player.getInventory(), player);
    }

    private static BatchOrganContents contents(GameTestHelper h) {
        BatchOrganContents contents =
                h.getBlockEntity(FURNACE).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
        if (contents == null) h.fail("no furnace contents", FURNACE);
        return contents;
    }
}
