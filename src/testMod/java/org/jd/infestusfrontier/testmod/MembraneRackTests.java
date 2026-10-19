package org.jd.infestusfrontier.testmod;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.core.organ.OrganHistory;
import org.jd.infestusfrontier.core.storage.BiomassTank;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.processing.BatchOrganMenu;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.processing.api.BatchOrganContents;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassCapabilities;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/** The Membrane Rack: the second batch organ, with two slots, water and no biomass; a hopper can run it. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class MembraneRackTests {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos RACK = GROUND.above();
    private static final int FLESH_TICKS = 400;
    private static final int LEATHER_TICKS = 800;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void rackPlacesOnlyOnLivingSubstrate(GameTestHelper h) {
        h.setBlock(GROUND, Blocks.STONE);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, ProcessingModule.MEMBRANE_RACK_ITEM.get(), 1);
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(Blocks.AIR, RACK);
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.placeAt(player, player.getMainHandItem(), GROUND, Direction.UP);
        h.assertBlockPresent(ProcessingModule.MEMBRANE_RACK.get(), RACK);
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 700)
    public static void fleshStringAndWaterMakeASheet(GameTestHelper h) {
        placeRack(h);
        load(h, Items.ROTTEN_FLESH, 1);
        load(h, Items.STRING, 1);
        load(h, Items.WATER_BUCKET, 1);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
        BatchOrganMenu menu = menu(h, player);
        h.assertValueEqual(menu.ingredientSlots(), 2, "two ingredient wells");
        h.assertValueEqual(menu.waterCapacity(), 1000, "a small water tank");
        h.runAfterDelay(1, () -> {
            BatchOrganContents started = contents(h);
            h.assertTrue(started.batch().isPresent(), "a batch runs on its own");
            h.assertValueEqual(started.waterMb(), 900, "100 mB of water used");
        });
        h.runAtTickTime(FLESH_TICKS - 2, () -> h.assertTrue(contents(h).output().isEmpty(), "not done early"));
        h.runAtTickTime(FLESH_TICKS + 2, () -> {
            BatchOrganContents done = contents(h);
            h.assertTrue(done.output().is(ProcessingModule.MEMBRANE_SHEET.get()), "one Membrane Sheet");
            h.assertValueEqual(done.history().completedBatches(), 1, "counted");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 1300)
    public static void leatherTakesTwiceAsLong(GameTestHelper h) {
        placeRack(h);
        load(h, Items.LEATHER, 1);
        load(h, Items.STRING, 1);
        load(h, Items.WATER_BUCKET, 1);
        h.runAfterDelay(1, () -> h.assertTrue(contents(h).batch().isPresent(), "a batch runs"));
        h.runAtTickTime(FLESH_TICKS + 2, () -> h.assertTrue(contents(h).output().isEmpty(), "leather is slower"));
        h.runAtTickTime(LEATHER_TICKS + 2, () -> {
            h.assertTrue(contents(h).output().is(ProcessingModule.MEMBRANE_SHEET.get()), "one Membrane Sheet");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void rackTakesNoBiomass(GameTestHelper h) {
        placeRack(h);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, StorageModule.BIOMASS_BUCKET.get(), 1);
        EcologyTests.use(h, player, RACK, Direction.UP);
        h.assertTrue(player.getMainHandItem().is(StorageModule.BIOMASS_BUCKET.get()), "the bucket stays full");
        h.assertTrue(contents(h).biomass().capacity().isZero(), "no biomass tank");
        h.assertValueEqual(
                BiomassFluids.refusal(
                        new FluidStack(StorageModule.BIOMASS.get(), 1000),
                        h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(RACK), Direction.UP),
                        h.getLevel().getCapability(BiomassCapabilities.BLOCK, h.absolutePos(RACK), Direction.UP)),
                BiomassFluids.WRONG_FLUID,
                "biomass is the wrong fluid here");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 700)
    public static void aHopperLoadsTheRackAndTheRackRuns(GameTestHelper h) {
        placeRack(h);
        load(h, Items.WATER_BUCKET, 1);
        h.setBlock(HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = (HopperBlockEntity) h.getBlockEntity(HOPPER);
        hopper.setItem(0, new ItemStack(Items.ROTTEN_FLESH, 4));
        hopper.setItem(1, new ItemStack(Items.STRING, 4));
        h.runAfterDelay(40, () -> {
            BatchOrganContents fed = contents(h);
            h.assertTrue(fed.batch().isPresent(), "the hopper's items started a batch");
            h.assertValueEqual(
                    hopper.countItem(Items.ROTTEN_FLESH)
                            + fed.ingredients().get(0).getCount(),
                    3,
                    "one flesh taken");
            h.assertTrue(fed.batch().orElseThrow().starter().isEmpty(), "nobody is credited for a hopper's work");
        });
        h.runAtTickTime(FLESH_TICKS + 40, () -> {
            IItemHandler ports =
                    h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(RACK), Direction.DOWN);
            h.assertTrue(ports != null, "the rack answers item handlers");
            h.assertValueEqual(ports.getSlots(), 3, "two ingredient ports and the output");
            h.assertTrue(ports.extractItem(0, 1, true).isEmpty(), "ingredients cannot be pulled out");
            h.assertTrue(
                    ports.insertItem(2, new ItemStack(Items.STRING), true).is(Items.STRING),
                    "nothing can be pushed into the output");
            ItemStack sheet = ports.extractItem(2, 1, false);
            h.assertTrue(sheet.is(ProcessingModule.MEMBRANE_SHEET.get()), "the sheet comes out of the output port");
            h.succeed();
        });
    }

    private static final BlockPos HOPPER = RACK.above();

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aHopperOfferingOneIngredientCannotClogTheRack(GameTestHelper h) {
        placeRack(h);
        load(h, Items.WATER_BUCKET, 1);
        h.getBlockEntity(RACK)
                .applyComponents(
                        DataComponentMap.builder()
                                .set(
                                        ProcessingModule.BATCH_CONTENTS.get(),
                                        new BatchOrganContents(
                                                List.of(new ItemStack(Items.ROTTEN_FLESH, 64), ItemStack.EMPTY),
                                                ItemStack.EMPTY,
                                                1000,
                                                BiomassTank.empty(Biomass.ZERO),
                                                OrganHistory.NONE,
                                                Optional.empty()))
                                .build(),
                        DataComponentPatch.EMPTY);
        h.setBlock(HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity hopper = (HopperBlockEntity) h.getBlockEntity(HOPPER);
        hopper.setItem(0, new ItemStack(Items.ROTTEN_FLESH, 8));
        hopper.setItem(1, new ItemStack(Items.STRING, 4));
        h.runAfterDelay(40, () -> {
            BatchOrganContents fed = contents(h);
            h.assertFalse(fed.ingredients().get(1).is(Items.ROTTEN_FLESH), "the second slot never takes flesh");
            h.assertTrue(fed.batch().isPresent(), "the string got past the flesh and a batch runs");
            h.assertValueEqual(
                    hopper.countItem(Items.ROTTEN_FLESH)
                            + fed.ingredients().get(0).getCount(),
                    72 - 1,
                    "every flesh but the batch's is in the hopper or the first slot");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aSlotTakesOnlyWhatOneBatchCanUseWithTheRest(GameTestHelper h) {
        placeRack(h);
        load(h, Items.ROTTEN_FLESH, 1);
        Player leather = EcologyTests.holding(h, GameType.SURVIVAL, Items.LEATHER, 1);
        EcologyTests.use(h, leather, RACK, Direction.UP);
        h.assertValueEqual(leather.getMainHandItem().getCount(), 1, "leather stays in hand beside flesh");
        h.assertTrue(contents(h).ingredients().get(1).isEmpty(), "no recipe takes flesh and leather together");
        load(h, Items.STRING, 1);
        h.assertTrue(contents(h).ingredients().get(1).is(Items.STRING), "string goes in beside flesh");
        h.succeed();
    }

    private static void placeRack(GameTestHelper h) {
        h.setBlock(GROUND, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(RACK, ProcessingModule.MEMBRANE_RACK.get());
    }

    private static void load(GameTestHelper h, Item item, int count) {
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, item, count);
        for (int i = 0; i < count; i++) EcologyTests.use(h, player, RACK, Direction.UP);
    }

    private static BatchOrganMenu menu(GameTestHelper h, Player player) {
        MenuProvider provider = (MenuProvider) h.getBlockEntity(RACK);
        return (BatchOrganMenu) provider.createMenu(1, player.getInventory(), player);
    }

    private static BatchOrganContents contents(GameTestHelper h) {
        var tag = h.getBlockEntity(RACK).saveWithoutMetadata(h.getLevel().registryAccess());
        return BatchOrganContents.CODEC
                .parse(
                        h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                        tag.get("contents"))
                .getOrThrow();
    }
}
