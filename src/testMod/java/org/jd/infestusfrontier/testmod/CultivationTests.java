package org.jd.infestusfrontier.testmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.cultivation.CultivationModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.MouthBlock;
import org.jd.infestusfrontier.logistics.MouthMode;
import org.jd.infestusfrontier.logistics.Port;
import org.jd.infestusfrontier.logistics.VascularJunctionBlock;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/** The farm: beds that grow crops, a pouch that keeps its reserve, a planter and a harvester that run on biomass. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class CultivationTests {
    private static final BlockPos CENTER = new BlockPos(3, 1, 3);
    private static final BlockPos ORGAN = CENTER.above();

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void rootingGelMakesABedThatTakesWheatAndWaterButNoBiomass(GameTestHelper h) {
        h.setBlock(CENTER, EcologyTests.mature());
        Player grafter = EcologyTests.holding(h, GameType.SURVIVAL, CultivationModule.ROOTING_GEL.get(), 1);
        EcologyTests.use(h, grafter, CENTER, Direction.UP);
        h.assertBlockPresent(CultivationModule.CULTIVATION_TISSUE.get(), CENTER);
        Player sower = EcologyTests.holding(h, GameType.SURVIVAL, Items.WHEAT_SEEDS, 1);
        EcologyTests.use(h, sower, CENTER, Direction.UP);
        h.assertBlockPresent(Blocks.WHEAT, ORGAN);
        h.assertFalse(h.getBlockState(CENTER).isFertile(h.getLevel(), h.absolutePos(CENTER)), "dry at first");
        Player water = EcologyTests.holding(h, GameType.SURVIVAL, Items.WATER_BUCKET, 1);
        EcologyTests.use(h, water, CENTER, Direction.NORTH);
        h.assertTrue(water.getMainHandItem().is(Items.BUCKET), "the bucket emptied into the bed");
        h.assertTrue(h.getBlockState(CENTER).isFertile(h.getLevel(), h.absolutePos(CENTER)), "wet now");
        IFluidHandler tank =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(CENTER), Direction.UP);
        h.assertValueEqual(
                tank.fill(BiomassFluids.of(Biomass.of(100)), IFluidHandler.FluidAction.SIMULATE),
                0,
                "no biomass in a bed");
        h.assertValueEqual(tank.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE), 0, "full");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aPouchGivesOnlyAboveItsReserveAndTheStickMovesIt(GameTestHelper h) {
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(ORGAN, CultivationModule.SEED_POUCH.get());
        Player sower = EcologyTests.holding(h, GameType.SURVIVAL, Items.WHEAT_SEEDS, 5);
        for (int i = 0; i < 5; i++) EcologyTests.use(h, sower, ORGAN, Direction.UP);
        h.assertTrue(sower.getMainHandItem().isEmpty(), "five seeds went in by hand");
        Player stone = EcologyTests.holding(h, GameType.SURVIVAL, Items.STONE, 1);
        EcologyTests.use(h, stone, ORGAN, Direction.UP);
        h.assertValueEqual(stone.getMainHandItem().getCount(), 1, "stone is not planting stock: the screen opens");
        IItemHandler surplus =
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(ORGAN), Direction.UP);
        h.assertValueEqual(surplus.extractItem(0, 64, true).getCount(), 4, "the reserve starts at one");
        Player stick = EcologyTests.holding(h, GameType.SURVIVAL, Items.STICK, 1);
        EcologyTests.use(h, stick, ORGAN, Direction.UP);
        EcologyTests.use(h, stick, ORGAN, Direction.UP);
        h.assertValueEqual(surplus.extractItem(0, 64, true).getCount(), 2, "two sticks: reserve three");
        for (int click = 3; click < 65; click++) EcologyTests.use(h, stick, ORGAN, Direction.UP);
        h.assertValueEqual(surplus.extractItem(0, 64, true).getCount(), 5, "at 65 clicks the reserve is 0 again");
        EcologyTests.use(h, stick, ORGAN, Direction.UP);
        h.assertValueEqual(surplus.extractItem(0, 64, false).getCount(), 4, "then 1: four taken");
        h.assertValueEqual(surplus.getStackInSlot(0).getCount(), 1, "the reserve stays");
        h.assertTrue(
                surplus.insertItem(1, new ItemStack(Items.WHEAT_SEEDS, 4), true).getCount() == 4, "one kind a slot");
        h.assertTrue(
                surplus.insertItem(1, new ItemStack(Items.CARROT, 4), true).isEmpty(),
                "a second kind takes the next slot");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 400)
    public static void theProboscisPlantsItsPlotFromAPouchAndPaysFiveEach(GameTestHelper h) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                h.setBlock(CENTER.offset(dx, 0, dz), CultivationModule.CULTIVATION_TISSUE.get());
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(ORGAN, CultivationModule.PLANTING_PROBOSCIS.get());
        BlockPos pouchGround = CENTER.west();
        h.setBlock(pouchGround, EcologyTests.mature());
        h.setBlock(pouchGround.above(), CultivationModule.SEED_POUCH.get());
        IItemHandler pouch = h.getLevel()
                .getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(pouchGround.above()), Direction.UP);
        pouch.insertItem(0, new ItemStack(Items.WHEAT_SEEDS, 6), false);
        Player fuel = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, fuel, ORGAN, Direction.UP);
        h.runAfterDelay(300, () -> {
            int wheat = 0;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    if (h.getBlockState(ORGAN.offset(dx, 0, dz)).is(Blocks.WHEAT)) wheat++;
            h.assertValueEqual(wheat, 5, "five seeds planted: six in the pouch less its reserve of one");
            h.assertValueEqual(pouch.getStackInSlot(0).getCount(), 1, "the reserve stays in the pouch");
            h.assertValueEqual(ProcessingTests.handler(h, ORGAN).tank().stored().units(), 975, "25 BU paid");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aJunctionFeedingThePlanterDoesNotHurryIt(GameTestHelper h) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                h.setBlock(CENTER.offset(dx, 0, dz), CultivationModule.CULTIVATION_TISSUE.get());
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(ORGAN, CultivationModule.PLANTING_PROBOSCIS.get());
        h.getLevel()
                .getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(ORGAN), Direction.UP)
                .insertItem(0, new ItemStack(Items.WHEAT_SEEDS, 8), false);
        Player fuel = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, fuel, ORGAN, Direction.UP);
        // the junction stands beside the planter on the plot's east cell, the Bladder beyond it
        BlockPos junction = ORGAN.east();
        BlockPos tank = junction.east();
        h.setBlock(junction.below(), EcologyTests.mature());
        h.setBlock(tank.below(), EcologyTests.mature());
        h.setBlock(tank, StorageModule.BIOMASS_BLADDER.get());
        h.getLevel()
                .getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(tank), Direction.UP)
                .fill(BiomassFluids.of(Biomass.of(4000)), IFluidHandler.FluidAction.EXECUTE);
        h.setBlock(
                junction,
                LogisticsModule.VASCULAR_JUNCTION
                        .get()
                        .defaultBlockState()
                        .setValue(VascularJunctionBlock.PORT.get(Direction.EAST), Port.INTAKE)
                        .setValue(VascularJunctionBlock.PORT.get(Direction.WEST), Port.OUTLET));
        h.runAfterDelay(70, () -> {
            int wheat = 0;
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    if (h.getBlockState(ORGAN.offset(dx, 0, dz)).is(Blocks.WHEAT)) wheat++;
            h.assertTrue(wheat <= 4, "at most one planting a pulse while the junction tops the tank up: " + wheat);
            h.assertTrue(wheat >= 2, "and it keeps planting: " + wheat);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 500)
    public static void theCompostGlandMakesBoneMealFromFourSeedsAndWater(GameTestHelper h) {
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(ORGAN, ProcessingModule.COMPOST_GLAND.get());
        Player water = EcologyTests.holding(h, GameType.SURVIVAL, Items.WATER_BUCKET, 1);
        EcologyTests.use(h, water, ORGAN, Direction.UP);
        IItemHandler ports =
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(ORGAN), Direction.UP);
        h.assertTrue(
                ports.insertItem(0, new ItemStack(Items.WHEAT_SEEDS, 3), false).isEmpty(), "a hopper loads seeds");
        h.runAfterDelay(2, () -> {
            h.assertTrue(compost(h).batch().isEmpty(), "three seeds do not start a batch");
            ports.insertItem(0, new ItemStack(Items.WHEAT_SEEDS, 1), false);
            h.runAfterDelay(2, () -> {
                h.assertTrue(compost(h).batch().isPresent(), "four seeds start on their own");
                h.assertValueEqual(compost(h).batch().orElseThrow().batch().ticksRequired(), 400, "20 seconds");
                h.assertValueEqual(compost(h).waterMb(), 900, "100 mB of water spent");
                h.assertTrue(compost(h).ingredients().get(0).isEmpty(), "all four seeds taken");
            });
        });
        h.runAtTickTime(410, () -> {
            h.assertTrue(compost(h).output().is(Items.BONE_MEAL), "one bone meal");
            h.assertValueEqual(compost(h).output().getCount(), 1, "one");
            h.succeed();
        });
    }

    private static BatchOrganContents compost(GameTestHelper h) {
        BatchOrganContents contents =
                h.getBlockEntity(ORGAN).collectComponents().get(ProcessingModule.BATCH_CONTENTS.get());
        if (contents == null) h.fail("no compost gland contents", ORGAN);
        return contents;
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void theCorollaCutsRipeCropsIntoItsSlotsAndLeavesTheRest(GameTestHelper h) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                h.setBlock(CENTER.offset(dx, 0, dz), CultivationModule.CULTIVATION_TISSUE.get());
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(ORGAN, CultivationModule.HARVEST_COROLLA.get());
        h.setBlock(ORGAN.north(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        h.setBlock(ORGAN.south(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3));
        h.setBlock(ORGAN.east(), Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, 7));
        Player fuel = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, fuel, ORGAN, Direction.UP);
        h.runAfterDelay(100, () -> {
            h.assertBlockPresent(Blocks.AIR, ORGAN.north());
            h.assertBlockPresent(Blocks.AIR, ORGAN.east());
            h.assertBlockPresent(Blocks.WHEAT, ORGAN.south());
            IItemHandler out =
                    h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(ORGAN), Direction.UP);
            int wheat = 0, carrots = 0;
            for (int slot = 0; slot < out.getSlots(); slot++) {
                if (out.getStackInSlot(slot).is(Items.WHEAT))
                    wheat += out.getStackInSlot(slot).getCount();
                if (out.getStackInSlot(slot).is(Items.CARROT))
                    carrots += out.getStackInSlot(slot).getCount();
            }
            h.assertValueEqual(wheat, 1, "one wheat from the ripe wheat");
            h.assertTrue(carrots >= 2, "carrots from the ripe carrots: " + carrots);
            h.assertValueEqual(
                    out.insertItem(0, new ItemStack(Items.STONE), true).getCount(), 1, "nothing goes in");
            h.assertValueEqual(
                    out.extractItem(0, 64, true).getCount(),
                    out.getStackInSlot(0).getCount(),
                    "everything comes out");
            h.assertValueEqual(
                    ProcessingTests.handler(h, ORGAN).tank().stored().units(), 990, "10 BU paid for two crops");
            h.assertItemEntityNotPresent(Items.WHEAT, ORGAN, 3.0);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void aFullCorollaLeavesTheCropStanding(GameTestHelper h) {
        h.setBlock(CENTER, EcologyTests.mature());
        h.setBlock(CENTER.north(), CultivationModule.CULTIVATION_TISSUE.get());
        h.setBlock(ORGAN, CultivationModule.HARVEST_COROLLA.get());
        h.setBlock(ORGAN.north(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        IItemHandler out =
                h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(ORGAN), Direction.UP);
        var inner = h.getBlockEntity(ORGAN);
        for (int slot = 0; slot < out.getSlots(); slot++)
            ((org.jd.infestusfrontier.cultivation.PlotOrganMenu) ((net.minecraft.world.MenuProvider) inner)
                            .createMenu(
                                    1,
                                    EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0)
                                            .getInventory(),
                                    null))
                    .getSlot(slot)
                    .set(new ItemStack(Items.STONE, 64));
        Player fuel = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, fuel, ORGAN, Direction.UP);
        h.runAfterDelay(60, () -> {
            h.assertBlockPresent(Blocks.WHEAT, ORGAN.north());
            h.assertValueEqual(ProcessingTests.handler(h, ORGAN).tank().stored().units(), 1000, "nothing paid");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 100)
    public static void anOutputMouthEmptiesTheCorollaIntoAChest(GameTestHelper h) {
        // the corolla stands on an output Mouth; an Item Vein leads to an intake Mouth under the chest
        h.setBlock(CENTER, mouth(MouthMode.OUTPUT));
        h.setBlock(CENTER.north(), CultivationModule.CULTIVATION_TISSUE.get());
        h.setBlock(ORGAN, CultivationModule.HARVEST_COROLLA.get());
        h.setBlock(ORGAN.north(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
        h.setBlock(CENTER.east(), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(CENTER.east().east(), mouth(MouthMode.INTAKE));
        h.setBlock(ORGAN.east().east(), Blocks.CHEST);
        Player fuel = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, fuel, ORGAN, Direction.UP);
        h.runAfterDelay(80, () -> {
            ChestBlockEntity chest =
                    (ChestBlockEntity) h.getBlockEntity(ORGAN.east().east());
            h.assertValueEqual(chest.countItem(Items.WHEAT), 1, "the wheat reached the chest");
            h.succeed();
        });
    }

    private static BlockState mouth(MouthMode mode) {
        return LogisticsModule.MOUTH.get().defaultBlockState().setValue(MouthBlock.MODE, mode);
    }
}
