package org.jd.infestusfrontier.testmod;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jd.infestusfrontier.core.foundation.Biomass;
import org.jd.infestusfrontier.ecology.EcologyModule;
import org.jd.infestusfrontier.interaction.InteractionModule;
import org.jd.infestusfrontier.logistics.LogisticsModule;
import org.jd.infestusfrontier.logistics.Port;
import org.jd.infestusfrontier.logistics.VascularJunctionBlock;
import org.jd.infestusfrontier.logistics.VeinBlock;
import org.jd.infestusfrontier.processing.ProcessingModule;
import org.jd.infestusfrontier.storage.StorageModule;
import org.jd.infestusfrontier.storage.api.BiomassFluids;

/** Veins and the Vascular Junction: grafting, pumping, pushing on, holding back, breaking, and the probe on a port. */
@GameTestHolder("infestusfrontier_tests")
@PrefixGameTestTemplate(false)
public final class LogisticsTests {
    private static final int Y = 1;

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void capillaryGelGrowsAVeinOnlyFromExposedMatureSubstrate(GameTestHelper h) {
        BlockPos basic = new BlockPos(1, Y, 1);
        BlockPos mature = new BlockPos(3, Y, 1);
        BlockPos covered = new BlockPos(5, Y, 1);
        h.setBlock(basic, EcologyModule.LIVING_SUBSTRATE.get());
        h.setBlock(mature, EcologyTests.mature());
        h.setBlock(covered, EcologyTests.mature());
        h.setBlock(covered.above(), Blocks.STONE);
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, LogisticsModule.CAPILLARY_GEL.get(), 3);
        EcologyTests.use(h, player, basic, Direction.UP);
        EcologyTests.use(h, player, covered, Direction.NORTH);
        h.assertBlockPresent(EcologyModule.LIVING_SUBSTRATE.get(), basic);
        h.assertBlockPresent(EcologyModule.LIVING_SUBSTRATE.get(), covered);
        h.assertValueEqual(player.getMainHandItem().getCount(), 3, "nothing spent on a basic or a covered cell");
        EcologyTests.use(h, player, mature, Direction.UP);
        h.assertBlockPresent(LogisticsModule.BIOMASS_VEIN.get(), mature);
        h.assertValueEqual(player.getMainHandItem().getCount(), 2, "one gel spent");
        h.setBlock(mature.east(), LogisticsModule.BIOMASS_VEIN.get());
        h.assertTrue(
                h.getBlockState(mature).getValue(PipeBlock.EAST)
                        && h.getBlockState(mature.east()).getValue(PipeBlock.WEST),
                "the two cells connect in the block state");
        h.assertFalse(h.getBlockState(mature).getValue(PipeBlock.WEST), "no vein to the west");
        h.succeed();
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aJunctionPumpsABladderThroughVeinsIntoALoomAndOnToABladder(GameTestHelper h) {
        // ground row at y=1 from x=1 to x=6; the source bladder at x=1, the junction at x=2, veins under x=2..5, the
        // loom on x=4, a tank on x=5
        for (int x = 1; x <= 6; x++) h.setBlock(new BlockPos(x, Y, 1), EcologyTests.mature());
        BlockPos source = new BlockPos(1, Y + 1, 1);
        BlockPos junction = new BlockPos(2, Y + 1, 1);
        BlockPos loom = new BlockPos(4, Y + 1, 1);
        BlockPos sink = new BlockPos(5, Y + 1, 1);
        h.setBlock(source, StorageModule.BIOMASS_BLADDER.get());
        fill(h, source, 3000);
        h.setBlock(
                junction,
                LogisticsModule.VASCULAR_JUNCTION
                        .get()
                        .defaultBlockState()
                        .setValue(VascularJunctionBlock.PORT.get(Direction.WEST), Port.INTAKE)
                        .setValue(VascularJunctionBlock.PORT.get(Direction.DOWN), Port.OUTLET));
        for (int x = 2; x <= 5; x++) h.setBlock(new BlockPos(x, Y, 1), LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(loom, ProcessingModule.BONE_LOOM.get());
        h.setBlock(sink, StorageModule.BIOMASS_BLADDER.get());
        h.runAfterDelay(150, () -> {
            int loomHas = ProcessingTests.handler(h, loom).tank().stored().units();
            int sinkHas = ProcessingTests.handler(h, sink).tank().stored().units();
            int sourceHas = ProcessingTests.handler(h, source).tank().stored().units();
            h.assertValueEqual(loomHas, 1000, "the loom on the vein filled first, to its capacity");
            h.assertTrue(sinkHas > 0, "the rest flows on to the tank at the end");
            int inTransit = 0;
            for (int x = 2; x <= 5; x++) inTransit += biomassIn(h, new BlockPos(x, Y, 1));
            inTransit += biomassIn(h, junction);
            h.assertValueEqual(sourceHas + loomHas + sinkHas + inTransit, 3000, "every BU is somewhere");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aFullDestinationHoldsThePacketAndTheVeinResumesWhenRoomReturns(GameTestHelper h) {
        BlockPos cell = new BlockPos(1, Y, 1);
        BlockPos next = new BlockPos(2, Y, 1);
        BlockPos tank = next.above();
        h.setBlock(cell, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(next, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(tank, StorageModule.BIOMASS_BLADDER.get());
        fill(h, tank, 4000);
        IFluidHandler port =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(cell), Direction.UP);
        h.assertValueEqual(
                port.fill(BiomassFluids.of(Biomass.of(250)), IFluidHandler.FluidAction.EXECUTE),
                250,
                "a packet goes in from above");
        h.assertValueEqual(
                port.fill(BiomassFluids.of(Biomass.of(100)), IFluidHandler.FluidAction.EXECUTE),
                0,
                "a full cell takes no more");
        h.runAfterDelay(30, () -> {
            h.assertValueEqual(
                    biomassIn(h, cell) + biomassIn(h, next), 250, "the packet waits in the vein before the full tank");
            h.assertValueEqual(ProcessingTests.handler(h, tank).tank().stored().units(), 4000, "the tank stays full");
            IFluidHandler out =
                    h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(tank), Direction.UP);
            out.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            h.runAfterDelay(VeinBlock.RETRY_TICKS + 10, () -> {
                h.assertValueEqual(
                        ProcessingTests.handler(h, tank).tank().stored().units(),
                        3250,
                        "the packet arrived once there was room");
                h.assertValueEqual(biomassIn(h, cell) + biomassIn(h, next), 0, "the vein is empty again");
                h.succeed();
            });
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void anItemVeinCarriesAHoppersItemsToAChest(GameTestHelper h) {
        BlockPos hopper = new BlockPos(1, Y + 1, 1);
        BlockPos chest = new BlockPos(5, Y + 1, 1);
        for (int x = 1; x <= 5; x++) h.setBlock(new BlockPos(x, Y, 1), LogisticsModule.ITEM_VEIN.get());
        h.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        h.setBlock(chest, Blocks.CHEST);
        ((HopperBlockEntity) h.getBlockEntity(hopper)).setItem(0, new ItemStack(Items.BONE, 20));
        h.runAfterDelay(180, () -> {
            ChestBlockEntity box = (ChestBlockEntity) h.getBlockEntity(chest);
            int inChest = box.countItem(Items.BONE);
            int inHopper = ((HopperBlockEntity) h.getBlockEntity(hopper)).countItem(Items.BONE);
            h.assertTrue(inChest > 0, "bones reached the chest");
            h.assertValueEqual(inChest + inHopper + itemsIn(h, 1, 5), 20, "every bone is somewhere");
            h.assertTrue(itemsIn(h, 1, 1) == 0 || inHopper > 0, "the first cell drains towards the chest");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty", timeoutTicks = 200)
    public static void aSealedFluidVeinCarriesWaterFromASourceToABowlAndRefusesBiomass(GameTestHelper h) {
        BlockPos water = new BlockPos(1, Y + 1, 1);
        BlockPos junctionGround = new BlockPos(2, Y, 1);
        BlockPos junction = junctionGround.above();
        BlockPos vein = new BlockPos(3, Y, 1);
        BlockPos bowlGround = new BlockPos(4, Y, 1);
        BlockPos bowl = bowlGround.above();
        h.setBlock(water.below(), Blocks.STONE);
        h.setBlock(water, Blocks.WATER);
        BlockState sealedVein =
                LogisticsModule.FLUID_VEIN.get().defaultBlockState().setValue(VeinBlock.SEALED, true);
        h.setBlock(junctionGround, sealedVein);
        h.setBlock(vein, EcologyTests.mature());
        h.setBlock(bowlGround, sealedVein);
        h.setBlock(bowl, ProcessingModule.CULTURE_BOWL.get());
        Player grafter = EcologyTests.holding(h, GameType.SURVIVAL, LogisticsModule.FILTER_MEMBRANE.get(), 1);
        EcologyTests.use(h, grafter, vein, Direction.UP);
        h.assertBlockPresent(LogisticsModule.FLUID_VEIN.get(), vein);
        h.assertFalse(h.getBlockState(vein).getValue(VeinBlock.SEALED), "unsealed at first");
        IFluidHandler unsealed =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(vein), Direction.UP);
        h.assertValueEqual(
                unsealed.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE),
                0,
                "an unsealed vein carries nothing");
        Player sealer = EcologyTests.holding(h, GameType.SURVIVAL, LogisticsModule.SEALING_RESIN.get(), 1);
        EcologyTests.use(h, sealer, vein, Direction.UP);
        h.assertTrue(h.getBlockState(vein).getValue(VeinBlock.SEALED), "sealed");
        h.assertTrue(sealer.getMainHandItem().isEmpty(), "the resin is spent");
        Player sealAgain = EcologyTests.holding(h, GameType.SURVIVAL, LogisticsModule.SEALING_RESIN.get(), 1);
        EcologyTests.use(h, sealAgain, vein, Direction.UP);
        h.assertValueEqual(sealAgain.getMainHandItem().getCount(), 1, "a sealed vein takes no second resin");
        IFluidHandler sealed =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(vein), Direction.UP);
        h.assertValueEqual(
                sealed.fill(BiomassFluids.of(Biomass.of(100)), IFluidHandler.FluidAction.SIMULATE), 0, "never biomass");
        h.assertValueEqual(
                sealed.fill(new FluidStack(Fluids.LAVA, 100), IFluidHandler.FluidAction.SIMULATE), 0, "never lava");
        h.setBlock(
                junction,
                LogisticsModule.VASCULAR_JUNCTION
                        .get()
                        .defaultBlockState()
                        .setValue(VascularJunctionBlock.PORT.get(Direction.WEST), Port.INTAKE)
                        .setValue(VascularJunctionBlock.PORT.get(Direction.DOWN), Port.OUTLET));
        h.runAfterDelay(120, () -> {
            IFluidHandler bowlTanks =
                    h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(bowl), Direction.UP);
            int bowlWater = 0;
            for (int tank = 0; tank < bowlTanks.getTanks(); tank++)
                if (bowlTanks.getFluidInTank(tank).is(Fluids.WATER))
                    bowlWater += bowlTanks.getFluidInTank(tank).getAmount();
            h.assertTrue(bowlWater >= 1000, "the bowl got at least the one bucket the source gave: " + bowlWater);
            h.assertBlockPresent(Blocks.AIR, water);
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aBrokenCellCarriesItsFluidAndDropsItsItems(GameTestHelper h) {
        BlockPos cell = new BlockPos(1, Y, 1);
        BlockPos itemCell = new BlockPos(6, Y, 1);
        h.setBlock(cell, LogisticsModule.BIOMASS_VEIN.get());
        h.setBlock(itemCell, LogisticsModule.ITEM_VEIN.get());
        h.getLevel()
                .getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(cell), Direction.UP)
                .fill(BiomassFluids.of(Biomass.of(200)), IFluidHandler.FluidAction.EXECUTE);
        h.getLevel()
                .getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(itemCell), Direction.UP)
                .insertItem(0, new ItemStack(Items.BONE, 5), false);
        h.getLevel().destroyBlock(h.absolutePos(cell), true);
        h.getLevel().destroyBlock(h.absolutePos(itemCell), true);
        h.runAfterDelay(2, () -> {
            List<ItemEntity> drops = h.getEntities(EntityType.ITEM, cell, 2.0).stream()
                    .filter(drop -> drop.getItem().is(LogisticsModule.BIOMASS_VEIN_ITEM.get()))
                    .toList();
            h.assertValueEqual(drops.size(), 1, "one dropped cell");
            SimpleFluidContent carried = drops.get(0).getItem().get(LogisticsModule.FLUID_CONTENT.get());
            h.assertTrue(
                    carried != null && carried.getAmount() == 200 && BiomassFluids.isBiomass(carried.copy()),
                    "200 mB of biomass on the item");
            int bones = 0;
            for (ItemEntity drop : h.getEntities(EntityType.ITEM, itemCell, 2.0))
                if (drop.getItem().is(Items.BONE)) bones += drop.getItem().getCount();
            h.assertValueEqual(bones, 5, "the five bones dropped");
            Player builder = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            builder.setItemInHand(
                    net.minecraft.world.InteractionHand.MAIN_HAND,
                    drops.get(0).getItem().copy());
            h.setBlock(cell.below(), Blocks.STONE);
            h.placeAt(builder, builder.getMainHandItem(), cell.below(), Direction.UP);
            h.assertBlockPresent(LogisticsModule.BIOMASS_VEIN.get(), cell);
            h.assertValueEqual(biomassIn(h, cell), 200, "placed again with its packet");
            h.succeed();
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void aCreativeBreakDropsAnItemVeinsPacketOnceAndASealedVeinMovesSealed(GameTestHelper h) {
        BlockPos cell = new BlockPos(1, Y, 1);
        h.setBlock(cell, LogisticsModule.ITEM_VEIN.get());
        h.getLevel()
                .getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(cell), Direction.UP)
                .insertItem(0, new ItemStack(Items.BONE, 5), false);
        Player creative = EcologyTests.holding(h, GameType.CREATIVE, Items.AIR, 0);
        BlockState state = h.getBlockState(cell);
        state.getBlock().playerWillDestroy(h.getLevel(), h.absolutePos(cell), state, creative);
        h.getLevel().removeBlock(h.absolutePos(cell), false);
        h.runAfterDelay(2, () -> {
            int bonesOnGround = 0;
            ItemStack veinItem = ItemStack.EMPTY;
            for (ItemEntity drop : h.getEntities(EntityType.ITEM, cell, 2.0)) {
                if (drop.getItem().is(Items.BONE))
                    bonesOnGround += drop.getItem().getCount();
                if (drop.getItem().is(LogisticsModule.ITEM_VEIN_ITEM.get()))
                    veinItem = drop.getItem().copy();
            }
            h.assertValueEqual(bonesOnGround, 5, "the five bones dropped");
            h.assertFalse(veinItem.isEmpty(), "the Creative player gets the cell back");
            Player builder = EcologyTests.holding(h, GameType.SURVIVAL, Items.AIR, 0);
            builder.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, veinItem);
            h.setBlock(cell.below(), Blocks.STONE);
            h.placeAt(builder, builder.getMainHandItem(), cell.below(), Direction.UP);
            h.assertBlockPresent(LogisticsModule.ITEM_VEIN.get(), cell);
            h.assertValueEqual(itemsIn(h, 1, 1), 0, "the placed cell holds no second copy of the bones");
            BlockPos sealed = new BlockPos(4, Y, 1);
            h.setBlock(
                    sealed, LogisticsModule.FLUID_VEIN.get().defaultBlockState().setValue(VeinBlock.SEALED, true));
            h.getLevel().destroyBlock(h.absolutePos(sealed), true);
            h.runAfterDelay(2, () -> {
                var drops = h.getEntities(EntityType.ITEM, sealed, 2.0).stream()
                        .filter(drop -> drop.getItem().is(LogisticsModule.FLUID_VEIN_ITEM.get()))
                        .toList();
                h.assertValueEqual(drops.size(), 1, "one sealed Fluid Vein dropped");
                ItemStack dropped = drops.getFirst().getItem().copy();
                builder.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dropped);
                h.setBlock(sealed.below(), Blocks.STONE);
                h.placeAt(builder, builder.getMainHandItem(), sealed.below(), Direction.UP);
                h.assertBlockPresent(LogisticsModule.FLUID_VEIN.get(), sealed);
                h.assertTrue(h.getBlockState(sealed).getValue(VeinBlock.SEALED), "a moved sealed vein stays sealed");
                h.succeed();
            });
        });
    }

    @GameTest(templateNamespace = "infestusfrontier_tests", template = "empty")
    public static void theProbeCyclesAJunctionPortAndSneakingReadsIt(GameTestHelper h) {
        BlockPos ground = new BlockPos(1, Y, 1);
        BlockPos junction = ground.above();
        h.setBlock(ground, EcologyTests.mature());
        h.setBlock(junction, LogisticsModule.VASCULAR_JUNCTION.get());
        Player player = EcologyTests.holding(h, GameType.SURVIVAL, InteractionModule.SYNAPTIC_PROBE.get(), 1);
        var north = VascularJunctionBlock.PORT.get(Direction.NORTH);
        h.assertValueEqual(h.getBlockState(junction).getValue(north), Port.CLOSED, "closed at first");
        EcologyTests.use(h, player, junction, Direction.NORTH);
        h.assertValueEqual(h.getBlockState(junction).getValue(north), Port.INTAKE, "then intake");
        EcologyTests.use(h, player, junction, Direction.NORTH);
        h.assertValueEqual(h.getBlockState(junction).getValue(north), Port.OUTLET, "then outlet");
        EcologyTests.use(h, player, junction, Direction.NORTH);
        h.assertValueEqual(h.getBlockState(junction).getValue(north), Port.CLOSED, "then closed again");
        h.assertValueEqual(
                h.getBlockState(junction).getValue(VascularJunctionBlock.PORT.get(Direction.UP)),
                Port.CLOSED,
                "other faces untouched");
        player.setShiftKeyDown(true);
        EcologyTests.use(h, player, junction, Direction.NORTH);
        h.assertValueEqual(h.getBlockState(junction).getValue(north), Port.CLOSED, "sneaking reads, it does not cycle");
        IFluidHandler byHand =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(junction), null);
        h.assertValueEqual(
                byHand.fill(BiomassFluids.of(Biomass.BUCKET), IFluidHandler.FluidAction.EXECUTE),
                1000,
                "a bucket fills the tank by hand");
        IFluidHandler closedFace =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(junction), Direction.NORTH);
        h.assertValueEqual(
                closedFace.drain(250, IFluidHandler.FluidAction.SIMULATE).getAmount(),
                0,
                "a closed face gives nothing");
        h.assertValueEqual(
                byHand.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.SIMULATE),
                0,
                "one fluid at a time");
        h.succeed();
    }

    private static void fill(GameTestHelper h, BlockPos tank, int mb) {
        IFluidHandler handler =
                h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(tank), Direction.UP);
        h.assertValueEqual(
                handler.fill(BiomassFluids.of(Biomass.of(mb)), IFluidHandler.FluidAction.EXECUTE), mb, "filled " + mb);
    }

    static int biomassIn(GameTestHelper h, BlockPos pos) {
        IFluidHandler handler = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(pos), null);
        if (handler == null) return 0;
        int total = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++)
            if (BiomassFluids.isBiomass(handler.getFluidInTank(tank)))
                total += handler.getFluidInTank(tank).getAmount();
        return total;
    }

    private static int itemsIn(GameTestHelper h, int fromX, int toX) {
        int total = 0;
        for (int x = fromX; x <= toX; x++) {
            var handler = h.getLevel()
                    .getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(new BlockPos(x, Y, 1)), null);
            if (handler != null) total += handler.getStackInSlot(0).getCount();
        }
        return total;
    }
}
