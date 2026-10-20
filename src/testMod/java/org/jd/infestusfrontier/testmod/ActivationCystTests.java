package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.storage.StorageModule;

/** The Activation Cyst: the treatments in 20 seconds, the Bowl's three in 20 instead of 60, no slot for a stray item. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class ActivationCystTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos CYST = GROUND.above();
    private static final BlockPos BOWL_GROUND = new BlockPos(4, 1, 1);
    private static final BlockPos BOWL = BOWL_GROUND.above();
    private static final int CYST_TICKS = 400;
    private static final int BOWL_TICKS = 1200;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 500)
    public static void elasticGelAndBinderBecomeTwoCapillaryGelsForFiftyBiomass(GameTestHelper h) {
        placeCyst(h);
        load(h, CYST, ProcessingModule.ELASTIC_GEL.get(), 1);
        load(h, CYST, ProcessingModule.FUSION_BINDER.get(), 1);
        load(h, CYST, StorageModule.BIOMASS_BUCKET.get(), 1);
        h.runAfterDelay(1, () -> {
            BatchOrganContents started = contents(h, CYST);
            h.assertTrue(started.batch().isPresent(), "a batch runs on its own");
            h.assertValueEqual(
                    started.batch().orElseThrow().batch().recipe(),
                    "infestusfrontier:activating/capillary_gel",
                    "the capillary gel recipe");
            h.assertValueEqual(started.batch().orElseThrow().batch().ticksRequired(), CYST_TICKS, "20 seconds");
            h.assertValueEqual(started.biomass().stored().units(), 950, "50 BU spent");
        });
        h.runAtTickTime(CYST_TICKS + 2, () -> {
            BatchOrganContents done = contents(h, CYST);
            h.assertTrue(done.output().is(ProcessingModule.CAPILLARY_GEL.get()), "capillary gel out");
            h.assertValueEqual(done.output().getCount(), 2, "two doses");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theBowlsTreatmentsTakeAThirdOfTheTimeInTheCyst(GameTestHelper h) {
        placeCyst(h);
        h.setBlock(BOWL_GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(BOWL, ProcessingModule.CULTURE_BOWL.get());
        for (BlockPos organ : new BlockPos[] {CYST, BOWL}) {
            load(h, organ, Items.SLIME_BALL, 1);
            load(h, organ, EcologyModule.SPORE_CULTURE.get(), 1);
            load(h, organ, Items.WATER_BUCKET, 1);
        }
        h.runAfterDelay(1, () -> {
            h.assertValueEqual(ticksRequired(h, CYST), CYST_TICKS, "elastic gel in the cyst: 20 seconds");
            h.assertValueEqual(ticksRequired(h, BOWL), BOWL_TICKS, "elastic gel in the bowl: 60 seconds");
            h.assertValueEqual(contents(h, CYST).waterMb(), 950, "the cyst spent 50 mB of water");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSlotTakesOnlyWhatOneTreatmentUsesWithTheRest(GameTestHelper h) {
        placeCyst(h);
        IItemHandler ports =
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(CYST), Direction.UP);
        h.assertTrue(ports != null, "the cyst has item ports");
        h.assertTrue(ports.insertItem(0, new ItemStack(Items.SAND, 4), false).isEmpty(), "sand goes in");
        h.assertValueEqual(
                ports.insertItem(1, new ItemStack(Items.SAND, 4), true).getCount(),
                4,
                "a second slot never takes sand");
        h.assertTrue(
                ports.insertItem(1, new ItemStack(ProcessingModule.MEMBRANE_SHEET.get()), false)
                        .isEmpty(),
                "a sheet joins the sand");
        h.assertValueEqual(
                ports.insertItem(2, new ItemStack(Items.STRING), true).getCount(),
                1,
                "string has no treatment with sand and a sheet");
        h.assertValueEqual(ports.insertItem(2, new ItemStack(Items.DIRT), true).getCount(), 1, "dirt never");
        h.assertValueEqual(ports.extractItem(0, 4, true).getCount(), 0, "no port gives an ingredient back");
        h.succeed();
    }

    private static void placeCyst(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(CYST, ProcessingModule.ACTIVATION_CYST.get());
    }

    private static void load(GameTestHelper h, BlockPos organ, Item item, int count) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, item, count);
        for (int i = 0; i < count; i++) EcologyTests.use(h, player, organ, Direction.UP);
    }

    private static int ticksRequired(GameTestHelper h, BlockPos organ) {
        BatchOrganContents contents = contents(h, organ);
        h.assertTrue(contents.batch().isPresent(), "a batch runs at " + organ);
        return contents.batch().orElseThrow().batch().ticksRequired();
    }

    private static BatchOrganContents contents(GameTestHelper h, BlockPos organ) {
        BatchOrganContents contents =
                h.getBlockEntity(organ).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
        if (contents == null) h.fail("no organ contents", organ);
        return contents;
    }
}
